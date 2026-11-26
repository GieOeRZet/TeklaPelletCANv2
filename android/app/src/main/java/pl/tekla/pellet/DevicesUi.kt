package pl.tekla.pellet

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Okno parowania z modulem w sieci: haslo urzadzenia (raz) -> staly token. Zapomniane haslo: kod odzyskiwania + nowe haslo. */
@Composable
fun PairDialog(host: String, label: String, close: () -> Unit, done: (Paired) -> Unit) {
    var pw by remember { mutableStateOf("") }; var code by remember { mutableStateOf("") }; var recover by remember { mutableStateOf(false) }; var err by remember { mutableStateOf("") }; var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!busy) close() }, title = { Text(if (recover) "Odzyskiwanie hasła" else "Sparuj: $label") }, text = {
        Column {
            if (recover) {
                Text("Wpisz kod odzyskiwania zapisany przy konfiguracji modułu i nowe hasło (min. 8 znaków).", color = Mut, fontSize = 13.sp)
                OutlinedTextField(code, { code = it.uppercase() }, label = { Text("Kod odzyskiwania") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            } else Text("Hasło urządzenia jest potrzebne tylko teraz - aplikacja zapamięta zaszyfrowany token, a nie hasło.", color = Mut, fontSize = 13.sp)
            OutlinedTextField(pw, { pw = it }, label = { Text(if (recover) "Nowe hasło urządzenia" else "Hasło urządzenia") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
            if (err.isNotEmpty()) Text(err, color = Bad, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            TextButton({ recover = !recover; err = "" }) { Text(if (recover) "Wróć do parowania" else "Nie pamiętam hasła") }
        }
    }, confirmButton = {
        TextButton({
            busy = true; err = ""
            scope.launch {
                if (recover) { val e = recoverPassword(host, code, pw); if (e != null) { err = e; busy = false; return@launch } }
                val (p, e) = pairDevice(host, pw, Ctl.clientName); busy = false
                if (p != null) done(p) else err = e ?: "Błąd parowania"
            }
        }, enabled = !busy && pw.isNotEmpty() && (!recover || code.isNotBlank())) { Text(if (busy) "…" else if (recover) "Ustaw i sparuj" else "Sparuj") }
    }, dismissButton = { TextButton({ close() }, enabled = !busy) { Text("Anuluj") } })
}

@Composable
fun AppSettings(ui: Ui) {
    val scope = rememberCoroutineScope(); val found by Discovery.found.collectAsState()
    var host by remember { mutableStateOf("") }; var pairing by remember { mutableStateOf<Pair<String, String>?>(null) }; var forget by remember { mutableStateOf<Paired?>(null) }
    LaunchedEffect(Unit) { Discovery.start(Ctl.ctx) }
    Card("Moduły") {
        Text(if (ui.src.isDemo) "Tryb demo: pokazuję przykładowe wartości. Sparuj moduł, aby zobaczyć prawdziwe dane." else "Połączono z: ${Ctl.activeName()}. Gdy moduł jest offline, widać ostatnie znane wartości, a próba zmiany pokazuje komunikat.", color = Mut, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        for (d in Ctl.devices) Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { Ctl.activate(d.id); ui.go("home") }.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(if (d.id == Ctl.activeId) Okc else Mut, androidx.compose.foundation.shape.RoundedCornerShape(4.dp))); Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) { Text(d.name, color = Txt, fontSize = 15.sp); Text(d.host + " · " + d.id, color = Mut, fontSize = 11.sp) }
            TextButton({ forget = d }) { Text("Usuń", color = Bad) }
        }
        if (Ctl.devices.isEmpty()) { OutlinedButton({ Ctl.useDemo(); ui.go("home") }, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Uruchom demo", color = Txt) } }
    }
    val fresh = found.values.filter { f -> Ctl.devices.none { it.id == f.id } }
    Card("Wykryte w sieci") {
        if (fresh.isEmpty()) Text("Brak nowych modułów. Wykrywanie działa w tej samej sieci Wi-Fi (mDNS); jeśli moduł jest nieskonfigurowany, użyj „Nowy moduł (Bluetooth)” poniżej.", color = Mut, fontSize = 13.sp)
        for (f in fresh) Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable { pairing = f.host to f.name }.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(f.name, color = Txt, fontSize = 15.sp); Text(f.host + " · fw " + f.fw, color = Mut, fontSize = 11.sp) }; Text("Sparuj ›", color = Acc)
        }
    }
    Card("Dodaj ręcznie") {
        OutlinedTextField(host, { host = it }, label = { Text("Adres modułu (np. 192.168.1.50 lub VPN)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button({ pairing = host.trim() to host.trim() }, Modifier.fillMaxWidth().padding(top = 8.dp), enabled = host.isNotBlank()) { Text("Sparuj") }
    }
    BleSetupCard(ui)
    AppPinCard(ui)
    BoilerAccessCard(ui)
    pairing?.let { (h, n) -> PairDialog(h, n, { pairing = null }) { p -> pairing = null; Ctl.addPaired(p); ui.toast("Sparowano: ${p.name}"); ui.go("home") } }
    forget?.let { d -> AlertDialog(onDismissRequest = { forget = null }, title = { Text("Usunąć ${d.name}?") }, text = { Text("Aplikacja zapomni ten moduł. Token zostaje w module do czasu usunięcia na stronie serwisowej lub w Module ESP › Sparowani klienci.") },
        confirmButton = { TextButton({ Ctl.forget(d.id); forget = null }) { Text("Usuń", color = Bad) } }, dismissButton = { TextButton({ forget = null }) { Text("Anuluj") } }) }
}

private fun phoneSsids(c: Context): List<String> = try {
    val ok = c.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    @Suppress("DEPRECATION") if (!ok) emptyList() else (c.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager).scanResults.map { it.SSID }.filter { it.isNotEmpty() }.distinct()
} catch (e: Exception) { emptyList() }

@Composable
private fun BoilerAccessCard(ui: Ui) {
    var p1 by remember { mutableStateOf("") }
    var p2 by remember { mutableStateOf("") }
    var p3 by remember { mutableStateOf("") }
    Card("Poziomy dostępu kotła") {
        Text("PIN-y są przechowywane wyłącznie w Androidzie. ESP pozostaje Full i nigdy ich nie otrzymuje.", color = Mut, fontSize = 13.sp)
        Text("Aktualny poziom: \${BoilerAccess.level()}", color = Acc, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
        for (level in 1..3) {
            val value = when (level) { 1 -> p1; 2 -> p2; else -> p3 }
            val set = BoilerAccess.hasPin(level)
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value, { v ->
                    val z = v.filter(Char::isDigit).take(4)
                    when (level) { 1 -> p1 = z; 2 -> p2 = z; else -> p3 = z }
                }, label = { Text("PIN poziomu \$level") }, singleLine = true, visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                Button({
                    if (value.length == 4) { BoilerAccess.setPin(level, value); when (level) { 1 -> p1 = ""; 2 -> p2 = ""; else -> p3 = "" }; ui.toast("PIN poziomu \$level ustawiony") }
                    else ui.toast("PIN poziomu \$level musi mieć 4 cyfry")
                }) { Text(if (set) "Zmień" else "Ustaw") }
                if (set) TextButton({ BoilerAccess.clearPin(level); ui.toast("PIN poziomu \$level usunięty") }) { Text("×", color = Bad) }
            }
        }
    }
}

@Composable
private fun PinField(label: String, v: String, set: (String) -> Unit) = OutlinedTextField(v, set, label = { Text(label) }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword), modifier = Modifier.fillMaxWidth())

/** Pierwsza konfiguracja modulu przez Bluetooth: wykrycie -> Wi-Fi (z listy sieci telefonu) + haslo urzadzenia/PIN-y -> modul laczy sie z Wi-Fi, BLE sie wylacza -> aplikacja paruje automatycznie. */
@Composable
fun BleSetupCard(ui: Ui) {
    val scope = rememberCoroutineScope(); val ctx = Ctl.ctx; val devs by Ble.devices.collectAsState(); val clip = LocalClipboardManager.current
    var scanning by remember { mutableStateOf(false) }; var sel by remember { mutableStateOf<BleDev?>(null) }; var info by remember { mutableStateOf<JSONObject?>(null) }
    var ssid by remember { mutableStateOf("") }; var wpw by remember { mutableStateOf("") }; var pw by remember { mutableStateOf("") }
    var useRec by remember { mutableStateOf(false) }; var rec by remember { mutableStateOf("") }; var msg by remember { mutableStateOf("") }; var bad by remember { mutableStateOf(false) }; var busy by remember { mutableStateOf(false) }
    var newCode by remember { mutableStateOf<String?>(null) }; var nets by remember { mutableStateOf<List<String>>(emptyList()) }
    fun scan() { scanning = Ble.startScan(ctx); if (!scanning) { msg = "Włącz Bluetooth w telefonie."; bad = true } else { msg = ""; nets = phoneSsids(ctx) } }
    val perms = if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION) else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r -> if (Build.VERSION.SDK_INT >= 31 && (r[Manifest.permission.BLUETOOTH_SCAN] != true || r[Manifest.permission.BLUETOOTH_CONNECT] != true)) { msg = "Bez uprawnienia Bluetooth nie wykryję modułu."; bad = true } else scan() }
    DisposableEffect(Unit) { onDispose { Ble.stopScan() } }
    Card("Nowy moduł (Bluetooth)") {
        val setup = info?.optBoolean("setup_needed") == true
        if (newCode != null) {
            Text("Kod odzyskiwania", color = Txt, fontWeight = FontWeight.SemiBold); Text(newCode!!, color = Acc2, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Zapisz go w bezpiecznym miejscu. Jest widoczny tylko teraz i pozwala ustawić nowe hasło bez fizycznego dostępu do modułu.", color = Mut, fontSize = 12.sp)
            Row { TextButton({ clip.setText(AnnotatedString(newCode!!)) }) { Text("Kopiuj") }; TextButton({ newCode = null }) { Text("Zapisałem") } }
        }
        if (sel == null) {
            Text("Gdy moduł jest nowy albo nie ma Wi-Fi, udostępnia Bluetooth tylko na czas konfiguracji (po połączeniu z siecią Bluetooth się wyłącza).", color = Mut, fontSize = 13.sp)
            Button({ launcher.launch(perms) }, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(if (scanning) "Szukam… (odśwież)" else "Szukaj modułów") }
            for (d in devs.values.sortedByDescending { it.rssi }) Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable {
                Ble.stopScan(); scanning = false; sel = d; info = null; msg = "Łączę z ${d.name}…"; bad = false
                scope.launch { val s = Ble.Session(ctx, d.address); val e = s.connect(); val i = if (e == null) s.info() else null; s.close(); if (i != null) { info = i; msg = "" } else { msg = e ?: "Nie udało się odczytać modułu."; bad = true; sel = null } }
            }.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) { Text(d.name, color = Txt, modifier = Modifier.weight(1f)); Text("${d.rssi} dBm", color = Mut, fontSize = 12.sp) }
        } else if (info != null) {
            val i = info!!
            Text("${i.optString("name")} · ${i.optString("id")} · fw ${i.optString("fw")}", color = Mut, fontSize = 12.sp)
            if (i.optInt("api") != 2) Text("Niezgodna wersja API modułu.", color = Bad, fontSize = 13.sp)
            Text("Sieć Wi-Fi", color = Txt, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            if (nets.isNotEmpty()) Row(Modifier.fillMaxWidth()) { Column { for (n in nets.take(6)) TextButton({ ssid = n }) { Text(n, color = if (n == ssid) Acc else Txt) } } }
            OutlinedTextField(ssid, { ssid = it }, label = { Text("Nazwa sieci (SSID)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(wpw, { wpw = it }, label = { Text("Hasło Wi-Fi") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            if (setup) {
                Text("Hasło urządzenia służy wyłącznie do konfiguracji modułu. Poziomy dostępu kotła ustawiasz w aplikacji.", color = Mut, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                OutlinedTextField(pw, { pw = it }, label = { Text("Hasło urządzenia (min. 8 znaków)") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            } else {
                if (useRec) { OutlinedTextField(rec, { rec = it.uppercase() }, label = { Text("Kod odzyskiwania") }, singleLine = true, modifier = Modifier.fillMaxWidth()); OutlinedTextField(pw, { pw = it }, label = { Text("Nowe hasło urządzenia") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()) }
                else OutlinedTextField(pw, { pw = it }, label = { Text("Hasło urządzenia") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
                TextButton({ useRec = !useRec }) { Text(if (useRec) "Mam hasło" else "Nie pamiętam hasła (kod odzyskiwania)") }
            }
            val ready = !busy && ssid.isNotBlank() && pw.length >= (if (setup || useRec) 8 else 1) && (!useRec || rec.isNotBlank())
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({
                    busy = true; bad = false; msg = "Wysyłam konfigurację…"; val d = sel!!; val code = if (setup) newRecoveryCode() else null
                    scope.launch {
                        val j = JSONObject().put("ssid", ssid).put("wifi_pass", wpw)
                        if (setup) j.put("password", pw).put("recovery", code) else if (useRec) j.put("recovery", rec.trim()).put("new_password", pw) else j.put("password", pw)
                        val s = Ble.Session(ctx, d.address); var e = s.connect(); var st = -1
                        if (e == null) { val r = s.provision(j); st = r.first; e = r.second }
                        s.close(); if (e == null) e = bleStateMsg(st)
                        if (e != null) { msg = e; bad = true; busy = false; return@launch }
                        val id = i.optString("id"); if (code != null) newCode = code
                        msg = "Moduł łączy się z Wi-Fi… (do 90 s)"; var p: Paired? = null
                        for (k in 0 until 45) { delay(2000); val f = Discovery.found.value[id]; if (f != null) { val (pp, _) = pairDevice(f.host, pw, Ctl.clientName); if (pp != null) { p = pp; break } } }
                        busy = false
                        if (p != null) { Ctl.addPaired(p); msg = "Gotowe: moduł sparowany."; bad = false; sel = null; info = null; pw = ""; wpw = ""; ui.go("home") } else { msg = "Moduł zapisał konfigurację, ale nie pojawił się w sieci. Sprawdź hasło Wi-Fi i spróbuj „Wykryte w sieci”."; bad = true }
                    }
                }, enabled = ready) { Text(if (busy) "…" else "Skonfiguruj") }
                OutlinedButton({ sel = null; info = null; msg = "" }, enabled = !busy) { Text("Anuluj", color = Txt) }
            }
        }
        if (msg.isNotEmpty()) Text(msg, color = if (bad) Bad else Mut, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
    }
}
