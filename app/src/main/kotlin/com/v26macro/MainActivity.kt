package com.v26macro

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.v26macro.capture.ScreenCaptureService
import com.v26macro.input.GestureService
import com.v26macro.overlay.MacroConfig
import com.v26macro.overlay.MacroSettings
import com.v26macro.overlay.OverlayService
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var projectionManager: MediaProjectionManager

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result not needed */ }

    private val projectionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            ScreenCaptureService.start(this, result.resultCode, result.data!!)
            OverlayService.start(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

        setContent {
            MaterialTheme(colorScheme = AppColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppColors.background,
                ) {
                    HomeScreen(
                        onRequestNotif = ::requestNotificationPermission,
                        onRequestOverlay = ::requestOverlayPermission,
                        onRequestAccessibility = ::openAccessibilitySettings,
                        onLaunch = ::startCapture,
                        onStop = ::stopAll,
                    )
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            )
        }
    }

    private fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun startCapture() {
        projectionLauncher.launch(projectionManager.createScreenCaptureIntent())
    }

    private fun stopAll() {
        ScreenCaptureService.stop(this)
        OverlayService.stop(this)
    }
}

// ── 색상 ──────────────────────────────────────────────────────────────
private val AppColors = darkColorScheme(
    primary = Color(0xFFF97316),
    onPrimary = Color.White,
    secondary = Color(0xFF38BDF8),
    background = Color(0xFF0B1220),
    surface = Color(0xFF111C2E),
    onSurface = Color(0xFFE5E7EB),
    surfaceVariant = Color(0xFF1B2740),
    onSurfaceVariant = Color(0xFFB8C2D6),
)

private val Accent = Color(0xFFF97316)
private val Muted = Color(0xFF6B7280)
private val Good = Color(0xFF22C55E)

// ── 메인 화면 ─────────────────────────────────────────────────────────
@Composable
private fun HomeScreen(
    onRequestNotif: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onLaunch: () -> Unit,
    onStop: () -> Unit,
) {
    val ctx = LocalContext.current
    val cfg by MacroSettings.config(ctx).collectAsState(initial = MacroConfig())
    val scope = rememberCoroutineScope()

    var notifGranted by remember { mutableStateOf(notifOk(ctx)) }
    var overlayGranted by remember { mutableStateOf(canDrawOverlays(ctx)) }
    var accessibilityGranted by remember { mutableStateOf(isAccessibilityEnabled(ctx)) }

    // 화면 복귀 시 권한 상태 갱신 (다른 설정 화면 다녀온 경우)
    LaunchedEffect(Unit) {
        notifGranted = notifOk(ctx)
        overlayGranted = canDrawOverlays(ctx)
        accessibilityGranted = isAccessibilityEnabled(ctx)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Header()
        PermissionsCard(
            notif = notifGranted,
            overlay = overlayGranted,
            accessibility = accessibilityGranted,
            onRequestNotif = {
                onRequestNotif()
                notifGranted = notifOk(ctx)
            },
            onRequestOverlay = {
                onRequestOverlay()
                overlayGranted = canDrawOverlays(ctx)
            },
            onRequestAccessibility = {
                onRequestAccessibility()
                accessibilityGranted = isAccessibilityEnabled(ctx)
            },
        )

        SectionLabel("일과 설정")

        ToggleRow(
            icon = Icons.Filled.AttachMoney,
            label = "후원금 정산",
            subtitle = "활성화 시 1회 실행",
            checked = cfg.sponsorEnabled,
            onChecked = { v -> scope.launch { MacroSettings.update(ctx) { it.copy(sponsorEnabled = v) } } },
        )

        ToggleRow(
            icon = Icons.Filled.Storefront,
            label = "포인트상점",
            subtitle = "활성화 시 1회 실행",
            checked = cfg.pointShopEnabled,
            onChecked = { v -> scope.launch { MacroSettings.update(ctx) { it.copy(pointShopEnabled = v) } } },
        )

        StepperRow(
            icon = Icons.Filled.SportsBaseball,
            label = "홈런레이스",
            value = cfg.homerunCount,
            min = 0,
            max = MacroSettings.HOMERUN_MAX,
            unit = "라운드",
            onChange = { v -> scope.launch { MacroSettings.update(ctx) { it.copy(homerunCount = v) } } },
        )

        StepperRow(
            icon = Icons.Filled.EmojiEvents,
            label = "스페셜매치",
            value = cfg.specialMatchCount,
            min = 0,
            max = MacroSettings.SPECIALMATCH_MAX,
            unit = "매치",
            onChange = { v -> scope.launch { MacroSettings.update(ctx) { it.copy(specialMatchCount = v) } } },
        )

        ToggleRow(
            icon = Icons.Filled.Leaderboard,
            label = "랭킹챌린지 (베타)",
            subtitle = "흐름 정의 전이라 실패할 수 있음",
            checked = cfg.rankingEnabled,
            onChecked = { v -> scope.launch { MacroSettings.update(ctx) { it.copy(rankingEnabled = v) } } },
        )

        ToggleRow(
            icon = Icons.Filled.Groups,
            label = "리그모드 (베타)",
            subtitle = "흐름 정의 전이라 실패할 수 있음",
            checked = cfg.leagueEnabled,
            onChecked = { v -> scope.launch { MacroSettings.update(ctx) { it.copy(leagueEnabled = v) } } },
        )

        Spacer(Modifier.height(8.dp))
        ActionButtons(onLaunch = onLaunch, onStop = onStop)

        Disclaimer()
    }
}

// ── UI 빌딩 블록 ──────────────────────────────────────────────────────
@Composable
private fun Header() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Accent.copy(alpha = 0.15f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Bolt, contentDescription = null, tint = Accent)
        }
        Column {
            Text(
                "V26 매크로",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "에뮬레이터 자동화 컨트롤",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun PermissionsCard(
    notif: Boolean,
    overlay: Boolean,
    accessibility: Boolean,
    onRequestNotif: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.NotificationsActive, contentDescription = null, tint = Accent)
                Spacer(Modifier.width(8.dp))
                Text("권한 설정", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
            PermissionRow("알림 권한", notif, onRequestNotif)
            PermissionRow("오버레이(다른 앱 위에 그리기)", overlay, onRequestOverlay)
            PermissionRow("접근성 서비스", accessibility, onRequestAccessibility)
        }
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onTap: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (granted) Icons.Filled.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (granted) Good else Muted,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Button(
            onClick = onTap,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (granted) MaterialTheme.colorScheme.surfaceVariant else Accent,
                contentColor = if (granted) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
            ),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(if (granted) "OK" else "허용", fontSize = 12.sp)
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Filled.Settings, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ToggleRow(
    icon: ImageVector,
    label: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(icon)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
            Switch(checked = checked, onCheckedChange = onChecked)
        }
    }
}

@Composable
private fun StepperRow(
    icon: ImageVector,
    label: String,
    value: Int,
    min: Int,
    max: Int,
    unit: String,
    onChange: (Int) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(icon)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(label, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                Text(
                    if (value == 0) "비활성 (0)" else "$value $unit",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                )
            }
            StepperButton("−", enabled = value > min) { onChange((value - 1).coerceAtLeast(min)) }
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    value.toString(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
            }
            StepperButton("+", enabled = value < max) { onChange((value + 1).coerceAtMost(max)) }
        }
    }
}

@Composable
private fun StepperButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(34.dp)
            .background(
                if (enabled) Accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(10.dp),
            ),
    ) {
        Text(label, color = if (enabled) Accent else Muted, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(Accent.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun ActionButtons(onLaunch: () -> Unit, onStop: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Button(
            onClick = onLaunch,
            modifier = Modifier.weight(1f).height(54.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.White),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("매크로 시작", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Button(
            onClick = onStop,
            modifier = Modifier.height(54.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            shape = RoundedCornerShape(14.dp),
        ) {
            Icon(Icons.Filled.Stop, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("정지")
        }
    }
}

@Composable
private fun Disclaimer() {
    Text(
        "⚠ 본 앱은 개인 학습/연구 용도로만 사용하세요. 게임 매크로는 컴투스 이용약관에 위배될 수 있습니다.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
    )
}

// ── 권한 체크 헬퍼 ──────────────────────────────────────────────────
private fun notifOk(ctx: Context): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
        android.content.pm.PackageManager.PERMISSION_GRANTED
}

private fun canDrawOverlays(ctx: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(ctx)

private fun isAccessibilityEnabled(ctx: Context): Boolean {
    val expected = ComponentName(ctx, GestureService::class.java).flattenToString()
    val enabled = Settings.Secure.getString(
        ctx.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}

