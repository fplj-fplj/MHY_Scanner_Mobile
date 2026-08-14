package com.fplj.mhyscanner.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Filled.PlayArrow, null) },
                    label = { Text("直播") }
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Filled.Refresh, null) },
                    label = { Text("屏幕") }
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Filled.AccountCircle, null) },
                    label = { Text("账号") }
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Icon(Icons.Filled.Settings, null) },
                    label = { Text("设置") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AnimatedContent(
                targetState = tab,
                transitionSpec = {
                    // 进入快、退出更快:ease-out + 轻微上移
                    (fadeIn(tween(160, easing = LinearOutSlowInEasing)) +
                        slideInVertically(tween(160, easing = LinearOutSlowInEasing)) { it / 24 }) togetherWith
                        (fadeOut(tween(90, easing = LinearOutSlowInEasing)) +
                            slideOutVertically(tween(90, easing = LinearOutSlowInEasing)) { -it / 32 })
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