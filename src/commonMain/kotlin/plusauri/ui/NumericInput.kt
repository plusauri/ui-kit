package plusauri.ui

import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.round

/**
 * A validated numeric text field with rich keyboard shortcuts.
 *
 * **Keyboard shortcuts (when the field has focus):**
 * | Key              | Action |
 * |------------------|--------|
 * | `k` / `Shift+k`  | Multiply / divide current value by 1,000 |
 * | `m` / `Shift+m`  | Multiply / divide current value by 1,000,000 |
 * | `b` / `Shift+b`  | Multiply / divide current value by 1,000,000,000 |
 * | `-`              | Flip the sign of the whole value (when [allowNegative]) — typed anywhere |
 * | ↑ / ↓           | Increment / decrement by [incrementStep] |
 * | Shift+↑ / Shift+↓ | Increment / decrement the digit to the left of the cursor |
 *
 * **Focus selects the value**, by click or by Tab, so typing replaces it; a second click places the
 * caret as usual. Values are right-aligned, flush against any suffix.
 *
 * The field is a `BasicTextField` drawn inside Material3's
 * `OutlinedTextFieldDefaults.DecorationBox`. Prefix/suffix strings go in the decoration's
 * prefix/suffix slots, so they are never part of the raw text value and need no cursor-position
 * bookkeeping.
 *
 * [onValueChange] receives `null` when the field is empty or the entry is still
 * in-progress (e.g. "-" or "3."). Callers should treat `null` as "no committed
 * value yet" rather than zero.
 *
 * **Non-null backing is fine.** A caller whose sink cannot hold `null` may coerce on every change
 * (`onValueChange = { x = it ?: 0.0 }`) and push the result straight back in as [value]. While the
 * field has focus its own text is authoritative: an external [value] is acknowledged but does not
 * rewrite the buffer, so the coerced default cannot round-trip into an un-deletable "0" the moment
 * the last digit is backspaced. On blur the buffer is
 * reconciled to [value], so an emptied field shows the default the caller actually stored. The
 * flip side: a *programmatic* change to [value] while the user is typing in this exact field is
 * deferred to blur (or Enter), which is what a user mid-edit would expect anyway. There is no need
 * for a nullable local mirror or a `lastExternal` guard at the call site.
 *
 * @param value          Current value, or null if the field should appear empty.
 * @param onValueChange  Emitted on every valid edit; null when empty/in-progress.
 * @param allowDecimal   When false, decimal input and display are suppressed.
 * @param allowNegative  When false, a minus sign is rejected. When true, typing `-` anywhere flips
 *                       the sign of the current value (an empty field still starts one via a leading `-`).
 * @param range          When set, values outside the range show an error highlight.
 * @param prefix         Text displayed before the input area (e.g. "$").
 * @param suffix         Text displayed after the input area (e.g. "%").
 * @param label          Floating label shown on the border when the field has a value or focus.
 * @param placeholder    Hint shown when the field is empty.
 * @param incrementStep  Amount added/subtracted by ↑/↓ arrow keys (default 1).
 * @param decimalPlaces  Fixed decimal places for display and rounding. When null, trailing
 *                       zeros are trimmed automatically.
 * @param maxTotalDigits When set, limits the total number of digit characters (integer +
 *                       decimal digits, excluding the decimal point and minus sign) the user
 *                       can enter. Display precision is also capped dynamically:
 *                       `decimalPlaces = maxTotalDigits - integerDigitCount`. Has no effect
 *                       when null (default), preserving existing behavior.
 * @param clampToRange        When true and [range] is set, typed values that fall outside the
 *                            range are silently clamped to the nearest bound rather than shown
 *                            with an error highlight. Default false preserves existing behavior.
 * @param allowScaleShortcuts When false, the k/K/m/M/b/B scale shortcuts are ignored and the
 *                            characters are treated as invalid input. Default true.
 * @param blankWhenZero  When true, an external value of `0` renders as an empty field so the user
 *                       can type a new number without first selecting/deleting a seed `0` — a
 *                       display choice for model defaults of zero, independent of the focus rule
 *                       above. Deliberately typing `0` still shows `0`. Default false.
 * @param showSeparator  When true, thousands separators are shown (e.g. 1,234,567).
 * @param enabled        When false the field is non-interactive.
 * @param missingFieldLabel  Identity used to light this field when the user reaches for a
 *                       disabled OK ([MissingFieldPointer]), and the name it reports to
 *                       [InvalidFields]. Defaults to [label], which is usually the string the
 *                       host's list of required fields names. Pass a different string when the
 *                       control's floating label is not the name that list uses.
 * @param modifier       Applied to the underlying `BasicTextField`.
 */
@Composable
fun NumericInput(
    value: Double?,
    onValueChange: (Double?) -> Unit,
    allowDecimal: Boolean = true,
    allowNegative: Boolean = true,
    range: ClosedFloatingPointRange<Double>? = null,
    prefix: String = "",
    suffix: String = "",
    label: String? = null,
    placeholder: String = "",
    incrementStep: Double = 1.0,
    decimalPlaces: Int? = null,
    maxTotalDigits: Int? = null,
    clampToRange: Boolean = false,
    allowScaleShortcuts: Boolean = true,
    blankWhenZero: Boolean = false,
    showSeparator: Boolean = false,
    placeholderStyle: TextStyle? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    outlined: Boolean = true,
    missingFieldLabel: String? = label,
    /** Fill behind the digits; null = the theme's. A host can tint it to signal the value's cost. */
    containerColor: Color? = null,
    /** Digit color while enabled; null = onSurface. Pairs with a dark [containerColor]. */
    contentColor: Color? = null,
    modifier: Modifier = Modifier,
) {
    var lastExternal by remember { mutableStateOf(value) }
    var isFocused by remember { mutableStateOf(false) }
    // Bumped by Enter; the composition that observes the bump reconciles the buffer to [value]
    // even though the field is still focused. `seen` is deliberately not snapshot state — it is
    // bookkeeping written during composition, not something to recompose on.
    var commitTick by remember { mutableStateOf(0) }
    val commitSeen = remember { object { var tick = 0 } }
    var fieldValue by remember {
        val text = numericFieldText(value, allowDecimal, decimalPlaces, maxTotalDigits, blankWhenZero)
        mutableStateOf(TextFieldValue(text = text, selection = TextRange(text.length)))
    }
    // Focus selects the whole value, so the first keystroke replaces it rather than landing wherever
    // the click put the caret: a user who clicks into a number field expects to overwrite it, not
    // to edit around the old digits. A click focuses first and places its caret after — on press
    // *and* again on release — so the selection made on focus would be wiped by the very click
    // that caused it. While this latch is set, caret-only changes are absorbed. It clears on the
    // next press into the already-focused field (a deliberate caret placement), on a drag
    // selection, on any key (so a field reached by Tab, where no placing click follows, does not
    // swallow the first arrow key), and on blur. Bookkeeping, not display state, hence not
    // snapshot state.
    val selectAllOnFocus = remember { object { var pending = false } }

    /** Rewrites the buffer from [value] when the two disagree — see [numericReconciledText]. */
    fun reconcileToExternal() {
        numericReconciledText(value, fieldValue.text, allowDecimal, decimalPlaces, maxTotalDigits, blankWhenZero)
            ?.let { fieldValue = TextFieldValue(text = it, selection = TextRange(it.length)) }
    }

    // Sync display text when the parent pushes a new value — unless the user is typing here. While
    // focused the buffer is authoritative: a parent that coerces our `null` to a default and echoes
    // it back would otherwise rewrite the field mid-edit (backspace the last digit, watch "0"
    // reappear, un-deletable). The value is still acknowledged so a later real change is seen; the
    // buffer catches up at the commit points — blur (below) and Enter (the tick).
    val committing = commitTick != commitSeen.tick
    commitSeen.tick = commitTick
    val externalChanged = value != lastExternal
    lastExternal = value
    if ((externalChanged && !isFocused) || committing) reconcileToExternal()

    val parsedValue = fieldValue.text.toDoubleOrNull()
    val showError = isError || isMissingFieldPointed(missingFieldLabel)
    val valueOutOfRange = parsedValue != null && range != null && parsedValue !in range
    val isOutOfRange = showError || valueOutOfRange

    // Red is not enough: report the value to the host's open form ([InvalidFields]), which can
    // block OK and name this field until it is back in range. Named by its own label, else its
    // [LabeledField] caption, so the form uses the words on screen, and the name matches the label
    // the "point at the field" highlight addresses ([MissingFieldPointer]).
    val invalidFields = LocalInvalidFields.current
    if (invalidFields != null) {
        // Null when the field has no name at all: the form names it ([InvalidFields.labels]).
        val reportAs = missingFieldLabel ?: label ?: LocalFieldCaption.current
        val reportKey = remember { Any() }
        DisposableEffect(invalidFields, valueOutOfRange, reportAs) {
            if (valueOutOfRange) invalidFields.report(reportKey, reportAs) else invalidFields.withdraw(reportKey)
            onDispose { invalidFields.withdraw(reportKey) }
        }
    }

    fun emit(text: String) {
        val parsed = text.toDoubleOrNull()
        lastExternal = parsed
        onValueChange(parsed)
    }

    /**
     * Applies [newValue] clamped by [allowNegative] and [range], then updates field + emits. The
     * cursor lands at the end unless [keepCaretFromEnd] is given, in which case it is placed that
     * many characters from the end — used by the sign toggle to leave the caret on the same digit
     * even though a leading `-` was added or removed.
     */
    fun applyToField(newValue: Double, keepCaretFromEnd: Int? = null) {
        val v = (if (!allowNegative) newValue.coerceAtLeast(0.0) else newValue)
            .let { d -> range?.let { d.coerceIn(it.start, it.endInclusive) } ?: d }
        // Arrow-key / scale commits always show the real number, including zero — blankWhenZero
        // only affects the external-value seed display, not deliberate edits.
        val text = v.toNumericDisplay(allowDecimal, decimalPlaces, maxTotalDigits)
        val caret = keepCaretFromEnd?.let { (text.length - it).coerceIn(0, text.length) } ?: text.length
        fieldValue = TextFieldValue(text = text, selection = TextRange(caret))
        emit(text)
    }

    /**
     * Increments or decrements the digit immediately to the left of the cursor.
     * Digits wrap 0→9 and 9→0; non-digit characters (`.`, `-`) are skipped.
     */
    fun adjustDigitAtCursor(direction: Int) {
        val text = fieldValue.text
        val cursor = fieldValue.selection.start.coerceIn(0, text.length)
        var pos = cursor - 1
        while (pos >= 0 && !text[pos].isDigit()) pos--
        if (pos < 0) return
        val newDigit = ((text[pos] - '0' + direction + 10) % 10)
        val newText = text.substring(0, pos) + newDigit.digitToChar() + text.substring(pos + 1)
        fieldValue = fieldValue.copy(text = newText)
        emit(newText)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val fill = containerColor ?: Color.Transparent
    val fieldColors = if (outlined) OutlinedTextFieldDefaults.colors(
                          focusedContainerColor = fill,
                          unfocusedContainerColor = fill,
                          errorContainerColor = fill,
                      )
                      else OutlinedTextFieldDefaults.colors(
                          unfocusedBorderColor = Color.Transparent,
                          focusedBorderColor = Color.Transparent,
                          errorBorderColor = Color.Transparent,
                          disabledBorderColor = Color.Transparent,
                          focusedContainerColor = fill,
                          unfocusedContainerColor = fill,
                          errorContainerColor = fill,
                      )
    val visualTransformation: VisualTransformation =
        if (showSeparator) remember { ThousandsSeparatorTransformation() }
        else VisualTransformation.None
    BasicTextField(
        value = fieldValue,
        onValueChange = onValueChange@{ raw ->
            if (selectAllOnFocus.pending && raw.text == fieldValue.text && raw.selection.collapsed) {
                return@onValueChange
            }
            // A pasted "$1,752,700" loses its formatting here and then goes down the normal path.
            val new = stripAmountFormatting(raw.text, raw.selection.end, allowDecimal)
                ?.let { (text, caret) -> TextFieldValue(text = text, selection = TextRange(caret)) }
                ?: raw
            selectAllOnFocus.pending = false
            // Check for a scale shortcut typed at the end of the number (k/K/m/M/b/B).
            // Handling here (rather than onPreviewKeyEvent) ensures it works on both
            // desktop and browser: arrow keys come through onPreviewKeyEvent cleanly on
            // both platforms, but printable characters on Wasm/browser bypass
            // onPreviewKeyEvent's preventDefault path and always reach onValueChange.
            val scaled = if (allowScaleShortcuts) scaleCharAt(new.text, fieldValue.text) else null
            // Typing '-' anywhere flips the sign of the whole value (like k/m/b scale the magnitude),
            // so negatives don't require positioning the caret before the first digit. Preserve the
            // caret's distance from the end so it stays on the same digit across the sign change.
            val signToggled = if (allowNegative) signToggleAt(new.text, fieldValue.text) else null
            when {
                scaled != null -> applyToField(scaled)
                signToggled != null -> {
                    val keepFromEnd = fieldValue.text.length - fieldValue.selection.start
                    if (signToggled == 0.0) {
                        // Zero: keep the raw in-progress text (e.g. "0.0000") and just flip a leading
                        // '-', so a negative decimal can be built up digit-by-digit from a zero seed.
                        val (raw, caret) = toggleZeroSign(fieldValue.text, fieldValue.selection.start)
                        fieldValue = TextFieldValue(text = raw, selection = TextRange(caret))
                        emit(raw)
                    } else {
                        applyToField(signToggled, keepCaretFromEnd = keepFromEnd)
                    }
                }
                new.text.isEmpty() || (isValidNumericPrefix(new.text, allowNegative, allowDecimal) &&
                    (maxTotalDigits == null || countTotalDigits(new.text) <= maxTotalDigits)) -> {
                    val parsed = new.text.toDoubleOrNull()
                    if (clampToRange && range != null && parsed != null && parsed !in range) {
                        applyToField(parsed)
                    } else {
                        fieldValue = new
                        emit(new.text)
                    }
                }
            }
        },
        singleLine = true,
        enabled = enabled,
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            textAlign = TextAlign.End,
            color = if (enabled) contentColor ?: MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (allowDecimal) KeyboardType.Decimal else KeyboardType.Number,
        ),
        visualTransformation = visualTransformation,
        interactionSource = interactionSource,
        cursorBrush = SolidColor(
            if (isOutOfRange) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        ),
        modifier = modifier
            .pointAtMissingField(missingFieldLabel)
            .fieldFocusStop()
            .pointerInput(Unit) {
                // Initial pass, not consumed: seen before the text field acts on the press, so a
                // second click into a focused field clears the latch in time to place its caret.
                // The press that focuses the field arrives while it is still unfocused.
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press && isFocused) selectAllOnFocus.pending = false
                    }
                }
            }
            .onFocusChanged { state ->
                if (isFocused && !state.isFocused) reconcileToExternal()
                if (!isFocused && state.isFocused) {
                    fieldValue = fieldValue.copy(selection = TextRange(0, fieldValue.text.length))
                    selectAllOnFocus.pending = true
                }
                if (!state.isFocused) selectAllOnFocus.pending = false
                isFocused = state.isFocused
            }
            .onPreviewKeyEvent { event ->
            // Only handle non-printable keys here: arrow keys work via onPreviewKeyEvent
            // on both desktop and browser. Scale shortcuts (k/m/b) are handled in
            // onValueChange instead, which is the only reliable interception point on Wasm.
            if (!enabled || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            selectAllOnFocus.pending = false
            val current = fieldValue.text.toDoubleOrNull() ?: 0.0
            when (event.key) {
                Key.DirectionUp -> {
                    if (event.isShiftPressed) adjustDigitAtCursor(+1)
                    else applyToField(current + incrementStep)
                    true
                }
                Key.DirectionDown -> {
                    if (event.isShiftPressed) adjustDigitAtCursor(-1)
                    else applyToField(current - incrementStep)
                    true
                }
                // Enter is a commit point like blur: a parent that reacts to Enter (a slider
                // companion snapping to its step) pushes a new value while we are still focused,
                // and the tick lets that one through. Not consumed — the field stays focused and an
                // enclosing dialog may still confirm on it.
                Key.Enter, Key.NumPadEnter -> {
                    commitTick++
                    false
                }
                else -> false
            }
        },
        decorationBox = { innerTextField ->
            // Material outlined labels sit *inside* the box when empty and unfocused (expanded
            // state). That fills small money fields with bulky title text. The public
            // DecorationBox overload hardcodes Attached(alwaysMinimize=false); Material 1.9's
            // Attached(alwaysMinimize=true) is only on the TextFieldState decorator path.
            // Workaround: [numericDecorationValue] — see that helper for the contract and tests.
            OutlinedTextFieldDefaults.DecorationBox(
                value = numericDecorationValue(fieldValue.text, hasLabel = label != null),
                // Core text sizes to its content width, so TextAlign.End is a no-op for short values
                // (ages like "67"). Force the core field to the full remaining content width so
                // End alignment matches money/% rows (numbers flush to the trailing padding).
                innerTextField = {
                    Layout(content = { innerTextField() }) { measurables, constraints ->
                        val width = when {
                            constraints.hasBoundedWidth -> constraints.maxWidth
                            constraints.minWidth > 0 -> constraints.minWidth
                            else -> measurables.firstOrNull()
                                ?.maxIntrinsicWidth(constraints.maxHeight)
                                ?: 0
                        }.coerceAtLeast(0)
                        val placeable = measurables.first().measure(
                            Constraints(
                                minWidth = width,
                                maxWidth = width,
                                minHeight = 0,
                                maxHeight = constraints.maxHeight,
                            ),
                        )
                        layout(width, placeable.height) {
                            placeable.placeRelative(0, 0)
                        }
                    }
                },
                enabled = enabled,
                singleLine = true,
                visualTransformation = visualTransformation,
                interactionSource = interactionSource,
                isError = isOutOfRange,
                colors = fieldColors,
                label       = label?.let { l -> { Text(l, softWrap = false) } },
                prefix      = if (prefix.isNotEmpty())      ({ Text(prefix,      style = MaterialTheme.typography.bodyMedium) }) else null,
                suffix      = if (suffix.isNotEmpty())      ({ Text(suffix,      style = MaterialTheme.typography.bodyMedium) }) else null,
                // Placeholder only when the real text is empty. With a label we force a zero-width
                // decoration value so Material treats the field as non-empty for label placement;
                // in that case Material would hide the placeholder slot anyway.
                placeholder = if (placeholder.isNotEmpty() && fieldValue.text.isEmpty()) {
                    {
                        Text(
                            placeholder,
                            style = placeholderStyle ?: MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                } else null,
                contentPadding = OutlinedTextFieldDefaults.contentPadding(
                    start = 8.dp, top = 3.dp, end = 8.dp, bottom = 3.dp
                ),
            )
        },
    )
}

/**
 * A thin integer wrapper over [NumericInput].
 *
 * Converts between [Int] and the underlying [Double] storage; forces
 * `allowDecimal = false`. All keyboard shortcuts from [NumericInput] are
 * available; [incrementStep] defaults to 1.
 *
 * @param value         Current integer value, or null if the field is empty.
 * @param onValueChange Emitted on every valid edit; null when empty.
 * @param allowNegative When false, negative values are rejected.
 * @param range         Optional valid range; out-of-range values show an error highlight.
 * @param label         Floating label shown on the border when the field has a value or focus.
 * @param prefix        Text displayed before the input area.
 * @param suffix        Text displayed after the input area.
 * @param placeholder   Hint shown when the field is empty.
 * @param incrementStep Amount added/subtracted by ↑/↓ arrow keys (default 1).
 * @param blankWhenZero When true, an external `0` renders empty (see [NumericInput]).
 * @param showSeparator When true, thousands separators are shown (e.g. 1,234,567).
 * @param enabled       When false the field is non-interactive.
 * @param modifier      Applied to the underlying `BasicTextField`.
 */
@Composable
fun IntegerInput(
    value: Int?,
    onValueChange: (Int?) -> Unit,
    allowNegative: Boolean = true,
    range: IntRange? = null,
    label: String? = null,
    prefix: String = "",
    suffix: String = "",
    placeholder: String = "",
    incrementStep: Int = 1,
    blankWhenZero: Boolean = false,
    showSeparator: Boolean = false,
    placeholderStyle: TextStyle? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    outlined: Boolean = true,
    missingFieldLabel: String? = label,
    modifier: Modifier = Modifier,
) {
    NumericInput(
        value = value?.toDouble(),
        onValueChange = { onValueChange(it?.toInt()) },
        allowDecimal = false,
        allowNegative = allowNegative,
        range = range?.let { it.first.toDouble()..it.last.toDouble() },
        label = label,
        prefix = prefix,
        suffix = suffix,
        placeholder = placeholder,
        incrementStep = incrementStep.toDouble(),
        blankWhenZero = blankWhenZero,
        showSeparator = showSeparator,
        placeholderStyle = placeholderStyle,
        isError = isError,
        enabled = enabled,
        outlined = outlined,
        missingFieldLabel = missingFieldLabel,
        modifier = modifier,
    )
}

// ── Pure helpers (tested) ─────────────────────────────────────────────────────

/**
 * Value fed to [OutlinedTextFieldDefaults.DecorationBox] for label placement only.
 *
 * - Non-empty text is passed through unchanged (normal typing / committed values).
 * - Empty text **with** a label becomes a zero-width space so Material keeps the label
 *   minimized on the border instead of filling the box (see decorationBox comment).
 * - Empty text **without** a label stays empty (no label to force; placeholders work).
 *
 * Does **not** change the BasicTextField content, [onValueChange] emissions, or parsing —
 * only the decoration layer's notion of emptiness. Safe for all [NumericInput] call sites:
 * unlabeled fields (many sliders' companions, pure amounts) are unchanged; labeled fields
 * only change where the label sits when empty.
 */
internal fun numericDecorationValue(text: String, hasLabel: Boolean): String =
    when {
        text.isNotEmpty() -> text
        hasLabel -> "\u200B"
        else -> ""
    }

/**
 * Text shown for an external [value] in [NumericInput].
 *
 * - `null` → empty (uncommitted / cleared).
 * - `0` with [blankWhenZero] → empty so a seed zero is not a typing obstacle.
 * - otherwise → [Double.toNumericDisplay].
 */
internal fun numericFieldText(
    value: Double?,
    allowDecimal: Boolean,
    decimalPlaces: Int? = null,
    maxTotalDigits: Int? = null,
    blankWhenZero: Boolean = false,
): String = when {
    value == null -> ""
    blankWhenZero && value == 0.0 -> ""
    else -> value.toNumericDisplay(allowDecimal, decimalPlaces, maxTotalDigits)
}

/**
 * Text the field should show at a commit point (blur, Enter, or an external change while
 * unfocused), or `null` when the buffer already agrees with [value] and must be left alone.
 *
 * External values are held back while the field is focused, so at commit the buffer may lag: an
 * emptied field whose caller stored a default, or a lone `-` / `.` the user walked away from.
 * Both are rewritten to [numericFieldText]. A buffer that parses to [value] is kept verbatim —
 * "0" stays "0" under [blankWhenZero] because the user typed it, and "7." keeps its dot.
 */
internal fun numericReconciledText(
    value: Double?,
    fieldText: String,
    allowDecimal: Boolean,
    decimalPlaces: Int? = null,
    maxTotalDigits: Int? = null,
    blankWhenZero: Boolean = false,
): String? {
    val parsed = fieldText.toDoubleOrNull()
    val agrees = parsed == value && (parsed != null || fieldText.isEmpty())
    if (agrees) return null
    return numericFieldText(value, allowDecimal, decimalPlaces, maxTotalDigits, blankWhenZero)
}

// ── Private helpers ───────────────────────────────────────────────────────────

/**
 * Inserts US thousands separators for display only. The raw [TextFieldValue.text]
 * stored in state remains comma-free so all parsing and key-interception logic is
 * unaffected. Handles negative numbers and decimal fractions; only the integer part
 * receives commas.
 */
private class ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val visual = raw.insertThousandsSeparators()
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, raw.length)
                if (clamped == 0) return 0
                var seen = 0
                for (i in visual.indices) {
                    if (visual[i] != ',') seen++
                    if (seen == clamped) return i + 1
                }
                return visual.length
            }
            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, visual.length)
                var commas = 0
                for (i in 0 until clamped) {
                    if (visual[i] == ',') commas++
                }
                return (clamped - commas).coerceIn(0, raw.length)
            }
        }
        return TransformedText(AnnotatedString(visual), offsetMapping)
    }
}

private fun String.insertThousandsSeparators(): String {
    if (isEmpty()) return this
    val dotIdx = indexOf('.')
    val intPart = if (dotIdx >= 0) substring(0, dotIdx) else this
    val decPart = if (dotIdx >= 0) substring(dotIdx) else ""
    val isNeg = intPart.startsWith('-')
    val digits = if (isNeg) intPart.drop(1) else intPart
    if (digits.isEmpty()) return this
    val sb = StringBuilder()
    for (i in digits.indices) {
        sb.append(digits[i])
        val remaining = digits.length - i - 1
        if (remaining > 0 && remaining % 3 == 0) sb.append(',')
    }
    return (if (isNeg) "-" else "") + sb.toString() + decPart
}

/**
 * Detects a single `-` typed anywhere into [oldText] and, when the field already holds a parseable
 * number, returns that number with its sign flipped — so `-` acts on the whole value immediately
 * (like the k/m/b scale keys), instead of only working as a leading prefix the user has to position
 * the cursor for. Returns null when the edit isn't a lone `-` insertion, or the field has no
 * committed number yet — so a leading `-` on an empty (or still-in-progress) field falls through to
 * normal handling and starts a negative number the usual way. Only consulted when negatives are
 * allowed; callers gate on `allowNegative`.
 */
internal fun signToggleAt(newText: String, oldText: String): Double? {
    if (newText.length != oldText.length + 1) return null
    val insertPos = (0 until newText.length).first { i ->
        i == oldText.length || newText[i] != oldText[i]
    }
    if (newText[insertPos] != NumberEditKeys.NEGATE) return null
    // Removing the inserted '-' must exactly reconstruct oldText (a pure single-char insertion).
    if (newText.removeRange(insertPos, insertPos + 1) != oldText) return null
    val base = oldText.toDoubleOrNull() ?: return null
    return -base
}

/**
 * Sign-toggles a **zero-valued** field textually instead of numerically. A zero renders identically
 * with or without a sign, and `-0.0` would reformat back to "0" — discarding an in-progress decimal
 * scaffold ("0.0000") the user is clearly building a negative out of. So we keep [oldText] verbatim
 * and just add or remove a leading `-`, preserving the caret's distance from the end so it stays on
 * the same character. Returns the new (text, caret). Used only when the field parses to 0.
 *
 * Examples (caret shown as `|`): `0|` → `-0|`, `0.|` → `-0.|`, `0|.` → `-0|.`, `0.0000|` → `-0.0000|`.
 */
internal fun toggleZeroSign(oldText: String, caret: Int): Pair<String, Int> {
    val raw = if (oldText.startsWith('-')) oldText.substring(1) else "-$oldText"
    val fromEnd = oldText.length - caret
    val newCaret = (raw.length - fromEnd).coerceIn(0, raw.length)
    return raw to newCaret
}

/**
 * Formats this value for display in a numeric text field.
 *
 * Before formatting, the value is pre-rounded to eliminate IEEE 754 noise:
 * - When [decimalPlaces] is set, rounds to exactly that many decimal places.
 * - When [maxTotalDigits] is set (and [decimalPlaces] is null), decimal places are
 *   derived as `maxTotalDigits - integerDigitCount`, so total significant digits never
 *   exceed [maxTotalDigits].
 * - Otherwise rounds to 10 decimal places — invisible to users but silently
 *   fixes artifacts like `0.07 * 100 = 7.000000000000001` → `7`.
 *
 * Whole-number doubles are shown without a decimal point ("1000" not "1000.0").
 * Kotlin's [Double.toString] produces scientific notation above ~10^7; values
 * where the integer part is representable as a [Long] are formatted as plain
 * decimal strings to avoid that.
 */
fun Double.toNumericDisplay(
    allowDecimal: Boolean,
    decimalPlaces: Int? = null,
    maxTotalDigits: Int? = null,
): String {
    if (this == 0.0) return "0"
    if (this.isNaN() || this.isInfinite()) return ""

    val effectiveDecimalPlaces: Int? = when {
        decimalPlaces != null -> decimalPlaces
        maxTotalDigits != null -> {
            val intDigits = abs(this).toLong().let { if (it == 0L) 1 else it.toString().length }
            (maxTotalDigits - intDigits).coerceAtLeast(0)
        }
        else -> null
    }

    val v = if (allowDecimal) {
        val factor = if (effectiveDecimalPlaces != null) 10.0.pow(effectiveDecimalPlaces) else 1e10
        round(this * factor) / factor
    } else {
        this
    }
    if (v == 0.0) return "0"

    val absValue = abs(v)
    val longSafe = absValue < Long.MAX_VALUE.toDouble()

    return if (!allowDecimal || (longSafe && v == v.toLong().toDouble())) {
        v.toLong().toString()
    } else {
        val s = v.toString()
        when {
            'E' in s || 'e' in s -> {
                // Scientific notation (large or very small value): surface the integer part.
                if (longSafe) v.toLong().toString() else s
            }
            s.contains('.') -> s.trimEnd('0').trimEnd('.')
            else -> s
        }
    }
}

/** Counts total digit characters in [text], ignoring the minus sign and decimal point. */
private fun countTotalDigits(text: String): Int =
    text.count { it.isDigit() }
