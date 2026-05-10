package com.v26macro.v2.trace

import android.graphics.Bitmap
import android.graphics.Point

/**
 * 트레이스 레코더 — 사용자가 손으로 게임 진행하는 동안 매크로가 옆에서 기록.
 *
 * 기록 내용 (1초 간격):
 *   - 화면 PNG
 *   - 그 시점 OCR 결과 (JSON)
 *   - 사용자 탭 좌표 + 그 위치의 OCR 텍스트
 *
 * 결과는 traces/<task>-<timestamp>/ 폴더로. Claude 가 그 폴더만 받아서 YAML 초안 자동 생성.
 *
 * Phase 4 에서 구현. AccessibilityService 에서 탭 이벤트 수집 가능하지만 현재 우리
 * 서비스는 탭을 '주입' 하는 쪽이라 외부 탭 인식엔 추가 설정 필요.
 * 대안: 시스템 dumpsys input + adb getevent 로 PC 사이드에서 기록 후 폴더 푸시.
 */
class TraceRecorder {
    fun start(@Suppress("UNUSED_PARAMETER") taskName: String) {
        TODO("Phase 4: 캡처 + OCR 1Hz 기록 + tap 이벤트 (또는 AccessibilityEvent) 수집")
    }

    fun stop() {
        TODO("Phase 4: 폴더 마무리 + 디스코드로 푸시 옵션")
    }

    fun recordTap(@Suppress("UNUSED_PARAMETER") screen: Bitmap, @Suppress("UNUSED_PARAMETER") point: Point) {
        TODO("Phase 4")
    }
}
