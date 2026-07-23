# -*- coding: utf-8 -*-
"""Build merchant USER manuals (URL / split / subscription) — same design as merchant-ops PDF.

Layout/fonts/cover identical to `_build_merchant_ops_pdf_v2.py`.
Content style matches merchant-ops (메뉴 경로 / 절차 / 주의 / 체크리스트).
"""
from __future__ import annotations

import shutil
import sys
from pathlib import Path

import fitz

ROOT = Path(__file__).resolve().parent
LOGO = ROOT / "assets" / "cover-brand-logo.png"
VERSION = "2.64"
DATE = "2026-07-23"

LANG_FONT = {
    "ko": Path(r"C:\Windows\Fonts\malgun.ttf"),
    "en": Path(r"C:\Windows\Fonts\malgun.ttf"),
    "ja": Path(r"C:\Windows\Fonts\msgothic.ttc"),
    "zh": Path(r"C:\Windows\Fonts\msyh.ttc"),
    "th": Path(r"C:\Windows\Fonts\LeelawUI.ttf"),
}
for k, alts in {
    "ja": [Path(r"C:\Windows\Fonts\YuGothM.ttc"), Path(r"C:\Windows\Fonts\msgothic.ttc")],
    "zh": [Path(r"C:\Windows\Fonts\msyh.ttc"), Path(r"C:\Windows\Fonts\simsun.ttc")],
    "th": [
        Path(r"C:\Windows\Fonts\LeelawUI.ttf"),
        Path(r"C:\Windows\Fonts\tahomabd.ttf"),
        Path(r"C:\Windows\Fonts\tahoma.ttf"),
    ],
}.items():
    if not LANG_FONT[k].exists():
        for a in alts:
            if a.exists():
                LANG_FONT[k] = a
                break


# ---------------------------------------------------------------------------
# Content — same tone/structure as merchant-ops (not API integration specs)
# ---------------------------------------------------------------------------
MANUALS: dict[str, dict[str, dict]] = {
    "merchant-url-user": {
        "ko": {
            "title": "URL결제 사용자 메뉴얼",
            "sub": "가맹점 권한 화면 기준 — 결제 URL 공유·거래 확인·일상 운영",
            "toc": "목차",
            "sections": [
                ("이 매뉴얼의 범위",
                 "가맹점(MERCHANT) 계정으로 접근 가능한 화면을 기준으로, 고객에게 공개 결제 링크를 보내고 승인 여부를 확인하는 방법을 설명합니다.\n"
                 "메뉴는 본사정책 → 접근·권한에서 부여한 권한에 따라 달라집니다. 접근불가 메뉴는 사이드바에 보이지 않습니다.\n"
                 "개발자용 API·키 발급·연동 스펙은 포함하지 않습니다. 연동이 필요하면 상위 본사·총판에 「연동·배포 → 가맹 API 출시」 안내를 요청하십시오.\n"
                 "결제대행사(PG) 이름은 가맹·구매자 UI에서 ICOPAY로 중립 표시됩니다."),
                ("로그인과 화면",
                 "1) 가맹점 ID로 로그인  2) 허용된 메뉴만 좌측에 표시  3) 언어(KO/EN/JP/CH/TH) 전환  4) 대시보드가 있으면 최근 결제 요약 확인"),
                ("공지사항",
                 "메뉴 경로: 업체관리 → 공지사항\n"
                 "본사·총판 운영 공지(수수료·정산일·점검·웹결제 중지)를 로그인 후 먼저 확인합니다."),
                ("업체정보조회 — 결제 URL",
                 "메뉴 경로: 업체관리 → 업체정보조회\n"
                 "결제 URL: 공개 일회 결제 주소(예: https://서비스도메인/checkout/업체코드). [복사]로 전달합니다.\n"
                 "URL 재결제 URL: 저장 카드 재결제용(본사·기능이 켜져 있을 때만 표시).\n"
                 "절차: 업체정보조회 → 결제 URL 확인 → [복사] → SMS·메신저·메일·쇼핑몰에 붙여넣기 → 본인 브라우저에서 테스트 오픈.\n"
                 "주의: 웹결제 미사용·중지면 URL이 열리지 않거나 안내만 표시될 수 있습니다. 상위 일괄 중지도 확인하십시오."),
                ("고객에게 전달할 때",
                 "1) 복사한 URL만 전달하거나, 금액·상품명을 메시지에 함께 적습니다.\n"
                 "2) 결제창 금액·상품명 표시는 본사정책 따름/가맹 설정에 따릅니다.\n"
                 "3) 신규 고객에는 일반 결제 URL을 쓰고, 재결제 URL은 저장 카드 고객에게만 사용합니다."),
                ("결제내역 확인",
                 "메뉴 경로: 결제관리 → 결제내역 (권한이 있으면 URL결제내역)\n"
                 "기간·상태·키워드(주문번호)로 검색합니다. 승인·실패·취소·환불을 구분합니다.\n"
                 "고객 문의 시 주문번호·결제시각·금액을 함께 적어 두면 처리가 빠릅니다."),
                ("자주 하는 확인",
                 "· URL을 열었는데 안내만 보임 → 웹결제 사용여부·상위 중지 여부\n"
                 "· 결제가 실패함 → 결제내역 실패 사유 확인, 필요 시 상위 「리스크 현황」 문의\n"
                 "· 메뉴가 없음 → 본사·총판에 해당 메뉴 권한 개방 요청"),
                ("일상 체크리스트",
                 "1 공지 확인  2 결제 URL 복사·테스트 오픈  3 당일/전일 승인·실패 점검  4 실패 급증 시 주문번호와 함께 상위 공유  5 재결제 URL 오남용 여부"),
                ("문의",
                 f"메뉴가 없거나 웹결제가 중지된 경우 상위 본사·총판에 권한·설정 개방을 요청하십시오. 문서 버전 V{VERSION}."),
            ],
        },
        "en": {
            "title": "URL Payment — User Manual",
            "sub": "Merchant screens — share payment URL, verify results, daily ops",
            "toc": "Contents",
            "sections": [
                ("Scope",
                 "How merchant staff share a public payment link and verify approvals on permitted screens.\n"
                 "Menus follow HQ Policy → Access. Hidden menus need HQ/distributor grants.\n"
                 "Does not cover API keys or integration specs — ask HQ for Merchant API launch docs.\n"
                 "PG names stay neutralized as ICOPAY on merchant/buyer UI."),
                ("Sign-in",
                 "1) Sign in  2) Sidebar shows permitted menus only  3) Switch KO/EN/JP/CH/TH  4) Check dashboard if shown"),
                ("Notices",
                 "Path: Companies → Notices\nRead HQ/distributor announcements (fees, settlement, maintenance, web-pay suspend) after login."),
                ("Company info — Payment URL",
                 "Path: Companies → Company info\n"
                 "Payment URL: public one-time checkout (e.g. https://domain/checkout/{compCode}). Use Copy.\n"
                 "Re-pay URL: saved-card repay (shown only when enabled).\n"
                 "Steps: Company info → Copy Payment URL → paste to SMS/messenger/email/shop → open once yourself.\n"
                 "Note: Off/Suspended web-pay may block the URL."),
                ("Sharing with customers",
                 "1) Share the URL; add amount/product notes in the message if needed.\n"
                 "2) Checkout display follows HQ/merchant settings.\n"
                 "3) New buyers use Payment URL; Re-pay URL only for returning saved-card customers."),
                ("Payment list",
                 "Path: Payments → Payment list (or URL payment list if permitted)\n"
                 "Search by period/status/keyword (order no.). Review approvals, failures, cancels, refunds."),
                ("Quick checks",
                 "· Notice-only page → web-pay / HQ suspend\n"
                 "· Failures → check fail reason; ask HQ Risk dashboard if needed\n"
                 "· Missing menu → ask HQ to open permissions"),
                ("Daily checklist",
                 "1 Notices  2 Copy & test Payment URL  3 Today/yesterday approvals & failures  4 Escalate spikes with order ids  5 Avoid misuse of Re-pay URL"),
                ("Support",
                 f"Ask HQ/distributor for missing menus or web-pay settings. Document version V{VERSION}."),
            ],
        },
        "ja": {
            "title": "URL決済 ユーザーマニュアル",
            "sub": "加盟店権限画面 — 決済URL共有・結果確認・日常運用",
            "toc": "目次",
            "sections": [
                ("対象範囲",
                 "加盟店ログインで決済リンクを送り承認を確認する案内です。APIキー・連携仕様は含みません。\n"
                 "メニューは本社政策→権限に従い、アクセス不可は非表示です。購入者UIの決済代行名はICOPAY表示です。"),
                ("ログイン",
                 "1) ログイン  2) 許可メニューのみ  3) 言語切替  4) ダッシュボード確認"),
                ("お知らせ",
                 "経路: 業者管理 → お知らせ\nログイン後に本社・総代理の告知を確認します。"),
                ("業者情報 — 決済URL",
                 "経路: 業者管理 → 業者情報照会\n"
                 "決済URLを[コピー]して共有します。再決済URLは機能ON時のみ。\n"
                 "手順: 照会 → コピー → SMS等へ貼付 → 自分で一度開く。\n"
                 "注意: Web決済停止中は開けない場合があります。"),
                ("顧客への共有",
                 "金額・商品名は必要ならメッセージに併記。新規は通常URL、保存カード顧客のみ再決済URL。"),
                ("決済一覧",
                 "経路: 決済管理 → 決済一覧（権限があればURL決済一覧）\n期間・状態・注文番号で検索し承認/失敗/取消/返金を確認します。"),
                ("よくある確認",
                 "案内のみ表示→Web決済/停止。失敗→履歴確認・上位へ。メニュー無し→権限依頼。"),
                ("チェックリスト",
                 "1 お知らせ  2 URLコピー・テスト  3 当日/前日の承認・失敗  4 急増時は注文番号共有  5 再決済URLの誤用防止"),
                ("問い合わせ",
                 f"権限・設定は上位の総代理/本社へ。文書 V{VERSION}。"),
            ],
        },
        "zh": {
            "title": "URL支付用户手册",
            "sub": "按商户权限画面 — 分享支付 URL、核对结果、日常运营",
            "toc": "目录",
            "sections": [
                ("适用范围",
                 "商户登录后向客户发送公开支付链接并核对是否成功。不含 API 密钥与对接规格。\n"
                 "菜单以总部策略→权限为准；无权限菜单不显示。买家端支付机构名显示为 ICOPAY。"),
                ("登录与画面",
                 "1) 登录  2) 仅显示允许菜单  3) 切换语言  4) 查看仪表盘（如有）"),
                ("公告",
                 "路径：企业管理 → 公告\n登录后先查看总部/总代理运营公告。"),
                ("企业信息 — 支付 URL",
                 "路径：企业管理 → 企业信息查询\n"
                 "支付 URL：公开一次性结账地址，使用[复制]。\n"
                 "再支付 URL：仅在启用时显示（已存卡）。\n"
                 "步骤：查询 → 复制 → 粘贴到短信/即时消息 → 自行打开测试。\n"
                 "注意：未启用或停用网页支付时链接可能无法打开。"),
                ("发给客户",
                 "必要时在消息中注明金额/商品名。新客用普通支付 URL；再支付 URL 仅用于老客户。"),
                ("支付明细",
                 "路径：支付管理 → 支付明细（或 URL 支付明细）\n按日期、状态、订单号查询。"),
                ("常见检查",
                 "仅显示提示→网页支付/停用。失败→查明细并联系上级。无菜单→申请权限。"),
                ("日常清单",
                 "1 公告  2 复制并测试 URL  3 当日/前日成败  4 异常增多时附订单号上报  5 避免滥用再支付 URL"),
                ("联系",
                 f"缺菜单或停用请联系上级总代理/总部。文档 V{VERSION}。"),
            ],
        },
        "th": {
            "title": "คู่มือผู้ใช้ชำระด้วย URL",
            "sub": "ตามหน้าจอสิทธิ์ร้าน — แชร์ URL ชำระ ตรวจผล งานประจำวัน",
            "toc": "สารบัญ",
            "sections": [
                ("ขอบเขต",
                 "วิธีแชร์ลิงก์ชำระสาธารณะและตรวจการอนุมัติ ไม่รวมคีย์ API/สเปกเชื่อมต่อ\n"
                 "เมนูตามนโยบาย HQ → สิทธิ์ ชื่อ PG บน UI ผู้ซื้อเป็น ICOPAY"),
                ("เข้าสู่ระบบ",
                 "1) เข้าสู่ระบบ  2) แสดงเฉพาะเมนูที่อนุญาต  3) สลับภาษา  4) ดูแดชบอร์ด"),
                ("ประกาศ",
                 "เส้นทาง: จัดการร้าน → ประกาศ\nตรวจประกาศ HQ/ตัวแทนหลังล็อกอิน"),
                ("ข้อมูลร้าน — Payment URL",
                 "เส้นทาง: จัดการร้าน → ข้อมูลร้าน\n"
                 "คัดลอก Payment URL แล้วส่งลูกค้า เปิดทดสอบเองหนึ่งครั้ง\n"
                 "หมายเหตุ: ปิดเว็บชำระอาจเปิดลิงก์ไม่ได้"),
                ("ส่งให้ลูกค้า",
                 "ระบุจำนวนเงินในข้อความถ้าจำเป็น ลูกค้าใหม่ใช้ URL ปกติ"),
                ("รายการชำระ",
                 "เส้นทาง: การชำระ → รายการชำระ ค้นตามวันที่/สถานะ/เลขคำสั่ง"),
                ("ตรวจเร็ว",
                 "เห็นแต่ข้อความแจ้ง → ปิดเว็บชำระ ไม่มีเมนู → ขอสิทธิ์"),
                ("เช็คลิสต์",
                 "1 ประกาศ  2 คัดลอก/ทดสอบ URL  3 ตรวจสำเร็จ/ล้มเหลว  4 แจ้งเลขคำสั่งเมื่อผิดปกติ"),
                ("ติดต่อ",
                 f"ติดต่อตัวแทน/HQ เอกสาร V{VERSION}"),
            ],
        },
    },
    "merchant-split-user": {
        "ko": {
            "title": "분할결제 사용자 메뉴얼",
            "sub": "가맹점 권한 화면 기준 — 분할결제 URL·계약·회차·일상 운영",
            "toc": "목차",
            "sections": [
                ("이 매뉴얼의 범위",
                 "분할결제를 사용하는 가맹점이 고객에게 분할 신청 링크를 주고, 계약·회차 납부 상태를 확인하는 방법을 설명합니다.\n"
                 "메뉴는 본사정책 → 접근·권한에 따릅니다. 개발 연동·분할 계약 API 스펙은 포함하지 않습니다.\n"
                 "결제대행사(PG) 이름은 가맹·구매자 UI에서 ICOPAY로 중립 표시됩니다."),
                ("로그인과 화면",
                 "1) 가맹점 ID로 로그인  2) 허용된 메뉴만 표시  3) 언어 전환  4) 대시보드 요약 확인"),
                ("분할결제 사용 조건",
                 "상위(본사·총판)에서 가맹 「분할결제 사용여부」가 사용(ON)이어야 분할결제 URL·분할관리 메뉴가 보입니다.\n"
                 "꺼져 있으면 상위 담당자에게 사용 개방을 요청하십시오."),
                ("업체정보조회 — 분할결제 URL",
                 "메뉴 경로: 업체관리 → 업체정보조회\n"
                 "분할결제 URL: 고객이 분할 신청·회차 결제를 진행하는 공개 진입 주소. [복사]로 전달합니다.\n"
                 "절차: 업체정보조회 → 분할결제 URL 확인 → [복사] → 고객 채널에 전달 → 본인 브라우저에서 테스트.\n"
                 "주의: 분할결제 미사용이면 URL이 없거나 동작하지 않을 수 있습니다."),
                ("분할관리(권한 시)",
                 "메뉴 경로: 분할관리\n"
                 "계약 목록·회차 상태·미납을 확인합니다. 고객 문의 시 계약번호·회차·납부일을 함께 확인합니다.\n"
                 "회차 수·기간 규칙은 가맹/본사 설정을 따르며, 직원이 임의로 바꾸지 마십시오."),
                ("결제내역과의 관계",
                 "메뉴 경로: 결제관리 → 결제내역 (또는 분할결제내역)\n"
                 "각 회차 결제는 결제내역에도 나타납니다. 승인 실패 시 재시도·다른 카드 안내가 가능합니다."),
                ("자주 하는 확인",
                 "· 분할 URL/메뉴 없음 → 분할결제 사용여부·권한 확인\n"
                 "· 회차 실패 → 결제내역 실패 사유 확인 후 고객 안내\n"
                 "· 연체·미납 → 분할관리에서 계약·회차 상태 확인 후 상위 문의"),
                ("일상 체크리스트",
                 "1 공지 확인  2 분할결제 URL 유효·테스트  3 신규 계약·미납 회차 점검  4 실패 급증 시 계약/주문번호와 함께 상위 공유"),
                ("문의",
                 f"설정·권한·정산은 상위 본사·총판에 연락하십시오. 문서 버전 V{VERSION}."),
            ],
        },
        "en": {
            "title": "Split Payment — User Manual",
            "sub": "Merchant screens — split URL, contracts, installments, daily ops",
            "toc": "Contents",
            "sections": [
                ("Scope",
                 "Share the split-pay link and track contracts/installments. Not an API guide.\n"
                 "Menus follow HQ permissions. PG names show as ICOPAY on buyer UI."),
                ("Sign-in",
                 "1) Sign in  2) Permitted menus only  3) Language  4) Dashboard"),
                ("Enablement",
                 "Split payment must be ON for your merchant. Ask HQ if URL/menus are missing."),
                ("Company info — Split URL",
                 "Path: Companies → Company info\n"
                 "Copy Split payment URL and share with the customer. Test in your browser.\n"
                 "Note: Off split-pay may hide or disable the URL."),
                ("Split management (if permitted)",
                 "Path: Split management\n"
                 "Review contracts, installment status, arrears. Do not change installment rules arbitrarily."),
                ("Payment list",
                 "Path: Payments → Payment list (or Split payment list)\n"
                 "Each installment appears in payment history. On failure, ask the customer to retry."),
                ("Quick checks",
                 "· No URL/menu → enablement/permissions\n"
                 "· Failed installment → fail reason in payment list\n"
                 "· Arrears → check Split management then escalate"),
                ("Daily checklist",
                 "1 Notices  2 Test Split URL  3 New contracts & unpaid installments  4 Escalate spikes with contract/order ids"),
                ("Support",
                 f"Ask HQ/distributor for settings. Document version V{VERSION}."),
            ],
        },
        "ja": {
            "title": "分割決済 ユーザーマニュアル",
            "sub": "加盟店権限画面 — 分割URL・契約・回次・日常運用",
            "toc": "目次",
            "sections": [
                ("対象範囲",
                 "分割決済リンク共有と契約・回次確認。API仕様は含みません。購入者UIはICOPAY表示。"),
                ("ログイン",
                 "1) ログイン  2) 許可メニュー  3) 言語  4) ダッシュボード"),
                ("利用条件",
                 "「分割決済使用」がONであること。URL/メニューが無い場合は上位へ依頼。"),
                ("業者情報 — 分割URL",
                 "経路: 業者管理 → 業者情報照会\n分割決済URLをコピーし顧客に送り、自分で一度開いて確認します。"),
                ("分割管理",
                 "経路: 分割管理\n契約・回次・未納を確認。回次ルールは設定に従い勝手に変更しません。"),
                ("決済一覧",
                 "各回次は決済一覧にも表示。失敗時は再試行案内。"),
                ("よくある確認",
                 "URL無し→使用/権限。失敗→履歴。延滞→分割管理→上位。"),
                ("チェックリスト",
                 "1 お知らせ  2 URLテスト  3 新規契約・未納  4 急増時は契約/注文番号共有"),
                ("問い合わせ",
                 f"設定は上位へ。文書 V{VERSION}。"),
            ],
        },
        "zh": {
            "title": "分期支付用户手册",
            "sub": "按商户权限画面 — 分期 URL、合同、期次、日常运营",
            "toc": "目录",
            "sections": [
                ("适用范围",
                 "分享分期链接并核对合同/期次。不含 API 规格。买家端显示 ICOPAY。"),
                ("登录",
                 "1) 登录  2) 允许菜单  3) 语言  4) 仪表盘"),
                ("启用条件",
                 "须开启「使用分期支付」，否则无 URL/菜单。"),
                ("企业信息 — 分期 URL",
                 "路径：企业管理 → 企业信息查询\n复制分期支付 URL 发给客户并自行测试。"),
                ("分期管理",
                 "路径：分期管理\n查看合同、期次、欠款。勿擅自改期次规则。"),
                ("支付明细",
                 "各期会出现在支付明细。失败时可请客户重试。"),
                ("常见检查",
                 "无 URL→启用/权限。失败→明细。欠款→分期管理→上级。"),
                ("日常清单",
                 "1 公告  2 测试分期 URL  3 新合同与欠款  4 异常附合同/订单号上报"),
                ("联系",
                 f"设置请联系上级。文档 V{VERSION}。"),
            ],
        },
        "th": {
            "title": "คู่มือผู้ใช้แบ่งจ่าย",
            "sub": "ตามหน้าจอสิทธิ์ร้าน — URL แบ่งจ่าย สัญญา งวด งานประจำวัน",
            "toc": "สารบัญ",
            "sections": [
                ("ขอบเขต",
                 "แชร์ลิงก์แบ่งจ่ายและตรวจสัญญา/งวด ไม่รวมสเปก API ผู้ซื้อเห็น ICOPAY"),
                ("เข้าสู่ระบบ",
                 "1) เข้าสู่ระบบ  2) เมนูที่อนุญาต  3) ภาษา  4) แดชบอร์ด"),
                ("เงื่อนไข",
                 "ต้องเปิดใช้แบ่งจ่าย หากไม่มี URL/เมนู ขอ HQ"),
                ("ข้อมูลร้าน — URL แบ่งจ่าย",
                 "เส้นทาง: จัดการร้าน → ข้อมูลร้าน\nคัดลอก URL แบ่งจ่าย ส่งลูกค้า แล้วทดสอบเอง"),
                ("จัดการแบ่งจ่าย",
                 "ดูสัญญา/งวด/ค้างชำระ อย่าเปลี่ยนกติกางวดเอง"),
                ("รายการชำระ",
                 "แต่ละงวดโผล่ในรายการชำระ ล้มเหลวให้ลองใหม่"),
                ("ตรวจเร็ว",
                 "ไม่มี URL → เปิดใช้/สิทธิ์ ค้างชำระ → จัดการแบ่งจ่าย"),
                ("เช็คลิสต์",
                 "1 ประกาศ  2 ทดสอบ URL  3 สัญญาใหม่/ค้าง  4 แจ้งเลขสัญญาเมื่อผิดปกติ"),
                ("ติดต่อ",
                 f"ติดต่อตัวแทน/HQ เอกสาร V{VERSION}"),
            ],
        },
    },
    "merchant-subscribe-user": {
        "ko": {
            "title": "구독결제 사용자 메뉴얼",
            "sub": "가맹점 권한 화면 기준 — 구독(정기) 상태·청구·해지·일상 운영",
            "toc": "목차",
            "sections": [
                ("이 매뉴얼의 범위",
                 "구독(정기) 결제를 사용하는 가맹점이 고객 구독 상태·자동 청구·해지를 확인하는 방법을 설명합니다.\n"
                 "가맹 API subscription/prepare 등 개발 연동 스펙은 포함하지 않습니다. 연동은 상위 「가맹 API 출시」 문서를 따르십시오.\n"
                 "메뉴는 본사정책 → 접근·권한에 따릅니다. 구매자 UI의 결제대행사명은 ICOPAY로 표시됩니다."),
                ("로그인과 화면",
                 "1) 가맹점 ID로 로그인  2) 허용된 메뉴만 표시  3) 언어 전환  4) 대시보드 요약 확인"),
                ("구독 사용 조건",
                 "본사·가맹 설정에서 구독(정기결제)이 켜져 있고 관련 메뉴 권한이 열려 있어야 합니다.\n"
                 "메뉴가 없으면 상위 담당자에게 사용·권한 개방을 요청하십시오."),
                ("고객 안내 포인트",
                 "· 구독은 약정 주기(월 등)로 자동 청구될 수 있습니다.\n"
                 "· 결제 실패 시 재시도·카드 갱신이 필요할 수 있습니다.\n"
                 "· 해지는 계약·화면 정책에 따릅니다. 직원이 임의로 약관을 바꾸지 마십시오."),
                ("구독·결제 내역 확인",
                 "메뉴 경로: 결제관리 → 구독결제내역 (또는 결제내역)\n"
                 "기간·상태·주문/구독 식별자로 검색합니다. 성공·실패·해지 여부를 구분합니다.\n"
                 "고객 문의 시 구독/주문 식별자·청구일·금액을 함께 전달하면 처리가 빠릅니다."),
                ("해지·문의 대응",
                 "고객이 해지를 요청하면 화면에서 허용된 해지 절차를 따릅니다.\n"
                 "권한이 없으면 상위 운영자에게 구독 식별자를 전달합니다."),
                ("자주 하는 확인",
                 "· 구독 메뉴 없음 → 사용여부·권한 확인\n"
                 "· 청구 실패 → 결제내역 실패 사유·카드 갱신 안내\n"
                 "· 해지 요청 → 화면 절차 또는 상위 전달"),
                ("일상 체크리스트",
                 "1 공지 확인  2 당일 구독 청구 성공/실패 점검  3 실패 건 고객 안내  4 해지 요청 처리·상위 공유"),
                ("문의",
                 f"설정·권한·정산은 상위 본사·총판에 연락하십시오. 문서 버전 V{VERSION}."),
            ],
        },
        "en": {
            "title": "Subscription Payment — User Manual",
            "sub": "Merchant screens — subscription status, billing, cancel, daily ops",
            "toc": "Contents",
            "sections": [
                ("Scope",
                 "Check subscription status, recurring charges, and cancellations. Not an API guide.\n"
                 "Menus follow HQ permissions. Buyer UI shows ICOPAY."),
                ("Sign-in",
                 "1) Sign in  2) Permitted menus  3) Language  4) Dashboard"),
                ("Requirements",
                 "Subscription must be enabled and menus permitted. Ask HQ if missing."),
                ("Customer talking points",
                 "· Recurring charges by cycle\n"
                 "· Failed charges may need retry/card update\n"
                 "· Cancel per on-screen policy — do not change terms yourself"),
                ("View history",
                 "Path: Payments → Subscription payment list (or Payment list)\n"
                 "Filter by date/status/subscription or order id."),
                ("Cancellations",
                 "Follow on-screen cancel if allowed, or escalate the subscription id to HQ."),
                ("Quick checks",
                 "· No menu → enablement/permissions\n"
                 "· Charge failed → fail reason / ask customer to update card\n"
                 "· Cancel request → on-screen or escalate"),
                ("Daily checklist",
                 "1 Notices  2 Today subscription charges success/fail  3 Contact customers on failures  4 Process cancels / escalate"),
                ("Support",
                 f"Ask HQ/distributor. Document version V{VERSION}."),
            ],
        },
        "ja": {
            "title": "定期決済 ユーザーマニュアル",
            "sub": "加盟店権限画面 — 定期状態・請求・解約・日常運用",
            "toc": "目次",
            "sections": [
                ("対象範囲",
                 "定期決済の状態・請求・解約確認。API仕様は含みません。購入者UIはICOPAY表示。"),
                ("ログイン",
                 "1) ログイン  2) 許可メニュー  3) 言語  4) ダッシュボード"),
                ("利用条件",
                 "定期決済が有効でメニュー権限があること。無い場合は上位へ。"),
                ("顧客案内",
                 "周期課金・失敗時の再試行/カード更新・解約は画面ポリシーに従います。"),
                ("履歴確認",
                 "経路: 決済管理 → 定期決済履歴（または決済一覧）\n期間・状態・識別子で検索します。"),
                ("解約対応",
                 "画面の解約手順、または上位へ識別子を連絡します。"),
                ("よくある確認",
                 "メニュー無し→使用/権限。請求失敗→履歴・カード更新。解約→画面または上位。"),
                ("チェックリスト",
                 "1 お知らせ  2 当日請求の成否  3 失敗顧客案内  4 解約処理"),
                ("問い合わせ",
                 f"設定は上位へ。文書 V{VERSION}。"),
            ],
        },
        "zh": {
            "title": "订阅支付用户手册",
            "sub": "按商户权限画面 — 订阅状态、扣款、取消、日常运营",
            "toc": "目录",
            "sections": [
                ("适用范围",
                 "核对订阅状态、自动扣款与取消。不含 API 规格。买家端显示 ICOPAY。"),
                ("登录",
                 "1) 登录  2) 允许菜单  3) 语言  4) 仪表盘"),
                ("使用条件",
                 "须启用订阅且有菜单权限，否则联系上级。"),
                ("客户说明",
                 "按周期扣款；失败可能需重试/换卡；取消按画面政策。"),
                ("查看明细",
                 "路径：支付管理 → 订阅支付明细（或支付明细）\n按日期/状态/标识查询。"),
                ("取消处理",
                 "按画面流程，或将订阅标识交给上级。"),
                ("常见检查",
                 "无菜单→启用/权限。扣款失败→明细/换卡。取消→画面或上报。"),
                ("日常清单",
                 "1 公告  2 当日扣款成败  3 失败客户沟通  4 处理取消"),
                ("联系",
                 f"设置请联系上级。文档 V{VERSION}。"),
            ],
        },
        "th": {
            "title": "คู่มือผู้ใช้ชำระรายงวด",
            "sub": "ตามหน้าจอสิทธิ์ร้าน — สถานะสมาชิก เรียกเก็บ ยกเลิก งานประจำวัน",
            "toc": "สารบัญ",
            "sections": [
                ("ขอบเขต",
                 "ตรวจสถานะสมาชิก การเรียกเก็บ และการยกเลิก ไม่รวมสเปก API ผู้ซื้อเห็น ICOPAY"),
                ("เข้าสู่ระบบ",
                 "1) เข้าสู่ระบบ  2) เมนูที่อนุญาต  3) ภาษา  4) แดชบอร์ด"),
                ("เงื่อนไข",
                 "ต้องเปิดใช้สมาชิกและมีสิทธิ์เมนู"),
                ("คุยกับลูกค้า",
                 "เรียกเก็บตามรอบ ล้มเหลวอาจต้องลองใหม่/เปลี่ยนบัตร ยกเลิกตามนโยบายหน้าจอ"),
                ("ดูประวัติ",
                 "เส้นทาง: การชำระ → รายการชำระสมาชิก ค้นตามวันที่/สถานะ/รหัส"),
                ("ยกเลิก",
                 "ทำตามหน้าจอ หรือส่งรหัสสมาชิกให้ HQ"),
                ("ตรวจเร็ว",
                 "ไม่มีเมนู → เปิดใช้/สิทธิ์ เรียกเก็บล้มเหลว → ดูเหตุผล"),
                ("เช็คลิสต์",
                 "1 ประกาศ  2 ตรวจเรียกเก็บวันนี้  3 แจ้งลูกค้าเมื่อล้มเหลว  4 จัดการยกเลิก"),
                ("ติดต่อ",
                 f"ติดต่อตัวแทน/HQ เอกสาร V{VERSION}"),
            ],
        },
    },
}


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


def resolve_font(lang: str) -> tuple[fitz.Font, Path]:
    font_path = LANG_FONT.get(lang) or LANG_FONT["ko"]
    if not font_path.exists():
        font_path = Path(r"C:\Windows\Fonts\malgun.ttf")
    return fitz.Font(fontfile=str(font_path)), font_path


def build_one(out_dir: Path, lang: str, meta: dict) -> Path:
    """Identical layout to merchant-ops `_build_merchant_ops_pdf_v2.build_one`."""
    font, font_path = resolve_font(lang)
    path = out_dir / f"{lang}.pdf"
    doc = fitz.open()
    page_w, page_h = fitz.paper_size("a4")
    margin = 46
    max_w = page_w - 2 * margin
    line_h = 14

    def new_page():
        return doc.new_page(width=page_w, height=page_h)

    page = new_page()
    # cover bar — same as merchant-ops
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
        page.insert_image(
            fitz.Rect(page_w - 210, 36, page_w - 38, 112),
            filename=str(LOGO),
            keep_proportion=True,
        )

    y = 168
    blocks: list[tuple[str, bool]] = [(meta["toc"], True)]
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

    doc.set_metadata({
        "title": meta["title"],
        "author": "ICOPAY",
        "subject": f"V{VERSION}",
    })
    doc.save(path, deflate=True, garbage=3)
    doc.close()
    print("font", lang, font_path.name, "->", path.name, path.stat().st_size)
    return path


def main() -> None:
    if not LOGO.exists():
        print("missing logo", LOGO, file=sys.stderr)
        sys.exit(1)
    for mid, langs in MANUALS.items():
        out = ROOT / "pdf" / mid
        out.mkdir(parents=True, exist_ok=True)
        # same cover logo asset as merchant-ops
        shutil.copy2(LOGO, out / "logo.png")
        ops_logo = ROOT / "pdf" / "merchant-ops" / "logo.png"
        if ops_logo.exists():
            shutil.copy2(ops_logo, out / "logo.png")
        for lang, meta in langs.items():
            build_one(out, lang, meta)


if __name__ == "__main__":
    main()
