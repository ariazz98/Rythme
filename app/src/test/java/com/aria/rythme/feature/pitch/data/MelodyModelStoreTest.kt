package com.aria.rythme.feature.pitch.data

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.security.MessageDigest

class MelodyModelStoreTest {
    @get:Rule val folder = TemporaryFolder()
    private val bytes = ByteArray(4096) { (it % 239).toByte() }
    private val digest get() = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun store(interceptor: Interceptor): MelodyModelStore = MelodyModelStore(
        folder.root, OkHttpClient.Builder().addInterceptor(interceptor).build(),
        MelodyModelSpec("test", "https://example.test/model", bytes.size.toLong(), digest)
    )
    private fun response(chain: Interceptor.Chain, code: Int, data: ByteArray, range: String? = null): Response =
        Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(code).message("test")
            .apply { if (range != null) header("Content-Range", range) }.body(data.toResponseBody()).build()

    @Test fun queryingDoesNotDownloadAndOnlyVerifiedFileIsInstalled() = runBlocking {
        var calls = 0
        val store = store { chain -> calls++; response(chain, 200, bytes) }
        assertFalse(store.status().installed)
        assertEquals(0, calls)
        store.download({ true }) { _, _ -> }
        assertTrue(store.status().installed)
        assertArrayEquals(bytes, store.installedFile().readBytes())
        store.download({ true }) { _, _ -> }
        assertEquals(1, calls)
    }

    @Test fun validRangeResumesExistingDownload() = runBlocking {
        File(folder.root, "test.part").writeBytes(bytes.copyOfRange(0, 1000))
        val store = store { chain ->
            assertEquals("bytes=1000-", chain.request().header("Range"))
            response(chain, 206, bytes.copyOfRange(1000, bytes.size), "bytes 1000-4095/4096")
        }
        store.download({ true }) { _, _ -> }
        assertArrayEquals(bytes, store.installedFile().readBytes())
    }

    @Test fun ignoredRangeRestartsWithoutDuplicatingBytes() = runBlocking {
        File(folder.root, "test.part").writeBytes(bytes.copyOfRange(0, 1000))
        val store = store { chain -> response(chain, 200, bytes) }
        store.download({ true }) { _, _ -> }
        assertArrayEquals(bytes, store.installedFile().readBytes())
    }

    @Test fun wrongRangeCannotAppendOrActivateModel() = runBlocking {
        val partial = File(folder.root, "test.part")
        partial.writeBytes(bytes.copyOfRange(0, 1000))
        val store = store { chain -> response(chain, 206, bytes.copyOfRange(1000, 4096), "bytes 999-4095/4096") }
        try { store.download({ true }) { _, _ -> }; fail("Must reject mismatched range") } catch (_: IOException) { }
        assertFalse(store.status().installed)
        assertEquals(1000L, partial.length())
    }

    @Test fun corruptBytesCannotBecomeInstalled() = runBlocking {
        val store = store { chain -> response(chain, 200, bytes.copyOf().apply { this[100] = 0 }) }
        try { store.download({ true }) { _, _ -> }; fail("Must reject checksum mismatch") } catch (_: IOException) { }
        assertFalse(store.status().installed)
        assertFalse(File(folder.root, "test.part").exists())
    }

    @Test fun disallowedNetworkMakesNoRequestAndKeepsPartial() = runBlocking {
        File(folder.root, "test.part").writeBytes(bytes.copyOfRange(0, 1000))
        val store = store { error("Must not access network") }
        try { store.download({ false }) { _, _ -> }; fail("Must not download") } catch (_: IOException) { }
        assertEquals(1000L, store.status().downloadedBytes)
    }

    @Test fun deletingModelKeepsUnrelatedUserData() = runBlocking {
        val unrelated = File(folder.root, "other-data").apply { writeText("keep") }
        val store = store { chain -> response(chain, 200, bytes) }
        store.download({ true }) { _, _ -> }
        store.deleteModel()
        assertFalse(store.status().installed)
        assertEquals("keep", unrelated.readText())
    }
    @Test fun importValidatesAndKeepsSource() = runBlocking {
        val store = store { error("Import must not use the network") }
        val source = File(folder.root, "downloaded.onnx").apply { writeBytes(bytes) }
        store.importFile({ source.inputStream() }) { _, _ -> }
        assertTrue(store.status().installed)
        assertArrayEquals(bytes, source.readBytes())
    }

    @Test fun wrongImportPreservesExistingDownload() = runBlocking {
        val partial = File(folder.root, "test.part").apply { writeBytes(bytes.copyOfRange(0, 1000)) }
        val store = store { error("Import must not use the network") }
        try {
            store.importFile({ bytes.copyOf().apply { this[100] = 0 }.inputStream() }) { _, _ -> }
            fail("Must reject wrong model")
        } catch (_: IOException) { }
        assertEquals(1000L, partial.length())
        assertFalse(store.status().installed)
        assertFalse(File(folder.root, "test.import").exists())
    }

    @Test fun oversizedImportPreservesInstalledModel() = runBlocking {
        val store = store { error("Import must not use the network") }
        store.importFile({ bytes.inputStream() }) { _, _ -> }
        try {
            store.importFile({ ByteArray(5000).inputStream() }) { _, _ -> }
            fail("Must reject excessive bytes")
        } catch (_: IOException) { }
        assertTrue(store.status().installed)
        assertArrayEquals(bytes, store.installedFile().readBytes())
    }

    @Test fun cancelImportCleansTemporaryFile() = runBlocking {
        val store = store { error("Import must not use the network") }
        try {
            store.importFile({ bytes.inputStream() }) { _, _ -> throw kotlinx.coroutines.CancellationException() }
            fail("Must cancel")
        } catch (_: kotlinx.coroutines.CancellationException) { }
        assertFalse(store.status().installed)
        assertFalse(File(folder.root, "test.import").exists())
    }

}
