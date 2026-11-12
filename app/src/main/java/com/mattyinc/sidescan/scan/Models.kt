package com.mattyinc.sidescan.scan

/** Overall verdict for a scanned app, from lowest to highest concern. */
enum class RiskLevel { SAFE, LOW, MEDIUM, HIGH }

/** A single supporting signal, with the weight it contributed to the score. */
data class RiskFlag(
    val id: String,
    val title: String,
    val weight: Int,
    val detail: String? = null
)

/** The result of evaluating one installed package. */
data class ScannedApp(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val installerPackage: String?,
    val isSystem: Boolean,
    val fromStore: Boolean,
    /** One-line reason this app landed where it did (e.g. "Bundles a proxy engine"). */
    val headline: String,
    /** Plain-language explanation of why it's concerning (or why it's fine). */
    val verdict: String,
    /** Supporting technical signals, shown under "details". */
    val flags: List<RiskFlag>,
    val score: Int,
    val level: RiskLevel
)
