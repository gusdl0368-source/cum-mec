package com.v26macro.runner.tasks

import com.v26macro.input.GestureService
import com.v26macro.runner.TaskContext
import com.v26macro.runner.TaskKind
import com.v26macro.runner.TaskResult
import com.v26macro.util.humanDelay
import kotlinx.coroutines.delay

/**
 * 리그모드.
 *
 * 흐름 (1차 구현 — 게임 종료 처리는 사용자 추가 흐름 받은 뒤):
 *   1. 플레이볼 메뉴 → '리그 모드' 배너 (entry)
 *   2. 리그 메인 → 'PLAY BALL' 버튼
 *   3. SELECT TYPE 화면 → '풀 플레이' 카드 → 'START'
 *   4. 로딩/인트로 → 마이크 탭으로 스킵
 *   5. 인게임 — 마이크 / OUTS 영역 / 포수 리드 버튼이 보일 때마다 계속 탭
 *
 * 인게임 가정:
 *   - 타격 OFF + 투구 ON 상태 (매크로가 검증/토글하지 않음, 사용자가 게임 설정에서 설정해둘 것)
 *   - 포수 리드 = 자동 투구 위임. 매크로는 그냥 계속 누름.
 *   - OUTS / 마이크는 애니/해설 스킵 용도로 추정.
 *
 * 종료 처리는 다음 단계에서 추가. 지금은 15분 안전 한도 후 메인 복귀.
 *
 * 필요한 템플릿:
 *   leaguemode/{entry, play_ball_button, select_type_header, full_play_card,
 *               start_button, mic_button, outs_area, catcher_lead}
 */
class LeagueModeTask : Task {
    override val kind = TaskKind.LeagueMode
    private val bucket = kind.bucket

    private val maxGameMs = 15 * 60 * 1000L  // 15분 안전 한도

    override suspend fun run(ctx: TaskContext): TaskResult = with(ctx) {
        progress("리그모드 시작")

        // 1) 플레이볼 → 리그모드 배너
        if (!enterPlayballAndOpen(bucket, "entry")) {
            return missingAssets("home/playball or $bucket/entry")
        }
        humanDelay(900L, 200L)

        // 2) 리그 메인 → PLAY BALL
        if (!tapTemplate(bucket, "play_ball_button", timeoutMs = 6000L)) {
            tapBack(); returnToMainMenu()
            return missingAssets("$bucket/play_ball_button")
        }
        humanDelay(900L, 200L)

        // 3) SELECT TYPE → 풀 플레이 → START
        if (waitForTemplate(bucket, "select_type_header", timeoutMs = 8000L) == null) {
            tapBack(); returnToMainMenu()
            return TaskResult.Failed("SELECT TYPE 화면 미진입")
        }
        if (!tapTemplate(bucket, "full_play_card", timeoutMs = 4000L)) {
            tapBack(); returnToMainMenu()
            return missingAssets("$bucket/full_play_card")
        }
        humanDelay(700L, 200L)
        if (!tapTemplate(bucket, "start_button", timeoutMs = 4000L)) {
            tapBack(); returnToMainMenu()
            return missingAssets("$bucket/start_button")
        }
        humanDelay(1500L, 300L)

        // 4) 인게임 - 마이크/OUTS/포수리드 반복 탭
        progress("리그모드: 게임 진행 중 (자동 탭)")
        playUntilEnd(this)

        // 5) 종료 처리 (추후) - 일단 메인으로 복귀
        returnToMainMenu()
        progress("리그모드 완료")
        return TaskResult.Success
    }

    /**
     * 게임이 끝날 때까지(또는 안전 한도까지) 마이크/OUTS/포수리드를 빠르게 반복 탭.
     *
     * `tap()` 의 humanDelay 를 우회해서 GestureService.tap 을 직접 호출 — 빠른 폴링.
     * 게임 종료 신호는 추후 사용자 흐름 받은 뒤 추가 (현재는 15분 한도로 강제 종료).
     */
    private suspend fun playUntilEnd(ctx: TaskContext) = with(ctx) {
        val deadline = System.currentTimeMillis() + maxGameMs
        var taps = 0
        while (System.currentTimeMillis() < deadline) {
            // 보이는 것만 빠르게 탭
            find(bucket, "catcher_lead")?.let {
                GestureService.tap(it.centerX.toFloat(), it.centerY.toFloat())
                taps++
            }
            find(bucket, "outs_area")?.let {
                GestureService.tap(it.centerX.toFloat(), it.centerY.toFloat())
                taps++
            }
            find(bucket, "mic_button")?.let {
                GestureService.tap(it.centerX.toFloat(), it.centerY.toFloat())
                taps++
            }
            // TODO: 게임 종료 신호 감지 (예: result_screen 식별자)
            delay(450L)
        }
        progress("리그모드: 안전 한도 도달, ${taps}회 탭 후 종료")
    }
}
