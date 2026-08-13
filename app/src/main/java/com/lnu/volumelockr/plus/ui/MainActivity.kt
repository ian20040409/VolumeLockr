package com.lnu.volumelockr.plus.ui

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationBarView
import com.lnu.volumelockr.plus.R
import com.lnu.volumelockr.plus.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupNavigation()
        setupWindowInsets()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private var isDndDialogShowing = false
    private var activeDndDialog: androidx.appcompat.app.AlertDialog? = null

    override fun onPause() {
        super.onPause()
        binding.toolbar.dismissPopupMenus()
    }

    override fun onDestroy() {
        binding.toolbar.dismissPopupMenus()
        activeDndDialog?.dismiss()
        activeDndDialog = null
        themeAnimator?.cancel()
        themeAnimator = null
        super.onDestroy()
    }

    private fun checkPermissions() {
        val uiModeManager = getSystemService(android.content.Context.UI_MODE_SERVICE) as android.app.UiModeManager
        val isTv = uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION

        val notificationManager = getSystemService(android.content.Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (!isTv && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && !notificationManager.isNotificationPolicyAccessGranted) {
            if (!isDndDialogShowing) {
                isDndDialogShowing = true
                activeDndDialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("需要勿擾模式權限")
                    .setMessage("請授予勿擾模式(DND)權限，否則將無法調整與鎖定鈴聲音量。")
                    .setPositiveButton("前往設定") { _, _ ->
                        isDndDialogShowing = false
                        try {
                            val intent = android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    .setNegativeButton("忽略") { dialog, _ ->
                        isDndDialogShowing = false
                        dialog.dismiss()
                        checkPostNotificationsPermission()
                    }
                    .setOnCancelListener {
                        isDndDialogShowing = false
                        checkPostNotificationsPermission()
                    }
                    .show()
            }
            return
        }

        checkPostNotificationsPermission()
    }

    private fun checkPostNotificationsPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            val perms = mutableListOf<String>()
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                perms.add(android.Manifest.permission.POST_NOTIFICATIONS)
            }
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.NEARBY_WIFI_DEVICES
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                perms.add(android.Manifest.permission.NEARBY_WIFI_DEVICES)
            }
            if (perms.isNotEmpty()) {
                androidx.core.app.ActivityCompat.requestPermissions(
                    this,
                    perms.toTypedArray(),
                    101
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options, menu)
        
        val uiModeManager = getSystemService(android.content.Context.UI_MODE_SERVICE) as android.app.UiModeManager
        if (uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION) {
            menu.findItem(R.id.about)?.isVisible = false
        }
        
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.about -> {
                startActivity(android.content.Intent(this, AboutActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupNavigation() {
        setSupportActionBar(binding.toolbar)
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragment_container_view) as NavHostFragment
        navController = navHostFragment.navController
        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.volumeSliderFragment, R.id.settingsFragment)
        )
        binding.toolbar.setupWithNavController(navController, appBarConfiguration)

        val navView: NavigationBarView? = binding.bottomNavigation ?: binding.navigationRail
        navView?.setupWithNavController(navController)
        
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val view = navView?.findViewById<android.view.View>(destination.id)
            val iconView = view?.findViewById<android.widget.ImageView>(com.google.android.material.R.id.navigation_bar_item_icon_view)
            iconView?.apply {
                if (destination.id == R.id.settingsFragment) {
                    animate()
                        .rotationBy(180f)
                        .setDuration(500)
                        .start()
                } else {
                    scaleX = 0.8f
                    scaleY = 0.8f
                    animate()
                        .scaleX(1.2f)
                        .scaleY(1.2f)
                        .setDuration(350)
                        .withEndAction {
                            animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(400)
                                .start()
                        }
                        .start()
                }
            }
            
            if (destination.id == R.id.tvRemoteFragment) {
                applyThemeColors(isTvRemote = true)
            } else {
                applyThemeColors(isTvRemote = false)
            }
        }
        
        val uiModeManager = getSystemService(android.content.Context.UI_MODE_SERVICE) as android.app.UiModeManager
        if (uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION) {
            (navView as? android.view.View)?.visibility = android.view.View.GONE
        }
    }

    private fun setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { v, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(bars.left, bars.top, bars.right, 0)
            WindowInsetsCompat.CONSUMED
        }

        binding.bottomNavigation?.let { view ->
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
                val bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
                )
                v.setPadding(bars.left, 0, bars.right, bars.bottom)
                WindowInsetsCompat.CONSUMED
            }
        }

        binding.navigationRail?.let { view ->
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
                val bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
                )
                v.setPadding(bars.left, bars.top, 0, bars.bottom)
                WindowInsetsCompat.CONSUMED
            }
        }
    }

    private var themeAnimator: android.animation.ValueAnimator? = null

    @Suppress("DEPRECATION")
    private fun applyThemeColors(isTvRemote: Boolean) {
        val themeContext = if (isTvRemote) {
            android.view.ContextThemeWrapper(this, R.style.ThemeOverlay_App_RoyalPurple)
        } else {
            this
        }
        
        val targetSurface = com.google.android.material.color.MaterialColors.getColor(themeContext, com.google.android.material.R.attr.colorSurface, android.graphics.Color.BLACK)
        val targetOnSurface = com.google.android.material.color.MaterialColors.getColor(themeContext, com.google.android.material.R.attr.colorOnSurface, android.graphics.Color.WHITE)
        val colorSecondaryContainer = com.google.android.material.color.MaterialColors.getColor(themeContext, com.google.android.material.R.attr.colorSecondaryContainer, android.graphics.Color.GRAY)
        val colorOnSecondaryContainer = com.google.android.material.color.MaterialColors.getColor(themeContext, com.google.android.material.R.attr.colorOnSecondaryContainer, android.graphics.Color.BLACK)
        val colorOnSurfaceVariant = com.google.android.material.color.MaterialColors.getColor(themeContext, com.google.android.material.R.attr.colorOnSurfaceVariant, android.graphics.Color.GRAY)
        
        val typedValue = android.util.TypedValue()
        val hasSurfaceContainer = themeContext.theme.resolveAttribute(com.google.android.material.R.attr.colorSurfaceContainer, typedValue, true)
        val targetBottomNavBg = if (hasSurfaceContainer) typedValue.data else targetSurface

        binding.toolbar.setTitleTextColor(targetOnSurface)
        binding.toolbar.navigationIcon?.setTint(targetOnSurface)

        val navView = binding.bottomNavigation ?: binding.navigationRail
        if (navView != null) {
            val iconTintList = android.content.res.ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                ),
                intArrayOf(colorOnSecondaryContainer, colorOnSurfaceVariant)
            )
            val textColorList = android.content.res.ColorStateList(
                arrayOf(
                    intArrayOf(android.R.attr.state_checked),
                    intArrayOf(-android.R.attr.state_checked)
                ),
                intArrayOf(targetOnSurface, colorOnSurfaceVariant)
            )
            
            if (navView is com.google.android.material.bottomnavigation.BottomNavigationView) {
                navView.itemActiveIndicatorColor = android.content.res.ColorStateList.valueOf(colorSecondaryContainer)
                navView.itemIconTintList = iconTintList
                navView.itemTextColor = textColorList
            } else if (navView is com.google.android.material.navigationrail.NavigationRailView) {
                navView.itemActiveIndicatorColor = android.content.res.ColorStateList.valueOf(colorSecondaryContainer)
                navView.itemIconTintList = iconTintList
                navView.itemTextColor = textColorList
            }
        }

        val initialSurface = (binding.root.background as? android.graphics.drawable.ColorDrawable)?.color ?: window.statusBarColor
        val initialBottomNavBg = window.navigationBarColor

        themeAnimator?.cancel()
        themeAnimator = android.animation.ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300
            val argbEvaluator = android.animation.ArgbEvaluator()
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                val currentSurface = argbEvaluator.evaluate(fraction, initialSurface, targetSurface) as Int
                val currentBottomNavBg = argbEvaluator.evaluate(fraction, initialBottomNavBg, targetBottomNavBg) as Int

                binding.root.setBackgroundColor(currentSurface)
                binding.fragmentContainerView.setBackgroundColor(currentSurface)
                window.decorView.setBackgroundColor(currentSurface)
                binding.appBarLayout.setBackgroundColor(currentSurface)
                binding.toolbar.setBackgroundColor(currentSurface)
                window.statusBarColor = currentSurface
                window.navigationBarColor = currentBottomNavBg

                if (navView is com.google.android.material.bottomnavigation.BottomNavigationView) {
                    navView.setBackgroundColor(currentBottomNavBg)
                } else if (navView is com.google.android.material.navigationrail.NavigationRailView) {
                    navView.setBackgroundColor(currentBottomNavBg)
                }
            }
            start()
        }

        val isLightSurface = androidx.core.graphics.ColorUtils.calculateLuminance(targetSurface) > 0.5
        val isLightBottomNavBg = androidx.core.graphics.ColorUtils.calculateLuminance(targetBottomNavBg) > 0.5
        val windowInsetsController = androidx.core.view.WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = isLightSurface
        windowInsetsController.isAppearanceLightNavigationBars = isLightBottomNavBg
    }
}
