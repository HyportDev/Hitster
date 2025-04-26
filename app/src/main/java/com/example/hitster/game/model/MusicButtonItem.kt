package com.example.hitster.game.model

import androidx.annotation.DrawableRes
import com.example.hitster.R
import com.example.hitster.game.model.GameAction.OnPauseClick
import com.example.hitster.game.model.GameAction.OnPlayClick

enum class MusicButtonItem(
    @DrawableRes val icon: Int,
    val action: GameAction
) {
    PAUSE(icon = R.drawable.ic_pause, action = OnPauseClick),
    PLAY(icon = R.drawable.ic_play, action = OnPlayClick)
}
