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
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.fplj.mhyscanner.R

/** 屏幕扫码保活服务:持有 MediaProjection 并前台运行,避免应用切后台时截图被系统回收 */
class ScanService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        instance = this
        startForegroundCompat()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
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
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val CHANNEL_ID = "scan_service"
        private const val NOTIFICATION_ID = 1001

        @Volatile
        private var projection: MediaProjection? = null

        @Volatile
        private var instance: ScanService? = null

        /** 创建 MediaProjection 并拉起前台服务保活,失败返回 null */
        fun createProjection(context: Context, resultCode: Int, data: Intent): MediaProjection? {
            val manager = context.getSystemService(MediaProjectionManager::class.java)
            val proj = manager.getMediaProjection(resultCode, data) ?: return null
            projection = proj
            ContextCompat.startForegroundService(context, Intent(context, ScanService::class.java))
            return proj
        }

        /** 停止并释放投影与前台服务 */
        fun stopProjection() {
            projection?.stop()
            projection = null
            instance?.stopSelf()
        }
    }
}