package com.aria.rythme.feature.pitch.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

data class MelodyModelSpec(val id: String, val url: String, val bytes: Long, val sha256: String) {
    init { require(id.matches(Regex("[a-z0-9-]+"))); require(bytes > 0) }
    companion object {
        val Recommended = MelodyModelSpec(
            "rmvpe-rvc-v1",
            "https://huggingface.co/lj1995/VoiceConversionWebUI/resolve/e6d0c1a17da07c33557852f9dfa2bd44cc75737d/rmvpe.onnx",
            361_688_443,
            "5370e71ac80af8b4b7c793d27efd51fd8bf962de3a7ede0766dac0befa3660fd"
        )
    }
}

data class MelodyModelStatus(val installed: Boolean = false, val downloadedBytes: Long = 0)

/** 文件位于 noBackupFilesDir；不在构造、查询或应用启动时联网/加载推理模型。 */
class MelodyModelStore(
    private val directory: File,
    client: OkHttpClient,
    val spec: MelodyModelSpec = MelodyModelSpec.Recommended
) {
    private val client = client.newBuilder().readTimeout(30, TimeUnit.SECONDS).build()
    private val lock = Mutex()
    private val model = File(directory, "${spec.id}.onnx")
    private val partial = File(directory, "${spec.id}.part")
    @Volatile private var call: Call? = null
    @Volatile private var importing: InputStream? = null
    private var verified: Pair<Long, Long>? = null

    fun cancelTransfer() { call?.cancel(); runCatching { importing?.close() } }

    suspend fun status(): MelodyModelStatus = lock.withLock {
        withContext(Dispatchers.IO) {
            // 导入不做断点续传；清理上次进程中断留下的专用临时文件。
            File(directory, "${spec.id}.import").delete()
            val valid = verifyInstalled()
            MelodyModelStatus(valid, if (valid) spec.bytes else partial.length().coerceAtMost(spec.bytes))
        }
    }

    suspend fun <T> withInstalledFile(block: suspend (File) -> T): T = lock.withLock {
        withContext(Dispatchers.IO) {
            if (!verifyInstalled()) throw IOException("请先配置兼容的歌曲对照模型")
            block(model)
        }
    }

    suspend fun installedFile(): File = lock.withLock {
        withContext(Dispatchers.IO) {
            if (!verifyInstalled()) throw IOException("模型尚未下载，或文件校验未通过")
            model
        }
    }

    /** 只有明确点击下载才调用。暂停保留分片；内容总长度与 SHA-256 均正确才发布。 */
    suspend fun download(networkAllowed: () -> Boolean, progress: (Long, Boolean) -> Unit) = lock.withLock {
        withContext(Dispatchers.IO) {
            directory.mkdirs()
            if (verifyInstalled()) return@withContext
            if (partial.length() > spec.bytes) partial.delete()
            if (partial.length() < spec.bytes) {
                if (!networkAllowed()) throw IOException("下载已暂停：请连接允许使用的网络")
                val offset = partial.length()
                if (directory.usableSpace < spec.bytes - offset + 16L * 1024 * 1024) {
                    throw IOException("存储空间不足，请释放空间后重试")
                }
                val request = Request.Builder().url(spec.url).header("Accept-Encoding", "identity")
                    .apply { if (offset > 0) header("Range", "bytes=$offset-") }.build()
                val transfer = client.newCall(request)
                call = transfer
                try {
                    currentCoroutineContext().ensureActive()
                    transfer.execute().use { response ->
                        val append = when (response.code) {
                            200 -> false // 服务端忽略 Range 时重写，不拼接重复内容。
                            206 -> {
                                validateRange(response.header("Content-Range"), offset, spec.bytes)
                                true
                            }
                            else -> throw IOException("下载失败（HTTP ${response.code}），可稍后重试")
                        }
                        val body = response.body ?: throw IOException("下载响应为空")
                        var written = if (append) offset else 0L
                        val remaining = spec.bytes - written
                        if (directory.usableSpace < remaining + 16L * 1024 * 1024) {
                            throw IOException("存储空间不足，请释放空间后重试")
                        }
                        if (body.contentLength() >= 0 && body.contentLength() != remaining) {
                            throw IOException("下载大小与模型版本不匹配")
                        }
                        body.byteStream().use { input ->
                            partial.outputStream(append).use { output ->
                                val buffer = ByteArray(64 * 1024)
                                var lastReport = 0L
                                while (true) {
                                    currentCoroutineContext().ensureActive()
                                    if (!networkAllowed()) throw IOException("下载已暂停：网络发生变化")
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    if (written + count > spec.bytes) throw IOException("下载内容超出预期大小")
                                    output.write(buffer, 0, count)
                                    written += count
                                    val now = System.nanoTime()
                                    if (now - lastReport >= 100_000_000) {
                                        progress(written, false); lastReport = now
                                    }
                                }
                            }
                        }
                        if (written != spec.bytes) throw IOException("下载中断，已保留进度，可继续下载")
                    }
                } finally {
                    if (call === transfer) call = null
                }
            }
            currentCoroutineContext().ensureActive()
            progress(partial.length(), true)
            if (sha256(partial, currentCoroutineContext()[Job]) != spec.sha256) {
                partial.delete()
                throw IOException("模型校验未通过，请重新下载")
            }
            currentCoroutineContext().ensureActive()
            Files.move(partial.toPath(), model.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            verified = model.length() to model.lastModified()
            progress(spec.bytes, false)
        }
    }

    /** 导入独立临时文件，验证失败绝不损坏现有模型或下载分片。 */
    suspend fun importFile(open: () -> InputStream, progress: (Long, Boolean) -> Unit) = lock.withLock {
        withContext(Dispatchers.IO) {
            directory.mkdirs()
            val candidate = File(directory, "${spec.id}.import")
            try {
                if (directory.usableSpace < spec.bytes + 16L * 1024 * 1024) throw IOException("存储空间不足，请释放空间后重试")
                open().use { input ->
                    importing = input
                    candidate.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var written = 0L
                        var reported = 0L
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            written += count
                            if (written > spec.bytes) throw IOException("文件大小不匹配，请选择兼容的 RMVPE 模型")
                            output.write(buffer, 0, count)
                            val now = System.nanoTime()
                            if (now - reported > 100_000_000) { progress(written, false); reported = now }
                        }
                        if (written != spec.bytes) throw IOException("文件不完整或版本不匹配，请重新选择模型")
                    }
                }
                progress(spec.bytes, true)
                if (sha256(candidate, currentCoroutineContext()[Job]) != spec.sha256) throw IOException("模型校验失败，请选择指定版本的原始文件")
                currentCoroutineContext().ensureActive()
                Files.move(candidate.toPath(), model.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                verified = model.length() to model.lastModified()
                partial.delete()
                progress(spec.bytes, false)
            } finally { importing = null; candidate.delete() }
        }
    }

    suspend fun discardPartial() = lock.withLock { withContext(Dispatchers.IO) {
        if (partial.exists() && !partial.delete()) throw IOException("无法清除下载文件")
    } }

    suspend fun deleteModel() = lock.withLock { withContext(Dispatchers.IO) {
        if (model.exists() && !model.delete()) throw IOException("无法删除模型")
        if (partial.exists() && !partial.delete()) throw IOException("无法删除未完成的下载")
        verified = null
    } }

    private suspend fun verifyInstalled(): Boolean {
        if (model.length() != spec.bytes) { verified = null; return false }
        val fingerprint = model.length() to model.lastModified()
        if (verified == fingerprint) return true
        val valid = sha256(model, currentCoroutineContext()[Job]) == spec.sha256
        verified = fingerprint.takeIf { valid }
        return valid
    }
}

private fun File.outputStream(append: Boolean) = java.io.FileOutputStream(this, append)

internal fun validateRange(header: String?, offset: Long, total: Long) {
    val parts = Regex("bytes (\\d+)-(\\d+)/(\\d+)").matchEntire(header.orEmpty())?.groupValues
        ?: throw IOException("服务器未返回有效的续传范围")
    if (parts[1].toLongOrNull() != offset || parts[2].toLongOrNull() != total - 1 || parts[3].toLongOrNull() != total) {
        throw IOException("服务器返回的续传范围不匹配")
    }
}

internal fun sha256(file: File, job: Job? = null): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().use { input ->
        val bytes = ByteArray(64 * 1024)
        while (true) {
            job?.ensureActive()
            val count = input.read(bytes)
            if (count < 0) break
            digest.update(bytes, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
