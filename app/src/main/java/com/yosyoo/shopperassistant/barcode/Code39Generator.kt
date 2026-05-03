package com.yosyoo.shopperassistant.barcode

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.oned.Code39Writer
import java.util.Locale

class InvalidCode39ContentException(message: String) : IllegalArgumentException(message)

object Code39Generator {
    private const val MaxLength = 80
    private val allowedCharacters = buildSet {
        addAll('A'..'Z')
        addAll('0'..'9')
        addAll(listOf(' ', '-', '.', '$', '/', '+', '%'))
    }

    fun normalizeOrThrow(input: String): String {
        val normalized = input.trim().uppercase(Locale.US)
        if (normalized.isEmpty()) {
            throw InvalidCode39ContentException("请输入条码内容")
        }
        if (normalized.length > MaxLength) {
            throw InvalidCode39ContentException("Code39 最多支持 $MaxLength 个字符")
        }
        if (normalized.any { it !in allowedCharacters }) {
            throw InvalidCode39ContentException("该内容不能生成 Code39")
        }
        return normalized
    }

    fun generate(
        text: String,
        width: Int = 1200,
        height: Int = 420,
        margin: Int = 24,
    ): Bitmap {
        val normalized = normalizeOrThrow(text)
        val textAreaHeight = 88
        val barcodeHeight = (height - textAreaHeight).coerceAtLeast(180)
        val matrix = Code39Writer().encode(
            normalized,
            BarcodeFormat.CODE_39,
            width,
            barcodeHeight,
            mapOf(EncodeHintType.MARGIN to margin),
        )

        val pixels = IntArray(matrix.width * matrix.height)
        for (y in 0 until matrix.height) {
            val offset = y * matrix.width
            for (x in 0 until matrix.width) {
                pixels[offset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }

        val barcodeBitmap = Bitmap.createBitmap(matrix.width, matrix.height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, matrix.width, 0, 0, matrix.width, matrix.height)
        }
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            canvas.drawColor(Color.WHITE)
            canvas.drawBitmap(barcodeBitmap, 0f, 0f, null)
            canvas.drawHumanReadableText(
                text = normalized,
                width = width,
                top = barcodeHeight.toFloat(),
                height = textAreaHeight.toFloat(),
                horizontalPadding = margin.toFloat(),
            )
        }
    }

    private fun Canvas.drawHumanReadableText(
        text: String,
        width: Int,
        top: Float,
        height: Float,
        horizontalPadding: Float,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            textSize = 52f
        }

        val maxTextWidth = width - horizontalPadding * 2
        while (paint.measureText(text) > maxTextWidth && paint.textSize > 24f) {
            paint.textSize -= 2f
        }

        val fontMetrics = paint.fontMetrics
        val baseline = top + (height - fontMetrics.ascent - fontMetrics.descent) / 2f
        drawText(text, width / 2f, baseline, paint)
    }
}
