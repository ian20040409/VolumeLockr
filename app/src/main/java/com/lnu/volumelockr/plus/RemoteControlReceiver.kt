package com.lnu.volumelockr.plus

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.lnu.volumelockr.plus.service.VolumeService

class RemoteControlReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.lnu.volumelockr.plus.ACTION_SET_LOCK") {
            val serviceIntent = Intent(context, VolumeService::class.java).apply {
                action = intent.action
                putExtras(intent)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
