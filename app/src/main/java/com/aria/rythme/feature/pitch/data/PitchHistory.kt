package com.aria.rythme.feature.pitch.data

/** 不可变分块列表：追加只复制末尾小块，已发布的 UI 快照不会被修改。 */
class PitchHistory private constructor(
    private val chunks: List<List<PitchFrame>>,
    private val tail: List<PitchFrame>
) : AbstractList<PitchFrame>(), RandomAccess {
    override val size: Int = chunks.size * CHUNK_SIZE + tail.size
    override fun get(index: Int): PitchFrame {
        if (index !in indices) throw IndexOutOfBoundsException("$index / $size")
        val chunk = index / CHUNK_SIZE
        return if (chunk < chunks.size) chunks[chunk][index % CHUNK_SIZE] else tail[index % CHUNK_SIZE]
    }
    fun append(frame: PitchFrame): PitchHistory {
        require(isEmpty() || frame.timeMs > last().timeMs) { "轨迹时间必须递增" }
        return if (tail.size < CHUNK_SIZE) PitchHistory(chunks, tail + frame)
        else PitchHistory(chunks + listOf(tail), listOf(frame))
    }
    private fun lowerBound(timeMs: Long): Int {
        var low = 0; var high = size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (get(mid).timeMs < timeMs) low = mid + 1 else high = mid
        }
        return low
    }
    fun window(startMs: Long, endMs: Long): List<PitchFrame> {
        if (endMs < startMs) return emptyList()
        return subList(lowerBound(startMs), lowerBound(endMs + 1))
    }
    fun atOrBefore(timeMs: Long): PitchFrame? = getOrNull(lowerBound(timeMs + 1) - 1)
    companion object {
        private const val CHUNK_SIZE = 256
        val Empty = PitchHistory(emptyList(), emptyList())
        fun fromFrames(frames: List<PitchFrame>): PitchHistory {
            require(frames.zipWithNext().all { (a, b) -> a.timeMs < b.timeMs })
            val parts = frames.chunked(CHUNK_SIZE)
            return if (parts.isEmpty()) Empty else PitchHistory(parts.dropLast(1), parts.last())
        }
    }
}
