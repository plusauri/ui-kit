package tutil.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * A pill that says "hover me for the detail card" — the one explicit place an item's hover card
 * opens from (`info-hover-chips`). Two hosts, one look:
 *
 * - **A table value** — a [DataTableView] row card under [RowTooltipAnchor.ValueChip]: the Balance
 *   Sheet's End value, Cash Flow's Value, Sales / Inv's After. [text] is the cell's own text in its
 *   own style and color, so it reads as the same number as its plain neighbors, and a row with no
 *   card keeps plain text — the pills themselves mark which rows have one.
 * - **An "Info" button** — beside Edit / Delete on a [CollectionEditorComponent] item.
 *
 * **Why an explicit chip.** A hover zone on the name (with its underbar cue) spans the widest thing
 * on the row, so a reader scanning a list opened cards they never asked for — a design partner found it "a bit
 * aggressive". The chip shrinks the zone to itself, and a card only opens when it is
 * reached for. Hosts open the card to the chip's **left** ([InfoChipCardOptions]).
 *
 * **It is one of the pill family** ([ActionPill], `StatusPill`): a faint primary tint on a 6 dp
 * rounded rectangle, led by a [PillSwatch] with an `i` knocked out — the marker is what says "there
 * is something here", as a `+` or `×` does on an action pill. Primary, like the invitation kinds
 * ([ActionKind.Go]), because it offers more and costs nothing. Hover deepens the tint.
 */
@Composable
fun InfoChip(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    kind: InfoChipKind = InfoChipKind.Detail,
    /**
     * Drawn after [text] and never cut: [text] ellipsizes into the room left, this stays whole. For
     * a count the card holds more of — the Money Movements Why pill's `+1`.
     */
    trailing: String? = null,
) {
    val source = remember { MutableInteractionSource() }
    val hovered by source.collectIsHoveredAsState()
    val hue = kind.hue()
    Row(
        modifier = modifier
            .hoverable(source)
            .background(hue.copy(alpha = if (hovered) 0.28f else 0.14f), RoundedCornerShape(6.dp))
            // Vertical inset = a plain table cell's, so a chipped row is no taller than a plain one.
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        PillSwatch(color = hue, glyph = "i")
        Text(
            text = text,
            style = style,
            color = color,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (trailing != null) {
            Text(text = trailing, style = style, color = color, maxLines = 1, softWrap = false)
        }
    }
}

/**
 * What an [InfoChip]'s card holds — and so its hue. The behavior is the same for every kind (hover
 * the pill, the card opens; only the pill opens it); the color says what kind of card to expect.
 *
 * - [Detail] — an **item's** canonical detail card: a Balance Sheet row, a collection item. Primary
 *   blue, as it always was.
 * - [Description] — a **general description** of a control: the paragraph that explains what a
 *   setting does, tucked behind the label that names it instead of standing in the layout. Brand
 *   gold, so it never reads as "this item has a card". Convention:
 *   `planning/design-notes/description-info-chips.md`.
 */
enum class InfoChipKind {
    Detail,
    Description;

    @Composable
    internal fun hue(): Color = when (this) {
        Detail -> MaterialTheme.colorScheme.primary
        Description -> PlusauriGold
    }
}

/** Where a description card opens: below its label, over the control it describes, paragraph-wide. */
private val DescriptionCardOptions: HoverDetailOptions =
    InfoChipCardOptions.copy(placement = TipPlacement.BesideAnchor(TipSide.Below), maxWidth = 420.dp)

/**
 * A control's **label as a description pill**: [label] in an [InfoChipKind.Description] chip, and
 * hovering it opens [description] in the standard copyable card ([CopyableTooltipBody]) under it.
 *
 * Use it where a control would otherwise carry a paragraph of explanation in the layout. The label
 * stays where it was, so nothing moves for a reader who already knows the control; a reader who
 * does not reaches for the pill. Convention: `planning/design-notes/description-info-chips.md`.
 */
@Composable
fun DescriptionInfoChip(
    label: String,
    description: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    HoverDetailPopup(
        detail = { CopyableTooltipBody(title = label, detail = description) },
        modifier = modifier,
        options = DescriptionCardOptions,
    ) {
        InfoChip(text = AnnotatedString(label), style = style, kind = InfoChipKind.Description)
    }
}
