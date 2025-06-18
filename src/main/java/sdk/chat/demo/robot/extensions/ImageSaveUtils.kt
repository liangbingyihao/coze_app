package sdk.chat.demo.robot.extensions

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

/**
 * 图片保存工具类（兼容 Android 10+ 和旧版本）
 */
object ImageSaveUtils {

    /**
     * 保存 Bitmap 到系统相册（自动选择最佳方式）
     * @param context Context
     * @param bitmap 要保存的 Bitmap
     * @param filename 文件名（不含后缀）
     * @param format 图片格式（JPEG/PNG/WEBP）
     * @return 保存成功返回 Uri，失败返回 null
     */
    fun saveBitmapToGallery(
        context: Context,
        bitmap: Bitmap,
        filename: String,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
    ): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveViaMediaStore(context, bitmap, filename, format) // Android 10+ 方式
        } else {
            saveLegacy(context, bitmap, filename, format) // 旧版本方式
        }
    }

    /**
     * Android 10+ 使用 MediaStore API 保存
     */
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun saveViaMediaStore(
        context: Context,
        bitmap: Bitmap,
        filename: String,
        format: Bitmap.CompressFormat
    ): Uri? {
        val contentResolver = context.contentResolver
        val mimeType = when (format) {
            Bitmap.CompressFormat.JPEG -> "image/jpeg"
            Bitmap.CompressFormat.PNG -> "image/png"
            Bitmap.CompressFormat.WEBP -> "image/webp"
            else -> "image/jpeg"
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.${format.name.lowercase()}")
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        return try {
            val uri =
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return null

            contentResolver.openOutputStream(uri)?.use { os ->
                if (bitmap.compress(format, 100, os)) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                    contentResolver.update(uri, contentValues, null, null)
                    uri
                } else {
                    contentResolver.delete(uri, null, null)
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Android 9 及以下版本使用传统方式保存
     */
    @Suppress("DEPRECATION")
    private fun saveLegacy(
        context: Context,
        bitmap: Bitmap,
        filename: String,
        format: Bitmap.CompressFormat
    ): Uri? {
        val picturesDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        if (!picturesDir.exists() && !picturesDir.mkdirs()) {
            return null
        }

        val imageFile = File(picturesDir, "$filename.${format.name.lowercase()}")
        return try {
            FileOutputStream(imageFile).use { fos ->
                if (bitmap.compress(format, 100, fos)) {
                    // 通知系统扫描新文件（显示在图库中）
                    context.sendBroadcast(
                        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(imageFile))
                    )
                    Uri.fromFile(imageFile)
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}