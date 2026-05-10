package com.v26macro.v2.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.runtime.DisposableEffect
import com.v26macro.v2.input.GestureAccessibilityService

/**
 * 권한 4종 상태를 라이브로 관찰. 사용자가 시스템 설정 다녀와도 onResume 에서 재평가.
 *
 * MediaProjection 은 Activity Result 라 별도로 처리 (PermissionsScreen 에서).
 */
class PermissionState internal constructor(
    val notification: Boolean,
    val overlay: Boolean,
    val accessibility: Boolean,
    val mediaProjection: Boolean,
) {
    val allGranted: Boolean
        get() = notification && overlay && accessibility && mediaProjection
}

@Composable
fun rememberPermissionState(): PermissionState {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var tick by remember { mutableStateOf(0) }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) tick++
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    // tick 변경되면 다시 평가
    @Suppress("UNUSED_EXPRESSION") tick
    return PermissionState(
        notification = hasNotificationPermission(context),
        overlay = Settings.canDrawOverlays(context),
        accessibility = GestureAccessibilityService.isEnabled(context),
        mediaProjection = MediaProjectionHolder.hasProjection,
    )
}

private fun hasNotificationPermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= 33) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

internal fun overlaySettingsIntent(context: Context): Intent =
    Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}")
    )

internal fun accessibilitySettingsIntent(): Intent =
    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
