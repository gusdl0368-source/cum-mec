package com.v26macro.runner.tasks

import com.v26macro.runner.TaskContext
import com.v26macro.runner.TaskResult
import com.v26macro.util.humanDelay

/**
 * Shared helpers used by every Task: returning to the lobby, dismissing modal
 * popups (이벤트 배너, 보상 알림 등), and checking that we're on the V26 main screen.
 */

const val BUCKET_HOME = "home"

/** Tap "닫기"/"확인" popups until none are detected, then verify we are on the lobby. */
suspend fun TaskContext.dismissPopupsAndReturnToLobby(maxLoops: Int = 6): Boolean {
    repeat(maxLoops) {
        // popups: "닫기", "확인", "x", "다음에"
        val closed = listOf("popup_close", "popup_confirm", "popup_later")
            .any { tapTemplate(BUCKET_HOME, it, timeoutMs = 800L) }
        if (!closed) return@repeat
        humanDelay()
    }
    // Tap the lobby/home button if visible
    tapTemplate(BUCKET_HOME, "lobby_button", timeoutMs = 1500L)
    val onLobby = waitForTemplate(BUCKET_HOME, "lobby_indicator", timeoutMs = 8000L) != null
    if (!onLobby) progress("로비로 복귀 실패")
    return onLobby
}

/** Tag a Task as missing required assets so it can short-circuit cleanly. */
fun missingAssets(vararg names: String): TaskResult =
    TaskResult.Skipped("필수 템플릿 누락: ${names.joinToString(", ")}")
