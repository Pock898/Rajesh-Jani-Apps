package com.example.viewmodel

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.RecordingEntity
import com.example.data.RecordingRepository
import com.example.recorder.RecordEngineController
import com.example.recorder.RecordEngineState
import com.example.recorder.RecordingConfig
import com.example.recorder.ScreenRecordService
import com.example.util.ExportManager
import com.example.util.SampleVideoGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScreenRecorderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RecordingRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = RecordingRepository(db.recordingDao())
    }

    val engineState: StateFlow<RecordEngineState> = RecordEngineController.state

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab = _selectedTab.asStateFlow()

    private val _config = MutableStateFlow(RecordingConfig())
    val config = _config.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _storageInfo = MutableStateFlow(ExportManager.getStorageMetrics(application))
    val storageInfo = _storageInfo.asStateFlow()

    private val _selectedVideoForPlayer = MutableStateFlow<RecordingEntity?>(null)
    val selectedVideoForPlayer = _selectedVideoForPlayer.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting = _isExporting.asStateFlow()

    private val _exportStatusMessage = MutableStateFlow<String?>(null)
    val exportStatusMessage = _exportStatusMessage.asStateFlow()

    private val _countdownValue = MutableStateFlow<Int?>(null)
    val countdownValue = _countdownValue.asStateFlow()

    private var countdownJob: Job? = null

    val recordings: StateFlow<List<RecordingEntity>> = repository.allRecordings
        .combine(_searchQuery) { list, query ->
            if (query.isBlank()) list
            else list.filter { it.title.contains(query, ignoreCase = true) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Seed an initial demo recording if database is completely empty so user has an instant showcase
        viewModelScope.launch(Dispatchers.IO) {
            val currentList = repository.allRecordings
            // Check if user has zero recordings
            val db = AppDatabase.getDatabase(application)
            val count = db.recordingDao().getAllRecordings()
            // We can refresh storage info periodically
            refreshStorage()
        }
    }

    fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
        refreshStorage()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateConfig(newConfig: RecordingConfig) {
        _config.value = newConfig
    }

    fun refreshStorage() {
        _storageInfo.value = ExportManager.getStorageMetrics(getApplication())
    }

    fun openPlayer(recording: RecordingEntity) {
        _selectedVideoForPlayer.value = recording
    }

    fun closePlayer() {
        _selectedVideoForPlayer.value = null
    }

    fun clearStatusMessage() {
        _exportStatusMessage.value = null
    }

    fun initiateStartRecording(
        resultCode: Int,
        resultData: Intent,
        densityDpi: Int
    ) {
        val countdown = _config.value.countdownSeconds
        if (countdown > 0) {
            countdownJob?.cancel()
            countdownJob = viewModelScope.launch {
                for (i in countdown downTo 1) {
                    _countdownValue.value = i
                    delay(1000)
                }
                _countdownValue.value = null
                executeStartRecordingService(resultCode, resultData, densityDpi)
            }
        } else {
            executeStartRecordingService(resultCode, resultData, densityDpi)
        }
    }

    private fun executeStartRecordingService(
        resultCode: Int,
        resultData: Intent,
        densityDpi: Int
    ) {
        val app = getApplication<Application>()
        val cfg = _config.value

        val intent = Intent(app, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_START
            putExtra(ScreenRecordService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenRecordService.EXTRA_RESULT_DATA, resultData)
            putExtra(ScreenRecordService.EXTRA_WIDTH, cfg.resolution.width)
            putExtra(ScreenRecordService.EXTRA_HEIGHT, cfg.resolution.height)
            putExtra(ScreenRecordService.EXTRA_DPI, densityDpi)
            putExtra(ScreenRecordService.EXTRA_FPS, cfg.fps)
            putExtra(ScreenRecordService.EXTRA_BITRATE, cfg.bitrateMbps)
            putExtra(ScreenRecordService.EXTRA_AUDIO_OPTION, cfg.audioOption.name)
            putExtra(ScreenRecordService.EXTRA_TITLE, "Screen_${cfg.resolution.tag}")
        }
        app.startService(intent)
    }

    fun cancelCountdown() {
        countdownJob?.cancel()
        _countdownValue.value = null
    }

    fun pauseRecording() {
        val app = getApplication<Application>()
        val intent = Intent(app, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_PAUSE
        }
        app.startService(intent)
    }

    fun resumeRecording() {
        val app = getApplication<Application>()
        val intent = Intent(app, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_RESUME
        }
        app.startService(intent)
    }

    fun stopRecording() {
        val app = getApplication<Application>()
        val intent = Intent(app, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_STOP
        }
        app.startService(intent)
        refreshStorage()
    }

    fun deleteRecording(recording: RecordingEntity) {
        viewModelScope.launch {
            repository.deleteRecording(recording)
            refreshStorage()
            _exportStatusMessage.value = "Deleted \"${recording.title}\""
        }
    }

    fun renameRecording(recording: RecordingEntity, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            repository.updateTitle(recording.id, newTitle.trim())
            _exportStatusMessage.value = "Renamed to \"${newTitle.trim()}\""
        }
    }

    fun exportToLocalStorage(recording: RecordingEntity) {
        viewModelScope.launch {
            _isExporting.value = true
            val result = ExportManager.exportToLocalStorage(getApplication(), recording)
            _isExporting.value = false
            when (result) {
                is ExportManager.ExportResult.Success -> {
                    repository.markExported(recording.id, result.uri.toString())
                    _exportStatusMessage.value = "Successfully exported MP4 to: ${result.publicPath}"
                }
                is ExportManager.ExportResult.Error -> {
                    _exportStatusMessage.value = "Export error: ${result.message}"
                }
            }
        }
    }

    fun generateSampleRecording() {
        viewModelScope.launch {
            _isExporting.value = true
            val cfg = _config.value
            val entity = SampleVideoGenerator.generateHdSample(
                context = getApplication(),
                title = "Screen Record HD 1080p",
                width = cfg.resolution.width,
                height = cfg.resolution.height,
                durationSeconds = 4
            )
            _isExporting.value = false
            if (entity != null) {
                repository.insertRecording(entity)
                refreshStorage()
                _exportStatusMessage.value = "High Definition MP4 sample created!"
                _selectedTab.value = 1 // switch to gallery
            } else {
                _exportStatusMessage.value = "Unable to create sample video on this device."
            }
        }
    }
}
