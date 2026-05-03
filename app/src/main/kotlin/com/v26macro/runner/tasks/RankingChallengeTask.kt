package com.v26macro.runner.tasks

import com.v26macro.runner.TaskContext
import com.v26macro.runner.TaskKind
import com.v26macro.runner.TaskResult
import com.v26macro.util.humanDelay

/**
 * 랭킹챌린지.
 *
 * 한 세트 = 5경기. 한 세트 끝나면 '총 5게임 진행 결과' 화면 → '확인' → 메인. 갱신 버튼으로
 * 새 5경기 가져옴. 무료 갱신 3 + 포인트 6 + 스타 6 = 갱신 15회 (V26 가 같은 버튼 라벨만
 * 바꿈). 무료는 즉시 갱신, 포인트/스타는 추가 확인 다이얼로그 한 번 더 필요.
 *
 * 사용자 설정 (UI 에서 조절):
 *   - refreshLevel: 0=무료만(4세트) / 1=포인트까지(10세트) / 2=스타까지(16세트)
 *   - leaveLastSet: 마지막 1세트(5경기) 남기고 종료할지 여부
 *     (남기면 다음날 매칭 점수 낮춰서 쉬운 상대 → 어제 남은 5판부터 시작)
 *
 * 필요한 템플릿:
 *   rankingchallenge/{entry, play_ball, continuous_play, proceed, result_indicator,
 *                     next, summary_done, summary_confirm, refresh_button}
 *   (선택) refresh_paid_confirm, refresh_done, incomplete_confirm
 */
class RankingChallengeTask(
    private val refreshLevel: Int = 1,        // 0=free, 1=point, 2=star
    private val leaveLastSet: Boolean = true,
) : Task {
    override val kind = TaskKind.RankingChallenge
    private val bucket = kind.bucket

    private val matchTimeoutMs = 120_000L    // 한 경기 최대 2분
    private val summaryWaitMs = 30_000L      // 5경기 후 총결과 화면 대기

    override suspend fun run(ctx: TaskContext): TaskResult = with(ctx) {
        progress("랭킹챌린지 시작")

        if (!enterPlayballAndOpen(bucket, "entry")) {
            return missingAssets("home/playball or $bucket/entry")
        }
        humanDelay(900L, 200L)

        // refreshLevel 에 따라 가능한 세트 수 결정. leaveLastSet 이면 마지막 1세트 남김.
        val baseMaxSets = when (refreshLevel) {
            0 -> 4    // 1 + 3 free
            1 -> 10   // 1 + 3 free + 6 point
            else -> 16  // 1 + 3 free + 6 point + 6 star
        }
        val maxSets = if (leaveLastSet) (baseMaxSets - 1).coerceAtLeast(1) else baseMaxSets
        val levelLabel = when (refreshLevel) { 0 -> "무료만"; 1 -> "포인트까지"; else -> "스타까지" }
        val leaveLabel = if (leaveLastSet) "마지막5판 남김" else "전부 진행"
        progress("랭킹챌린지: ${levelLabel}, ${leaveLabel}, 목표 ${maxSets}세트")

        var totalMatches = 0
        var setsPlayed = 0

        for (set in 1..maxSets) {
            // 갱신 완료 체크
            if (find(bucket, "refresh_done") != null) {
                progress("금일 갱신 완료 - 종료")
                break
            }

            // PLAY BALL 안 보이면 = 5경기가 이미 다 진행된 상태
            // (사용자가 수동으로 했거나 매크로 이전 실행에서 진행 후 갱신 안 한 케이스)
            // → 갱신 한 번 눌러서 새 5경기 받고 시작.
            if (find(bucket, "play_ball") == null) {
                progress("PLAY BALL 없음 - 갱신 후 재시도 (이미 5경기 진행됐을 수 있음)")
                if (!performRefresh(this)) {
                    progress("초기 갱신 실패 - 종료")
                    break
                }
                humanDelay(1500L, 300L)
                if (find(bucket, "play_ball") == null) {
                    progress("갱신 후에도 PLAY BALL 없음 - 종료")
                    break
                }
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

            // 3-b) summary 후에 승격 화면이 뜨는 경우가 있어서 한 번 처리
            dismissPromotionIfPresent(this)

            // 4) 갱신 (다음 세트로 이어짐). 마지막 세트면 갱신 안 누름 = "마지막 5판 남기기".
            val isLastSet = set == maxSets
            if (!isLastSet) {
                if (!performRefresh(this)) {
                    progress("세트 후 갱신 실패 - 종료")
                    break
                }
            }
        }

        returnToMainMenu()
        progress("랭킹챌린지 완료: ${setsPlayed}세트 / ${totalMatches}경기 ($levelLabel, $leaveLabel)")
        return TaskResult.Success
    }

    /**
     * 갱신 단계: ranking main 검증 → refresh_done 체크 → refresh_button 탭 →
     * 포인트/스타 다이얼로그 처리 → 잔여 팝업 처리.
     *
     * 매크로가 세트 사이에 갱신할 때, 그리고 진입 시 PLAY BALL 안 보일 때(이미 5경기
     * 끝나있는 케이스) 둘 다 호출됨.
     */
    private suspend fun performRefresh(ctx: TaskContext): Boolean = with(ctx) {
        // 승격 화면이 떠있으면 먼저 dismiss (main_indicator 가려질 수 있음)
        dismissPromotionIfPresent(this)

        // ranking main 화면임을 검증.
        // continuous_play 는 5경기 다 끝나면 어두워져서 PNG 매칭이 불안정.
        // 대신 '챌린지 포인트' 헤더 같은 상태와 무관한 main_indicator 를 사용.
        val onRankingMain = waitForTemplate(
            bucket, "main_indicator", timeoutMs = 8000L
        ) != null
        if (!onRankingMain) {
            progress("갱신: ranking main 못 찾음 (main_indicator 안 보임)")
            return@with false
        }
        // 갱신 완료 상태면 false 반환 (호출자가 종료 결정)
        if (find(bucket, "refresh_done") != null) {
            progress("갱신: 금일 갱신 완료 표시")
            return@with false
        }
        // 갱신 버튼 탭 (좌표 우선)
        if (!tapTemplate(bucket, "refresh_button", timeoutMs = 5000L)) {
            progress("갱신: refresh_button 탭 실패 (좌표/PNG 모두 미정의)")
            return@with false
        }
        humanDelay(900L, 200L)
        // 포인트/스타 갱신 시 추가 확인 다이얼로그 (무료는 안 뜸). 안 떠도 무해.
        tapTemplate(bucket, "refresh_paid_confirm", timeoutMs = 1500L)
        humanDelay(600L, 200L)
        // 드물게 '경기 안한 상대 있음' 팝업
        tapTemplate(bucket, "incomplete_confirm", timeoutMs = 1200L)
        humanDelay(500L, 200L)
        return@with true
    }

    /**
     * 승격 화면 처리 — 따로 버튼 없이 화면 어디든 탭하면 넘어감.
     * 5경기 다 이긴 뒤 등급(BRONZE I → BRONZE II 등) 올라갈 때 뜸. 보이면 매칭 영역
     * 중심을 탭해 dismiss.
     */
    private suspend fun dismissPromotionIfPresent(ctx: TaskContext): Boolean = with(ctx) {
        val match = find(bucket, "promotion") ?: return@with false
        progress("승격 화면 감지 → 탭으로 넘김")
        tap(match.centerX, match.centerY)
        humanDelay(1100L, 300L)
        return@with true
    }

    /**
     * 결과 화면(result_indicator) 또는 총결과(summary_done) 둘 중 하나가 나타날
     * 때까지 대기. 도중에 승격 화면이 뜨면 자동 처리.
     */
    private suspend fun waitForResultOrSummary(ctx: TaskContext): Boolean = with(ctx) {
        val deadline = System.currentTimeMillis() + matchTimeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (find(bucket, "summary_done") != null) return true
            if (find(bucket, "result_indicator") != null) return true
            dismissPromotionIfPresent(this)
            kotlinx.coroutines.delay(500L)
        }
        return false
    }
}
