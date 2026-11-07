package dev.andikune.rentalio.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.andikune.rentalio.ui.theme.GamingColors
import dev.andikune.rentalio.ui.theme.GlassColors
import dev.andikune.rentalio.data.model.GitHubRelease

@Composable
fun UpdateDialog(
    release: GitHubRelease,
    downloadProgress: Float,
    onUpdateClick: () -> Unit,
    onLaterClick: () -> Unit,
    onSkipClick: () -> Unit
) {
    Dialog(
        onDismissRequest = onLaterClick,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFF0B1322))
                .border(BorderStroke(2.dp, GamingColors.NeonCyan), RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = GamingColors.NeonCyan,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "PEMBARUAN APLIKASI TERSEDIA",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Versi Baru: ${release.tagName}",
                    color = GamingColors.NeonGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E293B))
                        .padding(12.dp)
                ) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            text = "Catatan Rilis:",
                            color = GlassColors.AccentCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = release.body.ifBlank { "Tidak ada catatan rilis." },
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                val progressFraction = when {
                    downloadProgress in 0f..1f -> downloadProgress
                    downloadProgress > 1f -> (downloadProgress / 100f).coerceIn(0f, 1f)
                    else -> -1f
                }
                val isDownloading = progressFraction >= 0f && progressFraction < 1f

                if (progressFraction >= 0f) {
                    Text(
                        text = if (progressFraction >= 1f) {
                            "Unduhan selesai. Lanjutkan izin pemasangan APK."
                        } else {
                            "Mengunduh... ${(progressFraction * 100).toInt()}%"
                        },
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = GamingColors.NeonCyan,
                        trackColor = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Button(
                    onClick = onUpdateClick,
                    enabled = !isDownloading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GamingColors.NeonCyan,
                        contentColor = Color.Black,
                        disabledContainerColor = GamingColors.NeonCyan.copy(alpha = 0.35f),
                        disabledContentColor = Color.Black.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text(
                        text = if (isDownloading) "MENGUNDUH..." else "UNDUH & INSTALL SEKARANG",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onLaterClick,
                        border = BorderStroke(1.dp, Color.Gray),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("NANTI", color = Color.LightGray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onSkipClick,
                        border = BorderStroke(1.dp, GamingColors.NeonRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("LEWATI", color = GamingColors.NeonRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
