package com.v26macro.v2.capture

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.v26macro.v2.App
import com.v26macro.v2.MainActivity
import com.v26macro.v2.R

/**
 * MediaProjection 기반 포그라운드 캡처 서비스.
 *
 * Phase 1: 알림만 띄우는 빈 서비스.
 * Phase 2: ImageReader → Bitmap Flow 노출, 비트맵 풀링, 1fps default + 트리거 모드.
 */
class ScreenCaptureService : Service() {

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        latestFrame = null
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val tap = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, App.CHANNEL_CAPTURE)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.capture_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setContentIntent(tap)
            .setOngoing(true)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1001

        @Volatile
        var latestFrame: Bitmap? = null
            internal set

        fun start(context: Context) {
            context.startForegroundService(Intent(context, ScreenCaptureService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, ScreenCaptureService::class.java))
        }
    }
}
