package com.aria.rythme.feature.player.presentation

import com.aria.rythme.ui.component.PlaylistPanelState
import com.aria.rythme.ui.component.queueScrollThumb
import org.junit.Assert.*
import org.junit.Test

class PlayerQueueDetailsTest {
    @Test fun elasticSwitchKeepsItsExistingHalfViewportThreshold() {
        val state = PlaylistPanelState()
        state.contentHeightPx = 800f
        assertEquals(400f, state.switchThresholdPx, 0f)
    }
    @Test fun recentHistoryStartsAtTheBottomWhileUpcomingStartsAtTheTop() {
        val queue = queueScrollThumb(20, 54f, 400f, 0, 0, false, 18f)!!
        val history = queueScrollThumb(20, 54f, 400f, 0, 0, true, 18f)!!
        assertEquals(0f, queue.top, .001f)
        assertEquals(400f, history.top + history.height, .001f)
    }
    @Test fun shortHistoryUsesItsMeasuredHeightWithoutRemovingElasticThresholds() {
        val state = PlaylistPanelState()
        state.contentHeightPx = 800f
        state.historyContentHeightPx = 44f + 2 * 54f
        assertEquals(152f, state.historyExtentPx, 0f)
        assertEquals(76f, state.switchThresholdPx, 0f)
        state.historyContentHeightPx = 1000f
        assertEquals(800f, state.historyExtentPx, 0f)
        assertEquals(400f, state.switchThresholdPx, 0f)
    }
    @Test fun shortListsNeedNoScrollbarAndExtentsStayInsideTheViewport() {
        assertNull(queueScrollThumb(2, 54f, 400f, 0, 0, false, 18f))
        val thumb = queueScrollThumb(20, 54f, 400f, 100, 100, false, 18f)!!
        assertEquals(400f, thumb.top + thumb.height, .001f)
    }
}
