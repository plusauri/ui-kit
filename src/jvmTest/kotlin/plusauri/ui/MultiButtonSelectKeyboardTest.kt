package plusauri.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

/**
 * A chip toolbar from the keyboard ([MultiButtonSelect]): a [FocusHandoff] lands on the first
 * button, the arrows move along the toolbar, Enter toggles, and Shift-Enter is "only this".
 */
@OptIn(ExperimentalTestApi::class)
class MultiButtonSelectKeyboardTest {

    private val labels = listOf("Alpha", "Beta", "Gamma", "Bravo")

    private fun ComposeUiTest.show(mask: MutableList<Boolean>, handoff: FocusHandoff) {
        setContent {
            MaterialTheme {
                MultiButtonSelect(
                    buttonSelections = labels.map { ButtonData(ButtonImage.Unicode(it)) to ToggleState.Deselected },
                    selectedMask = mask.toList(),
                    onToggle = { mask[it] = !mask[it] },
                    onSetAll = { on -> mask.indices.forEach { mask[it] = on } },
                    bulkControl = MbsBulkControl.AllAndClear,
                    bulkLabels = MbsBulkLabels(selectAll = "All", clear = "Clear"),
                    toolbarOnly = true,
                    focusHandoff = handoff,
                )
            }
        }
    }

    /** The focusable surface of the button labeled [label] (its text merges up into it). */
    private fun ComposeUiTest.button(label: String) = onNode(hasClickAction() and hasText(label))

    @Test
    fun `the handoff lands on the first chip, arrows move, Enter toggles, Shift-Enter isolates`() = runComposeUiTest {
        val mask = mutableStateListOf(true, false, true, false)
        val handoff = FocusHandoff()
        show(mask, handoff)

        runOnIdle { handoff.request() }
        waitForIdle()
        button("Alpha").assertIsFocused()
        assertFalse(handoff.pending, "a handoff is taken once")

        button("Alpha").performKeyInput { pressKey(Key.DirectionRight) }
        waitForIdle()
        button("Beta").assertIsFocused()

        button("Beta").performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        assertEquals(listOf(true, true, true, false), mask.toList())

        button("Beta").performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Enter) } }
        waitForIdle()
        assertEquals(listOf(false, true, false, false), mask.toList())

        button("Beta").performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.Enter) } }
        waitForIdle()
        assertEquals(listOf(true, false, true, true), mask.toList(), "again: all but this one")

        button("Beta").performKeyInput { pressKey(Key.DirectionLeft) }
        waitForIdle()
        button("Alpha").assertIsFocused()
    }

    @Test
    fun `a letter focuses the next button it starts, across every toolbar in the group`() = runComposeUiTest {
        val left = mutableStateListOf(false, false)
        val right = mutableStateListOf(false)
        val handoff = FocusHandoff()
        setContent {
            MaterialTheme {
                val group = androidx.compose.runtime.remember { ButtonTypeAhead() }
                androidx.compose.foundation.layout.Row {
                    MultiButtonSelect(
                        buttonSelections = listOf("📈BS", "▦BS").map { ButtonData(ButtonImage.Unicode(it)) to ToggleState.Deselected },
                        selectedMask = left.toList(),
                        onToggle = { left[it] = !left[it] },
                        toolbarOnly = true,
                        focusHandoff = handoff,
                        typeAhead = group,
                        typeAheadSlot = 0,
                    )
                    MultiButtonSelect(
                        buttonSelections = listOf(ButtonData(ButtonImage.Unicode("Bra")) to ToggleState.Deselected),
                        selectedMask = right.toList(),
                        onToggle = { right[it] = !right[it] },
                        toolbarOnly = true,
                        typeAhead = group,
                        typeAheadSlot = 1,
                    )
                }
            }
        }
        runOnIdle { handoff.request() }
        waitForIdle()
        // Focus is on the first BS; b goes to the next b — the table — and b again to Bra.
        button("📈BS").performKeyInput { pressKey(Key.B) }
        waitForIdle()
        button("▦BS").assertIsFocused()
        button("▦BS").performKeyInput { pressKey(Key.B) }
        waitForIdle()
        button("Bra").assertIsFocused()
        // Shift (a capital) searches backwards: from Bra back to the table.
        button("Bra").performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.B) } }
        waitForIdle()
        button("▦BS").assertIsFocused()
        button("▦BS").performKeyInput { pressKey(Key.B) }
        waitForIdle()
        button("Bra").performKeyInput { pressKey(Key.Enter) }
        waitForIdle()
        assertEquals(listOf(true), right.toList())
        assertEquals(listOf(false, false), left.toList())
    }

    @Test
    fun `bang turns every chip on, and off again when they already are`() = runComposeUiTest {
        val mask = mutableStateListOf(true, false, true, false)
        val handoff = FocusHandoff()
        show(mask, handoff)
        runOnIdle { handoff.request() }
        waitForIdle()
        button("Alpha").performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.One) } }
        waitForIdle()
        assertEquals(listOf(true, true, true, true), mask.toList())
        button("Alpha").performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.One) } }
        waitForIdle()
        assertEquals(listOf(false, false, false, false), mask.toList())
    }
}
