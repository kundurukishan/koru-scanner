package com.koru.scanner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.koru.scanner.wifi.AccessPoint
import com.koru.scanner.wifi.WifiBand
import com.koru.scanner.wifi.WifiScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScannerUiState(
    val hasPermission: Boolean = false,
    val wifiEnabled: Boolean = true,
    val locationEnabled: Boolean = true,
    val isScanning: Boolean = false,
    val accessPoints: List<AccessPoint> = emptyList(),
    /** Epoch millis of the last successful read, or null before the first one. */
    val lastUpdatedMillis: Long? = null,
    val notice: String? = null,
    /** Bands the user has toggled off in the legend. */
    val hiddenBands: Set<WifiBand> = emptySet(),
    val autoRefresh: Boolean = true,
) {
    val visibleAccessPoints: List<AccessPoint>
        get() = accessPoints.filter { it.band !in hiddenBands }

    val countsPerBand: Map<WifiBand, Int>
        get() = accessPoints.groupingBy { it.band }.eachCount()
}

class ScannerViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = WifiScanner(application)

    private val _uiState = MutableStateFlow(ScannerUiState(hasPermission = scanner.hasPermission()))
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()

    val requiredPermissions: Array<String> get() = scanner.requiredPermissions

    private var resultsJob: Job? = null
    private var noticeJob: Job? = null

    init {
        if (scanner.hasPermission()) startListening()
    }

    /** Call after the permission dialog closes, or whenever the screen comes back to the foreground. */
    fun onPermissionsChanged() {
        val granted = scanner.hasPermission()
        _uiState.update { it.copy(hasPermission = granted) }
        if (granted) {
            startListening()
            refresh()
        }
    }

    /** Re-read cached results immediately and ask the chip for a fresh full-channel sweep. */
    fun refresh() {
        if (!scanner.hasPermission()) return
        loadResults()
        val accepted = scanner.startScan()
        _uiState.update { it.copy(isScanning = accepted) }
        if (accepted) {
            showNotice(getApplication<Application>().getString(R.string.scan_started))
        } else {
            showNotice(getApplication<Application>().getString(R.string.scan_throttled))
        }
    }

    fun toggleBand(band: WifiBand) {
        _uiState.update { state ->
            val hidden = state.hiddenBands.toMutableSet()
            if (!hidden.add(band)) hidden.remove(band)
            state.copy(hiddenBands = hidden)
        }
    }

    fun setAutoRefresh(enabled: Boolean) {
        _uiState.update { it.copy(autoRefresh = enabled) }
    }

    fun dismissNotice() {
        noticeJob?.cancel()
        _uiState.update { it.copy(notice = null) }
    }

    private fun startListening() {
        if (resultsJob != null) return
        resultsJob = viewModelScope.launch {
            scanner.scanResultsAvailable().collect { loadResults() }
        }
    }

    private fun loadResults() {
        val points = scanner.currentAccessPoints()
        _uiState.update {
            it.copy(
                accessPoints = points,
                isScanning = false,
                wifiEnabled = scanner.isWifiEnabled,
                locationEnabled = scanner.isLocationEnabled,
                lastUpdatedMillis = System.currentTimeMillis(),
            )
        }
    }

    private fun showNotice(text: String) {
        noticeJob?.cancel()
        _uiState.update { it.copy(notice = text) }
        noticeJob = viewModelScope.launch {
            delay(NOTICE_DURATION_MILLIS)
            _uiState.update { if (it.notice == text) it.copy(notice = null) else it }
        }
    }

    private companion object {
        const val NOTICE_DURATION_MILLIS = 4_000L
    }
}
