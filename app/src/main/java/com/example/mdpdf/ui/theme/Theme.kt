package com.example.mdpdf.ui.theme

import androidx.compose.material3.MaterialTheme
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
    background = MatchaBackground,
    onBackground = OnMatchaBackground,
    surface = MatchaSurface,
    onSurface = OnMatchaSurface,
    surfaceVariant = MatchaSurfaceVariant,
    onSurfaceVariant = OnMatchaSurfaceVariant,
    outline = MatchaOutline,
)

@Composable
fun MdPdfTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MatchaLightColorScheme,
        typography = Typography,
        content = content
    )
}
