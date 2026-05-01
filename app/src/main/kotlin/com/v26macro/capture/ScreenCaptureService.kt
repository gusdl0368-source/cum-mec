package com.v26macro.capture

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.DisplayMetrics
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.v26macro.MainActivity
import com.v26macro.R
import com.v26macro.V26MacroApp
import com.v26macro.util.Logger

class ScreenCaptureService : LifecycleService() {

    private var projection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private val handlerThread = HandlerThread("frame-reader").apply { start() }
    private val handler = Handler(handlerThread.looper)

    private var width = 0
    private var height = 0
    private var density = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(NOTIF_ID, buildNotification())

        val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
        val resultData = intent?.getParcelableExtra<Intent>(EXTRA_RESULT_DATA)
        if (resultCode == 0 || resultData == null) {
            Logger.e("ScreenCaptureService missing projection result")
            stopSelf()
            return START_NOT_STICKY
        }

        runningRef = this
        startProjection(resultCode, resultData)
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val tap = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, V26MacroApp.CHANNEL_CAPTURE)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.capture_running))
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .setContentIntent(tap)
            .build()
    }

    @Suppress("DEPRECATION")
    private fun startProjection(resultCode: Int, data: Intent) {
        val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val proj = mgr.getMediaProjection(resultCode, data) ?: run {
            Logger.e("getMediaProjection returned null")
            stopSelf()
            return
        }
        projection = proj
        proj.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Logger.i("MediaProjection stopped")
                tearDown()
            }
        }, handler)

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(metrics)
        width = metrics.widthPixels
        height = metrics.heightPixels
        density = metrics.densityDpi

        createVirtualDisplay()
    }

    private fun createVirtualDisplay() {
        val proj = projection ?: return
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        reader.setOnImageAvailableListener({ r ->
            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                val plane = image.planes[0]
                val rowStride = plane.rowStride
                val pixelStride = plane.pixelStride
                val rowPadding = rowStride - pixelStride * width
                val bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(plane.buffer)
                val cropped = if (rowPadding == 0) bitmap
                else Bitmap.createBitmap(bitmap, 0, 0, width, height)
                FrameProvider.publish(cropped)
            } catch (t: Throwable) {
                Logger.w("frame copy failed", t)
            } finally {
                image.close()
            }
        }, handler)
        imageReader = reader

        virtualDisplay = proj.createVirtualDisplay(
            "v26macro-cap",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface, null, handler
        )
        Logger.i("VirtualDisplay started ${width}x${height} dpi=$density")
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Re-create the virtual display when rotation/size changes.
        val wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION") wm.defaultDisplay.getRealMetrics(metrics)
        if (metrics.widthPixels != width || metrics.heightPixels != height) {
            virtualDisplay?.release()
            imageReader?.close()
            width = metrics.widthPixels
            height = metrics.heightPixels
            density = metrics.densityDpi
            createVirtualDisplay()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        tearDown()
        handlerThread.quitSafely()
        super.onDestroy()
    }

    private fun tearDown() {
        runningRef = null
        virtualDisplay?.release(); virtualDisplay = null
        imageReader?.close(); imageReader = null
        projection?.stop(); projection = null
        FrameProvider.clear()
    }

    companion object {
        private const val NOTIF_ID = 1001
        private const val EXTRA_RESULT_CODE = "code"
        private const val EXTRA_RESULT_DATA = "data"

        @Volatile
        private var runningRef: ScreenCaptureService? = null

        val isRunning: Boolean get() = runningRef != null

        fun start(ctx: Context, resultCode: Int, data: Intent) {
            val intent = Intent(ctx, ScreenCaptureService::class.java).apply {
                putExtra(EXTRA_RESULT_CODE, resultCode)
                putExtra(EXTRA_RESULT_DATA, data)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, ScreenCaptureService::class.java))
        }
    }
}
