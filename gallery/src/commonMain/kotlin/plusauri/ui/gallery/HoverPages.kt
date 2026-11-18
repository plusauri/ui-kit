package plusauri.ui.gallery

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import plusauri.ui.CopyableTooltipBody
import plusauri.ui.DescriptionInfoChip
import plusauri.ui.HoverDetailOptions
import plusauri.ui.HoverDetailPopup
import plusauri.ui.HoverTooltip
import plusauri.ui.InfoChip
import plusauri.ui.InfoChipCardOptions
import plusauri.ui.InfoChipKind
import plusauri.ui.OverflowTooltipText
import plusauri.ui.PlusauriGold
import plusauri.ui.TipPlacement
import plusauri.ui.TooltipCard

internal const val InfoChipsBlurb =
    "InfoChip is a small labeled pill: blue for a detail, gold for a description. " +
        "DescriptionInfoChip turns a control's label into a gold chip whose explanation opens on hover, " +
        "so a paragraph of help does not stand in the layout."

internal const val HoverCardsBlurb =
    "HoverTooltip names or explains what is under the pointer. HoverDetailPopup opens a richer card " +
        "that stays while the pointer is on it. TooltipCard is the card both draw; CopyableTooltipBody " +
        "is a title, a detail and a copy button to put in one."

internal const val OverflowTextBlurb =
    "Text that ellipsizes when it does not fit, and shows the whole of itself on hover only when " +
        "something was cut."

@Composable
fun InfoChipsPage() = PageColumn {
    ExampleCard("InfoChip, both kinds") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            InfoChip(AnnotatedString("Detail"))
            InfoChip(AnnotatedString("Description"), kind = InfoChipKind.Description)
            InfoChip(
                buildAnnotatedString {
                    append("Why: ")
                    withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append("required minimum") }
                },
            )
        }
    }

    ExampleCard("InfoChip with a trailing count that is never cut") {
        Box(Modifier.width(170.dp)) {
            InfoChip(AnnotatedString("A reason long enough to be cut short"), trailing = "+2")
        }
    }

    var years by remember { mutableStateOf<Int?>(30) }
    ExampleCard("DescriptionInfoChip as a control's label") {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            DescriptionInfoChip(
                label = "Horizon",
                description = "How many years the plan runs. A longer horizon asks the savings to last " +
                    "longer, so it lowers the chance they do.",
            )
            plusauri.ui.IntegerInput(
                value = years,
                onValueChange = { years = it },
                range = 1..80,
                suffix = " yr",
                modifier = Modifier.width(110.dp),
            )
        }
        Hint("Hover the gold label to read its explanation.")
    }

    ExampleCard("A chip with a hover card (InfoChipCardOptions)") {
        HoverDetailPopup(
            detail = { CopyableTooltipBody("Taxable account", "Gains are taxed each year they are realized.") },
            options = InfoChipCardOptions,
        ) {
            InfoChip(AnnotatedString("Taxable"))
        }
    }
}

@Composable
fun HoverCardsPage() = PageColumn {
    ExampleCard("HoverTooltip with plain text") {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HoverTooltip(text = "A short name, after a moment's hover") {
                HoverTarget("Hover me")
            }
            HoverTooltip(text = "Placed below the target", placeBelow = true) {
                HoverTarget("Below")
            }
            HoverTooltip(text = "Frosted card", frosted = true) {
                HoverTarget("Frosted")
            }
        }
    }

    ExampleCard("HoverTooltip with a rich, interactive card") {
        HoverTooltip(
            interactive = true,
            maxWidth = 320.dp,
            tooltip = {
                CopyableTooltipBody(
                    title = "Real return",
                    detail = "The return after inflation. Move onto this card to select its text or " +
                        "copy it with the button.",
                    accent = PlusauriGold,
                )
            },
        ) {
            HoverTarget("Hover, then move onto the card")
        }
    }

    var clicks by remember { mutableStateOf(0) }
    ExampleCard("HoverDetailPopup, under the pointer and beside the anchor") {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HoverDetailPopup(
                detail = { CopyableTooltipBody("Under the pointer", "The default placement.") },
                onClick = { clicks++ },
            ) {
                HoverTarget("Under the pointer")
            }
            HoverDetailPopup(
                detail = { CopyableTooltipBody("Beside the anchor", "Placed at the end of what is hovered.") },
                options = HoverDetailOptions(placement = TipPlacement.BesideAnchor(plusauri.ui.TipSide.End)),
            ) {
                HoverTarget("Beside the anchor")
            }
        }
        Hint("A click on the first card runs the anchor's own action.")
        Readout("clicks: $clicks")
    }

    ExampleCard("TooltipCard on its own") {
        TooltipCard(maxWidth = 300.dp) {
            CopyableTooltipBody(
                title = "A card, without the hover",
                detail = "The surface every hover card is drawn on, for a host that places it itself.",
            )
        }
    }
}

@Composable
private fun HoverTarget(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
    )
}

@Composable
fun OverflowTextPage() = PageColumn {
    val long = "Required minimum distributions begin in the year the owner turns seventy-three"
    ExampleCard("Cut short: hover for the whole text") {
        Box(Modifier.width(220.dp)) {
            OverflowTooltipText(long, style = MaterialTheme.typography.bodyMedium)
        }
    }
    ExampleCard("Fits: no hover card") {
        OverflowTooltipText("Short enough to fit", style = MaterialTheme.typography.bodyMedium)
    }
    ExampleCard("Two lines before it cuts") {
        Column(Modifier.width(220.dp)) {
            OverflowTooltipText("$long, unless the plan says otherwise.", maxLines = 2, modifier = Modifier.fillMaxWidth())
        }
    }
}
