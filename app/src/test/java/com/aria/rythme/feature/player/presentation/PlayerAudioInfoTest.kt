package com.aria.rythme.feature.player.presentation

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerAudioInfoTest {
    @Test fun codecLabelsUseAudioTrackMimeInsteadOfContainerNames() {
        assertEquals("FLAC", playerCodecLabel("audio/flac"))
        assertEquals("ALAC", playerCodecLabel("audio/alac"))
        assertEquals("MP3", playerCodecLabel("audio/mpeg"))
        assertEquals("AAC", playerCodecLabel("audio/mp4a-latm"))
        assertEquals("未知编码", playerCodecLabel("audio/mp4"))
        assertEquals("未知编码", playerCodecLabel("audio/wav"))
    }
}
