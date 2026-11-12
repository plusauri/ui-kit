package tutil.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// The hover-card paper and the one-card-at-a-time owner, shared by the Compose cards here and the
// canvas chart cards in the product.

/** Family A border — also the Compose [TooltipCard] frosted variant, so the two look like one object. */
val ChartTipBorder = Color(0xFFBDBDBD.toInt())

/**
 * Frosted paper — bars and axes show through; text stays on a light wash.
 *
 * The two legibility knobs for every canvas hover card are [ChartTipFill] and [ChartTipInk], and they
 * are the *only* two: charts get their tip style from [chartTipTextStyle] rather than repeating a
 * literal. Raised from 0.78 and darkened from `0xFF212121` (2026-08-17) — at 0.78 enough of a
 * colored bar or gridline read through that the text sat on texture rather than on paper, which
 * costs more legibility than the raw contrast ratio suggests.
 *
 * Deliberately light in both themes: the card is paper laid over the plot, not a themed surface.
 * [HoverTooltip] `frosted = true` uses this same fill so a Compose tip over a chart can be see-through.
 */
val ChartTipFill = Color.White.copy(alpha = 0.88f)

/** Ink for hover-card text. One definition; see [ChartTipFill]. */
val ChartTipInk = Color(0xFF121212)

/**
 * Dwell on the same year/bar/dot before the card latches.
 *
 * Latching is invisible: it does not move, resize, or add anything. It flips event routing — before
 * it, a Move re-targets the chart hit-test so the card tracks the pointer; after it, Moves inside
 * the card's sticky rect are absorbed so you can travel onto the copy plate without the card
 * retargeting out from under you.
 */
const val ChartTipCopyLatchMs = 400L

/**
 * Which chart layer may currently show a hover card. Exactly one, or none.
 *
 * Compose delivers pointer events to one layer at a time, so "last Move wins" is the correct
 * arbitration — the loser is by definition not under the pointer. Held in a
 * [staticCompositionLocalOf] rather than a top-level `object` so a second window or a test can
 * scope its own instance.
 */
/**
 * **At most one hover card app-wide.** Canvas layers claim on Move/Enter; [HoverDetailPopup] and
 * interactive [HoverTooltip] cards claim when they open. Whoever holds the token is the one card
 * on screen; every other holder closes at once — which is what turns a close *grace* into a grace:
 * the outgoing card lingers only until the next one arrives, never beside it. Scrolling down a
 * table used to stack two or three row cards for the length of the grace.
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
