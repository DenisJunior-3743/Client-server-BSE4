package com.trustbank.loanapp.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class ExtendedColors(
    val success: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val neutralText: Color,
    val mutedText: Color,
    val sidebarBackground: Color,
)

private val LocalExtendedColors = staticCompositionLocalOf {
    ExtendedColors(
        success = AppColors.Success600,
        successContainer = AppColors.Success50,
        onSuccessContainer = AppColors.Success700,
        warning = AppColors.Warning600,
        warningContainer = AppColors.Warning50,
        onWarningContainer = AppColors.Warning700,
        neutralText = AppColors.Neutral800,
        mutedText = AppColors.Neutral500,
        sidebarBackground = AppColors.Neutral900,
    )
}

private val AppLightColorScheme = lightColorScheme(
    primary = AppColors.Primary600,
    onPrimary = Color.White,
    primaryContainer = AppColors.Primary50,
    onPrimaryContainer = AppColors.Primary700,
    secondary = AppColors.Neutral700,
    onSecondary = Color.White,
    error = AppColors.Danger600,
    onError = Color.White,
    errorContainer = AppColors.Danger50,
    onErrorContainer = AppColors.Danger700,
    background = AppColors.Neutral50,
    onBackground = AppColors.Neutral900,
    surface = Color.White,
    onSurface = AppColors.Neutral900,
    surfaceVariant = AppColors.Neutral100,
    onSurfaceVariant = AppColors.Neutral600,
    outline = AppColors.Neutral300,
    outlineVariant = AppColors.Neutral200,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

object TrustBankTheme {
    val extendedColors: ExtendedColors
        @Composable get() = LocalExtendedColors.current
}

@Composable
fun TrustBankLoansTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalExtendedColors provides LocalExtendedColors.current) {
        MaterialTheme(
            colorScheme = AppLightColorScheme,
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
