package com.aria.rythme.ui.component

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.*
import org.junit.Test

class ActionMenuMotionTest {
    @Test fun dismissStartsAtActualSnapshotAndReturnsToSourceWithoutAnEndpointJump() {
        for (surface in MenuSourceSurface.entries) for (upward in listOf(false,true))
            for (width in listOf(21f,45f,104f)) for(height in listOf(100f,313f,650f)) {
            val source=Rect(350f-width,80f,350f,125f)
            val target=Rect(100f,80f,350f,80f+height)
            for(open in listOf(.02f,.12f,.3f,.7f,1f)) {
                val start=ActionMenuMotion.openingGeometry(source,target,open,34f,.5f,upward)
                fun at(p:Float)=ActionMenuMotion.dismissGeometry(start,source,target,p,surface,upward)
                assertEquals(start,at(0f))
                assertEquals(source,at(1f).body)
                assertTrue((at(.00001f).body.center-start.body.center).getDistance()<.1f)
                assertTrue((at(.99999f).body.center-source.center).getDistance()<.1f)
                for(step in 0..100) {
                    val g=at(step/100f)
                    assertTrue(g.body.width>0f && g.body.height>0f)
                    assertTrue(g.distance(g.body.center.x,g.body.center.y).isFinite())
                    assertTrue(g.neck in 0f..0.31f)
                }
            }
        }
    }
    @Test fun onlyIconGlassFadesAndItsTransparentTailOutlivesTheContent() {
        assertEquals(1f, ActionMenuMotion.dismissalAlpha(0f,MenuSourceSurface.Icon), 0f)
        assertEquals(1f, ActionMenuMotion.dismissalAlpha(.7f,MenuSourceSurface.Icon), 0f)
        assertEquals(0f, ActionMenuMotion.dismissalAlpha(1f,MenuSourceSurface.Icon), 0f)
        val values = (0..100).map { ActionMenuMotion.dismissalAlpha(it / 100f,MenuSourceSurface.Icon) }
        assertTrue(values.zipWithNext().all { (a, b) -> a >= b })
        assertTrue((0..100).all { ActionMenuMotion.dismissalAlpha(it/100f,MenuSourceSurface.Glass)==1f })
    }
    @Test fun openingWidthReversesBeforeTopAndRightEdgeStaysAnchored() {
        val source = Rect(300f, 60f, 350f, 105f)
        for (height in listOf(181f, 313f, 650f)) {
            val target = Rect(100f, 60f, 350f, 60f + height)
            val early=ActionMenuMotion.geometry(source,target,.4f,34f).body
            val late=ActionMenuMotion.geometry(source,target,.54f,34f).body
            assertTrue(early.width>target.width && early.width>late.width)
            assertTrue(late.top<target.top && late.top<early.top)
            for(step in 30..100) assertEquals(target.right,
                ActionMenuMotion.geometry(source,target,step/100f,34f).body.right,.001f)
            assertEquals(target, ActionMenuMotion.geometry(source, target, 1f, 34f).body)
        }
    }

    @Test fun glassSourceOvershootsUpwardThenReturnsAfterTheTailHasRetracted() {
        val source=Rect(246f,60f,350f,105f)
        val target=Rect(100f,60f,350f,373f)
        val start=MenuMorphGeometry(target,34f)
        fun at(ms:Float)=ActionMenuMotion.dismissGeometry(start,source,target,ms/480f,MenuSourceSurface.Glass)
        val recovered=at(260f)
        assertEquals(0f,recovered.neck,.001f)
        assertTrue(recovered.body.top<source.top-4f)
        assertTrue(recovered.body.width<source.width)
        assertTrue(at(360f).body.top>recovered.body.top)
        assertEquals(source,at(480f).body)
        assertTrue(at(150f).neck>.1f)
    }

    @Test fun upwardClosingMirrorsTheEntireNeckNotOnlyItsBoundingBox() {
        val source=Rect(246f,60f,350f,105f)
        val target=Rect(100f,60f,350f,373f)
        fun flip(r:Rect)=Rect(r.left,-r.bottom,r.right,-r.top)
        for(step in 1..99) {
            val down=ActionMenuMotion.dismissGeometry(MenuMorphGeometry(target,34f),source,target,step/100f,MenuSourceSurface.Glass)
            val up=ActionMenuMotion.dismissGeometry(MenuMorphGeometry(flip(target),34f,flipped=true),flip(source),flip(target),step/100f,MenuSourceSurface.Glass,true)
            assertEquals(flip(down.body),up.body)
            for(x in listOf(180f,270f,330f)) for(y in listOf(60f,90f,130f,220f))
                assertEquals(down.distance(x,y),up.distance(x,-y),.001f)
        }
    }

    @Test fun shortInterruptedOpeningDoesNotGrowIntoAFullMenuOnDismiss() {
        val source=Rect(246f,60f,350f,105f)
        val target=Rect(100f,60f,350f,710f)
        val start=ActionMenuMotion.geometry(source,target,.02f,34f)
        for(step in 0..100) {
            val g=ActionMenuMotion.dismissGeometry(start,source,target,step/100f,MenuSourceSurface.Glass)
            assertTrue(g.body.height<source.height*1.1f)
            assertTrue(g.body.width<=source.width+.01f)
        }
    }

    @Test fun sourceGlyphReturnsWhileTailIsStillVisibleAndMaterialHasCleared() {
        for(surface in MenuSourceSurface.entries) {
            val p=190f/ActionMenuMotion.dismissMillis(surface)
            assertEquals(1f,ActionMenuMotion.closingSourceAlpha(p,surface),.001f)
            assertEquals(1f,ActionMenuMotion.dismissalAlpha(p,surface),.001f)
            assertEquals(0f,ActionMenuMotion.closingMaterialProgress(p,surface),.001f)
        }
    }
    @Test fun upwardPresentationHasExactEndpointsAndFiniteIntermediateShapes() {
        val source = Rect(300f, 600f, 350f, 645f)
        val target = Rect(100f, 332f, 350f, 645f)
        assertEquals(source, ActionMenuMotion.openingGeometry(source, target, 0f, 34f, .5f, true).body)
        assertEquals(target, ActionMenuMotion.openingGeometry(source, target, 1f, 34f, .5f, true).body)
        for (step in 0..100) {
            val g = ActionMenuMotion.openingGeometry(source, target, step / 100f, 34f, .5f, true)
            assertTrue(g.body.width > 0 && g.body.height > 0)
            assertTrue(g.distance(g.body.center.x, g.body.center.y).isFinite())
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
