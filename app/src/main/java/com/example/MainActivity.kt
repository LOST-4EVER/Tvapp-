package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.kurdishtv.data.ChannelCacheStorage
import com.example.kurdishtv.data.CustomPlaylistStorage
import com.example.kurdishtv.data.FavoriteStorage
import com.example.kurdishtv.data.RecentStorage
import com.example.kurdishtv.data.SettingsStorage
import com.example.kurdishtv.network.NetworkClient
import com.example.kurdishtv.network.NetworkMonitor
import com.example.kurdishtv.repository.TvRepository
import com.example.kurdishtv.ui.motion.LocalLivePulse
import com.example.kurdishtv.ui.motion.rememberLivePulse
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
        UpdateChecker(okHttpClient)
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

        setContent {
            val settings by settingsViewModel.settings.collectAsState()

            KurdishTvTheme(settings = settings) {
                // One shared pulse drives every LIVE badge in the app.
                val livePulse = rememberLivePulse(settings.livePulse && !settings.reduceMotion)
                CompositionLocalProvider(LocalLivePulse provides livePulse) {
                    KurdishTvNavGraph(
                        viewModel = viewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}
