package pl.tekla.pellet

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
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

data class UpdateNotice(val kind:String,val title:String,val current:String,val latest:String,val changelog:String,val downloadUrl:String?)

object UpdateCenter {
    private const val MANIFEST_URL = "https://raw.githubusercontent.com/GieOeRZet/TeklaPelletCANv2/main/update_manifest.json"
    private val http=OkHttpClient.Builder().connectTimeout(4,TimeUnit.SECONDS).readTimeout(6,TimeUnit.SECONDS).build()
    val notices=mutableStateListOf<UpdateNotice>()
    private var started=false
    private var lastCheck=0L
    fun start(){if(started)return;started=true;refresh()}
    fun refresh(){
        val now=System.currentTimeMillis();if(now-lastCheck<10*60*1000L)return;lastCheck=now
        Ctl.scope.launch(Dispatchers.Main.immediate){
            val found=withContext(Dispatchers.IO){
                try{
                    val req=Request.Builder().url(MANIFEST_URL).header("Cache-Control","no-cache").build()
                    http.newCall(req).execute().use{r->
                        if(!r.isSuccessful)return@use emptyList<UpdateNotice>()
                        val root=JSONObject(r.body?.string()?:"{}");val out=mutableListOf<UpdateNotice>()
                        val app=root.optJSONObject("app");val av=app?.optString("version").orEmpty()
                        val currentApp=Ctl.ctx.packageManager.getPackageInfo(Ctl.ctx.packageName,0).versionName ?: "0.1"
                        if(av.isNotEmpty()&&av!=currentApp)out+=UpdateNotice("app","Nowa wersja aplikacji",currentApp,av,app.optString("changelog"),app.optString("downloadUrl").ifEmpty{null})
                        val fw=root.optJSONObject("firmware");val fv=fw?.optString("version").orEmpty();val cur=Ctl.firmwareVersion
                        if(fv.isNotEmpty()&&!cur.isNullOrEmpty()&&cur!="demo"&&fv!=cur)out+=UpdateNotice("firmware","Nowy firmware modułu ESP",cur,fv,fw.optString("changelog"),fw.optString("downloadUrl").ifEmpty{null})
                        out
                    }
                }catch(_:Exception){emptyList()}
            }
            notices.clear();notices.addAll(found)
        }
    }
}
val PROGICON = listOf(133, 135, 122, 128, 120)
fun navTabs(v: Map<String, Double>): List<Triple<String, String, String>> {
    // Dolna belka jest skrótem nawigacyjnym, nie kopią całego menu.
    // CO jest jednym wejściem (jak w panelu); CO2..CO4 pozostają dostępne z Menu.
    val out = mutableListOf(Triple("kociol", "Kocioł", "83"), Triple("palnik", "Palnik", "85"))
    val co = (v["0x2105"] ?: 0.0).toInt().coerceIn(0, 4)
    if (co > 0) out += Triple("co1", "CO", "86")
    if ((v["0x2106"] ?: 0.0) != 0.0) out += Triple("cwu", "CWU", "88")
    out += Triple("power", "Power", "115")
    out += Triple("more", "Menu", "96")
    return out
}

fun fuelDots(fuel: Double, cap: Double, res: Double): Pair<Int, Boolean> {
    if (cap <= 0) return 0 to false
    if (res > 0 && fuel <= res) return 0 to true
    val pm = Math.floor(fuel * 1000 / cap).toInt(); val th = intArrayOf(80, 250, 380, 570, 740, 870); val dd = intArrayOf(2, 5, 9, 14, 19, 24)
    for (i in 5 downTo 0) if (pm >= th[i]) return dd[i] to false
    return 0 to false
}

@Composable
fun NotificationIcon(hasNew:Boolean=false) {
    val c=if(hasNew)Acc else Mut
    Canvas(Modifier.size(24.dp)) {
        val p=androidx.compose.ui.graphics.Path().apply {
            moveTo(7f,17f);lineTo(17f,17f);lineTo(15.8f,15f);lineTo(15.8f,10.5f)
            cubicTo(15.8f,7.2f,14.2f,5.5f,12f,5.2f);cubicTo(9.8f,5.5f,8.2f,7.2f,8.2f,10.5f)
            lineTo(8.2f,15f);close()
        }
        drawPath(p,c,style=Stroke(1.8f));drawLine(c,Offset(6f,18f),Offset(18f,18f),1.8f);drawCircle(c,1.3f,Offset(12f,20f))
    }
}
@Composable
fun BottomNavIcon(id: String, selected: Boolean) {
    val icon = when (id) {
        "kociol" -> 83
        "palnik" -> 85
        "co1", "co2", "co3", "co4" -> 86
        "cwu" -> 88
        "solary" -> 87
        "bufor" -> 84
        "kaskada" -> 82
        "magazyn" -> 99
        "more" -> 96
        else -> 96
    }
    Ic(icon, 24.dp)
}

@Composable
fun TeklaApp() {
    TeklaTheme {
        if (AppLock.isSet() && !Ctl.appUnlocked) { LockScreen { Ctl.appUnlocked = true }; return@TeklaTheme }
        val src = Ctl.source ?: return@TeklaTheme
        val ctx = Ctl.ctx
        val vals by src.values.collectAsState(); val level by src.level.collectAsState(); val full by src.fullMode.collectAsState(); val conn by src.connected.collectAsState(); val tabs = navTabs(vals)
        var frame by remember { mutableIntStateOf(0) }
        LaunchedEffect(Unit) { UpdateCenter.start(); while (true) { delay(130); frame++ } }
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
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF17232D), Color(0xFF0F1820), Color(0xFF16212A))
                    )
                )
            ) {
            Scaffold(
                containerColor = Color.Transparent,
                topBar = { TopBar(ui, screen, conn, src.isDemo) },
                bottomBar = {
                    NavigationBar(containerColor = GlassBar, tonalElevation = 0.dp) {
                        for ((id, t, _) in tabs) {
                            NavigationBarItem(
                                selected = screen == id || (id == "co1" && screen.startsWith("co")),
                                onClick = { go(id) },
                                icon = { BottomNavIcon(id, screen == id) },
                                label = { Text(t, fontSize = 9.sp, maxLines = 1, softWrap = false) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Acc,
                                    selectedTextColor = Acc,
                                    indicatorColor = Glass2,
                                    unselectedIconColor = Mut,
                                    unselectedTextColor = Mut
                                )
                            )
                        }
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
}

@Composable
fun TopBar(ui: Ui, screen: String, conn: Boolean, demo: Boolean) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    if (screen == "home") {
        Row(
            Modifier.fillMaxWidth().background(GlassBar).statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Text("🔥", fontSize = 18.sp)
                Column {
                    Text(if (demo) "TeklaPellet" else Ctl.activeName(), color = Txt, fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(
                        if (demo) "Demo" else if (conn) "Połączono" else "Offline",
                        color = if (demo) Warn else if (conn) Okc else Bad,
                        fontSize = 9.sp
                    )
                }
            }
            Box(
                Modifier.clickable { ui.go("notifications") }.padding(4.dp)
            ) {
                NotificationIcon(ui.alarmActive || UpdateCenter.notices.isNotEmpty())
                if (ui.alarmActive || UpdateCenter.notices.isNotEmpty())
                    Box(Modifier.size(6.dp).background(if (ui.alarmActive) Bad else Acc2, RoundedCornerShape(4.dp)).align(Alignment.TopEnd))
            }
        }
        return
    }
    val title = when {
        screen == "more" -> "Menu"
        screen == "module" -> "Moduł ESP"
        screen == "log" -> "Dziennik"
        screen == "appset" -> "Ustawienia aplikacji"
        screen == "notifications" -> "Powiadomienia"
        screen.startsWith("set:") -> "Nastawy"
        screen.startsWith("sched:") -> "Harmonogram"
        else -> ui.m.screens[screen]?.title ?: ui.m.menu.firstOrNull { ui.m.routeOf(it) == screen }?.title ?: screen
    }
    Row(
        Modifier.fillMaxWidth().background(GlassBar).statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "‹",
            color = Txt, fontSize = 38.sp, fontWeight = FontWeight.Light,
            modifier = Modifier.clickable { ui.go("home") }.padding(horizontal = 8.dp)
        )
        Text(
            title, color = Txt, fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
            maxLines = 1, modifier = Modifier.weight(1f), textAlign = TextAlign.Center
        )
        Box(Modifier.clickable { ui.go("notifications") }.padding(5.dp)) {
            Ic(78, 22.dp)
            if (ui.alarmActive || UpdateCenter.notices.isNotEmpty())
                Box(Modifier.size(6.dp).background(if (ui.alarmActive) Bad else Acc2, RoundedCornerShape(4.dp)).align(Alignment.TopEnd))
        }
        Spacer(Modifier.width(6.dp))
        Spacer(Modifier.width(28.dp))
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
        screen == "notifications" -> NotificationView(ui)
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
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Ic(id, 126.dp) }
}

@Composable
fun BarView(ui: Ui, cells: List<Cell>) {
    if (cells.isEmpty()) return
    val frame = LocalFrame.current
    Row(Modifier.fillMaxWidth().padding(top = 7.dp).background(Glass2, RoundedCornerShape(14.dp))
        .border(1.dp, Line, RoundedCornerShape(14.dp)).padding(7.dp).heightIn(min = 50.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
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
val CardBrush = Brush.verticalGradient(listOf(Color(0xB82E3A45), Color(0x8F1D2933)))

/** Karta/kafel z cienką linią LED przy górnej krawędzi (kolor stanu; gdy null - wygaszona). */
@Composable
fun GlowCard(modifier: Modifier, led: Color?, click: (() -> Unit)?, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.clip(CardShape).background(CardBrush).border(1.dp, Line, CardShape).then(if (click != null) Modifier.clickable(onClick = click) else Modifier)) {
        Box(
    Modifier.align(Alignment.TopCenter).fillMaxWidth(0.64f).height(2.dp)
        .background(Brush.horizontalGradient(listOf(Color.Transparent, led ?: Color(0x7890A0AA), Color.Transparent)))
)
        Column(Modifier.fillMaxSize().padding(horizontal = 13.dp, vertical = 11.dp), content = content)
    }
}

@Composable
fun Tile(title: String, modeIcon: Int?, led: Color?, modifier: Modifier, go: () -> Unit, ixs: List<IxData>, value: @Composable () -> Unit) {
    GlowCard(modifier.heightIn(min = 100.dp), led, go) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(title.uppercase(), color = Mut, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp, modifier = Modifier.weight(1f)); if (modeIcon != null) Ic(modeIcon, 17.dp) }
        Spacer(Modifier.height(4.dp)); value(); Spacer(Modifier.weight(1f).heightIn(min = 6.dp)); if (ixs.isNotEmpty()) IxRow(ixs)
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

/** Zbiornik paliwa: wizualizacja jak przezroczysta szklanka z poziomem cieczy. */
@Composable
fun FuelGlassIcon(fuel: Double, cap: Double, empty: Boolean, w: androidx.compose.ui.unit.Dp = 42.dp, h: androidx.compose.ui.unit.Dp = 50.dp) {
    Canvas(Modifier.size(w, h)) {
        val sc = size.width / 42f
        val left = 5f * sc; val top = 3f * sc; val width = 32f * sc; val height = 44f * sc
        val pct = if (cap > 0) (fuel / cap).coerceIn(0.0, 1.0).toFloat() else 0f
        drawRoundRect(Color(0xFF111318).copy(alpha = 0.72f), Offset(left, top), Size(width, height), androidx.compose.ui.geometry.CornerRadius(7f * sc))
        if (!empty && pct > 0f) {
            val liquidH = (height - 3f * sc) * pct
            val liquidTop = top + height - liquidH
            drawRoundRect(Acc2.copy(alpha = 0.16f), Offset(left + 1.5f * sc, liquidTop), Size(width - 3f * sc, liquidH), androidx.compose.ui.geometry.CornerRadius(5.5f * sc))
            drawLine(Acc2, Offset(left + 3f * sc, liquidTop), Offset(left + width - 3f * sc, liquidTop), 1.8f * sc)
        }
        drawRoundRect(Color(0xFF9A9AA5), Offset(left, top), Size(width, height), androidx.compose.ui.geometry.CornerRadius(7f * sc), style = Stroke(1.5f * sc))
        drawRoundRect(Color(0xFF4A4A53), Offset(left + 4f * sc, top + 2f * sc), Size(width - 8f * sc, 3f * sc), androidx.compose.ui.geometry.CornerRadius(2f * sc))
        if (empty) drawLine(Acc2, Offset(left + 9f * sc, top + height / 2f), Offset(left + width - 9f * sc, top + height / 2f), 1.5f * sc)
    }
}

@Composable
fun FuelTile(ui: Ui, modifier: Modifier) {
    val fuel = ui.n("0x3601") ?: 0.0
    val cap = ui.n("0x3600") ?: 0.0
    val res = ui.n("0x3603") ?: 0.0
    val empty = res > 0 && fuel <= res
    Tile("Paliwo", null, if (empty) Acc2 else null, modifier, { ui.open(Dlg.Fuel) }, emptyList()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FuelGlassIcon(fuel, cap, empty)
            Column {
                BigNum(f1(fuel, 0), "kg")
                Text(if (empty) "rezerwa" else "z " + f1(cap, 0) + " kg", color=Mut, fontSize=10.5.sp, maxLines=1)
            }
        }
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
fun HomeModuleTile(ui: Ui, id: String, modifier: Modifier) {
    when (id) {
        "solary" -> {
            val temp = ui.n("0x2109"); val power = ui.n("0x2101") ?: 0.0
            Tile("Solary", null, if ((ui.n("0x311C") ?: 0.0) != 0.0) Okc else null, modifier, { ui.go(id) }, emptyList()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Ic(87, 52.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        BigNum(f1(temp, 1), "°C", 25)
                        Text(if (power >= 1000) f1(power / 1000.0, 1) + " kW" else f1(power, 0) + " W", color = Mut, fontSize = 11.sp)
                    }
                }
            }
        }
        "bufor" -> {
            val top = ui.n("0x2112"); val bottom = ui.n("0x2113")
            Tile("Bufor", null, if ((ui.n("0x2277") ?: 0.0) != 0.0) Okc else null, modifier, { ui.go(id) }, emptyList()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Ic(84, 56.dp)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        BigNum(f1(top, 1), "°C", 25)
                        Text("dół  " + f1(bottom, 1) + " °C", color = Mut, fontSize = 11.sp)
                    }
                }
            }
        }
        "fuel" -> FuelTile(ui, modifier)
        "power" -> PowerTile(ui, modifier)
        else -> {
            val s = ui.m.screens[id] ?: return
            val title = if (id.startsWith("co") && id.length == 3) "CO " + id.last() else if (id == "cwu") "CWU" else s.title
            val active = (id.startsWith("co") || id == "cwu") && (ui.n(s.mode)?.toInt() ?: 0) > 0
            val mi = s.mode?.let { m -> ui.m.programModes.firstOrNull { it.v == (ui.n(m)?.toInt() ?: -1) }?.icon }
            Tile(title, mi, if (active) Okc else null, modifier, { ui.go(id) }, s.bar.mapNotNull { barIx(ui, it, id) }) {
                if (s.big != null) {
                    val value = f1(ui.n(s.big), 1)
                    val target = s.pill?.let { f1(ui.n(it), 0) }
                    BigNum(value, if (s.bigUnit == "°C") "°C" else s.bigUnit, 25)
                    if (target != null) Text("→ " + target + " °C", color = Acc2, fontSize = 11.sp)
                } else if (s.kv.isNotEmpty()) {
                    val k = s.kv.first()
                    Text(f1(ui.n(k.pid), k.dec) + " " + k.unit, color = Txt, fontSize = 22.sp)
                }
            }
        }
    }
}

@Composable
fun HomeTileGrid(ui: Ui, entries: List<String>) {
    if (entries.isEmpty()) return
    var page by remember(entries) { mutableIntStateOf(0) }
    val pages = entries.chunked(6)
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val pageWidth = this@BoxWithConstraints.maxWidth
            val pageWidthPx = pageWidth.value.toInt().coerceAtLeast(1)
            val scroll = rememberScrollState(page * pageWidthPx)
            LaunchedEffect(page, pageWidthPx) { scroll.animateScrollTo(page * pageWidthPx) }
            LaunchedEffect(scroll.value, pageWidthPx) {
                page = ((scroll.value + pageWidthPx / 2) / pageWidthPx).coerceIn(0, pages.lastIndex)
            }
            Row(
                Modifier.fillMaxWidth().horizontalScroll(scroll),
                horizontalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                pages.forEachIndexed { index, ids ->
                    Column(
                        Modifier.width(pageWidth),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ids.chunked(2).forEach { pair ->
                            Row(
                                Modifier.fillMaxWidth().height(86.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                HomeModuleTile(ui, pair[0], Modifier.weight(1f).fillMaxHeight())
                                if (pair.size > 1) HomeModuleTile(ui, pair[1], Modifier.weight(1f).fillMaxHeight())
                                else Spacer(Modifier.weight(1f))
                            }
                        }
                        repeat(3 - ids.chunked(2).size) {
                            Spacer(Modifier.fillMaxWidth().height(96.dp))
                        }
                    }
                }
            }
        }
        if (pages.size > 1) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                pages.indices.forEach { i ->
                    Box(
                        Modifier.padding(horizontal = 4.dp)
                            .size(if (i == page) 8.dp else 6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (i == page) Acc else Mut)
                            .clickable { page = i }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeView(ui: Ui) {
    val t = ui.n("0x2001")
    val set = ui.n("0x2004")
    val bs = ui.n("0x2007")?.toInt() ?: 0
    val on = ui.n("0x2005") != 0.0
    val run = bs in 1..8
    val mode = ui.m.statusTables["burner"]?.getOrNull(bs) ?: if (on) "Praca" else "Wyłączony"

    // HOME jest podglądem instalacji. Pełne nastawy i diagnostyka są dopiero po wejściu
    // w Kocioł/Palnik albo przez Menu. Kafelki są generowane wyłącznie dla aktywnych
    // pozycji modelu/menu; dodatkowe CO pozostają w Menu.
    val entries = buildList {
        visibleTileIds(ui)
            .filter { it != "kociol" && it != "palnik" }
            .distinct()
            .forEach { add(it) }
        if (!contains("fuel")) add("fuel")
        if (!contains("power")) add("power")
    }

    Column(
        Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GlowCard(
            Modifier.fillMaxWidth().height(205.dp),
            if (run) Okc else null,
            { ui.go("kociol") }
        ) {
            Row(
                Modifier.fillMaxWidth().height(126.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    Modifier.width(108.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    BoilerImage(ui)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).background(if (run) Okc else Mut, RoundedCornerShape(4.dp)))
                        Spacer(Modifier.width(5.dp))
                        Text(if (run) "Praca" else "Wyłączony", color = if (run) Okc else Mut, fontSize = 11.sp)
                    }
                }
                Column(
                    Modifier.weight(1f).padding(start = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(f1(t, 1), color = Txt, fontSize = 35.sp, fontWeight = FontWeight.Light)
                        Text("°C", color = Mut, fontSize = 13.sp, modifier = Modifier.padding(start = 3.dp, bottom = 5.dp))
                    }
                    Text("→ " + f1(set, 1) + " °C", color = Acc, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Jasność płomienia  " + f1(ui.n("0x4005"), 0) + " %",
                        color = Txt, fontSize = 12.sp
                    )
                    Text(
                        "Spalanie  " + f1(ui.n("0x3710"), 1) + " kg/h",
                        color = Mut, fontSize = 12.sp
                    )
                }
            }
            BarView(ui, ui.m.screens.getValue("kociol").bar + ui.m.screens.getValue("palnik").bar)
        }

        if (entries.isNotEmpty()) HomeTileGrid(ui, entries)
    }
}
@Composable
fun HomeValueRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Mut, fontSize = 11.sp, modifier = Modifier.weight(1f))
        Text(value, color = Txt, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
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
        RowItem("Dostęp ESP", "Full · zapis modelowy")
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
fun LevelSep(t: String) { Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { HorizontalDivider(Modifier.weight(1f), color = Line); Text(t, color = Mut, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp); HorizontalDivider(Modifier.weight(1f), color = Line) } }

@Composable
fun MoreView(ui: Ui) {
    val items = ui.m.menu.filter { ui.m.menuVisible(it, ui.v, ui.level) }.mapNotNull { m -> ui.m.routeOf(m)?.let { it to m } }
    val names = mapOf(1 to "POZIOM 1 — INSTALATOR", 2 to "POZIOM 2", 3 to "POZIOM 3")
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (Ctl.devices.isEmpty()) {
            GlowCard(Modifier.fillMaxWidth(), Warn, null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("BRAK MODUŁU", color=Warn, fontSize=10.sp, fontWeight=FontWeight.Bold, letterSpacing=.8.sp)
                        Text("Dodaj moduł TeklaPellet", color=Txt, fontSize=16.sp, fontWeight=FontWeight.Medium)
                        Text("mDNS, adres ręczny lub Bluetooth.", color=Mut, fontSize=11.sp)
                    }
                    Button({ ui.go("appset") }) { Text("Dodaj") }
                }
            }
        } else Card("URZĄDZENIE") {
            RowItem("Aktywny moduł", Ctl.activeName()) { ui.go("appset") }
            RowItem("Moduły", Ctl.devices.size.toString()) { ui.go("appset") }
        }
        Card("MENU KOTŁA") {
            var lastLevel = -1
            for ((r, m) in items) {
                if (m.levelMin != lastLevel) {
                    if (m.levelMin >= 1) LevelSep(names.getValue(m.levelMin))
                    lastLevel = m.levelMin
                }
                Row(Modifier.fillMaxWidth().heightIn(min=52.dp).clickable { ui.go(r) }.padding(horizontal=4.dp), verticalAlignment=Alignment.CenterVertically) {
                    Ic(m.icon,24.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(m.title,color=Txt,fontSize=15.sp,modifier=Modifier.weight(1f))
                    Text("›",color=Mut,fontSize=22.sp)
                }
            }
        }
        LevelSep("MODUŁ ESP")
        Card("MODUŁ") {
            MenuRow("Moduł ESP", "Wi-Fi, CAN, OTA, klienci, diagnostyka") { ui.go("module") }
            MenuRow("Dziennik modułu", "Logi i diagnostyka") { ui.go("log") }
            MenuRow("Sieć CAN", "Węzły i stan komunikacji") { ui.go("network") }
        }
        LevelSep("APLIKACJA")
        Card("APLIKACJA") {
            MenuRow("Powiadomienia", "Alarmy, aktualizacje aplikacji i firmware") { ui.go("notifications") }
            MenuRow("Ustawienia aplikacji", "Demo, urządzenia, bezpieczeństwo") { ui.go("appset") }
        }
    }
}

@Composable
fun MenuRow(title: String, subtitle: String, click: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min=56.dp).clickable(onClick=click).padding(horizontal=4.dp, vertical=6.dp), verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) { Text(title,color=Txt,fontSize=15.sp); Text(subtitle,color=Mut,fontSize=11.sp) }
        Text("›",color=Mut,fontSize=20.sp)
    }
}
@Composable
fun NotificationView(ui: Ui) {
    LaunchedEffect(Unit){UpdateCenter.refresh()}
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        if(ui.alarmActive) Card("KOCIOŁ"){MenuRow("Aktywny alarm","Otwórz historię alarmów"){ui.go("alarms")}}
        if(UpdateCenter.notices.isEmpty()) Card("AKTUALIZACJE"){
            Text("Brak nowych aktualizacji.",color=Mut,fontSize=13.sp)
            Text("Aplikacja sprawdza wersje publikowane z gałęzi main.",color=Mut,fontSize=11.sp,modifier=Modifier.padding(top=6.dp))
        } else Card("AKTUALIZACJE"){
            for(n in UpdateCenter.notices) MenuRow(n.title,n.current+" → "+n.latest){ui.toast(if(n.changelog.isBlank())"Dostępna wersja "+n.latest else n.changelog)}
        }
        Card("SYSTEM"){MenuRow("Sprawdź ponownie","Manifest aktualizacji z main"){UpdateCenter.refresh();ui.toast("Sprawdzam aktualizacje…")}}
    }
}

@Composable
fun Dialogs(ui: Ui, d: Dlg, close: () -> Unit) {
    val scope = rememberCoroutineScope(); var err by remember(d) { mutableStateOf("") }
    when (d) {
        is Dlg.Pin -> { var pin by remember { mutableStateOf("") }
            AlertDialog(onDismissRequest = close, title = { Text("PIN poziomu") }, text = { Column { OutlinedTextField(pin, { pin = it }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)); if (err.isNotEmpty()) Text(err, color = Bad, fontSize = 13.sp) } },
                confirmButton = { TextButton({ scope.launch { val level = BoilerAccess.unlock(pin); ui.toast(if (level > 0) "Poziom $level" else "Błędny PIN poziomu"); if (level > 0) close() } }) { Text("OK") } }, dismissButton = { TextButton(close) { Text("Anuluj") } }) }
        is Dlg.Fuel -> { val fuel = ui.n("0x3601") ?: 0.0; val cap = ui.n("0x3600") ?: 0.0; val res = ui.n("0x3603") ?: 0.0; val empty = res > 0 && fuel <= res
            AlertDialog(onDismissRequest = close, title = { Text("Paliwo") }, text = { Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) { FuelGlassIcon(fuel, cap, empty, 72.dp, 86.dp); BigNum(f1(fuel, 0), "kg", 40) }
                LinearProgressIndicator(progress = { if (cap > 0) (fuel / cap).coerceIn(0.0, 1.0).toFloat() else 0f }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp), color = Acc2, trackColor = Line)
                KvRow("pojemność", f1(cap, 0) + " kg"); KvRow("rezerwa", f1(res, 0) + " kg")
                Text("Zmiana poziomu paliwa z aplikacji nie jest jeszcze obsługiwana: panel nie ma zmierzonego kodowania zapisu dla tego parametru (0x3601).", color = Mut, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp)) } },
                confirmButton = { TextButton(close) { Text("Zamknij") } }) }
        is Dlg.Mode -> { AlertDialog(onDismissRequest = close, title = { Text("Dostęp ESP") }, text = { Text("ESP pracuje w trybie Full. Uprawnienia zapisu są kontrolowane lokalnie przez poziom dostępu aplikacji oraz walidację modelu urządzenia.") }, confirmButton = { TextButton(close) { Text("OK") } }) }
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
    var set by remember { mutableStateOf(AppLock.isSet()) }
    var bio by remember { mutableStateOf(AppLock.biometricEnabled()) }
    var pin by remember { mutableStateOf("") }
    val activity = LocalContext.current as? androidx.fragment.app.FragmentActivity
    Card("Zabezpieczenie aplikacji") {
        Text("Chroni wejście do aplikacji. Jest niezależne od poziomów dostępu kotła.", color = Mut, fontSize = 13.sp)
        OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(12) }, label = { Text("Nowy PIN (4–12 cyfr)") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button({ if (pin.length >= 4) { AppLock.set(pin); pin = ""; set = true; Ctl.appUnlocked = true; ui.toast("PIN ustawiony") } else ui.toast("Min. 4 cyfry") }) { Text(if (set) "Zmień PIN" else "Ustaw PIN") }
            if (set) OutlinedButton({ AppLock.clear(); BoilerAccess.lock(); set = false; bio = false; Ctl.appUnlocked = true; ui.toast("Zabezpieczenie wyłączone") }) { Text("Wyłącz", color = Txt) }
        }
        if (set) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Biometria", color = Txt)
                    Text("Odcisk palca / twarz, jeśli telefon obsługuje.", color = Mut, fontSize = 12.sp)
                }
                Switch(checked = bio, onCheckedChange = { want ->
                    val a = activity
                    val ok = a != null && BiometricManager.from(a).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
                    if (want && !ok) ui.toast("Biometria nie jest dostępna na tym telefonie.") else { bio = want; AppLock.setBiometric(want) }
                })
            }
        }
    }
}

private fun showBiometric(activity: androidx.fragment.app.FragmentActivity, ok: () -> Unit, fail: (String) -> Unit) {
    val prompt = BiometricPrompt(activity, object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { ok() }
        override fun onAuthenticationError(code: Int, msg: CharSequence) { fail(msg.toString()) }
    })
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Odblokuj TeklaPellet")
        .setSubtitle("Użyj biometrii telefonu")
        .setNegativeButtonText("Użyj PIN-u")
        .build()
    prompt.authenticate(info)
}

@Composable
fun LockScreen(onOk: () -> Unit) {
    var pin by remember { mutableStateOf("") }; var err by remember { mutableStateOf("") }
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize().background(Bg).padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("TeklaPellet", color = Txt, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(pin, { pin = it.filter(Char::isDigit).take(12) }, label = { Text("PIN aplikacji") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword))
        if (err.isNotEmpty()) Text(err, color = Bad, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        Button({ val e = AppLock.check(pin); if (e == null) onOk() else { err = e; pin = "" } }, Modifier.padding(top = 12.dp)) { Text("Odblokuj PIN-em") }
        if (AppLock.biometricEnabled() && ctx is androidx.fragment.app.FragmentActivity) {
            OutlinedButton({ showBiometric(ctx, onOk) { err = it } }, Modifier.padding(top = 8.dp)) { Text("Odblokuj biometrią") }
        }
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
