# WordCraft → Android Porting Plan

> Decisions based on the repository audit in [ARCHITECTURE.md](ARCHITECTURE.md). Every major
> component is classified as `REUSE` / `PORT` / `REIMPLEMENT` / `ANDROID-NATIVE` / `NOT NEEDED`.

## Decision summary

**The Rust document engine is reused, not rewritten.** WordCraft's core (`doc`, `docx`,
`engine`, …) is platform-neutral Rust with no `unsafe`, serde at its boundary, and a byte-based
file API — it cross-compiles to `aarch64-linux-android` as a `cdylib` and is called from Kotlin
through a thin C ABI. The UI layer is rebuilt natively (Jetpack Compose), following the same
architecture as desktop: *a thin shell that renders `Session` state and dispatches commands by
id*. No WebView anywhere.

This satisfies the DOCX requirement with zero reimplementation: `DOCX → wordcraft-docx →
Document → Android editor → Document → wordcraft-docx → DOCX`, with the real OOXML
reader/writer that already ships with round-trip tests.

## Component classification

| Component | Decision | Notes |
|---|---|---|
| `geom` (units, rects) | **REUSE** | compiled into the Android `.so` |
| `doc` — document model (paragraphs, runs, tables, styles, numbering, sections, fields, notes, comments, media, serde JSON) | **REUSE** | verbatim; JSON serde is the UI⇄core contract |
| `doc` — edit primitives (insert/delete/split/format) | **REUSE** | reached through engine commands |
| `docx` — OOXML reader/writer | **REUSE** | high-priority feature, verbatim incl. encryption, OMML, passthrough parts |
| `formats` (txt/md/html/rtf/odt/tex) | **REUSE** | free: open/import + export from the file menu |
| `docbin` (Word 97-2003 `.doc`) | **REUSE** | free via `engine::io` dispatch |
| `fonts` (font DB, shaping, substitution) | **REUSE** | scans `/system/fonts`; bundled OFL fonts; Calibri→Carlito etc. |
| `proof` (spell/grammar) | **REUSE** | offline, language per run |
| `layout` (line breaking, tables, pagination, hit testing) | **REUSE** | used by engine session; powers future print-layout mode + find-hit info |
| `render` (vello_cpu raster) + `pdf` (krilla) | **REUSE** | enables PDF export & page rendering later; compiled in via `engine` |
| `engine` — `Session`, selection, undo/redo, command registry (~600 ids), find/replace, `io::open_bytes/save_bytes` | **REUSE** | the heart of the Android port; all editing goes through `Session::run` |
| `engine` — keyboard keymap / desktop shortcut parsing | **PORT** | Android key events (incl. hardware keyboards) mapped onto command ids in Kotlin |
| `zotero`, `control-key`, `mcp` | **NOT NEEDED** | desktop/agent tooling; excluded from the Android FFI build graph where possible |
| FFI/JNI glue (`extern "C"`, opaque handles, JSON strings) | **PORT** | new crate `rust-core/wordcraft-ffi` in the Android project; path-depends on the untouched WordCraft workspace |
| `ui-egui` (43k lines ribbon/canvas/dialogs) | **REIMPLEMENT** | replaced by Jetpack Compose; same *pattern* (state + commands), not the code |
| `apps/wordcraft` (eframe desktop shell) | **NOT NEEDED** | desktop only |
| `apps/wordcraft-web` (WASM/Trunk) | **NOT NEEDED** | proves the core is portable, but Android is native, not a web wrapper |
| `apps/wordcraft-cli`, `xtask` | **NOT NEEDED** | dev tooling (CLI useful during tests on host only) |
| Editor surface (canvas, caret, selection, IME) | **REIMPLEMENT** | native Compose block editor driving engine commands |
| Toolbar / ribbon → mobile toolbar + expandable format panel | **REIMPLEMENT** | mobile-first: `B I U S Align + ⋮` compact bar, overflow sheet |
| File picker (open/save/save-as/share) | **ANDROID-NATIVE** | Storage Access Framework (`ActivityResultContracts`) |
| Storage (recent documents, autosave copies) | **ANDROID-NATIVE** | app-private storage + SAF URIs; no `std::path` assumptions in core |
| Clipboard | **ANDROID-NATIVE** | `android.content.ClipboardManager`; text goes through `edit.copy/cut/paste` commands |
| IME / soft keyboard + hardware keyboard | **ANDROID-NATIVE** | Compose `TextField` edits mapped to `text.insert` / `text.backspace` / `text.newParagraph` |
| Dark mode / orientation / phone+tablet layouts | **ANDROID-NATIVE** | Compose theme + resource qualifiers |
| Read Aloud (speech), printing dialogs, MCP, control channel | **NOT NEEDED** | desktop OS integrations; documented as out of scope |

## Communication contract (Kotlin ⇄ Rust)

Opaque `Long` handle → `wordcraft_engine::Session`. C ABI functions (all results as UTF-8 JSON
or allocated byte buffers, freed by explicit `wc_free_*`):

| Function | Purpose |
|---|---|
| `wc_session_new()` | blank document |
| `wc_open_bytes(name, bytes, len)` | `engine::io::open_bytes` (docx/doc/odt/rtf/md/txt/json by name) |
| `wc_save_bytes(handle, name)` | `engine::io::save_bytes` → bytes (docx by default) |
| `wc_run(handle, id, paramsJson)` | `Session::run` — every edit/format/undo/find command |
| `wc_state_json(handle)` | document JSON + selection + `canUndo/canRedo` + active formatting at caret (toolbar state) |
| `wc_free(ptr)` | release a buffer returned by the core |

Kotlin owns: SAF streams, clipboard, IME, theming, navigation, recents. Rust owns: document
truth, validation, undo, format semantics, DOCX.

## Editor design (native, not WebView)

Mobile-first **block editor** in Compose:

- Body renders as a lazily composed list of block rows (paragraphs and tables) from the engine's
  document JSON; each paragraph row is a `BasicTextField` styled with its `CharProps`
  (bold/italic/underline/strike/color/highlight/font/size) and `ParaProps` (alignment, indent,
  list markers, heading style).
- Edits are diffed and dispatched: insert → `text.insert`, delete → `text.backspace`/
  `text.delete`, Enter → `text.newParagraph`, cross-row merge on backspace at offset 0.
  The engine snapshots for undo, so undo/redo/coalescing/list continuation behave as desktop.
- Selection within a row is native text selection; toolbar/`select.range` gives cross-paragraph
  formatting scope. Copy/cut/paste via the platform clipboard.
- Toolbar: `B I U S · Align · + · ⋮` with an expandable sheet (font, size, color, highlight,
  alignment, lists, indent, insert link/table/image, heading style).

## Project structure

```
wordcraft-android/
├── app/                  # Android app: MainActivity, file ops (SAF), toolbar, navigation, theme
├── editor/               # Compose editor surface (blocks, caret/selection, IME mapping)
├── document-core/        # Kotlin API over the FFI (Session handle, commands, state JSON → models)
├── storage/              # Recent documents, app-private copies, SAF helpers
├── ui/                   # Shared Compose components (toolbar, sheets, theme tokens)
├── rust-core/            # cargo crate wordcraft-ffi → libwordcraft_core.so (path deps on ../wordcraft)
├── tests/                # JVM unit tests + DOCX round-trip suites (shared fixtures)
├── docs/                 # ARCHITECTURE.md, PORTING_PLAN.md, TESTING.md
└── README.md
```

The cloned `wordcraft/` checkout stays **unmodified**; `rust-core` references its crates by
`path = "../../wordcraft/crates/…"`.

## Build targets

- Kotlin + Gradle, Jetpack Compose, minSdk **29** (Android 10), targetSdk 35, **arm64-v8a**,
  Rust `stable-x86_64-pc-windows-gnu` host cross-compiling `aarch64-linux-android`
  (NDK 26.x clang as linker), `cdylib` bundled under `jniLibs/arm64-v8a/`.
- Verification: Rust unit/round-trip tests (host + target build), JVM unit tests for the Kotlin
  models/FFI mapping, `./gradlew test`, `./gradlew assembleDebug`, `assembleRelease`.

## Limitations accepted (documented, not faked)

- **Print-layout pagination view** (exact Word page rendering) is future work: `layout`+`render`
  are compiled in and reachable, v1 edits in reflow mode (Word mobile does the same).
- Cross-block drag-selection and some desktop-only commands (track-changes review UI, mail
  merge, ribbon dialogs) are reachable by id but not all have Android UI in v1.
- Read Aloud / printing / MCP / control channel are out of scope on Android.
- CJK/system font shaping uses the same HarfRust pipeline; fonts come from `/system/fonts` +
  bundled OFL fonts (proprietary Word fonts substitute as on desktop).
