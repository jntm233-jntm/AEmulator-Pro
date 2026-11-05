package app.aemu

import android.content.Context
import android.content.res.Configuration
import app.aemu.core.VmSettings
import java.io.File
import java.util.Locale

/** Настройки самого приложения (не отдельной системы): язык, тема, умолчания для новых систем. */
object AppPrefs {
    private const val FILE = "app"

    /** Языки интерфейса: код BCP-47 → самоназвание. Пустой код — как в системе. */
    val LANGUAGES = listOf(
        "" to "",
        "en" to "English",
        "ru" to "Русский",
        "uk" to "Українська",
        "de" to "Deutsch",
        "fr" to "Français",
        "es" to "Español",
        "pt-BR" to "Português (Brasil)",
        "it" to "Italiano",
        "pl" to "Polski",
        "tr" to "Türkçe",
        "ar" to "العربية",
        "fa" to "فارسی",
        "hi" to "हिन्दी",
        "id" to "Bahasa Indonesia",
        "vi" to "Tiếng Việt",
        "zh-CN" to "简体中文",
        "ja" to "日本語",
        "ko" to "한국어",
    )

    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2

    private fun sp(ctx: Context) = ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun prefs(ctx: Context) = sp(ctx)

    fun language(ctx: Context): String = sp(ctx).getString("lang", "") ?: ""
    fun setLanguage(ctx: Context, tag: String) = sp(ctx).edit().putString("lang", tag).apply()

    fun theme(ctx: Context): Int = sp(ctx).getInt("theme", THEME_SYSTEM)
    fun setTheme(ctx: Context, v: Int) = sp(ctx).edit().putInt("theme", v).apply()

    fun dynamicColor(ctx: Context): Boolean = sp(ctx).getBoolean("dynamic", false)
    fun setDynamicColor(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("dynamic", v).apply()

    /**
     * Экран системы не заходит под вырез или отверстие камеры. Читается при открытии системы: выключенный,
     * он заставляет экран занимать всю площадь дисплея, в том числе под вырезом.
     */
    fun cutoutAvoid(ctx: Context): Boolean = sp(ctx).getBoolean("cutout_avoid", true)
    fun setCutoutAvoid(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("cutout_avoid", v).apply()

    // ------------------------------------------------------------------ the bar under the screen and the "…" button

    /** the bar floats over the system screen instead of taking a strip under it */
    fun navOverlay(ctx: Context): Boolean = sp(ctx).getBoolean("nav_overlay", false)
    fun setNavOverlay(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("nav_overlay", v).apply()

    /** opacity of the bar, 0–100 % (0: invisible, still takes presses) */
    fun navAlpha(ctx: Context): Int = sp(ctx).getInt("nav_alpha", 100)
    fun setNavAlpha(ctx: Context, v: Int) = sp(ctx).edit().putInt("nav_alpha", v.coerceIn(0, 100)).apply()

    /** the bar can be dragged around; its place is not kept after the system is closed */
    fun navMovable(ctx: Context): Boolean = sp(ctx).getBoolean("nav_movable", false)
    fun setNavMovable(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("nav_movable", v).apply()

    /** no "…" button: the phone's back key or gesture opens the emulator menu instead */
    fun hideMenu(ctx: Context): Boolean = sp(ctx).getBoolean("hide_menu", false)
    fun setHideMenu(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("hide_menu", v).apply()

    /** opacity of the "…" button, 0–100 % (0: invisible, still takes presses) */
    fun menuAlpha(ctx: Context): Int = sp(ctx).getInt("menu_alpha", 100)
    fun setMenuAlpha(ctx: Context, v: Int) = sp(ctx).edit().putInt("menu_alpha", v.coerceIn(0, 100)).apply()

    /** the "…" button can be dragged around the system screen (the place is kept for the next launch) */
    fun menuMovable(ctx: Context): Boolean = sp(ctx).getBoolean("menu_movable", true)
    fun setMenuMovable(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("menu_movable", v).apply()

    /** the shared-folder hint on the home screen was closed with its "×" */
    fun storageHintOff(ctx: Context): Boolean = sp(ctx).getBoolean("storage_hint_off", false)
    fun setStorageHintOff(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("storage_hint_off", v).apply()

    /**
     * «Живые обновления» (Android 16, live updates): уведомления о работе системы и импорте показываются
     * «островком» в строке состояния. Выключено — обычные уведомления.
     */
    fun liveUpdates(ctx: Context): Boolean = sp(ctx).getBoolean("live_updates", true)
    fun setLiveUpdates(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("live_updates", v).apply()

    /**
     * То же значение из файла настроек: его переключают в основном процессе, а уведомление о работающей системе
     * строит процесс :vm, у которого свой кэш SharedPreferences.
     */
    fun liveUpdatesNow(ctx: Context): Boolean {
        val xml = runCatching { File(ctx.applicationInfo.dataDir, "shared_prefs/$FILE.xml").readText() }.getOrNull()
            ?: return liveUpdates(ctx)
        return Regex("<boolean[^>]*name=\"live_updates\"[^>]*value=\"(true|false)\"").find(xml)
            ?.groupValues?.get(1)?.toBooleanStrictOrNull() ?: true
    }

    /** проверять обновления при запуске приложения */
    fun autoCheckUpdates(ctx: Context): Boolean = sp(ctx).getBoolean("auto_updates", true)
    fun setAutoCheckUpdates(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("auto_updates", v).apply()

    /** когда последний раз проверяли обновления (мс): в сеть ходим не чаще раза в час */
    fun lastUpdateCheck(ctx: Context): Long = sp(ctx).getLong("last_update_check", 0L)
    fun setLastUpdateCheck(ctx: Context, v: Long) = sp(ctx).edit().putLong("last_update_check", v).apply()
    /** the "system is starting" window starts folded into the small chip in the top-left corner */
    fun hideBootCard(ctx: Context): Boolean = sp(ctx).getBoolean("hide_boot_card", false)
    fun setHideBootCard(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("hide_boot_card", v).apply()

    /**
     * «Предиктивный» возврат (Android 13+): экран следует за жестом «назад», а не закрывается рывком.
     * Работает только там, где система присылает события жеста; иначе возврат обычный.
     */
    fun predictiveBack(ctx: Context): Boolean = sp(ctx).getBoolean("predictive_back", true)
    fun setPredictiveBack(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("predictive_back", v).apply()

    /** «погружение»: the warning that explains it has been shown and "do not show again" was ticked */
    fun immersiveHintOff(ctx: Context): Boolean = sp(ctx).getBoolean("immersive_hint_off", false)
    fun setImmersiveHintOff(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("immersive_hint_off", v).apply()

    /** id of the machine that lives in the :vm process right now (the machine writes it, the library shows "stop") */
    fun runningId(ctx: Context): String = sp(ctx).getString("running_id", "") ?: ""
    fun setRunningId(ctx: Context, id: String) = sp(ctx).edit().putString("running_id", id).apply()

    /**
     * То же самое, но из файла настроек: значение пишет процесс :vm, и кэш SharedPreferences основного процесса
     * иначе показывал бы устаревшее «машина запущена» уже после её остановки.
     */
    fun runningIdNow(ctx: Context): String {
        val xml = runCatching { File(ctx.applicationInfo.dataDir, "shared_prefs/$FILE.xml").readText() }.getOrNull()
            ?: return runningId(ctx)
        return Regex("<string[^>]*name=\"running_id\"[^>]*>(.*?)</string>").find(xml)?.groupValues?.get(1) ?: ""
    }

    /**
     * «…» на экране системы: машина запоминает, куда кнопку утащили (её можно утащить и за пределы экрана),
     * а этой настройкой её место сбрасывается в угол. Место лежит в файле (см. [MenuButtonPos]), потому что
     * SharedPreferences у каждого процесса свой: сброс из основного процесса :vm иначе не увидел бы.
     */
    fun resetMenuButton(ctx: Context) {
        app.aemu.core.MenuButtonPos.reset(ctx)
        // прежние версии хранили место в SharedPreferences — чистим и их
        ctx.applicationContext.getSharedPreferences("vm_ui", Context.MODE_PRIVATE)
            .edit().remove("menu_x").remove("menu_y").apply()
    }

    /** what a running system takes from the settings above */
    data class VmUi(
        val navOverlay: Boolean = false,
        val navAlpha: Int = 100,
        val navMovable: Boolean = false,
        val hideMenu: Boolean = false,
        val menuAlpha: Int = 100,
        val menuMovable: Boolean = true,
    )

    /**
     * The same values, taken from the prefs file itself: a running system lives in its own process, and its
     * cached SharedPreferences would keep the values it read at startup while the settings screen changes the file.
     */
    fun vmUiNow(ctx: Context): VmUi {
        val cached = VmUi(navOverlay(ctx), navAlpha(ctx), navMovable(ctx), hideMenu(ctx), menuAlpha(ctx), menuMovable(ctx))
        val xml = runCatching { File(ctx.applicationInfo.dataDir, "shared_prefs/$FILE.xml").readText() }.getOrNull()
            ?: return cached
        fun b(key: String, d: Boolean): Boolean =
            Regex("<boolean[^>]*name=\"$key\"[^>]*value=\"(true|false)\"").find(xml)
                ?.groupValues?.get(1)?.toBooleanStrictOrNull() ?: d
        fun i(key: String, d: Int): Int =
            Regex("<int[^>]*name=\"$key\"[^>]*value=\"(-?\\d+)\"").find(xml)
                ?.groupValues?.get(1)?.toIntOrNull()?.coerceIn(0, 100) ?: d
        return VmUi(b("nav_overlay", cached.navOverlay), i("nav_alpha", cached.navAlpha),
            b("nav_movable", cached.navMovable), b("hide_menu", cached.hideMenu), i("menu_alpha", cached.menuAlpha),
            b("menu_movable", cached.menuMovable))
    }

    /** Умолчания, которые получает только что импортированная система. */
    fun defaults(ctx: Context): VmSettings {
        val p = sp(ctx)
        val d = VmSettings()
        return d.copy(
            gpu = p.getBoolean("def_gpu", d.gpu),
            jit = p.getBoolean("def_jit", d.jit),
            netProxy = p.getBoolean("def_proxy", d.netProxy),
            showNavBar = p.getBoolean("def_nav", d.showNavBar),
            keepScreenOn = p.getBoolean("def_awake", d.keepScreenOn),
            showLed = p.getBoolean("def_led", d.showLed),
        )
    }

    fun setDefaults(ctx: Context, s: VmSettings) = sp(ctx).edit()
        .putBoolean("def_gpu", s.gpu).putBoolean("def_jit", s.jit).putBoolean("def_proxy", s.netProxy)
        .putBoolean("def_nav", s.showNavBar).putBoolean("def_awake", s.keepScreenOn).putBoolean("def_led", s.showLed).apply()

    /** Контекст с выбранным языком — для attachBaseContext каждой активности. */
    /**
     * Language and theme of the app applied to an activity's resources. The theme goes into the night bits of
     * uiMode: values-night (window background), isSystemInDarkTheme and enableEdgeToEdge's status bar icons then
     * follow the app's choice — before, a dark app theme on a light system kept a white window background and dark,
     * invisible status bar icons.
     */
    fun wrap(base: Context): Context {
        val tag = language(base)
        val night = when (theme(base)) {
            THEME_LIGHT -> Configuration.UI_MODE_NIGHT_NO
            THEME_DARK -> Configuration.UI_MODE_NIGHT_YES
            else -> -1
        }
        if (tag.isEmpty() && night < 0) return base
        val cfg = Configuration(base.resources.configuration)
        if (tag.isNotEmpty()) {
            val loc = Locale.forLanguageTag(tag)
            Locale.setDefault(loc)
            cfg.setLocale(loc)
            cfg.setLayoutDirection(loc)
        }
        if (night >= 0) cfg.uiMode = (cfg.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or night
        return base.createConfigurationContext(cfg)
    }
}
