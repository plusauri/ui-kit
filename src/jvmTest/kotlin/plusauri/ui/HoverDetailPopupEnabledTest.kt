package plusauri.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [HoverDetailPopup]'s `enabled` is how an anchor with its own hit-test (a scatter plot's marks)
 * says "the pointer left the mark". It must behave like leaving the anchor: an **unlatched** card
 * closes at once, but a **latched** one — the reader dwelt, the card tinted — stays for the flight
 * grace so the pointer can travel off the mark to it.
 *
 * The failure this guards against: if `enabled = false` closed the card unconditionally, a
 * scatter's card would tint on a dwell and then vanish on the first move toward it.
 */
@OptIn(ExperimentalTestApi::class)
class HoverDetailPopupEnabledTest {

    private var enabled by mutableStateOf(true)

    private fun ComposeUiTest.cardShown() =
        onAllNodesWithText("CARD").fetchSemanticsNodes().isNotEmpty()

    private fun ComposeUiTest.hostPopup() {
        setContent {
            CompositionLocalProvider(
                LocalHoverDetailsDefault provides true,
                LocalHoverFlight provides remember { HoverFlightState() },
            ) {
                HoverDetailPopup(
                    detail = { Text("CARD") },
                    options = FollowsPointerCardOptions,
                    enabled = enabled,
                ) {
                    Box(Modifier.size(400.dp, 200.dp))
                }
            }
        }
        onRoot().performMouseInput { moveTo(Offset(100f, 100f)) }
        mainClock.advanceTimeBy(100)
        waitForIdle()
        assertTrue(cardShown(), "hovering the anchor opens the card")
    }

    @Test
    fun latchedCardOutlivesLeavingTheMark() = runComposeUiTest {
        enabled = true
        hostPopup()
        mainClock.advanceTimeBy(ChartTipCopyLatchMs * 2)
        waitForIdle()

        enabled = false
        mainClock.advanceTimeBy(100)
        waitForIdle()
        assertTrue(cardShown(), "a latched card waits for the reader to reach it")

        mainClock.advanceTimeBy(FollowsPointerCardOptions.flightGraceMs + 200)
        waitForIdle()
        assertFalse(cardShown(), "…but not forever: the flight grace runs out")
    }

    @Test
    fun unlatchedCardClosesAtOnce() = runComposeUiTest {
        enabled = true
        hostPopup()

        enabled = false
        mainClock.advanceTimeBy(50)
        waitForIdle()
        assertFalse(cardShown(), "an unlatched card was following the pointer; nothing is under it now")
    }
}
