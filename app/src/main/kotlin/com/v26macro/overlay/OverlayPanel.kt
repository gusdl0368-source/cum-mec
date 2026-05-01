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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v26macro.runner.RunnerState

/**
 * 매크로 컨트롤 오버레이.
 *
 * 기본 상태는 **48dp 작은 동그라미** — 화면 캡처 매칭을 거의 가리지 않는다. 탭하면
 * 작은 컨트롤 패널이 펼쳐지고, 매크로가 실행 중(Launching/Running)이면 자동으로
 * 다시 동그라미로 접혀 화면 가림을 최소화한다.
 *
 * 색상으로 상태 표시:
 *   - 회색: 대기
 *   - 노랑: 게임 실행 중
 *   - 주황: 매크로 진행 중
 *   - 초록: 완료
 *   - 빨강: 오류
 */
@Composable
fun OverlayPanel(
    state: RunnerState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    // 매크로가 돌기 시작하면 자동으로 접어서 화면 가림 최소화
    LaunchedEffect(state) {
        if (state is RunnerState.Launching || state is RunnerState.Running) {
            expanded = false
        }
    }

    val scheme = darkColorScheme(
        primary = Color(0xFFF97316),
        onPrimary = Color.White,
        background = Color(0xE60B1220),
        onSurface = Color.White,
    )

    MaterialTheme(colorScheme = scheme) {
        if (expanded) {
            ExpandedPanel(
                state = state,
                onStart = { expanded = false; onStart() },
                onStop = onStop,
                onClose = onClose,
                onCollapse = { expanded = false },
            )
        } else {
            CollapsedBall(
                state = state,
                onTap = { expanded = true },
            )
        }
    }
}

// ── 접힌 상태: 작은 동그라미 ─────────────────────────────────────
@Composable
private fun CollapsedBall(state: RunnerState, onTap: () -> Unit) {
    val color = when (state) {
        is RunnerState.Idle -> Color(0xFF6B7280)
        is RunnerState.Launching -> Color(0xFFFBBF24)
        is RunnerState.Running -> Color(0xFFF97316)
        is RunnerState.Done -> if (state.launchOk) Color(0xFF22C55E) else Color(0xFFEF4444)
    }
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.85f))
            .clickable(onClick = onTap),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Bolt,
            contentDescription = "매크로 패널 열기",
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
    }
}

// ── 펼친 상태: 작은 패널 (180dp) ─────────────────────────────────
@Composable
private fun ExpandedPanel(
    state: RunnerState,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
    onCollapse: () -> Unit,
) {
    val accent = Color(0xFFF97316)
    Box(
        modifier = Modifier
            .width(190.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xE60B1220))
            .padding(8.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            // 헤더: 작게 보이기 / 닫기
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "V26 매크로",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                MiniIconButton(
                    icon = Icons.Filled.UnfoldLess,
                    tint = Color(0xFFE5E7EB),
                    bg = Color(0x331F2937),
                    onClick = onCollapse,
                )
                Spacer(Modifier.width(4.dp))
                MiniIconButton(
                    icon = Icons.Filled.Close,
                    tint = Color(0xFFFCA5A5),
                    bg = Color(0x33EF4444),
                    onClick = onClose,
                )
            }

            // 상태
            StatusLine(state)

            // 시작/정지
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                ActionPill(
                    icon = Icons.Filled.PlayArrow,
                    label = "시작",
                    bg = accent,
                    fg = Color.White,
                    onClick = onStart,
                    modifier = Modifier.weight(1f),
                )
                ActionPill(
                    icon = Icons.Filled.Stop,
                    label = "정지",
                    bg = Color(0x331F2937),
                    fg = Color.White,
                    onClick = onStop,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun MiniIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    bg: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun ActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    bg: Color,
    fg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(3.dp))
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun StatusLine(state: RunnerState) {
    val text = when (val s = state) {
        RunnerState.Idle -> "대기"
        is RunnerState.Launching -> "[실행] ${s.progress}"
        is RunnerState.Running -> "[${s.current.label}] ${s.progress}"
        is RunnerState.Done -> if (s.launchOk) "완료 — ${s.results.size}개" else "실패"
    }
    val color = when {
        state is RunnerState.Done && !state.launchOk -> Color(0xFFFCA5A5)
        state is RunnerState.Idle -> Color(0xFF9CA3AF)
        else -> Color(0xFF93C5FD)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0x331F2937))
            .padding(horizontal = 6.dp, vertical = 5.dp),
    ) {
        Text(text, color = color, fontSize = 10.sp, maxLines = 2)
    }
}
