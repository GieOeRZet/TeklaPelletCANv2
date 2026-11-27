package pl.tekla.pellet

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// Model aplikacji wczytany z assets/app.json (menu, ekrany, konfiguracja, harmonogramy, demo) i assets/model.min.json (listy nastaw, skale PID).
// Jedno zrodlo z WWW (tools/gen_app_model.py). Klucze wartosci: "0x2001" (wielkie litery hex).

fun JSONArray.objs(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }
fun JSONArray.strs(): List<String> = (0 until length()).map { getString(it) }
fun JSONArray.ints(): List<Int> = (0 until length()).map { getInt(it) }
fun JSONObject.str(k: String): String? = if (has(k) && !isNull(k)) getString(k) else null
fun JSONObject.dbl(k: String, d: Double): Double = if (has(k) && !isNull(k)) getDouble(k) else d
fun normPid(p: String): String = "0x" + p.removePrefix("0x").removePrefix("0X").uppercase()

class Opt(val v: Int, val label: String)

class SetRow(
    val pid: String?, val label: String, val levelMin: Int, val min: Double, val max: Double,
    val step: Double, val def: Double, val options: List<Opt>?,
)

class Cond(val pid: String, val op: String, val v: Int)
class MenuRow(val title: String, val target: String?, val levelMin: Int, val cond: List<Cond>, val icon: Int, val iconSel: Int)
class Cell(val k: String, val p: String, val fr: List<Int>, val n: String?, val nd: Boolean, val hm: Boolean, val l: String? = null)
class Kv(val label: String, val pid: String, val unit: String, val dec: Int, val cond: String?)
class ScreenDef(
    val id: String, val title: String, val list: String, val big: String?, val bigUnit: String, val state: String?, val stateP: String?,
    val pill: String?, val status: String?, val mode: String?, val sched: String?, val image: Boolean, val kv: List<Kv>, val bar: List<Cell>,
)
class CfgRow(val pid: String, val label: String, val levelMin: Int, val min: Double, val max: Double, val def: Double, val options: List<Opt>?)
class Circuit(val id: String, val title: String, val pids: List<String>)
class ProgramMode(val v: Int, val label: String, val icon: Int)

class AppModel(ctx: Context) {
    private val app = JSONObject(ctx.assets.open("app.json").bufferedReader().use { it.readText() })
    private val mm = JSONObject(ctx.assets.open("model.min.json").bufferedReader().use { it.readText() })

    val menu: List<MenuRow> = app.getJSONArray("menu").objs().map { m ->
        MenuRow(m.getString("title"), m.str("target"), m.getInt("level_min"),
            m.getJSONArray("cond").objs().map { Cond(normPid(it.getString("pid")), it.getString("op"), it.getInt("val")) }, m.getInt("icon"), m.getInt("icon_sel"))   // glify menu = bitmapy icon_sel (icon = tlo/stan nieaktywny)
    }
    val screens: Map<String, ScreenDef> = app.getJSONObject("screens").let { s ->
        s.keys().asSequence().associateWith { id ->
            val o = s.getJSONObject(id); val big = o.optJSONArray("big")
            ScreenDef(id, o.getString("t"), o.getString("list"), big?.getString(0)?.let { normPid(it) }, big?.getString(1) ?: "", o.str("state"), o.str("stateP")?.let { normPid(it) },
                o.str("pill")?.let { normPid(it) }, o.str("status")?.let { normPid(it) }, o.str("mode")?.let { normPid(it) }, o.str("sched"), o.optBoolean("image", false),
                o.getJSONArray("kv").let { a -> (0 until a.length()).map { i -> val r = a.getJSONArray(i); Kv(r.getString(0), normPid(r.getString(1)), r.getString(2), r.getInt(3), if (r.length() > 4) normPid(r.getString(4)) else null) } },
                o.getJSONArray("bar").objs().map { c -> Cell(c.getString("k"), normPid(c.getString("p")), c.optJSONArray("fr")?.ints() ?: emptyList(), c.str("n")?.let { normPid(it) }, c.optInt("nd", 0) == 1, c.optInt("hm", 0) == 1, c.str("l")) })
        }
    }
    val statusTables: Map<String, List<String>> = app.getJSONObject("status").let { s -> s.keys().asSequence().associateWith { s.getJSONArray(it).strs() } }
    val config: List<CfgRow> = app.getJSONArray("config").objs().map { c ->
        CfgRow(normPid(c.getString("pid")), c.getString("label"), c.getInt("level_min"), c.dbl("min", 0.0), c.dbl("max", 1.0), c.dbl("default", 0.0),
            c.optJSONArray("options")?.objs()?.map { Opt(it.getInt("v"), it.getString("label")) })
    }
    private val sch = app.getJSONObject("schedules")
    val circuits: List<Circuit> = sch.getJSONArray("circuits").objs().map { c ->
        val days = sch.getJSONArray("days").strs(); val p = c.getJSONObject("pids")
        Circuit(c.getString("id"), c.getString("title"), days.map { normPid(p.getString(it)) })
    }
    val days: List<String> = sch.getJSONArray("days").strs()
    val dayNames: List<String> = sch.getJSONArray("day_names").strs()
    val defaultMask: Int = sch.getInt("default_mask")
    val programModes: List<ProgramMode> = app.getJSONArray("program_modes").objs().map { ProgramMode(it.getInt("v"), it.getString("label"), it.getInt("icon")) }
    val boilerImages: Map<Int, Int> = app.getJSONObject("boiler_images").let { b -> b.keys().asSequence().associate { it.toInt() to b.getInt(it) } }
    val pumpFrames: List<Int> = app.getJSONArray("pump_frames").ints()
    val demoConfig: Map<String, Double> = app.getJSONObject("demo").getJSONObject("config").let { d -> d.keys().asSequence().associate { normPid(it) to d.getDouble(it) } }
    val demoValues: Map<String, Double> = app.getJSONObject("demo").getJSONObject("values").let { d -> d.keys().asSequence().associate { normPid(it) to d.getDouble(it) } }

    private val pidScale: Map<String, Double> = mm.getJSONObject("pids").let { p -> p.keys().asSequence().associate { normPid(it) to p.getJSONArray(it).optDouble(5, 1.0) } }
    fun scale(pid: String): Double = pidScale[normPid(pid)] ?: 1.0
    val lists: Map<String, List<SetRow>> = mm.getJSONObject("lists").let { l ->
        l.keys().asSequence().associateWith { key ->
            l.getJSONArray(key).objs().map { r ->
                val od = r.optJSONObject("options")
                val opts: List<Opt>? = if (od != null) od.keys().asSequence().map { it.toInt() }.sorted().map { k -> Opt(k, if (od.isNull(k.toString())) (if (k == 0) "Wyłączone" else "Włączone") else od.getString(k.toString())) }.toList()
                else r.optJSONArray("options_dialog")?.strs()?.drop(1)?.mapIndexed { i, s -> Opt(i, s) }
                SetRow(r.str("pid")?.let { normPid(it) }, r.str("label_pl") ?: "", r.optInt("level_min", 0), r.dbl("min", 0.0), r.dbl("max", 0.0), r.dbl("step", 1.0), r.dbl("default", 0.0), opts)
            }
        }
    }

    fun menuVisible(m: MenuRow, v: Map<String, Double>, level: Int): Boolean =
        m.levelMin <= level && m.cond.all { c -> val x = v[c.pid]; x != null && (if (c.op == ">=") x >= c.v else if (c.op == "!=") x.toInt() != c.v else x.toInt() == c.v) }

    /** Trasa ekranu dla wiersza menu panelu (null = element wbudowany / nieobslugiwany). */
    fun routeOf(m: MenuRow): String? = when (m.target) {
        "KOCIOL" -> "kociol"; "PALNIK" -> "palnik"; "OGRZEWANIE" -> "co" + m.title.last(); "CWU" -> "cwu"; "SOLARY" -> "solary"; "BUFOR" -> "bufor"
        "KASKADA" -> "kaskada"; "MAGAZYN" -> "magazyn"; "ALARMY" -> "alarms"; "INFO" -> "info"; "KONFIGURACJA" -> "config"; "TEST_WYJSC" -> "outtest"
        "STATYSTYKI" -> "stats"; "SIEC" -> "network"; "INTERNET" -> "internet"; else -> null
    }
}
