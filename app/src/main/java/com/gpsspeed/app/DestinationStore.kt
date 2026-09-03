package com.gpsspeed.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 本地持久化“我的目的地”列表（名称 + 经纬度）。
 * 用 SharedPreferences 存 JSON 数组，线程安全。
 */
object DestinationStore {

    private const val PREFS = "dest_prefs"
    private const val KEY_LIST = "dest_list"

    data class Destination(
        val name: String,
        val lat: Double,
        val lng: Double
    ) {
        fun toJson(): JSONObject = JSONObject()
            .put("name", name)
            .put("lat", lat)
            .put("lng", lng)
    }

    fun getList(context: Context): List<Destination> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_LIST, "[]") ?: "[]"
        return try {
            val arr = JSONArray(raw)
            val out = mutableListOf<Destination>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(
                    Destination(
                        o.optString("name", ""),
                        o.optDouble("lat", 0.0),
                        o.optDouble("lng", 0.0)
                    )
                )
            }
            out
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun add(context: Context, dest: Destination) {
        val list = getList(context).toMutableList()
        list.add(dest)
        save(context, list)
    }

    fun remove(context: Context, index: Int) {
        val list = getList(context).toMutableList()
        if (index in list.indices) {
            list.removeAt(index)
            save(context, list)
        }
    }

    fun clear(context: Context) {
        save(context, emptyList())
    }

    private fun save(context: Context, list: List<Destination>) {
        val arr = JSONArray()
        list.forEach { arr.put(it.toJson()) }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LIST, arr.toString())
            .apply()
    }
}
