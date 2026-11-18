package plusauri.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import plusauri.ui.ButtonData
import plusauri.ui.ButtonImage
import plusauri.ui.CompactDropdownMenu
import plusauri.ui.CompactDropdownMenuItem
import plusauri.ui.CompactFilterChip
import plusauri.ui.CompactMenuBelowOffset
import plusauri.ui.EnumSelect
import plusauri.ui.MbsGroupingConstraint
import plusauri.ui.MultiButtonSelect
import plusauri.ui.SelectionMark
import plusauri.ui.ToggleButtonWidth
import plusauri.ui.ToggleImageButton
import plusauri.ui.ToggleState
import plusauri.ui.TwoOptionToggle
import plusauri.ui.ViewSide

internal const val TwoOptionToggleBlurb =
    "A two-option choice as one control: a MultiButtonSelect at the compact toolbar size, with a " +
        "check mark on the option that is on, so color is not the only cue."

internal const val MultiButtonSelectBlurb =
    "A toolbar of ToggleImageButtons that show and hide content panels. Exactly one, one or none, " +
        "or any combination, with the toolbar on any of the four sides."

internal const val ToggleImageButtonBlurb =
    "A button with an image or a character and a bar that shows its state. A click toggles " +
        "between Selected and Deselected."

internal const val EnumSelectBlurb =
    "An outlined field that opens a compact menu of options. Pass a display function for " +
        "translated or friendlier names."

internal const val MenusBlurb =
    "CompactDropdownMenu is the kit's denser menu, with CompactDropdownMenuItem rows. " +
        "CompactFilterChip is a filter chip at the same compact height."

// ── TwoOptionToggle ───────────────────────────────────────────────────────────

@Composable
fun TwoOptionTogglePage() = PageColumn {
    var override by remember { mutableStateOf(false) }
    ExampleCard("System | Override") {
        TwoOptionToggle("System", "Override", secondSelected = override, onSelect = { override = it })
        Readout("secondSelected: $override")
    }

    var percent by remember { mutableStateOf(true) }
    ExampleCard("Amount | % of value, starting on the second option") {
        TwoOptionToggle("Amount", "% of value", secondSelected = percent, onSelect = { percent = it })
        Readout("secondSelected: $percent")
    }

    var changes by remember { mutableStateOf(0) }
    var side by remember { mutableStateOf(false) }
    ExampleCard("onSelect is called only on a change") {
        TwoOptionToggle(
            "Pay off by",
            "Pay amount",
            secondSelected = side,
            onSelect = { side = it; changes++ },
        )
        Hint("Click the option that is already on: the count does not move.")
        Readout("changes: $changes")
    }
}

// ── MultiButtonSelect ─────────────────────────────────────────────────────────

@Composable
fun MultiButtonSelectPage() = PageColumn {
    ExampleCard("ExactlyOne, toolbar on top (one panel is always shown)") {
        MultiButtonSelect(
            buttonSelections = listOf(
                iconButton(Icons.Filled.Timeline, "Forecast") to ToggleState.Selected,
                iconButton(Icons.Filled.AccountBalance, "Accounts") to ToggleState.Deselected,
                iconButton(Icons.Filled.FamilyRestroom, "Family") to ToggleState.Deselected,
            ),
            contentMaker = { i -> PanelPlaceholder(listOf("Forecast panel", "Accounts panel", "Family panel")[i]) },
            buttonBarSide = ViewSide.Top,
            groupingConstraint = MbsGroupingConstraint.ExactlyOne,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    ExampleCard("OneOrNone, toolbar on the left (click again to hide)") {
        MultiButtonSelect(
            buttonSelections = listOf(
                iconButton(Icons.Filled.Payments, "Cash") to ToggleState.Deselected,
                iconButton(Icons.Filled.Edit, "Edit") to ToggleState.Deselected,
                iconButton(Icons.Filled.Delete, "Remove") to ToggleState.Deselected,
            ),
            contentMaker = { i -> PanelPlaceholder(listOf("Cash panel", "Edit panel", "Remove panel")[i]) },
            buttonBarSide = ViewSide.Left,
            groupingConstraint = MbsGroupingConstraint.OneOrNone,
        )
    }

    ExampleCard("NoConstraint, toolbar on the bottom (any combination)") {
        MultiButtonSelect(
            buttonSelections = listOf(
                iconButton(Icons.Filled.Timeline, "Forecast") to ToggleState.Deselected,
                iconButton(Icons.Filled.Sync, "Reconcile") to ToggleState.Selected,
                iconButton(Icons.Filled.FamilyRestroom, "Family") to ToggleState.Selected,
            ),
            contentMaker = { i -> PanelPlaceholder(listOf("Forecast panel", "Reconcile panel", "Family panel")[i]) },
            buttonBarSide = ViewSide.Bottom,
            groupingConstraint = MbsGroupingConstraint.NoConstraint,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    ExampleCard("ExactlyOne, toolbar on the right") {
        MultiButtonSelect(
            buttonSelections = listOf(
                iconButton(Icons.Filled.AccountBalance, "Accounts") to ToggleState.Selected,
                iconButton(Icons.Filled.Payments, "Cash") to ToggleState.Deselected,
            ),
            contentMaker = { i -> PanelPlaceholder(listOf("Accounts panel", "Cash panel")[i]) },
            buttonBarSide = ViewSide.Right,
            groupingConstraint = MbsGroupingConstraint.ExactlyOne,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    var mode by remember { mutableStateOf(0) }
    ExampleCard("Toolbar only, with characters and hover names") {
        MultiButtonSelect(
            buttonSelections = listOf("A", "B", "C").mapIndexed { i, s ->
                ButtonData(ButtonImage.Unicode(s), label = "Mode $s") to
                    (if (i == mode) ToggleState.Selected else ToggleState.Deselected)
            },
            groupingConstraint = MbsGroupingConstraint.ExactlyOne,
            selectedIndex = mode,
            onSelectedIndexChange = { mode = it },
            toolbarOnly = true,
            tooltips = listOf("First mode", "Second mode", "Third mode"),
        )
        Readout("selectedIndex: $mode")
    }
}

@Composable
private fun iconButton(icon: ImageVector, label: String): ButtonData =
    ButtonData(
        buttonImage = ButtonImage.Content(label) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
        },
        label = label,
    )

@Composable
private fun PanelPlaceholder(label: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().height(80.dp).padding(8.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ── ToggleImageButton ─────────────────────────────────────────────────────────

@Composable
fun ToggleImageButtonPage() = PageColumn {
    var stateIcon by remember { mutableStateOf(ToggleState.Deselected) }
    ExampleCard("Composable image (a Material icon)") {
        ToggleImageButton(
            buttonData = ButtonData(
                buttonImage = ButtonImage.Content("Folder") {
                    Icon(Icons.Filled.Folder, contentDescription = "Folder", modifier = Modifier.size(32.dp))
                },
                label = "Folder",
            ),
            state = stateIcon,
            onStateChange = { stateIcon = it },
        )
        Readout("state: ${stateIcon.name}")
    }

    var state1 by remember { mutableStateOf(ToggleState.Deselected) }
    ExampleCard("Character, no label") {
        ToggleImageButton(
            buttonData = ButtonData(ButtonImage.Unicode("🔔")),
            state = state1,
            onStateChange = { state1 = it },
        )
        Readout("state: ${state1.name}")
    }

    var state2 by remember { mutableStateOf(ToggleState.Selected) }
    ExampleCard("Character with a label, starts selected") {
        ToggleImageButton(
            buttonData = ButtonData(ButtonImage.Unicode("⭐"), label = "Favorite"),
            state = state2,
            onStateChange = { state2 = it },
        )
        Readout("state: ${state2.name}")
    }

    var state3 by remember { mutableStateOf(ToggleState.Deselected) }
    ExampleCard("ContentPair: a different picture for each state") {
        ToggleImageButton(
            buttonData = ButtonData(
                buttonImage = ButtonImage.ContentPair(
                    selected = { Text("✅", style = MaterialTheme.typography.headlineMedium) },
                    deselected = { Text("⬜", style = MaterialTheme.typography.headlineMedium) },
                ),
                label = "Enable",
            ),
            state = state3,
            onStateChange = { state3 = it },
        )
        Readout("state: ${state3.name}")
    }

    var state4 by remember { mutableStateOf(ToggleState.Deselected) }
    ExampleCard("SelectionMark.CheckBox (needs ToggleButtonWidth.Fit)") {
        ToggleImageButton(
            buttonData = ButtonData(
                ButtonImage.Content("Check") { Icon(Icons.Filled.Check, contentDescription = null) },
                label = "Include",
            ),
            state = state4,
            onStateChange = { state4 = it },
            selectionMark = SelectionMark.CheckBox,
            widthMode = ToggleButtonWidth.Fit,
        )
        Readout("state: ${state4.name}")
    }

    val icons = listOf("🏠" to "Home", "💼" to "Work", "🌍" to "Travel")
    val states = remember { mutableStateListOf(*Array(icons.size) { ToggleState.Deselected }) }
    ExampleCard("A row of independent toggles") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            icons.forEachIndexed { i, (icon, label) ->
                ToggleImageButton(
                    buttonData = ButtonData(ButtonImage.Unicode(icon), label = label),
                    state = states[i],
                    onStateChange = { states[i] = it },
                )
            }
        }
        Readout(icons.mapIndexed { i, (_, label) -> "$label=${states[i].name.take(3)}" }.joinToString("  "))
    }

    ExampleCard("Disabled") {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ToggleImageButton(
                buttonData = ButtonData(ButtonImage.Unicode("🔒"), label = "Off"),
                state = ToggleState.Deselected,
                onStateChange = {},
                enabled = false,
            )
            ToggleImageButton(
                buttonData = ButtonData(ButtonImage.Unicode("🔒"), label = "On"),
                state = ToggleState.Selected,
                onStateChange = {},
                enabled = false,
            )
        }
    }
}

// ── EnumSelect ────────────────────────────────────────────────────────────────

private enum class RiskTolerance { Conservative, Moderate, Aggressive }
private enum class Currency { USD, EUR, GBP, JPY, CAD }

@Composable
fun EnumSelectPage() = PageColumn {
    var risk by remember { mutableStateOf(RiskTolerance.Moderate) }
    ExampleCard("Enum names as labels (toString)") {
        EnumSelect(options = RiskTolerance.entries, value = risk, onValueChange = { risk = it })
        Readout("value: $risk")
    }

    var currency by remember { mutableStateOf(Currency.USD) }
    ExampleCard("A display function, and a shorter name once chosen") {
        EnumSelect(
            options = Currency.entries,
            value = currency,
            onValueChange = { currency = it },
            display = { c ->
                when (c) {
                    Currency.USD -> "US dollar (USD)"
                    Currency.EUR -> "Euro (EUR)"
                    Currency.GBP -> "British pound (GBP)"
                    Currency.JPY -> "Japanese yen (JPY)"
                    Currency.CAD -> "Canadian dollar (CAD)"
                }
            },
            displaySelected = { it.name },
        )
        Readout("value: $currency")
    }

    ExampleCard("Disabled") {
        EnumSelect(
            options = RiskTolerance.entries,
            value = RiskTolerance.Conservative,
            onValueChange = {},
            enabled = false,
        )
    }
}

// ── Menus and filter chips ────────────────────────────────────────────────────

@Composable
fun MenusPage() = PageColumn {
    var open by remember { mutableStateOf(false) }
    var chosen by remember { mutableStateOf("Median") }
    ExampleCard("CompactDropdownMenu below its anchor") {
        Box {
            OutlinedButton(onClick = { open = true }) { Text("Percentile: $chosen") }
            CompactDropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                offset = CompactMenuBelowOffset,
            ) {
                listOf("10th", "25th", "Median", "75th", "90th").forEach { p ->
                    CompactDropdownMenuItem(
                        text = p,
                        selected = p == chosen,
                        onClick = { chosen = p; open = false },
                    )
                }
                CompactDropdownMenuItem(
                    text = "Reset",
                    shortcutHint = "R",
                    onClick = { chosen = "Median"; open = false },
                )
            }
        }
        Readout("chosen: $chosen")
    }

    val filters = listOf("Taxable", "Tax-deferred", "Tax-free", "Cash")
    val on = remember { mutableStateListOf(true, true, false, false) }
    ExampleCard("CompactFilterChip") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEachIndexed { i, name ->
                CompactFilterChip(
                    selected = on[i],
                    onClick = { on[i] = !on[i] },
                    label = { Text(name) },
                    leadingIcon = if (on[i]) {
                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else {
                        null
                    },
                )
            }
            CompactFilterChip(selected = false, onClick = {}, label = { Text("Disabled") }, enabled = false)
        }
        Readout(filters.filterIndexed { i, _ -> on[i] }.joinToString(", ", prefix = "on: "))
    }
}
