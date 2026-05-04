package com.yosyoo.shopperassistant.barcode

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object BarcodeImageStorage {
    fun saveToPictures(
        context: Context,
        bitmap: Bitmap,
        barcodeText: String,
    ): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName(barcodeText))
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/ShopperAssistant",
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("无法创建图片文件")

        try {
            resolver.openOutputStream(uri)?.use { outputStream ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                    "无法写入 PNG 图片"
                }
            } ?: error("无法打开图片文件")

            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (throwable: Throwable) {
            resolver.delete(uri, null, null)
            throw throwable
        }
    }

    fun createShareIntent(
        context: Context,
        bitmap: Bitmap,
        barcodeText: String,
    ): Intent {
        val sharedDirectory = File(context.cacheDir, "shared_images").apply {
            mkdirs()
        }
        val imageFile = File(sharedDirectory, fileName(barcodeText))
        FileOutputStream(imageFile).use { outputStream ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                "无法写入分享图片"
            }
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile,
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun fileName(barcodeText: String): String {
        val safeSegment = barcodeText
            .filter { it.isLetterOrDigit() }
            .take(24)
            .ifBlank { "code39" }
        return "code39_${safeSegment}_${System.currentTimeMillis()}.png"
    }
}
