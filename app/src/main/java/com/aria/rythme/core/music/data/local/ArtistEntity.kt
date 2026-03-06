package com.aria.rythme.core.music.data.local

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aria.rythme.core.music.data.model.Artist

@Entity(tableName = "artists")
data class ArtistEntity(
    @PrimaryKey val id: Long,
    val name: String,
    val albumCount: Int,
    val songCount: Int,
    val coverUri: String?
) {
    fun toArtist(): Artist = Artist(
        id = id,
        name = name,
        albumCount = albumCount,
        songCount = songCount,
        coverUri = coverUri?.let { Uri.parse(it) }
    )
}
