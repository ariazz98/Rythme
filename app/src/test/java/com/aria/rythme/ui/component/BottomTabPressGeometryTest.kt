package com.aria.rythme.ui.component

import com.aria.rythme.ui.component.utils.bottomTabEmphasisScale
import com.aria.rythme.ui.component.utils.bottomTabPressScale
import com.aria.rythme.ui.component.utils.BottomTabMorphGeometry
import com.aria.rythme.ui.component.utils.BottomBarMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BottomTabPressGeometryTest {
    @Test
    fun sharedPressScaleKeepsTheOriginalWidthGrowthAcrossDensities() {
        for (density in listOf(1f, 2f, 3.25f, 4f)) {
            for (widthDp in listOf(240f, 327.23f, 480f)) {
                for (step in 0..100) {
                    val progress = step / 100f
                    val width = widthDp * density
                    val scale = bottomTabPressScale(width, 16f * density, progress)
                    // 像素尺寸乘回 Float 缩放时有约 0.0002px 的舍入误差。
                    assertEquals(16f * density * progress, width * (scale - 1f), .0005f)
                }
            }
        }
    }

    @Test
    fun emphasisUsesTheReducedFinalSizeInsteadOfMultiplyingTwoMagnifications() {
        for (width in listOf(240f, 327.23f, 480f)) {
            for (step in 0..100) {
                val progress = step / 100f
                val parentScale = bottomTabPressScale(width, 16f, progress)
                val localScale = bottomTabEmphasisScale(progress, parentScale)
                assertEquals(1f + .15f * progress, parentScale * localScale, .00001f)
                assertTrue(localScale >= 1f)
                if (step > 0) assertTrue(parentScale * localScale > parentScale)
            }
        }
    }

    @Test
    fun restingAndUnmeasuredSurfacesDoNotIntroduceAnOffsetOrExtraScale() {
        assertEquals(1f, bottomTabPressScale(327f, 16f, 0f), 0f)
        assertEquals(1f, bottomTabPressScale(0f, 16f, 1f), 0f)
        assertEquals(1f, bottomTabEmphasisScale(0f, 1f), 0f)
        // 分离/合并期间没有额外父级缩放，保留原来两种着色共同使用的局部倍率。
        assertEquals(1.15f, bottomTabEmphasisScale(1f), 0f)
    }

    @Test
    fun endpointHorizontalGapsMatchVerticalGapAcrossDirectionsAndDeformations() {
        for (density in listOf(1f, 2f, 3.25f, 4f)) {
            for (isLtr in listOf(true, false)) {
                for (widthDp in listOf(240f, 327.23f, 480f)) {
                    val g = BottomTabMorphGeometry(
                        widthDp * density, 59f * density, 47.5f * density,
                        4f * density, 1f, isLtr, 14f * density
                    )
                    val press = bottomTabPressScale(g.width, 16f * density, 1f)
                    val shellHeight = BottomBarMetrics.ExpandedSelectorHeight * density
                    for (rebound in listOf(.99f, 1f, 1.02f)) {
                        val shellWidth = g.width * press * rebound
                        for (scaleX in listOf(1.1f, 74f / 56f, 1.6f)) {
                            for (scaleY in listOf(1.05f, 74f / 56f, 1.5f)) {
                                val dropletWidth = g.tabWidth * scaleX
                                val dropletHeight = shellHeight * scaleY
                                val outset = g.selectorEdgeOutset(
                                    shellWidth, shellHeight, dropletWidth, dropletHeight
                                )
                                val left = g.selectorCenterX(if (isLtr) 0f else 3f, outset)
                                val right = g.selectorCenterX(if (isLtr) 3f else 0f, outset)
                                val verticalGap = (dropletHeight - shellHeight) / 2f
                                val leftGap = (g.width - shellWidth) / 2f - (left - dropletWidth / 2f)
                                val rightGap = right + dropletWidth / 2f - (g.width + shellWidth) / 2f
                                assertEquals(verticalGap, leftGap, .0005f)
                                assertEquals(verticalGap, rightGap, .0005f)
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun edgeAdjustmentLeavesMidpointAndRestingSelectionUnchanged() {
        val g = BottomTabMorphGeometry(327f, 59f, 47.5f, 4f, 1f, true, 14f)
        for (outset in listOf(-8f, 0f, 3f, 8f)) {
            assertEquals(g.width / 2f, g.selectorCenterX(1.5f, outset), .0001f)
            assertEquals(g.iconCenter(0).x, g.selectorCenterX(0f, outset * 0f), 0f)
            assertEquals(g.iconCenter(3).x, g.selectorCenterX(3f, outset * 0f), 0f)
        }
    }

    @Test
    fun equalGapMovesEndpointsOutBeyondThePreviousArcCenterAlignment() {
        val g = BottomTabMorphGeometry(327f, 59f, 47.5f, 4f, 1f, true, 14f)
        val press = bottomTabPressScale(g.width, 16f, 1f)
        val dropletScale = 74f / 56f
        val dropletWidth = g.tabWidth * dropletScale
        val hiddenHeight = BottomBarMetrics.ExpandedSelectorHeight
        val dropletHeight = hiddenHeight * dropletScale
        // 旧实现对齐椭圆圆弧中心，隐藏层仅横向放大，因而左右间距小于上下。
        val oldCenter = (g.width - g.width * press + hiddenHeight * press) / 2f +
            (dropletWidth - dropletHeight) / 2f
        val oldOutset = g.padding + g.tabWidth / 2f - oldCenter
        val newOutset = g.selectorEdgeOutset(
            g.width * press, hiddenHeight, dropletWidth, dropletHeight
        )
        val addedOutset = hiddenHeight * (press - 1f) / 2f
        assertEquals(addedOutset, newOutset - oldOutset, .0001f)
        assertTrue(addedOutset in 1.2f..1.3f)
        assertTrue(g.selectorCenterX(0f, newOutset) < g.selectorCenterX(0f, oldOutset))
        assertTrue(g.selectorCenterX(3f, newOutset) > g.selectorCenterX(3f, oldOutset))
    }
}
