package plusauri.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** The row is one switch: its words are a click target, and it reports its state where the label is. */
@OptIn(ExperimentalTestApi::class)
class LabeledSlideToggleTest {

    @Test
    fun `a click on the label toggles, and the row says on or off`() = runComposeUiTest {
        var on by mutableStateOf(false)
        setContent { MaterialTheme { LabeledSlideToggle("Auto-save", on, { on = it }, hint = "Save after every change") } }

        onNodeWithText("Auto-save").assertIsOff().performClick()
        assertEquals(true, on)
        onNodeWithText("Auto-save").assertIsOn()
        onNodeWithText("Save after every change").performClick()
        assertEquals(false, on)
    }

    @Test
    fun `a disabled row keeps its value and ignores clicks`() = runComposeUiTest {
        var changes = 0
        setContent { MaterialTheme { LabeledSlideToggle("Keep", checked = true, onCheckedChange = { changes++ }, enabled = false) } }

        onNodeWithText("Keep").assertIsNotEnabled().assertIsOn().performClick()
        assertEquals(0, changes)
    }
}
