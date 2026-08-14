package com.fplj.mhyscanner.screen

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import com.fplj.mhyscanner.scanner.Frame
import com.fplj.mhyscanner.scanner.FrameSource

/** 基于 MediaProjection 的屏幕抓帧,输出缩放到 ~1280 宽的 ARGB 像素 */
class ScreenFrameSource(
    private val context: Context,
    private val projection: MediaProjection
) : FrameSource {

    private val handlerThread = HandlerThread("mhy-screen").apply { start() }
    private val handler = Handler(handlerThread.looper)

    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var lastFrameAt = 0L
    private val throttleMs = 200L

    override fun open(onFrame: (Frame) -> Unit, onError: (String) -> Unit): Boolean {
        return runCatching {
            val metrics = context.resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels
            val density = metrics.densityDpi

            val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            reader.setOnImageAvailableListener({ r ->
                val now = System.currentTimeMillis()
                if (now - lastFrameAt < throttleMs) {
                    r.acquireLatestImage()?.close()
                    return@setOnImageAvailableListener
                }
                lastFrameAt = now
                val image = r.acquireLatestImage() ?: return@setOnImageAvailableListener
                try {
                    val bitmap = imageToBitmap(image)
                    if (bitmap != null) {
                        val scaled = downscale(bitmap, 1280)
                        val pixels = IntArray(scaled.width * scaled.height)
                        scaled.getPixels(pixels, 0, scaled.width, 0, 0, scaled.width, scaled.height)
                        onFrame(Frame(rgb = pixels, rgbWidth = scaled.width, rgbHeight = scaled.height))
                        if (scaled !== bitmap) scaled.recycle()
                        bitmap.recycle()
                    }
                } finally {
                    image.close()
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
        val bitmap = Bitmap.createBitmap(
            image.width + rowPadding / pixelStride,
            image.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        return if (rowPadding == 0) bitmap else Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
    }

    private fun downscale(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) return bitmap
        val ratio = maxWidth.toFloat() / bitmap.width
        val newHeight = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, maxWidth, newHeight, true)
    }
}