package com.example.hitster.game.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.hitster.R
import com.example.hitster.game.model.ButtonState
import com.example.hitster.game.model.GameAction
import com.example.hitster.game.model.GameAction.AddToken
import com.example.hitster.game.model.GameAction.Guess
import com.example.hitster.game.model.GameAction.PlaceToken
import com.example.hitster.game.model.GameAction.Reveal
import com.example.hitster.game.model.GameAction.TakeBackToken
import com.example.hitster.game.model.GamePhase
import com.example.hitster.game.model.GameUiState
import com.example.hitster.game.model.MusicButtonItem
import com.example.hitster.game.model.Player
import com.example.hitster.game.model.Song
import com.example.hitster.game.model.SongItem
import com.example.hitster.game.model.TokenBet
import com.example.hitster.game.model.UnknownSong
import com.example.hitster.game.ui.SongCardValidation.Companion.toSongCardValidation
import com.example.hitster.res.toText
import com.example.hitster.ui.Button
import com.example.hitster.ui.HitsterTheme
import kotlin.math.roundToInt

@Composable
internal fun GameScreen(state: GameUiState, onAction: (GameAction) -> Unit) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val dragState = remember { TokenDragState() }
    var screenPositionInRoot by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { screenPositionInRoot = it.positionInRoot() }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isLandscape) {
                Column(modifier = Modifier.weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        MusicButtonSection(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            musicButton = state.musicButton,
                            primaryAction = state.primaryButton.action,
                            onAction = onAction,
                            height = 50.dp
                        )
                        LazyRow(
                            modifier = Modifier.padding(start = 100.dp, end = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            playerList(state.players, onAction)
                        }
                    }
                    TimelineSection(state, dragState, onAction)
                }
            } else {
                Column(modifier = Modifier.weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        playerList(state.players, onAction)
                    }
                    TimelineSection(state, dragState, onAction)
                }
                MusicButtonSection(
                    musicButton = state.musicButton,
                    primaryAction = state.primaryButton.action,
                    onAction = onAction
                )
            }
        }

        DraggedToken(dragState = dragState, screenPositionInRoot = screenPositionInRoot)
    }
}

/** The token that follows the finger, drawn on top of everything else. */
@Composable
private fun DraggedToken(dragState: TokenDragState, screenPositionInRoot: Offset) {
    val playerName = dragState.draggedPlayerName ?: return
    val halfTokenSize = with(LocalDensity.current) { TokenSize.roundToPx() / 2 }
    Token(
        modifier = Modifier
            .offset {
                val position = dragState.dragPosition - screenPositionInRoot
                IntOffset(
                    x = position.x.roundToInt() - halfTokenSize,
                    y = position.y.roundToInt() - halfTokenSize
                )
            }
            .scale(1.15f),
        playerName = playerName
    )
}

/**
 * The timeline of the selected player plus everything that belongs to the current round: the token
 * bets on the current player's gaps and the buttons below.
 */
@Composable
private fun TimelineSection(
    state: GameUiState,
    dragState: TokenDragState,
    onAction: (GameAction) -> Unit
) {
    val selectedPlayer = state.players.find { it.isSelected } ?: return
    val isCurrentPlayersTimeline = selectedPlayer.isCurrentPlayer
    val isBettingOpen = isCurrentPlayersTimeline && state.phase == GamePhase.TOKEN_PLACEMENT
    val showGaps = isCurrentPlayersTimeline &&
        state.phase in setOf(GamePhase.TOKEN_PLACEMENT, GamePhase.REVEALED)

    Column {
        SongRow(
            songItems = selectedPlayer.songs,
            primaryButton = state.primaryButton,
            phase = state.phase,
            isSongLoaded = state.currentSong != null,
            showGaps = showGaps,
            onlyGapsWithBet = state.phase == GamePhase.REVEALED,
            tokenBets = if (showGaps) state.tokenBets else emptyList(),
            dragState = if (isBettingOpen) dragState else null,
            isTakeBackEnabled = isBettingOpen,
            onAction = onAction
        )

        AnimatedVisibility(isBettingOpen) {
            TokenBettingSection(
                modifier = Modifier.padding(horizontal = 16.dp),
                players = state.players.filter { player ->
                    !player.isCurrentPlayer &&
                        player.tokens > 0 &&
                        state.tokenBets.none { it.playerName == player.name }
                },
                hasPlacedTokens = state.tokenBets.isNotEmpty(),
                dragState = dragState,
                onPlaceToken = { playerName, gapIndex ->
                    onAction(PlaceToken(playerName, gapIndex))
                }
            )
        }

        AnimatedVisibility(state.phase == GamePhase.REVEALED && state.tokenWinnerName != null) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(R.string.game_tokenWinner, state.tokenWinnerName.orEmpty()),
                fontWeight = FontWeight.SemiBold,
                color = SongCardValidation.VALID.color
            )
        }

        AnimatedVisibility(
            state.phase == GamePhase.REVEALED && state.refundedTokenPlayerNames.isNotEmpty()
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = stringResource(
                    R.string.game_tokenRefunded,
                    state.refundedTokenPlayerNames.joinToString()
                ),
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        // The token is earned by whoever's turn it is, no matter which timeline is on screen.
        AnimatedVisibility(state.isAddTokenButtonVisible) {
            Button(
                modifier = Modifier.padding(start = 16.dp, end = 16.dp),
                title = stringResource(R.string.game_add_token),
                primary = true,
                onClick = {
                    state.players.find { it.isCurrentPlayer }?.let { onAction(AddToken(it)) }
                }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TokenBettingSection(
    modifier: Modifier = Modifier,
    players: List<Player>,
    hasPlacedTokens: Boolean,
    dragState: TokenDragState,
    onPlaceToken: (playerName: String, gapIndex: Int) -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(
                if (players.isEmpty()) R.string.game_tokenAllBetsPlaced else R.string.game_tokenHint
            ),
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary
        )
        if (hasPlacedTokens) {
            Text(
                text = stringResource(R.string.game_tokenTakeBackHint),
                color = MaterialTheme.colorScheme.secondary
            )
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            players.forEach { player ->
                DraggableToken(
                    playerName = player.name,
                    dragState = dragState,
                    onDropped = { gapIndex -> onPlaceToken(player.name, gapIndex) }
                )
            }
        }
    }
}

private fun LazyListScope.playerList(playerItems: List<Player>, onAction: (GameAction) -> Unit) {
    items(playerItems, key = { it.name }) {
        PlayerCard(
            modifier = Modifier.animateItem(),
            playerName = it.name,
            isSelected = it.isSelected,
            tokens = it.tokens,
            onClick = { onAction(GameAction.OnPlayerClick(it)) }
        )
    }
}

@Composable
private fun SongRow(
    songItems: List<SongItem>,
    primaryButton: ButtonState,
    phase: GamePhase,
    isSongLoaded: Boolean,
    showGaps: Boolean,
    onlyGapsWithBet: Boolean,
    tokenBets: List<TokenBet>,
    dragState: TokenDragState?,
    isTakeBackEnabled: Boolean,
    onAction: (GameAction) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(songItems.size) {
        val indexOfUnknownSong = songItems.indexOfFirst { it is UnknownSong }
        if (indexOfUnknownSong > 0) listState.animateScrollToItem(indexOfUnknownSong)
    }
    val entries = remember(songItems, showGaps, onlyGapsWithBet, tokenBets) {
        songItems.toTimelineEntries(
            showGaps = showGaps,
            tokenBets = tokenBets,
            onlyGapsWithBet = onlyGapsWithBet
        )
    }
    Column {
        Text(
            modifier = Modifier.padding(top = 16.dp, start = 16.dp),
            text = "Songs: " + songItems
                .filter { it is Song && it.correctLocation != false }
                .size,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.secondary
        )
        LazyRow(
            modifier = Modifier.fillMaxWidth().heightIn(50.dp, 200.dp),
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(entries, key = { entry -> entry.key }) { entry ->
                when (entry) {
                    is TimelineEntry.Gap -> GapSlot(
                        modifier = Modifier.animateItem(),
                        gapIndex = entry.gapIndex,
                        bet = entry.bet,
                        dragState = dragState,
                        onTakeBack = if (isTakeBackEnabled && entry.bet != null) {
                            { onAction(TakeBackToken(entry.gapIndex)) }
                        } else null
                    )

                    is TimelineEntry.Card -> when (val songItem = entry.songItem) {
                        is Song ->
                            SongCard(
                                modifier = Modifier.animateItem(),
                                title = songItem.title.getString(),
                                artist = songItem.artist.getString(),
                                year = songItem.releaseYear.toString(),
                                validation = songItem.correctLocation.toSongCardValidation(),
                                color = songItem.color ?: Color(0xffcfd8dc)
                            )

                        is UnknownSong -> {
                            GuessCard(
                                modifier = Modifier.animateItem(),
                                isSubmitEnabled = phase != GamePhase.GUESSING || isSongLoaded,
                                onClickSubmit = {
                                    onAction(if (phase == GamePhase.GUESSING) Guess else Reveal)
                                },
                                onClickLeft = if (phase == GamePhase.GUESSING && songItem.canMoveLeft) {
                                    { onAction(GameAction.MoveLeft) }
                                } else null,
                                onClickRight = if (phase == GamePhase.GUESSING && songItem.canMoveRight) {
                                    { onAction(GameAction.MoveRight) }
                                } else null
                            )
                        }
                    }
                }
            }
        }

        with(primaryButton) {
            AnimatedVisibility(isVisible) {
                Button(
                    modifier = Modifier.padding(16.dp),
                    title = title?.let { stringResource(it) } ?: "",
                    primary = true,
                    onClick = { onAction(action) }
                )
            }
        }
    }
}

@PreviewLightDark
@Composable
fun GameScreenPreview() {
    HitsterTheme {
        GameScreen(GameUiState(
            currentSong = Song("That's Amore".toText(), "Dean Martin".toText(), 1953),
            players = listOf(
                Player(
                    name = "Timo",
                    songs = listOf(
                        Song("That's Amore".toText(), "Dean Martin".toText(), 1953),
                        Song("Alles nur geklaut".toText(), "Die Prinzen".toText(), 1993),
                        UnknownSong(),
                        Song("Anyone".toText(), "Justin Bieber".toText(), 2021)
                    ),
                    isCurrentPlayer = true,
                    isSelected = true,
                    tokens = 2
                ),
                Player("Luno", emptyList(), false, false, 1),
                Player("Lily", emptyList(), false, false, 0)
            ),
            musicButton = MusicButtonItem.PAUSE,
            primaryButton = ButtonState(
                isVisible = true,
                title = R.string.game_buttonReveal,
                action = Reveal
            ),
            phase = GamePhase.TOKEN_PLACEMENT,
            tokenBets = listOf(TokenBet(playerName = "Lily", gapIndex = 0))
        ), {})
    }
}
