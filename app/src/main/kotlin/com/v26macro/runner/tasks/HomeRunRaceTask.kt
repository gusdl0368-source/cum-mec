package com.v26macro.runner.tasks

import com.v26macro.runner.TaskContext
import com.v26macro.runner.TaskKind
import com.v26macro.runner.TaskResult
import com.v26macro.util.humanDelay

/**
 * 홈런레이스: 일일 가능 횟수만큼 입장 -> 자동 진행 -> 결과 수령 -> 재입장 반복.
 *
 * Required templates (assets/templates/homerunrace/):
 *   - entry.png        메인의 "홈런레이스" 아이콘
 *   - start.png        입장/시작 버튼
 *   - auto.png         자동 진행/스킵 버튼 (있으면 누름)
 *   - result.png       결과 화면 식별자
 *   - claim.png        결과 보상 수령 버튼
 *   - no_more.png      (선택) "오늘 가능 횟수 초과" 표시 → 보이면 즉시 종료
 *   - exit.png         종료 버튼
 */
class HomeRunRaceTask : Task {
    override val kind = TaskKind.HomeRunRace
    private val bucket = kind.bucket

    private val maxAttempts = 8 // safety cap

    override suspend fun run(ctx: TaskContext): TaskResult = with(ctx) {
        progress("홈런레이스 시작")

        if (!tapTemplate(bucket, "entry", timeoutMs = 8000L)) {
            return missingAssets("$bucket/entry")
        }

        var played = 0
        for (attempt in 1..maxAttempts) {
            if (find(bucket, "no_more", threshold = 0.82) != null) {
                progress("홈런레이스: 일일 횟수 소진")
                break
            }
            if (!tapTemplate(bucket, "start", timeoutMs = 6000L)) {
                progress("홈런레이스: 시작 버튼 사라짐")
                break
            }
            // 게임 진행 중에는 자동 버튼 한 번씩 눌러줌
            tapTemplate(bucket, "auto", timeoutMs = 4000L)
            // 결과 대기 → 보상 수령
            val gotResult = waitForTemplate(bucket, "result", timeoutMs = 60_000L) != null
            if (!gotResult) {
                progress("홈런레이스: 결과 화면 타임아웃")
                break
            }
            tapTemplate(bucket, "claim", timeoutMs = 4000L)
            // 보상 팝업 닫기
            dismissPopupsAndReturnToLobbyOnce(this)
            played++
            humanDelay(800L, 300L)
        }

        tapTemplate(bucket, "exit", timeoutMs = 3000L)
        dismissPopupsAndReturnToLobby()
        progress("홈런레이스 완료: ${played}회")
        return TaskResult.Success
    }
}

private suspend fun dismissPopupsAndReturnToLobbyOnce(ctx: TaskContext) {
    repeat(2) {
        ctx.tapTemplate(BUCKET_HOME, "popup_close", timeoutMs = 1500L)
        ctx.tapTemplate(BUCKET_HOME, "popup_confirm", timeoutMs = 1500L)
    }
}
