package com.example.hitster.game.model

data class GameViewState(
    val musicButton: MusicButtonItem,
    val primaryButton: ButtonState,
    val currentSong: Song?,
    val currentPlayer: Player?,
    val playerItems: List<PlayerItem>,
    val songItems: List<SongItem>
) {
    companion object {
        fun initial() : GameViewState {
            return GameViewState(
                musicButton = MusicButtonItem.PLAY,
                primaryButton = ButtonState(),
                currentSong = null,
                currentPlayer = null,
                playerItems = emptyList(),
                songItems = emptyList()
            )
        }
    }
}