package app.aemu.core

import android.content.Context
import java.io.File

/**
 * Место кнопки «…» на экране системы. Хранится обычным файлом, а не в SharedPreferences: место пишет и читает и
 * основной процесс (сброс из настроек), и процесс :vm, а кэш SharedPreferences у каждого процесса свой и сбросить
 * его извне нечем — из-за этого сброс «не срабатывал».
 */
object MenuButtonPos {
    private fun file(ctx: Context): File = File(ctx.filesDir, "menu_button.txt")

    /** x, y или null, если кнопку ещё не двигали. */
    fun read(ctx: Context): FloatArray? = runCatching {
        file(ctx).readText().trim().split(Regex("\\s+")).map { it.toFloat() }.toFloatArray()
    }.getOrNull()?.takeIf { it.size == 2 }

    fun write(ctx: Context, x: Float, y: Float) {
        val f = file(ctx)
        val text = "$x $y"
        // через временный файл, чтобы не оставить половину записи; renameTo может и не сработать
        val renamed = runCatching {
            val tmp = File(f.path + ".tmp")
            tmp.writeText(text)
            tmp.renameTo(f)
        }.getOrDefault(false)
        if (!renamed) runCatching { f.writeText(text) }
    }

    fun reset(ctx: Context) {
        runCatching { file(ctx).delete() }
    }
}
