package com.lnu.volumelockr.plus.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.lnu.volumelockr.plus.R
import com.lnu.volumelockr.plus.databinding.ActivityAboutBinding
import com.mikepenz.aboutlibraries.LibsBuilder

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        val uiModeManager = getSystemService(android.content.Context.UI_MODE_SERVICE) as android.app.UiModeManager
        val isTv = uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        supportActionBar?.title = getString(R.string.about)
        
        if (isTv) {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            binding.btnBackTv.visibility = View.VISIBLE
            binding.btnBackTv.setOnClickListener { finish() }
        } else {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            binding.versionName.text = "v${pInfo.versionName}"
        } catch (e: Exception) {
            binding.versionName.visibility = View.GONE
        }

        binding.btnGithubIan.setOnClickListener {
            openUrl("https://github.com/ian20040409/VolumeLockr-PLUS")
        }

        binding.btnGithubOriginal.setOnClickListener {
            openUrl("https://github.com/jonathanklee/VolumeLockr")
        }

        if (savedInstanceState == null) {
            binding.root.postDelayed({
                if (!isDestroyed && !isFinishing) {
                    val libsFragment = LibsBuilder().supportFragment()
                    supportFragmentManager.beginTransaction()
                        .add(R.id.about_libs_container, libsFragment)
                        .commitAllowingStateLoss()
                }
            }, 200)
        }

        var isLoaded = false
        fun revealScreen() {
            if (isLoaded) return
            isLoaded = true

            binding.loadingIndicator.visibility = View.GONE
            binding.aboutLibsContainer.visibility = View.VISIBLE
            android.widget.Toast.makeText(this, R.string.toast_libs_loaded, android.widget.Toast.LENGTH_SHORT).show()
        }

        val checkRunnable = object : Runnable {
            override fun run() {
                if (isLoaded) return
                val recyclerView = findRecyclerView(binding.root)
                val adapter = recyclerView?.adapter
                val itemCount = adapter?.itemCount ?: 0

                // AboutLibraries has multiple items once loaded. Wait for JSON parsing AND render of 3 items.
                val isFullyLoadedAndRendered = recyclerView != null &&
                        adapter != null &&
                        itemCount >= 3 &&
                        recyclerView.childCount >= 3 &&
                        (recyclerView.getChildAt(2)?.height ?: 0) > 0

                if (isFullyLoadedAndRendered) {
                    recyclerView?.let { rv ->
                        val spanCount = if (resources.getBoolean(R.bool.use_two_columns)) 2 else 1
                        if (spanCount > 1 && rv.layoutManager !is GridLayoutManager) {
                            rv.layoutManager = GridLayoutManager(this@AboutActivity, spanCount)
                        }

                        // Make RecyclerView items focusable for Android TV D-Pad scrolling
                        if (isTv) {
                            rv.isFocusable = false
                            val attachListener = object : RecyclerView.OnChildAttachStateChangeListener {
                                override fun onChildViewAttachedToWindow(view: View) {
                                    view.isFocusable = true
                                    view.isClickable = true
                                    val typedValue = android.util.TypedValue()
                                    theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
                                    view.setBackgroundResource(typedValue.resourceId)
                                }
                                override fun onChildViewDetachedFromWindow(view: View) {}
                            }
                            rv.addOnChildAttachStateChangeListener(attachListener)
                            
                            // Apply to already attached children
                            for (i in 0 until rv.childCount) {
                                attachListener.onChildViewAttachedToWindow(rv.getChildAt(i))
                            }
                        }
                    }

                    // Wait 2 full GPU rendering frames before revealing to eliminate UI freezes
                    android.view.Choreographer.getInstance().postFrameCallback {
                        android.view.Choreographer.getInstance().postFrameCallback {
                            revealScreen()
                        }
                    }
                } else {
                    binding.root.postDelayed(this, 150)
                }
            }
        }
        binding.root.post(checkRunnable)

        val timeoutMs = if (isTv) 12000L else 5000L
        binding.root.postDelayed({
            revealScreen()
        }, timeoutMs)
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
            customTabsIntent.launchUrl(this, Uri.parse(url))
        }.onFailure {
            // Fallback to standard intent if custom tabs fail
            runCatching {
                super.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun startActivity(intent: Intent?) {
        startActivity(intent, null)
    }

    private var isOpeningUrl = false

    override fun startActivity(intent: Intent?, options: Bundle?) {
        if (!isOpeningUrl && intent?.action == Intent.ACTION_VIEW) {
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
