package com.aria.rythme.feature.player.presentation

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerPanelMotionTest {
    private val large = Rect(0f, 480f, 393f, 510f)
    private val compact = Rect(114f, 110f, 361f, 140f)

    @Test fun bothDirectionsEndAtTheirRealLayoutBounds() {
        for ((start, end) in listOf(large to compact, compact to large)) {
            assertEquals(start, PlayerPanelMotion.titleBoundsAt(start, end, 0))
            assertEquals(end, PlayerPanelMotion.titleBoundsAt(start, end, 500))
        }
    }

    @Test fun horizontalRelocationIsConfinedToTheInvisibleHandoff() {
        for (ms in 0..150) assertEquals(large.left, PlayerPanelMotion.titleBoundsAt(large, compact, ms).left, 0f)
        for (ms in 200..500) assertEquals(compact.left, PlayerPanelMotion.titleBoundsAt(large, compact, ms).left, 0f)
        assertTrue(PlayerPanelMotion.titleBoundsAt(large, compact, 100).top < large.top)
    }

    @Test fun artworkTimingHasNoOvershootOrReverseMovement() {
        var previous = 0f
        for (ms in 0..500) {
            val next = PlayerPanelMotion.Easing.transform(ms / 500f)
            assertTrue(next >= previous && next in 0f..1f)
            previous = next
        }
    }
}
