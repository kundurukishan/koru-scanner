package com.koru.scanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.koru.scanner.ui.ScannerScreen
import com.koru.scanner.ui.theme.KoruTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ScannerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KoruTheme {
                ScannerScreen(viewModel = viewModel)
            }
        }
    }
}
