package com.aria.rythme.feature.player.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PlayerStateTest {
    @Test
    fun emptyQueueHasNoCurrentPresentationIdentityOrTransportActions() {
        val state = PlayerState()

        assertEquals("empty", state.currentQueueEntryIdentity)
        assertFalse(state.canPlayPrevious)
        assertFalse(state.canPlayNext)
        assertFalse(state.canShowLyrics)
    }
}
