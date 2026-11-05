package dev.andikune.rentalio.ui

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.andikune.rentalio.R
import dev.andikune.rentalio.data.model.RentalUiState
import dev.andikune.rentalio.service.KioskForegroundService
import dev.andikune.rentalio.ui.components.AdminDashboardDialog
import dev.andikune.rentalio.ui.components.AppCarouselView
import dev.andikune.rentalio.ui.components.GameOverOverlay
import dev.andikune.rentalio.ui.components.NeonTimerWidget
import dev.andikune.rentalio.ui.components.UpdateDialog
import dev.andikune.rentalio.ui.theme.GlassColors
import dev.andikune.rentalio.ui.theme.liquidGlass
import dev.andikune.rentalio.ui.theme.RentalIOGamingTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var gyroscopeHelper: GyroscopeHelper
    private lateinit var ivParallaxBackground: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Strictly lock to Landscape orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        // Keep screen awake for gaming lounge use
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Set layout with 3D Parallax ImageView and ComposeView
        setContentView(R.layout.activity_main)

        ivParallaxBackground = findViewById(R.id.iv_parallax_background)
        val composeKioskUi = findViewById<ComposeView>(R.id.compose_kiosk_ui)

        // Initialize Gyroscope Parallax Engine
        gyroscopeHelper = GyroscopeHelper(this, maxOffsetPx = 45f)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                gyroscopeHelper.offsetFlow.collect { offset ->
                    ivParallaxBackground.translationX = offset.translationX
                    ivParallaxBackground.translationY = offset.translationY
                }
            }
        }

        // Start Persistent Kiosk Foreground Service
        KioskForegroundService.start(this)

        // Configure Compose UI Layer
        composeKioskUi.setContent {
            RentalIOGamingTheme {
                val state by viewModel.uiState.collectAsState()

                // Intercept back button to prevent exiting Kiosk Mode
                BackHandler(enabled = true) {
                    if (state.showAdminDialog) {
                        viewModel.dismissAdminDialog()
                    } else if (state.showUpdateDialog) {
                        viewModel.dismissUpdateDialog()
                    }
                    // Otherwise do nothing: stay locked inside launcher
                }

                KioskMainScreen(
                    state = state,
                    onSelectApp = { viewModel.selectApp(it) },
                    onLaunchApp = { viewModel.launchSelectedApp(this@MainActivity) },
                    onOpenAdmin = { viewModel.openAdminDialog() },
                    onCheckUpdate = { viewModel.checkForUpdates(silent = false) },
                    onAddTime = { viewModel.addRentalTime(it) },
                    onSetTime = { viewModel.setRentalTime(it) },
                    onToggleLockTask = { viewModel.toggleLockTask(it, this@MainActivity) },
                    onChangePin = { viewModel.updateAdminPin(it) },
                    onDismissAdmin = { viewModel.dismissAdminDialog() },
                    onUnlockWithPin = { viewModel.unlockWithPin(it) },
                    onUpdateNow = { viewModel.downloadAndInstallUpdate() },
                    onLaterUpdate = { viewModel.dismissUpdateDialog() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        gyroscopeHelper.startListening()
        viewModel.checkDeviceOwnerStatus()
    }

    override fun onPause() {
        super.onPause()
        gyroscopeHelper.stopListening()
    }
}

@Composable
fun KioskMainScreen(
    state: RentalUiState,
    onSelectApp: (dev.andikune.rentalio.data.model.WhitelistApp) -> Unit,
    onLaunchApp: () -> Unit,
    onOpenAdmin: () -> Unit,
    onCheckUpdate: () -> Unit,
    onAddTime: (Long) -> Unit,
    onSetTime: (Long) -> Unit,
    onToggleLockTask: (Boolean) -> Unit,
    onChangePin: (String) -> Unit,
    onDismissAdmin: () -> Unit,
    onUnlockWithPin: (String) -> Unit,
    onUpdateNow: () -> Unit,
    onLaterUpdate: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // Main Landscape Layout: Left App Carousel, Right Action Panel & Timer
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // [PANEL KIRI - APP CAROUSEL (Vertical Scroll Ring)]
            AppCarouselView(
                apps = state.whitelistApps,
                selectedApp = state.selectedApp,
                onSelectApp = onSelectApp,
                modifier = Modifier
                    .width(320.dp)
                    .fillMaxHeight()
            )

            // [PANEL KANAN - ACTION & FLOATING TIMER]
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Status & Header Bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(cornerRadius = 18.dp, isSelected = false)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    // Logo Crest (Liquid Glass Focus style from HOME.iO-TV)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .liquidGlass(cornerRadius = 12.dp, isSelected = true)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = "Rental.iO",
                            tint = GlassColors.AccentGlow,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "RENTAL.IO",
                            color = GlassColors.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.5.sp
                        )
                        Text(
                            text = "Liquid Glass Edition",
                            color = GlassColors.TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Mode Status Chip (Emerald Accent from HOME.iO-TV)
                    Box(
                        modifier = Modifier
                            .liquidGlass(cornerRadius = 10.dp, isSelected = false)
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (state.isDeviceOwnerActive) Icons.Default.Security else Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = if (state.isDeviceOwnerActive) GlassColors.AccentGreen else GlassColors.AccentAmber,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.isDeviceOwnerActive) "DEVICE OWNER AKTIF" else "KIOSK OFFLINE READY",
                                color = if (state.isDeviceOwnerActive) GlassColors.AccentGreen else GlassColors.AccentAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // In-App Update Button
                    IconButton(
                        onClick = onCheckUpdate,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Cek Update",
                            tint = GlassColors.AccentGlow,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Admin Security Settings Icon
                    IconButton(
                        onClick = onOpenAdmin,
                        modifier = Modifier
                            .testTag("admin_settings_button")
                            .size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = "Admin Panel",
                            tint = GlassColors.TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Center: App Preview & Launch Card (Liquid Glass Hero Panel)
                val activeApp = state.selectedApp
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .liquidGlass(cornerRadius = 24.dp, isSelected = false)
                        .padding(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(64.dp)
                                    .liquidGlass(cornerRadius = 18.dp, isSelected = true)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Gamepad,
                                    contentDescription = activeApp?.label ?: "Game",
                                    tint = GlassColors.AccentGlow,
                                    modifier = Modifier.size(34.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(18.dp))

                            Column {
                                Text(
                                    text = activeApp?.label ?: "Pilih Game dari Whitelist",
                                    color = GlassColors.TextPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = activeApp?.packageName ?: "dev.andikune.rentalio",
                                    color = GlassColors.AccentGlow,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }

                        // Sapphire Frost Glow Launch Button
                        Button(
                            onClick = onLaunchApp,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GlassColors.AccentGlow,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .testTag("btn_launch_app")
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "BUKA APLIKASI SEKARANG",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Floating Neon Timer Widget
                NeonTimerWidget(
                    remainingSeconds = state.remainingSeconds,
                    isExpired = state.isExpired,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Fullscreen Blocking Overlay when expired (Game Over)
        AnimatedVisibility(
            visible = state.isExpired,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            GameOverOverlay(
                onUnlockWithPin = onUnlockWithPin,
                errorMessage = state.errorMessage
            )
        }

        // Admin Dashboard Dialog
        if (state.showAdminDialog) {
            AdminDashboardDialog(
                remainingSeconds = state.remainingSeconds,
                isDeviceOwner = state.isDeviceOwnerActive,
                isLockTaskActive = state.isLockTaskModeActive,
                onAddTime = onAddTime,
                onSetTime = onSetTime,
                onToggleLockTask = onToggleLockTask,
                onChangePin = onChangePin,
                onCheckUpdate = onCheckUpdate,
                onDismiss = onDismissAdmin
            )
        }

        // In-App Update Dialog
        if (state.showUpdateDialog && state.updateRelease != null) {
            UpdateDialog(
                release = state.updateRelease,
                downloadProgress = state.downloadProgress,
                onUpdateClick = onUpdateNow,
                onLaterClick = onLaterUpdate,
                onSkipClick = onLaterUpdate
            )
        }
    }
}
