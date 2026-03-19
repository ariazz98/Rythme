package com.aria.rythme.core.music.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface LyricsDao {

    @Query("SELECT * FROM lyrics WHERE song_id = :songId LIMIT 1")
    suspend fun getBySongId(songId: Long): LyricsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LyricsEntity)

    @Query("DELETE FROM lyrics WHERE song_id = :songId")
    suspend fun deleteBySongId(songId: Long)

    @Query("DELETE FROM lyrics")
    suspend fun deleteAll()
}
