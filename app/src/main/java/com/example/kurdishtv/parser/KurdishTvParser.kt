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
                    val uniqueId = "${sourceTag}_${cleanId(formattedName)}_${abs(url.hashCode())}"

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
        val lines = trimmed.lines().map { it.trim() }.filter { it.isNotEmpty() }

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
                        }
                    }
                }
                (line.startsWith("http://") || line.startsWith("https://")) && !line.startsWith("#") -> {
                    val name = currentExtName ?: "Kurdish Channel ${count + 1}"
                    channels.add(buildChannel(name, line, currentLogo, currentGroup, sourceTag))
                    count++
                    currentExtName = null
                    currentLogo = null
                    currentGroup = null
                }
            }
        }

        val deduplicated = deduplicate(channels)
        return if (deduplicated.isNotEmpty()) deduplicated else KurdishChannelCatalog.getDefaultChannels()
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
        val uniqueId = "${sourceTag}_${cleanId(formattedName)}_${abs(url.hashCode())}"

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
            combined.contains("news") || combined.contains("rudaw") || combined.contains("kurdistan 24") || combined.contains("nrt") || combined.contains("channel 8") || combined.contains("speda") || combined.contains("payam") -> "News"
            combined.contains("music") || combined.contains("korek") || combined.contains("vin") -> "Music"
            combined.contains("kids") || combined.contains("child") || combined.contains("pepule") || combined.contains("zarok") -> "Kids"
            combined.contains("quran") || combined.contains("islam") -> "Quran"
            combined.contains("sport") -> "Sports"
            combined.contains("docu") -> "Documentary"
            combined.contains("kurd") -> "Kurdish"
            else -> "General"
        }
    }

    private fun cleanId(name: String): String =
        name.lowercase(Locale.ROOT).replace(nonAlphaNumericRegex, "").take(16)

    private fun parseExtName(line: String): String {
        val commaIndex = line.lastIndexOf(',')
        return if (commaIndex != -1 && commaIndex < line.length - 1) {
            line.substring(commaIndex + 1).trim()
        } else {
            "Kurdish Channel"
        }
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
