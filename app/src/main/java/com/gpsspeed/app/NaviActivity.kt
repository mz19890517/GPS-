package com.gpsspeed.app

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.amap.api.navi.AMapNavi
import com.amap.api.navi.AMapNaviListener
import com.amap.api.navi.AMapNaviView
import com.amap.api.navi.AMapNaviViewListener
import com.amap.api.navi.enums.PathPlanningStrategy
import com.amap.api.navi.model.AMapCalcRouteResult
import com.amap.api.navi.model.AMapLaneInfo
import com.amap.api.navi.model.AMapModelCross
import com.amap.api.navi.model.AMapNaviCameraInfo
import com.amap.api.navi.model.AMapNaviCross
import com.amap.api.navi.model.AMapNaviInfo
import com.amap.api.navi.model.AMapNaviLocation
import com.amap.api.navi.model.AMapNaviRouteNotifyData
import com.amap.api.navi.model.AMapNaviTrafficFacilityInfo
import com.amap.api.navi.model.AMapServiceAreaInfo
import com.amap.api.navi.model.AimLessModeCongestionInfo
import com.amap.api.navi.model.AimLessModeStat
import com.amap.api.navi.model.NaviInfo
import com.amap.api.navi.model.NaviLatLng
import com.autonavi.tbt.TrafficFacilityInfo
import kotlin.math.cos
import kotlin.math.sin

class NaviActivity : AppCompatActivity(), AMapNaviListener, AMapNaviViewListener {

    private lateinit var naviView: AMapNaviView
    private lateinit var tvRoadName: TextView
    private lateinit var tvLightCount: TextView
    private lateinit var tvLoading: TextView

    private var navi: AMapNavi? = null
    private var autoRouteRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navi)

        naviView = findViewById(R.id.navi_view)
        tvRoadName = findViewById(R.id.tv_road_name)
        tvLightCount = findViewById(R.id.tv_light_count)
        tvLoading = findViewById(R.id.tv_loading)

        tvLightCount.text = ""
        tvLoading.text = getString(R.string.navi_loading)

        naviView.setAMapNaviViewListener(this)
        naviView.onCreate(savedInstanceState)

        // 运行时设置用户在软件内填写的高德 Key
        val amapKey = KeyStore.getAmapKey(this)
        if (amapKey.isNotEmpty()) {
            AMapNavi.setApiKey(applicationContext, amapKey)
            tvLoading.text = ""
        } else {
            tvLoading.text = getString(R.string.key_not_configured)
        }

        navi = AMapNavi.getInstance(this)
        navi?.addAMapNaviListener(this)

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        checkLocationPermission()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQ_LOCATION
            )
            return
        }
        startAutoNavigation()
    }

    /** 自动定位当前坐标，算一条到前方 20 公里处的路线以进入导航模式 */
    private fun startAutoNavigation() {
        if (!KeyStore.hasAmapKey(this)) {
            tvLoading.text = getString(R.string.key_not_configured)
            return
        }
        val lm = getSystemService(LOCATION_SERVICE) as LocationManager
        val loc = try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            } else null
        } catch (e: SecurityException) {
            null
        }

        if (loc == null) {
            tvLoading.text = getString(R.string.navi_no_location)
            // 没拿到定位，用一个默认坐标（北京天安门）尝试
            routeToAhead(39.90923, 116.397428, 0f)
            return
        }
        routeToAhead(loc.latitude, loc.longitude, loc.bearing)
    }

    private fun routeToAhead(lat: Double, lng: Double, bearing: Float) {
        val b = if (bearing in 1f..359f) bearing else 90f
        val endPoint = offsetPoint(lat, lng, 20000.0, b)

        val startList = listOf(NaviLatLng(lat, lng))
        val endList = listOf(NaviLatLng(endPoint.first, endPoint.second))

        tvLoading.text = getString(R.string.navi_routing)
        autoRouteRequested = true
        navi?.calculateDriveRoute(
            startList, endList, emptyList(), PathPlanningStrategy.DRIVING_AVOID_CONGESTION
        )
    }

    /** 沿给定方向偏移 distanceMeters 米，返回 (lat, lng) */
    private fun offsetPoint(lat: Double, lng: Double, distanceMeters: Double, bearingDeg: Float): Pair<Double, Double> {
        val bearingRad = Math.toRadians(bearingDeg.toDouble())
        val latRad = Math.toRadians(lat)
        val metersPerDegLat = 111320.0
        val dLat = (distanceMeters / metersPerDegLat) * cos(bearingRad)
        val dLng = (distanceMeters / metersPerDegLat) * sin(bearingRad) / cos(latRad)
        return Pair(lat + dLat, lng + dLng)
    }

    override fun onInitNaviFailure() {
        tvLoading.text = getString(R.string.navi_no_key)
    }

    override fun onInitNaviSuccess() {}

    override fun onCalculateRouteSuccess(intArray: IntArray) {
        navi?.startNavi(AMapNavi.GPSNaviMode)
        tvLoading.text = ""
    }

    override fun onCalculateRouteSuccess(result: AMapCalcRouteResult?) {
        navi?.startNavi(AMapNavi.GPSNaviMode)
        tvLoading.text = ""
    }

    override fun onCalculateRouteFailure(errorCode: Int) {
        tvLoading.text = getString(R.string.navi_route_fail)
    }

    override fun onCalculateRouteFailure(result: AMapCalcRouteResult?) {
        tvLoading.text = getString(R.string.navi_route_fail)
    }

    override fun onNaviInfoUpdate(info: NaviInfo?) {
        if (info != null) {
            tvLoading.text = ""
            val remain = info.routeRemainLightCount
            tvLightCount.text = getString(R.string.navi_remain_lights, remain)
            val road = info.currentRoadName
            tvRoadName.text = getString(R.string.navi_cur_road, road ?: "")
        }
    }

    override fun onArriveDestination() {}

    override fun onGpsOpenStatus(enabled: Boolean) {}

    override fun onStartNavi(type: Int) {}

    override fun onTrafficStatusUpdate() {}

    override fun onLocationChange(location: AMapNaviLocation?) {}

    override fun onGetNavigationText(text: String) {}

    override fun onGetNavigationText(p0: Int, p1: String) {}

    override fun onEndEmulatorNavi() {}

    override fun onReCalculateRouteForYaw() {}

    override fun onReCalculateRouteForTrafficJam() {}

    override fun onArrivedWayPoint(wayID: Int) {}

    override fun onNaviInfoUpdated(naviInfo: AMapNaviInfo?) {}

    override fun updateCameraInfo(cameraInfos: Array<out AMapNaviCameraInfo>?) {}

    override fun updateIntervalCameraInfo(cameraInfo: AMapNaviCameraInfo?, cameraInfo1: AMapNaviCameraInfo?, interval: Int) {}

    override fun onServiceAreaUpdate(serviceAreas: Array<out AMapServiceAreaInfo>?) {}

    override fun showCross(cross: AMapNaviCross?) {}

    override fun hideCross() {}

    override fun showModeCross(cross: AMapModelCross?) {}

    override fun hideModeCross() {}

    override fun showLaneInfo(laneInfos: Array<out AMapLaneInfo>?, laneBackgroundInfo: ByteArray?, laneRecommendedInfo: ByteArray?) {}

    override fun showLaneInfo(amapLaneInfo: AMapLaneInfo?) {}

    override fun hideLaneInfo() {}

    override fun notifyParallelRoad(type: Int) {}

    override fun OnUpdateTrafficFacility(amapNaviTrafficFacilityInfo: Array<out AMapNaviTrafficFacilityInfo>?) {}

    override fun OnUpdateTrafficFacility(amapNaviTrafficFacilityInfo: AMapNaviTrafficFacilityInfo?) {}

    override fun OnUpdateTrafficFacility(trafficFacilityInfo: TrafficFacilityInfo?) {}

    override fun updateAimlessModeStatistics(aimLessModeStat: AimLessModeStat?) {}

    override fun updateAimlessModeCongestionInfo(aimLessModeCongestionInfo: AimLessModeCongestionInfo?) {}

    override fun onPlayRing(type: Int) {}

    override fun onNaviRouteNotify(aMapNaviRouteNotifyData: AMapNaviRouteNotifyData?) {}

    override fun onNaviSetting() {}

    override fun onNaviCancel() {}

    override fun onNaviBackClick(): Boolean {
        finish()
        return true
    }

    override fun onNaviMapMode(i: Int) {}

    override fun onNaviTurnClick() {}

    override fun onNextRoadClick() {}

    override fun onScanViewButtonClick() {}

    override fun onLockMap(isLock: Boolean) {}

    override fun onNaviViewLoaded() {}

    override fun onMapTypeChanged(i: Int) {}

    override fun onNaviViewShowMode(i: Int) {}

    override fun onResume() {
        super.onResume()
        naviView.onResume()
    }

    override fun onPause() {
        super.onPause()
        naviView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        naviView.onDestroy()
        navi?.removeAMapNaviListener(this)
        navi?.destroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        naviView.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_LOCATION && grantResults.isNotEmpty()
            && grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startAutoNavigation()
        }
    }

    companion object {
        private const val REQ_LOCATION = 2001
    }
}
