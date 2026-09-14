package com.aria.rythme.feature.pitch.data

import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/** 分块解码到临时单声道文件；长歌不会生成整首原采样率的内存数组。 */
internal object SongAudioDecoder {
    fun decode(source: File, output: File, check: () -> Unit, progress: (Long) -> Unit): Pair<Int, Int> {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(source.path)
            val track = (0 until extractor.trackCount).firstOrNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: throw IOException("歌曲中没有可解码的音轨")
            val format = extractor.getTrackFormat(track)
            extractor.selectTrack(track)
            val decoder = MediaCodec.createDecoderByType(requireNotNull(format.getString(MediaFormat.KEY_MIME)))
            codec = decoder
            decoder.configure(format, null, null, 0); decoder.start()
            var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var encoding = AudioFormat.ENCODING_PCM_16BIT
            var samples = 0; var inputDone = false; var outputDone = false
            var lastOutput = System.nanoTime()
            val info = MediaCodec.BufferInfo()
            output.outputStream().buffered().use { out ->
                while (!outputDone) {
                    check()
                    if (System.nanoTime() - lastOutput > 30_000_000_000L) throw IOException("音频解码超时")
                    if (!inputDone) {
                        val index = decoder.dequeueInputBuffer(1000)
                        if (index >= 0) {
                            val input = requireNotNull(decoder.getInputBuffer(index))
                            val count = extractor.readSampleData(input, 0)
                            if (count < 0) { decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM); inputDone = true }
                            else { decoder.queueInputBuffer(index, 0, count, extractor.sampleTime, 0); extractor.advance() }
                        }
                    }
                    val index = decoder.dequeueOutputBuffer(info, 1000)
                    if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        val f = decoder.outputFormat
                        val newRate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        if (samples > 0 && rate != newRate) throw IOException("暂不支持中途改变采样率的音频")
                        rate = newRate; channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        encoding = if (f.containsKey(MediaFormat.KEY_PCM_ENCODING)) f.getInteger(MediaFormat.KEY_PCM_ENCODING) else AudioFormat.ENCODING_PCM_16BIT
                        require(rate in 8000..384000 && channels in 1..32)
                        if (encoding !in listOf(AudioFormat.ENCODING_PCM_16BIT, AudioFormat.ENCODING_PCM_FLOAT)) throw IOException("暂不支持此 PCM 格式")
                    } else if (index >= 0) {
                        try {
                            val b = requireNotNull(decoder.getOutputBuffer(index)).order(ByteOrder.LITTLE_ENDIAN)
                            b.position(info.offset); b.limit(info.offset + info.size)
                            val sampleBytes = if (encoding == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
                            val mono = ByteBuffer.allocate(b.remaining() / (sampleBytes * channels) * 4).order(ByteOrder.LITTLE_ENDIAN)
                            while (b.remaining() >= sampleBytes * channels) {
                                var sum = 0.0
                                repeat(channels) { sum += if (sampleBytes == 4) b.float.toDouble() else b.short / 32768.0 }
                                val sample = (sum / channels).toFloat()
                                if (!sample.isFinite()) throw IOException("音频包含无效采样")
                                mono.putFloat(sample); samples++
                            }
                            if (samples.toLong() * 4 > 2_000_000_000L || output.parentFile!!.usableSpace < 16L * 1024 * 1024) throw IOException("分析所需临时空间不足或音频过长")
                            out.write(mono.array())
                            progress(samples * 1000L / rate)
                            lastOutput = System.nanoTime()
                            outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        } finally { decoder.releaseOutputBuffer(index, false) }
                    }
                }
            }
            if (samples == 0) throw IOException("歌曲没有有效音频采样")
            return rate to samples
        } finally { codec?.release(); extractor.release() }
    }

    fun resample(source: File, output: File, rate: Int, count: Int, check: () -> Unit, progress: (Float) -> Unit): Int {
        val total = (count * 16000.0 / rate).roundToLong()
        if (total !in 1..Int.MAX_VALUE.toLong()) throw IOException("音频时长超出支持范围")
        val cutoff = min(1.0, 16000.0 / rate) * .94
        fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
        val phases = 16000 / gcd(rate, 16000)
        // 有理采样率的滤波系数按相位复用，避免整曲逐采样重复计算三角函数。
        val kernels = Array(phases) { phase ->
            val fraction = (phase.toLong() * rate % 16000) / 16000.0
            DoubleArray(64) { index ->
                val d = fraction - (index - 31)
                val x = Math.PI * cutoff * d
                if (abs(d) > 32) 0.0 else cutoff * (if (abs(x) < 1e-12) 1.0 else sin(x) / x) * (.5 + .5 * cos(Math.PI * d / 32))
            }
        }
        RandomAccessFile(source, "r").use { input ->
            output.outputStream().buffered().use { out ->
                var start = 0
                while (start < total) {
                    check()
                    val length = min(8192, total.toInt() - start)
                    val first = max(0, floor(start.toDouble() * rate / 16000).toInt() - 32)
                    val last = min(count, ceil((start.toDouble() + length) * rate / 16000).toInt() + 33)
                    val samples = readFloats(input, first, last - first)
                    val buffer = ByteBuffer.allocate(length * 4).order(ByteOrder.LITTLE_ENDIAN)
                    for (i in start until start + length) {
                        val center = (i.toLong() * rate / 16000).toInt()
                        val kernel = kernels[i % phases]
                        var sum = 0.0; var weight = 0.0
                        if (rate == 16000) { buffer.putFloat(samples[i - first]); continue }
                        for (j in center - 31..center + 32) {
                            if (j < 0 || j >= count) continue
                            val w = kernel[j - center + 31]
                            sum += samples[j - first] * w; weight += w
                        }
                        val value = (sum / weight).toFloat()
                        if (!value.isFinite()) throw IOException("重采样产生无效数据")
                        buffer.putFloat(value)
                    }
                    out.write(buffer.array()); start += length
                    progress(start.toFloat() / total)
                }
            }
        }
        return total.toInt()
    }

    fun readFloats(input: RandomAccessFile, start: Int, count: Int): FloatArray {
        val bytes = ByteArray(count * 4)
        input.seek(start.toLong() * 4); input.readFully(bytes)
        return FloatArray(count).also { ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(it) }
    }
}
