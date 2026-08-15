package com.fplj.mhyscanner.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import com.fplj.mhyscanner.log.AppLog
import com.fplj.mhyscanner.scanner.Frame
import com.fplj.mhyscanner.scanner.FrameSource

/** 基于 MediaProjection 的屏幕抓帧,输出缩放到 ~1280 宽的 ARGB 像素 */
class ScreenFrameSource(
    private val context: Context,
    private val projection: MediaProjection
) : FrameSource {

    private val logTag = "ScreenFrameSource"
    private val handlerThread = HandlerThread("mhy-screen").apply { start() }
    private val handler = Handler(handlerThread.looper)

    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var lastFrameAt = 0L
    private val throttleMs = 200L

    // 复用缓冲,避免每帧分配全屏 Bitmap / IntArray 造成内存压力
    private var fullBitmap: Bitmap? = null
    private var scaledBitmap: Bitmap? = null
    private var pixels = IntArray(0)
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)

    override fun open(onFrame: (Frame) -> Unit, onError: (String) -> Unit): Boolean {
        return runCatching {
            val metrics = context.resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            reader.setOnImageAvailableListener({ r ->
                // 帧回调线程的任何异常都会直接闪退,统一兜底
                try {
                    val now = System.currentTimeMillis()
                    if (now - lastFrameAt < throttleMs) {
                        r.acquireLatestImage()?.close()
                        return@setOnImageAvailableListener
                    }
                    lastFrameAt = now
                    val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                    try {
                        val bitmap = imageToBitmap(image) ?: return@setOnImageAvailableListener
                        val scaled = downscale(bitmap)
                        if (pixels.size != scaled.width * scaled.height) {
                            pixels = IntArray(scaled.width * scaled.height)
                        }
                        scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
                        onFrame(Frame(rgb = pixels, rgbWidth = scaled.width, rgbHeight = scaled.height))
                    } finally {
                        image.close()
                    }
                } catch (e: Exception) {
                    AppLog.error(logTag, "帧处理异常: ${e.message}")
                }
            }, handler)

            virtualDisplay = projection.createVirtualDisplay(
                "MHY_Scanner_Screen",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface, null, handler
            )
            imageReader = reader
            true
        }.getOrElse {
            AppLog.error(logTag, "屏幕捕获打开失败: ${it.message}")
            onError(it.message ?: "屏幕捕获失败")
            false
        }
    }

    override fun close() {
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.close() }
        imageReader = null
        handlerThread.quitSafely()
    }

    private fun imageToBitmap(image: Image): Bitmap? {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val bmp = fullBitmap
        val needW = image.width + rowPadding / pixelStride
        if (bmp == null || bmp.width != needW || bmp.height != image.height) {
            fullBitmap = Bitmap.createBitmap(needW, image.height, Bitmap.Config.ARGB_8888).also {
                fullBitmap = it
            }
        }
        buffer.rewind()
        fullBitmap!!.copyPixelsFromBuffer(buffer)
        return if (rowPadding == 0) {
            fullBitmap
        } else {
            Bitmap.createBitmap(fullBitmap!!, 0, 0, image.width, image.height)
        }
    }

    /** 缩放复用到 ~1280 宽,目标位图与像素缓冲均复用 */
    private fun downscale(bitmap: Bitmap): Bitmap {
        if (bitmap.width <= 1280) return bitmap
        val ratio = 1280f / bitmap.width
        val newW = 1280
        val newH = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        val target = scaledBitmap
        if (target == null || target.width != newW || target.height != newH) {
            scaledBitmap = Bitmap.createBitmap(newW, newH, Bitmap.Config.ARGB_8888).also {
                scaledBitmap = it
            }
        }
        Canvas(scaledBitmap!!).drawBitmap(
            bitmap,
            null,
            RectF(0f, 0f, newW.toFloat(), newH.toFloat()),
            paint
        )
        return scaledBitmap!!
    }
}
