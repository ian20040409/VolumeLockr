package com.lnu.volumelockr.plus.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.lnu.volumelockr.plus.R
import com.lnu.volumelockr.plus.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { v, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(bars.left, bars.top, bars.right, 0)
            windowInsets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.scrollView) { v, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(bars.left, 0, bars.right, bars.bottom)
            windowInsets
        }

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

        binding.btnLicenses.setOnClickListener {
            startActivity(Intent(this, LicensesActivity::class.java))
        }
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

}
