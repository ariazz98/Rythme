package com.aria.rythme.ui.component

import androidx.compose.ui.geometry.Rect
import com.aria.rythme.ui.component.utils.BottomBarGeometry
import com.aria.rythme.ui.component.utils.BottomBarMetrics
import com.aria.rythme.ui.component.utils.BottomTabMorphGeometry
import com.aria.rythme.ui.component.utils.bottomBarBottomSpacing
import com.aria.rythme.ui.component.utils.expandedBottomBarContentInset
import com.aria.rythme.ui.component.utils.searchSurfaceMergeProgress
import kotlin.math.sqrt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomBarGeometryTest {
    @Test
    fun contentInsetMatchesExpandedBarAndSystemSafeArea() {
        val expanded = BottomBarGeometry(BottomBarMetrics.ExpandedTabHeight, BottomBarMetrics.ExpandedMiniHeight, 1f)
        for (navigationInset in listOf(0f, 12f, 24f, 48f)) {
            assertEquals(expanded.height + bottomBarBottomSpacing(navigationInset) + navigationInset,
                expandedBottomBarContentInset(navigationInset), 0f)
        }
        assertEquals(133f, expandedBottomBarContentInset(0f), 0f)
        assertEquals(168f, expandedBottomBarContentInset(48f), 0f)
    }

    @Test
    fun contentInsetDoesNotFollowAnimatedHeight() {
        val inset = expandedBottomBarContentInset(24f)
        for (step in 0..100) {
            val fraction = step / 100f
            val tab = BottomBarMetrics.CompactHeight + (BottomBarMetrics.ExpandedTabHeight - BottomBarMetrics.CompactHeight) * fraction
            val mini = BottomBarMetrics.CompactHeight + (BottomBarMetrics.ExpandedMiniHeight - BottomBarMetrics.CompactHeight) * fraction
            val animated = BottomBarGeometry(tab, mini, fraction)
            assertTrue(inset >= animated.height + bottomBarBottomSpacing(24f) + 24f)
        }
    }

    @Test
    fun expandedEndpointsMatchTheMeasuredReference() {
        val g = BottomBarGeometry(BottomBarMetrics.ExpandedTabHeight, BottomBarMetrics.ExpandedMiniHeight, 1f)
        assertEquals(112f, g.height, 0f)
        assertEquals(0f, g.miniTop, 0f)
        assertEquals(7f, g.tabTop - (g.miniTop + g.miniHeight), 0f)
        assertEquals(59.5f, g.centerTravel, 0f)
    }

    @Test
    fun compactSurfacesStayOnTheSameCenterLine() {
        val g = BottomBarGeometry(BottomBarMetrics.CompactHeight, BottomBarMetrics.CompactHeight, 0f)
        assertEquals(g.tabTop, g.miniTop, 0f)
        assertEquals(45.125f, g.miniHeight * BottomBarMetrics.CompactScale, 0.001f)
        assertEquals(13.3f, BottomBarMetrics.CompactGap * BottomBarMetrics.CompactScale, 0.001f)
    }

    @Test
    fun tabCenterRemainsFixedWithIndependentOrReversingAnimationChannels() {
        val bottom = 820f
        for (tab in listOf(0f, .2f, .5f, .8f, 1f)) {
            for (mini in listOf(0f, .2f, .5f, .8f, 1f)) {
                for (position in listOf(0f, .2f, .5f, .8f, 1f, 1.02f, 1.04f)) {
                    val compact = BottomBarMetrics.CompactHeight
                    val g = BottomBarGeometry(
                        compact + (BottomBarMetrics.ExpandedTabHeight - compact) * tab,
                        compact + (BottomBarMetrics.ExpandedMiniHeight - compact) * mini,
                        position
                    )
                    val worldCenter = bottom - g.height + g.tabCenterY
                    assertEquals(bottom - 29.5f, worldCenter, 0.0001f)
                    assertEquals(g.tabCenterY, g.scalePivotY * g.height, 0.0001f)
                    assertTrue(g.tabTop >= -0.0001f && g.miniTop >= -0.0001f)
                    assertTrue(g.tabTop + g.tabHeight <= g.height + 0.0001f)
                    assertTrue(g.miniTop + g.miniHeight <= g.height + 0.0001f)
                }
            }
        }
    }

    @Test
    fun positionOvershootMovesOnlyTheMiniPlayerWithoutClippingOrMovingTabCenter() {
        val settled = BottomBarGeometry(59f, 46f, 1f)
        val peak = BottomBarGeometry(59f, 46f, 1.038f)
        assertEquals(59.5f * .038f, peak.height - settled.height, 0.0001f)
        assertEquals(settled.height - settled.tabCenterY, peak.height - peak.tabCenterY, 0f)
        assertEquals(0f, peak.miniTop, 0.0001f)
        assertEquals(settled.miniHeight, peak.miniHeight, 0f)
        assertEquals(settled.tabHeight, peak.tabHeight, 0f)
    }

    @Test
    fun bottomSpacingRespectsVisualAndSystemSafeAreas() {
        for (navigationInset in listOf(0f, 12f, 24f, 48f)) {
            val extra = bottomBarBottomSpacing(navigationInset)
            assertTrue(extra >= 8f)
            assertTrue(navigationInset + extra >= 21f)
        }
        assertEquals(21f, bottomBarBottomSpacing(0f), 0f)
        assertEquals(8f, bottomBarBottomSpacing(48f), 0f)
    }

    @Test
    fun retainedIconsMoveOnlyFromTheirLabelOffsetToTheCircleCenter() {
        val expanded = BottomTabMorphGeometry(327f, 59f, 47.5f, 4f, 1f, true, 14f)
        val compact = BottomTabMorphGeometry(327f, 47.5f, 47.5f, 4f, 0f, true, 14f)
        assertEquals(7f, expanded.height / 2f - expanded.iconCenter(0).y, 0f)
        assertEquals(compact.height / 2f, compact.iconCenter(0).y, 0f)
    }

    @Test
    fun calibratedHeightsMatchTheReferenceAtTheAuditedScreenWidth() {
        // 用户录屏宽 1180px，当前真机宽 1200px、3.25px/dp；比较屏宽占比，而非 pt=dp。
        val measuredHeights = listOf(
            BottomBarMetrics.ExpandedTabHeight to 188f,
            BottomBarMetrics.ExpandedMiniHeight to 146f,
            BottomBarMetrics.CompactHeight * BottomBarMetrics.CompactScale to 144f
        )
        for ((heightDp, referencePx) in measuredHeights) {
            val ratio = (heightDp * 3.25f / 1200f) / (referencePx / 1180f)
            assertTrue(ratio in 0.99f..1.01f)
        }
    }

    @Test
    fun selectorKeepsItsVerticalClearanceInsideTheThinnerTabCapsule() {
        assertEquals(52f, BottomBarMetrics.ExpandedSelectorHeight, 0f)
        assertEquals(3.5f, (BottomBarMetrics.ExpandedTabHeight - BottomBarMetrics.ExpandedSelectorHeight) / 2f, 0f)
    }

    @Test
    fun refractionRangeAndStrengthAreBothReducedByAboutFifteenPercent() {
        for (density in listOf(1f, 2f, 3.25f, 4f)) {
            for (heightDp in listOf(47.5f, 52f, 56f, 64f)) {
                val height = heightDp * density
                val previousRange = height * 10f / 56f
                val previousAmount = height * 10f / 52f
                val range = height * BottomBarMetrics.SelectorRefractionHeightRatio
                val amount = height * BottomBarMetrics.SelectorRefractionAmountRatio
                assertTrue(range / previousRange in 0.849f..0.851f)
                assertEquals(.85f, amount / previousAmount, .00001f)
            }
        }
    }

    @Test
    fun currentSelectorUsesTheApprovedSmallerRangeAndStrength() {
        for (density in listOf(1f, 2f, 3.25f, 4f)) {
            val height = BottomBarMetrics.ExpandedSelectorHeight * density
            assertEquals(7.9f * density, height * BottomBarMetrics.SelectorRefractionHeightRatio, .0001f)
            assertEquals(8.5f * density, height * BottomBarMetrics.SelectorRefractionAmountRatio, .0001f)
        }
    }

    @Test
    fun weakerRefractionMovesTheSamplingTurningPointOutwardAndPreservesRelativeScaling() {
        // 完全按住、无速度拉伸的上下直线段；不作为整个曲面或动态过程的替代验收。
        val pressScale = 74f / 56f
        val previousBand = 52f * 10f / 56f
        val previousRelativeDepth = (previousBand + 10f - sqrt(previousBand * previousBand + 100f)) / 52f
        val currentRelativeDepth = (7.9f + 8.5f - sqrt(7.9f * 7.9f + 8.5f * 8.5f)) / 52f
        for (density in listOf(1f, 2f, 3.25f, 4f)) {
            for (heightDp in listOf(47.5f, 52f, 56f, 64f)) {
                val height = heightDp * density
                val band = height * BottomBarMetrics.SelectorRefractionHeightRatio
                val amount = height * BottomBarMetrics.SelectorRefractionAmountRatio
                val nearestSampleDepth = band + amount - sqrt(band * band + amount * amount)
                val hiddenEdgeDepth = height / 2f * (1f - 1f / pressScale)
                assertEquals(currentRelativeDepth, nearestSampleDepth / height, .00001f)
                assertTrue(nearestSampleDepth / height < previousRelativeDepth)
                assertTrue(nearestSampleDepth < hiddenEdgeDepth)
            }
        }
    }

    @Test
    fun opticalProgressStillStartsAtZeroAndStaysInsideTheSafeHalfHeight() {
        val height = BottomBarMetrics.ExpandedSelectorHeight * 3.25f
        for (step in 0..100) {
            val progress = step / 100f
            val band = height * BottomBarMetrics.SelectorRefractionHeightRatio * progress
            val amount = height * BottomBarMetrics.SelectorRefractionAmountRatio * progress
            assertTrue(band in 0f..height / 2f)
            assertTrue(amount.isFinite() && amount >= 0f)
            if (step == 0) {
                assertEquals(0f, band, 0f)
                assertEquals(0f, amount, 0f)
            }
        }
    }

    @Test
    fun calibratedCompactBoundsAreCirclesWithCenteredIconsInBothDirections() {
        val diameter = BottomBarMetrics.CompactHeight
        for (isLtr in listOf(false, true)) {
            val g = BottomTabMorphGeometry(327f, diameter, diameter, 4f, 0f, isLtr, 14f)
            val primary = g.primaryBounds(diameter)
            val search = g.searchBounds(diameter)
            assertEquals(primary.height, primary.width, 0f)
            assertEquals(search.height, search.width, 0f)
            for (index in 0..2) assertEquals(primary.center, g.iconCenter(index))
            assertEquals(search.center, g.iconCenter(3))
        }
    }

    @Test
    fun searchSurfaceDisappearsWithGeometryInsteadOfWaitingForBaseProgress() {
        val search = Rect(250f, 0f, 330f, 60f)
        assertEquals(0f, searchSurfaceMergeProgress(Rect(0f, 0f, 290f, 60f), search), 0f)
        assertEquals(.5f, searchSurfaceMergeProgress(Rect(0f, 0f, 310f, 60f), search), 0.001f)
        for (right in listOf(330f, 334f, 336.6f)) {
            assertEquals(1f, searchSurfaceMergeProgress(Rect(0f, 0f, right, 60f), search), 0f)
        }
    }

    @Test
    fun searchMergeIsMirroredInRtlAndContinuousAtItsBoundaries() {
        fun mirror(r: Rect) = Rect(330f - r.right, r.top, 330f - r.left, r.bottom)
        val search = Rect(250f, 0f, 330f, 60f)
        var previous = 0f
        for (step in 0..800) {
            val primary = Rect(0f, 0f, 250f + step / 10f, 60f)
            val value = searchSurfaceMergeProgress(primary, search)
            assertTrue(value >= previous && value - previous < .004f)
            assertEquals(value, searchSurfaceMergeProgress(mirror(primary), mirror(search)), .0001f)
            previous = value
        }
        assertEquals(1f, previous, 0f)
    }
}
