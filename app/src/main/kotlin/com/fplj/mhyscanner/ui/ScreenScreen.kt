package com.fplj.mhyscanner.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fplj.mhyscanner.MainViewModel

@Composable
fun ScreenScreen(vm: MainViewModel, onRequestScreenCapture: () -> Unit) {
    val uiState by vm.uiState.collectAsState()

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("屏幕扫码", style = MaterialTheme.typography.headlineSmall)
        Text(
            "通过系统录屏权限实时截屏,自动识别米游社/官方登录二维码。识别成功后需一键确认登录。",
            style = MaterialTheme.typography.bodyMedium
        )

        val primaryInteractions = remember { MutableInteractionSource() }
        if (uiState.scanning) {
            Button(onClick = vm::stopScan) { Text("停止扫描") }
        } else {
            Button(
                onClick = onRequestScreenCapture,
                interactionSource = primaryInteractions,
                modifier = Modifier.fillMaxWidth().pressScale(primaryInteractions)
            ) {
                Text("开始屏幕扫描")
            }
            Text(
                "首次使用会弹出系统授权窗口,请选择「开始录制」并允许,之后可随时开始屏幕扫描。",
                style = MaterialTheme.typography.bodySmall
            )
        }

        StatusPill(uiState.status)
    }
}