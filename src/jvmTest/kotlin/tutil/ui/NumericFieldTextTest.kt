package tutil.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Empty-vs-zero display and blur reconciliation for [NumericInput].
 *
 * Seed zeros and callers that coerce in-progress `null` back to `0` were making money and
 * ±yr fields painful: the field snapped to "0" whenever the user cleared or left a lone `-`.
 * The focused round trip itself is covered by [NumericInputNonNullBackingTest].
 */
class NumericFieldTextTest {

    @Test
    fun `null always renders empty`() {
        assertEquals("", numericFieldText(null, allowDecimal = true))
        assertEquals("", numericFieldText(null, allowDecimal = true, blankWhenZero = true))
    }

    @Test
    fun `zero renders as 0 unless blankWhenZero`() {
        assertEquals("0", numericFieldText(0.0, allowDecimal = true, blankWhenZero = false))
        assertEquals("", numericFieldText(0.0, allowDecimal = true, blankWhenZero = true))
        assertEquals("", numericFieldText(-0.0, allowDecimal = false, blankWhenZero = true))
    }

    @Test
    fun `non-zero values format normally with blankWhenZero`() {
        assertEquals("20", numericFieldText(20.0, allowDecimal = true, blankWhenZero = true))
        assertEquals("-7", numericFieldText(-7.0, allowDecimal = false, blankWhenZero = true))
        assertEquals("1.5", numericFieldText(1.5, allowDecimal = true, blankWhenZero = true))
    }

    @Test
    fun `blur leaves a buffer that already agrees with the value alone`() {
        assertNull(numericReconciledText(20.0, "20", allowDecimal = true))
        assertNull(numericReconciledText(null, "", allowDecimal = true))
        // Typed "0" survives blankWhenZero — the user put it there.
        assertNull(numericReconciledText(0.0, "0", allowDecimal = true, blankWhenZero = true))
        assertNull(numericReconciledText(7.0, "7.", allowDecimal = true))
    }

    @Test
    fun `blur reconciles an emptied field to the default the caller stored`() {
        assertEquals("0", numericReconciledText(0.0, "", allowDecimal = true))
        assertEquals("65", numericReconciledText(65.0, "", allowDecimal = false))
        // Under blankWhenZero the reconciled text for 0 is itself empty — a rewrite to the same thing.
        assertEquals("", numericReconciledText(0.0, "", allowDecimal = true, blankWhenZero = true))
    }

    @Test
    fun `blur reconciles an abandoned prefix or a stale number`() {
        assertEquals("", numericReconciledText(null, "-", allowDecimal = true))
        assertEquals("0", numericReconciledText(0.0, "-.", allowDecimal = true))
        assertEquals("42", numericReconciledText(42.0, "41", allowDecimal = true))
    }
}
