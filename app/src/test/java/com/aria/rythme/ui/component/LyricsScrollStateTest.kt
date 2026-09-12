package com.aria.rythme.ui.component

import org.junit.Assert.*
import org.junit.Test

class LyricsScrollStateTest {
    @Test fun wakeOnlyTapKeepsBlurUntilRelease() {
        val state = LyricsScrollState()
        state.onLyricsTouchDown(clearOnPress = false)
        assertFalse(state.isClearMode)
        assertTrue(state.isTouchClarityDeferred)
        assertFalse(canSelectLyric(4, state.browsingAtTouchStart))
        state.onLyricsTouchDown() // 后续 MOVE 不得覆盖首次按下的分类。
        assertFalse(state.isClearMode)
        state.onLyricsTouchReleased()
        assertFalse(state.isClearMode)
    }
    @Test fun nearTapLongPressAndDragCanClearDeferredBlur() {
        val state = LyricsScrollState()
        state.onLyricsTouchDown(clearOnPress = false)
        state.allowLyricsTouchClarity()
        assertTrue(state.isClearMode)
        state.onLyricsTouchReleased()
        assertFalse(state.isClearMode)
        state.onLyricsTouchDown(clearOnPress = false)
        state.onUserScrollStart()
        assertTrue(state.isClearMode)
        state.onLyricsTouchReleased()
        state.onUserScrollStop(1000)
        assertTrue(state.isClearMode)
    }
    @Test fun waitsForFullGracePeriodThenStaysClearUntilAnimationCompletes() {
        val state = LyricsScrollState()
        state.onUserScrollStart()
        state.onUserScrollStop(1000)
        state.onResumeTimerFired(3999, true)
        assertEquals(LyricsScrollMode.WaitingToResume, state.mode)
        state.onResumeTimerFired(4000, true)
        assertEquals(LyricsScrollMode.ReturningToFollow, state.mode)
        assertTrue(state.isClearMode)
        state.onFollowAnimationFinished()
        assertTrue(state.isAutoFollow)
        assertFalse(state.isClearMode)
    }
    @Test fun pausedBrowsingDoesNotReturnAndPlaybackRestartsGracePeriod() {
        val state = LyricsScrollState()
        state.onUserScrollStart()
        state.onUserScrollStop(1000)
        state.onResumeTimerFired(10000, false)
        assertEquals(LyricsScrollMode.WaitingToResume, state.mode)
        state.onPlaybackChanged(true, 10000)
        state.onResumeTimerFired(12999, true)
        assertFalse(state.canFollow)
        state.onResumeTimerFired(13000, true)
        assertTrue(state.canFollow)
    }
    @Test fun touchInterruptsFollowingAndRestartsWaitingOnRelease() {
        val state = LyricsScrollState()
        state.onUserScrollStart()
        state.onUserScrollStop(1000)
        state.onTouchDown()
        state.onResumeTimerFired(5000, true)
        assertFalse(state.canFollow)
        state.onTouchReleased(6000)
        state.onResumeTimerFired(8999, true)
        assertFalse(state.canFollow)
        state.onResumeTimerFired(9000, true)
        assertTrue(state.canFollow)
    }
    @Test fun staleAnimationCompletionCannotOverrideNewDrag() {
        val state = LyricsScrollState()
        state.onLyricLineClicked()
        state.onTouchDown()
        state.onUserScrollStart()
        state.onFollowAnimationFinished()
        assertEquals(LyricsScrollMode.ManualScrolling, state.mode)
        assertTrue(state.isClearMode)
    }
    @Test fun explicitSelectionCanReturnWhilePaused() {
        val state = LyricsScrollState()
        state.onUserScrollStart()
        state.onUserScrollStop(0)
        state.onLyricLineClicked()
        assertTrue(state.canFollow)
        assertFalse(state.isClearMode)
        state.onFollowAnimationFinished()
        assertTrue(state.isAutoFollow)
    }
    @Test fun controlsOnlyAutoHideDuringIdlePlaybackFollow() {
        val state = LyricsScrollState()
        state.reset(1000)
        assertFalse(state.canAutoHideControls(4999, true))
        assertTrue(state.canAutoHideControls(5000, true))
        assertFalse(state.canAutoHideControls(5000, false))
        state.onTouchDown()
        assertFalse(state.canAutoHideControls(10000, true))
        state.onTouchReleased(10000)
        state.onLyricLineClicked()
        assertFalse(state.canAutoHideControls(20000, true))
        state.onFollowAnimationFinished()
        assertTrue(state.canAutoHideControls(20000, true))
    }
    @Test fun tapUsesStateBeforeTouchClearsBlurAndHonorsThreeSentenceBoundary() {
        val state = LyricsScrollState()
        state.onLyricsTouchDown()
        assertTrue(state.isClearMode)
        assertFalse(state.browsingAtTouchStart)
        assertTrue(canSelectLyric(3, state.browsingAtTouchStart))
        assertTrue(canSelectLyric(-3, state.browsingAtTouchStart))
        assertFalse(canSelectLyric(4, state.browsingAtTouchStart))
        assertFalse(canSelectLyric(-4, state.browsingAtTouchStart))
        state.onUserScrollStart()
        state.onLyricsTouchReleased()
        state.onUserScrollStop(1000)
        state.onLyricsTouchDown()
        assertTrue(state.browsingAtTouchStart)
        assertTrue(canSelectLyric(100, state.browsingAtTouchStart))
    }
    @Test fun selectedReturnRestoresBlurImmediatelyButNewTouchCanInterrupt() {
        val state = LyricsScrollState()
        state.onUserScrollStart()
        state.onUserScrollStop(1000)
        state.onLyricLineClicked()
        assertFalse(state.isClearMode)
        assertEquals(LyricsScrollMode.ReturningToFollow, state.mode)
        state.onPlaybackChanged(false, 1100)
        assertTrue(state.canFollow)
        state.onLyricsTouchDown()
        assertTrue(state.isClearMode)
        assertFalse(state.browsingAtTouchStart)
        state.onUserScrollStart()
        state.onLyricsTouchReleased()
        state.onUserScrollStop(1200)
        assertTrue(state.isClearMode)
    }
    @Test fun onlyEffectiveFlingChangesControlsInItsDirection() {
        assertNull(lyricsFlingControlsVisibility(0f, 50f, true, true))
        assertNull(lyricsFlingControlsVisibility(-49f, 50f, true, true))
        assertNull(lyricsFlingControlsVisibility(49f, 50f, true, true))
        assertEquals(false, lyricsFlingControlsVisibility(-200f, 50f, true, true))
        assertEquals(true, lyricsFlingControlsVisibility(200f, 50f, true, true))
    }
    @Test fun outwardBoundaryFlingAndInvalidVelocityDoNotToggleControls() {
        assertNull(lyricsFlingControlsVisibility(-500f, 50f, true, false))
        assertNull(lyricsFlingControlsVisibility(500f, 50f, false, true))
        assertNull(lyricsFlingControlsVisibility(Float.NaN, 50f, true, true))
        assertNull(lyricsFlingControlsVisibility(Float.POSITIVE_INFINITY, 50f, true, true))
    }
    @Test fun lyricPressClearsBeforeDraggingButOtherControlsDoNot() {
        val state = LyricsScrollState()
        state.onTouchDown()
        assertFalse(state.isClearMode)
        state.onLyricsTouchDown()
        assertTrue(state.isClearMode)
        assertTrue(state.isAutoFollow)
        assertFalse(state.canFollow)
        assertFalse(state.canAutoHideControls(10000, true))
        state.onLyricsTouchReleased()
        state.onTouchReleased(1000)
        assertFalse(state.isClearMode)
    }
    @Test fun releaseAfterDraggingStaysClearAndResetClearsTouchState() {
        val state = LyricsScrollState()
        state.onLyricsTouchDown()
        state.onUserScrollStart()
        state.onLyricsTouchReleased()
        state.onUserScrollStop(1000)
        assertTrue(state.isClearMode)
        state.onLyricsTouchDown()
        state.reset(2000)
        assertFalse(state.isLyricsTouching)
        assertFalse(state.isClearMode)
    }
    @Test fun manualBrowsingUsesTheSameFourSecondIdleWindow() {
        val state = LyricsScrollState()
        state.onUserScrollStart()
        state.onUserScrollStop(1000)
        state.onResumeTimerFired(4000, true)
        state.onFollowAnimationFinished()
        assertFalse(state.canAutoHideControls(4999, true))
        assertTrue(state.canAutoHideControls(5000, true))
        state.reset(1000)
        assertTrue(state.canAutoHideControls(5000, true))
    }
    @Test fun inertiaEndStartsGracePeriodNotFingerRelease() {
        val state = LyricsScrollState()
        state.onTouchDown()
        state.onUserScrollStart()
        state.onTouchReleased(1000)
        state.onResumeTimerFired(5000, true)
        assertEquals(LyricsScrollMode.ManualScrolling, state.mode)
        state.onUserScrollStop(5000)
        state.onResumeTimerFired(7999, true)
        assertEquals(LyricsScrollMode.WaitingToResume, state.mode)
    }
}
