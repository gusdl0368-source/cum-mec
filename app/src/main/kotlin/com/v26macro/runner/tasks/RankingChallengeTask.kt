package com.v26macro.runner.tasks

import com.v26macro.runner.TaskContext
import com.v26macro.runner.TaskKind
import com.v26macro.runner.TaskResult
import com.v26macro.util.humanDelay

/**
 * 랭킹챌린지.
 *
 * 흐름:
 *   1. (메인) 플레이볼 → 리그모드 → 랭킹챌린지 카드
 *   2. '연속 경기' 보라 버튼 (5경기 자동 진행 트리거)
 *   3. 알림 다이얼로그의 '경기 진행' 파란 버튼
 *   4. 5경기 자동 진행. 매 경기 결과 화면이 보이면 '다음 경기 시작 (n)' 좌표 탭으로
 *      5초 카운트다운 즉시 스킵. 마지막 5/5 결과 화면도 같은 좌표('다음') 라 같이 처리.
 *   5. 메인 BACK
 *
 * 매 결과창의 버튼이 같은 위치이므로 next 항목은 좌표(가드=result_indicator) 추천.
 *
 * 필요한 템플릿:
 *   rankingchallenge/{entry, continuous_play, proceed, result_indicator, next}
 */
class RankingChallengeTask : Task {
    override val kind = TaskKind.RankingChallenge
    private val bucket = kind.bucket

    private val maxMatches = 7              // 5경기지만 안전 마진
    private val matchTimeoutMs = 120_000L   // 한 경기 최대 2분 (시뮬레이션 자동)

    override suspend fun run(ctx: TaskContext): TaskResult = with(ctx) {
        progress("랭킹챌린지 시작")

        // 1) 플레이볼 → 리그모드 → 랭킹챌린지
        if (!enterPlayballAndOpen(bucket, "entry")) {
            return missingAssets("home/playball or $bucket/entry")
        }

        // 2) 연속 경기 버튼
        if (!tapTemplate(bucket, "continuous_play", timeoutMs = 5000L)) {
            tapBack(); returnToMainMenu()
            return missingAssets("$bucket/continuous_play")
        }
        humanDelay(800L, 200L)

        // 3) '경기 진행' 다이얼로그 확정
        if (!tapTemplate(bucket, "proceed", timeoutMs = 5000L)) {
            tapBack(); tapBack(); returnToMainMenu()
            return missingAssets("$bucket/proceed")
        }
        humanDelay(900L, 200L)

        // 4) 5경기 자동 진행 - 매 결과창에서 다음 버튼 즉시 탭
        var played = 0
        repeat(maxMatches) {
            val gotResult = waitForTemplate(
                bucket, "result_indicator",
                timeoutMs = matchTimeoutMs,
            )
            if (gotResult == null) {
                progress("랭킹챌린지: 결과 화면 타임아웃 (시뮬 끝났을 수도)")
                return@repeat
            }
            played++
            progress("랭킹챌린지: ${played}/5 결과 → 다음")
            // 다음 경기 시작 / 다음 — 좌표(가드=result_indicator)면 result 보일 때만 탭
            tapTemplate(bucket, "next", timeoutMs = 4000L)
            humanDelay(700L, 200L)
            // 잔여 팝업 흡수
            dismissPopups(maxLoops = 1)
        }

        // 5) 메인으로
        returnToMainMenu()
        progress("랭킹챌린지 완료: ${played}경기")
        return TaskResult.Success
    }
}
