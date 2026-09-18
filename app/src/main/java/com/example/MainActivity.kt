package com.example

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ads.AdMobManager
import com.example.recorder.AudioOption
import com.example.ui.components.VideoPlayerDialog
import com.example.ui.screens.RecordingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.StitchBlue
import com.example.ui.theme.StitchBlueContainer
import com.example.ui.theme.StitchOutline
import com.example.ui.theme.StitchSurface
import com.example.ui.theme.StitchTextPrimary
import com.example.ui.theme.StitchTextSecondary
import com.example.util.ExportManager
import com.example.viewmodel.ScreenRecorderViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: ScreenRecorderViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Google Mobile Ads SDK with provided AdMob IDs
        AdMobManager.initialize(this)

        setContent {
            MyApplicationTheme {
                ScreenRecorderApp(
                    activity = this,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun ScreenRecorderApp(
    activity: Activity,
    viewModel: ScreenRecorderViewModel
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val engineState by viewModel.engineState.collectAsStateWithLifecycle()
    val config by viewModel.config.collectAsStateWithLifecycle()
    val recordings by viewModel.recordings.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val storageInfo by viewModel.storageInfo.collectAsStateWithLifecycle()
    val selectedVideoForPlayer by viewModel.selectedVideoForPlayer.collectAsStateWithLifecycle()
    val exportStatusMessage by viewModel.exportStatusMessage.collectAsStateWithLifecycle()
    val countdownValue by viewModel.countdownValue.collectAsStateWithLifecycle()

    // Activity Result Launcher for MediaProjection
    val mediaProjectionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val densityDpi = context.resources.displayMetrics.densityDpi
            viewModel.initiateStartRecording(result.resultCode, result.data!!, densityDpi)
        } else {
            Toast.makeText(context, "Screen record permission was not granted", Toast.LENGTH_SHORT).show()
        }
    }

    // Permission Launcher for Audio & Notifications
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        // After permissions check, launch media projection
        val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
        if (projectionManager != null) {
            mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
        } else {
            Toast.makeText(context, "Media projection not supported on this device", Toast.LENGTH_SHORT).show()
        }
    }

    fun requestStartRecording() {
        val permissionsToRequest = mutableListOf<String>()
        if (config.audioOption == AudioOption.MICROPHONE) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as? MediaProjectionManager
            if (projectionManager != null) {
                mediaProjectionLauncher.launch(projectionManager.createScreenCaptureIntent())
            } else {
                Toast.makeText(context, "Media projection not supported", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(exportStatusMessage) {
        exportStatusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    LaunchedEffect(engineState.error) {
        engineState.error?.let { err ->
            snackbarHostState.showSnackbar(err)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = StitchSurface,
                tonalElevation = 4.dp,
                modifier = Modifier
                    .background(StitchSurface)
                    .navigationBarsPadding(),
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { viewModel.setSelectedTab(0) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.Videocam else Icons.Outlined.Videocam,
                            contentDescription = "Record Studio"
                        )
                    },
                    label = { Text("Studio", fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StitchBlue,
                        selectedTextColor = StitchBlue,
                        indicatorColor = StitchBlueContainer,
                        unselectedIconColor = StitchTextSecondary,
                        unselectedTextColor = StitchTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_item_studio")
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { viewModel.setSelectedTab(1) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary,
                            contentDescription = "Recordings"
                        )
                    },
                    label = { Text("Videos", fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StitchBlue,
                        selectedTextColor = StitchBlue,
                        indicatorColor = StitchBlueContainer,
                        unselectedIconColor = StitchTextSecondary,
                        unselectedTextColor = StitchTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_item_videos")
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { viewModel.setSelectedTab(2) },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("Settings", fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StitchBlue,
                        selectedTextColor = StitchBlue,
                        indicatorColor = StitchBlueContainer,
                        unselectedIconColor = StitchTextSecondary,
                        unselectedTextColor = StitchTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> StudioScreen(
                    engineState = engineState,
                    config = config,
                    storageInfo = storageInfo,
                    countdownValue = countdownValue,
                    onStartRequested = { requestStartRecording() },
                    onStopRequested = {
                        viewModel.stopRecording()
                        AdMobManager.showInterstitialAd(activity) {
                            viewModel.setSelectedTab(1)
                        }
                    },
                    onPauseRequested = { viewModel.pauseRecording() },
                    onResumeRequested = { viewModel.resumeRecording() },
                    onCancelCountdown = { viewModel.cancelCountdown() },
                    onGenerateSample = {
                        viewModel.generateSampleRecording()
                        AdMobManager.showInterstitialAd(activity) {
                            viewModel.setSelectedTab(1)
                        }
                    },
                    onNavigateToSettings = { viewModel.setSelectedTab(2) },
                    onNavigateToRecordings = { viewModel.setSelectedTab(1) },
                    onToggleAudio = {
                        val nextAudio = when (config.audioOption) {
                            AudioOption.MICROPHONE -> AudioOption.INTERNAL
                            AudioOption.INTERNAL -> AudioOption.MUTED
                            AudioOption.MUTED -> AudioOption.MICROPHONE
                        }
                        viewModel.updateConfig(config.copy(audioOption = nextAudio))
                    },
                    onToggleCountdown = {
                        val nextCd = when (config.countdownSeconds) {
                            3 -> 5
                            5 -> 0
                            else -> 3
                        }
                        viewModel.updateConfig(config.copy(countdownSeconds = nextCd))
                    }
                )

                1 -> RecordingsScreen(
                    recordings = recordings,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onPlayRecording = { viewModel.openPlayer(it) },
                    onExportRecording = { viewModel.exportToLocalStorage(it) },
                    onDeleteRecording = { viewModel.deleteRecording(it) },
                    onRenameRecording = { rec, newTitle -> viewModel.renameRecording(rec, newTitle) },
                    onNavigateToStudio = { viewModel.setSelectedTab(0) },
                    onGenerateSample = {
                        viewModel.generateSampleRecording()
                        AdMobManager.showInterstitialAd(activity) {}
                    }
                )

                2 -> SettingsScreen(
                    config = config,
                    onConfigChange = { viewModel.updateConfig(it) },
                    onWatchRewardedAd = {
                        AdMobManager.showRewardedAd(
                            activity = activity,
                            onUserEarnedReward = { _, _ ->
                                viewModel.updateConfig(
                                    config.copy(
                                        fps = 60,
                                        bitrateMbps = 16
                                    )
                                )
                                Toast.makeText(
                                    context,
                                    "🎉 Pro Unlocked: Ultra 60 FPS & 16 Mbps bitrate active!",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            onAdClosed = {}
                        )
                    }
                )
            }
        }
    }

    // In-App Video Player Dialog
    if (selectedVideoForPlayer != null) {
        val recording = selectedVideoForPlayer!!
        VideoPlayerDialog(
            recording = recording,
            onDismiss = { viewModel.closePlayer() },
            onExportToLocalStorage = {
                viewModel.exportToLocalStorage(recording)
            },
            onShare = {
                ExportManager.shareVideo(context, recording)
            }
        )
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}

