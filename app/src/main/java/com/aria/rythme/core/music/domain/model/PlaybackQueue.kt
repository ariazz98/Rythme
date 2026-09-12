package com.aria.rythme.core.music.domain.model

import com.aria.rythme.core.music.data.model.Song
import java.util.UUID

/** 重建同一条目的媒体列表不产生历史，真正的单曲循环则产生新记录。 */
internal fun shouldRecordPlaybackHistory(previousId: String?, nextId: String?, repeated: Boolean): Boolean =
    previousId != null && (previousId != nextId || repeated)

/**
 * 一次具体的入队记录。
 *
 * [Song.id] 标识资料库歌曲，[id] 标识这首歌在当前队列中的一次出现；
 * 因此同一首歌可以重复入队，并被独立选择、重排和删除。
 */
data class QueueEntry(
    val id: String,
    val song: Song
) {
    companion object {
        fun create(song: Song): QueueEntry = QueueEntry(
            id = UUID.randomUUID().toString(),
            song = song
        )
    }
}

/** 播放层对外发布的原子队列快照。 */
data class PlaybackQueue(
    val entries: List<QueueEntry> = emptyList(),
    val currentIndex: Int = -1,
    val orderedEntryCount: Int = entries.size
) {
    val currentEntry: QueueEntry?
        get() = entries.getOrNull(currentIndex)

    val orderedEntries: List<QueueEntry>
        get() = entries.take(orderedEntryCount.coerceIn(0, entries.size))

    val autoplayEntries: List<QueueEntry>
        get() = entries.drop(orderedEntryCount.coerceIn(0, entries.size))

    fun indexOf(entryId: String?): Int =
        if (entryId == null) -1 else entries.indexOfFirst { it.id == entryId }
}
