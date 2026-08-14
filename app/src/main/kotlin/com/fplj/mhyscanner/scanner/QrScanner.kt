package com.fplj.mhyscanner.scanner

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter

object QrScanner {

    private val hints: Map<DecodeHintType, Any> = mapOf(
        DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
        DecodeHintType.TRY_HARDER to true
    )

    /** 从 YUV 平面(如 MediaCodec 输出)解码,失败返回 null */
    fun decodeLuma(
        yuvData: ByteArray,
        dataWidth: Int,
        dataHeight: Int,
        left: Int,
        top: Int,
        width: Int,
        height: Int
    ): String? {
        if (width <= 0 || height <= 0) return null
        return runCatching {
            val source = PlanarYUVLuminanceSource(yuvData, dataWidth, dataHeight, left, top, width, height, false)
            val reader = MultiFormatReader()
            reader.setHints(hints)
            val result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
            reader.reset()
            result.text
        }.getOrNull()
    }

    /** 从 ARGB 像素数组解码,失败返回 null */
    fun decodeRgb(pixels: IntArray, width: Int, height: Int): String? {
        if (width <= 0 || height <= 0) return null
        return runCatching {
            val source = RGBLuminanceSource(width, height, pixels)
            val reader = MultiFormatReader()
            reader.setHints(hints)
            val result = reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
            reader.reset()
            result.text
        }.getOrNull()
    }

    /** 将文本渲染为二维码 Bitmap,供"添加账号-手机扫码"使用 */
    fun renderQr(content: String, size: Int = 512): Bitmap? = runCatching {
        val bits = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, mapOf(DecodeHintType.MARGIN to 1))
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bits[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    }.getOrNull()
}