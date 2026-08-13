package com.lnu.volumelockr.plus.ui

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.lnu.volumelockr.plus.R
import com.lnu.volumelockr.plus.service.VolumeService

class SettingsFragment : PreferenceFragmentCompat() {

    companion object {
        const val ALLOW_LOWER_PREFERENCE = "allow_lower"
        const val HIDE_TV_LAUNCHER_ICON_PREFERENCE = "hide_tv_launcher_icon"
        const val PASSWORD_SETTINGS_ENTRY = "password_settings_entry"
        const val PASSWORD_PROTECTED_PREFERENCE = "password_protected"
        const val BIOMETRIC_PROTECTED_PREFERENCE = "biometric_protected"
        const val PASSWORD_CHANGE_PREFERENCE = "password"
        const val PASSWORD_SALT_PREFERENCE = "password_salt"
        const val PASSWORD_HASH_PREFERENCE = "password_hash"
        const val DELAY_IN_MS = 100L
    }

    private lateinit var shouldAllowLower: SwitchPreferenceCompat
    private var hideTvLauncherIcon: SwitchPreferenceCompat? = null

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey)
        
        val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
        val isTv = uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        if (isTv) {
            findPreference<SwitchPreferenceCompat>("autostart_on_boot")?.isVisible = false
        }

        shouldAllowLower = findPreference(ALLOW_LOWER_PREFERENCE)!!
        hideTvLauncherIcon = findPreference(HIDE_TV_LAUNCHER_ICON_PREFERENCE)

        findPreference<Preference>(PASSWORD_SETTINGS_ENTRY)?.setOnPreferenceClickListener {
            findNavController().navigate(R.id.passwordSettingsFragment)
            true
        }

        shouldAllowLower.setOnPreferenceChangeListener { preferences, _ ->
            VolumeService.start(preferences.context)
            true
        }

        hideTvLauncherIcon?.let { pref ->
            val context = requireContext()
            pref.isChecked = isTvLauncherIconHidden(context)
            pref.setOnPreferenceChangeListener { _, newValue ->
                setTvLauncherIconHidden(context, newValue as Boolean)
                true
            }
        }
    }

    private fun isTvLauncherIconHidden(context: Context): Boolean {
        val componentName = ComponentName(context, "${context.packageName}.TvLauncherAlias")
        val state = context.packageManager.getComponentEnabledSetting(componentName)
        return state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED
    }

    private fun setTvLauncherIconHidden(context: Context, hide: Boolean) {
        val componentName = ComponentName(context, "${context.packageName}.TvLauncherAlias")
        val newState = if (hide) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        }
        context.packageManager.setComponentEnabledSetting(
            componentName,
            newState,
            PackageManager.DONT_KILL_APP
        )
    }
}
