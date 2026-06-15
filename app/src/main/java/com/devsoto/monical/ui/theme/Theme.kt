package com.devsoto.monical.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Receipt-paper theme. Dynamic color and the default Material palette are dropped in favour of
 * the fixed [Moni] tokens; most composables set their own colors/typography explicitly, so this
 * mainly seeds sensible defaults for the few Material 3 widgets in use.
 */
private val MonicalColorScheme = lightColorScheme(
    primary = Moni.accent,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    secondary = Moni.accentDark,
    background = Moni.desk,
    onBackground = Moni.ink,
    surface = Moni.paper,
    onSurface = Moni.ink,
    error = Moni.accentDark,
)

@Composable
fun MonicalTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MonicalColorScheme,
        content = content,
    )
}
