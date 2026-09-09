package com.aria.rythme.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class TopBarComponentMetricsTest {
    @Test
    fun singleButtonIsCircularAndDoubleButtonHasWiderIndependentSlots() {
        assertEquals(45f, TopBarComponentMetrics.surfaceWidth(1), 0f)
        assertEquals(104f, TopBarComponentMetrics.surfaceWidth(2), 0f)
        assertEquals(48f, TopBarComponentMetrics.itemWidth(2), 0f)
        assertEquals(8f, TopBarComponentMetrics.surfaceWidth(2) - 2 * TopBarComponentMetrics.itemWidth(2), 0f)
        assertEquals(0f, TopBarComponentMetrics.surfaceWidth(0), 0f)
    }

    @Test
    fun avatarInsetLeavesGlassVisibleOnAllSides() {
        assertEquals(40f, TopBarComponentMetrics.avatarSize, 0f)
        assertEquals(
            TopBarComponentMetrics.SurfaceHeight,
            TopBarComponentMetrics.avatarSize + 2f * TopBarComponentMetrics.AvatarInset,
            0f
        )
    }

    @Test
    fun wrapperPaddingPreservesTrailingEdgeAndIndependentGroupGap() {
        val halfGap = TopBarComponentMetrics.GroupGap / 2f
        assertEquals(15.5f, TopBarComponentMetrics.rowPadding + halfGap, 0f)
        assertEquals(19.5f, TopBarComponentMetrics.rowPadding + TopBarComponentMetrics.titlePadding, 0f)
        assertEquals(12f, halfGap * 2f, 0f)
    }

    @Test
    fun phoneGeometryMatchesReferenceAfterWidthNormalization() {
        val density = 3.25f
        val deviceWidth = 1200f
        val scale = TopBarComponentMetrics.referenceScale(deviceWidth / density)
        assertEquals(45f * deviceWidth / 393f, TopBarComponentMetrics.SurfaceHeight * scale * density, 0.001f)
        assertEquals(166.1f, TopBarComponentMetrics.avatarSize * scale * density * TopBarComponentMetrics.AvatarPressedScale, 0.1f)
        assertEquals(1f, TopBarComponentMetrics.referenceScale(800f), 0f)
        assertEquals(1f, TopBarComponentMetrics.referenceScale(0f), 0f)
    }

    @Test
    fun visualGapDoesNotCreateAnUntouchableStripInsideCapsule() {
        assertEquals(104f, 2f * TopBarComponentMetrics.touchWidth(2), 0f)
        assertEquals(24.65f, TopBarComponentMetrics.touchWidth(2) / 2f + TopBarComponentMetrics.iconOffset(2, 0), 0.001f)
        assertEquals(80.65f, TopBarComponentMetrics.touchWidth(2) * 1.5f + TopBarComponentMetrics.iconOffset(2, 1), 0.001f)
    }
}
