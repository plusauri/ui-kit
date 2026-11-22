package plusauri.ui

import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Whether a toggle button is selected or deselected. */
enum class ToggleState {
    Selected, Deselected;

    fun toggle(): ToggleState = if (this == Selected) Deselected else Selected
}

/**
 * The image content shown inside a [ToggleImageButton].
 *
 * - [Unicode] renders an arbitrary text/emoji string.
 * - [Content] renders a single composable regardless of toggle state.
 * - [ContentPair] renders different composables for [ToggleState.Selected] and
 *   [ToggleState.Deselected] — useful for paired on/off icons.
 */
sealed class ButtonImage {
    data class Unicode(val text: String) : ButtonImage()
    /**
     * @param name What the art pictures — its `contentDescription`. When the button has no tip of
     *   its own, [MultiButtonSelect] shows this on hover ([ButtonData.hoverName]), so an icon-only
     *   button says what it is. Null for art that carries its own words.
     */
    class Content(val name: String?, val composable: @Composable () -> Unit) : ButtonImage() {
        constructor(composable: @Composable () -> Unit) : this(null, composable)
    }
    class ContentPair(
        val selected: @Composable () -> Unit,
        val deselected: @Composable () -> Unit,
    ) : ButtonImage()
}

/**
 * How a [ToggleImageButton] sizes horizontally.
 *
 * [Square] is the icon-toolbar shape: width == height == `buttonSize`. Text in a square has room
 * for an icon or a two- or three-letter abbreviation at most, with the full name in a hover tip.
 *
 * [Fit] keeps the height but lets **content drive the width**, so the same toggle behavior — the
 * selected/deselected indicator bar, the state contract, the [MultiButtonSelect] grouping — works
 * for word-length labels like "Show totals" without a parallel chip control. [ButtonImage]'s
 * `Unicode` and `Content` can carry text in either mode; [Fit] gives it room.
 */
enum class ToggleButtonWidth {
    Square,
    Fit,
}

/**
 * An explicit, non-color mark of which toggle is selected, drawn in a **leading slot** of every
 * button in the row.
 *
 * Exists for the two-option row. With three or more toggles the odd one out is the selected one
 * and the pill / underbar treatment reads fine; with exactly two there is no "odd one out" — the
 * reader sees two differently colored chips and has to know which color means *on*. For a reader
 * who cannot separate the two hues there is nothing to go on at all. The mark answers that with
 * shape, which is also why it is a checkbox rather than a bolder color.
 *
 * The slot is reserved in **both** states — an empty box when deselected, a box with a check when
 * selected — so choosing never changes a button's width. A mark that appeared only on the selected
 * toggle would reflow the whole row on every click, which is more jarring than the ambiguity it
 * fixes.
 *
 * A creation-time choice on [ToggleImageButton] / `MultiButtonSelect`, not a per-slot one: either
 * the row carries marks or it does not. Only meaningful with [ToggleButtonWidth.Fit]; a square
 * icon toggle has no room beside its glyph.
 */
enum class SelectionMark {
    /** No slot; the pill and underbar carry selection (the default). */
    None,

    /** A small outlined box leading the content, ticked when selected. */
    CheckBox,
}

/** Side of the [SelectionMark.CheckBox] box — sized to sit beside label-small text. */
private val SelectionMarkSize: Dp = 13.dp

/** Gap between a [SelectionMark] and the content it leads. */
private val SelectionMarkGap: Dp = 6.dp

/**
 * How strongly a `deselectedAccent` paints the underbar and the surface.
 *
 * Fairly strong on purpose: a theme `error` red is already a dark, desaturated tone, and at a low
 * alpha (around 10%) over a light surface it lands somewhere between pink and nothing, so the tab
 * that most needs to be recognized at a glance would be the faintest.
 *
 * Selection is still legible because it is not carried by saturation alone: the selected slot swaps
 * to the **container** treatment and keeps its full-strength underbar, and the surface alpha still
 * differs by 6 points. If a slot color does start losing its selected state, raise the selected
 * side rather than lowering these — an unreadable severity is the worse failure.
 */
private const val DeselectedAccentIndicatorAlpha = 0.85f
private const val DeselectedAccentSurfaceAlpha = 0.22f

/**
 * The bar under a [ToggleImageButton] — 4 dp, rounded — as its own piece, so a control that is not a
 * toggle can wear the same mark — for example a navigation rail that draws it under each entry in
 * that entry's own color. The caller gives the width.
 */
@Composable
fun ToggleUnderbar(color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(color),
    )
}

/** Horizontal breathing room inside a [ToggleButtonWidth.Fit] button, where width is content-driven. */
private val FitHorizontalPadding: Dp = 10.dp

/** Material's standard disabled-content alpha, applied to whatever artwork a caller supplied. */
private const val DisabledContentAlpha: Float = 0.38f

/**
 * Describes the visual content of a [ToggleImageButton].
 *
 * @param buttonImage Image or unicode content to display.
 * @param label       Optional label shown below the button; hidden when blank.
 */
data class ButtonData(
    val buttonImage: ButtonImage,
    val label: String = "",
    /**
     * Words a letter search matches when they are drawn inside [buttonImage] rather than [label].
     * [label] is also painted under the chip, so a chip that already shows its name in the face
     * passes the name here and leaves [label] blank.
     */
    val typeAhead: String = "",
) {
    /**
     * The hover tip an icon button gets when its toolbar passes none: the art's name, unless the
     * label under the button already says it. Blank for text buttons — their words are the label.
     */
    val hoverName: String
        get() = (buttonImage as? ButtonImage.Content)?.name
            ?.takeIf { it.isNotBlank() && !it.equals(label, ignoreCase = true) }
            .orEmpty()
}

/**
 * A toggle button with an image (or unicode) and an optional label.
 *
 * The button shows a colored state-indicator bar directly below the image:
 * primary blue over `primaryContainer` when [state] is [ToggleState.Selected], soft gold over a
 * warm cream pill when [ToggleState.Deselected] (see `toggleDeselectedSurfaceColor`). Clicking
 * flips the state and calls [onStateChange].
 *
 * The component is stateless — the caller owns [state] and decides whether to
 * apply the change returned via [onStateChange].
 *
 * @param buttonData    Image and optional label for this button.
 * @param state         Current toggle state.
 * @param onStateChange Called with the new [ToggleState] on every click.
 * @param enabled       When false the button is non-interactive and dimmed.
 * @param buttonSize    Side length in [ToggleButtonWidth.Square]; the **height** in
 *                      [ToggleButtonWidth.Fit], where content drives the width.
 * @param widthMode     [ToggleButtonWidth.Square] (default) or [ToggleButtonWidth.Fit]
 *                      for text-width buttons.
 * @param textStyle     Style for [ButtonImage.Unicode] content. Defaults to the roomy headline that
 *                      suits a glyph in a square; text-width callers pass something label-sized.
 * @param selectedAccent Optional series / legend color. When non-null and selected, the surface
 *                      tint and underbar use this accent (e.g. a chart series' color) instead of
 *                      theme primary — so the toggle stays legible as a color key. Disabled chrome
 *                      is unchanged.
 * @param selectionMark Optional leading checkbox mark that says *selected* by shape rather than by
 *                      color; see [SelectionMark] for when it earns its slot. Requires
 *                      [ToggleButtonWidth.Fit].
 * @param deselectedAccent Optional color for the **unselected** surface and underbar, replacing the
 *                      warm cream / soft-gold pair.
 *
 *                      Pass it only when the slot's color means something *while it is not
 *                      selected* — a severity, a lane identity — rather than merely marking which
 *                      one is open. A legend row wants [selectedAccent] alone: its colors answer
 *                      "which series is this", a question that is only asked of the series you
 *                      turned on. Tabs that sort messages by severity want the pair, because an
 *                      Errors tab is red whether or not you are looking at it.
 *
 *                      Deliberately dimmer than [selectedAccent] rather than a second full-strength
 *                      color: if both states are equally saturated, the accent stops carrying
 *                      selection at all and the underbar is doing the whole job alone.
 * @param folded        On, but what the button shows is folded away (a chip whose table row is
 *                      collapsed). Draws the selected underbar on the resting surface; still
 *                      announced as selected. Ignored when not selected, disabled, or accented.
 * @param modifier      Applied to the outermost [Column].
 */
@Composable
fun ToggleImageButton(
    buttonData: ButtonData,
    state: ToggleState = ToggleState.Deselected,
    onStateChange: (ToggleState) -> Unit,
    enabled: Boolean = true,
    buttonSize: Dp = 48.dp,
    widthMode: ToggleButtonWidth = ToggleButtonWidth.Square,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium,
    selectedAccent: Color? = null,
    deselectedAccent: Color? = null,
    selectionMark: SelectionMark = SelectionMark.None,
    folded: Boolean = false,
    modifier: Modifier = Modifier,
    /**
     * Where the face (mark and label) sits when the button is wider than it, as in a side toolbar's
     * equal-width column. Centered by default; a column passes [Alignment.CenterStart] so the marks line up.
     */
    faceAlignment: Alignment = Alignment.Center,
) {
    require(selectionMark == SelectionMark.None || widthMode == ToggleButtonWidth.Fit) {
        "selectionMark needs ToggleButtonWidth.Fit — a square toggle has no room beside its glyph"
    }
    val isSelected = state == ToggleState.Selected
    // On, but what it shows is folded away: the selected underbar says "on", the resting surface
    // says "not on screen". Only a selected button can be folded.
    val foldedLook = folded && isSelected && enabled && selectedAccent == null
    // A disabled toggle is not "selected" in any sense the reader can act on, so it must not wear
    // the selected indicator. Disabling has to show on the surface and underbar, not only block the
    // click: a text-mode toggle keeps its words in the image slot rather than in `label`, so
    // without this a disabled option would look identical to a live, selected one.
    val indicatorColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        isSelected && selectedAccent != null -> selectedAccent
        isSelected -> toggleSelectedIndicatorColor(MaterialTheme.colorScheme)
        deselectedAccent != null -> deselectedAccent.copy(alpha = DeselectedAccentIndicatorAlpha)
        else -> toggleDeselectedIndicatorColor(MaterialTheme.colorScheme)
    }
    val surfaceColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
        isSelected && selectedAccent != null -> selectedAccent.copy(alpha = 0.16f)
        foldedLook -> toggleDeselectedSurfaceColor(MaterialTheme.colorScheme)
        isSelected -> toggleSelectedSurfaceColor(MaterialTheme.colorScheme)
        deselectedAccent != null -> deselectedAccent.copy(alpha = DeselectedAccentSurfaceAlpha)
        else -> toggleDeselectedSurfaceColor(MaterialTheme.colorScheme)
    }
    // One ink for both states: the warm/cool pill and the underbar say which slot is open, so the
    // text does not also have to. Accented and disabled slots keep Surface's own contentColorFor.
    val contentColor = when {
        !enabled || selectedAccent != null && isSelected || deselectedAccent != null && !isSelected ->
            contentColorFor(surfaceColor)
        else -> toggleContentColor(MaterialTheme.colorScheme, isSelected && !foldedLook)
    }
    val fit = widthMode == ToggleButtonWidth.Fit

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        // Fit: the column takes its children's widest intrinsic width, so the surface and the
        // indicator below it share one content-driven width instead of a fixed square.
        // The toggle's state, announced: a chip row otherwise says nothing about which chips are
        // on, and a reader (or a UI test) has only the tint to go by. Disabled is announced here
        // too, on the node that carries the option's tag — on the inner Surface alone, a reader of
        // the option would find a dimmed chip described as an ordinary one.
        modifier = (if (fit) modifier.width(IntrinsicSize.Max) else modifier)
            .semantics {
                selected = isSelected
                if (!enabled) disabled()
            },
    ) {
        val interaction = remember { MutableInteractionSource() }
        val focused by interaction.collectIsFocusedAsState()
        Surface(
            onClick = { onStateChange(state.toggle()) },
            enabled = enabled,
            interactionSource = interaction,
            shape = RoundedCornerShape(8.dp),
            color = surfaceColor,
            contentColor = contentColor,
            modifier = if (fit) {
                Modifier.fillMaxWidth().height(buttonSize)
            } else {
                Modifier.size(buttonSize)
            },
        ) {
            Box(
                contentAlignment = faceAlignment,
                // The kit's keyboard focus ring, on the button face itself — first in the chain, so
                // it spans the face before any padding. Not on the Surface's modifier: Surface
                // widens that to its minimum touch target, and the ring would swallow the label.
                modifier = Modifier.focusRingFor(focused, RoundedCornerShape(8.dp))
                    .then(if (fit) Modifier.padding(horizontal = FitHorizontalPadding) else Modifier)
                    // Alpha on the container dims every ButtonImage variant alike — Unicode text,
                    // an arbitrary Content composable, and paired icons — without each caller
                    // having to thread a disabled color through its own artwork.
                    .alpha(if (enabled) 1f else DisabledContentAlpha),
            ) {
                val image: @Composable () -> Unit = {
                    when (val img = buttonData.buttonImage) {
                        is ButtonImage.Unicode -> Text(
                            img.text.withFlagEmojiFont(LocalFlagEmojiFont.current),
                            style = textStyle,
                        )
                        is ButtonImage.Content -> img.composable()
                        is ButtonImage.ContentPair ->
                            if (isSelected) img.selected() else img.deselected()
                    }
                }
                if (selectionMark == SelectionMark.None) {
                    image()
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SelectionMarkBox(selectionMark, checked = isSelected)
                        Spacer(Modifier.width(SelectionMarkGap))
                        image()
                    }
                }
            }
        }
        // Indicator width matches the button exactly — avoids expanding to container width.
        ToggleUnderbar(
            color = indicatorColor,
            modifier = if (fit) Modifier.fillMaxWidth() else Modifier.width(buttonSize),
        )
        if (buttonData.label.isNotEmpty()) {
            Text(
                text = buttonData.label,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = if (enabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
            )
        }
    }
}

/**
 * The leading mark for [SelectionMark.CheckBox]: an outlined box in the current content color,
 * with a check inside when [checked]. Drawn by hand rather than with Material's `Checkbox` because
 * that one brings its own ~40.dp touch target and colors, and this is a glyph inside a chip, not a
 * control of its own — the chip is the control. The box is drawn in both states so the slot's
 * width is the same whether or not it is ticked.
 */
@Composable
private fun SelectionMarkBox(mark: SelectionMark, checked: Boolean) {
    when (mark) {
        SelectionMark.None -> Unit
        SelectionMark.CheckBox -> Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(SelectionMarkSize)
                .border(1.dp, LocalContentColor.current, RoundedCornerShape(2.dp)),
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SelectionMarkSize - 2.dp),
                )
            }
        }
    }
}
