package com.example.hitster.game.model

/**
 * A token another player placed on a gap of the current player's timeline.
 *
 * @param gapIndex the gap of the current player's timeline the token sits in. A timeline of n cards
 * has n + 1 gaps, gap i is located in front of card i.
 * @param isCorrect null as long as the song is not revealed yet.
 */
data class TokenBet(
    val playerName: String,
    val gapIndex: Int,
    val isCorrect: Boolean? = null
)
