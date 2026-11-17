package plusauri.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The pill family's leading marker: a small filled square in [color] with one bold [glyph] knocked
 * out of it in the surface color, or — [glyph] null — the square as an empty outline. Shared by
 * [ActionPill] and [InfoChip] so every pill's key reads the same.
 */
@Composable
fun PillSwatch(
    color: Color,
    glyph: String?,
    size: Dp = 10.dp,
    glyphSize: TextUnit = 9.sp,
) {
    Box(
        modifier = Modifier.size(size)
            .then(
                if (glyph == null) Modifier.border(1.5.dp, color, RoundedCornerShape(3.dp))
                else Modifier.background(color, RoundedCornerShape(3.dp)),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (glyph != null) {
            Text(
                text = glyph,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                fontSize = glyphSize,
                lineHeight = glyphSize,
                color = MaterialTheme.colorScheme.surface,
            )
        }
    }
}
