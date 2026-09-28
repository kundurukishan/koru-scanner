package com.koru.scanner.wifi

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Thin wrapper over [WifiManager] that hides the permission / API-level noise.
 *
 * A single `startScan()` asks the Wi-Fi chip to sweep every channel it supports on every
 * band (2.4 / 5 / 6 GHz where the hardware allows). Results are delivered asynchronously via
 * [WifiManager.SCAN_RESULTS_AVAILABLE_ACTION] and read back with [WifiManager.getScanResults].
 */
class WifiScanner(context: Context) {

    private val appContext = context.applicationContext
    private val wifiManager =
        appContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val isWifiEnabled: Boolean get() = wifiManager.isWifiEnabled

    /** Android returns an empty scan list when device location is off (API 28+). */
    val isLocationEnabled: Boolean get() = LocationManagerCompat.isLocationEnabled(locationManager)

    /** The runtime permissions the app must hold to read scan results. */
    val requiredPermissions: Array<String>
        get() = buildList {
            add(Manifest.permission.ACCESS_FINE_LOCATION)
            add(Manifest.permission.ACCESS_COARSE_LOCATION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.NEARBY_WIFI_DEVICES)
            }
        }.toTypedArray()

    /** True when we can actually read SSIDs (fine location is the gatekeeper). */
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Kick off a full-channel scan. Returns false if the platform refused, which is usually
     * foreground throttling (4 scans per 2 minutes on API 28+). Cached results remain readable.
     */
    fun startScan(): Boolean = try {
        @Suppress("DEPRECATION")
        wifiManager.startScan()
    } catch (_: SecurityException) {
        false
    }

    /** Emits every time the platform announces fresh scan results. */
    fun scanResultsAvailable(): Flow<Boolean> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val updated = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, true)
                trySend(updated)
            }
        }
        ContextCompat.registerReceiver(
            appContext,
            receiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION),
            // The action is a protected system broadcast; only the OS can send it.
            ContextCompat.RECEIVER_EXPORTED,
        )
        awaitClose { appContext.unregisterReceiver(receiver) }
    }

    /** Latest results the platform holds, mapped into display rows and sorted strongest first. */
    fun currentAccessPoints(): List<AccessPoint> {
        val results: List<ScanResult> = try {
            wifiManager.scanResults ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
        val nowMicros = SystemClock.elapsedRealtime() * 1000
        return results
            .map { it.toAccessPoint(nowMicros) }
            .sortedWith(compareByDescending<AccessPoint> { it.rssiDbm }.thenBy { it.ssid })
    }

    private fun ScanResult.toAccessPoint(nowMicros: Long): AccessPoint {
        val ssidText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            wifiSsid?.toString()?.trim('"') ?: ""
        } else {
            @Suppress("DEPRECATION")
            SSID ?: ""
        }
        return AccessPoint(
            ssid = ssidText,
            bssid = BSSID ?: "",
            frequencyMhz = frequency,
            channel = WifiChannels.channelOf(frequency),
            band = WifiBand.fromFrequencyMhz(frequency),
            channelWidthLabel = WifiChannels.channelWidthLabel(channelWidth),
            rssiDbm = level,
            security = WifiChannels.securityLabel(capabilities),
            ageMillis = ((nowMicros - timestamp) / 1000).coerceAtLeast(0),
        )
    }
}
