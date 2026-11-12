package tutil.ui

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

// The ambient values a hover card reads from whatever opened it. Each was declared beside the
// screen that first needed it; they live here so the card and its hosts share one definition.

/**
 * The density outside a locally scaled region — the navigation strip draws at [StripScale]. A hover
 * card opened from inside it reads this so its body renders at the app's scale, not the strip's.
 * Null everywhere else.
 */
val LocalUnscaledDensity = staticCompositionLocalOf<Density?> { null }

/**
 * Optional hook for an enclosing hover/dismiss-on-exit popup (e.g. the collection-editor detail
 * tooltip) to keep itself open while this button's copy menu is open. The button reports `true`
 * when its copy menu is expanded and `false` when it closes or leaves composition; a host
 * that relies on pointer-exit dismissal would otherwise tear down the moment the menu (a sibling
 * popup) takes the pointer, destroying the menu before a selection is made. Defaults to `null`
 * (no host to notify), so it is a no-op everywhere else.
 */
val LocalPopupDismissSuppressor = staticCompositionLocalOf<((Boolean) -> Unit)?> { null }

/**
 * Horizontal space an enclosing [HelpContainer] using [HelpIconLayout.ShareBoth] reserves in
 * the top-right corner for its overlaid help icon. Content that places its own controls in that
 * corner (e.g. [CopyableDataTable]'s copy button) reads this and insets them so the icon does
 * not cover them. Defaults to `0.dp` — i.e. no overlay to clear.
 *
 * CompositionLocals propagate into [androidx.compose.ui.window.Popup] content, so a hover card
 * that is *not* under the overlay must reset this to `0.dp` (see [HoverDetailPopup]) — otherwise
 * a title-row copy on the card is left-shifted by the parent's FeatureToolbar width even though
 * nothing is overlaid on the popup.
 */
val LocalTopEndOverlayInset = staticCompositionLocalOf { 0.dp }
