package com.example.hitster.game.model

import com.example.hitster.R
import com.example.hitster.res.toText

data class Player(
    val name: String,
    val songs: List<Song> = emptyList(),
    val tokens: Int = 2
) {
    companion object {
        fun initialise(name: String): Player {
            val randomYear = (1980..2010).random()
            return Player(
                name = name,
                songs = listOf(
                    Song(
                        title = R.string.home_startingPoint.toText(),
                        artist = R.string.home_startingPoint.toText(),
                        releaseYear = randomYear
                    )
                ),
                tokens = 2
            )
        }
    }
}
