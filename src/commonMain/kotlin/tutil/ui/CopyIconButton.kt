// `ClipboardManager` / `LocalClipboardManager` are deprecated in favour of `Clipboard` /
// `LocalClipboard`, whose `setClipEntry` is a **suspend** function taking a platform `ClipEntry`.
// Compose Multiplatform 1.11 ships no common way to build a plain-text `ClipEntry`: the desktop
// constructor takes an AWT `Transferable`, so a real migration means an `expect`/`actual`
// `clipEntryOf(text)` per target (jvm, wasmJs, iosArm64, iosSimulatorArm64) plus a coroutine scope
// at every copy button. That is a multiplatform change, not a warning cleanup, so the deprecated —
// and still fully functional — API is kept here deliberately and the noise is silenced at the file
// level. See planning/todos/compose-deprecation-migrations.md.
@file:Suppress("DEPRECATION")

package tutil.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import tutil.ui.i18n.LocalAppStrings

/**
 * Best-effort text clipboard write. Returns `false` when the platform rejects the write
 * (wasm/browser: permission denied / non-secure context / user gesture lost) instead of throwing
 * through the pointer pipeline as an uncaught runtime error.
 */
fun trySetClipboardText(clipboard: ClipboardManager, text: String): Boolean =
    try {
        clipboard.setText(AnnotatedString(text))
        true
    } catch (_: Throwable) {
        false
    }

/**
 * A generic, content-agnostic "Copy" affordance: a compact [IconButton] that writes [textToCopy]
 * to the clipboard and briefly swaps its icon to a check as confirmation. This is the reusable
 * primitive behind the app's "everything is copyable" philosophy (see `doc/philosophies/`) — unlike
 * `TableCopyButton`, it is not coupled to `DataTable`, so any surface (help dialogs, cards, panels)
 * can drop a copy button onto its header row.
 *
 * Styling deliberately mirrors `TableCopyButton`: a [ToolbarIconButtonSize] cell (min-interactive-size
 * relaxed so it hugs the icon), a [ToolbarPlateIconSize] [BrandedTableCopyIcon], and the same 1200ms
 * check-swap feedback, so copy affordances read as one family wherever they appear.
 *
 * @param textToCopy computed lazily on click, so callers can assemble the payload (often Markdown)
 *   from live state without rebuilding it every recomposition.
 * @param contentColor Unused for the idle branded glyph (fixed pastel palette); kept for API
 *   stability for callers that used to tint Material [ContentCopy] on inverse surfaces.
 */
@Composable
fun CopyIconButton(
    textToCopy: () -> String,
    modifier: Modifier = Modifier,
    contentDescription: String = LocalAppStrings.current.table.copy,
    copiedDescription: String = LocalAppStrings.current.table.copied,
    @Suppress("UNUSED_PARAMETER") contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    /** Told after a **successful** copy — for a host whose next step follows it (close a menu). */
    onCopied: () -> Unit = {},
) {
    val clipboard = LocalClipboardManager.current

    // Bump a tick on each **successful** copy; the effect flips showCheck true for a beat, then
    // back — the icon becomes a primary-tinted check while it holds. Keyed on the tick so repeat
    // copies re-trigger. Denied clipboard writes stay silent (no false check).
    var copiedTick by remember { mutableIntStateOf(0) }
    var showCheck by remember { mutableStateOf(false) }
    LaunchedEffect(copiedTick) {
        if (copiedTick == 0) return@LaunchedEffect
        showCheck = true
        delay(1200)
        showCheck = false
    }

    val tip = LocalAppStrings.current.table.copyContentHint
    HoverTooltip(tip, modifier = modifier) {
        ToolbarPlateIconButton(
            onClick = {
                if (trySetClipboardText(clipboard, textToCopy())) {
                    copiedTick++
                    onCopied()
                }
            },
        ) {
            if (showCheck) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = copiedDescription,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                BrandedTableCopyIcon(
                    contentDescription = contentDescription,
                    modifier = Modifier.size(ToolbarPlateIconSize),
                )
            }
        }
    }
}
