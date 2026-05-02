package com.v26macro.runner.tasks

import com.v26macro.input.GestureService
import com.v26macro.runner.TaskContext
import com.v26macro.util.Logger
import com.v26macro.util.humanDelay

/**
 * 매크로 시작 시 V26을 메인 화면으로 끌어오는 단계.
 *
 * 우선순위:
 *   1. 이미 메인(`home/playball`)이면 그대로 통과
 *   2. V26 이 포그라운드 앱이면 (접근성으로 com.com2us.* 패키지 확인) 메인으로 BACK 만 눌러
 *      복귀 시도. 매크로 정지하고 다시 시작했을 때 게임을 굳이 닫고 다시 켜지 않게 해줌.
 *   3. 둘 다 아니면 LDPlayer 홈으로 나가서 `launcher/v26_icon` 을 찾아 실행.
 *
 * 필요한 템플릿:
 *  - assets/templates/launcher/v26_icon.png  → LDPlayer 홈의 V26 아이콘 (3 단계에서 사용)
 *  - assets/templates/home/playball.png      → 메인 화면의 '플레이볼' 메뉴 버튼
 */
object LaunchGame {

    suspend fun run(ctx: TaskContext, onProgress: (String) -> Unit): Boolean = with(ctx) {
        // 0) 첫 화면 캡처 대기 — 시작 직후엔 frame() 이 null 이라 매칭이 무조건 실패함
        val frameDeadline = System.currentTimeMillis() + 5000L
        while (frame() == null && System.currentTimeMillis() < frameDeadline) {
            kotlinx.coroutines.delay(150L)
        }
        if (frame() == null) {
            Logger.e("LaunchGame: 화면 캡처가 시작 안 됨 - MediaProjection 권한 확인")
            return@with false
        }

        // 디버그: 매크로 시작 시점의 프레임을 디스크에 한 장 저장. 사용자가 이 PNG 와
        // 자신의 템플릿을 비교해서 매칭이 왜 실패하는지 눈으로 확인할 수 있음.
        com.v26macro.capture.FrameProvider.dumpToDisk("launch_frame")

        // 1) 이미 메인이면 즉시 통과
        if (isOnMainMenu()) {
            onProgress("이미 V26 메인 화면")
            return@with true
        }

        // 2) V26 가 포그라운드면 그대로 통과. playball PNG 가 안 잡혀도(또는 캡처 안
        //    돼있어도) BACK 폭주 안 함. 이후 Task 가 자체적으로 메인으로 복귀 시도함.
        if (GestureService.isV26Foreground()) {
            onProgress("V26 동작 중 — 그대로 시작")
            if (find(BUCKET_HOME, "playball") == null) {
                Logger.w(
                    "isOnMainMenu 가 false 인데 V26 가 포그라운드. " +
                        "home/playball.png 가 없거나 매칭 실패. 일단 BACK 안 누르고 진행."
                )
            }
            return@with true
        }

        // 3) V26 가 백그라운드/종료 상태 → 런처에서 실행
        onProgress("LDPlayer 홈으로 나가는 중")
        if (!GestureService.pressHome()) {
            Logger.e("pressHome failed - 접근성 서비스가 켜져 있는지 확인")
            return@with false
        }
        humanDelay(1500L, 400L)

        onProgress("V26 아이콘 탐색 중")
        if (!tapTemplate("launcher", "v26_icon", timeoutMs = 8000L)) {
            Logger.e("v26_icon 템플릿을 찾지 못함 - launcher/v26_icon.png 가 필요합니다")
            return@with false
        }

        onProgress("V26 로딩 중...")
        // 메인 화면 진입 신호 = home/playball PNG 매칭. 다른 폴백 없음 (이전엔 V26
        // 포그라운드 25초로 메인이라고 가정했는데, 로딩/스플래시/공지 화면에서도
        // V26 가 포그라운드라 너무 일찍 진행해버림. playball 매칭만 신뢰하도록.)
        val maxWaitMs = 180_000L
        val startMs = System.currentTimeMillis()
        val deadline = startMs + maxWaitMs
        var arrived = false

        while (System.currentTimeMillis() < deadline) {
            val elapsed = (System.currentTimeMillis() - startMs) / 1000

            if (isOnMainMenu()) {
                Logger.i("LaunchGame: home/playball 매칭 (${elapsed}초) - 팝업 정착 대기")
                arrived = true

                // 메인이 잠깐 보이고 그 위로 팝업이 뜨는 케이스 대응:
                // 8초 동안 추가로 popup_close 감시. 뜰 때마다 닫고 메인 유지 확인.
                val settleEnd = System.currentTimeMillis() + 8_000L
                while (System.currentTimeMillis() < settleEnd) {
                    if (tapTemplate(BUCKET_HOME, "popup_close", timeoutMs = 600L)) {
                        onProgress("V26 메인 진입: 후속 팝업 닫음")
                        humanDelay(700L, 200L)
                    } else {
                        kotlinx.coroutines.delay(500L)
                    }
                }
                break
            }

            // 게임이 켜졌지만 이벤트 팝업/광고가 플레이볼을 가리고 있는 케이스 —
            // popup_close (X) 가 보이면 닫고 다시 체크.
            if (tapTemplate(BUCKET_HOME, "popup_close", timeoutMs = 800L)) {
                onProgress("V26 로딩 중... ${elapsed}s (팝업 닫음)")
                humanDelay(700L, 200L)
                continue
            }

            if (GestureService.isV26Foreground()) {
                onProgress("V26 로딩 중... ${elapsed}s (플레이볼 대기)")
            } else {
                onProgress("V26 로딩 중... ${elapsed}s (대기)")
            }

            kotlinx.coroutines.delay(800L)
        }

        if (!arrived) {
            Logger.e("V26 메인(플레이볼) 진입 타임아웃 - 공지/로그인이 막고 있거나 playball PNG 매칭 안 됨")
            return@with false
        }

        onProgress("V26 진입 완료")
        return@with true
    }
}
