package dev.andikune.rentalio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
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
        targetValue = if (isCritical) 1.05f else 1f,
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

    val hours = (remainingSeconds / 3600).toString().padStart(2, '0')
    val minutes = ((remainingSeconds % 3600) / 60).toString().padStart(2, '0')
    val seconds = (remainingSeconds % 60).toString().padStart(2, '0')

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .testTag("floating_neon_timer")
            .scale(if (isCritical) pulseScale else 1f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = "Timer",
                tint = accentColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (isExpired) "WAKTU HABIS" else "SISA WAKTU",
                color = accentColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
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
