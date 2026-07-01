/**
 * 통합 결제내역(/calc/payList) 그리드 + VIEW SETTING 단일 정의.
 * 노티매핑설정 기본 카탈로그는 서버 catalog/pay-list-integrated-default.json 과 동기 유지.
 * @see site/js/screens.js applyPayListIntegratedCatalog
 */
(function (w) {
  'use strict';
  /** 결제내역·결제개요 등 — VIEW SETTING·그리드에서 숨김(수수료내역 /calc/feeList 전용) */
  var PAY_LIST_FEE_VIEW_HIDDEN_KEYS = ['chillFeeAmt', 'feeCnt', 'feeRate'];
  /** 동일 데이터·동일 의미 중복 열 — VIEW SETTING·조직항목설정에서 제외 */
  var PAY_LIST_DUPLICATE_VIEW_HIDDEN_KEYS = [
    'cardAprvNo', 'displayPayCur', 'displayPayAmt', 'customerNm', 'corpNm', 'payAprv', 'pgApproveAmt'
  ];
  w.PG_PAY_LIST_FEE_VIEW_HIDDEN_KEYS = PAY_LIST_FEE_VIEW_HIDDEN_KEYS.slice();
  w.PG_PAY_LIST_DUPLICATE_VIEW_HIDDEN_KEYS = PAY_LIST_DUPLICATE_VIEW_HIDDEN_KEYS.slice();

  w.PG_PAY_LIST_INTEGRATED = {
    /** VIEW SETTING 체크 목록에서 제외되는 고정열 (renderTableColumnGuide) */
    columnGuideFixedKeys: ['rowNo', 'compId', 'compNm', 'compDivNm', 'trnDate', 'trnTime'],
    /** VIEW SETTING·조직항목설정·그리드 토글에서 제외(수수료내역 화면만 수수료 열 사용) */
    columnGuideHiddenKeys: PAY_LIST_FEE_VIEW_HIDDEN_KEYS.concat(PAY_LIST_DUPLICATE_VIEW_HIDDEN_KEYS),
    /**
     * 저장 전·초기화 시 결제관리 통합 그리드 VIEW SETTING 기본 체크(고정열 제외).
     * 헬로 우선순위(viewSettingHelloPriorityKeys)와 동일 — 승인번호·거래번호·주문번호·정산주기·고객·통화·결제금액·카드번호·상태·후속조치.
     */
    viewSettingDefaultSelectedKeys: [
      'chillTransactionId', 'trnId', 'orderNo', 'calcCycle', 'chillCustomer',
      'currency', 'chillAmount', 'payCardNo', 'chillPaymentStatus', 'payActions'
    ],
    /** 헬로 활성화 시 VIEW SETTING·그리드 토글 열 우선 순서(거래시간 등 고정열은 그 앞에 항상 표시) */
    viewSettingHelloPriorityKeys: [
      'chillTransactionId', 'trnId', 'orderNo', 'calcCycle', 'chillCustomer',
      'currency', 'chillAmount', 'payCardNo', 'chillPaymentStatus', 'payActions'
    ],
    /**
     * 조직항목설정: 조직 유형별 허용 열 체크의 기본안(REGIONAL 은 런타임에서 전체 토글열로 확장).
     * 본사(REGIONAL)=전체, 총판(MASTER_DIST), 지사·대리점·영업점(BRANCH_GROUP), 가맹점(MERCHANT) 순으로 좁아집니다.
     */
    orgAllowanceDefaultKeysByScope: {
      REGIONAL: null,
      MASTER_DIST: [
        'routeNo', 'chillTransactionId', 'trnId', 'chillCustomer', 'orderNo', 'paymentChannel',
        'payerDeviceLabel', 'payerRegion',
        'payCompletedAt',
        'chillAmount', 'icopayAmt', 'totalAmt', 'currency',
        'payCustomerIndicator', 'displayPaySummary',
        'regionalBaseCur', 'masterDistBaseCur', 'merchantBaseCur',
        'chillPaymentStatus', 'outcomeReasonPreview', 'settledYn',
        'payDivNm', 'productNm', 'customerEmail', 'customerTel', 'regionalNm', 'masterNm', 'branchNm',
        'compRegNo', 'payCard', 'instalMonth', 'payMethod', 'pgNm', 'pgApproveNo',
        'holdAmt', 'holdDttm', 'settleAmt', 'calcDt', 'terminalId', 'calcCycle',
        'payCardNo', 'payActions', 'payRemark'
      ],
      BRANCH_GROUP: [
        'routeNo', 'chillTransactionId', 'trnId', 'chillCustomer', 'orderNo', 'paymentChannel',
        'payerDeviceLabel', 'payerRegion',
        'payCompletedAt',
        'chillAmount', 'icopayAmt', 'totalAmt', 'currency',
        'payCustomerIndicator', 'displayPaySummary',
        'regionalBaseCur', 'masterDistBaseCur', 'merchantBaseCur',
        'chillPaymentStatus', 'outcomeReasonPreview', 'settledYn',
        'payDivNm', 'productNm', 'customerEmail', 'customerTel', 'regionalNm', 'masterNm', 'branchNm',
        'compRegNo', 'payMethod', 'pgNm', 'pgApproveNo', 'holdAmt', 'holdDttm',
        'settleAmt', 'calcDt'
      ],
      MERCHANT: [
        'routeNo', 'chillTransactionId', 'trnId', 'chillCustomer', 'orderNo', 'paymentChannel',
        'payerDeviceLabel', 'payerRegion',
        'payCompletedAt',
        'chillAmount', 'icopayAmt', 'totalAmt', 'currency',
        'payCustomerIndicator', 'displayPaySummary',
        'regionalBaseCur', 'masterDistBaseCur', 'merchantBaseCur',
        'chillPaymentStatus', 'outcomeReasonPreview', 'settledYn',
        'payDivNm', 'productNm', 'payActions', 'payRemark'
      ]
    },
    headerGroups: [
      { label: '사업자번호', keys: ['compRegNo'] },
      { label: 'PG승인', keys: ['pgApproveNo'] },
      { label: '보류', keys: ['holdAmt', 'holdDttm'] },
      { label: '고객표시', keys: ['payCustomerIndicator'] },
      { label: '입력통화', keys: ['displayPaySummary'] }
    ],
    /** gridType: 'checkbox' | 'payActions' | 'payRemark', 그 외 일반 열 */
    columns: [
      { key: '_chk', label: '선택', gridType: 'checkbox' },
      { key: 'rowNo', label: '번호' },
      { key: 'compNm', label: '업체명' },
      { key: 'compId', label: '업체코드' },
      { key: 'trnDate', label: '거래일' },
      { key: 'trnTime', label: '거래시간' },
      { key: 'routeNo', label: '루트' },
      { key: 'chillTransactionId', label: '승인번호' },
      { key: 'trnId', label: '거래번호(우리)' },
      { key: 'chillCustomer', label: '고객' },
      { key: 'orderNo', label: '주문번호' },
      { key: 'paymentChannel', label: 'Payment Channel' },
      { key: 'payerDeviceLabel', label: '단말기' },
      { key: 'payerRegion', label: '위치' },
      { key: 'payCompletedAt', label: '결제시각' },
      { key: 'chillAmount', label: '결제금액' },
      { key: 'icopayAmt', label: 'ICOPAY' },
      { key: 'chillFeeAmt', label: '수수료' },
      { key: 'totalAmt', label: '총금액' },
      { key: 'currency', label: '통화' },
      { key: 'payCustomerIndicator', label: '고객표시' },
      { key: 'displayPaySummary', label: '통화ㅣ금액', columnGuideLabel: '입력통화' },
      { key: 'displayPayCur', label: '고객통화' },
      { key: 'displayPayAmt', label: '고객금액' },
      { key: 'regionalBaseCur', label: '본사기준통화' },
      { key: 'masterDistBaseCur', label: '총판기준통화' },
      { key: 'merchantBaseCur', label: '가맹기준통화' },
      { key: 'chillPaymentStatus', label: '상태' },
      { key: 'outcomeReasonPreview', label: '처리사유' },
      { key: 'settledYn', label: '정산' },
      { key: 'compRegNo', label: '사업자번호' },
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
      { key: 'customerEmail', label: '이메일(결제자)' },
      { key: 'customerTel', label: '휴대폰(결제자)' },
      { key: 'regionalNm', label: '총판' },
      { key: 'masterNm', label: '지사' },
      { key: 'branchNm', label: '대리점' },
      { key: 'payActions', label: '후속조치', gridType: 'payActions' },
      { key: 'payRemark', label: '비고', gridType: 'payRemark' }
    ]
  };

  /** 통합내역(/calc/chillPayTrList) VIEW SETTING·조직항목설정 기본안 (고정: 번호·TransactionId·업체·거래일·거래시간·Route) */
  w.PG_CHILL_PAY_TR_VIEW_DEFAULTS = {
    viewSettingDefaultSelectedKeys: [
      'routeNo', 'merchant', 'customer', 'orderNo', 'paymentChannel',
      'payCompletedAt', 'amount', 'fee', 'totalAmount', 'currency', 'status', 'settled', 'icopay'
    ],
    orgAllowanceDefaultKeysByScope: {
      REGIONAL: null,
      MASTER_DIST: [
        'routeNo', 'merchant', 'customer', 'orderNo', 'paymentChannel', 'payCompletedAt',
        'amount', 'refundAmount', 'fee', 'discount', 'totalAmount', 'currency', 'status', 'settled', 'icopay', 'description'
      ],
      BRANCH_GROUP: [
        'routeNo', 'merchant', 'customer', 'orderNo', 'paymentChannel', 'payCompletedAt',
        'amount', 'fee', 'totalAmount', 'currency', 'status', 'settled', 'icopay', 'refundAmount'
      ],
      MERCHANT: [
        'merchant', 'customer', 'orderNo', 'paymentChannel', 'payCompletedAt',
        'amount', 'currency', 'status', 'settled', 'fee', 'totalAmount'
      ]
    }
  };

  /** 통합개요(/calc/jpayTrList) VIEW SETTING — PG_JPAY_TR_OVERVIEW 참고 */
  w.PG_JPAY_TR_VIEW_DEFAULTS = {
    viewSettingDefaultSelectedKeys: [
      'masterDistNm', 'portalLabel', 'merchant', 'orderNo', 'customer', 'amount', 'currency',
      'icopay', 'statusNm', 'fee', 'refundStatus', 'chargeback', 'cardBin', 'urlSource'
    ]
  };

  /** 통합정산(/calc/chillPaySettlementList) VIEW SETTING·조직항목설정 기본안 (고정열: 번호만 — 통화 포함 나머지는 토글·허용 목록) */
  w.PG_CHILL_PAY_SETTLEMENT_VIEW_DEFAULTS = {
    viewSettingDefaultSelectedKeys: [
      'transactionId', 'trnDate', 'trnTime', 'routeNo', 'merchant', 'customer', 'orderNo', 'paymentChannel', 'payCompletedAt',
      'settleAmount', 'netAmount', 'settled', 'transferDate', 'icopayExpectedSettleAt', 'cutOffTime', 'exchangeRate', 'serviceAmount', 'serviceVAT', 'serviceWHT',
      'amount', 'fee', 'currency', 'status', 'icopay', 'description'
    ],
    orgAllowanceDefaultKeysByScope: {
      REGIONAL: null,
      MASTER_DIST: [
        'transactionId', 'trnDate', 'trnTime', 'routeNo', 'merchant', 'customer', 'orderNo', 'paymentChannel', 'payCompletedAt',
        'settleAmount', 'netAmount', 'settled', 'transferDate', 'icopayExpectedSettleAt', 'cutOffTime', 'exchangeRate', 'serviceAmount', 'serviceVAT', 'serviceWHT',
        'amount', 'fee', 'currency', 'status', 'icopay', 'description'
      ],
      BRANCH_GROUP: [
        'transactionId', 'trnDate', 'trnTime', 'routeNo', 'merchant', 'customer', 'orderNo', 'paymentChannel', 'payCompletedAt',
        'settleAmount', 'netAmount', 'settled', 'transferDate', 'icopayExpectedSettleAt', 'cutOffTime', 'exchangeRate', 'serviceAmount', 'serviceVAT', 'serviceWHT',
        'amount', 'refundAmount', 'fee', 'discount', 'totalAmount', 'icopay', 'currency', 'status', 'description'
      ],
      MERCHANT: [
        'transactionId', 'trnDate', 'trnTime', 'routeNo', 'merchant', 'customer', 'orderNo', 'paymentChannel', 'payCompletedAt',
        'settleAmount', 'netAmount', 'settled', 'transferDate', 'icopayExpectedSettleAt', 'cutOffTime', 'amount', 'fee', 'currency', 'status'
      ]
    }
  };
})(typeof window !== 'undefined' ? window : globalThis);
