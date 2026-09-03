package com.gpsspeed.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity(), LocationListener {

    private lateinit var tvSpeed: TextView
    private lateinit var tvUnit: TextView
    private lateinit var tvAccuracy: TextView
    private lateinit var tvSource: TextView
    private var useMph = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvSpeed = findViewById(R.id.tv_speed)
        tvUnit = findViewById(R.id.tv_unit)
        tvAccuracy = findViewById(R.id.tv_accuracy)
        tvSource = findViewById(R.id.tv_source)

        findViewById<android.view.View>(R.id.btn_toggle_unit).setOnClickListener {
            useMph = !useMph
            tvUnit.text = if (useMph) "MPH" else "km/h"
        }

        findViewById<android.view.View>(R.id.btn_navi).setOnClickListener {
            val hasKey = KeyStore.hasAmapKey(this)
            if (!hasKey) {
                Toast.makeText(this, R.string.key_hint_first, Toast.LENGTH_SHORT).show()
            }
            LogStore.log("Main", "进入红绿灯页面, hasKey=$hasKey")
            startActivity(Intent(this, NaviActivity::class.java))
        }

        findViewById<android.view.View>(R.id.btn_key_settings).setOnClickListener {
            showKeyDialog()
        }

        findViewById<android.view.View>(R.id.btn_log).setOnClickListener {
            startActivity(Intent(this, LogActivity::class.java))
        }

        requestLocation()
    }

    private fun showKeyDialog() {
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        container.setPadding(48, 24, 48, 8)

        // 显示当前 APK 签名 SHA1（申请高德 Key 需要）
        val tvSha1 = TextView(this)
        tvSha1.textSize = 13f
        tvSha1.setTextColor(0xFF555555.toInt())
        tvSha1.text = getString(R.string.key_sha1_hint) + "\n" + KeyStore.getSignatureSha1(this)
        container.addView(tvSha1)

        // 当前 Key 状态
        val tvStatus = TextView(this)
        tvStatus.textSize = 13f
        tvStatus.text = if (KeyStore.hasAmapKey(this)) {
            getString(R.string.key_configured) + KeyStore.getAmapKey(this)
        } else {
            getString(R.string.key_not_configured)
        }
        tvStatus.setTextColor(if (KeyStore.hasAmapKey(this)) 0xFF2E7D32.toInt() else 0xFFC62828.toInt())
        container.addView(tvStatus)

        val etKey = EditText(this)
        etKey.hint = getString(R.string.key_edit_hint)
        etKey.setSingleLine(true)
        etKey.setText(KeyStore.getAmapKey(this))
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.topMargin = 20
        etKey.layoutParams = lp
        container.addView(etKey)

        // 检测结果展示
        val tvDetect = TextView(this)
        tvDetect.textSize = 14f
        val dlp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        dlp.topMargin = 14
        tvDetect.layoutParams = dlp
        container.addView(tvDetect)

        // 检测按钮（内容内按钮，避免被键盘/多按钮问题挡住）
        val btnDetect = TextView(this)
        btnDetect.text = getString(R.string.key_detect)
        btnDetect.textSize = 15f
        btnDetect.gravity = android.view.Gravity.CENTER
        btnDetect.setTextColor(android.graphics.Color.WHITE)
        btnDetect.setBackgroundColor(0xFF0288D1.toInt())
        btnDetect.isClickable = true
        val dlp2 = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        dlp2.topMargin = 14
        btnDetect.layoutParams = dlp2
        btnDetect.setPadding(0, 14, 0, 14)
        container.addView(btnDetect)

        // 保存按钮（内容内按钮）
        val btnSave = TextView(this)
        btnSave.text = getString(R.string.key_save)
        btnSave.textSize = 15f
        btnSave.gravity = android.view.Gravity.CENTER
        btnSave.setTextColor(android.graphics.Color.WHITE)
        btnSave.setBackgroundColor(0xFF2E7D32.toInt())
        btnSave.isClickable = true
        val dlp3 = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        dlp3.topMargin = 10
        btnSave.layoutParams = dlp3
        btnSave.setPadding(0, 14, 0, 14)
        container.addView(btnSave)

        btnDetect.setOnClickListener {
            LogStore.log("Key", "点击了检测Key按钮")
            val key = etKey.text.toString().trim()
            if (key.isEmpty()) {
                LogStore.log("Key", "检测失败: Key为空")
                tvDetect.text = getString(R.string.key_detect_empty)
                tvDetect.setTextColor(0xFFC62828.toInt())
                return@setOnClickListener
            }
            LogStore.log("Key", "开始检测Key, 长度=${key.length}, 前6位=${key.take(6)}")
            tvDetect.text = getString(R.string.key_detect_pending)
            tvDetect.setTextColor(0xFF0288D1.toInt())
            detectKey(key) { result ->
                LogStore.log("Key", "检测结果: $result")
                tvDetect.text = result
                tvDetect.setTextColor(
                    if (result.contains(getString(R.string.key_detect_valid)))
                        0xFF2E7D32.toInt() else 0xFFC62828.toInt()
                )
            }
        }

        btnSave.setOnClickListener {
            val key = etKey.text.toString().trim()
            if (key.isEmpty()) {
                Toast.makeText(this, R.string.key_empty, Toast.LENGTH_SHORT).show()
            } else {
                KeyStore.saveAmapKey(this, key)
                Toast.makeText(this, R.string.key_saved, Toast.LENGTH_SHORT).show()
            }
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.key_dialog_title)
            .setView(container)
            .setNegativeButton(R.string.key_cancel, null)
            .show()
    }

    /** 通过高德逆地理编码接口校验 Key 是否有效 */
    private fun detectKey(key: String, callback: (String) -> Unit) {
        Thread {
            try {
                val url = URL(
                    "https://restapi.amap.com/v3/geocode/regeo?location=116.397428,39.90923&key=$key"
                )
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "GET"
                val code = conn.responseCode
                val resp = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                LogStore.log("Key", "HTTP $code, 响应: ${resp.take(200)}")
                val valid = resp.contains("\"status\":\"1\"") && resp.contains("\"infocode\":\"10000\"")
                val invalidKey = resp.contains("\"infocode\":\"10001\"")
                Handler(Looper.getMainLooper()).post {
                    callback(
                        when {
                            valid -> getString(R.string.key_detect_valid)
                            invalidKey -> getString(R.string.key_detect_invalid)
                            else -> getString(R.string.key_detect_invalid)
                        }
                    )
                }
            } catch (e: Exception) {
                LogStore.log("Key", "检测异常: ${e.javaClass.simpleName}: ${e.message}")
                Handler(Looper.getMainLooper()).post {
                    callback(getString(R.string.key_detect_network))
                }
            }
        }.start()
    }

    private fun requestLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION
            )
            return
        }
        startUpdates()
    }

    private fun startUpdates() {
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        try {
            val gps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val net = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            (gps ?: net)?.let { updateUI(it) }

            if (gps != null) {
                lm.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 0L, 0f, this
                )
            }
            if (net != null) {
                lm.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 1000L, 1f, this
                )
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onLocationChanged(location: Location) {
        updateUI(location)
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    private fun updateUI(location: Location) {
        val speedMs = location.speed
        val speedValue = if (useMph) speedMs * 2.23694f else speedMs * 3.6f
        val accuracy = if (location.hasAccuracy()) location.accuracy else -1f

        tvSpeed.text = String.format("%.0f", speedValue)
        tvAccuracy.text = if (accuracy >= 0)
            String.format("精度: %.0fm", accuracy)
        else
            getString(R.string.accuracy_unknown)
        tvSource.text = String.format(
            "%s · %.6f, %.6f",
            if (location.provider == LocationManager.GPS_PROVIDER) "GPS" else "网络",
            location.latitude,
            location.longitude
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startUpdates()
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val REQUEST_LOCATION = 1001
    }
}
