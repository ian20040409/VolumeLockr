package com.lnu.volumelockr.ui

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
import com.lnu.volumelockr.R
import com.lnu.volumelockr.databinding.ActivityAboutBinding
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
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.about)

        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            binding.versionName.text = "v${pInfo.versionName}"
        } catch (e: Exception) {
            binding.versionName.visibility = View.GONE
        }

        binding.btnGithubIan.setOnClickListener {
            openUrl("https://github.com/ian20040409/VolumeLockr")
        }

        binding.btnGithubOriginal.setOnClickListener {
            openUrl("https://github.com/jonathanklee/VolumeLockr")
        }

        if (savedInstanceState == null) {
            val libsFragment = LibsBuilder()
                .supportFragment()

            supportFragmentManager.beginTransaction()
                .add(R.id.about_libs_container, libsFragment)
                .commit()
        }

        binding.root.post {
            val recyclerView = findRecyclerView(binding.root)
            recyclerView?.let { rv ->
                rv.isNestedScrollingEnabled = false
                val spanCount = if (resources.getBoolean(R.bool.use_two_columns)) 2 else 1
                if (spanCount > 1) {
                    rv.layoutManager = GridLayoutManager(this, spanCount)
                }
            }
        }
    }

    private fun openUrl(url: String) {
        runCatching {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
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
