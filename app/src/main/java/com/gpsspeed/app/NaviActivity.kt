package com.gpsspeed.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
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
import com.autonavi.tbt.TrafficFacilityInfo

class NaviActivity : AppCompatActivity(), AMapNaviListener, AMapNaviViewListener {

    private lateinit var naviView: AMapNaviView
    private lateinit var etDest: EditText
    private lateinit var btnStart: Button
    private lateinit var lightPanel: LinearLayout
    private lateinit var tvLightCount: TextView

    private var navi: AMapNavi? = null
    private var isNaviStarted = false
    private var lastLat = 0.0
    private var lastLng = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navi)

        naviView = findViewById(R.id.navi_view)
        etDest = findViewById(R.id.et_dest)
        btnStart = findViewById(R.id.btn_start_navi)
        lightPanel = findViewById(R.id.light_panel)
        tvLightCount = findViewById(R.id.tv_light_count)

        naviView.setAMapNaviViewListener(this)
        naviView.onCreate(savedInstanceState)

        navi = AMapNavi.getInstance(this)
        navi?.addAMapNaviListener(this)

        btnStart.setOnClickListener { startNavigate() }
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
        }
    }

    private fun startNavigate() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, R.string.navi_need_location, Toast.LENGTH_SHORT).show()
            return
        }
        val dest = etDest.text.toString().trim()
        if (TextUtils.isEmpty(dest)) {
            toast(R.string.navi_dest_hint)
            return
        }
        val n = navi ?: run {
            toast(R.string.navi_no_key)
            return
        }
        // 关闭输入面板，进入导航全屏
        findViewById<View>(R.id.dest_panel).visibility = View.GONE
        // 地址关键字算路（起点为空表示从当前位置出发）
        n.calculateDriveRoute("", dest, emptyList(), PathPlanningStrategy.DRIVING_AVOID_CONGESTION)
    }

    override fun onInitNaviSuccess() {}

    override fun onCalculateRouteSuccess(intArray: IntArray) {
        navi?.startNavi(AMapNavi.GPSNaviMode)
        isNaviStarted = true
        btnStart.visibility = View.GONE
    }

    override fun onCalculateRouteSuccess(result: AMapCalcRouteResult?) {
        navi?.startNavi(AMapNavi.GPSNaviMode)
        isNaviStarted = true
        btnStart.visibility = View.GONE
    }

    override fun onCalculateRouteFailure(errorCode: Int) {
        toast(R.string.navi_route_fail)
    }

    override fun onCalculateRouteFailure(result: AMapCalcRouteResult?) {
        toast(R.string.navi_route_fail)
    }

    override fun onNaviInfoUpdate(info: NaviInfo?) {
        // 红绿灯剩余数量
        if (info != null && lightPanel != null) {
            val remain = info.routeRemainLightCount
            lightPanel.visibility = View.VISIBLE
            tvLightCount.text = getString(R.string.navi_remain_lights, remain)
        }
    }

    override fun onArriveDestination() {
        Toast.makeText(this, "到达目的地", Toast.LENGTH_SHORT).show()
    }

    override fun onGpsOpenStatus(enabled: Boolean) {}

    override fun onStartNavi(type: Int) {}

    override fun onTrafficStatusUpdate() {}

    override fun onLocationChange(location: AMapNaviLocation?) {
        location?.let {
            lastLat = it.coord.latitude
            lastLng = it.coord.longitude
        }
    }

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
            Toast.makeText(this, "位置权限已开启，点击开始导航", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toast(resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val REQ_LOCATION = 2001
    }
}
