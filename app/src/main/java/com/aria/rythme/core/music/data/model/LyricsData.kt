package com.aria.rythme.core.music.data.model

/**
 * 歌词类型
 */
enum class LyricsType {
    /** 纯文本（无时间戳） */
    PLAIN,
    /** 标准 LRC（行级同步） */
    SYNCED,
    /** Enhanced LRC（逐字同步） */
    WORD_SYNCED
}

/**
 * 歌词来源
 */
enum class LyricsSource {
    /** Room 缓存 */
    CACHE,
    /** 内嵌标签（ID3 USLT / Vorbis Comment） */
    EMBEDDED,
    /** 本地 .lrc 文件 */
    LOCAL_FILE,
    /** LRCLIB 在线 */
    ONLINE
}

/**
 * 歌词加载状态
 */
enum class LyricsStatus {
    /** 空闲 */
    IDLE,
    /** 加载中 */
    LOADING,
    /** 加载成功 */
    LOADED,
    /** 无歌词 */
    NOT_FOUND,
    /** 加载失败 */
    ERROR
}

/**
 * 歌词数据
 *
 * @param lines 歌词行列表（按时间排序）
 * @param type 歌词类型
 * @param source 歌词来源
 * @param plainText 纯文本歌词（无时间戳时使用）
 */
data class LyricsData(
    val lines: List<LyricLine>,
    val type: LyricsType,
    val source: LyricsSource,
    val plainText: String? = null
)
