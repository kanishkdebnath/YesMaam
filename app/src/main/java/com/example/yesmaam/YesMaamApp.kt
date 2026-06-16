package com.example.yesmaam

import android.app.Application
import com.example.yesmaam.di.AppContainer

class YesMaamApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
