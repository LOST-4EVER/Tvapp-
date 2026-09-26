package com.example.kurdishtv.viewmodel

import com.example.kurdishtv.model.CategoryFilter
import com.example.kurdishtv.model.Channel

data class TvUiState(
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val channels: List<Channel> = emptyList(),
    val recentChannels: List<Channel> = emptyList(),
    val customPlaylistUrls: Set<String> = emptySet(),
    val selectedCategory: CategoryFilter = CategoryFilter.ALL,
    val searchQuery: String = "",
    val selectedChannel: Channel? = null,
    val sleepTimerMinutes: Int = 0
) {
    val filteredChannels: List<Channel>
        get() {
            return channels.filter { channel ->
                val matchesCategory = when (selectedCategory) {
                    CategoryFilter.ALL -> true
                    CategoryFilter.NEWS -> channel.category.contains("News", ignoreCase = true)
                    CategoryFilter.KURDISH -> channel.category.contains("Kurdish", ignoreCase = true)
                    CategoryFilter.SPORT -> channel.category.contains("Sport", ignoreCase = true)
                    CategoryFilter.KIDS -> channel.category.contains("Kids", ignoreCase = true)
                    CategoryFilter.DOCUMENTARY -> channel.category.contains("Docu", ignoreCase = true)
                    CategoryFilter.QURAN -> channel.category.contains("Quran", ignoreCase = true)
                    CategoryFilter.MUSIC -> channel.category.contains("Music", ignoreCase = true)
                    CategoryFilter.GENERAL -> channel.category.contains("General", ignoreCase = true) || channel.category.contains("Family", ignoreCase = true)
                    CategoryFilter.FAVORITES -> channel.isFavorite
                    CategoryFilter.HD -> channel.isHd
                }

                val matchesSearch = if (searchQuery.isBlank()) {
                    true
                } else {
                    channel.name.contains(searchQuery, ignoreCase = true) ||
                            channel.category.contains(searchQuery, ignoreCase = true)
                }

                matchesCategory && matchesSearch
            }
        }
}
