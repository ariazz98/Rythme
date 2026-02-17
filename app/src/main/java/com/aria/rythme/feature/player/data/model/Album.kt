package com.aria.rythme.feature.player.data.model

import android.net.Uri

/**
 * 专辑数据类
 *
 * 代表一张音乐专辑的信息。
 *
 * @param id 专辑唯一标识
 * @param title 专辑名称
 * @param artist 艺术家名称
 * @param artistId 艺术家ID
 * @param songCount 歌曲数量
 * @param coverUri 专辑封面URI
 * @param year 发行年份
 */
data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long = 0,
    val songCount: Int = 0,
    val coverUri: Uri? = null,
    val year: Int = 0
) {
    /**
     * 获取专辑描述文本
     */
    val description: String
        get() = "$artist · $songCount 首歌曲"
}
