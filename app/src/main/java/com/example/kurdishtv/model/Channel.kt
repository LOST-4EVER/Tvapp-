package com.example.kurdishtv.model

/**
 * Data model representing a Kurdish TV channel.
 *
 * [id] is the identity used for list keys, favourites and watch history, so it has
 * to be stable across refreshes — it is derived from the source, name and stream
 * URL rather than from the channel's position in the list.
 *
 * [originalId] is the id before duplicate resolution. Two entries for the same
 * stream can arrive from different sources and collide on id; the loser is given
 * a suffixed [id] to keep lazy-layout keys unique, which would otherwise make its
 * stored favourite unreachable. Keeping the pre-suffix value means a favourite
 * still resolves to the channel the user actually tapped.
 */
data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val category: String = "General",
    val quality: String = "HLS / 1080p",
    val isFavorite: Boolean = false,
    val isHd: Boolean = true,
    val originalId: String = id
)
