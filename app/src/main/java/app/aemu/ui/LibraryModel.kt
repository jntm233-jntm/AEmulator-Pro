package app.aemu.ui

import android.app.ActivityManager
import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.aemu.AppPrefs
import app.aemu.core.GuestImage
import app.aemu.core.ImageStore
import app.aemu.core.VmSettings
import app.aemu.importer.ImportBus
import app.aemu.importer.ImportState
import app.aemu.importer.ImporterService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LibraryModel(app: Application) : AndroidViewModel(app) {
    private val _images = MutableStateFlow<List<GuestImage>>(emptyList())
    val images: StateFlow<List<GuestImage>> = _images
    /** Импорт ведёт foreground-служба из этого же процесса: экран лишь читает её состояние. */
    val import: StateFlow<ImportState> = ImportBus.state
    @Volatile private var cloneCancelled = false

    init {
        refresh()
        // когда импорт закончился (готово, ошибка, отмена) — обновить список машин
        viewModelScope.launch {
            ImportBus.state.map { it.active }.distinctUntilChanged().collect { active -> if (!active) refresh() }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _images.value = withContext(Dispatchers.IO) { ImageStore.list(getApplication()) }
        }
    }

    fun import(uri: Uri) {
        if (ImportBus.state.value.active) return
        val ctx = getApplication<Application>()
        val name = runCatching {
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "firmware"
        ImporterService.startImport(ctx, uri, name)
    }

    /**
     * Открывает машину из библиотеки. Процесс :vm после остановки умирает не мгновенно, и запуск, попавший в
     * умирающий процесс, пропадает — поэтому, если процесс жив, а машины в нём уже нет, сначала ждём его ухода.
     */
    fun startVm(ctx: android.content.Context, id: String, recovery: Boolean = false, immersive: Boolean = false) {
        if (!vmRunning() || AppPrefs.runningIdNow(ctx).isNotEmpty()) {
            VmActivity.start(ctx, id, recovery, immersive)
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            var waited = 0
            // остановка занимает до ~5 с (гость синхронизирует диски), ждём чуть дольше
            while (vmRunning() && waited < 80) {
                delay(100)
                waited++
            }
            withContext(Dispatchers.Main) { VmActivity.start(ctx, id, recovery, immersive) }
        }
    }

    fun clone(src: GuestImage, name: String, copyData: Boolean, customSettings: app.aemu.core.VmSettings? = null) {
        if (ImportBus.state.value.active) return
        val ctx = getApplication<Application>()
        cloneCancelled = false
        ImportBus.update { ImportState(active = true, file = name, step = "Copying") }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val img = ImageStore.clone(ctx, src, name, copyData, customSettings, { cloneCancelled }) { s ->
                    ImportBus.update { it.copy(step = "Copying $s") }
                }
                ImportBus.update { ImportState(active = false, done = img, step = "Done", progress = 1f) }
            } catch (t: Throwable) {
                ImportBus.update { ImportState(active = false, file = name, error = t.message ?: t.toString()) }
            } finally { refresh() }
        }
    }

    fun cancelImport() {
        cloneCancelled = true
        val ctx = getApplication<Application>()
        if (ImportBus.state.value.active) ImporterService.cancel(ctx) else ImportBus.cancel()
    }

    /** A machine is running (they live in the :vm process); its files must not be changed under it. */
    fun vmRunning(): Boolean {
        val ctx = getApplication<Application>()
        val am = ctx.getSystemService(ActivityManager::class.java)
        return am.runningAppProcesses.orEmpty().any { it.processName == ctx.packageName + ":vm" }
    }

    /**
     * Останавливает машину в :vm: сначала просит её завершиться по-хорошему (у неё есть приёмник app.aemu.STOP),
     * а если она не ответила за несколько секунд — завершает процессы приложения (:vm, :binder, :gl).
     */
    fun stopRunningVm() {
        val ctx = getApplication<Application>()
        ctx.sendBroadcast(Intent("app.aemu.STOP").setPackage(ctx.packageName))
        viewModelScope.launch(Dispatchers.Default) {
            delay(4000)
            if (!vmRunning()) return@launch
            val am = ctx.getSystemService(ActivityManager::class.java)
            am.runningAppProcesses.orEmpty()
                .filter { it.processName.startsWith(ctx.packageName + ":") }
                .forEach { android.os.Process.killProcess(it.pid) }
            AppPrefs.setRunningId(ctx, "")
        }
    }

    fun resetData(img: GuestImage) {
        viewModelScope.launch(Dispatchers.IO) {
            ImageStore.resetData(getApplication(), img.id)
            refresh()
        }
    }

    fun dismissImport() { ImportBus.reset() }

    fun delete(img: GuestImage) {
        viewModelScope.launch(Dispatchers.IO) {
            ImageStore.delete(getApplication(), img.id)
            refresh()
        }
    }

    fun rename(img: GuestImage, name: String) = update(img) { it.copy(name = name) }

    fun updateSettings(img: GuestImage, s: VmSettings) = update(img) { it.copy(settings = s) }

    private fun update(img: GuestImage, f: (GuestImage) -> GuestImage) {
        viewModelScope.launch(Dispatchers.IO) {
            ImageStore.update(getApplication(), img.id, f)
            refresh()
        }
    }
}
