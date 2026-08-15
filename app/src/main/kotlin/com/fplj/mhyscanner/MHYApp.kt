package com.fplj.mhyscanner

import android.app.Application
import com.fplj.mhyscanner.log.AppLog

/** 应用入口:初始化日志持久化,并捕获未处理崩溃写入日志,便于重启后回顾 */
class MHYApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)
        val default = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val stack = throwable.stackTraceToString()
                AppLog.error("CRASH", "进程崩溃 @${thread.name}\n$stack")
            } catch (_: Throwable) {
                // 崩溃时资源可能已不可用,尽力而为
            }
            default?.uncaughtException(thread, throwable)
                ?: runCatching { throwable.printStackTrace() }
        }
    }
}
