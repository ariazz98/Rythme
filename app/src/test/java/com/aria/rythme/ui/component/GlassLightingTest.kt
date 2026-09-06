package com.aria.rythme.ui.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class GlassLightingTest {
    @Test
    fun darkGlassDoesNotReuseLightThemeHighlightStrength() {
        val light = glassLightingProfile(false)
        val dark = glassLightingProfile(true)
        assertTrue(dark.coreAlpha < light.coreAlpha / 2f)
        assertTrue(dark.glowAlpha < light.glowAlpha / 2f)
        assertTrue(dark.coreAlpha > 0f && dark.glowAlpha > 0f)
    }

    @Test
    fun strongHighlightStaysAtTopAndBottomInsteadOfWrappingCorners() {
        for (dark in listOf(false, true)) {
            val power = glassLightingProfile(dark).directionPower
            assertEquals(1f, 1f.pow(power), 0f)
            assertEquals(0f, 0f.pow(power), 0f)
            assertTrue(sqrt(0.5f).pow(power) < 0.07f)
        }
    }

    @Test
    fun bodyReflectionStaysSubtleAndFadesAtBothEnds() {
        for (step in 0..1000) {
            val alpha = glassBodyReflectionAlpha(step / 1000f)
            assertTrue(alpha.isFinite() && alpha in 0f..0.020f)
        }
        assertEquals(0.020f, glassBodyReflectionAlpha(0.27f), 0.00001f)
        assertTrue(glassBodyReflectionAlpha(0f) < 0.00001f)
        assertTrue(glassBodyReflectionAlpha(1f) < 0.00001f)
    }

    @Test
    fun bodyReflectionHasNoSuddenLuminanceStepAtFormerStops() {
        val sampleStep = 0.001f
        for (step in 1 until 1000) {
            val depth = step * sampleStep
            val before = glassBodyReflectionAlpha(depth - sampleStep)
            val center = glassBodyReflectionAlpha(depth)
            val after = glassBodyReflectionAlpha(depth + sampleStep)
            // 斜率不应在旧的 12%、20%、30%、50% 等位置突然改变。
            assertTrue(abs(after - 2f * center + before) < 0.00002f)
        }
    }
}
