package com.gpsspeed.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

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
            startActivity(Intent(this, NaviActivity::class.java))
        }

        requestLocation()
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
