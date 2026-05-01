package com.v26macro.runner.tasks

import com.v26macro.input.GestureService
import com.v26macro.runner.TaskContext
import com.v26macro.util.Logger
import com.v26macro.util.humanDelay

/**
 * 매크로 실행 직전에 LDPlayer 홈 화면으로 나간 뒤 V26 게임을 실행해 메인 화면까지 진입하는 단계.
 *
 * 동작:
 *  1. 이미 메인 화면에 있으면(home/playball 매칭) 곧바로 성공
 *  2. AccessibilityService.GLOBAL_ACTION_HOME 으로 LDPlayer 홈 표시
 *  3. launcher/v26_icon.png 템플릿을 찾아 탭 → 게임 실행
 *  4. home/playball.png 가 보일 때까지 최대 90초 대기 (스플래시·공지·로그인 처리 포함)
 *
 * 필요한 템플릿:
 *  - assets/templates/launcher/v26_icon.png  → LDPlayer 홈의 V26 아이콘
 *  - assets/templates/home/playball.png      → 메인 화면의 '플레이볼' 메뉴 버튼
 */
object LaunchGame {

    suspend fun run(ctx: TaskContext, onProgress: (String) -> Unit): Boolean = with(ctx) {
        // 1) 이미 메인이면 스킵
        if (isOnMainMenu()) {
            onProgress("이미 V26 메인 화면")
            return@with true
        }

        // 2) HOME 키로 LDPlayer 런처로
        onProgress("LDPlayer 홈으로 나가는 중")
        if (!GestureService.pressHome()) {
            Logger.e("pressHome failed - 접근성 서비스가 켜져 있는지 확인")
            return@with false
        }
        humanDelay(1500L, 400L)

        // 3) V26 아이콘 탭
        onProgress("V26 아이콘 탐색 중")
        if (!tapTemplate("launcher", "v26_icon", timeoutMs = 8000L)) {
            Logger.e("v26_icon 템플릿을 찾지 못함 - launcher/v26_icon.png 가 필요합니다")
            return@with false
        }

        // 4) 메인 화면(플레이볼 보임) 진입 대기 — 스플래시/공지/로그인이 있을 수 있어 90초
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
