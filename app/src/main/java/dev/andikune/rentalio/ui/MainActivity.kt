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
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import dev.andikune.rentalio.service.KioskForegroundService
import dev.andikune.rentalio.ui.components.AdminDashboardDialog
import dev.andikune.rentalio.ui.components.GameOverOverlay
import dev.andikune.rentalio.ui.components.NeonTimerWidget
import dev.andikune.rentalio.ui.components.UpdateDialog
import dev.andikune.rentalio.ui.theme.GlassColors
import dev.andikune.rentalio.ui.theme.RentalIOGamingTheme
import dev.andikune.rentalio.ui.theme.liquidGlass
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    private lateinit var gyroscopeHelper: GyroscopeHelper
    private lateinit var ivParallaxBackground: ImageView
    private lateinit var ivPanelRadialGlow: ImageView
    private lateinit var flLeftCarouselContainer: FrameLayout
    private lateinit var btnToggleCarousel: FrameLayout
    private lateinit var rvAppCarousel: RecyclerView
    private lateinit var carouselAdapter: AppCarouselAdapter
    private lateinit var snapHelper: LinearSnapHelper

    private var currentGlowColor: Int = GlassColors.AccentGlow.toArgb()
    private var glowAnimator: ValueAnimator? = null
    private var lastSnappedPosition: Int = -1
    private var isCarouselOpen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Strictly lock to Landscape orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_main)

        ivParallaxBackground = findViewById(R.id.iv_parallax_background)
        ivPanelRadialGlow = findViewById(R.id.iv_panel_radial_glow)
        flLeftCarouselContainer = findViewById(R.id.fl_left_carousel_container)
        btnToggleCarousel = findViewById(R.id.btn_toggle_carousel)
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

        // 2. Setup Vertical C-Shape (C Terbalik) Carousel & Infinite Loop
        setupCurvedCarousel()

        // 3. Floating Toggle Button Logic (Kiri Tengah)
        setupToggleCarouselButton()

        // 4. Start Persistent Kiosk Foreground Service
        KioskForegroundService.start(this)

        // 5. Setup Right Panel Compose UI Layer
        setupRightPanelCompose(composeKioskUi)

        // Initial default radial glow
        updateBackgroundRadialGlow(currentGlowColor)
    }

    private fun setupToggleCarouselButton() {
        btnToggleCarousel.setOnClickListener {
            toggleCarouselVisibility()
        }
    }

    private fun toggleCarouselVisibility() {
        isCarouselOpen = !isCarouselOpen
        if (isCarouselOpen) {
            flLeftCarouselContainer.visibility = View.VISIBLE
            flLeftCarouselContainer.animate()
                .alpha(1f)
                .setDuration(260)
                .withEndAction {
                    applyCShapeCurve()
                    checkCenterSnappedItem()
                }
                .start()
            btnToggleCarousel.animate().scaleX(1.1f).scaleY(1.1f).setDuration(180).start()
        } else {
            flLeftCarouselContainer.animate()
                .alpha(0f)
                .setDuration(220)
                .withEndAction {
                    flLeftCarouselContainer.visibility = View.INVISIBLE
                }
                .start()
            btnToggleCarousel.animate().scaleX(1.0f).scaleY(1.0f).setDuration(180).start()
        }
    }

    private fun setupCurvedCarousel() {
        val layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        rvAppCarousel.layoutManager = layoutManager
        rvAppCarousel.setHasFixedSize(false)

        carouselAdapter = AppCarouselAdapter(this) { app, position ->
            if (position == lastSnappedPosition) {
                // Posisi tengah (fokus utama) -> langsung luncurkan aplikasi
                viewModel.launchSelectedApp(this@MainActivity)
            } else {
                // Posisi pinggir -> scroll ke tengah
                viewModel.selectApp(app)
                smoothScrollToCenter(position)
            }
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
                // Padding ensures any item can snap to the exact center
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

        // Apply C-Shape (C Terbalik) transformation dynamically during scrolling
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

                        // Infinite loop initial middle virtual position aligned to modulo 0
                        val middleVirtual = (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % apps.size)
                        rvAppCarousel.scrollToPosition(middleVirtual)

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
     * Mathematical C-Shape Transformation (C Terbalik / Kurva `)` ):
     * - Tombol pemicu ada di kiri luar layar.
     * - Aplikasi di posisi tengah (fokus utama): paling dekat ke tombol (translationX = 0f, scale = 1.0f, alpha = 1.0f).
     * - Aplikasi di atas & bawah: mundur ke KANAN (translationX POSITIF) dan mengecil (scale = 0.75f -> 0.50f, alpha = 0.8f -> 0.45f).
     * - PivotX = 0f (pivot di sisi kiri).
     */
    private fun applyCShapeCurve() {
        val rvHeight = rvAppCarousel.height
        if (rvHeight <= 0) return
        val centerY = rvHeight / 2f
        val maxDistance = rvHeight / 2f
        val maxShiftPx = 65f * resources.displayMetrics.density

        for (i in 0 until rvAppCarousel.childCount) {
            val child = rvAppCarousel.getChildAt(i) ?: continue
            val childCenterY = (child.top + child.bottom) / 2f
            val distance = abs(childCenterY - centerY)
            val ratio = (distance / maxDistance).coerceIn(0f, 1f)

            // Scale: 1.0 at center -> 0.75 at mid -> 0.50 at edges
            val scale = 1.0f - (0.50f * ratio)

            // Alpha: 1.0 at center -> 0.80 at mid -> 0.45 at edges
            val alpha = (1.0f - (0.55f * ratio)).coerceIn(0.40f, 1.0f)

            // TranslationX: 0f at center, POSITIVE shift to the right for items above & below
            val translationX = maxShiftPx * ratio.pow(1.15f)

            child.pivotX = 0f // Pivot di sisi kiri menghadap tombol pemicu
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
            val apps = carouselAdapter.getItems()
            if (apps.isNotEmpty()) {
                val app = apps[position % apps.size]
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
        val width = ivPanelRadialGlow.width.takeIf { it > 0 } ?: 320
        val gradient = GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = width * 0.95f
            setGradientCenter(0.5f, 0.5f)
            colors = intArrayOf(
                ColorUtils.setAlphaComponent(color, 110),
                ColorUtils.setAlphaComponent(color, 40),
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
                    } else if (isCarouselOpen) {
                        toggleCarouselVisibility()
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
 * RealTimeClockWidget:
 * Real-time clock at TOP-RIGHT, updating per second via LaunchedEffect + delay(1000)
 * Line 1: Hari, Tanggal ("Senin, 12 Okt")
 * Line 2: Jam ("14:30:00")
 * Locale Indonesia, floating freely without background box.
 */
@Composable
fun RealTimeClockWidget(modifier: Modifier = Modifier) {
    var currentTime by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = LocalDateTime.now()
            delay(1000)
        }
    }

    val locale = remember { Locale.forLanguageTag("id-ID") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d MMM", locale) }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm:ss", locale) }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
    ) {
        Text(
            text = currentTime.format(dateFormatter),
            color = GlassColors.TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = currentTime.format(timeFormatter),
            color = GlassColors.AccentGlow,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
    }
}

/**
 * Clean Right Panel & System Overlays Composable:
 * - Clean Overlay: No solid black boxes
 * - Header: "RENTAL.IO" and Kiosk Status Chip separated with Spacer(weight(1f))
 * - RealTimeClockWidget next to Settings/Admin buttons
 * - Center Game Preview: Floating freely without card borders, tap to launch
 * - NeonTimerWidget: Vertical digital clock
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
            // 1. Top Header: Separated Brand, Status Chip, RealTimeClockWidget & Actions (No enclosing solid box)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                // Brand Crest
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
                        text = "Clean Glass Edition",
                        color = GlassColors.TextSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Kiosk Status Chip (Outline only)
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
                            text = if (state.isDeviceOwnerActive) "DEVICE OWNER AKTIF" else "KIOSK READY",
                            color = if (state.isDeviceOwnerActive) GlassColors.AccentGreen else GlassColors.AccentAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // RealTimeClockWidget: Hari, Tanggal di baris atas & Jam di baris bawah
                RealTimeClockWidget()

                Spacer(modifier = Modifier.width(14.dp))

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

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Center: Game Preview (Floating Freely Without Box / Border)
            val activeApp = state.selectedApp
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onLaunchApp
                    )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Floating Glowing Icon
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .liquidGlass(cornerRadius = 36.dp, isSelected = true)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircleFilled,
                            contentDescription = activeApp?.label ?: "Game",
                            tint = GlassColors.AccentGlow,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = activeApp?.label ?: "Pilih Game dari Menu",
                        color = GlassColors.TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = activeApp?.category ?: "Ketuk aplikasi di tengah untuk membuka",
                        color = GlassColors.AccentGlow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 3. Digital Clock Vertical Clean Timer Widget
            NeonTimerWidget(
                remainingSeconds = state.remainingSeconds,
                isExpired = state.isExpired,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Fullscreen Blocking Overlay when rental expired
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
