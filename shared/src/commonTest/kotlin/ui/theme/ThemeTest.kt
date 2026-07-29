package ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemeTest {
    @Test
    fun surfaceRolesUseTheValeraPalette() {
        assertEquals(Color(0xFFFBF8FF), lightScheme.surface)
        assertEquals(Color(0xFFEFEDF4), lightScheme.surfaceContainer)
        assertEquals(Color(0xFF121318), darkScheme.surface)
        assertEquals(Color(0xFF1F1F25), darkScheme.surfaceContainer)

        assertDescendingLuminance(
            listOf(
                lightScheme.surfaceContainerLowest,
                lightScheme.surfaceBright,
                lightScheme.surfaceContainerLow,
                lightScheme.surfaceContainer,
                lightScheme.surfaceContainerHigh,
                lightScheme.surfaceContainerHighest,
                lightScheme.surfaceDim,
            ),
        )
        assertAscendingLuminance(
            listOf(
                darkScheme.surfaceContainerLowest,
                darkScheme.surfaceDim,
                darkScheme.surfaceContainerLow,
                darkScheme.surfaceContainer,
                darkScheme.surfaceContainerHigh,
                darkScheme.surfaceContainerHighest,
                darkScheme.surfaceBright,
            ),
        )
    }

    @Test
    fun semanticColorsHaveTextContrast() {
        assertSemanticContrast(lightScheme, lightExtendedColors)
        assertSemanticContrast(darkScheme, darkExtendedColors)
    }

    private fun assertSemanticContrast(scheme: ColorScheme, extendedColors: ExtendedColors) {
        listOf(
            scheme.onPrimary to scheme.primary,
            scheme.onPrimaryContainer to scheme.primaryContainer,
            scheme.onSecondary to scheme.secondary,
            scheme.onSecondaryContainer to scheme.secondaryContainer,
            scheme.onTertiary to scheme.tertiary,
            scheme.onTertiaryContainer to scheme.tertiaryContainer,
            scheme.onError to scheme.error,
            scheme.onErrorContainer to scheme.errorContainer,
            scheme.onSurface to scheme.surface,
            scheme.onSurface to scheme.surfaceContainerLow,
            scheme.onSurface to scheme.surfaceContainer,
            scheme.onSurface to scheme.surfaceContainerHigh,
            scheme.onSurface to scheme.surfaceContainerHighest,
            scheme.primary to scheme.surface,
            scheme.tertiary to scheme.surface,
            scheme.error to scheme.surface,
            extendedColors.success to scheme.surface,
            extendedColors.onSuccessContainer to extendedColors.successContainer,
        ).forEach { (foreground, background) ->
            val contrast = contrastRatio(foreground, background)
            assertTrue(
                contrast >= 4.5f,
                "Expected at least 4.5:1 contrast, got $contrast for $foreground on $background",
            )
        }
    }

    private fun contrastRatio(first: Color, second: Color): Float {
        val firstLuminance = first.luminance()
        val secondLuminance = second.luminance()
        return (max(firstLuminance, secondLuminance) + 0.05f) /
                (min(firstLuminance, secondLuminance) + 0.05f)
    }

    private fun assertDescendingLuminance(colors: List<Color>) {
        assertTrue(colors.zipWithNext().all { (first, second) -> first.luminance() >= second.luminance() })
    }

    private fun assertAscendingLuminance(colors: List<Color>) {
        assertTrue(colors.zipWithNext().all { (first, second) -> first.luminance() <= second.luminance() })
    }
}
