package com.aria.rythme.core.music.data.repository

import com.aria.rythme.core.music.data.local.AlbumDao
import com.aria.rythme.core.music.data.local.ArtistDao
import com.aria.rythme.core.music.data.local.SongDao
import com.aria.rythme.core.music.data.model.Album
import com.aria.rythme.core.music.data.model.Artist
import com.aria.rythme.core.music.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 音乐仓库 — 纯查询层
 *
 * 只负责对外暴露数据查询 API。
 * 扫描/同步职责已迁移到 MusicIndexer。
 */
class MusicRepository(
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao
) {
    // ==================== 歌曲查询 ====================

    fun getAllSongs(): Flow<List<Song>> =
        songDao.getAllSongs().map { entities -> entities.map { it.toSong() } }

    suspend fun getAllSongsOnce(): List<Song> =
        songDao.getAllSongsOnce().map { it.toSong() }

    suspend fun getSongById(id: Long): Song? =
        songDao.getSongById(id)?.toSong()

    fun getSongsByAlbum(albumId: Long): Flow<List<Song>> =
        songDao.getSongsByAlbum(albumId).map { entities -> entities.map { it.toSong() } }

    fun getSongsByArtist(artistId: Long): Flow<List<Song>> =
        songDao.getSongsByArtist(artistId).map { entities -> entities.map { it.toSong() } }

    fun getSongsByGenre(genre: String): Flow<List<Song>> =
        songDao.getSongsByGenre(genre).map { entities -> entities.map { it.toSong() } }

    fun getSongsByComposer(composer: String): Flow<List<Song>> =
        songDao.getSongsByComposer(composer).map { entities -> entities.map { it.toSong() } }

    fun getSongsByFolder(folderId: Long): Flow<List<Song>> =
        songDao.getSongsByFolder(folderId).map { entities -> entities.map { it.toSong() } }

    fun searchSongs(query: String): Flow<List<Song>> =
        songDao.searchSongs(query).map { entities -> entities.map { it.toSong() } }

    fun getRecentlyAdded(limit: Int = 50): Flow<List<Song>> =
        songDao.getRecentlyAdded(limit).map { entities -> entities.map { it.toSong() } }

    // ==================== 专辑查询 ====================

    fun getAllAlbums(): Flow<List<Album>> =
        albumDao.getAllAlbums().map { entities -> entities.map { it.toAlbum() } }

    suspend fun getAlbumById(id: Long): Album? =
        albumDao.getAlbumById(id)?.toAlbum()

    fun getAlbumsByArtist(artistId: Long): Flow<List<Album>> =
        albumDao.getAlbumsByArtist(artistId).map { entities -> entities.map { it.toAlbum() } }

    // ==================== 艺术家查询 ====================

    fun getAllArtists(): Flow<List<Artist>> =
        artistDao.getAllArtists().map { entities -> entities.map { it.toArtist() } }

    suspend fun getArtistById(id: Long): Artist? =
        artistDao.getArtistById(id)?.toArtist()

    // ==================== 分类列表 ====================

    fun getAllGenres(): Flow<List<String>> = songDao.getAllGenres()

    fun getAllComposers(): Flow<List<String>> = songDao.getAllComposers()

    // ==================== 统计 ====================

    suspend fun getSongCount(): Int = songDao.getSongCount()

    suspend fun hasCache(): Boolean = songDao.getSongCount() > 0
}
