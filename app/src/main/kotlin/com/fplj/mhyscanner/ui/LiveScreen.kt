package com.fplj.mhyscanner.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fplj.mhyscanner.MainViewModel
import com.fplj.mhyscanner.live.LiveStream

@Composable
fun LiveScreen(vm: MainViewModel) {
    val uiState by vm.uiState.collectAsState()
    var rid by rememberSaveable { mutableStateOf("") }
    var platform by rememberSaveable { mutableIntStateOf(0) }
    val platforms = listOf("B站直播" to 0, "抖音直播" to 1)

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            title = "直播抢码",
            subtitle = "解析直播间画面中的米游社登录二维码。填入房间号后开始,自动拉流并识别。"
        )

        AppCard(tonal = true) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    platforms.forEach { (label, idx) ->
                        Surface(
                            onClick = { platform = idx },
                            shape = RoundedCornerShape(12.dp),
                            color = if (platform == idx) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (platform == idx) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                label,
                                style = MaterialTheme.typography.labelLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = rid,
                    onValueChange = { rid = it },
                    label = { Text("直播间房间号") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
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
                text = "开始扫描",
                icon = Icons.Filled.PlayArrow,
                onClick = {
                    if (rid.isBlank()) vm.showMessage("请输入房间号")
                    else vm.startStreamScan(
                        if (platform == 0) LiveStream.Platform.BILIBILI else LiveStream.Platform.DOUYIN,
                        rid.trim()
                    )
                }
            )
        }

        StatusBanner(uiState.status, active = uiState.scanning)
    }
}