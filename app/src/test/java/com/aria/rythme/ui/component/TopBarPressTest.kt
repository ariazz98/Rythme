package com.aria.rythme.ui.component

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test

class TopBarPressTest {
    @Test fun avatarHeadroomCoversItsGlassContainerWithoutAddingTheTwoGains() {
        assertEquals(1.92f, topBarPressHeadroom(1f, avatar = true, dark = false), 0.001f)
        assertEquals(6.5f, topBarPressHeadroom(1f, avatar = true, dark = true), 0f)
        assertEquals(1f, topBarPressHeadroom(0f, avatar = true, dark = true), 0f)
        assertEquals(1f, topBarPressHeadroom(Float.NaN, avatar = true, dark = false), 0f)
        assertEquals(3f, topBarPressHeadroom(1f, avatar = false, dark = true), 0f)
    }

    @Test fun darkPressHasItsOwnLightAndForegroundProfileWithoutChangingLightMode() {
        assertEquals(0.25f, topBarDarkPressDelta(0f, 1f), 0.001f)
        assertEquals(0f, topBarDarkPressDelta(0f, 0f), 0f)
        assertTrue(topBarDarkPressDelta(1f, 1f) < topBarDarkPressDelta(0f, 1f))
        assertEquals(3f, topBarDarkForegroundGain(1f), 0f)
        assertEquals(0.6f, TopBarPressMotion.iconTargetAlpha(true, dark = true), 0f)
        assertEquals(0.2f, TopBarPressMotion.iconTargetAlpha(true, dark = false), 0f)
        assertEquals(1f, TopBarPressMotion.iconTargetAlpha(false, dark = true), 0f)
        assertEquals(1f, TopBarPressMotion.iconTargetAlpha(true, avatar = true, dark = true), 0f)
    }

    @Test fun darkAvatarUsesAnIndependentDimBaseAndHdrGain() {
        val dark = topBarAvatarColors(0f, hdr = true, dark = true)[0]
        val light = topBarAvatarColors(0f, hdr = true, dark = false)[0]
        assertTrue(dark.luminance() < light.luminance() * 0.25f)
        assertEquals(6.5f, topBarAvatarGain(1f, dark = true), 0f)
        assertEquals(1.9f, topBarAvatarGain(1f, dark = false), 0.001f)
        assertEquals(1f, topBarAvatarGain(0f, dark = true), 0f)
    }

    @Test fun avatarHdrGainPreservesLinearColourRatiosAndReturnsToOne() {
        assertEquals(1f, topBarAvatarGain(0f), 0f)
        assertEquals(1.9f, topBarAvatarGain(1f), 0.001f)
        assertEquals(1f, topBarAvatarGain(Float.NaN), 0f)
        val rest = topBarAvatarColors(0f, true)[0].convert(ColorSpaces.LinearExtendedSrgb)
        val held = topBarAvatarColors(1f, true)[0].convert(ColorSpaces.LinearExtendedSrgb)
        // Color 使用 FP16 打包，往返传递函数会累积数个 ULP；仍必须保留 SDR 白以上的分量。
        assertEquals(rest.red * 1.9f, held.red, 0.004f)
        assertEquals(rest.green * 1.9f, held.green, 0.004f)
        assertEquals(rest.blue * 1.9f, held.blue, 0.004f)
        assertTrue(held.green > 1f)
        assertEquals(rest.alpha, held.alpha, 0f)
    }

    @Test fun highlightHasABroadLocalPeakAndNoIdleGain() {
        assertEquals(0f, topBarPressLightStrength(0f, 0f), 0f)
        assertEquals(0.92f, topBarPressLightStrength(0f, 1f), 0.001f)
        assertTrue(topBarPressLightStrength(1f, 1f) < topBarPressLightStrength(0f, 1f))
        assertTrue(topBarPressLightStrength(2f, 1f) > 0.12f)
        assertEquals(0f, topBarPressLightStrength(Float.NaN, 1f), 0f)
    }

    @Test fun pressDimmingHasNoFadeDelay() {
        val spec = TopBarPressMotion.iconDown.vectorize(Float.VectorConverter)
        assertEquals(0.20f, spec.getValueFromNanos(
            0L, AnimationVector1D(1f), AnimationVector1D(0.20f), AnimationVector1D(0f)
        ).value, 0f)
    }

    @Test fun heldIconsRemainFaintlyVisibleButAvatarsAndUnpressedNeighboursStayOpaque() {
        assertEquals(0.20f, TopBarPressMotion.iconTargetAlpha(pressed = true), 0f)
        assertTrue(TopBarPressMotion.iconTargetAlpha(pressed = true) > 0f)
        assertEquals(1f, TopBarPressMotion.iconTargetAlpha(pressed = false), 0f)
        assertEquals(1f, TopBarPressMotion.iconTargetAlpha(pressed = true, avatar = true), 0f)
    }

    @Test fun buttonsGainTheSameWidthInsteadOfTheSameScale() {
        assertEquals(61f, 45f * TopBarPressMotion.scale(45f, false, 1f), 0.001f)
        assertEquals(120f, 104f * TopBarPressMotion.scale(104f, false, 1f), 0.001f)
        assertEquals(1.36f, TopBarPressMotion.scale(45f, true, 1f), 0.001f)
    }

    @Test fun releaseUndershootIsNotClampedToStaticSize() {
        assertTrue(TopBarPressMotion.scale(45f, false, -0.25f) < 1f)
        assertTrue(TopBarPressMotion.scale(104f, false, 1.08f) > TopBarPressMotion.scale(104f, false, 1f))
        assertEquals(1f, TopBarPressMotion.scale(0f, false, 1f), 0f)
    }

    @Test fun pressingOneSlotDoesNotSelectItsNeighbourOrAnOutsidePoint() {
        assertEquals(0, TopBarPressMotion.hitIndex(51.9f, 104f, 2))
        assertEquals(1, TopBarPressMotion.hitIndex(52f, 104f, 2))
        assertNull(TopBarPressMotion.hitIndex(104f, 104f, 2))
        assertNull(TopBarPressMotion.hitIndex(-1f, 104f, 2))
        assertNull(TopBarPressMotion.hitIndex(Float.NaN, 104f, 2))
    }

    @Test fun neckAppearsOnlyWhenTheSurfacesApproach() {
        val left = Rect(0f, 0f, 45f, 45f)
        val rest = Rect(57f, 0f, 102f, 45f)
        val geometry = TopBarJoinedGeometry(left, rest, 14f)
        assertTrue(geometry.distance(51f, 22.5f) > 0f)
        val pressed = Rect(rest.center - Offset(30.6f, 30.6f), rest.center + Offset(30.6f, 30.6f))
        val joined = TopBarJoinedGeometry(left, pressed, 14f)
        assertTrue(joined.distance(47f, 22.5f) < 0f)
        assertTrue(joined.distance(47f, -10f) > 0f)
    }

    @Test fun joinedSurfaceRetainsOuterEdgesAndTopBottomSymmetry() {
        val g = TopBarJoinedGeometry(Rect(0f, 0f, 45f, 45f), Rect(49f, -8f, 110f, 53f), 14f)
        assertEquals(0f, g.distance(0f, 22.5f), 0.001f)
        assertEquals(0f, g.distance(110f, 22.5f), 0.001f)
        for (x in 0..110 step 2) assertEquals(g.distance(x.toFloat(), 12.5f), g.distance(x.toFloat(), 32.5f), 0.001f)
    }
}
