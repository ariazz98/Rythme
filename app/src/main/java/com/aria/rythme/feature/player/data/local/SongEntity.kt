package com.aria.rythme.feature.player.data.local

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aria.rythme.feature.player.data.model.Song

/**
 * 歌曲数据库实体类
 * 
 * 用于 Room 数据库存储歌曲信息，作为本地缓存。
 */
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey
    val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val album: String,
    val albumId: Long,
    val duration: Long,
    val trackNumber: Int,
    val path: String,
    val uri: String,              // Uri 转为 String 存储
    val coverUri: String?,        // Uri 转为 String 存储
    val dateAdded: Long,
    val dateModified: Long,
    val size: Long,
    val mimeType: String,
    val lastScanned: Long = System.currentTimeMillis()  // 最后扫描时间
) {
    /**
     * 转换为领域模型 Song
     */
    fun toSong(): Song = Song(
        id = id,
        title = title,
        artist = artist,
        artistId = artistId,
        album = album,
        albumId = albumId,
        duration = duration,
        trackNumber = trackNumber,
        path = path,
        uri = Uri.parse(uri),
        coverUri = coverUri?.let { Uri.parse(it) },
        dateAdded = dateAdded,
        dateModified = dateModified,
        size = size,
        mimeType = mimeType
    )
    
    companion object {
        /**
         * 从 Song 领域模型转换为 Entity
         */
        fun fromSong(song: Song): SongEntity = SongEntity(
            id = song.id,
            title = song.title,
            artist = song.artist,
            artistId = song.artistId,
            album = song.album,
            albumId = song.albumId,
            duration = song.duration,
            trackNumber = song.trackNumber,
            path = song.path,
            uri = song.uri.toString(),
            coverUri = song.coverUri?.toString(),
            dateAdded = song.dateAdded,
            dateModified = song.dateModified,
            size = song.size,
            mimeType = song.mimeType
        )
    }
}
