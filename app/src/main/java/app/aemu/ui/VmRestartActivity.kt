package app.aemu.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.aemu.AppPrefs
import app.aemu.R
import app.aemu.core.ImageStore
import app.aemu.core.ProcessDeathWatch
import app.aemu.core.awaitVmProcessExit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Перезапуск по схеме «из рук в руки».
 *
 * Живёт в основном процессе, а не в :vm, поэтому переживает смерть процесса с машиной: ни будильников,
 * ни фоновых запусков не нужно. [VmActivity] отдаёт сюда id машины и Binder, живущий столько же, сколько
 * процесс :vm; здесь ждём его смерти и сразу открываем новое окно системы.
 */
class VmRestartActivity : ComponentActivity() {
    private var failed by mutableStateOf(false)
    private var launched = false

    override fun attachBaseContext(base: Context) = super.attachBaseContext(AppPrefs.wrap(base))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra(VmActivity.EXTRA_ID)
        val oldProcess = intent.extras?.getBinder(EXTRA_PROCESS)
        if (id == null || oldProcess == null || ImageStore.get(this, id) == null) { finish(); return }
        val recovery = intent.getBooleanExtra(VmActivity.EXTRA_RECOVERY, false)
        val immersive = intent.getBooleanExtra(VmActivity.EXTRA_IMMERSIVE, false)
        setContent {
            AemuTheme {
                BackHandler(enabled = !failed) { /* сначала доводим передачу процесса до конца */ }
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally) {
                        if (!failed) CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text(stringResource(if (failed) R.string.vm_restart_failed else R.string.vm_restarting))
                        if (failed) TextButton(onClick = { finish() }) { Text(stringResource(R.string.close)) }
                    }
                }
            }
        }
        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                if (!launched && !failed) {
                    try {
                        withTimeout(10_000) { awaitVmProcessExit(BinderDeathWatch(oldProcess)) }
                        VmActivity.start(this@VmRestartActivity, id, recovery, immersive)
                        launched = true
                        finish()
                    } catch (_: TimeoutCancellationException) { failed = true }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) { failed = true }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_PROCESS = "old_vm_process"

        fun handoff(context: Context, id: String, recovery: Boolean, oldProcess: IBinder, immersive: Boolean = false) {
            context.startActivity(Intent(context, VmRestartActivity::class.java)
                .putExtra(VmActivity.EXTRA_ID, id).putExtra(VmActivity.EXTRA_RECOVERY, recovery)
                .putExtra(VmActivity.EXTRA_IMMERSIVE, immersive)
                .putExtras(Bundle().apply { putBinder(EXTRA_PROCESS, oldProcess) })
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

private class BinderDeathWatch(private val binder: IBinder) : ProcessDeathWatch {
    private var recipient: IBinder.DeathRecipient? = null
    override val alive get() = binder.isBinderAlive
    override fun register(onDeath: () -> Unit) {
        val listener = IBinder.DeathRecipient { onDeath() }
        recipient = listener
        try { binder.linkToDeath(listener, 0) } catch (_: RemoteException) { onDeath() }
    }
    override fun unregister() {
        recipient?.let { runCatching { binder.unlinkToDeath(it, 0) } }
        recipient = null
    }
}
