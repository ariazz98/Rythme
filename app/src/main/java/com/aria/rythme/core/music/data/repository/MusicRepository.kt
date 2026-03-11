package com.aria.rythme.core.music.data.repository

import com.aria.rythme.core.music.data.local.AlbumDao
import com.aria.rythme.core.music.data.local.AlbumEntity
import com.aria.rythme.core.music.data.local.ArtistDao
import com.aria.rythme.core.music.data.local.ArtistEntity
import com.aria.rythme.core.music.data.local.SongDao
import com.aria.rythme.core.music.data.local.SongEntity
import com.aria.rythme.core.music.data.local.SongOverrideDao
import com.aria.rythme.core.music.data.local.SongOverrideEntity
import com.aria.rythme.core.music.data.model.Album
import com.aria.rythme.core.music.data.model.Artist
import com.aria.rythme.core.music.data.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * 音乐仓库
 *
 * 查询时自动合并用户覆盖层（song_overrides），
 * 确保用户手动编辑的信息不会被 MediaStore 同步覆盖。
 */
class MusicRepository(
    private val songDao: SongDao,
    private val albumDao: AlbumDao,
    private val artistDao: ArtistDao,
    private val songOverrideDao: SongOverrideDao
) {
    // ==================== 歌曲查询（自动合并覆盖层） ====================

    fun getAllSongs(): Flow<List<Song>> =
        songDao.getAllSongs().withOverrides()

    suspend fun getAllSongsOnce(): List<Song> {
        val overrides = songOverrideDao.getAllOverridesOnce().asMap()
        return songDao.getAllSongsOnce().map { it.toSong().applyOverride(overrides[it.id]) }
    }

    suspend fun getSongById(id: Long): Song? {
        val override = songOverrideDao.getOverride(id)
        return songDao.getSongById(id)?.toSong()?.applyOverride(override)
    }

    fun getSongsByAlbum(albumId: Long): Flow<List<Song>> =
        songDao.getSongsByAlbum(albumId).withOverrides()

    fun getSongsByArtist(artistId: Long): Flow<List<Song>> =
        songDao.getSongsByArtist(artistId).withOverrides()

    fun getSongsByGenre(genre: String): Flow<List<Song>> =
        songDao.getAllSongs().withOverrides().map { songs ->
            songs.filter { it.genre == genre }
        }

    fun getSongsByComposer(composer: String): Flow<List<Song>> =
        songDao.getAllSongs().withOverrides().map { songs ->
            songs.filter { it.composer == composer }
        }

    fun getSongsByFolder(folderId: Long): Flow<List<Song>> =
        songDao.getSongsByFolder(folderId).withOverrides()

    fun searchSongs(query: String): Flow<List<Song>> =
        songDao.searchSongs(query).withOverrides()

    fun getRecentlyAdded(limit: Int = 50): Flow<List<Song>> =
        songDao.getRecentlyAdded(limit).withOverrides()

    /**
     * 获取指定专辑中指定艺术家的歌曲
     */
    fun getSongsByAlbumAndArtist(albumId: Long, artistId: Long): Flow<List<Song>> =
        songDao.getSongsByAlbumAndArtist(albumId, artistId).withOverrides()

    /**
     * 获取指定专辑中指定作曲者的歌曲（合并覆盖层后筛选）
     */
    fun getSongsByAlbumAndComposer(albumId: Long, composer: String): Flow<List<Song>> =
        songDao.getSongsByAlbum(albumId).withOverrides().map { songs ->
            songs.filter { it.composer == composer }
        }

    /**
     * 获取指定专辑中指定流派的歌曲（合并覆盖层后筛选）
     */
    fun getSongsByAlbumAndGenre(albumId: Long, genre: String): Flow<List<Song>> =
        songDao.getSongsByAlbum(albumId).withOverrides().map { songs ->
            songs.filter { it.genre == genre }
        }

    // ==================== 专辑查询 ====================

    fun getAllAlbums(): Flow<List<Album>> =
        albumDao.getAllAlbums().map { entities -> entities.map { it.toAlbum() } }

    suspend fun getAlbumById(id: Long): Album? =
        albumDao.getAlbumById(id)?.toAlbum()

    fun getAlbumsByArtist(artistId: Long): Flow<List<Album>> =
        albumDao.getAlbumsByArtist(artistId).map { entities -> entities.map { it.toAlbum() } }

    /**
     * 查找包含指定艺术家歌曲的所有专辑（通过 songs 表 JOIN，不限于专辑主艺术家）
     */
    fun getAlbumsContainingArtist(artistId: Long): Flow<List<Album>> =
        albumDao.getAlbumsContainingArtist(artistId).map { entities -> entities.map { it.toAlbum() } }

    /**
     * 查找包含指定作曲者歌曲的所有专辑（合并覆盖层后提取 albumId，再批量查询）
     */
    fun getAlbumsContainingComposer(composer: String): Flow<List<Album>> =
        getSongsByComposer(composer).map { songs ->
            val albumIds = songs.map { it.albumId }.distinct()
            if (albumIds.isEmpty()) emptyList()
            else albumDao.getAlbumsByIds(albumIds).map { it.toAlbum() }
        }

    /**
     * 查找包含指定流派歌曲的所有专辑（合并覆盖层后提取 albumId，再批量查询）
     */
    fun getAlbumsContainingGenre(genre: String): Flow<List<Album>> =
        getSongsByGenre(genre).map { songs ->
            val albumIds = songs.map { it.albumId }.distinct()
            if (albumIds.isEmpty()) emptyList()
            else albumDao.getAlbumsByIds(albumIds).map { it.toAlbum() }
        }

    // ==================== 艺术家查询 ====================

    fun getAllArtists(): Flow<List<Artist>> =
        artistDao.getAllArtists().map { entities -> entities.map { it.toArtist() } }

    suspend fun getArtistById(id: Long): Artist? =
        artistDao.getArtistById(id)?.toArtist()

    // ==================== 分类列表（合并覆盖层后去重） ====================

    fun getAllGenres(): Flow<List<String>> =
        songDao.getAllSongs().withOverrides().map { songs ->
            songs.map { it.genre }.filter { it.isNotEmpty() }.distinct().sorted()
        }

    fun getAllComposers(): Flow<List<String>> =
        songDao.getAllSongs().withOverrides().map { songs ->
            songs.map { it.composer }.filter { it.isNotEmpty() }.distinct().sorted()
        }

    // ==================== 统计 ====================

    suspend fun getSongCount(): Int = songDao.getSongCount()

    suspend fun hasCache(): Boolean = songDao.getSongCount() > 0

    // ==================== 歌曲编辑 ====================

    /**
     * 保存用户对歌曲的编辑
     *
     * 计算与原始数据的差异，仅将用户修改的字段存入覆盖层。
     * 保存后重建聚合表，使 Artist/Album 反映最新数据。
     */
    suspend fun updateSong(original: Song, edited: Song) {
        val override = SongOverrideEntity(
            songId = original.id,
            title = edited.title.takeIf { it != original.title },
            artist = edited.artist.takeIf { it != original.artist },
            album = edited.album.takeIf { it != original.album },
            albumArtist = edited.albumArtist.takeIf { it != original.albumArtist },
            genre = edited.genre.takeIf { it != original.genre },
            composer = edited.composer.takeIf { it != original.composer },
            trackNumber = edited.trackNumber.takeIf { it != original.trackNumber },
            discNumber = edited.discNumber.takeIf { it != original.discNumber },
            year = edited.year.takeIf { it != original.year }
        )

        // 所有字段都没改，删除已有的覆盖
        if (override.isAllNull()) {
            songOverrideDao.delete(original.id)
        } else {
            songOverrideDao.upsert(override)
        }

        rebuildAggregations()
    }

    /**
     * 从 songs 表 + 覆盖层聚合重建 albums 和 artists 表
     */
    suspend fun rebuildAggregations() {
        val overrides = songOverrideDao.getAllOverridesOnce().asMap()
        val allSongs = songDao.getAllSongsOnce().map { it.applyOverride(overrides[it.id]) }

        // 重建专辑表
        val albums = allSongs.groupBy { it.albumId }.map { (albumId, songs) ->
            val first = songs.first()
            AlbumEntity(
                id = albumId,
                title = first.album,
                artist = first.albumArtist.ifEmpty { first.artist },
                artistId = first.artistId,
                songCount = songs.size,
                totalDuration = songs.sumOf { it.duration },
                coverUri = first.coverUri,
                year = songs.maxOf { it.year }
            )
        }
        albumDao.deleteAll()
        albumDao.upsertAll(albums)

        // 重建艺术家表
        val artists = allSongs.groupBy { it.artistId }.map { (artistId, songs) ->
            val first = songs.first()
            val albumIds = songs.map { it.albumId }.distinct()
            ArtistEntity(
                id = artistId,
                name = first.artist,
                albumCount = albumIds.size,
                songCount = songs.size,
                coverUri = songs.maxByOrNull { it.dateAdded }?.coverUri
            )
        }
        artistDao.deleteAll()
        artistDao.upsertAll(artists)
    }

    // ==================== 内部工具 ====================

    /**
     * 将歌曲 Flow 与覆盖层 Flow 合并，自动应用用户编辑
     */
    private fun Flow<List<SongEntity>>.withOverrides(): Flow<List<Song>> =
        this.combine(songOverrideDao.getAllOverrides()) { songs, overrideList ->
            val overrideMap = overrideList.associateBy { it.songId }
            songs.map { it.toSong().applyOverride(overrideMap[it.id]) }
        }

}

// ==================== 扩展函数 ====================

/**
 * 将覆盖层应用到 Song，非 null 字段覆盖原始值
 */
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

/**
 * 将覆盖层应用到 SongEntity（用于聚合重建）
 */
private fun SongEntity.applyOverride(override: SongOverrideEntity?): SongEntity {
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

/**
 * 判断覆盖层是否所有字段都为 null（即用户没有实际修改）
 */
private fun SongOverrideEntity.isAllNull(): Boolean =
    title == null && artist == null && album == null &&
        albumArtist == null && genre == null && composer == null &&
        trackNumber == null && discNumber == null && year == null

/**
 * 从列表构建 songId -> Override 的 Map
 */
private fun List<SongOverrideEntity>.asMap(): Map<Long, SongOverrideEntity> =
    associateBy { it.songId }
