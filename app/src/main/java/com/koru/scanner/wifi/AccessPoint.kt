package com.koru.scanner.wifi

/** One row in the scan list: a single BSSID seen on a single channel. */
data class AccessPoint(
    val ssid: String,
    val bssid: String,
    val frequencyMhz: Int,
    val channel: Int?,
    val band: WifiBand,
    val channelWidthLabel: String,
    val rssiDbm: Int,
    val security: String,
    /** Milliseconds since this result was captured by the Wi-Fi chip. */
    val ageMillis: Long,
) {
    val isHidden: Boolean get() = ssid.isEmpty()
    val signalLevel: Int get() = WifiChannels.signalLevel(rssiDbm)
}
