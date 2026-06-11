package com.example.hitster.game.view

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hitster.R
import com.example.hitster.game.model.ButtonState
import com.example.hitster.game.model.GameAction
import com.example.hitster.game.model.GameAction.AddToken
import com.example.hitster.game.model.GameAction.Guess
import com.example.hitster.game.model.GameUiState
import com.example.hitster.game.model.MusicButtonItem
import com.example.hitster.game.model.Player
import com.example.hitster.game.model.Song
import com.example.hitster.game.model.SongItem
import com.example.hitster.game.model.UnknownSong
import com.example.hitster.game.view.SongCardValidation.Companion.toSongCardValidation
import com.example.hitster.res.toText
import com.example.hitster.ui.Button
import com.example.hitster.ui.HitsterTheme
import com.example.hitster.ui.PlayerCard

@Composable
internal fun GameScreen(state: GameUiState, onAction: (GameAction) -> Unit) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLandscape) {
            Row(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxHeight().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    playerList(state.players, onAction)
                }
                state.players.find { it.isSelected }?.let {
                    SongRow(it.songs, state.primaryButton, onAction)
                }
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
                state.players.find { it.isSelected }?.let {
                    SongRow(it.songs, state.primaryButton, onAction)
                }
                AnimatedVisibility(state.isAddTokenButtonVisible && state.players.any { it.isSelected && it.isCurrentPlayer}) {
                    Button(
                        modifier = Modifier.padding(
                            start = 16.dp,
                            end = 16.dp
                        ),
                        title = stringResource(R.string.game_add_token),
                        primary = true,
                        onClick = {
                            state.players.find { it.isSelected }?.let {
                                onAction(AddToken(it))
                            }
                        }
                    )
                }
            }
        }

        MusicButtonSection(state.musicButton, state.primaryButton.action, onAction)
    }
}

private fun LazyListScope.playerList(playerItems: List<Player>, onAction: (GameAction) -> Unit) {
    items(playerItems, key = { it.name }) {
        PlayerCard(
            modifier = Modifier.animateItem(),
            playerName = it.name,
            selected = it.isSelected,
            tokens = it.tokens,
            onClick = { onAction(GameAction.OnPlayerClick(it)) }
        )
    }
}

@Composable
private fun SongRow(
    songItems: List<SongItem>,
    primaryButton: ButtonState,
    onAction: (GameAction) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(songItems.size) {
        val indexOfUnknownSong = songItems.indexOfFirst { it is UnknownSong }
        if (indexOfUnknownSong > 0) listState.animateScrollToItem(indexOfUnknownSong)
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
            items(songItems, key = { item -> item.id }) { songItem ->
                when (songItem) {
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
                            onClickSubmit = { onAction(Guess) },
                            onClickLeft = if (songItem.canMoveLeft) {
                                { onAction(GameAction.MoveLeft) }
                            } else null,
                            onClickRight = if (songItem.canMoveRight) {
                                { onAction(GameAction.MoveRight) }
                            } else null
                        )
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

@Preview(showBackground = true)
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
                title = R.string.game_buttonGuess,
                action = Guess
            )
        ), {})
    }
}
