package plusauri.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [HoverDetailOptions.resizable]: a hover card grows from its corner grip, stays open while the
 * drag carries the pointer off it, and its size is remembered for its host ([HoverCardSizes]) so
 * the next card from the same host opens at it.
 */
@OptIn(ExperimentalTestApi::class)
class HoverCardResizeTest {

    private val key = "hover-card-resize-test"
    private var options by mutableStateOf(HoverDetailOptions(sizeKey = key))

    @AfterEach
    fun forget() = HoverCardSizes.clear()

    private fun ComposeUiTest.cardShown() =
        onAllNodesWithText("CARD").fetchSemanticsNodes().isNotEmpty()

    private fun ComposeUiTest.gripShown() =
        onAllNodesWithTag(HoverCardGripTag).fetchSemanticsNodes().isNotEmpty()

    /** With the card up there are two roots, the anchor's and the popup's; the popup's spans the window. */
    private fun ComposeUiTest.windowRoot() = onAllNodes(isRoot()).let { roots ->
        val widest = roots.fetchSemanticsNodes().withIndex().maxBy { it.value.boundsInRoot.width }.index
        roots[widest]
    }

    private fun ComposeUiTest.openCard() {
        setContent {
            CompositionLocalProvider(LocalHoverDetailsDefault provides true) {
                HoverDetailPopup(detail = { Text("CARD") }, options = options) {
                    Box(Modifier.size(600.dp, 400.dp))
                }
            }
        }
        // The default placement opens under the pointer, so the card is hovered — and shows its grip — at once.
        onRoot().performMouseInput { moveTo(Offset(40f, 40f)) }
        mainClock.advanceTimeBy(100)
        waitForIdle()
        assertTrue(cardShown(), "hovering the anchor opens the card")
    }

    @Test
    fun dragGrowsTheCardAndRemembersItForTheHost() = runComposeUiTest {
        openCard()
        assertTrue(gripShown(), "the grip shows while the pointer is on the card")
        val before = onNodeWithTag(HoverCardGripTag).fetchSemanticsNode().boundsInRoot

        // Driven from the root, in window coordinates as a real mouse is: input aimed at the grip node
        // re-resolves its moving bounds on every step and would add the grip's own travel to the hand's.
        windowRoot().performMouseInput {
            moveTo(before.center)
            press()
            // Far enough that the pointer leaves the card mid-drag: the card must hold.
            repeat(10) { moveBy(Offset(20f, 12f)) }
        }
        mainClock.advanceTimeBy(1_000)
        waitForIdle()
        assertTrue(cardShown(), "a drag in progress holds the card open")
        windowRoot().performMouseInput { release() }
        waitForIdle()

        val size = HoverCardSizes[key]
        assertNotNull(size, "the drag stored a size for the host")
        val after = onNodeWithTag(HoverCardGripTag).fetchSemanticsNode().boundsInRoot
        assertTrue(after.right > before.right + 50f, "the grip moved right with the drag: $before → $after")
        assertTrue(after.bottom > before.bottom + 30f, "and down: $before → $after")
        // The card's top-left is pinned: growing it does not slide it, so the grip followed the hand
        // (less the touch slop the drag detector spends before it reports movement).
        assertTrue(after.right - before.right in 150f..204f, "the grip followed the drag: $before → $after")
    }

    @Test
    fun aCardThatIsNotResizableHasNoGrip() = runComposeUiTest {
        options = HoverDetailOptions(sizeKey = key, resizable = false)
        openCard()
        assertFalse(gripShown(), "resizable = false draws no grip")
    }
}
