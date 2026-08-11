package com.example.hitster.game.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hitster.R
import com.example.hitster.ui.HitsterTheme

private val PlayerCardShape = RoundedCornerShape(16.dp)
private const val MaxVisibleTokens = 5

@Composable
internal fun PlayerCard(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    isCurrentPlayer: Boolean,
    playerName: String,
    tokens: Int,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        label = "playerCardContainer"
    )
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val turnLabel = stringResource(R.string.game_playerTurn)
    val description = stringResource(R.string.game_playerTokens, playerName, tokens) +
        if (isCurrentPlayer) ", $turnLabel" else ""

    Card(
        modifier = modifier.clearAndSetSemantics { contentDescription = description },
        shape = PlayerCardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 6.dp else 0.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        // Only the player whose turn it is gets the ring, so browsing a foreign timeline never
        // hides who is actually playing.
        border = if (isCurrentPlayer) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.secondary)
        } else {
            null
        },
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isCurrentPlayer) {
                    Text(
                        text = turnLabel.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(
                    text = playerName,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor
                )
            }
            TokenRow(tokens = tokens, emptyColor = contentColor.copy(alpha = 0.35f))
        }
    }
}

/** The tokens a player still owns, as coins instead of a number. */
@Composable
private fun TokenRow(tokens: Int, emptyColor: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (tokens == 0) {
            // Keeps the card height stable once somebody runs out of tokens.
            TokenDot(color = Color.Transparent, borderColor = emptyColor)
        } else {
            repeat(minOf(tokens, MaxVisibleTokens)) {
                TokenDot(color = MaterialTheme.colorScheme.tertiary, borderColor = null)
            }
            if (tokens > MaxVisibleTokens) {
                Text(
                    text = "+${tokens - MaxVisibleTokens}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun TokenDot(color: Color, borderColor: Color?) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(CircleShape)
            .background(color)
            .then(
                if (borderColor == null) Modifier else Modifier.border(1.dp, borderColor, CircleShape)
            )
    )
}

@Preview
@Composable
private fun PlayerCardPreview() {
    HitsterTheme {
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlayerCard(
                isSelected = true,
                isCurrentPlayer = true,
                playerName = "Timo",
                tokens = 2,
                onClick = {}
            )
            PlayerCard(
                isSelected = false,
                isCurrentPlayer = false,
                playerName = "Luno",
                tokens = 1,
                onClick = {}
            )
            PlayerCard(
                isSelected = false,
                isCurrentPlayer = false,
                playerName = "Lily",
                tokens = 0,
                onClick = {}
            )
        }
    }
}
