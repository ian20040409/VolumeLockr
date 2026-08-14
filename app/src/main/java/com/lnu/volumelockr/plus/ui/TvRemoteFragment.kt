package com.lnu.volumelockr.plus.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.lnu.volumelockr.plus.R
import com.lnu.volumelockr.plus.databinding.FragmentTvRemoteBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class TvRemoteFragment : Fragment() {

    private var _binding: FragmentTvRemoteBinding? = null
    private val binding get() = _binding!!
    private val scope = CoroutineScope(Dispatchers.Main)
    private var currentIp: String = ""
    private var syncJob: Job? = null
    private var isTrackingTouch = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val contextThemeWrapper = android.view.ContextThemeWrapper(requireContext(), R.style.ThemeOverlay_App_RoyalPurple)
        val localInflater = inflater.cloneInContext(contextThemeWrapper)
        _binding = FragmentTvRemoteBinding.inflate(localInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val prefs = requireContext().getSharedPreferences("tv_remote", Context.MODE_PRIVATE)
        val recentIpsString = prefs.getString("recent_ips", "") ?: ""
        val recentIps = recentIpsString.split(",").filter { it.isNotEmpty() }.toMutableList()
        
        setupIpAdapter(recentIps)
        
        if (recentIps.isNotEmpty()) {
            binding.ipAddressInput.setText(recentIps[0], false)
        }

        binding.scanQrButton.setOnClickListener {
            val googleApiAvailability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
            val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(requireContext())
            if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
                if (googleApiAvailability.isUserResolvableError(resultCode)) {
                    googleApiAvailability.getErrorDialog(requireActivity(), resultCode, 9000)?.show()
                } else {
                    Toast.makeText(context, R.string.toast_play_services_missing, Toast.LENGTH_SHORT).show()
                }
                return@setOnClickListener
            }

            val options = com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
                .build()
            val scanner = com.google.mlkit.vision.codescanner.GmsBarcodeScanning.getClient(requireContext(), options)
            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    barcode.rawValue?.let { scannedRaw ->
                        val ipRegex = Regex("""\b\d{1,3}\.\d{1,3}\.\d{1,3}\.\d{1,3}\b""")
                        val cleanIp = ipRegex.find(scannedRaw)?.value ?: scannedRaw.trim()
                        val tokenRegex = Regex("""token=([a-zA-Z0-9_-]+)""")
                        val scannedToken = tokenRegex.find(scannedRaw)?.groupValues?.get(1)
                        if (!scannedToken.isNullOrEmpty()) {
                            saveTokenForIp(cleanIp, scannedToken)
                        }
                        binding.ipAddressInput.setText(cleanIp, false)
                        currentIp = cleanIp
                        testConnection()
                    }
                }
                .addOnFailureListener {
                    // Ignore or show error
                }
        }

        binding.connectButton.setOnClickListener {
            val ip = binding.ipAddressInput.text.toString().trim()
            if (ip.isNotEmpty()) {
                currentIp = ip
                if (getTokenForIp(currentIp).isEmpty()) {
                    showPinInputDialog { pin ->
                        saveTokenForIp(currentIp, pin)
                        testConnection()
                    }
                } else {
                    testConnection()
                }
            } else {
                Toast.makeText(context, R.string.toast_enter_ip, Toast.LENGTH_SHORT).show()
            }
        }

        binding.hideIconSwitch.setOnCheckedChangeListener { _, isChecked ->
            sendConfigCommand("hide_icon", isChecked, true)
        }

        binding.hideUnlockSwitch.setOnCheckedChangeListener { _, isChecked ->
            sendConfigCommand("hide_unlock", isChecked, false)
        }

        binding.volumeSlider.addOnChangeListener { slider, value, fromUser ->
            binding.volumeValueText.text = "${value.toInt()} / ${binding.volumeSlider.valueTo.toInt()}"
            if (fromUser) {
                sendVolumeCommand(value.toInt())
            }
        }
        
        binding.volumeValueText.setOnClickListener {
            val min = binding.volumeSlider.valueFrom.toInt()
            val max = binding.volumeSlider.valueTo.toInt()
            val current = binding.volumeSlider.value.toInt()
            showDirectVolumeInputDialog(min, max, current) { clamped ->
                binding.volumeSlider.value = clamped.toFloat()
                sendVolumeCommand(clamped)
            }
        }
        
        binding.volumeSlider.addOnSliderTouchListener(object : com.google.android.material.slider.Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: com.google.android.material.slider.Slider) {
                slider.parent?.requestDisallowInterceptTouchEvent(true)
                isTrackingTouch = true
            }
            override fun onStopTrackingTouch(slider: com.google.android.material.slider.Slider) {
                isTrackingTouch = false
            }
        })

        binding.lockButton.setOnClickListener {
            val volume = binding.volumeSlider.value.toInt()
            sendLockCommand(volume, true)
        }

        binding.unlockButton.setOnClickListener {
            sendLockCommand(0, false)
        }
        
        binding.launchAppButton.setOnClickListener {
            sendLaunchCommand()
        }
    }

    private fun sendLaunchCommand() {
        val targetIp = currentIp.ifEmpty { binding.ipAddressInput.text.toString().trim() }
        if (targetIp.isEmpty()) {
            Toast.makeText(context, R.string.toast_enter_ip, Toast.LENGTH_SHORT).show()
            return
        }
        
        Toast.makeText(context, getString(R.string.toast_connecting_adb, targetIp), Toast.LENGTH_SHORT).show()
        
        scope.launch {
            val result = com.lnu.volumelockr.plus.adb.AdbController.launchTvApp(requireContext(), targetIp)
            
            withContext(Dispatchers.Main) {
                if (result == "SUCCESS") {
                    Toast.makeText(context, R.string.toast_app_launched, Toast.LENGTH_SHORT).show()
                } else if (result == "AUTH_REQUIRED") {
                    Toast.makeText(context, R.string.toast_auth_required, Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, getString(R.string.toast_adb_error, result), Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun getTokenForIp(ip: String): String {
        val prefs = context?.getSharedPreferences("tv_remote_tokens", Context.MODE_PRIVATE)
        return prefs?.getString("token_$ip", "") ?: ""
    }

    private fun saveTokenForIp(ip: String, token: String) {
        val prefs = context?.getSharedPreferences("tv_remote_tokens", Context.MODE_PRIVATE)
        prefs?.edit()?.putString("token_$ip", token)?.apply()
    }

    private fun sendConfigCommand(endpoint: String, hide: Boolean, useAdbRestart: Boolean) {
        if (currentIp.isEmpty()) return
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val token = getTokenForIp(currentIp)
                    val url = URL("http://$currentIp:8080/$endpoint?hide=$hide&token=$token")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.requestMethod = "GET"
                    val code = connection.responseCode
                    connection.disconnect()
                    
                    if (code == 200 && endpoint == "hide_icon" && useAdbRestart) {
                        // Use ADB to restart the launcher so the ghost icon is removed immediately
                        com.lnu.volumelockr.plus.adb.AdbController.forceStopLaunchers(requireContext(), currentIp)
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    private var volumeJob: Job? = null

    private fun sendVolumeCommand(volume: Int) {
        if (currentIp.isEmpty()) return
        volumeJob?.cancel()
        volumeJob = scope.launch {
            delay(100) // Debounce for smooth dragging
            withContext(Dispatchers.IO) {
                try {
                    val token = getTokenForIp(currentIp)
                    val url = URL("http://$currentIp:8080/set_volume?stream=3&volume=$volume&token=$token")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.requestMethod = "GET"
                    connection.responseCode
                    connection.disconnect()
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    private fun testConnection() {
        binding.connectButton.isEnabled = false
        scope.launch {
            val pingResult = withContext(Dispatchers.IO) {
                try {
                    val token = getTokenForIp(currentIp)
                    val url = URL("http://$currentIp:8080/ping?token=$token")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.requestMethod = "GET"
                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                        connection.disconnect()
                        responseBody
                    } else if (responseCode == 401) {
                        connection.disconnect()
                        "UNAUTHORIZED"
                    } else {
                        connection.disconnect()
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
            binding.connectButton.isEnabled = true
            if (pingResult == "UNAUTHORIZED") {
                Toast.makeText(context, R.string.toast_auth_required, Toast.LENGTH_SHORT).show()
                binding.controlCard.visibility = View.GONE
                syncJob?.cancel()
                showPinInputDialog { pin ->
                    saveTokenForIp(currentIp, pin)
                    testConnection()
                }
            } else if (pingResult != null && pingResult.startsWith("OK")) {
                Toast.makeText(context, R.string.toast_connected, Toast.LENGTH_SHORT).show()
                binding.controlCard.visibility = View.VISIBLE
                
                val parts = pingResult.split(",")
                if (parts.size >= 3) {
                    val tokenOrMax = parts[1]
                    if (tokenOrMax.length >= 16) {
                        saveTokenForIp(currentIp, tokenOrMax)
                    }
                    val maxVol = (if (tokenOrMax.length >= 16) parts.getOrNull(2) else parts.getOrNull(1))?.toFloatOrNull() ?: 15f
                    val currVol = (if (tokenOrMax.length >= 16) parts.getOrNull(3) else parts.getOrNull(2))?.toFloatOrNull() ?: 0f
                    binding.volumeSlider.valueTo = maxVol
                    binding.volumeSlider.value = currVol.coerceIn(0f, maxVol)
                    binding.volumeValueText.text = "${binding.volumeSlider.value.toInt()} / ${maxVol.toInt()}"
                }
                
                val hideIconIdx = if (parts.getOrNull(1)?.length ?: 0 >= 16) 4 else 3
                val hideUnlockIdx = if (parts.getOrNull(1)?.length ?: 0 >= 16) 5 else 4
                if (parts.size > hideUnlockIdx) {
                    binding.hideIconSwitch.setOnCheckedChangeListener(null)
                    binding.hideUnlockSwitch.setOnCheckedChangeListener(null)
                    
                    binding.hideIconSwitch.isChecked = parts[hideIconIdx].toBoolean()
                    binding.hideUnlockSwitch.isChecked = parts[hideUnlockIdx].toBoolean()
                    
                    binding.hideIconSwitch.setOnCheckedChangeListener { _, isChecked ->
                        sendConfigCommand("hide_icon", isChecked, true)
                    }
                    binding.hideUnlockSwitch.setOnCheckedChangeListener { _, isChecked ->
                        sendConfigCommand("hide_unlock", isChecked, false)
                    }
                }
                
                startVolumeSync()
                
                // Save to recent IPs
                val prefs = requireContext().getSharedPreferences("tv_remote", Context.MODE_PRIVATE)
                val recentIpsString = prefs.getString("recent_ips", "") ?: ""
                val recentIps = recentIpsString.split(",").filter { it.isNotEmpty() }.toMutableList()
                recentIps.remove(currentIp)
                recentIps.add(0, currentIp)
                if (recentIps.size > 5) {
                    recentIps.removeAt(recentIps.size - 1)
                }
                prefs.edit().putString("recent_ips", recentIps.joinToString(",")).apply()
                setupIpAdapter(recentIps)
            } else {
                Toast.makeText(context, R.string.toast_connection_failed, Toast.LENGTH_SHORT).show()
                binding.controlCard.visibility = View.GONE
                syncJob?.cancel()
            }
        }
    }

    private fun setupIpAdapter(ips: List<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, ips)
        binding.ipAddressInput.setAdapter(adapter)
    }

    private fun startVolumeSync() {
        syncJob?.cancel()
        syncJob = scope.launch {
            while (isActive) {
                delay(1000)
                if (currentIp.isNotEmpty() && binding.controlCard.visibility == View.VISIBLE) {
                    val pingResult = withContext(Dispatchers.IO) {
                        try {
                            val token = getTokenForIp(currentIp)
                            val url = URL("http://$currentIp:8080/ping?token=$token")
                            val connection = url.openConnection() as HttpURLConnection
                            connection.connectTimeout = 3000
                            connection.readTimeout = 3000
                            connection.requestMethod = "GET"
                            if (connection.responseCode == 200) {
                                connection.inputStream.bufferedReader().use { it.readText() }
                            } else null
                        } catch (e: Exception) {
                            null
                        }
                    }
                    if (pingResult != null && pingResult.startsWith("OK")) {
                        val parts = pingResult.split(",")
                        if (parts.size >= 3) {
                            val tokenOrMax = parts[1]
                            val maxVol = (if (tokenOrMax.length >= 16) parts.getOrNull(2) else parts.getOrNull(1))?.toFloatOrNull() ?: 15f
                            val currVol = (if (tokenOrMax.length >= 16) parts.getOrNull(3) else parts.getOrNull(2))?.toFloatOrNull() ?: 0f
                            if (!isTrackingTouch) {
                                binding.volumeSlider.valueTo = maxVol
                                binding.volumeSlider.value = currVol.coerceIn(0f, maxVol)
                                binding.volumeValueText.text = "${binding.volumeSlider.value.toInt()} / ${maxVol.toInt()}"
                            }
                        }
                    }
                }
            }
        }
    }

    private fun showDirectVolumeInputDialog(min: Int, max: Int, current: Int, onConfirm: (Int) -> Unit) {
        val context = requireContext()
        val input = com.google.android.material.textfield.TextInputEditText(context).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(current.toString())
            setSelection(text?.length ?: 0)
        }
        val container = android.widget.FrameLayout(context).apply {
            val padding = (24 * context.resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, 0)
            addView(input)
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.set_custom_volume))
            .setMessage(context.getString(R.string.enter_volume_value, min, max))
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val entered = input.text.toString().toIntOrNull()
                if (entered != null) {
                    val clamped = entered.coerceIn(min, max)
                    onConfirm(clamped)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun showPinInputDialog(onConfirm: (String) -> Unit) {
        val context = context ?: return
        val input = com.google.android.material.textfield.TextInputEditText(context).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "123456"
        }
        val container = android.widget.FrameLayout(context).apply {
            val padding = (24 * context.resources.displayMetrics.density).toInt()
            setPadding(padding, padding / 2, padding, 0)
            addView(input)
        }

        com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
            .setTitle(context.getString(R.string.enter_pairing_pin_title))
            .setMessage(context.getString(R.string.enter_pairing_pin_message))
            .setView(container)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val pin = input.text.toString().trim()
                if (pin.isNotEmpty()) {
                    onConfirm(pin)
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun sendLockCommand(volume: Int, locked: Boolean) {
        scope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    val token = getTokenForIp(currentIp)
                    // stream=3 is AudioManager.STREAM_MUSIC
                    val urlString = "http://$currentIp:8080/set_lock?stream=3&volume=$volume&locked=$locked&token=$token"
                    val url = URL(urlString)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.requestMethod = "GET"
                    val responseCode = connection.responseCode
                    connection.disconnect()
                    responseCode == 200
                } catch (e: Exception) {
                    false
                }
            }
            if (success) {
                Toast.makeText(context, R.string.toast_command_sent, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, R.string.toast_command_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        syncJob?.cancel()
        _binding = null
    }
}
