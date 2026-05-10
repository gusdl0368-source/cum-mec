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
 * 실패 갤러리 — Phase 4 에서 채움.
 *
 * 화면:
 *   - runs/<timestamp>/ 폴더 시간순 리스트
 *   - 항목 탭하면 그 시점 PNG + OCR 결과 (텍스트 박스) + YAML 어디서 막혔는지
 *   - "디스코드로 푸시" 버튼 (선택)
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun FailuresScreen(nav: NavController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("실패 갤러리") }) },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Phase 4 — runs/ 폴더 시간순 + 스크린샷 + OCR 결과",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
