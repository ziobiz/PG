# -*- coding: utf-8 -*-
"""Build merchant-ops PDFs (KO/EN/JP/CH/TH) — all manuals stay PDF-unified."""
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

FONT_FILES = [
    Path(r"C:\Windows\Fonts\malgun.ttf"),
    Path(r"C:\Windows\Fonts\msgothic.ttc"),
    Path(r"C:\Windows\Fonts\msyh.ttc"),
]


def fontfile() -> str:
    for p in FONT_FILES:
        if p.exists():
            return str(p)
    raise SystemExit("CJK font not found")


# Compact docs (same scope as former HTML merchant-ops)
DOCS = {
    "ko": {
        "title": "가맹점 운영 메뉴얼",
        "sub": "가맹점 권한 화면 기준 — 결제·수수료·결제 URL·일상 운영",
        "toc": "목차",
        "sections": [
            ("이 매뉴얼의 범위",
             "가맹점(MERCHANT) 계정으로 접근 가능한 관리자 화면 기준의 기본 운영 안내입니다.\n"
             "메뉴는 본사정책 → 접근·권한에서 부여한 권한에 따라 달라집니다. 접근불가 메뉴는 사이드바에 보이지 않습니다.\n"
             "결제대행사(PG) 이름은 가맹·구매자 UI에서 ICOPAY로 중립 표시됩니다."),
            ("로그인과 화면",
             "1) 가맹점 ID로 로그인  2) 허용된 메뉴만 좌측에 표시  3) 언어(KO/EN/JP/CH/TH) 전환  4) 대시보드가 있으면 최근 결제 요약 확인"),
            ("공지사항",
             "메뉴 경로: 업체관리 → 공지사항\n본사·총판 운영 공지(수수료·정산일·점검)를 로그인 후 먼저 확인합니다."),
            ("업체정보조회 — 결제 URL",
             "메뉴 경로: 업체관리 → 업체정보조회\n"
             "결제 URL: 공개 일회 결제 주소(예: https://서비스도메인/checkout/업체코드). [복사]로 전달합니다.\n"
             "URL 재결제 URL / 챗봇결제 URL / 분할결제 URL: 해당 기능이 켜진 경우에만 표시됩니다.\n"
             "절차: 업체정보조회 → 결제 URL 확인 → [복사] → 브라우저에서 테스트 오픈.\n"
             "주의: 웹결제 미사용·중지면 URL이 동작하지 않을 수 있습니다."),
            ("결제내역",
             "메뉴 경로: 결제관리 → 결제내역\n기간·상태·키워드로 검색합니다. 승인·실패·취소·환불을 확인합니다.\n"
             "권한이 있으면 URL/챗봇/구독/분할 결제내역 및 상태별 목록도 사용합니다."),
            ("수수료·정산",
             "메뉴 경로: 정산관리 → 수수료내역 / 일별수수료 / 가맹점정산내역\n"
             "수수료율·정산주기는 상위 정책과 가맹 설정을 따릅니다. 문의 시 정산일·금액을 함께 전달합니다."),
            ("가맹점 API·챗봇",
             "업체관리 → 가맹점API: 연동 키·문서(권한 시). ICOPAY 통합 checkout을 사용합니다.\n"
             "챗봇관리: 상품·주문 관리 후 챗봇결제 URL을 공유합니다(사용 가맹)."),
            ("운영 메뉴얼 메뉴",
             "메뉴 경로: 운영관리 → 운영 메뉴얼\n언어를 선택한 뒤 항목을 클릭하면 새 창에서 PDF 매뉴얼이 열립니다."),
            ("일상 체크리스트",
             "1 공지 확인  2 결제 URL 확인  3 당일/전일 승인·실패 점검  4 실패 급증 시 주문번호와 함께 상위 공유  5 수수료·정산 이상 여부  6 챗봇 주문·상품(해당 시)"),
            ("문의",
             f"메뉴가 없으면 상위 본사·총판에 URL 권한 개방을 요청하십시오. 문서 버전 V{VERSION}."),
        ],
    },
    "en": {
        "title": "Merchant Operations Manual",
        "sub": "Permission screens — payments, fees, payment URL, daily ops",
        "toc": "Contents",
        "sections": [
            ("Scope",
             "Day-to-day guide for screens a Merchant login can open.\n"
             "Menus follow HQ Policy → Access permissions. Hidden menus need HQ/distributor access grants.\n"
             "PG names stay neutralized as ICOPAY on merchant/buyer UI."),
            ("Sign-in",
             "1) Sign in  2) Sidebar shows permitted menus only  3) Switch language KO/EN/JP/CH/TH  4) Check dashboard summary if shown"),
            ("Notices",
             "Path: Companies → Notices\nRead HQ/distributor announcements after login."),
            ("Company info — Payment URL",
             "Path: Companies → Company info\n"
             "Payment URL: public checkout (e.g. https://domain/checkout/{compCode}). Use Copy.\n"
             "Re-pay / Chatbot / Split-pay URLs appear when enabled.\n"
             "Steps: open Company info → Copy Payment URL → verify in browser.\n"
             "Note: Off/Suspended web-pay may block the URL."),
            ("Payment list",
             "Path: Payments → Payment list\nSearch by period/status/keyword. Review approvals, failures, cancels, refunds.\n"
             "Channel lists (if permitted): URL, Chatbot, Subscription/Split."),
            ("Fees & settlement",
             "Path: Settlement → Fee list / Daily fees / Merchant settlement\n"
             "Rates follow HQ/distributor policy. Include settlement date and amount when asking support."),
            ("Merchant API & Chatbot",
             "Companies → Merchant API: keys/docs (if permitted). Use ICOPAY unified checkout.\n"
             "Chatbot admin: manage products/orders and share Chatbot payment URL."),
            ("Ops manuals menu",
             "Path: Operations → Ops manuals\nPick a language and open a PDF manual in a new window."),
            ("Daily checklist",
             "1 Notices  2 Payment URL  3 Today/yesterday approvals & failures  4 Escalate spikes with order ids  5 Fee/settlement check  6 Chatbot orders if used"),
            ("Support",
             f"Ask HQ/distributor to grant missing page URLs. Document version V{VERSION}."),
        ],
    },
    "ja": {
        "title": "加盟店 運営マニュアル",
        "sub": "加盟店権限画面 — 決済・手数料・決済URL・日常運用",
        "toc": "目次",
        "sections": [
            ("対象範囲",
             "加盟店(MERCHANT)ログインで開ける画面の基本運用案内です。\n"
             "メニューは本社政策→権限に従います。アクセス不可は非表示です。\n"
             "加盟・購入者UIの決済代行社名はICOPAYとして中立表示されます。"),
            ("ログイン",
             "1) ログイン  2) 許可メニューのみ表示  3) 言語切替  4) ダッシュボード確認"),
            ("お知らせ",
             "経路: 業者管理 → お知らせ\nログイン後に本社・総代理の告知を確認します。"),
            ("業者情報 — 決済URL",
             "経路: 業者管理 → 業者情報照会\n"
             "決済URLを[コピー]して共有します。再決済/ボット/分割URLは機能ON時のみ。\n"
             "Web決済が停止中だと決済できない場合があります。"),
            ("決済一覧",
             "経路: 決済管理 → 決済一覧\n期間・状態で検索し承認/失敗/取消/返金を確認します。"),
            ("手数料・精算",
             "経路: 精算管理 → 手数料一覧 / 日別手数料 / 加盟店精算一覧\n料率は上位政策に従います。"),
            ("加盟店API・ボット",
             "業者管理 → 加盟店API、チャットボット管理で商品・注文と決済URLを扱います。"),
            ("運営マニュアル",
             "経路: 運用管理 → 運営マニュアル\n言語を選びPDFを新窓で開きます。"),
            ("チェックリスト",
             "1 告知  2 決済URL  3 承認/失敗  4 異常時は注文番号共有  5 手数料・精算  6 ボット(該当時)"),
            ("問い合わせ",
             f"メニューが無い場合は上位へ権限開放を依頼。文書版 V{VERSION}。"),
        ],
    },
    "zh": {
        "title": "商户运营手册",
        "sub": "按商户权限画面 — 支付、手续费、支付 URL、日常运营",
        "toc": "目录",
        "sections": [
            ("适用范围",
             "面向商户(MERCHANT)可访问管理画面的日常操作说明。\n"
             "菜单取决于总部策略→权限。不可访问的菜单会隐藏。\n"
             "商户/买家界面收单机构名以 ICOPAY 中立显示。"),
            ("登录",
             "1) 登录  2) 仅显示已授权菜单  3) 切换语言  4) 查看仪表盘摘要"),
            ("公告",
             "路径: 企业管理 → 公告\n登录后先阅读总部/总代理公告。"),
            ("企业信息 — 支付 URL",
             "路径: 企业管理 → 企业信息查询\n"
             "复制支付 URL（例: https://域名/checkout/商户代码）。再支付/机器人/分期 URL 在功能开启时显示。\n"
             "网页支付关闭或暂停时可能无法支付。"),
            ("支付列表",
             "路径: 支付管理 → 支付列表\n按期间/状态搜索，核对准成功/失败/取消/退款。"),
            ("手续费与结算",
             "路径: 结算管理 → 手续费明细 / 按日手续费 / 商户结算明细\n费率遵循上级政策。"),
            ("商户 API 与机器人",
             "企业管理 → 商户 API；聊天机器人管理商品/订单并分享支付 URL。"),
            ("运营手册",
             "路径: 运营管理 → 运营手册\n选择语言后在新窗口打开 PDF。"),
            ("日常清单",
             "1 公告  2 支付 URL  3 当日成败  4 异常附订单号  5 手续费/结算  6 机器人(如有)"),
            ("支持",
             f"缺少菜单时请上级开放权限。文档版本 V{VERSION}。"),
        ],
    },
    "th": {
        "title": "คู่มือปฏิบัติการร้านค้า",
        "sub": "ตามหน้าจอสิทธิ์ร้านค้า — ชำระ ค่าธรรมเนียม URL งานประจำวัน",
        "toc": "สารบัญ",
        "sections": [
            ("ขอบเขต",
             "คู่มือการใช้งานหน้าจอที่บัญชีร้านค้า (MERCHANT) เข้าถึงได้\n"
             "เมนูขึ้นกับนโยบาย HQ → สิทธิ์ เมนูที่ห้ามเข้าจะไม่แสดง\n"
             "ชื่อผู้ให้บริการชำระใน UI ร้าน/ผู้ซื้อเป็น ICOPAY แบบกลาง"),
            ("เข้าสู่ระบบ",
             "1) ล็อกอิน  2) แสดงเฉพาะเมนูที่อนุญาต  3) เปลี่ยนภาษา  4) ดูสรุปแดชบอร์ด"),
            ("ประกาศ",
             "เส้นทาง: จัดการบริษัท → ประกาศ\nอ่านประกาศหลังล็อกอิน"),
            ("ข้อมูลบริษัท — URL ชำระ",
             "เส้นทาง: จัดการบริษัท → ดูข้อมูลบริษัท\n"
             "คัดลอก URL ชำระ (เช่น https://โดเมน/checkout/รหัสร้าน) URL ชำระซ้ำ/แชทบอท/แบ่งจ่ายแสดงเมื่อเปิดใช้\n"
             "หากชำระเว็บปิด/ระงับ อาจชำระไม่ได้"),
            ("รายการชำระ",
             "เส้นทาง: การชำระเงิน → รายการชำระเงิน\nค้นตามช่วงวัน/สถานะ ตรวจสำเร็จ ล้มเหลว ยกเลิก คืนเงิน"),
            ("ค่าธรรมเนียมและการชำระผล",
             "เส้นทาง: การชำระผล → รายการค่าธรรมเนียม / รายวัน / ชำระผลร้านค้า\nอัตราตามนโยบายต้นสังกัด"),
            ("API และแชทบอท",
             "จัดการบริษัท → API ร้านค้า และเมนูแชทบอทสำหรับสินค้า/คำสั่งและ URL ชำระ"),
            ("คู่มือปฏิบัติการ",
             "เส้นทาง: การปฏิบัติการ → คู่มือปฏิบัติการ\nเลือกภาษาแล้วเปิด PDF ในหน้าต่างใหม่"),
            ("เช็คลิสต์",
             "1 ประกาศ  2 URL ชำระ  3 สำเร็จ/ล้มเหลว  4 ส่งเลขคำสั่งเมื่อผิดปกติ  5 ค่าธรรมเนียม/ชำระผล  6 แชทบอท(ถ้ามี)"),
            ("ติดต่อ",
             f"ถ้าไม่มีเมนู ให้ขอเปิดสิทธิ์จากต้นสังกัด เวอร์ชันเอกสาร V{VERSION}"),
        ],
    },
}


def build_one(lang: str, meta: dict, font_path: str) -> Path:
    path = OUT / f"{lang}.pdf"
    doc = fitz.open()
    page_w, page_h = fitz.paper_size("a4")
    margin = 46
    font = font_path

    def add_page():
        return doc.new_page(width=page_w, height=page_h)

    page = add_page()
    # cover
    page.draw_rect(fitz.Rect(0, 0, page_w, 148), color=(0.10, 0.23, 0.36), fill=(0.10, 0.23, 0.36))
    page.insert_textbox(
        fitz.Rect(margin, 22, page_w - 220, 42),
        "ICOPAY · Payment Gateway",
        fontsize=10, fontfile=font, color=(1, 1, 1),
    )
    page.insert_textbox(
        fitz.Rect(margin, 46, page_w - 220, 95),
        meta["title"],
        fontsize=17, fontfile=font, color=(1, 1, 1),
    )
    page.insert_textbox(
        fitz.Rect(margin, 96, page_w - 220, 130),
        meta["sub"],
        fontsize=9, fontfile=font, color=(0.85, 0.92, 0.95),
    )
    page.insert_textbox(
        fitz.Rect(margin, 128, page_w - 220, 145),
        f"V{VERSION} · {DATE}",
        fontsize=8, fontfile=font, color=(0.75, 0.82, 0.88),
    )
    if LOGO.exists():
        plate = fitz.Rect(page_w - 220, 28, page_w - 28, 120)
        page.draw_rect(plate, color=(1, 1, 1), fill=(1, 1, 1))
        page.insert_image(fitz.Rect(page_w - 210, 36, page_w - 38, 112), filename=str(LOGO), keep_proportion=True)

    y = 168
    # toc
    toc_lines = meta["toc"] + "\n" + "\n".join(f"{i}. {h}" for i, (h, _) in enumerate(meta["sections"], 1))
    rc = page.insert_textbox(
        fitz.Rect(margin, y, page_w - margin, page_h - margin),
        toc_lines,
        fontsize=10, fontfile=font, color=(0.1, 0.25, 0.3),
    )
    y += 28 + 14 * (len(meta["sections"]) + 1)

    for i, (heading, body) in enumerate(meta["sections"], 1):
        text = f"{i}. {heading}\n{body}\n"
        while True:
            if y > page_h - margin - 60:
                page = add_page()
                y = margin
            used = page.insert_textbox(
                fitz.Rect(margin, y, page_w - margin, page_h - margin),
                text,
                fontsize=10, fontfile=font, color=(0.12, 0.12, 0.12),
            )
            if used >= 0:
                # advance: approximate from newlines
                y += max(36, (text.count("\n") + 2) * 13)
                break
            # overflow → new page and retry
            page = add_page()
            y = margin

    doc.set_metadata({
        "title": meta["title"],
        "author": "ICOPAY",
        "subject": f"Merchant ops manual V{VERSION}",
    })
    doc.save(path, deflate=True, garbage=3)
    doc.close()
    return path


def main() -> None:
    if not LOGO.exists():
        print("missing logo", LOGO, file=sys.stderr)
        sys.exit(1)
    ff = fontfile()
    print("font", ff)
    for lang, meta in DOCS.items():
        p = build_one(lang, meta, ff)
        print("wrote", p.name, p.stat().st_size)


if __name__ == "__main__":
    main()
