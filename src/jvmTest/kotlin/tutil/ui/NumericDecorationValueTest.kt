package tutil.ui

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Guards the empty-label float workaround used by [NumericInput]'s decoration box.
 * Call-site safety: unlabeled fields must keep empty decoration (placeholders still work);
 * labeled empty fields get a zero-width space so Material parks the label on the border.
 */
class NumericDecorationValueTest {

    @Test
    fun `non-empty text passes through with or without a label`() {
        assertEquals("1234.5", numericDecorationValue("1234.5", hasLabel = true))
        assertEquals("1234.5", numericDecorationValue("1234.5", hasLabel = false))
        assertEquals("0", numericDecorationValue("0", hasLabel = true))
    }

    @Test
    fun `empty with label forces zero-width space for minimized Material label`() {
        assertEquals("\u200B", numericDecorationValue("", hasLabel = true))
    }

    @Test
    fun `empty without label stays empty so unlabeled clients are unchanged`() {
        assertEquals("", numericDecorationValue("", hasLabel = false))
    }
}
