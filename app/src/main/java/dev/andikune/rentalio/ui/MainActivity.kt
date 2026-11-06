package dev.andikune.rentalio.ui

import android.animation.ValueAnimator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.ui.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import dev.andikune.rentalio.ui.components.CrashLogDialog
import dev.andikune.rentalio.ui.components.GameOverOverlay
import dev.andikune.rentalio.ui.components.NeonTimerWidget
import dev.andikune.rentalio.ui.components.UpdateDialog
import dev.andikune.rentalio.ui.theme.GamingColors
import dev.andikune.rentalio.ui.theme.GlassColors
import dev.andikune.rentalio.ui.theme.RentalIOGamingTheme
import dev.andikune.rentalio.ui.theme.liquidGlass
import dev.andikune.rentalio.utils.CrashHandler
import dev.andikune.rentalio.utils.SecurityHelper
import dev.andikune.rentalio.utils.SystemOverlayHelper
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
    private var hasInitializedCarousel: Boolean = false
    private var hasNotifiedNinetyPercent: Boolean = false

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100

                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 300)
                val tempCelsius = tempRaw / 10.0f

                val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                val netStatus = if (cm != null) {
                    val activeNetwork = cm.activeNetwork
                    val caps = cm.getNetworkCapabilities(activeNetwork)
                    when {
                        caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WIFI"
                        caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "CELLULAR"
                        else -> "NONE"
                    }
                } else "WIFI"

                viewModel.updateBatteryTelemetry(batteryPct, isCharging, tempCelsius, netStatus)

                if (isCharging && batteryPct >= 90 && !hasNotifiedNinetyPercent) {
                    hasNotifiedNinetyPercent = true
                    Toast.makeText(
                        this@MainActivity,
                        "⚠️ Baterai sudah mencapai 90%, harap cabut charger untuk menjaga kesehatan baterai.",
                        Toast.LENGTH_LONG
                    ).show()
                } else if (!isCharging) {
                    hasNotifiedNinetyPercent = false
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        applyFullscreenImmersive()
        setContentView(R.layout.activity_main)

        ivParallaxBackground = findViewById(R.id.iv_parallax_background)
        ivPanelRadialGlow = findViewById(R.id.iv_panel_radial_glow)
        flLeftCarouselContainer = findViewById(R.id.fl_left_carousel_container)
        btnToggleCarousel = findViewById(R.id.btn_toggle_carousel)
        rvAppCarousel = findViewById(R.id.rv_app_carousel)
        val composeKioskUi = findViewById<ComposeView>(R.id.compose_kiosk_ui)

        SystemOverlayHelper.requestOverlayPermission(this)

        gyroscopeHelper = GyroscopeHelper(this, maxOffsetPx = 45f)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                gyroscopeHelper.offsetFlow.collect { offset ->
                    ivParallaxBackground.translationX = offset.translationX
                    ivParallaxBackground.translationY = offset.translationY
                }
            }
        }

        setupCurvedCarousel()
        setupToggleCarouselButton()
        KioskForegroundService.start(this)
        setupRightPanelCompose(composeKioskUi)

        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        ivPanelRadialGlow.alpha = 0f
        ivPanelRadialGlow.visibility = View.INVISIBLE
        updateBackgroundRadialGlow(currentGlowColor)
    }

    private fun applyFullscreenImmersive() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyFullscreenImmersive()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        val state = viewModel.uiState.value
        if (state.showAdminDialog) {
            return super.onKeyDown(keyCode, event)
        }

        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP,
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_MUTE -> return true
            KeyEvent.KEYCODE_POWER -> return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        val state = viewModel.uiState.value
        if (!state.showAdminDialog) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP,
                KeyEvent.KEYCODE_VOLUME_DOWN,
                KeyEvent.KEYCODE_VOLUME_MUTE -> return true
            }
        }
        return super.onKeyUp(keyCode, event)
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
            ivPanelRadialGlow.visibility = View.VISIBLE
            flLeftCarouselContainer.animate().alpha(1f).setDuration(260).withEndAction {
                applyCShapeCurve()
                checkCenterSnappedItem()
            }.start()
            ivPanelRadialGlow.animate().alpha(0.80f).setDuration(260).start()
            btnToggleCarousel.animate().scaleX(1.1f).scaleY(1.1f).setDuration(180).start()
        } else {
            flLeftCarouselContainer.animate().alpha(0f).setDuration(220).withEndAction {
                flLeftCarouselContainer.visibility = View.INVISIBLE
            }.start()
            ivPanelRadialGlow.animate().alpha(0f).setDuration(220).withEndAction {
                ivPanelRadialGlow.visibility = View.INVISIBLE
            }.start()
            btnToggleCarousel.animate().scaleX(1.0f).scaleY(1.0f).setDuration(180).start()
        }
    }

    private fun setupCurvedCarousel() {
        val layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        rvAppCarousel.layoutManager = layoutManager
        rvAppCarousel.setHasFixedSize(false)

        carouselAdapter = AppCarouselAdapter(this) { app, position ->
            val currentCenterView = snapHelper.findSnapView(layoutManager)
            val currentCenterPosition = if (currentCenterView != null) layoutManager.getPosition(currentCenterView) else -1

            if (position == currentCenterPosition || position == lastSnappedPosition) {
                viewModel.selectApp(app)
                viewModel.launchSelectedApp(this@MainActivity)
            } else {
                smoothScrollToCenter(position)
            }
        }
        rvAppCarousel.adapter = carouselAdapter

        snapHelper = LinearSnapHelper()
        snapHelper.attachToRecyclerView(rvAppCarousel)

        rvAppCarousel.addOnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
            val totalHeight = bottom - top
            if (totalHeight > 0) {
                val itemHeight = totalHeight / 5
                carouselAdapter.setItemHeight(itemHeight)
                applyCShapeCurve()

                if (!hasInitializedCarousel && carouselAdapter.itemCount > 0) {
                    hasInitializedCarousel = true
                    val middleVirtual = carouselAdapter.getInitialCenterPosition()
                    rvAppCarousel.scrollToPosition(middleVirtual)
                    rvAppCarousel.post {
                        applyCShapeCurve()
                        checkCenterSnappedItem()
                    }
                }
            }
        }

        rvAppCarousel.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                applyCShapeCurve()
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                applyCShapeCurve()
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    checkCenterSnappedItem()
                }
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    val apps = state.whitelistApps
                    if (apps.isNotEmpty()) {
                        val currentItems = carouselAdapter.getItems()
                        if (currentItems != apps) {
                            val currentHeight = rvAppCarousel.height
                            val itemHeight = if (currentHeight > 0) currentHeight / 5 else 0
                            carouselAdapter.setItems(apps, itemHeight)

                            if (!hasInitializedCarousel && currentHeight > 0) {
                                hasInitializedCarousel = true
                                val middleVirtual = carouselAdapter.getInitialCenterPosition()
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
        }
    }

    private fun applyCShapeCurve() {
        val rvHeight = rvAppCarousel.height
        if (rvHeight <= 0) return
        val centerY = rvHeight / 2f
        val maxDistance = rvHeight / 2f
        val maxShiftPx = 150f * resources.displayMetrics.density

        for (i in 0 until rvAppCarousel.childCount) {
            val child = rvAppCarousel.getChildAt(i) ?: continue
            val childCenterY = (child.top + child.bottom) / 2f
            val distance = abs(childCenterY - centerY)
            val ratio = (distance / maxDistance).coerceIn(0f, 1f)

            val scale = 1.0f - (0.25f * ratio)
            val alpha = (1.0f - (0.50f * ratio)).coerceIn(0.45f, 1.0f)
            val translationX = maxShiftPx * ratio.pow(1.2f)

            child.pivotX = 0f
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
                updateBackgroundRadialGlow(animatedColor)
                carouselAdapter.updateGlowColorDirectly(rvAppCarousel, animatedColor)
            }
            start()
        }
        carouselAdapter.updateFocusedItem(lastSnappedPosition, targetColor)
    }

    private fun updateBackgroundRadialGlow(color: Int) {
        val width = ivPanelRadialGlow.width.takeIf { it > 0 } ?: 320
        val gradient = GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = width * 1.4f
            setGradientCenter(0.45f, 0.5f)
            colors = intArrayOf(
                ColorUtils.setAlphaComponent(color, 85),
                ColorUtils.setAlphaComponent(color, 45),
                ColorUtils.setAlphaComponent(color, 15),
                android.graphics.Color.TRANSPARENT
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
        val previousCrashLog = CrashHandler.getAndClearCrashLog(this)

        composeKioskUi.setContent {
            RentalIOGamingTheme {
                val state by viewModel.uiState.collectAsState()
                val allApps by viewModel.allDeviceApps.collectAsState()

                // State lokal untuk mengontrol visibilitas dialog crash
                var showCrashDialog by remember { mutableStateOf(previousCrashLog != null) }

                if (showCrashDialog && previousCrashLog != null) {
                    CrashLogDialog(
                        logText = previousCrashLog,
                        onDismiss = { showCrashDialog = false }
                    )
                }

                BackHandler(enabled = true) {
                    if (state.showAdminDialog) {
                        viewModel.dismissAdminDialog()
                    } else if (state.showUpdateDialog) {
                        viewModel.dismissUpdateDialog()
                    } else if (state.showUnlimitedSummaryDialog) {
                        viewModel.dismissUnlimitedSummary()
                    } else if (isCarouselOpen) {
                        toggleCarouselVisibility()
                    }
                }

                RightPanelAndOverlays(
                    state = state,
                    allApps = allApps,
                    currentSettings = viewModel.preferences.getSettings(),
                    onLaunchApp = { viewModel.launchSelectedApp(this@MainActivity) },
                    onOpenAdmin = { viewModel.openAdminDialog() },
                    onCheckUpdate = { viewModel.checkForUpdates(silent = false) },
                    onAddTime = { viewModel.addRentalTime(it) },
                    onSetTime = { viewModel.setRentalTime(it) },
                    onSaveDuration = { h, m, s, b, unlim ->
                        viewModel.savePackageDuration(h, m, s, b, unlim)
                    },
                    onSaveIdentity = { rName, pLabel, b1, b2, b3, auto ->
                        viewModel.saveIdentityAndBanners(rName, pLabel, b1, b2, b3, auto)
                    },
                    onSaveWhitelist = { viewModel.saveWhitelist(it) },
                    onVerifyPin = { viewModel.verifyAdminPin(it) },
                    onToggleLockTask = { viewModel.toggleLockTask(it, this@MainActivity) },
                    onChangePin = { viewModel.updateAdminPin(it) },
                    onDismissAdmin = { viewModel.dismissAdminDialog() },
                    onUnlockWithPin = { viewModel.unlockWithPin(it) },
                    onUpdateNow = { viewModel.downloadAndInstallUpdate() },
                    onLaterUpdate = { viewModel.dismissUpdateDialog() },
                    onFinishUnlimited = { viewModel.finishUnlimitedSession() },
                    onDismissSummary = { viewModel.dismissUnlimitedSummary() }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        applyFullscreenImmersive()
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

    override fun onDestroy() {
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
        super.onDestroy()
    }
}

@Composable
fun CustomStatusBar(
    batteryLevel: Int,
    isCharging: Boolean,
    networkStatus: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0x33000000))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        when (networkStatus) {
            "WIFI" -> Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = "Wi-Fi",
                tint = GlassColors.AccentCyan,
                modifier = Modifier.size(16.dp)
            )
            "CELLULAR" -> Icon(
                imageVector = Icons.Default.SignalCellularAlt,
                contentDescription = "Seluler",
                tint = GlassColors.AccentGreen,
                modifier = Modifier.size(16.dp)
            )
            else -> Icon(
                imageVector = Icons.Default.WifiOff,
                contentDescription = "Offline",
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            val batteryColor = when {
                isCharging -> GlassColors.AccentCyan
                batteryLevel <= 15 -> GlassColors.AccentRed
                batteryLevel <= 30 -> GlassColors.AccentAmber
                else -> GlassColors.AccentGreen
            }

            Icon(
                imageVector = when {
                    isCharging -> Icons.Default.BatteryChargingFull
                    batteryLevel <= 15 -> Icons.Default.BatteryAlert
                    else -> Icons.Default.BatteryFull
                },
                contentDescription = "Baterai",
                tint = batteryColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = "$batteryLevel%",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

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
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false
        )
        Text(
            text = currentTime.format(timeFormatter),
            color = GlassColors.AccentGlow,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun RightPanelAndOverlays(
    state: RentalUiState,
    allApps: List<dev.andikune.rentalio.data.model.WhitelistApp>,
    currentSettings: dev.andikune.rentalio.data.local.RentalPreferences.SettingsData,
    onLaunchApp: () -> Unit,
    onOpenAdmin: () -> Unit,
    onCheckUpdate: () -> Unit,
    onAddTime: (Long) -> Unit,
    onSetTime: (Long) -> Unit,
    onSaveDuration: (hours: Int, minutes: Int, seconds: Int, bonusMinutes: Int, isUnlimited: Boolean) -> Unit,
    onSaveIdentity: (rentalName: String, phoneLabel: String, banner1: String, banner2: String, banner3: String, autoStart: Boolean) -> Unit,
    onSaveWhitelist: (Set<String>) -> Unit,
    onVerifyPin: (String) -> Boolean,
    onToggleLockTask: (Boolean) -> Unit,
    onChangePin: (String) -> Unit,
    onDismissAdmin: () -> Unit,
    onUnlockWithPin: (String) -> Unit,
    onUpdateNow: () -> Unit,
    onLaterUpdate: () -> Unit,
    onFinishUnlimited: () -> Unit,
    onDismissSummary: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .liquidGlass(cornerRadius = 10.dp, isSelected = true)
                ) {
                    Icon(
                        imageVector = Icons.Default.Gamepad,
                        contentDescription = "Rental",
                        tint = GlassColors.AccentGlow,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = state.rentalName.ifBlank { "RENTAL.IO" },
                        color = GlassColors.TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.2.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = state.phoneLabel.ifBlank { "HP NO. 1" },
                        color = GlassColors.AccentGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                Box(
                    modifier = Modifier
                        .liquidGlass(cornerRadius = 8.dp, isSelected = false)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (state.isDeviceOwnerActive) Icons.Default.Security else Icons.Default.CloudOff,
                            contentDescription = null,
                            tint = if (state.isDeviceOwnerActive) GlassColors.AccentGreen else GlassColors.AccentAmber,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (state.isDeviceOwnerActive) "KIOSK ON" else "STANDBY",
                            color = if (state.isDeviceOwnerActive) GlassColors.AccentGreen else GlassColors.AccentAmber,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                CustomStatusBar(
                    batteryLevel = state.batteryLevel,
                    isCharging = state.isCharging,
                    networkStatus = state.networkStatus
                )

                Spacer(modifier = Modifier.width(10.dp))

                RealTimeClockWidget()

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onCheckUpdate,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Cek Update",
                        tint = GlassColors.AccentGlow,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = onOpenAdmin,
                    modifier = Modifier
                        .testTag("admin_settings_button")
                        .size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin Panel",
                        tint = GlassColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            if (state.isBatteryLow) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xD9B91C1C))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BatteryAlert, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PERINGATAN: Baterai tinggal ${state.batteryLevel}%! Harap hubungi admin rental untuk mengisi daya.",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            val activeApp = state.selectedApp
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(16.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onLaunchApp
                    )
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(58.dp)) {
                        Icon(
                            imageVector = Icons.Default.Gamepad,
                            contentDescription = activeApp?.label ?: "Game",
                            tint = GlassColors.AccentGlow,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = activeApp?.label ?: "Pilih Game",
                        color = GlassColors.TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Sentuh untuk meluncurkan game",
                        color = GlassColors.TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                NeonTimerWidget(
                    remainingSeconds = state.remainingSeconds,
                    isExpired = state.isExpired,
                    isUnlimitedMode = state.isUnlimitedMode,
                    unlimitedElapsedSeconds = state.unlimitedElapsedSeconds,
                    packageHours = state.packageHours,
                    packageMinutes = state.packageMinutes,
                    bonusMinutes = state.bonusMinutes
                )

                if (state.isUnlimitedMode) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Button(
                        onClick = onFinishUnlimited,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xCC0E1726),
                            contentColor = GamingColors.NeonCyan
                        ),
                        border = BorderStroke(1.dp, GamingColors.NeonCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("SELESAIKAN SEWA UNLIMITED", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (state.isOverheated) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xF27F1D1D))
                    .padding(24.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Overheat",
                        tint = Color.Yellow,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "PERINGATAN SUHU TINGGI (>45°C)!",
                        color = Color.Yellow,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "CABUT CHARGER SEKARANG DEMI KEAMANAN PERANGKAT!",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Suhu Baterai: ${state.batteryTemperature}°C | Status Pengisian: ${if (state.isCharging) "Mengecas" else "Tidak Mengecas"}",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = state.isExpired && !state.isUnlimitedMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            GameOverOverlay(
                phoneLabel = state.phoneLabel,
                bannerTexts = state.bannerTexts,
                onUnlockWithPin = onUnlockWithPin,
                errorMessage = state.errorMessage
            )
        }

        if (state.showUnlimitedSummaryDialog) {
            Dialog(onDismissRequest = onDismissSummary) {
                Box(
                    modifier = Modifier
                        .width(420.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0F172A))
                        .border(BorderStroke(2.dp, GamingColors.NeonCyan), RoundedCornerShape(18.dp))
                        .padding(20.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = GamingColors.NeonCyan,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "RINGKASAN SEWA UNLIMITED",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E293B))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Perangkat:", color = GamingColors.TextSecondary, fontSize = 12.sp)
                                    Text(state.phoneLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Total Waktu:", color = GamingColors.TextSecondary, fontSize = 12.sp)
                                    Text(SecurityHelper.formatDuration(state.unlimitedSummarySeconds), color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Tarif / Menit:", color = GamingColors.TextSecondary, fontSize = 12.sp)
                                    Text("Rp 100", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("TOTAL TAGIHAN:", color = GamingColors.NeonCyan, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold)
                                    Text(SecurityHelper.formatRupiah(state.unlimitedSummaryCost), color = GamingColors.NeonGreen, fontSize = 16.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = onDismissSummary,
                            colors = ButtonDefaults.buttonColors(containerColor = GamingColors.NeonCyan, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("SELESAI & BAYAR", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        if (state.showAdminDialog) {
            AdminDashboardDialog(
                currentSettings = currentSettings,
                allInstalledApps = allApps,
                isDeviceOwner = state.isDeviceOwnerActive,
                isLockTaskActive = state.isLockTaskModeActive,
                onSaveDuration = onSaveDuration,
                onSaveIdentity = onSaveIdentity,
                onSaveWhitelist = onSaveWhitelist,
                onAddTime = onAddTime,
                onSetTime = onSetTime,
                onToggleLockTask = onToggleLockTask,
                onChangePin = onChangePin,
                onCheckUpdate = onCheckUpdate,
                onVerifyPin = onVerifyPin,
                onDismiss = onDismissAdmin
            )
        }

        if (state.showUpdateDialog && state.updateRelease != null) {
            UpdateDialog(
                release = state.updateRelease,
                downloadProgress = state.downloadProgress.toFloat(),
                onUpdateClick = onUpdateNow,
                onLaterClick = onLaterUpdate,
                onSkipClick = onLaterUpdate
            )
        }
    }
}
