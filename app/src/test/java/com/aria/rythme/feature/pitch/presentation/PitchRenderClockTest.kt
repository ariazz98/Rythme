package com.aria.rythme.feature.pitch.presentation

import org.junit.Assert.*
import org.junit.Test

class PitchRenderClockTest {
    @Test fun `vsync advances between detector packets`() {
        val clock = PitchRenderClock()
        clock.accept(1000, 1_000_000_000)
        assertEquals(944.0, clock.timeMs(1_008_000_000), 0.001)
        assertEquals(952.0, clock.timeMs(1_016_000_000), 0.001)
        clock.accept(1032, 1_032_000_000)
        assertEquals(976.0, clock.timeMs(1_040_000_000), 0.001)
    }
    @Test fun `missing input never creates future data`() {
        val clock = PitchRenderClock()
        clock.accept(1000, 1_000_000_000)
        assertEquals(1000.0, clock.timeMs(9_000_000_000), 0.0)
    }
    @Test fun `delayed packet does not reset phase`() {
        val clock = PitchRenderClock()
        clock.accept(1000, 1_000_000_000)
        clock.timeMs(1_016_000_000)
        clock.accept(1032, 1_052_000_000)
        assertEquals(992.0, clock.timeMs(1_056_000_000), 0.001)
    }
    @Test fun `new capture resets old time`() {
        val clock = PitchRenderClock()
        clock.accept(10000, 1_000_000_000)
        clock.timeMs(1_016_000_000)
        clock.accept(64, 2_000_000_000)
        assertEquals(16.0, clock.timeMs(2_016_000_000), 0.001)
    }
}
