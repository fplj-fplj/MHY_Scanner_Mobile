package com.fplj.mhyscanner.ui

/** 注入到 geetest 页面中的 JS 桥 */
class MhyJsBridge(
    private val onPassed: (String) -> Unit,
    private val onClose: () -> Unit
) {
    @android.webkit.JavascriptInterface
    fun passed(json: String) {
        onPassed(json)
    }

    @android.webkit.JavascriptInterface
    fun close() {
        onClose()
    }
}