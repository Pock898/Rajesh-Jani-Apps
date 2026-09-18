package com.example.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.example.data.RecordingEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale

object ExportManager {

    sealed class ExportResult {
        data class Success(val publicPath: String, val uri: Uri) : ExportResult()
        data class Error(val message: String) : ExportResult()
    }

    suspend fun exportToLocalStorage(
        context: Context,
        recording: RecordingEntity
    ): ExportResult = withContext(Dispatchers.IO) {
        val sourceFile = File(recording.filePath)
        if (!sourceFile.exists()) {
            return@withContext ExportResult.Error("Source video file not found on disk")
        }

        val sanitizedTitle = recording.title
            .trim()
            .replace(Regex("[^a-zA-Z0-9_ -]"), "")
            .ifEmpty { "ScreenRecording" }
        val fileName = "${sanitizedTitle}_HD.mp4"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                    put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/ScreenRecorder")
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                    put(MediaStore.Video.Media.DATE_ADDED, System.currentTimeMillis() / 1000)
                }

                val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
                val itemUri = resolver.insert(collection, values)
                    ?: return@withContext ExportResult.Error("Failed to create MediaStore entry")

                resolver.openOutputStream(itemUri)?.use { outputStream ->
                    FileInputStream(sourceFile).use { inputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: return@withContext ExportResult.Error("Failed to open output stream")

                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(itemUri, values, null, null)

                ExportResult.Success("Movies/ScreenRecorder/$fileName", itemUri)
            } else {
                val moviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
                val targetDir = File(moviesDir, "ScreenRecorder").apply { mkdirs() }
                val targetFile = File(targetDir, fileName)

                FileInputStream(sourceFile).use { inputStream ->
                    FileOutputStream(targetFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(targetFile.absolutePath),
                    arrayOf("video/mp4"),
                    null
                )

                val uri = Uri.fromFile(targetFile)
                ExportResult.Success(targetFile.absolutePath, uri)
            }
        } catch (e: Exception) {
            ExportResult.Error("Export failed: ${e.localizedMessage}")
        }
    }

    fun shareVideo(context: Context, recording: RecordingEntity) {
        val file = File(recording.filePath)
        if (!file.exists()) return

        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            Uri.fromFile(file)
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, recording.title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Screen Recording (MP4)"))
    }

    fun openInExternalPlayer(context: Context, recording: RecordingEntity) {
        val file = File(recording.filePath)
        if (!file.exists()) return

        val uri = try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (_: Exception) {
            Uri.fromFile(file)
        }

        val viewIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(viewIntent)
        } catch (_: Exception) {
            shareVideo(context, recording)
        }
    }

    fun formatDuration(durationMs: Long): String {
        val totalSec = durationMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format(Locale.US, "%02d:%02d", min, sec)
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    fun getStorageMetrics(context: Context): StorageInfo {
        return try {
            val path = context.getExternalFilesDir(null) ?: Environment.getDataDirectory()
            val stat = StatFs(path.path)
            val blockSize = stat.blockSizeLong
            val availableBlocks = stat.availableBlocksLong
            val totalBlocks = stat.blockCountLong

            val freeBytes = availableBlocks * blockSize
            val totalBytes = totalBlocks * blockSize

            val freeGb = freeBytes / (1024.0 * 1024.0 * 1024.0)
            val totalGb = totalBytes / (1024.0 * 1024.0 * 1024.0)
            val usedGb = (totalBytes - freeBytes) / (1024.0 * 1024.0 * 1024.0)
            val usedRatio = if (totalBytes > 0) (totalBytes - freeBytes).toFloat() / totalBytes else 0f

            // Estimate hours remaining at 12 Mbps (~1.5 MB/sec ~ 5.4 GB/hr)
            val estimatedHours = if (freeGb > 0.0) freeGb / 5.4 else 0.0

            StorageInfo(freeGb, totalGb, usedGb, usedRatio, estimatedHours)
        } catch (_: Exception) {
            StorageInfo(16.0, 64.0, 48.0, 0.75f, 3.2)
        }
    }

    data class StorageInfo(
        val freeGb: Double,
        val totalGb: Double,
        val usedGb: Double,
        val usedRatio: Float,
        val estimatedHours: Double
    )
}
