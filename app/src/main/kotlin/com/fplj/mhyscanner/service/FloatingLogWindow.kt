package com.fplj.mhyscanner.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.net.Uri
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.fplj.mhyscanner.log.AppLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 悬浮日志窗:TYPE_APPLICATION_OVERLAY 半透明面板,实时显示最近日志,可拖动。
 * 由 MainActivity 依据 Config.floatingLogEnabled 启停。
 */
object FloatingLogWindow {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var windowManager: WindowManager? = null
    private var rootView: View? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var textView: TextView? = null
    private var collectJob: Job? = null

    private var lastX = 0f
    private var lastY = 0f
    private var startX = 0f
    private var startY = 0f

    val isShowing: Boolean get() = rootView != null

    fun canDraw(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** 拉起系统悬浮窗授权页 */
    fun requestPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { AppLog.warn("FloatLog", "打开悬浮窗授权页失败:${it.message}") }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun start(context: Context) {
        if (rootView != null) return
        if (!canDraw(context)) {
            AppLog.warn("FloatLog", "悬浮窗权限未授予,无法显示日志窗")
            return
        }
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val logText = TextView(context).apply {
            setBackgroundColor(Color.argb(232, 24, 24, 24))
            setTextColor(Color.WHITE)
            textSize = 11f
            typeface = Typeface.MONOSPACE
            setPadding(12, 10, 12, 10)
            maxLines = 8
        }
        textView = logText

        val closeBtn = TextView(context).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 0, 10, 0)
            setOnClickListener { stop() }
        }

        val header = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.argb(232, 45, 45, 45))
            setPadding(12, 6, 0, 6)
            addView(
                TextView(context).apply {
                    text = "▤ 拖动"
                    setTextColor(Color.WHITE)
                    textSize = 11f
                }
            )
            addView(View(context).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) })
            addView(closeBtn)
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.argb(232, 24, 24, 24))
            addView(header)
            addView(logText)
        }

        @Suppress("DEPRECATION")
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16
            y = 220
        }
        layoutParams = params

        // 拖动:整个面板可拖;关闭按钮是子 View,点击事件优先于父级拖动
        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastX = event.rawX
                    lastY = event.rawY
                    layoutParams?.let { startX = it.x.toFloat(); startY = it.y.toFloat() }
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - lastX
                    val dy = event.rawY - lastY
                    val p = layoutParams
                    if (p != null) {
                        // gravity 为 TOP|END:x 从屏幕右缘测量,右移时 x 应减小
                        p.x = (startX - dx).toInt()
                        p.y = (startY + dy).toInt()
                        runCatching { wm.updateViewLayout(container, p) }
                    }
                    true
                }
                else -> false
            }
        }

        try {
            wm.addView(container, params)
        } catch (e: Exception) {
            AppLog.error("FloatLog", "悬浮窗创建失败:${e.message}")
            layoutParams = null
            return
        }
        windowManager = wm
        rootView = container

        collectJob = scope.launch {
            AppLog.entries.collectLatest { list ->
                textView?.text = list.takeLast(8).reversed().joinToString("\n") {
                    "[${it.level}] ${it.timeText} ${it.tag}: ${it.message}"
                }
            }
        }
        AppLog.info("FloatLog", "悬浮日志窗已开启")
    }

    fun stop() {
        if (rootView == null) return
        collectJob?.cancel()
        collectJob = null
        runCatching { windowManager?.removeView(rootView) }
        windowManager = null
        rootView = null
        textView = null
        layoutParams = null
        AppLog.info("FloatLog", "悬浮日志窗已关闭")
    }
}
