package com.mattyinc.sidescan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mattyinc.sidescan.ui.ScanScreen
import com.mattyinc.sidescan.ui.theme.SideScanTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ScanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SideScanTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                ScanScreen(
                    state = state,
                    onScan = { includeSystem -> viewModel.scan(includeSystem) }
                )
            }
        }
    }
}
