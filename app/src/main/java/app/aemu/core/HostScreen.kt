package app.aemu.core

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager

/**
 * Экран самого телефона для варианта «Хост» в настройках экрана системы: настоящее разрешение дисплея (в портретной
 * ориентации — телефон можно держать боком в момент выбора) и его плотность в dpi.
 */
object HostScreen {
    data class Size(val width: Int, val height: Int, val density: Int)

    @Suppress("DEPRECATION") // до API 30 полный размер дисплея даёт только Display.getRealMetrics
    fun metrics(ctx: Context): Size {
        val dm = ctx.resources.displayMetrics
        var w = dm.widthPixels
        var h = dm.heightPixels
        if (Build.VERSION.SDK_INT >= 30) {
            // maximumWindowMetrics: the whole display, not the window the settings happen to be shown in
            val b = ctx.getSystemService(WindowManager::class.java)?.maximumWindowMetrics?.bounds
            if (b != null && b.width() > 0 && b.height() > 0) { w = b.width(); h = b.height() }
        } else {
            val wm = ctx.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            val real = DisplayMetrics()
            wm?.defaultDisplay?.getRealMetrics(real)
            if (real.widthPixels > 0 && real.heightPixels > 0) { w = real.widthPixels; h = real.heightPixels }
        }
        if (w > h) { val t = w; w = h; h = t }
        return Size(VmSettings.clampWidth(w), VmSettings.clampHeight(h), VmSettings.clampDensity(dm.densityDpi))
    }

    /** Образ с экраном телефона в настройках, отмеченных как «Хост»; остальные образы возвращаются как есть. */
    fun resolve(ctx: Context, img: GuestImage): GuestImage {
        if (!img.settings.hostScreen) return img
        val s = metrics(ctx)
        return img.copy(settings = img.settings.copy(width = s.width, height = s.height, density = s.density))
    }
}
