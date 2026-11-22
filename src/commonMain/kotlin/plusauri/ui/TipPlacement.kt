package plusauri.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.math.roundToInt

/**
 * Where a hover card goes, relative to what raised it. **The one placement point** for every hover
 * card: the Compose popups ([HoverTooltip], [HoverDetailPopup], [OverflowTooltipText]) and any card
 * a host draws itself on a chart canvas all resolve their geometry through [tipCardOffset] with one
 * of these.
 *
 * ## Terms used across the hover-card KDoc
 *
 * The hover cards fall into three **families**, which share this placer, [TipDepth] and the
 * paper tokens in `ChartTipTokens.kt`:
 * - **Family A**: a card the host *draws* on a chart canvas (a readout for the bar or point under
 *   the pointer). The kit supplies its geometry ([tipCardOffset]), fill and ink ([ChartTipFill],
 *   [ChartTipInk], [ChartTipBorder]) and depth ([drawTipShadow], [drawTipFace], [drawTipEdge]);
 *   the host paints it.
 * - **Family B**: a rich Compose card in a popup, with content the reader may move into to select
 *   or copy ([HoverDetailPopup], an `interactive` [HoverTooltip]).
 * - **Family C**: a one-line or short-prose hint in a popup that only names or explains (the string
 *   [HoverTooltip], [OverflowTooltipText]'s full-text reveal).
 *
 * Words for how a card behaves over time:
 * - **Parked**: the card sits at a fixed corner of the layout and never follows the pointer
 *   ([Parked]).
 * - **Latched**: the pointer has stayed still on the anchor long enough ([ChartTipCopyLatchMs])
 *   that the card stops following it, repaints in the frozen fill ([TipFrozenFill]), and waits for
 *   the reader to come and copy from it.
 * - **Flight**: the pointer has left the anchor and is travelling toward its card. While a flight
 *   is under way the card stays open and the anchors crossed on the way do not open their own
 *   cards ([HoverFlightState]).
 *
 * Placement is deliberately only *where*. Chrome (frosted or opaque) and timing (intent, close
 * grace, flight) vary independently of it: the same placement can be frosted over a chart and
 * opaque over a table. So those stay options on the host ([HoverDetailOptions], [HoverTooltip]'s
 * parameters), not variants here.
 */
sealed interface TipPlacement {
    /**
     * Canvas readout: rides the pointer, offset beside it ([FollowsPointerDxPx] / [FollowsPointerDyPx])
     * so it never sits under the cursor and never steals Move. The only placement that re-computes
     * on every Move.
     *
     * Vertical side: **above the pointer by preference**, below only when the card does not fit
     * above — height decides only when it has to. Above wins over below for an asymmetric reason:
     * with the card's *bottom* edge just over the pointer, every short card sits in the same
     * place, and the one card too tall to fit drops below with its *top* edge just under the
     * pointer. So the edge nearest the hand barely moves from one data point to the next, and that
     * jump is what reads as bouncing. Preferring below would keep the top edge fixed instead, but
     * would send every short card down over the lower part of the plot. Horizontal: right of the
     * pointer, left when that would overrun the edge.
     */
    data object FollowsPointer : TipPlacement

    /**
     * Opens with its top-left a hair up-left of the pointer ([TipPointerInsetPx]) so the card is
     * hovered the instant it opens, then holds. For a card the reader moves *into* — a copy button,
     * a selection. Its cost is that it covers the anchor under the pointer, so a click aimed at the
     * anchor lands on the card ([HoverDetailPopup]'s `onClick` mirrors the anchor's click to make
     * that survivable).
     */
    data object UnderPointer : TipPlacement

    /**
     * Hangs beside the anchor on [side], never under the pointer, and flips to the other side when
     * there is no room. [alignX] is [TipAlignX.Center] for a small anchor (an icon, a year label)
     * and [TipAlignX.AtPointer] for a wide one (a table row) — a centered card on a full-width row
     * lands nowhere near the hand.
     */
    data class BesideAnchor(
        val side: TipSide = TipSide.Above,
        val alignX: TipAlignX = TipAlignX.Center,
    ) : TipPlacement

    /**
     * Parked: the card's [cardCorner] sits on the corner (window / canvas px) and the card grows
     * away from it; it never follows the pointer. For a pair of tables read side by side, a chart
     * readout that must stay out of the region the pointer is sweeping, or a click-opened panel
     * that covers its own button.
     *
     * The corner is **read at placement time** through [cornerAt], never remembered: a corner
     * captured from an earlier layout goes stale when the same table is shown in another host (a
     * dialog sits at a different x than a side panel), and a stale corner parks the card over the
     * far side of the window. A host that measures the corner keeps the `LayoutCoordinates` and
     * reads `boundsInWindow()` inside the lambda. [key] is the placement's identity for equality
     * (the flight in [HoverFlightState] keys on it; every row parking in the same corner shares
     * one): a string naming the corner, or the fixed offset itself.
     *
     * [clampY] false means "do not pull the card back on screen vertically", for a card parked
     * below a chart: a card too tall to fit below the plot must be *sized* to the room, because
     * clamping it up drags it over the bars the pointer is on, which is the one thing parking
     * there exists to prevent. x is always clamped.
     */
    class Parked(
        val cornerAt: () -> IntOffset,
        val cardCorner: TipCorner,
        val clampY: Boolean = true,
        val key: Any,
    ) : TipPlacement {
        /** A fixed corner — a test, or a corner derived from the anchor inside the provider. */
        constructor(corner: IntOffset, cardCorner: TipCorner, clampY: Boolean = true) :
            this({ corner }, cardCorner, clampY, key = corner)

        override fun equals(other: Any?): Boolean =
            other is Parked && other.key == key && other.cardCorner == cardCorner && other.clampY == clampY
        override fun hashCode(): Int = (key.hashCode() * 31 + cardCorner.hashCode()) * 31 + clampY.hashCode()
        override fun toString(): String = "Parked(key=$key, cardCorner=$cardCorner, clampY=$clampY)" // i18n-ok: developer debug toString
    }
}

/**
 * Which side of the anchor a [TipPlacement.BesideAnchor] card prefers. [End] hangs the card off the
 * anchor's trailing edge at the **pointer's y** (its top a hair above the pointer, so it reads as
 * the row's card without covering the row); `alignX` does not apply. It does **not** flip: a card
 * too wide for the room to the right is clamped back over the row it belongs to, because a card
 * that jumped to the far side of the window (over a neighboring table) would read as that table's.
 * The default for a table's row cards ([TableRowCardOptions]): a card just right of the hovered cell
 * stays out of the way of the rows below and is still one short move to reach.
 */
enum class TipSide {
    Above,
    Below,
    End,
    /**
     * [End]'s mirror: the card's trailing edge a gap left of the anchor, at the pointer's y. For an
     * anchor on the right of a wide surface (the second column of a menu, a table's value chips —
     * [InfoChipCardOptions]), where [End] would put the card off the surface's edge; like [End] it
     * does not flip.
     */
    Start,
}

/** Horizontal alignment of a [TipPlacement.BesideAnchor] card against its anchor. */
enum class TipAlignX {
    /** Centered on the anchor. */
    Center,
    /** Left edge a hair left of the pointer's x, like [TipPlacement.UnderPointer] — for wide anchors. */
    AtPointer,
    /** Left edges aligned. */
    Start,
    /** Right edges aligned. */
    End,
}

/** Which corner of a [TipPlacement.Parked] card sits on its `corner`. */
enum class TipCorner { TopStart, TopEnd, BottomStart, BottomEnd }

/**
 * The clear space between a [TipPlacement.BesideAnchor] card and its anchor. One token, so every
 * card sits the same distance from what raised it.
 */
val TipGap: Dp = 6.dp

/**
 * How far up-left of the pointer an [TipPlacement.UnderPointer] card's corner sits. Window px, not
 * dp: it is a hit-slop, not a layout distance.
 */
internal const val TipPointerInsetPx = 12

/**
 * How far beside the pointer a [TipPlacement.FollowsPointer] card's near edge rides: twice
 * [TipPointerInsetPx]. A card riding closer reads as attached to the cursor, and the extra air is
 * what lets a latched card's settled chrome read as a change of state rather than a flicker.
 */
internal const val FollowsPointerDxPx = 24

/** How far above the pointer a [TipPlacement.FollowsPointer] card's bottom edge rides. */
internal const val FollowsPointerDyPx = 8

/**
 * The **"this card is frozen for you"** fill — the one color every hover card takes when it stops
 * following the pointer and waits to be copied from, whether it is a card drawn on a canvas or a
 * Compose card (see "latched" on [TipPlacement]). A cool off-white rather than a wash of the theme
 * primary, so the card reads as *settled* rather than *highlighted*. Change it here and every card
 * follows.
 *
 * In the dark theme the card stays on its dark surface and is only nudged toward this — a light
 * card would leave the theme's light ink unreadable.
 */
val TipFrozenFill: Color = Color(0xFFF0F4F1)

/** The frozen fill for a card whose resting fill is [base] — [TipFrozenFill] on a light surface. */
@Composable
fun tipFrozenFill(base: Color): Color =
    if (base.luminance() >= 0.5f) TipFrozenFill else lerp(base, TipFrozenFill, 0.12f)

/**
 * The **band** wash: a faint tint of the theme primary laid *over* whatever is there, for a strip
 * (a chart's time axis, say) and its card while that strip's tip is up. It cues "this hover is
 * about the strip", distinct from [TipFrozenFill], which says a card has settled.
 */
const val TipBandTintAlpha = 0.10f

/** [TipBandTintAlpha] of the theme primary, to lay over a strip or card. */
@Composable
fun tipBandTint(): Color = MaterialTheme.colorScheme.primary.copy(alpha = TipBandTintAlpha)

/** A card fill washed toward the band tint — for a Compose card whose surface is opaque. */
@Composable
fun tipBandFill(base: Color): Color = lerp(base, MaterialTheme.colorScheme.primary, TipBandTintAlpha)

/**
 * Where a card's top-left goes. Pure: every host's `PopupPositionProvider` and any canvas-drawn
 * card call this, and the tests pin the geometry here without a popup.
 *
 * @param placement    the [TipPlacement].
 * @param anchorBounds the anchor in the space of [bounds] (window px for a popup, canvas px for a
 *                     drawn card, where the anchor is the whole canvas).
 * @param pointer      the pointer **local to the anchor**; ignored by placements that do not use it.
 * @param cardSize     the measured card.
 * @param bounds       the space the card must stay inside (window or canvas size).
 * @param gapPx        [TipGap] in px, for [TipPlacement.BesideAnchor].
 * @param edgePadPx    inset from [bounds]' edges the card is kept clear of — the canvas passes a few
 *                     px so a drawn card never kisses the paper edge; popups pass 0.
 */
fun tipCardOffset(
    placement: TipPlacement,
    anchorBounds: IntRect,
    pointer: Offset,
    cardSize: IntSize,
    bounds: IntSize,
    gapPx: Int,
    edgePadPx: Int = 0,
): IntOffset {
    val minX = edgePadPx
    val minY = edgePadPx
    val maxX = (bounds.width - cardSize.width - edgePadPx).coerceAtLeast(minX)
    val maxY = (bounds.height - cardSize.height - edgePadPx).coerceAtLeast(minY)
    val px = anchorBounds.left + pointer.x.roundToInt()
    val py = anchorBounds.top + pointer.y.roundToInt()
    return when (placement) {
        TipPlacement.FollowsPointer -> {
            var x = px + FollowsPointerDxPx
            if (x + cardSize.width > bounds.width - edgePadPx) x = px - cardSize.width - FollowsPointerDxPx
            val above = py - cardSize.height - FollowsPointerDyPx
            val y = if (above >= edgePadPx) above else py + FollowsPointerDyPx
            IntOffset(x.coerceIn(minX, maxX), y.coerceIn(minY, maxY))
        }
        TipPlacement.UnderPointer -> IntOffset(
            (px - TipPointerInsetPx).coerceIn(minX, maxX),
            (py - TipPointerInsetPx).coerceIn(minY, maxY),
        )
        is TipPlacement.BesideAnchor -> {
            if (placement.side == TipSide.End) {
                return IntOffset((anchorBounds.right + gapPx).coerceIn(minX, maxX), (py - TipPointerInsetPx).coerceIn(minY, maxY))
            }
            if (placement.side == TipSide.Start) {
                return IntOffset((anchorBounds.left - gapPx - cardSize.width).coerceIn(minX, maxX), (py - TipPointerInsetPx).coerceIn(minY, maxY))
            }
            val x = when (placement.alignX) {
                TipAlignX.Center -> anchorBounds.left + (anchorBounds.width - cardSize.width) / 2
                TipAlignX.AtPointer -> px - TipPointerInsetPx
                TipAlignX.Start -> anchorBounds.left
                TipAlignX.End -> anchorBounds.right - cardSize.width
            }
            val above = anchorBounds.top - gapPx - cardSize.height
            val below = anchorBounds.bottom + gapPx
            val y = when (placement.side) {
                TipSide.Above -> if (above >= minY) above else below
                TipSide.Below -> if (below + cardSize.height <= bounds.height - edgePadPx) below else above
                TipSide.End, TipSide.Start -> error("handled above")
            }
            IntOffset(x.coerceIn(minX, maxX), y.coerceIn(minY, maxY))
        }
        is TipPlacement.Parked -> {
            val corner = placement.cornerAt()
            val x = when (placement.cardCorner) {
                TipCorner.TopStart, TipCorner.BottomStart -> corner.x
                TipCorner.TopEnd, TipCorner.BottomEnd -> corner.x - cardSize.width
            }
            val y = when (placement.cardCorner) {
                TipCorner.TopStart, TipCorner.TopEnd -> corner.y
                TipCorner.BottomStart, TipCorner.BottomEnd -> corner.y - cardSize.height
            }
            IntOffset(x.coerceIn(minX, maxX), if (placement.clampY) y.coerceIn(minY, maxY) else y)
        }
    }
}

/**
 * A [PopupPositionProvider] over [tipCardOffset] for a Compose hover card. [placement] is resolved
 * per layout from the anchor's window bounds, so a parked card can pin itself to a corner *of the
 * anchor* (a help panel over its own `?` button, a readout below its chart) without the host
 * measuring anything. [pointer] is the anchor-local pointer the host tracks (read on every layout,
 * so it need not be a remember key); [onPlaced] receives the card's window rect when a host needs
 * it ([HoverDetailPopup] uses it to judge a flight toward the card).
 */
@Composable
fun rememberTipPositionProvider(
    pointer: State<Offset>,
    vararg keys: Any?,
    onPlaced: ((IntRect) -> Unit)? = null,
    placement: (anchorBounds: IntRect) -> TipPlacement,
): PopupPositionProvider {
    val gapPx = with(LocalDensity.current) { TipGap.roundToPx() }
    // The lambdas are read through updated state so a host that recomposes with a new placement
    // (a parked corner re-measured) is honored without having to list every input in [keys].
    val placementNow by rememberUpdatedState(placement)
    val onPlacedNow by rememberUpdatedState(onPlaced)
    return remember(gapPx, *keys) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val at = tipCardOffset(placementNow(anchorBounds), anchorBounds, pointer.value, popupContentSize, windowSize, gapPx)
                onPlacedNow?.invoke(IntRect(at, popupContentSize))
                return at
            }
        }
    }
}

/**
 * Whether a pointer that moved [from] → [to] is **closing on** [card]: the move points at the
 * nearest point of the card within ±[ClosingConeCos]'s cone, or has already arrived inside it.
 *
 * The one test every latched card uses to tell "reaching for my card" from "moving on": the Compose
 * cards ([HoverFlightState]) and any canvas card a host latches. Judged against the card's
 * **nearest edge**, not its center: a card beside a table row is reached sideways, and a glide
 * straight down the rows beside it is perpendicular to that, so it reads as moving on (each row
 * then raises its own card, as a reader scanning down expects). Judged on a whole displacement,
 * never one sample, so the hand's wobble on the way cannot drop the card.
 */
fun isClosingOn(from: Offset, to: Offset, card: Rect): Boolean {
    if (to.x in card.left..card.right && to.y in card.top..card.bottom) return true
    val nearest = Offset(from.x.coerceIn(card.left, card.right), from.y.coerceIn(card.top, card.bottom))
    val aim = nearest - from
    val move = to - from
    val aimLen = aim.getDistance()
    val moveLen = move.getDistance()
    if (aimLen == 0f) return true
    if (moveLen == 0f) return false
    return (aim.x * move.x + aim.y * move.y) / (aimLen * moveLen) > ClosingConeCos
}

/**
 * Half-angle of [isClosingOn]'s cone, as a cosine: about 70°. Wide, because a hand reaching for a
 * card beside it drifts; narrow enough that a move along the card's edge (down a column of rows)
 * does not count.
 */
private const val ClosingConeCos = 0.35f

/** Displacement (px) a latched card waits for before judging the pointer's direction — hand jitter. */
const val ClosingJudgeMinPx = 4f
