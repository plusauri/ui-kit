package tutil.ui

// Shared numeric-prefix validation used by NumericInput and SliderWithNumericInput.

private val positiveDecimalPrefixRe = Regex("""^\d*\.?\d*$""")
private val signedDecimalPrefixRe   = Regex("""^-?\d*\.?\d*$""")
private val positiveIntegerPrefixRe = Regex("""^\d*$""")
private val signedIntegerPrefixRe   = Regex("""^-?\d*$""")

/** Returns true when [text] is a valid numeric prefix (may be in-progress). */
internal fun isValidNumericPrefix(
    text: String,
    allowNegative: Boolean,
    allowDecimal: Boolean = true,
): Boolean = when {
    allowDecimal  && allowNegative  -> text.matches(signedDecimalPrefixRe)
    allowDecimal  && !allowNegative -> text.matches(positiveDecimalPrefixRe)
    !allowDecimal && allowNegative  -> text.matches(signedIntegerPrefixRe)
    else                            -> text.matches(positiveIntegerPrefixRe)
}

internal fun isInProgressNumeric(text: String, allowNegative: Boolean): Boolean =
    text.toDoubleOrNull() != null ||
        (allowNegative && text == "-") ||
        text.endsWith(".")

/** Characters a copied amount carries that are never part of the number: `$1,752,700`. */
private fun Char.isAmountFormatting(): Boolean = this == ',' || this == '$' || isWhitespace()

/** Cents a whole-number field can drop without changing the value: `.00` on `1752700.00`. */
private val zeroCentsRe = Regex("""\.0*$""")

/**
 * Strips the formatting a pasted amount brings with it — thousands separators, a dollar sign,
 * spaces — so `$1,752,700` from a listing site enters as `1752700` and then validates like
 * anything typed. [caret] is moved left by the characters removed before it. A whole-number field
 * also drops trailing zero cents. Returns null when [text] needs no change.
 */
internal fun stripAmountFormatting(text: String, caret: Int, allowDecimal: Boolean): Pair<String, Int>? {
    var stripped = text.filterNot { it.isAmountFormatting() }
    if (!allowDecimal && stripped.contains('.') && zeroCentsRe.containsMatchIn(stripped)) {
        stripped = stripped.replace(zeroCentsRe, "")
    }
    if (stripped == text) return null
    val removedBeforeCaret = text.take(caret).count { it.isAmountFormatting() }
    return stripped to (caret - removedBeforeCaret).coerceIn(0, stripped.length)
}
