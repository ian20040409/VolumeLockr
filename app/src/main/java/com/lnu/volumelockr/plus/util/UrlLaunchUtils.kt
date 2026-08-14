package com.lnu.volumelockr.plus.util

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.browser.customtabs.CustomTabsIntent
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object UrlLaunchUtils {

    private const val QR_CODE_SIZE = 512
    private const val QR_DIALOG_PADDING = 32

    fun openUrl(activity: Activity, url: String) {
        val uiModeManager = activity.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

        if (isTv) {
            showQrCodeDialog(activity, url)
            return
        }

        runCatching {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(activity, Uri.parse(url))
        }.onFailure {
            runCatching {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }.onFailure {
                showQrCodeDialog(activity, url)
            }
        }
    }

    fun showQrCodeDialog(activity: Activity, url: String) {
        runCatching {
            val bits = QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, QR_CODE_SIZE, QR_CODE_SIZE)
            val bmp = Bitmap.createBitmap(QR_CODE_SIZE, QR_CODE_SIZE, Bitmap.Config.RGB_565)
            for (x in 0 until QR_CODE_SIZE) {
                for (y in 0 until QR_CODE_SIZE) {
                    bmp.setPixel(x, y, if (bits[x, y]) Color.BLACK else Color.WHITE)
                }
            }

            val imageView = ImageView(activity).apply {
                setImageBitmap(bmp)
                setPadding(QR_DIALOG_PADDING, QR_DIALOG_PADDING, QR_DIALOG_PADDING, QR_DIALOG_PADDING)
            }

            val dialog = MaterialAlertDialogBuilder(activity)
                .setTitle(url)
                .setView(imageView)
                .setPositiveButton(android.R.string.ok, null)
                .create()

            dialog.setOnShowListener {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.requestFocus()
            }

            dialog.show()
        }
    }

    fun findRecyclerView(view: View): RecyclerView? {
        if (view is RecyclerView) return view
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                val rv = findRecyclerView(child)
                if (rv != null) return rv
            }
        }
        return null
    }
}
