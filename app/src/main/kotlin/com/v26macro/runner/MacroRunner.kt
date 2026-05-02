package com.v26macro.runner

import android.content.Context
import com.v26macro.overlay.MacroSettings
import com.v26macro.runner.tasks.HomeRunRaceTask
import com.v26macro.runner.tasks.LaunchGame
import com.v26macro.runner.tasks.LeagueModeTask
import com.v26macro.runner.tasks.PointShopTask
import com.v26macro.runner.tasks.RankingChallengeTask
import com.v26macro.runner.tasks.SpecialMatchTask
import com.v26macro.runner.tasks.SponsorPayoutTask
import com.v26macro.runner.tasks.Task
import com.v26macro.runner.tasks.BUCKET_HOME
import com.v26macro.runner.tasks.isOnMainMenu
import com.v26macro.runner.tasks.returnToMainMenu
import com.v26macro.util.Logger
import com.v26macro.vision.CoordLibrary
import com.v26macro.vision.TemplateLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 사용자 설정(MacroSettings)에 따라 일과를 순차 실행한다:
 *   - 후원금 정산: ON 이면 1회
 *   - 포인트상점:   ON 이면 1회
 *   - 홈런레이스:   homerunCount 만큼 (0 이면 스킵)
 *   - 스페셜매치:   specialMatchCount 만큼 (0 이면 스킵)
 *   - 랭킹/리그:    ON 이면 1회 (베타)
 */
class MacroRunner(private val appContext: Context) {

    private val library = TemplateLibrary(appContext)
    private val coords = CoordLibrary(appContext)
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var job: Job? = null

    private val _state = MutableStateFlow<RunnerState>(RunnerState.Idle)
    val state: StateFlow<RunnerState> = _state.asStateFlow()

    val isRunning: Boolean
        get() = _state.value is RunnerState.Running || _state.value is RunnerState.Launching

    fun start() {
        if (isRunning) {
            Logger.w("start() ignored - already running")
            return
        }
        job = scope.launch {
            val cfg = MacroSettings.snapshot(appContext)
            Logger.i("MacroRunner config: $cfg")

            // ── 종료 다이얼로그 watchdog ──
            // BACK 이 의도치 않게 더 눌리거나 시스템에서 BACK 이벤트가 발생해서
            // "게임을 종료하시겠습니까?" 다이얼로그가 떴을 때 매크로 어떤 단계에서든
            // 자동으로 취소를 누르도록 백그라운드에서 감시. 부모 코루틴(매크로)이
            // 끝나거나 stop() 으로 취소되면 자동으로 같이 종료됨.
            launch {
                val watchCtx = TaskContext(library, coords) { /* 진행 메시지 안 갱신 */ }
                while (isActive) {
                    try {
                        delay(900L)
                        if (watchCtx.find(BUCKET_HOME, "exit_cancel") != null) {
                            Logger.w("watchdog: 게임 종료 다이얼로그 감지 - 자동 취소")
                            watchCtx.tapTemplate(BUCKET_HOME, "exit_cancel", timeoutMs = 1500L)
                        }
                    } catch (t: Throwable) {
                        Logger.w("watchdog 오류 (계속 동작): ${t.message}")
                    }
                }
            }

            val tasks: List<Task> = buildList {
                if (cfg.sponsorEnabled) add(SponsorPayoutTask())
                if (cfg.pointShopEnabled) add(PointShopTask())
                if (cfg.homerunCount > 0) add(HomeRunRaceTask(maxRounds = cfg.homerunCount))
                if (cfg.specialMatchCount > 0) add(SpecialMatchTask(maxRounds = cfg.specialMatchCount))
                if (cfg.rankingEnabled) add(RankingChallengeTask())
                if (cfg.leagueEnabled) add(LeagueModeTask())
            }
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
            for ((idx, task) in tasks.withIndex()) {
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

                // 다음 task 가 있으면 메인 화면임을 검증/복구
                val isLast = idx == tasks.lastIndex
                if (!isLast) {
                    if (!ctx.isOnMainMenu()) {
                        Logger.w("task ${task.kind} 후 메인 인식 실패 - 복귀 시도")
                        _state.value = RunnerState.Running(task.kind, "메인 복귀 중")
                        val recovered = ctx.returnToMainMenu(maxBack = 6)
                        if (!recovered) {
                            Logger.w("메인 복귀 실패 - 다음 task 진행 (실패 가능)")
                        }
                    } else {
                        Logger.i("task ${task.kind} 후 메인 확인 완료")
                    }
                }
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
