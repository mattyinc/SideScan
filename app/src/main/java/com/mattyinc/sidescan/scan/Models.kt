package com.mattyinc.sidescan.scan

/** Overall verdict for a scanned app, from lowest to highest concern. */
enum class RiskLevel { SAFE, LOW, MEDIUM, HIGH }

/** A single reason an app was flagged, with the weight it contributed to the score. */
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
    val fromPlayStore: Boolean,
    val flags: List<RiskFlag>,
    val score: Int,
    val level: RiskLevel
)
