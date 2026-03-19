package com.aria.rythme.core.music.data.lyrics

import com.aria.rythme.core.music.data.model.LyricsData
import com.aria.rythme.core.music.data.model.LyricsSource
import com.aria.rythme.core.music.data.model.LyricsType
import com.aria.rythme.core.music.data.model.Song
import com.aria.rythme.core.utils.RythmeLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * LRCLIB HTTP 客户端
 *
 * 从 https://lrclib.net 获取歌词。
 * OkHttp 由 coil-network-okhttp 传递依赖提供。
 */
object LrclibApi {

    private const val TAG = "LrclibApi"
    private const val BASE_URL = "https://lrclib.net/api"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /**
     * 从 LRCLIB 获取歌词
     *
     * 先尝试 /get 精确匹配（track_name + artist_name + duration），
     * 失败后回退到 /search 关键字搜索。
     */
    suspend fun fetch(song: Song): LyricsData? = withContext(Dispatchers.IO) {
        // 精确匹配
        val exact = fetchExact(song)
        if (exact != null) return@withContext exact

        // 关键字搜索
        fetchSearch(song)
    }

    private fun fetchExact(song: Song): LyricsData? {
        try {
            val durationSec = song.duration / 1000
            val url = "$BASE_URL/get?" +
                    "track_name=${encode(song.title)}" +
                    "&artist_name=${encode(song.artist)}" +
                    "&duration=$durationSec"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Rythme 1.0 (https://github.com/ariazz98/Rythme)")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            return parseLrclibResponse(body)
        } catch (e: Exception) {
            RythmeLogger.e(TAG, "精确匹配失败", e)
            return null
        }
    }

    private fun fetchSearch(song: Song): LyricsData? {
        try {
            val query = song.title
            val url = "$BASE_URL/search?q=${encode(query)}"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Rythme 1.0 (https://github.com/ariazz98/Rythme)")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            val array = org.json.JSONArray(body)
            if (array.length() == 0) return null

            // 取第一个结果
            return parseLrclibResponse(array.getJSONObject(0).toString())
        } catch (e: Exception) {
            RythmeLogger.e(TAG, "搜索失败", e)
            return null
        }
    }

    private fun parseLrclibResponse(json: String): LyricsData? {
        try {
            val obj = JSONObject(json)

            // 优先使用同步歌词
            val syncedLyrics = obj.optString("syncedLyrics", "")
            if (syncedLyrics.isNotBlank()) {
                val parsed = LrcParser.parse(syncedLyrics, LyricsSource.ONLINE)
                if (parsed != null) return parsed
            }

            // 回退到纯文本歌词
            val plainLyrics = obj.optString("plainLyrics", "")
            if (plainLyrics.isNotBlank()) {
                return LyricsData(
                    lines = emptyList(),
                    type = LyricsType.PLAIN,
                    source = LyricsSource.ONLINE,
                    plainText = plainLyrics
                )
            }

            return null
        } catch (e: Exception) {
            RythmeLogger.e(TAG, "解析响应失败", e)
            return null
        }
    }

    private fun encode(s: String): String = URLEncoder.encode(s, "UTF-8")
}
