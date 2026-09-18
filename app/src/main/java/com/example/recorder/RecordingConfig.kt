package com.example.recorder

enum class AudioOption(val label: String, val description: String) {
    MICROPHONE("Microphone", "Records your voice and external sounds"),
    INTERNAL("Internal Device Audio", "Captures in-app sounds and media"),
    MUTED("Mute (No Audio)", "Silent recording without any audio track")
}

enum class OrientationOption(val label: String) {
    AUTO("Auto (Follow Screen)"),
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape")
}

data class ResolutionPreset(
    val label: String,
    val width: Int,
    val height: Int,
    val tag: String
)

val AvailableResolutions = listOf(
    ResolutionPreset("1080p Full HD", 1080, 1920, "FHD"),
    ResolutionPreset("2K Quad HD", 1440, 2560, "2K"),
    ResolutionPreset("720p HD", 720, 1280, "HD"),
    ResolutionPreset("480p SD", 480, 854, "SD")
)

val AvailableFramerates = listOf(60, 30, 24)
val AvailableBitrates = listOf(16, 12, 8, 4) // in Mbps
val AvailableCountdowns = listOf(3, 5, 0) // in seconds

data class RecordingConfig(
    val resolution: ResolutionPreset = AvailableResolutions[0],
    val fps: Int = 60,
    val bitrateMbps: Int = 12,
    val audioOption: AudioOption = AudioOption.MICROPHONE,
    val countdownSeconds: Int = 3,
    val orientation: OrientationOption = OrientationOption.AUTO,
    val stopOnScreenOff: Boolean = true,
    val showTouchesHint: Boolean = false
)
