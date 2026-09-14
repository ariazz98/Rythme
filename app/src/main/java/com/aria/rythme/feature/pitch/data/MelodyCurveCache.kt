package com.aria.rythme.feature.pitch.data

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

data class MelodyCurve(val startMs: Long, val endMs: Long, val hz: FloatArray, val confidence: FloatArray) {
    val reliableCount get() = confidence.indices.count { confidence[it] >= DISPLAY_THRESHOLD && hz[it] > 0 }
    companion object { const val DISPLAY_THRESHOLD = 0.5f }
}

/** 只保存派生曲线，文件损坏时重新生成。模型与曲线可分别删除。 */
class MelodyCurveCache(private val directory: File, private val maxBytes: Long = MAX_BYTES) {
    fun keys(): Set<String> = directory.listFiles().orEmpty().filter { it.extension == "curve" }.map { it.nameWithoutExtension }.toSet()
    fun bytes(): Long = directory.listFiles()?.filter { it.extension == "curve" }?.sumOf { it.length() } ?: 0
    fun clear() { directory.listFiles()?.filter { it.isFile }?.forEach { check(it.delete()) { "无法清理曲线缓存" } } }
    fun read(key: String): MelodyCurve? {
        val file = file(key)
        if (!file.exists()) return null
        return try {
            DataInputStream(file.inputStream().buffered()).use { input ->
                require(input.readInt() == MAGIC)
                val start = input.readLong(); val end = input.readLong(); val count = input.readInt()
                require(start >= 0 && end > start && end - start <= 3_600_000 && count in 1..360001)
                require(count == ((end - start) / 10).toInt() + 1)
                val hz = FloatArray(count); val confidence = FloatArray(count)
                repeat(count) { i ->
                    hz[i] = input.readFloat(); confidence[i] = input.readFloat()
                    require(hz[i].isFinite() && hz[i] in 0f..2500f && confidence[i].isFinite() && confidence[i] in 0f..1f)
                }
                require(input.read() == -1)
                file.setLastModified(System.currentTimeMillis())
                MelodyCurve(start, end, hz, confidence)
            }
        } catch (_: Exception) { file.delete(); null }
    }

    fun write(key: String, curve: MelodyCurve) {
        directory.mkdirs()
        val target = file(key); val temporary = File(directory, "$key.tmp")
        try {
            DataOutputStream(temporary.outputStream().buffered()).use { out ->
                out.writeInt(MAGIC); out.writeLong(curve.startMs); out.writeLong(curve.endMs); out.writeInt(curve.hz.size)
                curve.hz.indices.forEach { out.writeFloat(curve.hz[it]); out.writeFloat(curve.confidence[it]) }
            }
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            var total = bytes()
            directory.listFiles()?.filter { it.extension == "curve" && it != target }?.sortedBy { it.lastModified() }?.forEach {
                if (total > maxBytes) { val length = it.length(); if (it.delete()) total -= length }
            }
        } finally { temporary.delete() }
    }

    private fun file(key: String): File { require(key.matches(Regex("[a-f0-9]{64}"))); return File(directory, "$key.curve") }
    companion object { private const val MAGIC = 0x524D4332; const val MAX_BYTES = 64L * 1024 * 1024 }
}
