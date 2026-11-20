package plusauri.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A compact, clickable pill for a small inline action beside a control — **Clear**, **Remove**,
 * **Reset** — drawn in the same style as the status pills on the Forecast Detail headers
 * (`StatusPill`): a faint tint of [color] on a rounded **rectangle** (not a stadium), a solid
 * [color] swatch leading, and the label in the theme's `onSurface` so it stays legible over any tint.
 *
 * **Why a new control and not one of the existing ones.** Material's `TextButton` has no edge, so in
 * a tight row it wraps its own label letter by letter. `OutlinedButton` is control-height and reads
 * as a peer of the fields, which a one-word side action is not. [CompactHeaderActionChip] is the
 * stadium-shaped filled chip the header bands use — the right family for a band, but beside a field
 * the oval reads heavier than the field it serves. The status pill's look — tint, small radius,
 * swatch — was the one already judged attractive, so an action in that shape is one vocabulary
 * fewer, not one more.
 *
 * **It is the inline-action rule.** Every small side action beside a field or an item is one of
 * these, never an inline `TextButton`. Among the other button families: a dialog's answer is a
 * [DialogConfirmButton], a back-out or any other footer choice a [DialogDismissButton], and a
 * small action on the thing beside it an [ActionPill].
 *
 * **The color says what kind of action it is**, not where it sits — [ActionKind]. Three kinds, three
 * hues, resolved in one place ([ActionKind.color]) so a Remove is the same red on every editor and
 * the tint is never re-decided at a call site. This is a different axis from the item-type accent
 * bars (Rule 6), which color *regions*; a pill inside an amber real-asset region is not amber.
 */
/**
 * What an [ActionPill] does to the thing beside it, which is what decides its color.
 *
 * - [Add] — creates something: a row, a link, a phase. **Blue**, the theme primary: additive,
 *   the same hue the comfort band and the brand use for "good".
 * - [Clear] — un-answers a question without destroying anything: a link unset, a picker back to
 *   "not set". **Neutral** (`onSurfaceVariant`): quiet on purpose. Amber was tried first and
 *   rejected on two counts — it is the app's *warning* tone, and Rule 5's carve-out built Clear
 *   precisely so un-answering is a comfortable, legitimate move; and amber is also the real-asset
 *   region color, so a Clear beside a real-asset picker read as "belongs to the asset" rather than
 *   "caution". Gray keeps the trio's logic (blue adds, red destroys, gray un-answers) without
 *   borrowing either meaning.
 * - [Remove] — deletes a thing that exists: a cost row, a contribution, an heir. **Red**, the
 *   theme error color: the same channel every destructive affordance uses.
 * - [Reset] — puts a value back to what it was or to its default: an override reverted, a set of
 *   shares normalized. **Neutral**, like [Clear] — nothing is lost that a reader did not ask to
 *   lose — but its own glyph, because "back to default" and "no answer" are different promises.
 * - [Toggle] — switches a thing on or off, and is drawn [selected] when on: the COLO × / Wage ×
 *   column switches on the states table. **Primary**, like [Add] — the same soft blue the rest of
 *   the pill family wears, not the graphite secondary it wore first, which read as a disabled gray.
 *   Its swatch is a **box**: ticked when on, empty when off, so the state is legible without
 *   comparing tints. (A check drawn in both states said nothing.)
 * - [Go] — takes the reader to where a thing is edited: the balance sheet's Loans rows, which are
 *   a view of a record authored elsewhere, carry *Edit on House* / *Edit in Cash Flows*. **Primary**
 *   like [Add] — an invitation, nothing lost — with an arrow glyph, because the pill is a door,
 *   not an action on what is under it.
 * - [Run] — starts work the reader asked for and would not want started for them: the Classifiers
 *   tab's sensitivity sweep, several forecasts that never run on an edit. **Primary**, like [Go] —
 *   an invitation, nothing lost — with a play glyph, because the pill starts something here rather
 *   than taking the reader anywhere. Added 2026-10-04: [Go] is documented as a door, and a sweep is
 *   not one.
 * - [Invert] — flips a reading the other way round: the tax-lens map's "levies" ↔ "free of".
 *   **Tertiary**, the one theme hue the other kinds do not use, because it is neither additive
 *   nor destructive nor a return to anything. The only kind that is normally drawn [selected].
 * - [Copy] — puts a short value on the clipboard and changes nothing: the reference on a Reach Out
 *   question, which the sender pastes into a forum search or an email. **Neutral**, like [Clear]:
 *   nothing happens to the thing itself. The glyph is `#`, read as "a reference number", because a
 *   copy symbol is not in every font the app draws with (wasm included) and `#` is. Added
 *   2026-10-19; the caller swaps the label to a short "Copied" for a beat.
 * - [Previous] / [Next] — step a view one place back or forward: the Forecaster's year, one year at
 *   a time. **Primary**, like [Go] — they move the reader through the forecast and change nothing —
 *   with `‹` / `›`, drawn glyph-only beside the Year scrubber. Two kinds rather than one, because the
 *   glyph is the kind's and the two directions need different marks. Added 2026-10-29,
 *   when the year-step buttons moved from bare icons into the pill family.
 *
 * Each kind also owns a **glyph** ([glyph]) knocked out of the swatch — `+`, `×`, `−`, `↺` — so the
 * marker is a key to the kind and not a decoration, and so a glyph-only pill (a `×` at the end of a
 * dense table row, where a word would not fit) still says which kind it is.
 *
 * A fourth kind is a design decision, not a convenience: add it here with its reason, never a
 * bare `Color` parameter at a call site.
 */
enum class ActionKind(internal val glyph: String) {
    Add("+"),
    Clear("×"),
    Remove("−"),
    Reset("↺"),
    Invert("⇄"),
    Toggle("✓"),
    Go("→"),
    Run("▶"),
    Copy("#"),
    Previous("‹"),
    Next("›");

    @Composable
    internal fun color(): Color = when (this) {
        Add -> MaterialTheme.colorScheme.primary
        Clear -> MaterialTheme.colorScheme.onSurfaceVariant
        Remove -> MaterialTheme.colorScheme.error
        Reset -> MaterialTheme.colorScheme.onSurfaceVariant
        Invert -> MaterialTheme.colorScheme.tertiary
        Toggle -> MaterialTheme.colorScheme.primary
        Go -> MaterialTheme.colorScheme.primary
        Run -> MaterialTheme.colorScheme.primary
        Copy -> MaterialTheme.colorScheme.onSurfaceVariant
        Previous -> MaterialTheme.colorScheme.primary
        Next -> MaterialTheme.colorScheme.primary
    }
}

/**
 * @param label    The action's word, or **null** for a glyph-only pill — the swatch and its glyph
 *                 alone, for a dense row where a word would set the row's height or width. Pass
 *                 [contentDescription] then, so the action still has a name for accessibility.
 *                 (Tried on a table row's remove and judged too quiet; kept for the cases where a
 *                 word truly cannot fit.)
 * @param selected For a pill that is a **toggle** rather than a one-shot action: true draws the
 *                 tint at full strength so the on state is read at a glance. A one-shot action
 *                 leaves it false.
 * @param density  [ActionPillDensity.Standard] everywhere a pill stands on its own.
 *                 [ActionPillDensity.Thin] only where two are **stacked** beside a control that must
 *                 not grow — the All / Clear pair on a toggle row.
 */
/** How much vertical room an [ActionPill] takes — see the `density` parameter. */
enum class ActionPillDensity { Standard, Thin }

object ActionPillDefaults {
    /**
     * Pinned height of a [ActionPillDensity.Thin] pill: a `labelSmall` line and nothing else. Two
     * of these plus [ThinStackGap] are what a stacked All / Clear pair costs a toggle row.
     */
    val ThinHeight: Dp = 16.dp

    /** Gap between two stacked thin pills. */
    val ThinStackGap: Dp = 2.dp
}

@Composable
fun ActionPill(
    label: String?,
    kind: ActionKind,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selected: Boolean = false,
    density: ActionPillDensity = ActionPillDensity.Standard,
    contentDescription: String? = label,
    /**
     * When set, the tint and swatch use this instead of [ActionKind.color]. The glyph stays the
     * kind's. For a Clear that should read as a primary action — dropping a selection — rather
     * than the quiet un-answer beside a field.
     */
    hue: Color? = null,
) {
    val color = hue ?: kind.color()
    val tint = when {
        !enabled -> 0.07f
        selected -> 0.34f
        else -> 0.14f
    }
    // Disabled dims the word the way Material dims a disabled button's label, so a greyed pill
    // reads as "nothing to do" rather than as a pill that forgot its color.
    val ink = if (enabled) MaterialTheme.colorScheme.onSurface
    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    val shape = RoundedCornerShape(6.dp)
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Surface(
            shape = shape,
            color = color.copy(alpha = tint),
            contentColor = ink,
            modifier = modifier
                .clickable(enabled = enabled, onClick = onClick)
                .then(
                    if (contentDescription != null) {
                        Modifier.semantics { this.contentDescription = contentDescription }
                    } else {
                        Modifier
                    },
                ),
        ) {
            val thin = density == ActionPillDensity.Thin
            // A glyph-only pill has nothing but the swatch to be seen by, so the swatch is bigger.
            val swatch = if (label == null) 14.dp else 10.dp
            val glyphSize = if (label == null) 11.sp else 9.sp
            Row(
                // A glyph-only pill is square-ish: the same inset on every side. Thin cuts the
                // vertical inset to nothing and pins the height, so a stacked pair has a known size.
                modifier = Modifier
                    .then(if (thin) Modifier.height(ActionPillDefaults.ThinHeight) else Modifier)
                    .padding(
                        horizontal = if (label == null) 4.dp else if (thin) 6.dp else 8.dp,
                        vertical = if (thin) 0.dp else if (label == null) 4.dp else 2.dp,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // The swatch, with the kind's glyph knocked out of it in the surface color — the
                // same two-shapes-and-contrast idea as [HelpChipMark], for the same reason: a
                // filled square with one bold glyph survives being 10 dp; anything finer does not.
                // A Toggle that is off shows the box empty — outlined in the kind color, no glyph —
                // so on/off is read from the box, not from the tint.
                val swatchColor = if (enabled) color else color.copy(alpha = 0.38f)
                val emptyBox = kind == ActionKind.Toggle && !selected
                PillSwatch(
                    color = swatchColor,
                    glyph = if (emptyBox) null else kind.glyph,
                    size = swatch,
                    glyphSize = glyphSize,
                )
                if (label != null) {
                    Text(
                        text = label,
                        style = if (thin) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        softWrap = false,
                    )
                }
            }
        }
    }
}
