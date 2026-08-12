package com.lnu.volumelockr.plus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.lnu.volumelockr.plus.service.VolumeService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
            val isTv = uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION

            val defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context)
            val autostartEnabled = defaultPrefs.getBoolean("autostart_on_boot", true)

            val prefs = context.getSharedPreferences(VolumeService.APP_SHARED_PREFERENCES, Context.MODE_PRIVATE)
            val locks = prefs.getString(VolumeService.LOCKS_KEY, "")
            val hasLocks = !locks.isNullOrEmpty() && locks != "{}"

            if (isTv || (autostartEnabled && hasLocks)) {
                VolumeService.start(context)
                Toast.makeText(context, context.getString(R.string.toast_autostarted), Toast.LENGTH_LONG).show()
            }
        }
    }
}
