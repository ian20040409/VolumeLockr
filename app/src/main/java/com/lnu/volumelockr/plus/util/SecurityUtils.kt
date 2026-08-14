package com.lnu.volumelockr.plus.util

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Base64
import android.util.Log
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
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object SecurityUtils {

    private const val TAG = "SecurityUtils"
    private const val ENCRYPTED_PREFS_FILE = "secure_settings"
    private const val FALLBACK_PREFS_FILE = "secure_settings_fallback"
    private const val PBKDF2_ITERATIONS = 10000
    private const val KEY_LENGTH = 256
    private const val SALT_LENGTH = 16

    var isAppUnlocked: Boolean = false

    fun getSecurePreferences(context: Context): SharedPreferences {
        var securePrefs: SharedPreferences? = null
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            securePrefs = EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to open EncryptedSharedPreferences, attempting reset and recovery", e)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context.deleteSharedPreferences(ENCRYPTED_PREFS_FILE)
                } else {
                    context.getSharedPreferences(ENCRYPTED_PREFS_FILE, Context.MODE_PRIVATE).edit().clear().commit()
                }

                try {
                    val keyStore = KeyStore.getInstance("AndroidKeyStore")
                    keyStore.load(null)
                    keyStore.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
                    keyStore.deleteEntry("_androidx_security_master_key_")
                } catch (_: Exception) {
                    // Ignore keystore cleanup errors
                }

                val masterKey = MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()

                securePrefs = EncryptedSharedPreferences.create(
                    context,
                    ENCRYPTED_PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                )
            } catch (recoveryEx: Exception) {
                Log.e(TAG, "EncryptedSharedPreferences recovery failed, falling back to secure fallback storage", recoveryEx)
            }
        }

        return securePrefs ?: context.getSharedPreferences(FALLBACK_PREFS_FILE, Context.MODE_PRIVATE)
    }

    fun isPasswordProtected(context: Context): Boolean {
        val isEnabled = PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(SettingsFragment.PASSWORD_PROTECTED_PREFERENCE, false)
        if (!isEnabled) {
            return false
        }

        val hasPassword = isPasswordSet(context)
        if (!hasPassword) {
            PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(SettingsFragment.PASSWORD_PROTECTED_PREFERENCE, false)
                .apply()
        }
        return hasPassword
    }

    fun isBiometricEnabled(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
            .getBoolean(SettingsFragment.BIOMETRIC_PROTECTED_PREFERENCE, false)
    }

    fun canUseBiometric(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        } else {
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
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
            showBiometricPrompt(
                activity = activity,
                isPwd = isPwd,
                onSuccess = onSuccess,
                onFallbackToPassword = {
                    if (isPwd) {
                        showPasswordDialog(activity, onSuccess, onCancel)
                    } else {
                        onCancel?.invoke()
                    }
                },
                onCancel = onCancel
            )
        } else if (isPwd) {
            showPasswordDialog(activity, onSuccess, onCancel)
        } else {
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
        onFallbackToPassword: () -> Unit,
        onCancel: (() -> Unit)? = null
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
                } else if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_CANCELED
                ) {
                    onCancel?.invoke()
                } else {
                    Toast.makeText(activity, errString, Toast.LENGTH_SHORT).show()
                    if (isPwd) {
                        onFallbackToPassword()
                    } else {
                        onCancel?.invoke()
                    }
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
            editText.postDelayed({
                val service = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                service.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
            }, SettingsFragment.DELAY_IN_MS)
        }

        dialog.show()
    }

    fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return salt
    }

    fun hashPassword(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    fun savePassword(context: Context, newPassword: String): Boolean {
        return try {
            val prefs = getSecurePreferences(context)
            val salt = generateSalt()
            val hash = hashPassword(newPassword, salt)

            val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
            val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)

            prefs.edit()
                .putString(SettingsFragment.PASSWORD_SALT_PREFERENCE, saltBase64)
                .putString(SettingsFragment.PASSWORD_HASH_PREFERENCE, hashBase64)
                .remove(SettingsFragment.PASSWORD_CHANGE_PREFERENCE)
                .apply()

            val defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context)
            if (defaultPrefs.contains(SettingsFragment.PASSWORD_CHANGE_PREFERENCE)) {
                defaultPrefs.edit().remove(SettingsFragment.PASSWORD_CHANGE_PREFERENCE).apply()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save password", e)
            false
        }
    }

    fun clearPassword(context: Context) {
        try {
            getSecurePreferences(context).edit()
                .remove(SettingsFragment.PASSWORD_SALT_PREFERENCE)
                .remove(SettingsFragment.PASSWORD_HASH_PREFERENCE)
                .remove(SettingsFragment.PASSWORD_CHANGE_PREFERENCE)
                .apply()

            val defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context)
            defaultPrefs.edit()
                .remove(SettingsFragment.PASSWORD_CHANGE_PREFERENCE)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear password", e)
        }
    }

    fun verifyPassword(context: Context, challenger: String): Boolean {
        var isMatch = false
        try {
            val prefs = getSecurePreferences(context)
            val saltBase64 = prefs.getString(SettingsFragment.PASSWORD_SALT_PREFERENCE, null)
            val hashBase64 = prefs.getString(SettingsFragment.PASSWORD_HASH_PREFERENCE, null)

            if (saltBase64 != null && hashBase64 != null) {
                val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
                val storedHash = Base64.decode(hashBase64, Base64.NO_WRAP)
                val computedHash = hashPassword(challenger, salt)
                isMatch = MessageDigest.isEqual(storedHash, computedHash)
            } else {
                val legacyEncrypted = prefs.getString(SettingsFragment.PASSWORD_CHANGE_PREFERENCE, null)
                val defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context)
                val legacyDefault = defaultPrefs.getString(SettingsFragment.PASSWORD_CHANGE_PREFERENCE, null)
                val legacy = legacyEncrypted ?: legacyDefault
                if (!legacy.isNullOrEmpty() && MessageDigest.isEqual(legacy.toByteArray(), challenger.toByteArray())) {
                    savePassword(context, challenger)
                    isMatch = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error verifying password", e)
        }
        return isMatch
    }

    fun isPasswordSet(context: Context): Boolean {
        var isSet = false
        try {
            val prefs = getSecurePreferences(context)
            val hash = prefs.getString(SettingsFragment.PASSWORD_HASH_PREFERENCE, null)
            val legacyEncrypted = prefs.getString(SettingsFragment.PASSWORD_CHANGE_PREFERENCE, null)

            if (!hash.isNullOrEmpty() || !legacyEncrypted.isNullOrEmpty()) {
                isSet = true
            } else {
                val defaultPrefs = PreferenceManager.getDefaultSharedPreferences(context)
                val legacyDefault = defaultPrefs.getString(SettingsFragment.PASSWORD_CHANGE_PREFERENCE, null)
                if (!legacyDefault.isNullOrEmpty()) {
                    savePassword(context, legacyDefault)
                    isSet = true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if password is set", e)
        }
        return isSet
    }
}
