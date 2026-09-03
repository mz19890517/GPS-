package com.gpsspeed.app

import android.app.Application

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        LogStore.init(this)
        LogStore.log("App", "应用启动")
    }

    companion object {
        lateinit var instance: App
            private set
    }
}
