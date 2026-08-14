package com.fplj.mhyscanner.live

import android.content.Context
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.fplj.mhyscanner.scanner.Frame
import com.fplj.mhyscanner.scanner.FrameSource
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 基于 ExoPlayer + ImageReader 的直播流抓帧。
 * HLS / FLV / MP4 等均由 Media3 内置解复用器解码,直接消费解码后的 YUV 帧。
 */
@UnstableApi
class ExoFrameSource(
    context: Context,
    private val streamUrl: String
) : FrameSource {

    private val appContext = context.applicationContext
    private val handlerThread = HandlerThread("mhy-stream").apply { start() }
    private val handler = Handler(handlerThread.looper)

    private var player: ExoPlayer? = null
    private var imageReader: ImageReader? = null
    private val readerReady = AtomicBoolean(false)
    private val requestedDims = android.util.Size(0, 0)

    override fun open(onFrame: (Frame) -> Unit, onError: (String) -> Unit): Boolean {
        runCatching {
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent("Mozilla/5.0 (Linux; Android 13)")
            val renderersFactory = DefaultRenderersFactory(appContext)
            val p = ExoPlayer.Builder(appContext)
                .setRenderersFactory(renderersFactory)
                .setMediaSourceFactory(DefaultMediaSourceFactory(appContext).setDataSourceFactory(dataSourceFactory))
                .build()
            player = p

            p.addListener(object : Player.Listener {
                override fun onVideoSizeChanged(videoSize: VideoSize) {
                    if (videoSize.width > 0 && videoSize.height > 0) {
                        ensureReader(videoSize.width, videoSize.height, onFrame)
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    onError(error.errorCodeName)
                }
            })

            p.setMediaItem(MediaItem.fromUri(streamUrl))
            p.prepare()
            p.playWhenReady = true
        }.onFailure {
            onError(it.message ?: "初始化失败")
            return false
        }
        return true
    }

    private fun ensureReader(width: Int, height: Int, onFrame: (Frame) -> Unit) {
        if (readerReady.get() && requestedDims.width == width && requestedDims.height == height) return
        requestedDims.set(width, height)

        imageReader?.close()
        val reader = ImageReader.newInstance(width, height, android.graphics.PixelFormat.RGBA_8888, 3)
        imageReader = reader
        readerReady.set(true)

        reader.setOnImageAvailableListener({ r ->
            val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
            try {
                onFrame(frameFromRgba(image))
            } finally {
                image.close()
            }
        }, handler)

        player?.setVideoSurface(reader.surface)
    }

    /** ImageReader 输出为 RGBA_8888,转成全帧灰度供 ZXing 识别 */
    private fun frameFromRgba(image: android.media.Image): Frame {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val width = image.width
        val height = image.height
        val gray = ByteArray(width * height)
        var p = 0
        for (row in 0 until height) {
            val base = row * rowStride
            for (x in 0 until width) {
                val o = base + x * pixelStride
                val r = buffer.get(o).toInt() and 0xFF
                val g = buffer.get(o + 1).toInt() and 0xFF
                val b = buffer.get(o + 2).toInt() and 0xFF
                gray[p++] = ((r * 299 + g * 587 + b * 114) / 1000).toByte()
            }
        }
        return Frame(
            luma = gray,
            dataWidth = width,
            dataHeight = height,
            left = 0,
            top = 0,
            width = width,
            height = height
        )
    }

    override fun close() {
        readerReady.set(false)
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        runCatching { imageReader?.close() }
        imageReader = null
        handlerThread.quitSafely()
    }
}