# Changelog

All notable changes to Kurdish TV Live are recorded here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Fixed
- **Search could not find channels whose name contains a separator.** The playlist
  parser rewrites every `-` and `_` in a raw channel name into a space, so a channel
  shown as `NRT 1` is stored under exactly that string. Typing the name the way it is
  written in most community playlists — `nrt-1`, `NRT_1` — matched nothing, and from
  the sofa an empty grid for a channel that is visibly on screen is indistinguishable
  from search being broken. Both the query and the names it is compared against are
  now put into one canonical form, so `nrt-1`, `nrt_1`, `nrt1` and `NRT 1` all find
  the same channel. Accent and script folding are deliberately not attempted.
- **The keyboard's Search key did nothing.** The field had no IME action, so most
  keyboards showed a bare newline that did not submit and did not dismiss. It now
  takes a Search action that clears focus, which is what closes the keyboard, and the
  manifest declares `adjustResize` so the window shrinks on the older releases and TV
  boxes whose IMEs do not report an accurate height.
- **A downloaded update could become uninstallable.** Tapping Install without the
  "allow from this source" permission replaced the update card's state with a
  *failure*, and the card renders its buttons from that state — so the Install button
  disappeared and was replaced by "Retry", which re-runs the update *check* rather
  than the install. The flow was: tap Install, get bounced to Settings, grant the
  permission, return, and find the already-downloaded APK could no longer be installed
  from the app at all. The permission gap is no longer a failure state, so the button
  survives the round trip, and the card now says up front that the permission is
  needed. The permission is also read once per Settings visit instead of on every
  recomposition, which was a `PackageManager` binder call per frame.
- **A running sleep timer recomposed the entire app once a second.** The countdown
  was three fields on the main UI state, and the navigation graph collects that
  state, so a timer invalidated every screen beneath it — a grid of several hundred
  cards included — sixty times a minute, to redraw a badge on a player that was often
  not even the visible screen. It has its own flow now, collected only by the player.
- **Two animations ran at 60fps for the whole life of the app, drawing nothing.** The
  LIVE-badge pulse and the focus-ring turn are `withFrameNanos` loops, and both were
  keyed only on whether the viewer had switched motion *off* — which says nothing
  about whether anything on screen is actually reading the number. So the loops ran
  continuously on the player, where no focus ring is ever drawn, in Settings, which
  has none, and whenever no element held focus at all. A frame callback that wakes the
  CPU sixty times a second for nothing is pure battery, and it is the kind of bug that
  is invisible in a screenshot. Both clocks are now demand-driven: a `WatchedFloat`
  carries a count of its readers, badges and focused rings register while they are
  composed, and the loop exists exactly while something is drawing from it.
- **Visiting the browse screen on a tablet downloaded a live stream to a black box.**
  The preview pane is paused by design, but a *prepared* ExoPlayer is not idle — it
  opens the connection and fills its buffer whether or not a frame is ever shown, so
  merely arriving at the screen pulled down a whole live channel. The player now
  defers loading until playback is asked for on that pane, and releases the
  connection outright when it is paused rather than fetching into the background.
  The fullscreen player still pre-loads, which is what makes its first frames appear
  quickly.
- **The player's error screen swallowed the remote's first press.** The full-screen
  "channel unavailable" overlay was a `clickable`, which makes it the largest and
  first focus target on the player — and it sits above the controls. A viewer whose
  stream had just failed pressed OK to retry and the press was eaten by an invisible
  scrim that only revealed the controls, so they had to press a second time to do
  what they had asked for. It cannot simply be marked unfocusable, because that
  applies to every focus target below it and Retry sits below it; it now uses the
  pointer-handler `tapOnly` the transport scrim already uses, which creates no focus
  target at all.
- **A phone on its side showed barely one row of channels.** The logo well is square
  and as wide as its cell, so it set the whole card height, and at full size a single
  row plus a sliver was all that fitted — worse than the portrait layout it is meant
  to accompany. The card now has a compact form for short windows, shrinking the well
  and tightening the spacing to buy back a second row while keeping the name and
  category line.
- **The buffer held 50 seconds of a live stream.** Anything that far ahead of a live
  playhead cannot be played "late" anyway, so it was memory and data spent on a
  rebuffer that may never come. Down to 30 seconds, still well above what the
  15-second floor needs to start playback.
- **Every search keystroke and every step of the D-pad across the category rail
  allocated a list.** `rulesFor` was `StringRules.filter { ... }` — a fresh list per
  call, in the two places the app most needs to be cheap. Now a lookup, indexed once.
- **The player's picture shape was forgotten.** It was `rememberSaveable`,
  which survives a rotation and nothing else: every cold start, every return visit
  and the small side player pane silently reset the picture to Fit, so the button
  had to be pressed again each time. The resize mode and the video colour filter
  are preferences now — persisted with the rest of the settings, read back by the
  fullscreen player and applied to the side pane too, which hard-coded Fit and
  contradicted the setting the viewer had just made.
- **A slow category switch could overwrite a newer one.** The guard that was
  meant to discard stale results was inverted: it skipped the publish when nothing
  had moved, and published when it had — so a slow pass for the tab you left could
  land after the tab you chose and repaint the grid with the old one. The
  selection is now applied immediately and the result only when it is still the
  newest request.
- **The channels list was audited rather than assumed.** All 101 catalogue streams
  were probed live: every one returns HTTP 200 with a real HLS manifest, and there
  are no duplicate ids, names or stream URLs, and no category filed under a filter
  nothing matches. The per-card colour comes from a stable hash of the channel's
  name over a fixed eight-colour palette; simulated against the catalogue it
  distributes 6–17 channels per colour, with no clustering.
- **The app did not compile.** The previous pass added a `focusToken` argument to four
  `ChannelGrid` call sites and to `FeaturedHeroCard`, which does not take one, and three
  of the calls were missing the comma before it. `FeaturedHeroCard` no longer receives
  an argument it never had, and the grid calls are well formed. The same pass also fed
  the navigator's entry id — a UUID `String` — into the grid's `Int` token, defaulted
  with `?: 0`, which is a type error as well; the token is a `String` end to end now.
  The same pass also called `androidx.compose.runtime.awaitFrame`, which does not
  exist, and used `combinedClickable` without the `@OptIn(ExperimentalFoundationApi::class)
  the API still requires. All four are fixed, and the release build compiles.
- **Typing in the search field threw focus out of it.** Focus placement was keyed on
  the channel list, so the filter narrowing on the first keystroke moved focus to a
  card and dismissed the keyboard, ending the search after one letter. The grid now
  takes focus only when it already had it, or when nothing has placed it yet this
  visit — the viewer returning from a video.
- **Focus left the grid when it should not have.** Where the viewer's focus was is now
  saved rather than only remembered, so returning from the player or from Settings puts
  focus back on the card they were on. It used to be re-requested against the *first*
  card whenever the channel list changed at all, which yanked the grid back to the top
  on every favourite toggle.
- **The focus ring never lit for a card the grid focused.** A `focusRequester` binds to
  the first focus target below it, and it was declared *before* the card's click — which
  brings a focus target of its own — so it bound to a target the ring was not watching.
  It now sits after the click, where the only target below it is the ring's.
- **The card favourite button was still a D-pad stop.** `Modifier.clickable` brings a
  focus target of its own, so `bouncyClickable(focusable = false)` left the heart in the
  tab order anyway: one press of the right arrow landed on the heart and a second was
  needed to reach the next channel. A non-focusable clickable is now genuinely
  unfocusable, and focus falls through to the focus ring underneath it.
- **A settings row with a switch had two D-pad stops**, because `Switch` is focusable by
  design and the row itself already toggles it. The switch is now the row's indicator
  rather than a second control.
- **The player's scrim swallowed the remote.** The full-screen tap target was a
  `clickable`, which made it the largest and first focus target on the player — the
  first press of any direction key went to an invisible surface instead of to Back or
  Play/Pause. It is a pointer handler now, so it is tappable and not focusable at all.
- **Updates could never install.** CI generated a brand-new signing key on every
  run because the committed key was missing, so each release was signed
  differently and Android rejected every update with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`
  ("App not installed as package conflicts with an existing package"). The key is
  now committed and the build fails loudly rather than silently generating a new
  one. See [SIGNING.md](SIGNING.md).
- **Header drew under the status bar and swallowed taps.** The activity was
  edge-to-edge but no screen consumed the insets, so the top of the screen was
  partly unreachable. Insets are now applied per route, excluding the player so
  fullscreen video is not letterboxed.
- **Recently-watched history was silently corrupted.** Channel ids can contain
  commas, which the comma-delimited storage split in half. History is now stored
  as a JSON array and the old format is migrated on first read.
- **Religious channels were unreachable.** Zarok, Marjaeyat and Alabbassia were
  filed under a category that no filter matched, so they could only be found by
  searching. A **Religious** filter now exists and the channels are filed under it.
- **Zarok was miscategorised as a children's channel** by the parser, because the
  kids rule matched on the channel name before the religious rules ran.

### Added
- **In-app updates.** Settings → App update checks for a newer build, shows its
  size and release notes, downloads it with a progress bar, and hands it to the
  system installer. A check also runs quietly at launch.
- **D-pad and TV remote support.** The app is a TV app but had no focus handling
  at all, and `bouncyClickable` suppresses the default ripple — so on a TV box
  nothing showed where focus was. Every interactive element is now focusable and
  draws an expressive focus ring.
- **Leanback support declared** in the manifest, with `touchscreen` marked
  optional so the app installs on TV boxes.
- New **Quran** and **Religious** category icons.
- The grid remembers the card the viewer was on and returns focus there; the entrance
  animation only plays for a new set of channels rather than replaying on every card
  that scrolls into view.
- Settings → About now explains the Play Protect warning and how to recover from
  a stale-signature install.

### Changed
- The updater now reads the **GitHub releases API** (the repository is public) and
  falls back to a static manifest, so a rate-limited API response is never
  reported to the user as "you are up to date".
- Channel logos decode at the size they are actually drawn at rather than at full
  source resolution, which was the largest memory cost in the channel grid.
- Networking gained transparent gzip, a larger HTTP cache, longer connection
  keep-alive, and a smaller dispatcher pool so image loads cannot starve playlist
  fetches.
- R8 can shrink the app again: the previous ProGuard rules kept every class and
  member under `com.example.**`, which defeated minification entirely.
- The shared LIVE pulse and the shared focus-ring rotation are published as state read
  during drawing, not as plain numbers read during composition. As numbers they
  invalidated every reader on every frame — with a badge and a ring on every card in a
  grid of several hundred, that was several hundred recompositions per frame to animate
  an 8dp dot. Nothing recomposes for either of them now.
- The pulse and the ring are driven by their own frame loops instead of
  `rememberInfiniteTransition`s, and neither loop runs at all while motion is switched
  off. The transitions ran regardless and their output was discarded.
- The LIVE dot scales with a canvas transform rather than a `graphicsLayer`, and the
  header logo and the empty-state entrance only attach a layer while they are actually
  animating. Three more render nodes per screen are gone.
- The focus ring reuses one `Stroke` and one scratch `Path` instead of allocating both
  on every frame.
- Whole-catalogue filtering during cold-start hydration and playlist merges moved off
  the main thread, the playlist parser splits its input lazily instead of copying it
  three times, and logo loads no longer build a redundant per-request crossfade.

## [1.0.21]

### Added
- Material 3 Expressive shape library: the full shape catalogue built from
  procedural polar-radius geometry, so shapes scale from a chip to a hero tile.
- A bouncing loading indicator that morphs between expressive shapes, replacing
  the plain spinner.
- 34 new Iraq and Kurdistan channels; every stream and logo was verified live
  before being added.

### Fixed
- Dead stream and logo URLs removed or replaced.
- Cold start no longer refetches every playlist when the cache is fresh.

## [1.0.16]

### Fixed
- Local builds could never be installed over a published release.
- A single failing playlist no longer discarded the channels that did load.

[Unreleased]: https://github.com/LOST-4EVER/Tvapp-/compare/v1.0.21...HEAD
[1.0.21]: https://github.com/LOST-4EVER/Tvapp-/releases/tag/v1.0.21
[1.0.16]: https://github.com/LOST-4EVER/Tvapp-/releases/tag/v1.0.16
