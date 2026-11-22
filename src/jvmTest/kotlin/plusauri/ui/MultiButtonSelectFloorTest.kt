package plusauri.ui

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [mbsMayToggle] — the `minSelected` floor for a controlled-multi toolbar.
 *
 * The rule lives in [MultiButtonSelect] rather than at a call site on purpose. The obvious
 * hand-rolled version — an `enabledMask` computed by the host plus a bespoke tooltip — both
 * re-implements something the control should own and looks wrong: [ToggleImageButton] fades
 * disabled content to 38%, so the *selected* button holding the floor would render as though it
 * were off.
 *
 * Refusing the flip, and leaving the button drawn as selected, is what the control does instead.
 *
 * A floor and an [MbsBulkControl.AllAndClear] pair contradict each other, since the controlled-multi
 * `setAll` path does not consult the floor; a row that offers Clear should not also set a floor.
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

    /** The floor generalizes past one, even though 1 is the common value. */
    @Test
    fun aFloorOfTwoKeepsTwo() {
        assertFalse(mbsMayToggle(listOf(true, true, false), i = 0, minSelected = 2))
        assertTrue(mbsMayToggle(listOf(true, true, true), i = 0, minSelected = 2))
    }
}
