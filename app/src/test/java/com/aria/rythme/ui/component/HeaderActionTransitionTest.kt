package com.aria.rythme.ui.component

import org.junit.Assert.*
import org.junit.Test

class HeaderActionTransitionTest {
    private val filter = Action.Icon("filter", 1)
    private val more = Action.Icon("more", 2)
    private val star = Action.Icon("star", 3)
    private val avatar = Action.Avatar("avatar", name = "ARiA")
    private val empty = headerActionLayout(emptyList(), emptyList())
    private val single = headerActionLayout(emptyList(), listOf(more))
    private val double = headerActionLayout(emptyList(), listOf(filter, more))
    private val root = headerActionLayout(listOf(more), listOf(avatar))
    private val triple = headerActionLayout(listOf(star, filter), listOf(more))
    private val add = Action.Icon("add", 4)
    private val library = headerActionLayout(listOf(Action.Icon("add", 6), Action.Icon("edit_library", 5)), listOf(avatar))
    private val playlists = headerActionLayout(listOf(add, filter), listOf(more))

    @Test fun layoutsMatchSteadySlotsAndTrailingEdge() {
        assertEquals(57f, single.width, 0f)
        assertEquals(116f, double.width, 0f)
        assertEquals(173f, triple.width, 0f)
        listOf(single, double, root, triple).forEach {
            assertEquals(-6f, it.shells.last().bounds.right, 0f)
            assertEquals(45f, it.shells.last().bounds.height, 0f)
        }
    }

    @Test fun allTopologiesHaveExactEndpointsAndFiniteIntermediateGeometry() {
        val layouts = listOf(empty, single, double, root, triple, library, playlists)
        for (from in layouts) for (to in layouts) {
            val morph = HeaderActionMorph(from, to)
            assertEquals(from, morph.frame(0f))
            assertEquals(to, morph.frame(1f))
            for (step in 1..99) {
                val frame = morph.frame(step / 100f)
                assertTrue(frame.width.isFinite())
                assertTrue(frame.shells.size <= 2)
                frame.shells.forEach { assertTrue(it.bounds.width + 0.0001f >= it.bounds.height) }
                assertEquals(frame.glyphs.size, frame.glyphs.map { it.identity }.distinct().size)
            }
        }
    }

    @Test fun commonMoreMovesAcrossGroupsWithoutFadingBlurringOrDuplication() {
        val morph = HeaderActionMorph(root, triple)
        for (step in 0..100) {
            val moreGlyph = morph.frame(step / 100f).glyphs.single { it.action.key == "more" }
            assertEquals(1f, moreGlyph.alpha, 0f)
            assertEquals(0f, moreGlyph.blur, 0f)
        }
        assertTrue(root.glyphs.first().x != triple.glyphs.last().x)
    }

    @Test fun sameAddGlyphKeepsItsSlotWithLocalScaleWhileOtherActionsAreReplaced() {
        val source = headerActionLayout(listOf(add, Action.Icon("edit_library", 5)), listOf(avatar))
        val morph = HeaderActionMorph(source, playlists)
        val addX = source.glyphs.single { it.action.key == "add" }.x
        val avatarX = library.glyphs.single { it.action.key == "avatar" }.x
        assertFalse(library.glyphs.any { it.action.key == "more" })
        for (step in 0..100) {
            val frame = morph.frame(step / 100f)
            val addGlyph = frame.glyphs.single { it.action.key == "add" }
            val center = library.shells.first().bounds.center.x
            assertEquals(center + (addX - center) * addGlyph.scale, addGlyph.x, 0.0001f)
            assertEquals(1f, addGlyph.alpha, 0f)
            assertEquals(0f, addGlyph.blur, 0f)
            frame.glyphs.filter { it.action.key in setOf("avatar", "more") }.forEach {
                assertEquals(avatarX, it.x, 0f)
            }
        }
        assertTrue(morph.frame(0.2f).glyphs.single { it.action.key == "avatar" }.alpha < 1f)
        assertTrue(morph.frame(120f / 520f).glyphs.single { it.action.key == "more" }.blur > 0f)
    }

    @Test fun libraryCompositeAddReplacesWithPlaylistPlusDespiteSameActionKey() {
        val morph = HeaderActionMorph(library, playlists)
        val replacing = morph.frame(120f / 520f).glyphs.filter { it.action.key == "add" }
        assertEquals(setOf(4, 6), replacing.map { (it.action as Action.Icon).iconRes }.toSet())
        assertTrue(replacing.all { it.blur > 0f })
        val final = morph.frame(1f).glyphs.single { it.action.key == "add" }
        assertEquals(4, (final.action as Action.Icon).iconRes)
        assertEquals(0f, final.blur, 0f)
    }

    @Test fun secondaryMoreRemainsInRightmostSlotWhileSurfaceChanges() {
        for (from in listOf(single, double, playlists)) for (to in listOf(single, double, playlists)) {
            val morph = HeaderActionMorph(from, to)
            for (step in 0..100) {
                val frame = morph.frame(step / 100f)
                val moreGlyph = frame.glyphs.single { it.action.key == "more" }
                assertTrue(frame.glyphs.all { it.x <= moreGlyph.x })
                assertEquals(1f, moreGlyph.alpha, 0f)
                assertEquals(0f, moreGlyph.blur, 0f)
            }
        }
    }

    @Test fun replacementHasHoldFadeAndSharpenWhileUnchangedIconStaysClear() {
        val morph = HeaderActionMorph(double, headerActionLayout(emptyList(), listOf(star, more)))
        assertEquals(1f, morph.frame(0.03f).glyphs.single { it.action.key == "filter" }.alpha, 0f)
        assertFalse(morph.frame(0.5f).glyphs.any { it.action.key == "filter" })
        assertTrue(morph.frame(120f / 520f).glyphs.single { it.action.key == "star" }.blur > 0f)
        assertEquals(0f, morph.frame(0.9f).glyphs.single { it.action.key == "star" }.blur, 0f)
    }

    @Test fun interruptionRebasesFromVisibleFrameInsteadOfEndpoint() {
        val middle = HeaderActionMorph(root, triple).frame(0.35f)
        val returning = HeaderActionMorph(middle, root)
        assertEquals(middle, returning.frame(0f))
        assertEquals(root, returning.frame(1f))
        assertEquals(middle.glyphs.map { it.identity }.distinct().size, middle.glyphs.size)
        assertEquals(middle.connection, returning.frame(0.00001f).connection, 0.0001f)
    }

    @Test fun visualIdentityIgnoresCallbackAndStateColor() {
        val changed = headerActionLayout(emptyList(), listOf(more.copy(isActive = true, onClick = {})))
        assertEquals(single.glyphs.single().identity, changed.glyphs.single().identity)
        val glyph = HeaderActionMorph(single, changed).frame(0.5f).glyphs.single()
        assertEquals(1f, glyph.alpha, 0f)
        assertEquals(0f, glyph.blur, 0f)
        assertTrue((glyph.action as Action.Icon).isActive)
    }

    @Test fun tabSwitchSnapsAndNavigationUsesRemainingEntryProgress() {
        val state = HeaderActionTransitionState(root, "root", "root")
        state.retarget(triple, "child", "triple", 0.2f, false)
        assertEquals(0f, state.progress(0.2f), 0f)
        assertEquals(0.5f, state.progress(0.6f), 0.0001f)
        state.lastFrame = state.morph!!.frame(0.4f)
        state.retarget(single, "tab", "single", 0f, true)
        assertNull(state.morph)
        assertEquals(single, state.lastFrame)
    }

    @Test fun completedMotionCannotRestartWhenOldEntryBeginsToExit() {
        val state = HeaderActionTransitionState(root, "root", "root")
        state.retarget(triple, "child", "triple", 0f, false)
        state.finish(triple)
        assertNull(state.morph)
        assertEquals(triple, state.lastFrame)
    }

    @Test fun glassKeepsSpatialOrderEvenWhenMoreChangesGroups() {
        for (step in 1..99) {
            val shells = HeaderActionMorph(root, triple).frame(step / 100f).shells
            assertTrue(shells.single { "filter" in it.keys }.bounds.center.x < shells.single { "avatar" in it.keys }.bounds.center.x)
        }
    }

    @Test fun sharedMoreScalesAndMovesWithShellButNeverBlurs() {
        val target = headerActionLayout(emptyList(), listOf(star, more))
        val morph = HeaderActionMorph(double, target)
        val initial = double.glyphs.single { it.action.key == "more" }
        val peak = morph.frame(100f / 520f).glyphs.single { it.action.key == "more" }
        val trough = morph.frame(330f / 520f).glyphs.single { it.action.key == "more" }
        assertEquals(1.20f, peak.scale, 0.0001f)
        assertTrue(peak.x > initial.x)
        assertEquals(.968f, trough.scale, 0.0001f)
        assertTrue(trough.x < initial.x)
        assertEquals(0f, peak.blur, 0f)
        assertEquals(0f, trough.blur, 0f)
    }

    @Test fun geometryHasMeasuredPeakAndSmallNegativeTailAfterContentIsClear() {
        val morph = HeaderActionMorph(library, playlists)
        assertEquals(54f, morph.frame(100f / 520f).shells.first().bounds.height, .001f)
        val tail = morph.frame(330f / 520f)
        assertEquals(43.56f, tail.shells.first().bounds.height, .001f)
        assertTrue(tail.glyphs.all { it.alpha == 1f && it.blur == 0f })
        assertEquals(playlists, morph.frame(1f))
    }

    @Test fun mergingConsumesThinSideInsteadOfDuplicatingFullTarget() {
        val frame = HeaderActionMorph(library, double).frame(90f / 520f)
        val side = frame.shells.single { "edit_library" in it.keys }.bounds
        val main = frame.shells.single { "avatar" in it.keys }.bounds
        assertTrue(side.height < main.height * .65f)
        assertTrue(side.width < library.shells.first().bounds.width * .6f)
        assertTrue(side.center.x < main.center.x)
    }

    @Test fun splittingGrowsSideBeforeDetachingAndRestoresExactLayout() {
        val morph = HeaderActionMorph(double, library)
        val frame = morph.frame(90f / 520f)
        val side = frame.shells.single { "edit_library" in it.keys }.bounds
        val main = frame.shells.single { "avatar" in it.keys }.bounds
        assertTrue(side.height < main.height * .65f)
        assertTrue(side.right > main.left)
        assertEquals(library, morph.frame(1f))
    }

    @Test fun fadeOnlyGroupsKeepFullCanvasSoTheyCannotBeClippedByGrowingLayout() {
        assertEquals(library.width, HeaderActionMorph(empty, library).frame(.05f).width, 0f)
        assertEquals(library.width, HeaderActionMorph(library, empty).frame(.05f).width, 0f)
        val entering = HeaderActionMorph(empty, library).frame(100f / 520f)
        assertEquals(0f, entering.connection, 0f)
        assertTrue(entering.shells.all { it.bounds.height == 45f })
    }
}
