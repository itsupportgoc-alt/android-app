#!/usr/bin/env python3
"""Show label/location/params for chosen WordCraft engine command ids.

Reads the upstream `crates/engine/src/cmd/*.rs` sources and resolves the
`.params(CONST)` references against the `const NAME: &str = r#"..."#;` blocks in
the same file, so the printed contract is the real one.

Usage:
    python3 tools/command_info.py <cmd-src-dir> <id> [<id> …]
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

SPEC_RE = re.compile(
    r'CommandSpec::new\(\s*"(?P<id>[^"]+)"\s*,\s*"(?P<label>(?:[^"\\]|\\.)*)"\s*,\s*'
    r'"(?P<location>(?:[^"\\]|\\.)*)"',
    re.S,
)
CONST_RE = re.compile(r'(?:pub\s+)?const\s+([A-Z0-9_]+)\s*:\s*&str\s*=\s*r#"(?P<body>.*?)"#\s*;', re.S)
RAW_CONST_RE = re.compile(r'(?:pub\s+)?const\s+([A-Z0-9_]+)\s*:\s*&str\s*=\s*"(?P<body>(?:[^"\\]|\\.)*)"\s*;', re.S)
CHAIN_RE = re.compile(r'\.(?P<what>params|key|pure|when)\((?P<arg>"(?:[^"\\]|\\.)*")?\)')
IDENT_ARG_RE = re.compile(r'\.params\(\s*([A-Z0-9_]+)\s*\)')


def unescape(s: str) -> str:
    return s.encode("utf-8").decode("unicode_escape").encode("latin-1").decode("utf-8")


def main() -> int:
    if len(sys.argv) < 3:
        print(__doc__)
        return 2
    src = Path(sys.argv[1])
    wanted = set(sys.argv[2:])
    for f in sorted(src.glob("*.rs")):
        text = f.read_text(encoding="utf-8")
        consts: dict[str, str] = {}
        for m in list(CONST_RE.finditer(text)) + list(RAW_CONST_RE.finditer(text)):
            consts[m.group(1)] = m.group("body")
        for m in SPEC_RE.finditer(text):
            cid = m.group("id")
            if cid not in wanted:
                continue
            tail = text[m.end(): m.end() + 900]
            params = "(default {})"
            for c in CHAIN_RE.finditer(tail):
                if c.group("what") == "params":
                    arg = c.group("arg")
                    params = unescape(arg[1:-1]) if arg else "(default {})"
                elif c.group("what") == "pure":
                    break
            idm = IDENT_ARG_RE.search(tail)
            if idm and idm.group(1) in consts:
                params = consts[idm.group(1)]
            print(f"### {cid}\n    label    : {unescape(m.group('label'))}\n"
                  f"    location : {unescape(m.group('location'))}\n    params   : {params}\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
