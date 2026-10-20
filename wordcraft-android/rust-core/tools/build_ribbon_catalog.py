#!/usr/bin/env python3
"""Generate the Android ribbon catalog from the WordCraft engine's command registry.

The engine's `CommandSpec::new(id, label, location)` entries carry the ribbon
location ("Tab › Group") — the upstream engine *is* the ribbon model.  This script
turns that registry into the Kotlin catalog the Android app renders:

  * only ids that are really present in the shipped `libwordcraft_core.so` (the
    app cannot be rebuilt against a newer engine — the Rust crates are not
    vendored — so a missing id would be a dead button),
  * the documented parameter contract of every command becomes the fields of the
    "options" dialog the ribbon opens,
  * commands that need a platform service (a picture, a file, a print job) are
    marked as app actions instead.

Usage:
    python3 tools/build_ribbon_catalog.py <cmd-src-dir> <libwordcraft_core.so> <out.kt>
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from build_catalog import SPEC_RE, unescape  # noqa: E402

# ----------------------------------------------------------------- the tabs we render

TABS = ["Insert", "Draw", "Design", "Layout", "References", "Mailings", "Review"]

# Commands whose registry entry carries no group ("References > Update Field"). They are put in
# the group Word puts them in, so no group is ever captioned with an empty string.
GROUP_ALIASES: dict[tuple[str, str], str] = {
    ("References", ""): "Fields",
}

# Commands the app layer owns (they need a picker, a print job or a share sheet).
APP_ACTIONS = {
    "insert.picture": "PICK_IMAGE",
    "insert.textFromFile": "PICK_TEXT_FILE",
    "mailings.recipients": "PICK_CSV",
    "review.compare": "PICK_DOCUMENT",
    "review.combine": "PICK_DOCUMENT",
}

# Fields the engine expects but does not document in `.params(...)` (found by
# scanning the command bodies for `req_*` parameter reads).
EXTRA_FIELDS = {
    "design.themeColors": [("name", "TEXT", True, [])],
    "design.theme": [("name", "TEXT", True, [])],
    "insert.icon": [("id", "TEXT", False, [])],
}

# Dynamic choice providers: the field is filled from a live engine query.
CHOICE_PROVIDERS = {
    "design.theme": ("design.themes", "themes"),
    "design.themeColors": ("design.themes", "themes"),
}

# The commands Word draws as *large* buttons (icon above the label) in each group.
# The rest of the group becomes the small icon-and-label rows underneath.
LARGE_COMMANDS: dict[tuple[str, str], list[str]] = {
    ("Insert", "Illustrations"): ["insert.picture", "insert.shape", "insert.icon"],
    ("Insert", "Links"): ["insert.link"],
    ("Insert", "Symbols"): ["insert.equation", "insert.symbol"],
    ("Insert", "Text"): ["insert.textBox", "insert.wordArt", "insert.object"],
    ("Insert", "Pages"): ["insert.coverPage", "insert.blankPage", "insert.pageBreak"],
    ("Insert", "Tables"): ["insert.table", "table.fromText"],
    ("Insert", "Header & Footer"): ["insert.header", "insert.footer", "insert.pageNumber"],
    ("Draw", "Tools"): ["draw.select", "draw.lasso", "draw.eraser"],
    ("Draw", "Pens"): ["draw.pen", "draw.pencil", "draw.highlighter"],
    ("Draw", "Convert"): ["draw.inkToShape"],
    ("Draw", "Replay"): ["draw.replay"],
    ("Design", "Document Formatting"): ["design.theme", "design.themeColors", "design.themeFonts"],
    ("Design", "Page Background"): ["design.watermark", "design.pageColor", "design.pageBorders"],
    ("Layout", "Arrange"): ["arrange.position", "arrange.wrap", "arrange.align"],
    ("Layout", "Page Setup"): ["layout.margins", "layout.orientation", "layout.size"],
    ("Layout", "Paragraph"): ["para.indents", "para.spacing"],
    ("Layout", "Breaks"): ["text.columnBreak"],
    ("References", "Citations & Bibliography"): ["references.citation", "references.bibliography", "references.sources"],
    ("References", "Captions"): ["references.caption"],
    ("References", "Table of Authorities"): ["references.tableOfAuthorities"],
    ("References", "Footnotes"): ["references.footnote", "references.endnote"],
    ("References", "Research"): ["references.researcher"],
    ("References", "Index"): ["references.index", "references.markEntry"],
    ("References", "Table of Contents"): ["references.toc", "references.updateToc"],
    ("References", "Fields"): ["references.updateFields"],
    ("Mailings", "Start Mail Merge"): ["mailings.start", "mailings.recipients"],
    ("Mailings", "Write & Insert Fields"): ["mailings.addressBlock", "mailings.greetingLine", "mailings.insertField"],
    ("Mailings", "Preview Results"): ["mailings.preview"],
    ("Mailings", "Finish"): ["mailings.finish"],
    ("Mailings", "Create"): ["mailings.envelopes", "mailings.labels"],
    ("Review", "Ink"): ["review.hideInk"],
    ("Review", "Protect"): ["review.restrict"],
    ("Review", "Comments"): ["review.newComment", "review.comments", "review.reply"],
    ("Review", "Tracking"): ["review.trackChanges", "review.showMarkup"],
    ("Review", "Changes"): ["review.accept", "review.reject"],
    ("Review", "Proofing"): ["review.spelling", "review.wordCount", "review.thesaurus"],
    ("Review", "Speech"): ["review.readAloud"],
    ("Review", "Compare"): ["review.compare", "review.combine"],
    ("Review", "Accessibility"): ["file.accessibility"],
    ("Review", "Language"): ["review.language"],
}

# ----------------------------------------------------------------- icons

ICON_RULES: list[tuple[str, str]] = [
    ("insert.picture", "picture"),
    ("insert.icon", "icons"),
    ("insert.shape", "shapes"),
    ("insert.chart", "chart"),
    ("insert.smartArt", "smartart"),
    ("insert.canvas", "canvas"),
    ("insert.textBox", "textbox"),
    ("insert.wordArt", "wordart"),
    ("insert.equation", "equation"),
    ("insert.symbol", "symbol"),
    ("insert.field", "field"),
    ("fields.", "field"),
    ("insert.link", "link"),
    ("link.", "link"),
    ("insert.bookmark", "bookmark"),
    ("insert.crossReference", "crossref"),
    ("insert.coverPage", "coverPage"),
    ("insert.dateTime", "dateTime"),
    ("insert.dateField", "dateTime"),
    ("insert.timeField", "dateTime"),
    ("insert.dropCap", "dropCap"),
    ("insert.textFromFile", "textFile"),
    ("insert.quickParts", "quickParts"),
    ("insert.autoText", "quickParts"),
    ("insert.docProperty", "docProperty"),
    ("insert.signatureLine", "signature"),
    ("insert.object", "object"),
    ("insert.spreadsheet", "spreadsheet"),
    ("insert.header", "header"),
    ("insert.editHeader", "header"),
    ("insert.removeHeader", "header"),
    ("insert.createHeader", "header"),
    ("insert.footer", "footer"),
    ("insert.editFooter", "footer"),
    ("insert.removeFooter", "footer"),
    ("insert.createFooter", "footer"),
    ("insert.pageNumber", "pageNumber"),
    ("layout.pageNumberFormat", "pageNumber"),
    ("insert.pageBreak", "pagebreak"),
    ("text.pageBreak", "pagebreak"),
    ("insert.blankPage", "coverPage"),
    ("table.", "table"),
    ("design.themeFonts", "fonts"),
    ("design.themeColors", "colors"),
    ("design.themes", "theme"),
    ("design.theme", "theme"),
    ("design.styleSet", "styleSet"),
    ("design.paragraphSpacing", "spacing"),
    ("design.watermark", "watermark"),
    ("design.pageColor", "pageColor"),
    ("design.pageBorders", "pageBorders"),
    ("design.effects", "effects"),
    ("design.setDefault", "save"),
    ("arrange.rotate", "rotate"),
    ("arrange.rotation", "rotate"),
    ("arrange.wrap", "wrap"),
    ("arrange.wrapText", "wrap"),
    ("arrange.moveWithText", "position"),
    ("arrange.position", "position"),
    ("arrange.bounds", "size"),
    ("arrange.nudge", "position"),
    ("arrange.group", "group"),
    ("arrange.ungroup", "group"),
    ("arrange.selectionPane", "list"),
    ("arrange.align", "align"),
    ("arrange.bringForward", "position"),
    ("arrange.sendBackward", "position"),
    ("layout.margins", "margins"),
    ("layout.orientation", "orientation"),
    ("layout.size", "size"),
    ("layout.columns", "columns"),
    ("layout.break", "breaks"),
    ("layout.pageSetup", "margins"),
    ("layout.section", "margins"),
    ("layout.lineNumbers", "lineNumbers"),
    ("layout.hyphenation", "hyphenation"),
    ("layout.manualHyphenation", "hyphenation"),
    ("layout.verticalAlign", "align"),
    ("layout.sectionRtl", "align"),
    ("layout.textDirection", "align"),
    ("layout.documentGrid", "grid"),
    ("para.indents", "indents"),
    ("para.spacing", "spacing"),
    ("text.columnBreak", "breaks"),
    ("references.citation", "citations"),
    ("references.citations", "citations"),
    ("references.sources", "bibliography"),
    ("references.bibliography", "bibliography"),
    ("references.citationStyle", "bibliography"),
    ("references.caption", "caption"),
    ("references.tableOfFigures", "caption"),
    ("references.updateFigures", "caption"),
    ("references.markCitation", "citations"),
    ("references.tableOfAuthorities", "citations"),
    ("references.updateAuthorities", "citations"),
    ("references.noteOptions", "footnote"),
    ("references.footnote", "footnote"),
    ("references.endnote", "footnote"),
    ("references.convertNotes", "footnote"),
    ("references.nextFootnote", "footnote"),
    ("references.notes", "footnote"),
    ("references.markEntry", "index"),
    ("references.index", "index"),
    ("references.updateIndex", "index"),
    ("references.toc", "toc"),
    ("references.updateToc", "toc"),
    ("references.removeToc", "toc"),
    ("references.addText", "toc"),
    ("references.researcher", "research"),
    ("references.updateFields", "field"),
    ("mailings.", "mail"),
    ("review.newComment", "comment"),
    ("review.reply", "comment"),
    ("review.editComment", "comment"),
    ("review.deleteComment", "comment"),
    ("review.resolveComment", "comment"),
    ("review.nextComment", "comment"),
    ("review.previousComment", "comment"),
    ("review.comments", "comment"),
    ("view.commentsPane", "comment"),
    ("review.trackChanges", "trackChanges"),
    ("review.trackingOptions", "trackChanges"),
    ("review.markup", "trackChanges"),
    ("review.showMarkup", "trackChanges"),
    ("review.changes", "trackChanges"),
    ("review.acceptAll", "accept"),
    ("review.accept", "accept"),
    ("review.rejectAll", "reject"),
    ("review.reject", "reject"),
    ("review.nextChange", "trackChanges"),
    ("review.previousChange", "trackChanges"),
    ("review.restrict", "protect"),
    ("review.wordCount", "wordCount"),
    ("review.spelling", "proofing"),
    ("review.issues", "proofing"),
    ("review.suggestions", "proofing"),
    ("review.addToDictionary", "proofing"),
    ("review.ignoreAll", "proofing"),
    ("review.applySuggestion", "proofing"),
    ("review.thesaurus", "thesaurus"),
    ("review.proofing", "proofing"),
    ("review.readAloud", "readAloud"),
    ("readAloud.", "readAloud"),
    ("review.compare", "compare"),
    ("review.combine", "compare"),
    ("review.language", "language"),
    ("file.accessibility", "accessibility"),
    ("view.accessibilityPane", "accessibility"),
    ("view.altTextPane", "accessibility"),
    ("draw.pen", "pen"),
    ("draw.pencil", "pencil"),
    ("draw.highlighter", "highlighter"),
    ("draw.addPen", "pen"),
    ("draw.removePen", "pen"),
    ("draw.eraser", "eraser"),
    ("draw.erase", "eraser"),
    ("draw.lasso", "lasso"),
    ("draw.lassoMove", "lasso"),
    ("draw.lassoDelete", "eraser"),
    ("draw.lassoColor", "colorize"),
    ("draw.select", "ink"),
    ("draw.stroke", "ink"),
    ("draw.inkToShape", "convert"),
    ("draw.convertInk", "convert"),
    ("draw.replay", "replay"),
]

DEFAULT_ICON = "generic"


def icon_for(cid: str) -> str:
    for needle, icon in ICON_RULES:
        if cid.startswith(needle) or needle in cid:
            return icon
    return DEFAULT_ICON


# ----------------------------------------------------------------- parameter contracts


def split_top(text: str, sep: str = ",") -> list[str]:
    """Split on `sep` at brace/bracket/paren depth 0, ignoring quoted text."""
    parts: list[str] = []
    depth = 0
    cur = ""
    in_str = False
    esc = False
    for ch in text:
        if in_str:
            cur += ch
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == '"':
                in_str = False
            continue
        if ch == '"':
            in_str = True
            cur += ch
            continue
        if ch in "{[(":
            depth += 1
        elif ch in "}])":
            depth -= 1
        if ch == sep and depth == 0:
            parts.append(cur)
            cur = ""
        else:
            cur += ch
    if cur.strip():
        parts.append(cur)
    return parts


def first_object(text: str) -> str:
    """The first `{...}` of the contract (alternatives separated by `|` are ignored)."""
    start = text.find("{")
    if start < 0:
        return ""
    depth = 0
    in_str = False
    esc = False
    for i in range(start, len(text)):
        ch = text[i]
        if in_str:
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == '"':
                in_str = False
            continue
        if ch == '"':
            in_str = True
        elif ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return text[start : i + 1]
    return ""


FIELD_RE = re.compile(r'^\s*"(?P<name>[A-Za-z_][\w]*)"\s*(?P<opt>\?)?\s*:\s*(?P<rest>.*)$', re.S)
QUOTED_RE = re.compile(r'^"(?P<lit>[^"]*)"')
TOKEN_RE = re.compile(r"^(?P<tok>[A-Za-z_][\w]*)")
# `"a"|"b"|"c"` - a set of literal choices
LITERAL_SET_RE = re.compile(r'^"(?P<first>[^"]*)"(?P<rest>(\s*\|\s*"[^"]*")+)')
# A numeric range such as `1-3` or `1..10`
RANGE_RE = re.compile(r"^\d+(\s*-\s*|\s*\.\.\s*)\d+$")

# Types the engine reads as a structured value: a text box cannot produce them.
COMPLEX_TYPES = {"Source", "Pos", "SectionProps", "StoryRef", "SectionRef", "SourceRef"}


def clean_option(text: str) -> str:
    """`heading` - drop the trailing explanation the contract adds."""
    text = text.split(" (")[0].strip()
    return text


def field_kind(rest: str) -> tuple[str, list[str]] | None:
    """(kind, options) for a contract entry, or None when it is not editable."""
    rest = rest.strip()
    if not rest:
        return None
    if rest[0] in "[{":
        return None  # array / object
    m = LITERAL_SET_RE.match(rest)
    if m:
        options = [m.group("first")] + re.findall(r'"([^"]*)"', m.group("rest"))
        options = [clean_option(o) for o in options]
        options = [o for o in options if o and "…" not in o and "..." not in o]
        return ("CHOICE", options)
    m = QUOTED_RE.match(rest)
    if m:
        lit = m.group("lit")
        if "|" in lit:
            options = [clean_option(o) for o in lit.split("|") if o]
            options = [o for o in options if o and "…" not in o and "..." not in o]
            return ("CHOICE", options)
        return ("TEXT", [])
    if RANGE_RE.match(rest):
        return ("NUMBER", [])
    m = TOKEN_RE.match(rest)
    if not m:
        return None
    tok = m.group("tok")
    if tok in COMPLEX_TYPES:
        return None
    if tok in ("n", "number", "pt", "f32", "f64", "i32", "u32"):
        return ("NUMBER", [])
    if tok in ("bool", "boolean"):
        return ("BOOL", [])
    if tok in ("string", "name", "str", "base64", "path", "text", "id", "tag", "code", "instr", "query", "category", "url"):
        return ("TEXT", [])
    return ("TEXT", [])


def parse_fields(params: str) -> list[tuple[str, str, bool, list[str]]]:
    """[(name, kind, required, options)] from a documented parameter contract."""
    obj = first_object(params)
    if not obj:
        return []
    inner = obj[1:-1]
    out: list[tuple[str, str, bool, list[str]]] = []
    for part in split_top(inner):
        m = FIELD_RE.match(part)
        if not m:
            continue
        kind = field_kind(m.group("rest"))
        if kind is None:
            continue
        k, opts = kind
        out.append((m.group("name"), k, not bool(m.group("opt")), opts))
    return out


# ----------------------------------------------------------------- the engine registry


CONST_RE = re.compile(
    r'(?:pub\s+)?const\s+([A-Z][A-Z0-9_]*)\s*:\s*&str\s*=\s*(?:r#"(?P<raw>.*?)"#|"(?P<plain>(?:[^"\\]|\\.)*)")\s*;',
    re.S,
)


def registry_region(text: str) -> str:
    """The body of `fn specs()` - the command registry (helpers live outside it)."""
    start = text.find("fn specs")
    if start < 0:
        return text
    end = text.find("\n}", start)
    return text[start : end if end > 0 else len(text)]


def load_registry(src: Path) -> list[dict]:
    cmds: list[dict] = []
    seen: set[str] = set()
    for f in sorted(src.glob("*.rs")):
        text = f.read_text(encoding="utf-8")
        consts = {m.group(1): unescape(m.group("raw") if m.group("raw") is not None else m.group("plain")) for m in CONST_RE.finditer(text)}
        region = registry_region(text)
        for m in SPEC_RE.finditer(region):
            cid = m.group("id")
            if cid in seen:
                continue
            seen.add(cid)
            tail = region[m.end():]
            nxt = tail.find("CommandSpec::new(")
            body = tail[:nxt] if nxt >= 0 else tail
            params = "{}"
            pm = re.search(
                r'\.params\(\s*(?:r#"(?P<raw>.*?)"#|"(?P<plain>(?:[^"\\]|\\.)*)"|(?P<const>[A-Z][A-Z0-9_]*))',
                body,
                re.S,
            )
            if pm:
                if pm.group("const"):
                    params = consts.get(pm.group("const"), "{}")
                else:
                    params = unescape(pm.group("raw") if pm.group("raw") is not None else pm.group("plain"))
            # Required parameters the contract forgot to document.
            required: list[str] = []
            for rm in re.finditer(r'req_\w+\(\s*v\s*,\s*"(?P<name>[A-Za-z_][\w]*)"', body):
                required.append(rm.group("name"))
            cmds.append(
                {
                    "id": cid,
                    "label": unescape(m.group("label")),
                    "location": unescape(m.group("location")),
                    "params": params,
                    "required": sorted(set(required)),
                }
            )
    return cmds


# ----------------------------------------------------------------- Kotlin emission


def kt_string(s: str) -> str:
    return '"' + s.replace("\\", "\\\\").replace('"', '\\"').replace("$", "\\$") + '"'


def kt_field(name: str, kind: str, required: bool, options: list[str], provider: str | None = None) -> str:
    label = name
    parts = [kt_string(name), f"FieldKind.{kind}", kt_string(label), "true" if required else "false"]
    if provider:
        parts.append(kt_string(provider))
    elif options:
        parts.append("null")
    if options:
        parts.append(", ".join(kt_string(o) for o in options))
    return f'ribbonField({", ".join(parts)})'


def main() -> int:
    if len(sys.argv) != 4:
        print(__doc__)
        return 2
    src, so_path, out_path = Path(sys.argv[1]), Path(sys.argv[2]), Path(sys.argv[3])
    cmds = load_registry(src)
    blob = so_path.read_bytes() if so_path.exists() else b""
    if not blob:
        print(f"warning: {so_path} not found — cannot verify ids", file=sys.stderr)

    tabs: dict[str, dict[str, list[dict]]] = {t: {} for t in TABS}
    skipped: list[str] = []
    for c in cmds:
        tab = c["location"].split("›")[0].strip()
        if tab not in tabs:
            continue
        if blob and c["id"].encode() not in blob:
            skipped.append(c["id"])
            continue
        group = c["location"].split("›")[1].strip() if "›" in c["location"] else ""
        group = GROUP_ALIASES.get((tab, group), group)
        tabs[tab].setdefault(group, []).append(c)

    lines: list[str] = []
    add = lines.append
    add("package com.wordcraft.editor")
    add("")
    add("// GENERATED FILE - do not edit by hand.")
    add("//")
    add("// Source: the WordCraft engine's own command registry (crates/engine/src/cmd/*.rs), whose")
    add("// `CommandSpec::new(id, label, location)` entries carry the ribbon location (\"Tab > Group\").")
    add("// Regenerate with:")
    add("//   python3 tools/build_ribbon_catalog.py <cmd-src-dir> <libwordcraft_core.so> <out.kt>")
    add("//")
    add("// Every id below was verified to be present in the shipped libwordcraft_core.so, so each")
    add("// button is backed by a command the app can really run.")
    add("")
    add("/** Tabs whose ribbon is generated from the engine's command registry. */")
    add("val EngineRibbonTabs = listOf(" + ", ".join(kt_string(t) for t in TABS) + ")")
    add("")

    for tab in TABS:
        varname = tab.lower() + "Ribbon"
        add(f"private val {varname} = listOf(")
        groups = tabs[tab]
        gi = 0
        for caption, items in groups.items():
            gi += 1
            large_ids = set(LARGE_COMMANDS.get((tab, caption), []))
            # A group with a single command has nothing to lay out beside it, so that command
            # is drawn as the group's large button (as Word does).
            if len(items) == 1:
                large_ids.add(items[0]["id"])
            add(f"    RibbonGroup({kt_string(caption)}, listOf(")
            for c in items:
                cid = c["id"]
                fields = parse_fields(c["params"])
                seen_names = {f[0] for f in fields}
                for req in c["required"]:
                    if req not in seen_names:
                        fields.append((req, "TEXT", True, []))
                        seen_names.add(req)
                for extra_name, extra_kind, extra_req, extra_opts in EXTRA_FIELDS.get(cid, []):
                    if extra_name not in seen_names:
                        fields.append((extra_name, extra_kind, extra_req, extra_opts))
                        seen_names.add(extra_name)
                provider = CHOICE_PROVIDERS[cid][0] if cid in CHOICE_PROVIDERS else None
                app = APP_ACTIONS.get(cid)
                args = [kt_string(cid), kt_string(c["label"]), kt_string(icon_for(cid))]
                if app:
                    args.append(f"appAction = AppActionKind.{app}")
                elif fields:
                    args.append("fields = listOf(" + ", ".join(kt_field(f[0], f[1], f[2], f[3], provider if f[0] == "name" else None) for f in fields) + ")")
                if cid in large_ids:
                    args.append("large = true")
                add(f"        ribbonCmd({', '.join(args)}),")
            add("    ))," + ("" if gi < len(groups) else ""))
        add(")")
        add("")

    add("object EngineRibbon {")
    add("    /** Tab -> its captioned ribbon groups, in the engine's own order. */")
    add("    val tabs: Map<String, List<RibbonGroup>> = mapOf(")
    for tab in TABS:
        add(f"        {kt_string(tab)} to {tab.lower()}Ribbon,")
    add("    )")
    add("}")
    add("")

    # ---- the index of every engine command (the command search needs its parameters) ----
    index_lines: list[str] = []
    iadd = index_lines.append
    iadd("package com.wordcraft.editor")
    iadd("")
    iadd("// GENERATED FILE - do not edit by hand.")
    iadd("//")
    iadd("// The parameter contract of every command the shipped engine exposes, so the command")
    iadd("// search can open the same options dialog the ribbon opens.")
    iadd("// Regenerate with tools/build_ribbon_catalog.py.")
    iadd("")
    iadd("object EngineCommandIndex {")
    iadd("    /** Command id -> the fields its options dialog shows. */")
    iadd("    val fields: Map<String, List<RibbonField>> = mapOf(")
    for c in cmds:
        if blob and c["id"].encode() not in blob:
            continue
        flds = parse_fields(c["params"])
        seen_names = {f[0] for f in flds}
        for req in c["required"]:
            if req not in seen_names:
                flds.append((req, "TEXT", True, []))
                seen_names.add(req)
        provider = CHOICE_PROVIDERS[c["id"]][0] if c["id"] in CHOICE_PROVIDERS else None
        if not flds:
            continue
        rendered = ", ".join(
            kt_field(f[0], f[1], f[2], f[3], provider if (provider and f[0] == "name") else None) for f in flds
        )
        iadd(f"        {kt_string(c['id'])} to listOf({rendered}),")
    iadd("    )")
    iadd("")
    iadd("    /** The fields of [id] (empty when the command takes none). */")
    iadd("    fun fieldsOf(id: String): List<RibbonField> = fields[id].orEmpty()")
    iadd("}")
    iadd("")
    index_path = out_path.with_name("EngineCommandIndex.kt")
    index_path.write_text("\n".join(index_lines), encoding="utf-8")
    print(f"wrote the command index to {index_path}")

    out_path.write_text("\n".join(lines), encoding="utf-8")
    total = sum(len(v) for t in tabs.values() for v in t.values())
    print(f"wrote {total} commands for {len(TABS)} tabs to {out_path}")
    if skipped:
        print(f"skipped {len(skipped)} ids that are not in the shipped .so: {', '.join(skipped)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
