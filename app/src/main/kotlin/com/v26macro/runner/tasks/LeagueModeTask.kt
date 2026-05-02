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
 * 한 게임 흐름:
 *   1. (첫 게임만) 플레이볼 → 리그 메인 → PLAY BALL → SELECT TYPE → 풀 플레이 → START
 *   2. 인게임 폴링: 라이브 5/5 → 시뮬 전환 / 코치 조언 → 교체 / 그 외 → 마이크/OUTS/포수리드
 *   3. 게임 종료 (result_next 또는 play_again 보임)
 *   4. 결과 → MVP/보상 → 다음매치 화면 순서로 '다음' 누름
 *   5. 다음매치 화면의 '한 번 더 하기' 누르면 다음 게임 자동 시작 → 1번으로 (단 첫 진입 단계는 스킵)
 *
 * 종료 조건:
 *   - 다음 매치의 '한 번 더 하기' 가 더 이상 안 보이면 (일일 한도 등) → 종료
 *   - 안전 상한 (maxGames) 도달 → 종료
 *
 * 가정:
 *   - 게임 설정에서 타격 OFF / 투구 ON 으로 미리 설정해둘 것
 */
class LeagueModeTask : Task {
    override val kind = TaskKind.LeagueMode
    private val bucket = kind.bucket

    private val maxGameMs = 15 * 60 * 1000L     // 한 게임 최대 15분
    private val postGameTimeoutMs = 90_000L     // 결과 → 다음매치 처리 1.5분
    private val maxGames = 30                   // 안전 상한 (자연 종료 우선)

    override suspend fun run(ctx: TaskContext): TaskResult = with(ctx) {
        progress("리그모드 시작")

        // ── 첫 진입: 플레이볼 → 리그 메인 → PLAY BALL → SELECT TYPE → 풀 플레이 → START ──
        if (!enterPlayballAndOpen(bucket, "entry")) {
            return missingAssets("home/playball or $bucket/entry")
        }
        humanDelay(900L, 200L)

        if (!tapTemplate(bucket, "play_ball_button", timeoutMs = 6000L)) {
            tapBack(); returnToMainMenu()
            return missingAssets("$bucket/play_ball_button")
        }
        humanDelay(900L, 200L)

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

        // ── 다중 게임 루프 ──
        var gamesPlayed = 0
        for (gameNum in 1..maxGames) {
            progress("리그모드: ${gameNum}경기 진행")

            // 한 게임 진행 (인게임 자동 처리)
            playOneGame(this)
            gamesPlayed++

            // 결과 → MVP/보상 → 다음매치 → 한 번 더 하기
            val continued = handlePostGame(this)
            if (!continued) {
                progress("리그모드: 더 진행 못함 (일일 한도 또는 화면 못 찾음)")
                break
            }
            // continued=true 면 다음 게임 자동 시작됨, 다음 iter 에서 playOneGame 재진입
        }

        returnToMainMenu()
        progress("리그모드 완료: ${gamesPlayed}경기")
        return TaskResult.Success
    }

    /**
     * 한 게임을 자동 진행. 결과 화면이 뜨면(result_next 또는 play_again 보임) 즉시 반환.
     *
     * 폴링 우선순위:
     *   1. 게임 종료 신호 → return
     *   2. 라이브 5/5 → 시뮬 전환
     *   3. 코치 조언 → 교체
     *   4. 일반 인게임 탭 (마이크/OUTS/포수리드)
     */
    private suspend fun playOneGame(ctx: TaskContext) = with(ctx) {
        val deadline = System.currentTimeMillis() + maxGameMs
        var taps = 0
        var inSimulation = false

        while (System.currentTimeMillis() < deadline) {
            // 게임 종료 신호 체크
            if (find(bucket, "result_next") != null || find(bucket, "play_again") != null) {
                progress("리그모드: 게임 종료 화면 감지")
                return@with
            }

            if (!inSimulation) {
                // 1) 라이브 플레이 5/5 → 시뮬 전환
                if (find(bucket, "live_play_full") != null) {
                    progress("리그모드: 라이브 5/5 감지 - 시뮬 전환 시도")
                    if (switchToSimulation(this)) {
                        inSimulation = true
                        progress("리그모드: 시뮬레이션 모드")
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
                    GestureService.tap(it.centerX.toFloat(), it.centerY.toFloat()); taps++
                }
                find(bucket, "outs_area")?.let {
                    GestureService.tap(it.centerX.toFloat(), it.centerY.toFloat()); taps++
                }
                find(bucket, "mic_button")?.let {
                    GestureService.tap(it.centerX.toFloat(), it.centerY.toFloat()); taps++
                }
            } else {
                // 시뮬 모드 - 마이크만 가끔
                find(bucket, "mic_button")?.let {
                    GestureService.tap(it.centerX.toFloat(), it.centerY.toFloat())
                }
            }
            delay(450L)
        }
        progress("리그모드: 게임 안전 한도 도달 (${taps}회 탭)")
    }

    /**
     * 게임 종료 후 후속 화면들 자동 처리:
     *   - 다음매치 화면(play_again 보임) → '한 번 더 하기' 탭 → true 반환 (다음 게임 시작)
     *   - 결과/MVP 화면(result_next 보임) → '다음' 탭 → 다음 화면 대기 (계속 루프)
     *   - 90초 안에 처리 못하면 false (자연 종료)
     */
    private suspend fun handlePostGame(ctx: TaskContext): Boolean = with(ctx) {
        val deadline = System.currentTimeMillis() + postGameTimeoutMs
        while (System.currentTimeMillis() < deadline) {
            // 다음 매치 화면 → 한 번 더 하기로 다음 게임 시작
            if (find(bucket, "play_again") != null) {
                progress("리그모드: 한 번 더 하기 → 다음 게임")
                tapTemplate(bucket, "play_again", timeoutMs = 3000L)
                humanDelay(2500L, 500L)  // 다음 게임 로딩 여유
                return@with true
            }
            // 결과 / MVP 화면 → 다음 탭
            if (tapTemplate(bucket, "result_next", timeoutMs = 1500L)) {
                humanDelay(900L, 250L)
                continue
            }
            delay(500L)
        }
        return@with false
    }

    /**
     * 일시정지 → 시뮬레이션 전환 → 확인. 실패 시 false.
     */
    private suspend fun switchToSimulation(ctx: TaskContext): Boolean = with(ctx) {
        if (!tapTemplate(bucket, "pause_button", timeoutMs = 3000L)) {
            progress("시뮬 전환: 일시정지 버튼 못 찾음")
            return@with false
        }
        humanDelay(700L, 200L)
        if (!tapTemplate(bucket, "switch_to_sim", timeoutMs = 4000L)) {
            progress("시뮬 전환: '시뮬레이션 전환' 카드 못 찾음")
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
