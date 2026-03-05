package com.aria.rythme.core.music.data.datasource

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.aria.rythme.core.utils.RythmeLogger
import com.aria.rythme.core.music.data.model.Album
import com.aria.rythme.core.music.data.model.Artist
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.music.data.settings.ScanSettings
import com.aria.rythme.core.music.data.settings.AppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import java.io.File

/**
 * 扫描结果
 */
data class ScanResult(
    val scannedCount: Int,
    val addedCount: Int = 0,
    val removedCount: Int = 0
)

/**
 * MediaStore 数据源
 *
 * 通过 Android MediaStore API 扫描设备上的本地音乐文件。
 * 只负责扫描，不负责数据存储。
 *
 * ## 权限要求
 * - Android 13+: READ_MEDIA_AUDIO
 * - Android 12及以下: READ_EXTERNAL_STORAGE
 *
 * ## 过滤规则
 * - 最小时长：默认 30 秒（可配置）
 * - 最小大小：默认 100KB（可配置）
 * - 排除系统音效目录：Ringtones/Alarms/Notifications
 *
 * @param context 应用上下文
 * @param settingsRepository 扫描设置仓库
 */
class MediaStoreSource(
    private val context: Context,
    private val settingsRepository: AppSettingsRepository
) {

    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * 从 MediaStore 扫描歌曲
     * 
     * 扫描设备上的所有音频文件，根据过滤规则过滤。
     * 
     * @return 符合条件的歌曲列表
     * @throws Exception 扫描失败时抛出异常
     */
    suspend fun scanFromMediaStore(): List<Song> = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.first()
        RythmeLogger.d(TAG, "开始扫描歌曲，过滤配置: 最小时长=${settings.minDurationMs}ms, 最小大小=${settings.minSizeBytes}bytes")
        
        val songs = mutableListOf<Song>()
        val cursor = querySongs()

        cursor?.use {
            while (it.moveToNext()) {
                val song = parseSongCursor(it)
                if (shouldIncludeSong(song, settings)) {
                    songs.add(song)
                }
            }
        }
        
        RythmeLogger.d(TAG, "扫描完成，共 ${songs.size} 首歌曲符合条件")
        return@withContext songs
    }
    
    /**
     * 判断歌曲是否应被包含
     *
     * @param song 歌曲
     * @param settings 扫描设置
     * @return 是否应包含
     */
    private fun shouldIncludeSong(song: Song, settings: ScanSettings): Boolean {
        // 检查文件是否存在（MediaStore 索引可能滞后于实际文件删除）
        if (song.path.isNotEmpty() && !File(song.path).exists()) {
            return false
        }

        // 检查最小时长
        if (song.duration < settings.minDurationMs) {
            return false
        }

        // 检查最小大小
        if (song.size < settings.minSizeBytes) {
            return false
        }

        // 检查是否在系统音效目录
        if (settings.excludeSystemDirs && isInSystemAudioDir(song.path)) {
            return false
        }

        return true
    }
    
    /**
     * 判断路径是否在系统音效目录
     *
     * @param path 文件路径
     * @return 是否在系统音效目录
     */
    private fun isInSystemAudioDir(path: String): Boolean {
        val lowerPath = path.lowercase()
        return ScanSettings.SYSTEM_AUDIO_DIRS.any { dir ->
            lowerPath.contains("/$dir/", ignoreCase = true) ||
            lowerPath.contains("/${dir.lowercase()}/")
        }
    }

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
     *
     * 默认过滤条件：IS_MUSIC = 1, IS_PENDING = 0, IS_TRASHED = 0
     * 排除非音乐文件、未完成下载、已删除文件
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

        // 基础过滤：仅音乐文件，排除未完成和已删除
        val baseSelection = "${MediaStore.Audio.Media.IS_MUSIC} = 1" +
                " AND ${MediaStore.Audio.Media.IS_PENDING} = 0" +
                " AND ${MediaStore.Audio.Media.IS_TRASHED} = 0"

        val finalSelection = if (selection != null) {
            "$baseSelection AND ($selection)"
        } else {
            baseSelection
        }

        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        return try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                finalSelection,
                selectionArgs,
                sortOrder
            )
        } catch (e: Exception) {
            RythmeLogger.e(TAG, "查询歌曲失败", e)
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
            RythmeLogger.e(TAG, "查询专辑失败", e)
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
            RythmeLogger.e(TAG, "查询艺术家失败", e)
            null
        }
    }

    /**
     * 解析歌曲游标
     * 
     * 安全地解析游标数据，避免列不存在时崩溃
     */
    private fun parseSongCursor(cursor: Cursor): Song {
        val id = cursor.getLongOrDefault(MediaStore.Audio.Media._ID, 0L)
        val title = cursor.getStringOrDefault(MediaStore.Audio.Media.TITLE, "未知歌曲")
        val artist = cursor.getStringOrDefault(MediaStore.Audio.Media.ARTIST, "未知艺术家")
        val artistId = cursor.getLongOrDefault(MediaStore.Audio.Media.ARTIST_ID, 0L)
        val album = cursor.getStringOrDefault(MediaStore.Audio.Media.ALBUM, "未知专辑")
        val albumId = cursor.getLongOrDefault(MediaStore.Audio.Media.ALBUM_ID, 0L)
        val duration = cursor.getLongOrDefault(MediaStore.Audio.Media.DURATION, 0L)
        val trackNumber = cursor.getIntOrDefault(MediaStore.Audio.Media.TRACK, 0)
        val path = cursor.getStringOrDefault(MediaStore.Audio.Media.DATA, "")
        val dateAdded = cursor.getLongOrDefault(MediaStore.Audio.Media.DATE_ADDED, 0L)
        val dateModified = cursor.getLongOrDefault(MediaStore.Audio.Media.DATE_MODIFIED, 0L)
        val size = cursor.getLongOrDefault(MediaStore.Audio.Media.SIZE, 0L)
        val mimeType = cursor.getStringOrDefault(MediaStore.Audio.Media.MIME_TYPE, "audio/*")

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
    
    // Cursor 安全扩展函数
    private fun Cursor.getStringOrDefault(column: String, default: String): String {
        val index = getColumnIndex(column)
        return if (index >= 0) getString(index) ?: default else default
    }
    
    private fun Cursor.getLongOrDefault(column: String, default: Long): Long {
        val index = getColumnIndex(column)
        return if (index >= 0) getLong(index) else default
    }
    
    private fun Cursor.getIntOrDefault(column: String, default: Int): Int {
        val index = getColumnIndex(column)
        return if (index >= 0) getInt(index) else default
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
    
    companion object {
        private const val TAG = "MediaStoreSource"
    }
}
