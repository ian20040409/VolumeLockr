package com.lnu.volumelockr.plus.ui

import android.app.Application
import com.google.android.material.color.DynamicColors

class VolumeLockrApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}
