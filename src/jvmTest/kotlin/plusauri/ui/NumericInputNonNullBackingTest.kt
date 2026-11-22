package plusauri.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsNodeInteraction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * The "non-null backing" round trip on [NumericInput] / [PercentInput].
 *
 * A caller whose sink cannot hold `null` coerces on every change — `onValueChange = { x = it ?: 0.0 }`
 * — and pushes the result straight back in as `value`. Without the field's focus-aware sync, that
 * echo would rewrite the buffer mid-edit: backspace the last digit, the coerced default re-renders
 * as an un-deletable "0" (or "65", or whatever the default is) and the field is locked.
 *
 * These drive the real control through real keystrokes and a real focus change, because the risk
 * lives in the round trip between the field and its parent — calling `onValueChange` directly
 * would pass even if that round trip were broken. Each test is written as the plain anti-pattern,
 * with no mirror or `lastExternal` guard at the call site: that is the supported contract.
 */
@OptIn(ExperimentalTestApi::class)
class NumericInputNonNullBackingTest {

    private fun SemanticsNodeInteraction.editableText(): String =
        fetchSemanticsNode().config[SemanticsProperties.EditableText].text

    /**
     * A field backed by a non-null `Double` seeded at [seed], with a button beside it that takes
     * focus when clicked. Returns the field text after [drive] and the value the caller holds.
     */
    private fun driveNonNullField(
        seed: Double,
        allowDecimal: Boolean = true,
        drive: (field: SemanticsNodeInteraction, blur: () -> Unit) -> Unit,
    ): Pair<String, Double> {
        var stored = seed
        var fieldText = ""
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    var x by remember { mutableStateOf(seed) }
                    Column {
                        NumericInput(
                            value = x,
                            onValueChange = { x = it ?: seed },
                            allowDecimal = allowDecimal,
                        )
                        Button(onClick = {}) { Text("Elsewhere") }
                    }
                    stored = x
                }
            }
            val field = onNode(hasSetTextAction())
            field.performClick()
            drive(field) { onNode(hasText("Elsewhere")).performClick() }
            waitForIdle()
            fieldText = field.editableText()
        }
        return fieldText to stored
    }

    @Test
    fun `backspacing the last digit empties the field even though the caller stores a default`() {
        val (text, stored) = driveNonNullField(seed = 0.0) { field, _ ->
            field.performTextReplacement("7")
            field.performTextClearance()
        }

        assertEquals("", text, "the coerced echo must not re-render as an un-deletable digit")
        assertEquals(0.0, stored, "the caller still sees the coerced default")
    }

    @Test
    fun `a non-zero default is held back the same way`() {
        // blankWhenZero can't help here — the default is 65, not 0.
        val (text, _) = driveNonNullField(seed = 65.0, allowDecimal = false) { field, _ ->
            field.performTextClearance()
            for (digit in "7") field.performTextInput(digit.toString())
        }

        assertEquals("7", text, "clearing then typing must not fight a 65 that snaps back")
    }

    @Test
    fun `an in-progress decimal survives the echo`() {
        val (text, _) = driveNonNullField(seed = 0.0) { field, _ ->
            field.performTextReplacement("7")
            field.performTextInput(".")
        }

        assertEquals("7.", text)
    }

    @Test
    fun `blur reconciles an emptied field to the default the caller stored`() {
        val (text, stored) = driveNonNullField(seed = 65.0, allowDecimal = false) { field, blur ->
            field.performTextClearance()
            blur()
        }

        assertEquals("65", text, "on blur the field shows what the caller actually holds")
        assertEquals(65.0, stored)
    }

    @Test
    fun `an external change while unfocused still lands`() {
        var text = ""
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    var x by remember { mutableStateOf(1.0) }
                    Column {
                        NumericInput(value = x, onValueChange = { x = it ?: 1.0 }, allowDecimal = false)
                        Button(onClick = { x = 42.0 }) { Text("Set") }
                    }
                }
            }
            onNode(hasText("Set")).performClick()
            waitForIdle()
            text = onNode(hasSetTextAction()).editableText()
        }

        assertEquals("42", text)
    }

    @Test
    fun `PercentInput with a non-null sink can be emptied and retyped`() {
        var stored = 0.05
        var text = ""
        runComposeUiTest {
            setContent {
                MaterialTheme {
                    var share by remember { mutableStateOf(0.05) }
                    PercentInput(value = share, onValueChange = { share = it ?: 0.0 })
                    stored = share
                }
            }
            val field = onNode(hasSetTextAction())
            field.performClick()
            field.performTextClearance()
            for (digit in "12") field.performTextInput(digit.toString())
            waitForIdle()
            text = field.editableText()
        }

        assertEquals("12", text)
        assertEquals(0.12, stored, 1e-12)
    }
}
