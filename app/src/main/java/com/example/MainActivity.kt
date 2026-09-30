package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.kurdishtv.ads.StartIoAds
import com.example.kurdishtv.data.ChannelCacheStorage
import com.example.kurdishtv.data.CustomPlaylistStorage
import com.example.kurdishtv.data.FavoriteStorage
import com.example.kurdishtv.data.RecentStorage
import com.example.kurdishtv.data.SettingsStorage
import com.example.kurdishtv.network.NetworkClient
import com.example.kurdishtv.network.NetworkMonitor
import com.example.kurdishtv.repository.TvRepository
import com.example.kurdishtv.ui.navigation.KurdishTvNavGraph
import com.example.kurdishtv.update.UpdateChecker
import com.example.kurdishtv.viewmodel.SettingsViewModel
import com.example.kurdishtv.viewmodel.TvViewModel
import com.example.ui.theme.KurdishTvTheme

class MainActivity : ComponentActivity() {

    private val favoriteStorage by lazy { FavoriteStorage(applicationContext) }
    private val recentStorage by lazy { RecentStorage(applicationContext) }
    private val customPlaylistStorage by lazy { CustomPlaylistStorage(applicationContext) }
    private val channelCacheStorage by lazy { ChannelCacheStorage(applicationContext) }
    private val settingsStorage by lazy { SettingsStorage(applicationContext) }
    private val okHttpClient by lazy { NetworkClient.getOkHttpClient(applicationContext) }
    private val networkMonitor by lazy { NetworkMonitor(applicationContext) }

    private val repository by lazy {
        TvRepository(
            favoriteStorage = favoriteStorage,
            recentStorage = recentStorage,
            customPlaylistStorage = customPlaylistStorage,
            channelCacheStorage = channelCacheStorage,
            okHttpClient = okHttpClient
        )
    }

    private val updateChecker by lazy {
        // The application context, so the check throttle survives the activity —
        // which is the point of it: throttling per-instance would mean every
        // launch is a first launch.
        UpdateChecker(okHttpClient, applicationContext)
    }

    private val viewModel: TvViewModel by viewModels {
        TvViewModel.Factory(
            repository = repository,
            networkMonitor = networkMonitor,
            updateChecker = updateChecker,
            appContext = applicationContext
        )
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModel.Factory(settingsStorage)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Start.io has to be initialised before any ad is requested, and its callback
        // is the only reliable "ready" signal. Initialising here rather than in a
        // custom Application class keeps the dependency wiring in one place; `init`
        // is idempotent, so an activity recreation does not re-run it.
        StartIoAds.init(applicationContext)

        setContent {
            val settings by settingsViewModel.settings.collectAsState()

            KurdishTvTheme(settings = settings) {
                // No frame loops here any more.
                //
                // This used to stand up two of them for the whole life of the
                // process: a shared `withFrameNanos` loop breathing every LIVE badge
                // on screen, and another slowly turning every focus ring. Both were
                // correctly demand-driven — they stopped when nothing was reading
                // them and when the app left the foreground — and both were still
                // there, which is the point: the demand was never the problem, the
                // animation was.
                KurdishTvNavGraph(
                    viewModel = viewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }
}
