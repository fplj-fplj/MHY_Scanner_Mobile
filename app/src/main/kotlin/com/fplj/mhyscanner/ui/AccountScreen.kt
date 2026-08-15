package com.fplj.mhyscanner.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.fplj.mhyscanner.MainViewModel
import com.fplj.mhyscanner.data.Account

@Composable
fun AccountScreen(vm: MainViewModel) {
    val uiState by vm.uiState.collectAsState()
    var showAdd by rememberSaveable { mutableStateOf(false) }

    // 账号添加成功后自动关闭弹窗并停止扫码轮询
    LaunchedEffect(Unit) {
        vm.accountAdded.collect {
            vm.stopQrLogin()
            showAdd = false
        }
    }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        ScreenHeader(
            title = "我的账号",
            subtitle = if (uiState.config.account.isEmpty()) "添加一个账号,用于扫码确认登录。"
            else "已选择第 ${uiState.config.lastAccount + 1}/${uiState.config.account.size} 个账号进行扫描。",
            trailing = {
                TextButton(onClick = { showAdd = true }) {
                    Icon(Icons.Filled.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(4.dp))
                    Text("添加账号")
                }
            }
        )
        Spacer(Modifier.size(16.dp))

        if (uiState.config.account.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Filled.Person,
                    title = "还没有账号",
                    hint = "点击右上角「添加账号」,通过扫码、Cookie 或手机号登录任一米游社账号。"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                itemsIndexed(uiState.config.account) { index, acc ->
                    AccountRow(
                        account = acc,
                        selected = index == uiState.config.lastAccount,
                        onSelect = { vm.selectAccount(index) },
                        onDelete = { vm.deleteAccount(acc.uid) },
                        onEditNote = { vm.updateNote(acc.uid, it) }
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddAccountDialog(vm, onClose = { showAdd = false })
    }
}

@Composable
private fun AccountRow(
    account: Account,
    selected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onEditNote: (String) -> Unit
) {
    var editingNote by remember { mutableStateOf(false) }

    AppCard(
        tonal = selected,
        onClick = onSelect
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccountAvatar(account)
            Column(
                Modifier.weight(1f).padding(start = 14.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    account.name.ifEmpty { account.uid },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "UID: ${account.uid} · ${account.type}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (account.note.isNotEmpty()) {
                    Text(
                        account.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            SelectionCheck(selected)
            IconButton(onClick = { editingNote = true }) {
                Icon(Icons.Filled.Edit, "编辑备注", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, "删除", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (editingNote) {
        var note by remember { mutableStateOf(account.note) }
        AlertDialog(
            onDismissRequest = { editingNote = false },
            title = { Text("编辑备注") },
            text = { OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注") }) },
            confirmButton = {
                TextButton(onClick = { onEditNote(note); editingNote = false }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { editingNote = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun AddAccountDialog(vm: MainViewModel, onClose: () -> Unit) {
    val uiState by vm.uiState.collectAsState()
    var body by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf("扫码", "Cookie", "手机号")

    AlertDialog(
        onDismissRequest = {
            if (uiState.addingAccount) vm.stopQrLogin()
            onClose()
        },
        title = { Text("添加账号") },
        text = {
            Column {
                TabRow(selectedTabIndex = body, containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    tabs.forEachIndexed { index, label ->
                        Tab(selected = body == index, onClick = { body = index }, text = { Text(label) })
                    }
                }
                Spacer(Modifier.size(16.dp))
                when (body) {
                    0 -> QrLoginTab(vm)
                    1 -> CookieTab(vm)
                    else -> PhoneTab(vm)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (uiState.addingAccount) vm.stopQrLogin()
                onClose()
            }) { Text("关闭") }
        }
    )
}

@Composable
private fun QrLoginTab(vm: MainViewModel) {
    val uiState by vm.uiState.collectAsState()

    LaunchedEffect(Unit) { vm.startQrLogin() }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Crossfade(
            targetState = uiState.qrImage,
            animationSpec = AppSpring.Default,
            label = "qrImage"
        ) { qr ->
            Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
                if (qr != null) {
                    Image(
                        bitmap = qr.asImageBitmap(),
                        contentDescription = "登录二维码",
                        modifier = Modifier.size(220.dp)
                    )
                }
            }
        }
        Text(
            uiState.qrStatus.ifEmpty { "正在生成二维码…" },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        TextButton(onClick = { vm.startQrLogin() }) { Text("重新生成") }
        Text(
            "请使用米游社 App 扫码并确认登录",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CookieTab(vm: MainViewModel) {
    var cookie by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = cookie,
            onValueChange = { cookie = it },
            label = { Text("Cookie") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            minLines = 4
        )
        PrimaryActionButton(
            text = "添加",
            icon = Icons.Filled.Add,
            onClick = { vm.addAccountByCookie(cookie); cookie = "" }
        )
    }
}

@Composable
private fun PhoneTab(vm: MainViewModel) {
    val uiState by vm.uiState.collectAsState()
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.filter { c -> c.isDigit() }.take(11) },
            label = { Text("手机号") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("验证码") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = { vm.sendSmsCode(phone) },
                enabled = uiState.phoneState.step == 0,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("发送验证码")
            }
        }
        PrimaryActionButton(
            text = "确认并登录",
            icon = Icons.Filled.Add,
            onClick = { vm.submitSmsCode(phone, code) },
            enabled = uiState.phoneState.step == 1 && code.isNotBlank()
        )
        StatusBanner(uiState.status, active = uiState.phoneState.step == 1 && code.isNotBlank())
    }
}