package plusauri.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** [scaleCharAt]: one `k` / `m` / `b` typed into a number scales it; the capital divides. */
class ScaleCharAtTest {

    @Test
    fun `lowercase multiplies`() {
        assertEquals(5_000.0, scaleCharAt(newText = "5k", oldText = "5"))
        assertEquals(5_000_000.0, scaleCharAt(newText = "5m", oldText = "5"))
        assertEquals(5_000_000_000.0, scaleCharAt(newText = "5b", oldText = "5"))
    }

    @Test
    fun `uppercase divides`() {
        assertEquals(5.0, scaleCharAt(newText = "5000K", oldText = "5000"))
        assertEquals(2.5, scaleCharAt(newText = "2500000M", oldText = "2500000"))
        assertEquals(3.0, scaleCharAt(newText = "3000000000B", oldText = "3000000000"))
    }

    @Test
    fun `the key works wherever the caret is`() {
        assertEquals(12_000.0, scaleCharAt(newText = "k12", oldText = "12"))
        assertEquals(12_000.0, scaleCharAt(newText = "1k2", oldText = "12"))
        assertEquals(-1_500.0, scaleCharAt(newText = "-1.5k", oldText = "-1.5"))
    }

    @Test
    fun `anything but one scale key typed into a number is not a scale`() {
        assertNull(scaleCharAt(newText = "5x", oldText = "5"), "not a scale key")
        assertNull(scaleCharAt(newText = "5kk", oldText = "5"), "two characters")
        assertNull(scaleCharAt(newText = "k", oldText = ""), "nothing to scale")
        assertNull(scaleCharAt(newText = "5", oldText = "5"), "no insertion")
    }
}
