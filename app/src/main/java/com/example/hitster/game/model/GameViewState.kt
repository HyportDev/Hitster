package com.example.hitster.game.model

import com.example.hitster.R

data class GameViewState(
    val musicButton: MusicButtonItem,
    val primaryButton: ButtonState,
    val skipButton: ButtonState,
    val addTokenButton: ButtonState,
    val currentSong: Song?,
    val currentPlayer: Player?,
    val playerItems: List<PlayerItem>,
    val songItems: List<SongItem>,
    val isStealInProgress: Boolean = false,
    val message: String? = null
) {
    companion object {
        fun initial() : GameViewState {
            return GameViewState(
                musicButton = MusicButtonItem.PLAY,
                primaryButton = ButtonState(),
                skipButton = ButtonState(
                    isVisible = false,
                    title = R.string.game_skip,
                    action = GameAction.SkipSong
                ),
                addTokenButton = ButtonState(
                    isVisible = false,
                    title = R.string.game_addToken,
                    action = GameAction.AddToken
                ),
                currentSong = null,
                currentPlayer = null,
                playerItems = emptyList(),
                songItems = emptyList()
            )
        }
    }
}