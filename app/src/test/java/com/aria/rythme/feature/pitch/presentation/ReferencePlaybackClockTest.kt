package com.aria.rythme.feature.pitch.presentation

import org.junit.Assert.*
import org.junit.Test

class ReferencePlaybackClockTest {
    @Test fun serviceClockCorrectionCannotReverseOrFreezeContinuousPlayback() {
        val clock = ReferencePlaybackClock()
        var previous = clock.sample(1000, 0, true, 0)
        for ((index, raw) in listOf(1016L, 996L, 1012L, 1028L, 1044L).withIndex()) {
            val position = clock.sample(raw, 0, true, (index + 1) * 16_000_000L)
            assertTrue("Must keep moving forward", position > previous)
            assertTrue("No abrupt frame speed change", position - previous in 12..20)
            previous = position
        }
    }
    @Test fun actualSeekCanMoveBackEvenBySmallDistance() {
        val clock = ReferencePlaybackClock()
        clock.sample(1000, 0, true, 0)
        assertEquals(980L, clock.sample(980, 1, true, 16_000_000))
        assertEquals(500L, clock.sample(500, 2, false, 32_000_000))
        assertEquals(510L, clock.sample(510, 2, true, 42_000_000))
    }
    @Test fun pausedClockDoesNotRunAndLongAbsenceUsesFreshPosition() {
        val clock = ReferencePlaybackClock()
        clock.sample(1000, 0, false, 0)
        assertEquals(1000L, clock.sample(1000, 0, false, 500_000_000))
        assertEquals(11000L, clock.sample(11000, 0, true, 10_000_000_000))
    }
    @Test fun stalePlayingFlagCannotExtrapolateFarBeyondAudio() {
        val clock = ReferencePlaybackClock()
        clock.sample(1000, 0, true, 0)
        var value = 0L
        repeat(300) { value = clock.sample(1000, 0, true, (it + 1) * 16_000_000L) }
        assertTrue(value in 1000L..1200L)
    }

}
