#!/usr/bin/env python3
"""Extract the WordCraft engine command registry and check it against the shipped .so.

Reads the upstream `crates/engine/src/cmd/*.rs` sources (the engine's command
registry), extracts every `CommandSpec::new(id, label, location, ...)` together
with its `.params("...")` / `.key("...")` / `.pure()` chain, and reports which ids
are actually present in the built `libwordcraft_core.so`.

Usage:
    python3 tools/extract_commands.py <cmd-src-dir> <libwordcraft_core.so> [out.json]
"""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

# `CommandSpec::new("id", "label", "location", ...)` — the id/label/location are the
# first three string arguments; the run closure follows.
SPEC_RE = re.compile(
    r'CommandSpec::new\(\s*"(?P<id>[^"]+)"\s*,\s*"(?P<label>(?:[^"\\]|\\.)*)"\s*,\s*'
    r'"(?P<location>(?:[^"\\]|\\.)*)"',
    re.S,
)
CHAIN_RE = re.compile(r'\.(?P<what>params|key|pure|when)\((?P<arg>"(?:[^"\\]|\\.)*")?\)')


def unescape(s: str) -> str:
    return s.encode("utf-8").decode("unicode_escape").encode("latin-1").decode("utf-8")


def parse_dir(src: Path) -> list[dict]:
    cmds: list[dict] = []
    for f in sorted(src.glob("*.rs")):
        text = f.read_text(encoding="utf-8")
        for m in SPEC_RE.finditer(text):
            tail = text[m.end(): m.end() + 600]
            params = "{}"
            shortcut = ""
            for c in CHAIN_RE.finditer(tail):
                what = c.group("what")
                arg = c.group("arg")
                if what == "params" and arg:
                    params = unescape(arg[1:-1])
                elif what == "key" and arg:
                    shortcut = unescape(arg[1:-1])
                elif what == "pure":
                    break
            cmds.append(
                {
                    "id": m.group("id"),
                    "label": unescape(m.group("label")),
                    "location": unescape(m.group("location")),
                    "params": params,
                    "shortcut": shortcut,
                    "module": f.stem,
                }
            )
    return cmds


def main() -> int:
    if len(sys.argv) < 3:
        print(__doc__)
        return 2
    src = Path(sys.argv[1])
    so = Path(sys.argv[2])
    cmds = parse_dir(src)
    blob = so.read_bytes() if so.exists() else b""
    for c in cmds:
        c["in_so"] = bool(blob) and (c["id"].encode() in blob)
    out = Path(sys.argv[3]) if len(sys.argv) > 3 else None
    if out:
        out.write_text(json.dumps(cmds, indent=1), encoding="utf-8")
    present = sum(1 for c in cmds if c["in_so"])
    print(f"parsed {len(cmds)} specs; {present} ids found in {so.name}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
