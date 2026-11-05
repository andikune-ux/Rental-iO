package dev.andikune.rentalio.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Clean Liquid Glass Theme (Transparent Fill, Thin Glowing Outline)
 */
object GlassColors {
    val DeepBackground = Color(0xFF050810)
    val DeepBackgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF0A0E1A), Color(0xFF0F172A), Color(0xFF050810))
    )
    val GlassTint = Color(0x0DFFFFFF)
    val GlassTintLighter = Color(0x1AFFFFFF)
    val GlassSolidFallback = Color(0x0AFFFFFF) // Transparent Clean Overlay
    val GlassBorder = Color(0x26FFFFFF)
    val AccentGlow = Color(0xFF4DA3FF) // Frost Sapphire Blue
    val AccentGlowLight = Color(0xFF80C3FF)
    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFF94A3B8)
    val SurfaceDark = Color(0xFF0F172A)
    val AccentGreen = Color(0xFF10B981) // Emerald Green
    val AccentRed = Color(0xFFEF4444)   // Ruby Red
    val AccentAmber = Color(0xFFF59E0B) // Amber
    val AccentCyan = Color(0xFF38BDF8)
    val AccentPurple = Color(0xFF818CF8)

    // Compatibility mappings
    val DeepBlack = DeepBackground
    val DarkSurface = SurfaceDark
    val SurfaceCard = GlassSolidFallback
    val SurfaceCardGlass = Color(0x0DFFFFFF)
    val NeonCyan = AccentGlow
    val NeonMagenta = AccentPurple
    val NeonGreen = AccentGreen
    val NeonRed = AccentRed
    val NeonAmber = AccentAmber
    val NeonViolet = AccentPurple
    val StabiloYellow = AccentGlow
    val StabiloGreen = AccentGreen
    val StabiloCyan = AccentCyan
    val StabiloPink = AccentPurple
    val StabiloOrange = AccentAmber
    val StabiloRed = AccentRed

    // Gradients
    val CyanGlowBrush = Brush.linearGradient(listOf(AccentGlow, AccentCyan))
    val BlueGlowBrush = Brush.linearGradient(listOf(AccentGlow, Color(0xFF2563EB)))
    val GreenGlowBrush = Brush.linearGradient(listOf(AccentGreen, Color(0xFF34D399)))
    val RedAlertBrush = Brush.linearGradient(listOf(AccentRed, Color(0xFF991B1B)))
    val PurpleGlowBrush = Brush.linearGradient(listOf(AccentPurple, AccentGlow))
    val MagentaGlowBrush = PurpleGlowBrush
    val StabiloLimeGlowBrush = CyanGlowBrush
}

typealias GamingColors = GlassColors

/**
 * Liquid Glass modifier: Only thin border with transparent interior
 */
fun Modifier.liquidGlass(
    cornerRadius: Dp = 20.dp,
    isSelected: Boolean = false,
    borderWidth: Dp = 1.dp
): Modifier {
    val shape = RoundedCornerShape(cornerRadius)
    val backgroundColor = if (isSelected) Color(0x1A4DA3FF) else Color(0x08FFFFFF)
    val currentBorderColor = if (isSelected) GlassColors.AccentGlow else GlassColors.GlassBorder
    val effectiveBorderWidth = if (isSelected) 1.5.dp else borderWidth

    return this
        .clip(shape)
        .then(
            if (isSelected) {
                Modifier.drawBehind {
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = listOf(GlassColors.AccentGlow.copy(alpha = 0.20f), Color.Transparent),
                            radius = size.maxDimension * 0.8f
                        )
                    )
                }
            } else Modifier
        )
        .background(backgroundColor, shape)
        .border(BorderStroke(effectiveBorderWidth, currentBorderColor), shape)
}

private val DarkColorScheme = darkColorScheme(
    primary = GlassColors.AccentGlow,
    onPrimary = Color.White,
    secondary = GlassColors.AccentCyan,
    onSecondary = Color.Black,
    tertiary = GlassColors.AccentGreen,
    background = GlassColors.DeepBackground,
    onBackground = GlassColors.TextPrimary,
    surface = GlassColors.SurfaceDark,
    onSurface = GlassColors.TextPrimary,
    error = GlassColors.AccentRed,
    onError = Color.White
)

@Composable
fun RentalIOGamingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
