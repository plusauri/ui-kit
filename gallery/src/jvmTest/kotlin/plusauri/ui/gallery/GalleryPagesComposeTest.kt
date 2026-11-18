package plusauri.ui.gallery

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Every page opens from the page list and composes without throwing, in light and in dark. A page
 * that passes a control arguments it refuses (a MultiButtonSelect with two panels selected under
 * ExactlyOne, say) fails here rather than in front of someone trying the kit.
 */
@OptIn(ExperimentalTestApi::class)
class GalleryPagesComposeTest {

    @Test
    fun `page names are unique`() {
        val names = galleryPages.map { it.name }
        assertEquals(names, names.distinct())
    }

    @Test
    fun `every page opens in light and in dark`() = runComposeUiTest {
        setContent { GalleryApp() }
        listOf("Light", "Dark").forEach { theme ->
            onAllNodesWithText(theme).onFirst().performScrollTo().performClick()
            galleryPages.forEach { page ->
                // The page list's entry comes first; the page's own heading repeats the name once open.
                onAllNodesWithText(page.name).onFirst().performScrollTo().performClick()
                waitForIdle()
                onAllNodesWithText(page.blurb).onFirst().assertIsDisplayed()
            }
        }
    }
}
