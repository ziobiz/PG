/**
 * 가맹점 운영 매뉴얼 HTML × 5국어 생성
 * Run: node site/manuals/build-merchant-ops.mjs
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const OUT = path.join(__dirname, 'generated');
const VERSION = '2.51';
const DATE = '2026-07-22';
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

/** @type {{id:string,audience:string,titles:Record<string,string>,subs:Record<string,string>,sections:Record<string,Array<{h:string,html:string}>>}} */
const MANUAL = {
  id: 'merchant-ops',
  audience: 'merchant',
  titles: {
    ko: '가맹점 운영 메뉴얼',
    en: 'Merchant Operations Manual',
    ja: '加盟店 運営マニュアル',
    zh: '商户运营手册',
    th: 'คู่มือปฏิบัติการร้านค้า'
  },
  subs: {
    ko: '가맹점 권한 화면 기준 — 결제·수수료·결제 URL·일상 운영',
    en: 'Merchant-permission screens — payments, fees, payment URL, daily ops',
    ja: '加盟店権限画面 — 決済・手数料・決済URL・日常運用',
    zh: '按商户权限画面 — 支付、手续费、支付 URL、日常运营',
    th: 'ตามหน้าจอสิทธิ์ร้านค้า — ชำระ ค่าธรรมเนียม URL ชำระ งานประจำวัน'
  },
  sections: {
    ko: [
      {
        h: '이 매뉴얼의 범위',
        html: `<p>본 문서는 <strong>가맹점(MERCHANT)</strong> 계정으로 로그인했을 때 접근할 수 있는 관리자 화면을 기준으로, 기본 운영 방법을 설명합니다.</p>
<ul>
<li>실제로 보이는 메뉴는 <strong>본사정책 → 접근·권한(본사 권한)</strong>에서 가맹점 단계·개별 조직에 부여한 권한에 따라 달라집니다.</li>
<li>권한이 <strong>접근불가</strong>인 메뉴는 사이드바에 표시되지 않습니다. 필요 메뉴가 없으면 상위 본사·총판에 권한 개방을 요청하십시오.</li>
<li>결제대행사(PG) 이름은 가맹·구매자 화면에서 ICOPAY 중립으로 표시됩니다.</li>
</ul>`
      },
      {
        h: '로그인과 화면 구성',
        html: `<ol>
<li>관리자 사이트에 가맹점 로그인 ID로 접속합니다.</li>
<li>좌측 사이드바에 <strong>권한이 허용된 메뉴만</strong> 표시됩니다.</li>
<li>상단에서 표시 언어(KO/EN/JP/CH/TH)를 바꿀 수 있습니다.</li>
<li>대시보드(있을 경우)에서 최근 결제·요약 지표를 먼저 확인하는 것을 권장합니다.</li>
</ol>`
      },
      {
        h: '공지사항',
        html: `<p class="menu-path">업체관리 → 공지사항</p>
<p>본사·총판이 게시한 운영 공지를 확인합니다. 수수료·정산일·점검 안내가 공지로 공지되는 경우가 많으므로, 로그인 후 먼저 확인하십시오.</p>`
      },
      {
        h: '업체정보조회 — 결제 URL 확인',
        html: `<p class="menu-path">업체관리 → 업체정보조회</p>
<p>가맹점 본인 업체 상세입니다. 여기서 <strong>고객에게 전달할 결제 주소</strong>를 확인·복사합니다.</p>
<table>
<tr><th>항목</th><th>설명</th></tr>
<tr><td>결제 URL</td><td>공개 일회 결제 페이지. 예: <code>https://(서비스도메인)/checkout/업체코드</code>. <strong>[복사]</strong>로 클립보드에 넣습니다.</td></tr>
<tr><td>URL 재결제 URL</td><td>저장 카드 재결제용(본사·PG 설정이 켜져 있을 때만 표시).</td></tr>
<tr><td>챗봇결제 URL</td><td>챗봇 쇼핑·주문 진입용. 챗봇 사용 가맹만 해당.</td></tr>
<tr><td>분할결제 URL</td><td>분할결제 사용 ON인 가맹의 고객 신청 진입 주소.</td></tr>
</table>
<ol>
<li>업체정보조회를 연다.</li>
<li>웹결제(또는 결제 URL) 카드에서 <strong>결제 URL</strong> 값을 확인한다.</li>
<li><strong>[복사]</strong>를 눌러 SMS·메신저·쇼핑몰에 붙여넣는다.</li>
<li>테스트로 본인 브라우저에서 URL을 열어 결제창이 뜨는지 확인한다.</li>
</ol>
<p><strong>주의:</strong> 웹결제 사용여부가 미사용·중지이면 URL이 열리지 않거나 안내만 표시될 수 있습니다. 상위 일괄 중지도 확인하십시오.</p>`
      },
      {
        h: '결제내역 조회',
        html: `<p class="menu-path">결제관리 → 결제내역</p>
<p>승인·실패·취소·환불 등 <strong>거래 전체를 조회</strong>하는 기본 화면입니다. (본사 권한이 열려 있어야 메뉴가 보입니다.)</p>
<ul>
<li><strong>기간·상태·키워드</strong>로 검색한 뒤 [검색]을 누릅니다.</li>
<li>그리드에서 승인금액·상태·주문번호·구매자 정보를 확인합니다.</li>
<li>무효·환불 등 후속조치는 권한이 허용된 경우에만 결제내역에서 수행합니다.</li>
</ul>
<p>채널별 하위 메뉴(권한이 있을 때):</p>
<ul>
<li><strong>URL결제내역</strong> — 공개 결제 URL로 들어온 거래</li>
<li><strong>챗봇결제내역</strong> — 챗봇·카탈로그 결제</li>
<li><strong>구독결제내역 / 분할결제내역</strong> — 해당 상품을 쓰는 가맹</li>
<li>성공·실패·취소·무효·환불 전용 목록 — 상태별 빠른 조회</li>
</ul>`
      },
      {
        h: '수수료내역·일별수수료',
        html: `<p class="menu-path">정산관리 → 수수료내역 / 일별수수료</p>
<ul>
<li><strong>수수료내역</strong>: 거래별 또는 집계된 수수료·차감 내역을 확인합니다.</li>
<li><strong>일별수수료</strong>: 일자 단위로 수수료를 묶어 보고, 이상 일자를 점검합니다.</li>
</ul>
<p>수수료율·정산주기는 상위(본사·총판) 정책과 가맹 개별 설정에 따릅니다. 숫자가 예상과 다르면 결제내역의 승인액·상태와 함께 대조하십시오.</p>`
      },
      {
        h: '가맹점정산내역',
        html: `<p class="menu-path">정산관리 → 가맹점정산내역</p>
<p>정산 실행·배포 이후 가맹점에 반영된 <strong>정산 결과</strong>를 조회합니다. 정산일·지급 예정·보류 여부를 확인하고, 문의 시 해당 정산 일자·금액을 전달하면 처리가 빠릅니다.</p>`
      },
      {
        h: '가맹점 API',
        html: `<p class="menu-path">업체관리 → 가맹점API</p>
<p>쇼핑몰·앱 연동용 키·문서·샘플에 접근하는 포털입니다(권한이 열린 경우). API 연동은 <strong>ICOPAY 통합 checkout</strong> 경로를 사용하며, 응답의 결제대행사 식별은 ICOPAY로 중립화됩니다. 상세 스펙은 포털 내 문서를 따릅니다.</p>`
      },
      {
        h: '챗봇관리(사용 가맹)',
        html: `<p class="menu-path">챗봇관리 → 상품관리 / 주문관리 / 기본설정</p>
<p>챗봇결제를 쓰는 가맹만 해당합니다. 상품·가격·재고·주문을 관리하고, 업체정보조회의 <strong>챗봇결제 URL</strong>을 고객 채널에 공유합니다. 챗봇 사용이 꺼져 있으면 상품관리 메뉴가 숨겨질 수 있습니다.</p>`
      },
      {
        h: '운영매뉴얼 메뉴',
        html: `<p class="menu-path">운영관리 → 운영매뉴얼</p>
<p>본 문서를 포함한 가맹점용 매뉴얼을 언어별로 새 창에서 엽니다. 인쇄 또는 브라우저의 「PDF로 저장」으로 보관할 수 있습니다.</p>`
      },
      {
        h: '일상 운영 체크리스트',
        html: `<ol>
<li>공지사항 확인</li>
<li>업체정보조회에서 결제 URL 유효·복사 가능 여부 확인</li>
<li>결제내역에서 당일(또는 전일) 승인·실패 건수 점검</li>
<li>실패가 많으면 카드·리스크·고객 입력 오류 여부를 상위와 공유</li>
<li>수수료·정산 화면에서 이상 금액 유무 확인</li>
<li>필요 시 챗봇 주문·상품 상태 확인</li>
</ol>`
      },
      {
        h: '문의',
        html: `<p>화면이 없거나 권한이 부족하면 <strong>상위 본사·총판 운영 담당</strong>에게 메뉴 URL과 함께 권한 개방을 요청하십시오. 결제·정산 금액 이상은 해당 거래의 주문번호·승인일시를 함께 전달하면 됩니다.</p>
<p>문서 버전은 플랫폼 라이브 버전(V${VERSION})과 함께 관리됩니다.</p>`
      }
    ],
    en: [
      {
        h: 'Scope',
        html: `<p>This guide explains day-to-day operations for screens a <strong>Merchant (MERCHANT)</strong> login can open.</p>
<ul>
<li>Menus depend on <strong>HQ Policy → Access / HQ permissions</strong> for the merchant tier or org.</li>
<li>Menus set to <strong>No access</strong> are hidden. Ask your HQ/distributor to open them if needed.</li>
<li>Acquirer (PG) names stay neutral as ICOPAY on merchant/buyer UI.</li>
</ul>`
      },
      {
        h: 'Sign-in and layout',
        html: `<ol>
<li>Sign in to the admin site with the merchant login ID.</li>
<li>The left sidebar shows <strong>only permitted menus</strong>.</li>
<li>Switch UI language (KO/EN/JP/CH/TH) from the header when available.</li>
<li>Use the dashboard (if shown) for a quick payment summary.</li>
</ol>`
      },
      {
        h: 'Notices',
        html: `<p class="menu-path">Companies → Notices</p>
<p>Read HQ/distributor announcements (fees, settlement dates, maintenance). Check after every login.</p>`
      },
      {
        h: 'Company info — payment URL',
        html: `<p class="menu-path">Companies → Company info</p>
<p>Your merchant profile. Copy the <strong>customer payment address</strong> here.</p>
<table>
<tr><th>Field</th><th>Meaning</th></tr>
<tr><td>Payment URL</td><td>Public one-time checkout, e.g. <code>https://(service-domain)/checkout/{compCode}</code>. Use <strong>Copy</strong>.</td></tr>
<tr><td>URL re-pay URL</td><td>Saved-card re-pay (only if HQ/PG enables it).</td></tr>
<tr><td>Chatbot payment URL</td><td>Chatbot storefront entry (chatbot merchants).</td></tr>
<tr><td>Split-pay URL</td><td>Customer split-pay signup when split-pay is ON.</td></tr>
</table>
<ol>
<li>Open Company info.</li>
<li>Find <strong>Payment URL</strong> on the web-payment card.</li>
<li>Click <strong>Copy</strong> and share via SMS/chat/shop.</li>
<li>Open the URL yourself to verify the checkout page loads.</li>
</ol>
<p><strong>Note:</strong> If web pay is Off/Suspended (or HQ bulk-suspended), the URL may not accept payments.</p>`
      },
      {
        h: 'Payment list',
        html: `<p class="menu-path">Payments → Payment list</p>
<p>Primary screen for approvals, failures, cancels, refunds (menu must be permitted).</p>
<ul>
<li>Filter by period / status / keyword, then Search.</li>
<li>Check amount, status, order id, buyer fields in the grid.</li>
<li>Void/refund actions appear only when permission allows.</li>
</ul>
<p>Channel lists (when permitted): URL payments, Chatbot payments, Subscription / Split-pay lists, plus success/fail/cancel/void/refund shortcuts.</p>`
      },
      {
        h: 'Fee list & daily fees',
        html: `<p class="menu-path">Settlement → Fee list / Daily fees</p>
<ul>
<li><strong>Fee list</strong>: per-txn or aggregated fee lines.</li>
<li><strong>Daily fees</strong>: day buckets for fee monitoring.</li>
</ul>
<p>Rates and settlement cycles follow HQ/distributor policy and merchant overrides. Cross-check against Payment list amounts/status if figures look wrong.</p>`
      },
      {
        h: 'Merchant settlement list',
        html: `<p class="menu-path">Settlement → Merchant settlement</p>
<p>Results after settlement runs/distribution. Note settlement date, payable amount, and hold flags when contacting support.</p>`
      },
      {
        h: 'Merchant API portal',
        html: `<p class="menu-path">Companies → Merchant API</p>
<p>Keys, docs, and samples for shop/app integration (if permitted). Use ICOPAY unified checkout APIs; PG identity in responses is neutralized to ICOPAY.</p>`
      },
      {
        h: 'Chatbot admin (if used)',
        html: `<p class="menu-path">Chatbot → Products / Orders / Settings</p>
<p>Manage catalog and orders; share the Chatbot payment URL from Company info. Product menus may hide if chatbot is disabled.</p>`
      },
      {
        h: 'Ops manuals menu',
        html: `<p class="menu-path">Operations → Ops manuals</p>
<p>Open merchant manuals (including this one) in a new window by language. Print or Save as PDF from the browser.</p>`
      },
      {
        h: 'Daily checklist',
        html: `<ol>
<li>Read notices</li>
<li>Verify Payment URL on Company info</li>
<li>Review today’s (or yesterday’s) approvals/failures</li>
<li>Escalate unusual failure spikes with sample order ids</li>
<li>Scan fee/settlement screens for anomalies</li>
<li>Check chatbot orders/products if applicable</li>
</ol>`
      },
      {
        h: 'Support',
        html: `<p>If a menu is missing, ask HQ/distributor to grant that page URL. For amount disputes, send order id and approval time. Document version tracks live platform V${VERSION}.</p>`
      }
    ],
    ja: [
      {
        h: '対象範囲',
        html: `<p>本マニュアルは<strong>加盟店(MERCHANT)</strong>ログインで開ける管理画面の基本運用を説明します。</p>
<ul>
<li>表示メニューは<strong>本社政策→権限</strong>の加盟店段階・個別組織権限に従います。</li>
<li><strong>アクセス不可</strong>のメニューはサイドバーに出ません。必要なら上位へ権限開放を依頼してください。</li>
<li>加盟・購入者UIでは決済代行社名はICOPAYとして中立表示されます。</li>
</ul>`
      },
      {
        h: 'ログインと画面',
        html: `<ol>
<li>加盟店IDで管理画面にログインします。</li>
<li>左メニューには<strong>許可された項目のみ</strong>が出ます。</li>
<li>表示言語(KO/EN/JP/CH/TH)を切り替えられます。</li>
<li>ダッシュボードがあれば直近決済の概要を先に確認します。</li>
</ol>`
      },
      {
        h: 'お知らせ',
        html: `<p class="menu-path">業者管理 → お知らせ</p>
<p>本社・総代理の運営告知(手数料・精算日・メンテ等)を確認します。ログイン後まず確認してください。</p>`
      },
      {
        h: '業者情報照会 — 決済URL',
        html: `<p class="menu-path">業者管理 → 業者情報照会</p>
<p>自加盟店の詳細です。<strong>お客様へ渡す決済URL</strong>をここで確認・コピーします。</p>
<table>
<tr><th>項目</th><th>説明</th></tr>
<tr><td>決済URL</td><td>公開ワンタイム決済。例 <code>https://(サービスドメイン)/checkout/業者コード</code>。<strong>[コピー]</strong>で取得。</td></tr>
<tr><td>URL再決済URL</td><td>保存カード再決済(本社・PG設定ON時のみ)。</td></tr>
<tr><td>チャットボット決済URL</td><td>ボット店舗入口(利用加盟のみ)。</td></tr>
<tr><td>分割決済URL</td><td>分割決済ON時の顧客申込入口。</td></tr>
</table>
<ol>
<li>業者情報照会を開く</li>
<li>Web決済カードの<strong>決済URL</strong>を確認</li>
<li><strong>[コピー]</strong>して共有</li>
<li>自分のブラウザで開き決済画面が出るか確認</li>
</ol>
<p><strong>注意:</strong> Web決済が未使用・停止(または一括停止)だと決済できない場合があります。</p>`
      },
      {
        h: '決済一覧',
        html: `<p class="menu-path">決済管理 → 決済一覧</p>
<p>承認・失敗・取消・返金などを照会する基本画面です(権限が必要)。</p>
<ul>
<li>期間・状態・キーワードで検索</li>
<li>金額・状態・注文番号・購入者を確認</li>
<li>無効・返金は権限がある場合のみ実行</li>
</ul>
<p>チャネル別: URL決済一覧、チャットボット、購読/分割、成功/失敗/取消など。</p>`
      },
      {
        h: '手数料一覧・日別手数料',
        html: `<p class="menu-path">精算管理 → 手数料一覧 / 日別手数料</p>
<ul>
<li><strong>手数料一覧</strong>: 取引別・集計手数料</li>
<li><strong>日別手数料</strong>: 日単位の確認</li>
</ul>
<p>料率・精算周期は上位政策と加盟個別設定に従います。金額が想定外なら決済一覧と突合してください。</p>`
      },
      {
        h: '加盟店精算一覧',
        html: `<p class="menu-path">精算管理 → 加盟店精算一覧</p>
<p>精算実行・配信後の結果を照会します。問い合わせ時は精算日・金額を添えてください。</p>`
      },
      {
        h: '加盟店API',
        html: `<p class="menu-path">業者管理 → 加盟店API</p>
<p>連携キー・ドキュメント・サンプル用ポータル(権限がある場合)。ICOPAY統合checkoutを使用し、応答のPG識別はICOPAYに中立化されます。</p>`
      },
      {
        h: 'チャットボット管理(利用時)',
        html: `<p class="menu-path">チャットボット管理 → 商品/注文/基本設定</p>
<p>商品・注文を管理し、業者情報のチャットボット決済URLを共有します。未使用時は商品メニューが非表示になることがあります。</p>`
      },
      {
        h: '運営マニュアルメニュー',
        html: `<p class="menu-path">運用管理 → 運営マニュアル</p>
<p>本ドキュメントを含む加盟向けマニュアルを言語別に新窓で開きます。印刷またはPDF保存が可能です。</p>`
      },
      {
        h: '日常チェックリスト',
        html: `<ol>
<li>お知らせ確認</li>
<li>決済URLの有効性確認</li>
<li>当日/前日の承認・失敗件数</li>
<li>失敗急増時は注文番号例を添えて上位へ</li>
<li>手数料・精算の異常有無</li>
<li>ボット注文・商品(該当時)</li>
</ol>`
      },
      {
        h: '問い合わせ',
        html: `<p>メニューが無い場合は上位に当該URLの権限開放を依頼してください。金額照会は注文番号・承認日時を添付。文書版はライブ V${VERSION} と同期管理します。</p>`
      }
    ],
    zh: [
      {
        h: '适用范围',
        html: `<p>本文说明<strong>商户(MERCHANT)</strong>登录后可访问管理画面的基本操作。</p>
<ul>
<li>菜单取决于<strong>总部策略→权限</strong>中商户层级/个别组织的授权。</li>
<li><strong>不可访问</strong>的菜单不会出现在侧栏；需要时请向上级申请开放。</li>
<li>商户/买家界面中收单机构名称以 ICOPAY 中立展示。</li>
</ul>`
      },
      {
        h: '登录与界面',
        html: `<ol>
<li>使用商户账号登录管理站。</li>
<li>左侧仅显示<strong>已授权菜单</strong>。</li>
<li>可切换界面语言(KO/EN/JP/CH/TH)。</li>
<li>如有仪表盘，先查看近期支付摘要。</li>
</ol>`
      },
      {
        h: '公告',
        html: `<p class="menu-path">企业管理 → 公告</p>
<p>查看总部/总代理公告（手续费、结算日、维护等）。每次登录后请先阅读。</p>`
      },
      {
        h: '企业信息 — 支付 URL',
        html: `<p class="menu-path">企业管理 → 企业信息查询</p>
<p>本商户详情。在此确认并<strong>复制给客户的支付地址</strong>。</p>
<table>
<tr><th>字段</th><th>说明</th></tr>
<tr><td>支付 URL</td><td>公开一次性结账，例如 <code>https://(服务域名)/checkout/商户代码</code>。点<strong>[复制]</strong>。</td></tr>
<tr><td>URL 再支付 URL</td><td>保存卡再支付（总部/PG 开启时才显示）。</td></tr>
<tr><td>聊天机器人支付 URL</td><td>机器人商城入口（启用商户）。</td></tr>
<tr><td>分期支付 URL</td><td>开启分期时的客户申请入口。</td></tr>
</table>
<ol>
<li>打开企业信息查询</li>
<li>在网页支付卡片查看<strong>支付 URL</strong></li>
<li>点<strong>[复制]</strong>并分享</li>
<li>自行打开链接确认结账页可显示</li>
</ol>
<p><strong>注意:</strong> 网页支付为未使用/暂停（或总部批量暂停）时可能无法支付。</p>`
      },
      {
        h: '支付列表',
        html: `<p class="menu-path">支付管理 → 支付列表</p>
<p>查询成功/失败/取消/退款等交易的主画面（需权限）。</p>
<ul>
<li>按期间、状态、关键词搜索</li>
<li>核对金额、状态、订单号、买家信息</li>
<li>作废/退款仅在有权限时可用</li>
</ul>
<p>渠道列表：URL 支付、机器人、订阅/分期，以及成功/失败等快捷列表。</p>`
      },
      {
        h: '手续费明细与按日手续费',
        html: `<p class="menu-path">结算管理 → 手续费明细 / 按日手续费</p>
<ul>
<li><strong>手续费明细</strong>：按交易或汇总</li>
<li><strong>按日手续费</strong>：按日排查</li>
</ul>
<p>费率与结算周期遵循上级政策及商户覆盖设置；异常时请与支付列表对照。</p>`
      },
      {
        h: '商户结算明细',
        html: `<p class="menu-path">结算管理 → 商户结算明细</p>
<p>查看结算执行/下发后的结果。咨询时请附上结算日与金额。</p>`
      },
      {
        h: '商户 API',
        html: `<p class="menu-path">企业管理 → 商户 API</p>
<p>密钥、文档与示例门户（有权限时）。使用 ICOPAY 统一 checkout；响应中的 PG 标识中立为 ICOPAY。</p>`
      },
      {
        h: '聊天机器人管理（如使用）',
        html: `<p class="menu-path">聊天机器人 → 商品 / 订单 / 基本设置</p>
<p>管理商品与订单，并分享企业信息中的机器人支付 URL。未启用时商品菜单可能隐藏。</p>`
      },
      {
        h: '运营手册菜单',
        html: `<p class="menu-path">运营管理 → 运营手册</p>
<p>按语言在新窗口打开商户手册（含本文）。可用浏览器打印或另存 PDF。</p>`
      },
      {
        h: '日常检查清单',
        html: `<ol>
<li>阅读公告</li>
<li>确认支付 URL</li>
<li>核对当日/昨日成功与失败</li>
<li>失败激增时附订单号上报</li>
<li>检查手续费/结算异常</li>
<li>如适用，检查机器人订单与商品</li>
</ol>`
      },
      {
        h: '联系支持',
        html: `<p>缺少菜单时请上级开放对应 URL 权限。金额争议请附订单号与批准时间。文档版本随线上 V${VERSION} 管理。</p>`
      }
    ],
    th: [
      {
        h: 'ขอบเขต',
        html: `<p>คู่มือนี้อธิบายการใช้งานหน้าจอที่บัญชี<strong>ร้านค้า (MERCHANT)</strong> เข้าถึงได้</p>
<ul>
<li>เมนูขึ้นกับ<strong>นโยบาย HQ → สิทธิ์</strong>ของระดับร้านหรือองค์กรนั้น</li>
<li>เมนูที่ตั้ง<strong>ห้ามเข้า</strong>จะไม่โชว์ในแถบซ้าย — ขอเปิดสิทธิ์จาก HQ/ตัวแทน</li>
<li>ชื่อผู้ให้บริการชำระเงินใน UI ร้าน/ผู้ซื้อแสดงเป็น ICOPAY แบบกลาง</li>
</ul>`
      },
      {
        h: 'เข้าสู่ระบบและหน้าจอ',
        html: `<ol>
<li>เข้าสู่ระบบด้วยรหัสร้านค้า</li>
<li>แถบซ้ายแสดง<strong>เฉพาะเมนูที่อนุญาต</strong></li>
<li>เปลี่ยนภาษา UI (KO/EN/JP/CH/TH) ได้</li>
<li>ถ้ามีแดชบอร์ด ให้ดูสรุปการชำระล่าสุดก่อน</li>
</ol>`
      },
      {
        h: 'ประกาศ',
        html: `<p class="menu-path">จัดการบริษัท → ประกาศ</p>
<p>อ่านประกาศจาก HQ/ตัวแทน (ค่าธรรมเนียม วันชำระ ปิดปรับปรุง) ทุกครั้งหลังล็อกอิน</p>`
      },
      {
        h: 'ข้อมูลบริษัท — URL ชำระเงิน',
        html: `<p class="menu-path">จัดการบริษัท → ดูข้อมูลบริษัท</p>
<p>รายละเอียดร้านของคุณ — คัดลอก<strong>ที่อยู่ชำระสำหรับลูกค้า</strong>ที่นี่</p>
<table>
<tr><th>รายการ</th><th>ความหมาย</th></tr>
<tr><td>URL ชำระ</td><td>หน้าชำระครั้งเดียวสาธารณะ เช่น <code>https://(โดเมนบริการ)/checkout/รหัสร้าน</code> กด<strong>[คัดลอก]</strong></td></tr>
<tr><td>URL ชำระซ้ำ</td><td>ชำระด้วยบัตรที่บันทึก (เมื่อ HQ/PG เปิด)</td></tr>
<tr><td>URL ชำระแชทบอท</td><td>ทางเข้าร้านแชทบอท</td></tr>
<tr><td>URL แบ่งจ่าย</td><td>ทางสมัครแบ่งจ่ายเมื่อเปิดใช้</td></tr>
</table>
<ol>
<li>เปิดดูข้อมูลบริษัท</li>
<li>ดู<strong>URL ชำระ</strong>ในการ์ดชำระเว็บ</li>
<li>กด<strong>[คัดลอก]</strong>แล้วส่งต่อ</li>
<li>เปิดลิงก์เองเพื่อตรวจว่าหน้าชำระขึ้น</li>
</ol>
<p><strong>หมายเหตุ:</strong> ถ้าชำระเว็บปิด/ระงับ (หรือ HQ ระงับรวม) อาจชำระไม่ได้</p>`
      },
      {
        h: 'รายการชำระเงิน',
        html: `<p class="menu-path">การชำระเงิน → รายการชำระเงิน</p>
<p>หน้ารายการหลักสำหรับสำเร็จ/ล้มเหลว/ยกเลิก/คืนเงิน (ต้องมีสิทธิ์)</p>
<ul>
<li>กรองช่วงวัน สถานะ คำค้น แล้วค้นหา</li>
<li>ดูจำนวนเงิน สถานะ เลขคำสั่ง ผู้ซื้อ</li>
<li>โมฆะ/คืนเงินทำได้เมื่อมีสิทธิ์</li>
</ul>
<p>รายการตามช่องทาง: URL, แชทบอท, สมัครสมาชิก/แบ่งจ่าย และทางลัดสำเร็จ/ล้มเหลว ฯลฯ</p>`
      },
      {
        h: 'รายการค่าธรรมเนียมและรายวัน',
        html: `<p class="menu-path">การชำระผล → รายการค่าธรรมเนียม / ค่าธรรมเนียมรายวัน</p>
<ul>
<li><strong>รายการค่าธรรมเนียม</strong>: รายธุรกรรมหรือรวม</li>
<li><strong>รายวัน</strong>: ตรวจเป็นรายวัน</li>
</ul>
<p>อัตราและรอบชำระตามนโยบายต้นสังกัดและการตั้งค่าร้าน — หากผิดปกติให้เทียบกับรายการชำระ</p>`
      },
      {
        h: 'รายการชำระผลร้านค้า',
        html: `<p class="menu-path">การชำระผล → รายการชำระผลร้านค้า</p>
<p>ดูผลหลังรัน/แจกจ่ายการชำระผล แจ้งวันและยอดเมื่อติดต่อฝ่ายสนับสนุน</p>`
      },
      {
        h: 'API ร้านค้า',
        html: `<p class="menu-path">จัดการบริษัท → API ร้านค้า</p>
<p>พอร์ทัลคีย์ เอกสาร ตัวอย่าง (ถ้ามีสิทธิ์) ใช้ checkout รวมของ ICOPAY และทำให้ตัวตน PG เป็นกลางเป็น ICOPAY</p>`
      },
      {
        h: 'จัดการแชทบอท (ถ้าใช้)',
        html: `<p class="menu-path">แชทบอท → สินค้า / คำสั่ง / ตั้งค่า</p>
<p>จัดการสินค้าและคำสั่ง แล้วแชร์ URL ชำระแชทบอทจากข้อมูลบริษัท หากปิดใช้ เมนูสินค้าอาจหาย</p>`
      },
      {
        h: 'เมนูคู่มือปฏิบัติการ',
        html: `<p class="menu-path">การปฏิบัติการ → คู่มือปฏิบัติการ</p>
<p>เปิดคู่มือร้านค้า (รวมฉบับนี้) ตามภาษาในหน้าต่างใหม่ พิมพ์หรือบันทึก PDF จากเบราว์เซอร์ได้</p>`
      },
      {
        h: 'เช็คลิสต์ประจำวัน',
        html: `<ol>
<li>อ่านประกาศ</li>
<li>ตรวจ URL ชำระ</li>
<li>ดูสำเร็จ/ล้มเหลววันนี้หรือเมื่อวาน</li>
<li>ถ้าล้มเหลวพุ่ง ส่งตัวอย่างเลขคำสั่ง</li>
<li>ตรวจค่าธรรมเนียม/ชำระผลผิดปกติ</li>
<li>ตรวจคำสั่ง/สินค้าแชทบอท (ถ้ามี)</li>
</ol>`
      },
      {
        h: 'ติดต่อช่วยเหลือ',
        html: `<p>ถ้าไม่มีเมนู ให้ขอเปิดสิทธิ์ URL นั้นจากต้นสังกัด ข้อพิพาทยอดให้แนบเลขคำสั่งและเวลาอนุมัติ เวอร์ชันเอกสารตามไลฟ์ V${VERSION}</p>`
      }
    ]
  }
};

function renderHtml(lang) {
  const ui = UI[lang];
  const title = MANUAL.titles[lang];
  const sub = MANUAL.subs[lang];
  const sections = MANUAL.sections[lang];
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
for (const lang of LANGS) {
  const file = `${MANUAL.id}-${lang}.html`;
  fs.writeFileSync(path.join(OUT, file), renderHtml(lang), 'utf8');
  n++;
  console.log('wrote', file);
}

// merge into catalog.json if present
const catalogPath = path.join(OUT, 'catalog.json');
let catalog = { version: VERSION, date: DATE, format: 'mixed', audiences: [], items: [] };
if (fs.existsSync(catalogPath)) {
  try { catalog = JSON.parse(fs.readFileSync(catalogPath, 'utf8')); } catch (e) { /* ignore */ }
}
catalog.version = VERSION;
catalog.date = DATE;
if (!Array.isArray(catalog.items)) catalog.items = [];
catalog.items = catalog.items.filter((it) => it && it.id !== MANUAL.id);
catalog.items.push({
  id: MANUAL.id,
  audience: MANUAL.audience,
  format: 'html',
  titles: MANUAL.titles,
  pathPrefix: `manuals/generated/${MANUAL.id}`
});
fs.writeFileSync(catalogPath, JSON.stringify(catalog, null, 2), 'utf8');
console.log('Generated', n, 'HTML + updated catalog.json');
