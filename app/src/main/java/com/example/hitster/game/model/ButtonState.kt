package com.example.hitster.game.model

import androidx.annotation.StringRes

data class ButtonState(
    val isVisible: Boolean = false,
    @StringRes val title: Int? = null,
    val action: GameAction = GameAction.NextPlayer
)