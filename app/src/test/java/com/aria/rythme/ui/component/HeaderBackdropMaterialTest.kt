package com.aria.rythme.ui.component

import org.junit.Assert.*
import org.junit.Test

class HeaderBackdropMaterialTest {
    @Test fun blurStrengthFallsContinuouslyTowardTheBottom() {
        assertEquals(1f, HeaderBackdropMaterial.strength(0f), 0f)
        assertEquals(0f, HeaderBackdropMaterial.strength(1f), 0f)
        var previous = 1f
        for (step in 0..100) {
            val current = HeaderBackdropMaterial.strength(step / 100f)
            assertTrue(current <= previous)
            previous = current
        }
        assertTrue(HeaderBackdropMaterial.topRadiusDp > HeaderBackdropMaterial.bottomRadiusDp)
        assertTrue(HeaderBackdropMaterial.bottomRadiusDp > 0f)
    }

    @Test fun tintKeepsBottomColorWhileProtectingStatusBarReadability() {
        assertEquals(.85f, HeaderBackdropMaterial.tintAlpha(0f), .00001f)
        assertEquals(.4f, HeaderBackdropMaterial.tintAlpha(1f), .00001f)
        assertEquals(HeaderBackdropMaterial.tintAlpha(0f), HeaderBackdropMaterial.tintAlpha(-1f), 0f)
        assertEquals(HeaderBackdropMaterial.tintAlpha(1f), HeaderBackdropMaterial.tintAlpha(2f), 0f)
    }
}
