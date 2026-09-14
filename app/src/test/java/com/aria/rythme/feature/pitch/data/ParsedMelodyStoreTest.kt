package com.aria.rythme.feature.pitch.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ParsedMelodyStoreTest {
    @get:Rule val temp = TemporaryFolder()
    private fun entry(key: String = "a".repeat(64)) = ParsedMelodyEntry(key, 1, "知我", "艺人", "content://test/1", "", 20, 5, 100, 123)
    private fun curve() = MelodyCurve(0, 20, floatArrayOf(220f, 221f, 222f), floatArrayOf(1f, 1f, 1f))
    @Test fun entryAndCurveSurviveRestartAndTemporaryCacheClear() = runBlocking {
        val root = temp.newFolder("library")
        ParsedMelodyStore(root).save(entry(), curve())
        val cache = MelodyCurveCache(temp.newFolder("cache")); cache.write(entry().key, curve()); cache.clear()
        val reopened = ParsedMelodyStore(root); reopened.refresh()
        assertEquals(listOf(entry()), reopened.entries.value)
        assertArrayEquals(curve().hz, reopened.read(entry())!!.hz, .01f)
    }
    @Test fun reanalysisReplacesOnlySameSourceAfterSavingNewResult() = runBlocking {
        val root = temp.newFolder("library"); val store = ParsedMelodyStore(root)
        store.save(entry(), curve()); val replacement = entry("b".repeat(64)); store.save(replacement, curve())
        assertEquals(listOf(replacement), store.entries.value)
        assertFalse(File(root, entry().key + ".curve").exists())
        assertTrue(store.ignoredLegacyKeys().contains(entry().key))
    }
    @Test fun coldWriterReplacesPreviousAnalysisWithoutPriorRefresh() = runBlocking {
        val root = temp.newFolder("library")
        ParsedMelodyStore(root).save(entry(), curve())
        val writer = ParsedMelodyStore(root); val replacement = entry("c".repeat(64))
        writer.save(replacement, curve())
        assertEquals(listOf(replacement), writer.entries.value)
        val reopened = ParsedMelodyStore(root); reopened.refresh()
        assertEquals(listOf(replacement), reopened.entries.value)
    }
    @Test fun removalKeepsOriginalAudioAndPreventsLegacyResurrection() = runBlocking {
        val audio = temp.newFile("song.mp3"); audio.writeText("original")
        val store = ParsedMelodyStore(temp.newFolder("library")); store.save(entry(), curve()); store.remove(entry())
        assertTrue(store.entries.value.isEmpty()); assertEquals("original", audio.readText())
        assertEquals(setOf(entry().key), store.ignoredLegacyKeys())
    }
    @Test fun corruptCurveKeepsListEntryForRecovery() = runBlocking {
        val root = temp.newFolder("library"); val store = ParsedMelodyStore(root); store.save(entry(), curve())
        File(root, entry().key + ".curve").writeText("broken")
        assertNull(store.read(entry())); store.refresh(); assertEquals(listOf(entry()), store.entries.value)
    }
    @Test fun invalidKeyCannotEscapeLibrary() = runBlocking {
        val store = ParsedMelodyStore(temp.newFolder("library"))
        try { store.save(entry("../other"), curve()); fail("Expected invalid key") } catch (_: IllegalArgumentException) { }
    }
}
