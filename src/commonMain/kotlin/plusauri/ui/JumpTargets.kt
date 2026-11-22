package plusauri.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequesterModifierNode
import androidx.compose.ui.focus.requestFocus
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.relocation.bringIntoView
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The places a jump can land, so a "show me where this is set" link can take the reader straight
 * to a control. Each place is keyed by the same string that names it everywhere else: a control's
 * [fieldTag] path. Every [fieldTag] registers here while it is on screen, so a control is a jump
 * target the moment it is tagged, with nothing per call site and no list to keep in step.
 *
 * A jump is two steps, and the first is the host's. The host first navigates to the place (opens
 * the right screen, tab and section), then calls [land] with the control's tag. Navigation and
 * the call can happen in the same frame, so [land] waits a few frames for the control to compose.
 * A control the host could not bring on screen (one inside an item the reader has to choose
 * first) is simply never registered; [land] returns null, and the host can say what is left to do.
 *
 * The host provides an instance through [LocalJumpTargets] around the screens a jump can reach,
 * and sets [flashColor] from its theme. A test can provide one to watch landings. The same
 * controls drawn with no provider (in a dialog or a preview that is not a jump destination)
 * register with nothing.
 */
class JumpTargets {
    private val shown = LinkedHashMap<String, MutableList<JumpTargetNode>>()

    /** The outline a landing flashes. Set by the provider from its theme. */
    var flashColor: Color = Color.Unspecified

    /** The key of the last landing, for a host (a test) to watch. */
    var landed by mutableStateOf<String?>(null)
        private set

    /** How many landings there have been, so a host can tell two landings on one key apart. */
    var landings by mutableIntStateOf(0)
        private set

    internal fun register(node: JumpTargetNode) {
        shown.getOrPut(node.key) { mutableListOf() } += node
    }

    internal fun unregister(node: JumpTargetNode) {
        shown[node.key]?.let { nodes ->
            nodes -= node
            if (nodes.isEmpty()) shown -= node.key
        }
    }

    /**
     * The nodes a landing on [key] uses, the first taking focus: the control itself; or when only
     * its options are tagged (a toggle row: `"$key[OPTION]"`, [fieldOption]) every option, so the
     * whole choice flashes; or when only its parts are (an amount with its currency and year:
     * `"$key.value"`, [fieldTagPart]) every part, the value first.
     */
    // A key shown twice is a card and its close-up, or a control and the same one in a dialog over
    // it: the newest is the one in front. Nodes registered as one [group] (the rows of one table
    // that share a key, such as every row of one category in a ledger) land together, so all of
    // the newest node's group flash, the first taking focus.
    private fun nodesFor(key: String): List<JumpTargetNode> =
        shown[key]?.let { nodes ->
            val newest = nodes.last()
            if (newest.group == null) listOf(newest) else nodes.filter { it.group === newest.group }
        }
            ?: shown.filterKeys { it.startsWith("$key[") }.values.map { it.first() }.ifEmpty { null }
            ?: CompoundParts.mapNotNull { part -> shown["$key.$part"]?.first() }

    /** Whether [key] is on screen now. */
    fun isShown(key: String): Boolean = nodesFor(key).isNotEmpty()

    /**
     * Land on [keys]' first entry once it is on screen, waiting up to [frames] frames for it to
     * compose: bring it into view, take focus (its first focusable child, when it is a wrapper — the
     * same rule as [takesFocusHandoff]), and flash an outline once so the eye finds it.
     *
     * The rest of [keys] are fallbacks, nearest first: the Add button an option sits behind, or the
     * section a field's item lives in. One is taken only once the budget has run out
     * without the first: a section the jump opened this frame composes its control a frame or two
     * later, and landing on its header in the meantime would be landing short. Returns the key it
     * landed on, or null when none showed.
     */
    suspend fun land(keys: List<String>, frames: Int = 8): String? {
        if (keys.isEmpty()) return null
        repeat(frames) {
            withFrameNanos { }
            if (nodesFor(keys.first()).isNotEmpty()) return landOn(keys.first())
        }
        return keys.drop(1).firstOrNull { nodesFor(it).isNotEmpty() }?.let { landOn(it) }
    }

    private suspend fun landOn(key: String): String? {
        // Composed last frame; one more lets a just-opened tab or section finish its layout.
        withFrameNanos { }
        val first = nodesFor(key).firstOrNull() ?: return null
        first.bringIntoViewAndFocus()
        nodesFor(key).forEach { it.flash(flashColor) }
        landed = key
        landings++
        return key
    }
}

/** A compound control's tagged parts ([FieldTagPart]), the one that takes focus first. */
private val CompoundParts = listOf(FieldTagPart.Value, FieldTagPart.Currency, FieldTagPart.Year)

/**
 * The place a subtree is part of, by an id the host chooses (`"reports.quarterly"`), so the same
 * line on two reports (a subtotal that appears on both) can be two places. The kit only carries
 * the value: the host provides it around the subtree, registers a row inside it under
 * `jumpTarget(jumpScoped(LocalJumpScope.current, key))`, and builds the keys it passes to
 * [JumpTargets.land] the same way.
 */
val LocalJumpScope = staticCompositionLocalOf<String?> { null }

/** [key] qualified by [scope]: the form a scoped row registers under. */
fun jumpScoped(scope: String?, key: String): String = if (scope == null) key else "$scope>$key"

/** The [JumpTargets] the host provides, or null outside one. */
val LocalJumpTargets = staticCompositionLocalOf<JumpTargets?> { null }

/**
 * Registers this node under [key] as a place a jump can land. [fieldTag] does this for every
 * write control; use it directly only for a place with no write control (a read-side row).
 *
 * [group] ties together the nodes of one surface that share a key, such as the rows of one table,
 * so a landing takes all of them; null for a place that is one node.
 */
fun Modifier.jumpTarget(key: String, group: Any? = null): Modifier = this then JumpTargetElement(key, group)

private data class JumpTargetElement(val key: String, val group: Any?) : ModifierNodeElement<JumpTargetNode>() {
    override fun create() = JumpTargetNode(key, group)

    override fun update(node: JumpTargetNode) {
        node.group = group
        node.rekey(key)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "jumpTarget"
        properties["key"] = key
    }
}

internal class JumpTargetNode(key: String, var group: Any? = null) :
    Modifier.Node(),
    CompositionLocalConsumerModifierNode,
    FocusRequesterModifierNode,
    DrawModifierNode {

    var key: String = key
        private set

    private var targets: JumpTargets? = null

    // Made on the first flash: most tagged controls are never landed on.
    private var flash: Animatable<Float, *>? = null
    private var color: Color = Color.Unspecified

    override fun onAttach() {
        targets = currentValueOf(LocalJumpTargets)?.also { it.register(this) }
    }

    override fun onDetach() {
        targets?.unregister(this)
        targets = null
    }

    fun rekey(next: String) {
        if (next == key) return
        targets?.unregister(this)
        key = next
        targets?.register(this)
    }

    suspend fun bringIntoViewAndFocus() {
        if (!isAttached) return
        bringIntoView()
        runCatching { requestFocus() }
    }

    fun flash(color: Color) {
        if (!isAttached) return
        this.color = color
        val anim = flash ?: Animatable(0f).also { flash = it }
        // The node's own scope, so the flash ends with the control rather than with the jump.
        coroutineScope.launch {
            anim.animateTo(1f, tween(180)) { invalidateDraw() }
            delay(900)
            anim.animateTo(0f, tween(700)) { invalidateDraw() }
        }
    }

    override fun ContentDrawScope.draw() {
        val a = flash?.value ?: 0f
        if (a > 0f && color.isSpecified) {
            val pad = 6.dp.toPx()
            val radius = CornerRadius(10.dp.toPx())
            val topLeft = Offset(-pad, -pad)
            val box = Size(size.width + 2 * pad, size.height + 2 * pad)
            drawRoundRect(color.copy(alpha = 0.10f * a), topLeft, box, radius)
            drawRoundRect(color.copy(alpha = 0.85f * a), topLeft, box, radius, style = Stroke(2.dp.toPx()))
        }
        drawContent()
    }
}

private val Color.isSpecified: Boolean get() = this != Color.Unspecified
