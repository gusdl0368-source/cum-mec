package com.v26macro.v2.flow

import android.graphics.Rect

/**
 * YAML 플로우의 단계 (Step) 정의. PROJECT_PLAN.md 의 DSL 사양 참조.
 *
 * Phase 3 에서 YamlParser 가 이 sealed 계층으로 역직렬화. Runner 가 패턴매칭으로 실행.
 */
sealed class Step {
    data class TapText(
        val text: String,
        val region: Rect? = null,
        val occurrence: Int = 1,
        val threshold: Float = 0.7f,
        val timeoutMs: Long = 5_000L,
    ) : Step()

    data class TapWhenVisible(val text: String, val timeoutMs: Long = 10_000L) : Step()
    data class TapWhenVisibleIfPresent(val text: String, val timeoutMs: Long = 1_500L) : Step()

    data class TapXY(
        val x: Int,
        val y: Int,
        val guardText: String? = null,
    ) : Step()

    data class TapTemplate(val name: String, val threshold: Float = 0.85f) : Step()
    data class WaitText(val text: String, val timeoutMs: Long = 10_000L) : Step()
    data class WaitTextGone(val text: String, val timeoutMs: Long = 10_000L) : Step()
    data class Back(val count: Int = 1) : Step()
    data class Sleep(val seconds: Double) : Step()
    data class Loop(val until: Condition, val max: Int, val body: List<Step>) : Step()
    data class Branch(
        val condition: Condition,
        val then: List<Step>,
        val otherwise: List<Step> = emptyList(),
    ) : Step()
    data class Macro(val name: String) : Step()
    data class LlmFallback(val intent: String) : Step()
}

sealed class Condition {
    data class TextVisible(val text: String) : Condition()
    data class TextNotVisible(val text: String) : Condition()
    data class TextCountGte(val text: String, val n: Int) : Condition()
    data class TemplateMatched(val name: String) : Condition()
    data class Or(val left: Condition, val right: Condition) : Condition()
    data class And(val left: Condition, val right: Condition) : Condition()
}

data class Flow(
    val name: String,
    val require: Condition? = null,
    val steps: List<Step>,
)
