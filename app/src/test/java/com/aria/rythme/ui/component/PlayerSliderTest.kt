package com.aria.rythme.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerSliderTest {
    @Test fun relativeDragStartsFromTheExistingProgress() {
        assertEquals(.25f, sliderDragProgress(.25f, 0f, 300f), .0001f)
        assertEquals(.75f, sliderDragProgress(.25f, 150f, 300f), .0001f)
    }
    @Test fun bothEndsAreClamped() {
        assertEquals(0f, sliderDragProgress(.25f, -300f, 300f), 0f)
        assertEquals(1f, sliderDragProgress(.75f, 300f, 300f), 0f)
    }
    @Test fun invalidGeometryDoesNotCreateAnInvalidSeek() {
        assertEquals(.5f, sliderDragProgress(.5f, 100f, 0f), 0f)
        assertEquals(.5f, sliderDragProgress(.5f, Float.NaN, 300f), 0f)
    }
}
