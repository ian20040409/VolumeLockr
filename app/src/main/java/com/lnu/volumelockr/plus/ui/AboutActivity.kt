package com.lnu.volumelockr.plus.ui

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import com.lnu.volumelockr.plus.R
import com.lnu.volumelockr.plus.databinding.ActivityAboutBinding
import com.lnu.volumelockr.plus.util.UrlLaunchUtils

class AboutActivity : BaseSecuredActivity() {

    private lateinit var binding: ActivityAboutBinding
    private var isOpeningUrl = false

    override fun getSecuredContentView(): View = binding.scrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupStandardInsets(binding.appBarLayout, binding.scrollView)

        setSupportActionBar(binding.toolbar)
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
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
        } catch (_: Exception) {
            binding.versionName.visibility = View.GONE
        }

        binding.btnGithubIan.setOnClickListener {
            UrlLaunchUtils.openUrl(this, "https://github.com/ian20040409/VolumeLockr-PLUS")
        }

        binding.btnGithubOriginal.setOnClickListener {
            UrlLaunchUtils.openUrl(this, "https://github.com/jonathanklee/VolumeLockr")
        }

        binding.btnLicenses.setOnClickListener {
            startActivity(Intent(this, LicensesActivity::class.java))
        }
    }

    override fun startActivity(intent: Intent?) {
        startActivity(intent, null)
    }

    override fun startActivity(intent: Intent?, options: Bundle?) {
        if (!isOpeningUrl && intent?.action == Intent.ACTION_VIEW) {
            val url = intent.dataString
            if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                isOpeningUrl = true
                try {
                    UrlLaunchUtils.openUrl(this, url)
                } finally {
                    isOpeningUrl = false
                }
                return
            }
        }
        super.startActivity(intent, options)
    }
}
