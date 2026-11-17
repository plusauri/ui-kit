package plusauri.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

/**
 * Focus asked for by keyboard, held until the control that should take it is on screen. The same
 * shape as [RunRequest], for the same reason: Enter on a section header opens it and asks for a
 * chip that does not exist until the section has composed.
 *
 * Taken once. A control that composes later — the reader navigates back — does not take it again.
 */
class FocusHandoff {
    var pending by mutableStateOf(false)
        private set

    fun request() { pending = true }

    fun taken() { pending = false }
}

/**
 * This node takes focus when [handoff] is requested — or its first focusable child does, when the
 * node is a wrapper. Null is a no-op, so a caller can pass a handoff that only some hosts have.
 */
@Composable
fun Modifier.takesFocusHandoff(handoff: FocusHandoff?): Modifier {
    if (handoff == null) return this
    val requester = remember { FocusRequester() }
    val pending = handoff.pending
    LaunchedEffect(pending) {
        if (!pending) return@LaunchedEffect
        // The section this sits in has only just opened, and the key that opened it is still
        // being dispatched. One frame is not enough for a heavy section: Modifications on a
        // large plan is still in its first layout passes, and a focus taken then is dropped
        // when that layout settles. A few frames, once, then give up.
        repeat(3) { withFrameNanos { } }
        if (!handoff.pending) return@LaunchedEffect
        runCatching { requester.requestFocus() }
        handoff.taken()
    }
    return focusRequester(requester)
}
