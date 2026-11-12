package com.mattyinc.sidescan

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mattyinc.sidescan.scan.AppScanner
import com.mattyinc.sidescan.scan.RiskEngine
import com.mattyinc.sidescan.scan.RiskLevel
import com.mattyinc.sidescan.scan.ScannedApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ScanState {
    data object Idle : ScanState
    data class Running(val done: Int, val total: Int) : ScanState
    data class Done(val apps: List<ScannedApp>, val includeSystem: Boolean) : ScanState
}

class ScanViewModel(app: Application) : AndroidViewModel(app) {

    private val scanner = AppScanner(app)
    private val engine = RiskEngine(scanner)

    private val _state = MutableStateFlow<ScanState>(ScanState.Idle)
    val state: StateFlow<ScanState> = _state.asStateFlow()

    fun scan(includeSystem: Boolean = false) {
        viewModelScope.launch {
            val results = withContext(Dispatchers.Default) {
                val packages = scanner.installedApps(includeSystem)
                val total = packages.size
                val out = ArrayList<ScannedApp>(total)
                packages.forEachIndexed { index, pkg ->
                    out += engine.evaluate(pkg)
                    _state.value = ScanState.Running(index + 1, total)
                }
                out.sortedWith(
                    compareByDescending<ScannedApp> { it.score }
                        .thenBy { it.label.lowercase() }
                )
            }
            _state.value = ScanState.Done(results, includeSystem)
        }
    }

    fun reset() {
        _state.value = ScanState.Idle
    }

    companion object {
        fun summarize(apps: List<ScannedApp>): Map<RiskLevel, Int> =
            RiskLevel.entries.associateWith { level -> apps.count { it.level == level } }
    }
}
