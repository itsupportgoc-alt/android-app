#!/usr/bin/env python3
"""Build the WordCraft engine command catalog from the upstream command sources.

Reads `crates/engine/src/cmd/*.rs` (the engine's command registry) and writes a
JSON catalog of every command: id, label, ribbon location (`Tab › Group`) and the
resolved parameter contract. Parameter contracts are read from the `.params(...)`
chain, following const references back to their `const NAME: &str = r#"…"#;`
definition in the same file.

Usage:
    python3 tools/build_catalog.py <cmd-src-dir> <out.json>
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

SPEC_RE = re.compile(
    r'CommandSpec::new\(\s*"(?P<id>[^"]+)"\s*,\s*"(?P<label>(?:[^"\\]|\\.)*)"\s*,\s*'
    r'"(?P<location>(?:[^"\\]|\\.)*)"',
    re.S,
)
# `.params(r#"…"#)` or `.params("…")` or `.params(CONST)`
PARAMS_RE = re.compile(
    r'\.params\(\s*(?:r#"(?P<raw>.*?)"#|"(?P<plain>(?:[^"\\]|\\.)*)"|(?P<const>[A-Z][A-Z0-9_]*))',
    re.S,
)
KEY_RE = re.compile(r'\.key\(\s*(?:r#"(?P<raw>.*?)"#|"(?P<plain>(?:[^"\\]|\\.)*)")', re.S)
CONST_RE = re.compile(
    r'(?:pub\s+)?const\s+([A-Z][A-Z0-9_]*)\s*:\s*&str\s*=\s*(?:r#"(?P<raw>.*?)"#|"(?P<plain>(?:[^"\\]|\\.)*)")\s*;',
    re.S,
)


def unescape(s: str) -> str:
    return s.encode("utf-8").decode("unicode_escape").encode("latin-1").decode("utf-8")


def body(m: re.Match) -> str:
    if m.group("raw") is not None:
        return m.group("raw")
    return unescape(m.group("plain"))


def main() -> int:
    if len(sys.argv) != 3:
        print(__doc__)
        return 2
    src, out = Path(sys.argv[1]), Path(sys.argv[2])
    catalog: list[dict] = []
    seen: set[str] = set()
    for f in sorted(src.glob("*.rs")):
        text = f.read_text(encoding="utf-8")
        consts = {m.group(1): body(m) for m in CONST_RE.finditer(text)}
        for m in SPEC_RE.finditer(text):
            cid = m.group("id")
            if cid in seen:
                continue
            seen.add(cid)
            # Only look at this spec's own chain: stop at the next command spec.
            tail = text[m.end():]
            nxt = tail.find("CommandSpec::new(")
            if nxt >= 0:
                tail = tail[:nxt]
            params = "{}"
            pm = PARAMS_RE.search(tail)
            if pm:
                if pm.group("const"):
                    params = consts.get(pm.group("const"), "{}")
                else:
                    params = body(pm)
            shortcut = ""
            km = KEY_RE.search(tail)
            if km:
                shortcut = body(km)
            catalog.append(
                {
                    "id": cid,
                    "label": unescape(m.group("label")),
                    "location": unescape(m.group("location")),
                    "params": params,
                    "shortcut": shortcut,
                    "module": f.stem,
                }
            )
    out.write_text(json.dumps(catalog, indent=1, ensure_ascii=False), encoding="utf-8")
    print(f"wrote {len(catalog)} commands to {out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
