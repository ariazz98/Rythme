package com.aria.rythme.feature.pitch.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class PracticePlaybackBoundaryTest {
    @Test fun naturalEndKeepsTailEvenWhenPlayerHasPaused() {
        assertEquals(PracticePlaybackBoundary.Tail, practicePlaybackBoundary(true, true, true, false))
    }
    @Test fun ordinaryPauseNearEndMustNotFinishRecording() {
        assertEquals(PracticePlaybackBoundary.Paused, practicePlaybackBoundary(true, false, true, false))
    }
    @Test fun manualSongChangeNeverAttachesNewSongOrTreatsItAsTail() {
        assertEquals(PracticePlaybackBoundary.SongChanged, practicePlaybackBoundary(false, true, false, true))
    }
    @Test fun seekPausesEvenIfAudioIsStillPlaying() {
        assertEquals(PracticePlaybackBoundary.PositionChanged, practicePlaybackBoundary(true, false, false, true))
    }
    @Test fun uninterruptedPlaybackContinuesCapture() {
        assertEquals(PracticePlaybackBoundary.Continue, practicePlaybackBoundary(true, false, true, true))
    }
}
