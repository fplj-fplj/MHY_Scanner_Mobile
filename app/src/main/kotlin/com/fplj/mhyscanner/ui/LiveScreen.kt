package com.fplj.mhyscanner.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fplj.mhyscanner.MainViewModel
import com.fplj.mhyscanner.live.LiveStream

@Composable
fun LiveScreen(vm: MainViewModel) {
    val uiState by vm.uiState.collectAsState()
    var rid by rememberSaveable { mutableStateOf("") }
    var platform by rememberSaveable { mutableIntStateOf(0) }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("直播抢码", style = MaterialTheme.typography.headlineSmall)
        Text(
            "解析直播间画面中的米游社登录二维码。输入房间号(RID),开始后会自动拉流并识别二维码。",
            style = MaterialTheme.typography.bodyMedium
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = platform == 0,
                onClick = { platform = 0 },
                label = { Text("B站直播") }
            )
            FilterChip(
                selected = platform == 1,
                onClick = { platform = 1 },
                label = { Text("抖音直播") }
            )
        }

        OutlinedTextField(
            value = rid,
            onValueChange = { rid = it },
            label = { Text("直播间房间号") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )

        val primaryInteractions = remember { MutableInteractionSource() }
        if (uiState.scanning) {
            Button(onClick = vm::stopScan) { Text("停止扫描") }
        } else {
            Button(
                onClick = {
                    if (rid.isBlank()) vm.showMessage("请输入房间号") else vm.startStreamScan(
                        if (platform == 0) LiveStream.Platform.BILIBILI else LiveStream.Platform.DOUYIN,
                        rid.trim()
                    )
                },
                interactionSource = primaryInteractions,
                modifier = Modifier.fillMaxWidth().pressScale(primaryInteractions)
            ) { Text("开始扫描") }
        }

        StatusPill(uiState.status)
    }
}