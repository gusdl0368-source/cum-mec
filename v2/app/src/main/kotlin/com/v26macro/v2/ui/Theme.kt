package com.v26macro.v2.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    primary = Color(0xFF7C5CFF),
    secondary = Color(0xFF7DD3FC),
    background = Color(0xFF0F172A),
    surface = Color(0xFF111827),
)

private val Light = lightColorScheme(
    primary = Color(0xFF6D28D9),
    secondary = Color(0xFF0284C7),
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val scheme = if (isSystemInDarkTheme()) Dark else Light
    MaterialTheme(colorScheme = scheme, content = content)
}

object Routes {
    const val PERMISSIONS = "permissions"
    const val HOME = "home"
    const val LIVE = "live"
    const val EDITOR = "editor"
    const val FAILURES = "failures"
}
