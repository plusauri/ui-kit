@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

// Versions match Tutilis's root build, which takes this build through `includeBuild`: one Kotlin
// and one Compose across the composite.
plugins {
    kotlin("multiplatform") version "2.3.20"
    id("com.android.library") version "8.6.1"
    id("org.jetbrains.compose") version "1.11.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20"
}

group = "com.plusauri"
version = "0.1.0-SNAPSHOT"

android {
    namespace = "com.plusauri.ui.kit"
    compileSdk = 36
}

/**
 * The general controls, without the product: a later product can depend on this module without
 * the forecast engine, the dossier model, or the reference data.
 *
 * It depends on Compose and nothing else. A host owns i18n, so a control here takes its strings as
 * parameters or kit words (`KitWords.kt`) and never reads a product's string table.
 * `UiKitBoundaryTest` holds both rules.
 */
kotlin {
    jvm()
    androidTarget()
    iosSimulatorArm64()
    iosArm64()
    wasmJs {
        browser()
    }

    jvmToolchain(17)

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
        }

        commonTest.dependencies {
            implementation(kotlin("test"))
        }

        // A control's tests move with it. The pure ones are plain JUnit 5; the ones that drive a
        // control compose it, so they need the Compose UI test harness and a desktop runtime.
        jvmTest.dependencies {
            implementation("org.junit.jupiter:junit-jupiter:5.11.4")
            @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
            implementation(compose.desktop.currentOs)
        }
    }
}

tasks.named<Test>("jvmTest") {
    useJUnitPlatform()
    // Keeps a test JVM that composes from registering as a foreground macOS app.
    systemProperty("apple.awt.UIElement", "true")
}
