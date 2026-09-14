package com.aria.rythme.feature.pitch.data

import org.junit.Assert.*
import org.junit.Test

class PracticeTakesTest {
    @Test fun backwardJumpRetainsOriginalTimesAndSeparatesPaths() {
        val before = PracticeTakes().append(PitchFrame(5000, null)).append(PitchFrame(5032, null))
        val after = before.beginSegment().append(PitchFrame(1000, null)).append(PitchFrame(1032, null))
        assertEquals(listOf(5000L, 5032L), after.older.single().map { it.timeMs })
        assertEquals(listOf(1000L, 1032L), after.current.map { it.timeMs })
        assertTrue(before.older.isEmpty())
    }
    @Test fun singingSameRangeAgainPreservesBothTakes() {
        val first = PracticeTakes().append(PitchFrame(1000, DetectedPitch(220f, 1f)))
        val repeated = first.beginSegment().append(PitchFrame(1000, DetectedPitch(440f, 1f)))
        assertEquals(220f, repeated.older.single().single().pitch!!.frequencyHz, .01f)
        assertEquals(440f, repeated.current.single().pitch!!.frequencyHz, .01f)
    }
    @Test fun repeatedSeeksWithoutSingingDoNotCreateEmptyTakes() {
        assertEquals(PracticeTakes(), PracticeTakes().beginSegment().beginSegment())
    }
    @Test fun clockCorrectionCannotUnsortCurrentSegment() {
        val takes = PracticeTakes().append(PitchFrame(1000, null)).append(PitchFrame(950, null)).append(PitchFrame(1032, null))
        assertEquals(listOf(1000L, 1032L), takes.current.map { it.timeMs })
    }
}
