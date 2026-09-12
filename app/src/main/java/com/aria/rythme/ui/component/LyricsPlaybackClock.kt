package com.aria.rythme.ui.component

import kotlin.math.abs
import kotlin.math.exp

/** 仅服务歌词展示：帧时钟持续推进，媒体采样校准速度，不周期性重置动画。 */
internal class LyricsPlaybackClock {
    private var initialized = false
    private var playing = false
    private var mediaPositionMs = 0.0
    private var sampledAtNs = 0L
    private var previousFrameNs = 0L
    private var displayPositionMs = 0.0
    private var rate = 1.0
    private var revision = 0L

    fun sample(positionMs: Long, isPlaying: Boolean, discontinuity: Long, nowNs: Long): Long {
        val position = positionMs.coerceAtLeast(0L).toDouble()
        val predicted = if (initialized && playing) displayPositionMs +
            ((nowNs - previousFrameNs).coerceAtLeast(0L) / NANOS_PER_MS) * rate else displayPositionMs
        val snap = !initialized || !isPlaying || playing != isPlaying || revision != discontinuity ||
            abs(position - predicted) > DISCONTINUITY_THRESHOLD_MS
        mediaPositionMs = position
        sampledAtNs = nowNs
        playing = isPlaying
        revision = discontinuity
        if (snap) {
            displayPositionMs = position
            previousFrameNs = nowNs
            rate = 1.0
        }
        initialized = true
        return displayPositionMs.toLong()
    }

    fun frame(frameNs: Long): Long {
        if (!initialized || !playing || frameNs <= previousFrameNs) return displayPositionMs.toLong()
        val elapsedMs = (frameNs - previousFrameNs) / NANOS_PER_MS
        val target = mediaPositionMs + (frameNs - sampledAtNs).coerceAtLeast(0L) / NANOS_PER_MS
        val error = target - (displayPositionMs + elapsedMs)
        val desiredRate = (1.0 + error / CORRECTION_WINDOW_MS).coerceIn(.9, 1.1)
        // 连速度也平滑变化，避免每个新采样让高亮边界突然加速／减速。
        rate += (desiredRate - rate) * (1.0 - exp(-elapsedMs / RATE_SMOOTHING_MS))
        displayPositionMs += elapsedMs * rate
        previousFrameNs = frameNs
        return displayPositionMs.toLong()
    }

    private companion object {
        const val NANOS_PER_MS = 1_000_000.0
        const val CORRECTION_WINDOW_MS = 240.0
        const val RATE_SMOOTHING_MS = 80.0
        const val DISCONTINUITY_THRESHOLD_MS = 500.0
    }
}
