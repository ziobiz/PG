#!/usr/bin/env python3
"""Insert exact-match proxy for /, /login.html, /index.html in icopay.co.kr nginx servers.

LINE/WhatsApp read the first HTML. Apex icopay.co.kr was serving static files, so OG
never came from pg-app. Portal hosts (jpjp/hqth) already proxy to 8080.
"""
from __future__ import annotations

import pathlib
import sys

SNIPPET = """
    # LINE/WhatsApp Open Graph: first HTML via pg-app (same as jpjp/hqth)
    location = / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_hide_header ETag;
        add_header Cache-Control "no-store";
    }
    location = /login.html {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_hide_header ETag;
        add_header Cache-Control "no-store";
    }
    location = /index.html {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_hide_header ETag;
        add_header Cache-Control "no-store";
    }
"""

MARK = "location = /login.html"


def iter_conf_files() -> list[pathlib.Path]:
    roots = [pathlib.Path("/etc/nginx")]
    out: list[pathlib.Path] = []
    for root in roots:
        if not root.exists():
            continue
        out.extend(p for p in root.rglob("*.conf") if p.is_file())
    return out


def server_blocks(text: str) -> list[tuple[int, int]]:
    """Return (start, end) indices of server { } blocks (brace-matched)."""
    blocks = []
    i = 0
    n = len(text)
    while True:
        j = text.find("server", i)
        if j < 0:
            break
        k = text.find("{", j)
        if k < 0:
            break
        # ensure it's a server { not server_name
        between = text[j + 6 : k]
        if between.strip() not in ("",):
            i = j + 6
            continue
        depth = 0
        p = k
        while p < n:
            if text[p] == "{":
                depth += 1
            elif text[p] == "}":
                depth -= 1
                if depth == 0:
                    blocks.append((j, p + 1))
                    i = p + 1
                    break
            p += 1
        else:
            break
    return blocks


def is_apex_icopay(block: str) -> bool:
    if "try_files" not in block:
        return False
    if "listen 443" not in block:
        return False
    if "server_name" not in block:
        return False
    # Match apex / www only, not api. or hqth. or jpjp.
    names = []
    for line in block.splitlines():
        s = line.strip()
        if s.startswith("server_name"):
            names.append(s)
    blob = " ".join(names)
    if "icopay.co.kr" not in blob:
        return False
    # skip API / portal dedicated servers
    skip_tokens = ("api.icopay.co.kr", "hqth.", "jpjp.", "jp.icopay.co.kr")
    # If the only icopay names are apex/www, patch.
    tokens = blob.replace(";", " ").split()
    icopay_hosts = [t for t in tokens if "icopay.co.kr" in t]
    if not icopay_hosts:
        return False
    for h in icopay_hosts:
        if h.startswith("api.") or h.startswith("hqth.") or h.startswith("jpjp.") or h.startswith("jp."):
            return False
        if h not in ("icopay.co.kr", "www.icopay.co.kr"):
            # wildcard *.icopay.co.kr should not be treated as apex-only
            if h.startswith("*."):
                return False
    return "icopay.co.kr" in icopay_hosts or "www.icopay.co.kr" in icopay_hosts


def already_patched(block: str) -> bool:
    return MARK in block and "proxy_pass http://127.0.0.1:8080" in block


def insert_snippet(block: str) -> str:
    if already_patched(block):
        return block
    # Prefer insert before prefix location / { that uses try_files
    needle_try = None
    lines = block.splitlines(keepends=True)
    out = []
    inserted = False
    i = 0
    while i < len(lines):
        line = lines[i]
        stripped = line.strip()
        if (not inserted) and stripped.startswith("location /") and stripped.endswith("{") and "location /api" not in stripped:
            # location / {  (prefix)
            if stripped == "location / {" or stripped.startswith("location / {"):
                out.append(SNIPPET)
                if not SNIPPET.endswith("\n"):
                    out.append("\n")
                inserted = True
        out.append(line)
        i += 1
    if not inserted:
        # before last closing brace of server
        text = "".join(out) if out else block
        last = text.rfind("}")
        if last < 0:
            return block
        return text[:last] + SNIPPET + "\n" + text[last:]
    return "".join(out)


def main() -> int:
    changed = []
    for path in iter_conf_files():
        try:
            original = path.read_text(encoding="utf-8")
        except Exception:
            continue
        if "icopay.co.kr" not in original:
            continue
        blocks = server_blocks(original)
        if not blocks:
            continue
        new = original
        offset = 0
        file_changed = False
        for start, end in blocks:
            adj_s = start + offset
            adj_e = end + offset
            block = new[adj_s:adj_e]
            if not is_apex_icopay(block):
                continue
            patched = insert_snippet(block)
            if patched != block:
                new = new[:adj_s] + patched + new[adj_e:]
                offset += len(patched) - len(block)
                file_changed = True
        if file_changed:
            bak = path.with_suffix(path.suffix + ".bak-og")
            if not bak.exists():
                bak.write_text(original, encoding="utf-8")
            path.write_text(new, encoding="utf-8")
            changed.append(str(path))
    if not changed:
        print("NO_CHANGE (already patched or apex server block not found)")
        # still success if already patched somewhere
        for path in iter_conf_files():
            try:
                t = path.read_text(encoding="utf-8")
            except Exception:
                continue
            if MARK in t and "icopay.co.kr" in t:
                print("ALREADY", path)
                return 0
        print("ERROR: could not find icopay.co.kr apex nginx server", file=sys.stderr)
        return 2
    for p in changed:
        print("PATCHED", p)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
