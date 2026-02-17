package com.aria.rythme.feature.player.data.datasource

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.aria.rythme.feature.player.data.model.Album
import com.aria.rythme.feature.player.data.model.Artist
import com.aria.rythme.feature.player.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import androidx.core.net.toUri

/**
 * MediaStore 数据源
 *
 * 通过 Android MediaStore API 查询设备上的本地音乐文件。
 * 提供歌曲、专辑、艺术家的查询功能。
 *
 * ## 权限要求
 * - Android 13+: READ_MEDIA_AUDIO
 * - Android 12及以下: READ_EXTERNAL_STORAGE
 *
 * ## 使用示例
 * ```kotlin
 * val mediaStoreSource = MediaStoreSource(context)
 * mediaStoreSource.getAllSongs().collect { songs ->
 *     // 处理歌曲列表
 * }
 * ```
 */
class MediaStoreSource(private val context: Context) {

    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * 获取所有歌曲
     *
     * @return 歌曲列表流
     */
    fun getAllSongs(): Flow<List<Song>> = flow {
        val songs = mutableListOf<Song>()
        val cursor = querySongs()

        cursor?.use {
            while (it.moveToNext()) {
                songs.add(parseSongCursor(it))
            }
        }

        emit(songs)
    }.flowOn(Dispatchers.IO)

    /**
     * 按专辑获取歌曲
     *
     * @param albumId 专辑ID
     * @return 歌曲列表流
     */
    fun getSongsByAlbum(albumId: Long): Flow<List<Song>> = flow {
        val songs = mutableListOf<Song>()
        val selection = "${MediaStore.Audio.Media.ALBUM_ID} = ?"
        val selectionArgs = arrayOf(albumId.toString())
        val cursor = querySongs(selection, selectionArgs)

        cursor?.use {
            while (it.moveToNext()) {
                songs.add(parseSongCursor(it))
            }
        }

        emit(songs)
    }.flowOn(Dispatchers.IO)

    /**
     * 按艺术家获取歌曲
     *
     * @param artistId 艺术家ID
     * @return 歌曲列表流
     */
    fun getSongsByArtist(artistId: Long): Flow<List<Song>> = flow {
        val songs = mutableListOf<Song>()
        val selection = "${MediaStore.Audio.Media.ARTIST_ID} = ?"
        val selectionArgs = arrayOf(artistId.toString())
        val cursor = querySongs(selection, selectionArgs)

        cursor?.use {
            while (it.moveToNext()) {
                songs.add(parseSongCursor(it))
            }
        }

        emit(songs)
    }.flowOn(Dispatchers.IO)

    /**
     * 获取所有专辑
     *
     * @return 专辑列表流
     */
    fun getAllAlbums(): Flow<List<Album>> = flow {
        val albums = mutableListOf<Album>()
        val cursor = queryAlbums()

        cursor?.use {
            while (it.moveToNext()) {
                albums.add(parseAlbumCursor(it))
            }
        }

        emit(albums)
    }.flowOn(Dispatchers.IO)

    /**
     * 获取所有艺术家
     *
     * @return 艺术家列表流
     */
    fun getAllArtists(): Flow<List<Artist>> = flow {
        val artists = mutableListOf<Artist>()
        val cursor = queryArtists()

        cursor?.use {
            while (it.moveToNext()) {
                artists.add(parseArtistCursor(it))
            }
        }

        emit(artists)
    }.flowOn(Dispatchers.IO)

    /**
     * 搜索歌曲
     *
     * @param query 搜索关键词
     * @return 歌曲列表流
     */
    fun searchSongs(query: String): Flow<List<Song>> = flow {
        val songs = mutableListOf<Song>()
        val selection = "${MediaStore.Audio.Media.TITLE} LIKE ? OR ${MediaStore.Audio.Media.ARTIST} LIKE ?"
        val selectionArgs = arrayOf("%$query%", "%$query%")
        val cursor = querySongs(selection, selectionArgs)

        cursor?.use {
            while (it.moveToNext()) {
                songs.add(parseSongCursor(it))
            }
        }

        emit(songs)
    }.flowOn(Dispatchers.IO)

    /**
     * 查询歌曲游标
     */
    private fun querySongs(selection: String? = null, selectionArgs: Array<String>? = null): Cursor? {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE
        )

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        return try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 查询专辑游标
     */
    private fun queryAlbums(): Cursor? {
        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.ARTIST_ID,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
            MediaStore.Audio.Albums.FIRST_YEAR
        )

        val sortOrder = "${MediaStore.Audio.Albums.ALBUM} ASC"

        return try {
            contentResolver.query(
                MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 查询艺术家游标
     */
    private fun queryArtists(): Cursor? {
        val projection = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS
        )

        val sortOrder = "${MediaStore.Audio.Artists.ARTIST} ASC"

        return try {
            contentResolver.query(
                MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 解析歌曲游标
     */
    private fun parseSongCursor(cursor: Cursor): Song {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
        val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
        val artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
        val artistId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID))
        val album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
        val albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
        val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
        val trackNumber = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK))
        val path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
        val dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED))
        val dateModified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED))
        val size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE))
        val mimeType = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE))

        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
        val coverUri = getAlbumCoverUri(albumId)

        return Song(
            id = id,
            title = title,
            artist = artist,
            artistId = artistId,
            album = album,
            albumId = albumId,
            duration = duration,
            trackNumber = trackNumber,
            path = path,
            uri = uri,
            coverUri = coverUri,
            dateAdded = dateAdded,
            dateModified = dateModified,
            size = size,
            mimeType = mimeType
        )
    }

    /**
     * 解析专辑游标
     */
    private fun parseAlbumCursor(cursor: Cursor): Album {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID))
        val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM))
        val artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST))
        val artistId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST_ID))
        val songCount = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS))
        val year = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.FIRST_YEAR))
        val coverUri = getAlbumCoverUri(id)

        return Album(
            id = id,
            title = title,
            artist = artist,
            artistId = artistId,
            songCount = songCount,
            coverUri = coverUri,
            year = year
        )
    }

    /**
     * 解析艺术家游标
     */
    private fun parseArtistCursor(cursor: Cursor): Artist {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID))
        val name = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST))
        val albumCount = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS))
        val songCount = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS))

        return Artist(
            id = id,
            name = name,
            albumCount = albumCount,
            songCount = songCount
        )
    }

    /**
     * 获取专辑封面URI
     */
    private fun getAlbumCoverUri(albumId: Long): Uri? {
        return if (albumId > 0) {
            ContentUris.withAppendedId(
                "content://media/external/audio/albumart".toUri(),
                albumId
            )
        } else {
            null
        }
    }
}
