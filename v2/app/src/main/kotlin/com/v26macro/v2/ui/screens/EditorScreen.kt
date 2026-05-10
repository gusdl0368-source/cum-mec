package com.v26macro.v2.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

/**
 * YAML 편집기 — Phase 4 에서 채움.
 *
 * 화면:
 *   - 좌측: flows/ 파일 목록 (sponsor / pointshop / homerunrace / specialmatch /
 *           rankingchallenge / leaguemode)
 *   - 우측: 멀티라인 텍스트 에디터 (간단 신택스 하이라이트는 보너스)
 *   - 저장 시 즉시 반영 (다음 RUN 부터 새 YAML 사용)
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun EditorScreen(nav: NavController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("YAML 편집기") }) },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Phase 4 — flows/ 파일 인-앱 편집",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
