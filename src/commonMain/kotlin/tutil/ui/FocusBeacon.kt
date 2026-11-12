package tutil.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint

/**
 * **Where am I?** A plain `?` pulses whatever holds keyboard focus, so a reader who has lost track
 * of it sees it again ([keyboardFocusRing] answers [pings]). When nothing holds focus — the state in
 * which a reader is most lost — nothing can pulse, so the window counts it as [unanswered] and the
 * Simulator says so, with how to start again.
 *
 * Taken only where nothing else wants the key: a text field types its `?` as always.
 */
object FocusBeacon {
    /**
     * The key that asks. The kit owns it so the ring works without the product's shortcut table;
     * the product documents the same key there (`tutil.shortcuts.ControlKey.WhereAmI`), and a test
     * in `:tutil-ui` holds the two to the same glyph.
     */
    const val WHERE_AM_I: Char = '?'

    /** Bumped by a `?` that reached a focused control. */
    var pings by mutableIntStateOf(0)
        private set

    /** Bumped by a `?` that found nothing focused. */
    var unanswered by mutableIntStateOf(0)
        private set

    fun ping() { pings++ }

    fun nothingFocused() { unanswered++ }
}

/** A plain `?` key-down: no Ctrl, Alt or Cmd (Shift is how `?` is typed). */
fun KeyEvent.isWhereAmI(): Boolean =
    type == KeyEventType.KeyDown && !isCtrlPressed && !isAltPressed && !isMetaPressed &&
        utf16CodePoint == FocusBeacon.WHERE_AM_I.code
