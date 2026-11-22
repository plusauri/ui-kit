package plusauri.ui

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The fields in an open edit form that hold a value outside their own range: the half of "what
 * should block saving" that the controls know and the form's own list of required fields does not.
 *
 * **Why.** A field that only turns red on an out-of-range value still lets the form save it, so a
 * nonsense value (a year of `1063`) reaches code that was never meant to see it. Every ranged
 * [NumericInput] reports here while it is out of range. The host provides an instance through
 * [LocalInvalidFields] around the form, and the host's form reads [labels] to disable its OK
 * button and name the fields that need attention, with no per-form code. Rules that need two
 * fields at once (an end date before a start date) stay in the form's own checks; a single
 * control cannot see them.
 *
 * A field withdraws its report when it comes back in range or leaves the composition, so a section
 * the reader folds away does not keep the form from saving.
 */
class InvalidFields {
    /** One field's report. [label] is null for a field with no name of its own. */
    private class Report(val label: String?)

    private val entries = mutableStateMapOf<Any, Report>()

    /**
     * The names of the fields out of range now, in report order, each once. A field with no name
     * of its own reads as [unnamed], which the form supplies because the form owns the words; the
     * control that reported it does not.
     */
    fun labels(unnamed: String): List<String> = entries.values.map { it.label ?: unnamed }.distinct()

    /** Report the field identified by [key] as invalid, named [label], or unnamed with null. */
    fun report(key: Any, label: String?) {
        entries[key] = Report(label)
    }

    /** Withdraw the report for the field identified by [key], if there is one. */
    fun withdraw(key: Any) {
        entries.remove(key)
    }
}

/**
 * The open form's [InvalidFields], provided by the host. Null outside one, where a red field is
 * only red.
 */
val LocalInvalidFields = staticCompositionLocalOf<InvalidFields?> { null }

/**
 * The caption of the [LabeledField] a control sits in. A control that reports itself (to
 * [InvalidFields]) uses it when it has no label of its own, so the host can name the field with
 * the very words on screen.
 */
val LocalFieldCaption = staticCompositionLocalOf<String?> { null }
