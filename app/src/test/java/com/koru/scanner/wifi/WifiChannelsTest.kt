package com.koru.scanner.wifi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WifiChannelsTest {

    @Test
    fun `2_4 GHz channels`() {
        assertEquals(1, WifiChannels.channelOf(2412))
        assertEquals(6, WifiChannels.channelOf(2437))
        assertEquals(11, WifiChannels.channelOf(2462))
        assertEquals(13, WifiChannels.channelOf(2472))
        assertEquals(14, WifiChannels.channelOf(2484))
        assertNull(WifiChannels.channelOf(2413))
    }

    @Test
    fun `5 GHz channels`() {
        assertEquals(36, WifiChannels.channelOf(5180))
        assertEquals(40, WifiChannels.channelOf(5200))
        assertEquals(100, WifiChannels.channelOf(5500))
        assertEquals(149, WifiChannels.channelOf(5745))
        assertEquals(165, WifiChannels.channelOf(5825))
        assertEquals(177, WifiChannels.channelOf(5885))
        // 4.9 GHz public-safety block
        assertEquals(184, WifiChannels.channelOf(4920))
    }

    @Test
    fun `6 GHz channels`() {
        assertEquals(2, WifiChannels.channelOf(5935))
        assertEquals(1, WifiChannels.channelOf(5955))
        assertEquals(5, WifiChannels.channelOf(5975))
        assertEquals(37, WifiChannels.channelOf(6135))
        assertEquals(233, WifiChannels.channelOf(7115))
    }

    @Test
    fun `60 GHz channels`() {
        assertEquals(1, WifiChannels.channelOf(58_320))
        assertEquals(2, WifiChannels.channelOf(60_480))
        assertEquals(6, WifiChannels.channelOf(69_120))
    }

    @Test
    fun `unknown frequencies have no channel`() {
        assertNull(WifiChannels.channelOf(0))
        assertNull(WifiChannels.channelOf(3000))
        assertNull(WifiChannels.channelOf(5905))
    }

    @Test
    fun `band detection`() {
        assertEquals(WifiBand.GHZ_2_4, WifiBand.fromFrequencyMhz(2437))
        assertEquals(WifiBand.GHZ_5, WifiBand.fromFrequencyMhz(5180))
        assertEquals(WifiBand.GHZ_5, WifiBand.fromFrequencyMhz(5885))
        assertEquals(WifiBand.GHZ_6, WifiBand.fromFrequencyMhz(5935))
        assertEquals(WifiBand.GHZ_6, WifiBand.fromFrequencyMhz(6135))
        assertEquals(WifiBand.GHZ_60, WifiBand.fromFrequencyMhz(60_480))
        assertEquals(WifiBand.UNKNOWN, WifiBand.fromFrequencyMhz(0))
    }

    @Test
    fun `channel width labels`() {
        assertEquals("20 MHz", WifiChannels.channelWidthLabel(0))
        assertEquals("80 MHz", WifiChannels.channelWidthLabel(2))
        assertEquals("320 MHz", WifiChannels.channelWidthLabel(5))
        assertEquals("", WifiChannels.channelWidthLabel(42))
    }

    @Test
    fun `security labels`() {
        assertEquals("WPA2", WifiChannels.securityLabel("[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]"))
        assertEquals("WPA2/WPA3", WifiChannels.securityLabel("[RSN-PSK+SAE-CCMP][ESS][MFPC]"))
        assertEquals("WPA3", WifiChannels.securityLabel("[RSN-SAE-CCMP][ESS][MFPR][MFPC]"))
        assertEquals("WPA2-EAP", WifiChannels.securityLabel("[WPA2-EAP/SHA1-CCMP][RSN-EAP/SHA1-CCMP][ESS]"))
        assertEquals("WPA", WifiChannels.securityLabel("[WPA-PSK-TKIP][ESS]"))
        assertEquals("WEP", WifiChannels.securityLabel("[WEP][ESS]"))
        assertEquals("OWE", WifiChannels.securityLabel("[RSN-OWE-CCMP][ESS]"))
        assertEquals("Open", WifiChannels.securityLabel("[ESS]"))
        assertEquals("Open", WifiChannels.securityLabel(null))
    }

    @Test
    fun `signal levels`() {
        assertEquals(4, WifiChannels.signalLevel(-40))
        assertEquals(3, WifiChannels.signalLevel(-60))
        assertEquals(2, WifiChannels.signalLevel(-70))
        assertEquals(1, WifiChannels.signalLevel(-80))
        assertEquals(0, WifiChannels.signalLevel(-95))
    }
}
