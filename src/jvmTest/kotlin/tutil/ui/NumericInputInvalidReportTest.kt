package tutil.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * An out-of-range [NumericInput] reports itself to [InvalidFields] under the name on screen: its
 * label, else its [LabeledField] caption. A field with neither reports itself unnamed, and the pane
 * names it when it reads the list. The control holds no words of its own for that.
 */
@OptIn(ExperimentalTestApi::class)
class NumericInputInvalidReportTest {

    private val unnamed = "A value out of range"

    @Test
    fun `a field with no name is named by the pane, and coming back in range withdraws it`() = runComposeUiTest {
        val fields = InvalidFields()
        var value by mutableStateOf<Double?>(150.0)
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalInvalidFields provides fields) {
                    NumericInput(value = value, onValueChange = { value = it }, range = 0.0..100.0)
                }
            }
        }
        waitForIdle()
        assertEquals(listOf(unnamed), fields.labels(unnamed))

        value = 50.0
        waitForIdle()
        assertTrue(fields.labels(unnamed).isEmpty(), fields.labels(unnamed).toString())
    }

    @Test
    fun `a field reports under its caption when it has no label`() = runComposeUiTest {
        val fields = InvalidFields()
        setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalInvalidFields provides fields, LocalFieldCaption provides "Birth year") {
                    NumericInput(value = 1063.0, onValueChange = {}, range = 1900.0..2100.0)
                }
            }
        }
        waitForIdle()
        assertEquals(listOf("Birth year"), fields.labels(unnamed))
    }
}
