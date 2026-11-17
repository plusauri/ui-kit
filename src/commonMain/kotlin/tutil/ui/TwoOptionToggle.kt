package tutil.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * The dossier editors' **two-option choice row** — System | Override, Amount | % of value,
 * No loan | Loan, Pay off by | Pay amount — as one control.
 *
 * It is [MultiButtonSelect] in its `buttonSelections` form (never Material's segmented buttons —
 * entry-control conventions, Rule 2) at the compact toolbar size, with **checkbox marks**: on a
 * two-option row there is no odd-one-out, so color alone does not say which side is on, and the
 * box does. Five editors carried this exact parameter list by hand, and the first to add the marks
 * left the other four without them — which is the divergence this wrapper exists to end.
 *
 * @param first     Label of the left option.
 * @param second    Label of the right option.
 * @param secondSelected True when the right option is the one on.
 * @param onSelect  Called with `true` when the right option is chosen, `false` for the left. Called
 *                  only on a change, so a caller may seed or clear state in it without guarding.
 */
@Composable
fun TwoOptionToggle(
    first: String,
    second: String,
    secondSelected: Boolean,
    onSelect: (secondSelected: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    /** Per-option [dossierField] tags, first then second — see [dossierOption]. */
    optionTags: List<String?>? = null,
) {
    MultiButtonSelect(
        buttonSelections = listOf(first, second).map { ButtonData(ButtonImage.Unicode(it)) to ToggleState.Deselected },
        optionTags = optionTags,
        selectedIndex = if (secondSelected) 1 else 0,
        onSelectedIndexChange = { idx ->
            val pick = idx == 1
            if (pick != secondSelected) onSelect(pick)
        },
        toolbarOnly = true,
        buttonSize = MultiButtonSelectDefaults.CompactHeight,
        widthMode = ToggleButtonWidth.Fit,
        labelTextStyle = MaterialTheme.typography.labelSmall,
        selectionMark = SelectionMark.CheckBox,
        modifier = modifier,
    )
}
