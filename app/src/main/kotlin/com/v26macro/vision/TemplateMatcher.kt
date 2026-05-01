package com.v26macro.vision

import android.graphics.Bitmap
import android.graphics.Rect
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfDouble
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc

/**
 * OpenCV-backed template matching with optional multi-scale search.
 *
 * Returned [Match.score] uses TM_CCOEFF_NORMED — values ≥ ~0.85 are usually safe.
 */
data class Match(val rect: Rect, val score: Double, val scale: Double) {
    val centerX: Int get() = rect.centerX()
    val centerY: Int get() = rect.centerY()
}

object TemplateMatcher {

    /**
     * @param scales scale factors to try; default = single scale.
     * @param threshold minimum score to accept.
     * @param region optional sub-rect of [frame] to search in (in frame pixel coords).
     */
    fun findBest(
        frame: Bitmap,
        template: Bitmap,
        scales: DoubleArray = doubleArrayOf(1.0, 0.9, 1.1, 0.8, 1.2),
        threshold: Double = 0.85,
        region: Rect? = null,
    ): Match? {
        val frameMat = Mat()
        val tmplMatBase = Mat()
        Utils.bitmapToMat(frame, frameMat)
        Utils.bitmapToMat(template, tmplMatBase)
        Imgproc.cvtColor(frameMat, frameMat, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.cvtColor(tmplMatBase, tmplMatBase, Imgproc.COLOR_RGBA2GRAY)

        val searchMat = if (region != null) {
            val safe = Rect(
                region.left.coerceAtLeast(0),
                region.top.coerceAtLeast(0),
                region.right.coerceAtMost(frameMat.cols()),
                region.bottom.coerceAtMost(frameMat.rows()),
            )
            frameMat.submat(safe.top, safe.bottom, safe.left, safe.right)
        } else frameMat

        var best: Match? = null
        for (scale in scales) {
            val tmpl = Mat()
            val newW = (tmplMatBase.cols() * scale).toInt()
            val newH = (tmplMatBase.rows() * scale).toInt()
            if (newW < 8 || newH < 8) continue
            if (newW > searchMat.cols() || newH > searchMat.rows()) continue
            Imgproc.resize(tmplMatBase, tmpl, Size(newW.toDouble(), newH.toDouble()))

            val result = Mat()
            Imgproc.matchTemplate(searchMat, tmpl, result, Imgproc.TM_CCOEFF_NORMED)
            val mm = Core.minMaxLoc(result)
            val score = mm.maxVal
            if (score >= threshold && (best == null || score > best.score)) {
                val offX = (region?.left ?: 0)
                val offY = (region?.top ?: 0)
                val left = mm.maxLoc.x.toInt() + offX
                val top = mm.maxLoc.y.toInt() + offY
                best = Match(
                    rect = Rect(left, top, left + newW, top + newH),
                    score = score,
                    scale = scale,
                )
            }
            tmpl.release()
            result.release()
        }
        if (searchMat !== frameMat) searchMat.release()
        frameMat.release()
        tmplMatBase.release()
        return best
    }

    /** Returns whether two consecutive frames look identical (used to detect "page settled"). */
    fun framesEqual(a: Bitmap, b: Bitmap, threshold: Double = 0.995): Boolean {
        if (a.width != b.width || a.height != b.height) return false
        val ma = Mat(); val mb = Mat()
        Utils.bitmapToMat(a, ma); Utils.bitmapToMat(b, mb)
        Imgproc.cvtColor(ma, ma, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.cvtColor(mb, mb, Imgproc.COLOR_RGBA2GRAY)
        val result = Mat()
        Imgproc.matchTemplate(ma, mb, result, Imgproc.TM_CCOEFF_NORMED)
        val mean = MatOfDouble()
        val std = MatOfDouble()
        Core.meanStdDev(result, mean, std)
        val score = mean.toArray().firstOrNull() ?: 0.0
        ma.release(); mb.release(); result.release()
        return score >= threshold
    }
}
