package com.aria.rythme.feature.pitch.presentation

import org.junit.Assert.*
import org.junit.Test

class PitchViewportTest {
    private fun follow(top: Float, vararg notes: Float) =
        followPitchTop(top, notes.toList(), 20f, 120f, 400f, 26f)

    @Test fun `recent notes stay out of both floating regions`() {
        val top = follow(76f, 60f, 67f, 69f)
        for (note in listOf(60f, 67f, 69f)) assertTrue((top - note) * 20 in 120f..400f)
    }
    @Test fun `high input after dragging low returns below readout`() {
        val top = follow(50f, 83f)
        assertEquals(120f, (top - 83f) * 20, 0.01f)
    }
    @Test fun `low input after dragging high returns above controls`() {
        val top = follow(110f, 33f)
        assertEquals(400f, (top - 33f) * 20, 0.01f)
    }
    @Test fun `wide jump prioritizes latest without distorting pitch intervals`() {
        val top = follow(76f, 33f, 83f)
        assertTrue((top - 83f) * 20 in 120f..400f)
        assertEquals(1000f, (top - 33f) * 20 - (top - 83f) * 20, 0.01f)
    }
    @Test fun `stable input does not move viewport`() { assertEquals(76f, follow(76f, 64f, 65f), 0f) }
    @Test fun `silence retains manually selected range`() { assertEquals(110f, follow(110f), 0f) }
    @Test fun `drag spans all midi notes and clamps at boundaries`() {
        assertEquals(127f, clampPitchTop(300f, 26f), 0f)
        assertEquals(26f, clampPitchTop(-300f, 26f), 0f)
    }
    @Test fun `history browsing clamps and does not track growing duration`() {
        assertEquals(10000.0, clampHistoryEnd(2000.0, 42000.0), 0.0)
        assertEquals(42000.0, clampHistoryEnd(99000.0, 42000.0), 0.0)
        assertEquals(20000.0, clampHistoryEnd(20000.0, 50000.0), 0.0)
        assertEquals(3000.0, clampHistoryEnd(0.0, 3000.0), 0.0)
    }
    @Test fun `cursor advances before history window begins scrolling`() {
        val left = 47.0
        val width = 273.0
        fun cursor(time: Double) = left + (time - pitchWindowStart(time)) / 10000 * width
        assertEquals(left, cursor(0.0), 0.0)
        assertEquals(left + width / 2, cursor(5000.0), 0.0)
        assertEquals(left + width, cursor(10000.0), 0.0)
        assertEquals(left + width, cursor(15000.0), 0.0)
        assertEquals(0.0, pitchWindowStart(9999.0), 0.0)
        assertEquals(1.0, pitchWindowStart(10001.0), 0.0)
        // 早期历史点先保持原位，窗口填满后才向左移动。
        assertEquals(2000.0, 2000 - pitchWindowStart(5000.0), 0.0)
        assertEquals(1000.0, 2000 - pitchWindowStart(11000.0), 0.0)
    }
}
