package pl.tekla.pellet

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.drawText
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min

sealed class Dlg {
    object Pin : Dlg()
    object Mode : Dlg()
    object Fuel : Dlg()
    class EditRow(val row: SetRow, val cur: Double?, val done: (Double) -> Unit) : Dlg()
    class EditCfg(val row: CfgRow) : Dlg()
}

class Ui(val src: DataSource, val v: Map<String, Double>, val level: Int, val full: Boolean, val go: (String) -> Unit, val open: (Dlg) -> Unit, val toast: (String) -> Unit, val alarmActive: Boolean = false, val hist: List<Float> = emptyList()) {
    val m get() = Ctl.model
    fun n(p: String?): Double? = if (p == null) null else v[p]
}

fun f1(x: Double?, d: Int = 1): String = if (x == null || x.isNaN()) "--" else if (d == 0) Math.round(x).toString() else String.format(java.util.Locale.US, "%.${d}f", x)
val PROGICON = listOf(133, 135, 122, 128, 120)

fun navIcon(ui: Ui, route: String): Int = ui.m.menu.firstOrNull { ui.m.routeOf(it) == route }?.icon ?: 0

@Composable
fun TeklaApp() {
    TeklaTheme {
        var locked by remember { mutableStateOf(AppLock.isSet()) }
        if (locked) { LockScreen { locked = false }; return@TeklaTheme }
        val src = Ctl.source ?: return@TeklaTheme
        val ctx = Ctl.ctx
        val vals by src.values.collectAsState(); val level by src.level.collectAsState(); val full by src.fullMode.collectAsState(); val conn by src.connected.collectAsState()
        var frame by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) { while (true) { delay(130); frame++ } }
        val stack = remember { mutableStateListOf("home").also { l -> Ctl.startScreen?.let { if (it != "home") l.add(it) } } }
        var dlg by remember { mutableStateOf<Dlg?>(null) }
        val hist = remember(src) { mutableStateListOf<Float>().also { l -> if (src.isDemo) { val base = (src.values.value["0x2001"] ?: 60.0).toFloat(); for (i in 0 until 120) l.add(base + 3.5f * kotlin.math.sin((i - 119) / 11.0).toFloat() - 1.5f) } } }
        LaunchedEffect(src) { while (true) { delay(15000); src.values.value["0x2001"]?.let { hist.add(it.toFloat()); while (hist.size > 120) hist.removeAt(0) } } }
        var alarmActive by remember { mutableStateOf(false) }
        LaunchedEffect(src) { while (true) { alarmActive = src.alarms().any { it.state == "AKTYWNY" }; delay(15000) } }
        fun go(s: String) { if (s == "home") { stack.clear(); stack.add("home") } else if (stack.last() != s) stack.add(s) }
        BackHandler(stack.size > 1) { stack.removeAt(stack.lastIndex) }
        val ui = Ui(src, vals, level, full, ::go, { dlg = it }, { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() }, alarmActive, hist.toList())
        val screen = stack.last()
        CompositionLocalProvider(LocalFrame provides frame) {
            Scaffold(
                containerColor = Bg,
                topBar = { TopBar(ui, screen, conn, src.isDemo) },
                bottomBar = {
                    val co = Ctl.model.circuits.map { it.id }.filter { it.startsWith("co") }.take(2)
                    val coRoute = co.firstOrNull() ?: "co1"
                    val coLabel = if (co.size >= 2) "CO1/2" else "CO"
                    val nav = listOf(
                        Triple("home", "Pulpit", 0),
                        Triple(coRoute, coLabel, 0),
                        Triple("cwu", "CWU", 0),
                        Triple("power", "Power", 0),
                        Triple("more", "Menu", 0)
                    )
                    NavigationBar(containerColor = Color(0xFF101014)) {
                        for ((id, t, _) in nav) NavigationBarItem(
                            selected = screen == id,
                            onClick = { go(id) },
                            icon = { BottomNavIcon(t, selected = screen == id) },
                            label = { Text(t, fontSize = 9.sp, maxLines = 1, softWrap = false) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Acc, selectedTextColor = Acc, indicatorColor = Panel2,
                                unselectedIconColor = Mut, unselectedTextColor = Mut
                            )
                        )
                    }
                },
            ) { pad ->
                Column(Modifier.padding(pad).fillMaxSize().then(if (screen == "home") Modifier else Modifier.verticalScroll(rememberScrollState())).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    key(screen) { Body(ui, screen) }
                }
            }
            dlg?.let { Dialogs(ui, it) { dlg = null } }
        }
    }
}

@Composable
fun BottomNavIcon(label: String, selected: Boolean) {
    val color = if (selected) Acc else Mut
    Canvas(Modifier.size(24.dp)) {
        val w = size.width; val h = size.height; val s = 1.7.dp.toPx()
        when {
            label == "Pulpit" -> {
                drawRoundRect(color, Offset(w*.16f,h*.16f), Size(w*.68f,h*.68f), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()), style=Stroke(s))
                drawLine(color, Offset(w*.50f,h*.10f), Offset(w*.50f,h*.90f), s)
                drawLine(color, Offset(w*.10f,h*.50f), Offset(w*.90f,h*.50f), s)
            }
            label == "Power" -> {
                drawArc(color, 210f, 120f, false, Offset(w*.20f,h*.14f), Size(w*.60f,h*.68f), style=Stroke(s))
                drawLine(color, Offset(w*.50f,h*.10f), Offset(w*.50f,h*.53f), s)
            }
            label.contains("CO") -> {
                drawLine(color, Offset(w*.28f,h*.18f), Offset(w*.28f,h*.82f), s)
                drawLine(color, Offset(w*.42f,h*.12f), Offset(w*.42f,h*.88f), s)
                drawLine(color, Offset(w*.58f,h*.12f), Offset(w*.58f,h*.88f), s)
                drawLine(color, Offset(w*.72f,h*.18f), Offset(w*.72f,h*.82f), s)
                drawLine(color, Offset(w*.20f,h*.36f), Offset(w*.80f,h*.36f), s)
                drawLine(color, Offset(w*.20f,h*.64f), Offset(w*.80f,h*.64f), s)
            }
            label == "Kocioł" -> {
                drawRoundRect(color, Offset(w*.28f,h*.12f), Size(w*.44f,h*.76f), androidx.compose.ui.geometry.CornerRadius(w*.08f), style=Stroke(s))
                drawRect(color, Offset(w*.39f,h*.49f), Size(w*.22f,h*.22f), style=Stroke(s))
                drawLine(color, Offset(w*.18f,h*.88f), Offset(w*.82f,h*.88f), s)
            }
            label == "Palnik" -> {
                val p=androidx.compose.ui.graphics.Path().apply { moveTo(w*.50f,h*.08f); cubicTo(w*.30f,h*.35f,w*.60f,h*.40f,w*.36f,h*.62f); cubicTo(w*.18f,h*.80f,w*.38f,h*.94f,w*.52f,h*.90f); cubicTo(w*.78f,h*.82f,w*.76f,h*.56f,w*.61f,h*.43f); cubicTo(w*.57f,h*.30f,w*.60f,h*.18f,w*.50f,h*.08f); close() }
                drawPath(p, color, style=Stroke(s, cap=androidx.compose.ui.graphics.StrokeCap.Round, join=androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            label == "OLD_CO_ICON_UNUSED" -> {
                drawLine(color, Offset(w*.28f,h*.18f), Offset(w*.28f,h*.82f), s); drawLine(color, Offset(w*.42f,h*.12f), Offset(w*.42f,h*.88f), s)
                drawLine(color, Offset(w*.58f,h*.12f), Offset(w*.58f,h*.88f), s); drawLine(color, Offset(w*.72f,h*.18f), Offset(w*.72f,h*.82f), s)
                drawLine(color, Offset(w*.20f,h*.36f), Offset(w*.80f,h*.36f), s); drawLine(color, Offset(w*.20f,h*.64f), Offset(w*.80f,h*.64f), s)
            }
            label == "CWU" -> {
                val p=androidx.compose.ui.graphics.Path().apply { moveTo(w*.50f,h*.10f); cubicTo(w*.22f,h*.45f,w*.24f,h*.62f,w*.50f,h*.88f); cubicTo(w*.76f,h*.62f,w*.78f,h*.45f,w*.50f,h*.10f); close() }
                drawPath(p, color, style=Stroke(s, cap=androidx.compose.ui.graphics.StrokeCap.Round, join=androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            else -> for (i in 0..2) drawRoundRect(color, Offset(w*.18f,h*(.28f+i*.22f)), Size(w*.64f,h*.07f), androidx.compose.ui.geometry.CornerRadius(h*.03f))
        }
    }
}

@Composable
fun NotificationGlyph(active: Boolean) {
    Canvas(Modifier.size(24.dp)) {
        val col = if (active) Bad else Mut
        val s = 1.7.dp.toPx()
        drawArc(col, 205f, 130f, false, Offset(size.width*.22f,size.height*.12f), Size(size.width*.56f,size.height*.62f), style=Stroke(s))
        drawLine(col, Offset(size.width*.30f,size.height*.73f), Offset(size.width*.70f,size.height*.73f), s)
        drawCircle(col, size.minDimension*.055f, Offset(size.width*.50f,size.height*.84f))
        if (active) drawCircle(Bad, size.minDimension*.10f, Offset(size.width*.79f,size.height*.16f))
    }
}

@Composable
fun MenuGlyph(title: String) {
    val color = Mut
    Canvas(Modifier.size(28.dp)) {
        val w=size.width; val h=size.height; val s=1.6.dp.toPx()
        when {
            title.contains("Kocioł", true) -> {
                drawRoundRect(color, Offset(w*.28f,h*.10f), Size(w*.44f,h*.78f), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()), style=Stroke(s))
                drawRect(color, Offset(w*.39f,h*.48f), Size(w*.22f,h*.22f), style=Stroke(s))
            }
            title.contains("Palnik", true) -> {
                drawArc(color, 205f, 230f, false, Offset(w*.22f,h*.12f), Size(w*.56f,h*.70f), style=Stroke(s))
            }
            title.contains("Alarm", true) -> {
                drawCircle(color, w*.30f, Offset(w*.50f,h*.48f), style=Stroke(s)); drawLine(color,Offset(w*.50f,h*.28f),Offset(w*.50f,h*.55f),s); drawLine(color,Offset(w*.50f,h*.55f),Offset(w*.65f,h*.65f),s)
            }
            title.contains("Inform", true) -> {
                drawCircle(color,w*.34f,Offset(w*.50f,h*.48f),style=Stroke(s)); drawCircle(color,w*.04f,Offset(w*.50f,h*.34f)); drawLine(color,Offset(w*.50f,h*.46f),Offset(w*.50f,h*.68f),s)
            }
            title.contains("Konfigur", true) -> {
                drawCircle(color,w*.23f,Offset(w*.50f,h*.50f),style=Stroke(s)); for(i in 0..7){ val a=i*Math.PI/4; drawLine(color,Offset(w*.50f+(w*.33f*kotlin.math.cos(a)).toFloat(),h*.50f+(h*.33f*kotlin.math.sin(a)).toFloat()),Offset(w*.50f+(w*.43f*kotlin.math.cos(a)).toFloat(),h*.50f+(h*.43f*kotlin.math.sin(a)).toFloat()),s) }
            }
            else -> {
                drawRoundRect(color,Offset(w*.18f,h*.18f),Size(w*.64f,h*.64f),androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),style=Stroke(s))
            }
        }
    }
}

@Composable
fun TopBar(ui: Ui, screen: String, conn: Boolean, demo: Boolean) {
    val title = when {
        screen == "home" -> "TeklaPellet"
        screen == "more" -> "Menu"
        screen == "notifications" -> "Powiadomienia"
        screen == "module" -> "Moduł ESP"
        screen == "log" -> "Dziennik"
        screen == "appset" -> "Ustawienia aplikacji"
        screen.startsWith("set:") -> "Nastawy"
        screen.startsWith("sched:") -> "Harmonogram"
        else -> ui.m.screens[screen]?.title ?: ui.m.menu.firstOrNull { ui.m.routeOf(it) == screen }?.title ?: screen
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }

    Row(
        Modifier.fillMaxWidth().background(Color(0xFF101014)).statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (screen != "home") {
            Text("‹", color = Txt, fontSize = 31.sp, modifier = Modifier.clickable { ui.go("home") }.padding(horizontal = 3.dp))
        }
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(Modifier.size(7.dp).background(if (demo) Warn else if (conn) Okc else Bad, RoundedCornerShape(4.dp)))
            Column {
                Text(
                    if (screen == "home") {
                        if (demo) "TeklaPellet (demo)" else Ctl.activeName()
                    } else title,
                    color = Txt, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1
                )
                if (screen == "home") {
                    Text(
                        if (demo) "tryb demonstracyjny" else if (conn) "połączono" else "offline · ostatnie dane",
                        color = Mut, fontSize = 10.5.sp, maxLines = 1
                    )
                }
            }
        }
        if (screen == "home") {
            val wx = ui.n("0x215F")
            if (wx != null) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Ic(if (ui.n("0x3729")?.toInt() == 1) 180 else 181, 22.dp)
                Text(f1(wx) + "°", color = Txt, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(java.text.SimpleDateFormat("HH:mm", java.util.Locale("pl")).format(now), color = Acc2, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            if (screen == "home") Text(java.text.SimpleDateFormat("d MMM", java.util.Locale("pl")).format(now), color = Mut, fontSize = 9.5.sp)
        }
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (ui.alarmActive) Color(0xFF3A1B1B) else Color.Transparent)
                .clickable { ui.go("notifications") }
                .padding(5.dp),
            contentAlignment = Alignment.Center
        ) {
            NotificationGlyph(ui.alarmActive)
            if (ui.alarmActive) Box(
                Modifier.align(Alignment.TopEnd).size(7.dp).background(Bad, RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun Chip(t: String, dot: Color, click: () -> Unit) {
    Row(Modifier.clip(RoundedCornerShape(20.dp)).border(1.dp, Line, RoundedCornerShape(20.dp)).background(Panel).clickable(onClick = click).padding(horizontal = 10.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(dot, RoundedCornerShape(4.dp))); if (t.isNotEmpty()) { Spacer(Modifier.width(6.dp)); Text(t, color = Mut, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
    }
}

@Composable
fun Card(title: String?, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().background(CardBrush, RoundedCornerShape(16.dp)).border(1.dp, Line, RoundedCornerShape(16.dp)).padding(14.dp)) {
        if (title != null) { Text(title.uppercase(), color = Mut, fontSize = 11.sp, letterSpacing = 0.9.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(8.dp)) }
        content()
    }
}

@Composable
fun RowItem(label: String, value: String, locked: Boolean = false, click: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().then(if (click != null) Modifier.clickable(onClick = click) else Modifier).padding(vertical = 13.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label + if (locked) " 🔒" else "", color = if (locked) Mut else Txt, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(value, color = Txt, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    }
    HorizontalDivider(color = Line)
}

@Composable
fun BigValue(v: String, unit: String) { Row(verticalAlignment = Alignment.Bottom) { Text(v, color = Txt, fontSize = 46.sp, fontWeight = FontWeight.Light); Text(unit, color = Acc, fontSize = 18.sp, modifier = Modifier.padding(start = 3.dp, bottom = 6.dp)) } }

@Composable
fun PowerView(ui: Ui) {
    val scope = rememberCoroutineScope()
    val on = (ui.n("0x2005") ?: 0.0) != 0.0
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GlowCard(Modifier.fillMaxWidth().height(118.dp), if (on) Okc else null, null) {
            Row(Modifier.fillMaxSize().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Zasilanie kotła", color = Txt, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (on) "Kocioł włączony" else "Kocioł wyłączony", color = if (on) Okc else Mut, fontSize = 12.sp)
                }
                Switch(
                    checked = on,
                    onCheckedChange = { v -> scope.launch { ui.src.write("0x2005", if (v) 257.0 else 256.0) } },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Acc, uncheckedThumbColor = Color.White, uncheckedTrackColor = Panel2)
                )
            }
        }
        GlowCard(Modifier.fillMaxWidth(), null, null) {
            Text("Tryby", color = Txt, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            val modes = ui.m.programModes
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for (m in modes.take(4)) {
                    val selected = (ui.n("0x2002")?.toInt() ?: -1) == m.v
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                            .background(if (selected) Acc.copy(alpha=.22f) else Panel2)
                            .border(1.dp, if (selected) Acc else Line, RoundedCornerShape(10.dp))
                            .clickable { scope.launch { ui.src.write("0x2002", m.v.toDouble()) } }
                            .padding(vertical=11.dp),
                        contentAlignment = Alignment.Center
                    ) { Text(m.label, color = if (selected) Acc2 else Mut, fontSize = 11.sp, maxLines=1, softWrap=false) }
                }
            }
        }
    }
}

@Composable
fun Body(ui: Ui, screen: String) {
    when {
        screen == "power" -> PowerView(ui)
        screen == "home" -> HomeView(ui)
        screen == "more" -> MoreView(ui)
        screen == "notifications" -> NotificationsView(ui)
        screen == "appset" -> AppSettings(ui)
        screen == "config" -> ConfigView(ui)
        screen == "alarms" -> AlarmsView(ui)
        screen == "info" -> InfoView(ui)
        screen == "stats" -> ListView(ui, "0x11|STATYSTYKI|inst1", readOnly = true)
        screen == "network" -> NetworkView(ui)
        screen == "circuits" -> CircuitsView(ui)
        screen == "module" -> ModuleView(ui)
        screen == "log" -> LogView(ui)
        screen == "outtest" -> OutTestView(ui)
        screen.startsWith("set:") -> ListView(ui, screen.removePrefix("set:"))
        screen.startsWith("sched:") -> SchedView(ui, screen.removePrefix("sched:"))
        ui.m.screens.containsKey(screen) -> ScreenView(ui, ui.m.screens[screen]!!)
        else -> Text("Brak ekranu: $screen", color = Mut)
    }
}

@Composable
fun BoilerImage(ui: Ui) {
    val id = ui.m.boilerImages[ui.n("0x370A")?.toInt() ?: 2] ?: 12
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Ic(id, 170.dp) }
}

@Composable
fun BarView(ui: Ui, cells: List<Cell>) {
    if (cells.isEmpty()) return
    val frame = LocalFrame.current
    Row(Modifier.fillMaxWidth().padding(top = 12.dp).background(Panel2, RoundedCornerShape(12.dp)).border(1.dp, Line, RoundedCornerShape(12.dp)).padding(10.dp).heightIn(min = 66.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
        for (c in cells) {
            val s = ui.n(c.p)?.toInt() ?: 0
            if (s == 0) continue    // stan 0 = ikona niewidoczna (jak w panelu)
            // stały układ komórki: linia opisu (14 dp) + kwadrat ikony (32 dp) wyśrodkowany => ikony zawsze na tej samej wysokości
            var label = ""; var icon = 0; var isz = 30
            when (c.k) {
                "pump" -> { icon = ui.m.pumpFrames[frame % ui.m.pumpFrames.size]; isz = 20 }
                "mixer" -> { icon = if (s == 1) 106 else 107; isz = 22; label = "mieszacz" }
                "grate" -> { icon = 59; isz = 28; label = if (s == 1) "◀◀◀" else "▶▶▶" }
                else -> {
                    if (c.n != null) { val x = ui.n(c.n) ?: 0.0; label = if (c.hm) { val mi = Math.round(x).toInt(); "${mi / 60}:${(mi % 60).toString().padStart(2, '0')}" } else if (c.nd) f1(x, 1) else if (x != 0.0) Math.round(x).toString() else "" }
                    icon = c.fr[frame % c.fr.size]
                }
            }
            Column(Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.height(14.dp), contentAlignment = Alignment.Center) { Text(label, color = Acc2, fontSize = 10.sp, maxLines = 1, softWrap = false) }
                Box(Modifier.size(32.dp), contentAlignment = Alignment.Center) { Ic(icon, isz.dp) }
            }
        }
    }
}

class IxData(val label: String, val value: String, val on: Boolean, val color: Color)
val CCLOSE = Color(0xFF5AB4FF); val COPEN = Color(0xFFFF5D5D)

/** Delikatny wskaźnik wyjścia: mała kropka (świeci kolorem funkcji gdy aktywne, inaczej ledwie widoczna) + drobna etykieta. Mieszacz: O (otwieranie, czerwony) / Z (zamykanie, niebieski). */
@Composable
fun Ix(d: IxData) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(6.dp).drawBehind { if (d.on) drawCircle(d.color.copy(alpha = 0.35f), radius = size.minDimension * 0.95f); drawCircle(if (d.on) d.color else Color(0xFF2A2A32)) })
        Text(d.label, color = if (d.on) Color(0xFFB4B4BE) else Color(0xFF5D5D68), fontSize = 10.5.sp, maxLines = 1, softWrap = false)
        if (d.value.isNotEmpty()) Text(d.value, color = d.color, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

fun barIx(ui: Ui, c: Cell, id: String): IxData? {
    if (ui.n(c.p) == null) return null     // wyjście nie istnieje (kocioł nie zwraca danych) - bez wskaźnika
    val v = ui.n(c.p)?.toInt() ?: 0; val on = v != 0
    return when (c.k) {
        "mixer" -> IxData("Mieszacz", if (v == 1) "Z" else if (v == 2) "O" else "", on, if (v == 1) CCLOSE else COPEN)
        "pump" -> IxData(c.l ?: "Pompa", "", on, Okc)
        "grate" -> IxData("Ruszt", if (v == 1) "◀" else if (on) "▶" else "", on, Okc)
        else -> {
            var value = ""
            if (c.n != null && on) { val x = ui.n(c.n) ?: 0.0; value = if (c.hm) { val mi = Math.round(x).toInt(); "${mi / 60}:${(mi % 60).toString().padStart(2, '0')}" } else if (c.nd) f1(x, 1) + " s" else if (x != 0.0) Math.round(x).toString() + "%" else "" }
            IxData(c.l ?: "Wyjście", value, on, if (id == "palnik") Acc2 else Okc)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IxRow(items: List<IxData>) { FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) { for (d in items) Ix(d) } }

@Composable
fun KvRow(l: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(l, color = Mut, fontSize = 12.5.sp, modifier = Modifier.weight(1f)); Text(v, color = Txt, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
}

val CardShape = RoundedCornerShape(18.dp)
val CardBrush = Brush.verticalGradient(listOf(Color(0xFF34414A), Color(0xFF242E35), Color(0xFF1A2228)))

/** Karta/kafel z cienką linią LED przy górnej krawędzi (kolor stanu; gdy null - wygaszona). */
@Composable
fun GlowCard(modifier: Modifier, led: Color?, click: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.clip(CardShape).background(CardBrush).border(1.dp, Line, CardShape).then(if (click != null) Modifier.clickable(onClick = click) else Modifier)) {
        Box(Modifier.align(Alignment.TopCenter).fillMaxWidth(0.64f).height(2.dp).background(Brush.horizontalGradient(listOf(Color.Transparent, led ?: Color(0xFF2A2A32), Color.Transparent))))
        Column(Modifier.fillMaxSize().padding(horizontal = 13.dp, vertical = 11.dp), content = content)
    }
}

@Composable
fun Tile(title: String, modeIcon: Int?, led: Color?, modifier: Modifier, go: () -> Unit, ixs: List<IxData>, value: @Composable () -> Unit) {
    GlowCard(modifier.heightIn(min = 92.dp), led, go) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(title.uppercase(), color = Txt, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (modeIcon != null) Ic(modeIcon, 17.dp)
        }
        Spacer(Modifier.height(4.dp)); value()
        Spacer(Modifier.weight(1f).heightIn(min = 4.dp))
        if (ixs.isNotEmpty()) IxRow(ixs.take(3))
    }
}

@Composable
fun BigNum(big: String, small: String = "", size: Int = 26) {
    Row(verticalAlignment = Alignment.Bottom) { Text(big, color = Txt, fontSize = size.sp, fontWeight = FontWeight.Light, style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum")); if (small.isNotEmpty()) Text(small, color = Mut, fontSize = 12.sp, modifier = Modifier.padding(start = 3.dp, bottom = 3.dp)) }
}

fun tileTitle(id: String, s: ScreenDef) = if (id.startsWith("co") && id.length == 3) "CO ${id.last()}" else if (id == "cwu") "CWU" else s.title

@Composable
fun SysTile(ui: Ui, id: String, modifier: Modifier) {
    val s = ui.m.screens.getValue(id)
    val active = (id.startsWith("co") || id == "cwu") && (ui.n(s.mode)?.toInt() ?: 0) > 0
    val mi = s.mode?.let { m -> ui.m.programModes.firstOrNull { it.v == (ui.n(m)?.toInt() ?: -1) }?.icon }
    Tile(tileTitle(id, s), mi, if (active) Color(0xFFE8E8EE) else null, modifier, { ui.go(id) }, s.bar.mapNotNull { barIx(ui, it, id) }) {
        if (s.state != null) Text(ui.m.statusTables[s.state]?.getOrNull(ui.n(s.stateP ?: s.big)?.toInt() ?: 0) ?: "?", color = Txt, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        else if (s.big != null) BigNum(f1(ui.n(s.big)), (if (s.bigUnit == "°C") "°" else s.bigUnit) + (if (s.pill != null) " / ${f1(ui.n(s.pill), 0)}°" else ""))
    }
}

/** Zbiornik paliwa: wypełnienie rzędami kropek w pionie, od dołu (6 rzędów x 4 kolumny; liczba zapalonych wg promili pojemności); przy rezerwie litera E. */
@Composable
fun HopperIcon(level: Float, empty: Boolean, w: androidx.compose.ui.unit.Dp = 40.dp, h: androidx.compose.ui.unit.Dp = 48.dp) {
    Canvas(Modifier.size(w, h)) {
        val sc = size.width / 40f
        val left = 5f * sc
        val top = 3f * sc
        val width = 30f * sc
        val height = 42f * sc
        val radius = 7f * sc
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFF4A4B54), Color(0xFF17171C))),
            Offset(left, top), Size(width, height),
            androidx.compose.ui.geometry.CornerRadius(radius)
        )
        drawRoundRect(
            Color(0xFF9A9BA5),
            Offset(left, top), Size(width, height),
            androidx.compose.ui.geometry.CornerRadius(radius),
            style = Stroke(1.4f * sc)
        )
        if (!empty) {
            val pct = level.coerceIn(0f, 1f)
            val liquidH = (height - 5f * sc) * pct
            val liquidTop = top + height - liquidH
            drawRoundRect(
                Brush.verticalGradient(listOf(Color(0xFFFFB04A), Color(0xFFE06D19))),
                Offset(left + 2f * sc, liquidTop),
                Size(width - 4f * sc, liquidH),
                androidx.compose.ui.geometry.CornerRadius(5f * sc)
            )
            // spokojne "lustro" cieczy zamiast kropek na poziomach
            drawLine(
                Color(0xFFFFD08A).copy(alpha = 0.75f),
                Offset(left + 5f * sc, liquidTop + 2f * sc),
                Offset(left + width - 5f * sc, liquidTop + 2f * sc),
                strokeWidth = 1.2f * sc
            )
        } else {
            drawLine(Acc2, Offset(left + 8f * sc, top + height / 2), Offset(left + width - 8f * sc, top + height / 2), 2f * sc)
        }
    }
}

@Composable
fun FuelTile(ui: Ui, modifier: Modifier) {
    val fuel = ui.n("0x3601") ?: 0.0; val cap = ui.n("0x3600") ?: 0.0; val res = ui.n("0x3603") ?: 0.0; val empty = res > 0 && fuel <= res; val level = if (cap > 0) (fuel / cap).coerceIn(0.0, 1.0).toFloat() else 0f
    Tile("Paliwo", null, if (empty) Acc2 else null, modifier, { ui.open(Dlg.Fuel) }, listOf(IxData(if (empty) "rezerwa" else "z ${f1(cap, 0)} kg", "", empty, Acc2))) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { HopperIcon(level, empty, 34.dp, 42.dp); BigNum(f1(fuel, 0), "kg", 25) }
    }
}

@Composable
fun PowerTile(ui: Ui, modifier: Modifier, wide: Boolean = false) {
    val on = ui.n("0x2005") != 0.0; val prog = PROGICON[if (on) min((ui.n("0x2002") ?: 0.0).toInt(), 4) else 4]
    if (wide) { GlowCard(modifier, if (on) Okc else null, { ui.go("kociol") }) {   // samotny kafel w ostatnim wierszu: jedna kompaktowa linia
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) { Text("ZASILANIE", color = Mut, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp); Text(if (on) "Włączone" else "Wyłączone", color = Txt, fontSize = 19.sp, fontWeight = FontWeight.Medium) }
            Ix(IxData("Kocioł", "", on, Okc)); Ic(prog, 22.dp)
        } }; return }
    Tile("Zasilanie", prog, if (on) Okc else null, modifier, { ui.go("kociol") }, listOf(IxData("Kocioł", "", on, Okc))) { Text(if (on) "Włączone" else "Wyłączone", color = Txt, fontSize = 19.sp, fontWeight = FontWeight.Medium) }
}

@Composable
fun TileGrid(ui: Ui, entries: List<String>, rowH: androidx.compose.ui.unit.Dp = 116.dp) {
    for (pair in entries.chunked(2)) {
        Row(Modifier.height(if (pair.size == 1) 70.dp else rowH), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            for (id in pair) { val m = if (pair.size == 1) Modifier.fillMaxWidth().fillMaxHeight() else Modifier.weight(1f).fillMaxHeight(); when (id) { "fuel" -> FuelTile(ui, m); "power" -> PowerTile(ui, m, pair.size == 1); else -> SysTile(ui, id, m) } }
        }
    }
}

fun visibleTileIds(ui: Ui): List<String> = ui.m.menu.filter { ui.m.menuVisible(it, ui.v, ui.level) }.mapNotNull { ui.m.routeOf(it) }.filter { it != "kociol" && it != "palnik" && ui.m.screens.containsKey(it) }

@Composable
fun CircuitsView(ui: Ui) {
    val ids = visibleTileIds(ui).filter { (it.startsWith("co") && it.length == 3) || it == "cwu" }
    if (ids.isEmpty()) Text("Brak skonfigurowanych obwodów", color = Mut) else TileGrid(ui, ids)
}

@Composable
fun Spark(hist: List<Float>, set: Float?, modifier: Modifier) {
    Canvas(modifier) {
        if (hist.size < 2) return@Canvas
        val lo = minOf(hist.min(), set ?: hist.min()) - 1f; val hi = maxOf(hist.max(), set ?: hist.max()) + 1f; val span = (hi - lo).coerceAtLeast(1f)
        fun px(i: Int): Float = size.width * i / (hist.size - 1)
        fun py(v: Float): Float = size.height * (1f - (v - lo) / span)
        val line = androidx.compose.ui.graphics.Path().apply { moveTo(px(0), py(hist[0])); for (i in 1 until hist.size) lineTo(px(i), py(hist[i])) }
        val fill = androidx.compose.ui.graphics.Path().apply { addPath(line); lineTo(size.width, size.height); lineTo(0f, size.height); close() }
        drawPath(fill, Brush.verticalGradient(listOf(Acc.copy(alpha = 0.22f), Color.Transparent)))
        if (set != null) drawLine(Color(0xFF5D5D68), Offset(0f, py(set)), Offset(size.width, py(set)), 1.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
        drawPath(line, Acc2, style = Stroke(1.6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        drawCircle(Acc2, 3.dp.toPx(), Offset(size.width, py(hist.last())))
    }
}

@Composable
fun HomeView(ui: Ui) {
    val t = ui.n("0x2001"); val set = ui.n("0x2004"); val bs = ui.n("0x2007")?.toInt() ?: 0
    val on = ui.n("0x2005") != 0.0
    val prog = PROGICON[if (on) min((ui.n("0x2002") ?: 0.0).toInt(), 4) else 4]
    val run = bs in 1..8
    val entries = visibleTileIds(ui).filter { it != "kociol" && it != "palnik" } + listOf("fuel", "power")
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
        GlowCard(Modifier.fillMaxWidth().height(160.dp), if (run) Warn else null, { ui.go("kociol") }) {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.width(86.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                        val imageId = ui.m.boilerImages[ui.n("0x370A")?.toInt() ?: 2] ?: 12
                        Ic(imageId, 76.dp)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Kocioł", color = Txt, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Box(Modifier.size(8.dp).background(if (run) Okc else Mut, RoundedCornerShape(4.dp)))
                            Spacer(Modifier.width(5.dp))
                            Text(if (run) "Praca" else "Postój", color = if (run) Okc else Mut, fontSize = 10.5.sp)
                        }
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(f1(t), color = Txt, fontSize = 32.sp, fontWeight = FontWeight.Light)
                            Text("°C", color = Txt, fontSize = 14.sp, modifier = Modifier.padding(start = 3.dp, bottom = 4.dp))
                        }
                        Text("→ ${f1(set,0)}°C", color = Acc2, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column(Modifier.width(66.dp), horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Ic(prog, 17.dp)
                        Text("${f1(ui.n("0x4005"),0)} %", color = Txt, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(f1(ui.n("0x3710"),1) + " kg/h", color = Mut, fontSize = 9.5.sp)
                    }
                }
                val kix = ui.m.screens.getValue("palnik").bar.mapNotNull { barIx(ui, it, "palnik") } +
                        ui.m.screens.getValue("kociol").bar.filter { it.k != "anim" }.mapNotNull { barIx(ui, it, "kociol") }
                if (kix.isNotEmpty()) {
                    Box(Modifier.fillMaxWidth().height(34.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = .045f)), contentAlignment = Alignment.Center) {
                        IxRow(kix.take(6))
                    }
                }
            }
        }
        TileGrid(ui, entries, 94.dp)
    }
}

@Composable
fun Led(t: String, on: Boolean, c: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) { Box(Modifier.size(34.dp, 7.dp).background(if (on) c else Color(0xFF3A3A42), RoundedCornerShape(4.dp))); Text(t, color = Mut, fontSize = 11.sp) }
}

@Composable
fun ScreenView(ui: Ui, s: ScreenDef) {
    Card(s.title) {
        if (s.image) BoilerImage(ui)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (s.big != null) {
                if (s.state != null) Text(ui.m.statusTables[s.state]?.getOrNull(ui.n(s.stateP ?: s.big)?.toInt() ?: 0) ?: "?", color = Txt, fontSize = 30.sp)
                else BigValue(f1(ui.n(s.big)), s.bigUnit)
            }
            if (s.status != null) Text(ui.m.statusTables["ch"]?.getOrNull(ui.n(s.status)?.toInt() ?: 0) ?: "", color = Acc2, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            if (s.pill != null) Text("${f1(ui.n(s.pill), 0)}°C", color = Acc2, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp).background(Panel2, RoundedCornerShape(6.dp)).border(1.dp, Line, RoundedCornerShape(6.dp)).padding(horizontal = 9.dp, vertical = 3.dp))
            if (s.mode != null) ModeChips(ui, s.mode)
        }
        BarView(ui, s.bar)
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
            OutlinedButton({ ui.go("set:" + s.list) }) { Text("Nastawy", color = Txt) }
            if (s.sched != null) OutlinedButton({ ui.go("sched:" + s.sched) }) { Text("Harmonogram", color = Txt) }
        }
    }
    val rows = s.kv.filter { it.cond == null || (ui.n(it.cond) ?: 0.0) != 0.0 }
    Card("Wartości") { if (rows.isEmpty()) Text("Brak danych", color = Mut); for (k in rows) RowItem(k.label, f1(ui.n(k.pid), k.dec) + " " + k.unit) }
}

@Composable
fun ModeChips(ui: Ui, pid: String) {
    val cur = ui.n(pid)?.toInt(); val scope = rememberCoroutineScope()
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)) {
        for (m in ui.m.programModes) {
            val sel = cur == m.v
            Row(Modifier.background(if (sel) Acc else Panel2, RoundedCornerShape(20.dp)).border(1.dp, if (sel) Acc else Line, RoundedCornerShape(20.dp))
                .clickable { scope.launch { val e = ui.src.write(pid, m.v.toDouble()); ui.toast(e ?: "Zapisano") } }.padding(horizontal = 7.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Ic(m.icon, 16.dp); Spacer(Modifier.width(3.dp)); Text(m.label, color = if (sel) Color.Black else Mut, fontSize = 10.sp, maxLines = 1, softWrap = false, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun ListView(ui: Ui, key: String, readOnly: Boolean = false) {
    val rows = ui.m.lists[key] ?: emptyList(); var lv by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    LaunchedEffect(key) { lv = ui.src.read(rows.mapNotNull { it.pid }) }
    Card("Nastawy - " + (key.split("|").getOrNull(1) ?: key)) {
        for (r in rows) {
            val pid = r.pid ?: continue; if (r.label.isBlank()) continue
            val cur = lv[pid]; val locked = ui.level < r.levelMin
            val shown = if (cur == null) "…" else r.options?.firstOrNull { it.v == Math.round(cur).toInt() }?.label ?: f1(cur, if (ui.m.scale(pid) < 1.0) 1 else 0)
            RowItem(r.label, shown, locked, if (readOnly) null else { { ui.open(Dlg.EditRow(r, cur) { nv -> lv = lv + (pid to nv) }) } })
        }
    }
}

@Composable
fun ConfigView(ui: Ui) {
    Card("Konfiguracja kotła") {
        for (r in ui.m.config.filter { it.levelMin <= ui.level }) {
            val cur = ui.n(r.pid); RowItem(r.label, if (cur == null) "…" else r.options?.firstOrNull { it.v == Math.round(cur).toInt() }?.label ?: f1(cur, 0), false) { ui.open(Dlg.EditCfg(r)) }
        }
        if (ui.level < 1) Text("Więcej opcji po wpisaniu PIN poziomu 1", color = Mut, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
    }
    Card("Dostęp") {
        RowItem("Tryb pracy ESP", if (ui.full) "pełna kontrola" else "tylko odczyt") { ui.open(Dlg.Mode) }
        RowItem("Poziom PIN", ui.level.toString()) { ui.open(Dlg.Pin) }
        RowItem("Ustawienia aplikacji (demo / ESP)", "›") { ui.go("appset") }
    }
}

@Composable
fun SchedView(ui: Ui, id: String) {
    val c = ui.m.circuits.first { it.id == id }; var day by remember { mutableIntStateOf(0) }; val masks = remember { mutableStateMapOf<String, Int>() }; val scope = rememberCoroutineScope()
    LaunchedEffect(id) { for ((k, v) in ui.src.read(c.pids)) masks[k] = v.toLong().toInt() }
    val pid = c.pids[day]; val mask = masks[pid] ?: 0
    Card("Harmonogram - " + c.title) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for ((i, d) in ui.m.days.withIndex()) Box(Modifier.weight(1f).background(if (i == day) Acc else Panel2, RoundedCornerShape(20.dp)).clickable { day = i }.padding(vertical = 7.dp), contentAlignment = Alignment.Center) { Text(d, color = if (i == day) Color.Black else Mut, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
        }
        Text(ui.m.dayNames[day], color = Txt, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth().padding(top = 10.dp), textAlign = TextAlign.Center)
        for (row in 0 until 4) Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (col in 0 until 6) { val h = row * 6 + col; val on = (mask shr h) and 1 == 1
                Box(Modifier.weight(1f).height(44.dp).background(if (on) Acc else Panel2, RoundedCornerShape(10.dp)).border(1.dp, if (on) Acc else Line, RoundedCornerShape(10.dp)).clickable { masks[pid] = mask xor (1 shl h) }, contentAlignment = Alignment.Center) { Text("$h", color = if (on) Color.Black else Mut, fontWeight = FontWeight.SemiBold) } }
        }
        Text("pomarańczowe = komfort, szare = eko", color = Mut, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), textAlign = TextAlign.Center)
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)) {
            OutlinedButton({ for (p in c.pids) masks[p] = mask; ui.toast("Skopiowano na wszystkie dni (zapisz każdy dzień)") }) { Text("Kopiuj na wszystkie dni", color = Txt, fontSize = 12.sp) }
            Button({ if (ui.level < 1) { ui.toast("Wymagany PIN poziomu 1"); ui.open(Dlg.Pin) } else scope.launch { val e = ui.src.write(pid, mask.toLong().and(0xFFFFFF).toDouble()); ui.toast(e ?: "Zapisano") } }) { Text("Zapisz") }
        }
    }
}

@Composable
fun AlarmsView(ui: Ui) {
    var a by remember { mutableStateOf<List<AlarmRow>?>(null) }; LaunchedEffect(Unit) { a = ui.src.alarms() }
    Card("Historia alarmów") { if (a == null) Text("…", color = Mut) else if (a!!.isEmpty()) Text("Brak alarmów", color = Mut); for (r in a ?: emptyList()) RowItem("${r.n}. [${r.code}] ${r.text}", r.state) }
}

@Composable
fun NetworkView(ui: Ui) {
    var a by remember { mutableStateOf<List<NodeRow>?>(null) }; LaunchedEffect(Unit) { a = ui.src.network() }
    Card("Sieć CAN") { if (a == null) Text("…", color = Mut); for (r in a ?: emptyList()) RowItem("${r.node} - ${r.name}", r.state) }
}

@Composable
fun InfoView(ui: Ui) {
    Card("Informacje o programie") {
        RowItem("Aplikacja", "TeklaPellet 0.1"); RowItem("Źródło danych", if (ui.src.isDemo) "demo (dane przykładowe)" else "urządzenie ESP")
        RowItem("Tryb", if (ui.full) "pełna kontrola" else "tylko odczyt"); RowItem("Poziom PIN", ui.level.toString())
    }
}

@Composable
fun OutTestView(ui: Ui) {
    val rows = listOf("Pompa kotłowa" to "0x2277", "Pompa CWU" to "0x2278", "Pompa CO1" to "0x2299", "Podajnik zasobnika" to "0x2251", "Podajnik palnika" to "0x2252", "Zapalarka" to "0x2253", "Dmuchawa" to "0x2254", "Ruszt" to "0x2276", "Mieszacz CO1" to "0x22B9", "Mieszacz kotła" to "0x216F")
    Card("Test wyjść - podgląd stanów") {
        for ((l, p) in rows) RowItem(l, listOf("wyłączone", "włączone", "włączone (2)").getOrElse(ui.n(p)?.toInt() ?: 0) { "?" })
        Text("Zapis testów wyjść jest wyłączony (wymaga osobnej zgody właściciela).", color = Mut, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun NotificationsView(ui: Ui) {
    val alarm = ui.alarmActive
    Card("Powiadomienia") {
        Row(
            Modifier.fillMaxWidth().clickable { ui.go("alarms") }.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(34.dp).background(if (alarm) Color(0xFF3A1B1B) else Panel2, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) {
                Ic(78, 20.dp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (alarm) "Aktywny alarm kotła" else "Brak aktywnych alarmów", color = Txt, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(if (alarm) "Otwórz historię i potwierdź alarm." else "Stan alarmów jest prawidłowy.", color = Mut, fontSize = 12.sp)
            }
            Text("›", color = Mut, fontSize = 22.sp)
        }
    }
    Card("Aktualizacje") {
        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background(Panel2, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Text("A", color = Acc, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Nowa wersja aplikacji", color = Txt, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Sprawdzanie wersji z oficjalnego kanału main.", color = Mut, fontSize = 12.sp)
            }
        }
        HorizontalDivider(color = Line)
        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).background(Panel2, RoundedCornerShape(10.dp)), contentAlignment = Alignment.Center) { Text("ESP", color = Acc2, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Nowy firmware modułu ESP", color = Txt, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Dostępność aktualizacji będzie zgłaszana po porównaniu wersji modułu z wydaniem main.", color = Mut, fontSize = 12.sp)
            }
        }
    }
    Text(
        "Powiadomienia o aktualizacjach są oddzielone od alarmów kotła. Aktualizacja firmware korzysta z OTA modułu; aplikacja nie omija zabezpieczeń ESP.",
        color = Mut, fontSize = 11.5.sp, modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
fun LevelSep(t: String) { Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { HorizontalDivider(Modifier.weight(1f), color = Line); Text(t, color = Mut, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp); HorizontalDivider(Modifier.weight(1f), color = Line) } }

@Composable
fun MoreView(ui: Ui) {
    val items = ui.m.menu
        .filter { ui.m.menuVisible(it, ui.v, ui.level) }
        .mapNotNull { m -> ui.m.routeOf(m)?.let { it to m } }
        .filter { it.first !in listOf("cwu", "co1", "co2", "co3", "co4") }

    Card("Dostęp") {
        RowItem("Tryb pracy modułu", if (ui.full) "pełna kontrola" else "tylko odczyt") { ui.open(Dlg.Mode) }
        RowItem("Poziom PIN", ui.level.toString()) { ui.open(Dlg.Pin) }
    }

    Card("Menu kotła i instalacji") {
        val names = mapOf(1 to "POZIOM 1 — INSTALATOR", 2 to "POZIOM 2", 3 to "POZIOM 3")
        for (lv in 0..3) {
            val g = items.filter { it.second.levelMin == lv }
            if (g.isEmpty()) continue
            if (lv >= 1) LevelSep(names.getValue(lv))
            for ((r, m) in g) Row(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { ui.go(r) }.padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MenuGlyph(m.title); Spacer(Modifier.width(10.dp))
                Text(m.title, color = Txt, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Text("›", color = Mut)
            }
        }
    }

    if (ui.level >= 1) {
        Card("Poziom 1 — dodatkowe ustawienia") {
            BeepRow(ui)
        }
    }

    LevelSep("MODUŁ ESP")
    Card(null) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { ui.go("module") }.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Moduł ESP", color = Txt, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text("Wi-Fi · CAN · OTA · klienci · diagnostyka", color = Mut, fontSize = 11.sp)
            Spacer(Modifier.width(8.dp)); Text("›", color = Mut)
        }
        HorizontalDivider(color = Line)
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { ui.go("network") }.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Sieć CAN", color = Txt, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text("›", color = Mut)
        }
        HorizontalDivider(color = Line)
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { ui.go("log") }.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Dziennik modułu", color = Txt, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text("›", color = Mut)
        }
    }

    LevelSep("APLIKACJA")
    Card(null) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { ui.go("appset") }.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Ustawienia aplikacji", color = Txt, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text("PIN · demo · urządzenia · wygląd", color = Mut, fontSize = 11.sp)
            Spacer(Modifier.width(8.dp)); Text("›", color = Mut)
        }
        HorizontalDivider(color = Line)
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { ui.go("notifications") }.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Powiadomienia", color = Txt, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(if (ui.alarmActive) "alarm" else "›", color = if (ui.alarmActive) Bad else Mut)
        }
        HorizontalDivider(color = Line)
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { ui.go("info") }.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("O aplikacji", color = Txt, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text("›", color = Mut)
        }
    }
}

@Composable
fun Dialogs(ui: Ui, d: Dlg, close: () -> Unit) {
    val scope = rememberCoroutineScope(); var err by remember(d) { mutableStateOf("") }
    when (d) {
        is Dlg.Pin -> { var pin by remember { mutableStateOf("") }
            AlertDialog(onDismissRequest = close, title = { Text("PIN poziomu") }, text = { Column { OutlinedTextField(pin, { pin = it }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)); if (err.isNotEmpty()) Text(err, color = Bad, fontSize = 13.sp) } },
                confirmButton = { TextButton({ scope.launch { val e = ui.src.unlock(pin); if (e == null) { ui.toast("Poziom ${ui.src.level.value}"); close() } else err = e } }) { Text("OK") } }, dismissButton = { TextButton(close) { Text("Anuluj") } }) }
        is Dlg.Fuel -> { val fuel = ui.n("0x3601") ?: 0.0; val cap = ui.n("0x3600") ?: 0.0; val res = ui.n("0x3603") ?: 0.0; val empty = res > 0 && fuel <= res; val level = if (cap > 0) (fuel / cap).coerceIn(0.0, 1.0).toFloat() else 0f
            AlertDialog(onDismissRequest = close, title = { Text("Paliwo") }, text = { Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) { HopperIcon(level, empty, 72.dp, 86.dp); BigNum(f1(fuel, 0), "kg", 40) }
                LinearProgressIndicator(progress = { if (cap > 0) (fuel / cap).coerceIn(0.0, 1.0).toFloat() else 0f }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), color = Acc2, trackColor = Line)
                KvRow("pojemność", f1(cap, 0) + " kg"); KvRow("rezerwa", f1(res, 0) + " kg")
                Text("Zmiana poziomu paliwa z aplikacji nie jest jeszcze obsługiwana: panel nie ma zmierzonego kodowania zapisu dla tego parametru (0x3601).", color = Mut, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp)) } },
                confirmButton = { TextButton(close) { Text("Zamknij") } }) }
        is Dlg.Mode -> { var pw by remember { mutableStateOf("") }; val to = !ui.full
            AlertDialog(onDismissRequest = close, title = { Text(if (to) "Włączyć pełną kontrolę?" else "Przełączyć na tylko odczyt?") }, text = { Column { if (!ui.src.isDemo) OutlinedTextField(pw, { pw = it }, label = { Text("Hasło urządzenia") }, singleLine = true, visualTransformation = PasswordVisualTransformation()); if (err.isNotEmpty()) Text(err, color = Bad, fontSize = 13.sp) } },
                confirmButton = { TextButton({ scope.launch { val e = ui.src.setFullMode(to, pw); if (e == null) { ui.toast(if (to) "Pełna kontrola" else "Tylko odczyt"); close() } else err = e } }) { Text("OK") } }, dismissButton = { TextButton(close) { Text("Anuluj") } }) }
        is Dlg.EditRow -> { val r = d.row; val pid = r.pid!!
            if (ui.level < r.levelMin) { LaunchedEffect(Unit) { ui.toast("Wymagany PIN poziomu ${r.levelMin}"); close(); ui.open(Dlg.Pin) }; return }
            val sc = ui.m.scale(pid)
            if (r.options != null) ChooseDialog(r.label, r.options, d.cur?.let { Math.round(it).toInt() }, err, close) { v -> scope.launch { val e = ui.src.write(pid, v.toDouble()); if (e == null) { d.done(v.toDouble()); ui.toast("Zapisano"); close() } else err = e } }
            else NumberDialog(r.label, r.min * sc, r.max * sc, (r.step * sc).let { if (it <= 0.0) 1.0 else it }, d.cur ?: (r.def * sc), err, close) { v -> scope.launch { val e = ui.src.write(pid, v); if (e == null) { d.done(v); ui.toast("Zapisano"); close() } else err = e } } }
        is Dlg.EditCfg -> { val r = d.row; val cur = ui.n(r.pid)
            if (r.options != null) ChooseDialog(r.label, r.options, cur?.let { Math.round(it).toInt() }, err, close) { v -> scope.launch { val e = ui.src.write(r.pid, v.toDouble()); if (e == null) { ui.toast("Zapisano"); close() } else err = e } }
            else NumberDialog(r.label, r.min, r.max, 1.0, cur ?: r.def, err, close) { v -> scope.launch { val e = ui.src.write(r.pid, v); if (e == null) { ui.toast("Zapisano"); close() } else err = e } } }
    }
}

@Composable
fun ChooseDialog(title: String, opts: List<Opt>, cur: Int?, err: String, close: () -> Unit, pick: (Int) -> Unit) {
    AlertDialog(onDismissRequest = close, title = { Text(title) }, text = { Column { for (o in opts) Row(Modifier.fillMaxWidth().clickable { pick(o.v) }.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) { RadioButton(o.v == cur, { pick(o.v) }); Text(o.label) }; if (err.isNotEmpty()) Text(err, color = Bad, fontSize = 13.sp) } },
        confirmButton = {}, dismissButton = { TextButton(close) { Text("Anuluj") } })
}

@Composable
fun NumberDialog(title: String, lo: Double, hi: Double, step: Double, cur: Double, err: String, close: () -> Unit, save: (Double) -> Unit) {
    var v by remember { mutableFloatStateOf(min(max(cur, lo), hi).toFloat()) }
    val steps = if (step > 0) max(0, Math.round((hi - lo) / step).toInt() - 1) else 0
    AlertDialog(onDismissRequest = close, title = { Text(title) }, text = { Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(f1(v.toDouble(), if (step < 1.0) 1 else 0), fontSize = 40.sp, fontWeight = FontWeight.Light, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        Slider(v, { v = it }, valueRange = lo.toFloat()..max(hi, lo + 0.001).toFloat(), steps = steps)
        Row(Modifier.fillMaxWidth()) { Text(f1(lo, if (step < 1.0) 1 else 0), color = Mut, fontSize = 12.sp, modifier = Modifier.weight(1f)); Text(f1(hi, if (step < 1.0) 1 else 0), color = Mut, fontSize = 12.sp) }
        if (err.isNotEmpty()) Text(err, color = Bad, fontSize = 13.sp) } },
        confirmButton = { TextButton({ save(lo + Math.round((v - lo) / step) * step) }) { Text("Zapisz") } }, dismissButton = { TextButton(close) { Text("Anuluj") } })
}

@Composable
fun AppPinCard(ui: Ui) {
    var set by remember { mutableStateOf(AppLock.isSet()) }; var pin by remember { mutableStateOf("") }
    Card("PIN aplikacji") {
        Text("Blokada ekranu aplikacji przy uruchomieniu (osobna od PIN-ów kotła).", color = Mut, fontSize = 13.sp)
        OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(8) }, label = { Text("Nowy PIN (4-8 cyfr)") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button({ if (pin.length >= 4) { AppLock.set(pin); pin = ""; set = true; ui.toast("PIN ustawiony") } else ui.toast("Min. 4 cyfry") }) { Text(if (set) "Zmień PIN" else "Ustaw PIN") }
            if (set) OutlinedButton({ AppLock.clear(); set = false; ui.toast("PIN usunięty") }) { Text("Usuń", color = Txt) }
        }
    }
}

@Composable
fun LockScreen(onOk: () -> Unit) {
    var pin by remember { mutableStateOf("") }; var err by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(Bg).padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("TeklaPellet", color = Txt, fontSize = 24.sp, fontWeight = FontWeight.SemiBold); Spacer(Modifier.height(16.dp))
        OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(8) }, label = { Text("PIN aplikacji") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
        if (err.isNotEmpty()) Text(err, color = Bad, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        Button({ val e = AppLock.check(pin); if (e == null) onOk() else { err = e; pin = "" } }, Modifier.padding(top = 12.dp)) { Text("Odblokuj") }
    }
}

@Composable
fun LogView(ui: Ui) {
    var l by remember { mutableStateOf<List<String>?>(null) }; LaunchedEffect(Unit) { l = ui.src.log() }
    Card("Dziennik") { if (l == null) Text("…", color = Mut) else if (l!!.isEmpty()) Text("Brak wpisów", color = Mut); for (x in l ?: emptyList()) Text(x, color = Mut, fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace) }
}

@Composable
fun ModuleView(ui: Ui) {
    val scope = rememberCoroutineScope(); var wifi by remember { mutableStateOf("…") }; var nets by remember { mutableStateOf<List<Pair<String, Int>>?>(null) }
    var ask by remember { mutableStateOf<String?>(null) }; var ssid by remember { mutableStateOf("") }; var txt by remember { mutableStateOf("") }; var err by remember { mutableStateOf("") }
    var otaKind by remember { mutableStateOf("fw") }; var unlocked by remember { mutableStateOf(ui.src.serviceUnlocked) }; var cl by remember { mutableStateOf<List<Pair<Int, String>>>(emptyList()) }; var recCode by remember { mutableStateOf("") }; var recSaved by remember { mutableStateOf(false) }; var ipi by remember { mutableStateOf<IpInfo?>(null) }; var ipBusy by remember { mutableStateOf(false) }
    val clip = androidx.compose.ui.platform.LocalClipboardManager.current
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch { val data = withContext(kotlinx.coroutines.Dispatchers.IO) { Ctl.ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }; if (data == null) ui.toast("Nie można odczytać pliku") else { val e = ui.src.ota(otaKind, data); ui.toast(e ?: "Wgrano" ) } }
    }
    LaunchedEffect(Unit) { wifi = ui.src.wifiStatus() }
    Card("Moduł ESP") {
        RowItem("Operacje serwisowe", if (unlocked) "odblokowane ✓" else "odblokuj ›") { txt = ""; err = ""; ask = "unlock" }
        RowItem("Nazwa urządzenia", "zmień ›") { txt = ""; err = ""; ask = "name" }
        RowItem("Adres IP (DHCP / stały)", "ustaw ›") { scope.launch { ipi = ui.src.ipInfo(); if (ipi == null && !unlocked) ui.toast("Najpierw odblokuj operacje serwisowe") else if (ipi != null) { err = ""; ask = "ip" } } }
        RowItem("Wi-Fi (z wykrywaniem sieci)", wifi) { scope.launch { val n = ui.src.wifiScan(); if (n.isEmpty() && !unlocked) ui.toast("Najpierw odblokuj operacje serwisowe") else { nets = n; ask = "scan" } } }
        RowItem("Hasło urządzenia", "zmień ›") { txt = ""; err = ""; ask = "pw" }
        RowItem("Aktualizacja: firmware", "wgraj ›") { otaKind = "fw"; picker.launch("*/*") }
        RowItem("Aktualizacja: paczka UI", "wgraj ›") { otaKind = "ui"; picker.launch("*/*") }
        RowItem("Sparowani klienci", "›") { scope.launch { cl = ui.src.clients(); if (cl.isEmpty() && !unlocked) ui.toast("Najpierw odblokuj operacje serwisowe") else ask = "clients" } }
        RowItem("Kod odzyskiwania hasła", "nowy ›") { recCode = newRecoveryCode(); recSaved = false; err = ""; ask = "recovery" }
        RowItem("Dziennik", "›") { ui.go("log") }
        RowItem("Sieć CAN (moduły)", "›") { ui.go("network") }
        RowItem("Restart modułu", "›") { txt = ""; err = ""; ask = "restart" }
        RowItem("Reset fabryczny", "›") { txt = ""; err = ""; ask = "factory" }
        Text("Operacje serwisowe (Wi-Fi, hasło, nazwa, klienci, aktualizacje, restart) wymagają jednorazowego podania hasła urządzenia (ważne 5 min, aplikacja odnawia je w tle do zamknięcia). Podpisy plików sprawdza urządzenie.", color = Mut, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
    }
    when (ask) {
        "unlock", "name" -> AlertDialog(onDismissRequest = { ask = null }, title = { Text(if (ask == "unlock") "Hasło urządzenia" else "Nowa nazwa urządzenia (max 23 znaki)") }, text = { Column { OutlinedTextField(txt, { txt = it }, singleLine = true, visualTransformation = if (ask == "unlock") PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None); if (err.isNotEmpty()) Text(err, color = Bad, fontSize = 13.sp) } },
            confirmButton = { TextButton({ scope.launch { val a = ask; val e = if (a == "unlock") ui.src.serviceUnlock(txt) else ui.src.setDeviceName(txt.trim()); if (e == null) { if (a == "unlock") unlocked = true; ui.toast(if (a == "unlock") "Odblokowano" else "Zapisano"); ask = null } else err = e } }) { Text("OK") } }, dismissButton = { TextButton({ ask = null }) { Text("Anuluj") } })
        "ip" -> { val cur = ipi!!; var dh by remember { mutableStateOf(cur.dhcp) }; var a by remember { mutableStateOf(cur.ip) }; var m by remember { mutableStateOf(cur.mask) }; var g by remember { mutableStateOf(cur.gw) }; var d1 by remember { mutableStateOf(cur.dns1) }; var d2 by remember { mutableStateOf(cur.dns2) }
            AlertDialog(onDismissRequest = { if (!ipBusy) ask = null }, title = { Text("Adres IP modułu") }, text = { Column {
                Row(verticalAlignment = Alignment.CenterVertically) { Text("Automatycznie (DHCP)", color = Txt, modifier = Modifier.weight(1f)); Switch(dh, { dh = it }) }
                if (!dh) for ((l, v, set) in listOf(Triple("Adres IP", a, { x: String -> a = x }), Triple("Maska", m, { x: String -> m = x }), Triple("Brama", g, { x: String -> g = x }), Triple("DNS 1", d1, { x: String -> d1 = x }), Triple("DNS 2 (opcjonalnie)", d2, { x: String -> d2 = x })))
                    OutlinedTextField(v, set, label = { Text(l) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Po zmianie aplikacja połączy się z nowym adresem i potwierdzi go (do 60 s). Bez potwierdzenia moduł wraca do poprzednich ustawień.", color = Mut, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                if (err.isNotEmpty()) Text(err, color = Bad, fontSize = 13.sp) } },
                confirmButton = { TextButton({ ipBusy = true; err = ""; scope.launch { val e = ui.src.ipApply(IpInfo(dh, a.trim(), m.trim(), g.trim(), d1.trim(), d2.trim())); ipBusy = false; if (e == null) { ui.toast("Adres zmieniony i potwierdzony"); ask = null } else err = e } }, enabled = !ipBusy) { Text(if (ipBusy) "Zmieniam…" else "Zastosuj") } }, dismissButton = { TextButton({ ask = null }, enabled = !ipBusy) { Text("Anuluj") } }) }
        "clients" -> AlertDialog(onDismissRequest = { ask = null }, title = { Text("Sparowani klienci") }, text = { Column { for ((i, n) in cl) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(n, color = Txt, modifier = Modifier.weight(1f)); TextButton({ scope.launch { val e = ui.src.revokeClient(i); if (e == null) cl = ui.src.clients() else ui.toast(e) } }) { Text("Usuń", color = Bad) } }; if (cl.isEmpty()) Text("Brak") } }, confirmButton = {}, dismissButton = { TextButton({ ask = null }) { Text("Zamknij") } })
        "recovery" -> AlertDialog(onDismissRequest = { ask = null }, title = { Text("Kod odzyskiwania hasła") }, text = { Column {
            Text(recCode, color = Acc2, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(if (recSaved) "Zapisano w module. Zapisz ten kod w bezpiecznym miejscu - nie będzie już widoczny; poprzedni kod przestał działać." else "Nowy kod zastąpi poprzedni. Najpierw zapisz go w module, potem zanotuj. Pozwala ustawić nowe hasło bez fizycznego dostępu.", color = Mut, fontSize = 13.sp)
            if (err.isNotEmpty()) Text(err, color = Bad, fontSize = 13.sp) } },
            confirmButton = { if (!recSaved) TextButton({ scope.launch { val e = ui.src.setRecovery(recCode); if (e == null) recSaved = true else err = e } }) { Text("Zapisz w module") } else TextButton({ clip.setText(androidx.compose.ui.text.AnnotatedString(recCode)) }) { Text("Kopiuj") } }, dismissButton = { TextButton({ ask = null }) { Text("Zamknij") } })
        "scan" -> AlertDialog(onDismissRequest = { ask = null }, title = { Text("Wi-Fi") }, text = { Column { for ((n, r) in nets ?: emptyList()) Row(Modifier.fillMaxWidth().clickable { ssid = n; txt = ""; err = ""; ask = "wifipw" }.padding(vertical = 10.dp)) { Text(n, modifier = Modifier.weight(1f)); Text("$r dBm", color = Mut) }; if (nets.isNullOrEmpty()) Text("Brak sieci") } }, confirmButton = {}, dismissButton = { TextButton({ ask = null }) { Text("Zamknij") } })
        "wifipw", "pw", "restart", "factory" -> {
            val title = when (ask) { "wifipw" -> "Hasło sieci $ssid"; "pw" -> "Nowe hasło (min. 8 znaków)"; "restart" -> "Restart - podaj hasło urządzenia"; else -> "Reset fabryczny kasuje hasło, sieć i ustawienia modułu. Podaj hasło" }
            AlertDialog(onDismissRequest = { ask = null }, title = { Text(title) }, text = { Column { OutlinedTextField(txt, { txt = it }, singleLine = true, visualTransformation = PasswordVisualTransformation()); if (err.isNotEmpty()) Text(err, color = Bad, fontSize = 13.sp) } },
                confirmButton = { TextButton({ scope.launch { val a = ask; val e = when (a) { "wifipw" -> ui.src.wifiSave(ssid, txt); "pw" -> ui.src.changePassword(txt); "restart" -> ui.src.restart(txt); else -> ui.src.factoryReset(txt) }; if (e == null) { ui.toast("Wykonano"); ask = null } else err = e } }) { Text("OK") } }, dismissButton = { TextButton({ ask = null }) { Text("Anuluj") } })
        }
    }
}

/** Wiersz menu panelu "Alarm dźwiękowy" (przełącznik, PID 0x3506, poziom 1). */
@Composable
fun BeepRow(ui: Ui) {
    var on by remember { mutableStateOf<Boolean?>(null) }; val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { on = ui.src.read(listOf("0x3506"))["0x3506"]?.let { it != 0.0 } }
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Alarm dźwiękowy", color = Txt, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(on == true, { v -> scope.launch { val e = ui.src.write("0x3506", if (v) 1.0 else 0.0); if (e == null) on = v else ui.toast(e) } }, enabled = on != null)
    }
}
