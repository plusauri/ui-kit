package plusauri.ui.gallery

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.browser.document

/** The gallery in a browser: `./gradlew :gallery:wasmJsBrowserDevelopmentRun`. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(document.body!!) {
        GalleryApp()
    }
}
