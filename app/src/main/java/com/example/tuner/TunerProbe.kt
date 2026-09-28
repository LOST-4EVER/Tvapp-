package com.example.tuner

// TEMPORARY probe: does the satellite tuning API exist in the public SDK?
// Deleted once CI answers.
internal object TunerProbe {
    fun probe(manager: android.media.tv.TvInputManager, service: android.media.tv.TvInputService) {
        val tunerCount: Int = manager.tunerCount
        val request = android.media.tv.TvInputManager.TuneRequest(
            1, 11836000, android.media.tv.Tuner.TYPE_DVB_S2
        )
        val frequency: Int = request.frequency
        val tuner: android.media.tv.Tuner? = service.openTuner(request)
    }
}
