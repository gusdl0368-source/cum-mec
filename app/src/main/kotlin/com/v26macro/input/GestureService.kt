package com.v26macro.input

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import com.v26macro.util.Logger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * AccessibilityService that injects taps and swipes via dispatchGesture().
 * The MacroRunner calls into the static [tap] / [swipe] / [longPress] methods
 * which suspend until the gesture completes.
 *
 * The service does not read the window content - we rely on screen capture for vision.
 */
class GestureService : AccessibilityService() {

    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Logger.i("GestureService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // intentionally empty - we only need gesture dispatch.
    }

    override fun onInterrupt() {
        Logger.w("GestureService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (instance === this) instance = null
    }

    private suspend fun dispatch(gesture: GestureDescription): Boolean = suspendCoroutine { cont ->
        val ok = dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(g: GestureDescription?) { cont.resume(true) }
            override fun onCancelled(g: GestureDescription?) { cont.resume(false) }
        }, mainHandler)
        if (!ok) cont.resume(false)
    }

    companion object {
        @Volatile
        var instance: GestureService? = null
            private set

        val isReady: Boolean get() = instance != null

        suspend fun tap(x: Float, y: Float, durationMs: Long = 60L): Boolean {
            val svc = instance ?: run {
                Logger.w("tap requested but accessibility service not connected")
                return false
            }
            val path = Path().apply { moveTo(x, y) }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
                .build()
            return svc.dispatch(gesture)
        }

        suspend fun longPress(x: Float, y: Float, durationMs: Long = 800L): Boolean =
            tap(x, y, durationMs)

        suspend fun swipe(
            x1: Float, y1: Float, x2: Float, y2: Float,
            durationMs: Long = 300L,
        ): Boolean {
            val svc = instance ?: run {
                Logger.w("swipe requested but accessibility service not connected")
                return false
            }
            val path = Path().apply {
                moveTo(x1, y1)
                lineTo(x2, y2)
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
                .build()
            return svc.dispatch(gesture)
        }
    }
}
