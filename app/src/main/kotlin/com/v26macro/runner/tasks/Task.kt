package com.v26macro.runner.tasks

import com.v26macro.runner.TaskContext
import com.v26macro.runner.TaskKind
import com.v26macro.runner.TaskResult

/**
 * One automation step. Implementations are small state machines that drive the
 * V26 UI by repeatedly: find template -> tap -> wait for next template.
 *
 * NOTE: The supplied implementations are SCAFFOLDS. They show the expected
 * sequence and call out which template assets are required. The user must
 * crop screenshots from V26 into app/src/main/assets/templates/<bucket>/.
 */
interface Task {
    val kind: TaskKind
    suspend fun run(ctx: TaskContext): TaskResult
}
