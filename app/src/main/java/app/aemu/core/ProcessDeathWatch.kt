package app.aemu.core

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Наблюдение за чужим процессом через его Binder: «жив» или уже умер.
 *
 * Нужно перезапуску окна системы: ждать смерти процесса :vm надо ровно в тот момент, когда она
 * случилась, — без опроса по таймеру и без задержек.
 */
interface ProcessDeathWatch {
    val alive: Boolean
    fun register(onDeath: () -> Unit)
    fun unregister()
}

/** Приостанавливает корутину до смерти процесса; при отмене наблюдение снимается. */
suspend fun awaitVmProcessExit(watch: ProcessDeathWatch) {
    if (!watch.alive) return
    suspendCancellableCoroutine { cont ->
        watch.register { if (cont.isActive) cont.resume(Unit) }
        cont.invokeOnCancellation { watch.unregister() }
    }
}
