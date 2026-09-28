package com.koru.scanner.wifi

/**
 * Pure functions that translate what `android.net.wifi.ScanResult` reports into
 * something a human wants to read. Deliberately free of Android imports so it can be
 * unit-tested on the JVM.
 */
object WifiChannels {

    /**
     * IEEE 802.11 channel number for a centre frequency, or null when the frequency
     * does not sit on a known channel grid. Same arithmetic as the framework's
     * `ScanResult.convertFrequencyMhzToChannelIfSupported`.
     */
    fun channelOf(frequencyMhz: Int): Int? = when (WifiBand.fromFrequencyMhz(frequencyMhz)) {
        WifiBand.GHZ_2_4 -> when {
            frequencyMhz == 2484 -> 14
            frequencyMhz in 2412..2472 && (frequencyMhz - 2412) % 5 == 0 -> (frequencyMhz - 2412) / 5 + 1
            else -> null
        }
        WifiBand.GHZ_5 -> when {
            frequencyMhz in 5000..5899 && frequencyMhz % 5 == 0 -> (frequencyMhz - 5000) / 5
            // 4.9 GHz block: channels 183..196 are defined as (f - 4000) / 5
            frequencyMhz in 4900..4999 && frequencyMhz % 5 == 0 -> (frequencyMhz - 4000) / 5
            else -> null
        }
        WifiBand.GHZ_6 -> when {
            frequencyMhz == 5935 -> 2
            frequencyMhz >= 5955 && (frequencyMhz - 5955) % 5 == 0 -> (frequencyMhz - 5955) / 5 + 1
            else -> null
        }
        WifiBand.GHZ_60 -> when {
            frequencyMhz >= 58_320 && (frequencyMhz - 58_320) % 2160 == 0 -> (frequencyMhz - 58_320) / 2160 + 1
            else -> null
        }
        WifiBand.UNKNOWN -> null
    }

    /**
     * Human label for `ScanResult.channelWidth`. The integer constants are stable
     * framework values (CHANNEL_WIDTH_20MHZ = 0 … CHANNEL_WIDTH_320MHZ = 5).
     */
    fun channelWidthLabel(channelWidth: Int): String = when (channelWidth) {
        0 -> "20 MHz"
        1 -> "40 MHz"
        2 -> "80 MHz"
        3 -> "160 MHz"
        4 -> "80+80 MHz"
        5 -> "320 MHz"
        else -> ""
    }

    /**
     * Coarse security label derived from the capabilities string, e.g.
     * "[WPA2-PSK-CCMP][RSN-PSK-CCMP][ESS]".
     */
    fun securityLabel(capabilities: String?): String {
        val caps = capabilities.orEmpty().uppercase()
        return when {
            "SAE" in caps || "WPA3" in caps -> if ("PSK" in caps) "WPA2/WPA3" else "WPA3"
            "OWE" in caps -> "OWE"
            "WPA2" in caps || "RSN" in caps -> if ("EAP" in caps) "WPA2-EAP" else "WPA2"
            "WPA" in caps -> "WPA"
            "WEP" in caps -> "WEP"
            else -> "Open"
        }
    }

    /** Signal level in 0..4 buckets, similar to the classic `WifiManager.calculateSignalLevel(rssi, 5)`. */
    fun signalLevel(rssiDbm: Int): Int = when {
        rssiDbm >= -55 -> 4
        rssiDbm >= -67 -> 3
        rssiDbm >= -75 -> 2
        rssiDbm >= -85 -> 1
        else -> 0
    }
}
