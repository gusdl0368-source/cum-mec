package com.v26macro.runner.tasks

import com.v26macro.input.GestureService
import com.v26macro.runner.TaskContext
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
 * 게임 내 뒤로가기. 시스템 BACK 키 (AccessibilityService.GLOBAL_ACTION_BACK) 만 사용한다.
 * 화면 색깔(팀 컬러)에 영향받지 않으므로 템플릿 매칭보다 안정적.
 *
 * 메인에서 BACK 을 누르면 V26이 "게임을 종료하시겠습니까?" 다이얼로그를 띄울 수 있어서,
 * 호출 후 home/exit_cancel 이 보이면 자동으로 취소를 누른다.
 */
suspend fun TaskContext.tapBack(): Boolean {
    val ok = GestureService.pressBack()
    humanDelay(700L, 200L)
    // 안전장치: 종료 확인 다이얼로그가 떴으면 즉시 취소
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
 * 메인 화면이 보일 때까지 시스템 BACK 키를 누름 (최대 [maxBack] 회).
 *
 * 매 반복마다 아래 순서로 처리한다:
 *  1. 이미 메인이면 종료
 *  2. 팝업이 가로막고 있으면 popup_close 로 닫고 다음 반복으로 (BACK 안 누름)
 *  3. 종료 다이얼로그(메인에서 BACK 한 번 더 눌렀을 때)면 exit_cancel 로 취소 — 메인에 있다는 뜻
 *  4. 그 외엔 BACK 키 한 번
 */
suspend fun TaskContext.returnToMainMenu(maxBack: Int = 10): Boolean {
    repeat(maxBack) {
        if (isOnMainMenu()) return true
        if (tapTemplate(BUCKET_HOME, "popup_close", timeoutMs = 800L)) {
            humanDelay()
            return@repeat
        }
        if (tapTemplate(BUCKET_HOME, "exit_cancel", timeoutMs = 600L)) {
            // 종료 다이얼로그가 떠 있다 = 우리는 메인에 있다.
            humanDelay()
            return true
        }
        GestureService.pressBack()
        humanDelay(900L, 200L)
    }
    return isOnMainMenu()
}

/** 필수 템플릿이 빠졌을 때 Task에서 빠르게 종료하기 위한 결과 만들기. */
fun missingAssets(vararg names: String): com.v26macro.runner.TaskResult =
    com.v26macro.runner.TaskResult.Skipped("필수 템플릿 누락: ${names.joinToString(", ")}")
