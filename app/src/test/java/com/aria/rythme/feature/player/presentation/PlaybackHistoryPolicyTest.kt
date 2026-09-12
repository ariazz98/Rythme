package com.aria.rythme.feature.player.presentation

import com.aria.rythme.core.music.domain.model.shouldRecordPlaybackHistory
import org.junit.Assert.*
import org.junit.Test

class PlaybackHistoryPolicyTest {
    @Test fun firstEntryDoesNotCreateHistory() {
        assertFalse(shouldRecordPlaybackHistory(null, "a", false))
    }
    @Test fun rebuildingCurrentEntryDoesNotCreateHistory() {
        assertFalse(shouldRecordPlaybackHistory("a", "a", false))
    }
    @Test fun singleRepeatCreatesANewOccurrenceEveryTime() {
        repeat(3) { assertTrue(shouldRecordPlaybackHistory("a", "a", true)) }
    }
    @Test fun movingBetweenOccurrencesRecordsBothDirections() {
        assertTrue(shouldRecordPlaybackHistory("a", "b", false))
        assertTrue(shouldRecordPlaybackHistory("b", "a", false))
    }
}
