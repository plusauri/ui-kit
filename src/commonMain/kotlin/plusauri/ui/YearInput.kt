package plusauri.ui

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import plusauri.ui.IntegerInput

/**
 * A validated integer input for entering a year, constrained to [yearRange].
 *
 * Thin wrapper over [IntegerInput] with sensible defaults for year entry:
 * non-negative values, a 1900–2400 range, and the host's year placeholder ([LocalYearInputWords]).
 *
 * All [IntegerInput] keyboard shortcuts are available: ↑/↓ steps by 1,
 * Shift+↑/↓ adjusts the digit to the left of the cursor.
 *
 * @param value         Current year, or null if the field is empty.
 * @param onValueChange Emitted on every valid edit; null when empty.
 * @param yearRange     Valid year range (default `1900..2400`).
 * @param label         Floating label shown on the border when the field has a value or focus.
 * @param placeholder   Hint shown when the field is empty.
 * @param enabled       When false the field is non-interactive.
 * @param modifier      Applied to the underlying text field. Defaults to [width] sized for a
 *                      4-digit year; pass an explicit modifier to override.
 */
/** Width of a [YearInput] sized for a four-digit year — the default, and what a caller that adds
 * a modifier of its own should chain onto rather than lose. */
val YearInputWidth = 72.dp

/**
 * Width of an integer entry sized for two digits — an age, a count of years. The dossier editors
 * had been writing `48.dp` inline for this; name it here, beside the year's, and reach for it.
 */
val TwoDigitInputWidth = 48.dp

@Composable
fun YearInput(
    value: Int?,
    onValueChange: (Int?) -> Unit,
    yearRange: IntRange = 1900..2400,
    label: String? = null,
    placeholder: String = LocalYearInputWords.current.placeholder,
    isError: Boolean = false,
    enabled: Boolean = true,
    outlined: Boolean = true,
    placeholderStyle: TextStyle? = null,
    missingFieldLabel: String? = label,
    modifier: Modifier = Modifier.width(YearInputWidth),
) {
    IntegerInput(
        value = value,
        onValueChange = onValueChange,
        allowNegative = false,
        range = yearRange,
        label = label,
        placeholder = placeholder,
        placeholderStyle = placeholderStyle,
        isError = isError,
        enabled = enabled,
        outlined = outlined,
        missingFieldLabel = missingFieldLabel,
        modifier = modifier,
    )
}
