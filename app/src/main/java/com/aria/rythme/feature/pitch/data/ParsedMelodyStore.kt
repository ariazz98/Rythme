package com.aria.rythme.feature.pitch.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Properties

/** 与可淘汰的分析缓存分开；只存派生曲线与歌曲定位信息，不复制原歌曲。 */
data class ParsedMelodyEntry(
    val key: String, val songId: Long, val title: String, val artist: String,
    val uri: String, val coverUri: String, val durationMs: Long,
    val modified: Long, val sourceSize: Long, val addedAt: Long = System.currentTimeMillis()
)

class ParsedMelodyStore(private val directory: File) {
    private val curves = MelodyCurveCache(directory, maxBytes = Long.MAX_VALUE)
    private val mutex = Mutex()
    private val mutable = MutableStateFlow<List<ParsedMelodyEntry>>(emptyList())
    val entries = mutable.asStateFlow()

    suspend fun ignoredLegacyKeys(): Set<String> = withContext(Dispatchers.IO) {
        mutex.withLock { File(directory, "legacy-ignored").takeIf { it.exists() }?.readLines()
            ?.filter { it.matches(Regex("[a-f0-9]{64}")) }?.toSet().orEmpty() }
    }
    suspend fun refresh() = withContext(Dispatchers.IO) { mutex.withLock { reload() } }
    private fun reload() {
        mutable.value = directory.listFiles().orEmpty().filter { it.extension == "properties" }.mapNotNull { file ->
            runCatching {
                val p = Properties().apply { file.inputStream().use { load(it) } }
                fun value(key: String) = requireNotNull(p.getProperty(key))
                check(value("schema") == "1")
                val key = value("key"); validateKey(key)
                check(file.nameWithoutExtension == key)
                ParsedMelodyEntry(key, value("songId").toLong(), value("title"), value("artist"),
                    value("uri"), value("cover"), value("duration").toLong(), value("modified").toLong(),
                    value("size").toLong(), value("added").toLong())
            }.getOrNull()
        }.sortedByDescending { it.addedAt }
    }
    suspend fun save(entry: ParsedMelodyEntry, curve: MelodyCurve) = withContext(Dispatchers.IO) {
        mutex.withLock {
            validateKey(entry.key)
            reload()
            curves.write(entry.key, curve)
            val p = Properties().apply {
                setProperty("schema", "1"); setProperty("key", entry.key); setProperty("songId", entry.songId.toString())
                setProperty("title", entry.title); setProperty("artist", entry.artist); setProperty("uri", entry.uri)
                setProperty("cover", entry.coverUri); setProperty("duration", entry.durationMs.toString())
                setProperty("modified", entry.modified.toString()); setProperty("size", entry.sourceSize.toString())
                setProperty("added", entry.addedAt.toString())
            }
            val temp = File(directory, "${entry.key}.metadata.tmp")
            try {
                temp.outputStream().use { p.store(it, "Rythme parsed melody") }
                Files.move(temp.toPath(), File(directory, "${entry.key}.properties").toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
            } finally { temp.delete() }
            // 新结果和索引都成功后才替换同一首的旧解析。
            mutable.value.filter { it.uri == entry.uri && it.key != entry.key }.forEach { removeFiles(it) }
            reload()
        }
    }
    suspend fun read(entry: ParsedMelodyEntry): MelodyCurve? = withContext(Dispatchers.IO) {
        mutex.withLock { validateKey(entry.key); curves.read(entry.key) }
    }
    suspend fun remove(entry: ParsedMelodyEntry) = withContext(Dispatchers.IO) {
        mutex.withLock { removeFiles(entry); reload() }
    }
    private fun removeFiles(entry: ParsedMelodyEntry) {
        validateKey(entry.key)
        File(directory, "legacy-ignored").appendText("${entry.key}\n")
        val index = File(directory, "${entry.key}.properties")
        check(!index.exists() || index.delete()) { "无法移除解析记录" }
        File(directory, "${entry.key}.curve").delete()
    }
    private fun validateKey(key: String) { require(key.matches(Regex("[a-f0-9]{64}"))) }
}
