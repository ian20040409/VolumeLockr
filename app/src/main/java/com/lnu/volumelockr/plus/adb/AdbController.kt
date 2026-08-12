package com.lnu.volumelockr.plus.adb

import android.content.Context
import com.tananaev.adblib.AdbConnection
import com.tananaev.adblib.AdbStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Socket

object AdbController {
    
    suspend fun launchTvApp(context: Context, ip: String): String {
        return withContext(Dispatchers.IO) {
            var socket: Socket? = null
            var connection: AdbConnection? = null
            var stream: AdbStream? = null
            try {
                val crypto = AdbKeyManager.getCrypto(context)
                socket = Socket(ip, 5555)
                socket.soTimeout = 10000
                
                connection = AdbConnection.create(socket, crypto)
                connection.connect()
                
                stream = connection.open("shell:am start -n com.lnu.volumelockr.plus/com.lnu.volumelockr.plus.ui.MainActivity")
                
                // Optional: read the response to ensure it executes, but we don't need the exact string
                try {
                    stream.read()
                } catch (e: Exception) {}
                
                "SUCCESS"
            } catch (e: Exception) {
                if (e.message?.contains("auth", ignoreCase = true) == true || e.message?.contains("rejected", ignoreCase = true) == true) {
                    "AUTH_REQUIRED"
                } else {
                    e.message ?: "Failed to connect via ADB"
                }
            } finally {
                try { stream?.close() } catch (e: Exception) {}
                try { connection?.close() } catch (e: Exception) {}
                try { socket?.close() } catch (e: Exception) {}
            }
        }
    }

    suspend fun forceStopLaunchers(context: Context, ip: String): String {
        return withContext(Dispatchers.IO) {
            var socket: Socket? = null
            var connection: AdbConnection? = null
            var stream: AdbStream? = null
            try {
                val crypto = AdbKeyManager.getCrypto(context)
                socket = Socket(ip, 5555)
                socket.soTimeout = 10000
                
                connection = AdbConnection.create(socket, crypto)
                connection.connect()
                
                val commands = listOf(
                    "am force-stop com.google.android.apps.tv.launcherx",
                    "am force-stop com.google.android.tvlauncher",
                    "am force-stop com.google.android.leanbacklauncher"
                )
                
                for (cmd in commands) {
                    try {
                        stream = connection.open("shell:$cmd")
                        stream.read()
                        stream.close()
                    } catch (e: Exception) {}
                }
                
                "SUCCESS"
            } catch (e: Exception) {
                if (e.message?.contains("auth", ignoreCase = true) == true || e.message?.contains("rejected", ignoreCase = true) == true) {
                    "AUTH_REQUIRED"
                } else {
                    e.message ?: "Failed to connect via ADB"
                }
            } finally {
                try { connection?.close() } catch (e: Exception) {}
                try { socket?.close() } catch (e: Exception) {}
            }
        }
    }
}
