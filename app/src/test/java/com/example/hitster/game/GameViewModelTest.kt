package com.example.hitster.game

import app.cash.turbine.test
import com.example.hitster.game.data.AccessTokenProvider
import com.example.hitster.game.model.Player
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GameViewModelTest {

 private val viewModel = GameViewModel(AccessTokenProvider())

 private val players = listOf(Player(name = "Tim"), Player("Tom"))

 @Test
 fun `first player becomes current player during initialisation`() = runTest {
  viewModel.viewState.test {
   awaitItem()
   viewModel.init(players = players.map { it.name}, playlists = emptyList())
   val result = awaitItem()
   assertEquals("Tim", result.currentPlayer!!.name)
   assertEquals(1, result.currentPlayer!!.songs.size) // Starting point
  }
 }


}