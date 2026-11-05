package dev.andikune.rentalio.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.andikune.rentalio.ui.theme.GamingColors

@Composable
fun GameOverOverlay(
    onUnlockWithPin: (String) -> Unit,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    var enteredPin by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "gameOverFlash")
    val redAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "redAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .testTag("game_over_blocking_overlay")
            .fillMaxSize()
            .background(Color(0xF505080E))
            .border(
                BorderStroke(
                    width = 4.dp,
                    brush = Brush.radialGradient(
                        listOf(GamingColors.NeonRed.copy(alpha = redAlpha), Color.Transparent)
                    )
                )
            )
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Left Column: Warning & Game Over Banner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.weight(1.1f).padding(horizontal = 16.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(GamingColors.NeonRed.copy(alpha = 0.2f))
                        .border(BorderStroke(2.dp, GamingColors.NeonRed), shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Expired Alert",
                        tint = GamingColors.NeonRed,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "GAME OVER",
                    color = GamingColors.NeonRed,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 4.sp,
                    modifier = Modifier.alpha(redAlpha + 0.1f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "MASA SEWA TELAH HABIS\nSILAKAN HUBUNGI ADMIN RENTAL",
                    color = GamingColors.TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Perangkat terkunci dalam Kiosk Mode demi keamanan aset. Masukkan Master PIN untuk membuka kembali.",
                    color = GamingColors.TextSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                if (!errorMessage.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = GamingColors.NeonRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Right Column: Neon Keypad
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(0.9f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(GamingColors.SurfaceCardGlass)
                    .border(
                        BorderStroke(1.dp, GamingColors.NeonRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(20.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Admin PIN",
                        tint = GamingColors.NeonRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ADMIN MASTER PIN",
                        color = GamingColors.NeonRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // PIN Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 14.dp)
                ) {
                    for (i in 0 until 4) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) GamingColors.NeonCyan else Color.White.copy(alpha = 0.15f)
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isFilled) GamingColors.NeonCyan else Color.Gray
                                    ),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // 3x4 Numeric Keypad
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "OK")
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    keys.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { key ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(width = 54.dp, height = 38.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when (key) {
                                                "OK" -> GamingColors.NeonCyan
                                                "C" -> GamingColors.NeonRed.copy(alpha = 0.3f)
                                                else -> Color(0xFF1E293B)
                                            }
                                        )
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                when (key) {
                                                    "OK" -> GamingColors.NeonCyan
                                                    "C" -> GamingColors.NeonRed
                                                    else -> Color.White.copy(alpha = 0.1f)
                                                }
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            when (key) {
                                                "C" -> {
                                                    if (enteredPin.isNotEmpty()) {
                                                        enteredPin = enteredPin.dropLast(1)
                                                    }
                                                }
                                                "OK" -> {
                                                    if (enteredPin.isNotEmpty()) {
                                                        onUnlockWithPin(enteredPin)
                                                    }
                                                }
                                                else -> {
                                                    if (enteredPin.length < 6) {
                                                        enteredPin += key
                                                        if (enteredPin.length == 4) {
                                                            onUnlockWithPin(enteredPin)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                ) {
                                    if (key == "C") {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Clear",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            color = if (key == "OK") Color.Black else Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
