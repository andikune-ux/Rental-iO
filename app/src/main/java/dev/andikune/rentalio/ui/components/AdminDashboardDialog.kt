package dev.andikune.rentalio.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (pinError) {
                                Text(
                                    text = "PIN Salah!",
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
                                        onCheckedChange = { isUnlimitedInput = it },
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
                                        onSaveDuration(h, m, s, b, isUnlimitedInput)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = GamingColors.NeonCyan, contentColor = Color.Black),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("TERAPKAN PENGATURAN DURASI", fontWeight = FontWeight.Black, fontSize = 13.sp)
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
                            }
                        }

                        1 -> {
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
                                        text = "Geser >>",
                                        color = Color.Gray,
                                        fontSize = 10.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))

                                LazyRow(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF0F172A)),
                                    contentPadding = PaddingValues(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(allInstalledApps) { app ->
                                        val isChecked = selectedWhitelist.contains(app.packageName)
                                        
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center,
                                            modifier = Modifier
                                                .width(100.dp)
                                                .heightIn(min = 130.dp, max = 150.dp) // FIX BUG LAYOUT: Tinggi seragam
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
                                                .padding(10.dp)
                                        ) {
                                            Box(
                                                contentAlignment = Alignment.Center,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color.Black.copy(alpha = 0.4f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Android,
                                                    contentDescription = null,
                                                    tint = if (isChecked) Color.White else Color.Gray,
                                                    modifier = Modifier.size(32.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(10.dp))
                                            
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
                                            Spacer(modifier = Modifier.height(8.dp))
                                            
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isChecked) GamingColors.NeonCyan else Color.Transparent)
                                                    .border(1.dp, if (isChecked) GamingColors.NeonCyan else Color.Gray, CircleShape)
                                            ) {
                                                if (isChecked) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = null,
                                                        tint = Color.Black,
                                                        modifier = Modifier.size(16.dp).align(Alignment.Center)
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
                                                newPinInput = ""
                                            } else {
                                                newPinMessage = "PIN harus 6 angka."
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
                                        color = GamingColors.NeonGreen,
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
