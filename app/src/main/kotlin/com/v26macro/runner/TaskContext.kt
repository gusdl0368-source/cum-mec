package com.v26macro.runner

import android.graphics.Bitmap
import android.graphics.Rect
import com.v26macro.capture.FrameProvider
import com.v26macro.input.GestureService
import com.v26macro.util.Logger
import com.v26macro.util.humanDelay
import com.v26macro.util.waitFor
import com.v26macro.vision.CoordLibrary
import com.v26macro.vision.Match
import com.v26macro.vision.TemplateLibrary
import com.v26macro.vision.TemplateMatcher

/**
 * Shared helpers passed to every Task. Centralizes "find a button -> tap it ->
 * wait for next state" so each Task can read like a script.
 *
 * `tapTemplate` 은 coords.json 에 좌표가 정의된 항목이라면 템플릿 매칭을 건너뛰고
 * 좌표를 그대로 탭한다. 이 동작은 팀 컬러로 배경이 바뀌는 메뉴 버튼처럼 매칭 점수가
 * 흔들리는 곳에서 유용하다.
 */
class TaskContext(
    private val library: TemplateLibrary,
    private val coords: CoordLibrary,
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

    /**
     * coords.json 에 정의된 정규화 좌표가 있으면 그 좌표를 그대로 탭하고, 없으면
     * 템플릿을 찾아 그 중심을 탭한다.
     */
    suspend fun tapTemplate(
        bucket: String,
        name: String,
        timeoutMs: Long = 15_000L,
        threshold: Double = 0.85,
    ): Boolean {
        // 1) 좌표 우선
        coords.get(bucket, name)?.let { (xFrac, yFrac) ->
            val w = screenWidth.coerceAtLeast(1)
            val h = screenHeight.coerceAtLeast(1)
            val x = (w * xFrac).toInt().coerceIn(0, w - 1)
            val y = (h * yFrac).toInt().coerceIn(0, h - 1)
            Logger.i("coord-tap $bucket/$name @($x,$y) (frac=$xFrac,$yFrac)")
            val ok = GestureService.tap(x.toFloat(), y.toFloat())
            humanDelay()
            return ok
        }

        // 2) 템플릿 매칭
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
