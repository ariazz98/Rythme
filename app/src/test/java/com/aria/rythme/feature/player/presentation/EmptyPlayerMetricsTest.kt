package com.aria.rythme.feature.player.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerLayoutMetricsTest {
    @Test fun phoneDimensionsFollowTheReferenceWidthRatherThanPausedArtwork() {
        val width = 1200f / 3.25f
        val scale = PlayerLayoutMetrics.scale(width)
        assertEquals(252f / 393f, PlayerLayoutMetrics.CoverSize * scale / width, .0001f)
        assertEquals(60f / 4f, PlayerLayoutMetrics.HandleWidth / PlayerLayoutMetrics.HandleHeight, 0f)
        assertEquals(37f * 1200f / 393f, PlayerLayoutMetrics.PlaySize * scale * 3.25f, .001f)
    }

    @Test fun largeOrUnknownWindowsDoNotEnlargeTheReferenceControls() {
        for (width in listOf(393f, 800f, 0f, -1f, Float.NaN)) {
            assertEquals(1f, PlayerLayoutMetrics.scale(width), 0f)
        }
    }
}
