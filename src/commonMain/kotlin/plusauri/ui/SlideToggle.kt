package plusauri.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The grip ridges' green on an on rail (Tutilis's "favorable" green). */
private val RailGreen = Color(0xFF2E7D32)

/** The grip ridges' red on an off rail (Tutilis's "avoid" red). */
private val RailRed = Color(0xFFD32F2F)

/** Soft on-rail green — [RailGreen] lightened so form rows stay calm. */
private val SoftOnGreen = Color(0xFF4C9A50)

/** Soft off-rail red — [RailRed] lightened to match [SoftOnGreen] presence. */
private val SoftOffRed = Color(0xFFE05A5A)

/**
 * Shared metrics for [SlideToggle]. Tweak here to resize every slide toggle at once — same
 * single-source-of-truth pattern as [CompactFilterChipDefaults].
 *
 * Sized for desktop/web density: roughly half the footprint of Material 3's [androidx.compose.material3.Switch]
 * (~52×32) while still reading as a physical slide switch.
 */
object SlideToggleDefaults {
    /** Outer track width. */
    val Width: Dp = 36.dp

    /** Outer track height — short enough to sit inline with bodyMedium form rows. */
    val Height: Dp = 18.dp

    /** Inset of the rectangular grip (thumb) from the track edge on every side. */
    val ThumbInset: Dp = 2.dp

    /** Grip width as a fraction of track width (the rest is the exposed state-color rail). */
    const val ThumbWidthFraction: Float = 0.42f

    /** Track / grip corner radius — small enough to stay rectangular, not pill-shaped. */
    val CornerRadius: Dp = 3.dp

    /** Thumb slide + track recolor duration. */
    const val AnimationMs: Int = 160

    /**
     * Default red/green hardware-switch palette: off rail is a soft [RailRed], on rail a soft
     * [RailGreen]. Neutral settings still use these as pure on/off LEDs (not "good/bad").
     * Rails are slightly lightened so they sit with the warm Plusauri surfaces without shouting.
     */
    @Composable
    fun colors(
        checkedTrackColor: Color = SoftOnGreen,
        uncheckedTrackColor: Color = SoftOffRed,
        checkedThumbColor: Color = Color.White,
        uncheckedThumbColor: Color = Color.White,
        checkedGripColor: Color = RailGreen.copy(alpha = 0.55f),
        uncheckedGripColor: Color = RailRed.copy(alpha = 0.55f),
        disabledCheckedTrackColor: Color = SoftOnGreen.copy(alpha = 0.42f),
        disabledUncheckedTrackColor: Color = SoftOffRed.copy(alpha = 0.42f),
        disabledThumbColor: Color = Color.White.copy(alpha = 0.70f),
        disabledGripColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
        trackBorderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
    ): SlideToggleColors = SlideToggleColors(
        checkedTrackColor = checkedTrackColor,
        uncheckedTrackColor = uncheckedTrackColor,
        checkedThumbColor = checkedThumbColor,
        uncheckedThumbColor = uncheckedThumbColor,
        checkedGripColor = checkedGripColor,
        uncheckedGripColor = uncheckedGripColor,
        disabledCheckedTrackColor = disabledCheckedTrackColor,
        disabledUncheckedTrackColor = disabledUncheckedTrackColor,
        disabledThumbColor = disabledThumbColor,
        disabledGripColor = disabledGripColor,
        trackBorderColor = trackBorderColor,
    )

    /**
     * Theme-primary palette for settings where red/green would read as "bad/good" rather than
     * plain on/off. Unchecked track is a muted surface; checked track is [ColorScheme.primary].
     */
    @Composable
    fun primaryColors(): SlideToggleColors {
        val scheme = MaterialTheme.colorScheme
        return colors(
            checkedTrackColor = scheme.primary,
            uncheckedTrackColor = scheme.surfaceVariant,
            checkedThumbColor = scheme.onPrimary,
            uncheckedThumbColor = scheme.onSurfaceVariant,
            checkedGripColor = scheme.primary.copy(alpha = 0.45f),
            uncheckedGripColor = scheme.onSurfaceVariant.copy(alpha = 0.45f),
            disabledCheckedTrackColor = scheme.primary.copy(alpha = 0.38f),
            disabledUncheckedTrackColor = scheme.onSurface.copy(alpha = 0.12f),
            disabledThumbColor = scheme.onSurface.copy(alpha = 0.38f),
            disabledGripColor = scheme.onSurface.copy(alpha = 0.28f),
            trackBorderColor = scheme.outline.copy(alpha = 0.45f),
        )
    }
}

/** Color roles for [SlideToggle]. Prefer [SlideToggleDefaults.colors] / [SlideToggleDefaults.primaryColors]. */
@Immutable
data class SlideToggleColors(
    val checkedTrackColor: Color,
    val uncheckedTrackColor: Color,
    val checkedThumbColor: Color,
    val uncheckedThumbColor: Color,
    val checkedGripColor: Color,
    val uncheckedGripColor: Color,
    val disabledCheckedTrackColor: Color,
    val disabledUncheckedTrackColor: Color,
    val disabledThumbColor: Color,
    val disabledGripColor: Color,
    val trackBorderColor: Color,
) {
    @Composable
    internal fun trackColor(enabled: Boolean, checked: Boolean): State<Color> {
        val target = when {
            !enabled && checked -> disabledCheckedTrackColor
            !enabled && !checked -> disabledUncheckedTrackColor
            checked -> checkedTrackColor
            else -> uncheckedTrackColor
        }
        return rememberUpdatedState(target)
    }

    @Composable
    internal fun thumbColor(enabled: Boolean, checked: Boolean): State<Color> {
        val target = when {
            !enabled -> disabledThumbColor
            checked -> checkedThumbColor
            else -> uncheckedThumbColor
        }
        return rememberUpdatedState(target)
    }

    @Composable
    internal fun gripColor(enabled: Boolean, checked: Boolean): State<Color> {
        val target = when {
            !enabled -> disabledGripColor
            checked -> checkedGripColor
            else -> uncheckedGripColor
        }
        return rememberUpdatedState(target)
    }
}

/**
 * A compact rectangular slide switch — physical toggle aesthetic at desktop density.
 *
 * LTR layout (checked = grip slides right, matching Material and every other switch in the app):
 * ```
 * |||.RED....|     off — grip left, red exposed rail on the right
 * |....GREEN.|||   on  — green exposed rail on the left, grip right
 * ```
 * The exposed rail carries the state color; the rectangular grip carries three vertical "grab"
 * ridges. Drop-in API shape for Material [androidx.compose.material3.Switch] call sites.
 *
 * @param checked         Whether the toggle is on.
 * @param onCheckedChange Called with the new value when the user toggles; null makes it non-interactive.
 * @param modifier        Applied to the drawn control (default size comes from [SlideToggleDefaults]).
 * @param enabled         When false the control is non-interactive and dimmed.
 * @param colors          Track / thumb / grip colors; default is red/green hardware-switch LEDs.
 */
@Composable
fun SlideToggle(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SlideToggleColors = SlideToggleDefaults.colors(),
) {
    val position by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = tween(durationMillis = SlideToggleDefaults.AnimationMs),
        label = "slideTogglePosition",
    )
    // Animate track/thumb/grip recolor in lockstep with the slide so the rail doesn't "pop".
    val trackOff = colors.trackColor(enabled, checked = false).value
    val trackOn = colors.trackColor(enabled, checked = true).value
    val thumbOff = colors.thumbColor(enabled, checked = false).value
    val thumbOn = colors.thumbColor(enabled, checked = true).value
    val gripOff = colors.gripColor(enabled, checked = false).value
    val gripOn = colors.gripColor(enabled, checked = true).value
    val trackColor = lerp(trackOff, trackOn, position)
    val thumbColor = lerp(thumbOff, thumbOn, position)
    val gripColor = lerp(gripOff, gripOn, position)
    val borderColor = colors.trackBorderColor

    val toggleableModifier = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            onValueChange = onCheckedChange,
            enabled = enabled,
            role = Role.Switch,
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
        )
    } else {
        Modifier
    }

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Canvas(
            modifier = modifier
                .requiredSize(SlideToggleDefaults.Width, SlideToggleDefaults.Height)
                .then(toggleableModifier),
        ) {
            val w = size.width
            val h = size.height
            val corner = CornerRadius(
                SlideToggleDefaults.CornerRadius.toPx(),
                SlideToggleDefaults.CornerRadius.toPx(),
            )
            val inset = SlideToggleDefaults.ThumbInset.toPx()
            val thumbW = (w * SlideToggleDefaults.ThumbWidthFraction).coerceAtLeast(h - inset * 2f)
            val thumbH = h - inset * 2f
            val travel = (w - inset * 2f - thumbW).coerceAtLeast(0f)
            val thumbX = inset + travel * position
            val thumbY = inset

            // Track — filled with the state color (the "exposed rail").
            drawRoundRect(
                color = trackColor,
                size = Size(w, h),
                cornerRadius = corner,
            )
            drawRoundRect(
                color = borderColor,
                size = Size(w, h),
                cornerRadius = corner,
                style = Stroke(width = 1.dp.toPx()),
            )

            // Rectangular grip (thumb). Clip grip lines to the thumb body.
            val thumbPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = thumbX,
                        top = thumbY,
                        right = thumbX + thumbW,
                        bottom = thumbY + thumbH,
                        cornerRadius = CornerRadius(corner.x * 0.75f, corner.y * 0.75f),
                    ),
                )
            }
            val thumbCorner = CornerRadius(corner.x * 0.75f, corner.y * 0.75f)
            // Soft under-thumb shade so the grip lifts off the colored rail.
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.16f),
                topLeft = Offset(thumbX, thumbY + 0.6.dp.toPx()),
                size = Size(thumbW, thumbH),
                cornerRadius = thumbCorner,
            )
            drawPath(thumbPath, color = thumbColor)
            // Hairline rim keeps the white grip crisp on both red and green rails.
            drawRoundRect(
                color = Color.Black.copy(alpha = 0.10f),
                topLeft = Offset(thumbX, thumbY),
                size = Size(thumbW, thumbH),
                cornerRadius = thumbCorner,
                style = Stroke(width = 1.dp.toPx()),
            )

            clipPath(thumbPath) {
                // Three vertical grip ridges — the part you'd grab on a physical slide switch.
                val lineCount = 3
                val gripInsetX = thumbW * 0.26f
                val gripInsetY = thumbH * 0.20f
                val span = thumbW - gripInsetX * 2f
                val stroke = 1.15.dp.toPx()
                for (i in 0 until lineCount) {
                    val t = if (lineCount == 1) 0.5f else i / (lineCount - 1).toFloat()
                    val x = thumbX + gripInsetX + span * t
                    drawLine(
                        color = gripColor,
                        start = Offset(x, thumbY + gripInsetY),
                        end = Offset(x, thumbY + thumbH - gripInsetY),
                        strokeWidth = stroke,
                    )
                }
            }
        }
    }
}

/**
 * A setting that is on or off: its [label], and an optional [hint] under it, to the left of a [SlideToggle].
 * The whole row is the switch, so a click on the words toggles it as well, and a screen reader hears one
 * switch with the label as its name. When [enabled] is false the row reads as disabled but still shows the
 * last value.
 *
 * @param modifier Applied to the row, which is the toggleable node (a test tag goes here).
 * @param colors   The toggle's colors. Prefer [SlideToggleDefaults.primaryColors] where red and green would
 *                 read as bad and good rather than as off and on.
 */
@Composable
fun LabeledSlideToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    enabled: Boolean = true,
    colors: SlideToggleColors = SlideToggleDefaults.colors(),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                enabled = enabled,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (hint != null) {
                Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // Drawn only: the row carries the switch's click and semantics.
        SlideToggle(checked = checked, onCheckedChange = null, enabled = enabled, colors = colors)
    }
}
