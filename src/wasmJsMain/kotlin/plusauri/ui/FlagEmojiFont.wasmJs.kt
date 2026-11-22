package plusauri.ui

import androidx.compose.runtime.Composable

/**
 * A pass-through: the kit ships no emoji font for the web. A wasm host that shows flags loads an
 * emoji font with flag ligatures itself and registers it as a fallback font family.
 */
@Composable
actual fun ProvideFlagEmojiFont(content: @Composable () -> Unit) {
    content()
}
