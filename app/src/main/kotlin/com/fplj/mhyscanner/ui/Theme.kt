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

// 黑/白灰阶单色主题:primary 用近黑(浅色)/近白(深色),容器与文字用灰阶分层
private val Ink = Color(0xFF1A1A1A)
private val Paper = Color(0xFFF7F7F7)

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E5E5),
    onPrimaryContainer = Ink,
    inversePrimary = Color(0xFFC9C9C9),
    secondary = Color(0xFF5A5A5A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0E0E0),
    onSecondaryContainer = Color(0xFF1F1F1F),
    tertiary = Color(0xFF444444),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD9D9D9),
    onTertiaryContainer = Color(0xFF212121),
    background = Paper,
    onBackground = Color(0xFF111111),
    surface = Color(0xFFFDFDFD),
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFEFEFEF),
    onSurfaceVariant = Color(0xFF4A4A4A),
    surfaceTint = Ink,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF4F4F4),
    surfaceContainer = Color(0xFFEFEFEF),
    surfaceContainerHigh = Color(0xFFE9E9E9),
    surfaceContainerHighest = Color(0xFFE2E2E2),
    outline = Color(0xFFB5B5B5),
    outlineVariant = Color(0xFFD8D8D8),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE6E6E6),
    onPrimary = Ink,
    primaryContainer = Color(0xFF3A3A3A),
    onPrimaryContainer = Color(0xFFF5F5F5),
    inversePrimary = Color(0xFF2E2E2E),
    secondary = Color(0xFFBDBDBD),
    onSecondary = Color(0xFF2E2E2E),
    secondaryContainer = Color(0xFF3E3E3E),
    onSecondaryContainer = Color(0xFFE8E8E8),
    tertiary = Color(0xFFAAAAAA),
    onTertiary = Color(0xFF2B2B2B),
    tertiaryContainer = Color(0xFF464646),
    onTertiaryContainer = Color(0xFFE0E0E0),
    background = Color(0xFF111111),
    onBackground = Color(0xFFE8E8E8),
    surface = Color(0xFF161616),
    onSurface = Color(0xFFE8E8E8),
    surfaceVariant = Color(0xFF262626),
    onSurfaceVariant = Color(0xFFC4C4C4),
    surfaceTint = Color(0xFFE6E6E6),
    surfaceContainerLowest = Color(0xFF0D0D0D),
    surfaceContainerLow = Color(0xFF161616),
    surfaceContainer = Color(0xFF1C1C1C),
    surfaceContainerHigh = Color(0xFF222222),
    surfaceContainerHighest = Color(0xFF2C2C2C),
    outline = Color(0xFF757575),
    outlineVariant = Color(0xFF3D3D3D),
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