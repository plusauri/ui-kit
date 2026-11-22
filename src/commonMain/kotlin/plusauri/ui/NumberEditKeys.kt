package plusauri.ui

/**
 * The keys a number field acts on rather than types: `k`, `m` and `b` scale the whole value by a
 * thousand, a million or a billion (the capital divides), and `-` flips its sign wherever the caret
 * is.
 *
 * The kit owns these letters so a field works without a host shortcut table. A host that lists its
 * shortcuts should read the keys from here, or test that its list agrees.
 */
object NumberEditKeys {
    const val THOUSANDS: Char = 'k'
    const val MILLIONS: Char = 'm'
    const val BILLIONS: Char = 'b'
    const val NEGATE: Char = '-'
}

/**
 * If [newText] is exactly [oldText] with one scale character inserted anywhere,
 * returns the scaled result; otherwise returns null.
 *
 * | Char | Operation      |
 * |------|----------------|
 * | `k`  | × 1,000        |
 * | `K`  | ÷ 1,000        |
 * | `m`  | × 1,000,000    |
 * | `M`  | ÷ 1,000,000    |
 * | `b`  | × 1,000,000,000 |
 * | `B`  | ÷ 1,000,000,000 |
 */
fun scaleCharAt(newText: String, oldText: String): Double? {
    if (newText.length != oldText.length + 1) return null
    // Find the first position where the texts diverge — that's the inserted char.
    val insertPos = (0 until newText.length).first { i ->
        i == oldText.length || newText[i] != oldText[i]
    }
    // Removing the inserted char must exactly reconstruct oldText.
    if (newText.removeRange(insertPos, insertPos + 1) != oldText) return null
    val base = oldText.toDoubleOrNull() ?: return null
    val typed = newText[insertPos]
    val scale = when (typed.lowercaseChar()) {
        NumberEditKeys.THOUSANDS -> 1_000.0
        NumberEditKeys.MILLIONS -> 1_000_000.0
        NumberEditKeys.BILLIONS -> 1_000_000_000.0
        else -> return null
    }
    // Shift (the capital) divides.
    return if (typed.isUpperCase()) base / scale else base * scale
}
