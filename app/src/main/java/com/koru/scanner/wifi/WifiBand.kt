package com.koru.scanner.wifi

/**
 * Wi-Fi frequency bands. Ranges mirror the ones the Android framework uses in
 * `ScanResult.is24GHz()/is5GHz()/is6GHz()/is60GHz()`.
 */
enum class WifiBand(val label: String) {
    GHZ_2_4("2.4 GHz"),
    GHZ_5("5 GHz"),
    GHZ_6("6 GHz"),
    GHZ_60("60 GHz"),
    UNKNOWN("Other");

    companion object {
        fun fromFrequencyMhz(mhz: Int): WifiBand = when (mhz) {
            in 2400..2500 -> GHZ_2_4
            in 4900..5899 -> GHZ_5   // includes the 4.9 GHz public-safety block used by some regions
            in 5925..7125 -> GHZ_6
            in 57_000..71_000 -> GHZ_60
            else -> UNKNOWN
        }

        /** Bands worth listing in the legend, in display order. */
        val DISPLAYED: List<WifiBand> = listOf(GHZ_2_4, GHZ_5, GHZ_6, GHZ_60)
    }
}
