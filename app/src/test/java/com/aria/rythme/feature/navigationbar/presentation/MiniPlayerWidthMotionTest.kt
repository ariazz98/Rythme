package com.aria.rythme.feature.navigationbar.presentation

import androidx.compose.animation.core.FloatSpringSpec
import com.aria.rythme.ui.component.utils.MINI_MOTION_STIFFNESS
import com.aria.rythme.ui.component.utils.TAB_MOTION_STIFFNESS
import com.aria.rythme.ui.component.utils.capsuleWidthDamping
import com.aria.rythme.ui.component.utils.miniPlayerPositionAnimationSpec
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniPlayerWidthMotionTest {
    @Test
    fun collapseUndershootsCompactWidthByTwoPercentThenSettles() {
        val values = sample(start = 327f, target = 211f)
        assertEquals(211f * 0.98f, values.min(), 0.1f)
        assertEquals(211f, values.last(), 0.1f)
        assertTrue(values.all { it > 0f })
    }

    @Test
    fun expansionOvershootsFullWidthByTwoPercentThenSettles() {
        val values = sample(start = 211f, target = 327f)
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
    fun collapseMovesFartherInEachEqualTimeInterval() {
        val easing = miniPlayerPositionAnimationSpec(expanding = false).easing
        val positions = (0..10).map { 1f - easing.transform(it / 10f) }
        val distances = positions.zipWithNext { a, b -> abs(b - a) }
        assertTrue(distances.zipWithNext { a, b -> b > a }.all { it })
        assertEquals(1f, positions.first(), 0f)
        assertEquals(0f, positions.last(), 0f)
    }

    @Test
    fun expansionMovesLessInEachEqualTimeInterval() {
        val easing = miniPlayerPositionAnimationSpec(expanding = true).easing
        val positions = (0..10).map { easing.transform(it / 10f) }
        val distances = positions.zipWithNext { a, b -> abs(b - a) }
        assertTrue(distances.zipWithNext { a, b -> b < a }.all { it })
        assertEquals(0f, positions.first(), 0f)
        assertEquals(1f, positions.last(), 0f)
    }

    @Test
    fun bothDirectionsShortenPositionDurationByTwentyPercent() {
        val previous = FloatSpringSpec(1f, MINI_MOTION_STIFFNESS, 0.01f)
            .getDurationNanos(0f, 1f, 0f) / 1_000_000L
        val expected = (previous.toInt() * 0.8f).toInt()
        assertEquals(expected, miniPlayerPositionAnimationSpec(true).durationMillis)
        assertEquals(expected, miniPlayerPositionAnimationSpec(false).durationMillis)
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
}
