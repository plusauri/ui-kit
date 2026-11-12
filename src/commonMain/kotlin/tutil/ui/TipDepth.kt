package tutil.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How much a hover card stands off the page — the **one** configuration for every hover-tip
 * family: canvas chart readouts ([drawChartTooltipChrome]) and Compose cards ([TooltipCardSurface]).
 *
 * - [Flat] — the chrome as it was before 2026-09-23: a 1 px border and a single hard drop shadow.
 * - [Raised] — a little physical depth: a soft, layered shadow that falls further below than to the
 *   side (light from above), a faint top-lit sheen across the fill, and a beveled edge — lighter
 *   along the top, darker along the bottom. Subtle on purpose: a card is paper lifted off the
 *   chart, not a button to press.
 *
 * Switch with [HoverTipDepth], the one global value; nothing else reads a depth.
 */
enum class TipDepth(
    /** Compose [androidx.compose.material3.Surface] shadow for Family B/C cards. */
    internal val composeShadow: Dp,
    /** Top-of-card white wash, as alpha; 0 draws no sheen. */
    internal val sheenAlpha: Float,
    /** Bottom-of-card shade, as black alpha; 0 draws none. */
    internal val shadeAlpha: Float,
    /** Whether the border is lit from above (light top edge, dark bottom edge). */
    internal val bevel: Boolean,
) {
    Flat(composeShadow = 4.dp, sheenAlpha = 0f, shadeAlpha = 0f, bevel = false),
    Raised(composeShadow = 10.dp, sheenAlpha = 0.14f, shadeAlpha = 0.05f, bevel = true),
}

/**
 * The depth every hover card draws with — **the** global setting, app-wide. Defaults to
 * [TipDepth.Raised]; assign [TipDepth.Flat] for the old flat chrome.
 *
 * Snapshot state rather than a composition local on purpose: the canvas chart tips paint in a
 * `DrawScope` that cannot read a local, and a state read there still invalidates the draw — so a
 * change here repaints every open card of every family. A settings toggle can bind to it directly.
 */
var HoverTipDepth: TipDepth by mutableStateOf(TipDepth.Raised)

/**
 * Face gradient for a card spanning [top]..[bottom] px: sheen fading out by 40% of the height,
 * shade fading in over the bottom 40%. Null when the depth draws neither.
 */
internal fun TipDepth.faceBrush(top: Float, bottom: Float): Brush? {
    if (sheenAlpha == 0f && shadeAlpha == 0f) return null
    return Brush.verticalGradient(
        0f to Color.White.copy(alpha = sheenAlpha),
        0.4f to Color.Transparent,
        0.6f to Color.Transparent,
        1f to Color.Black.copy(alpha = shadeAlpha),
        startY = top,
        endY = bottom,
    )
}

/**
 * Border brush for [edge]: lit from above when [TipDepth.bevel], plain otherwise. Top is [edge]
 * lifted halfway to white; bottom is [edge] pushed a third of the way to black.
 */
internal fun TipDepth.edgeBrush(edge: Color, top: Float, bottom: Float): Brush =
    if (!bevel) {
        SolidColor(edge)
    } else {
        Brush.verticalGradient(
            0f to lerp(edge, Color.White, 0.5f),
            1f to lerp(edge, Color.Black, 0.33f),
            startY = top,
            endY = bottom,
        )
    }

/**
 * Canvas drop shadow for a card at ([x], [y]) sized [w]×[h]. [Flat] is the historical single hard
 * rect at (+2, +3) in [flatColor]; [Raised] stacks three widening, fainter rects that fall mostly
 * downward, which reads as a soft shadow without a blur pass (a canvas blur is not portable here).
 */
internal fun DrawScope.drawTipShadow(
    depth: TipDepth,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    radius: Float,
    flatColor: Color,
) {
    if (depth == TipDepth.Flat) {
        drawRoundRect(flatColor, Offset(x + 2f, y + 3f), Size(w, h), CornerRadius(radius))
        return
    }
    // Spread, drop and weight per layer: outermost is widest, lowest and faintest.
    val base = flatColor.alpha
    listOf(
        Triple(4f, 7f, 0.35f),
        Triple(2f, 4f, 0.55f),
        Triple(0.5f, 2f, 0.9f),
    ).forEach { (spread, drop, weight) ->
        drawRoundRect(
            flatColor.copy(alpha = base * weight),
            Offset(x - spread + 1f, y - spread * 0.5f + drop),
            Size(w + spread * 2f, h + spread),
            CornerRadius(radius + spread),
        )
    }
}

/** Face gradient over a drawn card; no-op for a depth with none. */
internal fun DrawScope.drawTipFace(depth: TipDepth, x: Float, y: Float, w: Float, h: Float, radius: Float) {
    val brush = depth.faceBrush(y, y + h) ?: return
    drawRoundRect(brush, Offset(x, y), Size(w, h), CornerRadius(radius))
}

/** 1 px edge, beveled when the depth says so. */
internal fun DrawScope.drawTipEdge(depth: TipDepth, edge: Color, x: Float, y: Float, w: Float, h: Float, radius: Float) {
    drawRoundRect(depth.edgeBrush(edge, y, y + h), Offset(x, y), Size(w, h), CornerRadius(radius), style = Stroke(1f))
}
