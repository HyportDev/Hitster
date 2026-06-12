package com.example.hitster.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hitster.R
import com.example.hitster.game.model.GameAction
import com.example.hitster.game.model.GameAction.NextPlayer
import com.example.hitster.game.model.MusicButtonItem

@Composable
internal fun MusicButtonSection(
    modifier: Modifier = Modifier,
    musicButton: MusicButtonItem,
    primaryAction: GameAction,
    onAction: (GameAction) -> Unit,
    height: Dp = 80.dp
) {
    Box(modifier = modifier.padding(bottom = 4.dp), contentAlignment = Alignment.Center) {
        IconButton(
            modifier = Modifier.align(Alignment.BottomCenter).size(height),
            colors = IconButtonDefaults.filledIconButtonColors(),
            onClick = { onAction(musicButton.action) }
        ) {
            Icon(
                modifier = Modifier.size(height/2),
                painter = painterResource(musicButton.icon),
                contentDescription = stringResource(musicButton.contentDescription)
            )
        }
        AnimatedVisibility(
            modifier = Modifier.offset(x = height * 1.25f),
            visible = primaryAction == NextPlayer,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            val smallerButtonSize = height * 2 / 3
            IconButton(
                modifier = Modifier.align(Alignment.BottomCenter).size(smallerButtonSize),
                colors = IconButtonDefaults.outlinedIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                onClick = { onAction(NextPlayer) }
            ) {
                Icon(
                    modifier = Modifier.size(smallerButtonSize/2),
                    painter = painterResource(R.drawable.ic_skip),
                    contentDescription = stringResource(R.string.game_buttonNext)
                )
            }
        }
    }
}
