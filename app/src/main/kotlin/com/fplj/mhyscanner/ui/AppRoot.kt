package com.fplj.mhyscanner.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.fplj.mhyscanner.MainViewModel
import kotlinx.coroutines.launch

@Composable
fun AppRoot(vm: MainViewModel, onRequestScreenCapture: () -> Unit) {
    val uiState by vm.uiState.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.messages.collect { msg ->
            scope.launch { snackbarHostState.showSnackbar(msg) }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.PlayArrow, null) },
                    label = { Text("直播") },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer)
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Videocam, null) },
                    label = { Text("屏幕") },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer)
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.AccountCircle, null) },
                    label = { Text("账号") },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer)
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(Icons.Filled.Settings, null) },
                    label = { Text("设置") },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = MaterialTheme.colorScheme.primaryContainer)
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val reduceMotion = isReduceMotionEnabled()
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    // 页面切换:进出同路径、同节奏的弹簧,方向一致(向上),保持空间连续、可中断。
                    // 系统减弱动效时退化为纯淡入淡出。
                    if (reduceMotion) {
                        (fadeIn(AppSpring.Default) togetherWith fadeOut(AppSpring.Default))
                    } else {
                        (fadeIn(AppSpring.Default) +
                            slideInVertically(AppSpring.Slide) { it / 24 }) togetherWith
                            (fadeOut(AppSpring.Default) +
                                slideOutVertically(AppSpring.Slide) { -it / 24 })
                    }
                },
                label = "tabContent"
            ) { target ->
                when (target) {
                    0 -> LiveScreen(vm)
                    1 -> ScreenScreen(vm, onRequestScreenCapture)
                    2 -> AccountScreen(vm)
                    else -> SettingsScreen(vm)
                }
            }
        }
    }

    uiState.pendingConfirmGame?.let { game ->
        AlertDialog(
            onDismissRequest = { vm.confirmLogin(false) },
            title = { Text("确认登录") },
            text = { Text("已在屏幕中识别到 $game 账号的登录二维码,是否确认登录该账号?") },
            confirmButton = { TextButton(onClick = { vm.confirmLogin(true) }) { Text("确认") } },
            dismissButton = { TextButton(onClick = { vm.confirmLogin(false) }) { Text("取消") } }
        )
    }

    if (uiState.phoneState.step == 2 && uiState.phoneState.gt.isNotEmpty()) {
        GeetestDialog(
            gt = uiState.phoneState.gt,
            challenge = uiState.phoneState.challenge,
            onPassed = vm::onGeetestPassed,
            onClose = vm::cancelGeetest
        )
    }
}