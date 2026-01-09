package com.example.hitster.game.model

import androidx.compose.ui.graphics.Color
import com.example.hitster.res.Text

sealed class SongItem(val id: String)

data class Song(
    val title: Text,
    val artist: Text,
    val releaseYear: Int,
    val color: Color? = null,
    val correctLocation: Boolean? = null
) : SongItem(id = "song-$title-$artist-$releaseYear")

data class UnknownSong(
    val canMoveLeft: Boolean = true,
    val canMoveRight: Boolean = false
) : SongItem(id = "unknown-song")
