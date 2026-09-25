# Chickie Runway

A fully offline Android casual game (Kotlin + Jetpack Compose) with two modes — **Farm Quiz** and
**Matching Pairs** — starring Chickie, a cheerful hen dashing down a sunny country road.

- applicationId / namespace: `com.chickierunway.game`
- minSdk 24 · compileSdk 36 · targetSdk 36 · JDK 17 · portrait only

---

## Features

**Farm Quiz** — 3 difficulty levels (Easy ★ / Medium ★★ / Hard ★★★) × 10 questions about farms
and chickens, each with 4 options and exactly one correct answer. One question at a time, progress
bar and `n/10`. Tapping an answer turns the correct option green (✓) and a wrong pick red (✗), then
auto-advances after ~0.75 s. Best score per category is saved. Pause overlay: Continue / Restart /
Menu. Results panel: `Score: X/10`, Continue / Restart / Menu. Empty space under the answers is
filled with the mascots, who react to each answer (cheer on ✓, surprised on ✗).

**Matching Pairs** — 9 levels (pairs / columns / seconds):

| Level | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 |
|---|---|---|---|---|---|---|---|---|---|
| Pairs | 2 | 3 | 4 | 6 | 6 | 8 | 8 | 10 | 12 |
| Columns | 2 | 2 | 2 | 3 | 3 | 4 | 4 | 4 | 4 |
| Seconds | 60 | 70 | 70 | 80 | 80 | 100 | 100 | 110 | 120 |

A deck of `pairs` icons is picked from the pool of 18 road-themed icons, duplicated and shuffled.
The whole board always fits on one screen — card size is computed from the free space for the
level's columns × rows (portrait playing-card shape, logo on the card backs), no scrolling. Flip two cards:
matches stay open, mismatches flip back after a short delay. The countdown pauses while paused
(and when the app goes to the background). Clear all pairs → **Well Done**; time out →
**Out of Time**. Best matched pairs per level are saved. Level 1 is always open; each next level
unlocks when the previous one is cleared.

**Screens** — Main · Menu · Quiz categories · Quiz · Quiz results · Levels (9 bubbles + Game Rules)
· Pairs game · Pairs results · Results summary · Settings (sound, privacy link, rate, reset with
confirmation) · Game Rules.

## Offline only

- The manifest has **no `INTERNET` permission** and no runtime permissions.
- No services, WorkManager, alarms, notifications, or keep-awake.
- No networking, ads, analytics, payments, maps, Firebase, or attribution/tracking SDKs.
- "Privacy Policy" and "Rate the Game" only hand a URL to an external app (browser / Play Store)
  through an `ACTION_VIEW` intent; the game itself never connects anywhere.

## Local storage & DataStore

All progress lives in **DataStore Preferences** under one key (`game_data_json`) that holds a
single JSON string:

```kotlin
@Serializable data class GameData(
    val quizBest: Map<String, Int> = emptyMap(),   // category id -> best score
    val pairsBest: Map<String, Int> = emptyMap(),  // "1".."9"   -> best matched pairs
    val settings: Settings = Settings()
)
@Serializable data class Settings(val soundEnabled: Boolean = true)
```

`GameSerializer` (context-free, `ignoreUnknownKeys = true`, `coerceInputValues = true`) never
throws: empty storage, a missing key, empty/corrupted JSON, wrong shapes or explicit nulls all
decode to defaults, and impossible values are clamped. The repository also catches DataStore I/O
errors on read (emits defaults) and on write (returns `false`).

## Privacy

The game collects nothing. Scores and the sound toggle stay on the device and are excluded from
cloud backup and device transfer (`allowBackup=false`, `data_extraction_rules.xml`). Before
publishing, replace `AppLinks.PRIVACY_POLICY_URL` in `SettingsScreen.kt` (currently a placeholder)
with your hosted policy — Google Play requires one. A policy for this app can simply state that no
personal data is collected or shared.

## Missing integrations (intentionally not included)

Networking (Retrofit/OkHttp/Ktor), WebView, Firebase (Analytics/Crashlytics/Remote Config), ads,
in-app purchases, Play Games, analytics, maps, push notifications, AppsFlyer / OneSignal / any
attribution or tracking SDK, Room, DI frameworks. Crash reports are only available through the
Play Console's built-in Android vitals.

## Visual concept — "Sunny Road Dash"

Hand-drawn cartoon art: a winding country road under a bright sky, Chickie the white hen with a
red comb (4 poses), glossy gold-framed charcoal plates, and 18 road props (cones, lights, signs,
vehicles…).

| Token | HEX | Usage |
|---|---|---|
| Sun Yolk | `#FFC629` | titles (outlined), logo gold, borders, progress fill |
| Sun Yolk Deep | `#F2A900` | chip / progress borders |
| Beak Orange | `#E56F01` | plate frame accents |
| Beak Orange Deep | `#B55400` | shadows |
| Sky Blue | `#4DAEFD` | sky, splash background, timer chip |
| Asphalt | `#3F3C47` | road, panel & card-back gradient top |
| Charcoal | `#2C3039` | plate centres |
| Night Navy | `#171C2F` | panel gradient bottom, title outline, chips |
| Cream | `#FFF8E7` | body text on dark, card faces, question card |
| Grass Green | `#719108` | decoration |
| Comb Red | `#E53935` | accents |
| Correct Green | `#157F45` | correct / cleared (always with ✓ text) |
| Wrong Red | `#C62F2F` | wrong / low time (always with ✗ / "Hurry!") |
| Locked Gray | `#6C7086` | locked levels (with 🔒 + "Locked") |

**Unique layouts**

- **Home ("Road Dash")** — the road in the background art is the stage: road props (cone,
  traffic light, stop sign, barrier) bob on the verges, Chickie sprints and hops down the middle,
  the logo floats in the sky, and **Start** (with a checkered-flag icon) sits at the finish line.
  No stats card, no button stack.
- **Menu** — small logo, live progress chips, four framed road-sign buttons each with a prop
  icon, and the mascots filling the space below.
- **Quiz difficulty** — three cards, each with its own mascot, ★ rating, tagline and best score.
- **Levels** — 9 gold bubbles on three little dashed-line roads in a zig-zag.
- **Pairs** — blurred background, outlined gold "Level n", timer + pairs chips, full-screen board.
- **Results / pause / rules / settings** — Compose panels with a mascot "sitting" on the top
  border; the mascot's pose matches the outcome.

**Typography** — Material 3 type scale on the system font (no bundled fonts), all sizes in `sp`;
titles are gold with a dark outline (drawn twice: stroke + fill) so they read over the art.

**Buttons** — `btn_plate` (long plate) is drawn as **3-slice** (left cap | stretched middle |
right cap) so any width keeps round, undistorted ends; the WHITE bold label (plus an optional
prop icon) is drawn on top in code. Answer feedback fills the plate's inner area green/red while
the gold frame stays. Main/menu buttons use the ornate `btn_menu` at ~70–74% width with
`Modifier.aspectRatio(720/244)` inside scrollable columns.

**Modal panels** (pause / results / rules / trip log / settings) are drawn in Compose — rounded
rectangle, asphalt→navy gradient, gold border — and grow with their content.

## App icon

Adaptive icon (`mipmap-anydpi-v26`): background layer = the road scene cropped from the
background art (`ic_launcher_bg.jpg`), foreground = the Chickie Runway logo inside the 66 dp safe
zone (`ic_launcher_fg.png`), plus a monochrome silhouette (`ic_launcher_mono.png`) for Android 13+
themed icons. Legacy icons for API 24–25 (`mipmap-*dpi`, square + round) are made from `icon.jpg`.
A 512×512 Play Store icon is in `store/play_store_icon_512.png`.

## Splash

`androidx.core:core-splashscreen`: Sky Blue background (`#4DAEFD`) with a small Chickie emblem
(`splash_emblem.png`, ~15 KB, sized for the splash safe circle), then `postSplashScreenTheme`.
Same look on API 24–36.

## Accessibility

White/cream text on charcoal/navy plates and panels (high contrast); outlined gold titles. All
touch targets ≥ 48 dp (icon buttons 54 dp, plates ≥ 58 dp, level bubbles, 56 dp switch row).
Every image has a content description or is marked decorative; cards announce
"Card n, face down / Traffic cone / matched"; answer feedback uses a polite live region. State is
never colour-only (✓ / ✗ / 🔒 / "Locked" / "Hurry!" text). Text is in `sp`; screens scroll when
content does not fit, and decorative mascots disappear first when space is short.

## Tech stack

Kotlin 2.0.21 · Jetpack Compose (BOM 2024.12.01, Material 3) · Navigation Compose 2.8.5
(type-safe `@Serializable` routes) · Lifecycle/ViewModel 2.8.7 · Coroutines 1.9.0 + Flow ·
DataStore Preferences 1.1.1 · Kotlinx Serialization 1.7.3 · AGP 8.7.3 · Gradle 8.9 (Kotlin DSL,
`gradle/libs.versions.toml`) · JDK 17. Every imported package is a direct catalog dependency
(incl. `compose.foundation`, `compose.animation`, `ui-unit`, `ui-text`, `ui-geometry`, …).

## Architecture

```
app/src/main/java/com/chickierunway/game/
├─ ChickieApp.kt            Application: creates GameRepository + SoundManager, syncs sound toggle
├─ MainActivity.kt          splash, edge-to-edge, theme, CompositionLocal for sounds
├─ Assets.kt                single entry point for ALL image resources
├─ audio/SoundManager.kt    SoundPool SFX (click, correct, wrong, match, win, lose)
├─ data/                    GameData, GameSerializer, GameRepository (DataStore), QuizBank
├─ logic/GameLogic.kt       pure rules: levels, deck building, unlock, completed count
└─ ui/
   ├─ theme/                palette, typography, Material 3 theme
   ├─ components/           PlateButton, MenuPlateButton, IconPlateButton, ModalPanel,
   │                        PanelOverlay, GameTopBar, ErrorFallback, …
   ├─ navigation/           Routes.kt (typed routes), AppNavHost.kt (safe navigation)
   ├─ viewmodel/            ProgressViewModel, QuizViewModel, PairsViewModel (+ Factories)
   └─ screens/              all screens
```

Simple MVVM: ViewModels expose immutable `StateFlow` UI state, screens collect with
`collectAsStateWithLifecycle`. The repository is created once in `ChickieApp` and handed to plain
`ViewModelProvider.Factory` classes — no DI framework.

**Safe navigation** — every route argument has a default; screens validate them (unknown quiz
category, level outside 1..9, impossible scores) and show `ErrorFallback` with a Back button.
Navigation calls only fire if the calling screen is still the current back-stack entry (no double
pushes/pops during transitions); pop never empties the stack; "Menu" pops to the always-present
Main start destination. Empty or reset storage simply shows zeros and only level 1 unlocked.
After process death, typed routes restore from saved state; an in-progress quiz/level restarts.

## Art, quiz and sound files

- **Art** — `app/src/main/res/drawable-nodpi/` (compressed; background as JPG):

  | File | Source art | Used for |
  |---|---|---|
  | `bg_main.jpg` | background.jpg (cropped 9:16, 1080×1920) | all screens (blurred on game screens) |
  | `logo.png` | logo.png | home, menu, card backs, launcher foreground |
  | `mascot_a…d.png` | hero_1, hero_4, hero_3, hero_2 | happy / wink / surprised / running |
  | `btn_menu.png` | button_clean.png | framed menu plate (720×244) |
  | `btn_plate.png` | button.png | all other buttons (3-slice) |
  | `plate_round.png` | button_circle_clean.png | level bubbles |
  | `icon_back.png`, `icon_pause.png` | button_back / button_pause | round icon buttons |
  | `item_01…18.png` | all 18 item_*.png | pairs deck + decorative icons |

  All references go through `Assets.kt`; replace files with the same names to reskin. If a plate's
  proportions change, update `BTN_MENU_ASPECT` / `PLATE_CAP_FACTOR` / `PLATE_INNER_INSET`.
  Spoken names of the items are in `Assets.itemNames`.
- **Quiz** — `data/QuizBank.kt` (marked REPLACEABLE): 3 levels × 10 questions × 4 options. Keep the
  level ids (`easy`, `medium`, `hard`) stable — they key the saved best scores. `QuizBankTest`
  checks the structure.
- **Sounds** — `res/raw/sfx_*.wav` are synthesized placeholder tones (click, correct, wrong, match,
  win, lose); replace with real SFX of the same names.

## Open in Android Studio

1. File → Open → select the project folder (Ladybug 2024.2.1 or newer).
2. Settings → Build Tools → Gradle → **Gradle JDK = 17**.
3. SDK Manager: install **Android 16 (API 36)** platform and **Build-Tools 36.0.0**.
4. Sync. `gradle.properties` contains `android.suppressUnsupportedCompileSdk=36` because AGP 8.7.x
   predates API 36. If resource linking ever fails against the API 36 `android.jar` with this AGP,
   bump `agp` in `gradle/libs.versions.toml` to 8.9.1+ (Gradle 8.11.1+) — no code changes needed.

The Gradle 8.9 wrapper is included (`./gradlew`). If you prefer, CI uses a fixed Gradle 8.9 via
`gradle/actions/setup-gradle`, so the wrapper is not required there.

## 16 KB memory page size

Pure Kotlin/Compose — no native (`.so`) libraries, so the app is 16 KB page-size compatible. Still
verify the built artifacts: `unzip -l app-release.apk | grep '\.so$'` should print nothing (CI also
checks and would run `zipalign -c -P 16` if any native lib appeared). In Android Studio you can use
*Build → Analyze APK* on the AAB/APK.

## Debug build

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
./gradlew :app:testDebugUnitTest      # unit tests
```

Use `assembleDebug`/`testDebugUnitTest` for development — `./gradlew build` also assembles the
release variant and will stop if signing variables are not set (by design).

## Release signing (PKCS12, env vars only)

`app/build.gradle.kts` reads **only** these environment variables:

| Variable | Meaning |
|---|---|
| `ANDROID_KEYSTORE_PATH` | path to the `.p12` keystore |
| `ANDROID_KEYSTORE_PASSWORD` | store password |
| `ANDROID_KEY_ALIAS` | key alias |
| `ANDROID_KEY_PASSWORD` | key password (same as store password for PKCS12) |

`signingConfigs.release` is used by `buildTypes.release`. If any `assemble*/bundle*/package*Release`
task is requested and a variable is missing (or the file does not exist) the build **fails with a
clear message**. It never falls back to the debug key.

### Generate a PKCS12 keystore

```bash
keytool -genkeypair -v -storetype PKCS12 \
  -keystore chickierunway-release.p12 -alias chickierunway \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=Chickie Runway, OU=Mobile, O=Chickie Runway, C=BY"
```

Back it up in at least two safe places. Losing it (without Play App Signing) means you can never
update the app. Never commit it — `.gitignore` excludes `*.p12`, `*.jks`, `*.keystore`,
`signing.env`, `*.b64`.

### Local signed build (non-minified release)

```bash
export ANDROID_KEYSTORE_PATH=/abs/path/chickierunway-release.p12
export ANDROID_KEYSTORE_PASSWORD='…'
export ANDROID_KEY_ALIAS=chickierunway
export ANDROID_KEY_PASSWORD='…'
./gradlew :app:assembleRelease :app:bundleRelease
# APK: app/build/outputs/apk/release/app-release.apk
# AAB: app/build/outputs/bundle/release/app-release.aab
```

### Verify the signature (apksigner)

```bash
$ANDROID_HOME/build-tools/36.0.0/apksigner verify --verbose --print-certs \
  app/build/outputs/apk/release/app-release.apk
# must say "Verifies" and the DN must NOT be "CN=Android Debug"
jarsigner -verify -certs app/build/outputs/bundle/release/app-release.aab   # AAB
```

## GitHub Secrets

Repository → Settings → Secrets and variables → Actions → *New repository secret*:

| Secret | Value |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | `base64 -w0 chickierunway-release.p12` (macOS: `base64 -i file.p12`) |
| `ANDROID_KEYSTORE_PASSWORD` | store password |
| `ANDROID_KEY_ALIAS` | `chickierunway` |
| `ANDROID_KEY_PASSWORD` | key password |

## GitHub Actions

`.github/workflows/android-build.yml` runs on push to `main` and manually (`workflow_dispatch`):
JDK 17 → SDK Platform 36 + Build-Tools 36.0.0 → Gradle 8.9 via `gradle/actions/setup-gradle` →
secrets check → decode keystore to `$RUNNER_TEMP` → unit tests → signed `assembleRelease` +
`bundleRelease` → `apksigner verify --print-certs` (fails on verification error or
`CN=Android Debug`) → AAB `jarsigner` check → 16 KB check → uploads **APK** (test artifact) and
**AAB** (Google Play artifact) → deletes the keystore. Secrets are never echoed. No emulator step.

> **A green CI build is not proof that the app launches.** Always install and run it on a real
> device or emulator (below).

## APK vs AAB

- **AAB** (`app-release.aab`) — the **only** file you upload to Google Play.
- **APK** (`app-release.apk`) — for side-loading and testing on devices; not for Play.

## Staged R8

1. Ship the first release with `isMinifyEnabled = false` and `isShrinkResources = false` (current
   state) and verify it completely.
2. Then set both to `true` in `app/build.gradle.kts`. `proguard-rules.pro` already keeps
   Kotlinx-Serialization serializers and the `@Serializable` data models / navigation routes.
3. Rebuild the signed release and repeat the full verification checklist — pay special attention
   to saved progress (JSON) and navigation between all screens.

## Local release verification

```bash
adb uninstall com.chickierunway.game   # optional: clean state
adb install -r app/build/outputs/apk/release/app-release.apk
adb logcat --pid=$(adb shell pidof -s com.chickierunway.game) '*:E'
```

Checklist:

- [ ] Themed splash, then the Road Dash home screen; launcher icon is the Chickie logo.
- [ ] Menu → Quiz: every difficulty, answer feedback (green ✓ / red ✗), auto-advance, results,
      Continue / Restart / Menu, best score updates.
- [ ] Pause (button and system Back) in Quiz and Pairs; timer stops while paused and in background.
- [ ] Pairs: win level 1 → level 2 unlocks; let a level time out → "Out of Time".
- [ ] Level 9 (24 cards) fits on a small phone (e.g. 360×640 dp) without scrolling.
- [ ] Results summary shows bests and "Cleared n / 9".
- [ ] Settings: sound toggle works immediately and persists after restart; Privacy / Rate open an
      external app or show a toast; Reset asks for confirmation and clears progress.
- [ ] Rotate is locked to portrait; content respects status/navigation bars and display cutouts.
- [ ] Kill the process in the background (`adb shell am kill com.chickierunway.game`) on each
      screen and return — no crash.
- [ ] Airplane mode: everything works.
- [ ] `adb logcat` shows no `FATAL EXCEPTION`.
- [ ] Repeat all of the above after enabling R8.

## Data reset

In-app: Settings → Reset Progress → Reset (clears quiz bests and pairs progress; the sound
preference is kept). Full wipe: Android Settings → Apps → Chickie Runway → Storage → Clear
storage, or `adb shell pm clear com.chickierunway.game`.

## Known limitations

- Sound effects are synthesized placeholder tones.
- English only (strings are in code for the placeholder build).
- An in-progress quiz or pairs level restarts after process death (saved bests are not affected).
- Portrait is locked; on Android 16 large screens (≥ 600 dp) the system may ignore the orientation
  lock and letterbox or resize the app.
- `AppLinks.PRIVACY_POLICY_URL` is a placeholder until you publish your policy.
- The game uses its own day palette in both light and dark system themes.
