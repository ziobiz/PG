# -*- coding: utf-8 -*-
"""Stamp document version on formal ops manuals (super-ops / dist-ops / hq-ops) PDF cover.

Other manuals already print V{version} in HTML→PDF. These imported formal PDFs
had no version text; overlay a top banner so PDF body matches list V badge.

Run:  python site/manuals/_stamp_ops_manual_version.py
"""
from __future__ import annotations

import io
import sys
from pathlib import Path

from pypdf import PdfReader, PdfWriter
from reportlab.lib.colors import Color, HexColor
from reportlab.pdfgen import canvas

ROOT = Path(__file__).resolve().parent
PDF_ROOT = ROOT / "pdf"
VERSION = "2.76"
DATE = "2026-07-24"

# (manual_id, langs) — 총본사·총판 (+ 본사: 동일 형식 PDF)
TARGETS = [
    ("super-ops", ["ko", "en", "ja", "zh", "th"]),
    ("dist-ops", ["ko", "en", "ja", "zh", "th"]),
    ("hq-ops", ["ko", "en", "ja", "zh", "th"]),
]

LABEL = {
    "ko": "문서 버전",
    "en": "Document version",
    "ja": "文書バージョン",
    "zh": "文档版本",
    "th": "เวอร์ชันเอกสาร",
}


def make_banner(page_w: float, page_h: float, lang: str) -> bytes:
    buf = io.BytesIO()
    c = canvas.Canvas(buf, pagesize=(page_w, page_h))
    bar_h = 22
    # top strip
    c.setFillColor(HexColor("#1a3a5c"))
    c.rect(0, page_h - bar_h, page_w, bar_h, fill=1, stroke=0)
    c.setFillColor(Color(1, 1, 1, alpha=1))
    c.setFont("Helvetica-Bold", 9)
    label = LABEL.get(lang, LABEL["en"])
    text = f"{label}: V{VERSION}  ·  {DATE}  ·  ICOPAY Platform Manuals"
    c.drawString(14, page_h - 15, text)
    c.save()
    buf.seek(0)
    return buf.read()


def stamp_one(pdf_path: Path, lang: str) -> None:
    reader = PdfReader(str(pdf_path))
    if not reader.pages:
        raise RuntimeError(f"empty pdf: {pdf_path}")
    page0 = reader.pages[0]
    box = page0.mediabox
    w = float(box.width)
    h = float(box.height)
    banner = PdfReader(io.BytesIO(make_banner(w, h, lang)))
    page0.merge_page(banner.pages[0])
    writer = PdfWriter()
    for p in reader.pages:
        writer.add_page(p)
    # preserve metadata lightly
    if reader.metadata:
        try:
            writer.add_metadata(reader.metadata)
        except Exception:
            pass
    tmp = pdf_path.with_suffix(".pdf.tmp")
    with open(tmp, "wb") as f:
        writer.write(f)
    tmp.replace(pdf_path)


def main() -> None:
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass
    n = 0
    for mid, langs in TARGETS:
        for lang in langs:
            path = PDF_ROOT / mid / f"{lang}.pdf"
            if not path.exists():
                print(f"SKIP missing {path}")
                continue
            stamp_one(path, lang)
            n += 1
            print(f"OK {mid}/{lang}.pdf  V{VERSION}")
    print(f"DONE stamped {n} files  V{VERSION}")


if __name__ == "__main__":
    main()
