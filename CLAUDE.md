# What the Fit (直男穿搭)

Offline Android outfit randomizer: check body-part categories → randomly draw one available item per part.

## Stack
Kotlin · Jetpack Compose (Material 3) · MVVM + Repository · Room · Paging 3 · Hilt · DataStore.
Single `:app` module (`com.txwstudio.app.whatthefit`), minSdk 26 / compileSdk 36.

## Commands
```
./gradlew :app:assembleDebug              # build
./gradlew :app:testDebugUnitTest          # unit tests (generator logic)
./gradlew :app:connectedDebugAndroidTest  # DAO tests (needs emulator/device)
```

## Architecture
- `Category` = parts; it drives generation (`domain/OutfitGenerator`).
- Brand / color / occasion are generic `Tag`s (kind-discriminated), linked to items via a cross-ref.

## Must-know
- **Don't bump the Room DB version.** Pre-1.0: on any schema change, nuke instead — `adb uninstall com.txwstudio.app.whatthefit` then reinstall (re-seeds defaults).
- **Keep `android.disallowKotlinSourceSets=false`** in `gradle.properties` — AGP 9's built-in Kotlin needs it for KSP (Room/Hilt).
- **All user-facing text → `strings.xml`** (en default + `values-zh-rTW` + `values-zh-rCN`); no hardcoded strings.
- **Language is OS-managed** (per-app locale via `res/xml/locales_config.xml`); Settings deep-links to the system screen. **Material You is always on.** Don't re-add in-app pickers/toggles.
- Commit/PR messages: zh-TW, concise, professional, no emojis.
