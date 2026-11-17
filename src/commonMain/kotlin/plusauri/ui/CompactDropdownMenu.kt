package plusauri.ui

import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import plusauri.ui.LocalFlagEmojiFont
import plusauri.ui.listKey
import plusauri.ui.withFlagEmojiFont

/** Height of a [CompactDropdownMenuItem] row. Toolbar-plate scale, not a touch target. */
val CompactMenuItemHeight: Dp = 28.dp

/** Gap between a trigger and a [CompactMenuPlacement.BelowAnchor] menu. */
val CompactMenuBelowGap: Dp = 2.dp

/**
 * Where a [CompactDropdownMenu] sits relative to its parent Box.
 *
 * - [AnchorOffset]: [CompactDropdownMenu.offset] from the parent's top-start — context menus
 *   under the pointer.
 * - [BelowAnchor]: just under the parent; [CompactDropdownMenu.offset] is an extra nudge
 *   (pass [CompactMenuBelowOffset] to clear the trigger by 2 dp).
 * - [EndAnchor]: just past the parent's end (right in LTR) — nav-rail popovers.
 * - [AboveAnchor]: just over the parent, bottom edge on the trigger's top; [CompactDropdownMenu.offset]
 *   is an extra nudge (pass [CompactMenuAboveOffset]). For triggers on the window's bottom edge — the
 *   Simulator tools bar — where a menu opened below would be clamped back over its own trigger.
 */
enum class CompactMenuPlacement { AnchorOffset, BelowAnchor, EndAnchor, AboveAnchor }

/** [CompactMenuPlacement.BelowAnchor] with the shared 2 dp gap. */
val CompactMenuBelowOffset: DpOffset = DpOffset(0.dp, CompactMenuBelowGap)

/** [CompactMenuPlacement.AboveAnchor] with the shared 2 dp gap. */
val CompactMenuAboveOffset: DpOffset = DpOffset(0.dp, -CompactMenuBelowGap)

/** [CompactMenuPlacement.EndAnchor] with the shared 2 dp gap. */
val CompactMenuEndOffset: DpOffset = DpOffset(CompactMenuBelowGap, 0.dp)

/**
 * A context menu at this product's density.
 *
 * Material's [androidx.compose.material3.DropdownMenu] pads its surface 8 dp top and bottom and
 * gives each item a 48 dp touch-target row. On a menu of one or two short entries that is mostly
 * empty space — the menu reads as a floating card rather than a menu — and neither padding is
 * exposed as a parameter, so the fix cannot live at the call site.
 *
 * This is a plain [Popup] with a [Surface] and a tight [Column], which is all Material's own menu is
 * underneath. What is deliberately kept: dismiss-on-outside-click and focusability (via
 * [PopupProperties]), and an anchor-relative [offset] so a caller can open it under the pointer.
 * What is dropped: the entry animation and the touch-sized rows.
 *
 * **Keyboard.** The menu takes focus when it opens, so it works without a pointer: Down / Up move
 * between rows (Ctrl-N / Ctrl-P and Tab / Shift-Tab too — [listKey]), Enter picks the focused row, Esc closes. [onKeyEvent] sees
 * every other key-down first, for a caller's own keys — the Peek menu's letter per entry.
 *
 * @param offset from the anchor — click position for [CompactMenuPlacement.AnchorOffset], or an
 *   extra nudge for [CompactMenuPlacement.BelowAnchor].
 * @param placement [CompactMenuPlacement.AnchorOffset] (default) or below the parent trigger.
 * @param onKeyEvent a caller's own keys, asked before the built-in row navigation; true = consumed.
 */
@Composable
fun CompactDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    offset: DpOffset = DpOffset.Zero,
    placement: CompactMenuPlacement = CompactMenuPlacement.AnchorOffset,
    onKeyEvent: ((KeyEvent) -> Boolean)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return
    val density = LocalDensity.current
    val positionProvider = remember(offset, placement, density) {
        CompactMenuPositionProvider(offset, placement, density)
    }
    // Cap tall catalogues (50 states, growth types) at a slice of the window so the
    // menu scrolls instead of running off the display. Short lists are unaffected.
    val maxHeight = with(density) {
        (LocalWindowInfo.current.containerSize.height * 0.72f).toDp()
    }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        OpenLists.Registered()
        // The column holds focus while no row does, so the first Down has somewhere to start from;
        // moving "next" from it lands on the first row.
        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current
        val inputModes = LocalInputModeManager.current
        var hasFocus by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { focusRequester.requestFocusOncePlaced { hasFocus } }
        CompactMenuPanel(
            maxHeight = maxHeight,
            modifier = Modifier
                .focusRequester(focusRequester)
                .onFocusChanged { hasFocus = it.hasFocus }
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    // A key in the menu is keyboard use. On desktop a focused row is drawn only in
                    // keyboard mode, which Compose enters only when its own focus keys (Tab) move
                    // focus; the steps below move it themselves. After a click, the first Ctrl-N
                    // or Down moved to a row that showed nothing, until a Tab (owner, 2026-10-14).
                    inputModes.requestInputMode(InputMode.Keyboard)
                    if (onKeyEvent?.invoke(event) == true) return@onPreviewKeyEvent true
                    when (event.listKey) {
                        Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Next)
                        Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Previous)
                        // Explicit rather than left to the Popup: its own Esc handling does not
                        // reach a menu whose focus sits on the column or a row.
                        Key.Escape -> {
                            onDismissRequest()
                            true
                        }
                        else -> false
                    }
                }
                .focusable(),
            content = content,
        )
    }
}

/**
 * Ask for focus until [hasFocus], once each frame for up to [PopupFocusFrames] frames — how a menu
 * that has just opened takes focus. In a live window a new popup can take more than a frame to be
 * measured and placed, and one request in its first frame finds nothing, so focus stays on whatever
 * opened it: the Peek menu, opened by its shortcut, left focus on its button, and no key reached the
 * menu; a Display level, entered by Enter, left it on the row. The test clock places a popup at
 * once, so a test passes either way — this is the fix for what only the live window shows.
 */
suspend fun FocusRequester.requestFocusOncePlaced(hasFocus: () -> Boolean) {
    repeat(PopupFocusFrames) {
        if (hasFocus()) return
        runCatching { requestFocus() }
        withFrameNanos { }
    }
}

/** How many frames [requestFocusOncePlaced] keeps asking. */
private const val PopupFocusFrames = 6

/**
 * The plate a [CompactDropdownMenu] draws its rows on — surface, border, and a column as wide as its
 * widest row — without the popup around it.
 *
 * [CompactDropdownMenu] is this inside a [Popup]. A picture of an open menu (a lesson figure, which
 * cannot paint a popup) draws this directly, so the picture is the menu rather than a likeness of it.
 *
 * @param modifier applied to the row column, ahead of its padding — the popup's focus and keys.
 */
@Composable
fun CompactMenuPanel(
    modifier: Modifier = Modifier,
    maxHeight: Dp = Dp.Unspecified,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)),
    ) {
        // 2 dp, not Material's 8: enough that the first row's hover highlight does not touch the
        // border, and no more. IntrinsicSize.Max so every row is as wide as the widest label
        // rather than each sizing itself. Zero the 48 dp touch floor so grid Surfaces and
        // clickable rows honor CompactMenuItemHeight.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
            Column(
                modifier = modifier
                    .padding(vertical = 2.dp)
                    .width(IntrinsicSize.Max)
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState()),
                content = content,
            )
        }
    }
}

/**
 * One row of a [CompactDropdownMenu]: optional leading glyph, then a label, then an optional
 * [shortcutHint] (the key that picks it) at the trailing edge.
 *
 * Sized to [CompactMenuItemHeight] rather than Material's 48 dp minimum — this is a pointer-driven
 * desktop menu, and a touch target's worth of height around a one-line label is what made the menu
 * look like a card.
 */
@Composable
fun CompactDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    shortcutHint: String? = null,
) {
    val color: Color =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(CompactMenuItemHeight)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        leadingIcon?.invoke()
        Text(
            text = text.withFlagEmojiFont(LocalFlagEmojiFont.current),
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            softWrap = false,
        )
        // The row's key, right-aligned and muted, the way a desktop menu shows its accelerators.
        if (shortcutHint != null) {
            Spacer(Modifier.weight(1f))
            Text(
                text = shortcutHint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
            )
        }
    }
}

private class CompactMenuPositionProvider(
    private val offset: DpOffset,
    private val placement: CompactMenuPlacement,
    private val density: Density,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val dx = with(density) { offset.x.roundToPx() }
        val dy = with(density) { offset.y.roundToPx() }
        val rawX = when (placement) {
            CompactMenuPlacement.EndAnchor -> when (layoutDirection) {
                LayoutDirection.Ltr -> anchorBounds.right + dx
                LayoutDirection.Rtl -> anchorBounds.left - popupContentSize.width - dx
            }
            else -> when (layoutDirection) {
                LayoutDirection.Ltr -> anchorBounds.left + dx
                LayoutDirection.Rtl -> anchorBounds.right - popupContentSize.width - dx
            }
        }
        val rawY = when (placement) {
            CompactMenuPlacement.AnchorOffset -> anchorBounds.top + dy
            CompactMenuPlacement.BelowAnchor -> anchorBounds.bottom + dy
            CompactMenuPlacement.EndAnchor -> anchorBounds.top + dy
            CompactMenuPlacement.AboveAnchor -> anchorBounds.top - popupContentSize.height + dy
        }
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(rawX.coerceIn(0, maxX), rawY.coerceIn(0, maxY))
    }
}
