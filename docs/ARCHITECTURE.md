# WordCraft — Architecture Analysis (for the Android port)

> Derived from the actual source of `https://github.com/storytold/wordcraft.git` (cloned
> 2026-10-13, `main`), version **0.6.1**, edition 2024, `rust-version = 1.90`,
> license **MIT OR Apache-2.0**. The source code is the authority for this document; the
> repository's own `docs/architecture.md` was cross-checked but is partly stale (it describes
> ~85.6k lines / 15 crates; the current tree measures ~195k lines of Rust across 16 crates +
> 3 apps + `xtask`).

## 1. How WordCraft currently works

WordCraft is a Rust workspace: a document-processing core with several thin front ends.

**Data flow** (verified in source):

1. **Open** — `engine::io::open_bytes(name, bytes)` picks a reader by extension/signature:
   `wordcraft-docx` (`.docx/.docm/.dotx/.dotm`, encrypted Word packages), `wordcraft-docbin`
   (Word 97-2003 `.doc` over a CFB container), `wordcraft-formats` (`.odt/.rtf/.html/.md/.tex/.txt`),
   or JSON (`serde_json` round-trip of the `Document`). It returns a `doc::Document`.
2. **Edit** — *every* user-visible action is a **command with a string id** dispatched through
   `Session::run(id, &serde_json::Value) -> Result<Value, CmdError>` (`crates/engine/src/session.rs:1119`).
   The registry (`crates/engine/src/cmd/mod.rs::registry()`) assembles ~600 `CommandSpec`s from
   48 modules (`text.insert`, `text.backspace`, `text.newParagraph`, `format.bold`,
   `format.italic`, `format.underline`, `format.strikethrough`, `format.size`, `format.color`,
   `format.highlight`, `para.align*`, `para.indent`, `lists.*`, `edit.undo`, `edit.redo`,
   `edit.copy/cut/paste`, `edit.find/findReplace/replace/replaceAll`, `select.range`, `caret.set`,
   `table.*`, `insert.*` …). The ribbon, keyboard, CLI, JSON control channel and MCP server all
   dispatch by the same ids — the UI is a thin shell over commands.
3. **Undo/redo** — `Session::edit_snapshot` before a mutating command, cheap because blocks are
   `Arc`-shared (`Arc::make_mut` copy-on-write); typing is coalesced into one undo step
   (`join_next_undo`), with an undo depth limit and eviction. `edit.undo` / `edit.redo` /
   `edit.undoTo` are ordinary commands.
4. **Layout** — `wordcraft-layout` turns the `Document` into a display list (positioned glyphs,
   boxes, images) incrementally per paragraph; `Session` owns the `DocLayout` + `LayoutCache`.
5. **Draw** — `wordcraft-render` (vello_cpu CPU rasteriser) paints pages for the egui canvas;
   `wordcraft-pdf` (krilla) exports PDF from the same display list.
6. **Save** — `engine::io::save_bytes(name, doc)` dispatches by extension (docx / formats / JSON),
   with optional password encryption.

**Selection** lives in `Session` (`Selection { anchor: Pos, focus: Pos }` where
`Pos = { story, path, offset }`; `Path` is a `Vec<u32>` addressing a paragraph, including inside
nested table cells). Commands read/modify it — the UI never edits the document directly.

## 2. Components that are Rust

Everything except the front ends. Workspace layout (`Cargo.toml` members = `crates/*`, `apps/*`, `xtask`):

| Layer | Crate | ~Lines | Role |
|---|---|---|---|
| L0 | `geom` | 0.6k | Units (pt, twip, EMU, in, cm), rects, measurement parsing |
| L1 | `doc` | 17.5k | **Document model**: `Document` (serde), `Block::Para/Table`, `Paragraph`, `Run`, `CharProps`, `ParaProps`, styles, numbering/lists, sections, fields, notes, comments, revisions, media; **edit primitives** (`insert_text`, `delete_range`, `split_paragraph`, `format_range`, `format_paragraphs`, `insert_fragment`, `copy_range`) |
| L1 | `fonts` | 4.5k | Font DB (bundled OFL + system dirs), metrics, outlines, shaping (HarfRust), Word font substitution (Carlito↔Calibri …), OpenType MATH |
| L1 | `proof` | 6.2k | Spelling/grammar (English, Polish), hyphenation |
| L2 | `layout` | 19.6k | Shaping, line breaking, tabs, lists, tables, pagination, headers/footers, floating objects, hit testing → display list |
| L2 | `docx` | 23.8k | **OOXML reader/writer**: `read_xml`/`read_with_password` → `Document`; `write(doc) -> Vec<u8>`; zip + quick-xml; OMML, charts, SDT, custom XML passthrough |
| L2 | `docbin` | 6.0k | Word 97-2003 `.doc` reader |
| L2 | `formats` | 13.1k | TXT/Markdown/HTML/RTF/ODT/LaTeX import/export |
| L3 | `render` | 2.1k | vello_cpu page rasteriser → PNG/JPEG |
| L3 | `pdf` | 2.4k | krilla PDF export |
| L4 | `engine` | 41.1k | `Session` (doc + selection + undo + layout cache + view state), **command registry**, feature catalog, samples, `io` dispatch, find/replace patterns, Read Aloud |
| L4 | `zotero`, `control-key` | 1.8k | Zotero protocol, control-port keys |
| L5 | `mcp` | 1.2k | MCP server exposing every command as a tool |
| L6 | `ui-egui` | 43.1k | egui front end: ribbon, Backstage, canvas, dialogs, panes, i18n, themes |
| apps | `wordcraft` | 2.9k | Desktop app (eframe + wgpu) |
| apps | `wordcraft-cli` | 0.7k | `convert`, `text`, `inspect`, `run`, `commands`, `parity`, `mcp` |
| apps | `wordcraft-web` | 0.6k | Browser build (eframe web runner, WebGPU/WebGL2) |

Quality rules (from `AGENTS.md`): `unsafe_code = "forbid"`, no panics in production code
(`deny(clippy::unwrap_used …)` on crate roots), hostile input is bounded, **821 documented
tests / 1,848 `#[test]` functions measured in-tree**, DOCX round-trip tests in
`crates/docx/tests/roundtrip.rs`, `cargo xtask ci` = fmt + clippy + tests + assets + layers + wasm.

## 3. Components that are UI

- `crates/ui-egui` (43k lines): ribbon, keytips, mini toolbar, Backstage, canvas + rulers,
  panes, dialogs, themes (`theme::Tokens`), i18n catalogs, control channel. Reads `Session`
  state only — it never mutates the document except by running commands.
- `apps/wordcraft`: window, logging, file associations (eframe/wgpu).
- `apps/wordcraft-web`: same UI compiled to WASM.

## 4. WebAssembly-specific pieces

`#[cfg(target_arch = "wasm32")]` appears in 23 files; the WASM build is a *native build of the
same UI*, not a separate codebase:

- `engine/src/io.rs` — filesystem paths (`open_path`, `read_file`, `write_file`) are stubbed on
  wasm; the byte-based API (`open_bytes`/`save_bytes`) is shared and **platform-independent**.
- `fonts/src/fontdb.rs` — system font directory scanning disabled; fonts come from bundled data.
- `engine/src/speech.rs`, `print.rs`, `cmd/font.rs` — OS integrations (speech programs, printer
  enumeration) stubbed.
- `apps/wordcraft-web` — eframe web runner + `Trunk`, WebGPU with WebGL2 fallback; browser
  clipboard/font store glue in `src/web/`.

Everything below L6 (`doc`, `docx`, `layout`, `fonts`, `formats`, `engine`, …) is
platform-neutral Rust with no OS-API dependency beyond `std` (plus `cfg` gates for fs access).

## 5. Components that can run on Android

Directly (pure Rust, `std`-only, compiles for `aarch64-linux-android`):

- `geom`, `doc`, `docx`, `formats`, `docbin`, `fonts`, `proof`, `layout`, `render`, `pdf`,
  `engine` (Session + command registry + undo + find/replace + `io::open_bytes/save_bytes`),
  `zotero`, `control-key`.
- System font scan (`fonts/fontdb.rs`) naturally reads `/system/fonts` on Android; proprietary
  Word fonts substitute to metric-compatible open fonts (`fonts/src/word.rs`) — same as desktop.
- Read Aloud's non-wasm branch shells out to `say`/`espeak`/PowerShell — it *compiles* on
  Android but is unusable; it is simply not invoked (feature → not needed).
- `mcp`, `ui-egui`, `apps/*` do **not** run on Android (they are desktop/web UI or tooling).

## 6. Components that need modification

- **Glue layer (new):** no C/JNI API exists today. A new FFI crate must expose
  Session/document/commands as a C ABI for Kotlin. (WordCraft itself is untouched; the glue
  lives in the Android project and *path-depends* on the WordCraft crates.)
- **`engine::io` path functions:** unusable as-is on Android (no meaningful `std::path` for
  SAF content URIs). Not modified — Android passes **bytes** in/out (SAF → `open_bytes`,
  `save_bytes` → SAF), which is the platform-neutral API that already exists.
- **Clipboard:** `ui-egui/system_clipboard.rs` (arboard) is desktop-only; Android clipboard is
  provided by the Kotlin UI (`edit.copy/cut/paste` commands are driven with text supplied by
  the platform).
- **Keyboard:** desktop keymap (`engine/keymap.rs`, egui key handling) is replaced by Android
  IME/hardware-key events mapped onto the same commands (`text.insert`, `text.backspace`, …).

## 7. Components that must be rewritten

- **The entire UI** — `ui-egui` cannot be reused (egui/eframe/wgpu on Android is not a
  mobile-first, IME-native, non-WebView solution). The Android UI is a native Jetpack Compose
  front end that mirrors the same architecture: *a thin shell that renders `Session` state and
  dispatches commands by id*.
- **Editor surface:** WordCraft's canvas is a paginated WYSIWYG surface driven by the Rust
  display list. The Android editor is a mobile-first reflow surface (block list rendered in
  Compose, Word-mobile "print layout → reflow" style) built on the same document model.

## 8. Components that can be reused

- **Document model (`doc`)** — reused as-is, including serde JSON serialization, which becomes
  the UI ⇄ core data contract.
- **DOCX (`docx`)** — reused as-is: `read_xml`/`read_with_password`/`write` (the full OOXML
  reader/writer with round-trip tests).
- **Engine (`engine`)** — reused as-is: `Session`, selection, undo/redo, the whole command
  registry (formatting, lists, alignment, indent, find/replace, clipboard commands, table and
  insert commands), file I/O dispatch.
- **`formats`, `docbin`, `fonts`, `proof`, `layout`, `render`, `pdf`** — reused (pulled in by
  `engine`); layout/render/pdf also unlock later print-layout rendering and PDF export on Android.

## 9. How DOCX is currently handled

- **Read:** zip container → OOXML parts (`[Content_Types].xml`, `word/document.xml`, styles,
  numbering, settings, media) parsed with `quick-xml` into `doc::Document`
  (`crates/docx/src/read/*`: story, props, math, sdt, embed, charts, wordart…). Transitional
  and Strict namespaces; `.docx/.docm/.dotx/.dotm`; encrypted packages decrypted via RustCrypto
  (`aes/sha1/sha2/hmac`).
- **Write:** `Document` → OOXML parts (`crates/docx/src/write/*`), `write(doc) -> Vec<u8>`.
  Round-trip fidelity (formatting, tables, images, hyperlinks, page breaks, lists, alignment) is
  enforced by `crates/docx/tests/roundtrip.rs` and friends.
- **Dispatch:** `engine::io::open_bytes` / `save_bytes` choose docx by extension/content
  signature — the byte API is exactly what Android needs (content-URI streaming lives in Kotlin).

## 10. How the Android app should communicate with the core

Follow WordCraft's own "everything is a command" architecture:

```
Kotlin / Compose UI  ──(C ABI / JNI, JSON)──►  libwordcraft_core.so (Rust)
   render state  ◄──── wc_doc_json / wc_state_json ────  wordcraft-engine::Session
   key/IME/toolbar events ──► wc_run(id, paramsJson) ──► Session::run (undo, coalescing)
   SAF open/save ──► wc_open_bytes(name, bytes) / wc_save_bytes(name) ──► engine::io
```

- One `Session` per open document, owned by the Rust side behind an opaque handle.
- **JSON over the existing serde boundary**: the `Document` (camelCase serde) serializes to JSON
  for the UI; commands take/return `serde_json::Value`. No per-feature FFI surface — new
  WordCraft features become available by id.
- Kotlin maps IME edits and toolbar taps to the same commands the desktop UI uses
  (`text.insert`, `text.backspace`, `text.newParagraph`, `format.bold`, `para.alignCenter`,
  `edit.undo`, `edit.findReplace`, `select.range`, …), so undo, coalescing, list continuation,
  AutoCorrect etc. behave exactly as on desktop.
- Threading: the `Session` handle is used from a single engine thread (commands marshalled from
  the UI); no `unsafe` in Rust beyond the extern "C" boundary (FFI glue is outside the
  `unsafe_code = "forbid"` workspace and kept minimal).

## Licenses

- Code: **MIT OR Apache-2.0** (dual), `NOTICE` required; assets/fonts each listed in
  `ATTRIBUTION.md`. Dependencies are permissively licensed (no GPL/LGPL/AGPL allowed by
  project policy). The Android port preserves these licenses and carries `NOTICE` +
  `ATTRIBUTION.md`.
