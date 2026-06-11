package com.example.hitster.game.model

sealed interface GameAction {
    data object OnPlayClick : GameAction
    data object OnPauseClick : GameAction
    data object MoveLeft : GameAction
    data object MoveRight : GameAction
    data object Guess : GameAction
    data object NextPlayer : GameAction
    data class AddToken(val player: Player) : GameAction
    data class OnPlayerClick(val player: Player) : GameAction
}
