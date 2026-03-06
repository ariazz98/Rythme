package com.aria.rythme.core.music.data.datasource

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.aria.rythme.core.utils.RythmeLogger
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.music.data.settings.ScanSettings
import com.aria.rythme.core.music.data.settings.AppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import androidx.core.net.toUri

/**
 * MediaStore 指纹 — 轻量级，用于增量对比
 */
data class MediaStoreFingerprint(
    val id: Long,
    val dateModified: Long,
    val size: Long,
    val generationModified: Long
)

/**
 * MediaStore 数据源
 *
 * 支持两种查询模式：
 * 1. 指纹查询（4 列）— 快速获取 MediaStore 状态用于增量对比
 * 2. 完整查询（全部列）— 获取完整元数据用于插入/更新
 */
class MediaStoreSource(
    private val context: Context,
    private val settingsRepository: AppSettingsRepository
) {
    private val contentResolver: ContentResolver = context.contentResolver

    /**
     * 获取当前 MediaStore version
     */
    fun getMediaStoreVersion(): String {
        return MediaStore.getVersion(context)
    }

    /**
     * 获取当前 MediaStore generation
     */
    fun getMediaStoreGeneration(): Long {
        return MediaStore.getGeneration(context, MediaStore.VOLUME_EXTERNAL)
    }

    /**
     * 指纹查询 — 仅 4 列，用于增量对比
     */
    suspend fun queryFingerprints(): List<MediaStoreFingerprint> = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.first()
        val fingerprints = mutableListOf<MediaStoreFingerprint>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.DATE_MODIFIED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.GENERATION_MODIFIED,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )

        val cursor = queryBase(projection)
        cursor?.use {
            while (it.moveToNext()) {
                val duration = it.getLongOrDefault(MediaStore.Audio.Media.DURATION, 0L)
                val size = it.getLongOrDefault(MediaStore.Audio.Media.SIZE, 0L)
                val path = it.getStringOrDefault(MediaStore.Audio.Media.DATA, "")

                // 应用基础过滤（时长/大小/系统目录）
                if (duration < settings.minDurationMs) continue
                if (size < settings.minSizeBytes) continue
                if (settings.excludeSystemDirs && isInSystemAudioDir(path)) continue

                fingerprints.add(
                    MediaStoreFingerprint(
                        id = it.getLongOrDefault(MediaStore.Audio.Media._ID, 0L),
                        dateModified = it.getLongOrDefault(MediaStore.Audio.Media.DATE_MODIFIED, 0L),
                        size = size,
                        generationModified = it.getLongOrDefault(MediaStore.Audio.Media.GENERATION_MODIFIED, 0L)
                    )
                )
            }
        }

        RythmeLogger.d(TAG, "指纹查询完成: ${fingerprints.size} 条")
        fingerprints
    }

    /**
     * 按 ID 列表查询完整歌曲数据
     */
    suspend fun querySongsByIds(ids: List<Long>): List<Song> = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext emptyList()

        val songs = mutableListOf<Song>()
        // 分批查询避免 SQL IN 子句过长
        ids.chunked(500).forEach { chunk ->
            val placeholders = chunk.joinToString(",") { "?" }
            val selection = "${MediaStore.Audio.Media._ID} IN ($placeholders)"
            val selectionArgs = chunk.map { it.toString() }.toTypedArray()
            val cursor = queryFull(selection, selectionArgs)
            cursor?.use {
                while (it.moveToNext()) {
                    songs.add(parseSongCursor(it))
                }
            }
        }
        songs
    }

    /**
     * 全量扫描（首次安装或 MediaStore version 变化时使用）
     */
    suspend fun scanAllSongs(): List<Song> = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.first()
        RythmeLogger.d(TAG, "全量扫描开始")

        val songs = mutableListOf<Song>()
        val cursor = queryFull()

        cursor?.use {
            while (it.moveToNext()) {
                val song = parseSongCursor(it)
                if (shouldIncludeSong(song, settings)) {
                    songs.add(song)
                }
            }
        }

        RythmeLogger.d(TAG, "全量扫描完成: ${songs.size} 首歌曲")
        songs
    }

    // ==================== 内部方法 ====================

    private fun queryBase(projection: Array<String>, selection: String? = null, selectionArgs: Array<String>? = null): Cursor? {
        val baseSelection = "${MediaStore.Audio.Media.IS_MUSIC} = 1" +
                " AND ${MediaStore.Audio.Media.IS_PENDING} = 0" +
                " AND ${MediaStore.Audio.Media.IS_TRASHED} = 0"

        val finalSelection = if (selection != null) {
            "$baseSelection AND ($selection)"
        } else {
            baseSelection
        }

        return try {
            contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                finalSelection,
                selectionArgs,
                "${MediaStore.Audio.Media.TITLE} ASC"
            )
        } catch (e: Exception) {
            RythmeLogger.e(TAG, "MediaStore 查询失败", e)
            null
        }
    }

    private fun queryFull(selection: String? = null, selectionArgs: Array<String>? = null): Cursor? {
        return queryBase(FULL_PROJECTION, selection, selectionArgs)
    }

    private fun shouldIncludeSong(song: Song, settings: ScanSettings): Boolean {
        if (song.duration < settings.minDurationMs) return false
        if (song.size < settings.minSizeBytes) return false
        if (settings.excludeSystemDirs && isInSystemAudioDir(song.path)) return false
        return true
    }

    private fun isInSystemAudioDir(path: String): Boolean {
        val lowerPath = path.lowercase()
        return ScanSettings.SYSTEM_AUDIO_DIRS.any { dir ->
            lowerPath.contains("/$dir/", ignoreCase = true) ||
            lowerPath.contains("/${dir.lowercase()}/")
        }
    }

    private fun parseSongCursor(cursor: Cursor): Song {
        val id = cursor.getLongOrDefault(MediaStore.Audio.Media._ID, 0L)
        val albumId = cursor.getLongOrDefault(MediaStore.Audio.Media.ALBUM_ID, 0L)
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
        val coverUri = getAlbumCoverUri(albumId)

        return Song(
            id = id,
            title = cursor.getStringOrDefault(MediaStore.Audio.Media.TITLE, "未知歌曲"),
            artist = cursor.getStringOrDefault(MediaStore.Audio.Media.ARTIST, "未知艺术家"),
            artistId = cursor.getLongOrDefault(MediaStore.Audio.Media.ARTIST_ID, 0L),
            album = cursor.getStringOrDefault(MediaStore.Audio.Media.ALBUM, "未知专辑"),
            albumId = albumId,
            duration = cursor.getLongOrDefault(MediaStore.Audio.Media.DURATION, 0L),
            trackNumber = cursor.getIntOrDefault(MediaStore.Audio.Media.TRACK, 0),
            path = cursor.getStringOrDefault(MediaStore.Audio.Media.DATA, ""),
            uri = uri,
            coverUri = coverUri,
            dateAdded = cursor.getLongOrDefault(MediaStore.Audio.Media.DATE_ADDED, 0L),
            dateModified = cursor.getLongOrDefault(MediaStore.Audio.Media.DATE_MODIFIED, 0L),
            size = cursor.getLongOrDefault(MediaStore.Audio.Media.SIZE, 0L),
            mimeType = cursor.getStringOrDefault(MediaStore.Audio.Media.MIME_TYPE, "audio/*"),
            genre = cursor.getStringOrDefault(MediaStore.Audio.Media.GENRE, ""),
            composer = cursor.getStringOrDefault(MediaStore.Audio.Media.COMPOSER, ""),
            bitrate = cursor.getIntOrDefault(MediaStore.Audio.Media.BITRATE, 0),
            year = cursor.getIntOrDefault(MediaStore.Audio.Media.YEAR, 0),
            discNumber = parseDiscNumber(cursor.getStringOrDefault(MediaStore.Audio.Media.DISC_NUMBER, "")),
            albumArtist = cursor.getStringOrDefault(MediaStore.Audio.Media.ALBUM_ARTIST, ""),
            folderName = cursor.getStringOrDefault(MediaStore.Audio.Media.BUCKET_DISPLAY_NAME, ""),
            folderId = cursor.getLongOrDefault(MediaStore.Audio.Media.BUCKET_ID, 0L),
            generationAdded = cursor.getLongOrDefault(MediaStore.Audio.Media.GENERATION_ADDED, 0L),
            generationModified = cursor.getLongOrDefault(MediaStore.Audio.Media.GENERATION_MODIFIED, 0L)
        )
    }

    private fun parseDiscNumber(raw: String): Int {
        // DISC_NUMBER 格式可能是 "1/2" 或 "1"
        return raw.split("/").firstOrNull()?.trim()?.toIntOrNull() ?: 0
    }

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

    // Cursor 安全扩展
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

    companion object {
        private const val TAG = "MediaStoreSource"

        private val FULL_PROJECTION = arrayOf(
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
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.GENRE,
            MediaStore.Audio.Media.COMPOSER,
            MediaStore.Audio.Media.BITRATE,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.DISC_NUMBER,
            MediaStore.Audio.Media.ALBUM_ARTIST,
            MediaStore.Audio.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Audio.Media.BUCKET_ID,
            MediaStore.Audio.Media.GENERATION_ADDED,
            MediaStore.Audio.Media.GENERATION_MODIFIED
        )
    }
}
