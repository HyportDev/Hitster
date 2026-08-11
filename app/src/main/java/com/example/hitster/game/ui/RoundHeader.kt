package com.example.hitster.game.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hitster.R
import com.example.hitster.game.model.GamePhase
import com.example.hitster.ui.HitsterTheme

/**
 * Says whose turn it is and what that player is supposed to do right now. Without it the screen
 * never states the current player, because the timeline below can belong to anybody.
 */
@Composable
internal fun RoundHeader(
    modifier: Modifier = Modifier,
    currentPlayerName: String,
    phase: GamePhase,
    isSongLoaded: Boolean,
    wasGuessCorrect: Boolean?,
    compact: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = stringResource(R.string.game_turnOf, currentPlayerName),
            style = if (compact) {
                MaterialTheme.typography.titleLarge
            } else {
                MaterialTheme.typography.headlineMedium
            },
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        AnimatedContent(
            targetState = phaseHint(phase, isSongLoaded, wasGuessCorrect),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "phaseHint"
        ) { hint ->
            Text(
                text = stringResource(hint.textRes),
                style = MaterialTheme.typography.titleSmall,
                color = hint.color()
            )
        }
    }
}

private data class PhaseHint(val textRes: Int, val isResult: Boolean, val isCorrect: Boolean)

@Composable
private fun PhaseHint.color(): Color = when {
    !isResult -> MaterialTheme.colorScheme.secondary
    isCorrect -> SongCardValidation.VALID.color
    else -> SongCardValidation.INVALID.color
}

private fun phaseHint(
    phase: GamePhase,
    isSongLoaded: Boolean,
    wasGuessCorrect: Boolean?
): PhaseHint = when (phase) {
    GamePhase.GUESSING -> PhaseHint(
        textRes = if (isSongLoaded) R.string.game_phaseGuess else R.string.game_phaseLoading,
        isResult = false,
        isCorrect = false
    )

    GamePhase.TOKEN_PLACEMENT -> PhaseHint(R.string.game_phaseTokens, false, false)

    GamePhase.REVEALED -> PhaseHint(
        textRes = if (wasGuessCorrect == true) {
            R.string.game_phaseCorrect
        } else {
            R.string.game_phaseWrong
        },
        isResult = true,
        isCorrect = wasGuessCorrect == true
    )
}

@Preview
@Composable
private fun RoundHeaderPreview() {
    HitsterTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RoundHeader(
                currentPlayerName = "Timo",
                phase = GamePhase.GUESSING,
                isSongLoaded = false,
                wasGuessCorrect = null
            )
            RoundHeader(
                currentPlayerName = "Timo",
                phase = GamePhase.TOKEN_PLACEMENT,
                isSongLoaded = true,
                wasGuessCorrect = null
            )
            RoundHeader(
                currentPlayerName = "Timo",
                phase = GamePhase.REVEALED,
                isSongLoaded = false,
                wasGuessCorrect = true
            )
            RoundHeader(
                currentPlayerName = "Timo",
                phase = GamePhase.REVEALED,
                isSongLoaded = false,
                wasGuessCorrect = false
            )
        }
    }
}
