package com.koru.scanner.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.koru.scanner.wifi.WifiBand

private val LightColors = lightColorScheme(
    primary = Color(0xFF0D3B4C),
    secondary = Color(0xFF3F6C7A),
    tertiary = Color(0xFF7E5700),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD3E8),
    secondary = Color(0xFFA6C8D2),
    tertiary = Color(0xFFF6C26B),
)

@Composable
fun KoruTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

/**
 * One distinct hue per band so an SSID broadcast on 2.4, 5 and 6 GHz is easy to tell apart.
 * Each pair keeps enough contrast against the default surface in its theme.
 */
@Composable
fun WifiBand.color(darkTheme: Boolean = isSystemInDarkTheme()): Color = when (this) {
    WifiBand.GHZ_2_4 -> if (darkTheme) Color(0xFFFFB74D) else Color(0xFFE65100) // amber / deep orange
    WifiBand.GHZ_5 -> if (darkTheme) Color(0xFF64B5F6) else Color(0xFF1565C0)   // light blue / blue
    WifiBand.GHZ_6 -> if (darkTheme) Color(0xFFCE93D8) else Color(0xFF7B1FA2)   // lilac / purple
    WifiBand.GHZ_60 -> if (darkTheme) Color(0xFF4DB6AC) else Color(0xFF00695C)  // teal
    WifiBand.UNKNOWN -> if (darkTheme) Color(0xFFBDBDBD) else Color(0xFF616161) // grey
}
