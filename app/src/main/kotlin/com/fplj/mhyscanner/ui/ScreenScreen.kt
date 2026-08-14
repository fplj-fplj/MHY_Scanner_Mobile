package com.fplj.mhyscanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ScreenShare
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fplj.mhyscanner.MainViewModel

@Composable
fun ScreenScreen(vm: MainViewModel, onRequestScreenCapture: () -> Unit) {
    val uiState by vm.uiState.collectAsState()

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            title = "屏幕扫码",
            subtitle = "通过系统录屏实时识别米游社登录二维码,识别后在弹出的对话框中一键确认。"
        )

        AppCard(tonal = true) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.ScreenShare,
                            null,
                            Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column(Modifier.padding(start = 14.dp)) {
                        Text("需要系统录屏权限", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "首次使用会弹出系统授权窗口,请选择「开始录制」并允许。授权成功后即可随时开始扫描。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        if (uiState.scanning) {
            PrimaryActionButton(
                text = "停止扫描",
                icon = Icons.Filled.Stop,
                onClick = vm::stopScan,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            PrimaryActionButton(
                text = "开始屏幕扫描",
                icon = Icons.Filled.ScreenShare,
                onClick = onRequestScreenCapture
            )
        }

        StatusBanner(uiState.status, active = uiState.scanning)
    }
}