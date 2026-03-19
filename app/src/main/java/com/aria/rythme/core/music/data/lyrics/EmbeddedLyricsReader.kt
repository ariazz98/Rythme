package com.aria.rythme.core.music.data.lyrics

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Metadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.inspector.MetadataRetriever
import com.aria.rythme.core.music.data.model.LyricsData
import com.aria.rythme.core.music.data.model.LyricsSource
import com.aria.rythme.core.music.data.model.LyricsType
import com.aria.rythme.core.utils.RythmeLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 从音频文件内嵌标签读取歌词
 *
 * 支持 ID3v2 USLT（Unsynchronised Lyrics）和 Vorbis Comment LYRICS 字段。
 */
@UnstableApi
class EmbeddedLyricsReader(private val context: Context) {

    /**
     * 从音频文件 URI 读取内嵌歌词
     *
     * @return LyricsData 或 null
     */
    suspend fun read(uri: Uri): LyricsData? = withContext(Dispatchers.IO) {
        try {
            val mediaItem = MediaItem.fromUri(uri)
            val retriever = MetadataRetriever.Builder(context, mediaItem).build()
            val trackGroups = retriever.retrieveTrackGroups().get()

            for (i in 0 until trackGroups.length) {
                val trackGroup = trackGroups[i]
                for (j in 0 until trackGroup.length) {
                    val format = trackGroup.getFormat(j)
                    val metadata = format.metadata ?: continue
                    val lyrics = extractLyricsFromMetadata(metadata)
                    if (lyrics != null) return@withContext lyrics
                }
            }
            null
        } catch (e: Exception) {
            RythmeLogger.e(TAG, "读取内嵌歌词失败", e)
            null
        }
    }

    private fun extractLyricsFromMetadata(metadata: Metadata): LyricsData? {
        for (i in 0 until metadata.length()) {
            val entry = metadata.get(i)
            if (entry is androidx.media3.extractor.metadata.id3.TextInformationFrame) {
                continue
            }
            val text = when (entry) {
                is androidx.media3.extractor.metadata.id3.BinaryFrame -> {
                    if (entry.id == "USLT") {
                        String(entry.data, Charsets.UTF_8)
                    } else null
                }
                is androidx.media3.extractor.metadata.vorbis.VorbisComment -> {
                    if (entry.key.equals("LYRICS", ignoreCase = true) ||
                        entry.key.equals("UNSYNCEDLYRICS", ignoreCase = true) ||
                        entry.key.equals("SYNCEDLYRICS", ignoreCase = true)
                    ) {
                        entry.value
                    } else null
                }
                else -> null
            }

            if (!text.isNullOrBlank()) {
                val parsed = LrcParser.parse(text, LyricsSource.EMBEDDED)
                if (parsed != null) return parsed
                return LyricsData(
                    lines = emptyList(),
                    type = LyricsType.PLAIN,
                    source = LyricsSource.EMBEDDED,
                    plainText = text
                )
            }
        }
        return null
    }

    companion object {
        private const val TAG = "EmbeddedLyricsReader"
    }
}
