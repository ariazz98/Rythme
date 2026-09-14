package com.aria.rythme.feature.pitch.presentation

import com.aria.rythme.feature.pitch.data.DetectedPitch
import com.aria.rythme.feature.pitch.data.PitchFrame
import org.junit.Assert.*
import org.junit.Test

class PitchTraceVisibilityTest {
    private fun frame(time: Long, voiced: Boolean = true) = PitchFrame(time, if (voiced) DetectedPitch(220f, .9f) else null)
    @Test fun isolatedValidReadingsSurviveSilence() {
        assertEquals(listOf(1, 3), isolatedPitchIndices(listOf(frame(0, false), frame(32), frame(64, false), frame(96), frame(128, false))))
        assertEquals(listOf(0), isolatedPitchIndices(listOf(frame(32))))
    }
    @Test fun continuousTraceRemainsALineButTimeGapsLeaveVisibleDots() {
        assertTrue(isolatedPitchIndices(listOf(frame(0), frame(32), frame(64))).isEmpty())
        assertEquals(listOf(0, 1), isolatedPitchIndices(listOf(frame(0), frame(128))))
    }
    @Test fun microphoneWindowBehindPlaybackStillHasALivePoint() {
        val frames = listOf(frame(936), frame(968))
        assertEquals(frames.last(), latestVisiblePitch(frames, 1032))
    }
    @Test fun silenceOrStaleInputNeverHoldsAnOldPitch() {
        assertNull(latestVisiblePitch(listOf(frame(936), frame(968, false)), 1032))
        assertNull(latestVisiblePitch(listOf(frame(0)), 201))
    }
    @Test fun bufferedFutureSamplesDoNotHideAlreadyAvailableReading() {
        val frames = listOf(frame(936), frame(968), frame(1032))
        assertEquals(frames[1], latestVisiblePitch(frames, 1000))
        assertNull(latestVisiblePitch(frames, 900))
    }
}
