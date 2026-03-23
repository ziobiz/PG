/**
 * PG 솔루션 - 23개 메뉴별 화면 HTML 템플릿 (fxhj 구조: 검색폼 + 요약 + 버튼 + 그리드 + 페이지네이션)
 */
(function () {
  'use strict';

  /** 정산주기 저장값(v)·화면표시(t) — 가맹점 정산방법·결제내역 검색 공통 */
  var CALC_CYCLE_OPTIONS = [
    { v: '', t: '선택' },
    { v: 'RT', t: '실시간' },
    { v: 'M5', t: '5분' },
    { v: 'M10', t: '10분' },
    { v: 'H1', t: '1시간' },
    { v: 'H2', t: '2시간' },
    { v: 'H4', t: '4시간' },
    { v: 'D1', t: 'D+1' },
    { v: 'D2', t: 'D+2' },
    { v: 'D3', t: 'D+3' },
    { v: 'D5', t: 'D+5' },
    { v: 'D7', t: 'D+7' },
    { v: 'D10', t: 'D+10' },
    { v: 'D15', t: 'D+15' },
    { v: 'D20', t: 'D+20' },
    { v: 'D30', t: 'D+30' },
    { v: 'W3', t: 'W+3' },
    { v: 'W5', t: 'W+5' },
    { v: 'W7', t: 'W+7' },
    { v: 'W10', t: 'W+10' },
    { v: 'W14', t: 'W+14' },
    { v: 'WEEKLY', t: 'Weekly' },
    { v: 'WEEKLY2', t: 'Weekly2' }
  ];
  var CALC_CYCLE_SEARCH_OPTIONS = [{ v: '', t: '전체' }].concat(CALC_CYCLE_OPTIONS.filter(function (o) { return o.v !== ''; }));

  /** 정산구분: 정산 마감 후 개시 방식 (수동/자동/펌뱅킹) */
  var CALC_PROC_OPTIONS = [
    { v: 'MANUAL', t: '수동' },
    { v: 'AUTO', t: '자동' },
    { v: 'FUMBANKING', t: '펌뱅킹' }
  ];
  /** 이체및송금구분: 펌뱅킹 연동 시 이체 실행 (수동/자동/사용안함) */
  var TRANSFER_REMIT_OPTIONS = [
    { v: 'MANUAL', t: '수동' },
    { v: 'AUTO', t: '자동' },
    { v: 'NONE', t: '사용안함' }
  ];

  /** 본사 영업일·휴일: 연간 미니달력 + 공휴일 프리셋 (hq-holiday-calendar.js) */
  var HQ_HOLIDAY_UI_HTML = '<div class="col-12"><div class="hq-holiday-ui-wrap border rounded p-2 bg-light mt-1">' +
    '<div class="d-flex flex-wrap align-items-center gap-2 mb-2">' +
    '<label class="small mb-0 text-nowrap">연도</label><select class="form-select form-select-sm hq-holiday-year" style="width:auto;min-width:5rem"></select>' +
    '<button type="button" class="btn btn-sm btn-outline-primary hq-holiday-load-presets">4국 공휴일 불러오기</button>' +
    '<button type="button" class="btn btn-sm btn-outline-secondary hq-holiday-refresh">달력 동기화</button></div>' +
    '<p class="text-muted small mb-2">날짜를 클릭하면 비영업일에서 추가/제거됩니다. [4국 공휴일 불러오기]는 위 국가 코드에 맞춰 아래 목록에 병합합니다.</p>' +
    '<div class="hq-holiday-calendar-grid"></div></div></div>';

  /** 본사 영업일·휴일: 기간형 추가 목록(언제부터~언제까지/내용/추가일/작성자) */
  var REGIONAL_BIZDAY_RANGE_UI_HTML = '<div class="col-12"><div class="border rounded p-2 bg-light mt-1">' +
    '<div class="d-flex flex-wrap align-items-end gap-2 mb-2">' +
    '<div><label class="form-label mb-1">언제부터</label><input type="date" class="form-control form-control-sm" id="bizHolidayFromDate"></div>' +
    '<div><label class="form-label mb-1">언제까지</label><input type="date" class="form-control form-control-sm" id="bizHolidayToDate"></div>' +
    '<div style="min-width:220px"><label class="form-label mb-1">내용</label><input type="text" class="form-control form-control-sm" id="bizHolidayReason" placeholder="예: 설 연휴"></div>' +
    '<div><label class="form-label mb-1">작성자</label><input type="text" class="form-control form-control-sm" id="bizHolidayWriter" placeholder="작성자"></div>' +
    '<div><button type="button" class="btn btn-sm btn-primary" id="bizHolidayAddBtn">추가</button></div>' +
    '</div>' +
    '<div class="table-responsive"><table class="table table-sm table-bordered mb-0"><thead><tr><th style="width:120px">언제부터</th><th style="width:120px">언제까지</th><th>내용</th><th style="width:130px">추가한날짜</th><th style="width:120px">작성자</th><th style="width:170px">처리</th></tr></thead>' +
    '<tbody id="bizHolidayRangeTbody"><tr><td colspan="6" class="text-muted text-center">추가된 기간이 없습니다.</td></tr></tbody></table></div>' +
    '<input type="hidden" name="businessHolidayRangesJson" id="businessHolidayRangesJson">' +
    '</div></div>';

  /** 본사설정 > 영업일설정: 등록된 설정 목록 */
  var HQ_BIZDAY_PROFILE_LIST_HTML = '<div class="col-12"><div class="border rounded p-2 bg-light mt-1">' +
    '<div class="d-flex justify-content-between align-items-center mb-2"><strong>저장된 영업일 설정 목록</strong><small class="text-muted">목록에서 행을 선택하면 상단 편집영역으로 불러옵니다.</small></div>' +
    '<div class="table-responsive"><table class="table table-sm table-bordered mb-0"><thead><tr><th style="width:64px">번호</th><th style="width:220px">이름</th><th style="width:140px">기준국가</th><th>추가 리스트(휴일) 건수</th><th style="width:130px">수정일</th></tr></thead>' +
    '<tbody id="hqBizdayProfileTbody"><tr><td colspan="5" class="text-center text-muted">저장된 설정이 없습니다.</td></tr></tbody></table></div>' +
    '</div></div>';

  var MENU_SCREENS = {
    '/hq/pgApiMng': {
      emptyMessage: '조회된 데이터가 없습니다.',
      searchRows: [[
        { label: 'PG사명', type: 'text', name: 'searchPgNm' },
        { label: '사용여부', type: 'select', name: 'searchUseYn', options: [{ v: '', t: '전체' }, { v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }] },
        { type: 'searchBtn', label: '검색' }
      ]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'hqPgApiAddBtn', label: 'PG사 연동 추가', cls: 'btn-success' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'pgNm', label: '업체명' }, { key: 'pgCd', label: '업체코드' }, { key: 'apiEndpoint', label: 'API 엔드포인트' }, { key: 'useYn', label: '사용여부' }, { key: 'regDt', label: '등록일' }]
    },
    '/hq/defaultCommission': {
      isForm: true,
      formSections: [
        {
          title: '기본정책',
          notice: '정책 템플릿(A/B/C/D...)을 만들고 이름을 바꾼 뒤 배포할 수 있습니다. 배포된 정책은 가맹점 생성 시 [본사정책 따름]을 선택하면 자동 부여됩니다.',
          rows: [
            [{ label: '정책코드', type: 'select', name: 'templateScope', options: [{ v: 'HQPOL:A', t: 'A' }, { v: 'HQPOL:B', t: 'B' }, { v: 'HQPOL:C', t: 'C' }, { v: 'HQPOL:D', t: 'D' }], col: 2 }, { label: '정책명', type: 'text', name: 'policyName', col: 2, placeholder: '예: 기본정책 A' }, { label: '배포', type: 'select', name: 'deployYn', options: [{ v: 'Y', t: '배포' }, { v: 'N', t: '미배포' }], col: 2 }],
            [{ label: '실패수수료', type: 'text', name: 'failFee', col: 2 }, { label: '이용수수료', type: 'text', name: 'usageRate', col: 2 }, { label: '결제 수수료', type: 'text', name: 'payRate', col: 2 }, { label: '취소 수수료', type: 'text', name: 'cancelRate', col: 2 }, { label: '환불 수수료', type: 'text', name: 'refundRate', col: 2 }, { label: '건당 수수료', type: 'text', name: 'perTxFee', col: 2 }],
            [{ label: '정산수수료', type: 'text', name: 'feeSettlementPerTx', col: 2 }, { label: 'USDT수수료', type: 'text', name: 'feeUsdt', col: 2 }, { label: 'FX수수료', type: 'text', name: 'feeFx', col: 2 }, { label: '롤링(담보금) 비율%', type: 'text', name: 'rollingPct', col: 2, placeholder: '5 또는 10' }, { label: '롤링보류일수', type: 'text', name: 'rollingDays', col: 2, placeholder: '120 또는 180' }, { label: '비고', type: 'text', name: 'memo', col: 2 }]
          ]
        }
      ],
      buttons: [
        { id: 'hqDefaultCommissionTemplateAddBtn', label: '정책 추가', cls: 'btn-success' },
        { id: 'hqDefaultCommissionTemplateDeleteBtn', label: '정책 삭제', cls: 'btn-outline-danger' },
        { id: 'hqDefaultCommissionSaveBtn', label: '저장', cls: 'btn-primary' }
      ]
    },
    '/hq/businessDaySetting': {
      isForm: true,
      formSections: [
        {
          title: '영업일 설정',
          notice: '4개국(KR/US/JP/TH) 기준으로 이름별 영업일 설정을 저장합니다. 업체등록의 본사 설정에서 이 이름을 선택하면 해당 국가/비영업일 목록이 적용됩니다.',
          rows: [
            [{ label: '이름', type: 'text', name: 'hqBizdayProfileName', col: 3, placeholder: '예: KR 기본 영업일' },
             { label: '기준국가선택', type: 'select', name: 'holidayCountryCodes', options: [{ v: 'KR', t: 'KR (대한민국)' }, { v: 'US', t: 'US (미국)' }, { v: 'JP', t: 'JP (일본)' }, { v: 'TH', t: 'TH (태국)' }], col: 2 },
             { label: '선택ID', type: 'text', name: 'hqBizdayProfileId', col: 2, readonly: true }],
            [{ label: '추가 리스트 (한 줄에 YYYY-MM-DD 하나)', type: 'textarea', name: 'businessHolidayExtraDates', col: 12, rows: 5, placeholder: 'YYYY-MM-DD 한 줄에 하나' }],
            [{ type: 'customHtml', html: HQ_HOLIDAY_UI_HTML, col: 12 }],
            [{ type: 'customHtml', html: HQ_BIZDAY_PROFILE_LIST_HTML, col: 12 }]
          ]
        }
      ],
      buttons: [
        { id: 'hqBizdayProfileNewBtn', label: '신규', cls: 'btn-outline-secondary' },
        { id: 'hqBizdayProfileDeleteBtn', label: '삭제', cls: 'btn-outline-danger' },
        { id: 'hqBizdayProfileSaveBtn', label: '저장', cls: 'btn-primary' }
      ]
    },
    '/hq/notifyEnv': {
      isForm: true,
      formSections: [
        {
          title: '전산 노티 수신 (NOTI 전산노티대상 연동)',
          notice: '아래 URL을 ziobiz/NOTI 전산노티대상 설정에 등록하세요. 경로 끝 토큰으로 무단 호출을 막습니다. 운영 배포 후 [공개 URL 베이스]에 https://실제도메인 을 넣으면 안내 URL이 고정됩니다.',
          rows: [
            [{ label: '노티 수신 URL', type: 'text', name: 'notifyIngressUrl', col: 6, readonly: true }],
            [{ label: 'Ingress 토큰(참고)', type: 'text', name: 'ingressToken', col: 6, readonly: true }],
            [{ label: '공개 URL 베이스', type: 'text', name: 'publicBaseUrl', col: 6, placeholder: '비우면 브라우저 접속 기준(예: http://localhost:8080)' }],
            [{ label: '노티 성공 응답 본문', type: 'textarea', name: 'notifyOkResponse', col: 6, placeholder: '{"result":"OK"}' }]
          ]
        },
        {
          title: '결제 후속조치 (NOTI 환경설정 대응)',
          notice: '각 기능을 Y로 켠 경우에만 결제내역 그리드의 후속조치 버튼이 API에서 허용됩니다. 자동무효 배치 등은 추후 연동합니다.',
          rows: [
            [{ label: '자동무효', type: 'select', name: 'autoVoidYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 2 }, { label: '이메일무효', type: 'select', name: 'emailVoidYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 2 }, { label: '자동환불', type: 'select', name: 'autoRefundYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 2 }, { label: '강제환불', type: 'select', name: 'forceRefundYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 2 }],
            [{ label: '자동무효 기준(시간, 예정)', type: 'text', name: 'autoVoidAfterHours', col: 2, placeholder: '예: 24' }]
          ]
        },
        {
          title: '로그인·OTP 정책 (ziobiz/NOTI 계정관리 대응)',
          notice: '모든 사용자에 OTP를 요구할지 본사(총본사) 설정에서 통일합니다. OTP 필수 시 로그인·등록 단계에서 OTP 검증을 붙일 수 있습니다(연동 예정). 사용자관리 그리드의 OTP 등록 여부와 연계됩니다.',
          rows: [
            [{ label: 'OTP 사용 필수', type: 'select', name: 'otpRequiredYn', options: [{ v: 'Y', t: '예 (전 사용자)' }, { v: 'N', t: '아니오' }], col: 2 },
             { label: 'OTP 형식 정책', type: 'select', name: 'otpPolicyMode', options: [{ v: 'NOTI', t: 'NOTI 동일' }, { v: 'CUSTOM', t: '커스텀' }], col: 2 },
             { label: '비밀번호 정책', type: 'select', name: 'passwordPolicyMode', options: [{ v: 'NOTI', t: 'NOTI 동일' }, { v: 'CUSTOM', t: '커스텀' }], col: 2 },
             { label: '비밀번호찾기 기능', type: 'select', name: 'forgotPasswordEnabledYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 2 }],
            [{ label: '관리담당 사용자관리 권한', type: 'select', name: 'managerUserControlEnabledYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 2 },
             { label: '관리담당 비밀번호 초기화', type: 'select', name: 'managerPasswordResetEnabledYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 2 }]
          ]
        },
        {
          title: '총판 노티 대상 생성',
          notice: '여기서 생성한 URL을 ziobiz/NOTI 전산노티대상에 등록한 뒤, 총판 상세정보에서 선택하여 연결합니다.',
          rows: [
            [{ label: '노티 대상명', type: 'text', name: 'newNotifyTargetName', col: 3, placeholder: '예: 총판A 수신' },
             { label: '생성된 URL', type: 'text', name: 'newNotifyTargetUrl', col: 6, readonly: true },
             { label: '생성', type: 'text', name: 'newNotifyTargetAction', col: 3, readonly: true, button: '노티 생성' }],
            [{ label: '삭제 대상', type: 'select', name: 'deleteNotifyTargetId', col: 3, loadNotifyTargets: true },
             { label: '삭제', type: 'text', name: 'deleteNotifyTargetAction', col: 3, readonly: true, button: '선택 삭제' }]
          ]
        }
      ],
      buttons: [{ id: 'hqNotifyRegenTokenBtn', label: '토큰 재발급', cls: 'btn-warning' }, { id: 'hqNotifyEnvSaveBtn', label: '저장', cls: 'btn-primary' }]
    },
    '/hq/notifyMapping': {
      isForm: true,
      formHtmlId: 'hqNotifyMappingForm',
      formSections: [
        {
          title: '노티매핑설정',
          notice: '각 PG사가 CALLBACK·RESULT(및 Background 등)로 넘기는 노티 파라미터를, 전산의 어느 화면(URL)과 그리드/필드(internalKey)에 반영할지 정의합니다. 결제대행사가 추가되면 JSON의 vendors 배열에 동일 구조로 항목을 추가하면 됩니다. 실제 수신 파싱·저장 로직은 이 정의를 참조해 단계적으로 연동합니다.',
          rows: [
            [{ label: '매핑 정의 (JSON)', type: 'textarea', name: 'mappingDefinitionJson', col: 6, rows: 22, placeholder: '{ "version": 1, "vendors": [ ... ] }' }],
            [{ label: '최종 수정일시', type: 'text', name: 'updatedAt', col: 3, readonly: true }]
          ]
        },
        {
          title: '구조 안내',
          notice: 'vendorCode·vendorName: PG 식별. channels: CALLBACK(서버 노티), RESULT(브라우저 리다이렉트) 등. targetPageUrl·targetPageLabel: 전산 메뉴 경로. fieldMappings: pgField(피지사 파라미터명) → internalKey(결제내역 그리드 키 등). 상세는 저장소 docs/노티매핑설정.md 를 참고하세요.',
          rows: []
        }
      ],
      buttons: [{ id: 'hqNotifyMappingSaveBtn', label: '저장', cls: 'btn-primary' }]
    },
    '/hq/orgViewColumnAllowance': {
      isForm: true,
      formHtmlId: 'hqOrgViewColumnAllowanceForm',
      formSections: [
        {
          title: '본사별 VIEW SETTING 노출설정',
          notice: '총본사가 각 본사(REGIONAL)마다, 화면별 그리드에서 VIEW SETTING으로 노출·선택 가능한 열을 지정합니다. 여기서 체크한 항목만 해당 본사 및 그 하위 조직(총판·가맹점 등) 사용자가 개인 VIEW SETTING에서 켜고 끌 수 있습니다. 이 설정이 없으면(불러오기 시 정책 없음) 전 항목 선택 가능합니다. 고정 열(번호·업체명·거래일·Route No 등)은 항상 표시되며 여기 목록에 나오지 않습니다.',
          rows: [
            [{ label: '설정 대상 본사', type: 'select', name: 'regionalOrgCode', col: 4, options: [{ v: '', t: '선택' }], loadRegionalBranches: true }],
            [{ label: '설정 대상 화면', type: 'select', name: 'targetPageUrl', col: 4, options: [
              { v: '/calc/payList', t: '결제내역(통합)' },
              { v: '/comp/compMngTree', t: '업체관리' }
            ] }],
            [{ type: 'customHtml', col: 12, html: '<div class="mb-2"><span class="form-label d-block">이 본사에 노출할 열 (VIEW SETTING에서 선택 가능)</span><div id="hqOrgAllowColumnChecks" class="column-guide-list border rounded p-2 bg-light"></div><p class="text-muted small mb-0 mt-1">체크한 열만 하위 사용자 화면의 VIEW SETTING에 나타납니다.</p></div>' }]
          ]
        }
      ],
      buttons: [
        { id: 'hqOrgAllowLoadBtn', label: '불러오기', cls: 'btn-outline-secondary' },
        { id: 'hqOrgAllowSaveBtn', label: '노출 항목 저장', cls: 'btn-primary' },
        { id: 'hqOrgAllowDeleteBtn', label: '노출 제한 해제', cls: 'btn-outline-danger' }
      ]
    },
    '/hq/apiConfig': {
      isForm: true,
      formSections: [
        {
          title: 'API 구성 세팅',
          notice: '여러 PG사 API 연동 후, 가맹점에게 발급하는 통합 API 기본 구성입니다.',
          rows: [
            [{ label: 'API 기본 URL', type: 'text', name: 'baseUrl', col: 6, placeholder: 'https://api.example.com/v1' }],
            [{ label: '인증방식', type: 'select', name: 'authType', options: [{ v: 'API_KEY', t: 'API Key' }, { v: 'Bearer', t: 'Bearer Token' }, { v: 'BASIC', t: 'Basic' }], col: 2 }, { label: '타임아웃(초)', type: 'text', name: 'timeoutSec', col: 2 }],
            [{ label: '비고', type: 'textarea', name: 'memo', col: 6 }]
          ]
        },
        {
          title: 'ChillPay (칠리페이) 연동',
          notice: 'DirectCredit 결제 API 연동 설정. ChillPay 가맹점 등록 후 발급받은 API Key, MD5 Key를 입력하세요.',
          rows: [
            [{ label: 'Merchant Code', type: 'text', name: 'chillpayMerchantCode', col: 2, placeholder: 'M035594' }, { label: 'API Key', type: 'text', name: 'chillpayApiKey', col: 4, placeholder: 'ChillPay에서 발급' }],
            [{ label: 'MD5 Secret Key', type: 'text', name: 'chillpayMd5Key', col: 4, placeholder: 'CheckSum 생성용' }, { label: 'Route No', type: 'text', name: 'chillpayRouteNo', col: 1, placeholder: '4' }, { label: 'Sandbox', type: 'select', name: 'chillpaySandbox', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '운영' }], col: 1 }]
          ]
        },
        {
          title: '정산/환수 정책',
          notice: '환수금 처리 시 수수료 포함 여부와 정산 VAT 부과 여부를 본사 정책으로 설정합니다.',
          rows: [
            [
              { label: '환수금 수수료 포함', type: 'select', name: 'recallIncludeFeeYn', options: [{ v: 'Y', t: '포함' }, { v: 'N', t: '제외' }], col: 2 },
              { label: '정산 VAT 부과', type: 'select', name: 'settlementVatApplyYn', options: [{ v: 'Y', t: '부과' }, { v: 'N', t: '미부과' }], col: 2 }
            ]
          ]
        }
      ],
      buttons: [{ id: 'hqApiConfigSaveBtn', label: '저장', cls: 'btn-primary' }]
    },
    '/hq/permissionMng': {
      emptyMessage: '조회된 데이터가 없습니다.',
      searchRows: [[
        { label: '본사명', type: 'text', name: 'searchHqNm' },
        { label: '메뉴ID', type: 'text', name: 'searchMenuId' },
        { type: 'searchBtn', label: '검색' }
      ]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'hqPermissionSaveBtn', label: '권한 저장', cls: 'btn-primary' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'hqNm', label: '업체명' }, { key: 'hqCd', label: '업체코드' }, { key: 'menuId', label: '메뉴ID' }, { key: 'menuNm', label: '메뉴명' }, { key: 'accessYn', label: '접근허용' }, { key: 'regDt', label: '적용일' }]
    },
    '/hq/accountMng': {
      emptyMessage: '등록된 업체별 접근 규칙이 없습니다.',
      noticeList: [
        'ziobiz/NOTI의 계정관리에서 하던 것처럼, 로그인 ID(사용자)별로 접근 가능한 업체코드(본사·총판·가맹점 등)를 지정합니다.',
        '사용자관리 화면의 권한그룹은 [본사별 권한 세팅] 메뉴와 동일한 정책을 사용합니다. OTP 필수 여부는 [전산노티·결제환경]의 로그인·OTP 정책에서 설정합니다.'
      ],
      searchRows: [[{ type: 'searchBtn', label: '새로고침' }]],
      summary: ['건수'],
      buttons: [
        { id: 'searchBtn', label: '새로고침', cls: 'btn-primary' },
        { id: 'hqAccountAccessAddBtn', label: '접근권한 추가', cls: 'btn-success' }
      ],
      columns: [
        { key: '_chk', type: 'checkbox' },
        { key: 'rowNo', label: '번호' },
        { key: 'compNm', label: '업체명' },
        { key: 'compCode', label: '업체코드' },
        { key: 'username', label: '사용자ID' },
        { key: 'regDt', label: '등록일시' },
        { key: 'id', type: 'accountAccessDelete', label: '삭제' }
      ]
    },
    '/system/noticeList': {
      emptyMessage: '조회된 데이터가 없습니다.',
      searchRows: [
        [
          { label: '제목', type: 'text', name: 'searchTitle' },
          { label: '작성일', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate' },
          { type: 'searchBtn', label: 'Q 검색' }
        ]
      ],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', label: '', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'title', label: '제목' }, { key: 'regDt', label: '작성일' }, { key: 'hitCnt', label: '조회수' }]
    },
    '/comp/myCompMng': {
      hideListGrid: true,
      searchRows: [],
      summary: [],
      buttons: [],
      columns: [],
      hasCompInfoDetailForm: true,
      compInfoDetailFormSections: [
        {
          title: '본사 정보 상세',
          notice: '로그인한 계정에 연결된 업체(총본사·본사·총판·가맹점 등) 정보가 자동으로 표시됩니다. 아래에서 조회·수정합니다.',
          rows: [
            [{ label: '업체코드', type: 'text', name: 'compId', col: 2, readonly: true }, { label: '업체구분', type: 'select', name: 'compDiv', options: [{ v: '', t: '선택' }, { v: 'HEADQUARTERS', t: '총본사' }, { v: 'REGIONAL', t: '본사' }, { v: 'MASTER_DIST', t: '총판' }, { v: 'BRANCH', t: '지사' }, { v: 'AGENCY', t: '대리점' }, { v: 'SALES_OFFICE', t: '영업점' }, { v: 'MERCHANT', t: '가맹점' }], col: 2 }, { label: '업체명', type: 'text', name: 'compNm', col: 2 }, { label: '사업자번호', type: 'regNoWithType', name: 'regNo', col: 2 }, { label: '업태', type: 'text', name: 'bizType', col: 2 }, { label: '종목', type: 'text', name: 'industry', col: 2 }],
            [{ label: '대표자명', type: 'text', name: 'ceoNm', col: 2 }, { label: '휴대폰', type: 'text', name: 'ceoMobile', col: 2 }, { label: '업체전화', type: 'text', name: 'compTel', col: 2 }, { label: '팩스', type: 'text', name: 'fax', col: 2 }, { label: '이메일', type: 'text', name: 'email', col: 2 }, { label: '비고', type: 'text', name: 'remark', col: 2 }],
            [{ type: 'countryAddressRow', zipLabel: '우편번호', addrLabel: '주소', addrDetailLabel: '상세주조', addrEtcLabel: '기타' }],
            [{ label: '대표 아이디 (중복검사)', type: 'text', name: 'loginId', col: 2, button: '중복확인' }, { label: '비밀번호', type: 'passwordReset', name: 'pwdReset', col: 2 }],
            [{ label: '보조 아이디 (중복검사)', type: 'text', name: 'assistantLoginId', col: 2, button: '중복확인' }, { label: '패스워드', type: 'password', name: 'assistantPwd', col: 2 }],
            [{ label: '배경/로고 변경권한', type: 'select', name: 'brandingEditAllowedYn', options: [{ v: 'N', t: '미부여' }, { v: 'Y', t: '부여' }], col: 2 }],
            [{ label: '특이사항', type: 'textarea', name: 'siteSummary', col: 6 }]
          ]
        },
        {
          type: 'branding',
          title: '브랜딩 설정',
          id: 'brandingCard',
          notice: '총본사/본사/총판은 배경이미지·로고·테마를 설정할 수 있습니다. 본사/총판은 총본사에서 [배경/로고 변경권한]을 부여한 경우에만 수정 가능합니다.'
        },
        {
          type: 'pgInfoDisplay',
          title: '결제대행사정보',
          id: 'pgInfoCard',
          notice: '가맹점만 표시됩니다. 결제 URL은 간편결제용으로, API 연동과 별도로 가맹점 생성 시 즉시 결제 페이지를 제공합니다.'
        }
      ],
      compInfoDetailButtons: [{ id: 'compInfoUpdateBtn', label: '수정 저장', cls: 'btn-primary' }]
    },
    '/comp/compMngTree': {
      searchFormClass: 'comp-mng-search-multiline',
      searchRows: [
        [
          { label: '업체구분', type: 'select', name: 'searchCompDiv', options: [{ v: '', t: '전체' }, { v: 'REGIONAL', t: '본사' }, { v: 'MASTER_DIST', t: '총판' }, { v: 'BRANCH', t: '지사' }, { v: 'AGENCY', t: '대리점' }, { v: 'SALES_OFFICE', t: '영업점' }, { v: 'MERCHANT', t: '가맹점' }], size: 10 },
          { label: '대표자명', type: 'text', name: 'searchCeoNm', size: 12 },
          { label: '업체사용상태', type: 'select', name: 'searchUseYn', options: [{ v: '', t: '전체' }, { v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], size: 10 },
          { label: '업체코드', type: 'text', name: 'searchCompId', size: 12 },
          { label: '업체명', type: 'text', name: 'searchCompNm', size: 12 }
        ],
        [
          { label: '지급보류', type: 'select', name: 'searchPayHoldYn', options: [{ v: '', t: '전체' }, { v: 'Y', t: '보류' }, { v: 'N', t: '정상' }], size: 10 },
          { label: '터미널ID', type: 'text', name: 'searchTerminalId', size: 12 },
          { label: '휴대폰', type: 'text', name: 'searchCeoMobile', size: 12 },
          { label: '사업자번호', type: 'text', name: 'searchRegNo', size: 12 },
          { type: 'compMngSearchActions', label: '하위업체포함', checkboxName: 'searchIncludeSub', searchLabel: '검색' }
        ]
      ],
      noticeList: ['업체구분·업체사용상태는 \'전체\'로 두고 검색하세요. 데이터가 없으면 [시드 생성]을 눌러주세요.', '엑셀등록: [SAMPLE]으로 서식 있는 xlsx(헤더 색·표선·가운데 정렬)를 받아 예시 행을 수정·추가한 뒤 [엑셀등록]에 업로드하세요.'],
      noticeRefButton: { id: 'noticeRefBtn', label: '참고', cls: 'btn-success' },
      summary: ['건수'],
      buttons: [{ id: 'seedBtn', label: '시드 생성', cls: 'btn-outline-warning' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }, { id: 'excelSampleBtn', label: 'SAMPLE', cls: 'btn-outline-secondary' }, { id: 'excelRegBtn', label: '엑셀등록', cls: 'btn-outline-success' }, { id: 'compRegBtn', label: '등록', cls: 'btn-danger' }],
      tableColumnGuide: true,
      columns: [
        { key: '_chk', type: 'checkbox' },
        { key: 'rowNo', label: '번호' },
        { key: 'compNm', label: '업체명' },
        { key: 'compId', label: '업체코드' },
        { key: 'compDivNm', label: '업체구분' },
        { key: 'settlementAmt', label: '정산금' },
        { key: 'receivables', label: '미수금' },
        { key: 'regNo', label: '사업자번호' },
        { key: 'ceoNm', label: '대표자명' },
        { key: 'contact', label: '연락처' },
        { key: 'bankNm', label: '은행' },
        { key: 'accountNo', label: '계좌번호' },
        { key: 'transferFee', label: '이체수수료' },
        { key: 'calcCycle', label: '정산주기' },
        { key: 'calcProcType', label: '정산구분' },
        { key: 'transferType', label: '이체및송금' },
        { key: 'transferCycleHours', label: '이체주기(시)' },
        { key: 'calcExcludeYn', label: '정산제외' },
        { key: 'calcExcludeTarget', label: '정산제외대상' },
        { key: 'calcStartTime', label: '정산개시시간' },
        { key: 'payHoldYn', label: '지급보류' },
        { key: 'useYn', label: '업체사용상태' },
        { key: 'terminalCountTerminal', label: '터미널(단말)' },
        { key: 'terminalCountWeb', label: '터미널(웹)' },
        { key: 'regDt', label: '등록일자' }
      ],
      emptyMessage: '조회된 데이터가 없습니다.',
      tableScrollable: true
    },
    '/comp/compReg': {
      isForm: true,
      formSections: [
        {
          title: '기본정보',
          notice: '업체코드는 등록 저장 시에만 자동 부여되며(업체구분별 접두 2자리+순번 8자리), 부여 후에는 변경할 수 없습니다. 업체관리 목록에 동일 코드로 표시됩니다. 업체구분을 선택하면 해당 입력 항목이 표시됩니다. 조직 이동은 상위로만 가능하며(하위로 이동 불가), 이동 시 하위 전체가 함께 이동합니다. 사용여부 미사용 시 하위 전체 미사용, 가맹점은 상위 변경으로 개별 활성화할 수 있습니다.',
          rows: [
            [{ label: '상위 본사', type: 'text', name: 'parentComp', col: 2, button: '검색', placeholder: '상위 코드' }, { label: '업체구분*', type: 'select', name: 'compDiv', options: [{ v: '', t: '선택' }, { v: 'REGIONAL', t: '본사' }, { v: 'MASTER_DIST', t: '총판' }, { v: 'BRANCH', t: '지사' }, { v: 'AGENCY', t: '대리점' }, { v: 'SALES_OFFICE', t: '영업점' }, { v: 'MERCHANT', t: '가맹점' }], col: 1 }, { label: '업체명*', type: 'text', name: 'compNm', col: 2 }, { label: '사업자번호*', type: 'regNoWithType', name: 'regNo', col: 2 }, { label: '업태', type: 'text', name: 'bizType', col: 1 }, { label: '종목', type: 'text', name: 'industry', col: 1 }],
            [{ label: '대표자명*', type: 'text', name: 'ceoNm', col: 2 }, { label: '휴대폰*', type: 'text', name: 'ceoMobile', col: 2 }, { label: '업체전화*', type: 'text', name: 'compTel', col: 2 }, { label: '팩스', type: 'text', name: 'fax', col: 2 }, { label: '이메일', type: 'text', name: 'email', col: 2 }, { label: '비고', type: 'text', name: 'remark', col: 2 }],
            [{ type: 'countryAddressRow', zipLabel: '우편번호*', addrLabel: '주소*', addrDetailLabel: '상세주소', addrEtcLabel: '기타' }],
            [{ label: '사용여부*', type: 'select', name: 'useYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 1 }, { label: '로그인ID*', type: 'text', name: 'loginId', col: 2, button: '중복확인' }, { label: '비밀번호*', type: 'password', name: 'pwd', col: 2 }]
          ]
        },
        {
          title: '본사 설정 (환기준)',
          id: 'regionalExtraCard',
          regionalOnly: true,
          notice: '총본사 로그인 시에만 본사를 추가할 수 있습니다. 본사는 기준 화폐를 최대 3가지까지 지정할 수 있습니다.',
          rows: [
            [{ label: '기준 화폐1*', type: 'select', name: 'baseCurrency1', options: [{ v: '', t: '선택' }, { v: 'KRW', t: 'KRW (원)' }, { v: 'USD', t: 'USD (달러)' }, { v: 'JPY', t: 'JPY (엔)' }, { v: 'THB', t: 'THB (바트)' }, { v: 'EUR', t: 'EUR (유로)' }], col: 2 }, { label: '기준 화폐2', type: 'select', name: 'baseCurrency2', options: [{ v: '', t: '선택' }, { v: 'KRW', t: 'KRW (원)' }, { v: 'USD', t: 'USD (달러)' }, { v: 'JPY', t: 'JPY (엔)' }, { v: 'THB', t: 'THB (바트)' }, { v: 'EUR', t: 'EUR (유로)' }], col: 2 }, { label: '기준 화폐3', type: 'select', name: 'baseCurrency3', options: [{ v: '', t: '선택' }, { v: 'KRW', t: 'KRW (원)' }, { v: 'USD', t: 'USD (달러)' }, { v: 'JPY', t: 'JPY (엔)' }, { v: 'THB', t: 'THB (바트)' }, { v: 'EUR', t: 'EUR (유로)' }], col: 2 }, { label: '사업자형태', type: 'text', name: 'bizNature', col: 2 }, { label: '취급물품', type: 'text', name: 'product', col: 2 }, { label: '대표사이트', type: 'text', name: 'homepage', col: 2, placeholder: 'https://' }],
            [{ label: '정산담당자명', type: 'text', name: 'settleName', col: 2 }, { label: '정산담당자연락처', type: 'text', name: 'settleTelNo', col: 2, placeholder: '010-0000-0000' }, { label: '송금자명(입금시)', type: 'text', name: 'remitterName', col: 2, placeholder: '입금 시 송금자명' }]
          ]
        },
        {
          title: '영업일 · 휴일 (본사)',
          id: 'regionalBusinessHolidayCard',
          regionalOrMasterDistOnly: true,
          notice: '영업일 상세는 [본사설정 > 영업일설정]에서 관리합니다. 여기서는 적용할 설정 이름을 선택하세요.',
          rows: [
            [{ label: '영업일 설정 이름', type: 'select', name: 'holidayProfileName', options: [{ v: '', t: '선택' }], col: 3 }],
            [{ label: '기준국가', type: 'text', name: 'holidayProfileCountry', col: 2, readonly: true }],
            [{ type: 'customHtml', html: '<input type="hidden" name="holidayCountryCode"><input type="hidden" name="holidayCountryCodes"><input type="hidden" name="businessHolidayExtraDates"><input type="hidden" name="businessHolidayRangesJson">', col: 12 }]
          ]
        },
        {
          title: '본사 업체 상세 정보',
          id: 'regionalDetailCard',
          regionalOnly: true,
          notice: '본사 등록 시 입력합니다.',
          rows: [
            [{ type: 'countryBankRow', bankLabel: '계좌은행*', accountNoLabel: '계좌번호*', accountHolderLabel: '예금주*' }],
            [{ label: 'SWIFT', type: 'text', name: 'swift', col: 2, placeholder: 'SWIFT 코드' }, { label: '지점이름', type: 'text', name: 'branchName', col: 2 }, { label: '지점 주소', type: 'text', name: 'branchAddr', col: 2 }, { label: '담당전화번호', type: 'text', name: 'contactTel', col: 2 }],
            [{ label: '코인 지갑 주소', type: 'text', name: 'walletAddress', col: 4, placeholder: '코인 수취 지갑 주소' }, { label: '네트워크', type: 'text', name: 'networkName', col: 2, placeholder: '네트워크 이름' }, { label: '이체수수료', type: 'text', name: 'transferFee', col: 2, placeholder: '원' }, { label: '크립토 이체 수수료(USD)', type: 'text', name: 'cryptoTransferFee', col: 2, placeholder: 'USD' }],
            [{ label: '잔액알림금액', type: 'text', name: 'balanceNotifyAmt', col: 2, smsButton: true, smsColor: 'primary' }, { label: '의심거래/오류알림', type: 'text', name: 'suspiciousNotifyAmt', col: 2, smsButton: true, smsColor: 'warning' }, { label: '해외로그인알림', type: 'text', name: 'overseasLoginNotifyAmt', col: 2, smsButton: true, smsColor: 'success' }, { label: '임시비밀번호알림', type: 'text', name: 'tempPwdNotifyAmt', col: 2, smsButton: true, smsColor: 'secondary' }, { label: '비거래기준월', type: 'text', name: 'nonTranCriterionMonth', col: 2, placeholder: '60' }],
            [{ label: '동일카드 중복결제 한도(WEB)*', type: 'text', name: 'sameCardLimitWebDay', col: 2, placeholder: '일' }, { label: '회', type: 'text', name: 'sameCardLimitWebTimes', col: 2, placeholder: '회' }, { label: '원', type: 'text', name: 'sameCardLimitWebAmt', col: 2, placeholder: '원' }, { label: '동일카드 중복결제 한도(단말)*', type: 'text', name: 'sameCardLimitTerminalDay', col: 2, placeholder: '일' }, { label: '회', type: 'text', name: 'sameCardLimitTerminalTimes', col: 2, placeholder: '회' }, { label: '원', type: 'text', name: 'sameCardLimitTerminalAmt', col: 2, placeholder: '원' }],
            [{ label: '일 이용료', type: 'text', name: 'dailyUsageFee', col: 2 }, { label: '입금자명조회*', type: 'select', name: 'depositNameLookup', options: [{ v: '', t: '선택' }, { v: 'N', t: '미조회' }, { v: 'Y', t: '조회' }], col: 2 }, { label: '이체/출금 인증번호', type: 'text', name: 'transferAuthNo', col: 2 }],
            [{ label: '신규회원 한도 자동전환*', type: 'select', name: 'autoConvertNewMemberLimit', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 2 }, { label: '신규회원 일한도*', type: 'text', name: 'newMemberDailyLimit', col: 2 }, { label: '전환기준일*', type: 'text', name: 'convertRefDate', col: 2 }, { label: '전환 일한도*', type: 'text', name: 'convertDailyLimit', col: 2 }, { label: '적용시작일*', type: 'text', name: 'applyStartDate', col: 2 }]
          ]
        },
        {
          type: 'regionalCardLimitTable',
          title: '카드사별 동일카드 제한',
          id: 'regionalCardLimitCard',
          regionalOnly: true
        },
        {
          title: '정산정보',
          id: 'regionalSettleCard',
          regionalOnly: true,
          rows: [
            [{ label: 'PG수수료(일반)*', type: 'text', name: 'pgFeeGeneral', col: 2, placeholder: '%' }, { label: '이체수수료', type: 'text', name: 'transferFee', col: 2, placeholder: '원' }, { label: '차액정산 월횟수', type: 'text', name: 'settleDiffMonthCnt', col: 2 }, { label: '정산보고서 은행*', type: 'select', name: 'settleReportBankCd', options: [{ v: '', t: '선택하세요' }, { v: '04', t: '국민' }, { v: '20', t: '우리' }, { v: '81', t: 'KEB하나' }, { v: '88', t: '신한' }, { v: '11', t: 'NH농협' }], col: 2 }],
            [{ label: 'PG수수료(삼성페이)', type: 'text', name: 'pgFeeSamsung', col: 2 }, { label: 'SMS수수료', type: 'text', name: 'smsFee', col: 2 }, { label: '세금계산서 이메일', type: 'text', name: 'taxInvoiceEmail', col: 2 }, { label: '계좌번호', type: 'text', name: 'settleAccountNo', col: 2 }],
            [{ label: '직결수수료', type: 'text', name: 'directFee', col: 2 }, { label: '솔루션수수료', type: 'text', name: 'solutionFee', col: 2, placeholder: '0.1%' }, { label: '예금주명*', type: 'text', name: 'settleAccountHolder', col: 2 }]
          ]
        },
        {
          title: '출금 제한 시간 설정',
          id: 'regionalWithdrawLimitCard',
          regionalOnly: true,
          notice: '출금제한일에는 출금이 실행되지 않습니다. 시작시간과 종료시간은 출금제한일 이외날짜에 적용됩니다.',
          rows: [
            [{ label: '출금제한', type: 'select', name: 'withdrawRestrictType', options: [{ v: '', t: '선택' }, { v: 'MONTHLY', t: '매월' }, { v: 'WEEKLY', t: '매주' }, { v: 'NONE', t: '미사용' }], col: 1 }, { label: '출금제한시작시간*', type: 'time', name: 'withdrawRestrictStartTime', col: 1 }, { label: '출금제한종료시간*', type: 'time', name: 'withdrawRestrictEndTime', col: 1 }]
          ]
        },
        {
          title: '결제 제한 시간 설정',
          id: 'regionalPayLimitCard',
          regionalOnly: true,
          rows: [
            [{ label: '단말 결제제한*', type: 'select', name: 'terminalPayRestrict', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 1 }, { label: 'WEB 결제제한*', type: 'select', name: 'webPayRestrict', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 1 }]
          ]
        },
        {
          title: '기본 수수료 설정',
          id: 'regionalDefaultFeeCard',
          regionalOnly: true,
          rows: [
            [{ label: '본사', type: 'text', name: 'defaultFeeHq', col: 2, placeholder: '0.0' }, { label: '총판', type: 'text', name: 'defaultFeeDist', col: 2, placeholder: '0.0' }, { label: '지사', type: 'text', name: 'defaultFeeBranch', col: 2, placeholder: '0.0' }, { label: '대리점', type: 'text', name: 'defaultFeeAgency', col: 2, placeholder: '0.0' }, { label: '영업점', type: 'text', name: 'defaultFeeSalesOffice', col: 2, placeholder: '0.0' }]
          ]
        },
        {
          title: '기본 결제한도 설정',
          id: 'regionalPayLimitDefaultCard',
          regionalOnly: true,
          rows: [
            [{ label: '1회 한도*', type: 'text', name: 'defaultPayLimitPerTx', col: 2, placeholder: '0' }, { label: '일 한도*', type: 'text', name: 'defaultPayLimitDay', col: 2, placeholder: '0' }, { label: '월 한도*', type: 'text', name: 'defaultPayLimitMonth', col: 2, placeholder: '0' }, { label: '연 한도(법인)*', type: 'text', name: 'defaultPayLimitYearCorp', col: 2, placeholder: '0' }, { label: '연 한도(개인)*', type: 'text', name: 'defaultPayLimitYearInd', col: 2, placeholder: '0' }]
          ]
        },
        {
          type: 'regionalTerminalTable',
          title: '기본 터미널 정보',
          id: 'regionalTerminalCard',
          regionalOnly: true
        },
        {
          title: '상세정보',
          id: 'distributorExtraCard',
          masterDistOnly: true,
          notice: '총판일 때만 입력합니다. 총판은 1가지 화폐만 지정할 수 있습니다. 노티 URL 1은 기본(필수)이며, 2~4는 보조 URL입니다. 이 주소로 들어온 결제 정보만 해당 총판에서 조회됩니다.',
          rows: [
            [{ label: '기준 화폐*', type: 'select', name: 'baseCurrency', options: [{ v: '', t: '선택' }, { v: 'KRW', t: 'KRW (원)' }, { v: 'USD', t: 'USD (달러)' }, { v: 'JPY', t: 'JPY (엔)' }, { v: 'THB', t: 'THB (바트)' }, { v: 'EUR', t: 'EUR (유로)' }], col: 2 }, { label: '사이트개요', type: 'text', name: 'siteSummary', col: 2, placeholder: '사이트개요' }, { label: '취급물품', type: 'text', name: 'product', col: 2 }, { label: '대표사이트', type: 'text', name: 'homepage', col: 2, placeholder: 'https://' }],
            [{ label: '정산담당자명', type: 'text', name: 'settleName', col: 2 }, { label: '정산담당자연락처', type: 'text', name: 'settleTelNo', col: 2, placeholder: '010-0000-0000' }, { label: '정산형태', type: 'select', name: 'settleType', options: [{ v: '', t: '선택' }, { v: 'M', t: '가맹점별' }, { v: 'G', t: '총판' }], col: 1 }, { label: '요율(%)', type: 'text', name: 'commissionRate', col: 1, placeholder: '요율' }, { label: '사용한도', type: 'text', name: 'limitAmt', col: 2, placeholder: '사용한도' }],
            [{ label: '노티 URL 1(기본)*', type: 'select', name: 'notifyUrl1', col: 6, loadNotifyTargets: true, button: '노티선택' }, { label: '노티 URL 2(보조)', type: 'select', name: 'notifyUrl2', col: 6, loadNotifyTargets: true, button: '노티선택' }],
            [{ label: '노티 URL 3', type: 'select', name: 'notifyUrl3', col: 6, loadNotifyTargets: true, button: '노티선택' }, { label: '노티 URL 4', type: 'select', name: 'notifyUrl4', col: 6, loadNotifyTargets: true, button: '노티선택' }]
          ]
        },
        {
          title: '가맹점 상세 정보',
          id: 'merchantExtraCard',
          merchantOnly: true,
          notice: '가맹점일 때만 입력합니다.',
          rows: [
            [{ label: '사업자형태', type: 'text', name: 'bizNature', col: 2 }, { label: '취급물품', type: 'text', name: 'product', col: 2 }, { label: '대표사이트', type: 'text', name: 'homepage', col: 2, placeholder: 'https://' }, { label: '정산담당자명', type: 'text', name: 'settleName', col: 2 }, { label: '정산담당자연락처', type: 'text', name: 'settleTelNo', col: 2 }]
          ]
        },
        {
          title: '계좌정보',
          id: 'settlementAccountCard',
          distributorMerchantOnlyNoRegional: true,
          rows: [
            [{ type: 'countryBankRow', bankLabel: '계좌은행*', accountNoLabel: '계좌번호*', accountHolderLabel: '예금주*' }],
            [{ label: 'SWIFT', type: 'text', name: 'swift', col: 2, placeholder: 'SWIFT 코드' }, { label: '지점이름', type: 'text', name: 'branchName', col: 2 }, { label: '지점 주소', type: 'text', name: 'branchAddr', col: 2 }, { label: '담당전화번호', type: 'text', name: 'contactTel', col: 2 }],
            [{ label: '코인 지갑 주소', type: 'text', name: 'walletAddress', col: 4, placeholder: '코인 수취 지갑 주소' }, { label: '네트워크', type: 'text', name: 'networkName', col: 2, placeholder: '네트워크 이름' }, { label: '이체수수료', type: 'text', name: 'transferFee', col: 2, placeholder: '원' }, { label: '크립토 이체 수수료(USD)', type: 'text', name: 'cryptoTransferFee', col: 2, placeholder: 'USD' }]
          ]
        },
        {
          title: '출금 제한 설정',
          id: 'withdrawLimitCard',
          merchantOnly: true,
          rows: [
            [{ label: '출금제한일', type: 'text', name: 'withdrawLimitDays', col: 1, placeholder: '일' }, { label: '출금시작시간', type: 'time', name: 'withdrawStartTime', col: 1 }, { label: '출금종료시간', type: 'time', name: 'withdrawEndTime', col: 1 }]
          ]
        },
        {
          title: '지급한도 설정',
          id: 'payLimitCard',
          merchantOnly: true,
          rows: [
            [{ label: '기본한도(원)', type: 'text', name: 'payLimitDefault', col: 2, placeholder: '1회 지급한도' }, { label: '추가한도(원)', type: 'text', name: 'payLimitExtra', col: 2 }, { label: '한도알림', type: 'select', name: 'payLimitAlertSms', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: 'SMS' }], col: 1 }]
          ]
        },
        {
          title: '보류율 설정',
          id: 'holdRateCard',
          merchantOnly: true,
          notice: '본사정책 따름 선택 시 본사설정의 수수료 정책(롤링 비율/일수)에 연동되어 반영됩니다.',
          rows: [
            [{ label: '본사정책 따름', type: 'select', name: 'holdRateFollowHq', options: [{ v: 'Y', t: '본사정책 따름' }, { v: 'N', t: '직접입력' }], col: 2 }],
            [{ label: '보류율(%)', type: 'text', name: 'holdRate', col: 1, placeholder: '5', holdRateOnly: true }, { label: '보류기간(일)', type: 'text', name: 'holdDays', col: 1, placeholder: '120', holdRateOnly: true }]
          ]
        },
        {
          title: '수수료정책',
          id: 'commissionPolicyCard',
          merchantOnly: true,
          notice: '본사정책 따름 선택 시 본사설정에서 배포한 정책 템플릿을 선택할 수 있고, 선택한 정책값이 가맹점 생성 시 자동 부여됩니다.',
          rows: [
            [{ label: '본사정책 따름', type: 'select', name: 'commissionFollowHq', options: [{ v: 'Y', t: '본사정책 따름' }, { v: 'N', t: '직접입력' }], col: 2 }, { label: '본사 정책선택', type: 'select', name: 'hqPolicyScope', options: [{ v: '', t: '기본(DEFAULT)' }], col: 2, hqPolicyOnly: true }],
            [{ label: '실패수수료', type: 'text', name: 'failFee', col: 2, customOnly: true }, { label: '이용수수료', type: 'text', name: 'usageRate', col: 2, customOnly: true }, { label: '결제 수수료', type: 'text', name: 'payRate', col: 2, customOnly: true }],
            [{ label: '취소 수수료', type: 'text', name: 'cancelRate', col: 2, customOnly: true }, { label: '환불 수수료', type: 'text', name: 'refundRate', col: 2, customOnly: true }, { label: '비고', type: 'text', name: 'commissionMemo', col: 2, customOnly: true }],
            [{ label: '정산수수료', type: 'text', name: 'feeSettlementPerTx', col: 2, customOnly: true }, { label: 'USDT수수료', type: 'text', name: 'feeUsdt', col: 2, customOnly: true }, { label: 'FX수수료', type: 'text', name: 'feeFx', col: 2, customOnly: true }]
          ]
        },
        {
          title: '정산방법',
          id: 'calcMethodCard',
          merchantOnly: true,
          notice: '정산주기·영업일(휴일 제외) 기준은 본사 정산정보와 동일합니다. 정산마감시간까지 거래를 마감하고, 정산자동개시시간에 정산 계산(정산구분 자동 시). 정산구분: 수동=마감 후 운영자가 개시, 자동=개시시간에 자동, 펌뱅킹=마감 금액 기준 이체까지 연동. 이체및송금구분은 펌뱅킹 송금 방식(수동/자동/사용안함). 정산제외여부 사용 시 마감·자동개시가 보류되며, 해제 후 해당일 정산마감·자동개시 규칙이 적용됩니다. 정산최소금액 미만이면 해당 주기 정산은 다음 순번으로 이월됩니다. 이체및송금최소금액 미만이면 펌뱅킹 송금이 제한됩니다. 이체시간은 펌뱅킹 연동 시 해당 시각에 이체합니다.',
          rows: [
            [{ label: '정산주기', type: 'select', name: 'calcCycle', options: CALC_CYCLE_OPTIONS, col: 1 }, { label: '정산마감시간', type: 'time', name: 'calcCloseTime', col: 1 }, { label: '정산자동개시시간', type: 'time', name: 'calcStartTime', col: 1 }],
            [{ label: '정산구분', type: 'select', name: 'calcProcType', options: CALC_PROC_OPTIONS, col: 1 }, { label: '이체및송금구분', type: 'select', name: 'transferType', options: TRANSFER_REMIT_OPTIONS, col: 1 }, { label: '이체주기(일)', type: 'text', name: 'transferCycleDays', col: 1 }, { label: '이체시간', type: 'time', name: 'transferExecTime', col: 1 }],
            [{ label: '정산제외여부', type: 'select', name: 'calcExcludeYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 1 }, { label: '정산제외대상', type: 'select', name: 'calcExcludeTarget', options: [{ v: 'NONE', t: '해당없음' }, { v: 'WEB', t: 'WEB' }, { v: 'OFFLINE', t: '오프라인' }, { v: 'BOTH', t: 'WEB+오프라인' }], col: 1 }, { label: '지급보류', type: 'select', name: 'payHoldYn', options: [{ v: 'N', t: '아니오' }, { v: 'Y', t: '예' }], col: 1 }],
            [{ label: '정산최소금액(원)', type: 'text', name: 'calcMinAmt', col: 1, placeholder: '미만 시 다음 주기' }, { label: '이체및송금최소금액(원)', type: 'text', name: 'autoTransferMin', col: 1, placeholder: '펌뱅킹 최소' }]
          ]
        },
        {
          type: 'pgBindingList',
          title: '결제대행사 설정',
          id: 'pgBindingCard',
          merchantOnly: true,
          notice: '본사설정 > PG사 API 연동에 등록된 결제대행사를 선택하고 MID·API KEY 등을 입력하세요. 등록 화면에서는 하단 [저장] 시 한꺼번에 반영됩니다.'
        },
        {
          title: '웹결제 사용',
          id: 'webPaymentCard',
          merchantOnly: true,
          notice: '미사용 선택 시 WEB 결제 시스템이 중지됩니다.',
          rows: [
            [{ label: '웹결제 사용여부', type: 'select', name: 'webPaymentUseYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 }]
          ]
        },
        {
          title: '대표 기본상품정보 (온라인 URL 결제용)',
          id: 'defaultProductCard',
          merchantOnly: true,
          rows: [
            [{ label: '상품명', type: 'text', name: 'defaultProductName', col: 2, placeholder: '대표 상품명' }, { label: '상품코드', type: 'text', name: 'defaultProductCode', col: 1 }, { label: '기본금액(원)', type: 'text', name: 'defaultProductAmount', col: 1, placeholder: '0' }, { label: '상품설명', type: 'text', name: 'defaultProductDesc', col: 4 }]
          ]
        },
        {
          title: '결제통보 URL',
          id: 'notifyUrlCard',
          merchantOnly: true,
          notice: '결제 응답을 가맹점에게 송부할 노티 주소. 등록 시 결제통보 URL관리에 자동 반영됩니다.',
          rows: [
            [{ label: 'URL Background', type: 'text', name: 'notifyUrlBackground', col: 5, placeholder: 'https://' }, { label: 'URL Result', type: 'text', name: 'notifyUrlResult', col: 5, placeholder: 'https://' }]
          ]
        },
        {
          type: 'branding',
          title: '브랜딩 설정',
          id: 'brandingCard',
          regionalOrMasterDistOnly: true,
          notice: '본사·총판만 설정 가능. 메인이미지=로그인 화면 왼쪽 배경, 로고=로그인창 상단·사이드바 상단.'
        },
        { title: '기타(본사)', id: 'regionalMiscCard', regionalOnly: true, notice: '메인이미지는 2MB, 로고이미지는 1MB까지 업로드 가능합니다. PNG파일을 추천합니다.', rows: [[{ label: 'COPYRIGHT', type: 'textarea', name: 'copyright', col: 6, placeholder: 'Copyright © 2025 ICOPAY Service by Ontheline Co., Ltd.' }, { label: '특이사항', type: 'textarea', name: 'remark', col: 6 }]] },
        { title: '기타', rows: [[{ label: '특이사항', type: 'textarea', name: 'remark', col: 6 }]], branchAgencySalesHide: true },
        { title: '첨부파일', rows: [[{ type: 'file', name: 'attach', col: 4, button: '추가' }]] }
      ],
      buttons: [{ id: 'compRegSaveBtn', label: '저장', cls: 'btn-primary' }, { id: 'compRegCancelBtn', label: '취소', cls: 'btn-secondary' }]
    },
    '/comp/compDetail': {
      isForm: true,
      isCompDetail: true,
      formSections: [
        {
          title: '기본정보',
          notice: '업체구분에 따라 해당하는 입력 항목이 표시됩니다. 사용여부를 미사용으로 변경하면 하위 조직 전체가 미사용됩니다. 가맹점은 상위 지점을 변경하여 다른 사용 중인 상위 아래로 활성화할 수 있습니다.',
          rows: [
            [{ label: '업체코드', type: 'text', name: 'compId', col: 2, readonly: true }, { label: '상위 본사', type: 'text', name: 'parentComp', col: 2, button: '검색', placeholder: '상위 코드' }, { label: '업체구분*', type: 'select', name: 'compDiv', options: [{ v: '', t: '선택' }, { v: 'REGIONAL', t: '본사' }, { v: 'MASTER_DIST', t: '총판' }, { v: 'BRANCH', t: '지사' }, { v: 'AGENCY', t: '대리점' }, { v: 'SALES_OFFICE', t: '영업점' }, { v: 'MERCHANT', t: '가맹점' }], col: 1 }, { label: '업체명*', type: 'text', name: 'compNm', col: 2 }, { label: '사업자번호*', type: 'regNoWithType', name: 'regNo', col: 2 }, { label: '업태', type: 'text', name: 'bizType', col: 1 }, { label: '종목', type: 'text', name: 'industry', col: 1 }],
            [{ label: '대표자명*', type: 'text', name: 'ceoNm', col: 2 }, { label: '휴대폰*', type: 'text', name: 'ceoMobile', col: 2 }, { label: '업체전화*', type: 'text', name: 'compTel', col: 2 }, { label: '팩스', type: 'text', name: 'fax', col: 2 }, { label: '이메일', type: 'text', name: 'email', col: 2 }, { label: '비고', type: 'text', name: 'remark', col: 2 }],
            [{ type: 'countryAddressRow', zipLabel: '우편번호*', addrLabel: '주소*', addrDetailLabel: '상세주소', addrEtcLabel: '기타' }],
            [{ label: '사용여부*', type: 'select', name: 'useYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 1 }, { label: '로그인ID*', type: 'text', name: 'loginId', col: 2, button: 'ID변경' }, { label: '비밀번호', type: 'passwordReset', name: 'pwdReset', col: 2 }]
          ]
        },
        {
          title: '본사 설정 (환기준)',
          id: 'regionalExtraCard',
          regionalOnly: true,
          notice: '총본사 로그인 시에만 본사를 추가할 수 있습니다. 본사는 기준 화폐를 최대 3가지까지 지정할 수 있습니다.',
          rows: [
            [{ label: '기준 화폐1*', type: 'select', name: 'baseCurrency1', options: [{ v: '', t: '선택' }, { v: 'KRW', t: 'KRW (원)' }, { v: 'USD', t: 'USD (달러)' }, { v: 'JPY', t: 'JPY (엔)' }, { v: 'THB', t: 'THB (바트)' }, { v: 'EUR', t: 'EUR (유로)' }], col: 2 }, { label: '기준 화폐2', type: 'select', name: 'baseCurrency2', options: [{ v: '', t: '선택' }, { v: 'KRW', t: 'KRW (원)' }, { v: 'USD', t: 'USD (달러)' }, { v: 'JPY', t: 'JPY (엔)' }, { v: 'THB', t: 'THB (바트)' }, { v: 'EUR', t: 'EUR (유로)' }], col: 2 }, { label: '기준 화폐3', type: 'select', name: 'baseCurrency3', options: [{ v: '', t: '선택' }, { v: 'KRW', t: 'KRW (원)' }, { v: 'USD', t: 'USD (달러)' }, { v: 'JPY', t: 'JPY (엔)' }, { v: 'THB', t: 'THB (바트)' }, { v: 'EUR', t: 'EUR (유로)' }], col: 2 }, { label: '사업자형태', type: 'text', name: 'bizNature', col: 2 }, { label: '취급물품', type: 'text', name: 'product', col: 2 }, { label: '대표사이트', type: 'text', name: 'homepage', col: 2, placeholder: 'https://' }],
            [{ label: '정산담당자명', type: 'text', name: 'settleName', col: 2 }, { label: '정산담당자연락처', type: 'text', name: 'settleTelNo', col: 2, placeholder: '010-0000-0000' }, { label: '송금자명(입금시)', type: 'text', name: 'remitterName', col: 2, placeholder: '입금 시 송금자명' }]
          ]
        },
        {
          title: '영업일 · 휴일 (본사)',
          id: 'regionalBusinessHolidayCard',
          regionalOrMasterDistOnly: true,
          notice: '영업일 상세는 [본사설정 > 영업일설정]에서 관리합니다. 여기서는 적용할 설정 이름을 선택하세요.',
          rows: [
            [{ label: '영업일 설정 이름', type: 'select', name: 'holidayProfileName', options: [{ v: '', t: '선택' }], col: 3 }],
            [{ label: '기준국가', type: 'text', name: 'holidayProfileCountry', col: 2, readonly: true }],
            [{ type: 'customHtml', html: '<input type="hidden" name="holidayCountryCode"><input type="hidden" name="holidayCountryCodes"><input type="hidden" name="businessHolidayExtraDates"><input type="hidden" name="businessHolidayRangesJson">', col: 12 }]
          ]
        },
        {
          title: '본사 업체 상세 정보',
          id: 'regionalDetailCard',
          regionalOnly: true,
          notice: '본사 등록 시 입력합니다.',
          rows: [
            [{ type: 'countryBankRow', bankLabel: '계좌은행*', accountNoLabel: '계좌번호*', accountHolderLabel: '예금주*' }],
            [{ label: 'SWIFT', type: 'text', name: 'swift', col: 2, placeholder: 'SWIFT 코드' }, { label: '지점이름', type: 'text', name: 'branchName', col: 2 }, { label: '지점 주소', type: 'text', name: 'branchAddr', col: 2 }, { label: '담당전화번호', type: 'text', name: 'contactTel', col: 2 }],
            [{ label: '코인 지갑 주소', type: 'text', name: 'walletAddress', col: 4, placeholder: '코인 수취 지갑 주소' }, { label: '네트워크', type: 'text', name: 'networkName', col: 2, placeholder: '네트워크 이름' }, { label: '이체수수료', type: 'text', name: 'transferFee', col: 2, placeholder: '원' }, { label: '크립토 이체 수수료(USD)', type: 'text', name: 'cryptoTransferFee', col: 2, placeholder: 'USD' }],
            [{ label: '잔액알림금액', type: 'text', name: 'balanceNotifyAmt', col: 2, smsButton: true, smsColor: 'primary' }, { label: '의심거래/오류알림', type: 'text', name: 'suspiciousNotifyAmt', col: 2, smsButton: true, smsColor: 'warning' }, { label: '해외로그인알림', type: 'text', name: 'overseasLoginNotifyAmt', col: 2, smsButton: true, smsColor: 'success' }, { label: '임시비밀번호알림', type: 'text', name: 'tempPwdNotifyAmt', col: 2, smsButton: true, smsColor: 'secondary' }, { label: '비거래기준월', type: 'text', name: 'nonTranCriterionMonth', col: 2, placeholder: '60' }],
            [{ label: '동일카드 중복결제 한도(WEB)*', type: 'text', name: 'sameCardLimitWebDay', col: 2, placeholder: '일' }, { label: '회', type: 'text', name: 'sameCardLimitWebTimes', col: 2, placeholder: '회' }, { label: '원', type: 'text', name: 'sameCardLimitWebAmt', col: 2, placeholder: '원' }, { label: '동일카드 중복결제 한도(단말)*', type: 'text', name: 'sameCardLimitTerminalDay', col: 2, placeholder: '일' }, { label: '회', type: 'text', name: 'sameCardLimitTerminalTimes', col: 2, placeholder: '회' }, { label: '원', type: 'text', name: 'sameCardLimitTerminalAmt', col: 2, placeholder: '원' }],
            [{ label: '일 이용료', type: 'text', name: 'dailyUsageFee', col: 2 }, { label: '입금자명조회*', type: 'select', name: 'depositNameLookup', options: [{ v: '', t: '선택' }, { v: 'N', t: '미조회' }, { v: 'Y', t: '조회' }], col: 2 }, { label: '이체/출금 인증번호', type: 'text', name: 'transferAuthNo', col: 2 }],
            [{ label: '신규회원 한도 자동전환*', type: 'select', name: 'autoConvertNewMemberLimit', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 2 }, { label: '신규회원 일한도*', type: 'text', name: 'newMemberDailyLimit', col: 2 }, { label: '전환기준일*', type: 'text', name: 'convertRefDate', col: 2 }, { label: '전환 일한도*', type: 'text', name: 'convertDailyLimit', col: 2 }, { label: '적용시작일*', type: 'text', name: 'applyStartDate', col: 2 }]
          ]
        },
        {
          type: 'regionalCardLimitTable',
          title: '카드사별 동일카드 제한',
          id: 'regionalCardLimitCard',
          regionalOnly: true
        },
        {
          title: '정산정보',
          id: 'regionalSettleCard',
          regionalOnly: true,
          rows: [
            [{ label: 'PG수수료(일반)*', type: 'text', name: 'pgFeeGeneral', col: 2, placeholder: '%' }, { label: '이체수수료', type: 'text', name: 'transferFee', col: 2, placeholder: '원' }, { label: '차액정산 월횟수', type: 'text', name: 'settleDiffMonthCnt', col: 2 }, { label: '정산보고서 은행*', type: 'select', name: 'settleReportBankCd', options: [{ v: '', t: '선택하세요' }, { v: '04', t: '국민' }, { v: '20', t: '우리' }, { v: '81', t: 'KEB하나' }, { v: '88', t: '신한' }, { v: '11', t: 'NH농협' }], col: 2 }],
            [{ label: 'PG수수료(삼성페이)', type: 'text', name: 'pgFeeSamsung', col: 2 }, { label: 'SMS수수료', type: 'text', name: 'smsFee', col: 2 }, { label: '세금계산서 이메일', type: 'text', name: 'taxInvoiceEmail', col: 2 }, { label: '계좌번호', type: 'text', name: 'settleAccountNo', col: 2 }],
            [{ label: '직결수수료', type: 'text', name: 'directFee', col: 2 }, { label: '솔루션수수료', type: 'text', name: 'solutionFee', col: 2, placeholder: '0.1%' }, { label: '예금주명*', type: 'text', name: 'settleAccountHolder', col: 2 }]
          ]
        },
        {
          title: '출금 제한 시간 설정',
          id: 'regionalWithdrawLimitCard',
          regionalOnly: true,
          notice: '출금제한일에는 출금이 실행되지 않습니다. 시작시간과 종료시간은 출금제한일 이외날짜에 적용됩니다.',
          rows: [
            [{ label: '출금제한', type: 'select', name: 'withdrawRestrictType', options: [{ v: '', t: '선택' }, { v: 'MONTHLY', t: '매월' }, { v: 'WEEKLY', t: '매주' }, { v: 'NONE', t: '미사용' }], col: 1 }, { label: '출금제한시작시간*', type: 'time', name: 'withdrawRestrictStartTime', col: 1 }, { label: '출금제한종료시간*', type: 'time', name: 'withdrawRestrictEndTime', col: 1 }]
          ]
        },
        {
          title: '결제 제한 시간 설정',
          id: 'regionalPayLimitCard',
          regionalOnly: true,
          rows: [
            [{ label: '단말 결제제한*', type: 'select', name: 'terminalPayRestrict', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 1 }, { label: 'WEB 결제제한*', type: 'select', name: 'webPayRestrict', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 1 }]
          ]
        },
        {
          title: '기본 수수료 설정',
          id: 'regionalDefaultFeeCard',
          regionalOnly: true,
          rows: [
            [{ label: '본사', type: 'text', name: 'defaultFeeHq', col: 2, placeholder: '0.0' }, { label: '총판', type: 'text', name: 'defaultFeeDist', col: 2, placeholder: '0.0' }, { label: '지사', type: 'text', name: 'defaultFeeBranch', col: 2, placeholder: '0.0' }, { label: '대리점', type: 'text', name: 'defaultFeeAgency', col: 2, placeholder: '0.0' }, { label: '영업점', type: 'text', name: 'defaultFeeSalesOffice', col: 2, placeholder: '0.0' }]
          ]
        },
        {
          title: '기본 결제한도 설정',
          id: 'regionalPayLimitDefaultCard',
          regionalOnly: true,
          rows: [
            [{ label: '1회 한도*', type: 'text', name: 'defaultPayLimitPerTx', col: 2, placeholder: '0' }, { label: '일 한도*', type: 'text', name: 'defaultPayLimitDay', col: 2, placeholder: '0' }, { label: '월 한도*', type: 'text', name: 'defaultPayLimitMonth', col: 2, placeholder: '0' }, { label: '연 한도(법인)*', type: 'text', name: 'defaultPayLimitYearCorp', col: 2, placeholder: '0' }, { label: '연 한도(개인)*', type: 'text', name: 'defaultPayLimitYearInd', col: 2, placeholder: '0' }]
          ]
        },
        {
          type: 'regionalTerminalTable',
          title: '기본 터미널 정보',
          id: 'regionalTerminalCard',
          regionalOnly: true
        },
        {
          title: '상세정보',
          id: 'distributorExtraCard',
          masterDistOnly: true,
          notice: '총판일 때만 입력합니다. 총판은 1가지 화폐만 지정할 수 있습니다. 노티 URL 1은 기본(필수)이며, 2~4는 보조 URL입니다. 이 주소로 들어온 결제 정보만 해당 총판에서 조회됩니다.',
          rows: [
            [{ label: '기준 화폐*', type: 'select', name: 'baseCurrency', options: [{ v: '', t: '선택' }, { v: 'KRW', t: 'KRW (원)' }, { v: 'USD', t: 'USD (달러)' }, { v: 'JPY', t: 'JPY (엔)' }, { v: 'THB', t: 'THB (바트)' }, { v: 'EUR', t: 'EUR (유로)' }], col: 2 }, { label: '사이트개요', type: 'text', name: 'siteSummary', col: 2, placeholder: '사이트개요' }, { label: '취급물품', type: 'text', name: 'product', col: 2 }, { label: '대표사이트', type: 'text', name: 'homepage', col: 2, placeholder: 'https://' }],
            [{ label: '정산담당자명', type: 'text', name: 'settleName', col: 2 }, { label: '정산담당자연락처', type: 'text', name: 'settleTelNo', col: 2, placeholder: '010-0000-0000' }, { label: '정산형태', type: 'select', name: 'settleType', options: [{ v: '', t: '선택' }, { v: 'M', t: '가맹점별' }, { v: 'G', t: '총판' }], col: 1 }, { label: '요율(%)', type: 'text', name: 'commissionRate', col: 1, placeholder: '요율' }, { label: '사용한도', type: 'text', name: 'limitAmt', col: 2, placeholder: '사용한도' }],
            [{ label: '노티 URL 1(기본)*', type: 'select', name: 'notifyUrl1', col: 6, loadNotifyTargets: true, button: '노티선택' }, { label: '노티 URL 2(보조)', type: 'select', name: 'notifyUrl2', col: 6, loadNotifyTargets: true, button: '노티선택' }],
            [{ label: '노티 URL 3', type: 'select', name: 'notifyUrl3', col: 6, loadNotifyTargets: true, button: '노티선택' }, { label: '노티 URL 4', type: 'select', name: 'notifyUrl4', col: 6, loadNotifyTargets: true, button: '노티선택' }]
          ]
        },
        {
          title: '가맹점 상세 정보',
          id: 'merchantExtraCard',
          merchantOnly: true,
          notice: '가맹점일 때만 입력합니다.',
          rows: [
            [{ label: '사업자형태', type: 'text', name: 'bizNature', col: 2 }, { label: '취급물품', type: 'text', name: 'product', col: 2 }, { label: '대표사이트', type: 'text', name: 'homepage', col: 2, placeholder: 'https://' }, { label: '정산담당자명', type: 'text', name: 'settleName', col: 2 }, { label: '정산담당자연락처', type: 'text', name: 'settleTelNo', col: 2 }]
          ]
        },
        {
          title: '계좌정보',
          id: 'settlementAccountCard',
          distributorMerchantOnlyNoRegional: true,
          rows: [
            [{ type: 'countryBankRow', bankLabel: '계좌은행*', accountNoLabel: '계좌번호*', accountHolderLabel: '예금주*' }],
            [{ label: 'SWIFT', type: 'text', name: 'swift', col: 2, placeholder: 'SWIFT 코드' }, { label: '지점이름', type: 'text', name: 'branchName', col: 2 }, { label: '지점 주소', type: 'text', name: 'branchAddr', col: 2 }, { label: '담당전화번호', type: 'text', name: 'contactTel', col: 2 }],
            [{ label: '코인 지갑 주소', type: 'text', name: 'walletAddress', col: 4, placeholder: '코인 수취 지갑 주소' }, { label: '네트워크', type: 'text', name: 'networkName', col: 2, placeholder: '네트워크 이름' }, { label: '이체수수료', type: 'text', name: 'transferFee', col: 2, placeholder: '원' }, { label: '크립토 이체 수수료(USD)', type: 'text', name: 'cryptoTransferFee', col: 2, placeholder: 'USD' }]
          ]
        },
        {
          title: '출금 제한 설정',
          id: 'withdrawLimitCard',
          merchantOnly: true,
          rows: [
            [{ label: '출금제한일', type: 'text', name: 'withdrawLimitDays', col: 1, placeholder: '일' }, { label: '출금시작시간', type: 'time', name: 'withdrawStartTime', col: 1 }, { label: '출금종료시간', type: 'time', name: 'withdrawEndTime', col: 1 }]
          ]
        },
        {
          title: '지급한도 설정',
          id: 'payLimitCard',
          merchantOnly: true,
          rows: [
            [{ label: '기본한도(원)', type: 'text', name: 'payLimitDefault', col: 2, placeholder: '1회 지급한도' }, { label: '추가한도(원)', type: 'text', name: 'payLimitExtra', col: 2 }, { label: '한도알림', type: 'select', name: 'payLimitAlertSms', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: 'SMS' }], col: 1 }]
          ]
        },
        {
          title: '보류율 설정',
          id: 'holdRateCard',
          merchantOnly: true,
          notice: '본사정책 따름 선택 시 본사설정의 수수료 정책(롤링 비율/일수)에 연동되어 반영됩니다.',
          rows: [
            [{ label: '본사정책 따름', type: 'select', name: 'holdRateFollowHq', options: [{ v: 'Y', t: '본사정책 따름' }, { v: 'N', t: '직접입력' }], col: 2 }],
            [{ label: '보류율(%)', type: 'text', name: 'holdRate', col: 1, placeholder: '5', holdRateOnly: true }, { label: '보류기간(일)', type: 'text', name: 'holdDays', col: 1, placeholder: '120', holdRateOnly: true }]
          ]
        },
        {
          title: '수수료정책',
          id: 'commissionPolicyCard',
          merchantOnly: true,
          notice: '본사정책 따름 선택 시 본사설정이 적용되며, 직접입력 시 아래 항목을 입력합니다.',
          rows: [
            [{ label: '본사정책 따름', type: 'select', name: 'commissionFollowHq', options: [{ v: 'Y', t: '본사정책 따름' }, { v: 'N', t: '직접입력' }], col: 2 }],
            [{ label: '실패수수료', type: 'text', name: 'failFee', col: 2, customOnly: true }, { label: '이용수수료', type: 'text', name: 'usageRate', col: 2, customOnly: true }, { label: '결제 수수료', type: 'text', name: 'payRate', col: 2, customOnly: true }],
            [{ label: '취소 수수료', type: 'text', name: 'cancelRate', col: 2, customOnly: true }, { label: '환불 수수료', type: 'text', name: 'refundRate', col: 2, customOnly: true }, { label: '비고', type: 'text', name: 'commissionMemo', col: 2, customOnly: true }],
            [{ label: '정산수수료', type: 'text', name: 'feeSettlementPerTx', col: 2, customOnly: true }, { label: 'USDT수수료', type: 'text', name: 'feeUsdt', col: 2, customOnly: true }, { label: 'FX수수료', type: 'text', name: 'feeFx', col: 2, customOnly: true }]
          ]
        },
        {
          title: '정산방법',
          id: 'calcMethodCard',
          merchantOnly: true,
          notice: '정산주기·영업일(휴일 제외) 기준은 본사 정산정보와 동일합니다. 정산마감시간까지 거래를 마감하고, 정산자동개시시간에 정산 계산(정산구분 자동 시). 정산구분: 수동=마감 후 운영자가 개시, 자동=개시시간에 자동, 펌뱅킹=마감 금액 기준 이체까지 연동. 이체및송금구분은 펌뱅킹 송금 방식(수동/자동/사용안함). 정산제외여부 사용 시 마감·자동개시가 보류되며, 해제 후 해당일 정산마감·자동개시 규칙이 적용됩니다. 정산최소금액 미만이면 해당 주기 정산은 다음 순번으로 이월됩니다. 이체및송금최소금액 미만이면 펌뱅킹 송금이 제한됩니다. 이체시간은 펌뱅킹 연동 시 해당 시각에 이체합니다.',
          rows: [
            [{ label: '정산주기', type: 'select', name: 'calcCycle', options: CALC_CYCLE_OPTIONS, col: 1 }, { label: '정산마감시간', type: 'time', name: 'calcCloseTime', col: 1 }, { label: '정산자동개시시간', type: 'time', name: 'calcStartTime', col: 1 }],
            [{ label: '정산구분', type: 'select', name: 'calcProcType', options: CALC_PROC_OPTIONS, col: 1 }, { label: '이체및송금구분', type: 'select', name: 'transferType', options: TRANSFER_REMIT_OPTIONS, col: 1 }, { label: '이체주기(일)', type: 'text', name: 'transferCycleDays', col: 1 }, { label: '이체시간', type: 'time', name: 'transferExecTime', col: 1 }],
            [{ label: '정산제외여부', type: 'select', name: 'calcExcludeYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 1 }, { label: '정산제외대상', type: 'select', name: 'calcExcludeTarget', options: [{ v: 'NONE', t: '해당없음' }, { v: 'WEB', t: 'WEB' }, { v: 'OFFLINE', t: '오프라인' }, { v: 'BOTH', t: 'WEB+오프라인' }], col: 1 }, { label: '지급보류', type: 'select', name: 'payHoldYn', options: [{ v: 'N', t: '아니오' }, { v: 'Y', t: '예' }], col: 1 }],
            [{ label: '정산최소금액(원)', type: 'text', name: 'calcMinAmt', col: 1, placeholder: '미만 시 다음 주기' }, { label: '이체및송금최소금액(원)', type: 'text', name: 'autoTransferMin', col: 1, placeholder: '펌뱅킹 최소' }]
          ]
        },
        {
          type: 'pgBindingList',
          title: '결제대행사 설정',
          id: 'pgBindingCard',
          merchantOnly: true,
          notice: '본사설정 > PG사 API 연동에 등록된 결제대행사 이름(코드)을 선택한 뒤 MID·API KEY·IV KEY를 입력합니다. [추가] 시 입력란이 열리고, 업체정보(가맹점)에서는 [저장][삭제][수정]마다 확인창이 두 번 뜹니다.'
        },
        {
          title: '웹결제 사용',
          id: 'webPaymentCard',
          merchantOnly: true,
          notice: '미사용 선택 시 WEB 결제 시스템이 중지됩니다.',
          rows: [
            [{ label: '웹결제 사용여부', type: 'select', name: 'webPaymentUseYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 }]
          ]
        },
        {
          title: '대표 기본상품정보 (온라인 URL 결제용)',
          id: 'defaultProductCard',
          merchantOnly: true,
          rows: [
            [{ label: '상품명', type: 'text', name: 'defaultProductName', col: 2, placeholder: '대표 상품명' }, { label: '상품코드', type: 'text', name: 'defaultProductCode', col: 1 }, { label: '기본금액(원)', type: 'text', name: 'defaultProductAmount', col: 1, placeholder: '0' }, { label: '상품설명', type: 'text', name: 'defaultProductDesc', col: 4 }]
          ]
        },
        {
          title: '결제통보 URL',
          id: 'notifyUrlCard',
          merchantOnly: true,
          notice: '결제 응답을 가맹점에게 송부할 노티 주소. 등록 시 결제통보 URL관리에 자동 반영됩니다.',
          rows: [
            [{ label: 'URL Background', type: 'text', name: 'notifyUrlBackground', col: 5, placeholder: 'https://' }, { label: 'URL Result', type: 'text', name: 'notifyUrlResult', col: 5, placeholder: 'https://' }]
          ]
        },
        {
          type: 'branding',
          title: '브랜딩 설정',
          id: 'brandingCard',
          regionalOrMasterDistOnly: true,
          notice: '본사·총판만 설정 가능. 메인이미지=로그인 화면 왼쪽 배경, 로고=로그인창 상단·사이드바 상단.'
        },
        { title: '기타(본사)', id: 'regionalMiscCard', regionalOnly: true, notice: '메인이미지는 2MB, 로고이미지는 1MB까지 업로드 가능합니다. PNG파일을 추천합니다.', rows: [[{ label: 'COPYRIGHT', type: 'textarea', name: 'copyright', col: 6, placeholder: 'Copyright © 2025 ICOPAY Service by Ontheline Co., Ltd.' }, { label: '특이사항', type: 'textarea', name: 'remark', col: 6 }]] },
        { title: '첨부파일', rows: [[{ type: 'file', name: 'attach', col: 4, button: '추가' }]] },
        { type: 'pgInfoDisplay', title: '결제 URL', id: 'pgInfoCard', merchantOnly: true, notice: '가맹점만 표시됩니다.' }
      ],
      buttons: [{ id: 'compDetailListBtn', label: '목록', cls: 'btn-secondary' }, { id: 'compDetailSaveBtn', label: '저장', cls: 'btn-primary' }]
    },
    '/commission/commisionList': {
      searchRows: [
        [
          { label: '업체코드', type: 'text', name: 'searchCompId' },
          { label: '업체명', type: 'text', name: 'searchCompNm' }
        ],
        [
          { label: '적용일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate' },
          { type: 'searchBtn' }
        ]
      ],
      tableScrollable: true,
      noticeList: [
        '적용시작일을 비우면 저장 시점(서버 시각) 기준으로 적용됩니다.',
        '동일 가맹점에 미래 적용일이 중복되지 않도록 한 번에 한 건만 등록하는 것을 권장합니다.',
        '상위 조직 수수료 정책이 바뀌면 이후 신규 가맹점 등록 시 하위 배분 설정에 반영될 수 있습니다.'
      ],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'commissionSettingBtn', label: '수수료설정', cls: 'btn-info' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      /** 2단 헤더 좁은 컬럼: 한 줄 표시 + site.css .commission-split-grid */
      tableExtraClass: 'commission-split-grid',
      headerGroups: [
        { label: '총본사', keys: ['hqNm', 'hqRate', 'hqPerTxFee'] },
        { label: '본사', keys: ['regionalNm', 'regionalRate', 'regionalPerTxFee'] },
        { label: '총판', keys: ['masterNm', 'masterRate', 'masterPerTxFee'] },
        { label: '지사', keys: ['branchNm', 'branchRate', 'branchPerTxFee'] },
        { label: '대리점', keys: ['agencyNm', 'agencyRate', 'agencyPerTxFee'] },
        { label: '영업점', keys: ['salesOfficeNm', 'salesOfficeRate', 'salesOfficePerTxFee'] },
        { label: '합계', keys: ['totalNm', 'totalRate', 'totalPerTxFee'] },
        { label: '처리', keys: ['inlineActions'] }
      ],
      columns: [
        { key: '_chk', type: 'checkbox' },
        { key: 'rowNo', label: 'No.' },
        { key: 'compNm', label: '가맹점' },
        { key: 'compId', label: '업체코드' },
        { key: 'hqNm', label: '업체명' }, { key: 'hqRate', label: '요율%' }, { key: 'hqPerTxFee', label: '건당료' },
        { key: 'regionalNm', label: '업체명' }, { key: 'regionalRate', label: '요율%' }, { key: 'regionalPerTxFee', label: '건당료' },
        { key: 'masterNm', label: '업체명' }, { key: 'masterRate', label: '요율%' }, { key: 'masterPerTxFee', label: '건당료' },
        { key: 'branchNm', label: '업체명' }, { key: 'branchRate', label: '요율%' }, { key: 'branchPerTxFee', label: '건당료' },
        { key: 'agencyNm', label: '업체명' }, { key: 'agencyRate', label: '요율%' }, { key: 'agencyPerTxFee', label: '건당료' },
        { key: 'salesOfficeNm', label: '업체명' }, { key: 'salesOfficeRate', label: '요율%' }, { key: 'salesOfficePerTxFee', label: '건당료' },
        { key: 'totalNm', label: '업체명' }, { key: 'totalRate', label: '요율%' }, { key: 'totalPerTxFee', label: '건당료' },
        { key: 'applyDt', label: '적용시작일' },
        { key: 'inlineActions', type: 'commissionInlineActions', label: '처리' }
      ],
      hasCommissionHistoryTable: true,
      commissionHistory: {
        headerGroups: [
          { label: '총본사', keys: ['hqNm', 'hqRate', 'hqPerTxFee'] },
          { label: '본사', keys: ['regionalNm', 'regionalRate', 'regionalPerTxFee'] },
          { label: '총판', keys: ['masterNm', 'masterRate', 'masterPerTxFee'] },
          { label: '지사', keys: ['branchNm', 'branchRate', 'branchPerTxFee'] },
          { label: '대리점', keys: ['agencyNm', 'agencyRate', 'agencyPerTxFee'] },
          { label: '영업점', keys: ['salesOfficeNm', 'salesOfficeRate', 'salesOfficePerTxFee'] },
          { label: '합계', keys: ['totalNm', 'totalRate', 'totalPerTxFee'] }
        ],
        columns: [
          { key: 'rowNo', label: 'No.' },
          { key: 'compNm', label: '가맹점' },
          { key: 'startDttm', label: '시작일시' },
          { key: 'endDttm', label: '종료일시' },
          { key: 'hqNm', label: '업체명' }, { key: 'hqRate', label: '요율%' }, { key: 'hqPerTxFee', label: '건당료' },
          { key: 'regionalNm', label: '업체명' }, { key: 'regionalRate', label: '요율%' }, { key: 'regionalPerTxFee', label: '건당료' },
          { key: 'masterNm', label: '업체명' }, { key: 'masterRate', label: '요율%' }, { key: 'masterPerTxFee', label: '건당료' },
          { key: 'branchNm', label: '업체명' }, { key: 'branchRate', label: '요율%' }, { key: 'branchPerTxFee', label: '건당료' },
          { key: 'agencyNm', label: '업체명' }, { key: 'agencyRate', label: '요율%' }, { key: 'agencyPerTxFee', label: '건당료' },
          { key: 'salesOfficeNm', label: '업체명' }, { key: 'salesOfficeRate', label: '요율%' }, { key: 'salesOfficePerTxFee', label: '건당료' },
          { key: 'totalNm', label: '업체명' }, { key: 'totalRate', label: '요율%' }, { key: 'totalPerTxFee', label: '건당료' },
          { key: 'changedBy', label: '변경자' }
        ]
      }
    },
    '/comp/compInfoHistList': {
      searchRows: [
        [
          { label: '업체코드', type: 'text', name: 'searchCompId' },
          { label: '변경일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate' },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'chgType', label: '변경구분' }, { key: 'chgDt', label: '변경일시' }, { key: 'chgDesc', label: '변경내용' }]
    },
    '/calc/payList': {
      payListVariant: 'INTEGRATED',
      /** VIEW SETTING: 1행 제목·저장, 2행 컬럼 체크(줄바꿈) */
      tableColumnGuideTwoRow: true,
      searchFormClass: 'pay-mng-search-form',
      searchRows: [
        [
          { label: '거래인자', type: 'select', name: 'searchTranFactor', options: [{ v: '', t: '전체' }], size: 11 },
          { type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate' },
          { label: '업체명', type: 'select', name: 'searchCompNm', options: [{ v: '', t: '전체' }] },
          { label: '터미널ID', type: 'text', name: 'searchTmnId' }
        ],
        [
          { label: '결제구분', type: 'select', name: 'searchPayDivCd', options: [{ v: '', t: '전체' }, { v: '10', t: '결제' }, { v: '20', t: '취소' }], size: 11 },
          { label: '정산구분', type: 'select', name: 'searchPayProcCd', options: [{ v: '', t: '전체' }, { v: '10', t: '정산대기' }, { v: '20', t: '정산완료' }, { v: '30', t: '결제취소' }, { v: '40', t: '정산취소' }], size: 8 },
          { label: '단계별', type: 'select', name: 'searchStep', options: [{ v: '', t: '출판' }] },
          { type: 'text', name: 'searchKeyword', placeholder: '검색어', size: 17 }
        ],
        [
          { label: 'PG사', type: 'select', name: 'searchPg', options: [{ v: '', t: '전체' }], size: 11 },
          { label: '정산주기', type: 'select', name: 'searchCycle', options: CALC_CYCLE_SEARCH_OPTIONS },
          { label: '사업자번호', type: 'text', name: 'searchRegNo' },
          { label: '카드승인번호', type: 'text', name: 'searchCardAprvNo' },
          { type: 'searchBtn', label: 'Q 검색' }
        ]
      ],
      searchRows2: [],
      searchRows3: [],
      noticeList: [
        '통합 결제내역: 칠페이 API 동기화·노티 적재·URL직접결제 등 전 출처를 한 그리드에 표시합니다. 앞쪽 컬럼(거래일~Settled)은 칠페이 거래내역 시트 필드와 대응합니다.',
        '[후속조치]는 본사설정 > 전산노티·결제환경에서 기능을 켠 경우에만 동작합니다 (NOTI 환경설정과 동일).',
        '취소 건에 대한 정산 수수료 및 부가세는 정산 주기에 따라 반영됩니다.',
        '정산 주기 및 정산 수수료는 가맹점별로 상이할 수 있습니다.'
      ],
      summary: ['건수', '승인금액', '취소금액', '결제금액', '총수수료', '보류금액', '지급액'],
      buttons: [{ id: 'reclaimBtn', label: '상신회수', cls: 'btn-warning' }, { id: 'excelDownBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      /** 참고: 결제내역 UI 2단 헤더 — 정산주기 뒤 PG승인(금액·일시), 보류(금액·일시), 수수료(건·%) */
      headerGroups: [
        { label: '사업자', keys: ['compRegDivNm', 'compRegNo'] },
        { label: 'PG승인', keys: ['pgApproveAmt', 'payAprv'] },
        { label: '보류', keys: ['holdAmt', 'holdDttm'] },
        { label: '수수료', keys: ['feeCnt', 'feeRate'] }
      ],
      /** 앞쪽 고정 순서: 번호 → 업체명 → 업체코드 → 거래일 → 거래시간 → Route No → TransactionId(칠페이) */
      columns: [
        { key: '_chk', type: 'checkbox' },
        { key: 'rowNo', label: '번호' },
        { key: 'compNm', label: '업체명' },
        { key: 'compId', label: '업체코드' },
        { key: 'trnDate', label: '거래일' },
        { key: 'trnTime', label: '거래시간' },
        { key: 'routeNo', label: 'Route No' },
        { key: 'chillTransactionId', label: 'TransactionId(칠페이)' },
        { key: 'trnId', label: '거래번호(우리)' },
        { key: 'chillCustomer', label: 'Customer(칠페이)' },
        { key: 'orderNo', label: 'OrderNo' },
        { key: 'paymentChannel', label: 'Payment Channel' },
        { key: 'payCompletedAt', label: '결제시각' },
        { key: 'chillAmount', label: 'Amount' },
        { key: 'icopayAmt', label: 'ICOPAY' },
        { key: 'chillFeeAmt', label: 'Fee' },
        { key: 'totalAmt', label: 'TotalAmount' },
        { key: 'currency', label: 'Currency' },
        { key: 'chillPaymentStatus', label: 'Status' },
        { key: 'settledYn', label: 'Settled' },
        { key: 'compRegDivNm', label: '구분' },
        { key: 'compRegNo', label: '번호' },
        { key: 'payDivNm', label: '구분' },
        { key: 'payCard', label: '결제카드' },
        { key: 'cardAprvNo', label: '승인번호' },
        { key: 'payCardNo', label: '카드번호' },
        { key: 'instalMonth', label: '할부개월' },
        { key: 'payMethod', label: '결제수단' },
        { key: 'corpNm', label: '법인명' },
        { key: 'pgNm', label: 'PG사' },
        { key: 'terminalId', label: '단말기' },
        { key: 'calcCycle', label: '정산주기' },
        { key: 'pgApproveAmt', label: '금액' },
        { key: 'payAprv', label: '일시' },
        { key: 'holdAmt', label: '금액' },
        { key: 'holdDttm', label: '일시' },
        { key: 'feeCnt', label: '건' },
        { key: 'feeRate', label: '%' },
        { key: 'settleAmt', label: '지급액' },
        { key: 'calcDt', label: '지급일시' },
        { key: 'pgApproveNo', label: 'PG승인번호' },
        { key: 'productNm', label: '구매상품' },
        { key: 'customerNm', label: '고객명(결제자)' },
        { key: 'customerTel', label: '휴대폰(결제자)' },
        { key: 'regionalNm', label: '총판' },
        { key: 'masterNm', label: '지사' },
        { key: 'branchNm', label: '대리점' },
        { type: 'payActions', label: '후속조치', key: 'payActions' }
      ],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/calc/calcList': {
      searchFormClass: 'screen-search-form screen-distribution-search',
      tableScrollable: true,
      distributionThreeRowHeader: true,
      searchRows: [
        [
          { label: '조회기준', type: 'select', name: 'searchDateType', options: [
            { v: 'APPROVE', t: '승인일자' },
            { v: 'SETTLE', t: '정산일자' }
          ], size: 10 },
          { label: '기간', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate', quickdateLabels: ['오늘', '전일', '금주', '전주'], quickdateRanges: ['day', 'prevDay', 'weekCal', 'prevWeekCal'] },
          { label: '업체구분', type: 'select', name: 'searchCompDiv', options: [
            { v: '', t: '전체(단계별 합산)' },
            { v: 'REGIONAL', t: '본사' },
            { v: 'MASTER_DIST', t: '총판' },
            { v: 'BRANCH', t: '지사' },
            { v: 'AGENCY', t: '대리점' },
            { v: 'SALES_OFFICE', t: '영업점' }
          ] }
        ],
        [
          { label: '업체코드', type: 'text', name: 'searchCompId' },
          { label: '업체명', type: 'text', name: 'searchCompNm' },
          { type: 'searchBtn' }
        ]
      ],
      noticeList: [
        '유통망 정산: 로그인 소속 조직부터 그 하위(영업점)까지만 조회됩니다. 가맹점 단위 행은 표시되지 않으며, 하위 가맹 정산액이 조직 행에 합산됩니다.',
        '업체구분을 선택하면 해당 단계(예: 대리점) 조직만 한 행으로 보입니다. 조회기준·승인일자는 추후 거래일 기준 필터와 연동 예정이며, 현재는 정산일(calc_dt) 기준입니다.'
      ],
      summary: ['Total', '정산금액', '수수료', '지급액'],
      buttons: [
        { id: 'printBtn', label: '인쇄설정', cls: 'btn-success' },
        { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      columns: [
        { key: 'rowNo', label: 'No.' },
        { key: 'settleMonth', label: '정산월' },
        { key: 'orgDivNm', label: '구분' },
        { key: 'regionalNm', label: '본사' },
        { key: 'masterNm', label: '총판' },
        { key: 'branchNm', label: '지사' },
        { key: 'agencyNm', label: '대리점' },
        { key: 'compId', label: '업체코드' },
        { key: 'aprvCnt', label: '승인건수' },
        { key: 'aprvAmt', label: '승인금액' },
        { key: 'aprvFeeCnt', label: '승인수수료건' },
        { key: 'aprvFeePct', label: '승인수수료%' },
        { key: 'aprvFeeSum', label: '승인수수료합계' },
        { key: 'aprvFeeVat', label: '승인부가세' },
        { key: 'canCnt', label: '취소건수' },
        { key: 'canAmt', label: '취소금액' },
        { key: 'canFeeCnt', label: '취소수수료건' },
        { key: 'canFeePct', label: '취소수수료%' },
        { key: 'canFeeSum', label: '취소수수료합계' },
        { key: 'canFeeVat', label: '취소부가세' },
        { key: 'settleAmt', label: '정산금액' }
      ]
    },
    '/calc/calcGmList': {
      searchRows: [
        [
          { label: '정산일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate', col: 5 },
          { type: 'quickdate' }
        ],
        [
          { label: '가맹점코드', type: 'text', name: 'searchCompId' },
          { label: '업체명', type: 'text', name: 'searchCompNm' },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수', '금액', '수수료금액', '수수료부가세', '보류금액', '정산금액'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      headerGroups: [
        { label: '사업자', keys: ['bizType', 'bizNo'] },
        { label: 'PG승인', keys: ['amount', 'payNo'] },
        { label: '수수료', keys: ['feeCnt', 'feeRate', 'feeAmt', 'feeVat', 'holdRate', 'holdAmt'] }
      ],
      columns: [
        { key: '_chk', type: 'checkbox' },
        { key: 'rowNo', label: '번호' },
        { key: 'compNm', label: '업체명' },
        { key: 'compId', label: '업체코드' },
        { key: 'bizType', label: '사업자구분' },
        { key: 'bizNo', label: '사업자번호' },
        { key: 'payDivNm', label: '구분' },
        { key: 'payCard', label: '결제카드' },
        { key: 'cardAprvNo', label: '카드승인번호' },
        { key: 'payCardNo', label: '카드번호' },
        { key: 'instalMonth', label: '할부개월' },
        { key: 'payMethod', label: '결제수단' },
        { key: 'corpNm', label: '법인명' },
        { key: 'pgNm', label: 'PG사명' },
        { key: 'terminalId', label: '터미널ID' },
        { key: 'amount', label: '금액' },
        { key: 'payNo', label: '번호' },
        { key: 'feeCnt', label: '수수료(건)' },
        { key: 'feeRate', label: '수수료(%)' },
        { key: 'feeAmt', label: '수수료(금액)' },
        { key: 'feeVat', label: '수수료(부가세)' },
        { key: 'holdRate', label: '보류율(%)' },
        { key: 'holdAmt', label: '보류금액' },
        { key: 'calcCycle', label: '정산주기' },
        { key: 'settleAmt', label: '정산금액' },
        { key: 'calcDt', label: '정산일시' },
        { key: 'approveDt', label: '승인일시' },
        { key: 'cancelDt', label: '취소일시' },
        { key: 'payStatus', label: '지급상태' },
        { key: 'productNm', label: '구매상품' },
        { key: 'customerNm', label: '고객명(결제자)' },
        { key: 'customerTel', label: '휴대폰(결제자)' },
        { key: 'regionalNm', label: '총판' },
        { key: 'masterNm', label: '지사' },
        { key: 'branchNm', label: '대리점' }
      ]
    },
    '/calc/compPointMngList': {
      searchRows: [
        [
          { label: '업체코드', type: 'text', name: 'searchCompId' },
          { label: '적용일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate' },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수', '환수금액'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [
        { key: '_chk', type: 'checkbox' },
        { key: 'rowNo', label: '번호' },
        { key: 'compNm', label: '업체명' },
        { key: 'compId', label: '업체코드' },
        { key: 'calcDt', label: '발생일자' },
        { key: 'statusNm', label: '처리구분' },
        { key: 'settleAmt', label: '원거래금액' },
        { key: 'recallAmt', label: '환수금액' },
        { key: 'deductAmt', label: '정산반영(-)' },
        { key: 'feeIncludedYn', label: '수수료포함' },
        { key: 'vatAppliedYn', label: 'VAT적용' }
      ]
    },
    '/calc/feeList': {
      searchRows: [
        [
          { label: '업체코드', type: 'text', name: 'searchCompId' },
          { label: '업체명', type: 'text', name: 'searchCompNm' },
          { label: '거래일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate' },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수', '총수수료', '부가세'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [
        { key: '_chk', type: 'checkbox' },
        { key: 'rowNo', label: '번호' },
        { key: 'compNm', label: '업체명' },
        { key: 'compId', label: '업체코드' },
        { key: 'trnDate', label: '거래일자' },
        { key: 'trnId', label: '거래ID' },
        { key: 'statusNm', label: '상태' },
        { key: 'amount', label: '결제금액' },
        { key: 'perTxFee', label: '건당수수료' },
        { key: 'usageFee', label: '이용수수료' },
        { key: 'failFee', label: '실패수수료' },
        { key: 'cancelFee', label: '취소수수료' },
        { key: 'refundFee', label: '환불수수료' },
        { key: 'payFeeRate', label: '결제수수료율(%)' },
        { key: 'payFee', label: '결제수수료' },
        { key: 'settlementPerTxFee', label: '정산수수료' },
        { key: 'usdtFee', label: 'USDT수수료' },
        { key: 'fxFee', label: 'FX수수료' },
        { key: 'totalFee', label: '총수수료' },
        { key: 'feeVat', label: '부가세' },
        { key: 'vatAppliedYn', label: 'VAT적용' }
      ]
    },
    '/calc/balanceList': {
      searchRows: [
        [
          { label: '업체코드', type: 'text', name: 'searchCompId' },
          { label: '업체명', type: 'text', name: 'searchCompNm' },
          { label: '검색조건', type: 'select', name: 'searchCondition', options: [{ v: '', t: '전체' }, { v: 'ETC', t: 'ETC' }, { v: '카드', t: '카드' }] },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수', '충전내역합계'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }, { id: 'chargeBtn', label: '충전실행', cls: 'btn-success' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'condition', label: '검색조건' }, { key: 'chargeType', label: '거래구분' }, { key: 'payMethod', label: '결제수단' }, { key: 'chargeNm', label: '거래명칭' }, { key: 'chargeAmt', label: '충전내역' }, { key: 'sumChargeAmt', label: '충전내역합계' }]
    },
    '/calc/unpaidMng': {
      searchRows: [
        [
          { label: '업체코드', type: 'text', name: 'searchCompId' },
          { label: '업체명', type: 'text', name: 'searchCompNm' },
          { label: '잔액', type: 'text', name: 'searchBalance' },
          { label: '미수금', type: 'text', name: 'searchUnpaid' },
          { label: '미수금차감', type: 'select', name: 'searchDeductStatus', options: [{ v: '', t: '미수금차감' }, { v: 'Y', t: '카드승인' }, { v: 'N', t: '미수금차감' }] },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수', '잔액합계', '미수금합계'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'settleAmt', label: '정산잔액' }, { key: 'deductCnt', label: '미수금' }, { key: 'deductStatus', label: '미수금차감' }]
    },
    '/calc/balcInfo': {
      searchRows: [
        [
          { label: '업체코드', type: 'text', name: 'searchCompId' },
          { label: '업체명', type: 'text', name: 'searchCompNm' },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['잔액합계', '미수금합계', '차감합계', '가용잔액합계'],
      buttons: [
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' },
        { id: 'balanceDeductBtn', label: '선택차감', cls: 'btn-warning' },
        { id: 'balanceManualDeductBtn', label: '직접입력차감', cls: 'btn-outline-warning' }
      ],
      columns: [
        { key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' },
        { key: 'balcAmount', label: '잔액(지급보류)' }, { key: 'unpaidAmount', label: '미수금' }, { key: 'deductedAmount', label: '차감누계' }, { key: 'remainAmount', label: '가용잔액' }
      ]
    },
    '/calc/exCalcList': {
      searchRows: [
        [
          { label: '정산기간', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate' },
          { label: '업체코드', type: 'text', name: 'searchCompId', placeholder: '가맹점 코드' },
          { type: 'searchBtn' }
        ]
      ],
      summary: [],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'exCalcBtn', label: '정산실행', cls: 'btn-warning' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'calcDt', label: '정산일자' }, { key: 'targetAmt', label: '정산대상금액' }, { key: 'totalFee', label: '공제수수료' }, { key: 'rollingReserveAmt', label: '롤링보류' }, { key: 'payAmount', label: '지급액' }, { key: 'status', label: '상태' }]
    },
    '/calc/settlementReport': {
      noticeList: [
        '[리포트 형식] 가맹점 정산 리포트: 총본사·본사·총판 등이 소속 가맹에 보내는 정산 형식. 본사 지급 리포트: 총본사가 본사(REGIONAL)에 지급할 금액을 본사 단위로 합산(총본사·본사 로그인만 선택 가능).',
        '[하위 구분] 정산집계·정산실시·정산집계표. 예치·Processing·건당요금·+7영업일은 백엔드 상수이며 응답 meta에 안내가 있습니다.'
      ],
      searchRows: [
        [
          { label: '리포트 형식', type: 'select', name: 'searchReportKind', options: [{ v: 'MERCHANT_STMT', t: '가맹점 정산 리포트' }, { v: 'REGIONAL_PAYOUT', t: '본사 지급 리포트(총본사→본사)' }], size: 22 },
          { label: '리포트구분', type: 'select', name: 'searchReportSub', options: [{ v: 'AGG', t: '정산집계' }, { v: 'EXE', t: '정산실시' }, { v: 'SUM', t: '정산집계표' }], size: 12 },
          { label: '결제일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate' }
        ],
        [
          { label: '가맹점코드', type: 'text', name: 'searchCompId', placeholder: '가맹점 코드' },
          { label: '총판(상위)코드', type: 'text', name: 'searchMasterId', placeholder: '총판 조직 코드' },
          { label: '본사코드', type: 'text', name: 'searchRegionalId', placeholder: '본사 지급 리포트 시 필터' },
          { label: '통화', type: 'select', name: 'searchCurType', options: [{ v: '', t: '전체' }, { v: 'KRW', t: 'KRW' }, { v: 'USD', t: 'USD' }, { v: 'JPY', t: 'JPY' }, { v: 'THB', t: 'THB' }], size: 8 },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수', '결제액', '환불', '순액', '정산금'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [],
      columnsBySub: {
        AGG: [
          { key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' },
          { key: 'payDate', label: '결제일' }, { key: 'compNm', label: '가맹점명' }, { key: 'compId', label: '가맹코드' }, { key: 'curType', label: '통화' },
          { key: 'grossPay', label: '결제액' }, { key: 'refundAmt', label: '환불/취소' }, { key: 'netPay', label: '순결제' },
          { key: 'depositAmt', label: '예치(10%)' }, { key: 'processingFeeTotal', label: 'Processing(5.6%)' },
          { key: 'txnFeeTotal', label: '건당수수료합' }, { key: 'settlementAmt', label: '정산금(추정)' },
          { key: 'settlementDueDt', label: '지급예정일(+7영업일)' }, { key: 'settledYn', label: '정산완료' }
        ],
        EXE: [
          { key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' },
          { key: 'calcDt', label: '정산일' }, { key: 'compNm', label: '가맹점명' }, { key: 'compId', label: '가맹코드' },
          { key: 'approveAmt', label: '승인합' }, { key: 'cancelAmt', label: '취소합' }, { key: 'netPay', label: '순액' },
          { key: 'depositAmt', label: '예치(10%)' }, { key: 'processingFeeTotal', label: 'Processing(5.6%)' },
          { key: 'payAmount', label: '지급액(배치)' }, { key: 'totalFee', label: '공제수수료' }, { key: 'rollingReserveAmt', label: '롤링보류' },
          { key: 'settlementDueDt', label: '지급예정일(+7영업일)' }, { key: 'settledYn', label: '완료' }, { key: 'status', label: '상태' }
        ],
        SUM: [
          { key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' },
          { key: 'periodFrom', label: '기간FROM' }, { key: 'periodTo', label: '기간TO' },
          { key: 'grossPay', label: '결제액합' }, { key: 'refundAmt', label: '환불합' }, { key: 'netPay', label: '순결제합' },
          { key: 'depositAmt', label: '예치합' }, { key: 'processingFeeTotal', label: 'Processing합' }, { key: 'txnFeeTotal', label: '건당수수료합' },
          { key: 'settlementAmt', label: '정산금합(추정)' }, { key: 'approveCnt', label: '승인건' }, { key: 'refundCnt', label: '환불건' }, { key: 'rowCount', label: '집계행수' }
        ]
      },
      columnsRegionalPayout: {
        AGG: [
          { key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' },
          { key: 'payDate', label: '결제일' }, { key: 'regionalCompId', label: '본사코드' }, { key: 'regionalNm', label: '본사명' }, { key: 'merchantCnt', label: '가맹점수' }, { key: 'curType', label: '통화' },
          { key: 'grossPay', label: '결제액합' }, { key: 'refundAmt', label: '환불/취소' }, { key: 'netPay', label: '순결제' },
          { key: 'depositAmt', label: '예치(10%)' }, { key: 'processingFeeTotal', label: 'Processing(5.6%)' },
          { key: 'txnFeeTotal', label: '건당수수료합' }, { key: 'settlementAmt', label: '지급액(추정)' },
          { key: 'settlementDueDt', label: '지급예정일(+7영업일)' }, { key: 'settledYn', label: '정산완료' }
        ],
        EXE: [
          { key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' },
          { key: 'calcDt', label: '정산일' }, { key: 'regionalCompId', label: '본사코드' }, { key: 'regionalNm', label: '본사명' },
          { key: 'batchRunCnt', label: '배치건수' }, { key: 'approveAmt', label: '승인합' }, { key: 'cancelAmt', label: '취소합' }, { key: 'netPay', label: '순액' },
          { key: 'payAmount', label: '지급액합' }, { key: 'totalFee', label: '공제수수료합' }, { key: 'rollingReserveAmt', label: '롤링보류합' },
          { key: 'settlementDueDt', label: '지급예정일(+7영업일)' }, { key: 'settledYn', label: '완료' }, { key: 'status', label: '상태' }
        ],
        SUM: [
          { key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' },
          { key: 'periodFrom', label: '기간FROM' }, { key: 'periodTo', label: '기간TO' },
          { key: 'grossPay', label: '결제액합' }, { key: 'refundAmt', label: '환불합' }, { key: 'netPay', label: '순결제합' },
          { key: 'depositAmt', label: '예치합' }, { key: 'processingFeeTotal', label: 'Processing합' }, { key: 'txnFeeTotal', label: '건당수수료합' },
          { key: 'settlementAmt', label: '지급액합(추정)' }, { key: 'approveCnt', label: '승인건' }, { key: 'refundCnt', label: '환불건' }, { key: 'rowCount', label: '집계행수' }
        ]
      },
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/pay/payHoldList': {
      searchRows: [
        [
          { label: '조회일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate', col: 5 },
          { type: 'quickdate' }
        ],
        [
          { label: '업체명', type: 'text', name: 'searchCompNm' },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수', '보류금액'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'holdDt', label: '보류일시' }, { key: 'holdAmount', label: '보류금액' }, { key: 'holdReason', label: '보류사유' }]
    },
    '/calc/collateralList': {
      noticeList: [
        '담보금(롤링): 결제(승인) 건별로 정산 실행 시 설정된 비율(%)만큼 예치되며, 보류 영업일(주말 제외·공휴일 미반영) 후 해지일에 정산 실행하면 지급액에 합산됩니다.',
        '비율·보류 일수: 본사설정 수수료정책의 롤링(담보금) 또는 가맹점 정산설정에서 「보류율 본사정책 따름=N」일 때 개별 보류율·일수를 사용합니다.'
      ],
      searchRows: [
        [
          { label: '적용일(담보)', type: 'daterange', from: 'searchFromDate', to: 'searchToDate', col: 5 },
          { type: 'quickdate' }
        ],
        [
          { label: '업체코드', type: 'text', name: 'searchCompId', col: 3 },
          { label: '업체명', type: 'text', name: 'searchCompNm', col: 3 },
          { label: '상태', type: 'select', name: 'searchStatus', options: [{ v: '', t: '전체' }, { v: 'HOLD', t: '보류' }, { v: 'RELEASED', t: '해지' }], col: 2 },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수', '담보금액'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [
        { key: '_chk', type: 'checkbox' },
        { key: 'rowNo', label: '번호' },
        { key: 'compNm', label: '업체명' },
        { key: 'compId', label: '업체코드' },
        { key: 'trnId', label: '거래ID' },
        { key: 'reserveAmt', label: '담보금액' },
        { key: 'rollingPct', label: '적용비율(%)' },
        { key: 'holdBusinessDays', label: '보류영업일' },
        { key: 'holdStartDt', label: '적용일' },
        { key: 'releaseDt', label: '해지(반환)일' },
        { key: 'remainingBizDays', label: '남은영업일' },
        { key: 'statusNm', label: '상태' },
        { key: 'releasedAt', label: '해지처리일시' },
        { key: 'settlementNote', label: '정산반영안내' }
      ]
    },
    '/noti/notiUrlMng': {
      searchRows: [
        [
          { label: '업체코드', type: 'text', name: 'searchCompId' },
          { label: 'URL구분', type: 'select', name: 'searchUrlType', options: [{ v: '', t: '전체' }, { v: 'PAY', t: '결제통보' }, { v: 'BACKGROUND', t: 'URL Background' }, { v: 'RESULT', t: 'URL Result' }], size: 8 },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'urlType', label: 'URL구분' }, { key: 'notiUrl', label: '통보URL' }, { key: 'useYn', label: '사용여부' }]
    },
    '/noti/notiSendMngList': {
      searchRows: [
        [
          { label: '전송일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate', col: 5 },
          { type: 'quickdate' }
        ],
        [
          { label: '업체명', type: 'text', name: 'searchCompNm' },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수', '성공', '실패'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'sendDt', label: '전송일시' }, { key: 'result', label: '결과' }, { key: 'retryCnt', label: '재전송횟수' }]
    },
    '/noti/notiCashReceiptUrlMng': {
      searchRows: [
        [
          { label: '업체코드', type: 'text', name: 'searchCompId' },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'notiUrl', label: '현금영수증 통보URL' }, { key: 'useYn', label: '사용여부' }]
    },
    '/noti/notiCashReceiptSendMngList': {
      searchRows: [
        [
          { label: '전송일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate', col: 5 },
          { type: 'quickdate' }
        ],
        [
          { label: '업체명', type: 'text', name: 'searchCompNm' },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'sendDt', label: '전송일시' }, { key: 'result', label: '결과' }]
    },
    '/user/userMng': {
      rowGroupByKey: 'permissionGroupNm',
      searchRows: [
        [
          { label: '사용자ID', type: 'text', name: 'searchUserId' },
          { label: '사용자명', type: 'text', name: 'searchUserNm' },
          { label: '업체코드', type: 'text', name: 'searchCompId' },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'addBtn', label: '등록', cls: 'btn-success' }],
      columns: [
        { key: '_chk', type: 'checkbox' },
        { key: 'rowNo', label: '번호' },
        { key: 'compNm', label: '업체명' },
        { key: 'compId', label: '업체코드' },
        { key: 'userId', label: '사용자ID' },
        { key: 'userNm', label: '사용자명' },
        { key: 'permissionGroupNm', label: '권한그룹' },
        { key: 'roleNm', label: '역할' },
        { key: 'otpRegisteredYn', label: 'OTP' },
        { key: 'useYn', label: '사용여부' },
        { key: 'id', type: 'userResetPassword', label: '비밀번호' },
        { key: 'id', type: 'userDelete', label: '삭제' }
      ]
    },
    '/set/gridSetMng': {
      searchRows: [
        [
          { label: '메뉴', type: 'select', name: 'searchMenuId', options: [{ v: '', t: '선택' }, { v: 'M0301', t: '결제내역' }, { v: 'M0404', t: '유통망정산내역' }] },
          { type: 'searchBtn' }
        ]
      ],
      summary: [],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'saveBtn', label: '저장', cls: 'btn-primary' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'sortOrder', label: '순서' }, { key: 'colId', label: '항목ID' }, { key: 'colNm', label: '항목명' }, { key: 'dispYn', label: '표시여부' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/calc/withdrawList': {
      searchRows: [[{ label: '조회일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'withdrawDt', label: '출금일시' }, { key: 'amount', label: '출금금액' }, { key: 'status', label: '상태' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/calc/salesByComp': {
      searchRows: [[{ label: '조회일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'salesAmt', label: '매출금액' }, { key: 'regDt', label: '집계일시' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/calc/payerSum': {
      searchRows: [[{ label: '조회일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'payerId', label: '결제자ID' }, { key: 'totalAmt', label: '누적금액' }, { key: 'cnt', label: '건수' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/calc/withdrawByAcct': {
      searchRows: [[{ label: '조회일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'acctNo', label: '출금계좌' }, { key: 'sumAmt', label: '집계금액' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/support/complaintList': {
      searchRows: [[{ label: '접수일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'complaintNo', label: '민원번호' }, { key: 'title', label: '제목' }, { key: 'regDt', label: '접수일' }, { key: 'status', label: '처리상태' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/comp/compInfo': {
      searchRows: [[{ label: '업체코드', type: 'text', name: 'searchCompId' }, { label: '업체명(본사명)', type: 'text', name: 'searchCompNm' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'compInfoDetailBtn', label: '상세(지역본사정보)', cls: 'btn-info' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명(본사명)' }, { key: 'compId', label: '업체코드' }, { key: 'compDivNm', label: '업체구분' }, { key: 'regNo', label: '사업자번호' }, { key: 'regDt', label: '등록일' }],
      emptyMessage: '조회된 데이터가 없습니다.',
      hasCompInfoDetailForm: true,
      compInfoDetailFormSections: [
        {
          title: '본사 정보 상세 (업체정보조회)',
          notice: '상위 본사(우리)가 권한을 준 회사의 정보입니다. 그리드에서 한 건 선택 후 [상세] 버튼으로 조회·수정합니다.',
          rows: [
            [{ label: '업체코드', type: 'text', name: 'compId', col: 2, readonly: true }, { label: '업체구분', type: 'select', name: 'compDiv', options: [{ v: '', t: '선택' }, { v: 'REGIONAL', t: '본사' }, { v: 'MASTER_DIST', t: '총판' }, { v: 'BRANCH', t: '지사' }, { v: 'AGENCY', t: '대리점' }, { v: 'SALES_OFFICE', t: '영업점' }, { v: 'MERCHANT', t: '가맹점' }], col: 2 }],
            [{ label: '업체명(본사명)*', type: 'text', name: 'compNm', col: 2 }, { label: '사업자번호*', type: 'regNoWithType', name: 'regNo', col: 2 }],
            [{ label: '업태', type: 'text', name: 'bizType', col: 2 }, { label: '종목', type: 'text', name: 'industry', col: 2 }],
            [{ label: '대표자명*', type: 'text', name: 'ceoNm', col: 2 }, { label: '휴대폰*', type: 'text', name: 'ceoMobile', col: 2 }, { label: '업체전화*', type: 'text', name: 'compTel', col: 2 }, { label: '팩스', type: 'text', name: 'fax', col: 2 }, { label: '이메일', type: 'text', name: 'email', col: 2 }, { label: '비고', type: 'text', name: 'remark', col: 2 }],
            [{ type: 'countryAddressRow', zipLabel: '우편번호*', addrLabel: '주소*', addrDetailLabel: '상세주소', addrEtcLabel: '기타' }],
            [{ label: '사용여부*', type: 'select', name: 'useYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 }, { label: '로그인ID*', type: 'text', name: 'loginId', col: 2 }, { label: '비밀번호', type: 'passwordReset', name: 'pwdReset', col: 2 }],
            [{ label: '사업자형태', type: 'text', name: 'bizNature', col: 2 }, { label: '취급물품', type: 'text', name: 'product', col: 2 }],
            [{ label: '대표사이트', type: 'text', name: 'homepage', col: 2 }, { label: '정산담당자명', type: 'text', name: 'settleName', col: 2 }],
            [{ label: '정산담당자연락처', type: 'text', name: 'settleTelNo', col: 2 }],
            [{ label: '계좌은행', type: 'select', name: 'bankCd', options: [{ v: '', t: '선택' }, { v: '04', t: '국민' }, { v: '20', t: '우리' }, { v: '81', t: 'KEB하나' }, { v: '88', t: '신한' }, { v: '11', t: 'NH농협' }], col: 2 }, { label: '이체수수료(원)', type: 'text', name: 'transferFee', col: 2 }],
            [{ label: '계좌번호*', type: 'text', name: 'accountNo', col: 2 }, { label: '예금주*', type: 'text', name: 'accountHolder', col: 2 }],
            [{ label: '수수료 설정 권한', type: 'select', name: 'commissionConfigAllowed', options: [{ v: 'N', t: '미부여' }, { v: 'Y', t: '부여' }], col: 2 }, { label: '기준 화폐1', type: 'select', name: 'baseCurrency1', options: [{ v: '', t: '선택' }, { v: 'KRW', t: 'KRW (원)' }, { v: 'USD', t: 'USD (달러)' }, { v: 'JPY', t: 'JPY (엔)' }, { v: 'THB', t: 'THB (바트)' }, { v: 'EUR', t: 'EUR (유로)' }], col: 2 }, { label: '기준 화폐2', type: 'select', name: 'baseCurrency2', options: [{ v: '', t: '선택' }, { v: 'KRW', t: 'KRW (원)' }, { v: 'USD', t: 'USD (달러)' }, { v: 'JPY', t: 'JPY (엔)' }, { v: 'THB', t: 'THB (바트)' }, { v: 'EUR', t: 'EUR (유로)' }], col: 2 }, { label: '기준 화폐3', type: 'select', name: 'baseCurrency3', options: [{ v: '', t: '선택' }, { v: 'KRW', t: 'KRW (원)' }, { v: 'USD', t: 'USD (달러)' }, { v: 'JPY', t: 'JPY (엔)' }, { v: 'THB', t: 'THB (바트)' }, { v: 'EUR', t: 'EUR (유로)' }], col: 2 }],
            [{ label: '특이사항', type: 'textarea', name: 'remark', col: 6 }]
          ]
        },
        {
          type: 'pgInfoDisplay',
          title: '결제대행사정보',
          id: 'pgInfoCard',
          notice: '가맹점만 표시됩니다. 결제 URL은 간편결제용으로, API 연동과 별도로 가맹점 생성 시 즉시 결제 페이지를 제공합니다.'
        }
      ],
      compInfoDetailButtons: [{ id: 'compInfoUpdateBtn', label: '수정 저장', cls: 'btn-primary' }]
    },
    '/comp/compMng': {
      searchRows: [[{ label: '업체코드', type: 'text', name: 'searchCompId' }, { label: '업체명', type: 'text', name: 'searchCompNm' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'compRegBtn', label: '등록', cls: 'btn-danger' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'compDivNm', label: '업체구분' }, { key: 'regDt', label: '등록일' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/comp/compChangeHistory': {
      searchRows: [[{ label: '업체코드', type: 'text', name: 'searchCompId' }, { label: '변경일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'chgDt', label: '변경일시' }, { key: 'chgItem', label: '변경항목' }, { key: 'beforeVal', label: '변경전' }, { key: 'afterVal', label: '변경후' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/calc/offsetCancelList': {
      searchRows: [[{ label: '조회일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }], [{ label: '업체명', type: 'text', name: 'searchCompNm' }, { type: 'searchBtn' }]],
      summary: ['건수', '취소금액'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'cancDt', label: '취소일시' }, { key: 'cancAmount', label: '취소금액' }, { key: 'paySeq', label: '원거래번호' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/calc/urlPayList': {
      searchRows: [[{ label: '조회일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }], [{ label: '업체명', type: 'text', name: 'searchCompNm' }, { type: 'searchBtn' }]],
      summary: ['건수', '금액'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'payDt', label: '결제일시' }, { key: 'orderNo', label: '주문번호' }, { key: 'amount', label: '금액' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/settlement/distributionList': {
      searchRows: [[{ label: '정산일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수', '정산금액', '수수료', '지급액'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/settlement/franchiseList': {
      searchRows: [[{ label: '정산일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수', '금액', '수수료금액', '수수료부가세', '보류금액', '정산금액'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/settlement/recallMng': {
      searchRows: [[{ label: '조회일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'calcDt', label: '정산일자' }, { key: 'settleAmt', label: '정산잔액' }, { key: 'recallAmt', label: '미수금' }, { key: 'deductAmt', label: '미수금 차감' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/settlement/balanceMng': {
      searchRows: [[{ label: '조회일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'balcAmount', label: '잔액' }, { key: 'unpaidAmount', label: '미수금' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/settlement/execute': {
      searchRows: [[{ label: '정산대상일', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'searchBtn' }]],
      summary: [],
      buttons: [{ id: 'searchBtn', label: '조회', cls: 'btn-primary' }, { id: 'executeBtn', label: '정산실행', cls: 'btn-danger' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'calcDt', label: '정산일' }, { key: 'targetAmt', label: '정산대상금액' }, { key: 'totalFee', label: '공제수수료' }, { key: 'rollingReserveAmt', label: '롤링보류' }, { key: 'payAmount', label: '지급액' }, { key: 'status', label: '상태' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/settlement/holdList': {
      searchRows: [[{ label: '조회일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'holdDt', label: '보류일' }, { key: 'holdAmount', label: '보류금액' }, { key: 'holdReason', label: '보류사유' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/notify/payUrlMng': {
      searchRows: [[{ label: '업체코드', type: 'text', name: 'searchCompId' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'addBtn', label: '등록', cls: 'btn-success' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'notifyUrl', label: '결제통보 URL' }, { key: 'useYn', label: '사용여부' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/notify/paySendMng': {
      searchRows: [[{ label: '전송일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'sendDt', label: '전송일시' }, { key: 'result', label: '결과' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/notify/cashReceiptUrlMng': {
      searchRows: [[{ label: '업체코드', type: 'text', name: 'searchCompId' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'addBtn', label: '등록', cls: 'btn-success' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'notifyUrl', label: '현금영수증통보 URL' }, { key: 'useYn', label: '사용여부' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/notify/cashReceiptSendMng': {
      searchRows: [[{ label: '전송일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'sendDt', label: '전송일시' }, { key: 'result', label: '결과' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/user/menuOrderMng': {
      searchRows: [[{ label: '메뉴', type: 'select', name: 'searchMenuId', options: [{ v: '', t: '선택' }] }, { type: 'searchBtn' }]],
      summary: [],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'saveBtn', label: '저장', cls: 'btn-primary' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'sortOrder', label: '순서' }, { key: 'colId', label: '항목ID' }, { key: 'colNm', label: '항목명' }, { key: 'dispYn', label: '표시여부' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/risk/list': {
      searchRows: [[{ label: '업체코드', type: 'text', name: 'searchCompId' }, { label: '리스크구분', type: 'select', name: 'searchRiskDiv', options: [{ v: '', t: '전체' }] }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'riskDiv', label: '리스크구분' }, { key: 'riskDesc', label: '내용' }, { key: 'regDt', label: '등록일' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    }
  };

  /** 결제관리: 통합 결제내역과 동일 UI, payListVariant만 다름 (docs/결제관리_기획_NOTI참고.md) */
  (function mergePayListVariants() {
    var base = MENU_SCREENS['/calc/payList'];
    if (!base) return;
    /** @param keepPayActions true면 통합 결제내역과 컬럼 100% 동일(후속조치 포함). 노티내역 전용. */
    function cloneWith(v, notices, keepPayActions) {
      var o = JSON.parse(JSON.stringify(base));
      o.payListVariant = v;
      if (notices && notices.length) o.noticeList = notices;
      if (!keepPayActions) {
        o.columns = (o.columns || []).filter(function (col) { return col.type !== 'payActions'; });
      }
      return o;
    }
    MENU_SCREENS['/calc/payNotiList'] = cloneWith('NOTI', [
      '노티내역: 통합 결제내역과 동일한 그리드입니다(칠페이 시트 컬럼·2단 헤더·요약바·후속조치 포함). 조회만 origin=NOTI(전산 노티 적재)로 제한됩니다.',
      'ziobiz/NOTI 종합거래의 노티거래내역과 동일 성격의 데이터입니다.',
      '[후속조치]는 본사설정 > 전산노티·결제환경에서 기능을 켠 경우에만 동작합니다 (NOTI 환경설정과 동일).',
      '취소 건에 대한 정산 수수료 및 부가세는 정산 주기에 따라 반영됩니다.',
      '정산 주기 및 정산 수수료는 가맹점별로 상이할 수 있습니다.'
    ], true);
    MENU_SCREENS['/calc/paySuccessList'] = cloneWith('SUCCESS', ['성공내역: 통합 결제내역에서 승인 성공(결제) 상태만 간추렸습니다.']);
    MENU_SCREENS['/calc/payFailList'] = cloneWith('FAIL', ['실패내역: 통합 결제내역에서 실패·거절만 간추렸습니다.']);
    MENU_SCREENS['/calc/payRefundList'] = cloneWith('REFUND', ['환불내역: 통합 결제내역에서 환불만 간추렸습니다.']);
    MENU_SCREENS['/calc/payForceRefundList'] = cloneWith('FORCE_REFUND', ['강제환불: 통합 결제내역에서 강제환불만 간추렸습니다.']);
    MENU_SCREENS['/calc/payCancelList'] = cloneWith('CANCEL', ['취소내역: 통합 결제내역에서 취소만 간추렸습니다.']);
    MENU_SCREENS['/calc/offsetCancList'] = cloneWith('OFFSET_CANCEL', ['상계취소내역: 정산 상계 처리용 — 승인 성공(결제)을 제외한 전 건(실패·환불·강제환불·취소·기타)을 한 화면에서 봅니다. 이후 빈도·집계로 상계에 활용합니다.']);
    MENU_SCREENS['/pay/easyPay'] = cloneWith('URL_PAY', ['URL결제내역: 가맹점 API연동 노티 외, 플랫폼이 칠페이 결제 API로 발급한 결제수소(URL)로 발생한 전 건(성공·실패·환불·취소 등). 통합 결제내역에도 포함되며, 여기서는 origin=URL 만 조회합니다.']);
  })();

  /** 정산 메뉴(/settlement/*) 중 /calc/*와 동일 API·그리드를 쓰는 화면은 컬럼을 복제해 드리프트를 막음 */
  (function mirrorSettlementScreensToCalc() {
    try {
      var gm = MENU_SCREENS['/calc/calcGmList'];
      var fr = MENU_SCREENS['/settlement/franchiseList'];
      if (gm && fr && gm.columns && gm.columns.length) {
        fr.columns = JSON.parse(JSON.stringify(gm.columns));
        fr.headerGroups = gm.headerGroups ? JSON.parse(JSON.stringify(gm.headerGroups)) : fr.headerGroups;
        fr.summary = gm.summary ? gm.summary.slice() : fr.summary;
      }
      var cl = MENU_SCREENS['/calc/calcList'];
      var dist = MENU_SCREENS['/settlement/distributionList'];
      if (cl && dist && cl.columns && cl.columns.length) {
        dist.columns = JSON.parse(JSON.stringify(cl.columns));
        dist.summary = cl.summary ? cl.summary.slice() : dist.summary;
        dist.searchRows = cl.searchRows ? JSON.parse(JSON.stringify(cl.searchRows)) : dist.searchRows;
        dist.noticeList = cl.noticeList ? cl.noticeList.slice() : dist.noticeList;
        dist.distributionThreeRowHeader = !!cl.distributionThreeRowHeader;
        dist.searchFormClass = cl.searchFormClass || dist.searchFormClass;
        dist.tableScrollable = cl.tableScrollable;
        dist.buttons = cl.buttons ? JSON.parse(JSON.stringify(cl.buttons)) : dist.buttons;
      }
      var ex = MENU_SCREENS['/settlement/execute'];
      var exc = MENU_SCREENS['/calc/exCalcList'];
      if (ex && exc && ex.columns && ex.columns.length) {
        exc.columns = JSON.parse(JSON.stringify(ex.columns));
      }
      var fee = MENU_SCREENS['/calc/feeList'];
      if (fee && !MENU_SCREENS['/settlement/feeList']) {
        MENU_SCREENS['/settlement/feeList'] = JSON.parse(JSON.stringify(fee));
      }
      var rc = MENU_SCREENS['/calc/compPointMngList'];
      var rcs = MENU_SCREENS['/settlement/recallMng'];
      if (rc && rcs) {
        rcs.columns = JSON.parse(JSON.stringify(rc.columns || []));
        rcs.searchRows = JSON.parse(JSON.stringify(rc.searchRows || []));
        rcs.summary = (rc.summary || []).slice();
      }
      var sr = MENU_SCREENS['/calc/settlementReport'];
      if (sr && !MENU_SCREENS['/settlement/settlementReport']) {
        MENU_SCREENS['/settlement/settlementReport'] = JSON.parse(JSON.stringify(sr));
      }
      /* /settlement/settlementReport 가 이미 있으면 본사 지급 컬럼만 동기화 */
      var srs = MENU_SCREENS['/settlement/settlementReport'];
      if (sr && srs && sr.columnsRegionalPayout) {
        srs.columnsRegionalPayout = JSON.parse(JSON.stringify(sr.columnsRegionalPayout));
      }
      var col = MENU_SCREENS['/calc/collateralList'];
      if (col && !MENU_SCREENS['/settlement/collateralList']) {
        MENU_SCREENS['/settlement/collateralList'] = JSON.parse(JSON.stringify(col));
      }
    } catch (e) { /* ignore */ }
  })();

  /** 글자수(라벨·옵션·placeholder)에 연동된 입력창 너비(ch) 자동 계산 */
  function autoCh(field) {
    var labelLen = (field.label || '').length;
    if (field.type === 'select' && field.options && field.options.length) {
      var maxOpt = 0;
      field.options.forEach(function (o) { var l = String(o.t || o.v || '').length; if (l > maxOpt) maxOpt = l; });
      return Math.max(6, Math.min(20, labelLen + Math.max(4, maxOpt) + 2));
    }
    if (field.type === 'text') {
      var phLen = (field.placeholder || '').length;
      return Math.max(8, Math.min(24, labelLen + (phLen || 10) + 2));
    }
    if (field.type === 'daterange') return 20;
    return 10;
  }

  function sizeStyle(ch) {
    if (ch == null) return '';
    var n = Math.max(4, Number(ch) || 10);
    return ' width:' + n + 'ch; min-width:' + n + 'ch; max-width:none';
  }

  function wrapSearchCell(content, hasLabel) {
    return '<div class="search-cell' + (hasLabel ? ' search-cell--with-label' : '') + '">' + content + '</div>';
  }

  var INPUT_SCALE = 1.3;
  var DATE_SCALE = 1.4;

  function renderSearchCell(field) {
    var inner = '';
    var ch = field.size != null ? field.size : autoCh(field);
    var sz = sizeStyle(Math.ceil(ch * INPUT_SCALE));
    if (field.type === 'daterange') {
      var dateCh = Math.ceil(10 * DATE_SCALE);
      inner = (field.label ? '<span class="search-cell-label">' + field.label + '</span>' : '') + '<div class="search-cell-input search-cell-input--daterange"><input type="date" class="form-control form-control-sm search-date-input" id="' + (field.from || 'searchFromDate') + '" name="' + (field.from || 'searchFromDate') + '" style="' + sizeStyle(dateCh) + '"> ~ <input type="date" class="form-control form-control-sm search-date-input" id="' + (field.to || 'searchToDate') + '" name="' + (field.to || 'searchToDate') + '" style="' + sizeStyle(dateCh) + '"></div>';
      return wrapSearchCell(inner, !!field.label);
    }
    if (field.type === 'quickdate') {
      var labels = field.quickdateLabels || ['당일', '당월', '전일', '1주', '2주', '전월'];
      var ranges = field.quickdateRanges || ['day', 'month', 'prevDay', 'week', 'week2', 'prevMonth'];
      var btns = '';
      for (var i = 0; i < labels.length; i++) {
        var lbl = labels[i];
        var cls = (lbl === '당일' || lbl === '당월') ? ' quick-date--pink' : '';
        btns += '<button type="button" class="btn btn-outline-primary btn-sm mr-1 quick-date' + cls + '" data-range="' + (ranges[i] || '') + '">' + lbl + '</button>';
      }
      inner = '<div class="search-cell-input">' + btns + '</div>';
      return wrapSearchCell(inner, false);
    }
    if (field.type === 'compMngSearchActions') {
      var cbName = field.checkboxName || 'searchIncludeSub';
      var searchLbl = field.searchLabel || '검색';
      inner = '<div class="search-cell-input comp-mng-search-actions-wrap d-flex align-items-center gap-2 flex-wrap">' +
        '<label class="d-flex align-items-center mb-0"><input type="checkbox" class="form-check-input me-1" id="' + cbName + '" name="' + cbName + '">' + (field.label || '') + '</label>' +
        '<button type="button" class="btn btn-primary btn-sm screen-search-btn">' + searchLbl + '</button>' +
        '</div>';
      return wrapSearchCell(inner, false);
    }
    if (field.type === 'searchBtn') {
      var btnLabel = field.label || '검색';
      var iconHtml = field.noIcon ? '' : '<i class="bi bi-search"></i> ';
      inner = '<div class="search-cell-input search-cell-input--right"><button type="button" class="btn btn-primary btn-sm screen-search-btn">' + iconHtml + btnLabel + '</button></div>';
      return wrapSearchCell(inner, false);
    }
    if (field.type === 'button' && field.name === 'searchReset') {
      inner = '<div class="search-cell-input"><button type="button" class="btn btn-outline-secondary btn-sm search-reset-btn">' + (field.label || '초기화') + '</button></div>';
      return wrapSearchCell(inner, false);
    }
    if (field.type === 'checkbox') {
      var cbName = field.name || '';
      inner = (field.label ? '<span class="search-cell-label">' + field.label + '</span>' : '') + '<div class="search-cell-input"><input type="checkbox" class="form-check-input" id="' + cbName + '" name="' + cbName + '"></div>';
      return wrapSearchCell(inner, !!field.label);
    }
    if (field.type === 'select') {
      var opts = (field.options || []).map(function (o) { return '<option value="' + (o.v || '') + '">' + (o.t || o.v) + '</option>'; }).join('');
      inner = (field.label ? '<span class="search-cell-label">' + field.label + '</span>' : '') + '<div class="search-cell-input"><select class="form-control form-control-sm _searchChange" id="' + (field.name || '') + '" name="' + (field.name || '') + '" style="' + sz + '">' + opts + '</select></div>';
      return wrapSearchCell(inner, !!field.label);
    }
    if (field.type === 'text') {
      inner = (field.label ? '<span class="search-cell-label">' + field.label + '</span>' : '') + '<div class="search-cell-input"><input type="text" class="form-control form-control-sm _searchText" id="' + (field.name || '') + '" name="' + (field.name || '') + '" placeholder="' + (field.placeholder || '') + '" style="' + sz + '"></div>';
      return wrapSearchCell(inner, !!field.label);
    }
    return '';
  }

  function renderSearchRow(row) {
    var cells = Array.isArray(row) ? row : (row ? [row] : []);
    if (cells.length === 0) return '';
    var html = cells.map(renderSearchCell).filter(Boolean).join('');
    return html ? '<div class="search-form-row">' + html + '</div>' : '';
  }

  function renderSearchForm(cfg) {
    var rows = cfg.searchRows || [];
    var rows2 = cfg.searchRows2 || [];
    var rows3 = cfg.searchRows3 || [];
    var formClass = 'screen-search-form' + (cfg.searchFormClass ? ' ' + cfg.searchFormClass : '');
    var html = '<form id="screenSearchForm" class="' + formClass + '" onsubmit="return false;">';
    rows.forEach(function (r) { html += renderSearchRow(r); });
    rows2.forEach(function (r) { html += renderSearchRow(r); });
    rows3.forEach(function (r) { html += renderSearchRow(r); });
    html += '</form>';
    return html;
  }

  function renderNotice(cfg) {
    var list = cfg.noticeList || [];
    var refBtn = cfg.noticeRefButton;
    if (list.length === 0 && !refBtn) return '';
    var items = list.map(function (t) { return '<li>' + t + '</li>'; }).join('');
    var noticeHtml = list.length > 0 ? '<ul class="mb-0">' + items + '</ul>' : '';
    var btnHtml = refBtn ? '<button type="button" class="btn btn-sm ' + (refBtn.cls || 'btn-success') + ' ms-2" id="' + (refBtn.id || 'noticeRefBtn') + '">' + (refBtn.label || '참고') + '</button>' : '';
    return '<div class="search-notice mb-2 d-flex align-items-center flex-wrap">' + (list.length > 0 ? '<div class="search-notice-text flex-grow-1">' + noticeHtml + '</div>' : '') + btnHtml + '</div>';
  }

  function renderTableColumnGuide(cfg) {
    if (cfg.tableColumnGuide === false || !cfg.columns || cfg.columns.length === 0) return '';
    /** 번호·업체·거래일시·Route No 는 전산 기본 노출(결제 그리드 앞쪽 고정) — VIEW SETTING에서 토글 제외 */
    var fixedKeys = ['rowNo', 'compId', 'compNm', 'compDivNm', 'trnDate', 'trnTime', 'routeNo'];
    var cols = cfg.columns.filter(function (c) {
      if (c.type === 'checkbox' || c.type === 'payActions' || c.type === 'accountAccessDelete' || c.type === 'userResetPassword' || c.type === 'userDelete') return false;
      return fixedKeys.indexOf(c.key) === -1;
    });
    if (cols.length === 0) return '';
    var items = cols.map(function (c) {
      var key = c.key || '';
      var label = c.label || c.key;
      return '<label class="column-guide-item column-guide-item--on"><input type="checkbox" class="column-guide-check" data-key="' + key + '" checked> <span class="column-guide-label">' + label + '</span></label>';
    }).join('');
    var actionsHtml =
      '<button type="button" class="btn btn-xs btn-outline-primary" id="compMngSaveColumnsBtn">저장</button>' +
      '<button type="button" class="btn btn-xs btn-outline-secondary" id="compMngClearColumnsBtn">해제</button>';
    var rootClass = 'table-column-guide mb-3 p-2 border rounded bg-light';
    if (cfg.tableColumnGuideTwoRow === true) {
      return '<div class="' + rootClass + ' table-column-guide--two-row" id="tableColumnGuide">' +
        '<div class="column-guide-row column-guide-top">' +
        '<div class="column-guide-title">VIEW SETTING</div>' +
        '<div class="column-guide-actions">' + actionsHtml + '</div>' +
        '</div>' +
        '<div class="column-guide-row column-guide-checkboxes">' +
        '<div class="column-guide-list">' + items + '</div>' +
        '</div>' +
        '</div>';
    }
    return '<div class="' + rootClass + '" id="tableColumnGuide">' +
      '<div class="column-guide-row column-guide-title">VIEW SETTING</div>' +
      '<div class="column-guide-row column-guide-body">' +
      '<div class="column-guide-list">' + items + '</div>' +
      '<div class="column-guide-actions">' + actionsHtml + '</div>' +
      '</div>' +
      '</div>';
  }

  function renderFormField(f, readonlyAttr) {
    var hqC = f.hideForHeadquarters ? ' comp-info-hide-if-hq' : '';
    var hqPolicyC = f.hqPolicyOnly ? ' hq-policy-only' : '';
    if (f.type === 'hidden') {
      return '<input type="hidden" name="' + (f.name || '') + '" id="' + (f.name || '') + '">';
    }
    if (f.type === 'customHtml') {
      var colH = f.col || 12;
      return '<div class="col-sm-' + colH + '">' + (f.html || '') + '</div>';
    }
    if (f.type === 'passwordReset') {
      var col = f.col || 2;
      var label = (f.label || '비밀번호').replace(/\*$/, '');
      return '<div class="col-sm-' + col + ' form-field-block' + hqC + hqPolicyC + '"><label class="form-label">' + label + '</label><div class="form-input-with-btn"><button type="button" class="btn btn-outline-secondary btn-sm" id="compDetailPwdResetBtn" data-action="비밀번호 초기화">비밀번호 초기화</button></div></div>';
    }
    var isRequired = !!(f.required || (f.label && f.label.indexOf('*') !== -1));
    var reqClass = isRequired ? ' required-input' : '';
    if (f.type === 'regNoWithType') {
      var col = f.col || 2;
      var label = (f.label || '사업자번호').replace(/\*$/, '') + (f.label && f.label.indexOf('*') !== -1 ? ' <span class="text-danger">*</span>' : '');
      return '<div class="col-sm-' + col + ' form-field-block' + hqC + hqPolicyC + '"><label class="form-label">' + label + '</label>' +
        '<div class="d-flex gap-1 align-items-center"><select class="form-control form-control-sm' + reqClass + '" name="regType" id="regType" style="width:auto;min-width:70px"><option value="CORP">법인</option><option value="PERSONAL">개인</option></select>' +
        '<input type="text" class="form-control form-control-sm flex-grow-1' + reqClass + '" name="' + (f.name || 'regNo') + '" id="' + (f.name || 'regNo') + '" placeholder="번호 입력"></div></div>';
    }
    var col = f.col || 2;
    var req = (f.label && f.label.indexOf('*') !== -1) ? '' : '';
    var label = (f.label || '').replace(/\*$/, '') + (f.label && f.label.indexOf('*') !== -1 ? ' <span class="text-danger">*</span>' : '');
    var name = f.name || '';
    var id = name;
    var ro = (readonlyAttr || f.readonly) ? ' readonly' : '';
    var inp = '';
    if (f.type === 'text' || f.type === 'password') {
      inp = '<input type="' + (f.type || 'text') + '" class="form-control form-control-sm' + reqClass + '" name="' + name + '" id="' + id + '"' + (f.placeholder ? ' placeholder="' + f.placeholder + '"' : '') + ro + '>';
    } else if (f.type === 'time') {
      inp = '<input type="time" class="form-control form-control-sm' + reqClass + '" name="' + name + '" id="' + id + '"' + (f.placeholder ? ' placeholder="' + f.placeholder + '"' : '') + '>';
    } else if (f.type === 'select') {
      var opts = (f.options || []).map(function (o) { return '<option value="' + (o.v || '') + '">' + (o.t || o.v) + '</option>'; }).join('');
      var selAttrs = (f.readonly ? ' disabled' : '')
        + (f.loadCountries ? ' data-load-countries="true"' : '')
        + (f.bankByCountry ? ' data-bank-by-country="true"' : '')
        + (f.loadNotifyTargets ? ' data-load-notify-targets="true"' : '')
        + (f.loadRegionalBranches ? ' data-load-regional-branches="true"' : '');
      inp = '<select class="form-control form-control-sm' + reqClass + '" name="' + name + '" id="' + id + '"' + selAttrs + '>' + opts + '</select>';
    } else if (f.type === 'textarea') {
      var taRows = f.rows != null ? Math.max(2, parseInt(f.rows, 10) || 3) : 3;
      inp = '<textarea class="form-control form-control-sm' + reqClass + '" name="' + name + '" id="' + id + '" rows="' + taRows + '"' + ro + '></textarea>';
    } else if (f.type === 'file') {
      inp = '<input type="file" class="form-control form-control-sm" name="' + name + '" id="' + id + '">';
    } else {
      inp = '<input type="text" class="form-control form-control-sm' + reqClass + '" name="' + name + '" id="' + id + '"' + (f.placeholder ? ' placeholder="' + f.placeholder + '"' : '') + ro + '>';
    }
    var inpWrap = inp;
    if (f.button) {
      inpWrap = '<div class="form-input-with-btn"><span class="form-input-wrap">' + inp + '</span><button type="button" class="btn btn-outline-secondary btn-sm" data-field="' + name + '" data-action="' + f.button + '">' + f.button + '</button></div>';
    }
    if (f.smsButton) {
      var smsCls = 'btn-outline-primary';
      if (f.smsColor === 'warning') smsCls = 'btn-outline-warning';
      else if (f.smsColor === 'success') smsCls = 'btn-outline-success';
      else if (f.smsColor === 'secondary') smsCls = 'btn-outline-secondary';
      inpWrap = '<div class="form-input-with-btn"><span class="form-input-wrap">' + inp + '</span><button type="button" class="btn ' + smsCls + ' btn-sm" data-field="' + name + '">SMS수신</button></div>';
    }
    var blockClass = 'col-sm-' + col + ' form-field-block';
    if (f.customOnly) blockClass += ' commission-custom-only';
    if (f.holdRateOnly) blockClass += ' hold-rate-custom-only';
    return '<div class="' + blockClass + hqC + hqPolicyC + '"><label class="form-label">' + label + '</label>' + inpWrap + '</div>';
  }

  function renderFormSections(cfg) {
    var sections = cfg.formSections || [];
    if (sections.length === 0) return '';
    var formId = cfg.isCompDetail ? 'compDetailForm' : (cfg.formHtmlId || 'compRegForm');
    return renderFormSectionsWithId(sections, formId, null);
  }

  function renderFormSectionsWithId(sections, formId, buttons) {
    if (!sections || sections.length === 0) return '';
    var html = '<form id="' + (formId || 'compRegForm') + '" class="comp-reg-form" onsubmit="return false;">';
    if (formId === 'compRegForm') {
      html += '<div class="comp-div-hint alert alert-info py-2 mb-3" role="alert">' +
        '<small><strong>업체구분</strong>을 선택하시면 해당 등록 유형에 맞는 입력 창이 표시됩니다. (총판/지사/대리점/가맹점)</small></div>';
    }
    sections.forEach(function (sec) {
      var cardClass = 'card mb-3';
      if (sec.merchantOnly) cardClass += ' merchant-only-section d-none';
      else if (sec.regionalOnly) cardClass += ' regional-only-section d-none';
      else if (sec.masterDistOnly) cardClass += ' master-dist-only-section d-none';
      else if (sec.regionalOrMasterDistOnly) cardClass += ' regional-or-master-dist-only-section d-none';
      else if (sec.distributorOnly) cardClass += ' distributor-only-section d-none';
      else if (sec.distributorMerchantOnlyNoRegional) cardClass += ' distributor-merchant-no-regional-section d-none';
      else if (sec.distributorOrMerchantOnly) cardClass += ' distributor-or-merchant-section d-none';
      if (sec.branchAgencySalesHide) cardClass += ' branch-agency-sales-hide-section';
      var cardId = sec.id ? ' id="' + sec.id + '"' : '';
      html += '<div' + cardId + ' class="' + cardClass + '"><div class="card-header">' + (sec.title || '') + '</div><div class="card-body">';
      if (sec.notice) html += '<p class="text-muted small mb-2">' + sec.notice + '</p>';
      if (sec.type === 'branding') {
        html += '<p class="text-danger small mb-2">메인이미지는 2MB, 로고이미지는 1MB까지 업로드 가능합니다. 가능하면 PNG파일을 추천합니다.</p>' +
          '<div class="row mb-2"><div class="col-sm-6"><label class="form-label">메인이미지</label><div class="input-group input-group-sm">' +
          '<input type="text" class="form-control" name="mainImageUrl" id="brandingMainImageUrl" readonly placeholder="업로드된 이미지">' +
          '<input type="file" class="d-none" id="brandingMainImageFile" accept="image/png,image/jpeg,image/jpg">' +
          '<button type="button" class="btn btn-outline-secondary" id="brandingMainImageBrowse">Browse</button></div></div></div>' +
          '<div class="row mb-2"><div class="col-sm-6"><label class="form-label">로고이미지</label><div class="input-group input-group-sm">' +
          '<input type="text" class="form-control" name="logoImageUrl" id="brandingLogoImageUrl" readonly placeholder="업로드된 이미지">' +
          '<input type="file" class="d-none" id="brandingLogoImageFile" accept="image/png,image/jpeg,image/jpg">' +
          '<button type="button" class="btn btn-outline-secondary" id="brandingLogoImageBrowse">Browse</button></div></div></div>' +
          '<div class="row mb-2"><div class="col-sm-4"><label class="form-label">배경테마</label><select class="form-control form-control-sm" name="brandingTheme" id="brandingTheme">' +
          '<option value="DEFAULT">기본(현재)</option><option value="LIGHT">Light (흰배경/검정글씨)</option><option value="DARK">Dark (어두운배경/흰글씨)</option>' +
          '<option value="PASTEL_1">파스텔1</option><option value="PASTEL_2">파스텔2</option><option value="PASTEL_3">파스텔3</option><option value="PASTEL_4">파스텔4</option><option value="PASTEL_5">파스텔5</option>' +
          '</select></div></div>';
      } else if (sec.type === 'pgBindingList') {
        html += '<div class="pg-binding-list-wrap"><table class="table table-sm table-bordered pg-binding-table"><thead><tr>' +
          '<th>운영</th><th>착신화</th><th>결제대행사</th><th>결제구분</th><th>MID</th><th>루트번호</th><th>API KEY</th><th>IV KEY</th><th>할부</th><th>최대할부</th><th style="min-width:200px">작업</th></tr></thead><tbody id="pgBindingTbody"></tbody></table>' +
          '<button type="button" class="btn btn-outline-primary btn-sm mt-2" id="pgBindingAddBtn">+ 결제대행사 추가</button>' +
          '<input type="hidden" name="pgBindings" id="pgBindingsHidden" value="[]"></div>';
      } else if (sec.type === 'pgInfoDisplay') {
        html += '<div id="pgInfoDisplayWrap" class="pg-info-display">' +
          '<div class="row mb-2"><div class="col-sm-3"><label class="form-label">웹결제 사용여부</label><select class="form-control form-control-sm" name="webPaymentUseYn"><option value="Y">사용</option><option value="N">미사용</option></select></div>' +
          '<div class="col-sm-9"><label class="form-label">결제 URL</label><div class="input-group input-group-sm"><input type="text" class="form-control" id="paymentUrlDisplay" readonly placeholder="가맹점 선택 후 조회"><button type="button" class="btn btn-outline-primary" id="paymentUrlCopyBtn">복사</button></div></div></div>' +
          '</div>';
      } else if (sec.type === 'regionalCardLimitTable') {
        html += '<div class="d-flex justify-content-end mb-2"><button type="button" class="btn btn-success btn-sm me-1" id="regionalCardLimitAddBtn">추가</button><button type="button" class="btn btn-danger btn-sm" id="regionalCardLimitDelBtn">삭제</button></div>' +
          '<div class="table-responsive"><table class="table table-sm table-bordered"><thead class="table-info"><tr>' +
          '<th style="width:40px"><input type="checkbox" class="regional-card-limit-check-all" title="전체선택"></th>' +
          '<th class="text-danger">결제구분</th><th class="text-danger">카드사</th><th class="text-danger">일</th><th class="text-danger">회</th><th class="text-danger">원</th><th class="text-danger">등록사유</th><th>등록일자</th><th>수정일자</th><th>비고</th></tr></thead>' +
          '<tbody id="regionalCardLimitTbody"></tbody></table></div>' +
          '<div class="text-center text-muted py-2 empty-table-msg" id="regionalCardLimitEmpty">조회 된 데이터가 없습니다.</div>' +
          '<input type="hidden" name="regionalCardLimits" id="regionalCardLimitsHidden" value="[]">';
      } else if (sec.type === 'regionalTerminalTable') {
        html += '<div class="table-responsive"><table class="table table-sm table-bordered"><thead class="table-info"><tr>' +
          '<th>No.</th><th>결제대행사</th><th>터미널ID</th><th>비고</th></tr></thead>' +
          '<tbody id="regionalTerminalTbody"></tbody></table></div>' +
          '<div class="text-center text-muted py-2 empty-table-msg" id="regionalTerminalEmpty">조회 된 데이터가 없습니다.</div>' +
          '<button type="button" class="btn btn-outline-primary btn-sm mt-2" id="regionalTerminalAddBtn">+ 터미널 추가</button>' +
          '<input type="hidden" name="regionalTerminals" id="regionalTerminalsHidden" value="[]">';
      } else {
        (sec.rows || []).forEach(function (row) {
          var first = (row || [])[0];
          if (first && first.type === 'countryAddressRow') {
            var opt = first;
            html += '<div class="row country-address-row" data-country-address="true">' +
              '<div class="col-sm-2 form-field-block"><label class="form-label">국가</label><select class="form-control form-control-sm" name="addrCountryCd" data-addr-country-select><option value="">선택</option><option value="JP">JAPAN</option><option value="KR">KOREA</option><option value="TH">THAILAND</option><option value="OTHER">기타</option></select></div>' +
              '<div class="col-sm-2 form-field-block addr-country-other-wrap d-none"><label class="form-label">국가</label><select class="form-control form-control-sm" name="addrCountryCdOther">' + (window.PG_COUNTRY_OTHER_OPTIONS || '<option value="">선택</option>') + '</select></div>' +
              '<div class="col-sm-2 form-field-block zip-wrap"><label class="form-label">' + (opt.zipLabel || '우편번호*') + '</label><div class="form-input-with-btn" data-zip-search-wrap><input type="text" class="form-control form-control-sm" name="zipCode" placeholder="검색" data-zip-input><button type="button" class="btn btn-outline-secondary btn-sm" data-addr-zip-search>검색</button></div></div>' +
              '<div class="col-sm-2 form-field-block"><label class="form-label">' + (opt.addrLabel || '주소*') + '</label><input type="text" class="form-control form-control-sm" name="addr" data-addr-input></div>' +
              '<div class="col-sm-2 form-field-block"><label class="form-label">' + (opt.addrDetailLabel || '상세주소') + '</label><input type="text" class="form-control form-control-sm" name="addrDetail"></div>' +
              (opt.addrEtcLabel ? '<div class="col-sm-2 form-field-block"><label class="form-label">' + opt.addrEtcLabel + '</label><input type="text" class="form-control form-control-sm" name="addrEtc" placeholder="기타 입력"></div>' : '') +
              '</div>';
          } else if (first && first.type === 'countryBankRow') {
            var opt = first;
            var bankHq = opt.hideForHeadquarters ? ' comp-info-hide-if-hq' : '';
            html += '<div class="row country-bank-row' + bankHq + '" data-country-bank="true">' +
              '<div class="col-sm-2 form-field-block"><label class="form-label">국가</label><select class="form-control form-control-sm" name="countryCd" data-country-select><option value="">선택</option><option value="JP">JAPAN</option><option value="KR">KOREA</option><option value="TH">THAILAND</option><option value="OTHER">기타</option></select></div>' +
              '<div class="col-sm-2 form-field-block country-other-wrap d-none"><label class="form-label">국가</label><select class="form-control form-control-sm" name="countryCdOther">' + (window.PG_COUNTRY_OTHER_OPTIONS || '<option value="">선택</option>') + '</select></div>' +
              '<div class="col-sm-2 form-field-block bank-select-wrap"><label class="form-label">' + (opt.bankLabel || '계좌은행*') + '</label><select class="form-control form-control-sm" name="bankCd" data-bank-select><option value="">국가 선택 후</option></select></div>' +
              '<div class="col-sm-2 form-field-block bank-text-wrap d-none"><label class="form-label">' + (opt.bankLabel || '계좌은행*') + '</label><input type="text" class="form-control form-control-sm" name="bankCdText" placeholder="은행명 직접입력"></div>' +
              '<div class="col-sm-2 form-field-block"><label class="form-label">' + (opt.accountNoLabel || '계좌번호*') + '</label><input type="text" class="form-control form-control-sm" name="' + (opt.accountNoName || 'accountNo') + '"></div>' +
              '<div class="col-sm-2 form-field-block"><label class="form-label">' + (opt.accountHolderLabel || '계좌주명*') + '</label><input type="text" class="form-control form-control-sm" name="' + (opt.accountHolderName || 'accountHolder') + '"></div>' +
              (opt.extraFields ? opt.extraFields.map(function (ef) {
                return '<div class="col-sm-' + (ef.col || 2) + ' form-field-block"><label class="form-label">' + (ef.label || '') + '</label><input type="text" class="form-control form-control-sm" name="' + (ef.name || '') + '" placeholder="' + (ef.placeholder || '') + '"></div>';
              }).join('') : '') +
              '</div>';
          } else {
            html += '<div class="row">';
            (row || []).forEach(function (f) { html += renderFormField(f); });
            html += '</div>';
          }
        });
      }
      html += '</div></div>';
    });
    html += '</form>';
    if (buttons && buttons.length > 0) {
      html += '<div class="row mb-2"><div class="col-sm-12">';
      buttons.forEach(function (b) {
        html += '<button type="button" class="btn ' + (b.cls || 'btn-secondary') + ' btn-sm mr-1" id="' + (b.id || '') + '">' + (b.label || '') + '</button>';
      });
      html += '</div></div>';
    }
    return html;
  }

  function renderFormRows(cfg) {
    var rows = cfg.formRows || [];
    if (rows.length === 0) return '';
    var html = '<form id="compRegForm" class="comp-reg-form" onsubmit="return false;"><div class="row">';
    rows.forEach(function (r) {
      var col = r.col || 2;
      var req = r.required ? ' <span class="text-danger">*</span>' : '';
      if (r.type === 'text') {
        html += '<div class="col-sm-' + col + ' mb-2"><label class="form-label">' + (r.label || '') + req + '</label><input type="text" class="form-control form-control-sm" name="' + (r.name || '') + '" id="' + (r.name || '') + '"></div>';
      }
    });
    html += '</div></form>';
    return html;
  }

  function renderSummary(cfg) {
    var items = cfg.summary || [];
    if (items.length === 0) return '';
    var fmt = cfg.summaryFormat !== undefined ? cfg.summaryFormat : '0';
    var html = '<div class="row mb-2 summary-bar-wrap"><div class="col-sm-12">';
    items.forEach(function (s) {
      html += '<span class="summary-item mr-3" id="summary_' + s + '">' + s + ': ' + fmt + '</span>';
    });
    html += '</div></div>';
    return html;
  }

  function renderButtons(cfg) {
    var btns = cfg.buttons || [];
    var html = '<div class="row mb-2 screen-action-row"><div class="col-sm-12 screen-action-buttons">';
    btns.forEach(function (b) {
      html += '<button type="button" class="btn ' + (b.cls || 'btn-secondary') + ' btn-sm mr-1" id="' + (b.id || '') + '">' + (b.label || '') + '</button>';
    });
    html += '</div></div>';
    return html;
  }

  /** 총합(요약) 왼쪽 + 액션 버튼 오른쪽 한 줄 배치 (모든 목록 화면 공통) */
  function renderSummaryAndActions(cfg) {
    var items = cfg.summary || [];
    var btns = cfg.buttons || [];
    var fmt = cfg.summaryFormat !== undefined ? cfg.summaryFormat : '0';
    var summaryHtml = '';
    if (items.length > 0) {
      summaryHtml = '<div class="summary-total-bar">';
      items.forEach(function (s) {
        summaryHtml += '<span class="summary-total-item" id="summary_' + s + '">' + s + ': ' + fmt + '</span>';
      });
      summaryHtml += '</div>';
    }
    var buttonsHtml = '';
    if (btns.length > 0) {
      buttonsHtml = '<div class="screen-action-buttons">';
      btns.forEach(function (b) {
        buttonsHtml += '<button type="button" class="btn ' + (b.cls || 'btn-secondary') + ' btn-sm" id="' + (b.id || '') + '">' + (b.label || '') + '</button>';
      });
      buttonsHtml += '</div>';
    }
    if (!summaryHtml && !buttonsHtml) return '';
    return '<div class="screen-summary-action-row">' + summaryHtml + buttonsHtml + '</div>';
  }

  /** 유통망정산내역: 승인/취소 × 수수료 4단 중첩 헤더 */
  function buildDistributionListTheadHtml() {
    return (
      '<tr>' +
      '<th rowspan="3" data-key="rowNo" class="text-nowrap">No.</th>' +
      '<th rowspan="3" data-key="settleMonth" class="text-nowrap">정산월</th>' +
      '<th rowspan="3" data-key="orgDivNm" class="text-nowrap">구분</th>' +
      '<th rowspan="3" data-key="regionalNm" class="text-nowrap">본사</th>' +
      '<th rowspan="3" data-key="masterNm" class="text-nowrap">총판</th>' +
      '<th rowspan="3" data-key="branchNm" class="text-nowrap">지사</th>' +
      '<th rowspan="3" data-key="agencyNm" class="text-nowrap">대리점</th>' +
      '<th rowspan="3" data-key="compId" class="text-nowrap">업체코드</th>' +
      '<th colspan="6" class="dist-th-group text-center">승인</th>' +
      '<th colspan="6" class="dist-th-group text-center">취소</th>' +
      '<th rowspan="3" data-key="settleAmt" class="text-nowrap">정산금액</th>' +
      '</tr>' +
      '<tr>' +
      '<th rowspan="2" data-key="aprvCnt" class="text-nowrap">건수</th>' +
      '<th rowspan="2" data-key="aprvAmt" class="text-nowrap">금액</th>' +
      '<th colspan="4" class="dist-th-fee text-center text-nowrap">수수료</th>' +
      '<th rowspan="2" data-key="canCnt" class="text-nowrap">건수</th>' +
      '<th rowspan="2" data-key="canAmt" class="text-nowrap">금액</th>' +
      '<th colspan="4" class="dist-th-fee text-center text-nowrap">수수료</th>' +
      '</tr>' +
      '<tr>' +
      '<th data-key="aprvFeeCnt" class="text-nowrap dist-th-fee-sub">건</th>' +
      '<th data-key="aprvFeePct" class="text-nowrap dist-th-fee-sub">%</th>' +
      '<th data-key="aprvFeeSum" class="text-nowrap dist-th-fee-sub">합계</th>' +
      '<th data-key="aprvFeeVat" class="text-nowrap dist-th-fee-sub">부가세</th>' +
      '<th data-key="canFeeCnt" class="text-nowrap dist-th-fee-sub">건</th>' +
      '<th data-key="canFeePct" class="text-nowrap dist-th-fee-sub">%</th>' +
      '<th data-key="canFeeSum" class="text-nowrap dist-th-fee-sub">합계</th>' +
      '<th data-key="canFeeVat" class="text-nowrap dist-th-fee-sub">부가세</th>' +
      '</tr>'
    );
  }

  function renderTable(cfg, tabId) {
    var cols = cfg.columns || [];
    if (cfg.distributionThreeRowHeader) {
      var emptyMsg = cfg.emptyMessage || '조회된 데이터가 없습니다.';
      var emptyRow = '<tr><td colspan="' + cols.length + '" class="empty-state-cell text-center text-muted py-4">' + emptyMsg + '</td></tr>';
      var respClass = 'table-responsive' + (cfg.tableScrollable ? ' table-scrollable' : '');
      var tblExtra = cfg.tableExtraClass ? (' ' + cfg.tableExtraClass) : '';
      return '<div class="' + respClass + '">' +
        '<table class="table table-bordered table-hover table-sm screen-distribution-grid' + tblExtra + '" id="grid_' + (tabId || '') + '">' +
        '<thead>' + buildDistributionListTheadHtml() + '</thead>' +
        '<tbody>' + emptyRow + '</tbody></table></div>';
    }
    var ths = cols.map(function (c) {
      if (c.type === 'checkbox') return '<th style="width:40px"><input type="checkbox" class="grid-check-all" title="전체선택"></th>';
      return '<th>' + (c.label || c.key) + '</th>';
    }).join('');
    var emptyMsg = cfg.emptyMessage || '조회된 데이터가 없습니다.';
    var emptyRow = '<tr><td colspan="' + cols.length + '" class="empty-state-cell text-center text-muted py-4">' + emptyMsg + '</td></tr>';
    var respClass = 'table-responsive' + (cfg.tableScrollable ? ' table-scrollable' : '');
    var tblExtra = cfg.tableExtraClass ? (' ' + cfg.tableExtraClass) : '';
    var html = '<div class="' + respClass + '"><table class="table table-bordered table-hover table-sm' + tblExtra + '" id="grid_' + (tabId || '') + '"><thead><tr>' + ths + '</tr></thead><tbody>' + emptyRow + '</tbody></table></div>';
    return html;
  }

  function renderPagination(tabId) {
    return '<div class="pagination-row">' +
      '<div class="pagination-view-at-once">' +
      '<span class="pagination-label">한 번에 보기:</span>' +
      '<div class="pagination-size-options">' +
      '<button type="button" class="pagination-size-opt" data-size="10">10</button>' +
      '<button type="button" class="pagination-size-opt" data-size="25">25</button>' +
      '<button type="button" class="pagination-size-opt" data-size="50">50</button>' +
      '<button type="button" class="pagination-size-opt pagination-size-opt--active" data-size="100">100</button>' +
      '</div>' +
      '<span class="pagination-total">건 (총 <span id="totalElementsCount">0</span>건)</span>' +
      '</div>' +
      '<input type="hidden" id="recordsPerPage" value="100">' +
      '<input type="hidden" id="pageCnt" value="1">' +
      '<span id="totalPageCount" style="display:none">1</span>' +
      '<div class="pagination-center"><div class="pagination-pages" id="paging_' + (tabId || '') + '"></div></div>' +
      '</div>';
  }

  var PAGE_FOOTER_HTML = '<div class="page-footer">Copyright © 2023 ICOPAY Service by Ontheline Co., Ltd.</div>';

  function getScreenHtml(url, tabId) {
    var cfg = MENU_SCREENS[url];
    tabId = tabId || (url.replace(/^\//, '').replace(/\//g, '_'));
    if (!cfg) {
      return '<div class="card"><div class="card-body"><p class="text-muted mb-0">화면 정보가 없습니다.</p>' + PAGE_FOOTER_HTML + '</div></div>';
    }
    var html = '<div class="content" id="screenContent_' + tabId + '">';
    html += '<div class="card"><div class="card-body">';
    if (cfg.isForm && cfg.formSections && cfg.formSections.length > 0) {
      html += renderFormSections(cfg);
      html += renderSummaryAndActions(cfg);
    } else if (cfg.isForm && cfg.formRows && cfg.formRows.length > 0) {
      html += renderFormRows(cfg);
      html += renderSummaryAndActions(cfg);
    } else {
      if (!cfg.hideListGrid) {
        html += renderSearchForm(cfg);
        if (cfg.noticeList && cfg.noticeList.length > 0) html += renderNotice(cfg);
        html += renderSummaryAndActions(cfg);
        if (cfg.columns && cfg.columns.length > 0) html += renderTableColumnGuide(cfg);
        html += renderTable(cfg, tabId);
        html += renderPagination(tabId);
        if (cfg.hasCommissionHistoryTable) {
          html += '<div class="card mt-4 commission-history-card"><div class="card-header py-2 fw-semibold">수수료 변경 히스토리</div><div class="card-body pt-2">' +
            '<p class="text-muted small mb-2" id="commissionHistSubtitle_' + tabId + '">목록에서 가맹점 행을 클릭하면 해당 업체의 변경 이력이 표시됩니다. (최근 변경이 No.1)</p>' +
            '<div class="table-responsive table-scrollable"><table class="table table-bordered table-sm table-hover mb-0 commission-split-grid" id="grid_commissionHist_' + tabId + '">' +
            '<thead><tr><th class="text-muted">…</th></tr></thead><tbody><tr><td class="text-center text-muted py-3">조회 전</td></tr></tbody></table></div></div></div>';
        }
      }
      if (cfg.hasSelectedTable) {
        html += '<div class="card mt-4" id="compMngSelectedCard"><div class="card-header">선택된 업체</div><div class="card-body"><p class="text-muted small mb-2">위 테이블에서 선택 후 [선택 저장] 버튼을 누르면 선택된 항목만 아래에 표시됩니다.</p><div class="table-responsive table-scrollable" id="compMngSelectedWrap"><table class="table table-bordered table-sm" id="grid_compMngSelected"><thead><tr id="compMngSelectedThead"></tr></thead><tbody id="compMngSelectedTbody"><tr><td colspan="20" class="text-center text-muted py-4">선택된 항목이 없습니다.</td></tr></tbody></table></div></div></div>';
      }
      if (cfg.hasCompInfoDetailForm && cfg.compInfoDetailFormSections && cfg.compInfoDetailFormSections.length > 0) {
        html += '<div class="card mt-3"><div class="card-body" id="compInfoDetailCard">';
        html += renderFormSectionsWithId(cfg.compInfoDetailFormSections, 'compInfoDetailForm', cfg.compInfoDetailButtons);
        html += '</div></div>';
      }
    }
    html += PAGE_FOOTER_HTML;
    html += '</div></div></div>';
    return html;
  }

  window.PG_CALC_CYCLE_OPTIONS = CALC_CYCLE_OPTIONS;
  window.PG_CALC_CYCLE_SEARCH_OPTIONS = CALC_CYCLE_SEARCH_OPTIONS;
  window.PG_SCREENS = {
    getScreenHtml: getScreenHtml,
    getMenuScreens: function () { return MENU_SCREENS; },
    buildDistributionListTheadHtml: buildDistributionListTheadHtml
  };
})();
