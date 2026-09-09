package com.aria.rythme.ui.component

import org.junit.Assert.*
import org.junit.Test

class GlassHdrTest {
    @Test fun darkPressUsesHdrWithoutEnablingStaticGlassAndReturnsToSdr() {
        val state = GlassHdrState()
        state.configurePressEnvironment(true, true)
        assertTrue(state.pressAvailable)
        assertFalse(state.enabled)
        assertEquals(1f, state.requestedHeadroom, 0f)
        state.requestPressHeadroom("button", 3f)
        state.requestPressHeadroom("avatar", 6.5f)
        assertEquals(6.5f, state.requestedHeadroom, 0f)
        state.requestPressHeadroom("avatar", 0f)
        assertEquals(3f, state.requestedHeadroom, 0f)
        state.requestPressHeadroom("button", 0f)
        assertEquals(1f, state.requestedHeadroom, 0f)
    }

    @Test fun leavingDarkThemeRestoresTheExistingLightHeadroomLimit() {
        val state = GlassHdrState()
        state.configurePressEnvironment(true, true)
        state.requestPressHeadroom("avatar", 6.5f)
        state.configurePressEnvironment(true, false)
        assertEquals(2f, state.requestedHeadroom, 0f)
        state.requestPressHeadroom("avatar", 0f)
        assertEquals(1.2f, state.requestedHeadroom, 0f)
        state.configurePressEnvironment(false, true)
        assertFalse(state.pressAvailable)
        assertEquals(1f, state.requestedHeadroom, 0f)
    }

    @Test fun transientPressHeadroomUsesTheMaximumAndRestoresStaticGlass() {
        val state = GlassHdrState()
        state.requestPressHeadroom("first", 1.92f)
        state.requestPressHeadroom("second", 1.5f)
        assertEquals(1.92f, state.requestedHeadroom, 0f)
        state.requestPressHeadroom("first", 1.2f)
        assertEquals(1.5f, state.requestedHeadroom, 0f)
        state.requestPressHeadroom("second", Float.NaN)
        assertEquals(GlassHdrHeadroom, state.requestedHeadroom, 0f)
    }

    @Test fun staticGlassHdrIsLightOnlyAndRequiresControllableHeadroom() {
        assertTrue(glassHdrEligible(35, false, true))
        assertFalse(glassHdrEligible(34, false, true))
        assertFalse(glassHdrEligible(36, true, true))
        assertFalse(glassHdrEligible(36, false, false))
        assertEquals(1.2f, GlassHdrHeadroom, 0f)
    }

    @Test fun invalidDisplayValuesHaveAnSdrFallback() {
        for (ratio in listOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -1f, 0f)) {
            assertEquals(100, glassHdrBucket(ratio))
        }
    }

    @Test fun tinyDisplayUpdatesDoNotRebuildTheCache() {
        val state = GlassHdrState()
        state.configure(true, 1.10f)
        val generation = state.generation
        state.configure(true, 1.101f)
        state.configure(true, 1.102f)
        assertEquals(generation, state.generation)
        state.configure(true, 1.12f)
        assertEquals(generation + 1, state.generation)
    }

    @Test fun disablingHdrInvalidatesOnceAndIgnoresFurtherDisplayChanges() {
        val state = GlassHdrState()
        state.configure(true, 1.2f)
        val generation = state.generation
        state.configure(false, 1f)
        assertFalse(state.enabled)
        assertEquals(generation + 1, state.generation)
        state.configure(false, 1.15f)
        assertEquals(generation + 1, state.generation)
    }

    @Test fun displayNoiseAtBucketBoundaryDoesNotThrashCaches() {
        val state = GlassHdrState()
        state.configure(true, 1.10f)
        val generation = state.generation
        repeat(10) {
            state.configure(true, 1.1049f)
            state.configure(true, 1.1051f)
        }
        assertEquals(generation, state.generation)
        state.configure(true, 1.107f)
        assertEquals(generation + 1, state.generation)
        repeat(10) {
            state.configure(true, 1.1049f)
            state.configure(true, 1.1051f)
        }
        assertEquals(generation + 1, state.generation)
    }
}
