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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class TvRemoteFragment : Fragment() {

    private var _binding: FragmentTvRemoteBinding? = null
    private val binding get() = _binding!!
    private val scope = CoroutineScope(Dispatchers.Main)
    private var currentIp: String = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTvRemoteBinding.inflate(inflater, container, false)
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

        binding.connectButton.setOnClickListener {
            val ip = binding.ipAddressInput.text.toString().trim()
            if (ip.isNotEmpty()) {
                currentIp = ip
                testConnection()
            } else {
                Toast.makeText(context, R.string.toast_enter_ip, Toast.LENGTH_SHORT).show()
            }
        }

        binding.hideIconSwitch.setOnCheckedChangeListener { _, isChecked ->
            sendConfigCommand("hide_icon", isChecked)
        }

        binding.hideUnlockSwitch.setOnCheckedChangeListener { _, isChecked ->
            sendConfigCommand("hide_unlock", isChecked)
        }

        binding.volumeSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                sendVolumeCommand(value.toInt())
            }
        }

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

    private fun sendConfigCommand(endpoint: String, hide: Boolean) {
        if (currentIp.isEmpty()) return
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val url = URL("http://$currentIp:8080/$endpoint?hide=$hide")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.requestMethod = "GET"
                    val code = connection.responseCode
                    connection.disconnect()
                    
                    if (code == 200 && endpoint == "hide_icon") {
                        // Use ADB to restart the launcher so the ghost icon is removed immediately
                        com.lnu.volumelockr.plus.adb.AdbController.forceStopLaunchers(requireContext(), currentIp)
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }
    }

    private fun sendVolumeCommand(volume: Int) {
        if (currentIp.isEmpty()) return
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val url = URL("http://$currentIp:8080/set_volume?stream=3&volume=$volume")
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
                    val url = URL("http://$currentIp:8080/ping")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 3000
                    connection.readTimeout = 3000
                    connection.requestMethod = "GET"
                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
                        connection.disconnect()
                        responseBody
                    } else {
                        connection.disconnect()
                        null
                    }
                } catch (e: Exception) {
                    null
                }
            }
            binding.connectButton.isEnabled = true
            if (pingResult != null && pingResult.startsWith("OK")) {
                Toast.makeText(context, R.string.toast_connected, Toast.LENGTH_SHORT).show()
                binding.controlCard.visibility = View.VISIBLE
                
                val parts = pingResult.split(",")
                if (parts.size >= 3) {
                    val maxVol = parts[1].toFloatOrNull() ?: 15f
                    val currVol = parts[2].toFloatOrNull() ?: 0f
                    binding.volumeSlider.valueTo = maxVol
                    binding.volumeSlider.value = currVol.coerceIn(0f, maxVol)
                }
                
                sendConfigCommand("hide_icon", binding.hideIconSwitch.isChecked)
                sendConfigCommand("hide_unlock", binding.hideUnlockSwitch.isChecked)
                
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
            }
        }
    }

    private fun setupIpAdapter(ips: List<String>) {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, ips)
        binding.ipAddressInput.setAdapter(adapter)
    }

    private fun sendLockCommand(volume: Int, locked: Boolean) {
        scope.launch {
            val success = withContext(Dispatchers.IO) {
                try {
                    // stream=3 is AudioManager.STREAM_MUSIC
                    val urlString = "http://$currentIp:8080/set_lock?stream=3&volume=$volume&locked=$locked"
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
        _binding = null
    }
}
