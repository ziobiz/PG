# -*- coding: utf-8 -*-
"""Shared CSS / shell for merchant USER manuals — identical to docs/icopay-chatbot-merchant-manual-*.html."""

VERSION = "2.67"
DATE_KO = "2026년 7월"
DATE_EN = "July 2026"
DATE_META = "2026-07-23"

CSS = r"""
  *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
  body { font-family: 'Malgun Gothic', 'Apple SD Gothic Neo', 'Noto Sans KR', 'Segoe UI', sans-serif; font-size: 11pt; line-height: 1.7; color: #1a1a1a; background: #f5f6fa; }
  .page-wrap { max-width: 860px; margin: 32px auto; background: #fff; border-radius: 10px; box-shadow: 0 2px 16px rgba(0,0,0,.10); overflow: hidden; }
  .cover { background: linear-gradient(135deg, #1a3a5c 0%, #1565c0 60%, #1e88e5 100%); color: #fff; padding: 48px 52px 40px; display: flex; align-items: flex-start; justify-content: space-between; gap: 32px; }
  .cover-body { flex: 1; }
  .cover-logo { flex-shrink: 0; display: flex; align-items: flex-start; padding-top: 4px; }
  .cover-logo img { height: 64px; background: rgba(255,255,255,0.92); padding: 10px 18px; border-radius: 8px; box-shadow: 0 2px 10px rgba(0,0,0,.20); }
  .cover .logo-line { font-size: 13pt; font-weight: 700; letter-spacing: 2px; opacity: .85; margin-bottom: 14px; }
  .cover h1 { font-size: 26pt; font-weight: 900; line-height: 1.25; margin-bottom: 10px; }
  .cover .subtitle { font-size: 11.5pt; opacity: .82; margin-bottom: 24px; }
  .cover .meta { font-size: 9.5pt; opacity: .65; border-top: 1px solid rgba(255,255,255,.25); padding-top: 14px; }
  .body { padding: 44px 52px 60px; }
  .toc { background: #f0f4ff; border-left: 4px solid #1565c0; border-radius: 0 8px 8px 0; padding: 20px 26px; margin-bottom: 40px; }
  .toc h2 { font-size: 12pt; font-weight: 700; color: #1565c0; margin-bottom: 12px; }
  .toc ol { padding-left: 20px; }
  .toc li { font-size: 10pt; line-height: 2.1; color: #1a3a5c; }
  .toc a { color: #1565c0; text-decoration: none; }
  .toc a:hover { text-decoration: underline; }
  h2.section-title { font-size: 15pt; font-weight: 800; color: #1a3a5c; border-bottom: 2.5px solid #1565c0; padding-bottom: 7px; margin: 42px 0 16px; page-break-after: avoid; }
  h3.sub-title { font-size: 12pt; font-weight: 700; color: #1565c0; margin: 24px 0 10px; page-break-after: avoid; }
  p { margin-bottom: 10px; }
  .info-box { background: #e3f0ff; border: 1px solid #90caf9; border-left: 4px solid #1565c0; border-radius: 6px; padding: 12px 16px; margin: 14px 0; font-size: 10pt; color: #1a3a5c; }
  .warn-box { background: #fff8e1; border: 1px solid #ffe082; border-left: 4px solid #f9a825; border-radius: 6px; padding: 12px 16px; margin: 14px 0; font-size: 10pt; color: #5d4037; }
  .check-box { background: #e8f5e9; border: 1px solid #a5d6a7; border-left: 4px solid #2e7d32; border-radius: 6px; padding: 12px 16px; margin: 14px 0; font-size: 10pt; color: #1b5e20; }
  .hq-box { background: #fce4ec; border: 1px solid #f48fb1; border-left: 4px solid #c62828; border-radius: 6px; padding: 12px 16px; margin: 14px 0; font-size: 10pt; color: #b71c1c; }
  table { width: 100%; border-collapse: collapse; margin: 14px 0 20px; font-size: 10pt; page-break-inside: avoid; }
  th { background: #1565c0; color: #fff; font-weight: 700; padding: 9px 12px; text-align: left; border: 1px solid #1245a0; }
  td { padding: 8px 12px; border: 1px solid #cfd8dc; vertical-align: top; }
  tr:nth-child(even) td { background: #f4f7ff; }
  td.center, th.center { text-align: center; }
  .perm-full { display: inline-block; background: #2e7d32; color: #fff; font-size: 8pt; font-weight: 700; padding: 1px 7px; border-radius: 4px; }
  .perm-view { display: inline-block; background: #1565c0; color: #fff; font-size: 8pt; font-weight: 700; padding: 1px 7px; border-radius: 4px; }
  .perm-none { display: inline-block; background: #9e9e9e; color: #fff; font-size: 8pt; font-weight: 700; padding: 1px 7px; border-radius: 4px; }
  pre { background: #1e2a38; color: #cdd9e5; font-family: 'Consolas', 'D2Coding', monospace; font-size: 9.5pt; line-height: 1.6; padding: 16px 20px; border-radius: 8px; margin: 12px 0 18px; overflow-x: auto; white-space: pre-wrap; word-break: break-all; page-break-inside: avoid; }
  code { background: #e8edf5; color: #c0392b; font-family: 'Consolas', monospace; font-size: 9pt; padding: 1px 5px; border-radius: 3px; }
  .flow { background: #f8f9fd; border: 1px solid #dce3f5; border-radius: 8px; padding: 20px 24px; margin: 14px 0 20px; font-size: 10pt; page-break-inside: avoid; }
  .flow-row { display: flex; align-items: flex-start; margin-bottom: 7px; }
  .flow-actor { display: inline-block; min-width: 80px; font-weight: 700; color: #1565c0; flex-shrink: 0; padding-top: 1px; }
  .flow-arrow { color: #888; margin: 0 10px; font-size: 13pt; flex-shrink: 0; }
  .flow-desc { color: #37474f; }
  .step-badge { display: inline-block; background: #1565c0; color: #fff; font-weight: 800; font-size: 9pt; padding: 2px 10px; border-radius: 20px; margin-right: 8px; vertical-align: middle; }
  .faq-item { border: 1px solid #e0e6f0; border-radius: 8px; margin-bottom: 14px; page-break-inside: avoid; }
  .faq-q { background: #e8edf8; padding: 11px 16px; font-weight: 700; font-size: 10.5pt; color: #1a3a5c; border-radius: 8px 8px 0 0; }
  .faq-q::before { content: "Q. "; color: #1565c0; }
  .faq-a { padding: 11px 16px; font-size: 10pt; color: #37474f; background: #fff; border-radius: 0 0 8px 8px; }
  .faq-a::before { content: "A. "; font-weight: 700; color: #2e7d32; }
  .badge-req { display: inline-block; background: #e53935; color: #fff; font-size: 8pt; font-weight: 700; padding: 1px 6px; border-radius: 4px; margin-left: 4px; }
  .badge-opt { display: inline-block; background: #757575; color: #fff; font-size: 8pt; font-weight: 700; padding: 1px 6px; border-radius: 4px; margin-left: 4px; }
  .menu-path { display: inline-block; background: #ececec; border: 1px solid #d0d0d0; border-radius: 5px; padding: 3px 11px; font-size: 9.5pt; color: #37474f; font-weight: 600; margin-bottom: 12px; }
  .menu-path::before { content: "📌 "; }
  hr.section-rule { border: none; border-top: 1px dashed #c5cae9; margin: 36px 0; }
  .footer { background: #1a3a5c; color: rgba(255,255,255,.65); text-align: center; font-size: 9pt; padding: 18px 24px; }
  ul { padding-left: 20px; margin-bottom: 12px; }
  li { margin-bottom: 4px; }
  @media print {
    body { background: #fff; font-size: 10pt; }
    .page-wrap { max-width: 100%; margin: 0; border-radius: 0; box-shadow: none; }
    .cover { padding: 28px 36px 24px; }
    .body { padding: 28px 36px 36px; }
    .faq-item, pre, table, .flow { page-break-inside: avoid; }
    .footer, .no-print, .print-btn { display: none !important; }
    @page { size: A4; margin: 14mm 12mm; }
  }
"""

UI = {
    "ko": {
        "print": "🖨️ 인쇄 / PDF 저장",
        "toc": "목 차",
        "permTitle": "📋 가맹점 메뉴 권한 요약",
        "colMenu": "메뉴",
        "colPerm": "권한",
        "colNote": "설명",
        "perm": {"view": "조회만", "full": "전체", "none": "접근불가"},
        "menuPrefix": "메뉴: ",
        "footer": "ICOPAY &nbsp;|&nbsp; icopay.co.kr &nbsp;|&nbsp; 문의: ICOPAY 본사·총판 담당자",
        "logoLine": "ICOPAY · Payment Gateway",
    },
    "en": {
        "print": "🖨️ Print / Save as PDF",
        "toc": "Table of Contents",
        "permTitle": "📋 Merchant Menu Permission Summary",
        "colMenu": "Menu",
        "colPerm": "Permission",
        "colNote": "Notes",
        "perm": {"view": "View Only", "full": "Full Access", "none": "No Access"},
        "menuPrefix": "Menu: ",
        "footer": "ICOPAY &nbsp;|&nbsp; icopay.co.kr &nbsp;|&nbsp; Contact: ICOPAY HQ / Distributor",
        "logoLine": "ICOPAY · Payment Gateway",
    },
    "ja": {
        "print": "🖨️ 印刷 / PDF保存",
        "toc": "目 次",
        "permTitle": "📋 加盟店メニュー権限サマリー",
        "colMenu": "メニュー",
        "colPerm": "権限",
        "colNote": "説明",
        "perm": {"view": "参照のみ", "full": "全権限", "none": "アクセス不可"},
        "menuPrefix": "メニュー: ",
        "footer": "ICOPAY &nbsp;|&nbsp; icopay.co.kr &nbsp;|&nbsp; 問合せ: ICOPAY本社・総代理",
        "logoLine": "ICOPAY · Payment Gateway",
    },
    "zh": {
        "print": "🖨️ 打印 / 保存 PDF",
        "toc": "目 录",
        "permTitle": "📋 商户菜单权限摘要",
        "colMenu": "菜单",
        "colPerm": "权限",
        "colNote": "说明",
        "perm": {"view": "仅查看", "full": "全部", "none": "不可访问"},
        "menuPrefix": "菜单: ",
        "footer": "ICOPAY &nbsp;|&nbsp; icopay.co.kr &nbsp;|&nbsp; 咨询: ICOPAY 总部/总代理",
        "logoLine": "ICOPAY · Payment Gateway",
    },
    "th": {
        "print": "🖨️ พิมพ์ / บันทึก PDF",
        "toc": "สารบัญ",
        "permTitle": "📋 สรุปสิทธิ์เมนูร้านค้า",
        "colMenu": "เมนู",
        "colPerm": "สิทธิ์",
        "colNote": "หมายเหตุ",
        "perm": {"view": "ดูอย่างเดียว", "full": "เต็มสิทธิ์", "none": "เข้าไม่ได้"},
        "menuPrefix": "เมนู: ",
        "footer": "ICOPAY &nbsp;|&nbsp; icopay.co.kr &nbsp;|&nbsp; ติดต่อ: ICOPAY HQ / ตัวแทน",
        "logoLine": "ICOPAY · Payment Gateway",
    },
}


def render_doc(
    *,
    lang: str,
    page_title: str,
    title_html: str,
    subtitle: str,
    meta: str,
    perm_rows: list[tuple[str, str, str]],
    toc: list[tuple[str, str]],
    body_html: str,
    logo_src: str,
    footer_extra: str,
) -> str:
    ui = UI[lang]
    perm_html = []
    for menu, perm_key, note in perm_rows:
        label = ui["perm"][perm_key]
        cls = {"view": "perm-view", "full": "perm-full", "none": "perm-none"}[perm_key]
        perm_html.append(
            f"<tr><td>{menu}</td><td class=\"center\"><span class=\"{cls}\">{label}</span></td><td>{note}</td></tr>"
        )
    toc_html = "".join(f'<li><a href="#{sid}">{label}</a></li>' for sid, label in toc)
    # menu-path prefix localization via CSS content is KO-centric in chatbot; inject text in HTML instead
    prefix = ui["menuPrefix"]
    body = body_html.replace('class="menu-path">', 'class="menu-path">' + prefix)
    return f"""<!DOCTYPE html>
<html lang="{lang}">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>{page_title}</title>
<style>{CSS}
  .menu-path::before {{ content: none; }}
</style>
</head>
<body>
<style id="print-btn-style">
  .print-btn {{ position: fixed; bottom: 28px; right: 28px; z-index: 9999; background: #1565c0; color: #fff; border: none; border-radius: 10px; padding: 11px 20px; font-size: 10.5pt; font-weight: 700; cursor: pointer; box-shadow: 0 4px 16px rgba(21,101,192,.45); }}
  @media print {{ .print-btn {{ display: none !important; }} }}
</style>
<button class="print-btn" onclick="window.print()">{ui["print"]}</button>
<div class="page-wrap">
  <div class="cover">
    <div class="cover-body">
      <div class="logo-line">{ui["logoLine"]}</div>
      <h1>{title_html}</h1>
      <div class="subtitle">{subtitle}</div>
      <div class="meta">{meta}</div>
    </div>
    <div class="cover-logo">
      <img src="{logo_src}" alt="Brand">
    </div>
  </div>
  <div class="body">
    <div class="warn-box" style="margin-bottom:28px;">
      <strong>{ui["permTitle"]}</strong><br><br>
      <table style="margin:8px 0 0;">
        <tr><th>{ui["colMenu"]}</th><th class="center">{ui["colPerm"]}</th><th>{ui["colNote"]}</th></tr>
        {"".join(perm_html)}
      </table>
    </div>
    <div class="toc">
      <h2>{ui["toc"]}</h2>
      <ol>{toc_html}</ol>
    </div>
    {body}
  </div>
  <div class="footer">{footer_extra}<br>{ui["footer"]}</div>
</div>
</body>
</html>
"""
