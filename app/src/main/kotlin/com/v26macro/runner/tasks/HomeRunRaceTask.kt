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
 *   3. 결과창의 '확인' 버튼이 보일 때까지 좌상단 '최고스코어' 영역을 계속 탭
 *      - 도중에 타구 경로 화면이 뜨면(공을 친 경우) ball_path_confirm 으로 넘김
 *   4. 결과창에서:
 *      - 더 돌릴 횟수 남았으면 '재도전' 탭 → 다시 (3)부터
 *      - 없으면 '확인' 탭 → break
 *   5. 메인으로 뒤로가기
 *
 * 필요한 템플릿: homerunrace/entry, play, top_left_target, confirm
 *               (선택) ball_path_confirm, retry
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
            // 플레이 버튼이 더 이상 안 보이면 일과 종료(횟수 소진)
            if (!tapTemplate(bucket, "play", timeoutMs = 5000L)) {
                progress("홈런레이스: 플레이 버튼 없음 - 종료")
                break
            }
            humanDelay(900L, 300L)

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
            dismissPopups(maxLoops = 2)
        }

        // 메인으로
        returnToMainMenu()
        progress("홈런레이스 완료: ${played}회")
        return TaskResult.Success
    }

    /**
     * 한 라운드: 결과창의 confirm 가 보일 때까지 좌상단을 탭. 도중에 타구 경로 화면이
     * 나오면 ball_path_confirm 으로 넘김.
     */
    private suspend fun playOneRound(ctx: TaskContext, timeoutMs: Long): Boolean = with(ctx) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            // 결과창 도달했으면 끝
            if (find(bucket, "confirm", threshold = 0.85) != null) return true
            // 타구 경로 화면이면 확인 눌러 넘기기
            val ballPath = find(bucket, "ball_path_confirm")
            if (ballPath != null) {
                tap(ballPath.centerX, ballPath.centerY)
                humanDelay(700L, 200L)
                continue
            }
            // 좌상단 '최고스코어' 영역을 탭 (스윙 트리거)
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
