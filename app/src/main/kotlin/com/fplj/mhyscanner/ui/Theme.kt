package com.fplj.mhyscanner.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 参考米游社 Blue 主题,手工调校的主色板
private val Blue = Color(0xFF4F6AFF)
private val BlueBright = Color(0xFF6E8BFF)
private val Navy = Color(0xFF1E2A4F)
private val NavyDeep = Color(0xFF12192E)

private val LightColors = lightColorScheme(
    primary = Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E8FF),
    onPrimaryContainer = Navy,
    inversePrimary = BlueBright,
    secondary = Color(0xFF5B6478),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE2E6F4),
    onSecondaryContainer = Color(0xFF1D2335),
    tertiary = Color(0xFF7A5AA8),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF0E6FF),
    onTertiaryContainer = Color(0xFF2D1B47),
    background = Color(0xFFF7F8FC),
    onBackground = Color(0xFF1A1D26),
    surface = Color(0xFFFBFBFF),
    onSurface = Color(0xFF1A1D26),
    surfaceVariant = Color(0xFFEEF0F8),
    onSurfaceVariant = Color(0xFF454B5C),
    surfaceTint = Blue,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F4FB),
    surfaceContainer = Color(0xFFECEEF7),
    surfaceContainerHigh = Color(0xFFE7E9F3),
    surfaceContainerHighest = Color(0xFFE1E4EF),
    outline = Color(0xFFB9C0D1),
    outlineVariant = Color(0xFFDBE0EE),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

private val DarkColors = darkColorScheme(
    primary = BlueBright,
    onPrimary = Color(0xFF243063),
    primaryContainer = Color(0xFF38477F),
    onPrimaryContainer = Color(0xFFE3E8FF),
    inversePrimary = Blue,
    secondary = Color(0xFFC0C7DA),
    onSecondary = Color(0xFF313A4E),
    secondaryContainer = Color(0xFF464E66),
    onSecondaryContainer = Color(0xFFE2E6F4),
    tertiary = Color(0xFFD3BCF5),
    onTertiary = Color(0xFF3A2A55),
    tertiaryContainer = Color(0xFF59446F),
    onTertiaryContainer = Color(0xFFF0E6FF),
    background = NavyDeep,
    onBackground = Color(0xFFE4E7F2),
    surface = Color(0xFF171E36),
    onSurface = Color(0xFFE4E7F2),
    surfaceVariant = Color(0xFF232B47),
    onSurfaceVariant = Color(0xFFC2C8DE),
    surfaceTint = BlueBright,
    surfaceContainerLowest = Color(0xFF10152A),
    surfaceContainerLow = Color(0xFF171E36),
    surfaceContainer = Color(0xFF1C233D),
    surfaceContainerHigh = Color(0xFF222A47),
    surfaceContainerHighest = Color(0xFF2C3551),
    outline = Color(0xFF6E7690),
    outlineVariant = Color(0xFF3B4360),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

/** 紧凑清晰的排版:小字 12sp、正文 14sp、标题 18sp 突出 */
private val AppTypography = Typography(
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 22.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.2.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

@Composable
fun MHYTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}