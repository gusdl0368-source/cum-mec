package com.v26macro.runner.tasks

import com.v26macro.runner.TaskContext
import com.v26macro.runner.TaskKind
import com.v26macro.runner.TaskResult

/**
 * 후원금 정산: 메인 로비의 "후원금" 아이콘 -> "정산" 버튼 -> "확인"으로 보상 수령.
 *
 * Required template assets (place in app/src/main/assets/templates/sponsor/):
 *   - entry.png       메인 화면에 보이는 후원금 아이콘
 *   - claim.png       정산/수령 버튼
 *   - confirm.png     수령 후 뜨는 확인 버튼 (선택, 없으면 자동으로 popups로 닫음)
 *   - empty.png       (선택) "정산할 후원금이 없습니다" 안내 — 보이면 즉시 종료
 */
class SponsorPayoutTask : Task {
    override val kind = TaskKind.SponsorPayout
    private val bucket = kind.bucket

    override suspend fun run(ctx: TaskContext): TaskResult = with(ctx) {
        progress("후원금 시작")

        if (!tapTemplate(bucket, "entry", timeoutMs = 8000L)) {
            return missingAssets("$bucket/entry")
        }

        // 이미 정산할 게 없으면 빠르게 종료
        if (find(bucket, "empty", threshold = 0.82) != null) {
            progress("후원금: 정산할 항목 없음")
            dismissPopupsAndReturnToLobby()
            return TaskResult.Success
        }

        if (!tapTemplate(bucket, "claim", timeoutMs = 8000L)) {
            dismissPopupsAndReturnToLobby()
            return TaskResult.Failed("정산 버튼을 찾지 못함")
        }
        // 확인 팝업이 나오면 닫기. 없어도 OK.
        tapTemplate(bucket, "confirm", timeoutMs = 4000L)

        dismissPopupsAndReturnToLobby()
        progress("후원금 완료")
        return TaskResult.Success
    }
}
