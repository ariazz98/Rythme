# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug          # Build debug APK
./gradlew assembleRelease        # Build release APK
./gradlew installDebug           # Build and install on connected device
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumented tests
./gradlew lint                   # Lint checks
./gradlew clean build            # Full clean build
```

Config: compile/target SDK 36, min SDK 33, Java 11, Kotlin 2.3.10, AGP 9.0.1. Version catalog at `gradle/libs.versions.toml`.

## Architecture

Single-module app under `app/src/main/java/com/aria/rythme/`:

```
feature/          # One subpackage per screen (home, player, library, search, playlist, songlist, navigationbar)
core/
  mvi/            # Legacy MVI support; reserve for genuinely complex state machines
  music/          # Domain models, Room DB, MediaStore scanner, PlaybackController, MusicRepository
  navigation/     # Per-tab NavigationState and UI-owned Navigator
  extensions/     # Compose extension functions
ui/
  component/      # Shared composables (MiniPlayer, BottomNavigationBar, etc.)
  theme/          # RythmeTheme, colors, typography
di/               # Koin modules
```

### Feature architecture (complexity-driven)

Choose the smallest structure that fits the feature:

- Pure presentation/navigation page: a composable with explicit callbacks; no ViewModel required.
- Ordinary data page: a ViewModel exposing `StateFlow<UiState>` and normal methods.
- Genuine state machine (player, scanner, complex editor): an explicit reducer/state machine is allowed.

`core/mvi/` is legacy support while existing complex features are migrated. New features must not add empty
Intent/Action/Effect types or route simple clicks through a reducer solely for consistency.

### Navigation (Navigation3)

Routes are defined as `@Serializable` data objects/classes in
`feature/navigationbar/domain/model/RythmeRoute.kt`. Navigation uses `navigation3-runtime` — no string routes anywhere.

`NavigationState` manages per-tab back stacks. Navigation belongs to the route/UI boundary and is passed to screens as
callbacks. ViewModels must not retain a Navigator.

**Player is NOT a route.** It renders as `AnimatedVisibility` overlay in `RythmeApp.kt` above the main Scaffold so the bottom nav and back stack remain alive underneath.

### Dependency Injection (Koin 4.1.1)

Koin is used only for objects with real dependencies:
- `playModule` — singletons: Room DB, MusicRepository, PlaybackController, MediaStoreWatcher
- `playerModule` — PlayerViewModel scoped to viewModel

Do not create one module per feature. Prefer consolidated data/playback/viewModel modules. `PlaybackController` wraps
ExoPlayer/MediaSession and is a singleton initialized at app start.

### Data Flow

```
MediaStore / Room DB
       ↓
  MusicRepository (single source of truth, exposes StateFlows/Flows)
       ↓
  ViewModels when needed (expose UI state and operations)
       ↓
  Compose UI (collectAsState)
```

`MediaStoreWatcher` detects file system changes and triggers catalog indexing automatically.

### UI & Glass Effects

Uses `io.github.kyant0:backdrop:1.0.6` for iOS-style frosted-glass effects. The `LayerBackdrop` composable captures a background layer via `GraphicsLayer`; the `drawBackdrop` modifier applies blur/lens effects. Blur requires API 31+, lens refraction requires API 33+. `LocalBackdrop`, `LocalInnerPadding`, and `LocalTopBarState` are CompositionLocals used across the UI hierarchy.

Image loading: Coil 3 with OkHttp backend. Album art colors extracted via Palette.

## Key Conventions

- **Language:** Code comments and some identifiers are in Chinese — this is intentional.
- **Backdrop context parameters:** The backdrop library uses Kotlin experimental context parameters (`-Xcontext-parameters` compiler flag is already configured in `build.gradle.kts`).
- **No XML layouts** — 100% Jetpack Compose with Material3.
- **No Hilt** — Koin is the DI framework.
- **No Retrofit** — Media data comes from MediaStore (local files only).
- **Motion reference:** Significant motion must match an observed Apple Music behavior or an explicitly approved Rythme behavior.
- **No fake controls:** Do not display controls whose behavior is not implemented.

## 协作约定

- **先暴露歧义,再动手。** 请求存在多种合理解释时(排序策略、动画时长、布局断点、交互触发时机等),列出 2–3 个选项连同各自取舍让用户选,不要默默挑一种再写代码。对显而易见的单一解读不用问。
- **把目标翻译成可验证的成功标准。** 修 bug 前先口头描述复现路径和预期行为,再动代码。UI 改动在本项目靠运行设备验证(见前文注意事项),不要用"编译通过"冒充"功能正确"。
