package dev.andikune.rentalio.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.rentalio.ui.theme.GlassColors
import dev.andikune.rentalio.ui.theme.liquidGlass
import dev.andikune.rentalio.utils.SecurityHelper

@Composable
fun NeonTimerWidget(
    remainingSeconds: Long,
    isExpired: Boolean,
    modifier: Modifier = Modifier
) {
    val isCritical = remainingSeconds in 1..300
    val infiniteTransition = rememberInfiniteTransition(label = "glassPulse")
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

    Box(
        modifier = modifier
            .testTag("floating_neon_timer")
            .scale(if (isCritical) pulseScale else 1f)
            .liquidGlass(cornerRadius = 20.dp, isSelected = isCritical)
            .padding(horizontal = 24.dp, vertical = 14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
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
                    modifier = Modifier.size(18.dp)
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
                    letterSpacing = 1.8.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Digital timer display: 01j : 45m : 22s
            Text(
                text = SecurityHelper.formatDuration(remainingSeconds),
                color = if (isExpired) GlassColors.AccentRed else GlassColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Liquid Glass Accent Progress Bar (Frost Sapphire Blue)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x33FFFFFF))
            ) {
                val progressFraction = (remainingSeconds % 3600L).toFloat() / 3600f
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = progressFraction.coerceIn(0.02f, 1f))
                        .height(4.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    accentColor,
                                    GlassColors.AccentGlowLight
                                )
                            )
                        )
                )
            }
        }
    }
}
