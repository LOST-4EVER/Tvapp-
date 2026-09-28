package com.example.kurdishtv.model

import java.lang.ref.WeakReference
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

        // Built once per distinct channel list and reused by every keystroke after
        // the first. Null when the query is empty, because then nothing is tested.
        val keys = if (isQueryEmpty) null else searchKeysFor(channels)

        val result = ArrayList<Channel>(if (isQueryEmpty) channels.size else 16)
        for (i in channels.indices) {
            val channel = channels[i]
            if (needsFlags) {
                val byFlag = when (category) {
                    CategoryFilter.FAVORITES -> channel.isFavorite
                    else -> channel.isHd
                }
                if (!byFlag) continue
            } else if (rules.isNotEmpty()) {
                val categoryText = channel.category
                var matched = false
                for (r in rules.indices) {
                    if (categoryText.contains(rules[r].needle, ignoreCase = true)) {
                        matched = true
                        break
                    }
                }
                if (!matched) continue
            }

            if (keys == null || keys[i].contains(cleanQuery)) {
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
     * The pre-normalised search keys for a channel list, built once and reused.
     *
     * A search is one substring test per channel against the *channel's own*
     * normalised name and category. Producing that string per channel per keystroke
     * cost two allocations for every channel on every keypress — a name and a
     * category, each lowercased and then filtered — so a search across a merged
     * list of a thousand channels churned four thousand short-lived strings to
     * answer one question, and then threw all of them away.
     *
     * The key is built once per distinct list instead, so the only work a keystroke
     * does is the substring test itself. It changes when the list changes, which is
     * the only time it can: a new merge produces a new list, and identity is
     * therefore the whole test.
     *
     * Held through a [WeakReference] so the memo does not become the thing that
     * keeps a discarded channel list — and its few hundred channels — alive for the
     * life of the process.
     */
    private class SearchKeyCache(
        val source: List<Channel>,
        val keys: Array<String>
    )

    @Volatile
    private var keyCache: WeakReference<SearchKeyCache>? = null

    private fun searchKeysFor(channels: List<Channel>): Array<String> {
        keyCache?.get()?.let { cached ->
            if (cached.source === channels) return cached.keys
        }
        val built = Array(channels.size) { i ->
            val name = normalizeQuery(channels[i].name)
            val category = normalizeQuery(channels[i].category)
            // A separator that `normalizeQuery` can never produce, so a query can
            // never match across the join — without it, a query like "1g" would
            // match "NRT 1" followed by "General".
            if (category.isEmpty()) name else "$name$SEARCH_KEY_SEPARATOR$category"
        }
        keyCache = WeakReference(SearchKeyCache(channels, built))
        return built
    }

    private const val SEARCH_KEY_SEPARATOR = '/'

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
    private fun normalizeQuery(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        // The overwhelming majority of names and categories are already free of
        // separators, so scan first and only build a string when something has to
        // come out. `filter` allocated unconditionally.
        var needsRebuild = false
        for (i in lower.indices) {
            if (!lower[i].isLetterOrDigit()) {
                needsRebuild = true
                break
            }
        }
        if (!needsRebuild) return lower

        val out = StringBuilder(lower.length)
        for (c in lower) {
            if (c.isLetterOrDigit()) out.append(c)
        }
        return out.toString()
    }
}
