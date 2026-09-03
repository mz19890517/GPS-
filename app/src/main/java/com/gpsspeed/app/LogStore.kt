package com.gpsspeed.app

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 简单的环形日志存储，内存 + 本地文件持久化。
 * 线程安全，供排查问题使用。
 */
object LogStore {

    private const val FILE_NAME = "gps_log.txt"
    private const val MAX_LINES = 500

    private val list = mutableListOf<String>()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var file: java.io.File? = null
    private var listeners = mutableListOf<() -> Unit>()

    @Volatile
    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        file = java.io.File(context.filesDir, FILE_NAME)
        // 加载已有日志
        try {
            if (file!!.exists()) {
                file!!.readLines().takeLast(MAX_LINES).forEach { list.add(it) }
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    fun log(tag: String, msg: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val line = "[$time][$tag] $msg"
        synchronized(list) {
            list.add(line)
            if (list.size > MAX_LINES) {
                list.removeAt(0)
            }
            persistLocked()
        }
        // 通知 UI 刷新（主线程）
        mainHandler.post { listeners.forEach { it() } }
    }

    private fun persistLocked() {
        val f = file ?: return
        try {
            var i = 0
            f.writeText(list.joinToString("\n"))
        } catch (e: Exception) {
            // ignore
        }
    }

    fun addListener(l: () -> Unit) {
        synchronized(listeners) { listeners.add(l) }
    }

    fun removeListener(l: () -> Unit) {
        synchronized(listeners) { listeners.remove(l) }
    }

    fun allText(): String = synchronized(list) { list.joinToString("\n") }

    fun clear() {
        synchronized(list) {
            list.clear()
            file?.delete()
        }
    }

    fun exportText(): String = allText()
}
