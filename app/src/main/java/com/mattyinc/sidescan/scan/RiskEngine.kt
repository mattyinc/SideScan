package com.mattyinc.sidescan.scan

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo

/**
 * Evaluates one installed package into a [ScannedApp].
 *
 * Key idea: a permission on its own means little — almost every app holds a few.
 * What matters is the *combination* and, above all, hard evidence of a bundled
 * proxy engine. So permissions are weighted heavily only when the app was
 * sideloaded (installed outside a vetted store, where nothing checked it), and
 * barely at all when it came from Google Play / Galaxy Store. Each app also gets
 * a plain-language verdict explaining *why* it landed where it did.
 */
class RiskEngine(private val scanner: AppScanner) {

    fun evaluate(pkg: PackageInfo): ScannedApp {
        val ai = pkg.applicationInfo ?: return unknown(pkg)

        val installer = scanner.installerOf(pkg.packageName)
        val isSystem = scanner.isSystemLike(pkg.packageName, ai)
        val fromStore = installer in Signatures.trustedStoreInstallers
        val sideloaded = !isSystem && !fromStore

        // --- Signals -------------------------------------------------------
        val requested = pkg.requestedPermissions?.toSet() ?: emptySet()
        val hasInstall = "android.permission.REQUEST_INSTALL_PACKAGES" in requested
        val hasDelete = "android.permission.REQUEST_DELETE_PACKAGES" in requested
        val hasOverlay = "android.permission.SYSTEM_ALERT_WINDOW" in requested
        val hasQueryAll = "android.permission.QUERY_ALL_PACKAGES" in requested
        val cleartext = (ai.flags and ApplicationInfo.FLAG_USES_CLEARTEXT_TRAFFIC) != 0
        val hasBoot = "android.permission.RECEIVE_BOOT_COMPLETED" in requested ||
            pkg.receivers?.any { it.name?.contains("boot", ignoreCase = true) == true } == true

        val proxyLibs = scanner.deepScanNativeLibs(ai)
        // The dex scan is expensive; only run it where it could matter.
        val proxySdks = if (!isSystem && (sideloaded || proxyLibs.isNotEmpty())) {
            scanner.scanDexForSdks(ai)
        } else emptyList()
        val proxy = (proxyLibs + proxySdks).distinct()

        // --- Score ---------------------------------------------------------
        // Permissions count at full weight only when sideloaded; heavily
        // discounted for store/system apps that were vetted.
        val tech = mutableListOf<RiskFlag>()
        var score = 0
        fun add(id: String, title: String, sideWeight: Int, storeWeight: Int, detail: String? = null) {
            val w = if (sideloaded) sideWeight else storeWeight
            if (w > 0) { score += w; tech += RiskFlag(id, title, w, detail) }
        }

        if (proxy.isNotEmpty()) {
            score += 55
            proxy.forEach { tech += RiskFlag("proxy:$it", "Bundled proxy/bandwidth-sharing engine: $it", 55) }
        }
        if (sideloaded) {
            tech += RiskFlag("sideloaded", "Installed outside a vetted app store", 18,
                installer?.let { "Installer: $it" } ?: "No installer recorded (sideloaded or adb)")
            score += 18
        }
        add("install", "Can install other apps (dropper capability)", 22, 6)
        add("delete", "Can uninstall other apps", 10, 2)
        add("overlay", "Can draw over other apps (overlay)", 12, 3)
        add("boot", "Auto-starts on boot", 6, 1)
        add("cleartext", "Allows unencrypted HTTP traffic", 4, 1)
        // QUERY_ALL_PACKAGES is informational — common and weak on its own.
        if (hasQueryAll) { score += 2; tech += RiskFlag("queryall", "Can list all installed apps", 2) }

        score = score.coerceIn(0, 100)
        val level = when {
            proxy.isNotEmpty() -> RiskLevel.HIGH          // bundled proxyware is always High
            score >= 60 -> RiskLevel.HIGH
            score >= 38 -> RiskLevel.MEDIUM
            score >= 18 -> RiskLevel.LOW
            else -> RiskLevel.SAFE
        }

        val headline = buildHeadline(proxy, sideloaded, fromStore, hasInstall, hasOverlay, level)
        val verdict = buildVerdict(proxy, sideloaded, fromStore, hasInstall, hasDelete, hasOverlay, hasBoot, cleartext, level)

        return ScannedApp(
            packageName = pkg.packageName,
            label = scanner.label(ai),
            versionName = pkg.versionName,
            installerPackage = installer,
            isSystem = isSystem,
            fromStore = fromStore,
            headline = headline,
            verdict = verdict,
            flags = tech.sortedByDescending { it.weight },
            score = score,
            level = level
        )
    }

    private fun buildHeadline(
        proxy: List<String>, sideloaded: Boolean, fromStore: Boolean,
        hasInstall: Boolean, hasOverlay: Boolean, level: RiskLevel
    ): String = when {
        proxy.isNotEmpty() -> "Bundles a bandwidth-sharing proxy engine"
        sideloaded && hasInstall -> "Sideloaded app that can install other apps"
        sideloaded && hasOverlay -> "Sideloaded app that can overlay your screen"
        sideloaded -> "Installed from outside a vetted store"
        level == RiskLevel.SAFE -> "No proxyware traits found"
        else -> "Holds some powerful permissions"
    }

    /** The explanation a person actually reads — why this app is (or isn't) a concern. */
    private fun buildVerdict(
        proxy: List<String>, sideloaded: Boolean, fromStore: Boolean,
        hasInstall: Boolean, hasDelete: Boolean, hasOverlay: Boolean,
        hasBoot: Boolean, cleartext: Boolean, level: RiskLevel
    ): String {
        val parts = mutableListOf<String>()

        if (proxy.isNotEmpty()) {
            parts += "This app bundles a bandwidth-sharing proxy engine (${proxy.joinToString(", ")}) — " +
                "the same proxyware pattern found in CricFy. Your device's internet connection and IP " +
                "address get relayed for third parties, usually when the phone is idle or charging. " +
                "This is the decisive red flag."
        }
        if (sideloaded) {
            parts += "It was installed from outside a vetted app store, so nothing checked what it does " +
                "before it reached your phone."
        }
        val dropper = buildList { if (hasInstall) add("install"); if (hasDelete) add("remove") }
        if (dropper.isNotEmpty()) {
            parts += "It can ${dropper.joinToString(" and ")} other apps on your device — dropper-style " +
                "capability that lets it pull in further software."
        }
        if (hasOverlay) {
            parts += "It can draw over other apps, which can be used for fake prompts or forced ads."
        }
        if (hasBoot) {
            parts += "It restarts itself on boot to keep running in the background."
        }
        if (cleartext && parts.isNotEmpty()) {
            parts += "It also permits unencrypted HTTP traffic."
        }

        if (parts.isEmpty()) {
            return if (fromStore) {
                "From a vetted store with no proxyware traits. Listed only so you can see the permissions it holds."
            } else {
                "No proxyware engine or dropper pattern detected."
            }
        }

        // Closing guidance tied to the verdict level.
        parts += when (level) {
            RiskLevel.HIGH -> "Recommended: uninstall it unless you specifically trust it."
            RiskLevel.MEDIUM -> "Worth reviewing — confirm you trust the source and need these capabilities."
            else -> "Low concern on its own, but worth knowing where it came from."
        }
        return parts.joinToString(" ")
    }

    private fun unknown(pkg: PackageInfo): ScannedApp = ScannedApp(
        packageName = pkg.packageName,
        label = pkg.packageName,
        versionName = pkg.versionName,
        installerPackage = null,
        isSystem = false,
        fromStore = false,
        headline = "Could not be read",
        verdict = "This app's details could not be read on this device.",
        flags = emptyList(),
        score = 0,
        level = RiskLevel.SAFE
    )
}
