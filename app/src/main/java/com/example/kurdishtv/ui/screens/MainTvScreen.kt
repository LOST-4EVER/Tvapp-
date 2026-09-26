package com.example.kurdishtv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
import com.example.kurdishtv.viewmodel.TvUiState
import com.example.ui.theme.DarkBackground

@Composable
fun MainTvScreen(
    uiState: TvUiState,
    onSearchQueryChanged: (String) -> Unit,
    onCategorySelected: (CategoryFilter) -> Unit,
    onChannelClick: (Channel) -> Unit,
    onFavoriteToggle: (String) -> Unit,
    onAddCustomPlaylist: (String) -> Unit,
    onRemoveCustomPlaylist: (String) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showImportDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

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

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isTabletLandscape = maxWidth >= 900.dp
        val isMediumScreen = maxWidth >= 600.dp && maxWidth < 900.dp
        val filtered = uiState.filteredChannels

        if (isTabletLandscape) {
            // Adaptive Split Screen Layout for Tablets & Large Screens
            Row(modifier = Modifier.fillMaxSize()) {
                AdaptiveNavigationRail(
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = onCategorySelected,
                    onOpenImport = { showImportDialog = true },
                    onRefresh = onRetryClick
                )

                // Channels Browsing Column
                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .fillMaxHeight()
                ) {
                    OfflineBanner(
                        isOffline = uiState.isOffline,
                        onRetry = onRetryClick
                    )

                    TopHeaderBar(
                        channelCount = filtered.size,
                        onOpenImport = { showImportDialog = true },
                        onRefresh = onRetryClick
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
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 150.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(
                                items = filtered,
                                key = { _, ch -> ch.id }
                            ) { _, channel ->
                                ChannelCard(
                                    channel = channel,
                                    onClick = { onChannelClick(channel) },
                                    onFavoriteToggle = { onFavoriteToggle(channel.id) }
                                )
                            }
                        }
                    }
                }

                // Live Player Side Supporting Pane
                SidePlayerPane(
                    channel = uiState.selectedChannel ?: filtered.firstOrNull(),
                    onFullscreenClick = onChannelClick,
                    onFavoriteToggle = onFavoriteToggle,
                    modifier = Modifier
                        .weight(0.85f)
                        .fillMaxHeight()
                )
            }
        } else if (isMediumScreen) {
            // Medium Screens (Foldables & Tablets in Portrait)
            Row(modifier = Modifier.fillMaxSize()) {
                AdaptiveNavigationRail(
                    selectedCategory = uiState.selectedCategory,
                    onCategorySelected = onCategorySelected,
                    onOpenImport = { showImportDialog = true },
                    onRefresh = onRetryClick
                )

                Scaffold(
                    containerColor = DarkBackground,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        Column {
                            OfflineBanner(
                                isOffline = uiState.isOffline,
                                onRetry = onRetryClick
                            )

                            TopHeaderBar(
                                channelCount = filtered.size,
                                onOpenImport = { showImportDialog = true },
                                onRefresh = onRetryClick
                            )

                            SearchBarM3(
                                query = uiState.searchQuery,
                                onQueryChange = onSearchQueryChanged
                            )

                            Spacer(modifier = Modifier.height(8.dp))
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
                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 160.dp),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(14.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(
                                    items = filtered,
                                    key = { _, ch -> ch.id }
                                ) { _, channel ->
                                    ChannelCard(
                                        channel = channel,
                                        onClick = { onChannelClick(channel) },
                                        onFavoriteToggle = { onFavoriteToggle(channel.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Mobile Vertical Handheld Design
            Scaffold(
                containerColor = DarkBackground,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    Column {
                        OfflineBanner(
                            isOffline = uiState.isOffline,
                            onRetry = onRetryClick
                        )

                        TopHeaderBar(
                            channelCount = filtered.size,
                            onOpenImport = { showImportDialog = true },
                            onRefresh = onRetryClick
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
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 155.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            val isBrowsingHome =
                                uiState.searchQuery.isBlank() && uiState.selectedCategory == CategoryFilter.ALL
                            val heroChannel = uiState.selectedChannel ?: filtered.firstOrNull()

                            if (isBrowsingHome && heroChannel != null) {
                                item(span = { GridItemSpan(maxLineSpan) }, key = "featured_hero_section") {
                                    FeaturedHeroCard(
                                        channel = heroChannel,
                                        onWatchClick = onChannelClick,
                                        onFavoriteToggle = onFavoriteToggle
                                    )
                                }
                            }

                            if (isBrowsingHome && uiState.recentChannels.isNotEmpty()) {
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
                            ) { _, channel ->
                                ChannelCard(
                                    channel = channel,
                                    onClick = { onChannelClick(channel) },
                                    onFavoriteToggle = { onFavoriteToggle(channel.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
