package com.st10028374.vitality_vault.main

import android.content.Context
import android.util.Log
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote

object SpotifyManager {

    private var spotifyAppRemote: SpotifyAppRemote? = null
    private const val CLIENT_ID = "e0dd676a77b04de2a5a4df5dacc4b66b"
    private const val REDIRECT_URI = "vitalityvault://callback"

    fun isConnected() = spotifyAppRemote != null
    fun getRemote(): SpotifyAppRemote? = spotifyAppRemote

    fun connect(
        context: Context,
        onConnected: (SpotifyAppRemote) -> Unit,
        onFailure: (Throwable) -> Unit
    ) {
        if (spotifyAppRemote != null) {
            onConnected(spotifyAppRemote!!)
            return
        }

        val connectionParams = ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(remote: SpotifyAppRemote) {
                spotifyAppRemote = remote
                Log.d("SpotifyManager", "Connected to Spotify")
                onConnected(remote)
            }

            override fun onFailure(throwable: Throwable) {
                Log.e("SpotifyManager", "Spotify connection failed: ${throwable.message}")
                onFailure(throwable)
            }
        })
    }

    fun disconnect() {
        spotifyAppRemote?.let {
            SpotifyAppRemote.disconnect(it)
            spotifyAppRemote = null
            Log.d("SpotifyManager", "Disconnected from Spotify")
        }
    }

    fun stopPlaybackAndDisconnect() {
        spotifyAppRemote?.let { remote ->
            remote.playerApi.pause()
            SpotifyAppRemote.disconnect(remote)
            spotifyAppRemote = null
            Log.d("SpotifyManager", "Playback stopped and disconnected from Spotify")
        }
    }
}
