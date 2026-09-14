package com.aria.rythme.feature.pitch.data

import android.content.Context
import com.aria.rythme.core.music.data.model.Song
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext

data class SongMelodyState(
    val song: Song? = null,
    val busy: Boolean = false,
    val progress: Float = 0f,
    val stage: String = "",
    val curve: MelodyCurve? = null,
    val history: PitchHistory = PitchHistory.Empty,
    val error: String? = null,
    val cached: Boolean = false
)

class SongMelodyManager(private val context: Context, private val analyzer: SongMelodyAnalyzer, private val cache: MelodyCurveCache, private val library: ParsedMelodyStore) {
    private val mutable = MutableStateFlow(SongMelodyState())
    val state = mutable.asStateFlow()
    private val estimates = context.getSharedPreferences("melody-analysis-timing", Context.MODE_PRIVATE)
    fun estimateSeconds(duration: Long): Int? {
        val ratio = estimates.getFloat("seconds_per_audio_second", 0f)
        return if (ratio > 0) (duration / 1000.0 * ratio).toInt().coerceAtLeast(1) else null
    }
    fun prepare(song: Song): Boolean {
        if (mutable.value.busy) return false
        mutable.value = SongMelodyState(song = song, busy = true, stage = "准备分析")
        return true
    }
    fun failed(message: String) { mutable.update { it.copy(busy = false, error = message) } }
    fun cancelled() { mutable.update { if (it.busy) it.copy(busy = false, stage = "已取消") else it } }
    suspend fun run() {
        val song = mutable.value.song ?: return
        val started = System.nanoTime()
        try {
            val result = analyzer.analyze(song) { progress, stage -> mutable.update { it.copy(progress = progress, stage = stage) } }
            library.save(song.parsedEntry(result.key, result.curve.endMs), result.curve)
            val history = withContext(Dispatchers.Default) {
                PitchHistory.fromFrames(result.curve.hz.indices.map { i ->
                    val pitch = if (result.curve.confidence[i] >= MelodyCurve.DISPLAY_THRESHOLD && result.curve.hz[i] > 0)
                        DetectedPitch(result.curve.hz[i], result.curve.confidence[i]) else null
                    PitchFrame(i * 10L, pitch)
                })
            }
            if (!result.cached) estimates.edit().putFloat("seconds_per_audio_second",
                ((System.nanoTime() - started) / 1e9 / (result.curve.endMs / 1000.0)).toFloat()).apply()
            android.util.Log.i("SongMelody", "complete cached=${result.cached} elapsedMs=${(System.nanoTime() - started) / 1_000_000} frames=${result.curve.hz.size}")
            mutable.value = SongMelodyState(song = song, curve = result.curve, history = history, progress = 1f, cached = result.cached)
        } catch (error: CancellationException) { cancelled(); throw error }
        catch (error: Exception) { failed(error.message ?: "无法分析歌曲，请稍后重试") }
        catch (link: LinkageError) { failed("当前设备无法加载音高分析运行库，请检查应用安装是否完整") }
        catch (memory: OutOfMemoryError) { failed("设备可用内存不足，请关闭其他应用后重试") }
    }
    /** 旧版只有内容哈希缓存，首次打开列表时匹配本地歌曲；只读取缓存，不触发模型推理。 */
    suspend fun importLegacy(songs: List<Song>) {
        library.refresh()
        val missing = cache.keys() - library.entries.value.map { it.key }.toSet() - library.ignoredLegacyKeys()
        if (missing.isEmpty() || mutable.value.busy) return
        val remaining = missing.toMutableSet()
        val durations = withContext(Dispatchers.IO) { missing.mapNotNull { cache.read(it)?.endMs } }
        // 旧缓存没有歌曲索引，先用时长筛选，避免对整个大型资料库逐文件计算哈希。
        for (song in songs.filter { song -> durations.any { kotlin.math.abs(it - song.duration) <= 2_000 } }) {
            if (remaining.isEmpty()) break
            if (library.entries.value.any { it.uri == song.uri.toString() }) continue
            try {
                analyzer.legacyCached(song)?.let { result ->
                    if (remaining.remove(result.key)) library.save(song.parsedEntry(result.key, result.curve.endMs), result.curve)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: java.io.IOException) { /* 不可读取的原曲不影响其他已解析曲目。 */ }
            catch (_: SecurityException) { }
        }
    }
    suspend fun cacheBytes(): Long = withContext(Dispatchers.IO) { cache.bytes() }
    suspend fun clearCache() = withContext(Dispatchers.IO) {
        if (!mutable.value.busy) { cache.clear(); mutable.value = SongMelodyState() }
    }
}

private fun Song.parsedEntry(key: String, endMs: Long) = ParsedMelodyEntry(
    key, id, title, artist, uri.toString(), coverUri?.toString().orEmpty(), endMs, dateModified, size)
