# -*- coding: utf-8 -*-
"""Build 서버 웹훅(HMAC) API 연동 메뉴얼 PDF (merchant-webhook-hmac).

Run:  python site/manuals/_build_merchant_webhook_hmac_v3.py
"""
from __future__ import annotations

import json
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent
REPO = ROOT.parents[1]
sys.path.insert(0, str(ROOT))

from _merchant_user_manual_theme import render_doc  # noqa: E402
from _merchant_webhook_hmac_content import VERSION, webhook_doc  # noqa: E402

LANGS = ["ko", "en", "ja", "zh", "th"]
MID = "merchant-webhook-hmac"
GEN = ROOT / "generated"
DOCS = REPO / "docs"
PDF_DIR = ROOT / "pdf" / MID
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
        doc = webhook_doc(lang)
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
        (GEN / f"{MID}-{lang}.html").write_text(html, encoding="utf-8")
        (DOCS / f"icopay-merchant-webhook-hmac-manual-{lang}.html").write_text(
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
        tmp = GEN / f"_pdf_{MID}-{lang}.html"
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
        print(f"OK {MID}/{lang}.pdf ({pdf_path.stat().st_size // 1024} KB)")

    cat = GEN / "catalog.json"
    if cat.exists():
        data = json.loads(cat.read_text(encoding="utf-8"))
        titles = {
            "ko": "서버 웹훅(HMAC) API 연동 메뉴얼",
            "en": "Server Webhook (HMAC) API Manual",
            "ja": "サーバーWebhook(HMAC) API連携マニュアル",
            "zh": "服务器 Webhook(HMAC) API 对接手册",
            "th": "คู่มือเชื่อมต่อ Server Webhook (HMAC) API",
        }
        items = data.get("items") or []
        found = False
        for it in items:
            if it.get("id") == MID:
                it["docVersion"] = VERSION
                it["titles"] = titles
                it["pdfDir"] = f"manuals/pdf/{MID}"
                it["audience"] = "merchant"
                it["format"] = "pdf"
                found = True
                break
        if not found:
            # insert after merchant-ops
            insert_at = 0
            for i, it in enumerate(items):
                if it.get("id") == "merchant-ops":
                    insert_at = i + 1
                    break
            items.insert(
                insert_at,
                {
                    "id": MID,
                    "audience": "merchant",
                    "format": "pdf",
                    "titles": titles,
                    "pdfDir": f"manuals/pdf/{MID}",
                    "docVersion": VERSION,
                },
            )
            data["items"] = items
        cat.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
        print("catalog.json updated:", MID, VERSION)
    print("DONE", MID, "V" + VERSION)


if __name__ == "__main__":
    main()
