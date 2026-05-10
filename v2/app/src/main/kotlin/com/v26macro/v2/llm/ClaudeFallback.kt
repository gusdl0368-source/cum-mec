package com.v26macro.v2.llm

import android.graphics.Bitmap
import android.graphics.Point

/**
 * OCR + 템플릿 매칭 둘 다 막혔을 때 호출되는 마지막 수단.
 *
 * 동작:
 *   1. 현재 화면 비트맵 + 의도(intent: "결과 화면의 '다음' 버튼") 를 Claude API 로 보냄
 *   2. Claude Haiku 4.5 가 좌표 (x, y) 응답
 *   3. 그 좌표 탭 후 다음 단계 진행
 *
 * 비용: 회당 ~₩30. config.yaml 에 ANTHROPIC_API_KEY 있을 때만 활성화.
 * Phase 4 에서 구현.
 */
class ClaudeFallback(
    @Suppress("UNUSED_PARAMETER") private val apiKey: String,
) {
    suspend fun locate(
        @Suppress("UNUSED_PARAMETER") screen: Bitmap,
        @Suppress("UNUSED_PARAMETER") intent: String,
    ): Point? {
        TODO("Phase 4: Anthropic Messages API + claude-haiku-4-5 + image content + JSON tool result")
    }
}
