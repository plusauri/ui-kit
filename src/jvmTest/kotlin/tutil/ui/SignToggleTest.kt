package tutil.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [signToggleAt] lets a user type `-` anywhere in a numeric field to flip the whole value's sign
 * (the way k/m/b scale its magnitude), so negative entry no longer requires positioning the caret
 * before the first digit. Guards that it fires only on a lone `-` insertion into a committed number.
 */
class SignToggleTest {

    @Test
    fun `minus appended flips a positive value`() {
        assertEquals(-10.0, signToggleAt(newText = "10-", oldText = "10"))
    }

    @Test
    fun `minus in the middle flips too`() {
        assertEquals(-10.0, signToggleAt(newText = "1-0", oldText = "10"))
    }

    @Test
    fun `leading minus flips (same as prefix, but via the toggle)`() {
        assertEquals(-10.0, signToggleAt(newText = "-10", oldText = "10"))
    }

    @Test
    fun `minus flips a negative value back to positive`() {
        assertEquals(10.0, signToggleAt(newText = "--10", oldText = "-10"))
        assertEquals(10.0, signToggleAt(newText = "-10-", oldText = "-10"))
    }

    @Test
    fun `decimals keep their fractional part when flipped`() {
        assertEquals(-1.5, signToggleAt(newText = "1.5-", oldText = "1.5"))
    }

    @Test
    fun `empty field is not a toggle so a leading minus can start a negative number`() {
        assertNull(signToggleAt(newText = "-", oldText = ""))
    }

    @Test
    fun `an in-progress field with no parseable number does not toggle`() {
        // A bare "-" isn't a committed number; typing another "-" shouldn't flip anything.
        assertNull(signToggleAt(newText = "--", oldText = "-"))
    }

    @Test
    fun `non-minus insertions and multi-char edits are ignored`() {
        assertNull(signToggleAt(newText = "100", oldText = "10")) // inserted a digit
        assertNull(signToggleAt(newText = "-100", oldText = "10")) // more than one char changed
        assertNull(signToggleAt(newText = "10", oldText = "10")) // no insertion
    }

    @Test
    fun `signToggleAt reports zero as -0 so the composable routes it to the text toggle`() {
        // The field detects the zero case with Kotlin `== 0.0` (true for -0.0), the kept-scaffold path.
        assertTrue(signToggleAt(newText = "0-", oldText = "0") == 0.0)
        assertTrue(signToggleAt(newText = "0.0000-", oldText = "0.0000") == 0.0)
    }

    // ── Zero-value sign toggle: keep the raw text, just add/remove a leading '-' ──────────────

    @Test
    fun `zero keeps its text and gains a leading minus, caret held from the end`() {
        assertEquals("-0" to 2, toggleZeroSign(oldText = "0", caret = 1))       // 0|   -> -0|
        assertEquals("-0." to 3, toggleZeroSign(oldText = "0.", caret = 2))     // 0.|  -> -0.|
        assertEquals("-0." to 2, toggleZeroSign(oldText = "0.", caret = 1))     // 0|.  -> -0|.
        assertEquals("-0.0000" to 7, toggleZeroSign(oldText = "0.0000", caret = 6)) // 0.0000| -> -0.0000|
    }

    @Test
    fun `toggling a negative zero removes the leading minus`() {
        assertEquals("0" to 1, toggleZeroSign(oldText = "-0", caret = 2))        // -0|  -> 0|
        assertEquals("0.0000" to 6, toggleZeroSign(oldText = "-0.0000", caret = 7))
    }
}
