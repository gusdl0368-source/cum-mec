package com.v26macro.v2.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun PermissionsScreen(state: PermissionState, onAllGranted: () -> Unit) {
    LaunchedEffect(state.allGranted) { if (state.allGranted) onAllGranted() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("권한 설정") }) },
    ) { padding ->
        PermissionsList(state = state, padding = padding)
    }
}

@Composable
private fun PermissionsList(state: PermissionState, padding: PaddingValues) {
    val context = LocalContext.current
    val items = listOf(
        PermissionEntry(
            title = "알림",
            description = "포그라운드 서비스 (화면 캡처/매크로 실행) 표시에 필요.",
            granted = state.notification,
            actionLabel = "권한 요청",
            launcher = NotificationLauncher,
        ),
        PermissionEntry(
            title = "다른 앱 위에 표시",
            description = "라이브 뷰/컨트롤 패널을 V26 위에 띄우기 위해 필요.",
            granted = state.overlay,
            actionLabel = "설정 열기",
            launcher = OverlayLauncher,
        ),
        PermissionEntry(
            title = "접근성 (탭/스와이프)",
            description = "AccessibilityService 활성화 후 V26 매크로를 토글 ON.",
            granted = state.accessibility,
            actionLabel = "설정 열기",
            launcher = AccessibilityLauncher,
        ),
        PermissionEntry(
            title = "화면 캡처 (MediaProjection)",
            description = "에뮬레이터 화면을 OCR/매칭 위해 캡처. 처음 1회 동의.",
            granted = state.mediaProjection,
            actionLabel = "동의 다이얼로그",
            launcher = MediaProjectionLauncher,
        ),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(items) { entry -> PermissionRow(entry, context) }
    }
}

private data class PermissionEntry(
    val title: String,
    val description: String,
    val granted: Boolean,
    val actionLabel: String,
    val launcher: PermLauncher,
)

@Composable
private fun PermissionRow(entry: PermissionEntry, context: Context) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (entry.granted) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (entry.granted) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(0.dp).fillMaxWidth(0f))
                }
                Text(
                    entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = if (entry.granted) 8.dp else 0.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                entry.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!entry.granted) {
                Spacer(Modifier.height(12.dp))
                LaunchButton(entry.launcher, label = entry.actionLabel)
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    "허용됨",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

private sealed interface PermLauncher
private object NotificationLauncher : PermLauncher
private object OverlayLauncher : PermLauncher
private object AccessibilityLauncher : PermLauncher
private object MediaProjectionLauncher : PermLauncher

@Composable
private fun LaunchButton(which: PermLauncher, label: String) {
    val context = LocalContext.current
    when (which) {
        is NotificationLauncher -> {
            if (Build.VERSION.SDK_INT >= 33) {
                val launcher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) { /* 결과는 lifecycle resume 에서 PermissionState 가 다시 평가 */ }
                Button(onClick = { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                    Text(label)
                }
            } else {
                Text("자동 허용 (Android 12 이하)")
            }
        }
        is OverlayLauncher -> {
            Button(onClick = { context.startActivity(overlaySettingsIntent(context)) }) {
                Text(label)
            }
        }
        is AccessibilityLauncher -> {
            Button(onClick = { context.startActivity(accessibilitySettingsIntent()) }) {
                Text(label)
            }
        }
        is MediaProjectionLauncher -> {
            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult(),
            ) { result ->
                val data: Intent? = result.data
                if (result.resultCode == Activity.RESULT_OK && data != null) {
                    MediaProjectionHolder.set(result.resultCode, data)
                }
            }
            Button(onClick = {
                val mpm = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE)
                    as MediaProjectionManager
                launcher.launch(mpm.createScreenCaptureIntent())
            }) {
                Text(label)
            }
            OutlinedButton(
                onClick = { MediaProjectionHolder.clear() },
                modifier = Modifier.padding(top = 4.dp),
            ) { Text("동의 초기화") }
        }
    }
}
