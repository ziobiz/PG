# -*- coding: utf-8 -*-
"""Build 가맹점 운영 메뉴얼 (merchant-ops) — chatbot design, live VERSION sync.

Run:  python site/manuals/_build_merchant_ops_v3.py
"""
from __future__ import annotations

import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[1]
sys.path.insert(0, str(ROOT))

from _merchant_ops_content import ops_doc  # noqa: E402
from _merchant_user_manual_theme import VERSION, render_doc  # noqa: E402

LANGS = ["ko", "en", "ja", "zh", "th"]
GEN = ROOT / "generated"
DOCS = REPO / "docs"
PDF_DIR = ROOT / "pdf" / "merchant-ops"
ASSETS_LOGO = ROOT / "assets" / "logo-ontheline.png"
LOGO_SRC = DOCS / "logo-ontheline.png"


def find_chrome() -> Path:
    for c in [
        Path(r"C:\Program Files\Google\Chrome\Application\chrome.exe"),
        Path(r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe"),
        Path(r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"),
        Path(r"C:\Program Files\Microsoft\Edge\Application\msedge.exe"),
    ]:
        if c.exists():
            return c
    raise SystemExit("Chrome/Edge not found")


def html_to_pdf(chrome: Path, html_path: Path, pdf_path: Path) -> None:
    pdf_path.parent.mkdir(parents=True, exist_ok=True)
    cmd = [
        str(chrome),
        "--headless=new",
        "--disable-gpu",
        "--no-pdf-header-footer",
        "--no-first-run",
        f"--print-to-pdf={pdf_path.resolve()}",
        html_path.resolve().as_uri(),
    ]
    r = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
    if not pdf_path.exists() or pdf_path.stat().st_size < 1000:
        raise RuntimeError(f"PDF failed {html_path.name}: {r.stderr[-400:]}")


def main() -> None:
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass
    if LOGO_SRC.exists():
        ASSETS_LOGO.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(LOGO_SRC, ASSETS_LOGO)

    chrome = find_chrome()
    print("VERSION", VERSION, "chrome", chrome)
    GEN.mkdir(parents=True, exist_ok=True)
    PDF_DIR.mkdir(parents=True, exist_ok=True)
    if ASSETS_LOGO.exists():
        shutil.copy2(ASSETS_LOGO, PDF_DIR / "logo.png")

    logo_abs = ASSETS_LOGO.resolve().as_uri()
    for lang in LANGS:
        doc = ops_doc(lang)
        html = render_doc(
            lang=lang,
            page_title=doc["page_title"],
            title_html=doc["title_html"],
            subtitle=doc["subtitle"],
            meta=doc["meta"],
            perm_rows=doc["perm_rows"],
            toc=doc["toc"],
            body_html=doc["body"],
            logo_src="../assets/logo-ontheline.png",
            footer_extra=doc["footer_extra"],
        )
        (GEN / f"merchant-ops-{lang}.html").write_text(html, encoding="utf-8")
        (DOCS / f"icopay-merchant-ops-manual-{lang}.html").write_text(
            render_doc(
                lang=lang,
                page_title=doc["page_title"],
                title_html=doc["title_html"],
                subtitle=doc["subtitle"],
                meta=doc["meta"],
                perm_rows=doc["perm_rows"],
                toc=doc["toc"],
                body_html=doc["body"],
                logo_src="./logo-ontheline.png",
                footer_extra=doc["footer_extra"],
            ),
            encoding="utf-8",
        )
        tmp = GEN / f"_pdf_merchant-ops-{lang}.html"
        tmp.write_text(
            render_doc(
                lang=lang,
                page_title=doc["page_title"],
                title_html=doc["title_html"],
                subtitle=doc["subtitle"],
                meta=doc["meta"],
                perm_rows=doc["perm_rows"],
                toc=doc["toc"],
                body_html=doc["body"],
                logo_src=logo_abs,
                footer_extra=doc["footer_extra"],
            ),
            encoding="utf-8",
        )
        pdf_path = PDF_DIR / f"{lang}.pdf"
        html_to_pdf(chrome, tmp, pdf_path)
        tmp.unlink(missing_ok=True)
        print(f"OK merchant-ops/{lang}.pdf ({pdf_path.stat().st_size // 1024} KB)")

    # catalog version sync
    cat = GEN / "catalog.json"
    if cat.exists():
        text = cat.read_text(encoding="utf-8")
        import re

        text2 = re.sub(r'"version"\s*:\s*"[^"]+"', f'"version": "{VERSION}"', text, count=1)
        cat.write_text(text2, encoding="utf-8")
        print("catalog.json version →", VERSION)
    print("DONE merchant-ops V" + VERSION)


if __name__ == "__main__":
    main()
