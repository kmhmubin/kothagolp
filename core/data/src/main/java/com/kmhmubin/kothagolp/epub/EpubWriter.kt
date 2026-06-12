package com.kmhmubin.kothagolp.epub

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.URL
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Writes EPUB files with proper structure
 */
class EpubWriter(private val context: Context) {

    /**
     * Write EPUB to an output stream
     */
    suspend fun writeEpub(
        outputStream: OutputStream,
        metadata: EpubMetadata,
        chapters: List<EpubChapter>,
        coverImageBytes: ByteArray? = null,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val hasCover = coverImageBytes != null
            val totalSteps = chapters.size + 5 // Cover, title, nav, styles, finalize
            var currentStep = 0

            ZipOutputStream(outputStream).use { zip ->
                // 1. MIMETYPE - Must be first and uncompressed
                onProgress(++currentStep, totalSteps, "Writing mimetype...")
                writeMimetype(zip)

                // 2. META-INF/container.xml
                onProgress(++currentStep, totalSteps, "Writing container...")
                writeEntry(zip, "META-INF/container.xml", EpubBuilder.buildContainerXml())

                // 3. OEBPS/content.opf
                onProgress(++currentStep, totalSteps, "Writing package document...")
                writeEntry(
                    zip,
                    "OEBPS/content.opf",
                    EpubBuilder.buildContentOpf(metadata, chapters, hasCover)
                )

                // 4. OEBPS/toc.ncx
                writeEntry(zip, "OEBPS/toc.ncx", EpubBuilder.buildTocNcx(metadata, chapters))

                // 5. OEBPS/nav.xhtml
                writeEntry(zip, "OEBPS/nav.xhtml", EpubBuilder.buildNavXhtml(metadata, chapters))

                // 6. OEBPS/styles.css
                onProgress(++currentStep, totalSteps, "Writing styles...")
                writeEntry(zip, "OEBPS/styles.css", EpubBuilder.buildStylesCss())

                // 7. Cover image (if available)
                if (coverImageBytes != null) {
                    onProgress(++currentStep, totalSteps, "Writing cover image...")
                    writeEntry(zip, "OEBPS/cover.jpg", coverImageBytes)
                    writeEntry(zip, "OEBPS/cover.xhtml", EpubBuilder.buildCoverXhtml(metadata))
                }

                // 8. Title page
                onProgress(++currentStep, totalSteps, "Writing title page...")
                writeEntry(
                    zip,
                    "OEBPS/title.xhtml",
                    EpubBuilder.buildTitleXhtml(metadata, chapters.size)
                )

                // 9. Chapters
                chapters.forEachIndexed { index, chapter ->
                    onProgress(
                        currentStep + index + 1,
                        totalSteps,
                        "Writing chapter ${index + 1}/${chapters.size}..."
                    )
                    writeEntry(
                        zip,
                        "OEBPS/chapters/${chapter.fileName}",
                        EpubBuilder.buildChapterXhtml(chapter)
                    )
                }

                onProgress(totalSteps, totalSteps, "Finalizing EPUB...")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Write EPUB to a URI
     */
    suspend fun writeEpubToUri(
        uri: Uri,
        metadata: EpubMetadata,
        chapters: List<EpubChapter>,
        coverImageBytes: ByteArray? = null,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> }
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                writeEpub(outputStream, metadata, chapters, coverImageBytes, onProgress)
            } ?: Result.failure(Exception("Could not open output stream"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Download and process cover image
     */
    suspend fun downloadCoverImage(url: String, maxSize: Int = 1200): ByteArray? =
        withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection()
                connection.connectTimeout = 10000
                connection.readTimeout = 15000

                connection.getInputStream().use { input ->
                    val originalBitmap = BitmapFactory.decodeStream(input)
                        ?: return@withContext null

                    // Resize if needed
                    val bitmap = if (originalBitmap.width > maxSize ||
                        originalBitmap.height > maxSize) {
                        val scale = minOf(
                            maxSize.toFloat() / originalBitmap.width,
                            maxSize.toFloat() / originalBitmap.height
                        )
                        val newWidth = (originalBitmap.width * scale).toInt()
                        val newHeight = (originalBitmap.height * scale).toInt()
                        Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true).also {
                            if (it != originalBitmap) originalBitmap.recycle()
                        }
                    } else {
                        originalBitmap
                    }

                    // Convert to JPEG bytes
                    ByteArrayOutputStream().use { baos ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                        bitmap.recycle()
                        baos.toByteArray()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

    // ================================================================
    // PRIVATE HELPERS
    // ================================================================

    /**
     * Write mimetype as first entry, uncompressed (EPUB spec requirement)
     */
    private fun writeMimetype(zip: ZipOutputStream) {
        val mimetypeBytes = "application/epub+zip".toByteArray(Charsets.US_ASCII)

        val entry = ZipEntry("mimetype").apply {
            method = ZipEntry.STORED
            size = mimetypeBytes.size.toLong()
            compressedSize = mimetypeBytes.size.toLong()
            crc = CRC32().apply { update(mimetypeBytes) }.value
        }

        zip.putNextEntry(entry)
        zip.write(mimetypeBytes)
        zip.closeEntry()
    }

    private fun writeEntry(zip: ZipOutputStream, path: String, content: String) {
        writeEntry(zip, path, content.toByteArray(Charsets.UTF_8))
    }

    private fun writeEntry(zip: ZipOutputStream, path: String, content: ByteArray) {
        val entry = ZipEntry(path).apply {
            method = ZipEntry.DEFLATED
        }
        zip.putNextEntry(entry)
        zip.write(content)
        zip.closeEntry()
    }
}