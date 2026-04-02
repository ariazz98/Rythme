package com.aria.rythme.core.music.data.lyrics

import com.aria.rythme.core.music.data.model.LyricsData
import com.aria.rythme.core.music.data.model.LyricsSource
import com.aria.rythme.core.music.data.model.Song

/**
 * 歌词来源接口
 *
 * 每个实现代表一种歌词获取方式（内嵌标签、本地文件、在线服务等）。
 * LyricsRepository 按优先级依次调用，获取到即停。
 */
interface LyricsProvider {
    val source: LyricsSource
    suspend fun provide(song: Song): LyricsData?
}
