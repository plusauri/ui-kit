package plusauri.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * On a real Mac, the arrow keys arrive with a character from the private-use area (← is U+F702).
 * Chip rows and pickers read typed characters as type-ahead, so an arrow read as a character would
 * be swallowed as an unmatched letter, and ← would do nothing.
 * Compose's test key input carries no character, so the rule is checked on the code point itself.
 */
class MacArrowKeysAreNotTypingTest {

    @Test
    fun `the Mac arrow and function-key characters are keys, not typing`() {
        for (cp in listOf(0xF700, 0xF701, 0xF702, 0xF703, 0xF729, 0xF72B)) assertNull(typedCharOf(cp), "U+%04X".format(cp))
    }

    @Test
    fun `letters, digits and punctuation are typing`() {
        assertEquals('b', typedCharOf('b'.code))
        assertEquals('7', typedCharOf('7'.code))
        assertEquals('?', typedCharOf('?'.code))
        assertNull(typedCharOf(' '.code))
        assertNull(typedCharOf(0))
    }
}
