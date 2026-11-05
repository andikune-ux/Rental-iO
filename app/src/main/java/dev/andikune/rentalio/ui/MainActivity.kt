package dev.andikune.rentalio.ui

import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import dev.andikune.rentalio.R
import dev.andikune.rentalio.data.model.RentalUiState
import dev.andikune.rentalio.data.model.WhitelistApp
import dev.andikune.rentalio.service.KioskForegroundService
import dev.andikune.rentalio.ui.components.AdminDashboardDialog
import dev.andikune.rentalio.ui.components.GameOverOverlay
import dev.andikune.rentalio.ui.components.NeonTimerWidget
import dev.andikune.rentalio.ui.components.UpdateDialog
import dev.andikune.rentalio.ui.theme.GlassColors
import dev.andikune.rentalio.ui.theme.RentalIOGamingTheme
import dev.andikune.rentalio.ui.theme.liquidGlass
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.pow

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var gyroscopeHelper: GyroscopeHelper
    private lateinit var ivParallaxBackground: ImageView
    private lateinit var ivPanelRadialGlow: ImageView
    private lateinit var rvAppCarousel: RecyclerView
    private lateinit var carouselAdapter: AppCarouselAdapter
    private lateinit var snapHelper: LinearSnapHelper

    private var currentGlowColor: Int = GlassColors.AccentGlow.toArgb()
    private var glowAnimator: ValueAnimator? = null
    private var lastSnappedPosition: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Strictly lock to Landscape orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)

        ivParallaxBackground = findViewById(R.id.iv_parallax_background)
        ivPanelRadialGlow = findViewById(R.id.iv_panel_radial_glow)
        rvAppCarousel = findViewById(R.id.rv_app_carousel)
        val composeKioskUi = findViewById<ComposeView>(R.id.compose_kiosk_ui)

        // 1. Gyroscope 3D Parallax Background
        gyroscopeHelper = GyroscopeHelper(this, maxOffsetPx = 45f)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                gyroscopeHelper.offsetFlow.collect { offset ->
                    ivParallaxBackground.translationX = offset.translationX
                    ivParallaxBackground.translationY = offset.translationY
                }
            }
        }

        // 2. Setup Vertical C-Shape Carousel & SnapHelper
        setupCurvedCarousel()

        // 3. Start Persistent Kiosk Foreground Service
        KioskForegroundService.start(this)

        // 4. Setup Right Panel Compose UI Layer
        setupRightPanelCompose(composeKioskUi)

        // Initial default radial glow
        updateBackgroundRadialGlow(currentGlowColor)
    }

    private fun setupCurvedCarousel() {
        val layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        rvAppCarousel.layoutManager = layoutManager
        rvAppCarousel.setHasFixedSize(false)

        carouselAdapter = AppCarouselAdapter(this) { app, position ->
            viewModel.selectApp(app)
            smoothScrollToCenter(position)
        }
        rvAppCarousel.adapter = carouselAdapter

        snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(rvAppCarousel)

        // Ensure exactly 5 items fit within the RecyclerView viewport before scrolling
        rvAppCarousel.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
            val totalHeight = bottom - top
            if (totalHeight > 0) {
                val itemHeight = totalHeight / 5
                carouselAdapter.setItemHeight(itemHeight)
                // Padding ensures the first and last item can snap to the exact center
                val verticalPadding = itemHeight * 2
                if (rvAppCarousel.paddingTop != verticalPadding) {
                    rvAppCarousel.setPadding(0, verticalPadding, 0, verticalPadding)
                    rvAppCarousel.post {
                        applyCShapeCurve()
                        checkCenterSnappedItem()
                    }
                }
            }
        }

        // Apply C-Shape transformation dynamically during scrolling
        rvAppCarousel.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                applyCShapeCurve()
                checkCenterSnappedItem()
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    applyCShapeCurve()
                    checkCenterSnappedItem()
                }
            }
        })

        // Observe Whitelist Apps from ViewModel
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val apps = state.whitelistApps
                    if (apps.isNotEmpty()) {
                        val currentHeight = rvAppCarousel.height
                        val itemHeight = if (currentHeight > 0) currentHeight / 5 else 0
                        carouselAdapter.setItems(apps, itemHeight)
                        rvAppCarousel.post {
                            applyCShapeCurve()
                            checkCenterSnappedItem()
                        }
                    }
                }
            }
        }
    }

    /**
     * Mathematical C-Shape transformation:
     * - Center (Focus / Item 3): scale = 1.0f, alpha = 1.0f, translationX = 0f
     * - Positions 2 & 4 (Mid): scale = 0.75f, alpha = 0.8f, translationX = -45dp
     * - Positions 1 & 5 (Edges): scale = 0.50f, alpha = 0.5f, translationX = -85dp
     * Bends into a prominent 'C' shape facing right.
     */
    private fun applyCShapeCurve() {
        val rvHeight = rvAppCarousel.height
        if (rvHeight <= 0) return
        val centerY = rvHeight / 2f
        val maxDistance = rvHeight / 2f
        val maxShiftPx = 80f * resources.displayMetrics.density

        for (i in 0 until rvAppCarousel.childCount) {
            val child = rvAppCarousel.getChildAt(i) ?: continue
            val childCenterY = (child.top + child.bottom) / 2f
            val distance = abs(childCenterY - centerY)
            val ratio = (distance / maxDistance).coerceIn(0f, 1f)

            // Scale: 1.0 at center -> 0.75 at mid -> 0.50 at edges
            val scale = 1.0f - (0.50f * ratio)

            // Alpha: 1.0 at center -> 0.8 at mid -> 0.5 at edges
            val alpha = (1.0f - (0.50f * ratio)).coerceIn(0.45f, 1.0f)

            // TranslationX: 0 at center, negative shift to the left at edges
            val translationX = -maxShiftPx * ratio.pow(1.15f)

            child.pivotX = child.width * 0.85f // Pivot biased to right edge for curved arc
            child.pivotY = child.height / 2f
            child.scaleX = scale
            child.scaleY = scale
            child.alpha = alpha
            child.translationX = translationX
        }
    }

    private fun smoothScrollToCenter(position: Int) {
        val layoutManager = rvAppCarousel.layoutManager as? LinearLayoutManager ?: return
        val rvHeight = rvAppCarousel.height
        val itemHeight = if (rvHeight > 0) rvHeight / 5 else 0
        val offset = (rvHeight / 2) - (itemHeight / 2)
        layoutManager.scrollToPositionWithOffset(position, offset)
        rvAppCarousel.post {
            applyCShapeCurve()
            checkCenterSnappedItem()
        }
    }

    /**
     * Identifies the center-snapped item, triggers Palette color extraction, and animates glow.
     */
    private fun checkCenterSnappedItem() {
        val layoutManager = rvAppCarousel.layoutManager ?: return
        val snapView = snapHelper.findSnapView(layoutManager) ?: return
        val position = layoutManager.getPosition(snapView)

        if (position != RecyclerView.NO_POSITION && position != lastSnappedPosition) {
            lastSnappedPosition = position
            val apps = viewModel.uiState.value.whitelistApps
            if (position in apps.indices) {
                val app = apps[position]
                viewModel.selectApp(app)
                extractIconPalette(app.packageName)
            }
        }
    }

    /**
     * Extracts dominant/vibrant color from app icon using AndroidX Palette API.
     */
    private fun extractIconPalette(packageName: String) {
        val bitmap = getAppIconBitmap(this, packageName)
        Palette.from(bitmap).generate { palette ->
            val dominantColor = palette?.dominantSwatch?.rgb
                ?: palette?.vibrantSwatch?.rgb
                ?: palette?.lightVibrantSwatch?.rgb
                ?: GlassColors.AccentGlow.toArgb()

            animateGlowTransition(dominantColor)
        }
    }

    private fun animateGlowTransition(targetColor: Int) {
        glowAnimator?.cancel()
        val startColor = currentGlowColor
        glowAnimator = ValueAnimator.ofArgb(startColor, targetColor).apply {
            duration = 450
            addUpdateListener { animator ->
                val animatedColor = animator.animatedValue as Int
                currentGlowColor = animatedColor

                // a. Animate Layer Radial Gradient Glow on background behind glass
                updateBackgroundRadialGlow(animatedColor)

                // b. Animate Dynamic Glow & Border on active center item
                carouselAdapter.updateGlowColorOnly(animatedColor)
            }
            start()
        }
        carouselAdapter.updateFocusedItem(lastSnappedPosition, targetColor)
    }

    private fun updateBackgroundRadialGlow(color: Int) {
        val width = ivPanelRadialGlow.width.takeIf { it > 0 } ?: 340
        val gradient = GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = width * 0.9f
            setGradientCenter(0.5f, 0.5f)
            colors = intArrayOf(
                ColorUtils.setAlphaComponent(color, 115),
                ColorUtils.setAlphaComponent(color, 45),
                Color.TRANSPARENT
            )
        }
        ivPanelRadialGlow.setImageDrawable(gradient)
    }

    private fun getAppIconBitmap(context: Context, packageName: String): Bitmap {
        val drawable: Drawable = try {
            context.packageManager.getApplicationIcon(packageName)
        } catch (_: Exception) {
            ContextCompat.getDrawable(context, R.mipmap.ic_launcher)!!
        }

        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun setupRightPanelCompose(composeKioskUi: ComposeView) {
        composeKioskUi.setContent {
            RentalIOGamingTheme {
                val state by viewModel.uiState.collectAsState()

                BackHandler(enabled = true) {
                    if (state.showAdminDialog) {
                        viewModel.dismissAdminDialog()
                    } else if (state.showUpdateDialog) {
                        viewModel.dismissUpdateDialog()
                    }
                }

                RightPanelAndOverlays(
                    state = state,
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
        rvAppCarousel.post {
            applyCShapeCurve()
        }
    }

    override fun onPause() {
        super.onPause()
        gyroscopeHelper.stopListening()
    }
}

/**
 * Right Panel & System Overlays Composable:
 * Keeps the Action button, Timer widget, and System Overlays strictly unchanged in their
 * signature Frost Sapphire & Liquid Glass aesthetic.
 */
@Composable
fun RightPanelAndOverlays(
    state: RentalUiState,
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
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Header Bar (Liquid Glass from HOME.iO-TV)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(cornerRadius = 18.dp, isSelected = false)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
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

                // Mode Status Chip
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

            // 2. Center: App Preview & Launch Card (Liquid Glass Hero Panel)
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
                                text = activeApp?.label ?: "Pilih Game dari Coverflow",
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

                    // Sapphire Frost Action Button ("Buka Aplikasi" Tetap Sesuai Permintaan)
                    Button(
                        onClick = onLaunchApp,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GlassColors.AccentGlow,
                            contentColor = androidx.compose.ui.graphics.Color.White
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
                            text = "BUKA APLIKASI",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Floating Neon Timer Widget (Tetap Sesuai Permintaan)
            NeonTimerWidget(
                remainingSeconds = state.remainingSeconds,
                isExpired = state.isExpired,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Fullscreen Blocking Overlay when expired
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
