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
 * 흐름:
 *   1. 플레이볼 메뉴 → '리그 모드' 배너 (entry)
 *   2. 리그 메인 → 'PLAY BALL' 버튼
 *   3. SELECT TYPE 화면 → '풀 플레이' 카드 → 'START'
 *   4. 로딩/인트로 → 마이크 탭으로 스킵
 *   5. 인게임 — 매 폴링마다 다음을 처리:
 *      a. 라이브 플레이 5/5 체크 차면 → 일시정지 → 시뮬 전환 → 확인 (시뮬 모드)
 *      b. 코치 조언(선발 교체 추천) 다이얼로그 뜨면 → '교체' 탭
 *      c. 그 외엔 마이크 / OUTS 영역 / 포수 리드 버튼을 보이는 대로 탭
 *   6. 시뮬 모드로 전환 후엔 게임 종료까지 개입 불가 → 종료 신호 대기 (추후)
 *
 * 가정:
 *   - 게임 설정에서 타격 OFF / 투구 ON 으로 미리 설정해둘 것 (매크로가 토글하지 않음)
 *
 * 종료 처리는 사용자 추가 흐름 받은 뒤 정의. 지금은 15분 안전 한도.
 *
 * 필요한 템플릿:
 *   leaguemode/{entry, play_ball_button, select_type_header, full_play_card,
 *               start_button, mic_button, outs_area, catcher_lead,
 *               live_play_full, pause_button, switch_to_sim, sim_confirm,
 *               coach_advice, coach_change_yes}
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
     * 게임 끝날 때까지(또는 안전 한도까지) 인게임 이벤트 폴링 + 반복 탭.
     *
     * 폴링 주기마다 우선순위 순으로 처리:
     *   1. 라이브 플레이 5/5 → 시뮬 전환 (한 번만, 이후 시뮬 모드)
     *   2. 코치 조언(투수 교체 추천) → '교체' 탭
     *   3. 일반 인게임 — 마이크 / OUTS / 포수 리드 보이면 탭 (시뮬 모드 진입 후엔 마이크만)
     *
     * 게임 종료 신호는 추후 사용자 흐름 받은 뒤 추가 (현재는 15분 한도).
     */
    private suspend fun playUntilEnd(ctx: TaskContext) = with(ctx) {
        val deadline = System.currentTimeMillis() + maxGameMs
        var taps = 0
        var inSimulation = false

        while (System.currentTimeMillis() < deadline) {
            if (!inSimulation) {
                // 1) 라이브 플레이 5/5 → 시뮬 전환
                if (find(bucket, "live_play_full") != null) {
                    progress("리그모드: 라이브 5/5 감지 - 시뮬 전환 시도")
                    if (switchToSimulation(this)) {
                        inSimulation = true
                        progress("리그모드: 시뮬레이션 모드 - 종료 대기")
                        continue
                    }
                }

                // 2) 코치 조언 - 선발 교체 추천
                if (find(bucket, "coach_advice") != null) {
                    progress("리그모드: 투수 교체 추천 → 교체")
                    tapTemplate(bucket, "coach_change_yes", timeoutMs = 2500L)
                    humanDelay(700L, 200L)
                    continue
                }

                // 3) 일반 인게임 탭들
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
            } else {
                // 시뮬 모드 - 개입 불가. 마이크만 가끔 눌러서 화면 가려진 거 닫기.
                find(bucket, "mic_button")?.let {
                    GestureService.tap(it.centerX.toFloat(), it.centerY.toFloat())
                }
                // TODO: 게임 종료 신호 감지 (사용자 추가 흐름)
            }
            delay(450L)
        }
        val mode = if (inSimulation) "(시뮬 진행 후)" else ""
        progress("리그모드: 안전 한도 도달 ${mode}, ${taps}회 탭 후 종료")
    }

    /**
     * 일시정지 → 시뮬레이션 전환 → 확인 흐름. 한 단계라도 실패하면 false.
     * 실패 시 계속 인게임 모드로 남고, 다음 라이브 5/5 감지 때 다시 시도.
     */
    private suspend fun switchToSimulation(ctx: TaskContext): Boolean = with(ctx) {
        if (!tapTemplate(bucket, "pause_button", timeoutMs = 3000L)) {
            progress("시뮬 전환: 일시정지 버튼 못 찾음")
            return@with false
        }
        humanDelay(700L, 200L)
        if (!tapTemplate(bucket, "switch_to_sim", timeoutMs = 4000L)) {
            progress("시뮬 전환: '시뮬레이션 전환' 카드 못 찾음")
            // 일시정지 메뉴는 떴는데 카드 못 찾았으면 BACK 으로 메뉴 닫기
            tapBack()
            return@with false
        }
        humanDelay(700L, 200L)
        if (!tapTemplate(bucket, "sim_confirm", timeoutMs = 4000L)) {
            progress("시뮬 전환: 확인 다이얼로그 못 찾음")
            return@with false
        }
        humanDelay(1200L, 300L)
        return@with true
    }
}
