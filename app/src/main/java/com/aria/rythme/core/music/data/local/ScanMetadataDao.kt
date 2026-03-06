package com.aria.rythme.core.music.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface ScanMetadataDao {

    @Query("SELECT * FROM scan_metadata WHERE id = 0")
    suspend fun get(): ScanMetadataEntity?

    @Upsert
    suspend fun upsert(metadata: ScanMetadataEntity)
}
