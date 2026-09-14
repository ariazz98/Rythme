package com.aria.rythme.feature.pitch.data

import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sin

class SongMelodyProcessingTest {
    @get:Rule val folder = TemporaryFolder()
    @Test fun windowedMelMatchesWholeSignalIncludingFileEdges() {
        val pcm = FloatArray(160_123) { (.4 * sin(it * .09)).toFloat() }
        val full = MelodyDsp.mel(pcm, Runnable {})
        for (start in listOf(0, 384, 768)) {
            val begin = start - 64
            val first = (begin * 160 - 512).coerceAtLeast(0)
            val last = ((begin + 512) * 160 + 512).coerceAtMost(pcm.size)
            val block = MelodyDsp.melFrames(pcm.copyOfRange(first, last), first, pcm.size, begin, 512, Runnable {})
            for (m in block.indices) for (f in 0 until 512) {
                val global = begin + f
                assertEquals(if (global in full[m].indices) full[m][global] else 0f, block[m][f], .00001f)
            }
        }
    }
    @Test fun chunkedResamplingKeepsGlobalSamplePhase() {
        for (rate in listOf(16000, 44100, 48000)) {
            val pcm = FloatArray(rate * 2 + 17) { (.5 * sin(it * .03)).toFloat() }
            val source = File(folder.root, "source-$rate.f32")
            val bytes = ByteBuffer.allocate(pcm.size * 4).order(ByteOrder.LITTLE_ENDIAN)
            pcm.forEach { bytes.putFloat(it) }; source.writeBytes(bytes.array())
            val target = File(folder.root, "result-$rate.f32")
            SongAudioDecoder.resample(source, target, rate, pcm.size, {}, {})
            val result = FloatArray(target.length().toInt() / 4)
            ByteBuffer.wrap(target.readBytes()).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(result)
            assertArrayEquals(MelodyDsp.resample(pcm, rate, Runnable {}), result, .00001f)
        }
    }
    @Test fun wholeSongCacheRoundTripsAndRejectsTruncation() {
        val cache = MelodyCurveCache(folder.root)
        val key = "a".repeat(64)
        val curve = MelodyCurve(0, 241_337, FloatArray(24134) { 440f }, FloatArray(24134) { .9f })
        cache.write(key, curve)
        val read = requireNotNull(cache.read(key))
        assertEquals(curve.endMs, read.endMs); assertArrayEquals(curve.hz, read.hz, 0f)
        File(folder.root, "$key.curve").writeBytes(byteArrayOf(1, 2))
        assertNull(cache.read(key))
    }
    @Test fun invalidModelOutputCannotBecomeAPitch() {
        assertEquals(0f, decodeMelodyScores(FloatArray(360)).first, 0f)
        assertEquals(0f, decodeMelodyScores(FloatArray(360) { -0.0000001f }).second, 0f)
        try { decodeMelodyScores(FloatArray(360) { Float.NaN }); fail("Must reject NaN") } catch (_: IllegalArgumentException) { }
    }
}
