package com.example.kurdishtv.model

object KurdishChannelCatalog {

    fun getDefaultChannels(): List<Channel> {
        return listOf(
            Channel(
                id = "rudaw_tv_hd",
                name = "Rudaw TV",
                streamUrl = "https://live.rudaw.net/hls/rudaw-tv/master.m3u8",
                logoUrl = "https://www.rudaw.net/images/rudaw_logo.png",
                category = "News",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "kurdistan24_tv_hd",
                name = "Kurdistan 24",
                streamUrl = "https://d1x82nydcxndze.cloudfront.net/live/index.m3u8",
                logoUrl = "https://www.kurdistan24.net/assets/images/logo.png",
                category = "News",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "kurdsat_tv_hd",
                name = "KurdSat HD",
                streamUrl = "https://hlspackager.akamaized.net/live/DB/KURDSAT_HD/HLS/KURDSAT_HD.m3u8",
                logoUrl = "https://kurdsat.tv/logo.png",
                category = "General",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "kurdsat_news_hd",
                name = "KurdSat News",
                streamUrl = "https://hlspackager.akamaized.net/live/DB/KURDSAT_NEWS/HLS/KURDSAT_NEWS.m3u8",
                logoUrl = "https://kurdsat.tv/logo.png",
                category = "News",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "channel8_kurdish_hd",
                name = "Channel 8 HD",
                streamUrl = "https://live.channel8.com/Channel8-Kurdish/index.m3u8",
                logoUrl = "https://channel8.com/logo.png",
                category = "News",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "kurdmax_show_hd",
                name = "Kurdmax Show",
                streamUrl = "https://6476e46b58f91.streamlock.net/liveTrans/SHOW1/playlist.m3u8",
                logoUrl = "https://kurdmax.tv/logo.png",
                category = "General",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "kurdmax_sorani_hd",
                name = "Kurdmax Sorani",
                streamUrl = "https://6476e46b58f91.streamlock.net/liveTrans/KurdmaxS0rani!/playlist.m3u8",
                logoUrl = "https://kurdmax.tv/logo.png",
                category = "General",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "kurdmax_music_hd",
                name = "Kurdmax Music",
                streamUrl = "https://6476e46b58f91.streamlock.net/music/livestream/playlist.m3u8",
                logoUrl = "https://kurdmax.tv/logo.png",
                category = "Music",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "trt_kurdi_hd",
                name = "TRT Kurdi",
                streamUrl = "https://tv-trtkurdi.live.trt.com.tr/master.m3u8",
                logoUrl = "https://trtkurdi.net/logo.png",
                category = "General",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "nrt_tv_hd",
                name = "NRT TV",
                streamUrl = "https://hlspackager.akamaized.net/live/DB/NRT_HD/HLS/NRT_HD.m3u8",
                logoUrl = "https://nrttv.com/logo.png",
                category = "News",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "waar_tv_hd",
                name = "Waar TV",
                streamUrl = "https://live.kwikmotion.com/waarmedialive/waarmedia.smil/playlist.m3u8",
                logoUrl = "https://waartv.com/logo.png",
                category = "General",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "speda_tv_hd",
                name = "Speda TV",
                streamUrl = "http://speda.teradek.live:1935/live/spedahd/playlist.m3u8",
                logoUrl = "https://speda.net/logo.png",
                category = "News",
                quality = "HLS / 720p",
                isHd = false
            ),
            Channel(
                id = "zarok_tv_hd",
                name = "Zarok TV",
                streamUrl = "https://live.zaroktv.com/zarok/playlist.m3u8",
                logoUrl = "https://zaroktv.com/logo.png",
                category = "Kids",
                quality = "HLS / 720p",
                isHd = true
            ),
            Channel(
                id = "ava_entertainment_hd",
                name = "AVA Entertainment",
                streamUrl = "https://live.avatv.net/ava/playlist.m3u8",
                logoUrl = "https://avatv.net/logo.png",
                category = "General",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "kurdistan_tv_hd",
                name = "Kurdistan TV",
                streamUrl = "https://live.kurdistantv.net/ktvlive/ktvlive.smil/playlist.m3u8",
                logoUrl = "https://kurdistantv.net/logo.png",
                category = "News",
                quality = "HLS / 1080p",
                isHd = true
            ),
            Channel(
                id = "zagros_tv_hd",
                name = "Zagros TV",
                streamUrl = "https://live.zagrostv.net/zagroslive/zagroslive.smil/playlist.m3u8",
                logoUrl = "https://zagrostv.net/logo.png",
                category = "News",
                quality = "HLS / 720p",
                isHd = true
            ),
            Channel(
                id = "korek_tv_hd",
                name = "Korek TV",
                streamUrl = "https://live.korektv.com/music/playlist.m3u8",
                logoUrl = "https://korektv.com/logo.png",
                category = "Music",
                quality = "HLS / 720p",
                isHd = false
            )
        )
    }
}
