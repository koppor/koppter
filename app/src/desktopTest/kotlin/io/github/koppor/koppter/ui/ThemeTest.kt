package io.github.koppor.koppter.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.koppor.koppter.storage.ColorTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemeTest {
    // [utest->dsn~color-themes~1]
    @Test
    fun eachThemeHasALightAndADarkVariantWithReadableText() {
        assertEquals(Color(0xFFECEFF4), colorScheme(ColorTheme.NORD, dark = false).background)
        assertEquals(Color(0xFF2E3440), colorScheme(ColorTheme.NORD, dark = true).background)
        assertEquals(Color(0xFF8DA101), colorScheme(ColorTheme.EVERFOREST, dark = false).primary)

        for (theme in ColorTheme.entries) {
            assertTrue(colorScheme(theme, dark = true).background.luminance() < colorScheme(theme, dark = false).background.luminance())
            for (dark in listOf(false, true)) {
                val colors = colorScheme(theme, dark)
                val pairs = listOf(
                    colors.onSurface to colors.surface,
                    colors.onSurface to colors.surfaceContainerHighest,
                    colors.onSurfaceVariant to colors.surfaceVariant,
                    colors.onPrimaryContainer to colors.primaryContainer,
                    colors.onSecondaryContainer to colors.secondaryContainer,
                    colors.onTertiaryContainer to colors.tertiaryContainer,
                    colors.onErrorContainer to colors.errorContainer,
                )
                // Text of the size of a label reads at a ratio of 3; Everforest does not reach the 4.5 of body text everywhere.
                pairs.forEach { (text, ground) -> assertTrue(contrast(text, ground) >= 3f, "$theme dark=$dark: $text on $ground") }
            }
        }
    }
}
