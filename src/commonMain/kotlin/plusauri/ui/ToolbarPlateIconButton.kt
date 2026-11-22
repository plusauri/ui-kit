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

/*
 * A "plate" here is a square, rounded badge with a visible rim that an icon is drawn on, as in the
 * small action clusters (copy · zoom · filter · mail · help) in a chart or table header.
 */

/**
 * Hit cell for a header toolbar's plate buttons (copy · zoom · mail · help · …). A few dp larger
 * than [ToolbarPlateIconSize] so the plates' rims are not tight against the button's edge.
 */
val ToolbarIconButtonSize = 28.dp

/**
 * Glyph size for plate icons in chart and table header toolbars (zoom · filter · merge · mail ·
 * copy · compact help), so the cluster shares one optical weight. Large enough for the pictograms
 * to read at header density while leaving room for the plate's rim inside [ToolbarIconButtonSize].
 */
val ToolbarPlateIconSize = 23.dp

/**
 * Clip and ripple shape for header toolbar **plate** buttons.
 *
 * Material [IconButton] defaults to a **circle**, which shears a square badge’s corners and
 * makes the rim look thin/beveled. Match the badge’s rounded-rect (~22% of plate) so the full rim
 * stays visible. Use via [ToolbarPlateIconButton] (or pass as `shape=` on [IconButton]).
 */
val ToolbarPlateButtonShape: Shape = RoundedCornerShape(percent = 22)

/**
 * [IconButton] sized for a header toolbar's plate cluster that does **not** circle-clip plate art.
 * Prefer this over bare [IconButton] whenever the child is a square plate. It shows no hover tip of
 * its own, so wrap it in [HoverTooltip] to give the icon a visible name.
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
