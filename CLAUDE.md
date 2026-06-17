# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Monical is a native Android app (Kotlin + Jetpack Compose, Material 3) for **scanning purchase receipts/tickets**. The flow: capture a receipt with the camera or pick one from the gallery → extract the raw text on-device with **ML Kit** → structure it (date, total, line-item products) with **Gemini 2.5 Flash** via Firebase AI Logic, falling back to a regex parser when offline → let the user **review/edit** the result → save it to **Cloud Firestore**, scoped per user via **Firebase Anonymous Auth**.

Application ID and namespace: `com.devsoto.monical`. `minSdk 33`, `targetSdk`/`compileSdk 36`, Java 11 source/target.

## Commands

Use the Gradle wrapper (`gradlew.bat` on Windows / `./gradlew` elsewhere). All commands run from the repo root.

- Build debug APK: `gradlew.bat assembleDebug`
- Full build: `gradlew.bat build`
- Install on a connected device/emulator: `gradlew.bat installDebug`
- Clean: `gradlew.bat clean`
- Unit tests (JVM, `app/src/test`): `gradlew.bat test` — or just the debug variant with `gradlew.bat testDebugUnitTest`
- Run a single unit test: `gradlew.bat testDebugUnitTest --tests "com.devsoto.monical.RegexReceiptParserTest"` (append `.methodName` to target one method)
- Instrumented tests (require a running device/emulator, `app/src/androidTest`): `gradlew.bat connectedAndroidTest`
- Run a single instrumented test: `gradlew.bat connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.devsoto.monical.ExampleInstrumentedTest`

There is no separate lint/format step configured beyond the Android Gradle Plugin defaults; `gradlew.bat lint` runs Android Lint.

## Architecture & conventions

MVVM + Compose, single Gradle module `:app`, manual DI (no Hilt). Code is split into `data/` and `ui/` under `com.devsoto.monical`.

- **Build config:** Dependency versions are centralized in the version catalog at [gradle/libs.versions.toml](gradle/libs.versions.toml) and referenced as `libs.*` aliases in [app/build.gradle.kts](app/build.gradle.kts). Add or bump dependencies there, not inline. `RepositoriesMode.FAIL_ON_PROJECT_REPOS` is set, so declare repositories only in [settings.gradle.kts](settings.gradle.kts).
- **DI:** [AppContainer.kt](app/src/main/java/com/devsoto/monical/AppContainer.kt) constructs the data-layer singletons; it's created in [MonicalApplication.kt](app/src/main/java/com/devsoto/monical/MonicalApplication.kt) and reached from ViewModels via `ScanViewModel.Factory`.
- **Scan flow:** A single shared [ScanViewModel](app/src/main/java/com/devsoto/monical/ui/scan/ScanViewModel.kt) holds the flow state (`ScanUiState`/`ScanPhase`) and is shared across the capture and review screens so the parsed `ReceiptDraft` survives navigation without serializing it through nav args. [MonicalNavHost](app/src/main/java/com/devsoto/monical/ui/navigation/MonicalNavHost.kt) navigates by observing `ScanPhase`.
- **Data layer (`data/`):** `model/` (`Receipt`, `ReceiptItem`, `ReceiptDraft`; plus `ReceiptCard`/`ReceiptSummary`/`MonthlyRollup` and the pure `SummaryMath` aggregation helpers); `ocr/MlKitTextRecognizer`; `parse/` — the `ReceiptParser` interface with `GeminiReceiptParser` (Firebase AI Logic, model `gemini-2.5-flash`), `RegexReceiptParser` (offline fallback), and `ReceiptParserRouter` (tries Gemini, falls back on any exception). `ReceiptJsonMapper` maps Gemini JSON → draft and is pure/JVM-testable. `refine/` — `ReceiptPostProcessor` applies the learned `CorrectionDictionary` + future-year date fix between parse and review; `learnCorrections` diffs the raw vs. edited draft to grow the dictionary. `auth/AuthManager` (anonymous sign-in); `repository/` — `ReceiptRepository` + `FirestoreReceiptRepository`, `CorrectionRepository` + `FirestoreCorrectionRepository`, and `SettingsRepository` + `FirestoreSettingsRepository`.
- **Firestore read budget:** to keep reads cheap, Home observes a single denormalized doc `users/{uid}/meta/summary` (pending totals + lightweight `ReceiptCard` list + monthly archive rollups) instead of the whole `receipts` collection; opening a receipt fetches one full doc. Active (PENDING) docs live in `users/{uid}/receipts/{id}`; archived (RETURNED/NONE) docs move to the cold `users/{uid}/archive/{id}` collection. Every mutation runs in a transaction that also updates `meta/summary` via `SummaryMath`. The learned dictionary is the single doc `users/{uid}/meta/corrections`, loaded once and cached in `ScanViewModel`.
- **Return %% (devolución) setting:** the configurable share lives in `UserSettings` (`data/model`), persisted as the single doc `users/{uid}/meta/settings` (field `returnShare`, default `DEFAULT_RETURN_SHARE = 0.75`) via `SettingsRepository`, and edited on the **Settings** screen (`ui/settings`) with the integer-only `PercentageCalculator`. It's **forward-only**: pending receipts use the current global share, derived live in the UI (`pendingTotal * share`); when a receipt is archived, the share is **frozen** onto `Receipt.returnShare` (stamped by `FirestoreReceiptRepository` at archive time) and `SummaryMath.archivedRefund` uses that stamped value, so changing the setting never rewrites returned history. `SummaryMath` itself stays share-free; `ReceiptSummary` no longer stores a `pendingRefund`.
- **UI (`ui/`):** 100% Compose — no XML layouts, no Fragments. `enableEdgeToEdge()` is on, so account for insets (root `Scaffold` provides `innerPadding`). Theme in `ui/theme/`; wrap trees in `MonicalTheme { }`. Camera capture uses `ActivityResultContracts.TakePicture` writing to a `FileProvider` Uri (see `ui/capture/ImageCapture.kt` + `res/xml/file_paths.xml`); gallery uses the `PickVisualMedia` Photo Picker.
- **Parser design note:** keep `RegexReceiptParser`/`ReceiptJsonMapper`, `SummaryMath`, and the `refine/` module free of Android dependencies so they stay unit-testable on the JVM (`org.json` is pulled in as a `testImplementation`).
- **Tests:** JUnit 4 for unit tests; Espresso + Compose UI test and `AndroidJUnitRunner` for instrumented tests. The valuable unit coverage today is the offline parser and JSON mapper, the summary aggregation, and the post-processing/learning (`RegexReceiptParserTest`, `ReceiptJsonMapperTest`, `SummaryMathTest`, `ReturnLogicTest`, `ReceiptPostProcessorTest`, `CorrectionLearningTest`, `PercentageInputTest`).

## Firebase setup (required to build & run)

The `com.google.gms.google-services` plugin is applied, so a `app/google-services.json` **must exist** or the build fails. It is git-ignored — each developer supplies their own from a Firebase project registered with package `com.devsoto.monical`. A placeholder file is committed locally to allow compilation/unit tests; replace it with the real one for any Firestore/Gemini functionality. In the Firebase console enable: **Authentication → Anonymous**, **Cloud Firestore**, and **Firebase AI Logic (Gemini)**. Suggested Firestore rule: allow read/write on `users/{uid}/{document=**}` only when `request.auth.uid == uid` (covers the `receipts`, `archive`, and `meta` subtrees).

## Local setup notes

`local.properties` (git-ignored) holds the `sdk.dir` path and is required for builds; it is machine-specific and must exist locally.
