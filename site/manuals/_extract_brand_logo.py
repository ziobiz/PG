# -*- coding: utf-8 -*-
"""Use the real embedded 'on the line' logo PNG from HQ/chatbot manuals."""
from __future__ import annotations

import sys
from pathlib import Path

import fitz

ROOT = Path(__file__).resolve().parent
ASSETS = ROOT / "assets"
PDF_ROOT = ROOT / "pdf"
ASSETS.mkdir(parents=True, exist_ok=True)


def extract_brand_logo() -> bytes:
    # HQ ops: known good PNG 638x198
    hq = PDF_ROOT / "hq-ops" / "ko.pdf"
    if hq.exists():
        doc = fitz.open(hq)
        try:
            for img in doc[0].get_images(full=True):
                d = doc.extract_image(img[0])
                w, h = int(d.get("width") or 0), int(d.get("height") or 0)
                if d.get("ext") == "png" and 300 <= w <= 900 and 80 <= h <= 280:
                    print("hq embedded", w, h, len(d["image"]))
                    return d["image"]
        finally:
            doc.close()

    # Chatbot: 444x138 jpeg logo (img with that size)
    cb = PDF_ROOT / "merchant-chatbot" / "ko.pdf"
    if cb.exists():
        doc = fitz.open(cb)
        try:
            for img in doc[0].get_images(full=True):
                d = doc.extract_image(img[0])
                w, h = int(d.get("width") or 0), int(d.get("height") or 0)
                if 400 <= w <= 500 and 100 <= h <= 160:
                    print("chatbot embedded", w, h, len(d["image"]))
                    return d["image"]
        finally:
            doc.close()

    raise SystemExit("brand logo not found")


def main() -> None:
    blob = extract_brand_logo()
    out = ASSETS / "cover-brand-logo.png"
    # convert jpeg to png via pixmap if needed
    if blob[:8] == b"\x89PNG\r\n\x1a\n":
        out.write_bytes(blob)
    else:
        # wrap via fitz
        pix = fitz.Pixmap(fitz.open("png", blob) if False else None)
        # decode with PIL
        from io import BytesIO
        from PIL import Image
        im = Image.open(BytesIO(blob)).convert("RGBA")
        buf = BytesIO()
        im.save(buf, format="PNG")
        out.write_bytes(buf.getvalue())
    print("wrote", out, out.stat().st_size)
    for d in sorted(PDF_ROOT.iterdir()):
        if d.is_dir():
            (d / "logo.png").write_bytes(out.read_bytes())
            print("sync", d.name)


if __name__ == "__main__":
    main()
