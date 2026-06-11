package com.example.hitster.game.model

data class GameUiState(
    val musicButton: MusicButtonItem,
    val primaryButton: ButtonState,
    val players: List<Player>,
    val currentSong: Song?,
    val isAddTokenButtonVisible: Boolean = false
)
