package com.aria.rythme.ui.component

import org.junit.Assert.*
import org.junit.Test

class ProfileSheetDismissMotionTest {
    @Test fun inheritsReleaseVelocityInsteadOfRestartingAtRest() {
        for (speed in listOf(500f, 2500f, 12000f)) {
            val remaining = 1500f
            val motion = ProfileSheetDismissMotion(speed, remaining, 2500f)
            val step = .0001f
            val measured = remaining * motion.easing.transform(step) / (step * motion.durationMs / 1000f)
            assertEquals(speed, measured, maxOf(100f, speed * .1f))
        }
    }

    @Test fun shorterRemainingTravelFinishesSoonerWithoutReversing() {
        val far = ProfileSheetDismissMotion(2500f, 2000f, 2500f)
        val near = ProfileSheetDismissMotion(2500f, 300f, 2500f)
        assertTrue(near.durationMs < far.durationMs)
        for (motion in listOf(far, near, ProfileSheetDismissMotion(0f, 1400f, 2500f))) {
            var previous = 0f
            for (i in 1..100) {
                val current = motion.easing.transform(i / 100f)
                assertTrue(current >= previous)
                previous = current
            }
            assertEquals(1f, previous, .001f)
        }
    }
}
