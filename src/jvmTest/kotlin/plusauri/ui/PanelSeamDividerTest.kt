package plusauri.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Either side folds on a click, names the click it offers, and grows its panel with a drag toward the content. */
@OptIn(ExperimentalTestApi::class)
class PanelSeamDividerTest {

    @Test
    fun `the pill names what a click does and folds the panel, on either side`() {
        for (side in PanelSide.entries) runComposeUiTest {
            var collapsed by mutableStateOf(false)
            setContent {
                MaterialTheme {
                    Row(Modifier.height(300.dp)) {
                        PanelSeamDivider(side, collapsed, { collapsed = !collapsed }, hideLabel = "Hide list", showLabel = "Show list")
                    }
                }
            }
            onNodeWithContentDescription("Hide list").performClick()
            assertTrue(collapsed, "$side")
            onNodeWithContentDescription("Show list").performClick()
            assertEquals(false, collapsed, "$side")
        }
    }

    @Test
    fun `a drag toward the content grows the panel, whichever side it is on`() {
        for ((side, toward) in listOf(PanelSide.Leading to 1f, PanelSide.Trailing to -1f)) runComposeUiTest {
            var grown = 0f
            setContent {
                MaterialTheme {
                    Row(Modifier.height(300.dp)) {
                        PanelSeamDivider(side, false, {}, "Hide list", "Show list", onResize = { grown += it })
                    }
                }
            }
            // Grab the rule above the pill, so the drag starts on the seam and not on the click target.
            onNodeWithContentDescription("Hide list").performMouseInput {
                moveTo(center.copy(y = 2f))
                press()
                moveBy(Offset(40f * toward, 0f))
                release()
            }
            assertTrue(grown > 0f, "$side grew by $grown")
        }
    }
}
