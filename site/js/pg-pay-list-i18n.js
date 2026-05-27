/**
 * 통합 결제내역(/calc/payList) 및 동일 그리드 변형 — 그리드·액션바 다국어 (ziobiz/NOTI 스타일: KO JP EN CH TH).
 * localStorage 키: pg_pay_list_ui_locale
 */
(function (w) {
  'use strict';

  var STORAGE_KEY = 'pg_pay_list_ui_locale';
  var USER_SET_KEY = 'pg_pay_list_ui_locale_user_set';
  var LOCALES = ['KO', 'JP', 'EN', 'CH', 'TH'];

  var UI = {
    search: { KO: '검색', EN: 'Search', JP: '検索', CH: '搜索', TH: 'ค้นหา' },
    /** 정산 실행(/settlement/execute) 검색 버튼 — 스냅 라벨이 「조회」일 때만 사용 */
    lookup: { KO: '조회', EN: 'Load', JP: '照会', CH: '查询', TH: 'โหลด' },
    refresh: { KO: '새로고침', EN: 'Refresh', JP: '再読込', CH: '刷新', TH: 'รีเฟรช' },
    excel: { KO: '엑셀다운로드', EN: 'Excel download', JP: 'Excel取得', CH: 'Excel下载', TH: 'ดาวน์โหลด Excel' },
    listExcel: { KO: '엑셀리스트다운', EN: 'Excel list download', JP: 'Excelリストダウン', CH: 'Excel列表下载', TH: 'ดาวน์โหลด Excel รายการ' },
    detailTitle: { KO: '선택 일자 상세', EN: 'Selected date detail', JP: '選択日の詳細', CH: '所选日期明细', TH: 'รายละเอียดวันที่เลือก' },
    detailTitleIr: { KO: '선택 일자 상세 (통합 결제내역)', EN: 'Selected date — integrated payments', JP: '選択日の詳細（統合決済）', CH: '所选日期明细（综合支付）', TH: 'รายละเอียดวันที่เลือก (ชำระรวม)' },
    detailTitleVr: { KO: '선택 일자 불일치', EN: 'Selected date — mismatches', JP: '選択日の不一致', CH: '所选日期不一致', TH: 'วันที่เลือก — ไม่ตรงกัน' },
    selectAll: { KO: '전체선택', EN: 'Select all', JP: '全選択', CH: '全选', TH: 'เลือกทั้งหมด' },
    empty: { KO: '조회된 데이터가 없습니다.', EN: 'No records found.', JP: '該当データがありません。', CH: '没有查询到数据。', TH: 'ไม่พบข้อมูล' },
    searchReset: { KO: '검색 초기화', EN: 'Reset search', JP: '検索リセット', CH: '重置搜索', TH: 'รีเซ็ตการค้นหา' },
    sortDesc: { KO: '내림차순', EN: 'Descending', JP: '降順', CH: '降序', TH: 'จากมากไปน้อย' },
    sortAsc: { KO: '오름차순', EN: 'Ascending', JP: '昇順', CH: '升序', TH: 'จากน้อยไปมาก' },
    sortToolbarAria: { KO: '정렬 순서', EN: 'Sort order', JP: '並び順', CH: '排序', TH: 'ลำดับการเรียง' },
    helloBtn: { KO: '헬로', EN: 'Hello', JP: 'Hello', CH: '提示', TH: 'Hello' },
    receivableReg: { KO: '미수금등록', EN: 'Register receivable', JP: '未収金登録', CH: '登记应收', TH: 'ลงทะเบียนลูกหนี้' },
    excelSample: { KO: 'SAMPLE', EN: 'SAMPLE', JP: 'SAMPLE', CH: 'SAMPLE', TH: 'SAMPLE' },
    excelReg: { KO: '엑셀등록', EN: 'Excel upload', JP: 'Excel登録', CH: 'Excel导入', TH: 'อัปโหลด Excel' },
    compReg: { KO: '등록', EN: 'Register', JP: '登録', CH: '注册', TH: 'ลงทะเบียน' },
    compInfoDetail: { KO: '상세(지역본사정보)', EN: 'Detail (regional HQ)', JP: '詳細（地域本社）', CH: '详情（区域总部）', TH: 'รายละเอียด (สำนักงานใหญ่ภูมิภาค)' },
    printSetup: { KO: '인쇄설정', EN: 'Print setup', JP: '印刷設定', CH: '打印设置', TH: 'ตั้งค่าพิมพ์' },
    cgTitle: { KO: 'VIEW SETTING', EN: 'VIEW SETTING', JP: 'VIEW SETTING', CH: 'VIEW SETTING', TH: 'VIEW SETTING' },
    cgDefault: { KO: '기본', EN: 'Default', JP: '既定', CH: '默认', TH: 'ค่าเริ่มต้น' },
    cgRelease: { KO: '해제', EN: 'Clear', JP: '解除', CH: '清除', TH: 'ยกเลิก' },
    cgSelectAllCols: { KO: '선택', EN: 'Select all', JP: '全選択', CH: '全选', TH: 'เลือกทั้งหมด' },
    cgSave: { KO: '저장', EN: 'Save', JP: '保存', CH: '保存', TH: 'บันทึก' },
    cgRestore: { KO: '복원', EN: 'Restore', JP: '復元', CH: '恢复', TH: 'คืนค่า' },
    cgRestoreTip: {
      KO: '바로 직전에 서버에 저장된 열 구성(또는 화면을 불러온 당시의 저장 상태)으로 되돌립니다.',
      EN: 'Restores the column layout last saved on the server (or when the screen was loaded).',
      JP: '直前にサーバーへ保存した列構成(または画面読み込み時の保存状態)に戻します。',
      CH: '恢复为上次在服务器保存的列布局（或打开本页时的保存状态）。',
      TH: 'คืนค่าเป็นการจัดคอลัมน์ที่บันทึกไว้ล่าสุดบนเซิร์ฟเวอร์ (หรือตอนโหลดหน้าจอ)'
    },
    userAdd: { KO: '추가', EN: 'Add', JP: '追加', CH: '添加', TH: 'เพิ่ม' },
    hqPgApiOpSave: { KO: '운영 저장', EN: 'Save operational', JP: '運用を保存', CH: '保存运营', TH: 'บันทึกการทำงาน' },
    hqPgApiAdd: { KO: 'PG사 연동 추가', EN: 'Add PG linkage', JP: 'PG連携を追加', CH: '添加 PG 对接', TH: 'เพิ่มการเชื่อม PG' },
    commissionSetting: { KO: '수수료설정', EN: 'Fee settings', JP: '手数料設定', CH: '手续费设置', TH: 'ตั้งค่าค่าธรรมเนียม' },
    /** 정산배포(/settlement/settlementResultDistribute) 툴바 */
    settlementPublishDistribute: { KO: '배포실행', EN: 'Deploy run', JP: '配布実行', CH: '下发执行', TH: 'รันแจกจ่าย' },
    settlementPublishHold: { KO: '홀딩실행', EN: 'Hold run', JP: 'ホールド実行', CH: '暂缓执行', TH: 'รันพัก' },
    /** 정산보류내역 — [선택 건 지급보류 해제] */
    payoutHoldReleaseBulk: {
      KO: '선택 건 지급보류 해제',
      EN: 'Release payout hold (selected)',
      JP: '選択実行の支給保留解除',
      CH: '解除所选记录的支付暂缓',
      TH: 'ปลดการพักจ่ายที่เลือก'
    }
  };

  /** 공통 옵션·검색필드 라벨·placeholder (name 기준) */
  var OPT0 = { KO: '전체', EN: 'All', JP: 'すべて', CH: '全部', TH: 'ทั้งหมด' };
  var LBL = {
    searchTranFactor: { KO: '거래인자', EN: 'Search by', JP: '検索項目', CH: '查询维度', TH: 'ค้นหาตาม' },
    searchCompField: { KO: '업체', EN: 'Company', JP: '加盟店', CH: '商户', TH: 'ร้านค้า' },
    searchMid: { KO: 'MID', EN: 'MID', JP: 'MID', CH: 'MID', TH: 'MID' },
    searchNotifyChannel: { KO: '수신채널', EN: 'Notify channel', JP: '受信チャネル', CH: '通知渠道', TH: 'ช่องทางแจ้งเตือน' },
    searchPayDivCd: { KO: '상태구분', EN: 'Status type', JP: '状態区分', CH: '状态区分', TH: 'ประเภทสถานะ' },
    searchPayProcCd: { KO: '정산구분', EN: 'Settlement status', JP: '精算区分', CH: '结算状态', TH: 'สถานะการชำระบัญชี' },
    searchKeyword: { KO: '검색어', EN: 'Keyword', JP: '検索語', CH: '关键词', TH: 'คำค้น' },
    /** screens.js text 필드 기본 키 `name + ':label'` — searchKeyword 전용 */
    'searchKeyword:label': { KO: '검색어', EN: 'Keyword', JP: '検索語', CH: '关键词', TH: 'คำค้น' },
    searchPgCd: { KO: '결제대행사', EN: 'PG / acquirer', JP: '決済代行', CH: '支付机构', TH: 'ผู้ให้บริการชำระเงิน' },
    searchCycle: { KO: '정산주기', EN: 'Settlement cycle', JP: '精算サイクル', CH: '结算周期', TH: 'รอบการชำระบัญชี' },
    searchRegNo: { KO: '사업자번호', EN: 'Business reg. no.', JP: '事業者番号', CH: '营业执照号', TH: 'เลขทะเบียน' },
    searchCardAprvNo: { KO: '카드승인번호', EN: 'Card approval no.', JP: 'カード承認番号', CH: '卡授权号', TH: 'เลขอนุมัติบัตร' },
    searchChillTxnId: { KO: '피지거래번호', EN: 'ChillPay txn ID', JP: 'ChillPay取引ID', CH: 'ChillPay 交易号', TH: 'รหัสธุรกรรม ChillPay' },
    'searchMid:label': { KO: 'MID', EN: 'MID', JP: 'MID', CH: 'MID', TH: 'MID' },
    searchOrderBy: { KO: '정렬', EN: 'Sort by', JP: '並び替え', CH: '排序', TH: 'เรียงตาม' },
    searchMerchantCode: { KO: 'MID', EN: 'MID', JP: 'MID', CH: 'MID', TH: 'MID' },
    searchOrderNo: { KO: '주문번호', EN: 'Order no.', JP: '注文番号', CH: '订单号', TH: 'เลขคำสั่งซื้อ' },
    searchChillStatus: { KO: '상태', EN: 'Status', JP: '状態', CH: '状态', TH: 'สถานะ' },
    searchPaymentChannel: { KO: '채널', EN: 'Channel', JP: 'チャネル', CH: '渠道', TH: 'ช่องทาง' },
    searchRouteNo: { KO: 'Route', EN: 'Route', JP: 'ルート', CH: '路由', TH: 'Route' },
    searchFieldType: { KO: '검색구분', EN: 'Search field', JP: '検索区分', CH: '搜索字段', TH: 'ฟิลด์ค้นหา' },
    searchStatusGroup: { KO: '상태그룹', EN: 'Status group', JP: '状態グループ', CH: '状态分组', TH: 'กลุ่มสถานะ' },
    'drLbl:정산기간': { KO: '정산기간', EN: 'Settlement period', JP: '精算期間', CH: '结算期间', TH: 'ช่วงการชำระบัญชี' },
    'drLbl:정산대상기간': { KO: '정산대상기간', EN: 'Settlement window', JP: '精算対象期間', CH: '结算对象期间', TH: 'ช่วงเวลาที่ชำระครอบคลุม' },
    'drLbl:정산대상일': { KO: '정산대상일', EN: 'Settlement as-of date', JP: '精算対象日', CH: '结算目标日', TH: 'วันที่ชำระ (เป้า)' },
    searchCalcProcType: { KO: '정산구분', EN: 'Settlement type', JP: '精算区分', CH: '结算类型', TH: 'ประเภทการชำระ' },
    searchDateType: { KO: '조회기준', EN: 'Search basis', JP: '照会基準', CH: '查询依据', TH: 'เกณฑ์การค้นหา' },
    searchCompDiv: { KO: '업체구분', EN: 'Org type', JP: '組織区分', CH: '组织类型', TH: 'ประเภทองค์กร' },
    'searchCompId:label': { KO: '업체코드', EN: 'Company code', JP: '加盟店コード', CH: '商户代码', TH: 'รหัสร้าน' },
    'searchCompNm:label': { KO: '업체명', EN: 'Company name', JP: '加盟店名', CH: '商户名称', TH: 'ชื่อร้าน' },
    searchCompId: { KO: '업체코드', EN: 'Company code', JP: '加盟店コード', CH: '商户代码', TH: 'รหัสร้าน' },
    searchCompNm: { KO: '업체명', EN: 'Company name', JP: '加盟店名', CH: '商户名称', TH: 'ชื่อร้าน' },
    searchTaxScope: { KO: '보고구분', EN: 'Report scope', JP: '報告区分', CH: '报表范围', TH: 'ขอบเขตรายงาน' },
    searchYearMonth: { KO: '귀속월', EN: 'Attribution month', JP: '帰属月', CH: '归属月', TH: 'เดือนที่ครอบคลุม' },
    'searchYearMonth:label': { KO: '귀속월', EN: 'Attribution month', JP: '帰属月', CH: '归属月', TH: 'เดือนที่ครอบคลุม' },
    'drLbl:정산일': { KO: '정산일', EN: 'Settlement date', JP: '精算日', CH: '结算日', TH: 'วันชำระ' },
    'drLbl:기간': { KO: '기간', EN: 'Period', JP: '期間', CH: '期间', TH: 'ช่วงเวลา' },
    'drLbl:등록일자': { KO: '등록일자', EN: 'Registered date', JP: '登録日', CH: '登记日期', TH: 'วันที่ลงทะเบียน' },
    'drLbl:적용일(담보)': { KO: '적용일(담보)', EN: 'As-of date (collateral)', JP: '適用日（担保）', CH: '适用日（保证金）', TH: 'วันที่มีผล (หลักประกัน)' },
    'drLbl:결제일자': { KO: '결제일자', EN: 'Payment date', JP: '決済日', CH: '支付日期', TH: 'วันที่ชำระ' },
    'drLbl:전송일자': { KO: '전송일자', EN: 'Send date', JP: '送信日', CH: '发送日期', TH: 'วันที่ส่ง' },
    'drLbl:거래일자': { KO: '거래일자', EN: 'Txn date', JP: '取引日', CH: '交易日期', TH: 'วันที่ทำรายการ' },
    'drLbl:적재일': { KO: '적재일', EN: 'Ingest date', JP: '取込日', CH: '入库日', TH: 'วันที่บันทึก' },
    'drLbl:작성일': { KO: '작성일', EN: 'Created date', JP: '作成日', CH: '创建日期', TH: 'วันที่สร้าง' },
    'searchTitle:label': { KO: '제목', EN: 'Title', JP: 'タイトル', CH: '标题', TH: 'หัวข้อ' },
    searchStatus: { KO: '상태', EN: 'Status', JP: '状態', CH: '状态', TH: 'สถานะ' },
    searchReportKind: { KO: '리포트 형식', EN: 'Report format', JP: 'レポート形式', CH: '报表格式', TH: 'รูปแบบรายงาน' },
    searchReportSub: { KO: '리포트구분', EN: 'Report type', JP: 'レポート区分', CH: '报表类别', TH: 'ประเภทรายงาน' },
    searchSettlementReportMerchLbl: { KO: '가맹점코드', EN: 'Merchant code', JP: '加盟店コード', CH: '商户代码', TH: 'รหัสร้านค้า' },
    searchMasterId: { KO: '총판(상위)코드', EN: 'Master dist. code', JP: '総販（上位）コード', CH: '总代代码', TH: 'รหัสตัวแทนหลัก' },
    'searchMasterId:label': { KO: '총판(상위)코드', EN: 'Master dist. code', JP: '総販（上位）コード', CH: '总代代码', TH: 'รหัสตัวแทนหลัก' },
    searchRegionalId: { KO: '본사코드', EN: 'Regional HQ code', JP: '本社コード', CH: '本部代码', TH: 'รหัสสำนักงานใหญ่' },
    'searchRegionalId:label': { KO: '본사코드', EN: 'Regional HQ code', JP: '本社コード', CH: '本部代码', TH: 'รหัสสำนักงานใหญ่' },
    searchCurType: { KO: '통화', EN: 'Currency', JP: '通貨', CH: '币种', TH: 'สกุลเงิน' },
    searchMenuId: { KO: '메뉴 선택', EN: 'Select menu', JP: 'メニュー選択', CH: '选择菜单', TH: 'เลือกเมนู' },
    searchRiskDiv: { KO: '리스크구분', EN: 'Risk type', JP: 'リスク区分', CH: '风险类型', TH: 'ประเภทความเสี่ยง' },
    searchUrlType: { KO: 'URL구분', EN: 'URL type', JP: 'URL区分', CH: 'URL类型', TH: 'ประเภท URL' },
    searchUserId: { KO: '사용자 ID', EN: 'User ID', JP: 'ユーザーID', CH: '用户ID', TH: 'รหัสผู้ใช้' },
    'searchUserId:label': { KO: '사용자 ID', EN: 'User ID', JP: 'ユーザーID', CH: '用户ID', TH: 'รหัสผู้ใช้' },
    searchUserNm: { KO: '사용자명', EN: 'User name', JP: 'ユーザー名', CH: '用户名', TH: 'ชื่อผู้ใช้' },
    'searchUserNm:label': { KO: '사용자명', EN: 'User name', JP: 'ユーザー名', CH: '用户名', TH: 'ชื่อผู้ใช้' },
    searchUseStatus: { KO: '사용여부', EN: 'Status', JP: '使用状態', CH: '使用状态', TH: 'สถานะการใช้งาน' },
    'searchPgNm:label': { KO: 'PG사명', EN: 'PG name', JP: 'PG社名', CH: 'PG 名称', TH: 'ชื่อ PG' },
    searchUseYn: { KO: '사용여부', EN: 'In use', JP: '使用状態', CH: '使用状态', TH: 'สถานะการใช้งาน' },
    compTreeSearchUseStatus: { KO: '업체사용상태', EN: 'Company status', JP: '加盟店使用状態', CH: '商户使用状态', TH: 'สถานะการใช้งานร้าน' },
    searchPayHoldYn: { KO: '지급보류', EN: 'Payout hold', JP: '支払保留', CH: '支付暂缓', TH: 'ระงับการจ่าย' },
    'cb:searchIncludeSub': { KO: '하위업체포함', EN: 'Include sub-orgs', JP: '下位組織を含む', CH: '含下级组织', TH: 'รวมองค์กรย่อย' },
    'searchCeoNm:label': { KO: '대표자명', EN: 'CEO name', JP: '代表者名', CH: '负责人姓名', TH: 'ชื่อผู้แทน' },
    'searchTerminalId:label': { KO: '터미널ID', EN: 'Terminal ID', JP: '端末ID', CH: '终端ID', TH: 'รหัสเทอร์มินัล' },
    'searchCeoMobile:label': { KO: '휴대폰', EN: 'Mobile', JP: '携帯', CH: '手机', TH: 'มือถือ' },
    'searchChangedBy:label': { KO: '변경자명', EN: 'Changed by', JP: '変更者名', CH: '变更人', TH: 'ผู้แก้ไข' },
    'drLbl:접속일자': { KO: '접속일자', EN: 'Access date', JP: '接続日', CH: '访问日期', TH: 'วันที่เข้าใช้' },
    searchCompNmHqLabel: { KO: '업체명(본사명)', EN: 'Name (regional HQ)', JP: '店名（本社名）', CH: '名称（本部名）', TH: 'ชื่อ (สำนักงานใหญ่ภูมิภาค)' },
    searchCompOrgPick: { KO: '업체선택(조직)', EN: 'Company (org)', JP: '取引先選択（組織）', CH: '企业选择（组织）', TH: 'เลือกบริษัท (องค์กร)' },
    searchCompListUseYn: { KO: '업체사용여부', EN: 'Company in use', JP: '店舗の使用有無', CH: '商户是否使用', TH: 'ร้านเปิดใช้งานหรือไม่' }
  };
  var PH = {
    searchTranValue: { KO: '값', EN: 'Value', JP: '値', CH: '值', TH: 'ค่า' },
    searchCompQ: { KO: '업체명·코드', EN: 'Name or code', JP: '店名・コード', CH: '名称或代码', TH: 'ชื่อหรือรหัส' },
    searchMid: { KO: 'MID', EN: 'MID', JP: 'MID', CH: 'MID', TH: 'MID' },
    searchKeyword: { KO: '주문·거래·고객·칠페이 ID 등', EN: 'Order, txn, customer, ChillPay ID…', JP: '注文・取引・顧客・ChillPay ID 等', CH: '订单、交易、客户、ChillPay ID 等', TH: 'คำสั่งซื้อ ธุรกรรม ลูกค้า ChillPay ID ฯลฯ' },
    searchKeywordShort: { KO: '검색어', EN: 'Keyword', JP: '検索語', CH: '关键词', TH: 'คำค้น' },
    searchChillTxnId: { KO: '승인번호(TransactionId)', EN: 'Approval no. (TransactionId)', JP: '承認番号(TransactionId)', CH: '授权号(TransactionId)', TH: 'เลขอนุมัติ (TransactionId)' },
    searchMerchantCode: { KO: 'MerchantCode', EN: 'MerchantCode', JP: 'MerchantCode', CH: 'MerchantCode', TH: 'MerchantCode' },
    searchChillStatus: { KO: 'Paid, WaitAuthorize…', EN: 'Paid, WaitAuthorize…', JP: 'Paid, WaitAuthorize…', CH: 'Paid, WaitAuthorize…', TH: 'Paid, WaitAuthorize…' },
    searchPaymentChannel: { KO: 'Appendix B', EN: 'Appendix B', JP: 'Appendix B', CH: 'Appendix B', TH: 'Appendix B' },
    searchRouteNo: { KO: '숫자', EN: 'Number', JP: '数値', CH: '数字', TH: 'ตัวเลข' },
    searchKeywordEnPh: { KO: 'SearchKeyword', EN: 'SearchKeyword', JP: 'SearchKeyword', CH: 'SearchKeyword', TH: 'SearchKeyword' },
    searchSettlementReportCompPh: { KO: '가맹점 코드', EN: 'Merchant code', JP: '加盟店コード', CH: '商户代码', TH: 'รหัสร้านค้า' },
    searchMasterId: { KO: '총판 조직 코드', EN: 'Master dist. org code', JP: '総販組織コード', CH: '总代组织代码', TH: 'รหัสองค์กรตัวแทนหลัก' },
    searchRegionalId: { KO: '본사 지급 리포트 시 필터', EN: 'Filter for HQ payout report', JP: '本社支払レポート用フィルタ', CH: '本部拨付报表筛选', TH: 'ตัวกรองรายงานจ่ายสำนักงานใหญ่' },
    searchYearMonth: { KO: 'YYYY-MM', EN: 'YYYY-MM', JP: 'YYYY-MM', CH: 'YYYY-MM', TH: 'YYYY-MM' }
  };

  function optMap(base) {
    var o = { KO: OPT0.KO, EN: OPT0.EN, JP: OPT0.JP, CH: OPT0.CH, TH: OPT0.TH };
    if (base) {
      ['EN', 'JP', 'CH', 'TH'].forEach(function (k) {
        if (base[k]) o[k] = base[k];
      });
    }
    return o;
  }

  function isOptAllKoText(s) {
    var t = String(s == null ? '' : s).trim();
    return t === OPT0.KO;
  }

  /** OPT[name|value] 또는 한글 「전체」 공통 라벨(OPT0) */
  function resolveOptText(loc, optKey, fallback) {
    if (loc === 'KO') return fallback;
    var row = OPT[optKey];
    if (row) return row[loc] || row.EN || fallback;
    if (isOptAllKoText(fallback)) return OPT0[loc] || OPT0.EN || fallback;
    return fallback;
  }

  var OPT = {
    'searchTranFactor|ORDER_NO': optMap({ EN: 'Order no.', JP: '注文番号', CH: '订单号', TH: 'เลขคำสั่งซื้อ' }),
    'searchTranFactor|CUSTOMER_ID': optMap({ EN: 'Customer ID', JP: '顧客ID', CH: '客户ID', TH: 'รหัสลูกค้า' }),
    'searchTranFactor|TRN_ID': optMap({ EN: 'Txn ID', JP: '取引ID', CH: '交易ID', TH: 'รหัสธุรกรรม' }),
    'searchTranFactor|AMT': optMap({ EN: 'Amount', JP: '金額', CH: '金额', TH: 'จำนวนเงิน' }),
    'searchTranFactor|MERCHANT': optMap({ EN: 'Merchant', JP: '加盟店', CH: '商户', TH: 'ร้านค้า' }),
    'searchTranFactor|ROUTE': optMap({ EN: 'Route', JP: 'ルート', CH: '路由', TH: 'Route' }),
    'searchTranFactor|MID': optMap({ EN: 'MID', JP: 'MID', CH: 'MID', TH: 'MID' }),
    'searchCompField|NM': optMap({ EN: 'Name', JP: '店名', CH: '名称', TH: 'ชื่อ' }),
    'searchCompField|CODE': optMap({ EN: 'Code', JP: 'コード', CH: '代码', TH: 'รหัส' }),
    'searchPgCd|': optMap(),
    'searchCycle|': optMap(),
    'searchTranFactor|': optMap(),
    'searchCondition|': optMap(),
    'searchPayDivCd|': optMap(),
    'searchPayProcCd|': optMap(),
    'searchPayDivCd|10': optMap({ EN: 'Success', JP: '成功', CH: '成功', TH: 'สำเร็จ' }),
    'searchPayDivCd|20': optMap({ EN: 'Cancel', JP: '取消', CH: '取消', TH: 'ยกเลิก' }),
    'searchPayDivCd|FAIL': optMap({ EN: 'Fail', JP: '失敗', CH: '失败', TH: 'ล้มเหลว' }),
    'searchPayDivCd|40': optMap({ EN: 'Auto void', JP: '自動無効', CH: '自动作废', TH: 'โมฆะอัตโนมัติ' }),
    'searchPayDivCd|41': optMap({ EN: 'Email void', JP: 'メール無効', CH: '邮件作废', TH: 'โมฆะทางอีเมล' }),
    'searchPayDivCd|42': optMap({ EN: 'Auto refund', JP: '自動返金', CH: '自动退款', TH: 'คืนเงินอัตโนมัติ' }),
    'searchPayDivCd|31': optMap({ EN: 'Force refund', JP: '強制返金', CH: '强制退款', TH: 'บังคับคืนเงิน' }),
    'searchPayProcCd|10': optMap({ EN: 'Pending settlement', JP: '精算待ち', CH: '待结算', TH: 'รอชำระบัญชี' }),
    'searchPayProcCd|20': optMap({ EN: 'Settled', JP: '精算済', CH: '已结算', TH: 'ชำระแล้ว' }),
    'searchPayProcCd|30': optMap({ EN: 'Payment cancelled', JP: '決済取消', CH: '支付取消', TH: 'ยกเลิกการชำระ' }),
    'searchPayProcCd|40': optMap({ EN: 'Settlement cancelled', JP: '精算取消', CH: '结算取消', TH: 'ยกเลิกการหักบัญชี' }),
    'searchNotifyChannel|ALL': optMap({ EN: 'All', JP: 'すべて', CH: '全部', TH: 'ทั้งหมด' }),
    'searchNotifyChannel|CALLBACK': optMap({ EN: 'CALLBACK', JP: 'CALLBACK', CH: 'CALLBACK', TH: 'CALLBACK' }),
    'searchNotifyChannel|RESULT': optMap({ EN: 'RESULT URL', JP: 'RESULT URL', CH: 'RESULT URL', TH: 'RESULT URL' }),
    'searchNotifyChannel|BOTH': optMap({ EN: 'BOTH', JP: 'BOTH', CH: 'BOTH', TH: 'BOTH' }),
    'searchOrderBy|TransactionId': optMap({ EN: 'Auth / Txn ID', JP: '承認／取引ID', CH: '授权/交易ID', TH: 'รหัสอนุมัติ' }),
    'searchOrderBy|TransactionDate': optMap({ EN: 'Txn date', JP: '取引日', CH: '交易日期', TH: 'วันที่ทำรายการ' }),
    'searchOrderBy|OrderNo': optMap({ EN: 'Order no.', JP: '注文番号', CH: '订单号', TH: 'เลขคำสั่งซื้อ' }),
    'searchOrderBy|PaymentDate': optMap({ EN: 'PaymentDate', JP: 'PaymentDate', CH: 'PaymentDate', TH: 'PaymentDate' }),
    'searchOrderBy|Amount': optMap({ EN: 'Amount', JP: '金額', CH: '金额', TH: 'จำนวนเงิน' }),
    'searchOrderBy|Merchant': optMap({ EN: 'Merchant', JP: 'Merchant', CH: 'Merchant', TH: 'Merchant' }),
    'searchOrderBy|Customer': optMap({ EN: 'Customer', JP: '顧客', CH: '客户', TH: 'ลูกค้า' }),
    'searchOrderBy|Status': optMap({ EN: 'Status', JP: '状態', CH: '状态', TH: 'สถานะ' }),
    'searchFieldType|COMP_NM': optMap({ EN: 'Merchant name', JP: '加盟店名', CH: '商户名称', TH: 'ชื่อร้านค้า' }),
    'searchFieldType|COMP_ID': optMap({ EN: 'Merchant code', JP: '加盟店コード', CH: '商户代码', TH: 'รหัสร้านค้า' }),
    'searchFieldType|APPROVAL_NO': optMap({ EN: 'Approval no.', JP: '承認番号', CH: '授权号', TH: 'เลขอนุมัติ' }),
    'searchFieldType|ORDER_NO': optMap({ EN: 'Order no.', JP: '注文番号', CH: '订单号', TH: 'เลขคำสั่งซื้อ' }),
    'searchFieldType|MID': optMap({ EN: 'MID', JP: 'MID', CH: 'MID', TH: 'MID' }),
    'searchFieldType|ROUTE': optMap({ EN: 'Route', JP: 'ルート', CH: '路由', TH: 'Route' }),
    'searchFieldType|CURRENCY': optMap({ EN: 'Currency', JP: '通貨', CH: '币种', TH: 'สกุลเงิน' }),
    'searchFieldType|STATUS': optMap({ EN: 'Status', JP: '状態', CH: '状态', TH: 'สถานะ' }),
    'searchFieldType|AMOUNT': optMap({ EN: 'Amount', JP: '金額', CH: '金额', TH: 'จำนวนเงิน' }),
    'searchFieldType|CALC_CYCLE': optMap({ EN: 'Settlement cycle', JP: '精算サイクル', CH: '结算周期', TH: 'รอบการชำระ' }),
    'searchFieldType|CALC_METHOD': optMap({ EN: 'Settlement method', JP: '精算方法', CH: '结算方式', TH: 'วิธีการชำระ' }),
    'searchFieldType|SETTLEMENT_PUBLISH_STS': optMap({ EN: 'Publish status', JP: '配布状態', CH: '发布状态', TH: 'สถานะเผยแพร่' }),
    'searchFieldType|PAYOUT_HOLD_YN': optMap({ EN: 'Payout hold', JP: '支払保留', CH: '支付暂缓', TH: 'พักจ่าย' }),
    'searchFieldType|SETTLE_TARGET_DAY': optMap({ EN: 'Settlement target date', JP: '精算対象日', CH: '结算目标日', TH: 'วันที่เป้าหมายชำระ' }),
    'searchFieldType|SETTLE_RUN_DAY': optMap({ EN: 'Settlement date', JP: '精算日', CH: '结算日', TH: 'วันชำระ' }),
    'searchCalcProcType|': optMap(),
    'searchCalcProcType|MANUAL': optMap({ EN: 'Manual', JP: '手動', CH: '手动', TH: 'ด้วยมือ' }),
    'searchFieldType|CUSTOMER_ID': optMap({ EN: 'Customer ID', JP: '顧客ID', CH: '客户ID', TH: 'รหัสลูกค้า' }),
    'searchStatusGroup|SUCCESS': optMap({ EN: 'Success', JP: '成功', CH: '成功', TH: 'สำเร็จ' }),
    'searchStatusGroup|FAIL': optMap({ EN: 'Fail', JP: '失敗', CH: '失败', TH: 'ล้มเหลว' }),
    'searchStatusGroup|CANCEL': optMap({ EN: 'Cancel', JP: '取消', CH: '取消', TH: 'ยกเลิก' }),
    'searchStatusGroup|VOID': optMap({ EN: 'Void', JP: '無効', CH: '作废', TH: 'โมฆะ' }),
    'searchStatusGroup|MANUAL_VOID': optMap({ EN: 'Manual void', JP: '手動無効', CH: '手动作废', TH: 'โมฆะด้วยมือ' }),
    'searchStatusGroup|REFUND': optMap({ EN: 'Refund', JP: '返金', CH: '退款', TH: 'คืนเงิน' }),
    'searchStatusGroup|FORCE_REFUND': optMap({ EN: 'Force refund', JP: '強制返金', CH: '强制退款', TH: 'บังคับคืนเงิน' }),
    'searchStatusGroup|EXCLUDE_SUCCESS': optMap({ EN: 'Exclude success', JP: '成功除く', CH: '不含成功', TH: 'ยกเว้นสำเร็จ' }),
    'searchFieldType|ALL': optMap(),
    'searchStatusGroup|ALL': optMap(),
    'searchDateType|APPROVE': optMap({ EN: 'Approval date', JP: '承認日', CH: '授权日期', TH: 'วันที่อนุมัติ' }),
    'searchDateType|SETTLE': optMap({ EN: 'Settlement date', JP: '精算日', CH: '结算日', TH: 'วันที่ชำระ' }),
    'searchCompDiv|': optMap({ EN: 'All (by tier)', JP: 'すべて（段階別合算）', CH: '全部（按级汇总）', TH: 'ทั้งหมด (รวมตามระดับ)' }),
    'searchCompDiv|HEADQUARTERS': optMap({ EN: 'Root HQ', JP: '総本部', CH: '总总部', TH: 'สำนักงานใหญ่สุด' }),
    'searchCompDiv|REGIONAL': optMap({ EN: 'Regional HQ', JP: '本社', CH: '本部', TH: 'สำนักงานใหญ่' }),
    'searchCompDiv|MASTER_DIST': optMap({ EN: 'Master dist.', JP: '総販', CH: '总代', TH: 'ตัวแทนหลัก' }),
    'searchCompDiv|BRANCH': optMap({ EN: 'Branch', JP: '支社', CH: '分公司', TH: 'สาขา' }),
    'searchCompDiv|AGENCY': optMap({ EN: 'Agency', JP: '代理店', CH: '代理', TH: 'ตัวแทน' }),
    'searchCompDiv|SALES_OFFICE': optMap({ EN: 'Sales office', JP: '営業店', CH: '营业所', TH: 'จุดขาย' }),
    'searchStatus|': optMap(),
    'searchStatus|HOLD': optMap({ EN: 'On hold', JP: '保留中', CH: '暂扣', TH: 'พักอยู่' }),
    'searchStatus|RELEASED': optMap({ EN: 'Released', JP: '解放済', CH: '已释放', TH: 'ปลดแล้ว' }),
    'searchReportKind|MERCHANT_STMT': optMap({ EN: 'Merchant settlement report', JP: '加盟店精算レポート', CH: '商户结算报表', TH: 'รายงานชำระร้านค้า' }),
    'searchReportKind|REGIONAL_PAYOUT': optMap({ EN: 'HQ → regional payout report', JP: '本社支払レポート（総本部→本社）', CH: '总部→本部拨付报表', TH: 'รายงานจ่าย HQ→สำนักงานใหญ่' }),
    'searchReportSub|AGG': optMap({ EN: 'Settlement aggregate', JP: '精算集計', CH: '结算汇总', TH: 'สรุปการชำระ' }),
    'searchReportSub|EXE': optMap({ EN: 'Settlement runs', JP: '精算実行', CH: '结算执行', TH: 'รันชำระบัญชี' }),
    'searchReportSub|SUM': optMap({ EN: 'Summary sheet', JP: '精算集計表', CH: '结算汇总表', TH: 'แผ่นสรุป' }),
    'searchReportSub|RST': optMap({ EN: 'Confirmed settlement (report)', JP: '確定精算（レポート）', CH: '已确认结算（报表）', TH: 'ชำระที่ยืนยัน (รายงาน)' }),
    'searchCurType|': optMap(),
    'searchUrlType|': optMap(),
    'searchUrlType|PAY': optMap({ EN: 'Payment notify', JP: '決済通報', CH: '支付通知', TH: 'แจ้งชำระ' }),
    'searchUrlType|BACKGROUND': optMap({ EN: 'URL Background', JP: 'URL Background', CH: 'URL Background', TH: 'URL Background' }),
    'searchUrlType|RESULT': optMap({ EN: 'URL Result', JP: 'URL Result', CH: 'URL Result', TH: 'URL Result' }),
    'searchMenuId|': optMap({ EN: 'Select', JP: '選択', CH: '请选择', TH: 'เลือก' }),
    'searchMenuId|M0301': optMap({ EN: 'Payments', JP: '決済履歴', CH: '支付历史', TH: 'ประวัติการชำระ' }),
    'searchMenuId|M0404': optMap({ EN: 'Distribution settlement', JP: '流通網精算履歴', CH: '分销结算明细', TH: 'ชำระเครือข่าย' }),
    'searchRiskDiv|': optMap(),
    'searchUseStatus|': optMap(),
    'searchUseStatus|ACTIVE': optMap({ EN: 'Active', JP: '使用中', CH: '使用中', TH: 'ใช้งาน' }),
    'searchUseStatus|INACTIVE': optMap({ EN: 'Inactive', JP: '未使用', CH: '未使用', TH: 'ไม่ใช้งาน' }),
    'searchUseStatus|SUSPENDED': optMap({ EN: 'Suspended', JP: '永久停止', CH: '永久停用', TH: 'ระงับถาวร' }),
    'searchMailKind|': optMap(),
    'searchMailKind|VOID_TEST': optMap({ EN: 'VOID test', JP: 'VOIDテスト', CH: 'VOID 测试', TH: 'ทดสอบ VOID' }),
    'searchMailKind|VOID_TXN': optMap({ EN: 'Email void (txn)', JP: 'メール無効（取引）', CH: '邮件作废（交易）', TH: 'อีเมลโมฆะ (ธุรกรรม)' }),
    'searchMailStatus|': optMap(),
    'searchMailStatus|SUCCESS': optMap({ EN: 'Success', JP: '成功', CH: '成功', TH: 'สำเร็จ' }),
    'searchMailStatus|FAIL': optMap({ EN: 'Fail', JP: '失敗', CH: '失败', TH: 'ล้มเหลว' }),
    'searchTaxScope|WEEKLY': optMap({ EN: 'By period (confirmed runs)', JP: '期間別（確定精算実行）', CH: '按期间（已确认执行）', TH: 'ตามช่วง (รันที่ยืนยันแล้ว)' }),
    'searchTaxScope|MONTHLY': optMap({ EN: 'Monthly roll-up', JP: '月次集約（帰属月）', CH: '按月汇总（归属月）', TH: 'รวมรายเดือน (เดือนอ้างอิง)' }),
    'searchUseYn|': optMap(),
    'searchUseYn|Y': optMap({ EN: 'Active', JP: '使用', CH: '使用', TH: 'ใช้งาน' }),
    'searchUseYn|N': optMap({ EN: 'Inactive', JP: '未使用', CH: '未使用', TH: 'ไม่ใช้งาน' }),
    'searchUseYn|ALL': optMap({ EN: 'All', JP: 'すべて', CH: '全部', TH: 'ทั้งหมด' }),
    'searchPayHoldYn|': optMap(),
    'searchPayHoldYn|Y': optMap({ EN: 'On hold', JP: '保留', CH: '暂缓', TH: 'พักจ่าย' }),
    'searchPayHoldYn|N': optMap({ EN: 'Normal', JP: '正常', CH: '正常', TH: 'ปกติ' }),
    'searchCompDiv|MERCHANT': optMap({ EN: 'Merchant', JP: '加盟店', CH: '商户', TH: 'ร้านค้า' }),
    'searchPolicyCur|': optMap(),
    'searchPolicyCur|JPY': optMap({ EN: 'JPY', JP: 'JPY', CH: 'JPY', TH: 'JPY' }),
    'searchPolicyCur|USD': optMap({ EN: 'USD', JP: 'USD', CH: 'USD', TH: 'USD' }),
    'searchPolicyCur|THB': optMap({ EN: 'THB', JP: 'THB', CH: 'THB', TH: 'THB' }),
    'searchPolicyCur|CNY': optMap({ EN: 'CNY', JP: 'CNY', CH: 'CNY', TH: 'CNY' }),
    'searchPolicyCur|KRW': optMap({ EN: 'KRW', JP: 'KRW', CH: 'KRW', TH: 'KRW' })
  };

  var QD = {
    day: optMap({ EN: 'Today', JP: '当日', CH: '今天', TH: 'วันนี้' }),
    month: optMap({ EN: 'This month', JP: '当月', CH: '本月', TH: 'เดือนนี้' }),
    prevDay: optMap({ EN: 'Prev. day', JP: '前日', CH: '昨天', TH: 'เมื่อวาน' }),
    week: optMap({ EN: '1 week', JP: '1週', CH: '1周', TH: '1 สัปดาห์' }),
    week2: optMap({ EN: '2 weeks', JP: '2週', CH: '2周', TH: '2 สัปดาห์' }),
    prevMonth: optMap({ EN: 'Prev. month', JP: '前月', CH: '上月', TH: 'เดือนก่อน' }),
    prevMonth2: optMap({ EN: '2 months ago', JP: '2ヶ月前', CH: '两个月前', TH: '2 เดือนก่อน' }),
    weekCal: optMap({ EN: 'This week', JP: '今週', CH: '本周', TH: 'สัปดาห์นี้' }),
    prevWeekCal: optMap({ EN: 'Last week', JP: '先週', CH: '上周', TH: 'สัปดาห์ที่แล้ว' })
  };

  var PAY_FOLLOW = {
    AUTO_VOID: { KO: '무효처리', EN: 'Auto void', JP: '自動無効', CH: '自动作废', TH: 'โมฆะอัตโนมัติ' },
    EMAIL_VOID: { KO: '이메일 무효', EN: 'Email void', JP: 'メール無効', CH: '邮件作废', TH: 'โมฆะทางอีเมล' },
    AUTO_REFUND: { KO: '환불처리', EN: 'Auto refund', JP: '自動返金', CH: '自动退款', TH: 'คืนเงินอัตโนมัติ' },
    FORCE_REFUND: { KO: '강제환불', EN: 'Force refund', JP: '強制返金', CH: '强制退款', TH: 'บังคับคืนเงิน' }
  };

  /** setSummaryText 등에서 쓰는 한글 키 → 로케일 라벨 (값은 그대로 두고 접두만 번역) */
  var SUMMARY_LBL = {
    건수: { KO: '건수', EN: 'Count', JP: '件数', CH: '笔数', TH: 'จำนวนรายการ' },
    성공: { KO: '성공', EN: 'Success', JP: '成功', CH: '成功', TH: 'สำเร็จ' },
    실패: { KO: '실패', EN: 'Failure', JP: '失敗', CH: '失败', TH: 'ล้มเหลว' },
    금액: { KO: '금액', EN: 'Amount', JP: '金額', CH: '金额', TH: 'จำนวนเงิน' },
    수수료금액: { KO: '수수료금액', EN: 'Fee amount', JP: '手数料金額', CH: '手续费金额', TH: 'ยอดค่าธรรมเนียม' },
    수수료부가세: { KO: '수수료부가세', EN: 'Fee VAT', JP: '手数料消費税', CH: '手续费增值税', TH: 'VAT ค่าธรรมเนียม' },
    보류금액: { KO: '보류금액', EN: 'Hold amount', JP: '保留金額', CH: '暂扣金额', TH: 'ยอดพักรอ' },
    미수금: { KO: '미수금', EN: 'Receivable', JP: '未収金', CH: '应收', TH: 'ลูกหนี้' },
    정산금액: { KO: '정산금액', EN: 'Settlement amount', JP: '精算金額', CH: '结算金额', TH: 'ยอดชำระบัญชี' },
    Total: { KO: 'Total', EN: 'Total', JP: '合計', CH: '合计', TH: 'รวม' },
    수수료: { KO: '수수료', EN: 'Fee', JP: '手数料', CH: '手续费', TH: 'ค่าธรรมเนียม' },
    지급액: { KO: '지급액', EN: 'Payout', JP: '支払額', CH: '拨付金额', TH: 'ยอดจ่าย' },
    총수수료: { KO: '총수수료', EN: 'Total fees', JP: '手数料合計', CH: '手续费合计', TH: 'ค่าธรรมเนียมรวม' },
    부가세: { KO: '부가세', EN: 'VAT', JP: '消費税', CH: '增值税', TH: 'VAT' },
    지급예상합: { KO: '지급예상합', EN: 'Expected payout', JP: '支払予定合計', CH: '预计拨付合计', TH: 'ยอดจ่ายโดยประมาณ' },
    지급예상: { KO: '지급예상', EN: 'Expected payout', JP: '支払予定', CH: '预计拨付', TH: 'ยอดจ่ายโดยประมาณ' },
    정산액합: { KO: '정산액합', EN: 'Settlement total', JP: '精算額合計', CH: '结算额合计', TH: 'ยอดชำระบัญชีรวม' },
    정산예상: { KO: '정산예상', EN: 'Expected settlement', JP: '精算予定', CH: '预计结算', TH: 'ยอดชำระบัญชีโดยประมาณ' },
    환수금액: { KO: '환수금액', EN: 'Recovery', JP: '回収金額', CH: '回收金额', TH: 'ยอดกู้คืน' },
    잔여: { KO: '잔여', EN: 'Remaining', JP: '残高', CH: '剩余', TH: 'คงเหลือ' },
    결제액: { KO: '결제액', EN: 'Payment', JP: '決済額', CH: '支付金额', TH: 'ยอดชำระ' },
    '환불/취소': { KO: '환불/취소', EN: 'Refund / cancel', JP: '返金／取消', CH: '退款/取消', TH: 'คืนเงิน/ยกเลิก' },
    정산액: { KO: '정산액', EN: 'Settlement amount', JP: '精算額', CH: '结算额', TH: 'ยอดชำระบัญชี' },
    환불: { KO: '환불', EN: 'Refund', JP: '返金', CH: '退款', TH: 'คืนเงิน' },
    순액: { KO: '순액', EN: 'Net', JP: '純額', CH: '净额', TH: 'สุทธิ' },
    정산금: { KO: '정산금', EN: 'Settlement', JP: '精算金', CH: '结算款', TH: 'เงินชำระบัญชี' },
    총거래: { KO: '총거래', EN: 'Total txn amount', JP: '総取引', CH: '总交易', TH: 'ยอดธุรกรรมรวม' },
    추정결산: { KO: '추정결산', EN: 'Est. settlement', JP: '推定決算', CH: '预估结算', TH: 'ประมาณการชำระบัญชี' },
    승인: { KO: '승인', EN: 'Approved', JP: '承認', CH: '授权', TH: 'อนุมัติ' },
    취소: { KO: '취소', EN: 'Cancel', JP: '取消', CH: '取消', TH: 'ยกเลิก' },
    담보: { KO: '담보', EN: 'Collateral', JP: '担保', CH: '担保', TH: 'หลักประกัน' },
    승인금액: { KO: '승인금액', EN: 'Approved amount', JP: '承認金額', CH: '授权金额', TH: 'ยอดอนุมัติ' },
    취소금액: { KO: '취소금액', EN: 'Cancelled amount', JP: '取消金額', CH: '取消金额', TH: 'ยอดยกเลิก' },
    결제금액: { KO: '결제금액', EN: 'Payment amount', JP: '決済金額', CH: '支付金额', TH: 'ยอดชำระเงิน' },
    담보금액: { KO: '담보금액', EN: 'Collateral amount', JP: '担保金額', CH: '担保金额', TH: 'ยอดหลักประกัน' },
    잔액합계: { KO: '잔액합계', EN: 'Balance total', JP: '残高合計', CH: '余额合计', TH: 'ยอดคงเหลือรวม' },
    미수금합계: { KO: '미수금합계', EN: 'Receivable total', JP: '未収金合計', CH: '应收合计', TH: 'ยอดลูกหนี้รวม' },
    보증금: { KO: '보증금', EN: 'Deposit', JP: '保証金', CH: '保证金', TH: 'เงินประกัน' },
    정산료: { KO: '정산료', EN: 'Settlement fee', JP: '精算手数料', CH: '结算手续费', TH: 'ค่าธรรมเนียมชำระบัญชี' },
    VAT: { KO: 'VAT', EN: 'VAT', JP: 'VAT', CH: '增值税', TH: 'VAT' }
  };

  function packN(ko, en, jp, ch, th) {
    return { KO: ko, EN: en, JP: jp, CH: ch, TH: th };
  }
  /** 화면별 안내 문단 (인덱스 = noticeList 순서) */
  var NOTICES = {
    '/calc/payList': [
      packN('통합 결제내역: 칠페이 API 동기화·노티 적재·URL직접결제 등 전 출처를 한 그리드에 표시합니다. 앞쪽 컬럼(거래일~Settled)은 칠페이 거래내역 시트와 대응합니다.', 'Integrated payment history shows all sources (ChillPay API sync, inbound notify, URL pay, etc.) in one grid. Leading columns align with the ChillPay transaction sheet.', '統合決済履歴は、ChillPay API 同期・ノティ取込・URL 直決済など全ソースを一つのグリッドに表示します。先頭列は ChillPay 取引シートに対応します。', '综合支付历史在一个表格中展示所有来源（ChillPay API 同步、入账通知、URL 支付等）。前列与 ChillPay 交易表对应。', 'ประวัติการชำระเงินรวมแสดงทุกแหล่ง (ซิงค์ ChillPay API, แจ้งเวียน, URL ฯลฯ) ในตารางเดียว คอลัมน์หน้าสอดคล้องกับชีต ChillPay'),
      packN('[순서] 내림차순·오름차순은 상단 [새로고침] 왼쪽 메뉴에서 고르며, 누르는 즉시 목록을 다시 조회합니다.', '[Sort] Choose Descending/Ascending in the menu left of [Refresh]; the list reloads immediately.', '[並び順][再読込]左のメニューで降順・昇順を選ぶと、すぐに一覧を再取得します。', '[排序] 在顶部 [刷新] 左侧菜单选择升/降序，点击后立即重新查询。', '[เรียงลำดับ] เลือกจาก/มากไปน้อยทางเมนูซ้ายของ [รีเฟรช] แล้วรายการจะโหลดใหม่ทันที'),
      packN('[후속조치]는 본사설정 > 전산설정관리에서 기능을 켠 경우에만 동작합니다 (NOTI 환경설정과 동일).', '[Follow-up] actions run only when enabled in HQ Settings > Ledger system settings (same as NOTI).', '[後続対応]は本社設定＞全算設定で有効化した場合のみ動作します（NOTI と同様）。', '[后续处理] 仅在「本社设置 > 账务系统设置」开启时生效（与 NOTI 相同）。', '[ดำเนินการต่อ] ทำงานเมื่อเปิดในตั้งค่าระบบบัญชีเท่านั้น (เหมือน NOTI)'),
      packN('취소 건에 대한 정산 수수료 및 부가세는 정산 주기에 따라 반영됩니다.', 'Settlement fees and VAT for cancelled items follow the settlement cycle.', '取消取引の精算手数料・消費税は精算サイクルに従って反映されます。', '取消交易的结算手续费与增值税按结算周期反映。', 'ค่าธรรมเนียมและ VAT ของรายการยกเลิกสะท้อนตามรอบชำระบัญชี'),
      packN('정산 주기 및 정산 수수료는 가맹점별로 상이할 수 있습니다.', 'Settlement cycle and fees may differ per merchant.', '精算サイクル・手数料は加盟店ごとに異なる場合があります。', '结算周期与手续费可能因商户而异。', 'รอบและค่าธรรมเนียมอาจต่างกันในแต่ละร้าน'),
      packN('상단 한 줄: 건수·통화별 총거래·승인·취소·수수료·담보·부가세·추정결산(승인−(취소+수수료+담보+부가세), 수수료내역과 동일 건별 산식). 아래: 성공·실패 등 상태 pill. 본사·총본사는 통화별 병기.', 'Top row: count and per-currency total txn, approve, cancel, fees, collateral, VAT, est. settlement (approve−(cancel+fees+collateral+VAT); same per-txn rules as fee list). Below: status pills. HQ shows multiple currencies.', '上段: 件数・通貨別総取引・承認・取消・手数料・担保・消費税・推定決算（承認−(取消+手数料+担保+消費税)）。下段: 状態 pill。', '首行：件数及分币种总交易、批准、取消、手续费、担保、增值税、预估结算（批准−(取消+手续费+担保+增值税)）。下方状态 pill。', 'แถวบน: จำนวนและยอดตามสกุล รวมธุรกรรม อนุมัติ ยกเลิก ค่าธรรมเนียม หลักประกัน VAT ประมาณการชำระ (อนุมัติ−(ยกเลิก+ค่าธรรมเนียม+หลักประกัน+VAT)) ด้านล่าง pill สถานะ')
    ],
    '/calc/payNotiList': [
      packN('노티내역: 통합 결제내역과 동일한 그리드입니다(칠페이 시트 컬럼·2단 헤더·요약바·후속조치 포함). 조회만 origin=NOTI(전산 노티 적재)로 제한됩니다.', 'Notify list uses the same grid as integrated payments; data is limited to origin=NOTI.', 'ノティ履歴は統合決済と同一グリッド。origin=NOTI のみ。', '通知列表与综合支付相同表格，仅 origin=NOTI。', 'รายการแจ้งเตือน: กริดเดียวกับรวม จำกัด origin=NOTI'),
      packN('ziobiz/NOTI 종합거래의 노티거래내역과 동일 성격의 데이터입니다.', 'Same nature as ziobiz/NOTI consolidated notify transactions.', 'ziobiz/NOTI 総合取引のノティ取引と同種。', '与 ziobiz/NOTI 综合交易的入账数据同类。', 'ลักษณะเดียวกับธุรกรรมแจ้งเตือน NOTI'),
      packN('[후속조치]는 본사설정 > 전산설정관리에서 기능을 켠 경우에만 동작합니다 (NOTI 환경설정과 동일).', '[Follow-up] only when enabled in ledger settings (NOTI parity).', '[後続対応]は全算設定で有効時のみ（NOTI と同様）。', '[后续处理] 需账务设置开启（同 NOTI）。', '[ดำเนินการต่อ] เมื่อเปิดในการตั้งค่า (NOTI)'),
      packN('취소 건에 대한 정산 수수료 및 부가세는 정산 주기에 따라 반영됩니다.', 'Fees/VAT for cancels follow the settlement cycle.', '取消の手数料・税は精算サイクルに従います。', '取消相关费用按结算周期。', 'ค่าธรรมเนียมยกเลิกตามรอบ'),
      packN('정산 주기 및 정산 수수료는 가맹점별로 상이할 수 있습니다.', 'Cycle and fees may differ per merchant.', 'サイクル・手数料は店舗により異なります。', '周期与手续费因商户而异。', 'รอบและค่าธรรมเนียมต่างกันได้')
    ],
    '/calc/paySuccessList': [
      packN('성공내역: 통합 결제내역에서 승인 성공(결제) 상태만 간추렸습니다.', 'Success list: approved (paid) rows only.', '成功のみ抽出。', '仅成功（已支付）记录。', 'เฉพาะรายการสำเร็จ'),
      packN('상단은 건수와 해당 상태(성공) 요약 pill만 표시합니다(일별통합과 동일). 금액·수수료 한 줄 집계는 통합 결제내역·수수료내역을 이용하세요.', 'Top shows count and success status pill only (like daily integrated). Use integrated pay list or fee list for amount/fee summary.', '上段は件数と成功 pill のみ（日次統合と同様）。金額・手数料一行は統合決済・手数料一覧を利用。', '顶部仅显示件数与成功状态 pill（同按日汇总）。金额与手续费一行请用综合支付历史或手续费明细。', 'ด้านบนแสดงจำนวนและ pill สำเร็จเท่านั้น ใช้รายการรวมหรือค่าธรรมเนียมสำหรับสรุปยอด')
    ],
    '/calc/payFailList': [
      packN('실패내역: 통합 결제내역에서 실패·거절만 간추렸습니다.', 'Failed/rejected rows only.', '失敗・拒否のみ。', '仅失败/拒绝。', 'เฉพาะล้มเหลว/ปฏิเสธ'),
      packN('상단은 건수와 해당 상태(실패) 요약 pill만 표시합니다(일별통합과 동일).', 'Top shows count and fail status pill only (like daily integrated).', '上段は件数と失敗 pill のみ。', '顶部仅显示件数与失败状态 pill。', 'ด้านบนแสดงจำนวนและ pill ล้มเหลวเท่านั้น')
    ],
    '/calc/payRefundList': [
      packN('환불처리: 통합 결제내역에서 일반·자동환불(내부 30·42)만 간추렸습니다.', 'Refund processing: refund states (30·42) only.', '返金処理: 30・42 のみ。', '退款处理：仅内部状态 30·42。', 'คืนเงิน: เฉพาะสถานะ 30·42'),
      packN('상단은 건수와 해당 상태(환불) 요약 pill만 표시합니다(일별통합과 동일).', 'Top shows count and refund status pill only.', '上段は件数と返金 pill のみ。', '顶部仅显示件数与退款状态 pill。', 'ด้านบนแสดงจำนวนและ pill คืนเงินเท่านั้น')
    ],
    '/calc/payForceRefundList': [
      packN('강제환불: 통합 결제내역에서 강제환불(내부 31)만 간추렸습니다.', 'Force refund: internal state 31 only.', '強制返金: 31 のみ。', '强制退款：仅内部状态 31。', 'บังคับคืน: เฉพาะ 31'),
      packN('상단은 건수와 해당 상태(강제환불) 요약 pill만 표시합니다(일별통합과 동일).', 'Top shows count and force-refund pill only.', '上段は件数と強制返金 pill のみ。', '顶部仅显示件数与强制退款 pill。', 'ด้านบนแสดงจำนวนและ pill บังคับคืนเท่านั้น')
    ],
    '/calc/payCancelList': [
      packN('취소내역: 통합 결제내역에서 취소만 간추렸습니다.', 'Cancellations only.', '取消のみ。', '仅取消。', 'เฉพาะยกเลิก'),
      packN('상단은 건수와 해당 상태(취소) 요약 pill만 표시합니다(일별통합과 동일).', 'Top shows count and cancel pill only.', '上段は件数と取消 pill のみ。', '顶部仅显示件数与取消 pill。', 'ด้านบนแสดงจำนวนและ pill ยกเลิกเท่านั้น')
    ],
    '/calc/payVoidList': [
      packN('무효처리: 통합 결제내역에서 자동·시스템 무효(내부 21·40)만 표시합니다. 이메일무효(22·41)는 「이메일무효」메뉴, 취소(20)와 구분됩니다.', 'Void processing: internal states 21·40 only. Email void (22·41) is on email-void menu; distinct from cancel (20).', '無効処理: 21・40 のみ。メール無効は別メニュー。', '作废处理：仅 21·40；邮件作废见专用菜单。', 'โมฆะ 21·40 เท่านั้น อีเมลโมฆะเมนูแยก'),
      packN('상단은 건수와 해당 상태(무효) 요약 pill만 표시합니다(일별통합과 동일).', 'Top shows count and void pill only.', '上段は件数と無効 pill のみ。', '顶部仅显示件数与作废 pill。', 'ด้านบนแสดงจำนวนและ pill โมฆะเท่านั้น')
    ],
    '/calc/payEmailVoidList': [
      packN('이메일무효: 통합 결제내역에서 수동·이메일 무효(내부 22·41)만 표시합니다. 자동무효(21·40)는 「무효처리」메뉴입니다.', 'Email/manual void: internal 22·41 only. Auto void (21·40) is on void menu.', 'メール無効: 22・41 のみ。自動無効は無効処理メニュー。', '邮件作废：仅 22·41；自动作废见作废菜单。', 'โมฆะอีเมล: 22·41 โมฆะอัตโนมัติเมนูแยก'),
      packN('상단은 건수와 해당 상태(이메일 무효) 요약 pill만 표시합니다(일별통합과 동일).', 'Top shows count and email-void pill only.', '上段は件数とメール無効 pill のみ。', '顶部仅显示件数与邮件作废 pill。', 'ด้านบนแสดงจำนวนและ pill อีเมลโมฆะเท่านั้น')
    ],
    '/calc/offsetCancList': [
      packN('상계취소내역: 가맹 정산에 이미 반영된 건(settled=Y)이 이후 취소·무효·환불·강제환불(내부 20·21·22·30·31·40·41·42)로 바뀐 경우만 표시합니다. 정산 전 실패(F0·99) 등은 제외됩니다.', 'Offset-cancel: rows already settled then later cancelled/void/refunded.', '相殺取消: 精算反映後に状態変化したもののみ。', '已结算后又取消/作废/退款的冲销场景。', 'หักบัญชีแล้วเปลี่ยนสถานะ'),
      packN('동일 조건으로 노티 반영 시 「환수금관리」에 POST_SETTLE_REFUND 자동 등록이 되며, 차기 정산 지급액에서 FIFO 차감됩니다(전산설정·가맹 환수모드와 동일).', 'Under the same rules, POST_SETTLE_REFUND may post to recovery; next payout deducts FIFO.', '同条件で回収管理へ連携し次回精算で FIFO 減算。', '同条件下记入回收管理，下次结算 FIFO 扣减。', 'ลงทะเบียนกู้คืน หัก FIFO รอบถัดไป')
    ],
    '/pay/easyPay': [packN('URL결제내역: 가맹점 API연동 노티 외, 플랫폼이 칠페이 결제 API로 발급한 결제수소(URL)로 발생한 전 건(성공·실패·환불·취소 등). 통합 결제내역에도 포함되며, 여기서는 origin=URL 만 조회합니다.', 'URL pay: all URL-harness transactions; this screen filters origin=URL.', 'URL 決済: origin=URL のみ。', 'URL 支付：此处仅 origin=URL。', 'URL: กรอง origin=URL')],
    '/pay/chatbotPay': [
      packN('챗봇결제내역: 웹 EFO 챗봇 결제 플로우에서 동일 칠페이(URL/카드) API로 생성·적재한 건만 표시합니다. 통합 결제내역에도 포함되며, 여기서는 origin=CHATBOT 만 조회합니다.', 'Chatbot pay: origin=CHATBOT only.', 'チャットボット決済: origin=CHATBOT。', '聊天机器人支付：仅 origin=CHATBOT。', 'แชทบอท: origin=CHATBOT'),
      packN('URL결제내역과 동일 API(/api/calc/payList)·그리드를 사용하며 payListVariant=CHATBOT_PAY 로 구분합니다.', 'Same API/grid as URL pay with payListVariant=CHATBOT_PAY.', 'URL 決済と同一 API。', '与 URL 支付相同 API。', 'API เดียวกับ URL')
    ],
    '/calc/chillPayTrList': [
      packN('ChillPay API Transaction Services — Search Payment Transaction(실시간)입니다. ICOPAY 내부 DB(pg_trnsctn)가 아니라 칠페이 서버에서 직접 목록을 가져옵니다. ziobiz/NOTI 노티미들웨어의 종합거래·피지거래내역과 유사한 용도로 쓸 수 있습니다.', 'ChillPay API — live search; data comes from ChillPay, not ICOPAY pg_trnsctn. Similar use case to ziobiz/NOTI consolidated / ChillPay views.', 'ChillPay API からリアルタイム取得。ICOPAY DB ではありません。', 'ChillPay 接口实时查询，数据来自 ChillPay 服务器。', 'ดึงจาก ChillPay แบบเรียลไทม์ ไม่ใช่ DB ICOPAY'),
      packN('자격: 배포설정 > API배포설정 또는 tb_pg_agency(ChillPay)의 MerchantCode·ApiKey·MD5 Secret Key·샌드박스 여부를 사용합니다.', 'Credentials: deploy settings / tb_pg_agency (MerchantCode, ApiKey, MD5 secret, sandbox).', '認証情報は API 配備設定／tb_pg_agency を使用。', '凭据来自部署配置与 tb_pg_agency。', 'ใช้รหัสจากการตั้งค่า API'),
      packN('순서(내림차순·오름차순)는 [새로고침] 왼쪽 메뉴에서 고르며, 누르는 즉시 다시 조회됩니다(기본 내림차순). TransactionDate 범위는 검색 기간(날짜)을 ChillPay 형식(dd/MM/yyyy HH:mm:ss)으로 변환합니다. 문서: ChillPay-API-Transaction-Services-Document-EN_v1.0.6.', 'Sort order is next to [Refresh]. Default DESC. Date range is converted to ChillPay format per API doc v1.0.6.', '並び順は[再読込]左。期間は ChillPay 形式に変換。', '排序在刷新旁，日期按文档转 ChillPay 格式。', 'เรียงลำดับข้างรีเฟรช'),
      packN('그리드 열 노출은 상단 VIEW SETTING에서 조정합니다(저장 시 사용자별로 유지). 번호·승인번호·업체명·업체코드·거래일·거래시간(JP·TH 두 줄)·루트는 그리드에 항상 표시되며 VIEW SETTING 목록에는 나오지 않습니다. 거래일은 YYYY-MM-DD(예: 2026-05-09) 형식으로 표시됩니다. 본사설정 → 조직항목설정에서 화면「통합내역」 허용 열을 제한할 수 있습니다.', 'Columns via VIEW SETTING (per user). Fixed leading columns are always visible. Transaction date is shown as YYYY-MM-DD (e.g. 2026-05-09). Org column allowance in HQ settings.', '列は VIEW SETTING。固定列は常時表示。取引日は YYYY-MM-DD（例: 2026-05-09）形式。', '列通过 VIEW SETTING 调整，前列固定。交易日期以 YYYY-MM-DD（例：2026-05-09）显示。', 'คอลัมน์ตั้งค่า VIEW วันที่ทำรายการแสดงเป็น YYYY-MM-DD (เช่น 2026-05-09)')
    ],
    '/calc/chillPaySettlementList': [
      packN(
        '상단 한 줄: 건수·총거래·승인·취소·수수료·담보·부가세·지급예상(승인−(취소+수수료+부가세))·추정결산(지급예상−담보). 아래 상태 pill과 별도입니다.',
        'Top row: count, total txn, approve, cancel, fees, collateral, VAT, expected payout, est. settlement — separate from status pills below.',
        '上段: 件数・総取引・承認・取消・手数料・担保・消費税・支払予定・推定決算。下の状態 pill とは別。',
        '首行：件数、总交易、批准、取消、手续费、担保、增值税、预计拨付、预估结算；与下方状态 pill 分开。',
        'แถวบน: สรุปยอดตามสูตรชำระบัญชี แยกจาก pill สถานะด้านล่าง'
      ),
      packN('「예정(ICOPAY)」열은 배포설정 API연동설정(tb_pg_agency)의 T+N(주말 제외 영업일·결제와 동일 시각) 또는 D+N(달력+N일·일괄 시각)으로 계산합니다. OFF·MID 미매칭이면 비웁니다. 가맹 업체정보의 결제대행사 행에서 예정모드를 비우면 연동 기본을 따르고, OFF/T/D로 덮어쓸 수 있습니다.', 'Expected (ICOPAY) uses T+N / D+N from tb_pg_agency; empty when OFF or MID mismatch.', '「予定(ICOPAY)」は T+N/D+N で算出。', '「预计(ICOPAY)」按 T+N/D+N 计算。', 'คาดการณ์ ICOPAY ตาม T+N/D+N'),
      packN('칠페이 Transaction Services — Search Settlement Transaction API로 조회합니다. 통합내역은 결제 검색 중심이고, 통합정산은 정산 검색·지급액·순액·서비스비·이체일 등 정산 원문이 다릅니다. ICOPAY 정산 실행·유통망 정산 테이블과 무관합니다.', 'Settlement search API returns settlement-centric fields; unrelated to ICOPAY settlement run tables.', '精算検索 API。決済検索とは異なる項目。', '结算查询 API，字段与支付查询不同。', 'API ค้นหาเซตเทิลเมนต์'),
      packN('「정산(이체)」열은 승인 성공 건에만 ChillPay Settled를 정산완료 / 미정산으로 보입니다. 실패·취소·환불·무효 등은 칸을 비웁니다.', 'Settled column only for successful approvals; others blank.', '「精算」列は成功時のみ。', '「结算」列仅成功时显示。', 'คอลัมน์ Settled เฉพาะสำเร็จ'),
      packN('칠페이 정산 API 정렬 키는 통합내역과 같이 TransactionId(기본)·PaymentDate 등 문서 표를 따릅니다. 상단 [새로고침] 왼쪽에서 내림차순·오름차순(OrderDir)을 고릅니다.', 'Sort keys per ChillPay doc; order next to Refresh.', '並び順はドキュメント準拠。', '排序遵循文档。', 'เรียงตามเอกสาร ChillPay'),
      packN('자격: 배포설정 > API배포설정·tb_pg_agency(ChillPay)의 MerchantCode·ApiKey·MD5 Secret Key·샌드박스와 동일합니다.', 'Same credentials as ChillPay payment search.', '認証は決済検索と同じ。', '凭据与支付查询相同。', 'ข้อมูลยืนยันเหมือนการค้นหาชำระเงิน')
    ],
    '/calc/collateralList': [
      packN('담보금(롤링): 결제(승인) 건별로 정산 실행 시 설정된 비율(%)만큼 예치되며, 보류 영업일(주말 제외·공휴일 미반영) 후 해지일에 정산 실행하면 지급액에 합산됩니다.', 'Rolling collateral: per approved payment, a settlement run withholds the configured %; after the hold business days (weekends excluded; holidays not applied), a run on/after the release date adds it to payout.', '担保金（ローリング）: 決済（承認）ごとに精算実行で設定した率(%)を預かり、保留営業日（土日除く・祝日は未反映）経過後の解放日に精算実行すると支払額に合算されます。', '滚动保证金：每笔批准支付在结算执行时按设定比例(%)暂扣；经过保留营业日（不含周末·不含节假日）后，在解放日及之后的结算执行并入拨付额。', 'หลักประกัน(โรลลิง): ต่อรายการอนุมัติ หักตาม % ที่ตั้งตอนรันชำระ หลังวันทำการพัก (ไม่นับสุดสัปดาห์·ไม่นับวันหยุด) เมื่อรันชำระในวันปลดหรือหลัง รวมในยอดจ่าย'),
      packN('비율·보류 일수: 본사설정 수수료정책의 롤링(담보금) 또는 가맹점 정산설정에서 「보류율 본사정책 따름=N」일 때 개별 보류율·일수를 사용합니다.', 'Rate / hold days: when merchant settlement has “follow HQ hold policy = N”, use per-merchant hold rate and days from HQ fee policy rolling (collateral) or merchant settlement settings.', '率・保留日数: 本社設定の手数料政策のローリング（担保金）、または加盟店精算設定で「保留率本社政策従う=N」のとき、個別の保留率・日数を使用します。', '比例与保留天数：当商户结算设置「留存率跟随总部政策=N」时，使用总部手续费政策中的滚动（保证金）或商户结算里的个别留存率与天数。', 'อัตรา/วันพัก: เมื่อตั้งค่าชำระร้านค้า「อัตราพักตาม HQ=N」 ใช้อัตราและวันพักรายร้านจากนโยบายค่าธรรมเนียม HQ หรือการตั้งค่าชำระ'),
      packN('해제일시·남은일자는 영업일 기준입니다. 루트는 해당 거래의 결제 루트(route_no)입니다.', 'Release time and remaining days are business-day based. Route is the payment route_no for that transaction.', '解放日時・残日数は営業日基準です。ルートは当該取引の決済ルート(route_no)です。', '解除日时间与剩余天数按营业日计。路由为该笔支付的 route_no。', 'เวลาปลดและวันที่เหลือคิดตามวันทำการ Route คือ route_no ของรายการนั้น')
    ],
    '/settlement/collateralList': [
      packN('담보금(롤링): 결제(승인) 건별로 정산 실행 시 설정된 비율(%)만큼 예치되며, 보류 영업일(주말 제외·공휴일 미반영) 후 해지일에 정산 실행하면 지급액에 합산됩니다.', 'Rolling collateral: per approved payment, a settlement run withholds the configured %; after the hold business days (weekends excluded; holidays not applied), a run on/after the release date adds it to payout.', '担保金（ローリング）: 決済（承認）ごとに精算実行で設定した率(%)を預かり、保留営業日（土日除く・祝日は未反映）経過後の解放日に精算実行すると支払額に合算されます。', '滚动保证金：每笔批准支付在结算执行时按设定比例(%)暂扣；经过保留营业日（不含周末·不含节假日）后，在解放日及之后的结算执行并入拨付额。', 'หลักประกัน(โรลลิง): ต่อรายการอนุมัติ หักตาม % ที่ตั้งตอนรันชำระ หลังวันทำการพัก (ไม่นับสุดสัปดาห์·ไม่นับวันหยุด) เมื่อรันชำระในวันปลดหรือหลัง รวมในยอดจ่าย'),
      packN('비율·보류 일수: 본사설정 수수료정책의 롤링(담보금) 또는 가맹점 정산설정에서 「보류율 본사정책 따름=N」일 때 개별 보류율·일수를 사용합니다.', 'Rate / hold days: when merchant settlement has “follow HQ hold policy = N”, use per-merchant hold rate and days from HQ fee policy rolling (collateral) or merchant settlement settings.', '率・保留日数: 本社設定の手数料政策のローリング（担保金）、または加盟店精算設定で「保留率本社政策従う=N」のとき、個別の保留率・日数を使用します。', '比例与保留天数：当商户结算设置「留存率跟随总部政策=N」时，使用总部手续费政策中的滚动（保证金）或商户结算里的个别留存率与天数。', 'อัตรา/วันพัก: เมื่อตั้งค่าชำระร้านค้า「อัตราพักตาม HQ=N」 ใช้อัตราและวันพักรายร้านจากนโยบายค่าธรรมเนียม HQ หรือการตั้งค่าชำระ'),
      packN('해제일시·남은일자는 영업일 기준입니다. 루트는 해당 거래의 결제 루트(route_no)입니다.', 'Release time and remaining days are business-day based. Route is the payment route_no for that transaction.', '解放日時・残日数は営業日基準です。ルートは当該取引の決済ルート(route_no)です。', '解除日时间与剩余天数按营业日计。路由为该笔支付的 route_no。', 'เวลาปลดและวันที่เหลือคิดตามวันทำการ Route คือ route_no ของรายการนั้น')
    ],
    '/calc/unpaidMng': [
      packN('「미수금」은 해당 정산 주기에 지급해야 할 금액이 부족하거나(정산금 부족), 차지백·과태료 등으로 정산 시 부족분이 생겼을 때 가맹점에 부과되는 금액입니다. 정산 실행 시 지급액에서 환수금(FIFO)을 먼저 차감한 뒤 미수금(FIFO)이 차감됩니다. 정산 지급액이 음수로 확정되면 부족분이 자동 미수로 등록되며(사유코드 AUTO_SETTLEMENT_DEFICIT, 메모에 실행ID), 차기 정산에서 양(+) 지급이 나올 때 FIFO로 먼저 처리됩니다.', 'Receivables are amounts charged to a merchant when payout for a cycle is short (e.g. short settlement, chargebacks, penalties). On a settlement run, recovery (FIFO) is deducted from payout first, then receivables (FIFO). If payout is finalized negative, the shortfall is auto-posted as receivable (reason AUTO_SETTLEMENT_DEFICIT, memo links the run id) and cleared FIFO when a later run has positive payout.', '「未収金」は、当該精算サイクルで支払うべき金額が不足する場合（精算金不足）、チャージバック・罰金などで精算時に不足が生じたときに加盟店へ課される金額です。精算実行時は支払額から回収金(FIFO)を先に差し引いた後、未収金(FIFO)を差し引きます。支払額が負で確定すると不足額が自動未収として登録され（理由コード AUTO_SETTLEMENT_DEFICIT、メモに実行ID）、次回以降プラスの支払が出たときに FIFO で先に処理されます。', '未收金指：结算周期内应付不足、拒付或罚款等产生的商户欠款。结算执行时先从拨付额扣回款(FIFO)，再扣未收(FIFO)。若拨付额为负，差额自动登记为未收（原因码 AUTO_SETTLEMENT_DEFICIT，备注含执行 ID），待后续正拨付时按 FIFO 冲减。', 'ลูกหนี้คือยอดที่เรียกเก็บจากร้านเมื่อจ่ายรอบชำระไม่พอ (เช่น ยอดชำระขาด ชาร์จแบ็ก ค่าปรับ) ตอนรันชำระหักกู้คืน FIFO จากยอดจ่ายก่อน แล้วจึงหักลูกหนี้ FIFO หากยอดจ่ายติดลบ ระบบลงทะเบียนลูกหนี้อัตโนมัติ (รหัส AUTO_SETTLEMENT_DEFICIT, เมโมผูก run ID) และหัก FIFO เมื่อรอบถัดไปมียอดจ่ายเป็นบวก'),
      packN('「미수금등록」은 총본사·본사·총판 조직 단계의 기본 권한(미수금관리 화면: 수정 이상)으로 가능하며, 지사·대리점·영업점 등은 기본 조회만입니다. 필요 시 본사권한설정에서 조직·단계별로 「미수금관리」(/calc/unpaidMng) 권한을 MODIFY/DELETE 로 올려 수동 등록을 허용할 수 있습니다.', 'Registering receivables is allowed by default at root HQ / HQ / master-distributor org levels (Receivables screen: MODIFY or higher); branches, agencies, and sales offices are view-only by default. Raise org-level permission for 「Receivables」(/calc/unpaidMng) to MODIFY/DELETE in HQ permissions if manual entry is needed.', '「未収金登録」は、総本部・本社・総販の組織段階の既定権限（未収管理画面：修正以上）で可能であり、支社・代理店・営業店などは既定では参照のみです。必要に応じて本社権限設定で組織・段階ごとに「未収管理」(/calc/unpaidMng) を MODIFY/DELETE に上げ、手動登録を許可できます。', '登记未收默认开放给总总部、总部、总代层级（应收管理：需修改级以上）；分公司、代理、营业点默认只读。可在总部权限中把「应收管理」(/calc/unpaidMng) 提到 MODIFY/DELETE 以允许手工登记。', 'การลงทะเบียนลูกหนี้เปิดให้ระดับสำนักงานใหญ่สุด/สำนักงานใหญ่/ตัวแทนหลักเป็นค่าเริ่มต้น (หน้าลูกหนี้: ต้อง MODIFY ขึ้นไป) สาขา/ตัวแทน/จุดขายอ่านอย่างเดียว ปรับสิทธิ์องค์กรเป็น MODIFY/DELETE สำหรับ /calc/unpaidMng ได้ในการตั้งค่า HQ'),
      packN('「미수금등록」은 한 창에서 가맹 검색·선택 후 <strong>추가</strong>(신규 미수금) 또는 <strong>차감</strong>(잔여 미수금 FIFO 감소)과 금액·메모를 입력합니다. 목록 행을 더블클릭하면 해당 업체가 선택된 채로 열립니다. API는 POST /api/settlement/receivable 의 direction=ADD|DEDUCT 와 동일하며, 대손·등록 취소는 writeOff·cancel API를 사용합니다.', 'In one window, search and pick a merchant, then choose **Add** (new receivable) or **Deduct** (reduce open balance FIFO) and enter amount and memo. Double-click a grid row to open with that merchant selected. Same as POST /api/settlement/receivable with direction=ADD|DEDUCT; write-offs and cancellations use writeOff/cancel APIs.', '「未収金登録」は一画面で加盟店を検索・選択し、<strong>追加</strong>（新規未収）または<strong>控除</strong>（残高未収をFIFOで減額）および金額・メモを入力します。一覧行をダブルクリックするとその加盟店が選択された状態で開きます。APIは POST /api/settlement/receivable の direction=ADD|DEDUCT と同じで、貸倒・登録取消は writeOff・cancel API を使用します。', '在同一窗口搜索并选择商户，选<strong>增加</strong>（新增应收）或<strong>扣减</strong>（按 FIFO 减少未收余额），填写金额与备注。双击表格行会以该商户打开。与 POST /api/settlement/receivable direction=ADD|DEDUCT 一致；核销与取消用 writeOff/cancel API。', 'ค้นหาและเลือกร้านในหน้าต่างเดียว จากนั้นเลือก<strong>เพิ่ม</strong>(ลูกหนี้ใหม่) หรือ<strong>หัก</strong>(ลดยอดคงค้าง FIFO) พร้อมจำนวนและเมโม ดับเบิลคลิกแถวตารางเพื่อเปิดพร้อมร้านนั้น เหมือน POST /api/settlement/receivable direction=ADD|DEDUCT; writeOff/cancel สำหรับตัดหนี้/ยกเลิก'),
      packN('가맹이 본사설정 「환수/미수금설정」에서 수동(MANUAL)인 경우에만 행의 [환수처리]로 다음 정산 마감 반영을 요청할 수 있습니다. 자동(AUTO) 가맹은 정산 시 미수금이 FIFO로 차감됩니다.', 'Only merchants set to manual recovery in HQ 「Recovery / receivables」can use row **[Request recovery]** to ask for deduction on the next settlement close. AUTO merchants have receivables deducted FIFO during settlement.', '加盟店が本社設定「回収・未収設定」で手動(MANUAL)のときのみ、行の［回収処理］で次回精算締め時の反映を依頼できます。自動(AUTO)の加盟店は精算時に未収金を FIFO で控除します。', '仅当商户在总部「回款/应收」设为手动(MANUAL)时，才可用行的【回款处理】请求在下次结算关账时扣减。自动(AUTO)商户在结算时按 FIFO 扣减应收。', 'ร้านที่ตั้งเป็น MANUAL ใน HQ 「กู้คืน/ลูกหนี้」เท่านั้นที่กด [ดำเนินการกู้คืน] ในแถวเพื่อขอหักรอบปิดชำระถัดไป ร้าน AUTO ระบบหักลูกหนี้ FIFO ตอนชำระ')
    ],
    '/settlement/unpaidMng': [
      packN('「미수금」은 해당 정산 주기에 지급해야 할 금액이 부족하거나(정산금 부족), 차지백·과태료 등으로 정산 시 부족분이 생겼을 때 가맹점에 부과되는 금액입니다. 정산 실행 시 지급액에서 환수금(FIFO)을 먼저 차감한 뒤 미수금(FIFO)이 차감됩니다. 정산 지급액이 음수로 확정되면 부족분이 자동 미수로 등록되며(사유코드 AUTO_SETTLEMENT_DEFICIT, 메모에 실행ID), 차기 정산에서 양(+) 지급이 나올 때 FIFO로 먼저 처리됩니다.', 'Receivables are amounts charged to a merchant when payout for a cycle is short (e.g. short settlement, chargebacks, penalties). On a settlement run, recovery (FIFO) is deducted from payout first, then receivables (FIFO). If payout is finalized negative, the shortfall is auto-posted as receivable (reason AUTO_SETTLEMENT_DEFICIT, memo links the run id) and cleared FIFO when a later run has positive payout.', '「未収金」は、当該精算サイクルで支払うべき金額が不足する場合（精算金不足）、チャージバック・罰金などで精算時に不足が生じたときに加盟店へ課される金額です。精算実行時は支払額から回収金(FIFO)を先に差し引いた後、未収金(FIFO)を差し引きます。支払額が負で確定すると不足額が自動未収として登録され（理由コード AUTO_SETTLEMENT_DEFICIT、メモに実行ID）、次回以降プラスの支払が出たときに FIFO で先に処理されます。', '未收金指：结算周期内应付不足、拒付或罚款等产生的商户欠款。结算执行时先从拨付额扣回款(FIFO)，再扣未收(FIFO)。若拨付额为负，差额自动登记为未收（原因码 AUTO_SETTLEMENT_DEFICIT，备注含执行 ID），待后续正拨付时按 FIFO 冲减。', 'ลูกหนี้คือยอดที่เรียกเก็บจากร้านเมื่อจ่ายรอบชำระไม่พอ (เช่น ยอดชำระขาด ชาร์จแบ็ก ค่าปรับ) ตอนรันชำระหักกู้คืน FIFO จากยอดจ่ายก่อน แล้วจึงหักลูกหนี้ FIFO หากยอดจ่ายติดลบ ระบบลงทะเบียนลูกหนี้อัตโนมัติ (รหัส AUTO_SETTLEMENT_DEFICIT, เมโมผูก run ID) และหัก FIFO เมื่อรอบถัดไปมียอดจ่ายเป็นบวก'),
      packN('「미수금등록」은 총본사·본사·총판 조직 단계의 기본 권한(미수금관리 화면: 수정 이상)으로 가능하며, 지사·대리점·영업점 등은 기본 조회만입니다. 필요 시 본사권한설정에서 조직·단계별로 「미수금관리」(/calc/unpaidMng) 권한을 MODIFY/DELETE 로 올려 수동 등록을 허용할 수 있습니다.', 'Registering receivables is allowed by default at root HQ / HQ / master-distributor org levels (Receivables screen: MODIFY or higher); branches, agencies, and sales offices are view-only by default. Raise org-level permission for 「Receivables」(/calc/unpaidMng) to MODIFY/DELETE in HQ permissions if manual entry is needed.', '「未収金登録」は、総本部・本社・総販の組織段階の既定権限（未収管理画面：修正以上）で可能であり、支社・代理店・営業店などは既定では参照のみです。必要に応じて本社権限設定で組織・段階ごとに「未収管理」(/calc/unpaidMng) を MODIFY/DELETE に上げ、手動登録を許可できます。', '登记未收默认开放给总总部、总部、总代层级（应收管理：需修改级以上）；分公司、代理、营业点默认只读。可在总部权限中把「应收管理」(/calc/unpaidMng) 提到 MODIFY/DELETE 以允许手工登记。', 'การลงทะเบียนลูกหนี้เปิดให้ระดับสำนักงานใหญ่สุด/สำนักงานใหญ่/ตัวแทนหลักเป็นค่าเริ่มต้น (หน้าลูกหนี้: ต้อง MODIFY ขึ้นไป) สาขา/ตัวแทน/จุดขายอ่านอย่างเดียว ปรับสิทธิ์องค์กรเป็น MODIFY/DELETE สำหรับ /calc/unpaidMng ได้ในการตั้งค่า HQ'),
      packN('「미수금등록」은 한 창에서 가맹 검색·선택 후 <strong>추가</strong>(신규 미수금) 또는 <strong>차감</strong>(잔여 미수금 FIFO 감소)과 금액·메모를 입력합니다. 목록 행을 더블클릭하면 해당 업체가 선택된 채로 열립니다. API는 POST /api/settlement/receivable 의 direction=ADD|DEDUCT 와 동일하며, 대손·등록 취소는 writeOff·cancel API를 사용합니다.', 'In one window, search and pick a merchant, then choose **Add** (new receivable) or **Deduct** (reduce open balance FIFO) and enter amount and memo. Double-click a grid row to open with that merchant selected. Same as POST /api/settlement/receivable with direction=ADD|DEDUCT; write-offs and cancellations use writeOff/cancel APIs.', '「未収金登録」は一画面で加盟店を検索・選択し、<strong>追加</strong>（新規未収）または<strong>控除</strong>（残高未収をFIFOで減額）および金額・メモを入力します。一覧行をダブルクリックするとその加盟店が選択された状態で開きます。APIは POST /api/settlement/receivable の direction=ADD|DEDUCT と同じで、貸倒・登録取消は writeOff・cancel API を使用します。', '在同一窗口搜索并选择商户，选<strong>增加</strong>（新增应收）或<strong>扣减</strong>（按 FIFO 减少未收余额），填写金额与备注。双击表格行会以该商户打开。与 POST /api/settlement/receivable direction=ADD|DEDUCT 一致；核销与取消用 writeOff/cancel API。', 'ค้นหาและเลือกร้านในหน้าต่างเดียว จากนั้นเลือก<strong>เพิ่ม</strong>(ลูกหนี้ใหม่) หรือ<strong>หัก</strong>(ลดยอดคงค้าง FIFO) พร้อมจำนวนและเมโม ดับเบิลคลิกแถวตารางเพื่อเปิดพร้อมร้านนั้น เหมือน POST /api/settlement/receivable direction=ADD|DEDUCT; writeOff/cancel สำหรับตัดหนี้/ยกเลิก'),
      packN('가맹이 본사설정 「환수/미수금설정」에서 수동(MANUAL)인 경우에만 행의 [환수처리]로 다음 정산 마감 반영을 요청할 수 있습니다. 자동(AUTO) 가맹은 정산 시 미수금이 FIFO로 차감됩니다.', 'Only merchants set to manual recovery in HQ 「Recovery / receivables」can use row **[Request recovery]** to ask for deduction on the next settlement close. AUTO merchants have receivables deducted FIFO during settlement.', '加盟店が本社設定「回収・未収設定」で手動(MANUAL)のときのみ、行の［回収処理］で次回精算締め時の反映を依頼できます。自動(AUTO)の加盟店は精算時に未収金を FIFO で控除します。', '仅当商户在总部「回款/应收」设为手动(MANUAL)时，才可用行的【回款处理】请求在下次结算关账时扣减。自动(AUTO)商户在结算时按 FIFO 扣减应收。', 'ร้านที่ตั้งเป็น MANUAL ใน HQ 「กู้คืน/ลูกหนี้」เท่านั้นที่กด [ดำเนินการกู้คืน] ในแถวเพื่อขอหักรอบปิดชำระถัดไป ร้าน AUTO ระบบหักลูกหนี้ FIFO ตอนชำระ')
    ],
    '/hq/pgApiMng': [
      packN('연동 용도(노티·URL·챗봇·API)와 용도별 엔드포인트를 구분해 저장합니다. URL 용도 행은 「URL금액」에서 일반(일반형) / DP(DISPLAY) / BLIND를 지정할 수 있으며, 본사 URL결제설정(FX JSON)의 해당 PG 금액 모드와 동일합니다. 노티=미들웨어 수신 매칭, URL=공개 URL 결제 플로우, 챗봇/API=PG사 API 직연동(동일 연동 URL). 목록 「연동용도」는 파스텔 색으로 구분됩니다. API Key·MD5는 목록 미노출. [삭제]는 등록일 오른쪽, 신규는 [PG사 연동 추가]입니다.', 'Integration kinds (notify, URL, chatbot, API) and per-kind endpoints are stored separately. For URL rows, set Normal / DP (DISPLAY) / BLIND under “URL amount”; this matches HQ URL pay FX JSON per PG. Notify = middleware inbound match; URL = public URL pay; Chatbot/API = direct PG API (same URL). Scope badges use pastel colors. API key / MD5 are hidden in the list. Delete sits right of reg. date; use “Add PG linkage” for new.', '連携用途（ノティ・URL・チャットボット・API）と用途別エンドポイントを分けて保存します。URL行は「URL金額」で標準／DP(DISPLAY)／BLINDを指定でき、本社URL決済設定(FX JSON)の当該PGモードと一致します。ノティ=ミドルウェア受信、URL=公開URL決済、チャットボット／API=PG API直連携。一覧の用途はパステル色。API Key・MD5は一覧非表示。削除は登録日の右、新規は「PG連携を追加」。', '按用途（通知、URL、聊天机器人、API）及各自端点分别保存。URL 行可在「URL 金额」指定标准／DP(DISPLAY)／BLIND，与总部 URL 支付 FX JSON 中该 PG 的模式一致。通知=中间件入账匹配；URL=公开 URL 支付；聊天机器人/API=直连 PG API。列表用途用浅色区分。列表不显示 API Key/MD5。删除在注册日期右侧，新增用「添加 PG 对接」。', 'บันทึกแยกตามประเภท (แจ้งเตือน URL แชทบอท API) และ endpoint ต่อประเภท แถว URL ตั้งค่า มาตรฐาน/DP/BLIND ที่คอลัมน์ URL ตรงกับ FX JSON ของ HQ แจ้งเตือน=จับคู่ middleware URL=จ่ายสาธารณะ แชทบอท/API=เชื่อม API โดยตรง ไม่แสดง API Key/MD5 ในรายการ ลบอยู่ขวาวันที่ ลงทะเบียน ใหม่ใช้ปุ่มเพิ่มการเชื่อม PG'),
      packN('통합정산 「예정(ICOPAY)」열: PG사 연동 편집에서 T+N(주말 제외 영업일·결제와 동일 시각) 또는 D+N(달력+N일·일괄 시각)을 저장합니다. OFF면 예정일을 채우지 않습니다. D는 일괄 시각(HH:mm) 필수.', '“Expected (ICOPAY)” uses T+N (business days, same time as payment) or D+N (calendar days + batch time) from the PG linkage editor. OFF leaves the date empty. D requires batch time (HH:mm).', '統合精算の「予定(ICOPAY)」列は、PG連携編集で T+N（営業日・決済と同時刻）または D+N（暦日+N・一括時刻）を保存します。OFF は予定日を空にします。D は一括時刻(HH:mm)が必須です。', '「预计(ICOPAY)」列在 PG 对接编辑中保存 T+N（营业日、与支付同时刻）或 D+N（自然日+N、批量时刻）。OFF 不填预计日。D 必须填写批量时刻(HH:mm)。', 'คอลัมน์คาด(ICOPAY) ใช้ T+N (วันทำการ เวลาเดียวกับการชำระ) หรือ D+N (ปฏิทิน+N เวลารวม) จากแก้ไขการเชื่อม PG ถ้า OFF ไม่เติมวันที่ D ต้องมีเวลารวม HH:mm'),
      packN('ChillPay는 PG코드 CHILLPAY, API·URL 엔드포인트는 ChillPayService가 병합 반영합니다. 운영 DB는 db/V35_pg_agency_integration_scope.sql 적용 후 배포하세요.', 'ChillPay expects PG codes starting with CHILLPAY; ChillPayService merges API/URL endpoints. Apply db/V35_pg_agency_integration_scope.sql to the production DB before deploy.', 'ChillPay は PGコード CHILLPAY、API/URL エンドポイントは ChillPayService がマージ反映します。本番 DB には db/V35_pg_agency_integration_scope.sql を適用してからデプロイしてください。', 'ChillPay 要求 PG 代码以 CHILLPAY 开头；API/URL 端点由 ChillPayService 合并。生产库请先执行 db/V35_pg_agency_integration_scope.sql 再部署。', 'ChillPay ใช้รหัส PG ขึ้นต้น CHILLPAY ChillPayService รวม endpoint API/URL ใช้สคริปต์ db/V35_pg_agency_integration_scope.sql กับ DB ก่อน deploy')
    ],
    '/calc/compPointMngList': [
      packN('「환수금」은 정산이 반영된 뒤(승인 건이 settled 등으로 정산에 올라간 이후) 같은 거래가 환불·취소·무효·차지백 등으로 바뀔 때 정산에서 거둬야 할 금액이 자동으로 잡히는 내역입니다. 금액은 전산설정(환수금 수수료 포함) 및 수수료내역과 동일한 건별 산식입니다. 다음 정산 지급액에서는 환수금(FIFO)을 먼저 차감한 뒤 미수금(FIFO)을 차감합니다. 거래별 산출·검증은 「회수·거래기준」(/settlement/recallMng) 화면을 참고하세요.', 'Recovery rows are created when a transaction was already reflected in settlement (e.g. settled) and later changes to refund, cancel, void, chargeback, etc. Amounts follow ledger settings (whether fees are included in recovery) and the same per-txn rules as fee history. On the next payout, recovery (FIFO) is deducted first, then receivables (FIFO). For per-txn calculation and checks, use the “Recovery by transaction” screen (/settlement/recallMng).', '「回収金」は、精算反映後（承認取引が settled 等で精算に載った後）に同一取引が返金・取消・無効・チャージバック等へ変わったとき、精算で回収すべき金額が自動計上される明細です。金額は全算設定（回収金に手数料を含むか）および手数料一覧と同じ件別計算式です。次回の支払額からは回収金(FIFO)を先に差し引いた後、未収金(FIFO)を差し引きます。取引別の算出・照合は「回収・取引基準」(/settlement/recallMng) を参照してください。', '「回款」指：交易已参与结算（如 settled）之后又变为退款、取消、作废、拒付等时，系统自动生成的应从结算侧收回的金额。金额按账务设置（回款是否含手续费）及与手续费明细相同的逐笔规则计算。下次拨付时先按 FIFO 扣回款，再扣应收。逐笔计算与核对请使用「回款·按交易」(/settlement/recallMng) 画面。', 'รายการกู้คืนเกิดเมื่อธุรกรรมถูกสะท้อนชำระแล้ว (เช่น settled) ต่อมาเปลี่ยนเป็นคืนเงิน·ยกเลิก·โมฆะ·ชาร์จแบ็ก ฯลฯ ยอดคิดตามการตั้งค่า (รวมค่าธรรมเนียมหรือไม่) และกฎรายรายการเดียวกับประวัติค่าธรรมเนียม รอบถัดไปหักกู้คืน FIFO แล้วจึงหักลูกหนี้ FIFO ตรวจรายรายการที่ /settlement/recallMng')
    ],
    '/settlement/recallMng': [
      packN('「환수금」은 정산이 반영된 뒤(승인 건이 settled 등으로 정산에 올라간 이후) 같은 거래가 환불·취소·무효·차지백 등으로 바뀔 때 정산에서 거둬야 할 금액이 자동으로 잡히는 내역입니다. 금액은 전산설정(환수금 수수료 포함) 및 수수료내역과 동일한 건별 산식입니다. 다음 정산 지급액에서는 환수금(FIFO)을 먼저 차감한 뒤 미수금(FIFO)을 차감합니다. 거래별 산출·검증은 「회수·거래기준」(/settlement/recallMng) 화면을 참고하세요.', 'Recovery rows are created when a transaction was already reflected in settlement (e.g. settled) and later changes to refund, cancel, void, chargeback, etc. Amounts follow ledger settings (whether fees are included in recovery) and the same per-txn rules as fee history. On the next payout, recovery (FIFO) is deducted first, then receivables (FIFO). For per-txn calculation and checks, use the “Recovery by transaction” screen (/settlement/recallMng).', '「回収金」は、精算反映後（承認取引が settled 等で精算に載った後）に同一取引が返金・取消・無効・チャージバック等へ変わったとき、精算で回収すべき金額が自動計上される明細です。金額は全算設定（回収金に手数料を含むか）および手数料一覧と同じ件別計算式です。次回の支払額からは回収金(FIFO)を先に差し引いた後、未収金(FIFO)を差し引きます。取引別の算出・照合は「回収・取引基準」(/settlement/recallMng) を参照してください。', '「回款」指：交易已参与结算（如 settled）之后又变为退款、取消、作废、拒付等时，系统自动生成的应从结算侧收回的金额。金额按账务设置（回款是否含手续费）及与手续费明细相同的逐笔规则计算。下次拨付时先按 FIFO 扣回款，再扣应收。逐笔计算与核对请使用「回款·按交易」(/settlement/recallMng) 画面。', 'รายการกู้คืนเกิดเมื่อธุรกรรมถูกสะท้อนชำระแล้ว (เช่น settled) ต่อมาเปลี่ยนเป็นคืนเงิน·ยกเลิก·โมฆะ·ชาร์จแบ็ก ฯลฯ ยอดคิดตามการตั้งค่า (รวมค่าธรรมเนียมหรือไม่) และกฎรายรายการเดียวกับประวัติค่าธรรมเนียม รอบถัดไปหักกู้คืน FIFO แล้วจึงหักลูกหนี้ FIFO ตรวจรายรายการที่ /settlement/recallMng')
    ],
    '/calc/settlementReport': [
      packN('[리포트 형식] 가맹점 정산 리포트: 총본사·본사·총판 등이 소속 가맹에 보내는 정산 형식. 본사 지급 리포트: 총본사가 본사(REGIONAL)에 지급할 금액을 본사 단위로 합산(총본사·본사 로그인만 선택 가능).', '[Report format] Merchant report: settlement layout sent to merchants under your org tree. HQ payout report: amounts the root HQ pays to each REGIONAL (H) company, aggregated per regional org (selectable only when logged in as root HQ or regional HQ).', '[レポート形式] 加盟店精算：組織配下の加盟店向けの精算体裁。本社支払：総本部が本社(REGIONAL)へ支払う金額を本社単位で集計（総本部・本社ログイン時のみ選択可）。', '[报表格式] 商户结算报表：向隶属商户下发的结算版式。本部拨付报表：总总部按本部(REGIONAL)汇总应付金额（仅总总部/本部登录可选）。', '[รูปแบบรายงาน] รายงานร้าน: รูปแบบชำระที่ส่งให้ร้านในโครงสร้างองค์กร รายงานจ่าย HQ: ยอดที่สำนักงานใหญ่สุดจ่ายให้แต่ละ REGIONAL รวมตามรหัส本社 (เลือกได้เมื่อล็อกอินเป็น HQ รากหรือ HQ ภูมิภาคเท่านั้น)'),
      packN('[하위 구분] 정산집계·정산실시·정산집계표·확정정산(리포트). 정산집계·정산실시·확정정산에서 실행 ID가 있는 행을 클릭하면 하단에 해당 정산 실행에 포함된 거래 목록이 표시됩니다. 집계표(SUM)는 요약 1행만 조회되며, 본사 지급 리포트의 정산실시(EXE)는 합산 행이라 실행 ID가 없을 수 있습니다.', '[Sub-type] Aggregate, runs, summary sheet, confirmed (report). Click a row with a run ID in aggregate, runs, or confirmed to show included transactions below. SUM is one summary row; HQ payout report → Runs (EXE) may be an aggregate row without a run ID.', '[下位区分] 集計・実行・集計表・確定精算（レポート）。集計・実行・確定精算で実行IDがある行をクリックすると、下部に当該精算実行に含まれる取引一覧が表示されます。集計表(SUM)は要約1行のみで、本社支払レポートの実行(EXE)は合算行のため実行IDがない場合があります。', '[子类型] 结算汇总、执行、汇总表、已确认（报表）。在汇总、执行或已确认中点击含执行 ID 的行，可在下方显示该执行包含的交易列表。汇总表(SUM)仅一行摘要；本部拨付报表的执行(EXE)可能为汇总行而无执行 ID。', '[ประเภทย่อย] สรุป / รัน / แผ่นสรุป / ยืนยันแล้ว คลิกแถวที่มี run ID ใน AGG/EXE/RST เพื่อแสดงธุรกรรมด้านล่าง SUM เป็นหนึ่งแถวสรุป EXE รายงานจ่าย HQ อาจไม่มี run ID'),
      packN('정산집계·정산실시의 비율형 수수료·건당수수료·부가세는 수수료 정책·거래 상태별 수수료내역 계산과 동일 규칙으로 집계합니다. 통화 열은 정책 통화(THB/KRW/USD/JPY 등)입니다.', 'Percentage fees, per-txn fees, and VAT in aggregate and runs are summed using the same rules as fee policy and per-status fee history. The currency column shows policy currency (THB/KRW/USD/JPY, etc.).', '精算集計・実行の比率型手数료・件当手数료・消費税は、手数料政策・取引状態別の手数料一覧計算と同一ルールで集計します。通貨列は政策通貨（THB/KRW/USD/JPY 等）です。', '结算汇总、执行中的比例手续费、按笔手续费、增值税按手续费政策及各交易状态的手续费明细相同规则汇总。货币列显示政策货币（THB/KRW/USD/JPY 等）。', 'ค่าธรรมเนียม % ต่อรายการ และ VAT ใน AGG/EXE รวมตามกฎเดียวกับนโยบายและประวัติค่าธรรมเนียม คอลัมน์สกุลเงินแสดงสกุลนโยบาย (THB/KRW/USD/JPY ฯลฯ)'),
      packN('[배포 기준] 집계(AGG)·실시(EXE)·집계표(SUM)에는 정산배포가 완료된 실행(DISTRIBUTED, 레거시 null 허용)만 포함합니다. 가맹점정산내역·유통 집계와 동일합니다. 확정정산(RST)도 배포·확정된 실행만 표시합니다.', '[Publish gate] AGG/EXE/SUM include only settlement runs published as DISTRIBUTED (legacy null allowed)—same gate as merchant statements and distribution rollup. RST lists only published and CALCULATED runs.', '[配布基準] AGG/EXE/SUM は配布済み(DISTRIBUTED、レガシーnull可)の実行のみ。加盟店精算・流通集計と同一。RST も配布・確定済みのみ。', '[下发口径] AGG/EXE/SUM 仅含已下发(DISTRIBUTED，兼容历史 null)的执行，与商户结算明细、流通汇总一致。RST 亦仅已下发且已确认。', '[เกณฑ์เผยแพร่] AGG/EXE/SUM รวมเฉพาะรันที่เผยแพร่แล้วเป็น DISTRIBUTED (รองรับ null เดิม) เหมือนรายการชำระร้านและสรุปห่วงโซ่ RST เฉพาะที่เผยแพร่และ CALCULATED')
    ],
    '/settlement/settlementReport': [
      packN('[리포트 형식] 가맹점 정산 리포트: 총본사·본사·총판 등이 소속 가맹에 보내는 정산 형식. 본사 지급 리포트: 총본사가 본사(REGIONAL)에 지급할 금액을 본사 단위로 합산(총본사·본사 로그인만 선택 가능).', '[Report format] Merchant report: settlement layout sent to merchants under your org tree. HQ payout report: amounts the root HQ pays to each REGIONAL (H) company, aggregated per regional org (selectable only when logged in as root HQ or regional HQ).', '[レポート形式] 加盟店精算：組織配下の加盟店向けの精算体裁。本社支払：総本部が本社(REGIONAL)へ支払う金額を本社単位で集計（総本部・本社ログイン時のみ選択可）。', '[报表格式] 商户结算报表：向隶属商户下发的结算版式。本部拨付报表：总总部按本部(REGIONAL)汇总应付金额（仅总总部/本部登录可选）。', '[รูปแบบรายงาน] รายงานร้าน: รูปแบบชำระที่ส่งให้ร้านในโครงสร้างองค์กร รายงานจ่าย HQ: ยอดที่สำนักงานใหญ่สุดจ่ายให้แต่ละ REGIONAL รวมตามรหัส本社 (เลือกได้เมื่อล็อกอินเป็น HQ รากหรือ HQ ภูมิภาคเท่านั้น)'),
      packN('[하위 구분] 정산집계·정산실시·정산집계표·확정정산(리포트). 정산집계·정산실시·확정정산에서 실행 ID가 있는 행을 클릭하면 하단에 해당 정산 실행에 포함된 거래 목록이 표시됩니다. 집계표(SUM)는 요약 1행만 조회되며, 본사 지급 리포트의 정산실시(EXE)는 합산 행이라 실행 ID가 없을 수 있습니다.', '[Sub-type] Aggregate, runs, summary sheet, confirmed (report). Click a row with a run ID in aggregate, runs, or confirmed to show included transactions below. SUM is one summary row; HQ payout report → Runs (EXE) may be an aggregate row without a run ID.', '[下位区分] 集計・実行・集計表・確定精算（レポート）。集計・実行・確定精算で実行IDがある行をクリックすると、下部に当該精算実行に含まれる取引一覧が表示されます。集計表(SUM)は要約1行のみで、本社支払レポートの実行(EXE)は合算行のため実行IDがない場合があります。', '[子类型] 结算汇总、执行、汇总表、已确认（报表）。在汇总、执行或已确认中点击含执行 ID 的行，可在下方显示该执行包含的交易列表。汇总表(SUM)仅一行摘要；本部拨付报表的执行(EXE)可能为汇总行而无执行 ID。', '[ประเภทย่อย] สรุป / รัน / แผ่นสรุป / ยืนยันแล้ว คลิกแถวที่มี run ID ใน AGG/EXE/RST เพื่อแสดงธุรกรรมด้านล่าง SUM เป็นหนึ่งแถวสรุป EXE รายงานจ่าย HQ อาจไม่มี run ID'),
      packN('정산집계·정산실시의 비율형 수수료·건당수수료·부가세는 수수료 정책·거래 상태별 수수료내역 계산과 동일 규칙으로 집계합니다. 통화 열은 정책 통화(THB/KRW/USD/JPY 등)입니다.', 'Percentage fees, per-txn fees, and VAT in aggregate and runs are summed using the same rules as fee policy and per-status fee history. The currency column shows policy currency (THB/KRW/USD/JPY, etc.).', '精算集計・実行の比率型手数료・件当手数료・消費税は、手数料政策・取引状態別の手数料一覧計算と同一ルールで集計します。通貨列は政策通貨（THB/KRW/USD/JPY 等）です。', '结算汇总、执行中的比例手续费、按笔手续费、增值税按手续费政策及各交易状态的手续费明细相同规则汇总。货币列显示政策货币（THB/KRW/USD/JPY 等）。', 'ค่าธรรมเนียม % ต่อรายการ และ VAT ใน AGG/EXE รวมตามกฎเดียวกับนโยบายและประวัติค่าธรรมเนียม คอลัมน์สกุลเงินแสดงสกุลนโยบาย (THB/KRW/USD/JPY ฯลฯ)'),
      packN('[배포 기준] 집계(AGG)·실시(EXE)·집계표(SUM)에는 정산배포가 완료된 실행(DISTRIBUTED, 레거시 null 허용)만 포함합니다. 가맹점정산내역·유통 집계와 동일합니다. 확정정산(RST)도 배포·확정된 실행만 표시합니다.', '[Publish gate] AGG/EXE/SUM include only settlement runs published as DISTRIBUTED (legacy null allowed)—same gate as merchant statements and distribution rollup. RST lists only published and CALCULATED runs.', '[配布基準] AGG/EXE/SUM は配布済み(DISTRIBUTED、レガシーnull可)の実行のみ。加盟店精算・流通集計と同一。RST も配布・確定済みのみ。', '[下发口径] AGG/EXE/SUM 仅含已下发(DISTRIBUTED，兼容历史 null)的执行，与商户结算明细、流通汇总一致。RST 亦仅已下发且已确认。', '[เกณฑ์เผยแพร่] AGG/EXE/SUM รวมเฉพาะรันที่เผยแพร่แล้วเป็น DISTRIBUTED (รองรับ null เดิม) เหมือนรายการชำระร้านและสรุปห่วงโซ่ RST เฉพาะที่เผยแพร่และ CALCULATED')
    ],
    '/ops/taxReport': [
      packN(
        '총본사·본사(REGIONAL)·총판(MASTER_DIST) 또는 ADMIN만 사용합니다. 다른 로그인은 목록이 비어 있거나 거부됩니다.',
        'Only root HQ, regional HQ (REGIONAL), master distributor (MASTER_DIST), or ADMIN may use this screen. Other logins see an empty list or are denied.',
        '総本部・本社(REGIONAL)・総販(MASTER_DIST) または ADMIN のみ利用できます。その他のログインでは一覧が空か拒否されます。',
        '仅总总部、本部(REGIONAL)、总代(MASTER_DIST)或 ADMIN 可使用本画面；其他登录将看到空列表或被拒绝。',
        'ใช้ได้เฉพาะ HQ สูงสุด HQ ภูมิภาค(REGIONAL) ตัวแทนหลัก(MASTER_DIST) หรือ ADMIN การล็อกอินอื่นจะว่างหรือถูกปฏิเสธ'
      ),
      packN(
        '로그인 조직 트리의 하위 가맹만 대상입니다(타 총판·타 본사 가맹 제외).',
        'Only merchants under the logged-in org tree (excludes merchants under other distributors or other regional HQs).',
        'ログイン組織ツリー配下の加盟店のみが対象です（他総販・他本社配下は除く）。',
        '仅限登录组织树下属商户（不含其他总代或其他本部下属）。',
        'เฉพาะร้านในโครงสร้างองค์กรของผู้ล็อกอิน (ยกเว้นใต้ตัวแทนหลัก/สำนักงานใหญ่อื่น)'
      ),
      packN(
        '행 원천: 확정정산(CALCULATED)·정산배포(DISTRIBUTED)·가맹점정산내역 노출 규칙을 통과한 정산 실행입니다.',
        'Row source: settlement runs that passed merchant-statement visibility rules as CALCULATED and DISTRIBUTED.',
        '行の元データ: 確定精算(CALCULATED)・精算配布(DISTRIBUTED)・加盟店精算明細の表示ルールを満たした精算実行です。',
        '行来源：已通过商户结算明细展示规则的已确认(CALCULATED)、已下发(DISTRIBUTED)结算执行。',
        'แถวจากรันที่ผ่านกฎการแสดง CALCULATED และ DISTRIBUTED'
      ),
      packN(
        '「월 통합」은 귀속월(YYYY-MM) 전체를 한 번에 조회합니다. 엑셀에는 실행 목록·TOTAL·가맹별 합계가 포함됩니다.',
        'Monthly roll-up loads the entire attribution month (YYYY-MM) at once. Excel includes run lines, TOTAL, and per-merchant subtotals.',
        '「月次集約」は帰属月(YYYY-MM)全体を一度に照会します。Excel には実行一覧・TOTAL・加盟店別集計が含まれます。',
        '「按月汇总」一次性查询归属月（YYYY-MM）全月。Excel 含执行明细、TOTAL 与商户小计。',
        'โหมดรวมเดือนดึงทั้งเดือน YYYY-MM ใน Excel มีรัน TOTAL และย่อยตามร้าน'
      ),
      packN(
        'FinalPayAfterRemittance는 송금 수수료 반영 후 지급 기준액으로, 실제 은행 송금과 일치시키는 용도로 검증하세요.',
        'FinalPayAfterRemittance is the bank-alignment payout after remittance fees; use it to reconcile actual bank transfers.',
        'FinalPayAfterRemittance は送金手数料反映後の支払基準額で、実際の銀行送金との照合にご利用ください。',
        'FinalPayAfterRemittance 为扣减汇款手续费后的拨付基准金额，可与实际银行汇款核对。',
        'FinalPayAfterRemittance = ยอดจ่ายหลังค่าธรรมเนียมโอน ใช้เทียบกับโอนจริง'
      )
    ],
    '/calc/dailyIntegrated': [
      packN(
        '통합내역(칠페이 결제 검색)과 동일 자격·필터로, 거래일자 구간을 일 단위로 집계합니다. 일자별 성공·실패·취소·무효·이메일무효·환불·강제환불·기타 건수는 해당 일 집계 기준입니다(칠페이 일 100건 초과 시 금액·상태 요약은 샘플일 수 있음). 일자 행을 더블클릭하면 아래에 해당 일 통합내역 전체·총거래~추정결산 요약이 표시됩니다.',
        'Uses the same credentials and filters as integrated ChillPay payment search, aggregated by calendar day. Per-day counts follow that day’s aggregation (when a day exceeds ~100 ChillPay rows, amount/status summaries may be sampled). Double-click a date row to show all integrated rows and the financial summary for that day below.',
        '統合内訳（ChillPay 決済検索）と同一資格・フィルタで、取引日範囲を日単位で集計します。日別件数は当日の集計基準です。日付行をダブルクリックすると当日の統合内訳と総取引〜推定決算の要約を下に表示します。',
        '与「整合明细（ChillPay 支付搜索）」相同权限与筛选，按交易日区间做按日汇总。双击日期行可在下方显示该日全部整合明细及总交易〜预估结算摘要。',
        'ใช้สิทธิ์และตัวกรองเดียวกับค้นหา ChillPay รวมรายวัน ดับเบิลคลิกแถววันที่เพื่อดูรายการและสรุปยอดของวันนั้น'
      ),
      packN(
        '조회 기간은 최대 93일입니다. 칠페이 API 장애 시 해당 일에 오류 메시지가 표시될 수 있습니다.',
        'The query window is up to 93 days. If the ChillPay API errors, that day may show an error message.',
        '照会期間は最大93日です。ChillPay API 障害時は当日にエラーが表示されることがあります。',
        '查询区间最长93天。ChillPay API 故障时该日可能显示错误信息。',
        'ช่วงสูงสุด 93 วัน หาก API ChillPay ล้มเหลวอาจแสดงข้อความในวันนั้น'
      )
    ],
    '/calc/dailyPay': [
      packN(
        '결제내역(tb_pg_trnsctn, 적재일)과 동일 필터로 일자별 집계합니다. 일자별 성공·실패·취소·무효·이메일무효·환불·강제환불·기타 건수는 해당 일 전체 건 기준입니다. 일자 행을 더블클릭하면 아래에 해당 일 결제내역 전체·총거래~추정결산 요약이 표시됩니다.',
        'Same filters as payment history (tb_pg_trnsctn, ingest date), aggregated by day. Per-day bucket counts are for all rows that day. Double-click a date row to load all payment rows and the financial summary for that day below.',
        '決済履歴（tb_pg_trnsctn、取込日）と同一フィルタで日別集計します。日付行をダブルクリックすると当日の決済履歴と総取引〜推定決算の要約を下に表示します。',
        '与支付历史（tb_pg_trnsctn，入库日）相同筛选，按日汇总。双击日期行可在下方加载该日全部支付明细及总交易〜预估结算摘要。',
        'สรุปรายวันด้วยตัวกรองเดียวกับประวัติการชำระ ดับเบิลคลิกวันที่เพื่อโหลดรายการและสรุปยอดของวันนั้น'
      ),
      packN(
        '조회 기간은 최대 93일입니다.',
        'The query window is up to 93 days.',
        '照会期間は最大93日です。',
        '查询区间最长93天。',
        'ช่วงสูงสุด 93 วัน'
      )
    ],
    '/calc/feeList': [
      packN(
        '상단 한 줄: 건수·총거래·승인·취소·수수료·담보·부가세·지급예상(승인−(취소+수수료+부가세))·추정결산(지급예상−담보). 정산예상(구) 라벨은 제거되었습니다.',
        'Top row: count, total txn, approve, cancel, fees, collateral, VAT, expected payout (approve−(cancel+fees+VAT)), est. settlement (expected−collateral). Legacy “expected settlement” label removed.',
        '上段: 件数・総取引・承認・取消・手数料・担保・消費税・支払予定(承認−(取消+手数料+消費税))・推定決算(支払予定−担保)。旧「精算予定」ラベルは廃止。',
        '首行：件数、总交易、批准、取消、手续费、担保、增值税、预计拨付(批准−(取消+手续费+增值税))、预估结算(预计拨付−担保)。已移除旧「预计结算」标签。',
        'แถวบน: จำนวน ยอดรวมธุรกรรม อนุมัติ ยกเลิก ค่าธรรมเนียม หลักประกัน VAT ยอดจ่ายโดยประมาณ ประมาณการชำระบัญชี'
      ),
      packN(
        '검색: 첫 줄에서 거래일·빠른기간을 정한 뒤, 둘째 줄에서 검색구분·검색어·상태그룹을 맞추고 오른쪽 [검색]을 누릅니다. 「전체」는 해당 조건으로 좁히지 않습니다. 앞쪽 열 순서(업체·거래일·거래시간·루트·승인번호·거래번호)는 통합 결제내역 기본과 같습니다. 건당수수료 열은 거래 성공 시 과금되는 성공(건당) 고정액만 표시합니다. 기타수수료: USDT·FX는 승인금액 대비 %(「결제(%)」 합계에 포함), 3DS는 정책통화 기준 건당 고정(합계 열에는 미포함·별도 열). 세 항목은 결제·건당 등과 별도로 동시 과금될 수 있습니다. 금액이 없으면 USDT·FX·3DS 열은 — 입니다. 정산 수수료는 정산 실행 시 1회 과금되며, 송금(이체) 수수료는 그 이후 송금 처리 시 과금되어 정산리포트에 정산 수수료·송금 수수료로 각각 표시됩니다. 이 화면의 총수수료·지급예상에는 정산·송금 건당액이 포함되지 않습니다. 결제(성공): 건당·%(승인 시 부과) 열, 담보(롤링%·추정액), 지급예상액, 정산액(지급예상−담보추정). 실패·취소·무효·환불 등은 상태별 수수료 규칙을 따르며, 무효·환불 계열은 성공 건과 동일한 건당·%가 추가로 과금될 수 있습니다(이중 과금). 차감(취소·환불·무효·실패 등): 지급예상액은 0, 총수수료·부가세는 과금액(양수), 정산액은 −(총수수료+부가세)입니다. 담보 추정은 승인 건에만 표시됩니다. 본사·총판 등은 로그인 조직 하위 가맹점만 조회됩니다.',
        'Search: set transaction dates and quick range on the first row; on the second row set search field, keyword, and status group, then click [Search] on the right. [All] does not narrow that dimension. Leading columns (merchant, date, time, route, approval no., txn id) match the integrated payment list. The per-txn fee column shows only the flat success fee charged on successful transactions. Other fees: USDT·FX are % of approved amount (included in the Pay(%) total); 3DS is a fixed per-txn charge in policy currency (not in the sum totals, separate column). Those three may accrue alongside pay/per-txn fees. When there is no amount, USDT·FX·3DS show an em dash. Settlement fees are charged once per settlement run; wire/transfer fees are charged when the transfer is processed and appear separately on settlement reports as settlement fee and wire fee. This screen’s total fees and expected payout exclude settlement/wire per-txn rows. Pay (success): per-txn and % columns charged on approval; collateral (rolling % and estimate); expected payout; settlement amount (expected minus collateral estimate). Fail/cancel/void/refund follow state-specific fee rules; void/refund families may incur the same per-txn/% as success (double charge). Deductions (cancel/refund/void/fail, etc.): expected payout is 0; total fee and VAT are charged amounts (positive); settlement amount is −(total fee + VAT). Collateral estimate is shown only for approved transactions. HQ/distributors see only merchants under the logged-in organization.',
        '検索：1行目で取引日・クイック期間を指定し、2行目で検索区分・キーワード・状態グループを合わせて右の［検索］を押します。「すべて」はその条件での絞り込みを行いません。先頭列（加盟店・取引日・時刻・ルート・承認番号・取引番号）は統合決済一覧と同じ順です。件当手数料列は取引成功時のみ課される成功（件当）固定額を表示します。その他手数料：USDT・FXは承認金額比の%（「決済(%)」合計に含む）、3DSは政策通貨基準の件当固定（合計列には含まず別列）。3つは決済・件当等とは別に同時課金され得ます。金額がない場合USDT・FX・3DSは「—」です。精算手数料は精算実行時に1回課金され、送金（振込）手数料はその後の送金処理で課金され精算レポートに精算手数料・送金手数料として表示されます。この画面の手数料合計・支払予定額には精算・送金の件当は含みません。決済（成功）：件当・%（承認時）列、担保（ロール%・見積額）、支払予定額、精算額（支払予定−担保見積）。失敗・取消・無効・返金等は状態別の手数料ルールに従い、無効・返金系は成功取引と同様の件当・%が追加課金され得ます（二重課金）。控除（取消・返金・無効・失敗等）：支払予定額は0、手数料合計・消費税は課金額（正）、精算額は−(手数料合計+消費税)です。担保見積は承認取引のみ表示されます。本社・総販等はログイン組織配下の加盟店のみ照会できます。',
        '搜索：首行设交易日与快捷区间，次行设搜索字段、关键词、状态分组后点右侧【搜索】。「全部」不按该维度筛选。前列顺序（商户、交易日期、时间、路由、授权号、交易号）与综合支付列表一致。按笔手续费列仅显示成功交易时收取的固定成功费。其他费用：USDT·FX 为批准金额比例%（计入「支付(%)」合计）；3DS 为政策货币按笔固定（不计入合计列，单独列）。三者可与支付/按笔等同时计费。无金额时 USDT·FX·3DS 显示「—」。结算手续费在结算执行时收取一次；汇款（转账）手续费在后续汇款处理时收取并在结算报告中分列。本屏手续费合计与预计拨付不含结算/汇款按笔。支付（成功）：按笔与%（批准时）列、担保（滚动%·估计额）、预计拨付额、结算额（预计−担保估计）。失败·取消·作废·退款等按状态计费规则；作废·退款类可能与成功交易同样再收按笔/%（双重计费）。扣减（取消·退款·作废·失败等）：预计拨付为0；手续费合计与增值税为计费额（正）；结算额为−(手续费合计+增值税)。担保估计仅对批准交易显示。总部/总代等仅可查登录组织下属商户。',
        'ค้นหา: แถวแรกตั้งวันที่และช่วงด่วน แถวสองตั้งฟิลด์ค้นหา คำค้น กลุ่มสถานะ แล้วกด [ค้นหา] ขวา 「ทั้งหมด」ไม่กรองมิตินั้น คอลัมน์หน้าเหมือนรายการชำระรวม ค่าธรรมเนียมต่อรายการแสดงเฉพาะค่าคงที่ตอนสำเร็จ USDT·FX เป็น % ของยอดอนุมัติ (รวมใน「ชำระ(%)」) 3DS เป็นคงที่ต่อรายการตามสกุลนโยบาย (คอลัมน์แยก) ค่าธรรมเนียมชำระบัญชี/โอนต่อรายการไม่รวมในยอดรวมหน้านี้ สำเร็จ: คอลัมน์ต่อรายการ·% หลักประกัน ยอดจ่ายโดยประมาณ ยอดชำระ (ประมาณ−หลักประกัน) ล้มเหลว·ยกเลิก·โมฆะ·คืนเงิน ตามกฎสถานะ โมฆะ/คืนอาจถูกเก็บซ้ำ หัก: ยอดจ่ายโดยประมาณ=0 รวม+Vat เป็นบวก ยอดชำระ=−(รวม+Vat) ประมาณหลักประกันเฉพาะอนุมัติ เห็นเฉพาะร้านใต้องค์กรที่ล็อกอิน'
      )
    ],
    '/settlement/feeList': [
      packN(
        '상단 한 줄: 건수·총거래·승인·취소·수수료·담보·부가세·지급예상(승인−(취소+수수료+부가세))·추정결산(지급예상−담보). 정산예상(구) 라벨은 제거되었습니다.',
        'Top row: count, total txn, approve, cancel, fees, collateral, VAT, expected payout (approve−(cancel+fees+VAT)), est. settlement (expected−collateral). Legacy “expected settlement” label removed.',
        '上段: 件数・総取引・承認・取消・手数料・担保・消費税・支払予定(承認−(取消+手数料+消費税))・推定決算(支払予定−担保)。旧「精算予定」ラベルは廃止。',
        '首行：件数、总交易、批准、取消、手续费、担保、增值税、预计拨付(批准−(取消+手续费+增值税))、预估结算(预计拨付−担保)。已移除旧「预计结算」标签。',
        'แถวบน: จำนวน ยอดรวมธุรกรรม อนุมัติ ยกเลิก ค่าธรรมเนียม หลักประกัน VAT ยอดจ่ายโดยประมาณ ประมาณการชำระบัญชี'
      ),
      packN(
        '검색: 첫 줄에서 거래일·빠른기간을 정한 뒤, 둘째 줄에서 검색구분·검색어·상태그룹을 맞추고 오른쪽 [검색]을 누릅니다. 「전체」는 해당 조건으로 좁히지 않습니다. 앞쪽 열 순서(업체·거래일·거래시간·루트·승인번호·거래번호)는 통합 결제내역 기본과 같습니다. 건당수수료 열은 거래 성공 시 과금되는 성공(건당) 고정액만 표시합니다. 기타수수료: USDT·FX는 승인금액 대비 %(「결제(%)」 합계에 포함), 3DS는 정책통화 기준 건당 고정(합계 열에는 미포함·별도 열). 세 항목은 결제·건당 등과 별도로 동시 과금될 수 있습니다. 금액이 없으면 USDT·FX·3DS 열은 — 입니다. 정산 수수료는 정산 실행 시 1회 과금되며, 송금(이체) 수수료는 그 이후 송금 처리 시 과금되어 정산리포트에 정산 수수료·송금 수수료로 각각 표시됩니다. 이 화면의 총수수료·지급예상에는 정산·송금 건당액이 포함되지 않습니다. 결제(성공): 건당·%(승인 시 부과) 열, 담보(롤링%·추정액), 지급예상액, 정산액(지급예상−담보추정). 실패·취소·무효·환불 등은 상태별 수수료 규칙을 따르며, 무효·환불 계열은 성공 건과 동일한 건당·%가 추가로 과금될 수 있습니다(이중 과금). 차감(취소·환불·무효·실패 등): 지급예상액은 0, 총수수료·부가세는 과금액(양수), 정산액은 −(총수수료+부가세)입니다. 담보 추정은 승인 건에만 표시됩니다. 본사·총판 등은 로그인 조직 하위 가맹점만 조회됩니다.',
        'Search: set transaction dates and quick range on the first row; on the second row set search field, keyword, and status group, then click [Search] on the right. [All] does not narrow that dimension. Leading columns (merchant, date, time, route, approval no., txn id) match the integrated payment list. The per-txn fee column shows only the flat success fee charged on successful transactions. Other fees: USDT·FX are % of approved amount (included in the Pay(%) total); 3DS is a fixed per-txn charge in policy currency (not in the sum totals, separate column). Those three may accrue alongside pay/per-txn fees. When there is no amount, USDT·FX·3DS show an em dash. Settlement fees are charged once per settlement run; wire/transfer fees are charged when the transfer is processed and appear separately on settlement reports as settlement fee and wire fee. This screen’s total fees and expected payout exclude settlement/wire per-txn rows. Pay (success): per-txn and % columns charged on approval; collateral (rolling % and estimate); expected payout; settlement amount (expected minus collateral estimate). Fail/cancel/void/refund follow state-specific fee rules; void/refund families may incur the same per-txn/% as success (double charge). Deductions (cancel/refund/void/fail, etc.): expected payout is 0; total fee and VAT are charged amounts (positive); settlement amount is −(total fee + VAT). Collateral estimate is shown only for approved transactions. HQ/distributors see only merchants under the logged-in organization.',
        '検索：1行目で取引日・クイック期間を指定し、2行目で検索区分・キーワード・状態グループを合わせて右の［検索］を押します。「すべて」はその条件での絞り込みを行いません。先頭列（加盟店・取引日・時刻・ルート・承認番号・取引番号）は統合決済一覧と同じ順です。件当手数料列は取引成功時のみ課される成功（件当）固定額を表示します。その他手数料：USDT・FXは承認金額比の%（「決済(%)」合計に含む）、3DSは政策通貨基準の件当固定（合計列には含まず別列）。3つは決済・件当等とは別に同時課金され得ます。金額がない場合USDT・FX・3DSは「—」です。精算手数料は精算実行時に1回課金され、送金（振込）手数料はその後の送金処理で課金され精算レポートに精算手数料・送金手数料として表示されます。この画面の手数料合計・支払予定額には精算・送金の件当は含みません。決済（成功）：件当・%（承認時）列、担保（ロール%・見積額）、支払予定額、精算額（支払予定−担保見積）。失敗・取消・無効・返金等は状態別の手数料ルールに従い、無効・返金系は成功取引と同様の件当・%が追加課金され得ます（二重課金）。控除（取消・返金・無効・失敗等）：支払予定額は0、手数料合計・消費税は課金額（正）、精算額は−(手数料合計+消費税)です。担保見積は承認取引のみ表示されます。本社・総販等はログイン組織配下の加盟店のみ照会できます。',
        '搜索：首行设交易日与快捷区间，次行设搜索字段、关键词、状态分组后点右侧【搜索】。「全部」不按该维度筛选。前列顺序（商户、交易日期、时间、路由、授权号、交易号）与综合支付列表一致。按笔手续费列仅显示成功交易时收取的固定成功费。其他费用：USDT·FX 为批准金额比例%（计入「支付(%)」合计）；3DS 为政策货币按笔固定（不计入合计列，单独列）。三者可与支付/按笔等同时计费。无金额时 USDT·FX·3DS 显示「—」。结算手续费在结算执行时收取一次；汇款（转账）手续费在后续汇款处理时收取并在结算报告中分列。本屏手续费合计与预计拨付不含结算/汇款按笔。支付（成功）：按笔与%（批准时）列、担保（滚动%·估计额）、预计拨付额、结算额（预计−担保估计）。失败·取消·作废·退款等按状态计费规则；作废·退款类可能与成功交易同样再收按笔/%（双重计费）。扣减（取消·退款·作废·失败等）：预计拨付为0；手续费合计与增值税为计费额（正）；结算额为−(手续费合计+增值税)。担保估计仅对批准交易显示。总部/总代等仅可查登录组织下属商户。',
        'ค้นหา: แถวแรกตั้งวันที่และช่วงด่วน แถวสองตั้งฟิลด์ค้นหา คำค้น กลุ่มสถานะ แล้วกด [ค้นหา] ขวา 「ทั้งหมด」ไม่กรองมิตินั้น คอลัมน์หน้าเหมือนรายการชำระรวม ค่าธรรมเนียมต่อรายการแสดงเฉพาะค่าคงที่ตอนสำเร็จ USDT·FX เป็น % ของยอดอนุมัติ (รวมใน「ชำระ(%)」) 3DS เป็นคงที่ต่อรายการตามสกุลนโยบาย (คอลัมน์แยก) ค่าธรรมเนียมชำระบัญชี/โอนต่อรายการไม่รวมในยอดรวมหน้านี้ สำเร็จ: คอลัมน์ต่อรายการ·% หลักประกัน ยอดจ่ายโดยประมาณ ยอดชำระ (ประมาณ−หลักประกัน) ล้มเหลว·ยกเลิก·โมฆะ·คืนเงิน ตามกฎสถานะ โมฆะ/คืนอาจถูกเก็บซ้ำ หัก: ยอดจ่ายโดยประมาณ=0 รวม+Vat เป็นบวก ยอดชำระ=−(รวม+Vat) ประมาณหลักประกันเฉพาะอนุมัติ เห็นเฉพาะร้านใต้องค์กรที่ล็อกอิน'
      )
    ],
    '/calc/exCalcList': [
      packN(
        '이 메뉴는 정산방법이 비자동(수동·펌뱅킹 등)인 가맹을 「수동실행」하는 화면입니다. 정산방법이 자동인 가맹은 정산 배치(크론)가 돌며, 목록에는 이력이 보일 수 있으나 행 선택은 비활성입니다. 목록은 정산일(calc_dt)이 정산기간 안에 드는 실행입니다. [수동실행]: 기간 필수·동일 주기·마감·격자 규칙 적용하되 AUTO 가맹은 서버에서 제외됩니다. 검색의 정산구분은 전체·수동만 제공합니다. 자동 배치와 동일한 마감·영업일·D0 등 제약이 적용됩니다. 지급 부족 시 미수금 자동등록·환수/FIFO 규칙은 기존과 동일합니다.',
        'This screen runs settlement manually for merchants whose method is non-automatic (manual, firm banking, etc.). Automatic merchants are handled by the batch (cron); runs may appear in the list but row selection is disabled. Rows are runs whose settlement date (calc_dt) falls in the selected period. [Manual run]: period is required; same cycle, cutoff, and grid rules apply, but AUTO merchants are excluded on the server. Search settlement type offers only All / Manual. Cutoff, business days, D0, and other constraints match the automatic batch. Short payout still auto-posts receivables; recovery/FIFO rules are unchanged.',
        '本画面は、精算方法が非自動（手動・ファームバンキング等）の加盟店向けに「手動実行」するためのものです。自動の加盟店はバッチ（cron）で処理され、一覧に履歴が見えることはあっても行選択は無効です。一覧は精算日(calc_dt)が精算期間に含まれる実行です。[手動実行]：期間必須・同一周期・締め・格子ルールを適用しますが、AUTO加盟店はサーバー側で除外されます。検索の精算区分は全体・手動のみです。自動バッチと同じ締め・営業日・D0等の制約が適用されます。支払不足時の未収自動登録・回収/FIFOルールは従来と同じです。',
        '本屏对手动类（手动、银企等）非自动结算商户进行「手动执行」。自动类商户由定时批处理；列表可见历史但不可选行。列表为精算日(calc_dt)落在所选精算期间内的执行。[手动执行]：须填期间，周期·截止·网格规则相同，但服务端排除 AUTO 商户。搜索的结算类型仅「全部·手动」。截止、营业日、D0 等与自动批处理一致。拨付不足时自动登记应收及回收/FIFO 规则不变。',
        'หน้านี้รันชำระด้วยมือสำหรับร้านที่วิธีชำระไม่ใช่อัตโนมัติ ร้านอัตโนมัติถูก cron จัดการ อาจเห็นในรายการแต่เลือกแถวไม่ได้ แสดงเฉพาะรันที่ calc_dt อยู่ในช่วง [รันด้วยมือ] ต้องมีช่วงเวลา ใช้กฎรอบเดียวกันแต่ตัด AUTO ฝั่งเซิร์ฟเวอร์ ค้นหาแบ่งเพียงทั้งหมด/ด้วยมือ ข้อจำกัดเหมือน batch ยอดไม่พอลงลูกหนี้อัตโนมัติและ FIFO เหมือนเดิม'
      )
    ],
    '/settlement/execute': [
      packN(
        '이 메뉴는 정산방법이 비자동(수동·펌뱅킹 등)인 가맹을 「수동실행」하는 화면입니다. 정산방법이 자동인 가맹은 정산 배치(크론)가 돌며, 목록에는 이력이 보일 수 있으나 행 선택은 비활성입니다. 목록은 정산일(calc_dt)이 정산기간 안에 드는 실행입니다. [수동실행]: 기간 필수·동일 주기·마감·격자 규칙 적용하되 AUTO 가맹은 서버에서 제외됩니다. 검색의 정산구분은 전체·수동만 제공합니다. 자동 배치와 동일한 마감·영업일·D0 등 제약이 적용됩니다. 지급 부족 시 미수금 자동등록·환수/FIFO 규칙은 기존과 동일합니다.',
        'This screen runs settlement manually for merchants whose method is non-automatic (manual, firm banking, etc.). Automatic merchants are handled by the batch (cron); runs may appear in the list but row selection is disabled. Rows are runs whose settlement date (calc_dt) falls in the selected period. [Manual run]: period is required; same cycle, cutoff, and grid rules apply, but AUTO merchants are excluded on the server. Search settlement type offers only All / Manual. Cutoff, business days, D0, and other constraints match the automatic batch. Short payout still auto-posts receivables; recovery/FIFO rules are unchanged.',
        '本画面は、精算方法が非自動（手動・ファームバンキング等）の加盟店向けに「手動実行」するためのものです。自動の加盟店はバッチ（cron）で処理され、一覧に履歴が見えることはあっても行選択は無効です。一覧は精算日(calc_dt)が精算期間に含まれる実行です。[手動実行]：期間必須・同一周期・締め・格子ルールを適用しますが、AUTO加盟店はサーバー側で除外されます。検索の精算区分は全体・手動のみです。自動バッチと同じ締め・営業日・D0等の制約が適用されます。支払不足時の未収自動登録・回収/FIFOルールは従来と同じです。',
        '本屏对手动类（手动、银企等）非自动结算商户进行「手动执行」。自动类商户由定时批处理；列表可见历史但不可选行。列表为精算日(calc_dt)落在所选精算期间内的执行。[手动执行]：须填期间，周期·截止·网格规则相同，但服务端排除 AUTO 商户。搜索的结算类型仅「全部·手动」。截止、营业日、D0 等与自动批处理一致。拨付不足时自动登记应收及回收/FIFO 规则不变。',
        'หน้านี้รันชำระด้วยมือสำหรับร้านที่วิธีชำระไม่ใช่อัตโนมัติ ร้านอัตโนมัติถูก cron จัดการ อาจเห็นในรายการแต่เลือกแถวไม่ได้ แสดงเฉพาะรันที่ calc_dt อยู่ในช่วง [รันด้วยมือ] ต้องมีช่วงเวลา ใช้กฎรอบเดียวกันแต่ตัด AUTO ฝั่งเซิร์ฟเวอร์ ค้นหาแบ่งเพียงทั้งหมด/ด้วยมือ ข้อจำกัดเหมือน batch ยอดไม่พอลงลูกหนี้อัตโนมัติและ FIFO เหมือนเดิม'
      )
    ],
    '/settlement/settlementResultDistribute': [
      packN(
        '정산배포: PENDING 만 표시. 과거 DB가 V101 백필로 전부 DISTRIBUTED였다면 운영 DB에 db/V111_settlement_publish_pending_reopen.sql 적용 후 목록이 채워집니다. 처음 열 때 정산기간이 비어 있으면 최근 1년입니다. 행 클릭 시 정산일 당일 해당 가맹 전체 거래를 아래에 표시합니다. 체크 후 배포실행 → DISTRIBUTED, 홀딩실행 → HOLD.',
        'Settlement distribution: shows PENDING only. If an older DB was fully backfilled to DISTRIBUTED (V101), run db/V111_settlement_publish_pending_reopen.sql on the production DB to repopulate this list. When opened with an empty period, the default is the last year. Click a row to load that merchant’s payments for the settlement calendar day. After selecting rows: Deploy run → DISTRIBUTED; Hold run → HOLD.',
        '精算配布: PENDINGのみ表示。過去DBがV101バックフィルで全件DISTRIBUTEDの場合は、本番DBにdb/V111_settlement_publish_pending_reopen.sqlを適用すると一覧が埋まります。精算期間が空のまま開いたときは直近1年です。行をクリックすると精算日当日の当該加盟店の全決済取引を下に表示します。チェック後: 配布実行→DISTRIBUTED、ホールド実行→HOLD。',
        '结算下发：仅显示 PENDING。若历史库经 V101 回填全部为 DISTRIBUTED，请在生产库执行 db/V111_settlement_publish_pending_reopen.sql 后列表才会出现数据。首次打开若精算期间为空，默认为最近一年。单击行可在下方加载该商户精算日当天全部支付。勾选后：下发执行→DISTRIBUTED；暂缓执行→HOLD。',
        'แจกจ่ายผลชำระ: แสดงเฉพาะ PENDING หาก DB เก่าถูก backfill เป็น DISTRIBUTED ทั้งหมด (V101) ให้รัน db/V111_settlement_publish_pending_reopen.sql บน DB จริงแล้วรายการจะกลับมา เมื่อเปิดโดยช่วงว่าง ค่าเริ่มต้นคือ 1 ปีล่าสุด คลิกแถวเพื่อโหลดการชำระทั้งหมดของร้านในวันปฏิทินของวันชำระ หลังเลือก: รันแจกจ่าย→DISTRIBUTED; รันพัก→HOLD'
      )
    ],
    '/settlement/settlementResultHold': [
      packN(
        '정산대기: HOLD — 가맹 정산내역에 안 나감. 처음 열 때 정산기간이 비어 있으면 최근 1년입니다. 해제·배포는 「정산보류내역」 등 운영 절차. 노출 주기 요약은 정산배포와 같습니다.',
        'Settlement pending: HOLD — not posted to merchant settlement statements. When opened with an empty settlement period, the default is the last year. Release and distribution follow operational procedures such as Settlement hold list. Cadence display summary matches Settlement distribution.',
        '精算待ち: HOLD — 加盟店精算一覧には載りません。精算期間が空のまま開いたときは直近1年が既定です。解除・配布は「精算保留一覧」などの運用手順に従ってください。表示サイクル要約は精算配布と同じです。',
        '结算待处理：HOLD — 不会出现在商户结算明细中。首次打开若精算期间为空，默认为最近一年。解除与下发请按「结算暂缓明细」等运营流程。展示周期说明与结算下发一致。',
        'รอชำระ: HOLD — ไม่ขึ้นรายการชำระร้านค้า เมื่อเปิดโดยช่วงชำระว่าง ค่าเริ่มต้นคือ 1 ปีล่าสุด การปลดและแจกจ่ายตามขั้นตอนเช่น รายการพักชำระ สรุปรอบแสดงเหมือนหน้าแจกจ่ายผลชำระ'
      )
    ],
    '/calc/paySettlementHoldList': [
      packN(
        '정산방법에서 지급보류가 「보류」인 가맹점은 정산 실행 시 결과가 가맹점정산내역·유통망정산 집계에 나타나지 않고 이 화면에만 적치됩니다. 정산 금액·수수료 등은 이미 계산·저장된 값입니다.',
        'Merchants with payout hold set to “hold” in settlement method do not appear in merchant settlement or distributor totals when a run completes; rows land on this screen only. Amounts and fees are already calculated and stored.',
        '精算方法で支給保留が「保留」の加盟店は、精算実行時に加盟店精算一覧・流通網精算集計へは出ず、この画面にのみ溜まります。精算金額・手数料等は既に計算・保存済みの値です。',
        '结算方式中支付暂缓为「暂缓」的商户，结算执行完成后不会出现在商户结算与分销汇总中，仅堆积在本屏。结算金额与手续费等为已计算并保存的值。',
        'ร้านที่ตั้งค่าพักจ่ายเป็น「พัก」จะไม่ไปรวมในรายการชำระร้าน/เครือข่ายหลังรันชำระ แสดงเฉพาะหน้านี้ ยอดและค่าธรรมเนียมคำนวณและบันทึกแล้ว'
      ),
      packN(
        '「보류해제」열의 [Y→N 해제]로 한 건만 바로 해제하거나, 체크 후 [선택 건 지급보류 해제]로 여러 건을 한 번에 처리할 수 있습니다. 더블 확인 후 실행 행의 지급보류(Y)가 N으로 바뀌며 가맹점정산내역(및 유통 집계)에 반영됩니다. 가맹점 설정의 지급보류는 그대로이며, 이후 신규 정산 건은 다시 이 목록에 쌓일 수 있습니다.',
        'Use [Y→N release] in the Release column for a single row, or check rows and [Release payout hold (selected)] for bulk. After confirmation, payout hold on the run becomes N and posts to merchant settlement (and distributor totals). The merchant profile payout-hold setting is unchanged; new runs may appear here again.',
        '「保留解除」列の［Y→N解除］で1件だけ即解除するか、チェック後［選択実行の支給保留解除］で複数件を一括処理できます。確認後、実行行の支給保留(Y)がNになり加盟店精算一覧（および流通集計）へ反映されます。加盟店設定の支給保留は変わらず、以後の新規精算は再びこの一覧に溜まることがあります。',
        '「解除暂缓」列用【Y→N 解除】逐条解除，或勾选后【解除所选记录的支付暂缓】批量处理。确认后该执行行的支付暂缓变为 N 并写入商户结算（及分销汇总）。商户配置的支付暂缓不变，后续新结算仍可能再进入本列表。',
        'คอลัมน์ปลด: กด [Y→N] ทีละแถว หรือเลือกหลายแถวแล้ว [ปลดพักจ่ายที่เลือก] หลังยืนยัน Y เป็น N และสะท้อนในรายการชำระร้าน การตั้งค่าพักจ่ายของร้านไม่เปลี่ยน รันใหม่อาจกลับมาที่นี่'
      ),
      packN(
        '결제 건별 롤링 예치(담보)는 「담보금내역」(/calc/collateralList)에서 확인하세요.',
        'Per-transaction rolling collateral is shown under Collateral list (/calc/collateralList).',
        '取引別ロール預り（担保）は「担保金一覧」(/calc/collateralList)で確認してください。',
        '按笔滚动保证金请在「保证金记录」(/calc/collateralList)查看。',
        'หลักประกันหมุนเวียนต่อรายการดูที่รายการหลักประกัน (/calc/collateralList)'
      ),
      packN(
        '표시 열은 [헬로] 옆 VIEW SETTING에서 조정할 수 있습니다(저장 시 사용자별 유지). 체크·보류해제 열은 항상 표시됩니다.',
        'Toggle visible columns via VIEW SETTING next to Hello (saved per user). Checkbox and Release columns always stay visible.',
        '表示列は［Hello］横の VIEW SETTING で調整できます（保存時ユーザー別に維持）。チェック・保留解除列は常に表示されます。',
        '可通过 Hello 旁的 VIEW SETTING 调整显示列（按用户保存）。勾选与解除暂缓列始终显示。',
        'สลับคอลัมน์ที่มองเห็นผ่าน VIEW SETTING ข้าง Hello (บันทึกต่อผู้ใช้) คอลัมน์เลือกและปลดพักแสดงเสมอ'
      )
    ],
    '/settlement/paySettlementHoldList': [
      packN(
        '정산방법에서 지급보류가 「보류」인 가맹점은 정산 실행 시 결과가 가맹점정산내역·유통망정산 집계에 나타나지 않고 이 화면에만 적치됩니다. 정산 금액·수수료 등은 이미 계산·저장된 값입니다.',
        'Merchants with payout hold set to “hold” in settlement method do not appear in merchant settlement or distributor totals when a run completes; rows land on this screen only. Amounts and fees are already calculated and stored.',
        '精算方法で支給保留が「保留」の加盟店は、精算実行時に加盟店精算一覧・流通網精算集計へは出ず、この画面にのみ溜まります。精算金額・手数料等は既に計算・保存済みの値です。',
        '结算方式中支付暂缓为「暂缓」的商户，结算执行完成后不会出现在商户结算与分销汇总中，仅堆积在本屏。结算金额与手续费等为已计算并保存的值。',
        'ร้านที่ตั้งค่าพักจ่ายเป็น「พัก」จะไม่ไปรวมในรายการชำระร้าน/เครือข่ายหลังรันชำระ แสดงเฉพาะหน้านี้ ยอดและค่าธรรมเนียมคำนวณและบันทึกแล้ว'
      ),
      packN(
        '「보류해제」열의 [Y→N 해제]로 한 건만 바로 해제하거나, 체크 후 [선택 건 지급보류 해제]로 여러 건을 한 번에 처리할 수 있습니다. 더블 확인 후 실행 행의 지급보류(Y)가 N으로 바뀌며 가맹점정산내역(및 유통 집계)에 반영됩니다. 가맹점 설정의 지급보류는 그대로이며, 이후 신규 정산 건은 다시 이 목록에 쌓일 수 있습니다.',
        'Use [Y→N release] in the Release column for a single row, or check rows and [Release payout hold (selected)] for bulk. After confirmation, payout hold on the run becomes N and posts to merchant settlement (and distributor totals). The merchant profile payout-hold setting is unchanged; new runs may appear here again.',
        '「保留解除」列の［Y→N解除］で1件だけ即解除するか、チェック後［選択実行の支給保留解除］で複数件を一括処理できます。確認後、実行行の支給保留(Y)がNになり加盟店精算一覧（および流通集計）へ反映されます。加盟店設定の支給保留は変わらず、以後の新規精算は再びこの一覧に溜まることがあります。',
        '「解除暂缓」列用【Y→N 解除】逐条解除，或勾选后【解除所选记录的支付暂缓】批量处理。确认后该执行行的支付暂缓变为 N 并写入商户结算（及分销汇总）。商户配置的支付暂缓不变，后续新结算仍可能再进入本列表。',
        'คอลัมน์ปลด: กด [Y→N] ทีละแถว หรือเลือกหลายแถวแล้ว [ปลดพักจ่ายที่เลือก] หลังยืนยัน Y เป็น N และสะท้อนในรายการชำระร้าน การตั้งค่าพักจ่ายของร้านไม่เปลี่ยน รันใหม่อาจกลับมาที่นี่'
      ),
      packN(
        '결제 건별 롤링 예치(담보)는 「담보금내역」(/calc/collateralList)에서 확인하세요.',
        'Per-transaction rolling collateral is shown under Collateral list (/calc/collateralList).',
        '取引別ロール預り（担保）は「担保金一覧」(/calc/collateralList)で確認してください。',
        '按笔滚动保证金请在「保证金记录」(/calc/collateralList)查看。',
        'หลักประกันหมุนเวียนต่อรายการดูที่รายการหลักประกัน (/calc/collateralList)'
      ),
      packN(
        '표시 열은 [헬로] 옆 VIEW SETTING에서 조정할 수 있습니다(저장 시 사용자별 유지). 체크·보류해제 열은 항상 표시됩니다.',
        'Toggle visible columns via VIEW SETTING next to Hello (saved per user). Checkbox and Release columns always stay visible.',
        '表示列は［Hello］横の VIEW SETTING で調整できます（保存時ユーザー別に維持）。チェック・保留解除列は常に表示されます。',
        '可通过 Hello 旁的 VIEW SETTING 调整显示列（按用户保存）。勾选与解除暂缓列始终显示。',
        'สลับคอลัมน์ที่มองเห็นผ่าน VIEW SETTING ข้าง Hello (บันทึกต่อผู้ใช้) คอลัมน์เลือกและปลดพักแสดงเสมอ'
      )
    ],
    '/calc/calcGmList': [
      packN(
        '한 행은 정산 실행으로 저장된 귀사(가맹) 정산 결과입니다. 정산기간·빠른기간으로 조회한 뒤 [검색] 하세요.',
        'Each row is a settlement run result saved for your merchant. Set the settlement period or quick range, then click [Search].',
        '1行は精算実行として保存された貴社（加盟店）の精算結果です。精算期間・クイック期間を指定してから［検索］してください。',
        '每行为您（商户）已保存的结算执行结果。请设定结算期间或快捷区间后点击【搜索】。',
        'แต่ละแถวคือผลชำระที่บันทึกจากรันชำระ ตั้งช่วงชำระหรือช่วงด่วนแล้วกด [ค้นหา]'
      ),
      packN(
        '정산대상기간·결제금액·수수료·보증금·정산료·VAT·지급액은 정산배포·정산실행과 동일한 실행 저장값·집계 규칙을 따릅니다. 수수료 열은 건당·결제%·취소·환불(무효 등) 구간을 합산한 거래수수료(tb_settlement_run.total_fee)입니다.',
        'Target period, payment amount, fees, collateral, settlement fee, VAT, and payout follow the same stored run values and aggregation rules as settlement publish and runs. The fee column is total transaction fees (tb_settlement_run.total_fee) summing per-txn, pay %, cancel, refund (void, etc.) buckets.',
        '精算対象期間・決済金額・手数료・担保金・精算料・VAT・支払額は、精算配布・精算実行と同じ実行保存値・集計ルールに従います。手数料列は件当・決済%・取消・返金（無効等）区間を合算した取引手数料（tb_settlement_run.total_fee）です。',
        '结算目标期间、支付金额、手续费、保证金、结算费、VAT、拨付额与结算下发、结算执行采用相同的执行保存值与汇总规则。手续费列为按笔、支付%、取消、退款（作废等）区间汇总的交易手续费（tb_settlement_run.total_fee）。',
        'ช่วงเป้าหมาย ยอดชำระ ค่าธรรมเนียม หลักประกัน ค่าธรรมเนียมชำระ VAT และยอดจ่ายใช้กฎเดียวกับแจกจ่าย/รันชำระ คอลัมน์ค่าธรรมเนียมคือ total_fee รวมต่อรายการ % ยกเลิก คืน โมฆะ ฯลฯ'
      ),
      packN(
        '표시 열은 [헬로] 옆 VIEW SETTING에서 조정할 수 있습니다(저장 시 사용자별 유지). 건당·취소·환불 등 세부 분해 열은 같은 거래 구간 합산 보조값입니다.',
        'Toggle visible columns via VIEW SETTING next to Hello (saved per user). Per-txn, cancel, refund, etc. breakdown columns are auxiliary sums for the same transaction window.',
        '表示列は［Hello］横の VIEW SETTING で調整できます（保存時ユーザー別に維持）。件当・取消・返金等の内訳列は同一取引区間の合算補助値です。',
        '可通过 Hello 旁的 VIEW SETTING 调整显示列（按用户保存）。按笔、取消、退款等明细列为同一交易区间的汇总辅助值。',
        'สลับคอลัมน์ที่มองเห็นผ่าน VIEW SETTING ข้าง Hello (บันทึกต่อผู้ใช้) คอลัมน์แยกต่อรายการ ยกเลิก คืน ฯลฯ เป็นยอดรวมเสริมในช่วงธุรกรรมเดียวกัน'
      )
    ],
    '/settlement/franchiseList': [
      packN(
        '한 행은 정산 실행으로 저장된 귀사(가맹) 정산 결과입니다. 정산기간·빠른기간으로 조회한 뒤 [검색] 하세요.',
        'Each row is a settlement run result saved for your merchant. Set the settlement period or quick range, then click [Search].',
        '1行は精算実行として保存された貴社（加盟店）の精算結果です。精算期間・クイック期間を指定してから［検索］してください。',
        '每行为您（商户）已保存的结算执行结果。请设定结算期间或快捷区间后点击【搜索】。',
        'แต่ละแถวคือผลชำระที่บันทึกจากรันชำระ ตั้งช่วงชำระหรือช่วงด่วนแล้วกด [ค้นหา]'
      ),
      packN(
        '정산대상기간·결제금액·수수료·보증금·정산료·VAT·지급액은 정산배포·정산실행과 동일한 실행 저장값·집계 규칙을 따릅니다. 수수료 열은 건당·결제%·취소·환불(무효 등) 구간을 합산한 거래수수료(tb_settlement_run.total_fee)입니다.',
        'Target period, payment amount, fees, collateral, settlement fee, VAT, and payout follow the same stored run values and aggregation rules as settlement publish and runs. The fee column is total transaction fees (tb_settlement_run.total_fee) summing per-txn, pay %, cancel, refund (void, etc.) buckets.',
        '精算対象期間・決済金額・手数료・担保金・精算料・VAT・支払額は、精算配布・精算実行と同じ実行保存値・集計ルールに従います。手数料列は件当・決済%・取消・返金（無効等）区間を合算した取引手数料（tb_settlement_run.total_fee）です。',
        '结算目标期间、支付金额、手续费、保证金、结算费、VAT、拨付额与结算下发、结算执行采用相同的执行保存值与汇总规则。手续费列为按笔、支付%、取消、退款（作废等）区间汇总的交易手续费（tb_settlement_run.total_fee）。',
        'ช่วงเป้าหมาย ยอดชำระ ค่าธรรมเนียม หลักประกัน ค่าธรรมเนียมชำระ VAT และยอดจ่ายใช้กฎเดียวกับแจกจ่าย/รันชำระ คอลัมน์ค่าธรรมเนียมคือ total_fee รวมต่อรายการ % ยกเลิก คืน โมฆะ ฯลฯ'
      ),
      packN(
        '표시 열은 [헬로] 옆 VIEW SETTING에서 조정할 수 있습니다(저장 시 사용자별 유지). 건당·취소·환불 등 세부 분해 열은 같은 거래 구간 합산 보조값입니다.',
        'Toggle visible columns via VIEW SETTING next to Hello (saved per user). Per-txn, cancel, refund, etc. breakdown columns are auxiliary sums for the same transaction window.',
        '表示列は［Hello］横の VIEW SETTING で調整できます（保存時ユーザー別に維持）。件当・取消・返金等の内訳列は同一取引区間の合算補助値です。',
        '可通过 Hello 旁的 VIEW SETTING 调整显示列（按用户保存）。按笔、取消、退款等明细列为同一交易区间的汇总辅助值。',
        'สลับคอลัมน์ที่มองเห็นผ่าน VIEW SETTING ข้าง Hello (บันทึกต่อผู้ใช้) คอลัมน์แยกต่อรายการ ยกเลิก คืน ฯลฯ เป็นยอดรวมเสริมในช่วงธุรกรรมเดียวกัน'
      )
    ],
    '/calc/dailyFee': [
      packN(
        '상단·일자 더블클릭 하단 「선택 일자 상세」: 건수 옆에 총거래·승인·취소·수수료·담보·부가세·지급예상·추정결산을 수수료내역과 동일 산식으로 표시합니다.',
        'Top bar and bottom “Selected date detail” (double-click a day): same formulas as fee history — total txn, approve, cancel, fees, collateral, VAT, expected payout, est. settlement.',
        '上段・日付ダブルクリック下段「選択日詳細」: 手数料明細と同一式で総取引・承認・取消・手数料・担保・消費税・支払予定・推定決算を表示。',
        '顶部与双击日期下方「选择日期详情」：与手续费明细相同公式显示总交易、批准、取消、手续费、担保、增值税、预计拨付、预估结算。',
        'แถบบนและรายละเอียดวันที่ (ดับเบิลคลิก): สูตรเดียวกับประวัติค่าธรรมเนียม'
      ),
      packN(
        '수수료내역과 동일 산식·동일 필터로 일자별 합계를 표시합니다(건당·결제%·USDT·FX·3DS·실패·취소·무효·환불·차지백·총수수료·부가세·지급예상·정산액 등). 정산유무는 해당 일 거래의 settled_yn 이 전부 Y이면 정산완료, 전부 N이면 정산대기, 혼합이면 부분정산입니다.',
        'Daily totals use the same formulas and filters as fee history (per-txn, %, USDT, FX, 3DS, fail/cancel/void/refund/chargeback, total fee, VAT, expected payout, settlement amount, etc.). Settlement state for a day is “completed” if all rows are settled=Y, “pending” if all N, or “partial” if mixed.',
        '手数料明細と同一の計算式・フィルタで日別合計を表示します。精算有無は当日の settled_yn がすべて Y なら精算完了、すべて N なら待ち、混在なら一部精算です。',
        '与手续费明细相同公式与筛选，按日显示合计。若该日交易 settled_yn 全为 Y 为已结算，全 N 为待结算，混合为部分结算。',
        'สรุปรายวันตามสูตรเดียวกับประวัติค่าธรรมเนียม สถานะชำระจาก settled_yn ของวันนั้น'
      ),
      packN(
        '첫 화면은 집계 부하·게이트웨이 시간 초과(504)를 줄이기 위해 최근 7일(당일 포함)만 자동 조회합니다. 당월·당일 등은 빠른기간 버튼 뒤 [검색]으로 넓히면 됩니다.',
        'The first load auto-queries the last 7 days (including today) to reduce load and 504 risk. Use quick-range then [Search] to widen (e.g. this month).',
        '初回は負荷と504回避のため直近7日（当日含む）のみ自動照会します。当月などはクイック期間のあと[検索]で広げます。',
        '首次进入为降低聚合负载与504风险，自动只查最近7天（含当天）。需要当月等请用快捷日期后点「搜索」扩大。',
        'โหลดครั้งแรก 7 วันล่าสุดเพื่อลดโหลด ขยายช่วงด้วยปุ่มช่วงแล้วกดค้นหา'
      ),
      packN(
        '일자 행을 더블클릭하면 아래에 해당 일 수수료내역 전체가 표시됩니다(수수료내역 화면과 동일 열 구성). 조회 기간은 최대 93일입니다. 데이터가 매우 많으면 상단 집계가 일부만 반영될 수 있습니다(meta.capped).',
        'Double-click a date row to show all fee rows for that day (same columns as fee history). Query window up to 93 days. Very large data may cap the top aggregate (see meta.capped).',
        '日付行をダブルクリックすると当日の手数料明細をすべて表示します（手数料明細画面と同じ列）。照会は最大93日。データが極端に多いと上部集計が一部のみになることがあります（meta.capped）。',
        '双击日期行在下方显示该日全部手续费明细（与手续费明细列一致）。查询最长93天；数据量极大时顶部汇总可能部分反映（meta.capped）。',
        'ดับเบิลคลิกวันที่เพื่อดูค่าธรรมเนียมทั้งหมดของวันนั้น ช่วงสูงสุด 93 วัน อาจมี meta.capped'
      ),
      packN(
        '미래 일자는 표시되지 않습니다(전산 표시 기준일). 일자 순서는 [내림차순](최신일 위)·[오름차순]으로 바꿀 수 있으며 기본은 내림차순입니다.',
        'Future dates are not shown (system as-of). Toggle Desc/Asc with the toolbar next to [Refresh]; default is newest first.',
        '未来日は表示しません。並び順は[再読込]横の降順・昇順で切替、既定は降順です。',
        '不显示未来日期。顺序可用「刷新」旁降序/升序切换，默认最新在上。',
        'ไม่แสดงวันในอนาคต เรียงใหม่สุดก่อนเป็นค่าเริ่มต้น'
      )
    ],
    '/ops/verifyReport': [
      packN(
        'ChillPay 통합내역(API, 거래일 TransactionDate)을 기준으로 결제내역 NOTI(origin=NOTI)와 승인번호(TransactionId)·결제액·상태를 대조합니다.',
        'Reconciles ChillPay integrated rows (API, txn date TransactionDate) against payment history NOTI (origin=NOTI) by approval no. (TransactionId), amount, and status.',
        'ChillPay統合履歴（API・取引日TransactionDate）を基準に、決済履歴NOTI（origin=NOTI）と承認番号（TransactionId）・決済額・状態を照合します。',
        '以 ChillPay 综合记录（API，交易日 TransactionDate）为基准，对照支付历史 NOTI（origin=NOTI）的授权号（TransactionId）、支付额与状态。',
        'เทียบ ChillPay รวม (API, TransactionDate) กับ NOTI ชำระ (origin=NOTI) ด้วยเลขอนุมัติ ยอด และสถานะ'
      ),
      packN(
        '일치 건은 하단 「선택 일자 불일치」 목록에서 제외됩니다. JPAY 등 다른 PG·URL/챗봇만 있는 건은 대상이 아니며, 통합에 없고 결제에만 있는 건은 오류로 표시하지 않습니다.',
        'Matched rows are omitted from the bottom 「Selected date — mismatches」 list. JPAY-only or URL/chatbot-only rows are out of scope; payment-only rows not in integrated data are not flagged as errors.',
        '一致件は下部「選択日の不一致」から除外します。JPAY等のみ・URL/チャットボットのみは対象外。統合に無く決済のみの件はエラーにしません。',
        '一致记录在下方「所选日期不一致」中排除。仅 JPAY、仅 URL/聊天机器人不在范围；综合无而支付有的记录不标为错误。',
        'รายการที่ตรงกันจะไม่อยู่ในรายการไม่ตรงกันด้านล่าง JPAY/URL/แชทบอทเท่านั้นไม่รวม มีแค่ชำระไม่มีรวมไม่ถือเป็นข้อผิดพลาด'
      ),
      packN(
        '일자 행을 더블클릭하면 해당 일의 불일치 건만 표시합니다(승인번호로 통합내역·결제내역에서 추적). 결제시간·금액 표기 형식 차이는 불일치로 잡지 않습니다.',
        'Double-click a date row to show mismatches for that day only (trace by approval no. in integrated vs payment). Time or amount formatting differences are not treated as mismatches.',
        '日付行をダブルクリックすると当日の不一致のみ表示（承認番号で統合・決済を追跡）。決済時刻・金額表記の差は不一致にしません。',
        '双击日期行仅显示该日不一致（按授权号在综合与支付中追踪）。支付时间或金额格式差异不算不一致。',
        'ดับเบิลคลิกวันที่แสดงเฉพาะรายการไม่ตรงกันของวันนั้น (ตามเลขอนุมัติ) ความต่างรูปแบบเวลา/ยอดไม่นับ'
      ),
      packN(
        '총본사·본사(REGIONAL)·총판(MASTER_DIST) 또는 ADMIN만 사용합니다. 조회 기간은 최대 93일입니다.',
        'Only root HQ, regional HQ (REGIONAL), master distributor (MASTER_DIST), or ADMIN. Query window up to 93 days.',
        '総本部・本社(REGIONAL)・総販(MASTER_DIST) または ADMIN のみ。照会は最大93日。',
        '仅总总部、本部(REGIONAL)、总代(MASTER_DIST) 或 ADMIN。查询最长 93 天。',
        'เฉพาะ HQ สูงสุด/ภูมิภาค/ตัวแทนหลักหรือ ADMIN ช่วงสูงสุด 93 วัน'
      )
    ],
    '/ops/integratedReport': [
      packN(
        '총본사·본사(REGIONAL)·총판(MASTER_DIST) 또는 ADMIN만 사용합니다. 조회 범위는 로그인 조직의 하위 가맹 거래입니다.',
        'Only root HQ, regional HQ (REGIONAL), master distributor (MASTER_DIST), or ADMIN. Scope is merchants under the logged-in org tree.',
        '総本部・本社(REGIONAL)・総販(MASTER_DIST) または ADMIN のみ。範囲はログイン組織配下の加盟店取引です。',
        '仅总总部、本部(REGIONAL)、总代(MASTER_DIST)或 ADMIN。范围为登录组织下属商户交易。',
        'เฉพาะ HQ สูงสุด/ภูมิภาค/ตัวแทนหลักหรือ ADMIN ขอบเขตคือร้านใต้องค์กร'
      ),
      packN(
        '집계 기준일은 거래 적재일(created_at)이며, 일별결제와 동일합니다. 상단 요약·상태별 금액은 조직 기준 표시 통화로 합산합니다.',
        'Aggregation is by transaction ingest date (created_at), same as daily payment. Top summary and bucket amounts are summed in the org display currency.',
        '集計基準日は取引取込日（created_at）で、日別決済と同じです。上部要約・状態別金額は組織表示通貨で合算します。',
        '汇总口径为交易入库日（created_at），与按日支付一致。顶部摘要与状态金额按组织展示币种汇总。',
        'รวมตามวันที่บันทึก (created_at) เหมือนรายวัน สรุปด้านบนเป็นสกุลแสดงขององค์กร'
      ),
      packN(
        '일자 행을 더블클릭하면 아래 「선택 일자 상세 (통합 결제내역)」에 해당 일의 통합 결제내역(동일 필터)과 총거래·승인·취소·수수료·담보·부가세·추정결산이 표시됩니다. 행 순서는 적재일(일자) 기준이며, [새로고침] 옆 「내림차순·오름차순」으로 최신일 우선/과거일 우선을 바꿀 수 있습니다(통합 결제내역과 동일).',
        'Double-click a date row to load integrated payment rows for that day (same filters) with total txn, approve, cancel, fees, collateral, VAT, and est. settlement in the detail toolbar. Row order is by ingest date; use Desc/Asc next to [Refresh] like integrated payment list.',
        '日付行をダブルクリックすると「選択日詳細（統合決済履歴）」に当日の統合決済と総取引・承認・取消・手数料・担保・消費税・推定決算を表示します。並びは取込日基準で、[再読込]横の降順・昇順で切替できます。',
        '双击日期行在下方「选择日期详情（综合支付）」加载该日综合支付及总交易、批准、取消、手续费、担保、增值税、预估结算。行序按入库日；在「刷新」侧切换升降序，与综合支付列表一致。',
        'ดับเบิลคลิกวันที่เพื่อโหลดรายการชำระรวมของวันนั้นพร้อมสรุปยอด เรียงตามวันที่บันทึก'
      ),
      packN(
        '[엑셀다운로드]는 결제내역과 동일한 상단 메뉴 형태이며, 현재 조회된 일자별 통합 리포트 표를 서식 xlsx로 받습니다.',
        '[Excel download] uses the same top action pattern as payment history and exports the currently loaded daily integrated report as styled xlsx.',
        '[Excelダウンロード]は決済履歴と同じ上部操作で、現在表示中の日別統合レポートを書式付きxlsxで取得します。',
        '「Excel 下载」与支付历史相同顶部操作，将当前查询到的按日综合报表导出为带格式 xlsx。',
        'ดาวน์โหลด Excel แบบเดียวกับรายการชำระ ส่งออกตารางรายงานรวมรายวันที่โหลดอยู่'
      ),
      packN(
        '요약 바: 검색 기간 전체 거래 건수(건수)와 통화별 총결제액(승인−취소)·총수수료(부가세 제외)·총보증금(담보 추정)·예상지급액을 결제내역 상단과 같은 형식으로 표시합니다.',
        'Summary bar: for the search range, shows total txn count and per-currency total payment (approve−cancel), total fee ex-VAT, total deposit (collateral estimate), and expected payout—same layout as payment history.',
        '要約バー: 検索期間全体の件数と、通貨別の総決済額（承認−取消）・総手数料（税抜）・総保証金（担保見込み）・見込み支払額を決済履歴上部と同形式で表示します。',
        '摘要栏：在搜索区间内显示总笔数及分币种总支付（批准−取消）、总手续费（不含增值税）、总保证金（担保估计）、预计拨付，版式与支付历史顶部一致。',
        'แถบสรุป: จำนวนรายการและยอดรวมตามสกุล ค่าธรรมเนียมก่อน VAT เงินประกัน ยอดจ่ายโดยประมาณ เหมือนหน้าประวัติชำระ'
      )
    ],
    '/commission/commisionList': [
      packN(
        'VIEW SETTING 열 목록은 본사설정 → 조직항목설정(화면: 수수료관리)에서 허용한 키와 동일합니다. 신규 열 「통화(policyCur)」는 적용 수수료 정책의 통화코드(ISO 숫자·알파)를 THB·JPY 등 알파로 표시합니다. 조직항목설정을 바꾼 뒤 새로고침·재조회하면 체크 목록·노출 제한이 반영됩니다.',
        'VIEW SETTING columns match keys allowed in HQ settings → Org columns (screen: Commission management). The new 「Currency (policyCur)」 column shows the applied policy currency code (ISO numeric/alpha) as THB, JPY, etc. After changing org columns, refresh and search again to update the checklist and visibility rules.',
        'VIEW SETTING の列一覧は、本社設定 → 組織項目設定（画面：手数料管理）で許可したキーと同じです。新列「通貨(policyCur)」は適用手数料ポリシーの通貨コード（ISO 数字・アルファ）を THB・JPY 等のアルファで表示します。組織項目設定を変更した後は再読込・再検索でチェック一覧と表示制限が反映されます。',
        'VIEW SETTING 列与「总部设置 → 组织字段」（手续费管理）允许的键一致。新列「货币(policyCur)」将适用手续费政策的货币代码显示为 THB、JPY 等。修改组织字段后请刷新并重新查询以更新勾选与可见性。',
        'คอลัมน์ VIEW SETTING ตรงกับที่อนุญาตใน ตั้งค่า HQ → คอลัมน์องค์กร (หน้าจัดการค่าธรรมเนียม) คอลัมน์ใหม่ policyCur แสดงรหัสสกุลเงินเป็น THB/JPY ฯลฯ หลังแก้ให้รีเฟรชและค้นหาใหม่'
      ),
      packN(
        '적용시작일을 비우면 저장 시점(서버 시각) 기준으로 적용됩니다.',
        'If the effective start date is blank, it is applied as of the save time (server clock).',
        '適用開始日を空にすると、保存時点（サーバー時刻）基準で適用されます。',
        '若留空生效开始日，则按保存时刻（服务器时间）生效。',
        'ว่างวันที่เริ่มใช้จะถือเวลาบันทึก (เซิร์ฟเวอร์)'
      ),
      packN(
        '동일 가맹점에 미래 적용일이 중복되지 않도록 한 번에 한 건만 등록하는 것을 권장합니다.',
        'To avoid overlapping future effective dates for the same merchant, register one row at a time.',
        '同一加盟店で将来の適用日が重複しないよう、一度に1件だけ登録することを推奨します。',
        '为避免同一商户未来生效日重叠，建议每次只登记一行。',
        'แนะนำลงทะเบียนทีละแถวเพื่อไม่ให้วันที่ซ้ำในร้านเดียวกัน'
      ),
      packN(
        '상위 조직 수수료 정책이 바뀌면 이후 신규 가맹점 등록 시 하위 배분 설정에 반영될 수 있습니다.',
        'If an upstream org fee policy changes, new merchant registrations may inherit updated downstream splits.',
        '上位組織の手数料ポリシーが変わると、以降の新規加盟店登録時に下位の配分設定へ反映される場合があります。',
        '若上级组织手续费政策变更，后续新注册商户的分成设置可能会随之变化。',
        'หากนโยบายค่าธรรมเนียมขององค์กรระดับบนเปลี่ยน การลงทะเบียนร้านใหม่ภายหลังอาจสะท้อนการแบ่งส่วนล่างสุด'
      )
    ],
    '/comp/compMngTree': [
      packN(
        '기본 조회는 업체사용상태가 사용인 업체만 표시합니다. 미사용·전체는 셀렉트에서 선택하세요. 조직별 화면 권한(옵저버·수정 등)은 사용/미사용과 관계없이 동일하게 적용됩니다. 미사용으로 바꾼 조직은 결제·정산·노티가 중단되며, 사용으로 되돌리면 복구됩니다. 상위를 미사용으로 두면 하위 프로필도 함께 미사용 처리됩니다.',
        'By default only companies marked in use are listed. Pick inactive or all in the filter. Screen permissions (observer, edit, etc.) apply regardless of use flag. Disabling an org stops pay, settlement, and notify; re-enabling restores. Disabling a parent disables descendant profiles.',
        '既定では使用中の加盟店のみ表示します。未使用・すべてはセレクトで選択してください。画面権限は使用状態に依存しません。未使用にすると決済・精算・ノティが停止し、使用に戻すと復旧します。上位を未使用にすると下位も未使用になります。',
        '默认仅显示「使用中」的商户；在筛选器选择未使用或全部。界面权限与使用标志无关。停用组织会停止支付、结算与通知；恢复使用即可恢复。上级停用则下级档案一并停用。',
        'ค่าเริ่มต้นแสดงเฉพาะร้านที่สถานะใช้งาน เลือกไม่ใช้หรือทั้งหมดในตัวกรอง สิทธิ์หน้าจอไม่ขึ้นกับสถานะ ปิดใช้งานจะหยุดการชำระ การหักบัญชี และแจ้งเตือน เปิดกลับได้ ปิดระดับบนจะปิดโปรไฟล์ลูกด้วย'
      ),
      packN(
        '엑셀등록: [SAMPLE]으로 서식 있는 xlsx(헤더 색·표선·가운데 정렬)를 받아 예시 행을 수정·추가한 뒤 [엑셀등록]에 업로드하세요.',
        'Excel register: download [SAMPLE] styled xlsx (header color, borders, center align), edit sample rows, then upload via [Excel register].',
        'Excel登録: [SAMPLE]の書式付きxlsxを取得し、例示行を編集・追加してから[Excel登録]へアップロードしてください。',
        'Excel 导入：下载带格式的 [SAMPLE] xlsx（表头颜色、边框、居中），修改示例行后通过「Excel 导入」上传。',
        'นำเข้า Excel: ดาวน์โหลด [SAMPLE] แบบมีรูปแบบ แก้ไขแถวตัวอย่าง แล้วอัปโหลดที่ [นำเข้า Excel]'
      )
    ]
  };

  /** 2단 헤더 그룹 라벨 — keys 시그니처로 매칭 */
  var HG = {
    'compRegNo': { KO: '사업자번호', EN: 'Business reg. no.', JP: '事業者番号', CH: '营业执照号', TH: 'เลขทะเบียนธุรกิจ' },
    'pgApproveAmt,payAprv': { KO: 'PG승인', EN: 'PG approval', JP: 'PG承認', CH: 'PG授权', TH: 'อนุมัติ PG' },
    'holdAmt,holdDttm': { KO: '보류', EN: 'Hold', JP: '保留', CH: '暂扣', TH: 'พักรอ' },
    'feeCnt,feeRate': { KO: '수수료', EN: 'Fee', JP: '手数料', CH: '手续费', TH: 'ค่าธรรมเนียม' },
    'payCustomerIndicator': { KO: '고객표시', EN: 'Shopper label', JP: '顧客表示', CH: '客户显示', TH: 'ป้ายลูกค้า' },
    'displayPaySummary': { KO: '입력통화', EN: 'Input currency', JP: '入力通貨', CH: '输入币种', TH: 'สกุลที่ป้อน' },
    'displayPayCur': { KO: '고객통화', EN: 'Shopper ccy', JP: '顧客通貨', CH: '客户币种', TH: 'สกุลลูกค้า' },
    'displayPayAmt': { KO: '고객금액', EN: 'Shopper amt', JP: '顧客金額', CH: '客户金额', TH: 'ยอดลูกค้า' },
    'trnDate,trnTime,routeNo,chillTransactionId,trnId': { KO: '거래', EN: 'Transaction', JP: '取引', CH: '交易', TH: 'ธุรกรรม' },
    'txnFixedFeesSum,pctFeesSum': { KO: '승인 / 결제수수료(%)', EN: 'Approval / pay fee (%)', JP: '承認／決済手数料(%)', CH: '授权/支付手续费(%)', TH: 'อนุมัติ / ค่าธรรมเนียม (%)' },
    'usdtFee,fxFee,fee3dsFee': { KO: '기타수수료', EN: 'Other fees', JP: 'その他手数料', CH: '其他手续费', TH: 'ค่าธรรมเนียมอื่น' },
    'rollingPctPlain,rollingDays,rollingHoldEst': { KO: '담보(롤링)', EN: 'Collateral (rolling)', JP: '担保(ロール)', CH: '担保(滚动)', TH: 'หลักประกัน (โรล)' },
    'failFee,cancelFee,voidFee,manualVoidFee,refundFee,chargebackFee': { KO: '실패·취소·무효·환불·차지백', EN: 'Fail·cancel·void·refund·CB', JP: '失敗・取消・無効・返金・CB', CH: '失败·取消·作废·退款·拒付', TH: 'ล้มเหลว·ยกเลิก·โมฆะ·คืน·CB' },
    'totalFee,feeVat,expectedPayout,settlementAmt': { KO: '차감·지급', EN: 'Deduction·payout', JP: '控除・支払', CH: '扣减·拨付', TH: 'หัก·จ่าย' }
  };

  /** 열 키별 비한국어(및 명시 KO). 비어 있으면 카탈로그 스냅샷(한국어) 유지 */
  var COL = {
    _chk: { EN: 'Sel', JP: '選択', CH: '选', TH: 'เลือก' },
    rowNo: { EN: 'No.', JP: '番号', CH: '序号', TH: 'ลำดับ' },
    sortOrder: { EN: 'Order', JP: '順序', CH: '顺序', TH: 'ลำดับ' },
    colId: { EN: 'Item ID', JP: '項目ID', CH: '项目 ID', TH: 'รหัสรายการ' },
    colNm: { EN: 'Item name', JP: '項目名', CH: '项目名称', TH: 'ชื่อรายการ' },
    dispYn: { EN: 'Visible', JP: '表示有無', CH: '是否显示', TH: 'แสดงหรือไม่' },
    compNm: { EN: 'Merchant name', JP: '加盟店名', CH: '商户名称', TH: 'ชื่อร้านค้า' },
    compId: { EN: 'Merchant code', JP: '加盟店コード', CH: '商户代码', TH: 'รหัสร้านค้า' },
    trnDate: { EN: 'Txn date', JP: '取引日', CH: '交易日期', TH: 'วันที่ทำรายการ' },
    trnTime: { EN: 'Txn time', JP: '取引時刻', CH: '交易时间', TH: 'เวลาทำรายการ' },
    routeNo: { EN: 'Route', JP: 'ルート', CH: '路由', TH: 'Route' },
    chillTransactionId: { EN: 'Auth / Txn ID', JP: '承認／取引ID', CH: '授权/交易ID', TH: 'รหัสอนุมัติ/ธุรกรรม' },
    trnId: { EN: 'Our txn no.', JP: '自社取引番号', CH: '内部交易号', TH: 'เลขธุรกรรมภายใน' },
    chillCustomer: { EN: 'Customer', JP: '顧客', CH: '客户', TH: 'ลูกค้า' },
    orderNo: { EN: 'Order no.', JP: '注文番号', CH: '订单号', TH: 'เลขคำสั่งซื้อ' },
    paymentChannel: { EN: 'Payment channel', JP: '決済チャネル', CH: '支付渠道', TH: 'ช่องทางชำระเงิน' },
    payCompletedAt: { EN: 'Paid at', JP: '決済日時', CH: '支付完成时间', TH: 'เวลาชำระเงิน' },
    chillAmount: { EN: 'Payment amount', JP: '決済金額', CH: '支付金额', TH: 'ยอดชำระ' },
    icopayAmt: { EN: 'ICOPAY', JP: 'ICOPAY', CH: 'ICOPAY', TH: 'ICOPAY' },
    chillFeeAmt: { EN: 'Fee', JP: '手数料', CH: '手续费', TH: 'ค่าธรรมเนียม' },
    totalAmt: { EN: 'Total', JP: '合計', CH: '合计', TH: 'รวม' },
    currency: { EN: 'Currency', JP: '通貨', CH: '币种', TH: 'สกุลเงิน' },
    payCustomerIndicator: { EN: 'Shopper label', JP: '顧客表示', CH: '客户显示', TH: 'ป้ายลูกค้า' },
    displayPaySummary: { EN: 'Ccy | amount', JP: '通貨｜金額', CH: '币种｜金额', TH: 'สกุล｜ยอด' },
    displayPayCur: { EN: 'Shopper ccy', JP: '顧客通貨', CH: '客户币种', TH: 'สกุลลูกค้า' },
    displayPayAmt: { EN: 'Shopper amt', JP: '顧客金額', CH: '客户金额', TH: 'ยอดลูกค้า' },
    regionalBaseCur: { EN: 'HQ base ccy', JP: '本社基準通貨', CH: '总部基准货币', TH: 'สกุลฐานสำนักงานใหญ่' },
    masterDistBaseCur: { EN: 'Dist. base ccy', JP: '総販基準通貨', CH: '总代基准货币', TH: 'สกุลฐานตัวแทนหลัก' },
    merchantBaseCur: { EN: 'Merchant base ccy', JP: '加盟店基準通貨', CH: '商户基准货币', TH: 'สกุลฐานร้านค้า' },
    chillPaymentStatus: { EN: 'Status', JP: '状態', CH: '状态', TH: 'สถานะ' },
    statusNm: { EN: 'Status', JP: '状態', CH: '状态', TH: 'สถานะ' },
    amount: { EN: 'Payment amount', JP: '決済金額', CH: '支付金额', TH: 'ยอดชำระ' },
    payCur: { EN: 'Pay ccy', JP: '決済通貨', CH: '支付币种', TH: 'สกุลชำระ' },
    policyCur: { EN: 'Policy ccy', JP: '政策通貨', CH: '政策币种', TH: 'สกุลนโยบาย' },
    expectedSettleDate: { EN: 'Est. settle', JP: '精算予定', CH: '预计结算', TH: 'ชำระโดยประมาณ' },
    rollingPctPlain: { EN: 'Coll. %', JP: '担保率(%)', CH: '担保率(%)', TH: 'หลักประกัน (%)' },
    rollingDays: { EN: 'Hold days', JP: '保留日', CH: '暂扣天数', TH: 'วันพัก' },
    vatAppliedYn: { EN: 'VAT applied', JP: 'VAT適用', CH: 'VAT适用', TH: 'VAT' },
    settledYn: { EN: 'Settlement', JP: '精算', CH: '结算', TH: 'การชำระบัญชี' },
    compRegNo: { EN: 'Business no.', JP: '事業者番号', CH: '注册号', TH: 'เลขทะเบียน' },
    payDivNm: { EN: 'Type', JP: '区分', CH: '类型', TH: 'ประเภท' },
    payCard: { EN: 'Card', JP: '決済カード', CH: '支付卡', TH: 'บัตร' },
    cardAprvNo: { EN: 'Card auth no.', JP: 'カード承認番号', CH: '卡授权号', TH: 'เลขอนุมัติบัตร' },
    payCardNo: { EN: 'Card no.', JP: 'カード番号', CH: '卡号', TH: 'เลขบัตร' },
    instalMonth: { EN: 'Installment', JP: '分割回数', CH: '分期月数', TH: 'งวดผ่อน' },
    payMethod: { EN: 'Pay method', JP: '決済手段', CH: '支付方式', TH: 'วิธีชำระ' },
    corpNm: { EN: 'Corporate name', JP: '法人名', CH: '法人名称', TH: 'ชื่อนิติบุคคล' },
    pgNm: { EN: 'PG', JP: 'PG', CH: 'PG', TH: 'PG' },
    terminalId: { EN: 'Terminal', JP: '端末', CH: '终端', TH: 'เทอร์มินัล' },
    calcCycle: { EN: 'Settle cycle', JP: '精算周期', CH: '结算周期', TH: 'รอบชำระ' },
    calcProcType: { EN: 'Settlement class', JP: '精算区分', CH: '结算类别', TH: 'ประเภทการชำระ' },
    pgApproveAmt: { EN: 'Amount', JP: '金額', CH: '金额', TH: 'จำนวนเงิน' },
    payAprv: { EN: 'Time', JP: '日時', CH: '时间', TH: 'เวลา' },
    holdAmt: { EN: 'Amount', JP: '金額', CH: '金额', TH: 'จำนวนเงิน' },
    holdDttm: { EN: 'Time', JP: '日時', CH: '时间', TH: 'เวลา' },
    feeCnt: { EN: 'Cnt', JP: '件', CH: '笔数', TH: 'จำนวน' },
    feeRate: { EN: '%', JP: '%', CH: '%', TH: '%' },
    settleAmt: { EN: 'Payout', JP: '支払額', CH: '拨付金额', TH: 'ยอดจ่าย' },
    calcDt: { EN: 'Payout at', JP: '支払日時', CH: '拨付时间', TH: 'เวลาจ่าย' },
    pgApproveNo: { EN: 'PG approval no.', JP: 'PG承認番号', CH: 'PG授权号', TH: 'เลขอนุมัติ PG' },
    productNm: { EN: 'Product', JP: '購入商品', CH: '商品', TH: 'สินค้า' },
    customerNm: { EN: 'Payer name', JP: '顧客名', CH: '付款人姓名', TH: 'ชื่อผู้ชำระ' },
    customerTel: { EN: 'Payer mobile', JP: '携帯', CH: '付款人手机', TH: 'มือถือผู้ชำระ' },
    regionalNm: { EN: 'Distributor', JP: '総販', CH: '总代', TH: 'ตัวแทนหลัก' },
    masterNm: { EN: 'Branch', JP: '支社', CH: '分公司', TH: 'สาขา' },
    branchNm: { EN: 'Agency', JP: '代理店', CH: '代理', TH: 'ตัวแทน' },
    payActions: { EN: 'Follow-up', JP: '後続対応', CH: '后续处理', TH: 'ดำเนินการต่อ' },
    compDivNm: { EN: 'Org type', JP: '組織区分', CH: '组织类型', TH: 'ประเภทองค์กร' },
    settlementAmt: { EN: 'Settlement', JP: '精算金', CH: '结算款', TH: 'เงินชำระ' },
    receivables: { EN: 'Receivable', JP: '未収金', CH: '应收', TH: 'ลูกหนี้' },
    siteRoot: { EN: 'Root', JP: 'ルート', CH: '根路由', TH: 'รูท' },
    contact: { EN: 'Contact', JP: '連絡先', CH: '联系方式', TH: 'ติดต่อ' },
    bankNm: { EN: 'Bank', JP: '銀行', CH: '银行', TH: 'ธนาคาร' },
    accountNo: { EN: 'Account no.', JP: '口座番号', CH: '账号', TH: 'เลขบัญชี' },
    transferFee: { EN: 'Remit fee', JP: '送金手数料', CH: '汇款手续费', TH: 'ค่าธรรมเนียมโอน' },
    transferType: { EN: 'Transfer type', JP: '振込区分', CH: '转账类型', TH: 'ประเภทโอน' },
    transferCycleHours: { EN: 'Cycle (min)', JP: '周期(分)', CH: '周期(分)', TH: 'รอบ (นาที)' },
    calcExcludeYn: { EN: 'Settle exclude', JP: '精算除外', CH: '排除结算', TH: 'ยกเว้นชำระ' },
    calcExcludeTarget: { EN: 'Exclude target', JP: '除外対象', CH: '排除对象', TH: 'เป้าหมายยกเว้น' },
    calcStartTime: { EN: 'Settle start', JP: '精算開始', CH: '结算开始', TH: 'เริ่มชำระ' },
    payHoldYn: { EN: 'Payout hold', JP: '支払保留', CH: '支付暂缓', TH: 'พักจ่าย' },
    useYn: { EN: 'In use', JP: '使用', CH: '使用状态', TH: 'ใช้งาน' },
    terminalCountTerminal: { EN: 'Terminal (POS)', JP: '端末(端末)', CH: '终端(机具)', TH: 'เทอร์มินัล (POS)' },
    terminalCountWeb: { EN: 'Terminal (web)', JP: '端末(web)', CH: '终端(web)', TH: 'เทอร์มินัล (เว็บ)' },
    chgTarget: { EN: 'Field', JP: '変更対象', CH: '变更项', TH: 'ฟิลด์' },
    chgBefore: { EN: 'Before', JP: '変更前', CH: '变更前', TH: 'ก่อน' },
    chgAfter: { EN: 'After', JP: '変更後', CH: '变更后', TH: 'หลัง' },
    chgDt: { EN: 'Changed at', JP: '変更日時', CH: '变更时间', TH: 'เวลาแก้ไข' },
    riskDiv: { EN: 'Risk type', JP: 'リスク区分', CH: '风险类型', TH: 'ประเภทความเสี่ยง' },
    riskDesc: { EN: 'Description', JP: '内容', CH: '说明', TH: 'รายละเอียด' },
    regDt: { EN: 'Registered', JP: '登録日', CH: '注册日期', TH: 'วันที่ลงทะเบียน' },
    day: { EN: 'Date', JP: '日付', CH: '日期', TH: 'วันที่' },
    totalElements: { EN: 'Total (ChillPay)', JP: '総件数（ChillPay）', CH: '总笔数（ChillPay）', TH: 'รวม (ChillPay)' },
    txnCount: { EN: 'All txns', JP: '全件数', CH: '全部笔数', TH: 'ทุกรายการ' },
    txnFixedFeesSum: { EN: 'Per-txn fee', JP: '件当手数료', CH: '按笔手续费', TH: 'ค่าธรรมเนียมต่อรายการ' },
    pctFeesSum: { EN: 'Pay (%)', JP: '決済(%)', CH: '支付(%)', TH: 'ชำระ (%)' },
    usdtFee: { EN: 'USDT', JP: 'USDT', CH: 'USDT', TH: 'USDT' },
    fxFee: { EN: 'FX', JP: 'FX', CH: 'FX', TH: 'FX' },
    fee3dsFee: { EN: '3DS', JP: '3DS', CH: '3DS', TH: '3DS' },
    rollingHoldEst: { EN: 'Collateral est.', JP: '担保見積額', CH: '担保估计额', TH: 'ประมาณหลักประกัน' },
    failFee: { EN: 'Fail fee', JP: '失敗', CH: '失败', TH: 'ล้มเหลว' },
    cancelFee: { EN: 'Cancel fee', JP: '取消', CH: '取消', TH: 'ยกเลิก' },
    voidFee: { EN: 'Void fee', JP: '無効', CH: '作废', TH: 'โมฆะ' },
    manualVoidFee: { EN: 'Manual void', JP: '手動無効', CH: '手动作废', TH: 'โมฆะด้วยมือ' },
    refundFee: { EN: 'Refund fee', JP: '返金', CH: '退款', TH: 'คืนเงิน' },
    chargebackFee: { EN: 'Chargeback', JP: 'チャージバック', CH: '拒付', TH: 'ชาร์จแบ็ก' },
    totalFee: { EN: 'Total fee', JP: '手数料合計', CH: '手续费合计', TH: 'ค่าธรรมเนียมรวม' },
    feeVat: { EN: 'VAT', JP: '消費税', CH: '增值税', TH: 'VAT' },
    expectedPayout: { EN: 'Expected payout', JP: '支払予定額', CH: '预计拨付', TH: 'ยอดจ่ายโดยประมาณ' },
    settlementAmt: { EN: 'Settlement amt.', JP: '精算額', CH: '结算额', TH: 'ยอดชำระ' },
    settlementStateLabel: { EN: 'Settlement status', JP: '精算有無', CH: '结算状态', TH: 'สถานะการชำระ' },
    note: { EN: 'Note', JP: '備考', CH: '备注', TH: 'หมายเหตุ' },
    chillCount: { EN: 'Integrated (Chill) count', JP: '統合(Chill)件数', CH: '综合(Chill)笔数', TH: 'จำนวนรวม (Chill)' },
    matchedCount: { EN: 'Matched count', JP: '一致件数', CH: '一致笔数', TH: 'จำนวนที่ตรงกัน' },
    mismatchCount: { EN: 'Mismatch count', JP: '不一致件数', CH: '不一致笔数', TH: 'จำนวนไม่ตรงกัน' },
    approvalNo: { EN: 'Auth no.', JP: '承認番号', CH: '授权号', TH: 'เลขอนุมัติ' },
    chillAmt: { EN: 'Integrated pay amt', JP: '統合決済額', CH: '综合支付额', TH: 'ยอดชำระรวม' },
    notiAmt: { EN: 'NOTI pay amt', JP: 'NOTI決済額', CH: 'NOTI 支付额', TH: 'ยอด NOTI' },
    chillStatus: { EN: 'Integrated status', JP: '統合状態', CH: '综合状态', TH: 'สถานะรวม' },
    notiStatus: { EN: 'NOTI status', JP: 'NOTI状態', CH: 'NOTI 状态', TH: 'สถานะ NOTI' },
    reason: { EN: 'Note', JP: '備考', CH: '备注', TH: 'หมายเหตุ' },
    successCount: { EN: 'Success count', JP: '成功件数', CH: '成功笔数', TH: 'จำนวนสำเร็จ' }
  };

  /** 일별통합·일별결제·일별수수료 상태 버킷 열 */
  var STATUS_BUCKET = {
    SUCCESS: { EN: 'Success', JP: '成功', CH: '成功', TH: 'สำเร็จ' },
    FAIL: { EN: 'Fail', JP: '失敗', CH: '失败', TH: 'ล้มเหลว' },
    CANCEL: { EN: 'Cancel', JP: '取消', CH: '取消', TH: 'ยกเลิก' },
    VOID: { EN: 'Void', JP: '無効', CH: '作废', TH: 'โมฆะ' },
    EMAIL_VOID: { KO: '이메일 무효', EN: 'Email void', JP: 'メール無効', CH: '邮件作废', TH: 'โมฆะทางอีเมล' },
    REFUND: { EN: 'Refund', JP: '返金', CH: '退款', TH: 'คืนเงิน' },
    FORCE_REFUND: { EN: 'Force refund', JP: '強制返金', CH: '强制退款', TH: 'บังคับคืนเงิน' },
    OTHER: { EN: 'Other', JP: 'その他', CH: '其他', TH: 'อื่นๆ' }
  };

  var CHILL_TR_COL = Object.assign({}, COL, {
    transactionId: COL.chillTransactionId,
    merchant: { EN: 'Merchant (MID)', JP: 'Merchant(MID)', CH: 'Merchant(MID)', TH: 'Merchant (MID)' },
    customer: COL.chillCustomer,
    amount: { EN: 'Amount', JP: '金額', CH: '金额', TH: 'จำนวนเงิน' },
    refundAmount: { EN: 'Refund amount', JP: '返金額', CH: '退款金额', TH: 'ยอดคืน' },
    fee: COL.chillFeeAmt,
    discount: { EN: 'Discount', JP: '値引', CH: '折扣', TH: 'ส่วนลด' },
    totalAmount: COL.totalAmt,
    status: COL.chillPaymentStatus,
    settled: COL.settledYn,
    icopay: COL.icopayAmt,
    description: { EN: 'Description', JP: '説明', CH: '说明', TH: 'รายละเอียด' },
    transactionDate: { EN: 'Txn date (raw)', JP: '取引日(原文)', CH: '交易日期(原文)', TH: 'วันที่ทำรายการ (ดิบ)' },
    paymentDate: { EN: 'PaymentDate (raw)', JP: 'PaymentDate(原文)', CH: 'PaymentDate(原文)', TH: 'PaymentDate (ดิบ)' }
  });

  var CHILL_ST_COL = Object.assign({}, CHILL_TR_COL, {
    settleAmount: { EN: 'Settle amount', JP: '精算金額(Settle)', CH: '结算金额(Settle)', TH: 'ยอดชำระ Settle' },
    netAmount: { EN: 'Net amount', JP: '純額(Net)', CH: '净额(Net)', TH: 'สุทธิ Net' },
    exchangeRate: { EN: 'FX rate', JP: '為替レート', CH: '汇率', TH: 'อัตราแลกเปลี่ยน' },
    serviceAmount: { EN: 'Service fee', JP: 'サービス料', CH: '服务费', TH: 'ค่าบริการ' },
    serviceVAT: { EN: 'Service VAT', JP: 'サービス VAT', CH: '服务增值税', TH: 'VAT บริการ' },
    serviceWHT: { EN: 'Service WHT', JP: 'サービス WHT', CH: '服务预提税', TH: 'WHT บริการ' },
    transferDate: { EN: 'Transfer date', JP: '振込日', CH: '转账日', TH: 'วันโอน' },
    icopayExpectedSettleAt: { EN: 'Expected (ICOPAY)', JP: '予定(ICOPAY)', CH: '预计(ICOPAY)', TH: 'คาด (ICOPAY)' },
    icopayExpectedSettleRule: { EN: 'Expected rule', JP: '予定規則', CH: '预计规则', TH: 'กฎการคาดการณ์' },
    cutOffTime: { EN: 'Cut-off', JP: 'カットオフ', CH: '截止时间', TH: 'เวลาตัดรอบ' }
  });

  function tRow(row, loc, fallback) {
    if (!row) return fallback;
    if (loc === 'KO') return fallback;
    var v = row[loc];
    if (v != null && String(v).trim() !== '') return v;
    return fallback;
  }

  function ensureCatalogKoSnapshot() {
    var P = w.PG_PAY_LIST_INTEGRATED;
    if (!P || !P.columns) return;
    if (P._i18nKoSnap) return;
    P._i18nKoSnap = {
      cols: P.columns.map(function (c) { return { key: c.key, label: c.label }; }),
      hg: JSON.parse(JSON.stringify(P.headerGroups || []))
    };
  }

  function restoreCatalogFromKoSnap() {
    var P = w.PG_PAY_LIST_INTEGRATED;
    var snap = P && P._i18nKoSnap;
    if (!snap || !snap.cols) return;
    var byKey = {};
    snap.cols.forEach(function (x) { if (x && x.key) byKey[x.key] = x.label; });
    P.columns.forEach(function (c) {
      if (c && c.key && byKey[c.key] != null) c.label = byKey[c.key];
    });
    P.headerGroups = JSON.parse(JSON.stringify(snap.hg || []));
  }

  function applyCatalogLocale(loc) {
    var P = w.PG_PAY_LIST_INTEGRATED;
    if (!P || !P.columns) return;
    ensureCatalogKoSnapshot();
    if (loc === 'KO') {
      restoreCatalogFromKoSnap();
      return;
    }
    var snap = P._i18nKoSnap;
    var byKey = {};
    snap.cols.forEach(function (x) { if (x && x.key) byKey[x.key] = x.label; });
    P.columns.forEach(function (c) {
      if (!c || !c.key) return;
      var row = COL[c.key];
      c.label = tRow(row, loc, byKey[c.key] != null ? byKey[c.key] : c.label);
    });
    (P.headerGroups || []).forEach(function (g, i) {
      var sig = (g.keys || []).join(',');
      var row = HG[sig];
      var koLab = (snap.hg[i] && snap.hg[i].label) || g.label;
      g.label = tRow(row, loc, koLab);
    });
  }

  function ensureChillColSnap(scr) {
    if (!scr || !scr.columns || scr._i18nColSnap) return;
    scr._i18nColSnap = {
      cols: scr.columns.map(function (c) { return { key: c.key, label: c.label }; }),
      hg: JSON.parse(JSON.stringify(scr.headerGroups || []))
    };
  }

  function restoreChillColSnap(scr) {
    var snap = scr && scr._i18nColSnap;
    if (!snap || !scr || !scr.columns) return;
    var byKey = {};
    snap.cols.forEach(function (x) { if (x && x.key) byKey[x.key] = x.label; });
    scr.columns.forEach(function (c) {
      if (c && c.key && byKey[c.key] != null) c.label = byKey[c.key];
    });
    scr.headerGroups = JSON.parse(JSON.stringify(snap.hg || []));
  }

  function applyFeeListScreenLocale(loc) {
    var screens = w.PG_SCREENS && w.PG_SCREENS.getMenuScreens ? w.PG_SCREENS.getMenuScreens() : null;
    if (!screens) return;
    ['/calc/feeList', '/settlement/feeList'].forEach(function (url) {
      var scr = screens[url];
      if (!scr || !scr.columns) return;
      ensureChillColSnap(scr);
      if (loc === 'KO') {
        restoreChillColSnap(scr);
        return;
      }
      var snap = scr._i18nColSnap;
      var byKey = {};
      snap.cols.forEach(function (x) { if (x && x.key) byKey[x.key] = x.label; });
      scr.columns.forEach(function (c) {
        if (!c || !c.key) return;
        var row = COL[c.key];
        c.label = tRow(row, loc, byKey[c.key] != null ? byKey[c.key] : c.label);
      });
      (scr.headerGroups || []).forEach(function (g, i) {
        var sig = (g.keys || []).join(',');
        var row = HG[sig];
        var koLab = (snap.hg[i] && snap.hg[i].label) || g.label;
        g.label = tRow(row, loc, koLab);
      });
    });
  }

  function applyChillListCatalogLocale(loc) {
    var screens = w.PG_SCREENS && w.PG_SCREENS.getMenuScreens ? w.PG_SCREENS.getMenuScreens() : null;
    if (!screens) return;
    [['/calc/chillPayTrList', CHILL_TR_COL], ['/calc/chillPaySettlementList', CHILL_ST_COL]].forEach(function (pair) {
      var url = pair[0];
      var cmap = pair[1];
      var scr = screens[url];
      if (!scr || !scr.columns) return;
      ensureChillColSnap(scr);
      if (loc === 'KO') {
        restoreChillColSnap(scr);
        return;
      }
      var snap = scr._i18nColSnap;
      var byKey = {};
      snap.cols.forEach(function (x) { if (x && x.key) byKey[x.key] = x.label; });
      scr.columns.forEach(function (c) {
        if (!c || !c.key) return;
        var row = cmap[c.key];
        c.label = tRow(row, loc, byKey[c.key] != null ? byKey[c.key] : c.label);
      });
      (scr.headerGroups || []).forEach(function (g, i) {
        var sig = (g.keys || []).join(',');
        var row = HG[sig];
        var koLab = (snap.hg[i] && snap.hg[i].label) || g.label;
        g.label = tRow(row, loc, koLab);
      });
    });
  }

  var _screenSnap = null;

  /** 결제내역 동기 URL 외 — 검색폼·안내 스냅/로케일 적용 대상 */
  var EXTRA_I18N_SCREEN_SNAP_URLS = ['/calc/dailyIntegrated', '/calc/dailyPay', '/calc/dailyFee', '/calc/feeList', '/settlement/feeList', '/calc/exCalcList', '/settlement/execute',
    '/settlement/settlementResultDistribute', '/settlement/settlementResultHold', '/calc/calcGmList', '/settlement/franchiseList',
    '/calc/paySettlementHoldList', '/settlement/paySettlementHoldList', '/ops/integratedReport', '/ops/verifyReport',
    '/comp/compMngTree', '/comp/myCompMng', '/comp/compReg', '/comp/compDetail', '/comp/compInfo', '/comp/compMng',
    '/comp/compInfoHistList', '/comp/compChangeHistory', '/commission/commisionList'];

  /** 단일 헤더 그리드 — 로케일 변경 시 thead 재생성 */
  var COMP_GRID_SINGLE_HEADER_URLS = {
    '/comp/compMngTree': 1,
    '/comp/compInfo': 1,
    '/comp/compMng': 1,
    '/comp/compInfoHistList': 1,
    '/comp/compChangeHistory': 1,
    '/calc/settlementReport': 1,
    '/settlement/settlementReport': 1,
    '/noti/notiUrlMng': 1,
    '/notify/payUrlMng': 1,
    '/noti/notiCashReceiptUrlMng': 1,
    '/notify/cashReceiptUrlMng': 1,
    '/set/gridSetMng': 1,
    '/user/menuOrderMng': 1,
    '/risk/list': 1
  };

  function deepClone(o) {
    try {
      return JSON.parse(JSON.stringify(o));
    } catch (e) {
      return null;
    }
  }

  function ensureScreenSnap() {
    if (_screenSnap) return;
    var screens = w.PG_SCREENS && w.PG_SCREENS.getMenuScreens ? w.PG_SCREENS.getMenuScreens() : null;
    if (!screens) return;
    var urls = w.PG_SCREENS.getPayListIntegratedSyncUrls ? w.PG_SCREENS.getPayListIntegratedSyncUrls().slice() : [];
    try {
      Object.keys(screens).forEach(function (k) {
        if (!k) return;
        var s = screens[k];
        if (!s) return;
        var hasSr =
          (s.searchRows && s.searchRows.length) ||
          (s.searchRows2 && s.searchRows2.length) ||
          (s.searchRows3 && s.searchRows3.length);
        if (hasSr && urls.indexOf(k) === -1) urls.push(k);
      });
    } catch (eSnapUrls) {}
    EXTRA_I18N_SCREEN_SNAP_URLS.forEach(function (eu) {
      if (eu && urls.indexOf(eu) === -1) urls.push(eu);
    });
    _screenSnap = { urls: urls, buttons: {}, empty: {}, scrKo: {} };
    urls.forEach(function (u) {
      var scr = screens[u];
      if (!scr) return;
      if (scr.emptyMessage != null) _screenSnap.empty[u] = scr.emptyMessage;
      _screenSnap.buttons[u] = (scr.buttons || []).map(function (b) {
        return { id: b && b.id, label: b && b.label };
      });
      _screenSnap.scrKo[u] = {
        searchRows: deepClone(scr.searchRows),
        searchRows2: scr.searchRows2 ? deepClone(scr.searchRows2) : null,
        searchRows3: scr.searchRows3 ? deepClone(scr.searchRows3) : null,
        noticeList: scr.noticeList ? scr.noticeList.slice() : null,
        summary: scr.summary ? scr.summary.slice() : null,
        columns: scr.columns ? scr.columns.map(function (c) {
          return c ? { key: c.key, statusBucketKey: c.statusBucketKey, label: c.label, type: c.type } : null;
        }) : null,
        isDailySummaryScreen: !!scr.isDailySummaryScreen,
        isOpsVerifyReport: !!scr.isOpsVerifyReport
      };
    });
  }

  function applyDailySummaryColumnsLocale(scr, snap, loc) {
    if (!scr || (!scr.isDailySummaryScreen && !scr.isOpsVerifyReport) || !scr.columns || !snap || !snap.columns) return;
    if (loc === 'KO') {
      scr.columns.forEach(function (c, idx) {
        var s = snap.columns[idx];
        if (s && c && s.label != null) c.label = s.label;
      });
      return;
    }
    scr.columns.forEach(function (c) {
      if (!c) return;
      var snapRow = null;
      for (var i = 0; i < snap.columns.length; i++) {
        var s = snap.columns[i];
        if (!s) continue;
        if (c.statusBucketKey && s.statusBucketKey === c.statusBucketKey) {
          snapRow = s;
          break;
        }
        if (c.key && s.key === c.key) {
          snapRow = s;
          break;
        }
      }
      var fb = snapRow && snapRow.label != null ? snapRow.label : c.label;
      if (c.statusBucketKey) {
        var bk = STATUS_BUCKET[c.statusBucketKey];
        if (bk) c.label = tRow(bk, loc, fb);
      } else if (c.key) {
        var row = COL[c.key];
        if (row) c.label = tRow(row, loc, fb);
      }
    });
  }

  function pickNoticeLine(pack, loc) {
    if (!pack) return '';
    if (loc === 'KO') return pack.KO != null ? String(pack.KO) : '';
    var v = pack[loc];
    if (v != null && String(v).trim() !== '') return String(v);
    if (pack.EN != null && String(pack.EN).trim() !== '') return String(pack.EN);
    return pack.KO != null ? String(pack.KO) : '';
  }

  function walkSearchRowsApplyLocale(rows, snapRows, loc) {
    if (!rows || !snapRows || rows.length !== snapRows.length || loc === 'KO') return;
    for (var r = 0; r < rows.length; r++) {
      var row = rows[r];
      var srow = snapRows[r];
      if (!row || !srow || row.length !== srow.length) continue;
      for (var c = 0; c < row.length; c++) {
        var cell = row[c];
        var scell = srow[c];
        if (!cell || !scell) continue;
        if (cell.type === 'searchBtn') {
          if (cell._i18nOrigSearchLbl == null) cell._i18nOrigSearchLbl = scell.label != null ? scell.label : '검색';
          cell.label = UI.search[loc] || cell.label;
        } else if (cell.type === 'searchReset' || (cell.type === 'button' && cell.name === 'searchReset')) {
          cell.label = UI.searchReset[loc] || cell.label;
        } else if (cell.type === 'compMngSearchActions') {
          var ckb = cell.checkboxName || 'searchIncludeSub';
          var ckLb = LBL['cb:' + ckb];
          if (ckLb && cell.label != null) cell.label = ckLb[loc] || ckLb.EN || scell.label;
          cell.searchLabel = (UI.search[loc] || UI.search.EN || scell.searchLabel);
        } else if (cell.type === 'select' && cell.name) {
          var nm = cell.name;
          var lbKeySel = cell.i18nLblKey || nm;
          var lbRow = LBL[lbKeySel];
          if (lbRow) cell.label = lbRow[loc] || lbRow.EN || scell.label;
          (cell.options || []).forEach(function (opt) {
            var k = nm + '|' + (opt.v != null ? String(opt.v) : '');
            var snapOpt = (scell.options || []).filter(function (x) { return x && String(x.v) === String(opt.v); })[0];
            var fb = snapOpt ? snapOpt.t : opt.t;
            opt.t = resolveOptText(loc, k, fb);
          });
        } else if (cell.type === 'text' && cell.name) {
          var lr = LBL[cell.i18nLblKey || (cell.name + ':label')];
          if (lr) cell.label = lr[loc] || lr.EN || scell.label;
          var phKeyWalk = cell.i18nPhKey || scell.i18nPhKey || cell.name;
          var pr = PH[phKeyWalk];
          if (pr) cell.placeholder = pr[loc] || pr.EN || scell.placeholder;
        } else if (cell.type === 'daterange') {
          var drKey = scell.label ? ('drLbl:' + scell.label) : null;
          var dr = drKey ? LBL[drKey] : null;
          if (dr) cell.label = dr[loc] || dr.EN || scell.label;
        } else if (cell.type === 'checkbox' && cell.name) {
          var ck = LBL['cb:' + cell.name];
          if (ck) cell.label = ck[loc] || ck.EN || scell.label;
        } else if (cell.type === 'quickdate') {
          var rangesQd = cell.quickdateRanges || (scell && scell.quickdateRanges);
          if (!rangesQd || !rangesQd.length) rangesQd = ['day', 'month', 'prevDay', 'week', 'week2', 'prevMonth'];
          if (!cell.quickdateLabels) cell.quickdateLabels = [];
          var snapLabsQd = scell && scell.quickdateLabels ? scell.quickdateLabels : [];
          for (var qi = 0; qi < rangesQd.length; qi++) {
            var qk = rangesQd[qi];
            var qrow = QD[qk];
            var snapLb = snapLabsQd[qi] != null ? snapLabsQd[qi] : null;
            var fbLb = snapLb != null ? snapLb : (cell.quickdateLabels[qi] != null ? cell.quickdateLabels[qi] : '');
            if (qrow) cell.quickdateLabels[qi] = lblText(qrow, loc, fbLb);
            else if (snapLb != null) cell.quickdateLabels[qi] = snapLb;
          }
        }
      }
    }
  }

  function applyMenuScreensSearchAndNoticesLocale(loc) {
    if (!_screenSnap) ensureScreenSnap();
    var screens = w.PG_SCREENS && w.PG_SCREENS.getMenuScreens ? w.PG_SCREENS.getMenuScreens() : null;
    if (!screens || !_screenSnap || !_screenSnap.urls) return;
    _screenSnap.urls.forEach(function (u) {
      var scr = screens[u];
      var snap = _screenSnap.scrKo[u];
      if (!scr || !snap) return;
      if (loc === 'KO') {
        if (snap.searchRows) scr.searchRows = deepClone(snap.searchRows);
        if (snap.searchRows2 != null) scr.searchRows2 = deepClone(snap.searchRows2);
        if (snap.searchRows3 != null) scr.searchRows3 = deepClone(snap.searchRows3);
        if (snap.noticeList) scr.noticeList = snap.noticeList.slice();
        if (COMP_GRID_SINGLE_HEADER_URLS[u] && snap.columns && scr.columns) {
          scr.columns.forEach(function (c, idx) {
            var s = snap.columns[idx];
            if (s && c && s.key === c.key && s.label != null) c.label = s.label;
          });
        }
        if (scr.isDailySummaryScreen || scr.isOpsVerifyReport) applyDailySummaryColumnsLocale(scr, snap, 'KO');
        return;
      }
      walkSearchRowsApplyLocale(scr.searchRows, snap.searchRows, loc);
      if (scr.searchRows2 && snap.searchRows2) walkSearchRowsApplyLocale(scr.searchRows2, snap.searchRows2, loc);
      if (scr.searchRows3 && snap.searchRows3) walkSearchRowsApplyLocale(scr.searchRows3, snap.searchRows3, loc);
      if (scr.noticeList && snap.noticeList) {
        var packs = NOTICES[u];
        for (var i = 0; i < scr.noticeList.length; i++) {
          var pk = packs && packs[i];
          scr.noticeList[i] = pk ? pickNoticeLine(pk, loc) : snap.noticeList[i];
        }
      }
      if (COMP_GRID_SINGLE_HEADER_URLS[u] && snap.columns && scr.columns) {
        scr.columns.forEach(function (c) {
          if (!c || !c.key) return;
          var snapRow = snap.columns.filter(function (x) { return x && x.key === c.key; })[0];
          if (snapRow && snapRow.label != null) c.label = snapRow.label;
        });
      }
      if (scr.isDailySummaryScreen || scr.isOpsVerifyReport) applyDailySummaryColumnsLocale(scr, snap, loc);
    });
  }

  function refreshDailySummaryOpenPane(pane, cfg, tid, loc) {
    if (!pane || !cfg || !cfg.isDailySummaryScreen || !tid) return;
    loc = loc || getLocale();
    var kind = cfg.dailySummaryKind ? String(cfg.dailySummaryKind) : 'pay';
    var escHtml = function (s) {
      return (w.PG_UI_I18N && typeof w.PG_UI_I18N.t === 'function') ? w.PG_UI_I18N.t(String(s)) : String(s);
    };
    var theadRow = pane.querySelector('#grid_' + tid + ' thead tr');
    if (theadRow && cfg.columns && cfg.columns.length) {
      theadRow.innerHTML = cfg.columns.map(function (c) {
        var lab = c.label != null ? c.label : (c.key || '');
        return '<th class="text-nowrap">' + escHtml(lab) + '</th>';
      }).join('');
    }
    var detWrap = pane.querySelector('#dailyDetailWrap_' + tid);
    if (detWrap) {
      var titleEl = detWrap.querySelector('#dailyDetailTitle_' + tid);
      if (titleEl) {
        var daySuffix = '';
        var mDay = (titleEl.textContent || '').match(/\((\d{4}-\d{2}-\d{2})\)\s*$/);
        if (mDay) daySuffix = ' (' + mDay[1] + ')';
        titleEl.textContent = escHtml((UI.detailTitle && UI.detailTitle[loc] ? UI.detailTitle[loc] : '선택 일자 상세') + daySuffix);
      }
      refreshDailyDetailCountLabel(detWrap.querySelector('#dailyDetailCount_' + tid));
      var lex = detWrap.querySelector('#listExcelDownBtn span[data-pg-ui-t]');
      if (lex) lex.textContent = escHtml(UI.listExcel[loc] || UI.listExcel.KO);
      var hintTd = detWrap.querySelector('#grid_' + tid + '_detail tbody td.text-muted');
      if (hintTd && hintTd.colSpan >= 8) {
        hintTd.textContent = escHtml('위에서 일자를 더블클릭하세요.');
      }
      if (w.PG_UI_I18N && typeof w.PG_UI_I18N.applyDom === 'function') {
        try { w.PG_UI_I18N.applyDom(detWrap); } catch (eDetWrapI18n) {}
      }
    }
    var irDetHost = pane.querySelector('#integratedReportDetail_' + tid);
    if (irDetHost) {
      var irTitleEl = irDetHost.querySelector('#integratedReportDetailTitle_' + tid);
      if (irTitleEl) {
        var daySuffixIr = '';
        var mDayIr = (irTitleEl.textContent || '').match(/\((\d{4}-\d{2}-\d{2})\)\s*$/);
        if (mDayIr) daySuffixIr = ' (' + mDayIr[1] + ')';
        irTitleEl.textContent = escHtml((UI.detailTitleIr && UI.detailTitleIr[loc] ? UI.detailTitleIr[loc] : UI.detailTitleIr.KO) + daySuffixIr);
      }
      refreshDailyDetailCountLabel(irDetHost.querySelector('#integratedReportDetailCount_' + tid));
      var lexIr = irDetHost.querySelector('#listExcelDownBtn span[data-pg-ui-t]');
      if (lexIr) lexIr.textContent = escHtml(UI.listExcel[loc] || UI.listExcel.KO);
      if (w.PG_UI_I18N && typeof w.PG_UI_I18N.applyDom === 'function') {
        try { w.PG_UI_I18N.applyDom(irDetHost); } catch (eIrDetWrapI18n) {}
      }
    }
    var emptyCell = pane.querySelector('#grid_' + tid + ' tbody td.text-center.text-muted');
    if (emptyCell && emptyCell.closest('tr') && !emptyCell.closest('tr').classList.contains('daily-master-row')) {
      var onlyEmpty = pane.querySelectorAll('#grid_' + tid + ' tbody tr').length === 1;
      if (onlyEmpty) emptyCell.textContent = escHtml('데이터 없음');
    }
  }

  function uiTPlain(s, loc) {
    if (loc === 'KO') return String(s);
    return (w.PG_UI_I18N && typeof w.PG_UI_I18N.t === 'function') ? w.PG_UI_I18N.t(String(s)) : String(s);
  }

  function formatVerifyReportDayNote(row, loc) {
    loc = loc || getLocale();
    var hasMis = row && (row.hasMismatch === true || (row.mismatchCount != null && Number(row.mismatchCount) > 0));
    if (!hasMis) return uiTPlain('일치', loc);
    var nm = row.notiMissingCount != null ? Number(row.notiMissingCount) : NaN;
    var ns = row.statusDiffCount != null ? Number(row.statusDiffCount) : NaN;
    var na = row.amountDiffCount != null ? Number(row.amountDiffCount) : NaN;
    if (!isNaN(nm) || !isNaN(ns) || !isNaN(na)) {
      var parts = [];
      if (!isNaN(nm) && nm > 0) parts.push(uiTPlain('NOTI 미수신', loc) + ' ' + nm + uiTPlain('건', loc));
      if (!isNaN(ns) && ns > 0) parts.push(uiTPlain('상태 불일치', loc) + ' ' + ns + uiTPlain('건', loc));
      if (!isNaN(na) && na > 0) parts.push(uiTPlain('결제액 불일치', loc) + ' ' + na + uiTPlain('건', loc));
      var nr = row.requestNoNotiCount != null ? Number(row.requestNoNotiCount) : NaN;
      if (!isNaN(nr) && nr > 0) parts.push(uiTPlain('요청·대기', loc) + ' ' + nr + uiTPlain('건 제외', loc));
      if (parts.length) return parts.join(', ');
    }
    var note = row && row.note != null ? String(row.note) : '';
    return note ? uiTPlain(note, loc) : '';
  }

  function refreshVerifyReportOpenPane(pane, cfg, tid, loc) {
    if (!pane || !cfg || !cfg.isOpsVerifyReport || !tid) return;
    loc = loc || getLocale();
    if (typeof pane._verifyReportRefreshI18n === 'function') {
      try { pane._verifyReportRefreshI18n(loc); } catch (eVrCb) {}
      return;
    }
    var escHtml = function (s) {
      return uiTPlain(s, loc);
    };
    var theadRow = pane.querySelector('#grid_' + tid + ' thead tr');
    if (theadRow && cfg.columns && cfg.columns.length) {
      theadRow.innerHTML = cfg.columns.map(function (c) {
        var lab = c.label != null ? c.label : (c.key || '');
        return '<th class="text-nowrap">' + escHtml(lab) + '</th>';
      }).join('');
    }
    var detWrap = pane.querySelector('#verifyReportDetailWrap_' + tid);
    if (detWrap) {
      var titleEl = detWrap.querySelector('#verifyReportDetailTitle_' + tid);
      if (titleEl) {
        var daySuffix = '';
        var mDay = (titleEl.textContent || '').match(/\((\d{4}-\d{2}-\d{2})\)\s*$/);
        if (mDay) daySuffix = ' (' + mDay[1] + ')';
        titleEl.textContent = escHtml((UI.detailTitleVr && UI.detailTitleVr[loc] ? UI.detailTitleVr[loc] : UI.detailTitleVr.KO) + daySuffix);
      }
      if (w.PG_UI_I18N && typeof w.PG_UI_I18N.applyDom === 'function') {
        try { w.PG_UI_I18N.applyDom(detWrap); } catch (eDetVr) {}
      }
    }
    var emptyCell = pane.querySelector('#grid_' + tid + ' tbody td.text-center.text-muted');
    if (emptyCell && emptyCell.closest('tr') && !emptyCell.closest('tr').classList.contains('verify-report-day-row')) {
      var onlyEmpty = pane.querySelectorAll('#grid_' + tid + ' tbody tr').length === 1;
      if (onlyEmpty) emptyCell.textContent = escHtml(cfg.emptyMessage || UI.empty[loc] || UI.empty.KO);
    }
  }

  function patchCalcCycleSearchOptionAll(loc) {
    var arr = w.PG_CALC_CYCLE_SEARCH_OPTIONS;
    if (!arr || !arr.length) return;
    var o0 = arr[0];
    if (!o0) return;
    if (loc === 'KO') {
      if (o0._i18nOrigT != null) o0.t = o0._i18nOrigT;
      return;
    }
    if (o0._i18nOrigT == null) o0._i18nOrigT = o0.t != null ? o0.t : '전체';
    o0.t = OPT0[loc] || OPT0.EN || o0._i18nOrigT;
  }

  function applyScreenChromeLocale(loc) {
    var screens = w.PG_SCREENS && w.PG_SCREENS.getMenuScreens ? w.PG_SCREENS.getMenuScreens() : null;
    if (!screens) return;
    if (!_screenSnap) ensureScreenSnap();
    var urls = _screenSnap.urls || [];
    urls.forEach(function (u) {
      var scr = screens[u];
      if (!scr) return;
      if (loc === 'KO') {
        scr.emptyMessage = _screenSnap.empty[u] != null ? _screenSnap.empty[u] : scr.emptyMessage;
        var snapB = _screenSnap.buttons[u];
        if (snapB && snapB.length && scr.buttons) {
          var snapMap = {};
          snapB.forEach(function (x) {
            if (x && x.id) snapMap[String(x.id)] = x.label;
          });
          scr.buttons.forEach(function (b) {
            if (!b || !b.id) return;
            var id = String(b.id);
            if (snapMap[id] != null) b.label = snapMap[id];
          });
        }
        return;
      }
      scr.emptyMessage = UI.empty[loc] || scr.emptyMessage;
      (scr.buttons || []).forEach(function (b) {
        if (!b || !b.id) return;
        var id = String(b.id);
        if (id === 'payListRefreshBtn') b.label = UI.refresh[loc] || b.label;
        else if (id === 'excelDownBtn' || id === 'excelBtn') b.label = UI.excel[loc] || b.label;
        else if (id === 'excelAllDownBtn') b.label = (w.PG_UI_I18N && typeof w.PG_UI_I18N.t === 'function') ? w.PG_UI_I18N.t('모두다운로드') : b.label;
        else if (id === 'listExcelDownBtn') b.label = UI.listExcel[loc] || b.label;
        else if (id === 'searchBtn') {
          var snapB2 = _screenSnap.buttons[u];
          var origSb = (snapB2 || []).filter(function (x) { return x && String(x.id) === 'searchBtn'; })[0];
          if (origSb && String(origSb.label || '') === '조회') {
            b.label = UI.lookup[loc] || UI.lookup.EN || b.label;
          } else {
            b.label = UI.search[loc] || b.label;
          }
        }
        else if (id === 'excelSampleBtn') b.label = UI.excelSample[loc] || UI.excelSample.EN || b.label;
        else if (id === 'excelRegBtn') b.label = UI.excelReg[loc] || UI.excelReg.EN || b.label;
        else if (id === 'compRegBtn') b.label = UI.compReg[loc] || UI.compReg.EN || b.label;
        else if (id === 'compInfoDetailBtn') b.label = UI.compInfoDetail[loc] || UI.compInfoDetail.EN || b.label;
        else if (id === 'receivableRegBtn') b.label = UI.receivableReg[loc] || UI.receivableReg.EN || b.label;
        else if (id === 'hqPgApiOperationalSaveBtn') b.label = UI.hqPgApiOpSave[loc] || UI.hqPgApiOpSave.EN || b.label;
        else if (id === 'hqPgApiAddBtn') b.label = UI.hqPgApiAdd[loc] || UI.hqPgApiAdd.EN || b.label;
        else if (id === 'commissionSettingBtn') b.label = UI.commissionSetting[loc] || UI.commissionSetting.EN || b.label;
        else if (id === 'commissionInlineTopSaveBtn') b.label = UI.cgSave[loc] || UI.cgSave.EN || b.label;
        else if (id === 'settlementPublishDistributeBtn') b.label = UI.settlementPublishDistribute[loc] || UI.settlementPublishDistribute.EN || b.label;
        else if (id === 'settlementPublishHoldBtn') b.label = UI.settlementPublishHold[loc] || UI.settlementPublishHold.EN || b.label;
        else if (id === 'payoutHoldReleaseBtn') b.label = UI.payoutHoldReleaseBulk[loc] || UI.payoutHoldReleaseBulk.EN || b.label;
      });
    });
  }

  /** 통합 결제 그리드 + 유통망 정산 검색칸 등 동일 DOM i18n 훅 */
  function isPayMngDomPaneUrl(url) {
    if (!url) return false;
    var integrated = w.PG_SCREENS && w.PG_SCREENS.getPayListIntegratedSyncUrls ? w.PG_SCREENS.getPayListIntegratedSyncUrls() : [];
    if (integrated.indexOf(url) !== -1) return true;
    return url === '/calc/calcList' || url === '/settlement/distributionList'
      || url === '/calc/dailyIntegrated' || url === '/calc/dailyPay' || url === '/calc/dailyFee'
      || url === '/calc/feeList' || url === '/settlement/feeList'
      || url === '/calc/exCalcList' || url === '/settlement/execute'
      || url === '/calc/calcGmList' || url === '/settlement/franchiseList'
      || url === '/calc/collateralList' || url === '/settlement/collateralList'
      || url === '/calc/compPointMngList' || url === '/settlement/recallMng'
      || url === '/calc/unpaidMng' || url === '/settlement/unpaidMng'
      || url === '/calc/settlementReport' || url === '/settlement/settlementReport'
      || url === '/settlement/settlementResultDistribute' || url === '/settlement/settlementResultHold'
      || url === '/calc/paySettlementHoldList' || url === '/settlement/paySettlementHoldList'
      || url === '/noti/notiCashReceiptSendMngList' || url === '/notify/cashReceiptSendMng'
      || url === '/noti/notiSendMngList' || url === '/notify/paySendMng'
      || url === '/noti/notiUrlMng' || url === '/notify/payUrlMng'
      || url === '/noti/notiCashReceiptUrlMng' || url === '/notify/cashReceiptUrlMng'
      || url === '/set/gridSetMng' || url === '/user/menuOrderMng'
      || url === '/user/userMng'
      || url === '/ops/mailLog' || url === '/ops/taxReport' || url === '/ops/integratedReport' || url === '/ops/verifyReport' || url === '/ops/opsMng'
      || url === '/hq/pgApiMng'
      || url === '/commission/commisionList'
      || url === '/system/noticeList'
      || url === '/risk/list'
      || (url.indexOf('/comp/') === 0);
  }

  function refreshOpenPayListTheads(loc) {
    var screens = w.PG_SCREENS && w.PG_SCREENS.getMenuScreens ? w.PG_SCREENS.getMenuScreens() : null;
    var build = w.PG_SCREENS && w.PG_SCREENS.buildStandardDataGridTheadHtml;
    if (!screens || !build) return;
    var urls = w.PG_SCREENS.getPayListIntegratedSyncUrls ? w.PG_SCREENS.getPayListIntegratedSyncUrls() : [];
    document.querySelectorAll('.tab-pane.tabConDiv[formurl]').forEach(function (pane) {
      var url = pane.getAttribute('formurl');
      if (!url) return;
      var cfg = screens[url];
      if (!cfg) return;
      var inIntegrated = urls.indexOf(url) !== -1;
      var hasTwoRowHeader = cfg.headerGroups && cfg.headerGroups.length;
      var compSingle = COMP_GRID_SINGLE_HEADER_URLS[url];
      if (!inIntegrated && !hasTwoRowHeader && !compSingle) return;
      var tid = pane.id;
      var thead = pane.querySelector('#grid_' + tid + ' thead');
      if (!thead || !pane._lastGridCols || !pane._lastGridCols.length) return;
      thead.innerHTML = build(pane._lastGridCols, cfg.headerGroups || [], { selectAllTitle: '전체선택' });
      if (w.PG_UI_I18N && typeof w.PG_UI_I18N.applyDom === 'function') {
        try { w.PG_UI_I18N.applyDom(thead); } catch (eTheadI18n) {}
      }
      if (w.PG_TABLE_COL_RESIZE && typeof w.PG_TABLE_COL_RESIZE.refreshInSync === 'function') {
        w.PG_TABLE_COL_RESIZE.refreshInSync(pane);
      } else if (w.PG_TABLE_COL_RESIZE && typeof w.PG_TABLE_COL_RESIZE.refreshIn === 'function') {
        w.PG_TABLE_COL_RESIZE.refreshIn(pane);
      }
    });
  }

  function refreshOpenPayListPaneChrome(loc) {
    var sLab = UI.search[loc] || UI.search.KO;
    var rLab = UI.refresh[loc] || UI.refresh.KO;
    var eLab = UI.excel[loc] || UI.excel.KO;
    var pLab = UI.printSetup[loc] || UI.printSetup.KO;
    document.querySelectorAll('.tab-pane.tabConDiv[formurl] #listExcelDownBtn span[data-pg-ui-t]').forEach(function (lexSpan) {
      lexSpan.textContent = UI.listExcel[loc] || UI.listExcel.KO;
    });
    document.querySelectorAll('.tab-pane.tabConDiv[formurl]').forEach(function (pane) {
      var url = pane.getAttribute('formurl');
      if (!url) return;
      var rf = pane.querySelector('#payListRefreshBtn');
      if (rf) rf.textContent = rLab;
      var ex = pane.querySelector('#excelDownBtn');
      if (!ex) ex = pane.querySelector('#excelBtn');
      if (ex) ex.textContent = eLab;
      var exAll = pane.querySelector('#excelAllDownBtn');
      if (exAll) {
        var exAllSpan = exAll.querySelector('span[data-pg-ui-t]');
        if (exAllSpan && w.PG_UI_I18N && typeof w.PG_UI_I18N.t === 'function') {
          exAllSpan.textContent = w.PG_UI_I18N.t(exAllSpan.getAttribute('data-pg-ui-t') || '모두다운로드');
        } else if (w.PG_UI_I18N && typeof w.PG_UI_I18N.t === 'function') {
          exAll.textContent = w.PG_UI_I18N.t('모두다운로드');
        }
      }
      var pr = pane.querySelector('#printBtn');
      if (pr) pr.textContent = pLab;
      var tbSearch = pane.querySelector('#searchBtn');
      if (tbSearch) {
        var tsp = tbSearch.querySelector('span[data-pg-ui-t]');
        if (tsp) {
          var tk = tsp.getAttribute('data-pg-ui-t');
          if (tk && w.PG_UI_I18N && typeof w.PG_UI_I18N.t === 'function') tsp.textContent = w.PG_UI_I18N.t(tk);
          else tsp.textContent = sLab;
        } else {
          tbSearch.textContent = (url === '/settlement/execute')
            ? (UI.lookup[loc] || UI.lookup.EN || UI.lookup.KO)
            : sLab;
        }
      }
      var recvReg = pane.querySelector('#receivableRegBtn');
      if (recvReg) recvReg.textContent = (UI.receivableReg[loc] || UI.receivableReg.EN || UI.receivableReg.KO);
      if (url === '/user/userMng') {
        var addB = pane.querySelector('#addBtn');
        if (addB) addB.textContent = lblText(UI.userAdd, loc, addB.textContent);
        var saveUB = pane.querySelector('#saveBtn');
        if (saveUB) saveUB.textContent = lblText(UI.cgSave, loc, saveUB.textContent);
      }
      if (url === '/hq/pgApiMng') {
        var hqOpSave = pane.querySelector('#hqPgApiOperationalSaveBtn');
        if (hqOpSave) hqOpSave.textContent = lblText(UI.hqPgApiOpSave, loc, hqOpSave.textContent);
        var hqPgAdd = pane.querySelector('#hqPgApiAddBtn');
        if (hqPgAdd) hqPgAdd.textContent = lblText(UI.hqPgApiAdd, loc, hqPgAdd.textContent);
      }
      if (url === '/commission/commisionList') {
        var cSet = pane.querySelector('#commissionSettingBtn');
        if (cSet) cSet.textContent = lblText(UI.commissionSetting, loc, cSet.textContent);
        var cTopSave = pane.querySelector('#commissionInlineTopSaveBtn');
        if (cTopSave) cTopSave.textContent = lblText(UI.cgSave, loc, cTopSave.textContent);
        var cPagSave = pane.querySelector('#commissionPaginationSaveBtn');
        if (cPagSave) cPagSave.textContent = lblText(UI.cgSave, loc, cPagSave.textContent);
      }
      if (url === '/settlement/settlementResultDistribute') {
        var pubDist = pane.querySelector('#settlementPublishDistributeBtn');
        if (pubDist) pubDist.textContent = lblText(UI.settlementPublishDistribute, loc, pubDist.textContent);
        var pubHold = pane.querySelector('#settlementPublishHoldBtn');
        if (pubHold) pubHold.textContent = lblText(UI.settlementPublishHold, loc, pubHold.textContent);
      }
      if (url === '/calc/paySettlementHoldList' || url === '/settlement/paySettlementHoldList') {
        var phRel = pane.querySelector('#payoutHoldReleaseBtn');
        if (phRel) phRel.textContent = lblText(UI.payoutHoldReleaseBulk, loc, phRel.textContent);
      }
      var inlineSearch = pane.querySelector('.screen-search-btn');
      if (inlineSearch) {
        var ink = url === '/settlement/execute' ? '조회' : '검색';
        inlineSearch.innerHTML = '<i class="bi bi-search"></i> <span data-pg-ui-t="' + ink + '"></span>';
        var isp = inlineSearch.querySelector('span[data-pg-ui-t]');
        if (isp && w.PG_UI_I18N && typeof w.PG_UI_I18N.t === 'function') {
          isp.textContent = w.PG_UI_I18N.t(ink);
        }
      }
      var emptyCell = pane.querySelector('#grid_' + pane.id + ' tbody .empty-state-cell');
      if (emptyCell) {
        var ekEl = emptyCell.querySelector('[data-pg-ui-t]');
        if (ekEl) {
          var ek = ekEl.getAttribute('data-pg-ui-t');
          if (ek && w.PG_UI_I18N && typeof w.PG_UI_I18N.t === 'function') ekEl.textContent = w.PG_UI_I18N.t(ek);
        } else {
          emptyCell.textContent = UI.empty[loc] || UI.empty.KO;
        }
      }
      if (w.PG_UI_I18N && typeof w.PG_UI_I18N.applyDom === 'function') {
        try {
          var gid0 = pane.id;
          if (url === '/calc/exCalcList' || url === '/settlement/execute' || url === '/settlement/settlementResultDistribute' || url === '/settlement/settlementResultHold'
            || url === '/calc/settlementReport' || url === '/settlement/settlementReport'
            || url === '/calc/calcGmList' || url === '/settlement/franchiseList') {
            var actRowSe = pane.querySelector('.screen-summary-action-row');
            if (actRowSe) w.PG_UI_I18N.applyDom(actRowSe);
            var thSe = pane.querySelector('#grid_' + gid0 + ' thead');
            if (thSe) w.PG_UI_I18N.applyDom(thSe);
            var detCardSe = pane.querySelector('#settlementExecuteDetailCard_' + gid0);
            if (detCardSe) w.PG_UI_I18N.applyDom(detCardSe);
          }
          if (url === '/calc/paySettlementHoldList' || url === '/settlement/paySettlementHoldList') {
            var actPh = pane.querySelector('.screen-summary-action-row');
            if (actPh) w.PG_UI_I18N.applyDom(actPh);
            var thPh = pane.querySelector('#grid_' + gid0 + ' thead');
            if (thPh) w.PG_UI_I18N.applyDom(thPh);
            var tbPh = pane.querySelector('#grid_' + gid0 + ' tbody');
            if (tbPh) w.PG_UI_I18N.applyDom(tbPh);
          }
        } catch (eSeDom) {}
      }
    });
  }

  function lblText(row, loc, fallback) {
    if (!row) return fallback;
    if (loc === 'KO') return fallback;
    return row[loc] || row.EN || fallback;
  }

  function refreshOpenPayMngDomI18n(loc) {
    var screens = w.PG_SCREENS && w.PG_SCREENS.getMenuScreens ? w.PG_SCREENS.getMenuScreens() : null;
    if (!screens) return;
    document.querySelectorAll('.tab-pane.tabConDiv[formurl]').forEach(function (pane) {
      var url = pane.getAttribute('formurl');
      if (!url) return;
      var cfg = screens[url];
      var tid = pane.id || '';
      pane.querySelectorAll('[data-pg-i18n-lbl]').forEach(function (el) {
        var k = el.getAttribute('data-pg-i18n-lbl');
        if (!k) return;
        var row = LBL[k];
        if (row) el.textContent = lblText(row, loc, el.textContent);
      });
      pane.querySelectorAll('[data-pg-i18n-ph]').forEach(function (el) {
        var k = el.getAttribute('data-pg-i18n-ph');
        if (!k) return;
        var row = PH[k];
        if (row) el.setAttribute('placeholder', lblText(row, loc, el.getAttribute('placeholder') || ''));
      });
      pane.querySelectorAll('option[data-pg-i18n-opt]').forEach(function (el) {
        var k = el.getAttribute('data-pg-i18n-opt');
        if (!k) return;
        var row = OPT[k];
        if (row) el.textContent = lblText(row, loc, el.textContent);
        else el.textContent = resolveOptText(loc, k, el.textContent);
      });
      pane.querySelectorAll('[data-pg-i18n-qd]').forEach(function (el) {
        var k = el.getAttribute('data-pg-i18n-qd');
        if (!k) return;
        var row = QD[k];
        if (row) el.textContent = lblText(row, loc, el.textContent);
      });
      pane.querySelectorAll('[data-pg-i18n-reset="1"]').forEach(function (el) {
        el.textContent = lblText(UI.searchReset, loc, el.textContent);
      });
      pane.querySelectorAll('[data-pg-i18n-sort="DESC"]').forEach(function (el) {
        var sp = el.querySelector('span[data-pg-ui-t]');
        if (sp) sp.textContent = lblText(UI.sortDesc, loc, sp.textContent);
        else el.textContent = lblText(UI.sortDesc, loc, el.textContent);
      });
      pane.querySelectorAll('[data-pg-i18n-sort="ASC"]').forEach(function (el) {
        var sp = el.querySelector('span[data-pg-ui-t]');
        if (sp) sp.textContent = lblText(UI.sortAsc, loc, sp.textContent);
        else el.textContent = lblText(UI.sortAsc, loc, el.textContent);
      });
      pane.querySelectorAll('[data-pg-i18n-aria="sortToolbar"]').forEach(function (el) {
        el.setAttribute('aria-label', lblText(UI.sortToolbarAria, loc, el.getAttribute('aria-label') || ''));
      });
      var cgMap = {
        default: UI.cgDefault,
        release: UI.cgRelease,
        selectAllCols: UI.cgSelectAllCols,
        save: UI.cgSave,
        restore: UI.cgRestore,
        viewSettingTitle: UI.cgTitle
      };
      pane.querySelectorAll('[data-pg-i18n-cg-act]').forEach(function (el) {
        var act = el.getAttribute('data-pg-i18n-cg-act');
        var row = act ? cgMap[act] : null;
        if (row) el.textContent = lblText(row, loc, el.textContent);
      });
      pane.querySelectorAll('[data-pg-i18n-cg-title="restoreTip"]').forEach(function (el) {
        el.setAttribute('title', lblText(UI.cgRestoreTip, loc, el.getAttribute('title') || ''));
      });
      var packs = NOTICES[url];
      pane.querySelectorAll('[data-pg-notice-idx]').forEach(function (el) {
        var idx = parseInt(el.getAttribute('data-pg-notice-idx'), 10);
        if (isNaN(idx) || !packs || !packs[idx]) return;
        el.textContent = pickNoticeLine(packs[idx], loc);
      });
      if (cfg && cfg.isDailySummaryScreen) {
        try {
          refreshDailySummaryOpenPane(pane, cfg, tid, loc);
        } catch (eDailyPaneI18n) {}
      }
      if (cfg && cfg.isOpsVerifyReport) {
        try {
          refreshVerifyReportOpenPane(pane, cfg, tid, loc);
        } catch (eVrPaneI18n) {}
      }
      pane.querySelectorAll('.summary-total-item[data-pg-summary-key]').forEach(function (el) {
        var key = el.getAttribute('data-pg-summary-key');
        if (!key) return;
        var raw = el.textContent || '';
        var colon = raw.indexOf(':');
        var rest = colon >= 0 ? raw.slice(colon + 1).trim() : '';
        el.textContent = formatSummaryLine(key, rest);
      });
      pane.querySelectorAll('.pay-follow[data-pg-pay-follow]').forEach(function (el) {
        var act = el.getAttribute('data-pg-pay-follow');
        if (!act) return;
        var row = PAY_FOLLOW[act];
        if (row) el.textContent = lblText(row, loc, el.textContent);
      });
      var hello = pane.querySelector('#viewSettingHelloBtn_' + tid);
      if (hello) hello.textContent = lblText(UI.helloBtn, loc, hello.textContent);
      if (cfg && cfg.columns) {
        pane.querySelectorAll('.column-guide-item').forEach(function (item) {
          var cb = item.querySelector('.column-guide-check');
          var sp = item.querySelector('.column-guide-label');
          var k = cb ? cb.getAttribute('data-key') : '';
          if (!k || !sp) return;
          var col = cfg.columns.filter(function (cc) { return cc && cc.key === k; })[0];
          if (col && col.label) {
            sp.textContent = w.PG_UI_I18N && typeof w.PG_UI_I18N.t === 'function' ? w.PG_UI_I18N.t(String(col.label)) : col.label;
          }
        });
      }
      if (url === '/hq/pgApiMng' && pane._lastGridCols && pane._lastGridCols.length) {
        var buildTh2 = w.PG_SCREENS && w.PG_SCREENS.buildStandardDataGridTheadHtml;
        if (typeof buildTh2 === 'function' && cfg) {
          var theadPg2 = pane.querySelector('#grid_' + tid + ' thead');
          if (theadPg2) {
            var selT3 = (loc === 'KO' ? UI.selectAll.KO : (UI.selectAll[loc] || UI.selectAll.EN));
            theadPg2.innerHTML = buildTh2(pane._lastGridCols, cfg.headerGroups || [], { selectAllTitle: selT3 });
            if (w.PG_TABLE_COL_RESIZE && typeof w.PG_TABLE_COL_RESIZE.refreshInSync === 'function') {
              try { w.PG_TABLE_COL_RESIZE.refreshInSync(pane); } catch (eRs2) {}
            } else if (w.PG_TABLE_COL_RESIZE && typeof w.PG_TABLE_COL_RESIZE.refreshIn === 'function') {
              try { w.PG_TABLE_COL_RESIZE.refreshIn(pane); } catch (eR3) {}
            }
          }
        }
        var tbOnly = pane.querySelector('#grid_' + tid + ' tbody');
        if (tbOnly && w.PG_UI_I18N && typeof w.PG_UI_I18N.applyDom === 'function') {
          try { w.PG_UI_I18N.applyDom(tbOnly); } catch (eTbD) {}
        }
      }
      if (w.PG_UI_I18N && typeof w.PG_UI_I18N.applyDom === 'function') {
        try {
          var pr0 = pane.querySelector('.pagination-row');
          if (pr0) w.PG_UI_I18N.applyDom(pr0);
        } catch (ePagUi) {}
      }
      if (w.PG_UI && typeof w.PG_UI.refreshPayGridStatusBadges === 'function') {
        try { w.PG_UI.refreshPayGridStatusBadges(pane); } catch (ePayBadge) {}
      }
    });
    if (typeof w.PG_refreshPayListAggregateBarsDom === 'function') {
      try { w.PG_refreshPayListAggregateBarsDom(); } catch (eAgg) {}
    }
    document.querySelectorAll('.tab-pane.tabConDiv[formurl]').forEach(function (pane) {
      if (w.PG_UI_I18N && typeof w.PG_UI_I18N.syncDateInputLangUnder === 'function') {
        try { w.PG_UI_I18N.syncDateInputLangUnder(pane); } catch (ePanDt) {}
      }
    });
  }

  function normalizeLocale(x) {
    var u = String(x || '').toUpperCase().trim();
    if (u === 'JA') return 'JP';
    if (u === 'ZH' || u === 'ZH-CN' || u === 'ZH_CN') return 'CH';
    if (LOCALES.indexOf(u) !== -1) return u;
    return 'KO';
  }

  function getLocale() {
    try {
      var s = localStorage.getItem(STORAGE_KEY);
      return normalizeLocale(s || 'KO');
    } catch (e) {
      return 'KO';
    }
  }

  function formatSummaryLine(key, text) {
    var loc = getLocale();
    if (loc === 'KO') return key + ': ' + text;
    var row = SUMMARY_LBL[key];
    var lab = row ? (row[loc] || row.EN || key) : key;
    return lab + ': ' + text;
  }

  function summaryCountPrefix() {
    var loc = getLocale();
    if (loc === 'KO') return '건수: ';
    var row = SUMMARY_LBL['건수'];
    return (row && (row[loc] || row.EN) ? (row[loc] || row.EN) : '건수') + ': ';
  }

  function refreshDailyDetailCountLabel(countEl) {
    if (!countEl) return;
    var raw = countEl.getAttribute('data-pg-count');
    if (raw == null || raw === '') return;
    countEl.textContent = summaryCountPrefix() + raw;
  }

  function optionAllText() {
    var loc = getLocale();
    return OPT0[loc] || OPT0.KO;
  }

  function payFollowLabel(act) {
    var loc = getLocale();
    var row = PAY_FOLLOW[act];
    if (!row) return '';
    return loc === 'KO' ? row.KO : (row[loc] || row.EN || row.KO);
  }

  function setLocale(loc, opts) {
    loc = normalizeLocale(loc);
    opts = opts || {};
    try {
      localStorage.setItem(STORAGE_KEY, loc);
    } catch (e1) {}
    // 사용자가 명시적으로 선택한 로케일만 "사용자 설정"으로 간주
    if (!opts.silent) {
      try { localStorage.setItem(USER_SET_KEY, '1'); } catch (e1u) {}
    }
    try {
      if (typeof document !== 'undefined' && document.documentElement) {
        var langMap = { KO: 'ko', EN: 'en', JP: 'ja', CH: 'zh-Hans', TH: 'th' };
        document.documentElement.setAttribute('lang', langMap[loc] || 'ko');
      }
    } catch (eLang) {}
    applyCatalogLocale(loc);
    if (w.PG_SCREENS && typeof w.PG_SCREENS.syncPayListIntegratedScreenLabelsFromCatalog === 'function') {
      w.PG_SCREENS.syncPayListIntegratedScreenLabelsFromCatalog();
    }
    applyFeeListScreenLocale(loc);
    applyChillListCatalogLocale(loc);
    applyMenuScreensSearchAndNoticesLocale(loc);
    applyScreenChromeLocale(loc);
    patchCalcCycleSearchOptionAll(loc);
    refreshOpenPayListTheads(loc);
    refreshOpenPayListPaneChrome(loc);
    refreshOpenPayMngDomI18n(loc);
    if (!opts.silent) {
      try {
        w.dispatchEvent(new CustomEvent('pg-pay-list-locale-changed', { detail: { locale: loc } }));
      } catch (e2) {}
    }
    if (w.PG_ADMIN_SHELL_I18N && typeof w.PG_ADMIN_SHELL_I18N.apply === 'function') {
      try {
        w.PG_ADMIN_SHELL_I18N.apply(loc);
      } catch (eSh) {}
    }
    if (w.PG_APP_REFRESH_LOCALE_PANES && typeof w.PG_APP_REFRESH_LOCALE_PANES === 'function') {
      try {
        w.PG_APP_REFRESH_LOCALE_PANES();
      } catch (eRf) {}
    }
    var recvModal = typeof document !== 'undefined' ? document.getElementById('pgReceivableRegModal') : null;
    if (recvModal && w.PG_UI_I18N && typeof w.PG_UI_I18N.applyDom === 'function') {
      try { w.PG_UI_I18N.applyDom(recvModal); } catch (eRecvLoc) {}
    }
    var pgAgModal = typeof document !== 'undefined' ? document.getElementById('pgAgencyEditModal') : null;
    if (pgAgModal && w.PG_UI_I18N && typeof w.PG_UI_I18N.applyDom === 'function') {
      try { w.PG_UI_I18N.applyDom(pgAgModal); } catch (ePgAgLoc) {}
    }
    updateLangDropdownUi(loc);
  }

  function updateLangDropdownUi(loc) {
    loc = normalizeLocale(loc);
    var root = document.getElementById('pgPayListLangDropdown');
    if (!root) return;
    root.querySelectorAll('[data-pg-pay-locale]').forEach(function (el) {
      var v = normalizeLocale(el.getAttribute('data-pg-pay-locale'));
      el.classList.toggle('pg-pay-lang-chip--active', v === loc);
    });
  }

  function bindLangDropdownOnce() {
    var root = document.getElementById('pgPayListLangDropdown');
    if (!root || root._pgI18nBound) return;
    root._pgI18nBound = true;
    root.querySelectorAll('[data-pg-pay-locale]').forEach(function (el) {
      el.addEventListener('click', function (e) {
        e.preventDefault();
        var v = normalizeLocale(el.getAttribute('data-pg-pay-locale'));
        setLocale(v, {});
      });
      el.addEventListener('keydown', function (e) {
        if (e.key !== 'Enter' && e.key !== ' ') return;
        e.preventDefault();
        var v2 = normalizeLocale(el.getAttribute('data-pg-pay-locale'));
        setLocale(v2, {});
      });
    });
  }

  function init() {
    bindLangDropdownOnce();
    ensureCatalogKoSnapshot();
    ensureScreenSnap();
    var loc = getLocale();
    setLocale(loc, { silent: true });
  }

  w.PG_PAY_LIST_I18N = {
    LOCALES: LOCALES.slice(),
    STORAGE_KEY: STORAGE_KEY,
    getLocale: getLocale,
    setLocale: setLocale,
    init: init,
    formatSummaryLine: formatSummaryLine,
    summaryCountPrefix: summaryCountPrefix,
    refreshDailyDetailCountLabel: refreshDailyDetailCountLabel,
    optionAllText: optionAllText,
    payFollowLabel: payFollowLabel,
    refreshOpenPayMngDomI18n: refreshOpenPayMngDomI18n,
    refreshOpenPayListPaneChrome: refreshOpenPayListPaneChrome,
    isPayMngDomPaneUrl: isPayMngDomPaneUrl,
    formatVerifyReportDayNote: formatVerifyReportDayNote
  };
})(typeof window !== 'undefined' ? window : globalThis);
