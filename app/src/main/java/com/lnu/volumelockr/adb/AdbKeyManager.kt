package com.lnu.volumelockr.adb

import android.content.Context
import android.util.Base64
import com.tananaev.adblib.AdbBase64
import com.tananaev.adblib.AdbCrypto
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.PublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec

object AdbKeyManager {
    private const val PREFS_NAME = "adb_crypto"
    private const val KEY_PRIVATE = "private_key"
    private const val KEY_PUBLIC = "public_key"

    fun getCrypto(context: Context): AdbCrypto {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val privateBase64 = prefs.getString(KEY_PRIVATE, null)
        val publicBase64 = prefs.getString(KEY_PUBLIC, null)

        val keyPair = if (privateBase64 != null && publicBase64 != null) {
            val privateBytes = Base64.decode(privateBase64, Base64.DEFAULT)
            val publicBytes = Base64.decode(publicBase64, Base64.DEFAULT)

            val keyFactory = KeyFactory.getInstance("RSA")
            val privateKey: PrivateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(privateBytes))
            val publicKey: PublicKey = keyFactory.generatePublic(X509EncodedKeySpec(publicBytes))
            KeyPair(publicKey, privateKey)
        } else {
            val generator = KeyPairGenerator.getInstance("RSA")
            generator.initialize(2048)
            val generatedPair = generator.generateKeyPair()

            prefs.edit()
                .putString(KEY_PRIVATE, Base64.encodeToString(generatedPair.private.encoded, Base64.NO_WRAP))
                .putString(KEY_PUBLIC, Base64.encodeToString(generatedPair.public.encoded, Base64.NO_WRAP))
                .apply()

            generatedPair
        }

        val adbBase64 = object : AdbBase64 {
            override fun encodeToString(data: ByteArray): String {
                return Base64.encodeToString(data, Base64.NO_WRAP)
            }
        }

        return AdbCrypto.loadAdbKeyPair(adbBase64, keyPair)
    }
}
