package com.aria.rythme.core.music.data.model

/**
 * 歌词行
 *
 * @param startTimeMs 行开始时间（毫秒）
 * @param text 歌词文本
 * @param words 逐字时间（Enhanced LRC），可选
 */
data class LyricLine(
    val startTimeMs: Long,
    val text: String,
    val words: List<LyricWord>? = null
)

/**
 * 逐字歌词（Enhanced LRC）
 *
 * @param startTimeMs 单词开始时间（毫秒）
 * @param endTimeMs 单词结束时间（毫秒）
 * @param text 单词文本
 */
data class LyricWord(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
)
