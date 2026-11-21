# ui-kit

General Compose Multiplatform controls (package `plusauri.ui`): numeric and year inputs, multi-button selects, toggles, collapsible sections, compact menus, hover cards and tips, focus rings, labeled fields, the Plusauri theme. Targets JVM desktop, wasmJs, iOS and Android.

## Using it

The kit is on Maven Central, so no account or token is needed:

```kotlin
repositories { mavenCentral() }

kotlin {
    sourceSets {
        commonMain.dependencies { implementation("com.plusauri:ui-kit:0.6.0") }
    }
}
```

A few controls need words they cannot know, such as what the copy button calls itself. The host provides them once, in its own language, through the composition locals in `KitWords.kt` (`LocalCopyWords`, `LocalYearInputWords`, `LocalScrollCueWords`). Their defaults are blank on purpose, so a host that forgets them shows unnamed controls rather than English in a French window.

## The rule

The kit depends on Compose and nothing else. A control takes its strings as parameters, or reads a kit word (`KitWords.kt`) that the host provides once; it never reads a product's string table or model. `UiKitBoundaryTest` holds that line.

## Building

```
./gradlew jvmTest compileKotlinWasmJs
```

## The gallery

`gallery/` is a small app with a page for every family of controls, each one live, so the kit can be seen and tried without a host product. Start it with:

```
./gradlew :gallery:run                              # desktop
./gradlew :gallery:wasmJsBrowserDevelopmentRun      # browser
```

It depends on the kit and Compose only, and it provides the kit words (`KitWords.kt`) in English the way a host would. `UiKitBoundaryTest` holds it to the kit's rule, and `GalleryPagesComposeTest` opens every page in light and dark, so a page that hands a control arguments it refuses fails in CI. A new control joins its family's page in `GalleryPage.kt`, or gets a page of its own there. The gallery is never published.

## Releases

The kit is published to **Maven Central** as `com.plusauri:ui-kit` (with `ui-kit-jvm`, `ui-kit-wasm-js`, `ui-kit-iosarm64`, `ui-kit-iossimulatorarm64` and `ui-kit-android` beside it). Reading it needs no account and no token: `mavenCentral()` is enough.

Releases up to 0.6.0 also went to GitHub Packages (`https://maven.pkg.github.com/plusauri/ui-kit`), which needed a token to read; that copy is retired. 0.6.0 is the first version on Central. It was built from `48a30ec`, which carries the same code as the commit tagged `v0.6.0` plus the license files and the publishing setup.

To release:

1. Set `version` in `build.gradle.kts` to the new version, in the commit you mean to release. Versions are semver; while it is `0.x`, a minor bump may break callers.
2. Tag that commit `v<version>` and push the tag. `.github/workflows/release.yml` checks that the tag matches the build, runs the tests, signs every file and uploads to Central.
3. In the [Central Portal](https://central.sonatype.com) under **Deployments**, check the validated deployment and press **Publish**. A version on Central is permanent: it can never be removed or replaced, so a fix is the next version.

Running the Release workflow by hand (**Actions → Release → Run workflow**) is a dry run: it uploads to Central only, so the deployment can be checked in the Portal and then dropped.

Releases are signed with the key `Plusauri Releases` (fingerprint `1D3AD54F1509F3748C778A7937E2D16D15C4D9F8`, on `keyserver.ubuntu.com`).

`.github/workflows/ci.yml` runs `jvmTest` and `compileKotlinWasmJs` on every push.

## Who uses it

- **Tutilis** names the release it uses in `kotlin/tutil-ui/build.gradle.kts` (`UI_KIT_VERSION`). To work on the kit and Tutilis together, point a Tutilis tree at a kit checkout with `PLUSAURI_UI_KIT_DIR` (or the Gradle property `plusauri.uiKitDir`): the kit then joins Tutilis's build from source (`includeBuild`) in place of the release.
- **Loquilis** is to depend on the release the same way, never on a Tutilis checkout.

The history before this repo existed is the `kotlin/ui-kit` directory of Tutilis, carried over with `git subtree split`.

## License

MIT; see [LICENSE](LICENSE). The bundled flag font, `src/jvmMain/resources/font/NotoColorEmojiFlags.ttf`, is a subset of Google's Noto Color Emoji under the SIL Open Font License 1.1; its notice is [OFL.txt](src/jvmMain/resources/font/OFL.txt) beside it.
