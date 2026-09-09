package com.aria.rythme.ui.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import org.junit.Assert.*
import org.junit.Test

class CollapsibleHeaderStateTest {
    @Test fun upwardDragConsumesOnlyTheRemainingSearchHeight() {
        val state = CollapsibleHeaderState(56f, 56f)
        val connection = state.nestedScrollConnection
        assertEquals(Offset(0f, -20f), connection.onPreScroll(Offset(0f, -20f), NestedScrollSource.UserInput))
        assertEquals(36f, state.currentOffset, 0f)
        assertEquals(Offset(0f, -36f), connection.onPreScroll(Offset(0f, -100f), NestedScrollSource.UserInput))
        assertEquals(0f, state.currentOffset, 0f)
    }

    @Test fun downwardDragExpandsOnlyAfterTheListLeavesUnconsumedScroll() {
        val state = CollapsibleHeaderState(56f, 0f)
        val connection = state.nestedScrollConnection
        assertEquals(Offset.Zero, connection.onPreScroll(Offset(0f, 100f), NestedScrollSource.UserInput))
        assertEquals(Offset.Zero, connection.onPostScroll(Offset(0f, 100f), Offset.Zero, NestedScrollSource.UserInput))
        assertEquals(0f, state.currentOffset, 0f)
        assertEquals(Offset(0f, 56f), connection.onPostScroll(Offset.Zero, Offset(0f, 100f), NestedScrollSource.UserInput))
        assertEquals(1f, state.searchFraction, 0f)
    }
}
