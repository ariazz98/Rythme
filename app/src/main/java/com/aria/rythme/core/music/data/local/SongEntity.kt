package com.aria.rythme.core.music.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.aria.rythme.core.music.data.model.Song
import androidx.core.net.toUri

@Entity(
    tableName = "songs",
    indices = [
        Index("albumId"),
        Index("artistId"),
        Index("genre"),
        Index("composer"),
        Index("folderId"),
        Index("dateAdded"),
        Index("year")
    ]
)
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
    val uri: String,
    val coverUri: String?,
    val dateAdded: Long,
    val dateModified: Long,
    val size: Long,
    val mimeType: String,
    val genre: String = "",
    val composer: String = "",
    val bitrate: Int = 0,
    val year: Int = 0,
    val discNumber: Int = 0,
    val albumArtist: String = "",
    val folderName: String = "",
    val folderId: Long = 0,
    val generationAdded: Long = 0,
    val generationModified: Long = 0,
    val lastScanned: Long = System.currentTimeMillis()
) {
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
        uri = uri.toUri(),
        coverUri = coverUri?.toUri(),
        dateAdded = dateAdded,
        dateModified = dateModified,
        size = size,
        mimeType = mimeType,
        genre = genre,
        composer = composer,
        bitrate = bitrate,
        year = year,
        discNumber = discNumber,
        albumArtist = albumArtist,
        folderName = folderName,
        folderId = folderId,
        generationAdded = generationAdded,
        generationModified = generationModified
    )

    companion object {
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
            mimeType = song.mimeType,
            genre = song.genre,
            composer = song.composer,
            bitrate = song.bitrate,
            year = song.year,
            discNumber = song.discNumber,
            albumArtist = song.albumArtist,
            folderName = song.folderName,
            folderId = song.folderId,
            generationAdded = song.generationAdded,
            generationModified = song.generationModified
        )
    }
}
