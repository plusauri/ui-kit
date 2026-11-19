package plusauri.ui

import androidx.compose.foundation.focusable
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon

/** Whether a [CollapseGrabberDivider] runs across the width (a rule below a row) or the height
 *  (a rule beside a column/rail). Decides the divider, the pill's long axis, and the caret glyph. */
enum class CollapseOrientation { Horizontal, Vertical }

/** The grabber pill's length **along** the divider — ~1.2 physical inches, generous enough to find. */
private val GrabberLong = 120.dp

/**
 * Public face of [GrabberShort], for a parent that has to **reserve** the grabber's width before it
 * can measure anything else.
 *
 * `BirdsEyeBody` is the case: it lays its three panes out with an explicit [androidx.compose.ui.layout.Layout]
 * so the rule can run the full height of the pane, and an explicit layout has to know this width up
 * front rather than discovering it. Asking the parent for `IntrinsicSize.Min` instead is what that
 * code did first, and it crashed the Bird's Eye modal — see the note there.
 */
object CollapseGrabberDefaults {
    /** Width of a vertical grabber, height of a horizontal one. */
    val Thickness: Dp get() = GrabberShort
}

/**
 * The grabber pill's thickness **across** the divider. Deliberately shorter than the caret icon's
 * natural ~24dp box so the whitespace around the chevron is trimmed (~half) for a tighter, space-saving
 * divider; the caret glyph sits in the middle of its box and survives the clip.
 */
private val GrabberShort = 14.dp

/**
 * A thin divider rule whose middle ~[GrabberLong] section swells into a clickable pill carrying an
 * expand/collapse caret — the app's shared collapse affordance, used both below the global header
 * ([Horizontal][CollapseOrientation.Horizontal]) and beside the Simulator nav rail
 * ([Vertical][CollapseOrientation.Vertical]). Clicking toggles [collapsed] via [onToggle]; the caller
 * hides the adjacent region on collapse and the caret flips to point the other way.
 *
 * Caret glyph by orientation:
 *  - Horizontal reuses [ExposedDropdownMenuDefaults.TrailingIcon] (`expanded = !collapsed`) — the same
 *    rotating chevron the app's other collapsibles use (see ResizableCollapsible): points up while the
 *    region is shown ("pull up / hide"), down once hidden ("reveal").
 *  - Vertical uses an auto-mirrored keyboard arrow: points toward the region while it's shown ("push
 *    aside / hide"), away from it once hidden ("reveal") — the sideways analog of the same gesture.
 *
 * @param collapsedLabel a11y click label announced while the region is shown (the click will hide it).
 * @param expandedLabel  a11y click label announced while the region is hidden (the click will show it).
 * @param accentColor    optional state color for the pill (fill, border and caret). Null keeps the
 *                       neutral chrome, which is what every grabber that is only a grabber wants.
 *                       Pass a color only when the *collapsed region* has something the reader must
 *                       not miss while it is hidden — the dossier metadata panel's error shade is
 *                       the only such case today. A grabber that is always colored is decoration,
 *                       and decoration here would cost the color its meaning everywhere else.
 * @param onDrag         non-null makes the whole divider a drag handle across its rule (pixels,
 *                       positive = down / trailing), so one seam can both fold and size its region.
 *                       Null — every chrome divider — keeps it click-only.
 * @param onDragStopped  the drag's end, for a caller that commits a size once rather than per frame.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapseGrabberDivider(
    orientation: CollapseOrientation,
    collapsed: Boolean,
    onToggle: () -> Unit,
    collapsedLabel: String,
    expandedLabel: String,
    accentColor: Color? = null,
    onDrag: ((Float) -> Unit)? = null,
    onDragStopped: () -> Unit = {},
    look: SeamLook? = null,
    tooltip: String? = null,
    leading: (@Composable () -> Unit)? = null,
    /**
     * False when the seam strip owns the keyboard stop ([RegionSeamDivider]'s focus ring). The pill
     * stays a mouse click and does not take a second stop, and it does not draw its own ring.
     */
    pillTakesFocus: Boolean = true,
) {
    val horizontal = orientation == CollapseOrientation.Horizontal
    val actionLabel = if (collapsed) expandedLabel else collapsedLabel
    // The container spans the divider's long axis; the pill swaps its long/short axes to match.
    val containerModifier = (if (horizontal) Modifier.fillMaxWidth() else Modifier.fillMaxHeight())
        .then(
            if (onDrag == null) Modifier
            else Modifier.draggable(
                orientation = if (horizontal) Orientation.Vertical else Orientation.Horizontal,
                state = rememberDraggableState { onDrag(it) },
                onDragStopped = { onDragStopped() },
            ),
        )
    val pillModifier =
        if (horizontal) Modifier.width(GrabberLong).height(GrabberShort)
        else Modifier.width(GrabberShort).height(GrabberLong)
    // The rule is drawn in **two segments with the pill between them**, not as one full-length line
    // behind it. A single line showed through: an accented pill is a translucent wash (see
    // ToggleImageButton's deselected alphas), so the rule struck straight through the middle of it
    // and read as a pin through a bead rather than as a handle interrupting a seam.
    val rule: @Composable (Modifier) -> Unit = { mod ->
        when {
            look != null -> SeamRule(look.rule, look.dashed, mod, vertical = !horizontal)
            horizontal -> HorizontalDivider(modifier = mod)
            else -> VerticalDivider(modifier = mod)
        }
    }
    val pill: @Composable () -> Unit = { HoverTooltip(tooltip.orEmpty()) {
        Box(
            modifier = when {
                look == null -> pillModifier
                horizontal -> Modifier.width(GrabberLong).height(SeamPillShort)
                else -> Modifier.width(SeamPillShort).height(GrabberLong)
            }
                .clip(RoundedCornerShape(percent = 50))
                .background(
                    look?.fill ?: accentColor?.copy(alpha = 0.22f) ?: MaterialTheme.colorScheme.surfaceVariant,
                )
                .border(
                    BorderStroke(1.dp, look?.border ?: accentColor ?: MaterialTheme.colorScheme.outlineVariant),
                    RoundedCornerShape(percent = 50),
                )
                // Named, not just a caret: a screen reader otherwise announces a bare "button".
                .semantics { contentDescription = tooltip?.takeIf { it.isNotEmpty() } ?: actionLabel }
                .then(
                    if (pillTakesFocus) Modifier.keyboardFocusRing(RoundedCornerShape(percent = 50))
                    else Modifier.focusProperties { canFocus = false },
                )
                .clickable(role = Role.Button, onClickLabel = actionLabel, onClick = onToggle)
                .pointerHoverIcon(PointerIcon.Hand),
            contentAlignment = Alignment.Center,
        ) {
            // The caret takes the accent through LocalContentColor rather than a tint argument:
            // TrailingIcon has no color parameter, so tinting only the sideways caret would leave
            // the two orientations disagreeing about what an accented grabber looks like.
            CompositionLocalProvider(LocalContentColor provides (accentColor ?: LocalContentColor.current)) {
                if (look != null && horizontal) {
                    // A seam's pill carries its state in color, and the caret has to be read at a
                    // glance too — the drawn triangle, not the thin menu chevron.
                    SeamCaret(pointsUp = !collapsed, color = look.caret)
                } else if (horizontal) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = !collapsed)
                } else {
                    // A drawn solid triangle, not `Icons.KeyboardArrowLeft`. That icon is a thin
                    // 2 dp stroke in a 24 dp box, and the pill is 14 dp across — so it arrived both
                    // clipped and faint, in the one place where the glyph is the *entire* affordance
                    // (a vertical grabber has no room for a label). Drawing it keeps the weight and
                    // the fit under this component's control instead of the icon set's.
                    CollapseCaret(pointsTrailing = collapsed, color = look?.caret)
                }
            }
        }
    } }
    Box(modifier = containerModifier, contentAlignment = Alignment.Center) {
        if (horizontal) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // [leading] rides the left half, so the pill stays centered like every other seam.
                if (leading == null) rule(Modifier.weight(1f))
                else Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    leading()
                    rule(Modifier.weight(1f))
                }
                pill()
                rule(Modifier.weight(1f))
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                rule(Modifier.weight(1f))
                pill()
                rule(Modifier.weight(1f))
            }
        }
    }
}

/** Half the pill's short axis, so the triangle fills it without touching the rounded edge. */
private val CaretHalfWidth = 3.5.dp

/** Long enough to read as a direction at a glance; the pill is 14 dp across. */
private val CaretHalfHeight = 5.dp

/**
 * The solid left/right triangle inside a vertical grabber.
 *
 * Filled rather than a chevron because this glyph is doing the whole job: a horizontal grabber sits
 * under a labelled region, but a vertical one is a bare 14 dp strip, and a hairline chevron in it
 * is easy to miss entirely.
 *
 * Painted in `onSurface` rather than taking the accent, even on an accented grabber. An amber
 * triangle on an amber wash is the one combination where the color costs contrast instead of adding
 * meaning — and the pill's fill and border are already carrying the severity, so the caret is free
 * to be simply legible.
 */
@Composable
private fun CollapseCaret(pointsTrailing: Boolean, color: Color? = null) {
    val color = color ?: MaterialTheme.colorScheme.onSurface
    val mirror = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(modifier = Modifier.size(CaretHalfWidth * 2, CaretHalfHeight * 2)) {
        val right = pointsTrailing != mirror
        val path = Path().apply {
            if (right) {
                moveTo(0f, 0f)
                lineTo(size.width, size.height / 2f)
                lineTo(0f, size.height)
            } else {
                moveTo(size.width, 0f)
                lineTo(0f, size.height / 2f)
                lineTo(size.width, size.height)
            }
            close()
        }
        drawPath(path, color)
    }
}

/**
 * The horizontal rule **above a region inside a panel**, as a collapse control — the same grabber
 * language as the four chrome dividers, for content rather than chrome. First use: the loan editor
 * embedded in a Real Asset editor under its *Financed by* toggle, which is most of that editor's
 * height once a mortgage is on it.
 *
 * Same geometry as [CollapsibleToolsBarDivider] (region *below* its grabber, caret inverted so it
 * points down at the region while shown and up while hidden) with the a11y labels supplied by the
 * caller, since only the caller knows what it is hiding. [accentColor] follows the rule on
 * [CollapseGrabberDivider]: null unless the hidden region holds something the reader must not miss.
 */
@Composable
fun CollapsibleDetailDivider(
    collapsed: Boolean,
    onToggle: () -> Unit,
    /** Announced while the region is shown — the click will hide it. */
    hideLabel: String,
    /** Announced while the region is hidden — the click will show it. */
    showLabel: String,
    accentColor: Color? = null,
) {
    CollapseGrabberDivider(
        orientation = CollapseOrientation.Horizontal,
        // Invert [collapsed] so TrailingIcon points down while the region is shown and up while hidden.
        collapsed = !collapsed,
        onToggle = onToggle,
        // Labels swap with the invert: when [collapsed] is true the region is hidden, the divider
        // receives false, and [collapsedLabel] is announced — that click will *show* the region.
        collapsedLabel = showLabel,
        expandedLabel = hideLabel,
        accentColor = accentColor,
    )
}

/**
 * A [RegionSeamDivider]'s restore button: [onReset] puts the region back to full height, and
 * [label] is its hover tip and a11y name. One value, so a reset cannot arrive without its words.
 */
class SeamReset(val label: String, val onReset: () -> Unit)

/** How much of a region under a [RegionSeamDivider] is showing — the seam's three looks. */
enum class RegionShown { Open, Partial, Closed }

/** A seam's colors, and whether its rule is dashed; see [seamLook]. */
class SeamLook(val fill: Color, val border: Color, val caret: Color, val rule: Color, val dashed: Boolean)

/**
 * The seam's palette, one look per [RegionShown] — the state is told by **color and line**, since a
 * caret alone is too small to read at a glance:
 * - **Open**: the brand blue pill on a plain rule — everything is showing, nothing to flag.
 * - **Partial**: the same blue pill on a **dashed** blue rule — the region has been dragged shorter
 *   and scrolls inside; the dashes say "there is more than you see".
 * - **Closed**: a gold pill on a gold rule — something is tucked away here. Gold, not the error
 *   color the chrome grabbers' `accentColor` reserves: a closed row is a choice, not a problem.
 */
@Composable
fun seamLook(shown: RegionShown): SeamLook {
    val scheme = MaterialTheme.colorScheme
    val dark = scheme.surface.luminance() < 0.5f
    return when (shown) {
        RegionShown.Open, RegionShown.Partial -> SeamLook(
            fill = scheme.primaryContainer,
            border = scheme.primary.copy(alpha = 0.7f),
            caret = scheme.primary,
            rule = if (shown == RegionShown.Partial) scheme.primary.copy(alpha = 0.8f) else scheme.outlineVariant,
            dashed = shown == RegionShown.Partial,
        )
        RegionShown.Closed -> SeamLook(
            fill = if (dark) PlusauriGoldContainerDark else PlusauriGoldContainer,
            border = PlusauriGold,
            caret = if (dark) PlusauriGoldLight else PlusauriGoldDeep,
            rule = PlusauriGold.copy(alpha = 0.8f),
            dashed = false,
        )
    }
}

/**
 * The horizontal rule **below a region inside a panel** that says how much of the region is showing
 * ([RegionShown], in color and line — see [seamLook]) and lets you change it: click the pill to close
 * or open the region, and — when [onDrag] is given — drag the rule to size it, with [reset] as the
 * pill-shaped restore button at its end while [RegionShown.Partial]. Hovering the pill names what the
 * click will do ([hideLabel] / [showLabel], also the a11y label).
 *
 * Used by every Forecast Detail row ([ForecastDetailRow], sizable) and by the Forecast Detail control
 * row ([ForecastDetailControlRow], open/closed only). Same geometry as the header's divider (region
 * *above* its grabber), so the caret points up at the region while shown and down while closed.
 */
@Composable
fun RegionSeamDivider(
    shown: RegionShown,
    onToggle: () -> Unit,
    /** Hover tip and a11y label while the region shows — the click will close it. */
    hideLabel: String,
    /** Hover tip and a11y label while the region is closed — the click will open it. */
    showLabel: String,
    modifier: Modifier = Modifier,
    onDrag: ((Float) -> Unit)? = null,
    onDragStopped: () -> Unit = {},
    reset: SeamReset? = null,
    /** The region sits *below* the seam (a bottom bar): the caret flips to keep pointing at it. */
    regionBelow: Boolean = false,
    /**
     * Named on the seam itself while [RegionShown.Closed], left of the pill, so a closed region costs
     * one thin line rather than a label line plus a seam. Clicking the name opens it too.
     */
    closedName: String? = null,
    /**
     * The whole strip — the closed name and the pill — is one keyboard stop. Focus tints that
     * strip. The name and the pill stay mouse clicks and do not take their own stop.
     */
    focusStrip: Boolean = false,
) {
    val look = seamLook(shown)
    val closed = shown == RegionShown.Closed
    Box(
        modifier
            .fillMaxWidth()
            .then(
                if (focusStrip) {
                    Modifier
                        .keyboardFocusTint()
                        .focusable()
                        .semantics {
                            role = Role.Button
                            contentDescription = if (closed) showLabel else hideLabel
                        }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        CollapseGrabberDivider(
            orientation = CollapseOrientation.Horizontal,
            // The caret points up while `collapsed` is false; flipping it for a region below makes
            // it point down at the region while shown. Labels and tip follow the real state below.
            collapsed = closed != regionBelow,
            onToggle = onToggle,
            collapsedLabel = if (regionBelow) showLabel else hideLabel,
            expandedLabel = if (regionBelow) hideLabel else showLabel,
            onDrag = onDrag,
            onDragStopped = onDragStopped,
            look = look,
            tooltip = if (closed) showLabel else hideLabel,
            pillTakesFocus = !focusStrip,
            leading = closedName?.takeIf { closed }?.let { name ->
                {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .then(if (focusStrip) Modifier.focusProperties { canFocus = false } else Modifier)
                            .clickable(onClick = onToggle)
                            .padding(start = 12.dp, end = 8.dp),
                    )
                }
            },
        )
        if (shown == RegionShown.Partial && reset != null) {
            // Pilled like the grabber, and centered on the rule, so it reads as part of the seam.
            HoverTooltip(reset.label, Modifier.align(Alignment.CenterEnd)) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(look.fill)
                        .border(BorderStroke(1.dp, look.border), RoundedCornerShape(percent = 50))
                        .padding(horizontal = 6.dp),
                ) { ResetToFullButton(reset.label, reset.onReset) }
            }
        }
    }
}

/**
 * A **split** between two panes, drawn as a seam: the same rule and pill as [RegionSeamDivider], so
 * the dossier's Bird's Eye | editors split looks like every other grabber. It does not fold — a
 * split has no region to close — so its pill carries a grip rather than a caret.
 *
 * Drag to move it; or focus the pill (Tab) and ↑ / ↓ move it by [nudge].
 */
@Composable
fun SplitSeam(
    onDrag: (deltaPx: Float) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    nudge: Dp = 24.dp,
) {
    val look = seamLook(RegionShown.Open)
    val nudgePx = with(LocalDensity.current) { nudge.toPx() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .draggable(orientation = Orientation.Vertical, state = rememberDraggableState(onDrag)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SeamRule(look.rule, look.dashed, Modifier.weight(1f), vertical = false)
        HoverTooltip(label) {
            Box(
                modifier = Modifier
                    .width(GrabberLong)
                    .height(SeamPillShort)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(look.fill)
                    .border(BorderStroke(1.dp, look.border), RoundedCornerShape(percent = 50))
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                        when (event.key) {
                            Key.DirectionUp -> { onDrag(-nudgePx); true }
                            Key.DirectionDown -> { onDrag(nudgePx); true }
                            else -> false
                        }
                    }
                    .semantics { contentDescription = label }
                    .keyboardFocusRing(RoundedCornerShape(percent = 50))
                    .focusable()
                    .pointerHoverIcon(PointerIcon.Hand),
                contentAlignment = Alignment.Center,
            ) {
                // The grip: two short bars, the move-me mark, where a folding seam draws its caret.
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(2) {
                        Box(Modifier.width(18.dp).height(2.dp).clip(RoundedCornerShape(1.dp)).background(look.caret))
                    }
                }
            }
        }
        SeamRule(look.rule, look.dashed, Modifier.weight(1f), vertical = false)
    }
}

/** The seam pill's thickness: a notch taller than a chrome grabber's, to hold the [SeamCaret]. */
private val SeamPillShort = 16.dp

/** A seam's solid up/down triangle — big enough to read at a glance, in the seam's state color. */
@Composable
private fun SeamCaret(pointsUp: Boolean, color: Color) {
    Canvas(Modifier.size(width = 11.dp, height = 7.dp)) {
        val path = Path().apply {
            if (pointsUp) {
                moveTo(0f, size.height); lineTo(size.width / 2f, 0f); lineTo(size.width, size.height)
            } else {
                moveTo(0f, 0f); lineTo(size.width / 2f, size.height); lineTo(size.width, 0f)
            }
            close()
        }
        drawPath(path, color)
    }
}

/** A seam's rule, solid or dashed, in [color] — across the seam, or down it when [vertical]. */
@Composable
private fun SeamRule(color: Color, dashed: Boolean, modifier: Modifier, vertical: Boolean = false) {
    Canvas(if (vertical) modifier.width(1.5.dp) else modifier.height(1.5.dp)) {
        drawLine(
            color = color,
            start = if (vertical) Offset(size.width / 2f, 0f) else Offset(0f, size.height / 2f),
            end = if (vertical) Offset(size.width / 2f, size.height) else Offset(size.width, size.height / 2f),
            strokeWidth = if (vertical) size.width else size.height,
            pathEffect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())) else null,
        )
    }
}

/** The one reset-to-full-height button, for any resizable region; [onClick] does the reset. */
@Composable
fun ResetToFullButton(name: String, onClick: () -> Unit) {
    TipIconButton(tip = name, onClick = onClick, size = 20.dp) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = name,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Which edge of the window a side panel sits on, for [PanelSeamDivider]. */
enum class PanelSide { Leading, Trailing }

/**
 * The vertical seam between a side panel and the content beside it: click the pill to fold or open the
 * panel, drag the rule to size it. The look is the region seam's ([seamLook]): a blue pill while the panel
 * shows, gold once it is folded away. The caret points at the panel while it shows and away once folded,
 * on either [side], so a caller never inverts flags to get a right-hand panel.
 *
 * The caller lays the panel out and hides it when [collapsed]; this draws only the seam.
 *
 * @param hideLabel Hover tip and a11y label while the panel shows (the click will fold it).
 * @param showLabel Hover tip and a11y label while it is folded (the click will open it).
 * @param onResize  Non-null makes the rule a resize handle while the panel shows: pixels, **positive when
 *                  the panel should grow**, whichever side it is on. Clamp in the caller.
 * @param onResizeStopped The drag's end, for a caller that keeps the width once rather than per frame.
 */
@Composable
fun PanelSeamDivider(
    side: PanelSide,
    collapsed: Boolean,
    onToggle: () -> Unit,
    hideLabel: String,
    showLabel: String,
    onResize: ((Float) -> Unit)? = null,
    onResizeStopped: () -> Unit = {},
) {
    val trailing = side == PanelSide.Trailing
    val mirror = LocalLayoutDirection.current == LayoutDirection.Rtl
    // A drag reports pixels rightward on screen; a left-hand panel grows with them, a right-hand one shrinks.
    val growsTrailing = !trailing
    CollapseGrabberDivider(
        orientation = CollapseOrientation.Vertical,
        // The caret points leading while `collapsed` is false; a trailing panel flips it to point at itself.
        collapsed = collapsed != trailing,
        onToggle = onToggle,
        collapsedLabel = if (trailing) showLabel else hideLabel,
        expandedLabel = if (trailing) hideLabel else showLabel,
        look = seamLook(if (collapsed) RegionShown.Closed else RegionShown.Open),
        tooltip = if (collapsed) showLabel else hideLabel,
        onDrag = onResize?.takeIf { !collapsed }?.let { resize -> { px -> resize(if (growsTrailing != mirror) px else -px) } },
        onDragStopped = onResizeStopped,
    )
}
