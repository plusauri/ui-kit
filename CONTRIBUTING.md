# Contributing to the ui-kit

Thank you for looking. Issues and pull requests are both welcome. For a new control, or a change to how an existing one behaves, please open an issue first, so we can agree on the shape before you build it.

## Building and testing

You need JDK 17 or newer. From the repository root:

```
./gradlew jvmTest compileKotlinWasmJs
./gradlew :gallery:run          # every control, live, on the desktop
```

CI runs the first line on every push and pull request. Those two commands need nothing beyond the JDK. The full `./gradlew build` also needs an Android SDK (`ANDROID_HOME`, or `sdk.dir` in `local.properties`), and the iOS targets build only on a Mac with Xcode.

## Rules the code keeps

- **The kit depends on Compose and nothing else.** A control takes its strings as parameters, or reads a kit word (`KitWords.kt`) that the host provides. It never names a host product. `UiKitBoundaryTest` enforces the imports and dependencies, and CI checks that comments do not lean on any one host's screens.
- **A new control appears in the gallery.** It joins its family's page in `gallery/src/commonMain/kotlin/plusauri/ui/gallery/GalleryPage.kt`, or gets a page of its own. `GalleryPagesComposeTest` composes the first view of every page in light and dark mode.
- **A control's tests move with it**, in `src/jvmTest`.
- **American English** in code, comments and docs.

## License

The kit is MIT-licensed (see [LICENSE](LICENSE)). By contributing, you agree that your contribution is licensed the same way.
