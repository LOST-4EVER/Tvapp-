package com.example.kurdishtv.model

import java.lang.ref.WeakReference
import java.text.Normalizer
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
private data class CategoryRule(
    val filter: CategoryFilter,
    val needle: String,
    /**
     * The same text, folded once at construction.
     *
     * Every category test in this file compares against this rather than passing
     * `ignoreCase = true` at the call site. That flag is not a cheap "compare
     * case-insensitively": it is a per-character comparison that folds both sides
     * at every position of the haystack, so a category of thirty characters
     * against one needle is thirty folded comparisons — and
     * [countsByCategory] runs nine of them for every channel in the catalogue.
     * Six hundred channels is five thousand of those, on the thread that draws
     * the chips, every time the channel list changes — which is to say, every
     * time a heart is tapped.
     *
     * Folding the category string once per channel and comparing plainly against
     * a needle that was folded when this file was loaded turns all of that back
     * into one linear scan per needle. The two are not identical in the abstract:
     * per-character folding also equates things like the Kelvin sign with a plain
     * k. For Latin-script category names they agree, and the ambiguity is one
     * this file already resolved the same way in [normalizeQuery].
     */
    val foldedNeedle: String = needle.lowercase(Locale.ROOT)
)

/** Favourites and HD are not substring tests, so they carry no needle. */
private val stringRules: List<CategoryRule> = listOf(
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
private val rulesByCategory: Map<CategoryFilter, List<CategoryRule>> =
    stringRules.groupBy { it.filter }

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
                val categoryText = channel.category.lowercase(Locale.ROOT)
                var matched = false
                for (r in rules.indices) {
                    if (categoryText.contains(rules[r].foldedNeedle)) {
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
     *
     * Memoised by list identity, for the same reason [searchKeysFor] memoises its
     * keys. The single caller keys its `remember` on the channel list itself, and
     * `remember` compares keys with `equals` — which for a list of several hundred
     * data classes is a field-by-field comparison of every one of them, on every
     * recomposition of the category bar. That bar sits in the browse screen's
     * permanent chrome, so this ran on every keystroke of the search field, on a
     * list that had not changed: several hundred nine-field comparisons to answer
     * "is this the same list I already counted".
     *
     * Identity is the right test, not equality. The list is replaced wholesale by
     * a merge, a favourite rebuild or a filter pass, so a *different* list object
     * is exactly the signal that the counts are stale — and a fresh object holding
     * identical channels cannot be distinguished from a changed one by equality
     * without paying for it.
     */
    fun countsByCategory(channels: List<Channel>): Map<CategoryFilter, Int> {
        countCache?.get()?.let { cached ->
            if (cached.source === channels) return cached.counts
        }
        val computed = computeCounts(channels)
        countCache = WeakReference(CountCache(channels, computed))
        return computed
    }

    private class CountCache(
        val source: List<Channel>,
        val counts: Map<CategoryFilter, Int>
    )

    @Volatile
    private var countCache: WeakReference<CountCache>? = null

    private fun computeCounts(channels: List<Channel>): Map<CategoryFilter, Int> {
        val counts = IntArray(CategoryFilter.entries.size)
        counts[CategoryFilter.ALL.ordinal] = channels.size

        for (channel in channels) {
            if (channel.isFavorite) counts[CategoryFilter.FAVORITES.ordinal]++
            if (channel.isHd) counts[CategoryFilter.HD.ordinal]++

            val categoryText = channel.category.lowercase(Locale.ROOT)
            for (i in stringRules.indices) {
                val rule = stringRules[i]
                if (categoryText.contains(rule.foldedNeedle)) {
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
        if (category == CategoryFilter.ALL) emptyList() else rulesByCategory[category].orEmpty()

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
     * addition to the hot path so much as a change of shape within it. What it does
     * *not* fold is deliberate and is now the only thing it leaves out: Latin accents.
     * A Kurdish playlist writes its Latin-script names without them, so `ç` and `c`
     * are not two spellings of one channel here but two different channels, and
     * folding them would make a real channel unfindable by its real name. See
     * [foldForSearch] for the script folding that *is* done, and why.
     */
    private fun normalizeQuery(text: String): String = foldForSearch(text)
}

/**
 * Puts a channel name, a category or a typed query into one canonical shape, so that
 * the three can be compared as plain substrings.
 *
 * The previous version lowercased, then dropped every character that was not a letter
 * or a digit. That is right for a Latin-script playlist and wrong in three specific
 * ways for a *Kurdish* one, and all three fail the same visible way: an empty result
 * grid for a channel that is plainly on screen, which from the sofa is
 * indistinguishable from search being broken.
 *
 *  1. **The letters Kurmanji and Sorani add to Arabic were never folded.** `ڕ`, `ڵ`, `ۆ`,
 *     `ێ` and `ە` are separate codepoints from the plain letters they are written
 *     with, and a keyboard, a transcriber and a playlist author each pick a different
 *     one. A viewer who types `کوردی` with a plain `ر` cannot find a channel stored as
 *     `کوردی` with `ڕ`, even though they are the same word and the viewer would bet
 *     money on which channel they meant. Folding each Kurdish letter onto its base
 *     makes both spellings collapse to one key.
 *  2. **Arabic-Indic digits were not digits at all, to this code.** `١٢` is what a
 *     Kurdish playlist most often writes for a channel number, and `isLetterOrDigit`
 *     said yes, so it survived — as `١٢`, which will never equal an ASCII `12`. The
 *     Extended Arabic-Indic form `۱۲` has the same problem. Both now map onto ASCII, so
 *     a remote's `12` finds a channel numbered `١٢` and vice versa.
 *  3. **Presentation forms were silently deleted.** Arabic has a second set of shaped
 *     letters for end-of-word positions (U+FE70 and onwards), which are combining
 *     marks rather than letters. `isLetterOrDigit` said no to all of them, so a name
 *     written in shaped form lost exactly the letters that made it distinguishable,
 *     and matched nothing. They are folded to their base letters first.
 *
 * Also folded, because they are routine confusions in the same script and cost nothing
 * to treat as one: the alef variants (أ إ آ → ا), ى → ی, ة → ه, and the two Arabic yeh
 * codepoints onto one.
 *
 * The cost is one pass over a short string, plus an NFKC pass only for the rare names
 * that actually contain presentation forms. The per-channel keys are built once and
 * memoised by [searchKeysFor], so this is paid once per channel per list rather than
 * per keystroke, and the query itself is a handful of characters.
 *
 * Internal, and free functions, so the folding can be pinned by a test rather than
 * only checked by typing at it.
 */
internal fun foldForSearch(text: String): String {
    val lower = text.lowercase(Locale.ROOT)

    // The fast path, and it is the overwhelming majority of input: every category
    // string, and most names, are ASCII letters and digits already. Nothing to fold
    // and nothing to strip, so the original string is returned and no builder is
    // allocated at all. This preserves the behaviour the existing code was careful to
    // preserve, on the inputs it was careful about.
    var plainAscii = true
    for (i in lower.indices) {
        val c = lower[i]
        if (c !in 'a'..'z' && c !in '0'..'9') {
            plainAscii = false
            break
        }
    }
    if (plainAscii) return lower

    // Only strings that really contain shaped letters are put through NFKC, so the
    // common Arabic case is not paying for a compatibility decomposition it does not
    // need, and cannot be altered by one.
    val source = if (containsPresentationForm(lower)) {
        Normalizer.normalize(lower, Normalizer.Form.NFKC)
    } else {
        lower
    }

    val out = StringBuilder(source.length)
    for (raw in source) {
        val c = foldSearchChar(raw)
        // Folding produces one non-digit for every digit and a letter for every
        // letter; everything else — separators, spaces, tatweel, harakat — is
        // dropped, as it was before.
        if (c.isLetterOrDigit()) out.append(c)
    }
    return out.toString()
}

private fun containsPresentationForm(text: String): Boolean {
    for (c in text) {
        // Presentation Forms-A (FB50-FDFF) and Arabic Presentation Forms-B
        // (FE70-FEFF). Both are combining marks, not letters.
        if (c.code in 0xFB50..0xFDFF || c.code in 0xFE70..0xFEFF) return true
    }
    return false
}

/**
 * The single-character fold. Kept as a `when` rather than a lookup table because the
 * digit ranges compile to an arithmetic case and the rest to a jump table, and a
 * `Map<Char, Char>` probe per character is exactly the kind of cost the fast path in
 * [foldForSearch] exists to avoid.
 */
private fun foldSearchChar(c: Char): Char = when (c) {
    // Arabic-Indic digits U+0660-0669 and Extended Arabic-Indic U+06F0-06F9.
    in '٠'..'٩' -> '0' + (c - '٠')
    in '۰'..'۹' -> '0' + (c - '۰')

    // The letters Kurdish adds to Arabic, folded onto the plain letter they are
    // written with. This is the whole point of the function.
    'ڕ' -> 'ر' // reh with small v
    'ڵ' -> 'ل' // lam with small v
    'ۆ' -> 'و' // oe
    'ێ' -> 'ی' // yeh with small v
    'ە' -> 'ه' // ae
    'ژ' -> 'ز' // jeh
    'پ' -> 'ب' // peh
    'چ' -> 'ج' // tcheh
    'ڤ' -> 'ف' // veh
    'گ' -> 'ک' // gaf
    'ھ' -> 'ه' // heh doachashmee
    'ډ' -> 'د' // dal with small v
    'ڋ' -> 'د' // dal with three dots below

    // Alef variants, ya/tatweel-free yeh, and ta marbuta. Routine confusions in the
    // same script: a viewer who types ا for أ is not looking for a different channel.
    'أ', 'إ', 'آ', 'ٱ' -> 'ا'
    'ى' -> 'ی' // alef maksura, used as yeh
    'ة' -> 'ه'
    'ؤ' -> 'و'
    'ئ' -> 'ی'

    // Zero-width and joiner characters that a playlist or a copy-paste can carry and
    // that carry no sound. Written as escapes rather than as the characters themselves:
    // an invisible literal in source is unreviewable, and a tool that rewrites the
    // file can silently drop one. Each is mapped to a space, which the `isLetterOrDigit`
    // test below then discards along with every other separator.
    '\u00AD', '\u0640', '\u200B', '\u200C', '\u200D', '\uFEFF' -> ' '

    else -> c
}
