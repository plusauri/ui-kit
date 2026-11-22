package plusauri.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Shared body for hover tooltips that should be **copyable and select/pasteable** — title row with a
 * [CopyIconButton], optional multi-line [detail], both inside a [SelectionContainer].
 *
 * One body for every such card (on slider markers, chips, chart points), so they all get the same
 * expanded-help chrome. Prefer this over a bare string tooltip when the hover is meant to teach,
 * not just name.
 *
 * Renders inside [TooltipCard] / [HoverTooltip]; does not provide its own surface.
 *
 * [accent] draws a small color swatch ahead of the title, for a card that names something the
 * reader knows by its color (a chart series, say, whose line and legend share it).
 */
@Composable
fun CopyableTooltipBody(
    title: String,
    detail: String? = null,
    modifier: Modifier = Modifier,
    accent: Color? = null,
) {
    val copyText = listOfNotNull(
        title.takeIf { it.isNotBlank() },
        detail?.takeIf { it.isNotBlank() },
    ).joinToString("\n\n")

    // Text selection works only when the enclosing card does not disable it: pass
    // `selectable = true` to [TooltipCard] (an `interactive` [HoverTooltip] does by default). A
    // [HoverDetailPopup] card disables selection, so there the copy button is the only way out. The
    // copy button is the reliable path either way.
    SelectionContainer {
        Column(
            modifier = modifier
                .widthIn(min = 160.dp)
                .padding(start = 10.dp, top = 6.dp, end = 4.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (accent != null) {
                    Box(
                        Modifier
                            .padding(end = 4.dp)
                            .size(10.dp)
                            .background(accent, RoundedCornerShape(3.dp)),
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = LocalContentColor.current,
                    modifier = Modifier.weight(1f),
                )
                CopyIconButton(
                    textToCopy = { copyText },
                    contentColor = LocalContentColor.current,
                )
            }
            if (!detail.isNullOrBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = LocalContentColor.current.copy(alpha = 0.92f),
                )
            }
        }
    }
}
