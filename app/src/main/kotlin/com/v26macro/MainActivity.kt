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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.v26macro.capture.ScreenCaptureService
import com.v26macro.input.GestureService
import com.v26macro.overlay.OverlayService

class MainActivity : ComponentActivity() {

    private lateinit var projectionManager: MediaProjectionManager

    private val notifPermLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* result not needed - user can re-tap */ }

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
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Home(
                        onRequestNotif = ::requestNotificationPermission,
                        onRequestOverlay = ::requestOverlayPermission,
                        onRequestAccessibility = ::openAccessibilitySettings,
                        onStartCapture = ::startCapture,
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

@Composable
private fun Home(
    onRequestNotif: () -> Unit,
    onRequestOverlay: () -> Unit,
    onRequestAccessibility: () -> Unit,
    onStartCapture: () -> Unit,
    onStop: () -> Unit,
) {
    val ctx = LocalContext.current
    var accessibilityOk by remember { mutableStateOf(isAccessibilityEnabled(ctx)) }
    var overlayOk by remember { mutableStateOf(canDrawOverlays(ctx)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("V26 매크로", style = MaterialTheme.typography.headlineSmall)
        Text(
            "에뮬레이터에서 V26을 실행한 뒤 아래 권한을 모두 허용하고 '시작'을 누르세요. " +
                "오버레이 패널에서 일과를 선택하고 동작을 켤 수 있습니다."
        )

        Spacer(Modifier.height(8.dp))

        Button(onClick = onRequestNotif) { Text("1. 알림 권한 허용") }
        Button(onClick = {
            onRequestOverlay()
            overlayOk = canDrawOverlays(ctx)
        }) {
            Text(if (overlayOk) "2. 오버레이 권한 OK" else "2. 오버레이 권한 허용")
        }
        Button(onClick = {
            onRequestAccessibility()
            accessibilityOk = isAccessibilityEnabled(ctx)
        }) {
            Text(if (accessibilityOk) "3. 접근성 서비스 OK" else "3. 접근성 서비스 켜기")
        }

        Spacer(Modifier.height(12.dp))

        Button(onClick = onStartCapture) { Text("4. 화면 캡처 시작 + 오버레이 띄우기") }
        Button(onClick = onStop) { Text("정지") }

        Spacer(Modifier.height(20.dp))
        Text(
            "주의: 게임 매크로는 컴투스 이용약관에 위배될 수 있습니다. 본 앱은 개인 학습/연구 용도로만 사용하세요.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
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
