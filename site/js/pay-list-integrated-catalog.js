/**
 * 통합 결제내역(/calc/payList) 그리드 + VIEW SETTING 단일 정의.
 * 노티매핑설정 기본 카탈로그는 서버 catalog/pay-list-integrated-default.json 과 동기 유지.
 * @see site/js/screens.js applyPayListIntegratedCatalog
 */
(function (w) {
  'use strict';
  w.PG_PAY_LIST_INTEGRATED = {
    /** VIEW SETTING 체크 목록에서 제외되는 고정열 (renderTableColumnGuide) */
    columnGuideFixedKeys: ['rowNo', 'compId', 'compNm', 'compDivNm', 'trnDate', 'trnTime', 'routeNo'],
    /**
     * 저장 전·초기화 시 결제관리 통합 그리드 VIEW SETTING 기본 체크(고정열 제외).
     * 목록에 없는 열은 제거하지 않으며 체크만 꺼진 상태로 둡니다.
     */
    viewSettingDefaultSelectedKeys: [
      'chillTransactionId', 'trnId', 'chillCustomer', 'orderNo', 'paymentChannel',
      'payCompletedAt', 'chillAmount', 'icopayAmt', 'chillFeeAmt', 'totalAmt', 'currency',
      'regionalBaseCur', 'masterDistBaseCur', 'merchantBaseCur',
      'chillPaymentStatus', 'settledYn', 'payDivNm', 'cardAprvNo', 'productNm', 'customerNm',
      'payActions'
    ],
    /**
     * 조직항목설정: 조직 유형별 허용 열 체크의 기본안(REGIONAL 은 런타임에서 전체 토글열로 확장).
     * 본사(REGIONAL)=전체, 총판(MASTER_DIST), 지사·대리점·영업점(BRANCH_GROUP), 가맹점(MERCHANT) 순으로 좁아집니다.
     */
    orgAllowanceDefaultKeysByScope: {
      REGIONAL: null,
      MASTER_DIST: [
        'chillTransactionId', 'trnId', 'chillCustomer', 'orderNo', 'paymentChannel', 'payCompletedAt',
        'chillAmount', 'icopayAmt', 'chillFeeAmt', 'totalAmt', 'currency',
        'regionalBaseCur', 'masterDistBaseCur', 'merchantBaseCur',
        'chillPaymentStatus', 'settledYn',
        'payDivNm', 'cardAprvNo', 'productNm', 'customerNm', 'customerTel', 'regionalNm', 'masterNm', 'branchNm',
        'compRegNo', 'payCard', 'instalMonth', 'payMethod', 'pgNm', 'pgApproveAmt', 'payAprv',
        'holdAmt', 'holdDttm', 'feeCnt', 'feeRate', 'settleAmt', 'calcDt', 'pgApproveNo', 'corpNm', 'terminalId', 'calcCycle',
        'payCardNo', 'payActions'
      ],
      BRANCH_GROUP: [
        'chillTransactionId', 'trnId', 'chillCustomer', 'orderNo', 'paymentChannel', 'payCompletedAt',
        'chillAmount', 'icopayAmt', 'chillFeeAmt', 'totalAmt', 'currency',
        'regionalBaseCur', 'masterDistBaseCur', 'merchantBaseCur',
        'chillPaymentStatus', 'settledYn',
        'payDivNm', 'cardAprvNo', 'productNm', 'customerNm', 'customerTel', 'regionalNm', 'masterNm', 'branchNm',
        'compRegNo', 'payMethod', 'pgNm', 'pgApproveAmt', 'payAprv', 'holdAmt', 'holdDttm', 'feeCnt', 'feeRate',
        'settleAmt', 'calcDt', 'pgApproveNo'
      ],
      MERCHANT: [
        'chillTransactionId', 'trnId', 'chillCustomer', 'orderNo', 'paymentChannel', 'payCompletedAt',
        'chillAmount', 'icopayAmt', 'chillFeeAmt', 'totalAmt', 'currency',
        'regionalBaseCur', 'masterDistBaseCur', 'merchantBaseCur',
        'chillPaymentStatus', 'settledYn',
        'payDivNm', 'cardAprvNo', 'productNm', 'customerNm', 'payActions'
      ]
    },
    headerGroups: [
      { label: '사업자번호', keys: ['compRegNo'] },
      { label: 'PG승인', keys: ['pgApproveAmt', 'payAprv'] },
      { label: '보류', keys: ['holdAmt', 'holdDttm'] },
      { label: '수수료', keys: ['feeCnt', 'feeRate'] }
    ],
    /** gridType: 'checkbox' | 'payActions', 그 외 일반 열 */
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
      { key: 'payCompletedAt', label: '결제시각' },
      { key: 'chillAmount', label: '결제금액' },
      { key: 'icopayAmt', label: 'ICOPAY' },
      { key: 'chillFeeAmt', label: '수수료' },
      { key: 'totalAmt', label: '총금액' },
      { key: 'currency', label: '통화' },
      { key: 'regionalBaseCur', label: '본사기준통화' },
      { key: 'masterDistBaseCur', label: '총판기준통화' },
      { key: 'merchantBaseCur', label: '가맹기준통화' },
      { key: 'chillPaymentStatus', label: '상태' },
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
      { key: 'customerTel', label: '휴대폰(결제자)' },
      { key: 'regionalNm', label: '총판' },
      { key: 'masterNm', label: '지사' },
      { key: 'branchNm', label: '대리점' },
      { key: 'payActions', label: '후속조치', gridType: 'payActions' }
    ]
  };

  /** 통합내역(/calc/chillPayTrList) VIEW SETTING·조직항목설정 기본안 (고정: 번호·TransactionId·업체·거래일·거래시간·Route) */
  w.PG_CHILL_PAY_TR_VIEW_DEFAULTS = {
    viewSettingDefaultSelectedKeys: [
      'merchant', 'customer', 'orderNo', 'paymentChannel',
      'payCompletedAt', 'amount', 'fee', 'totalAmount', 'currency', 'status', 'settled', 'icopay'
    ],
    orgAllowanceDefaultKeysByScope: {
      REGIONAL: null,
      MASTER_DIST: [
        'merchant', 'customer', 'orderNo', 'paymentChannel', 'payCompletedAt',
        'amount', 'refundAmount', 'fee', 'discount', 'totalAmount', 'currency', 'status', 'settled', 'icopay', 'description',
        'transactionDate', 'paymentDate'
      ],
      BRANCH_GROUP: [
        'merchant', 'customer', 'orderNo', 'paymentChannel', 'payCompletedAt',
        'amount', 'fee', 'totalAmount', 'currency', 'status', 'settled', 'icopay', 'refundAmount',
        'transactionDate', 'paymentDate'
      ],
      MERCHANT: [
        'merchant', 'customer', 'orderNo', 'paymentChannel', 'payCompletedAt',
        'amount', 'currency', 'status', 'settled', 'fee', 'totalAmount'
      ]
    }
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
        'amount', 'refundAmount', 'fee', 'discount', 'totalAmount', 'icopay', 'currency', 'status', 'description',
        'transactionDate', 'paymentDate'
      ],
      BRANCH_GROUP: [
        'transactionId', 'trnDate', 'trnTime', 'routeNo', 'merchant', 'customer', 'orderNo', 'paymentChannel', 'payCompletedAt',
        'settleAmount', 'netAmount', 'settled', 'transferDate', 'icopayExpectedSettleAt', 'cutOffTime', 'exchangeRate', 'serviceAmount', 'serviceVAT', 'serviceWHT',
        'amount', 'refundAmount', 'fee', 'discount', 'totalAmount', 'icopay', 'currency', 'status', 'description',
        'transactionDate', 'paymentDate'
      ],
      MERCHANT: [
        'transactionId', 'trnDate', 'trnTime', 'routeNo', 'merchant', 'customer', 'orderNo', 'paymentChannel', 'payCompletedAt',
        'settleAmount', 'netAmount', 'settled', 'transferDate', 'icopayExpectedSettleAt', 'cutOffTime', 'amount', 'fee', 'currency', 'status'
      ]
    }
  };
})(typeof window !== 'undefined' ? window : globalThis);
