package com.aria.rythme.feature.navigationbar.presentation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomBarStateTest {
    @Test
    fun collapsedBarRetainsPreparationUntilExpansionThreshold() {
        val state = BottomBarState(scrollThresholdPx = 48f)
        scroll(state, -48f)
        scroll(state, 24f)
        assertFalse(state.isExpanded)
        assertEquals(1f, state.collapsePreparationProgress, 0.001f)
        scroll(state, 24f)
        assertTrue(state.isExpanded)
        assertEquals(0f, state.collapsePreparationProgress, 0.001f)
    }

    @Test
    fun preparationProgressTracksOnlyDownwardDistanceAndDoesNotJumpAtThreshold() {
        val state = BottomBarState(scrollThresholdPx = 48f)
        scroll(state, -12f)
        assertEquals(0.25f, state.collapsePreparationProgress, 0.001f)
        scroll(state, -12f)
        assertEquals(0.5f, state.collapsePreparationProgress, 0.001f)
        scroll(state, -24f)
        assertFalse(state.isExpanded)
        assertEquals(1f, state.collapsePreparationProgress, 0.001f)
        scroll(state, -100f)
        assertEquals(1f, state.collapsePreparationProgress, 0.001f)
    }

    @Test
    fun reversalAndTabSelectionClearPreparation() {
        val state = BottomBarState(scrollThresholdPx = 48f)
        scroll(state, -24f)
        scroll(state, 1f)
        assertEquals(0f, state.collapsePreparationProgress, 0.001f)
        scroll(state, -24f)
        state.onTabSelected(3)
        assertTrue(state.isExpanded)
        assertEquals(0f, state.collapsePreparationProgress, 0.001f)
    }

    @Test
    fun downwardScrollCollapsesOnlyAfterAccumulatedThreshold() {
        val state = BottomBarState(scrollThresholdPx = 48f)

        scroll(state, -24f)
        assertTrue(state.isExpanded)

        scroll(state, -24f)
        assertFalse(state.isExpanded)
    }

    @Test
    fun changingDirectionResetsAccumulatedDistance() {
        val state = BottomBarState(scrollThresholdPx = 48f)

        scroll(state, -30f)
        scroll(state, 20f)
        scroll(state, -20f)
        assertTrue(state.isExpanded)

        scroll(state, -28f)
        assertFalse(state.isExpanded)
    }

    @Test
    fun upwardScrollExpandsCollapsedBar() {
        val state = BottomBarState(scrollThresholdPx = 48f)

        scroll(state, -48f)
        assertFalse(state.isExpanded)

        scroll(state, 48f)
        assertTrue(state.isExpanded)
    }

    @Test
    fun unconsumedDragOnStaticContentDoesNotCollapse() {
        val state = BottomBarState(scrollThresholdPx = 48f)

        state.nestedScrollConnection.onPostScroll(
            consumed = Offset.Zero,
            available = Offset(0f, -96f),
            source = NestedScrollSource.UserInput
        )

        assertTrue(state.isExpanded)
    }

    @Test
    fun searchDoesNotReplaceLastPrimaryTabAndAnyTapExpands() {
        val state = BottomBarState(scrollThresholdPx = 48f)

        state.onTabSelected(2)
        scroll(state, -48f)
        assertFalse(state.isExpanded)

        state.onTabSelected(3)
        assertTrue(state.isExpanded)
        assertEquals(2, state.lastPrimaryTabIndex)
    }

    private fun scroll(state: BottomBarState, deltaY: Float) {
        state.nestedScrollConnection.onPostScroll(
            consumed = Offset(0f, deltaY),
            available = Offset.Zero,
            source = NestedScrollSource.UserInput
        )
    }
}
