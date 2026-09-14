package com.aria.rythme.feature.pitch.data

import android.content.Context
import com.aria.rythme.core.music.data.model.Song
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.security.MessageDigest

internal data class MelodyAnalysisResult(val curve: MelodyCurve, val cached: Boolean, val key: String)

class SongMelodyAnalyzer(private val context: Context, private val models: MelodyModelStore, private val cache: MelodyCurveCache) {
    internal suspend fun legacyCached(song: Song): MelodyAnalysisResult? = withContext(Dispatchers.IO) {
        if (cache.keys().isEmpty()) return@withContext null
        val hash = MessageDigest.getInstance("SHA-256")
        context.contentResolver.openInputStream(song.uri)?.use { input ->
            val buffer = ByteArray(65536)
            while (true) { currentCoroutineContext().ensureActive(); val n = input.read(buffer); if (n < 0) break; hash.update(buffer, 0, n) }
        } ?: return@withContext null
        val sourceHash = hash.digest().joinToString("") { "%02x".format(it) }
        val key = MessageDigest.getInstance("SHA-256").digest(
            "$sourceHash|${models.spec.sha256}|stream-sinc32-mel-htk512-context64-v1".toByteArray()
        ).joinToString("") { "%02x".format(it) }
        cache.read(key)?.let { MelodyAnalysisResult(it, true, key) }
    }

    internal suspend fun analyze(song: Song, progress: (Float, String) -> Unit): MelodyAnalysisResult = withContext(Dispatchers.IO) {
        if (song.duration > 3_600_000L) throw IOException("首版暂支持 60 分钟以内的歌曲")
        val coroutine = currentCoroutineContext()
        val check = { coroutine.ensureActive() }
        val root = File(context.cacheDir, "melody-analysis-work").apply { mkdirs() }
        // 单任务入口保证没有并发分析；清理进程中断后遗留的本功能临时数据。
        root.listFiles()?.forEach { it.deleteRecursively() }
        val work = File(root, "current").apply { mkdirs() }
        try {
            val source = File(work, "source.audio")
            val digest = MessageDigest.getInstance("SHA-256")
            progress(0f, "读取歌曲")
            context.contentResolver.openInputStream(song.uri)?.use { input ->
                source.outputStream().buffered().use { output ->
                    val bytes = ByteArray(64 * 1024); var copied = 0L
                    while (true) {
                        check(); val n = input.read(bytes); if (n < 0) break
                        copied += n
                        if (copied > 1_000_000_000L || work.usableSpace < 16L * 1024 * 1024) throw IOException("歌曲过大或临时存储空间不足")
                        digest.update(bytes, 0, n); output.write(bytes, 0, n)
                    }
                }
            } ?: throw IOException("无法读取当前歌曲，请检查文件是否仍存在及媒体权限")
            val sourceHash = digest.digest().joinToString("") { "%02x".format(it) }
            val key = MessageDigest.getInstance("SHA-256").digest(
                "$sourceHash|${models.spec.sha256}|stream-sinc32-mel-htk512-context64-v1".toByteArray()
            ).joinToString("") { "%02x".format(it) }
            cache.read(key)?.let { return@withContext MelodyAnalysisResult(it, true, key) }
            val raw = File(work, "decoded.f32")
            val (rate, count) = SongAudioDecoder.decode(source, raw, check) { ms ->
                progress(.03f + .12f * (ms.toFloat() / song.duration.coerceAtLeast(1)).coerceIn(0f, 1f), "解码歌曲")
            }
            if (count * 1000L / rate > 3_600_000L) throw IOException("首版暂支持 60 分钟以内的歌曲")
            source.delete()
            val pcm = File(work, "mono16k.f32")
            val samples = SongAudioDecoder.resample(raw, pcm, rate, count, check) { progress(.15f + .1f * it, "准备音频") }
            raw.delete()
            if (samples < 160) throw IOException("音频过短，无法生成参考旋律")
            val frames = samples / 160 + 1
            val hz = FloatArray(frames); val confidence = FloatArray(frames)
            models.withInstalledFile { model ->
                progress(.25f, "加载模型")
                val env = OrtEnvironment.getEnvironment()
                OrtSession.SessionOptions().use { options ->
                    options.setIntraOpNumThreads(2); options.setInterOpNumThreads(1)
                    env.createSession(model.path, options).use { session ->
                        val inputName = session.inputNames.single()
                        RandomAccessFile(pcm, "r").use { audio ->
                            val inferenceStarted = System.nanoTime()
                            for (start in 0 until frames step 384) {
                                check()
                                val begin = start - 64
                                val first = ((begin * 160) - 512).coerceAtLeast(0)
                                val end = ((begin + 512) * 160 + 512).coerceAtMost(samples)
                                val segment = SongAudioDecoder.readFloats(audio, first, end - first)
                                val mel = MelodyDsp.melFrames(segment, first, samples, begin, 512, Runnable(check))
                                OnnxTensor.createTensor(env, arrayOf(mel)).use { tensor ->
                                    session.run(mapOf(inputName to tensor)).use { result ->
                                        @Suppress("UNCHECKED_CAST")
                                        val scores = (result[0].value as Array<Array<FloatArray>>)[0]
                                        for (j in 0 until minOf(384, frames - start)) {
                                            val (frequency, certainty) = decodeMelodyScores(scores[j + 64])
                                            hz[start + j] = frequency; confidence[start + j] = certainty
                                        }
                                    }
                                }
                                check()
                                val done = minOf(start + 384, frames)
                                val elapsed = (System.nanoTime() - inferenceStarted) / 1e9
                                val eta = if (done >= 1152) (elapsed / done * (frames - done)).toInt() else -1
                                progress(.25f + .74f * done / frames, if (eta >= 0) "提取参考旋律 · 约剩 ${eta} 秒" else "提取参考旋律 · 正在估算剩余时间")
                            }
                        }
                    }
                }
            }
            check()
            val curve = MelodyCurve(0, samples * 1000L / 16000, hz, confidence)
            cache.write(key, curve)
            MelodyAnalysisResult(curve, false, key)
        } finally { work.deleteRecursively() }
    }
}

internal fun decodeMelodyScores(scores: FloatArray): Pair<Float, Float> {
    // CPU Sigmoid 优化可能在 0/1 边界产生极小浮点误差；只容忍数值舍入，不接受异常输出。
    require(scores.size == 360 && scores.all { it.isFinite() && it in -.0001f..1.0001f }) {
        "模型输出无效（维度 ${scores.size}，范围 ${scores.minOrNull()} 至 ${scores.maxOrNull()}）"
    }
    val peak = scores.indices.maxBy { scores[it] }
    val certainty = scores[peak].coerceIn(0f, 1f)
    if (certainty < .03f) return 0f to certainty
    var sum = 0.0; var weight = 0.0
    for (k in maxOf(0, peak - 4)..minOf(359, peak + 4)) {
        val score = scores[k].coerceIn(0f, 1f)
        sum += score * (20 * k + 1997.3794084376191); weight += score
    }
    val hz = (10 * Math.pow(2.0, sum / weight / 1200)).toFloat()
    require(hz.isFinite() && hz in 0f..2500f)
    return hz to certainty
}
