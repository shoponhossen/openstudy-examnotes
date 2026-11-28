package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ExamNestDarkColorScheme = darkColorScheme(
    primary = PrimaryIndigo,
    onPrimary = OnPrimaryIndigo,
    primaryContainer = PrimaryContainerIndigo,
    onPrimaryContainer = OnPrimaryContainerIndigo,
    secondary = SecondaryAmber,
    onSecondary = OnSecondaryAmber,
    secondaryContainer = SecondaryContainerAmber,
    onSecondaryContainer = OnSecondaryContainerAmber,
    tertiary = TertiaryMint,
    onTertiary = OnTertiaryMint,
    tertiaryContainer = TertiaryContainerMint,
    onTertiaryContainer = OnTertiaryContainerMint,
    background = SurfaceBase,
    onBackground = OnSurfacePrimary,
    surface = SurfaceBase,
    onSurface = OnSurfacePrimary,
    surfaceVariant = SurfaceContainerHighest,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    outline = OutlineSubtle,
    outlineVariant = OutlineVariant,
    error = ErrorRed,
    onError = OnErrorRed,
    errorContainer = ErrorContainerRed,
    onErrorContainer = OnErrorContainerRed
)

val ExamNestShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun ExamNestTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ExamNestDarkColorScheme,
        typography = ExamNestTypography,
        shapes = ExamNestShapes,
        content = content
    )
}
