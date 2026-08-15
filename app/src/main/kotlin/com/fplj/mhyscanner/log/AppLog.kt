package com.fplj.mhyscanner.log

import android.content.Context
import android.util.Log
import java.io.BufferedWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 一条日志 */
@Serializable
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
 *
 * 额外持久化到 filesDir/logs/app.log:进程崩溃/被杀后重启,可通过
 * [history] 回顾上次运行(崩溃前)的日志,便于定位闪退根因。
 */
object AppLog {

    const val MaxEntries = 1000
    private const val MaxFileBytes = 512 * 1024
    private const val MaxFileLines = 800
    private const val HistoryLoad = 300

    private val json = Json { ignoreUnknownKeys = true }

    private val buffer = ArrayDeque<LogEntry>()
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    /** 上次运行(崩溃前)的日志尾部,按时间正序,供日志页回顾 */
    private val _history = MutableStateFlow<List<LogEntry>>(emptyList())
    val history: StateFlow<List<LogEntry>> = _history.asStateFlow()

    private var file: File? = null
    private var writer: BufferedWriter? = null

    /** 初始化持久化(幂等)。在 Application.onCreate 调用 */
    @Synchronized
    fun init(context: Context) {
        if (file != null) return
        val dir = File(context.filesDir, "logs").apply { mkdirs() }
        val f = File(dir, "app.log")
        file = f
        _history.value = runCatching {
            f.readLines().takeLast(HistoryLoad).mapNotNull(::parseLine)
        }.getOrDefault(emptyList())
        writer = runCatching { f.bufferedWriter(Charsets.UTF_8) }.getOrNull()
    }

    @Synchronized
    fun log(level: Char, tag: String, message: String) {
        val entry = LogEntry(level = level, tag = tag, message = message)
        buffer.addLast(entry)
        while (buffer.size > MaxEntries) buffer.removeFirst()
        _entries.value = buffer.toList()
        writeLine(entry)
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
        _history.value = emptyList()
        runCatching { file?.writeText("") }
    }

    fun debug(tag: String, message: String) = log('D', tag, message)
    fun info(tag: String, message: String) = log('I', tag, message)
    fun warn(tag: String, message: String) = log('W', tag, message)
    fun error(tag: String, message: String) = log('E', tag, message)

    /** 最近 n 条(新到旧) */
    fun latest(n: Int): List<LogEntry> = _entries.value.takeLast(n).reversed()

    // ---------- 文件持久化 ----------

    private fun writeLine(entry: LogEntry) {
        val w = writer ?: return
        val f = file ?: return
        runCatching {
            w.write(json.encodeToString(entry))
            w.newLine()
            w.flush()
            if (f.length() > MaxFileBytes) trimFile()
        }
    }

    /** 文件过大时只保留最近 MaxFileLines 行 */
    private fun trimFile() {
        val f = file ?: return
        val lines = f.readLines()
        if (lines.size <= MaxFileLines) return
        f.writeText(lines.takeLast(MaxFileLines).joinToString("\n") + "\n", Charsets.UTF_8)
    }

    private fun parseLine(line: String): LogEntry? = runCatching {
        json.decodeFromString<LogEntry>(line)
    }.getOrNull()
}
