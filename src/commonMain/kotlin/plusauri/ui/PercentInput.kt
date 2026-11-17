package plusauri.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A numeric input for entering a percentage value.
 *
 * Values are stored and passed as fractions (e.g. `0.07` for 7%) but displayed
 * as percentages (e.g. "7"). The `%` suffix is added automatically.
 *
 * Inherits all [NumericInput] keyboard shortcuts: k/m/b scale, ↑/↓ step,
 * Shift+↑/↓ digit-at-cursor.
 *
 * @param value          Current value as a fraction (e.g. `0.07` = 7%), or null if empty.
 * @param onValueChange  Emitted on every valid edit as a fraction; null when empty.
 * @param allowNegative  When false, negative percentages are rejected.
 * @param range          Optional valid range in fractional units (e.g. `0.0..1.0`).
 * @param label          Floating label shown on the border when the field has a value or focus.
 * @param prefix         Text shown before the editable area (e.g. "μ=").
 * @param placeholder    Hint shown when the field is empty.
 * @param decimalPlaces  Max decimal places shown in the percentage display (e.g. `2` → "12.34%").
 *                       When null, floating-point noise is cleaned automatically (default).
 * @param maxTotalDigits When set, caps the total digit count (integer + decimal digits) the
 *                       user can enter, applied to the displayed percentage value. See
 *                       [NumericInput.maxTotalDigits] for full semantics.
 * @param clampToRange        When true and [range] is set, out-of-range typed values are
 *                            silently clamped instead of shown with an error highlight.
 * @param allowScaleShortcuts When false, the k/K/m/M/b/B scale shortcuts are disabled.
 * @param blankWhenZero  When true, an external 0% renders empty (see [NumericInput.blankWhenZero]).
 *                       Use when the parent coerces in-progress `null` back to `0` so backspace
 *                       does not trap a sticky zero.
 * @param enabled        When false the field is non-interactive.
 * @param modifier       Applied to the underlying text field.
 */
@Composable
fun PercentInput(
    value: Double?,
    onValueChange: (Double?) -> Unit,
    allowNegative: Boolean = false,
    range: ClosedFloatingPointRange<Double>? = null,
    label: String? = null,
    prefix: String = "",
    placeholder: String = "",
    decimalPlaces: Int? = null,
    maxTotalDigits: Int? = null,
    clampToRange: Boolean = false,
    allowScaleShortcuts: Boolean = true,
    blankWhenZero: Boolean = false,
    enabled: Boolean = true,
    isError: Boolean = false,
    missingFieldLabel: String? = label,
    modifier: Modifier = Modifier,
) {
    NumericInput(
        value = value?.times(100.0),
        onValueChange = { displayVal -> onValueChange(displayVal?.div(100.0)) },
        allowDecimal = true,
        allowNegative = allowNegative,
        range = range?.let { (it.start * 100.0)..(it.endInclusive * 100.0) },
        label = label,
        prefix = prefix,
        suffix = "%",
        placeholder = placeholder,
        decimalPlaces = decimalPlaces,
        maxTotalDigits = maxTotalDigits,
        clampToRange = clampToRange,
        allowScaleShortcuts = allowScaleShortcuts,
        blankWhenZero = blankWhenZero,
        enabled = enabled,
        isError = isError,
        missingFieldLabel = missingFieldLabel,
        modifier = modifier,
    )
}
