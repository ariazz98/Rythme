package com.aria.rythme.feature.player.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PlayerStateTest {
    @Test
    fun duplicateSongsHaveDistinctQueueEntryPresentationIdentity() {
        val playlistSongIds = listOf(42L, 42L)
        val first = queueEntryPresentationIdentity(42L, playlistSongIds, currentIndex = 0)
        val second = queueEntryPresentationIdentity(42L, playlistSongIds, currentIndex = 1)

        assertNotEquals(first, second)
    }

    @Test
    fun sameQueueEntryKeepsStablePresentationIdentity() {
        assertEquals(
            "42:0",
            queueEntryPresentationIdentity(42L, listOf(42L), currentIndex = 0)
        )
    }
}
