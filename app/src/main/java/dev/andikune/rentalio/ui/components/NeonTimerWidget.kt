package dev.andikune.rentalio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.rentalio.ui.theme.GlassColors
import dev.andikune.rentalio.ui.theme.liquidGlass
import java.util.Locale

@Composable
fun NeonTimerWidget(
    remainingSeconds: Long,
    isExpired: Boolean,
    modifier: Modifier = Modifier
) {
    val isCritical = remainingSeconds in 1..300
    val infiniteTransition = rememberInfiniteTransition(label = "timerPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isCritical) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val accentColor by animateColorAsState(
        targetValue = when {
            isExpired -> GlassColors.AccentRed
            isCritical -> GlassColors.AccentAmber
            else -> GlassColors.AccentGlow
        },
        label = "accentColor"
    )

    val hours = remainingSeconds / 3600
    val minutes = (remainingSeconds % 3600) / 60
    val seconds = remainingSeconds % 60

    Box(
        modifier = modifier
            .testTag("floating_neon_timer")
            .scale(if (isCritical) pulseScale else 1f)
            .liquidGlass(cornerRadius = 20.dp, isSelected = isCritical)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header: Status Icon + Status Text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = when {
                        isExpired -> Icons.Default.Warning
                        isCritical -> Icons.Default.HourglassBottom
                        else -> Icons.Default.Timer
                    },
                    contentDescription = "Timer Status",
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        isExpired -> "MASA SEWA HABIS"
                        isCritical -> "WAKTU HAMPIR HABIS"
                        else -> "SISA WAKTU SEWA"
                    },
                    color = accentColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.6.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Vertical Digital Clock: Big Numbers (JAM : MNT : DTK) with Subtitle Labels
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                TimeBlock(
                    value = String.format(Locale.US, "%02d", hours),
                    label = "JAM",
                    color = if (isExpired) GlassColors.AccentRed else GlassColors.TextPrimary
                )

                Text(
                    text = ":",
                    color = accentColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )

                TimeBlock(
                    value = String.format(Locale.US, "%02d", minutes),
                    label = "MNT",
                    color = if (isExpired) GlassColors.AccentRed else GlassColors.TextPrimary
                )

                Text(
                    text = ":",
                    color = accentColor,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp)
                )

                TimeBlock(
                    value = String.format(Locale.US, "%02d", seconds),
                    label = "DTK",
                    color = if (isExpired) GlassColors.AccentRed else GlassColors.TextPrimary
                )
            }
        }
    }
}

@Composable
private fun TimeBlock(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = color,
            fontSize = 30.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )
        Text(
            text = label,
            color = GlassColors.TextSecondary,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp
        )
    }
}
