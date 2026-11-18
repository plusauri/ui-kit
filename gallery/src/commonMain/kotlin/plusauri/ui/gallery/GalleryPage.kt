package plusauri.ui.gallery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp

/** One page of the gallery: a family of controls, shown under [name] in the page list's [group]. */
class GalleryPage(
    val name: String,
    val group: String,
    val blurb: String,
    val content: @Composable () -> Unit,
)

/**
 * Every page, in the order the page list shows them. A new kit control joins an existing family's
 * page, or gets a page here, so every control the kit ships can be seen.
 */
val galleryPages: List<GalleryPage> = listOf(
    // ── Inputs ────────────────────────────────────────────────────────────────
    GalleryPage("NumericInput", "Inputs", NumericInputBlurb) { NumericInputPage() },
    GalleryPage("PercentInput", "Inputs", PercentInputBlurb) { PercentInputPage() },
    GalleryPage("YearInput", "Inputs", YearInputBlurb) { YearInputPage() },
    GalleryPage("Labeled fields", "Inputs", LabeledFieldsBlurb) { LabeledFieldsPage() },
    // ── Choices ───────────────────────────────────────────────────────────────
    GalleryPage("TwoOptionToggle", "Choices", TwoOptionToggleBlurb) { TwoOptionTogglePage() },
    GalleryPage("MultiButtonSelect", "Choices", MultiButtonSelectBlurb) { MultiButtonSelectPage() },
    GalleryPage("ToggleImageButton", "Choices", ToggleImageButtonBlurb) { ToggleImageButtonPage() },
    GalleryPage("EnumSelect", "Choices", EnumSelectBlurb) { EnumSelectPage() },
    GalleryPage("Menus and filter chips", "Choices", MenusBlurb) { MenusPage() },
    // ── Buttons ───────────────────────────────────────────────────────────────
    GalleryPage("ActionPill", "Buttons", ActionPillBlurb) { ActionPillPage() },
    GalleryPage("Icon buttons", "Buttons", IconButtonsBlurb) { IconButtonsPage() },
    // ── Hover and chips ───────────────────────────────────────────────────────
    GalleryPage("Info chips", "Hover and chips", InfoChipsBlurb) { InfoChipsPage() },
    GalleryPage("Hover cards", "Hover and chips", HoverCardsBlurb) { HoverCardsPage() },
    GalleryPage("OverflowTooltipText", "Hover and chips", OverflowTextBlurb) { OverflowTextPage() },
    // ── Layout ────────────────────────────────────────────────────────────────
    GalleryPage("CollapsibleSection", "Layout", CollapsibleSectionBlurb) { CollapsibleSectionPage() },
    GalleryPage("Seams and grabbers", "Layout", SeamsBlurb) { SeamsPage() },
    GalleryPage("FadingScrollColumn", "Layout", ScrollingBlurb) { ScrollingPage() },
)

/**
 * The card each example sits in. When [title] is null the title row and divider are left out, for
 * an example whose content already carries its own heading.
 */
@Composable
fun ExampleCard(title: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (title != null) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                HorizontalDivider()
            }
            content()
        }
    }
}

/** The page body: its example cards, stacked. */
@Composable
fun PageColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(24.dp), content = content)
}

/** A monospace line under an example that shows the value the control reports. */
@Composable
fun Readout(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        fontFamily = FontFamily.Monospace,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** A plain sentence under an example that says what to try. */
@Composable
fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
