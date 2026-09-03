package com.gpsspeed.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LogActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView
    private val refreshListener = { refresh() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        tvLog = findViewById(R.id.tv_log)

        findViewById<android.view.View>(R.id.btn_back_log).setOnClickListener { finish() }

        findViewById<android.view.View>(R.id.btn_clear_log).setOnClickListener {
            LogStore.clear()
            refresh()
            Toast.makeText(this, R.string.log_cleared, Toast.LENGTH_SHORT).show()
        }

        findViewById<android.view.View>(R.id.btn_copy_log).setOnClickListener {
            val text = LogStore.exportText()
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("log", text))
            Toast.makeText(this, R.string.log_copied, Toast.LENGTH_SHORT).show()
        }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        LogStore.addListener(refreshListener)
    }

    override fun onPause() {
        super.onPause()
        LogStore.removeListener(refreshListener)
    }

    private fun refresh() {
        val text = LogStore.allText()
        runOnUiThread { tvLog.text = if (text.isEmpty()) getString(R.string.log_empty) else text }
    }
}
