package plusauri.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

// The ambient values a hover card reads from whatever opened it. They live here so the card and
// its hosts share one definition.

/**
 * The density outside a locally scaled region (a strip drawn at a reduced scale, say). **The host
 * provides it** inside such a region; a hover card opened from inside it reads this so its body
 * renders at the app's scale, not the region's. Null everywhere else.
 */
val LocalUnscaledDensity = staticCompositionLocalOf<Density?> { null }

/**
 * Optional hook for an enclosing hover/dismiss-on-exit popup (a [HoverDetailPopup] card, say) to
 * keep itself open while a copy button's menu inside it is open. The button reports `true`
 * when its copy menu is expanded and `false` when it closes or leaves composition; a host
 * that relies on pointer-exit dismissal would otherwise tear down the moment the menu (a sibling
 * popup) takes the pointer, destroying the menu before a selection is made. Defaults to `null`
 * (no host to notify), so it is a no-op everywhere else.
 */
val LocalPopupDismissSuppressor = staticCompositionLocalOf<((Boolean) -> Unit)?> { null }

/**
 * Horizontal space an enclosing container reserves in its top-right corner for controls it overlays
 * there (a help icon, say). **The container provides it.** Content that places its own controls in
 * that corner (a table's copy button) reads this and insets them so the overlay does not cover
 * them. Defaults to `0.dp`, i.e. no overlay to clear.
 *
 * CompositionLocals propagate into [androidx.compose.ui.window.Popup] content, so a hover card
 * that is *not* under the overlay must reset this to `0.dp` (see [HoverDetailPopup]) — otherwise
 * a title-row copy button on the card is left-shifted by the parent's overlay width even though
 * nothing is overlaid on the popup.
 */
val LocalTopEndOverlayInset = staticCompositionLocalOf { 0.dp }
