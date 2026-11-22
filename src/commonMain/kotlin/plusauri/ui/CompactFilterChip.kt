package plusauri.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.SelectableChipColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import plusauri.ui.MultiButtonSelect
import plusauri.ui.MultiButtonSelectDefaults

/**
 * Shared metrics for compact [FilterChip] toolbars (view toggles, filter chips over a table, etc.).
 *
 * Height and content insets come from [MultiButtonSelectDefaults]' compact values so chip rows and
 * [MultiButtonSelect] toggle rows stay one knob — same single-source-of-truth idea as [CompactButtonDefaults].
 */
object CompactFilterChipDefaults {
    /** Target chip height — [MultiButtonSelectDefaults.CompactHeight] (28.dp). */
    val Height: Dp = MultiButtonSelectDefaults.CompactHeight

    /** Horizontal gap between chips in a row / FlowRow. */
    val HorizontalGap: Dp = 4.dp

    /** Vertical gap when a chip FlowRow wraps. */
    val VerticalGap: Dp = 4.dp

    /**
     * Trailing pad under a chip FlowRow (e.g. a filter row above a chart) — tight so the band
     * does not read as an airy strip under the chips.
     */
    val RowBottomPadding: Dp = 4.dp

    /** Horizontal inset for hand-rolled compact chips (Surface + Row), matching MultiButtonSelect. */
    val ContentHorizontalPadding: Dp = MultiButtonSelectDefaults.CompactHorizontalPadding

    /** Vertical inset for hand-rolled compact chips. */
    val ContentVerticalPadding: Dp = MultiButtonSelectDefaults.CompactVerticalPadding
}

/**
 * A [FilterChip] at desktop/web toolbar density: [CompactFilterChipDefaults.Height], zero forced
 * min interactive size (so Material's ~40.dp min does not win), and `labelMedium` text so labels
 * still fit the shorter pill.
 *
 * Prefer this over a raw [FilterChip] whenever the chip sits in a dense toggle row.
 */
@Composable
fun CompactFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    shape: Shape = FilterChipDefaults.shape,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(),
    border: BorderStroke? = FilterChipDefaults.filterChipBorder(enabled, selected),
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        FilterChip(
            selected = selected,
            onClick = onClick,
            label = {
                ProvideTextStyle(MaterialTheme.typography.labelMedium) {
                    label()
                }
            },
            modifier = modifier
                .height(CompactFilterChipDefaults.Height)
                .defaultMinSize(minHeight = CompactFilterChipDefaults.Height),
            enabled = enabled,
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            shape = shape,
            colors = colors,
            border = border,
        )
    }
}
