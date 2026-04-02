package com.aria.rythme.core.music.data.repository

import com.aria.rythme.core.music.data.local.LyricsDao
import com.aria.rythme.core.music.data.local.LyricsEntity
import com.aria.rythme.core.music.data.lyrics.LrcParser
import com.aria.rythme.core.music.data.lyrics.LyricsProvider
import com.aria.rythme.core.music.data.model.LyricsData
import com.aria.rythme.core.music.data.model.LyricsSource
import com.aria.rythme.core.music.data.model.LyricsType
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.utils.RythmeLogger

/**
 * 歌词仓库
 *
 * 统一调度歌词获取逻辑：
 * Room 缓存 → 按优先级遍历 LyricsProvider 列表
 *
 * 获取到即停，结果缓存到 Room。"无歌词"不缓存（下次重试）。
 */
class LyricsRepository(
    private val lyricsDao: LyricsDao,
    private val providers: List<LyricsProvider>
) {

    /**
     * 获取歌词
     */
    suspend fun getLyrics(song: Song): LyricsData? {
        // 清理过期在线缓存（30 天 TTL）
        lyricsDao.deleteExpired(System.currentTimeMillis() - TTL_ONLINE_MS)

        // 1. Room 缓存
        val cached = loadFromCache(song.id)
        if (cached != null) {
            RythmeLogger.d(TAG, "缓存命中: ${song.title}")
            return cached
        }

        // 2. 按优先级遍历 Provider
        for (provider in providers) {
            val data = provider.provide(song)
            if (data != null) {
                RythmeLogger.d(TAG, "${provider.source} 歌词: ${song.title}")
                saveToCache(song.id, data)
                return data
            }
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
        val type = try {
            LyricsType.valueOf(entity.type)
        } catch (e: Exception) {
            null
        }

        // 纯文本歌词直接返回
        if (type == LyricsType.PLAIN) {
            return LyricsData(
                lines = emptyList(),
                type = LyricsType.PLAIN,
                source = source,
                plainText = entity.rawContent,
                rawContent = entity.rawContent
            )
        }

        // 同步歌词重新解析
        return LrcParser.parse(entity.rawContent, source)
    }

    private suspend fun saveToCache(songId: Long, data: LyricsData) {
        val content = data.rawContent ?: data.plainText ?: return
        lyricsDao.insert(
            LyricsEntity(
                songId = songId,
                rawContent = content,
                source = data.source.name,
                type = data.type.name,
                cachedAt = System.currentTimeMillis()
            )
        )
    }

    companion object {
        private const val TAG = "LyricsRepository"
        private const val TTL_ONLINE_MS = 30L * 24 * 60 * 60 * 1000 // 30 天
    }
}
