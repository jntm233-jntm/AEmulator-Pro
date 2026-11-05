package app.aemu

import android.app.Activity
import android.app.Application
import android.os.Bundle

class AemuApp : Application() {
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                AppVisibility.onStart()
                // импорт закончился, пока приложение было свёрнуто: вернулись — уведомление о результате не нужно
                if (activity is app.aemu.ui.MainActivity) app.aemu.importer.ImporterService.clearResult(this@AemuApp)
            }

            override fun onActivityStopped(activity: Activity) = AppVisibility.onStop()
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
