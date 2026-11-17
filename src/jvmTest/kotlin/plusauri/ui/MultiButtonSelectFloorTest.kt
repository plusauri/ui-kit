package plusauri.ui

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [mbsMayToggle] — the `minSelected` floor for a controlled-multi toolbar.
 *
 * The rule lives in [MultiButtonSelect] rather than at a call site on purpose. The Sensitivity
 * panel's two rows first grew a hand-rolled version of it — an `enabledMask` computed by the host
 * plus a bespoke tooltip — which was both a re-implementation of something the control should own
 * and visibly wrong: [ToggleImageButton] fades disabled content to 38%, so the *selected* button
 * holding the floor rendered as though it were off.
 *
 * Refusing the flip, and leaving the button drawn as selected, is what the control does instead.
 *
 * **No production call site passes `minSelected` any more** (as of 2026-09-09). The Bar-height row
 * became pick-one, and the Types row traded the floor for the [MbsBulkControl.AllAndClear] pair —
 * a floor and a Clear button contradict each other, since the controlled-multi `setAll` path does
 * not consult the floor. The rule is kept, and kept tested, because it is the correct behavior for
 * a controlled-multi row that genuinely cannot be empty, and because the hand-rolled version above
 * is what call sites regrow without it. Treat this as coverage for an available guarantee, not for
 * live code.
 */
class MultiButtonSelectFloorTest {

    @Test
    fun aFloorOfZeroNeverBlocksAnything() {
        val onlyOne = listOf(true, false, false)
        onlyOne.indices.forEach { i ->
            assertTrue(mbsMayToggle(onlyOne, i, minSelected = 0), "index $i is free with no floor")
        }
    }

    @Test
    fun turningSomethingOnIsAlwaysAllowed() {
        assertTrue(mbsMayToggle(listOf(true, false), i = 1, minSelected = 1))
    }

    @Test
    fun theLastSelectedButtonRefusesToComeOff() {
        assertFalse(
            mbsMayToggle(listOf(false, true, false), i = 1, minSelected = 1),
            "the sole survivor holds the floor",
        )
    }

    @Test
    fun aSelectedButtonComesOffWhileOthersRemain() {
        assertTrue(
            mbsMayToggle(listOf(true, true, false), i = 0, minSelected = 1),
            "two selected means either may go",
        )
    }

    /** The floor generalizes past one, even though 1 is the only value any call site has ever used. */
    @Test
    fun aFloorOfTwoKeepsTwo() {
        assertFalse(mbsMayToggle(listOf(true, true, false), i = 0, minSelected = 2))
        assertTrue(mbsMayToggle(listOf(true, true, true), i = 0, minSelected = 2))
    }
}
