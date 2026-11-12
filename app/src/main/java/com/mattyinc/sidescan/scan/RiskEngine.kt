package com.mattyinc.sidescan.scan

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo

/**
 * Turns one installed package into a [ScannedApp] verdict by combining
 * PackageManager heuristics (install source, permissions, components) with an
 * APK inspection for proxyware engines and SDKs.
 */
class RiskEngine(private val scanner: AppScanner) {

    fun evaluate(pkg: PackageInfo): ScannedApp {
        val ai = pkg.applicationInfo ?: return unknown(pkg)
        val flags = mutableListOf<RiskFlag>()

        val installer = scanner.installerOf(pkg.packageName)
        val fromPlay = installer in Signatures.playStoreInstallers
        val isSystem = (ai.flags and ApplicationInfo.FLAG_SYSTEM) != 0

        // 1. Install source — the single strongest signal.
        if (!isSystem && !fromPlay) {
            flags += RiskFlag(
                id = "sideloaded",
                title = "Installed outside the Play Store",
                weight = 25,
                detail = installer?.let { "Installer: $it" }
                    ?: "No installer recorded (sideloaded or adb)"
            )
        }

        // 2. Permissions.
        val requested: Set<String> = pkg.requestedPermissions?.toSet() ?: emptySet()
        Signatures.highRiskPermissions.forEach { (perm, desc) ->
            if (perm in requested) flags += RiskFlag("perm:$perm", desc, 20)
        }
        Signatures.mediumRiskPermissions.forEach { (perm, desc) ->
            if (perm in requested) flags += RiskFlag("perm:$perm", desc, 8)
        }

        // 3. Cleartext HTTP allowed.
        if ((ai.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0) {
            flags += RiskFlag("cleartext", "Allows unencrypted HTTP traffic", 5)
        }

        // 4. Boot receiver (always-on / restart pattern).
        val hasBootReceiver = pkg.receivers?.any {
            it.name?.contains("boot", ignoreCase = true) == true
        } == true
        if (hasBootReceiver) {
            flags += RiskFlag("bootrecv", "Has a boot receiver (auto-start)", 6)
        }

        // 5. Bundled proxy-engine native libraries (cheap).
        scanner.deepScanNativeLibs(ai).forEach {
            flags += RiskFlag("lib:$it", "Bundled proxyware engine: $it", 40)
        }

        // 6. Proxy SDK in dex — only when something already looks off.
        val prelimScore = flags.sumOf { it.weight }
        if (!isSystem && (prelimScore >= 20 || !fromPlay)) {
            scanner.scanDexForSdks(ai).forEach {
                flags += RiskFlag("sdk:$it", "Bandwidth-sharing SDK detected: $it", 40)
            }
        }

        val score = flags.sumOf { it.weight }.coerceIn(0, 100)
        return ScannedApp(
            packageName = pkg.packageName,
            label = scanner.label(ai),
            versionName = pkg.versionName,
            installerPackage = installer,
            isSystem = isSystem,
            fromPlayStore = fromPlay,
            flags = flags.sortedByDescending { it.weight },
            score = score,
            level = levelFor(score)
        )
    }

    private fun levelFor(score: Int): RiskLevel = when {
        score >= 60 -> RiskLevel.HIGH
        score >= 30 -> RiskLevel.MEDIUM
        score >= 12 -> RiskLevel.LOW
        else -> RiskLevel.SAFE
    }

    private fun unknown(pkg: PackageInfo): ScannedApp = ScannedApp(
        packageName = pkg.packageName,
        label = pkg.packageName,
        versionName = pkg.versionName,
        installerPackage = null,
        isSystem = false,
        fromPlayStore = false,
        flags = emptyList(),
        score = 0,
        level = RiskLevel.SAFE
    )
}
