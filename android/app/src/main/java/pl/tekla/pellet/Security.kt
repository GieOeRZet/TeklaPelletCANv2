package pl.tekla.pellet

import android.content.Context
import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** PIN aplikacji (blokada ekranu): PBKDF2-SHA256 (10000 iteracji, sol 16 B) w SharedPreferences; 5 błędnych prób = blokada 60 s. Nie chroni urządzenia ESP (to robi hasło + PIN-y poziomów). */
object AppLock {
    private fun p() = Ctl.ctx.getSharedPreferences("lock", Context.MODE_PRIVATE)
    private fun hash(pin: String, salt: ByteArray): ByteArray = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(pin.toCharArray(), salt, 10000, 256)).encoded
    fun isSet(): Boolean = p().contains("h")
    fun set(pin: String) { val s = ByteArray(16).also { SecureRandom().nextBytes(it) }; p().edit().putString("s", Base64.encodeToString(s, 0)).putString("h", Base64.encodeToString(hash(pin, s), 0)).apply() }
    fun clear() { p().edit().clear().apply() }
    private var fails = 0; private var until = 0L
    /** null = OK, inaczej komunikat. */
    fun check(pin: String): String? {
        val now = System.currentTimeMillis(); if (now < until) return "Zablokowane - spróbuj za ${(until - now) / 1000 + 1} s"
        val s = Base64.decode(p().getString("s", "") ?: "", 0); val h = Base64.decode(p().getString("h", "") ?: "", 0)
        if (java.security.MessageDigest.isEqual(hash(pin, s), h)) { fails = 0; return null }
        if (++fails >= 5) { until = now + 60000; fails = 0 }; return "Błędny PIN"
    }
}
