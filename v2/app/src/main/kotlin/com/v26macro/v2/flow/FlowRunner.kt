package com.v26macro.v2.flow

/**
 * Flow 한 개를 실행. 매 단계마다:
 *   1. 시작 시각 + 단계 정의 로그
 *   2. 필요한 캡처 / OCR / 매칭 호출
 *   3. tap/swipe/back 호출
 *   4. 성공/실패/타임아웃 기록
 *   5. 실패 시 자동 스크린샷 + 실패 갤러리 적재
 *   6. 마지막 수단으로 LlmFallback (있으면)
 *
 * Phase 3 에서 채움.
 */
class FlowRunner {
    sealed class Result {
        object Success : Result()
        data class Failed(val reason: String, val stepIndex: Int) : Result()
    }

    suspend fun run(@Suppress("UNUSED_PARAMETER") flow: Flow): Result {
        TODO("Phase 3: Step 패턴매칭 + 매크로 디스패치 + 실패 캡처")
    }
}

/**
 * Named macros — 코틀린에서 정의된 복잡 동작.
 * Phase 3 에서 등록 시작:
 *   - back_to_main : 메인('플레이볼') 까지 BACK + exit_cancel 자동 처리
 *   - dismiss_popups : 알려진 팝업 X 닫기
 *   - wait_loading_done : '로딩' 사라질 때까지
 */
object NamedMacros {
    val all: Map<String, suspend () -> Unit> = emptyMap()
}
