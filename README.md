# Koru Wi-Fi Scanner

A small Android app (Kotlin + Jetpack Compose) that asks the Wi-Fi chip to sweep every
channel it supports and lists what it hears:

* **SSID** — coloured by band (2.4 GHz amber, 5 GHz blue, 6 GHz purple, 60 GHz teal),
  so the same network broadcast on several bands is easy to tell apart.
* **Channel** and centre frequency (MHz), plus channel width and security type.
* **RSSI** in dBm, with a 4-bar signal indicator. Rows are sorted strongest first.

Tap a band chip in the legend to hide/show that band. Auto-refresh re-scans every 30 s
while the app is in the foreground; the refresh button forces a scan.

## Requirements

* Android 8.0 (API 26) or newer.
* Precise location permission — Android hides scan results without it. On Android 13+
  the app also asks for `NEARBY_WIFI_DEVICES`.
* Device location must be turned on (Android 9+), and Wi-Fi (or "Wi-Fi scanning") enabled.

## Building

```
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Install with `adb install app/build/outputs/apk/debug/app-debug.apk`.

## Notes on scanning

Since Android 9 the platform throttles foreground apps to four `startScan()` calls per
two minutes. When a request is throttled the app shows a notice and keeps displaying the
most recent cached results, which are still refreshed whenever the system performs its
own scans.

Channel numbers are derived from centre frequency with the same arithmetic the framework
uses (`WifiChannels.channelOf`), covering 2.4, 4.9/5, 6 and 60 GHz.
