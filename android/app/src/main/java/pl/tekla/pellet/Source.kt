package pl.tekla.pellet

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.sin

class AlarmRow(val n: Int, val code: Int, val text: String, val state: String)
class NodeRow(val node: Int, val name: String, val state: String)

/** Zrodlo danych: tryb demo (wbudowane dane) albo urzadzenie ESP (REST). UI jest identyczne dla obu. */
interface DataSource {
    val isDemo: Boolean
    val values: StateFlow<Map<String, Double>>
    val connected: StateFlow<Boolean>
    val level: StateFlow<Int>
    val fullMode: StateFlow<Boolean>
    suspend fun read(pids: List<String>): Map<String, Double>
    suspend fun write(pid: String, v: Double): String?            // null = OK, inaczej komunikat bledu
    suspend fun unlock(pin: String): String?
    suspend fun setFullMode(full: Boolean, password: String): String?
    suspend fun alarms(): List<AlarmRow>
    suspend fun network(): List<NodeRow>
    suspend fun wifiStatus(): String
    suspend fun wifiScan(): List<Pair<String, Int>>
    suspend fun wifiSave(ssid: String, pass: String): String?
    suspend fun changePassword(newPw: String): String?
    suspend fun restart(password: String): String?
    suspend fun factoryReset(password: String): String?
    suspend fun log(): List<String>
    suspend fun ota(kind: String, data: ByteArray): String?          // kind: "fw" | "ui"
    fun close()
}

private fun fmtPid(p: String) = normPid(p)

/** Demo: dane przykladowe + cykl pracy palnika (jak serwer-atrapa WWW). Konfiguracja kotla (obwody, CWU, bufor...) jest edytowalna i zmienia menu. */
class DemoSource(private val model: AppModel, private val scope: CoroutineScope) : DataSource {
    override val isDemo = true
    private val store = HashMap<String, Double>()
    private val _values = MutableStateFlow<Map<String, Double>>(emptyMap())
    override val values: StateFlow<Map<String, Double>> = _values
    override val connected = MutableStateFlow(true)
    override val level = MutableStateFlow(0)
    override val fullMode = MutableStateFlow(true)
    private var tick = 0
    private var job: Job? = null

    init {
        for (rows in model.lists.values) for (r in rows) if (r.pid != null) store[r.pid] = r.def * model.scale(r.pid)
        store.putAll(model.demoValues); store.putAll(model.demoConfig)
        for (c in model.circuits) for (p in c.pids) store[p] = model.defaultMask.toDouble()
        publish()
        job = scope.launch { while (isActive) { delay(1000); tick++; publish() } }
    }

    private fun publish() {
        val m = HashMap(store); val ph = (tick / 8) % 8; val bs = intArrayOf(0, 1, 2, 5, 5, 6, 7, 0)[ph]; val run = bs in 1..8
        fun w(a: Double, b: Double) = Math.round((a + sin(tick / 6.0) * b) * 10) / 10.0
        m["0x2007"] = bs.toDouble(); m["0x2001"] = w(62.0, 3.0); m["0x4000"] = w(41.0, 1.0); m["0x4001"] = w(48.0, 2.0); m["0x2110"] = w(53.0, 1.0)
        m["0x4005"] = if (run) 30.0 else 0.0; m["0x3706"] = if (run) 5.2 else 0.0; m["0x3710"] = if (run) 1.4 else 0.0; m["0x2250"] = if (run) 100.0 else 0.0
        m["0x226C"] = if (run) 31.0 else 0.0; m["0x299C"] = if (run) 52.0 else 0.0
        m["0x2254"] = if (run) 1.0 else 0.0; m["0x2251"] = if (run && tick % 6 < 3) 1.0 else 0.0
        m["0x2252"] = if (bs == 1 || bs == 5) 1.0 else 0.0; m["0x2253"] = if (bs == 1) 1.0 else 0.0; m["0x2276"] = if (bs == 7) 1.0 else 0.0
        m["0x22B9"] = if (ph == 2) 2.0 else if (ph == 5) 1.0 else 0.0
        _values.value = m
    }

    override suspend fun read(pids: List<String>): Map<String, Double> = pids.mapNotNull { p -> store[fmtPid(p)]?.let { fmtPid(p) to it } }.toMap()
    override suspend fun write(pid: String, v: Double): String? { store[fmtPid(pid)] = v; publish(); return null }
    override suspend fun unlock(pin: String): String? { val lv = mapOf("1111" to 1, "2222" to 2, "3333" to 3)[pin] ?: return "Błędny PIN (demo: 1111 / 2222 / 3333)"; level.value = lv; return null }
    override suspend fun setFullMode(full: Boolean, password: String): String? { fullMode.value = full; return null }
    override suspend fun alarms() = listOf(AlarmRow(1, 12, "Przykładowy alarm (dane demo)", "potwierdzony"))
    override suspend fun network() = listOf(NodeRow(4, "Kocioł", "pracuje"), NodeRow(95, "TeklaPellet (ten moduł)", "pracuje"))
    override suspend fun wifiStatus() = "DomowaSiec (demo) ●"
    override suspend fun wifiScan() = listOf("DomowaSiec" to -52, "Goscinna" to -70)
    override suspend fun wifiSave(ssid: String, pass: String): String? = null
    override suspend fun changePassword(newPw: String): String? = null
    override suspend fun restart(password: String): String? = null
    override suspend fun factoryReset(password: String): String? = null
    override suspend fun log() = listOf("start aplikacji (demo)", "dane przykładowe")
    override suspend fun ota(kind: String, data: ByteArray): String? = "Aktualizacja niedostępna w trybie demo"
    override fun close() { job?.cancel() }
}

val ERR = mapOf(
    "read_only_mode" to "Tryb „tylko odczyt” - zmień tryb, aby zapisywać.", "blocked" to "Parametr zablokowany (poza listami panelu).", "level_too_low" to "Wymagany wyższy poziom PIN.",
    "out_of_range" to "Wartość poza zakresem.", "not_writable" to "Parametr tylko do odczytu.", "unknown_pid" to "Nieznany parametr.", "no_encoding" to "Zapis tego parametru nie jest obsługiwany.",
    "bad_password" to "Błędne hasło.", "bad_pin" to "Błędny PIN.", "locked" to "Zbyt wiele prób - spróbuj za chwilę.", "can_timeout" to "Brak odpowiedzi kotła.",
    "verify_failed" to "Odczyt zwrotny nie potwierdził zapisu.", "unauthorized" to "Sesja wygasła - połącz ponownie.",
)

/** Urzadzenie ESP: REST (docs/ESP_API_V2.md). Dane na zywo przez odpytywanie /api/state co 1 s. */
class RemoteSource(private val host: String, private val scope: CoroutineScope) : DataSource {
    override val isDemo = false
    private val http = OkHttpClient.Builder().connectTimeout(4, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS).build()
    private val json = "application/json".toMediaType()
    private var token: String? = null
    private val _values = MutableStateFlow<Map<String, Double>>(emptyMap())
    override val values: StateFlow<Map<String, Double>> = _values
    override val connected = MutableStateFlow(false)
    override val level = MutableStateFlow(0)
    override val fullMode = MutableStateFlow(false)
    private var job: Job? = null
    private val base = (if (host.startsWith("http")) host else "http://$host").trimEnd('/')

    private suspend fun call(method: String, path: String, body: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
        try {
            val rb = if (method == "GET") null else (body?.toString() ?: "{}").toRequestBody(json)
            val req = Request.Builder().url(base + path).method(method, rb).apply { token?.let { header("Authorization", "Bearer $it") } }.build()
            http.newCall(req).execute().use { r -> val t = r.body?.string() ?: "{}"; try { JSONObject(t) } catch (e: Exception) { JSONObject().put("ok", false).put("code", "bad_response") } }
        } catch (e: Exception) { JSONObject().put("ok", false).put("code", "network").put("msg", e.message ?: "brak połączenia") }
    }
    private fun err(j: JSONObject): String = ERR[j.optString("code")] ?: (j.optString("msg").ifEmpty { "Błąd: " + j.optString("code") })

    suspend fun login(password: String): String? {
        val j = call("POST", "/api/login", JSONObject().put("password", password)); if (!j.optBoolean("ok")) return err(j)
        token = j.getString("token"); fullMode.value = j.optString("mode") == "full"; level.value = j.optInt("level", 0); connected.value = true
        job = scope.launch { while (isActive) { poll(); delay(1000) } }; return null
    }
    private suspend fun poll() {
        val j = call("GET", "/api/state"); if (!j.optBoolean("ok")) { connected.value = false; return }
        connected.value = true; val v = j.optJSONObject("v") ?: return; val m = HashMap(_values.value)
        for (k in v.keys()) m[normPid(k)] = v.getDouble(k); _values.value = m
        val i = call("GET", "/api/info"); if (i.optBoolean("ok")) { fullMode.value = i.optString("mode") == "full"; level.value = i.optInt("level", level.value) }
    }
    override suspend fun read(pids: List<String>): Map<String, Double> {
        val out = HashMap<String, Double>()
        for (chunk in pids.chunked(24)) { val j = call("GET", "/api/params?pids=" + chunk.joinToString(",")); val v = j.optJSONObject("values") ?: continue; for (k in v.keys()) out[normPid(k)] = v.getDouble(k) }
        return out
    }
    override suspend fun write(pid: String, v: Double): String? {
        val j = call("PUT", "/api/param/" + fmtPid(pid), JSONObject().put("value", v)); if (!j.optBoolean("ok")) return err(j)
        _values.value = HashMap(_values.value).also { it[fmtPid(pid)] = j.optDouble("v", v) }; return null
    }
    override suspend fun unlock(pin: String): String? { val j = call("POST", "/api/level", JSONObject().put("pin", pin)); if (!j.optBoolean("ok")) return err(j); level.value = j.optInt("level"); return null }
    override suspend fun setFullMode(full: Boolean, password: String): String? {
        val j = call("POST", "/api/mode", JSONObject().put("mode", if (full) "full" else "readonly").put("password", password)); if (!j.optBoolean("ok")) return err(j)
        fullMode.value = j.optString("mode") == "full"; return null
    }
    override suspend fun alarms(): List<AlarmRow> = (call("GET", "/api/alarms").optJSONArray("alarms")?.objs() ?: emptyList()).map {
        AlarmRow(it.optInt("n"), it.optInt("code"), it.optString("text"), if (it.optBoolean("active")) "AKTYWNY" else if (it.optBoolean("acked")) "potwierdzony" else "nowy")
    }
    override suspend fun network(): List<NodeRow> = (call("GET", "/api/network").optJSONArray("nodes")?.objs() ?: emptyList()).map { NodeRow(it.optInt("node"), it.optString("name"), it.optString("state")) }
    override fun close() { job?.cancel() }
    override suspend fun wifiStatus(): String { val j = call("GET", "/api/wifi"); return if (j.optBoolean("ok")) j.optString("ssid", "--") + if (j.optBoolean("connected")) " ●" else " ○" else err(j) }
    override suspend fun wifiScan(): List<Pair<String, Int>> = (call("GET", "/api/wifi/scan").optJSONArray("networks")?.objs() ?: emptyList()).map { it.optString("ssid") to it.optInt("rssi") }
    override suspend fun wifiSave(ssid: String, pass: String): String? { val j = call("POST", "/api/wifi", JSONObject().put("ssid", ssid).put("pass", pass)); return if (j.optBoolean("ok")) null else err(j) }
    override suspend fun changePassword(newPw: String): String? { val j = call("POST", "/api/password", JSONObject().put("new", newPw)); return if (j.optBoolean("ok")) null else err(j) }
    override suspend fun restart(password: String): String? { val j = call("POST", "/api/restart", JSONObject().put("password", password)); return if (j.optBoolean("ok")) null else err(j) }
    override suspend fun factoryReset(password: String): String? { val j = call("POST", "/api/factory_reset", JSONObject().put("password", password)); return if (j.optBoolean("ok")) null else err(j) }
    override suspend fun log(): List<String> = call("GET", "/api/log").optJSONArray("lines")?.strs() ?: emptyList()
    override suspend fun ota(kind: String, data: ByteArray): String? = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url("$base/api/ota/$kind").post(data.toRequestBody("application/octet-stream".toMediaType())).apply { token?.let { header("Authorization", "Bearer $it") } }.build()
            http.newBuilder().readTimeout(120, TimeUnit.SECONDS).build().newCall(req).execute().use { r -> val j = try { JSONObject(r.body?.string() ?: "{}") } catch (e: Exception) { JSONObject() }; if (j.optBoolean("ok")) null else (ERR[j.optString("code")] ?: "Błąd: " + j.optString("code")) }
        } catch (e: Exception) { e.message ?: "brak połączenia" }
    }
}
