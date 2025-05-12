package com.example.hitster.game.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.hitster.R
import com.example.hitster.game.model.GameAction.OnPauseClick
import com.example.hitster.game.model.GameAction.OnPlayClick

enum class MusicButtonItem(
    @DrawableRes val icon: Int,
    val action: GameAction,
    @StringRes val contentDescription: Int,
) {
    PAUSE(
        icon = R.drawable.ic_pause,
        action = OnPauseClick,
        contentDescription = R.string.game_pause)
    ,
    PLAY(
        icon = R.drawable.ic_play,
        action = OnPlayClick,
        contentDescription = R.string.game_play
    )
}
