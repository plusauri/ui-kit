package plusauri.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import androidx.compose.ui.unit.dp

/** The dialog wears its title and a named close, and its footer's two button families answer or back out. */
@OptIn(ExperimentalTestApi::class)
class ResizableDialogTest {

    @Test
    fun `the close button, the dismiss and the confirm each reach their callers`() = runComposeUiTest {
        var open by mutableStateOf(true)
        var answers = 0
        setContent {
            MaterialTheme {
                if (open) ResizableDialog("Choose a model", onDismiss = { open = false }, closeLabel = "Close (Esc)", defaultWidth = 420.dp) {
                    Text("Body")
                    DialogFooter {
                        DialogDismissButton("Cancel", onClick = { open = false })
                        DialogConfirmButton("Create", onClick = { answers++ })
                    }
                }
            }
        }
        onNodeWithText("Choose a model").assertExists()
        onNodeWithText("Create").performClick()
        assertEquals(1, answers)
        onNodeWithContentDescription("Close (Esc)").performClick()
        assertEquals(false, open)
    }
}
