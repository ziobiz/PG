# -*- coding: utf-8 -*-
"""Generate crypto checkout API docs (KO/EN/JA/ZH/TH HTML+TXT). Run from this directory."""
from __future__ import annotations

from pathlib import Path

OUT = Path(__file__).resolve().parent
DOC_ID = "ICOPAY-CRYPTO-CHECKOUT-001"
DOC_VER = "1.0"
DATE = "2026-10-15"

CSS = """
    body { font-family: system-ui, "Malgun Gothic", "Segoe UI", sans-serif; max-width: 960px; margin: 1.5rem auto; padding: 0 1rem; line-height: 1.55; color: #222; }
    h1 { font-size: 1.35rem; margin-bottom: 0.25rem; }
    h2 { font-size: 1.05rem; margin-top: 1.75rem; border-bottom: 1px solid #ccc; padding-bottom: 0.25rem; }
    h3 { font-size: 0.98rem; margin-top: 1.25rem; }
    p.meta { color: #555; font-size: 0.92rem; }
    table { width: 100%; border-collapse: collapse; font-size: 0.88rem; margin: 0.75rem 0 1rem; }
    th, td { border: 1px solid #ccc; padding: 0.45rem 0.5rem; vertical-align: top; }
    th { background: #f5f5f5; text-align: left; }
    code { background: #f4f4f4; padding: 0.05em 0.3em; border-radius: 3px; }
    pre { background: #f8f8f8; border: 1px solid #ddd; padding: 0.75rem; overflow: auto; font-size: 0.82rem; white-space: pre-wrap; }
    .flow { background: #fafafa; border: 1px solid #ddd; padding: 0.75rem 1rem; font-size: 0.85rem; font-family: ui-monospace, monospace; }
    .note { background: #eef6ff; border-left: 4px solid #0d6efd; padding: 0.65rem 0.85rem; margin: 0.75rem 0; font-size: 0.92rem; }
    .warn { background: #fff8e6; border-left: 4px solid #e6a800; padding: 0.65rem 0.85rem; margin: 0.75rem 0; font-size: 0.92rem; }
    .lang-nav { font-size: 0.92rem; margin-bottom: 1rem; padding: 0.5rem 0; border-bottom: 1px solid #eee; }
    a { color: #0d6efd; }
"""

LANGS = {
    "en": {
        "file": "crypto-checkout-api.html",
        "txt": "crypto-checkout-api.txt",
        "html_lang": "en",
        "title": "ICOPAY Crypto Checkout API — Merchant Integration",
        "nav": '<strong>English</strong> · <a href="crypto-checkout-api.ko.html">한국어</a> · <a href="crypto-checkout-api.ja.html">日本語</a> · <a href="crypto-checkout-api.ch.html">中文</a> · <a href="crypto-checkout-api.th.html">ไทย</a>',
        "related": 'Related: <a href="unified-checkout-api-flow.html">Unified checkout flow</a> · <a href="unified-checkout-api-parameters.html">Prepare parameters</a>',
        "h_scope": "1. Scope",
        "scope": """
  <p>Add <strong>crypto</strong> payments on the same merchant MID that already uses card checkout.
    Shopper and merchant UI always show <strong>ICOPAY</strong> only (no PG brand).</p>
  <div class="note"><strong>Recommended:</strong> same unified prepare as cards, with
    <code>payMeans=CRYPTO</code>. Reuse the existing NOTI Callback/Result used for cards (e.g. ElementPay).</div>
""",
        "h_prereq": "2. Prerequisites (HQ must enable)",
        "prereq": """
  <table>
    <tr><th>Item</th><th>Note</th></tr>
    <tr><td>Merchant crypto use = Y</td><td>Company detail or HQ Policy → Pay·URL → Crypto settings</td></tr>
    <tr><td>Allowed assets</td><td>e.g. USDT only for first go-live</td></tr>
    <tr><td>Merchant receive wallet</td><td>One network + address per enabled asset (settlement payout)</td></tr>
    <tr><td>HQ custody wallet</td><td>ICOPAY deposit address for that asset×network</td></tr>
    <tr><td>Company use = Y</td><td>Unused / permanent stop blocks crypto</td></tr>
  </table>
""",
        "h_api": "3. Prepare (recommended)",
        "api": """
  <p><code>POST {publicApiBaseUrl}/api/middleware/v1/merchant/checkout/prepare</code></p>
  <p>Header: <code>Content-Type: application/json</code>, <code>X-Icopay-Merchant-Broker-Secret</code> (same live secret as cards).</p>
  <pre>{
  "compId": "6000000064",
  "orderNo": "ORD-CRYPTO-001",
  "amount": "10000",
  "displayCurrency": "JPY",
  "payMeans": "CRYPTO",
  "productName": "Sample product",
  "lang": "ENG",
  "buyer": {
    "email": "buyer@example.com",
    "phone": "1012345678",
    "countryIso2": "KR",
    "firstName": "Gildong",
    "lastName": "Hong"
  }
}</pre>
  <table>
    <tr><th>Field</th><th>Required</th><th>Description</th></tr>
    <tr><td>payMeans</td><td class="m">M*</td><td><code>CRYPTO</code> (aliases: checkoutMeans / paymentMeans)</td></tr>
    <tr><td>amount</td><td>M</td><td>Display amount; ICOPAY quotes to USD then remittance asset</td></tr>
    <tr><td>displayCurrency</td><td>O</td><td>JPY/USD/… — merchant crypto display currency if omitted</td></tr>
    <tr><td>assetCode / networkCode</td><td>O</td><td>Optional; shopper can pick on ICOPAY crypto page</td></tr>
    <tr><td>buyer name/email</td><td>M*</td><td>Latin letters for name/email; phone digits (same as crypto UI)</td></tr>
  </table>
  <p><strong>Response:</strong> <code>pgVendor</code> always <code>ICOPAY</code>. Use <code>payUrl</code> / <code>sessionToken</code>.
    Crypto session tokens may start with <code>cr_</code>.</p>
  <p>Alternative: <code>POST /api/pay/crypto/prepare</code> with <code>compId</code> (same product rules).</p>
""",
        "h_url": "4. URL channels (non-API)",
        "url": """
  <table>
    <tr><th>URL</th><th>When</th></tr>
    <tr><td>Crypto payment URL</td><td>Crypto-only link on company crypto card</td></tr>
    <tr><td><code>/checkout-all/{compId}</code></td><td>Unified (card + crypto) when web pay and crypto use are both ON</td></tr>
    <tr><td><code>/checkout/{compId}</code></td><td>Card URL pay (existing)</td></tr>
  </table>
""",
        "h_cb": "5. Callback / Result",
        "cb": """
  <div class="warn">Do <strong>not</strong> register a separate crypto Callback on ICOPAY company “Payment notify URL”.
    Flow: deposit confirm → ICOPAY → <code>POST /noti/crypto</code> → NOTI relays to the <strong>same merchant callbackUrl</strong> as cards (match by Comp-Id).</div>
  <p>Extend your existing webhook handler:</p>
  <ul>
    <li>Match order via <code>orderid</code> / <code>orderNo</code></li>
    <li>Idempotent success handling (<code>returncode</code> / JPAY-compatible schema)</li>
    <li>Extra fields: <code>assetCode</code>, <code>networkCode</code>, <code>depositTxHash</code></li>
    <li>Branch when <code>pgKind</code>/<code>van</code> indicates crypto</li>
  </ul>
  <p>Status poll: <code>GET .../merchant/checkout/status?compId=&amp;orderNo=</code> or
    <code>GET /api/pay/crypto/status</code>.</p>
""",
        "h_chk": "6. Go-live checklist",
        "chk": """
  <ul>
    <li>HQ crypto use + asset + custody + receive wallet for the MID</li>
    <li>prepare with <code>payMeans=CRYPTO</code> → open payUrl → small deposit → HQ Crypto deposits confirm</li>
    <li>Callback received on existing card URL; store crypto fields</li>
    <li>Card regression still OK</li>
  </ul>
""",
        "footer": "Plain text: <a href=\"crypto-checkout-api.txt\">crypto-checkout-api.txt</a>",
    },
    "ko": {
        "file": "crypto-checkout-api.ko.html",
        "txt": "crypto-checkout-api.ko.txt",
        "html_lang": "ko",
        "title": "ICOPAY 크립토 Checkout API — 가맹 연동",
        "nav": '<a href="crypto-checkout-api.html">English</a> · <strong>한국어</strong> · <a href="crypto-checkout-api.ja.html">日本語</a> · <a href="crypto-checkout-api.ch.html">中文</a> · <a href="crypto-checkout-api.th.html">ไทย</a>',
        "related": '관련: <a href="unified-checkout-api-flow.ko.html">통합 Checkout 흐름</a> · <a href="unified-checkout-api-parameters.ko.html">Prepare 파라미터</a>',
        "h_scope": "1. 범위",
        "scope": """
  <p>이미 <strong>카드</strong>가 연동된 동일 가맹 MID에 <strong>크립토</strong> 결제를 추가합니다.
    가맹·구매자 화면에는 항상 <strong>ICOPAY</strong>만 표시됩니다(PG명 미노출).</p>
  <div class="note"><strong>권장:</strong> 카드와 같은 통합 prepare에 <code>payMeans=CRYPTO</code>를 넣습니다.
    Callback/Result는 카드용 NOTI 등록분을 <strong>재사용</strong>합니다(Comp-Id 매칭).</div>
""",
        "h_prereq": "2. 전제 (본사 설정)",
        "prereq": """
  <table>
    <tr><th>항목</th><th>비고</th></tr>
    <tr><td>크립토 사용 = Y</td><td>업체상세 또는 본사정책 → 결제·URL → 크립토설정</td></tr>
    <tr><td>허용자산</td><td>1차 오픈은 USDT만 권장</td></tr>
    <tr><td>가맹 수취 지갑</td><td>활성 자산마다 네트워크+주소 1세트(정산 출금용)</td></tr>
    <tr><td>대표수탁지갑</td><td>해당 자산×네트워크 ICOPAY 입금 주소</td></tr>
    <tr><td>업체 사용 = Y</td><td>미사용·영구정지면 크립토 차단</td></tr>
  </table>
""",
        "h_api": "3. Prepare (권장)",
        "api": """
  <p><code>POST {publicApiBaseUrl}/api/middleware/v1/merchant/checkout/prepare</code></p>
  <p>헤더: <code>Content-Type: application/json</code>, <code>X-Icopay-Merchant-Broker-Secret</code> (카드와 동일 Live 시크릿).</p>
  <pre>{
  "compId": "6000000064",
  "orderNo": "ORD-CRYPTO-001",
  "amount": "10000",
  "displayCurrency": "JPY",
  "payMeans": "CRYPTO",
  "productName": "샘플 상품",
  "lang": "KOR",
  "buyer": {
    "email": "buyer@example.com",
    "phone": "1012345678",
    "countryIso2": "KR",
    "firstName": "Gildong",
    "lastName": "Hong"
  }
}</pre>
  <table>
    <tr><th>필드</th><th>필수</th><th>설명</th></tr>
    <tr><td>payMeans</td><td>M*</td><td><code>CRYPTO</code> (별칭: checkoutMeans / paymentMeans)</td></tr>
    <tr><td>amount</td><td>M</td><td>표시금액 → ICOPAY가 USD 청구 후 자산 송금액 안내</td></tr>
    <tr><td>displayCurrency</td><td>O</td><td>JPY/USD 등. 생략 시 가맹 크립토 표시통화</td></tr>
    <tr><td>assetCode / networkCode</td><td>O</td><td>생략 가능 — 결제창에서 선택</td></tr>
    <tr><td>buyer 성명·이메일</td><td>M*</td><td>성명·이메일 영문, 전화 숫자(결제창과 동일)</td></tr>
  </table>
  <p><strong>응답:</strong> <code>pgVendor</code>는 항상 <code>ICOPAY</code>. <code>payUrl</code> / <code>sessionToken</code> 사용.
    크립토 세션은 <code>cr_</code> 접두일 수 있습니다.</p>
  <p>대체: <code>POST /api/pay/crypto/prepare</code> (<code>compId</code> 포함).</p>
""",
        "h_url": "4. URL 채널 (비 API)",
        "url": """
  <table>
    <tr><th>URL</th><th>언제</th></tr>
    <tr><td>크립토 결제 URL</td><td>크립토 전용 (업체 크립토설정 카드)</td></tr>
    <tr><td><code>/checkout-all/{compId}</code></td><td>웹결제+크립토 사용 모두 ON일 때 통합(카드/크립토 선택)</td></tr>
    <tr><td><code>/checkout/{compId}</code></td><td>기존 카드 URL 결제</td></tr>
  </table>
""",
        "h_cb": "5. Callback / Result",
        "cb": """
  <div class="warn">ICOPAY 업체상세 「결제통보 URL」에 크립토 전용 Callback을 <strong>새로 등록하지 않습니다</strong>.
    흐름: 입금 확정 → ICOPAY → <code>POST /noti/crypto</code> → NOTI가 Comp-Id로 <strong>기존 카드 callbackUrl</strong>에 릴레이.</div>
  <p>기존 웹훅 핸들러를 확장하세요.</p>
  <ul>
    <li><code>orderid</code> / <code>orderNo</code>로 주문 매칭</li>
    <li>멱등 성공 처리 (JPAY 호환 <code>returncode</code> 등)</li>
    <li>추가 필드: <code>assetCode</code>, <code>networkCode</code>, <code>depositTxHash</code></li>
    <li><code>pgKind</code>/<code>van</code>이 crypto이면 결제수단=크립토로 저장</li>
  </ul>
  <p>상태 조회: <code>GET .../merchant/checkout/status</code> 또는 <code>GET /api/pay/crypto/status</code>.</p>
""",
        "h_chk": "6. 오픈 전 체크리스트",
        "chk": """
  <ul>
    <li>본사 크립토 사용·자산·수탁·가맹 수취 설정</li>
    <li>prepare(<code>payMeans=CRYPTO</code>) → payUrl → 소액 입금 → 크립토내역 확정</li>
    <li>기존 Callback으로 수신·크립토 필드 저장</li>
    <li>카드 결제 회귀 정상</li>
  </ul>
""",
        "footer": "텍스트: <a href=\"crypto-checkout-api.ko.txt\">crypto-checkout-api.ko.txt</a>",
    },
}

# JA / ZH / TH compact from EN structure with translations
JA = {
    "file": "crypto-checkout-api.ja.html",
    "txt": "crypto-checkout-api.ja.txt",
    "html_lang": "ja",
    "title": "ICOPAY クリプト Checkout API — 加盟連携",
    "nav": '<a href="crypto-checkout-api.html">English</a> · <a href="crypto-checkout-api.ko.html">한국어</a> · <strong>日本語</strong> · <a href="crypto-checkout-api.ch.html">中文</a> · <a href="crypto-checkout-api.th.html">ไทย</a>',
    "related": '関連: <a href="unified-checkout-api-flow.ja.html">統合 Checkout フロー</a> · <a href="unified-checkout-api-parameters.ja.html">Prepare パラメータ</a>',
    "h_scope": "1. 範囲",
    "scope": """
  <p>カード連携済みの同一加盟 MID に<strong>クリプト</strong>決済を追加します。表示は常に<strong>ICOPAY</strong>のみです。</p>
  <div class="note"><strong>推奨:</strong> 統合 prepare に <code>payMeans=CRYPTO</code>。Callback/Result はカード用 NOTI 登録を<strong>再利用</strong>（Comp-Id）。</div>
""",
    "h_prereq": "2. 前提（本社設定）",
    "prereq": LANGS["en"]["prereq"].replace("HQ Policy", "本社政策").replace("Crypto settings", "クリプト設定"),
    "h_api": "3. Prepare（推奨）",
    "api": LANGS["en"]["api"].replace("Recommended", "推奨").replace("Sample product", "サンプル商品"),
    "h_url": "4. URL チャネル",
    "url": LANGS["en"]["url"],
    "h_cb": "5. Callback / Result",
    "cb": """
  <div class="warn">会社詳細の「決済通知 URL」にクリプト専用 Callback を<strong>新規登録しません</strong>。
    入金確定 → ICOPAY → <code>/noti/crypto</code> → NOTI が既存カード callbackUrl へリレー。</div>
  <ul>
    <li><code>orderid</code> / <code>orderNo</code> で照合</li>
    <li>冪等処理</li>
    <li>追加: <code>assetCode</code>, <code>networkCode</code>, <code>depositTxHash</code></li>
  </ul>
""",
    "h_chk": "6. 公開前チェック",
    "chk": LANGS["en"]["chk"],
    "footer": 'テキスト: <a href="crypto-checkout-api.ja.txt">crypto-checkout-api.ja.txt</a>',
}
ZH = {
    "file": "crypto-checkout-api.ch.html",
    "txt": "crypto-checkout-api.ch.txt",
    "html_lang": "zh",
    "title": "ICOPAY 加密货币 Checkout API — 商户对接",
    "nav": '<a href="crypto-checkout-api.html">English</a> · <a href="crypto-checkout-api.ko.html">한국어</a> · <a href="crypto-checkout-api.ja.html">日本語</a> · <strong>中文</strong> · <a href="crypto-checkout-api.th.html">ไทย</a>',
    "related": '相关: <a href="unified-checkout-api-flow.ch.html">统一 Checkout 流程</a> · <a href="unified-checkout-api-parameters.ch.html">Prepare 参数</a>',
    "h_scope": "1. 范围",
    "scope": """
  <p>在已对接<strong>银行卡</strong>的同一商户 MID 上增加<strong>加密货币</strong>支付。对客/对商户仅显示 <strong>ICOPAY</strong>。</p>
  <div class="note"><strong>推荐:</strong> 统一 prepare 增加 <code>payMeans=CRYPTO</code>。Callback/Result <strong>复用</strong> 银行卡 NOTI 注册（按 Comp-Id）。</div>
""",
    "h_prereq": "2. 前提（总部设置）",
    "prereq": LANGS["en"]["prereq"],
    "h_api": "3. Prepare（推荐）",
    "api": LANGS["en"]["api"],
    "h_url": "4. URL 渠道",
    "url": LANGS["en"]["url"],
    "h_cb": "5. Callback / Result",
    "cb": """
  <div class="warn">勿在公司详情「支付通知 URL」单独注册加密 Callback。
    入金确认 → ICOPAY → <code>/noti/crypto</code> → NOTI 转发至既有银行卡 callbackUrl。</div>
  <ul>
    <li>用 <code>orderid</code>/<code>orderNo</code> 匹配订单</li>
    <li>幂等处理</li>
    <li>额外字段: <code>assetCode</code>, <code>networkCode</code>, <code>depositTxHash</code></li>
  </ul>
""",
    "h_chk": "6. 上线检查",
    "chk": LANGS["en"]["chk"],
    "footer": '纯文本: <a href="crypto-checkout-api.ch.txt">crypto-checkout-api.ch.txt</a>',
}
TH = {
    "file": "crypto-checkout-api.th.html",
    "txt": "crypto-checkout-api.th.txt",
    "html_lang": "th",
    "title": "ICOPAY Crypto Checkout API — การเชื่อมต่อร้านค้า",
    "nav": '<a href="crypto-checkout-api.html">English</a> · <a href="crypto-checkout-api.ko.html">한국어</a> · <a href="crypto-checkout-api.ja.html">日本語</a> · <a href="crypto-checkout-api.ch.html">中文</a> · <strong>ไทย</strong>',
    "related": 'ที่เกี่ยวข้อง: <a href="unified-checkout-api-flow.th.html">Unified checkout flow</a> · <a href="unified-checkout-api-parameters.th.html">Prepare parameters</a>',
    "h_scope": "1. ขอบเขต",
    "scope": """
  <p>เพิ่มการชำระ<strong>คริปโต</strong>บน MID เดียวกับที่เชื่อมบัตรแล้ว แสดงเฉพาะ <strong>ICOPAY</strong></p>
  <div class="note"><strong>แนะนำ:</strong> unified prepare พร้อม <code>payMeans=CRYPTO</code> ใช้ Callback/Result ของบัตรซ้ำ (Comp-Id)</div>
""",
    "h_prereq": "2. ข้อกำหนดล่วงหน้า (HQ)",
    "prereq": LANGS["en"]["prereq"],
    "h_api": "3. Prepare (แนะนำ)",
    "api": LANGS["en"]["api"],
    "h_url": "4. URL",
    "url": LANGS["en"]["url"],
    "h_cb": "5. Callback / Result",
    "cb": """
  <div class="warn">ไม่ต้องลงทะเบียน Callback คริปโตแยกที่ Payment notify URL
    ยืนยันเงินเข้า → ICOPAY → <code>/noti/crypto</code> → NOTI ส่งต่อไปยัง callbackUrl บัตรเดิม</div>
  <ul>
    <li>จับคู่คำสั่งซื้อด้วย <code>orderid</code>/<code>orderNo</code></li>
    <li>ทำแบบ idempotent</li>
    <li>ฟิลด์เพิ่ม: <code>assetCode</code>, <code>networkCode</code>, <code>depositTxHash</code></li>
  </ul>
""",
    "h_chk": "6. เช็คลิสต์ก่อนเปิดใช้",
    "chk": LANGS["en"]["chk"],
    "footer": 'ข้อความ: <a href="crypto-checkout-api.th.txt">crypto-checkout-api.th.txt</a>',
}

LANGS["ja"] = JA
LANGS["zh"] = ZH
LANGS["th"] = TH


def render_html(L: dict) -> str:
    return f"""<!DOCTYPE html>
<html lang="{L['html_lang']}">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>{L['title']}</title>
  <style>{CSS}</style>
</head>
<body>
  <p class="lang-nav"><strong>Lang:</strong> {L['nav']}</p>
  <h1>{L['title']}</h1>
  <p class="meta">Doc ID: {DOC_ID} · v{DOC_VER} · {DATE} · ICOPAY neutral crypto</p>
  <p>{L['related']}</p>
  <h2>{L['h_scope']}</h2>
  {L['scope']}
  <h2>{L['h_prereq']}</h2>
  {L['prereq']}
  <h2>{L['h_api']}</h2>
  {L['api']}
  <h2>{L['h_url']}</h2>
  {L['url']}
  <h2>{L['h_cb']}</h2>
  {L['cb']}
  <h2>{L['h_chk']}</h2>
  {L['chk']}
  <p class="meta">{L['footer']}</p>
</body>
</html>
"""


def html_to_txt(html: str, title: str) -> str:
    import re
    t = re.sub(r"<style>.*?</style>", "", html, flags=re.S)
    t = re.sub(r"<[^>]+>", "\n", t)
    t = re.sub(r"\n{3,}", "\n\n", t)
    return f"{title}\nDoc ID: {DOC_ID} v{DOC_VER} ({DATE})\n\n{t.strip()}\n"


def main() -> None:
    for key, L in LANGS.items():
        html = render_html(L)
        (OUT / L["file"]).write_text(html, encoding="utf-8")
        (OUT / L["txt"]).write_text(html_to_txt(html, L["title"]), encoding="utf-8")
        print("wrote", L["file"], L["txt"])


if __name__ == "__main__":
    main()
