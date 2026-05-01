package com.v26macro.runner

enum class TaskKind(val label: String, val bucket: String) {
    SponsorPayout("후원금 정산", "sponsor"),
    PointShop("포인트상점", "pointshop"),
    HomeRunRace("홈런레이스", "homerunrace"),
    SpecialMatch("스페셜매치", "specialmatch"),
    RankingChallenge("랭킹챌린지", "rankingchallenge"),
    LeagueMode("리그모드", "leaguemode"),
}

sealed class TaskResult {
    data object Success : TaskResult()
    data class Skipped(val reason: String) : TaskResult()
    data class Failed(val reason: String) : TaskResult()
}

sealed class RunnerState {
    data object Idle : RunnerState()
    data class Launching(val progress: String) : RunnerState()
    data class Running(val current: TaskKind, val progress: String) : RunnerState()
    data class Done(
        val results: Map<TaskKind, TaskResult>,
        val launchOk: Boolean = true,
    ) : RunnerState()
}
