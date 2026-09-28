package com.example.kurdishtv.ui.screens

import androidx.compose.animation.core.tween
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.kurdishtv.model.AppSettings
import com.example.kurdishtv.model.CategoryFilter
import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.ui.components.AdaptiveNavigationRail
import com.example.kurdishtv.ui.components.CategoryBar
import com.example.kurdishtv.ui.components.ChannelCard
import com.example.kurdishtv.ui.components.EmptyChannelState
import com.example.kurdishtv.ui.components.FeaturedHeroCard
import com.example.kurdishtv.ui.components.ImportPlaylistDialog
import com.example.kurdishtv.ui.components.OfflineBanner
import com.example.kurdishtv.ui.components.RecentChannelsRow
import com.example.kurdishtv.ui.components.SearchBarM3
import com.example.kurdishtv.ui.components.SidePlayerPane
import com.example.kurdishtv.ui.components.TopHeaderBar
import com.example.kurdishtv.ui.motion.ExpressiveMotion
import com.example.kurdishtv.ui.motion.rememberTvFocusRequester
import com.example.kurdishtv.ui.motion.staggeredEntrance
import com.example.kurdishtv.viewmodel.TvUiState
import com.example.ui.theme.LocalAppColors
import kotlinx.coroutines.delay

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
    focusToken: String = ""
) {
    val colors = LocalAppColors.current
    var showImportDialog by remember { mutableStateOf(false) }
    var appliedStartCategory by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

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
        val isShortLandscape = maxHeight < 480.dp
        val gridMinCellSize = when {
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
                            onOpenImport = { showImportDialog = true },
                            onRefresh = onRetryClick,
                            onOpenSettings = onOpenSettings
                        )

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
                                onOpenImport = { showImportDialog = true }
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
                                    focusToken = focusToken
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
                                onOpenImport = { showImportDialog = true }
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
                                    focusToken = focusToken
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
                            onOpenImport = { showImportDialog = true }
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
                                focusToken = focusToken
                            )
                        }
                    }
                }
            }
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
            onQueryChange = onSearchQueryChanged
        )
        // No spacer: CategoryBar carries its own vertical content padding, and the
        // three explicit spacers this replaced added a different amount on each
        // layout for no reason.
        CategoryBar(
            selectedCategory = uiState.selectedCategory,
            onCategorySelected = onCategorySelected,
            channels = uiState.channels,
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
                    focusToken = focusToken
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
    focusToken: String
) {
    // A television is driven by a D-pad, so focus is a primary state rather than a
    // detail: a screen where nothing holds focus is a screen where the remote does
    // nothing, and where the first press of any direction key lands somewhere
    // arbitrary because focus is being picked for the first time.
    val gridState = rememberLazyGridState()
    val gridFocus = rememberTvFocusRequester()

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

    // The entrance stagger belongs to a new *set* of channels, not to a scroll.
    //
    // A lazy layout recycles item compositions, so an unconditional entrance replayed
    // on every card that scrolled into view: each newly attached card waited out its
    // own delay — up to fourteen steps of it — and then faded in, which is what made
    // the grid look like it was lagging a few hundred milliseconds behind the D-pad.
    // The stagger is armed when the set changes and disarmed once it has had time to
    // play, after which cards that arrive from off screen simply appear.
    var entranceActive by remember { mutableStateOf(true) }
    LaunchedEffect(uiState.selectedCategory, uiState.searchQuery, filtered.size) {
        entranceActive = true
        delay(ExpressiveMotion.STAGGER_MAX_ITEMS * ExpressiveMotion.STAGGER_STEP_MS + 300L)
        entranceActive = false
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minCellSize),
        state = gridState,
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
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
            item(span = { GridItemSpan(maxLineSpan) }, key = "recent_channels_section") {
                RecentChannelsRow(
                    recentChannels = uiState.recentChannels,
                    onChannelClick = onChannelClick
                )
            }
        }

        if (isHome && heroChannel != null) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "featured_hero_section") {
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
            key = { _, ch -> ch.id }
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
                modifier = Modifier
                    // Placement + fade. Without it, changing category or clearing a
                    // search snapped every surviving card to a new slot at once;
                    // with it the grid slides the cards that persist into their new
                    // positions and cross-fades the ones that arrive.
                    .animateItem(
                        fadeInSpec = tween(ExpressiveMotion.DURATION_MEDIUM, easing = ExpressiveMotion.emphasized),
                        placementSpec = ExpressiveMotion.spatialDefaultOffset
                    )
                    .staggeredEntrance(index = index, animate = entranceActive)
            )
        }
    }
}
