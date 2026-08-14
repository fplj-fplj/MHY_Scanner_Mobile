package com.fplj.mhyscanner.ui

import android.content.Context
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun GeetestDialog(
    gt: String,
    challenge: String,
    onPassed: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val bridge = remember { MhyJsBridge(onPassed, onClose) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("滑块验证") },
        text = {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        addJavascriptInterface(bridge, "MHY")
                        loadDataWithBaseURL(
                            "https://static.geetest.com/",
                            buildGeetestHtml(ctx, gt, challenge),
                            "text/html",
                            "utf-8",
                            null
                        )
                    }
                },
                modifier = Modifier.height(380.dp).fillMaxSize()
            )
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onClose) { Text("取消") } }
    )
}

private fun buildGeetestHtml(context: Context, gt: String, challenge: String): String {
    val template = context.assets.open("geetest.html").bufferedReader().use { it.readText() }
    return template
        .replace("__GT__", gt)
        .replace("__CHALLENGE__", challenge)
}