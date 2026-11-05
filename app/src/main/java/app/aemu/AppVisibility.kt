package app.aemu

/**
 * Сколько экранов приложения сейчас видно. Счётчик ведёт [AemuApp] по жизненному циклу активностей; службам он
 * нужен, чтобы не показывать уведомление о результате, пока пользователь и так смотрит на приложение.
 */
object AppVisibility {
    @Volatile private var started = 0

    val visible: Boolean get() = started > 0

    fun onStart() { started++ }

    fun onStop() { if (started > 0) started-- }
}
