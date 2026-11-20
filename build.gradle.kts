@file:OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)

// Versions match Tutilis's root build, which takes this build through `includeBuild`: one Kotlin
// and one Compose across the composite.
plugins {
    kotlin("multiplatform") version "2.3.20"
    id("com.android.library") version "8.6.1"
    id("org.jetbrains.compose") version "1.11.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20"
    // Maven Central: POM, sources and javadoc jars, signing and the Central Portal upload, for
    // every Kotlin Multiplatform target. It applies `maven-publish` itself. 0.34.0 is the newest
    // that runs on this build's Gradle 8.8 and AGP 8.6 (0.35 needs Gradle 8.13, 0.36 AGP 8.13); it
    // moves when the three repos move Gradle and AGP together.
    id("com.vanniktech.maven.publish") version "0.34.0"
}

/**
 * The release version, written here and nowhere else. A release is a tag `v<version>` on a commit
 * whose build says the same version: `.github/workflows/release.yml` refuses a tag that disagrees,
 * then publishes to Maven Central and GitHub Packages. Raise it in the commit you mean to release.
 */
group = "com.plusauri"
version = "0.6.0"

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
    androidTarget {
        // Android is configured, not shipped, but a host that compiles its own Android target
        // against the kit needs a variant to resolve.
        publishLibraryVariants("release")
    }
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

/**
 * Maven Central, as `com.plusauri:ui-kit`. Central wants a full POM and a signature on every file.
 * The release workflow supplies the Central user token and the signing key from `plusauri/ui-kit`'s
 * secrets as Gradle properties (`mavenCentralUsername` / `mavenCentralPassword`,
 * `signingInMemoryKey` / `signingInMemoryKeyPassword`). Without a key, as in `publishToMavenLocal`,
 * nothing is signed.
 *
 * A version on Central is permanent: it can never be removed or replaced, so a fix is the next
 * version.
 */
mavenPublishing {
    publishToMavenCentral()
    if (providers.gradleProperty("signingInMemoryKey").isPresent) signAllPublications()

    pom {
        name.set("Plusauri ui-kit")
        description.set(
            "General Compose Multiplatform controls: numeric and year inputs, multi-button selects, " +
                "toggles, collapsible sections, hover cards and tips, labeled fields and a theme. " +
                "JVM desktop, wasmJs, iOS and Android.",
        )
        inceptionYear.set("2026")
        url.set("https://github.com/plusauri/ui-kit")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
                distribution.set("repo")
            }
        }
        developers {
            developer {
                id.set("plusauri")
                name.set("Plusauri, LLC")
                url.set("https://www.plusauri.com")
            }
        }
        scm {
            url.set("https://github.com/plusauri/ui-kit")
            connection.set("scm:git:https://github.com/plusauri/ui-kit.git")
            developerConnection.set("scm:git:ssh://git@github.com/plusauri/ui-kit.git")
        }
    }
}

/**
 * GitHub Packages, `plusauri/ui-kit`: still published beside Central while Tutilis and Loquilis
 * move over, then retired. The release workflow publishes with its own `GITHUB_TOKEN`; a hand
 * publish needs a classic token with `write:packages` in `GITHUB_TOKEN` (or the Gradle properties
 * `gpr.user` / `gpr.key`). `publishToMavenLocal` needs neither.
 */
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/plusauri/ui-kit")
            credentials {
                username = providers.gradleProperty("gpr.user").orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
                password = providers.gradleProperty("gpr.key").orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
            }
        }
    }
}
