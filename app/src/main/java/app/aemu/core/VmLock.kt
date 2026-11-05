package app.aemu.core

import android.content.Context
import android.util.Base64
import java.io.File
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Пароль на отдельную машину. Хранится только соль и медленный хеш (PBKDF2-HMAC-SHA1), рядом с образом: файл
 * `lock` в его каталоге. Файл удаляется вместе с образом; контейнер, снятый с машины, пароля не наследует.
 */
object VmLock {
    private const val FILE = "lock"
    private const val ITERATIONS = 50_000
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256

    private fun file(ctx: Context, id: String): File = File(VmPaths(ctx, id).dir, FILE)

    fun isLocked(ctx: Context, id: String): Boolean = file(ctx, id).isFile

    /** Sets the password, or removes the lock when [password] is null/blank. */
    fun set(ctx: Context, id: String, password: String?) {
        val f = file(ctx, id)
        if (password.isNullOrBlank()) {
            runCatching { f.delete() }
            return
        }
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val hash = hash(password, salt, ITERATIONS)
        f.parentFile?.mkdirs()
        f.writeText("v1:$ITERATIONS:${b64(salt)}:${b64(hash)}")
    }

    fun check(ctx: Context, id: String, password: String): Boolean {
        val text = runCatching { file(ctx, id).readText() }.getOrNull() ?: return false
        val parts = text.trim().split(':')
        if (parts.size != 4 || parts[0] != "v1") return false
        val iterations = parts[1].toIntOrNull() ?: return false
        val salt = unb64(parts[2]) ?: return false
        val want = unb64(parts[3]) ?: return false
        return MessageDigest.isEqual(hash(password, salt, iterations), want)
    }

    private fun hash(password: String, salt: ByteArray, iterations: Int): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
            .generateSecret(PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS)).encoded

    private fun b64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun unb64(text: String) = runCatching { Base64.decode(text, Base64.NO_WRAP) }.getOrNull()
}
