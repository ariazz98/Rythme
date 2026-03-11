package com.aria.rythme.core.music.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumDao {

    @Query("SELECT * FROM albums ORDER BY title ASC")
    fun getAllAlbums(): Flow<List<AlbumEntity>>

    @Query("SELECT * FROM albums WHERE id = :id")
    suspend fun getAlbumById(id: Long): AlbumEntity?

    @Query("SELECT * FROM albums WHERE artistId = :artistId ORDER BY year DESC")
    fun getAlbumsByArtist(artistId: Long): Flow<List<AlbumEntity>>

    /**
     * 通过 songs 表查找包含指定艺术家歌曲的所有专辑（不限于专辑主艺术家）
     */
    @Query("""
        SELECT DISTINCT a.* FROM albums a
        INNER JOIN songs s ON a.id = s.albumId
        WHERE s.artistId = :artistId
        ORDER BY a.year DESC
    """)
    fun getAlbumsContainingArtist(artistId: Long): Flow<List<AlbumEntity>>

    /**
     * 根据 ID 列表批量获取专辑
     */
    @Query("SELECT * FROM albums WHERE id IN (:ids) ORDER BY year DESC")
    suspend fun getAlbumsByIds(ids: List<Long>): List<AlbumEntity>

    @Query("SELECT COUNT(*) FROM albums")
    suspend fun getAlbumCount(): Int

    @Upsert
    suspend fun upsertAll(albums: List<AlbumEntity>)

    @Query("DELETE FROM albums")
    suspend fun deleteAll()
}
