package plusauri.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Where a hover card lands: [tipCardOffset], the one placer behind every popup and every
 * canvas-drawn card. The popup itself is a separate window the UI tests cannot see into, so the
 * contract is pinned on the placement math, in particular that a [BelowAnchorAtPointer] card
 * **never contains the pointer**, which is the whole reason a list's rows stay clickable while
 * their card is open.
 */
class TipPlacementTest {

    private val window = IntSize(1400, 900)
    private val card = IntSize(320, 180)
    private val gap = 6

    /** A full-width list row, 40 px tall, a third of the way down the window. */
    private val row = IntRect(left = 100, top = 300, right = 1300, bottom = 340)

    private fun IntOffset.contains(pointerInWindow: Offset): Boolean =
        pointerInWindow.x >= x && pointerInWindow.x <= x + card.width &&
            pointerInWindow.y >= y && pointerInWindow.y <= y + card.height

    @Test
    fun `under the cursor, the pointer lands just inside the card`() {
        val pointer = Offset(500f, 20f)
        val at = tipCardOffset(TipPlacement.UnderPointer, row, pointer, card, window, gap)
        assertTrue(at.contains(Offset(row.left + pointer.x, row.top + pointer.y)), "pointer inside the card at $at")
    }

    @Test
    fun `below the anchor, the card hangs a clear gap under the row and the pointer is outside it`() {
        val pointer = Offset(500f, 20f)
        val at = tipCardOffset(BelowAnchorAtPointer, row, pointer, card, window, gap)
        assertEquals(row.bottom + gap, at.y, "top edge sits the gap below the row")
        assertEquals(row.left + 500 - 12, at.x, "at the pointer's x, the usual hair to the left")
        assertFalse(at.contains(Offset(row.left + pointer.x, row.top + pointer.y)), "pointer must not be inside the card at $at")
    }

    @Test
    fun `below the anchor flips above when there is no room below, still clear of the row`() {
        val lowRow = IntRect(left = 100, top = 800, right = 1300, bottom = 840)
        val pointer = Offset(500f, 20f)
        val at = tipCardOffset(BelowAnchorAtPointer, lowRow, pointer, card, window, gap)
        assertEquals(lowRow.top - gap - card.height, at.y, "bottom edge sits the gap above the row")
        assertFalse(at.contains(Offset(lowRow.left + pointer.x, lowRow.top + pointer.y)))
    }

    @Test
    fun `parked right-aligned pins the card's bottom-right to the corner and ignores the pointer`() {
        val corner = IntOffset(1300, 700)
        val a = tipCardOffset(TipPlacement.Parked(corner, TipCorner.BottomEnd), row, Offset(50f, 5f), card, window, gap)
        val b = tipCardOffset(TipPlacement.Parked(corner, TipCorner.BottomEnd), row, Offset(900f, 35f), card, window, gap)
        assertEquals(IntOffset(corner.x - card.width, corner.y - card.height), a)
        assertEquals(a, b, "a parked card does not follow the pointer")
    }

    @Test
    fun `parked left-aligned pins the card's bottom-left to the corner`() {
        val corner = IntOffset(100, 700)
        val at = tipCardOffset(TipPlacement.Parked(corner, TipCorner.BottomStart), row, Offset(50f, 5f), card, window, gap)
        assertEquals(IntOffset(corner.x, corner.y - card.height), at)
    }

    @Test
    fun `beside the anchor above, centered, sits the gap over the anchor and flips below near the top`() {
        val icon = IntRect(left = 600, top = 400, right = 624, bottom = 424)
        val at = tipCardOffset(TipPlacement.BesideAnchor(TipSide.Above), icon, Offset(12f, 12f), card, window, gap)
        assertEquals(icon.top - gap - card.height, at.y)
        assertEquals(icon.left + (icon.width - card.width) / 2, at.x, "centered on the icon")
        val topIcon = IntRect(left = 600, top = 10, right = 624, bottom = 34)
        val flipped = tipCardOffset(TipPlacement.BesideAnchor(TipSide.Above), topIcon, Offset(12f, 12f), card, window, gap)
        assertEquals(topIcon.bottom + gap, flipped.y, "no room above: hangs below")
    }

    @Test
    fun `start hangs the card off the anchor's leading edge at the pointer's y, mirroring end`() {
        val cell = IntRect(left = 600, top = 300, right = 900, bottom = 330)
        val end = tipCardOffset(TipPlacement.BesideAnchor(TipSide.End), cell, Offset(40f, 15f), card, window, gap)
        val start = tipCardOffset(TipPlacement.BesideAnchor(TipSide.Start), cell, Offset(40f, 15f), card, window, gap)
        assertEquals(cell.right + gap, end.x)
        assertEquals(cell.left - gap - card.width, start.x, "card's trailing edge a gap left of the cell")
        assertEquals(end.y, start.y, "same vertical rule as End")
    }

    @Test
    fun `follows the pointer above-right by preference, below only when above will not fit`() {
        val canvas = IntSize(800, 600)
        val whole = IntRect(0, 0, 800, 600)
        val mid = tipCardOffset(TipPlacement.FollowsPointer, whole, Offset(100f, 400f), card, canvas, 0, edgePadPx = 4)
        assertEquals(IntOffset(100 + FollowsPointerDxPx, 400 - card.height - FollowsPointerDyPx), mid, "room above: bottom edge just over the pointer")
        // A taller card that still fits stays above — height must not flip a card that fits.
        val tall = tipCardOffset(TipPlacement.FollowsPointer, whole, Offset(100f, 400f), IntSize(320, 300), canvas, 0, edgePadPx = 4)
        assertEquals(400 - 300 - FollowsPointerDyPx, tall.y, "a taller card that still fits stays above")
        val nearTop = tipCardOffset(TipPlacement.FollowsPointer, whole, Offset(100f, 100f), card, canvas, 0, edgePadPx = 4)
        assertEquals(100 + FollowsPointerDyPx, nearTop.y, "no room above: top edge just under the pointer")
        val nearRight = tipCardOffset(TipPlacement.FollowsPointer, whole, Offset(780f, 400f), card, canvas, 0, edgePadPx = 4)
        assertTrue(nearRight.x + card.width <= 780, "flipped to the left of the pointer: $nearRight")
        assertTrue(nearRight.x >= 4 && nearRight.y >= 4)
    }

    @Test
    fun `a parked card with clampY off is never pulled back up over its anchor`() {
        val corner = IntOffset(200, 850)
        val at = tipCardOffset(TipPlacement.Parked(corner, TipCorner.TopStart, clampY = false), row, Offset.Zero, card, window, gap)
        assertEquals(corner.y, at.y, "top edge stays on the corner even though the card runs off the window")
    }

    @Test
    fun `end of anchor hangs off the cell's trailing edge at the pointer's y, clamping back over it at the edge`() {
        val nameCell = IntRect(left = 100, top = 300, right = 340, bottom = 340)
        val pointer = Offset(120f, 20f)
        val at = tipCardOffset(EndOfAnchor, nameCell, pointer, card, window, gap)
        assertEquals(nameCell.right + gap, at.x, "just right of the underbar")
        assertEquals(nameCell.top + 20 - 12, at.y, "at the pointer's row")
        assertFalse(at.contains(Offset(nameCell.left + pointer.x, nameCell.top + pointer.y)), "pointer stays on the row")
        val farRight = IntRect(left = 1100, top = 300, right = 1340, bottom = 340)
        val clamped = tipCardOffset(EndOfAnchor, farRight, pointer, card, window, gap)
        assertEquals(window.width - card.width, clamped.x, "no room to the right: pulled back over its own row, never to the far side")
    }

    @Test
    fun `start of anchor hangs off a last-column chip's leading edge, clamping back over it at the edge`() {
        val chip = IntRect(left = 1100, top = 300, right = 1180, bottom = 320)
        val pointer = Offset(40f, 10f)
        val at = tipCardOffset(StartOfAnchor, chip, pointer, card, window, gap)
        assertEquals(chip.left - gap - card.width, at.x, "just left of the chip")
        assertEquals(chip.top + 10 - 12, at.y, "at the pointer's row")
        assertFalse(at.contains(Offset(chip.left + pointer.x, chip.top + pointer.y)), "pointer stays on the chip")
        val farLeft = IntRect(left = 100, top = 300, right = 180, bottom = 320)
        val clamped = tipCardOffset(StartOfAnchor, farLeft, pointer, card, window, gap)
        assertEquals(0, clamped.x, "no room to the left: pulled back over its own row, never to the far side")
    }

    /** Where a left-table row's card parks: bottom-right of a pair of tables ending at x=1300. */
    private val parkedRight = IntRect(left = 980, top = 520, right = 1300, bottom = 700)

    @Test
    fun `leaving a row to the right, toward the parked card, is a flight`() {
        assertTrue(isHeadingToward(exit = Offset(600f, 320f), velocity = Offset(14f, 2f), card = parkedRight))
    }

    @Test
    fun `moving down to the next row is not a flight, even when the card is below`() {
        assertFalse(isHeadingToward(exit = Offset(600f, 340f), velocity = Offset(1f, 9f), card = parkedRight))
    }

    @Test
    fun `moving away from the card is not a flight, and neither is standing still`() {
        assertFalse(isHeadingToward(exit = Offset(600f, 320f), velocity = Offset(-12f, 1f), card = parkedRight))
        assertFalse(isHeadingToward(exit = Offset(600f, 320f), velocity = Offset.Zero, card = parkedRight))
    }

    @Test
    fun `every placement stays inside the window`() {
        val edgeRow = IntRect(left = 1200, top = 10, right = 1400, bottom = 50)
        val pointer = Offset(190f, 30f)
        val placements = listOf(
            TipPlacement.UnderPointer,
            BelowAnchorAtPointer,
            EndOfAnchor,
            StartOfAnchor,
            TipPlacement.BesideAnchor(TipSide.Above),
            TipPlacement.BesideAnchor(TipSide.Below, TipAlignX.End),
            TipPlacement.FollowsPointer,
            TipPlacement.Parked(IntOffset(1395, 895), TipCorner.BottomEnd),
            TipPlacement.Parked(IntOffset(-40, 20), TipCorner.BottomStart),
        )
        for (placement in placements) {
            val at = tipCardOffset(placement, edgeRow, pointer, card, window, gap)
            assertTrue(at.x >= 0 && at.x + card.width <= window.width, "$placement x=$at")
            assertTrue(at.y >= 0 && at.y + card.height <= window.height, "$placement y=$at")
        }
    }
}
