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
 * 게임 내 뒤로가기. home/back 템플릿을 우선 사용하고, 없으면 시스템 BACK 키로 폴백.
 */
suspend fun TaskContext.tapBack(): Boolean {
    val tapped = tapTemplate(BUCKET_HOME, "back", timeoutMs = 1500L)
    if (tapped) return true
    val ok = GestureService.pressBack()
    humanDelay()
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

/** 메인 화면이 보일 때까지 뒤로가기를 누름 (최대 [maxBack] 회). */
suspend fun TaskContext.returnToMainMenu(maxBack: Int = 5): Boolean {
    repeat(maxBack) {
        if (isOnMainMenu()) return true
        // 팝업이 가로막고 있으면 먼저 닫기
        if (tapTemplate(BUCKET_HOME, "popup_close", timeoutMs = 800L)) {
            humanDelay()
            return@repeat
        }
        tapBack()
        humanDelay(900L, 200L)
    }
    return isOnMainMenu()
}

/** 필수 템플릿이 빠졌을 때 Task에서 빠르게 종료하기 위한 결과 만들기. */
fun missingAssets(vararg names: String): com.v26macro.runner.TaskResult =
    com.v26macro.runner.TaskResult.Skipped("필수 템플릿 누락: ${names.joinToString(", ")}")
