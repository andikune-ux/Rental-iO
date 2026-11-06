package dev.andikune.rentalio.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import dev.andikune.rentalio.ui.theme.GlassColors
import kotlinx.coroutines.delay

@Composable
fun GameOverOverlay(
    phoneLabel: String = "HP NO. 1",
    bannerTexts: List<String> = listOf(
        "Rental HP murah 30 menit 3ribu aja 😆",
        "Rental HP murah 60 Menit 5ribu aja 😁",
        "Mohon menggunakan HP rental dengan hati-hati..."
    ),
    onUnlockWithPin: (String) -> Unit,
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    var enteredPin by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "gameOverFlash")
    val redAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(700),
            repeatMode = RepeatMode.Reverse
        ),
        label = "redAlpha"
    )

    // Animated Banner Carousel index
    var bannerIndex by remember { mutableIntStateOf(0) }
    val safeBanners = remember(bannerTexts) {
        if (bannerTexts.isEmpty()) listOf("Selamat datang di Rental.iO Gaming Kiosk") else bannerTexts
    }

    LaunchedEffect(safeBanners) {
        while (true) {
            delay(4000L)
            if (safeBanners.isNotEmpty()) {
                bannerIndex = (bannerIndex + 1) % safeBanners.size
            }
        }
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .testTag("game_over_blocking_overlay")
            .fillMaxSize()
            .background(Color(0xFA05080E))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Left Column: Warning Info, Phone Label, and Animated Sliding Banner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .weight(1.15f)
                    .fillMaxHeight()
                    .padding(vertical = 8.dp)
            ) {
                // Top Warning Section
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(GamingColors.NeonRed.copy(alpha = 0.2f))
                                .border(BorderStroke(2.dp, GamingColors.NeonRed), shape = CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Expired Alert",
                                tint = GamingColors.NeonRed,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = "GAME OVER",
                            color = GamingColors.NeonRed,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 3.sp,
                            modifier = Modifier.alpha(redAlpha)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Phone Label Indicator
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0x3310B981))
                            .border(BorderStroke(1.dp, GlassColors.AccentGreen.copy(alpha = 0.5f)), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Smartphone,
                                contentDescription = null,
                                tint = GlassColors.AccentGreen,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = phoneLabel.ifBlank { "HP NO. 1" },
                                color = GlassColors.AccentGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "MASA SEWA TELAH HABIS\nSILAKAN HUBUNGI ADMIN RENTAL",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    if (!errorMessage.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = errorMessage,
                            color = GamingColors.NeonRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Bottom: Animated Sliding Carousel Banner (Bagian 1 Butir 5)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xCC0E1726))
                        .border(
                            BorderStroke(1.dp, GamingColors.NeonCyan.copy(alpha = 0.4f)),
                            RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = "Promo Info",
                            tint = GamingColors.NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AnimatedContent(
                            targetState = safeBanners[bannerIndex.coerceIn(0, safeBanners.size - 1)],
                            transitionSpec = {
                                (slideInVertically { height -> height } + fadeIn())
                                    .togetherWith(slideOutVertically { height -> -height } + fadeOut())
                            },
                            label = "bannerSlideAnimation",
                            modifier = Modifier.weight(1f)
                        ) { bannerText ->
                            Text(
                                text = bannerText,
                                color = GamingColors.TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Start,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            // Right Column: Clean 6-Digit PIN Keypad (Bagian 1 Butir 2 & Bagian 2 Butir 10)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(0.95f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xE60E1726))
                    .border(
                        BorderStroke(1.dp, GamingColors.NeonRed.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Admin PIN",
                        tint = GamingColors.NeonRed,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ADMIN MASTER PIN",
                        color = GamingColors.NeonRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 6 PIN Dots (No PIN hint/leakage)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    for (i in 0 until 6) {
                        val isFilled = i < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) GamingColors.NeonCyan else Color.White.copy(alpha = 0.15f)
                                )
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isFilled) GamingColors.NeonCyan else Color.Gray.copy(alpha = 0.4f)
                                    ),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // 3x4 Numeric Keypad: Compact, perfectly proportioned for landscape
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("C", "0", "OK")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    keys.forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { key ->
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(width = 56.dp, height = 36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when (key) {
                                                "OK" -> GamingColors.NeonCyan
                                                "C" -> GamingColors.NeonRed.copy(alpha = 0.35f)
                                                else -> Color(0xFF1E293B)
                                            }
                                        )
                                        .border(
                                            BorderStroke(
                                                1.dp,
                                                when (key) {
                                                    "OK" -> GamingColors.NeonCyan
                                                    "C" -> GamingColors.NeonRed
                                                    else -> Color.White.copy(alpha = 0.12f)
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
                                                        val updated = enteredPin + key
                                                        enteredPin = updated
                                                        if (updated.length == 6) {
                                                            onUnlockWithPin(updated)
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                ) {
                                    if (key == "C") {
                                        Icon(
                                            imageVector = Icons.Default.Backspace,
                                            contentDescription = "Hapus",
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    } else {
                                        Text(
                                            text = key,
                                            color = if (key == "OK") Color.Black else Color.White,
                                            fontSize = 14.sp,
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
