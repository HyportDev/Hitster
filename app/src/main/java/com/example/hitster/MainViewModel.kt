package com.example.hitster

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.hitster.data.AccessTokenProvider
import com.example.hitster.home.model.SpotifyConnected
import com.example.hitster.home.model.SpotifyDisconnected
import com.example.hitster.home.model.SpotifyItem
import com.example.hitster.home.model.SpotifyLoading
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class MainViewModel(accessTokenProvider: AccessTokenProvider) : ViewModel() {

    /**
     * Follows the session itself rather than the last login result, so a refresh that fails later
     * shows up here too.
     */
    val spotifyState: StateFlow<SpotifyItem> = accessTokenProvider
        .isAuthorized
        .map { isAuthorized ->
            when (isAuthorized) {
                null -> SpotifyLoading
                true -> SpotifyConnected
                false -> SpotifyDisconnected
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SpotifyLoading)
}
