package com.v26macro.v2.vision

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * OpenCV 템플릿 매칭 — 텍스트 없는 아이콘 (마이크/일시정지/+) 만 처리.
 * Phase 2 끝/Phase 3 초에 채움. OpenCV 의존성 무거워서 우선 stub.
 */
data class TemplateMatch(
    val name: String,
    val box: Rect,
    val score: Float,
)

class TemplateMatcher {
    fun match(
        @Suppress("UNUSED_PARAMETER") frame: Bitmap,
        @Suppress("UNUSED_PARAMETER") templateName: String,
        @Suppress("UNUSED_PARAMETER") threshold: Float = 0.85f,
    ): TemplateMatch? {
        TODO("Phase 2/3: OpenCV matchTemplate (TM_CCOEFF_NORMED) + 다중 스케일")
    }
}
