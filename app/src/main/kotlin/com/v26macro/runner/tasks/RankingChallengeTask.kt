package com.v26macro.runner.tasks

import com.v26macro.runner.TaskContext
import com.v26macro.runner.TaskKind
import com.v26macro.runner.TaskResult
import com.v26macro.util.humanDelay

/**
 * 랭킹챌린지.
 *
 * 한 세트 = 5경기. 한 세트가 끝나면 '총 5게임 진행 결과' 화면이 떠서 '확인' 후 메인 복귀.
 * 그 다음 갱신 버튼을 누르면 상대 리스트가 즉시 갱신되고 또 5경기를 돌릴 수 있음.
 *
 * 갱신 우선순위 (V26 가 자동으로 라벨만 바꿔줌, 같은 위치 버튼):
 *   1. 무료 갱신 3회 (즉시 갱신, 다이얼로그 없음)
 *   2. 포인트 갱신 6회 (P × 3,000 — 게임 포인트 차감)
 *   3. 스타 갱신 6회 (★ × 50 — 스타 차감)
 *   4. '금일 갱신 완료' 표시되면 종료
 *
 * 모든 갱신을 자동 진행. 1세트 + 갱신 15회 = 최대 16세트(80경기). 사용자가 포인트/스타를
 * 안 쓰고 싶으면 maxSets 만 줄이면 됨 (현재 16, 무료만 쓰려면 4).
 *
 * 필요한 템플릿:
 *   rankingchallenge/{entry, continuous_play, proceed, result_indicator, next,
 *                     summary_done, summary_confirm, refresh_button}
 *   (선택) refresh_done, incomplete_confirm
 */
class RankingChallengeTask : Task {
    override val kind = TaskKind.RankingChallenge
    private val bucket = kind.bucket

    private val maxSets = 16                 // 1 + 3 free + 6 point + 6 star
    private val matchTimeoutMs = 120_000L    // 한 경기 최대 2분
    private val summaryWaitMs = 30_000L      // 5경기 후 총결과 화면 대기

    override suspend fun run(ctx: TaskContext): TaskResult = with(ctx) {
        progress("랭킹챌린지 시작")

        if (!enterPlayballAndOpen(bucket, "entry")) {
            return missingAssets("home/playball or $bucket/entry")
        }
        humanDelay(900L, 200L)

        var totalMatches = 0
        var setsPlayed = 0

        for (set in 1..maxSets) {
            // 갱신 완료 체크 (2세트부터: 첫 세트는 무조건 진행)
            if (set > 1 && find(bucket, "refresh_done") != null) {
                progress("금일 갱신 완료 - 종료")
                break
            }

            progress("랭킹챌린지: ${set}번째 5경기 세트")
            setsPlayed = set

            // 1) 연속 경기 → 경기 진행
            if (!tapTemplate(bucket, "continuous_play", timeoutMs = 6000L)) {
                progress("연속 경기 버튼 못 찾음 - 종료")
                break
            }
            humanDelay(700L, 200L)
            if (!tapTemplate(bucket, "proceed", timeoutMs = 5000L)) {
                progress("경기 진행 버튼 못 찾음 - 종료")
                break
            }
            humanDelay(900L, 200L)

            // 2) 5경기 자동 진행 - 매 결과창 next 탭. summary_done 보이면 멈춤.
            for (match in 1..6) {  // 5 + 안전마진
                val arrived = waitForResultOrSummary(this)
                if (!arrived) {
                    progress("결과 화면 타임아웃 (세트 ${set}, ${match}번째)")
                    return@with TaskResult.Failed("매치 결과 타임아웃")
                }
                if (find(bucket, "summary_done") != null) break
                tapTemplate(bucket, "next", timeoutMs = 4000L)
                totalMatches++
                humanDelay(600L, 200L)
            }

            // 3) 총 결과 화면 도달 보장 + 확인 누르기
            val summary = find(bucket, "summary_done")
                ?: waitForTemplate(bucket, "summary_done", timeoutMs = summaryWaitMs)
            if (summary != null) {
                tapTemplate(bucket, "summary_confirm", timeoutMs = 5000L)
                humanDelay(1200L, 300L)
            } else {
                progress("총 결과 화면 못 봄 - 갱신 시도")
            }

            // 4) 갱신 (다음 세트로 이어짐). 마지막 세트면 갱신 안 누름.
            val isLastSet = set == maxSets
            if (!isLastSet) {
                if (find(bucket, "refresh_done") != null) {
                    progress("금일 갱신 완료 - 종료")
                    break
                }
                if (!tapTemplate(bucket, "refresh_button", timeoutMs = 5000L)) {
                    progress("갱신 버튼 못 찾음 - 종료")
                    break
                }
                humanDelay(900L, 200L)
                // 가끔 뜨는 '경기 안한 상대 있음' 팝업 처리
                tapTemplate(bucket, "incomplete_confirm", timeoutMs = 1500L)
                humanDelay(700L, 200L)
            }
        }

        returnToMainMenu()
        progress("랭킹챌린지 완료: ${setsPlayed}세트 / ${totalMatches}경기")
        return TaskResult.Success
    }

    /**
     * 결과 화면(result_indicator) 또는 총결과(summary_done) 둘 중 하나가 나타날
     * 때까지 대기.
     */
    private suspend fun waitForResultOrSummary(ctx: TaskContext): Boolean = with(ctx) {
        val deadline = System.currentTimeMillis() + matchTimeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (find(bucket, "summary_done") != null) return true
            if (find(bucket, "result_indicator") != null) return true
            kotlinx.coroutines.delay(500L)
        }
        return false
    }
}
