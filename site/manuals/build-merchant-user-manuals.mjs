/**
 * @deprecated Prefer chatbot-design builder:
 *   python site/manuals/_build_merchant_user_manuals_v3.py
 * (cover / permission table / STEP / FAQ — same as docs/icopay-chatbot-merchant-manual-*.html)
 *
 * Legacy thin HTML (merchant-ops CSS) — kept for reference only.
 * Run: node site/manuals/build-merchant-user-manuals.mjs
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const OUT = path.join(__dirname, 'generated');
const VERSION = '2.64';
const DATE = '2026-07-23';
const LANGS = ['ko', 'en', 'ja', 'zh', 'th'];

const UI = {
  ko: { print: '인쇄 / PDF 저장', toc: '목차', version: '문서 버전' },
  en: { print: 'Print / Save PDF', toc: 'Contents', version: 'Document version' },
  ja: { print: '印刷 / PDF保存', toc: '目次', version: '文書バージョン' },
  zh: { print: '打印 / 保存 PDF', toc: '目录', version: '文档版本' },
  th: { print: 'พิมพ์ / บันทึก PDF', toc: 'สารบัญ', version: 'เวอร์ชันเอกสาร' }
};

function esc(s) {
  return String(s == null ? '' : s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

/** @type {Array<{id:string,audience:string,titles:Record<string,string>,subs:Record<string,string>,sections:Record<string,Array<{h:string,html:string}>>}>} */
const MANUALS = [
  {
    id: 'merchant-url-user',
    audience: 'merchant',
    titles: {
      ko: 'URL결제 사용자 메뉴얼',
      en: 'URL Payment — User Manual',
      ja: 'URL決済 ユーザーマニュアル',
      zh: 'URL支付用户手册',
      th: 'คู่มือผู้ใช้ชำระด้วย URL'
    },
    subs: {
      ko: '가맹점 권한 화면 기준 — 결제 URL 공유·거래 확인·일상 운영',
      en: 'Merchant screens — share payment URL, verify results, daily ops',
      ja: '加盟店権限画面 — 決済URL共有・結果確認・日常運用',
      zh: '按商户权限画面 — 分享支付 URL、核对结果、日常运营',
      th: 'ตามหน้าจอสิทธิ์ร้าน — แชร์ URL ชำระ ตรวจผล งานประจำวัน'
    },
    sections: {
      ko: [
        { h: '이 매뉴얼의 범위', html: `<p>가맹점(<strong>MERCHANT</strong>) 계정으로 접근 가능한 화면을 기준으로, 고객에게 공개 결제 링크를 보내고 승인 여부를 확인하는 방법을 설명합니다.</p>
<ul>
<li>메뉴는 <strong>본사정책 → 접근·권한</strong>에서 부여한 권한에 따라 달라집니다. 접근불가 메뉴는 사이드바에 보이지 않습니다.</li>
<li>개발자용 API·키 발급·연동 스펙은 포함하지 않습니다. 연동이 필요하면 상위 본사·총판에 <strong>연동·배포 → 가맹 API 출시</strong> 안내를 요청하십시오.</li>
<li>결제대행사(PG) 이름은 가맹·구매자 UI에서 <strong>ICOPAY</strong>로 중립 표시됩니다.</li>
</ul>` },
        { h: '로그인과 화면', html: `<ol>
<li>가맹점 ID로 로그인합니다.</li>
<li>허용된 메뉴만 좌측에 표시됩니다.</li>
<li>언어(KO/EN/JP/CH/TH)를 전환할 수 있습니다.</li>
<li>대시보드가 있으면 최근 결제 요약을 확인합니다.</li>
</ol>` },
        { h: '공지사항', html: `<p class="menu-path">업체관리 → 공지사항</p>
<p>본사·총판 운영 공지(수수료·정산일·점검·웹결제 중지)를 로그인 후 먼저 확인합니다.</p>` },
        { h: '업체정보조회 — 결제 URL', html: `<p class="menu-path">업체관리 → 업체정보조회</p>
<table>
<tr><th>항목</th><th>설명</th></tr>
<tr><td>결제 URL</td><td>공개 일회 결제 주소. 예: <code>https://(서비스도메인)/checkout/업체코드</code>. <strong>[복사]</strong>로 전달합니다.</td></tr>
<tr><td>URL 재결제 URL</td><td>저장 카드 재결제용(본사·기능이 켜져 있을 때만 표시).</td></tr>
</table>
<ol>
<li>업체정보조회를 연다.</li>
<li><strong>결제 URL</strong>을 확인하고 [복사]한다.</li>
<li>SMS·메신저·메일·쇼핑몰에 붙여넣는다.</li>
<li>본인 브라우저에서 URL을 열어 결제창이 뜨는지 확인한다.</li>
</ol>
<p><strong>주의:</strong> 웹결제 미사용·중지면 URL이 열리지 않거나 안내만 표시될 수 있습니다.</p>` },
        { h: '고객에게 전달할 때', html: `<ul>
<li>복사한 URL만 전달하거나, 금액·상품명을 메시지에 함께 적습니다.</li>
<li>결제창 금액·상품명 표시는 본사정책 따름/가맹 설정에 따릅니다.</li>
<li>신규 고객에는 일반 결제 URL을 쓰고, 재결제 URL은 저장 카드 고객에게만 사용합니다.</li>
</ul>` },
        { h: '결제내역 확인', html: `<p class="menu-path">결제관리 → 결제내역 (또는 URL결제내역)</p>
<ul>
<li>기간·상태·키워드(주문번호)로 검색합니다.</li>
<li>승인·실패·취소·환불을 구분합니다.</li>
<li>고객 문의 시 주문번호·결제시각·금액을 함께 전달하면 처리가 빠릅니다.</li>
</ul>` },
        { h: '자주 하는 확인', html: `<ul>
<li>URL을 열었는데 안내만 보임 → 웹결제 사용여부·상위 중지 여부</li>
<li>결제가 실패함 → 결제내역 실패 사유, 필요 시 상위 「리스크 현황」</li>
<li>메뉴가 없음 → 본사·총판에 권한 개방 요청</li>
</ul>` },
        { h: '일상 체크리스트', html: `<ol>
<li>공지 확인</li>
<li>결제 URL 복사·테스트 오픈</li>
<li>당일/전일 승인·실패 점검</li>
<li>실패 급증 시 주문번호와 함께 상위 공유</li>
<li>재결제 URL 오남용 여부</li>
</ol>` },
        { h: '문의', html: `<p>메뉴가 없거나 웹결제가 중지된 경우 상위 본사·총판에 권한·설정 개방을 요청하십시오. 문서 버전 V${VERSION}.</p>` }
      ],
      en: [
        { h: 'Scope', html: `<p>How merchant staff share a public payment link and verify approvals. Not an API integration guide.</p>
<ul><li>Menus follow HQ Policy → Access.</li><li>PG names show as ICOPAY on buyer UI.</li></ul>` },
        { h: 'Sign-in', html: `<ol><li>Sign in with merchant ID</li><li>Sidebar shows permitted menus</li><li>Switch language</li><li>Check dashboard if shown</li></ol>` },
        { h: 'Notices', html: `<p class="menu-path">Companies → Notices</p><p>Read HQ/distributor announcements after login.</p>` },
        { h: 'Company info — Payment URL', html: `<p class="menu-path">Companies → Company info</p>
<table><tr><th>Item</th><th>Description</th></tr>
<tr><td>Payment URL</td><td>Public one-time checkout. Use <strong>Copy</strong>.</td></tr>
<tr><td>Re-pay URL</td><td>Saved-card repay (when enabled).</td></tr></table>
<ol><li>Open Company info</li><li>Copy Payment URL</li><li>Share via SMS/messenger/email</li><li>Open once yourself to verify</li></ol>
<p><strong>Note:</strong> Off/Suspended web-pay may block the URL.</p>` },
        { h: 'Sharing with customers', html: `<ul><li>Add amount/product notes if needed</li><li>New buyers use Payment URL; Re-pay URL only for returning customers</li></ul>` },
        { h: 'Payment list', html: `<p class="menu-path">Payments → Payment list</p><p>Search by period/status/order no. Review approvals, failures, cancels, refunds.</p>` },
        { h: 'Quick checks', html: `<ul><li>Notice-only page → web-pay / HQ suspend</li><li>Failures → fail reason / Risk dashboard</li><li>Missing menu → ask HQ</li></ul>` },
        { h: 'Daily checklist', html: `<ol><li>Notices</li><li>Copy &amp; test Payment URL</li><li>Today/yesterday approvals &amp; failures</li><li>Escalate spikes with order ids</li></ol>` },
        { h: 'Support', html: `<p>Ask HQ/distributor for missing menus or web-pay settings. Document version V${VERSION}.</p>` }
      ],
      ja: [
        { h: '対象範囲', html: `<p>加盟店ログインで決済リンクを送り承認を確認する案内です。API連携仕様は含みません。</p>` },
        { h: 'ログイン', html: `<ol><li>ログイン</li><li>許可メニューのみ</li><li>言語切替</li><li>ダッシュボード</li></ol>` },
        { h: 'お知らせ', html: `<p class="menu-path">業者管理 → お知らせ</p>` },
        { h: '業者情報 — 決済URL', html: `<p class="menu-path">業者管理 → 業者情報照会</p>
<p>決済URLを[コピー]して共有し、自分で一度開いて確認します。Web決済停止中は開けない場合があります。</p>` },
        { h: '顧客への共有', html: `<p>金額・商品名は必要ならメッセージに併記。新規は通常URL。</p>` },
        { h: '決済一覧', html: `<p class="menu-path">決済管理 → 決済一覧</p>` },
        { h: 'よくある確認', html: `<ul><li>案内のみ→Web決済/停止</li><li>失敗→履歴・上位</li><li>メニュー無し→権限依頼</li></ul>` },
        { h: 'チェックリスト', html: `<ol><li>お知らせ</li><li>URLテスト</li><li>当日/前日の成否</li><li>急増時は注文番号共有</li></ol>` },
        { h: '問い合わせ', html: `<p>権限・設定は上位へ。文書 V${VERSION}。</p>` }
      ],
      zh: [
        { h: '适用范围', html: `<p>商户登录后分享公开支付链接并核对结果。不含 API 对接规格。</p>` },
        { h: '登录', html: `<ol><li>登录</li><li>允许菜单</li><li>语言</li><li>仪表盘</li></ol>` },
        { h: '公告', html: `<p class="menu-path">企业管理 → 公告</p>` },
        { h: '企业信息 — 支付 URL', html: `<p class="menu-path">企业管理 → 企业信息查询</p>
<p>复制支付 URL 发给客户并自行测试。未启用网页支付时可能无法打开。</p>` },
        { h: '发给客户', html: `<p>必要时注明金额/商品名。新客用普通支付 URL。</p>` },
        { h: '支付明细', html: `<p class="menu-path">支付管理 → 支付明细</p>` },
        { h: '常见检查', html: `<ul><li>仅提示→网页支付/停用</li><li>失败→明细</li><li>无菜单→申请权限</li></ul>` },
        { h: '日常清单', html: `<ol><li>公告</li><li>测试 URL</li><li>当日成败</li><li>异常附订单号</li></ol>` },
        { h: '联系', html: `<p>请联系上级。文档 V${VERSION}。</p>` }
      ],
      th: [
        { h: 'ขอบเขต', html: `<p>แชร์ลิงก์ชำระและตรวจผล ไม่รวมสเปก API</p>` },
        { h: 'เข้าสู่ระบบ', html: `<ol><li>เข้าสู่ระบบ</li><li>เมนูที่อนุญาต</li><li>ภาษา</li><li>แดชบอร์ด</li></ol>` },
        { h: 'ประกาศ', html: `<p class="menu-path">จัดการร้าน → ประกาศ</p>` },
        { h: 'ข้อมูลร้าน — Payment URL', html: `<p class="menu-path">จัดการร้าน → ข้อมูลร้าน</p>
<p>คัดลอก Payment URL ส่งลูกค้า แล้วทดสอบเอง</p>` },
        { h: 'ส่งให้ลูกค้า', html: `<p>ระบุจำนวนเงินในข้อความถ้าจำเป็น</p>` },
        { h: 'รายการชำระ', html: `<p class="menu-path">การชำระ → รายการชำระ</p>` },
        { h: 'ตรวจเร็ว', html: `<ul><li>เห็นแต่ข้อความแจ้ง → ปิดเว็บชำระ</li><li>ไม่มีเมนู → ขอสิทธิ์</li></ul>` },
        { h: 'เช็คลิสต์', html: `<ol><li>ประกาศ</li><li>ทดสอบ URL</li><li>ตรวจสำเร็จ/ล้มเหลว</li></ol>` },
        { h: 'ติดต่อ', html: `<p>ติดต่อตัวแทน/HQ เอกสาร V${VERSION}</p>` }
      ]
    }
  },
  {
    id: 'merchant-split-user',
    audience: 'merchant',
    titles: {
      ko: '분할결제 사용자 메뉴얼',
      en: 'Split Payment — User Manual',
      ja: '分割決済 ユーザーマニュアル',
      zh: '分期支付用户手册',
      th: 'คู่มือผู้ใช้แบ่งจ่าย'
    },
    subs: {
      ko: '가맹점 권한 화면 기준 — 분할결제 URL·계약·회차·일상 운영',
      en: 'Merchant screens — split URL, contracts, installments, daily ops',
      ja: '加盟店権限画面 — 分割URL・契約・回次・日常運用',
      zh: '按商户权限画面 — 分期 URL、合同、期次、日常运营',
      th: 'ตามหน้าจอสิทธิ์ร้าน — URL แบ่งจ่าย สัญญา งวด งานประจำวัน'
    },
    sections: {
      ko: [
        { h: '이 매뉴얼의 범위', html: `<p>분할결제를 사용하는 가맹점이 고객에게 분할 신청 링크를 주고, 계약·회차 납부 상태를 확인하는 방법입니다. API 연동 스펙은 포함하지 않습니다.</p>
<ul><li>메뉴는 본사정책 → 접근·권한에 따릅니다.</li><li>결제대행은 구매자 UI에서 ICOPAY로 표시됩니다.</li></ul>` },
        { h: '로그인과 화면', html: `<ol><li>가맹점 ID로 로그인</li><li>허용된 메뉴만 표시</li><li>언어 전환</li><li>대시보드 확인</li></ol>` },
        { h: '분할결제 사용 조건', html: `<p>가맹 「분할결제 사용여부」가 <strong>사용(ON)</strong>이어야 분할결제 URL·분할관리 메뉴가 보입니다. 꺼져 있으면 상위 담당자에게 요청하십시오.</p>` },
        { h: '업체정보조회 — 분할결제 URL', html: `<p class="menu-path">업체관리 → 업체정보조회</p>
<table>
<tr><th>항목</th><th>설명</th></tr>
<tr><td>분할결제 URL</td><td>고객이 분할 신청·회차 결제를 진행하는 공개 진입 주소. <strong>[복사]</strong>로 전달합니다.</td></tr>
</table>
<ol>
<li>업체정보조회 → 분할결제 URL 확인</li>
<li>[복사] → 고객 채널에 전달</li>
<li>본인 브라우저에서 테스트</li>
</ol>
<p><strong>주의:</strong> 분할결제 미사용이면 URL이 없거나 동작하지 않을 수 있습니다.</p>` },
        { h: '분할관리(권한 시)', html: `<p class="menu-path">분할관리</p>
<ul>
<li>계약 목록·회차 상태·미납을 확인합니다.</li>
<li>고객 문의 시 계약번호·회차·납부일을 함께 확인합니다.</li>
<li>회차 수·기간 규칙은 가맹/본사 설정을 따르며 임의로 바꾸지 마십시오.</li>
</ul>` },
        { h: '결제내역과의 관계', html: `<p class="menu-path">결제관리 → 결제내역 (또는 분할결제내역)</p>
<p>각 회차 결제는 결제내역에도 나타납니다. 승인 실패 시 재시도·다른 카드 안내가 가능합니다.</p>` },
        { h: '자주 하는 확인', html: `<ul>
<li>분할 URL/메뉴 없음 → 사용여부·권한</li>
<li>회차 실패 → 결제내역 실패 사유</li>
<li>연체·미납 → 분할관리 확인 후 상위 문의</li>
</ul>` },
        { h: '일상 체크리스트', html: `<ol>
<li>공지 확인</li>
<li>분할결제 URL 유효·테스트</li>
<li>신규 계약·미납 회차 점검</li>
<li>실패 급증 시 계약/주문번호와 함께 상위 공유</li>
</ol>` },
        { h: '문의', html: `<p>설정·권한·정산은 상위 본사·총판에 연락하십시오. 문서 버전 V${VERSION}.</p>` }
      ],
      en: [
        { h: 'Scope', html: `<p>Share the split-pay link and track contracts/installments. Not an API guide.</p>` },
        { h: 'Sign-in', html: `<ol><li>Sign in</li><li>Permitted menus</li><li>Language</li><li>Dashboard</li></ol>` },
        { h: 'Enablement', html: `<p>Split payment must be <strong>ON</strong>. Ask HQ if URL/menus are missing.</p>` },
        { h: 'Company info — Split URL', html: `<p class="menu-path">Companies → Company info</p>
<p>Copy <strong>Split payment URL</strong>, share with the customer, and test in your browser.</p>` },
        { h: 'Split management', html: `<p class="menu-path">Split management</p>
<p>Review contracts, installment status, arrears. Do not change installment rules arbitrarily.</p>` },
        { h: 'Payment list', html: `<p class="menu-path">Payments → Payment list</p>
<p>Each installment appears in payment history.</p>` },
        { h: 'Quick checks', html: `<ul><li>No URL/menu → enablement/permissions</li><li>Failed installment → fail reason</li><li>Arrears → Split management then escalate</li></ul>` },
        { h: 'Daily checklist', html: `<ol><li>Notices</li><li>Test Split URL</li><li>New contracts &amp; unpaid</li><li>Escalate with contract/order ids</li></ol>` },
        { h: 'Support', html: `<p>Ask HQ/distributor. Document version V${VERSION}.</p>` }
      ],
      ja: [
        { h: '対象範囲', html: `<p>分割リンク共有と契約・回次確認。API仕様は含みません。</p>` },
        { h: 'ログイン', html: `<ol><li>ログイン</li><li>許可メニュー</li><li>言語</li><li>ダッシュボード</li></ol>` },
        { h: '利用条件', html: `<p>「分割決済使用」がONであること。</p>` },
        { h: '業者情報 — 分割URL', html: `<p class="menu-path">業者管理 → 業者情報照会</p><p>分割決済URLをコピーし顧客に送ります。</p>` },
        { h: '分割管理', html: `<p class="menu-path">分割管理</p><p>契約・回次・未納を確認します。</p>` },
        { h: '決済一覧', html: `<p>各回次は決済一覧にも表示されます。</p>` },
        { h: 'よくある確認', html: `<ul><li>URL無し→使用/権限</li><li>失敗→履歴</li><li>延滞→分割管理</li></ul>` },
        { h: 'チェックリスト', html: `<ol><li>お知らせ</li><li>URLテスト</li><li>新規契約・未納</li></ol>` },
        { h: '問い合わせ', html: `<p>設定は上位へ。文書 V${VERSION}。</p>` }
      ],
      zh: [
        { h: '适用范围', html: `<p>分享分期链接并核对合同/期次。不含 API 规格。</p>` },
        { h: '登录', html: `<ol><li>登录</li><li>允许菜单</li><li>语言</li><li>仪表盘</li></ol>` },
        { h: '启用条件', html: `<p>须开启「使用分期支付」。</p>` },
        { h: '企业信息 — 分期 URL', html: `<p class="menu-path">企业管理 → 企业信息查询</p><p>复制分期支付 URL 发给客户。</p>` },
        { h: '分期管理', html: `<p class="menu-path">分期管理</p><p>查看合同、期次、欠款。</p>` },
        { h: '支付明细', html: `<p>各期会出现在支付明细。</p>` },
        { h: '常见检查', html: `<ul><li>无 URL→启用/权限</li><li>失败→明细</li><li>欠款→分期管理</li></ul>` },
        { h: '日常清单', html: `<ol><li>公告</li><li>测试分期 URL</li><li>新合同与欠款</li></ol>` },
        { h: '联系', html: `<p>设置请联系上级。文档 V${VERSION}。</p>` }
      ],
      th: [
        { h: 'ขอบเขต', html: `<p>แชร์ลิงก์แบ่งจ่ายและตรวจสัญญา/งวด ไม่รวมสเปก API</p>` },
        { h: 'เข้าสู่ระบบ', html: `<ol><li>เข้าสู่ระบบ</li><li>เมนูที่อนุญาต</li><li>ภาษา</li><li>แดชบอร์ด</li></ol>` },
        { h: 'เงื่อนไข', html: `<p>ต้องเปิดใช้แบ่งจ่ายของร้าน</p>` },
        { h: 'ข้อมูลร้าน — URL แบ่งจ่าย', html: `<p class="menu-path">จัดการร้าน → ข้อมูลร้าน</p><p>คัดลอก URL แบ่งจ่าย แล้วส่งลูกค้า</p>` },
        { h: 'จัดการแบ่งจ่าย', html: `<p class="menu-path">จัดการแบ่งจ่าย</p><p>ดูสัญญา/งวด/ค้างชำระ</p>` },
        { h: 'รายการชำระ', html: `<p>แต่ละงวดโผล่ในรายการชำระ</p>` },
        { h: 'ตรวจเร็ว', html: `<ul><li>ไม่มี URL → เปิดใช้/สิทธิ์</li><li>ค้างชำระ → จัดการแบ่งจ่าย</li></ul>` },
        { h: 'เช็คลิสต์', html: `<ol><li>ประกาศ</li><li>ทดสอบ URL</li><li>สัญญาใหม่/ค้าง</li></ol>` },
        { h: 'ติดต่อ', html: `<p>ติดต่อตัวแทน/HQ เอกสาร V${VERSION}</p>` }
      ]
    }
  },
  {
    id: 'merchant-subscribe-user',
    audience: 'merchant',
    titles: {
      ko: '구독결제 사용자 메뉴얼',
      en: 'Subscription Payment — User Manual',
      ja: '定期決済 ユーザーマニュアル',
      zh: '订阅支付用户手册',
      th: 'คู่มือผู้ใช้ชำระรายงวด'
    },
    subs: {
      ko: '가맹점 권한 화면 기준 — 구독(정기) 상태·청구·해지·일상 운영',
      en: 'Merchant screens — subscription status, billing, cancel, daily ops',
      ja: '加盟店権限画面 — 定期状態・請求・解約・日常運用',
      zh: '按商户权限画面 — 订阅状态、扣款、取消、日常运营',
      th: 'ตามหน้าจอสิทธิ์ร้าน — สถานะสมาชิก เรียกเก็บ ยกเลิก งานประจำวัน'
    },
    sections: {
      ko: [
        { h: '이 매뉴얼의 범위', html: `<p>구독(정기) 결제를 사용하는 가맹점이 고객 구독 상태·자동 청구·해지를 확인하는 방법입니다. API 연동 스펙은 포함하지 않습니다.</p>
<ul><li>연동은 상위 「가맹 API 출시」 문서를 따르십시오.</li><li>구매자 UI의 결제대행사명은 ICOPAY로 표시됩니다.</li></ul>` },
        { h: '로그인과 화면', html: `<ol><li>가맹점 ID로 로그인</li><li>허용된 메뉴만 표시</li><li>언어 전환</li><li>대시보드 확인</li></ol>` },
        { h: '구독 사용 조건', html: `<p>본사·가맹 설정에서 구독(정기결제)이 켜져 있고 관련 메뉴 권한이 열려 있어야 합니다. 없으면 상위 담당자에게 요청하십시오.</p>` },
        { h: '고객 안내 포인트', html: `<ul>
<li>구독은 약정 주기(월 등)로 자동 청구될 수 있습니다.</li>
<li>결제 실패 시 재시도·카드 갱신이 필요할 수 있습니다.</li>
<li>해지는 계약·화면 정책에 따릅니다. 직원이 임의로 약관을 바꾸지 마십시오.</li>
</ul>` },
        { h: '구독·결제 내역 확인', html: `<p class="menu-path">결제관리 → 구독결제내역 (또는 결제내역)</p>
<ul>
<li>기간·상태·주문/구독 식별자로 검색합니다.</li>
<li>성공·실패·해지 여부를 구분합니다.</li>
<li>문의 시 구독/주문 식별자·청구일·금액을 함께 전달합니다.</li>
</ul>` },
        { h: '해지·문의 대응', html: `<p>화면에서 허용된 해지 절차를 따르거나, 권한이 없으면 상위 운영자에게 구독 식별자를 전달합니다.</p>` },
        { h: '자주 하는 확인', html: `<ul>
<li>구독 메뉴 없음 → 사용여부·권한</li>
<li>청구 실패 → 결제내역 실패 사유·카드 갱신 안내</li>
<li>해지 요청 → 화면 절차 또는 상위 전달</li>
</ul>` },
        { h: '일상 체크리스트', html: `<ol>
<li>공지 확인</li>
<li>당일 구독 청구 성공/실패 점검</li>
<li>실패 건 고객 안내</li>
<li>해지 요청 처리·상위 공유</li>
</ol>` },
        { h: '문의', html: `<p>설정·권한·정산은 상위 본사·총판에 연락하십시오. 문서 버전 V${VERSION}.</p>` }
      ],
      en: [
        { h: 'Scope', html: `<p>Check subscription status, recurring charges, and cancellations. Not an API guide.</p>` },
        { h: 'Sign-in', html: `<ol><li>Sign in</li><li>Permitted menus</li><li>Language</li><li>Dashboard</li></ol>` },
        { h: 'Requirements', html: `<p>Subscription must be enabled and menus permitted.</p>` },
        { h: 'Customer talking points', html: `<ul><li>Recurring charges by cycle</li><li>Failed charges may need retry/card update</li><li>Cancel per on-screen policy</li></ul>` },
        { h: 'View history', html: `<p class="menu-path">Payments → Subscription payment list</p>
<p>Filter by date/status/subscription or order id.</p>` },
        { h: 'Cancellations', html: `<p>Follow on-screen cancel if allowed, or escalate the subscription id to HQ.</p>` },
        { h: 'Quick checks', html: `<ul><li>No menu → enablement/permissions</li><li>Charge failed → fail reason / update card</li><li>Cancel request → on-screen or escalate</li></ul>` },
        { h: 'Daily checklist', html: `<ol><li>Notices</li><li>Today charges success/fail</li><li>Contact customers on failures</li><li>Process cancels</li></ol>` },
        { h: 'Support', html: `<p>Ask HQ/distributor. Document version V${VERSION}.</p>` }
      ],
      ja: [
        { h: '対象範囲', html: `<p>定期決済の状態・請求・解約確認。API仕様は含みません。</p>` },
        { h: 'ログイン', html: `<ol><li>ログイン</li><li>許可メニュー</li><li>言語</li><li>ダッシュボード</li></ol>` },
        { h: '利用条件', html: `<p>定期決済が有効でメニュー権限があること。</p>` },
        { h: '顧客案内', html: `<ul><li>周期課金</li><li>失敗時は再試行/カード更新</li><li>解約は画面ポリシー</li></ul>` },
        { h: '履歴確認', html: `<p class="menu-path">決済管理 → 定期決済履歴</p>` },
        { h: '解約対応', html: `<p>画面の解約手順、または上位へ識別子を連絡します。</p>` },
        { h: 'よくある確認', html: `<ul><li>メニュー無し→使用/権限</li><li>請求失敗→履歴</li><li>解約→画面または上位</li></ul>` },
        { h: 'チェックリスト', html: `<ol><li>お知らせ</li><li>当日請求の成否</li><li>失敗顧客案内</li><li>解約処理</li></ol>` },
        { h: '問い合わせ', html: `<p>設定は上位へ。文書 V${VERSION}。</p>` }
      ],
      zh: [
        { h: '适用范围', html: `<p>核对订阅状态、扣款与取消。不含 API 规格。</p>` },
        { h: '登录', html: `<ol><li>登录</li><li>允许菜单</li><li>语言</li><li>仪表盘</li></ol>` },
        { h: '使用条件', html: `<p>须启用订阅且有菜单权限。</p>` },
        { h: '客户说明', html: `<ul><li>按周期扣款</li><li>失败可能需重试/换卡</li><li>取消按画面政策</li></ul>` },
        { h: '查看明细', html: `<p class="menu-path">支付管理 → 订阅支付明细</p>` },
        { h: '取消处理', html: `<p>按画面流程，或将订阅标识交给上级。</p>` },
        { h: '常见检查', html: `<ul><li>无菜单→启用/权限</li><li>扣款失败→明细</li><li>取消→画面或上报</li></ul>` },
        { h: '日常清单', html: `<ol><li>公告</li><li>当日扣款成败</li><li>失败客户沟通</li><li>处理取消</li></ol>` },
        { h: '联系', html: `<p>设置请联系上级。文档 V${VERSION}。</p>` }
      ],
      th: [
        { h: 'ขอบเขต', html: `<p>ตรวจสถานะสมาชิก การเรียกเก็บ และการยกเลิก ไม่รวมสเปก API</p>` },
        { h: 'เข้าสู่ระบบ', html: `<ol><li>เข้าสู่ระบบ</li><li>เมนูที่อนุญาต</li><li>ภาษา</li><li>แดชบอร์ด</li></ol>` },
        { h: 'เงื่อนไข', html: `<p>ต้องเปิดใช้สมาชิกและมีสิทธิ์เมนู</p>` },
        { h: 'คุยกับลูกค้า', html: `<ul><li>เรียกเก็บตามรอบ</li><li>ล้มเหลวอาจต้องลองใหม่/เปลี่ยนบัตร</li><li>ยกเลิกตามนโยบายหน้าจอ</li></ul>` },
        { h: 'ดูประวัติ', html: `<p class="menu-path">การชำระ → รายการชำระสมาชิก</p>` },
        { h: 'ยกเลิก', html: `<p>ทำตามหน้าจอ หรือส่งรหัสสมาชิกให้ HQ</p>` },
        { h: 'ตรวจเร็ว', html: `<ul><li>ไม่มีเมนู → เปิดใช้/สิทธิ์</li><li>เรียกเก็บล้มเหลว → ดูเหตุผล</li></ul>` },
        { h: 'เช็คลิสต์', html: `<ol><li>ประกาศ</li><li>ตรวจเรียกเก็บวันนี้</li><li>แจ้งลูกค้าเมื่อล้มเหลว</li><li>จัดการยกเลิก</li></ol>` },
        { h: 'ติดต่อ', html: `<p>ติดต่อตัวแทน/HQ เอกสาร V${VERSION}</p>` }
      ]
    }
  }
];

function renderHtml(manual, lang) {
  const ui = UI[lang];
  const title = manual.titles[lang];
  const sub = manual.subs[lang];
  const sections = manual.sections[lang];
  const toc = sections.map((s, i) => `<li><a href="#s${i + 1}">${esc(s.h)}</a></li>`).join('');
  const body = sections.map((s, i) =>
    `<h2 class="section-title" id="s${i + 1}">${i + 1}. ${esc(s.h)}</h2>\n${s.html}`
  ).join('\n');
  const htmlLang = lang === 'zh' ? 'zh-CN' : lang === 'ja' ? 'ja' : lang === 'th' ? 'th' : lang === 'en' ? 'en' : 'ko';

  return `<!DOCTYPE html>
<html lang="${htmlLang}">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>${esc(title)} V${VERSION}</title>
<style>
  *,*::before,*::after{box-sizing:border-box;margin:0;padding:0}
  body{font-family:'Malgun Gothic','Segoe UI','Noto Sans',sans-serif;font-size:11pt;line-height:1.75;color:#1a1a1a;background:#f0f2f8}
  .page-wrap{max-width:960px;margin:32px auto;background:#fff;border-radius:10px;box-shadow:0 2px 18px rgba(0,0,0,.11);overflow:hidden}
  .cover{background:linear-gradient(135deg,#1a3a5c 0%,#1565c0 55%,#26a69a 100%);color:#fff;padding:40px 48px;display:flex;gap:24px;justify-content:space-between;align-items:flex-start}
  .cover h1{font-size:20pt;font-weight:900;line-height:1.3;margin:8px 0}
  .cover .subtitle{opacity:.85;font-size:10.5pt;margin-bottom:16px}
  .cover .meta{font-size:9pt;opacity:.7;border-top:1px solid rgba(255,255,255,.25);padding-top:12px}
  .brand-box{background:rgba(255,255,255,.92);color:#1a3a5c;border-radius:8px;padding:12px 16px;min-width:160px;text-align:center}
  .brand-box img{max-height:56px;max-width:200px;object-fit:contain}
  .brand-box .nm{font-weight:800;font-size:11pt;margin-top:6px}
  .brand-box .addr{font-size:8pt;opacity:.8;margin-top:4px;line-height:1.4}
  .body{padding:40px 48px 56px}
  .toc{background:#e8f5f3;border-left:4px solid #00897b;border-radius:0 8px 8px 0;padding:16px 22px;margin-bottom:28px}
  .toc h2{font-size:12pt;color:#00695c;margin-bottom:8px}
  .toc ol{padding-left:20px}
  .toc a{color:#00695c;text-decoration:none}
  h2.section-title{font-size:14pt;font-weight:800;color:#1a3a5c;border-bottom:2px solid #00897b;padding-bottom:6px;margin:36px 0 14px}
  p,li{margin-bottom:8px}
  ul,ol{padding-left:20px;margin-bottom:12px}
  table{width:100%;border-collapse:collapse;margin:12px 0 18px;font-size:10pt}
  th{background:#1a3a5c;color:#fff;padding:8px 10px;text-align:left}
  td{padding:7px 10px;border:1px solid #cfd8dc}
  tr:nth-child(even) td{background:#f4f7ff}
  code{background:#eceff1;padding:1px 6px;border-radius:4px;font-size:9.5pt}
  .menu-path{display:inline-block;background:#eceff1;border:1px solid #cfd8dc;border-radius:5px;padding:3px 10px;font-size:9.5pt;font-weight:600;margin-bottom:10px}
  .footer{background:#1a3a5c;color:rgba(255,255,255,.65);text-align:center;font-size:9pt;padding:16px}
  .print-btn{position:fixed;bottom:24px;right:24px;z-index:99;background:#00897b;color:#fff;border:none;border-radius:10px;padding:10px 18px;font-weight:700;cursor:pointer;box-shadow:0 4px 14px rgba(0,137,123,.4)}
  @media print{body{background:#fff}.page-wrap{margin:0;box-shadow:none;border-radius:0}.print-btn{display:none!important}@page{size:A4;margin:16mm}}
</style>
</head>
<body>
<button type="button" class="print-btn" onclick="window.print()">${esc(ui.print)}</button>
<div class="page-wrap">
  <div class="cover">
    <div class="cover-body">
      <div style="font-weight:700;letter-spacing:1px;opacity:.85">__BRAND_SITE_NAME__</div>
      <h1>${esc(title)}</h1>
      <div class="subtitle">${esc(sub)}</div>
      <div class="meta">${esc(ui.version)}: <strong>V${VERSION}</strong> · ${DATE} · ICOPAY Platform Manuals</div>
    </div>
    <div class="brand-box">
      <img src="__BRAND_LOGO_URL__" alt="__BRAND_SITE_NAME__" onerror="this.style.display='none'">
      <div class="nm">__BRAND_COMP_NM__</div>
      <div class="addr">__BRAND_ADDR__</div>
      <div class="addr">__BRAND_TEL__ __BRAND_EMAIL__</div>
    </div>
  </div>
  <div class="body">
    <div class="toc"><h2>${esc(ui.toc)}</h2><ol>${toc}</ol></div>
    ${body}
  </div>
  <div class="footer">
    __BRAND_SITE_NAME__ · ${esc(title)} · V${VERSION}<br>
    __BRAND_COPYRIGHT__
  </div>
</div>
</body>
</html>
`;
}

fs.mkdirSync(OUT, { recursive: true });
let n = 0;
for (const manual of MANUALS) {
  for (const lang of LANGS) {
    const file = `${manual.id}-${lang}.html`;
    fs.writeFileSync(path.join(OUT, file), renderHtml(manual, lang), 'utf8');
    n++;
    console.log('wrote', file);
  }
}

const catalogPath = path.join(OUT, 'catalog.json');
let catalog = { version: VERSION, date: DATE, format: 'pdf', audiences: [], items: [] };
if (fs.existsSync(catalogPath)) {
  try { catalog = JSON.parse(fs.readFileSync(catalogPath, 'utf8')); } catch (e) { /* ignore */ }
}
catalog.version = VERSION;
catalog.date = DATE;
if (!Array.isArray(catalog.items)) catalog.items = [];
const ids = new Set(MANUALS.map((m) => m.id));
catalog.items = catalog.items.filter((it) => it && !ids.has(it.id));
for (const manual of MANUALS) {
  catalog.items.push({
    id: manual.id,
    audience: manual.audience,
    format: 'pdf',
    titles: manual.titles,
    pdfDir: `manuals/pdf/${manual.id}`
  });
}
fs.writeFileSync(catalogPath, JSON.stringify(catalog, null, 2), 'utf8');
console.log('Generated', n, 'HTML + updated catalog.json');
