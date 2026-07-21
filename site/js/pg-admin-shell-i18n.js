/**
 * 관리자 공통 셸(사이드바·탭·브레드크럼·헤더 라벨) 다국어 — PG_PAY_LIST_I18N 로케일과 동기.
 */
(function (w) {
  'use strict';

  function getLoc() {
    if (w.PG_PAY_LIST_I18N && typeof w.PG_PAY_LIST_I18N.getLocale === 'function') {
      return w.PG_PAY_LIST_I18N.getLocale();
    }
    return 'KO';
  }

  function pick(row, loc) {
    if (!row) return '';
    if (loc === 'KO') return row.KO;
    return row[loc] || row.EN || row.KO;
  }

  function escHtml(s) {
    return String(s == null ? '' : s)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  var SHELL = {
    pageTitle: {
      KO: 'PG 통합관리자',
      EN: 'PG Admin Console',
      JP: 'PG 統合管理',
      CH: 'PG 综合管理',
      TH: 'ระบบบริหาร PG'
    },
    langLabel: { KO: '언어', EN: 'Language', JP: '言語', CH: '语言', TH: 'ภาษา' },
    langGroupAria: {
      KO: '그리드 및 화면 언어',
      EN: 'Grid and UI language',
      JP: 'グリッド・画面言語',
      CH: '网格与界面语言',
      TH: 'ภาษาของกริดและหน้าจอ'
    },
    labelIp: { KO: '접속 IP:', EN: 'Client IP:', JP: '接続IP:', CH: '访问 IP:', TH: 'IP ที่เชื่อม:' },
    labelTime: { KO: '접속시간:', EN: 'Signed in:', JP: '接続時刻:', CH: '登录时间:', TH: 'เวลาเข้าใช้:' },
    dimmed: {
      KO: '잠시만 기다려주십시오',
      EN: 'Please wait…',
      JP: '少々お待ちください',
      CH: '请稍候…',
      TH: 'กรุณารอสักครู่…'
    },
    closeAllTabs: { KO: '전체닫기', EN: 'Close all', JP: 'すべて閉じる', CH: '关闭全部', TH: 'ปิดทั้งหมด' },
    myInfo: { KO: '나의정보', EN: 'My profile', JP: 'マイ情報', CH: '我的信息', TH: 'ข้อมูลของฉัน' },
    logOut: { KO: '로그아웃', EN: 'Log out', JP: 'ログアウト', CH: '退出登录', TH: 'ออกจากระบบ' },
    mainTab: { KO: '메인', EN: 'Home', JP: 'ホーム', CH: '主页', TH: 'หลัก' },
    mainHint1: {
      KO: '좌측 메뉴에서 화면을 선택하세요. 메뉴 클릭 시 해당 탭이 열립니다.',
      EN: 'Choose a screen from the left menu. Each item opens a new tab.',
      JP: '左メニューから画面を選択してください。クリックでタブが開きます。',
      CH: '请从左侧菜单选择功能，点击后在标签页中打开。',
      TH: 'เลือกเมนูด้านซ้าย แต่ละรายการจะเปิดเป็นแท็บ'
    },
    mainHint2: {
      KO: '결제내역 그리드 기본 노출 항목에 Route No(PG 라우트)가 포함됩니다. 세부 컬럼은 결제내역 화면의 VIEW SETTING에서 조정할 수 있습니다.',
      EN: 'The payment list grid includes Route No (PG route) by default. Adjust columns in VIEW SETTING on the payment list screen.',
      JP: '決済一覧グリッドには既定で Route No (PGルート) が含まれます。列は VIEW SETTING で調整できます。',
      CH: '支付列表网格默认包含 Route No（PG 路由）。可在支付列表的 VIEW SETTING 中调整列。',
      TH: 'กริดรายการชำระเงินมี Route No (PG route) เป็นค่าเริ่มต้น ปรับคอลัมน์ได้ที่ VIEW SETTING'
    },
    foldExpand: { KO: ' » 펴기', EN: ' » Expand', JP: ' » 展開', CH: ' » 展开', TH: ' » ขยาย' },
    foldCollapse: { KO: ' « 접기', EN: ' « Collapse', JP: ' « 折りたたみ', CH: ' « 折叠', TH: ' « ย่อ' },
    alertMaxTabs: {
      KO: '메뉴는 최대 12탭 입니다. 추가 시 처음 탭이 자동 삭제됩니다.',
      EN: 'You can open up to 12 tabs. Adding another removes the oldest tab.',
      JP: 'タブは最大12です。追加で先頭タブが削除されます。',
      CH: '最多 12 个标签页，新增时将自动移除最早的一个。',
      TH: 'เปิดได้สูงสุด 12 แท็บ หากเกินจะลบแท็บแรกอัตโนมัติ'
    },
    alertNoMenuPerm: {
      KO: '이 메뉴에 대한 접근 권한이 없습니다. [본사권한설정]에서 해당 화면 권한을 확인하세요.',
      EN: 'You do not have access to this menu. Check permissions in HQ permission settings.',
      JP: 'このメニューへのアクセス権がありません。[本社権限設定]で権限を確認してください。',
      CH: '您无权访问此菜单。请在【总部权限设置】中检查对应画面权限。',
      TH: 'ไม่มีสิทธิ์เมนูนี้ ตรวจสอบที่การตั้งค่าสิทธิ์สำนักงานใหญ่'
    }
  };

  var PARENT_SEG = {
    본사설정: { EN: 'HQ settings', JP: '本社設定', CH: '总部设置', TH: 'ส่วนตั้งค่า HQ' },
    업체관리: { EN: 'Merchant', JP: '加盟店管理', CH: '商户管理', TH: 'จัดการร้านค้า' },
    결제관리: { EN: 'Payments', JP: '決済管理', CH: '支付管理', TH: 'การชำระเงิน' },
    챗봇관리: { EN: 'Chatbot management', JP: 'チャットボット管理', CH: '聊天机器人管理', TH: 'จัดการ Chatbot' },
    분할관리: { EN: 'Split payment ops', JP: '分割決済運用', CH: '分次支付运营', TH: 'จัดการแบ่งงวด' },
    결제내역: { EN: 'Payment list', JP: '決済一覧', CH: '支付列表', TH: 'รายการชำระเงิน' },
    정산관리: { EN: 'Settlement', JP: '精算管理', CH: '结算管理', TH: 'การชำระบัญชี' },
    통보관리: { EN: 'Notifications', JP: '通知管理', CH: '通知管理', TH: 'การแจ้งเตือน' },
    사용자관리: { EN: 'Users', JP: 'ユーザー管理', CH: '用户管理', TH: 'ผู้ใช้' },
    운영관리: { EN: 'Operations', JP: '運用管理', CH: '运营管理', TH: 'ปฏิบัติการ' },
    검수관리: { EN: 'Inspection management', JP: '検収管理', CH: '检收管理', TH: 'จัดการตรวจสอบ' },
    리스크관리: { EN: 'Inspection management', JP: '検収管理', CH: '检收管理', TH: 'จัดการตรวจสอบ' },
    배포설정: { EN: 'Deployment', JP: 'デプロイ設定', CH: '部署设置', TH: 'การใช้งานจริง' },
    '본사정책': { EN: 'HQ policy', JP: '本社ポリシー', CH: '总部政策', TH: 'นโยบาย HQ' },
    '본사 정책': { EN: 'HQ policy', JP: '本社ポリシー', CH: '总部政策', TH: 'นโยบาย HQ' },
    '연동·배포': { EN: 'Integration & deploy', JP: '連携・デプロイ', CH: '联动与部署', TH: 'เชื่อมต่อและใช้งานจริง' }
  };

  function T(en, jp, ch, th) {
    return { EN: en, JP: jp, CH: ch, TH: th };
  }

  /** PARENT_SEG·URL_TR 번역문 → 한국어 키 복원(로케일 전환 시 data-pg-*-ko 오염 방지) */
  function reverseParentSegKey(displayText) {
    var txt = String(displayText == null ? '' : displayText).trim();
    if (!txt) return '';
    if (PARENT_SEG[txt]) return txt === '본사 정책' ? '본사정책' : txt;
    var keys = Object.keys(PARENT_SEG);
    for (var i = 0; i < keys.length; i++) {
      var k = keys[i];
      var row = PARENT_SEG[k];
      if (!row) continue;
      if (row.EN === txt || row.JP === txt || row.CH === txt || row.TH === txt) {
        return k === '본사 정책' ? '본사정책' : k;
      }
    }
    return txt;
  }

  function reverseUrlTrKo(url, displayText) {
    var tr = URL_TR[url];
    if (!tr) return '';
    var txt = String(displayText == null ? '' : displayText).trim();
    if (!txt) return '';
    if (tr.EN === txt || tr.JP === txt || tr.CH === txt || tr.TH === txt) {
      var info = (w.PG_MENU_INFO && w.PG_MENU_INFO[url]) || {};
      if (info.label) return info.label;
    }
    return '';
  }

  /** 사이드바·탭 한국어 라벨 — textContent 캐시 대신 data-pg-ui-t / PG_MENU_INFO 우선 */
  function resolveMenuKoLabel(anchor, url) {
    if (!anchor) return '';
    var uiT = anchor.getAttribute('data-pg-ui-t');
    if (uiT && String(uiT).trim()) return String(uiT).trim();
    var info = (w.PG_MENU_INFO && w.PG_MENU_INFO[url]) || {};
    if (info.label) return info.label;
    var stored = anchor.getAttribute('data-pg-menu-ko');
    if (stored && String(stored).trim()) {
      var sk = String(stored).trim();
      var rev = reverseUrlTrKo(url, sk);
      if (rev) return rev;
      if (!URL_TR[url] || sk === (info.label || '')) return sk;
    }
    return reverseUrlTrKo(url, anchor.textContent) || String(anchor.textContent || '').trim();
  }

  function resolveParentKoLabel(span) {
    if (!span) return '';
    var uiT = span.getAttribute('data-pg-ui-t');
    if (uiT && String(uiT).trim()) return String(uiT).trim();
    var stored = span.getAttribute('data-pg-parent-ko');
    if (stored && PARENT_SEG[String(stored).trim()]) return String(stored).trim();
    return reverseParentSegKey(span.textContent);
  }

  var URL_TR = {
    '/hq/pgApiMng': T('API integration', 'API連携設定', 'API 联动设置', 'การเชื่อม API'),
    '/hq/defaultCommission': T('Commission', '手数料設定', '手续费设置', 'ค่าธรรมเนียม'),
    '/hq/pgAgencyCostPolicy': T('Agency fee settings', '代行手数料設定', '代理手续费设置', 'ตั้งค่าค่าธรรมเนียมตัวแทน'),
    '/hq/chargebackPolicy': T('Chargeback', 'チャージバック設定', '拒付/退单设置', 'นโยบาย chargeback'),
    '/hq/riskCardPolicy': T('Risk settings', 'リスク設定', '风险设置', 'ตั้งค่าความเสี่ยง'),
    '/hq/businessDaySetting': T('Business days', '営業日設定', '营业日设置', 'วันทำการ'),
    '/hq/apiConfig': T('API deployment', 'API配信設定', 'API 部署设置', 'การตั้งค่า API'),
    '/hq/apiMerchantDeployReg': T('API merchant registration', 'API加盟店登録', 'API 商户注册', 'ลงทะเบียนร้าน API'),
    '/hq/merchantApiGenerate': T('Merchant API (generate)', '加盟店API生成', '生成商户 API', 'สร้าง Merchant API'),
    '/hq/merchantApiDeployDocs': T('API deployment docs', 'API配布ドキュメント', 'API 部署文档', 'เอกสาร API สำหรับร้าน'),
    '/hq/merchantApiDeployKit': T('Merchant API (generate)', '加盟店API生成', '生成商户 API', 'สร้าง Merchant API'),
    '/deploy/launchGuide': T('Launch guide', '公開ガイド', '发布指南', 'คู่มือเปิดใช้'),
    '/hq/urlPayDeploy': T('URL payment', 'URL決済設定', 'URL 支付设置', 'ชำระเงิน URL'),
    '/hq/paymentOrchestration': T('Payment acquirer logic', '決済代行ロジック', '支付机构逻辑', 'ตรรกะผู้ให้บริการชำระ'),
    '/calc/jpayTrList': T('Integrated overview', '統合概要', '整合概览', 'ภาพรวมรวม'),
    '/calc/payOverview': T('Payment overview', '決済概要', '支付概览', 'ภาพรวมการชำระ'),
    '/calc/queryIntegrated': T('Daily query', '日別照会', '按日查询', 'ค้นหารายวัน'),
    '/calc/splitPayList': T('Contract management', '契約管理', '合同管理', 'จัดการสัญญา'),
    '/pay/splitPay': T('Split payment list', '分割決済一覧', '分次支付记录', 'รายการชำระแบ่งงวด'),
    '/splitpay/progressMng': T('Progress', '進行管理', '进度管理', 'ความคืบหน้า'),
    '/splitpay/mailMng': T('Email management', 'メール管理', '邮件管理', 'จัดการอีเมล'),
    '/splitpay/emailSettings': T('Email settings', 'メール設定', '邮件设置', 'ตั้งค่าอีเมล'),
    '/hq/domainConfig': T('Domain', 'ドメイン構成', '域名配置', 'โดเมน'),
    '/hq/serverManage': T('Server ops', 'サーバー運用', '服务器运维', 'เซิร์ฟเวอร์'),
    '/hq/chatbotAiSettings': T('AI chatbot settings', 'AIチャットボット設定', 'AI 聊天机器人设置', 'ตั้งค่าแชทบอท AI'),
    '/chatbot/productMng': T('Products', '商品管理', '商品管理', 'สินค้า'),
    '/chatbot/orderMng': T('Orders', '注文管理', '订单管理', 'คำสั่งซื้อ'),
    '/chatbot/chatbotKbMng': T('Basic settings', '基本設定', '基本设置', 'การตั้งค่าพื้นฐาน'),
    '/hq/permissionMng': T('HQ permissions', '本社権限設定', '总部权限', 'สิทธิ์ HQ'),
    '/hq/opsModeMng': T('Tablet settings', 'タブレット設定', '平板设置', 'ตั้งค่าแท็บเล็ต'),
    '/hq/userSettings': T('User defaults', 'ユーザー設定', '用户设置', 'ค่าผู้ใช้'),
    '/hq/notifyEnv': T('Notify config', 'ノティ構成', '通知环境配置', 'ตั้งค่าแจ้งเตือน'),
    '/hq/notifyMapping': T('Notify mapping', 'ノティマッピング', '通知映射', 'แมปแจ้งเตือน'),
    '/hq/notifyInbound': T('Notify inbound', 'ノティ受信情報', '通知接收信息', 'ข้อมูลรับแจ้งเตือน'),
    '/hq/ledgerSysSettings': T('Ledger system', '全算設定', '账务系统设置', 'ระบบบัญชี'),
    '/hq/settlementAdmin': T('Settlement admin', '精算管理設定', '结算管理设置', 'ตั้งค่าการชำระบัญชี'),
    '/hq/receivableRecoverySettings': T('Recovery / receivables', '回収・未収設定', '回款/应收设置', 'กู้คืน/ลูกหนี้'),
    '/hq/orgViewColumnAllowance': T('Org columns', '組織項目設定', '组织字段设置', 'คอลัมน์องค์กร'),
    '/hq/accountMng': T('Merchant access', '加盟店アクセス', '商户访问控制', 'การเข้าถึงร้าน'),
    '/system/noticeList': T('Notices', 'お知らせ', '公告', 'ประกาศ'),
    '/comp/myCompMng': T('Merchant lookup', '加盟店情報照会', '商户信息查询', 'ค้นหาร้านค้า'),
    '/comp/merchantApiPortal': T('Merchant API', '加盟店API', '商户 API', 'API ร้านค้า'),
    '/comp/compInfo': T('Merchant registry (HQ)', '加盟店台帳(本社)', '商户台账(总部)', 'ทะเบียนร้าน (HQ)'),
    '/comp/compMng': T('Merchant list (simple)', '加盟店一覧(簡易)', '商户列表(简)', 'รายการร้าน (แบบง่าย)'),
    '/comp/compMngTree': T('Merchant tree', '加盟店管理', '商户管理', 'จัดการร้านค้า'),
    '/comp/compDetail': T('Merchant detail', '加盟店情報', '商户详情', 'รายละเอียดร้าน'),
    '/commission/commisionList': T('Commission mgmt', '手数料管理', '手续费管理', 'ค่าธรรมเนียม'),
    '/comp/compInfoHistList': T('Change history', '加盟店変更履歴', '商户变更历史', 'ประวัติการเปลี่ยนแปลง'),
    '/comp/compChangeHistory': T('Change history', '加盟店変更履歴', '商户变更历史', 'ประวัติการเปลี่ยนแปลง'),
    '/comp/compReg': T('Register merchant', '加盟店登録', '商户注册', 'ลงทะเบียนร้าน'),
    '/calc/payList': T('Payment list', '決済一覧', '支付列表', 'รายการชำระเงิน'),
    '/calc/chillPayTrList': T('Integrated transactions', '統合取引', '整合交易', 'ธุรกรรมรวม'),
    '/calc/chillPaySettlementList': T('Integrated settlement', '統合精算', '整合结算', 'การชำระรวม'),
    '/calc/payNotiList': T('Notify log', 'ノティ一覧', '通知记录', 'บันทึกแจ้งเตือน'),
    '/calc/paySuccessList': T('Approved', '成功一覧', '成功交易', 'สำเร็จ'),
    '/calc/payFailList': T('Failed', '失敗一覧', '失败交易', 'ล้มเหลว'),
    '/calc/payRefundList': T('Refunds', '返金一覧', '退款', 'คืนเงิน'),
    '/calc/payForceRefundList': T('Forced refunds', '強制返金', '强制退款', 'บังคับคืนเงิน'),
    '/calc/payCancelList': T('Cancellations', '取消一覧', '取消', 'ยกเลิก'),
    '/calc/payVoidList': T('Void processing', '無効処理', '作废处理', 'โมฆะ'),
    '/calc/payEmailVoidList': T('Email void', 'メール無効', '邮件作废', 'โมฆะอีเมล'),
    '/calc/offsetCancList': T('Netting cancels', '相殺取消', '轧差取消', 'ยกเลิกหักกลบ'),
    '/pay/easyPay': T('URL payments', 'URL決済一覧', 'URL 支付记录', 'ชำระ URL'),
    '/pay/chatbotPay': T('Chatbot payments', 'チャットボット決済', '聊天机器人支付', 'ชำระแชทบอท'),
    '/pay/jpaySubscription': T('Subscription payments', '定期決済一覧', '订阅支付记录', 'รายการชำระสมัคร'),
    '/calc/calcList': T('Channel settlement', '流通網精算', '渠道结算', 'ชำระช่องทาง'),
    '/settlement/distributionList': T('Channel settlement', '流通網精算', '渠道结算', 'ชำระช่องทาง'),
    '/calc/calcGmList': T('Merchant settlement', '加盟店精算', '商户结算', 'ชำระร้านค้า'),
    '/settlement/franchiseList': T('Merchant settlement', '加盟店精算', '商户结算', 'ชำระร้านค้า'),
    '/calc/paySettlementHoldList': T('Settlement hold', '精算保留', '结算暂缓', 'พักการชำระ'),
    '/calc/feeList': T('Fee history', '手数料一覧', '手续费明细', 'ประวัติค่าธรรมเนียม'),
    '/settlement/feeList': T('Fee history', '手数料一覧', '手续费明细', 'ประวัติค่าธรรมเนียม'),
    '/calc/dailyIntegrated': T('Daily integrated', '日別統合', '按日整合', 'รวมรายวัน'),
    '/calc/dailyPay': T('Daily payments', '日別決済', '按日支付', 'ชำระรายวัน'),
    '/calc/dailyFee': T('Daily fees', '日別手数料', '按日手续费', 'ค่าธรรมเนียมรายวัน'),
    '/calc/compPointMngList': T('Recovery balance', '回収金管理', '回款管理', 'กู้คืน'),
    '/calc/balanceList': T('Balances', '残高一覧', '余额', 'ยอดคงเหลือ'),
    '/calc/unpaidMng': T('Receivables', '未収管理', '应收管理', 'ลูกหนี้'),
    '/calc/exCalcList': T('Run settlement', '精算実行', '执行结算', 'รันชำระบัญชี'),
    '/settlement/execute': T('Run settlement', '精算実行', '执行结算', 'รันชำระบัญชี'),
    '/settlement/settlementResultDistribute': T('Distribute', '精算配布', '结算下发', 'แจกจ่ายผลชำระ'),
    '/settlement/settlementResultHold': T('On hold', '精算待ち', '结算待处理', 'รอชำระ'),
    '/calc/settlementReport': T('Settlement report', '精算レポート', '结算报表', 'รายงานชำระ'),
    '/calc/collateralList': T('Collateral', '担保金一覧', '保证金', 'หลักประกัน'),
    '/settlement/collateralList': T('Collateral', '担保金一覧', '保证金', 'หลักประกัน'),
    '/noti/notiUrlMng': T('Payment notify URLs', '決済通報URL', '支付通知 URL', 'URL แจ้งชำระ'),
    '/noti/notiSendMngList': T('Payment notify send', '決済通報送信', '支付通知发送', 'ส่งแจ้งชำระ'),
    '/noti/notiCashReceiptUrlMng': T('Cash-receipt notify URLs', '現金領収通知URL', '现金收据通知 URL', 'URL ใบเสร็จ'),
    '/noti/notiCashReceiptSendMngList': T('Cash-receipt notify send', '現金領収通知送信', '现金收据通知发送', 'ส่งใบเสร็จ'),
    '/user/userMng': T('User management', 'ユーザー管理', '用户管理', 'ผู้ใช้'),
    '/set/gridSetMng': T('Column order by menu', 'メニュー別項目順', '按菜单的列顺序', 'ลำดับคอลัมน์ตามเมนู'),
    '/ops/opsMng': T('Operations', '運用管理', '运营管理', 'ปฏิบัติการ'),
    '/ops/mailLog': T('Mail management', 'メール管理', '邮件管理', 'จัดการเมล'),
    '/ops/taxReport': T('TAX report', 'TAXレポート', 'TAX报表', 'รายงาน TAX'),
    '/ops/agencyTxnList': T('Agency fees', '代行手数료', '代理手续费', 'ค่าธรรมเนียมตัวแทน'),
    '/ops/distributionTxnList': T('Channel fee details', '流通網内訳', '渠道费用明细', 'รายละเอียดค่าธรรมเนียมช่องทาง'),
    '/ops/distributionSettlement': T('Channel settlement (ops)', '流通網精算(運営)', '渠道结算(运营)', 'ชำระช่องทาง(ปฏิบัติการ)'),
    '/calc/integratedCheck': T('Integrated check', '統合チェック', '整合检查', 'ตรวจสอบรวม'),
    '/ops/integratedReport': T('Integrated report', '統合レポート', '综合报表', 'รายงานรวม'),
    '/ops/verifyReport': T('Verify report', '検証レポート', '验证报表', 'รายงานตรวจสอบ'),
    '/ops/inactiveCard': T('Card management', 'カード管理', '卡片管理', 'จัดการบัตร'),
    '/ops/notiProvision': T('NOTI management', 'ノティ管理', 'NOTI管理', 'จัดการ NOTI'),
    '/ops/integrationPlan': T('Integration plan', '連携進行案', '联调计划', 'แผนเชื่อมต่อ'),
    '/ops/jpayWorkPlan': T('JPAY-only integration', 'JPAY専用連携', 'JPAY 专用联动', 'เชื่อมต่อ JPAY เฉพาะ'),
    '/ops/merchantApiPolicy': T('Merchant API rollout', '加盟店API配布', '商户 API 发布', 'นโยบาย Merchant API'),
    '/ops/launchChecklist': T('Launch checklist', '配布チェックリスト', '上线检查清单', 'เช็กลิสต์เปิดใช้'),
    '/risk/list': T('Risk dashboard', 'リスク状況', '风险看板', 'ภาพรวมความเสี่ยง'),
    '/deploy/integrationPlan': T('Integration plan', '連携進行案', '联调计划', 'แผนเชื่อมต่อ'),
    '/deploy/jpayWorkPlan': T('JPAY-only integration', 'JPAY専用連携', 'JPAY 专用联动', 'เชื่อมต่อ JPAY เฉพาะ'),
    '/deploy/merchantApiPolicy': T('Merchant API rollout', '加盟店API配布', '商户 API 发布', 'นโยบาย Merchant API'),
    '/deploy/launchChecklist': T('Launch checklist', '配布チェックリスト', '上线检查清单', 'เช็กลิสต์เปิดใช้'),
    '/hq/hub/policy-fees': T('Fees & risk', '手数料・リスク', '手续费与风险', 'ค่าธรรมเนียมและความเสี่ยง'),
    '/hq/hub/payment-channel': T('Payments & URL', '決済・URL', '支付与 URL', 'ชำระเงินและ URL'),
    '/hq/hub/notify': T('Notify center', 'ノティセンター', '通知中心', 'ศูนย์แจ้งเตือน'),
    '/hq/hub/settlement': T('Settlement & biz days', '精算・営業日', '结算与营业日', 'ชำระบัญชีและวันทำการ'),
    '/hq/hub/org-view': T('Org & screens', '組織・画面', '组织与界面', 'องค์กรและหน้าจอ'),
    '/hq/hub/platform': T('Platform', 'プラットフォーム', '平台', 'แพลตฟอร์ม'),
    '/hq/platformReleaseNotes': T('Release notes', 'アップデート内容', '更新内容', 'ประวัติอัปเดต'),
    '/hq/hub/access': T('Access & permissions', 'アクセス・権限', '访问与权限', 'การเข้าถึงและสิทธิ์'),
    '/hq/hub/merchant-api': T('Merchant API launch', '加盟店API公開', '商户 API 发布', 'เปิดใช้ Merchant API')
  };

  function tParentSeg(seg, loc) {
    var t = seg.trim();
    if (t === '본사 정책') t = '본사정책';
    if (!t || loc === 'KO') return t;
    var row = PARENT_SEG[t];
    return row ? pick(row, loc) : t;
  }

  function tParentChain(parentKo, loc) {
    if (!parentKo || loc === 'KO') return parentKo || '';
    return String(parentKo)
      .split(/\s*>\s*/)
      .map(function (p) {
        return tParentSeg(p, loc);
      })
      .join(' > ');
  }

  function tUrlLabel(url, loc, fallbackKo) {
    if (url === '/main') return pick(SHELL.mainTab, loc);
    var info = (w.PG_MENU_INFO && w.PG_MENU_INFO[url]) || {};
    var ko = fallbackKo != null && fallbackKo !== '' ? fallbackKo : info.label || url;
    if (loc === 'KO') return ko;
    var tr = URL_TR[url];
    if (tr) return pick(tr, loc);
    return ko;
  }

  function mainLabel(loc) {
    return pick(SHELL.mainTab, loc || getLoc());
  }

  function foldSpanText(isCollapsed, loc) {
    loc = loc || getLoc();
    return isCollapsed ? pick(SHELL.foldExpand, loc) : pick(SHELL.foldCollapse, loc);
  }

  function syncActiveHeader(loc) {
    loc = loc || getLoc();
    var activeA = document.querySelector('#copyTopTabUl .tab-a.active');
    var li = activeA && activeA.closest('.copyTopTab');
    var url = li ? li.getAttribute('top_tab_url') || '' : '/main';
    if (!url) url = '/main';
    var info = (w.PG_MENU_INFO && w.PG_MENU_INFO[url]) || {};
    var catalog = li && li.getAttribute('data-pg-catalog-tab') === '1';
    var tabText = activeA ? String(activeA.textContent || '').trim() : '';
    var menuLabel = catalog && tabText ? tabText : tUrlLabel(url, loc, info.label);

    var breadcrumb = document.querySelector('.breadcrumb-item.navi, li.navi');
    if (breadcrumb) {
      if (info.parent) {
        breadcrumb.textContent = tParentChain(info.parent, loc) + ' > ' + menuLabel;
      } else {
        breadcrumb.textContent = menuLabel;
      }
    }
    var titleEl = document.getElementById('common__header__title');
    if (titleEl) {
      titleEl.innerHTML = '<i class="bi bi-chevron-right"></i> ' + escHtml(menuLabel);
      titleEl.classList.toggle('empty-title', url === '/system/noticeList');
    }
  }

  function applySideNav(loc) {
    document.querySelectorAll('#side-nav-ul > .side-nav-item').forEach(function (item) {
      var link = item.querySelector(':scope > .side-nav-link');
      if (!link) return;
      var sp = link.querySelector('span:not(.menu-arrow)');
      if (!sp) return;
      var key = resolveParentKoLabel(sp);
      sp.setAttribute('data-pg-parent-ko', key);
      sp.textContent = ' ' + (loc === 'KO' ? key : tParentSeg(key, loc)) + ' ';
    });
    document.querySelectorAll('#side-nav-ul .child-li[data-url] > a').forEach(function (a) {
      var li = a.closest('.child-li');
      var url = li && li.getAttribute('data-url');
      if (!url) return;
      var ko = resolveMenuKoLabel(a, url);
      a.setAttribute('data-pg-menu-ko', ko);
      a.textContent = loc === 'KO' ? ko : tUrlLabel(url, loc, ko);
    });
  }

  function applyTopTabs(loc) {
    var ul = document.getElementById('copyTopTabUl');
    if (!ul) return;
    ul.querySelectorAll('.copyTopTab').forEach(function (li) {
      var url = li.getAttribute('top_tab_url');
      var tabA = li.querySelector('.tab-a');
      if (!tabA || !url) return;
      if (li.getAttribute('data-pg-catalog-tab') === '1') return;
      var info = (w.PG_MENU_INFO && w.PG_MENU_INFO[url]) || {};
      var sideA = document.querySelector('#side-nav-ul .child-li[data-url="' + url + '"] > a');
      var sideKo = sideA ? resolveMenuKoLabel(sideA, url) : '';
      var fallbackKo = info.label || sideKo || tabA.getAttribute('data-pg-tab-ko') || '';
      if (fallbackKo) {
        tabA.setAttribute('data-pg-tab-ko', fallbackKo);
      }
      tabA.textContent = tUrlLabel(url, loc, fallbackKo);
    });
  }

  function applyStaticChrome(loc) {
    var el;
    el = document.getElementById('pgShellLabelIp');
    if (el) el.textContent = pick(SHELL.labelIp, loc);
    el = document.getElementById('pgShellLabelTime');
    if (el) el.textContent = pick(SHELL.labelTime, loc);
    el = document.querySelector('#pgPayListLangDropdown .pg-pay-lang-label');
    if (el) el.textContent = pick(SHELL.langLabel, loc);
    var langRoot = document.getElementById('pgPayListLangDropdown');
    if (langRoot) {
      var aria = pick(SHELL.langGroupAria, loc);
      langRoot.setAttribute('aria-label', aria);
      langRoot.setAttribute('title', aria);
    }
    el = document.querySelector('.dimmed-text');
    if (el) el.textContent = pick(SHELL.dimmed, loc);
    el = document.getElementById('tabTopAllCloseBtn');
    if (el) {
      el.innerHTML = '<i class="bi bi-x-lg"></i> ' + escHtml(pick(SHELL.closeAllTabs, loc));
    }
    el = document.getElementById('myInfoBtn');
    if (el) {
      el.innerHTML = '<i class="bi bi-person-circle me-1"></i>' + escHtml(pick(SHELL.myInfo, loc));
    }
    el = document.getElementById('logOutBtn');
    if (el) {
      el.innerHTML = '<i class="bi bi-box-arrow-right me-1"></i>' + escHtml(pick(SHELL.logOut, loc));
    }
    el = document.getElementById('pgShellMainHint1');
    if (el) el.textContent = pick(SHELL.mainHint1, loc);
    el = document.getElementById('pgShellMainHint2');
    if (el) el.textContent = pick(SHELL.mainHint2, loc);
    try {
      document.title = pick(SHELL.pageTitle, loc);
    } catch (e0) {}

    var leftMenu = document.querySelector('.left-side-menu');
    var span = document.getElementById('leftSideFoldSpan');
    if (span && leftMenu) {
      span.textContent = foldSpanText(leftMenu.classList.contains('collapsed'), loc);
    }
  }

  function apply(loc) {
    loc = loc || getLoc();
    applyStaticChrome(loc);
    try {
      if (w.PG_UI_I18N && typeof w.PG_UI_I18N.applyDom === 'function') {
        w.PG_UI_I18N.applyDom(document.body);
      }
    } catch (eShellI18nDom) {}
    /* 사이드바·탭 라벨은 URL_TR/PARENT_SEG가 최종 — applyDom(data-pg-ui-t)보다 나중에 적용 */
    applySideNav(loc);
    applyTopTabs(loc);
    syncActiveHeader(loc);
    try {
      if (typeof w.PG_refreshHeaderUserDisplay === 'function') w.PG_refreshHeaderUserDisplay();
    } catch (eHdr) {}
    try {
      if (typeof w.PG_syncTabletLaunchBoard === 'function') w.PG_syncTabletLaunchBoard();
    } catch (eTblLaunch) {}
  }

  w.PG_ADMIN_SHELL_I18N = {
    apply: apply,
    getLoc: getLoc,
    tUrlLabel: tUrlLabel,
    tParentSeg: tParentSeg,
    tParentChain: tParentChain,
    resolveMenuKoLabel: resolveMenuKoLabel,
    resolveParentKoLabel: resolveParentKoLabel,
    mainLabel: function () {
      return mainLabel(getLoc());
    },
    foldSpanText: foldSpanText,
    syncActiveHeader: syncActiveHeader,
    escHtml: escHtml,
    pickShell: function (key) {
      return pick(SHELL[key], getLoc());
    }
  };
})(typeof window !== 'undefined' ? window : globalThis);
