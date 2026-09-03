package com.gpsspeed.app

import android.content.Context
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.util.Base64
import java.security.MessageDigest
import java.security.cert.Certificate

object KeyStore {

    private const val PREFS = "amap_key_prefs"
    private const val KEY_AMAP = "amap_key"

    fun getAmapKey(context: Context): String {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_AMAP, "") ?: ""
    }

    fun saveAmapKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_AMAP, key.trim())
            .apply()
    }

    fun hasAmapKey(context: Context): Boolean = getAmapKey(context).isNotEmpty()

    /** 获取当前 APK 签名证书的 SHA1（申请高德 Key 时需填写） */
    fun getSignatureSha1(context: Context): String {
        return try {
            val info = context.packageManager.getPackageInfo(
                context.packageName,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
                    PackageManager.GET_SIGNING_CERTIFICATES
                else
                    @Suppress("DEPRECATION") PackageManager.GET_SIGNATURES
            )
            val sha1 = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners?.firstOrNull()?.toByteArray()
            } else {
                @Suppress("DEPRECATION")
                info.signatures?.firstOrNull()?.toByteArray()
            }
            sha1?.let { sha1Of(it) } ?: "无法获取"
        } catch (e: Exception) {
            "获取失败"
        }
    }

    private fun sha1Of(signature: ByteArray): String {
        return try {
            val md = MessageDigest.getInstance("SHA1")
            val digest = md.digest(signature)
            val sha1 = byteArrayToHex(digest)
            "SHA1:$sha1"
        } catch (e: Exception) {
            "计算失败"
        }
    }

    private fun byteArrayToHex(bytes: ByteArray): String {
        val hex = StringBuilder()
        bytes.forEach {
            val h = Integer.toHexString(it.toInt() and 0xFF)
            if (h.length == 1) hex.append('0')
            hex.append(h)
        }
        return hex.toString().uppercase()
    }
}
