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
    class EditRow(val row: SetRow, val cur: Double?, val done: (Double) -> Unit) : Dlg()
    class EditCfg(val row: CfgRow) : Dlg()
}

class Ui(val src: DataSource, val v: Map<String, Double>, val level: Int, val full: Boolean, val go: (String) -> Unit, val open: (Dlg) -> Unit, val toast: (String) -> Unit) {
    val m get() = Ctl.model
    fun n(p: String?): Double? = if (p == null) null else v[p]
}

fun f1(x: Double?, d: Int = 1): String = if (x == null || x.isNaN()) "--" else if (d == 0) Math.round(x).toString() else String.format(java.util.Locale.US, "%.${d}f", x)
val PROGICON = listOf(133, 135, 122, 128, 120)
val TABS = listOf(Triple("home", "Pulpit", "⌂"), Triple("co1", "Ogrzewanie", "▥"), Triple("sched:co1", "Harmonogram", "▦"), Triple("alarms", "Alarmy", "🔔"), Triple("more", "Menu", "≡"))

fun fuelDots(fuel: Double, cap: Double, res: Double): Pair<Int, Boolean> {
    if (cap <= 0) return 0 to false
    if (res > 0 && fuel <= res) return 0 to true
    val pm = Math.floor(fuel * 1000 / cap).toInt(); val th = intArrayOf(80, 250, 380, 570, 740, 870); val dd = intArrayOf(2, 5, 9, 14, 19, 24)
    for (i in 5 downTo 0) if (pm >= th[i]) return dd[i] to false
    return 0 to false
}

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
        var alarmCount by remember { mutableIntStateOf(0) }
        LaunchedEffect(src) { while (true) { alarmCount = src.alarms().size; delay(15000) } }
        fun go(s: String) { if (s == "home") { stack.clear(); stack.add("home") } else if (stack.last() != s) stack.add(s) }
        BackHandler(stack.size > 1) { stack.removeAt(stack.lastIndex) }
        val ui = Ui(src, vals, level, full, ::go, { dlg = it }, { Toast.makeText(ctx, it, Toast.LENGTH_SHORT).show() })
        val screen = stack.last()
        CompositionLocalProvider(LocalFrame provides frame) {
            Scaffold(
                containerColor = Bg,
                topBar = { TopBar(ui, screen, conn, src.isDemo) },
                bottomBar = {
                    NavigationBar(containerColor = Color(0xFF101014)) {
                        for ((id, t, ic) in TABS) NavigationBarItem(selected = screen == id, onClick = { go(id) }, icon = { BadgedBox(badge = { if (id == "alarms" && alarmCount > 0) Badge(containerColor = Acc, contentColor = Color.Black) { Text("$alarmCount") } }) { Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) { val n = ic.toIntOrNull(); if (n != null) Ic(n, 24.dp) else Text(ic, fontSize = 20.sp) } } }, label = { Text(t, fontSize = 9.sp, maxLines = 1, softWrap = false) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = Acc, selectedTextColor = Acc, indicatorColor = Panel2, unselectedIconColor = Mut, unselectedTextColor = Mut))
                    }
                },
            ) { pad ->
                Column(Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    key(screen) { Body(ui, screen) }
                }
            }
            dlg?.let { Dialogs(ui, it) { dlg = null } }
        }
    }
}

@Composable
fun TopBar(ui: Ui, screen: String, conn: Boolean, demo: Boolean) {
    val title = when {
        screen == "home" -> "Pulpit"; screen == "more" -> "Więcej"; screen == "module" -> "Moduł ESP"; screen == "log" -> "Dziennik"; screen == "appset" -> "Ustawienia aplikacji"
        screen.startsWith("set:") -> "Nastawy"; screen.startsWith("sched:") -> "Harmonogram"
        else -> ui.m.screens[screen]?.title ?: ui.m.menu.firstOrNull { ui.m.routeOf(it) == screen }?.title ?: screen
    }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    Row(Modifier.fillMaxWidth().background(Color(0xFF101014)).statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Txt, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (screen == "home") Text(java.text.SimpleDateFormat("EEE, d MMM · HH:mm", java.util.Locale("pl")).format(now), color = Mut, fontSize = 12.sp, maxLines = 1)
        }
        Chip("", if (demo) Warn else if (conn) Okc else Bad) { ui.go("appset") }
        Chip(if (ui.full) "Pełna kontrola" else "Tylko odczyt", if (ui.full) Warn else Okc) { ui.open(Dlg.Mode) }
        Chip("PIN ${ui.level}", Mut) { ui.open(Dlg.Pin) }
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
    Column(modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).border(1.dp, Line, RoundedCornerShape(16.dp)).padding(14.dp)) {
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
fun Body(ui: Ui, screen: String) {
    when {
        screen == "home" -> HomeView(ui)
        screen == "more" -> MoreView(ui)
        screen == "appset" -> AppSettings(ui)
        screen == "config" -> ConfigView(ui)
        screen == "alarms" -> AlarmsView(ui)
        screen == "info" -> InfoView(ui)
        screen == "stats" -> ListView(ui, "0x11|STATYSTYKI|inst1", readOnly = true)
        screen == "network" -> NetworkView(ui)
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

@Composable
fun Dot(on: Boolean, c: Color = Okc) {
    Box(Modifier.size(10.dp).background(if (on) c else Color(0xFF2A2A30), RoundedCornerShape(5.dp)).border(1.dp, if (on) c else Color(0xFF3A3A42), RoundedCornerShape(5.dp)))
}

/** Wskaźnik: etykieta + dioda (świeci wg stanu wyjścia, przygaszona gdy wyłączone). Mieszacz: 1 = zamykanie (niebieska), 2 = otwieranie (czerwona). */
@Composable
fun Ind(ui: Ui, label: String, pid: String, kind: String = "on") {
    val v = ui.n(pid)?.toInt() ?: 0
    val c = when (kind) { "mix" -> if (v == 1) Color(0xFF4D9BFF) else Color(0xFFFF4D4D); "burn" -> Color(0xFFF2C200); else -> Okc }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) { Dot(v != 0, c); Text(label, color = Mut, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
fun KvRow(l: String, v: String) {
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text(l, color = Mut, fontSize = 13.sp, modifier = Modifier.weight(1f)); Text(v, color = Txt, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1) }
}

@Composable
fun HomeCard(title: String, big: String, unit: String, modifier: Modifier, go: String, ui: Ui, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.background(Panel, RoundedCornerShape(16.dp)).border(1.dp, Line, RoundedCornerShape(16.dp)).clickable { ui.go(go) }.padding(12.dp)) {
        Text(title.uppercase(), color = Mut, fontSize = 11.sp, letterSpacing = 0.9.sp, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 4.dp)) { Text(big, color = Txt, fontSize = 24.sp, fontWeight = FontWeight.Light); if (unit.isNotEmpty()) Text(unit, color = Mut, fontSize = 12.sp, modifier = Modifier.padding(start = 3.dp, bottom = 3.dp)) }
        content()
    }
}

@Composable
fun HomeView(ui: Ui) {
    // Pulpit: łuk z temperaturą kotła + wartości obok, karty CO / CWU / Palnik / Kocioł ze wskaźnikami (dioda), ostatni alarm.
    val t = ui.n("0x2001"); val set = ui.n("0x2004"); val bs = ui.n("0x2007")?.toInt() ?: 0; val on = ui.n("0x2005") != 0.0
    val nco = ui.n("0x2105")?.toInt() ?: 0; val cwu = (ui.n("0x2106") ?: 0.0) != 0.0
    val frac = (((t ?: 20.0) - 20.0) / 70.0).coerceIn(0.0, 1.0).toFloat()
    var alarms by remember { mutableStateOf<List<AlarmRow>>(emptyList()) }; LaunchedEffect(Unit) { alarms = ui.src.alarms() }
    fun mode(p: String) = ui.m.programModes.firstOrNull { it.v == (ui.n(p)?.toInt() ?: -1) }?.label ?: "--"
    Card(null) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(0.95f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().padding(6.dp)) {
                    val sw = size.width * 0.065f; val d = size.minDimension - sw
                    drawArc(Line, 135f, 270f, false, Offset(sw / 2, sw / 2), Size(d, d), style = Stroke(sw, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    drawArc(Brush.sweepGradient(listOf(Warn, Acc, Warn)), 135f, 270f * frac.coerceAtLeast(0.01f), false, Offset(sw / 2, sw / 2), Size(d, d), style = Stroke(sw, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.Top) { Text(f1(t), color = Txt, fontSize = 38.sp, fontWeight = FontWeight.Light); Text("°C", color = Acc, fontSize = 14.sp, modifier = Modifier.padding(start = 2.dp, top = 4.dp)) }
                    Text(ui.m.statusTables["burner"]?.getOrNull(bs) ?: "?", color = Acc2, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text("zadana ${f1(set, 0)} °C", color = Mut, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
            Column(Modifier.weight(1.05f)) {
                KvRow("Powrót", f1(ui.n("0x4000")) + " °C"); KvRow("Temp. palnika", f1(ui.n("0x226C")) + " °C"); KvRow("Temp. zewnętrzna", f1(ui.n("0x215F")) + " °C"); KvRow("Moc palnika", f1(ui.n("0x4005"), 0) + " %")
            }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (nco >= 1) HomeCard("Ogrzewanie 1", f1(ui.n("0x4001")), "°C", Modifier.weight(1f), "co1", ui) {
            KvRow("zadana", f1(ui.n("0x22F5"), 1) + " °C"); KvRow("tryb", mode("0x2064"))
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { Ind(ui, "Pompa", "0x2299"); Ind(ui, "Mieszacz", "0x22B9", "mix") }
        }
        if (cwu) HomeCard("Ciepła woda", f1(ui.n("0x2110")), "°C", Modifier.weight(1f), "cwu", ui) {
            KvRow("zadana", f1(ui.n("0x201A"), 1) + " °C"); KvRow("tryb", mode("0x2014"))
            Row(Modifier.padding(top = 10.dp)) { Ind(ui, "Pompa", "0x2278") }
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        HomeCard("Palnik", ui.m.statusTables["burner"]?.getOrNull(bs) ?: "?", "", Modifier.weight(1f), "palnik", ui) {
            KvRow("jasność płomienia", f1(ui.n("0x2250"), 0) + " %")
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Ind(ui, "Dmuchawa", "0x2254", "burn"); Ind(ui, "Podajnik", "0x2252", "burn") }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Ind(ui, "Zapalarka", "0x2253", "burn"); Ind(ui, "Ruszt", "0x2276") }
            }
        }
        val fuel = ui.n("0x3601") ?: 0.0; val cap = ui.n("0x3600") ?: 0.0
        HomeCard("Paliwo", f1(fuel, 0), "kg z ${f1(cap, 0)}", Modifier.weight(1f), "magazyn", ui) {
            Box(Modifier.fillMaxWidth().padding(top = 8.dp).height(8.dp).background(Line, RoundedCornerShape(4.dp))) { Box(Modifier.fillMaxWidth(if (cap > 0) (fuel / cap).coerceIn(0.03, 1.0).toFloat() else 0f).fillMaxHeight().background(Brush.horizontalGradient(listOf(Acc, Warn)), RoundedCornerShape(4.dp))) }
            KvRow("rezerwa", f1(ui.n("0x3603"), 0) + " kg")
        }
    }
    Column(Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp)).border(1.dp, Color(0xFF5A2A2A), RoundedCornerShape(16.dp)).clickable { ui.go("alarms") }.padding(14.dp)) {
        Text("OSTATNI ALARM", color = Mut, fontSize = 11.sp, letterSpacing = 0.9.sp, fontWeight = FontWeight.SemiBold)
        val a = alarms.firstOrNull()
        if (a == null) Text("Brak alarmów", color = Txt, fontSize = 18.sp, modifier = Modifier.padding(top = 6.dp))
        else { Row(Modifier.padding(top = 6.dp)) { Text("kod ${a.code}", color = Bad, fontSize = 18.sp, fontWeight = FontWeight.Bold); Text(" · ${a.text}", color = Txt, fontSize = 18.sp) }; KvRow("w historii ${alarms.size} zdarzeń", if (a.state == "AKTYWNY") "AKTYWNY" else "nie aktywny") }
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
fun MoreView(ui: Ui) {
    val items = ui.m.menu.filter { ui.m.menuVisible(it, ui.v, ui.level) }.mapNotNull { m -> ui.m.routeOf(m)?.let { it to m } }.filter { it.first !in listOf("kociol", "palnik", "cwu") }
    Card("Więcej") {
        for ((r, m) in items) Row(Modifier.fillMaxWidth().clickable { ui.go(r) }.padding(vertical = 11.dp, horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) { Ic(m.icon, 22.dp); Spacer(Modifier.width(10.dp)); Text(m.title, color = Txt, fontSize = 15.sp, modifier = Modifier.weight(1f)); Text("›", color = Mut) }
        Row(Modifier.fillMaxWidth().clickable { ui.go("module") }.padding(vertical = 11.dp, horizontal = 4.dp)) { Text("Moduł ESP (Wi-Fi, hasło, aktualizacja)", color = Txt, fontSize = 15.sp, modifier = Modifier.weight(1f)); Text("›", color = Mut) }
        Row(Modifier.fillMaxWidth().clickable { ui.go("appset") }.padding(vertical = 11.dp, horizontal = 4.dp)) { Text("Ustawienia aplikacji (demo / ESP)", color = Txt, fontSize = 15.sp, modifier = Modifier.weight(1f)); Text("›", color = Mut) }
    }
}

@Composable
fun AppSettings(ui: Ui) {
    var host by remember { mutableStateOf(Ctl.host) }; var pw by remember { mutableStateOf("") }; var err by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }; val scope = rememberCoroutineScope()
    Card("Źródło danych") {
        Text(if (ui.src.isDemo) "Teraz: tryb demo - dane przykładowe, bez urządzenia. W Konfiguracji możesz ustawić obwody, CWU, bufor itp.; PIN-y demo: 1111 / 2222 / 3333." else "Teraz: połączono z urządzeniem ESP.", color = Mut, fontSize = 13.sp)
        Spacer(Modifier.height(10.dp))
        OutlinedButton({ Ctl.useDemo(); ui.go("home") }, Modifier.fillMaxWidth()) { Text("Użyj trybu demo", color = Txt) }
    }
    AppPinCard(ui)
    Card("Połącz z urządzeniem ESP") {
        OutlinedTextField(host, { host = it }, label = { Text("Adres (np. teklapellet.local lub 192.168.1.50)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(pw, { pw = it }, label = { Text("Hasło urządzenia") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
        if (err.isNotEmpty()) Text(err, color = Bad, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        Button({ busy = true; err = ""; scope.launch { val rs = RemoteSource(host.trim(), Ctl.scope); val e = rs.login(pw); busy = false; if (e == null) { Ctl.host = host.trim(); Ctl.use(rs); ui.go("home") } else { rs.close(); err = e } } }, Modifier.fillMaxWidth().padding(top = 10.dp), enabled = !busy && host.isNotBlank() && pw.isNotEmpty()) { Text(if (busy) "Łączenie…" else "Połącz") }
        Text("Zapis do kotła wymaga trybu „pełna kontrola” (hasło) i PIN-u. Domyślnie urządzenie działa tylko do odczytu.", color = Mut, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun Dialogs(ui: Ui, d: Dlg, close: () -> Unit) {
    val scope = rememberCoroutineScope(); var err by remember(d) { mutableStateOf("") }
    when (d) {
        is Dlg.Pin -> { var pin by remember { mutableStateOf("") }
            AlertDialog(onDismissRequest = close, title = { Text("PIN poziomu") }, text = { Column { OutlinedTextField(pin, { pin = it }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)); if (err.isNotEmpty()) Text(err, color = Bad, fontSize = 13.sp) } },
                confirmButton = { TextButton({ scope.launch { val e = ui.src.unlock(pin); if (e == null) { ui.toast("Poziom ${ui.src.level.value}"); close() } else err = e } }) { Text("OK") } }, dismissButton = { TextButton(close) { Text("Anuluj") } }) }
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
    var otaKind by remember { mutableStateOf("fw") }
    val picker = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch { val data = withContext(kotlinx.coroutines.Dispatchers.IO) { Ctl.ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() } }; if (data == null) ui.toast("Nie można odczytać pliku") else { val e = ui.src.ota(otaKind, data); ui.toast(e ?: "Wgrano" ) } }
    }
    LaunchedEffect(Unit) { wifi = ui.src.wifiStatus() }
    Card("Moduł ESP") {
        RowItem("Wi-Fi", wifi) { scope.launch { nets = ui.src.wifiScan(); ask = "scan" } }
        RowItem("Hasło urządzenia", "zmień ›") { txt = ""; err = ""; ask = "pw" }
        RowItem("Aktualizacja: firmware", "wgraj ›") { otaKind = "fw"; picker.launch("*/*") }
        RowItem("Aktualizacja: paczka UI", "wgraj ›") { otaKind = "ui"; picker.launch("*/*") }
        RowItem("Dziennik", "›") { ui.go("log") }
        RowItem("Sieć CAN (moduły)", "›") { ui.go("network") }
        RowItem("Restart modułu", "›") { txt = ""; err = ""; ask = "restart" }
        RowItem("Reset fabryczny", "›") { txt = ""; err = ""; ask = "factory" }
        Text("Zapis (Wi-Fi, hasło, aktualizacje) wymaga trybu „pełna kontrola”; podpisy plików sprawdza urządzenie.", color = Mut, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
    }
    when (ask) {
        "scan" -> AlertDialog(onDismissRequest = { ask = null }, title = { Text("Wi-Fi") }, text = { Column { for ((n, r) in nets ?: emptyList()) Row(Modifier.fillMaxWidth().clickable { ssid = n; txt = ""; err = ""; ask = "wifipw" }.padding(vertical = 10.dp)) { Text(n, modifier = Modifier.weight(1f)); Text("$r dBm", color = Mut) }; if (nets.isNullOrEmpty()) Text("Brak sieci") } }, confirmButton = {}, dismissButton = { TextButton({ ask = null }) { Text("Zamknij") } })
        "wifipw", "pw", "restart", "factory" -> {
            val title = when (ask) { "wifipw" -> "Hasło sieci $ssid"; "pw" -> "Nowe hasło (min. 8 znaków)"; "restart" -> "Restart - podaj hasło urządzenia"; else -> "Reset fabryczny kasuje hasło, sieć i ustawienia modułu. Podaj hasło" }
            AlertDialog(onDismissRequest = { ask = null }, title = { Text(title) }, text = { Column { OutlinedTextField(txt, { txt = it }, singleLine = true, visualTransformation = PasswordVisualTransformation()); if (err.isNotEmpty()) Text(err, color = Bad, fontSize = 13.sp) } },
                confirmButton = { TextButton({ scope.launch { val a = ask; val e = when (a) { "wifipw" -> ui.src.wifiSave(ssid, txt); "pw" -> ui.src.changePassword(txt); "restart" -> ui.src.restart(txt); else -> ui.src.factoryReset(txt) }; if (e == null) { ui.toast("Wykonano"); ask = null } else err = e } }) { Text("OK") } }, dismissButton = { TextButton({ ask = null }) { Text("Anuluj") } })
        }
    }
}
