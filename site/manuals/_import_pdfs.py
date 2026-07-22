# -*- coding: utf-8 -*-
"""Import formal ICOPAY manuals PDFs into site/manuals/pdf/{id}/{lang}.pdf"""
from __future__ import annotations

import re
import shutil
import sys
from pathlib import Path

try:
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
except Exception:
    pass

ROOT = Path(__file__).resolve().parents[2]  # repo root
OUT = Path(__file__).resolve().parent / "pdf"

LANG_MAP = {
    "KR": "ko",
    "EN": "en",
    "JP": "ja",
    "CH": "zh",
    "TH": "th",
}


def find_docs_root() -> Path:
    docs = Path(r"C:\Users\ziobi\Documents")
    for d in docs.iterdir():
        if d.is_dir() and d.name.startswith("ICOPAY"):
            return d
    raise SystemExit("ICOPAY manuals folder not found")


def lang_from_name(name: str) -> str | None:
    m = re.search(r"_(KR|EN|JP|CH|TH)_", name, re.I)
    if m:
        return LANG_MAP[m.group(1).upper()]
    # Japanese-only risk sheet
    if "リスクトリガー" in name or "리스크" in name:
        return "ja"
    return None


def classify(rel: str, name: str) -> str | None:
    # folder-based (unicode may vary) + filename keywords
    low = name.lower()
    if "mhq" in low or "총본사" in name:
        return "super-ops"
    if "headquarters" in low:
        return "hq-ops"
    if "distributor" in low:
        return "dist-ops"
    if "chatbot" in low or "챗봇" in name:
        return "merchant-chatbot"
    if "리스クトリガー" in name or "리스크" in name and "トリガー" in name:
        return "hqdist-risk-intro"
    # path folders
    if "총본사" in rel:
        return "super-ops"
    if "본사" in rel and "총본사" not in rel:
        return "hq-ops"
    if "총판" in rel:
        if "리스ク" in name or "トリガー" in name or "리스크" in name:
            return "hqdist-risk-intro"
        if "distributor" in low:
            return "dist-ops"
        return "dist-ops"
    if "기타" in rel or "가맹" in rel:
        if "chatbot" in low or "챗봇" in name:
            return "merchant-chatbot"
    return None


def main() -> None:
    src_root = find_docs_root()
    print("SRC", src_root)
    if OUT.exists():
        shutil.rmtree(OUT)
    OUT.mkdir(parents=True)

    # Also prefer Downloads MHQ V2 if present (user attached)
    downloads = Path(r"C:\Users\ziobi\Downloads")
    extra = []
    for p in downloads.iterdir():
        if p.is_file() and "총본사" in p.name and p.suffix.lower() == ".pdf":
            extra.append(p)

    copied = []
    for p in list(src_root.rglob("*.pdf")) + extra:
        if not p.is_file():
            continue
        try:
            rel = str(p.relative_to(src_root)) if src_root in p.parents or p.parent == src_root else f"Downloads/{p.name}"
        except ValueError:
            rel = f"Downloads/{p.name}"
        mid = classify(rel, p.name)
        lang = lang_from_name(p.name)
        if mid is None:
            print("SKIP classify", rel)
            continue
        if lang is None:
            # Downloads Korean HQ manual without lang tag → ko
            if mid == "super-ops" and "Downloads" in rel:
                lang = "ko"
            else:
                print("SKIP lang", rel)
                continue
        dest_dir = OUT / mid
        dest_dir.mkdir(parents=True, exist_ok=True)
        dest = dest_dir / f"{lang}.pdf"
        # Prefer larger file if duplicate (V3 docs over small V2 stub)
        if dest.exists() and dest.stat().st_size >= p.stat().st_size:
            print("KEEP larger", dest, ">", p.name)
            continue
        shutil.copy2(p, dest)
        copied.append((mid, lang, dest.stat().st_size, p.name))
        print("COPY", mid, lang, dest.stat().st_size, "<=", p.name)

    print("--- done", len(copied), "files ---")
    for mid in sorted({c[0] for c in copied}):
        langs = sorted(x[1] for x in copied if x[0] == mid)
        print(mid, langs)


if __name__ == "__main__":
    main()
