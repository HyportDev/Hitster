package com.example.hitster.game

import kotlinx.serialization.Serializable

interface Routes {
    @Serializable
    data object Home : Routes

    @Serializable
    data class Game(val playerNames: List<String>, val playlists: List<String>) : Routes
}