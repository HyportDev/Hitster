package com.example.hitster.game.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hitster.game.model.GameAction
import com.example.hitster.game.model.GameViewState
import com.example.hitster.game.model.Player
import com.example.hitster.game.model.PlayerItem
import com.example.hitster.game.model.Song
import com.example.hitster.game.model.SongItemColor
import com.example.hitster.game.model.SongItemColor.AQUA
import com.example.hitster.game.model.SongItemColor.BLUE
import com.example.hitster.game.model.SongItemColor.GREEN
import com.example.hitster.game.model.SongItemColor.LIGHT_PURPLE
import com.example.hitster.game.model.SongItemColor.PURPLE
import com.example.hitster.game.model.SongItemColor.YELLOW
import com.example.hitster.game.model.UnknownSong
import com.example.hitster.game.view.SongCardValidation.Companion.toSongCardValidation
import com.example.hitster.ui.Button
import com.example.hitster.ui.PlayerCard

@Composable
internal fun GameScreen(state: GameViewState, onAction: (GameAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(WindowInsets.systemBars.asPaddingValues()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.playerItems) {
                    PlayerCard(
                        playerName = it.playerName,
                        selected = it.isSelected,
                        onClick = { onAction(GameAction.OnSelectPlayer(it)) }
                    )
                }
            }

            HorizontalPager(
                modifier = Modifier.padding(vertical = 16.dp),
                state = rememberPagerState { state.songItems.size },
                pageSize = PageSize.Fixed(200.dp),
                pageSpacing = 8.dp,
                contentPadding = PaddingValues(16.dp)
            ) { page ->
                when(val songItem = state.songItems[page]) {
                    is Song ->
                        SongCard(
                            title = songItem.title,
                            artist = songItem.artist,
                            year = songItem.releaseYear.toString(),
                            validation = songItem.correctLocation.toSongCardValidation(),
                            color = songItem.color.getColor()
                        )
                    UnknownSong -> GuessCard(
                        onClickLeft = { onAction(GameAction.MoveLeft) },
                        onClickRight = { onAction(GameAction.MoveRight) }
                    )
                }
            }

            with(state.primaryButton) {
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


        IconButton(
            modifier = Modifier.size(100.dp).padding(bottom = 4.dp),
            colors = IconButtonDefaults.filledIconButtonColors(),
            onClick = { onAction(state.musicButton.action) }
        ) {
            Icon(
                modifier = Modifier.size(50.dp),
                painter = painterResource(state.musicButton.icon),
                contentDescription = null
            )
        }
    }
}

private fun SongItemColor?.getColor(): Color {
    return when(this) {
        YELLOW -> Color(0xfffbc02d)
        GREEN -> Color(0xff8bc34a)
        AQUA -> Color(0xffb2dfdb)
        LIGHT_PURPLE -> Color(0xffe1bee7)
        BLUE -> Color(0xff03a9f4)
        PURPLE -> Color(0xff9c27b0)
        null -> Color(0xffcfd8dc)
    }
}

@Preview(showBackground = true)
@Composable
fun GameScreenPreview() {
    MaterialTheme {
        GameScreen(GameViewState.initial().copy(currentPlayer = Player(
            name = "Luno",
            songs = listOf(
                Song("That's Amore", "Dean Martin", 1953),
                Song("Alles nur geklaut", "Die Prinzen", 1993),
                Song("Anyone", "Justin Bieber", 2021)
            )
        ),
            playerItems = listOf(
                PlayerItem("Timo", true, true),
                PlayerItem("Luno", false, false),
                PlayerItem("Lily", false, false)
            ),
            songItems = listOf(
            Song("That's Amore", "Dean Martin", 1953),
            Song("Alles nur geklaut", "Die Prinzen", 1993),
            Song("Anyone", "Justin Bieber", 2021)
        )), {})
    }
}