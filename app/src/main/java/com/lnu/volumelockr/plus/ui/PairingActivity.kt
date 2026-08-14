package com.lnu.volumelockr.plus.ui

import android.app.UiModeManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.lnu.volumelockr.plus.R
import com.lnu.volumelockr.plus.databinding.ActivityPairingBinding
import com.lnu.volumelockr.plus.service.TvRemoteServer
import com.lnu.volumelockr.plus.service.VolumeService
import com.lnu.volumelockr.plus.util.AppConstants
import java.net.Inet4Address
import java.net.NetworkInterface

class PairingActivity : BaseSecuredActivity() {

    private lateinit var binding: ActivityPairingBinding
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {}
        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    private val pinHandler = Handler(Looper.getMainLooper())
    private val pinRunnable = object : Runnable {
        override fun run() {
            val (pin, remainingSec) = TvRemoteServer.getOrGeneratePairingPin()
            binding.pinCodeText.text = getString(R.string.pairing_pin_label, pin, remainingSec)
            pinHandler.postDelayed(this, PIN_REFRESH_INTERVAL_MS)
        }
    }

    private val pairedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AppConstants.ACTION_PAIRED_SUCCESS) {
                Toast.makeText(this@PairingActivity, R.string.toast_connected, Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    override fun getSecuredContentView(): View = binding.nestedScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityPairingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupStandardInsets(binding.appBarLayout, binding.nestedScrollView)

        setSupportActionBar(binding.toolbar)
        val uiModeManager = getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
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
                val bitMatrix = QRCodeWriter().encode(qrData, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE)
                val width = bitMatrix.width
                val height = bitMatrix.height
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                    }
                }
                binding.qrCodeImage.setImageBitmap(bitmap)
                binding.qrCodeImage.visibility = View.VISIBLE
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        VolumeService.start(this)
        val intent = Intent(this, VolumeService::class.java)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
        isBound = true
        pinHandler.post(pinRunnable)
        ContextCompat.registerReceiver(
            this,
            pairedReceiver,
            IntentFilter(AppConstants.ACTION_PAIRED_SUCCESS),
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

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList() ?: return null
            val preferredInterfaces = interfaces.filter { intf ->
                intf.isUp && !intf.isLoopback && (intf.name.startsWith("wlan") || intf.name.startsWith("eth")) &&
                    !intf.name.contains("p2p")
            }
            val candidateInterfaces = preferredInterfaces.ifEmpty {
                interfaces.filter { intf ->
                    intf.isUp && !intf.isLoopback && !intf.name.contains("p2p") &&
                        !intf.name.contains("dummy") && !intf.name.contains("tun")
                }
            }.ifEmpty { interfaces }

            for (networkInterface in candidateInterfaces) {
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is Inet4Address) {
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

    companion object {
        private const val QR_CODE_SIZE = 512
        private const val PIN_REFRESH_INTERVAL_MS = 1000L
    }
}
