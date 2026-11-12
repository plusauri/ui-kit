package tutil.ui

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

/**
 * The sizes readers have dragged hover cards to, for the life of the app — one slot per **host**
 * ([HoverDetailOptions.sizeKey], or the options value itself when a host names none).
 *
 * Session-scoped and shared rather than held by each card because a hover card is gone the moment
 * the pointer leaves: a size kept on the one card would be lost before it was ever seen again. With
 * a slot per host, widening one row's card in a table opens the next row's card at that width too.
 * Not persisted: a size is a reading adjustment for the content in front of the reader now, and
 * a remembered giant card on next launch would be a surprise rather than a convenience.
 */
object HoverCardSizes {
    private val sizes = mutableStateMapOf<Any, DpSize>()

    operator fun get(key: Any): DpSize? = sizes[key]

    operator fun set(key: Any, size: DpSize) {
        sizes[key] = size
    }

    /** Forget every slot — tests only. */
    fun clear() = sizes.clear()
}

/**
 * How a [TooltipCard] takes part in a reader's resize — see [HoverDetailOptions.resizable].
 *
 * @param size       The explicit size, or null while the card still hugs its content (the default
 *                   look: nothing changes for a card nobody has resized).
 * @param showGrip   Whether the corner grip is drawn — the host shows it only while the pointer is
 *                   on the card (the only time it can be dragged) or a drag is under way.
 * @param maxSize    The largest the card may be dragged to — the room left in the window below and
 *                   right of the card's pinned corner.
 * @param onResizing A drag started (`true`) or ended (`false`); the host holds the card open and
 *                   still for its duration.
 * @param onResize   The new size, on every drag step.
 */
class TooltipCardResize(
    val size: DpSize?,
    val showGrip: Boolean,
    val maxSize: DpSize,
    val onResizing: (Boolean) -> Unit,
    val onResize: (DpSize) -> Unit,
)

/** Smallest a hover card can be dragged to — enough for a title row and a line or two. */
internal val HoverCardMinSize = DpSize(160.dp, 80.dp)

/** Hover cards' grip: smaller than the help card's, since it sits over content rather than a reserved margin. */
internal val HoverCardGripSize = 20.dp

/** Test tag on a hover card's resize grip. */
const val HoverCardGripTag = "hover-card-resize-grip"
