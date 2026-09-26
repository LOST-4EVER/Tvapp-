package com.example.kurdishtv.parser

import com.example.kurdishtv.model.Channel
import org.json.JSONArray
import java.util.Locale

object KurdishTvParser {

    private val nonAlphaNumericRegex = "[^a-z0-9]".toRegex()
    private val tvgLogoRegex = """tvg-logo="([^"]+)"""".toRegex(RegexOption.IGNORE_CASE)
    private val groupTitleRegex = """group-title="([^"]+)"""".toRegex(RegexOption.IGNORE_CASE)

    /**
     * Parses JSON playlist content from endpoints such as:
     * https://gist.githubusercontent.com/RekoPlany/58da74c17bd9fdccda3033c77710a7f0/raw/channels.json
     */
    fun parseJson(content: String): List<Channel> {
        val channels = mutableListOf<Channel>()
        try {
            val array = JSONArray(content)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val rawName = obj.optString("name", "Kurdish Channel")
                val url = obj.optString("url", "").trim()
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    val rawCategory = obj.optString("category", "General")
                    val logo = obj.optString("logo").ifEmpty { obj.optString("thumbnail") }.takeIf { it.isNotBlank() }
                    val rawId = obj.optString("id", "").ifEmpty { "chan_${i}_${url.hashCode()}" }

                    val formattedName = formatChannelName(rawName)
                    val mappedCategory = when {
                        rawCategory.contains("News", ignoreCase = true) -> "News"
                        rawCategory.contains("Sport", ignoreCase = true) -> "Sports"
                        rawCategory.contains("Kids", ignoreCase = true) -> "Kids"
                        rawCategory.contains("Quran", ignoreCase = true) -> "Quran"
                        rawCategory.contains("Docu", ignoreCase = true) -> "Documentary"
                        rawCategory.contains("Kurdish", ignoreCase = true) -> "Kurdish"
                        rawCategory.contains("Music", ignoreCase = true) -> "Music"
                        else -> "General"
                    }
                    val isHd = formattedName.contains("HD", ignoreCase = true) ||
                            url.contains("1080", ignoreCase = true) ||
                            rawCategory.contains("HD", ignoreCase = true)
                    val quality = if (isHd) "HLS / 1080p" else "HLS / 720p"

                    channels.add(
                        Channel(
                            id = rawId,
                            name = formattedName,
                            streamUrl = url,
                            logoUrl = logo ?: getChannelLogo(formattedName),
                            category = mappedCategory,
                            quality = quality,
                            isHd = isHd
                        )
                    )
                }
            }
        } catch (_: Exception) {}
        return channels.distinctBy { it.streamUrl }
    }

    /**
     * Parses text/M3U format content from endpoints. Supports:
     * 1. Line-by-line format: channel_name;stream_url
     * 2. Standard M3U format with #EXTINF
     */
    fun parse(content: String): List<Channel> {
        if (content.trim().startsWith("[")) {
            val jsonChannels = parseJson(content)
            if (jsonChannels.isNotEmpty()) return jsonChannels
        }

        val channels = mutableListOf<Channel>()
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() }

        var currentExtName: String? = null
        var currentLogo: String? = null
        var currentGroup: String? = null

        for (line in lines) {
            when {
                line.startsWith("#EXTINF:", ignoreCase = true) -> {
                    currentExtName = parseExtName(line)
                    currentLogo = parseTvgLogo(line)
                    currentGroup = parseGroupTitle(line)
                }
                line.contains(";") && !line.startsWith("#") -> {
                    val parts = line.split(";", limit = 2)
                    if (parts.size == 2) {
                        val rawName = parts[0].trim()
                        val url = parts[1].trim()
                        if (url.startsWith("http://") || url.startsWith("https://")) {
                            channels.add(buildChannel(rawName, url, null, null))
                        }
                    }
                }
                (line.startsWith("http://") || line.startsWith("https://")) && !line.startsWith("#") -> {
                    val name = currentExtName ?: "Kurdish Channel ${channels.size + 1}"
                    channels.add(buildChannel(name, line, currentLogo, currentGroup))
                    currentExtName = null
                    currentLogo = null
                    currentGroup = null
                }
            }
        }

        val distinctChannels = channels.distinctBy { it.streamUrl }
        return if (distinctChannels.isNotEmpty()) distinctChannels else getFallbackChannels()
    }

    /**
     * Dynamically formats raw channel names like "rudaw-tv" -> "Rudaw TV"
     */
    fun formatChannelName(rawName: String): String {
        var clean = rawName
            .replace("-", " ")
            .replace("_", " ")
            .replace("  ", " ")
            .trim()

        clean = clean.split(" ").joinToString(" ") { word ->
            when (word.lowercase(Locale.ROOT)) {
                "tv" -> "TV"
                "hd" -> "HD"
                "4k" -> "4K"
                "sd" -> "SD"
                "nrt" -> "NRT"
                "ktv" -> "KTV"
                "ava" -> "AVA"
                "net" -> "NET"
                "trt" -> "TRT"
                "kurdistan24" -> "Kurdistan 24"
                "kurdsat" -> "KurdSat"
                "kurdmax" -> "KurdMax"
                else -> word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            }
        }
        return clean
    }

    private fun buildChannel(rawName: String, url: String, logoUrl: String?, groupTitle: String?): Channel {
        val formattedName = formatChannelName(rawName)
        val id = rawName.lowercase(Locale.ROOT).replace(nonAlphaNumericRegex, "") + "_" + url.hashCode()

        val category = when {
            isKidsChannel(formattedName) -> "Kids"
            isMusicChannel(formattedName) -> "Music"
            isNewsChannel(formattedName) -> "News"
            groupTitle != null && groupTitle.isNotBlank() -> groupTitle
            else -> "General"
        }

        val isHd = formattedName.contains("HD", ignoreCase = true) || url.contains("1080", ignoreCase = true) || url.contains("smil", ignoreCase = true)
        val quality = if (isHd) "HLS / 1080p" else "HLS / 720p"

        val defaultLogo = logoUrl ?: getChannelLogo(formattedName)

        return Channel(
            id = id,
            name = formattedName,
            streamUrl = url,
            logoUrl = defaultLogo,
            category = category,
            quality = quality,
            isHd = isHd
        )
    }

    private fun isNewsChannel(name: String): Boolean {
        val newsKeywords = listOf("rudaw", "kurdistan 24", "nrt", "channel 8", "ktv", "speda", "payam", "zagros", "kurdistan tv", "ronahi", "trt kurdi")
        return newsKeywords.any { name.lowercase(Locale.ROOT).contains(it) }
    }

    private fun isMusicChannel(name: String): Boolean {
        val musicKeywords = listOf("music", "korek", "kurdmax music", "vin tv")
        return musicKeywords.any { name.lowercase(Locale.ROOT).contains(it) }
    }

    private fun isKidsChannel(name: String): Boolean {
        val kidsKeywords = listOf("zarok", "pepule", "afarin", "kids", "child")
        return kidsKeywords.any { name.lowercase(Locale.ROOT).contains(it) }
    }

    private fun parseExtName(line: String): String {
        val commaIndex = line.lastIndexOf(',')
        return if (commaIndex != -1 && commaIndex < line.length - 1) {
            line.substring(commaIndex + 1).trim()
        } else {
            "Kurdish Channel"
        }
    }

    private fun parseTvgLogo(line: String): String? {
        return tvgLogoRegex.find(line)?.groupValues?.get(1)
    }

    private fun parseGroupTitle(line: String): String? {
        return groupTitleRegex.find(line)?.groupValues?.get(1)
    }

    private fun getChannelLogo(channelName: String): String {
        return when {
            channelName.contains("Rudaw", ignoreCase = true) -> "https://www.rudaw.net/images/rudaw_logo.png"
            channelName.contains("Kurdistan 24", ignoreCase = true) -> "https://www.kurdistan24.net/assets/images/logo.png"
            channelName.contains("KurdSat", ignoreCase = true) -> "https://kurdsat.tv/logo.png"
            else -> "https://raw.githubusercontent.com/BotanAtomic/Kurdistan-TV/master/logo.png"
        }
    }

    /**
     * Fallback Kurdish channels list
     */
    fun getFallbackChannels(): List<Channel> {
        return listOf(
            Channel(
                id = "rudaw_tv",
                name = "Rudaw TV",
                streamUrl = "https://svs.itworkscdn.net/rudawlive/rudawlive.smil/playlist.m3u8",
                logoUrl = "https://www.rudaw.net/images/rudaw_logo.png",
                category = "News",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "kurdistan24_tv",
                name = "Kurdistan 24",
                streamUrl = "https://live.kurdistan24.net/k24live/smil:k24live.smil/playlist.m3u8",
                logoUrl = "https://www.kurdistan24.net/assets/images/logo.png",
                category = "News",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "kurdistan_tv",
                name = "Kurdistan TV",
                streamUrl = "https://live.kurdistantv.net/ktvlive/ktvlive.smil/playlist.m3u8",
                logoUrl = "https://raw.githubusercontent.com/BotanAtomic/Kurdistan-TV/master/logo.png",
                category = "News",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "zagros_tv",
                name = "Zagros TV",
                streamUrl = "https://live.zagrostv.net/zagroslive/zagroslive.smil/playlist.m3u8",
                logoUrl = "https://raw.githubusercontent.com/BotanAtomic/Kurdistan-TV/master/logo.png",
                category = "News",
                quality = "HLS / 720p",
                isHd = true
            ),
            Channel(
                id = "kurdsat_tv",
                name = "KurdSat TV",
                streamUrl = "https://stream.kurdsat.tv/live/kurdsat/playlist.m3u8",
                logoUrl = "https://kurdsat.tv/logo.png",
                category = "General",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "kurdsat_news",
                name = "KurdSat News",
                streamUrl = "https://stream.kurdsat.tv/live/kurdsatnews/playlist.m3u8",
                logoUrl = "https://kurdsat.tv/logo.png",
                category = "News",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "kurdmax_show",
                name = "Kurdmax Show",
                streamUrl = "https://stream.kurdmax.tv/live/kurdmaxshow/playlist.m3u8",
                logoUrl = "https://raw.githubusercontent.com/BotanAtomic/Kurdistan-TV/master/logo.png",
                category = "General",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "kurdmax_music",
                name = "Kurdmax Music",
                streamUrl = "https://stream.kurdmax.tv/live/kurdmaxmusic/playlist.m3u8",
                logoUrl = "https://raw.githubusercontent.com/BotanAtomic/Kurdistan-TV/master/logo.png",
                category = "Music",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "kurdmax_pepule",
                name = "Kurdmax Pepule",
                streamUrl = "https://stream.kurdmax.tv/live/kurdmaxpepule/playlist.m3u8",
                logoUrl = "https://raw.githubusercontent.com/BotanAtomic/Kurdistan-TV/master/logo.png",
                category = "Kids",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "nrt_tv",
                name = "NRT TV",
                streamUrl = "https://live.nrttv.com/nrt/live/playlist.m3u8",
                logoUrl = "https://raw.githubusercontent.com/BotanAtomic/Kurdistan-TV/master/logo.png",
                category = "News",
                quality = "HLS / 720p",
                isHd = false
            ),
            Channel(
                id = "nrt2_tv",
                name = "NRT 2",
                streamUrl = "https://live.nrttv.com/nrt2/live/playlist.m3u8",
                logoUrl = "https://raw.githubusercontent.com/BotanAtomic/Kurdistan-TV/master/logo.png",
                category = "General",
                quality = "HLS / 720p",
                isHd = false
            ),
            Channel(
                id = "channel8_hd",
                name = "Channel 8 HD",
                streamUrl = "https://live.channel8.com/hls/stream.m3u8",
                logoUrl = "https://raw.githubusercontent.com/BotanAtomic/Kurdistan-TV/master/logo.png",
                category = "News",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "speda_tv",
                name = "Speda TV",
                streamUrl = "https://live.speda.net/spedatv/playlist.m3u8",
                logoUrl = "https://raw.githubusercontent.com/BotanAtomic/Kurdistan-TV/master/logo.png",
                category = "News",
                quality = "HLS / 720p",
                isHd = false
            ),
            Channel(
                id = "payam_tv",
                name = "Payam TV",
                streamUrl = "https://live.payamtv.net/payam/playlist.m3u8",
                logoUrl = "https://raw.githubusercontent.com/BotanAtomic/Kurdistan-TV/master/logo.png",
                category = "News",
                quality = "HLS / 720p",
                isHd = false
            ),
            Channel(
                id = "waar_tv",
                name = "Waar TV",
                streamUrl = "https://live.waartv.com/waarlive/playlist.m3u8",
                logoUrl = "https://raw.githubusercontent.com/BotanAtomic/Kurdistan-TV/master/logo.png",
                category = "General",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "ava_entertainment",
                name = "AVA Entertainment",
                streamUrl = "https://live.avatv.net/ava/playlist.m3u8",
                logoUrl = "https://raw.githubusercontent.com/BotanAtomic/Kurdistan-TV/master/logo.png",
                category = "General",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "zarok_tv",
                name = "Zarok TV",
                streamUrl = "https://live.zaroktv.com/zarok/playlist.m3u8",
                logoUrl = "https://raw.githubusercontent.com/BotanAtomic/Kurdistan-TV/master/logo.png",
                category = "Kids",
                quality = "HLS / 720p",
                isHd = true
            ),
            Channel(
                id = "trt_kurdi",
                name = "TRT Kurdi",
                streamUrl = "https://tv-trtturk.medya.trt.com.tr/m3u8/trt_kurdi.m3u8",
                logoUrl = "https://raw.githubusercontent.com/BotanAtomic/Kurdistan-TV/master/logo.png",
                category = "General",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "korek_tv",
                name = "Korek TV",
                streamUrl = "https://live.korektv.com/music/playlist.m3u8",
                logoUrl = "https://raw.githubusercontent.com/BotanAtomic/Kurdistan-TV/master/logo.png",
                category = "Music",
                quality = "HLS / 720p",
                isHd = false
            ),
            Channel(
                id = "ronahi_tv",
                name = "Ronahi TV",
                streamUrl = "https://live.ronahitv.net/ronahi/playlist.m3u8",
                logoUrl = "https://raw.githubusercontent.com/BotanAtomic/Kurdistan-TV/master/logo.png",
                category = "News",
                quality = "HLS / 720p",
                isHd = false
            ),
            Channel(
                id = "vin_tv",
                name = "Vin TV",
                streamUrl = "https://live.vintv.net/vintv/playlist.m3u8",
                logoUrl = "https://raw.githubusercontent.com/BotanAtomic/Kurdistan-TV/master/logo.png",
                category = "Music",
                quality = "HLS / 720p",
                isHd = false
            )
        )
    }
}
