package dev.andikune.rentalio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.rentalio.ui.theme.GlassColors
import dev.andikune.rentalio.utils.SecurityHelper

@Composable
fun NeonTimerWidget(
    remainingSeconds: Long,
    isExpired: Boolean,
    isUnlimitedMode: Boolean = false,
    unlimitedElapsedSeconds: Long = 0L,
    packageHours: Int = 1,
    packageMinutes: Int = 0,
    bonusMinutes: Int = 0,
    modifier: Modifier = Modifier
) {
    val isCritical = !isUnlimitedMode && remainingSeconds in 1..300
    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCritical) 1.05f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val accentColor by animateColorAsState(
        targetValue = when {
            isUnlimitedMode -> GlassColors.AccentCyan
            isExpired -> GlassColors.AccentRed
            isCritical -> GlassColors.AccentAmber
            else -> GlassColors.AccentGlow
        },
        label = "accentColor"
    )

    // Calculate display time
    val displaySeconds = if (isUnlimitedMode) unlimitedElapsedSeconds else remainingSeconds
    val hours = (displaySeconds / 3600).toString().padStart(2, '0')
    val minutes = ((displaySeconds % 3600) / 60).toString().padStart(2, '0')
    val seconds = (displaySeconds % 60).toString().padStart(2, '0')

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .testTag("floating_neon_timer")
            .scale(if (isCritical) pulseScale else 1f)
    ) {
        if (isUnlimitedMode) {
            // Mode Unlimited (Bagian 1 Butir 4)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AllInclusive,
                    contentDescription = "Unlimited",
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "MODE UNLIMITED (STOPWATCH)",
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Digital Stopwatch Count-Up
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimerBlock(value = hours, label = "JAM", color = accentColor)
                Text(":", color = accentColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                TimerBlock(value = minutes, label = "MNT", color = accentColor)
                Text(":", color = accentColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                TimerBlock(value = seconds, label = "DTK", color = accentColor)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Live running bill: "Tagihan saat ini: Rp X.XXX" (Minutes * 100)
            val currentBill = SecurityHelper.calculateUnlimitedCost(unlimitedElapsedSeconds)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x3300D4FF))
                    .border(
                        1.dp,
                        GlassColors.AccentCyan.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AttachMoney,
                        contentDescription = null,
                        tint = GlassColors.AccentCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Tagihan saat ini: ${SecurityHelper.formatRupiah(currentBill)}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            // Mode Reguler Hitung Mundur (Bagian 1 Butir 6)
            // Header: Detail Paket & Bonus (Paket: [Jam/Menit] | Bonus: [X] Mnt)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Timer",
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val packageText = buildString {
                    append("Paket: ")
                    if (packageHours > 0) append("${packageHours} Jam ")
                    if (packageMinutes > 0 || packageHours == 0) append("${packageMinutes} Mnt ")
                    if (bonusMinutes > 0) append("| Bonus: ${bonusMinutes} Mnt")
                }
                Text(
                    text = packageText,
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimerBlock(value = hours, label = "JAM", color = accentColor)
                Text(":", color = accentColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                TimerBlock(value = minutes, label = "MNT", color = accentColor)
                Text(":", color = accentColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                TimerBlock(value = seconds, label = "DTK", color = accentColor)
            }
        }
    }
}

@Composable
private fun TimerBlock(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = GlassColors.TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = label,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}
