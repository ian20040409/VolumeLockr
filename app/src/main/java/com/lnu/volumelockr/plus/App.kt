package com.lnu.volumelockr.plus

import android.app.Activity
import android.app.Application
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.color.DynamicColors
import com.lnu.volumelockr.plus.util.SecurityUtils

class App : Application() {

    private var startedActivityCount = 0
    private var isChangingConfig = false

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)

        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        if (uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }

        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}

            override fun onActivityStarted(activity: Activity) {
                if (startedActivityCount == 0 && !isChangingConfig) {
                    SecurityUtils.isAppUnlocked = false
                }
                startedActivityCount++
            }

            override fun onActivityResumed(activity: Activity) {}

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivityStopped(activity: Activity) {
                isChangingConfig = activity.isChangingConfigurations
                startedActivityCount = maxOf(0, startedActivityCount - 1)
                if (startedActivityCount == 0 && !isChangingConfig) {
                    SecurityUtils.isAppUnlocked = false
                }
            }

            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

            override fun onActivityDestroyed(activity: Activity) {}
        })
    }
}
