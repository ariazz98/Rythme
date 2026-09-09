package com.aria.rythme.feature.navigationbar.presentation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomBarStateTest {
    @Test
    fun collapsedBarRetainsPreparationUntilThePageReachesTop() {
        val state = BottomBarState(scrollThresholdPx = 48f)
        scroll(state, -48f)
        scroll(state, 24f)
        assertFalse(state.isExpanded)
        assertEquals(1f, state.collapsePreparationProgress, 0.001f)
        scroll(state, 240f)
        assertFalse(state.isExpanded)
        assertEquals(1f, state.collapsePreparationProgress, 0.001f)
        scroll(state, 1f, isAtTop = true)
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
    fun upwardScrollInTheMiddleDoesNotExpandRegardlessOfDistance() {
        val state = BottomBarState(scrollThresholdPx = 48f)

        scroll(state, -48f)
        assertFalse(state.isExpanded)

        scroll(state, 48f)
        assertFalse(state.isExpanded)
        scroll(state, 480f)
        assertFalse(state.isExpanded)
    }

    @Test
    fun unconsumedDragOnStaticContentDoesNotCollapse() {
        val state = BottomBarState(scrollThresholdPx = 48f)

        state.nestedScrollConnection { true }.onPostScroll(
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

    @Test
    fun aFlingCanExpandAtTopButCannotCollapseAwayFromTop() {
        val state = BottomBarState(scrollThresholdPx = 48f)
        scroll(state, -96f, source = NestedScrollSource.SideEffect)
        assertTrue(state.isExpanded)
        scroll(state, -48f)
        scroll(state, 480f, source = NestedScrollSource.SideEffect)
        assertFalse(state.isExpanded)
        scroll(state, 1f, isAtTop = true, source = NestedScrollSource.SideEffect)
        assertTrue(state.isExpanded)
        assertEquals(0f, state.collapsePreparationProgress, 0f)
    }

    @Test
    fun pullingAtTopExpandsEvenWhenContentCannotConsumeMoreScroll() {
        val state = BottomBarState(scrollThresholdPx = 48f)
        scroll(state, -48f)
        state.nestedScrollConnection { true }.onPostScroll(
            consumed = Offset.Zero,
            available = Offset(0f, 1f),
            source = NestedScrollSource.UserInput
        )
        assertTrue(state.isExpanded)
    }

    @Test
    fun unconsumedScrollDoesNotPretendThePageHasReachedTop() {
        val state = BottomBarState(scrollThresholdPx = 48f)
        scroll(state, -48f)
        state.nestedScrollConnection { false }.onPostScroll(
            consumed = Offset.Zero,
            available = Offset(0f, 96f),
            source = NestedScrollSource.UserInput
        )
        assertFalse(state.isExpanded)
    }

    @Test
    fun horizontalScrollAtTopDoesNotExpandAndPositionCallbackStaysLive() {
        val state = BottomBarState(scrollThresholdPx = 48f)
        var isAtTop = false
        val connection = state.nestedScrollConnection { isAtTop }
        scroll(state, -48f)
        connection.onPostScroll(Offset(0f, 96f), Offset.Zero, NestedScrollSource.UserInput)
        assertFalse(state.isExpanded)
        isAtTop = true
        connection.onPostScroll(Offset(96f, 0f), Offset.Zero, NestedScrollSource.UserInput)
        assertFalse(state.isExpanded)
        connection.onPostScroll(Offset(0f, 1f), Offset.Zero, NestedScrollSource.UserInput)
        assertTrue(state.isExpanded)
    }

    @Test
    fun reachingTopClearsOldCollapseDistanceBeforeTheNextDownwardScroll() {
        val state = BottomBarState(scrollThresholdPx = 48f)
        scroll(state, -48f)
        scroll(state, 1f, isAtTop = true)
        scroll(state, -24f)
        assertTrue(state.isExpanded)
        assertEquals(0.5f, state.collapsePreparationProgress, 0.001f)
        scroll(state, -24f)
        assertFalse(state.isExpanded)
    }

    private fun scroll(
        state: BottomBarState,
        deltaY: Float,
        isAtTop: Boolean = false,
        source: NestedScrollSource = NestedScrollSource.UserInput
    ) {
        state.nestedScrollConnection { isAtTop }.onPostScroll(
            consumed = Offset(0f, deltaY),
            available = Offset.Zero,
            source = source
        )
    }
}
