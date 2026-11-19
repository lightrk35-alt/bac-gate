package com.example.ui.theme

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val ZarqyneGameColorScheme = darkColorScheme(
    primary = ZarqyneCyan,
    onPrimary = DeepOledBlack,
    primaryContainer = DarkNavyElevated,
    onPrimaryContainer = SoftSilver,
    secondary = ZarqyneViolet,
    onSecondary = SoftSilver,
    secondaryContainer = DarkNavyCard,
    onSecondaryContainer = SoftSilver,
    tertiary = HitEmerald,
    onTertiary = DeepOledBlack,
    tertiaryContainer = HitEmeraldSurface,
    onTertiaryContainer = HitEmerald,
    error = MissCrimson,
    onError = SoftSilver,
    errorContainer = MissCrimsonSurface,
    onErrorContainer = MissCrimson,
    background = DeepOledBlack,
    onBackground = SoftSilver,
    surface = DarkNavy,
    onSurface = SoftSilver,
    surfaceVariant = DarkNavyElevated,
    onSurfaceVariant = MutedSilver,
    outline = HairlineNavy
)

val BacGateShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

/**
 * Custom angled chamfered 2D game plate shape — replaces standard Material rounded cards
 * with crisp diagonal corner cuts characteristic of ZARQYNE.
 */
@Composable
fun rememberChamferShape(cut: Dp = 14.dp): Shape {
    val cutPx = with(LocalDensity.current) { cut.toPx() }
    return GenericShape { size, _ ->
        val c = cutPx.coerceAtMost(minOf(size.width, size.height) * 0.35f)
        moveTo(c, 0f)
        lineTo(size.width - c, 0f)
        lineTo(size.width, c)
        lineTo(size.width, size.height - c)
        lineTo(size.width - c, size.height)
        lineTo(c, size.height)
        lineTo(0f, size.height - c)
        lineTo(0f, c)
        close()
    }
}

@Composable
fun BacGateTheme(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = ZarqyneGameColorScheme,
            typography = BacGateTypography,
            shapes = BacGateShapes,
            content = content
        )
    }
}
