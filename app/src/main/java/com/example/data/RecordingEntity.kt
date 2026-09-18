package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val durationMs: Long = 0L,
    val fileSizeBytes: Long = 0L,
    val width: Int = 1080,
    val height: Int = 1920,
    val fps: Int = 60,
    val bitrateMbps: Int = 12,
    val hasAudio: Boolean = true,
    val audioSource: String = "Microphone",
    val timestamp: Long = System.currentTimeMillis(),
    val isExported: Boolean = false,
    val exportUri: String? = null
)
