package com.v26macro.runner.tasks

import com.v26macro.runner.TaskContext
import com.v26macro.runner.TaskKind
import com.v26macro.runner.TaskResult
import com.v26macro.util.humanDelay

/**
 * 스페셜매치 — 잠재력 60오버롤 매치 자동.
 *
 * 흐름:
 *   1. (메인) 플레이볼 → 스페셜매치
 *   2. '잠재력' 탭
 *   3. 카드 카루셀에서 60 OVR 카드 보일 때까지 좌측 화살표(←) 반복 탭
 *   4. 60 매치 화면 START (1차)
 *   5. SELECT TYPE 화면 진입 대기
 *   6. (라운드마다) 랜덤픽 카드 → 직접플레이 OFF → 게이지 좌측 끝 → START(최종)
 *   7. 결과창 → 다음 → 한 번 더 / 확인
 *   8. 메인으로 BACK
 *
 * 게이지를 좌측으로 두면 최소 볼/포인트로 진행, 직접플레이 OFF 면 시뮬레이션 자동 진행.
 *
 * 필요한 템플릿: specialmatch/{entry, jamjeryeok_tab, carousel_left, match_60ovr,
 *                match_start1, select_type_header, random_pick_card, gauge_left,
 *                start2, result_next, confirm}
 *               (선택) direct_play_on, play_again
 */
class SpecialMatchTask(
    private val maxRounds: Int = 5,         // 사용자가 UI 에서 정한 매치 수 (0 이면 MacroRunner 가 스킵)
) : Task {
    override val kind = TaskKind.SpecialMatch
    private val bucket = kind.bucket

    private val matchTimeoutMs = 240_000L  // 자동 진행이 길어질 수 있어 4분

    override suspend fun run(ctx: TaskContext): TaskResult = with(ctx) {
        progress("스페셜매치 시작")

        // 1) 플레이볼 → 스페셜매치
        if (!enterPlayballAndOpen(bucket, "entry")) {
            return missingAssets("home/playball or $bucket/entry")
        }

        // 2) 잠재력 탭
        if (!tapTemplate(bucket, "jamjeryeok_tab", timeoutMs = 5000L)) {
            tapBack(); returnToMainMenu()
            return missingAssets("$bucket/jamjeryeok_tab")
        }
        humanDelay()

        // 3) 카루셀에서 60 OVR 카드 찾을 때까지 ← 화살표 반복
        var found = find(bucket, "match_60ovr") != null
        if (!found) {
            repeat(8) {
                if (find(bucket, "match_60ovr") != null) {
                    found = true
                    return@repeat
                }
                tapTemplate(bucket, "carousel_left", timeoutMs = 1500L)
                humanDelay(700L, 200L)
            }
        }
        if (!found) {
            tapBack(); returnToMainMenu()
            return TaskResult.Failed("60 OVR 카드를 찾지 못함 — match_60ovr/carousel_left 템플릿 확인")
        }

        // 4) 60 매치 START (1차)
        if (!tapTemplate(bucket, "match_start1", timeoutMs = 5000L)) {
            tapBack(); returnToMainMenu()
            return missingAssets("$bucket/match_start1")
        }
        humanDelay(900L, 250L)

        // 5~7) 라운드 루프
        var played = 0
        for (round in 1..maxRounds) {
            // SELECT TYPE 화면 — 1차에는 반드시 와야 함. 2라운드 이후엔 '한 번 더'
            // 선택에 따라 다시 올 수도 있고, 바로 매치로 갈 수도 있어 짧게만 대기.
            val onSelectType = waitForTemplate(
                bucket, "select_type_header",
                timeoutMs = if (round == 1) 10_000L else 5_000L,
            ) != null

            if (onSelectType) {
                // 5-1) 랜덤픽 플레이 카드
                //   캡처 도구에서 좌표 + 가드=select_type_header 로 저장하면
                //   "SELECT TYPE 화면일 때만 카드 위치 탭" 으로 동작.
                tapTemplate(bucket, "random_pick_card", timeoutMs = 4000L)
                humanDelay()

                // 5-2) 직접 플레이가 ON 상태면 끄기 (direct_play_on 매칭될 때만)
                val onState = find(bucket, "direct_play_on")
                if (onState != null) {
                    tap(onState.centerX, onState.centerY)
                    humanDelay()
                }

                // 5-3) 게이지를 좌측 끝으로 (최소 볼 사용)
                //   좌표 + 가드=select_type_header 추천.
                tapTemplate(bucket, "gauge_left", timeoutMs = 3000L)
                humanDelay()

                // 5-4) 최종 START
                //   좌표 + 가드=direct_play_off 추천. 직접플레이가 확실히 OFF 일 때만
                //   START 가 눌려서 실수로 직접 플레이 모드로 시작되는 사고 방지.
                if (!tapTemplate(bucket, "start2", timeoutMs = 5000L)) {
                    progress("스페셜매치: start2 실패 (직접플레이 OFF 가드 미충족 가능)")
                    break
                }
            } else if (round == 1) {
                progress("스페셜매치: SELECT TYPE 화면 미진입")
                break
            }
            // round >= 2 에서 SELECT TYPE 안 보이면 = 바로 매치 진행 중이라 가정

            // 6) 결과 대기
            val ok = waitForTemplate(bucket, "result_next", timeoutMs = matchTimeoutMs) != null
            if (!ok) {
                progress("스페셜매치: 결과 타임아웃")
                break
            }
            tapTemplate(bucket, "result_next", timeoutMs = 3000L)
            played++
            humanDelay(700L, 200L)

            // 7) 마지막이면 확인, 아니면 한 번 더
            val isLast = round >= maxRounds
            val again = if (!isLast) tapTemplate(bucket, "play_again", timeoutMs = 3000L) else false
            if (!again) {
                tapTemplate(bucket, "confirm", timeoutMs = 4000L)
                dismissPopups(maxLoops = 2)
                break
            }
            dismissPopups(maxLoops = 2)
            humanDelay(900L, 250L)
        }

        // 8) 메인으로
        returnToMainMenu()
        progress("스페셜매치 완료: ${played}회")
        return TaskResult.Success
    }
}
