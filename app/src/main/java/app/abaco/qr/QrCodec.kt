package app.abaco.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.QRCodeWriter

fun parseHex(hex: String, fallback: Int): Int {
    val cleaned = hex.trim().removePrefix("#")
    if (cleaned.length != 6) return fallback
    return try {
        Color.parseColor("#$cleaned")
    } catch (_: Exception) {
        fallback
    }
}

fun contrastRatio(foreground: Int, background: Int): Double {
    fun channel(value: Int): Double {
        val s = value / 255.0
        return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
    }
    fun luma(color: Int): Double {
        return 0.2126 * channel(Color.red(color)) +
            0.7152 * channel(Color.green(color)) +
            0.0722 * channel(Color.blue(color))
    }
    val a = luma(foreground)
    val b = luma(background)
    val hi = maxOf(a, b)
    val lo = minOf(a, b)
    return (hi + 0.05) / (lo + 0.05)
}

fun encodeQr(text: String, size: Int, fgHex: String, bgHex: String, ecc: String): Bitmap? {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) return null
    val level = when (ecc) {
        "L" -> ErrorCorrectionLevel.L
        "Q" -> ErrorCorrectionLevel.Q
        "H" -> ErrorCorrectionLevel.H
        else -> ErrorCorrectionLevel.M
    }
    val fg = parseHex(fgHex, Color.parseColor("#1A1814"))
    val bg = parseHex(bgHex, Color.parseColor("#FFFDF9"))
    return try {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to level,
            EncodeHintType.MARGIN to 1,
            EncodeHintType.CHARACTER_SET to "UTF-8",
        )
        val matrix = QRCodeWriter().encode(trimmed, BarcodeFormat.QR_CODE, size, size, hints)
        val pixels = IntArray(size * size)
        var index = 0
        for (y in 0 until size) {
            for (x in 0 until size) {
                pixels[index++] = if (matrix.get(x, y)) fg else bg
            }
        }
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        }
    } catch (_: Exception) {
        null
    }
}

fun formatCodeName(name: String?): String = when (name) {
    "QR_CODE" -> "Código QR"
    "EAN_13" -> "Código de barras EAN-13"
    "EAN_8" -> "Código de barras EAN-8"
    "UPC_A" -> "Código de barras UPC-A"
    "UPC_E" -> "Código de barras UPC-E"
    "CODE_128" -> "Código de barras Code 128"
    "CODE_39" -> "Código de barras Code 39"
    "CODE_93" -> "Código de barras Code 93"
    "ITF" -> "Código de barras ITF"
    "DATA_MATRIX" -> "Data Matrix"
    "PDF_417" -> "PDF417"
    "RSS_14", "RSS_EXPANDED" -> "Código de barras RSS"
    null, "" -> "Código"
    else -> name.replace('_', ' ')
}

fun looksLikeUrl(text: String): Boolean {
    val value = text.trim()
    return value.startsWith("http://") || value.startsWith("https://")
}
