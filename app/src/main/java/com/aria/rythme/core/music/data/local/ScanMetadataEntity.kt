package com.aria.rythme.core.music.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scan_metadata")
data class ScanMetadataEntity(
    @PrimaryKey val id: Int = 0,
    val lastMediaStoreVersion: String = "",
    val lastMediaStoreGeneration: Long = -1,
    val lastFullScanTimestamp: Long = 0,
    val lastIncrementalScanTimestamp: Long = 0,
    val totalSongsScanned: Int = 0
)
