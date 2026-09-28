package com.koru.scanner.ui

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.koru.scanner.R
import com.koru.scanner.ScannerUiState
import com.koru.scanner.ScannerViewModel
import com.koru.scanner.ui.theme.color
import com.koru.scanner.wifi.AccessPoint
import com.koru.scanner.wifi.WifiBand
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

/** Foreground apps may start at most 4 scans per 2 minutes, so 30 s keeps us inside the budget. */
private const val AUTO_REFRESH_INTERVAL_MILLIS = 30_000L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(viewModel: ScannerViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val lifecycleOwner = LocalLifecycleOwner.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.onPermissionsChanged() }

    // Re-check permission / Wi-Fi / location every time we come back to the foreground,
    // then keep polling while the screen is visible and auto-refresh is on.
    LaunchedEffect(state.autoRefresh) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.onPermissionsChanged()
            while (state.autoRefresh) {
                delay(AUTO_REFRESH_INTERVAL_MILLIS)
                viewModel.refresh()
            }
        }
    }

    LaunchedEffect(state.notice) {
        val notice = state.notice ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(notice)
        viewModel.dismissNotice()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = viewModel::refresh, enabled = state.hasPermission) {
                        Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.action_scan))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (state.isScanning) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                Spacer(Modifier.height(4.dp))
            }

            if (!state.hasPermission) {
                PermissionCard(onRequest = { permissionLauncher.launch(viewModel.requiredPermissions) })
                return@Column
            }

            PrerequisiteWarnings(state)
            BandLegend(state = state, onToggle = viewModel::toggleBand)
            StatusRow(state = state, onAutoRefreshChange = viewModel::setAutoRefresh)
            HorizontalDivider()

            val rows = state.visibleAccessPoints
            if (rows.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(rows, key = { it.bssid + it.frequencyMhz }) { ap ->
                        AccessPointRow(ap)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionCard(onRequest: () -> Unit) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.permission_title), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.permission_body), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onRequest) { Text(stringResource(R.string.action_grant)) }
                TextButton(onClick = {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                }) { Text(stringResource(R.string.action_open_settings)) }
            }
        }
    }
}

@Composable
private fun PrerequisiteWarnings(state: ScannerUiState) {
    val context = LocalContext.current
    if (!state.wifiEnabled) {
        WarningCard(
            title = stringResource(R.string.wifi_off_title),
            body = stringResource(R.string.wifi_off_body),
            onAction = { context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) },
        )
    }
    if (!state.locationEnabled) {
        WarningCard(
            title = stringResource(R.string.location_off_title),
            body = stringResource(R.string.location_off_body),
            onAction = { context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)) },
        )
    }
}

@Composable
private fun WarningCard(title: String, body: String, onAction: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            TextButton(onClick = onAction, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.action_open_settings))
            }
        }
    }
}

/** One chip per band; tap to hide/show that band. Chip colour matches the SSID colour in the list. */
@Composable
private fun BandLegend(state: ScannerUiState, onToggle: (WifiBand) -> Unit) {
    val counts = state.countsPerBand
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WifiBand.DISPLAYED.forEach { band ->
            val bandColor = band.color()
            val selected = band !in state.hiddenBands
            FilterChip(
                selected = selected,
                onClick = { onToggle(band) },
                label = { Text("${band.label} · ${counts[band] ?: 0}") },
                leadingIcon = {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) bandColor else bandColor.copy(alpha = 0.35f)),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = bandColor.copy(alpha = 0.18f),
                    selectedLabelColor = bandColor,
                ),
            )
        }
    }
}

@Composable
private fun StatusRow(state: ScannerUiState, onAutoRefreshChange: (Boolean) -> Unit) {
    val formatter = remember { DateFormat.getTimeInstance(DateFormat.MEDIUM) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val updated = state.lastUpdatedMillis?.let { formatter.format(Date(it)) } ?: "—"
        Text(
            text = "${state.visibleAccessPoints.size} of ${state.accessPoints.size} networks · updated $updated",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(stringResource(R.string.auto_refresh), style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.width(6.dp))
        Switch(checked = state.autoRefresh, onCheckedChange = onAutoRefreshChange)
    }
}

@Composable
private fun EmptyState() {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            stringResource(R.string.empty_results),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AccessPointRow(ap: AccessPoint) {
    val bandColor = ap.band.color()
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Band stripe
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(48.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(bandColor),
        )
        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = if (ap.isHidden) stringResource(R.string.hidden_ssid) else ap.ssid,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    fontStyle = if (ap.isHidden) FontStyle.Italic else FontStyle.Normal,
                    color = bandColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                BandTag(label = ap.band.label, color = bandColor)
            }
            Text(
                text = listOf(ap.bssid, ap.security, ap.channelWidthLabel)
                    .filter { it.isNotEmpty() }
                    .joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            SignalBars(level = ap.signalLevel, color = bandColor)
        }

        Spacer(Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = ap.channel?.let { "Ch $it" } ?: "Ch ?",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = bandColor,
            )
            Text(
                text = "${ap.frequencyMhz} MHz",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${ap.rssiDbm} dBm",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun BandTag(label: String, color: Color) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** Four ascending bars; the lit count follows the RSSI bucket. */
@Composable
private fun SignalBars(level: Int, color: Color) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        for (i in 1..4) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .height((4 + i * 3).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(if (i <= level) color else color.copy(alpha = 0.2f)),
            )
        }
    }
}
