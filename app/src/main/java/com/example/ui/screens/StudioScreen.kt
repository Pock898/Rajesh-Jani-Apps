package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.recorder.AudioOption
import com.example.recorder.RecordEngineState
import com.example.recorder.RecordingConfig
import com.example.ads.AdMobBannerView
import com.example.ui.components.StitchBadge
import com.example.ui.theme.StitchBlue
import com.example.ui.theme.StitchBlueContainer
import com.example.ui.theme.StitchGreen
import com.example.ui.theme.StitchGreenContainer
import com.example.ui.theme.StitchOutline
import com.example.ui.theme.StitchRed
import com.example.ui.theme.StitchRedContainer
import com.example.ui.theme.StitchSurface
import com.example.ui.theme.StitchSurfaceContainer
import com.example.ui.theme.StitchYellow
import com.example.util.ExportManager
import java.util.Locale

@Composable
fun StudioScreen(
    engineState: RecordEngineState,
    config: RecordingConfig,
    storageInfo: ExportManager.StorageInfo,
    countdownValue: Int?,
    onStartRequested: () -> Unit,
    onStopRequested: () -> Unit,
    onPauseRequested: () -> Unit,
    onResumeRequested: () -> Unit,
    onCancelCountdown: () -> Unit,
    onGenerateSample: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToRecordings: () -> Unit,
    onToggleAudio: () -> Unit,
    onToggleCountdown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecording = engineState.isRecording
    val isPaused = engineState.isPaused
    val scrollState = rememberScrollState()

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isRecording && !isPaused) 1.14f else 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Stitch Light styling
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Screen Recorder",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Full Screen High Definition MP4",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isRecording) {
                    StitchBadge(
                        text = if (isPaused) "PAUSED" else "RECORDING",
                        backgroundColor = StitchRedContainer,
                        textColor = StitchRed,
                        dotColor = StitchRed
                    )
                } else {
                    StitchBadge(
                        text = "READY",
                        backgroundColor = StitchGreenContainer,
                        textColor = StitchGreen,
                        dotColor = StitchGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Hero Record Center Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Status subtitle
                    Text(
                        text = if (isRecording) "Current Recording Time" else "Tap to start screen capture",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Timer or Status Display
                    val timeFormatted = formatSeconds(engineState.elapsedSeconds)
                    Text(
                        text = if (isRecording) timeFormatted else "00:00",
                        fontSize = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isRecording) StitchRed else MaterialTheme.colorScheme.onSurface,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Pulse Circle Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(170.dp)
                    ) {
                        // Outer animated halo
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .scale(pulseScale)
                                .clip(CircleShape)
                                .background(
                                    if (isRecording) {
                                        StitchRed.copy(alpha = 0.12f)
                                    } else {
                                        StitchBlue.copy(alpha = 0.10f)
                                    }
                                )
                        )

                        // Middle ring
                        Box(
                            modifier = Modifier
                                .size(136.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isRecording) {
                                        StitchRed.copy(alpha = 0.20f)
                                    } else {
                                        StitchBlue.copy(alpha = 0.18f)
                                    }
                                )
                        )

                        // Main action button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(108.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        colors = if (isRecording) {
                                            listOf(StitchRed, Color(0xFFC5221F))
                                        } else {
                                            listOf(StitchBlue, Color(0xFF1557B0))
                                        }
                                    )
                                )
                                .clickable {
                                    if (isRecording) {
                                        onStopRequested()
                                    } else {
                                        onStartRequested()
                                    }
                                }
                                .testTag("record_fab_button")
                        ) {
                            if (isRecording) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Recording",
                                    tint = Color.White,
                                    modifier = Modifier.size(46.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = "Start Recording",
                                    tint = Color.White,
                                    modifier = Modifier.size(46.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Control buttons during recording
                    if (isRecording) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (isPaused) onResumeRequested() else onPauseRequested()
                                },
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("pause_resume_button")
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                    contentDescription = if (isPaused) "Resume" else "Pause"
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(if (isPaused) "Resume" else "Pause")
                            }

                            Button(
                                onClick = onStopRequested,
                                colors = ButtonDefaults.buttonColors(containerColor = StitchRed),
                                shape = RoundedCornerShape(20.dp),
                                modifier = Modifier.testTag("stop_recording_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop"
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop & Save MP4")
                            }
                        }
                    } else {
                        Text(
                            text = "Instant high definition screen recorder\nSaved directly to your local storage",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // High Definition Preset Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpecBadge(
                    label = "Resolution",
                    value = config.resolution.tag,
                    modifier = Modifier.weight(1f)
                )
                SpecBadge(
                    label = "Framerate",
                    value = "${config.fps} FPS",
                    modifier = Modifier.weight(1f)
                )
                SpecBadge(
                    label = "Bitrate",
                    value = "${config.bitrateMbps} Mbps",
                    modifier = Modifier.weight(1f)
                )
                SpecBadge(
                    label = "Audio",
                    value = if (config.audioOption == AudioOption.MUTED) "Muted" else "Mic",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Storage Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = StitchSurface),
                border = androidx.compose.foundation.BorderStroke(1.dp, StitchOutline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SdStorage,
                                contentDescription = "Storage",
                                tint = StitchBlue,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Local Storage",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Text(
                            text = String.format(Locale.US, "%.1f GB Free", storageInfo.freeGb),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = StitchGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { storageInfo.usedRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = StitchBlue,
                        trackColor = StitchSurfaceContainer
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = String.format(Locale.US, "Used: %.1f / %.1f GB", storageInfo.usedGb, storageInfo.totalGb),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = String.format(Locale.US, "≈ %.1f hrs recording time", storageInfo.estimatedHours),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Quick Studio Utilities
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick Audio Toggle Card
                QuickActionCard(
                    title = "Audio Track",
                    subtitle = config.audioOption.label,
                    icon = if (config.audioOption == AudioOption.MUTED) Icons.Default.MicOff else Icons.Default.Mic,
                    modifier = Modifier.weight(1f),
                    onClick = onToggleAudio
                )

                // Quick Countdown Toggle Card
                QuickActionCard(
                    title = "Countdown",
                    subtitle = if (config.countdownSeconds == 0) "No delay" else "${config.countdownSeconds} seconds",
                    icon = Icons.Default.Timer,
                    modifier = Modifier.weight(1f),
                    onClick = onToggleCountdown
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Test Demo Generator Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onGenerateSample() },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = StitchBlueContainer),
                border = androidx.compose.foundation.BorderStroke(1.dp, StitchBlue.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(StitchBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.VideoLibrary,
                                contentDescription = "Demo Sample",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Create 1080p HD Sample",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = StitchBlue
                            )
                            Text(
                                text = "Test high definition playback & local storage export",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = onGenerateSample,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = StitchBlue)
                    ) {
                        Text("Create", fontSize = 12.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // AdMob Fixed Size Banner
            AdMobBannerView(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Countdown Full-Screen Overlay
        AnimatedVisibility(
            visible = countdownValue != null,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable { onCancelCountdown() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Recording starts in",
                        fontSize = 20.sp,
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${countdownValue ?: 3}",
                        fontSize = 110.sp,
                        fontWeight = FontWeight.Black,
                        color = StitchRed
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onCancelCountdown,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun SpecBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StitchSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, StitchOutline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Normal
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = StitchSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, StitchOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(StitchSurfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = StitchBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatSeconds(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.US, "%02d:%02d", m, s)
}
