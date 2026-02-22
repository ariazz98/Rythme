package com.aria.rythme.core.utils

import java.util.Locale

/**
 * 格式化时长
 */
fun formatPosition(position: Long): String {
    val totalSeconds = position / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
}

fun formatLeftTime(position: Long, duration: Long): String {
    if (position >= duration) {
        return "-0:00"
    }
    val leftTimeSeconds = (duration - position) / 1000
    val minutes = leftTimeSeconds / 60
    val seconds = leftTimeSeconds % 60
    return String.format(Locale.getDefault(), "-%d:%02d", minutes, seconds)
}