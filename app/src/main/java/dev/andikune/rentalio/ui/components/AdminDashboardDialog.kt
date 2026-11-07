package dev.andikune.rentalio.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.andikune.rentalio.data.local.RentalPreferences
import dev.andikune.rentalio.data.model.WhitelistApp
import dev.andikune.rentalio.ui.theme.GamingColors
import dev.andikune.rentalio.utils.SecurityHelper
import kotlinx.coroutines.delay

@Composable
fun AdminDashboardDialog(
    currentSettings: RentalPreferences.SettingsData,
    allInstalledApps: List<WhitelistApp>,
    isDeviceOwner: Boolean,
    isLockTaskActive: Boolean,
    isDefaultLauncher: Boolean,
    isOverlayGranted: Boolean,
    isSessionRunning: Boolean,
    initiallyAuthenticated: Boolean = false,
    onSaveDuration: (hours: Int, minutes: Int, seconds: Int, bonusMinutes: Int, isUnlimited: Boolean) -> Unit,
    onSaveIdentity: (rentalName: String, phoneLabel: String, banner1: String, banner2: String, banner3: String, autoStart: Boolean) -> Unit,
    onSaveWhitelist: (Set<String>) -> Unit,
    onAddTime: (Long) -> Unit,
    onSetTime: (Long) -> Unit,
    onEndSession: () -> Unit,
    onToggleLockTask: (Boolean) -> Unit,
    onChangePin: (String) -> Unit,
    onCheckUpdate: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onRequestDefaultLauncher: () -> Unit,
    onRequestOverlay: () -> Unit,
    onDismiss: () -> Unit
) {
    var isAuthenticated by remember { mutableStateOf(initiallyAuthenticated) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var failedPinAttempts by remember { mutableIntStateOf(0) }
    var pinLockoutSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(pinLockoutSeconds) {
        if (pinLockoutSeconds > 0) {
            delay(1000)
            pinLockoutSeconds -= 1
        }
    }

    val submitPin: (String) -> Unit = { enteredPin ->
        if (pinLockoutSeconds > 0) {
            pinError = true
        } else if (onVerifyPin(enteredPin)) {
            isAuthenticated = true
            pinError = false
            failedPinAttempts = 0
            pinInput = ""
        } else {
            failedPinAttempts += 1
            pinError = true
            pinInput = ""
            if (failedPinAttempts >= 5) {
                failedPinAttempts = 0
                pinLockoutSeconds = 30
            }
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }

    // PERBAIKAN TIMER: Gunakan .ifBlank { "0" } agar tidak error saat kosong
    var hoursInput by remember { mutableStateOf(if (currentSettings.packageHours > 0) currentSettings.packageHours.toString() else "0") }
    var minutesInput by remember { mutableStateOf(if (currentSettings.packageMinutes > 0) currentSettings.packageMinutes.toString() else "0") }
    var secondsInput by remember { mutableStateOf("0") }
    var bonusMinutesInput by remember { mutableStateOf(if (currentSettings.bonusMinutes > 0) currentSettings.bonusMinutes.toString() else "0") }
    var isUnlimitedInput by remember { mutableStateOf(currentSettings.isUnlimitedMode) }

    var rentalNameInput by remember { mutableStateOf(currentSettings.rentalName) }
    var phoneLabelInput by remember { mutableStateOf(currentSettings.phoneLabel) }
    var banner1Input by remember { mutableStateOf(currentSettings.bannerText1) }
    var banner2Input by remember { mutableStateOf(currentSettings.bannerText2) }
    var banner3Input by remember { mutableStateOf(currentSettings.bannerText3) }
    var autoStartInput by remember { mutableStateOf(currentSettings.autoStartOnBoot) }

    var selectedWhitelist by remember { mutableStateOf(currentSettings.whitelistPackages.toMutableSet()) }

    var newPinInput by remember { mutableStateOf("") }
    var newPinMessage by remember { mutableStateOf<String?>(null) }
    var newPinIsError by remember { mutableStateOf(false) }

    var durationError by remember { mutableStateOf<String?>(null) }
    var whitelistSearch by remember { mutableStateOf("") }
    var confirmEndSession by remember { mutableStateOf(false) }

    LaunchedEffect(confirmEndSession) {
        if (confirmEndSession) {
            delay(4000)
            confirmEndSession = false
        }
    }

    val filteredApps = remember(allInstalledApps, whitelistSearch) {
        val query = whitelistSearch.trim()
        if (query.isEmpty()) {
            allInstalledApps
        } else {
            allInstalledApps.filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        }
    }

    val estimatedPrice by remember {
        derivedStateOf {
            val h = hoursInput.toIntOrNull() ?: 0
            val m = minutesInput.toIntOrNull() ?: 0
            SecurityHelper.calculateRegularCost(h, m)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .testTag("admin_dashboard_dialog")
                .fillMaxWidth(0.92f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0B1322))
                .border(
                    BorderStroke(
                        2.dp,
                        Brush.horizontalGradient(
                            listOf(GamingColors.NeonCyan, GamingColors.NeonMagenta)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin",
                        tint = GamingColors.NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ADMIN CONTROL PANEL",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = Color.LightGray
                        )
                    }
                }

                if (!isAuthenticated) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(vertical = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = GamingColors.NeonCyan,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Masukkan 6-Digit Master PIN",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                for (i in 0 until 6) {
                                    val isFilled = i < pinInput.length
                                    Box(
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(if (isFilled) GamingColors.NeonCyan else Color.White.copy(alpha = 0.15f))
                                            .border(BorderStroke(1.dp, if (isFilled) GamingColors.NeonCyan else Color.Gray.copy(alpha = 0.5f)), CircleShape)
                                    )
                                }
                            }

                            val authKeys = listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf("C", "0", "OK")
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                authKeys.forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        row.forEach { k ->
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(width = 64.dp, height = 42.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(if (k == "OK") GamingColors.NeonCyan else Color(0xFF1E293B))
                                                    .clickable {
                                                        if (pinLockoutSeconds > 0 && k != "C") return@clickable
                                                        when (k) {
                                                            "C" -> if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                                                            "OK" -> submitPin(pinInput)
                                                            else -> {
                                                                if (pinInput.length < 6) {
                                                                    val next = pinInput + k
                                                                    pinInput = next
                                                                    if (next.length == 6) {
                                                                        submitPin(next)
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                            ) {
                                                Text(
                                                    text = k,
                                                    color = if (k == "OK") Color.Black else Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (pinLockoutSeconds > 0) {
                                Text(
                                    text = "Terlalu banyak percobaan. Tunggu ${pinLockoutSeconds} detik.",
                                    color = GamingColors.NeonRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            } else if (pinError) {
                                Text(
                                    text = "PIN Salah! Sisa percobaan sebelum terkunci: ${5 - failedPinAttempts}",
                                    color = GamingColors.NeonRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }
                        }
                    }
                } else {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = GamingColors.NeonCyan,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = GamingColors.NeonCyan,
                                height = 3.dp
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("WAKTU", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("IDENTITAS", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("WHITELIST", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Apps, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            text = { Text("KEAMANAN", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    when (selectedTab) {
                        0 -> {
                            // UI Waktu
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF152238))
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Mode Timer Unlimited",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Tagihan berjalan Rp 100/menit",
                                            color = GamingColors.TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Switch(
                                        checked = isUnlimitedInput,
                                        onCheckedChange = {
                                            isUnlimitedInput = it
                                            durationError = null
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = GamingColors.NeonCyan,
                                            checkedTrackColor = GamingColors.NeonCyan.copy(alpha = 0.3f)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                if (!isUnlimitedInput) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = hoursInput,
                                            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) hoursInput = it },
                                            label = { Text("Jam", fontSize = 11.sp) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GamingColors.NeonCyan, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = minutesInput,
                                            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) minutesInput = it },
                                            label = { Text("Menit", fontSize = 11.sp) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GamingColors.NeonCyan, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = secondsInput,
                                            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) secondsInput = it },
                                            label = { Text("Detik", fontSize = 11.sp) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GamingColors.NeonCyan, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = bonusMinutesInput,
                                            onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) bonusMinutesInput = it },
                                            label = { Text("Bonus", fontSize = 11.sp) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GamingColors.NeonGreen, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                            modifier = Modifier.weight(1.2f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF0F2537))
                                            .border(1.dp, GamingColors.NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 14.dp, vertical = 10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Estimasi Harga:",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = SecurityHelper.formatRupiah(estimatedPrice),
                                                color = GamingColors.NeonGreen,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        val h = hoursInput.toIntOrNull() ?: 0
                                        val m = minutesInput.toIntOrNull() ?: 0
                                        val s = secondsInput.toIntOrNull() ?: 0
                                        val b = bonusMinutesInput.toIntOrNull() ?: 0
                                        val totalSeconds = (h * 3600L) + (m * 60L) + s + (b * 60L)
                                        if (!isUnlimitedInput && totalSeconds <= 0L) {
                                            durationError = "Isi durasi lebih dari 0 detik, atau aktifkan mode Unlimited."
                                        } else {
                                            durationError = null
                                            onSaveDuration(h, m, s, b, isUnlimitedInput)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GamingColors.NeonCyan, contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("TERAPKAN PENGATURAN DURASI", fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }

                                if (durationError != null) {
                                    Text(
                                        text = durationError!!,
                                        color = GamingColors.NeonRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "TAMBAH WAKTU CEPAT KE SESI AKTIF",
                                    color = GamingColors.TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf("+15 Mnt" to 900L, "+30 Mnt" to 1800L, "+1 Jam" to 3600L, "+2 Jam" to 7200L).forEach { (lbl, sec) ->
                                        Button(
                                            onClick = { onAddTime(sec) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
                                            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(lbl, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (isSessionRunning) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    OutlinedButton(
                                        onClick = {
                                            if (confirmEndSession) {
                                                confirmEndSession = false
                                                onEndSession()
                                            } else {
                                                confirmEndSession = true
                                            }
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (confirmEndSession) GamingColors.NeonRed else Color.Gray.copy(alpha = 0.5f)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().height(44.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LockOpen,
                                            contentDescription = null,
                                            tint = if (confirmEndSession) GamingColors.NeonRed else Color.LightGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (confirmEndSession) {
                                                "TEKAN LAGI UNTUK KONFIRMASI AKHIRI SESI"
                                            } else {
                                                "AKHIRI SESI SEKARANG"
                                            },
                                            color = if (confirmEndSession) GamingColors.NeonRed else Color.LightGray,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        1 -> {
                            // UI Identitas
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = rentalNameInput,
                                        onValueChange = { rentalNameInput = it },
                                        label = { Text("Nama Rental") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GamingColors.NeonCyan, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                        modifier = Modifier.weight(1.3f)
                                    )
                                    OutlinedTextField(
                                        value = phoneLabelInput,
                                        onValueChange = { phoneLabelInput = it },
                                        label = { Text("Label HP (HP NO. 1)") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GamingColors.NeonCyan, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF152238))
                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Auto-Start Saat HP Menyala",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Switch(
                                        checked = autoStartInput,
                                        onCheckedChange = { autoStartInput = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = GamingColors.NeonCyan)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "TEKS BANNER DI LAYAR KUNCI",
                                    color = GamingColors.NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = banner1Input,
                                    onValueChange = { banner1Input = it },
                                    label = { Text("Teks Banner 1") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GamingColors.NeonCyan, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = banner2Input,
                                    onValueChange = { banner2Input = it },
                                    label = { Text("Teks Banner 2") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GamingColors.NeonCyan, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = banner3Input,
                                    onValueChange = { banner3Input = it },
                                    label = { Text("Teks Banner 3") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GamingColors.NeonCyan, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = {
                                        onSaveIdentity(rentalNameInput, phoneLabelInput, banner1Input, banner2Input, banner3Input, autoStartInput)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GamingColors.NeonCyan, contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("SIMPAN IDENTITAS", fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                            }
                        }

                        2 -> {
                            // UI WHITELIST (PERBAIKAN TOTAL: MENGGUNAKAN GRID VERTIKAL)
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "PILIH APLIKASI YANG DIIZINKAN",
                                        color = GamingColors.NeonCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${selectedWhitelist.size} Dipilih",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))

                                OutlinedTextField(
                                    value = whitelistSearch,
                                    onValueChange = { whitelistSearch = it },
                                    label = { Text("Cari aplikasi atau package...", fontSize = 11.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = GamingColors.NeonCyan,
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                LazyVerticalGrid(
                                    columns = GridCells.Adaptive(minSize = 100.dp), // Menyesuaikan ukuran layar
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0F172A)),
                                    contentPadding = PaddingValues(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (filteredApps.isEmpty()) {
                                        item {
                                            Text(
                                                text = "Aplikasi tidak ditemukan.",
                                                color = Color.Gray,
                                                fontSize = 11.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(16.dp)
                                            )
                                        }
                                    }

                                    items(filteredApps) { app ->
                                        val isChecked = selectedWhitelist.contains(app.packageName)
                                        
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(130.dp) // Tinggi fix agar rapi
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(if (isChecked) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFF1E293B))
                                                .border(
                                                    BorderStroke(2.dp, if (isChecked) GamingColors.NeonCyan else Color.Transparent),
                                                    RoundedCornerShape(14.dp)
                                                )
                                                .clickable {
                                                    val updated = selectedWhitelist.toMutableSet()
                                                    if (isChecked) updated.remove(app.packageName) else updated.add(app.packageName)
                                                    selectedWhitelist = updated
                                                }
                                                .padding(8.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color.Black.copy(alpha = 0.4f))
                                            ) {
                                                AppIcon(
                                                    packageName = app.packageName,
                                                    tint = if (isChecked) Color.White else Color.Gray,
                                                    modifier = Modifier.size(38.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Text(
                                                text = app.label,
                                                color = if (isChecked) Color.White else Color.LightGray,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 2,
                                                minLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                                textAlign = TextAlign.Center,
                                                lineHeight = 14.sp
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isChecked) GamingColors.NeonCyan else Color.Transparent)
                                                    .border(1.dp, if (isChecked) GamingColors.NeonCyan else Color.Gray, CircleShape)
                                            ) {
                                                if (isChecked) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.Black,
                                                        modifier = Modifier.size(14.dp).align(Alignment.Center)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = { onSaveWhitelist(selectedWhitelist) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GamingColors.NeonCyan, contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("SIMPAN WHITELIST (${selectedWhitelist.size} APLIKASI)", fontWeight = FontWeight.Black, fontSize = 13.sp)
                                }
                            }
                        }

                        3 -> {
                            // UI Keamanan
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = "GANTI MASTER PIN",
                                    color = GamingColors.NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = newPinInput,
                                        onValueChange = { if (it.length <= 6 && it.all { c -> c.isDigit() }) newPinInput = it },
                                        label = { Text("PIN Baru (6 Angka)") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = GamingColors.NeonCyan, focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Button(
                                        onClick = {
                                            if (newPinInput.length == 6) {
                                                onChangePin(newPinInput)
                                                newPinMessage = "PIN berhasil disimpan!"
                                                newPinIsError = false
                                                newPinInput = ""
                                            } else {
                                                newPinMessage = "PIN harus 6 angka."
                                                newPinIsError = true
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GamingColors.NeonMagenta, contentColor = Color.White),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("SIMPAN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (newPinMessage != null) {
                                    Text(
                                        text = newPinMessage!!,
                                        color = if (newPinIsError) GamingColors.NeonRed else GamingColors.NeonGreen,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "STATUS KIOSK",
                                    color = GamingColors.NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF152238))
                                        .padding(12.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isDeviceOwner) "Device Owner: AKTIF" else "Device Owner: Belum Diset",
                                            color = if (isDeviceOwner) GamingColors.NeonGreen else GamingColors.NeonAmber,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isLockTaskActive) "LockTask: AKTIF" else "LockTask: Standby",
                                            color = GamingColors.TextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Button(
                                        onClick = { onToggleLockTask(!isLockTaskActive) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isLockTaskActive) GamingColors.NeonRed else GamingColors.NeonCyan,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isLockTaskActive) Icons.Default.LockOpen else Icons.Default.Lock,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isLockTaskActive) "LEPAS" else "KUNCI",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                StatusActionRow(
                                    title = "HOME LAUNCHER",
                                    isActive = isDefaultLauncher,
                                    activeText = "Aktif sebagai Home default",
                                    inactiveText = "Belum menjadi Home default",
                                    buttonText = if (isDefaultLauncher) "GANTI" else "JADIKAN HOME",
                                    onClick = onRequestDefaultLauncher
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                StatusActionRow(
                                    title = "IZIN OVERLAY",
                                    isActive = isOverlayGranted,
                                    activeText = "Overlay alarm & countdown aktif",
                                    inactiveText = "Overlay belum diizinkan",
                                    buttonText = if (isOverlayGranted) "PENGATURAN" else "IZINKAN",
                                    onClick = onRequestOverlay
                                )

                                if (!isDeviceOwner) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Untuk kunci senyap tanpa dialog, jadikan aplikasi ini Device Owner lewat ADB:\n" +
                                            "adb shell dpm set-device-owner dev.andikune.rentalio/.receiver.AdminReceiver",
                                        color = GamingColors.TextSecondary,
                                        fontSize = 10.sp,
                                        lineHeight = 14.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = onCheckUpdate,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937), contentColor = GamingColors.NeonCyan),
                                    border = BorderStroke(1.dp, GamingColors.NeonCyan.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("CEK PEMBARUAN APLIKASI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusActionRow(
    title: String,
    isActive: Boolean,
    activeText: String,
    inactiveText: String,
    buttonText: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF152238))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = if (isActive) activeText else inactiveText,
                color = if (isActive) GamingColors.NeonGreen else GamingColors.NeonAmber,
                fontSize = 10.sp
            )
        }
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isActive) Color(0xFF1E293B) else GamingColors.NeonCyan,
                contentColor = if (isActive) Color.White else Color.Black
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(buttonText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AppIcon(
    packageName: String,
    tint: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val bitmap = remember(packageName) {
        try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val size = 96
            val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            drawable.setBounds(0, 0, size, size)
            drawable.draw(canvas)
            bmp
        } catch (_: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = packageName,
            modifier = modifier
        )
    } else {
        Icon(
            imageVector = Icons.Default.Android,
            contentDescription = packageName,
            tint = tint,
            modifier = modifier
        )
    }
}
