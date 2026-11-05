package app.aemu.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Public
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import app.aemu.AppPrefs
import app.aemu.BuildConfig
import app.aemu.update.AppUpdateManager
import app.aemu.update.ReleaseInfo
import app.aemu.update.UpdateState
import kotlinx.coroutines.Job
import app.aemu.R

/** 小褋褘谢泻邪 薪邪 芯褉懈谐懈薪邪谢褜薪褘泄 锌褉芯械泻褌. */
object Links {
    const val GITHUB = "https://github.com/uxazu/aemulator"
    const val ORIGINAL = "https://t.me/istratii_tech1"
    const val AUTHOR = "https://github.com/jntm233-jntm"
    const val PROJECT = "https://github.com/jntm233-jntm/AEmulator-Pro"
}

/** Правовые документы, которые открываются из настроек. */
private enum class Legal { NONE, LICENSE, NOTICE }

@Composable
private fun LegalDialog(kind: Legal, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val text = remember(kind) {
        when (kind) {
            // полный текст лицензии лежит в res/raw, чтобы его можно было прочитать и без репозитория
            Legal.LICENSE -> runCatching {
                ctx.resources.openRawResource(R.raw.license_gpl3).bufferedReader().use { it.readText() }
            }.getOrDefault("GNU General Public License v3.0")
            else -> ctx.getString(R.string.legal_notice_text)
        }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (kind == Legal.LICENSE) R.string.as_license else R.string.as_notice)) },
        text = {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                Text(text, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}

class AppSettingsActivity : ComponentActivity() {
    override fun attachBaseContext(base: Context) = super.attachBaseContext(AppPrefs.wrap(base))

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { AemuTheme { AppSettings(onBack = ::finish, onRestyle = ::restart) } }
    }

    /** 携蟹褘泻 锌褉懈屑械薪褟械褌褋褟 锌械褉械褋芯蟹写邪薪懈械屑 邪泻褌懈胁薪芯褋褌懈 写谢褟 锌褉懈屑械薪械薪懈褟 薪芯胁芯谐芯 ContextWrapper 褋 谢芯泻邪谢褜褞. */
    private fun restart() {
        recreate()
    }
}

@Composable
private fun AppSettings(onBack: () -> Unit, onRestyle: () -> Unit) {
    val ctx = LocalContext.current
    val scroll = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var langDialog by remember { mutableStateOf(false) }
    var defaults by remember { mutableStateOf(AppPrefs.defaults(ctx)) }
    var theme by remember { mutableStateOf(AppPrefs.theme(ctx)) }
    var dynamic by remember { mutableStateOf(AppPrefs.dynamicColor(ctx)) }
    var cutout by remember { mutableStateOf(AppPrefs.cutoutAvoid(ctx)) }
    var navOverlay by remember { mutableStateOf(AppPrefs.navOverlay(ctx)) }
    var navAlpha by remember { mutableStateOf(AppPrefs.navAlpha(ctx)) }
    var navMovable by remember { mutableStateOf(AppPrefs.navMovable(ctx)) }
    var hideMenu by remember { mutableStateOf(AppPrefs.hideMenu(ctx)) }
    var menuAlpha by remember { mutableStateOf(AppPrefs.menuAlpha(ctx)) }
    var menuMovable by remember { mutableStateOf(AppPrefs.menuMovable(ctx)) }
    var bootHidden by remember { mutableStateOf(AppPrefs.hideBootCard(ctx)) }
    var liveNotes by remember { mutableStateOf(AppPrefs.liveUpdates(ctx)) }
    var predictive by remember { mutableStateOf(AppPrefs.predictiveBack(ctx)) }
    var legal by remember { mutableStateOf(Legal.NONE) }

    // прогресс жеста «назад»: экран едет за пальцем, а не закрывается рывком
    var backP by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    // 芦薪芯胁褘械 褋懈褋褌械屑褘禄: 褉邪蟹写械谢 写谢懈薪薪褘泄, 锌芯 褍屑芯谢褔邪薪懈褞 褋胁褢褉薪褍褌
    var defaultsOpen by remember { mutableStateOf(false) }
    // 芯斜褖邪褟 锌邪锌泻邪: 锌芯写褋泻邪蟹泻邪 薪褍卸薪邪 褌芯谢褜泻芯 锌芯泻邪 写芯褋褌褍锌 薪械 胁褘写邪薪
    var storageGranted by remember { mutableStateOf(Build.VERSION.SDK_INT < 30 || android.os.Environment.isExternalStorageManager()) }
    val storageLife = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(storageLife) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME)
                storageGranted = Build.VERSION.SDK_INT < 30 || android.os.Environment.isExternalStorageManager()
        }
        storageLife.lifecycle.addObserver(obs)
        onDispose { storageLife.lifecycle.removeObserver(obs) }
    }

    fun open(url: String) = runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    fun copy(text: String) {
        (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("address", text))
        Toast.makeText(ctx, ctx.getString(R.string.as_copied), Toast.LENGTH_SHORT).show()
    }
    val langTag = AppPrefs.language(ctx)
    val langName = AppPrefs.LANGUAGES.firstOrNull { it.first == langTag }?.second?.ifEmpty { null } ?: stringResource(R.string.as_lang_system)

    // предиктивный возврат: экран следует за жестом, отпускание закрывает его.
    // Где система такой жест не присылает, экран всё равно уезжает с той же анимацией, а не закрывается рывком.
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var leaving by remember { mutableStateOf(false) }
    val exit by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (leaving) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "exit",
    )
    // окно полупрозрачное: экран въезжает справа над главным, поэтому у входа своя анимация
    var entered by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(Unit) { entered = true }
    val enter by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(240),
        label = "enter",
    )
    val backProgress = maxOf(backP, exit)

    fun leave() {
        if (leaving) return
        leaving = true
        scope.launch { kotlinx.coroutines.delay(210); onBack() }
    }

    // обновления: авто-проверка (см. MainActivity) и ручная проверка + загрузка/установка
    var autoUpdates by remember { mutableStateOf(AppPrefs.autoCheckUpdates(ctx)) }
    var updateState by remember { mutableStateOf<UpdateState>(UpdateState.Idle) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    fun checkForUpdates() {
        updateState = UpdateState.Checking
        scope.launch {
            val res = AppUpdateManager.checkLatestRelease()
            res.fold(
                onSuccess = { rel ->
                    if (rel != null) {
                        if (AppUpdateManager.isNewerVersion(rel.versionName, BuildConfig.VERSION_NAME)) {
                            updateState = UpdateState.Available(rel)
                        } else {
                            updateState = UpdateState.UpToDate(BuildConfig.VERSION_NAME, rel.tagName)
                        }
                    } else {
                        updateState = UpdateState.UpToDate(BuildConfig.VERSION_NAME, "")
                    }
                },
                onFailure = { err ->
                    updateState = UpdateState.Error(err.localizedMessage ?: "network error")
                }
            )
        }
    }

    fun startDownload(rel: ReleaseInfo) {
        updateState = UpdateState.Downloading(rel, 0f, 0L, rel.sizeBytes)
        downloadJob = scope.launch {
            try {
                val file = AppUpdateManager.downloadApk(ctx, rel) { progress, downloaded, total ->
                    updateState = UpdateState.Downloading(rel, progress, downloaded, total)
                }
                updateState = UpdateState.ReadyToInstall(rel, file)
                AppUpdateManager.installApk(ctx, file)
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    updateState = UpdateState.Error(e.localizedMessage ?: "download error")
                }
            }
        }
    }

    if (predictive) {
        androidx.activity.compose.PredictiveBackHandler { progress ->
            try {
                progress.collect { backP = it.progress }
                leave()
            } catch (c: kotlinx.coroutines.CancellationException) {
                backP = 0f
                throw c
            }
        }
    } else {
        androidx.activity.compose.BackHandler { leave() }
    }

    // затемнение под диалогами рисуем сами: окно настроек полупрозрачное, и системное затемнение
    // окна гасило бы главный экран за ним (он «темнел» бы и светлел при возврате)
    val dialogOpen = legal != Legal.NONE || langDialog || updateState !is UpdateState.Idle
    val dialogScrim by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (dialogOpen) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(150),
        label = "dialogScrim",
    )

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scroll.nestedScrollConnection).graphicsLayer {
            val p = backProgress.coerceIn(0f, 1f)
            val n = enter.coerceIn(0f, 1f)
            // вход: выезд справа с растворением, главный экран при этом не затемняется —
            // иначе он «мигает» тёмным до появления настроек;
            // к концу возврата экран полностью уходит вправо и растворяется, поэтому finish() не виден
            translationX = size.width * (0.26f * (1f - n) + 0.30f * p)
            alpha = n * (1f - p)
            val s = (0.96f + 0.04f * n) * (1f - 0.10f * p)
            scaleX = s
            scaleY = s
        },
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.settings)) },
                navigationIcon = { IconButton(onClick = { leave() }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back)) } },
                scrollBehavior = scroll,
            )
        },
    ) { pad ->
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = pad.calculateTopPadding() + 8.dp, bottom = pad.calculateBottomPadding() + 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { AboutCard() }

            // 芯斜褖邪褟 锌邪锌泻邪 褋懈褋褌械屑: 锌芯写褋泻邪蟹泻邪 褌芯谢褜泻芯 锌芯泻邪 写芯褋褌褍锌邪 薪械褌
            if (!storageGranted) item { StorageAccessSection() }

            item {
                Section(stringResource(R.string.as_appearance)) {
                    Row_(Icons.Rounded.Translate, stringResource(R.string.as_language), langName) { langDialog = true }
                    Text(stringResource(R.string.as_theme), style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp))
                    val options = listOf(R.string.as_theme_system, R.string.as_theme_light, R.string.as_theme_dark)
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)) {
                        options.forEachIndexed { i, res ->
                            ToggleButton(
                                checked = theme == i,
                                onCheckedChange = { if (theme != i) { theme = i; AppPrefs.setTheme(ctx, i) } },
                                shapes = when (i) {
                                    0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                    options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                    else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                },
                                modifier = Modifier.weight(1f).semantics { role = Role.RadioButton },
                                contentPadding = PaddingValues(horizontal = 8.dp),
                            ) {
                                // 芯写薪邪 褋褌褉芯泻邪: 写谢懈薪薪褘械 薪邪蟹胁邪薪懈褟 (芦小懈褋褌械屑薪邪褟禄) 薪械 锌械褉械薪芯褋褟褌褋褟 锌芯 斜褍泻胁邪屑
                                Text(stringResource(res), maxLines = 1, softWrap = false,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    if (Build.VERSION.SDK_INT >= 31) {
                        Toggle(stringResource(R.string.as_dynamic), stringResource(R.string.as_dynamic_sub), dynamic) {
                            dynamic = it; AppPrefs.setDynamicColor(ctx, it)
                        }
                    }
                    Toggle(stringResource(R.string.as_predictive_back), stringResource(R.string.as_predictive_back_sub), predictive) {
                        predictive = it; AppPrefs.setPredictiveBack(ctx, it)
                    }
                }
            }

            item {
                Section(stringResource(R.string.as_screen)) {
                    Toggle(stringResource(R.string.as_cutout), stringResource(R.string.as_cutout_sub), cutout) {
                        cutout = it
                        AppPrefs.setCutoutAvoid(ctx, it)
                    }
                }
            }

            item {
                Section(stringResource(R.string.as_nav_menu)) {
                    Toggle(stringResource(R.string.as_nav_overlay), stringResource(R.string.as_nav_overlay_sub), navOverlay) {
                        navOverlay = it
                        AppPrefs.setNavOverlay(ctx, it)
                    }
                    SliderRow(stringResource(R.string.as_nav_alpha), navAlpha) {
                        navAlpha = it
                        AppPrefs.setNavAlpha(ctx, it)
                    }
                    Toggle(stringResource(R.string.as_nav_movable), stringResource(R.string.as_nav_movable_sub), navMovable) {
                        navMovable = it
                        AppPrefs.setNavMovable(ctx, it)
                    }
                    Toggle(stringResource(R.string.as_hide_menu), stringResource(R.string.as_hide_menu_sub), hideMenu) {
                        hideMenu = it
                        AppPrefs.setHideMenu(ctx, it)
                    }
                    Toggle(stringResource(R.string.as_menu_movable), stringResource(R.string.as_menu_movable_sub), menuMovable) {
                        menuMovable = it
                        AppPrefs.setMenuMovable(ctx, it)
                    }
                    SliderRow(stringResource(R.string.as_menu_alpha), menuAlpha) {
                        menuAlpha = it
                        AppPrefs.setMenuAlpha(ctx, it)
                    }
                    Row_(Icons.Rounded.RestartAlt, stringResource(R.string.m_reset_menu_pos),
                        stringResource(R.string.as_reset_menu_pos_sub)) {
                        AppPrefs.resetMenuButton(ctx)
                        // 械褋谢懈 褋懈褋褌械屑邪 褋械泄褔邪褋 芯褌泻褉褘褌邪, 械褢 泻薪芯锌泻邪 胁械褉薪褢褌褋褟 胁 褍谐芯谢 褋褉邪蟹褍
                        ctx.sendBroadcast(Intent("app.aemu.RESET_MENU").setPackage(ctx.packageName))
                        Toast.makeText(ctx, R.string.as_reset_menu_pos_done, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            item {
                Section(stringResource(R.string.as_boot)) {
                    Toggle(stringResource(R.string.as_boot_hidden), stringResource(R.string.as_boot_hidden_sub), bootHidden) {
                        bootHidden = it
                        AppPrefs.setHideBootCard(ctx, it)
                    }
                }
            }

            item {
                Section(stringResource(R.string.as_notifications)) {
                    Toggle(stringResource(R.string.as_live_updates), stringResource(R.string.as_live_updates_sub), liveNotes) {
                        liveNotes = it
                        AppPrefs.setLiveUpdates(ctx, it)
                        // 械褋谢懈 褋懈褋褌械屑邪 褍卸械 蟹邪锌褍褖械薪邪, 械褢 褍胁械写芯屑谢械薪懈械 锌械褉械褉懈褋芯胁褘胁邪械褌褋褟 胁 薪芯胁芯屑 胁懈写械 褋褉邪蟹褍
                        ctx.sendBroadcast(Intent("app.aemu.NOTIF_STYLE").setPackage(ctx.packageName))
                    }
                }
            }

            item {
                Section(stringResource(R.string.as_defaults), stringResource(R.string.as_defaults_sub),
                    expanded = defaultsOpen, onToggle = { defaultsOpen = !defaultsOpen }) {
                    fun set(s: app.aemu.core.VmSettings) { defaults = s; AppPrefs.setDefaults(ctx, s) }
                    Toggle(stringResource(R.string.vs_gpu), stringResource(R.string.vs_gpu_sub), defaults.gpu) { set(defaults.copy(gpu = it)) }
                    Toggle(stringResource(R.string.vs_jit), stringResource(R.string.vs_jit_sub), defaults.jit) { set(defaults.copy(jit = it)) }
                    Toggle(stringResource(R.string.vs_proxy), stringResource(R.string.vs_proxy_sub), defaults.netProxy) { set(defaults.copy(netProxy = it)) }
                    Toggle(stringResource(R.string.vs_nav), stringResource(R.string.vs_nav_sub), defaults.showNavBar) { set(defaults.copy(showNavBar = it)) }
                    Toggle(stringResource(R.string.vs_awake), stringResource(R.string.vs_awake_sub), defaults.keepScreenOn) { set(defaults.copy(keepScreenOn = it)) }
                    Toggle(stringResource(R.string.as_led), stringResource(R.string.as_led_sub), defaults.showLed) { set(defaults.copy(showLed = it)) }
                }
            }

            item {
                Section(stringResource(R.string.as_updates)) {
                    Toggle(
                        stringResource(R.string.as_auto_updates),
                        stringResource(R.string.as_auto_updates_sub),
                        autoUpdates
                    ) {
                        autoUpdates = it
                        AppPrefs.setAutoCheckUpdates(ctx, it)
                    }
                    Row_(
                        Icons.Rounded.SystemUpdate,
                        stringResource(R.string.as_check_updates),
                        stringResource(R.string.as_check_updates_sub, BuildConfig.VERSION_NAME)
                    ) {
                        checkForUpdates()
                    }
                }
            }

            item {
                Section(stringResource(R.string.as_links)) {

                    Row_(Icons.Rounded.Code, stringResource(R.string.as_original_app), "github.com/uxazu/aemulator") { open(Links.GITHUB) }
                    Row_(Icons.Rounded.History, stringResource(R.string.as_orig_author), "t.me/istratii_tech1") { open(Links.ORIGINAL) }
                    Row_(Icons.Rounded.Person, stringResource(R.string.as_author), "github.com/jntm233-jntm") { open(Links.AUTHOR) }
                    Row_(Icons.Rounded.Public, stringResource(R.string.as_project), "github.com/jntm233-jntm/AEmulator-Pro") { open(Links.PROJECT) }
                    Row_(Icons.Rounded.Description, stringResource(R.string.as_license), "GNU GPL v3") { legal = Legal.LICENSE }
                    Row_(Icons.Rounded.Article, stringResource(R.string.as_notice), stringResource(R.string.as_notice_sub)) { legal = Legal.NOTICE }
                }
            }
        }
    }

    // затемнение поверх содержимого настроек, под окном диалога; тапы «мимо» не проваливаются в список
    if (dialogScrim > 0f) {
        Box(
            Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.45f * dialogScrim))
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null
                ) {}
        )
    }
    }

    if (legal != Legal.NONE) LegalDialog(legal) { legal = Legal.NONE }

    UpdateDialog(
        state = updateState,
        onDismiss = { updateState = UpdateState.Idle },
        onStartDownload = ::startDownload,
        onCancelDownload = {
            downloadJob?.cancel()
            updateState = UpdateState.Idle
        },
        onInstall = { file -> AppUpdateManager.installApk(ctx, file) },
        onOpenUrl = { url -> open(url) }
    )


    if (langDialog) {
        AlertDialog(
            onDismissRequest = { langDialog = false },
            icon = { Icon(Icons.Rounded.Language, null) },
            title = { Text(stringResource(R.string.as_language)) },
            text = {
                LazyColumn {
                    items(AppPrefs.LANGUAGES) { (tag, name) ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable {
                                langDialog = false
                                if (tag != langTag) { AppPrefs.setLanguage(ctx, tag); onRestyle() }
                            }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = tag == langTag, onClick = null, modifier = Modifier.padding(horizontal = 12.dp))
                            Text(name.ifEmpty { stringResource(R.string.as_lang_system) }, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { langDialog = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }

}

/** 袛芯褋褌褍锌 泻芯 胁褋械屑 褎邪泄谢邪屑: 芯斜褖邪褟 锌邪锌泻邪 褋懈褋褌械屑 胁 芦袙薪褍褌褉械薪薪懈泄 薪邪泻芯锌懈褌械谢褜/AEmulator禄. */
@Composable
private fun StorageAccessSection() {
    val ctx = LocalContext.current
    // 褉邪蟹写械谢 锌芯泻邪蟹褘胁邪械褌褋褟 褌芯谢褜泻芯 锌芯泻邪 写芯褋褌褍锌邪 薪械褌 (褝褌芯 褉械褕邪械褌 胁褘蟹褘胁邪褞褖懈泄 褝泻褉邪薪)
    Section(stringResource(R.string.storage_title)) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.storage_text), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                runCatching {
                    ctx.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                        Uri.parse("package:${ctx.packageName}")))
                }.onFailure {
                    ctx.startActivity(Intent(android.provider.Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                }
            }) { Text(stringResource(R.string.allow)) }
        }
    }
}

@Composable
private fun AboutCard() {
    Card(shape = MaterialTheme.shapes.extraLarge, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.logo), null, Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)))
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(stringResource(R.string.as_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.as_about), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
    }
}

@Composable
private fun Section(
    title: String,
    subtitle: String? = null,
    expanded: Boolean? = null,
    onToggle: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column {
        // 蟹邪谐芯谢芯胁芯泻 泻邪褌械谐芯褉懈懈; 褋芯 褋褌褉械谢泻芯泄 懈 薪邪卸邪褌懈械屑 鈥?械褖褢 懈 褋胁芯褉邪褔懈胁邪械褌 褋芯写械褉卸懈屑芯械
        Row(
            Modifier.fillMaxWidth()
                .then(if (onToggle != null) Modifier.clickable { onToggle() } else Modifier)
                .padding(start = 8.dp, end = 8.dp, bottom = if (subtitle == null) 8.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f))
            if (expanded != null) Icon(
                if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        subtitle?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp))
        }
        if (expanded == null || expanded) {
            Card(shape = MaterialTheme.shapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(vertical = 8.dp)) { content() }
            }
        }
    }
}

@Composable
private fun Row_(icon: ImageVector, title: String, sub: String, mono: Boolean = false, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(sub, fontFamily = if (mono) FontFamily.Monospace else null, maxLines = 2) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    )
}

/** A row with a 0鈥?00 % slider: the value is shown above it and written to the settings on every step. */
@Composable
private fun SliderRow(title: String, value: Int, onChange: (Int) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = {
            Column {
                Text("$value%")
                Slider(value = value.toFloat(), onValueChange = { onChange(it.roundToInt()) },
                    valueRange = 0f..100f, modifier = Modifier.fillMaxWidth())
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
