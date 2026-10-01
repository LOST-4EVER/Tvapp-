package com.example.kurdishtv.parser

import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.model.KurdishChannelCatalog
import com.example.kurdishtv.model.foldForSearch
import org.json.JSONArray
import java.util.Locale
import kotlin.math.abs

object KurdishTvParser {

    /**
     * Kept ASCII on purpose, and paired with the hash fallback in [cleanId].
     *
     * The obvious fix — allowing letters through `[^a-z0-9]` — was tried and is wrong
     * for a different reason than it first appears: these ids become `LazyVerticalGrid`
     * keys, `testTag` fragments and `SharedPreferences` values. Arabic-script names
     * folded in fine, but a name written with a character that a shell, a log or an
     * XML attribute mangles produced an id that did not survive the round trip, and
     * the grid threw on the duplicate key. Keeping the id ASCII and carrying the
     * distinguishing information in a stable hash avoids that entirely.
     */
    private val nonAlphaNumericRegex = "[^a-z0-9]".toRegex()
    private val whitespaceRegex = "\\s+".toRegex()
    private val tvgLogoRegex = """tvg-logo="([^"]+)"""".toRegex(RegexOption.IGNORE_CASE)
    private val groupTitleRegex = """group-title="([^"]+)"""".toRegex(RegexOption.IGNORE_CASE)

    /** A run of non-space characters in an already-trimmed, already-collapsed name. */
    private val wordRegex = "[^ ]+".toRegex()

    /**
     * Words the playlists carry in whatever case, and the spelling they are shown in.
     *
     * Data rather than a `when` in [formatChannelName]: the mapping never changes at
     * runtime, so a lookup table is both faster (a hash probe instead of a linear
     * chain of string equals) and impossible to get out of step with the list of
     * words that are meant to be canonicalised.
     */
    private val canonicalWords: Map<String, String> = mapOf(
        "tv" to "TV",
        "hd" to "HD",
        "4k" to "4K",
        "sd" to "SD",
        "nrt" to "NRT",
        "ktv" to "KTV",
        "ava" to "AVA",
        "trt" to "TRT",
        "kurdistan24" to "Kurdistan 24",
        "kurdsat" to "KurdSat",
        "kurdmax" to "KurdMax"
    )

    fun parseJson(content: String, sourceTag: String = "json"): List<Channel> {
        val channels = mutableListOf<Channel>()
        try {
            val array = JSONArray(content.trim())
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                val rawName = obj.optString("name", "Kurdish Channel").trim()
                val url = obj.optString("url", "").trim()
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    val rawCategory = obj.optString("category", "General")
                    val logo = obj.optString("logo").ifEmpty { obj.optString("thumbnail") }.takeIf { it.isNotBlank() }

                    val formattedName = formatChannelName(rawName)
                    val category = mapCategory(rawCategory, formattedName)
                    val isHd = formattedName.contains("HD", ignoreCase = true) || url.contains("1080", ignoreCase = true)
                    val quality = if (isHd) "HLS / 1080p" else "HLS / 720p"
                    // Stable id derived from the source, name and stream URL (no index) so
                    // favourites/recents survive source reordering between refreshes.
                    val uniqueId = "${sourceTag}_${cleanId(formattedName)}_${stableHash(url)}"

                    channels.add(
                        Channel(
                            id = uniqueId,
                            name = formattedName,
                            streamUrl = url,
                            logoUrl = logo,
                            category = category,
                            quality = quality,
                            isHd = isHd
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return deduplicate(channels)
    }

    fun parse(content: String, sourceTag: String = "m3u"): List<Channel> {
        val trimmed = content.trim()
        if (trimmed.startsWith("[")) {
            val jsonChannels = parseJson(trimmed, sourceTag)
            if (jsonChannels.isNotEmpty()) return jsonChannels
        }

        val channels = mutableListOf<Channel>()
        // A lazy sequence rather than `lines().map { }.filter { }`. Those three calls
        // materialise three full copies of the playlist — and the largest of the merged
        // sources is a few thousand lines — to hand this loop one line at a time either
        // way, so the copies bought nothing but garbage on the IO thread.
        val lines = trimmed.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }

        var currentExtName: String? = null
        var currentLogo: String? = null
        var currentGroup: String? = null
        var count = 0

        for (line in lines) {
            when {
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    currentExtName = parseExtName(line)
                    currentLogo = parseTvgLogo(line)
                    currentGroup = parseGroupTitle(line)
                }
                // "name;url" line format. Only treat a line as such when it is not a raw
                // stream URL itself (URLs may legitimately contain ';' in query params).
                line.contains(";") && !line.startsWith("#") &&
                        !line.startsWith("http://") && !line.startsWith("https://") -> {
                    val parts = line.split(";", limit = 2)
                    if (parts.size == 2) {
                        val rawName = parts[0].trim()
                        val url = parts[1].trim()
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            channels.add(buildChannel(rawName, url, null, null, sourceTag))
                            count++
                            // Consume the pending #EXTINF so the next bare URL line
                            // cannot inherit this entry's name and logo.
                            currentExtName = null
                            currentLogo = null
                            currentGroup = null
                        }
                    }
                }
                line.startsWith("http://") || line.startsWith("https://") -> {
                    val name = currentExtName ?: "Kurdish Channel ${count + 1}"
                    channels.add(buildChannel(name, line, currentLogo, currentGroup, sourceTag))
                    count++
                    currentExtName = null
                    currentLogo = null
                    currentGroup = null
                }
            }
        }

        // Note: deliberately no fallback to the bundled catalog here. A source that
        // returns nothing usable is a problem with that source, not a reason to inject
        // 35 unrelated channels into the results. TvRepository seeds the catalog once
        // at the top level, before any source is parsed.
        return deduplicate(channels)
    }

    /**
     * Normalises a playlist's raw name into the form the app displays and searches.
     *
     * The three steps are ordered so that each one makes the next one cheaper: the
     * separators are flattened to single spaces, the runs are collapsed, and the
     * ends are trimmed — after which the string is guaranteed to contain no leading,
     * trailing or repeated space.
     *
     * That guarantee is what lets the word pass be a single regex substitution.
     * It used to be `split(" ").filter { it.isNotBlank() }.joinToString(" ")`, which
     * re-did the work the collapse and trim had already done: it allocated a list of
     * every word in the name, another list of the non-blank ones, and a string to
     * join them back together — to arrive at the identical string. This runs once
     * per channel per source, and the largest merged playlists run to a few thousand
     * channels, so that was several thousand throwaway lists on every refresh.
     */
    fun formatChannelName(rawName: String): String {
        val flattened = rawName
            .replace("-", " ")
            .replace("_", " ")
            .replace(whitespaceRegex, " ")
            .trim()

        if (flattened.isEmpty()) return "Kurdish Channel"

        val clean = wordRegex.replace(flattened) { match ->
            val word = match.value
            canonicalWords[word.lowercase(Locale.ROOT)]
                ?: word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
        return clean.ifBlank { "Kurdish Channel" }
    }

    private fun buildChannel(
        rawName: String,
        url: String,
        logoUrl: String?,
        groupTitle: String?,
        sourceTag: String
    ): Channel {
        val formattedName = formatChannelName(rawName)
        val category = mapCategory(groupTitle ?: "", formattedName)
        val isHd = formattedName.contains("HD", ignoreCase = true) || url.contains("1080", ignoreCase = true)
        val quality = if (isHd) "HLS / 1080p" else "HLS / 720p"
        // Stable id: no positional index, so the same stream keeps its identity across fetches.
        val uniqueId = "${sourceTag}_${cleanId(formattedName)}_${stableHash(url)}"

        return Channel(
            id = uniqueId,
            name = formattedName,
            streamUrl = url,
            logoUrl = logoUrl,
            category = category,
            quality = quality,
            isHd = isHd
        )
    }

    /**
     * The same category words, written in Kurdish script.
     *
     * Every needle in [mapCategory] is Latin, and a channel whose `group-title` is
     * absent or generic and whose *name* is in Kurdish script matched none of them and
     * fell through to `else` — landing in **General**. That is not a rare fallback:
     * the channels most likely to be named in Kurdish script are the ones that belong
     * in the Kurdish tab, so the tab those channels were missing from was the one
     * guaranteed to be full of everything else.
     *
     * Tested against [foldForSearch]'s output rather than the raw string, so that
     * `کوردی` (Sorani yeh) and `کوردي` (Arabic yeh) are recognised as the same word
     * here exactly as they are in search. Note that fold maps Kurdish *letter
     * variants* — ڕ ڵ ۆ ێ ە گ چ پ ژ ڤ ھ ډ ڋ — onto their plain forms; it does not
     * transliterate Arabic script into Latin, so these needles have to be written in
     * the script they appear in. A single transliteration table would be the tidier
     * design, but a wrong transliteration silently misfiles live channels, whereas an
     * unrecognised word here only leaves a channel where it already was.
     *
     * Each entry is a whole word rather than a fragment, and the set is deliberately
     * short: only words whose meaning is not in doubt in either dialect.
     */
    private val scriptNeedles: Map<String, String> = mapOf(
        "Sports" to "وەرزش",
        "Quran" to "قورئان",
        "Religious" to "ئایینی",
        "News" to "هەواڵ",
        "Music" to "گۆرانی",
        "Kids" to "منداڵان",
        "Documentary" to "دۆکیومەنتاری",
        "Kurdish" to "کورد"
    )

    /**
     * [scriptNeedles] with every needle folded once, at class-init, by the same
     * function [mapCategory] folds the haystack with.
     *
     * Folding here rather than at each call is the difference between one probe and
     * eight folds per channel: this runs once per parsed entry across every source on
     * every refresh, so several thousand times.
     */
    private val foldedNeedles: Map<String, String> =
        scriptNeedles.mapValues { (_, needle) -> foldForSearch(needle) }

    private fun mapCategory(rawGroup: String, channelName: String): String {
        val combined = "$rawGroup $channelName".lowercase(Locale.ROOT)
        // Folded, not lowercased: this is what collapses the two spellings of one
        // Kurdish word onto each other and strips the separators, so a needle below
        // matches the middle of a longer phrase.
        val folded = foldForSearch(combined)

        fun inScript(category: String): Boolean {
            val needle = foldedNeedles[category] ?: return false
            return folded.contains(needle)
        }

        return when {
            // Sports is checked first: several sports channels carry "Kurdistan"
            // or a news-style group title, and the broad Kurdish/news tests below
            // would otherwise swallow them, leaving the Sports tab empty.
            combined.contains("sport") || combined.contains("football") ||
                combined.contains("soccer") || combined.contains("tennis") ||
                combined.contains("basket") || inScript("Sports") -> "Sports"
            // Religious before Kids: several religious channels are grouped under
            // a kids/family group title in the community playlists, and were being
            // filed as children's channels.
            combined.contains("quran") || combined.contains("islam") ||
                inScript("Quran") -> "Quran"
            combined.contains("zarok") || combined.contains("religious") ||
                combined.contains("hussain") || combined.contains("marjaeyat") ||
                combined.contains("abbassia") || combined.contains("mahdi") ||
                combined.contains("sajjad") || combined.contains("imam") ||
                combined.contains("karbala") || inScript("Religious") -> "Religious"
            combined.contains("news") || combined.contains("rudaw") ||
                combined.contains("kurdistan 24") || combined.contains("nrt") ||
                combined.contains("channel 8") || combined.contains("speda") ||
                combined.contains("payam") || inScript("News") -> "News"
            combined.contains("music") || combined.contains("korek") ||
                combined.contains("vin") || inScript("Music") -> "Music"
            combined.contains("kids") || combined.contains("child") ||
                combined.contains("pepule") || inScript("Kids") -> "Kids"
            combined.contains("docu") || inScript("Documentary") -> "Documentary"
            // Last, as it always has been — the broad test, so anything more specific
            // above gets first refusal. `کوردستان` contains `کورد`, so the single
            // Kurdish needle covers the whole family of Kurdish-script names.
            combined.contains("kurd") || inScript("Kurdish") -> "Kurdish"
            else -> "General"
        }
    }

    /**
     * The name component of a channel id.
     *
     * A display name in Latin script contributes itself, lowercased and stripped of
     * punctuation — the long-standing behaviour, and unchanged for every channel that
     * already had a readable id, which is what keeps existing favourites and recents
     * resolvable.
     *
     * A name in another script contributes nothing: `[^a-z0-9]` strips every one of
     * its letters, so the id came out as `krd__2847193` — a name-shaped hole in the
     * middle of the id. It was still *unique*, because the URL hash that follows it
     * came from the stream and not from the name, so this never produced a collision.
     * What it did produce was an id that carries no information about the channel in
     * any human-readable part, which is what makes a duplicate-id bug in the merge
     * impossible to spot from a log.
     *
     * So the name part falls back to a stable hash of the name itself. The id stays
     * the same length and the same shape, a Latin channel's id is bit-for-bit what it
     * was, and an Arabic-script channel gets an id that at least distinguishes
     * *Kurdistan TV* from *NRT* without having to look the whole id up.
     */
    private fun cleanId(name: String): String {
        val ascii = name.lowercase(Locale.ROOT).replace(nonAlphaNumericRegex, "")
        if (ascii.isNotEmpty()) return ascii.take(16)
        // Folded first, so the two spellings of one Kurdish word do not produce two
        // different ids for one channel.
        val foldedName = foldForSearch(name)
        return if (foldedName.isNotEmpty()) {
            "n" + stableHash(foldedName)
        } else {
            // A name with no letters and no digits in any script. Nothing to derive
            // from, so fall back to the empty-safe literal the old regex produced,
            // and let the URL hash — which always follows — carry the uniqueness.
            "n"
        }
    }

    /**
     * A deterministic hash of the stream URL.
     *
     * `String.hashCode()` is specified by the Kotlin/Java API, so it is stable
     * across runs and devices. This wrapper exists so the id derivation is
     * obvious at the call sites and so the value is always non-negative, which
     * keeps ids free of a leading minus sign.
     */
    private fun stableHash(value: String): String {
        val hash = abs(value.hashCode())
        // Zero is a legal hash but a poor discriminator; fold it to 1.
        return if (hash == 0) "1" else hash.toString()
    }

    /**
     * Extracts the display name from an `#EXTINF:` line.
     *
     * The separator is the first comma that is *not* inside a quoted attribute value,
     * not the last one. Group titles routinely contain commas (`group-title="News,
     * Sport"`) and so do display names (`Kurdistan TV, HD`), so splitting on the last
     * comma — the previous behaviour — turned the first into an empty name and the
     * second into just "HD".
     */
    private fun parseExtName(line: String): String {
        var inQuotes = false
        for (i in line.indices) {
            val c = line[i]
            if (c == '"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                return line.substring(i + 1).trim().ifBlank { "Kurdish Channel" }
            }
        }
        return "Kurdish Channel"
    }

    /**
     * The two attribute readers use `groups[1].value` rather than
     * `groupValues[1]`.
     *
     * `groupValues` builds a `List<String>` of *every* group in the match — and with
     * it the entire matched text as element zero — to hand back one string. That is
     * two throwaway lists for every `#EXTINF:` line in every playlist, several
     * thousand times per refresh, to read one attribute. Reading the group directly
     * allocates nothing but the value.
     */
    private fun parseTvgLogo(line: String): String? =
        tvgLogoRegex.find(line)?.groups[1]?.value?.trim()?.takeIf { it.isNotBlank() }

    private fun parseGroupTitle(line: String): String? =
        groupTitleRegex.find(line)?.groups[1]?.value?.trim()?.takeIf { it.isNotBlank() }

    fun getFallbackChannels(): List<Channel> = KurdishChannelCatalog.getDefaultChannels()

    /**
     * Drops channels that are the same stream reached by a different spelling of the
     * same URL.
     *
     * The comparison used to be on the raw string, so it only caught an *exact*
     * repeat. Real playlists do not repeat exactly:
     *
     *  - `http://` and `https://` for one stream, which is common when two
     *    community playlists both carry the same channel;
     *  - a trailing slash, which some CDNs treat as a distinct object;
     *  - a fragment, which is never sent to the server at all.
     *
     * Each of those is one channel shown twice in the grid, each with its own id, its
     * own logo request and its own heart — a favourite tapped on one of them does
     * nothing to the other. [normalizeStreamUrl] produces the comparison key; the
     * channel's own [Channel.streamUrl] is left exactly as the playlist published it,
     * because that is the URL that has to be handed to the player and rewriting it
     * would be a change of behaviour rather than a de-duplication.
     *
     * First occurrence wins, so the order of the merged list is preserved and the
     * result is deterministic.
     */
    fun deduplicate(channels: List<Channel>): List<Channel> {
        val seenUrls = mutableSetOf<String>()
        val result = mutableListOf<Channel>()
        for (channel in channels) {
            if (channel.streamUrl.isNotBlank() &&
                seenUrls.add(normalizeStreamUrl(channel.streamUrl))
            ) {
                result.add(channel)
            }
        }
        return result
    }
}

/**
 * The comparison key for [KurdishTvParser.deduplicate]: two URLs that address the
 * same stream produce the same string.
 *
 * Only the parts that genuinely cannot change which object is fetched are folded:
 * the scheme, the host's case, a default port, the fragment and a trailing slash.
 * The path and the query are left alone — case-sensitive, and two paths that differ
 * only by case are two different objects on most servers, so folding them would merge
 * channels that are not the same.
 *
 * Free and pure so it can be pinned by a test; see `ParserUrlTest`.
 */
internal fun normalizeStreamUrl(url: String): String {
    var value = url.trim()
    if (value.isEmpty()) return value

    // Fragment. Never transmitted, so it cannot select a different stream.
    val hash = value.indexOf('#')
    if (hash >= 0) value = value.substring(0, hash)

    val schemeEnd = value.indexOf("://")
    if (schemeEnd > 0) {
        val scheme = value.substring(0, schemeEnd).lowercase(Locale.ROOT)
        val rest = value.substring(schemeEnd + 3)
        val authorityEnd = rest.indexOfFirst { it == '/' || it == '?' }
        val authority = if (authorityEnd < 0) rest else rest.substring(0, authorityEnd)
        val tail = if (authorityEnd < 0) "" else rest.substring(authorityEnd)

        // A default port is the same origin written longer. An IPv6 literal is
        // bracketed, so only a colon *after* the closing bracket is a port separator —
        // anything else would cut a bare address like `2001:db8::1` in half.
        val closingBracket = authority.lastIndexOf(']')
        val colon = authority.lastIndexOf(':')
        val hostAndPort = if (colon > closingBracket) {
            val host = authority.substring(0, colon).lowercase(Locale.ROOT)
            val port = authority.substring(colon + 1)
            if (port.toIntOrNull() == if (scheme == "https") 443 else 80) host else "$host:$port"
        } else {
            authority.lowercase(Locale.ROOT)
        }

        value = "$scheme://$hostAndPort$tail"
    }

    // A trailing slash on the path. Trimmed only from the path portion, so a query
    // value that legitimately ends in one is untouched.
    val queryStart = value.indexOf('?')
    if (queryStart < 0) {
        value = value.trimEnd('/')
    } else {
        val path = value.substring(0, queryStart).trimEnd('/')
        value = path + value.substring(queryStart)
    }

    // Never let trimming turn a bare scheme into something unparseable: `http://`
    // with both slashes removed is not a URL.
    return if (value.endsWith(":/") || value.endsWith(":")) "$value/" else value
}
