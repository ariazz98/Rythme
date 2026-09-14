package com.aria.rythme.feature.pitch.data

import org.junit.Assert.*
import org.junit.Test
import java.util.Random
import kotlin.math.*

class PitchDetectorTest {
    private val detector = PitchDetector()
    private fun tone(hz: Double, amplitude: Double = 0.2) = FloatArray(2048) { (amplitude * sin(2 * PI * hz * it / 16000)).toFloat() }

    @Test fun stableTonesCoverLowAndHighVocalRange() {
        for (hz in listOf(65.4064, 110.0, 220.0, 261.6256, 440.0, 880.0)) {
            val result = requireNotNull(detector.detect(tone(hz))) { "Missing $hz Hz" }
            assertTrue("$hz Hz became ${result.frequencyHz}", abs(1200 * log2(result.frequencyHz / hz)) < 6)
            assertTrue(result.confidence > 0.9)
        }
    }

    @Test fun harmonicSignalFindsFundamentalInsteadOfStrongSecondHarmonic() {
        val input = FloatArray(2048) { i ->
            val phase = 2 * PI * 220 * i / 16000
            (0.03 * sin(phase) + 0.18 * sin(2 * phase) + 0.09 * sin(3 * phase)).toFloat()
        }
        val result = requireNotNull(detector.detect(input))
        assertEquals(220f, result.frequencyHz, 1f)
    }

    @Test fun silenceNoiseAndVeryQuietInputProduceNoPitch() {
        assertNull(detector.detect(FloatArray(2048)))
        assertNull(detector.detect(FloatArray(2048) { 0.2f }))
        assertNull(detector.detect(tone(440.0, 0.0001)))
        val random = Random(42)
        repeat(5) { assertNull(detector.detect(FloatArray(2048) { (random.nextFloat() - 0.5f) * 0.3f })) }
    }

    @Test fun invalidOrAboveRangeInputDoesNotProduceAReading() {
        assertNull(detector.detect(tone(440.0).apply { this[10] = Float.NaN }))
        assertNull(detector.detect(tone(2000.0)))
    }

    @Test fun noteOctaveAndCentsUseA440Reference() {
        val a4 = DetectedPitch(440f, 1f)
        assertEquals("A4", a4.noteName); assertEquals(0, a4.cents)
        assertEquals("C4", DetectedPitch(261.6256f, 1f).noteName)
        assertEquals("A3", DetectedPitch(220f, 1f).noteName)
        assertEquals(25, DetectedPitch((440 * 2.0.pow(25.0 / 1200)).toFloat(), 1f).cents)
        assertEquals(-25, DetectedPitch((440 * 2.0.pow(-25.0 / 1200)).toFloat(), 1f).cents)
    }

    @Test fun silenceAfterAToneClearsPreviousResult() {
        assertNotNull(detector.detect(tone(440.0)))
        assertNull(detector.detect(FloatArray(2048)))
        assertEquals(220f, requireNotNull(detector.detect(tone(220.0))).frequencyHz, 1f)
    }

    @Test fun lowGainMicrophoneCanDetectQuietPeriodicSoundWithoutAcceptingQuietNoise() {
        assertEquals(440f, requireNotNull(detector.detect(tone(440.0, 0.0006))).frequencyHz, 1f)
        val random = Random(19)
        repeat(5) { assertNull(detector.detect(FloatArray(2048) { (random.nextFloat() - 0.5f) * 0.002f })) }
    }
}
