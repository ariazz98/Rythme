package com.aria.rythme.feature.player.data.model

import android.net.Uri

/**
 * 艺术家数据类
 *
 * 代表一位音乐艺术家的信息。
 *
 * @param id 艺术家唯一标识
 * @param name 艺术家名称
 * @param albumCount 专辑数量
 * @param songCount 歌曲数量
 * @param coverUri 艺术家封面URI（通常使用最新专辑封面）
 */
data class Artist(
    val id: Long,
    val name: String,
    val albumCount: Int = 0,
    val songCount: Int = 0,
    val coverUri: Uri? = null
) {
    /**
     * 获取艺术家描述文本
     */
    val description: String
        get() = "$albumCount 张专辑 · $songCount 首歌曲"
}
