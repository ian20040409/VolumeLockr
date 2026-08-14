package com.lnu.volumelockr.plus.ui

import android.os.Bundle
import android.view.View
import com.lnu.volumelockr.plus.R
import com.lnu.volumelockr.plus.databinding.ActivityPasswordBinding

class PasswordActivity : BaseSecuredActivity() {

    private lateinit var binding: ActivityPasswordBinding

    override fun getSecuredContentView(): View = binding.passwordSettingsContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupStandardInsets(binding.appBarLayout, binding.passwordSettingsContainer)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = getString(R.string.password_header)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun onAuthenticated() {
        attachFragmentIfNeeded()
    }

    private fun attachFragmentIfNeeded() {
        if (supportFragmentManager.findFragmentById(R.id.password_settings_container) == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.password_settings_container, PasswordSettingsFragment())
                .commitAllowingStateLoss()
        }
    }
}
