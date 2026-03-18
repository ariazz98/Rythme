package com.aria.rythme.core.music.data.local

import androidx.core.net.toUri
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aria.rythme.core.music.data.model.Playlist

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val coverUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun toPlaylist(songCount: Int = 0): Playlist = Playlist(
        id = id,
        name = name,
        description = description,
        songCount = songCount,
        coverUri = coverUri?.toUri(),
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
