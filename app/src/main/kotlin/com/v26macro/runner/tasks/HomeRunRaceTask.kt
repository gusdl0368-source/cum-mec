package com.v26macro.runner.tasks

import com.v26macro.runner.TaskContext
import com.v26macro.runner.TaskKind
import com.v26macro.runner.TaskResult
import com.v26macro.util.humanDelay
import kotlinx.coroutines.delay

/**
 * 홈런레이스.
 *
 * 흐름:
 *   1. (메인) 플레이볼 → 홈런레이스 탭
 *   2. 플레이 버튼
 *   3. 결과창에 도달할 때까지 좌상단 '최고스코어' 영역을 계속 탭. 도중에 공을 친 경우
 *      타구 경로 화면이 뜨면 '확인' 으로 넘김.
 *   4. 결과창에서 더 돌릴지에 따라 '재도전' 또는 '확인'.
 *   5. 메인으로 BACK.
 *
 * 같은 '확인' 버튼이 두 화면(타구경로/결과창)에서 똑같이 나타나기 때문에 템플릿만으로는
 * 구별 불가. 대신 컨텍스트로 구별:
 *   - 화면에 retry(재도전) 가 보인다 → 결과창 (공 친 경우/못 친 경우 모두 retry 있음)
 *   - confirm 만 보이고 retry 가 없다 → 타구 경로 화면
 *   - 둘 다 안 보인다 → 게임플레이 중 (좌상단 탭)
 *
 * 필요한 템플릿: homerunrace/entry, play, top_left_target, confirm, retry
 */
class HomeRunRaceTask : Task {
    override val kind = TaskKind.HomeRunRace
    private val bucket = kind.bucket

    private val maxRounds = 8           // 일일 가능 횟수 안전 상한
    private val resultTimeoutMs = 90_000L  // 한 라운드 최대 길이

    override suspend fun run(ctx: TaskContext): TaskResult = with(ctx) {
        progress("홈런레이스 시작")

        // 플레이볼 → 홈런레이스
        if (!enterPlayballAndOpen(bucket, "entry")) {
            return missingAssets("home/playball or $bucket/entry")
        }

        var played = 0
        for (round in 1..maxRounds) {
            // 1라운드는 플레이 버튼 필수. 2라운드부터는 '재도전' 으로 곧바로 시작될 수
            // 있어서 플레이 버튼이 안 보이면 그냥 폴링 루프로 진입.
            if (round == 1) {
                if (!tapTemplate(bucket, "play", timeoutMs = 5000L)) {
                    progress("홈런레이스: 플레이 버튼 없음 - 종료")
                    break
                }
                humanDelay(900L, 300L)
            } else {
                // 짧게만 시도. 안 보이면 바로 게임 진행 중이라고 가정.
                if (tapTemplate(bucket, "play", timeoutMs = 1500L)) {
                    humanDelay(900L, 300L)
                }
            }

            // 게임 진행: confirm(또는 ball_path_confirm) 보일 때까지 좌상단 탭 반복
            val ok = playOneRound(this, resultTimeoutMs)
            if (!ok) {
                progress("홈런레이스 라운드 ${round}: 결과창 타임아웃")
                break
            }
            played++

            // 결과창에서 마지막 라운드라면 confirm, 아니면 retry
            val isLast = round >= maxRounds
            val tappedRetry = if (!isLast) tapTemplate(bucket, "retry", timeoutMs = 3000L) else false
            if (!tappedRetry) {
                tapTemplate(bucket, "confirm", timeoutMs = 4000L)
                dismissPopups(maxLoops = 2)
                break
            }
            // 재도전 후 다음 라운드는 보통 자동 시작. 다음 iter 에서 짧게 play 만 시도하고 폴링 진입.
            dismissPopups(maxLoops = 2)
        }

        // 메인으로
        returnToMainMenu()
        progress("홈런레이스 완료: ${played}회")
        return TaskResult.Success
    }

    /**
     * 한 라운드: 결과창에 도달할 때까지 좌상단 탭 + 타구 경로 화면 자동 스킵.
     *
     * 매 폴링마다 retry / confirm 의 가시성을 조합해 어느 화면인지 결정한다:
     *   - retry 있음              → 결과창 (loop 종료, 호출자가 retry/confirm 결정)
     *   - retry 없음 + confirm 있음 → 타구 경로 화면 (confirm 눌러 스킵 후 계속)
     *   - 둘 다 없음               → 게임플레이 중 (좌상단 탭)
     */
    private suspend fun playOneRound(ctx: TaskContext, timeoutMs: Long): Boolean = with(ctx) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val retryMatch = find(bucket, "retry")
            if (retryMatch != null) {
                // 결과창 도달 (공 쳤든 못 쳤든 retry 가 있음)
                return true
            }
            val confirmMatch = find(bucket, "confirm")
            if (confirmMatch != null) {
                // 타구 경로 화면 - 확인으로 스킵
                tap(confirmMatch.centerX, confirmMatch.centerY)
                humanDelay(700L, 200L)
                continue
            }
            // 게임플레이 - 좌상단 '최고스코어' 영역을 탭 (스윙 트리거)
            val topLeft = find(bucket, "top_left_target")
            if (topLeft != null) {
                tap(topLeft.centerX, topLeft.centerY)
            } else {
                // 템플릿 못 찾으면 화면 좌상단 영역 임의 좌표 탭 (1/8 ~ 1/8)
                tap(screenWidth / 8, screenHeight / 8)
            }
            delay(200L)
        }
        return false
    }
}

/**
 * 메인의 플레이볼을 눌러 메뉴를 연 뒤 [bucket]/[name] 을 탭한다.
 * 이미 해당 메뉴 안이라면 곧바로 entry 만 시도.
 */
internal suspend fun TaskContext.enterPlayballAndOpen(
    bucket: String,
    name: String,
    timeoutMs: Long = 8000L,
): Boolean {
    if (!isOnMainMenu()) returnToMainMenu()
    // 메인의 플레이볼을 누르면 홈런/스페셜/랭킹/리그 탭이 보이는 메뉴가 나옴
    tapTemplate(BUCKET_HOME, "playball", timeoutMs = 5000L)
    humanDelay(800L, 200L)
    return tapTemplate(bucket, name, timeoutMs = timeoutMs)
}
