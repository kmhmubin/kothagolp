package com.emptycastle.novery.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ImageUtils {

    /**
     * Copy image from URI to app's internal storage
     * @return File path of the copied image
     */
    suspend fun saveImageToInternalStorage(
        context: Context,
        imageUri: Uri,
        novelUrl: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(imageUri) ?: return@withContext null
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            // Create covers directory
            val coversDir = File(context.filesDir, "covers")
            if (!coversDir.exists()) {
                coversDir.mkdirs()
            }

            // Generate filename from novel URL
            val filename = "cover_${novelUrl.hashCode().toString().replace("-", "")}.jpg"
            val file = File(coversDir, filename)

            // Compress and save
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            file.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Delete custom cover image
     */
    suspend fun deleteCustomCover(context: Context, filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (filePath.startsWith("file://") || filePath.startsWith("/")) {
                val file = File(filePath.removePrefix("file://"))
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Get URI for sharing/viewing custom cover
     */
    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
}