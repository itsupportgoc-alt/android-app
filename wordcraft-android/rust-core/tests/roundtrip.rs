//! DOCX round-trip: build a document through the engine's command registry, save it as .docx,
//! reopen it and verify the formatting survived — the exact path the Android app takes
//! (`open_bytes` → commands → `save_bytes`).

use serde_json::json;
use wordcraft_doc::para::PAGE_BREAK;
use wordcraft_doc::{Block, Document, ListKind};
use wordcraft_engine::Session;
use wordcraft_core::{open_session, save_session};

fn run(s: &mut Session, id: &str, params: serde_json::Value) {
    if let Err(e) = s.run(id, &params) {
        panic!("command `{id}` failed: {e}");
    }
}

fn para<'a>(doc: &'a Document, i: usize) -> &'a wordcraft_doc::Paragraph {
    match doc.body.get(i).map(|b| &**b) {
        Some(Block::Para(p)) => p,
        Some(Block::Table(_)) => panic!("body[{i}] is a table, expected a paragraph"),
        None => panic!("no body[{i}]"),
    }
}

/// The run covering byte `off` of paragraph `i`.
fn run_at(doc: &Document, i: usize, off: usize) -> wordcraft_doc::Run {
    let p = para(doc, i);
    let mut start = 0usize;
    for r in &p.runs {
        let end = start + r.len;
        if off >= start && off < end {
            return r.clone();
        }
        start = end;
    }
    panic!("no run covers byte {off} of body[{i}] (text: {:?})", p.text)
}

/// A representative rich document built the way the Android editor builds it.
fn build_document() -> Session {
    let mut s = Session::new(Document::new());
    // Two paragraphs.
    run(&mut s, "text.insert", json!({"text": "Hello world"}));
    run(&mut s, "text.newParagraph", json!({}));
    run(&mut s, "text.insert", json!({"text": "Second paragraph"}));
    // Bold "Hello", italic "world".
    run(&mut s, "select.text", json!({"text": "Hello"}));
    run(&mut s, "format.bold", json!({}));
    run(&mut s, "select.text", json!({"text": "world"}));
    run(&mut s, "format.italic", json!({}));
    run(&mut s, "format.underline", json!({}));
    run(&mut s, "format.strikethrough", json!({}));
    // Heading style on the second paragraph.
    run(&mut s, "caret.set", json!({"pos": {"block": 1, "off": 0}}));
    run(&mut s, "para.style", json!({"style": "Heading1"}));
    // Center the heading.
    run(&mut s, "para.align", json!({"value": "center"}));
    // A third paragraph as a bulleted list item.
    run(&mut s, "caret.set", json!({"pos": {"block": 1, "off": 17}}));
    run(&mut s, "text.newParagraph", json!({}));
    run(&mut s, "text.insert", json!({"text": "Item one"}));
    run(&mut s, "para.bullets", json!({}));
    // A hyperlink on "Item one".
    run(&mut s, "select.text", json!({"text": "Item one"}));
    run(&mut s, "insert.link", json!({"url": "https://example.com"}));
    // A page break at the end.
    run(&mut s, "caret.set", json!({"pos": {"block": 2, "off": 8}}));
    run(&mut s, "text.pageBreak", json!({}));
    // A 2×2 table after the list item.
    run(&mut s, "caret.set", json!({"pos": {"block": 2, "off": 9}}));
    run(&mut s, "insert.table", json!({"rows": 2, "cols": 2}));
    s
}

#[test]
fn docx_roundtrip_keeps_formatting() {
    let s = build_document();

    // --- save & reopen through the same byte API the app uses ---
    let bytes = save_session(&s, "rich.docx").expect("save docx");
    assert!(!bytes.is_empty());
    let s2 = open_session("rich.docx", &bytes).expect("reopen docx");
    let doc = &s2.doc;

    // Paragraph texts.
    assert_eq!(para(doc, 0).text, "Hello world");
    assert_eq!(para(doc, 1).text, "Second paragraph");
    // The page break is a control char inside paragraph 2's text.
    assert!(para(doc, 2).text.contains(PAGE_BREAK), "page break lost");

    // Character formatting on "Hello" (byte 0): bold, not italic.
    let hello = run_at(doc, 0, 0);
    assert_eq!(hello.props.bold, Some(true), "bold lost");
    assert_ne!(hello.props.italic, Some(true));

    // "world" starts at byte 6: italic + underline + strikethrough.
    let world = run_at(doc, 0, 6);
    assert_eq!(world.props.italic, Some(true), "italic lost");
    assert!(world.props.underline.is_some() && !matches!(world.props.underline, Some(wordcraft_doc::props::Underline::None)), "underline lost");
    assert_eq!(world.props.strike, Some(true), "strikethrough lost");

    // Heading style + centered alignment on paragraph 1.
    let head = para(doc, 1);
    assert_eq!(head.props.style.as_deref(), Some("Heading1"), "style lost");
    assert_eq!(head.props.align, Some(wordcraft_doc::props::Align::Center), "alignment lost");

    // Bulleted list on paragraph 2 (and the numbering definition came across).
    let item = para(doc, 2);
    let num = item.props.numbering.expect("list numbering lost");
    assert_ne!(num.num, 0);
    assert!(
        s2.doc.numbering.find_kind(ListKind::Bullet).is_some(),
        "bullet numbering definition lost"
    );

    // Hyperlink on "Item one".
    let linked = run_at(doc, 2, 0);
    assert_eq!(
        linked.props.link.as_deref(),
        Some("https://example.com"),
        "hyperlink lost"
    );

    // Table: present with 2×2.
    let table = doc.body.iter().find_map(|b| match &**b {
        Block::Table(t) => Some(t),
        Block::Para(_) => None,
    });
    let table = table.expect("table lost");
    assert_eq!(table.rows.len(), 2);
    assert_eq!(table.grid.len(), 2);
    assert_eq!(match &*table.rows[0].cells[0].blocks[0] {
        Block::Para(p) => p.text.clone(),
        _ => String::new(),
    }, "");
}

#[test]
fn docx_survives_edit_after_reopen() {
    // open → edit → save → reopen (the full user loop).
    let s = build_document();
    let bytes = save_session(&s, "rich.docx").unwrap();
    let mut s2 = open_session("rich.docx", &bytes).unwrap();

    run(&mut s2, "caret.set", json!({"pos": {"block": 0, "off": 11}}));
    run(&mut s2, "text.insert", json!({"text": " again"}));
    assert_eq!(para(&s2.doc, 0).text, "Hello world again");

    let bytes2 = save_session(&s2, "rich2.docx").unwrap();
    let s3 = open_session("rich2.docx", &bytes2).unwrap();
    assert_eq!(para(&s3.doc, 0).text, "Hello world again");
    // Formatting of the first paragraph survived the second trip too.
    assert_eq!(run_at(&s3.doc, 0, 0).props.bold, Some(true));
}

#[test]
fn undo_redo_roundtrip() {
    let mut s = Session::new(Document::new());
    run(&mut s, "text.insert", json!({"text": "Alpha"}));
    run(&mut s, "text.newParagraph", json!({}));
    run(&mut s, "text.insert", json!({"text": "Beta"}));
    assert_eq!(para(&s.doc, 1).text, "Beta");

    run(&mut s, "edit.undo", json!({}));
    assert_eq!(para(&s.doc, 1).text, "", "undo should clear the second insert");
    run(&mut s, "edit.undo", json!({}));
    // The second undo removes the paragraph split (typing groups per paragraph).
    assert_eq!(para(&s.doc, 0).text, "Alpha");
    assert_eq!(s.doc.body.len(), 1, "undo should merge the split back");

    run(&mut s, "edit.redo", json!({}));
    run(&mut s, "edit.redo", json!({}));
    assert_eq!(para(&s.doc, 0).text, "Alpha");
    assert_eq!(para(&s.doc, 1).text, "Beta");
}

#[test]
fn find_and_replace_work_through_commands() {
    let mut s = Session::new(Document::new());
    run(&mut s, "text.insert", json!({"text": "The cat sat on the mat"}));
    let found = s.run("edit.find", &json!({"text": "cat"})).expect("find");
    assert!(found.get("matches").and_then(|v| v.as_u64()).unwrap_or(0) >= 1 || !s.find.results.is_empty());
    run(&mut s, "edit.replaceAll", json!({"text": "cat", "with": "dog"}));
    assert_eq!(para(&s.doc, 0).text, "The dog sat on the mat");
}

#[test]
fn picture_survives_docx_roundtrip() {
    // 1×1 transparent PNG.
    let png: &[u8] = &[
        0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, 0xC4,
        0x89, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x44, 0x41, 0x54, 0x78, 0x9C, 0x62, 0x00, 0x01, 0x00, 0x00,
        0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, 0xB4, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, 0xAE,
        0x42, 0x60, 0x82,
    ];
    let b64 = base64_encode(png);

    let mut s = Session::new(Document::new());
    run(&mut s, "text.insert", json!({"text": "Pic: "}));
    run(&mut s, "insert.picture", json!({"data": b64, "width": 72.0}));
    assert!(!s.doc.media.is_empty(), "picture not stored");

    let bytes = save_session(&s, "pic.docx").expect("save");
    let s2 = open_session("pic.docx", &bytes).expect("reopen");
    assert!(!s2.doc.media.is_empty(), "media lost on roundtrip");
    let has_object = s2.doc.body.iter().any(|b| match &**b {
        Block::Para(p) => !p.objects.is_empty(),
        Block::Table(_) => false,
    });
    assert!(has_object, "inline image lost on roundtrip");
}

#[test]
fn plain_text_and_markdown_roundtrip() {
    let s = build_document();
    let md = save_session(&s, "doc.md").expect("save md");
    let s2 = open_session("doc.md", &md).expect("reopen md");
    assert_eq!(para(&s2.doc, 0).text, "Hello world");

    let txt = save_session(&s, "doc.txt").expect("save txt");
    let s3 = open_session("doc.txt", &txt).expect("reopen txt");
    assert!(para(&s3.doc, 0).text.contains("Hello world"));
}

/// Minimal base64 (the crate is available through the FFI deps).
fn base64_encode(data: &[u8]) -> String {
    use base64::Engine as _;
    base64::engine::general_purpose::STANDARD.encode(data)
}

/// Writes real .docx files into `tests/fixtures/` and reads one straight back from disk,
/// proving the bytes on disk open as documents (the fixtures the app is tested against).
#[test]
fn writes_and_reads_fixture_files() {
    let fixtures = std::path::Path::new(env!("CARGO_MANIFEST_DIR")).join("../tests/fixtures");
    std::fs::create_dir_all(&fixtures).expect("create fixtures dir");

    let s = build_document();
    let rich = save_session(&s, "rich.docx").expect("save rich.docx");
    std::fs::write(fixtures.join("rich.docx"), &rich).expect("write rich.docx");

    let plain = Session::new(Document::from_text("First line\nSecond line"));
    let simple = save_session(&plain, "simple.docx").expect("save simple.docx");
    std::fs::write(fixtures.join("simple.docx"), &simple).expect("write simple.docx");

    // The bytes we just wrote to disk open as a document (actual file, actual reader).
    let on_disk = std::fs::read(fixtures.join("rich.docx")).expect("read fixture back");
    let reopened = open_session("rich.docx", &on_disk).expect("open fixture from disk");
    assert_eq!(para(&reopened.doc, 0).text, "Hello world");
    assert_eq!(run_at(&reopened.doc, 0, 0).props.bold, Some(true));
}
