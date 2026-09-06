package com.aria.rythme.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

class GlassRefractionTest {
    @Test
    fun headerMidlineIsOutsideTheRefractionBandAtDifferentDensities() {
        for (density in listOf(1f, 2f, 2.625f, 3.25f, 4f)) {
            val halfHeight = 22f * density
            val height = glassRefractionHeight(24f * density, 44f * density)
            assertEquals(halfHeight, height, 0f)
            // Backdrop 在计算中线的零向量法线前，先检查 -sd >= refractionHeight。
            assertTrue(halfHeight >= height)
        }
    }

    @Test
    fun existingBottomBarAndMenuRefractionStaysUnchanged() {
        for (minDimension in listOf(50f, 56f, 64f, 68f, 256f)) {
            assertEquals(24f, glassRefractionHeight(24f, minDimension), 0f)
        }
        for (step in 0..100) {
            val progress = step / 100f
            assertEquals(24f * progress, glassRefractionHeight(24f * progress, 56f), 0f)
            assertEquals(10f * progress, glassRefractionHeight(10f * progress, 56f), 0f)
        }
    }

    @Test
    fun morphingSurfacesNeverLetRefractionCrossTheirMidline() {
        for (step in 1..2048) {
            val minDimension = step / 8f
            val height = glassRefractionHeight(24f, minDimension)
            assertTrue(height.isFinite() && height > 0f)
            assertTrue(height <= minDimension / 2f)
            assertTrue(height <= 24f)
        }
    }

    @Test
    fun unmeasuredOrEmptySurfacesSkipRefraction() {
        for (value in listOf(0f, -1f, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY)) {
            assertEquals(0f, glassRefractionHeight(24f, value), 0f)
            assertEquals(0f, glassRefractionHeight(value, 44f), 0f)
        }
    }

    @Test
    fun refractionOffsetApproachesZeroContinuouslyAtHeaderMidline() {
        val halfHeight = 22.0
        val height = glassRefractionHeight(24f, 44f).toDouble()
        // 对应 Backdrop 2.0.1 在胶囊中部直线段的距离判断与 circleMap。
        fun offset(y: Double): Double {
            val edgeDistance = halfHeight - abs(y)
            if (edgeDistance >= height) return 0.0
            val x = 1.0 - edgeDistance / height
            return -32.0 * (1.0 - sqrt(1.0 - x * x)) * sign(y)
        }
        assertEquals(0.0, offset(0.0), 0.0)
        for (y in listOf(0.001, 0.01, 0.1)) {
            assertTrue(offset(y).isFinite())
            assertTrue(abs(offset(y)) < 0.001)
            assertEquals(-offset(y), offset(-y), 0.0000001)
        }
    }
}
