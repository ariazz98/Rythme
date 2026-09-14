package com.aria.rythme.feature.pitch.data

import kotlin.math.*

data class DetectedPitch(val frequencyHz: Float, val confidence: Float) {
    val midiNote: Float get() = (69 + 12 * log2(frequencyHz / 440.0)).toFloat()
    val nearestMidi: Int get() = midiNote.roundToInt()
    val cents: Int get() = ((midiNote - nearestMidi) * 100).roundToInt()
    val noteName: String get() = "${NAMES[Math.floorMod(nearestMidi, 12)]}${Math.floorDiv(nearestMidi, 12) - 1}"
    companion object { private val NAMES = arrayOf("C", "C♯", "D", "D♯", "E", "F", "F♯", "G", "G♯", "A", "A♯", "B") }
}

/** 单声部 YIN 基频估计；静音/非周期声音不输出上一帧的音高。无需模型或网络。 */
class PitchDetector(private val sampleRate: Int = 16_000, private val frameSize: Int = 2048) {
    private val maximumLag = min(ceil(sampleRate / 55.0).toInt() + 2, frameSize / 2 - 1)
    private val difference = DoubleArray(maximumLag + 1)

    fun detect(samples: FloatArray): DetectedPitch? {
        require(samples.size == frameSize)
        var energy = 0.0
        var mean = 0.0
        for (sample in samples) {
            if (!sample.isFinite()) return null
            mean += sample; energy += sample * sample
        }
        mean /= samples.size
        val variance = energy / samples.size - mean * mean
        // 原始麦克风可能增益较低；幅度只排除极弱输入，可靠性继续由 YIN 周期性判断。
        if (variance < 0.0003 * 0.0003) return null
        val window = frameSize - maximumLag
        difference[0] = 1.0
        var cumulative = 0.0
        for (lag in 1..maximumLag) {
            var sum = 0.0
            for (i in 0 until window) {
                val delta = (samples[i] - samples[i + lag]).toDouble()
                sum += delta * delta
            }
            cumulative += sum
            difference[lag] = if (cumulative > 0) sum * lag / cumulative else 1.0
        }
        var lag = 2
        while (lag < maximumLag) {
            if (difference[lag] < 0.15) {
                while (lag < maximumLag && difference[lag + 1] < difference[lag]) lag++
                if (lag == maximumLag) return null
                val left = difference[lag - 1]; val middle = difference[lag]; val right = difference[lag + 1]
                val denominator = left - 2 * middle + right
                val adjustment = if (abs(denominator) > 1e-12) ((left - right) / (2 * denominator)).coerceIn(-1.0, 1.0) else 0.0
                val hz = (sampleRate / (lag + adjustment)).toFloat()
                return if (hz.isFinite() && hz in 55f..1000f) DetectedPitch(hz, (1 - middle).toFloat().coerceIn(0f, 1f)) else null
            }
            lag++
        }
        return null
    }
}
