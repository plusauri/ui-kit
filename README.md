# ui-kit

General Compose Multiplatform controls (package `plusauri.ui`): numeric and year inputs, multi-button selects, toggles, collapsible sections, compact menus, hover cards and tips, focus rings, labeled fields, the Plusauri theme. Targets JVM desktop, wasmJs, iOS and Android.

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

The kit is published to GitHub Packages as `com.plusauri:ui-kit` (with `ui-kit-jvm`, `ui-kit-wasm-js`, `ui-kit-iosarm64`, `ui-kit-iossimulatorarm64` and `ui-kit-android` beside it), from `https://maven.pkg.github.com/plusauri/ui-kit`. Reading it needs a classic GitHub token with `read:packages`; fine-grained tokens do not work with GitHub Packages' Maven registry.

To release:

1. Set `version` in `build.gradle.kts` to the new version, in the commit you mean to release. Versions are semver; while it is `0.x`, a minor bump may break callers.
2. Tag that commit `v<version>` and push the tag. `.github/workflows/release.yml` checks that the tag matches the build, runs the tests, and publishes every target. A version is published once: a fix is the next version.

`.github/workflows/ci.yml` runs `jvmTest` and `compileKotlinWasmJs` on every push.

## Who uses it

- **Tutilis** names the release it uses in `kotlin/tutil-ui/build.gradle.kts` (`UI_KIT_VERSION`). To work on the kit and Tutilis together, point a Tutilis tree at a kit checkout with `PLUSAURI_UI_KIT_DIR` (or the Gradle property `plusauri.uiKitDir`): the kit then joins Tutilis's build from source (`includeBuild`) in place of the release.
- **Loquilis** is to depend on the release the same way, never on a Tutilis checkout.

The history before this repo existed is the `kotlin/ui-kit` directory of Tutilis, carried over with `git subtree split`.
