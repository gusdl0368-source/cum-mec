package com.v26macro.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v26macro.runner.RunnerState

@Composable
fun OverlayPanel(
    state: RunnerState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
) {
    val scheme = darkColorScheme(
        primary = Color(0xFFF97316),
        onPrimary = Color.White,
        background = Color(0xE60B1220),
        surface = Color(0xE6111C2E),
        onSurface = Color.White,
    )
    val accent = Color(0xFFF97316)

    MaterialTheme(colorScheme = scheme) {
        Box(
            modifier = Modifier
                .width(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(scheme.background)
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // ── 헤더 ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(accent.copy(alpha = 0.18f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("V26 매크로", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x33EF4444))
                            .clickable(onClick = onClose),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "닫기", tint = Color(0xFFFCA5A5), modifier = Modifier.size(16.dp))
                    }
                }

                // ── 상태 ──
                StatusLine(state)

                // ── 시작/정지 버튼 ──
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onStart,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("시작", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onStop,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0x331F2937), contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Icon(Icons.Filled.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("정지")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLine(state: RunnerState) {
    val text = when (val s = state) {
        RunnerState.Idle -> "대기 중 — 시작 누르세요"
        is RunnerState.Launching -> "[게임 실행] ${s.progress}"
        is RunnerState.Running -> "[${s.current.label}] ${s.progress}"
        is RunnerState.Done -> if (s.launchOk)
            "완료 — ${s.results.size}개 일과 처리"
        else "실패 — V26 실행 안 됨"
    }
    val isError = state is RunnerState.Done && !state.launchOk
    val color = when {
        isError -> Color(0xFFFCA5A5)
        state is RunnerState.Idle -> Color(0xFF9CA3AF)
        else -> Color(0xFF93C5FD)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x331F2937))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text(text, color = color, fontSize = 11.sp)
    }
}
