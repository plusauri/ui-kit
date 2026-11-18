package plusauri.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import plusauri.ui.ActionKind
import plusauri.ui.ActionPill
import plusauri.ui.ActionPillDensity
import plusauri.ui.CopyIconButton
import plusauri.ui.PillSwatch
import plusauri.ui.PlusauriGold
import plusauri.ui.TipIconButton
import plusauri.ui.ToolbarPlateIconButton

internal const val ActionPillBlurb =
    "A compact pill for a small action beside a control: Add, Clear, Remove, Reset and the rest. " +
        "The kind sets the color and the glyph, so a Remove is the same red everywhere. " +
        "PillSwatch is the small marker the pills lead with."

internal const val IconButtonsBlurb =
    "Every loose icon button names itself on hover. TipIconButton takes the name as a required tip; " +
        "ToolbarPlateIconButton sets an icon on a raised plate; CopyIconButton copies text and says so."

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun ActionPillPage() = PageColumn {
    var last by remember { mutableStateOf("(none)") }
    ExampleCard("Every kind") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ActionKind.entries.forEach { kind ->
                ActionPill(label = kind.name, kind = kind, onClick = { last = kind.name })
            }
        }
        Readout("last clicked: $last")
    }

    ExampleCard("Thin density, for a dense row") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActionPill("Add", ActionKind.Add, onClick = {}, density = ActionPillDensity.Thin)
            ActionPill("Clear", ActionKind.Clear, onClick = {}, density = ActionPillDensity.Thin)
            ActionPill("Remove", ActionKind.Remove, onClick = {}, density = ActionPillDensity.Thin)
        }
    }

    var on by remember { mutableStateOf(true) }
    ExampleCard("Selected, disabled, glyph only, and a custom hue") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            ActionPill("Toggle", ActionKind.Toggle, onClick = { on = !on }, selected = on)
            ActionPill("Remove", ActionKind.Remove, onClick = {}, enabled = false)
            ActionPill(label = null, kind = ActionKind.Next, onClick = {}, contentDescription = "Next")
            ActionPill("Clear selection", ActionKind.Clear, onClick = {}, hue = MaterialTheme.colorScheme.primary)
        }
        Readout("selected: $on")
    }

    ExampleCard("PillSwatch") {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            PillSwatch(color = MaterialTheme.colorScheme.primary, glyph = "+")
            PillSwatch(color = MaterialTheme.colorScheme.error, glyph = "−")
            PillSwatch(color = PlusauriGold, glyph = "i")
            PillSwatch(color = MaterialTheme.colorScheme.tertiary, glyph = null)
            PillSwatch(color = MaterialTheme.colorScheme.primary, glyph = "▶", size = 16.dp)
        }
        Hint("A filled square with a glyph knocked out of it, or an empty outline when the glyph is null.")
    }
}

@Composable
fun IconButtonsPage() = PageColumn {
    var clicks by remember { mutableStateOf(0) }
    ExampleCard("TipIconButton") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TipIconButton(tip = "Add a row", onClick = { clicks++ }) {
                Icon(Icons.Filled.Add, contentDescription = "Add a row")
            }
            TipIconButton(tip = "Settings", onClick = { clicks++ }) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings")
            }
            TipIconButton(tip = "Delete (disabled)", onClick = {}, enabled = false) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete")
            }
            TipIconButton(tip = "Shown below the button", onClick = { clicks++ }, placeBelow = true) {
                Icon(Icons.Filled.Download, contentDescription = "Download")
            }
        }
        Hint("Hover each button for its name.")
        Readout("clicks: $clicks")
    }

    var plateClicks by remember { mutableStateOf(0) }
    ExampleCard("ToolbarPlateIconButton") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ToolbarPlateIconButton(onClick = { plateClicks++ }) {
                Icon(Icons.Filled.Tune, contentDescription = "Adjust", modifier = Modifier.size(18.dp))
            }
            ToolbarPlateIconButton(onClick = { plateClicks++ }) {
                Icon(Icons.Filled.Settings, contentDescription = "Settings", modifier = Modifier.size(18.dp))
            }
            ToolbarPlateIconButton(onClick = {}, enabled = false) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
            }
        }
        Readout("clicks: $plateClicks")
    }

    var copied by remember { mutableStateOf(0) }
    ExampleCard("CopyIconButton") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("4% of 1,000,000 is 40,000", style = MaterialTheme.typography.bodyMedium)
            CopyIconButton(textToCopy = { "4% of 1,000,000 is 40,000" }, onCopied = { copied++ })
        }
        Hint("Click to copy the sentence. The words on hover come from the host (LocalCopyWords).")
        Readout("copies: $copied")
    }
}
