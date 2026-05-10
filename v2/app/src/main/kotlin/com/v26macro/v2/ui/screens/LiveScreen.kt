package com.v26macro.v2.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
 * 라이브 뷰 — Phase 2 에서 채움.
 *
 * 화면:
 *   - 상단 2/3: 마지막 캡처 + OCR 박스 오버레이 (1280×720 → 화면 폭 맞춤 스케일)
 *   - 하단 1/3: 실시간 로그 스트림 (텍스트)
 *   - 우상단: REC 버튼 (Phase 4 트레이스 레코더)
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun LiveScreen(nav: NavController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("라이브 뷰") }) },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Phase 2 — 캡처 + OCR + 박스 오버레이",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "예정: SurfaceView 위에 Compose Canvas 로 OCR 박스",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
