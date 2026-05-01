package com.v26macro.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.v26macro.runner.RunnerState
import com.v26macro.runner.TaskKind

@Composable
fun OverlayPanel(
    state: RunnerState,
    enabled: Set<TaskKind>,
    onToggle: (TaskKind) -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onClose: () -> Unit,
) {
    val scheme = darkColorScheme(
        primary = Color(0xFFF97316),
        onPrimary = Color.White,
        background = Color(0xCC0F172A),
        surface = Color(0xCC0F172A),
        onSurface = Color.White,
    )
    MaterialTheme(colorScheme = scheme) {
        Box(
            modifier = Modifier
                .width(280.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(scheme.background)
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "V26 매크로",
                        color = Color.White,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        "닫기",
                        color = Color(0xFFFCA5A5),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x33FF4444))
                            .clickable(onClick = onClose)
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                Text(
                    text = when (val s = state) {
                        RunnerState.Idle -> "대기"
                        is RunnerState.Launching -> "[게임 실행] ${s.progress}"
                        is RunnerState.Running -> "[${s.current.label}] ${s.progress}"
                        is RunnerState.Done -> if (s.launchOk)
                            "완료: ${s.results.size}개 태스크"
                        else "게임 실행 실패"
                    },
                    color = Color(0xFFE5E7EB),
                    style = MaterialTheme.typography.bodySmall,
                )

                TaskKind.values().forEach { kind ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(checked = kind in enabled, onCheckedChange = { onToggle(kind) })
                        Text(kind.label, color = Color.White)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onStart, modifier = Modifier.weight(1f)) {
                        Text("시작")
                    }
                    Button(onClick = onStop, modifier = Modifier.weight(1f)) {
                        Text("정지")
                    }
                }
            }
        }
    }
}
