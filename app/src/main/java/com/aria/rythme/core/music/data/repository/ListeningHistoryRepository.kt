package com.aria.rythme.core.music.data.repository

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/** 收听来源不是导航路由；不改变队列身份或播放器的会话历史。 */
data class ListeningOrigin(
    val kind: String = "songs",
    val id: Long = 0,
    val artistId: Long? = null,
    val composer: String? = null,
    val genre: String? = null
)

data class ListeningRecord(val songId: Long, val origin: ListeningOrigin, val playedAt: Long)
/** 分区与原始顺序按入队位置保存，同曲重复入队也能分别恢复。 */
data class ResumeQueueState(
    val orderedCount: Int,
    val sourceOrder: List<Int>,
    val shuffle: Boolean = false,
    val repeat: String = "OFF",
    val autoplay: Boolean = false,
    val crossfade: Boolean = false
)
data class ResumePoint(
    val songId: Long,
    val positionMs: Long,
    val queueIds: List<Long>,
    val queueIndex: Int,
    val origin: ListeningOrigin,
    val queueState: ResumeQueueState? = null
)

internal fun ResumePoint.withAvailableSongs(available: Set<Long>): ResumePoint? {
    if (queueIds.getOrNull(queueIndex) != songId || songId !in available) return null
    val kept = queueIds.indices.filter { queueIds[it] in available }
    val positions = kept.withIndex().associate { it.value to it.index }
    val metadata = queueState?.let { saved ->
        val boundary = saved.orderedCount.coerceIn(0, queueIds.size)
        val orderedIndices = (0 until boundary).toList()
        val source = saved.sourceOrder.takeIf { it.sorted() == orderedIndices } ?: orderedIndices
        saved.copy(orderedCount = kept.count { it < boundary },
            sourceOrder = source.mapNotNull(positions::get))
    }
    return copy(queueIds = kept.map(queueIds::get), queueIndex = positions.getValue(queueIndex),
        queueState = metadata,
        // 旧快照没有分区信息，保留歌曲，但不能继续声称全部来自旧专辑。
        origin = if (metadata == null) ListeningOrigin() else origin)
}
data class ListeningHistory(val recent: List<ListeningRecord> = emptyList(), val resume: ResumePoint? = null)

internal fun rememberListening(recent: List<ListeningRecord>, record: ListeningRecord): List<ListeningRecord> =
    (listOf(record) + recent.filterNot { it.songId == record.songId }).take(50)

private val Context.listeningDataStore by preferencesDataStore("listening_history")

/** 单独保存小型首页记录，不通过升级音乐数据库清空现有曲库、歌单或用户编辑。 */
class ListeningHistoryRepository(context: Context) {
    private val store = context.applicationContext.listeningDataStore
    private val key = stringPreferencesKey("history_v1")
    val history = store.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }.map { decode(it[key]) }

    suspend fun record(point: ResumePoint, newListen: Boolean) {
        store.edit { prefs ->
            val old = decode(prefs[key])
            val recent = if (newListen) rememberListening(old.recent,
                ListeningRecord(point.songId, point.origin, System.currentTimeMillis())) else old.recent
            prefs[key] = encode(ListeningHistory(recent, point))
        }
    }

    private fun origin(json: JSONObject) = ListeningOrigin(
        kind = json.optString("kind", "songs"), id = json.optLong("id"),
        artistId = if (json.has("artist")) json.getLong("artist") else null,
        composer = if (json.has("composer")) json.getString("composer") else null,
        genre = if (json.has("genre")) json.getString("genre") else null
    )

    private fun ListeningOrigin.json() = JSONObject().put("kind", kind).put("id", id)
        .apply { artistId?.let { put("artist", it) }; composer?.let { put("composer", it) }; genre?.let { put("genre", it) } }

    private fun encode(history: ListeningHistory): String = JSONObject().apply {
        put("recent", JSONArray().apply { history.recent.forEach { record ->
            put(JSONObject().put("song", record.songId).put("origin", record.origin.json()).put("time", record.playedAt))
        } })
        history.resume?.let { point -> put("resume", JSONObject().put("song", point.songId)
            .put("position", point.positionMs.coerceAtLeast(0)).put("queue", JSONArray(point.queueIds))
            .put("index", point.queueIndex).put("origin", point.origin.json())
            .apply { point.queueState?.let { state ->
                put("queueState", JSONObject().put("orderedCount", state.orderedCount)
                    .put("sourceOrder", JSONArray(state.sourceOrder))
                    .put("shuffle", state.shuffle).put("repeat", state.repeat)
                    .put("autoplay", state.autoplay).put("crossfade", state.crossfade))
            } }) }
    }.toString()

    private fun decode(value: String?): ListeningHistory {
        if (value == null) return ListeningHistory()
        return try {
            val json = JSONObject(value)
            val recent = json.optJSONArray("recent") ?: JSONArray()
            ListeningHistory(
                recent = List(recent.length()) { index -> recent.getJSONObject(index).let {
                    ListeningRecord(it.getLong("song"), origin(it.getJSONObject("origin")), it.getLong("time"))
                } }.take(50),
                resume = json.optJSONObject("resume")?.let { point ->
                    val queue = point.getJSONArray("queue")
                    ResumePoint(point.getLong("song"), point.optLong("position").coerceAtLeast(0),
                        List(queue.length()) { queue.getLong(it) }, point.optInt("index"), origin(point.getJSONObject("origin")),
                        point.optJSONObject("queueState")?.let { state ->
                            val source = state.optJSONArray("sourceOrder") ?: JSONArray()
                            ResumeQueueState(state.getInt("orderedCount"),
                                List(source.length()) { source.getInt(it) },
                                state.optBoolean("shuffle"), state.optString("repeat", "OFF"),
                                state.optBoolean("autoplay"), state.optBoolean("crossfade"))
                        })
                }
            )
        } catch (_: org.json.JSONException) { ListeningHistory() }
    }
}
