package com.fplj.mhyscanner.scanner

import android.media.Image

/** 一帧待识别数据:luma(流解码)或 rgb(屏幕投影)二选一 */
data class Frame(
    val luma: ByteArray? = null,
    val dataWidth: Int = 0,
    val dataHeight: Int = 0,
    val left: Int = 0,
    val top: Int = 0,
    val width: Int = 0,
    val height: Int = 0,
    val rgb: IntArray? = null,
    val rgbWidth: Int = 0,
    val rgbHeight: Int = 0
) {
    val hasLuma: Boolean get() = luma != null
    val hasRgb: Boolean get() = rgb != null
}

/** 帧来源抽象:直播流与屏幕抓拍统一入口 */
interface FrameSource {
    fun open(onFrame: (Frame) -> Unit, onError: (String) -> Unit): Boolean
    fun close()
}

/** YUV_420_888 -> luma 平面(供 PlanarYUVLuminanceSource 使用) */
object FrameConverter {

    fun yuv420ToLuma(image: Image): Frame? {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        val width = image.width
        val height = image.height
        if (width <= 0 || height <= 0 || rowStride <= 0) return null

        val luma = ByteArray(rowStride * height)
        if (pixelStride == 1) {
            for (row in 0 until height) {
                buffer.position(row * rowStride)
                buffer.get(luma, row * rowStride, width)
            }
        } else {
            for (row in 0 until height) {
                val base = row * rowStride
                for (x in 0 until width) {
                    buffer.position(base + x * pixelStride)
                    luma[base + x] = buffer.get()
                }
            }
        }
        val crop = image.cropRect
        return Frame(
            luma = luma,
            dataWidth = rowStride,
            dataHeight = height,
            left = crop.left,
            top = crop.top,
            width = crop.width(),
            height = crop.height()
        )
    }
}