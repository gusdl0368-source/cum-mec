package com.v26macro.capture

import android.graphics.Bitmap
import com.v26macro.V26MacroApp
import com.v26macro.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream

/**
 * Singleton frame bus. ScreenCaptureService pushes the latest Bitmap; consumers
 * (template matchers, OCR) read [latest] or collect [frames]. Bitmaps are reused
 * by the producer so consumers must NOT keep references past the next frame.
 */
object FrameProvider {
    private val _frames = MutableStateFlow<Bitmap?>(null)
    val frames: StateFlow<Bitmap?> = _frames.asStateFlow()

    val latest: Bitmap? get() = _frames.value

    var screenWidth: Int = 0
        private set
    var screenHeight: Int = 0
        private set

    fun publish(bitmap: Bitmap) {
        screenWidth = bitmap.width
        screenHeight = bitmap.height
        _frames.value = bitmap
    }

    fun clear() {
        _frames.value = null
    }

    /**
     * 디버그용 — 현재 프레임을 PNG 로 저장해서 사용자가 캡처툴에서 만든 템플릿과
     * 픽셀 비교해볼 수 있게 한다. 경로: Android/data/com.v26macro/files/debug/<name>.png
     */
    fun dumpToDisk(name: String): String? {
        val bmp = latest ?: return null
        return try {
            val dir = File(V26MacroApp.instance.getExternalFilesDir(null), "debug").apply { mkdirs() }
            val out = File(dir, "$name.png")
            FileOutputStream(out).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            Logger.i("FrameProvider: 디버그 프레임 저장 ${out.absolutePath} (${bmp.width}x${bmp.height})")
            out.absolutePath
        } catch (e: Exception) {
            Logger.w("FrameProvider.dumpToDisk 실패: ${e.message}")
            null
        }
    }
}
