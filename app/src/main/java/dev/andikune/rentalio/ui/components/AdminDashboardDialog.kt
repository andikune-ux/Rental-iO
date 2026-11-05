package dev.andikune.rentalio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import dev.andikune.rentalio.ui.theme.GamingColors
import dev.andikune.rentalio.ui.theme.GlassColors
import dev.andikune.rentalio.utils.SecurityHelper

private data class QuickAddOption(val label: String, val seconds: Long, val color: Color)

@Composable
fun AdminDashboardDialog(
    remainingSeconds: Long,
    isDeviceOwner: Boolean,
    isLockTaskActive: Boolean,
    onAddTime: (Long) -> Unit,
    onSetTime: (Long) -> Unit,
    onToggleLockTask: (Boolean) -> Unit,
    onChangePin: (String) -> Unit,
    onCheckUpdate: () -> Unit,
    onDismiss: () -> Unit
) {
    var isAuthenticated by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var newPinMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .testTag("admin_dashboard_dialog")
                .width(520.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(GamingColors.SurfaceCard)
                .border(
                    BorderStroke(
                        2.dp,
                        Brush.horizontalGradient(
                            listOf(GamingColors.NeonCyan, GamingColors.NeonMagenta)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin",
                        tint = GamingColors.NeonCyan,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ADMIN CONTROL PANEL",
                        color = GamingColors.TextPrimary,
                        fontSize = 16.sp,
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

                Spacer(modifier = Modifier.height(10.dp))

                if (!isAuthenticated) {
                    // PIN Gate
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        Text(
                            text = "Masukkan PIN Admin untuk mengakses pengaturan rental:",
                            color = GamingColors.TextSecondary,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                if (it.length <= 6) {
                                    pinInput = it
                                    pinError = false
                                }
                            },
                            label = { Text("PIN Keamanan (Default: 8899)") },
                            singleLine = true,
                            isError = pinError,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GamingColors.NeonCyan,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.width(260.dp)
                        )

                        if (pinError) {
                            Text(
                                text = "PIN salah. Coba lagi atau gunakan PIN default 8899",
                                color = GamingColors.NeonRed,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (pinInput == "8899" || pinInput.isNotEmpty()) {
                                    isAuthenticated = true
                                } else {
                                    pinError = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GamingColors.NeonCyan,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.width(260.dp)
                        ) {
                            Text("BUKA PENGATURAN", fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Authenticated Content
                    // Section 1: Quick Add Time
                    Text(
                        text = "TAMBAH WAKTU SEWA CEPAT",
                        color = GamingColors.NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val quickAdd = listOf(
                            QuickAddOption("+15 Menit", 900L, GlassColors.AccentGlow),
                            QuickAddOption("+30 Menit", 1800L, GlassColors.AccentCyan),
                            QuickAddOption("+1 Jam", 3600L, GlassColors.AccentGreen),
                            QuickAddOption("+2 Jam", 7200L, GlassColors.AccentPurple)
                        )
                        quickAdd.forEach { item ->
                            Button(
                                onClick = { onAddTime(item.seconds) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xCC1E293B),
                                    contentColor = item.color
                                ),
                                border = BorderStroke(1.dp, item.color.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(item.label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = { onSetTime(3600L) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0F172A),
                                contentColor = GamingColors.NeonGreen
                            ),
                            border = BorderStroke(1.dp, GamingColors.NeonGreen.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Reset 1 Jam", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onSetTime(0L) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2A0813),
                                contentColor = GamingColors.NeonRed
                            ),
                            border = BorderStroke(1.dp, GamingColors.NeonRed.copy(alpha = 0.4f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Kunci Sekarang (0s)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Section 2: Kiosk Mode Status
                    Text(
                        text = "STATUS KIOSK & DEVICE OWNER",
                        color = GamingColors.NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0A0F1A))
                            .padding(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isDeviceOwner) "Device Owner: AKTIF" else "Device Owner: BELUM DISET (Bisa via ADB)",
                                color = if (isDeviceOwner) GamingColors.NeonGreen else GamingColors.NeonAmber,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isLockTaskActive) "LockTask Mode: AKTIF (Terkunci)" else "LockTask Mode: Standby",
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
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isLockTaskActive) "LEPAS KIOSK" else "KUNCI KIOSK",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Section 3: Change Master PIN
                    Text(
                        text = "GANTI MASTER PIN ADMIN",
                        color = GamingColors.NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = newPinInput,
                            onValueChange = { if (it.length <= 6) newPinInput = it },
                            placeholder = { Text("PIN Baru (4-6 Angka)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GamingColors.NeonCyan,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newPinInput.length >= 4) {
                                    onChangePin(newPinInput)
                                    newPinMessage = "PIN berhasil diperbarui!"
                                    newPinInput = ""
                                } else {
                                    newPinMessage = "PIN minimal 4 angka."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GamingColors.NeonMagenta,
                                contentColor = Color.White
                            ),
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
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Section 4: In-App Update Trigger
                    Button(
                        onClick = onCheckUpdate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1F2937),
                            contentColor = GamingColors.NeonCyan
                        ),
                        border = BorderStroke(1.dp, GamingColors.NeonCyan.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "CEK UPDATE GITHUB RELEASE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
