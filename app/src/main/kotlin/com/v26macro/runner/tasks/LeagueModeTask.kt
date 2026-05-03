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
    // 게임 수 상한 없음 — 자연 종료 (play_again 안 보임 = 일일 한도 도달) 까지 무한 반복

    override suspend fun run(ctx: TaskContext): TaskResult = with(ctx) {
        progress("리그모드 시작")

        // ── 첫 진입: 플레이볼 → 리그 메인 → PLAY BALL → SELECT TYPE → 풀 플레이 → START ──
        // 단, 이전에 경기 중 종료한 적이 있으면 '리섬 플레이' 다이얼로그가 뜸 → 이어하기로 재개.
        if (!enterPlayballAndOpen(bucket, "entry")) {
            return missingAssets("home/playball or $bucket/entry")
        }
        humanDelay(900L, 200L)

        // 리섬 플레이(이어하기) 다이얼로그 — 리그 대시보드 진입 직후 자동으로 뜨거나
        // PLAY BALL 누른 직후 뜸. 양쪽 케이스 모두 커버하기 위해 두 번 체크한다.
        var resumed = handleResumePlayIfPresent(this)

        if (!resumed) {
            if (!tapTemplate(bucket, "play_ball_button", timeoutMs = 6000L)) {
                // PLAY BALL 안 보이고 리섬도 아직 안 떴을 수도 있음 — 한 번 더 확인
                if (handleResumePlayIfPresent(this)) {
                    resumed = true
                } else {
                    tapBack(); returnToMainMenu()
                    return missingAssets("$bucket/play_ball_button")
                }
            } else {
                humanDelay(900L, 200L)
                // PLAY BALL 누른 뒤에 리섬 다이얼로그가 뜨는 케이스
                if (handleResumePlayIfPresent(this)) {
                    resumed = true
                }
            }
        }

        if (!resumed) {
            // 정상 흐름: SELECT TYPE → 풀 플레이 → START
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
        } else {
            progress("리그모드: 이전 경기 이어하기로 재개됨")
            humanDelay(1500L, 300L)
        }

        // ── 다중 게임 루프 (자연 종료 시까지 무한) ──
        var gamesPlayed = 0
        var gameNum = 0
        while (true) {
            gameNum++
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
            // 게임 종료 신호 체크 — 결과 화면 또는 MVP 화면 또는 다음매치 화면
            if (find(bucket, "result_next") != null
                || find(bucket, "mvp_next") != null
                || find(bucket, "play_again") != null
            ) {
                progress("리그모드: 게임 종료 화면 감지")
                return@with
            }

            if (!inSimulation) {
                // 1) 라이브 플레이 5/5 → 시뮬 전환
                // 0.9 임계값 강제 — 5개 체크 패턴이 1/5, 2/5 상태와 구별돼야 하는데
                // 기본 임계값(0.75)으론 부분 매칭도 통과해버림.
                if (find(bucket, "live_play_full", threshold = 0.9) != null) {
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
                // 시뮬 모드 - 개입 불가 상태지만 마이크 / OUTS 영역은 계속 탭해서
                // 해설 팝업이나 진행 애니메이션이 빨리 넘어가도록 한다.
                find(bucket, "mic_button")?.let {
                    GestureService.tap(it.centerX.toFloat(), it.centerY.toFloat())
                }
                find(bucket, "outs_area")?.let {
                    GestureService.tap(it.centerX.toFloat(), it.centerY.toFloat())
                }
            }
            // 루프 간격을 짧게(150ms) 둬서 초당 약 6회 폴링 — 마이크/OUTS/포수리드를
            // 빠르게 연타해 게임 진행을 가속.
            delay(150L)
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
            // 경기 결과 화면 (WIN/LOSE) → 다음
            if (tapTemplate(bucket, "result_next", timeoutMs = 1200L)) {
                humanDelay(900L, 250L)
                continue
            }
            // MVP / 보상 화면 → 다음 (또는 SKIP)
            if (tapTemplate(bucket, "mvp_next", timeoutMs = 1200L)) {
                humanDelay(900L, 250L)
                continue
            }
            delay(500L)
        }
        return@with false
    }

    /**
     * 리그 대시보드 진입 또는 PLAY BALL 탭 직후에 '리섬 플레이' 다이얼로그가 떠있는지
     * 확인하고, 떠있으면 '이어하기' 버튼 탭. true 면 SELECT TYPE 흐름 스킵하고 바로
     * 인게임으로 진행.
     *
     * 다이얼로그 헤더(resume_play_dialog) 와 버튼(resume_play_yes) 둘 다 선택 템플릿이라
     * 둘 다 정의 안 됐으면 그냥 false 반환 (정상 흐름 진행).
     */
    private suspend fun handleResumePlayIfPresent(ctx: TaskContext): Boolean = with(ctx) {
        val dialog = find(bucket, "resume_play_dialog") ?: return@with false
        progress("리그모드: 리섬 플레이 다이얼로그 감지 → 이어하기")
        // 1차: 명시적으로 정의된 '이어하기' 버튼
        if (tapTemplate(bucket, "resume_play_yes", timeoutMs = 2500L)) {
            humanDelay(1200L, 300L)
            return@with true
        }
        // 2차 안전망: 다이얼로그 중심 살짝 아래 (보통 '예/이어하기' 버튼 위치)
        progress("리그모드: resume_play_yes 템플릿 없음 — 다이얼로그 중심 추정 탭")
        tap(dialog.centerX, dialog.centerY + 120)
        humanDelay(1200L, 300L)
        return@with true
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
