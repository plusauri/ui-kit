package plusauri.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.focusable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

// The dialog family, moved from Tutilis's :tutil-ui (CompactButton.kt, DialogButtons.kt, NamedHeader.kt and
// HelpDialogScaffold.kt's ResizableDialogShell) when Loquilis became its second user. Which button goes where:
// a dialog's answer is a
// DialogConfirmButton, a back-out or any other footer choice a DialogDismissButton, and a small action on the
// thing beside it an ActionPill.

/** Defaults for [CompactButton]. Tweak here to resize every compact button at once. */
object CompactButtonDefaults {
    /** Pill height, matched to an M3 `FilterChip` (32.dp) so the button sits inline with chips. */
    val Height: Dp = 32.dp

    /** Icon size that fits the compact height with breathing room. */
    val IconSize: Dp = 18.dp

    /** Snug horizontal content padding (the height is fixed by [Height]). */
    val ContentPadding: PaddingValues = PaddingValues(horizontal = 14.dp)
}

/**
 * A compact, pill-shaped filled button sized to sit inline with `FilterChip`s and other slim controls, about
 * [CompactButtonDefaults.Height] tall, where Material's `Button` keeps a 40.dp minimum that cannot be overridden.
 * Content sits centered in a [Row] in `labelLarge`, so a caller drops in an icon and a `Text`.
 *
 * @param contentColor content (icon + text) color when enabled; the disabled state derives from the theme.
 */
@Composable
fun CompactButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    contentPadding: PaddingValues = CompactButtonDefaults.ContentPadding,
    shape: Shape = CircleShape,
    content: @Composable RowScope.() -> Unit,
) {
    // Match the Material button's disabled treatment so a disabled CompactButton reads the same.
    val container = if (enabled) containerColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val onContainer = if (enabled) contentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = shape,
            color = container,
            contentColor = onContainer,
            modifier = modifier.height(CompactButtonDefaults.Height),
        ) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                Row(
                    modifier = Modifier.padding(contentPadding).fillMaxHeight(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
            }
        }
    }
}

/**
 * The button that answers a dialog's question (**OK**, **Save**, **Create**, **Delete**): the one filled shape in
 * the footer, so the eye lands on it. [destructive] paints it in the error color for an action that loses
 * something the reader cannot get back; the word still carries the meaning, the tint is the second signal.
 */
@Composable
fun DialogConfirmButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    CompactButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerColor = if (destructive) scheme.error else scheme.primary,
        contentColor = if (destructive) scheme.onError else scheme.onPrimary,
    ) { Text(text) }
}

/** The back-out beside a [DialogConfirmButton] (Cancel, Close, Back, Not now): the same pill, in a quieter tone. */
@Composable
fun DialogDismissButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    CompactButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) { Text(text) }
}

/** A dialog footer: its buttons at the end, dismiss-weight ones first and the answer last. */
@Composable
fun DialogFooter(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Gap between a header's rule and the content under it. */
val HeaderContentGap: Dp = 8.dp

/** Gap between the header's ink and its rule. */
private val HeaderRuleGap: Dp = 6.dp

/**
 * The header for anything with a name: the name at the start, its controls at the end, a rule under both, and a
 * small gap before the content. The [title] slot is weighted and measured after [actions], so a long name is what
 * gives way; pass an [OverflowTooltipText] so a hover hands it back.
 */
@Composable
fun NamedHeader(
    modifier: Modifier = Modifier,
    contentGap: Dp = HeaderContentGap,
    verticalAlignment: Alignment.Vertical = Alignment.Bottom,
    actions: @Composable RowScope.() -> Unit = {},
    title: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = HeaderRuleGap),
            verticalAlignment = verticalAlignment,
            horizontalArrangement = Arrangement.End,
        ) {
            Box(Modifier.weight(1f).padding(end = 8.dp)) { title() }
            actions()
        }
        HorizontalDivider()
        if (contentGap > 0.dp) Spacer(Modifier.height(contentGap))
    }
}

/**
 * The close "✕" for a dialog header, sized to the toolbar plates beside it rather than Material's 48.dp target, so
 * a header row's glyphs share one center line. [label] is its hover tip and its spoken name (say the key too:
 * "Close (Esc)").
 */
@Composable
fun DialogCloseButton(onClick: () -> Unit, label: String, modifier: Modifier = Modifier) {
    TipIconButton(tip = label, onClick = onClick, buttonModifier = modifier, placeBelow = true) {
        Icon(imageVector = Icons.Filled.Close, contentDescription = label, modifier = Modifier.size(18.dp))
    }
}

/** The margin between a [ResizableDialog]'s edge and its content, for a body that bleeds a scrollbar into it. */
val ResizableDialogContentPadding: Dp = 24.dp

private val ResizableDialogMinWidth = 320.dp
private val ResizableDialogMaxWidth = 1100.dp
private val ResizableDialogMinHeight = 200.dp
private val ResizableDialogMaxHeight = 1000.dp

/**
 * The modal every resizable dialog shares: a surface, the [NamedHeader] title row with [headerActions] and a close
 * button, and a bottom-right grip ([HelpPopupCornerResize]) that sizes it. It has **no opinion about the body**:
 * [content] is a [ColumnScope], and the caller decides its own scrolling, usually a [FadingScrollColumn] given
 * `Modifier.weight(1f, fill = false)`, then a [DialogFooter].
 *
 * The title truncates to one line and hands the rest back on hover. The dialog takes the keys as it opens, and
 * Escape or a click outside calls [onDismiss].
 *
 * @param defaultHeight Null opens the dialog as tall as its content (up to a maximum), which suits a short
 *   question; the first drag of the grip fixes a height from there. A size resets on each open.
 */
@Composable
fun ResizableDialog(
    title: String,
    onDismiss: () -> Unit,
    closeLabel: String,
    defaultWidth: Dp,
    defaultHeight: Dp? = null,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    headerActions: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    var width by remember { mutableStateOf(defaultWidth) }
    var height by remember { mutableStateOf(defaultHeight) }
    // While the height follows the content, what it measured, so the first drag starts from there.
    var measured by remember { mutableStateOf(0.dp) }
    val fixed = height
    // The dialog takes the keys as it opens, so Escape reaches it; a box inside that asks for them after keeps them.
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = modifier
                .padding(16.dp)
                .width(width)
                .then(
                    if (fixed != null) Modifier.height(fixed)
                    else Modifier.heightIn(max = ResizableDialogMaxHeight).onSizeChanged { measured = with(density) { it.height.toDp() } },
                ),
            shape = MaterialTheme.shapes.large,
            color = containerColor,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
        ) {
            Box(
                modifier = (if (fixed != null) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                    .onPreviewKeyEvent { event ->
                        (event.type == KeyEventType.KeyDown && event.key == Key.Escape).also { if (it) onDismiss() }
                    }
                    .focusRequester(focus)
                    .focusable(),
            ) {
                Column(
                    modifier = (if (fixed != null) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                        // Keep the last line clear of the corner grip.
                        .padding(bottom = HelpCornerResizeSize / 2)
                        .padding(ResizableDialogContentPadding),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    NamedHeader(
                        contentGap = 0.dp,
                        actions = {
                            headerActions()
                            DialogCloseButton(onClick = onDismiss, label = closeLabel)
                        },
                    ) {
                        OverflowTooltipText(text = title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    content()
                }
                HelpPopupCornerResize(
                    modifier = Modifier.align(Alignment.BottomEnd),
                    onDragPx = { dx, dy ->
                        with(density) {
                            width = (width.toPx() + dx).coerceIn(ResizableDialogMinWidth.toPx(), ResizableDialogMaxWidth.toPx()).toDp()
                            height = ((height ?: measured).toPx() + dy).coerceIn(ResizableDialogMinHeight.toPx(), ResizableDialogMaxHeight.toPx()).toDp()
                        }
                    },
                )
            }
        }
    }
}
