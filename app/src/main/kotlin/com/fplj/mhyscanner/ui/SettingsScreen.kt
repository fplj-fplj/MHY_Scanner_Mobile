package com.fplj.mhyscanner.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.fplj.mhyscanner.MainViewModel
import com.fplj.mhyscanner.service.FloatingLogWindow

/** 设置页:主列表 + 日志 / 后台保活 子页面(轻量页内导航) */
@Composable
fun SettingsScreen(vm: MainViewModel) {
    var section by rememberSaveable { mutableStateOf<String?>(null) }

    when (section) {
        "logs" -> LogsScreen(onBack = { section = null })
        "keepalive" -> KeepAliveScreen(onBack = { section = null })
        else -> SettingsMain(
            vm = vm,
            onLogs = { section = "logs" },
            onKeepAlive = { section = "keepalive" }
        )
    }
}

@Composable
private fun SettingsMain(
    vm: MainViewModel,
    onLogs: () -> Unit,
    onKeepAlive: () -> Unit
) {
    val uiState by vm.uiState.collectAsState()
    val cfg = uiState.config
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        ScreenHeader(
            title = "设置",
            subtitle = "调整扫描与登录流程的自动化行为。"
        )

        AppCard(tonal = true) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                SwitchRow(
                    title = "自动确认登录",
                    desc = "识别到二维码后直接确认登录",
                    checked = cfg.autoLogin,
                    onChange = { vm.updateSettings(autoLogin = it) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchRow(
                    title = "登录成功自动退出",
                    desc = "确认登录成功后自动结束进程",
                    checked = cfg.autoExit,
                    onChange = { vm.updateSettings(autoExit = it) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchRow(
                    title = "识别成功后自动开始",
                    desc = "识别到二维码即开始确认流程",
                    checked = cfg.autoStart,
                    onChange = { vm.updateSettings(autoStart = it) }
                )
            }
        }

        AppCard(tonal = true) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                SettingsRow(
                    title = "查看日志",
                    desc = "查看最近操作记录,排查问题",
                    icon = Icons.Filled.BugReport,
                    onClick = onLogs
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SwitchRow(
                    title = "悬浮日志窗",
                    desc = "在其他应用上层显示最近日志,便于排查",
                    checked = cfg.floatingLogEnabled,
                    onChange = { enabled ->
                        if (enabled && !FloatingLogWindow.canDraw(context)) {
                            FloatingLogWindow.requestPermission(context)
                            vm.showMessage("请在系统弹窗中允许「悬浮窗」权限")
                        }
                        vm.updateSettings(floatingLogEnabled = enabled)
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow(
                    title = "后台保活",
                    desc = "检查后台存活性并增强保活",
                    icon = Icons.Filled.Security,
                    onClick = onKeepAlive
                )
            }
        }

        AppCard {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                SettingsRow(
                    title = "项目链接",
                    desc = "github.com/fplj-fplj/MHY_Scanner_Mobile",
                    icon = Icons.Filled.Link,
                    onClick = { uriHandler.openUri("https://github.com/fplj-fplj/MHY_Scanner_Mobile") }
                )
            }
        }

        AppCard {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Info,
                    null,
                    Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(Modifier.padding(start = 12.dp)) {
                    Text("关于", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "MHY_Scanner_Mobile v0.1.0\nMHY_Scanner 的 Android 移植版,遵循 GPL-3.0。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Text(
            "本工具仅供学习研究使用。登录米游社代表同意其用户协议,因使用本工具引起的账号风险由使用者自行承担。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(0.8f)
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    desc: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun SettingsRow(
    title: String,
    desc: String,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                icon,
                null,
                Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Column(Modifier.padding(start = if (icon != null) 10.dp else 0.dp).weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.Filled.ChevronRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
