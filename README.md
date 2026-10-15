# WordCraft for Android

A native Android word processor built on the **real WordCraft document engine** — the Rust
core of [storytold/wordcraft](https://github.com/storytold/wordcraft) (document model, DOCX
reader/writer, command registry, undo/redo), compiled to `libwordcraft_core.so` and driven
from a Jetpack Compose UI over a thin JNI bridge.

**Not a WebView.** The editor, toolbar, file picker, clipboard and keyboard are all native
Android; the document truth lives in the unmodified upstream Rust crates.

```
Android phone
   ↓
WordCraft-Android-*.apk  (Kotlin + Jetpack Compose, minSdk 29 / Android 10, arm64-v8a)
   ↓
libwordcraft_core.so     (JNI bridge, rust-core/)
   ↓
wordcraft-engine · wordcraft-docx · wordcraft-doc  (unmodified upstream crates)
   ↓
Open / Edit / Save  .docx
```

## Layout

| Path | What it is |
|---|---|
| `app/` | MainActivity, SAF file flows (new/open/save/save-as/share), recents, top bar |
| `editor/` | Compose editor surface: sections, tables, format toolbar, find/replace |
| `document-core/` | JNI bridge + engine wrapper + pure edit logic (diff → commands) + models |
| `storage/` | Recent documents (SharedPreferences) |
| `ui/` | Theme (light/dark) and shared colors |
| `rust-core/` | `wordcraft-ffi` crate: path-depends on the upstream `wordcraft/` crates |
| `../wordcraft/` | The pristine upstream WordCraft checkout (never modified) |

## System installation

### Prerequisites

Install the following on your development machine before building:

| Tool | Version | Notes |
|---|---|---|
| **JDK** | 17 | `JAVA_HOME` must point to it |
| **Android SDK** | platform 35 + build-tools 35.0.0 | `ANDROID_HOME` must point to it |
| **Android NDK** | 26.3.11579264 | `ndk.dir` matches `ANDROID_HOME` |
| **Gradle** | 8.10.2 | (via `gradle/wrapper`, no manual install needed) |
| **Rust** | stable | with the `aarch64-linux-android` target |

> **NDK flavor note (Windows):** the project pins the NDK's `clang.exe` directly in
> `rust-core/.cargo/config.toml` rather than using the wrapper scripts, because the
> wrappers break when paths contain spaces. The NDK is the only C toolchain required.

### 1. Clone and enter the repo

```bash
git clone <repo-url>
cd wordcraft-android
```

### 2. Set environment variables

Create/refresh a `local.properties` in the repo root (do not commit it):

```properties
sdk.dir=/path/to/Android/Sdk
ndk.dir=/path/to/Android/Sdk/ndk/26.3.11579264
```

Then export on every shell you run Gradle in:

```bash
export JAVA_HOME=/path/to/jdk-17
export ANDROID_HOME=/path/to/Android/Sdk
export PATH="$JAVA_HOME/bin:$PATH"
```

### 3. Install the Rust target (if not already)

```bash
rustup target add aarch64-linux-android
```

### 4. (Optional) Generate the DOCX fixture files

The round-trip tests write fixtures to `tests/fixtures/` on first run:

```bash
cd rust-core && cargo test -- --test-threads=1
```

This produces `tests/fixtures/rich.docx` and `tests/fixtures/simple.docx`, which the app
tests against at runtime.

## Build the APKs

```bash
# 1. Engine (.so) — cross-compile the JNI library into the APK's jniLibs
cd rust-core
cargo build --release --target aarch64-linux-android

# 2. Kotlin + Compose
cd ..
./gradlew assembleDebug      # → app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleRelease    # → app/build/outputs/apk/release/app-release.apk (signed locally)
```

The Gradle wrapper runs Gradle 8.10.2 — no manual Gradle install is needed.

## Install

```bash
# Debug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Release (signed with the local release.keystore)
adb install -r app/build/outputs/apk/release/app-release.apk
```

Requires an **ARM64 (arm64-v8a)** device running Android 10 (API 29) or newer. For a debug
build you can also use Android Studio → Run.

## Using the editor

- Type with the soft keyboard or a hardware keyboard; Enter starts a new paragraph,
  Backspace merges / leaves lists exactly like desktop WordCraft (all through the engine's
  own commands).
- Toolbar: **B I U S · Align · + · ⋮** — "+" inserts (table, link, image, page break, lists),
  "⋮" opens the full sheet (styles, font, size, colours, highlight, indents, word count).
- **Find/replace** via the toolbar search or Ctrl+F; undo/redo via toolbar sheet or Ctrl+Z/Y.
- File menu: New, Open… (system picker), Save / Save as…, Share, Recent documents.

## Download the APKs

Ready-made builds are attached to the GitHub releases:

- **Debug (install directly):** [WordCraft-v0.1.0-debug.apk](https://github.com/itsupportgoc-alt/android-app/releases/download/v0.1.0/WordCraft-v0.1.0-debug.apk)
- All releases: https://github.com/itsupportgoc-alt/android-app/releases

Or, after a local build, grab them from:

- `app/build/outputs/apk/debug/app-debug.apk` — debug build
- `app/build/outputs/apk/release/app-release.apk` — release build (signed with the local keystore)
- `app/src/main/jniLibs/arm64-v8a/libwordcraft_core.so` — the JNI engine library (copied in by
  the Gradle build at preBuild; do not hand-edit)

## Known limitations

- Editing happens in reflow mode (like Word mobile's reflow view), not paginated print layout;
  pagination/PDF export code is compiled in but not yet surfaced in the UI.
- Cross-paragraph drag selection and paragraph-level spacing *inside* one style run are
  approximated; paragraphs split into separate fields at every style/alignment/list change.
- Read Aloud, printing, track-changes review UI, mail merge and MCP are desktop-only and
  not exposed on Android.
- Password-protected documents prompt an error (decryption is in the engine; no password UI yet).

## Contributing

- The upstream `wordcraft/` engine is consumed **unmodified** via path-dependencies — never edit
  the `../wordcraft/` sources.
- View-model JSON crosses the JNI boundary as UTF-8 byte arrays; keep the 7
  `Java_com_wordcraft_documentcore_WordCraftNativeKt_*` exports in sync with the `external fun`
  declarations in `WordCraftNative.kt`.
- Tests: `./gradlew test` (Kotlin) and `cargo test` (Rust, host).
