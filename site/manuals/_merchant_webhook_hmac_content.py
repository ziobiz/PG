# -*- coding: utf-8 -*-
"""서버 웹훅(HMAC) API 연동 메뉴얼 — 운영관리 목록용 전용 PDF (5국어)."""
from __future__ import annotations

from _merchant_user_manual_theme import DATE_EN, DATE_KO

VERSION = "1.00"


def _meta(lang: str) -> str:
    targets = {
        "ko": "가맹 개발·운영 담당 (총본사·본사·총판 열람 가능)",
        "en": "Merchant developers & ops (also visible to Super HQ / HQ / Dist)",
        "ja": "加盟開発・運用担当（総本部・本社・総代理も閲覧可）",
        "zh": "商户开发与运营（总本部/总部/总代理也可查看）",
        "th": "นักพัฒนา/ปฏิบัติการร้าน (HQ สูงสุด/HQ/ตัวแทนดูได้)",
    }
    t = targets[lang]
    if lang == "ko":
        return f"작성 기준: ICOPAY 서버 통보(HMAC) &nbsp;|&nbsp; 대상: {t} &nbsp;|&nbsp; 발행: {DATE_KO} &nbsp;|&nbsp; V{VERSION}"
    if lang == "en":
        return f"Based on: ICOPAY server notify (HMAC) &nbsp;|&nbsp; Target: {t} &nbsp;|&nbsp; Published: {DATE_EN} &nbsp;|&nbsp; V{VERSION}"
    if lang == "ja":
        return f"基準: ICOPAYサーバー通知(HMAC) &nbsp;|&nbsp; 対象: {t} &nbsp;|&nbsp; 発行: {DATE_EN} &nbsp;|&nbsp; V{VERSION}"
    if lang == "zh":
        return f"依据: ICOPAY 服务器通知(HMAC) &nbsp;|&nbsp; 对象: {t} &nbsp;|&nbsp; 发布: {DATE_EN} &nbsp;|&nbsp; V{VERSION}"
    return f"อ้างอิง: การแจ้งเซิร์ฟเวอร์ ICOPAY (HMAC) &nbsp;|&nbsp; กลุ่มเป้าหมาย: {t} &nbsp;|&nbsp; เผยแพร่: {DATE_EN} &nbsp;|&nbsp; V{VERSION}"


def webhook_doc(lang: str) -> dict:
    if lang == "ko":
        return {
            "page_title": f"ICOPAY 서버 웹훅(HMAC) API 연동 메뉴얼 V{VERSION}",
            "title_html": "서버 웹훅(HMAC)<br>API 연동 메뉴얼",
            "subtitle": "결제 확정 서버 통보 수신 · NOTI→가맹 callbackUrl · X-Icopay-Signature 검증",
            "meta": _meta("ko"),
            "footer_extra": f"ICOPAY 서버 웹훅(HMAC) API 연동 메뉴얼 · V{VERSION}",
            "perm_rows": [
                ("업체관리 &gt; 가맹 상세 &gt; 웹훅 서명", "full", "활성/비활성·시크릿 재발급 (가맹 우선)"),
                ("본사정책 &gt; 결제 라우팅 &gt; 웹훅 서명", "view", "본사 기본(기본 OFF) — 총본사·본사"),
                ("업체관리 &gt; 가맹점API", "view", "활성화된 기능 안내 · 샘플 문서 링크"),
                ("가맹 callbackUrl 서버", "full", "raw body HMAC 검증 · HTTP 200/401"),
            ],
            "toc": [
                ("s1", "이 메뉴얼의 범위"),
                ("s2", "통보 경로 (NOTI / ICOPAY 직통)"),
                ("s3", "관리자 설정 (본사·가맹)"),
                ("s4", "서명 규격"),
                ("s5", "가맹 서버 구현"),
                ("s6", "발신 IP (참고)"),
                ("s7", "샘플·문서"),
                ("s8", "체크리스트"),
                ("s9", "FAQ"),
            ],
            "body": f"""
    <h2 class="section-title" id="s1">1. 이 메뉴얼의 범위</h2>
    <p>결제 확정 후 가맹 <strong>서버</strong>로 오는 <strong>백그라운드 통보(웹훅)</strong>와, 웹훅 서명(HMAC)을 켜었을 때의 검증 방법을 설명합니다.</p>
    <ul>
      <li>브라우저 <strong>Result URL</strong> 리다이렉트와는 <strong>별개</strong>입니다.</li>
      <li>결제대행사(PG) 이름·operationalPgCd는 페이로드에 포함되지 않습니다(<strong>ICOPAY</strong> 중립).</li>
      <li>일상 화면 운영은 「가맹점 운영 메뉴얼」을, Checkout API는 가맹점API·merchant-api-samples를 참고하십시오.</li>
    </ul>
    <hr class="section-rule">

    <h2 class="section-title" id="s2">2. 통보 경로 (NOTI / ICOPAY 직통)</h2>
    <table>
      <tr><th>경로</th><th>발신</th><th>설명</th></tr>
      <tr><td><strong>일반(권장 기준)</strong></td><td><strong>NOTI</strong> → 가맹 <code>callbackUrl</code></td><td>결제 확정 서버 POST</td></tr>
      <tr><td>보조</td><td>ICOPAY → 결제통보·수신통보 URL</td><td>직통 POST (서명 ON이면 동일 헤더·시크릿)</td></tr>
    </table>
    <div class="info-box">서명 활성 시 NOTI·ICOPAY 직통 모두 <strong>같은 HMAC 검증</strong>을 하면 됩니다. ICOPAY가 NOTI에 설정을 동기합니다.</div>
    <hr class="section-rule">

    <h2 class="section-title" id="s3">3. <span class="step-badge">STEP 1</span> 관리자 설정</h2>
    <div class="menu-path">본사정책 &gt; 결제·URL &gt; 결제 라우팅 &gt; 웹훅 서명</div>
    <div class="menu-path">업체관리 &gt; 가맹 상세 &gt; 웹훅 서명</div>
    <table>
      <tr><th>구분</th><th>메뉴</th><th>값</th></tr>
      <tr><td>본사 기본</td><td>결제 라우팅 → 웹훅 서명</td><td><strong>OFF(비활성)</strong>가 기본 / ON(활성)</td></tr>
      <tr><td>가맹</td><td>가맹 상세 → 웹훅 서명</td><td><code>본사설정 따름</code> / <code>활성</code> / <code>비활성</code> (<strong>가맹 우선</strong>)</td></tr>
    </table>
    <ol>
      <li>가맹에서 <strong>활성</strong>을 선택합니다. (본사 기본이 OFF여도 가맹만 ON 가능)</li>
      <li><strong>시크릿 재발급</strong> — 평문은 <strong>한 번만</strong> 표시됩니다. 가맹 서버 환경변수 등에만 보관합니다.</li>
      <li>채팅·티켓·로그에 시크릿을 남기지 마십시오.</li>
    </ol>
    <div class="warn-box">실효가 비활성(N)이면 서명 헤더 없이 POST됩니다. 서명 ON인데 시크릿을 서버에 안 넣으면 검증이 실패합니다.</div>
    <hr class="section-rule">

    <h2 class="section-title" id="s4">4. <span class="step-badge">STEP 2</span> 서명 규격</h2>
    <table>
      <tr><th>항목</th><th>내용</th></tr>
      <tr><td>헤더</td><td><code>X-Icopay-Signature: v1=&#123;hex&#125;</code></td></tr>
      <tr><td>알고리즘</td><td>HMAC-SHA256</td></tr>
      <tr><td>메시지</td><td><strong>raw HTTP body</strong> (수신 바이트 그대로 — 파싱 후 재직렬화 금지)</td></tr>
      <tr><td>키</td><td>웹훅 서명 시크릿 (재발급 시 받은 값)</td></tr>
      <tr><td>hex</td><td>대소문자 무시 비교 권장</td></tr>
      <tr><td>실패 응답</td><td>HTTP <strong>401</strong> 권장 · 성공 <strong>200</strong></td></tr>
    </table>
    <pre>expected = HMAC_SHA256_HEX(raw_body, webhook_secret)
header   = "v1=" + expected</pre>
    <hr class="section-rule">

    <h2 class="section-title" id="s5">5. <span class="step-badge">STEP 3</span> 가맹 서버 구현</h2>
    <ul>
      <li><strong>HTTPS</strong> 엔드포인트</li>
      <li>타임아웃 내 <strong>HTTP 200</strong> (검증 성공 시)</li>
      <li>동일 주문 <strong>중복 통보</strong>를 가정한 <strong>멱등</strong> 처리</li>
      <li>서명 활성 시: raw body로 HMAC 검증 후 JSON 파싱</li>
      <li>환경변수 예: <code>ICOPAY_WEBHOOK_SIGN_SECRET</code></li>
    </ul>
    <div class="check-box">페이로드 예 필드: <code>event</code>, <code>compId</code>, <code>trnId</code>, <code>orderNo</code>, <code>status</code>, <code>paymentStatus</code>, <code>amount</code>, <code>currency</code>, <code>paidAt</code>, <code>notifyChannel</code>, <code>ts</code> 등</div>
    <hr class="section-rule">

    <h2 class="section-title" id="s6">6. 발신 IP (참고)</h2>
    <table>
      <tr><th>발신</th><th>IPv4</th><th>IPv6</th></tr>
      <tr><td>NOTI (일반)</td><td><code>172.235.192.86</code></td><td><code>2400:8905::2000:6dff:fe4b:8079</code></td></tr>
      <tr><td>ICOPAY 직통</td><td><code>1.234.65.25</code></td><td>—</td></tr>
    </table>
    <div class="warn-box">IP allowlist만으로 검증하지 말고 <strong>HMAC을 주 검증</strong>으로 쓰십시오. IP는 보조입니다.</div>
    <hr class="section-rule">

    <h2 class="section-title" id="s7">7. 샘플·문서</h2>
    <ul>
      <li>가맹점API 화면 「활성화된 기능 안내」→ <strong>서버 웹훅 보안(HMAC) — NOTI</strong></li>
      <li><code>merchant-api-samples/docs/webhook-hmac-noti.ko.md</code> (EN·JA·CH·TH)</li>
      <li>PHP: <code>merchant-api-samples/php/notify_webhook.php</code></li>
      <li>JSP: <code>merchant-api-samples/jsp/notify-webhook.jsp</code></li>
    </ul>
    <hr class="section-rule">

    <h2 class="section-title" id="s8">8. 체크리스트</h2>
    <div class="check-box">
      <ol>
        <li>가맹 웹훅 서명 = 활성 (또는 본사 ON + 따름)</li>
        <li>시크릿 재발급 → 서버에만 보관</li>
        <li>callbackUrl HTTPS · 멱등</li>
        <li><code>X-Icopay-Signature</code> 검증 · 실패 401</li>
        <li>테스트 결제 후 통보 수신·서명 yes 로그 확인</li>
      </ol>
    </div>
    <hr class="section-rule">

    <h2 class="section-title" id="s9">9. FAQ</h2>
    <div class="faq-item"><div class="faq-q">본사 기본이 OFF인데 가맹만 켤 수 있나요?</div><div class="faq-a">네. 가맹 상세에서 활성하면 본사 OFF여도 서명됩니다.</div></div>
    <div class="faq-item"><div class="faq-q">시크릿을 다시 보고 싶습니다.</div><div class="faq-a">평문은 재발급 직후 1회만 보입니다. 분실 시 재발급하고 서버 설정을 교체하세요.</div></div>
    <div class="faq-item"><div class="faq-q">서명이 맞지 않습니다.</div><div class="faq-a">raw body를 그대로 쓰는지, 시크릿·헤더 형식(<code>v1=</code>)이 맞는지 확인하세요. JSON을 다시 stringify하면 실패합니다.</div></div>
    <div class="faq-item"><div class="faq-q">Result 페이지에서도 서명하나요?</div><div class="faq-a">아니요. 브라우저 Result와 서버 웹훅은 별개입니다.</div></div>
""",
        }
    if lang == "en":
        return {
            "page_title": f"ICOPAY Server Webhook (HMAC) API Manual V{VERSION}",
            "title_html": "Server Webhook (HMAC)<br>API Integration Manual",
            "subtitle": "Server notify on payment confirm · NOTI→callbackUrl · verify X-Icopay-Signature",
            "meta": _meta("en"),
            "footer_extra": f"ICOPAY Server Webhook (HMAC) API Manual · V{VERSION}",
            "perm_rows": [
                ("Company &gt; Merchant detail &gt; Webhook signing", "full", "Active/Inactive · rotate secret"),
                ("HQ Policy &gt; Payment routing &gt; Webhook signing", "view", "HQ default (OFF by default)"),
                ("Company &gt; Merchant API", "view", "Feature guide · sample links"),
                ("Merchant callbackUrl server", "full", "HMAC on raw body · HTTP 200/401"),
            ],
            "toc": [
                ("s1", "Scope"),
                ("s2", "Notify paths"),
                ("s3", "Admin settings"),
                ("s4", "Signature spec"),
                ("s5", "Merchant server"),
                ("s6", "Sender IPs"),
                ("s7", "Samples & docs"),
                ("s8", "Checklist"),
                ("s9", "FAQ"),
            ],
            "body": f"""
    <h2 class="section-title" id="s1">1. Scope</h2>
    <p>Background <strong>server webhook</strong> after payment confirm, and how to verify HMAC when signing is Active. Separate from browser Result URL. Payload stays <strong>ICOPAY</strong>-neutral (no PG name).</p>
    <hr class="section-rule">
    <h2 class="section-title" id="s2">2. Notify paths</h2>
    <table>
      <tr><th>Path</th><th>Sender</th><th>Notes</th></tr>
      <tr><td><strong>Primary</strong></td><td><strong>NOTI</strong> → merchant <code>callbackUrl</code></td><td>Server POST on confirm</td></tr>
      <tr><td>Secondary</td><td>ICOPAY → Background / inbound notify URLs</td><td>Same header/secret when signing ON</td></tr>
    </table>
    <div class="info-box">ICOPAY syncs signing settings to NOTI. Verify once for both paths.</div>
    <hr class="section-rule">
    <h2 class="section-title" id="s3">3. <span class="step-badge">STEP 1</span> Admin settings</h2>
    <div class="menu-path">HQ Policy &gt; Payment routing &gt; Webhook signing</div>
    <div class="menu-path">Company &gt; Merchant detail &gt; Webhook signing</div>
    <p>HQ default is usually <strong>OFF</strong>. Merchant can set Follow HQ / Active / Inactive (<strong>merchant wins</strong>). After Active, <strong>Rotate secret</strong> (plaintext once) → store only on merchant server.</p>
    <hr class="section-rule">
    <h2 class="section-title" id="s4">4. <span class="step-badge">STEP 2</span> Signature spec</h2>
    <table>
      <tr><th>Item</th><th>Detail</th></tr>
      <tr><td>Header</td><td><code>X-Icopay-Signature: v1=&#123;hex&#125;</code></td></tr>
      <tr><td>Algo</td><td>HMAC-SHA256</td></tr>
      <tr><td>Message</td><td><strong>raw HTTP body</strong> (no re-serialize)</td></tr>
      <tr><td>Fail / OK</td><td>HTTP <strong>401</strong> / <strong>200</strong></td></tr>
    </table>
    <pre>expected = HMAC_SHA256_HEX(raw_body, webhook_secret)
header   = "v1=" + expected</pre>
    <hr class="section-rule">
    <h2 class="section-title" id="s5">5. <span class="step-badge">STEP 3</span> Merchant server</h2>
    <ul><li>HTTPS</li><li>Idempotent handling</li><li>Verify HMAC before JSON parse when signing ON</li><li>Env e.g. <code>ICOPAY_WEBHOOK_SIGN_SECRET</code></li></ul>
    <hr class="section-rule">
    <h2 class="section-title" id="s6">6. Sender IPs (optional)</h2>
    <table>
      <tr><th>Sender</th><th>IPv4</th><th>IPv6</th></tr>
      <tr><td>NOTI</td><td><code>172.235.192.86</code></td><td><code>2400:8905::2000:6dff:fe4b:8079</code></td></tr>
      <tr><td>ICOPAY direct</td><td><code>1.234.65.25</code></td><td>—</td></tr>
    </table>
    <div class="warn-box">Prefer <strong>HMAC as primary</strong>; IP allowlist is secondary.</div>
    <hr class="section-rule">
    <h2 class="section-title" id="s7">7. Samples &amp; docs</h2>
    <ul>
      <li>Merchant API feature guide — Server webhook security (HMAC) — NOTI</li>
      <li><code>webhook-hmac-noti.md</code> (+ .ko / .ja / .ch / .th)</li>
      <li>PHP <code>notify_webhook.php</code> · JSP <code>notify-webhook.jsp</code></li>
    </ul>
    <hr class="section-rule">
    <h2 class="section-title" id="s8">8. Checklist</h2>
    <div class="check-box"><ol><li>Signing Active</li><li>Secret stored server-side only</li><li>HTTPS + idempotent callback</li><li>Verify header · 401 on fail</li><li>Test payment → signed notify log</li></ol></div>
    <hr class="section-rule">
    <h2 class="section-title" id="s9">9. FAQ</h2>
    <div class="faq-item"><div class="faq-q">Merchant Active while HQ OFF?</div><div class="faq-a">Yes — merchant override signs.</div></div>
    <div class="faq-item"><div class="faq-q">Lost the secret?</div><div class="faq-a">Rotate again and update the server; plaintext is shown only once.</div></div>
    <div class="faq-item"><div class="faq-q">Signature mismatch?</div><div class="faq-a">Use raw body bytes; do not re-stringify JSON. Check <code>v1=</code> format and secret.</div></div>
""",
        }
    return _loc(lang)


def _loc(lang: str) -> dict:
    en = webhook_doc("en")
    titles = {
        "ja": (
            "ICOPAY サーバーWebhook(HMAC) API連携マニュアル",
            "サーバーWebhook(HMAC)<br>API連携マニュアル",
            "決済確定サーバー通知 · NOTI→callbackUrl · X-Icopay-Signature検証",
        ),
        "zh": (
            "ICOPAY 服务器 Webhook(HMAC) API 对接手册",
            "服务器 Webhook(HMAC)<br>API 对接手册",
            "支付确认服务器通知 · NOTI→callbackUrl · 校验 X-Icopay-Signature",
        ),
        "th": (
            "ICOPAY คู่มือเชื่อมต่อ Server Webhook (HMAC) API",
            "Server Webhook (HMAC)<br>คู่มือเชื่อมต่อ API",
            "แจ้งเซิร์ฟเวอร์เมื่อยืนยันชำระ · NOTI→callbackUrl · ตรวจ X-Icopay-Signature",
        ),
    }
    page, title, sub = titles[lang]
    toc = {
        "ja": [
            ("s1", "対象範囲"),
            ("s2", "通知経路"),
            ("s3", "管理画面設定"),
            ("s4", "署名仕様"),
            ("s5", "加盟サーバー実装"),
            ("s6", "送信元IP"),
            ("s7", "サンプル・文書"),
            ("s8", "チェックリスト"),
            ("s9", "FAQ"),
        ],
        "zh": [
            ("s1", "适用范围"),
            ("s2", "通知路径"),
            ("s3", "管理设置"),
            ("s4", "签名规范"),
            ("s5", "商户服务器实现"),
            ("s6", "来源 IP"),
            ("s7", "样例与文档"),
            ("s8", "检查清单"),
            ("s9", "FAQ"),
        ],
        "th": [
            ("s1", "ขอบเขต"),
            ("s2", "เส้นทางแจ้ง"),
            ("s3", "ตั้งค่าแอดมิน"),
            ("s4", "สเปคลายเซ็น"),
            ("s5", "เซิร์ฟเวอร์ร้าน"),
            ("s6", "IP ผู้ส่ง"),
            ("s7", "ตัวอย่าง/เอกสาร"),
            ("s8", "เช็คลิสต์"),
            ("s9", "FAQ"),
        ],
    }
    perm = {
        "ja": [
            ("業者管理 &gt; 加盟詳細 &gt; Webhook署名", "full", "有効/無効・シークレット再発行"),
            ("本社政策 &gt; 決済ルーティング &gt; Webhook署名", "view", "本社既定(既定OFF)"),
            ("業者管理 &gt; 加盟店API", "view", "機能案内・サンプル"),
            ("加盟 callbackUrl サーバー", "full", "raw body HMAC · 200/401"),
        ],
        "zh": [
            ("企业管理 &gt; 商户详情 &gt; Webhook 签名", "full", "启用/停用 · 轮换密钥"),
            ("总部政策 &gt; 支付路由 &gt; Webhook 签名", "view", "总部默认(默认 OFF)"),
            ("企业管理 &gt; 商户 API", "view", "功能说明 · 样例"),
            ("商户 callbackUrl 服务器", "full", "raw body HMAC · 200/401"),
        ],
        "th": [
            ("จัดการร้าน &gt; รายละเอียดร้าน &gt; ลายเซ็น Webhook", "full", "เปิด/ปิด · ออก secret ใหม่"),
            ("นโยบาย HQ &gt; เส้นทางชำระ &gt; ลายเซ็น Webhook", "view", "ค่าเริ่ม HQ (ปิดเป็นค่าเริ่ม)"),
            ("จัดการร้าน &gt; Merchant API", "view", "คู่มือฟีเจอร์ · ตัวอย่าง"),
            ("เซิร์ฟเวอร์ callbackUrl", "full", "HMAC raw body · 200/401"),
        ],
    }
    # Replace English section headings with localized TOC labels in body via simple title swaps
    body = en["body"]
    swaps = {
        "ja": [
            ("1. Scope", "1. 対象範囲"),
            ("2. Notify paths", "2. 通知経路"),
            ("3. <span class=\"step-badge\">STEP 1</span> Admin settings", "3. <span class=\"step-badge\">STEP 1</span> 管理画面設定"),
            ("4. <span class=\"step-badge\">STEP 2</span> Signature spec", "4. <span class=\"step-badge\">STEP 2</span> 署名仕様"),
            ("5. <span class=\"step-badge\">STEP 3</span> Merchant server", "5. <span class=\"step-badge\">STEP 3</span> 加盟サーバー実装"),
            ("6. Sender IPs (optional)", "6. 送信元IP（参考）"),
            ("7. Samples &amp; docs", "7. サンプル・文書"),
            ("8. Checklist", "8. チェックリスト"),
            ("9. FAQ", "9. FAQ"),
            (
                "Background <strong>server webhook</strong> after payment confirm, and how to verify HMAC when signing is Active. Separate from browser Result URL. Payload stays <strong>ICOPAY</strong>-neutral (no PG name).",
                "決済確定後の<strong>サーバーWebhook</strong>と、署名有効時のHMAC検証を説明します。ブラウザ Result とは別です。ペイロードは<strong>ICOPAY</strong>中立です。",
            ),
            (
                "HQ default is usually <strong>OFF</strong>. Merchant can set Follow HQ / Active / Inactive (<strong>merchant wins</strong>). After Active, <strong>Rotate secret</strong> (plaintext once) → store only on merchant server.",
                "本社既定は通常<strong>OFF</strong>。加盟は本社に従う/有効/無効（<strong>加盟優先</strong>）。有効後<strong>シークレット再発行</strong>（平文1回）→加盟サーバーのみ保管。",
            ),
            (
                "Prefer <strong>HMAC as primary</strong>; IP allowlist is secondary.",
                "IP許可のみに頼らず<strong>HMACを主検証</strong>にしてください。",
            ),
        ],
        "zh": [
            ("1. Scope", "1. 适用范围"),
            ("2. Notify paths", "2. 通知路径"),
            ("3. <span class=\"step-badge\">STEP 1</span> Admin settings", "3. <span class=\"step-badge\">STEP 1</span> 管理设置"),
            ("4. <span class=\"step-badge\">STEP 2</span> Signature spec", "4. <span class=\"step-badge\">STEP 2</span> 签名规范"),
            ("5. <span class=\"step-badge\">STEP 3</span> Merchant server", "5. <span class=\"step-badge\">STEP 3</span> 商户服务器实现"),
            ("6. Sender IPs (optional)", "6. 来源 IP（参考）"),
            ("7. Samples &amp; docs", "7. 样例与文档"),
            ("8. Checklist", "8. 检查清单"),
            (
                "Background <strong>server webhook</strong> after payment confirm, and how to verify HMAC when signing is Active. Separate from browser Result URL. Payload stays <strong>ICOPAY</strong>-neutral (no PG name).",
                "说明支付确认后的<strong>服务器 Webhook</strong>，以及启用签名时如何校验 HMAC。与浏览器 Result 分开。载荷保持 <strong>ICOPAY</strong> 中立。",
            ),
            (
                "HQ default is usually <strong>OFF</strong>. Merchant can set Follow HQ / Active / Inactive (<strong>merchant wins</strong>). After Active, <strong>Rotate secret</strong> (plaintext once) → store only on merchant server.",
                "总部默认通常为 <strong>OFF</strong>。商户可设跟随总部/启用/停用（<strong>商户优先</strong>）。启用后<strong>重新签发密钥</strong>（明文一次）→ 仅保存在商户服务器。",
            ),
            (
                "Prefer <strong>HMAC as primary</strong>; IP allowlist is secondary.",
                "请以 <strong>HMAC 为主校验</strong>，IP 白名单仅作辅助。",
            ),
        ],
        "th": [
            ("1. Scope", "1. ขอบเขต"),
            ("2. Notify paths", "2. เส้นทางแจ้ง"),
            ("3. <span class=\"step-badge\">STEP 1</span> Admin settings", "3. <span class=\"step-badge\">STEP 1</span> ตั้งค่าแอดมิน"),
            ("4. <span class=\"step-badge\">STEP 2</span> Signature spec", "4. <span class=\"step-badge\">STEP 2</span> สเปคลายเซ็น"),
            ("5. <span class=\"step-badge\">STEP 3</span> Merchant server", "5. <span class=\"step-badge\">STEP 3</span> เซิร์ฟเวอร์ร้าน"),
            ("6. Sender IPs (optional)", "6. IP ผู้ส่ง (อ้างอิง)"),
            ("7. Samples &amp; docs", "7. ตัวอย่าง/เอกสาร"),
            ("8. Checklist", "8. เช็คลิสต์"),
            (
                "Background <strong>server webhook</strong> after payment confirm, and how to verify HMAC when signing is Active. Separate from browser Result URL. Payload stays <strong>ICOPAY</strong>-neutral (no PG name).",
                "อธิบาย <strong>Webhook เซิร์ฟเวอร์</strong> หลังยืนยันชำระ และการตรวจ HMAC เมื่อเปิดลายเซ็น แยกจาก Result ในเบราว์เซอร์ เพย์โหลดเป็นกลางแบบ <strong>ICOPAY</strong>",
            ),
            (
                "HQ default is usually <strong>OFF</strong>. Merchant can set Follow HQ / Active / Inactive (<strong>merchant wins</strong>). After Active, <strong>Rotate secret</strong> (plaintext once) → store only on merchant server.",
                "ค่าเริ่ม HQ มักเป็น <strong>OFF</strong> ร้านตั้ง ตาม HQ / เปิด / ปิด ได้ (<strong>ร้านมาก่อน</strong>) หลังเปิดให้<strong>ออก secret ใหม่</strong> (ข้อความชัด 1 ครั้ง) — เก็บเฉพาะเซิร์ฟเวอร์ร้าน",
            ),
            (
                "Prefer <strong>HMAC as primary</strong>; IP allowlist is secondary.",
                "ใช้ <strong>HMAC เป็นการตรวจหลัก</strong> IP allowlist เป็นตัวช่วย",
            ),
        ],
    }
    for a, b in swaps[lang]:
        body = body.replace(a, b)
    return {
        "page_title": f"{page} V{VERSION}",
        "title_html": title,
        "subtitle": sub,
        "meta": _meta(lang),
        "footer_extra": f"ICOPAY Webhook HMAC · V{VERSION}",
        "perm_rows": perm[lang],
        "toc": toc[lang],
        "body": body,
    }
