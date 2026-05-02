package com.v26macro.runner.tasks

import com.v26macro.input.GestureService
import com.v26macro.runner.TaskContext
import com.v26macro.util.Logger
import com.v26macro.util.humanDelay

/**
 * 모든 Task에서 공유하는 헬퍼.
 *
 * 메인 화면(= "플레이볼" 메뉴 버튼이 보이는 화면) 식별과 뒤로가기 처리를 담는다.
 */

const val BUCKET_HOME = "home"

/** 현재 V26 메인 화면(플레이볼 버튼이 보이는 화면)인지 검사. */
suspend fun TaskContext.isOnMainMenu(): Boolean =
    find(BUCKET_HOME, "playball") != null

/** 메인 화면에서 [bucket]/[name] 진입 버튼을 찾아 탭. 메인 아니면 먼저 뒤로 빠져나감. */
suspend fun TaskContext.enterFromMainMenu(
    bucket: String,
    name: String = "entry",
    timeoutMs: Long = 10_000L,
): Boolean {
    if (!isOnMainMenu()) returnToMainMenu()
    return tapTemplate(bucket, name, timeoutMs = timeoutMs)
}

/**
 * 게임 내 뒤로가기.
 *
 * 우선순위:
 *  1. 시각 버튼 home/back_button (있으면) 탭 — 시스템 BACK 안 먹히는 화면 대비
 *  2. 시스템 BACK 키 (AccessibilityService.GLOBAL_ACTION_BACK) — 폴백
 *
 * 호출 후 home/exit_cancel 이 보이면 자동으로 취소를 누른다.
 */
suspend fun TaskContext.tapBack(): Boolean {
    // 1) 시각 뒤로가기 버튼 우선 시도 (있으면)
    if (tapTemplate(BUCKET_HOME, "back_button", timeoutMs = 600L)) {
        humanDelay(900L, 250L)
        tapTemplate(BUCKET_HOME, "exit_cancel", timeoutMs = 800L)
        return true
    }
    // 2) 시스템 BACK 키 폴백
    val ok = GestureService.pressBack()
    humanDelay(900L, 250L)
    tapTemplate(BUCKET_HOME, "exit_cancel", timeoutMs = 800L)
    return ok
}

/** 팝업이 보이는 동안 닫기 버튼들을 반복적으로 탭. */
suspend fun TaskContext.dismissPopups(maxLoops: Int = 4) {
    repeat(maxLoops) {
        val closed = tapTemplate(BUCKET_HOME, "popup_close", timeoutMs = 800L)
        if (!closed) return
        humanDelay()
    }
}

/**
 * 메인 화면이 보일 때까지 화면 BACK / 시스템 BACK 키 반복 (최대 [maxBack] 회).
 *
 * 매 반복마다 아래 순서로 처리한다:
 *  1. 이미 메인이면 종료
 *  2. 팝업이 가로막고 있으면 popup_close 로 닫고 다음 반복 (BACK 안 누름)
 *  3. 종료 다이얼로그(메인에서 BACK 한 번 더 눌렀을 때) → exit_cancel 로 취소 → 메인 도착
 *  4. 화면 안의 시각 '뒤로 가기' 버튼(back_button) 이 있으면 탭
 *  5. 없으면 시스템 BACK 키
 *
 * 각 단계마다 어떤 액션을 했는지 로그에 남겨서 디버그 용이.
 */
suspend fun TaskContext.returnToMainMenu(maxBack: Int = 10): Boolean {
    Logger.i("returnToMainMenu: 시작 (maxBack=$maxBack)")
    repeat(maxBack) { iter ->
        if (isOnMainMenu()) {
            Logger.i("returnToMainMenu: ${iter + 1}번째 반복에서 메인 도착")
            return true
        }
        if (tapTemplate(BUCKET_HOME, "popup_close", timeoutMs = 800L)) {
            Logger.i("returnToMainMenu: ${iter + 1}번째 — 팝업 닫음")
            humanDelay(800L, 200L)
            return@repeat
        }
        if (tapTemplate(BUCKET_HOME, "exit_cancel", timeoutMs = 600L)) {
            // 종료 다이얼로그가 떠 있다 = 우리는 메인에 있다.
            Logger.i("returnToMainMenu: ${iter + 1}번째 — exit_cancel 매칭 (메인 가정)")
            humanDelay(800L, 200L)
            return true
        }
        if (tapTemplate(BUCKET_HOME, "back_button", timeoutMs = 600L)) {
            Logger.i("returnToMainMenu: ${iter + 1}번째 — 시각 back_button 탭")
            humanDelay(1100L, 300L)  // 화면 전환 충분히 대기
            return@repeat
        }
        Logger.i("returnToMainMenu: ${iter + 1}번째 — 시스템 BACK 키")
        GestureService.pressBack()
        humanDelay(1100L, 300L)
    }
    val finallyOk = isOnMainMenu()
    Logger.w("returnToMainMenu: ${maxBack}회 시도 후 결과 = $finallyOk")
    return finallyOk
}

/** 필수 템플릿이 빠졌을 때 Task에서 빠르게 종료하기 위한 결과 만들기. */
fun missingAssets(vararg names: String): com.v26macro.runner.TaskResult =
    com.v26macro.runner.TaskResult.Skipped("필수 템플릿 누락: ${names.joinToString(", ")}")
