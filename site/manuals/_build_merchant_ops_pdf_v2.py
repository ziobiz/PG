# -*- coding: utf-8 -*-
"""Build merchant-ops PDFs with proper CJK fonts (TextWriter)."""
from __future__ import annotations

import sys
from pathlib import Path

import fitz

ROOT = Path(__file__).resolve().parent
OUT = ROOT / "pdf" / "merchant-ops"
LOGO = ROOT / "assets" / "cover-brand-logo.png"
OUT.mkdir(parents=True, exist_ok=True)

VERSION = "2.67"  # deprecated — use _build_merchant_ops_v3.py
DATE = "2026-07-22"

LANG_FONT = {
    "ko": Path(r"C:\Windows\Fonts\malgun.ttf"),
    "en": Path(r"C:\Windows\Fonts\malgun.ttf"),
    "ja": Path(r"C:\Windows\Fonts\msgothic.ttc"),
    "zh": Path(r"C:\Windows\Fonts\msyh.ttc"),
    "th": Path(r"C:\Windows\Fonts\LeelawUI.ttf"),
}
# fallbacks
for k, alts in {
    "ja": [Path(r"C:\Windows\Fonts\YuGothM.ttc"), Path(r"C:\Windows\Fonts\msgothic.ttc")],
    "zh": [Path(r"C:\Windows\Fonts\msyh.ttc"), Path(r"C:\Windows\Fonts\simsun.ttc")],
    "th": [Path(r"C:\Windows\Fonts\LeelawUI.ttf"), Path(r"C:\Windows\Fonts\tahomabd.ttf"), Path(r"C:\Windows\Fonts\tahoma.ttf")],
}.items():
    if not LANG_FONT[k].exists():
        for a in alts:
            if a.exists():
                LANG_FONT[k] = a
                break


def load_docs():
    # Import DOCS from previous module file by exec of DOCS only — redefine here compactly
    from importlib.machinery import SourceFileLoader
    mod = SourceFileLoader("mop_src", str(ROOT / "_build_merchant_ops_pdf.py")).load_module()
    return mod.DOCS


def wrap_lines(font: fitz.Font, text: str, fontsize: float, max_w: float) -> list[str]:
    lines: list[str] = []
    for para in text.split("\n"):
        if not para:
            lines.append("")
            continue
        buf = ""
        for ch in para:
            trial = buf + ch
            if font.text_length(trial, fontsize=fontsize) <= max_w:
                buf = trial
            else:
                if buf:
                    lines.append(buf)
                buf = ch
        if buf:
            lines.append(buf)
    return lines


def build_one(lang: str, meta: dict) -> Path:
    font_path = LANG_FONT.get(lang) or LANG_FONT["ko"]
    if not font_path.exists():
        font_path = Path(r"C:\Windows\Fonts\malgun.ttf")
    font = fitz.Font(fontfile=str(font_path))
    path = OUT / f"{lang}.pdf"
    doc = fitz.open()
    page_w, page_h = fitz.paper_size("a4")
    margin = 46
    max_w = page_w - 2 * margin
    line_h = 14

    def new_page():
        p = doc.new_page(width=page_w, height=page_h)
        return p

    page = new_page()
    # cover bar
    page.draw_rect(fitz.Rect(0, 0, page_w, 148), color=(0.10, 0.23, 0.36), fill=(0.10, 0.23, 0.36))
    tw = fitz.TextWriter(page.rect)
    tw.append((margin, 36), "ICOPAY · Payment Gateway", font=font, fontsize=10)
    tw.append((margin, 62), meta["title"], font=font, fontsize=17)
    y = 78
    for ln in wrap_lines(font, meta["sub"], 9, max_w - 200):
        tw.append((margin, y), ln, font=font, fontsize=9)
        y += 12
    tw.append((margin, 138), f"V{VERSION} · {DATE}", font=font, fontsize=8)
    tw.write_text(page, color=(1, 1, 1))
    if LOGO.exists():
        plate = fitz.Rect(page_w - 220, 28, page_w - 28, 120)
        page.draw_rect(plate, color=(1, 1, 1), fill=(1, 1, 1))
        page.insert_image(fitz.Rect(page_w - 210, 36, page_w - 38, 112), filename=str(LOGO), keep_proportion=True)

    y = 168
    blocks = [(meta["toc"], True)]
    blocks += [(f"{i}. {h}", True) for i, (h, _) in enumerate(meta["sections"], 1)]
    blocks.append(("", False))
    for i, (h, body) in enumerate(meta["sections"], 1):
        blocks.append((f"{i}. {h}", True))
        blocks.append((body, False))

    for text, is_head in blocks:
        fontsize = 12 if is_head else 10
        color = (0.10, 0.23, 0.36) if is_head else (0.12, 0.12, 0.12)
        lines = wrap_lines(font, text, fontsize, max_w) if text else [""]
        for ln in lines:
            if y > page_h - margin - 20:
                page = new_page()
                y = margin
            tw = fitz.TextWriter(page.rect)
            tw.append((margin, y), ln, font=font, fontsize=fontsize)
            tw.write_text(page, color=color)
            y += line_h + (2 if is_head else 0)
        y += 6 if is_head else 8

    doc.set_metadata({"title": meta["title"], "author": "ICOPAY", "subject": f"V{VERSION}"})
    doc.save(path, deflate=True, garbage=3)
    doc.close()
    print("font", lang, font_path.name, "->", path.name, path.stat().st_size)
    return path


def main() -> None:
    docs = load_docs()
    if not LOGO.exists():
        print("missing logo", file=sys.stderr)
        sys.exit(1)
    for lang, meta in docs.items():
        build_one(lang, meta)


if __name__ == "__main__":
    main()
