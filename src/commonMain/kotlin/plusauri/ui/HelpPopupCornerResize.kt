package plusauri.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val HelpCornerResizeSize = 28.dp

/**
 * Bottom-right corner grip (same visual language as Bird's Eye dialog resize). Shared with
 * [HelpDialogScaffold] so the inline `?` card and the modal help dialogs are dragged the same way,
 * and with [TooltipCard] for the resizable hover cards ([HoverDetailOptions.resizable]).
 *
 * The callbacks are read through updated state: the drag detector is installed once, and a host
 * whose callbacks close over per-composition values (a hover card's current size) must not have
 * the first composition's lambdas frozen into it.
 */
@Composable
fun HelpPopupCornerResize(
    onDragPx: (dx: Float, dy: Float) -> Unit,
    modifier: Modifier = Modifier,
    gripSize: Dp = HelpCornerResizeSize,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val ridge = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (hovered) 0.85f else 0.55f)
    val wash = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (hovered) 0.12f else 0.06f)
    val onDragNow by rememberUpdatedState(onDragPx)
    val onStartNow by rememberUpdatedState(onDragStart)
    val onEndNow by rememberUpdatedState(onDragEnd)

    Box(
        modifier = modifier
            .size(gripSize)
            .hoverable(interaction)
            .pointerHoverIcon(PointerIcon.Crosshair)
            .background(wash, RoundedCornerShape(topStart = 8.dp))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onStartNow() },
                    onDragEnd = { onEndNow() },
                    onDragCancel = { onEndNow() },
                ) { change, dragAmount ->
                    change.consume()
                    onDragNow(dragAmount.x, dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(gripSize / 2)) {
            val stroke = 2.dp.toPx()
            val step = size.minDimension / 4f
            for (i in 0..2) {
                val o = step * (i + 1)
                drawLine(
                    color = ridge,
                    start = Offset(size.width - o, size.height),
                    end = Offset(size.width, size.height - o),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}
