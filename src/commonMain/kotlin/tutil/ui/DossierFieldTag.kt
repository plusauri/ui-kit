package tutil.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * Marks a dossier write control with the **proto field path** it edits, so a UI test can address
 * it by identity instead of by position or by its English caption.
 *
 * ## Why not the two things the existing tests do
 *
 * `onAllNodes(hasSetTextAction())[0]` is positional: it silently re-targets the moment a field is
 * added above it, and the test still passes while asserting about the wrong control.
 *
 * `onNodeWithText("Annual gross wages")` couples the test to an English source string — which is
 * exactly what this codebase's i18n work is trying to get away from, and worse, it asserts against
 * the *same* `AppStrings` table the UI reads, so a wrong-key bug passes both sides. It also breaks
 * under the `LabeledField` migration, which is about to rewrite every caption in the dossier.
 *
 * A tag keyed to the proto path survives all of that: it changes only when the *model* changes,
 * which is the one case where the test genuinely should be revisited.
 *
 * ## The convention
 *
 * Snake-case proto field names joined by `.`, rooted at the message the editor writes, with
 * repeated fields indexed:
 *
 * ```
 * "name"                              // FlowGenerator.name
 * "earned_income.annual_amount.value" // …annual_amount is a YearCurrencyValue triple
 * "worth.ownership_costs[0].kind"
 * ```
 *
 * Sub-parts of a compound control (`YearCurrencyValueInput`'s currency / value / as-of year) are
 * suffixed by the compound itself, so a call site names the field once.
 *
 * Tags are stripped from no build, and cheap enough to add as a control is touched rather than in
 * one sweep.
 *
 * A tag is also where a Glossary *Where it is* jump lands: the control registers under it with
 * the plan's [JumpTargets] while it is on screen ([jumpTarget]), so a catalog path whose
 * `fieldTag` names it ends on the control itself. Put the tag **ahead of** the control's
 * `clickable` / `focusable` in a modifier chain: the landing hands focus to the first focusable
 * *below* the tag, so one placed after them flashes the control but cannot focus it.
 *
 * The query side is `DossierEditorRobot`, in the product's tests.
 */
fun Modifier.dossierField(path: String): Modifier = this.testTag(path).jumpTarget(path)

/** Sub-part tags of a compound control — see [dossierField]. */
object DossierFieldPart {
    const val Value = "value"
    const val Currency = "currency"
    const val Year = "year"
}

/** `"$path.$part"`, or null when [path] is null — for compound controls with optional tagging. */
fun dossierFieldPart(path: String?, part: String): String? = path?.let { "$it.$part" }

/**
 * The tag of **one option** of a choice control: `"$path[$key]"`.
 *
 * A choice is addressed by the *identity* of the option, never by its index and never by its
 * caption — an enum constant's name, a person's id. The index re-targets when an option is added
 * above it; the caption is the same `AppStrings` table the UI reads. The key is what the control
 * writes to the proto when the option is picked, which is the only stable name it has.
 *
 * ```
 * "person.person_type[SECONDARY_OWNER]"   // a MultiButtonSelect toggle
 * "flow_generators.add[Pension]"          // a cell of the cash-flow add menu
 * "pension.person_id[per_alex]"           // a PersonSelect chip
 * ```
 *
 * The query side is `DossierEditorRobot.choose`, in the product's tests.
 */
fun dossierOption(path: String, key: String): String = "$path[$key]"

/** [dossierOption], or null when [path] is null — for controls with optional tagging. */
fun dossierOptionOrNull(path: String?, key: String): String? = path?.let { dossierOption(it, key) }
