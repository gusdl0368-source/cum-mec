package com.v26macro.runner

import android.content.Context
import com.v26macro.runner.tasks.HomeRunRaceTask
import com.v26macro.runner.tasks.LaunchGame
import com.v26macro.runner.tasks.LeagueModeTask
import com.v26macro.runner.tasks.PointShopTask
import com.v26macro.runner.tasks.RankingChallengeTask
import com.v26macro.runner.tasks.SpecialMatchTask
import com.v26macro.runner.tasks.SponsorPayoutTask
import com.v26macro.runner.tasks.Task
import com.v26macro.util.Logger
import com.v26macro.vision.CoordLibrary
import com.v26macro.vision.TemplateLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Orchestrates the daily routine across the six tasks. Tasks run sequentially in
 * the order the user requested (후원금 → 포인트상점 → 홈런레이스 → 스페셜매치 →
 * 랭킹챌린지 → 리그모드). Each task is independent — failures don't abort the
 * sequence; they are recorded and the next task still runs.
 */
class MacroRunner(appContext: Context) {

    private val library = TemplateLibrary(appContext)
    private val coords = CoordLibrary(appContext)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null

    private val _state = MutableStateFlow<RunnerState>(RunnerState.Idle)
    val state: StateFlow<RunnerState> = _state.asStateFlow()

    private val allTasks: List<Task> = listOf(
        SponsorPayoutTask(),
        PointShopTask(),
        HomeRunRaceTask(),
        SpecialMatchTask(),
        RankingChallengeTask(),
        LeagueModeTask(),
    )

    val isRunning: Boolean
        get() = _state.value is RunnerState.Running || _state.value is RunnerState.Launching

    fun start(enabled: Set<TaskKind>) {
        if (isRunning) {
            Logger.w("start() ignored - already running")
            return
        }
        job = scope.launch {
            val results = mutableMapOf<TaskKind, TaskResult>()

            // ── Prelude: V26 자동 실행 ──
            _state.value = RunnerState.Launching("V26 실행 중")
            val launchCtx = TaskContext(library, coords) { msg ->
                _state.value = RunnerState.Launching(msg)
            }
            val launched = try {
                LaunchGame.run(launchCtx) { msg ->
                    _state.value = RunnerState.Launching(msg)
                }
            } catch (t: Throwable) {
                Logger.e("LaunchGame crashed", t)
                false
            }
            if (!launched) {
                _state.value = RunnerState.Done(results, launchOk = false)
                return@launch
            }

            // ── Tasks ──
            val tasks = allTasks.filter { it.kind in enabled }
            for (task in tasks) {
                _state.value = RunnerState.Running(task.kind, "${task.kind.label} 진행 중")
                val ctx = TaskContext(library, coords) { msg ->
                    _state.value = RunnerState.Running(task.kind, msg)
                }
                val result = try {
                    task.run(ctx)
                } catch (t: Throwable) {
                    Logger.e("task ${task.kind} crashed", t)
                    TaskResult.Failed(t.message ?: "예외")
                }
                Logger.i("task ${task.kind} -> $result")
                results[task.kind] = result
            }
            _state.value = RunnerState.Done(results, launchOk = true)
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        _state.value = RunnerState.Idle
    }

    fun shutdown() {
        scope.cancel()
        library.clear()
    }
}
