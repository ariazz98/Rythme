package com.aria.rythme.core.music.data.repository

import androidx.media3.common.util.UnstableApi
import com.aria.rythme.core.music.data.local.LyricsDao
import com.aria.rythme.core.music.data.local.LyricsEntity
import com.aria.rythme.core.music.data.lyrics.EmbeddedLyricsReader
import com.aria.rythme.core.music.data.lyrics.LocalLrcMatcher
import com.aria.rythme.core.music.data.lyrics.LrcParser
import com.aria.rythme.core.music.data.lyrics.LrclibApi
import com.aria.rythme.core.music.data.model.LyricsData
import com.aria.rythme.core.music.data.model.LyricsSource
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.utils.RythmeLogger

/**
 * 歌词仓库
 *
 * 统一调度歌词获取逻辑：
 * Room 缓存 → 内嵌标签 → 本地 .lrc → LRCLIB 在线
 *
 * 获取到即停，结果缓存到 Room。"无歌词"不缓存（下次重试）。
 */
class LyricsRepository(
    private val lyricsDao: LyricsDao,
    private val embeddedReader: EmbeddedLyricsReader
) {

    /**
     * 获取歌词
     *
     * @param song 歌曲
     * @return LyricsData 或 null
     */
    suspend fun getLyrics(song: Song): LyricsData? {
        // 1. Room 缓存
        val cached = loadFromCache(song.id)
        if (cached != null) {
            RythmeLogger.d(TAG, "缓存命中: ${song.title}")
            return cached
        }

        // 2. 内嵌标签
        val embedded = embeddedReader.read(song.uri)
        if (embedded != null) {
            RythmeLogger.d(TAG, "内嵌歌词: ${song.title}")
            saveToCache(song.id, embedded)
            return embedded
        }

        // 3. 本地 .lrc 文件
        val local = LocalLrcMatcher.find(song)
        if (local != null) {
            RythmeLogger.d(TAG, "本地歌词: ${song.title}")
            saveToCache(song.id, local)
            return local
        }

        // 4. LRCLIB 在线
        val online = LrclibApi.fetch(song)
        if (online != null) {
            RythmeLogger.d(TAG, "在线歌词: ${song.title}")
            saveToCache(song.id, online)
            return online
        }

        RythmeLogger.d(TAG, "无歌词: ${song.title}")
        return null
    }

    private suspend fun loadFromCache(songId: Long): LyricsData? {
        val entity = lyricsDao.getBySongId(songId) ?: return null
        val source = try {
            LyricsSource.valueOf(entity.source)
        } catch (e: Exception) {
            LyricsSource.CACHE
        }
        return LrcParser.parse(entity.lrcContent, source)
    }

    private suspend fun saveToCache(songId: Long, data: LyricsData) {
        // 只缓存有同步歌词的
        if (data.lines.isEmpty()) return

        val lrcContent = buildLrcContent(data)
        lyricsDao.insert(
            LyricsEntity(
                songId = songId,
                lrcContent = lrcContent,
                source = data.source.name,
                type = data.type.name
            )
        )
    }

    /**
     * 将 LyricsData 还原为 LRC 文本用于缓存
     */
    private fun buildLrcContent(data: LyricsData): String {
        return data.lines.joinToString("\n") { line ->
            val min = line.startTimeMs / 60_000
            val sec = (line.startTimeMs % 60_000) / 1_000
            val ms = (line.startTimeMs % 1_000) / 10
            "[%02d:%02d.%02d]%s".format(min, sec, ms, line.text)
        }
    }

    companion object {
        private const val TAG = "LyricsRepository"
    }
}
