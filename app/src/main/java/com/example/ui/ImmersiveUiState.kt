package com.example.ui

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

/**
 * CompositionLocal providing the root-level immersive UI visibility state.
 * When true, top bars, bottom bars, and tab rows are visible.
 * When false, chrome is hidden and Scaffold content padding becomes zero.
 * The lambda allows toggling the state (e.g. on non-interactive taps).
 */
data class ImmersiveUiState(
    val isUiVisible: Boolean = true,
    val toggleUiVisibility: () -> Unit = {},
    val setUiVisible: (Boolean) -> Unit = {}
)

val LocalImmersiveUiState = compositionLocalOf { ImmersiveUiState() }

/**
 * Modifier to consume taps on interactive elements like cards/options/blanks so they
 * do not propagate to the background detectTapGestures and toggle immersive focus mode.
 */
fun Modifier.consumeTaps(): Modifier = this.pointerInput(Unit) {
    detectTapGestures { /* consume tap */ }
}
