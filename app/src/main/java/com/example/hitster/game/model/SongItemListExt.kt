package com.example.hitster.game.model

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

internal fun List<SongItem>.getGuessedYearRange(guessedIndex: Int): IntRange =
    this.getPlacedSongs().getGapYearRange(guessedIndex)

/**
 * The years a song may have to belong into gap [gapIndex]. A timeline of n cards has n + 1 gaps,
 * gap i is located in front of card i.
 */
internal fun List<Song>.getGapYearRange(gapIndex: Int): IntRange {
    val leftItemYear = this.getOrNull(gapIndex - 1)?.releaseYear ?: 0
    val rightItemYear = this.getOrNull(gapIndex)?.releaseYear ?: LocalDate.now().year
    return leftItemYear..rightItemYear
}

/** The timeline without the guess card. */
internal fun List<SongItem>.getPlacedSongs(): List<Song> = this.filterIsInstance<Song>()

/** Gaps that are taken by neither the guess card nor a token of [takenGaps]. */
internal fun List<SongItem>.getFreeGapIndices(takenGaps: List<Int>): List<Int> {
    val guessCardGap = this.getGuessCardPosition()
    return (0..this.getPlacedSongs().size).filterNot { it == guessCardGap || it in takenGaps }
}

internal fun List<SongItem>.getGuessCardPosition() : Int {
    return this.indexOfFirst { it is UnknownSong }
}
