package com.v26macro.capture

import android.graphics.Bitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
}
