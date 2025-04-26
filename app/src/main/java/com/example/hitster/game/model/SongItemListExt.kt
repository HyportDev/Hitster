package com.example.hitster.game.model

import android.util.Range
import java.time.LocalDate

internal fun List<SongItem>.moveSongItemLeft(index: Int): List<SongItem> {
    if (index in 1 until this.size) {
        val mutable = this.toMutableList()
        val tmp = mutable[index]
        mutable[index] = mutable[index - 1]
        mutable[index - 1] = tmp
        return mutable.toList()
    }
    return this
}

internal fun List<SongItem>.moveSongItemRight(index: Int): List<SongItem> {
    if (index in 0 until this.size - 1) {
        val mutable = this.toMutableList()
        val tmp = mutable[index]
        mutable[index] = mutable[index + 1]
        mutable[index + 1] = tmp
        return mutable.toList()
    }
    return this
}

internal fun List<SongItem>.getGuessedYearRange(guessedIndex: Int): Range<Int> {
    val leftItemYear = (this.getOrNull(guessedIndex - 1) as? Song)?.releaseYear ?: 0
    val rightItemYear = (this.getOrNull(guessedIndex + 1) as? Song)?.releaseYear
        ?: LocalDate.now().year
    return Range(leftItemYear, rightItemYear)
}

internal fun List<SongItem>.getGuessCardPosition() : Int {
    return this.indexOf(UnknownSong)
}
