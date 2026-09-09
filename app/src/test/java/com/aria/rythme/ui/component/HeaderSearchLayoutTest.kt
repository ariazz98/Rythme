package com.aria.rythme.ui.component

import org.junit.Assert.*
import org.junit.Test

class HeaderSearchLayoutTest {
    @Test fun derivedDimensionsPreserveApprovedSecondaryGeometry() {
        assertEquals(56f, HeaderSearchLayout.inlineHeight.value, 0f)
        assertEquals(12f, HeaderSearchLayout.toolbarSurfaceInset.value, 0f)
        assertEquals(9f, HeaderSearchLayout.closeTouchEndInset.value, 0f)
        assertEquals(21f, HeaderSearchLayout.bodyEndPadding(0f).value, 0f)
        assertEquals(77f, HeaderSearchLayout.bodyEndPadding(1f).value, 0f)
        assertEquals(HeaderSearchLayout.closeGap.value,
            HeaderSearchLayout.bodyEndPadding(1f).value - HeaderSearchLayout.horizontalInset.value - HeaderSearchLayout.surfaceHeight.value, 0f)
    }

    @Test fun revealAndChromeTimingKeepTheirExistingWindows() {
        assertEquals(300, HeaderSearchMotion.activationDuration)
        assertEquals(52f, HeaderSearchMotion.closeEnterTravel.value, 0f)
        for (step in 0..100) {
            val p = step / 100f
            assertEquals(((p - .9f) / .1f).coerceIn(0f, 1f), HeaderSearchMotion.placeholderAlpha(p), .00001f)
            assertEquals((1f - p * 3f).coerceIn(0f, 1f), HeaderSearchMotion.chromeAlpha(p), 0f)
        }
        assertFalse(HeaderSearchMotion.placeholderInteractive(.98f))
        assertTrue(HeaderSearchMotion.placeholderInteractive(1f))
    }
}
