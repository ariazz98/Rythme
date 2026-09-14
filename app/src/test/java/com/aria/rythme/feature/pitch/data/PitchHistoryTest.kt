package com.aria.rythme.feature.pitch.data

import org.junit.Assert.*
import org.junit.Test

class PitchHistoryTest {
    @Test fun `old snapshots remain intact after chunk rollover`() {
        var history = PitchHistory.Empty
        repeat(256) { history = history.append(PitchFrame(it * 32L, null)) }
        val old = history
        repeat(300) { history = history.append(PitchFrame((it + 256) * 32L, null)) }
        assertEquals(256, old.size)
        assertEquals(556, history.size)
        assertEquals(255 * 32L, old.last().timeMs)
        assertEquals(256 * 32L, history[256].timeMs)
    }
    @Test fun `long session retains beginning while querying a small late window`() {
        var history = PitchHistory.Empty
        repeat(20000) { history = history.append(PitchFrame((it + 1) * 32L, null)) }
        assertEquals(32L, history.first().timeMs)
        val visible = history.window(630000, 640000)
        assertTrue(visible.size in 310..315)
        assertTrue(visible.first().timeMs >= 630000)
        assertEquals(640000L, visible.last().timeMs)
        assertEquals(96L, history.atOrBefore(100)!!.timeMs)
        assertNull(history.atOrBefore(0))
    }
    @Test fun `pause boundary remains a real gap in history`() {
        val pitch = DetectedPitch(440f, 1f)
        val history = PitchHistory.Empty.append(PitchFrame(1000, pitch, 1064))
            .append(PitchFrame(1064, null)).append(PitchFrame(1128, pitch, 1192))
        assertNull(history.atOrBefore(1100)!!.pitch)
        assertEquals(3, history.window(1000, 1200).size)
    }
}
