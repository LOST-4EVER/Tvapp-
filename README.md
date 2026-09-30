# Kurdish TV Live

An Android TV app for streaming live Kurdish television, built with Jetpack Compose
and Media3 ExoPlayer. It merges a bundled offline catalogue with four live
community playlists plus any the viewer imports, and plays them as HLS on a
television, a TV box, a tablet or a phone.

<p align="center">
  <img src="app/src/main/res/mipmap-xxxhdpi/ic_launcher.png" width="96" alt="App icon">
</p>

| | |
|---|---|
| **Application ID** | `com.aistudio.kurdishtv.live` |
| **Version** | see `kurdishTvVersionName` in [`gradle.properties`](gradle.properties) |
| **Min SDK** | 24 (Android 7.0) |
| **Target / compile SDK** | 36 |
| **Language** | Kotlin 2.2.10, Jetpack Compose (BOM 2024.09.00) |
| **Build** | AGP 9.1.1, Gradle 9.3.1, JDK 21 |
| **Repository** | <https://github.com/LOST-4EVER/Tvapp-> |
| **Catalogue** | 95 channels, all verified end to end |

---

## Contents

- [What it does](#what-it-does)
- [Features](#features)
- [Architecture](#architecture)
- [Project layout](#project-layout)
- [Getting started](#getting-started)
- [Build variants and signing](#build-variants-and-signing)
- [Versioning](#versioning)
- [How channels are sourced](#how-channels-are-sourced)
- [How updates work](#how-updates-work)
- [CI/CD](#cicd)
- [Testing](#testing)
- [Performance notes](#performance-notes)
- [Configuration reference](#configuration-reference)
- [Troubleshooting](#troubleshooting)

---

## What it does

Kurdish-language television is published almost entirely as open HLS streams
scattered across community playlists, with no single reliable index. This app
concentrates them: it seeds itself with a hand-verified offline catalogue so it
is useful on first launch with no network at all, then merges several live public
playlists on top of that and lets the viewer import their own.

The design priority throughout is the **ten-foot experience**. The app is designed
for a D-pad and a ten-foot viewing distance first, and adapts to touch second.

## Features

**Browsing**
- Channel grid with artwork, live badges, and per-category colour identity
- Channel sidebar on televisions and wide tablets, with the number of each channel
- **Remote number pad** — type a channel number, see where it resolves, commit
- `CHANNEL UP`/`DOWN` steps through the list the viewer is actually looking at
- Twelve categories (News, Kurdish Culture, General, Music, Kids, Sports,
  Documentary, Quran, Religious, Favourites, HD 1080p, All), each showing live
  counts so a chip never leads to an empty grid
- Recently-watched row
- Featured hero card for the current selection

**Playback**
- HLS playback via Media3 ExoPlayer over a dedicated OkHttp client
- Fit / Fill / Zoom resize mode and six video colour presets, both **persisted**
- Sleep timer, mute, next/previous channel
- Side preview pane on tablets and wide screens, which deliberately does *not*
  fetch until playback is asked for
- Buffering indicator, live edge, and a retryable error state

**Search**
- Matching is canonicalised on both sides, so `nrt-1`, `nrt_1`, `nrt1` and
  `NRT 1` all find the same channel
- Category text is searchable alongside the name
- Debounced at 220 ms, computed off the main thread

**Library management**
- Import up to 20 custom M3U/JSON playlist links
- Favourites and watch history
- Per-category filtering, offline banner, and a clearable on-disk cache

**Platform**
- Appears on the **Android TV home screen** (`LEANBACK_LAUNCHER`)
- D-pad focus with a heavy outset focus ring, and correct focus restoration when
  returning from the player
- Edge-to-edge, with insets applied per route
- In-app updater with resumable download, progress, and signature pre-check
- **No animation anywhere.** Every spring, easing, frame loop and shape morph has
  been deleted, not disabled: focus and press are instant, and the only motion the
  app has left is a spinner replacing itself
- Material You dynamic colour on Android 12+, plus five bundled accent palettes

**Monetisation**
- An **Ads** section at the foot of Settings, below About, with a switch that is the
  viewer's own consent: a "Watch an ad" button that plays a Start.io full-page
  (interstitial) ad, and a banner, both shown only while it is on
- Switching ads off is a hard opt-out — the SDK is not started, nothing is requested,
  and an ad already fetched is released. The switch stays visible either way, so it can
  always be turned back on
- The full-page ad is preloaded as soon as the SDK reports ready, so the button is
  instant, and a tap made while a request is still out is honoured rather than dropped
- Real ads in release, test ads in debug, and an unfilled slot that draws nothing
  rather than an empty box. Start.io's display inventory is mobile, so neither format
  fills on a television — the placements are there for phones and tablets

## Architecture

Standard MVVM with a unidirectional data flow. No DI framework, no database —
state is `SharedPreferences` plus one JSON file, which is proportionate to what
the app actually stores.

```
┌──────────────────────────────────────────────────────────────┐
│  MainActivity                                                │
│  builds the repository, view models and the theme            │
└───────────────────────────┬──────────────────────────────────┘
                            │ provides
        ┌───────────────────▼───────────────────┐
        │  TvViewModel          SettingsVM      │
        │  ├ uiState       (channels, filters)  │
        │  ├ sleepTimer    (1 Hz — own flow)    │
        │  ├ channelJump   (keypad — own flow)  │
        │  └ updateState   (check/download)     │
        └───────────────────┬───────────────────┘
                            │ calls
        ┌───────────────────▼───────────────────┐
        │  TvRepository                           │
        │  merges 4 remote sources + catalogue    │
        │  + any user-imported playlists          │
        └───┬───────────────┬───────────────┬─────┘
            │               │               │
   ┌────────▼──────┐ ┌──────▼───────┐ ┌─────▼──────────┐
   │ KurdishTv     │ │ NetworkClient│ │ FavoriteStorage│
   │ Parser        │ │ + OkHttp     │ │ RecentStorage  │
   │ (M3U/JSON)    │ │ NetworkMonit.│ │ CustomPlaylist │
   └───────────────┘ └──────────────┘ └────────────────┘
                                      ChannelCacheStorage
```

**Data flow.** `TvRepository` fetches every source concurrently under a
`supervisorScope`, so one dead playlist cannot discard the channels that did
load. Results are deduplicated by stream URL, given collision-free ids, stamped
with favourites, and written to an atomic on-disk cache. `ChannelFilterEngine`
then does all filtering and searching off the main thread.

**The split of `TvUiState`.** Three things are deliberately *not* on the main
state, because each changes far more often than the screen it belongs to and
would invalidate every collector of the shared state:

| Flow | Cadence | Collected by |
|---|---|---|
| `sleepTimer` | once a second | the player only |
| `channelJump` | once per digit | the browse screen only |
| `updateState` | per download chunk | the settings screen only |

## Project layout

```
app/src/main/java/com/example/
├── MainActivity.kt              Entry point, dependency wiring
├── kurdishtv/
│   ├── ads/                     Start.io ad SDK wiring
│   │   ├── AdsPolicy.kt         the one gate every request goes through
│   │   ├── StartIoAds.kt        one-time SDK init, readiness and consent state
│   │   ├── StartIoAdsHost.kt    drives the SDK lifecycle from the preference
│   │   ├── StartIoBanner.kt     Compose banner that draws nothing when unfilled
│   │   └── StartIoFullPage.kt   preloaded full-page ad behind the button
│   ├── data/                    SharedPreferences + JSON cache
│   │   ├── ChannelCacheStorage    atomic channel-list cache
│   │   ├── CustomPlaylistStorage  user playlist links, validated
│   │   ├── FavoriteStorage        favourites
│   │   ├── RecentStorage          watch history, JSON + migration
│   │   └── SettingsStorage        AppSettings
│   ├── model/
│   │   ├── Channel.kt             the domain model
│   │   ├── AppSettings.kt         user preferences
│   │   ├── CategoryFilter.kt      the twelve categories
│   │   ├── ChannelFilterEngine.kt filtering, search, category counts
│   │   └── KurdishChannelCatalog  the offline catalogue (~100 channels)
│   ├── network/
│   │   ├── NetworkClient.kt       two OkHttp clients, capped reads
│   │   └── NetworkMonitor.kt      connectivity as a Flow
│   ├── parser/KurdishTvParser.kt  M3U / JSON → Channel
│   ├── repository/TvRepository.kt  source merge and cache policy
│   ├── update/
│   │   ├── UpdateChecker.kt       manifest + releases API, download
│   │   ├── UpdateManifestParser.kt parses both feed shapes
│   │   ├── UpdateModels.kt        AppUpdate, DownloadState, UpdateState
│   │   └── ApkInstaller.kt        FileProvider + signature check
│   ├── viewmodel/                 TvViewModel, SettingsViewModel, TvUiState
│   └── ui/
│       ├── screens/               MainTvScreen, PlayerScreen, SettingsScreen
│       ├── components/            23 shared composables
│       ├── player/                VideoPlayerView, controls, colour filters
│       ├── motion/                focus ring, TV click, shape tokens
│       ├── theme/                 M3 Expressive shapes and polygons
│       └── navigation/            NavGraph
└── ui/theme/                      colour, type, theme, TV detection
```

## Getting started

### Requirements

- **JDK 17 or newer** (the build targets Java 21; CI uses Temurin 21)
- The **Android SDK**, platform 36 and build-tools
  (`local.properties` → `sdk.dir=/path/to/Android/sdk`, or `ANDROID_HOME`)

### Build and test

```bash
./gradlew assembleDebug          # debug APK
./gradlew test                   # unit + Robolectric tests
./gradlew lintRelease            # Android Lint
./gradlew assembleRelease        # minified release APK
./gradlew auditStreams           # verify every catalogue stream against the network
```

`./gradlew` needs no Gradle installed. It uses a Gradle on `PATH` if there is
one, otherwise the official wrapper if `gradle/wrapper/gradle-wrapper.jar` has
been restored into the checkout, and otherwise it downloads the exact
distribution pinned in `gradle/wrapper/gradle-wrapper.properties` once and
caches it. The version it uses therefore always matches CI — CI pins the same
`9.3.1` rather than resolving whatever Gradle happened to be current.

### Build time

A few things are deliberately *not* in the build, because nothing consumed them:

- **KSP is not applied.** Every annotation processor that would use it
  (`room-compiler`, `moshi-codegen`) is commented out in `app/build.gradle.kts`,
  so it was joining the task graph and producing nothing. The plugin alias is
  still in the version catalog — re-add it in the same commit that uncomments a
  `ksp(...)` dependency.
- **The Maps Platform secrets plugin is not applied.** It generated `SECRET_*`
  `BuildConfig` fields, and this app reads no secrets: it talks only to public
  playlists and a public update manifest. It also pulled Google Maps Platform
  into a build with no Maps and no Firebase.

Configuration cache, build cache, parallel project evaluation and the Kotlin
compiler daemon are all enabled in `gradle.properties`.

### Run on a device

```bash
./gradlew installDebug
adb shell am start -n com.aistudio.kurdishtv.live/com.example.MainActivity
```

## Build variants and signing

| Variant | Minified | Signed with |
|---|---|---|
| `debug` | no | the local `debug.keystore` |
| `release` | yes (R8 full mode + resource shrinking) | the release key |

The release signing key is resolved in this order:

1. `KEYSTORE_PATH` / `STORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` from the
   environment — a real upload key
2. `release-key.jks` in the repository root, decoded from
   `debug.keystore.base64`
3. Otherwise the build **warns loudly** and falls back to the debug key, which
   cannot be installed over a published release

> ⚠️ **Every build in a chain must use the same key.** Android refuses to install
> an update signed with a different one, and reports it as
> `INSTALL_FAILED_UPDATE_INCOMPATIBLE` — "App not installed as package conflicts
> with an existing package". CI deliberately fails rather than generating a fresh
> key, because a new key silently breaks every future update. See
> [SIGNING.md](SIGNING.md).

R8 can shrink this app because [proguard-rules.pro](app/proguard-rules.pro) keeps
only what is genuinely reached reflectively. In particular it does **not** blanket-keep
Media3 — that single rule once pinned every extractor and renderer for formats
the app never plays.

## Versioning

Two independent numbers, deliberately:

- **`versionCode`** — the CI run number, passed as `-PversionCode`. This is what
  the updater compares against, so it must be the same scale as the
  `version_code` in `update.json`.
- **`versionName`** — the human-facing version in Settings → About. Never the run
  number, which is why it is a separate property.

`kurdishTvVersionCode` in `gradle.properties` is the **local floor**. It must
stay at or above the newest published build or Android rejects a local build as a
downgrade. Check `update.json` for the current published value.

CI derives the release tag from the newest published tag plus one, so every green
build gets its own version, its own tag, and its own immutable asset.

## How channels are sourced

Four public sources are merged concurrently on every fetch:

| Tag | Source |
|---|---|
| `gist` | a curated JSON gist |
| `krd` | `BotanAtomic/Kurdistan-TV` canal list |
| `iptv` | `iptv-org` Kurdish language playlist |
| `fbk` | a fallback M3U gist |

plus any viewer-imported playlists, plus the bundled offline catalogue.

**Stable ids.** A channel's id is derived from its source, name and stream URL —
never from its position in the list. Adding, removing or reordering entries
therefore keeps existing favourites and watch history pointing at the right
channel. `Channel.originalId` preserves the pre-deduplication id so a channel
that picks up a collision suffix is still the same channel to someone who
hearted it earlier.

**Caching.** A merged list younger than 30 minutes is served from disk without a
network call. Writes go to a temp file and are renamed, which is atomic on the
same filesystem, so a kill partway through can no longer leave a half-written
cache. A source that returns nothing usable is treated as a problem with that
source; it never falls back to injecting unrelated channels.

### Auditing the catalogue

Third-party streams rot, and a broken one is worse than a missing one: it takes
up a slot in the grid and only fails once the viewer commits to it.

```bash
./gradlew auditStreams              # or: python3 scripts/audit_streams.py
python3 scripts/audit_streams.py --url https://example.com/stream.m3u8
```

The script walks each stream all the way down — master playlist, then the
highest-bandwidth variant, then a real media segment — and requires actual
container bytes. A `200` on the manifest is close to worthless on its own: this
catalogue has had a channel whose master and variant both answered `200` while
every media segment returned `403`, so it passed every check that stopped short
of the media and could never play.

Two things it deliberately does:

- **It retries before calling a stream dead.** A live playlist is rewritten
  continuously, so a segment listed a moment ago can `404` by the time it is
  fetched. On a first run three streams failed and all three passed seconds later;
  a tool that trusted the first answer would have had working channels deleted.
  Streams that verify but not first try are reported as `flaky`.
- **It reports time-to-first-playlist.** That is the gap between pressing OK and
  seeing video, and it is invisible in the source. Three channels were on bare
  origins at 424-1027 ms and are now on CDN edge copies at 42-51 ms.

> Latency is measured from wherever the script runs. Ratios between two origins
> hold up reasonably; absolute milliseconds describe the runner, not a viewer's
> sofa. Treat the slow list as "look here next", not as a verdict.

It is a maintenance tool, not a build step, and is wired to no assemble task —
the build must never fail because a broadcaster is having a bad afternoon.

**Category mapping.** `KurdishTvParser` maps playlist group titles to the twelve
categories. The ordering is load-bearing: sports is tested before the broad
Kurdish/news rules (which would otherwise swallow sports channels carrying a
news-style group title), and religious is tested before kids (several religious
channels are grouped under a family title in community playlists).

## How updates work

`update.json` at the repository root is the primary feed. The release job writes
it, and it carries the CI run number that is also compiled into the APK as
`VERSION_CODE`, so the two are always directly comparable.

1. `UpdateChecker` reads the manifest. If it is unreachable it falls back to the
   GitHub releases API, and recovers the run number from the release body — the
   only other value on the right scale.
2. The comparison is numeric. A tag's patch number (22) is on a *different
   scale* from a run number (36); comparing them makes the app report itself
   current forever with a newer release available.
3. Background checks are throttled to once every six hours, because the request
   is deliberately uncacheable and releases do not appear and vanish. The
   Settings button always runs an unthrottled check.
4. Downloads stream to disk with HTTP range resume, keyed to the build number so
   a partial download of a *previous* release can never be resumed into a
   different APK, and the size is verified before the file is offered.
5. Before install, `ApkInstaller` compares signing certificates so a key change
   is reported as the uninstall it actually requires, rather than as a corrupt
   download.

## CI/CD

[`.github/workflows/github.yaml`](.github/workflows/github.yaml) runs on pushes
to `main`, on `v*` tags, and on pull requests. It:

1. sets up JDK 21 and Gradle 9.3.1 (the version the wrapper pins)
2. lints the workflow files themselves with `actionlint`
3. decodes and verifies the release signing key, failing loudly if it is unusable
4. resolves the release version and tag
5. runs Android Lint
6. builds a single universal release APK with `-PversionCode=${{ github.run_number }}`
7. publishes a GitHub release
8. rewrites `update.json` and commits it back

Pull requests build and lint but never publish a release.

**One artifact, one APK.** There is no `splits` block, so the release is a single
universal APK that runs on phones, tablets and televisions alike — there is
nothing to choose between and no per-ABI download to pick the wrong one from.

Note that the Android Lint step is **advisory**: `lint { abortOnError = false }`
means lint reports findings without failing the build, so that step annotates the
run summary but would not stop a release even if it found an error.

## Testing

```bash
./gradlew test
```

| Suite | Covers |
|---|---|
| `ChannelFilterEngineTest` | search canonicalisation, category filtering, count consistency |
| `KurdishTvParserTest` | M3U and JSON parsing, quoted commas, name formatting |
| `StorageTest` | favourites, watch history and its format migration, playlist link validation, the channel cache |
| `UpdateManifestParserTest` | both feed shapes, the version-scale rule, asset selection |
| `ExampleRobolectricTest` | context and activity launch smoke test |

The storage and updater suites exist because those are where data loss and
silently-missed updates have actually happened. The format-migration tests in
particular exist so the comma-delimited history bug cannot come back.

## Performance notes

### Launch time

[`app/src/main/baseline-prof.txt`](app/src/main/baseline-prof.txt) is an ART
baseline profile. AGP compiles it into `assets/dexopt/baseline.prof` at build
time, and `androidx.profileinstaller` installs it on first run so ART
AOT-compiles the startup path instead of discovering it at runtime. This is the
largest single launch-time lever available to an Android app, and it costs no
new toolchain.

The rules are hand-written and use `**` wildcards throughout, so a class rename
or move cannot invalidate one. `S` marks the startup path, `H` the hot path, and
a rule with no `->` pre-allocates the class so it is not loaded lazily on the
critical frame.

> The canonical way to produce this file is `./gradlew generateBaselineProfile`
> from a `macrobenchmark` module, which walks a real device and records what
> actually ran. The hand-written rules here are a good starting point; a
> generated profile would supersede them once you have hardware to run it on.

### Runtime

The app is built around the fact that the merged list can exceed a thousand
channels and the grid holds several hundred cards. The decisions that follow from
that:

- **Filtering runs off the main thread.** Whole-catalogue passes during cold-start
  hydration and playlist merges use `Dispatchers.Default`, with the filter and the
  state swap kept in one `update` so a query cannot move between them.
- **Search keys are memoised per list.** Normalised name+category strings are
  built once per distinct channel list rather than per keystroke, held through a
  `WeakReference` so the cache cannot keep a discarded list alive.
- **Category counts are one pass.** `countsByCategory` computes all twelve counts
  together rather than calling `filter` twelve times, and folds each channel's
  category string once instead of asking for nine case-insensitive substring
  matches, which is a per-character fold at every position of the string.
- **There is no motion to optimise.** The shared LIVE pulse and the focus rotation
  used to run `withFrameNanos` loops for the whole life of the app, drawing
  nothing; a `withFrameNanos` loop re-arms through the Choreographer, which keeps
  posting for as long as the display is on whether or not the app is in front of
  it. Both loops are gone, along with the shape-morphing layer, the grid's fade-in
  and the springs behind every press and focus.
- **The cache is counted, not parsed.** The merge needs to know how many channels
  are on disk before it will overwrite them. Reading that number used to mean
  building a `Channel` for every entry in the file; it is now a single pass that
  counts array elements and allocates nothing.
- **Scrolling the grid reuses item compositions.** Channel cells declare a
  content type, so a lazy layout can hand a recycled composition to the next
  cell instead of composing every one of several hundred channels from scratch.
- **Selecting a channel writes the watch history once.** The store hands back the
  history it just wrote, so the "continue watching" row no longer re-reads and
  re-parses the file it was just given.
- **Media traffic is isolated.** Video uses a separate OkHttp client with no HTTP
  cache, so HLS segments cannot evict playlists and logos, and a grid full of logos
  cannot starve a stream of its per-host connection slots.
- **Logo decode sizes are bucketed to powers of two**, so the same channel's logo
  is the same cached bitmap across different card sizes.

## Configuration reference

| Property | Meaning |
|---|---|
| `kurdishTvVersionCode` | local `versionCode` floor; must be ≥ the newest published build |
| `kurdishTvVersionName` | human-facing version shown in Settings → About |
| `-PversionCode=` | CI override, set to the GitHub run number |
| `KEYSTORE_PATH` | path to an upload keystore, for CI |
| Start.io app id | `208672276`, compiled into `StartIoAds.kt`; deliberately not an environment variable |
| `STORE_PASSWORD` / `KEY_PASSWORD` / `KEY_ALIAS` | keystore credentials |

The release key itself is **not** an environment variable. It is committed as
`debug.keystore.base64` and decoded in CI, precisely so a missing secret fails
the build instead of silently producing an uninstallable APK.

Permissions requested: `INTERNET`, `ACCESS_NETWORK_STATE`, `WAKE_LOCK`,
`MODIFY_AUDIO_SETTINGS`, and `REQUEST_INSTALL_PACKAGES` for in-app updates.
`usesCleartextTraffic` is enabled because a number of the streams are plain HTTP.

The installed app requests more than that list. The Start.io ad SDK declares
`ACCESS_WIFI_STATE`, `RECEIVE_BOOT_COMPLETED`,
`com.google.android.gms.permission.AD_ID`, `ACCESS_ADSERVICES_TOPICS` and the Play
install-referrer permission in its own manifest, and those merge into the APK. They
are left to the SDK rather than copied here so there is only one place for them to
drift.

## Troubleshooting

**"App not installed as package conflicts with an existing package."** An older
copy signed with a *different* key is present. Android cannot upgrade across a
key change — uninstall once, then install.

**"App blocked to protect your device."** Google Play Protect flags any sideloaded
app not published on the Play Store. Choose *More details → Install anyway*.

**No channels at all.** The offline catalogue should always be present, so an
empty grid means the app was modified. Check Settings → clear the channel cache,
then refresh. A green status bar with a 0 count per category would point at the
catalogue itself.

**A stream will not play.** Third-party HLS endpoints break. The catalogue entries
were probed live before release, but a stream that answered 200 then can 404
later. Import an alternative playlist from Settings.

**`./gradlew` cannot find Java.** Install JDK 17+, or set `JAVA_HOME`. The wrapper
reports this explicitly rather than failing obscurely.

---

## Contributing

- Comments in this codebase explain **why** a non-obvious decision was made, not
  what the line does. Many record a specific bug that would otherwise return.
  Please keep that standard — a comment that only restates the code is noise.
- Run `./gradlew test` before opening a pull request.
- Bump `kurdishTvVersionCode` if your change needs a local build installed over a
  published release.
- Add or update [CHANGELOG.md](CHANGELOG.md); it follows
  [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## License

See the repository for licensing terms.
