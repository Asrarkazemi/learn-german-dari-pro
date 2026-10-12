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
 * Modifier to attach tap-to-toggle at the ROOT content container of detail screens:
 * any tap NOT consumed by an interactive child (buttons, clickable chips, text fields)
 * toggles the shared immersive state. Scrolling does NOT trigger or restore chrome.
 */
fun Modifier.immersiveTapToggle(immersiveState: ImmersiveUiState): Modifier = this.pointerInput(immersiveState) {
    detectTapGestures(
        onTap = {
            immersiveState.toggleUiVisibility()
        }
    )
}

/**
 * Modifier preserved for backwards compatibility where explicit tap consumption is needed.
 */
fun Modifier.consumeTaps(): Modifier = this
