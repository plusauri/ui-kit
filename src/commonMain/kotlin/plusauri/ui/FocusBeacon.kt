package plusauri.ui

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
 * which a reader is most lost — nothing can pulse, so it counts as [unanswered], and the host can
 * show a message saying how to get focus back.
 *
 * The kit does not listen for the key itself. The host's window-level key handler checks
 * [isWhereAmI] and calls [ping] when a control holds focus, or [nothingFocused] when none does.
 * It should do so only where nothing else wants the key: a text field types its `?` as always.
 *
 * This is process-wide state: every window in the process shares one beacon.
 */
object FocusBeacon {
    /**
     * The key that asks. The kit owns it so the ring works without a host shortcut table. A host
     * that lists its shortcuts should read the key from here, or test that its list agrees.
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
