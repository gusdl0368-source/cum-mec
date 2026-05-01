package com.v26macro.runner.tasks

import com.v26macro.runner.TaskContext
import com.v26macro.runner.TaskKind
import com.v26macro.runner.TaskResult
import com.v26macro.util.humanDelay

/**
 * 스페셜매치: 메뉴 진입 -> 매치 시작 -> 자동 -> 결과 수령. 가능한 횟수만큼 반복.
 *
 * Required templates (assets/templates/specialmatch/):
 *   - entry.png        스페셜매치 메뉴 아이콘
 *   - start.png        매치 시작/입장 버튼
 *   - auto.png         자동 진행 토글
 *   - skip.png         (선택) 결과 스킵 버튼
 *   - result.png       결과 화면 식별
 *   - claim.png        보상 수령
 *   - no_more.png      (선택) 입장권 부족 / 횟수 소진 표시
 *   - exit.png         종료 버튼
 */
class SpecialMatchTask : Task {
    override val kind = TaskKind.SpecialMatch
    private val bucket = kind.bucket

    private val maxAttempts = 6

    override suspend fun run(ctx: TaskContext): TaskResult = with(ctx) {
        progress("스페셜매치 시작")

        if (!tapTemplate(bucket, "entry", timeoutMs = 8000L)) {
            return missingAssets("$bucket/entry")
        }

        var played = 0
        for (attempt in 1..maxAttempts) {
            if (find(bucket, "no_more", threshold = 0.82) != null) {
                progress("스페셜매치: 입장 불가")
                break
            }
            if (!tapTemplate(bucket, "start", timeoutMs = 6000L)) break
            tapTemplate(bucket, "auto", timeoutMs = 4000L)
            tapTemplate(bucket, "skip", timeoutMs = 3000L)
            val ok = waitForTemplate(bucket, "result", timeoutMs = 90_000L) != null
            if (!ok) {
                progress("스페셜매치: 결과 화면 타임아웃")
                break
            }
            tapTemplate(bucket, "claim", timeoutMs = 4000L)
            repeat(2) {
                tapTemplate(BUCKET_HOME, "popup_close", timeoutMs = 1500L)
                tapTemplate(BUCKET_HOME, "popup_confirm", timeoutMs = 1500L)
            }
            played++
            humanDelay(900L, 250L)
        }

        tapTemplate(bucket, "exit", timeoutMs = 3000L)
        dismissPopupsAndReturnToLobby()
        progress("스페셜매치 완료: ${played}회")
        return TaskResult.Success
    }
}
