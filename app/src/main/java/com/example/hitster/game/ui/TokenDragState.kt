package com.example.hitster.game.ui

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect

/**
 * Connects the draggable tokens with the gap slots of the timeline. Tokens are dragged across
 * composables, so every position is kept in root coordinates.
 */
@Stable
internal class TokenDragState {

    private val gapBounds = mutableStateMapOf<Int, Rect>()

    /** The player whose token is currently in the air, null while nothing is dragged. */
    var draggedPlayerName by mutableStateOf<String?>(null)
        private set

    var dragPosition by mutableStateOf(Offset.Zero)
        private set

    /** The gap the dragged token would land in, null when it is not above any gap. */
    var hoveredGapIndex by mutableStateOf<Int?>(null)
        private set

    fun registerGap(gapIndex: Int, bounds: Rect) {
        gapBounds[gapIndex] = bounds
    }

    fun unregisterGap(gapIndex: Int) {
        gapBounds.remove(gapIndex)
    }

    fun startDrag(playerName: String, position: Offset) {
        draggedPlayerName = playerName
        dragPosition = position
        hoveredGapIndex = gapAt(position)
    }

    fun drag(position: Offset) {
        dragPosition = position
        hoveredGapIndex = gapAt(position)
    }

    /** Ends the drag and returns the gap the token was dropped on, or null when it missed. */
    fun endDrag(): Int? {
        val droppedGapIndex = hoveredGapIndex
        draggedPlayerName = null
        hoveredGapIndex = null
        return droppedGapIndex
    }

    private fun gapAt(position: Offset): Int? = gapBounds.entries
        .filter { it.value.contains(position) }
        .minByOrNull { (it.value.center - position).getDistanceSquared() }
        ?.key
}
