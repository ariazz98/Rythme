package com.aria.rythme.feature.home.presentation

import com.aria.rythme.core.music.data.repository.*
import org.junit.Assert.*
import org.junit.Test

class ResumeQueueStateTest {
    private val source = ListeningOrigin("album", 8)
    private fun point() = ResumePoint(10, 12500, listOf(10, 20, 10, 30), 2, source,
        ResumeQueueState(2, listOf(1, 0), true, "ONE", false, true))

    @Test fun duplicateSongInAutoplayKeepsItsOccurrenceAndModes() {
        val result = point().withAvailableSongs(setOf(10, 20, 30))!!
        assertEquals(point(), result)
        assertEquals(2, result.queueIndex)
        assertEquals(2, result.queueState!!.orderedCount)
    }
    @Test fun removedFormalSongsRebaseBothBoundaryAndOriginalOrder() {
        val result = point().withAvailableSongs(setOf(10, 30))!!
        assertEquals(listOf(10L, 10L, 30L), result.queueIds)
        assertEquals(1, result.queueIndex)
        assertEquals(1, result.queueState!!.orderedCount)
        assertEquals(listOf(0), result.queueState!!.sourceOrder)
    }
    @Test fun missingCurrentSongDoesNotSelectANeighbor() {
        assertNull(point().withAvailableSongs(setOf(20, 30)))
    }
    @Test fun legacyQueueIsPreservedWithoutInventingAnAlbumBoundary() {
        val result = point().copy(queueState = null).withAvailableSongs(setOf(10, 20, 30))!!
        assertEquals(point().queueIds, result.queueIds)
        assertEquals(2, result.queueIndex)
        assertEquals(ListeningOrigin(), result.origin)
        assertNull(result.queueState)
    }
    @Test fun invalidSourcePermutationFallsBackToFormalOrderOnly() {
        val bad = point().copy(queueState = ResumeQueueState(2, listOf(3, 0)))
        assertEquals(listOf(0, 1), bad.withAvailableSongs(setOf(10, 20, 30))!!.queueState!!.sourceOrder)
    }
}
