# Changelog

All notable changes to Kurdish TV Live are recorded here.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and versions follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed
- **A logo tile now shows a neutral placeholder while it loads, instead of the
  channel's monogram.** The monogram is the channel's real fallback identity - it is
  what a channel with no `tvg-logo` is *supposed* to look like - but it was also being
  used as the loading state, so every tile in the grid drew letters and an accent
  gradient that it then tore down the instant the artwork arrived. On a cold start
  that is the whole first screen changing its mind at once, and it reads *slower*
  than a neutral block, because the eye has already read the monogram and then has to
  read the logo again. The load state is now three-valued rather than one boolean:
  a static `LogoSkeleton` in flight, the artwork once it lands, and the monogram only
  when there is genuinely nothing to show - the load failed, or the channel has no
  logo at all.
  - The skeleton does not shimmer. A shimmer is a per-frame shader pass and a render
    node held open for the whole wait, and a placeholder moving while the real content
    sits still beside it would be the one thing on screen contradicting the app's no
    animation rule.
  - **The grid deliberately does not get one.** The bundled catalogue is on screen
    before any load starts and stays there while a refresh runs behind it, so there is
    never blank space for a skeleton to stand in for. Replacing a hundred real cards
    with a hundred grey rectangles would throw away something the viewer can already
    use and make the screen slower, not faster. A card-shaped skeleton was written and
    cut, because nothing could reach it.

- **The focus ring's geometry is now three pure functions, and they have tests.**
  #39's own note records that nothing in this repository covers the UI layer - "the
  focus ring, the player, the D-pad flow" - and the ring is where that gap had already
  cost something, because all three of its layout bugs were invisible without a device.
  `FocusRingGeometryTest` pins the growth, the path box, the promised gap and the
  scrim's reach, using the numbers the app actually ships (3dp stroke, 4dp outset).
  - The scrim's width was wrong in the fix as much as in the original. Centred on the
    ring's path it spans 0-11dp from a card's edge, and the grid's gutter is 14dp, so
    an opaque band that size reaches 4dp into the *neighbouring* card - a visible
    notch rather than a focus ring. It now runs from the element's own edge to the
    ring's outer edge and stops, which is exactly half the gutter.

- **The interface no longer animates. At all.** Every spring, easing curve, frame
  loop and shape morph in the app has been deleted rather than switched off. A
  `snap()` spec still creates an animation, still registers a frame callback and
  still schedules a recomposition for the frame it lands on, so a reduced-motion
  switch would have bought none of the cost back - only the appearance of the
  setting.
  - Gone: `ExpressiveMotion`'s spring table, easing curves, durations and stagger;
    two app-wide `withFrameNanos` loops (the LIVE pulse and the focus rotation);
    `WatchedFloat`, `LocalLivePulse`, `LocalFocusRotation`; `LocalReduceMotion` and
    the two Settings rows that fed it; `staggeredEntrance` and the grid's
    `animateItem` fade; the whole morphing shape layer, under which chips, cards,
    wells, rails and fields all changed outline under the viewer; every press and
    focus scale; `AnimatedVisibility` on the offline banner, the update card, the
    player's transport controls and the buffering panel;
    `animateColorAsState` / `animateDpAsState` on the category chips;
    `animateScrollToItem` in the chip row and the sidebar; the number overlay's
    pop; and the header logo's swell.
  - Kept, because they are feedback rather than decoration: press fills, which
    still switch instantly, because a D-pad press is a *hold* and nothing at all
    happening for a third of a second reads as a dead app; the focus ring, now
    heavier and outset rather than inset over a flat band of the page colour,
    which is what replaces its rotation as the thing that makes focus findable
    from a sofa; and selection states, which switch rather than transition.
  - `BouncingLoader` was a hand-drawn copy of Material's seven-shape morphing
    sequence rather than a spinner, and it is now three static dots. A frozen
    spinner reads as a hung app; an ellipsis reads as working because it always
    did.
  - Renamed to match what they now do: `bouncyClickable` -> `tvClickable`,
    `BouncingLoader` -> `LoadingIndicator`, `SquishyPillButton` ->
    `LabelPillButton`, `rememberMorphingCorners` -> `staticCornerShape`.
  - The **Motion** setting and the **Animated LIVE badge** switch are gone from
    Settings, along with the `motion`, `live_pulse` and `reduceMotion` keys in
    settings storage. Existing installs carrying those keys are unaffected; the
    values are simply no longer read.
  - Removing the motion is what exposed the rest of the focus story, and three
    things in it turned out to be broken or missing. All three are fixed here
    rather than shipped, and none of them reached a released version.
    - **The ring was off-centre and the `outset` did not exist on two of its four
      sides.** `fittedPath` anchors a shape's *top-left* at the origin — it
      translates by `-bounds.left`/`-bounds.top` and then scales, and a Compose
      matrix has no pivot — so the outline spanned `0..ringSize` rather than being
      centred in it. Drawn as it came, the ring's top and left edges landed on the
      element's own top-left corner, eating 1.5dp into the content it was marking,
      while only the bottom and right carried the intended 4dp gap. The ring is now
      shifted back by the growth amount, so the gap is even on all four sides.
    - **The contrast scrim did nothing at all.** It was stroked at the ring's own
      width on the same path, and the ring was drawn immediately after it over the
      top — so six call sites passed a colour and got no band. It is now stroked at
      `2 * outset + stroke`, which fills the gap the outset left and puts the ring
      on the outer part of it.
    - **Two surfaces clipped the ring in half.** The sidebar row and the navigation
      rail item both applied `Modifier.clip` *before* the ring in the chain, which
      removes everything outside the element's bounds — which, for an outset ring,
      is all of it. Neither clip was doing any work: their backgrounds and press
      fills are already shape-aware and their content sits inside their padding.
    - **Six control families had no focus indication whatsoever.** The old springy
      click scaled an element 4% larger on D-pad focus, so removing the scale
      removed the only thing marking where the viewer was, and only the callers that
      had also passed a `pressedFill` got anything back. The Settings back button,
      both Settings row types, the sleep-timer options, every icon button and every
      labelled pill — the player's transport controls, reached by remote, over
      video — showed nothing at all when focused. All six carry a ring again. The
      two full-width Settings rows are the exception: a ring on a `fillMaxWidth()`
      row puts 7dp of accent off each end of the screen, so those are filled rather
      than outlined, which is the idiom every other full-width list here already
      uses.

- **A new colour system, and a flat, quieter way to draw a card.** With the motion
  gone the surfaces had to carry the interface on their own, and the old ramp was
  doing two jobs badly: a four-step neutral ramp for a five-level stack of
  surfaces, and a vertical gradient on every card so a near-black card would not
  read as a hole.
  - The palette is rebuilt on cool neutrals, so the warm accents and the LIVE red
    are the only warm things on screen and an accent reads as an accent.
  - `surfaceHigh` is a new fifth step for the surfaces that sit *on* a card -
    icon buttons, text fields, chips. They were drawn at the card's own level and
    read as part of the card rather than as controls on it.
  - `divider` separates two parts of one surface from a card's outline
    separating it from the page, so the sidebar's edge and the rule under each
    Settings heading are no longer the same weight as a card border.
  - `edgeHighlight` is a 1dp line of light along the inside top of a card, and it
    replaces the per-card `Brush.verticalGradient` outright. The gradient was a
    per-card draw in a grid of several hundred, and it made the top of every card
    look like the front face of something solid. The featured hero keeps an accent
    wash: it is one surface on the page, and the tint is what tells the viewer it
    is a channel rather than a section header.
  - The focus ring's scrim is now the app's own `focusScrim` role rather than
    `background` spelled out at each of the six call sites.

- **Numerals are monospaced, and the type scale is retuned.** This is a television:
  the sidebar numbers every row, the remote's number pad is the fastest way to
  reach a channel, and the number-pad overlay exists to be read from a sofa. In a
  proportional face a `1` is about half the width of an `8`, so a column of channel
  numbers wobbles and the overlay visibly changes width on every keystroke - which
  reads as a digit being replaced rather than as one being added. `ChannelNumber`
  and `NumeralLarge` are fixed-width for that reason and are the only two slots in
  the scale that are. The rest of the scale is retuned a step down and a notch
  tighter, and all-caps Latin markers such as FEATURED and RECENTLY WATCHED now
  share the one `labelSmall` style built for them.
  - Line heights still leave room for stacked vowel marks: Sorani is written with
    diacritics above *and* below the baseline, and a display line height tight
    enough for Latin puts the top and bottom of those marks on the line boundary.
  - Kurdish text still carries no positive letter spacing. Tracking is inserted as
    an extra advance between glyph clusters, and Arabic script is written as
    *connected* clusters, so on several Android releases that advance visibly opens
    the joins.

### Fixed
- **The selected category tab vanished when it had nothing behind it.** The chip
  row's "hide empty tabs" rule and its "keep the tab you are on" rule were written
  as two branches on whether the selected category had a non-zero count, and the
  branch that ran when it did not dropped the tab — so the one rule that existed
  for the empty case was the one case it skipped. Selecting Favourites with no
  favourites, or a category whose channels a refresh had just removed, left the
  grid explaining an empty category while the navigation no longer showed which
  one was open. The rail takes the same list, so it lost the selection too. Both
  now keep the selected tab regardless of its count.
- **Three catalogue streams had gone dead, and one of them was caught by the
  audit's whole reason for existing.** `Al Sharqiya News` fails before its playlist
  is read — the host's TLS certificate expired on 2026-09-29 — `Kurd 7`'s origin
  returns 404 for the playlist path, and `Iraqya Sports` answers 200 on its master
  *and* its variant while every media segment 404s, the exact manifest-stops-short
  trap documented above. None of the three has an alternative in the community
  gist that also carries it or in any public index, so all three were removed
  rather than left holding a slot that can only fail on first play. `Al Sharqiya
  News` is worth re-adding if the broadcaster renews its certificate.
- **The merged channel list was hashed into a map on the main thread.** `loadChannels`
  and the cold-start hydration both built `associateBy { it.id }` — a hash of every
  channel, which runs past a thousand entries once the remote playlists land — on
  the thread that draws the grid, and then read the two preference files through two
  separate `withContext` hops. The hash and the history lookup now run on
  `Dispatchers.Default` beside the filter, and the two file reads share one hop to IO.
- **Three channels could never play, and looked fine.** `BNAR Action` and `Soz Quran`
  return 404 on their master playlist. `Iraqia Kurdish` is worse: its playlist and
  its variant both resolve, and then *every media segment* 404s — so it passes any
  check that stops at the manifest, and still cannot play. All three failed on
  three separate attempts and have been removed rather than left in the grid where
  they can only waste a viewer's time. A 200 on the master was never proof a
  stream works, which is exactly the trap the earlier Channel 8 entry fell into.
- **Three channels took 10-21x longer to start than they needed to.** Rudaw TV,
  Waar TV and Payam TV all pointed at bare origins. Each is also published on the
  Akamai edge, and measured over five requests per URL the edge copies are far
  faster to first playlist:  Rudaw 424 ms -> 42 ms, Waar 813 ms -> 51 ms, Payam
  1027 ms -> 49 ms. Since that wait is exactly the gap between pressing OK and
  seeing video, this is the most visible latency in the app. Payam additionally
  advertises 1080p on the new origin, so it was corrected from 720p at the same
  time. Thirteen of the catalogue's channels now sit on that edge; these three
  had been left on bare origins.
- **The catalogue had no way to detect any of this.** Channel health was asserted
  by hand in a comment, and streams rot silently.

- **One failing source could cost the viewer every remote channel.** The merge
  launched each source with `async` and then collected it with an `await()`
  *inside* the `supervisorScope`. A supervisor stops a failing child from
  cancelling its siblings, but it still rethrows that child's exception once they
  finish - so a single `async` that threw propagated out of the scope, every
  `await()` after it never ran, and the sources that had already loaded were never
  added. The catch below it swallowed the evidence, and the app quietly served the
  98-channel offline catalogue as though that were all there was. A refresh that
  arrived mid-merge, or an edit to a playlist, was enough to trigger it.
  - Each source now goes through one total function that cannot throw, so there is
    nothing left to rethrow and every result is collected. The four built-ins and
    the viewer's own playlists also share that single code path instead of four
    copies of the same block.
- **A cancelled merge used to finish as if it had succeeded.** The catch around it
  was `catch (_: Exception)`, which quietly ate `CancellationException`. A merge
  cancelled halfway did not stop - it ran on to a normal return carrying whatever
  it had collected, which is how a cancellation became a plausible-looking
  short channel list instead of a cancellation. It is now rethrown, which is what
  structured concurrency requires and what makes the real cause visible.
- **The remote sources were never fetched at the same time.** Every source was
  started with `async` and then immediately `await`ed, inside the same loop - so
  the next source was not even launched until the previous one had finished,
  three attempts and a backoff later. Four built-in playlists were fetched one
  after another, and a viewer who had imported twenty was looking at a spinner
  for minutes. All sources are now launched first and awaited afterwards, so the
  slowest one sets the time instead of their sum.
- **A merge could leave the viewer watching a spinner.** With each source allowed
  three attempts at an eight second timeout plus backoff, a merge had no ceiling
  at all. There is now a thirty second deadline across every source at once; past
  it the sources still in flight are cancelled and the ones that already answered
  are kept, so a slow merge degrades to a smaller grid rather than to no grid.

### Added
- **A source that hiccups is now retried instead of written off.** Every remote
  playlist was fetched exactly once per refresh and a failure meant the whole
  source contributed nothing. That is worse than it sounds, because the merged
  result is written to a cache the app then treats as current for thirty minutes
  — so a two-second dropped connection did not cost a request, it cost every
  channel from that playlist for half an hour, and the next cold start served
  the gap from cache without asking again.
  - The population this hurts is exactly the one this app depends on. An audit of
    all 98 catalogue streams found several that only returned media on a second
    attempt, seconds after a first that had failed.
  - Failures are now split by whether retrying can help. A 404 or 403 is the
    source's answer and is not repeated; a 5xx, a 408, a 429, a timeout or a
    dropped connection is retried up to three times.
  - The wait between attempts is exponential with equal jitter. Jitter matters
    more than it looks: all four sources are fetched concurrently and tend to
    fail together, so a backoff without it would have them all retry in lockstep
    against the same origin at the same instant.
- **A partial refresh can no longer overwrite a good cache.** The retry above
  makes a source failing much less likely, but not impossible — and a refresh
  that lost a source used to write the smaller list to disk regardless. Because
  the cache is then treated as current for thirty minutes, that turned a
  two-second network blip into half an hour of missing channels, with no
  subsequent launch even attempting a refetch. A merge that would shrink the
  catalogue no longer replaces it.
  - The trade is accepted knowingly in the other direction: remove a custom
    playlist and its channels linger in the cache until it expires. That is the
    right way round — a few extra channels that stop appearing after one interval
    is a far smaller harm than hundreds that vanish and stay vanished.
  - The rule is a single comparison, which is exactly why it has a test. A
    predicate that small gets "tidied" — inverted, changed to `>` — and nothing
    else in the build would notice.
- **`scripts/audit_streams.py`, and `./gradlew auditStreams`.** Walks every
  catalogue stream the full way down to media bytes — master, then the
  highest-bandwidth variant, then an actual segment — and requires real container
  magic bytes rather than trusting a 200. It also reports time-to-first-playlist,
  which is what surfaced the three slow origins above.
  - It **retries before calling a stream dead.** Probing a live endpoint once
    produces false negatives: on the first run, three streams failed and all three
    passed on a second attempt seconds later, because a live playlist is rewritten
    continuously and a segment listed a moment ago can 404 by the time it is
    fetched. A tool that reported those as dead would have had working channels
    deleted from it. Streams that need a retry are reported separately as
    unreliable-but-alive.
  - Which streams land in that bucket **changes between runs** — successive audits
    flagged different channels each time — so it reflects the network between the
    runner and the origin, not a property of the channel. Read it as noise unless
    the same name repeats.
  - It **refuses to run on a partial parse.** The first version silently missed
    three channels because a comment added between two fields in the catalogue
    source was enough to break a strict pattern, so it reported a clean bill of
    health for channels it never looked at. It now cross-checks the number of
    parsed entries against the number of `id =` fields in the source and aborts
    if they disagree.
- **The remote's number pad works.** Every key on a television remote was ignored
  before, so reaching a channel on a screen of six hundred meant arcing across the
  grid and counting. The digits now build a channel number, shown large in the middle
  of the screen as it is typed, and a second of silence — or OK — acts on it. Numbers
  are positions in the list **on screen**, the same list the sidebar numbers, so a
  number never tunes a channel the viewer has filtered away or cannot see. The
  dedicated CHANNEL UP/DOWN keys, which most remotes carry, now step the selection
  through that list too. Both select rather than open: a number is how you *move*
  through a list, and jumping straight to fullscreen video made every mistyped digit
  a stream the app then had to start and tear down. Backspace edits the number and
  Back cancels it, and with no number up neither is consumed, so the system back
  gesture still leaves the app as it always did.
- **A channel sidebar beside the grid.** The grid is good at finding a channel and
  bad at choosing one — six hundred cards of artwork give no sense of what is in the
  list, what order it is in, or what is ten rows down. The sidebar lists the channels
  of the current category with their number, logo, name and favourite, on a
  television or on a tablet wide enough (1100dp) that the grid does not lose the
  space. It scrolls to keep the selected channel visible, and focusing a row selects
  it, so arrowing down the list browses and the preview pane follows. The number in
  each row is the number the keypad takes, which is what makes pressing `47`
  answerable at a glance.
- **Favouriting from the sidebar is reachable by the remote.** The heart is its own
  D-pad stop, to the right of the name, and is reached with the right arrow. The
  grid card's long-press is a *pointer* gesture — Compose's `clickable` acts on
  key-down, so there is no key-down duration left for a remote to distinguish a long
  press from a tap — which left the favourite unreachable on exactly the surface
  where it is a primary action.
- **The app is on the Android TV home screen.** The manifest declared the leanback
  feature and shipped a D-pad interface, but the launcher intent-filter carried only
  `android.intent.category.LAUNCHER`, so the app never appeared in the TV home
  launcher. On stock Android TV — the common case — that meant the app was effectively
  unlaunchable on the device it is designed for: there is no app drawer listing
  non-TV activities to fall back on. The leanback category is now declared alongside
  the normal one, so a single install serves phones, tablets and televisions, and a
  320x180 banner (`ic_tv_banner`) gives the TV home row a real tile instead of a
  placeholder.
- **TV mode is detected at all.** Nothing in the app read `uiMode`. A TV box and a
  tablet report comparable sizes and both declare the leanback feature, so every
  layout decision the app made — the grid's cell size, the settings column — was
  tuned for a screen held at arm's length and silently applied to a television.
  `LocalIsTv` now carries the answer, and the grid gets a 220dp ten-foot cell instead
  of falling through to the two-column *phone* grid it was being handed.
- **The app now carries Start.io banner ads, starting with one at the foot of
  Settings.** The Android app id (`208672276`) is the only thing tying an installed
  build to the account that gets paid, so it lives in the source beside the
  initialisation it belongs to rather than in a resource or an environment variable —
  repointing the revenue means editing and re-signing an APK, which is the right
  amount of friction for a value that has to be identical in every build.
  - **Test ads are on in debug and off in release.** Serving real inventory while
    developing counts against the account and can get it flagged, which is the
    documented reason the SDK ships the switch. It is wired to `BuildConfig.DEBUG`,
    so a debug build cannot bill and a release build cannot forget to.
  - **The banner is the last item on the Settings screen, below About.** Nothing
    there is ad-funded, so the ad goes where it cannot sit between the viewer and
    something they came to the screen to change.
  - **Nothing is requested until the SDK reports that it is ready.** This is the
    SDK's sharpest edge: a banner requested before initialisation completes is
    dropped with no callback and no error, so a request made at composition time is
    blank on exactly one launch — the cold start, where the screen is guaranteed not
    to be ready yet. The request is keyed on the readiness flag, which is Compose
    state so the screen gets its second chance the moment the callback fires.
  - **No fill draws nothing at all.** The slot is composed only once a banner view
    exists, and a failed impression clears it again. A bordered empty box reads as a
    broken ad and is worse than no ad — and on a television, where Start.io's display
    inventory does not extend, that is the normal outcome rather than an error, so it
    reports through the debug log and nowhere the viewer would see it. The ads are
    expected to be invisible on the primary TV target and to serve phones and tablets
    for now.
  - **The SDK's return ad and splash are disabled**, in the initialiser and in the
    manifest. A return ad is a full-screen surface shown on exit; the app has its own
    navigation and its own update flow, and a second uncontrolled one on the way out
    is not something this product wants.
  - **Permissions and ProGuard rules come from the SDK, not from here.** The AAR's
    manifest already declares `AD_ID`, `ACCESS_ADSERVICES_TOPICS` and the rest, and
    its bundled `proguard.txt` keeps `com.startapp.**` for the release build's R8
    pass, so the app declares neither — repeating them would only create a second
    place to drift.
  - **The SDK is pinned to 5.3.1, not the newest 5.3.2, and both reasons are hard
    build failures rather than warnings.** 5.3.2's AAR metadata requires `compileSdk`
    37, which this project does not use and AGP refuses to build against; it is the
    only release in the catalogue that constrains it. 5.3.2 also declares
    `kotlin-stdlib` 2.4.0 as a plain dependency rather than a range, which outvoted the
    2.2.10 stdlib this project compiles with, and a compiler that reads metadata only
    up to 2.3.0 does not degrade gracefully: it failed every Kotlin file in the app,
    the SDK's own included, behind one "incompatible version of Kotlin" line. 5.3.1
    declares 2.0.0, so the stdlib resolves to the compiler's own version and nothing
    has to be excluded. Re-check both before moving the pin.
  - Initialisation is idempotent, and it cannot take the app down: a device without
    Play services, a missing native library or a rejected app id all degrade to
    "no ads" rather than to a crash on launch.

### Changed
- **R8 was keeping the whole of media3, which is the largest dependency in the app.**
  `proguard-rules.pro` had `-keep class androidx.media3.** { *; }`, and R8 cannot
  remove what a `-keep` protects — so every decoder, extractor, renderer and data
  source for formats this app never plays (DASH, MP3, Ogg, MIDI, image sequences,
  every codec that is not H.264/AAC) went into the APK whatever else the build
  stripped. Media3 ships its own consumer rules covering the component tables it
  resolves reflectively, so the app's rule is now simply absent and the rest of
  media3 is shrinkable. This is the single largest APK-size win available.
- **Two dependencies were on the runtime classpath and never used.** Every glyph in
  the app is a hand-written vector drawable, so `compose.material.icons.core` was
  dead weight; and no `HttpLoggingInterceptor` is constructed anywhere, so
  `logging.interceptor` was dead weight too. Both are commented out with the reason
  rather than deleted, matching how the rest of the dependency block is kept.
- **The channel-logo memory cache was sized for a hundred bitmaps.** The cache holds
  *decoded* images, so a logo at the 512px ceiling is 1 MB; 20% of a large heap is
  far more than the grid ever has on screen, and is memory the OS will not hand to
  anything else while the app is alive. Sized for what is visible plus a screenful
  of scroll-back. The 48 MB *disk* cache is where the long tail belongs and is
  unchanged.
- **Settings stretched across the whole width of a television.** A `fillMaxWidth`
  card behind a 20dp phone gutter put a row's label hard against the left edge and
  its switch hard against the right, with a metre of empty card between them. Cards
  are now capped to a readable column and centred, and the gutter grows on TV.
  Phones and tablets are untouched.
- **Two settings that were already saved were not on the settings screen.** Video fit
  (fit/fill/zoom) and picture correction (the six colour presets) were persisted and
  applied, and the player overlay wrote them, but nothing in Settings surfaced
  either — so a viewer who had not found the player's controls had no idea the
  choice existed, let alone how to undo it. Both are now under Playback.
- **Tapping an accent colour did nothing.** With Material You on, the wallpaper's
  colour wins, so picking “Ember” changed a stored value and not one pixel — which
  reads as a dead control rather than as a conflict. Tapping a swatch is a request
  for that colour, so it now also switches Material You off, and the note above the
  row says so.
- **Three of the fifteen type slots were never defined.** `displaySmall`,
  `headlineSmall` and `bodySmall` fell back to the Material defaults, so the
  featured hero's largest text was the only large text on screen set in the default
  regular weight while everything around it was bold. All fifteen slots now come from
  the app's own scale.
- **The type scale was hostile to Kurdish text.** Line heights were set at Latin
  display ratios — 1.11 for `displayLarge` — which puts the stacked vowel marks of
  Sorani (تەلەفزیۆنی کوردی) on the line boundary, so multi-line Kurdish copy collided
  with the line above it. And positive letter spacing is inserted as an extra
  advance between glyph *clusters*, which on several Android releases lands between
  the joined forms of Arabic script and visibly opens the joins. Display and headline
  slots keep their negative tracking, which is Latin-only; every other slot is now
  untracked.
- **Every logo cell built a second composition.** `SubcomposeAsyncImage` composes a
  subcomposition for its loading and error states, per cell — dozens per screen, on
  every scroll of the grid — to show two lines of monogram that are the same every
  time. The monogram is now drawn underneath and the image painted over it, which is
  the same result with no subcomposition. This is also what the crossfade note above
  always claimed was happening; it was not, because the `loading` slot replaced the
  composable output rather than layering over it.
- **A deep Material You wallpaper accent produced unreadable container text.** The
  on-container colour was lerped towards white, which is right for the five bundled
  accents because they are all hand-picked light tones — but a wallpaper-derived
  primary is not hand-picked, and on the palettes that land on a deep tone the lerp
  produced a mid value the dark container could not separate from. The on-colour is
  now chosen by measured contrast when the accent came from the wallpaper, which is
  the rule the rest of the theme already uses. The bundled accents are untouched.
- **Dead code.** `CategoryFilter` carried a translated name for all twelve entries
  that nothing ever read; the two settings rows and the settings cards also shared a
  hand-rolled chip control that is now the generic one both use.
- **The background update check ran on every single launch.** The manifest request is
  sent with `Cache-Control: no-cache` on purpose, because a stale answer means missing
  a release — which also makes it a guaranteed network round trip every time the app
  was opened, and defeats the HTTP cache besides. A release does not appear and then
  vanish, so the automatic check is now throttled to once every six hours, recorded
  *before* the request so an unreachable server does not make each launch retry it.
  The explicit check in Settings is never throttled.
- **A search allocated four thousand strings per keystroke.** Matching normalised the
  query once but rebuilt every channel's normalised name and category on every keypress
  — two allocations each, over a thousand channels — to answer one question and then
  throw them all away. The per-channel key is now built once per channel list and
  reused, so a keystroke is one substring test per channel and allocates nothing. It
  is held behind a `WeakReference` so the memo does not keep a discarded list alive,
  and the name and category are joined by a separator a query can never contain, so
  `1g` still cannot match across "NRT 1" into "General".
- **Channel name formatting rebuilt every name from scratch.** `formatChannelName`
  flattened separators, collapsed runs of whitespace and trimmed — guaranteeing no
  leading, trailing or repeated space — and then ran `split(" ").filter { … }` and
  `joinToString(" ")` to do that same work again, allocating a list of every word, a
  list of the non-blank ones, and a string to join them, to arrive at the identical
  string. It is now a single substitution, and the eleven canonical spellings are a
  lookup table rather than a chain of string comparisons.
- **Two throwaway lists per playlist line.** `parseTvgLogo` and `parseGroupTitle` used
  `groupValues[1]`, which materialises every group in the match plus the whole matched
  text as element zero, to read one attribute. Both now read the group directly.
- **The channel cache was parsed in full, twice per refresh, to read one integer.**
  The merge asks how many channels are already on disk so it can refuse to
  overwrite a richer list with a smaller one, and it got that number from
  `getCachedChannels()?.size` - which reads the whole file and builds a `Channel`
  for every entry, six hundred of them, to read a count off the end and throw all
  of it away. It now counts the array's elements in a single pass that allocates
  nothing, tracking string and escape state so a `}` inside a channel name cannot
  be mistaken for the end of an entry.
- **A cache stamped in the future was treated as brand new, forever.** The age was
  computed by subtracting the file's timestamp from the clock, and a negative
  result - a device whose clock had been set forward and then corrected - was
  reported as `0`, which is the *youngest* age there is. The app then served that
  list as freshly written on every launch and never asked the network again, and
  nothing in the UI could tell. It is now reported as older than any freshness
  window, so it is refetched. `0` now means one thing only: nothing is cached.
- **Writing the cache could destroy the one it was replacing.** The atomic write
  fell back to delete-then-rename when `renameTo` would not overwrite. Deleting
  first means a second failure - and a full disk is exactly that - left no cache
  at all, which is the outcome the atomic write existed to prevent. It now copies
  over the old file, so the previous list stays readable until the new bytes
  land, and a temp file left behind by a failed write is cleaned up rather than
  sitting there as a second full copy of the catalogue.
- **The whole catalogue was written through one enormous string.** The cache was
  serialised by building a `JSONArray` of several hundred `JSONObject`s, calling
  `toString()` on it and writing the result - so the heap held every object and
  then a second full copy of all of them at once, on a device that may have 32 MB.
  It is now streamed field by field through a buffered writer, with the same
  bytes out.
- **Two quick presses of the heart could lose one of them.** Favourites, watch
  history and playlist links are each a read-modify-write of a whole stored set,
  and nothing held those steps together. Two overlapping toggles read the same
  set and wrote the same answer, so the second was silently dropped while the UI
  reported it had been applied - and a channel watched in the gap between two
  history writes fell out of the history entirely. All three now hold a lock
  across the whole operation.
- **The animation loops ran with the app in the background.** The shared LIVE
  pulse and the focus rotation are `withFrameNanos` loops, which re-arm through
  the Choreographer on every vsync; the Choreographer keeps posting for as long
  as the display is on, whether or not the app is in front of it. They already
  stopped when nothing was reading them and when the viewer had switched motion
  off, but not when the viewer pressed Home - so a box left on another input was
  still waking the CPU sixty times a second for a dot and a ring nobody could
  see. They now stop when the app leaves the foreground, and resume without a
  visible jump because both loops measure their phase from an origin that
  outlives them.
- **Category tests were doing five thousand case-folded comparisons per refresh.**
  `contains(needle, ignoreCase = true)` is not a cheap case-insensitive compare;
  it folds both characters at every position of the haystack. `countsByCategory`
  runs nine of them per channel, so a six-hundred-channel list paid about five
  thousand of those on the thread that draws the category chips, on every change
  to the channel list - which is to say, on every heart tapped. The category
  string is now folded once per channel and compared against needles that were
  folded when the file loaded.
- **A heart tap rebuilt the catalogue on the thread that draws it.** Toggling a
  favourite re-derived three lists over the whole channel list inside the state
  update, which runs on the main thread. The rebuild now happens on a worker and
  only the swap is published, with a guard that recomputes if the list was
  replaced in the meantime so the two answers cannot disagree.

### Fixed
- **The app reported itself online on a network that could not reach anything.**
  `NetworkMonitor` had two different definitions of "online": the callback required
  `NET_CAPABILITY_INTERNET` *and* `NET_CAPABILITY_VALIDATED`, while the startup check
  and `isCurrentlyOnline()` accepted `INTERNET` alone. On a captive network — hotel
  or airport Wi-Fi, or a router that has lost its uplink — the app opened convinced it
  was online, started a four-source fetch against a login portal, and only flipped to
  "offline" once the system's own validation completed. The banner appeared after the
  requests had already failed. Both paths now use one check.
- **A reconnect re-downloaded every source even when the data was seconds old.** The
  offline-to-online transition forced a refresh unconditionally, so a two-second Wi-Fi
  blip in the middle of a film re-fetched all four playlists and the update manifest
  and then overwrote a perfectly current cache with the result. The freshness check
  now decides, as it does everywhere else: recent data is served as-is and only a
  genuinely stale list goes back to the network. As a side effect a flapping network
  can no longer cancel and restart a download on every waver.
- **Every cold start read, parsed and filtered the channel cache twice.** The instant
  path hydrated the UI from the cache, and the load that followed immediately read the
  same file again and filtered the same list to arrive at the same answer. The instant
  path now reports whether the list came from a cache that is still fresh, and when it
  did the second pass is skipped — it was provably a no-op.
- **Logos re-decoded every time the grid scrolled between card sizes.** The Coil
  request's target size was the caller's own *draw* size, which quietly made decode
  resolution a function of the layout: the same channel's logo was a different
  cached bitmap on the compact landscape card than on the full-size one, so moving
  between them decoded the image again instead of hitting the cache — several
  hundred times a screenful, in exactly the gesture the app asks viewers to make
  most. Decode sizes are now bucketed to powers of two, so a grid laying out at
  137dp and one at 141dp share bitmaps, and a logo is never decoded below the size it
  is drawn at.
- **A deep Material You wallpaper accent produced unreadable container text.** The
  on-container colour was lerped towards white, which is right for the five bundled
  accents because they are all hand-picked light tones — but a wallpaper-derived
  primary is not hand-picked, and on the palettes that land on a deep tone the lerp
  produced a mid value the dark container could not separate from. The on-colour is
  now chosen by measured contrast when the accent came from the wallpaper, which is
  the rule the rest of the theme already uses. The bundled accents are untouched.
- **Every logo in the grid animated its own fade-in.** The crossfade was a loader
  default, so scrolling produced a rolling wave of per-frame alpha animations, each
  holding a render node open — the most expensive animation in the app and the one
  nobody asked for. Off; the monogram fallback is already on screen underneath, so a
  logo that simply appears reads as faster, not slower.
- **Every control in the app had two D-pad stops on it.** `bouncyClickable` applied
  `Modifier.focusable` on top of the `clickable` it wraps — but a click already brings
  a focus target of its own, which is the only reason it is reachable by a remote at
  all. So each chip, button, rail item and card had two focus targets stacked on the
  same box, and focus traversal chose between them; the one the viewer landed on was
  not necessarily the one the modifier was watching, so the press lift and the focus
  ring could disagree with where the highlight actually was. The click's own target is
  now the only one.
- **The navigation rail and the category chips disagreed with each other.** The rail
  carried its own hard-coded list of six categories while the chips computed theirs
  from what actually has channels behind it. So the rail offered Music and Kids on a
  playlist that has neither, and left six of the twelve ways of browsing —
  Kurdish Culture, General, Sports, Documentary, Quran, Religious — unreachable from
  the rail on exactly the wide screens where the rail exists. The rail now takes its
  list from the same computation the chips use, which removes the class of bug rather
  than this instance of it.
- **The rail could not be scrolled, so its lower half was unreachable.** It was one
  fixed-height column whose Settings and Import buttons were pinned to the bottom by
  a weighted spacer. With the full category list that overflows a short window, and
  the overflow was silently clipped. The categories scroll; the two actions are a
  fixed footer. Deliberately *not* done by making the one column scrollable, because a
  `weight` inside a scrollable column has no defined size and collapses unpredictably.
- **Two buttons announced the app's name instead of their own action.** The header
  brand tile and the rail's header tile both run a channel refresh, and both described
  themselves as "Kurdish TV Live" — so a screen reader read out the name of the app
  where the viewer needed to hear what pressing it would do. They now say "Refresh
  channels".
- **Deleted dead code.** Three unused loading composables (`ChannelCardSkeleton`,
  `BouncingLoadingState`, `BouncingLoaderRow`), an unused `VectorIcon`, and four
  unused colour constants — about 150 lines that no call site could reach.
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
