package com.aria.rythme.feature.pitch.presentation

import kotlin.math.roundToLong

/** 页面采样和逐帧绘制共用：正常播放平滑校时，真实 seek/切歌明确重置。 */
internal class ReferencePlaybackClock(private val onCorrection: (Long, Long) -> Unit = { _, _ -> }) {
    private var revision: Long? = null
    private var position = 0.0
    private var lastNanos = 0L
    private var lastRaw = 0L

    fun sample(rawPosition: Long, discontinuity: Long, playing: Boolean, nowNanos: Long = System.nanoTime()): Long {
        val raw = rawPosition.coerceAtLeast(0)
        val elapsedMs = ((nowNanos - lastNanos) / 1_000_000.0).coerceAtLeast(0.0)
        if (revision != discontinuity) {
            position = raw.toDouble()
            revision = discontinuity
        } else {
            if (raw < lastRaw && playing) onCorrection(lastRaw, raw)
            position = when {
                !playing || elapsedMs > 500 -> maxOf(position, raw.toDouble())
                else -> {
                    val projected = position + elapsedMs
                    // 约 200ms 内吸收播放器小幅校时；每帧速度限制在正常速度的 80–120%。
                    // 既不倒退，也不通过停住画布等待播放器追上来掩盖误差。
                    val correction = ((raw - projected) * elapsedMs / (200.0 + elapsedMs))
                        .coerceIn(-elapsedMs * .2, elapsedMs * .2)
                    // 播放器暂时不报告推进时不无限外推，等待权威时间重新跟上。
                    (projected + correction).coerceAtMost(maxOf(position, raw + 200.0))
                }
            }
        }
        lastRaw = raw
        lastNanos = nowNanos
        return position.roundToLong()
    }
}
