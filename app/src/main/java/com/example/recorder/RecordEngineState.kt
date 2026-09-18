package com.example.recorder

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class RecordEngineState(
    val isRecording: Boolean = false,
    val isPaused: Boolean = false,
    val elapsedSeconds: Long = 0L,
    val currentFilePath: String? = null,
    val currentTitle: String? = null,
    val error: String? = null
)

object RecordEngineController {
    private val _state = MutableStateFlow(RecordEngineState())
    val state = _state.asStateFlow()

    fun updateState(transform: (RecordEngineState) -> RecordEngineState) {
        _state.value = transform(_state.value)
    }

    fun reset() {
        _state.value = RecordEngineState()
    }
}
