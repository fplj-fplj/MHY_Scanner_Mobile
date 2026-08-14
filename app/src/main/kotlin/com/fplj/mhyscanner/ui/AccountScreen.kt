package com.fplj.mhyscanner.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.fplj.mhyscanner.MainViewModel
import com.fplj.mhyscanner.data.Account

@Composable
fun AccountScreen(vm: MainViewModel) {
    val uiState by vm.uiState.collectAsState()
    var showAdd by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("我的账号", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = { showAdd = true }) { Text("添加账号") }
        }

        if (uiState.config.account.isEmpty()) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("暂无账号,点击右上角添加")
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

    Card(Modifier.fillMaxWidth().clickable { onSelect() }) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            RadioButton(selected = selected, onClick = onSelect)
            Column(Modifier.weight(1f)) {
                Text(account.name.ifEmpty { account.uid }, style = MaterialTheme.typography.bodyLarge)
                Text("UID: ${account.uid} · ${account.type}", style = MaterialTheme.typography.bodySmall)
                if (account.note.isNotEmpty()) {
                    Text(account.note, style = MaterialTheme.typography.bodySmall)
                }
            }
            IconButton(onClick = { editingNote = true }) {
                Icon(Icons.Filled.Edit, contentDescription = "编辑备注")
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Filled.Delete, contentDescription = "删除")
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
                TabRow(selectedTabIndex = body) {
                    tabs.forEachIndexed { index, label ->
                        Tab(selected = body == index, onClick = { body = index }, text = { Text(label) })
                    }
                }
                Spacer(Modifier.height(12.dp))
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
        uiState.qrImage?.let { qr ->
            Image(
                bitmap = qr.asImageBitmap(),
                contentDescription = "登录二维码",
                modifier = Modifier.size(220.dp)
            )
        }
        Text(uiState.qrStatus.ifEmpty { "正在生成二维码…" }, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { vm.startQrLogin() }) { Text("重新生成") }
        }
        Text("请使用米游社 App 扫码并确认登录", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun CookieTab(vm: MainViewModel) {
    var cookie by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = cookie,
            onValueChange = { cookie = it },
            label = { Text("Cookie") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 4
        )
        Button(onClick = { vm.addAccountByCookie(cookie); cookie = "" }) { Text("添加") }
    }
}

@Composable
private fun PhoneTab(vm: MainViewModel) {
    val uiState by vm.uiState.collectAsState()
    var phone by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it.filter { c -> c.isDigit() }.take(11) },
            label = { Text("手机号") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("验证码") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Button(onClick = { vm.sendSmsCode(phone) }, enabled = uiState.phoneState.step == 0) {
                Text("发送验证码")
            }
        }
        Button(
            onClick = { vm.submitSmsCode(phone, code) },
            enabled = uiState.phoneState.step == 1 && code.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("确认并登录") }
        Text(
            uiState.status.ifEmpty { "" },
            style = MaterialTheme.typography.bodySmall
        )
    }
}