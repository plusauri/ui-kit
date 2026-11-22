package plusauri.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * Tags a form control with a stable **path** the host chooses, so a UI test can address the
 * control by identity instead of by its position on screen or by its caption.
 *
 * ## Why not position or caption
 *
 * `onAllNodes(hasSetTextAction())[0]` is positional: it silently re-targets the moment a field is
 * added above it, and the test still passes while asserting about the wrong control.
 *
 * `onNodeWithText("Annual salary")` couples the test to a display string. It breaks when the copy
 * is reworded or translated, and when the test reads the caption from the same string table the
 * UI reads, a wrong-key bug passes on both sides.
 *
 * A path that names the data the control edits survives all of that: it changes only when the
 * underlying model changes, which is the one case where the test genuinely should be revisited.
 *
 * ## A suggested convention
 *
 * The kit does not interpret the path, so any stable scheme works. One that reads well is
 * snake-case field names joined by `.`, rooted at the record the form writes, with list elements
 * indexed:
 *
 * ```
 * "name"                        // Contact.name
 * "address.postal_code"         // a field of a nested record
 * "phone_numbers[0].kind"       // a field of the first list element
 * ```
 *
 * The sub-parts of a compound control (an amount with its currency and as-of year) are suffixed by
 * the compound itself ([FieldTagPart], [fieldTagPart]), so a call site names the field once.
 *
 * Tags are kept in every build, and they are cheap enough to add as each control is touched rather
 * than in one sweep.
 *
 * A tag is also a place a jump can land: the control registers under its path with the
 * surrounding [JumpTargets] while it is on screen ([jumpTarget]), so [JumpTargets.land] on that
 * path brings the control into view, focuses it and flashes it. Put the tag **ahead of** the
 * control's `clickable` / `focusable` in a modifier chain: the landing hands focus to the first
 * focusable *below* the tag, so a tag placed after them flashes the control but cannot focus it.
 *
 * The query side (finding a node by tag and acting on it) lives in the host's own test helpers.
 */
fun Modifier.fieldTag(path: String): Modifier = this.testTag(path).jumpTarget(path)

/** Sub-part tags of a compound control (see [fieldTag]). */
object FieldTagPart {
    const val Value = "value"
    const val Currency = "currency"
    const val Year = "year"
}

/** `"$path.$part"`, or null when [path] is null — for compound controls with optional tagging. */
fun fieldTagPart(path: String?, part: String): String? = path?.let { "$it.$part" }

/**
 * The tag of **one option** of a choice control: `"$path[$key]"`.
 *
 * A choice is addressed by the *identity* of the option, never by its index and never by its
 * caption: an enum constant's name, or a record's id. The index re-targets when an option is added
 * above it, and the caption changes with the copy and the locale. The key is what the control
 * writes to the model when the option is picked, which is the only stable name it has.
 *
 * ```
 * "contact.kind[COMPANY]"          // a button of a MultiButtonSelect toggle row
 * "items.add[Invoice]"             // an entry of an "add" menu
 * "task.assignee_id[user_42]"      // a chip in a picker of people
 * ```
 *
 * The host's test helpers query by this form to pick an option.
 */
fun fieldOption(path: String, key: String): String = "$path[$key]"

/** [fieldOption], or null when [path] is null — for controls with optional tagging. */
fun fieldOptionOrNull(path: String?, key: String): String? = path?.let { fieldOption(it, key) }
