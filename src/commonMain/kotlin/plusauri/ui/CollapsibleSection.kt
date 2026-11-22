package plusauri.ui

import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.foundation.border
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.unit.Dp
import plusauri.ui.CollapsibleChromeColors
import plusauri.ui.CollapsibleHeaderChrome
import plusauri.ui.collapsibleChromeColors
import plusauri.ui.fieldTag

/** Shared defaults for [CollapsibleSection]. */
object CollapsibleSectionDefaults {
    /**
     * Gap between the header bar and the content it reveals.
     *
     * Shared so no call site has to invent its own. Content that touches its own header reads as
     * unowned, and "a bit of space under a header bar" is not a per-screen decision.
     *
     * Only the gap is shared. A call site that additionally shades or insets its body still owns
     * that, because a shaded container is a different thing from the gap under the bar.
     */
    val ContentPadding: PaddingValues = PaddingValues(top = 8.dp)
}

/**
 * Minimum height for an accordion header band: one compact chip plus the row's vertical padding.
 * Every section header is this tall whether or not it carries a chip or a help affordance.
 */
private val CollapsibleHeaderMinHeight: Dp = CompactFilterChipDefaults.Height + 8.dp

/**
 * A header bar that expands/collapses its [content]. Manages its own expanded state.
 *
 * @param header            Text shown in the header bar when collapsed (ignored when
 *                          [headerContent] is non-null).
 * @param expandedHeader    Text shown when expanded; defaults to [header].
 * @param initiallyExpanded Whether the section starts expanded.
 * @param modifier          Applied to the outer [Column].
 * @param leadingIcon       Optional glyph shown before the header text — use it to tie the bar to a
 *                          matching affordance elsewhere (e.g. the same icon as its nav-rail button).
 * @param headerContent     Optional rich header (e.g. multi-span label); when set, replaces the
 *                          plain [header] / [expandedHeader] text. Use [expandedHeaderContent] if
 *                          collapsed and expanded labels should differ.
 * @param expandedHeaderContent Rich header when expanded; defaults to [headerContent].
 * @param content           Content shown when expanded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsibleSection(
    header: String,
    expandedHeader: String = header,
    initiallyExpanded: Boolean = false,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    headerContent: (@Composable () -> Unit)? = null,
    expandedHeaderContent: (@Composable () -> Unit)? = headerContent,
    headerTrailing: @Composable RowScope.() -> Unit = {},
    headerChrome: CollapsibleHeaderChrome = CollapsibleHeaderChrome.Section,
    contentPadding: PaddingValues = CollapsibleSectionDefaults.ContentPadding,
    chevronAtTrailingEdge: Boolean = false,
    /** Test tag on the header toggle — see the stateless overload. */
    headerTag: String? = null,
    content: @Composable () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(initiallyExpanded) }
    CollapsibleSection(
        header = header,
        isExpanded = isExpanded,
        onToggle = { isExpanded = !isExpanded },
        expandedHeader = expandedHeader,
        modifier = modifier,
        headerTag = headerTag,
        contentPadding = contentPadding,
        chevronAtTrailingEdge = chevronAtTrailingEdge,
        leadingIcon = leadingIcon,
        headerContent = headerContent,
        expandedHeaderContent = expandedHeaderContent,
        headerTrailing = headerTrailing,
        headerChrome = headerChrome,
        content = content,
    )
}

/** The `stateDescription` a [CollapsibleSection]'s toggle announces while open. */
const val SectionExpanded = "expanded"
/** The `stateDescription` a [CollapsibleSection]'s toggle announces while closed. */
const val SectionCollapsed = "collapsed"

/**
 * A header bar that expands/collapses its [content]. Expanded state is driven externally.
 *
 * Use this overload when a parent needs to coordinate multiple sections (e.g. accordion).
 *
 * @param header         Text shown in the header bar when collapsed (ignored when [headerContent]
 *                       is non-null).
 * @param isExpanded     Whether the section is currently expanded.
 * @param onToggle       Called when the user taps the header.
 * @param expandedHeader Text shown when expanded; defaults to [header].
 * @param modifier       Applied to the outer [Column].
 * @param leadingIcon    Optional glyph shown before the header text — use it to tie the bar to a
 *                      matching affordance elsewhere (e.g. the same icon as its nav-rail button).
 * @param headerContent  Optional rich header; when set, replaces plain header text.
 * @param expandedHeaderContent Rich header when expanded; defaults to [headerContent].
 * @param headerTrailing Optional controls on the **right** of the bar (e.g. copy-all) that do **not**
 *                       toggle expand — only the title strip and chevron toggle.
 * @param content        Content shown when expanded.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollapsibleSection(
    header: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    expandedHeader: String = header,
    modifier: Modifier = Modifier,
    leadingIcon: (@Composable () -> Unit)? = null,
    headerContent: (@Composable () -> Unit)? = null,
    expandedHeaderContent: (@Composable () -> Unit)? = headerContent,
    headerTrailing: @Composable RowScope.() -> Unit = {},
    /**
     * [CollapsibleHeaderChrome.Section] (default) for top-level bars; [CollapsibleHeaderChrome.Nested]
     * when this section sits inside another collapsible (warm peach fill).
     */
    headerChrome: CollapsibleHeaderChrome = CollapsibleHeaderChrome.Section,
    /** Inset around the revealed [content]. See [CollapsibleSectionDefaults.ContentPadding]. */
    contentPadding: PaddingValues = CollapsibleSectionDefaults.ContentPadding,
    /**
     * Pushes the chevron to the **right end of the title strip**, next to [headerTrailing], instead
     * of letting it sit immediately after the title text.
     *
     * Default false. Pass true for a stack of bars with trailing actions and titles of varying
     * length (`Step 3` above `Step 1 · Review and confirm`), because a chevron that lands in a
     * different place on each row of a stack stops reading as the same control.
     */
    chevronAtTrailingEdge: Boolean = false,
    /**
     * A **statement** bar: paints the header solid in this color with white ink, in place of
     * [headerChrome]. For a section whose color *is* information, such as a category color that
     * a chart elsewhere on screen also draws, not for decoration.
     */
    headerBarColor: Color? = null,
    /** A [fieldTag] tag on the title + chevron toggle, for UI tests that open sections by identity. */
    headerTag: String? = null,
    /** On the title + chevron toggle, ahead of its click — a focus requester or observer sees it. */
    headerModifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(modifier = modifier) {
        // Filled, rounded bar so the header reads as interactive chrome against the page and against
        // expanded content. Top-level bars use soft-sky tokens; nested bars use warm peach so
        // containment is obvious without a second identical sky bar.
        val scheme = MaterialTheme.colorScheme
        val chrome = if (headerBarColor != null) {
            CollapsibleChromeColors(bar = headerBarColor, content = Color.White)
        } else {
            collapsibleChromeColors(scheme, headerChrome)
        }
        val barShape = RoundedCornerShape(8.dp)
        CompositionLocalProvider(LocalContentColor provides chrome.content) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    // Pin the band height rather than letting it be "tallest child + padding".
                    // Without this, whether a section shows a status chip or a help affordance
                    // changes how tall its header is, so a column of accordions steps up and down.
                    // Sized to the compact chip family, which is what those controls are.
                    .defaultMinSize(minHeight = CollapsibleHeaderMinHeight)
                    .clip(barShape)
                    .background(chrome.bar)
                    .chromeBorder(chrome.border, barShape)
                    .padding(vertical = 4.dp, horizontal = 8.dp),
            ) {
                // Title + chevron toggle — trailing actions stay outside this clickable so copy
                // does not expand/collapse the section.
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .then(headerModifier)
                        // Ahead of the clickable, so a jump landing on the tag finds the toggle's
                        // focus below it ([fieldTag]).
                        .then(headerTag?.let { Modifier.fieldTag(it) } ?: Modifier)
                        .clickable(role = Role.Button, onClick = onToggle)
                        // Open/closed is stated on the toggle itself, so a screen reader — or a
                        // UI test opening a section by identity — can tell without clicking.
                        .semantics { stateDescription = if (isExpanded) SectionExpanded else SectionCollapsed }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                ) {
                    leadingIcon?.invoke()
                    val rich = if (isExpanded) expandedHeaderContent else headerContent
                    // fill = true hands the title the whole strip, so the chevron is pushed to its
                    // far end; fill = false lets the title size to its text and the chevron follow
                    // immediately after it.
                    val titleModifier = Modifier.weight(1f, fill = chevronAtTrailingEdge)
                    if (rich != null) {
                        Box(modifier = titleModifier) { rich() }
                    } else {
                        Text(
                            if (isExpanded) expandedHeader else header,
                            style = MaterialTheme.typography.bodyMedium,
                            color = chrome.content,
                            modifier = titleModifier,
                        )
                    }
                    CompositionLocalProvider(
                        LocalMinimumInteractiveComponentSize provides 0.dp,
                        LocalContentColor provides chrome.icon,
                    ) {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isExpanded)
                    }
                }
                headerTrailing()
            }
        }
        if (isExpanded) {
            Box(modifier = Modifier.padding(contentPadding)) { content() }
        }
    }
}

/**
 * 1dp outline in [color] when the chrome asks for one; a no-op for [Color.Unspecified], so the
 * saturated bars pay nothing for the pale ones needing an edge.
 */
fun Modifier.chromeBorder(color: Color, shape: Shape): Modifier =
    if (color.isSpecified) border(1.dp, color, shape) else this

/**
 * A small rounded surface that visually groups a cluster of controls beneath a [CollapsibleSection]
 * bar — e.g. the slider/scrubber cluster on the left of a panel's first row. Its internal padding
 * insets the controls so the group reads as sitting *inside* the panel, aligned with the bar's
 * title text, rather than flush against the panel's left edge. The surfaceContainerLow tone sits a
 * step below the collapsible bar chrome so the cluster reads as recessed, nested content.
 *
 * @param modifier Applied to the outer [Surface] (e.g. a `weight`/width constraint from the caller).
 * @param content  The controls to group; wrap with `fillMaxWidth()` when the group is width-bounded.
 */
@Composable
fun ControlGroup(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        // Minimal pad — a cluster of small inputs on a title row should hug the controls.
        // Horizontal 6 keeps the nest edge clear of the label; vertical 1 avoids a tall empty
        // band once the children themselves are desktop-dense (no 48.dp Material mins).
        Box(Modifier.padding(horizontal = 6.dp, vertical = 1.dp)) {
            content()
        }
    }
}

/**
 * Whitespace between stacked collapsible sections, so adjacent bars read as distinct cards rather
 * than one blended block.
 *
 * The bars are rounded; a hairline divider between them would put a straight line between two
 * rounded corners and still leave them touching. A gap is what actually reads.
 *
 * This is for a plain stack. A stack with drag handles *between* its sections wants a wider gap
 * (8 dp), since there the gap is an affordance as well as a separator.
 */
val CollapsibleStackGap = 4.dp
