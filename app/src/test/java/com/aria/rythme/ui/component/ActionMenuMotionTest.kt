package com.aria.rythme.ui.component

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.*
import org.junit.Test

class ActionMenuMotionTest {
    @Test fun closingDoesNotReplayTheOpeningOvershoot() {
        val source=Rect(256f,60f,360f,105f)
        val target=Rect(110f,60f,360f,373f)
        for(step in 0..100) {
            val g=ActionMenuMotion.closingGeometry(source,target,step/100f,34f,.25f)
            assertTrue(g.body.height <= target.height+.001f)
        }
        val early=ActionMenuMotion.closingGeometry(source,target,100f/300f,34f,.25f)
        assertTrue(early.body.width < target.width*.6f)
        val middle=ActionMenuMotion.closingGeometry(source,target,167f/300f,34f,.25f)
        assertEquals(source.top,middle.body.top,.001f)
        assertTrue(middle.distance(source.center.x,source.center.y)<0f)
        assertTrue(middle.distance(source.left+source.width*.25f,source.center.y)<0f)
        assertTrue(middle.distance(source.left+source.width*.75f,source.center.y)<0f)
    }
    @Test fun shortAndTallMenusBothShrinkWithoutAnInitialHeightRebound() {
        for(height in listOf(181f,313f,650f)) {
            val source=Rect(256f,60f,360f,105f)
            val target=Rect(110f,60f,360f,60f+height)
            var previous=height
            for(step in 0..300) {
                val g=ActionMenuMotion.closingGeometry(source,target,step/300f,34f,.25f)
                assertTrue(g.body.height<=previous+.001f)
                assertTrue(g.body.width>0f && g.topCorner>0f)
                previous=g.body.height
            }
        }
    }
    @Test fun openingContractsTheWholeCapsuleBeforeGrowing() {
        val source=Rect(256f,60f,360f,105f)
        val target=Rect(110f,60f,360f,373f)
        val compact=ActionMenuMotion.geometry(source,target,17f/500f,34f,.25f)
        assertTrue(compact.body.width<source.width*.65f)
        assertEquals(0f,compact.neck,0f)
    }
    @Test fun intermediateCurveKnotsDoNotResetVelocityToZero() {
        val knots=arrayOf(0f to 0f,50f to .4f,100f to 1f,200f to 1f)
        val before=ActionMenuMotion.curve(50f,*knots)-ActionMenuMotion.curve(49.9f,*knots)
        val after=ActionMenuMotion.curve(50.1f,*knots)-ActionMenuMotion.curve(50f,*knots)
        assertTrue(before>.0005f)
        assertEquals(before,after,.00002f)
    }
    @Test fun openingFoldFollowsTheRemainingGlyphRatherThanAButtonNameOrPage() {
        assertEquals(.5f, ActionMenuMotion.openingFoldFraction(1, 0), 0f)
        assertEquals(.25f, ActionMenuMotion.openingFoldFraction(2, 1), 0f)
        assertEquals(.75f, ActionMenuMotion.openingFoldFraction(2, 0), 0f)
        assertEquals(.5f, ActionMenuMotion.openingFoldFraction(0, -1), 0f)
    }
    @Test fun closingFoldDoesNotInheritTheOppositeButtonRule() {
        assertEquals(.5f, ActionMenuMotion.ClosingFoldFraction, 0f)
        for (pressedIndex in 0..1) {
            assertNotEquals(ActionMenuMotion.openingFoldFraction(2, pressedIndex),
                ActionMenuMotion.ClosingFoldFraction)
        }
    }
    @Test fun closingConvergesAtTheWholeCapsuleCenterForEverySourceSize() {
        for (width in listOf(45f, 104f, 160f)) for (height in listOf(181f, 313f, 650f)) {
            val source = Rect(360f - width, 60f, 360f, 105f)
            val target = Rect(110f, 60f, 360f, 60f + height)
            for (ms in listOf(167f, 200f, 233f, 270f, 300f)) {
                val frame = ActionMenuMotion.closeFromSnapshot(
                    MenuMorphGeometry(target, 34f), source, target, ms / 300f,
                    ActionMenuMotion.ClosingFoldFraction)
                assertEquals(source.center.x, frame.body.center.x, .001f)
                assertEquals(0f, frame.skew, .001f)
            }
        }
    }
    @Test fun dismissalStartsExactlyAtTheRenderedFrameThroughoutOpening() {
        val source=Rect(256f,60f,360f,105f)
        for(height in listOf(181f,313f,650f)) for(ms in listOf(0f,17f,50f,100f,200f,350f,499f,500f)) {
            val target=Rect(110f,60f,360f,60f+height)
            val start=ActionMenuMotion.geometry(source,target,ms/500f,34f,.75f)
            assertEquals(start,ActionMenuMotion.closeFromSnapshot(start,source,target,0f,.25f))
            assertEquals(MenuMorphGeometry(source,source.height/2f),
                ActionMenuMotion.closeFromSnapshot(start,source,target,1f,.25f))
        }
    }
    @Test fun earlyAndLateDismissalsHaveNoEndpointOrFirstFrameGeometryJump() {
        val source=Rect(256f,60f,360f,105f)
        val target=Rect(110f,60f,360f,241f)
        for(ms in listOf(17f,80f,200f,350f,499f,500f)) {
            val start=ActionMenuMotion.geometry(source,target,ms/500f,34f,.75f)
            var previous=start
            for(step in 1..300) {
                val frame=ActionMenuMotion.closeFromSnapshot(start,source,target,step/300f,.25f)
                assertTrue(frame.body.width>0f && frame.body.height>0f)
                assertTrue(kotlin.math.abs(frame.body.left-previous.body.left)<4f)
                assertTrue(kotlin.math.abs(frame.body.bottom-previous.body.bottom)<4f)
                assertTrue(frame.distance(frame.body.center.x,frame.body.center.y).isFinite())
                previous=frame
            }
        }
    }
    @Test fun aSmallInterruptedDropDoesNotExpandIntoTheFullMenu() {
        val source=Rect(256f,60f,360f,105f)
        val target=Rect(110f,60f,360f,373f)
        val start=ActionMenuMotion.geometry(source,target,17f/500f,34f,.75f)
        for(step in 0..100) {
            val frame=ActionMenuMotion.closeFromSnapshot(start,source,target,step/100f,.25f)
            assertTrue(frame.body.height<=maxOf(start.body.height,source.height)+.01f)
        }
    }
    @Test fun anOpeningTailDismissalKeepsTheNormalClosingTrajectory() {
        val source=Rect(256f,60f,360f,105f)
        val target=Rect(110f,60f,360f,241f)
        val start=ActionMenuMotion.geometry(source,target,.7f,34f,.75f)
        val middle=ActionMenuMotion.closeFromSnapshot(start,source,target,.5f,.25f)
        assertTrue(middle.body.height>source.height*1.2f)
        assertEquals(300,ActionMenuMotion.CloseMillis)
    }
    @Test fun endpointsAreExactlyTheWholeSourceCapsuleAndFinalMenu() {
        for (width in listOf(45f, 104f, 160f)) {
            val source = Rect(360f - width, 60f, 360f, 105f)
            val target = Rect(110f, 60f, 360f, 373f)
            val closed = ActionMenuMotion.geometry(source, target, 0f, 34f)
            val open = ActionMenuMotion.geometry(source, target, 1f, 34f)
            assertEquals(source, closed.body)
            assertEquals(22.5f, closed.bodyCorner, 0f)
            assertEquals(target, open.body)
            assertEquals(34f, open.bodyCorner, 0f)
            assertEquals(0f,closed.neck,0f)
            assertEquals(0f,open.neck,0f)
        }
    }

    @Test fun oneModelWorksForDifferentSourcesAndMenuHeights() {
        for (sourceWidth in listOf(45f, 104f)) for (height in listOf(181f, 313f, 650f)) {
            val source = Rect(360f - sourceWidth, 60f, 360f, 105f)
            val target = Rect(110f, 60f, 360f, 60f + height)
            for (step in 0..100) {
                val g = ActionMenuMotion.geometry(source, target, step / 100f, 34f)
                assertTrue(g.body.width > 0f && g.body.height > 0f)
                assertTrue(g.bodyCorner.isFinite() && g.skew.isFinite())
                assertTrue(g.distance(g.body.center.x, g.body.center.y) < 0f)
                assertTrue(g.distance(-1000f, -1000f) > 0f)
            }
        }
    }

    @Test fun menuFitsSafeAreaWithoutChangingItsRightAnchorWhenSpaceAllows() {
        val source = Rect(255f, 60f, 360f, 105f)
        assertEquals(Rect(110f, 60f, 360f, 373f),
            ActionMenuMotion.targetBounds(source, 250f, 313f, 393f, 850f, 50f, 30f, 12f))
        val constrained = ActionMenuMotion.targetBounds(source, 250f, 1000f, 220f, 500f, 50f, 30f, 12f)
        assertEquals(12f, constrained.left, 0f)
        assertEquals(208f, constrained.right, 0f)
        assertEquals(50f, constrained.top, 0f)
        assertEquals(458f, constrained.bottom, 0f)
    }

    @Test fun sourceRemainsOwnedByOverlayUntilItsOwnExitFinishes() {
        val state = OverlayMenuState()
        val first = OverlayMenu.ActionMenu("first", Rect(0f, 0f, 45f, 45f), emptyList())
        val second = first.copy(sourceKey = "second")
        state.show(first)
        state.dismiss()
        assertSame(first, state.presentedAction)
        assertTrue(state.isVisible)
        state.show(second)
        state.finishActionExit(first)
        assertSame(second, state.presentedAction)
        state.dismiss()
        state.finishActionExit(second)
        assertNull(state.presentedAction)
        assertFalse(state.isVisible)
    }
}
