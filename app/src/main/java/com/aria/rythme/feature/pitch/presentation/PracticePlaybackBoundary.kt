package com.aria.rythme.feature.pitch.presentation

internal enum class PracticePlaybackBoundary { Continue, Tail, SongChanged, PositionChanged, Paused }

/** 手动切歌优先保留本次录制；自然结束与普通暂停必须区分，只有自然结束录取尾音。 */
internal fun practicePlaybackBoundary(
    sameSong: Boolean,
    naturallyEnded: Boolean,
    samePositionRevision: Boolean,
    playing: Boolean
): PracticePlaybackBoundary = when {
    !sameSong -> PracticePlaybackBoundary.SongChanged
    naturallyEnded -> PracticePlaybackBoundary.Tail
    !samePositionRevision -> PracticePlaybackBoundary.PositionChanged
    !playing -> PracticePlaybackBoundary.Paused
    else -> PracticePlaybackBoundary.Continue
}
