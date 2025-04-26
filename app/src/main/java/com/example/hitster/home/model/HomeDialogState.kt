package com.example.hitster.home.model

sealed interface HomeDialogState {
    data object Closed : HomeDialogState
    data class DeleteDialog(val text: String, val action: HomeAction) : HomeDialogState
    data class SelectionDialog(
        val text: String,
        val selectionItems: List<PlaylistSelectionItem>,
        val action: HomeAction
    ) : HomeDialogState
}