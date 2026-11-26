# -*- coding: utf-8 -*-
"""가맹점 운영 메뉴얼 content — chatbot merchant-manual design pattern."""
from __future__ import annotations

from _merchant_user_manual_theme import DATE_EN, DATE_KO

# merchant-ops PDF 문서 버전 (CJK Chrome PDF + 웹훅 절). 테마 공용 VERSION과 독립.
VERSION = "2.82"


def _meta(lang: str) -> str:
    targets = {
        "ko": "가맹점(MERCHANT) 운영 담당",
        "en": "Merchant operations staff",
        "ja": "加盟店運営担当",
        "zh": "商户运营人员",
        "th": "เจ้าหน้าที่ปฏิบัติการร้าน",
    }
    t = targets[lang]
    if lang == "ko":
        return f"작성 기준: ICOPAY 가맹점 권한 기준 &nbsp;|&nbsp; 대상: {t} &nbsp;|&nbsp; 발행: {DATE_KO} &nbsp;|&nbsp; V{VERSION}"
    if lang == "en":
        return f"Based on: ICOPAY Merchant Permission Levels &nbsp;|&nbsp; Target: {t} &nbsp;|&nbsp; Published: {DATE_EN} &nbsp;|&nbsp; V{VERSION}"
    if lang == "ja":
        return f"基準: ICOPAY加盟店権限 &nbsp;|&nbsp; 対象: {t} &nbsp;|&nbsp; 発行: {DATE_EN} &nbsp;|&nbsp; V{VERSION}"
    if lang == "zh":
        return f"依据: ICOPAY 商户权限标准 &nbsp;|&nbsp; 对象: {t} &nbsp;|&nbsp; 发布: {DATE_EN} &nbsp;|&nbsp; V{VERSION}"
    return f"อ้างอิง: สิทธิ์ร้าน ICOPAY &nbsp;|&nbsp; กลุ่มเป้าหมาย: {t} &nbsp;|&nbsp; เผยแพร่: {DATE_EN} &nbsp;|&nbsp; V{VERSION}"


def ops_doc(lang: str) -> dict:
    if lang == "ko":
        return {
            "page_title": f"ICOPAY 가맹점 운영 메뉴얼 V{VERSION}",
            "title_html": "가맹점<br>운영 메뉴얼",
            "subtitle": "가맹점 기준 — 결제·수수료·결제 URL·정산·일상 운영까지, 권한이 열린 화면에서 직접 하는 모든 것",
            "meta": _meta("ko"),
            "footer_extra": f"ICOPAY 가맹점 운영 메뉴얼 · V{VERSION}",
            "perm_rows": [
                ("업체관리 &gt; 내 업체정보", "view", "결제·재결제·챗봇·분할 URL 확인·복사 / 설정 변경 불가"),
                ("업체관리 &gt; 공지사항", "view", "본사·총판 운영 공지 확인"),
                ("결제관리 &gt; 결제내역·크립토내역", "view", "카드·크립토 입금 조회 (권한에 따라)"),
                ("정산관리 &gt; 수수료·정산·크립토정산", "view", "수수료·정산·크립토 출금 결과 조회"),
                ("업체등록·접근권한·크립토설정 변경", "none", "본사·총판 전용 (가맹은 크립토 조회·QR만)"),
            ],
            "toc": [
                ("s1", "이 매뉴얼의 범위"),
                ("s2", "시작 전 확인 (권한·웹결제)"),
                ("s3", "STEP 1 — 로그인·공지"),
                ("s4", "STEP 2 — 결제 URL 확인·공유"),
                ("s5", "STEP 3 — 결제내역 조회"),
                ("s6", "STEP 4 — 수수료·정산"),
                ("s7", "가맹점 API·챗봇·크립토(해당 시)"),
                ("s8", "서버 웹훅 보안(HMAC)·통보"),
                ("s9", "일상 체크리스트"),
                ("s10", "FAQ"),
            ],
            "body": f"""
    <h2 class="section-title" id="s1">1. 이 매뉴얼의 범위</h2>
    <p>본 문서는 <strong>가맹점(MERCHANT)</strong> 계정으로 로그인했을 때 접근할 수 있는 관리자 화면을 기준으로, 기본 운영 방법을 설명합니다.</p>
    <ul>
      <li>실제로 보이는 메뉴는 <strong>본사정책 → 접근·권한</strong>에서 가맹점 단계·개별 조직에 부여한 권한에 따라 달라집니다.</li>
      <li>권한이 <strong>접근불가</strong>인 메뉴는 사이드바에 표시되지 않습니다. 필요 메뉴가 없으면 상위 본사·총판에 권한 개방을 요청하십시오.</li>
      <li>결제대행사(PG) 이름은 가맹·구매자 화면에서 <strong>ICOPAY</strong>로 중립 표시됩니다.</li>
      <li>채널별 상세(URL·분할·구독·챗봇)는 같은 「운영 메뉴얼」의 가맹점용 사용자 메뉴얼을 참고하십시오.</li>
    </ul>
    <hr class="section-rule">

    <h2 class="section-title" id="s2">2. 시작 전 확인 (권한·웹결제)</h2>
    <table>
      <tr><th>확인 항목</th><th>확인 방법</th></tr>
      <tr><td>사이드바에 필요한 메뉴가 보이는가?</td><td>없으면 상위에 해당 메뉴 권한 개방 요청</td></tr>
      <tr><td>내 업체정보에 <strong>결제 URL</strong>이 있는가?</td><td>없으면 웹결제(URL결제) 사용/중지 상태를 상위 확인</td></tr>
    </table>
    <div class="hq-box">
      <strong>본사·총판에 요청해야 하는 사항</strong><br>
      • 메뉴 권한(결제내역·정산·API 등)<br>
      • 웹결제 사용 ON / 일괄 중지 해제<br>
      • 업체 기본정보(상호·주소) 변경<br>
      • 수수료·정산 주기 정책 안내
    </div>
    <hr class="section-rule">

    <h2 class="section-title" id="s3">3. <span class="step-badge">STEP 1</span> 로그인·공지</h2>
    <div class="menu-path">업체관리 &gt; 공지사항</div>
    <ol>
      <li>가맹점 ID로 로그인합니다.</li>
      <li>좌측에는 <strong>허용된 메뉴만</strong> 표시됩니다.</li>
      <li>표시 언어(KO/EN/JP/CH/TH)를 전환할 수 있습니다.</li>
      <li>공지에서 수수료·정산일·점검·웹결제 중지 안내를 먼저 확인합니다.</li>
    </ol>
    <hr class="section-rule">

    <h2 class="section-title" id="s4">4. <span class="step-badge">STEP 2</span> 결제 URL 확인·공유 <small style="font-size:10pt;font-weight:400;color:#888;">(조회 전용)</small></h2>
    <div class="menu-path">업체관리 &gt; 내 업체정보</div>
    <div class="info-box">내 업체정보는 <strong>조회 전용</strong>입니다. URL을 확인하고 <strong>복사</strong>할 수 있지만 설정을 직접 바꿀 수는 없습니다.</div>
    <pre>https://api.icopay.co.kr/checkout/{{업체코드}}</pre>
    <table>
      <tr><th>항목</th><th>설명</th></tr>
      <tr><td>결제 URL</td><td>카드(URL) 일회 결제. [복사] 후 고객에게 전달</td></tr>
      <tr><td>통합결제 URL</td><td>웹결제+크립토 사용이 모두 ON일 때 (<code>/checkout-all/업체코드</code>) — 카드·크립토 선택</td></tr>
      <tr><td>크립토 결제 URL</td><td>크립토 전용(크립토 사용 ON·상위 설정 시 표시)</td></tr>
      <tr><td>URL 재결제 URL</td><td>저장 카드 재결제(기능 ON일 때만)</td></tr>
      <tr><td>챗봇결제 URL</td><td>챗봇 사용 가맹만</td></tr>
      <tr><td>분할결제 URL</td><td>분할결제 사용 ON일 때만 (상위가 가맹 업체정보 → URL 분할결제에서 사용으로 저장)</td></tr>
    </table>
    <div class="check-box">절차: 업체정보 열기 → 결제 URL 확인 → [복사] → SMS·메신저·쇼핑몰에 붙여넣기 → 본인 브라우저에서 테스트 오픈</div>
    <div class="warn-box">웹결제 미사용·상위 중지면 URL이 없거나 안내만 표시될 수 있습니다. 크립토 URL은 상위가 크립토 사용·허용자산을 켠 뒤에만 표시됩니다.</div>
    <hr class="section-rule">

    <h2 class="section-title" id="s5">5. <span class="step-badge">STEP 3</span> 결제내역 조회 <small style="font-size:10pt;font-weight:400;color:#888;">(조회 전용)</small></h2>
    <div class="menu-path">결제관리 &gt; 결제내역 · 크립토내역</div>
    <ul>
      <li>기간·상태·키워드(주문번호)로 검색합니다.</li>
      <li>승인·실패·취소·환불을 구분합니다.</li>
      <li>권한이 있으면 URL/챗봇/구독/분할 결제내역·상태별 목록도 사용합니다.</li>
      <li><strong>크립토내역</strong>: 입금 대기·확정(Tx) 조회. 가맹은 조회 중심이며 확정·출금은 상위 운영.</li>
      <li>고객 문의 시 <strong>주문번호·결제시각·금액</strong>(크립토면 자산·네트워크)을 함께 전달합니다.</li>
    </ul>
    <hr class="section-rule">

    <h2 class="section-title" id="s6">6. <span class="step-badge">STEP 4</span> 수수료·정산</h2>
    <div class="menu-path">정산관리 &gt; 수수료내역 / 일별수수료 / 가맹점정산내역 / 크립토정산</div>
    <ul>
      <li><strong>수수료내역·일별수수료</strong>: 거래·일자 단위 수수료를 확인합니다. 채널(FIAT|CRYPTO)이 있으면 행을 구분합니다.</li>
      <li><strong>가맹점정산내역</strong>: 정산 실행 후 반영된 결과(정산일·지급·보류)를 확인합니다.</li>
      <li><strong>크립토정산</strong>(권한 시): 크립토 출금 큐·상태 조회. 가맹 수취 지갑 변경은 상위만 가능합니다.</li>
      <li>수수료율·정산주기는 상위 정책과 가맹 설정을 따릅니다.</li>
    </ul>
    <div class="info-box">금액이 예상과 다르면 결제내역 승인액·상태와 대조한 뒤, 정산일·금액을 상위에 전달하십시오.</div>
    <hr class="section-rule">

    <h2 class="section-title" id="s7">7. 가맹점 API·챗봇·크립토(해당 시)</h2>
    <div class="menu-path">업체관리 &gt; 가맹점API · 챗봇관리 · 크립토설정(조회)</div>
    <ul>
      <li><strong>가맹점API</strong>: 연동 키·문서(권한 시). 카드·크립토 모두 ICOPAY 통합 checkout(<code>prepare</code>, 크립토는 <code>payMeans=CRYPTO</code>)을 사용합니다. 크립토 Callback은 카드용 NOTI URL을 재사용합니다.</li>
      <li><strong>챗봇관리</strong>: 상품·주문 관리 후 챗봇결제 URL을 공유합니다. 상세는 「챗봇결제 가맹점 사용 메뉴얼」을 참고하십시오.</li>
      <li><strong>크립토</strong>: 가맹 로그인은 수취 주소 조회·QR만. 사용·자산·주소 변경은 본사·총판. API 상세는 상위 「가맹 API 출시」·merchant-api-samples 크립토 문서(5개국어).</li>
    </ul>
    <hr class="section-rule">

    <h2 class="section-title" id="s8">8. 서버 웹훅 보안(HMAC)·통보</h2>
    <div class="menu-path">업체관리 &gt; 가맹 상세 &gt; 웹훅 서명 · 가맹점API</div>
    <p>결제 확정 <strong>서버 통보</strong>는 주로 <strong>NOTI → 가맹 callbackUrl</strong> 로 POST 됩니다. (브라우저 Result URL과 별개)</p>
    <table>
      <tr><th>항목</th><th>내용</th></tr>
      <tr><td>설정</td><td>가맹 상세 → <strong>웹훅 서명</strong>: 본사설정 따름 / 활성 / 비활성. 활성 후 <strong>시크릿 재발급</strong>(평문 1회) → 가맹 서버에만 보관</td></tr>
      <tr><td>헤더</td><td><code>X-Icopay-Signature: v1=&#123;hex&#125;</code></td></tr>
      <tr><td>검증</td><td>HMAC-SHA256(<strong>raw body</strong>, secret). 실패 시 HTTP <strong>401</strong> 권장 · 성공 <strong>200</strong></td></tr>
      <tr><td>문서·샘플</td><td>가맹점API 「서버 웹훅 보안(HMAC) — NOTI」 · <code>merchant-api-samples/docs/webhook-hmac-noti.*.md</code> · PHP <code>notify_webhook.php</code></td></tr>
    </table>
    <div class="info-box">본사 기본이 OFF여도 <strong>가맹만 활성</strong>하면 서명됩니다. 저장·재발급 시 ICOPAY가 NOTI에 설정을 동기합니다.</div>
    <div class="warn-box">시크릿은 재발급 직후 1회만 평문으로 보입니다. 채팅·로그에 남기지 마십시오. IP allowlist만으로 검증하지 말고 <strong>HMAC을 주 검증</strong>으로 쓰십시오.</div>
    <div class="check-box">상세 연동(규격·샘플·IP·체크리스트): 같은 「운영 메뉴얼」목록의 <strong>서버 웹훅(HMAC) API 연동 메뉴얼</strong>을 참고하십시오.</div>
    <hr class="section-rule">

    <h2 class="section-title" id="s9">9. 일상 체크리스트</h2>
    <div class="check-box">
      <ol>
        <li>공지사항 확인</li>
        <li>결제 URL 유효·복사·테스트 오픈</li>
        <li>당일/전일 승인·실패 점검</li>
        <li>실패 급증 시 주문번호와 상위 공유</li>
        <li>수수료·정산 이상 여부</li>
        <li>챗봇 주문·상품(해당 시)</li>
        <li>웹훅 서명 사용 시: callbackUrl에서 <code>X-Icopay-Signature</code> 검증·시크릿 보관 확인</li>
      </ol>
    </div>
    <hr class="section-rule">

    <h2 class="section-title" id="s10">10. FAQ</h2>
    <div class="faq-item"><div class="faq-q">메뉴가 사이드바에 없습니다.</div><div class="faq-a">본사정책 → 접근·권한에서 닫혀 있습니다. 상위 본사·총판에 해당 메뉴 URL과 함께 권한 개방을 요청하세요.</div></div>
    <div class="faq-item"><div class="faq-q">결제 URL이 없거나 안내만 나옵니다.</div><div class="faq-a">웹결제 미사용·중지가 원인인 경우가 많습니다. 공지와 상위 담당자에게 확인하세요.</div></div>
    <div class="faq-item"><div class="faq-q">수수료·정산 금액이 예상과 다릅니다.</div><div class="faq-a">결제내역 승인액·상태와 대조한 뒤 정산일·금액을 상위에 전달하세요.</div></div>
    <div class="faq-item"><div class="faq-q">업체정보를 직접 수정하고 싶습니다.</div><div class="faq-a">내 업체정보는 조회 전용입니다. 상호·주소 변경은 본사·총판에 요청하세요.</div></div>
    <div class="faq-item"><div class="faq-q">문서 한글이 ??? 로 깨지거나 버전이 예전입니다.</div><div class="faq-a">가맹점 운영 메뉴얼 PDF는 Chrome 인쇄(UTF-8)로 재배포됩니다(문서 V{VERSION}). 새로고침·캐시 비우기 후에도 이전이면 상위에 배포 반영을 확인하세요.</div></div>
    <div class="faq-item"><div class="faq-q">크립토 결제 URL·통합결제 URL이 없습니다.</div><div class="faq-a">상위가 크립토 사용=Y와 허용자산을 켜고, 웹결제(카드)도 ON이어야 통합결제 URL이 생깁니다. 본사·총판에 요청하세요.</div></div>
    <div class="faq-item"><div class="faq-q">크립토 API Callback을 새로 등록해야 하나요?</div><div class="faq-a">아니요. 카드용 NOTI Callback을 Comp-Id로 재사용합니다. 연동 문서는 가맹점API·merchant-api-samples 크립토 가이드를 참고하세요.</div></div>
    <div class="faq-item"><div class="faq-q">웹훅 서명은 어디서 켜나요?</div><div class="faq-a">업체관리 → 가맹 상세 → 웹훅 서명에서 활성 후 시크릿 재발급. 본사 기본 OFF여도 가맹만 ON 가능. 검증은 가맹점API HMAC 안내·샘플을 따르세요.</div></div>
""",
        }
    if lang == "en":
        return {
            "page_title": f"ICOPAY Merchant Operations Manual V{VERSION}",
            "title_html": "Merchant<br>Operations Manual",
            "subtitle": "Merchant perspective — payments, fees, payment URL, settlement, and daily ops on permitted screens",
            "meta": _meta("en"),
            "footer_extra": f"ICOPAY Merchant Operations Manual · V{VERSION}",
            "perm_rows": [
                ("Company Mgmt &gt; My Company Info", "view", "View/copy Payment / Re-pay / Chatbot / Split URLs"),
                ("Company Mgmt &gt; Notices", "view", "Read HQ/distributor announcements"),
                ("Payments &gt; Payment list", "view", "View approvals/failures/cancels/refunds"),
                ("Settlement &gt; Fees / Merchant settlement", "view", "View fees and settlement results"),
                ("Registration / access rights", "none", "HQ / Distributor only"),
            ],
            "toc": [
                ("s1", "Scope"),
                ("s2", "Before You Start"),
                ("s3", "STEP 1 — Sign-in & notices"),
                ("s4", "STEP 2 — Payment URL"),
                ("s5", "STEP 3 — Payment list"),
                ("s6", "STEP 4 — Fees & settlement"),
                ("s7", "Merchant API · Chatbot · Crypto"),
                ("s8", "Server webhook security (HMAC)"),
                ("s9", "Daily checklist"),
                ("s10", "FAQ"),
            ],
            "body": f"""
    <h2 class="section-title" id="s1">1. Scope</h2>
    <p>Day-to-day guide for screens a <strong>Merchant (MERCHANT)</strong> login can open. Menus follow HQ Policy → Access. PG names stay neutralized as <strong>ICOPAY</strong>.</p>
    <div class="info-box">Channel details (URL / Split / Subscription / Chatbot / Crypto) are covered in the matching merchant user manuals and API samples under Ops manuals.</div>
    <hr class="section-rule">
    <h2 class="section-title" id="s2">2. Before You Start</h2>
    <div class="hq-box">Request from HQ: menu permissions, web-pay ON, crypto use/assets if needed, company master data changes, fee/settlement policy.</div>
    <hr class="section-rule">
    <h2 class="section-title" id="s3">3. <span class="step-badge">STEP 1</span> Sign-in &amp; notices</h2>
    <div class="menu-path">Companies &gt; Notices</div>
    <p>Sign in → sidebar shows permitted menus only → read notices (fees, settlement, maintenance, web-pay suspend).</p>
    <hr class="section-rule">
    <h2 class="section-title" id="s4">4. <span class="step-badge">STEP 2</span> Payment URL <small style="font-size:10pt;font-weight:400;color:#888;">(View Only)</small></h2>
    <div class="menu-path">Company Management &gt; My Company Info</div>
    <pre>https://api.icopay.co.kr/checkout/{{merchantCode}}</pre>
    <div class="check-box">Copy Payment URL → share → self-test. Also: Unified URL (<code>/checkout-all/</code>) when web-pay + crypto are ON; Crypto payment URL when crypto is enabled. Re-pay / Chatbot / Split when enabled.</div>
    <div class="warn-box">If web-pay is off or suspended, the card URL may be missing. Crypto URLs appear only after HQ enables crypto use and assets.</div>
    <hr class="section-rule">
    <h2 class="section-title" id="s5">5. <span class="step-badge">STEP 3</span> Payment list <small style="font-size:10pt;font-weight:400;color:#888;">(View Only)</small></h2>
    <div class="menu-path">Payments &gt; Payment list · Crypto deposits</div>
    <ul><li>Filter by date/status/order id.</li><li>Review approvals, failures, cancels, refunds.</li><li>Crypto deposits: view pending/confirmed; HQ confirms Tx.</li><li>Escalate with order id · time · amount (plus asset/network for crypto).</li></ul>
    <hr class="section-rule">
    <h2 class="section-title" id="s6">6. <span class="step-badge">STEP 4</span> Fees &amp; settlement</h2>
    <div class="menu-path">Settlement &gt; Fee list / Daily fees / Merchant settlement / Crypto settlement</div>
    <p>FIAT|CRYPTO channel rows may appear. Cross-check unexpected figures against Payment list; include settlement date and amount when asking HQ.</p>
    <hr class="section-rule">
    <h2 class="section-title" id="s7">7. Merchant API · Chatbot · Crypto</h2>
    <p>Merchant API (if permitted) uses ICOPAY unified checkout for web pay, API inline, API redirect, chatbot, split pay, WooCommerce, and crypto (<code>payMeans=CRYPTO</code>). Crypto Callback reuses the card NOTI URL (Comp-Id). Chatbot merchants: see Chatbot Merchant Manual. Crypto API docs (5 languages): merchant-api-samples crypto-checkout-api.</p>
    <hr class="section-rule">
    <h2 class="section-title" id="s8">8. Server webhook security (HMAC)</h2>
    <div class="menu-path">Company &gt; Merchant detail &gt; Webhook signing · Merchant API</div>
    <p>Confirmed payment <strong>server notify</strong> is mainly <strong>NOTI → merchant callbackUrl</strong> (separate from browser Result).</p>
    <table>
      <tr><th>Item</th><th>Detail</th></tr>
      <tr><td>Settings</td><td>Webhook signing: Follow HQ / Active / Inactive. After Active, <strong>Rotate secret</strong> (plaintext once) — store only on the merchant server</td></tr>
      <tr><td>Header</td><td><code>X-Icopay-Signature: v1=&#123;hex&#125;</code></td></tr>
      <tr><td>Verify</td><td>HMAC-SHA256(<strong>raw body</strong>, secret). Prefer HTTP <strong>401</strong> on fail · <strong>200</strong> on OK</td></tr>
      <tr><td>Docs</td><td>Merchant API “Server webhook security (HMAC) — NOTI” · <code>webhook-hmac-noti.*.md</code> · PHP <code>notify_webhook.php</code></td></tr>
    </table>
    <div class="info-box">HQ default may be OFF; merchant-only Active still signs. ICOPAY syncs settings to NOTI on save/rotate.</div>
    <div class="warn-box">Secret is shown in plaintext only once after rotate. Prefer <strong>HMAC as primary verification</strong>, not IP allowlist alone.</div>
    <div class="check-box">Full integration guide: see <strong>Server Webhook (HMAC) API Manual</strong> in the same Ops manuals list.</div>
    <hr class="section-rule">
    <h2 class="section-title" id="s9">9. Daily checklist</h2>
    <div class="check-box"><ol><li>Notices</li><li>Payment / Unified / Crypto URL test</li><li>Today/yesterday approvals &amp; failures</li><li>Escalate spikes</li><li>Fee/settlement anomalies</li><li>Chatbot / crypto deposits if used</li><li>If webhook signing: verify <code>X-Icopay-Signature</code> on callbackUrl and secret storage</li></ol></div>
    <hr class="section-rule">
    <h2 class="section-title" id="s10">10. FAQ</h2>
    <div class="faq-item"><div class="faq-q">Menu missing.</div><div class="faq-a">Ask HQ to open Access permissions for that URL.</div></div>
    <div class="faq-item"><div class="faq-q">Payment URL missing / notice only.</div><div class="faq-a">Usually web-pay off or suspended — check notices and HQ.</div></div>
    <div class="faq-item"><div class="faq-q">No crypto / unified URL.</div><div class="faq-a">Ask HQ to enable crypto use + assets (and web-pay for unified URL).</div></div>
    <div class="faq-item"><div class="faq-q">Need a new crypto Callback URL?</div><div class="faq-a">No — reuse the card NOTI Callback by Comp-Id. See crypto API guide in Merchant API samples.</div></div>
    <div class="faq-item"><div class="faq-q">Korean text shows as ??? or old version.</div><div class="faq-a">Merchant Ops PDF is rebuilt with Chrome print (UTF-8), document V{VERSION}. Hard-refresh or confirm deployment.</div></div>
    <div class="faq-item"><div class="faq-q">Where do I enable webhook signing?</div><div class="faq-a">Company → merchant detail → Webhook signing → Active + rotate secret. Merchant ON works even if HQ default is OFF. Follow Merchant API HMAC guide.</div></div>
""",
        }
    return _ops_loc(lang)


def _ops_loc(lang: str) -> dict:
    en = ops_doc("en")
    titles = {
        "ja": ("ICOPAY 加盟店 運営マニュアル", "加盟店<br>運営マニュアル", "加盟店視点 — 決済・手数料・決済URL・精算・日常運用"),
        "zh": ("ICOPAY 商户运营手册", "商户<br>运营手册", "商户视角 — 支付、手续费、支付 URL、结算与日常运营"),
        "th": ("ICOPAY คู่มือปฏิบัติการร้านค้า", "คู่มือ<br>ปฏิบัติการร้านค้า", "มุมมองร้าน — การชำระ ค่าธรรมเนียม URL ชำระ เคลียร์ริ่ง และงานประจำวัน"),
    }
    page, title, sub = titles[lang]
    toc = {
        "ja": [("s1", "対象範囲"), ("s2", "開始前確認"), ("s3", "STEP 1 — ログイン・告知"), ("s4", "STEP 2 — 決済URL"), ("s5", "STEP 3 — 決済一覧"), ("s6", "STEP 4 — 手数料・精算"), ("s7", "API・チャットボット"), ("s8", "サーバーWebhook署名(HMAC)"), ("s9", "チェックリスト"), ("s10", "FAQ")],
        "zh": [("s1", "适用范围"), ("s2", "开始前确认"), ("s3", "STEP 1 — 登录与公告"), ("s4", "STEP 2 — 支付 URL"), ("s5", "STEP 3 — 支付明细"), ("s6", "STEP 4 — 手续费与结算"), ("s7", "API与聊天机器人"), ("s8", "服务器 Webhook 签名(HMAC)"), ("s9", "日常清单"), ("s10", "FAQ")],
        "th": [("s1", "ขอบเขต"), ("s2", "ก่อนเริ่มใช้"), ("s3", "STEP 1 — เข้าสู่ระบบ/ประกาศ"), ("s4", "STEP 2 — Payment URL"), ("s5", "STEP 3 — รายการชำระ"), ("s6", "STEP 4 — ค่าธรรมเนียม/เคลียร์"), ("s7", "API/แชทบอท"), ("s8", "ความปลอดภัย Webhook (HMAC)"), ("s9", "เช็คลิสต์"), ("s10", "FAQ")],
    }
    perm = {
        "ja": [
            ("業者管理 &gt; 自社情報", "view", "決済URL等の確認・コピー"),
            ("業者管理 &gt; お知らせ", "view", "本社・総代理の告知"),
            ("決済管理 &gt; 決済一覧", "view", "承認・失敗等の参照"),
            ("精算管理", "view", "手数料・精算結果"),
            ("業者登録等", "none", "本社・総代理専用"),
        ],
        "zh": [
            ("企业管理 &gt; 我的企业信息", "view", "查看/复制支付 URL 等"),
            ("企业管理 &gt; 公告", "view", "查看总部/总代理公告"),
            ("支付管理 &gt; 支付明细", "view", "查看成功/失败等"),
            ("结算管理", "view", "手续费与结算结果"),
            ("企业注册等", "none", "总部/总代理专用"),
        ],
        "th": [
            ("จัดการร้าน &gt; ข้อมูลร้าน", "view", "ดู/คัดลอก Payment URL"),
            ("จัดการร้าน &gt; ประกาศ", "view", "อ่านประกาศ"),
            ("การชำระ &gt; รายการชำระ", "view", "ดูอนุมัติ/ล้มเหลว"),
            ("เคลียร์ริ่ง", "view", "ค่าธรรมเนียม/ผลเคลียร์"),
            ("ลงทะเบียนร้าน", "none", "เฉพาะ HQ"),
        ],
    }
    s7 = {
        "ja": "加盟店API（権限がある場合）は、ウェブ決済・APIインライン・APIリダイレクト・チャットボット・分割・WooCommerce のいずれも ICOPAY 統合 checkout（prepare/session/status）と同じ決済通知を使います。チャットボットはチャットボット加盟店マニュアルを参照してください。",
        "zh": "商户 API（有权限时）的网页支付、API 内联、API 重定向、聊天机器人、分期与 WooCommerce 均使用 ICOPAY 统一 checkout（prepare/session/status）及相同的支付通知。聊天机器人请参阅聊天机器人商户手册。",
        "th": "API ร้าน (เมื่อได้รับสิทธิ์) ใช้ checkout รวมของ ICOPAY สำหรับจ่ายเว็บ, API อินไลน์, API รีไดเรกต์, แชทบอท, แบ่งจ่าย และ WooCommerce พร้อมการแจ้งชำระแบบเดียวกัน ร้านที่ใช้แชทบอทดูคู่มือแชทบอท",
    }
    en_s7 = (
        "Merchant API (if permitted) uses ICOPAY unified checkout for web pay, API inline, "
        "API redirect, chatbot, split pay, WooCommerce, and crypto (<code>payMeans=CRYPTO</code>). "
        "Crypto Callback reuses the card NOTI URL (Comp-Id). Chatbot merchants: see Chatbot Merchant Manual. "
        "Crypto API docs (5 languages): merchant-api-samples crypto-checkout-api."
    )
    s8_title = {
        "ja": "8. サーバーWebhook署名(HMAC)",
        "zh": "8. 服务器 Webhook 签名(HMAC)",
        "th": "8. ความปลอดภัย Webhook (HMAC)",
    }
    s8_body = {
        "ja": """<div class="menu-path">業者管理 &gt; 加盟詳細 &gt; Webhook署名 · 加盟店API</div>
    <p>確定通知は主に <strong>NOTI → 加盟 callbackUrl</strong> です（ブラウザ Result とは別）。</p>
    <table>
      <tr><th>項目</th><th>内容</th></tr>
      <tr><td>設定</td><td>Webhook署名: 本社に従う / 有効 / 無効。有効後に<strong>シークレット再発行</strong>（平文1回）→ 加盟サーバーのみ保管</td></tr>
      <tr><td>ヘッダ</td><td><code>X-Icopay-Signature: v1=&#123;hex&#125;</code></td></tr>
      <tr><td>検証</td><td>HMAC-SHA256(<strong>raw body</strong>, secret)。失敗は HTTP <strong>401</strong> 推奨 · 成功 <strong>200</strong></td></tr>
      <tr><td>文書</td><td>加盟店API「サーバーWebhookセキュリティ(HMAC) — NOTI」· <code>webhook-hmac-noti.*.md</code></td></tr>
    </table>
    <div class="info-box">本社既定がOFFでも加盟のみ有効で署名されます。保存/再発行でICOPAYがNOTIへ同期します。</div>
    <div class="warn-box">シークレットは再発行直後のみ平文表示。IP許可だけに頼らず<strong>HMACを主検証</strong>にしてください。</div>""",
        "zh": """<div class="menu-path">企业管理 &gt; 商户详情 &gt; Webhook 签名 · 商户 API</div>
    <p>确认通知主要由 <strong>NOTI → 商户 callbackUrl</strong> 送达（与浏览器 Result 分开）。</p>
    <table>
      <tr><th>项目</th><th>说明</th></tr>
      <tr><td>设置</td><td>Webhook 签名：跟随总部 / 启用 / 停用。启用后<strong>重新签发密钥</strong>（明文一次）→ 仅保存在商户服务器</td></tr>
      <tr><td>请求头</td><td><code>X-Icopay-Signature: v1=&#123;hex&#125;</code></td></tr>
      <tr><td>校验</td><td>HMAC-SHA256(<strong>raw body</strong>, secret)。失败建议 HTTP <strong>401</strong> · 成功 <strong>200</strong></td></tr>
      <tr><td>文档</td><td>商户 API「服务器 Webhook 安全(HMAC) — NOTI」· <code>webhook-hmac-noti.*.md</code></td></tr>
    </table>
    <div class="info-box">即使总部默认为 OFF，仅商户启用也会签名。保存/轮换时 ICOPAY 会同步到 NOTI。</div>
    <div class="warn-box">密钥仅在轮换后显示一次明文。请以 <strong>HMAC 为主校验</strong>，勿仅依赖 IP 白名单。</div>""",
        "th": """<div class="menu-path">จัดการร้าน &gt; รายละเอียดร้าน &gt; ลายเซ็น Webhook · Merchant API</div>
    <p>การแจ้งยืนยันหลักคือ <strong>NOTI → callbackUrl ของร้าน</strong> (แยกจาก Result ในเบราว์เซอร์)</p>
    <table>
      <tr><th>รายการ</th><th>รายละเอียด</th></tr>
      <tr><td>ตั้งค่า</td><td>ลายเซ็น Webhook: ตาม HQ / เปิด / ปิด หลังเปิดให้<strong>ออก secret ใหม่</strong> (ข้อความชัด 1 ครั้ง) — เก็บเฉพาะเซิร์ฟเวอร์ร้าน</td></tr>
      <tr><td>Header</td><td><code>X-Icopay-Signature: v1=&#123;hex&#125;</code></td></tr>
      <tr><td>ตรวจ</td><td>HMAC-SHA256(<strong>raw body</strong>, secret) ล้มเหลวใช้ HTTP <strong>401</strong> · สำเร็จ <strong>200</strong></td></tr>
      <tr><td>เอกสาร</td><td>Merchant API 「ความปลอดภัย Webhook (HMAC) — NOTI」 · <code>webhook-hmac-noti.*.md</code></td></tr>
    </table>
    <div class="info-box">แม้ HQ ปิดเป็นค่าเริ่มต้น ร้านเปิดอย่างเดียวก็ลงลายเซ็นได้ ICOPAY ซิงก์ไป NOTI เมื่อบันทึก/ออกใหม่</div>
    <div class="warn-box">secret แสดงชัดครั้งเดียวหลังออกใหม่ ใช้ <strong>HMAC เป็นการตรวจหลัก</strong> อย่าพึ่ง IP allowlist อย่างเดียว</div>""",
    }
    en_s8_marker = '<h2 class="section-title" id="s8">8. Server webhook security (HMAC)</h2>'
    en_s8_end = '<hr class="section-rule">\n    <h2 class="section-title" id="s9">9. Daily checklist</h2>'
    body = en["body"].replace(en_s7, s7[lang])
    i0 = body.find(en_s8_marker)
    i1 = body.find(en_s8_end)
    if i0 >= 0 and i1 > i0:
        body = (
            body[:i0]
            + f'<h2 class="section-title" id="s8">{s8_title[lang]}</h2>\n    '
            + s8_body[lang]
            + "\n    "
            + body[i1:]
        )
    return {
        "page_title": f"{page} V{VERSION}",
        "title_html": title,
        "subtitle": sub,
        "meta": _meta(lang),
        "footer_extra": f"ICOPAY Merchant Ops · V{VERSION}",
        "perm_rows": perm[lang],
        "toc": toc[lang],
        "body": body,
    }
