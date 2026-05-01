package com.v26macro.runner

import android.graphics.Bitmap
import android.graphics.Rect
import com.v26macro.capture.FrameProvider
import com.v26macro.input.GestureService
import com.v26macro.util.Logger
import com.v26macro.util.humanDelay
import com.v26macro.util.waitFor
import com.v26macro.vision.Match
import com.v26macro.vision.TemplateLibrary
import com.v26macro.vision.TemplateMatcher

/**
 * Shared helpers passed to every Task. Centralizes "find a button -> tap it ->
 * wait for next state" so each Task can read like a script.
 */
class TaskContext(
    private val library: TemplateLibrary,
    val onProgress: (String) -> Unit,
) {
    val screenWidth: Int get() = FrameProvider.screenWidth
    val screenHeight: Int get() = FrameProvider.screenHeight

    fun frame(): Bitmap? = FrameProvider.latest

    /** Find a template in the latest frame. */
    suspend fun find(
        bucket: String,
        name: String,
        threshold: Double = 0.85,
        region: Rect? = null,
    ): Match? {
        val frame = frame() ?: return null
        val tmpl = library.load(bucket, name) ?: return null
        return TemplateMatcher.findBest(frame, tmpl, threshold = threshold, region = region)
    }

    /** Wait for a template to appear, then return its match. */
    suspend fun waitForTemplate(
        bucket: String,
        name: String,
        timeoutMs: Long = 15_000L,
        threshold: Double = 0.85,
        region: Rect? = null,
    ): Match? = waitFor(timeoutMs = timeoutMs) {
        find(bucket, name, threshold, region)
    }

    /** Try to find a template and tap its center. Returns true on success. */
    suspend fun tapTemplate(
        bucket: String,
        name: String,
        timeoutMs: Long = 15_000L,
        threshold: Double = 0.85,
    ): Boolean {
        val match = waitForTemplate(bucket, name, timeoutMs, threshold) ?: run {
            Logger.w("tapTemplate: $bucket/$name not found within ${timeoutMs}ms")
            return false
        }
        Logger.i("tap $bucket/$name @(${match.centerX},${match.centerY}) score=${"%.3f".format(match.score)}")
        val ok = GestureService.tap(match.centerX.toFloat(), match.centerY.toFloat())
        humanDelay()
        return ok
    }

    suspend fun tap(x: Int, y: Int): Boolean {
        val ok = GestureService.tap(x.toFloat(), y.toFloat())
        humanDelay()
        return ok
    }

    suspend fun swipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Long = 300L): Boolean {
        val ok = GestureService.swipe(x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), durationMs)
        humanDelay()
        return ok
    }

    fun progress(msg: String) {
        Logger.i("progress: $msg")
        onProgress(msg)
    }
}
