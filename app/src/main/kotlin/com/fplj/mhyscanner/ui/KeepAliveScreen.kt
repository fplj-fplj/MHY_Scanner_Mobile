package com.fplj.mhyscanner.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.fplj.mhyscanner.log.AppLog
import com.fplj.mhyscanner.service.ScanService

private data class CheckResult(val label: String, val ok: Boolean, val detail: String)

/** 后台存活性检查页:运行检查 + 增强保活引导 */
@Composable
fun KeepAliveScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var results by remember { mutableStateOf<List<CheckResult>?>(null) }

    fun runChecks() {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val ignoringBattery = pm.isIgnoringBatteryOptimizations(context.packageName)
        val notificationGranted = Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        val networkUp = (context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager)
            .activeNetwork != null
        val serviceAlive = ScanService.isAlive

        results = listOf(
            CheckResult(
                "电池优化",
                ignoringBattery,
                if (ignoringBattery) "已忽略,后台不易被杀" else "未忽略,系统可能在后台杀掉应用"
            ),
            CheckResult(
                "前台扫描服务",
                serviceAlive,
                if (serviceAlive) "前台服务运行中" else "未运行(屏幕扫描时自动驻留)"
            ),
            CheckResult(
                "通知权限",
                notificationGranted,
                if (notificationGranted) "已授予,可正常显示前台通知" else "未授予,前台服务通知可能被隐藏"
            ),
            CheckResult(
                "网络连接",
                networkUp,
                if (networkUp) "网络可用" else "无可用网络,无法抢码"
            )
        )
        AppLog.info("KeepAlive", "后台存活检查完成: 电池优化=$ignoringBattery 服务=$serviceAlive 通知=$notificationGranted 网络=$networkUp")
    }

    LaunchedEffect(Unit) { runChecks() }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", Modifier.size(18.dp))
                Spacer(Modifier.width(2.dp))
                Text("设置")
            }
            Text(
                "后台保活",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        Text(
            "检查当前后台存活性,并引导你完成保活设置。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        results?.let { list ->
            AppCard(tonal = true) {
                Column(Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    list.forEachIndexed { index, r ->
                        if (index > 0) androidx.compose.material3.HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (r.ok) Icons.Filled.Check else Icons.Filled.Close,
                                null,
                                Modifier.size(20.dp),
                                tint = if (r.ok) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.error
                            )
                            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                Text(r.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    r.detail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            TextButton(
                onClick = { runChecks() },
                modifier = Modifier.align(Alignment.End)
            ) { Text("重新检查") }
        }

        AppCard {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Info,
                        null,
                        Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "增强保活",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
                GuidanceRow("1. 忽略电池优化", "防止系统在后台杀掉应用。") {
                    TextButton(onClick = { openBatteryOptimization(context) }) { Text("去设置") }
                }
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                GuidanceRow("2. 允许自启动", "在系统设置 → 应用 → 本应用 → 自启动/开机自启 中允许。") {
                    TextButton(onClick = { openAppDetails(context) }) { Text("应用详情") }
                }
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                GuidanceRow("3. 锁定最近任务", "在最近任务中长按本应用,选择「锁定」,防止被一键清理。") {
                    Text("手动", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun GuidanceRow(
    title: String,
    desc: String,
    action: @Composable () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Start
            )
        }
        Spacer(Modifier.width(8.dp))
        action()
    }
}

private fun openBatteryOptimization(context: Context) {
    val intent = Intent(
        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
        Uri.parse("package:${context.packageName}")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure { AppLog.warn("KeepAlive", "打开电池优化设置失败:${it.message}") }
}

private fun openAppDetails(context: Context) {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.parse("package:${context.packageName}")
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
        .onFailure { AppLog.warn("KeepAlive", "打开应用详情失败:${it.message}") }
}
