package tutil.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

/**
 * Shared hover-intent delay for [HoverTooltip]: brief sustained hover before a card opens, so a
 * sweep across dense targets (slider triangles, toolbar toggles) does not erupt a tip. Same idea as
 * [HoverDetailPopup]'s 300 ms intent delay; this family is 200 ms because the cards are smaller.
 *
 * Pass a multiple at a call site that needs more dwell (e.g. a long tip sitting over an animation).
 */
const val HoverTooltipIntentMs = 200L

/**
 * Grace after the pointer leaves both anchor and popup before closing.
 *
 * Critical on desktop: opening a [Popup] is a separate OS window and fires a **spurious**
 * [androidx.compose.ui.input.pointer.PointerEventType.Exit] on the main window even though the
 * cursor never moved (see `doc/technical-choices/help-icon-tooltip.md`). Synchronous dismiss on
 * Exit causes the classic tooltip flutter loop. The grace swallows that artifact; a genuine leave
 * still closes after the delay.
 */
private const val HoverTooltipCloseMs = 300L

/**
 * After a place-below interactive card opens, ignore unhover for this long so the desktop
 * Popup-creation Exit cannot immediately close and re-open the card (slow flash loop).
 * Long enough to outlive the spurious Exit; short enough that a real leave still dismisses.
 */
private const val PlaceBelowOpenStickyMs = 500L

/**
 * Inner padding for Family C one-line / short-prose hints ([HoverTooltip] string overload,
 * [OverflowTooltipText]). Rich Family B cards pad themselves. 12/8, not 8/4 — a sentence
 * at 8/4 reads as text pressed against the card edge.
 */
val HoverTooltipHintPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)

/**
 * Wraps [content] so hovering it reveals [text] in a small tooltip above (flipping below when there's
 * no room). The reusable answer for "this little icon button needs a word of explanation": unlike
 * [OverflowTooltipText] — which only shows when a label is *clipped* — this always shows on hover, for
 * affordances whose meaning isn't self-evident from the icon alone.
 *
 * Uses a plain hover-driven [Popup] (the same approach as [OverflowTooltipText] / [HelpIcon]) rather
 * than Material3's `TooltipBox`, which the app avoids for its measurement quirks and version churn.
 *
 * Show/hide follows the anti-flutter model shared with [HoverDetailPopup]: hover-intent delay, close
 * grace for OS-spurious exits, and [androidx.compose.foundation.hoverable] rather than raw Enter/Exit.
 *
 * @param text     Tooltip contents. Blank ⇒ no tooltip (the wrapper is then just [content]).
 * @param modifier Applied to the wrapping [Box] (the hover anchor).
 * @param maxWidth Wrap width for longer, explanatory tooltips.
 * @param placeBelow When true the tooltip prefers *below* the anchor (flipping above only when there's
 *                 no room) — use when what sits above the anchor (e.g. a slider bar) must stay visible.
 *                 Default prefers above. Shorthand for `placement = BesideAnchor(Below)`.
 * @param placement Where the card goes — see [TipPlacement]. Null derives it from [placeBelow]:
 *                 beside the anchor, above or below, centered. The gap is [TipGap].
 * @param intentDelayMs Dwell before the tip opens. Default is the shared [HoverTooltipIntentMs]
 *                 (200 ms). Raise it when the tip would cover something the hover itself just
 *                 started (a chart animation, a toggle settling).
 * @param frosted When true, uses the canvas chart-tip chrome (frosted white, see-through) instead
 *                 of the opaque theme surface — for a long hint that would otherwise hide a chart
 *                 animation underneath.
 * @param frostedAlpha Fill alpha when [frosted] is true. Null keeps the shared [ChartTipFill]
 *                 wash (0.88). Lower it at a call site that must show more of what sits under the card.
 */
@Composable
fun HoverTooltip(
    text: String,
    modifier: Modifier = Modifier,
    maxWidth: Dp = 240.dp,
    placeBelow: Boolean = false,
    placement: TipPlacement? = null,
    intentDelayMs: Long = HoverTooltipIntentMs,
    frosted: Boolean = false,
    frostedAlpha: Float? = null,
    content: @Composable () -> Unit,
) {
    HoverTooltip(
        modifier = modifier,
        maxWidth = maxWidth,
        placeBelow = placeBelow,
        placement = placement,
        enabled = text.isNotBlank(),
        intentDelayMs = intentDelayMs,
        frosted = frosted,
        frostedAlpha = frostedAlpha,
        tooltip = {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = if (frosted) ChartTipInk else Color.Unspecified,
                ),
                modifier = Modifier.padding(HoverTooltipHintPadding),
            )
        },
        content = content,
    )
}

/**
 * Rich-content sibling of the string [HoverTooltip]: hovering [content] reveals an arbitrary
 * [tooltip] composable in the same [TooltipCard] (positioned identically — above, flipping below,
 * horizontally clamped). Use for multi-line breakdowns (e.g. a category's line items) where a single
 * [text] string won't do. The [tooltip] renders inside the card chrome and a [maxWidth] wrap with the
 * card's content color already applied; supply your own inner padding.
 *
 * @param enabled When false the wrapper is just [content] (no hover card) — lets a caller gate the
 *                tooltip without restructuring, mirroring the string overload's blank-text behavior.
 * @param interactive When true, the card stays while the pointer is over it (copy button / selection),
 *                **and** it obeys the global Alt / hover-details gate ([hoverDetailsEnabled]) — see
 *                the `gated` note in the body for why rich cards suppress but hints do not.
 * @param placement Where the card goes — see [TipPlacement]. Null derives it: beside the anchor,
 *                below when [placeBelow] else above, centered. An interactive card is **not** placed
 *                under the pointer by default any more — that put a 420 dp year card over the axis
 *                being swept (`hover-tip-placement.md` §4); pass [TipPlacement.UnderPointer] to ask
 *                for it. Beside-anchor interactive cards get the open-sticky guard against the
 *                desktop Popup-creation Exit.
 * @param selectable When true (typically with [interactive]), [TooltipCard] does not wrap content in
 *                [DisableSelection], so [CopyableTooltipBody] can offer select/paste.
 * @param intentDelayMs Dwell before the tip opens. Default is the shared [HoverTooltipIntentMs].
 * @param frosted Frosted chart-tip chrome ([ChartTipFill]) instead of the opaque theme surface.
 * @param frostedAlpha Fill alpha when [frosted] is true. Null keeps [ChartTipFill].
 */
@Composable
fun HoverTooltip(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 240.dp,
    placeBelow: Boolean = false,
    placement: TipPlacement? = null,
    enabled: Boolean = true,
    interactive: Boolean = false,
    selectable: Boolean = interactive,
    intentDelayMs: Long = HoverTooltipIntentMs,
    frosted: Boolean = false,
    frostedAlpha: Float? = null,
    /** Card fill when not [frosted]; null keeps the theme surface. A band's card passes its wash. */
    containerColor: Color? = null,
    /**
     * For an [interactive] card: dwell with the pointer still, card up, before it **latches** into
     * the frozen fill ([TipFrozenFill]) — the same "settled, come and copy" cue every other hover
     * card gives. A beside-anchor card never moves, so the latch here is the cue and the promise
     * (it stays latched until it closes), not a change of placement. Default: the charts' own
     * [ChartTipCopyLatchMs] for interactive cards, none for hints.
     */
    latchDelayMs: Long = if (interactive) ChartTipCopyLatchMs else 0L,
    /** Told when the card opens / closes, for a host that tints its strip while a card is up. */
    onVisibleChange: ((Boolean) -> Unit)? = null,
    tooltip: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    // Rich interactive cards are one-at-a-time app-wide (see [ChartTipOwner]); hints are not — a
    // hint on a card's own copy button must not close the card under it.
    val owner = LocalChartTipOwner.current
    val token = remember { Any() }
    val held = owner.holds(token)
    LaunchedEffect(held, interactive) { if (interactive && !held && visible) visible = false }
    LaunchedEffect(visible) { if (!visible) owner.release(token) }
    DisposableEffect(token) { onDispose { owner.release(token) } }
    // After open, place-below cards ignore unhover briefly so OS Popup Exit cannot flash-loop.
    var suppressClose by remember { mutableStateOf(false) }
    val anchorSource = remember { MutableInteractionSource() }
    val anchorHovered by anchorSource.collectIsHoveredAsState()
    val popupSource = remember { MutableInteractionSource() }
    val popupHovered by popupSource.collectIsHoveredAsState()

    // Pointer in anchor-local coords for under-cursor placement (interactive, not place-below).
    val pointerPos = remember { mutableStateOf(Offset.Zero) }

    val resolvedPlacement = placement
        ?: TipPlacement.BesideAnchor(side = if (placeBelow) TipSide.Below else TipSide.Above)
    val underPointer = resolvedPlacement is TipPlacement.UnderPointer
    // Beside-anchor interactive cards: the pointer is not in the card when it opens, so the desktop
    // Popup-creation Exit would read as a real leave and flash-loop the card without the sticky.
    val placeBelowInteractive = interactive && !underPointer

    // Rich [interactive] cards join the global Alt / hover-details gate ([hoverDetailsEnabled]).
    // They are peers of [HoverDetailPopup]'s cards — dense explanatory content that erupting
    // mid-presentation is exactly what the gesture exists to stop — so they suppress with it. The
    // one-line hint overload and [OverflowTooltipText]'s elision reveal stay ungated: those are the
    // "must-have" tooltips [LocalHoverDetailsDefault] deliberately never suppresses (a truncated
    // label with no way to read it is a dead end, not a distraction).
    val gated = enabled && (!interactive || hoverDetailsEnabled())
    val shown = visible && gated
    // Latched: pointer still for [latchDelayMs] with the card up. Any move before that restarts
    // the dwell (re-keyed on the pointer); once latched it holds until the card closes.
    var latched by remember { mutableStateOf(false) }
    LaunchedEffect(shown, latchDelayMs, pointerPos.value) {
        if (!shown || latchDelayMs <= 0L) { latched = false; return@LaunchedEffect }
        if (!latched) {
            delay(latchDelayMs)
            latched = true
        }
    }
    // Report transitions only (the effect also runs once at composition, with `shown` false), and
    // an open card reports closed on dispose so a host's open-count cannot leak.
    DisposableEffect(shown) {
        if (shown) onVisibleChange?.invoke(true)
        onDispose { if (shown) onVisibleChange?.invoke(false) }
    }

    // Open after sustained hover (intent). Do not re-arm while already visible.
    val dwellMs = intentDelayMs.coerceAtLeast(0L)
    LaunchedEffect(anchorHovered, gated, visible, dwellMs) {
        if (visible || !gated) return@LaunchedEffect
        if (anchorHovered) {
            delay(dwellMs)
            if (anchorHovered && gated && !visible) {
                visible = true
                if (interactive) owner.claim(token)
            }
        }
    }

    // Place-below interactive: after open, ignore unhover long enough for the desktop OS Exit
    // from Popup creation to pass (help-icon-tooltip.md) without flash-closing.
    LaunchedEffect(visible, placeBelowInteractive) {
        if (visible && placeBelowInteractive) {
            suppressClose = true
            delay(PlaceBelowOpenStickyMs)
            suppressClose = false
        } else if (!visible) {
            suppressClose = false
        }
    }

    // Close only after both anchor and popup are un-hovered for the grace window — and not while
    // [suppressClose] is active (place-below open sticky). A gate flip (Alt down) closes at once,
    // outranking the sticky window: the card must get out of the way the instant Alt is held, which
    // is the same rule [HoverDetailPopup] follows.
    LaunchedEffect(visible, anchorHovered, popupHovered, gated, suppressClose) {
        if (!visible) return@LaunchedEffect
        if (!gated) {
            visible = false
            suppressClose = false
            return@LaunchedEffect
        }
        if (suppressClose) return@LaunchedEffect
        if (!anchorHovered && !popupHovered) {
            delay(HoverTooltipCloseMs)
            if (!anchorHovered && !popupHovered && !suppressClose) {
                visible = false
            }
        }
    }

    val positionProvider = rememberTipPositionProvider(pointerPos, resolvedPlacement) { resolvedPlacement }

    Box(
        modifier = modifier
            .hoverable(anchorSource)
            .pointerInput(Unit) {
                // Track pointer for under-cursor placement without consuming (Initial pass).
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial).changes.lastOrNull()
                            ?.let { pointerPos.value = it.position }
                    }
                }
            },
    ) {
        content()
        if (visible && gated) {
            Popup(
                popupPositionProvider = positionProvider,
                properties = PopupProperties(
                    // Beside-anchor cards: non-focusable reduces desktop OS Exit thrash; pointer
                    // events still reach the card for hover/copy. Under-pointer interactive peeks
                    // stay focusable like HoverDetailPopup.
                    focusable = interactive && underPointer,
                    dismissOnClickOutside = !interactive,
                ),
                onDismissRequest = {
                    if (!suppressClose) {
                        visible = false
                    }
                },
            ) {
                Box(modifier = Modifier.hoverable(popupSource)) {
                    val restingFill = containerColor ?: MaterialTheme.colorScheme.surface
                    TooltipCard(
                        maxWidth = maxWidth,
                        selectable = selectable,
                        containerColor = if (latched) tipFrozenFill(restingFill) else restingFill,
                        // A frosted card that latches goes opaque in the frozen fill too.
                        frosted = frosted && !latched,
                        frostedAlpha = frostedAlpha,
                    ) { tooltip() }
                }
            }
        }
    }
}

/**
 * Global height cap for a tooltip card's contents. Content taller than this scrolls inside the card's
 * bottom-fading container ([TooltipCard]) instead of growing to swallow the window — the runaway a
 * long line-item breakdown (e.g. a holding-heavy Obligation Sales) would otherwise produce.
 */
val TooltipMaxHeight: Dp = 320.dp

/**
 * The shared chrome for **every Compose hover card** — Family B ([HoverTooltip] rich, [HoverDetailPopup],
 * risk-band readout) and Family C (one-line [HoverTooltip], [OverflowTooltipText]). Canvas Family A
 * stays drawn (`drawChartTooltipCard`). Click-help (`HelpTooltipCard`) is not a hover tip.
 *
 * Fill is theme [androidx.compose.material3.ColorScheme.surface] plus a 1 dp [androidx.compose.material3.ColorScheme.outline]
 * and the existing shadow — a lightness step + border, not hue, so the card lifts off the panel
 * instead of reading as a `surfaceContainerHigh` smudge. Do not hardcode white (dark theme).
 *
 * Width capped at [maxWidth] and height at [maxHeight] — content past that height scrolls inside a
 * [FadingScrollColumn] tinted to the card, with a floating scrollbar thumb so the overflow is
 * visible rather than discoverable. Change it here and every hover card follows.
 *
 * By default wraps in [DisableSelection] (safe inside parent [androidx.compose.foundation.text.selection.SelectionContainer]
 * and for non-interactive name tooltips). Set [selectable] true for copyable/select-paste cards
 * ([CopyableTooltipBody] on slider markers).
 *
 * Supply your own inner padding in [content]; the card adds none.
 */
@Composable
fun TooltipCard(
    modifier: Modifier = Modifier,
    maxWidth: Dp = 240.dp,
    maxHeight: Dp = TooltipMaxHeight,
    selectable: Boolean = false,
    /** Card fill; also the fade color, so a scrolled edge blends into whatever this is. */
    containerColor: Color = MaterialTheme.colorScheme.surface,
    /**
     * Canvas chart-tip chrome: frosted white so what sits under the card still reads. Uses
     * [ChartTipFill] / [ChartTipBorder] / 5 dp corners — the same object as [drawChartTooltipCard].
     * Wins over [containerColor].
     */
    frosted: Boolean = false,
    /** Override [ChartTipFill] alpha when [frosted]. Null keeps the shared 0.88 wash. */
    frostedAlpha: Float? = null,
    /**
     * Tint the frosted wash instead of using its white base, keeping the chart-tip shape, border and
     * alpha. For a card that belongs to something colored — Bird's Eye's beam tints its asset tip
     * toward the asset hue and its liability tip toward the liability hue, so a card that has parked
     * itself away from the bar it describes still says which side it came from. Keep it *very* faint:
     * this is an identity cue under dense content, not a surface color.
     */
    frostedTint: Color? = null,
    /** Reader resize — explicit size and a corner grip. Null (default) is a fixed, content-hugging card. */
    resize: TooltipCardResize? = null,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    // What the card measures at when the drag starts, so the first step grows it from where it is
    // rather than jumping to some default; then the running size, accumulated here so drag steps
    // that land between recompositions are not lost.
    var measured by remember { mutableStateOf(IntSize.Zero) }
    var dragSize by remember { mutableStateOf<DpSize?>(null) }
    // The floor a drag can shrink to: the minimum, or the card's own size when it started smaller —
    // a small card must not jump up to the minimum on the first step of a drag.
    var dragFloor by remember { mutableStateOf(HoverCardMinSize) }
    val explicit = resize?.size
    val fill = if (frosted) {
        (frostedTint ?: ChartTipFill).copy(alpha = frostedAlpha ?: ChartTipFill.alpha)
    } else {
        containerColor
    }
    val border = if (frosted) ChartTipBorder else MaterialTheme.colorScheme.outline
    val shape: Shape = if (frosted) RoundedCornerShape(5.dp) else MaterialTheme.shapes.large
    val body: @Composable () -> Unit = {
        TooltipCardSurface(
            modifier = modifier,
            containerColor = fill,
            borderColor = border,
            shape = shape,
        ) {
            Box {
                FadingScrollColumn(
                    // Resized: the reader's size replaces both caps — wider *and* taller than the
                    // default card is the point.
                    modifier = (if (explicit != null) Modifier.size(explicit) else Modifier.widthIn(max = maxWidth).heightIn(max = maxHeight))
                        .onSizeChanged { measured = it },
                    fadeColor = fill,
                    // Hug content so a short tooltip stays narrow (only long lists reach maxWidth);
                    // a resized card fills the width it was given.
                    fillWidth = explicit != null,
                    // The one opt-in today: a card that overflows must say so, or it reads as complete.
                    scrollbar = true,
                ) {
                    content()
                }
                if (resize != null && resize.showGrip) {
                    val grip: @Composable () -> Unit = { HelpPopupCornerResize(
                        modifier = Modifier.align(Alignment.BottomEnd).testTag(HoverCardGripTag),
                        gripSize = HoverCardGripSize,
                        onDragStart = {
                            val start = resize.size ?: with(density) { DpSize(measured.width.toDp(), measured.height.toDp()) }
                            dragSize = start
                            dragFloor = DpSize(minOf(start.width, HoverCardMinSize.width), minOf(start.height, HoverCardMinSize.height))
                            resize.onResizing(true)
                        },
                        onDragEnd = { resize.onResizing(false) },
                        onDragPx = { dx, dy ->
                            val from = dragSize ?: return@HelpPopupCornerResize
                            val next = with(density) {
                                DpSize(
                                    (from.width + dx.toDp()).coerceIn(dragFloor.width, resize.maxSize.width.coerceAtLeast(dragFloor.width)),
                                    (from.height + dy.toDp()).coerceIn(dragFloor.height, resize.maxSize.height.coerceAtLeast(dragFloor.height)),
                                )
                            }
                            dragSize = next
                            resize.onResize(next)
                        },
                    ) }
                    // A frosted card is white by construction; its grip takes the light ink, as its
                    // contents do ([HoverDetailOptions.lightContent]), or it vanishes in dark theme.
                    if (frosted) MaterialTheme(colorScheme = PlusauriLightColorScheme) { grip() } else grip()
                }
            }
        }
    }
    if (selectable) {
        body()
    } else {
        // See [OverflowTooltipText]: DisableSelection guards against a crash when this renders inside an
        // enclosing SelectionContainer, and plain tooltips are not meant to be selectable.
        DisableSelection { body() }
    }
}

/** Surface tokens for Family B/C hover cards and the matching always-visible [UsStateHoverCard]. */
@Composable
internal fun TooltipCardSurface(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    shape: Shape = MaterialTheme.shapes.large,
    content: @Composable () -> Unit,
) {
    val depth = HoverTipDepth
    // Brushes need the card's height, which a Surface border cannot know. The face goes *behind* the
    // content (inside the Surface, which clips it to the shape) so the sheen never washes over text;
    // the beveled edge goes over everything, where a border belongs. Flat keeps the plain border.
    Surface(
        modifier = modifier.then(
            if (depth == TipDepth.Flat) {
                Modifier
            } else {
                Modifier.drawWithContent {
                    drawContent()
                    val radius = (shape as? CornerBasedShape)?.topStart?.toPx(size, this) ?: 0f
                    drawTipEdge(depth, borderColor, 0f, 0f, size.width, size.height, radius)
                }
            },
        ),
        shape = shape,
        color = containerColor,
        tonalElevation = 0.dp,
        shadowElevation = depth.composeShadow,
        border = if (depth == TipDepth.Flat) BorderStroke(1.dp, borderColor) else null,
    ) {
        if (depth == TipDepth.Flat) {
            content()
        } else {
            Box(Modifier.drawBehind { drawTipFace(depth, 0f, 0f, size.width, size.height, 0f) }) {
                content()
            }
        }
    }
}
