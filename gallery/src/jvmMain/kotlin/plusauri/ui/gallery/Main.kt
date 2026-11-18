package plusauri.ui.gallery

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/** The gallery on the desktop: `./gradlew :gallery:run`. */
fun main() {
    // The build's `gradle.properties` gives the Gradle JVMs `-Dapple.awt.UIElement=true`, so a test
    // that composes never takes focus, and `jvmRun` passes it on. On a Mac that makes this app a
    // background agent: no Dock icon, not in Cmd-Tab, its window opening behind everything. The
    // gallery is a real app, so it says so before AWT starts.
    System.setProperty("apple.awt.UIElement", "false")
    System.setProperty("apple.awt.application.name", "ui-kit Gallery")
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "ui-kit Gallery",
            state = rememberWindowState(width = 1100.dp, height = 800.dp),
        ) {
            // Started from a terminal, the window would otherwise open behind it.
            LaunchedEffect(Unit) { window.toFront() }
            GalleryApp()
        }
    }
}
