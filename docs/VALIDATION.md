# Validation scope

## Automated checks

```sh
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

The 1.3.0 codebase has **92 JVM unit tests**, covering:

- Work consumption, countdown formats, boost boundaries, potions/snacks, Clock Tower, helpers and optional repeat forecasts.
- Export validation, the anonymized reported TH18 JSON, nested crafted modules, inherited Goblin flags, optional names and Builder Hall capacity.
- Account migration/isolation, widget grid sizing, next-builder/research summaries and shared Forge builder slots.
- Reminder scheduling, early alerts, once-only delivery, account-specific keys and boost recalculation.
- Clipboard export identity, duplicate/older comparisons, unrelated/future input rejection, stable release comparison, APK availability and daily update throttling.

The Town Hall 1–19 test varies the Town Hall field in one fixture. It does not validate every real village layout or future game update. Lint passes with warnings; this is not a claim of warning-free code.

## Emulator checks

Stock Android 13 / API 33 and Android 16 / API 36 were used during development of this release:

- Clipboard button → preview; native rich-text paste; keyboard commit; bounded visible text; clearing a loaded export.
- Full foreground suggestion workflow: opt-in, Review, explicit import of a synthetic running crafted module, duplicate suppression, older warnings, dismissal and matching another tag without changing the selected village prematurely.
- Account data restoration after test imports; manual Forge persistence across re-import. Adding a Forge job through the UI updates shared Home builder occupancy.
- Cached newer/older update banners. Live GitHub checks correctly recognized that the local build was newer than published 1.2.1.
- Five-minute early-alert setting and custom ringtone URI persistence, with original settings restored afterward.
- Local crash diagnostics capture stack frames without exception messages or village JSON.

Earlier API 36 checks cover migration, multiple villages, compact/detailed widgets, resizing, editor navigation, countdowns, notification actions and delivery after reboot. Physical audio/vibration, OEM battery policies, different launchers and accessibility settings still need broader coverage.

### Instrumentation commands

After installing the main debug APK, build and install the test APK:

```sh
./gradlew :app:assembleDebugAndroidTest
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w org.clashledger.app.test/org.clashledger.app.ImportSmoke
```

For the clipboard/update workflow:

```sh
./gradlew :app:assembleDebugAndroidTest -PtestRunner=org.clashledger.app.ConvenienceSmoke
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w org.clashledger.app.test/org.clashledger.app.ConvenienceSmoke
```

Use a test emulator. ConvenienceSmoke temporarily changes preferences/accounts and restores their snapshots. Clipboard contents and Android system state may change during tests.

## Known limits

- Crash diagnostics saves the latest Java crash locally for optional sharing. Emulator checks do not establish reliability on every physical device; diagnostic reports can help investigate failures.
- The supplied real export contains crafted-defense definitions without active module timers. Active-module and inherited-Goblin tests are synthetic; another export shape may need additional support.
- Active Forge data was absent from supplied exports. Forge/Auto Forge tracking is manual and survives re-import; review those timers after game actions. Builder Bite effects on Forge are not assumed.
- A real active Pet Potion export is still needed to confirm its export key. Manual recording is available. Other boost combinations have synthetic coverage.
- Apprentice/Lab Assistant active sessions use exported data; future repeated sessions are optional estimates. Other helpers are not modeled.
- Event multipliers are manual. Exported timers may already include discounts, which must not be applied again.
- Unknown IDs retain editable placeholder names and fallback artwork. Artwork may depict a different level. Helper Hut, Crafting Station and some other items still lack matching art.
- Progress starts from remaining work at import, not an upgrade-duration catalogue.
- Notifications may be delayed by device restrictions. Physical custom-audio playback, persisted document permissions on OEM devices and Do Not Disturb need real-device testing.
- Clipboard suggestions read only when focused and opted in. Dismissal history is bounded to 64 export identities. Import remains explicit.
- Automatic update checks run when the app opens, at most once per day, not continuously in the background. Download/install stays user-controlled.
- Game activity is not automatically observed. New actions require an export or manual edit; inventory-based tracking remains a proposal.

## Implementation references

- [GitHub latest-release API](https://docs.github.com/en/rest/releases/releases#get-the-latest-release)
- [Android ringtone picker](https://developer.android.com/reference/android/media/RingtoneManager)
- [Android notification channels](https://developer.android.com/develop/ui/views/notifications/channels)
- [Android clipboard behavior](https://developer.android.com/develop/ui/views/touch-and-input/copy-paste)
- [Supercell Forge guide](https://support.supercell.com/clash-of-clans/en/articles/the-forge.html)
- [Apprentice restrictions](https://supercell.com/en/games/clashofclans/blog/release-notes/june-update-full-patch-notes/)
