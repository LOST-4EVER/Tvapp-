# Kurdish TV Live (تەلەفزیۆنی کوردی)

A high-performance, modern Android & Android TV streaming application for live Kurdish television broadcasts, crafted with **Kotlin**, **Jetpack Compose (Material 3)**, and **Media3 ExoPlayer**. 

Designed mobile-first and ten-foot-ready with seamless D-pad remote navigation, adaptive multi-pane layouts for tablets and foldables, instant offline caching, live video color filters, and ultra-fast HLS streaming.

---

## 📺 Overview

Kurdish TV Live aggregates and streams Kurdish-language satellite, news, culture, entertainment, kids, music, sports, and religious channels from verified IPTV sources. The app provides:

- **Instant Offline Seeding:** Bundled offline catalogue ensures the app starts immediately with zero delay, even with no network connection.
- **Dynamic Multi-Source IPTV Merging:** Concurrently fetches and deduplicates channels from multiple remote playlists and user-imported custom M3U/JSON playlists.
- **Optimized Media3 Engine:** Isolated video network pipeline with custom connection pooling, TextureView surface rendering, and low-latency HLS buffer control.
- **Pure AMOLED Black & Custom Accents:** Default pitch-black (`#000000`) theme with five customizable accent palettes (Sun Gold, Ember, Rose, Jade, Azure) and Android 12+ dynamic wallpaper colors.
- **10-Foot Android TV Interface:** Outset high-contrast focus rings, remote number-pad direct channel jumps, and D-pad directional navigation.
- **Privacy-First Optional Ads System:** Ads are **disabled by default**. The Ads menu stays hidden until unlocked by holding the full cache reset button for 10 seconds, followed by a transparent Start.io terms of service and consent agreement.

---

## 🌟 Key Features

### 📡 Channel Browsing & Navigation
- **Responsive Layouts:** Handheld phone view, dual-pane tablet view, and 10-foot Android TV layout (`LEANBACK_LAUNCHER`).
- **Remote Number Pad Channel Jump:** Type a channel number with your remote's digit keys (e.g. `47`) to instantly target and scroll to channels.
- **12 Curated Categories:** All, News, Kurdish Culture, General, Music, Kids, Sports, Documentary, Quran, Religious, Favorites, and HD 1080p with live channel counters.
- **Instant Search:** Normalized, accent-insensitive search across channel names and category keywords computed off the main thread.
- **Favorites & History:** One-tap favoriting and automatic recently-watched tracking persisted locally.

### 🎬 High-Performance Video Playback
- **Media3 ExoPlayer Streaming:** Robust adaptive bitrate playback for HLS (`.m3u8`) and HTTP live streams.
- **Live Video Color Matrix Filters:** Hardware-accelerated color post-processing (`Vivid`, `Warm`, `Cool`, `Mono`, `Night`) directly applied to live video.
- **Aspect Ratio Modes:** `Fill`, `Fit`, and `Zoom` screen framing with persistent user preferences.
- **Playback Controls:** Sleep timer (15–120 min), audio mute toggle, channel stepping (`CH+` / `CH-`), and instant stream retry.
- **Side Preview Pane:** Compact live preview on wide screens that stays idle until actively requested.

### 🛡️ Privacy, Storage & Optional Ads
- **Ads Off by Default:** The app does not load, fetch, or request advertisements unless explicitly consented to by the user.
- **Protected Ad Menu Unlock:** The Ads menu in Settings is revealed only when holding the **Clear channel cache** / Full Cache Reset row for **10 seconds**.
- **Ad Provider Disclosure:** Clear Terms of Agreement detailing Start.io (StartApp Inc.) network responsibility, non-identifiable telemetry, and user opt-out control.
- **Local Persistence:** Uses atomic JSON file caches and SharedPreferences without heavy database overhead.
- **Custom Playlist Imports:** Import up to 20 custom M3U / JSON IPTV playlist URLs with real-time URL validation.

---

## 🏗️ Architecture & Project Structure

The project follows clean **Model-View-ViewModel (MVVM)** architecture with unidirectional data flow (StateFlow & Coroutines):

```
app/src/main/java/com/example/
├── MainActivity.kt                      // App entry point, edge-to-edge & theme container
├── kurdishtv/
│   ├── ads/                             // Ad policy, hardware safety & Start.io SDK
│   │   ├── AdEnvironment.kt             // Headless / render-node safety detector
│   │   ├── AdsPolicy.kt                 // Consent verification gates
│   │   ├── SafeWebViewHelper.kt         // Crash-resilient WebView render client
│   │   ├── StartIoAds.kt                // Start.io initialization & readiness
│   │   ├── StartIoAdsHost.kt            // Lifecycle-aware ad controller
│   │   ├── StartIoBanner.kt             // Compose banner container
│   │   └── StartIoFullPage.kt           // Interstitial / full-page ad wrapper
│   ├── data/                            // Local persistence layer
│   │   ├── ChannelCacheStorage.kt       // Atomic on-disk channel cache
│   │   ├── CustomPlaylistStorage.kt     // Custom M3U playlist link manager
│   │   ├── FavoriteStorage.kt           // Channel favorites store
│   │   ├── RecentStorage.kt             // Watch history persistence
│   │   └── SettingsStorage.kt           // User preferences storage
│   ├── model/                           // Domain models & business engines
│   │   ├── AppSettings.kt               // Theme, accent, ads & player preferences
│   │   ├── CategoryFilter.kt            // Category taxonomy & display names
│   │   ├── Channel.kt                   // Core channel data model
│   │   ├── ChannelFilterEngine.kt       // Background search & filter engine
│   │   └── KurdishChannelCatalog.kt     // Bundled offline catalog (~100 channels)
│   ├── network/                         // Networking & DNS
│   │   ├── FastCachingDns.kt            // Thread-safe in-memory DNS cache
│   │   ├── NetworkClient.kt             // Dedicated video & metadata OkHttp clients
│   │   └── NetworkMonitor.kt            // Connectivity status StateFlow
│   ├── parser/                          // M3U & JSON stream parsers
│   │   └── KurdishTvParser.kt           // Deduplication & category classification
│   ├── repository/                      // Remote sources & caching logic
│   │   └── TvRepository.kt              // Concurrent playlist fetcher & cache manager
│   ├── update/                          // In-app updater engine
│   │   ├── ApkInstaller.kt              // Package installer & signature checks
│   │   ├── UpdateChecker.kt             // GitHub release & manifest checker
│   │   └── UpdateModels.kt              // Update & download state models
│   ├── viewmodel/                       // Modular ViewModels & state controllers
│   │   ├── ChannelKeypadController.kt   // Number-pad digit jump controller
│   │   ├── FavoriteRebuilder.kt         // Asynchronous favorite rebuild helper
│   │   ├── SettingsViewModel.kt         // Settings state manager
│   │   ├── SleepTimerManager.kt         // 1-Hz player countdown manager
│   │   ├── TvUiState.kt                 // Main screen immutable UI state
│   │   ├── TvViewModel.kt               // Core channels & playback ViewModel
│   │   └── UpdateController.kt          // OTA download & install controller
│   └── ui/                              // Jetpack Compose UI
│       ├── components/                  // Header, CategoryBar, SearchBar, Cards
│       ├── keys/                        // D-pad key event policies & indices
│       ├── motion/                      // Outset focus rings, TV click & animations
│       ├── navigation/                  // NavGraph & route definitions
│       ├── player/                      // VideoPlayerView, ColorMatrix & controls
│       ├── screens/                     // MainTvScreen, PlayerScreen, SettingsScreen
│       └── theme/                       // M3 Color schemes, Shapes & Typography
└── ui/theme/                            // App theme providers & TV resolution
```

---

## 🚀 Getting Started

### Prerequisites
- **Android Studio Ladybug (or newer)**
- **JDK 21**
- **Android SDK Platform 36** (Min SDK: 24 / Android 7.0)

### Building the Project

```bash
# Assemble debug APK
./gradlew assembleDebug

# Run unit & Robolectric tests
./gradlew test

# Assemble minified release APK with R8 shrinking
./gradlew assembleRelease
```

---

## ⚡ Performance & Optimization

1. **Dedicated Video Pipeline:** Video segments are downloaded through an isolated `OkHttpClient` instance without HTTP caching, preventing channel logos from starving HLS video bandwidth.
2. **Instant Zero-Alloc Filtering:** Channel searches and category counts are computed off the main thread with memoized query keys.
3. **TextureView Surface Rendering:** Video streams render on hardware-accelerated TextureView surfaces allowing live 4x5 color matrix filters to apply seamlessly.
4. **Lean APK Footprint:** Vector SVG drawables replace bloated icon libraries; unused dependencies and code are stripped via R8 and resource shrinking.
5. **No Long Code Files:** Modular codebase with dedicated controllers (`ChannelKeypadController`, `SleepTimerManager`, `UpdateController`, `SettingsControls`, `SettingsDialogs`).

---

## 🔒 Security & Privacy

- **No Tracking by Default:** Advertising networks and tracking services are uninitialized and idle unless unlocked and approved by the viewer.
- **Hardware Fallback Protection:** WebView ad render nodes are safely guarded to prevent crashes in headless or emulator environments.
- **Secure Local Storage:** Stored URLs and settings are stripped of private credentials before being written to disk.

---

## 📄 License

Open-source project built for the Kurdish community. Distributed under the terms of the project repository license.
