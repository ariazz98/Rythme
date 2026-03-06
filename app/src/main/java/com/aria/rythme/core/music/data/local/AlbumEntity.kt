package com.aria.rythme.core.music.data.local

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aria.rythme.core.music.data.model.Album

@Entity(tableName = "albums")
data class AlbumEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val songCount: Int,
    val totalDuration: Long,
    val coverUri: String?,
    val year: Int
) {
    fun toAlbum(): Album = Album(
        id = id,
        title = title,
        artist = artist,
        artistId = artistId,
        songCount = songCount,
        coverUri = coverUri?.let { Uri.parse(it) },
        year = year,
        totalDuration = totalDuration
    )
}
