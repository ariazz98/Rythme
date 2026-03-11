package com.aria.rythme.core.music.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * 歌曲数据访问对象
 * 
 * 定义对 songs 表的所有数据库操作。
 */
@Dao
interface SongDao {
    
    // ==================== 查询操作 ====================
    
    /**
     * 获取所有歌曲（响应式）
     */
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>
    
    /**
     * 获取所有歌曲（一次性）
     */
    @Query("SELECT * FROM songs ORDER BY title ASC")
    suspend fun getAllSongsOnce(): List<SongEntity>
    
    /**
     * 根据ID获取歌曲
     */
    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: Long): SongEntity?
    
    /**
     * 根据路径获取歌曲
     */
    @Query("SELECT * FROM songs WHERE path = :path")
    suspend fun getSongByPath(path: String): SongEntity?
    
    /**
     * 获取歌曲数量
     */
    @Query("SELECT COUNT(*) FROM songs")
    suspend fun getSongCount(): Int
    
    /**
     * 搜索歌曲（按标题、艺术家、专辑、流派、作曲者）
     */
    @Query("""
        SELECT * FROM songs
        WHERE title LIKE '%' || :query || '%'
           OR artist LIKE '%' || :query || '%'
           OR album LIKE '%' || :query || '%'
           OR genre LIKE '%' || :query || '%'
           OR composer LIKE '%' || :query || '%'
        ORDER BY title ASC
    """)
    fun searchSongs(query: String): Flow<List<SongEntity>>
    
    /**
     * 获取指定艺术家的所有歌曲
     */
    @Query("SELECT * FROM songs WHERE artistId = :artistId ORDER BY album, trackNumber")
    fun getSongsByArtist(artistId: Long): Flow<List<SongEntity>>

    /**
     * 获取指定专辑中指定艺术家的歌曲
     */
    @Query("SELECT * FROM songs WHERE albumId = :albumId AND artistId = :artistId ORDER BY discNumber, trackNumber")
    fun getSongsByAlbumAndArtist(albumId: Long, artistId: Long): Flow<List<SongEntity>>

    /**
     * 获取指定专辑的所有歌曲
     */
    @Query("SELECT * FROM songs WHERE albumId = :albumId ORDER BY trackNumber")
    fun getSongsByAlbum(albumId: Long): Flow<List<SongEntity>>
    
    /**
     * 获取最近添加的歌曲
     */
    @Query("SELECT * FROM songs ORDER BY dateAdded DESC LIMIT :limit")
    fun getRecentlyAdded(limit: Int = 50): Flow<List<SongEntity>>

    /**
     * 按流派获取歌曲
     */
    @Query("SELECT * FROM songs WHERE genre = :genre ORDER BY title ASC")
    fun getSongsByGenre(genre: String): Flow<List<SongEntity>>

    /**
     * 按作曲者获取歌曲
     */
    @Query("SELECT * FROM songs WHERE composer = :composer ORDER BY title ASC")
    fun getSongsByComposer(composer: String): Flow<List<SongEntity>>

    /**
     * 按文件夹获取歌曲
     */
    @Query("SELECT * FROM songs WHERE folderId = :folderId ORDER BY title ASC")
    fun getSongsByFolder(folderId: Long): Flow<List<SongEntity>>

    /**
     * 获取所有不重复的流派
     */
    @Query("SELECT DISTINCT genre FROM songs WHERE genre != '' ORDER BY genre ASC")
    fun getAllGenres(): Flow<List<String>>

    /**
     * 获取所有不重复的作曲者
     */
    @Query("SELECT DISTINCT composer FROM songs WHERE composer != '' ORDER BY composer ASC")
    fun getAllComposers(): Flow<List<String>>

    /**
     * 获取所有歌曲的指纹数据（用于增量同步对比）
     */
    @Query("SELECT id, dateModified, size, generationModified FROM songs")
    suspend fun getAllFingerprints(): List<SongFingerprint>
    
    /**
     * 获取所有已缓存歌曲的ID
     */
    @Query("SELECT id FROM songs")
    suspend fun getAllSongIds(): List<Long>
    
    /**
     * 获取所有已缓存歌曲的路径
     */
    @Query("SELECT path FROM songs")
    suspend fun getAllSongPaths(): List<String>
    
    // ==================== 写入操作 ====================
    
    /**
     * 插入单首歌曲
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: SongEntity)
    
    /**
     * 批量插入歌曲
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<SongEntity>)
    
    /**
     * 更新或插入歌曲
     */
    @Upsert
    suspend fun upsert(song: SongEntity)
    
    /**
     * 批量更新或插入歌曲
     */
    @Upsert
    suspend fun upsertAll(songs: List<SongEntity>)
    
    /**
     * 更新歌曲
     */
    @Update
    suspend fun update(song: SongEntity)
    
    /**
     * 删除单首歌曲
     */
    @Delete
    suspend fun delete(song: SongEntity)
    
    /**
     * 根据ID删除歌曲
     */
    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    /**
     * 根据ID列表批量删除歌曲
     */
    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)
    
    /**
     * 删除不在指定ID列表中的歌曲（用于清理已删除的文件）
     */
    @Query("DELETE FROM songs WHERE id NOT IN (:validIds)")
    suspend fun deleteNotIn(validIds: List<Long>)
    
    /**
     * 清空所有歌曲
     */
    @Query("DELETE FROM songs")
    suspend fun deleteAll()
}
