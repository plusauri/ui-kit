package tutil.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector

// The words and the one glyph the kit's controls cannot know. The product owns i18n and its brand,
// so it provides these once, beside its own strings, and every control under it reads them. Popups
// inherit composition locals, so a hover card opened anywhere below sees the same values.
//
// The defaults are deliberately blank rather than English: a host that forgets to provide them shows
// unnamed controls, which a test notices, instead of English in a French window, which nobody does.

/** What [FadingScrollColumn]'s page-up and page-down cues call themselves, on hover and to a screen reader. */
class ScrollCueWords(val down: String, val up: String)

/** The words for [ScrollCueWords]; see the note at the top of this file. */
val LocalScrollCueWords = staticCompositionLocalOf { ScrollCueWords(down = "", up = "") }

/**
 * What [CopyIconButton] says: [copy] names the button, [copied] names the check it briefly shows after
 * a copy, and [hint] is its hover tip.
 */
class CopyWords(val copy: String, val copied: String, val hint: String)

/** The words for [CopyWords]; see the note at the top of this file. */
val LocalCopyWords = staticCompositionLocalOf { CopyWords(copy = "", copied = "", hint = "") }

/** The glyph [CopyIconButton] draws at rest. The product provides its branded copy plate. */
val LocalCopyGlyph = staticCompositionLocalOf<ImageVector> { Icons.Default.ContentCopy }
