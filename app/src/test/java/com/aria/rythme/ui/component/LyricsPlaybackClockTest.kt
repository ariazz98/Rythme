package com.aria.rythme.ui.component

import org.junit.Assert.*
import org.junit.Test

class LyricsPlaybackClockTest {
    private fun ns(ms: Long) = ms * 1_000_000L

    @Test fun advancesContinuouslyPastTheOldExtrapolationLimit() {
        val clock = LyricsPlaybackClock()
        clock.sample(1000, true, 0, ns(0))
        for (time in 16L..1000L step 16) assertEquals(1000 + time, clock.frame(ns(time)))
    }

    @Test fun smallSamplingErrorsDoNotJumpOrReverseTheDisplay() {
        val clock = LyricsPlaybackClock()
        clock.sample(1000, true, 0, ns(0))
        var previous = 1000L
        for (time in 8L..3000L step 8) {
            if (time % 200L == 0L) {
                val error = if (time % 400L == 0L) -20 else 20
                assertEquals(previous, clock.sample(1000 + time + error, true, 0, ns(time)))
            }
            val value = clock.frame(ns(time))
            assertTrue("No pause or catch-up jump at a sample boundary", value - previous in 6L..10L)
            previous = value
        }
        assertTrue(kotlin.math.abs(previous - 4000L) < 30)
    }

    @Test fun convergesToAConsistentSmallErrorWithoutSnapping() {
        val clock = LyricsPlaybackClock()
        clock.sample(1000, true, 0, ns(0))
        var value = 1000L
        for (time in 10L..3000L step 10) {
            if (time % 200L == 0L) clock.sample(1000 + time + 40, true, 0, ns(time))
            value = clock.frame(ns(time))
        }
        assertTrue(kotlin.math.abs(value - 4040L) < 5)
    }

    @Test fun evenSmallExplicitSeeksSynchronizeImmediately() {
        val clock = LyricsPlaybackClock()
        clock.sample(1000, true, 0, ns(0))
        clock.frame(ns(200))
        assertEquals(1100L, clock.sample(1100, true, 1, ns(200)))
        assertEquals(1116L, clock.frame(ns(216)))
    }

    @Test fun pauseFreezesAndResumeStartsAtTheActualPosition() {
        val clock = LyricsPlaybackClock()
        clock.sample(1000, true, 0, ns(0))
        clock.frame(ns(100))
        assertEquals(1095L, clock.sample(1095, false, 0, ns(100)))
        assertEquals(1095L, clock.frame(ns(1000)))
        clock.sample(1095, true, 0, ns(1000))
        assertEquals(1111L, clock.frame(ns(1016)))
    }

    @Test fun largeDiscontinuitiesAndStaleFramesDoNotAnimateThroughUnrelatedTime() {
        val clock = LyricsPlaybackClock()
        clock.sample(1000, true, 0, ns(100))
        assertEquals(1000L, clock.frame(ns(90)))
        assertEquals(9000L, clock.sample(9000, true, 0, ns(200)))
        assertEquals(9016L, clock.frame(ns(216)))
    }
}
