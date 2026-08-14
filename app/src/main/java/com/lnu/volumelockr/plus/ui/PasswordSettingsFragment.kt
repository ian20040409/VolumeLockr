package com.lnu.volumelockr.plus.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.lnu.volumelockr.plus.R
import com.lnu.volumelockr.plus.util.SecurityUtils

class PasswordSettingsFragment : PreferenceFragmentCompat() {

    companion object {
        const val PASSWORD_PROTECTED_PREFERENCE = "password_protected"
        const val BIOMETRIC_PROTECTED_PREFERENCE = "biometric_protected"
        const val PASSWORD_CHANGE_PREFERENCE = "password"
        const val DELAY_IN_MS = 100L
        const val MIN_PASSWORD_LENGTH = 4
    }

    private lateinit var passwordProtected: SwitchPreferenceCompat
    private lateinit var biometricProtected: SwitchPreferenceCompat
    private lateinit var passwordChange: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.password_preferences, rootKey)

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

                    val isPwd = SecurityUtils.isPasswordSet(requireContext())
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
                    performAuthentication {
                        biometricProtected.isChecked = false
                    }
                    false
                }
            }
        }

        passwordChange.isVisible = SecurityUtils.isPasswordSet(requireContext())
        passwordChange.setOnPreferenceClickListener {
            performAuthentication {
                showChangePasswordDialog(isInitialSet = false)
            }
            true
        }

        passwordProtected.setOnPreferenceChangeListener { _, value ->
            if (value == true) {
                if (!SecurityUtils.isPasswordSet(requireContext())) {
                    showChangePasswordDialog(isInitialSet = true)
                    false
                } else {
                    true
                }
            } else {
                performAuthentication {
                    passwordProtected.isChecked = false
                    SecurityUtils.clearPassword(requireContext())
                    passwordChange.isVisible = false
                }
                false
            }
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
                    if (SecurityUtils.savePassword(requireContext(), password)) {
                        dialog.dismiss()
                        if (isInitialSet) {
                            passwordProtected.isChecked = true
                        }
                        passwordChange.isVisible = true
                    } else {
                        Toast.makeText(context, R.string.password_save_error, Toast.LENGTH_SHORT).show()
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

    private fun showKeyboard(view: View) {
        val service = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        service.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }
}
