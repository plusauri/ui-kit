package tutil.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

/**
 * The keys a list or a row of choices acts on beyond the arrows: Ctrl-[LIST_DOWN] and Ctrl-[LIST_UP]
 * step through it as ↓ and ↑ do, and [TOGGLE_ALL] is the All / Clear toggle on a bulk chip row.
 *
 * The kit owns these so a control walks without the product's shortcut table. The product documents
 * the same keys there (`tutil.shortcuts.ControlKey`), and `ListKeysAgreeTest` in `:tutil-ui` holds the
 * two to the same glyphs and Compose keys.
 */
object ListKeys {
    const val LIST_DOWN: Char = 'N'
    const val LIST_UP: Char = 'P'
    const val TOGGLE_ALL: Char = '!'

    /** The Compose key [LIST_DOWN] is. */
    val ListDownKey: Key = Key.N

    /** The Compose key [LIST_UP] is. */
    val ListUpKey: Key = Key.P
}

/**
 * A key-down with no modifier held — the guard a focused control's own keys share, so "no Ctrl,
 * Alt, Cmd or Shift" is written once rather than at every handler.
 */
fun KeyEvent.isPlainKeyDown(): Boolean =
    type == KeyEventType.KeyDown && !isCtrlPressed && !isAltPressed && !isMetaPressed && !isShiftPressed

/** Enter, from either the main block or the keypad. */
val KeyEvent.isEnterKey: Boolean get() = key == Key.Enter || key == Key.NumPadEnter

/**
 * This key as a list of choices reads it: plain Ctrl-N ([ListKeys.LIST_DOWN]) is Down and plain
 * Ctrl-P ([ListKeys.LIST_UP]) is Up, as in Emacs, macOS text and most pickers; every other key is
 * itself. A menu or select list matches on this rather than [key], so the two chords move through
 * every list the same way, and the Help table's `ControlBinding.ListStep` row says so.
 *
 * A focused list consumes them, so they never reach the window. Neither is an app shortcut: Peek
 * moved off Ctrl-P to Ctrl-comma (2026-10-14) so that Ctrl-P can be ↑ wherever ↑ walks.
 */
val KeyEvent.listKey: Key get() = when {
    !isCtrlPressed || isAltPressed || isMetaPressed || isShiftPressed -> key
    key == ListKeys.ListDownKey -> Key.DirectionDown
    key == ListKeys.ListUpKey -> Key.DirectionUp
    else -> key
}

/**
 * A step along a list on a key-down: [Key.DirectionDown] for plain ↓ or plain Ctrl-N, and
 * [Key.DirectionUp] for plain ↑ or plain Ctrl-P ([listKey]). Null for anything else, including
 * Shift-↓, so a range or a caret move is never read as a step. For a surface whose ↑ / ↓ walk
 * items rather than move a caret, where the arrows are a reach on a laptop.
 */
val KeyEvent.listStep: Key? get() {
    if (type != KeyEventType.KeyDown) return null
    val chord = isCtrlPressed && (key == ListKeys.ListDownKey || key == ListKeys.ListUpKey)
    if (!chord && !isPlainKeyDown()) return null
    return listKey.takeIf { it == Key.DirectionDown || it == Key.DirectionUp }
}

/**
 * The key a walk reads: [listStep] when this is one, else [key]. Every handler whose ↑ / ↓ move
 * between controls matches on this, so Ctrl-N / Ctrl-P walk wherever the arrows do (owner,
 * 2026-10-14: the arrows are a reach on a laptop).
 */
val KeyEvent.walkKey: Key get() = listStep ?: key

/** Ctrl, Alt or Cmd is held — but not as Ctrl-N / Ctrl-P, which a walk reads as ↓ / ↑ ([walkKey]). */
val KeyEvent.hasChord: Boolean get() = listStep == null && (isCtrlPressed || isAltPressed || isMetaPressed)
