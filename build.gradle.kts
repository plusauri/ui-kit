@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

plugins {
    kotlin("multiplatform")
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "com.plusauri.tutilis"
version = "0.1.0-SNAPSHOT"

android {
    namespace = "com.plusauri.tutilis.ui.kit"
    compileSdk = 36
}

/**
 * The general controls, without the product: a later product can depend on this module without
 * the forecast engine, the dossier model, or the reference data.
 *
 * It depends on Compose and nothing in this repository. The product owns i18n, so a control here
 * takes its strings as parameters and never reads `AppStrings`. `UiKitBoundaryTest` holds both
 * rules. The track is `planning/todos/extract-ui-kit.md`.
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
}
