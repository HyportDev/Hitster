package com.example.hitster.game.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hitster.ui.HitsterTheme

@Composable
internal fun PlayerCard(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    playerName: String,
    tokens: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.elevatedCardElevation(),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
        } else {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        },
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = playerName,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer
                }
            )
            Text(
                text = "Coins: $tokens",
                fontSize = 16.sp,
                color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                } else {
                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                }
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun PlayerCardPreview() {
    HitsterTheme {
        Row {
            PlayerCard(
                isSelected = true,
                playerName = "Timo",
                tokens = 2,
                onClick = {}
            )
            PlayerCard(
                isSelected = false,
                playerName = "Luno",
                tokens = 1,
                onClick = {}
            )
        }
    }
}
