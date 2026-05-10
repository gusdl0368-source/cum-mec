package com.v26macro.v2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.v26macro.v2.permissions.PermissionsScreen
import com.v26macro.v2.permissions.rememberPermissionState
import com.v26macro.v2.ui.AppTheme
import com.v26macro.v2.ui.Routes
import com.v26macro.v2.ui.screens.EditorScreen
import com.v26macro.v2.ui.screens.FailuresScreen
import com.v26macro.v2.ui.screens.HomeScreen
import com.v26macro.v2.ui.screens.LiveScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    AppRoot()
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun AppRoot() {
    val nav = rememberNavController()
    val perms = rememberPermissionState()

    val startDestination = if (perms.allGranted) Routes.HOME else Routes.PERMISSIONS

    NavHost(navController = nav, startDestination = startDestination) {
        composable(Routes.PERMISSIONS) {
            PermissionsScreen(
                state = perms,
                onAllGranted = {
                    nav.navigate(Routes.HOME) {
                        popUpTo(Routes.PERMISSIONS) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) { HomeScreen(nav) }
        composable(Routes.LIVE) { LiveScreen(nav) }
        composable(Routes.EDITOR) { EditorScreen(nav) }
        composable(Routes.FAILURES) { FailuresScreen(nav) }
    }
}
