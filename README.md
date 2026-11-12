# SideScan

A small, local, read-only Android app that scans every installed app on your
device and flags the ones that behave like **proxyware** (bandwidth-sharing
SDKs) or **droppers** (apps that can install other apps). Built after tearing
down a real pirate-streaming APK that bundled the `CashRaven` proxy engine.

**SideScan never uninstalls anything, changes anything, or sends anything
off-device.** It only reads and reports.

## What it checks

For every installed app it combines PackageManager metadata with a look inside
the app's own APK:

- **Install source** — installed outside the Play Store (biggest single signal)
- **Permissions** — `REQUEST_INSTALL_PACKAGES`, `REQUEST_DELETE_PACKAGES`,
  `SYSTEM_ALERT_WINDOW` (installer/overlay power), plus `QUERY_ALL_PACKAGES`
  and `RECEIVE_BOOT_COMPLETED`
- **Cleartext traffic** — unencrypted HTTP allowed
- **Boot persistence** — a boot receiver that auto-starts the app
- **Bundled proxy engines** — known native libs such as `libcr_android.so`
  (CashRaven), by reading `lib/*.so` entries in the APK
- **Proxy SDKs in code** — known package paths (e.g. `io.cashraven`, IPRoyal
  Pawns, Bright Data, Peer2Profit) found in `classes*.dex`

Each app gets a weighted score and a verdict: **Safe / Low / Medium / High**,
with the exact reasons listed on each card.

## Why it needs `QUERY_ALL_PACKAGES`

On Android 11+ this permission is required to enumerate all installed apps.
SideScan uses it only for this local scan. It's the one ironic permission the
tool itself requests — and it requests nothing else.

## Building

Pushes to `main` trigger the **Build APK** GitHub Actions workflow, which
produces `app-debug.apk` as a downloadable artifact (Actions run → Artifacts →
`SideScan-debug-apk`). You can also run it manually from the Actions tab
(**Run workflow**).

Locally:

```bash
./gradlew assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk
```

Then install on your device (`adb install app-debug.apk`, or copy it over and
open it). It's a debug build signed with the standard debug key.

## Stack

Kotlin · Jetpack Compose (Material 3) · min SDK 26 · target SDK 35 · JDK 17

## Limitations

- The dex SDK scan is a byte search for known package paths; a pure-Java SDK
  that's been heavily renamed could be missed. The permission/install-source
  heuristics still catch the host app.
- Some devices restrict reading other apps' split APKs; those are skipped.
- Signatures are a starting set (seeded from the CashRaven teardown). Add more
  in `app/src/main/java/com/mattyinc/sidescan/scan/Signatures.kt`.
