package com.aria.rythme.feature.navigationbar.presentation

import androidx.compose.animation.core.FloatSpringSpec
import com.aria.rythme.ui.component.utils.MINI_MOTION_STIFFNESS
import com.aria.rythme.ui.component.utils.capsuleWidthDamping
import com.aria.rythme.ui.component.utils.miniPlayerExpansionAnimationSpec
import com.aria.rythme.ui.component.utils.miniPlayerPositionAnimationSpec
import com.aria.rythme.ui.component.utils.miniPlayerWidthAnimationSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniPlayerPhaseMotionTest {
    private val compact = 204f
    private val expanded = 327f
    private val delayNanos = 50_000_000L

    @Test
    fun expansionLiftsBeforeWidthAndNextButtonStartChanging() {
        val width = miniPlayerWidthAnimationSpec(compact, expanded, expanded)
        for (millis in 0..50) {
            val time = millis * 1_000_000L
            assertEquals(compact, width.getValueFromNanos(time, compact, expanded, 0f), 0.0001f)
            assertEquals(0f, width.getVelocityFromNanos(time, compact, expanded, 0f), 0.0001f)
            assertEquals(0f, miniPlayerExpansionAnimationSpec.getValueFromNanos(time, 0f, 1f, 0f), 0.0001f)
        }
        val position = miniPlayerPositionAnimationSpec(true)
        assertTrue(position.getValueFromNanos(50_000_000L, 0f, 1f, 0f) in 0.28f..0.33f)
        assertTrue(width.getValueFromNanos(51_000_000L, compact, expanded, 0f) > compact)
        assertTrue(miniPlayerExpansionAnimationSpec.getValueFromNanos(51_000_000L, 0f, 1f, 0f) > 0f)
    }

    @Test
    fun delayedExpansionUsesTheOriginalSpringAfterItsHold() {
        val width = miniPlayerWidthAnimationSpec(compact, expanded, expanded)
        val base = widthSpring(expanded)
        for (millis in 0..1000) {
            val time = millis * 1_000_000L
            assertEquals(base.getValueFromNanos(time, compact, expanded, 0f),
                width.getValueFromNanos(time + delayNanos, compact, expanded, 0f), 0.0001f)
        }
        assertEquals(delayNanos + base.getDurationNanos(compact, expanded, 0f),
            width.getDurationNanos(compact, expanded, 0f))
        assertEquals(0f, width.getEndVelocity(compact, expanded, 0f), 0f)
        val baseContent = FloatSpringSpec(1f, MINI_MOTION_STIFFNESS, 0.01f)
        assertEquals(delayNanos + baseContent.getDurationNanos(0f, 1f, 0f),
            miniPlayerExpansionAnimationSpec.getDurationNanos(0f, 1f, 0f))
        assertEquals(0f, miniPlayerExpansionAnimationSpec.getEndVelocity(0f, 1f, 0f), 0f)
    }

    @Test
    fun collapseWidthAndContentAreUnchangedAtEverySample() {
        val width = miniPlayerWidthAnimationSpec(compact, expanded, compact)
        val base = widthSpring(compact)
        val baseContent = FloatSpringSpec(1f, MINI_MOTION_STIFFNESS, 0.01f)
        for (millis in 0..1000) {
            val time = millis * 1_000_000L
            assertEquals(base.getValueFromNanos(time, expanded, compact, 0f),
                width.getValueFromNanos(time, expanded, compact, 0f), 0f)
            assertEquals(baseContent.getValueFromNanos(time, 1f, 0f, 0f),
                miniPlayerExpansionAnimationSpec.getValueFromNanos(time, 1f, 0f, 0f), 0f)
        }
    }

    @Test
    fun reversingWidthCarriesItsCurrentPositionAndVelocityWithoutAnotherHold() {
        for (expanding in listOf(true, false)) {
            val start = if (expanding) compact else expanded
            val target = if (expanding) expanded else compact
            val old = miniPlayerWidthAnimationSpec(compact, expanded, target)
            for (millis in listOf(80, 140, 220)) {
                val time = millis * 1_000_000L
                val value = old.getValueFromNanos(time, start, target, 0f)
                val velocity = old.getVelocityFromNanos(time, start, target, 0f)
                val reversed = miniPlayerWidthAnimationSpec(compact, expanded, start)
                val base = widthSpring(start)
                assertEquals(value, reversed.getValueFromNanos(0L, value, start, velocity), 0.0001f)
                assertEquals(velocity, reversed.getVelocityFromNanos(0L, value, start, velocity), 0.0001f)
                assertEquals(base.getDurationNanos(value, start, velocity),
                    reversed.getDurationNanos(value, start, velocity))
                assertEquals(base.getValueFromNanos(10_000_000L, value, start, velocity),
                    reversed.getValueFromNanos(10_000_000L, value, start, velocity), 0.0001f)
            }
        }
    }

    @Test
    fun movingOrPartiallyExpandedContentDoesNotWaitAgain() {
        val base = FloatSpringSpec(1f, MINI_MOTION_STIFFNESS, 0.01f)
        for ((value, velocity) in listOf(0f to 0.5f, 0.2f to 0f, 0.5f to -3f)) {
            assertEquals(base.getDurationNanos(value, 1f, velocity),
                miniPlayerExpansionAnimationSpec.getDurationNanos(value, 1f, velocity))
            assertEquals(velocity, miniPlayerExpansionAnimationSpec.getVelocityFromNanos(0L, value, 1f, velocity), 0.0001f)
        }
    }

    @Test
    fun cancelingBeforeWidthStartsLeavesTheCompactEndpointStable() {
        val opening = miniPlayerWidthAnimationSpec(compact, expanded, expanded)
        val value = opening.getValueFromNanos(25_000_000L, compact, expanded, 0f)
        val velocity = opening.getVelocityFromNanos(25_000_000L, compact, expanded, 0f)
        val closing = miniPlayerWidthAnimationSpec(compact, expanded, compact)
        for (millis in 0..500) {
            assertEquals(compact, closing.getValueFromNanos(millis * 1_000_000L, value, compact, velocity), 0f)
        }
    }

    @Test
    fun changingAvailableWidthWhileExpandedDoesNotInsertTheExpansionHold() {
        val spec = miniPlayerWidthAnimationSpec(244f, 367f, 367f)
        val base = FloatSpringSpec(capsuleWidthDamping(367f, 123f), MINI_MOTION_STIFFNESS, 0.1f)
        assertEquals(base.getDurationNanos(327f, 367f, 0f), spec.getDurationNanos(327f, 367f, 0f))
    }

    private fun widthSpring(target: Float) = FloatSpringSpec(
        capsuleWidthDamping(target, expanded - compact), MINI_MOTION_STIFFNESS, 0.1f
    )
}
