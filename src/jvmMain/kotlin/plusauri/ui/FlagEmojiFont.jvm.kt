package plusauri.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.Font

/** Identity of the bundled flags face. Not a system family name. */
const val FLAG_EMOJI_FONT_IDENTITY = "PlusauriFlags"

/**
 * True on Windows, where Segoe UI Emoji draws a regional-indicator pair as two letters.
 * macOS ligates the same pair with Apple Color Emoji and must not take this path.
 */
fun bundledFlagEmojiFontNeeded(): Boolean =
    System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)

fun flagEmojiFontBytes(): ByteArray {
    val name = "font/NotoColorEmojiFlags.ttf"
    val stream = Thread.currentThread().contextClassLoader?.getResourceAsStream(name)
        ?: FlagEmojiClasspath::class.java.getResourceAsStream("/$name")
        ?: error("Missing classpath font $name")
    return stream.use { it.readBytes() }
}

private object FlagEmojiClasspath

/**
 * On Windows, load the bundled flags subset (🇺🇸 US, 🇬🇧 GB, 🇩🇪 DE, 🇫🇷 FR and 🇪🇸 ES only) and
 * expose it as [LocalFlagEmojiFont].
 *
 * A last-resort fallback cannot win: Segoe UI Emoji already has the two code points,
 * and it has no flag ligature. A [FontFamily] of several faces does not help either —
 * Compose uses that list to pick a weight, then gives Skia only the one face it chose.
 * Call sites paint the flag run with this family directly. Everywhere else the local
 * is null and text is unchanged, which is what keeps macOS on Apple Color Emoji.
 */
@Composable
actual fun ProvideFlagEmojiFont(content: @Composable () -> Unit) {
    if (!bundledFlagEmojiFontNeeded()) {
        content()
        return
    }
    val family = remember {
        FontFamily(Font(FLAG_EMOJI_FONT_IDENTITY, flagEmojiFontBytes()))
    }
    CompositionLocalProvider(LocalFlagEmojiFont provides family, content = content)
}
