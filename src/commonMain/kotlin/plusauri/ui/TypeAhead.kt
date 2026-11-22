package plusauri.ui

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Type-to-select for a fixed option list, the reflex every native select honors: with a picker
 * focused, typing "m" lands on the first option starting with M, "m" again cycles to the next one,
 * "ma" narrows to March/May, and the buffer forgets itself after a second of silence.
 *
 * [EnumSelect] uses it, and a host's own pickers can use it too, so every picker behaves the same
 * way and none owns a copy. The matcher is pure state plus a clock: it knows nothing about
 * focus or menus. Callers feed it a character and the option texts and move the highlight to the
 * index it returns.
 *
 * Rules, per the ARIA listbox pattern that native selects implement:
 * - A fresh single character searches *after* the current option, so pressing "m" while on March
 *   goes to May rather than staying put.
 * - The same character repeated (`mm…`) keeps cycling through the single-letter matches.
 * - Any other continuation (`ma`) narrows the prefix and searches from the current option
 *   *inclusive*, so having landed on March, "a" stays there.
 * - A continuation that matches nothing starts a fresh search from that letter alone, so a new
 *   letter is never swallowed (`s s y` over short labels lands on the second S, then on Y).
 * - Matching is case-insensitive and ignores leading glyphs — a marker, a flag, a ● — so "m"
 *   finds "🇩🇪 Munich", not the flag. See [typeAheadText].
 *
 * @param timeout silence after which the buffer resets; the native convention is about a second.
 */
class TypeAheadMatcher(
    private val timeSource: TimeSource = TimeSource.Monotonic,
    private val timeout: Duration = 1.seconds,
) {
    private var buffer = ""
    private var lastTyped: TimeMark? = null

    /** What has been typed so far in this burst — for tests and diagnostics. */
    val prefix: String get() = buffer

    fun reset() {
        buffer = ""
        lastTyped = null
    }

    /**
     * Records [char] and returns the index of the option to highlight, or null when nothing starts
     * with the buffer. [texts] are the per-option strings to match, one entry per option, each
     * already through [typeAheadText]; [current] is the highlighted option, or null when the picker
     * has no selection yet.
     */
    fun onChar(char: Char, texts: List<String>, current: Int?): Int? =
        onChar(char, texts.size, current) { i, needle -> texts[i].startsWith(needle) }

    /** [onChar] over [SelectOption]s, which can each answer to more than one text. */
    fun onCharOption(char: Char, options: List<SelectOption>, current: Int?): Int? {
        val texts = options.map { it.typeAheadTexts() }
        return onChar(char, options.size, current) { i, needle -> texts[i].any { it.startsWith(needle) } }
    }

    /**
     * The general form: [matches] says whether option [i] starts with the needle. The buffer and
     * timing rules live here once, whatever a caller decides "starts with" consults.
     */
    fun onChar(char: Char, count: Int, current: Int?, matches: (i: Int, needle: String) -> Boolean): Int? {
        val stale = lastTyped?.let { it.elapsedNow() > timeout } ?: true
        if (stale) buffer = ""
        lastTyped = timeSource.markNow()

        val c = char.lowercaseChar()
        val repeated = buffer.isNotEmpty() && buffer.all { it == c }
        buffer += c
        val needle = if (repeated) c.toString() else buffer
        val exclusive = repeated || buffer.length == 1
        val start = if (exclusive) (current ?: -1) + 1 else (current ?: 0)
        if (count <= 0) return null
        search(count, start, needle, matches)?.let { return it }
        // A prefix that matches nothing was not a prefix: `s s y` on short chip labels means "the
        // second S, then Y", not "an option starting SSY". Start again from this letter alone,
        // rather than swallowing it until the buffer times out.
        if (buffer.length > 1) {
            buffer = c.toString()
            return search(count, (current ?: -1) + 1, buffer, matches)
        }
        return null
    }

    private fun search(count: Int, start: Int, needle: String, matches: (Int, String) -> Boolean): Int? {
        for (step in 0 until count) {
            val i = (start + step).mod(count)
            if (matches(i, needle)) return i
        }
        return null
    }
}

/**
 * The form of an option label the matcher compares against: leading non-alphanumerics dropped (a
 * `🇩🇪` flag, a `●` marker, a `🛒`), then lowercased. A label that is nothing but glyphs matches
 * nothing rather than everything.
 */
fun typeAheadText(label: String): String =
    label.trimStart { !it.isLetterOrDigit() }.lowercase()

/**
 * The one-letter jump over a list of items: the next of [labels] (each already [typeAheadText])
 * after [from] that starts with [c], wrapping, or before it when [backward]. [from] -1 starts at the
 * top. Null when none does. Useful for a list or a grid of cards that jumps by first letter.
 */
fun nextLabelStarting(labels: List<String>, from: Int, c: Char, backward: Boolean = false): Int? {
    if (labels.isEmpty()) return null
    val step = if (backward) -1 else 1
    val needle = c.lowercaseChar()
    return (1..labels.size).map { (from + step * it).mod(labels.size) }.firstOrNull { labels[it].startsWith(needle) }
}

/**
 * The strings a [SelectOption] can be found by. A [SelectOption.KeyLabel] answers to either half —
 * a state picker's "CA" and "California", or a key that is a name while the label is a detail
 * line — so a prefix on either one lands.
 */
internal fun SelectOption.typeAheadTexts(): List<String> = when (this) {
    is SelectOption.Label -> listOf(typeAheadText(label))
    is SelectOption.KeyLabel -> listOf(typeAheadText(key), typeAheadText(label))
}

/**
 * The character a key event contributes to type-ahead, or null when it is not a typed character:
 * not a key-down, carries a Ctrl/Meta/Alt chord (a shortcut, not typing), or is whitespace or a
 * control character. Space is deliberately excluded — on a focused option it commits, as it always
 * has, and no option label needs a space to be told apart.
 */
fun KeyEvent.typeAheadChar(): Char? {
    if (type != KeyEventType.KeyDown) return null
    if (isCtrlPressed || isMetaPressed || isAltPressed) return null
    return typedCharOf(utf16CodePoint)
}

/** The typed character a key's code point stands for, or null when it is a key rather than typing. */
internal fun typedCharOf(codePoint: Int): Char? {
    if (codePoint <= 0 || codePoint > Char.MAX_VALUE.code) return null
    val ch = codePoint.toChar()
    if (ch.isWhitespace() || ch.isISOControl() || ch.isSurrogate()) return null
    // macOS gives the arrow, Home/End and function keys characters of their own in the private-use
    // area (NSUpArrowFunctionKey is U+F700 …). They are keys, not typing: read as a character, ←
    // on a real Mac keyboard would be taken as a letter and swallowed. A synthetic test key carries
    // no character at all, so a UI test cannot catch this; MacArrowKeysAreNotTypingTest checks it
    // directly.
    if (ch in '\uE000'..'\uF8FF') return null
    return ch
}

/**
 * A letter or digit typed with no Ctrl/Alt/Cmd — the only keys a chip row or a keyboard walk of a
 * form searches by. Punctuation (`?`) and everything else goes on to whoever else wants it.
 */
fun KeyEvent.typeAheadLetter(): Char? = typeAheadChar()?.takeIf { it.isLetterOrDigit() }

/**
 * `!` — Shift-1 on a US layout — the All / Clear toggle on a bulk chip row.
 *
 * [typeAheadLetter] never reads it (punctuation), so it cannot collide with a letter search.
 * A synthetic test key carries no character; Shift-1 is that `!` when the code point is absent.
 */
fun KeyEvent.isBang(): Boolean {
    if (type != KeyEventType.KeyDown) return false
    if (isCtrlPressed || isAltPressed || isMetaPressed) return false
    val ch = typedCharOf(utf16CodePoint)
    if (ch == ListKeys.TOGGLE_ALL) return true
    // Shift-1, the key `!` is on a US layout; re-keying [ListKeys.TOGGLE_ALL] means changing this
    // too. A US layout types '!'; a synthetic test key may type '1' or nothing.
    return isShiftPressed && (key == Key.One || key == Key.NumPad1) && (ch == null || ch == '1')
}

/**
 * Type-to-focus over a row of toggle buttons ([MultiButtonSelect]): with a button focused, a letter
 * moves focus to the **next** button whose label starts with it, wrapping — so on a row of Bold,
 * Border and Bullets, `b` is Bold, `b b` Border, `b b b` Bullets — and Enter toggles it, as it
 * toggles any focused button. A letter moves focus rather than toggling because a row may be
 * single- or multi-select, and "go there" means the same in both.
 *
 * **One letter, one rule, no timer.** Unlike a long picker ([TypeAheadMatcher], where "ma" narrowing
 * to March is worth a one-second buffer), a chip row is short and its labels are short, so a letter
 * never depends on how fast the one before it was typed. [nextLabelStarting] follows the same
 * rule.
 *
 * One group can span several toolbars that read as one row: each registers its buttons under a
 * [slot], and matching runs over the slots in order. A toolbar with no group gets its own.
 */
class ButtonTypeAhead {
    internal class Button(val text: String, val focus: FocusRequester)

    private val slots = mutableMapOf<Int, List<Button>>()

    internal fun register(slot: Int, buttons: List<Button>) { slots[slot] = buttons }

    internal fun unregister(slot: Int, buttons: List<Button>) {
        if (slots[slot] === buttons) slots.remove(slot)
    }

    /**
     * [char] typed while button [index] of [slot] held focus: focuses the next match. False when
     * nothing matches, so the key goes on to whatever else might want it.
     */
    internal fun onChar(char: Char, slot: Int, index: Int, backward: Boolean = false): Boolean {
        val ordered = slots.keys.sorted()
        val flat = ordered.flatMap { s -> slots.getValue(s) }
        if (flat.isEmpty()) return false
        val current = ordered.takeWhile { it != slot }.sumOf { slots.getValue(it).size } + index
        val c = char.lowercaseChar()
        // Shift (a capital) searches backwards, so a long row need not be cycled all the way round.
        val step = if (backward) -1 else 1
        val hit = (1..flat.size).map { (current + step * it).mod(flat.size) }
            .firstOrNull { flat[it].text.startsWith(c) } ?: return false
        runCatching { flat[hit].focus.requestFocus() }
        return true
    }
}

/** The text a toggle button is found by: [ButtonData.typeAhead], else its label, else its Unicode face. */
internal fun ButtonData.typeAheadText(): String =
    typeAheadText(
        typeAhead.ifEmpty { label.ifEmpty { (buttonImage as? ButtonImage.Unicode)?.text.orEmpty() } },
    )
