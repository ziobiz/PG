# -*- coding: utf-8 -*-
"""Rich body content for merchant USER manuals (URL / split / subscribe) — chatbot-manual design."""
from __future__ import annotations

from _merchant_user_manual_theme import DATE_EN, DATE_KO, VERSION


def _meta(lang: str, target: str) -> str:
    if lang == "ko":
        return f"작성 기준: ICOPAY 가맹점 권한 기준 &nbsp;|&nbsp; 대상: {target} &nbsp;|&nbsp; 발행: {DATE_KO} &nbsp;|&nbsp; V{VERSION}"
    if lang == "en":
        return f"Based on: ICOPAY Merchant Permission Levels &nbsp;|&nbsp; Target: {target} &nbsp;|&nbsp; Published: {DATE_EN} &nbsp;|&nbsp; V{VERSION}"
    if lang == "ja":
        return f"基準: ICOPAY加盟店権限 &nbsp;|&nbsp; 対象: {target} &nbsp;|&nbsp; 発行: {DATE_EN} &nbsp;|&nbsp; V{VERSION}"
    if lang == "zh":
        return f"依据: ICOPAY 商户权限标准 &nbsp;|&nbsp; 对象: {target} &nbsp;|&nbsp; 发布: {DATE_EN} &nbsp;|&nbsp; V{VERSION}"
    return f"อ้างอิง: สิทธิ์ร้าน ICOPAY &nbsp;|&nbsp; กลุ่มเป้าหมาย: {target} &nbsp;|&nbsp; เผยแพร่: {DATE_EN} &nbsp;|&nbsp; V{VERSION}"


# ---------------------------------------------------------------------------
# URL Payment
# ---------------------------------------------------------------------------
def url_doc(lang: str) -> dict:
    if lang == "ko":
        return {
            "page_title": "ICOPAY URL결제 가맹점 사용 메뉴얼",
            "title_html": "URL결제<br>가맹점 사용 메뉴얼",
            "subtitle": "가맹점 기준 — 공개 결제 URL 공유부터 승인 확인·일상 운영까지, 가맹점이 직접 하는 모든 것",
            "meta": _meta("ko", "URL결제 사용 가맹점"),
            "footer_extra": f"ICOPAY URL결제 가맹점 메뉴얼 · V{VERSION}",
            "perm_rows": [
                ("업체관리 &gt; 내 업체정보", "view", "결제 URL·재결제 URL 확인·복사 / 설정 변경 불가"),
                ("업체관리 &gt; 공지사항", "view", "본사·총판 운영 공지 확인"),
                ("결제관리 &gt; 결제내역 (URL결제내역)", "view", "승인·실패·취소·환불 조회 / 임의 수정 불가"),
                ("업체등록·접근권한 등", "none", "본사·총판 전용"),
            ],
            "toc": [
                ("s1", "URL결제란?"),
                ("s2", "시작 전 확인 사항 (본사·상위 활성화)"),
                ("s3", "STEP 1 — 내 결제 URL 확인 (조회 전용)"),
                ("s4", "STEP 2 — 고객에게 URL 배포"),
                ("s5", "STEP 3 — 결제내역 확인 (조회 전용)"),
                ("s6", "재결제 URL 사용 시 주의"),
                ("s7", "일상 체크리스트"),
                ("s8", "자주 묻는 질문 (FAQ)"),
            ],
            "body": f"""
    <h2 class="section-title" id="s1">1. URL결제란?</h2>
    <p>ICOPAY URL결제는 가맹점이 <strong>공개 결제 링크 하나</strong>를 고객에게 보내면, 고객이 브라우저에서 금액·상품·카드 정보를 입력해 결제하는 방식입니다. 별도 쇼핑몰·앱 개발 없이 SMS·메신저·메일·QR로 즉시 수금할 수 있습니다.</p>
    <h3 class="sub-title">고객 결제 흐름</h3>
    <div class="flow">
      <div class="flow-row"><span class="flow-actor">가맹점</span><span class="flow-arrow">→</span><span class="flow-desc">내 업체정보에서 결제 URL 복사 후 고객에게 전달</span></div>
      <div class="flow-row"><span class="flow-actor">고객</span><span class="flow-arrow">→</span><span class="flow-desc">링크 클릭 → ICOPAY 결제창에서 정보 입력·카드 결제</span></div>
      <div class="flow-row"><span class="flow-actor">ICOPAY</span><span class="flow-arrow">→</span><span class="flow-desc">승인 처리 (결제대행사는 UI에 ICOPAY로 중립 표시)</span></div>
      <div class="flow-row"><span class="flow-actor">가맹점</span><span class="flow-arrow">→</span><span class="flow-desc">결제관리 → 결제내역에서 승인·실패 확인</span></div>
    </div>
    <h3 class="sub-title">가맹점이 직접 하는 일 vs 본사·총판</h3>
    <table>
      <tr><th>가맹점 직접 가능</th><th>본사·총판이 설정</th></tr>
      <tr><td>결제 URL·재결제 URL 확인·복사</td><td>웹결제(URL결제) 사용/중지</td></tr>
      <tr><td>고객에게 링크 공유</td><td>운영 PG·입력 모드·탭/브랜드 설정</td></tr>
      <tr><td>결제내역 조회</td><td>메뉴 권한·수수료·정산 정책</td></tr>
      <tr><td>공지 확인·일상 점검</td><td>업체 기본정보 변경</td></tr>
    </table>
    <hr class="section-rule">

    <h2 class="section-title" id="s2">2. 시작 전 확인 사항 (본사·상위 활성화)</h2>
    <p>URL결제는 <strong>상위(본사·총판)에서 웹결제(URL결제)가 사용</strong>으로 열려 있어야 합니다.</p>
    <table>
      <tr><th>확인 항목</th><th>확인 방법</th></tr>
      <tr><td>사이드바에 <strong>결제관리</strong> 메뉴가 보이는가?</td><td>보이면 권한 개방. 없으면 상위에 권한 요청</td></tr>
      <tr><td>내 업체정보에 <strong>결제 URL</strong>이 표시되는가?</td><td>URL이 있으면 사용 가능. 없으면 웹결제 사용여부·중지를 상위 확인</td></tr>
    </table>
    <div class="hq-box">
      <strong>본사·총판에 요청해야 하는 사항</strong><br>
      • 웹결제(URL결제) 사용 ON / 일괄 중지 해제<br>
      • 결제내역·URL결제내역 메뉴 권한<br>
      • 재결제 URL 기능 ON (해당 PG·연동일 때만)<br>
      • 업체 상호·주소 등 기본정보 변경
    </div>
    <hr class="section-rule">

    <h2 class="section-title" id="s3">3. <span class="step-badge">STEP 1</span> 내 결제 URL 확인 <small style="font-size:10pt;font-weight:400;color:#888;">(조회 전용)</small></h2>
    <div class="menu-path">업체관리 &gt; 내 업체정보</div>
    <div class="info-box">이 화면은 <strong>조회 전용</strong>입니다. URL을 확인하고 <strong>복사</strong>할 수 있지만, 업체 설정을 직접 바꿀 수는 없습니다.</div>
    <h3 class="sub-title">3-1. 결제 URL</h3>
    <p>웹결제가 활성화되면 아래 형식의 공개 결제 주소가 표시됩니다.</p>
    <pre>https://api.icopay.co.kr/checkout/{{업체코드}}</pre>
    <table>
      <tr><th>동작</th><th>방법</th></tr>
      <tr><td>URL 복사</td><td>결제 URL 옆 <strong>복사</strong> → 클립보드 저장</td></tr>
      <tr><td>테스트 오픈</td><td>본인 브라우저에서 붙여넣어 결제창이 열리는지 확인</td></tr>
    </table>
    <div class="warn-box">웹결제가 미사용이거나 상위 일괄 중지 상태이면 URL이 없거나, 열어도 안내만 표시될 수 있습니다. 상위 담당자에게 확인하세요.</div>
    <h3 class="sub-title">3-2. URL 재결제 URL (기능 ON일 때만)</h3>
    <p>저장 카드 재결제용 주소입니다. 신규 고객용 일반 결제 URL과 <strong>혼용하지 마세요</strong>.</p>
    <hr class="section-rule">

    <h2 class="section-title" id="s4">4. <span class="step-badge">STEP 2</span> 고객에게 URL 배포</h2>
    <div class="check-box">가맹점이 <strong>직접</strong> 수행하는 핵심 업무입니다.</div>
    <table>
      <tr><th>채널</th><th>활용</th></tr>
      <tr><td>카카오톡 · LINE · SMS</td><td>메시지에 링크 첨부. 필요 시 금액·상품명을 함께 기재</td></tr>
      <tr><td>이메일 · 견적서</td><td>청구 안내 메일·PDF에 URL 삽입</td></tr>
      <tr><td>자사몰 · 블로그</td><td>버튼·배너에 링크 연결</td></tr>
      <tr><td>QR (자체 제작)</td><td>복사한 URL로 QR을 만들어 매장·명함에 활용</td></tr>
    </table>
    <div class="info-box">결제창의 금액·상품명 표시 방식은 본사정책/가맹 설정을 따릅니다. 직원이 임의로 결제대행·수수료를 고객에게 안내하지 마세요. 구매자 화면의 결제기관명은 <strong>ICOPAY</strong>로 표시됩니다.</div>
    <hr class="section-rule">

    <h2 class="section-title" id="s5">5. <span class="step-badge">STEP 3</span> 결제내역 확인 <small style="font-size:10pt;font-weight:400;color:#888;">(조회 전용)</small></h2>
    <div class="menu-path">결제관리 &gt; 결제내역 (권한이 있으면 URL결제내역)</div>
    <ul>
      <li>기간·상태·키워드(주문번호)로 검색합니다.</li>
      <li>승인·실패·취소·환불을 구분합니다.</li>
      <li>고객 문의 시 <strong>주문번호·결제시각·금액</strong>을 함께 적어두면 상위 대응이 빨라집니다.</li>
    </ul>
    <div class="warn-box">가맹점 화면에서 승인 건을 임의로 삭제·조작할 수 없습니다. 취소·환불이 필요하면 상위 정책·담당자 안내를 따르세요.</div>
    <hr class="section-rule">

    <h2 class="section-title" id="s6">6. 재결제 URL 사용 시 주의</h2>
    <ul>
      <li>신규·일반 고객 → <strong>결제 URL</strong></li>
      <li>저장 카드가 있는 재결제 고객 → <strong>재결제 URL</strong> (표시될 때만)</li>
      <li>재결제 URL을 SNS에 공개하거나 불특정 다수에게 보내지 마세요.</li>
    </ul>
    <hr class="section-rule">

    <h2 class="section-title" id="s7">7. 일상 체크리스트</h2>
    <div class="check-box">
      <ol>
        <li>공지사항 확인 (수수료·점검·웹결제 중지)</li>
        <li>결제 URL 복사 · 본인 브라우저 테스트 오픈</li>
        <li>당일/전일 승인·실패 건수 점검</li>
        <li>실패 급증 시 주문번호와 함께 상위 공유</li>
        <li>재결제 URL 오남용 여부 점검</li>
      </ol>
    </div>
    <hr class="section-rule">

    <h2 class="section-title" id="s8">8. 자주 묻는 질문 (FAQ)</h2>
    <div class="faq-item"><div class="faq-q">결제 URL이 안 보여요.</div><div class="faq-a">웹결제 미사용이거나 상위 중지 상태입니다. 본사·총판에 사용 ON을 요청하세요.</div></div>
    <div class="faq-item"><div class="faq-q">링크를 열면 안내만 나옵니다.</div><div class="faq-a">웹결제 중지·점검·권한 문제일 수 있습니다. 공지와 상위 담당자에게 확인하세요.</div></div>
    <div class="faq-item"><div class="faq-q">결제가 실패했습니다.</div><div class="faq-a">결제내역의 실패 사유를 확인하세요. 필요 시 주문번호와 함께 상위에 「리스크·거래」 문의를 하세요.</div></div>
    <div class="faq-item"><div class="faq-q">메뉴가 사이드바에 없습니다.</div><div class="faq-a">본사정책 → 접근·권한에서 메뉴가 닫혀 있습니다. 상위에 권한 개방을 요청하세요.</div></div>
    <div class="faq-item"><div class="faq-q">업체정보를 직접 수정하고 싶습니다.</div><div class="faq-a">내 업체정보는 조회 전용입니다. 상호·주소 변경은 본사·총판에 요청하세요.</div></div>
    <div class="faq-item"><div class="faq-q">API 연동이 필요합니다.</div><div class="faq-a">이 메뉴얼은 사용자 운영 안내입니다. 연동은 상위 「가맹 API 출시」 문서를 요청하세요.</div></div>
""",
        }
    if lang == "en":
        return {
            "page_title": "ICOPAY URL Payment – Merchant Manual",
            "title_html": "URL Payment<br>Merchant Manual",
            "subtitle": "Merchant perspective — share public payment URLs, verify approvals, and run daily operations",
            "meta": _meta("en", "URL Payment Merchants"),
            "footer_extra": f"ICOPAY URL Payment Merchant Manual · V{VERSION}",
            "perm_rows": [
                ("Company Mgmt &gt; My Company Info", "view", "View/copy Payment URL & Re-pay URL — cannot edit settings"),
                ("Company Mgmt &gt; Notices", "view", "Read HQ/distributor announcements"),
                ("Payments &gt; Payment list (URL list)", "view", "View approvals/failures/cancels/refunds — no arbitrary edits"),
                ("Company registration / access rights", "none", "HQ / Distributor only"),
            ],
            "toc": [
                ("s1", "What is URL Payment?"),
                ("s2", "Before You Start (HQ activation)"),
                ("s3", "STEP 1 — View your Payment URL (View Only)"),
                ("s4", "STEP 2 — Share the URL with customers"),
                ("s5", "STEP 3 — Review payment history (View Only)"),
                ("s6", "Using Re-pay URL safely"),
                ("s7", "Daily checklist"),
                ("s8", "FAQ"),
            ],
            "body": f"""
    <h2 class="section-title" id="s1">1. What is URL Payment?</h2>
    <p>ICOPAY URL Payment lets merchants send <strong>one public payment link</strong>. Customers open it in a browser, enter amount/product/card details, and pay — without building a separate shop or app.</p>
    <h3 class="sub-title">Customer flow</h3>
    <div class="flow">
      <div class="flow-row"><span class="flow-actor">Merchant</span><span class="flow-arrow">→</span><span class="flow-desc">Copy Payment URL from My Company Info and share it</span></div>
      <div class="flow-row"><span class="flow-actor">Customer</span><span class="flow-arrow">→</span><span class="flow-desc">Opens link → pays on the ICOPAY checkout page</span></div>
      <div class="flow-row"><span class="flow-actor">ICOPAY</span><span class="flow-arrow">→</span><span class="flow-desc">Processes approval (PG brand stays neutralized as ICOPAY on UI)</span></div>
      <div class="flow-row"><span class="flow-actor">Merchant</span><span class="flow-arrow">→</span><span class="flow-desc">Checks result under Payments → Payment list</span></div>
    </div>
    <h3 class="sub-title">Merchant vs HQ</h3>
    <table>
      <tr><th>Merchant can do</th><th>HQ / Distributor sets</th></tr>
      <tr><td>Copy Payment / Re-pay URL</td><td>Web-pay (URL pay) on/off or suspend</td></tr>
      <tr><td>Share links with customers</td><td>Operating PG, input mode, branding</td></tr>
      <tr><td>View payment history</td><td>Menu permissions, fees, settlement</td></tr>
      <tr><td>Read notices & daily checks</td><td>Company master data changes</td></tr>
    </table>
    <hr class="section-rule">
    <h2 class="section-title" id="s2">2. Before You Start (HQ activation)</h2>
    <table>
      <tr><th>Check</th><th>How</th></tr>
      <tr><td>Is <strong>Payments</strong> visible in the sidebar?</td><td>If not, request menu permission</td></tr>
      <tr><td>Is <strong>Payment URL</strong> shown in My Company Info?</td><td>If missing, ask HQ to enable web-pay</td></tr>
    </table>
    <div class="hq-box"><strong>Request from HQ</strong><br>• Web-pay ON / clear suspend<br>• Payment list permissions<br>• Re-pay URL (when PG supports it)<br>• Company name/address changes</div>
    <hr class="section-rule">
    <h2 class="section-title" id="s3">3. <span class="step-badge">STEP 1</span> View your Payment URL <small style="font-size:10pt;font-weight:400;color:#888;">(View Only)</small></h2>
    <div class="menu-path">Company Management &gt; My Company Info</div>
    <div class="info-box">View-only screen — you can <strong>copy</strong> URLs but cannot edit company settings.</div>
    <pre>https://api.icopay.co.kr/checkout/{{merchantCode}}</pre>
    <div class="warn-box">If web-pay is off or suspended, the URL may be missing or show a notice page only.</div>
    <hr class="section-rule">
    <h2 class="section-title" id="s4">4. <span class="step-badge">STEP 2</span> Share the URL with customers</h2>
    <div class="check-box">Core merchant-operated task.</div>
    <table>
      <tr><th>Channel</th><th>Tips</th></tr>
      <tr><td>Messenger / SMS</td><td>Paste the link; add amount/product text if needed</td></tr>
      <tr><td>Email / invoice</td><td>Embed the URL in billing mail</td></tr>
      <tr><td>Website / blog</td><td>Button or banner link</td></tr>
      <tr><td>QR</td><td>Generate a QR from the copied URL for store use</td></tr>
    </table>
    <div class="info-box">Checkout display follows HQ/merchant policy. Buyer UI shows the provider as <strong>ICOPAY</strong>.</div>
    <hr class="section-rule">
    <h2 class="section-title" id="s5">5. <span class="step-badge">STEP 3</span> Review payment history <small style="font-size:10pt;font-weight:400;color:#888;">(View Only)</small></h2>
    <div class="menu-path">Payments &gt; Payment list</div>
    <ul>
      <li>Filter by date, status, order number.</li>
      <li>Separate approvals, failures, cancels, refunds.</li>
      <li>For support, note <strong>order id · time · amount</strong>.</li>
    </ul>
    <hr class="section-rule">
    <h2 class="section-title" id="s6">6. Using Re-pay URL safely</h2>
    <ul>
      <li>New customers → Payment URL</li>
      <li>Saved-card return customers → Re-pay URL (when shown)</li>
      <li>Do not post Re-pay URL publicly</li>
    </ul>
    <hr class="section-rule">
    <h2 class="section-title" id="s7">7. Daily checklist</h2>
    <div class="check-box"><ol><li>Read notices</li><li>Copy &amp; self-test Payment URL</li><li>Review today/yesterday success &amp; fail</li><li>Escalate spikes with order ids</li><li>Prevent Re-pay URL misuse</li></ol></div>
    <hr class="section-rule">
    <h2 class="section-title" id="s8">8. FAQ</h2>
    <div class="faq-item"><div class="faq-q">Payment URL is missing.</div><div class="faq-a">Ask HQ to enable web-pay / clear suspend.</div></div>
    <div class="faq-item"><div class="faq-q">Link opens a notice only.</div><div class="faq-a">Often suspend/maintenance — check notices and HQ.</div></div>
    <div class="faq-item"><div class="faq-q">Payment failed.</div><div class="faq-a">Check fail reason in the list; escalate with order id if needed.</div></div>
    <div class="faq-item"><div class="faq-q">Menu not in sidebar.</div><div class="faq-a">Request Access permissions from HQ/distributor.</div></div>
    <div class="faq-item"><div class="faq-q">Need API integration.</div><div class="faq-a">This is an ops manual — ask HQ for Merchant API launch docs.</div></div>
""",
        }
    # ja / zh / th — structured translations
    return _url_loc(lang)


def _url_loc(lang: str) -> dict:
    titles = {
        "ja": ("ICOPAY URL決済 加盟店マニュアル", "URL決済<br>加盟店マニュアル", "加盟店視点 — 公開決済URL共有から承認確認・日常運用まで", "URL決済利用加盟店"),
        "zh": ("ICOPAY URL支付商户手册", "URL支付<br>商户使用手册", "商户视角 — 从分享公开支付 URL 到核对成功与日常运营", "URL支付商户"),
        "th": ("ICOPAY คู่มือร้านชำระด้วย URL", "ชำระด้วย URL<br>คู่มือร้านค้า", "มุมมองร้าน — แชร์ URL สาธารณะ ตรวจอนุมัติ และงานประจำวัน", "ร้านที่ใช้ชำระ URL"),
    }
    page, title, sub, target = titles[lang]
    # Reuse EN structure with localized key strings via compact body
    en = url_doc("en")
    bodies = {
        "ja": en["body"]
        .replace("What is URL Payment?", "URL決済とは？")
        .replace("Before You Start (HQ activation)", "開始前の確認（本社・上位の有効化）")
        .replace("STEP 1</span> View your Payment URL", "STEP 1</span> 決済URLの確認")
        .replace("STEP 2</span> Share the URL with customers", "STEP 2</span> 顧客へのURL配布")
        .replace("STEP 3</span> Review payment history", "STEP 3</span> 決済履歴の確認")
        .replace("Using Re-pay URL safely", "再決済URL利用時の注意")
        .replace("Daily checklist", "日常チェックリスト")
        .replace(">FAQ<", ">よくある質問 (FAQ)<"),
        "zh": en["body"]
        .replace("What is URL Payment?", "什么是 URL 支付？")
        .replace("Before You Start (HQ activation)", "开始前确认（需总部启用）")
        .replace("STEP 1</span> View your Payment URL", "STEP 1</span> 查看支付 URL")
        .replace("STEP 2</span> Share the URL with customers", "STEP 2</span> 向客户分享 URL")
        .replace("STEP 3</span> Review payment history", "STEP 3</span> 查看支付明细")
        .replace("Using Re-pay URL safely", "再支付 URL 使用注意")
        .replace("Daily checklist", "日常清单")
        .replace(">FAQ<", ">常见问题 (FAQ)<"),
        "th": en["body"]
        .replace("What is URL Payment?", "ชำระด้วย URL คืออะไร?")
        .replace("Before You Start (HQ activation)", "ก่อนเริ่มใช้ (ต้องเปิดโดย HQ)")
        .replace("STEP 1</span> View your Payment URL", "STEP 1</span> ดู Payment URL")
        .replace("STEP 2</span> Share the URL with customers", "STEP 2</span> แชร์ URL ให้ลูกค้า")
        .replace("STEP 3</span> Review payment history", "STEP 3</span> ตรวจรายการชำระ")
        .replace("Using Re-pay URL safely", "ใช้ Re-pay URL อย่างระวัง")
        .replace("Daily checklist", "เช็คลิสต์ประจำวัน")
        .replace(">FAQ<", ">คำถามที่พบบ่อย (FAQ)<"),
    }
    toc_map = {
        "ja": [("s1", "URL決済とは？"), ("s2", "開始前の確認"), ("s3", "STEP 1 — 決済URL確認"), ("s4", "STEP 2 — URL配布"), ("s5", "STEP 3 — 決済履歴"), ("s6", "再決済URL注意"), ("s7", "チェックリスト"), ("s8", "FAQ")],
        "zh": [("s1", "什么是 URL 支付？"), ("s2", "开始前确认"), ("s3", "STEP 1 — 查看支付 URL"), ("s4", "STEP 2 — 分享 URL"), ("s5", "STEP 3 — 支付明细"), ("s6", "再支付 URL 注意"), ("s7", "日常清单"), ("s8", "FAQ")],
        "th": [("s1", "ชำระ URL คืออะไร?"), ("s2", "ก่อนเริ่มใช้"), ("s3", "STEP 1 — ดู Payment URL"), ("s4", "STEP 2 — แชร์ URL"), ("s5", "STEP 3 — รายการชำระ"), ("s6", "Re-pay URL"), ("s7", "เช็คลิสต์"), ("s8", "FAQ")],
    }
    perm = {
        "ja": [
            ("業者管理 &gt; 自社情報", "view", "決済URL・再決済URLの確認・コピー / 設定変更不可"),
            ("業者管理 &gt; お知らせ", "view", "本社・総代理の告知確認"),
            ("決済管理 &gt; 決済一覧", "view", "承認・失敗・取消・返金の参照"),
            ("業者登録・権限など", "none", "本社・総代理専用"),
        ],
        "zh": [
            ("企业管理 &gt; 我的企业信息", "view", "查看/复制支付 URL、再支付 URL — 不可改设置"),
            ("企业管理 &gt; 公告", "view", "查看总部/总代理公告"),
            ("支付管理 &gt; 支付明细", "view", "查看成功/失败/取消/退款"),
            ("企业注册·权限等", "none", "总部/总代理专用"),
        ],
        "th": [
            ("จัดการร้าน &gt; ข้อมูลร้าน", "view", "ดู/คัดลอก Payment URL — แก้การตั้งค่าไม่ได้"),
            ("จัดการร้าน &gt; ประกาศ", "view", "อ่านประกาศ HQ/ตัวแทน"),
            ("การชำระ &gt; รายการชำระ", "view", "ดูอนุมัติ/ล้มเหลว/ยกเลิก/คืนเงิน"),
            ("ลงทะเบียนร้าน / สิทธิ์", "none", "เฉพาะ HQ / ตัวแทน"),
        ],
    }
    return {
        "page_title": page,
        "title_html": title,
        "subtitle": sub,
        "meta": _meta(lang, target),
        "footer_extra": f"ICOPAY URL · V{VERSION}",
        "perm_rows": perm[lang],
        "toc": toc_map[lang],
        "body": bodies[lang],
    }


# ---------------------------------------------------------------------------
# Split
# ---------------------------------------------------------------------------
def split_doc(lang: str) -> dict:
    if lang == "ko":
        return {
            "page_title": "ICOPAY 분할결제 가맹점 사용 메뉴얼",
            "title_html": "분할결제<br>가맹점 사용 메뉴얼",
            "subtitle": "가맹점 기준 — 분할 신청 URL 공유부터 계약·회차·미납 확인까지",
            "meta": _meta("ko", "분할결제 사용 가맹점"),
            "footer_extra": f"ICOPAY 분할결제 가맹점 메뉴얼 · V{VERSION}",
            "perm_rows": [
                ("업체관리 &gt; 내 업체정보", "view", "분할결제 URL 확인·복사 / 설정 변경 불가"),
                ("분할관리", "view", "계약·회차·미납 조회 (권한에 따라 상이)"),
                ("결제관리 &gt; 분할결제내역·결제내역", "view", "회차 결제 승인·실패 확인"),
                ("업체등록·권한설정 등", "none", "본사·총판 전용"),
            ],
            "toc": [
                ("s1", "분할결제란?"),
                ("s2", "시작 전 확인 (분할결제 사용 ON)"),
                ("s3", "STEP 1 — 분할결제 URL 확인"),
                ("s4", "STEP 2 — 고객에게 링크 배포"),
                ("s5", "STEP 3 — 분할관리·회차 확인"),
                ("s6", "결제내역과의 관계"),
                ("s7", "일상 체크리스트"),
                ("s8", "FAQ"),
            ],
            "body": f"""
    <h2 class="section-title" id="s1">1. 분할결제란?</h2>
    <p>ICOPAY 분할결제는 고객이 <strong>분할 신청 페이지</strong>에서 금액·회차 등을 신청하고, 이후 <strong>회차별로 결제</strong>하는 방식입니다. 가맹점은 분할결제 URL을 공유하고 계약·회차 상태를 확인합니다.</p>
    <div class="flow">
      <div class="flow-row"><span class="flow-actor">가맹점</span><span class="flow-arrow">→</span><span class="flow-desc">분할결제 URL 복사·전달</span></div>
      <div class="flow-row"><span class="flow-actor">고객</span><span class="flow-arrow">→</span><span class="flow-desc">분할 신청 → 회차 결제</span></div>
      <div class="flow-row"><span class="flow-actor">가맹점</span><span class="flow-arrow">→</span><span class="flow-desc">분할관리·결제내역에서 상태 확인</span></div>
    </div>
    <div class="info-box">구매자·가맹 UI의 결제대행사명은 <strong>ICOPAY</strong>로 중립 표시됩니다. API·계약 스펙은 이 메뉴얼에 포함하지 않습니다.</div>
    <hr class="section-rule">
    <h2 class="section-title" id="s2">2. 시작 전 확인 (분할결제 사용 ON)</h2>
    <table>
      <tr><th>확인</th><th>방법</th></tr>
      <tr><td>가맹 <strong>분할결제 사용</strong> ON</td><td>내 업체정보에 분할결제 URL 표시 여부</td></tr>
      <tr><td>분할관리·결제내역 메뉴</td><td>사이드바 표시 여부 — 없으면 권한 요청</td></tr>
    </table>
    <div class="hq-box"><strong>상위에 요청</strong><br>• 총본사·본사·총판이 가맹 <strong>업체정보 → URL 분할결제</strong>에서 「분할결제 사용여부=사용」으로 저장하면 DB에 즉시 반영됩니다(재로그인 후에도 유지).<br>• 계약취소는 본사정책 기본(미사용) 또는 가맹별 사용/미사용으로 부여합니다.<br>• 분할관리·분할결제내역 메뉴 권한·회차·한도 정책 안내</div>
    <div class="info-box">가맹점은 내 업체정보에서 분할결제 사용여부를 <strong>직접 변경할 수 없습니다</strong>. 상위가 저장한 값이 기준입니다.</div>
    <hr class="section-rule">
    <h2 class="section-title" id="s3">3. <span class="step-badge">STEP 1</span> 분할결제 URL 확인 <small style="font-size:10pt;font-weight:400;color:#888;">(조회 전용)</small></h2>
    <div class="menu-path">업체관리 &gt; 내 업체정보</div>
    <pre>https://api.icopay.co.kr/split-pay/{{업체코드}}</pre>
    <div class="warn-box">분할결제가 OFF이면 URL이 없거나 동작하지 않습니다.</div>
    <hr class="section-rule">
    <h2 class="section-title" id="s4">4. <span class="step-badge">STEP 2</span> 고객에게 링크 배포</h2>
    <div class="check-box">메신저·SMS·견적서에 분할결제 URL을 전달합니다. 회차·금액 규칙은 화면·본사 설정을 따르며 직원이 임의로 변경하지 마세요.</div>
    <hr class="section-rule">
    <h2 class="section-title" id="s5">5. <span class="step-badge">STEP 3</span> 분할관리·회차 확인</h2>
    <div class="menu-path">분할관리</div>
    <ul>
      <li>계약 목록·회차 상태·미납을 확인합니다.</li>
      <li>고객 문의 시 <strong>계약번호·회차·납부일</strong>을 함께 확인합니다.</li>
    </ul>
    <hr class="section-rule">
    <h2 class="section-title" id="s6">6. 결제내역과의 관계</h2>
    <div class="menu-path">결제관리 &gt; 결제내역 / 분할결제내역</div>
    <p>각 회차 결제는 결제내역에도 나타납니다. 실패 시 사유를 확인하고 재시도·다른 카드 안내가 가능합니다.</p>
    <hr class="section-rule">
    <h2 class="section-title" id="s7">7. 일상 체크리스트</h2>
    <div class="check-box"><ol><li>공지 확인</li><li>분할 URL 테스트</li><li>신규 계약·미납 회차</li><li>실패 급증 시 계약/주문번호와 상위 공유</li></ol></div>
    <hr class="section-rule">
    <h2 class="section-title" id="s8">8. FAQ</h2>
    <div class="faq-item"><div class="faq-q">분할 URL/메뉴가 없습니다.</div><div class="faq-a">상위(총본사·본사·총판)에 가맹 업체정보 → URL 분할결제 「사용」저장과 메뉴 권한 개방을 요청하세요. 상위에서 저장한 사용여부는 재로그인 후에도 유지됩니다.</div></div>
    <div class="faq-item"><div class="faq-q">회차 결제가 실패합니다.</div><div class="faq-a">결제내역 실패 사유를 확인한 뒤 고객에게 재시도·카드 변경을 안내하세요.</div></div>
    <div class="faq-item"><div class="faq-q">연체·미납이 있습니다.</div><div class="faq-a">분할관리에서 계약·회차를 확인한 뒤 상위 정책에 따라 문의하세요.</div></div>
""",
        }
    if lang == "en":
        return {
            "page_title": "ICOPAY Split Payment – Merchant Manual",
            "title_html": "Split Payment<br>Merchant Manual",
            "subtitle": "Merchant perspective — share split-pay URLs, review contracts and installments",
            "meta": _meta("en", "Split Payment Merchants"),
            "footer_extra": f"ICOPAY Split Payment Merchant Manual · V{VERSION}",
            "perm_rows": [
                ("Company Mgmt &gt; My Company Info", "view", "View/copy Split-pay URL"),
                ("Split management", "view", "View contracts / installments / arrears"),
                ("Payments &gt; Split / Payment list", "view", "View installment charges"),
                ("Registration / access rights", "none", "HQ / Distributor only"),
            ],
            "toc": [
                ("s1", "What is Split Payment?"),
                ("s2", "Before You Start"),
                ("s3", "STEP 1 — Split-pay URL"),
                ("s4", "STEP 2 — Share with customers"),
                ("s5", "STEP 3 — Contracts & installments"),
                ("s6", "Relation to payment list"),
                ("s7", "Daily checklist"),
                ("s8", "FAQ"),
            ],
            "body": f"""
    <h2 class="section-title" id="s1">1. What is Split Payment?</h2>
    <p>Customers apply on a <strong>split-pay page</strong>, then pay <strong>by installment</strong>. Merchants share the URL and monitor contracts.</p>
    <div class="flow">
      <div class="flow-row"><span class="flow-actor">Merchant</span><span class="flow-arrow">→</span><span class="flow-desc">Copy &amp; share Split-pay URL</span></div>
      <div class="flow-row"><span class="flow-actor">Customer</span><span class="flow-arrow">→</span><span class="flow-desc">Apply → pay installments</span></div>
      <div class="flow-row"><span class="flow-actor">Merchant</span><span class="flow-arrow">→</span><span class="flow-desc">Review Split management &amp; payment list</span></div>
    </div>
    <div class="info-box">Buyer UI shows <strong>ICOPAY</strong>. This manual excludes API specs.</div>
    <hr class="section-rule">
    <h2 class="section-title" id="s2">2. Before You Start</h2>
    <div class="hq-box"><strong>Ask HQ / parent org</strong><br>• On merchant profile → <strong>URL Split Payment</strong>, set Split-pay = ON and Save. The value is written to DB immediately and remains after re-login.<br>• Contract-cancel follows HQ policy default (usually OFF) or a per-merchant Y/N grant.<br>• Open Split management / Split payment history menus.</div>
    <div class="info-box">Merchants <strong>cannot</strong> change Split-pay enable on My Company Info. Parent-saved value is authoritative.</div>
    <hr class="section-rule">
    <h2 class="section-title" id="s3">3. <span class="step-badge">STEP 1</span> Split-pay URL <small style="font-size:10pt;font-weight:400;color:#888;">(View Only)</small></h2>
    <div class="menu-path">Company Management &gt; My Company Info</div>
    <pre>https://api.icopay.co.kr/split-pay/{{merchantCode}}</pre>
    <div class="warn-box">If Split-pay is OFF, the URL is missing or inactive.</div>
    <hr class="section-rule">
    <h2 class="section-title" id="s4">4. <span class="step-badge">STEP 2</span> Share with customers</h2>
    <div class="check-box">Send the URL via messenger/SMS/invoice. Do not invent installment rules outside HQ policy.</div>
    <hr class="section-rule">
    <h2 class="section-title" id="s5">5. <span class="step-badge">STEP 3</span> Contracts &amp; installments</h2>
    <div class="menu-path">Split management</div>
    <ul><li>Review contracts, installment status, arrears.</li><li>Note contract id · installment · due date for support.</li></ul>
    <hr class="section-rule">
    <h2 class="section-title" id="s6">6. Relation to payment list</h2>
    <p>Each installment also appears in Payments. Check fail reasons and guide retry/card change.</p>
    <hr class="section-rule">
    <h2 class="section-title" id="s7">7. Daily checklist</h2>
    <div class="check-box"><ol><li>Notices</li><li>Test Split URL</li><li>New contracts / arrears</li><li>Escalate spikes with ids</li></ol></div>
    <hr class="section-rule">
    <h2 class="section-title" id="s8">8. FAQ</h2>
    <div class="faq-item"><div class="faq-q">No Split URL/menu.</div><div class="faq-a">Ask HQ/distributor to set merchant URL Split Payment = ON (Save persists after re-login) and open menus.</div></div>
    <div class="faq-item"><div class="faq-q">Installment failed.</div><div class="faq-a">Check fail reason; ask customer to retry or update card.</div></div>
""",
        }
    return _split_loc(lang)


def _split_loc(lang: str) -> dict:
    en = split_doc("en")
    titles = {
        "ja": ("ICOPAY 分割決済 加盟店マニュアル", "分割決済<br>加盟店マニュアル", "加盟店視点 — 分割申請URL共有から契約・回数確認まで", "分割決済利用加盟店"),
        "zh": ("ICOPAY 分期支付商户手册", "分期支付<br>商户使用手册", "商户视角 — 分享分期申请 URL 并核对合同与期次", "分期支付商户"),
        "th": ("ICOPAY คู่มือร้านชำระแบ่งจ่าย", "ชำระแบ่งจ่าย<br>คู่มือร้านค้า", "มุมมองร้าน — แชร์ URL แบ่งจ่าย และตรวจสัญญา/งวด", "ร้านที่ใช้แบ่งจ่าย"),
    }
    page, title, sub, target = titles[lang]
    toc = {
        "ja": [("s1", "分割決済とは？"), ("s2", "開始前確認"), ("s3", "STEP 1 — URL"), ("s4", "STEP 2 — 配布"), ("s5", "STEP 3 — 契約・回数"), ("s6", "決済一覧との関係"), ("s7", "チェックリスト"), ("s8", "FAQ")],
        "zh": [("s1", "什么是分期支付？"), ("s2", "开始前确认"), ("s3", "STEP 1 — URL"), ("s4", "STEP 2 — 分享"), ("s5", "STEP 3 — 合同与期次"), ("s6", "与支付明细的关系"), ("s7", "日常清单"), ("s8", "FAQ")],
        "th": [("s1", "แบ่งจ่ายคืออะไร?"), ("s2", "ก่อนเริ่มใช้"), ("s3", "STEP 1 — URL"), ("s4", "STEP 2 — แชร์"), ("s5", "STEP 3 — สัญญา/งวด"), ("s6", "สัมพันธ์รายการชำระ"), ("s7", "เช็คลิสต์"), ("s8", "FAQ")],
    }
    perm = {
        "ja": [("業者管理 &gt; 自社情報", "view", "分割決済URL確認・コピー"), ("分割管理", "view", "契約・回数・未納参照"), ("決済管理", "view", "回数決済の確認"), ("業者登録等", "none", "本社・総代理専用")],
        "zh": [("企业管理 &gt; 我的企业信息", "view", "查看/复制分期 URL"), ("分期管理", "view", "查看合同/期次/欠款"), ("支付管理", "view", "查看期次支付"), ("企业注册等", "none", "总部/总代理专用")],
        "th": [("จัดการร้าน &gt; ข้อมูลร้าน", "view", "ดู/คัดลอก URL แบ่งจ่าย"), ("จัดการแบ่งจ่าย", "view", "ดูสัญญา/งวด/ค้าง"), ("การชำระ", "view", "ดูการชำระงวด"), ("ลงทะเบียนร้าน", "none", "เฉพาะ HQ")],
    }
    return {
        "page_title": page,
        "title_html": title,
        "subtitle": sub,
        "meta": _meta(lang, target),
        "footer_extra": f"ICOPAY Split · V{VERSION}",
        "perm_rows": perm[lang],
        "toc": toc[lang],
        "body": en["body"],
    }


# ---------------------------------------------------------------------------
# Subscribe
# ---------------------------------------------------------------------------
def subscribe_doc(lang: str) -> dict:
    if lang == "ko":
        return {
            "page_title": "ICOPAY 구독결제 가맹점 사용 메뉴얼",
            "title_html": "구독결제<br>가맹점 사용 메뉴얼",
            "subtitle": "가맹점 기준 — 정기(구독) 청구 상태 확인·실패 대응·해지 안내",
            "meta": _meta("ko", "구독결제 사용 가맹점"),
            "footer_extra": f"ICOPAY 구독결제 가맹점 메뉴얼 · V{VERSION}",
            "perm_rows": [
                ("업체관리 &gt; 내 업체정보", "view", "구독 관련 URL·안내 확인(표시 시) / 설정 변경 불가"),
                ("결제관리 &gt; 구독결제내역", "view", "구독·청구 성공/실패·해지 상태 조회"),
                ("결제관리 &gt; 결제내역", "view", "개별 청구 건 상세 확인"),
                ("업체등록·권한설정 등", "none", "본사·총판 전용"),
            ],
            "toc": [
                ("s1", "구독결제란?"),
                ("s2", "시작 전 확인 (구독 사용 ON)"),
                ("s3", "STEP 1 — 고객 안내 포인트"),
                ("s4", "STEP 2 — 구독·청구 내역 확인"),
                ("s5", "STEP 3 — 실패·해지 대응"),
                ("s6", "일상 체크리스트"),
                ("s7", "FAQ"),
            ],
            "body": f"""
    <h2 class="section-title" id="s1">1. 구독결제란?</h2>
    <p>ICOPAY 구독(정기)결제는 약정 주기(예: 월)에 따라 <strong>자동 청구</strong>가 이루어지는 방식입니다. 가맹점은 구독 상태·청구 결과·해지 요청을 확인하고 고객을 안내합니다.</p>
    <div class="flow">
      <div class="flow-row"><span class="flow-actor">고객</span><span class="flow-arrow">→</span><span class="flow-desc">구독 신청·카드 등록 (진입 경로는 가맹/본사 안내에 따름)</span></div>
      <div class="flow-row"><span class="flow-actor">ICOPAY</span><span class="flow-arrow">→</span><span class="flow-desc">주기별 자동 청구</span></div>
      <div class="flow-row"><span class="flow-actor">가맹점</span><span class="flow-arrow">→</span><span class="flow-desc">구독결제내역에서 성공·실패·해지 확인</span></div>
    </div>
    <div class="info-box">구매자 UI의 결제대행사명은 <strong>ICOPAY</strong>입니다. 개발자 API 스펙은 포함하지 않습니다.</div>
    <hr class="section-rule">
    <h2 class="section-title" id="s2">2. 시작 전 확인 (구독 사용 ON)</h2>
    <table>
      <tr><th>확인</th><th>방법</th></tr>
      <tr><td>구독(정기결제) 사용</td><td>상위 설정 ON + 사이드바에 구독결제내역 표시</td></tr>
      <tr><td>메뉴 권한</td><td>없으면 본사·총판에 개방 요청</td></tr>
    </table>
    <div class="hq-box"><strong>상위에 요청</strong><br>• 구독결제 사용 ON<br>• 구독결제내역 메뉴 권한<br>• 해지·재시도 정책 안내</div>
    <hr class="section-rule">
    <h2 class="section-title" id="s3">3. <span class="step-badge">STEP 1</span> 고객 안내 포인트</h2>
    <div class="check-box">가맹점 직원이 고객에게 설명할 때 참고합니다.</div>
    <ul>
      <li>구독은 약정 주기로 <strong>자동 청구</strong>될 수 있습니다.</li>
      <li>청구 실패 시 재시도·카드 갱신이 필요할 수 있습니다.</li>
      <li>해지는 <strong>화면·계약 정책</strong>을 따르며, 직원이 임의로 약관을 바꾸지 않습니다.</li>
    </ul>
    <hr class="section-rule">
    <h2 class="section-title" id="s4">4. <span class="step-badge">STEP 2</span> 구독·청구 내역 확인 <small style="font-size:10pt;font-weight:400;color:#888;">(조회 전용)</small></h2>
    <div class="menu-path">결제관리 &gt; 구독결제내역 (또는 결제내역)</div>
    <ul>
      <li>기간·상태·구독/주문 식별자로 검색합니다.</li>
      <li>성공·실패·해지 여부를 구분합니다.</li>
      <li>문의 시 <strong>구독·주문 식별자·청구일·금액</strong>을 함께 전달합니다.</li>
    </ul>
    <hr class="section-rule">
    <h2 class="section-title" id="s5">5. <span class="step-badge">STEP 3</span> 실패·해지 대응</h2>
    <table>
      <tr><th>상황</th><th>가맹점 대응</th></tr>
      <tr><td>청구 실패</td><td>실패 사유 확인 → 고객에게 카드 갱신·재시도 안내 → 필요 시 상위 문의</td></tr>
      <tr><td>해지 요청</td><td>화면에서 허용된 해지 절차 수행. 권한이 없으면 식별자를 상위에 전달</td></tr>
    </table>
    <div class="warn-box">임의로 청구를 삭제하거나 약관을 변경하지 마세요.</div>
    <hr class="section-rule">
    <h2 class="section-title" id="s6">6. 일상 체크리스트</h2>
    <div class="check-box"><ol><li>공지 확인</li><li>당일 구독 청구 성공/실패 점검</li><li>실패 건 고객 안내</li><li>해지 요청 처리·상위 공유</li></ol></div>
    <hr class="section-rule">
    <h2 class="section-title" id="s7">7. FAQ</h2>
    <div class="faq-item"><div class="faq-q">구독 메뉴가 없습니다.</div><div class="faq-a">구독 사용여부·권한을 상위에 요청하세요.</div></div>
    <div class="faq-item"><div class="faq-q">청구가 계속 실패합니다.</div><div class="faq-a">결제내역 실패 사유를 확인하고 카드 갱신을 안내하세요. 반복되면 상위에 문의하세요.</div></div>
    <div class="faq-item"><div class="faq-q">고객이 해지를 원합니다.</div><div class="faq-a">화면 해지 절차를 따르거나, 권한이 없으면 구독 식별자를 상위에 전달하세요.</div></div>
""",
        }
    if lang == "en":
        return {
            "page_title": "ICOPAY Subscription Payment – Merchant Manual",
            "title_html": "Subscription Payment<br>Merchant Manual",
            "subtitle": "Merchant perspective — review recurring charges, handle failures, and guide cancellations",
            "meta": _meta("en", "Subscription Payment Merchants"),
            "footer_extra": f"ICOPAY Subscription Merchant Manual · V{VERSION}",
            "perm_rows": [
                ("Company Mgmt &gt; My Company Info", "view", "View subscription-related info when shown"),
                ("Payments &gt; Subscription list", "view", "View subscription / charge / cancel status"),
                ("Payments &gt; Payment list", "view", "View individual charge details"),
                ("Registration / access rights", "none", "HQ / Distributor only"),
            ],
            "toc": [
                ("s1", "What is Subscription Payment?"),
                ("s2", "Before You Start"),
                ("s3", "STEP 1 — Customer talking points"),
                ("s4", "STEP 2 — View subscription history"),
                ("s5", "STEP 3 — Failures & cancellations"),
                ("s6", "Daily checklist"),
                ("s7", "FAQ"),
            ],
            "body": f"""
    <h2 class="section-title" id="s1">1. What is Subscription Payment?</h2>
    <p>ICOPAY subscription (recurring) payment charges customers on an agreed cycle. Merchants review status, failures, and cancel requests.</p>
    <div class="info-box">Buyer UI shows <strong>ICOPAY</strong>. API specs are out of scope.</div>
    <hr class="section-rule">
    <h2 class="section-title" id="s2">2. Before You Start</h2>
    <div class="hq-box">Ask HQ to enable subscription and open Subscription payment list permissions.</div>
    <hr class="section-rule">
    <h2 class="section-title" id="s3">3. <span class="step-badge">STEP 1</span> Customer talking points</h2>
    <ul>
      <li>Charges may recur by cycle.</li>
      <li>Failures may need retry / card update.</li>
      <li>Cancel only via on-screen / contract policy.</li>
    </ul>
    <hr class="section-rule">
    <h2 class="section-title" id="s4">4. <span class="step-badge">STEP 2</span> View subscription history <small style="font-size:10pt;font-weight:400;color:#888;">(View Only)</small></h2>
    <div class="menu-path">Payments &gt; Subscription payment list</div>
    <ul><li>Filter by date/status/subscription or order id.</li><li>Note id · charge date · amount for support.</li></ul>
    <hr class="section-rule">
    <h2 class="section-title" id="s5">5. <span class="step-badge">STEP 3</span> Failures &amp; cancellations</h2>
    <table>
      <tr><th>Case</th><th>Action</th></tr>
      <tr><td>Charge failed</td><td>Check reason → guide card update/retry → escalate if needed</td></tr>
      <tr><td>Cancel request</td><td>Follow on-screen cancel, or send subscription id to HQ</td></tr>
    </table>
    <hr class="section-rule">
    <h2 class="section-title" id="s6">6. Daily checklist</h2>
    <div class="check-box"><ol><li>Notices</li><li>Today charges success/fail</li><li>Contact customers on failures</li><li>Process cancels</li></ol></div>
    <hr class="section-rule">
    <h2 class="section-title" id="s7">7. FAQ</h2>
    <div class="faq-item"><div class="faq-q">No subscription menu.</div><div class="faq-a">Request enablement and permissions from HQ.</div></div>
    <div class="faq-item"><div class="faq-q">Charges keep failing.</div><div class="faq-a">Check fail reason; ask customer to update card; escalate repeats.</div></div>
""",
        }
    return _subscribe_loc(lang)


def _subscribe_loc(lang: str) -> dict:
    en = subscribe_doc("en")
    titles = {
        "ja": ("ICOPAY 定期決済 加盟店マニュアル", "定期決済<br>加盟店マニュアル", "加盟店視点 — 定期請求の確認・失敗対応・解約案内", "定期決済利用加盟店"),
        "zh": ("ICOPAY 订阅支付商户手册", "订阅支付<br>商户使用手册", "商户视角 — 核对周期扣款、处理失败与取消指引", "订阅支付商户"),
        "th": ("ICOPAY คู่มือร้านชำระรายงวด", "ชำระรายงวด<br>คู่มือร้านค้า", "มุมมองร้าน — ตรวจการเรียกเก็บซ้ำ จัดการล้มเหลว และยกเลิก", "ร้านที่ใช้สมาชิก/รายงวด"),
    }
    page, title, sub, target = titles[lang]
    toc = {
        "ja": [("s1", "定期決済とは？"), ("s2", "開始前確認"), ("s3", "STEP 1 — 顧客案内"), ("s4", "STEP 2 — 履歴確認"), ("s5", "STEP 3 — 失敗・解約"), ("s6", "チェックリスト"), ("s7", "FAQ")],
        "zh": [("s1", "什么是订阅支付？"), ("s2", "开始前确认"), ("s3", "STEP 1 — 客户说明要点"), ("s4", "STEP 2 — 查看明细"), ("s5", "STEP 3 — 失败与取消"), ("s6", "日常清单"), ("s7", "FAQ")],
        "th": [("s1", "ชำระรายงวดคืออะไร?"), ("s2", "ก่อนเริ่มใช้"), ("s3", "STEP 1 — จุดอธิบายลูกค้า"), ("s4", "STEP 2 — ดูประวัติ"), ("s5", "STEP 3 — ล้มเหลว/ยกเลิก"), ("s6", "เช็คลิสต์"), ("s7", "FAQ")],
    }
    perm = {
        "ja": [("業者管理 &gt; 自社情報", "view", "関連情報確認"), ("決済管理 &gt; 定期決済履歴", "view", "請求・解約状態参照"), ("決済管理 &gt; 決済一覧", "view", "個別請求確認"), ("業者登録等", "none", "本社・総代理専用")],
        "zh": [("企业管理 &gt; 我的企业信息", "view", "查看相关信息"), ("支付管理 &gt; 订阅支付明细", "view", "查看扣款/取消状态"), ("支付管理 &gt; 支付明细", "view", "查看单笔扣款"), ("企业注册等", "none", "总部/总代理专用")],
        "th": [("จัดการร้าน &gt; ข้อมูลร้าน", "view", "ดูข้อมูลที่เกี่ยวข้อง"), ("การชำระ &gt; รายการสมาชิก", "view", "ดูสถานะเรียกเก็บ/ยกเลิก"), ("การชำระ &gt; รายการชำระ", "view", "ดูรายละเอียดแต่ละครั้ง"), ("ลงทะเบียนร้าน", "none", "เฉพาะ HQ")],
    }
    return {
        "page_title": page,
        "title_html": title,
        "subtitle": sub,
        "meta": _meta(lang, target),
        "footer_extra": f"ICOPAY Subscribe · V{VERSION}",
        "perm_rows": perm[lang],
        "toc": toc[lang],
        "body": en["body"],
    }


DOC_BUILDERS = {
    "merchant-url-user": url_doc,
    "merchant-split-user": split_doc,
    "merchant-subscribe-user": subscribe_doc,
}
