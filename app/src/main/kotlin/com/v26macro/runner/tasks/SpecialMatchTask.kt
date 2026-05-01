package com.v26macro.runner.tasks

import com.v26macro.runner.TaskContext
import com.v26macro.runner.TaskKind
import com.v26macro.runner.TaskResult
import com.v26macro.util.humanDelay

/**
 * 스페셜매치 - 잠재력 60오버롤 매치 자동.
 *
 * 흐름:
 *   1. (메인) 플레이볼 → 스페셜매치
 *   2. '잠재력' 탭
 *   3. '60오버롤 매치' 입장
 *   4. start1 (1차 스타트)
 *   5. random_pick (랜덤픽플레이)
 *   6. 게이지 손잡이를 우측 끝까지 드래그
 *   7. direct_play_on 가 보이면(켜져있으면) 탭해서 끔
 *   8. start2 (최종 스타트) — 전체 자동, 결과 대기
 *   9. 결과창에서 result_next → 한 번 더 하기 / 확인
 *  10. 더 돌릴 만큼 반복 후 메인으로 뒤로가기
 *
 * 필요한 템플릿: specialmatch/entry, jamjeryeok_tab, match_60ovr, start1,
 *               random_pick, gauge_handle, start2, result_next, confirm
 *               (선택) direct_play_on, play_again
 */
class SpecialMatchTask : Task {
    override val kind = TaskKind.SpecialMatch
    private val bucket = kind.bucket

    private val maxRounds = 5
    private val matchTimeoutMs = 240_000L  // 자동 진행 길어질 수 있어 4분

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

        // 3) 60오버롤 매치 입장
        if (!tapTemplate(bucket, "match_60ovr", timeoutMs = 5000L)) {
            tapBack(); returnToMainMenu()
            return missingAssets("$bucket/match_60ovr")
        }
        humanDelay(900L, 250L)

        var played = 0
        for (round in 1..maxRounds) {
            // 4) 1차 스타트 (1라운드만 필요. 2라운드 부터는 한 번 더 하기로 진입)
            if (round == 1) {
                if (!tapTemplate(bucket, "start1", timeoutMs = 6000L)) {
                    progress("스페셜매치: start1 실패")
                    break
                }
                humanDelay()
            }

            // 5) 랜덤픽플레이
            if (!tapTemplate(bucket, "random_pick", timeoutMs = 6000L)) {
                progress("스페셜매치: random_pick 실패")
                break
            }
            humanDelay()

            // 6) 게이지 손잡이를 찾아 우측 끝까지 드래그
            val handle = find(bucket, "gauge_handle")
            if (handle != null) {
                val targetX = (screenWidth - 60).coerceAtLeast(handle.centerX + 100)
                swipe(handle.centerX, handle.centerY, targetX, handle.centerY, durationMs = 600L)
                humanDelay()
            } else {
                progress("스페셜매치: gauge_handle 못 찾음 - 기본 위치로 진행")
            }

            // 7) 직접 플레이가 켜져있으면 끄기
            val onState = find(bucket, "direct_play_on")
            if (onState != null) {
                tap(onState.centerX, onState.centerY)
                humanDelay()
            }

            // 8) 최종 스타트
            if (!tapTemplate(bucket, "start2", timeoutMs = 5000L)) {
                progress("스페셜매치: start2 실패")
                break
            }

            // 자동 진행 → 결과창 대기
            val ok = waitForTemplate(bucket, "result_next", timeoutMs = matchTimeoutMs) != null
            if (!ok) {
                progress("스페셜매치: 결과 타임아웃")
                break
            }
            tapTemplate(bucket, "result_next", timeoutMs = 3000L)
            played++

            // 9) 더 돌릴지 여부에 따라 한 번 더 하기 / 확인
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

        // 10) 메인으로
        returnToMainMenu()
        progress("스페셜매치 완료: ${played}회")
        return TaskResult.Success
    }
}
