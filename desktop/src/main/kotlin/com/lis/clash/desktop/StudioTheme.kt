package com.lis.clash.desktop

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val StudioLight = lightColorScheme(
    primary = Color(0xFF246454), onPrimary = Color.White,
    primaryContainer = Color(0xFFE0EDE7), onPrimaryContainer = Color(0xFF164D3E),
    inversePrimary = Color(0xFF9BCDB8),
    secondary = Color(0xFF926239), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF3E7D8), onSecondaryContainer = Color(0xFF68421F),
    tertiary = Color(0xFF506675), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE1E9EF), onTertiaryContainer = Color(0xFF304653),
    background = Color(0xFFF3F3EE), onBackground = Color(0xFF202D32),
    surface = Color(0xFFFCFCF9), onSurface = Color(0xFF202D32),
    surfaceVariant = Color(0xFFEBEEE8), onSurfaceVariant = Color(0xFF62716D),
    surfaceTint = Color(0xFF246454),
    surfaceBright = Color(0xFFFFFFFF), surfaceDim = Color(0xFFDDE1DA),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF7F8F3),
    surfaceContainer = Color(0xFFF0F2EC), surfaceContainerHigh = Color(0xFFE9ECE5),
    surfaceContainerHighest = Color(0xFFE2E6DF),
    inverseSurface = Color(0xFF26332F), inverseOnSurface = Color(0xFFF0F3ED),
    error = Color(0xFFB34036), onError = Color.White,
    errorContainer = Color(0xFFFBE5E0), onErrorContainer = Color(0xFF80291F),
    outline = Color(0xFF8C9890), outlineVariant = Color(0xFFDCE2D9),
    scrim = Color(0xFF10211B)
)

private val StudioDark = darkColorScheme(
    primary = Color(0xFF94CEB5), onPrimary = Color(0xFF113D2D),
    primaryContainer = Color(0xFF254639), onPrimaryContainer = Color(0xFFC0EAD4),
    inversePrimary = Color(0xFF246454),
    secondary = Color(0xFFDEB387), onSecondary = Color(0xFF472D15),
    secondaryContainer = Color(0xFF493B2D), onSecondaryContainer = Color(0xFFF1D9BA),
    tertiary = Color(0xFFAABFCC), onTertiary = Color(0xFF1F3542),
    tertiaryContainer = Color(0xFF334651), onTertiaryContainer = Color(0xFFD8E8F2),
    background = Color(0xFF161E21), onBackground = Color(0xFFE3EAE6),
    surface = Color(0xFF1C2629), onSurface = Color(0xFFE3EAE6),
    surfaceVariant = Color(0xFF2A383A), onSurfaceVariant = Color(0xFFA5B5AF),
    surfaceTint = Color(0xFF94CEB5),
    surfaceBright = Color(0xFF364244), surfaceDim = Color(0xFF131C1F),
    surfaceContainerLowest = Color(0xFF11191C), surfaceContainerLow = Color(0xFF1A2427),
    surfaceContainer = Color(0xFF202B2E), surfaceContainerHigh = Color(0xFF293537),
    surfaceContainerHighest = Color(0xFF334043),
    inverseSurface = Color(0xFFE3EAE6), inverseOnSurface = Color(0xFF26332F),
    error = Color(0xFFFFB3A5), onError = Color(0xFF601C12),
    errorContainer = Color(0xFF653029), onErrorContainer = Color(0xFFFFDAD2),
    outline = Color(0xFF74867D), outlineVariant = Color(0xFF35433F),
    scrim = Color.Black
)

private fun studioText(size: Int, lineHeight: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = FontFamily.SansSerif, fontWeight = weight,
    fontSize = size.sp, lineHeight = lineHeight.sp
)

private val StudioTypography = Typography(
    displayLarge = studioText(48, 56, FontWeight.SemiBold),
    displayMedium = studioText(40, 48, FontWeight.SemiBold),
    displaySmall = studioText(32, 40, FontWeight.SemiBold),
    headlineLarge = studioText(30, 38, FontWeight.SemiBold),
    headlineMedium = studioText(26, 34, FontWeight.SemiBold),
    headlineSmall = studioText(23, 30, FontWeight.SemiBold),
    titleLarge = studioText(20, 28, FontWeight.SemiBold),
    titleMedium = studioText(15, 22, FontWeight.SemiBold),
    titleSmall = studioText(13, 20, FontWeight.SemiBold),
    bodyLarge = studioText(14, 22),
    bodyMedium = studioText(13, 20),
    bodySmall = studioText(12, 18),
    labelLarge = studioText(13, 18, FontWeight.Medium),
    labelMedium = studioText(11, 16, FontWeight.Medium),
    labelSmall = studioText(10, 14, FontWeight.Medium)
)

/** Shared desktop palette and compact control geometry, including all error and dialog colors. */
@Composable
internal fun StudioTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (dark) StudioDark else StudioLight,
        typography = StudioTypography,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(4.dp), small = RoundedCornerShape(6.dp),
            medium = RoundedCornerShape(8.dp), large = RoundedCornerShape(10.dp),
            extraLarge = RoundedCornerShape(16.dp)
        ),
        content = content
    )
}
