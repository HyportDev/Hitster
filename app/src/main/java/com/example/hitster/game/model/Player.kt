package com.example.hitster.game.model

import android.content.res.Resources
import com.example.hitster.R

data class Player(
    val name: String,
    val songs: List<Song> = emptyList()
) {
    companion object {
        fun initialise(name: String, resources: Resources): Player {
            val randomYear = (1980..2010).random()
            return Player(
                name = name,
                songs = listOf(
                    Song(
                        title = resources.getString(R.string.home_startingPoint),
                        artist = resources.getString(R.string.home_startingPoint),
                        releaseYear = randomYear
                    )
                )
            )
        }
    }
}
