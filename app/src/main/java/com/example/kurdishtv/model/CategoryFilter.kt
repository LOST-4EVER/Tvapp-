package com.example.kurdishtv.model

enum class CategoryFilter(val displayName: String) {
    ALL("All Channels"),
    NEWS("News"),
    KURDISH("Kurdish Culture"),
    GENERAL("General"),
    MUSIC("Music"),
    KIDS("Kids"),
    SPORT("Sports"),
    DOCUMENTARY("Documentary"),
    QURAN("Quran"),
    /** Channels the catalog files as "Religious" (Zarok, Marjaeyat, Alabbassia...). */
    RELIGIOUS("Religious"),
    FAVORITES("Favorites"),
    HD("HD 1080p")
}
