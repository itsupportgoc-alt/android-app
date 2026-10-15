//! Editor view model: the `Session`'s document as JSON the Compose UI renders directly.
//!
//! Ranges are UTF-16 indices (Kotlin `String` coordinates); byte math stays on this side of the
//! boundary. Paragraph list labels are computed with the same `Counters` walk the layout uses.

use serde_json::{json, Value};
use std::sync::Arc;
use wordcraft_doc::numbering::Counters;
use wordcraft_doc::props::{Align, LineSpacing, TextColor, Underline, VertAlign};
use wordcraft_doc::resolve::ResolvedChar;
use wordcraft_doc::{Block, InlineObject, Paragraph, Table};
use wordcraft_engine::cmd;
use wordcraft_engine::Session;

/// Full state the Android UI needs after every mutation.
pub fn state_json(s: &Session) -> Value {
    let mut counters = Counters::default();
    let mut blocks: Vec<Value> = Vec::new();
    walk_blocks(s, &s.doc.body, &[], &mut counters, &mut blocks);

    let para = s.doc.para_at(&s.sel.focus).map(|p| para_state(s, p));
    let format = cmd::format::state(s);
    json!({
        "canUndo": s.can_undo(),
        "canRedo": s.can_redo(),
        "dirty": s.dirty,
        "selection": {
            "anchor": serde_json::to_value(&s.sel.anchor).unwrap_or(Value::Null),
            "focus": serde_json::to_value(&s.sel.focus).unwrap_or(Value::Null),
        },
        "selectedText": s.selected_text(),
        "wordCount": s.word_count(),
        "blocks": blocks,
        "format": format,
        "para": para,
        "find": json!({
            "query": s.find.query,
            "replace": s.find.replace,
            "matches": s.find.results.len(),
            "current": s.find.current,
        }),
    })
}

/// Toolbar state for the focused paragraph (align, style, indents, list).
fn para_state(s: &Session, p: &Paragraph) -> Value {
    let rp = s.doc.styles.resolve_para(&p.props);
    let style_id = if rp.style.is_empty() { "Normal".to_string() } else { rp.style.clone() };
    let style_name = s.doc.styles.get(&style_id).map(|st| st.name.clone()).unwrap_or_else(|| style_id.clone());
    json!({
        "style": style_id,
        "styleName": style_name,
        "align": align_str(rp.align),
        "indentLeft": rp.indent_left,
        "indentFirst": rp.indent_first,
        "spaceBefore": rp.space_before,
        "spaceAfter": rp.space_after,
        "lineSpacing": line_spacing_json(&rp.line_spacing),
        "inList": rp.numbering.as_ref().is_some_and(|n| n.num != 0),
    })
}

fn walk_blocks(s: &Session, blocks: &[Arc<Block>], prefix: &[u32], counters: &mut Counters, out: &mut Vec<Value>) {
    for (i, b) in blocks.iter().enumerate() {
        let mut path = prefix.to_vec();
        path.push(i as u32);
        match &**b {
            Block::Para(p) => out.push(para_vm(s, p, path, counters)),
            Block::Table(t) => out.push(table_vm(s, t, path, counters)),
        }
    }
}

fn para_vm(s: &Session, p: &Paragraph, path: Vec<u32>, counters: &mut Counters) -> Value {
    let rp = s.doc.styles.resolve_para(&p.props);
    let style_id = if rp.style.is_empty() { "Normal".to_string() } else { rp.style.clone() };
    let style_name = s.doc.styles.get(&style_id).map(|st| st.name.clone()).unwrap_or_else(|| style_id.clone());

    // Spans: byte ranges of the run list, as UTF-16 indices, with resolved character formatting.
    let mut spans: Vec<Value> = Vec::with_capacity(p.runs.len());
    let mut byte = 0usize;
    for r in &p.runs {
        let start = clamp_boundary(&p.text, byte);
        byte = byte.saturating_add(r.len);
        let end = clamp_boundary(&p.text, byte);
        if start >= p.text.len() || start >= end {
            break;
        }
        let rc = s.doc.styles.resolve_char(Some(&style_id), &r.props);
        spans.push(span_json(&p.text, start, end, &rc));
    }

    let list = match &rp.numbering {
        Some(n) if n.num != 0 => counters
            .next_label(&s.doc.numbering, n.num, n.level)
            .map(|(label, lvl)| json!({ "label": label, "level": n.level, "hanging": lvl.hanging, "indent": lvl.indent }))
            .unwrap_or(Value::Null),
        _ => Value::Null,
    };

    let imgs: Vec<Value> = p.objects.iter().filter_map(|o| image_vm(s, o)).collect();

    json!({
        "k": "p",
        "path": path,
        "text": p.text,
        "spans": spans,
        "style": style_id,
        "styleName": style_name,
        "align": align_str(rp.align),
        "indentLeft": rp.indent_left,
        "indentFirst": rp.indent_first,
        "spaceBefore": rp.space_before,
        "spaceAfter": rp.space_after,
        "lineSpacing": line_spacing_json(&rp.line_spacing),
        "pageBreak": rp.page_break_before,
        "rtl": rp.bidi,
        "outline": rp.outline_level,
        "shading": rp.shading.map(|c| c.hex()),
        "list": list,
        "imgs": imgs,
        "empty": p.text.is_empty(),
    })
}

fn table_vm(s: &Session, t: &Table, path: Vec<u32>, counters: &mut Counters) -> Value {
    let rows: Vec<Value> = t
        .rows
        .iter()
        .enumerate()
        .map(|(ri, row)| {
            let cells: Vec<Value> = row
                .cells
                .iter()
                .enumerate()
                .map(|(ci, cell)| {
                    let mut inner: Vec<Value> = Vec::new();
                    let mut prefix = path.clone();
                    prefix.push(ri as u32);
                    prefix.push(ci as u32);
                    walk_blocks(s, &cell.blocks, &prefix, counters, &mut inner);
                    json!({ "span": cell.span(), "blocks": inner })
                })
                .collect();
            Value::Array(cells)
        })
        .collect();
    json!({ "k": "t", "path": path, "grid": t.grid, "rows": rows })
}

fn image_vm(s: &Session, o: &InlineObject) -> Option<Value> {
    match o {
        InlineObject::Image { media, w, h, alt, .. } => {
            let b64 = s
                .doc
                .media
                .get(media)
                .map(|bytes| base64::Engine::encode(&base64::engine::general_purpose::STANDARD, bytes.as_slice()));
            Some(json!({ "kind": "image", "w": w, "h": h, "alt": alt, "b64": b64 }))
        }
        InlineObject::Graphic { w, h, alt, .. } => Some(json!({ "kind": "graphic", "w": w, "h": h, "alt": alt })),
        InlineObject::Shape { w, h, .. } => Some(json!({ "kind": "shape", "w": w, "h": h })),
        _ => Some(json!({ "kind": "object" })),
    }
}

/// Resolved character formatting as compact JSON; default values are omitted.
fn span_json(text: &str, start: usize, end: usize, rc: &ResolvedChar) -> Value {
    let mut m = serde_json::Map::new();
    if rc.bold {
        m.insert("b".into(), Value::Bool(true));
    }
    if rc.italic {
        m.insert("i".into(), Value::Bool(true));
    }
    if let Some(u) = underline_str(rc.underline) {
        m.insert("u".into(), json!(u));
    }
    if rc.strike || rc.double_strike {
        m.insert("s".into(), Value::Bool(true));
    }
    if (rc.size - 11.0).abs() > 0.01 {
        m.insert("sz".into(), json!(rc.size));
    }
    if rc.font != wordcraft_doc::styles::BODY_FONT {
        m.insert("font".into(), json!(rc.font));
    }
    if let TextColor::Rgb(c) = rc.color {
        m.insert("color".into(), json!(c.hex()));
    }
    if let Some(c) = rc.highlight {
        m.insert("hl".into(), json!(c.hex()));
    }
    match rc.vert_align {
        VertAlign::Superscript => {
            m.insert("va".into(), json!("sup"));
        }
        VertAlign::Subscript => {
            m.insert("va".into(), json!("sub"));
        }
        VertAlign::Baseline => {}
    }
    if rc.caps {
        m.insert("caps".into(), Value::Bool(true));
    }
    if rc.small_caps {
        m.insert("scaps".into(), Value::Bool(true));
    }
    if rc.rtl {
        m.insert("rtl".into(), Value::Bool(true));
    }
    if let Some(l) = &rc.link {
        m.insert("link".into(), json!(l));
    }
    m.insert("s0".into(), json!(utf16(text, start)));
    m.insert("s1".into(), json!(utf16(text, end)));
    Value::Object(m)
}

/// UTF-16 index of a byte offset (clamped to a char boundary — files lie).
fn utf16(text: &str, byte: usize) -> usize {
    let mut b = byte.min(text.len());
    while b > 0 && !text.is_char_boundary(b) {
        b -= 1;
    }
    text[..b].encode_utf16().count()
}

fn clamp_boundary(text: &str, byte: usize) -> usize {
    let mut b = byte.min(text.len());
    while b > 0 && !text.is_char_boundary(b) {
        b -= 1;
    }
    b
}

fn align_str(a: Align) -> &'static str {
    match a {
        Align::Left => "left",
        Align::Center => "center",
        Align::Right => "right",
        Align::Justify => "justify",
        Align::Distribute => "distribute",
    }
}

fn line_spacing_json(l: &LineSpacing) -> Value {
    match l {
        LineSpacing::Multiple(v) => json!({ "kind": "multiple", "value": v }),
        LineSpacing::AtLeast(v) => json!({ "kind": "atLeast", "value": v }),
        LineSpacing::Exactly(v) => json!({ "kind": "exactly", "value": v }),
    }
}

fn underline_str(u: Underline) -> Option<&'static str> {
    Some(match u {
        Underline::None => return None,
        Underline::Single => "single",
        Underline::Words => "words",
        Underline::Double => "double",
        Underline::Thick => "thick",
        Underline::Dotted => "dotted",
        Underline::Dash => "dash",
        Underline::DotDash => "dotDash",
        Underline::DotDotDash => "dotDotDash",
        Underline::Wave => "wave",
        Underline::DoubleWave => "doubleWave",
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use wordcraft_engine::Session;

    #[test]
    fn blank_document_state() {
        let s = Session::new(wordcraft_doc::Document::new());
        let v = state_json(&s);
        assert_eq!(v["canUndo"], Value::Bool(false));
        assert_eq!(v["blocks"][0]["k"], "p");
        assert!(v["format"]["bold"].is_boolean());
        assert_eq!(v["para"]["style"], "Normal");
    }

    #[test]
    fn spans_are_utf16_indices() {
        let s = Session::new(wordcraft_doc::Document::from_text("héllo"));
        let v = state_json(&s);
        let t = v["blocks"][0]["text"].as_str().unwrap();
        assert_eq!(t, "héllo");
        // A single run covers the whole string: 5 UTF-16 units.
        let span = &v["blocks"][0]["spans"][0];
        assert_eq!(span["s0"], 0);
        assert_eq!(span["s1"], 5);
    }

    #[test]
    fn list_labels_walk_in_order() {
        let mut doc = wordcraft_doc::Document::from_text("one\ntwo\nthree");
        let n = doc.numbering.add_list(wordcraft_doc::ListKind::Bullet);
        for i in 0..3 {
            if let Block::Para(p) = Arc::make_mut(&mut doc.body[i]) {
                p.props.numbering = Some(wordcraft_doc::props::NumRef { num: n, level: 0 });
            }
        }
        let s = Session::new(doc);
        let v = state_json(&s);
        assert_eq!(v["blocks"][0]["list"]["label"], "•");
        assert_eq!(v["blocks"][1]["list"]["label"], "•");
    }
}
