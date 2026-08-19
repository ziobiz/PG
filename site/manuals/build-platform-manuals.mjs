/**
 * Generate platform ops manuals (HTML × 5 langs) under site/manuals/generated/
 * Run: node site/manuals/build-platform-manuals.mjs
 * Version must match ICOPAY_PLATFORM_RELEASE.currentLiveVersion
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const OUT = path.join(__dirname, 'generated');
const VERSION = '2.76';
const DATE = '2026-07-24';
const LANGS = ['ko', 'en', 'ja', 'zh', 'th'];

const UI = {
  ko: { print: '인쇄 / PDF 저장', menu: '메뉴', toc: '목차', version: '문서 버전', related: '관련', brandFallback: 'ICOPAY' },
  en: { print: 'Print / Save PDF', menu: 'Menu', toc: 'Contents', version: 'Document version', related: 'Related', brandFallback: 'ICOPAY' },
  ja: { print: '印刷 / PDF保存', menu: 'メニュー', toc: '目次', version: '文書バージョン', related: '関連', brandFallback: 'ICOPAY' },
  zh: { print: '打印 / 保存 PDF', menu: '菜单', toc: '目录', version: '文档版本', related: '相关', brandFallback: 'ICOPAY' },
  th: { print: 'พิมพ์ / บันทึก PDF', menu: 'เมนู', toc: 'สารบัญ', version: 'เวอร์ชันเอกสาร', related: 'เกี่ยวข้อง', brandFallback: 'ICOPAY' }
};

/** @typedef {{id:string,audience:string,titles:Record<string,string>,subs:Record<string,string>,sections:Record<string,Array<{h:string,html:string}>>}} Manual */

/** @type {Manual[]} */
const MANUALS = [
  {
    id: 'super-ops',
    audience: 'super',
    titles: {
      ko: '총본사 운영 메뉴얼',
      en: 'Super HQ Operations Manual',
      ja: '総本部 運営マニュアル',
      zh: '总本部运营手册',
      th: 'คู่มือปฏิบัติการสำนักงานใหญ่สูงสุด'
    },
    subs: {
      ko: '플랫폼·권한·PG·정산·배포 등 총본사 전역 운영',
      en: 'Platform-wide ops: access, PG, settlement, deploy',
      ja: '権限・PG・精算・配信など総本部の全体運用',
      zh: '权限、PG、结算、部署等总本部全局运营',
      th: 'สิทธิ์ PG ชำระเงิน ดีพลอย — การดำเนินงานทั้งแพลตฟอร์ม'
    },
    sections: {
      ko: [
        { h: '역할', html: '<p>총본사(HEADQUARTERS)는 <strong>본사정책</strong>·<strong>연동·배포</strong> 허브와 전 조직 권한을 관리합니다. 본사·총판·가맹 운영 매뉴얼과 함께 사용하십시오.</p>' },
        { h: '주요 메뉴', html: '<ul><li>본사정책 → 플랫폼(전산·도메인·서버·업데이트 내용)</li><li>운영관리 → <strong>운영매뉴얼</strong>(통합리포트 아래)</li><li>접근·권한 · 수수료·리스크 · AI·챗봇</li><li>연동·배포 → 결제대행사 설정 · 가맹 API 출시</li><li>업체관리 · 결제·정산</li></ul>' },
        { h: '권장 운영 순서', html: '<ol><li>총본사 기본정보·브랜딩(로고·사이트명) 확인</li><li>본사 권한·조직 단계 메뉴 권한</li><li>PG·노티·리스크 기본값</li><li>하위 본사/총판 등록 후 가맹 온보딩</li></ol>' },
        { h: '버전', html: '<p>본 문서는 플랫폼 라이브 버전과 동일하게 관리됩니다. <strong>본사정책 → 플랫폼 → 업데이트 내용</strong>과 맞춰 확인하십시오.</p>' }
      ],
      en: [
        { h: 'Role', html: '<p>Super HQ manages <strong>HQ Policy</strong> and <strong>Integration &amp; Deploy</strong> hubs and org-wide permissions.</p>' },
        { h: 'Key menus', html: '<ul><li>HQ Policy → Platform (ledger, domain, server, releases)</li><li>Operations → <strong>Ops manuals</strong> (below Integrated report)</li><li>Access · Fees &amp; Risk · AI/Chatbot</li><li>Integration &amp; Deploy → Payment agency settings · Merchant API launch</li></ul>' },
        { h: 'Suggested flow', html: '<ol><li>HQ basic info &amp; branding</li><li>Permissions</li><li>PG / NOTI / risk defaults</li><li>Register HQ/distributor then merchants</li></ol>' },
        { h: 'Version', html: '<p>Kept in sync with the live platform version under Platform → Release notes.</p>' }
      ],
      ja: [
        { h: '役割', html: '<p>総本部は<strong>本社政策</strong>・<strong>連携・配信</strong>と全組織権限を管理します。</p>' },
        { h: '主要メニュー', html: '<ul><li>本社政策 → プラットフォーム（電算・ドメイン・サーバー・更新内容）</li><li>運用管理 → <strong>運営マニュアル</strong>（統合レポート下）</li><li>権限・手数料リスク・AI</li><li>連携・配信</li></ul>' },
        { h: '推奨手順', html: '<ol><li>基本情報・ブランディング</li><li>権限</li><li>PG・NOTI・リスク既定</li><li>下位組織→加盟店</li></ol>' },
        { h: 'バージョン', html: '<p>ライブ版と同期管理します。</p>' }
      ],
      zh: [
        { h: '角色', html: '<p>总本部管理<strong>总部策略</strong>与<strong>对接·部署</strong>及全组织权限。</p>' },
        { h: '主要菜单', html: '<ul><li>总部策略 → 平台（账务·域名·服务器·更新内容）</li><li>运营管理 → <strong>运营手册</strong>（综合报表下方）</li><li>权限 · 手续费风险 · AI</li><li>对接·部署</li></ul>' },
        { h: '建议流程', html: '<ol><li>基本信息与品牌</li><li>权限</li><li>PG/通知/风险默认</li><li>下级组织→商户</li></ol>' },
        { h: '版本', html: '<p>与平台线上版本同步管理。</p>' }
      ],
      th: [
        { h: 'บทบาท', html: '<p>สำนักงานใหญ่สูงสุดจัดการ<strong>นโยบาย HQ</strong> และ<strong>เชื่อมต่อ·ดีพลอย</strong> รวมสิทธิ์ทั้งองค์กร</p>' },
        { h: 'เมนูหลัก', html: '<ul><li>นโยบาย HQ → แพลตฟอร์ม (บัญชี·โดเมน·เซิร์ฟเวอร์·ประวัติอัปเดต)</li><li>การปฏิบัติการ → <strong>คู่มือปฏิบัติการ</strong> (ใต้รายงานรวม)</li><li>สิทธิ์ · ค่าธรรมเนียม/ความเสี่ยง · AI</li><li>เชื่อมต่อ·ดีพลอย</li></ul>' },
        { h: 'ลำดับแนะนำ', html: '<ol><li>ข้อมูลพื้นฐานและแบรนด์</li><li>สิทธิ์</li><li>ค่าเริ่มต้น PG/NOTI/ความเสี่ยง</li><li>องค์กรย่อย→ร้านค้า</li></ol>' },
        { h: 'เวอร์ชัน', html: '<p>จัดการให้ตรงกับเวอร์ชันสดของแพลตฟอร์ม</p>' }
      ]
    }
  },
  {
    id: 'super-org-reg',
    audience: 'super',
    titles: {
      ko: '신규 조직 등록 메뉴얼',
      en: 'New Organization Registration Manual',
      ja: '新規組織登録マニュアル',
      zh: '新组织注册手册',
      th: 'คู่มือลงทะเบียนองค์กรใหม่'
    },
    subs: {
      ko: '총본사 → 본사 → 총판 → … → 가맹 전체 등록 흐름',
      en: 'Full tree: Super HQ → HQ → Distributor → … → Merchant',
      ja: '総本部→本社→総代理→…→加盟の全体登録',
      zh: '总本部→总部→总代理→…→商户全流程',
      th: 'ทั้งสาย: สำนักงานใหญ่→HQ→ตัวแทน→…→ร้านค้า'
    },
    sections: {
      ko: [
        { h: '조직 단계', html: '<p>총본사(HEADQUARTERS) → 본사(REGIONAL) → 총판(MASTER_DIST) → 지사/대리점/영업점 → 가맹점(MERCHANT).</p>' },
        { h: '등록 화면', html: '<p class="menu-path">업체관리 → 업체등록 / 업체관리</p><ol><li>상위 조직 선택</li><li>업체코드·업체명·구분·통화·정산 정보</li><li>로그인 ID·사용여부</li><li>저장 후 권한·수수료·PG 바인딩</li></ol>' },
        { h: '본사 추가 시', html: '<ul><li>총본사 로그인만 본사 추가 가능(정책에 따름)</li><li>기준 화폐 최대 3종</li><li>영업일·VIEW SETTING·허용 정산주기 상속 확인</li></ul>' },
        { h: '총판·하위', html: '<p>총판 등록 후 산하 가맹은 「신규가맹점 추가 메뉴얼」을 따릅니다. 위험 정책 기본은 <strong>본사정책 따름</strong>.</p>' },
        { h: '점검', html: '<ul><li>로그인·메뉴 노출</li><li>브랜딩(본사/총판)</li><li>결제·정산 테스트 1건</li></ul>' }
      ],
      en: [
        { h: 'Org levels', html: '<p>HEADQUARTERS → REGIONAL → MASTER_DIST → branch/agency/sales → MERCHANT.</p>' },
        { h: 'Screens', html: '<p class="menu-path">Companies → Register / Manage</p><ol><li>Select parent</li><li>Code, name, type, currency, settlement</li><li>Login &amp; use flag</li><li>Then permissions, fees, PG binding</li></ol>' },
        { h: 'Adding HQ', html: '<ul><li>Usually Super HQ login only</li><li>Up to 3 base currencies</li><li>Check business day / VIEW / settlement inheritance</li></ul>' },
        { h: 'Distributor &amp; below', html: '<p>Merchants follow the New Merchant manual. Default risk = Follow HQ.</p>' },
        { h: 'Checklist', html: '<ul><li>Login &amp; menus</li><li>Branding</li><li>One payment/settlement test</li></ul>' }
      ],
      ja: [
        { h: '組織段階', html: '<p>総本部→本社→総代理→支店等→加盟店。</p>' },
        { h: '画面', html: '<p class="menu-path">業者管理 → 登録/管理</p><ol><li>上位選択</li><li>コード・名・区分・通貨・精算</li><li>ログイン</li><li>権限・手数料・PG</li></ol>' },
        { h: '本社追加', html: '<ul><li>総本部ログイン中心</li><li>基準通貨最大3</li><li>営業日・VIEW・精算周期</li></ul>' },
        { h: '総代理以下', html: '<p>加盟は新規加盟マニュアル。リスク既定は本社に従う。</p>' },
        { h: '点検', html: '<ul><li>ログイン</li><li>ブランディング</li><li>決済1件</li></ul>' }
      ],
      zh: [
        { h: '组织层级', html: '<p>总本部→总部→总代理→分支等→商户。</p>' },
        { h: '画面', html: '<p class="menu-path">企业管理 → 注册/管理</p><ol><li>选择上级</li><li>代码、名称、类型、货币、结算</li><li>登录</li><li>权限、手续费、PG</li></ol>' },
        { h: '新增总部', html: '<ul><li>通常仅总本部登录</li><li>基准货币最多3种</li><li>营业日/VIEW/结算周期</li></ul>' },
        { h: '总代理以下', html: '<p>商户见《新商户添加手册》。风险默认遵循总部。</p>' },
        { h: '检查', html: '<ul><li>登录与菜单</li><li>品牌</li><li>一笔支付测试</li></ul>' }
      ],
      th: [
        { h: 'ระดับองค์กร', html: '<p>สำนักงานใหญ่สูงสุด→HQ→ตัวแทน→สาขาฯ→ร้านค้า</p>' },
        { h: 'หน้าจอ', html: '<p class="menu-path">จัดการบริษัท → ลงทะเบียน/จัดการ</p><ol><li>เลือกต้นสังกัด</li><li>รหัส ชื่อ ประเภท สกุลเงิน ชำระ</li><li>ล็อกอิน</li><li>สิทธิ์ ค่าธรรมเนียม PG</li></ol>' },
        { h: 'เพิ่ม HQ', html: '<ul><li>มักใช้ล็อกอินสำนักงานใหญ่สูงสุด</li><li>สกุลเงินฐานสูงสุด 3</li><li>วันทำการ/VIEW/รอบชำระ</li></ul>' },
        { h: 'ตัวแทนลงไป', html: '<p>ร้านค้าดูคู่มือเพิ่มร้านใหม่ ความเสี่ยงเริ่มต้น=ตาม HQ</p>' },
        { h: 'ตรวจ', html: '<ul><li>ล็อกอิน</li><li>แบรนด์</li><li>ทดสอบชำระ 1 รายการ</li></ul>' }
      ]
    }
  },
  {
    id: 'super-risk',
    audience: 'super',
    titles: {
      ko: '리스크관리 메뉴얼',
      en: 'Risk Management Manual',
      ja: 'リスク管理マニュアル',
      zh: '风险管理手册',
      th: 'คู่มือจัดการความเสี่ยง'
    },
    subs: {
      ko: '리스크설정 · 리스크 필터링 · 현황 · 비활성카드',
      en: 'Risk Settings, Filtering, dashboard, inactive cards',
      ja: 'リスク設定・フィルタ・状況・非活性カード',
      zh: '风险设置、过滤、看板、非活跃卡',
      th: 'ตั้งค่าความเสี่ยง ตัวกรอง แดชบอร์ด บัตรปิดใช้'
    },
    sections: {
      ko: [
        { h: '화면', html: '<p class="menu-path">본사정책 → 수수료·리스크 → 리스크</p><p>「리스크설정」= 카드 비성공 누적 쿨다운·자동 비활성. 「리스크 필터링」= PG 송부 전 사전 차단.</p>' },
        { h: '기본값(요약)', html: '<table><tr><th>항목</th><th>기본</th></tr><tr><td>1~3차 대기</td><td>5분 / 10분 / 1시간</td></tr><tr><td>자동 비활성</td><td>4회</td></tr><tr><td>카드 속도</td><td>10분 / 3회</td></tr><tr><td>이메일 속도</td><td>30분 / 5회</td></tr><tr><td>IP 속도</td><td>15분 / 10회</td></tr></table><p>상세·트리거 순서: 본사 트리거 운영 매뉴얼 · 총판용 설명안(운영관리 → 운영매뉴얼).</p>' },
        { h: '확인', html: '<ul><li>운영관리 → 리스크 현황</li><li>운영관리 → 비활성카드</li><li>가맹점 리스크 현황(본사 따름/별도/미사용)</li></ul>' },
        { h: '운영 팁', html: '<p>1회 실패 ≠ 영구 차단. 성공 시 쿨다운 횟수 초기화. 필터 스위치 미사용이면 해당 검사만 제외.</p>' }
      ],
      en: [
        { h: 'Screen', html: '<p class="menu-path">HQ Policy → Fees &amp; Risk → Risk</p><p>Settings = cooldowns; Filtering = pre-PG blocks.</p>' },
        { h: 'Defaults', html: '<table><tr><th>Item</th><th>Default</th></tr><tr><td>Tiers 1–3</td><td>5 / 10 / 60 min</td></tr><tr><td>Auto inactive</td><td>4</td></tr><tr><td>Card velocity</td><td>10m / 3</td></tr><tr><td>Email</td><td>30m / 5</td></tr><tr><td>IP</td><td>15m / 10</td></tr></table>' },
        { h: 'Where to check', html: '<ul><li>Risk dashboard</li><li>Inactive cards</li><li>Merchant risk mode</li></ul>' },
        { h: 'Tips', html: '<p>One failure ≠ permanent ban. Success resets count.</p>' }
      ],
      ja: [
        { h: '画面', html: '<p class="menu-path">本社政策 → 手数料・リスク → リスク</p>' },
        { h: '既定値', html: '<table><tr><th>項目</th><th>既定</th></tr><tr><td>1〜3次</td><td>5/10/60分</td></tr><tr><td>自動非活</td><td>4回</td></tr><tr><td>カード速度</td><td>10分/3</td></tr><tr><td>メール</td><td>30分/5</td></tr><tr><td>IP</td><td>15分/10</td></tr></table>' },
        { h: '確認', html: '<ul><li>リスク状況</li><li>非活性カード</li></ul>' },
        { h: 'ヒント', html: '<p>1回失敗≠永久停止。成功でリセット。</p>' }
      ],
      zh: [
        { h: '画面', html: '<p class="menu-path">总部策略 → 手续费·风险 → 风险</p>' },
        { h: '默认', html: '<table><tr><th>项</th><th>默认</th></tr><tr><td>1–3 阶</td><td>5/10/60 分</td></tr><tr><td>自动非活跃</td><td>4</td></tr><tr><td>卡速度</td><td>10分/3</td></tr><tr><td>邮箱</td><td>30分/5</td></tr><tr><td>IP</td><td>15分/10</td></tr></table>' },
        { h: '核对', html: '<ul><li>风险看板</li><li>非活跃卡</li></ul>' },
        { h: '提示', html: '<p>失败一次≠永久封禁；成功重置。</p>' }
      ],
      th: [
        { h: 'หน้าจอ', html: '<p class="menu-path">นโยบาย HQ → ค่าธรรมเนียม·ความเสี่ยง → ความเสี่ยง</p>' },
        { h: 'ค่าเริ่มต้น', html: '<table><tr><th>รายการ</th><th>ค่าเริ่ม</th></tr><tr><td>ขั้น 1–3</td><td>5/10/60 นาที</td></tr><tr><td>ปิดใช้อัตโนมัติ</td><td>4</td></tr><tr><td>ความเร็วบัตร</td><td>10น./3</td></tr><tr><td>อีเมล</td><td>30น./5</td></tr><tr><td>IP</td><td>15น./10</td></tr></table>' },
        { h: 'ตรวจ', html: '<ul><li>ภาพรวมความเสี่ยง</li><li>บัตรปิดใช้</li></ul>' },
        { h: 'ทิป', html: '<p>ล้มเหลวครั้งเดียว≠แบนถาวร สำเร็จแล้วรีเซ็ต</p>' }
      ]
    }
  },
  {
    id: 'hqdist-ops',
    audience: 'hqdist',
    titles: {
      ko: '본사 및 총판 운영 메뉴얼',
      en: 'HQ &amp; Distributor Operations Manual',
      ja: '本社・総代理店 運営マニュアル',
      zh: '总部与总代理运营手册',
      th: 'คู่มือปฏิบัติการ HQ และตัวแทนจำหน่าย'
    },
    subs: {
      ko: '가맹 등록부터 결제·정산·챗봇·분할·구독까지 전 기능',
      en: 'All features: merchants, payments, settlement, chatbot, split, subscription',
      ja: '加盟・決済・精算・チャットボット・分割・購読まで',
      zh: '商户、支付、结算、聊天机器人、分期、订阅全功能',
      th: 'ครบ: ร้านค้า ชำระ ชำระเงิน chatbot แบ่งจ่าย สมัครสมาชิก'
    },
    sections: {
      ko: [
        { h: '범위', html: '<p>본사(REGIONAL)·총판(MASTER_DIST) 운영자용. 총본사 전용(본사정책·연동배포 허브)은 제외하고, 업체·결제·정산·운영·챗봇·분할을 다룹니다.</p>' },
        { h: '업체·가맹', html: '<ul><li>업체관리: 하위 조직·가맹 조회/수정</li><li>수수료관리 · 리스크관리 트리거(본사 따름/별도/미사용)</li><li>웹결제·URL·API 인라인 · 구독·재구매·분할·챗봇 사용여부</li></ul>' },
        { h: '결제·정산', html: '<ul><li>결제관리: 결제내역·성공/실패/환불/무효 등</li><li>정산관리: 가맹·유통망 정산·정산실행</li><li>운영관리: 리스크 현황·비활성카드·노티관리(권한 시)</li></ul>' },
        { h: '상세 매뉴얼', html: '<p>신규가맹점 · 챗봇결제 · 정기(구독)결제 · 분할결제 · 리스크 트리거 소개는 「운영관리 → 운영매뉴얼」의 각 문서를 사용하십시오.</p>' }
      ],
      en: [
        { h: 'Scope', html: '<p>For REGIONAL &amp; MASTER_DIST. Excludes Super-HQ-only hubs; covers companies, pay, settlement, ops, chatbot, split.</p>' },
        { h: 'Merchants', html: '<ul><li>Company manage</li><li>Fees · risk trigger mode</li><li>Web/URL/API · subscribe · repay · split · chatbot flags</li></ul>' },
        { h: 'Pay &amp; settle', html: '<ul><li>Payment lists</li><li>Settlement</li><li>Risk dashboard / inactive cards / NOTI (if allowed)</li></ul>' },
        { h: 'Detail manuals', html: '<p>Use the sibling manuals in the Ops manuals tab.</p>' }
      ],
      ja: [
        { h: '範囲', html: '<p>本社・総代理向け。総本部専用ハブは除外。</p>' },
        { h: '加盟', html: '<ul><li>業者管理</li><li>手数料・リスクモード</li><li>各種決済フラグ</li></ul>' },
        { h: '決済・精算', html: '<ul><li>決済一覧</li><li>精算</li><li>リスク状況等</li></ul>' },
        { h: '詳細', html: '<p>同一タブの各マニュアルを参照。</p>' }
      ],
      zh: [
        { h: '范围', html: '<p>面向总部与总代理；不含总本部专用枢纽。</p>' },
        { h: '商户', html: '<ul><li>企业管理</li><li>手续费与风险模式</li><li>各类支付开关</li></ul>' },
        { h: '支付与结算', html: '<ul><li>支付列表</li><li>结算</li><li>风险看板等</li></ul>' },
        { h: '详细', html: '<p>见同一「运营手册」页其他文档。</p>' }
      ],
      th: [
        { h: 'ขอบเขต', html: '<p>สำหรับ HQ และตัวแทน ไม่รวมฮับเฉพาะสำนักงานใหญ่สูงสุด</p>' },
        { h: 'ร้านค้า', html: '<ul><li>จัดการบริษัท</li><li>ค่าธรรมเนียม/โหมดความเสี่ยง</li><li>สวิตช์ชำระต่างๆ</li></ul>' },
        { h: 'ชำระและชำระเงิน', html: '<ul><li>รายการชำระ</li><li>ชำระเงิน</li><li>ภาพรวมความเสี่ยง</li></ul>' },
        { h: 'รายละเอียด', html: '<p>ดูคู่มืออื่นในแท็บเดียวกัน</p>' }
      ]
    }
  },
  {
    id: 'hqdist-merchant-add',
    audience: 'hqdist',
    titles: {
      ko: '신규가맹점 추가 메뉴얼',
      en: 'New Merchant Onboarding Manual',
      ja: '新規加盟店追加マニュアル',
      zh: '新商户添加手册',
      th: 'คู่มือเพิ่มร้านค้าใหม่'
    },
    subs: {
      ko: '가맹 등록·결제설정·PG·수수료·리스크·오픈 점검',
      en: 'Register, payment setup, PG, fees, risk, go-live checks',
      ja: '登録・決済設定・PG・手数料・リスク・公開点検',
      zh: '注册、支付设置、PG、手续费、风险、上线检查',
      th: 'ลงทะเบียน ตั้งค่าชำระ PG ค่าธรรมเนียม ความเสี่ยง ตรวจเปิดใช้'
    },
    sections: {
      ko: [
        { h: '1. 등록', html: '<p class="menu-path">업체관리 → 업체등록</p><ol><li>상위(총판 등) 선택 · 가맹 구분</li><li>업체코드·명 · 연락처·주소</li><li>로그인 ID · 사용=Y</li><li>저장</li></ol>' },
        { h: '2. 결제·창', html: '<ul><li>웹결제·URL 결제 방식</li><li>결제창 구성(입력방식·로고)</li><li>운영 PG 바인딩(본사/배포 정책에 따름)</li></ul>' },
        { h: '3. 부가 서비스', html: '<ul><li>구독(정기) · 재구매 · 챗봇 — 필요 시 Y</li><li><strong>URL 분할결제</strong>: 가맹 카드에서 「분할결제 사용여부=사용」저장 시 DB 즉시 반영(재로그인 후에도 유지). 계약취소는 본사설정 따름/사용/미사용</li><li>리스크관리 트리거: 기본 <strong>본사정책 따름</strong></li></ul>' },
        { h: '4. 오픈 전', html: '<ol><li>테스트 결제 1건</li><li>노티/콜백 URL(해당 시)</li><li>가맹점API 키·문서 안내 위치 확인(연동 메뉴얼은 배포 허브)</li></ol>' }
      ],
      en: [
        { h: '1. Register', html: '<p class="menu-path">Companies → Register</p><ol><li>Parent &amp; merchant type</li><li>Code, name, contacts</li><li>Login · Use=Y</li><li>Save</li></ol>' },
        { h: '2. Checkout', html: '<ul><li>Web/URL modes</li><li>Window composition</li><li>Operational PG binding</li></ul>' },
        { h: '3. Add-ons', html: '<ul><li>Subscribe / repay / chatbot as needed</li><li><strong>URL Split Payment</strong>: Save Split-pay = ON on the merchant card — persisted immediately (survives re-login). Contract-cancel: Follow HQ / Y / N</li><li>Risk trigger: Follow HQ by default</li></ul>' },
        { h: '4. Go-live', html: '<ol><li>One test payment</li><li>NOTI/callback if any</li><li>Point merchants to API docs location (not full kit here)</li></ol>' }
      ],
      ja: [
        { h: '1. 登録', html: '<p class="menu-path">業者管理 → 登録</p><ol><li>上位・加盟区分</li><li>コード・名・連絡先</li><li>ログイン・使用Y</li><li>保存</li></ol>' },
        { h: '2. 決済', html: '<ul><li>Web/URL</li><li>画面構成</li><li>運用PG</li></ul>' },
        { h: '3. 追加', html: '<ul><li>購読・再購入・ボット</li><li><strong>URL分割払い</strong>: 加盟カードで使用=ON保存→DB即反映(再ログイン後も維持)。契約取消は本社に従う/使用/未使用</li><li>リスクは本社に従う</li></ul>' },
        { h: '4. 公開前', html: '<ol><li>テスト決済</li><li>NOTI</li><li>API文書の案内位置</li></ol>' }
      ],
      zh: [
        { h: '1. 注册', html: '<p class="menu-path">企业管理 → 注册</p><ol><li>上级与商户类型</li><li>代码、名称、联系方式</li><li>登录 · 使用=Y</li><li>保存</li></ol>' },
        { h: '2. 支付', html: '<ul><li>Web/URL</li><li>支付窗</li><li>运营 PG</li></ul>' },
        { h: '3. 附加', html: '<ul><li>订阅/再购/机器人</li><li><strong>URL 分期</strong>：在商户卡片保存「启用」后立即写入数据库（重新登录仍保持）。合同取消：遵循总部/启用/停用</li><li>风险默认遵循总部</li></ul>' },
        { h: '4. 上线前', html: '<ol><li>测试支付</li><li>通知</li><li>API 文档入口说明</li></ol>' }
      ],
      th: [
        { h: '1. ลงทะเบียน', html: '<p class="menu-path">จัดการบริษัท → ลงทะเบียน</p><ol><li>ต้นสังกัดและประเภท</li><li>รหัส ชื่อ ติดต่อ</li><li>ล็อกอิน · ใช้=Y</li><li>บันทึก</li></ol>' },
        { h: '2. ชำระ', html: '<ul><li>Web/URL</li><li>หน้าต่างชำระ</li><li>PG ปฏิบัติการ</li></ul>' },
        { h: '3. ส่วนเสริม', html: '<ul><li>สมัคร/ซื้อซ้ำ/แชทบอท</li><li><strong>URL แบ่งจ่าย</strong>: บันทึกเปิดใช้บนการ์ดร้าน → ลง DB ทันที (ค้างหลังล็อกอินใหม่) ยกเลิกสัญญา: ตาม HQ/ใช้/ไม่ใช้</li><li>ความเสี่ยงเริ่มต้นตาม HQ</li></ul>' },
        { h: '4. ก่อนเปิด', html: '<ol><li>ทดสอบชำระ</li><li>NOTI</li><li>ชี้ทางดูเอกสาร API</li></ol>' }
      ]
    }
  },
  {
    id: 'hqdist-chatbot',
    audience: 'hqdist',
    titles: {
      ko: '챗봇결제 운영 메뉴얼',
      en: 'Chatbot Payment Operations Manual',
      ja: 'チャットボット決済 運営マニュアル',
      zh: '聊天机器人支付运营手册',
      th: 'คู่มือปฏิบัติการชำระผ่านแชทบอท'
    },
    subs: {
      ko: '상품·공개 챗봇·결제 연동 운영',
      en: 'Products, public chatbot, payment ops',
      ja: '商品・公開ボット・決済運用',
      zh: '商品、公开机器人、支付运营',
      th: 'สินค้า แชทบอทสาธารณะ การชำระ'
    },
    sections: {
      ko: [
        { h: '활성화', html: '<p>가맹 상세에서 챗봇 관련 사용여부를 켠 뒤, <strong>챗봇관리</strong>에서 상품·주문을 운영합니다.</p>' },
        { h: '상품', html: '<ul><li>상품관리: 판매 활성(사용=Y) 개수 제한(플랜)</li><li>본사 판매금지 상품은 고객 노출 제외</li></ul>' },
        { h: '결제', html: '<ul><li>공개 챗봇 URL로 문의·상품·결제</li><li>URL 결제 방식(일반/재결제 등) 가맹 설정과 연동</li><li>운영 보류 시 상품·결제는 비활성, 문의만 가능</li></ul>' },
        { h: '점검', html: '<ol><li>공개 URL 접속</li><li>상품 노출·결제 1건</li><li>주문관리 반영</li></ol>' }
      ],
      en: [
        { h: 'Enable', html: '<p>Turn on chatbot flags on the merchant, then operate products/orders under Chatbot.</p>' },
        { h: 'Products', html: '<ul><li>Sales-active count limits</li><li>HQ-banned products hidden</li></ul>' },
        { h: 'Payment', html: '<ul><li>Public chatbot URL</li><li>Tied to URL pay mode</li><li>Ops hold: chat only</li></ul>' },
        { h: 'Checks', html: '<ol><li>Open URL</li><li>One paid order</li><li>Order list</li></ol>' }
      ],
      ja: [
        { h: '有効化', html: '<p>加盟でチャットボットをONし、チャットボット管理で商品・注文を運用。</p>' },
        { h: '商品', html: '<ul><li>販売活性数制限</li><li>販売禁止は非表示</li></ul>' },
        { h: '決済', html: '<ul><li>公開URL</li><li>URL決済方式連動</li><li>運用保留時は問合せのみ</li></ul>' },
        { h: '点検', html: '<ol><li>URL</li><li>決済1件</li><li>注文一覧</li></ol>' }
      ],
      zh: [
        { h: '启用', html: '<p>商户开启聊天机器人后，在聊天机器人管理中运营商品与订单。</p>' },
        { h: '商品', html: '<ul><li>销售激活数量限制</li><li>总部禁售不展示</li></ul>' },
        { h: '支付', html: '<ul><li>公开 URL</li><li>与 URL 支付方式联动</li><li>运营暂停时仅咨询</li></ul>' },
        { h: '检查', html: '<ol><li>打开 URL</li><li>一笔支付</li><li>订单列表</li></ol>' }
      ],
      th: [
        { h: 'เปิดใช้', html: '<p>เปิดแฟล็กแชทบอทที่ร้าน แล้วจัดการสินค้า/คำสั่งในเมนูแชทบอท</p>' },
        { h: 'สินค้า', html: '<ul><li>จำกัดจำนวนขายที่เปิดใช้</li><li>สินค้าห้ามขายไม่โชว์</li></ul>' },
        { h: 'ชำระ', html: '<ul><li>URL สาธารณะ</li><li>ผูกโหมด URL pay</li><li>พักดำเนินงาน=คุยอย่างเดียว</li></ul>' },
        { h: 'ตรวจ', html: '<ol><li>เปิด URL</li><li>ชำระ 1 รายการ</li><li>รายการคำสั่ง</li></ol>' }
      ]
    }
  },
  {
    id: 'hqdist-subscription',
    audience: 'hqdist',
    titles: {
      ko: '정기결제 운영 메뉴얼',
      en: 'Subscription (Recurring) Payment Manual',
      ja: '定期決済（購読）運営マニュアル',
      zh: '定期支付（订阅）运营手册',
      th: 'คู่มือชำระรายงวด (สมัครสมาชิก)'
    },
    subs: {
      ko: '구독(정기) 사용설정·플랜·내역·해지',
      en: 'Enable subscription, plans, history, cancel',
      ja: '購読ON・プラン・履歴・解約',
      zh: '开启订阅、计划、履历、解约',
      th: 'เปิดสมัคร แผน ประวัติ ยกเลิก'
    },
    sections: {
      ko: [
        { h: '개념', html: '<p>ICOPAY 통합 구독 API·화면으로 주기 청구합니다. 가맹·구매자 UI에는 결제대행을 노출하지 않습니다. 초회는 보안 정책상 3DS가 적용될 수 있습니다.</p>' },
        { h: '설정', html: '<ol><li>가맹 「구독결제 사용여부」=Y</li><li>플랜(주기·금액·통화)</li><li>구독 URL / 통합 subscription prepare</li></ol>' },
        { h: '운영', html: '<ul><li>결제관리 → 구독결제내역</li><li>상태: 활성/정지/해지/만료</li><li>실패 시 재시도·고객 안내</li></ul>' },
        { h: '주의', html: '<p>재구매(고객 선택 재결제)와 다릅니다. API 상세 스펙은 연동·배포 허브의 API 문서를 안내하십시오(본 매뉴얼은 운영 절차).</p>' }
      ],
      en: [
        { h: 'Concept', html: '<p>Recurring via ICOPAY unified subscription APIs/UI. PG names stay hidden from merchants/buyers.</p>' },
        { h: 'Setup', html: '<ol><li>Merchant subscribe = Y</li><li>Plan period/amount</li><li>Subscribe URL / prepare API</li></ol>' },
        { h: 'Ops', html: '<ul><li>Subscription list</li><li>Active/pause/cancel/expire</li><li>Retry on fail</li></ul>' },
        { h: 'Note', html: '<p>Different from repay. Point engineers to deploy API docs.</p>' }
      ],
      ja: [
        { h: '概要', html: '<p>ICOPAY統合購読で周期請求。加盟・購入者UIにPG名は出しません。</p>' },
        { h: '設定', html: '<ol><li>購読=Y</li><li>プラン</li><li>URL/prepare</li></ol>' },
        { h: '運用', html: '<ul><li>購読一覧</li><li>状態管理</li><li>失敗再試行</li></ul>' },
        { h: '注意', html: '<p>再購入とは別。API詳細は配信ハブ文書へ。</p>' }
      ],
      zh: [
        { h: '概念', html: '<p>通过 ICOPAY 统一订阅周期扣款；商户/买家界面不露 PG 名。</p>' },
        { h: '设置', html: '<ol><li>订阅=Y</li><li>计划</li><li>URL/prepare</li></ol>' },
        { h: '运营', html: '<ul><li>订阅列表</li><li>状态</li><li>失败重试</li></ul>' },
        { h: '注意', html: '<p>不同于再购买。API 详规见对接文档入口。</p>' }
      ],
      th: [
        { h: 'แนวคิด', html: '<p>เรียกเก็บตามรอบผ่าน ICOPAY subscription UI ไม่โชว์ชื่อ PG ให้ร้าน/ผู้ซื้อ</p>' },
        { h: 'ตั้งค่า', html: '<ol><li>สมัคร=Y</li><li>แผน</li><li>URL/prepare</li></ol>' },
        { h: 'ปฏิบัติการ', html: '<ul><li>รายการสมัคร</li><li>สถานะ</li><li>ลองใหม่เมื่อล้มเหลว</li></ul>' },
        { h: 'หมายเหตุ', html: '<p>ต่างจากซื้อซ้ำ รายละเอียด API ชี้ไปเอกสารดีพลอย</p>' }
      ]
    }
  },
  {
    id: 'hqdist-split',
    audience: 'hqdist',
    titles: {
      ko: '분할결제 운영 메뉴얼',
      en: 'Split Payment Operations Manual',
      ja: '分割決済 運営マニュアル',
      zh: '分期支付运营手册',
      th: 'คู่มือปฏิบัติการแบ่งจ่าย'
    },
    subs: {
      ko: 'URL 분할·회차·메일·내역',
      en: 'URL split, installments, email, history',
      ja: 'URL分割・回次・メール・履歴',
      zh: 'URL 分期、期次、邮件、履历',
      th: 'URL แบ่งงวด อีเมล ประวัติ'
    },
    sections: {
      ko: [
        { h: '활성화', html: '<p>총본사·본사·총판이 가맹 <strong>업체정보 → URL 분할결제</strong>에서 「분할결제 사용여부=사용」으로 <strong>저장</strong>하면 DB에 즉시 반영됩니다. 재로그인·재조회 후에도 유지됩니다. 월간/일간/멀티 기간·1회차(즉시/링크)·계약취소(본사따름/사용/미사용)를 함께 설정합니다.</p>' },
        { h: '고객 흐름', html: '<ol><li>분할결제 URL 또는 계약 API</li><li>1회차 결제</li><li>이후 회차 메일/링크</li></ol>' },
        { h: '운영 화면', html: '<ul><li>분할관리: 진행·이메일·계약취소(권한 시)</li><li>결제관리 → 분할결제내역</li></ul>' },
        { h: '주의', html: '<p>API URL 인라인의 「분할」선택과 공개 URL 분할은 설정이 다를 수 있습니다. 가맹은 내 업체정보에서 사용여부를 직접 바꿀 수 없으며, 상위 저장값이 기준입니다.</p>' }
      ],
      en: [
        { h: 'Enable', html: '<p>HQ/regional/distributor saves Split-pay = ON on merchant profile → <strong>URL Split Payment</strong>. Value persists in DB after re-login. Also set month/day/multi, first pay, and contract-cancel (Follow HQ / Y / N).</p>' },
        { h: 'Customer flow', html: '<ol><li>Split URL or contract API</li><li>First installment</li><li>Later emails/links</li></ol>' },
        { h: 'Screens', html: '<ul><li>Split management (progress / email / cancel when allowed)</li><li>Split payment list</li></ul>' },
        { h: 'Note', html: '<p>API inline split mode may differ from public URL split. Merchants cannot toggle enable on My Company Info — parent-saved value wins.</p>' }
      ],
      ja: [
        { h: '有効化', html: '<p>総本社・本社・総販が加盟<strong>業者情報 → URL分割払い</strong>で使用=ONを<strong>保存</strong>するとDBに即反映。再ログイン後も維持。月/日/マルチ・1回目・契約取消も設定。</p>' },
        { h: '顧客流れ', html: '<ol><li>URL/契約API</li><li>1回目</li><li>以降メール</li></ol>' },
        { h: '画面', html: '<ul><li>分割管理</li><li>分割決済一覧</li></ul>' },
        { h: '注意', html: '<p>APIインライン分割と公開URLは別設定の場合あり。加盟は自社情報で使用可否を変更不可。</p>' }
      ],
      zh: [
        { h: '启用', html: '<p>总总部/总部/总代在商户<strong>资料 → URL 分期</strong>将启用设为「使用」并<strong>保存</strong>后立即写入数据库，重新登录仍保持。可同时设置月/日/多选、首期、合同取消。</p>' },
        { h: '客户流程', html: '<ol><li>URL/合同 API</li><li>首期</li><li>后续邮件</li></ol>' },
        { h: '画面', html: '<ul><li>分期管理</li><li>分期支付列表</li></ul>' },
        { h: '注意', html: '<p>API 内联分期与公开 URL 分期可能不同。商户无法在「我的企业信息」自行改启用状态。</p>' }
      ],
      th: [
        { h: 'เปิดใช้', html: '<p>HQ/ภูมิภาค/ตัวแทนบันทึกเปิดใช้ที่ข้อมูลร้าน → <strong>URL แบ่งจ่าย</strong> แล้วค่าจะลง DB ทันที คงหลังล็อกอินใหม่ ตั้งเดือน/วัน/มัลติ งวดแรก และยกเลิกสัญญาได้</p>' },
        { h: 'ไหลลูกค้า', html: '<ol><li>URL/สัญญา API</li><li>งวดแรก</li><li>อีเมลถัดไป</li></ol>' },
        { h: 'หน้าจอ', html: '<ul><li>จัดการแบ่งจ่าย</li><li>รายการแบ่งจ่าย</li></ul>' },
        { h: 'หมายเหตุ', html: '<p>โหมด API inline อาจต่างจาก URL สาธารณะ ร้านเปลี่ยนสถานะเปิดใช้เองไม่ได้</p>' }
      ]
    }
  },
  {
    id: 'hqdist-risk-intro',
    audience: 'hqdist',
    titles: {
      ko: '리스크 트리거 발동 소개 안내',
      en: 'Risk Trigger Introduction (for Merchants)',
      ja: 'リスクトリガー発火のご案内',
      zh: '风险触发介绍（面向商户）',
      th: 'แนะนำทริกเกอร์ความเสี่ยง (สำหรับร้านค้า)'
    },
    subs: {
      ko: '가맹점에 설명할 기본값·발동 요약',
      en: 'Defaults &amp; triggers to explain to merchants',
      ja: '加盟店向け既定値・発火の要約',
      zh: '向商户说明的默认值与触发摘要',
      th: 'สรุปค่าเริ่มต้นและการทำงานสำหรับร้านค้า'
    },
    sections: {
      ko: [
        { h: '전달 목적', html: '<p>총판·본사가 가맹점에 「왜 결제가 잠깐 막히는지」를 설명할 때 사용합니다. 수치 변경은 본사 리스크 화면에서만 가능합니다.</p>' },
        { h: '두 가지 장치', html: '<table><tr><th>구분</th><th>내용</th></tr><tr><td>리스크설정</td><td>같은 카드 실패가 쌓이면 5분→10분→1시간 대기, 4회면 비활성카드</td></tr><tr><td>리스크 필터링</td><td>이메일/전화/속도 이상 시 PG로 보내기 전에 차단</td></tr></table>' },
        { h: '속도 기본', html: '<p>카드 10분/3회 · 이메일 30분/5회 · IP 15분/10회.</p>' },
        { h: '가맹에 말할 때', html: '<ul><li>한 번 실패해도 영구 정지가 아님</li><li>성공하면 횟수 리셋</li><li>막히면 상위 운영자에게 리스크 현황 코드 확인 요청</li></ul>' }
      ],
      en: [
        { h: 'Purpose', html: '<p>Explain temporary blocks to merchants. Only HQ can change numbers.</p>' },
        { h: 'Two layers', html: '<table><tr><th>Area</th><th>Meaning</th></tr><tr><td>Settings</td><td>5→10→60 min cooldowns; inactive at 4</td></tr><tr><td>Filtering</td><td>Block before PG on format/velocity issues</td></tr></table>' },
        { h: 'Velocity', html: '<p>Card 10/3 · Email 30/5 · IP 15/10.</p>' },
        { h: 'Talking points', html: '<ul><li>Not a permanent ban after one fail</li><li>Success resets</li><li>Ask ops to check Risk dashboard codes</li></ul>' }
      ],
      ja: [
        { h: '目的', html: '<p>加盟店への一時ブロック説明用。数値変更は本社のみ。</p>' },
        { h: '二層', html: '<table><tr><th>区分</th><th>内容</th></tr><tr><td>設定</td><td>5→10→60分、4回で非活性</td></tr><tr><td>フィルタ</td><td>PG送信前ブロック</td></tr></table>' },
        { h: '速度', html: '<p>カード10/3・メール30/5・IP15/10。</p>' },
        { h: '説明要点', html: '<ul><li>1回≠永久</li><li>成功でリセット</li><li>状況コード確認</li></ul>' }
      ],
      zh: [
        { h: '用途', html: '<p>向商户说明临时拦截；数值仅总部可改。</p>' },
        { h: '两层', html: '<table><tr><th>区域</th><th>含义</th></tr><tr><td>设置</td><td>5→10→60 分；4 次非活跃</td></tr><tr><td>过滤</td><td>送 PG 前拦截</td></tr></table>' },
        { h: '速度', html: '<p>卡 10/3 · 邮箱 30/5 · IP 15/10。</p>' },
        { h: '话术', html: '<ul><li>一次失败≠永久</li><li>成功重置</li><li>请运营查风险看板</li></ul>' }
      ],
      th: [
        { h: 'วัตถุประสงค์', html: '<p>อธิบายการบล็อกชั่วคราวให้ร้าน ค่าตัวเลขแก้ได้ที่ HQ เท่านั้น</p>' },
        { h: 'สองชั้น', html: '<table><tr><th>ส่วน</th><th>ความหมาย</th></tr><tr><td>ตั้งค่า</td><td>คูลดาวน์ 5→10→60 นาที; ปิดใช้ที่ 4</td></tr><tr><td>ตัวกรอง</td><td>บล็อกก่อนส่ง PG</td></tr></table>' },
        { h: 'ความเร็ว', html: '<p>บัตร 10/3 · อีเมล 30/5 · IP 15/10</p>' },
        { h: 'ประเด็นคุย', html: '<ul><li>ล้มเหลวครั้งเดียว≠แบนถาวร</li><li>สำเร็จแล้วรีเซ็ต</li><li>ขอดูรหัสในภาพรวมความเสี่ยง</li></ul>' }
      ]
    }
  },
  {
    id: 'merchant-user',
    audience: 'merchant',
    titles: {
      ko: '가맹점 유저 메뉴얼',
      en: 'Merchant User Manual',
      ja: '加盟店ユーザーマニュアル',
      zh: '商户用户手册',
      th: 'คู่มือผู้ใช้ร้านค้า'
    },
    subs: {
      ko: '화면 이용 · API 연동 문서 보는 법 · 리스크 트리거 안내',
      en: 'UI guide · how to open API docs · risk triggers',
      ja: '画面利用・API文書の見方・リスクトリガー',
      zh: '界面使用 · 如何查看 API 文档 · 风险触发',
      th: 'ใช้หน้าจอ · วิธีดูเอกสาร API · ทริกเกอร์ความเสี่ยง'
    },
    sections: {
      ko: [
        { h: '로그인·메뉴', html: '<p>발급받은 로그인 ID로 접속합니다. 사이드바는 상위 조직 권한에 따라 다릅니다. 결제내역·정산·업체정보를 주로 사용합니다.</p>' },
        { h: '결제·정산', html: '<ul><li>결제관리에서 승인·취소·환불 상태 확인</li><li>정산관리는 계약된 주기·리포트 기준</li></ul>' },
        { h: 'API 연동 소개(문서 위치)', html: '<p>상세 연동 스펙·샘플은 본 매뉴얼에 포함하지 않습니다. 상위 운영자(본사/총판/총본사)에게 <strong>연동·배포 → 가맹 API 출시 → API 문서 / 출시 가이드</strong> 열람 방법을 요청하십시오. 키 발급·체크리스트도 동일 허브입니다.</p>' },
        { h: '리스크 트리거', html: '<p>동일 카드 실패 누적 시 잠시 대기가 걸릴 수 있습니다(기본 5→10→60분, 4회 시 비활성카드). 이메일·전화·짧은 시간 다수 시도도 사전 차단될 수 있습니다. 정상 거래가 막히면 상위 운영자에게 「리스크 현황」 확인을 요청하세요. 한 번 실패만으로 영구 정지가 되지는 않습니다.</p>' },
        { h: '문의', html: '<p>계약·정산·연동 문의는 상위 총판/본사 담당자에게 연락하십시오.</p>' }
      ],
      en: [
        { h: 'Login &amp; menus', html: '<p>Use your issued login. Sidebar depends on parent permissions.</p>' },
        { h: 'Pay &amp; settle', html: '<ul><li>Payment lists for status</li><li>Settlement per contract cycle</li></ul>' },
        { h: 'API docs (where)', html: '<p>Full integration kits are not in this manual. Ask your parent org to open <strong>Integration &amp; Deploy → Merchant API launch → API docs / Launch guide</strong>.</p>' },
        { h: 'Risk triggers', html: '<p>Repeated card failures may cooldown (5→10→60 min; inactive at 4). Format/velocity filters may block before PG. Ask ops to check the Risk dashboard. One fail ≠ permanent ban.</p>' },
        { h: 'Contact', html: '<p>Reach your distributor/HQ contact.</p>' }
      ],
      ja: [
        { h: 'ログイン', html: '<p>発行IDでログイン。メニューは上位権限に依存。</p>' },
        { h: '決済・精算', html: '<ul><li>決済一覧</li><li>契約周期の精算</li></ul>' },
        { h: 'API文書の見方', html: '<p>詳細キットは本冊に含めません。上位に<strong>連携・配信 → 加盟API出稿 → API文書</strong>の開き方を依頼。</p>' },
        { h: 'リスクトリガー', html: '<p>失敗累積で一時待機(5→10→60分、4回で非活性)。1回≠永久停止。</p>' },
        { h: '問合せ', html: '<p>上位総代理/本社へ。</p>' }
      ],
      zh: [
        { h: '登录', html: '<p>使用下发的账号；菜单取决于上级权限。</p>' },
        { h: '支付与结算', html: '<ul><li>支付列表</li><li>按合约周期结算</li></ul>' },
        { h: '如何查看 API 文档', html: '<p>完整对接套件不在本手册。请上级打开<strong>对接·部署 → 商户 API 发布 → API 文档</strong>。</p>' },
        { h: '风险触发', html: '<p>失败累计可能冷却(5→10→60 分；4 次非活跃)。一次失败≠永久停用。</p>' },
        { h: '联系', html: '<p>联系总代理/总部对接人。</p>' }
      ],
      th: [
        { h: 'ล็อกอิน', html: '<p>ใช้ไอดีที่ได้รับ เมนูขึ้นกับสิทธิ์ต้นสังกัด</p>' },
        { h: 'ชำระและชำระเงิน', html: '<ul><li>รายการชำระ</li><li>ชำระตามรอบสัญญา</li></ul>' },
        { h: 'วิธีดูเอกสาร API', html: '<p>ชุดเชื่อมต่อเต็มไม่รวมในคู่มือนี้ ขอต้นสังกัดเปิด<strong>เชื่อมต่อ·ดีพลอย → เปิดตัว API ร้าน → เอกสาร API</strong></p>' },
        { h: 'ทริกเกอร์ความเสี่ยง', html: '<p>ล้มเหลวสะสมอาจคูลดาวน์ (5→10→60 นาที; ปิดใช้ที่ 4) ครั้งเดียว≠แบนถาวร</p>' },
        { h: 'ติดต่อ', html: '<p>ติดต่อตัวแทน/HQ</p>' }
      ]
    }
  }
];

function esc(s) {
  return String(s == null ? '' : s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');
}

function renderHtml(manual, lang) {
  const ui = UI[lang];
  const title = manual.titles[lang];
  const sub = manual.subs[lang];
  const sections = manual.sections[lang];
  const toc = sections.map((s, i) => `<li><a href="#s${i + 1}">${esc(s.h)}</a></li>`).join('');
  const body = sections.map((s, i) =>
    `<h2 class="section-title" id="s${i + 1}">${i + 1}. ${esc(s.h)}</h2>\n${s.html}`
  ).join('\n');

  return `<!DOCTYPE html>
<html lang="${lang === 'zh' ? 'zh-CN' : lang === 'ja' ? 'ja' : lang === 'th' ? 'th' : lang === 'en' ? 'en' : 'ko'}">
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
  .brand-box img{max-height:52px;max-width:160px;object-fit:contain}
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
  }
}

const catalog = {
  version: VERSION,
  date: DATE,
  note: 'HTML preview only. Production catalog.json with pdfDir/docVersion is maintained separately — do not overwrite when rebuilding HTML.',
  audiences: [
    { id: 'super', labels: { ko: '총본사용', en: 'Super HQ', ja: '総本部向け', zh: '总本部', th: 'สำนักงานใหญ่สูงสุด' } },
    { id: 'hqdist', labels: { ko: '본사 및 총판용', en: 'HQ & Distributor', ja: '本社・総代理向け', zh: '总部与总代理', th: 'HQ และตัวแทน' } },
    { id: 'merchant', labels: { ko: '가맹점용', en: 'Merchant', ja: '加盟店向け', zh: '商户', th: 'ร้านค้า' } }
  ],
  items: MANUALS.map((m) => ({
    id: m.id,
    audience: m.audience,
    titles: m.titles,
    pathPrefix: `manuals/generated/${m.id}`
  }))
};
/* Keep production PDF catalog intact (pdfDir + docVersion). */
const catalogPath = path.join(OUT, 'catalog.json');
if (fs.existsSync(catalogPath)) {
  try {
    const existing = JSON.parse(fs.readFileSync(catalogPath, 'utf8'));
    if (existing && existing.format === 'pdf' && Array.isArray(existing.items) && existing.items.some((it) => it.pdfDir)) {
      existing.version = VERSION;
      existing.date = DATE;
      fs.writeFileSync(catalogPath, JSON.stringify(existing, null, 2), 'utf8');
      console.log('Updated catalog.json version/date only (kept pdfDir catalog)');
    } else {
      fs.writeFileSync(catalogPath, JSON.stringify(catalog, null, 2), 'utf8');
    }
  } catch (e) {
    fs.writeFileSync(catalogPath, JSON.stringify(catalog, null, 2), 'utf8');
  }
} else {
  fs.writeFileSync(catalogPath, JSON.stringify(catalog, null, 2), 'utf8');
}
console.log('Generated', n, 'HTML files →', OUT);
