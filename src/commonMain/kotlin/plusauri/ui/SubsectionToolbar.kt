package plusauri.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Shared chrome for **subsection toolbars**: a row of large illustrated toggles that switches a
 * panel between its parts (for example a settings panel's Growth / Taxes / Fees pages). The icons
 * are flat illustrations in soft pastel colors, each drawn on a square rounded badge (a "plate")
 * that fills nearly the whole button. Call sites use [MultiButtonSelect] with `equalSlots = true`
 * and `buttonSize = [ButtonSize]`, and build each [ButtonData.buttonImage] via
 * [subsectionToolbarIcon] so icon pitch, surface size, and art fill stay consistent.
 */
object SubsectionToolbar {
    /** Clickable [ToggleImageButton] surface (and selection indicator width). */
    val ButtonSize: Dp = 58.dp

    /** Icon art inside the surface — a slight inset so the plate's rim reads. */
    val IconSize: Dp = 55.dp

    /** Gap between equal-width toolbar slots. */
    val SlotSpacing: Dp = 4.dp
}

/**
 * [ButtonImage] that paints a subsection icon at [SubsectionToolbar.IconSize], filling nearly
 * the full [SubsectionToolbar.ButtonSize] surface. [contentDescription] is also the button's hover
 * tip when its toolbar passes none ([ButtonData.hoverName]). Not itself a composable entrypoint — the
 * [ButtonImage.Content] lambda is composed later by [ToggleImageButton].
 */
fun subsectionToolbarIcon(
    drawable: DrawableResource,
    contentDescription: String,
    iconSize: Dp = SubsectionToolbar.IconSize,
): ButtonImage = ButtonImage.Content(contentDescription) {
    Image(
        painter = painterResource(drawable),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(iconSize),
    )
}

fun subsectionToolbarIcon(
    imageVector: ImageVector,
    contentDescription: String,
    iconSize: Dp = SubsectionToolbar.IconSize,
): ButtonImage = ButtonImage.Content(contentDescription) {
    Image(
        imageVector = imageVector,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(iconSize),
    )
}
