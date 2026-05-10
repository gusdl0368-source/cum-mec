package com.v26macro.v2.permissions

import android.content.Intent

/**
 * MediaProjection 동의 결과를 보관. ScreenCaptureService 에서 꺼내 씀.
 *
 * Phase 1 단계에선 자리만 잡아둠 — Phase 2 에서 ScreenCaptureService 가 이 데이터로
 * MediaProjection 인스턴스 생성.
 */
object MediaProjectionHolder {
    @Volatile
    var resultData: Intent? = null
        private set

    @Volatile
    var resultCode: Int = 0
        private set

    val hasProjection: Boolean
        get() = resultData != null && resultCode != 0

    fun set(code: Int, data: Intent) {
        resultCode = code
        resultData = data
    }

    fun clear() {
        resultCode = 0
        resultData = null
    }
}
