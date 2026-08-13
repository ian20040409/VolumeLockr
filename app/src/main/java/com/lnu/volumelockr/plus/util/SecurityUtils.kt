package com.lnu.volumelockr.plus.util

import android.content.Context
import android.os.Build
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.lnu.volumelockr.plus.R
import com.lnu.volumelockr.plus.ui.SettingsFragment
import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurityUtils {

    fun isPasswordProtected(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(SettingsFragment.PASSWORD_PROTECTED_PREFERENCE, false)
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(SettingsFragment.BIOMETRIC_PROTECTED_PREFERENCE, false)
    }

    fun canUseBiometric(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
        }
        val status = biometricManager.canAuthenticate(authenticators)
        return status == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        val isPwd = isPasswordProtected(activity)
        val isBio = isBiometricEnabled(activity)

        if (!isPwd && !isBio) {
            onSuccess()
            return
        }

        if (isBio && canUseBiometric(activity)) {
            showBiometricPrompt(activity, isPwd, onSuccess) {
                if (isPwd) {
                    showPasswordDialog(activity, onSuccess, onCancel)
                } else {
                    onCancel?.invoke()
                }
            }
        } else if (isPwd) {
            showPasswordDialog(activity, onSuccess, onCancel)
        } else {
            // Biometric is enabled but currently unavailable (e.g., user deleted fingerprints from system),
            // and there is no password fallback. Since modifying system fingerprints requires the device PIN,
            // we can safely assume the owner did this. We automatically disable the lock to prevent them from being permanently locked out.
            PreferenceManager.getDefaultSharedPreferences(activity)
                .edit()
                .putBoolean(SettingsFragment.BIOMETRIC_PROTECTED_PREFERENCE, false)
                .apply()
            
            Toast.makeText(activity, R.string.biometric_not_available, Toast.LENGTH_LONG).show()
            onSuccess()
        }
    }

    fun showBiometricPrompt(
        activity: FragmentActivity,
        isPwd: Boolean,
        onSuccess: () -> Unit,
        onFallbackToPassword: () -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    onFallbackToPassword()
                } else if (errorCode != BiometricPrompt.ERROR_USER_CANCELED) {
                    Toast.makeText(activity, errString, Toast.LENGTH_SHORT).show()
                    onFallbackToPassword()
                }
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)
        val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(activity.getString(R.string.biometric_prompt_title))
            .setSubtitle(activity.getString(R.string.biometric_prompt_subtitle))
            
        if (isPwd) {
            promptInfoBuilder.setNegativeButtonText(activity.getString(R.string.enter_password))
        } else {
            promptInfoBuilder.setNegativeButtonText(activity.getString(android.R.string.cancel))
        }

        try {
            biometricPrompt.authenticate(promptInfoBuilder.build())
        } catch (e: Exception) {
            onFallbackToPassword()
        }
    }

    fun showPasswordDialog(
        activity: FragmentActivity,
        onSuccess: () -> Unit,
        onCancel: (() -> Unit)? = null
    ) {
        val view = LayoutInflater.from(activity).inflate(R.layout.dialog_password, null)
        val editText = view.findViewById<EditText>(android.R.id.edit)

        val dialog = MaterialAlertDialogBuilder(activity)
            .setTitle(activity.getString(R.string.enter_password))
            .setCancelable(true)
            .setView(view)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val challenger = editText.text.toString()
                if (verifyPassword(activity, challenger)) {
                    onSuccess()
                } else {
                    Toast.makeText(activity, R.string.wrong_password, Toast.LENGTH_SHORT).show()
                    onCancel?.invoke()
                }
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                onCancel?.invoke()
            }
            .setOnCancelListener {
                onCancel?.invoke()
            }
            .create()

        dialog.setOnShowListener {
            editText.requestFocus()
            editText.postDelayed({ showKeyboard(activity, editText) }, SettingsFragment.DELAY_IN_MS)
        }

        dialog.show()
    }

    private fun showKeyboard(context: Context, view: View) {
        val service = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        service.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    fun verifyPassword(context: Context, challenger: String): Boolean {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            val prefs = EncryptedSharedPreferences.create(
                context,
                "secure_settings",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val saltBase64 = prefs.getString(SettingsFragment.PASSWORD_SALT_PREFERENCE, null)
            val hashBase64 = prefs.getString(SettingsFragment.PASSWORD_HASH_PREFERENCE, null)

            if (saltBase64 != null && hashBase64 != null) {
                val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
                val storedHash = Base64.decode(hashBase64, Base64.NO_WRAP)
                val spec = PBEKeySpec(challenger.toCharArray(), salt, 10000, 256)
                val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                val computedHash = factory.generateSecret(spec).encoded
                return MessageDigest.isEqual(storedHash, computedHash)
            }

            val legacyPassword = prefs.getString(SettingsFragment.PASSWORD_CHANGE_PREFERENCE, null)
            if (!legacyPassword.isNullOrEmpty()) {
                return MessageDigest.isEqual(legacyPassword.toByteArray(), challenger.toByteArray())
            }

            false
        } catch (e: Exception) {
            false
        }
    }
}
