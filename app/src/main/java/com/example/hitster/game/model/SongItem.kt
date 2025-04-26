package com.example.hitster.game.model

sealed interface SongItem

data class Song(
    val title: String,
    val artist: String,
    val releaseYear: Int,
    val color: SongItemColor? = null,
    val correctLocation: Boolean? = null
) : SongItem

data object UnknownSong : SongItem
