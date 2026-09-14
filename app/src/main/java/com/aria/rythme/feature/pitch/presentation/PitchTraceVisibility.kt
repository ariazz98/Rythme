package com.aria.rythme.feature.pitch.presentation

import com.aria.rythme.feature.pitch.data.PitchFrame

/** moveTo 不产生笔迹；没有可连接邻居的有效采样必须单独绘制。 */
internal fun isolatedPitchIndices(frames: List<PitchFrame>): List<Int> = frames.indices.filter { index ->
    val frame = frames[index]
    if (frame.pitch == null) false else {
        val before = frames.getOrNull(index - 1)
        val after = frames.getOrNull(index + 1)
        val connectedBefore = before?.pitch != null && frame.timeMs - before.timeMs in 1..96
        val connectedAfter = after?.pitch != null && after.timeMs - frame.timeMs in 1..96
        !connectedBefore && !connectedAfter
    }
}

/** 跟唱游标使用已经采集的真实音高，不等待歌曲当前位置之后的未来采样。 */
internal fun latestVisiblePitch(frames: List<PitchFrame>, positionMs: Long): PitchFrame? =
    frames.lastOrNull { it.timeMs <= positionMs }?.takeIf { it.pitch != null && positionMs - it.timeMs <= 200 }
