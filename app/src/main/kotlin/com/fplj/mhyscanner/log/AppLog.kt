package com.fplj.mhyscanner.log

import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 一条日志 */
data class LogEntry(
    val time: Long = System.currentTimeMillis(),
    val level: Char, // 'D' 'I' 'W' 'E'
    val tag: String,
    val message: String
) {
    val timeText: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date(time))
}

/**
 * 应用内集中日志:环形缓冲 + StateFlow,供日志页与悬浮窗共用同一数据源,
 * 同时镜像到 android.util.Log 便于 adb 排查。
 */
object AppLog {

    const val MaxEntries = 1000

    private val buffer = ArrayDeque<LogEntry>()
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    @Synchronized
    fun log(level: Char, tag: String, message: String) {
        val entry = LogEntry(level = level, tag = tag, message = message)
        buffer.addLast(entry)
        while (buffer.size > MaxEntries) buffer.removeFirst()
        _entries.value = buffer.toList()
        when (level) {
            'E' -> Log.e(tag, message)
            'W' -> Log.w(tag, message)
            'D' -> Log.d(tag, message)
            else -> Log.i(tag, message)
        }
    }

    @Synchronized
    fun clear() {
        buffer.clear()
        _entries.value = emptyList()
    }

    fun debug(tag: String, message: String) = log('D', tag, message)
    fun info(tag: String, message: String) = log('I', tag, message)
    fun warn(tag: String, message: String) = log('W', tag, message)
    fun error(tag: String, message: String) = log('E', tag, message)

    /** 最近 n 条(新到旧) */
    fun latest(n: Int): List<LogEntry> = _entries.value.takeLast(n).reversed()
}
