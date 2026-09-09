package com.aria.rythme.feature.home.presentation

import com.aria.rythme.core.music.data.repository.ListeningOrigin
import com.aria.rythme.core.music.data.repository.ListeningRecord
import com.aria.rythme.core.music.data.repository.rememberListening
import org.junit.Assert.*
import org.junit.Test

class HomeHistoryTest {
    @Test fun recentListeningMovesTheSongToFrontWithoutDuplicatingIt() {
        val source = ListeningOrigin("playlist", 12)
        val first = ListeningRecord(1, ListeningOrigin(), 1)
        val other = ListeningRecord(2, ListeningOrigin(), 2)
        val latest = ListeningRecord(1, source, 3)
        assertEquals(listOf(latest, other), rememberListening(listOf(other, first), latest))
    }

    @Test fun recentListeningIsBoundedAndKeepsTheNewestSource() {
        val previous = (1L..70L).map { ListeningRecord(it, ListeningOrigin(), it) }
        val record = ListeningRecord(100, ListeningOrigin("album", 6, composer = "test"), 100)
        val result = rememberListening(previous, record)
        assertEquals(50, result.size)
        assertEquals(record, result.first())
        assertEquals(49L, result.last().songId)
    }

    @Test fun resumeKeepsTheSecondOccurrenceOfTheSameSong() {
        assertEquals(2, restoredQueueIndex(listOf(1, 2, 1, 3), 2, setOf(1, 2, 3)))
    }

    @Test fun removedEarlierSongsShiftResumeIndexWithoutChangingOccurrence() {
        assertEquals(1, restoredQueueIndex(listOf(1, 2, 1, 3), 2, setOf(1, 3)))
    }

    @Test fun aMissingCurrentSongOrInvalidIndexDoesNotStartAnotherSong() {
        assertNull(restoredQueueIndex(listOf(1, 2, 3), 1, setOf(1, 3)))
        assertNull(restoredQueueIndex(listOf(1, 2, 3), 9, setOf(1, 2, 3)))
        assertNull(restoredQueueIndex(emptyList(), 0, emptySet()))
    }
}
