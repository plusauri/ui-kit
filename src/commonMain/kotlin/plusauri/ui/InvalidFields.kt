package plusauri.ui

import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * The fields in an open edit pane that hold a value outside their own range — the half of "what
 * stops OK" that the controls know and the editor's `requiredFields` list does not.
 *
 * **Why.** A ranged number field has always turned red on a value outside its range and then saved
 * it anyway. That is how a birth year of `1063` reached the forecast and crashed the app: the box
 * was red, OK was live. Every ranged [NumericInput] now reports here while it is out of range, and
 * the edit pane ([CollectionEditorComponent]) refuses OK and names the field in its `Needs:` row —
 * across every editor, with no per-editor code. Rules that need two fields at once (retirement after
 * death) stay in the editor's own list; a single control cannot see them.
 *
 * A field withdraws its report when it comes back in range or leaves the composition, so a section
 * the reader folds away does not hold OK hostage.
 */
class InvalidFields {
    /** One field's report. [label] is null for a field with no name of its own. */
    private class Report(val label: String?)

    private val entries = mutableStateMapOf<Any, Report>()

    /**
     * The names of the fields out of range now, in report order, each once. A field with no name
     * of its own reads as [unnamed], which the pane supplies because the pane owns the words; the
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

/** The open pane's [InvalidFields]; null outside one, where a red field is only red. */
val LocalInvalidFields = staticCompositionLocalOf<InvalidFields?> { null }

/**
 * The caption of the [LabeledField] a control sits in. A control that reports itself (to
 * [InvalidFields]) uses it when it has no label of its own, so the `Needs:` pill names the field
 * with the very words on screen.
 */
val LocalFieldCaption = staticCompositionLocalOf<String?> { null }
