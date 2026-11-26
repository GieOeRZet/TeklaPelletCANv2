package pl.tekla.pellet

import android.content.Context
import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Lokalna blokada aplikacji. Nie ma żadnego związku z hasłem/tokenem ESP. */
object AppLock {
    private const val ITER = 120_000
    private fun p() = Ctl.ctx.getSharedPreferences("lock", Context.MODE_PRIVATE)
    private fun hash(pin: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(pin.toCharArray(), salt, ITER, 256)).encoded

    fun isSet(): Boolean = p().contains("h")
    fun set(pin: String) {
        val s = ByteArray(16).also { SecureRandom().nextBytes(it) }
        p().edit().putString("s", Base64.encodeToString(s, Base64.NO_WRAP))
            .putString("h", Base64.encodeToString(hash(pin, s), Base64.NO_WRAP)).apply()
    }
    fun clear() { p().edit().clear().apply() }
    fun biometricEnabled(): Boolean = isSet() && p().getBoolean("biometric", false)
    fun setBiometric(enabled: Boolean) { if (isSet()) p().edit().putBoolean("biometric", enabled).apply() }

    private var fails = 0
    private var until = 0L
    fun check(pin: String): String? {
        val now = System.currentTimeMillis()
        if (now < until) return "Zablokowane - spróbuj za " + ((until - now) / 1000 + 1) + " s"
        val s0 = p().getString("s", "") ?: ""
        val h0 = p().getString("h", "") ?: ""
        if (s0.isEmpty() || h0.isEmpty()) return "Blokada aplikacji jest nieskonfigurowana"
        val s = Base64.decode(s0, Base64.DEFAULT)
        val h = Base64.decode(h0, Base64.DEFAULT)
        if (MessageDigest.isEqual(hash(pin, s), h)) { fails = 0; return null }
        if (++fails >= 5) { until = now + 60000; fails = 0 }
        return "Błędny PIN"
    }
}

/** Poziomy dostępu Estyma odtworzone po stronie Androida. ESP nie zna tych PIN-ów. */
object BoilerAccess {
    private const val ITER = 120_000
    private fun p() = Ctl.ctx.getSharedPreferences("boiler_access", Context.MODE_PRIVATE)
    private fun hash(pin: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(pin.toCharArray(), salt, ITER, 256)).encoded

    fun level(): Int = p().getInt("level", 0)
    fun setPin(level: Int, pin: String): Boolean {
        if (level !in 1..3 || pin.length != 4) return false
        val s = ByteArray(16).also { SecureRandom().nextBytes(it) }
        p().edit().putString("s$level", Base64.encodeToString(s, Base64.NO_WRAP))
            .putString("h$level", Base64.encodeToString(hash(pin, s), Base64.NO_WRAP)).apply()
        return true
    }
    fun clearPin(level: Int) { if (level in 1..3) p().edit().remove("s$level").remove("h$level").apply() }
    fun hasPin(level: Int): Boolean = p().contains("h$level")

    fun unlock(pin: String): Int {
        for (level in 3 downTo 1) {
            val s0 = p().getString("s$level", "") ?: ""
            val h0 = p().getString("h$level", "") ?: ""
            if (s0.isNotEmpty() && h0.isNotEmpty()) {
                val s = Base64.decode(s0, Base64.DEFAULT)
                val h = Base64.decode(h0, Base64.DEFAULT)
                if (MessageDigest.isEqual(hash(pin, s), h)) {
                    p().edit().putInt("level", level).apply()
                    return level
                }
            }
        }
        p().edit().putInt("level", 0).apply()
        return 0
    }
    fun lock() { p().edit().putInt("level", 0).apply() }
}
