package com.gpsspeed.app

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.gpsspeed.app.DestinationStore.Destination
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

@SuppressLint("SetTextI18n")
class DestinationActivity : AppCompatActivity() {

    private lateinit var etSearch: EditText
    private lateinit var lvResults: ListView
    private lateinit var lvSaved: ListView
    private lateinit var tvResultHint: TextView

    private var results: List<Destination> = emptyList()
    private var saved: List<Destination> = emptyList()

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_destination)

        etSearch = findViewById(R.id.et_search)
        lvResults = findViewById(R.id.lv_results)
        lvSaved = findViewById(R.id.lv_saved)
        tvResultHint = findViewById(R.id.tv_result_hint)

        findViewById<View>(R.id.btn_back_dest).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_clear_dest).setOnClickListener {
            DestinationStore.clear(this)
            refreshSaved()
            Toast.makeText(this, R.string.dest_cleared, Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.btn_search).setOnClickListener { doSearch() }
        etSearch.setOnEditorActionListener { _, _, _ ->
            doSearch()
            true
        }

        lvSaved.setOnItemClickListener { _, _, position, _ ->
            navigateTo(saved[position])
        }
        lvSaved.setOnItemLongClickListener { _, _, position, _ ->
            val d = saved[position]
            DestinationStore.remove(this, position)
            refreshSaved()
            Toast.makeText(this, getString(R.string.dest_deleted, d.name), Toast.LENGTH_SHORT).show()
            true
        }
        lvResults.setOnItemClickListener { _, _, position, _ ->
            val d = results[position]
            DestinationStore.add(this, d)
            refreshSaved()
            Toast.makeText(this, getString(R.string.dest_added, d.name), Toast.LENGTH_SHORT).show()
        }

        refreshSaved()
    }

    override fun onResume() {
        super.onResume()
        refreshSaved()
    }

    private fun refreshSaved() {
        saved = DestinationStore.getList(this)
        lvSaved.adapter = object : ArrayAdapter<Destination>(
            this, android.R.layout.simple_list_item_1, saved
        ) {
            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                val v = super.getView(position, convertView, parent) as TextView
                val d = getItem(position) ?: return v
                v.text = d.name + "\n" + String.format("%.6f, %.6f", d.lat, d.lng)
                v.setTextColor(0xFFDDDDDD.toInt())
                v.textSize = 15f
                v.setPadding(24, 24, 24, 24)
                v.gravity = Gravity.CENTER_VERTICAL
                return v
            }
        }
    }

    private fun doSearch() {
        val keyword = etSearch.text.toString().trim()
        if (keyword.isEmpty()) {
            Toast.makeText(this, R.string.dest_keyword_empty, Toast.LENGTH_SHORT).show()
            return
        }
        val webKey = KeyStore.getWebKey(this)
        if (!KeyStore.hasWebKey(this)) {
            Toast.makeText(this, R.string.dest_need_webkey, Toast.LENGTH_LONG).show()
            return
        }

        tvResultHint.text = getString(R.string.dest_searching)
        tvResultHint.visibility = View.VISIBLE
        lvResults.adapter = null
        LogStore.log("Dest", "发起POI搜索: $keyword")

        Thread {
            try {
                val keywordEnc = URLEncoder.encode(keyword, "UTF-8")
                val url = URL(
                    "https://restapi.amap.com/v3/place/text" +
                            "?key=$webKey&keywords=$keywordEnc&offset=20&page=1&extensions=base"
                )
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.requestMethod = "GET"
                val code = conn.responseCode
                val resp = conn.inputStream.bufferedReader().readText()
                conn.disconnect()
                LogStore.log("Dest", "搜索HTTP $code, 响应:${resp.take(200)}")

                val obj = JSONObject(resp)
                val status = obj.optString("status", "")
                val pois: List<Destination> = if (status == "1") {
                    val arr = obj.optJSONArray("pois") ?: JSONArray()
                    val out = mutableListOf<Destination>()
                    for (i in 0 until arr.length()) {
                        val p = arr.getJSONObject(i)
                        val location = p.optString("location", "")
                        val parts = location.split(",")
                        if (parts.size == 2) {
                            out.add(
                                Destination(
                                    p.optString("name", ""),
                                    parts[1].toDouble(),
                                    parts[0].toDouble()
                                )
                            )
                        }
                    }
                    out
                } else {
                    emptyList()
                }

                mainHandler.post {
                    results = pois
                    if (pois.isEmpty()) {
                        tvResultHint.text = getString(R.string.dest_no_result)
                    } else {
                        tvResultHint.text = getString(R.string.dest_result_count, pois.size)
                        lvResults.adapter = object : ArrayAdapter<Destination>(
                            this@DestinationActivity,
                            android.R.layout.simple_list_item_1,
                            pois
                        ) {
                            override fun getView(position: Int, convertView: View?, parent: android.view.ViewGroup): View {
                                val v = super.getView(position, convertView, parent) as TextView
                                val d = getItem(position) ?: return v
                                v.text = d.name + "  (点击保存)"
                                v.setTextColor(0xFF4FC3F7.toInt())
                                v.textSize = 15f
                                v.setPadding(24, 24, 24, 24)
                                v.gravity = Gravity.CENTER_VERTICAL
                                return v
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                LogStore.log("Dest", "搜索异常: ${e.javaClass.simpleName}: ${e.message}")
                mainHandler.post {
                    tvResultHint.text = getString(R.string.dest_search_fail)
                }
            }
        }.start()
    }

    /** 用 URI 调起高德 App 直接导航（读秒/悬浮窗均由高德内置） */
    private fun navigateTo(d: Destination) {
        LogStore.log("Dest", "一键导航: ${d.name} (${d.lat}, ${d.lng})")
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(
                "androidamap://navi?sourceApplication=gpsspeed" +
                        "&lat=${d.lat}&lon=${d.lng}&poiname=${Uri.encode(d.name)}&style=2&dev=0"
            )
            startActivity(intent)
        } catch (e: Exception) {
            LogStore.log("Dest", "拉起高德失败: ${e.message}")
            Toast.makeText(this, R.string.dest_no_amap, Toast.LENGTH_LONG).show()
        }
    }
}
