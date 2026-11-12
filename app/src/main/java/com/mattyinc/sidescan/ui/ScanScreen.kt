package com.mattyinc.sidescan.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mattyinc.sidescan.ScanState
import com.mattyinc.sidescan.ScanViewModel
import com.mattyinc.sidescan.scan.RiskLevel
import com.mattyinc.sidescan.scan.ScannedApp
import com.mattyinc.sidescan.ui.theme.RiskColors

@Composable
fun ScanScreen(state: ScanState, onScan: (Boolean) -> Unit) {
    var includeSystem by remember { mutableStateOf(false) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.size(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    "SideScan",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                "Finds bandwidth-sharing & dropper apps on your device.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )

            Spacer(Modifier.size(16.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Include system apps",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Switch(checked = includeSystem, onCheckedChange = { includeSystem = it })
            }

            Spacer(Modifier.size(8.dp))

            Button(
                onClick = { onScan(includeSystem) },
                modifier = Modifier.fillMaxWidth(),
                enabled = state !is ScanState.Running
            ) {
                Text(if (state is ScanState.Done) "Scan again" else "Scan my apps")
            }

            Spacer(Modifier.size(16.dp))

            when (state) {
                is ScanState.Idle -> HintText()
                is ScanState.Running -> RunningView(state)
                is ScanState.Done -> ResultsView(state.apps)
            }
        }
    }
}

@Composable
private fun HintText() {
    Text(
        "Tap Scan to check every installed app for the traits seen in " +
            "proxyware-laced apps: sideloaded origin, installer/overlay " +
            "permissions, boot persistence and bundled proxy engines.",
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun RunningView(state: ScanState.Running) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.size(24.dp))
        CircularProgressIndicator()
        Spacer(Modifier.size(12.dp))
        Text(
            "Scanning ${state.done} / ${state.total}",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(8.dp))
        LinearProgressIndicator(
            progress = {
                if (state.total == 0) 0f else state.done.toFloat() / state.total
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ResultsView(apps: List<ScannedApp>) {
    val summary = ScanViewModel.summarize(apps)
    val flagged = apps.filter { it.level != RiskLevel.SAFE }

    SummaryRow(summary)
    Spacer(Modifier.size(12.dp))

    if (flagged.isEmpty()) {
        Text(
            "Nothing suspicious found. Every scanned app came from a trusted " +
                "source with no proxyware traits.",
            color = RiskColors.Safe,
            fontSize = 14.sp
        )
        return
    }

    Text(
        "${flagged.size} app(s) worth a look",
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
    Spacer(Modifier.size(8.dp))

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items(flagged, key = { it.packageName }) { app -> AppCard(app) }
    }
}

@Composable
private fun SummaryRow(summary: Map<RiskLevel, Int>) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SummaryChip("High", summary[RiskLevel.HIGH] ?: 0, RiskColors.High, Modifier.weight(1f))
        SummaryChip("Medium", summary[RiskLevel.MEDIUM] ?: 0, RiskColors.Medium, Modifier.weight(1f))
        SummaryChip("Low", summary[RiskLevel.LOW] ?: 0, RiskColors.Low, Modifier.weight(1f))
        SummaryChip("Safe", summary[RiskLevel.SAFE] ?: 0, RiskColors.Safe, Modifier.weight(1f))
    }
}

@Composable
private fun SummaryChip(label: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("$count", color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

@Composable
private fun AppCard(app: ScannedApp) {
    var expanded by remember { mutableStateOf(app.level == RiskLevel.HIGH) }
    val color = when (app.level) {
        RiskLevel.HIGH -> RiskColors.High
        RiskLevel.MEDIUM -> RiskColors.Medium
        RiskLevel.LOW -> RiskColors.Low
        RiskLevel.SAFE -> RiskColors.Safe
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(10.dp).background(color, CircleShape)
                )
                Spacer(Modifier.size(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        app.label,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        app.packageName,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    Modifier
                        .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${app.level.name} · ${app.score}",
                        color = color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (expanded) {
                Spacer(Modifier.size(10.dp))
                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                Spacer(Modifier.size(8.dp))
                app.flags.forEach { flag ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        Text("•  ", color = color)
                        Column {
                            Text(
                                flag.title,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            flag.detail?.let {
                                Text(
                                    it,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
