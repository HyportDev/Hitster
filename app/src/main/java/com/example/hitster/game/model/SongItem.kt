package com.example.hitster.game.model

import com.example.hitster.res.Text

sealed interface SongItem

data class Song(
    val title: Text,
    val artist: Text,
    val releaseYear: Int,
    val color: SongItemColor? = null,
    val correctLocation: Boolean? = null
) : SongItem

data class UnknownSong(
    val canMoveLeft: Boolean = true,
    val canMoveRight: Boolean = false
) : SongItem
