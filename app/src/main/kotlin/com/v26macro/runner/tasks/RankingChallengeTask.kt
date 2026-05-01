package com.v26macro.runner.tasks

import com.v26macro.runner.TaskContext
import com.v26macro.runner.TaskKind
import com.v26macro.runner.TaskResult
import com.v26macro.util.humanDelay

/**
 * 랭킹챌린지: 메뉴 진입 -> 도전 -> 자동 -> 결과 수령. 일일 횟수까지 반복.
 *
 * Required templates (assets/templates/rankingchallenge/):
 *   - entry.png            랭킹챌린지 메뉴 아이콘
 *   - challenge.png        "도전" 버튼
 *   - opponent_pick.png    (선택) 상대 선택 화면 식별 - 보이면 첫 번째 슬롯 클릭
 *   - opponent_slot.png    상대 첫 번째 슬롯
 *   - auto.png             자동 진행
 *   - skip.png             (선택) 스킵
 *   - result.png           결과 화면 식별
 *   - claim.png            보상 수령
 *   - no_more.png          (선택) 횟수 소진 표시
 *   - exit.png             종료 버튼
 */
class RankingChallengeTask : Task {
    override val kind = TaskKind.RankingChallenge
    private val bucket = kind.bucket

    private val maxAttempts = 5

    override suspend fun run(ctx: TaskContext): TaskResult = with(ctx) {
        progress("랭킹챌린지 시작")

        if (!tapTemplate(bucket, "entry", timeoutMs = 8000L)) {
            return missingAssets("$bucket/entry")
        }

        var played = 0
        for (attempt in 1..maxAttempts) {
            if (find(bucket, "no_more", threshold = 0.82) != null) {
                progress("랭킹챌린지: 일일 횟수 소진")
                break
            }
            if (!tapTemplate(bucket, "challenge", timeoutMs = 6000L)) break

            // 상대 선택 화면이 뜨면 첫 슬롯 선택
            if (find(bucket, "opponent_pick") != null) {
                tapTemplate(bucket, "opponent_slot", timeoutMs = 3000L)
            }

            tapTemplate(bucket, "auto", timeoutMs = 4000L)
            tapTemplate(bucket, "skip", timeoutMs = 3000L)
            val ok = waitForTemplate(bucket, "result", timeoutMs = 120_000L) != null
            if (!ok) {
                progress("랭킹챌린지: 결과 타임아웃")
                break
            }
            tapTemplate(bucket, "claim", timeoutMs = 4000L)
            repeat(2) {
                tapTemplate(BUCKET_HOME, "popup_close", timeoutMs = 1500L)
                tapTemplate(BUCKET_HOME, "popup_confirm", timeoutMs = 1500L)
            }
            played++
            humanDelay(900L, 300L)
        }

        tapTemplate(bucket, "exit", timeoutMs = 3000L)
        dismissPopupsAndReturnToLobby()
        progress("랭킹챌린지 완료: ${played}회")
        return TaskResult.Success
    }
}
