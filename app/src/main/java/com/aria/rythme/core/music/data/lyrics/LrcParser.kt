package com.aria.rythme.core.music.data.lyrics

import com.aria.rythme.core.music.data.model.LyricLine
import com.aria.rythme.core.music.data.model.LyricWord
import com.aria.rythme.core.music.data.model.LyricsData
import com.aria.rythme.core.music.data.model.LyricsSource
import com.aria.rythme.core.music.data.model.LyricsType

/**
 * LRC 解析器
 *
 * 支持标准 LRC 和 Enhanced LRC（逐字同步）。
 *
 * 标准格式: `[mm:ss.xx]歌词文本`
 * Enhanced 格式: `[mm:ss.xx] <mm:ss.xx> word1 <mm:ss.xx> word2 <mm:ss.xx>`
 */
object LrcParser {

    // 匹配 [mm:ss.xx] 或 [mm:ss.xxx] 时间标签
    private val LINE_TIME_REGEX = Regex("""\[(\d{1,3}):(\d{2})[.:](\d{2,3})]""")

    // 匹配 Enhanced LRC 的逐字时间标签 <mm:ss.xx>
    private val WORD_TIME_REGEX = Regex("""<(\d{1,3}):(\d{2})[.:](\d{2,3})>""")

    /**
     * 解析 LRC 文本
     *
     * @param lrcContent LRC 文件内容
     * @param source 歌词来源
     * @return 解析后的 LyricsData，如果无法解析则返回 null
     */
    fun parse(lrcContent: String, source: LyricsSource): LyricsData? {
        if (lrcContent.isBlank()) return null

        val lines = mutableListOf<LyricLine>()
        var hasWordSync = false

        for (rawLine in lrcContent.lines()) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue

            // 提取所有行级时间标签（支持同一行多个时间标签）
            val timeMatches = LINE_TIME_REGEX.findAll(trimmed).toList()
            if (timeMatches.isEmpty()) continue

            // 获取时间标签后的文本内容
            val lastTagEnd = timeMatches.last().range.last + 1
            val textContent = trimmed.substring(lastTagEnd).trim()

            // 跳过空文本行
            if (textContent.isEmpty()) continue

            // 检测是否有 Enhanced LRC 逐字标签
            val wordTimeMatches = WORD_TIME_REGEX.findAll(textContent).toList()
            val words = if (wordTimeMatches.isNotEmpty()) {
                hasWordSync = true
                parseEnhancedWords(textContent, wordTimeMatches)
            } else {
                null
            }

            val displayText = words?.joinToString("") { it.text } ?: textContent

            // 为每个时间标签生成一行
            for (match in timeMatches) {
                val timeMs = parseTimeTag(match)
                lines.add(LyricLine(startTimeMs = timeMs, text = displayText, words = words))
            }
        }

        if (lines.isEmpty()) return null

        // 按时间排序
        lines.sortBy { it.startTimeMs }

        val type = when {
            hasWordSync -> LyricsType.WORD_SYNCED
            else -> LyricsType.SYNCED
        }

        return LyricsData(lines = lines, type = type, source = source)
    }

    /**
     * 解析时间标签为毫秒
     */
    private fun parseTimeTag(match: MatchResult): Long {
        val minutes = match.groupValues[1].toLong()
        val seconds = match.groupValues[2].toLong()
        val fraction = match.groupValues[3]
        val millis = if (fraction.length == 2) {
            fraction.toLong() * 10 // xx → xxx
        } else {
            fraction.toLong()
        }
        return minutes * 60_000 + seconds * 1_000 + millis
    }

    /**
     * 解析 Enhanced LRC 逐字时间
     *
     * 格式: `<00:01.00> Hello <00:01.50> World <00:02.00>`
     */
    private fun parseEnhancedWords(
        textContent: String,
        wordTimeMatches: List<MatchResult>
    ): List<LyricWord> {
        val words = mutableListOf<LyricWord>()

        for (i in wordTimeMatches.indices) {
            val currentMatch = wordTimeMatches[i]
            val startMs = parseWordTimeTag(currentMatch)

            // 提取当前时间标签之后到下一个时间标签之前的文本
            val textStart = currentMatch.range.last + 1
            val textEnd = if (i + 1 < wordTimeMatches.size) {
                wordTimeMatches[i + 1].range.first
            } else {
                textContent.length
            }

            val wordText = textContent.substring(textStart, textEnd).trim()
            if (wordText.isEmpty()) continue

            // 结束时间为下一个标签的时间，最后一个词没有明确结束时间则用 startMs
            val endMs = if (i + 1 < wordTimeMatches.size) {
                parseWordTimeTag(wordTimeMatches[i + 1])
            } else {
                startMs
            }

            words.add(LyricWord(startTimeMs = startMs, endTimeMs = endMs, text = wordText))
        }

        return words
    }

    /**
     * 解析逐字时间标签
     */
    private fun parseWordTimeTag(match: MatchResult): Long {
        val minutes = match.groupValues[1].toLong()
        val seconds = match.groupValues[2].toLong()
        val fraction = match.groupValues[3]
        val millis = if (fraction.length == 2) {
            fraction.toLong() * 10
        } else {
            fraction.toLong()
        }
        return minutes * 60_000 + seconds * 1_000 + millis
    }
}
