package com.lnu.volumelockr.plus.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.lnu.volumelockr.plus.R
import com.lnu.volumelockr.plus.databinding.ActivityLicensesBinding
import com.mikepenz.aboutlibraries.LibsBuilder

class LicensesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val binding = ActivityLicensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        val uiModeManager = getSystemService(android.content.Context.UI_MODE_SERVICE) as android.app.UiModeManager
        val isTv = uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        supportActionBar?.title = getString(R.string.open_source_licenses)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            val libsFragment = LibsBuilder().supportFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.about_libs_container, libsFragment)
                .commitAllowingStateLoss()
        }

        if (isTv) {
            val checkRunnable = object : Runnable {
                override fun run() {
                    if (isDestroyed || isFinishing) return
                    val recyclerView = findRecyclerView(binding.root)
                    val adapter = recyclerView?.adapter
                    
                    if (recyclerView != null && adapter != null && recyclerView.childCount > 0) {
                        recyclerView.isFocusable = false
                        val attachListener = object : RecyclerView.OnChildAttachStateChangeListener {
                            override fun onChildViewAttachedToWindow(view: View) {
                                view.isFocusable = true
                                view.isClickable = true
                            }
                            override fun onChildViewDetachedFromWindow(view: View) {}
                        }
                        recyclerView.addOnChildAttachStateChangeListener(attachListener)
                        
                        for (i in 0 until recyclerView.childCount) {
                            attachListener.onChildViewAttachedToWindow(recyclerView.getChildAt(i))
                        }
                    } else {
                        binding.root.postDelayed(this, 150)
                    }
                }
            }
            binding.root.post(checkRunnable)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun openUrl(url: String) {
        val uiModeManager = getSystemService(android.content.Context.UI_MODE_SERVICE) as android.app.UiModeManager
        val isTv = uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        
        if (isTv) {
            showQrCodeDialog(url)
            return
        }

        runCatching {
            val customTabsIntent = androidx.browser.customtabs.CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(this, android.net.Uri.parse(url))
        }.onFailure {
            runCatching {
                super.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url)))
            }.onFailure {
                showQrCodeDialog(url)
            }
        }
    }

    private fun showQrCodeDialog(url: String) {
        runCatching {
            val size = 512
            val bits = com.google.zxing.qrcode.QRCodeWriter().encode(url, com.google.zxing.BarcodeFormat.QR_CODE, size, size)
            val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.RGB_565)
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bmp.setPixel(x, y, if (bits[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }
            
            val imageView = android.widget.ImageView(this).apply {
                setImageBitmap(bmp)
                setPadding(32, 32, 32, 32)
            }
            
            val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle(url)
                .setView(imageView)
                .setPositiveButton(android.R.string.ok, null)
                .create()
                
            dialog.setOnShowListener {
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.requestFocus()
            }
            
            dialog.show()
        }
    }

    override fun startActivity(intent: android.content.Intent?) {
        startActivity(intent, null)
    }

    private var isOpeningUrl = false

    override fun startActivity(intent: android.content.Intent?, options: Bundle?) {
        if (!isOpeningUrl && intent?.action == android.content.Intent.ACTION_VIEW) {
            val url = intent.dataString
            if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                isOpeningUrl = true
                try {
                    openUrl(url)
                } finally {
                    isOpeningUrl = false
                }
                return
            }
        }
        super.startActivity(intent, options)
    }

    private fun findRecyclerView(view: View): RecyclerView? {
        if (view is RecyclerView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val rv = findRecyclerView(child)
                if (rv != null) return rv
            }
        }
        return null
    }
}
