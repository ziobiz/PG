# -*- coding: utf-8 -*-
"""Build merchant USER manuals (URL/split/subscribe) in chatbot-manual design.

Outputs:
  - site/manuals/generated/{id}-{lang}.html
  - docs/icopay-{kind}-user-manual-{lang}.html  (source mirrors)
  - site/manuals/pdf/{id}/{lang}.pdf           (Chrome headless)

Run:  python site/manuals/_build_merchant_user_manuals_v3.py
"""
from __future__ import annotations

import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[1]
sys.path.insert(0, str(ROOT))

from _merchant_user_manual_content import DOC_BUILDERS  # noqa: E402
from _merchant_user_manual_theme import VERSION, render_doc  # noqa: E402

LANGS = ["ko", "en", "ja", "zh", "th"]
GEN = ROOT / "generated"
DOCS = REPO / "docs"
PDF_ROOT = ROOT / "pdf"
LOGO_SRC = DOCS / "logo-ontheline.png"
ASSETS_LOGO = ROOT / "assets" / "logo-ontheline.png"

DOC_NAME = {
    "merchant-url-user": "icopay-url-payment-user-manual",
    "merchant-split-user": "icopay-split-payment-user-manual",
    "merchant-subscribe-user": "icopay-subscribe-payment-user-manual",
}


def find_chrome() -> Path:
    candidates = [
        Path(r"C:\Program Files\Google\Chrome\Application\chrome.exe"),
        Path(r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe"),
        Path(r"C:\Program Files (x86)\Microsoft\Edge\Application\msedge.exe"),
        Path(r"C:\Program Files\Microsoft\Edge\Application\msedge.exe"),
    ]
    for c in candidates:
        if c.exists():
            return c
    raise SystemExit("Chrome/Edge not found for PDF print")


def html_to_pdf(chrome: Path, html_path: Path, pdf_path: Path) -> None:
    pdf_path.parent.mkdir(parents=True, exist_ok=True)
    # file URL
    uri = html_path.resolve().as_uri()
    cmd = [
        str(chrome),
        "--headless=new",
        "--disable-gpu",
        "--no-pdf-header-footer",
        "--no-first-run",
        "--no-default-browser-check",
        f"--print-to-pdf={pdf_path.resolve()}",
        uri,
    ]
    r = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
    if not pdf_path.exists() or pdf_path.stat().st_size < 1000:
        raise RuntimeError(
            f"PDF failed for {html_path.name}: exit={r.returncode}\n"
            f"stdout={r.stdout[-500:]}\nstderr={r.stderr[-500:]}"
        )


def main() -> None:
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass

    if LOGO_SRC.exists():
        ASSETS_LOGO.parent.mkdir(parents=True, exist_ok=True)
        shutil.copy2(LOGO_SRC, ASSETS_LOGO)

    chrome = find_chrome()
    print("Chrome/Edge:", chrome)
    print("VERSION", VERSION)
    GEN.mkdir(parents=True, exist_ok=True)

    built = 0
    for mid, builder in DOC_BUILDERS.items():
        out_pdf_dir = PDF_ROOT / mid
        out_pdf_dir.mkdir(parents=True, exist_ok=True)
        # logo next to PDF for cover API
        if ASSETS_LOGO.exists():
            shutil.copy2(ASSETS_LOGO, out_pdf_dir / "logo.png")

        for lang in LANGS:
            doc = builder(lang)
            # relative logo for HTML next to assets / docs
            # generated HTML uses ../assets/logo-ontheline.png
            logo_rel_gen = "../assets/logo-ontheline.png"
            logo_rel_docs = "./logo-ontheline.png"

            html_gen = render_doc(
                lang=lang,
                page_title=doc["page_title"],
                title_html=doc["title_html"],
                subtitle=doc["subtitle"],
                meta=doc["meta"],
                perm_rows=doc["perm_rows"],
                toc=doc["toc"],
                body_html=doc["body"],
                logo_src=logo_rel_gen,
                footer_extra=doc["footer_extra"],
            )
            gen_path = GEN / f"{mid}-{lang}.html"
            gen_path.write_text(html_gen, encoding="utf-8")

            html_docs = render_doc(
                lang=lang,
                page_title=doc["page_title"],
                title_html=doc["title_html"],
                subtitle=doc["subtitle"],
                meta=doc["meta"],
                perm_rows=doc["perm_rows"],
                toc=doc["toc"],
                body_html=doc["body"],
                logo_src=logo_rel_docs,
                footer_extra=doc["footer_extra"],
            )
            docs_path = DOCS / f"{DOC_NAME[mid]}-{lang}.html"
            docs_path.write_text(html_docs, encoding="utf-8")

            # PDF: use absolute file path for logo so headless print embeds image
            logo_abs = ASSETS_LOGO.resolve().as_uri()
            html_pdf = render_doc(
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
            )
            tmp_html = GEN / f"_pdf_{mid}-{lang}.html"
            tmp_html.write_text(html_pdf, encoding="utf-8")
            pdf_path = out_pdf_dir / f"{lang}.pdf"
            html_to_pdf(chrome, tmp_html, pdf_path)
            tmp_html.unlink(missing_ok=True)
            built += 1
            print(f"OK {mid}/{lang}.pdf  ({pdf_path.stat().st_size // 1024} KB) + HTML")

    print(f"DONE {built} docs  V{VERSION}")


if __name__ == "__main__":
    main()
