package com.example.mdpdf.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MatchaLightColorScheme = lightColorScheme(
    primary = MatchaPrimary,
    onPrimary = OnMatchaPrimary,
    primaryContainer = MatchaPrimaryContainer,
    onPrimaryContainer = OnMatchaPrimaryContainer,
    secondary = MatchaSecondary,
    onSecondary = OnMatchaSecondary,
    secondaryContainer = MatchaSecondaryContainer,
    onSecondaryContainer = OnMatchaSecondaryContainer,
    tertiary = MatchaTertiary,
    onTertiary = OnMatchaTertiary,
    tertiaryContainer = MatchaTertiaryContainer,
    onTertiaryContainer = OnMatchaTertiaryContainer,
    error = MatchaError,
    onError = OnMatchaError,
    errorContainer = MatchaErrorContainer,
    onErrorContainer = OnMatchaErrorContainer,
    background = MatchaBackground,
    onBackground = OnMatchaBackground,
    surface = MatchaSurface,
    onSurface = OnMatchaSurface,
    surfaceVariant = MatchaSurfaceVariant,
    onSurfaceVariant = OnMatchaSurfaceVariant,
    outline = MatchaOutline,
    outlineVariant = MatchaOutlineVariant,
    inverseSurface = MatchaInverseSurface,
    inverseOnSurface = MatchaInverseOnSurface,
    inversePrimary = MatchaInversePrimary,
    surfaceTint = MatchaSurfaceTint,
)

private val MatchaDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = OnDarkPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = OnDarkPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = OnDarkSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = OnDarkSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = OnDarkTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = OnDarkTertiaryContainer,
    error = DarkError,
    onError = OnDarkError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = OnDarkErrorContainer,
    background = DarkBackground,
    onBackground = OnDarkBackground,
    surface = DarkSurface,
    onSurface = OnDarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnDarkSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,
    surfaceTint = DarkSurfaceTint,
)

private val MatchaBlackColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = OnDarkPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = OnDarkPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = OnDarkSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = OnDarkSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = OnDarkTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = OnDarkTertiaryContainer,
    error = DarkError,
    onError = OnDarkError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = OnDarkErrorContainer,
    background = BlackBackground,
    onBackground = OnBlackBackground,
    surface = BlackSurface,
    onSurface = OnBlackSurface,
    surfaceVariant = BlackSurfaceVariant,
    onSurfaceVariant = OnBlackSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    inverseSurface = BlackInverseSurface,
    inverseOnSurface = BlackInverseOnSurface,
    surfaceTint = DarkSurfaceTint,
)

@Composable
fun MdPdfTheme(
    appTheme: String = "light",
    content: @Composable () -> Unit
) {
    val colorScheme = when (appTheme) {
        "dark" -> MatchaDarkColorScheme
        "pure_black" -> MatchaBlackColorScheme
        else -> MatchaLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
