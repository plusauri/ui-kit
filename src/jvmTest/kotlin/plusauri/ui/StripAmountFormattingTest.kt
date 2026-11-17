package plusauri.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * A pasted amount arrives as it was copied — `$1,752,700` from a listing site — and [NumericInput]
 * used to reject the whole paste because `,` and `$` fail the numeric-prefix check.
 */
@OptIn(ExperimentalTestApi::class)
class StripAmountFormattingTest {

    /** Pastes [pasted] over the whole of a real [NumericInput]; returns its text and emitted value. */
    private fun paste(pasted: String, allowDecimal: Boolean = true): Pair<String, Double?> {
        var stored: Double? = null
        var text = ""
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    var x by remember { mutableStateOf<Double?>(null) }
                    NumericInput(value = x, onValueChange = { x = it }, allowDecimal = allowDecimal)
                    stored = x
                }
            }
            val field = onNode(hasSetTextAction())
            field.performClick()
            field.performTextReplacement(pasted)
            waitForIdle()
            text = field.fetchSemanticsNode().config[SemanticsProperties.EditableText].text
        }
        return text to stored
    }

    @Test
    fun `pasting a formatted amount into the real field enters the number`() {
        assertEquals("1752700" to 1752700.0, paste("$1,752,700"))
        assertEquals("477103.34" to 477103.34, paste("477,103.34"))
        assertEquals("1752700" to 1752700.0, paste("$1,752,700.00", allowDecimal = false))
    }


    @Test
    fun `plain digits need no change`() {
        assertNull(stripAmountFormatting("1752700", 7, allowDecimal = true))
        assertNull(stripAmountFormatting("477103.34", 9, allowDecimal = true))
    }

    @Test
    fun `commas and dollar sign are stripped`() {
        assertEquals("1752700" to 7, stripAmountFormatting("1,752,700", 9, allowDecimal = true))
        assertEquals("1752700" to 7, stripAmountFormatting("$1,752,700", 10, allowDecimal = true))
        assertEquals("477103.34" to 9, stripAmountFormatting("477,103.34", 10, allowDecimal = true))
    }

    @Test
    fun `surrounding spaces and non-breaking spaces are stripped`() {
        assertEquals("2061.82" to 7, stripAmountFormatting(" $2,061.82 ", 11, allowDecimal = true))
    }

    @Test
    fun `negative amounts keep their sign`() {
        assertEquals("-1200" to 5, stripAmountFormatting("-$1,200", 7, allowDecimal = true))
    }

    @Test
    fun `caret moves left by the characters removed before it`() {
        // Pasted into the middle of an existing "99": "9" + "$1,000" + "9", caret after the paste.
        assertEquals("910009" to 5, stripAmountFormatting("9$1,0009", 7, allowDecimal = true))
    }

    @Test
    fun `whole-number field drops zero cents but keeps real ones for validation to reject`() {
        assertEquals("1752700" to 7, stripAmountFormatting("$1,752,700.00", 13, allowDecimal = false))
        assertEquals("477103.34" to 9, stripAmountFormatting("477,103.34", 10, allowDecimal = false))
    }
}
