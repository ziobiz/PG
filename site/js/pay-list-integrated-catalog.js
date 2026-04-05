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
})(typeof window !== 'undefined' ? window : globalThis);
