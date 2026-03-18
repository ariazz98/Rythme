package com.aria.rythme.core.music.data.repository

import com.aria.rythme.core.music.data.local.PlaylistDao
import com.aria.rythme.core.music.data.local.PlaylistEntity
import com.aria.rythme.core.music.data.local.PlaylistSongEntity
import com.aria.rythme.core.music.data.local.SongDao
import com.aria.rythme.core.music.data.local.SongOverrideDao
import com.aria.rythme.core.music.data.local.SongOverrideEntity
import com.aria.rythme.core.music.data.model.Playlist
import com.aria.rythme.core.music.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class PlaylistRepository(
    private val playlistDao: PlaylistDao,
    private val songDao: SongDao,
    private val songOverrideDao: SongOverrideDao
) {

    fun getAllPlaylists(): Flow<List<Playlist>> =
        playlistDao.getAllPlaylists().map { entities ->
            entities.map { entity ->
                val count = playlistDao.getPlaylistSongCount(entity.id)
                entity.toPlaylist(songCount = count)
            }
        }

    suspend fun getPlaylistById(id: Long): Playlist? {
        val entity = playlistDao.getPlaylistById(id) ?: return null
        val count = playlistDao.getPlaylistSongCount(id)
        return entity.toPlaylist(songCount = count)
    }

    fun getPlaylistSongs(playlistId: Long): Flow<List<Song>> =
        combine(
            playlistDao.getSongIdsForPlaylist(playlistId),
            songOverrideDao.getAllOverrides()
        ) { songIds, overrides ->
            val overrideMap = overrides.associateBy { it.songId }
            songIds.mapNotNull { songId ->
                songDao.getSongById(songId)?.toSong()?.applyOverride(overrideMap[songId])
            }
        }

    suspend fun createPlaylist(name: String, description: String = ""): Long {
        val entity = PlaylistEntity(
            name = name,
            description = description
        )
        return playlistDao.insertPlaylist(entity)
    }

    suspend fun deletePlaylist(id: Long) {
        playlistDao.deletePlaylist(id)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        val maxPosition = playlistDao.getMaxPosition(playlistId) ?: -1
        playlistDao.insertPlaylistSong(
            PlaylistSongEntity(
                playlistId = playlistId,
                songId = songId,
                position = maxPosition + 1
            )
        )
        playlistDao.updatePlaylist(
            playlistDao.getPlaylistById(playlistId)!!.copy(updatedAt = System.currentTimeMillis())
        )
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.deletePlaylistSong(playlistId, songId)
        playlistDao.updatePlaylist(
            playlistDao.getPlaylistById(playlistId)!!.copy(updatedAt = System.currentTimeMillis())
        )
    }
}

private fun Song.applyOverride(override: SongOverrideEntity?): Song {
    if (override == null) return this
    return copy(
        title = override.title ?: title,
        artist = override.artist ?: artist,
        album = override.album ?: album,
        albumArtist = override.albumArtist ?: albumArtist,
        genre = override.genre ?: genre,
        composer = override.composer ?: composer,
        trackNumber = override.trackNumber ?: trackNumber,
        discNumber = override.discNumber ?: discNumber,
        year = override.year ?: year
    )
}
