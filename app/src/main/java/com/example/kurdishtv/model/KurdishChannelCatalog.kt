package com.example.kurdishtv.model

object KurdishChannelCatalog {

    /**
     * Curated offline catalog of Kurdish channels.
     *
     * Stream URLs are kept in sync with the app's own curated online playlist
     * (see [com.example.kurdishtv.repository.TvRepository]) so the offline / first-launch
     * experience matches what the live sources deliver.
     *
     * The list is built lazily once and reused. [Channel] is immutable, so sharing the
     * same instance is safe and avoids rebuilding the catalog on every UI-state default.
     */
    private val defaultChannels: List<Channel> by lazy { buildDefaultChannels() }

    fun getDefaultChannels(): List<Channel> = defaultChannels

    private fun buildDefaultChannels(): List<Channel> = listOf(
        // ── News ────────────────────────────────────────────────────────────────
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
            streamUrl = "https://hlspackager.akamaized.net/live/DB/K24/HLS/K24-avc1_2500000=10002,mp4a_128000=20000.m3u8",
            logoUrl = "https://www.kurdistan24.net/assets/images/logo.png",
            category = "News",
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
            id = "nrt_tv_hd",
            name = "NRT TV",
            streamUrl = "https://hlspackager.akamaized.net/live/DB/NRT_HD/HLS/NRT_HD-avc1_2500000=10002,mp4a_128000=20000.m3u8",
            logoUrl = "https://nrttv.com/logo.png",
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
            id = "kurdistan_tv_hd",
            name = "Kurdistan TV",
            streamUrl = "https://5a3ed7a72ed4b.streamlock.net/live/SMIL:myStream.smil/playlist.m3u8",
            logoUrl = "https://kurdistantv.net/logo.png",
            category = "News",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "zagros_tv_hd",
            name = "Zagros TV",
            streamUrl = "https://5a3ed7a72ed4b.streamlock.net/zagrostv/SMIL:myStream.smil/playlist.m3u8",
            logoUrl = "https://zagrostv.net/logo.png",
            category = "News",
            quality = "HLS / 720p",
            isHd = true
        ),
        Channel(
            id = "gali_kurdistan_hd",
            name = "Gali Kurdistan",
            streamUrl = "https://live.host247.net/gk/gksat/chunklist_w950982176.m3u8",
            category = "News",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "payam_tv_hd",
            name = "Payam TV",
            streamUrl = "https://media2.streambrothers.com:1936/8218/8218/chunklist_w1556137030.m3u8",
            category = "News",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "shams_tv_hd",
            name = "Shams TV",
            streamUrl = "https://stream.shams.tv/hls/0/stream.m3u8",
            category = "News",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "al_jazeera_hd",
            name = "Al Jazeera",
            streamUrl = "https://live-hls-web-aja.getaj.net/AJA/index.m3u8",
            category = "News",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "iraqia_kurdish_hd",
            name = "Iraqia Kurdish",
            streamUrl = "https://imn-live.esite-lab.com/hls/iraqia-kurdish.m3u8",
            category = "News",
            quality = "HLS / 1080p",
            isHd = true
        ),

        // ── General ─────────────────────────────────────────────────────────────
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
            id = "trt_kurdi_hd",
            name = "TRT Kurdi",
            streamUrl = "https://tv-trtkurdi.live.trt.com.tr/master.m3u8",
            logoUrl = "https://trtkurdi.net/logo.png",
            category = "General",
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
            id = "ava_entertainment_hd",
            name = "AVA Entertainment",
            streamUrl = "https://ava2.store/upload/ava.m3u8",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "speda_tv_hd",
            name = "Speda TV",
            streamUrl = "http://speda.teradek.live:1935/live/spedahd/playlist.m3u8",
            category = "General",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "kirkuk_tv_hd",
            name = "Kirkuk TV",
            streamUrl = "https://live.kirkuklive.live/hls/stream/index.m3u8",
            category = "General",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "nrt2_tv_hd",
            name = "NRT 2",
            streamUrl = "https://ca-rt.onetv.app/NRT2/index-0.m3u8?token=onetv202",
            category = "General",
            quality = "HLS / 720p",
            isHd = false
        ),

        // ── Kurdish culture ─────────────────────────────────────────────────────
        Channel(
            id = "komala_tv_hd",
            name = "Komala TV",
            streamUrl = "https://komhls.wns.live/hls/stream.m3u8",
            category = "Kurdish",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "kurd_channel_hd",
            name = "Kurd Channel",
            streamUrl = "https://kurdchhls.wns.live/hls/stream.m3u8",
            category = "Kurdish",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "sterk_tv_hd",
            name = "Sterk TV",
            streamUrl = "https://hlspackager.akamaized.net/live/DB/STERK_TV/HLS/STERK_TV.m3u8",
            category = "Kurdish",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "welat_tv_hd",
            name = "Welat TV",
            streamUrl = "https://stream.badinan.xyz/welat/tv/chunklist_w676236754.m3u8",
            category = "Kurdish",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "four_kurd_tv_hd",
            name = "4 Kurd TV",
            streamUrl = "https://4kuhls.persiana.live/hls/stream.m3u8",
            category = "Kurdish",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "parwarday_slemani_hd",
            name = "Parwarday Slemani",
            streamUrl = "https://parwarda.unitedmixmedia.tv/Parwarda/tracks-v2a1/mono.m3u8",
            category = "Kurdish",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "parwarday_hawler_hd",
            name = "Parwarday Hawler",
            streamUrl = "https://parwarda.unitedmixmedia.tv/Parwardayi_Hawler/tracks-v2a1/mono.m3u8",
            category = "Kurdish",
            quality = "HLS / 720p",
            isHd = false
        ),

        // ── Music ───────────────────────────────────────────────────────────────
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
            id = "korek_tv_hd",
            name = "Korek TV",
            streamUrl = "https://live.korektv.com/music/playlist.m3u8",
            logoUrl = "https://korektv.com/logo.png",
            category = "Music",
            quality = "HLS / 720p",
            isHd = false
        ),

        // ── Kids ────────────────────────────────────────────────────────────────
        Channel(
            id = "zarok_tv_hd",
            name = "Zarok TV",
            streamUrl = "https://zindisorani.zaroktv.com.tr/hls/0/stream.m3u8",
            category = "Kids",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "zarok_kurmanci_hd",
            name = "Zarok TV Kurmanci",
            streamUrl = "https://zindikurmanci.zaroktv.com.tr/hls/0/stream.m3u8",
            category = "Kids",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "afarin_kids_hd",
            name = "Afarin Kids",
            streamUrl = "https://65f16f0fdfc51.streamlock.net/afarinTV/livestream/playlist.m3u8",
            category = "Kids",
            quality = "HLS / 720p",
            isHd = false
        ),

        // ── Quran ───────────────────────────────────────────────────────────────
        Channel(
            id = "spi_quran_hd",
            name = "SPI Quran HD",
            streamUrl = "https://live20.bozztv.com/giatv/giatv-spi_quran_hd/spi_quran_hd/chunks.m3u8",
            category = "Quran",
            quality = "HLS / 1080p",
            isHd = true
        ),

        // ── Sports ──────────────────────────────────────────────────────────────
        Channel(
            id = "iraqia_sports_hd",
            name = "Iraqia Sports",
            streamUrl = "https://imn-live.esite-lab.com/hls/iraqia-sports-1.m3u8",
            category = "Sports",
            quality = "HLS / 1080p",
            isHd = true
        )
    )
}
