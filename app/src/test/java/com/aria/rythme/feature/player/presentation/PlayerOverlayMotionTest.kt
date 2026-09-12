package com.aria.rythme.feature.player.presentation

import org.junit.Assert.*
import org.junit.Test

class PlayerOverlayMotionTest {
    @Test fun gestureDismissalCarriesReleaseSpeedWithoutOvershooting() {
        for (velocity in listOf(500f, 3000f, 12000f)) {
            val travel = 1500f
            val motion = PlayerDismissMotion(velocity, travel)
            assertTrue(motion.durationMs in 1..PlayerOverlayMotion.CollapseMs)
            val values = (0..100).map { motion.easing.transform(it / 100f) }
            assertEquals(0f, values.first(), .001f)
            assertEquals(1f, values.last(), .001f)
            assertTrue(values.all { it in 0f..1f })
            assertTrue(values.zipWithNext().all { (a, b) -> b >= a })
            val initialSlope = motion.easing.transform(.001f) / .001f
            assertEquals(velocity / travel * motion.durationMs / 1000f, initialSlope, .04f)
        }
    }

    @Test fun tapDismissalKeepsOriginalTiming() {
        val motion = PlayerDismissMotion()
        assertEquals(PlayerOverlayMotion.CollapseMs, motion.durationMs)
        assertSame(PlayerOverlayMotion.CollapseEasing, motion.easing)
    }

    @org.junit.Test
    fun sourceGlassIsSkippedOnlyWhileCoveredAndRestoresImmediatelyOnClose() {
        org.junit.Assert.assertTrue(shouldDrawMiniPlayerGlass(false, false))
        org.junit.Assert.assertTrue(shouldDrawMiniPlayerGlass(true, false))
        org.junit.Assert.assertFalse(shouldDrawMiniPlayerGlass(true, true))
        // 关闭的首帧不能等待上帧的 opaque 状态清零。
        org.junit.Assert.assertTrue(shouldDrawMiniPlayerGlass(false, true))
    }

    @Test fun bothDirectionsHaveExactEndpointsAndMonotonicTravel() {
        for (expanding in listOf(true, false)) {
            val easing = PlayerOverlayMotion.easing(expanding)
            assertEquals(0f, easing.transform(0f), .001f)
            assertEquals(1f, easing.transform(1f), .001f)
            val values = (0..100).map { easing.transform(it / 100f) }
            assertTrue(values.zipWithNext().all { (a, b) -> b >= a })
        }
    }
    @Test fun closingBodyRemainsVisibleAfterTheFirstHalfOfGeometryTravel() {
        assertEquals(1f, PlayerOverlayMotion.contentAlpha(.5f, false), .001f)
        assertEquals(.5f, PlayerOverlayMotion.contentAlpha(.25f, false), .001f)
        assertEquals(0f, PlayerOverlayMotion.contentAlpha(0f, false), .001f)
    }
    @Test fun openingFrontLoadsMoreTravelThanClosingAtTheSameTimeFraction() {
        assertTrue(PlayerOverlayMotion.ExpandEasing.transform(.3f) >
            PlayerOverlayMotion.CollapseEasing.transform(.3f))
    }
}
