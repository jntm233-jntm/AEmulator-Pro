package app.aemu.importer

import app.aemu.core.GuestImage
import kotlinx.coroutines.flow.MutableStateFlow

/** Что показывает главный экран про импорт: пишет служба, читает экран. */
data class ImportState(
    val active: Boolean = false,
    val file: String = "",
    val step: String = "",
    val progress: Float = -1f,
    val log: List<String> = emptyList(),
    val error: String? = null,
    val done: GuestImage? = null,
)

/**
 * Один импорт за раз: служба (в основном процессе) ведёт его и пишет сюда, экран читает. Состояние живёт, пока
 * жив процесс, поэтому импорт идёт и при свёрнутом приложении.
 */
object ImportBus {
    val state = MutableStateFlow(ImportState())

    /** Импортёр, который можно прервать из экрана или из уведомления. */
    @Volatile var importer: Importer? = null

    fun update(f: (ImportState) -> ImportState) {
        state.value = f(state.value)
    }

    fun reset() {
        state.value = ImportState()
    }

    /** «Отмена»: текущий шаг импортёра увидит флаг и завершится. */
    fun cancel() {
        importer?.cancelled = true
    }
}
