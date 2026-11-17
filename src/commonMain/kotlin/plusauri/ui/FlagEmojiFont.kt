package plusauri.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily

/**
 * Flags font for Windows, or null everywhere else.
 *
 * Null means "leave the characters alone": macOS ligates them with Apple Color Emoji,
 * and the web subset ligates them because it is the only emoji face Skia has.
 */
val LocalFlagEmojiFont = staticCompositionLocalOf<FontFamily?> { null }

/**
 * On Windows, provide [LocalFlagEmojiFont] for [content]. Other targets already ligate
 * regional-indicator pairs, so this is a pass-through there. The font bytes live in
 * `jvmMain` resources and are not part of the wasm download.
 */
@Composable
expect fun ProvideFlagEmojiFont(content: @Composable () -> Unit)

/**
 * Paint each flag emoji in [family]. A no-op when [family] is null, so call sites can
 * run on every platform.
 *
 * The span covers only the two regional indicators. The flags font has no Latin and no
 * other emoji, so it must not become the font for the rest of the string.
 */
fun String.withFlagEmojiFont(family: FontFamily?): AnnotatedString {
    if (family == null) return AnnotatedString(this)
    return AnnotatedString(this).withFlagEmojiFont(family)
}

/**
 * See [String.withFlagEmojiFont]. Existing spans are kept.
 *
 * The flags font's ligature does not form when the style run ends on the second
 * regional indicator — Skia then draws that indicator from the next face. A word
 * joiner (U+2060) inside the same run, after the pair, keeps the ligature and
 * adds no break opportunity and no ink. The joiner is a zero-advance glyph in
 * the flags font. Without that glyph, fallback splits the run on the missing
 * code point and the second indicator is drawn from the next face.
 */
fun AnnotatedString.withFlagEmojiFont(family: FontFamily?): AnnotatedString {
    if (family == null) return this
    val ranges = flagEmojiRanges(text)
    if (ranges.isEmpty()) return this
    val builder = AnnotatedString.Builder()
    var cursor = 0
    for ((start, end) in ranges) {
        if (cursor < start) builder.append(subSequence(cursor, start))
        builder.pushStyle(SpanStyle(fontFamily = family))
        builder.append(subSequence(start, end))
        builder.append(FlagRunKeeper)
        builder.pop()
        cursor = end
    }
    if (cursor < length) builder.append(subSequence(cursor, length))
    return builder.toAnnotatedString()
}

/** Word joiner. Keeps the flag ligature inside its style run; draws nothing. */
private const val FlagRunKeeper = "\u2060"

/**
 * Half-open UTF-16 ranges of flag pairs (two regional indicators in a row).
 * One indicator is a surrogate pair, so a flag is four `Char`s.
 */
fun flagEmojiRanges(text: String): List<Pair<Int, Int>> {
    val ranges = ArrayList<Pair<Int, Int>>()
    var i = 0
    while (i < text.length) {
        if (isRegionalIndicatorAt(text, i) && isRegionalIndicatorAt(text, i + 2)) {
            ranges.add(i to i + 4)
            i += 4
        } else {
            i += if (i < text.length && text[i].isHighSurrogate()) 2 else 1
        }
    }
    return ranges
}

private fun isRegionalIndicatorAt(text: String, index: Int): Boolean {
    if (index < 0 || index + 1 >= text.length) return false
    val high = text[index]
    val low = text[index + 1]
    return high == '\uD83C' && low >= '\uDDE6' && low <= '\uDDFF'
}
