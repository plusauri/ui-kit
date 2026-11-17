package plusauri.ui

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Hit cell for [FeatureToolbar] plate buttons (copy · close-up · mail · help · …).
 * ~10% under the historical 28.dp Material IconButton so the cluster sits closer to the
 * zoom/Reveal plate ([SliderPlateIconSize]) rather than reading as a second, larger family.
 */
/** Touch cell for FeatureToolbar plate buttons — a few dp larger than [ToolbarPlateIconSize] so rims aren’t tight against the host. */
val ToolbarIconButtonSize = 28.dp

/**
 * Toolbar glyph size for chrome-plate icons in chart / table headers (close-up · filter ·
 * merge · mail · copy · compact help). Shared optical weight for the FeatureToolbar cluster.
 * 23.dp = prior 20.dp × 1.15 so pictograms read at header density (owner review 2026-08-07).
 * Still a hair above [SliderPlateIconSize] (plates need the rim; slider plates sit beside a track).
 */
val ToolbarPlateIconSize = 23.dp

/**
 * Host clip/ripple for FeatureToolbar **plate** buttons.
 *
 * Material [IconButton] defaults to a **circle**, which shears the square gold badge’s corners and
 * makes the rim look thin/beveled. Match the badge’s rounded-rect (~22% of plate) so the full rim
 * stays visible. Use via [ToolbarPlateIconButton] (or pass as `shape=` on [IconButton]).
 */
val ToolbarPlateButtonShape: Shape = RoundedCornerShape(percent = 22)

/**
 * [IconButton] sized for the FeatureToolbar cluster that does **not** circle-clip plate art.
 * Prefer this over bare [IconButton] whenever the child is a branded square plate.
 */
@Composable
fun ToolbarPlateIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        IconButton(
            onClick = onClick,
            modifier = modifier.size(ToolbarIconButtonSize),
            enabled = enabled,
            shape = ToolbarPlateButtonShape,
            content = content,
        )
    }
}
