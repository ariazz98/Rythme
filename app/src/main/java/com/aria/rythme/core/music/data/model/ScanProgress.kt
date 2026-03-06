package com.aria.rythme.core.music.data.model

sealed interface ScanProgress {
    data object Idle : ScanProgress
    data class Discovering(val foundCount: Int) : ScanProgress
    data class Syncing(val current: Int, val total: Int, val phase: SyncPhase) : ScanProgress
    data class PostProcessing(val step: String) : ScanProgress
    data class Completed(val stats: ScanStats) : ScanProgress
    data class Failed(val error: String) : ScanProgress
}

enum class SyncPhase { ADDING, UPDATING, DELETING }

data class ScanStats(
    val added: Int,
    val deleted: Int,
    val modified: Int,
    val unchanged: Int,
    val totalSongs: Int,
    val durationMs: Long
)
