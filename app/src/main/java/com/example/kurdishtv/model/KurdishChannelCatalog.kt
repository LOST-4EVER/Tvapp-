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
     *
     * Every stream URL and logo URL below is re-probed against the live network
     * before a release. Ids are derived from the source, name and stream URL and
     * never from list position, so adding, removing or reordering entries keeps
     * existing favourites and watch history pointing at the right channel.
     */
    private val cachedChannels: List<Channel> by lazy { buildDefaultChannels() }

    fun getDefaultChannels(): List<Channel> = cachedChannels

    // The catalog is split across small per-section builders instead of one giant
    // listOf() call: a single expression with 100+ arguments is far more than the
    // compiler resolves happily in one go, and a single bad entry then took the
    // whole file down with a wall of unrelated errors.

    // News — 11 channels
    private fun sectionNews(): List<Channel> = listOf(
        // ── News ────────────────────────────────────────────────────────────────
        Channel(
            id = "rudaw_tv_hd",
            name = "Rudaw TV",
            // Moved onto the Akamai edge. Measured over five requests each, the
            // previous origin took a median 424 ms to return the master playlist
            // and this takes 42 ms — about 10x faster to first frame, on a CDN
            // that does not sit behind the broadcaster's own origin. Both URLs
            // were verified end to end (master -> variant -> media segment) by
            // scripts/audit_streams.py before the swap.
            streamUrl = "https://hlspackager.akamaized.net/live/DB/RUDAW/HLS/RUDAW.m3u8",
            logoUrl = "https://i.imgur.com/Zo3IWOn.png",
            category = "News",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "kurdistan24_tv_hd",
            name = "Kurdistan 24",
            streamUrl = "https://hlspackager.akamaized.net/live/DB/K24/HLS/K24.m3u8",
            logoUrl = "https://i.imgur.com/9QWs2ms.png",
            category = "News",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "kurdsat_news_hd",
            name = "KurdSat News",
            streamUrl = "https://hlspackager.akamaized.net/live/DB/KURDSAT_NEWS/HLS/KURDSAT_NEWS.m3u8",
            logoUrl = "https://i.imgur.com/F3XRwkt.png",
            category = "News",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "nrt_tv_hd",
            name = "NRT TV",
            streamUrl = "https://hlspackager.akamaized.net/live/DB/NRT_HD/HLS/NRT_HD-avc1_2500000=10002,mp4a_128000=20000.m3u8",
            logoUrl = "https://i.imgur.com/uGKGtbW.png",
            category = "News",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "channel8_kurdish_hd",
            name = "Channel 8 HD",
            streamUrl = "https://live.channel8.com/Channel8-Kurdish/index.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/4/40/Channel8corp.png/960px-Channel8corp.png",
            category = "News",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "kurdistan_tv_hd",
            name = "Kurdistan TV",
            streamUrl = "https://5a3ed7a72ed4b.streamlock.net/live/SMIL:myStream.smil/playlist.m3u8",
            logoUrl = "https://i.imgur.com/mjQptpc.png",
            category = "News",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            // `isHd` was true while the entry below states 720p, which put this in
            // the "HD 1080p" tab at 720p. The id keeps its `_hd` suffix for the
            // reason given on `trt_1`: ids are stored in favourites.
            id = "zagros_tv_hd",
            name = "Zagros TV",
            streamUrl = "https://5a3ed7a72ed4b.streamlock.net/zagrostv/SMIL:myStream.smil/playlist.m3u8",
            logoUrl = "https://i.imgur.com/UjIuIQX.png",
            category = "News",
            quality = "HLS / 720p",
            isHd = false
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
            // Same swap as Rudaw, and the biggest win of the three: median 1027 ms
            // on the previous origin against 49 ms here, and it now advertises
            // 1080p rather than 720p.
            streamUrl = "https://hlspackager.akamaized.net/live/DB/PAYAM_TV/HLS/PAYAM_TV.m3u8",
            logoUrl = "https://i.imgur.com/qySEibb.png",
            category = "News",
            quality = "HLS / 1080p",
            isHd = true
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
            logoUrl = "https://i.imgur.com/BB93NQP.png",
            category = "News",
            quality = "HLS / 1080p",
            isHd = true
        ),
        // "Iraqia Kurdish" was removed here. Its master playlist resolved and its
        // variant resolved, but every media segment 404'd — across three separate
        // attempts. That is the failure mode a URL-200 check cannot see, and it
        // means the channel could never play no matter what the viewer did.
    )

    // General — 5 channels
    private fun sectionGeneral(): List<Channel> = listOf(

        // ── General ─────────────────────────────────────────────────────────────
        Channel(
            id = "kurdsat_tv_hd",
            name = "KurdSat HD",
            streamUrl = "https://hlspackager.akamaized.net/live/DB/KURDSAT_HD/HLS/KURDSAT_HD.m3u8",
            logoUrl = "https://i.imgur.com/UAbSwYA.png",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "trt_kurdi_hd",
            name = "TRT Kurdi",
            streamUrl = "https://tv-trtkurdi.medya.trt.com.tr/master.m3u8",
            logoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/7/79/TRT_Kurd%C3%AE_logo.svg/960px-TRT_Kurd%C3%AE_logo.svg.png",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "waar_tv_hd",
            name = "Waar TV",
            // Median 813 ms on the previous origin against 51 ms on the edge.
            streamUrl = "https://hlspackager.akamaized.net/live/DB/WAAR_HD/HLS/WAAR_HD.m3u8",
            logoUrl = "https://i.imgur.com/rK0y02d.png",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "avar_tv_hd",
            name = "Avar TV",
            streamUrl = "https://avr.host247.net/live/AvarTv/playlist.m3u8",
            logoUrl = "https://i.imgur.com/JazFBkW.jpeg",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "ilke_tv_hd",
            name = "Ilke TV",
            streamUrl = "https://stream.ilketv.com.tr/hls/ilkecanli.m3u8",
            logoUrl = "https://ilketv.com.tr/wp-content/uploads/2024/06/logo.png",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
    )

    // Kurdish culture — 12 channels
    private fun sectionKurdishCulture(): List<Channel> = listOf(

        // ── Kurdish culture ─────────────────────────────────────────────────────
        Channel(
            id = "komala_tv_hd",
            name = "Komala TV",
            streamUrl = "https://komhls.wns.live/hls/stream.m3u8",
            logoUrl = "https://i.imgur.com/D6KBLB8.png",
            category = "Kurdish",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "kurd_channel_hd",
            name = "Kurd Channel",
            streamUrl = "https://kurdchhls.wns.live/hls/stream.m3u8",
            logoUrl = "https://i.imgur.com/xPwzad9.png",
            category = "Kurdish",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "sterk_tv_hd",
            name = "Sterk TV",
            streamUrl = "https://hlspackager.akamaized.net/live/DB/STERK_TV/HLS/STERK_TV.m3u8",
            logoUrl = "https://i.imgur.com/bIheNAI.png",
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
        Channel(
            id = "rojava_hd",
            name = "Rojava HD",
            streamUrl = "https://hlspackager.akamaized.net/live/DB/ROJAVA_HD/HLS/ROJAVA_HD.m3u8",
            logoUrl = "https://i.imgur.com/42noysO.png",
            category = "Kurdish",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "ronahi_tv_hd",
            name = "Ronahi TV",
            streamUrl = "https://hlspackager.akamaized.net/live/DB/RONAHI_TV/HLS/RONAHI_TV.m3u8",
            logoUrl = "https://i.imgur.com/JSvJpD8.png",
            category = "Kurdish",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "cira_tv_hd",
            name = "Cira TV",
            streamUrl = "https://hlspackager.akamaized.net/live/DB/CIRA_TV/HLS/CIRA_TV.m3u8",
            logoUrl = "https://i.imgur.com/YFnmSLW.png",
            category = "Kurdish",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "jin_tv_hd",
            name = "Jin TV",
            streamUrl = "https://live.jintv.org/medialive/jintv.m3u8",
            logoUrl = "https://i.imgur.com/37ajg2j.png",
            category = "Kurdish",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "sercem_tv_hd",
            name = "Sercem TV",
            streamUrl = "https://canli.sercemtv.com.tr/hls/0/stream.m3u8",
            logoUrl = "https://sercemtv.com.tr/sercemlogo.jpg",
            category = "Kurdish",
            quality = "HLS / 1080p",
            isHd = true
        ),
    )

    // Religious and kids — 3 channels
    private fun sectionReligious(): List<Channel> = listOf(

        // ── Music ───────────────────────────────────────────────────────────────
        // No curated music entries: every stable Kurdish music source we could verify
        // is either dead or cleartext-only. The remote playlists merged at runtime
        // still populate this category.

        // ── Religious ──────────────────────────────────────────────────────────
        // These were previously filed under "Kids", which left them unreachable
        // from the category bar because no filter matched them.
        Channel(
            id = "zarok_tv_hd",
            name = "Zarok TV",
            streamUrl = "https://zindisorani.zaroktv.com.tr/hls/0/stream.m3u8",
            logoUrl = "https://i.imgur.com/o0eevnb.png",
            category = "Religious",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "zarok_kurmanci_hd",
            name = "Zarok TV Kurmanci",
            streamUrl = "https://zindikurmanci.zaroktv.com.tr/hls/0/stream.m3u8",
            logoUrl = "https://zaroktv.com.tr/wp-content/uploads/2021/10/cropped-LOGO-8K-192x192.png",
            category = "Religious",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "afarin_kids_hd",
            name = "Afarin Kids",
            streamUrl = "https://65f16f0fdfc51.streamlock.net/afarinTV/livestream/playlist.m3u8",
            logoUrl = "https://i.imgur.com/6MnXD7g.png",
            category = "Kids",
            quality = "HLS / 720p",
            isHd = false
        ),
    )

    // Quran — 2 channels
    private fun sectionQuran(): List<Channel> = listOf(

        // ── Quran ───────────────────────────────────────────────────────────────
        Channel(
            id = "amozhgary_tv_hd",
            name = "Amozhgary TV",
            streamUrl = "https://app-live.org/live/3268334b/index.m3u8",
            logoUrl = "https://i.imgur.com/sseqFsO.png",
            category = "Quran",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "spi_quran_hd",
            name = "SPI Quran HD",
            streamUrl = "https://live20.bozztv.com/giatv/giatv-spi_quran_hd/spi_quran_hd/chunks.m3u8",
            category = "Quran",
            quality = "HLS / 1080p",
            isHd = true
        ),
    )

    // Iraq & Kurdistan expansion — 23 channels
    private fun sectionIraqKurdistanExpansion(): List<Channel> = listOf(

        // The previous "Iraqia Sports" entry was removed: every sports path on
        // imn-live.esite-lab.com now 404s, so it could never start. Sports is
        // still a valid filter and will repopulate from the remote playlists.

        // ── Iraq & Kurdistan expansion ────────────────────────────────────────
        // Added after sweeping three public GitHub sources (iptv-org languages/kur.m3u,
        // iptv-org countries/iq.m3u and BotanAtomic/Kurdistan-TV canal.txt). Every
        // stream below was probed over HTTP and returned a valid HLS manifest, and
        // every logo was confirmed to return an image, so these are not aspirational
        // entries that would fail on first play.
        Channel(
            id = "afarin_baxcha",
            name = "Afarin Baxcha",
            streamUrl = "https://5dcabf026b188.streamlock.net/afarinTV/livestream/playlist.m3u8",
            logoUrl = "https://i.postimg.cc/4xRkBDRx/Afarin-Baxcha-200.png",
            category = "Kids",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "emantv",
            name = "EmanTv",
            streamUrl = "https://avr.host247.net/live/emantv/playlist.m3u8",
            logoUrl = "https://i.imgur.com/QmgaPSi.png",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "farmodatv",
            name = "FarmodaTV",
            streamUrl = "https://avr.host247.net/live/FarmodaTV/playlist.m3u8",
            logoUrl = "https://i.imgur.com/x4KCQlH.png",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "med_muzik",
            name = "Med Muzik",
            streamUrl = "http://54.36.110.140/live3/live3.m3u8",
            logoUrl = "https://i.imgur.com/gtUFmCD.png",
            category = "Music",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "mixkurdy",
            name = "Mixkurdy",
            streamUrl = "https://avr.host247.net/live/Mix-kurdy/playlist.m3u8",
            logoUrl = "https://i.imgur.com/WHeH5i7.png",
            category = "Music",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "abnsat",
            name = "ABNsat",
            streamUrl = "https://mediaserver.abnvideos.com/streams/abnsat.m3u8",
            logoUrl = "https://i.imgur.com/WhPlJzh.jpeg",
            category = "General",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "al_iraqia",
            name = "Al Iraqia",
            streamUrl = "https://imn-live.esite-lab.com/hls/iraqia-general.m3u8",
            logoUrl = "https://i.imgur.com/8qQouTW.jpg",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "al_janoub_tv",
            name = "Al Janoub TV",
            streamUrl = "https://live.alissahost.net/hls/test.m3u8",
            logoUrl = "https://i.imgur.com/rxDh6W1.png",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "al_sharqiya",
            name = "Al Sharqiya",
            streamUrl = "http://185.9.2.18/chid_144/index.m3u8",
            logoUrl = "https://i.imgur.com/mJIG5IV.png",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        // "Al Sharqiya News" was removed here. Its host's TLS certificate has
        // expired (leaf not valid after 2026-09-29), so every request is rejected
        // before a byte of the playlist is read. The community gist that also
        // carries this channel points at the same dead origin, and no public index
        // lists an alternative, so the entry is dropped rather than left holding a
        // News slot that can only fail. Re-add it once the broadcaster renews the
        // certificate.
        Channel(
            id = "al_aimma_tv",
            name = "Al-Aimma TV",
            streamUrl = "https://alaimma.tv/live/index.m3u8",
            logoUrl = "https://alaimma.tv/assets/images/logo.png",
            category = "Quran",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "al_jawadain_tv",
            name = "Al-Jawadain TV",
            streamUrl = "https://live.aljawadain.org/live/aljawadaintv/playlist.m3u8",
            logoUrl = "https://i.imgur.com/Eo3UYCK.png",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "alabbassia_tv",
            name = "Alabbassia TV",
            streamUrl = "https://stream.alabbassia.com/live/alabbassia/index.m3u8",
            logoUrl = "https://raw.githubusercontent.com/Alabbassia2026/Alabbassia-TV-Official/main/Logo%20PNG.png",
            category = "Religious",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "alawla_tv",
            name = "Alawla TV",
            streamUrl = "https://live.alawla.tv/hls/stream.m3u8",
            logoUrl = "https://i.imgur.com/3Kv0t3X.png",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "alquran",
            name = "Alquran",
            streamUrl = "https://ktvlive.online/stream/hls/ch1.m3u8",
            logoUrl = "https://i.imgur.com/lqVwrAF.png",
            category = "Quran",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "beitolabbas_tv_channel",
            name = "BeitolAbbas TV Channel",
            streamUrl = "https://live.beitolabbas.tv/live/beitolabbastv.m3u8",
            logoUrl = "https://i.imgur.com/s8PfUQy.png",
            category = "General",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "dijlah_tv",
            name = "Dijlah TV",
            streamUrl = "https://ghaasiflu.online/Dijlah/index.m3u8",
            logoUrl = "https://i.imgur.com/kyzH3G5.png",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "i_news",
            name = "I News",
            streamUrl = "https://live.i-news.tv/hls/stream.m3u8",
            logoUrl = "https://i.imgur.com/9lOuzHQ.png",
            category = "News",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "imam_hussein_tv_1",
            name = "Imam Hussein TV 1",
            streamUrl = "http://fa.imamhossaintv.com/live/ih1.m3u8",
            logoUrl = "https://i.imgur.com/pDdUvz7.png",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "imam_hussein_tv_2",
            name = "Imam Hussein TV 2",
            streamUrl = "http://ar.imamhossaintv.com/live/ih2.m3u8",
            logoUrl = "https://i.imgur.com/IXVSPdJ.png",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "imam_hussein_tv_3",
            name = "Imam Hussein TV 3",
            streamUrl = "http://en.imamhossaintv.com/live/ih3.m3u8",
            logoUrl = "https://i.imgur.com/emQRHQZ.png",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "imam_hussein_tv_4",
            name = "Imam Hussein TV 4",
            streamUrl = "http://ur.imamhossaintv.com/live/ih4.m3u8",
            logoUrl = "https://i.imgur.com/0UYcylY.png",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "imam_hussein_tv_5",
            name = "Imam Hussein TV 5",
            streamUrl = "http://live.al-zahratv.com/live/playlist.m3u8",
            logoUrl = "https://i.imgur.com/Oq2Ve7C.png",
            category = "General",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "imam_hussein_tv_6",
            name = "Imam Hussein TV 6",
            streamUrl = "http://live.imamhossaintv.com/live/ih6.m3u8",
            logoUrl = "https://i.imgur.com/lqhU0PY.png",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
    )

    // Iraq & Kurdistan expansion, continued — 9 channels
    private fun sectionIraqKurdistanExpansion2(): List<Channel> = listOf(
        Channel(
            id = "iraq_future",
            name = "Iraq Future",
            streamUrl = "https://viewmedia7219.bozztv.com/wmedia/viewmedia100/web_040/Stream/playlist.m3u8",
            logoUrl = "https://i.imgur.com/Z7woTe5.png",
            category = "General",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "ishtar_tv_sd",
            name = "Ishtar TV SD",
            streamUrl = "https://stream.ishtartv.com/live/iShtarHD/playlist.m3u8",
            logoUrl = "https://i.imgur.com/qzApokm.png",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "karbala_documentary",
            name = "Karbala Documentary",
            streamUrl = "https://ktvlive.online/stream/hls/ch3.m3u8",
            logoUrl = "https://www.karbala-tv.iq/icons/karbala.png",
            category = "Documentary",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "manu_chat",
            name = "Manu Chat",
            streamUrl = "https://vmi747311.contaboserver.net/hls/livetv.m3u8",
            logoUrl = "https://i.imgur.com/voyoljW.png",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "marjaeyat_tv_arabic",
            name = "Marjaeyat TV Arabic",
            streamUrl = "https://livefa.marjaeyattv.com/mtv_ar/playlist.m3u8",
            logoUrl = "https://i.imgur.com/LsYYkUI.png",
            category = "Religious",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "marjaeyat_tv_english",
            name = "Marjaeyat TV English",
            streamUrl = "https://livefa.marjaeyattv.com/mtv_en/playlist.m3u8",
            logoUrl = "https://i.imgur.com/Dvf61Jp.png",
            category = "Religious",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "marjaeyat_tv_persian",
            name = "Marjaeyat TV Persian",
            streamUrl = "https://livefa.marjaeyattv.com/mtv_fa/playlist.m3u8",
            logoUrl = "https://i.imgur.com/fCj05f0.png",
            category = "Religious",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "mbc_iraq",
            name = "MBC Iraq",
            streamUrl = "https://shd-gcp-live.edgenextcdn.net/live/bitmovin-mbc-iraq/e38c44b1b43474e1c39cb5b90203691e/index.m3u8",
            logoUrl = "https://i.imgur.com/D0LxiPE.png",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "samarra_tv",
            name = "Samarra TV",
            streamUrl = "https://live.bradosti.net/live/samaraweb_playlist.m3u8",
            logoUrl = "https://i.imgur.com/7uQoIXi.png",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        )
    )

    // Harvested channels — 20 channels
    private fun sectionHarvestedChannels(): List<Channel> = listOf(
        // ── Harvested channels ────────────────────────────────────────────────────
        // Every entry below was found in the app's own live sources plus public
        // community playlists, then verified end to end: the master playlist
        // resolves, its highest-bandwidth variant resolves, and a real media
        // segment from that variant returns MPEG-TS/fMP4 bytes. A 200 on the
        // master alone is not proof a stream plays — Channel 8's fMP4 path
        // answered 200 at every level and 403 on every segment, so its duplicate
        // entry was removed rather than kept.
        //
        // `quality` is the resolution the source actually publishes. Where a
        // playlist advertises no RESOLUTION attribute it reads "HLS / Auto"
        // rather than guessing, and the card shows that verbatim.

        // "Iraqya Sports" was removed here. Its master and variant playlists answer
        // 200 while every media segment returns 404 — the exact failure the audit
        // exists to catch, since a URL-level check would pass it. It was the only
        // curated Sports entry that could not play; MMN Sport still supplies the
        // Sports tab, and the category repopulates from the remote playlists.
        Channel(
            id = "mmn_sport",
            name = "MMN Sport",
            streamUrl = "http://mmn.mypsx.net:1935/live/mmnhdsport/playlist.m3u8",
            logoUrl = "https://i.postimg.cc/8NG9hZMg/MMNSPORT.png",
            category = "Sport",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "mmn_kids",
            name = "MMN Kids",
            streamUrl = "http://mmn.mypsx.net:1935/live/mmnhdkids/playlist.m3u8",
            logoUrl = "https://i.postimg.cc/GdS0dL0X/mmnkid.png",
            category = "Kids",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "spi_kids",
            name = "Spi Kids",
            // 4K, so genuinely HD; the variant is resolved by the manifest rather
            // than named in the path, so the quality stays open rather than
            // claiming a resolution this URL does not state.
            streamUrl = "https://live20.bozztv.com/giatv/giatv-spikids4k2025/spikids4k2025/chunks.m3u8",
            logoUrl = "https://i.postimg.cc/nFJkFmgm/KIDS.png",
            category = "Kids",
            quality = "HLS / Auto",
            isHd = true
        ),
        Channel(
            id = "mmn_quran",
            name = "MMN Quran",
            streamUrl = "http://mmn.mypsx.net:1935/live/mmnhdquran/playlist.m3u8",
            logoUrl = "https://i.postimg.cc/08wJZLfh/quran.png",
            category = "Quran",
            quality = "HLS / 1080p",
            isHd = true
        ),
        // "Soz Quran" was removed here: the playlist 404s on all three attempts.
        Channel(
            id = "mmn_documentary",
            name = "MMN Documentary",
            streamUrl = "http://mmn.mypsx.net:1935/live/mmnhddocumentary/playlist.m3u8",
            logoUrl = "https://i.postimg.cc/by0Sfk4T/clip.png",
            category = "Documentary",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "bnar_tv",
            name = "BNAR TV",
            streamUrl = "https://cdn.karwan.tv/bnar-family/tracks-v1a1/mono.m3u8",
            logoUrl = "https://i.postimg.cc/byWG7G01/BARINF.png",
            category = "Kurdish",
            quality = "HLS / Auto",
            isHd = false
        ),
        Channel(
            id = "bnar_movies",
            name = "BNAR Movies",
            streamUrl = "https://cdn.karwan.tv/bnar-movies/tracks-v1a1/mono.m3u8",
            logoUrl = "https://i.postimg.cc/1yGh2Bc1/bnarmov.png",
            category = "Kurdish",
            quality = "HLS / Auto",
            isHd = false
        ),
        // "BNAR Action" was removed here: the playlist 404s on all three attempts.
        // The other two Karwan channels (BNAR TV, BNAR Movies) are alive.
        Channel(
            id = "barin_movies",
            name = "Barin Movies",
            streamUrl = "https://cdn.karwan.tv/barin-movies/tracks-v1a1/mono.m3u8",
            logoUrl = "https://i.postimg.cc/gzZc6gR6/Barinm.png",
            category = "Kurdish",
            quality = "HLS / Auto",
            isHd = false
        ),
        Channel(
            id = "infinity_tv",
            name = "Infinity TV",
            streamUrl = "https://cdn.karwan.tv/infinity-tv/tracks-v1a1/mono.m3u8",
            category = "Kurdish",
            quality = "HLS / Auto",
            isHd = false
        ),
        Channel(
            id = "rojhelat",
            name = "Rozhelat",
            streamUrl = "https://hlspackager.akamaized.net/live/DB/ROJHELAT/HLS/ROJHELAT-avc1_2500000=10002,mp4a_128000=20000.m3u8",
            logoUrl = "https://www.rojhelat.media/wp-content/uploads/2026/01/logo-mob-2.png",
            category = "Kurdish",
            quality = "HLS / Auto",
            isHd = false
        ),
        Channel(
            id = "nuce_tv",
            name = "NUÇE TV",
            streamUrl = "https://hlspackager.akamaized.net/live/DB/NU_TV/HLS/NU_TV.m3u8",
            logoUrl = "https://i.ibb.co/7x6grdcN/WORLD-TV-PLUS-NUCE-TV.jpg",
            category = "Kurdish",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "gk_plus",
            name = "GK Plus",
            streamUrl = "https://live.host247.net/gk/gkplus/playlist.m3u8",
            logoUrl = "https://i.ibb.co/kgVxwSNk/WORLD-TV-PLUS-GK-PLUS.jpg",
            category = "Kurdish",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "ranya_city",
            name = "Ranya City",
            streamUrl = "https://avr.host247.net/Ranya/RanyaCity/playlist.m3u8",
            logoUrl = "https://i.ibb.co/VcK15tpv/WORLD-TV-PLUS-RANYA-CITY.jpg",
            category = "Kurdish",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "halabja_tv",
            name = "Halabja TV",
            streamUrl = "http://halabjatv.ddns.net:40/live/halabjatv/playlist.m3u8",
            logoUrl = "https://i.postimg.cc/ChRybCtn/halabja.png",
            category = "Kurdish",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "denge_zelal",
            name = "Denge Zelal",
            streamUrl = "https://dengetv.ozelip.com:3143/live/dengelive.m3u8",
            logoUrl = "https://r.resimlink.com/Tvf-DB3hOso8.png",
            category = "Kurdish",
            quality = "HLS / Auto",
            isHd = false
        ),
        Channel(
            id = "shna_tv",
            name = "Shna TV",
            streamUrl = "http://shnatv.ddns.net:1935/live/shnatv/playlist.m3u8",
            logoUrl = "https://i.postimg.cc/zDRMbxtm/D73-D4-D06-8642-4-D17-AC21-B718-D5-D84064.jpg",
            category = "Kurdish",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "iraqia_syriac",
            name = "Iraqia Syriac",
            streamUrl = "https://imn-live.esite-lab.com/hls/iraqia-syriac.m3u8",
            category = "Kurdish",
            quality = "HLS / Auto",
            isHd = false
        ),
        Channel(
            id = "mihrab_tv",
            name = "Mihrab TV",
            streamUrl = "http://bblserver.ddns.net:1935/live/mihrabfm/playlist.m3u8",
            logoUrl = "https://i.postimg.cc/y1r0XkCp/minara.png",
            category = "Religious",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "soran_entertainment",
            name = "Soran Entertainment",
            streamUrl = "http://avrstream.com:1935/live/SoranEntertainment/playlist.m3u8",
            logoUrl = "https://i.postimg.cc/Y9Tw96J5/Pics-Art-25-08-01-16-59-38-543.png",
            category = "Kurdish",
            quality = "HLS / 1080p",
            isHd = true
        ),
        // "Kurd 7" was removed here. Its origin returns 404 for the playlist path
        // on every attempt, and the community gist carries the identical URL, so
        // there is nothing to fall back to. Dropped rather than left in the Kurdish
        // list as a channel that can only fail once the viewer commits to it.
        Channel(
            id = "bask_plus",
            name = "Bask Plus",
            streamUrl = "http://baskhd.ddns.net:40/live/baskhd/playlist.m3u8",
            logoUrl = "https://i.postimg.cc/CxFf6zdt/photo-2026-02-16-06-41-54.jpg",
            category = "Kurdish",
            quality = "HLS / 1080p",
            isHd = true
        ),
    )

    // Harvested channels, continued — 10 channels
    private fun sectionHarvestedChannels2(): List<Channel> = listOf(
        Channel(
            id = "euro_kurd",
            name = "Euro Kurd",
            streamUrl = "http://baskhd.ddns.net:40/live/Eurokurd/playlist.m3u8",
            logoUrl = "https://i.postimg.cc/08Vjd4K2/EKURD.png",
            category = "Kurdish",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "mmn_news",
            name = "MMN News",
            streamUrl = "http://mmn.mypsx.net:1935/live/mmnhdnews/playlist.m3u8",
            category = "News",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "mmn_movies",
            name = "MMN Movies",
            streamUrl = "http://mmn.mypsx.net:1935/live/mmnhdmovies/playlist.m3u8",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "mmn_action",
            name = "MMN Action",
            streamUrl = "http://mmn.mypsx.net:1935/live/mmnhdaction/playlist.m3u8",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "mmn_show",
            name = "MMN Show",
            streamUrl = "http://mmn.mypsx.net:1935/live/mmnhddshow/playlist.m3u8",
            category = "General",
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "al_jazeera_2",
            name = "Al Jazeera 2",
            streamUrl = "https://live-hls-web-aja2-gcp.thehlive.com/AJA2/01.m3u8",
            category = "News",
            quality = "HLS / Auto",
            isHd = false
        ),
        Channel(
            id = "al_jazeera_mubasher",
            name = "Al Jazeera Mubasher",
            streamUrl = "https://live-hls-web-ajm-bp.thehlive.com/AJM/01.m3u8",
            category = "News",
            quality = "HLS / Auto",
            isHd = false
        ),
        Channel(
            id = "trt_arabic",
            name = "TRT Arabic",
            streamUrl = "https://tv-trtarabi.medya.trt.com.tr/master_1080.m3u8",
            category = "General",
            // The variant is named in the URL, so the quality is not a guess.
            // "HLS / Auto" was claiming the source does not say, about a source
            // whose own path says 1080.
            quality = "HLS / 1080p",
            isHd = true
        ),
        Channel(
            id = "trt_1",
            name = "TRT 1",
            // A 720p variant listed as HD.
            //
            // This was `isHd = true`, so TRT 1 appeared in the "HD 1080p" tab
            // alongside genuinely 1080p channels while streaming the 720p variant
            // its own URL names. `isHd` is a flat flag with no other source — the
            // HD tab and its count both read it directly — so the only way a viewer
            // could tell this entry apart was to play it and look.
            //
            // The `id` keeps its `_hd` suffix. Ids are derived once and stored in
            // favourites and watch history, so renaming one to match the corrected
            // flag would orphan every saved favourite for this channel. The id is
            // an opaque key, not a claim about the stream.
            streamUrl = "https://tv-trt1.medya.trt.com.tr/master_720.m3u8",
            category = "General",
            quality = "HLS / 720p",
            isHd = false
        ),
        Channel(
            id = "sky_news_arabia",
            name = "Sky News Arabia",
            streamUrl = "https://stream.skynewsarabia.com/hls/sna_720.m3u8",
            category = "News",
            quality = "HLS / 1080p",
            isHd = true
        ),
    )

    private fun buildDefaultChannels(): List<Channel> =
        sectionNews() +
            sectionGeneral() +
            sectionKurdishCulture() +
            sectionReligious() +
            sectionQuran() +
            sectionIraqKurdistanExpansion() +
            sectionIraqKurdistanExpansion2() +
            sectionHarvestedChannels() +
            sectionHarvestedChannels2()
}
