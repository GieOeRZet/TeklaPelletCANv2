package pl.tekla.pellet

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import java.util.UUID

class BleDev(val address: String, val name: String, val rssi: Int)

/** Klient BLE do pierwszej konfiguracji modulu (protokol: docs/API_ESP.md sekcja 12). Wymaga uprawnien BLUETOOTH_SCAN/CONNECT (Android 12+) albo lokalizacji (starsze). */
@SuppressLint("MissingPermission")
object Ble {
    val SVC: UUID = UUID.fromString("8f1c0001-5a7e-4c1b-9d2a-7e6b2f0a1001")
    private val INFO: UUID = UUID.fromString("8f1c0002-5a7e-4c1b-9d2a-7e6b2f0a1001")
    private val PROV: UUID = UUID.fromString("8f1c0003-5a7e-4c1b-9d2a-7e6b2f0a1001")
    val devices = MutableStateFlow<Map<String, BleDev>>(emptyMap())
    private var scanner: BluetoothLeScanner? = null
    private val cb = object : ScanCallback() {
        override fun onScanResult(t: Int, r: ScanResult) { val n = r.scanRecord?.deviceName ?: r.device.name ?: "TeklaPellet"; devices.value = devices.value + (r.device.address to BleDev(r.device.address, n, r.rssi)) }
    }
    private fun adapter(c: Context): BluetoothAdapter? = (c.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
    fun available(c: Context): Boolean = adapter(c)?.isEnabled == true
    fun startScan(c: Context): Boolean {
        val s = adapter(c)?.takeIf { it.isEnabled }?.bluetoothLeScanner ?: return false; devices.value = emptyMap(); scanner = s
        return try { s.startScan(listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(SVC)).build()), ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), cb); true } catch (e: Exception) { false }
    }
    fun stopScan() { try { scanner?.stopScan(cb) } catch (e: Exception) { }; scanner = null }

    /** Polaczenie z modulem; wynik: odczytany INFO (JSON) albo blad. Sesja zamykana przez close(). */
    class Session(val c: Context, val address: String) {
        private var gatt: BluetoothGatt? = null; private var info: BluetoothGattCharacteristic? = null; private var prov: BluetoothGattCharacteristic? = null
        private var step: CompletableDeferred<Int>? = null; private var read: CompletableDeferred<String?>? = null
        private val gcb = object : BluetoothGattCallback() {
            override fun onConnectionStateChange(g: BluetoothGatt, status: Int, state: Int) { if (state == BluetoothProfile.STATE_CONNECTED) g.requestMtu(256) else { step?.complete(-1); read?.complete(null) } }
            override fun onMtuChanged(g: BluetoothGatt, mtu: Int, status: Int) { g.discoverServices() }
            override fun onServicesDiscovered(g: BluetoothGatt, status: Int) { val s = g.getService(SVC); info = s?.getCharacteristic(INFO); prov = s?.getCharacteristic(PROV); step?.complete(if (info != null && prov != null) 0 else -2) }
            @Deprecated("API < 33") override fun onCharacteristicRead(g: BluetoothGatt, ch: BluetoothGattCharacteristic, status: Int) { @Suppress("DEPRECATION") read?.complete(if (status == 0) String(ch.value ?: ByteArray(0)) else null) }
            override fun onCharacteristicRead(g: BluetoothGatt, ch: BluetoothGattCharacteristic, value: ByteArray, status: Int) { read?.complete(if (status == 0) String(value) else null) }
            override fun onCharacteristicWrite(g: BluetoothGatt, ch: BluetoothGattCharacteristic, status: Int) { step?.complete(status) }
        }
        suspend fun connect(): String? {
            val dev = adapter(c)?.getRemoteDevice(address) ?: return "Bluetooth niedostępny"; step = CompletableDeferred()
            gatt = dev.connectGatt(c, false, gcb, BluetoothDevice.TRANSPORT_LE)
            val r = withTimeoutOrNull(15000) { step!!.await() } ?: return "Brak odpowiedzi modułu (Bluetooth)"
            return when (r) { 0 -> null; -2 -> "To nie jest moduł TeklaPellet"; else -> "Połączenie Bluetooth przerwane" }
        }
        suspend fun info(): JSONObject? {
            read = CompletableDeferred(); gatt?.readCharacteristic(info) ?: return null
            val s = withTimeoutOrNull(8000) { read!!.await() } ?: return null; return try { JSONObject(s) } catch (e: Exception) { null }
        }
        /** Zapis konfiguracji; przy pierwszym zapisie Android sam wymusza parowanie (szyfrowanie lacza) - wtedy ponawiamy. Zwraca kod stanu modulu (INFO.state) albo blad. */
        suspend fun provision(json: JSONObject): Pair<Int, String?> {
            val data = json.toString().toByteArray()
            for (attempt in 0..2) {
                step = CompletableDeferred(); prov!!.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                @Suppress("DEPRECATION") run { prov!!.value = data; gatt!!.writeCharacteristic(prov) }
                val st = withTimeoutOrNull(30000) { step!!.await() } ?: return -1 to "Brak potwierdzenia zapisu"
                if (st == 0) break
                if (attempt == 2 || (st != 5 && st != 15 && st != 8 && st != 137)) return -1 to "Zapis Bluetooth nieudany (kod $st)"
                delay(4000)   // trwa parowanie lacza (Just Works) - sprobuj ponownie
            }
            for (i in 0 until 40) { delay(500); val j = info() ?: continue; val s = j.optInt("state"); if (s != 1) return s to null }
            return -1 to "Brak wyniku od modułu"
        }
        fun close() { try { gatt?.close() } catch (e: Exception) { } ; gatt = null }
    }
}

fun bleStateMsg(s: Int): String? = when (s) { 2 -> null; 3 -> null; 4 -> "Błędne hasło urządzenia."; 5 -> "Zbyt wiele prób - spróbuj za minutę."; 6 -> "Błędne dane (hasło min. 8 znaków, SSID wymagany)."; 7 -> "Moduł nie mógł zapisać konfiguracji."; else -> "Nieznany wynik ($s)" }
