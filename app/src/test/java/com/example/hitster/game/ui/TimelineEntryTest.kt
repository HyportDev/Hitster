package com.example.hitster.game.ui

import com.example.hitster.game.model.Song
import com.example.hitster.game.model.SongItem
import com.example.hitster.game.model.TokenBet
import com.example.hitster.game.model.UnknownSong
import com.example.hitster.res.toText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class TimelineEntryTest {

    private val song1970 = song(1970)
    private val song1990 = song(1990)
    private val song2010 = song(2010)

    @Test
    fun `without gaps every song stays a card`() {
        val timeline = listOf(song1970, song1990, UnknownSong())

        val entries = timeline.toTimelineEntries(showGaps = false, tokenBets = emptyList())

        assertEquals(timeline.map { it.id }, entries.map { it.key })
    }

    @Test
    fun `the gap of the guess card is left out`() {
        // The guess card sits in gap 2, between 1990 and 2010.
        val timeline = listOf(song1970, song1990, UnknownSong(), song2010)

        val entries = timeline.toTimelineEntries(showGaps = true, tokenBets = emptyList())

        assertEquals(
            listOf(
                "gap-0",
                song1970.id,
                "gap-1",
                song1990.id,
                UnknownSong().id,
                song2010.id,
                "gap-3"
            ),
            entries.map { it.key }
        )
    }

    @Test
    fun `a guess card at the end has no gap behind it`() {
        val timeline = listOf(song1970, UnknownSong())

        val entries = timeline.toTimelineEntries(showGaps = true, tokenBets = emptyList())

        assertEquals(listOf("gap-0", song1970.id, UnknownSong().id), entries.map { it.key })
    }

    @Test
    fun `a guess card at the front has no gap in front of it`() {
        val timeline = listOf(UnknownSong(), song1970)

        val entries = timeline.toTimelineEntries(showGaps = true, tokenBets = emptyList())

        assertEquals(listOf(UnknownSong().id, song1970.id, "gap-1"), entries.map { it.key })
    }

    @Test
    fun `bets are attached to their gap`() {
        val timeline = listOf(song1970, song1990, UnknownSong())
        val bet = TokenBet(playerName = "Bob", gapIndex = 1)

        val entries = timeline.toTimelineEntries(showGaps = true, tokenBets = listOf(bet))

        assertEquals(bet, entries.filterIsInstance<TimelineEntry.Gap>().first { it.gapIndex == 1 }.bet)
    }

    @Test
    fun `only gaps with a bet survive after the reveal`() {
        val timeline = listOf(song1970, song1990, UnknownSong())
        val bet = TokenBet(playerName = "Bob", gapIndex = 1)

        val entries = timeline.toTimelineEntries(
            showGaps = true,
            tokenBets = listOf(bet),
            onlyGapsWithBet = true
        )

        assertEquals(
            listOf(song1970.id, "gap-1", song1990.id, UnknownSong().id),
            entries.map { it.key }
        )
    }

    private fun song(year: Int): SongItem =
        Song("Title $year".toText(), "Artist $year".toText(), year)
}
