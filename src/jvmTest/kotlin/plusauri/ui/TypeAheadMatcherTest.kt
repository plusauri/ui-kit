package plusauri.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TestTimeSource

/**
 * The type-to-select rules shared by every picker — pinned on the birth-month list a design partner was on
 * when they typed "M" and nothing happened.
 */
class TypeAheadMatcherTest {

    private val months = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    ).map(::typeAheadText)

    private fun matcher(clock: TestTimeSource = TestTimeSource()) = TypeAheadMatcher(timeSource = clock)

    @Test
    fun `a single letter lands on the first match after the current option`() {
        val m = matcher()
        assertEquals(2, m.onChar('m', months, current = null), "March from nothing selected")
        assertEquals(2, m.onChar('m', months, current = 0), "March from January")
    }

    @Test
    fun `repeating the letter cycles through its matches and wraps`() {
        val m = matcher()
        assertEquals(2, m.onChar('m', months, current = null))
        assertEquals(4, m.onChar('m', months, current = 2), "May after March")
        assertEquals(2, m.onChar('m', months, current = 4), "wraps back to March")
    }

    @Test
    fun `a second different letter narrows the prefix from the current option inclusive`() {
        val m = matcher()
        assertEquals(2, m.onChar('m', months, current = null))
        assertEquals(2, m.onChar('a', months, current = 2), "\"ma\" stays on March")
        assertEquals(4, m.onChar('y', months, current = 2), "\"may\" moves on to May")
        assertNull(m.onChar('z', months, current = 4), "\"mayz\" matches nothing")
    }

    @Test
    fun `matching is case-insensitive`() {
        assertEquals(2, matcher().onChar('M', months, current = null))
    }

    @Test
    fun `a pause resets the buffer so the next letter starts a fresh search`() {
        val clock = TestTimeSource()
        val m = matcher(clock)
        assertEquals(5, m.onChar('j', months, current = 0), "June after January")
        clock += 1500.milliseconds
        assertEquals(6, m.onChar('j', months, current = 5), "fresh \"j\" after June is July")
        assertEquals("j", m.prefix, "the pause dropped the earlier \"j\"")
        clock += 200.milliseconds
        assertEquals(6, m.onChar('u', months, current = 6), "\"ju\" within the window stays on July")
        assertEquals(5, m.onChar('n', months, current = 6), "\"jun\" wraps round to June")
    }

    @Test
    fun `leading glyphs and flags are ignored, a glyph-only label matches nothing`() {
        assertEquals("money market", typeAheadText("🇩🇪 Money market"))
        assertEquals("us equity", typeAheadText("● US Equity"))
        assertEquals("", typeAheadText("● "))
        val texts = listOf("● ", "🇩🇪 Money market").map(::typeAheadText)
        assertEquals(1, matcher().onChar('m', texts, current = null))
        assertNull(matcher().onChar('x', texts, current = null))
    }

    @Test
    fun `a key-label option answers to either its key or its label`() {
        val states = listOf(
            SelectOption.KeyLabel("CA", "California"),
            SelectOption.KeyLabel("CO", "Colorado"),
            SelectOption.KeyLabel("NY", "New York"),
        )
        val m = matcher()
        assertEquals(0, m.onCharOption('c', states, current = null))
        assertEquals(1, m.onCharOption('o', states, current = 0), "\"co\" via the key")
        val m2 = matcher()
        assertEquals(0, m2.onCharOption('c', states, current = null))
        assertEquals(0, m2.onCharOption('a', states, current = 0), "\"ca\" via the key or label")
        assertEquals(0, m2.onCharOption('l', states, current = 0), "\"cal\" via the label")
    }

    @Test
    fun `a continuation that matches nothing starts again from the new letter`() {
        val chips = listOf("bs", "bs", "cf", "cf", "si", "si", "rec", "yr")
        val m = matcher()
        assertEquals(4, m.onChar('s', chips, current = 0))
        assertEquals(5, m.onChar('s', chips, current = 4))
        assertEquals(7, m.onChar('y', chips, current = 5), "y after s s is Yr, not a dead SSY")
    }
}
