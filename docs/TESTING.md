# WordCraft Android — Test Results

> Last run: 2026-10-13 · Toolchain: Rust 1.99.0 (windows-gnu host + `aarch64-linux-android`
> target, NDK 26.3.11579264), JDK 17, Gradle 8.10.2, AGP 8.7.3, Kotlin 2.0.21

## 1. Rust engine tests (`wordcraft-android/rust-core`) — **10/10 pass**

```
cargo test        # host: x86_64-pc-windows-gnu (MinGW gcc)
```

| Suite | Tests | Result |
|---|---|---|
| `src/vm.rs` unit tests (view model) | 3 | ✅ 3 passed |
| `tests/roundtrip.rs` (DOCX / editing) | 7 | ✅ 7 passed |

Highlights of what the round-trip suite proves through the **same byte API the app uses**
(`open_session` = `engine::io::open_bytes`, `save_session` = `engine::io::save_bytes`):

- **`docx_roundtrip_keeps_formatting`** — builds a document through engine commands
  (type → split → bold/italic/underline/strike → Heading1 + center → bulleted list →
  hyperlink → page break → 2×2 table), saves `.docx`, reopens it and asserts every piece of
  formatting survived: run-level char props, paragraph style, alignment, numbering
  definition, hyperlink, page-break control char, table grid/rows.
- **`docx_survives_edit_after_reopen`** — open → edit → save → reopen → the *second* trip
  keeps text and formatting (the exact import → edit → save → reopen loop required by the brief).
- **`picture_survives_docx_roundtrip`** — a PNG goes through `insert.picture`, lands in the
  package media, and comes back as an inline object after reopen.
- **`undo_redo_roundtrip`** — typing groups, two undos (text, then paragraph split), redos.
- **`find_and_replace_work_through_commands`** — `edit.find` + `edit.replaceAll`.
- **`plain_text_and_markdown_roundtrip`** — `.md` / `.txt` export & re-import.
- **`writes_and_reads_fixture_files`** — writes real files to
  [`wordcraft-android/tests/fixtures/`](../wordcraft-android/tests/fixtures) (`rich.docx`,
  `simple.docx`) and reopens one **from disk**, asserting text + bold.

## 2. Kotlin unit tests (`./gradlew test`) — **31/31 pass per variant (debug + release)**

| Class | Tests | Covers |
|---|---|---|
| `TextDiffTest` | 8 | text diffing incl. newline split/merge, surrogate-pair safety |
| `SectionMapperTest` | 4 | section text ⇄ paragraph/offset mapping, clamping |
| `UtfConversionTest` | 4 | UTF-16 ⇄ UTF-8 offset math (CJK, emoji, boundary clamps) |
| `PlanOpsTest` | 7 | field edits → `select.range`/`text.insert`/`text.newParagraph`/`text.backspace` sequences, redundant-selection skipping, JSON escaping |
| `StateParserTest` | 8 | engine state JSON → models (selection, format, para, spans, lists, images, nested table cells) |

```
./gradlew test     # BUILD SUCCESSFUL — 0 failures, 0 errors (both variants)
```

## 3. Build verification

| Check | Result |
|---|---|
| `./gradlew assembleDebug` | ✅ `app/build/outputs/apk/debug/` (123.9 MB incl. 33.8 MB `.so`) |
| `./gradlew assembleRelease` | ✅ `app/build/outputs/apk/release/` (74.8 MB) |
| Release signing | ✅ `apksigner verify` — `CN=WordCraft Android Local Build` (local keystore `release.keystore`) |
| `.so` inside both APKs | ✅ `lib/arm64-v8a/libwordcraft_core.so`, 7 JNI symbols exported |
| `aapt dump badging` | ✅ `com.wordcraft.android`, `minSdk 29`, `targetSdk 35`, label `WordCraft` |

## 4. JNI boundary verification (static — no device available)

Because no ARM64 device/emulator was available in this environment, the native boundary was
verified statically:

1. **Symbol parity**: `llvm-nm -D` on the built `.so` vs the Kotlin `external fun` names —
   script-verified `diff` → `JNI_SYMBOLS_MATCH` (7/7).
   This check caught (and fixed) a real mismatch early: the Kotlin declarations
   (`nativeSessionNew`, …) initially didn't match the Rust symbols (`…_sessionNew`), which
   would have crashed with `UnsatisfiedLinkError` at first launch.
2. **Descriptors**: `javap -s` on the compiled `WordCraftNativeKt.class` confirms
   `()J`, `([B[B)J`, `(J)V`, `(J[B)[B`, `(J[B[B)[B`, `(J)[B`, `()[B` — matching the Rust
   `extern "C"` signatures exactly.
3. All strings cross as UTF-8 byte arrays (never JNI modified-UTF-8 `String`s), so emoji and
   CJK text are byte-exact (unit-tested on the Kotlin side).

## 5. Known gaps / not run

- **On-device runtime** was not executed (no ARM64 device or ARM translation emulator here).
  The engine behaviour is covered by the host tests above; the JNI surface by static
  verification; the Kotlin mapping logic by unit tests. First-launch smoke testing on a
  device is the recommended next step.
- Instrumented tests (`androidTest`) are not included (no emulator).
- Paragraphs that share one style run are edited inside a single text field, so *vertical
  spacing between two same-style paragraphs* renders as a line break rather than the Word
  `spaceAfter` gap (spacing is exact at every style/alignment/list boundary).
