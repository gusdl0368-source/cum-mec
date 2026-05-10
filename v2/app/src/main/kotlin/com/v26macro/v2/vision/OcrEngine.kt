package com.v26macro.v2.vision

import android.graphics.Bitmap
import android.graphics.Rect

/**
 * MLKit 한국어 OCR 래퍼. Phase 2 에서 채움.
 *
 * 핵심 책임:
 *   - bitmap 한 장에서 텍스트 블록 추출
 *   - findText("X") — fuzzy 매칭 (공백 정규화, 부분 포함)
 *   - region/occurrence/threshold 옵션 지원
 *   - confidence 가 낮은 매치는 제외 (기본 0.7)
 */
data class OcrBlock(
    val text: String,
    val box: Rect,
    val confidence: Float,
)

class OcrEngine {
    suspend fun recognize(@Suppress("UNUSED_PARAMETER") bitmap: Bitmap): List<OcrBlock> {
        TODO("Phase 2: MLKit text-recognition-korean 호출")
    }

    fun findText(
        @Suppress("UNUSED_PARAMETER") blocks: List<OcrBlock>,
        @Suppress("UNUSED_PARAMETER") query: String,
        @Suppress("UNUSED_PARAMETER") region: Rect? = null,
        @Suppress("UNUSED_PARAMETER") occurrence: Int = 1,
        @Suppress("UNUSED_PARAMETER") threshold: Float = 0.7f,
    ): OcrBlock? {
        TODO("Phase 2: 공백 무시 정규화 + 부분 포함 + region 필터 + n번째 매칭")
    }
}
