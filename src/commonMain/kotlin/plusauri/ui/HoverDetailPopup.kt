package plusauri.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.toRect
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import androidx.compose.material3.MaterialTheme

/**
 * Thickness of the issues locator underbar ([issuesLocatorUnderbar]). Heavier than a hover-zone
 * hairline so it reads as a warning, not a hover hint.
 */
val IssuesLocatorUnderbarThickness: Dp = 2.dp

/**
 * Draws a 2 dp [color] underbar along the bottom edge: marks an item (a list row, a card, a toolbar
 * button) whose content has problems the reader should look at.
 */
fun Modifier.issuesLocatorUnderbar(color: Color): Modifier = drawBehind {
    val stroke = IssuesLocatorUnderbarThickness.toPx()
    drawRect(
        color = color,
        topLeft = Offset(0f, size.height - stroke),
        size = Size(size.width, stroke),
    )
}

/**
 * Delay before a sustained hover over an anchor opens its detail popup (hover-intent). Zero: a
 * card placed beside its row covers nothing the hand is near, so there is no sweep to protect and
 * the card should simply be there when the eye arrives. A host whose cards open over dense anchors
 * passes a dwell through [HoverDetailOptions.intentDelayMs].
 */
private const val HOVER_INTENT_MS = 0L

/**
 * Below the anchor, at the pointer's x. Where a [HoverDetailPopup] card opens is one of the
 * [TipPlacement]s; the three that suit this popup:
 *
 * [TipPlacement.UnderPointer] (the default): the card's corner lands a hair up-left of the
 * pointer so the card is hovered the instant it opens. Right for a wide anchor whose card must not
 * flash, and for a card the reader is meant to move *into* (copy button, selection). Its cost is
 * that the card covers the anchor under the pointer, so a click aimed at the row lands on the card
 * instead (`onClick` mirrors the action to make that survivable).
 *
 * [TipPlacement.BesideAnchor] below, at the pointer's x (this value) keeps the pointer **on the
 * anchor**: the card hangs a clear gap below the row (above it when there is no room). The reader
 * can read it and still click the row it was about to click. That matters for a list whose rows
 * are click targets: a card that steals the click leaves the reader hunting for a spot on the row
 * where no card shows, which nobody can be expected to discover. Dismissal is hover-driven on
 * anchor *or* card, so crossing the gap into the card to copy from it still works.
 *
 * [TipPlacement.Parked]: for a pair of tables read side by side. Hovering a row in the left table
 * parks the card over a corner of the right table and vice versa, so the card is always away from
 * the hand, paints in one place instead of chasing the row, and a reader learns where to look once.
 */
val BelowAnchorAtPointer: TipPlacement = TipPlacement.BesideAnchor(TipSide.Below, TipAlignX.AtPointer)

/** Off the anchor's trailing edge at the pointer's y (table row cards). */
val EndOfAnchor: TipPlacement = TipPlacement.BesideAnchor(TipSide.End)

/** Room kept between a resized card and the window's right and bottom edges. */
private val ResizeWindowMargin = 8.dp

/** How far [HoverDetailOptions.surfaceTint] moves a card's fill toward its hue. */
private const val SurfaceTintStrength = 0.08f

/** Off the anchor's leading edge at the pointer's y — a value chip in a table's last column. */
val StartOfAnchor: TipPlacement = TipPlacement.BesideAnchor(TipSide.Start)

/**
 * A Compose card that behaves like a card drawn on a chart canvas: rides beside the pointer
 * ([TipPlacement.FollowsPointer]), no dwell, re-targets the instant the pointer crosses to the
 * next anchor (the single [ChartTipOwner] swaps cards on arrival), latches after a still
 * [ChartTipCopyLatchMs] into the frozen fill, and, once latched, holds through a move toward it
 * long enough to cross a few anchors on the way ([HoverDetailOptions.flightGraceMs]). Terms as
 * defined on [TipPlacement]. The default close grace, not `0`: grace only ever runs when nothing
 * else is opening, and that is when it has to cover a hover blip on the card itself (a fast scroll
 * that overshot the top of a long breakdown). For small chips, chart segments or the marks of a
 * scatter plot. The hold needs a [HoverFlightState]: the host provides one through
 * [LocalHoverFlight].
 */
val FollowsPointerCardOptions: HoverDetailOptions = HoverDetailOptions(
    placement = TipPlacement.FollowsPointer,
    flightGraceMs = 800,
    latchDelayMs = ChartTipCopyLatchMs,
)

/**
 * How a table's row cards open: beside the name cell, no dwell, and the chart cards'
 * stop-then-reach. A still [ChartTipCopyLatchMs] latches the card (frozen tint, stops following),
 * after which a move toward it is held long enough to cross a few rows, and a move away releases
 * it. The hold needs a [HoverFlightState]: the table, or a host holding several tables, provides
 * one through [LocalHoverFlight].
 */
val TableRowCardOptions: HoverDetailOptions = HoverDetailOptions(
    placement = EndOfAnchor,
    flightGraceMs = 800,
    latchDelayMs = ChartTipCopyLatchMs,
)

/**
 * [TableRowCardOptions] for a card hung off an [InfoChip]: the same timing, but the card opens to
 * the chip's **left**. Chips usually sit at the right, in a table's last value column or in a list
 * row's cluster of action buttons, so the room, and the item's name, are to the left. A table
 * whose chip column is not last still uses this, so the card opens in the same place on every
 * table.
 */
val InfoChipCardOptions: HoverDetailOptions = TableRowCardOptions.copy(placement = StartOfAnchor)


/**
 * How a [HoverDetailPopup] card opens: where, how wide, how fast, and in what chrome. One value so
 * a host with several rows (a table) configures them once.
 *
 * @param placement    Where the card opens: see [TipPlacement] and [BelowAnchorAtPointer].
 * @param maxWidth     The card's width cap. A host that parks a card over a table usually passes
 *                     that table's width.
 * @param intentDelayMs Dwell before the card opens. The default is `0`, so the card is there when
 *                     the eye arrives and simply replaces itself as the pointer moves down the
 *                     rows. Pass a dwell for an [TipPlacement.UnderPointer] card over dense
 *                     anchors, so a quick sweep across them does not flash a card over each.
 * @param closeDelayMs Grace after the pointer leaves both anchor and card before it closes. Lets a
 *                     reader cross into the card to copy from it; `0` for a parked card, where the
 *                     next row's card takes its place at once.
 * @param frosted      Chart-tip chrome — frosted white, see-through — instead of the theme surface.
 * @param frostedAlpha Fill alpha when frosted; null keeps the shared wash.
 * @param frostedTint  Wash the frost toward a hue, very faintly: the color of the series or side the
 *                     card describes, so a parked card still says where it came from.
 * @param lightContent Compose the card's contents under the light color scheme regardless of the
 *                     app theme; required with [frosted], whose surface is white by construction.
 */
data class HoverDetailOptions(
    val placement: TipPlacement = TipPlacement.UnderPointer,
    val maxWidth: Dp = 480.dp,
    val intentDelayMs: Long = HOVER_INTENT_MS,
    val closeDelayMs: Long = HOVER_CLOSE_MS,
    val frosted: Boolean = false,
    val frostedAlpha: Float? = null,
    val frostedTint: Color? = null,
    val lightContent: Boolean = false,
    /**
     * Wash the standard (solid, theme) card toward a hue, faintly — [SurfaceTintStrength] of the
     * way. For two tables read side by side, each table's own color on its cards: the side is
     * read at a glance, and the card is otherwise the standard one. For the frosted card, see
     * [frostedTint].
     */
    val surfaceTint: Color? = null,
    /**
     * For a card the pointer is not inside (parked, or riding beside it): how long it stays open
     * after the pointer leaves the anchor **heading toward it**, so a reader can cross other rows
     * (or another table) to copy from or scroll the card. During that flight the rows crossed do
     * not raise their own cards; see [HoverFlightState]. `0` (default) gives no such grace; a pair
     * of side-by-side tables might pass a full second. The grace is for **arriving** only: once the
     * pointer has been on the card, leaving it closes after the plain [closeDelayMs].
     */
    val flightGraceMs: Long = 0,
    /**
     * For a card the pointer is not inside and that follows it (beside the pointer, or beside the
     * anchor at the pointer's y — not parked, not under the pointer): dwell with the pointer still
     * before the card **latches** — repaints in the frozen tint, stops following, and on the next
     * move off the anchor in *any* direction holds for [flightGraceMs] so the reader can get into
     * it to copy; a move away releases it. The canvas cards' latch, in Compose. `0` (default) never
     * latches; [TableRowCardOptions] and [FollowsPointerCardOptions] pass [ChartTipCopyLatchMs].
     */
    val latchDelayMs: Long = 0,
    /**
     * The reader can drag the card's bottom-right corner to resize it. **On by default**: every card
     * through this host is a content card — the one-line hints go through [HoverTooltip] — and a
     * table or breakdown is exactly what a reader wants wider or taller than the default caps. The
     * card hugs its content until dragged; the grip shows only while the pointer is on it.
     * Pass `false` for a card whose size is part of its meaning.
     */
    val resizable: Boolean = true,
    /**
     * The [HoverCardSizes] slot a resize is remembered in. Null (default) is this options value
     * itself, so one slot per card kind ([TableRowCardOptions], [InfoChipCardOptions], …). A host
     * whose cards differ in shape from others of the same kind names its own (a table might key by
     * its own identity).
     */
    val sizeKey: Any? = null,
)

/**
 * The shared state behind [HoverDetailOptions.flightGraceMs]: **one** pointer "in flight" toward
 * a card (see [TipPlacement] for the term), for every [HoverDetailPopup] under the provider.
 *
 * The problem it solves is geometric. With two tables side by side, a left-table row's card parks
 * over the right table and a right-table row's over the left, so reaching either means crossing
 * the *other* table's rows, each of which would, with no timers, instantly raise its own card and
 * take the one the reader was heading for. So when a pointer leaves a row moving toward a parked
 * card (horizontal motion, toward the card's side; the next row down is a vertical move and does
 * not count), the departing popup declares a flight for its placement: it stays open, and rows
 * whose card would park **elsewhere** hold their fire. Rows whose card parks in the *same* corner
 * still replace it at once: those are the reader's own side, being read. The flight ends when the
 * pointer reaches the card (the card's own hover keeps it open from there), when it lands back on
 * a row of the same side, or when the grace runs out, at which point whatever row the pointer is
 * on opens normally.
 *
 * A latched card beside its anchor declares a flight too, keyed by its own popup. The rows the
 * pointer crosses then hold only while [headingOk] says the pointer is still reaching for it.
 *
 * The host provides one through [LocalHoverFlight] (a table, or a host holding several tables).
 * Unprovided, there is no flight, and a card closes as soon as its anchor is left.
 */
class HoverFlightState {
    /**
     * Whose card the pointer is heading for, or null when nothing is in flight. A parked card's
     * key is its placement (every row parking in the same corner shares one card slot); any other
     * card's key is its own popup, so only the card actually in flight is held.
     */
    var toward: Any? by mutableStateOf(null)

    /** Where the card in flight is (window px), so a crossed anchor can judge the pointer's intent. */
    var towardRect: IntRect? by mutableStateOf(null)

    /**
     * Whether the pointer is still reaching for the card in flight, judged by [isClosingOn] over
     * the whole move since the last judgement ([judgedFrom]) — never from one anchor's own last
     * sample, which on entering a row is a jump from wherever the pointer sat on its last visit.
     * Every anchor the pointer crosses reports to [observe]; the rows it crosses hold their fire
     * while this stays true.
     */
    var headingOk by mutableStateOf(true)
    private var judgedFrom: Offset? = null

    /** A card claims the flight — on latch, and again when the pointer leaves its anchor. */
    fun declare(key: Any, card: IntRect, at: Offset) {
        toward = key
        towardRect = card
        judgedFrom = at
        headingOk = true
    }

    /** The pointer is at [at] (window px). Judged once it has moved [ClosingJudgeMinPx] since the last judgement. */
    fun observe(at: Offset) {
        val card = towardRect ?: return
        val from = judgedFrom ?: run { judgedFrom = at; return }
        if ((at - from).getDistance() < ClosingJudgeMinPx) return
        headingOk = isClosingOn(from, at, card.toRect())
        judgedFrom = at
    }

    fun clear() {
        toward = null
        towardRect = null
        judgedFrom = null
        headingOk = true
    }
}

val LocalHoverFlight = compositionLocalOf<HoverFlightState?> { null }

/**
 * **Esc drops a latched card** (see [TipPlacement] for "latched"): once a card has tinted and the
 * reader has gone to it, Esc closes it. It is the way out of a card that is now in the way, without
 * steering the pointer off it. One slot, process-wide, because only one card is ever up
 * ([ChartTipOwner]).
 *
 * A latched card [hold]s the slot with how to close itself and [release]s it when it unlatches or
 * closes. [HoverDetailPopup] does this for its own cards, and a host that draws latching cards on a
 * canvas can do the same.
 *
 * **The host wires the key.** The kit installs no key handler: the host's window-level key handler
 * calls [escape] on Esc and consumes the key **only** when it returns true, so Esc keeps every
 * other meaning (closing an editor, a dialog) when no card is up. Without that call, Esc does not
 * close a card.
 */
object LatchedTipEscape {
    private var owner: Any? = null
    private var dismiss: (() -> Unit)? = null

    fun hold(owner: Any, dismiss: () -> Unit) {
        this.owner = owner
        this.dismiss = dismiss
    }

    fun release(owner: Any) {
        if (this.owner === owner) {
            this.owner = null
            dismiss = null
        }
    }

    /** Close the latched card, if one is up. True when one was — the caller consumes the key. */
    fun escape(): Boolean {
        val close = dismiss ?: return false
        owner = null
        dismiss = null
        close()
        return true
    }
}

/**
 * Whether a pointer leaving a row at velocity [velocity] (window px per event) from [exit] is
 * heading for a card at [card]. With [horizontalOnly] (the default) the move must be mostly
 * horizontal and toward the card's center x; otherwise any move that closes on the card's center
 * counts. False when the pointer is not moving. Pure, for the test.
 */
fun isHeadingToward(exit: Offset, velocity: Offset, card: IntRect, horizontalOnly: Boolean = true): Boolean {
    if (velocity == Offset.Zero) return false
    val cardCenterX = (card.left + card.right) / 2f
    if (horizontalOnly) {
        val horizontal = kotlin.math.abs(velocity.x) > kotlin.math.abs(velocity.y)
        if (!horizontal) return false
        return (cardCenterX - exit.x) * velocity.x > 0f
    }
    // Any direction that closes on the card: a card riding above-right of the pointer is reached
    // diagonally, and a strictly horizontal test would never see that as a flight.
    val cardCenterY = (card.top + card.bottom) / 2f
    return (cardCenterX - exit.x) * velocity.x + (cardCenterY - exit.y) * velocity.y > 0f
}

/** A latched card ignores pointer moves shorter than this (px) when deciding "away" — hand jitter. */
private const val LatchReleaseJitterPx = 3f

/**
 * Grace period after the pointer leaves both anchor and popup before the popup closes: long enough
 * to cross the gap from a row into a card placed beside it.
 */
private const val HOVER_CLOSE_MS = 400L

/**
 * The **resting** state of hover-detail surfaces: `true` (the default) means details show on
 * hover; `false` means they stay hidden until deliberately revealed. **The host provides it**,
 * typically from a persisted user setting behind a toolbar toggle. Unprovided it is `true`, so a
 * host that never thinks about it (a demo, a test, a non-desktop target) gets "hover shows the
 * detail".
 *
 * Combined with [LocalAltPressed] via **XOR** (see [hoverDetailsEnabled]): holding Alt transiently
 * flips the resting state. It *suppresses* while the default is on (the presenting case: point and
 * move without popups erupting) and *peeks* while the default is off. This gates the rich
 * [HoverDetailPopup] cards and the rich `interactive` [HoverTooltip] cards, and a host can gate its
 * own canvas readouts or hover highlights on [hoverDetailsEnabled] too.
 *
 * The must-have tooltips stay out of it and are deliberately never suppressed: text-elision
 * ([OverflowTooltipText]) and one-line [HoverTooltip] hints. A truncated label with no way to read
 * it is a dead end rather than a distraction, and a one-word "what does this button do" is not what
 * interrupts a presentation; the dense explanatory cards are, which is why only those are gated.
 */
val LocalHoverDetailsDefault = compositionLocalOf { true }

/**
 * Whether the Alt/Option modifier is currently held. **The host provides it**, from a window-level
 * key listener; the kit tracks no keys itself. `false` when unprovided (non-desktop targets,
 * tests). Modifiers are
 * *level-triggered* — the OS holds the bit for the duration of the press with no auto-repeat — so this
 * reads as a steady flag, not an event stream (unlike a character key such as Space, which would flood
 * repeat events). Read together with [LocalHoverDetailsDefault] via [hoverDetailsEnabled].
 */
val LocalAltPressed = compositionLocalOf { false }

/**
 * Live whether rich hover details should show right now: [LocalHoverDetailsDefault] XOR
 * [LocalAltPressed]. Read by [HoverDetailPopup] and interactive [HoverTooltip] cards; a host can
 * read it for its own canvas readouts and hover highlights, so one Alt press or one toolbar
 * default covers every dense hover surface that would otherwise interrupt a presentation.
 */
@Composable
fun hoverDetailsEnabled(): Boolean =
    LocalHoverDetailsDefault.current != LocalAltPressed.current

/**
 * Wraps an anchor [content] so that hovering it (after [HoverDetailOptions.intentDelayMs], zero
 * by default) opens a persistent detail [Popup] carrying [detail] in a [TooltipCard]. Copy-out is
 * via a copy button in [detail] (a [CopyableTooltipBody] has one); drag-selection inside the card
 * is disabled because the popup lives in a separate layout hierarchy from any enclosing `SelectionContainer`
 * (e.g. a table's), and letting the outer selection span that boundary crashes with "layouts are not
 * part of the same hierarchy".
 *
 * Where the card opens is [HoverDetailOptions.placement]. The default, [TipPlacement.UnderPointer],
 * puts the card's top-left a hair up-left of the pointer rather than at the anchor's corner: a wide
 * anchor (like a full-width table row) would otherwise place the card away from a mid-row cursor,
 * so neither the anchor nor the card would be hovered and the popup would flash open/closed. Placing
 * it under the cursor guarantees the card is hovered the moment it opens. The other placements
 * keep the pointer on the anchor instead; see [BelowAnchorAtPointer]. Hover = peek (persists); the
 * anchor keeps whatever click behavior it already has.
 *
 * Dismissal is driven by **hover state** of the anchor and the popup (with a short grace delay),
 * not by raw pointer-exit or click-outside — both of which fight nested popups. In particular a copy
 * button with a dropdown menu is a sibling popup; while it is open the pointer is over neither the
 * anchor nor this popup, so such a copy button reports through [LocalPopupDismissSuppressor] to keep
 * us open until a selection is made. `dismissOnClickOutside` is off for the same reason (a click in
 * the dropdown would otherwise read as "outside" and tear everything down before the copy runs).
 *
 * The card surface also resets [LocalTopEndOverlayInset] to `0.dp`. CompositionLocals flow into
 * Popup content, so without that reset a parent that overlays a control on its top-end corner would
 * left-shift any top-end control on this card (a title-row copy button) as if the parent's overlay
 * still covered the corner, which it does not on a separate popup surface.
 *
 * @param detail   The rich, copyable popup body, wrapped here in a [TooltipCard] with 10 dp of
 *                 padding.
 * @param modifier Applied to the anchor wrapper; hover is detected over its bounds. Scope it to the
 *                 element whose meaning needs explaining, not a whole row of unrelated controls.
 * @param onClick  When non-null, the whole card (its padded border included) is clickable and runs
 *                 this. Use it to mirror the anchor's own click action, so a click aimed at the
 *                 anchor still lands when the card has opened over it.
 * @param options  Placement, width, timing and chrome — see [HoverDetailOptions].
 * @param enabled  When false the card cannot open and closes if open — for an anchor that hosts
 *                 its own hit-test (a scatter of marks) and only sometimes has something to show.
 * @param content  The anchor.
 */
@Composable
fun HoverDetailPopup(
    detail: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    options: HoverDetailOptions = HoverDetailOptions(),
    /**
     * Card fill. Defaults to the plain tooltip surface. Pass a tonal token (e.g.
     * `surfaceContainerHigh`) when the card is a *document* rather than a tip — an About panel or a
     * canonical detail card reads as system chrome on a tinted surface and as a tooltip on white.
     */
    containerColor: Color = MaterialTheme.colorScheme.surface,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    // A corner-grip drag is under way ([HoverDetailOptions.resizable]): the pointer may leave the
    // card while growing it, so this holds the card open the way an open copy menu does.
    var resizing by remember { mutableStateOf(false) }
    // One card at a time ([ChartTipOwner]): opening claims the token; losing it (another card
    // opened) closes at once.
    val owner = LocalChartTipOwner.current
    val token = remember { Any() }
    val held = owner.holds(token)
    LaunchedEffect(held) { if (!held && visible) visible = false }
    LaunchedEffect(visible) { if (!visible) owner.release(token) }
    val anchorSource = remember { MutableInteractionSource() }
    val anchorHovered by anchorSource.collectIsHoveredAsState()
    val popupSource = remember { MutableInteractionSource() }
    val popupHovered by popupSource.collectIsHoveredAsState()

    // The resting default flipped by a held Alt (XOR): so `false` = cards suppressed right now. When the
    // default is on, holding Alt suppresses (present without cards); when off, holding Alt peeks.
    val detailsEnabled = hoverDetailsEnabled()

    // Latest pointer position within the anchor (local coords), used to place the card under the
    // cursor. It stops updating once the cursor moves onto the card (the anchor no longer sees moves),
    // so the card doesn't jitter while it's open.
    val pointerPos = remember { mutableStateOf(Offset.Zero) }
    // The last move within the anchor, for the exit direction; the anchor's window bounds and the
    // card's last computed rect, so "toward the card" can be judged in one coordinate space.
    val lastDelta = remember { mutableStateOf(Offset.Zero) }
    val anchorInWindow = remember { mutableStateOf<IntRect?>(null) }
    val cardInWindow = remember { mutableStateOf<IntRect?>(null) }
    val flight = LocalHoverFlight.current
    // Flight grace applies to any card the pointer is *not* inside — parked, or riding beside the
    // pointer — since those are the ones a deliberate move toward can be stolen from by the
    // anchors crossed on the way. An under-pointer card is already hovered when it opens.
    val parked = options.placement is TipPlacement.Parked
    val flightGrace = options.flightGraceMs.takeIf { it > 0 && options.placement !is TipPlacement.UnderPointer && flight != null }
    val flightKey: Any = if (parked) options.placement else token
    // True while a flight toward *another* card is under way and this anchor should hold its fire.
    // A parked card's rows always hold (reaching a card parked over the other table means crossing
    // that table's rows). For a card beside its anchor, the rows crossed hold while the pointer is
    // still reaching for it ([HoverFlightState.headingOk]): a slow glide down a table after a dwell
    // latched row A's card must not leave rows B, C, D silent for the length of the grace (it would
    // read as "only some cards show"), but a reach for A's card that crosses B must not raise B's.
    val foreignFlight = flight?.toward.let { it != null && it != flightKey }
    val heldByFlight = foreignFlight && (parked || flight?.headingOk == true)
    DisposableEffect(token) {
        onDispose {
            owner.release(token)
            // A popup leaving composition mid-flight (its chart re-laid out, its table scrolled
            // away) releases the flight the same way, for the same reason as the `finally` below.
            if (flight != null && flight.toward == flightKey) flight.clear()
        }
    }
    // Latched: the pointer sat still on the anchor for [HoverDetailOptions.latchDelayMs] with the
    // card up. Re-keyed on the pointer so any move restarts the dwell; cleared when the card goes.
    var latched by remember { mutableStateOf(false) }
    // [enabled] going false is an anchor with its own hit-test (a scatter of marks) saying "the
    // pointer left the mark" — the same thing as leaving the anchor, one level down. So it takes the
    // same path: an unlatched card closes at once (it was following the pointer, and nothing is
    // under it now), and a **latched** card stays for the close/flight logic below, so a reader who
    // settled on a mark can travel off it to the card. Closing unconditionally here would make a
    // latched card vanish on the first move toward it.
    LaunchedEffect(enabled) { if (!enabled && visible && !latched) visible = false }
    val anchorActive = anchorHovered && enabled
    val latchDelay = options.latchDelayMs.takeIf { it > 0 && options.placement !is TipPlacement.UnderPointer && !parked }
    // The pointer the card is placed from. While latched it is **frozen** at the latch point, so
    // the card stays where the reader saw it settle and a move toward it is a move toward a fixed
    // target rather than a chase. Moves away drop the latch (see the tracker below) and it follows again.
    val frozenPointer = remember { mutableStateOf(Offset.Zero) }
    val placementPointer = remember { derivedStateOf { if (latched) frozenPointer.value else pointerPos.value } }
    LaunchedEffect(visible, latchDelay, pointerPos.value) {
        if (!visible || latchDelay == null) { latched = false; return@LaunchedEffect }
        if (!latched) {
            delay(latchDelay)
            frozenPointer.value = pointerPos.value
            latched = true
            // The latch claims the flight at once, while the pointer is still on the anchor: the
            // reach then starts under a declared flight, so the first row it crosses holds even if
            // it is entered before this anchor's exit is processed.
            val card = cardInWindow.value
            val anchor = anchorInWindow.value
            if (flightGrace != null && card != null && anchor != null) {
                flight!!.declare(flightKey, card, Offset(anchor.left + pointerPos.value.x, anchor.top + pointerPos.value.y))
            }
        }
    }
    // A card that closes gives up any flight it declared.
    LaunchedEffect(visible) { if (!visible && flight != null && flight.toward == flightKey) flight.clear() }
    // Latched and up: Esc closes it ([LatchedTipEscape]).
    val escapable = latched && visible
    DisposableEffect(escapable) {
        if (escapable) {
            LatchedTipEscape.hold(token) {
                visible = false
                latched = false
                if (flight != null && flight.toward == flightKey) flight.clear()
            }
        }
        onDispose { LatchedTipEscape.release(token) }
    }

    // Open after a sustained hover, but only while details are enabled; re-keying on `anchorHovered`
    // (or `detailsEnabled`) cancels the pending open if the cursor leaves — or Alt is pressed to
    // suppress — first, so a quick sweep across the anchor doesn't flash the popup.
    LaunchedEffect(anchorHovered, detailsEnabled, heldByFlight, enabled) {
        if (anchorHovered && detailsEnabled && !heldByFlight && enabled) {
            if (options.intentDelayMs > 0) delay(options.intentDelayMs)
            visible = true
            owner.claim(token)
            // Landing here ends any flight: a row of the same side because the reader is reading
            // rows again and this card has replaced the one in flight, and a foreign one because
            // opening was allowed only when the pointer was not heading for that card anyway.
            if (flight != null) flight.clear()
        }
    }

    // Close when suppressed (Alt / default-off) — immediately, so the card gets out of the way the
    // instant Alt goes down — or, when still enabled, once the pointer is over neither anchor nor popup
    // and no copy menu is open (after the usual grace). Any re-key cancels a pending grace close.
    // Whether the pointer has been on the card since it opened. Leaving the card is the reader
    // saying "done" — the flight grace is for *arriving* at a parked card, not departing it, and
    // the stale last-move-in-the-anchor would otherwise still read as "heading toward the card"
    // and hold it up for the whole grace after the pointer has already left.
    val visitedCard = remember { mutableStateOf(false) }
    LaunchedEffect(visible) { if (!visible) visitedCard.value = false }
    LaunchedEffect(visible, anchorActive, popupHovered, menuOpen, resizing, detailsEnabled) {
        if (!visible) return@LaunchedEffect
        if (!detailsEnabled) {
            visible = false
            if (flight != null && flight.toward == flightKey) flight.clear()
        } else if (popupHovered) {
            // Reached the card: the flight is over, the card's own hover holds it open from here.
            visitedCard.value = true
            if (flight != null && flight.toward == flightKey) flight.clear()
        } else if (!anchorActive && !menuOpen && !resizing) {
            if (visitedCard.value) {
                // Left the card itself: no flight to wait out, only the ordinary close grace,
                // which a host may set to zero for a parked card so it is gone the moment the
                // reader leaves.
                if (options.closeDelayMs > 0) delay(options.closeDelayMs)
                visible = false
                return@LaunchedEffect
            }
            // Leaving a parked card's row toward the card: declare the flight and wait it out, so
            // the rows being crossed hold their fire and the card is still there on arrival.
            val card = cardInWindow.value
            val anchor = anchorInWindow.value
            // A latched card is held whichever way the pointer leaves: the dwell said "I want this
            // one", and the settled chrome promised it would wait.
            val toward = flightGrace != null && card != null && anchor != null &&
                (latched || isHeadingToward(Offset(anchor.left + pointerPos.value.x, anchor.top + pointerPos.value.y), lastDelta.value, card, horizontalOnly = parked))
            if (toward) {
                flight!!.declare(flightKey, card!!, Offset(anchor!!.left + pointerPos.value.x, anchor.top + pointerPos.value.y))
                try {
                    delay(flightGrace!!)
                    // Reaching here means the pointer did not arrive (arrival re-keys on
                    // `popupHovered` and cancels this), and the card goes the way it would have.
                    visible = false
                } finally {
                    // Whatever ends the wait — expiry, arrival, or a cancellation from elsewhere
                    // (another card claimed the [ChartTipOwner] and closed us, Alt went down, the
                    // anchor was re-entered) — a flight we declared must not outlive it. Left set,
                    // every other anchor under this provider holds its fire forever, and the whole
                    // surface goes dead to the mouse until this one anchor is hovered again.
                    if (flight.toward == flightKey) flight.clear()
                }
            } else {
                if (options.closeDelayMs > 0) delay(options.closeDelayMs)
                visible = false
            }
        }
    }

    val placement = options.placement
    val tipPositionProvider = rememberTipPositionProvider(
        placementPointer,
        placement,
        onPlaced = { cardInWindow.value = it },
    ) { placement }
    // Resize: the card's top-left is **pinned** from the first drag until it closes. Placement is
    // computed from the card's size, so without the pin a card that opens to the left of its anchor
    // or flips above it would slide away from the grip as it grew — the corner under the hand must
    // be the one that moves. The window size is captured for the drag's limit.
    val pinnedAt = remember { mutableStateOf<IntOffset?>(null) }
    val windowPx = remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(visible) { if (!visible) { pinnedAt.value = null; resizing = false } }
    val positionProvider = remember(tipPositionProvider) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                windowPx.value = windowSize
                val pin = pinnedAt.value
                    ?: return tipPositionProvider.calculatePosition(anchorBounds, windowSize, layoutDirection, popupContentSize)
                cardInWindow.value = IntRect(pin, popupContentSize)
                return pin
            }
        }
    }
    // One slot per host ([HoverCardSizes]). A parked placement is keyed by kind, not by its corner,
    // which for the fixed-corner form is a pixel position that moves with the layout.
    val sizeKey: Any = options.sizeKey
        ?: options.copy(placement = TipPlacement.UnderPointer) to
            options.placement.let { if (it is TipPlacement.Parked) TipPlacement.Parked::class else it }
    val density = LocalDensity.current
    val resize = if (!options.resizable) null else TooltipCardResize(
        size = HoverCardSizes[sizeKey],
        showGrip = popupHovered || resizing,
        maxSize = run {
            // The room right of and below the card's corner, less a margin for its border.
            val corner = pinnedAt.value ?: cardInWindow.value?.topLeft ?: IntOffset.Zero
            with(density) {
                DpSize(
                    (windowPx.value.width - corner.x).toDp() - ResizeWindowMargin,
                    (windowPx.value.height - corner.y).toDp() - ResizeWindowMargin,
                )
            }
        },
        onResizing = { on ->
            if (on && pinnedAt.value == null) pinnedAt.value = cardInWindow.value?.topLeft
            resizing = on
        },
        onResize = { HoverCardSizes[sizeKey] = it },
    )

    // Column (not Box) so a multi-line anchor stacks vertically rather than overlapping; the
    // hoverable bounds become the popup anchor. A read-only pointer tracker (Initial pass, never
    // consumes) records the cursor for placement without disturbing the anchor's own click/hover.
    Column(
        modifier = modifier
            .hoverable(anchorSource)
            .onGloballyPositioned { anchorInWindow.value = it.boundsInWindow().roundToIntRect() }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        event.changes.lastOrNull()
                            ?.let { change ->
                                // An Enter is a jump from wherever the pointer sat on its last visit,
                                // not a move: measuring a direction from it would read a reach for a
                                // card as a turn away, and the crossed row would raise its own card.
                                val delta = if (event.type == PointerEventType.Enter) Offset.Zero
                                            else change.position - pointerPos.value
                                // A real move only; the release/enter samples at the same point
                                // must not zero the direction the pointer actually left with.
                                if (delta != Offset.Zero) lastDelta.value = delta
                                pointerPos.value = change.position
                                // Every anchor reports the pointer to a flight under way, so the
                                // reach is judged over the whole path, rows crossed included.
                                val anchorNow = anchorInWindow.value
                                if (flight?.toward != null && anchorNow != null) {
                                    flight.observe(Offset(anchorNow.left + change.position.x, anchorNow.top + change.position.y))
                                }
                                // Latched and moving: toward the card is the reach — the card
                                // holds still and the move is not a request for a new one. Away
                                // from it (past a jitter threshold) is "never mind": drop the
                                // latch and let the card follow again.
                                if (latched && delta.getDistance() >= LatchReleaseJitterPx) {
                                    val card = cardInWindow.value
                                    val anchor = anchorInWindow.value
                                    val toward = card != null && anchor != null && isHeadingToward(
                                        Offset(anchor.left + change.position.x, anchor.top + change.position.y),
                                        delta, card, horizontalOnly = false,
                                    )
                                    if (!toward) {
                                        latched = false
                                        if (flight != null && flight.toward == flightKey) flight.clear()
                                    }
                                }
                            }
                    }
                }
            },
    ) {
        content()
        if (visible) {
            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = { visible = false },
                // A focusable popup takes the pointer events outside it on desktop, which is fine
                // while the card sits under the cursor (the click lands on the card and `onClick`
                // mirrors the row's) and exactly wrong for a card placed beside the pointer, whose
                // whole point is that the row under the pointer still takes the click.
                properties = PopupProperties(
                    focusable = placement is TipPlacement.UnderPointer,
                    dismissOnClickOutside = false,
                ),
            ) {
                // Popup is a separate surface: clear the top-end overlay inset inherited from an
                // enclosing container, so a title-row control on the card sits flush trailing (the
                // parent's overlaid controls do not cover this card). Also report copy-menu open
                // state so hover-dismiss does not tear the card down mid-pick.
                CompositionLocalProvider(
                    LocalPopupDismissSuppressor provides { open -> menuOpen = open },
                    LocalTopEndOverlayInset provides 0.dp,
                ) {
                    TooltipCard(
                        // Latched: the frozen fill ([tipFrozenFill]), which says "this one is
                        // settled; come and get it".
                        containerColor = (options.surfaceTint?.let { lerp(containerColor, it, SurfaceTintStrength) }
                            ?: containerColor).let { if (latched) tipFrozenFill(it) else it },
                        // The whole card (border included) is clickable when [onClick] is set, so a
                        // click meant for the anchor lands even on the card's padded edge.
                        modifier = Modifier
                            .hoverable(popupSource)
                            .then(
                                if (onClick != null) {
                                    Modifier.pointerHoverIcon(PointerIcon.Hand).clickable { onClick() }
                                } else {
                                    Modifier
                                },
                            ),
                        maxWidth = options.maxWidth,
                        frosted = options.frosted,
                        frostedAlpha = options.frostedAlpha,
                        frostedTint = options.frostedTint,
                        resize = resize,
                    ) {
                        // A frosted card is white by construction; its ink must follow the surface
                        // it is on, not the app theme ([HoverDetailOptions.lightContent]).
                        // A card opened from a locally scaled region (a zoomed-out strip) draws at
                        // the app's own scale, not the region's ([LocalUnscaledDensity]).
                        val unscaled = LocalUnscaledDensity.current
                        val body: @Composable () -> Unit = {
                            if (unscaled != null) {
                                CompositionLocalProvider(LocalDensity provides unscaled) {
                                    Box(modifier = Modifier.padding(10.dp)) { detail() }
                                }
                            } else {
                                Box(modifier = Modifier.padding(10.dp)) { detail() }
                            }
                        }
                        if (options.lightContent) MaterialTheme(colorScheme = PlusauriLightColorScheme) { body() } else body()
                    }
                }
            }
        }
    }
}
