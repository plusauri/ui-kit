package plusauri.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * **The** small icon button: an [IconButton] that always says what it does on hover.
 *
 * A button that shows only an icon has a name for a screen reader and, until this existed, often
 * nothing for a sighted reader — "what are all these buttons?" (design-partner first impression,
 * 2026-10). Every icon button in the product goes through here, so the hover tip cannot be left
 * off; `TipIconButtonScanTest` fails on a bare `IconButton(` anywhere else.
 *
 * Pass the same string the icon carries as its `contentDescription` unless the hover has more to
 * say: one name, read aloud and shown on hover.
 *
 * @param tip      Hover text. Blank shows nothing (see [HoverTooltip]), but say something.
 * @param modifier On the outer hover anchor — the caller's layout child, so `align`/`padding` work.
 * @param buttonModifier On the button itself, after its size — clip, background, a test tag.
 * @param size     Button square. Non-null drops Material's 48 dp minimum touch target, which would
 *                 otherwise set the height of the toolbar or header it sits in; null keeps Material's
 *                 default button.
 */
@Composable
fun TipIconButton(
    tip: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonModifier: Modifier = Modifier,
    size: Dp? = ToolbarIconButtonSize,
    enabled: Boolean = true,
    placeBelow: Boolean = false,
    content: @Composable () -> Unit,
) {
    HoverTooltip(text = tip, modifier = modifier, placeBelow = placeBelow) {
        if (size == null) {
            IconButton(onClick = onClick, enabled = enabled, modifier = buttonModifier, content = content)
        } else {
            CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                IconButton(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = Modifier.size(size).then(buttonModifier),
                    content = content,
                )
            }
        }
    }
}
