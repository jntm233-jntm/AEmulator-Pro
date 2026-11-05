package app.aemu.importer

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import app.aemu.AppPrefs
import app.aemu.AppVisibility
import app.aemu.core.LiveNotes
import app.aemu.R
import app.aemu.core.ImageStore
import app.aemu.ui.MainActivity

/**
 * Импорт прошивки в фоне: живёт в основном процессе как foreground-служба, поэтому разбор большого архива идёт и
 * когда приложение свёрнуто, а в шторке видно текущий шаг и прогресс.
 */
class ImporterService : Service() {
    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, flags: Int, startId: Int): Int {
        startForeground(ID, note(this, ImportBus.state.value))
        if (i?.action == ACTION_CANCEL) {
            ImportBus.cancel()
            // импорта нет — закрываем и уведомление, и саму службу
            if (!ImportBus.state.value.active) return finish()
            return START_NOT_STICKY
        }
        val uri = i?.getStringExtra(EXTRA_URI)?.let { Uri.parse(it) } ?: return finish()
        if (ImportBus.state.value.active) return START_NOT_STICKY
        val name = i.getStringExtra(EXTRA_NAME) ?: "firmware"
        ImportBus.update { ImportState(active = true, file = name, step = getString(R.string.import_start)) }
        push()
        Thread({ run(uri, name) }, "firmware-import").start()
        return START_NOT_STICKY
    }

    private fun run(uri: Uri, name: String) {
        val app = applicationContext
        val log = ArrayList<String>()
        var lastStep = ""
        var lastPct = -10
        val imp = Importer(app,
            onProgress = { step, p ->
                ImportBus.update { st -> st.copy(step = step, progress = if (p >= 0) p else st.progress) }
                val pct = (ImportBus.state.value.progress * 100).toInt()
                if (step != lastStep || Math.abs(pct - lastPct) >= 2) {
                    lastStep = step; lastPct = pct; push()
                }
            },
            log = { s -> log.add(s); ImportBus.update { st -> st.copy(log = log.takeLast(50)) } })
        ImportBus.importer = imp
        try {
            val raw = imp.import(uri, name)
            // умолчания из настроек приложения поверх того, что подобрал анализатор (экран, частоты)
            val d = AppPrefs.defaults(app)
            val img = raw.copy(settings = raw.settings.copy(gpu = d.gpu, jit = d.jit, netProxy = d.netProxy,
                showNavBar = d.showNavBar, keepScreenOn = d.keepScreenOn, showLed = d.showLed))
            if (img != raw) ImageStore.save(app, img)
            ImportBus.update { ImportState(active = false, done = img, step = "Done", progress = 1f) }
            result(getString(R.string.import_done), listOf(img.name, img.release).filter { it.isNotBlank() }.joinToString(" · "), "✓")
        } catch (t: Throwable) {
            // отмена — не ошибка: экран просто возвращается к обычному виду
            if (imp.cancelled) ImportBus.update { ImportState() }
            else {
                val msg = t.message ?: t.toString()
                ImportBus.update { ImportState(active = false, file = name, error = msg) }
                result(getString(R.string.import_failed), "$name · $msg", "!")
            }
        } finally {
            ImportBus.importer = null
            finish()
        }
    }

    /**
     * Обновление уведомления — только через startForeground: на部分 ROM (ColorOS, One UI) обычный notify
     * для уведомления переднего плана в фоне игнорируется, и прогресс замирает.
     */
    private fun push() = runCatching {
        startForeground(ID, note(this, ImportBus.state.value))
    }

    /** Итог импорта отдельным уведомлением — но только если приложение сейчас не на экране. */
    private fun result(title: String, text: String, mark: String) {
        if (AppVisibility.visible) return
        runCatching {
            val ch = LiveNotes.channel(this, CH, CH_OLD, getString(R.string.channel_import))
            val b = Notification.Builder(this, ch)
                .setContentTitle(title)
                .setContentText(text.take(140))
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setSmallIcon(R.drawable.ic_stat_import)
                .setAutoCancel(true)
                // «живое обновление» бывает только постоянным, поэтому итог сам уходит по тайм-ауту
                .setOngoing(true)
                .setTimeoutAfter(10_000)
                .setContentIntent(PendingIntent.getActivity(this, 0,
                    Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT))
            // итог импорта тоже показываем «облачком», как и сам импорт
            if (LiveNotes.on(this)) LiveNotes.promote(b, mark)
            getSystemService(NotificationManager::class.java).notify(RESULT_ID, b.build())
        }
    }

    /** Убирает уведомление и закрывает службу. */
    private fun finish(): Int {
        runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
        stopSelf()
        return START_NOT_STICKY
    }

    companion object {
        const val ACTION_CANCEL = "app.aemu.IMPORT_CANCEL"
        const val EXTRA_URI = "uri"
        const val EXTRA_NAME = "name"
        const val RESULT_ID = 3
        private const val CH = "import2"
        private const val CH_OLD = "import"
        private const val ID = 2

        /** Убирает уведомление об итоге импорта (приложение снова на экране). */
        fun clearResult(ctx: Context) {
            runCatching { ctx.getSystemService(NotificationManager::class.java).cancel(RESULT_ID) }
        }

        fun startImport(ctx: Context, uri: Uri, name: String) {
            val i = Intent(ctx, ImporterService::class.java)
                .putExtra(EXTRA_URI, uri.toString())
                .putExtra(EXTRA_NAME, name)
            runCatching { ctx.startForegroundService(i) }
        }

        fun cancel(ctx: Context) {
            runCatching { ctx.startService(Intent(ctx, ImporterService::class.java).setAction(ACTION_CANCEL)) }
        }

        fun note(ctx: Context, st: ImportState): Notification {
            val ch = LiveNotes.channel(ctx, CH, CH_OLD, ctx.getString(R.string.channel_import))
            val open = PendingIntent.getActivity(ctx, 0,
                Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val cancel = PendingIntent.getService(ctx, 0,
                Intent(ctx, ImporterService::class.java).setAction(ACTION_CANCEL),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val text = listOf(st.file, st.step).filter { it.isNotEmpty() }.joinToString(" · ")
            val known = st.progress in 0f..1f
            val pct = (st.progress * 100).toInt().coerceIn(0, 100)
            val b = Notification.Builder(ctx, ch)
                .setContentTitle(ctx.getString(R.string.import_running))
                .setContentText(text)
                .setStyle(Notification.BigTextStyle().bigText(text))
                .setSmallIcon(R.drawable.ic_stat_import)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(open)
                .addAction(Notification.Action.Builder(null, ctx.getString(R.string.cancel), cancel).build())
            if (known) b.setProgress(100, pct, false) else b.setProgress(0, 0, true)
            // «живое обновление» (Android 16): ход импорта виден в строке состояния (слева значок, справа процент).
            // ProgressStyle — одно из условий «повышаемости» уведомления.
            if (LiveNotes.on(ctx)) {
                runCatching {
                    b.setStyle(Notification.ProgressStyle()
                        .setStyledByProgress(known)
                        .setProgressIndeterminate(!known)
                        .addProgressSegment(Notification.ProgressStyle.Segment(100))
                        .setProgress(if (known) pct else 0))
                }
                LiveNotes.promote(b, if (known) "$pct %" else null)
            }
            return b.build().also { if (LiveNotes.on(ctx)) LiveNotes.logState(ctx, it) }
        }
    }
}
