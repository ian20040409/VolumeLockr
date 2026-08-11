package com.lnu.volumelockr.service

import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import kotlinx.coroutines.CoroutineScope

class TvRemoteServer(private val context: Context, private val serviceScope: CoroutineScope) {
    private var serverSocket: ServerSocket? = null
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = serviceScope.launch(Dispatchers.IO) {
            try {
                serverSocket = ServerSocket(8080)
                Log.d("TvRemoteServer", "Server started on port 8080")
                while (isActive) {
                    val client = serverSocket?.accept()
                    client?.let {
                        launch(Dispatchers.IO) { handleClient(it) }
                    }
                }
            } catch (e: Exception) {
                Log.e("TvRemoteServer", "Server error", e)
            }
        }
    }

    fun stop() {
        job?.cancel()
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun handleClient(client: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(client.getInputStream()))
            val output = client.getOutputStream()

            val requestLine = reader.readLine() ?: return
            Log.d("TvRemoteServer", "Request: $requestLine")

            if (requestLine.startsWith("GET /ping")) {
                sendResponse(output, 200, "OK")
            } else if (requestLine.startsWith("GET /set_lock")) {
                val parts = requestLine.split(" ")
                if (parts.size >= 2) {
                    val pathAndQuery = parts[1]
                    val query = pathAndQuery.substringAfter("?", "")
                    val params = parseQuery(query)

                    val stream = params["stream"]?.toIntOrNull()
                    val volume = params["volume"]?.toIntOrNull()
                    val locked = params["locked"]?.toBoolean()

                    if (stream != null && locked != null) {
                        val intent = Intent(context, VolumeService::class.java).apply {
                            action = "com.lnu.volumelockr.ACTION_SET_LOCK"
                            putExtra("stream", stream)
                            putExtra("locked", locked)
                            if (volume != null) {
                                putExtra("volume", volume)
                            }
                        }
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            context.startForegroundService(intent)
                        } else {
                            context.startService(intent)
                        }
                        sendResponse(output, 200, "OK")
                    } else {
                        sendResponse(output, 400, "Bad Request")
                    }
                }
            } else if (requestLine.startsWith("GET /hide_icon")) {
                val parts = requestLine.split(" ")
                val query = parts[1].substringAfter("?", "")
                val params = parseQuery(query)
                val hide = params["hide"]?.toBoolean() ?: true
                
                val state = if (hide) {
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                } else {
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                }
                
                context.packageManager.setComponentEnabledSetting(
                    android.content.ComponentName(context, "com.lnu.volumelockr.TvLauncherAlias"),
                    state,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
                
                // Attempt to kill launcher background processes to force a cache refresh
                try {
                    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                    am.killBackgroundProcesses("com.google.android.apps.tv.launcherx")
                    am.killBackgroundProcesses("com.google.android.tvlauncher")
                    am.killBackgroundProcesses("com.google.android.leanbacklauncher")
                } catch (e: Exception) {
                    Log.e("TvRemoteServer", "Failed to kill launcher processes", e)
                }
                
                sendResponse(output, 200, "OK")
            } else if (requestLine.startsWith("GET /hide_unlock")) {
                val parts = requestLine.split(" ")
                val query = parts[1].substringAfter("?", "")
                val params = parseQuery(query)
                val hide = params["hide"]?.toBoolean() ?: true
                
                val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
                prefs.edit().putBoolean("hide_tv_unlock_ui", hide).apply()
                
                // Broadcast to update UI
                context.sendBroadcast(Intent("com.lnu.volumelockr.ACTION_UI_UPDATE"))
                sendResponse(output, 200, "OK")
            } else if (requestLine.startsWith("GET /launch_app")) {
                val launchIntent = Intent(context, com.lnu.volumelockr.ui.MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                
                // On Android 10+, background activity launches are blocked. 
                // We use a high-priority notification with a full-screen intent to bypass this on Android TV.
                val pendingIntent = android.app.PendingIntent.getActivity(
                    context, 0, launchIntent, 
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )
                
                val channelId = "launch_channel"
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    val channel = android.app.NotificationChannel(channelId, "App Launch", android.app.NotificationManager.IMPORTANCE_HIGH)
                    notificationManager.createNotificationChannel(channel)
                }
                
                val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("VolumeLockr")
                    .setContentText("Launching...")
                    .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                    .setCategory(androidx.core.app.NotificationCompat.CATEGORY_ALARM)
                    .setFullScreenIntent(pendingIntent, true)
                    .build()
                
                notificationManager.notify(999, notification)
                
                // Try standard start just in case it's allowed
                try {
                    context.startActivity(launchIntent)
                } catch (e: Exception) {}

                // Clean up notification shortly after
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    notificationManager.cancel(999)
                }, 2000)

                sendResponse(output, 200, "OK")
            } else {
                sendResponse(output, 404, "Not Found")
            }
        } catch (e: Exception) {
            Log.e("TvRemoteServer", "Client error", e)
        } finally {
            try {
                client.close()
            } catch (e: Exception) {}
        }
    }

    private fun parseQuery(query: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        if (query.isEmpty()) return map
        query.split("&").forEach { param ->
            val kv = param.split("=")
            if (kv.size == 2) {
                map[kv[0]] = kv[1]
            }
        }
        return map
    }

    private fun sendResponse(output: OutputStream, statusCode: Int, message: String) {
        val response = "HTTP/1.1 $statusCode $message\r\n" +
                "Content-Type: text/plain\r\n" +
                "Connection: close\r\n\r\n" +
                message
        output.write(response.toByteArray())
        output.flush()
    }
}
