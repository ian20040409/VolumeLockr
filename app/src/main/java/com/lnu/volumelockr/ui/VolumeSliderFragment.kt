package com.lnu.volumelockr.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.lnu.volumelockr.R
import com.lnu.volumelockr.databinding.FragmentVolumeSliderBinding
import com.lnu.volumelockr.service.VolumeService

class VolumeSliderFragment : Fragment() {

    private var _binding: FragmentVolumeSliderBinding? = null
    private val binding get() = _binding!!
    private var mAdapter: VolumeAdapter? = null
    private var mService: VolumeService? = null
    private var isServiceBound = false

    private val uiUpdateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.lnu.volumelockr.ACTION_UI_UPDATE") {
                updateQuickActionState()
                mAdapter?.notifyDataSetChanged()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVolumeSliderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        androidx.core.content.ContextCompat.registerReceiver(
            requireContext(),
            uiUpdateReceiver,
            android.content.IntentFilter("com.lnu.volumelockr.ACTION_UI_UPDATE"),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
        
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        binding.allowLowerSwitch.setOnCheckedChangeListener(null)
        binding.allowLowerSwitch.isChecked = prefs.getBoolean(SettingsFragment.ALLOW_LOWER_PREFERENCE, true)
        binding.allowLowerSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(SettingsFragment.ALLOW_LOWER_PREFERENCE, isChecked).apply()
            VolumeService.start(requireContext())
        }
        
        mService?.let {
            handleServiceConnected()
        } ?: Intent(context, VolumeService::class.java).also { intent ->
            context?.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onPause() {
        requireContext().unregisterReceiver(uiUpdateReceiver)
        unbindServiceIfNeeded()
        clearSubtitle()
        super.onPause()
    }

    override fun onDestroyView() {
        unbindServiceIfNeeded()
        _binding?.recyclerView?.adapter = null
        _binding = null
        super.onDestroyView()
    }

    private fun setupRecyclerView(service: VolumeService) {
        val spanCount = if (resources.getBoolean(R.bool.use_two_columns)) 2 else 1
        binding.recyclerView.layoutManager = androidx.recyclerview.widget.GridLayoutManager(requireContext(), spanCount)
        mAdapter = VolumeAdapter(service.getVolumes(), service, requireContext()).also { adapter ->
            adapter.onLockStateChanged = { updateSubtitle() }
        }
        binding.recyclerView.adapter = mAdapter
    }

    private fun setupQuickActions() {
        binding.lockAllChip.setOnClickListener { lockAll() }
        binding.unlockAllChip.setOnClickListener { unlockAll() }
        updateQuickActionState()

        val focusChangeListener = View.OnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.animate().scaleX(1.05f).scaleY(1.05f).translationZ(8f).setDuration(150).start()
            } else {
                view.animate().scaleX(1f).scaleY(1f).translationZ(0f).setDuration(150).start()
            }
        }
        binding.lockAllChip.onFocusChangeListener = focusChangeListener
        binding.unlockAllChip.onFocusChangeListener = focusChangeListener
        binding.tvPairButton.onFocusChangeListener = focusChangeListener

        val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
        if (uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION) {
            binding.tvPairButton.visibility = View.VISIBLE
            binding.tvPairButton.setOnClickListener { showPairingDialog() }
            binding.systemSoundSettingsButton.visibility = View.GONE
            binding.aboutButton.visibility = View.VISIBLE
        } else {
            binding.systemSoundSettingsButton.setOnClickListener {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_SOUND_SETTINGS)
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            binding.aboutButton.visibility = View.GONE
        }
        
        binding.aboutButton.setOnClickListener {
            startActivity(Intent(requireContext(), AboutActivity::class.java))
        }
    }

    private fun showPairingDialog() {
        val ipAddress = getLocalIpAddress() ?: "Unknown"
        val dialogView = layoutInflater.inflate(R.layout.dialog_pairing, null)
        dialogView.findViewById<android.widget.TextView>(R.id.ip_address_text)?.text = ipAddress

        val dialog = com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setIcon(R.drawable.host_48px)
            .setTitle(R.string.pair_with_phone_title)
            .setView(dialogView)
            .setPositiveButton(android.R.string.ok, null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)?.requestFocus()
        }

        dialog.show()
    }

    private fun getLocalIpAddress(): String? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val intf = interfaces.nextElement()
                val addrs = intf.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return addr.hostAddress
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun lockAll() {
        val service = mService ?: return
        service.getVolumes().forEach { volume ->
            service.addLock(volume.stream, volume.value)
        }
        VolumeService.start(requireContext())
        service.startLocking()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            service.tryShowNotification()
        }
        mAdapter?.update(service.getVolumes())
        updateSubtitle()
    }

    private fun unlockAll() {
        val service = mService ?: return
        service.getVolumes().forEach { volume ->
            service.removeLock(volume.stream)
        }
        service.stopLocking()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            service.tryHideNotification()
        }
        mAdapter?.update(service.getVolumes())
        updateSubtitle()
    }

    private fun updateQuickActionState() {
        val isProtected = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getBoolean(SettingsFragment.PASSWORD_PROTECTED_PREFERENCE, false)
        binding.lockAllChip.isEnabled = !isProtected
        binding.unlockAllChip.isEnabled = !isProtected
        
        val uiModeManager = requireContext().getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
        val isTv = uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_TELEVISION
        val hideUnlock = PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("hide_tv_unlock_ui", true)
        
        if (isTv && hideUnlock) {
            binding.topActionContainer.visibility = View.GONE
        } else {
            binding.topActionContainer.visibility = View.VISIBLE
        }
    }

    private fun updateSubtitle() {
        if (!isAdded) {
            return
        }
        val service = mService ?: return
        val lockedCount = service.getLocks().size
        val totalCount = service.getVolumes().size
        val subtitle = resources.getString(R.string.locked_subtitle, lockedCount, totalCount)
        (requireActivity() as AppCompatActivity).supportActionBar?.subtitle = subtitle
    }

    private fun clearSubtitle() {
        (activity as? AppCompatActivity)?.supportActionBar?.subtitle = null
    }

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName?, service: IBinder?) {
            val binder = service as VolumeService.LocalBinder
            mService = binder.getService()
            isServiceBound = true
            handleServiceConnected()
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            isServiceBound = false
            mService = null
            mAdapter = null
        }
    }

    private fun handleServiceConnected() {
        mService?.let {
            setupRecyclerView(it)
            setupQuickActions()
            updateSubtitle()

            mService?.registerOnVolumeChangeListener(Handler(Looper.getMainLooper())) {
                mAdapter?.update(it.getVolumes())
                updateSubtitle()
            }
        }
    }

    private fun unbindServiceIfNeeded() {
        mService?.unregisterOnModeChangeListener()

        if (isServiceBound) {
            context?.let {
                runCatching { it.unbindService(connection) }
            }
            isServiceBound = false
        }

        mService = null
        mAdapter = null
    }
}
