package com.v26macro.v2.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.v26macro.v2.ui.Routes

/**
 * 메인 화면 — Phase 1 stub.
 *
 * Phase 4 에서 추가:
 *   - DataStore 로 토글/스케줄/디스코드 URL 영속화
 *   - 시작/정지 → MacroService 트리거
 *   - "다음 자동 실행" 카운트다운 표시
 */
@Composable
fun HomeScreen(nav: NavController) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("V26 매크로") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TaskTogglesCard()
            ActionsCard()
            NavigationCard(nav)
        }
    }
}

@Composable
private fun TaskTogglesCard() {
    Card(elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "태스크",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            listOf(
                "후원금 정산",
                "포인트상점",
                "홈런레이스",
                "스페셜매치",
                "랭킹챌린지",
                "리그모드",
            ).forEach { TaskRow(it) }
        }
    }
}

@Composable
private fun TaskRow(name: String) {
    var enabled by remember { mutableStateOf(true) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = enabled, onCheckedChange = { enabled = it })
    }
}

@Composable
private fun ActionsCard() {
    Card(elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "실행",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { /* Phase 3 */ },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("지금 실행 (Phase 3)") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { /* Phase 4 */ },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("일일 스케줄 설정 (Phase 4)") }
        }
    }
}

@Composable
private fun NavigationCard(nav: NavController) {
    Card(elevation = CardDefaults.cardElevation(1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "디버깅",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { nav.navigate(Routes.LIVE) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("라이브 뷰") }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = { nav.navigate(Routes.EDITOR) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("YAML 편집기") }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = { nav.navigate(Routes.FAILURES) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("실패 갤러리") }
        }
    }
}
