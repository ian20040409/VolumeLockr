package com.lnu.volumelockr.plus.ui

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.lnu.volumelockr.plus.R
import com.lnu.volumelockr.plus.databinding.ActivityLicensesBinding
import com.lnu.volumelockr.plus.util.UrlLaunchUtils
import com.mikepenz.aboutlibraries.LibsBuilder

class LicensesActivity : BaseSecuredActivity() {

    private lateinit var binding: ActivityLicensesBinding
    private var isOpeningUrl = false

    override fun getSecuredContentView(): View = binding.aboutLibsContainer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLicensesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupStandardInsets(binding.appBarLayout, binding.aboutLibsContainer)

        setSupportActionBar(binding.toolbar)
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        supportActionBar?.title = getString(R.string.open_source_licenses)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        if (savedInstanceState == null) {
            @Suppress("DEPRECATION")
            val libsFragment = LibsBuilder().supportFragment()
            supportFragmentManager.beginTransaction()
                .add(R.id.about_libs_container, libsFragment)
                .commitAllowingStateLoss()
        }

        if (isTv) {
            val checkRunnable = object : Runnable {
                override fun run() {
                    if (isDestroyed || isFinishing) return
                    val recyclerView = UrlLaunchUtils.findRecyclerView(binding.root)
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
