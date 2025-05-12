package com.example.hitster.home.model

import com.example.hitster.res.Text

sealed interface HomeDialogState {
    data object Closed : HomeDialogState
    data class DeleteDialog(val text: Text, val action: HomeAction) : HomeDialogState
    data class PlaylistDialog(val text: Text, val action: HomeAction) : HomeDialogState
    data class SelectionDialog(
        val text: Text,
        val selectionItems: List<PlaylistSelectionItem>,
        val action: HomeAction
    ) : HomeDialogState
}