package com.v26macro

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import org.opencv.android.OpenCVLoader

class V26MacroApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        OpenCVLoader.initLocal()
        registerNotificationChannels()
    }

    private fun registerNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CAPTURE,
                getString(R.string.screen_capture_channel),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        nm.createNotificationChannel(
            NotificationChannel(
                CHANNEL_OVERLAY,
                getString(R.string.overlay_channel),
                NotificationManager.IMPORTANCE_MIN
            )
        )
    }

    companion object {
        const val CHANNEL_CAPTURE = "capture"
        const val CHANNEL_OVERLAY = "overlay"

        lateinit var instance: V26MacroApp
            private set
    }
}
