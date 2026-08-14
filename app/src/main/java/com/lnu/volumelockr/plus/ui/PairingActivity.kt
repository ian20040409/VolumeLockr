package com.lnu.volumelockr.plus.ui

import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.lnu.volumelockr.plus.R
import com.lnu.volumelockr.plus.databinding.ActivityPairingBinding
import com.lnu.volumelockr.plus.service.TvRemoteServer
import com.lnu.volumelockr.plus.service.VolumeService
import java.net.NetworkInterface

import com.lnu.volumelockr.plus.util.SecurityUtils

class PairingActivity : AppCompatActivity() {

    private var isBound = false
    private val connection = object : android.content.ServiceConnection {
        override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {}
        override fun onServiceDisconnected(name: android.content.ComponentName?) {}
    }

    private val pinHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val pinRunnable = object : Runnable {
        override fun run() {
            val (pin, remainingSec) = TvRemoteServer.getOrGeneratePairingPin()
            binding.pinCodeText.text = getString(R.string.pairing_pin_label, pin, remainingSec)
            pinHandler.postDelayed(this, 1000)
        }
    }

    private val pairedReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.lnu.volumelockr.plus.ACTION_PAIRED_SUCCESS") {
                Toast.makeText(this@PairingActivity, R.string.toast_connected, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val isSecured = SecurityUtils.isPasswordProtected(this) || SecurityUtils.isBiometricEnabled(this)
        if (isSecured && !SecurityUtils.isAppUnlocked) {
            binding.nestedScrollView.visibility = View.INVISIBLE
            SecurityUtils.authenticate(
                this,
                onSuccess = {
                    SecurityUtils.isAppUnlocked = true
                    binding.nestedScrollView.visibility = View.VISIBLE
                },
                onCancel = {
                    finish()
                }
            )
        } else {
            binding.nestedScrollView.visibility = View.VISIBLE
        }
        VolumeService.start(this)
        val intent = Intent(this, VolumeService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
        isBound = true
        pinHandler.post(pinRunnable)
        ContextCompat.registerReceiver(
            this,
            pairedReceiver,
            IntentFilter("com.lnu.volumelockr.plus.ACTION_PAIRED_SUCCESS"),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        runCatching { unregisterReceiver(pairedReceiver) }
        pinHandler.removeCallbacks(pinRunnable)
        if (isBound) {
            runCatching { unbindService(connection) }
            isBound = false
        }
    }

    private lateinit var binding: ActivityPairingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityPairingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { v, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(bars.left, bars.top, bars.right, 0)
            windowInsets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.nestedScrollView) { v, windowInsets ->
            val bars = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(bars.left, 0, bars.right, bars.bottom)
            windowInsets
        }

        setSupportActionBar(binding.toolbar)
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
        val isTv = uiModeManager.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        supportActionBar?.title = getString(R.string.pair_with_phone_title)
        
        if (isTv) {
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            binding.btnBackTv.visibility = View.VISIBLE
            binding.btnBackTv.setText(android.R.string.ok)
            binding.btnBackTv.setOnClickListener { finish() }
            binding.btnBackTv.post {
                binding.btnBackTv.requestFocus()
            }
        } else {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        val ipAddress = getLocalIpAddress() ?: "Unknown"
        binding.ipAddressText.text = ipAddress

        val (pin, remainingSec) = TvRemoteServer.getOrGeneratePairingPin()
        binding.pinCodeText.text = getString(R.string.pairing_pin_label, pin, remainingSec)

        val token = TvRemoteServer.getAuthToken(this)
        if (ipAddress != "Unknown") {
            try {
                val qrData = "ip=$ipAddress&token=$token"
                val bitMatrix = com.google.zxing.qrcode.QRCodeWriter().encode(qrData, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512)
                val width = bitMatrix.width
                val height = bitMatrix.height
                val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.RGB_565)
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                    }
                }
                binding.qrCodeImage.setImageBitmap(bitmap)
                binding.qrCodeImage.visibility = View.VISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: return null
            val preferredInterfaces = interfaces.filter { intf ->
                intf.isUp && !intf.isLoopback && (intf.name.startsWith("wlan") || intf.name.startsWith("eth")) && !intf.name.contains("p2p")
            }
            val candidateInterfaces = preferredInterfaces.ifEmpty {
                interfaces.filter { intf ->
                    intf.isUp && !intf.isLoopback && !intf.name.contains("p2p") && !intf.name.contains("dummy") && !intf.name.contains("tun")
                }
            }.ifEmpty { interfaces }

            for (networkInterface in candidateInterfaces) {
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        val host = address.hostAddress
                        if (host != null && host != "127.0.0.1") {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
