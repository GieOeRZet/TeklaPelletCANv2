package pl.tekla.pellet

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Sparowany modul: klucz = id z /api/identity (a nie adres IP); token klienta z POST /api/pair. */
class Paired(val id: String, var name: String, var host: String, val token: String)

/** Szyfrowanie tokenow kluczem z Android Keystore (AES-GCM); w SharedPreferences leza tylko dane zaszyfrowane. */
object Vault {
    private const val ALIAS = "tp_vault"
    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val g = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        g.init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        return g.generateKey()
    }
    fun seal(text: String): String { val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, key()); val ct = c.doFinal(text.toByteArray()); return Base64.encodeToString(c.iv + ct, Base64.NO_WRAP) }
    fun open(s: String): String? = try { val b = Base64.decode(s, Base64.NO_WRAP); val c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, b, 0, 12)); String(c.doFinal(b, 12, b.size - 12)) } catch (e: Exception) { null }
}

/** Lista sparowanych modulow + aktywny modul (zapis zaszyfrowany). */
object DeviceStore {
    private fun prefs(c: Context) = c.getSharedPreferences("tp_devices", Context.MODE_PRIVATE)
    fun load(c: Context): MutableList<Paired> {
        val raw = prefs(c).getString("list", null)?.let { Vault.open(it) } ?: return mutableListOf()
        return try { val a = JSONArray(raw); MutableList(a.length()) { val o = a.getJSONObject(it); Paired(o.getString("id"), o.optString("name"), o.getString("host"), o.getString("token")) } } catch (e: Exception) { mutableListOf() }
    }
    fun save(c: Context, l: List<Paired>) {
        val a = JSONArray(); for (d in l) a.put(JSONObject().put("id", d.id).put("name", d.name).put("host", d.host).put("token", d.token))
        prefs(c).edit().putString("list", Vault.seal(a.toString())).apply()
    }
    fun active(c: Context): String? = prefs(c).getString("active", null)
    fun setActive(c: Context, id: String?) { prefs(c).edit().putString("active", id).apply() }
    fun cacheGet(c: Context, id: String): Map<String, Double> = try { val o = JSONObject(prefs(c).getString("cache_$id", "{}") ?: "{}"); o.keys().asSequence().associateWith { o.getDouble(it) } } catch (e: Exception) { emptyMap() }
    fun cachePut(c: Context, id: String, m: Map<String, Double>) { val o = JSONObject(); for ((k, v) in m) if (!v.isNaN()) o.put(k, v); prefs(c).edit().putString("cache_$id", o.toString()).apply() }
}

class Found(val id: String, val name: String, val host: String, val fw: String, val api: String)

/** Wykrywanie modulow w sieci (mDNS/NSD: _teklapellet._tcp, TXT: id, name, fw, api). */
object Discovery {
    val found = MutableStateFlow<Map<String, Found>>(emptyMap())
    private var mgr: NsdManager? = null; private var listener: NsdManager.DiscoveryListener? = null
    private val queue = ArrayDeque<NsdServiceInfo>(); private var resolving = false
    private fun next() {
        val m = mgr ?: return; if (resolving) return; val s = queue.removeFirstOrNull() ?: return; resolving = true
        @Suppress("DEPRECATION") m.resolveService(s, object : NsdManager.ResolveListener {
            override fun onResolveFailed(si: NsdServiceInfo, e: Int) { resolving = false; next() }
            override fun onServiceResolved(si: NsdServiceInfo) {
                resolving = false
                fun tx(k: String) = si.attributes[k]?.let { String(it) } ?: ""
                val id = tx("id"); @Suppress("DEPRECATION") val host = si.host?.hostAddress
                if (id.isNotEmpty() && host != null) found.value = found.value + (id to Found(id, tx("name").ifEmpty { si.serviceName }, host, tx("fw"), tx("api")))
                next()
            }
        })
    }
    fun start(c: Context) {
        if (listener != null) return
        val m = c.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager; mgr = m
        val l = object : NsdManager.DiscoveryListener {
            override fun onStartDiscoveryFailed(t: String, e: Int) { listener = null }
            override fun onStopDiscoveryFailed(t: String, e: Int) {}
            override fun onDiscoveryStarted(t: String) {}
            override fun onDiscoveryStopped(t: String) {}
            override fun onServiceFound(s: NsdServiceInfo) { queue.addLast(s); next() }
            override fun onServiceLost(s: NsdServiceInfo) {}
        }
        listener = l; try { m.discoverServices("_teklapellet._tcp", NsdManager.PROTOCOL_DNS_SD, l) } catch (e: Exception) { listener = null }
    }
    fun stop() { try { listener?.let { mgr?.stopServiceDiscovery(it) } } catch (e: Exception) { }; listener = null }
    fun hostFor(id: String): String? = found.value[id]?.host
}
