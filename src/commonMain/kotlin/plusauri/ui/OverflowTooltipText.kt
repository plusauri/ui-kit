package plusauri.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * Max width for the full-text hover card on an elided label. Long Sales/Inv. Why strings (and
 * similar multi-segment cells) wrap instead of stretching one horizontal line across the window.
 * Matches the spirit of [HoverTooltip]'s default [maxWidth].
 */
private val OverflowTooltipMaxWidth: Dp = 280.dp

/**
 * A [Text] — single-line by default — that ellipsizes when its content is wider than the space it's
 * given, and, only when that happens, reveals the full text in a hover tooltip.
 *
 * This is the reusable answer to labels that don't fit (large multiselect chips, packed chart
 * titles, …): the label stays apt and complete in the data, but the on-screen text shows as much
 * as fits followed by "…", and the user can hover to read the rest. Truncation is detected
 * programmatically via [onTextLayout]'s [androidx.compose.ui.text.TextLayoutResult.hasVisualOverflow],
 * so the tooltip appears exactly when (and only when) the label is actually clipped.
 *
 * For clipping to occur the caller must give this composable a bounded width — either a
 * `Modifier.widthIn(max = …)` cap or a flexible `Modifier.weight(…, fill = false)` slot in a
 * [androidx.compose.foundation.layout.Row]. An unbounded parent lets the text grow and nothing is
 * ever truncated.
 *
 * ## Why a plain [Box] + [Popup] rather than Material3's `TooltipBox`
 * The [modifier] must land on a plain [Box]: a Box is an ordinary `Layout`, so it forwards its
 * width constraint to the [Text] *and* passes intrinsic measurements straight through. That matters
 * on both fronts —
 * - `TooltipBox` measures its anchor with loose constraints, so a `weight`-bounded text measured
 *   full-width inside it flip-flops clipped/unclipped every frame (visible fluttering).
 * - `BoxWithConstraints` is a `SubcomposeLayout` and cannot answer intrinsic-size queries, which
 *   `SingleChoiceSegmentedButtonRow` (`width(IntrinsicSize.Min)`) makes — it throws.
 *
 * A plain [Box] avoids both. The tooltip is a hover-driven overlay [Popup] (same approach as
 * [HelpIcon]) so it never re-measures the text.
 *
 * @param text     The full label text (also the tooltip contents when clipped).
 * @param modifier Layout modifier bounding the width (e.g. `widthIn(max = …)` or `weight(…)`).
 * @param style    Text style; defaults to the ambient [LocalTextStyle].
 * @param color    Text color; [Color.Unspecified] defers to [style].
 * @param contentAlignment Where the text sits when it is *shorter* than the bound — the box fills
 *                  the width it was given, so a label that used to size to its own content and be
 *                  centered by its parent needs [Alignment.Center] here to stay centered. Defaults
 *                  to [Alignment.TopStart], matching a plain [Box].
 * @param maxLines Lines before ellipsis. At the default 1 the text never wraps (`softWrap = false`),
 *                  which is what makes it clip horizontally and report the overflow; above 1 it
 *                  wraps and ellipsizes on the last line, for a label that is *allowed* two lines
 *                  but still shouldn't grow without bound.
 */
@Composable
fun OverflowTooltipText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    contentAlignment: Alignment = Alignment.TopStart,
    maxLines: Int = 1,
) {
    OverflowTooltipText(
        text = AnnotatedString(text),
        modifier = modifier,
        style = style,
        color = color,
        contentAlignment = contentAlignment,
        maxLines = maxLines,
    )
}

/**
 * [OverflowTooltipText] over styled text — a title with an accented span, say. The on-screen label
 * keeps its spans; the tooltip deliberately shows the plain string, because a span color chosen to
 * read on the surface (an accent primary) is not guaranteed to read on the tooltip card's.
 */
@Composable
fun OverflowTooltipText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    contentAlignment: Alignment = Alignment.TopStart,
    maxLines: Int = 1,
) {
    // Windows draws a flag's two letters unless that run uses the bundled flags font.
    // Null on every other target, where the string is returned unchanged.
    val shown = text.withFlagEmojiFont(LocalFlagEmojiFont.current)
    val tooltip = text.text.withFlagEmojiFont(LocalFlagEmojiFont.current)
    // Reset on text change so a previously-clipped label re-measures against its new content.
    var clipped by remember(text) { mutableStateOf(false) }
    var hovered by remember { mutableStateOf(false) }

    // Centered above the anchor, flipping below if there's no room above — the shared placer; the
    // pointer is not used, so a fixed origin stands in for it.
    val noPointer = remember { mutableStateOf(Offset.Zero) }
    val positionProvider = rememberTipPositionProvider(noPointer) { TipPlacement.BesideAnchor(TipSide.Above) }

    Box(
        contentAlignment = contentAlignment,
        modifier = modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    when (awaitPointerEvent(PointerEventPass.Main).type) {
                        PointerEventType.Enter -> hovered = true
                        PointerEventType.Exit -> hovered = false
                    }
                }
            }
        },
    ) {
        Text(
            // No width modifier: the enclosing Box (carrying [modifier]) forwards its width
            // constraint here, and `softWrap = false` makes us clip to it and report the overflow.
            // Above one line we must wrap instead — otherwise the extra lines could never be used.
            text = shown,
            style = style,
            color = color,
            maxLines = maxLines,
            softWrap = maxLines > 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { clipped = it.hasVisualOverflow },
        )
        if (clipped && hovered) {
            Popup(
                popupPositionProvider = positionProvider,
                properties = PopupProperties(focusable = false),
            ) {
                // The Popup renders in its own layout hierarchy but inherits any enclosing
                // SelectionContainer (e.g. a copyable DataTable cell). Without DisableSelection the
                // tooltip's Text registers as a selectable there, and starting a drag-selection on the
                // anchor then crashes SelectionManager with "layouts are not part of the same
                // hierarchy" (it can't sort selectables across the two hierarchies). The tooltip text
                // shouldn't be selectable anyway.
                // The app's one hover-card chrome ([TooltipCard]) rather than a second hand-rolled
                // Surface: an elision reveal and a rich hover card are the same idea arriving by two
                // routes, and they should not look like two different things. TooltipCard already
                // applies the DisableSelection this used to do itself.
                TooltipCard(maxWidth = OverflowTooltipMaxWidth) {
                    Text(
                        // Plain string plus the flag-font span only. Color spans from the label are
                        // dropped: a color chosen for the surface is not guaranteed on this card.
                        // Soft-wrap under a width cap so multi-segment Why (and other long labels)
                        // stack vertically instead of one ultra-wide tooltip.
                        text = tooltip,
                        style = MaterialTheme.typography.bodySmall,
                        softWrap = true,
                        modifier = Modifier.padding(HoverTooltipHintPadding),
                    )
                }
            }
        }
    }
}
