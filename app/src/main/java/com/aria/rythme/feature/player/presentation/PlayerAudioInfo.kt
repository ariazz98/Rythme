package com.aria.rythme.feature.player.presentation

import android.media.MediaExtractor
import android.media.MediaFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aria.rythme.core.music.data.model.Song
import com.kyant.capsule.ContinuousRoundedRectangle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal fun playerCodecLabel(mime: String): String = when (mime.lowercase()) {
    "audio/flac", "audio/x-flac" -> "FLAC"
    "audio/alac", "audio/x-alac" -> "ALAC"
    "audio/raw" -> "PCM"
    "audio/mpeg" -> "MP3"
    "audio/mp4a-latm", "audio/aac" -> "AAC"
    "audio/opus" -> "Opus"
    "audio/vorbis" -> "Vorbis"
    else -> "未知编码"
}

private data class AudioInfo(val mime: String, val sampleRate: Int?, val channels: Int?, val bitrate: Int?)

/** 只依据音轨编码标识无损，不把 M4A/WAV 等容器名称当成编码结论。 */
@Composable
internal fun PlayerAudioInfoBadge(song: Song) {
    val context = LocalContext.current
    val info by produceState<AudioInfo?>(null, song.uri) {
        value = null
        value = withContext(Dispatchers.IO) {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(context, song.uri, null)
                val format = (0 until extractor.trackCount).map(extractor::getTrackFormat)
                    .firstOrNull { it.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true }
                fun number(key: String): Int? = format?.let {
                    if (it.containsKey(key)) it.getInteger(key).takeIf { n -> n > 0 } else null
                }
                format?.let { AudioInfo(it.getString(MediaFormat.KEY_MIME).orEmpty(),
                    number(MediaFormat.KEY_SAMPLE_RATE), number(MediaFormat.KEY_CHANNEL_COUNT), number(MediaFormat.KEY_BIT_RATE)) }
            } catch (_: Exception) {
                null
            } finally {
                extractor.release()
            }
        }
    }
    var expanded by remember(song.uri) { mutableStateOf(false) }
    val codec = info?.let { playerCodecLabel(it.mime) }
    val label = if (codec == "FLAC" || codec == "ALAC") "无损" else codec ?: "音质信息"
    Box(Modifier.clip(ContinuousRoundedRectangle(3.dp))
        .background(Color.White.copy(alpha = .08f))
        .clickable { expanded = true }
        .padding(horizontal = 5.dp, vertical = 1.dp)) {
        Text(label, color = Color.White.copy(alpha = .45f), fontSize = 9.sp, fontWeight = FontWeight.Medium)
    }
    if (expanded) {
        val detail = buildString {
            append("编码：").append(codec ?: "未能读取").append('\n')
            append("采样率：").append(info?.sampleRate?.let { "$it Hz" } ?: "未知").append('\n')
            append("声道数：").append(info?.channels ?: "未知").append('\n')
            val bitrate = info?.bitrate ?: song.bitrate.takeIf { it > 0 }
            append("码率：").append(bitrate?.let { "${it / 1000} kbps" } ?: "未知")
            if (info?.bitrate == null && bitrate != null) append("（媒体库记录）")
            append("\n\n这是本地文件的信息，不代表当前输出设备或蓝牙传输的音质。")
        }
        AlertDialog(
            onDismissRequest = { expanded = false },
            title = { Text("音频信息") }, text = { Text(detail) },
            confirmButton = { TextButton(onClick = { expanded = false }) { Text("关闭") } }
        )
    }
}
