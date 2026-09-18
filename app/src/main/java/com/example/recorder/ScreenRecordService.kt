package com.example.recorder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.RecordingEntity
import com.example.data.RecordingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ScreenRecordService : Service() {

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var mediaRecorder: MediaRecorder? = null

    private var recordingWidth = 1080
    private var recordingHeight = 1920
    private var screenDensityDpi = 420
    private var recordingFps = 60
    private var recordingBitrateMbps = 12
    private var audioOptionName = AudioOption.MICROPHONE.name
    private var currentTitle = "Screen Recording"
    private var outputFile: File? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null
    private var startTimeMs = 0L
    private var elapsedSeconds = 0L
    private var isPaused = false

    private lateinit var notificationManager: NotificationManager
    private val channelId = "screen_recording_channel"
    private val notificationId = 1001

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START
        when (action) {
            ACTION_START -> {
                val resultCode = intent?.getIntExtra(EXTRA_RESULT_CODE, 0) ?: 0
                val resultData = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent?.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent?.getParcelableExtra(EXTRA_RESULT_DATA)
                }

                recordingWidth = intent?.getIntExtra(EXTRA_WIDTH, 1080) ?: 1080
                recordingHeight = intent?.getIntExtra(EXTRA_HEIGHT, 1920) ?: 1920
                screenDensityDpi = intent?.getIntExtra(EXTRA_DPI, 420) ?: 420
                recordingFps = intent?.getIntExtra(EXTRA_FPS, 60) ?: 60
                recordingBitrateMbps = intent?.getIntExtra(EXTRA_BITRATE, 12) ?: 12
                audioOptionName = intent?.getStringExtra(EXTRA_AUDIO_OPTION) ?: AudioOption.MICROPHONE.name
                currentTitle = intent?.getStringExtra(EXTRA_TITLE) ?: "Screen Recording"

                if (resultCode != 0 && resultData != null) {
                    startForegroundNotification()
                    startRecording(resultCode, resultData)
                } else {
                    stopSelf()
                }
            }
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Screen Recorder Controls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live recording status and controls"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundNotification() {
        val notification = buildNotification("00:00", isPaused = false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            }
            startForeground(notificationId, notification, type)
        } else {
            startForeground(notificationId, notification)
        }
    }

    private fun buildNotification(timeFormatted: String, isPaused: Boolean): Notification {
        val appIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingAppIntent = PendingIntent.getActivity(
            this, 0, appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, ScreenRecordService::class.java).apply { action = ACTION_STOP }
        val pendingStop = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeIntent = Intent(this, ScreenRecordService::class.java).apply {
            action = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        }
        val pendingPauseResume = PendingIntent.getService(
            this, 2, pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusText = if (isPaused) "Paused • $timeFormatted" else "Recording • $timeFormatted"

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentTitle("Screen Recorder")
            .setContentText(statusText)
            .setContentIntent(pendingAppIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(
                if (isPaused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (isPaused) "Resume" else "Pause",
                pendingPauseResume
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop & Save", pendingStop)
            .build()
    }

    private fun startRecording(resultCode: Int, resultData: Intent) {
        try {
            val projectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, resultData)

            if (mediaProjection == null) {
                RecordEngineController.updateState { it.copy(error = "Could not initialize media projection") }
                stopSelf()
                return
            }

            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    stopRecording()
                }
            }, null)

            // Setup file destination in external files dir (Recordings folder)
            val dir = File(getExternalFilesDir(null), "Recordings").apply { mkdirs() }
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val sanitizedTitle = currentTitle.trim().replace(Regex("[^a-zA-Z0-9_ -]"), "").ifEmpty { "ScreenRecording" }
            outputFile = File(dir, "${sanitizedTitle}_${timeStamp}.mp4")

            initMediaRecorder(outputFile!!)

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenRecordDisplay",
                recordingWidth,
                recordingHeight,
                screenDensityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mediaRecorder?.surface,
                null,
                null
            )

            mediaRecorder?.start()
            startTimeMs = System.currentTimeMillis()
            elapsedSeconds = 0L
            isPaused = false

            RecordEngineController.updateState {
                it.copy(
                    isRecording = true,
                    isPaused = false,
                    elapsedSeconds = 0L,
                    currentFilePath = outputFile?.absolutePath,
                    currentTitle = currentTitle,
                    error = null
                )
            }

            startTimer()

        } catch (e: Exception) {
            RecordEngineController.updateState {
                it.copy(isRecording = false, error = "Failed to start recorder: ${e.localizedMessage}")
            }
            stopRecording()
        }
    }

    private fun initMediaRecorder(file: File) {
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(this)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        val hasMic = audioOptionName == AudioOption.MICROPHONE.name
        if (hasMic) {
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        }

        recorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setOutputFile(file.absolutePath)
        recorder.setVideoSize(recordingWidth, recordingHeight)
        recorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)

        if (hasMic) {
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(128000)
            recorder.setAudioSamplingRate(44100)
        }

        recorder.setVideoEncodingBitRate(recordingBitrateMbps * 1_000_000)
        recorder.setVideoFrameRate(recordingFps)

        recorder.prepare()
        mediaRecorder = recorder
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                if (!isPaused) {
                    elapsedSeconds++
                    val timeFormatted = formatTime(elapsedSeconds)
                    RecordEngineController.updateState { it.copy(elapsedSeconds = elapsedSeconds) }
                    notificationManager.notify(notificationId, buildNotification(timeFormatted, isPaused))
                }
            }
        }
    }

    private fun pauseRecording() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !isPaused) {
                mediaRecorder?.pause()
                isPaused = true
                RecordEngineController.updateState { it.copy(isPaused = true) }
                notificationManager.notify(notificationId, buildNotification(formatTime(elapsedSeconds), isPaused = true))
            }
        } catch (_: Exception) {}
    }

    private fun resumeRecording() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isPaused) {
                mediaRecorder?.resume()
                isPaused = false
                RecordEngineController.updateState { it.copy(isPaused = false) }
                notificationManager.notify(notificationId, buildNotification(formatTime(elapsedSeconds), isPaused = false))
            }
        } catch (_: Exception) {}
    }

    private fun stopRecording() {
        timerJob?.cancel()

        try {
            mediaRecorder?.stop()
        } catch (_: Exception) {}

        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (_: Exception) {}
        mediaRecorder = null

        try {
            virtualDisplay?.release()
        } catch (_: Exception) {}
        virtualDisplay = null

        try {
            mediaProjection?.stop()
        } catch (_: Exception) {}
        mediaProjection = null

        // Save entry into database
        val file = outputFile
        if (file != null && file.exists() && file.length() > 0) {
            val durationMs = elapsedSeconds * 1000L
            val finalTitle = currentTitle
            val entity = RecordingEntity(
                title = finalTitle,
                filePath = file.absolutePath,
                durationMs = durationMs,
                fileSizeBytes = file.length(),
                width = recordingWidth,
                height = recordingHeight,
                fps = recordingFps,
                bitrateMbps = recordingBitrateMbps,
                hasAudio = audioOptionName != AudioOption.MUTED.name,
                audioSource = audioOptionName,
                timestamp = System.currentTimeMillis()
            )

            serviceScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(applicationContext)
                val repo = RecordingRepository(db.recordingDao())
                repo.insertRecording(entity)
            }
        }

        RecordEngineController.reset()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        timerJob?.cancel()
        try {
            mediaRecorder?.release()
            virtualDisplay?.release()
            mediaProjection?.stop()
        } catch (_: Exception) {}
        RecordEngineController.reset()
    }

    private fun formatTime(seconds: Long): String {
        val m = seconds / 60
        val s = seconds % 60
        return String.format(Locale.US, "%02d:%02d", m, s)
    }

    companion object {
        const val ACTION_START = "com.example.recorder.START"
        const val ACTION_STOP = "com.example.recorder.STOP"
        const val ACTION_PAUSE = "com.example.recorder.PAUSE"
        const val ACTION_RESUME = "com.example.recorder.RESUME"

        const val EXTRA_RESULT_CODE = "extra_result_code"
        const val EXTRA_RESULT_DATA = "extra_result_data"
        const val EXTRA_WIDTH = "extra_width"
        const val EXTRA_HEIGHT = "extra_height"
        const val EXTRA_DPI = "extra_dpi"
        const val EXTRA_FPS = "extra_fps"
        const val EXTRA_BITRATE = "extra_bitrate"
        const val EXTRA_AUDIO_OPTION = "extra_audio_option"
        const val EXTRA_TITLE = "extra_title"
    }
}
