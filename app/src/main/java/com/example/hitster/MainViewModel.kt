package com.example.hitster

import androidx.lifecycle.ViewModel
import com.example.hitster.home.model.SpotifyConnected
import com.example.hitster.home.model.SpotifyDisconnected
import com.example.hitster.home.model.SpotifyItem
import com.example.hitster.home.model.SpotifyLoading
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel : ViewModel() {

    private var _spotifyState = MutableStateFlow<SpotifyItem>(SpotifyLoading)
    val spotifyState = _spotifyState.asStateFlow()

    fun setSpotifyState(connected: Boolean) {
        _spotifyState.update {
            if (connected) {
                SpotifyConnected
            } else {
                SpotifyDisconnected
            }
        }
    }
}