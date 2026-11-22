package plusauri.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * A vertically scrollable column that overlays fade gradients at the edges whenever more content
 * exists outside the viewport.
 *
 * Drop-in replacement for a plain `Column(Modifier.verticalScroll(...))` anywhere a scroll-content
 * hint is desirable. Bound the height via [modifier] (e.g. `weight(1f)` or `heightIn(max = …)`);
 * fades only show once content overflows that bound.
 *
 * @param fadeColor Color the edge gradients blend to — set it to the background the column sits on
 *                  so the fade reads as content dissolving into the surface. Defaults to the theme
 *                  surface; a [TooltipCard] passes its own card color, etc.
 * @param fadeHeight Thickness of each edge fade band. Larger values read as a stronger “more below
 *                  / above” cue (a narrow band of tool buttons wants a drastic height, so a
 *                  half-hidden button cannot look simply missing).
 * @param fadeTop When true, paint a top fade while [scrollState] can scroll backward.
 * @param fadeBottom When true, paint a bottom fade while more content remains below.
 * @param fillWidth When true (default) the scrolling column fills the available width — right for a
 *                  panel. Pass false to wrap to content width, so a bounded-but-narrow container
 *                  (e.g. a tooltip capped by `widthIn(max = …)`) hugs its content instead of stretching.
 * @param verticalArrangement Spacing/arrangement of the scrolling children; defaults to
 *                  [Arrangement.Top] (no inter-child gap), matching a plain `Column`.
 * @param horizontalAlignment Cross-axis alignment of children; default [Alignment.Start]. Pass
 *                  [Alignment.CenterHorizontally] when embedding in a NavigationRail, so the
 *                  scrolling band lines up with pinned siblings that the rail centers itself.
 * @param scrollCue When true, a small chevron rides in each fade band while there is more that way,
 *                  and **clicking it pages** the column that way. The fade alone says "this
 *                  continues"; the chevron says "you can get there", and has to keep that promise.
 *                  A chevron that is only drawn invites a click that does nothing, which leaves
 *                  the wheel as the only way down on a short window.
 * @param scrollbar When true, a slim thumb floats over the right edge whenever the content
 *                  overflows, sized and positioned to the scroll. It is drawn, not laid out, so it
 *                  takes no width and never pushes content. The fade says "there is more"; the
 *                  thumb says "and this is how much, and where you are". Without it, a reader who
 *                  never tries to scroll a hover card has no way to know it can. Default off;
 *                  hover cards, dialogs and side panels ([RailPanelScrollColumn]) opt in.
 * @param scrollbarGutter End padding reserved for the thumb, so it rides beside the content rather
 *                  than over its last characters. Zero (the default) is right for a hover card,
 *                  whose own padding is the gutter; a dialog whose content runs to the column's
 *                  edge pairs a gutter with [bleedEnd] so the thumb sits in the dialog's margin.
 */
@Composable
fun FadingScrollColumn(
    modifier: Modifier = Modifier,
    fadeColor: Color = MaterialTheme.colorScheme.surface,
    fadeHeight: Dp = 32.dp,
    fadeTop: Boolean = true,
    fadeBottom: Boolean = true,
    fillWidth: Boolean = true,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    scrollCue: Boolean = false,
    scrollbar: Boolean = false,
    scrollbarGutter: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    val standDown = remember { BottomFadeStandDown() }
    // Draw the fade with drawWithContent so it never steals clicks from content under the strip.
    val cueColor = scrollCueColor()
    Box(
        modifier = modifier.drawWithContent {
            drawContent()
            val fadeHeightPx = fadeHeight.toPx().coerceAtMost(size.height / 2f)
            if (fadeHeightPx <= 0f) return@drawWithContent

            // Strong multi-stop fade: content becomes unreadable well before the hard edge so
            // occluded controls never look “present but tiny.”
            if (fadeTop && scrollState.canScrollBackward) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to fadeColor,
                            0.45f to fadeColor.copy(alpha = 0.92f),
                            1f to Color.Transparent,
                        ),
                        startY = 0f,
                        endY = fadeHeightPx,
                    ),
                    topLeft = Offset.Zero,
                    size = Size(size.width, fadeHeightPx),
                )
                if (scrollCue) drawTopChevron(cueColor)
            }
            if (fadeBottom && scrollState.canScrollForward && !standDown.active) {
                drawBottomScrollCue(fadeColor, fadeHeightPx, chevron = scrollCue, cueColor = cueColor)
            }
        },
    ) {
        CompositionLocalProvider(LocalBottomFadeStandDown provides standDown) {
            Column(
                modifier = (if (fillWidth) Modifier.fillMaxWidth() else Modifier)
                    .padding(end = scrollbarGutter)
                    .verticalScroll(scrollState),
                verticalArrangement = verticalArrangement,
                horizontalAlignment = horizontalAlignment,
                content = content,
            )
        }
        // Composed last so it rides over the fades.
        if (scrollbar) ScrollbarThumb(scrollState)
        if (scrollCue) ScrollCueButtons(scrollState, showDown = fadeBottom && !standDown.active, showUp = fadeTop)
    }
}

/**
 * The scroll column of a whole side panel: a [FadingScrollColumn] that always wears the
 * [ScrollbarThumb], with a gutter so the thumb rides beside the content and never over controls at
 * its right edge, such as accordion chevrons. One wrapper owns that framing, so no panel decides it
 * alone.
 *
 * A panel full of charts or long forms needs more than the fade: the fade says the panel goes on,
 * but only a thumb says how far, and lets the reader grab it.
 */
@Composable
fun RailPanelScrollColumn(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    FadingScrollColumn(
        modifier = modifier,
        verticalArrangement = verticalArrangement,
        scrollbar = true,
        scrollbarGutter = scrollbarGutter(),
        content = content,
    )
}

/**
 * A fixed-size viewport that scrolls [content] vertically and wears the [ScrollbarThumb]: for a
 * region a drag has shrunk below its content (an accordion section, a split pane), where a chart
 * cut off at the bottom would otherwise be the only sign it scrolls. [modifier] sets the size.
 *
 * The content stops [SCROLLBAR_HIT_WIDTH] short of the edge, so the thumb rides in its own gutter:
 * drawn over the content, it would cover the right edge of a card. Every thumb sits beside content,
 * never on it: side panels reserve the same gutter ([RailPanelScrollColumn]), and padded panes and
 * hover cards have padding for it.
 */
@Composable
fun ScrollbarBox(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier) {
        Box(Modifier.fillMaxSize().padding(end = scrollbarGutter()).verticalScroll(scrollState)) { content() }
        ScrollbarThumb(scrollState)
    }
}

/**
 * The click targets over the chevrons [FadingScrollColumn] draws: each pages the column that way.
 * Only the chevron's own small patch takes the pointer, so the half-hidden controls under the rest
 * of the fade stay clickable — the reason the fade is drawn rather than laid out.
 */
@Composable
private fun BoxScope.ScrollCueButtons(scrollState: ScrollState, showDown: Boolean, showUp: Boolean) {
    val words = LocalScrollCueWords.current
    val scope = rememberCoroutineScope()
    fun page(direction: Float) {
        val viewport = scrollState.viewportSize.takeIf { it > 0 } ?: return
        scope.launch { scrollState.animateScrollBy(direction * viewport * SCROLL_PAGE_FRACTION) }
    }
    if (showDown && scrollState.canScrollForward) {
        TipIconButton(
            tip = words.down,
            onClick = { page(1f) },
            size = SCROLL_CUE_HIT_SIZE,
            // The mark is drawn, not an Icon, so the button carries the name itself.
            buttonModifier = Modifier.semantics { contentDescription = words.down },
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = SCROLL_CUE_HIT_INSET),
        ) {}
    }
    if (showUp && scrollState.canScrollBackward) {
        TipIconButton(
            tip = words.up,
            onClick = { page(-1f) },
            size = SCROLL_CUE_HIT_SIZE,
            // The mark is drawn, not an Icon, so the button carries the name itself.
            buttonModifier = Modifier.semantics { contentDescription = words.up },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = SCROLL_CUE_HIT_INSET),
        ) {}
    }
}

/** The chevrons' click patch: a little larger than the 14 × 6 dp mark, so it is easy to hit. */
private val SCROLL_CUE_HIT_SIZE: Dp = 24.dp

/** Pulls the patch in from the edge so the chevron, 10–16 dp in, sits in its middle. */
private val SCROLL_CUE_HIT_INSET: Dp = 1.dp

/** The up chevron, mirroring the one [drawBottomScrollCue] draws in the bottom band. */
private fun DrawScope.drawTopChevron(cueColor: Color) {
    val halfWidth = 7.dp.toPx()
    val drop = 6.dp.toPx()
    val centerX = size.width / 2f
    val topY = 10.dp.toPx()
    drawLine(cueColor, Offset(centerX - halfWidth, topY + drop), Offset(centerX, topY), 2.5.dp.toPx(), StrokeCap.Round)
    drawLine(cueColor, Offset(centerX, topY), Offset(centerX + halfWidth, topY + drop), 2.5.dp.toPx(), StrokeCap.Round)
}

/**
 * The scrollbar of a scrolling viewport: a slim thumb at the end edge, sized to the viewport's
 * share of the content and placed where the viewport is. It can be **dragged**, and a press on the
 * track above or below it pages that way. Shown only while the content overflows.
 *
 * Call it as the last child of the [Box] that holds the scrolling content, so it overlays the
 * content rather than taking width. Only its own narrow strip takes the pointer; everything else
 * passes through to the content under it.
 *
 * The kit's one scrollbar. [FadingScrollColumn] draws it when asked; a host's own scrolling pane
 * can call it directly. A pane whose content can run below the fold should always wear one, or a
 * button below the fold (a dialog's OK, say) can be lost with nothing on screen to say it scrolls.
 */
@Composable
fun BoxScope.ScrollbarThumb(scrollState: ScrollState) {
    val mode = LocalScrollbarMode.current
    if (scrollState.maxValue <= 0 || mode == ScrollbarMode.Never) return
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    var dragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // While scrolling: shown from a scroll until it has been still for a moment, and whenever the
    // pointer is on the strip, so a reader who knows it is there can still reach for it.
    var recentlyScrolled by remember { mutableStateOf(false) }
    if (mode == ScrollbarMode.WhileScrolling) {
        LaunchedEffect(scrollState) {
            snapshotFlow { scrollState.value }.drop(1).collectLatest {
                recentlyScrolled = true
                delay(SCROLLBAR_LINGER_MS)
                recentlyScrolled = false
            }
        }
    }
    val shown = mode == ScrollbarMode.Always || hovered || dragging || recentlyScrolled
    val visibility by animateFloatAsState(if (shown) 1f else 0f, tween(SCROLLBAR_FADE_MS))
    // matchParentSize, so the strip is as tall as the viewport without ever deciding its height.
    Box(Modifier.matchParentSize()) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .fillMaxHeight()
                .width(SCROLLBAR_HIT_WIDTH)
                .hoverable(interaction)
                .pointerInput(scrollState) {
                    detectVerticalDragGestures(
                        onDragStart = { dragging = true },
                        onDragEnd = { dragging = false },
                        onDragCancel = { dragging = false },
                    ) { change, dy ->
                        change.consume()
                        // The thumb's travel maps onto the whole scroll range.
                        val g = thumbGeometry(size.height.toFloat(), scrollState)
                        val travel = g.track - g.length
                        if (travel > 0f) scrollState.dispatchRawDelta(dy * scrollState.maxValue / travel)
                    }
                }
                .pointerInput(scrollState) {
                    detectTapGestures { at ->
                        val g = thumbGeometry(size.height.toFloat(), scrollState)
                        val page = size.height * SCROLL_PAGE_FRACTION
                        when {
                            at.y < g.top -> scope.launch { scrollState.animateScrollBy(-page) }
                            at.y > g.top + g.length -> scope.launch { scrollState.animateScrollBy(page) }
                        }
                    }
                }
                .drawBehind {
                    if (visibility <= 0f) return@drawBehind
                    val g = thumbGeometry(size.height, scrollState)
                    val thumbW = SCROLLBAR_THUMB_WIDTH.toPx() * if (hovered || dragging) 1.5f else 1f
                    val alpha = if (hovered || dragging) SCROLLBAR_THUMB_ACTIVE_ALPHA else SCROLLBAR_THUMB_ALPHA
                    drawRoundRect(
                        color = color.copy(alpha = alpha * visibility),
                        topLeft = Offset(size.width - thumbW - g.inset, g.top),
                        size = Size(thumbW, g.length),
                        cornerRadius = CornerRadius(thumbW / 2f, thumbW / 2f),
                    )
                },
        )
    }
}

/** Where the thumb sits in a viewport [viewport] px tall: its [top], [length], and the [track] it travels. */
private class ThumbGeometry(val top: Float, val length: Float, val track: Float, val inset: Float)

private fun Density.thumbGeometry(viewport: Float, scrollState: ScrollState): ThumbGeometry {
    val inset = SCROLLBAR_INSET.toPx()
    val track = viewport - 2 * inset
    val total = viewport + scrollState.maxValue
    val length = (track * viewport / total).coerceAtLeast(SCROLLBAR_MIN_THUMB.toPx()).coerceAtMost(track)
    val fraction = if (scrollState.maxValue > 0) scrollState.value.toFloat() / scrollState.maxValue else 0f
    return ThumbGeometry(top = inset + (track - length) * fraction, length = length, track = track, inset = inset)
}

/**
 * **Page Up** / **Page Down** scroll [scrollState] by most of a viewport, whatever inside it holds
 * focus. Read on the way *down* the tree ([onPreviewKeyEvent]), so a focused number field, which
 * would otherwise take the key, never sees it. The arrows stay with the fields: a number field
 * steps its value with them.
 */
fun Modifier.scrollPageKeys(scrollState: ScrollState, scope: CoroutineScope): Modifier =
    onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        val direction = when (event.key) {
            Key.PageDown -> 1f
            Key.PageUp -> -1f
            else -> return@onPreviewKeyEvent false
        }
        val viewport = scrollState.viewportSize.takeIf { it > 0 } ?: return@onPreviewKeyEvent false
        scope.launch { scrollState.animateScrollBy(direction * viewport * SCROLL_PAGE_FRACTION) }
        true
    }

/** How much of a viewport one page moves: most of it, so the line at the edge stays in sight. */
private const val SCROLL_PAGE_FRACTION: Float = 0.85f

/**
 * The bottom "there is more" cue of a scrolling viewport: a fade into [fadeColor], and a chevron
 * in it when [chevron] — the fade says the content continues, the chevron says scrolling gets
 * there. The one implementation: [FadingScrollColumn] draws through it, and a host's own
 * scrolling pane can too, so every bottom cue looks the same.
 */
fun DrawScope.drawBottomScrollCue(
    fadeColor: Color,
    fadeHeightPx: Float,
    chevron: Boolean,
    cueColor: Color,
) {
    val visibleBottom = size.height
    // Strong multi-stop fade: content becomes unreadable well before the hard edge so occluded
    // controls never look "present but tiny."
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                0.55f to fadeColor.copy(alpha = 0.92f),
                1f to fadeColor,
            ),
            startY = visibleBottom - fadeHeightPx,
            endY = visibleBottom,
        ),
        topLeft = Offset(0f, visibleBottom - fadeHeightPx),
        size = Size(size.width, fadeHeightPx),
    )
    if (chevron) {
        // Centered in the fade band, where the gradient is already opaque enough that it never
        // sits on top of legible controls.
        val halfWidth = 7.dp.toPx()
        val drop = 6.dp.toPx()
        val centerX = size.width / 2f
        val bottomY = visibleBottom - 10.dp.toPx()
        drawLine(cueColor, Offset(centerX - halfWidth, bottomY - drop), Offset(centerX, bottomY), 2.5.dp.toPx(), StrokeCap.Round)
        drawLine(cueColor, Offset(centerX, bottomY), Offset(centerX + halfWidth, bottomY - drop), 2.5.dp.toPx(), StrokeCap.Round)
    }
}

/**
 * Lets something riding a scroller's bottom edge, such as a sticky card pinned there, stand the
 * scroller's own bottom fade down while it rides. The card draws its own "more below" cue
 * above itself; the scroller's fade would otherwise wash out the card's bottom row.
 */
class BottomFadeStandDown {
    var active by mutableStateOf(false)
}

val LocalBottomFadeStandDown = staticCompositionLocalOf<BottomFadeStandDown?> { null }

/** The chevron's ink in [drawBottomScrollCue]. */
@Composable
fun scrollCueColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

/**
 * When the [ScrollbarThumb]s show, typically a user setting. Every thumb reads
 * [LocalScrollbarMode], so the choice is one value read in one place.
 *
 * [ScrollbarMode.defaultFor] gives a sensible default before the person chooses: a typical user is
 * the reader who may not know a panel scrolls, so their scrollbars always show; an experienced user
 * knows, and gets the quieter macOS habit.
 */
enum class ScrollbarMode {
    /** Shown whenever the content overflows. */
    Always,

    /** Shown while scrolling or under the pointer, then faded after [SCROLLBAR_LINGER_MS]. */
    WhileScrolling,

    /** Never drawn, and the gutters this file reserves for it ([scrollbarGutter]) close up. */
    Never,
    ;

    companion object {
        fun defaultFor(powerUser: Boolean): ScrollbarMode = if (powerUser) WhileScrolling else Always
    }
}

/** The person's [ScrollbarMode], already resolved against any default. The host provides it. */
val LocalScrollbarMode = compositionLocalOf { ScrollbarMode.Always }

/**
 * The end gutter a column keeps for its thumb: [SCROLLBAR_HIT_WIDTH], or nothing when scrollbars
 * are off. Only for gutters this file owns ([RailPanelScrollColumn], [ScrollbarBox]); a caller that
 * pairs its own gutter with a bleed keeps it, since closing one without the other would push its
 * content into the margin.
 */
@Composable
internal fun scrollbarGutter(): Dp =
    if (LocalScrollbarMode.current == ScrollbarMode.Never) 0.dp else SCROLLBAR_HIT_WIDTH

/** How long a [ScrollbarMode.WhileScrolling] thumb stays after the content stops moving. */
private const val SCROLLBAR_LINGER_MS: Long = 1000L

/** The fade in and out of a [ScrollbarMode.WhileScrolling] thumb. */
private const val SCROLLBAR_FADE_MS: Int = 300

/** Thumb width of [ScrollbarThumb] at rest. */
private val SCROLLBAR_THUMB_WIDTH: Dp = 4.dp

/** Gap between the thumb and the column's right edge, and between the thumb's travel and top/bottom. */
private val SCROLLBAR_INSET: Dp = 3.dp

/** A very long list would otherwise shrink the thumb to a dot that reads as a speck, not a control. */
private val SCROLLBAR_MIN_THUMB: Dp = 18.dp

/** Visible against both a frosted chart tooltip and a themed surface card without shouting over content. */
private const val SCROLLBAR_THUMB_ALPHA: Float = 0.45f

/** Hovered or held: the thumb says it is a control. */
private const val SCROLLBAR_THUMB_ACTIVE_ALPHA: Float = 0.7f

/**
 * Width of the strip that takes the pointer — wider than the thumb, which is too thin to hit. Kept
 * under a typical 16 dp content padding, so it never covers a control. Also the gutter a column
 * reserves when its content runs to the edge ([RailPanelScrollColumn], a padded dialog).
 */
val SCROLLBAR_HIT_WIDTH: Dp = 12.dp

/**
 * Lets a child extend [amount] past its parent's end edge — into padding the parent applied —
 * while reporting its original width, so siblings are laid out as if nothing changed. Built for a
 * [FadingScrollColumn] inside a padded dialog: the column bleeds into the dialog's end margin and
 * pads its own content back by the same [scrollbarGutter][FadingScrollColumn], so the text stays
 * aligned with the header and the floating thumb rides in the margin, at the dialog's edge.
 */
fun Modifier.bleedEnd(amount: Dp): Modifier = layout { measurable, constraints ->
    val extra = amount.roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = if (constraints.hasBoundedWidth) constraints.minWidth + extra else constraints.minWidth,
            maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + extra else constraints.maxWidth,
        ),
    )
    layout((placeable.width - extra).coerceAtLeast(0), placeable.height) { placeable.place(0, 0) }
}
