package plusauri.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** How long the arrival pulse takes to fade. Cosmetic only — nothing waits on it. */
private const val FocusPulseMillis = 650

/**
 * The kit's keyboard focus: a **ring** in the primary color while the node holds focus, and a
 * short **pulse** of fill as focus arrives, so a move made from the keyboard is seen at a glance —
 * Material's own focus tint is a faint overlay that disappears on a tinted chip or a colored bar.
 * WCAG 2.4.7 asks that keyboard focus be visible; this is how every keyboard stop shows it.
 *
 * Put it ahead of the node's click or focusable in the modifier chain, so it observes that focus.
 */
@Composable
fun Modifier.keyboardFocusRing(
    shape: Shape = RoundedCornerShape(6.dp),
    inset: Dp = 0.dp,
): Modifier {
    var focused by remember { mutableStateOf(false) }
    return onFocusChanged { focused = it.isFocused }.focusRingFor(focused, shape, inset)
}

/**
 * Draws [keyboardFocusRing]'s ring and pulse for a node whose focus is known some other way — a
 * Material control's own [androidx.compose.foundation.interaction.InteractionSource], where the
 * ring has to sit on the control's face rather than on the larger touch target Material wraps
 * around it.
 */
@Composable
fun Modifier.focusRingFor(
    focused: Boolean,
    shape: Shape = RoundedCornerShape(6.dp),
    inset: Dp = 0.dp,
): Modifier {
    val pulse = remember { Animatable(0f) }
    // A pulse as focus arrives, and again whenever the reader asks where it is ([FocusBeacon]).
    val pings = FocusBeacon.pings
    LaunchedEffect(focused, pings) {
        if (focused) {
            pulse.snapTo(1f)
            pulse.animateTo(0f, tween(FocusPulseMillis))
        } else {
            pulse.snapTo(0f)
        }
    }
    val color = MaterialTheme.colorScheme.primary
    return drawWithContent {
        drawContent()
        // [inset] keeps a full-width row's ring off its title. The stroke is centered on its
        // path, and a clip to this node (a rounded band, say) cuts the outer half, so the stroke
        // sits another half-width inside that margin.
        val strokePx = 1.5.dp.toPx()
        val insetPx = inset.toPx() + strokePx / 2f
        val inner = Size(
            (size.width - insetPx * 2f).coerceAtLeast(0f),
            (size.height - insetPx * 2f).coerceAtLeast(0f),
        )
        val outline = shape.createOutline(inner, layoutDirection, this)
        translate(left = insetPx, top = insetPx) {
            if (pulse.value > 0f) drawOutline(outline, color.copy(alpha = 0.28f * pulse.value))
            if (focused) drawOutline(outline, color, style = Stroke(width = strokePx))
        }
    }
}

/**
 * Keyboard focus for a full-width divider such as a [RegionSeamDivider]. A primary wash behind the
 * row, in the same shape a ring would have taken, instead of a border around a line that is only a
 * few pixels tall. Arrival and a [FocusBeacon] ping brighten it, then it settles. Put it ahead of [androidx.compose.foundation.focusable], the
 * same way as [keyboardFocusRing].
 */
@Composable
fun Modifier.keyboardFocusTint(
    shape: Shape = RoundedCornerShape(6.dp),
    inset: Dp = 1.dp,
    /** Resting wash alpha. A divider on the page surface needs little; a tinted bar needs more. */
    strength: Float = 0.14f,
): Modifier {
    var focused by remember { mutableStateOf(false) }
    val pulse = remember { Animatable(0f) }
    val pings = FocusBeacon.pings
    LaunchedEffect(focused, pings) {
        if (focused) {
            pulse.snapTo(1f)
            pulse.animateTo(0f, tween(FocusPulseMillis))
        } else {
            pulse.snapTo(0f)
        }
    }
    val color = MaterialTheme.colorScheme.primary
    return onFocusChanged { focused = it.isFocused }.drawWithContent {
        val alpha = if (focused) strength + 0.16f * pulse.value else 0f
        if (alpha > 0f) {
            val insetPx = inset.toPx()
            val inner = Size(
                (size.width - insetPx * 2f).coerceAtLeast(0f),
                (size.height - insetPx * 2f).coerceAtLeast(0f),
            )
            val outline = shape.createOutline(inner, layoutDirection, this)
            translate(left = insetPx, top = insetPx) {
                drawOutline(outline, color.copy(alpha = alpha))
            }
        }
        drawContent()
    }
}
