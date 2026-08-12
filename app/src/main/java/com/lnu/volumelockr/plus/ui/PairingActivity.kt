package com.lnu.volumelockr.plus.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.lnu.volumelockr.plus.R
import com.lnu.volumelockr.plus.databinding.ActivityPairingBinding
import com.lnu.volumelockr.plus.service.VolumeService
import java.net.NetworkInterface

class PairingActivity : AppCompatActivity() {

    private var isBound = false
    private val connection = object : android.content.ServiceConnection {
        override fun onServiceConnected(name: android.content.ComponentName?, service: android.os.IBinder?) {}
        override fun onServiceDisconnected(name: android.content.ComponentName?) {}
    }

    override fun onStart() {
        super.onStart()
        VolumeService.start(this)
        val intent = android.content.Intent(this, VolumeService::class.java)
        bindService(intent, connection, android.content.Context.BIND_AUTO_CREATE)
        isBound = true
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            runCatching { unbindService(connection) }
            isBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).let { controller ->
            controller.hide(WindowInsetsCompat.Type.statusBars())
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val binding = ActivityPairingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        val uiModeManager = getSystemService(android.content.Context.UI_MODE_SERVICE) as android.app.UiModeManager
        val isTv = uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
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

        if (ipAddress != "Unknown") {
            try {
                val bitMatrix = com.google.zxing.qrcode.QRCodeWriter().encode(ipAddress, com.google.zxing.BarcodeFormat.QR_CODE, 512, 512)
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
