package plusauri.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Brand gold — not the Material `primary` (that's blue); used for accents such as a pinned marker. */
val PlusauriGold = Color(0xFFC89B2C)
val PlusauriGoldLight = Color(0xFFE0B94D)
/**
 * One step *down* the gold ramp from [PlusauriGold]. Brand gold is only ~2.5:1 on the near-white
 * paper surface; this darkens it enough for a fine mark (a thin glyph, a hairline) to clear the
 * 3:1 contrast floor in light theme while still reading as gold.
 */
val PlusauriGoldDeep = Color(0xFFB08823)
val PlusauriGoldContainer = Color(0xFFF4DFAD)
/** Dark-theme companion to [PlusauriGoldContainer] — advanced/caution chrome fill. */
val PlusauriGoldContainerDark = Color(0xFF4E3910)
/**
 * Ink for text sitting **on** [PlusauriBlueLight] in the dark theme — a near-black that keeps the
 * pale blue readable where white would glare.
 */
val PlusauriOnBlueLight = Color(0xFF1A202C)

/** Deep graphite — Material `secondary` in light scheme; useful as a solid "hole" under gold marks. */
val PlusauriGraphite = Color(0xFF34271E)
private val PlusauriGraphiteContainer = Color(0xFFE9DED2)
val PlusauriBlue = Color(0xFF4489E2)
private val PlusauriBlueVivid = Color(0xFF4261F5)
val PlusauriBlueLight = Color(0xFF78A5EB)
private val PlusauriBlueContainer = Color(0xFFE1E7EF)
private val PlusauriBlueContainerDark = Color(0xFF123454)
private val PlusauriPaper = Color(0xFFFCFBFA)
/** Warm cream — [PlusauriGoldSoft] at ~10% over paper. The unselected toggle pill (light). */
private val PlusauriCream = Color(0xFFFDF8EE)
/** Soft brand gold, lighter than [PlusauriGold] — the unselected toggle underbar (light). */
private val PlusauriGoldSoft = Color(0xFFEBBE78)
/** Espresso ink for toggle labels in light theme, selected or not. */
private val PlusauriEspresso = Color(0xFF261910)
/** Dark-theme warm pill under an unselected toggle — the cream idea at low luminance. */
private val PlusauriCreamDark = Color(0xFF221C18)
/** Dark-theme unselected underbar — gold, dimmed so the selected blue still wins. */
private val PlusauriGoldSoftDark = Color(0xFF634E2F)
/** Dark-theme ink inside an unselected toggle pill — parchment, quieter than `onSurface`. */
private val PlusauriOnCreamDark = Color(0xFFD6CEBF)
/** Dark-theme selected pill — a deeper navy than `primaryContainer` so it pops against the warm pills. */
private val PlusauriToggleSelectedDark = Color(0xFF1C2954)
private val PlusauriDark = Color(0xFF141210)

/**
 * Soft sky chrome for collapsible header bars (horizontal bar only).
 *
 * [primaryContainer] (`PlusauriBlueContainer`) is too close to cool content cards (lavender/gray
 * surfaces) to mark section boundaries. This is a step more blue — still pastel, not solid primary —
 * so section bars read as chrome against expanded content.
 */
/*
 * A web page that wants to match these bars has to copy the four values below into its own CSS:
 * there is no token export from Kotlin to CSS, so change both sides together.
 */
private val PlusauriCollapsibleBar = Color(0xFFB4CAE6)
private val PlusauriOnCollapsibleBar = Color(0xFF0F2744)
private val PlusauriCollapsibleBarDark = Color(0xFF2A4568)
private val PlusauriOnCollapsibleBarDark = Color(0xFFD6E4F5)

/** Fill color for collapsible section header bars; theme-aware via [ColorScheme.background]. */
fun collapsibleHeaderBarColor(scheme: ColorScheme): Color =
    if (scheme.background.luminance() < 0.5f) PlusauriCollapsibleBarDark else PlusauriCollapsibleBar

/** Label / icon color on [collapsibleHeaderBarColor]. */
fun collapsibleHeaderOnBarColor(scheme: ColorScheme): Color =
    if (scheme.background.luminance() < 0.5f) PlusauriOnCollapsibleBarDark else PlusauriOnCollapsibleBar

/**
 * Soft peach nest-bar fill (light). In the warm brand band between paper cream and gold container —
 * near `#ffe6cc`–`#ffd9b3` so a collapsible **inside** a soft-sky section bar reads warm/contained
 * rather than as a second blue chrome strip or a cool primaryContainer twin.
 */
private val PlusauriNestedCollapsibleBar = Color(0xFFFFD9B3)
/** Dark ink on [PlusauriNestedCollapsibleBar] — graphite family for readable labels. */
private val PlusauriOnNestedCollapsibleBar = Color(0xFF3D2A18)
/** Dark-theme nest bar — warm brown so it still separates from cool sky section bars. */
private val PlusauriNestedCollapsibleBarDark = Color(0xFF5C4030)
private val PlusauriOnNestedCollapsibleBarDark = Color(0xFFFFE6CC)

/**
 * Header chrome for a collapsible **nested inside** a top-level [collapsibleHeaderBarColor] section.
 * Warm peach (not soft-sky) so nested bars read as contained chrome. **Single source** for the fill:
 * [CollapsibleSection] reads it through [collapsibleChromeColors]; do not hardcode nest peach
 * elsewhere.
 */
fun nestedCollapsibleHeaderBarColor(scheme: ColorScheme): Color =
    if (scheme.background.luminance() < 0.5f) PlusauriNestedCollapsibleBarDark else PlusauriNestedCollapsibleBar

/** Label / icon color on [nestedCollapsibleHeaderBarColor] — pair with the nest bar fill. */
fun nestedCollapsibleHeaderOnBarColor(scheme: ColorScheme): Color =
    if (scheme.background.luminance() < 0.5f) PlusauriOnNestedCollapsibleBarDark else PlusauriOnNestedCollapsibleBar

/*
 * A "where to find it" card: a small note that tells the reader where something is set or shown.
 * Pale butter yellow rather than [PlusauriCream]: cream sits within a hair of the paper page tokens
 * (see the phase chrome below), and this card has to be noticed by a reader who did not know to
 * look for it. A soft-gold border holds its edge; the dark pair is the same idea at low luminance,
 * bordered by the dimmed underbar gold.
 */
private val PlusauriWhereCard = Color(0xFFFFF4CF)
private val PlusauriWhereCardBorder = Color(0xFFEBD08F)
private val PlusauriWhereCardDark = Color(0xFF2B2414)
private val PlusauriWhereCardBorderDark = PlusauriGoldSoftDark

/** Fill for a "where to find it" card. */
fun whereCardColor(scheme: ColorScheme): Color =
    if (scheme.background.luminance() < 0.5f) PlusauriWhereCardDark else PlusauriWhereCard

/** Border for [whereCardColor]. */
fun whereCardBorderColor(scheme: ColorScheme): Color =
    if (scheme.background.luminance() < 0.5f) PlusauriWhereCardBorderDark else PlusauriWhereCardBorder

/*
 * Phase chrome ([CollapsibleHeaderChrome.Phase]) — the accordion bar over one step of a sequence,
 * such as one stage of a multi-stage editor. Cool ice blue-gray rather than the nest peach: such a
 * stack sits inside an editor dialog, not inside a soft-sky section bar, so the "contained, warm"
 * argument for peach does not apply, and three peach bands over a form read louder than the
 * fields they head. The fill is pale enough that it needs a **border** to hold its edge against
 * the page, and the chevron takes a **separate, softer tint** than the title so the mark reads as
 * furniture rather than as a second word. Not soft cream: cream sits within a hair of the
 * paper/cream page tokens and disappears into them.
 */
private val PlusauriPhaseBar = Color(0xFFE8EFF7)
private val PlusauriPhaseBarBorder = Color(0xFFCBDCEF)
private val PlusauriOnPhaseBar = Color(0xFF182230)
private val PlusauriPhaseBarIcon = Color(0xFF4B5E78)
/** Dark counterparts — slate rather than navy so the bar still separates from the sky section bars. */
private val PlusauriPhaseBarDark = Color(0xFF243244)
private val PlusauriPhaseBarBorderDark = Color(0xFF3B4D66)
private val PlusauriOnPhaseBarDark = Color(0xFFDCE6F2)
private val PlusauriPhaseBarIconDark = Color(0xFFA9BBD3)

/**
 * Lemon chiffon for a "worked example" arrival strip (light): a one-line banner at the top of a
 * page the reader arrived at from an example.
 *
 * Warm and very pale — a note, not a warning. It has to catch the eye for one read and then be
 * dismissed, so it sits brighter than [collapsibleHeaderBarColor] chrome but well short of the
 * tertiary/error tints a theme reserves for "something needs your attention". Lemon rather than the
 * nest peach so it does not read as a section header that lost its section, and chiffon-soft rather
 * than a saturated yellow so it never reads as a caution band.
 */
private val PlusauriWorkedExampleStrip = Color(0xFFFFFACD)
/** Dark ink on [PlusauriWorkedExampleStrip] — graphite family, same as the other warm bars. */
private val PlusauriOnWorkedExampleStrip = Color(0xFF3D3418)
/** Dark-theme counterpart: deep olive-amber, warm enough to stay the same idea at low luminance. */
private val PlusauriWorkedExampleStripDark = Color(0xFF474020)
private val PlusauriOnWorkedExampleStripDark = Color(0xFFFAF3CE)

/** Fill for the worked-example arrival strip; theme-aware via [ColorScheme.background]. */
fun workedExampleStripColor(scheme: ColorScheme): Color =
    if (scheme.background.luminance() < 0.5f) PlusauriWorkedExampleStripDark else PlusauriWorkedExampleStrip

/** Label color on [workedExampleStripColor]. */
fun workedExampleStripOnColor(scheme: ColorScheme): Color =
    if (scheme.background.luminance() < 0.5f) PlusauriOnWorkedExampleStripDark else PlusauriOnWorkedExampleStrip

/**
 * Toggle-button chrome (`ToggleImageButton`, so every `MultiButtonSelect` row): the **unselected**
 * slot is warm — cream pill, soft-gold underbar — so the row reads as brand rather than as gray
 * Material, and the **selected** slot keeps the cool blue `primaryContainer` / `primary` pair.
 * Warm-vs-cool is the second channel beside the underbar strength, so selection stays legible even
 * for a reader who cannot separate the hues.
 */
fun toggleDeselectedSurfaceColor(scheme: ColorScheme): Color =
    if (scheme.background.luminance() < 0.5f) PlusauriCreamDark else PlusauriCream

/** Underbar under an unselected toggle; pairs with [toggleDeselectedSurfaceColor]. */
fun toggleDeselectedIndicatorColor(scheme: ColorScheme): Color =
    if (scheme.background.luminance() < 0.5f) PlusauriGoldSoftDark else PlusauriGoldSoft

/** Pill under a selected toggle — `primaryContainer` in light, a deeper navy in dark. */
fun toggleSelectedSurfaceColor(scheme: ColorScheme): Color =
    if (scheme.background.luminance() < 0.5f) PlusauriToggleSelectedDark else scheme.primaryContainer

/** Underbar under a selected toggle — theme `primary` (vivid blue light, light blue dark). */
fun toggleSelectedIndicatorColor(scheme: ColorScheme): Color = scheme.primary

/**
 * Ink inside a toggle pill. Light theme uses one espresso for both states so the pill fill and
 * the underbar carry selection, not the text; dark theme lifts the selected slot to white over
 * the navy pill and keeps the unselected slot on parchment.
 */
fun toggleContentColor(scheme: ColorScheme, selected: Boolean): Color = when {
    scheme.background.luminance() >= 0.5f -> PlusauriEspresso
    selected -> Color.White
    else -> PlusauriOnCreamDark
}

/**
 * The resolved colors of one [CollapsibleHeaderChrome].
 *
 * @param bar     Header fill.
 * @param content Title and default on-bar content color (`LocalContentColor`).
 * @param border  1dp outline around the bar, or [Color.Unspecified] for none. Only the pale
 *                chromes need one; the saturated bars hold their own edge.
 * @param icon    Tint for the chevron. Defaults to [content]; the phase chrome softens it so the
 *                mark sits behind the title rather than competing with it.
 */
data class CollapsibleChromeColors(
    val bar: Color,
    val content: Color,
    val border: Color = Color.Unspecified,
    val icon: Color = content,
)

/**
 * Resolve the colors for [CollapsibleHeaderChrome] from the shared theme tokens.
 * Prefer this over choosing colors ad hoc so nest peach, soft-sky and phase blue-gray stay one-edit.
 */
fun collapsibleChromeColors(
    scheme: ColorScheme,
    chrome: CollapsibleHeaderChrome,
): CollapsibleChromeColors {
    val dark = scheme.background.luminance() < 0.5f
    return when (chrome) {
        CollapsibleHeaderChrome.Section -> CollapsibleChromeColors(
            bar = collapsibleHeaderBarColor(scheme),
            content = collapsibleHeaderOnBarColor(scheme),
        )
        CollapsibleHeaderChrome.Nested -> CollapsibleChromeColors(
            bar = nestedCollapsibleHeaderBarColor(scheme),
            content = nestedCollapsibleHeaderOnBarColor(scheme),
        )
        CollapsibleHeaderChrome.Phase -> CollapsibleChromeColors(
            bar = if (dark) PlusauriPhaseBarDark else PlusauriPhaseBar,
            content = if (dark) PlusauriOnPhaseBarDark else PlusauriOnPhaseBar,
            border = if (dark) PlusauriPhaseBarBorderDark else PlusauriPhaseBarBorder,
            icon = if (dark) PlusauriPhaseBarIconDark else PlusauriPhaseBarIcon,
        )
    }
}

/**
 * Left/right inset fraction for content nested under a top-level collapsible bar. About **1%** of
 * the host width, so nested panels still read as contained composition without ceding much usable
 * width. Prefer this shared constant over one-off horizontal padding so nesting feel stays
 * consistent across panels.
 */
const val NestedCollapsibleHorizontalInsetFraction = 0.03f * 0.35f

val PlusauriLightColorScheme: ColorScheme = lightColorScheme(
    primary = PlusauriBlueVivid,
    onPrimary = Color.White,
    primaryContainer = PlusauriBlueContainer,
    onPrimaryContainer = Color(0xFF13233A),
    secondary = PlusauriGraphite,
    onSecondary = Color.White,
    secondaryContainer = PlusauriGraphiteContainer,
    onSecondaryContainer = Color(0xFF211A15),
    tertiary = PlusauriBlue,
    onTertiary = Color.White,
    tertiaryContainer = PlusauriBlueContainer,
    onTertiaryContainer = Color(0xFF13233A),
    background = PlusauriPaper,
    onBackground = Color(0xFF1A1A1A),
    surface = PlusauriPaper,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFEAE3DA),
    onSurfaceVariant = Color(0xFF51483F),
    outline = Color(0xFF827568),
    outlineVariant = Color(0xFFD4C8BA),
)

val PlusauriDarkColorScheme: ColorScheme = darkColorScheme(
    primary = PlusauriBlueLight,
    onPrimary = Color(0xFF0E2848),
    primaryContainer = PlusauriBlueContainerDark,
    onPrimaryContainer = Color(0xFFE1E7EF),
    secondary = Color(0xFFD8C3AF),
    onSecondary = Color(0xFF2E2118),
    secondaryContainer = Color(0xFF463328),
    onSecondaryContainer = Color(0xFFF4DFD0),
    tertiary = PlusauriBlueLight,
    onTertiary = Color(0xFF0E2848),
    tertiaryContainer = PlusauriBlueContainerDark,
    onTertiaryContainer = Color(0xFFE1E7EF),
    background = PlusauriDark,
    onBackground = Color(0xFFF6F1E8),
    surface = PlusauriDark,
    onSurface = Color(0xFFF6F1E8),
    surfaceVariant = Color(0xFF51483F),
    onSurfaceVariant = Color(0xFFD7C9BA),
    outline = Color(0xFFA09487),
    outlineVariant = Color(0xFF51483F),
)

/**
 * The Plusauri Material theme in one call. Wrap every top-level composition in it (the main window,
 * and any second window such as an error dialog composed on its own), so each is painted with the
 * same palette rather than Material's purple defaults.
 */
@Composable
fun PlusauriTheme(isDarkTheme: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isDarkTheme) PlusauriDarkColorScheme else PlusauriLightColorScheme,
    ) {
        // Inside MaterialTheme so bodyLarge is already the local text style, and our
        // Windows flag family is copied onto it instead of being replaced by the theme.
        ProvideFlagEmojiFont(content)
    }
}
