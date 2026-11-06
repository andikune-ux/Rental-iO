package dev.andikune.rentalio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.andikune.rentalio.data.local.RentalPreferences
import dev.andikune.rentalio.data.model.WhitelistApp
import dev.andikune.rentalio.ui.theme.GamingColors
import dev.andikune.rentalio.utils.SecurityHelper

@Composable
fun AdminDashboardDialog(
    currentSettings: RentalPreferences.SettingsData,
    allInstalledApps: List<WhitelistApp>,
    isDeviceOwner: Boolean,
    isLockTaskActive: Boolean,
    onSaveDuration: (hours: Int, minutes: Int, seconds: Int, bonusMinutes: Int, isUnlimited: Boolean) -> Unit,
    onSaveIdentity: (rentalName: String, phoneLabel: String, banner1: String, banner2: String, banner3: String, autoStart: Boolean) -> Unit,
    onSaveWhitelist: (Set<String>) -> Unit,
    onAddTime: (Long) -> Unit,
    onSetTime: (Long) -> Unit,
    onToggleLockTask: (Boolean) -> Unit,
    onChangePin: (String) -> Unit,
    onCheckUpdate: () -> Unit,
    onVerifyPin: (String) -> Boolean,
    onDismiss: () -> Unit
) {
    var isAuthenticated by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    var selectedTab by remember { mutableIntStateOf(0) }

    var hoursInput by remember { mutableStateOf(currentSettings.packageHours.toString()) }
    var minutesInput by remember { mutableStateOf(currentSettings.packageMinutes.toString()) }
    var secondsInput by remember { mutableStateOf("0") }
    var bonusMinutesInput by remember { mutableStateOf(currentSettings.bonusMinutes.toString()) }
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

    val estimatedPrice by remember {
        derivedStateOf {
            val h = hoursInput.toIntOrNull() ?: 0
            val m = minutesInput.toIntOrNull() ?: 0
            SecurityHelper.calculateRegularCost(h, m)
        }
    }

    // PERBAIKAN: Tambahkan properties DialogProperties(usePlatformDefaultWidth = false)
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
                // Header Bar
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
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
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
                    // PERBAIKAN: Bungkus dengan Box dan verticalScroll agar tidak terpotong
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
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Masukkan 6-Digit Master PIN Admin",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // PIN Dots
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                for (i in 0 until 6) {
                                    val isFilled = i < pinInput.length
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clip(CircleShape)
                                            .background(if (isFilled) GamingColors.NeonCyan else Color.White.copy(alpha = 0.2f))
                                            .border(BorderStroke(1.dp, if (isFilled) GamingColors.NeonCyan else Color.Gray), CircleShape)
                                    )
                                }
                            }

                            // Keypad
                            val authKeys = listOf(
                                listOf("1", "2", "3"),
                                listOf("4", "5", "6"),
                                listOf("7", "8", "9"),
                                listOf("C", "0", "OK")
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                authKeys.forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        row.forEach { k ->
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(width = 60.dp, height = 36.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (k == "OK") GamingColors.NeonCyan else Color(0xFF1E293B))
                                                    .clickable {
                                                        when (k) {
                                                            "C" -> if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1)
                                                            "OK" -> {
                                                                if (onVerifyPin(pinInput)) {
                                                                    isAuthenticated = true
                                                                    pinError = false
                                                                } else {
                                                                    pinError = true
                                                                    pinInput = ""
                                                                }
                                                            }
                                                            else -> {
                                                                if (pinInput.length < 6) {
                                                                    val next = pinInput + k
                                                                    pinInput = next
                                                                    if (next.length == 6) {
                                                                        if (onVerifyPin(next)) {
                                                                            isAuthenticated = true
                                                                            pinError = false
                                                                        } else {
                                                                            pinError = true
                                                                            pinInput = ""
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                            ) {
                                                Text(
                                                    text = k,
                                                    color = if (k == "OK") Color.Black else Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (pinError) {
                                Text(
                                    text = "PIN Admin Salah. Silakan coba lagi.",
                                    color = GamingColors.NeonRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                } else {
                    // Dashboard Admin (Tabs)
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color(0xFF101B2E),
                        contentColor = GamingColors.NeonCyan,
                        indicator = { tabPositions ->
                            TabRowDefaults.SecondaryIndicator(
                                Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                color = GamingColors.NeonCyan
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("WAKTU & TARIF", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
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

                    Spacer(modifier = Modifier.height(10.dp))

                    when (selectedTab) {
                        0 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF152238))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Mode Timer Unlimited",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Stopwatch hitung maju + tagihan real-time Rp 100/menit",
                                            color = GamingColors.TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Switch(
                                        checked = isUnlimitedInput,
                                        onCheckedChange = { isUnlimitedInput = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = GamingColors.NeonCyan,
                                            checkedTrackColor = GamingColors.NeonCyan.copy(alpha = 0.5f)
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (!isUnlimitedInput) {
                                    Text(
                                        text = "ATUR DURASI PAKET SEWA REGULER",
                                        color = GamingColors.NeonCyan,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedTextField(
                                            value = hoursInput,
                                            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) hoursInput = it },
                                            label = { Text("Jam", fontSize = 10.sp) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = minutesInput,
                                            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) minutesInput = it },
                                            label = { Text("Menit", fontSize = 10.sp) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = secondsInput,
                                            onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) secondsInput = it },
                                            label = { Text("Detik", fontSize = 10.sp) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = bonusMinutesInput,
                                            onValueChange = { if (it.length <= 3 && it.all { c -> c.isDigit() }) bonusMinutesInput = it },
                                            label = { Text("Bonus (Mnt)", fontSize = 10.sp) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                            modifier = Modifier.weight(1.2f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0F2537))
                                            .border(1.dp, GamingColors.NeonGreen.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = "Estimasi Harga Paket:",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = SecurityHelper.formatRupiah(estimatedPrice),
                                                color = GamingColors.NeonGreen,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        val h = hoursInput.toIntOrNull() ?: 1
                                        val m = minutesInput.toIntOrNull() ?: 0
                                        val s = secondsInput.toIntOrNull() ?: 0
                                        val b = bonusMinutesInput.toIntOrNull() ?: 0
                                        onSaveDuration(h, m, s, b, isUnlimitedInput)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GamingColors.NeonCyan, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("TERAPKAN PENGATURAN DURASI", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "TAMBAH WAKTU CEPAT KE SESI AKTIF",
                                    color = GamingColors.TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    listOf("+15 Mnt" to 900L, "+30 Mnt" to 1800L, "+1 Jam" to 3600L, "+2 Jam" to 7200L).forEach { (lbl, sec) ->
                                        Button(
                                            onClick = { onAddTime(sec) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color.White),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(lbl, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedTextField(
                                        value = rentalNameInput,
                                        onValueChange = { rentalNameInput = it },
                                        label = { Text("Nama Usaha/Rental") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                        modifier = Modifier.weight(1.3f)
                                    )
                                    OutlinedTextField(
                                        value = phoneLabelInput,
                                        onValueChange = { phoneLabelInput = it },
                                        label = { Text("Label Perangkat (Contoh: HP NO. 1)") },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF152238))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Auto-Start Launcher Saat HP Nyala (Boot)",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Langsung masuk Rental.iO saat perangkat direstart",
                                            color = GamingColors.TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Switch(
                                        checked = autoStartInput,
                                        onCheckedChange = { autoStartInput = it },
                                        colors = SwitchDefaults.colors(checkedThumbColor = GamingColors.NeonCyan)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "3 TEKS BANNER BERJALAN DI LAYAR KUNCI",
                                    color = GamingColors.NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = banner1Input,
                                    onValueChange = { banner1Input = it },
                                    label = { Text("Teks Banner 1") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = banner2Input,
                                    onValueChange = { banner2Input = it },
                                    label = { Text("Teks Banner 2") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = banner3Input,
                                    onValueChange = { banner3Input = it },
                                    label = { Text("Teks Banner 3") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        onSaveIdentity(rentalNameInput, phoneLabelInput, banner1Input, banner2Input, banner3Input, autoStartInput)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GamingColors.NeonCyan, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("SIMPAN IDENTITAS & BANNER", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        2 -> {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Text(
                                    text = "PILIH APLIKASI YANG DIIZINKAN PADA CAROUSEL",
                                    color = GamingColors.NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Centang game/aplikasi yang boleh dimainkan oleh penyewa:",
                                    color = GamingColors.TextSecondary,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                LazyColumn(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F172A))
                                        .padding(6.dp)
                                ) {
                                    items(allInstalledApps) { app ->
                                        val isChecked = selectedWhitelist.contains(app.packageName)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    val updated = selectedWhitelist.toMutableSet()
                                                    if (isChecked) updated.remove(app.packageName) else updated.add(app.packageName)
                                                    selectedWhitelist = updated
                                                }
                                                .padding(vertical = 4.dp, horizontal = 6.dp)
                                        ) {
                                            Checkbox(
                                                checked = isChecked,
                                                onCheckedChange = { checked ->
                                                    val updated = selectedWhitelist.toMutableSet()
                                                    if (checked) updated.add(app.packageName) else updated.remove(app.packageName)
                                                    selectedWhitelist = updated
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = GamingColors.NeonCyan,
                                                    checkmarkColor = Color.Black
                                                )
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(app.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text(app.packageName, color = GamingColors.TextSecondary, fontSize = 9.sp)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Button(
                                    onClick = { onSaveWhitelist(selectedWhitelist) },
                                    colors = ButtonDefaults.buttonColors(containerColor = GamingColors.NeonCyan, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("SIMPAN DAFTAR WHITELIST (${selectedWhitelist.size} APLIKASI)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }

                        3 -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = "GANTI 6-DIGIT MASTER PIN ADMIN",
                                    color = GamingColors.NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))

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
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            if (newPinInput.length == 6) {
                                                onChangePin(newPinInput)
                                                newPinMessage = "Master PIN 6-digit berhasil disimpan!"
                                                newPinInput = ""
                                            } else {
                                                newPinMessage = "PIN harus tepat 6 angka."
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = GamingColors.NeonMagenta, contentColor = Color.White),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("SIMPAN PIN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                if (newPinMessage != null) {
                                    Text(
                                        text = newPinMessage!!,
                                        color = GamingColors.NeonGreen,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "STATUS KIOSK HARD LOCK",
                                    color = GamingColors.NeonCyan,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF0F172A))
                                        .padding(10.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isDeviceOwner) "Device Owner: AKTIF" else "Device Owner: Belum Diset",
                                            color = if (isDeviceOwner) GamingColors.NeonGreen else GamingColors.NeonAmber,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (isLockTaskActive) "LockTask Mode: AKTIF (Kunci Keras)" else "LockTask Mode: Standby",
                                            color = GamingColors.TextSecondary,
                                            fontSize = 10.sp
                                        )
                                    }
                                    Button(
                                        onClick = { onToggleLockTask(!isLockTaskActive) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isLockTaskActive) GamingColors.NeonRed else GamingColors.NeonCyan,
                                            contentColor = Color.Black
                                        ),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isLockTaskActive) Icons.Default.LockOpen else Icons.Default.Lock,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isLockTaskActive) "LEPAS KIOSK" else "KUNCI KIOSK",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = onCheckUpdate,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1F2937), contentColor = GamingColors.NeonCyan),
                                    border = BorderStroke(1.dp, GamingColors.NeonCyan.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("CEK PEMBARUAN GITHUB RELEASE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
