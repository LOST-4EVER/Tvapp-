package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.kurdishtv.data.CustomPlaylistStorage
import com.example.kurdishtv.data.FavoriteStorage
import com.example.kurdishtv.data.RecentStorage
import com.example.kurdishtv.network.NetworkClient
import com.example.kurdishtv.repository.TvRepository
import com.example.kurdishtv.ui.navigation.KurdishTvNavGraph
import com.example.kurdishtv.viewmodel.TvViewModel
import com.example.ui.theme.KurdishTvTheme

class MainActivity : ComponentActivity() {

    private val favoriteStorage by lazy { FavoriteStorage(applicationContext) }
    private val recentStorage by lazy { RecentStorage(applicationContext) }
    private val customPlaylistStorage by lazy { CustomPlaylistStorage(applicationContext) }
    private val okHttpClient by lazy { NetworkClient.createOkHttpClient(applicationContext) }

    private val repository by lazy {
        TvRepository(
            favoriteStorage = favoriteStorage,
            recentStorage = recentStorage,
            customPlaylistStorage = customPlaylistStorage,
            okHttpClient = okHttpClient
        )
    }

    private val viewModel: TvViewModel by viewModels {
        TvViewModel.Factory(repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KurdishTvTheme {
                KurdishTvNavGraph(viewModel = viewModel)
            }
        }
    }
}
