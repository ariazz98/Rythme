package com.aria.rythme.ui.component

import androidx.compose.ui.geometry.Size
import org.junit.Assert.*
import org.junit.Test

class MenuPanelTransformTest {
    @Test fun idleDoesNotChangeGeometry() {
        assertEquals(MenuPanelTransform(0f, 0f, 1f, 1f),
            menuPanelTransform(0f, 0f, Size(250f, 313f), 1f, PanelAnchor.TopEnd))
    }

    @Test fun dragCanExtendBelowTheOriginalViewport() {
        val height = 313f
        val t = menuPanelTransform(0f, 400f, Size(250f, height), 1f, PanelAnchor.TopEnd)
        assertEquals(.97f, t.scaleX, .0001f)
        assertEquals(1.03f, t.scaleY, .0001f)
        assertTrue(height / 2f + height / 2f * t.scaleY + t.y > height)
    }

    @Test fun densityAndCanvasPaddingMustNotChangeTheDragResponse() {
        val logical = menuPanelTransform(-70f, 160f, Size(250f, 313f), 1f, PanelAnchor.TopEnd)
        val pixels = menuPanelTransform(-210f, 480f, Size(750f, 939f), 3f, PanelAnchor.TopEnd)
        assertEquals(logical.x * 3f, pixels.x, .0001f)
        assertEquals(logical.y * 3f, pixels.y, .0001f)
        assertEquals(logical.scaleX, pixels.scaleX, .0001f)
        assertEquals(logical.scaleY, pixels.scaleY, .0001f)
    }

    @Test fun bottomAnchoredSongMenusKeepTheirExistingDirection() {
        val top = menuPanelTransform(40f, 150f, Size(250f, 313f), 1f, PanelAnchor.TopEnd)
        val bottom = menuPanelTransform(40f, -150f, Size(250f, 313f), 1f, PanelAnchor.BottomEnd)
        assertEquals(0f, top.x, 0f)
        assertEquals(-top.y, bottom.y, .0001f)
        assertEquals(top.scaleX, bottom.scaleX, .0001f)
        assertEquals(top.scaleY, bottom.scaleY, .0001f)
    }
}
