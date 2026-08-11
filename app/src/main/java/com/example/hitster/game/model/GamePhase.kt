package com.example.hitster.game.model

enum class GamePhase {
    /** The current player positions the guess card on their own timeline. */
    GUESSING,

    /** The current player committed to a gap, the other players may bet a token on a free gap. */
    TOKEN_PLACEMENT,

    /** The song is revealed, everybody sees who was right. */
    REVEALED
}
