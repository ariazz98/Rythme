package com.aria.rythme.ui.component

import com.aria.rythme.ui.component.utils.BottomTabMorphGeometry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomTabMorphGeometryTest {
    @Test
    fun primaryShrinksContinuouslyFromFullWidthWithoutJumpingToThreeTabs() {
        for (expansion in listOf(0f, 0.25f, 0.5f, 0.9f, 0.999f)) {
            val g = geometry(expansion)
            assertEquals(50f + (396f - 50f) * expansion, g.primary.width, 0.001f)
            assertTrue(g.primary.width >= g.diameter)
            assertTrue(g.search.width >= g.diameter)
        }
        val firstFrame = geometry(0.999f)
        assertTrue(firstFrame.primary.right > firstFrame.search.left)
        assertTrue(396f - firstFrame.primary.width < 0.5f)
    }

    @Test
    fun searchDetachesAtItsOldSlotButPrimaryKeepsFullWidth() {
        val g = geometry(1f)
        assertEquals(396f, g.primary.right, 0.001f)
        assertEquals(295f, g.search.left, 0.001f)
        assertEquals(0f, g.primary.left, 0.001f)
        assertEquals(396f, g.search.right, 0.001f)
        assertEquals(52.5f, g.iconCenter(0).x, 0.001f)
    }

    @Test
    fun collapsedIconsEndInsideTheirOwnCircles() {
        val g = geometry(0f)
        assertEquals(50f, g.primary.width, 0.001f)
        assertEquals(50f, g.search.width, 0.001f)
        for (index in 0..2) assertEquals(g.primary.center, g.iconCenter(index))
        assertEquals(g.search.center, g.iconCenter(3))
    }

    @Test
    fun rtlMirrorsShapesAndIconPaths() {
        for (expansion in listOf(0f, 0.25f, 0.5f, 0.75f, 1f)) {
            val ltr = geometry(expansion)
            val rtl = geometry(expansion, false)
            assertEquals(396f - ltr.primary.right, rtl.primary.left, 0.001f)
            for (index in 0..3) {
                assertEquals(396f - ltr.iconCenter(index).x, rtl.iconCenter(index).x, 0.001f)
            }
        }
    }

    @Test
    fun animatedWidthKeepsOuterEdgesAnchoredRegardlessOfLayoutPhase() {
        for (expansion in listOf(0f, 0.25f, 0.75f, 1f)) {
            val g = geometry(expansion)
            for (width in listOf(50f, 150f, 300f, 396f)) {
                val bounds = g.primaryBounds(width)
                assertEquals(0f, bounds.left, 0.001f)
                assertEquals(width, bounds.width, 0.001f)
            }
            for (width in listOf(50f, 75f, 101f)) {
                val bounds = g.searchBounds(width)
                assertEquals(396f, bounds.right, 0.001f)
                assertEquals(width, bounds.width, 0.001f)
            }
        }
    }

    @Test
    fun onlyEndpointOvershootMovesOuterEdgesAndMatchesFullCapsuleScale() {
        val g = geometry(0.8f)
        val primary = g.primaryBounds(396f * 1.02f)
        assertEquals(-396f * 0.01f, primary.left, 0.001f)
        assertEquals(396f * 1.01f, primary.right, 0.001f)
        assertEquals(198f, primary.center.x, 0.001f)
        val search = g.searchBounds(101f * 1.02f)
        assertEquals(396f + 101f * 0.01f, search.right, 0.001f)
        assertEquals(345.5f, search.center.x, 0.001f)
    }

    @Test
    fun compactUndershootKeepsBothCircleCentersFixed() {
        val g = geometry(0.1f)
        assertEquals(25f, g.primaryBounds(49f).center.x, 0.001f)
        assertEquals(371f, g.searchBounds(49f).center.x, 0.001f)
        assertEquals(49f, g.primaryBounds(49f).width, 0.001f)
        assertEquals(49f, g.searchBounds(49f).width, 0.001f)
    }

    @Test
    fun animatedBoundsMirrorIncludingRebound() {
        val ltr = geometry(0.7f)
        val rtl = geometry(0.7f, false)
        for (width in listOf(49f, 50f, 200f, 396f * 1.02f)) {
            assertEquals(396f - ltr.primaryBounds(width).right, rtl.primaryBounds(width).left, 0.001f)
        }
        for (width in listOf(49f, 50f, 75f, 101f * 1.02f)) {
            assertEquals(396f - ltr.searchBounds(width).left, rtl.searchBounds(width).right, 0.001f)
        }
    }

    private fun geometry(expansion: Float, isLtr: Boolean = true) = BottomTabMorphGeometry(
        width = 396f, height = 50f + 14f * expansion, diameter = 50f,
        padding = 4f, expansion = expansion, isLtr = isLtr
    )
}
