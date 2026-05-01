package com.v26macro.overlay

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.v26macro.MainActivity
import com.v26macro.R
import com.v26macro.V26MacroApp
import com.v26macro.runner.MacroRunner
import com.v26macro.util.Logger

/**
 * Floating control panel — 시작/정지 버튼과 현재 진행 상태만 보여준다. 일과 토글/횟수
 * 같은 설정은 MainActivity 의 카드 UI 에서 관리하고 DataStore (MacroSettings) 에 저장됨.
 */
class OverlayService : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner {

    private val savedState = SavedStateRegistryController.create(this)
    override val savedStateRegistry get() = savedState.savedStateRegistry
    private val store = ViewModelStore()
    override val viewModelStore: ViewModelStore get() = store

    private lateinit var wm: WindowManager
    private var rootView: ComposeView? = null
    private lateinit var runner: MacroRunner

    override fun onCreate() {
        super.onCreate()
        savedState.performAttach()
        savedState.performRestore(null)
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        runner = MacroRunner(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        startForeground(NOTIF_ID, buildNotification())
        if (rootView == null) showOverlay()
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private fun buildNotification(): Notification {
        val tap = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, V26MacroApp.CHANNEL_OVERLAY)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.overlay_running))
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .setContentIntent(tap)
            .build()
    }

    private fun showOverlay() {
        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeViewModelStoreOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
            setContent {
                val state by runner.state.collectAsState()
                OverlayPanel(
                    state = state,
                    onStart = { runner.start() },
                    onStop = { runner.stop() },
                    onClose = { stopOverlay() },
                )
            }
        }
        rootView = view

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 80
        }

        attachDragHandler(view, params)
        try {
            wm.addView(view, params)
        } catch (t: Throwable) {
            Logger.e("addView for overlay failed - check overlay permission", t)
            stopSelf()
        }
    }

    private fun attachDragHandler(view: View, params: WindowManager.LayoutParams) {
        var startX = 0
        var startY = 0
        var rawX = 0f
        var rawY = 0f
        view.setOnTouchListener { _, ev ->
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x; startY = params.y
                    rawX = ev.rawX; rawY = ev.rawY
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (ev.rawX - rawX).toInt()
                    params.y = startY + (ev.rawY - rawY).toInt()
                    runCatching { wm.updateViewLayout(view, params) }
                    false
                }
                else -> false
            }
        }
    }

    private fun stopOverlay() {
        rootView?.let { runCatching { wm.removeView(it) } }
        rootView = null
        stopSelf()
    }

    override fun onDestroy() {
        runner.stop()
        runner.shutdown()
        rootView?.let { runCatching { wm.removeView(it) } }
        rootView = null
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 1002

        fun start(ctx: Context) {
            val intent = Intent(ctx, OverlayService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, OverlayService::class.java))
        }
    }
}
