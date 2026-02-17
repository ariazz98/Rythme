package com.aria.rythme.feature.player.data.model

import android.net.Uri

/**
 * 歌曲数据类
 *
 * 代表一首音乐的所有元数据信息。
 *
 * @param id 歌曲唯一标识（MediaStore ID）
 * @param title 歌曲标题
 * @param artist 艺术家名称
 * @param artistId 艺术家ID
 * @param album 专辑名称
 * @param albumId 专辑ID
 * @param duration 歌曲时长（毫秒）
 * @param trackNumber 曲目编号
 * @param path 文件路径
 * @param uri 内容URI
 * @param coverUri 专辑封面URI
 * @param dateAdded 添加时间戳
 * @param dateModified 修改时间戳
 * @param size 文件大小（字节）
 * @param mimeType MIME类型
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long = 0,
    val album: String,
    val albumId: Long = 0,
    val duration: Long,
    val trackNumber: Int = 0,
    val path: String,
    val uri: Uri,
    val coverUri: Uri? = null,
    val dateAdded: Long = 0,
    val dateModified: Long = 0,
    val size: Long = 0,
    val mimeType: String = ""
) {
    /**
     * 获取格式化的时长字符串
     * 格式：mm:ss 或 h:mm:ss
     */
    val durationText: String
        get() {
            val totalSeconds = duration / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            
            return if (hours > 0) {
                String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
    
    /**
     * 获取艺术家-标题组合文本
     */
    val displaySubtitle: String
        get() = "$artist - $album"
    
    companion object {
        /**
         * 创建空的Song对象（用于占位）
         */
        fun empty(): Song = Song(
            id = -1,
            title = "未知歌曲",
            artist = "未知艺术家",
            album = "未知专辑",
            duration = 0,
            path = "",
            uri = Uri.EMPTY
        )
    }
}
