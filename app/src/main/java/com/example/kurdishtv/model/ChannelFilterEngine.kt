package com.example.kurdishtv.model

import java.util.Locale

/**
 * Which channels a category is asking for, and how to recognise one.
 *
 * Most categories are a substring test against the channel's own category string, so
 * the test is data rather than code. Hoisting it out of the `when` means a category
 * can be added without touching [filter], and it means [countsByCategory] can test
 * one channel against every category without re-reading its category string once per
 * category.
 */
private data class CategoryRule(val filter: CategoryFilter, val needle: String)

/** Favourites and HD are not substring tests, so they carry no needle. */
private val StringRules: List<CategoryRule> = listOf(
    CategoryRule(CategoryFilter.NEWS, "News"),
    CategoryRule(CategoryFilter.KURDISH, "Kurdish"),
    CategoryRule(CategoryFilter.GENERAL, "General"),
    CategoryRule(CategoryFilter.MUSIC, "Music"),
    CategoryRule(CategoryFilter.KIDS, "Kids"),
    CategoryRule(CategoryFilter.SPORT, "Sport"),
    CategoryRule(CategoryFilter.DOCUMENTARY, "Docu"),
    CategoryRule(CategoryFilter.QURAN, "Quran"),
    CategoryRule(CategoryFilter.RELIGIOUS, "Relig")
)

/**
 * The same rules, indexed by the category they belong to.
 *
 * Every category has at most one rule today, so this maps to single-element lists —
 * but it is a lookup rather than a scan precisely so that adding a second rule to a
 * category later does not silently change the meaning of the existing one.
 */
private val RulesByCategory: Map<CategoryFilter, List<CategoryRule>> =
    StringRules.groupBy { it.filter }

object ChannelFilterEngine {

    fun filter(
        channels: List<Channel>,
        category: CategoryFilter,
        query: String
    ): List<Channel> {
        val cleanQuery = normalizeQuery(query)
        val isQueryEmpty = cleanQuery.isEmpty()

        if (isQueryEmpty && category == CategoryFilter.ALL) {
            // The overwhelmingly common case on a cold start: return the list as it
            // is rather than copying several hundred entries to say "all of them".
            return channels
        }

        val rules = rulesFor(category)
        val needsFlags = category == CategoryFilter.FAVORITES || category == CategoryFilter.HD

        val result = ArrayList<Channel>(if (isQueryEmpty) channels.size else 16)
        for (channel in channels) {
            if (needsFlags) {
                val byFlag = when (category) {
                    CategoryFilter.FAVORITES -> channel.isFavorite
                    else -> channel.isHd
                }
                if (!byFlag) continue
            } else if (rules.isNotEmpty()) {
                val categoryText = channel.category
                var matched = false
                for (i in rules.indices) {
                    if (categoryText.contains(rules[i].needle, ignoreCase = true)) {
                        matched = true
                        break
                    }
                }
                if (!matched) continue
            }

            if (isQueryEmpty || matchesQuery(channel, cleanQuery)) {
                result.add(channel)
            }
        }
        return result
    }

    /**
     * How many channels each category would show, in one pass over the list.
     *
     * The category chips need this for every category at once — to decide which tabs
     * have anything behind them — and it used to get it by calling [filter] twelve
     * times. Twelve passes over the whole catalogue meant twelve reads of every
     * channel and twelve list allocations, and it ran on every change to the channel
     * list, which includes every favourite toggle. One pass, one set of counters.
     *
     * `ALL` is the size of the list, so it is filled in without being tested.
     */
    fun countsByCategory(channels: List<Channel>): Map<CategoryFilter, Int> {
        val counts = IntArray(CategoryFilter.entries.size)
        counts[CategoryFilter.ALL.ordinal] = channels.size

        for (channel in channels) {
            if (channel.isFavorite) counts[CategoryFilter.FAVORITES.ordinal]++
            if (channel.isHd) counts[CategoryFilter.HD.ordinal]++

            val categoryText = channel.category
            for (i in StringRules.indices) {
                val rule = StringRules[i]
                if (categoryText.contains(rule.needle, ignoreCase = true)) {
                    counts[rule.filter.ordinal]++
                }
            }
        }

        val result = HashMap<CategoryFilter, Int>(counts.size)
        for (filter in CategoryFilter.entries) {
            result[filter] = counts[filter.ordinal]
        }
        return result
    }

    /**
     * The substring tests a category implies, or an empty list when it implies
     * everything.
     *
     * A lookup rather than a scan. This used to be
     * `StringRules.filter { it.filter == category }`, which allocated a new list on
     * *every* filter call — and `filter` runs on every keystroke of the search field
     * and on every step of the D-pad across the category rail, so the allocation was
     * pure garbage in the two places the app most needs to be cheap. The index is built
     * once, from a list that never changes after this file is loaded.
     */
    private fun rulesFor(category: CategoryFilter): List<CategoryRule> =
        if (category == CategoryFilter.ALL) emptyList() else RulesByCategory[category].orEmpty()

    /**
     * Case-insensitive name-or-category match, without allocating.
     *
     * This used to be `channel.name.lowercase(Locale.ROOT).contains(cleanQuery)`, which
     * copies both the name and the category into freshly allocated lowercase strings
     * for every channel on every keystroke — over a thousand short-lived strings per
     * search, on the main thread, for the length of the search. Kotlin's
     * `contains(other, ignoreCase = true)` is the same test without the copies.
     */
    private fun matchesQuery(channel: Channel, cleanQuery: String): Boolean =
        normalizeQuery(channel.name).contains(cleanQuery) ||
            normalizeQuery(channel.category).contains(cleanQuery)

    /**
     * Puts a name or a query into one canonical shape so the two can be compared.
     *
     * A channel shown as `NRT 1` is stored under exactly that string, because
     * [KurdishTvParser.formatChannelName] rewrites every `-` and `_` in a playlist's
     * raw name into a space. A viewer who typed the name they know from somewhere
     * else — `nrt-1`, `NRT_1` — got an empty result set for a channel that was
     * plainly on screen, which from the sofa is indistinguishable from search being
     * broken.
     *
     * Applied to **both** sides, and separators are dropped rather than turned into
     * spaces: that way `nrt-1`, `nrt_1` and `nrt1` all match the same channel, and
     * `NRT 1` does too. Normalising only the query would fix the hyphen and leave
     * every spaced name unmatchable from an unspaced query, which is the commoner
     * direction — people type `nrt1`.
     *
     * The per-channel cost is one pass over a short string, and it replaces the
     * `ignoreCase` comparisons this used to do twice per channel, so it is not an
     * addition to the hot path so much as a change of shape within it. Accent
     * folding and script folding are deliberately *not* attempted: they are a larger
     * claim than this test can honestly make, and a much larger cost per keystroke.
     */
    private fun normalizeQuery(text: String): String =
        text.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }
}
