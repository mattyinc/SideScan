package com.mattyinc.sidescan.scan

/**
 * Known indicators of bandwidth-sharing ("proxyware") and dropper behaviour.
 *
 * The CashRaven entries were seeded directly from a teardown of a real
 * pirate-streaming APK (com.cricfytv.sports): the native engine
 * `libcr_android.so` and the `io.cashraven.sdk` package. The rest cover the
 * other widely-distributed proxyware SDKs.
 */
object Signatures {

    /** Native library file names (zip entries under lib/) belonging to proxy engines. */
    val proxyNativeLibs: Map<String, String> = mapOf(
        "libcr_android.so" to "CashRaven proxy engine",
        "libpws.so" to "IPRoyal Pawns",
        "libpawns.so" to "IPRoyal Pawns",
        "libluminati.so" to "Bright Data / Luminati",
        "libp2p.so" to "Peer-to-peer proxy engine"
    )

    /**
     * SDK package paths (dotted) searched for inside classes*.dex. DEX stores
     * type descriptors like `Lio/cashraven/sdk/CashRaven;`, so a byte search for
     * the slash form reliably matches without a full DEX parse.
     */
    val proxySdkPackages: Map<String, String> = mapOf(
        "io.cashraven" to "CashRaven bandwidth-sharing SDK",
        "com.iproyal" to "IPRoyal Pawns SDK",
        "io.pawns" to "IPRoyal Pawns SDK",
        "com.pawns" to "Pawns SDK",
        "com.luminati" to "Bright Data (Luminati) SDK",
        "com.brightdata" to "Bright Data SDK",
        "com.peer2profit" to "Peer2Profit SDK",
        "sdk.peer2profit" to "Peer2Profit SDK",
        "com.infatica" to "Infatica P2B SDK",
        "io.wannads" to "WannaMobile proxy SDK",
        "com.packetshare" to "PacketShare SDK"
    )

    /** Permissions that grant installer/dropper or overlay power (high weight). */
    val highRiskPermissions: Map<String, String> = mapOf(
        "android.permission.REQUEST_INSTALL_PACKAGES" to "Can install other apps (dropper capability)",
        "android.permission.REQUEST_DELETE_PACKAGES" to "Can uninstall other apps",
        "android.permission.SYSTEM_ALERT_WINDOW" to "Can draw overlays over other apps"
    )

    /** Permissions that raise suspicion in combination (lower weight). */
    val mediumRiskPermissions: Map<String, String> = mapOf(
        "android.permission.QUERY_ALL_PACKAGES" to "Can enumerate all installed apps",
        "android.permission.RECEIVE_BOOT_COMPLETED" to "Starts itself on boot"
    )

    /** Installer package names that count as a vetted app store. */
    val trustedStoreInstallers: Set<String> = setOf(
        "com.android.vending",            // Google Play
        "com.google.android.feedback",    // Play (legacy)
        "com.sec.android.app.samsungapps", // Samsung Galaxy Store
        "com.amazon.venezia",             // Amazon Appstore
        "com.huawei.appmarket",           // Huawei AppGallery
        "org.fdroid.fdroid"               // F-Droid
    )

    /** Package name prefixes that indicate an OEM / platform app, not a user install. */
    val systemPackagePrefixes: List<String> = listOf(
        "android", "com.android.", "com.google.android.",
        "com.samsung.", "com.sec.", "com.samsung.android.",
        "com.qualcomm.", "com.mediatek.", "com.knox.", "com.sec.android."
    )
}
