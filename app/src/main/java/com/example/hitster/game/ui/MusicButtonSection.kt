package com.example.hitster.game.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.hitster.R
import com.example.hitster.game.model.GameAction
import com.example.hitster.game.model.GameAction.NextPlayer
import com.example.hitster.game.model.MusicButtonItem
import com.example.hitster.ui.HitsterTheme

@Composable
internal fun MusicButtonSection(
    modifier: Modifier = Modifier,
    musicButton: MusicButtonItem,
    primaryAction: GameAction,
    onAction: (GameAction) -> Unit,
    height: Dp = 80.dp
) {
    val skipButtonSize = height * 2 / 3
    val gap = height / 4
    // The skip button sits beside the play button, so the section reserves room for it on both
    // sides: the play button stays centered whether or not the skip button is there, and nothing
    // is drawn outside these bounds.
    Box(
        modifier = modifier
            .padding(bottom = 4.dp)
            .width(height + (gap + skipButtonSize) * 2)
            .height(height),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            modifier = Modifier.size(height),
            colors = IconButtonDefaults.filledIconButtonColors(),
            onClick = { onAction(musicButton.action) }
        ) {
            Icon(
                modifier = Modifier.size(height / 2),
                painter = painterResource(musicButton.icon),
                contentDescription = stringResource(musicButton.contentDescription)
            )
        }
        AnimatedVisibility(
            modifier = Modifier.align(Alignment.CenterEnd),
            visible = primaryAction == NextPlayer,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            IconButton(
                modifier = Modifier.size(skipButtonSize),
                colors = IconButtonDefaults.outlinedIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary
                ),
                onClick = { onAction(NextPlayer) }
            ) {
                Icon(
                    modifier = Modifier.size(skipButtonSize / 2),
                    painter = painterResource(R.drawable.ic_skip),
                    contentDescription = stringResource(R.string.game_buttonNext)
                )
            }
        }
    }
}

@Preview
@Composable
private fun MusicButtonSectionPreview() {
    HitsterTheme {
        Box {
            MusicButtonSection(
                musicButton = MusicButtonItem.PAUSE,
                primaryAction = NextPlayer,
                onAction = {}
            )
        }
    }
}
