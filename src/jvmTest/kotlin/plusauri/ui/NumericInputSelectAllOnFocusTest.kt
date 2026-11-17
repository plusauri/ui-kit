package plusauri.ui

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Clicking into a [NumericInput] selects its value, so the first keystroke replaces it.
 *
 * A design partner, reviewing the cash rate: "when I click in here, I'd expect to see that just
 * blank out or to highlight it and overwrite it." The trap is ordering: a click focuses the field
 * and *then* places its caret, so a selection made on focus alone is wiped by the click that made
 * it. The assertion is therefore on what typing does after a real click, not on the focus handler.
 */
@OptIn(ExperimentalTestApi::class)
class NumericInputSelectAllOnFocusTest {

    @Test
    fun `a click selects the value and typing replaces it`() = runComposeUiTest {
        var emitted: Double? = 3.8
        setContent {
            MaterialTheme {
                var v by remember { mutableStateOf<Double?>(3.8) }
                NumericInput(
                    value = v,
                    onValueChange = { v = it; emitted = it },
                    modifier = Modifier.width(120.dp),
                )
            }
        }

        val field = onNode(hasSetTextAction())
        field.performClick()
        waitForIdle()
        assertEquals(
            TextRange(0, 3),
            field.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange],
            "the click must leave the whole value selected",
        )

        field.performTextInput("5")
        waitForIdle()
        assertEquals(5.0, emitted)
    }

    /** Only the focusing click selects: a second click places the caret, so a digit can be fixed. */
    @Test
    fun `a second click places the caret`() = runComposeUiTest {
        setContent {
            MaterialTheme {
                var v by remember { mutableStateOf<Double?>(3.8) }
                NumericInput(value = v, onValueChange = { v = it }, modifier = Modifier.width(120.dp))
            }
        }

        val field = onNode(hasSetTextAction())
        field.performClick()
        waitForIdle()
        // Past the double-click window: a double-click selects the word, which is not this case.
        mainClock.advanceTimeBy(1_000)
        field.performClick()
        waitForIdle()
        val selection = field.fetchSemanticsNode().config[SemanticsProperties.TextSelectionRange]
        assertEquals(true, selection.collapsed, "second click should leave a caret, not a selection")
    }
}
