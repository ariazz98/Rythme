package com.aria.rythme.feature.pitch.presentation

/** 检测以 32ms 为步长，显示使用 vsync；两帧缓冲只插值时间，不预测音高。 */
internal class PitchRenderClock(private val bufferMs: Double = 64.0) {
    private var anchorNanos = 0L
    private var anchorMs = 0L
    private var latestMs = -1L
    private var previousDisplayMs = 0.0

    fun accept(sampleMs: Long, receivedNanos: Long) {
        if (sampleMs == latestMs) return
        if (latestMs < 0 || sampleMs < latestMs) {
            anchorNanos = receivedNanos
            anchorMs = sampleMs
            previousDisplayMs = (sampleMs - bufferMs).coerceAtLeast(0.0)
        }
        latestMs = sampleMs
    }

    fun timeMs(frameNanos: Long): Double {
        if (latestMs < 0) return 0.0
        val elapsed = (frameNanos - anchorNanos).coerceAtLeast(0L) / 1_000_000.0
        val desired = anchorMs + elapsed - bufferMs
        previousDisplayMs = desired.coerceIn(previousDisplayMs, maxOf(previousDisplayMs, latestMs.toDouble()))
        return previousDisplayMs
    }
}
