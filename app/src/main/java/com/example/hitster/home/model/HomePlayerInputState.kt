package com.example.hitster.home.model

sealed interface HomePlayerInputState {
    data object Closed : HomePlayerInputState
    data class Editing(val value: String) : HomePlayerInputState
}