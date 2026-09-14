package com.aria.rythme.feature.pitch.presentation

import com.aria.rythme.feature.pitch.data.PitchFrame

/** AudioRecord 可能成批送达多个 hop；以采样时间推进，不能用同一播放器读数压掉整批检测点。 */
internal class PracticeCaptureClock {
    private var offset: Long? = null
    fun map(frame: PitchFrame, playbackPositionMs: Long): PitchFrame {
        val anchor = offset ?: (playbackPositionMs - frame.capturedUntilMs).also { offset = it }
        return frame.copy(timeMs = (anchor + frame.timeMs).coerceAtLeast(0),
            capturedUntilMs = (anchor + frame.capturedUntilMs).coerceAtLeast(0))
    }
}
