/**
 * PG 솔루션 - 23개 메뉴별 화면 HTML 템플릿 (fxhj 구조: 검색폼 + 요약 + 버튼 + 그리드 + 페이지네이션)
 */
(function () {
  'use strict';

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
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'pgCd', label: 'PG사코드' }, { key: 'pgNm', label: 'PG사명' }, { key: 'apiEndpoint', label: 'API 엔드포인트' }, { key: 'useYn', label: '사용여부' }, { key: 'regDt', label: '등록일' }]
    },
    '/hq/defaultCommission': {
      isForm: true,
      formSections: [
        {
          title: '기본 수수료 정책',
          notice: '결제 유입 금액에 대해 건당/취소/이용/실패/결제/환불 수수료를 차감하고, 롤링(담보금) 비율을 N일간 보류 후 정산 주기에 지급합니다.',
          rows: [
            [{ label: '건당 수수료(원)', type: 'text', name: 'perTxFee', col: 2 }, { label: '취소 수수료율(%)', type: 'text', name: 'cancelRate', col: 2 }, { label: '이용 수수료율(%)', type: 'text', name: 'usageRate', col: 2 }],
            [{ label: '실패 수수료(원/건)', type: 'text', name: 'failFee', col: 2 }, { label: '결제 수수료율(%)', type: 'text', name: 'payRate', col: 2 }, { label: '환불 수수료율(%)', type: 'text', name: 'refundRate', col: 2 }],
            [{ label: '롤링(담보금) 비율(%)', type: 'text', name: 'rollingPct', col: 2, placeholder: '5 또는 10' }, { label: '롤링 보류 일수', type: 'text', name: 'rollingDays', col: 2, placeholder: '120 또는 180' }, { label: '비고', type: 'text', name: 'memo', col: 2 }]
          ]
        }
      ],
      buttons: [{ id: 'hqDefaultCommissionSaveBtn', label: '저장', cls: 'btn-primary' }]
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
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'hqCd', label: '본사코드' }, { key: 'hqNm', label: '본사명' }, { key: 'menuId', label: '메뉴ID' }, { key: 'menuNm', label: '메뉴명' }, { key: 'accessYn', label: '접근허용' }, { key: 'regDt', label: '적용일' }]
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
      columns: [{ key: '_chk', label: '', type: 'checkbox' }, { key: 'title', label: '제목' }, { key: 'regDt', label: '작성일' }, { key: 'hitCnt', label: '조회수' }]
    },
    '/comp/myCompMng': {
      searchRows: [
        [
          { label: '업체코드', type: 'text', name: 'searchCompId' },
          { label: '업체명', type: 'text', name: 'searchCompNm' }
        ],
        [
          { label: '조회일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate' },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'compId', label: '업체코드' }, { key: 'compNm', label: '업체명' }, { key: 'regNo', label: '사업자번호' }, { key: 'regDt', label: '등록일' }]
    },
    '/comp/compMngTree': {
      searchRows: [
        [
          { label: '업체코드', type: 'text', name: 'searchCompId' },
          { label: '업체명', type: 'text', name: 'searchCompNm' },
          { type: 'searchBtn' }
        ]
      ],
      summary: [],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'compRegBtn', label: '등록', cls: 'btn-danger' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'compId', label: '업체코드' }, { key: 'compNm', label: '업체명' }, { key: 'compDiv', label: '구분' }, { key: 'regDt', label: '등록일' }]
    },
    '/comp/compReg': {
      isForm: true,
      formSections: [
        {
          title: '본사 정보 (업체정보)',
          notice: '상위 본사(우리)가 본사 권한을 준 그 회사의 정보를 입력합니다. 즉, 우리가 권한을 부여한 지역 본사/가맹점의 정보입니다.',
          rows: [
            [{ label: '상위 본사', type: 'text', name: 'parentComp', col: 2, button: '검색', placeholder: '권한 부여자(우리) 또는 상위 코드' }, { label: '업체구분*', type: 'select', name: 'compDiv', options: [{ v: '', t: '선택하세요' }, { v: 'AGENCY', t: '지역본사/대리점' }, { v: 'MERCHANT', t: '가맹점' }], col: 2 }],
            [{ label: '업체명(본사명)*', type: 'text', name: 'compNm', col: 2 }, { label: '사업자번호*', type: 'text', name: 'regNo', col: 2, placeholder: '000-00-00000' }],
            [{ label: '업태', type: 'text', name: 'bizType', col: 2, placeholder: '사업자등록증 업태' }, { label: '종목', type: 'text', name: 'industry', col: 2, placeholder: '사업자등록증 종목' }],
            [{ label: '대표자명*', type: 'text', name: 'ceoNm', col: 2 }, { label: '대표자 휴대폰*', type: 'text', name: 'ceoMobile', col: 2 }],
            [{ label: '업체전화*', type: 'text', name: 'compTel', col: 2 }, { label: '팩스', type: 'text', name: 'fax', col: 2 }],
            [{ label: '우편번호*', type: 'text', name: 'zipCode', col: 2 }, { label: '주소*', type: 'text', name: 'addr', col: 2 }],
            [{ label: '상세주소', type: 'text', name: 'addrDetail', col: 2 }, { label: '이메일', type: 'text', name: 'email', col: 2 }],
            [{ label: '사용여부*', type: 'select', name: 'useYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 }, { label: '로그인ID*', type: 'text', name: 'loginId', col: 2, button: '중복확인' }],
            [{ label: '비밀번호*', type: 'password', name: 'pwd', col: 2 }]
          ]
        },
        {
          title: '정산 계좌 정보',
          notice: '결제실패 수수료는 특정 결제대행사만 적용됩니다.',
          rows: [
            [{ label: '계좌은행*', type: 'select', name: 'bankCd', options: [{ v: '', t: '선택하세요' }, { v: '04', t: '국민' }, { v: '20', t: '우리' }, { v: '81', t: 'KEB하나' }, { v: '88', t: '신한' }, { v: '11', t: 'NH농협' }], col: 2 }, { label: '이체수수료(원)', type: 'text', name: 'transferFee', col: 2 }],
            [{ label: '계좌번호*', type: 'text', name: 'accountNo', col: 2 }, { label: '예금주*', type: 'text', name: 'accountHolder', col: 2 }]
          ]
        },
        { title: '기타', rows: [[{ label: '특이사항', type: 'textarea', name: 'remark', col: 6 }]] },
        { title: '첨부파일', rows: [[{ type: 'file', name: 'attach', col: 4, button: '추가' }]] }
      ],
      buttons: [{ id: 'compRegSaveBtn', label: '저장', cls: 'btn-primary' }, { id: 'compRegCancelBtn', label: '취소', cls: 'btn-secondary' }]
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
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'compId', label: '업체코드' }, { key: 'compNm', label: '업체명' }, { key: 'cmsnRate', label: '수수료율' }, { key: 'applyDt', label: '적용일' }]
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
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'compId', label: '업체코드' }, { key: 'chgType', label: '변경구분' }, { key: 'chgDt', label: '변경일시' }, { key: 'chgDesc', label: '변경내용' }]
    },
    '/calc/payList': {
      searchRows: [
        [
          { label: '거래인자', type: 'select', name: 'searchTranFactor', options: [{ v: '', t: '전체' }] },
          { type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate' },
          { label: '업체명', type: 'select', name: 'searchCompNm', options: [{ v: '', t: '전체' }] },
          { label: '터미널ID', type: 'text', name: 'searchTmnId' }
        ],
        [
          { label: '결제구분', type: 'select', name: 'searchPayDivCd', options: [{ v: '', t: '전체' }, { v: '10', t: '결제' }, { v: '20', t: '취소' }] },
          { label: '정산구분', type: 'select', name: 'searchPayProcCd', options: [{ v: '', t: '전체' }, { v: '10', t: '정산대기' }, { v: '20', t: '정산완료' }, { v: '30', t: '결제취소' }, { v: '40', t: '정산취소' }], size: 8 },
          { label: '단계별', type: 'select', name: 'searchStep', options: [{ v: '', t: '출판' }] },
          { type: 'text', name: 'searchKeyword', placeholder: '검색어', size: 17 }
        ],
        [
          { label: 'PG사', type: 'select', name: 'searchPg', options: [{ v: '', t: '전체' }], size: 8 },
          { label: '정산주기', type: 'select', name: 'searchCycle', options: [{ v: '', t: '전체' }] },
          { label: '사업자번호', type: 'text', name: 'searchRegNo' },
          { label: '카드승인번호', type: 'text', name: 'searchCardAprvNo' },
          { type: 'searchBtn', label: 'Q 검색' }
        ]
      ],
      searchRows2: [],
      searchRows3: [],
      noticeList: [
        '오류 이월의 취소는 PG사지 다시 PG사이언됩니다.',
        '취소 건에 대한 정산 수수료 및 부가세는 정산 주기에 따라 반영됩니다.',
        '정산 주기 및 정산 수수료는 가맹점별로 상이할 수 있습니다.'
      ],
      summary: ['건수', '승인금액', '취소금액', '합계금액', '정산수수료', '정산부가세', '지급액'],
      summaryFormat: '$0',
      buttons: [{ id: 'reclaimBtn', label: '상신회수', cls: 'btn-warning' }, { id: 'excelDownBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'compDivCode9', label: '가맹점' }, { key: 'compId', label: '업체코드' }, { key: 'compRegDivNm', label: '구분' }, { key: 'compRegNo', label: '번호' }, { key: 'payDivNm', label: '결제구분' }, { key: 'payProcNm', label: '정산구분' }, { key: 'payCard', label: '결제가드' }, { key: 'cardAprvNo', label: '가드승인번호' }, { key: 'payCardNo', label: '결제기드번호' }, { key: 'instalMonth', label: '할부개월' }, { key: 'payMethod', label: '결재수단' }, { key: 'corpNm', label: '법인명' }, { key: 'pgNm', label: 'PG사' }, { key: 'calcCycle', label: '정산주기' }, { key: 'payAprv', label: '결제승인' }, { key: 'payAmount', label: '금액' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/calc/payListNew': {
      searchRows: [
        [
          { label: '조회일자', type: 'select', name: 'searchDateType', options: [{ v: 'TRAN', t: '거래일자' }], size: 8 },
          { type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate' },
          { label: '업체명', type: 'text', name: 'searchCompNm' },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수', '승인금액', '취소금액'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'compId', label: '업체코드' }, { key: 'paySeq', label: '거래번호' }, { key: 'payDivNm', label: '결제구분' }, { key: 'payAmount', label: '금액' }, { key: 'payDttm', label: '승인일시' }]
    },
    '/calc/payFailList': {
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
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'failDt', label: '실패일시' }, { key: 'compId', label: '업체코드' }, { key: 'payAmount', label: '금액' }, { key: 'failReason', label: '실패사유' }]
    },
    '/calc/offsetCancList': {
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
      summary: ['건수', '취소금액'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'cancDt', label: '취소일시' }, { key: 'compId', label: '업체코드' }, { key: 'cancAmount', label: '취소금액' }, { key: 'paySeq', label: '원거래번호' }]
    },
    '/pay/easyPay': {
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
      summary: ['건수', '금액'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'orderNo', label: '주문번호' }, { key: 'compId', label: '업체코드' }, { key: 'payAmount', label: '금액' }, { key: 'payDt', label: '결제일시' }]
    },
    '/calc/cashReceiptList': {
      searchRows: [
        [
          { label: '발행일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate', col: 5 },
          { type: 'quickdate' }
        ],
        [
          { label: '업체명', type: 'text', name: 'searchCompNm' },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수', '금액'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'issueDt', label: '발행일시' }, { key: 'compId', label: '업체코드' }, { key: 'amount', label: '금액' }, { key: 'issueType', label: '발행구분' }]
    },
    '/calc/calcList': {
      searchRows: [
        [
          { label: '정산일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate', col: 5 },
          { type: 'quickdate' }
        ],
        [
          { label: '업체명', type: 'text', name: 'searchCompNm' },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수', '정산금액', '수수료', '지급액'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'calcDt', label: '정산일자' }, { key: 'compId', label: '업체코드' }, { key: 'calcAmount', label: '정산금액' }, { key: 'cmsnAmount', label: '수수료' }, { key: 'payAmount', label: '지급액' }]
    },
    '/calc/calcGmList': {
      searchRows: [
        [
          { label: '정산일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate', col: 5 },
          { type: 'quickdate' }
        ],
        [
          { label: '업체명', type: 'text', name: 'searchCompNm' },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수', '가맹정산금액', '지급액'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'calcDt', label: '정산일자' }, { key: 'compId', label: '업체코드' }, { key: 'gmAmount', label: '가맹정산금액' }, { key: 'payAmount', label: '지급액' }]
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
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'compId', label: '업체코드' }, { key: 'pointAmount', label: '환수금액' }, { key: 'applyDt', label: '적용일자' }, { key: 'reason', label: '사유' }]
    },
    '/calc/balcInfo': {
      searchRows: [
        [
          { label: '업체코드', type: 'text', name: 'searchCompId' },
          { label: '업체명', type: 'text', name: 'searchCompNm' },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['잔액합계', '미수금합계'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'compId', label: '업체코드' }, { key: 'compNm', label: '업체명' }, { key: 'balcAmount', label: '잔액' }, { key: 'unpaidAmount', label: '미수금' }]
    },
    '/calc/exCalcList': {
      searchRows: [
        [
          { label: '정산일자', type: 'text', name: 'calcDt' },
          { label: '업체구분', type: 'select', name: 'compDiv', options: [{ v: '', t: '전체' }, { v: '2', t: '총판' }, { v: '3', t: '지사' }, { v: '6', t: '대리점' }], size: 8 },
          { type: 'searchBtn' }
        ]
      ],
      summary: [],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'exCalcBtn', label: '정산실행', cls: 'btn-warning' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'calcDt', label: '정산일자' }, { key: 'compId', label: '업체코드' }, { key: 'status', label: '상태' }, { key: 'payAmount', label: '지급액' }]
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
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'holdDt', label: '보류일시' }, { key: 'compId', label: '업체코드' }, { key: 'holdAmount', label: '보류금액' }, { key: 'holdReason', label: '보류사유' }]
    },
    '/noti/notiUrlMng': {
      searchRows: [
        [
          { label: '업체코드', type: 'text', name: 'searchCompId' },
          { label: 'URL구분', type: 'select', name: 'urlType', options: [{ v: '', t: '전체' }, { v: 'PAY', t: '결제통보' }], size: 8 },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'compId', label: '업체코드' }, { key: 'urlType', label: 'URL구분' }, { key: 'notiUrl', label: '통보URL' }, { key: 'useYn', label: '사용여부' }]
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
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'sendDt', label: '전송일시' }, { key: 'compId', label: '업체코드' }, { key: 'result', label: '결과' }, { key: 'retryCnt', label: '재전송횟수' }]
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
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'compId', label: '업체코드' }, { key: 'notiUrl', label: '현금영수증 통보URL' }, { key: 'useYn', label: '사용여부' }]
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
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'sendDt', label: '전송일시' }, { key: 'compId', label: '업체코드' }, { key: 'result', label: '결과' }]
    },
    '/user/userMng': {
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
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'userId', label: '사용자ID' }, { key: 'userNm', label: '사용자명' }, { key: 'compId', label: '소속업체' }, { key: 'roleNm', label: '권한' }, { key: 'useYn', label: '사용여부' }]
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
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'sortOrder', label: '순서' }, { key: 'colId', label: '항목ID' }, { key: 'colNm', label: '항목명' }, { key: 'dispYn', label: '표시여부' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/calc/withdrawList': {
      searchRows: [[{ label: '조회일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'withdrawDt', label: '출금일시' }, { key: 'compId', label: '업체코드' }, { key: 'amount', label: '출금금액' }, { key: 'status', label: '상태' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/calc/salesByComp': {
      searchRows: [[{ label: '조회일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'compId', label: '사업자코드' }, { key: 'compNm', label: '업체명' }, { key: 'salesAmt', label: '매출금액' }, { key: 'regDt', label: '집계일시' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/calc/payerSum': {
      searchRows: [[{ label: '조회일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'payerId', label: '결제자ID' }, { key: 'totalAmt', label: '누적금액' }, { key: 'cnt', label: '건수' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/calc/withdrawByAcct': {
      searchRows: [[{ label: '조회일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'acctNo', label: '출금계좌' }, { key: 'compId', label: '업체코드' }, { key: 'sumAmt', label: '집계금액' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/support/complaintList': {
      searchRows: [[{ label: '접수일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'complaintNo', label: '민원번호' }, { key: 'compId', label: '업체코드' }, { key: 'title', label: '제목' }, { key: 'regDt', label: '접수일' }, { key: 'status', label: '처리상태' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/comp/compInfo': {
      searchRows: [[{ label: '업체코드', type: 'text', name: 'searchCompId' }, { label: '업체명(본사명)', type: 'text', name: 'searchCompNm' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'compInfoDetailBtn', label: '상세(지역본사정보)', cls: 'btn-info' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'compId', label: '업체코드' }, { key: 'compNm', label: '업체명(본사명)' }, { key: 'compDiv', label: '구분' }, { key: 'regNo', label: '사업자번호' }, { key: 'regDt', label: '등록일' }],
      emptyMessage: '조회된 데이터가 없습니다.',
      hasCompInfoDetailForm: true,
      compInfoDetailFormSections: [
        {
          title: '본사 정보 상세 (업체정보조회)',
          notice: '상위 본사(우리)가 권한을 준 회사의 정보입니다. 그리드에서 한 건 선택 후 [상세] 버튼으로 조회·수정합니다.',
          rows: [
            [{ label: '업체코드', type: 'text', name: 'compId', col: 2, readonly: true }, { label: '업체구분', type: 'select', name: 'compDiv', options: [{ v: '', t: '선택' }, { v: 'AGENCY', t: '지역본사/대리점' }, { v: 'MERCHANT', t: '가맹점' }], col: 2 }],
            [{ label: '업체명(본사명)*', type: 'text', name: 'compNm', col: 2 }, { label: '사업자번호*', type: 'text', name: 'regNo', col: 2 }],
            [{ label: '업태', type: 'text', name: 'bizType', col: 2 }, { label: '종목', type: 'text', name: 'industry', col: 2 }],
            [{ label: '대표자명*', type: 'text', name: 'ceoNm', col: 2 }, { label: '대표자 휴대폰*', type: 'text', name: 'ceoMobile', col: 2 }],
            [{ label: '업체전화*', type: 'text', name: 'compTel', col: 2 }, { label: '팩스', type: 'text', name: 'fax', col: 2 }],
            [{ label: '우편번호*', type: 'text', name: 'zipCode', col: 2 }, { label: '주소*', type: 'text', name: 'addr', col: 2 }],
            [{ label: '상세주소', type: 'text', name: 'addrDetail', col: 2 }, { label: '이메일', type: 'text', name: 'email', col: 2 }],
            [{ label: '사용여부*', type: 'select', name: 'useYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 }, { label: '로그인ID*', type: 'text', name: 'loginId', col: 2 }],
            [{ label: '계좌은행', type: 'select', name: 'bankCd', options: [{ v: '', t: '선택' }, { v: '04', t: '국민' }, { v: '20', t: '우리' }, { v: '81', t: 'KEB하나' }, { v: '88', t: '신한' }, { v: '11', t: 'NH농협' }], col: 2 }, { label: '이체수수료(원)', type: 'text', name: 'transferFee', col: 2 }],
            [{ label: '계좌번호*', type: 'text', name: 'accountNo', col: 2 }, { label: '예금주*', type: 'text', name: 'accountHolder', col: 2 }],
            [{ label: '특이사항', type: 'textarea', name: 'remark', col: 6 }]
          ]
        }
      ],
      compInfoDetailButtons: [{ id: 'compInfoUpdateBtn', label: '수정 저장', cls: 'btn-primary' }]
    },
    '/comp/compMng': {
      searchRows: [[{ label: '업체코드', type: 'text', name: 'searchCompId' }, { label: '업체명', type: 'text', name: 'searchCompNm' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'compRegBtn', label: '등록', cls: 'btn-danger' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'compId', label: '업체코드' }, { key: 'compNm', label: '업체명' }, { key: 'compDiv', label: '구분' }, { key: 'regDt', label: '등록일' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/comp/compChangeHistory': {
      searchRows: [[{ label: '업체코드', type: 'text', name: 'searchCompId' }, { label: '변경일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'chgDt', label: '변경일시' }, { key: 'compId', label: '업체코드' }, { key: 'chgItem', label: '변경항목' }, { key: 'beforeVal', label: '변경전' }, { key: 'afterVal', label: '변경후' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/calc/offsetCancelList': {
      searchRows: [[{ label: '조회일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }], [{ label: '업체명', type: 'text', name: 'searchCompNm' }, { type: 'searchBtn' }]],
      summary: ['건수', '취소금액'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'cancDt', label: '취소일시' }, { key: 'compId', label: '업체코드' }, { key: 'cancAmount', label: '취소금액' }, { key: 'paySeq', label: '원거래번호' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/calc/urlPayList': {
      searchRows: [[{ label: '조회일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }], [{ label: '업체명', type: 'text', name: 'searchCompNm' }, { type: 'searchBtn' }]],
      summary: ['건수', '금액'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'payDt', label: '결제일시' }, { key: 'compId', label: '업체코드' }, { key: 'orderNo', label: '주문번호' }, { key: 'amount', label: '금액' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/settlement/distributionList': {
      searchRows: [[{ label: '정산일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수', '금액'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'calcDt', label: '정산일' }, { key: 'compId', label: '업체코드' }, { key: 'amount', label: '금액' }, { key: 'status', label: '상태' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/settlement/franchiseList': {
      searchRows: [[{ label: '정산일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수', '금액'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'calcDt', label: '정산일' }, { key: 'compId', label: '가맹점코드' }, { key: 'amount', label: '금액' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/settlement/recallMng': {
      searchRows: [[{ label: '조회일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'recallDt', label: '환수일' }, { key: 'compId', label: '업체코드' }, { key: 'amount', label: '환수금액' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/settlement/balanceMng': {
      searchRows: [[{ label: '조회일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'compId', label: '업체코드' }, { key: 'balance', label: '잔액' }, { key: 'shortfall', label: '미수금' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/settlement/execute': {
      searchRows: [[{ label: '정산대상일', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'searchBtn' }]],
      summary: [],
      buttons: [{ id: 'searchBtn', label: '조회', cls: 'btn-primary' }, { id: 'executeBtn', label: '정산실행', cls: 'btn-danger' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'calcDt', label: '정산일' }, { key: 'compId', label: '업체코드' }, { key: 'targetAmt', label: '정산대상금액' }, { key: 'totalFee', label: '공제수수료' }, { key: 'rollingReserveAmt', label: '롤링보류' }, { key: 'payAmount', label: '지급액' }, { key: 'status', label: '상태' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/settlement/holdList': {
      searchRows: [[{ label: '조회일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'holdDt', label: '보류일' }, { key: 'compId', label: '업체코드' }, { key: 'amount', label: '금액' }, { key: 'reason', label: '보류사유' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/notify/payUrlMng': {
      searchRows: [[{ label: '업체코드', type: 'text', name: 'searchCompId' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'addBtn', label: '등록', cls: 'btn-success' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'compId', label: '업체코드' }, { key: 'notifyUrl', label: '결제통보 URL' }, { key: 'useYn', label: '사용여부' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/notify/paySendMng': {
      searchRows: [[{ label: '전송일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'sendDt', label: '전송일시' }, { key: 'compId', label: '업체코드' }, { key: 'result', label: '결과' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/notify/cashReceiptUrlMng': {
      searchRows: [[{ label: '업체코드', type: 'text', name: 'searchCompId' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'addBtn', label: '등록', cls: 'btn-success' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'compId', label: '업체코드' }, { key: 'notifyUrl', label: '현금영수증통보 URL' }, { key: 'useYn', label: '사용여부' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/notify/cashReceiptSendMng': {
      searchRows: [[{ label: '전송일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'sendDt', label: '전송일시' }, { key: 'compId', label: '업체코드' }, { key: 'result', label: '결과' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/user/menuOrderMng': {
      searchRows: [[{ label: '메뉴', type: 'select', name: 'searchMenuId', options: [{ v: '', t: '선택' }] }, { type: 'searchBtn' }]],
      summary: [],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'saveBtn', label: '저장', cls: 'btn-primary' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'sortOrder', label: '순서' }, { key: 'colId', label: '항목ID' }, { key: 'colNm', label: '항목명' }, { key: 'dispYn', label: '표시여부' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/risk/list': {
      searchRows: [[{ label: '업체코드', type: 'text', name: 'searchCompId' }, { label: '리스크구분', type: 'select', name: 'searchRiskDiv', options: [{ v: '', t: '전체' }] }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'compId', label: '업체코드' }, { key: 'riskDiv', label: '리스크구분' }, { key: 'riskDesc', label: '내용' }, { key: 'regDt', label: '등록일' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    }
  };

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
    if (field.type === 'searchBtn') {
      var btnLabel = field.label || '검색';
      inner = '<div class="search-cell-input search-cell-input--right"><button type="button" class="btn btn-primary btn-sm screen-search-btn"><i class="bi bi-search"></i> ' + btnLabel + '</button></div>';
      return wrapSearchCell(inner, false);
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
    var html = '<form id="screenSearchForm" class="screen-search-form" onsubmit="return false;">';
    rows.forEach(function (r) { html += renderSearchRow(r); });
    rows2.forEach(function (r) { html += renderSearchRow(r); });
    rows3.forEach(function (r) { html += renderSearchRow(r); });
    html += '</form>';
    return html;
  }

  function renderNotice(cfg) {
    var list = cfg.noticeList || [];
    if (list.length === 0) return '';
    var items = list.map(function (t) { return '<li>' + t + '</li>'; }).join('');
    return '<div class="search-notice mb-2"><ul class="mb-0">' + items + '</ul></div>';
  }

  function renderFormField(f, readonlyAttr) {
    var col = f.col || 2;
    var req = (f.label && f.label.indexOf('*') !== -1) ? '' : '';
    var label = (f.label || '').replace(/\*$/, '') + (f.label && f.label.indexOf('*') !== -1 ? ' <span class="text-danger">*</span>' : '');
    var name = f.name || '';
    var id = name;
    var ro = (readonlyAttr || f.readonly) ? ' readonly' : '';
    var inp = '';
    if (f.type === 'text' || f.type === 'password') {
      inp = '<input type="' + (f.type || 'text') + '" class="form-control form-control-sm" name="' + name + '" id="' + id + '"' + ro + '>';
    } else if (f.type === 'select') {
      var opts = (f.options || []).map(function (o) { return '<option value="' + (o.v || '') + '">' + (o.t || o.v) + '</option>'; }).join('');
      inp = '<select class="form-control form-control-sm" name="' + name + '" id="' + id + '"' + (f.readonly ? ' disabled' : '') + '>' + opts + '</select>';
    } else if (f.type === 'textarea') {
      inp = '<textarea class="form-control form-control-sm" name="' + name + '" id="' + id + '" rows="3"' + ro + '></textarea>';
    } else if (f.type === 'file') {
      inp = '<input type="file" class="form-control form-control-sm" name="' + name + '" id="' + id + '">';
    } else {
      inp = '<input type="text" class="form-control form-control-sm" name="' + name + '" id="' + id + '"' + ro + '>';
    }
    if (f.button) {
      inp += ' <button type="button" class="btn btn-outline-secondary btn-sm ml-1" data-field="' + name + '" data-action="' + f.button + '">' + f.button + '</button>';
    }
    return '<div class="col-sm-' + col + ' mb-2"><label class="form-label">' + label + '</label>' + inp + '</div>';
  }

  function renderFormSections(cfg) {
    var sections = cfg.formSections || [];
    if (sections.length === 0) return '';
    return renderFormSectionsWithId(sections, 'compRegForm', null);
  }

  function renderFormSectionsWithId(sections, formId, buttons) {
    if (!sections || sections.length === 0) return '';
    var html = '<form id="' + (formId || 'compRegForm') + '" class="comp-reg-form" onsubmit="return false;">';
    sections.forEach(function (sec) {
      html += '<div class="card mb-3"><div class="card-header">' + (sec.title || '') + '</div><div class="card-body">';
      if (sec.notice) html += '<p class="text-muted small mb-2">' + sec.notice + '</p>';
      (sec.rows || []).forEach(function (row) {
        html += '<div class="row">';
        (row || []).forEach(function (f) { html += renderFormField(f); });
        html += '</div>';
      });
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

  function renderTable(cfg, tabId) {
    var cols = cfg.columns || [];
    var ths = cols.map(function (c) {
      if (c.type === 'checkbox') return '<th style="width:40px"><input type="checkbox" class="grid-check-all" title="전체선택"></th>';
      return '<th>' + (c.label || c.key) + '</th>';
    }).join('');
    var emptyMsg = cfg.emptyMessage || '조회된 데이터가 없습니다.';
    var emptyRow = '<tr><td colspan="' + cols.length + '" class="empty-state-cell text-center text-muted py-4">' + emptyMsg + '</td></tr>';
    var html = '<div class="table-responsive"><table class="table table-bordered table-hover table-sm" id="grid_' + (tabId || '') + '"><thead><tr>' + ths + '</tr></thead><tbody>' + emptyRow + '</tbody></table></div>';
    return html;
  }

  function renderPagination(tabId) {
    return '<div class="pagination-row">' +
      '<div class="pagination-left">' +
      '<input type="number" class="form-control form-control-sm pagination-input" id="pageCnt" value="1" min="1"> <span class="pagination-sep">/</span> <span id="totalPageCount">1</span> <button type="button" class="btn btn-primary btn-sm pagination-btn" id="pageSearch">이동</button>' +
      '</div>' +
      '<div class="pagination-center"><div class="pagination-pages" id="paging_' + (tabId || '') + '"></div></div>' +
      '<div class="pagination-right">' +
      '<select id="recordsPerPage" class="form-control form-control-sm pagination-select _searchChange"><option value="100">100</option><option value="500">500</option><option value="1000">1,000</option></select> 개씩 보기' +
      '</div>' +
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
      html += renderSearchForm(cfg);
      if (cfg.noticeList && cfg.noticeList.length > 0) html += renderNotice(cfg);
      html += renderSummaryAndActions(cfg);
      html += renderTable(cfg, tabId);
      html += renderPagination(tabId);
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

  window.PG_SCREENS = {
    getScreenHtml: getScreenHtml,
    getMenuScreens: function () { return MENU_SCREENS; }
  };
})();
