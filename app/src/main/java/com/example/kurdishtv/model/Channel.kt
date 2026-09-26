package com.example.kurdishtv.model

/**
 * Data model representing a Kurdish TV channel.
 */
data class Channel(
    val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val category: String = "General",
    val quality: String = "HLS / 1080p",
    val isFavorite: Boolean = false,
    val isHd: Boolean = true
)
