package com.example.kurdishtv.model

enum class CategoryFilter(
    val displayName: String,
    val kurdishName: String
) {
    ALL("All Channels", "هەموو"),
    NEWS("News", "هەواڵ"),
    KURDISH("Kurdish Culture", "کوردی"),
    GENERAL("General", "گشتی"),
    MUSIC("Music", "مۆسیقا"),
    KIDS("Kids", "منداڵان"),
    SPORT("Sports", "وەرزش"),
    DOCUMENTARY("Documentary", "بەڵگەفیلم"),
    QURAN("Quran", "قورئان"),
    /** Channels the catalog files as "Religious" (Zarok, Marjaeyat, Alabbassia...). */
    RELIGIOUS("Religious", "ئایینی"),
    FAVORITES("Favorites", "دڵخوازەکان"),
    HD("HD 1080p", "کوالێتی بەرز")
}
