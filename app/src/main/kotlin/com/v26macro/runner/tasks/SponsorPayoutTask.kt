package com.v26macro.runner.tasks

import com.v26macro.runner.TaskContext
import com.v26macro.runner.TaskKind
import com.v26macro.runner.TaskResult
import com.v26macro.util.humanDelay

/**
 * 후원금 정산.
 *
 * 흐름: 메인의 후원금 아이콘 → 정산 → 보상 팝업 닫기 → 뒤로가기로 메인 복귀.
 *
 * 필요한 템플릿:
 *   - sponsor/entry  메인 화면의 후원금 아이콘
 *   - sponsor/claim  정산/수령 버튼
 *   - home/back      뒤로가기 (없으면 시스템 BACK 키 사용)
 *   - home/popup_close (선택) 보상 팝업 X
 */
class SponsorPayoutTask : Task {
    override val kind = TaskKind.SponsorPayout
    private val bucket = kind.bucket

    override suspend fun run(ctx: TaskContext): TaskResult = with(ctx) {
        progress("후원금 시작")

        if (!tapTemplate(bucket, "entry", timeoutMs = 8000L)) {
            return missingAssets("$bucket/entry")
        }

        if (!tapTemplate(bucket, "claim", timeoutMs = 8000L)) {
            tapBack()
            return TaskResult.Failed("정산 버튼을 찾지 못함")
        }
        humanDelay(900L, 300L)

        // 보상 획득 팝업 닫기 (몇 번 떠도 처리)
        dismissPopups(maxLoops = 4)

        // 뒤로가기로 메인 복귀
        progress("뒤로가기 → 메인")
        returnToMainMenu()

        progress("후원금 완료")
        return TaskResult.Success
    }
}
