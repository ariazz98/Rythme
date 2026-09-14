package com.aria.rythme.feature.pitch.presentation

/** MIDI 半音坐标；视口移动不会修改检测结果或压缩轨迹的音程。 */
internal fun clampPitchTop(top: Float, visibleSemitones: Float): Float =
    top.coerceIn(visibleSemitones.coerceIn(0f, 127f), 127f)

/** 把最近轨迹放进无悬浮控件的区域；跨度放不下时至少保留最新音高。 */
internal fun followPitchTop(
    currentTop: Float,
    recentNotes: List<Float>,
    semitonePx: Float,
    safeTopPx: Float,
    safeBottomPx: Float,
    visibleSemitones: Float
): Float {
    val notes = recentNotes.filter { it.isFinite() }
    if (notes.isEmpty() || semitonePx <= 0f || safeBottomPx <= safeTopPx) return currentTop
    var lower = notes.max() + safeTopPx / semitonePx
    var upper = notes.min() + safeBottomPx / semitonePx
    if (lower > upper) {
        lower = notes.last() + safeTopPx / semitonePx
        upper = notes.last() + safeBottomPx / semitonePx
    }
    return clampPitchTop(currentTop.coerceIn(lower, upper), visibleSemitones)
}

/** 录制继续进行时，历史窗口的绝对时间保持不变，直到用户返回实时。 */
internal fun clampHistoryEnd(requestedMs: Double, durationMs: Double): Double =
    requestedMs.coerceIn(minOf(10_000.0, durationMs), durationMs)

/** 首屏从零时刻铺开，填满十秒后才移动窗口；游标与轨迹使用同一时间原点。 */
internal fun pitchWindowStart(endMs: Double): Double = (endMs - 10_000.0).coerceAtLeast(0.0)
