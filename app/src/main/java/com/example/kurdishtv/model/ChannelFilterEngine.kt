package com.example.kurdishtv.model

import java.util.Locale

object ChannelFilterEngine {

    fun filter(
        channels: List<Channel>,
        category: CategoryFilter,
        query: String
    ): List<Channel> {
        val cleanQuery = query.trim().lowercase(Locale.ROOT)
        val isQueryEmpty = cleanQuery.isEmpty()

        return channels.filter { channel ->
            val matchesCategory = when (category) {
                CategoryFilter.ALL -> true
                CategoryFilter.NEWS -> channel.category.contains("News", ignoreCase = true)
                CategoryFilter.KURDISH -> channel.category.contains("Kurdish", ignoreCase = true)
                CategoryFilter.GENERAL -> channel.category.contains("General", ignoreCase = true)
                CategoryFilter.MUSIC -> channel.category.contains("Music", ignoreCase = true)
                CategoryFilter.KIDS -> channel.category.contains("Kids", ignoreCase = true)
                CategoryFilter.SPORT -> channel.category.contains("Sport", ignoreCase = true)
                CategoryFilter.DOCUMENTARY -> channel.category.contains("Docu", ignoreCase = true)
                CategoryFilter.QURAN -> channel.category.contains("Quran", ignoreCase = true)
                CategoryFilter.RELIGIOUS -> channel.category.contains("Relig", ignoreCase = true)
                CategoryFilter.FAVORITES -> channel.isFavorite
                CategoryFilter.HD -> channel.isHd
            }

            val matchesSearch = if (isQueryEmpty) {
                true
            } else {
                channel.name.lowercase(Locale.ROOT).contains(cleanQuery) ||
                        channel.category.lowercase(Locale.ROOT).contains(cleanQuery)
            }

            matchesCategory && matchesSearch
        }
    }
}
