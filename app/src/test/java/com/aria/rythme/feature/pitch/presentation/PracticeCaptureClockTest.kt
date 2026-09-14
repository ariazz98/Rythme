package com.aria.rythme.feature.pitch.presentation

import com.aria.rythme.feature.pitch.data.PitchFrame
import org.junit.Assert.*
import org.junit.Test

class PracticeCaptureClockTest {
    @Test fun batchedFramesKeepEverySampleInterval() {
        val clock = PracticeCaptureClock()
        val mapped = (0..3).map { n -> clock.map(PitchFrame(64 + n * 32L, null, 128 + n * 32L), 1000) }
        assertEquals(listOf(936L, 968L, 1000L, 1032L), mapped.map { it.timeMs })
    }
    @Test fun playerClockCorrectionDoesNotDropOrStretchSamples() {
        val clock = PracticeCaptureClock()
        val first = clock.map(PitchFrame(64, null, 128), 1000)
        val second = clock.map(PitchFrame(96, null, 160), 900)
        assertEquals(32L, second.timeMs - first.timeMs)
    }
    @Test fun newSegmentReanchorsAfterSeek() {
        val first = PracticeCaptureClock().map(PitchFrame(64, null, 128), 50_000)
        val afterSeek = PracticeCaptureClock().map(PitchFrame(64, null, 128), 1000)
        assertEquals(49_936L, first.timeMs); assertEquals(936L, afterSeek.timeMs)
    }
}
