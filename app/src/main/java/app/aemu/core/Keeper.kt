package app.aemu.core

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import app.aemu.R
import app.aemu.ui.VmActivity
import app.aemu.ui.VmHost

/**
 * Служба переднего плана процесса :vm. Пока она жива, Android не выгружает процесс
 * с прошивкой при сворачивании и не режет ему процессор.
 */
class Keeper : Service() {
    override fun onBind(i: Intent?): IBinder? = null

    override fun onStartCommand(i: Intent?, flags: Int, id: Int): Int {
        val title = i?.getStringExtra("title") ?: "Android"
        runCatching { startForeground(ID, note(this, title)) }
        if (i?.action == STOP) {
            runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
            stopSelf()
        }
        if (i?.action == STOP_VM) {
            // «остановить» из уведомления: просим окно системы выключиться, а если его нет — гасим сами
            runCatching { sendBroadcast(Intent("app.aemu.STOP").setPackage(packageName)) }
            Thread {
                Thread.sleep(4000)
                runCatching { VmHost.vm?.stop() }
                VmHost.vm = null
                android.os.Process.killProcess(android.os.Process.myPid())
            }.start()
        }
        return START_NOT_STICKY
    }

    companion object {
        private const val CH = "vm2"
        private const val CH_OLD = "vm"
        private const val ID = 1
        const val STOP = "app.aemu.KEEPER_STOP"
        const val STOP_VM = "app.aemu.KEEPER_STOP_VM"

        /** когда система в этом процессе запустилась: от этого времени идёт счётчик в уведомлении */
        @Volatile private var startedAt = 0L

        fun note(ctx: Context, title: String): Notification {
            if (startedAt == 0L) startedAt = System.currentTimeMillis()
            val ch = LiveNotes.channel(ctx, CH, CH_OLD, ctx.getString(R.string.channel_vm))
            val open = PendingIntent.getActivity(ctx, 0, Intent(ctx, VmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val stop = PendingIntent.getService(ctx, 2, Intent(ctx, Keeper::class.java).setAction(STOP_VM),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            val b = Notification.Builder(ctx, ch)
                .setContentTitle(ctx.getString(R.string.vm_running, title))
                .setContentText(ctx.getString(R.string.vm_running_hint))
                .setSmallIcon(R.drawable.ic_stat_vm)
                .setContentIntent(open)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                // вместо полоски прогресса — счётчик времени работы системы (в «облачке» он и нужен)
                .setWhen(startedAt)
                .setUsesChronometer(true)
                .addAction(Notification.Action.Builder(null, ctx.getString(R.string.shutdown), stop).build())
            // «живое обновление»: система видна «облачком» в строке состояния
            if (LiveNotes.on(ctx)) LiveNotes.promote(b, null)
            return b.build().also { if (LiveNotes.on(ctx)) LiveNotes.logState(ctx, it) }
        }

        fun hold(ctx: Context, title: String) {
            runCatching { ctx.startForegroundService(Intent(ctx, Keeper::class.java).putExtra("title", title)) }
        }

        fun release(ctx: Context) {
            runCatching { ctx.stopService(Intent(ctx, Keeper::class.java)) }
        }

    }
}
