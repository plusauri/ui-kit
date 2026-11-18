package plusauri.ui.gallery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import plusauri.ui.CopyWords
import plusauri.ui.FadingScrollColumn
import plusauri.ui.LocalCopyWords
import plusauri.ui.LocalScrollCueWords
import plusauri.ui.LocalYearInputWords
import plusauri.ui.PlusauriTheme
import plusauri.ui.ScrollCueWords
import plusauri.ui.TwoOptionToggle
import plusauri.ui.YearInputWords
import plusauri.ui.keyboardFocusRing

/**
 * The gallery's shell: the page list on the left, the chosen page on the right, and a light / dark
 * switch. Deliberately plain, and built from the kit where the kit has the piece (the theme, the
 * scrolling column, the two-option toggle), so the shell is one more place the controls are seen.
 *
 * The kit's words ([LocalCopyWords] and its siblings) are provided once here in English, the way a
 * host provides them in its own language.
 */
@Composable
fun GalleryApp(initialDark: Boolean = false) {
    var dark by rememberSaveable { mutableStateOf(initialDark) }
    var selected by rememberSaveable { mutableStateOf(galleryPages.first().name) }
    val page = galleryPages.first { it.name == selected }

    PlusauriTheme(isDarkTheme = dark) {
        CompositionLocalProvider(
            LocalCopyWords provides CopyWords(copy = "Copy", copied = "Copied", hint = "Click to copy"),
            LocalScrollCueWords provides ScrollCueWords(down = "More below", up = "More above"),
            LocalYearInputWords provides YearInputWords(placeholder = "Year"),
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Row(Modifier.fillMaxSize()) {
                    PageList(
                        selected = selected,
                        onSelect = { selected = it },
                        dark = dark,
                        onDarkChange = { dark = it },
                        modifier = Modifier.width(230.dp).fillMaxHeight(),
                    )
                    VerticalDivider()
                    // Keyed on the page so each page starts at its top with its own fresh state.
                    key(page.name) {
                        FadingScrollColumn(
                            modifier = Modifier.fillMaxSize(),
                            fadeColor = MaterialTheme.colorScheme.background,
                            scrollbar = true,
                            verticalArrangement = Arrangement.spacedBy(24.dp),
                        ) {
                            Column(
                                Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(24.dp),
                            ) {
                                PageHeader(page)
                                page.content()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageList(
    selected: String,
    onSelect: (String) -> Unit,
    dark: Boolean,
    onDarkChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    FadingScrollColumn(
        modifier = modifier.background(MaterialTheme.colorScheme.surface),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "ui-kit gallery",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            )
            TwoOptionToggle(
                first = "Light",
                second = "Dark",
                secondSelected = dark,
                onSelect = onDarkChange,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp),
            )
            galleryPages.groupBy { it.group }.forEach { (group, pages) ->
                Text(
                    group,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
                )
                pages.forEach { page ->
                    val isSelected = page.name == selected
                    Text(
                        page.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface,
                            )
                            .keyboardFocusRing()
                            .clickable { onSelect(page.name) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PageHeader(page: GalleryPage) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(page.name, style = MaterialTheme.typography.headlineSmall)
        Text(
            page.blurb,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
