package com.lnu.volumelockr.plus.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.lnu.volumelockr.plus.util.SecurityUtils

abstract class BaseSecuredActivity : AppCompatActivity() {

    abstract fun getSecuredContentView(): View?

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        checkAppLock()
    }

    protected open fun onAuthenticated() {}

    protected fun checkAppLock() {
        val isSecured = SecurityUtils.isPasswordProtected(this) || SecurityUtils.isBiometricEnabled(this)
        if (isSecured && !SecurityUtils.isAppUnlocked) {
            getSecuredContentView()?.visibility = View.INVISIBLE
            SecurityUtils.authenticate(
                this,
                onSuccess = {
                    SecurityUtils.isAppUnlocked = true
                    getSecuredContentView()?.visibility = View.VISIBLE
                    onAuthenticated()
                },
                onCancel = {
                    finish()
                }
            )
        } else {
            getSecuredContentView()?.visibility = View.VISIBLE
            onAuthenticated()
        }
    }

    protected fun setupStandardInsets(appBarLayout: View?, contentContainer: View?) {
        appBarLayout?.let { abl ->
            ViewCompat.setOnApplyWindowInsetsListener(abl) { v, windowInsets ->
                val bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
                v.setPadding(bars.left, bars.top, bars.right, 0)
                windowInsets
            }
        }

        contentContainer?.let { cc ->
            ViewCompat.setOnApplyWindowInsetsListener(cc) { v, windowInsets ->
                val bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
                )
                v.setPadding(bars.left, 0, bars.right, bars.bottom)
                windowInsets
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
