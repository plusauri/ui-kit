package plusauri.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import plusauri.ui.ActionKind
import plusauri.ui.ActionPill
import plusauri.ui.CollapseGrabberDivider
import plusauri.ui.CollapseOrientation
import plusauri.ui.CollapsibleDetailDivider
import plusauri.ui.CollapsibleHeaderChrome
import plusauri.ui.CollapsibleSection
import plusauri.ui.ControlGroup
import plusauri.ui.FadingScrollColumn
import plusauri.ui.NumericInput
import plusauri.ui.RegionSeamDivider
import plusauri.ui.RegionShown
import plusauri.ui.ResetToFullButton
import plusauri.ui.SeamReset
import plusauri.ui.SplitSeam
import plusauri.ui.TwoOptionToggle

internal const val CollapsibleSectionBlurb =
    "A header that expands and collapses its content, in three chromes: Section, Nested and Phase. " +
        "ControlGroup is the plain rounded frame that groups a few controls."

internal const val SeamsBlurb =
    "The grabber family. CollapsibleDetailDivider folds a section an item carries. " +
        "CollapseGrabberDivider and RegionSeamDivider fold or resize a region; SplitSeam moves the " +
        "split between two panes; ResetToFullButton puts a region back to its full size."

internal const val ScrollingBlurb =
    "A scrolling column that fades its edges where there is more to see, with an optional chevron " +
        "cue and a thin scrollbar. PageUp and PageDown scroll it when it has focus."

// ── CollapsibleSection ────────────────────────────────────────────────────────

@Composable
fun CollapsibleSectionPage() = PageColumn {
    ExampleCard("Starts collapsed") {
        CollapsibleSection(header = "Advanced options") {
            Text("This content is hidden until the header is clicked.", style = MaterialTheme.typography.bodyMedium)
        }
    }

    ExampleCard("Starts expanded, with a different header when open") {
        CollapsibleSection(header = "Show details", expandedHeader = "Hide details", initiallyExpanded = true) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("• Row 1", style = MaterialTheme.typography.bodyMedium)
                Text("• Row 2", style = MaterialTheme.typography.bodyMedium)
                Text("• Row 3", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    ExampleCard("The three chromes") {
        CollapsibleHeaderChrome.entries.forEach { chrome ->
            CollapsibleSection(header = "${chrome.name} chrome", headerChrome = chrome) {
                Text("Content of a ${chrome.name} section.", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    var count by remember { mutableStateOf(0) }
    ExampleCard("A trailing action in the header, chevron at the end") {
        CollapsibleSection(
            header = "Accounts ($count)",
            chevronAtTrailingEdge = true,
            headerTrailing = { ActionPill("Add", ActionKind.Add, onClick = { count++ }) },
        ) {
            Text("The header keeps its action whether open or closed.", style = MaterialTheme.typography.bodyMedium)
        }
    }

    var on by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf<Double?>(100.0) }
    ExampleCard("ControlGroup") {
        ControlGroup {
            Row(
                Modifier.padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TwoOptionToggle("Off", "On", secondSelected = on, onSelect = { on = it })
                NumericInput(value = amount, onValueChange = { amount = it }, enabled = on, modifier = Modifier.width(110.dp))
            }
        }
    }
}

// ── Seams and grabbers ────────────────────────────────────────────────────────

@Composable
fun SeamsPage() = PageColumn {
    var detailCollapsed by remember { mutableStateOf(true) }
    ExampleCard("CollapsibleDetailDivider (a carried section arrives folded)") {
        Text("A loan, with its details folded behind the divider.", style = MaterialTheme.typography.bodyMedium)
        CollapsibleDetailDivider(
            collapsed = detailCollapsed,
            onToggle = { detailCollapsed = !detailCollapsed },
            hideLabel = "Hide loan details",
            showLabel = "Show loan details",
        )
        if (!detailCollapsed) {
            Text("Rate, term and payment would sit here.", style = MaterialTheme.typography.bodyMedium)
        }
    }

    var hCollapsed by remember { mutableStateOf(false) }
    ExampleCard("CollapseGrabberDivider, horizontal") {
        if (!hCollapsed) Placeholder("Region above", 60.dp)
        CollapseGrabberDivider(
            orientation = CollapseOrientation.Horizontal,
            collapsed = hCollapsed,
            onToggle = { hCollapsed = !hCollapsed },
            collapsedLabel = "Show the region",
            expandedLabel = "Hide the region",
        )
        Placeholder("Content below", 40.dp)
    }

    var vCollapsed by remember { mutableStateOf(false) }
    var sideWidth by remember { mutableStateOf(160.dp) }
    val density = LocalDensity.current
    ExampleCard("CollapseGrabberDivider, vertical, and draggable") {
        Row(Modifier.fillMaxWidth().height(140.dp)) {
            if (!vCollapsed) {
                Box(Modifier.width(sideWidth).fillMaxHeight()) { Placeholder("Side pane", 140.dp) }
            }
            CollapseGrabberDivider(
                orientation = CollapseOrientation.Vertical,
                collapsed = vCollapsed,
                onToggle = { vCollapsed = !vCollapsed },
                collapsedLabel = "Show the side pane",
                expandedLabel = "Hide the side pane",
                onDrag = { dx -> sideWidth = (sideWidth + with(density) { dx.toDp() }).coerceIn(80.dp, 400.dp) },
            )
            Box(Modifier.weight(1f).fillMaxHeight()) { Placeholder("Main pane", 140.dp) }
        }
        Readout("side width: ${sideWidth.value.toInt()} dp")
    }

    var shown by remember { mutableStateOf(RegionShown.Open) }
    var regionHeight by remember { mutableStateOf(100.dp) }
    ExampleCard("RegionSeamDivider: Open, Partial and Closed, with a reset") {
        if (shown != RegionShown.Closed) Placeholder("Region", regionHeight)
        RegionSeamDivider(
            shown = shown,
            onToggle = { shown = if (shown == RegionShown.Closed) RegionShown.Open else RegionShown.Closed },
            hideLabel = "Hide the region",
            showLabel = "Show the region",
            closedName = "Region",
            onDrag = { dy ->
                regionHeight = (regionHeight + with(density) { dy.toDp() }).coerceIn(30.dp, 100.dp)
                shown = if (regionHeight < 100.dp) RegionShown.Partial else RegionShown.Open
            },
            reset = SeamReset("Full height") { regionHeight = 100.dp; shown = RegionShown.Open }
                .takeIf { shown == RegionShown.Partial },
        )
        Hint("Drag the seam up to make the region partial; the reset puts it back.")
        Readout("shown: $shown, height: ${regionHeight.value.toInt()} dp")
    }

    var topHeight by remember { mutableStateOf(80.dp) }
    ExampleCard("SplitSeam between two panes, and ResetToFullButton") {
        Column(Modifier.fillMaxWidth().height(220.dp)) {
            Box(Modifier.fillMaxWidth().height(topHeight)) { Placeholder("Top pane", topHeight) }
            SplitSeam(
                onDrag = { dy -> topHeight = (topHeight + with(density) { dy.toDp() }).coerceIn(30.dp, 170.dp) },
                label = "Move the split",
            )
            Box(Modifier.fillMaxWidth().weight(1f)) { Placeholder("Bottom pane", 220.dp) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ResetToFullButton("Put the split back") { topHeight = 80.dp }
            Readout("top pane: ${topHeight.value.toInt()} dp")
        }
        Hint("Drag the seam, or Tab to its pill and use Up and Down.")
    }
}

@Composable
private fun Placeholder(label: String, height: Dp) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── FadingScrollColumn ────────────────────────────────────────────────────────

@Composable
fun ScrollingPage() = PageColumn {
    ExampleCard("Fades at both ends") {
        ScrollDemo(scrollCue = false, scrollbar = false)
    }
    ExampleCard("With the chevron cue and the scrollbar") {
        ScrollDemo(scrollCue = true, scrollbar = true)
        Hint("The cue's words come from the host (LocalScrollCueWords). The scrollbar's thumb can be dragged.")
    }
}

@Composable
private fun ScrollDemo(scrollCue: Boolean, scrollbar: Boolean) {
    Box(Modifier.fillMaxWidth().height(200.dp)) {
        FadingScrollColumn(
            modifier = Modifier.fillMaxSize(),
            fadeColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            scrollCue = scrollCue,
            scrollbar = scrollbar,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            (1..30).forEach { i ->
                Text("Row $i of 30", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
