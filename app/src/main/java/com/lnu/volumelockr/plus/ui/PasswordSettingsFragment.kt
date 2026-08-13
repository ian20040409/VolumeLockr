package com.lnu.volumelockr.plus.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.lnu.volumelockr.plus.R
import com.lnu.volumelockr.plus.util.SecurityUtils
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class PasswordSettingsFragment : PreferenceFragmentCompat() {

    companion object {
        private const val TAG = "PasswordSettings"
        const val PASSWORD_PROTECTED_PREFERENCE = "password_protected"
        const val BIOMETRIC_PROTECTED_PREFERENCE = "biometric_protected"
        const val PASSWORD_CHANGE_PREFERENCE = "password"
        const val PASSWORD_SALT_PREFERENCE = "password_salt"
        const val PASSWORD_HASH_PREFERENCE = "password_hash"
        const val DELAY_IN_MS = 100L
        const val MIN_PASSWORD_LENGTH = 4
        private const val ENCRYPTED_PREFS_FILE = "secure_settings"
        private const val PBKDF2_ITERATIONS = 10000
        private const val KEY_LENGTH = 256
    }

    private var encryptedPrefs: SharedPreferences? = null

    private lateinit var passwordProtected: SwitchPreferenceCompat
    private lateinit var biometricProtected: SwitchPreferenceCompat
    private lateinit var passwordChange: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.password_preferences, rootKey)
        initializeEncryptedPrefs()
        
        val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
        val isTv = uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION

        passwordChange = findPreference(PASSWORD_CHANGE_PREFERENCE)!!
        passwordProtected = findPreference(PASSWORD_PROTECTED_PREFERENCE)!!
        biometricProtected = findPreference(BIOMETRIC_PROTECTED_PREFERENCE)!!

        if (isTv) {
            biometricProtected.isVisible = false
        } else {
            biometricProtected.isVisible = true
            biometricProtected.isEnabled = true
            biometricProtected.setOnPreferenceChangeListener { _, newValue ->
                if (newValue == true) {
                    if (!SecurityUtils.canUseBiometric(requireContext())) {
                        Toast.makeText(context, R.string.biometric_not_available, Toast.LENGTH_LONG).show()
                        return@setOnPreferenceChangeListener false
                    }
                    
                    val isPwd = isPasswordSet()
                    SecurityUtils.showBiometricPrompt(
                        requireActivity(),
                        isPwd,
                        onSuccess = {
                            biometricProtected.isChecked = true
                        },
                        onFallbackToPassword = {
                            if (isPwd) {
                                SecurityUtils.showPasswordDialog(
                                    requireActivity(),
                                    onSuccess = { biometricProtected.isChecked = true },
                                    onCancel = null
                                )
                            }
                        }
                    )
                    false
                } else {
                    biometricProtected.isChecked = false
                    false
                }
            }
        }

        passwordChange.isVisible = isPasswordSet()
        passwordChange.setOnPreferenceClickListener {
            performAuthentication {
                showChangePasswordDialog(isInitialSet = false)
            }
            true
        }

        passwordProtected.setOnPreferenceChangeListener { _, value ->
            if (value == true) {
                if (!isPasswordSet()) {
                    showChangePasswordDialog(isInitialSet = true)
                    false
                } else {
                    true
                }
            } else {
                passwordProtected.isChecked = false
                clearPassword()
                passwordChange.isVisible = false
                false
            }
        }
    }

    private fun initializeEncryptedPrefs() {
        try {
            val masterKey = MasterKey.Builder(requireContext())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            encryptedPrefs = EncryptedSharedPreferences.create(
                requireContext(),
                ENCRYPTED_PREFS_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "Failed to create encrypted preferences: security error", e)
            Toast.makeText(context, R.string.password_save_error, Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            Log.e(TAG, "Failed to create encrypted preferences: IO error", e)
            Toast.makeText(context, R.string.password_save_error, Toast.LENGTH_LONG).show()
        }
    }

    private fun performAuthentication(onSuccess: () -> Unit) {
        SecurityUtils.authenticate(requireActivity(), onSuccess) {
            // onCancel
        }
    }

    private fun showChangePasswordDialog(isInitialSet: Boolean = false) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_password, null)
        val inputLayout = view.findViewById<TextInputLayout>(R.id.password_input_layout)
        val editText = view.findViewById<EditText>(android.R.id.edit)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.change_password))
            .setView(view)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, null)
            .create()

        dialog.setOnShowListener {
            editText.requestFocus()
            editText.postDelayed({ showKeyboard(editText) }, DELAY_IN_MS)
            
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val password = editText.text.toString()
                val validationError = validatePassword(password)

                if (validationError != null) {
                    inputLayout.error = validationError
                } else {
                    inputLayout.error = null
                    if (savePassword(password)) {
                        dialog.dismiss()
                        if (isInitialSet) {
                            passwordProtected.isChecked = true
                        }
                        passwordChange.isVisible = true
                    }
                }
            }
        }

        dialog.show()
    }

    private fun validatePassword(password: String): String? {
        if (password.length < MIN_PASSWORD_LENGTH) {
            return getString(R.string.password_too_short, MIN_PASSWORD_LENGTH)
        }
        return null
    }

    private fun generateSalt(): ByteArray {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return salt
    }

    private fun hashPassword(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        return factory.generateSecret(spec).encoded
    }

    private fun savePassword(newPassword: String): Boolean {
        val prefs = encryptedPrefs
        if (prefs == null) {
            Toast.makeText(context, R.string.password_save_error, Toast.LENGTH_SHORT).show()
            return false
        }

        return try {
            val salt = generateSalt()
            val hash = hashPassword(newPassword, salt)
            val saltBase64 = Base64.encodeToString(salt, Base64.NO_WRAP)
            val hashBase64 = Base64.encodeToString(hash, Base64.NO_WRAP)

            prefs.edit()
                .putString(PASSWORD_SALT_PREFERENCE, saltBase64)
                .putString(PASSWORD_HASH_PREFERENCE, hashBase64)
                .remove(PASSWORD_CHANGE_PREFERENCE) // Remove legacy plaintext password
                .apply()

            passwordProtected.isEnabled = true
            if (biometricProtected.isVisible) {
                biometricProtected.isEnabled = true
            }
            true
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "Failed to save password: security error", e)
            Toast.makeText(context, R.string.password_save_error, Toast.LENGTH_SHORT).show()
            false
        } catch (e: IOException) {
            Log.e(TAG, "Failed to save password: IO error", e)
            Toast.makeText(context, R.string.password_save_error, Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun clearPassword() {
        encryptedPrefs?.edit()
            ?.remove(PASSWORD_SALT_PREFERENCE)
            ?.remove(PASSWORD_HASH_PREFERENCE)
            ?.remove(PASSWORD_CHANGE_PREFERENCE)
            ?.apply()
    }

    private fun verifyPassword(challenger: String): Boolean {
        val prefs = encryptedPrefs ?: return false
        val saltBase64 = prefs.getString(PASSWORD_SALT_PREFERENCE, null)
        val hashBase64 = prefs.getString(PASSWORD_HASH_PREFERENCE, null)

        if (saltBase64 != null && hashBase64 != null) {
            return try {
                val salt = Base64.decode(saltBase64, Base64.NO_WRAP)
                val storedHash = Base64.decode(hashBase64, Base64.NO_WRAP)
                val computedHash = hashPassword(challenger, salt)
                MessageDigest.isEqual(storedHash, computedHash)
            } catch (e: Exception) {
                false
            }
        }

        // Backward compatibility migration for legacy plaintext password
        val legacyPassword = prefs.getString(PASSWORD_CHANGE_PREFERENCE, null)
        if (!legacyPassword.isNullOrEmpty()) {
            val isLegacyMatch = MessageDigest.isEqual(legacyPassword.toByteArray(), challenger.toByteArray())
            if (isLegacyMatch) {
                savePassword(challenger) // Migrate to PBKDF2 hash format
                return true
            }
        }

        return false
    }

    private fun isPasswordSet(): Boolean {
        val prefs = encryptedPrefs ?: return false
        val hash = prefs.getString(PASSWORD_HASH_PREFERENCE, null)
        val legacy = prefs.getString(PASSWORD_CHANGE_PREFERENCE, null)
        return !hash.isNullOrEmpty() || !legacy.isNullOrEmpty()
    }

    private fun showKeyboard(view: View) {
        val service = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        service.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
}
