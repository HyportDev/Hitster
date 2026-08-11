package com.example.hitster.game.model

data class GameUiState(
    val musicButton: MusicButtonItem,
    val primaryButton: ButtonState,
    val players: List<Player>,
    val currentSong: Song?,
    val isAddTokenButtonVisible: Boolean = false,
    val phase: GamePhase = GamePhase.GUESSING,
    val tokenBets: List<TokenBet> = emptyList(),
    val tokenWinnerName: String? = null,
    /** Players who bet on a correct gap without winning the card and keep their token. */
    val refundedTokenPlayerNames: List<String> = emptyList()
)
