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
import com.example.hitster.game.model.ButtonState
import com.example.hitster.game.model.GameAction
import com.example.hitster.game.model.GameAction.Guess
import com.example.hitster.game.model.GameViewState
import com.example.hitster.game.model.Player
import com.example.hitster.game.model.PlayerItem
import com.example.hitster.game.model.Song
import com.example.hitster.game.model.SongItem
import com.example.hitster.game.model.UnknownSong
import com.example.hitster.game.view.SongCardValidation.Companion.toSongCardValidation
import com.example.hitster.res.toText
import com.example.hitster.ui.Button
import com.example.hitster.ui.HitsterTheme
import com.example.hitster.ui.PlayerCard

@Composable
internal fun GameScreen(state: GameViewState, onAction: (GameAction) -> Unit) {
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
                    playerList(state.playerItems, onAction)
                }
                SongRow(state.songItems, state.primaryButton, onAction)
            }
        } else {
            Column(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    playerList(state.playerItems, onAction)
                }
                SongRow(state.songItems, state.primaryButton, onAction)
            }
        }

        MusicButtonSection(state.musicButton, state.primaryButton.action, onAction)
    }
}

private fun LazyListScope.playerList(playerItems: List<PlayerItem>, onAction: (GameAction) -> Unit) {
    items(playerItems, key = { it.playerName }) {
        PlayerCard(
            modifier = Modifier.animateItem(),
            playerName = it.playerName,
            selected = it.isSelected,
            onClick = { onAction(GameAction.OnSelectPlayer(it)) }
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
        GameScreen(GameViewState.initial().copy(currentPlayer = Player(
            name = "Luno",
            songs = listOf(
                Song("That's Amore".toText(), "Dean Martin".toText(), 1953),
                Song("Alles nur geklaut".toText(), "Die Prinzen".toText(), 1993),
                Song("Anyone".toText(), "Justin Bieber".toText(), 2021)
            )
        ),
            playerItems = listOf(
                PlayerItem("Timo", true, true),
                PlayerItem("Luno", false, false),
                PlayerItem("Lily", false, false)
            ),
            songItems = listOf(
                Song("That's Amore".toText(), "Dean Martin".toText(), 1953),
                Song("Alles nur geklaut".toText(), "Die Prinzen".toText(), 1993),
                Song("Anyone".toText(), "Justin Bieber".toText(), 2021)
            )), {})
    }
}
