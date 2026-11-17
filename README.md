# ui-kit

General Compose Multiplatform controls (package `plusauri.ui`): numeric and year inputs, multi-button selects, toggles, collapsible sections, compact menus, hover cards and tips, focus rings, labeled fields, the Plusauri theme. Targets JVM desktop, wasmJs, iOS and Android.

## The rule

The kit depends on Compose and nothing else. A control takes its strings as parameters, or reads a kit word (`KitWords.kt`) that the host provides once; it never reads a product's string table or model. `UiKitBoundaryTest` holds that line.

## Building

```
./gradlew jvmTest compileKotlinWasmJs
```

## Who uses it

- **Tutilis** takes it through a Gradle composite build (`includeBuild`). By default Tutilis looks for this checkout beside its own, at `../ui-kit` from the Tutilis repo root (so one kit checkout serves every Tutilis worktree). To point a Tutilis tree at a different kit checkout, set `PLUSAURI_UI_KIT_DIR` or the Gradle property `plusauri.uiKitDir`. Tutilis's CI checks this repo out at the commit pinned in Tutilis's `kotlin/ui-kit.ref`.
- **Loquilis** is to depend on it the same way, never on a Tutilis checkout.

The history before this repo existed is the `kotlin/ui-kit` directory of Tutilis, carried over with `git subtree split`.
