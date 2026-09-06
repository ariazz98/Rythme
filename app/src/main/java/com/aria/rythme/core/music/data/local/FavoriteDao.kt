package com.aria.rythme.core.music.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE kind = :kind AND itemId = :itemId)")
    fun observeFavorite(kind: String, itemId: Long): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE kind = :kind AND itemId = :itemId)")
    suspend fun isFavorite(kind: String, itemId: Long): Boolean

    @Upsert
    suspend fun upsert(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE kind = :kind AND itemId = :itemId")
    suspend fun delete(kind: String, itemId: Long)

    @Transaction
    suspend fun toggle(kind: String, itemId: Long): Boolean {
        return if (isFavorite(kind, itemId)) {
            delete(kind, itemId)
            false
        } else {
            upsert(FavoriteEntity(kind = kind, itemId = itemId))
            true
        }
    }
}
