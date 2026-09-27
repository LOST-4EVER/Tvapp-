package com.example.kurdishtv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.kurdishtv.ui.motion.staggeredEntrance
import com.example.kurdishtv.viewmodel.TvUiState
import com.example.ui.theme.LocalAppColors

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
    modifier: Modifier = Modifier
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
                    onSearchQueryChanged = onSearchQueryChanged,
                    onCategorySelected = onCategorySelected,
                    onChannelClick = onChannelClick,
                    onFavoriteToggle = onFavoriteToggle,
                    onRetryClick = onRetryClick,
                    onOpenSettings = onOpenSettings,
                    onOpenImport = { showImportDialog = true }
                )
            }

            isTabletLandscape -> {
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
                        OfflineBanner(isOffline = uiState.isOffline, onRetry = onRetryClick)
                        TopHeaderBar(
                            channelCount = filtered.size,
                            isLoading = uiState.isLoading,
                            onOpenImport = { showImportDialog = true },
                            onRefresh = onRetryClick,
                            onOpenSettings = onOpenSettings
                        )
                        SearchBarM3(
                            query = uiState.searchQuery,
                            onQueryChange = onSearchQueryChanged
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        CategoryBar(
                            selectedCategory = uiState.selectedCategory,
                            onCategorySelected = onCategorySelected
                        )
                        Spacer(modifier = Modifier.height(6.dp))

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
                                onFavoriteToggle = onFavoriteToggle
                            )
                        }
                    }

                    SidePlayerPane(
                        channel = uiState.selectedChannel ?: filtered.firstOrNull(),
                        onFullscreenClick = onChannelClick,
                        onFavoriteToggle = onFavoriteToggle,
                        modifier = Modifier
                            .weight(0.85f)
                            .fillMaxHeight()
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
                            Column {
                                OfflineBanner(isOffline = uiState.isOffline, onRetry = onRetryClick)
                                TopHeaderBar(
                                    channelCount = filtered.size,
                                    isLoading = uiState.isLoading,
                                    onOpenImport = { showImportDialog = true },
                                    onRefresh = onRetryClick,
                                    onOpenSettings = onOpenSettings
                                )
                                SearchBarM3(
                                    query = uiState.searchQuery,
                                    onQueryChange = onSearchQueryChanged
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                CategoryBar(
                                    selectedCategory = uiState.selectedCategory,
                                    onCategorySelected = onCategorySelected
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }
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
                                    onFavoriteToggle = onFavoriteToggle
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
                        Column {
                            OfflineBanner(isOffline = uiState.isOffline, onRetry = onRetryClick)
                            TopHeaderBar(
                                channelCount = filtered.size,
                                isLoading = uiState.isLoading,
                                onOpenImport = { showImportDialog = true },
                                onRefresh = onRetryClick,
                                onOpenSettings = onOpenSettings
                            )
                            SearchBarM3(
                                query = uiState.searchQuery,
                                onQueryChange = onSearchQueryChanged
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            CategoryBar(
                                selectedCategory = uiState.selectedCategory,
                                onCategorySelected = onCategorySelected
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
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
                                onFavoriteToggle = onFavoriteToggle
                            )
                        }
                    }
                }
            }
        }
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
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (CategoryFilter) -> Unit,
    onChannelClick: (Channel) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onRetryClick: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenImport: () -> Unit
) {
    val colors = LocalAppColors.current
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        containerColor = colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                OfflineBanner(isOffline = uiState.isOffline, onRetry = onRetryClick)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TopHeaderBar(
                        channelCount = filtered.size,
                        isLoading = uiState.isLoading,
                        onOpenImport = onOpenImport,
                        onRefresh = onRetryClick,
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier.weight(1f)
                    )
                }
                SearchBarM3(
                    query = uiState.searchQuery,
                    onQueryChange = onSearchQueryChanged
                )
                CategoryBar(
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = onCategorySelected
                )
            }
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
                    onFavoriteToggle = onFavoriteToggle
                )
            }
        }
    }
}

@Composable
private fun ChannelGrid(
    filtered: List<Channel>,
    uiState: TvUiState,
    showLogos: Boolean,
    isHome: Boolean,
    minCellSize: Dp,
    onChannelClick: (Channel) -> Unit,
    onFavoriteToggle: (String) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minCellSize),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        val heroChannel = uiState.selectedChannel ?: filtered.firstOrNull()

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

        if (isHome && uiState.recentChannels.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }, key = "recent_channels_section") {
                RecentChannelsRow(
                    recentChannels = uiState.recentChannels,
                    onChannelClick = onChannelClick
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
                modifier = Modifier.staggeredEntrance(index = index)
            )
        }
    }
}
