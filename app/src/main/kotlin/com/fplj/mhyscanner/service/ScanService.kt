package com.fplj.mhyscanner.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.fplj.mhyscanner.R
import com.fplj.mhyscanner.log.AppLog
import kotlinx.coroutines.CompletableDeferred

/** 屏幕扫码保活服务:持有 MediaProjection 并前台运行,避免应用切后台时截图被系统回收 */
class ScanService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        instance = this
        AppLog.info("ScanService", "前台服务启动")
        startForegroundCompat()
        handleProjectionRequest(intent)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        AppLog.info("ScanService", "前台服务停止")
        instance = null
        stopProjection()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundCompat() {
        val channel = NotificationChannel(CHANNEL_ID, "扫码服务", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("MHY_Scanner_Mobile")
            .setContentText("屏幕扫码运行中")
            .setSmallIcon(R.drawable.ic_stat_scan)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            val type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION or
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            startForeground(NOTIFICATION_ID, notification, type)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /** 从 intent 读取屏幕捕获授权,在 startForeground 之后创建 MediaProjection 并回填异步结果 */
    private fun handleProjectionRequest(intent: Intent?) {
        val code = intent?.getIntExtra(EXTRA_PROJ_CODE, -1) ?: return
        val data = readParcelableIntent(intent) ?: return
        val deferred = pendingResult ?: return
        pendingResult = null
        val manager = getSystemService(MediaProjectionManager::class.java)
        val proj = runCatching { manager.getMediaProjection(code, data) }
            .onFailure {
                AppLog.error("ScanService", "创建 MediaProjection 失败: ${it.message}")
            }
            .getOrNull()
        projection = proj
        if (proj != null) {
            proj.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    projection = null
                    instance?.stopSelf()
                }
            }, null)
        }
        deferred.complete(proj)
    }

    private fun readParcelableIntent(intent: Intent): Intent? =
        if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(EXTRA_PROJ_DATA, Intent::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_PROJ_DATA)
        }

    companion object {
        private const val CHANNEL_ID = "scan_service"
        private const val NOTIFICATION_ID = 1001
        const val EXTRA_PROJ_CODE = "proj_code"
        const val EXTRA_PROJ_DATA = "proj_data"

        @Volatile
        private var projection: MediaProjection? = null

        @Volatile
        private var instance: ScanService? = null

        @Volatile
        private var pendingResult: CompletableDeferred<MediaProjection?>? = null

        /** 前台服务是否存活(用于后台保活检查) */
        val isAlive: Boolean get() = instance != null
        val currentProjection: MediaProjection? get() = projection

        /**
         * 先启动 mediaProjection 类型前台服务,再在服务内创建 MediaProjection。
         * Android 14+ 强制要求:getMediaProjection 必须在对应类型前台服务运行后调用。
         */
        fun startProjection(context: Context, resultCode: Int, data: Intent): CompletableDeferred<MediaProjection?> {
            val deferred = CompletableDeferred<MediaProjection?>()
            pendingResult = deferred
            val intent = Intent(context, ScanService::class.java)
                .putExtra(EXTRA_PROJ_CODE, resultCode)
                .putExtra(EXTRA_PROJ_DATA, data)
            ContextCompat.startForegroundService(context, intent)
            return deferred
        }

        /** 停止并释放投影与前台服务 */
        fun stopProjection() {
            projection?.stop()
            projection = null
            pendingResult?.complete(null)
            pendingResult = null
            instance?.stopSelf()
        }
    }
}