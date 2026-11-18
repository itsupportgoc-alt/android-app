# DevWord for Android

**DevWord** is a native Android word processor built on the **real WordCraft document
engine** — the Rust core of [storytold/wordcraft](https://github.com/storytold/wordcraft)
(document model, DOCX reader/writer, command registry, undo/redo), compiled to
`libwordcraft_core.so` and driven from a Jetpack Compose UI over a thin JNI bridge.

**Not a WebView.** The editor, toolbar, file picker, clipboard and keyboard are all native
Android; the document truth lives in the unmodified upstream Rust crates.

```
Android phone
   ↓
DevWord-*.apk             (Kotlin + Jetpack Compose, minSdk 29 / Android 10, arm64-v8a)
   ↓
libwordcraft_core.so     (JNI bridge, rust-core/)
   ↓
wordcraft-engine · wordcraft-docx · wordcraft-doc  (unmodified upstream crates)
   ↓
Open / Edit / Save  .docx
```

## App name and icon

| What | Where | Value |
|---|---|---|
| Launcher label | `app/src/main/res/values/strings.xml` | `DevWord` |
| Launcher icon | `app/src/main/res/drawable/ic_launcher_{bg,foreground}.xml` | pink gradient background + white **DW** monogram |
| App theme | `app/src/main/res/values/themes.xml` | `Theme.DevWord` |
| Package / namespace | `app/build.gradle.kts` | `com.wordcraft.android` (unchanged) |

The app was called **WordCraft** before v0.1.4; only the user-visible name, the launcher
icon and the theme name changed. The package id stays `com.wordcraft.android` so an
existing install is upgraded in place instead of appearing as a second app.

## Layout

| Path | What it is |
|---|---|
| `app/` | MainActivity, SAF file flows (new/open/save/save-as/share), recents, top bar |
| `editor/` | Compose editor surface: sections, tables, format toolbar, find/replace |
| `document-core/` | JNI bridge + engine wrapper + pure edit logic (diff → commands) + models |
| `storage/` | Recent documents (SharedPreferences) |
| `ui/` | Theme (light/dark) and shared colors |
| `rust-core/` | `wordcraft-ffi` crate: path-depends on the upstream `wordcraft/` crates |
| `rust-core/tools/` | The generator that turns the engine's command registry into the Kotlin ribbon catalog |
| `../wordcraft/` | The pristine upstream WordCraft checkout (never modified) |

## System installation

### Prerequisites

Install the following on your development machine before building:

| Tool | Version | Notes |
|---|---|---|
| **JDK** | 17 | `JAVA_HOME` must point to it — the release build's lint step does not run on newer JDKs (on JDK 27 it fails with `IllegalArgumentException: 27`) |
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

### Publishing a release

Each release must carry both the versioned assets and the stable aliases, because the
README download links point at `releases/latest/download/DevWord-release.apk` and
`releases/latest/download/DevWord-debug.apk` — GitHub resolves those to the newest
non-prerelease release, so the aliases have to be re-uploaded every time. The old
`WordCraft-*.apk` aliases are kept as well, so links bookmarked before the rename still
resolve to the current build:

| Asset name | Source |
|---|---|
| `devword-android-v<version>-release.apk` | `app/build/outputs/apk/release/app-release.apk` |
| `devword-android-v<version>-debug.apk` | `app/build/outputs/apk/debug/app-debug.apk` |
| `DevWord-release.apk` | same file as the versioned release asset |
| `DevWord-debug.apk` | same file as the versioned debug asset |
| `WordCraft-release.apk` | legacy alias, same bytes as `DevWord-release.apk` |
| `WordCraft-debug.apk` | legacy alias, same bytes as `DevWord-debug.apk` |

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
- The chrome is Microsoft Word for Android's phone layout. The white **app bar** holds Word's
  blue **W** tile, the document name with its *Saved to this device / Not saved · N words*
  caption, then undo, redo, search (opens Find) and the ⋮ menu. Beneath it sits the
  always-visible **format strip**: bold, italic, underline, strikethrough, alignment, insert,
  bullets, find and ⋮.
- The **ribbon lives at the bottom**, as it does in Word on a phone: the tab strip
  **Home · Insert · Draw · Layout · Review · View** with **⋯** for Design, References, Mailings,
  File and Help. Tapping a tab slides its ribbon groups up above the strip; tapping that tab
  again (or the pane's chevron) collapses it. The **Home** ribbon holds the everyday tools —
  Clipboard (paste/cut/copy), Font (name, size, grow/shrink, clear formatting,
  bold/italic/underline/strikethrough, font colour, highlighter), Paragraph (bullets,
  numbering, indent, alignment), Styles (Normal, Heading 1, Title, Heading 2, Subtitle) and
  Editing (Find, Replace, Select All). The **View** ribbon adds the view/zoom/window controls,
  and every other tab is generated from the engine's own command registry, so each button runs
  a real engine command with the parameters that command expects.
- **Find/replace** via the Editing group or Ctrl+F; undo/redo via the header buttons or Ctrl+Z/Y.
- **File** (behind ⋯) is Word's backstage: New, Open… (system picker), Save / Save as…, Save as
  PDF, Open PDF…, Share, Print and Recent documents.

### How the ribbon is built

The upstream engine already declares the ribbon layout in its own command registry —
every `CommandSpec::new(id, label, location)` carries the tab and the group it belongs to
(`"Insert › Illustrations"`). The Android catalog is **generated** from that registry, so a
ribbon button is never a decoration: it runs the engine command and opens the options
dialog built from that command's documented parameter contract.

```bash
# regenerate the catalog from the engine sources and the shipped .so
python3 rust-core/tools/build_ribbon_catalog.py \
    <engine-cmd-src-dir> app/src/main/jniLibs/arm64-v8a/libwordcraft_core.so \
    wordcraft-android/editor/src/main/java/com/wordcraft/editor/RibbonCatalogData.kt
```

Only ids that are present in the shipped `libwordcraft_core.so` are emitted, so every
button is backed by a command the app can really run. `RibbonCatalogTest` asserts that
every tab of the strip resolves to groups, that no command is bound twice and that every
group has the large button Word draws for it.

## Download the APKs

Ready-made builds are attached to the GitHub releases. The first two links always point at
the **latest** release:

- **Release (install directly, signed):** [DevWord-release.apk](https://github.com/itsupportgoc-alt/android-app/releases/latest/download/DevWord-release.apk)
- **Debug:** [DevWord-debug.apk](https://github.com/itsupportgoc-alt/android-app/releases/latest/download/DevWord-debug.apk)

Pinned builds (current release **v0.1.5**):

- [devword-android-v0.1.5-release.apk](https://github.com/itsupportgoc-alt/android-app/releases/download/v0.1.5/devword-android-v0.1.5-release.apk)
- [devword-android-v0.1.5-debug.apk](https://github.com/itsupportgoc-alt/android-app/releases/download/v0.1.5/devword-android-v0.1.5-debug.apk)

The repository is private, so the links and the releases page ask you to sign in with a
GitHub account that has access before the download starts.

All releases: https://github.com/itsupportgoc-alt/android-app/releases

Or, after a local build, grab them from:

- `app/build/outputs/apk/debug/app-debug.apk` — debug build
- `app/build/outputs/apk/release/app-release.apk` — release build (signed with the local keystore)
- `app/src/main/jniLibs/arm64-v8a/libwordcraft_core.so` — the JNI engine library (copied in by
  the Gradle build at preBuild; do not hand-edit)

## Known limitations

- Editing happens in reflow mode (like Word mobile's reflow view), not paginated print layout;
  the **File** tab exports the document to PDF and sends it to the Android print service.
- Cross-paragraph drag selection and paragraph-level spacing *inside* one style run are
  approximated; paragraphs split into separate fields at every style/alignment/list change.
- Read Aloud, mail merge and track changes are exposed as ribbon buttons that run the
  engine's commands, but there is no dedicated player, recipient-list editor or review pane
  yet; MCP is desktop-only.
- Password-protected documents prompt an error (decryption is in the engine; no password UI yet).

## Contributing

- The upstream `wordcraft/` engine is consumed **unmodified** via path-dependencies — never edit
  the `../wordcraft/` sources.
- View-model JSON crosses the JNI boundary as UTF-8 byte arrays; keep the 7
  `Java_com_wordcraft_documentcore_WordCraftNativeKt_*` exports in sync with the `external fun`
  declarations in `WordCraftNative.kt`.
- Tests: `./gradlew test` (Kotlin) and `cargo test` (Rust, host).
