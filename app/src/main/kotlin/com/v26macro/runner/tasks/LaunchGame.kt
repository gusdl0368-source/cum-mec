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
        // 1) 이미 메인이면 즉시 통과
        if (isOnMainMenu()) {
            onProgress("이미 V26 메인 화면")
            return@with true
        }

        // 2) V26 포그라운드 → BACK 으로 메인 복귀 시도
        if (GestureService.isV26Foreground()) {
            onProgress("V26 동작 중 - 메인으로 복귀")
            if (returnToMainMenu(maxBack = 8)) {
                onProgress("메인 복귀 완료")
                return@with true
            }
            // 복귀 실패 (게임 깊숙이 들어간 상태일 수도) → 런처에서 재실행 fallback
            Logger.w("returnToMainMenu 실패 - 런처에서 재실행 시도")
        }

        // 3) 백그라운드 또는 종료된 상태 → 런처에서 실행
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
        val arrived = waitForTemplate(
            BUCKET_HOME, "playball",
            timeoutMs = 90_000L,
        ) != null
        if (!arrived) {
            Logger.e("V26 메인(플레이볼) 진입 타임아웃 - 공지/로그인 화면이 막고 있을 수 있음")
            return@with false
        }

        onProgress("V26 메인 화면 진입 완료")
        return@with true
    }
}
