@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

import org.gradle.internal.os.OperatingSystem

// The plugins and their versions come from the root build, which already applies them to the kit.
plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

/**
 * The kit's gallery: one page per family of controls, each control live and editable, so the kit can
 * be seen and tried without a host product. It is an app, not a library, and is never published.
 *
 * It depends on the kit and Compose and nothing else, and the kit's boundary rule (no product
 * package, no product string table) holds here too: `UiKitBoundaryTest` scans this module as well.
 *
 * Desktop: `./gradlew :gallery:run`. Browser: `./gradlew :gallery:wasmJsBrowserDevelopmentRun`.
 */
group = "com.plusauri.ui"

kotlin {
    jvm {
        mainRun {
            mainClass.set("plusauri.ui.gallery.MainKt")
        }
    }
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "gallery.js"
            }
        }
        binaries.executable()
    }

    jvmToolchain(17)

    sourceSets {
        commonMain.dependencies {
            implementation(project(":"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
        }

        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }

        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.junit.jupiter:junit-jupiter:5.11.4")
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    // Keeps a test JVM that composes from registering as a foreground macOS app.
    systemProperty("apple.awt.UIElement", "true")
}

if (OperatingSystem.current().isMacOsX) {
    tasks.withType<JavaExec>().configureEach {
        jvmArgs("-Xdock:name=ui-kit Gallery", "-Dapple.awt.application.name=ui-kit Gallery")
        // The last -D wins: undo the daemon's UIElement=true (gradle.properties), which would make
        // the gallery a background agent with no Dock icon. Main.kt says the same before AWT starts.
        jvmArgs("-Dapple.awt.UIElement=false")
    }
}

/** `./gradlew :gallery:run`, the usual name for starting a desktop app. */
tasks.register("run") {
    group = "application"
    description = "Runs the gallery on the desktop."
    dependsOn("jvmRun")
}
