package app.aemu.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle

/**
 * «Живые обновления» Android 16 (live updates; на ColorOS это «облачко» в строке состояния).
 *
 * Чтобы система подняла обычное уведомление до такого вида, нужны все три условия:
 *  - разрешение `POST_PROMOTED_NOTIFICATIONS` в манифесте (иначе приложение даже не появляется
 *    в системных настройках «живых обновлений»);
 *  - канал с IMPORTANCE_DEFAULT (на LOW система уведомление не повышает) без звука и вибрации;
 *  - запрос повышения в самом уведомлении: флаг `FLAG_PROMOTED_ONGOING` (Android 16) и
 *    `setRequestPromotedOngoing` (Android 17, вызываем через отражение — в SDK 36 его ещё нет).
 */
object LiveNotes {
    /** система умеет «живые обновления» */
    fun supported(): Boolean = Build.VERSION.SDK_INT >= 36

    /** включено в настройках и система это умеет */
    fun on(ctx: Context): Boolean = supported() && app.aemu.AppPrefs.liveUpdatesNow(ctx)

    /**
     * Канал для уведомления. Важность — DEFAULT (иначе повышение невозможно), но без звука,
     * вибрации и значка: уведомление остаётся тихим. Старый канал с низкой важностью удаляем,
     * иначе он остался бы в настройках и продолжал бы глушить уведомления.
     *
     * Канал создаётся один раз за процесс: раньше на каждый вызов шли обращения к системной службе
     * (get/delete/create), и это заметно тормозило запуск приложения и системы.
     */
    private val ready = java.util.Collections.synchronizedSet(HashSet<String>())

    fun channel(ctx: Context, id: String, legacyId: String, name: String): String {
        if (id in ready) return id
        runCatching {
            val nm = ctx.getSystemService(NotificationManager::class.java) ?: return id
            if (nm.getNotificationChannel(id) != null) { ready.add(id); return id }
            runCatching { nm.deleteNotificationChannel(legacyId) }
            nm.createNotificationChannel(NotificationChannel(id, name, NotificationManager.IMPORTANCE_DEFAULT).apply {
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
            })
            ready.add(id)
        }
        return id
    }

    /** Просит повысить уведомление до «живого»; [shortText] — короткая подпись в строке состояния. */
    fun promote(builder: Notification.Builder, shortText: String?) {
        if (!supported()) return
        if (!shortText.isNullOrBlank()) runCatching { builder.setShortCriticalText(shortText.take(7)) }
        // Android 16: флаг в самом уведомлении
        runCatching { builder.setFlag(Notification.FLAG_PROMOTED_ONGOING, true) }
        // тот же запрос в extras — так его передаёт androidx.core
        runCatching { builder.addExtras(Bundle().apply { putBoolean("android.requestPromotedOngoing", true) }) }
        // Android 17: обычный метод Builder (в SDK 36 его нет, поэтому через отражение)
        runCatching {
            val m = builder.javaClass.getMethod("setRequestPromotedOngoing", java.lang.Boolean.TYPE)
            m.invoke(builder, true)
        }
    }

    /** Диагностика в logcat: видно, почему система не подняла уведомление. */
    fun logState(ctx: Context, n: Notification) {
        if (Build.VERSION.SDK_INT < 36) return
        runCatching {
            val nm = ctx.getSystemService(NotificationManager::class.java)
            android.util.Log.d("LiveNotes", "promotable=${n.hasPromotableCharacteristics()} " +
                "allowed=${nm?.canPostPromotedNotifications()}")
        }
    }
}
