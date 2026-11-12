package tutil.ui

import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged

/**
 * Which unanswered fields the form should point at right now.
 *
 * Empty until the user reaches for a disabled OK. Then [revealed] is the **full** missing list —
 * not the truncated `Needs: …` hint — so `+2 more` still has somewhere to land, and [first] is
 * the one to focus.
 *
 * Field identity is the label [requiredFields] already uses: the same string the control draws.
 * A parallel key would be a second name for that string. Controls opt in with
 * `missingFieldLabel` (on the input primitives) or [Modifier.pointAtMissingField].
 *
 * [generation] increments each time the user *clicks* the dead OK or one of the `Needs:` pills.
 * Hover lights the fields without moving focus — stealing the caret on a mouse-over is worse
 * than leaving the field off-screen. A click is sticky and **focuses** [target], which also
 * scrolls it into view: the dead OK targets the first missing field, and each pill targets its
 * own, so the pills read as a row of "next" buttons rather than one label about the first.
 */
data class MissingFieldPointer(
    val revealed: Set<String> = emptySet(),
    val target: String? = null,
    val generation: Int = 0,
    /** Where the pane learns which pointable field holds focus, for [AppShortcut.NextMissing]. */
    val focus: MissingFieldFocus? = null,
) {
    companion object {
        val None: MissingFieldPointer = MissingFieldPointer()
    }
}

/**
 * The label of the pointable field that holds focus right now, or null. Written by
 * [pointAtMissingField] as focus moves; read by the edit pane when Ctrl-J asks for the next
 * missing field, which is the first one that is not this. Plain, not state: nothing draws from it.
 */
class MissingFieldFocus {
    var label: String? = null
}

val LocalMissingFieldPointer = compositionLocalOf { MissingFieldPointer.None }

/** True when [label] is one of the fields OK has been asked to point at. */
@Composable
fun isMissingFieldPointed(label: String?): Boolean {
    if (label.isNullOrEmpty()) return false
    return label in LocalMissingFieldPointer.current.revealed
}

/**
 * Focuses this control when it is the pointer's [MissingFieldPointer.target] and the user has
 * clicked the disabled OK or this field's pill. Focus is what actually answers "where?": the caret lands in the field, and
 * the focused node bringing itself into view is a side effect, not the point.
 *
 * Hover-to-preview does not focus. [focusHost] is for wrappers that are not themselves a
 * text field or other focus target (an outlined group, a chip-row Box) and need a target
 * so [FocusRequester.requestFocus] has somewhere to land.
 *
 * Safe to call with a null [label] (no-op besides a cheap composition); hooks always run so
 * callers can pass a label that is only sometimes set.
 */
@Composable
fun Modifier.pointAtMissingField(label: String?, focusHost: Boolean = false): Modifier {
    val pointer = LocalMissingFieldPointer.current
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(pointer.generation) {
        // Only on an explicit click (generation bump). Keying this on [target] as well would
        // steal the caret the moment the current field became non-empty — typing "H" of
        // "House" would jump to the next missing control.
        if (!label.isNullOrEmpty() && pointer.generation > 0 && pointer.target == label) {
            // Next frame: a same-frame requestFocus loses to the click that triggered us.
            withFrameNanos { }
            runCatching { focusRequester.requestFocus() }
        }
    }
    if (label.isNullOrEmpty()) return this
    val focus = pointer.focus
    val focused = this
        .onFocusChanged { state ->
            when {
                focus == null -> {}
                state.hasFocus -> focus.label = label
                focus.label == label -> focus.label = null
            }
        }
        .focusRequester(focusRequester)
    return if (focusHost) focused.focusable() else focused
}

/**
 * Whether an unanswered field should draw as an error.
 *
 * Untouched and un-pointed stays calm — red-on-load for a blank form is worse than the hint
 * beside OK. Touch is the existing gate; pointing is the on-demand middle setting: the field
 * lights up when the user reaches for the dead button.
 */
fun missingFieldIsError(unanswered: Boolean, touched: Boolean, pointed: Boolean): Boolean =
    unanswered && (touched || pointed)
