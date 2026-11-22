package plusauri.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// The hover-card paper and the one-card-at-a-time arbiter, shared by the Compose cards here and any
// card a host draws on a chart canvas. The card families are defined on [TipPlacement].

/**
 * Border of a canvas-drawn card (Family A), and of the Compose [TooltipCard]'s frosted variant, so
 * the two look like one object.
 */
val ChartTipBorder = Color(0xFFBDBDBD.toInt())

/**
 * Frosted paper — bars and axes show through; text stays on a light wash.
 *
 * The two legibility knobs for every canvas hover card are [ChartTipFill] and [ChartTipInk], and they
 * are the *only* two: a chart should take its card text color from [ChartTipInk] rather than
 * repeating a literal. The fill is opaque enough (0.88) that the text sits on paper rather than on
 * texture: with much more of a colored bar or gridline showing through, legibility drops further
 * than the raw contrast ratio suggests.
 *
 * Deliberately light in both themes: the card is paper laid over the plot, not a themed surface.
 * [HoverTooltip] `frosted = true` uses this same fill so a Compose tip over a chart can be see-through.
 */
val ChartTipFill = Color.White.copy(alpha = 0.88f)

/** Ink for hover-card text. One definition; see [ChartTipFill]. */
val ChartTipInk = Color(0xFF121212)

/**
 * Dwell on the same data point (a bar, a dot, a row) before the card latches (see [TipPlacement]
 * for the term).
 *
 * Latching flips event routing: before it, a Move re-targets the hit-test so the card tracks the
 * pointer; after it, a move toward the card is absorbed, so the reader can travel onto the card and
 * copy from it without the card retargeting out from under them. A latched card also stops
 * following the pointer and repaints in the frozen fill ([tipFrozenFill]), so the reader can see it
 * has settled. A move away releases it.
 */
const val ChartTipCopyLatchMs = 400L

/**
 * **At most one hover card at a time.** A host's canvas layers claim on Move/Enter;
 * [HoverDetailPopup] and interactive [HoverTooltip] cards claim when they open. Whoever holds the
 * token is the one card on screen; every other holder closes at once. That is what turns a close
 * *grace* into a grace: the outgoing card lingers only until the next one arrives, never beside
 * it, so scrolling down a table does not stack two or three row cards for the length of the grace.
 *
 * Compose delivers pointer events to one layer at a time, so "last Move wins" is the correct
 * arbitration: the loser is by definition not under the pointer. Provided through
 * [LocalChartTipOwner], a [staticCompositionLocalOf] rather than a top-level `object`, so a second
 * window or a test can scope its own instance.
 */
class ChartTipOwner {
    private var current by mutableStateOf<Any?>(null)

    fun claim(token: Any) {
        if (current !== token) current = token
    }

    fun release(token: Any) {
        if (current === token) current = null
    }

    fun holds(token: Any): Boolean = current === token
}

val LocalChartTipOwner = staticCompositionLocalOf { ChartTipOwner() }
