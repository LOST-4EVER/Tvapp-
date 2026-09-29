package com.example.kurdishtv.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.kurdishtv.model.AppSettings
import com.example.kurdishtv.model.CategoryFilter
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.ui.components.AdaptiveNavigationRail
import com.example.kurdishtv.ui.components.CategoryBar
import com.example.kurdishtv.ui.components.ChannelCard
import com.example.kurdishtv.ui.components.ChannelNumberOverlay
import com.example.kurdishtv.ui.components.ChannelSidebar
import com.example.kurdishtv.ui.components.EmptyChannelState
import com.example.kurdishtv.ui.components.FeaturedHeroCard
import com.example.kurdishtv.ui.components.ImportPlaylistDialog
import com.example.kurdishtv.ui.components.OfflineBanner
import com.example.kurdishtv.ui.components.RecentChannelsRow
import com.example.kurdishtv.ui.components.SearchBarM3
import com.example.kurdishtv.ui.components.SidePlayerPane
import com.example.kurdishtv.ui.components.TopHeaderBar
import com.example.kurdishtv.ui.components.verticalEdgeFade
import com.example.kurdishtv.ui.motion.rememberTvFocusRequester
import com.example.kurdishtv.viewmodel.ChannelJump
import com.example.kurdishtv.viewmodel.TvUiState
import com.example.ui.theme.LocalAppColors
import com.example.ui.theme.LocalIsTv

@Composable
fun MainTvScreen(
    uiState: TvUiState,
    settings: AppSettings,
    settingsLoaded: Boolean,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (CategoryFilter) -> Unit,
    onChannelClick: (Channel) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onAddCustomPlaylist: (String) -> Unit,
    onRemoveCustomPlaylist: (String) -> Unit,
    onRetryClick: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Identity of the navigation entry this screen is showing.
     *
     * The grid keys its focus placement on this, so a viewer who watched a channel and
     * came back lands on the card they left rather than on a grid with nothing focused.
     * See [ChannelGrid].
     */
    focusToken: String = "",
    /**
     * The channel number being typed on the remote, or null when nothing is.
     *
     * Off [uiState] on purpose — it changes on every digit, and a field on the main
     * state would invalidate the whole navigation graph and a grid of several
     * hundred cards to redraw a two-digit readout. See [ChannelJump].
     */
    channelJump: ChannelJump? = null,
    /**
     * A channel the viewer *moved onto*, as opposed to opened.
     *
     * Arrowing down the sidebar changes what is selected — the preview pane follows
     * along — without starting anything. Deliberately separate from [onChannelClick],
     * which opens the player: browsing by highlight and watching a channel are two
     * different intentions and conflating them would start a stream on every arrow
     * press.
     */
    onChannelFocused: (Channel) -> Unit = {},
    onNumericKey: (Int) -> Unit = {},
    /**
     * Steps the selection by [delta] channels — `+1` for the remote's CHANNEL UP.
     *
     * Selects, rather than opens, for the same reason a typed number selects: this
     * is how a viewer browses, and starting a stream on every press of a key that a
     * thumb can hold would be worse than doing nothing.
     */
    onStepChannel: (Int) -> Unit = {},
    onNumericBackspace: () -> Unit = {},
    onNumericCommit: () -> Unit = {},
    onNumericCancel: () -> Unit = {},
    /**
     * Called once an [TvUiState.importMessage] has been shown.
     *
     * The message has to be cleared after it is displayed, and it was never
     * cleared: `LaunchedEffect(uiState.importMessage)` is keyed on the *text*, so
     * a second identical message — importing a second bad link, or a second good
     * one — produced exactly the same string, the key did not change, the effect
     * did not re-run, and the viewer got no confirmation at all for an action
     * they had just taken. The state also stayed set forever, so nothing could
     * tell "shown" from "not yet shown".
     */
    onImportMessageShown: () -> Unit = {}
) {
    val colors = LocalAppColors.current
    val isTv = LocalIsTv.current
    var showImportDialog by remember { mutableStateOf(false) }

    // Whether the search field holds D-pad focus right now.
    //
    // The number pad must not steal digits from a viewer who is typing a search:
    // pressing `4` into the search box has to put a 4 in the search box. Key
    // events reach this screen from the focused node *after* the text field has had
    // its chance at them, which handles most of it, but a field that has not yet
    // taken its first character would still be overwritten — so the state is tracked
    // explicitly rather than relied upon.
    var searchFieldFocused by remember { mutableStateOf(false) }
    var appliedStartCategory by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Which categories the chips decided to show, handed to the navigation rail so
    // both rows of navigation describe the same set. Seeded with every category so a
    // rail that composes before the first `CategoryBar` pass — which is what happens
    // on a wide screen, where the rail precedes the chrome in the tree — is never
    // briefly empty.
    //
    // Typed as `List` rather than left to inference: `CategoryFilter.entries` is an
    // `EnumEntries`, and a `List` of a different length coming back from `CategoryBar`
    // cannot be assigned to it.
    var visibleCategories by remember { mutableStateOf<List<CategoryFilter>>(CategoryFilter.entries) }

    // Apply the preferred landing category once, after stored settings have loaded.
    LaunchedEffect(settingsLoaded, settings.startCategory) {
        if (settingsLoaded && !appliedStartCategory) {
            appliedStartCategory = true
            onCategorySelected(settings.startCategory)
        }
    }

    LaunchedEffect(uiState.importMessage) {
        val msg = uiState.importMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            // Cleared *after* the snackbar has been shown rather than before, so
            // that returning to null cannot race the display. The state now means
            // "there is something new to say" again, which is what makes a repeat
            // of the same message show a second time.
            onImportMessageShown()
        }
    }

    if (showImportDialog) {
        ImportPlaylistDialog(
            customUrls = uiState.customPlaylistUrls,
            onAddUrl = { url ->
                onAddCustomPlaylist(url)
                showImportDialog = false
            },
            onRemoveUrl = onRemoveCustomPlaylist,
            onDismiss = { showImportDialog = false }
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            // The remote's number pad.
            //
            // Every key on a television remote was ignored here before, which is why
            // reaching a channel on a screen of six hundred meant arcing across the
            // grid and counting. Digits build a channel number, and the number is a
            // position in the list on screen — the same list the sidebar numbers.
            //
            // Only Key *Down* is handled. Key Up is the same physical press reported
            // again, and acting on both would enter every digit twice.
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                // A viewer typing a search gets their digits.
                if (searchFieldFocused) return@onKeyEvent false
                when (event.key) {
                    // `NumPad*` is a separate set of key codes on Android, and
                    // several remotes send the keypad codes rather than the main
                    // ones, so both have to be accepted.
                    Key.Zero, Key.NumPad0 -> { onNumericKey(0); true }
                    Key.One, Key.NumPad1 -> { onNumericKey(1); true }
                    Key.Two, Key.NumPad2 -> { onNumericKey(2); true }
                    Key.Three, Key.NumPad3 -> { onNumericKey(3); true }
                    Key.Four, Key.NumPad4 -> { onNumericKey(4); true }
                    Key.Five, Key.NumPad5 -> { onNumericKey(5); true }
                    Key.Six, Key.NumPad6 -> { onNumericKey(6); true }
                    Key.Seven, Key.NumPad7 -> { onNumericKey(7); true }
                    Key.Eight, Key.NumPad8 -> { onNumericKey(8); true }
                    Key.Nine, Key.NumPad9 -> { onNumericKey(9); true }
                    // The dedicated channel keys, which most television remotes
                    // carry and every one of which was ignored. They step the
                    // selection through the list on screen.
                    Key.ChannelUp -> { onStepChannel(1); true }
                    Key.ChannelDown -> { onStepChannel(-1); true }
                    // Commit early rather than waiting out the idle timer. Only while
                    // a number is up, so OK still reaches the card it is on.
                    Key.Enter, Key.NumPadEnter, Key.DirectionCenter ->
                        if (channelJump != null) { onNumericCommit(); true } else false
                    Key.Backspace ->
                        if (channelJump != null) { onNumericBackspace(); true } else false
                    // Back cancels the number, and only the number. With nothing
                    // being typed it is not consumed at all, so the system back
                    // gesture still leaves the app as it always did.
                    Key.Back, Key.Escape ->
                        if (channelJump != null) { onNumericCancel(); true } else false
                    else -> false
                }
            }
    ) {
        val isTabletLandscape = maxWidth >= 900.dp
        val isMediumScreen = maxWidth >= 600.dp
        // Phones in portrait are the tightest case. Shrinking the grid's minimum cell
        // there keeps two comfortable columns instead of squeezing one very wide one,
        // and keeps the header actions from colliding with the title on small screens.
        val isCompactWidth = maxWidth < 600.dp
        // A phone in landscape is wide but short. Judging on width alone put it in
        // the two-pane tablet layout, where the grid was squeezed into a narrow,
        // very tall column. Height has to be part of the decision.
        val isShortLandscape = maxHeight < 480.dp && !isTv
        // The channel sidebar costs 260dp of fixed width that the grid does not get
        // back. On a television there is always room; on a tablet the grid can
        // afford it only once the window is genuinely wide, or the channel names
        // start truncating — which defeats the point of a list.
        val showChannelSidebar = isTv || maxWidth >= 1100.dp
        val gridMinCellSize = when {
            // Ten-foot UI. Every other branch here is sized for a screen held at
            // arm's length: a 132-172dp card is a comfortable cell on a tablet,
            // but across a television it is a thumbnail, and the channel name
            // inside it cannot be read from a sofa.
            //
            // This has to be tested *before* the width rules, because a TV box
            // reports a compact width by phone standards and was being handed
            // the two-column phone grid — the most cramped layout in the app, on
            // the one device with the most pixels to spend.
            isTv -> 220.dp
            isShortLandscape -> 168.dp
            isCompactWidth -> 132.dp
            maxWidth >= 900.dp -> 172.dp
            else -> 150.dp
        }
        val filtered = uiState.filteredChannels
        val isBrowsingHome =
            uiState.searchQuery.isBlank() && uiState.selectedCategory == CategoryFilter.ALL

        when {
            // Short landscape (phone on its side) gets the compact single-pane
            // layout even though it is wide, because vertical space is the scarce
            // resource there.
            isShortLandscape -> {
                LandscapeCompactLayout(
                    uiState = uiState,
                    settings = settings,
                    filtered = filtered,
                    isBrowsingHome = isBrowsingHome,
                    gridMinCellSize = gridMinCellSize,
                    // Shared with the caller: the LaunchedEffect that surfaces
                    // importMessage targets *this* host state, so a locally created
                    // one here would mean nothing is ever shown.
                    snackbarHostState = snackbarHostState,
                    onSearchQueryChanged = onSearchQueryChanged,
                    onCategorySelected = onCategorySelected,
                    onChannelClick = onChannelClick,
                    onFavoriteToggle = onFavoriteToggle,
                    onRetryClick = onRetryClick,
                    onOpenSettings = onOpenSettings,
                    onOpenImport = { showImportDialog = true },
                    onSearchFocusChanged = { searchFieldFocused = it },
                    focusChannelId = channelJump?.target?.id,
                    focusToken = focusToken
                )
            }

            isTabletLandscape -> {
                // This branch has no Scaffold, so the snackbar host is overlaid
                // directly. Without it the import/sync messages were silently
                // dropped on exactly the layout with the most room to show them.
                Box(modifier = Modifier.fillMaxSize()) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        AdaptiveNavigationRail(
                            selectedCategory = uiState.selectedCategory,
                            onCategorySelected = onCategorySelected,
                            visibleCategories = visibleCategories,
                            onOpenImport = { showImportDialog = true },
                            onRefresh = onRetryClick,
                            onOpenSettings = onOpenSettings
                        )

                        // The channel list, beside the grid rather than instead of it.
                        //
                        // Wide enough to be worth the horizontal space, and no wider:
                        // the rail and the sidebar are both fixed, so on a 1000dp
                        // tablet a third column would leave the grid too narrow to
                        // read a channel name in. A television always has the room.
                        if (showChannelSidebar) {
                            ChannelSidebar(
                                channels = filtered,
                                selectedChannelId = uiState.selectedChannel?.id,
                                showLogos = settings.showLogos,
                                onChannelClick = onChannelClick,
                                onChannelFocused = onChannelFocused,
                                onFavoriteToggle = onFavoriteToggle
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1.15f)
                                .fillMaxHeight()
                        ) {
                            TvTopChrome(
                                uiState = uiState,
                                filteredCount = filtered.size,
                                onSearchQueryChanged = onSearchQueryChanged,
                                onCategorySelected = onCategorySelected,
                                onRetryClick = onRetryClick,
                                onOpenSettings = onOpenSettings,
                                onOpenImport = { showImportDialog = true },
                                onVisibleCategories = { visibleCategories = it },
                                onSearchFocusChanged = { searchFieldFocused = it }
                            )

                            if (filtered.isEmpty()) {
                                EmptyChannelState(
                                    searchQuery = uiState.searchQuery,
                                    onReset = {
                                        onSearchQueryChanged("")
                                        onCategorySelected(CategoryFilter.ALL)
                                    }
                                )
                            } else {
                                ChannelGrid(
                                    filtered = filtered,
                                    uiState = uiState,
                                    showLogos = settings.showLogos,
                                    isHome = isBrowsingHome,
                                    minCellSize = gridMinCellSize,
                                    onChannelClick = onChannelClick,
                                    onFavoriteToggle = onFavoriteToggle,
                                    focusToken = focusToken,
                                    focusChannelId = channelJump?.target?.id
                                )
                            }
                        }

                        SidePlayerPane(
                            channel = uiState.selectedChannel ?: filtered.firstOrNull(),
                            onFullscreenClick = onChannelClick,
                            onFavoriteToggle = onFavoriteToggle,
                            // Same picture shape the fullscreen player was set to.
                            resizeMode = settings.resizeMode,
                            modifier = Modifier
                                .weight(0.85f)
                                .fillMaxHeight()
                        )
                    }
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(16.dp)
                    )
                }
            }

            isMediumScreen -> {
                Row(modifier = Modifier.fillMaxSize()) {
                    AdaptiveNavigationRail(
                        selectedCategory = uiState.selectedCategory,
                        onCategorySelected = onCategorySelected,
                        visibleCategories = visibleCategories,
                        onOpenImport = { showImportDialog = true },
                        onRefresh = onRetryClick,
                        onOpenSettings = onOpenSettings
                    )

                    Scaffold(
                        containerColor = colors.background,
                        snackbarHost = { SnackbarHost(snackbarHostState) },
                        topBar = {
                            TvTopChrome(
                                uiState = uiState,
                                filteredCount = filtered.size,
                                onSearchQueryChanged = onSearchQueryChanged,
                                onCategorySelected = onCategorySelected,
                                onRetryClick = onRetryClick,
                                onOpenSettings = onOpenSettings,
                                onOpenImport = { showImportDialog = true },
                                onVisibleCategories = { visibleCategories = it },
                                onSearchFocusChanged = { searchFieldFocused = it }
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) { paddingValues ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                        ) {
                            if (filtered.isEmpty()) {
                                EmptyChannelState(
                                    searchQuery = uiState.searchQuery,
                                    onReset = {
                                        onSearchQueryChanged("")
                                        onCategorySelected(CategoryFilter.ALL)
                                    }
                                )
                            } else {
                                ChannelGrid(
                                    filtered = filtered,
                                    uiState = uiState,
                                    showLogos = settings.showLogos,
                                    isHome = isBrowsingHome,
                                    minCellSize = 160.dp,
                                    onChannelClick = onChannelClick,
                                    onFavoriteToggle = onFavoriteToggle,
                                    focusToken = focusToken,
                                    focusChannelId = channelJump?.target?.id
                                )
                            }
                        }
                    }
                }
            }

            else -> {
                Scaffold(
                    containerColor = colors.background,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TvTopChrome(
                            uiState = uiState,
                            filteredCount = filtered.size,
                            onSearchQueryChanged = onSearchQueryChanged,
                            onCategorySelected = onCategorySelected,
                            onRetryClick = onRetryClick,
                            onOpenSettings = onOpenSettings,
                            onOpenImport = { showImportDialog = true },
                            onVisibleCategories = { visibleCategories = it },
                            onSearchFocusChanged = { searchFieldFocused = it }
                        )
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        if (filtered.isEmpty()) {
                            EmptyChannelState(
                                searchQuery = uiState.searchQuery,
                                onReset = {
                                    onSearchQueryChanged("")
                                    onCategorySelected(CategoryFilter.ALL)
                                }
                            )
                        } else {
                            ChannelGrid(
                                filtered = filtered,
                                uiState = uiState,
                                showLogos = settings.showLogos,
                                isHome = isBrowsingHome,
                                minCellSize = gridMinCellSize,
                                onChannelClick = onChannelClick,
                                onFavoriteToggle = onFavoriteToggle,
                                focusToken = focusToken,
                                focusChannelId = channelJump?.target?.id
                            )
                        }
                    }
                }
            }
        }

        // The number pad's readout, above whichever layout is showing.
        //
        // A sibling of the `when` rather than part of any branch, because the digits
        // have to appear the same way in all four layouts — a viewer who has learned
        // where it is on a television must not have to hunt for it on a tablet.
        channelJump?.let { jump ->
            ChannelNumberOverlay(
                jump = jump,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

/**
 * The pinned chrome above the channel grid: offline state, brand row, search, and
 * the category chips.
 *
 * This is the app's only piece of permanent top-level navigation, and it used to be
 * written out four times — once per layout branch below — with slightly different
 * spacer heights in each. That is how the four layouts drifted apart: a change to the
 * header or the search field had to be made four times and three of them were easy to
 * forget, so a wide tablet and a phone ended up with different vertical rhythm for
 * the same content. One definition, four call sites.
 */
@Composable
private fun TvTopChrome(
    uiState: TvUiState,
    filteredCount: Int,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (CategoryFilter) -> Unit,
    onRetryClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenImport: () -> Unit,
    onVisibleCategories: (List<CategoryFilter>) -> Unit = {},
    onSearchFocusChanged: (Boolean) -> Unit = {},
    compact: Boolean = false
) {
    Column {
        OfflineBanner(isOffline = uiState.isOffline, onRetry = onRetryClick)
        TopHeaderBar(
            channelCount = filteredCount,
            isLoading = uiState.isLoading,
            onOpenImport = onOpenImport,
            onRefresh = onRetryClick,
            onOpenSettings = onOpenSettings,
            compact = compact
        )
        SearchBarM3(
            query = uiState.searchQuery,
            onQueryChange = onSearchQueryChanged,
            onFocusChanged = onSearchFocusChanged
        )
        // No spacer: CategoryBar carries its own vertical content padding, and the
        // three explicit spacers this replaced added a different amount on each
        // layout for no reason.
        CategoryBar(
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = onCategorySelected,
            channels = uiState.channels,
            onVisibleCategories = onVisibleCategories,
            compact = compact
        )
    }
}

/**
 * Single-pane layout for short landscape windows (a phone held sideways).
 *
 * The header, search and category chips share one row so the grid gets the full
 * height of the screen. Losing a whole row to chrome matters here because vertical
 * space is exactly what is scarce in landscape.
 */
@Composable
private fun LandscapeCompactLayout(
    uiState: TvUiState,
    settings: AppSettings,
    filtered: List<Channel>,
    isBrowsingHome: Boolean,
    gridMinCellSize: Dp,
    snackbarHostState: SnackbarHostState,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (CategoryFilter) -> Unit,
    onChannelClick: (Channel) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onRetryClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenImport: () -> Unit,
    onSearchFocusChanged: (Boolean) -> Unit,
    focusChannelId: String?,
    focusToken: String
) {
    val colors = LocalAppColors.current

    Scaffold(
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TvTopChrome(
                uiState = uiState,
                filteredCount = filtered.size,
                onSearchQueryChanged = onSearchQueryChanged,
                onCategorySelected = onCategorySelected,
                onRetryClick = onRetryClick,
                onOpenSettings = onOpenSettings,
                onOpenImport = onOpenImport,
                // Vertical space is the scarce resource in this layout — a phone on
                // its side has around 360dp of it — so the chrome gives some back
                // before the grid ever gets a chance to use it.
                onSearchFocusChanged = onSearchFocusChanged,
                compact = true
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (filtered.isEmpty()) {
                EmptyChannelState(
                    searchQuery = uiState.searchQuery,
                    onReset = {
                        onSearchQueryChanged("")
                        onCategorySelected(CategoryFilter.ALL)
                    }
                )
            } else {
                ChannelGrid(
                    filtered = filtered,
                    uiState = uiState,
                    showLogos = settings.showLogos,
                    isHome = isBrowsingHome,
                    minCellSize = gridMinCellSize,
                    onChannelClick = onChannelClick,
                    onFavoriteToggle = onFavoriteToggle,
                    focusChannelId = focusChannelId,
                    focusToken = focusToken,
                    compact = true
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ChannelGrid(
    filtered: List<Channel>,
    uiState: TvUiState,
    showLogos: Boolean,
    isHome: Boolean,
    minCellSize: Dp,
    onChannelClick: (Channel) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    focusToken: String,
    /**
     * A channel to move D-pad focus to, or null.
     *
     * Set by the remote's number pad. The grid owns the focus requester and the
     * anchor card, so this is the only way the number pad can move the highlight —
     * and the grid is the only thing that knows how to bring a card into a lazy
     * layout and then hand focus to it.
     */
    focusChannelId: String? = null,
    compact: Boolean = false
) {
    // A television is driven by a D-pad, so focus is a primary state rather than a
    // detail: a screen where nothing holds focus is a screen where the remote does
    // nothing, and where the first press of any direction key lands somewhere
    // arbitrary because focus is being picked for the first time.
    val gridState = rememberLazyGridState()
    val gridFocus = rememberTvFocusRequester()
    // Read here rather than passed in: the only thing the grid needs it for is the
    // background the edge fade dissolves into, and that is the page background.
    val colors = LocalAppColors.current

    // Where the viewer's focus was the last time this screen was on show.
    //
    // Saved rather than merely remembered, because the navigator composes one
    // destination at a time: the browse screen is disposed while the player is on top
    // of it, so coming back from a video is a *fresh* composition of this screen and
    // nothing in it survives. Without a record of where the viewer was, they came back
    // to a grid scrolled to their place but with nothing focused, and had to hunt.
    var lastFocusedId by rememberSaveable { mutableStateOf<String?>(null) }

    // The card the [FocusRequester] is currently attached to. Deliberately kept
    // separate from `lastFocusedId`, which is rewritten on every arrow press: binding
    // the requester to that value would rebind it — and so recompose every card on
    // screen — each time the viewer moved. This one changes only when focus has to be
    // placed somewhere new.
    var focusAnchorId by rememberSaveable { mutableStateOf<String?>(null) }

    // The card in this grid that holds focus right now, or null when focus is somewhere
    // else on the screen.
    //
    // Kept because "the results changed" and "the viewer wants focus moved" are not the
    // same event. Typing into the search field changes the results on every keystroke,
    // and a grid that took focus back on the first of them would pull the keyboard out
    // from under the viewer and end the search one letter in.
    var activeCardId by remember { mutableStateOf<String?>(null) }

    // Whether this visit has already handed focus to the grid. Plain state on purpose:
    // it resets when the screen leaves composition, which is exactly the case that has
    // to place focus — the viewer coming back from a video.
    var focusPlaced by remember { mutableStateOf(false) }

    val firstChannelId = filtered.firstOrNull()?.id

    LaunchedEffect(focusToken, firstChannelId) {
        if (firstChannelId == null) return@LaunchedEffect
        val restored = lastFocusedId?.takeIf { id -> filtered.any { it.id == id } }
        // Nothing here has ever been driven by a remote and the grid is scrolled, which
        // means the viewer has been scrolling with a finger. Leave their place alone
        // rather than yanking the list back to the top under them.
        if (restored == null && lastFocusedId == null && gridState.firstVisibleItemIndex > 0) {
            return@LaunchedEffect
        }
        // The fallback is also the recovery path: it fires when the anchored card has
        // left the list entirely — a favourite removed while the Favourites tab is on
        // screen — which is the one case where focus genuinely has nowhere to go.
        //
        // The anchor only moves when the grid is where focus already is, or when this
        // visit has not placed it yet. A card removed out from under a focused grid
        // leaves `activeCardId` naming a card that no longer exists, which still reads
        // as "the grid is in charge" and is exactly the case that has to recover.
        val anchor = restored ?: firstChannelId
        if (!focusPlaced || activeCardId != null) {
            focusAnchorId = anchor
        }
        focusPlaced = true
    }

    // Requested after a frame rather than during composition: the card is not in the
    // tree until the grid has laid it out, and asking for focus before it is attached
    // does nothing at all.
    LaunchedEffect(focusAnchorId) {
        val anchor = focusAnchorId ?: return@LaunchedEffect
        // Getting the anchor into the tree is this effect's problem, not the focus
        // system's: a lazy grid does not compose a card that is scrolled a long way off
        // screen, and `requestFocus` can only target a node that exists. That is the
        // case this guards — the anchor has fallen back to the first card while the
        // list is still scrolled — and it is checked against the scroll position rather
        // than against the outcome of the request, because a request that finds nothing
        // fails silently rather than reporting it.
        val recovering = anchor == firstChannelId && gridState.firstVisibleItemIndex > 0
        if (recovering) {
            runCatching { gridState.scrollToItem(0) }
        }
        withFrameNanos { }
        runCatching { gridFocus.requestFocus() }
    }

    // The remote's number pad, landing here as a channel id.
    //
    // Focus cannot be requested until the card exists: a lazy grid does not compose
    // a row that is scrolled off screen, and `requestFocus` against a node that was
    // never composed does nothing at all — silently. So the order is scroll, wait a
    // frame for the card to be laid out, then re-anchor, and the existing
    // `focusAnchorId` effect does the requesting.
    LaunchedEffect(focusChannelId) {
        val id = focusChannelId ?: return@LaunchedEffect
        val index = filtered.indexOfFirst { it.id == id }
        if (index < 0) return@LaunchedEffect
        lastFocusedId = id
        // This grid is taking focus deliberately, so it must not be treated as a
        // screen that has never placed it and re-anchored somewhere else.
        focusPlaced = true
        runCatching { gridState.scrollToItem(index) }
        withFrameNanos { }
        focusAnchorId = id
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minCellSize),
        state = gridState,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxSize()
            // The grid clips a card wherever the viewport ends, and a card clipped
            // through its logo reads as a rendering fault rather than as a list that
            // continues — a phone screenshot caught exactly that, a row reduced to a
            // floating category chip with the rest of the card sliced off above it.
            // Fading the clipping edge turns the same pixels into "there is more".
            .verticalEdgeFade(
                gridState = gridState,
                background = colors.background
            )
    ) {
        val heroChannel = uiState.selectedChannel ?: filtered.firstOrNull()

        // Recents come first when they exist.
        //
        // The hero was unconditionally above everything, which meant that for anyone
        // who had actually watched something, the first screenful of the app was an
        // advert for a channel they had not asked for, and the thing they *had* asked
        // for — pick up where you left off — was below the fold. Resume is the more
        // likely intent than browse, so it gets the better position; the hero follows
        // as the catalogue's anchor, and is skipped entirely once someone has recents
        // and is browsing, rather than competing with them for the first glance.
        if (isHome && uiState.recentChannels.isNotEmpty()) {
            item(
                span = { GridItemSpan(maxLineSpan) },
                key = "recent_channels_section",
                contentType = "section"
            ) {
                RecentChannelsRow(
                    recentChannels = uiState.recentChannels,
                    onChannelClick = onChannelClick
                )
            }
        }

        if (isHome && heroChannel != null) {
            item(
                span = { GridItemSpan(maxLineSpan) },
                key = "featured_hero_section",
                contentType = "section"
            ) {
                FeaturedHeroCard(
                    channel = heroChannel,
                    showLogos = showLogos,
                    onWatchClick = onChannelClick,
                    onFavoriteToggle = onFavoriteToggle
                )
            }
        }

        itemsIndexed(
            items = filtered,
            key = { _, ch -> ch.id },
            // What this buys, on a list this long.
            //
            // A lazy layout recycles the *composition* of an item as well as its
            // slot, but it will only reuse one for an item of the same
            // content type. Without one, every cell that scrolls into view is
            // composed from scratch and the previous one's state is thrown away —
            // and with six hundred channels on a television that is six hundred
            // composes over a session of arrowing down the list, on a device
            // that is already the slowest thing in the room.
            //
            // The two full-width sections are given their own type so they cannot
            // be handed a channel cell's recycled composition, which is what would
            // otherwise happen the moment one of them scrolled off the top.
            //
            // Two parameters, not one: the indexed overload of itemsIndexed hands
            // its content-type factory the index as well as the item.
            contentType = { _, _ -> "channel" }
        ) { index, channel ->
            ChannelCard(
                channel = channel,
                showLogos = showLogos,
                onClick = { onChannelClick(channel) },
                onFavoriteToggle = { onFavoriteToggle(channel.id) },
                // Exactly one card carries the focus handle: the anchored one. Handing
                // it to every card would put a requester registration on each of
                // several hundred rows for one job, for an outcome that needs exactly
                // one owner.
                focusRequester = if (channel.id == focusAnchorId) gridFocus else null,
                // Recorded so the next visit can put focus back where the viewer left
                // it, and so the grid can tell whether focus is still inside it. Writing
                // to plain state that nothing reads during composition, so moving
                // between cards costs one write and nothing else.
                //
                // The loss is checked against the card that gained it, because a card
                // also reports `false` the first time it is composed — which happens
                // every time one scrolls into view — and that would otherwise clear a
                // flag another card is still holding.
                onFocusChanged = { focused ->
                    if (focused) {
                        lastFocusedId = channel.id
                        activeCardId = channel.id
                    } else if (activeCardId == channel.id) {
                        activeCardId = null
                    }
                },
                compact = compact,
                // No placement animation and no staggered entrance.
                //
                // Both were per-item work on the largest list in the app: an
                // `animateItem` placement spring on every card, plus a delayed
                // `graphicsLayer` fade-and-rise on each one, re-armed on every
                // category change, search keystroke and list swap. On a grid this
                // long that is several hundred animated items at once, which is the
                // case springs are worst at — they retarget mid-flight and the grid
                // never settles. Cards now take their new positions at once, which
                // on a television read across the room is also the *clearer*
                // outcome: the new arrangement is simply there.
                modifier = Modifier
            )
        }
    }
}
