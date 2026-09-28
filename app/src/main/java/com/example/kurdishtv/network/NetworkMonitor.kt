package com.example.kurdishtv.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

class NetworkMonitor(context: Context) {
    private val connectivityManager =
        context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    val isOnline: Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(checkCurrentConnectivity())
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                trySend(isUsable(networkCapabilities))
            }
        }

        // Send current initial state
        trySend(checkCurrentConnectivity())

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager?.registerNetworkCallback(request, callback)

        awaitClose {
            try {
                connectivityManager?.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }.distinctUntilChanged().conflate()

    fun isCurrentlyOnline(): Boolean = checkCurrentConnectivity()

    private fun checkCurrentConnectivity(): Boolean {
        val cm = connectivityManager ?: return true
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return isUsable(capabilities)
    }

    /**
     * Whether a set of capabilities describes a network that can actually reach the
     * internet.
     *
     * `NET_CAPABILITY_INTERNET` alone only means the network *claims* to be able to
     * reach the internet. On a hotel or airport Wi-Fi that is captive — the request
     * is intercepted and redirected to a login page — and on a router that has lost
     * its uplink it is simply stale. `NET_CAPABILITY_VALIDATED` is the system having
     * actually probed it, so requiring it is the difference between "we think" and
     * "we know".
     *
     * This is one function on purpose. The startup check and the callback used to
     * disagree — the callback required INTERNET *and* VALIDATED while the startup
     * check accepted INTERNET alone — so on a captive network the app opened
     * convinced it was online, started a four-source fetch against a login portal,
     * and then flipped to "offline" the first time the system's own validation
     * completed. The banner appeared after the requests had already failed.
     */
    private fun isUsable(capabilities: NetworkCapabilities): Boolean =
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}
