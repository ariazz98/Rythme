package com.aria.rythme.feature.navigationbar.presentation

import androidx.compose.animation.core.FloatSpringSpec
import com.aria.rythme.ui.component.utils.MINI_MOTION_STIFFNESS
import com.aria.rythme.ui.component.utils.TAB_MOTION_STIFFNESS
import com.aria.rythme.ui.component.utils.capsuleWidthDamping
import com.aria.rythme.ui.component.utils.miniPlayerPositionAnimationSpec
import com.aria.rythme.ui.component.utils.miniPlayerWidthAnimationSpec
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniPlayerWidthMotionTest {
    @Test
    fun collapseUndershootsCompactWidthByTwoPercentThenSettles() {
        val values = sampleMiniWidth(start = 327f, target = 211f)
        assertEquals(211f * 0.98f, values.min(), 0.1f)
        assertEquals(211f, values.last(), 0.1f)
        assertTrue(values.all { it > 0f })
    }

    @Test
    fun expansionOvershootsFullWidthByTwoPercentThenSettles() {
        val values = sampleMiniWidth(start = 211f, target = 327f)
        assertEquals(327f * 1.02f, values.max(), 0.1f)
        assertEquals(327f, values.last(), 0.1f)
    }

    @Test
    fun zeroTravelDoesNotProduceInvalidDamping() {
        assertEquals(1f, capsuleWidthDamping(120f, 0f), 0f)
    }

    @Test
    fun bothTabCapsulesReboundInBothDirections() {
        for (expandedWidth in listOf(327f, 84f)) {
            val collapse = sample(expandedWidth, 50f, TAB_MOTION_STIFFNESS)
            val expand = sample(50f, expandedWidth, TAB_MOTION_STIFFNESS)
            assertEquals(50f * 0.98f, collapse.min(), 0.1f)
            assertEquals(expandedWidth * 1.02f, expand.max(), 0.1f)
            assertEquals(50f, collapse.last(), 0.1f)
            assertEquals(expandedWidth, expand.last(), 0.1f)
        }
    }

    @Test
    fun miniDurationIncreasesTwentyPercent() {
        val damping = capsuleWidthDamping(327f, 116f)
        val before = FloatSpringSpec(damping, 700f, 0.01f).getDurationNanos(211f, 327f, 0f)
        val after = FloatSpringSpec(damping, MINI_MOTION_STIFFNESS, 0.01f).getDurationNanos(211f, 327f, 0f)
        assertEquals(1.2f, after.toFloat() / before, 0.02f)
    }

    @Test
    fun collapseAcceleratesLateAndBrakesBeforeTheEndpoint() {
        val spec = miniPlayerPositionAnimationSpec(expanding = false)
        val duration = spec.getDurationNanos(1f, 0f, 0f)
        val positions = (0..10).map { spec.getValueFromNanos(duration * it / 10L, 1f, 0f, 0f) }
        val distances = positions.zipWithNext { a, b -> abs(b - a) }
        assertTrue(distances.take(6).zipWithNext { a, b -> b > a }.all { it })
        assertTrue(distances.last() < distances.max())
        assertTrue(1f - spec.getValueFromNanos(duration / 4L, 1f, 0f, 0f) < 0.15f)
        assertEquals(1f, positions.first(), 0f)
        assertEquals(0f, positions.last(), 0f)
    }

    @Test
    fun collapseRemainsMonotonicAndSettlesAtTheCompactEndpoint() {
        val spec = miniPlayerPositionAnimationSpec(false)
        val duration = spec.getDurationNanos(1f, 0f, 0f)
        val values = (0..1000).map { spec.getValueFromNanos(duration * it / 1000L, 1f, 0f, 0f) }
        assertTrue(values.all { it.isFinite() && it in 0f..1f })
        assertTrue(values.zipWithNext { a, b -> b <= a }.all { it })
        assertEquals(0f, values.last(), 0f)
    }

    @Test
    fun expansionAcceleratesFromRestThenReturnsFromASmallOvershoot() {
        val spec = miniPlayerPositionAnimationSpec(true)
        val positions = (0..600).map { spec.getValueFromNanos(it * 1_000_000L, 0f, 1f, 0f) }
        assertEquals(0f, spec.getVelocityFromNanos(0L, 0f, 1f, 0f), 0.0001f)
        assertTrue(positions[50] - positions[25] > positions[25] - positions[0])
        assertTrue(positions.max() in 1.035f..1.04f)
        assertTrue(positions[350] < positions[220])
        assertEquals(1f, positions.last(), 0.001f)
        assertEquals(0f, spec.getEndVelocity(0f, 1f, 0f), 0f)
    }

    @Test
    fun collapseKeepsItsPreviousDurationAndExpansionIncludesTheReturnTail() {
        val previous = FloatSpringSpec(1f, MINI_MOTION_STIFFNESS, 0.01f)
            .getDurationNanos(0f, 1f, 0f) / 1_000_000L
        val expected = (previous.toInt() * 0.8f).toInt()
        assertEquals(expected.toLong(), miniPlayerPositionAnimationSpec(false).getDurationNanos(1f, 0f, 0f) / 1_000_000L)
        assertTrue(miniPlayerPositionAnimationSpec(true).getDurationNanos(0f, 1f, 0f) > 350_000_000L)
    }

    @Test
    fun expansionPositionFitsTheRecordedCoverTrackWithinPixelTolerance() {
        // #381 起，每帧约 16.67ms；末态 y=2171，初态 y=2360。容许压缩和模板匹配误差。
        val y = listOf(2360, 2353, 2332, 2304, 2275, 2247, 2223, 2204, 2189, 2178,
            2171, 2167, 2164, 2164, 2164, 2165, 2166, 2167, 2168, 2169, 2169, 2170, 2171)
        val spec = miniPlayerPositionAnimationSpec(true)
        y.forEachIndexed { frame, observed ->
            val progress = spec.getValueFromNanos(frame * 1_000_000_000L / 60L, 0f, 1f, 0f)
            assertEquals(observed.toFloat(), 2360f - 189f * progress, 4f)
        }
    }

    @Test
    fun reexpansionCarriesTheCurrentPositionAndVelocityIntoItsSpring() {
        val collapse = miniPlayerPositionAnimationSpec(false)
        val expansion = miniPlayerPositionAnimationSpec(true)
        for (millis in listOf(40, 100, 180, 220)) {
            val time = millis * 1_000_000L
            val position = collapse.getValueFromNanos(time, 1f, 0f, 0f)
            val velocity = collapse.getVelocityFromNanos(time, 1f, 0f, 0f)
            assertEquals(position, expansion.getValueFromNanos(0L, position, 1f, velocity), 0.0001f)
            assertEquals(velocity, expansion.getVelocityFromNanos(0L, position, 1f, velocity), 0.0001f)
        }
    }

    private fun sample(start: Float, target: Float, stiffness: Float = MINI_MOTION_STIFFNESS): List<Float> {
        val spring = FloatSpringSpec(
            dampingRatio = capsuleWidthDamping(target, abs(target - start)),
            stiffness = stiffness,
            visibilityThreshold = 0.01f
        )
        return (0..1000).map { millis ->
            spring.getValueFromNanos(millis * 1_000_000L, start, target, 0f)
        }
    }

    private fun sampleMiniWidth(start: Float, target: Float): List<Float> {
        val spec = miniPlayerWidthAnimationSpec(minOf(start, target), maxOf(start, target), target)
        return (0..1000).map { spec.getValueFromNanos(it * 1_000_000L, start, target, 0f) }
    }
}
