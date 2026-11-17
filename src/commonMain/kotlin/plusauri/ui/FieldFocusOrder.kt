package plusauri.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/**
 * The text fields drawn inside one region, in reading order — so a keyboard move *into* the region
 * lands on its first **field**, not on whatever focusable happens to come first. A Modification
 * editor with a chart preview on top otherwise put the caret on the chart's copy button, and the
 * Enter meant to go back to the list copied the chart instead (owner, 2026-09-27).
 *
 * Provide one with [LocalFieldFocusOrder]; the text-entry primitives ([NumericInput],
 * [TightTextField]) join it through [fieldFocusStop].
 */
class FieldFocusOrder {
    internal class Field {
        val focus = FocusRequester()
        var x = 0f
        var y = 0f
    }

    private val fields = mutableListOf<Field>()

    internal fun add(field: Field) { fields += field }

    internal fun remove(field: Field) { fields -= field }

    /** Focus the first field in reading order (top to bottom, then left to right); false when none. */
    fun focusFirst(): Boolean {
        val first = fields.minWithOrNull(compareBy<Field>({ it.y }, { it.x })) ?: return false
        return runCatching { first.focus.requestFocus() }.isSuccess
    }
}

/** The region whose fields are being ordered; null outside one. */
val LocalFieldFocusOrder = staticCompositionLocalOf<FieldFocusOrder?> { null }

/** Joins the enclosing [FieldFocusOrder], if any — for a text-entry primitive's own modifier chain. */
@Composable
fun Modifier.fieldFocusStop(): Modifier {
    val order = LocalFieldFocusOrder.current ?: return this
    val field = remember { FieldFocusOrder.Field() }
    DisposableEffect(order) {
        order.add(field)
        onDispose { order.remove(field) }
    }
    return focusRequester(field.focus).onGloballyPositioned {
        val p = it.positionInRoot()
        field.x = p.x
        field.y = p.y
    }
}
