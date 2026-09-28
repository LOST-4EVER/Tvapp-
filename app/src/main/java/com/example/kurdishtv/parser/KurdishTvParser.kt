package com.example.kurdishtv.parser

import com.example.kurdishtv.model.Channel
import com.example.kurdishtv.model.KurdishChannelCatalog
import org.json.JSONArray
import java.util.Locale
import kotlin.math.abs

object KurdishTvParser {

    private val nonAlphaNumericRegex = "[^a-z0-9]".toRegex()
    private val whitespaceRegex = "\\s+".toRegex()
    private val tvgLogoRegex = """tvg-logo="([^"]+)"""".toRegex(RegexOption.IGNORE_CASE)
    private val groupTitleRegex = """group-title="([^"]+)"""".toRegex(RegexOption.IGNORE_CASE)

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

    fun formatChannelName(rawName: String): String {
        var clean = rawName
            .replace("-", " ")
            .replace("_", " ")
            .replace(whitespaceRegex, " ")
            .trim()

        clean = clean.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
            when (word.lowercase(Locale.ROOT)) {
                "tv" -> "TV"
                "hd" -> "HD"
                "4k" -> "4K"
                "sd" -> "SD"
                "nrt" -> "NRT"
                "ktv" -> "KTV"
                "ava" -> "AVA"
                "trt" -> "TRT"
                "kurdistan24" -> "Kurdistan 24"
                "kurdsat" -> "KurdSat"
                "kurdmax" -> "KurdMax"
                else -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
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

    private fun mapCategory(rawGroup: String, channelName: String): String {
        val combined = "$rawGroup $channelName".lowercase(Locale.ROOT)
        return when {
            // Sports is checked first: several sports channels carry "Kurdistan"
            // or a news-style group title, and the broad Kurdish/news tests below
            // would otherwise swallow them, leaving the Sports tab empty.
            combined.contains("sport") || combined.contains("football") ||
                combined.contains("soccer") || combined.contains("tennis") ||
                combined.contains("basket") -> "Sports"
            // Religious before Kids: several religious channels are grouped under
            // a kids/family group title in the community playlists, and were being
            // filed as children's channels.
            combined.contains("quran") || combined.contains("islam") -> "Quran"
            combined.contains("zarok") || combined.contains("religious") ||
                combined.contains("hussain") || combined.contains("marjaeyat") ||
                combined.contains("abbassia") || combined.contains("mahdi") ||
                combined.contains("sajjad") || combined.contains("imam") ||
                combined.contains("karbala") -> "Religious"
            combined.contains("news") || combined.contains("rudaw") ||
                combined.contains("kurdistan 24") || combined.contains("nrt") ||
                combined.contains("channel 8") || combined.contains("speda") ||
                combined.contains("payam") -> "News"
            combined.contains("music") || combined.contains("korek") ||
                combined.contains("vin") -> "Music"
            combined.contains("kids") || combined.contains("child") ||
                combined.contains("pepule") -> "Kids"
            combined.contains("docu") -> "Documentary"
            combined.contains("kurd") -> "Kurdish"
            else -> "General"
        }
    }

    private fun cleanId(name: String): String =
        name.lowercase(Locale.ROOT).replace(nonAlphaNumericRegex, "").take(16)

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

    private fun parseTvgLogo(line: String): String? =
        tvgLogoRegex.find(line)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }

    private fun parseGroupTitle(line: String): String? =
        groupTitleRegex.find(line)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }

    fun getFallbackChannels(): List<Channel> = KurdishChannelCatalog.getDefaultChannels()

    fun deduplicate(channels: List<Channel>): List<Channel> {
        val seenUrls = mutableSetOf<String>()
        val result = mutableListOf<Channel>()
        for (channel in channels) {
            if (channel.streamUrl.isNotBlank() && seenUrls.add(channel.streamUrl)) {
                result.add(channel)
            }
        }
        return result
    }
}
