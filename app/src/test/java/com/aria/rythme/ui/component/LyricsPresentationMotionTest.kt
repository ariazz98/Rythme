package com.aria.rythme.ui.component

import com.aria.rythme.core.music.data.model.LyricLine
import com.aria.rythme.core.music.data.model.LyricWord
import com.aria.rythme.core.music.data.model.LyricsSource
import com.aria.rythme.core.music.data.model.LyricsType
import com.aria.rythme.core.music.data.lyrics.LrcParser
import org.junit.Assert.*
import org.junit.Test

class LyricsPresentationMotionTest {
    @Test fun currentLineKeepsFullTextSizeAndOtherLinesAreOnlyTwoPercentSmaller() {
        val motion = LyricsPresentationMotion
        assertEquals(.98f, motion.textScale(0f), 0f)
        assertEquals(.99f, motion.textScale(.5f), .00001f)
        assertEquals(1f, motion.textScale(1f), 0f)
        assertEquals(.98f, motion.textScale(-1f), 0f)
        assertEquals(1f, motion.textScale(2f), 0f)
        for (step in 0..100) assertTrue(motion.textScale(step / 100f) in .98f..1f)
    }
    @Test fun deviceFixtureContainsRealSegmentTimesAndAnOrdinaryLineFallback() {
        val content = javaClass.getResource("/lyrics/presentation.lrc")!!.readText()
        val data = LrcParser.parse(content, LyricsSource.EMBEDDED)!!
        assertEquals(LyricsType.WORD_SYNCED, data.type)
        assertEquals(7, data.lines.size)
        assertEquals(2000L, data.lines.first().words!!.first().startTimeMs)
        assertTrue(data.lines.first().words!!.all { it.endTimeMs > it.startTimeMs })
        assertNull(data.lines[4].words)
    }
    @Test fun upperRowsMoveBeforeLowerRowsWithoutOvershoot() {
        val motion = LyricsPresentationMotion
        assertTrue(motion.rowProgress(160f, -1) > motion.rowProgress(160f, 0))
        assertTrue(motion.rowProgress(160f, 0) > motion.rowProgress(160f, 2))
        for (time in 0..motion.TotalDurationMs step 10) {
            for (row in -2..10) assertTrue(motion.rowProgress(time.toFloat(), row) in 0f..1f)
        }
        for (row in -2..10) assertEquals(1f, motion.rowProgress(motion.TotalDurationMs.toFloat(), row), 0f)
    }
    @Test fun offsetsEndAtZeroAndCanRemainFrozenForInterruptionRelease() {
        val wave = LyricsLineWave()
        wave.targetItem = 5
        wave.distance = 180f
        wave.consumed = 180f
        wave.elapsed = LyricsPresentationMotion.TotalDurationMs.toFloat()
        for (index in 3..12) assertEquals(0f, wave.offset(index), .001f)
        wave.elapsed = 180f
        val offset = wave.offset(7)
        wave.running = false
        assertEquals(offset, wave.offset(7), 0f)
    }
    @Test fun futureBrightnessIsMonotonicAndBrowsingRemovesBlur() {
        val motion = LyricsPresentationMotion
        assertTrue(motion.alpha(1) >= motion.alpha(2))
        assertTrue(motion.alpha(2) >= motion.alpha(3))
        assertEquals(1f, motion.alpha(0), 0f)
        for (row in -4..8) {
            if (row != 0) assertTrue(motion.alpha(row) < motion.alpha(0))
            assertEquals(0f, motion.blurDp(row, true), 0f)
        }
    }
    @Test fun wordProgressHonorsTimestampsAndZeroDurationNeverInventsTiming() {
        val word = LyricWord(1000, 2000, "你好")
        assertEquals(0f, lyricWordProgress(word, 999), 0f)
        assertEquals(.5f, lyricWordProgress(word, 1500), 0f)
        assertEquals(1f, lyricWordProgress(word, 3000), 0f)
        assertEquals(1f, lyricWordProgress(word.copy(endTimeMs = 1000), 1000), 0f)
    }
    @Test fun currentUnsungLettersStayBrighterThanEveryInactiveLine() {
        for (row in -20..20) {
            if (row != 0) assertTrue(LyricsPresentationMotion.alpha(row) < LyricsPresentationMotion.UnsungAlpha)
        }
        assertTrue(LyricsPresentationMotion.UnsungAlpha < LyricsPresentationMotion.alpha(0))
    }
    @Test fun distantLinesFadeDuringFollowButBecomeReadableWhileBrowsing() {
        val motion = LyricsPresentationMotion
        val levels = listOf(.30f, .24f, .16f, .09f, .04f, .02f)
        levels.forEachIndexed { index, alpha -> assertEquals(alpha, motion.alpha(index + 1), 0f) }
        for (row in 1..20) {
            assertTrue(motion.alpha(row) >= motion.alpha(row + 1))
            assertTrue(motion.alpha(row, true) >= .18f)
            assertTrue(motion.alpha(row, true) >= motion.alpha(row))
            assertTrue(motion.alpha(row, true) < motion.UnsungAlpha)
        }
        assertEquals(motion.alpha(0), motion.alpha(0, true), 0f)
        assertEquals(motion.alpha(-1), motion.alpha(-1, true), 0f)
    }
    @Test fun defocusIncreasesForUpcomingLinesAndPressRemovesOnlyBlur() {
        val motion = LyricsPresentationMotion
        assertEquals(0f, motion.blurDp(0, false), 0f)
        assertEquals(6.3f, motion.blurDp(-1, false), 0f)
        assertEquals(4.8f, motion.blurDp(1, false), 0f)
        assertEquals(5.8f, motion.blurDp(2, false), 0f)
        assertEquals(7.8f, motion.blurDp(3, false), .0001f)
        assertEquals(8.8f, motion.blurDp(4, false), .0001f)
        assertEquals(9f, motion.blurDp(5, false), 0f)
        assertTrue(motion.blurDp(-1, false) > motion.blurDp(2, false))
        assertTrue(motion.blurDp(-1, false) < motion.blurDp(3, false))
        for (row in 1..20) {
            assertTrue(motion.blurDp(row, false) <= motion.blurDp(row + 1, false))
            assertTrue(motion.blurDp(row, false) <= 9f)
            assertEquals(0f, motion.blurDp(row, true), 0f)
        }
    }
    @Test fun highlightUsesMeasuredAdvancesWithinATimedSegment() {
        assertEquals(1f, lyricGlyphFill(.5f, 0f, 20f, 40f), 0f)
        assertEquals(0f, lyricGlyphFill(.5f, 20f, 20f, 40f), 0f)
        assertEquals(.5f, lyricGlyphFill(.75f, 20f, 20f, 40f), 0f)
    }
    @Test fun repeatedTextWhitespaceAndCombiningMarksRetainTheirRanges() {
        val line = LyricLine(0, "你 你e\u0301", listOf(
            LyricWord(0, 1000, "你"), LyricWord(1000, 2000, "你e\u0301")))
        val glyphs = timedLyricGlyphs(line)
        assertEquals(listOf(0, null, 1, 1), glyphs.map { it.wordIndex })
        assertEquals("e\u0301", line.text.substring(glyphs.last().start, glyphs.last().end))
    }
    @Test fun missingOrMismatchedWordTimingFallsBackToTheOriginalLine() {
        assertTrue(timedLyricGlyphs(LyricLine(0, "普通歌词")).isEmpty())
        assertTrue(timedLyricGlyphs(LyricLine(0, "普通歌词", listOf(LyricWord(0, 1000, "不匹配")))).isEmpty())
        assertTrue(timedLyricGlyphs(LyricLine(0, "歌词", listOf(LyricWord(2000, 1000, "歌词")))).isEmpty())
    }
}
