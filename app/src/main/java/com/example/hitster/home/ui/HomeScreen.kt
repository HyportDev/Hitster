package com.example.hitster.home.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.hitster.R
import com.example.hitster.home.model.HomeAction
import com.example.hitster.home.model.HomeAction.AddPlayer
import com.example.hitster.home.model.HomeAction.AddPlaylistByLink
import com.example.hitster.home.model.HomeAction.AddPlaylistByLinkValueChange
import com.example.hitster.home.model.HomeAction.AddPlaylistFromLibrary
import com.example.hitster.home.model.HomeAction.DismissDialog
import com.example.hitster.home.model.HomeAction.OnDialogItemChecked
import com.example.hitster.home.model.HomeAction.OnPlayerInputChange
import com.example.hitster.home.model.HomeAction.OpenRemovePlayerDialog
import com.example.hitster.home.model.HomeAction.RemovePlaylist
import com.example.hitster.home.model.HomeDialogState
import com.example.hitster.home.model.HomePlayerInputState
import com.example.hitster.home.model.HomeViewState
import com.example.hitster.home.model.PlaylistViewState
import com.example.hitster.home.model.SpotifyItem
import com.example.hitster.home.model.SpotifyLoading
import com.example.hitster.home.ui.dialog.AddPlaylistDialog
import com.example.hitster.home.ui.dialog.DeleteDialogView
import com.example.hitster.home.ui.dialog.SelectionDialogView
import com.example.hitster.ui.Button
import com.example.hitster.ui.ErrorSnackbar
import com.example.hitster.ui.HitsterTheme
import com.example.hitster.ui.InputPlayerCard
import com.example.hitster.ui.PlayerCard
import com.example.hitster.ui.Title
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

@Composable
internal fun HomeScreen(
    state: HomeViewState,
    spotifyState: SpotifyItem,
    event: SharedFlow<Int>,
    onAction: (HomeAction) -> Unit,
    onStartGame: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(event) {
        event.collect {
            snackbarHostState.showSnackbar(message = context.getString(it))
        }
    }

    when(state.dialogState) {
        is HomeDialogState.DeleteDialog -> {
            DeleteDialogView(
                title = state.dialogState.text.getString(),
                onConfirm = { onAction(state.dialogState.action) },
                onDismiss = { onAction(DismissDialog) }
            )
        }
        is HomeDialogState.SelectionDialog -> {
            SelectionDialogView(
                title = state.dialogState.text.getString(),
                items = state.dialogState.selectionItems,
                onCheckboxChange = { item, checked ->
                    onAction(OnDialogItemChecked(item, checked))
                },
                onConfirm = { onAction(state.dialogState.action) },
                onDismiss = { onAction(DismissDialog) }
            )
        }
        is HomeDialogState.PlaylistDialog -> {
            AddPlaylistDialog(
                value = state.dialogState.text.getString(),
                onValueChange = { onAction(AddPlaylistByLinkValueChange(it)) },
                onConfirm = { onAction(state.dialogState.action) },
                onDismiss = { onAction(DismissDialog) }
            )
        }
        HomeDialogState.Closed -> {}
    }

    Column(modifier = Modifier.fillMaxSize()) {
        FlowRow(
            modifier = Modifier.weight(1f).padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PlayerBox(
                modifier = Modifier.defaultMinSize(minWidth = 200.dp),
                players = state.players,
                playerInputState = state.playerInputState,
                onAction = onAction
            )
            
            //HorizontalDivider(modifier = Modifier.fillMaxWidth().padding(top = 32.dp))

            PlaylistBox(
                modifier = Modifier,
                spotifyItem = spotifyState,
                playlists = state.playlists,
                onAction = onAction
            )

        }

        AnimatedVisibility(
            visible = state.players.isNotEmpty() && state.playlists.isNotEmpty(),
            enter = slideInHorizontally(),
            exit = slideOutHorizontally()
        ) {
            Button(
                modifier = Modifier.padding(16.dp),
                title = stringResource(R.string.home_startGame),
                primary = true,
                onClick = onStartGame
            )
        }
    }

    SnackbarHost(hostState = snackbarHostState) { snackbarData ->
        ErrorSnackbar(snackbarData)
    }
}

@Composable
private fun PlayerBox(
    modifier: Modifier = Modifier,
    players: List<String>,
    playerInputState: HomePlayerInputState,
    onAction: (HomeAction) -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Title(text = stringResource(R.string.home_players))
        FlowRow(
            modifier = Modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically)
        ) {
            for (player in players) {
                PlayerCard(playerName = player, onClick = { onAction(OpenRemovePlayerDialog(player)) })
            }
            when(playerInputState) {
                HomePlayerInputState.Closed -> {
                    IconButton(
                        content = {
                            Icon(
                                painter = painterResource(R.drawable.ic_add),
                                contentDescription = stringResource(R.string.home_addPlayer)
                            )
                        },
                        colors = IconButtonDefaults.iconButtonColors().copy(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        onClick = { onAction(OnPlayerInputChange("")) }
                    )
                }
                is HomePlayerInputState.Editing -> {
                    InputPlayerCard(
                        value = playerInputState.value,
                        onValueChange = { onAction(OnPlayerInputChange(it)) },
                        onApply = { onAction(AddPlayer(playerInputState.value)) }
                    )
                }
            }

        }
    }
}

@Composable
private fun PlaylistBox(
    modifier: Modifier,
    spotifyItem: SpotifyItem,
    playlists: List<PlaylistViewState>,
    onAction: (HomeAction) -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Title(text = stringResource(R.string.home_playlists))
            SpotifyView(spotifyItem = spotifyItem)
        }


        for (playlist in playlists) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (playlist) {
                        PlaylistViewState.Error -> Text(
                            text = stringResource(R.string.home_playlistNameError),
                            fontStyle = FontStyle.Italic
                        )

                        PlaylistViewState.Loading -> CircularProgressIndicator(
                            modifier = Modifier
                                .width(32.dp)
                                .padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.secondary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )

                        is PlaylistViewState.Success -> {
                            Text(modifier = Modifier.weight(1f), text = playlist.playlist.name ?: "")

                            IconButton(
                                modifier = Modifier,
                                onClick = { onAction(RemovePlaylist(playlist.playlist)) }
                            ) {
                                Icon(painter = painterResource(R.drawable.ic_cross), null)
                            }
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.home_addPlaylistByLink),
                primary = false,
                onClick = { onAction(AddPlaylistByLink) }
            )
            Button(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.home_addPlaylistFromLibrary),
                primary = false,
                onClick = { onAction(AddPlaylistFromLibrary) }
            )
        }
    }

}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    HitsterTheme {
        HomeScreen(
            state = HomeViewState(
                players = listOf("Timo", "Luno"),
                playlists = listOf(PlaylistViewState.Loading)
            ),
            spotifyState = SpotifyLoading,
            event = MutableSharedFlow()
            , {}, {}
        )
    }
}
