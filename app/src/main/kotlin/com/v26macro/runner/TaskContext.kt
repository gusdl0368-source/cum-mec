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
        threshold: Double = 0.75,
        region: Rect? = null,
    ): Match? {
        val frame = frame() ?: run {
            Logger.w("find $bucket/$name: 프레임 아직 없음 (캡처 시작 직후일 수 있음)")
            return null
        }
        val tmpl = library.load(bucket, name) ?: run {
            Logger.w("find $bucket/$name: 템플릿 PNG 가 없음 (assets/templates/$bucket/$name.png)")
            return null
        }
        val match = TemplateMatcher.findBest(frame, tmpl, threshold = threshold, region = region)
        if (match == null) {
            // 임계값을 0 으로 두고 한 번 더 — 진짜 점수가 얼마인지 로그로 남김
            val attempt = TemplateMatcher.findBest(frame, tmpl, threshold = 0.0, region = region)
            val scoreStr = attempt?.score?.let { "%.3f".format(it) } ?: "?"
            val scaleStr = attempt?.scale?.let { "%.2fx".format(it) } ?: "?"
            Logger.i("find $bucket/$name: NO MATCH (best=$scoreStr at $scaleStr, threshold=$threshold)")
        } else {
            Logger.i(
                "find $bucket/$name: MATCH score=${"%.3f".format(match.score)} " +
                    "scale=${"%.2fx".format(match.scale)} center=(${match.centerX},${match.centerY})"
            )
        }
        return match
    }

    /** Wait for a template to appear, then return its match. */
    suspend fun waitForTemplate(
        bucket: String,
        name: String,
        timeoutMs: Long = 15_000L,
        threshold: Double = 0.75,
        region: Rect? = null,
    ): Match? = waitFor(timeoutMs = timeoutMs) {
        find(bucket, name, threshold, region)
    }

    /**
     * 다음 우선순위로 탭 위치를 결정한다:
     *  1. coords.json 에 좌표가 정의되어 있으면 그 좌표를 사용
     *     - 좌표에 guard 템플릿이 지정되어 있으면 그 PNG 가 화면에 보일 때까지 대기 (timeoutMs)
     *       → 보이면 좌표 탭. 끝까지 안 보이면 실패.
     *     - guard 가 없으면 그대로 좌표 탭 (현재 화면 상태와 무관)
     *  2. 좌표가 없으면 기존대로 PNG 템플릿을 매칭해 그 중심을 탭
     */
    suspend fun tapTemplate(
        bucket: String,
        name: String,
        timeoutMs: Long = 15_000L,
        threshold: Double = 0.75,
    ): Boolean {
        // 1) 좌표 우선
        val coord = coords.get(bucket, name)
        if (coord != null) {
            val guardKey = coord.guardSplit()
            if (guardKey != null) {
                val (gb, gn) = guardKey
                val guardMatch = waitForTemplate(gb, gn, timeoutMs, threshold)
                if (guardMatch == null) {
                    Logger.w(
                        "coord-tap aborted: guard $gb/$gn not visible within ${timeoutMs}ms (target=$bucket/$name)"
                    )
                    return false
                }
                Logger.i("coord-tap guard $gb/$gn matched score=${"%.3f".format(guardMatch.score)}")
            }
            val w = screenWidth.coerceAtLeast(1)
            val h = screenHeight.coerceAtLeast(1)
            val x = (w * coord.xFrac).toInt().coerceIn(0, w - 1)
            val y = (h * coord.yFrac).toInt().coerceIn(0, h - 1)
            Logger.i("coord-tap $bucket/$name @($x,$y) frac=(${coord.xFrac},${coord.yFrac})")
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
