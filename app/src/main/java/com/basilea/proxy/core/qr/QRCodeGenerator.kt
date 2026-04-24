package com.basilea.proxy.core.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QRCodeGenerator {
    private const val DEFAULT_SIZE = 260
    private const val QR_MARGIN = 2

    fun generateQRCode(
        content: String,
        width: Int = DEFAULT_SIZE,
        height: Int = DEFAULT_SIZE,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.MARGIN to QR_MARGIN,
                EncodeHintType.CHARACTER_SET to "UTF-8"
            )

            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints)

            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    pixels[y * width + x] = if (bitMatrix[x, y]) foregroundColor else backgroundColor
                }
            }

            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (e: Exception) {
            null
        }
    }

    fun generateQRCodeWithLogo(
        content: String,
        size: Int = DEFAULT_SIZE,
        logoSize: Int = size / 5,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap? {
        val qrBitmap = generateQRCode(content, size, size, foregroundColor, backgroundColor) ?: return null

        return try {
            val canvas = android.graphics.Canvas(qrBitmap)
            val logoLeft = (size - logoSize) / 2f
            val logoTop = (size - logoSize) / 2f

            val paint = android.graphics.Paint().apply {
                color = backgroundColor
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawRect(logoLeft, logoTop, logoLeft + logoSize, logoTop + logoSize, paint)

            qrBitmap
        } catch (e: Exception) {
            qrBitmap
        }
    }
}