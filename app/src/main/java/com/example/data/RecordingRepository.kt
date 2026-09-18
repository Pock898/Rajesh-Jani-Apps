package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class RecordingRepository(private val recordingDao: RecordingDao) {

    val allRecordings: Flow<List<RecordingEntity>> = recordingDao.getAllRecordings()

    suspend fun insertRecording(recording: RecordingEntity): Long = withContext(Dispatchers.IO) {
        recordingDao.insertRecording(recording)
    }

    suspend fun getRecordingById(id: Long): RecordingEntity? = withContext(Dispatchers.IO) {
        recordingDao.getRecordingById(id)
    }

    suspend fun deleteRecording(recording: RecordingEntity) = withContext(Dispatchers.IO) {
        // Delete physical file if it exists
        try {
            val file = File(recording.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (_: Exception) {
        }
        recordingDao.deleteRecording(recording)
    }

    suspend fun markExported(id: Long, exportUri: String) = withContext(Dispatchers.IO) {
        recordingDao.markAsExported(id, exportUri)
    }

    suspend fun updateTitle(id: Long, newTitle: String) = withContext(Dispatchers.IO) {
        recordingDao.updateTitle(id, newTitle)
    }
}
