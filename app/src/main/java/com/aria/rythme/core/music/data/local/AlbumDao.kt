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

    @Query("SELECT COUNT(*) FROM albums")
    suspend fun getAlbumCount(): Int

    @Upsert
    suspend fun upsertAll(albums: List<AlbumEntity>)

    @Query("DELETE FROM albums")
    suspend fun deleteAll()
}
