package com.mattyinc.sidescan.scan

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import java.util.zip.ZipFile

/**
 * Reads installed-app metadata from PackageManager and, where needed, inspects
 * an app's own APK files (base + splits) for proxyware indicators. Entirely
 * local and read-only.
 */
class AppScanner(context: Context) {

    private val pm: PackageManager = context.packageManager

    /** Every installed package with permissions, services and receivers populated. */
    fun installedApps(includeSystem: Boolean): List<PackageInfo> {
        val flags = PackageManager.GET_PERMISSIONS or
            PackageManager.GET_SERVICES or
            PackageManager.GET_RECEIVERS
        val all: List<PackageInfo> = if (Build.VERSION.SDK_INT >= 33) {
            pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledPackages(flags)
        }
        return all.filter { pkg ->
            val ai = pkg.applicationInfo ?: return@filter false
            includeSystem || (ai.flags and ApplicationInfo.FLAG_SYSTEM) == 0
        }
    }

    /** The package that installed [packageName], or null if unknown/sideloaded. */
    fun installerOf(packageName: String): String? = try {
        if (Build.VERSION.SDK_INT >= 30) {
            pm.getInstallSourceInfo(packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            pm.getInstallerPackageName(packageName)
        }
    } catch (_: Exception) {
        null
    }

    fun label(ai: ApplicationInfo): String = try {
        pm.getApplicationLabel(ai).toString()
    } catch (_: Exception) {
        ai.packageName
    }

    /** Matches proxy-engine native libraries inside the app's APK(s). Cheap. */
    fun deepScanNativeLibs(ai: ApplicationInfo): List<String> {
        val found = linkedSetOf<String>()
        for (path in apkPaths(ai)) {
            try {
                ZipFile(path).use { zf ->
                    val entries = zf.entries()
                    while (entries.hasMoreElements()) {
                        val name = entries.nextElement().name
                        if (name.startsWith("lib/") && name.endsWith(".so")) {
                            val base = name.substringAfterLast('/')
                            Signatures.proxyNativeLibs[base]?.let { found.add(it) }
                        }
                    }
                }
            } catch (_: Exception) {
                // APK/split not readable on this device; skip it.
            }
        }
        return found.toList()
    }

    /**
     * Searches classes*.dex in the base APK for known proxy-SDK package paths.
     * More expensive than the native-lib scan, so callers run it selectively.
     */
    fun scanDexForSdks(ai: ApplicationInfo): List<String> {
        val path = ai.sourceDir ?: return emptyList()
        val needles: Map<String, String> =
            Signatures.proxySdkPackages.mapKeys { (pkg, _) -> pkg.replace('.', '/') }
        val found = linkedSetOf<String>()
        try {
            ZipFile(path).use { zf ->
                val entries = zf.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (!entry.name.endsWith(".dex")) continue
                    val text = zf.getInputStream(entry).use { input ->
                        String(input.readBytes(), Charsets.ISO_8859_1)
                    }
                    for ((slash, label) in needles) {
                        if (text.contains(slash)) found.add(label)
                    }
                    if (found.size == needles.values.toSet().size) break
                }
            }
        } catch (_: Exception) {
            // Unreadable or oversized; return whatever matched so far.
        }
        return found.toList()
    }

    private fun apkPaths(ai: ApplicationInfo): List<String> = buildList {
        ai.sourceDir?.let { add(it) }
        ai.splitSourceDirs?.let { splits -> addAll(splits.filterNotNull()) }
    }
}
