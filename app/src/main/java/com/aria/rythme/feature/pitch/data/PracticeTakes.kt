package com.aria.rythme.feature.pitch.data

/** 每次跳转开始新片段；各片段时间独立递增，旧片段保留原歌曲时间坐标。 */
data class PracticeTakes(
    val older: List<PitchHistory> = emptyList(),
    val current: PitchHistory = PitchHistory.Empty
) {
    fun beginSegment(): PracticeTakes = if (current.isEmpty()) this else PracticeTakes(older + listOf(current))
    fun append(frame: PitchFrame): PracticeTakes = if (current.lastOrNull()?.timeMs?.let { frame.timeMs <= it } == true) this
        else copy(current = current.append(frame))
}
