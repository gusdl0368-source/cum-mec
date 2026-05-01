package com.v26macro.runner.tasks

import com.v26macro.runner.TaskContext
import com.v26macro.runner.TaskKind
import com.v26macro.runner.TaskResult
import com.v26macro.util.humanDelay

/**
 * 리그모드: 리그 입장 -> 경기 시작 -> 자동/스킵 -> 결과 수령. 가능한 경기까지 반복.
 *
 * Required templates (assets/templates/leaguemode/):
 *   - entry.png        리그모드 메뉴 아이콘
 *   - play.png         경기 시작 버튼 ("플레이" / "경기시작")
 *   - lineup_ok.png    (선택) 라인업 확인 후 시작 버튼
 *   - auto.png         자동 진행
 *   - skip.png         (선택) 결과 스킵
 *   - result.png       결과 화면
 *   - claim.png        보상 수령
 *   - no_more.png      (선택) 더 이상 경기 없음
 *   - exit.png         종료
 */
class LeagueModeTask : Task {
    override val kind = TaskKind.LeagueMode
    private val bucket = kind.bucket

    private val maxGames = 12

    override suspend fun run(ctx: TaskContext): TaskResult = with(ctx) {
        progress("리그모드 시작")

        if (!tapTemplate(bucket, "entry", timeoutMs = 8000L)) {
            return missingAssets("$bucket/entry")
        }

        var played = 0
        for (game in 1..maxGames) {
            if (find(bucket, "no_more", threshold = 0.82) != null) {
                progress("리그모드: 가능한 경기 없음")
                break
            }
            if (!tapTemplate(bucket, "play", timeoutMs = 8000L)) break
            // 라인업 확인 화면이 뜨면 OK 누름
            tapTemplate(bucket, "lineup_ok", timeoutMs = 3000L)

            tapTemplate(bucket, "auto", timeoutMs = 5000L)
            tapTemplate(bucket, "skip", timeoutMs = 4000L)

            // 리그 경기는 길 수 있음 - 4분 대기
            val ok = waitForTemplate(bucket, "result", timeoutMs = 240_000L) != null
            if (!ok) {
                progress("리그모드: 결과 타임아웃")
                break
            }
            tapTemplate(bucket, "claim", timeoutMs = 4000L)
            repeat(3) {
                tapTemplate(BUCKET_HOME, "popup_close", timeoutMs = 1500L)
                tapTemplate(BUCKET_HOME, "popup_confirm", timeoutMs = 1500L)
            }
            played++
            humanDelay(1200L, 300L)
        }

        tapTemplate(bucket, "exit", timeoutMs = 3000L)
        dismissPopupsAndReturnToLobby()
        progress("리그모드 완료: ${played}경기")
        return TaskResult.Success
    }
}
