package com.v26macro.runner.tasks

import com.v26macro.runner.TaskContext
import com.v26macro.runner.TaskKind
import com.v26macro.runner.TaskResult
import com.v26macro.util.humanDelay

/**
 * 후원금 정산.
 *
 * 흐름:
 *   1. 메인의 후원금 아이콘 (entry)
 *   2. 정산/수령 버튼 (claim)
 *   3. 보상 수령 후 뜨는 확인 버튼 (confirm) — 한 번 또는 여러 번 뜰 수 있어 반복 처리
 *   4. 일반 팝업 잔여물 닫기
 *   5. 뒤로가기로 메인 복귀
 *
 * 필요한 템플릿:
 *   - sponsor/entry    메인 화면의 후원금 아이콘
 *   - sponsor/claim    정산/수령 버튼
 *   - sponsor/confirm  보상 수령 후의 확인 버튼
 *   - home/popup_close (선택) 잔여 팝업 X
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

        // 보상 수령 확인 — 안내가 한 번 또는 여러 번 뜰 수 있어서 더 이상 안 보일 때까지 반복
        progress("보상 수령 확인")
        repeat(4) {
            if (!tapTemplate(bucket, "confirm", timeoutMs = 2500L)) return@repeat
            humanDelay(500L, 200L)
        }

        // 잔여 일반 팝업 닫기
        dismissPopups(maxLoops = 2)

        // 뒤로가기로 메인 복귀
        progress("뒤로가기 → 메인")
        returnToMainMenu()

        progress("후원금 완료")
        return TaskResult.Success
    }
}
