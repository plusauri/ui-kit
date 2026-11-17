package plusauri.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Where a control's caption sits relative to the control — for [LabeledField] and for
 * [SliderWithNumericInput], which is a labeled field whose content is a slider.
 *
 * Material floating labels sit *inside* the outlined box and get clipped when the field is
 * sized for digits only (compact ages, years, counts). External labels avoid that and keep
 * i18n length from forcing every numeric cell wider.
 *
 * One enum, not two: until 2026-09-20 the slider had its own `SliderLabelPlacement` naming the
 * same choice, which is the divergence the entry-control conventions exist to stop. [Stacked] is
 * the value that kept them apart — it only means something for a slider row.
 */
enum class FieldLabelPlacement {
    /** Caption to the left of the control on one row. Default for dense form cells. */
    Start,

    /** Caption on its own line above the control (tight vertical gap). */
    Above,

    /**
     * **Slider rows only.** Caption left; the numeric entry sits over a rail that is **exactly the
     * same width** as the entry:
     *
     * ```
     * [ label ]  [ entry···· ]
     *            [ slider··· ]   ← same width as entry
     * ```
     *
     * [SliderWithNumericInput.trackWidth] (Small / Medium / Large / XLarge) is the single size
     * knob: it sets both the entry field and the rail. Optional currency sits left of that
     * matched pair. Prefer this for dense FlowRow knobs (money and percents). A free-standing
     * [LabeledField] has no rail to stack over, so it draws this as [Start].
     */
    Stacked,
}

/**
 * Wraps a control with an **external** caption so field width can follow the value budget
 * (e.g. 2-digit ages) while the label uses full i18n text without clipping.
 *
 * **This is the house style for dossier write controls** (decided 2026-08-28) — the label sits
 * beside the control, not floating on its border. Pass [label] = null into [IntegerInput] /
 * [NumericInput] / [TightTextField] / [EnumSelect] so Material does not also draw a floating label
 * inside the box. The floating-label slots on those controls stay for the sanctioned exceptions
 * (a labeled *group* box such as `YearCurrencyValueInput`) and for non-dossier surfaces.
 *
 * @param label      Visible caption (American English in the base locale; flag+EN for others).
 * @param placement  [FieldLabelPlacement.Start] (default) or [FieldLabelPlacement.Above].
 * @param modifier   Applied to the outer layout.
 * @param content    The field (prefer no internal floating label).
 */
@Composable
fun LabeledField(
    label: String,
    modifier: Modifier = Modifier,
    placement: FieldLabelPlacement = FieldLabelPlacement.Start,
    /**
     * Gap between caption and control. Null takes the placement's default — 6.dp beside, 2.dp
     * above. It exists so a control that had already hand-rolled this layout with its own spacing
     * ([tutil.ui.dossier.PersonSelect]) can adopt the primitive without its call sites shifting;
     * a new field should leave it null rather than re-deciding the gap per site.
     */
    spacing: Dp? = null,
    content: @Composable () -> Unit,
) {
    val caption: @Composable () -> Unit = {
        // [OverflowTooltipText], not a plain [Text]: the caption is one line and does not wrap, so
        // a translation longer than the space the row can give it has to *say* it was cut. A plain
        // Text defaults to TextOverflow.Clip — the label is sliced mid-glyph with nothing to signal
        // more text, which is precisely the i18n failure this whole label style exists to avoid.
        // Where the row is roomy enough the box sizes to the text and nothing changes.
        OverflowTooltipText(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            contentAlignment = Alignment.CenterStart,
            maxLines = 1,
        )
    }
    when (placement) {
        // Stacked has no meaning without a rail (see the enum); the beside form is the nearest.
        FieldLabelPlacement.Start, FieldLabelPlacement.Stacked -> {
            Row(
                modifier = modifier,
                horizontalArrangement = Arrangement.spacedBy(spacing ?: 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                caption()
                // The control learns its own caption, so a field that reports itself out of range
                // ([InvalidFields]) is named in the Needs row with the words on screen.
                CompositionLocalProvider(LocalFieldCaption provides label, content = content)
            }
        }
        FieldLabelPlacement.Above -> {
            Column(
                modifier = modifier,
                verticalArrangement = Arrangement.spacedBy(spacing ?: 2.dp),
            ) {
                // Slight start inset so the caption lines up with outlined field content padding.
                caption()
                CompositionLocalProvider(LocalFieldCaption provides label, content = content)
            }
        }
    }
}

/**
 * Headroom [LabeledGroupBox] adds above itself, so its floating label does not read as crowded by
 * whatever sits above.
 *
 * A floating label is centered on the top border, so half of it rises *above* the box's own edge
 * and eats that much of the column's spacing. Equal `spacedBy` therefore produces unequal-looking
 * gaps — tight above the label, honest below the box — and the group box reads as jammed against
 * the row above. This is the half-label the reader loses back, sized to a `bodySmall` label's
 * half-height, so the gap they see above the label is the column's own rhythm again.
 *
 * The box owns this rather than leaving it to callers (it was opt-in until 2026-09-11, and 13 of
 * 16 range editors had not opted in), which means a group box first in a column carries it too —
 * a small, uniform inset that is cheaper than one more thing every caller has to remember. A
 * control whose caption sits beside it ([LabeledField]) needs none of this: its label is inside
 * the flow, which is one more reason Rule 1 prefers that form. The gap *below* the label is
 * [GroupBoxLabelClearance].
 */
val FloatingLabelHeadroom: Dp = 8.dp

/**
 * Visible clearance between a group box's floating label and the first row of content under it.
 *
 * This is the **only** knob that moves that gap, and it is not `contentPadding.top`. Material
 * measures an outlined field's top inset as `max(contentPadding.top, labelHeight / 2)`: the label
 * is centered on the border, so its lower half hangs *inside* the box and any top padding at or
 * under that half-height is absorbed without moving the content at all. Three group boxes used to
 * pass 4dp–7dp there — all inside that dead zone — which is why every one of them drew its first
 * row flush against the label's descenders. [LabeledGroupBox] pads the content instead, *after*
 * the `max()` has already spent the half-label, so this number is the gap a reader actually sees,
 * whatever the label's type size.
 */
val GroupBoxLabelClearance: Dp = 8.dp

/**
 * An outlined **group box** — a Material outlined-field frame with a label floating on its top
 * border — around a row or block of related controls. The sanctioned exception to [LabeledField]'s
 * label-beside-the-control rule (`planning/design-notes/dossier-entry-control-conventions.md`),
 * for the cases where several sub-fields are read as one value: a year range, a
 * (year, currency, amount) triple.
 *
 * **Every group box in the app goes through here**, which is what makes the framing one decision:
 * the side and bottom insets, and above all the label-to-content clearance
 * ([GroupBoxLabelClearance]) that each hand-rolled `DecorationBox` had been getting wrong on its
 * own, and the headroom above the label ([FloatingLabelHeadroom]) that only three callers had
 * remembered to add. A caller that wants a different frame should change this one, not reach for
 * `OutlinedTextFieldDefaults.DecorationBox` again.
 *
 * [label] is a slot rather than a string so a caller can hand it an eliding text
 * (`OverflowTooltipText`) when the label carries more than a name.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabeledGroupBox(
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    // Headroom outside the frame, applied after the caller's modifier so a caller's own padding
    // adds to it rather than replacing it.
    Box(modifier = modifier.padding(top = FloatingLabelHeadroom)) {
        OutlinedTextFieldDefaults.DecorationBox(
            // Non-empty so the label stays in its floating position always; the inner fields'
            // placeholders would otherwise fight a resting label for the same space.
            value = " ",
            innerTextField = { Box(Modifier.padding(top = GroupBoxLabelClearance)) { content() } },
            enabled = enabled,
            singleLine = false,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            isError = isError,
            label = label,
            // Top is deliberately 0: it cannot move the content (see [GroupBoxLabelClearance]) and
            // a nonzero value here would only invite someone to tune the wrong number.
            contentPadding = OutlinedTextFieldDefaults.contentPadding(
                start = 4.dp, top = 0.dp, end = 4.dp, bottom = 4.dp,
            ),
        )
    }
}

/** [LabeledGroupBox] with a plain text label. */
@Composable
fun LabeledGroupBox(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    content: @Composable () -> Unit,
) = LabeledGroupBox(
    label = { Text(label) },
    modifier = modifier,
    enabled = enabled,
    isError = isError,
    content = content,
)
