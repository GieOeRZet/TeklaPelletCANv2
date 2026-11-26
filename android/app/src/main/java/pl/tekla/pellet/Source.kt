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

class IpInfo(val dhcp: Boolean, val ip: String, val mask: String, val gw: String, val dns1: String, val dns2: String)
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
    suspend fun serviceUnlock(password: String): String?          // odblokowanie operacji serwisowych modulu (haslo, 5 min)
    val serviceUnlocked: Boolean
    suspend fun clients(): List<Pair<Int, String>>
    suspend fun revokeClient(i: Int): String?
    suspend fun setDeviceName(name: String): String?
    suspend fun setRecovery(code: String): String?
    suspend fun ipInfo(): IpInfo?
    suspend fun ipApply(i: IpInfo): String?                      // zmiana adresu IP z potwierdzeniem z nowego adresu (inaczej modul wraca do starych ustawien)
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
    override val level = MutableStateFlow(BoilerAccess.level())
    override val fullMode = MutableStateFlow(true)
    private var tick = Ctl.startTick
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
    // PIN-y kotła są logiką aplikacji Android; ESP pozostaje Full. Te metody zmieniają wyłącznie lokalny poziom UI.\n    override suspend fun unlock(pin: String): String? { val l = BoilerAccess.unlock(pin); level.value = l; return if (l > 0 || BoilerAccess.hasPin(1) || BoilerAccess.hasPin(2) || BoilerAccess.hasPin(3)) { if (l > 0) null else "Błędny PIN poziomu" } else "Najpierw ustaw PIN poziomu w ustawieniach aplikacji." }\n    override suspend fun setFullMode(full: Boolean, password: String): String? { fullMode.value = full; return null }\n    override suspend fun alarms() = listOf(AlarmRow(1, 12, "Przykładowy alarm (dane demo)", "potwierdzony"))
    override suspend fun network() = listOf(NodeRow(4, "Kocioł", "pracuje"), NodeRow(95, "TeklaPellet (ten moduł)", "pracuje"))
    override suspend fun wifiStatus() = "DomowaSiec (demo) ●"
    override suspend fun wifiScan() = listOf("DomowaSiec" to -52, "Goscinna" to -70)
    override suspend fun wifiSave(ssid: String, pass: String): String? = null
    override suspend fun changePassword(newPw: String): String? = null
    override suspend fun restart(password: String): String? = null
    override suspend fun factoryReset(password: String): String? = null
    override suspend fun log() = listOf("start aplikacji (demo)", "dane przykładowe")
    override suspend fun serviceUnlock(password: String): String? = null
    override val serviceUnlocked = true
    override suspend fun clients() = listOf(0 to "Ten telefon (demo)")
    override suspend fun revokeClient(i: Int): String? = null
    override suspend fun setDeviceName(name: String): String? = null
    override suspend fun setRecovery(code: String): String? = null
    override suspend fun ipInfo() = IpInfo(true, "192.168.1.50", "255.255.255.0", "192.168.1.1", "192.168.1.1", "")
    override suspend fun ipApply(i: IpInfo): String? = null
    override suspend fun ota(kind: String, data: ByteArray): String? = "Aktualizacja niedostępna w trybie demo"
    override fun close() { job?.cancel() }
}

val ERR = mapOf(
    "blocked" to "Parametr zablokowany (poza listami panelu).",
    "out_of_range" to "Wartość poza zakresem.",
    "not_writable" to "Parametr tylko do odczytu.",
    "unknown_pid" to "Nieznany parametr.",
    "no_encoding" to "Zapis tego parametru nie jest obsługiwany.",
    "bad_password" to "Błędne hasło.",
    "locked" to "Zbyt wiele prób - spróbuj za chwilę.",
    "can_timeout" to "Brak odpowiedzi kotła.",
    "verify_failed" to "Odczyt zwrotny nie potwierdził zapisu.",
    "unauthorized" to "Sesja wygasła - połącz ponownie.",
)

const val OFFLINE_MSG = "Moduł jest offline – zmiana niemożliwa. Pokazuję ostatnie znane wartości."

/** Urzadzenie ESP: REST (docs/API_ESP.md). Autoryzacja tokenem klienta (POST /api/pair, raz). Dane na zywo przez odpytywanie /api/state co 1 s; ostatnie znane wartosci sa zapamietywane
 *  (modul offline = widac ostatni stan, a proba zmiany daje komunikat). */
class RemoteSource(host: String, private val scope: CoroutineScope, private var token: String? = null, val deviceId: String = "") : DataSource {
    override val isDemo = false
    @Volatile var host: String = host; private set
    private val http = OkHttpClient.Builder().connectTimeout(4, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS).build()
    private val json = "application/json".toMediaType()
    private val _values = MutableStateFlow<Map<String, Double>>(if (deviceId.isNotEmpty()) DeviceStore.cacheGet(Ctl.ctx, deviceId) else emptyMap())
    override val values: StateFlow<Map<String, Double>> = _values
    override val connected = MutableStateFlow(false)
    override val level = MutableStateFlow(0)
    override val fullMode = MutableStateFlow(true)
    private var job: Job? = null
    private var timeSynced = false
    private var svcPw: String? = null
    override val serviceUnlocked: Boolean get() = svcPw != null
    private fun base() = (if (host.startsWith("http")) host else "http://$host").trimEnd('/')

    suspend fun call(method: String, path: String, body: JSONObject? = null): JSONObject = withContext(Dispatchers.IO) {
        try {
            val rb = if (method == "GET") null else (body?.toString() ?: "{}").toRequestBody(json)
            val req = Request.Builder().url(base() + path).method(method, rb).apply { token?.let { header("Authorization", "Bearer $it") } }.build()
            http.newCall(req).execute().use { r -> val t = r.body?.string() ?: "{}"; try { JSONObject(t) } catch (e: Exception) { JSONObject().put("ok", false).put("code", "bad_response") } }
        } catch (e: Exception) { JSONObject().put("ok", false).put("code", "network").put("msg", e.message ?: "brak połączenia") }
    }
    private fun err(j: JSONObject): String = ERR[j.optString("code")] ?: (j.optString("msg").ifEmpty { "Błąd: " + j.optString("code") })
    private fun offline(): String? = if (connected.value) null else OFFLINE_MSG.also { Ctl.notify(it) }

    fun start() {
        if (job != null) return
        job = scope.launch {
            var lastSave = 0L; var downSince = 0L
            while (isActive) {
                val ok = poll()
                if (ok) { downSince = 0; if (!timeSynced) { timeSynced = syncPhoneTime() } } else { timeSynced = false
                    if (downSince == 0L) downSince = System.currentTimeMillis()
                    if (deviceId.isNotEmpty() && System.currentTimeMillis() - downSince > 8000) Discovery.hostFor(deviceId)?.let { if (it != host) { host = it; Ctl.deviceMoved(deviceId, it) } }   // adres moze sie zmienic (DHCP): znajdz po id
                }
                if (deviceId.isNotEmpty() && ok && System.currentTimeMillis() - lastSave > 15000) { lastSave = System.currentTimeMillis(); DeviceStore.cachePut(Ctl.ctx, deviceId, _values.value) }
                delay(1000)
            }
        }
    }
    private var authNotified = false
    private suspend fun syncPhoneTime(): Boolean {
        val phone = System.currentTimeMillis() / 1000L
        val j = call("GET", "/api/time")
        val boiler = j.optLong("boiler_timestamp", 0L)
        if (boiler > 0L && kotlin.math.abs(phone - boiler) <= 2L) return true
        val p = call("POST", "/api/time", JSONObject().put("epoch", phone))
        if (!p.optBoolean("ok")) { Ctl.notify("Nie udało się zsynchronizować zegara kotła: " + err(p)); return false }
        val rb = p.optLong("boiler_timestamp", 0L)
        if (rb != phone) { Ctl.notify("Kocioł nie potwierdził synchronizacji zegara."); return false }
        Ctl.notify("Zegar kotła zsynchronizowany z telefonem.")
        return true
    }

    private suspend fun poll(): Boolean {
        val j = call("GET", "/api/state")
        if (!j.optBoolean("ok")) { connected.value = false; if (j.optString("code") == "unauthorized" && !authNotified) { authNotified = true; Ctl.notify("Moduł odwołał to połączenie - sparuj go ponownie w Ustawieniach.") }; return false }
        connected.value = true; fullMode.value = true; val v = j.optJSONObject("v") ?: return true; val m = HashMap(_values.value)
        for (k in v.keys()) m[normPid(k)] = v.getDouble(k); _values.value = m
        
        return true
    }
    override suspend fun read(pids: List<String>): Map<String, Double> {
        val out = HashMap<String, Double>()
        for (chunk in pids.chunked(24)) { val j = call("GET", "/api/params?pids=" + chunk.joinToString(",")); val v = j.optJSONObject("values") ?: continue; for (k in v.keys()) out[normPid(k)] = v.getDouble(k) }
        return out
    }
    override suspend fun write(pid: String, v: Double): String? {
        offline()?.let { return it }
        val j = call("PUT", "/api/param/" + fmtPid(pid), JSONObject().put("value", v)); if (!j.optBoolean("ok")) return err(j)
        _values.value = HashMap(_values.value).also { it[fmtPid(pid)] = j.optDouble("v", v) }; return null
    }
    override suspend fun unlock(pin: String): String? { val l = BoilerAccess.unlock(pin); level.value = l; return if (l > 0) null else "Błędny PIN poziomu" }
    override suspend fun setFullMode(full: Boolean, password: String): String? = "ESP jest zawsze w trybie Full; poziom sterowania ustawia aplikacja."

    override suspend fun alarms(): List<AlarmRow> = (call("GET", "/api/alarms").optJSONArray("alarms")?.objs() ?: emptyList()).map {
        AlarmRow(it.optInt("n"), it.optInt("code"), it.optString("text"), if (it.optBoolean("active")) "AKTYWNY" else if (it.optBoolean("acked")) "potwierdzony" else "nowy")
    }
    override suspend fun network(): List<NodeRow> = (call("GET", "/api/network").optJSONArray("nodes")?.objs() ?: emptyList()).map { NodeRow(it.optInt("node"), it.optString("name"), it.optString("state")) }
    override fun close() { job?.cancel(); job = null }
    // ---- operacje serwisowe: najpierw odblokowanie haslem (5 min); haslo trzymane tylko w pamieci procesu i odnawiane automatycznie
    override suspend fun serviceUnlock(password: String): String? { offline()?.let { return it }; val j = call("POST", "/api/service/unlock", JSONObject().put("password", password)); if (!j.optBoolean("ok")) return err(j); svcPw = password; return null }
    private suspend fun svc(): String? { offline()?.let { return it }; val pw = svcPw ?: return "Najpierw odblokuj operacje serwisowe hasłem urządzenia."; val j = call("POST", "/api/service/unlock", JSONObject().put("password", pw)); return if (j.optBoolean("ok")) null else { svcPw = null; err(j) } }
    private suspend fun svcCall(method: String, path: String, body: JSONObject? = null): String? { svc()?.let { return it }; val j = call(method, path, body); return if (j.optBoolean("ok")) null else err(j) }
    override suspend fun wifiStatus(): String { val j = call("GET", "/api/wifi"); return if (j.optBoolean("ok")) j.optString("ssid", "--") + if (j.optBoolean("connected")) " ●" else " ○" else err(j) }
    override suspend fun wifiScan(): List<Pair<String, Int>> { if (svc() != null) return emptyList(); return (call("GET", "/api/wifi/scan").optJSONArray("networks")?.objs() ?: emptyList()).map { it.optString("ssid") to it.optInt("rssi") }.distinctBy { it.first } }
    override suspend fun wifiSave(ssid: String, pass: String): String? = svcCall("POST", "/api/wifi", JSONObject().put("ssid", ssid).put("pass", pass))
    override suspend fun changePassword(newPw: String): String? { val e = svcCall("POST", "/api/password", JSONObject().put("new", newPw)); if (e == null) svcPw = newPw; return e }
    override suspend fun restart(password: String): String? { if (password.isNotEmpty()) svcPw = password; return svcCall("POST", "/api/restart") }
    override suspend fun factoryReset(password: String): String? { if (password.isNotEmpty()) svcPw = password; return svcCall("POST", "/api/factory_reset", JSONObject().put("confirm", "RESET")) }
    override suspend fun clients(): List<Pair<Int, String>> { if (svc() != null) return emptyList(); return (call("GET", "/api/clients").optJSONArray("clients")?.objs() ?: emptyList()).map { it.optInt("i") to it.optString("name") } }
    override suspend fun revokeClient(i: Int): String? = svcCall("DELETE", "/api/clients/$i")
    override suspend fun setDeviceName(name: String): String? = svcCall("POST", "/api/device", JSONObject().put("name", name))
    override suspend fun setRecovery(code: String): String? = svcCall("POST", "/api/recovery", JSONObject().put("code", code))
    override suspend fun ipInfo(): IpInfo? {
        if (svc() != null) return null; val j = call("GET", "/api/ip"); if (!j.optBoolean("ok")) return null
        val c = j.optJSONObject("current"); val sv = j.optJSONObject("saved")
        return IpInfo(sv?.optBoolean("dhcp", true) ?: true, c?.optString("ip") ?: "", c?.optString("mask") ?: "", c?.optString("gw") ?: "", c?.optString("dns1") ?: "", c?.optString("dns2") ?: "")
    }
    /** Zmiana IP: POST /api/ip, potem w 60 s potwierdzenie z NOWEGO adresu (staly: podany; DHCP: adres znaleziony w mDNS po id). Bez potwierdzenia modul sam wraca do poprzednich ustawien. */
    override suspend fun ipApply(i: IpInfo): String? {
        svc()?.let { return it }
        val body = JSONObject().put("dhcp", i.dhcp); if (!i.dhcp) body.put("ip", i.ip).put("mask", i.mask).put("gw", i.gw).put("dns1", i.dns1).put("dns2", i.dns2)
        val j = call("POST", "/api/ip", body); if (!j.optBoolean("ok")) return if (j.optString("code") == "bad_ip") "Niepoprawny adres (maska, brama w tej samej podsieci, nie adres sieci/rozgłoszeniowy)." else err(j)
        val old = host; val newIp = j.optString("new_ip").ifEmpty { if (i.dhcp) "" else i.ip }
        val end = System.currentTimeMillis() + 55000
        while (System.currentTimeMillis() < end) {
            delay(2500)
            for (h in listOfNotNull(newIp.ifEmpty { null }, if (deviceId.isNotEmpty()) Discovery.hostFor(deviceId) else null, old).distinct()) {
                host = h; val r = call("POST", "/api/net/confirm")
                if (r.optBoolean("ok")) { if (deviceId.isNotEmpty()) Ctl.deviceMoved(deviceId, h); return null }
            }
        }
        host = old; return "Nie udało się potwierdzić nowego adresu w 60 s - moduł wraca do poprzednich ustawień."
    }
    override suspend fun log(): List<String> = call("GET", "/api/log").optJSONArray("lines")?.strs() ?: emptyList()
    override suspend fun ota(kind: String, data: ByteArray): String? = withContext(Dispatchers.IO) {
        svc()?.let { return@withContext it }
        try {
            val req = Request.Builder().url("${base()}/api/ota/$kind").post(data.toRequestBody("application/octet-stream".toMediaType())).apply { token?.let { header("Authorization", "Bearer $it") } }.build()
            http.newBuilder().readTimeout(120, TimeUnit.SECONDS).build().newCall(req).execute().use { r -> val j = try { JSONObject(r.body?.string() ?: "{}") } catch (e: Exception) { JSONObject() }; if (j.optBoolean("ok")) null else ERR[j.optString("code")] ?: ("Błąd: " + j.optString("code")) }
        } catch (e: Exception) { e.message ?: "brak połączenia" }
    }
}

/** Pierwsze parowanie: sprawdza tozsamosc (api == 2), potem POST /api/pair haslem -> staly token klienta. Zwraca sparowany modul albo komunikat bledu. */
suspend fun pairDevice(host: String, password: String, clientName: String): Pair<Paired?, String?> {
    val rs = RemoteSource(host, Ctl.scope)
    try {
        val id = rs.call("GET", "/api/identity"); if (!id.optBoolean("ok")) return null to "Nie znaleziono modułu pod adresem $host."
        if (id.optInt("api") != 2) return null to "Niezgodna wersja API modułu (${id.optInt("api")}); zaktualizuj aplikację lub firmware."
        if (id.optBoolean("setup_needed")) return null to "Moduł nie jest skonfigurowany - użyj „Nowy moduł (Bluetooth)” albo strony serwisowej."
        val j = rs.call("POST", "/api/pair", JSONObject().put("password", password).put("name", clientName))
        if (!j.optBoolean("ok")) return null to (ERR[j.optString("code")] ?: when (j.optString("code")) { "too_many_clients" -> "Moduł ma już 8 sparowanych klientów - usuń któregoś na stronie serwisowej."; else -> "Błąd parowania: " + j.optString("code") })
        return Paired(id.getString("id"), id.optString("name", "TeklaPellet"), host, j.getString("token")) to null
    } finally { rs.close() }
}

/** Zapomniane haslo: kod odzyskiwania + nowe haslo (bez fizycznego dostepu). */
suspend fun recoverPassword(host: String, code: String, newPassword: String): String? {
    val rs = RemoteSource(host, Ctl.scope); try {
        val j = rs.call("POST", "/api/recover", JSONObject().put("code", code.trim().uppercase()).put("new", newPassword)); if (j.optBoolean("ok")) return null
        return when (j.optString("code")) { "bad_code" -> "Błędny kod odzyskiwania."; "no_recovery" -> "Ten moduł nie ma ustawionego kodu odzyskiwania."; "locked" -> "Zbyt wiele prób - spróbuj za minutę."; "bad_request" -> "Nowe hasło musi mieć min. 8 znaków."; else -> rs.callMsg(j) }
    } finally { rs.close() }
}
private fun RemoteSource.callMsg(j: JSONObject) = j.optString("msg").ifEmpty { "Błąd: " + j.optString("code") }

/** Kod odzyskiwania generowany w aplikacji (20 znakow, ~100 bitow): XXXX-XXXX-XXXX-XXXX-XXXX. */
fun newRecoveryCode(): String { val a = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; val r = java.security.SecureRandom(); return (0 until 20).map { a[r.nextInt(32)] }.chunked(4).joinToString("-") { it.joinToString("") } }
