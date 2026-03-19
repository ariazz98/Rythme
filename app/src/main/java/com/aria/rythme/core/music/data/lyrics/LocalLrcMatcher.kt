package com.aria.rythme.core.music.data.lyrics

import com.aria.rythme.core.music.data.model.LyricsData
import com.aria.rythme.core.music.data.model.LyricsSource
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.utils.RythmeLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 本地 .lrc 文件匹配
 *
 * 匹配策略（按优先级）：
 * 1. 同目录同名 .lrc 文件
 * 2. 同目录 "标题 - 艺术家.lrc"
 * 3. 同目录 "艺术家 - 标题.lrc"
 */
object LocalLrcMatcher {

    private const val TAG = "LocalLrcMatcher"

    /**
     * 查找并解析本地 .lrc 文件
     */
    suspend fun find(song: Song): LyricsData? = withContext(Dispatchers.IO) {
        val audioFile = File(song.path)
        val dir = audioFile.parentFile ?: return@withContext null
        val baseName = audioFile.nameWithoutExtension

        // 候选文件名列表
        val candidates = listOf(
            "$baseName.lrc",
            "${song.title} - ${song.artist}.lrc",
            "${song.artist} - ${song.title}.lrc"
        )

        for (candidate in candidates) {
            val lrcFile = File(dir, candidate)
            if (lrcFile.exists() && lrcFile.canRead()) {
                try {
                    val content = lrcFile.readText(Charsets.UTF_8)
                    val parsed = LrcParser.parse(content, LyricsSource.LOCAL_FILE)
                    if (parsed != null) {
                        RythmeLogger.d(TAG, "找到本地歌词: ${lrcFile.name}")
                        return@withContext parsed
                    }
                } catch (e: Exception) {
                    RythmeLogger.e(TAG, "读取本地 .lrc 失败: ${lrcFile.path}", e)
                }
            }
        }
        null
    }
}
