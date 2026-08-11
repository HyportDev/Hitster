package com.example.hitster.game.ui

import com.example.hitster.game.model.Song
import com.example.hitster.game.model.SongItem
import com.example.hitster.game.model.TokenBet

/** One slot of the horizontal timeline: either a card or a gap between two cards. */
internal sealed class TimelineEntry(val key: String) {

    data class Card(val songItem: SongItem) : TimelineEntry(key = songItem.id)

    data class Gap(val gapIndex: Int, val bet: TokenBet?) : TimelineEntry(key = "gap-$gapIndex")
}

/**
 * Interleaves the timeline with its gaps. The gap the guess card already occupies is left out,
 * because the guess card itself is displayed there.
 *
 * @param showGaps false renders the plain timeline without any gap.
 * @param onlyGapsWithBet true drops every empty gap, used after the reveal where only the gaps
 * players actually bet on are still interesting.
 */
internal fun List<SongItem>.toTimelineEntries(
    showGaps: Boolean,
    tokenBets: List<TokenBet>,
    onlyGapsWithBet: Boolean = false
): List<TimelineEntry> {
    if (!showGaps) return this.map { TimelineEntry.Card(it) }

    val entries = mutableListOf<TimelineEntry>()
    var gapIndex = 0
    var isGapTakenByGuessCard = false

    fun addGap(index: Int) {
        val bet = tokenBets.find { it.gapIndex == index }
        if (bet != null || !onlyGapsWithBet) entries.add(TimelineEntry.Gap(index, bet))
    }

    this.forEach { songItem ->
        if (songItem is Song) {
            if (!isGapTakenByGuessCard) addGap(gapIndex)
            gapIndex++
            isGapTakenByGuessCard = false
        } else {
            isGapTakenByGuessCard = true
        }
        entries.add(TimelineEntry.Card(songItem))
    }
    if (!isGapTakenByGuessCard) addGap(gapIndex)

    return entries
}
