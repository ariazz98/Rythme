package com.aria.rythme.core.music.data.model

import android.net.Uri

/**
 * 播放列表数据类
 *
 * 代表用户创建的播放列表。
 *
 * @param id 播放列表唯一标识
 * @param name 播放列表名称
 * @param description 播放列表描述
 * @param songCount 歌曲数量
 * @param coverUri 播放列表封面URI（通常使用列表中第一首歌的封面）
 * @param createdAt 创建时间戳
 * @param updatedAt 更新时间戳
 * @param songIds 歌曲ID列表
 */
data class Playlist(
    val id: Long,
    val name: String,
    val description: String = "",
    val songCount: Int = 0,
    val coverUri: Uri? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val songIds: List<Long> = emptyList()
) {
    /**
     * 获取播放列表描述文本
     */
    val displayDescription: String
        get() = description.ifEmpty {
            "$songCount 首歌曲"
        }
}
