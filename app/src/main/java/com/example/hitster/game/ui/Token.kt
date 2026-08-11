package com.example.hitster.game.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hitster.R
import com.example.hitster.game.model.TokenBet
import com.example.hitster.game.ui.SongCardValidation.Companion.toSongCardValidation
import com.example.hitster.ui.HitsterTheme

internal val TokenSize = 44.dp
private val GapWidth = 60.dp

/** Makes a gap easier to hit than its pure bounds would. */
private val GapDropTolerance = 16.dp

@Composable
internal fun Token(
    modifier: Modifier = Modifier,
    playerName: String,
    borderColor: Color = MaterialTheme.colorScheme.tertiary
) {
    val description = stringResource(R.string.game_tokenOfPlayer, playerName)
    Box(
        modifier = modifier
            .size(TokenSize)
            .shadow(elevation = 4.dp, shape = CircleShape)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiaryContainer)
            .border(width = 3.dp, color = borderColor, shape = CircleShape)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = playerName.take(2).uppercase(),
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1
        )
    }
}

/**
 * A token that can be dragged onto a [GapSlot]. [onDropped] is called with the gap the token landed
 * in, dropping it anywhere else does nothing.
 */
@Composable
internal fun DraggableToken(
    modifier: Modifier = Modifier,
    playerName: String,
    dragState: TokenDragState,
    onDropped: (gapIndex: Int) -> Unit
) {
    var positionInRoot by remember { mutableStateOf(Offset.Zero) }
    val currentOnDropped by rememberUpdatedState(onDropped)
    val isDragged = dragState.draggedPlayerName == playerName

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Token(
            modifier = Modifier
                .onGloballyPositioned { positionInRoot = it.positionInRoot() }
                .alpha(if (isDragged) 0.3f else 1f)
                .pointerInput(playerName) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragState.startDrag(playerName, positionInRoot + offset)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            dragState.drag(positionInRoot + change.position)
                        },
                        onDragEnd = { dragState.endDrag()?.let { currentOnDropped(it) } },
                        onDragCancel = { dragState.endDrag() }
                    )
                },
            playerName = playerName
        )
        Text(
            text = playerName,
            fontSize = 12.sp,
            maxLines = 1,
            color = MaterialTheme.colorScheme.secondary
        )
    }
}

/**
 * A gap of the timeline a token can be dropped into. Passing a [dragState] turns the slot into a
 * drop target, passing null only displays it. [onTakeBack] makes a token that already sits here
 * tappable to hand it back to its owner.
 */
@Composable
internal fun GapSlot(
    modifier: Modifier = Modifier,
    gapIndex: Int,
    bet: TokenBet?,
    dragState: TokenDragState?,
    onTakeBack: (() -> Unit)? = null
) {
    val isHovered = dragState?.hoveredGapIndex == gapIndex
    val scale by animateFloatAsState(targetValue = if (isHovered) 1.2f else 1f, label = "gapScale")
    val tolerance = with(LocalDensity.current) { GapDropTolerance.toPx() }

    Box(
        modifier = modifier
            .width(GapWidth)
            .fillMaxHeight()
            .then(
                if (dragState == null) {
                    Modifier
                } else {
                    Modifier.onGloballyPositioned {
                        dragState.registerGap(gapIndex, it.boundsInRoot().inflate(tolerance))
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (dragState != null) {
            DisposableEffect(gapIndex) {
                onDispose { dragState.unregisterGap(gapIndex) }
            }
        }
        if (bet == null) {
            Box(
                modifier = Modifier
                    .scale(scale)
                    .size(TokenSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .border(
                        width = 2.dp,
                        color = if (isHovered) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.outline
                        },
                        shape = CircleShape
                    )
                    .semantics { contentDescription = "Gap $gapIndex" }
            )
        } else {
            val takeBackDescription = stringResource(R.string.game_tokenTakeBack, bet.playerName)
            Token(
                modifier = Modifier
                    .scale(scale)
                    .then(
                        if (onTakeBack == null) {
                            Modifier
                        } else {
                            Modifier.clickable(onClickLabel = takeBackDescription) { onTakeBack() }
                        }
                    ),
                playerName = bet.playerName,
                borderColor = bet.isCorrect.toSongCardValidation()?.color
                    ?: MaterialTheme.colorScheme.tertiary
            )
        }
    }
}

@Preview
@Composable
private fun TokenPreview() {
    HitsterTheme {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Token(playerName = "Timo")
            Token(
                playerName = "Luno",
                borderColor = SongCardValidation.VALID.color
            )
            Token(
                playerName = "Lily",
                borderColor = SongCardValidation.INVALID.color
            )
        }
    }
}
