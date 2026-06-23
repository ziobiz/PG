/**
 * PG admin — per-menu screen HTML (search form, summary, buttons, grid, pagination).
 * User-visible Korean is wrapped with L() and/or data-pg-ui-t for PG_UI_I18N.applyDom after render.
 */
(function () {
  'use strict';

  /**
   * 화면 정의는 앱 시작 시 1회 로드되고, 문자열이 여기서 즉시 번역되면(특히 JP/EN)
   * 이후 언어 변경 시 원문(키)을 복원할 수 없어 일부 문구가 고정됩니다.
   *
   * 따라서 이 파일의 L()은 "번역"이 아니라 "키(기본 KO 원문)"를 그대로 반환하고,
   * 실제 번역은 렌더 후 `PG_UI_I18N.applyDom` / STRING_MAP 파이프라인에서 처리합니다.
   */
  function L(s) {
    return s;
  }
  function escUi(s) {
    return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
  }

  /** 라벨: data-pg-ui-t는 내부 span만 적용(applyDom이 textContent를 덮어써도 필수 * 표시 유지). */
  function pgUiFormLabelSpan(keyKo, hasStar) {
    keyKo = String(keyKo == null ? '' : keyKo);
    if (!keyKo) return '<label class="form-label"></label>';
    var star = hasStar ? ' <span class="text-danger">*</span>' : '';
    return '<label class="form-label"><span data-pg-ui-t="' + escUi(keyKo) + '">' + escUi(L(keyKo)) + '</span>' + star + '</label>';
  }

  /** 정산·담보·환수·미수 — 빠른기간(당일~전월) */
  var QD_SETTLE_STD_LABELS = ['당일', '당월', '전일', '1주', '2주', '전월'];
  var QD_SETTLE_STD_RANGES = ['day', 'month', 'prevDay', 'week', 'week2', 'prevMonth'];
  /** 유통망정산 — 월 단위 + 2개월전 */
  var QD_DIST_MONTHLY_LABELS = QD_SETTLE_STD_LABELS.concat(['2개월전']);
  var QD_DIST_MONTHLY_RANGES = QD_SETTLE_STD_RANGES.concat(['prevMonth2']);
  function qdSettleStdField() {
    return { type: 'quickdate', quickdateLabels: QD_SETTLE_STD_LABELS.slice(), quickdateRanges: QD_SETTLE_STD_RANGES.slice() };
  }
  function qdDistMonthlyField() {
    return { type: 'quickdate', quickdateLabels: QD_DIST_MONTHLY_LABELS.slice(), quickdateRanges: QD_DIST_MONTHLY_RANGES.slice() };
  }

  function pgUiSpanText(keyKo) {
    keyKo = String(keyKo == null ? '' : keyKo);
    return '<span data-pg-ui-t="' + escUi(keyKo) + '">' + escUi(L(keyKo)) + '</span>';
  }

  function pgUiParagraph(keyKo, className) {
    var cls = className || 'small text-muted mb-3';
    keyKo = String(keyKo == null ? '' : keyKo);
    return '<p class="' + cls + '"><span data-pg-ui-t="' + escUi(keyKo) + '">' + escUi(L(keyKo)) + '</span></p>';
  }

  /** 안내 문단(번역에 <code>/<strong> 등 HTML 포함) — applyDom 시 innerHTML */
  function pgUiParagraphHtml(keyKo, className) {
    var cls = className || 'text-muted small mb-2';
    keyKo = String(keyKo == null ? '' : keyKo);
    return '<p class="' + cls + '" data-pg-ui-html="' + escUi(keyKo) + '">' + L(keyKo) + '</p>';
  }

  /** formSections.notice — 본문 중간에 <strong>/<code> 등이 있어도 HTML 안내로 처리 */
  function pgUiNoticeHasHtml(s) {
    return /<\s*(?:\/)?(?:strong|em|b|i|u|code|span|br|a|p|div|ul|ol|li)\b/i.test(String(s == null ? '' : s));
  }

  /** 인라인/표 셀 — applyDom 시 textContent */
  function pgUiSpanT(keyKo, className) {
    keyKo = String(keyKo == null ? '' : keyKo);
    var cls = className ? ' class="' + className + '"' : '';
    return '<span' + cls + ' data-pg-ui-t="' + escUi(keyKo) + '">' + escUi(L(keyKo)) + '</span>';
  }

  function pgUiThT(keyKo, className) {
    keyKo = String(keyKo == null ? '' : keyKo);
    var cls = className ? ' class="' + className + '"' : '';
    return '<th' + cls + ' data-pg-ui-t="' + escUi(keyKo) + '">' + escUi(L(keyKo)) + '</th>';
  }

  function pgUiTdT(keyKo, className) {
    keyKo = String(keyKo == null ? '' : keyKo);
    var cls = className ? ' class="' + className + '"' : '';
    return '<td' + cls + ' data-pg-ui-t="' + escUi(keyKo) + '">' + escUi(L(keyKo)) + '</td>';
  }

  function pgUiOptHtml(opts) {
    return opts.map(function (o) {
      var t = String(o.t != null ? o.t : '');
      return '<option value="' + escUi(o.v) + '" data-pg-ui-t="' + escUi(t) + '">' + escUi(L(t)) + '</option>';
    }).join('');
  }

  function pgUiLiT(keyKo) {
    keyKo = String(keyKo == null ? '' : keyKo);
    return '<li><span data-pg-ui-t="' + escUi(keyKo) + '">' + escUi(L(keyKo)) + '</span></li>';
  }

  function pgUiCardHeaderT(keyKo) {
    return '<div class="card-header fw-semibold">' + pgUiSpanText(keyKo) + '</div>';
  }

  /** 본사 AI챗봇설정 — 챗봇 플랜 월요금 통화 열 (서버 ChatbotProductPricingUtil.BILLING_CURRENCIES 와 동일) */
  var PG_CHATBOT_PLAN_CCY = ['JPY', 'KRW', 'USD', 'CNY', 'THB'];
  if (typeof window !== 'undefined') {
    window.PG_CHATBOT_PLAN_CCY = PG_CHATBOT_PLAN_CCY;
  }

  /** 가맹/업체정보조회·업체정보조회: 무효·환불 안내(HTML은 STATIC의 L 결과) */
  function merchantVoidRefundGuideHtml() {
    var k = '무효·환불 정산 방식 카드 안내';
    return '<p class="small text-muted mb-3 mb-md-2"><span data-pg-ui-t="' + escUi(k) + '">' + escUi(L(k)) + '</span></p>';
  }
  function merchantWebPaymentCardNoticeKo() {
    return '미사용 선택 시 WEB 결제 시스템이 중지됩니다. 「결제 URL」은 운영·WEB·URL결제 PG별 공개 경로로 자동 표시됩니다(예: JPAY /jpay-pay/업체코드, ChillPay /pay/업체코드). 「URL 재결제 URL」은 해당 PG가 저장 카드 재결제를 지원하고 본사 URL 재결제 기능·URL재결제 PG 바인딩이 있을 때만 표시됩니다. 「URL 결제 방식」은 공개 URL 결제에만 적용됩니다. API·챗봇은 각 설정 카드에서 별도 선택합니다.';
  }

  /** 동일 id(paymentUrlDisplay) — 화면별 placeholder 키만 다름 */
  function merchantPaymentUrlRowHtml(placeholderKo) {
    var ph = placeholderKo || '가맹점 저장 후 조회';
    return '<div class="row mb-2"><div class="col-sm-5"><label class="form-label" data-pg-ui-t="결제 URL">' + escUi(L('결제 URL')) + '</label><div class="input-group input-group-sm"><input type="text" class="form-control" id="paymentUrlDisplay" readonly placeholder="' + escUi(L(String(ph))) + '" data-pg-ui-placeholder="' + escUi(String(ph)) + '"><button type="button" class="btn btn-outline-primary" id="paymentUrlCopyBtn" data-pg-ui-t="복사">' + escUi(L('복사')) + '</button></div></div></div>';
  }

  function merchantPaymentRepayUrlRowHtml(placeholderKo) {
    var ph = placeholderKo || '가맹점 저장 후 조회';
    return '<div class="row mb-2"><div class="col-sm-5"><label class="form-label" data-pg-ui-t="URL 재결제 URL">' + escUi(L('URL 재결제 URL')) + '</label><div class="input-group input-group-sm"><input type="text" class="form-control" id="paymentRepayUrlDisplay" readonly placeholder="' + escUi(L(String(ph))) + '" data-pg-ui-placeholder="' + escUi(String(ph)) + '"><button type="button" class="btn btn-outline-primary" id="paymentRepayUrlCopyBtn" data-pg-ui-t="복사">' + escUi(L('복사')) + '</button></div></div></div>';
  }

  /** 웹결제(URL·JPAY) 상단 로고 — 로고설정「활성」일 때만 업로드 */
  function webPaymentHeaderLogoFieldBlock() {
    var phLogo = '업로드 시 자동 반영 · 또는 HTTPS URL 직접 입력';
    var logoHint = '「활성」일 때만 업로드 가능합니다. PNG·JPEG, 원본 최대 40MB. 서버에서 목표 2MB 이하(본사 AI챗봇설정과 동일)로 재압축합니다.';
    return '<div class="form-field-block web-payment-header-logo-upload-block w-100" id="webPaymentHeaderLogoBlock">' +
      '<label class="form-label" data-pg-ui-t="웹결제 상단 로고">' + escUi(L('웹결제 상단 로고')) + '</label>' +
      '<div class="input-group input-group-sm mb-1">' +
      '<input type="text" class="form-control form-control-sm" name="webPaymentHeaderLogoUrl" id="webPaymentHeaderLogoUrl" ' +
      'placeholder="' + escUi(L(phLogo)) + '" data-pg-ui-placeholder="' + escUi(phLogo) + '">' +
      '<input type="file" class="d-none" id="webPaymentHeaderLogoFile" accept="image/png,image/jpeg,image/jpg">' +
      '<button type="button" class="btn btn-outline-secondary btn-sm" id="webPaymentHeaderLogoBrowse"><span data-pg-ui-t="파일 선택">' + escUi(L('파일 선택')) + '</span></button>' +
      '<button type="button" class="btn btn-outline-primary btn-sm" id="webPaymentHeaderLogoUpload"><span data-pg-ui-t="업로드·최적화">' + escUi(L('업로드·최적화')) + '</span></button>' +
      '</div>' +
      '<div class="form-text text-muted small" data-pg-ui-t="' + escUi(logoHint) + '">' +
      escUi(L(logoHint)) +
      '</div></div>';
  }

  /** 웹결제 결제창 로고 아래 경고문구 — 경고메세지「활성」일 때만 입력 */
  function webPaymentHeaderSubtitleFieldBlock() {
    var ph = '결제창 로고 아래에 표시할 문구';
    var hint = '「활성」일 때만 직접 입력 가능합니다. 「기본」은 3DS 안전 결제 문구가 언어별로 표시됩니다. 로고설정이 미활성이면 문구도 표시되지 않습니다.';
    return '<div class="form-field-block web-payment-header-subtitle-block w-100" id="webPaymentHeaderSubtitleBlock">' +
      '<label class="form-label" data-pg-ui-t="경고메세지 문구">' + escUi(L('경고메세지 문구')) + '</label>' +
      '<input type="text" class="form-control form-control-sm" name="webPaymentHeaderSubtitleText" id="webPaymentHeaderSubtitleText" ' +
      'maxlength="200" placeholder="' + escUi(L(ph)) + '" data-pg-ui-placeholder="' + escUi(ph) + '">' +
      '<div class="form-text text-muted small" data-pg-ui-t="' + escUi(hint) + '">' +
      escUi(L(hint)) +
      '</div></div>';
  }

  function urlPayInputModeHintHtml() {
    var key = '입력방식 일반 설명';
    return '<div class="col-12"><p class="form-text text-muted small mb-2 url-pay-input-mode-hint" data-pg-ui-t="' + escUi(key) + '">' +
      escUi(L(key)) + '</p></div>';
  }

  /** 웹결제 카드 1행 — 웹결제·URL방식·입력방식 */
  function merchantWebPaymentCardPrimaryRow() {
    var ynUseOpts = [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }];
    return [
      { label: '웹결제', type: 'select', name: 'webPaymentUseYn', options: ynUseOpts, col: 2 },
      { label: 'URL 결제 방식', type: 'select', name: 'urlPayCheckoutMode', options: [{ v: 'STANDARD', t: '일반 URL 결제' }, { v: 'REPAY', t: '재결제 URL (저장 카드)' }], col: 2 },
      { label: '입력방식', type: 'select', name: 'urlPayInputMode', options: [
        { v: 'GENERAL', t: '일반' },
        { v: 'TYPE_AA', t: 'AA 타입' },
        { v: 'TYPE_BA', t: 'BA 타입' },
        { v: 'TYPE_AN', t: 'AN 타입' },
        { v: 'TYPE_AG', t: 'AG 타입' },
        { v: 'TYPE_AF', t: 'AF 타입' },
        { v: 'TYPE_AE', t: 'AE 타입' },
        { v: 'TYPE_BN', t: 'BN 타입' },
        { v: 'TYPE_BG', t: 'BG 타입' },
        { v: 'TYPE_BF', t: 'BF 타입' },
        { v: 'TYPE_BE', t: 'BE 타입' },
        { v: 'TYPE_CN', t: 'CN 타입' }
      ], col: 2 }
    ];
  }

  /** 웹결제 카드 2행 — 가맹점명·다국어 */
  function merchantWebPaymentCardSecondaryRow() {
    var activeOpts = [{ v: 'Y', t: '활성' }, { v: 'N', t: '비활성' }];
    return [
      { label: '가맹점명', type: 'select', name: 'urlPayCompanyNameShowYn', options: activeOpts, col: 2 },
      { label: '다국어 메뉴', type: 'select', name: 'urlPayLangMenuUseYn', options: activeOpts, col: 2 }
    ];
  }

  /** 웹결제 카드 — 대표 기본상품(상품명 사용 토글 포함) */
  function merchantWebPaymentDefaultProductRow() {
    return [
      { label: '상품명 사용', type: 'select', name: 'urlPayProductNameUseYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 },
      { label: '상품명', type: 'text', name: 'defaultProductName', col: 2, placeholder: '대표 상품명', blockExtraClass: 'url-pay-product-field' },
      { label: '상품코드', type: 'text', name: 'defaultProductCode', col: 1, blockExtraClass: 'url-pay-product-field' },
      { label: '기본금액', type: 'text', name: 'defaultProductAmount', col: 1, placeholder: '', blockExtraClass: 'url-pay-product-field' },
      { label: '상품설명', type: 'text', name: 'defaultProductDesc', col: 4, blockExtraClass: 'url-pay-product-field' }
    ];
  }

  function merchantWebPaymentCardRows(urlPlaceholderKo) {
    var urlPh = urlPlaceholderKo || '가맹점 저장 후 조회';
    return [
      merchantWebPaymentCardPrimaryRow(),
      [{ type: 'customHtml', col: 12, html: urlPayInputModeHintHtml }],
      merchantWebPaymentCardSecondaryRow(),
      [{ label: '로고설정', type: 'select', name: 'webPaymentHeaderLogoMode', options: [
        { v: 'DEFAULT', t: '기본(총판 로고)' },
        { v: 'HTML', t: '기본(HTML)' },
        { v: 'ACTIVE', t: '활성(가맹점 로고)' },
        { v: 'DISABLED', t: '미활성' }
      ], col: 3 }],
      [{ type: 'customHtml', col: 12, html: webPaymentHeaderLogoFieldBlock }],
      [{ label: '경고메세지', type: 'select', name: 'webPaymentHeaderSubtitleMode', options: [
        { v: 'DEFAULT', t: '기본(3DS 안전 결제)' }, { v: 'DISABLED', t: '비활성' }, { v: 'ACTIVE', t: '활성(직접 입력)' }
      ], col: 3 }],
      [{ type: 'customHtml', col: 12, html: webPaymentHeaderSubtitleFieldBlock }],
      merchantWebPaymentDefaultProductRow(),
      [{ type: 'customHtml', col: 12, html: function () { return merchantPaymentUrlRowHtml(urlPh); } },
       { type: 'customHtml', col: 12, html: function () { return merchantPaymentRepayUrlRowHtml(urlPh); } }]
    ];
  }

  /** 가맹 등록·정보 — 가맹 API 연동 채널(인라인·리다이렉트·WordPress) */
  function merchantApiIntegrationChannelsCardSection() {
    return {
      title: '가맹 API 연동 채널',
      id: 'merchantApiIntegrationChannelsCard',
      merchantOnly: true,
      notice: '가맹점 API(prepare·embed·redirect·WordPress) 연동 방식을 가맹별로 오픈합니다. 본사 전역 상한은 배포설정 → 결제로직설정입니다. WordPress 사용 시 API 인라인 또는 리다이렉트 중 하나 이상을 켜야 합니다.',
      rows: [
        [{ label: 'API 인라인 연동', type: 'select', name: 'apiBrokerInlineUseYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 3 },
         { label: 'API 리다이렉트 연동', type: 'select', name: 'apiBrokerRedirectUseYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 3 },
         { label: 'WordPress/WooCommerce', type: 'select', name: 'apiWordpressUseYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 3 }],
        [{ label: '', type: 'note', col: 12, text: '가맹점 API 생성·배포문서·가맹점API 화면에는 여기서 켠 채널만 노출됩니다. prepare API도 비활성 채널은 INTEGRATION_CHANNEL_DISABLED 로 거부됩니다.' }]
      ]
    };
  }

  /** 가맹 등록·정보 — JPAY API 구독(③ 인라인 전용) */
  function merchantJpayApiSubscriptionCardSection() {
    return {
      title: 'JPAY API 구독',
      id: 'jpayApiSubscriptionCard',
      merchantOnly: true,
      notice: '가맹 API subscription/prepare · jpay-subscribe.html 전용입니다. URL·챗봇·1회 inline-checkout 과 분리됩니다. 본사 결제로직설정 구독 ON + API연동설정 API구독 PG 바인딩 필요.',
      rows: [
        [{ label: 'JPAY API 구독 사용', type: 'select', name: 'apiJpaySubscriptionUseYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 3 }],
        [{ label: '', type: 'note', col: 12, text: 'prepare: POST /api/middleware/v1/merchant/jpay/subscription/prepare · 해지: POST .../subscription/cancel (최초 orderNo)' }]
      ]
    };
  }

  /** URL 분할결제 결제창 — 로고설정「활성」일 때만 업로드 */
  function splitPayHeaderLogoFieldBlock() {
    var phLogo = '업로드 시 자동 반영 · 또는 HTTPS URL 직접 입력';
    var logoHint = '「활성(가맹점 로고)」일 때만 업로드 가능합니다. PNG·JPEG, 원본 최대 40MB. 서버에서 목표 2MB 이하로 재압축합니다.';
    return '<div class="form-field-block split-pay-header-logo-upload-block w-100" id="splitPayHeaderLogoBlock">' +
      '<label class="form-label" data-pg-ui-t="분할결제 상단 로고">' + escUi(L('분할결제 상단 로고')) + '</label>' +
      '<div class="input-group input-group-sm mb-1">' +
      '<input type="text" class="form-control form-control-sm" name="splitPayHeaderLogoUrl" id="splitPayHeaderLogoUrl" ' +
      'placeholder="' + escUi(L(phLogo)) + '" data-pg-ui-placeholder="' + escUi(phLogo) + '">' +
      '<input type="file" class="d-none" id="splitPayHeaderLogoFile" accept="image/png,image/jpeg,image/jpg">' +
      '<button type="button" class="btn btn-outline-secondary btn-sm" id="splitPayHeaderLogoBrowse"><span data-pg-ui-t="파일 선택">' + escUi(L('파일 선택')) + '</span></button>' +
      '<button type="button" class="btn btn-outline-primary btn-sm" id="splitPayHeaderLogoUpload"><span data-pg-ui-t="업로드·최적화">' + escUi(L('업로드·최적화')) + '</span></button>' +
      '</div>' +
      '<div class="form-text text-muted small" data-pg-ui-t="' + escUi(logoHint) + '">' +
      escUi(L(logoHint)) +
      '</div></div>';
  }

  /** URL 분할결제 결제창 — 안내메세지「활성」일 때만 입력 */
  function splitPayHeaderSubtitleFieldBlock() {
    var ph = '결제창 로고 아래에 표시할 문구';
    var hint = '「활성(직접입력)」일 때만 직접 입력 가능합니다. 「기본」은 분할결제 안내 문구가 언어별로 표시됩니다. 로고설정이 비활성이면 문구도 표시되지 않습니다.';
    return '<div class="form-field-block split-pay-header-subtitle-block w-100" id="splitPayHeaderSubtitleBlock">' +
      '<label class="form-label" data-pg-ui-t="안내메세지 문구">' + escUi(L('안내메세지 문구')) + '</label>' +
      '<input type="text" class="form-control form-control-sm" name="splitPayHeaderSubtitleText" id="splitPayHeaderSubtitleText" ' +
      'maxlength="200" placeholder="' + escUi(L(ph)) + '" data-pg-ui-placeholder="' + escUi(ph) + '">' +
      '<div class="form-text text-muted small" data-pg-ui-t="' + escUi(hint) + '">' +
      escUi(L(hint)) +
      '</div></div>';
  }

  /** URL 분할결제 — 월기간 1~24개월 */
  function merchantSplitPayMonthIntervalOptions() {
    var opts = [];
    for (var m = 1; m <= 24; m++) {
      opts.push({ v: String(m), t: m + '개월' });
    }
    return opts;
  }

  /** URL 분할결제 — 멀티 최대 개월 (3·5·6·12=1년) */
  function merchantSplitPayMultiMaxOptions() {
    return [
      { v: '3', t: '3개월' },
      { v: '5', t: '5개월' },
      { v: '6', t: '6개월' },
      { v: '12', t: '1년' }
    ];
  }

  /** URL 분할결제 — 일기간 (5일 단위 프리셋) */
  function merchantSplitPayDayIntervalOptions() {
    return [5, 7, 10, 15, 20, 40, 50].map(function (d) {
      return { v: String(d), t: d + '일' };
    });
  }

  function merchantSplitPayUrlRowHtml(placeholderKo) {
    var ph = placeholderKo || '가맹점 저장 후 조회';
    return '<div class="row mb-2"><div class="col-sm-5"><label class="form-label" data-pg-ui-t="분할결제 URL">' + escUi(L('분할결제 URL')) + '</label><div class="input-group input-group-sm"><input type="text" class="form-control" id="splitPayUrlDisplay" readonly placeholder="' + escUi(L(String(ph))) + '" data-pg-ui-placeholder="' + escUi(String(ph)) + '"><button type="button" class="btn btn-outline-primary" id="splitPayUrlCopyBtn" data-pg-ui-t="복사">' + escUi(L('복사')) + '</button></div></div></div>';
  }

  /** 가맹 등록·정보 — URL 분할결제 계약·회차 설정 (API URL 인라인 중계 결제와 별도) */
  function merchantSplitPayCardSection() {
    return {
      title: 'URL 분할결제',
      id: 'splitPayCard',
      merchantOnly: true,
      notice: '「분할결제 사용여부」로 URL 분할결제·분할관리 메뉴 노출을 제어합니다. 사용 ON인 가맹은 분할결제 URL(공개) 또는 분할 계약 API로 고객 계약·회차 결제를 진행합니다. 「API URL 인라인 중계 결제」의 결제방식 선택과는 별개입니다. 회차는 운영 URL PG에 따라 pay.html 또는 jpay-pay.html 입니다.',
      rows: [
        [{ label: '분할결제 사용여부', type: 'select', name: 'splitPayEnabledYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 3 }],
        [{ label: '월/일 설정', type: 'select', name: 'splitPayIntervalType', options: [
          { v: 'MONTH', t: '월간' },
          { v: 'DAY', t: '일간' },
          { v: 'MULTI', t: '멀티' }
        ], col: 3 },
         { label: '기간', type: 'select', name: 'splitPayIntervalPeriod', options: merchantSplitPayMonthIntervalOptions(), col: 3 }],
        [{ label: '1회차 결제', type: 'select', name: 'splitPayFirstPayMode', options: [{ v: 'IMMEDIATE', t: '즉시결제' }, { v: 'LINK', t: '링크발송' }], col: 3 },
         { type: 'hidden', name: 'splitPayIntervalMonthYn' },
         { type: 'hidden', name: 'splitPayIntervalDayYn' },
         { type: 'hidden', name: 'splitPayIntervalMultiYn' },
         { type: 'hidden', name: 'splitPayMonthIntervalMonths' },
         { type: 'hidden', name: 'splitPayDayIntervalDays' },
         { type: 'hidden', name: 'splitPayMultiMaxMonths' }],
        [{ label: '로고설정', type: 'select', name: 'splitPayHeaderLogoMode', options: [
          { v: 'DEFAULT', t: '기본(총판 로고)' },
          { v: 'HTML', t: '기본(HTML)' },
          { v: 'ACTIVE', t: '활성(가맹점 로고)' },
          { v: 'DISABLED', t: '비활성' }
        ], col: 3 }],
        [{ type: 'customHtml', col: 12, html: splitPayHeaderLogoFieldBlock }],
        [{ label: '안내메세지', type: 'select', name: 'splitPayHeaderSubtitleMode', options: [
          { v: 'DEFAULT', t: '기본(분할결제 안내)' }, { v: 'DISABLED', t: '비활성' }, { v: 'ACTIVE', t: '활성(직접입력)' }
        ], col: 3 },
         { label: '다국어 메뉴', type: 'select', name: 'splitPayLangMenuUseYn', options: [{ v: 'Y', t: '활성' }, { v: 'N', t: '비활성' }], col: 3 }],
        [{ type: 'customHtml', col: 12, html: splitPayHeaderSubtitleFieldBlock }],
        [{ type: 'customHtml', col: 12, html: merchantSplitPayUrlRowHtml('가맹점 저장 후 조회') }],
        [{ label: '', type: 'note', col: 12, text: '분할결제 사용 ON 시 월간·일간·멀티 중 하나를 설정합니다. 멀티는 고객이 1개월~설정 최대개월 중 기간을 직접 선택합니다. 1회차는 즉시결제 또는 링크발송. 미납 회차는 매일 결제 링크 메일이 발송됩니다. 미사용이면 분할관리·분할결제내역 메뉴가 숨겨집니다.' }]
      ]
    };
  }

  /** 가맹 등록·정보 — API URL 인라인 중계 결제 방식(공개 URL·챗봇과 별도) */
  function merchantApiUrlPayCheckoutCardSection() {
    return {
      title: 'API URL 인라인 중계 결제',
      id: 'apiUrlPayCheckoutCard',
      merchantOnly: true,
      notice: '가맹 쇼핑몰 등에서 API 인라인 inline-checkout/prepare 를 호출할 때 저장된 값이 payUrl·결제창에 반영됩니다. 공개 결제 URL·챗봇결제와는 별도 설정입니다. ChillPay·JPAY API 인라인 모두 동일하게 적용됩니다.',
      rows: [
        [{ label: 'URL 결제 방식', type: 'select', name: 'apiUrlPayCheckoutMode', options: [
          { v: 'STANDARD', t: '일반 결제' },
          { v: 'REPAY', t: '재구매 결제' },
          { v: 'SPLIT_PAY', t: '분할 결제' }
        ], col: 3 }],
        [{ label: '', type: 'note', col: 12, text: '필수: 가맹 「API 인라인 연동」·「웹결제」사용, 운영 URL PG 바인딩, 본사 결제로직 URL INLINE 제공(Y). 일반 결제 → pay.html 또는 jpay-pay.html. 재구매 결제 → ChillPay API 인라인만(pay-repay), JPAY API 인라인 미지원. 분할 결제 → 1회 prepare 불가, API 분할 계약(POST /api/pay/split/contracts) 이용. 공개 URL 분할결제는 「URL 분할결제」사용 ON으로 별도 운영합니다.' }]
      ]
    };
  }

  /** 가맹 등록·정보 — JPAY jpay-pay.html 결제창 입력 필드(본사 기본 오버라이드) */
  function merchantJpayCheckoutFieldModeCardSection() {
    return {
      title: 'JPAY 결제창 입력 필드',
      id: 'jpayCheckoutFieldModeCard',
      merchantOnly: true,
      notice: 'JPAY URL 인라인 결제창(jpay-pay.html) 입력 필드입니다. JPAY 필수: (1)카드·CVV (2)성명 (3)이메일 (4)국가코드(ISO2) (5)전화(국가코드 제외). (6)배송 주소는 선택. <strong>본사 기본 따름</strong>이면 본사설정 → 결제로직설정 값을 사용합니다.',
      rows: [
        [{ label: 'JPAY 결제창 입력 필드', type: 'select', name: 'jpayCheckoutFieldMode', options: [
          { v: 'FOLLOW_HQ', t: '본사 기본 따름' },
          { v: 'FULL', t: '1형 전체 (카드·성명·이메일·전화·배송)' },
          { v: 'CARD_ONLY', t: '2형 필수 4항목 (카드·성명·이메일·전화)' },
          { v: 'CARD_PREFILL', t: '3형 카드·성명 + 가맹 prefill' }
        ], col: 4 }],
        [{ label: '', type: 'note', col: 12, text: 'JPAY 필수: 국가코드(ISO2)·전화번호는 분리 입력(전화에 +82 등 붙이지 않음). 1·2형은 접속국가가 국가코드 드롭다운 기본값. 3형은 prepare buyerPrefill 의 countryIso2·phone(국가코드 제외). 2형: 주소 숨김. 3형: 카드·성명만 고객 입력.' }]
      ]
    };
  }

  /** read-only 챗봇결제 URL (컨테이너마다 고유 id가 필요하면 별도 템플릿으로 분리) */
  function merchantChatbotPaymentUrlRowHtml(placeholderKo) {
    var ph = placeholderKo || '가맹점 저장 후 조회';
    return '<div class="row mb-2"><div class="col-sm-5"><label class="form-label" data-pg-ui-t="챗봇결제 URL">' + escUi(L('챗봇결제 URL')) + '</label><div class="input-group input-group-sm"><input type="text" class="form-control" id="chatbotPaymentUrlDisplay" readonly placeholder="' + escUi(L(String(ph))) + '" data-pg-ui-placeholder="' + escUi(String(ph)) + '"><button type="button" class="btn btn-outline-primary" id="chatbotPaymentUrlCopyBtn" data-pg-ui-t="복사">' + escUi(L('복사')) + '</button></div></div></div>';
  }

  /** 가맹점 홈페이지·쇼핑몰: 플로팅 챗봇 삽입용 &lt;script&gt; 한 줄 (복사) */
  function merchantChatbotEmbedScriptRowHtml(placeholderKo) {
    var ph = placeholderKo || '가맹점 저장 후 조회';
    var hintKo = '모든 페이지에 공통으로 넣으려면 HTML 하단의 body 태그 직전(또는 쇼핑몰 공통 스크립트)에 아래 한 줄을 붙여 넣으세요.';
    return '<div class="row mb-2"><div class="col-12">' +
      '<label class="form-label" data-pg-ui-t="챗봇 플로팅 위젯(홈페이지·쇼핑몰 삽입)">' + escUi(L('챗봇 플로팅 위젯(홈페이지·쇼핑몰 삽입)')) + '</label>' +
      '<div class="form-text text-muted small mb-1" data-pg-ui-t="' + escUi(hintKo) + '">' + escUi(L(hintKo)) + '</div>' +
      '<div class="input-group input-group-sm">' +
      '<input type="text" class="form-control font-monospace" id="chatbotEmbedScriptDisplay" readonly placeholder="' + escUi(L(String(ph))) + '" data-pg-ui-placeholder="' + escUi(String(ph)) + '">' +
      '<button type="button" class="btn btn-outline-primary" id="chatbotEmbedScriptCopyBtn" data-pg-ui-t="복사">' + escUi(L('복사')) + '</button></div></div></div>';
  }

  /** 가맹점 배포용: 챗봇 결제 URL QR(스캔 시 동일 페이지) */
  function merchantChatbotQrRowHtml() {
    return '<div class="row mb-2" id="chatbotQrRow" style="display:none">' +
      '<div class="col-12">' +
      '<label class="form-label" data-pg-ui-t="챗봇 결제 QR">' + escUi(L('챗봇 결제 QR')) + '</label>' +
      '<div class="form-text text-muted small mb-1" data-pg-ui-t="카메라로 스캔하면 챗봇 결제 페이지로 이동합니다. 전단·POP·매장 안내에 사용할 수 있습니다.">' + escUi(L('카메라로 스캔하면 챗봇 결제 페이지로 이동합니다. 전단·POP·매장 안내에 사용할 수 있습니다.')) + '</div>' +
      '<div class="d-flex flex-wrap align-items-start gap-3">' +
      '<div class="d-flex align-items-center justify-content-center border rounded bg-light" style="width:188px;height:188px">' +
      '<img id="chatbotQrImg" alt="" width="176" height="176" class="d-none" decoding="async" />' +
      '<span id="chatbotQrPlaceholder" class="text-muted small">—</span></div>' +
      '<div class="d-flex flex-column gap-2 align-items-stretch">' +
      '<a class="btn btn-outline-primary btn-sm" id="chatbotQrOpenPng" href="#" target="_blank" rel="noopener" data-pg-ui-t="QR 열기">' + escUi(L('QR 열기')) + '</a>' +
      '<button type="button" class="btn btn-outline-secondary btn-sm" id="chatbotQrDownloadBtn" data-pg-ui-t="PNG 저장">' + escUi(L('PNG 저장')) + '</button></div></div></div></div>';
  }

  /** 상품관리 — 플랜·프로모션 안내(alert, i18n) */
  function chatbotProductMngNoticeHtml() {
    var k = '판매 활성 상품 수는 플랜 상한을 넘을 수 없습니다. 등록(보관) 행은 플랜 대비 최대 +2건까지 가능합니다.(예: 10건 플랜 → 활성 최대 10, 등록 최대 12) 본사 판매금지·챗봇결제 미사용이면 노출이 제한됩니다. 상위 조직은 가맹 코드 입력 후 불러오기 하세요. 챗봇-pay 상단 프로모션(끔·그리드·다이나믹·하이브리드)과 편집 중 상품의 후보 포함은 상단 「챗봇-pay 상단 프로모션」카드에서 함께 설정합니다.';
    return (
      '<div class="alert alert-info py-2 px-3 small mb-0" role="note">' +
      '<span data-pg-ui-t="' + escUi(k) + '">' + escUi(L(k)) + '</span></div>'
    );
  }

  /** 가맹점·상위 조직 — 챗봇 상품: 상단 등록 폼 + 하단 목록(수정·삭제) */
  function chatbotProductMngGridHtml() {
    return (
      '<div class="chatbot-product-mng">' +
      '<div class="row g-2 mb-3 align-items-end" id="chatbotProdScopeRow">' +
      '<div class="col-md-5">' +
      '<label class="form-label small mb-0" data-pg-ui-t="대상 가맹점 코드">' + escUi(L('대상 가맹점 코드')) + '</label>' +
      '<input type="text" class="form-control form-control-sm" id="chatbotProdScopeCompId" maxlength="50" placeholder="">' +
      '</div>' +
      '<div class="col-auto pb-1">' +
      '<button type="button" class="btn btn-sm btn-primary mt-3 mt-md-4" id="chatbotProdLoadBtn" data-pg-ui-t="불러오기">' + escUi(L('불러오기')) + '</button>' +
      '</div></div>' +
      '<p class="small text-muted mb-2 d-none" id="chatbotProdScopeHint" data-pg-ui-t="코드를 비우고 불러오기하면 로그인 조직 산하 가맹점의 등록 상품을 한 목록으로 봅니다. 본사·총판 열 「본사 판매금지」가 Y면 가맹이 사용=ON이어도 고객 챗봇·카탈로그에 노출되지 않습니다.">' + escUi(L('코드를 비우고 불러오기하면 로그인 조직 산하 가맹점의 등록 상품을 한 목록으로 봅니다. 본사·총판 열 「본사 판매금지」가 Y면 가맹이 사용=ON이어도 고객 챗봇·카탈로그에 노출되지 않습니다.')) + '</p>' +
      '<div class="card mb-3 border-secondary-subtle" id="chatbotProdPromoShelfCard">' +
      '<div class="card-header py-2 d-flex flex-wrap align-items-center gap-2">' +
      '<strong data-pg-ui-t="챗봇-pay 상단 프로모션">' + escUi(L('챗봇-pay 상단 프로모션')) + '</strong>' +
      '</div>' +
      '<div class="card-body">' +
      '<p class="small text-muted mb-2" data-pg-ui-t="챗봇-pay 상단 프로모션 안내">' + escUi(L('챗봇-pay 상단 프로모션 안내')) + '</p>' +
      '<div class="row g-2 align-items-end">' +
      '<div class="col-md-5 col-lg-4">' +
      '<label class="form-label small mb-0" data-pg-ui-t="표시 방식">' + escUi(L('표시 방식')) + '</label>' +
      '<select class="form-select form-select-sm" id="chatbotProdPromoShelfMode">' +
      '<option value="HIDDEN" data-pg-ui-t="' + escUi('끔 (상단 숨김)') + '">' + escUi(L('끔 (상단 숨김)')) + '</option>' +
      '<option value="PROMOTION" data-pg-ui-t="' + escUi('프로모션 (전체 그리드)') + '">' + escUi(L('프로모션 (전체 그리드)')) + '</option>' +
      '<option value="DYNAMIC" data-pg-ui-t="' + escUi('다이나믹 (3칸 순환)') + '">' + escUi(L('다이나믹 (3칸 순환)')) + '</option>' +
      '<option value="HYBRID" data-pg-ui-t="' + escUi('하이브리드 (좌1고정+2칸 순환)') + '">' + escUi(L('하이브리드 (좌1고정+2칸 순환)')) + '</option>' +
      '</select></div>' +
      '<div class="col-md-3 col-lg-2">' +
      '<label class="form-label small mb-0" data-pg-ui-t="순환 간격(초)">' + escUi(L('순환 간격(초)')) + '</label>' +
      '<input type="number" class="form-control form-control-sm" id="chatbotProdPromoRotateSeconds" min="30" max="86400" step="30" data-pg-ui-placeholder="' + escUi('30의 배수') + '">' +
      '</div>' +
      '<div class="col-md-auto">' +
      '<button type="button" class="btn btn-sm btn-primary mt-3 mt-md-4" id="chatbotProdPromoShelfSaveBtn" data-pg-ui-t="저장">' + escUi(L('저장')) + '</button>' +
      '</div></div>' +
      '<hr class="my-2">' +
      '<div class="row g-2 mb-0" id="chatbotProdPromoShelfCandidateRow">' +
      '<div class="col-md-8 col-lg-7">' +
      '<label class="form-label small mb-0" title="' + escUi(L('상단 후보 포함 도움말')) + '" data-pg-ui-t="편집 상품 · 상단 후보 포함">' + escUi(L('편집 상품 · 상단 후보 포함')) + '</label>' +
      '<select class="form-select form-select-sm" id="chatbotFormPromoShelf" style="max-width:14rem"></select>' +
      '<p class="small text-muted mb-0 mt-1" data-pg-ui-t="상단 후보 포함 안내">' + escUi(L('상단 후보 포함 안내')) + '</p>' +
      '</div></div></div></div>' +
      '<div class="card mb-3 border-primary-subtle" id="chatbotProdRegisterCard">' +
      '<div class="card-header py-2 d-flex flex-wrap align-items-center gap-2">' +
      '<strong data-pg-ui-t="상품 등록">' + escUi(L('상품 등록')) + '</strong>' +
      '<span class="small text-muted ms-md-auto" id="chatbotProdRegisterHint" data-pg-ui-t="신규 등록 중입니다.">' + escUi(L('신규 등록 중입니다.')) + '</span>' +
      '</div>' +
      '<div class="card-body">' +
      '<div class="row g-2 mb-2 hq-only-form">' +
      '<div class="col-md-4">' +
      '<label class="form-label small mb-0" data-pg-ui-t="가맹점코드">' + escUi(L('가맹점코드')) + '</label>' +
      '<input type="text" class="form-control form-control-sm" id="chatbotFormMerchantCode" readonly tabindex="-1">' +
      '</div>' +
      '<div class="col-md-4">' +
      '<label class="form-label small mb-0" data-pg-ui-t="가맹점명">' + escUi(L('가맹점명')) + '</label>' +
      '<input type="text" class="form-control form-control-sm" id="chatbotFormMerchantName" readonly tabindex="-1">' +
      '</div>' +
      '<div class="col-md-4">' +
      '<label class="form-label small mb-0" data-pg-ui-title="Y=고객 챗봇·공개 카탈로그 비노출(개발·검수 등)" title="' + escUi(L('Y=고객 챗봇·공개 카탈로그 비노출(개발·검수 등)')) + '" data-pg-ui-t="본사 판매금지">' + escUi(L('본사 판매금지')) + '</label>' +
      '<select class="form-select form-select-sm" id="chatbotFormHqBlock"></select>' +
      '</div></div>' +
      '<div class="row g-2 mb-2">' +
      '<div class="col-md-2">' +
      '<label class="form-label small mb-0" data-pg-ui-title="저장 시 시스템이 코드를 자동 부여합니다." title="' + escUi(L('저장 시 시스템이 코드를 자동 부여합니다.')) + '" data-pg-ui-t="코드">' + escUi(L('코드')) + '</label>' +
      '<input type="text" class="form-control form-control-sm" id="chatbotFormCode" readonly tabindex="-1">' +
      '</div>' +
      '<div class="col-md-2">' +
      '<label class="form-label small mb-0" title="' + escUi(L('항목 성격(사람 서비스 등)을 지정하면 챗봇 응대 톤이 보정됩니다.')) + '" data-pg-ui-t="항목구성">' + escUi(L('항목구성')) + '</label>' +
      '<select class="form-select form-select-sm" id="chatbotFormItemNature"></select>' +
      '</div>' +
      '<div class="col-md-3">' +
      '<label class="form-label small mb-0" data-pg-ui-t="상품명">' + escUi(L('상품명')) + '</label>' +
      '<input type="text" class="form-control form-control-sm" id="chatbotFormTitle" maxlength="200">' +
      '</div>' +
      '<div class="col-md-2">' +
      '<label class="form-label small mb-0" data-pg-ui-title="일반 판매 또는 예약 상품" title="' + escUi(L('일반 판매 또는 예약 상품')) + '" data-pg-ui-t="판매·예약">' + escUi(L('판매·예약')) + '</label>' +
      '<select class="form-select form-select-sm" id="chatbotFormListingType"></select>' +
      '</div>' +
      '<div class="col-md-2">' +
      '<label class="form-label small mb-0" title="' + escUi(L('예약 상품만. 비우면 기본설정(분)')) + '" data-pg-ui-t="예약슬롯(분)">' + escUi(L('예약슬롯(분)')) + '</label>' +
      '<input type="number" class="form-control form-control-sm" id="chatbotFormResSlot" min="15" max="1440" step="1" placeholder="' + escUi(L('기본')) + '">' +
      '</div>' +
      '<div class="col-md-2">' +
      '<label class="form-label small mb-0" data-pg-ui-title="판매=고객 챗봇·카탈로그 노출, 대기=등록만(본사 차단 등 별개)" title="' + escUi(L('판매=고객 챗봇·카탈로그 노출, 대기=등록만(본사 차단 등 별개)')) + '" data-pg-ui-t="판매상태">' + escUi(L('판매상태')) + '</label>' +
      '<select class="form-select form-select-sm" id="chatbotFormUse"></select>' +
      '</div>' +
      '<div class="col-md-2">' +
      '<label class="form-label small mb-0" data-pg-ui-t="순서">' + escUi(L('순서')) + '</label>' +
      '<input type="number" class="form-control form-control-sm" id="chatbotFormSort" step="1" min="1" value="1">' +
      '</div></div>' +
      '<div class="row g-2 mb-1 d-none" id="chatbotFormPlaceListingHintRow">' +
      '<div class="col-12"><p class="small text-muted mb-0" id="chatbotFormPlaceListingHint"></p></div></div>' +
      '<div class="row g-2 mb-2 d-none" id="chatbotFormResCollectRow">' +
      '<div class="col-md-3">' +
      '<label class="form-label small mb-0" title="' + escUi(L('시간·장소 예약 상품의 선결제 금액 방식입니다.')) + '" data-pg-ui-t="예약 결제">' + escUi(L('예약 결제')) + '</label>' +
      '<select class="form-select form-select-sm" id="chatbotFormResCollect">' +
      '<option value="FULL">' + escUi(L('전액')) + '</option>' +
      '<option value="DEPOSIT">' + escUi(L('예약금')) + '</option>' +
      '</select>' +
      '</div>' +
      '<div class="col-md-3">' +
      '<label class="form-label small mb-0" id="chatbotFormDepositLbl" title="' + escUi(L('예약금 모드일 때 결제에서 징수할 금액입니다.')) + '" data-pg-ui-t="예약금액">' + escUi(L('예약금액')) + '</label>' +
      '<input type="text" class="form-control form-control-sm" id="chatbotFormDepositAmt" maxlength="22" placeholder="">' +
      '</div>' +
      '<div class="col-md-12"><p class="small text-muted mb-0 mt-1" id="chatbotFormResCollectHint"></p></div>' +
      '</div>' +
      '<div class="row g-2 mb-2">' +
      '<div class="col-12">' +
      '<label class="form-label small mb-0" data-pg-ui-t="설명">' + escUi(L('설명')) + '</label>' +
      '<textarea class="form-control form-control-sm chatbot-field-desc" id="chatbotFormDesc" rows="2" maxlength="8000"></textarea>' +
      '</div></div>' +
      '<div class="row g-2 mb-2">' +
      '<div class="col-md-3">' +
      '<label class="form-label small mb-0" data-pg-ui-t="금액">' + escUi(L('금액')) + '</label>' +
      '<input type="text" class="form-control form-control-sm" id="chatbotFormAmt">' +
      '</div>' +
      '<div class="col-md-2">' +
      '<label class="form-label small mb-0" data-pg-ui-t="통화">' + escUi(L('통화')) + '</label>' +
      '<select class="form-select form-select-sm" id="chatbotFormCur"></select>' +
      '</div>' +
      '<div class="col-md-12">' +
      '<label class="form-label small mb-0" data-pg-ui-t="이미지(슬롯)">' + escUi(L('이미지(슬롯)')) + '</label>' +
      '<p class="small text-muted mb-1" id="chatbotFormImgGrantHint"></p>' +
      '<div class="row g-2" id="chatbotFormImgSlotsRow">' +
      [1, 2, 3, 4].map(function (sn) {
        var slotLblKey = '상품 이미지 #' + String(sn);
        return (
          '<div class="col-6 col-lg-3 chatbot-form-img-slot" data-chatbot-img-slot="' + sn + '">' +
          '<div class="small text-muted mb-0" data-pg-ui-t="' + escUi(slotLblKey) + '">' + escUi(L(slotLblKey)) + '</div>' +
          '<div class="input-group input-group-sm mb-1">' +
          '<input type="text" class="form-control form-control-sm chatbot-field-img" id="chatbotFormImg' + sn + '" readonly placeholder="">' +
          '<button type="button" class="btn btn-outline-primary chatbot-form-img-upload" data-chatbot-img-slot="' + sn + '" id="chatbotFormImgUpload' + sn + '" data-pg-ui-t="업로드">' + escUi(L('업로드')) + '</button>' +
          '</div>' +
          '<input type="file" class="d-none chatbot-form-img-file" id="chatbotFormImgFile' + sn + '" accept=".png,.jpg,.jpeg,image/png,image/jpeg">' +
          '</div>'
        );
      }).join('') +
      '</div></div></div>' +
      '<div class="d-flex flex-wrap gap-2 mt-2">' +
      '<button type="button" class="btn btn-sm btn-primary" id="chatbotProdFormSaveBtn" data-pg-ui-t="저장">' + escUi(L('저장')) + '</button>' +
      '<button type="button" class="btn btn-sm btn-outline-secondary d-none" id="chatbotProdFormCancelEditBtn" data-pg-ui-t="수정 취소">' + escUi(L('수정 취소')) + '</button>' +
      '<button type="button" class="btn btn-sm btn-outline-primary" id="chatbotProdNewRegisterBtn" data-pg-ui-t="신규등록">' + escUi(L('신규등록')) + '</button>' +
      '</div></div></div>' +
      '<h6 class="mb-1 fw-semibold" data-pg-ui-t="등록된 상품">' + escUi(L('등록된 상품')) + '</h6>' +
      '<p class="small text-muted mb-2" data-pg-ui-t="열 너비: 표 헤더 각 칸의 오른쪽 가장자리를 드래그하면 열 너비를 조절할 수 있습니다. 설정은 이 브라우저에 저장됩니다.">' +
      escUi(L('열 너비: 표 헤더 각 칸의 오른쪽 가장자리를 드래그하면 열 너비를 조절할 수 있습니다. 설정은 이 브라우저에 저장됩니다.')) + '</p>' +
      '<div class="table-responsive chatbot-prod-table-responsive">' +
      '<table class="table table-sm table-bordered align-middle mb-2 chatbot-prod-grid" id="grid_chatbot_products">' +
      '<thead class="table-light"><tr>' +
      '<th style="width:2.75rem;text-align:center">#</th>' +
      '<th class="hq-only-col" data-pg-ui-t="가맹점코드">' + escUi(L('가맹점코드')) + '</th>' +
      '<th class="hq-only-col" data-pg-ui-t="가맹점명">' + escUi(L('가맹점명')) + '</th>' +
      '<th class="hq-only-col" data-pg-ui-title="Y=고객 챗봇·공개 카탈로그 비노출(개발·검수 등)" title="' + escUi(L('Y=고객 챗봇·공개 카탈로그 비노출(개발·검수 등)')) + '" data-pg-ui-t="본사 판매금지">' + escUi(L('본사 판매금지')) + '</th>' +
      '<th class="chatbot-prod-col-code" data-pg-ui-t="코드">' + escUi(L('코드')) + '</th>' +
      '<th class="chatbot-prod-col-title" data-pg-ui-t="상품명">' + escUi(L('상품명')) + '</th>' +
      '<th class="chatbot-prod-col-listing" style="width:9.5rem;min-width:9.5rem" data-pg-ui-title="일반 판매 또는 예약 상품" title="' + escUi(L('일반 판매 또는 예약 상품')) + '" data-pg-ui-t="판매·예약">' + escUi(L('판매·예약')) + '</th>' +
      '<th class="chatbot-prod-col-rescollect" style="width:8.5rem;min-width:8.5rem" data-pg-ui-t="예약결제">' + escUi(L('예약결제')) + '</th>' +
      '<th class="chatbot-prod-col-desc" data-pg-ui-t="설명">' + escUi(L('설명')) + '</th>' +
      '<th class="chatbot-prod-col-amt" style="width:5.5rem;min-width:5rem" data-pg-ui-t="금액">' + escUi(L('금액')) + '</th>' +
      '<th class="chatbot-prod-col-ccy" style="width:4.5rem;min-width:3.5rem;max-width:5.5rem" data-pg-ui-t="통화">' + escUi(L('통화')) + '</th>' +
      '<th class="chatbot-prod-col-sort text-center" style="width:3.25rem;max-width:3.5rem;min-width:3rem" data-pg-ui-t="순서">' + escUi(L('순서')) + '</th>' +
      '<th style="width:5rem" data-pg-ui-title="판매=고객 챗봇·카탈로그 노출, 대기=등록만(본사 차단 등 별개)" title="' + escUi(L('판매=고객 챗봇·카탈로그 노출, 대기=등록만(본사 차단 등 별개)')) + '" data-pg-ui-t="판매상태">' + escUi(L('판매상태')) + '</th>' +
      '<th style="width:4.75rem" data-pg-ui-title="Y=상단 프로모션 후보(표시 방식이 끔이면 고객 화면에는 안 나옴)" title="' + escUi(L('Y=상단 프로모션 후보(표시 방식이 끔이면 고객 화면에는 안 나옴)')) + '" data-pg-ui-t="상단 후보">' + escUi(L('상단 후보')) + '</th>' +
      '<th class="chatbot-prod-col-img" data-pg-ui-t="이미지">' + escUi(L('이미지')) + '</th>' +
      '<th style="width:8rem;text-align:center" data-pg-ui-t="관리">' + escUi(L('관리')) + '</th>' +
      '</tr></thead>' +
      '<tbody id="chatbotProdTbody">' +
      '<tr><td colspan="16" class="text-muted text-center py-3 small empty-state-cell" id="chatbotProdTbodyPlaceholder" data-pg-ui-t="불러오기 후 목록이 표시됩니다. 상단 폼에서 신규등록하거나 목록에서 수정·삭제할 수 있습니다.">' +
      escUi(L('불러오기 후 목록이 표시됩니다. 상단 폼에서 신규등록하거나 목록에서 수정·삭제할 수 있습니다.')) +
      '</td></tr>' +
      '</tbody></table></div>' +
      '</div>'
    );
  }

  /** 총본사·본사·총판 등: 산하 가맹 챗봇 안내 현황(목록) */
  function chatbotKbMerchantOverviewHtml() {
    return (
      '<div id="chatbotKbOverviewOuter" class="chatbot-kb-overview-outer mb-2">' +
      '<div id="chatbotKbOverviewWrap" class="mb-2">' +
      '<div class="row g-2 align-items-end flex-wrap mb-2">' +
      '<div class="col-lg-3 col-md-6"><label class="form-label small mb-0" data-pg-ui-t="업체코드">' + escUi(L('업체코드')) + '</label>' +
      '<input type="text" class="form-control form-control-sm" id="chatbotKbOvSearchId" maxlength="50" placeholder=""></div>' +
      '<div class="col-lg-3 col-md-6"><label class="form-label small mb-0" data-pg-ui-t="업체명">' + escUi(L('업체명')) + '</label>' +
      '<input type="text" class="form-control form-control-sm" id="chatbotKbOvSearchNm" maxlength="120" placeholder=""></div>' +
      '<div class="col-auto"><button type="button" class="btn btn-sm btn-primary mt-3 mt-md-4" id="chatbotKbOvSearchBtn" data-pg-ui-t="조회">' + escUi(L('조회')) + '</button></div>' +
      '<div class="col-auto ms-lg-auto text-muted small align-self-center" id="chatbotKbOvPageInfo"></div>' +
      '<div class="col-auto">' +
      '<button type="button" class="btn btn-sm btn-outline-secondary" id="chatbotKbOvPrev" data-pg-ui-t="이전">' + escUi(L('이전')) + '</button> ' +
      '<button type="button" class="btn btn-sm btn-outline-secondary" id="chatbotKbOvNext" data-pg-ui-t="다음">' + escUi(L('다음')) + '</button>' +
      '</div></div>' +
      '<div class="chatbot-kb-overview-table-wrap">' +
      '<table class="table table-sm table-bordered align-middle mb-0 table-no-col-resize" id="tbl_chatbot_kb_overview">' +
      '<colgroup>' +
      '<col class="chatbot-kb-col-compid">' +
      '<col class="chatbot-kb-col-compnm">' +
      '<col class="chatbot-kb-col-opmode">' +
      '<col class="chatbot-kb-col-prodslot">' +
      '<col class="chatbot-kb-col-kbnm">' +
      '<col class="chatbot-kb-col-tel">' +
      '<col class="chatbot-kb-col-email">' +
      '<col class="chatbot-kb-col-contact">' +
      '<col class="chatbot-kb-col-addr">' +
      '<col class="chatbot-kb-col-intro">' +
      '<col class="chatbot-kb-col-product">' +
      '<col class="chatbot-kb-col-commerce">' +
      '<col class="chatbot-kb-col-action">' +
      '</colgroup>' +
      '<thead class="table-light"><tr>' +
      '<th data-pg-ui-t="업체코드">' + escUi(L('업체코드')) + '</th>' +
      '<th data-pg-ui-t="업체명">' + escUi(L('업체명')) + '</th>' +
      '<th data-pg-ui-t="운영방식">' + escUi(L('운영방식')) + '</th>' +
      '<th class="text-end" data-pg-ui-title="등록건수 / 등록상한 · 판매활성/플랜활성상한 (0 또는 미설정이면 무제한)" data-pg-ui-t="등록·활성">' + escUi(L('등록·활성')) + '</th>' +
      '<th data-pg-ui-t="안내 회사명">' + escUi(L('안내 회사명')) + '</th>' +
      '<th data-pg-ui-t="전화">' + escUi(L('전화')) + '</th>' +
      '<th data-pg-ui-t="이메일">' + escUi(L('이메일')) + '</th>' +
      '<th data-pg-ui-t="담당자">' + escUi(L('담당자')) + '</th>' +
      '<th data-pg-ui-t="주소">' + escUi(L('주소')) + '</th>' +
      '<th data-pg-ui-t="회사소개">' + escUi(L('회사소개')) + '</th>' +
      '<th data-pg-ui-t="판매안내">' + escUi(L('판매안내')) + '</th>' +
      '<th class="text-center chatbot-kb-overview-th-commerce" data-pg-ui-title="채팅은 가능하나 고객용 상품·예약·결제만 일시 중지" data-pg-ui-t="상업 기능">' +
      escUi(L('상업 기능')) + '</th>' +
      '<th class="text-center" data-pg-ui-t="수정">' + escUi(L('수정')) + '</th>' +
      '</tr></thead><tbody id="chatbotKbOverviewTbody"></tbody></table></div></div></div>'
    );
  }

  /** 챗봇관리 — 주문·예약 내역(결제 연동) */
  function chatbotOrderMngScreenHtml() {
    return (
      '<div class="border-bottom pb-2 mb-3" id="chatbotOrderScopeOuter">' +
      '<div class="row g-2 align-items-end flex-wrap" id="chatbotOrderScopeRow">' +
      '<div class="col-md-5">' +
      '<label class="form-label small mb-0" data-pg-ui-t="대상 가맹점 코드">' + escUi(L('대상 가맹점 코드')) + '</label>' +
      '<input type="text" class="form-control form-control-sm" id="chatbotOrderScopeCompId" maxlength="50" placeholder="">' +
      '</div>' +
      '<div class="col-auto pb-1">' +
      '<button type="button" class="btn btn-sm btn-primary mt-3 mt-md-4" id="chatbotOrderLoadBtn" data-pg-ui-t="불러오기">' + escUi(L('불러오기')) + '</button>' +
      '</div></div>' +
      '<p class="small text-muted mb-0 mt-1" data-pg-ui-t="고객이 주문서를 제출하고 결제를 완료하면 접수(확정)됩니다. 예약 시간은 기본설정·상품별 슬롯으로 검증됩니다.">' +
      escUi(L('고객이 주문서를 제출하고 결제를 완료하면 접수(확정)됩니다. 예약 시간은 기본설정·상품별 슬롯으로 검증됩니다.')) +
      '</p></div>' +
      '<div class="table-responsive">' +
      '<table class="table table-sm table-bordered align-middle" id="grid_chatbot_orders">' +
      '<thead class="table-light"><tr>' +
      '<th style="min-width:5.5rem" data-pg-ui-t="상태">' + escUi(L('상태')) + '</th>' +
      '<th style="min-width:10rem" data-pg-ui-t="접수일시">' + escUi(L('접수일시')) + '</th>' +
      '<th style="min-width:8rem" data-pg-ui-t="상품">' + escUi(L('상품')) + '</th>' +
      '<th style="min-width:6rem" data-pg-ui-t="금액">' + escUi(L('금액')) + '</th>' +
      '<th style="min-width:5rem" data-pg-ui-t="주문자">' + escUi(L('주문자')) + '</th>' +
      '<th style="min-width:8rem" data-pg-ui-t="이메일">' + escUi(L('이메일')) + '</th>' +
      '<th style="min-width:6rem" data-pg-ui-t="전화">' + escUi(L('전화')) + '</th>' +
      '<th style="min-width:10rem" data-pg-ui-t="주소">' + escUi(L('주소')) + '</th>' +
      '<th style="min-width:9rem" data-pg-ui-t="예약">' + escUi(L('예약')) + '</th>' +
      '<th style="min-width:7rem" data-pg-ui-t="주문번호">' + escUi(L('주문번호')) + '</th>' +
      '<th style="min-width:7rem" data-pg-ui-t="거래번호">' + escUi(L('거래번호')) + '</th>' +
      '</tr></thead>' +
      '<tbody id="chatbotOrderTbody"><tr><td colspan="11" class="text-muted text-center py-3 small" data-pg-ui-t="불러오기를 눌러 주세요.">' +
      escUi(L('불러오기를 눌러 주세요.')) +
      '</td></tr></tbody></table></div>'
    );
  }

  /** 챗봇 — 플랜구매 블록: 상위 조직용 코드 입력 + 플랜·미수금 */
  function chatbotKbScopeAndPlanFormHtml() {
    return (
      '<div id="chatbotKbScopeOuter" class="mb-2">' +
      '<div class="row g-2 align-items-end flex-wrap" id="chatbotKbScopeRow">' +
      '<div class="col-md-5">' +
      '<label class="form-label small mb-0" data-pg-ui-t="대상 가맹점 코드">' + escUi(L('대상 가맹점 코드')) + '</label>' +
      '<input type="text" class="form-control form-control-sm" id="chatbotKbScopeCompId" maxlength="50" placeholder="">' +
      '</div>' +
      '<div class="col-auto pb-1">' +
      '<button type="button" class="btn btn-sm btn-primary mt-3 mt-md-4" id="chatbotKbFormLoadBtn" data-pg-ui-t="불러오기">' + escUi(L('불러오기')) + '</button>' +
      '</div></div>' +
      '<p class="small text-muted mb-0 mt-1" data-pg-ui-t="목록에서 [수정]을 누르면 코드가 채워지고 아래 플랜·미수금을 고칠 수 있습니다.">' +
      escUi(L('목록에서 [수정]을 누르면 코드가 채워지고 아래 플랜·미수금을 고칠 수 있습니다.')) +
      '</p></div>' +
      chatbotKbPlanPurchasePanelHtml()
    );
  }

  /** 챗봇 — 고객 안내·운영방식만(별도 섹션) */
  function chatbotKbMerchantGuidanceFormHtml() {
    return chatbotKbMerchantGuidancePanelHtml();
  }

  /** @deprecated 이름 호환 — 플랜+안내 한 블록이던 시절 */
  function chatbotKbScopeAndPublicFormHtml() {
    return chatbotKbScopeAndPlanFormHtml() + chatbotKbMerchantGuidancePanelHtml();
  }

  /** 플랜·과금·미수금(본사 AI챗봇설정 요금과 동일 통화) */
  function chatbotKbPlanPurchasePanelHtml() {
    return (
      '<div class="chatbot-kb-plan-shell border border-primary border-opacity-25 rounded-3 shadow-sm mb-3" id="chatbotKbProductPlanBanner">' +
      '<div class="chatbot-kb-plan-header px-3 py-2 small fw-semibold text-white" data-pg-ui-t="플랜·과금">' +
      escUi(L('플랜·과금')) + '</div>' +
      '<div class="chatbot-kb-plan-body p-3">' +
      '<div id="chatbotKbCommerceHoldBanner" class="alert alert-warning py-2 px-3 small mb-2 d-none" role="status"></div>' +
      '<div class="row g-2 mb-3">' +
      '<div class="col-md-4"><div class="rounded-3 p-3 h-100 chatbot-kb-plan-pill">' +
      '<div class="small text-muted mb-1" data-pg-ui-t="현재 플랜(건)">' + escUi(L('현재 플랜(건)')) + '</div>' +
      '<div id="chatbotKbPlanCurrentPill" class="fs-6 fw-bold text-primary">—</div>' +
      '<div class="small mt-2 text-muted" id="chatbotKbPlanNextPillLine" role="status">—</div>' +
      '<div class="small mt-2 text-muted" id="chatbotKbPlanMonthlyFeeLine">—</div></div></div>' +
      '<div class="col-md-4"><div class="rounded-3 p-3 h-100 chatbot-kb-plan-pill">' +
      '<div class="small text-muted mb-1" data-pg-ui-t="청구 통화">' + escUi(L('청구 통화')) + '</div>' +
      '<div id="chatbotKbPlanBillingCcyVal" class="fs-6 fw-semibold text-body">—</div>' +
      '<div class="small mt-2 text-muted" data-pg-ui-t="총판·가맹 기준통화">' + escUi(L('총판·가맹 기준통화')) + '</div></div></div>' +
      '<div class="col-md-4"><div class="rounded-3 p-3 h-100 chatbot-kb-plan-pill">' +
      '<div class="small text-muted mb-1" data-pg-ui-t="과금 기간(서울)">' + escUi(L('과금 기간(서울)')) + '</div>' +
      '<div id="chatbotKbPlanCycleHint" class="small fw-medium text-body">—</div>' +
      '<div class="small mt-2 text-muted" data-pg-ui-t="동일 달력월 말까지">' + escUi(L('동일 달력월 말까지')) + '</div></div></div></div>' +
      '<div class="mb-3"><div class="small text-muted mb-2" data-pg-ui-t="본사 AI챗봇설정과 동일 슬롯별 월 요금">' +
      escUi(L('본사 AI챗봇설정과 동일 슬롯별 월 요금')) + '</div>' +
      '<div id="chatbotKbPlanFeeCards" class="row g-2"></div></div>' +
      '<div class="d-flex flex-wrap align-items-center gap-2 mb-2">' +
      '<h6 class="small fw-semibold mb-0 text-body" data-pg-ui-t="챗봇 등록·이용 상품">' +
      escUi(L('챗봇 등록·이용 상품')) + '</h6>' +
      '<button type="button" class="btn btn-sm btn-outline-primary ms-auto" id="chatbotKbGoProductMngBtn" data-pg-ui-t="상품관리에서 등록·수정">' +
      escUi(L('상품관리에서 등록·수정')) + '</button></div>' +
      '<p class="small text-muted mb-2" data-pg-ui-t="챗봇 결제 및 공개 챗봇 노출 상품은 「상품관리」에서 설정합니다. 플랜마다 동시 「판매 활성」 가능 개수가 있으며, 「사용=Y」만 고객에게 판매·노출됩니다. 그보다 2건 더 많게 상품 행은 등록해 두되 판매 비활성(사용=N)으로 둘 수 있습니다. 안내(LLM·카탈로그)에는 판매 활성이면서 본사 판매금지 아닌 상품만 포함됩니다.">' +
      escUi(L('챗봇 결제 및 공개 챗봇 노출 상품은 「상품관리」에서 설정합니다. 플랜마다 동시 「판매 활성」 가능 개수가 있으며, 「사용=Y」만 고객에게 판매·노출됩니다. 그보다 2건 더 많게 상품 행은 등록해 두되 판매 비활성(사용=N)으로 둘 수 있습니다. 안내(LLM·카탈로그)에는 판매 활성이면서 본사 판매금지 아닌 상품만 포함됩니다.')) +
      '</p>' +
      '<dl class="row small mb-0">' +
      '<dt class="col-sm-4 col-md-3 text-muted mb-1" data-pg-ui-t="챗봇결제">' + escUi(L('챗봇결제')) + '</dt>' +
      '<dd class="col-sm-8 col-md-3 mb-1" id="chatbotKbPlanPaymentYnVal">—</dd>' +
      '<dt class="col-sm-4 col-md-3 text-muted mb-1" data-pg-ui-t="판매 활성·등록 한도">' + escUi(L('판매 활성·등록 한도')) + '</dt>' +
      '<dd class="col-sm-8 col-md-3 mb-1" id="chatbotKbPlanCapVal">—</dd>' +
      '<dt class="col-sm-4 col-md-3 text-muted mb-1" data-pg-ui-t="판매 활성(현재/플랜)">' + escUi(L('판매 활성(현재/플랜)')) + '</dt>' +
      '<dd class="col-sm-8 col-md-3 mb-1" id="chatbotKbPlanSaleActiveVal">—</dd>' +
      '<dt class="col-sm-4 col-md-3 text-muted mb-1" data-pg-ui-t="등록 건수(전체)">' + escUi(L('등록 건수(전체)')) + '</dt>' +
      '<dd class="col-sm-8 col-md-3 mb-1" id="chatbotKbPlanRegVal">—</dd>' +
      '<dt class="col-sm-4 col-md-3 text-muted mb-1" data-pg-ui-t="남은 등록 슬롯">' + escUi(L('남은 등록 슬롯')) + '</dt>' +
      '<dd class="col-sm-8 col-md-3 mb-1" id="chatbotKbPlanRemVal">—</dd>' +
      '</dl>' +
      '<div class="border-top pt-2 mt-3" id="chatbotKbPlanSlotEditRow">' +
      '<div class="small fw-semibold text-body mb-2" data-pg-ui-t="플랜 변경">' + escUi(L('플랜 변경')) + '</div>' +
      '<div class="mb-2">' +
      '<label class="form-label small mb-1" for="chatbotKbPlanSlotImmediateSel" data-pg-ui-t="즉시 상향(당월 반영)">' +
      escUi(L('즉시 상향(당월 반영)')) + '</label>' +
      '<div class="d-flex flex-wrap align-items-center gap-2">' +
      '<select id="chatbotKbPlanSlotImmediateSel" class="form-select form-select-sm" style="max-width: 16rem">' +
      '<option value="" data-pg-ui-t="선택 안 함">' + escUi(L('선택 안 함')) + '</option>' +
      '</select>' +
      '<span class="small text-muted flex-grow-1 min-w-0" id="chatbotKbPlanSlotImmediateHint"></span></div></div>' +
      '<div class="mb-2">' +
      '<label class="form-label small mb-1" for="chatbotKbPlanSlotNextSel" data-pg-ui-t="다음 플랜(예약·익월 적용)">' +
      escUi(L('다음 플랜(예약·익월 적용)')) + '</label>' +
      '<div class="d-flex flex-wrap align-items-center gap-2">' +
      '<select id="chatbotKbPlanSlotNextSel" class="form-select form-select-sm" style="max-width: 16rem">' +
      '<option value="" data-pg-ui-t="예약 없음(익월에도 현재 플랜 유지)">' + escUi(L('예약 없음(익월에도 현재 플랜 유지)')) + '</option>' +
      [10, 20, 50, 80, 100, 150, 200].map(function (slotN) {
        var kCnt = String(slotN) + '건';
        return '<option value="' + slotN + '" data-pg-ui-t="' + escUi(kCnt) + '">' + escUi(L(kCnt)) + '</option>';
      }).join('') +
      '</select>' +
      '<span class="small text-muted flex-grow-1 min-w-0" id="chatbotKbPlanSlotNextHint"></span></div></div>' +
      '</div>' +
      '<p class="small text-warning mb-0 d-none mt-2" id="chatbotKbPlanWarn"></p>' +
      '<p class="small alert alert-info py-2 px-3 mb-0 mt-2 d-none" id="chatbotKbPlanPendingHint" role="status"></p>' +
      '<div class="border-top pt-3 mt-3" id="chatbotKbBillingOuter">' +
      '<div class="d-flex flex-wrap align-items-center gap-2 mb-2">' +
      '<h6 class="small fw-semibold mb-0 text-body" data-pg-ui-t="미수금 내역(플랜)">' + escUi(L('미수금 내역(플랜)')) + '</h6>' +
      '<button type="button" class="btn btn-sm btn-outline-secondary ms-auto" id="chatbotKbBillingReloadBtn" data-pg-ui-t="새로고침">' +
      escUi(L('새로고침')) + '</button>' +
      '</div>' +
      '<p class="small text-muted mb-2" data-pg-ui-t="월 정기·업그레이드 차액은 미수금으로 등록되며 정산에서 환수됩니다.">' +
      escUi(L('월 정기·업그레이드 차액은 미수금으로 등록되며 정산에서 환수됩니다.')) +
      '</p>' +
      '<div class="table-responsive">' +
      '<table class="table table-sm table-bordered align-middle mb-2">' +
      '<thead><tr>' +
      '<th style="width: 110px" data-pg-ui-t="구분">' + escUi(L('구분')) + '</th>' +
      '<th style="width: 120px" data-pg-ui-t="청구월">' + escUi(L('청구월')) + '</th>' +
      '<th style="width: 200px" data-pg-ui-t="금액">' + escUi(L('금액')) + '</th>' +
      '<th style="width: 100px" data-pg-ui-t="상태">' + escUi(L('상태')) + '</th>' +
      '<th style="width: 180px" data-pg-ui-t="등록일">' + escUi(L('등록일')) + '</th>' +
      '</tr></thead>' +
      '<tbody id="chatbotKbBillingTbody"></tbody>' +
      '</table>' +
      '</div>' +
      '<div class="d-flex align-items-center gap-2">' +
      '<button type="button" class="btn btn-sm btn-outline-secondary" id="chatbotKbBillingPrevBtn" data-pg-ui-t="이전">' + escUi(L('이전')) + '</button>' +
      '<button type="button" class="btn btn-sm btn-outline-secondary" id="chatbotKbBillingNextBtn" data-pg-ui-t="다음">' + escUi(L('다음')) + '</button>' +
      '<span class="small text-muted ms-auto" id="chatbotKbBillingPageInfo">—</span>' +
      '</div>' +
      '<p class="small text-muted mb-0 mt-2" id="chatbotKbBillingLatestHint"></p>' +
      '</div></div></div>'
    );
  }

  function chatbotKbMerchantGuidancePanelHtml() {
    return (
      '<div class="chatbot-kb-guidance-shell border rounded-3 p-3 mb-3 bg-body-secondary bg-opacity-50" id="chatbotKbSection">' +
      '<p class="small text-muted mb-2" data-pg-ui-t="고객 챗봇 문의 시 참고되는 안내입니다. 아래 비우면 1~5는 업체등록 정보와 동일하게 안내됩니다.">' +
      escUi(L('고객 챗봇 문의 시 참고되는 안내입니다. 아래 비우면 1~5는 업체등록 정보와 동일하게 안내됩니다.')) +
      '</p>' +
      '<div class="row g-2 mb-2">' +
      '<div class="col-12">' +
      '<label class="form-label small mb-0" for="chatbotOperationMode" data-pg-ui-t="챗봇 운영방식">' + escUi(L('챗봇 운영방식')) + '</label>' +
      '<select class="form-select form-select-sm" id="chatbotOperationMode" name="chatbotOperationMode" style="max-width: 42rem">' +
      '<option value="SALE_PREPAID" data-pg-ui-t="' + escUi('상품판매 · 선불') + '">' + escUi(L('상품판매 · 선불')) + '</option>' +
      '<option value="SALE_POSTPAID" data-pg-ui-t="' + escUi('상품판매 · 후불') + '">' + escUi(L('상품판매 · 후불')) + '</option>' +
      '<option value="RESERVATION_PREPAID" data-pg-ui-t="' + escUi('예약방식 · 선불') + '">' + escUi(L('예약방식 · 선불')) + '</option>' +
      '<option value="RESERVATION_POSTPAID" data-pg-ui-t="' + escUi('예약방식 · 후불') + '">' + escUi(L('예약방식 · 후불')) + '</option>' +
      '<option value="HYBRID_RESERVATION_PREPAID" data-pg-ui-t="' + escUi('하이브리드 (판매+예약, 예약은 선불 고정)') + '">' + escUi(L('하이브리드 (판매+예약, 예약은 선불 고정)')) + '</option>' +
      '<option value="FACE_TO_FACE_POSTPAID" data-pg-ui-t="' + escUi('대면거래 (판매+예약 · 후불)') + '">' + escUi(L('대면거래 (판매+예약 · 후불)')) + '</option>' +
      '</select>' +
      '<p class="small text-muted mb-0 mt-1" data-pg-ui-t="선택한 운영방식에 맞춰 공개 챗봇 응대(선불·후불·예약 안내)가 적용됩니다.">' +
      escUi(L('선택한 운영방식에 맞춰 공개 챗봇 응대(선불·후불·예약 안내)가 적용됩니다.')) + '</p>' +
      '</div></div>' +
      '<div class="row g-2 mb-2">' +
      '<div class="col-12">' +
      '<label class="form-label small mb-0" for="chatbotMerchantVertical" data-pg-ui-t="가맹점 업체성격">' +
      escUi(L('가맹점 업체성격')) + '</label>' +
      '<select class="form-select form-select-sm" id="chatbotMerchantVertical" name="chatbotMerchantVertical" style="max-width: 42rem">' +
      [
        ['GENERAL_SALE', '일반판매'],
        ['ECOMMERCE', '이커머스'],
        ['CONSULTING', '컨설팅'],
        ['REAL_ESTATE', '부동산'],
        ['AUTO_SALES', '자동차판매'],
        ['SERVICE_TRADE', '서비스업'],
        ['MASSAGE_GENERAL', '일반마사지'],
        ['COSMETIC', '코스메틱'],
        ['CLUB_ENTERTAINMENT', '클럽(유흥)'],
        ['CLUB_MASSAGE', '클럽(마사지)'],
        ['RESTAURANT', '음식점'],
        ['VIP_CLUB', 'VIP 클럽'],
        ['OTHER', '기타']
      ].map(function (pair) {
        return '<option value="' + escUi(pair[0]) + '" data-pg-ui-t="' + escUi(pair[1]) + '">' + escUi(L(pair[1])) + '</option>';
      }).join('') +
      '</select>' +
      '<p class="small text-muted mb-1 mt-1" data-pg-ui-t="운영방식(선불·후불·예약)과 별개로, 업종에 맞는 주문·예약 질문 흐름을 잡는 분류입니다. 공개 챗봇 AI가 카탈로그·운영방식과 모순 없이 필요한 항목만 묻도록 서버에서 안내 블록으로 전달됩니다.">' +
      escUi(L('운영방식(선불·후불·예약)과 별개로, 업종에 맞는 주문·예약 질문 흐름을 잡는 분류입니다. 공개 챗봇 AI가 카탈로그·운영방식과 모순 없이 필요한 항목만 묻도록 서버에서 안내 블록으로 전달됩니다.')) + '</p>' +
      '<label class="form-label small mb-0" for="chatbotMerchantVerticalNotes" data-pg-ui-t="업체성격 보조 메모(선택)">' +
      escUi(L('업체성격 보조 메모(선택)')) + '</label>' +
      '<textarea class="form-control form-control-sm" id="chatbotMerchantVerticalNotes" name="chatbotMerchantVerticalNotes" rows="3" maxlength="2000" placeholder=""></textarea>' +
      '<p class="small text-muted mb-0 mt-1" data-pg-ui-t="특화 업종에서 반드시 받을 정보·피할 표현 등을 적으면 AI 수집 안내에 반영됩니다. 비우면 업체성격 기본 지침만 사용합니다.">' +
      escUi(L('특화 업종에서 반드시 받을 정보·피할 표현 등을 적으면 AI 수집 안내에 반영됩니다. 비우면 업체성격 기본 지침만 사용합니다.')) + '</p>' +
      '<label class="form-label small mb-0 mt-2" for="chatbotOrderSheetUiJson" data-pg-ui-t="챗봇 주문·예약 시트 UI(JSON, 선택)">' +
      escUi(L('챗봇 주문·예약 시트 UI(JSON, 선택)')) + '</label>' +
      '<textarea class="form-control form-control-sm font-monospace" id="chatbotOrderSheetUiJson" name="chatbotOrderSheetUiJson" rows="7" maxlength="12000" spellcheck="false" placeholder="{&quot;fields&quot;:{&quot;orderMemo&quot;:{&quot;hidden&quot;:true}}}"></textarea>' +
      '<p class="small text-muted mb-0 mt-1" data-pg-ui-t="고객 챗봇 「주문·결제」시트 필드 표시·라벨을 가맹별로 덮어씁니다. 최상위 fields 아래 키: ordererName, ordererEmail, ordererPhone, ordererAddr, orderMemo, reservationLocal, reservationCheckout, guestCount, serviceMinutes. 속성 예: hidden(true/false), labelKo, placeholderKo, showWhenReservation(이용시간 분, serviceMinutes만). 주소를 숨기면 prefillWhenHidden 을 4자 이상 필수. 이메일·전화는 숨길 수 없습니다. 비우면 업체성격 기본만 적용됩니다.">' +
      escUi(L('고객 챗봇 「주문·결제」시트 필드 표시·라벨을 가맹별로 덮어씁니다. 최상위 fields 아래 키: ordererName, ordererEmail, ordererPhone, ordererAddr, orderMemo, reservationLocal, reservationCheckout, guestCount, serviceMinutes. 속성 예: hidden(true/false), labelKo, placeholderKo, showWhenReservation(이용시간 분, serviceMinutes만). 주소를 숨기면 prefillWhenHidden 을 4자 이상 필수. 이메일·전화는 숨길 수 없습니다. 비우면 업체성격 기본만 적용됩니다.')) +
      '</p>' +
      '</div></div>' +
      '<div class="row g-2 mb-2 border-top pt-2 mt-1">' +
      '<div class="col-md-4">' +
      '<label class="form-label small mb-0" for="chatbotReservationSlotMinutes" data-pg-ui-t="예약 기본 슬롯(분)">' + escUi(L('예약 기본 슬롯(분)')) + '</label>' +
      '<select class="form-select form-select-sm" id="chatbotReservationSlotMinutes" name="chatbotReservationSlotMinutes" style="max-width: 16rem">' +
      [15, 30, 45, 60, 90, 120, 180, 240].map(function (n) {
        var kMin = String(n) + '분';
        return '<option value="' + n + '" data-pg-ui-t="' + escUi(kMin) + '">' + escUi(L(kMin)) + '</option>';
      }).join('') +
      '</select>' +
      '<p class="small text-muted mb-0 mt-1" data-pg-ui-t="예약 상품은 동일 시간대가 겹치지 않게 막습니다. 상품마다 다른 슬롯(분)은 「상품관리」에서 덮어쓸 수 있습니다.">' +
      escUi(L('예약 상품은 동일 시간대가 겹치지 않게 막습니다. 상품마다 다른 슬롯(분)은 「상품관리」에서 덮어쓸 수 있습니다.')) + '</p>' +
      '</div>' +
      '<div class="col-md-8">' +
      '<label class="form-label small mb-0" for="chatbotReservationZoneId" data-pg-ui-t="예약 타임존(IANA)">' + escUi(L('예약 타임존(IANA)')) + '</label>' +
      '<input type="text" class="form-control form-control-sm" id="chatbotReservationZoneId" name="chatbotReservationZoneId" maxlength="64" placeholder="Asia/Seoul">' +
      '</div></div>' +
      '<div class="row g-2 mb-2 border-top pt-2 mt-1"' +
      ' id="chatbotKbCatalogPolicyOuter">' +
      '<div class="col-12"><p class="small text-muted mb-1"' +
      ' id="chatbotKbCatalogEffectiveHint"></p></div>' +
      '<div class="col-12"><label class="form-label small mb-1"' +
      ' data-pg-ui-t="가맹에서 사용할 카탈로그 유형">' +
      escUi(L('가맹에서 사용할 카탈로그 유형')) + '</label>' +
      '<div id="chatbotKbCatalogLtChecks"' +
      ' class="d-flex flex-wrap gap-3"></div>' +
      '<p class="small text-warning mb-0 mt-2 d-none"' +
      ' id="chatbotKbCatalogLtWarn">' +
      escUi(L('허용된 유형 안에서 하나 이상 선택하세요.')) + '</p></div></div>' +
      '<div class="row g-2">' +
      '<div class="col-md-6"><label class="form-label small mb-0" data-pg-ui-t="회사이름">' + escUi(L('회사이름')) + '</label>' +
      '<input type="text" class="form-control form-control-sm" id="chatbotKbCompanyNm" name="chatbotKbCompanyNm" maxlength="200"></div>' +
      '<div class="col-md-6"><label class="form-label small mb-0" data-pg-ui-t="전화번호">' + escUi(L('전화번호')) + '</label>' +
      '<input type="text" class="form-control form-control-sm" id="chatbotKbTel" name="chatbotKbTel" maxlength="100"></div>' +
      '<div class="col-md-12"><label class="form-label small mb-0" data-pg-ui-t="회사주소">' + escUi(L('회사주소')) + '</label>' +
      '<textarea class="form-control form-control-sm" id="chatbotKbAddr" name="chatbotKbAddr" rows="2" maxlength="600"></textarea></div>' +
      '<div class="col-md-6"><label class="form-label small mb-0" data-pg-ui-t="이메일">' + escUi(L('이메일')) + '</label>' +
      '<input type="text" class="form-control form-control-sm" id="chatbotKbEmail" name="chatbotKbEmail" maxlength="120"></div>' +
      '<div class="col-md-6"><label class="form-label small mb-0" data-pg-ui-t="담당자 성명">' + escUi(L('담당자 성명')) + '</label>' +
      '<input type="text" class="form-control form-control-sm" id="chatbotKbContactNm" name="chatbotKbContactNm" maxlength="100"></div>' +
      '<div class="col-md-12"><label class="form-label small mb-0" for="chatbotKbWelcomeHint" data-pg-ui-t="기본 안내 (첫 화면 상단)">' +
      escUi(L('기본 안내 (첫 화면 상단)')) + '</label>' +
      '<div class="d-flex flex-wrap gap-2 mb-1">' +
      '<button type="button" class="btn btn-sm btn-outline-secondary" id="chatbotKbAiWelcomeBtn" data-pg-ui-t="AI로 기본 안내 초안">' +
      escUi(L('AI로 기본 안내 초안')) + '</button>' +
      '</div>' +
      '<textarea class="form-control form-control-sm" id="chatbotKbWelcomeHint" name="chatbotKbWelcomeHint" rows="2" maxlength="600" placeholder=""></textarea>' +
      '<p class="small text-muted mb-0 mt-1" data-pg-ui-t="비우면 시스템 기본 문구가 챗봇 첫 상단에 표시됩니다.">' +
      escUi(L('비우면 시스템 기본 문구가 챗봇 첫 상단에 표시됩니다.')) + '</p></div>' +
      '<div class="col-md-12"><label class="form-label small mb-0" data-pg-ui-t="회사소개">' + escUi(L('회사소개')) + '</label>' +
      '<div class="d-flex flex-wrap gap-2 mb-1">' +
      '<button type="button" class="btn btn-sm btn-outline-secondary" id="chatbotKbAiIntroBtn" data-pg-ui-t="AI로 회사소개 초안">' + escUi(L('AI로 회사소개 초안')) + '</button>' +
      '</div>' +
      '<textarea class="form-control form-control-sm" id="chatbotKbIntro" name="chatbotKbIntro" rows="4" maxlength="4000"></textarea></div>' +
      '<div class="col-md-12"><label class="form-label small mb-0" data-pg-ui-t="판매상품 안내(개요)">' + escUi(L('판매상품 안내(개요)')) + '</label>' +
      '<div class="d-flex flex-wrap gap-2 mb-1">' +
      '<button type="button" class="btn btn-sm btn-outline-secondary" id="chatbotKbAiProductBtn" data-pg-ui-t="AI로 판매상품 안내 초안">' + escUi(L('AI로 판매상품 안내 초안')) + '</button>' +
      '</div>' +
      '<textarea class="form-control form-control-sm" id="chatbotKbProductDesc" name="chatbotKbProductDesc" rows="4" maxlength="4000"></textarea></div>' +
      '<div class="col-12 d-flex flex-wrap gap-2 mt-2">' +
      '<button type="button" class="btn btn-sm btn-primary" id="chatbotKbSaveBtn" data-pg-ui-t="안내 저장">' + escUi(L('안내 저장')) + '</button>' +
      '</div></div></div>'
    );
  }

  /** 본사 AI챗봇설정 — ziobiz/Stock php-web/pages/ai.php 리포트 API 키·모델·순위와 동일 필드명 */
  function hqChatbotAiSettingsFormHtml() {
    var geminiModelLabels = {
      'gemini-3-flash-preview': 'Gemini 3 Flash',
      'gemini-3.1-pro-preview': 'Gemini 3.1 Pro',
      'gemini-3.1-flash-lite': 'Gemini 3.1 Flash-Lite',
      'gemini-2.5-pro': 'Gemini 2.5 Pro',
      'gemini-2.5-flash': 'Gemini 2.5 Flash',
      'gemini-2.5-flash-lite': 'Gemini 2.5 Flash-Lite',
      'gemini-2.0-flash': 'Gemini 2.0 Flash',
      'gemini-1.5-flash': 'Gemini 1.5 Flash',
      'gemini-1.5-pro': 'Gemini 1.5 Pro'
    };
    function geminiModelOptionLabel(id) {
      return geminiModelLabels[id] ? geminiModelLabels[id] + ' (' + id + ')' : id;
    }
    function modelSelectHtml(prov, presets) {
      var opts = '<option value="" data-pg-ui-t="자동(기본)">' + escUi(L('자동(기본)')) + '</option>';
      for (var i = 0; i < presets.length; i++) {
        var m = presets[i];
        var lab = (prov === 'gemini') ? geminiModelOptionLabel(m) : m;
        opts += '<option value="' + escUi(m) + '">' + escUi(lab) + '</option>';
      }
      opts += '<option value="custom" data-pg-ui-t="기타(직접입력)">' + escUi(L('기타(직접입력)')) + '</option>';
      return (
        '<div class="mb-2">' +
        '<label class="form-label small mb-1" data-pg-ui-t="모델(버전)">' + escUi(L('모델(버전)')) + '</label>' +
        '<div class="d-flex flex-wrap gap-2 align-items-center">' +
        '<select name="' + escUi(prov) + '_model_sel" class="form-select form-select-sm hq-ai-model-sel" data-hq-ai-prov="' + escUi(prov) + '" style="min-width:12rem">' + opts + '</select>' +
        '<input type="text" name="' + escUi(prov) + '_model_custom" class="form-control form-control-sm hq-ai-model-custom" data-hq-ai-prov="' + escUi(prov) + '" data-pg-ui-placeholder="모델명 직접입력" placeholder="' + escUi(L('모델명 직접입력')) + '" style="max-width:16rem;display:none">' +
        '<input type="hidden" name="report_' + escUi(prov) + '_model" class="hq-ai-model-hidden" data-hq-ai-prov="' + escUi(prov) + '" value="">' +
        '</div>' +
        '<div class="form-check mt-2">' +
        '<input class="form-check-input hq-ai-prov-disabled" type="checkbox" name="report_' + escUi(prov) + '_disabled" id="hq_ai_dis_' + escUi(prov) + '" value="Y">' +
        '<label class="form-check-label small" for="hq_ai_dis_' + escUi(prov) + '"><span data-pg-ui-t="이 제공자·모델 사용중지">' + escUi(L('이 제공자·모델 사용중지')) + '</span></label>' +
        '<span class="small text-muted d-block ms-4" data-pg-ui-t="체크 시 챗봇·상품안내 LLM에서 이 API 키·모델 조합을 호출하지 않습니다.">' + escUi(L('체크 시 챗봇·상품안내 LLM에서 이 API 키·모델 조합을 호출하지 않습니다.')) + '</span>' +
        '</div></div>'
      );
    }
    var gemPre = ['gemini-3-flash-preview', 'gemini-3.1-pro-preview', 'gemini-3.1-flash-lite', 'gemini-2.5-pro', 'gemini-2.5-flash', 'gemini-2.5-flash-lite', 'gemini-2.0-flash', 'gemini-1.5-flash', 'gemini-1.5-pro'];
    var groqPre = ['llama-3.1-8b-instant', 'llama-3.3-70b-versatile', 'llama-3.1-70b-versatile', 'mixtral-8x7b-32768'];
    var antPre = ['claude-sonnet-4-5', 'claude-3-5-sonnet-20241022', 'claude-3-opus-20240229', 'claude-3-haiku-20240307'];
    var oaiPre = ['gpt-4o-mini', 'gpt-4o', 'gpt-4-turbo', 'gpt-4'];
    function provBlk(label, prov, presets, placeholder) {
      var keyNm = 'report_' + prov + '_api_key';
      return (
        '<div class="col-md-6 mb-3">' +
        '<label class="form-label"><span data-pg-ui-t="' + escUi(label) + '">' + escUi(L(label)) + '</span></label>' +
        '<input type="password" name="' + escUi(keyNm) + '" class="form-control form-control-sm hq-ai-api-key-inp" autocomplete="off" data-hq-ai-key-def-ph="' + escUi(L(placeholder)) + '" placeholder="' + escUi(L(placeholder)) + '">' +
        '<span class="small text-muted d-block mt-1 hq-ai-key-hint" data-hq-ai-hint="' + escUi(keyNm) + '"></span>' +
        modelSelectHtml(prov, presets) +
        '</div>'
      );
    }
    var ordSel = '';
    var pvLabels = [['gemini', 'Google Gemini'], ['groq', 'Groq'], ['anthropic', 'Anthropic(Claude)'], ['openai', 'OpenAI']];
    for (var r = 1; r <= 4; r++) {
      ordSel += '<label class="me-2 small"><span data-pg-ui-t="' + escUi(String(r) + '순위') + '">' + escUi(L(String(r) + '순위')) + '</span></label><select name="hqAiProvOrder_' + r + '" class="form-select form-select-sm d-inline-block me-3 mb-2" style="width:auto;min-width:9rem">';
      ordSel += '<option value="" data-pg-ui-t="비사용">' + escUi(L('비사용')) + '</option>';
      for (var j = 0; j < pvLabels.length; j++) {
        ordSel += '<option value="' + escUi(pvLabels[j][0]) + '">' + escUi(pvLabels[j][1]) + '</option>';
      }
      ordSel += '</select>';
    }
    return (
      '<div class="hq-chatbot-ai-settings">' +
      '<p class="small text-muted mb-3">' +
      '<span data-pg-ui-t="ziobiz/Stock AI 페이지와 동일한 JSON 키(report_*_api_key, report_*_model, report_provider_order, report_*_disabled)로 저장합니다. 챗봇·상품 안내(LLM 호출 시 서버에서 이 설정을 참조합니다).">' +
      escUi(L('ziobiz/Stock AI 페이지와 동일한 JSON 키(report_*_api_key, report_*_model, report_provider_order, report_*_disabled)로 저장합니다. 챗봇·상품 안내(LLM 호출 시 서버에서 이 설정을 참조합니다).')) +
      '</span>' +
      '<br><span class="text-muted" data-pg-ui-t="챗봇 상단 로고 자동축소( config_json 최상위, 선택 ): chatbot_logo_target_max_bytes(기본 2097152), chatbot_logo_max_edge_px(기본 1024), chatbot_logo_jpeg_quality_start(0~1, 기본 0.92), chatbot_logo_llm_tune_yn=Y(순위 LLM이 권장 변 길이 제안 → 서버 JPEG 재압축).">' +
      escUi(L('챗봇 상단 로고 자동축소( config_json 최상위, 선택 ): chatbot_logo_target_max_bytes(기본 2097152), chatbot_logo_max_edge_px(기본 1024), chatbot_logo_jpeg_quality_start(0~1, 기본 0.92), chatbot_logo_llm_tune_yn=Y(순위 LLM이 권장 변 길이 제안 → 서버 JPEG 재압축).')) +
      '</span>' +
      '</p>' +
      '<h6 class="border-bottom pb-2 mb-3" data-pg-ui-t="리포트 API 키(챗봇·상품안내 공용)">' + escUi(L('리포트 API 키(챗봇·상품안내 공용)')) + '</h6>' +
      '<div class="row">' +
      provBlk('Google Gemini API 키', 'gemini', gemPre, 'AIzaSy…') +
      provBlk('Groq API 키', 'groq', groqPre, 'gsk_…') +
      provBlk('Anthropic(Claude) API 키', 'anthropic', antPre, 'sk-ant-…') +
      provBlk('OpenAI API 키', 'openai', oaiPre, 'sk-…') +
      '</div>' +
      '<div class="mb-4">' +
      '<label class="form-label" data-pg-ui-t="챗봇용 AI 제공자 순위 (1순위부터, 비사용은 건너뜀)">' + escUi(L('챗봇용 AI 제공자 순위 (1순위부터, 비사용은 건너뜀)')) + '</label>' +
      '<div class="d-flex flex-wrap align-items-center">' + ordSel + '</div>' +
      '</div>' +
      '<h6 class="border-bottom pb-2 mb-3" data-pg-ui-t="프롬프트 (챗봇)">' + escUi(L('프롬프트 (챗봇)')) + '</h6>' +
      '<div class="mb-3">' +
      '<label class="form-label" data-pg-ui-t="우선 지시 (시스템)">' + escUi(L('우선 지시 (시스템)')) + '</label>' +
      '<textarea name="ai_system_prompt_chatbot" rows="5" class="form-control form-control-sm hq-ai-prompt-ta" data-pg-ui-placeholder="등록 상품 안내 시 반드시 지킬 규칙, 언어, 금액 왜곡 금지 등" placeholder="' +
      escUi(L('등록 상품 안내 시 반드시 지킬 규칙, 언어, 금액 왜곡 금지 등')) + '"></textarea>' +
      '<p class="form-text small text-muted mb-0 mt-1" data-pg-ui-t="등록 상품 안내 시 반드시 지킬 규칙, 언어, 금액 왜곡 금지 등">' +
      escUi(L('등록 상품 안내 시 반드시 지킬 규칙, 언어, 금액 왜곡 금지 등')) + '</p></div>' +
      '<div class="mb-3">' +
      '<label class="form-label" data-pg-ui-t="상품 카탈로그 사용자 프롬프트 템플릿">' + escUi(L('상품 카탈로그 사용자 프롬프트 템플릿')) + '</label>' +
      '<textarea name="ai_prompt_chatbot_catalog" rows="4" class="form-control form-control-sm hq-ai-prompt-ta" data-pg-ui-placeholder="상품 목록·가격 매칭 시 사용할 역할 안내" placeholder="' +
      escUi(L('상품 목록·가격 매칭 시 사용할 역할 안내')) + '"></textarea>' +
      '<p class="form-text small text-muted mb-0 mt-1" data-pg-ui-t="상품 목록·가격 매칭 시 사용할 역할 안내">' +
      escUi(L('상품 목록·가격 매칭 시 사용할 역할 안내')) + '</p></div>' +
      '<h6 class="border-bottom pb-2 mb-3" data-pg-ui-t="챗봇 상품등록 플랜(월 이용료)">' +
      escUi(L('챗봇 상품등록 플랜(월 이용료)')) + '</h6>' +
      pgUiParagraph('등록 가능 건수(10·20·50·80·100·150·200)별로 JPY·KRW·USD·CNY·THB 월 청구금액을 입력합니다. 자동 청구 시 가맹 소속 총판의 기준통화(첫 통화)가 위 다섯 통화 중 하나이면 그 통화 칸 금액을 미수금으로 올립니다. 총판에 없거나 통화가 맞지 않으면 가맹점 기준통화로 동일 규칙을 적용합니다. 챗봇결제(Y)·한도 지정 가맹은 매월 1회(서울) 전월분·사유 CHATBOT_MONTHLY_SERVICE·메모 CHATBOT_BILL:YYYY-MM', 'small text-muted') +
      '<div class="table-responsive mb-3"><table class="table table-sm table-bordered table-no-col-resize">' +
      '<thead><tr><th data-pg-ui-t="등록 가능(건)">' + escUi(L('등록 가능(건)')) + '</th>' +
      PG_CHATBOT_PLAN_CCY.map(function (ccy) {
        return '<th class="small text-nowrap">' + escUi(ccy) + '</th>';
      }).join('') +
      '</tr></thead><tbody>' +
      [10, 20, 50, 80, 100, 150, 200].map(function (s) {
        var tds = '<td class="align-middle">' + String(s) + '</td>';
        for (var ci = 0; ci < PG_CHATBOT_PLAN_CCY.length; ci++) {
          var ccy = PG_CHATBOT_PLAN_CCY[ci];
          tds += '<td><input type="number" min="0" step="0.01" class="form-control form-control-sm" name="hq_ai_chatbot_slot_' + String(s) + '_' + ccy + '" value="0"></td>';
        }
        return '<tr>' + tds + '</tr>';
      }).join('') +
      '</tbody></table></div>' +
      '<details class="mb-2">' +
      '<summary class="small text-muted" style="cursor:pointer" data-pg-ui-t="고급 — 출력 형식 제한(ai_system_options_chatbot JSON)">' + escUi(L('고급 — 출력 형식 제한(ai_system_options_chatbot JSON)')) + '</summary>' +
      '<div class="mt-2 p-3 border rounded bg-light">' +
      '<textarea name="_ai_system_options_chatbot_raw" rows="6" class="form-control font-monospace form-control-sm" placeholder="{ &quot;max_sentences&quot;: 8, … }">' +
      '</textarea>' +
      '<span class="small text-muted" data-pg-ui-t="JSON 객체. 비워 두면 변경 없음.">' +
      escUi(L('JSON 객체. 비워 두면 변경 없음.')) +
      '</span></div>' +
      '</details>' +
      '</div>'
    );
  }

  /** 전산설정관리: 표준시 — ziobiz/NOTI 시간·동기화 설정 대응 (신규 기본 Asia/Bangkok) */
  var HQ_LEDGER_DISPLAY_TZ_OPTIONS = [
    { v: 'Asia/Bangkok', t: 'Asia/Bangkok — 태국 (기본)' },
    { v: 'Asia/Seoul', t: 'Asia/Seoul — 대한민국' },
    { v: 'Asia/Tokyo', t: 'Asia/Tokyo — 일본' },
    { v: 'Asia/Shanghai', t: 'Asia/Shanghai — 중국' },
    { v: 'Asia/Ho_Chi_Minh', t: 'Asia/Ho_Chi_Minh — 베트남' },
    { v: 'Asia/Singapore', t: 'Asia/Singapore — 싱가포르' },
    { v: 'Asia/Manila', t: 'Asia/Manila — 필리핀' },
    { v: 'Asia/Jakarta', t: 'Asia/Jakarta — 인도네시아(서)' },
    { v: 'Asia/Dubai', t: 'Asia/Dubai — UAE' },
    { v: 'UTC', t: 'UTC' },
    { v: 'Europe/London', t: 'Europe/London' },
    { v: 'America/New_York', t: 'America/New_York (미 동부)' },
    { v: 'America/Los_Angeles', t: 'America/Los_Angeles (미 서부)' }
  ];
  /** 결제 후속조치: 승인 시각 기준 경과(시간) — 드롭다운 저장 */
  var HQ_PAY_FOLLOW_ELAPSED_HOUR_OPTIONS = [
    { v: '', t: '미설정' },
    { v: '1', t: '1시간' },
    { v: '2', t: '2시간' },
    { v: '3', t: '3시간' },
    { v: '6', t: '6시간' },
    { v: '12', t: '12시간' },
    { v: '18', t: '18시간' },
    { v: '24', t: '24시간' },
    { v: '36', t: '36시간' },
    { v: '48', t: '48시간' },
    { v: '72', t: '72시간' },
    { v: '168', t: '168시간 (7일)' }
  ];
  /** 후속조치 기준 국가(Zone) — 태국·일본 우선 */
  var HQ_PAY_FOLLOW_REF_ZONE_OPTIONS = [
    { v: 'Asia/Bangkok', t: '태국 (Asia/Bangkok)' },
    { v: 'Asia/Tokyo', t: '일본 (Asia/Tokyo)' },
    { v: 'Asia/Seoul', t: '대한민국 (Asia/Seoul)' },
    { v: 'Asia/Shanghai', t: '중국 (Asia/Shanghai)' },
    { v: 'Asia/Ho_Chi_Minh', t: '베트남 (Asia/Ho_Chi_Minh)' },
    { v: 'Asia/Singapore', t: '싱가포르 (Asia/Singapore)' },
    { v: 'Asia/Manila', t: '필리핀 (Asia/Manila)' },
    { v: 'Asia/Jakarta', t: '인도네시아 (Asia/Jakarta)' },
    { v: 'Asia/Dubai', t: 'UAE (Asia/Dubai)' },
    { v: 'UTC', t: 'UTC' },
    { v: 'Europe/London', t: 'Europe/London' },
    { v: 'America/New_York', t: '미 동부 (America/New_York)' },
    { v: 'America/Los_Angeles', t: '미 서부 (America/Los_Angeles)' }
  ];
  /** 환불·강제환불: 승인일 기준 경과(일) */
  var HQ_PAY_FOLLOW_ELAPSED_DAY_OPTIONS = (function () {
    var a = [{ v: '', t: '미설정' }];
    var i;
    for (i = 1; i <= 30; i++) {
      a.push({ v: String(i), t: String(i) + '일' });
    }
    [45, 60, 90].forEach(function (d) {
      a.push({ v: String(d), t: String(d) + '일' });
    });
    return a;
  })();
  /** 강제환불 기간(일) — 0이면 메뉴·버튼 비노출 */
  var HQ_PAY_FOLLOW_FORCE_DAY_OPTIONS = (function () {
    var a = [{ v: '', t: '미설정' }, { v: '0', t: '0일 (메뉴·버튼 숨김)' }];
    var i;
    for (i = 1; i <= 30; i++) {
      a.push({ v: String(i), t: String(i) + '일' });
    }
    [45, 60, 90].forEach(function (d) {
      a.push({ v: String(d), t: String(d) + '일' });
    });
    return a;
  })();

  /** 전산설정관리 — 결제 후속조치 표 (NOTI 환경설정 대응) */
  function hqLedgerPayFollowNotiTableHtml() {
    function escA(s) {
      return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
    }
    var ynUse = pgUiOptHtml([{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }]);
    var ynRef = pgUiOptHtml([{ v: 'N', t: '미반영' }, { v: 'Y', t: '반영' }]);
    var zones = pgUiOptHtml([{ v: '', t: '(전산 표준시와 동일)' }].concat(HQ_PAY_FOLLOW_REF_ZONE_OPTIONS));
    var days = pgUiOptHtml(HQ_PAY_FOLLOW_ELAPSED_DAY_OPTIONS);
    var forceDays = pgUiOptHtml(HQ_PAY_FOLLOW_FORCE_DAY_OPTIONS);
    var timeInputCls = 'form-control form-control-sm hq-pay-follow-time';
    var emailVoidHintKey = '자동무효·이메일무효를 함께 켜면 시작만 비활성화되고, 실제 시작은 자동무효 마감 다음 분부터입니다. 마감은 항상 지정 가능합니다(비우면 23:59). 이메일무효만 켜면 시작·마감 모두 설정합니다.';
    var pfFooterTailKey = '테이블에 동기화됩니다. 무효·수동무효는 승인일(「시간 선택 국가」Zone) 당일입니다. 환불은 태국 기준 결제일 익일의 설정 시각부터 일수이며, 강제환불은 그 일반 환불이 끝난 다음날 같은 시각부터입니다. TH·JP 시계는 참고용입니다.';
    return '<div class="border rounded hq-pay-follow-wrap">' +
      '<table class="table table-sm table-bordered align-middle mb-0 hq-pay-follow-table">' +
      '<thead class="table-light"><tr>' +
      pgUiThT('구분', 'hq-pay-follow-col-kind') +
      pgUiThT('기준', 'hq-pay-follow-col-basis') +
      pgUiThT('설정(사용)', 'hq-pay-follow-col-use text-center') +
      pgUiThT('시간·일자 설정', 'hq-pay-follow-col-val') +
      pgUiThT('정산 반영', 'hq-pay-follow-col-ref text-center') +
      '</tr></thead><tbody>' +
      '<tr class="hq-pay-follow-row-zone">' +
      '<td class="fw-semibold" data-pg-ui-t="시간 선택 국가">' + escA(L('시간 선택 국가')) + '</td>' +
      '<td class="small text-muted" data-pg-ui-t="기준 Zone">' + escA(L('기준 Zone')) + '</td>' +
      '<td class="text-center">—</td>' +
      '<td><select name="payFollowRefZone" class="form-select form-select-sm">' + zones + '</select>' +
      '<div class="small text-muted mt-1 hq-pay-follow-dual-clock">' +
      '<span class="d-block"><strong>TH</strong> <span class="hq-pay-follow-clock-th">—</span></span>' +
      '<span class="d-block"><strong>JP</strong> <span class="hq-pay-follow-clock-jp">—</span></span>' +
      '<span class="d-block mt-1 text-wrap"><strong data-pg-ui-t="선택 기준">' + escA(L('선택 기준')) + '</strong> <span class="hq-pay-follow-clock-sel">—</span></span>' +
      '</div></td>' +
      '<td class="text-center text-muted small">—</td></tr>' +
      '<tr>' +
      '<td class="fw-semibold">' + pgUiSpanT('무효') + ' <span class="badge bg-secondary" data-pg-ui-t="자동무효">' + escA(L('자동무효')) + '</span></td>' +
      '<td class="small" data-pg-ui-t="승인일(기준 Zone) 당일 구간">' + escA(L('승인일(기준 Zone) 당일 구간')) + '</td>' +
      '<td class="text-center"><select name="autoVoidYn" class="form-select form-select-sm hq-pay-follow-sel-use">' + ynUse + '</select></td>' +
      '<td class="hq-pay-follow-void-times"><div class="d-flex flex-wrap align-items-center gap-1 gap-md-2">' +
      pgUiSpanT('시작', 'text-nowrap small') +
      '<input type="time" step="60" name="autoVoidStartTime" class="' + timeInputCls + '" data-pg-ui-title="비우면 0:00 (당일 자정)" title="' + escA(L('비우면 0:00 (당일 자정)')) + '" />' +
      pgUiSpanT('~ 마감', 'text-nowrap small') +
      '<input type="time" step="60" name="autoVoidEndTime" class="' + timeInputCls + '" data-pg-ui-title="비우면 21:00 — 태국·기준 Zone 당일 (JP 동일 시각 +2h → 23:00)" title="' + escA(L('비우면 21:00 — 태국·기준 Zone 당일 (JP 동일 시각 +2h → 23:00)')) + '" />' +
      '</div></td>' +
      '<td class="text-center"><select name="autoVoidReflectSettlementYn" class="form-select form-select-sm hq-pay-follow-sel-ref">' + ynRef + '</select></td></tr>' +
      '<tr>' +
      '<td class="fw-semibold">' + pgUiSpanT('수동무효') + ' <span class="badge bg-secondary" data-pg-ui-t="이메일 무효">' + escA(L('이메일 무효')) + '</span></td>' +
      '<td class="small" data-pg-ui-t="승인일(기준 Zone) 당일 시작~마감(자동무효와 동일 형식)">' + escA(L('승인일(기준 Zone) 당일 시작~마감(자동무효와 동일 형식)')) + '</td>' +
      '<td class="text-center"><select name="emailVoidYn" class="form-select form-select-sm hq-pay-follow-sel-use">' + ynUse + '</select></td>' +
      '<td class="hq-pay-follow-void-times"><div class="d-flex flex-wrap align-items-center gap-1 gap-md-2">' +
      pgUiSpanT('시작', 'text-nowrap small') +
      '<input type="time" step="60" name="emailVoidStartTime" class="' + timeInputCls + '" />' +
      pgUiSpanT('~ 마감', 'text-nowrap small') +
      '<input type="time" step="60" name="emailVoidEndTime" class="' + timeInputCls + '" data-pg-ui-title="비우면 23:59" title="' + escA(L('비우면 23:59')) + '" />' +
      '</div><div class="small text-muted mt-1" data-pg-ui-html="' + escUi(emailVoidHintKey) + '">' + L(emailVoidHintKey) + '</div></td>' +
      '<td class="text-center"><select name="emailVoidReflectSettlementYn" class="form-select form-select-sm hq-pay-follow-sel-ref">' + ynRef + '</select></td></tr>' +
      '<tr>' +
      '<td class="fw-semibold">' + pgUiSpanT('환불') + ' <span class="badge bg-secondary" data-pg-ui-t="자동환불">' + escA(L('자동환불')) + '</span></td>' +
      '<td class="small" data-pg-ui-t="태국(Asia/Bangkok) 기준 결제일 익일 지정 시각부터 N일(기본 7)">' + escA(L('태국(Asia/Bangkok) 기준 결제일 익일 지정 시각부터 N일(기본 7)')) + '</td>' +
      '<td class="text-center"><select name="autoRefundYn" class="form-select form-select-sm hq-pay-follow-sel-use">' + ynUse + '</select></td>' +
      '<td><div class="d-flex flex-wrap align-items-center gap-2">' +
      pgUiSpanT('익일 시작', 'text-nowrap small') +
      '<input type="time" step="60" name="autoRefundWindowStartTime" class="' + timeInputCls + '" data-pg-ui-title="비우면 0:00" title="' + escA(L('비우면 0:00')) + '" />' +
      '<select name="autoRefundAfterDays" class="form-select form-select-sm hq-pay-follow-sel-days">' + days + '</select></div></td>' +
      '<td class="text-center"><select name="autoRefundReflectSettlementYn" class="form-select form-select-sm hq-pay-follow-sel-ref">' + ynRef + '</select></td></tr>' +
      '<tr>' +
      '<td class="fw-semibold" data-pg-ui-t="강제환불">' + escA(L('강제환불')) + '</td>' +
      '<td class="small" data-pg-ui-t="태국 기준 일반 환불이 끝난 다음날 동일 시각부터 M일(M=0이면 메뉴 비노출)">' + escA(L('태국 기준 일반 환불이 끝난 다음날 동일 시각부터 M일(M=0이면 메뉴 비노출)')) + '</td>' +
      '<td class="text-center"><select name="forceRefundYn" class="form-select form-select-sm hq-pay-follow-sel-use">' + ynUse + '</select></td>' +
      '<td><select name="forceRefundAfterDays" class="form-select form-select-sm hq-pay-follow-sel-days">' + forceDays + '</select></td>' +
      '<td class="text-center"><select name="forceRefundReflectSettlementYn" class="form-select form-select-sm hq-pay-follow-sel-ref">' + ynRef + '</select></td></tr>' +
      '</tbody></table>' +
      '<p class="small text-muted px-2 py-2 mb-0">' +
      '<span data-pg-ui-t="저장은 화면 하단 [저장]으로 합니다.">' + escA(L('저장은 화면 하단 [저장]으로 합니다.')) + '</span> ' +
      '<code>tb_hq_notify_env_config</code> ' +
      '<span data-pg-ui-html="' + escUi(pfFooterTailKey) + '">' + L(pfFooterTailKey) + '</span></p>' +
      '</div>';
  }

  /** 전산설정 — 조직 단계별 결제 후속조치(4종) 허용 상한 */
  function hqLedgerPayFollowLevelCapsTableHtml() {
    function escA(s) {
      return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
    }
    var capYn = pgUiOptHtml([{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }]);
    var fields = [
      { k: 'autoVoid', t: '자동무효' },
      { k: 'emailVoid', t: '이메일 무효' },
      { k: 'autoRefund', t: '자동환불' },
      { k: 'forceRefund', t: '강제환불' }
    ];
    var capIntroKey = '총본사가 단계마다 사용할 수 있는 네 가지 후속조치를 제한합니다. 전역 NOTI 설정이 꺼져 있으면 해당 기능은 동작하지 않습니다. 가맹점은 등록 시 개별 선택과 함께 적용되며(미선택 시 미사용), 이 표는 단계별 상한입니다.';
    var capFooterKey = '[단계별 허용 저장]으로만 반영됩니다(하단 전체 저장과 별도). 총본사·시스템 관리자만 변경할 수 있습니다.';
    var thead = '<tr>' + pgUiThT('조직 단계', 'text-nowrap') +
      fields.map(function (f) { return pgUiThT(String(f.t), 'text-center text-nowrap'); }).join('') + '</tr>';
    var rows = COMP_MNG_SEARCH_COMP_DIV_LEVELS.map(function (lev) {
      var levT = String(lev.t);
      var cells = fields.map(function (f) {
        return '<td class="text-center"><select class="form-select form-select-sm hq-pf-cap-sel" name="pfCap_' + escA(lev.v) + '_' + escA(f.k) + '">' + capYn + '</select></td>';
      }).join('');
      return '<tr data-pf-cap-level="' + escA(lev.v) + '"><td class="fw-semibold text-nowrap" data-pg-ui-t="' + escUi(levT) + '">' + escA(L(levT)) + '</td>' + cells + '</tr>';
    }).join('');
    return '<div class="border rounded mt-3 hq-pay-follow-cap-wrap">' +
      '<div class="d-flex flex-wrap align-items-center justify-content-between gap-2 px-2 py-2 border-bottom bg-light">' +
      '<span class="fw-semibold" data-pg-ui-t="조직 단계별 후속조치 기능 허용">' + escA(L('조직 단계별 후속조치 기능 허용')) + '</span>' +
      '<button type="button" class="btn btn-primary btn-sm" id="hqLedgerPayFollowLevelCapsSaveBtn" data-pg-ui-t="단계별 허용 저장">' + escA(L('단계별 허용 저장')) + '</button></div>' +
      '<p class="small text-muted px-2 pt-2 mb-1" data-pg-ui-html="' + escUi(capIntroKey) + '">' + L(capIntroKey) + '</p>' +
      '<table class="table table-sm table-bordered align-middle mb-0 w-100" id="grid_hqPayFollowLevelCaps">' +
      '<thead class="table-light">' + thead + '</thead><tbody>' + rows + '</tbody></table>' +
      '<p class="small text-muted px-2 py-2 mb-0" data-pg-ui-html="' + escUi(capFooterKey) + '">' + L(capFooterKey) + '</p></div>';
  }

  /** 업체관리 목록 검색: OrgLevel.code 와 동일 순서 (총본사 1 … 가맹점 7) */
  var COMP_MNG_SEARCH_COMP_DIV_LEVELS = [
    { v: 'HEADQUARTERS', t: '총본사', ord: 1 },
    { v: 'REGIONAL', t: '본사', ord: 2 },
    { v: 'MASTER_DIST', t: '총판', ord: 3 },
    { v: 'BRANCH', t: '지사', ord: 4 },
    { v: 'AGENCY', t: '대리점', ord: 5 },
    { v: 'SALES_OFFICE', t: '영업점', ord: 6 },
    { v: 'MERCHANT', t: '가맹점', ord: 7 }
  ];

  /**
   * 업체관리 검색용 업체구분 셀렉트 옵션.
   * 총본사~가맹점 전 단계 + 전체(목록 API도 비관리자에게 전체 조직 반환).
   */
  function getCompMngSearchCompDivOptions(viewerOrgLevel, isAdmin) {
    return [{ v: '', t: '전체' }].concat(COMP_MNG_SEARCH_COMP_DIV_LEVELS.map(function (o) { return { v: o.v, t: o.t }; }));
  }

  /** 수수료설정: 조직 단계별(총본사~가맹) 수수료 격자 — 가맹 열은 총본사~영업점 합계(읽기 전용) */
  function hqDefaultCommissionTierMatrixHtml() {
    var tierLevels = [
      { k: 'hq', t: '총본사' },
      { k: 'regional', t: '본사' },
      { k: 'master', t: '총판' },
      { k: 'branch', t: '지사' },
      { k: 'agency', t: '대리점' },
      { k: 'salesOffice', t: '영업점' },
      { k: 'merchant', t: '가맹점' }
    ];
    var R = [
      { k: 'payRate', t: '결제수수료율', u: '%' },
      { k: 'perTxFee', t: '건당수수료', u: '(건)' },
      { k: 'failFee', t: '실패수수료', u: '(건)' },
      { k: 'cancelRate', t: '취소수수료', u: '(건)' },
      { k: 'voidFeePerTx', t: '무효수수료', u: '(건)' },
      { k: 'manualVoidFeePerTx', t: '수무효수수료', u: '(건)' },
      { k: 'refundRate', t: '환불수수료', u: '(건)' },
      { k: 'feeSettlementPerTx', t: '정산수수료', u: '(건)' },
      { k: 'remittanceTransferFee', t: '송금수수료', u: '(건)' },
      { k: 'usdtTransferFeeUsd', t: 'USDT 송금수수료', u: '(건)' },
      { k: 'fee3dsRate', t: '3DS 고정', u: '(건)' },
      { k: 'feeUsdt', t: 'USDT수수료율', u: '%' },
      { k: 'feeFx', t: 'FX수수료율', u: '%' },
      { k: 'usageRate', t: '월간이용료', u: '월' },
      { k: 'chargebackFeePerTx', t: '차지백수수료', u: '(건)' },
      { k: 'splitPayFeePct', t: '분할수수료율', u: '%' },
      { k: 'splitPayFixedFeePerInst', t: '분할고정수수료', u: '(건)' }
    ];
    var th = tierLevels.map(function (x) {
      var tk = String(x.t);
      return '<th class="text-center align-middle small text-nowrap" data-pg-ui-t="' + escUi(tk) + '">' + escUi(L(tk)) + '</th>';
    }).join('');
    var trb = '';
    R.forEach(function (r, idx) {
      var tds = tierLevels.map(function (lv) {
        var ro = lv.k === 'merchant' ? ' readonly tabindex="-1" class="form-control form-control-sm hq-tier-cell text-center bg-light"' : ' class="form-control form-control-sm hq-tier-cell text-center"';
        return '<td class="p-1"><input type="text"' + ro + ' data-fee="' + r.k + '" data-level="' + lv.k + '" autocomplete="off" /></td>';
      }).join('');
      var rtk = String(r.t);
      var ruk = String(r.u);
      trb += '<tr><td class="text-center small text-muted">' + (idx + 1) + '</td><td class="small text-start ps-2" data-pg-ui-t="' + escUi(rtk) + '">' + escUi(L(rtk)) + '</td><td class="small text-center text-muted" data-pg-ui-t="' + escUi(ruk) + '">' + escUi(L(ruk)) + '</td>' + tds + '</tr>';
    });
    return '<div class="table-responsive border rounded mb-3 hq-comm-tier-wrap">' +
      '<table class="table table-sm table-bordered align-middle mb-0 hq-comm-tier-matrix">' +
      '<thead class="table-light"><tr><th class="text-center align-middle" style="width:2.25rem">#</th>' +
      '<th class="text-center align-middle small" style="min-width:6.5rem" data-pg-ui-t="내용">' + escUi(L('내용')) + '</th><th class="text-center align-middle small" style="width:2.75rem" data-pg-ui-t="단위">' + escUi(L('단위')) + '</th>' + th + '</tr></thead><tbody>' + trb + '</tbody></table></div>';
  }

  /** 수수료설정: 기타(비고) 수수료 4슬롯 — 가맹 열은 총본사~영업점 합계(읽기 전용) */
  function hqDefaultExtraFeesCardHtml() {
    var tierLevels = [
      { k: 'hq', t: '총본사' },
      { k: 'regional', t: '본사' },
      { k: 'master', t: '총판' },
      { k: 'branch', t: '지사' },
      { k: 'agency', t: '대리점' },
      { k: 'salesOffice', t: '영업점' },
      { k: 'merchant', t: '가맹점' }
    ];
    var th = '<th class="text-center small" data-pg-ui-t="유형">' + escUi(L('유형')) + '</th><th class="text-center small" style="min-width:6rem" data-pg-ui-t="수수료명">' + escUi(L('수수료명')) + '</th>' +
      tierLevels.map(function (x) { var tk = String(x.t); return '<th class="text-center small text-nowrap" data-pg-ui-t="' + escUi(tk) + '">' + escUi(L(tk)) + '</th>'; }).join('');
    function extraRow(i) {
      var tds = tierLevels.map(function (lv) {
        var ro = lv.k === 'merchant' ? ' readonly tabindex="-1" class="form-control form-control-sm hq-tier-extra-cell text-center bg-light"' : ' class="form-control form-control-sm hq-tier-extra-cell text-center"';
        return '<td class="p-1"><input type="text"' + ro + ' data-slot="' + i + '" data-level="' + lv.k + '" autocomplete="off" /></td>';
      }).join('');
      return '<tr><td class="p-1 align-middle"><select name="extraFee' + i + 'Mode" class="form-select form-select-sm">' +
        '<option value="">—</option><option value="PCT">%</option><option value="FIX" data-pg-ui-t="고정">' + escUi(L('고정')) + '</option></select></td>' +
        '<td class="p-1 align-middle"><input type="text" name="extraFee' + i + 'Name" class="form-control form-control-sm" maxlength="64" data-pg-ui-placeholder="이름" placeholder="' + escUi(L('이름')) + '" autocomplete="off" /></td>' + tds + '</tr>';
    }
    return '<div class="card border mb-3 hq-extra-fees-card">' +
      '<div class="card-header py-2 px-3 bg-light">' +
      '<strong class="small d-block mb-1" data-pg-ui-t="기타 수수료 (비고 · 최대 4건)">' + escUi(L('기타 수수료 (비고 · 최대 4건)')) + '</strong>' +
      '<span class="text-muted small" data-pg-ui-t="이름·유형·조직별 값을 넣은 슬롯만 반영됩니다. 가맹 열은 총본사~영업점 합계로 표시·저장됩니다.">' + escUi(L('이름·유형·조직별 값을 넣은 슬롯만 반영됩니다. 가맹 열은 총본사~영업점 합계로 표시·저장됩니다.')) + '</span></div>' +
      '<div class="card-body py-2 px-3 table-responsive"><table class="table table-sm table-bordered align-middle mb-0 hq-extra-tier-table">' +
      '<thead class="table-light"><tr>' + th + '</tr></thead><tbody>' +
      extraRow(1) + extraRow(2) + extraRow(3) + extraRow(4) + '</tbody></table></div></div>';
  }

  /** 대행수수료설정 — 기타 수수료 4슬롯(가맹 단일 값) */
  function hqPgAgencyCostExtraFeesHtml() {
    function extraRow(i) {
      return '<tr><td class="p-1 align-middle"><select name="extraFee' + i + 'Mode" class="form-select form-select-sm">' +
        '<option value="">—</option><option value="PCT">%</option><option value="FIX" data-pg-ui-t="고정">' + escUi(L('고정')) + '</option></select></td>' +
        '<td class="p-1 align-middle"><input type="text" name="extraFee' + i + 'Name" class="form-control form-control-sm" maxlength="64" data-pg-ui-placeholder="이름" placeholder="' + escUi(L('이름')) + '" autocomplete="off" /></td>' +
        '<td class="p-1 align-middle"><input type="text" name="extraFee' + i + 'Value" class="form-control form-control-sm text-center" autocomplete="off" /></td></tr>';
    }
    return '<div class="card border mb-3 hq-pg-cost-extra-card">' +
      '<div class="card-header py-2 px-3 bg-light"><strong class="small" data-pg-ui-t="기타 수수료 (비고 · 최대 4건)">' + escUi(L('기타 수수료 (비고 · 최대 4건)')) + '</strong></div>' +
      '<div class="card-body py-2 px-3"><table class="table table-sm table-bordered align-middle mb-0">' +
      '<thead class="table-light"><tr><th class="text-center small" data-pg-ui-t="유형">' + escUi(L('유형')) + '</th><th class="text-center small" data-pg-ui-t="수수료명">' + escUi(L('수수료명')) + '</th><th class="text-center small" data-pg-ui-t="값">' + escUi(L('값')) + '</th></tr></thead><tbody>' +
      extraRow(1) + extraRow(2) + extraRow(3) + extraRow(4) + '</tbody></table></div></div>';
  }

  /** 대행수수료설정 — 본문(목록+편집) */
  function hqPgAgencyCostPolicyPageHtml() {
    return '<div id="hqPgAgencyCostFlash" class="alert alert-dismissible d-none mb-3" role="alert"><span data-pg-banner-text></span>' +
      '<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="' + escUi(L('닫기')) + '"></button></div>' +
      '<p class="small text-muted mb-3" data-pg-ui-html="노티·거래는 동일 저장소를 사용합니다. 여기서는 <strong>ICOPAY↔PG대행사 계약</strong> 관점의 수수료·담보·정산 주기(T+N 등)만 설정합니다. PG는 <strong>API연동설정</strong>에 등록된 PG코드별로 1건씩 저장합니다.">' + L('노티·거래는 동일 저장소를 사용합니다. 여기서는 <strong>ICOPAY↔PG대행사 계약</strong> 관점의 수수료·담보·정산 주기(T+N 등)만 설정합니다. PG는 <strong>API연동설정</strong>에 등록된 PG코드별로 1건씩 저장합니다.') + '</p>' +
      '<div class="row g-3"><div class="col-12 col-lg-5"><div class="card h-100"><div class="card-header py-2 small fw-semibold" data-pg-ui-t="저장된 원가 정책">' + escUi(L('저장된 원가 정책')) + '</div><div class="card-body p-2">' +
      '<div class="table-responsive border rounded" style="max-height:520px;overflow-y:auto"><table class="table table-sm table-hover align-middle mb-0 hq-pg-cost-policy-list-table">' +
      '<thead class="table-light"><tr><th class="text-nowrap" data-pg-ui-t="PG코드">' + escUi(L('PG코드')) + '</th><th data-pg-ui-t="PG대행사">' + escUi(L('PG대행사')) + '</th><th class="text-nowrap" data-pg-ui-t="결제%">' + escUi(L('결제%')) + '</th><th class="text-nowrap" data-pg-ui-t="정산주기">' + escUi(L('정산주기')) + '</th><th class="text-nowrap" data-pg-ui-t="통화">' + escUi(L('통화')) + '</th></tr></thead>' +
      '<tbody id="hqPgAgencyCostListTbody"><tr><td colspan="5" class="text-muted text-center small">' + escUi(L('불러오는 중…')) + '</td></tr></tbody></table></div>' +
      '<button type="button" class="btn btn-success btn-sm mt-2 w-100" id="hqPgAgencyCostNewBtn" data-pg-ui-t="신규 PG">' + escUi(L('신규 PG')) + '</button></div></div></div>' +
      '<div class="col-12 col-lg-7"><div class="card h-100"><div class="card-header py-2 small fw-semibold" data-pg-ui-t="편집">' + escUi(L('편집')) + '</div><div class="card-body p-2">' +
      '<div class="row g-2 mb-2"><div class="col-md-6"><label class="form-label small mb-0" for="hqPgCostPgCd" data-pg-ui-t="PG대행사">' + escUi(L('PG대행사')) + '</label>' +
      '<select class="form-select form-select-sm" id="hqPgCostPgCd" name="pgCd"></select></div>' +
      '<div class="col-md-3"><label class="form-label small mb-0" for="hqPgCostCurrencyCode" data-pg-ui-t="기준통화">' + escUi(L('기준통화')) + '</label>' +
      '<select class="form-select form-select-sm" id="hqPgCostCurrencyCode" name="currencyCode">' +
      '<option value="KRW">KRW</option><option value="USD">USD</option><option value="JPY">JPY</option><option value="EUR">EUR</option>' +
      '<option value="CNY">CNY</option><option value="THB">THB</option><option value="VND">VND</option><option value="GBP">GBP</option>' +
      '<option value="TWD">TWD</option><option value="HKD">HKD</option><option value="USDT">USDT</option></select></div>' +
      '<div class="col-md-3"><label class="form-label small mb-0" for="hqPgCostUseYn" data-pg-ui-t="사용">' + escUi(L('사용')) + '</label>' +
      '<select class="form-select form-select-sm" id="hqPgCostUseYn" name="useYn"><option value="Y" data-pg-ui-t="사용">' + escUi(L('사용')) + '</option><option value="N" data-pg-ui-t="미사용">' + escUi(L('미사용')) + '</option></select></div></div>' +
      '<div class="border rounded p-2 mb-2"><div class="small fw-semibold mb-2" data-pg-ui-t="수수료(건당·%)">' + escUi(L('수수료(건당·%)')) + '</div>' +
      '<div class="row g-2 small">' +
      feeInp('payRate', L('결제수수료(%)'), 2) + feeInp('perTxFee', L('건당수수료'), 2) + feeInp('failFee', L('실패수수료'), 2) +
      feeInp('cancelRate', L('취소수수료'), 2) + feeInp('voidFeePerTx', L('무효(건)'), 2) + feeInp('manualVoidFeePerTx', L('수동무효(건)'), 2) +
      feeInp('refundRate', L('환불(건)'), 2) + feeInp('feeSettlementPerTx', L('정산수수료'), 2) + feeInp('usageRate', L('월이용료'), 2) +
      feeInp('fee3dsRate', L('3DS(건)'), 2) + feeInp('chargebackFeePerTx', L('차지백(건)'), 2) +
      feeInp('remittanceTransferFee', L('송금이체'), 2) + feeInp('usdtTransferFeeUsd', L('USDT송금(USD)'), 2) +
      feeInp('feeUsdt', L('USDT(%)'), 2) + feeInp('feeFx', L('FX(%)'), 2) +
      '</div><div class="mt-2"><label class="form-label small mb-0" for="hqPgCostChargebackPolicyId" data-pg-ui-t="차지백 구간정책">' + escUi(L('차지백 구간정책')) + '</label>' +
      '<select class="form-select form-select-sm" id="hqPgCostChargebackPolicyId" name="chargebackPolicyId"><option value="" data-pg-ui-t="(미사용) 건당 차지백만">' + escUi(L('(미사용) 건당 차지백만')) + '</option></select></div></div>' +
      hqPgAgencyCostExtraFeesHtml() +
      '<div class="border rounded p-2 mb-2"><div class="small fw-semibold mb-2" data-pg-ui-t="담보(롤링)">' + escUi(L('담보(롤링)')) + '</div>' +
      '<div class="row g-2">' + feeInp('rollingPct', L('롤링(%)'), 3) + feeInp('rollingDays', L('보류일수'), 3) + '</div></div>' +
      '<div class="border rounded p-2 mb-2"><div class="small fw-semibold mb-2" data-pg-ui-t="PG 정산 주기 (거래 시각 기준)">' + escUi(L('PG 정산 주기 (거래 시각 기준)')) + '</div>' +
      '<p class="small text-muted mb-2" data-pg-ui-t="기준은 항상 TRANSACTION(결제 시각)입니다. T=영업일 N일 후 동일 시각, H=24×N시간, D=달력 N일·일괄 시각.">' + escUi(L('기준은 항상 TRANSACTION(결제 시각)입니다. T=영업일 N일 후 동일 시각, H=24×N시간, D=달력 N일·일괄 시각.')) + '</p>' +
      '<div class="row g-2"><div class="col-md-3"><label class="form-label small mb-0" data-pg-ui-t="기준">' + escUi(L('기준')) + '</label><input type="text" class="form-control form-control-sm" name="settleBasis" value="TRANSACTION" readonly /></div>' +
      '<div class="col-md-3"><label class="form-label small mb-0" for="hqPgCostSettleType" data-pg-ui-t="유형">' + escUi(L('유형')) + '</label>' +
      '<select class="form-select form-select-sm" id="hqPgCostSettleType" name="settleScheduleType">' +
      '<option value="T" data-pg-ui-t="PG정산유형 T">T</option><option value="H" data-pg-ui-t="PG정산유형 H">H</option><option value="D" data-pg-ui-t="PG정산유형 D">D</option></select></div>' +
      '<div class="col-md-3"><label class="form-label small mb-0" for="hqPgCostSettleLagN" data-pg-ui-t="N">N</label>' +
      '<input type="number" class="form-control form-control-sm" id="hqPgCostSettleLagN" name="settleLagN" min="1" max="30" value="1" /></div>' +
      '<div class="col-md-3" id="hqPgCostSettleBatchWrap"><label class="form-label small mb-0" for="hqPgCostSettleBatchTime" data-pg-ui-t="D 일괄시각">' + escUi(L('D 일괄시각')) + '</label>' +
      '<input type="text" class="form-control form-control-sm" id="hqPgCostSettleBatchTime" name="settleBatchTime" placeholder="09:00" data-pg-ui-placeholder="09:00" /></div></div></div>' +
      '<div class="mb-2"><label class="form-label small mb-0" for="hqPgCostPolicyRemark" data-pg-ui-t="정책비고">' + escUi(L('정책비고')) + '</label>' +
      '<textarea class="form-control form-control-sm" id="hqPgCostPolicyRemark" name="policyRemark" rows="2"></textarea></div>' +
      '<div class="d-flex flex-wrap gap-2"><button type="button" class="btn btn-primary btn-sm" id="hqPgAgencyCostSaveBtn" data-pg-ui-t="저장">' + escUi(L('저장')) + '</button></div>' +
      '</div></div></div></div>';
    function feeInp(name, labelKo, col) {
      var lk = String(labelKo);
      return '<div class="col-6 col-md-' + col + '"><label class="form-label small mb-0" for="hqPgCost_' + name + '" data-pg-ui-t="' + escUi(lk) + '">' + escUi(L(lk)) + '</label>' +
        '<input type="text" class="form-control form-control-sm" id="hqPgCost_' + name + '" name="' + name + '" autocomplete="off" /></div>';
    }
  }

  /** 정산주기: v·t 모두 코드 명(본사 API 로드 시 동일). 설명은 API의 d·option title */
  var CALC_CYCLE_OPTIONS = [
    { v: '', t: '선택' },
    { v: 'NONE', t: 'NONE' },
    { v: 'RT', t: 'RT' },
    { v: 'T0', t: 'T0' },
    { v: 'TM5', t: 'TM5' },
    { v: 'TM10', t: 'TM10' },
    { v: 'TM30', t: 'TM30' },
    { v: 'M5', t: 'M5' },
    { v: 'M10', t: 'M10' },
    { v: 'M30', t: 'M30' },
    { v: 'H1', t: 'H1' },
    { v: 'TH1', t: 'TH1' },
    { v: 'H2', t: 'H2' },
    { v: 'TH2', t: 'TH2' },
    { v: 'H4', t: 'H4' },
    { v: 'TH4', t: 'TH4' },
    { v: 'H6', t: 'H6' },
    { v: 'TH6', t: 'TH6' },
    { v: 'H8', t: 'H8' },
    { v: 'TH8', t: 'TH8' },
    { v: 'H12', t: 'H12' },
    { v: 'TH12', t: 'TH12' },
    { v: 'D0', t: 'D0' },
    { v: 'D1', t: 'D1' },
    { v: 'D2', t: 'D2' },
    { v: 'D3', t: 'D3' },
    { v: 'D5', t: 'D5' },
    { v: 'D7', t: 'D7' },
    { v: 'D10', t: 'D10' },
    { v: 'D15', t: 'D15' },
    { v: 'D20', t: 'D20' },
    { v: 'D30', t: 'D30' },
    { v: 'W3', t: 'W3' },
    { v: 'W5', t: 'W5' },
    { v: 'W7', t: 'W7' },
    { v: 'W10', t: 'W10' },
    { v: 'W14', t: 'W14' },
    { v: 'WK1W', t: 'WK1W' },
    { v: 'WK2W', t: 'WK2W' },
    { v: 'WK1WT', t: 'WK1WT' },
    { v: 'WK2WT', t: 'WK2WT' },
    { v: 'WK1WM', t: 'WK1WM' },
    { v: 'WK2WM', t: 'WK2WM' }
  ];
  var CALC_CYCLE_SEARCH_OPTIONS = [{ v: '', t: '전체' }].concat(CALC_CYCLE_OPTIONS.filter(function (o) { return o.v !== ''; }));

  /** 무효·수동무효·환불·강제환불 정산 방식 — 본사·가맹 수수료정책 공통 */
  var VOID_REFUND_SETTLE_MODE_OPTIONS = [
    { v: 'GENERAL', t: '일반형 (순매출 차감·무효·환불 시 성공 수수료 미추가)' },
    { v: 'REVENUE', t: '수익형 (순매출 미차감·무효·환불 시 성공 수수료 이중 과금)' },
    { v: 'HYBRID', t: '하이브리드1 (무효·수무: 순매출 차감·이중과금 / 환불·강제: 순매출 유지·건당만)' },
    { v: 'HYBRID2', t: '하이브리드2 (환불·강제: 순매출 차감·이중과금 / 무효·수무: 순매출 유지·건당만)' }
  ];
  if (typeof window !== 'undefined') {
    window.PG_VOID_REFUND_SETTLE_MODE_OPTIONS = VOID_REFUND_SETTLE_MODE_OPTIONS;
  }

  /** 본사설정 > 정산관리설정 — 정산주기·일정 미리보기 UI (getScreenHtml 呼び出し時に L 適用) */
  function hqSettlementAdminStaticHtml() {
    return ''
    + '<div class="hq-settlement-admin">'
    + '<div class="card mb-3">' + pgUiCardHeaderT('정산관리 안내') + '<div class="card-body small">'
    + pgUiParagraph('배치·수동 정산은 가맹 정산주기·AUTO·마감과 동일합니다. 표는 정산일과 집계기간(from~to), 자동가맹 수 요약입니다.', 'mb-2')
    + '<ul class="mb-0 ps-3">'
    + pgUiLiT('D+N · W+N: 일·주 단위, 실행마다 1건.')
    + pgUiLiT('WK: 주(또는 격주) 마감 뒤 영업일 3·10·30일째 등, 1건.')
    + pgUiLiT('RT: 건별. T0 · TM · TH: 당일 합산 갱신.')
    + pgUiLiT('M5·M10·M30: 분마다. H1~H12: 시간마다(예: H1 하루 24회).')
    + pgUiLiT('무효·환불 정산(본사 기본·총판별)은 본사설정 → 환수/미수금설정에서 설정합니다.')
    + '</ul>'
    + '</div></div>'
    + '<div class="card mb-3" id="hqStAutoBatchCard">' + pgUiCardHeaderT('자동 정산 배치 (총 스위치)') + '<div class="card-body">'
    + pgUiParagraph('가맹 정산구분 AUTO·정산주기와는 별개입니다. ① 서버 타이머가 켜져 있고 ② 본사 DB 모드가 허용일 때만 스케줄 tick 이 본문을 실행합니다. RT 건별 정산은 이 스위치와 무관합니다.', 'small text-muted mb-2')
    + '<ol class="small text-muted mb-3 ps-3"><li class="mb-1"><span data-pg-ui-t="① 서버(Java) — 서버가 시작될 때 읽는 설정으로, 주기적으로 정산을 돌릴 타이머를 켤지 말지 정합니다. 이 관리자 화면에서는 상태만 표시합니다.">' + escUi(L('① 서버(Java) — 서버가 시작될 때 읽는 설정으로, 주기적으로 정산을 돌릴 타이머를 켤지 말지 정합니다. 이 관리자 화면에서는 상태만 표시합니다.')) + '</span></li>'
    + '<li><span data-pg-ui-t="② 본사 DB — 활성(항상 tick 본문 시도)·비활성(tick 본문 끔)·자동(이번 주기에 돌릴 AUTO 가맹이 있을 때만) 중 하나를 저장합니다.">' + escUi(L('② 본사 DB — 활성(항상 tick 본문 시도)·비활성(tick 본문 끔)·자동(이번 주기에 돌릴 AUTO 가맹이 있을 때만) 중 하나를 저장합니다.')) + '</span></li></ol>'
    + '<div class="border rounded p-3 mb-2 bg-light">'
    + '<div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-1">'
    + '<span class="fw-semibold small" data-pg-ui-t="① 서버 — 자동 정산 타이머">' + escUi(L('① 서버 — 자동 정산 타이머')) + '</span>'
    + '<span id="hqStAutoBatchJvmBadge" class="badge rounded-pill text-bg-secondary" data-pg-ui-t="확인 중…">' + escUi(L('확인 중…')) + '</span></div>'
    + '<p class="small text-muted mb-0" id="hqStAutoBatchJvmNote" data-pg-ui-t="서버에서 응답을 불러오는 중입니다.">' + escUi(L('서버에서 응답을 불러오는 중입니다.')) + '</p></div>'
    + '<div class="border rounded p-3 mb-2">'
    + '<div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-1">'
    + '<span class="fw-semibold small" data-pg-ui-t="② 본사 DB — 배치 모드">' + escUi(L('② 본사 DB — 배치 모드')) + '</span>'
    + '<div class="d-flex flex-wrap align-items-center gap-2">'
    + '<select id="hqStAutoBatchYnSel" class="form-select form-select-sm" style="max-width:14rem">'
    + '<option value="ACTIVE" data-pg-ui-t="활성 (항상)">' + escUi(L('활성 (항상)')) + '</option><option value="AUTO" data-pg-ui-t="자동 (대상 있을 때만)">' + escUi(L('자동 (대상 있을 때만)')) + '</option><option value="INACTIVE" data-pg-ui-t="비활성">' + escUi(L('비활성')) + '</option></select>'
    + '<button type="button" class="btn btn-primary btn-sm" id="hqStAutoBatchSaveBtn" data-pg-ui-t="저장">' + escUi(L('저장')) + '</button></div></div>'
    + pgUiParagraph('②가 비활성이면 ①이 켜져 있어도 tick 본문은 실행되지 않습니다. 자동은 이번 주기에 실행할 AUTO 가맹이 없으면 스킵합니다.', 'small text-muted mb-0') + '</div>'
    + '<div class="alert alert-secondary mb-0 py-2 px-3 small" id="hqStAutoBatchResultAlert" role="status">'
    + '<div class="fw-semibold mb-1" data-pg-ui-t="현재 자동 배치">' + escUi(L('현재 자동 배치')) + '</div>'
    + '<div id="hqStAutoBatchEffectiveCell" class="mb-1">\u2014</div>'
    + '<div class="text-muted" id="hqStAutoBatchHint"></div></div>'
    + '</div></div>'
    + '<div class="card mb-3" id="hqMdBizCronCard">' + pgUiCardHeaderT('총판별 기준 영업일 및 정산 크론 기준') + '<div class="card-body">'
    + pgUiParagraph('현재영업일 열은 총판 업체등록 시 저장된 영업일·휴일(프로필명·기준국가 등)을 보여 주며, 비어 있으면 상위 본사(REGIONAL) 설정을 참고해 표시할 수 있습니다. 거래시간(1줄)은 결제·통합내역 그리드의 첫 번째 시각 줄입니다. 정산 크론(2줄)은 격자·마감·D0 및 두 번째 시각 줄에 쓰는 Zone입니다. 셀렉트만 바꿔서는 저장되지 않습니다. 행의 저장을 눌러 주세요.', 'small text-muted mb-2')
    + '<div class="d-flex flex-wrap align-items-center gap-2 mb-2"><button type="button" class="btn btn-outline-secondary btn-sm" id="hqMdBizCronRefreshBtn" data-pg-ui-t="목록 새로고침">' + escUi(L('목록 새로고침')) + '</button><span class="small text-muted" data-pg-ui-t="저장 후 서버 값을 다시 보려면 새로고침을 누르세요.">' + escUi(L('저장 후 서버 값을 다시 보려면 새로고침을 누르세요.')) + '</span></div>'
    + '<div class="table-responsive table-no-col-resize-wrap"><table class="table table-sm table-bordered align-middle mb-0 table-no-col-resize"><thead class="table-light"><tr><th data-pg-ui-t="총판">총판</th><th class="text-nowrap" data-pg-ui-t="현재영업일">현재영업일</th><th style="min-width:9rem" data-pg-ui-t="거래시간(1줄)">거래시간(1줄)</th><th style="min-width:14rem" data-pg-ui-t="정산 크론(2줄)">정산 크론(2줄)</th><th class="text-end text-nowrap" style="width:5.5rem" data-pg-ui-t="저장">저장</th></tr></thead><tbody id="hqMdBizCronTbody"><tr><td colspan="5" class="text-muted text-center">' + escUi(L('불러오는 중…')) + '</td></tr></tbody></table></div>'
    + '<p class="small text-muted mb-0 mt-2" id="hqMdBizCronHint"></p>'
    + '</div></div>'
    + '<div class="card mb-3">' + pgUiCardHeaderT('총판별 가맹 정산주기 (최대 10건·대표)') + '<div class="card-body">'
    + pgUiParagraph('총판(MASTER_DIST)마다 가맹점 등록 시 선택 가능한 정산주기를 최대 10개까지 지정합니다(2개·5개처럼 일부만 채워도 됩니다). 서로 다른 주기는 최소 2개 필요하며, 대표는 신규 가맹 시 셀렉트 기본값입니다. 아래 슬롯 셀렉트는 본사 표준 병합 전체(미사용 N 포함)이며, 코드·행 순서는 위 정산주기관리의 표준 주기(시스템)·DB등록 표와 동일합니다. 미설정 총판이거나 상위에 총판이 없으면 가맹 화면은 기존처럼 사용(Y)만 노출됩니다.', 'small text-muted mb-3')
    + '<div class="row g-2 align-items-end mb-2">'
    + '<div class="col-12 col-md-5"><label class="form-label small mb-0" data-pg-ui-t="총판">총판</label><select id="hqMdCycleOrgSel" class="form-select form-select-sm"><option value="">' + escUi(L('불러오는 중…')) + '</option></select></div>'
    + '<div class="col-auto d-grid"><button type="button" class="btn btn-primary btn-sm" id="hqMdCycleSaveBtn" data-pg-ui-t="저장">' + escUi(L('저장')) + '</button></div></div>'
    + '<div class="table-responsive table-no-col-resize-wrap"><table class="table table-sm table-bordered align-middle mb-0 table-no-col-resize"><thead class="table-light"><tr><th style="width:3.5rem">#</th><th data-pg-ui-t="정산주기">정산주기</th><th style="width:5rem" data-pg-ui-t="대표">대표</th></tr></thead><tbody id="hqMdCycleTbody"></tbody></table></div>'
    + '</div></div>'
    + '<div class="card mb-3">' + pgUiCardHeaderT('정산주기관리 (DB 등록)') + '<div class="card-body">'
    + '<div class="row g-2 align-items-end mb-2">'
    + '<div class="col-6 col-md-2"><label class="form-label small mb-0" data-pg-ui-t="유형">유형</label><select id="hqStAddFamily" class="form-select form-select-sm">'
    + '<option value="D" data-pg-ui-t="D+N (일)">' + escUi(L('D+N (일)')) + '</option><option value="W" data-pg-ui-t="W+N (주)">' + escUi(L('W+N (주)')) + '</option><option value="WK" data-pg-ui-t="WK 코드">' + escUi(L('WK 코드')) + '</option></select></div>'
    + '<div class="col-6 col-md-2" id="hqStOffsetWrap"><label class="form-label small mb-0">N</label><input type="number" id="hqStAddOffset" class="form-control form-control-sm" min="0" max="90" data-pg-ui-placeholder="예: 12" placeholder="' + escUi(L('예: 12')) + '"></div>'
    + '<div class="col-12 col-md-3 d-none" id="hqStWkWrap"><label class="form-label small mb-0" data-pg-ui-t="WK 코드">WK 코드</label><select id="hqStWkKey" class="form-select form-select-sm">'
    + '<option value="WK1W">WK1W</option><option value="WK2W">WK2W</option><option value="WK1WT">WK1WT</option><option value="WK2WT">WK2WT</option>'
    + '<option value="WK1WM">WK1WM</option><option value="WK2WM">WK2WM</option></select></div>'
    + '<div class="col-6 col-md-2"><label class="form-label small mb-0" data-pg-ui-t="표시명">표시명</label><input type="text" id="hqStAddLabel" class="form-control form-control-sm" data-pg-ui-placeholder="예: D+12" placeholder="' + escUi(L('예: D+12')) + '"></div>'
    + '<div class="col-6 col-md-1"><label class="form-label small mb-0" data-pg-ui-t="순서">순서</label><input type="number" id="hqStAddSort" class="form-control form-control-sm" value="100"></div>'
    + '<div class="col-6 col-md-2"><label class="form-label small mb-0" data-pg-ui-t="사용">사용</label><select id="hqStAddActive" class="form-select form-select-sm"><option value="Y">Y</option><option value="N">N</option></select></div>'
    + '<div class="col-12 col-md-3"><label class="form-label small mb-0" data-pg-ui-t="설명">설명</label><input type="text" id="hqStAddDesc" class="form-control form-control-sm" data-pg-ui-placeholder="내부 안내용" placeholder="' + escUi(L('내부 안내용')) + '"></div>'
    + '<div class="col-6 col-md-2 d-grid"><button type="button" class="btn btn-primary btn-sm" id="hqStAddBtn" data-pg-ui-t="추가">' + escUi(L('추가')) + '</button></div>'
    + '<div class="col-12 col-md-auto d-grid align-self-end"><button type="button" class="btn btn-outline-secondary btn-sm" id="hqStSeedMissingBtn" data-pg-ui-title="내장 표준 코드가 DB에 없을 때만 삽입합니다" title="' + escUi(L('내장 표준 코드가 DB에 없을 때만 삽입합니다')) + '" data-pg-ui-t="표준주기 DB복원">' + escUi(L('표준주기 DB복원')) + '</button></div></div>'
    + pgUiParagraph('표준 주기(시스템) — 설명은 DB 행으로 덮어쓸 수 있습니다. DB가 비었을 때는 표준주기 DB복원으로 내장 목록과 동일한 행을 한 번에 넣을 수 있습니다.', 'small text-muted mb-1')
    + '<div class="table-responsive mb-4 table-no-col-resize-wrap"><table class="table table-sm table-bordered align-middle mb-0 table-no-col-resize"><thead class="table-light"><tr><th data-pg-ui-t="코드">코드</th><th data-pg-ui-t="설명">설명</th><th class="text-nowrap"  data-pg-ui-title="RT·T0 및 TM·TH(당일 누적 재집계)" title="' + escUi(L('RT·T0 및 TM·TH(당일 누적 재집계)')) + '" data-pg-ui-t="방식">방식</th><th data-pg-ui-t="순서">순서</th><th data-pg-ui-t="사용">사용</th><th class="text-end" data-pg-ui-t="자동가맹">자동가맹</th></tr></thead><tbody id="hqStBuiltTbody"></tbody></table></div>'
    + pgUiParagraph('DB 등록 주기 — 저장·삭제(본사·관리자만)', 'small text-muted mb-1')
    + '<div class="table-responsive table-no-col-resize-wrap"><table class="table table-sm table-bordered align-middle mb-0 table-no-col-resize"><thead class="table-light"><tr><th>ID</th><th data-pg-ui-t="코드">코드</th><th data-pg-ui-t="표시명">표시명</th><th data-pg-ui-t="설명">설명</th><th class="text-nowrap"  data-pg-ui-title="RT·T0 및 TM·TH(당일 누적 재집계)" title="' + escUi(L('RT·T0 및 TM·TH(당일 누적 재집계)')) + '" data-pg-ui-t="방식">방식</th><th data-pg-ui-t="순서">순서</th><th data-pg-ui-t="사용">사용</th><th class="text-end" style="width:9rem" data-pg-ui-t="작업">작업</th></tr></thead><tbody id="hqStExtraTbody"></tbody></table></div>'
    + '</div></div>'
    + '<div class="card mb-3">' + pgUiCardHeaderT('정산일정 미리보기') + '<div class="card-body">'
    + '<div class="row g-2 align-items-end mb-2">'
    + '<div class="col-6 col-md-2"><label class="form-label small mb-0" data-pg-ui-t="시작일">시작일</label><input type="date" lang="en-CA" id="hqStFrom" class="form-control form-control-sm pg-date-input-iso"></div>'
    + '<div class="col-6 col-md-2"><label class="form-label small mb-0" data-pg-ui-t="종료일">종료일</label><input type="date" lang="en-CA" id="hqStTo" class="form-control form-control-sm pg-date-input-iso"></div>'
    + '<div class="col-6 col-md-2 d-grid"><button type="button" class="btn btn-outline-primary btn-sm" id="hqStSchedBtn" data-pg-ui-t="조회">' + escUi(L('조회')) + '</button></div></div>'
    + '<div class="table-responsive table-no-col-resize-wrap"><table class="table table-sm table-bordered align-middle mb-0 table-no-col-resize"><thead class="table-light"><tr><th data-pg-ui-t="정산일">정산일</th><th data-pg-ui-t="주기">주기</th><th data-pg-ui-t="대상 from">대상 from</th><th data-pg-ui-t="대상 to">대상 to</th><th data-pg-ui-t="비고">비고</th><th class="text-end" data-pg-ui-t="자동가맹">자동가맹</th></tr></thead><tbody id="hqStSchedTbody"></tbody></table></div>'
    + pgUiParagraph('일중(M·H·TM·TH)는 당일 행·비고는 요약입니다. 상세는 서버 집계 규칙과 동일합니다.', 'small text-muted mb-0 mt-2')
    + '</div></div>'
    + '<div class="card mb-3"><div class="card-header fw-semibold d-flex flex-wrap align-items-center justify-content-between gap-2">'
    + '<span data-pg-ui-t="가맹 정산주기 변경 이력">' + escUi(L('가맹 정산주기 변경 이력')) + '</span>'
    + '<div class="d-flex flex-wrap align-items-end gap-2">'
    + '<div><label class="form-label small mb-0" for="hqStHistMerch" data-pg-ui-t="가맹점">가맹점</label>'
    + '<select id="hqStHistMerch" class="form-select form-select-sm" style="min-width:12rem;max-width:32rem">'
    + '<option value="">' + escUi(L('전체 (업체 미선택)')) + '</option></select></div>'
    + '<button type="button" class="btn btn-outline-secondary btn-sm" id="hqStHistBtn" data-pg-ui-t="조회">' + escUi(L('조회')) + '</button></div></div>'
    + '<div class="card-body p-0">'
    + '<div class="table-responsive table-no-col-resize-wrap"><table class="table table-sm table-bordered align-middle mb-0 table-no-col-resize">'
    + '<thead class="table-light"><tr><th class="text-nowrap" data-pg-ui-t="일시">일시</th><th class="text-nowrap" data-pg-ui-t="가맹점 (업체번호 / 이름)">가맹점 (업체번호 / 이름)</th><th data-pg-ui-t="이전">이전</th><th data-pg-ui-t="변경">변경</th><th data-pg-ui-t="방식">방식</th><th data-pg-ui-t="작업자">작업자</th><th data-pg-ui-t="비고">비고</th></tr></thead><tbody id="hqStHistTbody"><tr><td colspan="7" class="text-muted text-center">' + escUi(L('조회 중…')) + '</td></tr></tbody></table></div>'
    + '</div></div>'
    + '</div>';
  }
  /** 본사설정 — 환수/미수금 (본사 기본 무효·환불·미수금 + 총판·가맹) — getScreenHtml 呼び出し時に L 適用 */
  function hqReceivableRecoveryStaticHtml() {
    return ''
    + '<div class="hq-receivable-recovery p-2">'
    + '<div class="card mb-3" id="hqStVoidRefundModesCard"><div class="card-header fw-semibold">' + pgUiSpanText('무효·환불 정산 방식 (본사 기본)') + '</div><div class="card-body">'
    + pgUiParagraph('거래 21·40 무효, 22·41 수동무효, 30·42 환불·자동환불, 31 강제환불 각각에 대해 순매출·이중 과금(성공 건당·%) 방식을 둡니다. 일반형은 승인 시 성공 수수료만, 무효·환불 건에는 무효/환불 건당만 과금합니다. 수익형은 순매출 미차감·무효·환불에도 성공 수수료를 다시 붙입니다. 하이브리드1·2는 무효 계열과 환불 계열을 나눕니다. 31 강제환불만 차지백 수수료(구간·건당)가 부과됩니다.')
    + '<div class="row g-2 align-items-end">'
    + '<div class="col-12 col-md-6"><label class="form-label small mb-0" for="hqStModeVoid" data-pg-ui-t="무효 (21·40)">무효 (21·40)</label><select id="hqStModeVoid" class="form-select form-select-sm"></select></div>'
    + '<div class="col-12 col-md-6"><label class="form-label small mb-0" for="hqStModeManualVoid" data-pg-ui-t="수동무효 (22·41)">수동무효 (22·41)</label><select id="hqStModeManualVoid" class="form-select form-select-sm"></select></div>'
    + '<div class="col-12 col-md-6"><label class="form-label small mb-0" for="hqStModeRefund" data-pg-ui-t="환불 (30·42)">환불 (30·42)</label><select id="hqStModeRefund" class="form-select form-select-sm"></select></div>'
    + '<div class="col-12 col-md-6"><label class="form-label small mb-0" for="hqStModeForceRefund" data-pg-ui-t="강제환불 (31)">강제환불 (31)</label><select id="hqStModeForceRefund" class="form-select form-select-sm"></select></div>'
    + '<div class="col-12"><button type="button" class="btn btn-primary btn-sm" id="hqStVoidRefundSaveBtn">' + pgUiSpanText('저장') + '</button></div></div></div></div>'
    + '<div class="card mb-3" id="hqVoidRefundMdCard"><div class="card-header fw-semibold">' + pgUiSpanText('무효·환불 정산 방식 (총판별)') + '</div><div class="card-body">'
    + pgUiParagraph('총판(MASTER_DIST)마다 무효·수동무효·환불·강제환불 정산 방식을 둡니다. 비우면 본사 기본과 동일합니다. 가맹 「총판·본사 따름」이면 총판 값(없으면 본사)을 따르고, 가맹에서 모드를 고르면 가맹이 우선합니다.')
    + '<div class="row g-2 align-items-end mb-2">'
    + '<div class="col-12 col-md-5"><label class="form-label small mb-0" data-pg-ui-t="총판">총판</label><select id="hqVoidRefundMdSel" class="form-select form-select-sm"><option value="" data-pg-ui-t="선택…">' + escUi(L('선택…')) + '</option></select></div></div>'
    + '<div class="row g-2 align-items-end">'
    + '<div class="col-12 col-md-6"><label class="form-label small mb-0" for="hqVoidMdModeVoid" data-pg-ui-t="무효 (21·40)">무효 (21·40)</label><select id="hqVoidMdModeVoid" class="form-select form-select-sm"></select></div>'
    + '<div class="col-12 col-md-6"><label class="form-label small mb-0" for="hqVoidMdModeManualVoid" data-pg-ui-t="수동무효 (22·41)">수동무효 (22·41)</label><select id="hqVoidMdModeManualVoid" class="form-select form-select-sm"></select></div>'
    + '<div class="col-12 col-md-6"><label class="form-label small mb-0" for="hqVoidMdModeRefund" data-pg-ui-t="환불 (30·42)">환불 (30·42)</label><select id="hqVoidMdModeRefund" class="form-select form-select-sm"></select></div>'
    + '<div class="col-12 col-md-6"><label class="form-label small mb-0" for="hqVoidMdModeForceRefund" data-pg-ui-t="강제환불 (31)">강제환불 (31)</label><select id="hqVoidMdModeForceRefund" class="form-select form-select-sm"></select></div>'
    + '<div class="col-12"><button type="button" class="btn btn-primary btn-sm" id="hqVoidRefundMdSaveBtn">' + pgUiSpanText('총판 저장') + '</button></div></div></div></div>'
    + '<div class="card mb-3" id="hqStReceivableCard"><div class="card-header fw-semibold">' + pgUiSpanText('미수금관리설정 (본사 기본)') + '</div><div class="card-body">'
    + pgUiParagraph('자동이면 미수금이 생긴 뒤 다음 정산 실행에서 지급액에 FIFO로 반영됩니다. 수동이면 다음 정산에 자동 반영하지 않고 잔액이 쌓이며, 미수금관리 화면에서 환수처리를 누른 건만 차기 정산에서 차감됩니다. 저장 시 아래 체크를 켜면 모든 가맹 tb_settlement_setting.receivable_recovery_mode도 같은 값으로 갱신됩니다(개별 오버라이드가 아닌 가맹만; 아래 「가맹」에서 가맹별로 다시 조정 가능).')
    + '<div class="row g-2 align-items-end">'
    + '<div class="col-12 col-md-5"><label class="form-label small mb-0" for="hqStReceivableMode" data-pg-ui-t="미수금처리 방식">미수금처리 방식</label>'
    + '<select id="hqStReceivableMode" class="form-select form-select-sm"><option value="AUTO" data-pg-ui-t="자동">' + escUi(L('자동')) + '</option><option value="MANUAL" data-pg-ui-t="수동">' + escUi(L('수동')) + '</option></select></div>'
    + '<div class="col-12 col-md-7 d-flex align-items-end"><div class="form-check mb-1">'
    + '<input class="form-check-input" type="checkbox" id="hqStReceivableSyncAll" checked>'
    + '<label class="form-check-label small" for="hqStReceivableSyncAll" data-pg-ui-t="모든 가맹 정산설정에 동일 적용">모든 가맹 정산설정에 동일 적용</label></div></div>'
    + '<div class="col-12 d-flex flex-wrap align-items-center gap-2">'
    + '<button type="button" class="btn btn-primary btn-sm" id="hqStReceivableSaveBtn">' + pgUiSpanText('저장') + '</button>'
    + '<button type="button" class="btn btn-link btn-sm p-0" id="hqStReceivableToUnpaid">' + pgUiSpanText('미수금관리(수동 환수처리)로 이동') + '</button></div></div></div></div>'
    + '<div class="card mb-3"><div class="card-header fw-semibold">' + pgUiSpanText('환수 / 미수금 설정') + '</div><div class="card-body small">'
    + pgUiParagraph('총판(MASTER_DIST)마다 자동/수동을 두고, 소속 가맹은 기본으로 그 값을 따릅니다. 특정 가맹만 개별로 바꾸면 가맹 설정이 우선합니다. 수동이면 「미수금관리」에서 환수처리 요청 건만 다음 정산 마감 시 차감되고, 자동이면 정산 시 FIFO로 차감합니다. 위 「미수금관리설정 (본사 기본)」이 총판·가맹 상속의 출발값이 됩니다(가맹 개별 오버라이드 제외).', 'text-muted mb-3')
    + '<p class="small fw-semibold mb-1" data-pg-ui-t="총판 — 소속 가맹 기본">총판 — 소속 가맹 기본</p>'
    + '<div class="row g-2 align-items-end mb-3">'
    + '<div class="col-12 col-md-5"><label class="form-label small mb-0" data-pg-ui-t="총판">총판</label><select id="hqRecvMdSel" class="form-select form-select-sm"><option value="" data-pg-ui-t="선택…">' + escUi(L('선택…')) + '</option></select></div>'
    + '<div class="col-6 col-md-2"><label class="form-label small mb-0" data-pg-ui-t="모드">모드</label><select id="hqRecvMdModeSel" class="form-select form-select-sm">'
    + '<option value="AUTO" data-pg-ui-t="자동">' + escUi(L('자동')) + '</option><option value="MANUAL" data-pg-ui-t="수동">' + escUi(L('수동')) + '</option></select></div>'
    + '<div class="col-auto d-grid"><button type="button" class="btn btn-primary btn-sm" id="hqRecvMdSaveBtn">' + pgUiSpanText('총판 저장') + '</button></div></div>'
    + '<p class="small fw-semibold mb-1" data-pg-ui-t="가맹 — 총판과 동일 또는 개별">가맹 — 총판과 동일 또는 개별</p>'
    + '<div class="row g-2 align-items-end mb-2">'
    + '<div class="col-12 col-md-5"><label class="form-label small mb-0" data-pg-ui-t="가맹점">가맹점</label><select id="hqRecvMerchSel" class="form-select form-select-sm"><option value="" data-pg-ui-t="선택…">' + escUi(L('선택…')) + '</option></select></div>'
    + '<div class="col-12 col-md-4 d-flex align-items-center pt-md-4"><div class="form-check mb-0">'
    + '<input class="form-check-input" type="checkbox" id="hqRecvInheritChk" checked>'
    + '<label class="form-check-label" for="hqRecvInheritChk" data-pg-ui-t="총판·본사 설정 따름">총판·본사 설정 따름</label></div></div>'
    + '<div class="col-6 col-md-2"><label class="form-label small mb-0" data-pg-ui-t="개별 모드">개별 모드</label><select id="hqRecvModeSel" class="form-select form-select-sm" disabled>'
    + '<option value="AUTO" data-pg-ui-t="자동">' + escUi(L('자동')) + '</option><option value="MANUAL" data-pg-ui-t="수동">' + escUi(L('수동')) + '</option></select></div>'
    + '<div class="col-auto d-grid"><button type="button" class="btn btn-primary btn-sm" id="hqRecvSaveBtn">' + pgUiSpanText('가맹 저장') + '</button></div></div>'
    + '<p class="text-muted small mb-3" id="hqRecvMerchHint"><span data-pg-ui-t="가맹을 선택하면 유효 모드·상속 출처가 여기에 표시됩니다.">' + escUi(L('가맹을 선택하면 유효 모드·상속 출처가 여기에 표시됩니다.')) + '</span></p>'
    + '<p class="small fw-semibold mb-1" data-pg-ui-t="유효 모드가 수동인 가맹">유효 모드가 수동인 가맹</p>'
    + '<div class="table-responsive"><table class="table table-sm table-bordered align-middle mb-0">'
    + '<thead class="table-light"><tr><th data-pg-ui-t="업체코드">업체코드</th><th data-pg-ui-t="업체명">업체명</th><th data-pg-ui-t="모드">모드</th><th data-pg-ui-t="출처">출처</th></tr></thead><tbody id="hqRecvManualTbody"></tbody></table></div>'
    + '</div></div>';
  }
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
    { v: 'AUTO_NO_MANUAL', t: '자동(수동불가)' },
    { v: 'ARBITRARY', t: '임의출금' },
    { v: 'NONE', t: '사용안함' }
  ];
  /** 가맹점·본사 출금제한 유형(저장값은 tb_settlement_setting.withdraw_restrict_type 또는 본사 regional JSON) */
  var WITHDRAW_POLICY_OPTIONS = [
    { v: '', t: '선택' },
    { v: 'DAILY', t: '매일' },
    { v: 'HOLIDAY', t: '공휴일' },
    { v: 'EVE_HOLIDAY_17', t: '공휴일 전날 17시 이후' },
    { v: 'EVE_HOLIDAY_18', t: '공휴일 전날 18시 이후' },
    { v: 'NONE', t: '미사용' }
  ];
  var CALC_METHOD_MERCHANT_NOTICE = '총판이 허용·대표 주기를 쓰면 가맹 정산주기 셀렉트가 그 범위로 바뀝니다. '
    + '정산안함: 배치 적립 없음. '
    + 'RT·건별 / T0·TM·TH·당일합산 / M5·M10·M30·분마다 / H1~H12·시간마다 / D·W·WK·실행마다 1건. '
    + 'D0 자동: 당일 23:50까지(총판별 정산 크론 기준 Zone). '
    + '이체및송금: 수동·자동·자동(수동불가)·임의출금·사용안함. 이체주기(분)는 자동 계열만. '
    + '지급보류: 정산은 진행, 출금만 제한. 정산제외: D0 등 휴일 제외 등(세부는 설정 화면).';

  /** 본사 영업일·휴일: 연간 미니달력 + 공휴일 프리셋 (hq-holiday-calendar.js) — L()는 화면 렌더 시점에 평가 */
  function hqHolidayUiHtml() {
    return '<div class="col-12"><div class="hq-holiday-ui-wrap border rounded p-2 bg-light mt-1" data-hq-calendar-readonly="true">' +
      '<div class="d-flex flex-wrap align-items-center gap-2 mb-2">' +
      '<label class="small mb-0 text-nowrap" data-pg-ui-t="연도">' + escUi(L('연도')) + '</label><select class="form-select form-select-sm hq-holiday-year" style="width:auto;min-width:5rem"></select>' +
      '<button type="button" class="btn btn-sm btn-outline-primary hq-holiday-load-presets" data-pg-ui-t="공휴일 프리셋 불러오기">' + escUi(L('공휴일 프리셋 불러오기')) + '</button>' +
      '<button type="button" class="btn btn-sm btn-outline-secondary hq-holiday-refresh" data-pg-ui-t="달력 동기화">' + escUi(L('달력 동기화')) + '</button>' +
      '<button type="button" class="btn btn-sm btn-outline-secondary" id="hqBizdayProfileNewBtn" data-pg-ui-t="신규">' + escUi(L('신규')) + '</button>' +
      '<button type="button" class="btn btn-sm btn-primary" id="hqBizdayProfileSaveBtn" data-pg-ui-t="저장">' + escUi(L('저장')) + '</button></div>' +
      '<p class="text-muted small mb-2" data-pg-ui-t="날짜를 클릭하면 비영업일에서 추가/제거됩니다. [공휴일 프리셋 불러오기]는 기준국가에 따라 병합합니다. KR/US/JP/TH/CN은 연도별 법정·공지 연휴, GLOBAL은 해당 연도 토·일만 포함합니다.">' + escUi(L('날짜를 클릭하면 비영업일에서 추가/제거됩니다. [공휴일 프리셋 불러오기]는 기준국가에 따라 병합합니다. KR/US/JP/TH/CN은 연도별 법정·공지 연휴, GLOBAL은 해당 연도 토·일만 포함합니다.')) + '</p>' +
      '<div class="hq-holiday-calendar-grid"></div></div></div>';
  }

  /** 본사 영업일·휴일: 기간형 추가 목록(언제부터~언제까지/내용/추가일/작성자) */
  var REGIONAL_BIZDAY_RANGE_UI_HTML = '<div class="col-12"><div class="border rounded p-2 bg-light mt-1">' +
    '<div class="d-flex flex-wrap align-items-end gap-2 mb-2">' +
    '<div><label class="form-label mb-1" data-pg-ui-t="언제부터">' + escUi(L('언제부터')) + '</label><input type="date" lang="en-CA" class="form-control form-control-sm pg-date-input-iso" id="bizHolidayFromDate"></div>' +
    '<div><label class="form-label mb-1" data-pg-ui-t="언제까지">' + escUi(L('언제까지')) + '</label><input type="date" lang="en-CA" class="form-control form-control-sm pg-date-input-iso" id="bizHolidayToDate"></div>' +
    '<div style="min-width:220px"><label class="form-label mb-1" data-pg-ui-t="내용">' + escUi(L('내용')) + '</label><input type="text" class="form-control form-control-sm" id="bizHolidayReason" data-pg-ui-placeholder="예: 설 연휴" placeholder="' + escUi(L('예: 설 연휴')) + '"></div>' +
    '<div><label class="form-label mb-1" data-pg-ui-t="작성자">' + escUi(L('작성자')) + '</label><input type="text" class="form-control form-control-sm" id="bizHolidayWriter" data-pg-ui-placeholder="작성자" placeholder="' + escUi(L('작성자')) + '"></div>' +
    '<div><button type="button" class="btn btn-sm btn-primary" id="bizHolidayAddBtn" data-pg-ui-t="추가">' + escUi(L('추가')) + '</button></div>' +
    '</div>' +
    '<div class="table-responsive"><table class="table table-sm table-bordered mb-0"><thead><tr><th style="width:120px" data-pg-ui-t="언제부터">' + escUi(L('언제부터')) + '</th><th style="width:120px" data-pg-ui-t="언제까지">' + escUi(L('언제까지')) + '</th><th data-pg-ui-t="내용">' + escUi(L('내용')) + '</th><th style="width:130px" data-pg-ui-t="추가한날짜">' + escUi(L('추가한날짜')) + '</th><th style="width:120px" data-pg-ui-t="작성자">' + escUi(L('작성자')) + '</th><th style="width:170px" data-pg-ui-t="처리">' + escUi(L('처리')) + '</th></tr></thead>' +
    '<tbody id="bizHolidayRangeTbody"><tr><td colspan="6" class="text-muted text-center" data-pg-ui-t="추가된 기간이 없습니다.">' + escUi(L('추가된 기간이 없습니다.')) + '</td></tr></tbody></table></div>' +
    '<input type="hidden" name="businessHolidayRangesJson" id="businessHolidayRangesJson">' +
    '</div></div>';

  /** 본사설정 > 영업일설정: 등록된 설정 목록 — 구분 옵션 value는 API/저장 호환으로 한국어 키 유지, 표시만 L() */
  var HQ_BIZDAY_KIND_OPTIONS = ['공휴일', '국경일', '기념일', '종교휴일', '임시공휴일', '대체공휴일'];
  function hqBizdayKindOptionsHtml() {
    return HQ_BIZDAY_KIND_OPTIONS.map(function (k) {
      return '<option value="' + escUi(k) + '" data-pg-ui-t="' + escUi(k) + '">' + escUi(L(k)) + '</option>';
    }).join('');
  }
  function hqBizdayManualUiHtml() {
    return '<div class="col-12"><div class="border rounded p-2 bg-light mt-1 mb-2">' +
      '<input type="hidden" name="businessHolidayExtraDates" id="hqBizdayExtraDatesHidden">' +
      '<input type="hidden" name="holidayManualEntriesJson" id="hqBizdayManualEntriesJson">' +
      '<strong class="small d-block mb-2" data-pg-ui-t="휴일·비영업일 구간 등록">' + escUi(L('휴일·비영업일 구간 등록')) + '</strong>' +
      pgUiParagraph('시작·종료일·구분·내용을 입력한 뒤 [구간 추가]로 넣거나, 목록의 [수정]으로 불러온 뒤 [수정 반영]으로 바꿉니다. [삭제]로 행을 제거할 수 있습니다. 하단 달력에 반영됩니다.', 'text-muted small mb-2') +
      '<div class="row g-2 align-items-end mb-2">' +
      '<div class="col-sm-6 col-md-2"><label class="form-label mb-1 small" data-pg-ui-t="시작일">' + escUi(L('시작일')) + '</label><input type="date" lang="en-CA" class="form-control form-control-sm pg-date-input-iso" id="hqBizdayRangeFrom" data-pg-ui-title="연도-월-일"></div>' +
      '<div class="col-sm-6 col-md-2"><label class="form-label mb-1 small" data-pg-ui-t="종료일">' + escUi(L('종료일')) + '</label><input type="date" lang="en-CA" class="form-control form-control-sm pg-date-input-iso" id="hqBizdayRangeTo" data-pg-ui-title="연도-월-일"></div>' +
      '<div class="col-sm-6 col-md-2"><label class="form-label mb-1 small" data-pg-ui-t="일자 구분">' + escUi(L('일자 구분')) + '</label><select class="form-select form-select-sm" id="hqBizdayRangeKind">' +
      hqBizdayKindOptionsHtml() +
      '</select></div>' +
      '<div class="col-sm-12 col-md-3"><label class="form-label mb-1 small" data-pg-ui-t="내용">' + escUi(L('내용')) + '</label><input type="text" class="form-control form-control-sm" id="hqBizdayRangeNote" data-pg-ui-placeholder="예: 설날 연휴" placeholder="' + escUi(L('예: 설날 연휴')) + '"></div>' +
      '<div class="col-sm-12 col-md-3"><label class="form-label mb-1 small d-block">&nbsp;</label><div class="d-flex flex-wrap gap-1 align-items-center">' +
      '<button type="button" class="btn btn-sm btn-primary" id="hqBizdayRangeAddBtn" data-pg-ui-t="구간 추가">' + escUi(L('구간 추가')) + '</button>' +
      '<button type="button" class="btn btn-sm btn-outline-secondary d-none" id="hqBizdayRangeCancelEditBtn" data-pg-ui-t="편집 취소">' + escUi(L('편집 취소')) + '</button></div></div></div>' +
      '<div class="table-responsive"><table class="table table-sm table-bordered mb-0"><thead><tr><th style="width:110px" data-pg-ui-t="시작">' + escUi(L('시작')) + '</th><th style="width:110px" data-pg-ui-t="종료">' + escUi(L('종료')) + '</th><th style="width:120px" data-pg-ui-t="구분">' + escUi(L('구분')) + '</th><th data-pg-ui-t="내용">' + escUi(L('내용')) + '</th><th style="width:72px" data-pg-ui-t="수정">' + escUi(L('수정')) + '</th><th style="width:72px" data-pg-ui-t="삭제">' + escUi(L('삭제')) + '</th></tr></thead>' +
      '<tbody id="hqBizdayManualTbody"><tr class="hq-bizday-manual-empty"><td colspan="6" class="text-center text-muted" data-pg-ui-t="등록된 구간이 없습니다.">' + escUi(L('등록된 구간이 없습니다.')) + '</td></tr></tbody></table></div></div></div>';
  }

  function hqBizdayProfileListHtml() {
    return '<div class="col-12"><div class="border rounded p-2 bg-light mt-1">' +
      '<div class="d-flex justify-content-between align-items-center mb-2"><strong data-pg-ui-t="저장된 영업일 설정 목록">' + escUi(L('저장된 영업일 설정 목록')) + '</strong><small class="text-muted" data-pg-ui-t="행의 [수정]으로 불러오거나, 데이터 열을 눌러 선택할 수 있습니다.">' + escUi(L('행의 [수정]으로 불러오거나, 데이터 열을 눌러 선택할 수 있습니다.')) + '</small></div>' +
      '<div class="table-responsive"><table class="table table-sm table-bordered mb-0"><thead><tr><th style="width:48px" data-pg-ui-t="번호">' + escUi(L('번호')) + '</th><th style="width:160px" data-pg-ui-t="이름">' + escUi(L('이름')) + '</th><th style="width:72px" data-pg-ui-t="기준국가">' + escUi(L('기준국가')) + '</th><th style="width:100px" data-pg-ui-t="등록자">' + escUi(L('등록자')) + '</th>' +
      '<th class="text-center align-middle" style="width:88px" data-pg-ui-title="저장된 비영업일 중 토·일·기준국가 법정(프리셋) 공휴일에 해당하는 일수." title="' + escUi(L('저장된 비영업일 중 토·일·기준국가 법정(프리셋) 공휴일에 해당하는 일수.')) + '" data-pg-ui-t="공식공휴일">' + escUi(L('공식공휴일')) + '</th>' +
      '<th class="text-center align-middle" style="width:88px" data-pg-ui-title="저장된 비영업일 중 위 공식에 해당하지 않는 일수(추가 지정 평일 등)." title="' + escUi(L('저장된 비영업일 중 위 공식에 해당하지 않는 일수(추가 지정 평일 등).')) + '" data-pg-ui-t="추가공휴일">' + escUi(L('추가공휴일')) + '</th>' +
      '<th class="text-center align-middle" style="width:80px" data-pg-ui-title="저장된 비영업 일자 수(중복 1회). 공식+추가와 일치." title="' + escUi(L('저장된 비영업 일자 수(중복 1회). 공식+추가와 일치.')) + '" data-pg-ui-t="총공휴일">' + escUi(L('총공휴일')) + '</th>' +
      '<th style="width:100px" data-pg-ui-t="작성일">' + escUi(L('작성일')) + '</th><th style="width:100px" data-pg-ui-t="수정일">' + escUi(L('수정일')) + '</th>' +
      '<th class="text-center align-middle" style="width:100px" data-pg-ui-t="총본사 기준">' + escUi(L('총본사 기준')) + '</th>' +
      '<th class="text-center" style="width:76px" data-pg-ui-t="수정">' + escUi(L('수정')) + '</th><th class="text-center" style="width:76px" data-pg-ui-t="삭제">' + escUi(L('삭제')) + '</th></tr></thead>' +
      '<tbody id="hqBizdayProfileTbody"><tr><td colspan="12" class="text-center text-muted" data-pg-ui-t="저장된 설정이 없습니다.">' + escUi(L('저장된 설정이 없습니다.')) + '</td></tr></tbody></table></div>' +
      '</div></div>';
  }

  /** 배포설정 메뉴 — JPAY·가맹점 API 중계 진행안(정적 문서). 구현 시 이 본문을 참고·갱신합니다. */
  var DEPLOY_STATIC_HTML = {
    integrationPlan: '<div class="deploy-static-doc text-muted small">' +
      '<h5 class="text-dark fw-semibold mb-3">PG 중계·미들웨어 연동 — 총괄 진행안</h5>' +
      '<p class="mb-2"><strong class="text-body">목표</strong> · PG사와는 1:1(단일 연동 자격), 가맹점에는 1:N으로 우리 API를 제공하고, 결제·3DS·노티 결과를 <strong class="text-body">우리 미들웨어에 적재</strong>한 뒤 가맹점에 통지하는 구조입니다. <strong class="text-body">JPAY(JPY·3DS 계열)를 1차 범위</strong>로 두고 단계적으로 확장합니다.</p>' +
      '<ul class="mb-3 ps-3">' +
      '<li class="mb-1"><strong class="text-body">본사설정과의 구분</strong> · 수수료·노티 URL·도메인·권한 등 <em>전사 운영 설정</em>은 <strong class="text-body">본사설정</strong>에 두고, <strong class="text-body">PG API 연동·배포 자격·중계 출시·JPAY·가맹점 API 문서·체크리스트</strong>는 이 <strong class="text-body">배포설정</strong>에서 관리합니다.</li>' +
      '<li class="mb-1"><strong class="text-body">노티</strong> · PG → 우리 <code>/api/open/pg-notify/…</code> 수신·<code>pg_trnsctn</code> 적재는 기존 파이프를 활용합니다. 가맹점으로의 아웃바운드 노티는 별도 설계(콜백 URL·서명·재시도)로 추가합니다.</li>' +
      '<li class="mb-1"><strong class="text-body">가맹점 분기</strong> · MID+루트만이 아니라 <strong class="text-body">등록 업체코드 + 결제대행사 MID</strong> 조합으로 바인딩을 찾는 방향(구현 단계에서 수신 본문·바인딩 스키마와 맞춤).</li>' +
      '</ul>' +
      '<h6 class="text-body fw-semibold mt-3">권장 단계</h6>' +
      '<ol class="ps-3 mb-0">' +
      '<li class="mb-1">사전: JPAY 연동 문서·샌드·계약 범위 확정</li>' +
      '<li class="mb-1">인바운드: JPAY 노티 필드 매핑(<strong class="text-body">노티매핑설정</strong> 벤더 JPAY) 및 적재 로직</li>' +
      '<li class="mb-1">아웃바운드: 가맹점 API(결제 세션·3DS 리턴) + 가맹점 웹훅</li>' +
      '<li class="mb-0">운영: 모니터링·키 로테이션·이 화면 체크리스트 점검</li>' +
      '</ol></div>',
    jpayWorkPlan: '<div class="deploy-static-doc text-muted small">' +
      '<h5 class="text-dark fw-semibold mb-3">JPAY 우선 — 단계별 작업 계획</h5>' +
      '<p class="mb-2">아래는 구현 시 작업 분해 예시입니다. 실제 일정·담당은 제이페이 제공 스펙에 맞춰 조정합니다.</p>' +
      '<table class="table table-sm table-bordered bg-white">' +
      '<thead><tr><th style="width:6rem" data-pg-ui-t="단계">단계</th><th data-pg-ui-t="내용">내용</th></tr></thead><tbody>' +
      '<tr><td class="text-nowrap">P0</td><td>JPAY API·3DS·노티 필드 정의서·샌드 MID/키 수령</td></tr>' +
      '<tr><td>P1</td><td><code>tb_pg_agency</code> 등에 JPAY 등록, 가맹점 <code>tb_merchant_pg_binding</code> (업체코드·MID) 검증</td></tr>' +
      '<tr><td>P2</td><td>노티매핑 JSON 벤더 <code>JPAY</code> — CALLBACK/RESULT 필드 매핑·표시값(displayMaps)</td></tr>' +
      '<tr><td>P3</td><td>수신 적재: 기존 ChillPay 경로와 병행할 JPAY 전용 파서/분기 또는 매핑 우선 적용</td></tr>' +
      '<tr><td>P4</td><td>가맹점→우리→JPAY 결제 세션·3DS 리턴 URL(우리 도메인) 연동</td></tr>' +
      '<tr><td>P5</td><td>UAT → 운영 전환, 본 메뉴 <strong class="text-body">배포 체크리스트</strong> 완료</td></tr>' +
      '</tbody></table>' +
      '<p class="mt-3 mb-0 small">참고: 기본 노티매핑 템플릿에 JPAY 벤더 슬롯이 이미 포함되어 있습니다. 필드 매핑은 스펙 확정 후 UI에서 채웁니다.</p></div>',
    merchantApiPolicy: '<div class="deploy-static-doc text-muted small">' +
      '<h5 class="text-dark fw-semibold mb-3">가맹점 API 배포 — 정책 요약</h5>' +
      '<div class="mb-3">' +
      '<button type="button" class="btn btn-primary btn-sm" id="merchantPolicyOpenKitBtn">가맹점 API 연동키트 화면 열기</button>' +
      '<span class="small text-secondary ms-2">배포설정 메뉴와 동일 화면입니다. 메뉴에 안 보이면 이 버튼으로 이동하세요.</span></div>' +
      '<ul class="ps-3 mb-3">' +
      '<li class="mb-1"><strong class="text-body">식별</strong> · 가맹점별 <strong class="text-body">업체코드</strong> + 발급 <strong class="text-body">API Key</strong> + 요청 무결성용 <strong class="text-body">비밀키(HMAC)</strong> (필요 시 암호화용 키·IV 별도)</li>' +
      '<li class="mb-1"><strong class="text-body">환경</strong> · 운영/스테이징 URL·키 분리, 키 분실 시 폐기·재발급</li>' +
      '<li class="mb-1"><strong class="text-body">콜백</strong> · 가맹점 HTTPS URL 등록, 우리→가맹점 노티 서명·재시도 정책</li>' +
      '<li class="mb-1"><strong class="text-body">문서</strong> · OpenAPI·샘플·오류 코드 — 배포 시 브로커 베이스(<code>/api/middleware/v1/pg/{pg}</code>) 고정</li>' +
      '</ul>' +
      '<p class="mb-0 small text-secondary">실제 키 발급·엔드포인트·시크릿 강제는 배포설정 <strong class="text-body">「가맹점 API 생성」</strong>(<code>/hq/merchantApiGenerate</code>) 화면에서 수행합니다.</p></div>',
    launchChecklist: '<div class="deploy-static-doc text-muted small">' +
      '<h5 class="text-dark fw-semibold mb-3">배포·운영 체크리스트</h5>' +
      '<ul class="list-unstyled mb-0">' +
      '<li class="mb-2"><i class="bi bi-check2-square me-1 text-secondary"></i> API·관리자 <strong class="text-body">HTTPS</strong>·공개 URL 베이스 설정</li>' +
      '<li class="mb-2"><i class="bi bi-check2-square me-1 text-secondary"></i> 전산 노티 URL이 PG/미들웨어에 등록됨 (<code>ingressToken</code> 일치)</li>' +
      '<li class="mb-2"><i class="bi bi-check2-square me-1 text-secondary"></i> <strong class="text-body">IP 허용·HMAC</strong> 등 <code>app.pg-notify</code> 운영값 반영</li>' +
      '<li class="mb-2"><i class="bi bi-check2-square me-1 text-secondary"></i> JPAY 샌드에서 승인·취소·무효·3DS 시나리오 검증</li>' +
      '<li class="mb-2"><i class="bi bi-check2-square me-1 text-secondary"></i> 결제내역·노티수령 로그로 <strong class="text-body">적재·분기</strong> 확인</li>' +
      '<li class="mb-0"><i class="bi bi-check2-square me-1 text-secondary"></i> 가맹점 콜백·장애 알림·로그 보존 정책</li>' +
      '</ul></div>'
  };

  /** 운영관리: 허브 안내 + 하위 메뉴에서 PG 연동·배포 정적 문서(배포설정과 동일 본문) */
  var OPS_MANAGEMENT_PLACEHOLDER_HTML = '<div class="ops-admin-placeholder text-muted small">' +
    '<h5 class="text-dark fw-semibold mb-3" data-pg-ui-t="운영관리">' + escUi(L('운영관리')) + '</h5>' +
    '<p class="mb-2" data-pg-ui-t="운영관리 그룹입니다. 운영 배치·점검·장애 대응 등 전용 화면을 여기에 둘 수 있습니다.">' +
    escUi(L('운영관리 그룹입니다. 운영 배치·점검·장애 대응 등 전용 화면을 여기에 둘 수 있습니다.')) + '</p>' +
    '<p class="mb-3" data-pg-ui-t="현재는 메뉴만 제공하며, 세부 기능은 이후 버전에서 연동합니다.">' +
    escUi(L('현재는 메뉴만 제공하며, 세부 기능은 이후 버전에서 연동합니다.')) + '</p>' +
    '<hr class="my-3" />' +
    '<h6 class="text-body fw-semibold mb-2" data-pg-ui-t="PG 연동·배포 참고 문서">' + escUi(L('PG 연동·배포 참고 문서')) + '</h6>' +
    '<p class="mb-0" data-pg-ui-t="아래 하위 메뉴는 배포설정의 동명 화면과 내용이 같습니다. JPAY 연동·가맹점 API·체크리스트 점검 시 활용하세요.">' +
    escUi(L('아래 하위 메뉴는 배포설정의 동명 화면과 내용이 같습니다. JPAY 연동·가맹점 API·체크리스트 점검 시 활용하세요.')) + '</p></div>';

  /** 운영관리 — 비활성카드 수동 등록 패널(목록 상단 고정) */
  var OPS_INACTIVE_CARD_REGISTER_HTML = '<div class="card mb-3 border-primary-subtle" id="opsInactiveCardRegCard">' +
    '<div class="card-body py-3">' +
    '<div class="fw-semibold mb-2" data-pg-ui-t="비활성 카드 등록">비활성 카드 등록</div>' +
    '<p class="small text-muted mb-2" data-pg-ui-t="카드 종류를 선택한 뒤 카드번호·사유를 입력하고 [등록]을 누르세요. 등록일시·등록자는 자동 저장됩니다.">' +
    escUi(L('카드 종류를 선택한 뒤 카드번호·사유를 입력하고 [등록]을 누르세요. 등록일시·등록자는 자동 저장됩니다.')) + '</p>' +
    '<p class="small text-warning mb-2 d-none" id="opsIcRegPermHint" data-pg-ui-t="본사권한설정에서 이 화면에 삭제(전체) 또는 수정 권한이 있어야 등록·해지할 수 있습니다.">' +
    escUi(L('본사권한설정에서 이 화면에 삭제(전체) 또는 수정 권한이 있어야 등록·해지할 수 있습니다.')) + '</p>' +
    '<div class="d-flex flex-wrap gap-2 align-items-end">' +
    '<div><label class="form-label small mb-0" data-pg-ui-t="PG">PG</label>' +
    '<select class="form-select form-select-sm" id="opsIcRegPg" style="min-width:7rem">' +
    '<option value="" data-pg-ui-t="전체 PG">전체 PG</option><option value="JPAY">JPAY</option><option value="CHILLPAY">ChillPay</option></select></div>' +
    '<div><label class="form-label small mb-0" data-pg-ui-t="카드 종류">카드 종류</label>' +
    '<select class="form-select form-select-sm" id="opsIcRegBrand" style="min-width:8.5rem">' +
    pgUiOptHtml([
      { v: '', t: '선택' },
      { v: 'VISA', t: 'Visa' },
      { v: 'MASTERCARD', t: 'Mastercard' },
      { v: 'AMEX', t: 'American Express' },
      { v: 'DINERS', t: 'Diners Club' },
      { v: 'JCB', t: 'JCB' },
      { v: 'DISCOVER', t: 'Discover' },
      { v: 'UNIONPAY', t: 'UnionPay' },
      { v: 'DOMESTIC_KR', t: '국내 전용(9)' },
      { v: 'OTHER', t: '기타' }
    ]) + '</select></div>' +
    '<div style="min-width:16rem"><label class="form-label small mb-0" data-pg-ui-t="카드번호">카드번호</label>' +
    '<p class="small text-muted mb-1" id="opsIcRegPanPickHint" data-pg-ui-t="카드 종류를 먼저 선택하세요.">' +
    escUi(L('카드 종류를 먼저 선택하세요.')) + '</p>' +
    '<div id="opsIcRegPanWarn" class="alert alert-warning py-1 px-2 small mb-1 d-none" role="alert"></div>' +
    '<div id="opsIcRegPan16" class="d-none ops-ic-pan-seg-row d-flex flex-wrap gap-1 align-items-center">' +
    '<input type="text" class="form-control form-control-sm font-monospace ops-ic-pan-seg text-center" style="width:3.25rem" maxlength="4" inputmode="numeric" autocomplete="off" disabled>' +
    '<input type="text" class="form-control form-control-sm font-monospace ops-ic-pan-seg text-center" style="width:3.25rem" maxlength="4" inputmode="numeric" autocomplete="off" disabled>' +
    '<input type="text" class="form-control form-control-sm font-monospace ops-ic-pan-seg text-center" style="width:3.25rem" maxlength="4" inputmode="numeric" autocomplete="off" disabled>' +
    '<input type="text" class="form-control form-control-sm font-monospace ops-ic-pan-seg text-center" style="width:3.25rem" maxlength="4" inputmode="numeric" autocomplete="off" disabled>' +
    '</div>' +
    '<div id="opsIcRegPanAmex" class="d-none ops-ic-pan-seg-row d-flex flex-wrap gap-1 align-items-center">' +
    '<input type="text" class="form-control form-control-sm font-monospace ops-ic-pan-seg text-center" style="width:3.25rem" maxlength="4" inputmode="numeric" autocomplete="off" disabled>' +
    '<span class="text-muted small px-0">-</span>' +
    '<input type="text" class="form-control form-control-sm font-monospace ops-ic-pan-seg text-center" style="width:4.75rem" maxlength="6" inputmode="numeric" autocomplete="off" disabled>' +
    '<span class="text-muted small px-0">-</span>' +
    '<input type="text" class="form-control form-control-sm font-monospace ops-ic-pan-seg text-center" style="width:4rem" maxlength="5" inputmode="numeric" autocomplete="off" disabled>' +
    '</div>' +
    '<div id="opsIcRegPanDiners" class="d-none ops-ic-pan-seg-row d-flex flex-wrap gap-1 align-items-center">' +
    '<input type="text" class="form-control form-control-sm font-monospace ops-ic-pan-seg text-center" style="width:3.25rem" maxlength="4" inputmode="numeric" autocomplete="off" disabled>' +
    '<span class="text-muted small px-0">-</span>' +
    '<input type="text" class="form-control form-control-sm font-monospace ops-ic-pan-seg text-center" style="width:4.75rem" maxlength="6" inputmode="numeric" autocomplete="off" disabled>' +
    '<span class="text-muted small px-0">-</span>' +
    '<input type="text" class="form-control form-control-sm font-monospace ops-ic-pan-seg text-center" style="width:3.25rem" maxlength="4" inputmode="numeric" autocomplete="off" disabled>' +
    '</div></div>' +
    '<div class="flex-grow-1" style="min-width:12rem"><label class="form-label small mb-0" data-pg-ui-t="사유">사유</label>' +
    '<input type="text" class="form-control form-control-sm" id="opsIcRegReason" maxlength="500"></div>' +
    '<button type="button" class="btn btn-primary btn-sm" id="opsIcRegBtn" data-pg-ui-t="등록">등록</button></div></div></div>';

  var API_MERCHANT_DEPLOY_REG_HTML = '<div class="api-merchant-deploy-reg text-body">' +
    '<h5 class="fw-semibold mb-2" data-pg-ui-t="1. API 가맹점 등록">1. API 가맹점 등록</h5>' +
    '<div class="alert alert-light border small mb-3">' +
    '<p class="fw-semibold text-dark mb-2" data-pg-ui-t="순서">순서</p>' +
    '<ol class="mb-2 ps-3">' +
    '<li class="mb-1"><span data-pg-ui-t="API연동설정에서 PG사 연동 추가 후, 연동용도에 API를 켭니다.">API연동설정에서 PG사 연동 추가 후, 연동용도에 API를 켭니다.</span></li>' +
    '<li class="mb-1"><span data-pg-ui-t="업체등록·업체관리에서 조직을 가맹점으로 등록하고, 「가맹 API 연동 채널」에서 인라인·리다이렉트·WordPress 중 해당 가맹에 맞는 방식만 켭니다.">업체등록·업체관리에서 조직을 가맹점으로 등록하고, 「가맹 API 연동 채널」에서 인라인·리다이렉트·WordPress 중 해당 가맹에 맞는 방식만 켭니다.</span></li>' +
    '<li class="mb-1"><span data-pg-ui-t="결제대행사(PG) 바인딩은 업체정보 결제대행사에서 저장합니다. PG와 연동 채널은 별개입니다.">결제대행사(PG) 바인딩은 업체정보 결제대행사에서 저장합니다. PG와 연동 채널은 별개입니다.</span></li>' +
    '<li class="mb-1"><span data-pg-ui-t="콜백·결과 URL은 업체정보 또는 통보관리 메뉴에서 등록합니다.">콜백·결과 URL은 업체정보 또는 통보관리 메뉴에서 등록합니다.</span></li>' +
    '<li class="mb-0"><span data-pg-ui-t="등록이 끝나면 2. 가맹점 API 생성에서 MID·엔드포인트·연동 JSON을 발급합니다.">등록이 끝나면 2. 가맹점 API 생성에서 MID·엔드포인트·연동 JSON을 발급합니다.</span></li></ol>' +
    '<p class="small text-muted mb-0" data-pg-ui-t="이 표는 API연동설정 DB를 읽어, 연동용도에 API가 포함된 행만 보여 줍니다.">이 표는 API연동설정 DB를 읽어, 연동용도에 API가 포함된 행만 보여 줍니다.</p></div>' +
    '<div class="d-flex flex-wrap gap-2 mb-3">' +
    '<button type="button" class="btn btn-primary btn-sm" id="apiMerchRegBtnPgApi" data-pg-ui-t="API연동설정">API연동설정</button>' +
    '<button type="button" class="btn btn-outline-primary btn-sm" id="apiMerchRegBtnCompReg" data-pg-ui-t="업체등록">업체등록</button>' +
    '<button type="button" class="btn btn-outline-secondary btn-sm" id="apiMerchRegBtnCompTree" data-pg-ui-t="업체관리">업체관리</button>' +
    '<button type="button" class="btn btn-success btn-sm" id="apiMerchRegBtnNextGen" data-pg-ui-t="2. 가맹점 API 생성">2. 가맹점 API 생성</button></div>' +
    '<label class="form-label small fw-semibold mb-1" data-pg-ui-t="연동용도에 API가 켜진 결제대행사">연동용도에 API가 켜진 결제대행사</label>' +
    '<div class="table-no-col-resize-wrap border rounded mb-2">' +
    '<table class="table table-sm table-bordered align-middle mb-0 w-100 table-no-col-resize">' +
    '<thead class="table-light"><tr><th class="text-nowrap" data-pg-ui-t="PG코드">PG코드</th><th data-pg-ui-t="결제대행사명">결제대행사명</th><th class="text-nowrap" data-pg-ui-t="연동용도">연동용도</th><th class="text-nowrap" data-pg-ui-t="본사 MID">본사 MID</th><th class="text-center text-nowrap" data-pg-ui-t="본사운영">본사운영</th></tr></thead>' +
    '<tbody id="apiMerchRegPgTbody"><tr><td colspan="5" class="text-center text-muted py-3" data-pg-ui-t="불러오는 중…">불러오는 중…</td></tr></tbody></table></div>' +
    '<p class="small text-muted mb-0" data-pg-ui-t="가맹 전용 MID·키는 업체 저장 시 가맹 바인딩에 들어갑니다. 본사 행과 다를 수 있습니다.">가맹 전용 MID·키는 업체 저장 시 가맹 바인딩에 들어갑니다. 본사 행과 다를 수 있습니다.</p></div>';

  var MERCHANT_API_GENERATE_HTML = '<div class="merchant-deploy-kit text-body">' +
    '<h5 class="fw-semibold mb-2" data-pg-ui-t="2. 가맹점 API 생성">2. 가맹점 API 생성</h5>' +
    '<div class="alert alert-info small mb-3 py-3 merchant-deploy-plain-help" role="region" data-pg-ui-aria-label="화면 안내">' +
    '<p class="fw-semibold text-dark mb-2" data-pg-ui-t="이 화면은 뭘 하나요?">이 화면은 뭘 하나요?</p>' +
    '<p class="mb-3 mb-md-2"><span data-pg-ui-t="결제를 여기서 승인하는 곳이 아닙니다. 다른 서버(가맹·브로커)에 넣을 연동 설정 글자 묶음(JSON)을 받아 가거나, 그 서버들이 쓰는 비밀번호(브로커 시크릿)를 새로 뽑거나, 보안을 더 켜 두는 곳입니다.">결제를 여기서 승인하는 곳이 아닙니다. 다른 서버(가맹·브로커)에 넣을 연동 설정 글자 묶음(JSON)을 받아 가거나, 그 서버들이 쓰는 비밀번호(브로커 시크릿)를 새로 뽑거나, 보안을 더 켜 두는 곳입니다.</span></p>' +
    '<p class="fw-semibold text-dark mb-1" data-pg-ui-t="많은 경우 이 순서만 하면 됩니다">많은 경우 이 순서만 하면 됩니다</p>' +
    '<ol class="mb-3 ps-3">' +
    '<li class="mb-1"><span data-pg-ui-t="업체명에 가맹 이름 일부를 넣고 가맹점 검색을 누릅니다. (이미 업체코드를 알면 검색 없이 코드만 입력해도 됩니다.)">업체명에 가맹 이름 일부를 넣고 가맹점 검색을 누릅니다. ' +
    '(이미 업체코드를 알면 검색 없이 코드만 입력해도 됩니다.)</span></li>' +
    '<li class="mb-1"><span data-pg-ui-t="표에서 해당 가맹 줄의 선택을 누릅니다. 위쪽 업체코드 칸이 채워집니다.">표에서 해당 가맹 줄의 선택을 누릅니다. 위쪽 업체코드 칸이 채워집니다.</span></li>' +
    '<li class="mb-1"><span data-pg-ui-t="PG 범위에서 실제 붙일 결제사(PG) 하나를 고릅니다. 잘 모르겠으면 일단 전체 PG로 받아 보고, 담당자에게 물어도 됩니다.">PG 범위에서 실제 붙일 결제사(PG) 하나를 고릅니다. 잘 모르겠으면 일단 전체 PG로 받아 보고, 담당자에게 물어도 됩니다.</span></li>' +
    '<li class="mb-0"><span data-pg-ui-t="연동 키트 JSON·PHP 중 가맹 환경에 맞는 패키지 버튼을 누릅니다.">연동 키트 JSON·PHP 중 가맹 환경에 맞는 패키지 버튼을 누릅니다.</span></li>' +
    '</ol>' +
    '<p class="fw-semibold text-dark mb-1" data-pg-ui-t="아래 두 가지는 꼭 구분하세요">아래 두 가지는 꼭 구분하세요</p>' +
    '<ul class="mb-3 ps-3">' +
    '<li class="mb-1"><span data-pg-ui-t="연동 키트 JSON — 지금 서버에 저장된 연동 정보를 읽기만 합니다. 가맹이나 PG가 망가지지는 않습니다.">연동 키트 JSON — 지금 서버에 저장된 연동 정보를 읽기만 합니다. 가맹이나 PG가 망가지지는 않습니다.</span></li>' +
    '<li class="mb-1"><span data-pg-ui-t="브로커 시크릿 재발급 — 누르는 순간 예전 비밀번호는 쓸 수 없게 됩니다. 유출·도용이 의심될 때나, 담당자가 교체하라고 했을 때만 누르세요. 브로커 서버 설정도 같은 날 맞춰 바꿔야 결제가 끊기지 않습니다.">브로커 시크릿 재발급 — 누르는 순간 예전 비밀번호는 쓸 수 없게 됩니다. ' +
    '유출·도용이 의심될 때나, 담당자가 교체하라고 했을 때만 누르세요. 브로커 서버 설정도 같은 날 맞춰 바꿔야 결제가 끊기지 않습니다.</span></li>' +
    '</ul>' +
    '<p class="fw-semibold text-dark mb-1" data-pg-ui-t="입력 칸은 이렇게 읽으면 됩니다">입력 칸은 이렇게 읽으면 됩니다</p>' +
    '<ul class="mb-3 ps-3">' +
    '<li class="mb-1"><span data-pg-ui-t="업체코드 — 그 가맹을 시스템에서 부른 번호(M000… 같은 것). 목록 없이 직접 쳐도 됩니다.">업체코드 — 그 가맹을 시스템에서 부른 번호(M000… 같은 것). 목록 없이 직접 쳐도 됩니다.</span></li>' +
    '<li class="mb-1"><span data-pg-ui-t="업체명 — 사람이 부르는 상호 일부. 검색용입니다.">업체명 — 사람이 부르는 상호 일부. 검색용입니다.</span></li>' +
    '<li class="mb-1"><span data-pg-ui-t="PG 범위 — 방금 말한 JSON·시크릿·강제 저장이 어느 PG 줄에 적용될지 고르는 것입니다.">PG 범위 — 방금 말한 JSON·시크릿·강제 저장이 어느 PG 줄에 적용될지 고르는 것입니다.</span></li>' +
    '</ul>' +
    '<p class="fw-semibold text-dark mb-1" data-pg-ui-t="강제(시크릿 헤더 필수)란?">강제(시크릿 헤더 필수)란?</p>' +
    '<p class="mb-2"><span data-pg-ui-t="체크하면 외부에서 우리 미들웨어를 부를 때 비밀번호 헤더를 반드시 붙이게 합니다. 보안을 올리는 설정이라, 개발·연동 담당과 말 맞춘 뒤 강제여부 저장을 누르는 것이 안전합니다.">체크하면 외부에서 우리 미들웨어를 부를 때 비밀번호 헤더를 반드시 붙이게 합니다. 보안을 올리는 설정이라, 개발·연동 담당과 말 맞춘 뒤 강제여부 저장을 누르는 것이 안전합니다.</span></p>' +
    '<p class="fw-semibold text-dark mb-1" data-pg-ui-t="표에 아무도 안 나올 때">표에 아무도 안 나올 때</p>' +
    '<p class="mb-0 text-muted"><span data-pg-ui-t="검색어를 줄이거나 바꿔 보세요. 그래도 0건이면, 지금 로그인한 계정으로는 그 가맹이 안 보이는 것입니다(상위 조직·권한). 본사 관리자에게 조회 범위를 물어보세요.">검색어를 줄이거나 바꿔 보세요. 그래도 0건이면, 지금 로그인한 계정으로는 그 가맹이 안 보이는 것입니다(상위 조직·권한). 본사 관리자에게 조회 범위를 물어보세요.</span></p>' +
    '</div>' +
    '<details class="merchant-deploy-help small mb-3 p-2 border rounded bg-light">' +
    '<summary class="fw-semibold text-secondary" data-pg-ui-t="개발자용 한 줄(접기)">개발자용 한 줄(접기)</summary>' +
    '<p class="mb-1 mt-2"><span data-pg-ui-t="키트에는 바인딩·공개 API 베이스·노티 URL·브로커 등이 JSON으로 포함됩니다. 강제 시 헤더 X-Icopay-Merchant-Broker-Secret 없이 /api/middleware/v1/pg/... 호출이 막힙니다. 신규 PG는 본사 PG 연동 설정에 올라오면 PG 범위 목록에 자동 반영됩니다.">키트에는 바인딩·공개 API 베이스·노티 URL·브로커 등이 JSON으로 포함됩니다. 강제 시 헤더 X-Icopay-Merchant-Broker-Secret 없이 /api/middleware/v1/pg/... 호출이 막힙니다. 신규 PG는 본사 PG 연동 설정에 올라오면 PG 범위 목록에 자동 반영됩니다.</span></p>' +
    '</details>' +
    '<div id="merchantGenKitSummary" class="alert alert-secondary small mb-3 py-2 d-none" role="status" aria-live="polite"></div>' +
    '<div class="row g-2 mb-3">' +
    '<div class="col-md-4"><label class="form-label small mb-0" data-pg-ui-t="업체코드 (직접 입력)">업체코드 (직접 입력)</label>' +
    '<input type="text" class="form-control form-control-sm" id="merchantDeployCompId" autocomplete="off" data-pg-ui-placeholder="예: M000123" placeholder="예: M000123"></div>' +
    '<div class="col-md-4"><label class="form-label small mb-0" data-pg-ui-t="업체명 (검색)">업체명 (검색)</label>' +
    '<input type="text" class="form-control form-control-sm" id="merchantDeploySearchCompNm" autocomplete="off"></div>' +
    '<div class="col-md-4"><label class="form-label small mb-0" data-pg-ui-t="PG 범위 (키트·시크릿)">PG 범위 (키트·시크릿)</label>' +
    '<select class="form-control form-control-sm" id="merchantDeployVendorScope"></select></div></div>' +
    '<div class="d-flex flex-wrap gap-2 align-items-center mb-3">' +
    '<button type="button" class="btn btn-primary btn-sm" id="merchantDeploySearchBtn" data-pg-ui-t="가맹점 검색">가맹점 검색</button>' +
    '<button type="button" class="btn btn-outline-primary btn-sm" id="merchantDeployLoadKitJsonBtn" data-pg-ui-t="JSON 연동 패키지">JSON 연동 패키지</button>' +
    '<button type="button" class="btn btn-outline-primary btn-sm" id="merchantDeployLoadKitPhpBtn" data-pg-ui-t="PHP 연동 패키지">PHP 연동 패키지</button>' +
    '<button type="button" class="btn btn-warning btn-sm" id="merchantDeployRotateSecretBtn" data-pg-ui-t="브로커 시크릿 재발급">브로커 시크릿 재발급</button>' +
    '<label class="small mb-0 ms-1 d-flex align-items-center gap-1"><input type="checkbox" id="merchantDeployEnforceYn" checked> ' +
    '<span data-pg-ui-t="강제(시크릿 헤더 필수)">강제(시크릿 헤더 필수)</span></label>' +
    '<button type="button" class="btn btn-outline-secondary btn-sm" id="merchantDeployEnforceBtn" data-pg-ui-t="강제여부 저장">강제여부 저장</button></div>' +
    '<div class="merchant-deploy-table-wrap table-no-col-resize-wrap border rounded mb-2">' +
    '<table class="table table-sm table-bordered align-middle merchant-deploy-merchant-table table-no-col-resize w-100 mb-0" id="merchantDeployMerchantGrid">' +
    '<colgroup><col class="merchant-deploy-col-act" /><col class="merchant-deploy-col-code" /><col class="merchant-deploy-col-master" /><col /><col class="merchant-deploy-col-pg" /><col class="merchant-deploy-col-channel" /><col class="merchant-deploy-col-cur" /><col class="merchant-deploy-col-broker" /><col class="merchant-deploy-col-issued-date" /><col class="merchant-deploy-col-issued-by" /></colgroup>' +
    '<thead class="table-light"><tr><th class="text-center text-nowrap" data-pg-ui-t="선택">선택</th><th class="text-nowrap" data-pg-ui-t="업체코드">업체코드</th><th class="text-nowrap" data-pg-ui-t="상위 총판">상위 총판</th><th data-pg-ui-t="업체명">업체명</th><th class="text-nowrap" data-pg-ui-t="PG대행사">PG대행사</th><th class="text-nowrap" data-pg-ui-t="채널" data-pg-ui-title="가맹 API 연동 채널: IN=INLINE, RE=REDIRECT, WO=WordPress/WooCommerce. 복수 사용 시 IN/RE 형식.">채널</th><th class="text-nowrap" data-pg-ui-t="기준통화">기준통화</th><th class="text-nowrap" data-pg-ui-t="브로커 시크릿">브로커 시크릿</th><th class="text-nowrap" data-pg-ui-t="발행일자">발행일자</th><th class="text-nowrap" data-pg-ui-t="발행자">발행자</th></tr></thead>' +
    '<tbody><tr><td colspan="10" class="text-center text-muted py-3" data-pg-ui-t="로딩 중…">로딩 중…</td></tr></tbody></table></div>' +
    '<p class="small text-muted mb-2" id="merchantDeployPageInfo"></p>' +
    '<label class="form-label small mb-0" id="merchantDeployKitLabel" data-pg-ui-t="연동 패키지 (JSON / PHP)">연동 패키지 (JSON / PHP)</label>' +
    '<pre id="merchantDeployKitJson" class="bg-light p-3 small mb-0" style="max-height:520px;overflow:auto;border:1px solid #dee2e6;white-space:pre-wrap;">' +
    '<span data-pg-ui-t="업체를 고른 뒤 JSON 또는 PHP 연동 패키지 버튼을 누르세요.">업체를 고른 뒤 JSON 또는 PHP 연동 패키지 버튼을 누르세요.</span></pre>' +
    '<div id="merchantDeployKitDetail" class="d-none mt-4">' +
    '<section class="mb-4"><h6 class="fw-semibold border-bottom pb-2 mb-3" data-pg-ui-t="가맹 API 연동 채널">가맹 API 연동 채널</h6>' +
    '<div class="small" id="merchantGenIntegrationChannels"></div></section>' +
    '<section class="mb-4"><h6 class="fw-semibold border-bottom pb-2 mb-3 d-flex flex-wrap align-items-baseline gap-1">' +
    '<span data-pg-ui-t="Checkout API 엔드포인트">Checkout API 엔드포인트</span>' +
    '<span class="text-muted fw-normal user-select-none" aria-hidden="true">/</span>' +
    '<button type="button" class="btn btn-link btn-sm p-0 align-baseline text-nowrap merchant-api-flow-doc-menu" id="merchantGenFlowDocOpen" data-pg-ui-t="연동설명서">연동설명서</button></h6>' +
    '<ul class="small mb-0 ps-3" id="merchantGenEndpoints"></ul></section>' +
    '<section class="mb-4"><h6 class="fw-semibold border-bottom pb-2 mb-3" data-pg-ui-t="결제 통보 (Webhook) 안내">결제 통보 (Webhook) 안내</h6>' +
    '<div class="small" id="merchantGenWebhook"></div></section>' +
    '<section class="mb-4"><h6 class="fw-semibold border-bottom pb-2 mb-3" data-pg-ui-t="WordPress JPAY 플러그인">WordPress JPAY 플러그인</h6>' +
    '<div class="small" id="merchantGenWordPress"></div></section>' +
    '<section class="mb-2"><h6 class="fw-semibold border-bottom pb-2 mb-3" data-pg-ui-t="연동 체크리스트">연동 체크리스트</h6>' +
    '<ol class="small mb-0 ps-3" id="merchantGenChecklist"></ol></section></div></div>';

  var MERCHANT_API_DEPLOY_DOCS_HTML = '<div class="merchant-api-deploy-docs text-body">' +
    '<h5 class="fw-semibold mb-2" data-pg-ui-t="API 배포 문서">API 배포 문서</h5>' +
    '<div class="alert alert-info small mb-3 py-3" role="region" data-pg-ui-aria-label="화면 안내">' +
    '<p class="fw-semibold text-dark mb-2" data-pg-ui-t="가맹점 연동용 자료">가맹점 연동용 자료</p>' +
    '<p class="mb-2" data-pg-ui-t="API배포문서 안내 본문">이 화면에서 가맹점에 전달할 연동 샘플·파라미터 규격·엔드포인트를 확인하고 다운로드할 수 있습니다. 브로커 시크릿 재발급은 「가맹점 API 생성」 화면에서 수행하세요.</p>' +
    '<p class="mb-0 text-muted small" data-pg-ui-t="API배포문서 안내 보안">브로커 시크릿은 가맹 서버에만 두고 브라우저·앱에 노출하지 마세요.</p></div>' +
    '<div class="row g-2 mb-3">' +
    '<div class="col-md-4"><label class="form-label small mb-0" data-pg-ui-t="업체코드 (직접 입력)">업체코드 (직접 입력)</label>' +
    '<input type="text" class="form-control form-control-sm" id="merchantApiDocsCompId" autocomplete="off" data-pg-ui-placeholder="예: M000123" placeholder="예: M000123"></div>' +
    '<div class="col-md-4"><label class="form-label small mb-0" data-pg-ui-t="업체명 (검색)">업체명 (검색)</label>' +
    '<input type="text" class="form-control form-control-sm" id="merchantApiDocsSearchCompNm" autocomplete="off"></div>' +
    '<div class="col-md-4 d-flex align-items-end"><button type="button" class="btn btn-primary btn-sm w-100" id="merchantApiDocsSearchBtn" data-pg-ui-t="가맹점 검색">가맹점 검색</button></div></div>' +
    '<div class="merchant-deploy-table-wrap table-no-col-resize-wrap border rounded mb-2">' +
    '<table class="table table-sm table-bordered align-middle merchant-deploy-merchant-table table-no-col-resize w-100 mb-0" id="merchantApiDocsMerchantGrid">' +
    '<colgroup><col class="merchant-deploy-col-act" /><col class="merchant-deploy-col-code" /><col class="merchant-deploy-col-master" /><col /><col class="merchant-deploy-col-pg" /><col class="merchant-deploy-col-channel" /><col class="merchant-deploy-col-cur" /><col class="merchant-deploy-col-broker" /><col class="merchant-deploy-col-issued-date" /><col class="merchant-deploy-col-issued-by" /></colgroup>' +
    '<thead class="table-light"><tr><th class="text-center text-nowrap" data-pg-ui-t="선택">선택</th><th class="text-nowrap" data-pg-ui-t="업체코드">업체코드</th><th class="text-nowrap" data-pg-ui-t="상위 총판">상위 총판</th><th data-pg-ui-t="업체명">업체명</th><th class="text-nowrap" data-pg-ui-t="PG대행사">PG대행사</th><th class="text-nowrap" data-pg-ui-t="채널" data-pg-ui-title="가맹 API 연동 채널: IN=INLINE, RE=REDIRECT, WO=WordPress/WooCommerce. 복수 사용 시 IN/RE 형식.">채널</th><th class="text-nowrap" data-pg-ui-t="기준통화">기준통화</th><th class="text-nowrap" data-pg-ui-t="브로커 시크릿">브로커 시크릿</th><th class="text-nowrap" data-pg-ui-t="발행일자">발행일자</th><th class="text-nowrap" data-pg-ui-t="발행자">발행자</th></tr></thead>' +
    '<tbody><tr><td colspan="10" class="text-center text-muted py-3" data-pg-ui-t="로딩 중…">로딩 중…</td></tr></tbody></table></div>' +
    '<p class="small text-muted mb-3" id="merchantApiDocsPageInfo"></p>' +
    '<div id="merchantApiDocsSummary" class="alert alert-secondary small mb-3 py-2 d-none" role="status" aria-live="polite"></div>' +
    '<div id="merchantApiDocsContent" class="d-none">' +
    '<section class="mb-4"><h6 class="fw-semibold border-bottom pb-2 mb-3" data-pg-ui-t="다운로드 자료">다운로드 자료</h6>' +
    '<div class="table-responsive border rounded mb-2"><table class="table table-sm table-bordered align-middle mb-0 w-100" id="merchantApiDocsDownloadGrid">' +
    '<thead class="table-light"><tr><th data-pg-ui-t="구분">구분</th><th data-pg-ui-t="설명">설명</th><th class="text-nowrap" data-pg-ui-t="다운로드">다운로드</th></tr></thead>' +
    '<tbody></tbody></table></div></section>' +
    '<section class="mb-4"><h6 class="fw-semibold border-bottom pb-2 mb-3" id="merchantApiDocsParamTitle" data-pg-ui-t="연동 파라미터 규격">연동 파라미터 규격</h6>' +
    '<p class="small text-muted mb-2" id="merchantApiDocsEndpointLine"></p>' +
    '<h6 class="small fw-semibold mt-3 mb-2" data-pg-ui-t="HTTP 헤더">HTTP 헤더</h6>' +
    '<div class="table-responsive border rounded mb-3"><table class="table table-sm table-bordered align-middle mb-0 w-100" id="merchantApiDocsHeadersGrid">' +
    '<thead class="table-light"><tr><th class="text-nowrap" data-pg-ui-t="번호">번호</th><th data-pg-ui-t="항목명">항목명</th><th class="text-nowrap" data-pg-ui-t="필수">필수</th><th data-pg-ui-t="예시값">예시값</th><th data-pg-ui-t="비고">비고</th></tr></thead><tbody></tbody></table></div>' +
    '<h6 class="small fw-semibold mt-3 mb-2" data-pg-ui-t="Prepare 본문 파라미터">Prepare 본문 파라미터</h6>' +
    '<div class="table-responsive border rounded mb-3"><table class="table table-sm table-bordered align-middle mb-0 w-100" id="merchantApiDocsPrepareGrid">' +
    '<thead class="table-light"><tr><th class="text-nowrap" data-pg-ui-t="번호">번호</th><th data-pg-ui-t="항목명">항목명</th><th data-pg-ui-t="JSON 경로">JSON 경로</th><th class="text-nowrap" data-pg-ui-t="타입">타입</th><th class="text-nowrap" data-pg-ui-t="최대길이">최대길이</th><th class="text-nowrap" data-pg-ui-t="필수">필수</th><th data-pg-ui-t="설명">설명</th><th data-pg-ui-t="비고">비고</th></tr></thead><tbody></tbody></table></div>' +
    '<h6 class="small fw-semibold mt-3 mb-2" data-pg-ui-t="buyer 객체 파라미터">buyer 객체 파라미터</h6>' +
    '<div class="table-responsive border rounded mb-3"><table class="table table-sm table-bordered align-middle mb-0 w-100" id="merchantApiDocsBuyerGrid">' +
    '<thead class="table-light"><tr><th class="text-nowrap" data-pg-ui-t="번호">번호</th><th data-pg-ui-t="항목명">항목명</th><th data-pg-ui-t="JSON 경로">JSON 경로</th><th class="text-nowrap" data-pg-ui-t="타입">타입</th><th class="text-nowrap" data-pg-ui-t="최대길이">최대길이</th><th class="text-nowrap" data-pg-ui-t="필수">필수</th><th data-pg-ui-t="설명">설명</th><th data-pg-ui-t="비고">비고</th></tr></thead><tbody></tbody></table></div>' +
    '<h6 class="small fw-semibold mt-3 mb-2" data-pg-ui-t="Status 조회 파라미터">Status 조회 파라미터</h6>' +
    '<div class="table-responsive border rounded mb-3"><table class="table table-sm table-bordered align-middle mb-0 w-100" id="merchantApiDocsStatusGrid">' +
    '<thead class="table-light"><tr><th class="text-nowrap" data-pg-ui-t="번호">번호</th><th data-pg-ui-t="항목명">항목명</th><th data-pg-ui-t="JSON 경로">JSON 경로</th><th class="text-nowrap" data-pg-ui-t="타입">타입</th><th class="text-nowrap" data-pg-ui-t="최대길이">최대길이</th><th class="text-nowrap" data-pg-ui-t="필수">필수</th><th data-pg-ui-t="설명">설명</th><th data-pg-ui-t="비고">비고</th></tr></thead><tbody></tbody></table></div>' +
    '<h6 class="small fw-semibold mt-3 mb-2" data-pg-ui-t="오류 코드">오류 코드</h6>' +
    '<div class="table-responsive border rounded mb-3"><table class="table table-sm table-bordered align-middle mb-0 w-100" id="merchantApiDocsErrorGrid">' +
    '<thead class="table-light"><tr><th data-pg-ui-t="오류코드">오류코드</th><th data-pg-ui-t="의미">의미</th></tr></thead><tbody></tbody></table></div></section>' +
    '<section class="mb-4"><h6 class="fw-semibold border-bottom pb-2 mb-3" data-pg-ui-t="가맹 API 연동 채널">가맹 API 연동 채널</h6>' +
    '<div class="small" id="merchantApiDocsIntegrationChannels"></div></section>' +
    '<section class="mb-4"><h6 class="fw-semibold border-bottom pb-2 mb-3 d-flex flex-wrap align-items-baseline gap-1">' +
    '<span data-pg-ui-t="Checkout API 엔드포인트">Checkout API 엔드포인트</span>' +
    '<span class="text-muted fw-normal user-select-none" aria-hidden="true">/</span>' +
    '<button type="button" class="btn btn-link btn-sm p-0 align-baseline text-nowrap merchant-api-flow-doc-menu" id="merchantApiDocsFlowDocOpen" data-pg-ui-t="연동설명서">연동설명서</button></h6>' +
    '<ul class="small mb-0 ps-3" id="merchantApiDocsEndpoints"></ul></section>' +
    '<section class="mb-4"><h6 class="fw-semibold border-bottom pb-2 mb-3" data-pg-ui-t="WordPress JPAY 플러그인">WordPress JPAY 플러그인</h6>' +
    '<div class="small" id="merchantApiDocsWordPress"></div></section>' +
    '<section class="mb-4"><h6 class="fw-semibold border-bottom pb-2 mb-3" data-pg-ui-t="결제 통보 (Webhook) 안내">결제 통보 (Webhook) 안내</h6>' +
    '<div class="small" id="merchantApiDocsWebhook"></div></section>' +
    '<section class="mb-2"><h6 class="fw-semibold border-bottom pb-2 mb-3" data-pg-ui-t="연동 체크리스트">연동 체크리스트</h6>' +
    '<ol class="small mb-0 ps-3" id="merchantApiDocsChecklist"></ol></section></div>' +
    '<div id="merchantApiDocsEmpty" class="text-center text-muted py-5 small" data-pg-ui-t="업체를 선택하면 연동 자료가 표시됩니다.">업체를 선택하면 연동 자료가 표시됩니다.</div></div>';

  var MERCHANT_API_PORTAL_HTML = '<div class="merchant-api-portal text-body pg-merchant-api-portal-viewer">' +
    '<h5 class="fw-semibold mb-2" data-pg-ui-t="가맹점 API 연동">가맹점 API 연동</h5>' +
    '<div class="alert alert-info small mb-3 py-3" role="region">' +
    '<p class="mb-2" data-pg-ui-t="가맹점API 안내 본문">본사에서 배포한 API 연동 키와 엔드포인트·샘플만 조회할 수 있습니다. 키 발급·재발급은 본사에서만 가능합니다.</p>' +
    '<p class="mb-0 text-muted small" data-pg-ui-t="가맹점API 안내 보안">브로커 시크릿은 가맹 서버에만 보관하고 브라우저·앱·공개 저장소에 노출하지 마세요.</p></div>' +
    '<div id="merchantApiPortalNotDeployed" class="alert alert-warning small d-none" role="status"></div>' +
    '<div id="merchantApiPortalMain" class="d-none">' +
    '<section class="mb-4"><h6 class="fw-semibold border-bottom pb-2 mb-3" data-pg-ui-t="연동 키">연동 키</h6>' +
    '<div class="table-responsive border rounded mb-2"><table class="table table-sm table-bordered align-middle mb-0 w-100" id="merchantApiPortalKeysGrid">' +
    '<thead class="table-light"><tr><th data-pg-ui-t="항목">항목</th><th data-pg-ui-t="값">값</th><th class="text-nowrap" data-pg-ui-t="작업">작업</th></tr></thead>' +
    '<tbody id="merchantApiPortalKeysBody"></tbody></table></div></section>' +
    '<section class="mb-4"><h6 class="fw-semibold border-bottom pb-2 mb-3" data-pg-ui-t="PG 바인딩 (MID)">PG 바인딩 (MID)</h6>' +
    '<ul class="small mb-0 ps-3" id="merchantApiPortalBindings"></ul></section>' +
    '<section class="mb-4"><h6 class="fw-semibold border-bottom pb-2 mb-3" data-pg-ui-t="가맹 API 연동 채널">가맹 API 연동 채널</h6>' +
    '<div class="small" id="merchantApiPortalIntegrationChannels"></div></section>' +
    '<section class="mb-4"><h6 class="fw-semibold border-bottom pb-2 mb-3 d-flex flex-wrap align-items-baseline gap-1">' +
    '<span data-pg-ui-t="Checkout API 엔드포인트">Checkout API 엔드포인트</span>' +
    '<span class="text-muted fw-normal user-select-none" aria-hidden="true">/</span>' +
    '<button type="button" class="btn btn-link btn-sm p-0 align-baseline text-nowrap merchant-api-flow-doc-menu" id="merchantApiPortalFlowDocOpen" data-pg-ui-t="연동설명서">연동설명서</button></h6>' +
    '<ul class="small mb-0 ps-3" id="merchantApiPortalEndpoints"></ul></section>' +
    '<section class="mb-4"><h6 class="fw-semibold border-bottom pb-2 mb-3" data-pg-ui-t="WordPress JPAY 플러그인">WordPress JPAY 플러그인</h6>' +
    '<div class="small" id="merchantApiPortalWordPress"></div></section>' +
    '<section class="mb-4"><h6 class="fw-semibold border-bottom pb-2 mb-3" data-pg-ui-t="결제 통보 (Webhook) 안내">결제 통보 (Webhook) 안내</h6>' +
    '<div class="small" id="merchantApiPortalWebhook"></div></section>' +
    '<section class="mb-4"><h6 class="fw-semibold border-bottom pb-2 mb-3" data-pg-ui-t="다운로드 자료">다운로드 자료</h6>' +
    '<div class="table-responsive border rounded mb-2"><table class="table table-sm table-bordered align-middle mb-0 w-100" id="merchantApiPortalDownloadGrid">' +
    '<thead class="table-light"><tr><th data-pg-ui-t="구분">구분</th><th data-pg-ui-t="설명">설명</th><th class="text-nowrap" data-pg-ui-t="다운로드">다운로드</th></tr></thead>' +
    '<tbody></tbody></table></div></section>' +
    '<section class="mb-4"><h6 class="fw-semibold border-bottom pb-2 mb-3" data-pg-ui-t="연동 체크리스트">연동 체크리스트</h6>' +
    '<ol class="small mb-0 ps-3" id="merchantApiPortalChecklist"></ol></section>' +
    '<section class="mb-2"><h6 class="fw-semibold border-bottom pb-2 mb-3" data-pg-ui-t="가맹 API 연동 시 유의사항">가맹 API 연동 시 유의사항</h6>' +
    '<ul class="small mb-0 ps-3">' +
    '<li class="mb-1" data-pg-ui-t="가맹 API 유의: prepare 서버 전용">Prepare API는 가맹 <strong>서버</strong>에서만 호출하세요. 브로커 시크릿을 브라우저·앱·공개 저장소에 노출하지 마세요.</li>' +
    '<li class="mb-1" data-pg-ui-t="가맹 API 유의: sessionToken embed">브라우저에는 Prepare 응답의 <code>sessionToken</code>과 Embed 스크립트만 전달하세요.</li>' +
    '<li class="mb-0" data-pg-ui-t="가맹 API 유의: 결제 확정">결제 확정은 웹훅(<code>merchantNotifyUrls</code>) 또는 Status API로 <strong>서버</strong>에서 확인하세요.</li>' +
    '</ul></section></div>' +
    '<div id="merchantApiPortalLoading" class="text-center text-muted py-5 small" data-pg-ui-t="로딩 중…">로딩 중…</div></div>';

  var MENU_SCREENS = {
    '/hq/apiMerchantDeployReg': {
      hideListGrid: true,
      staticHtml: API_MERCHANT_DEPLOY_REG_HTML,
      summary: [],
      buttons: []
    },
    '/hq/merchantApiGenerate': {
      hideListGrid: true,
      staticHtml: MERCHANT_API_GENERATE_HTML,
      summary: [],
      buttons: []
    },
    '/hq/merchantApiDeployDocs': {
      hideListGrid: true,
      staticHtml: MERCHANT_API_DEPLOY_DOCS_HTML,
      summary: [],
      buttons: []
    },
    '/comp/merchantApiPortal': {
      hideListGrid: true,
      staticHtml: MERCHANT_API_PORTAL_HTML,
      summary: [],
      buttons: []
    },
    '/hq/pgApiMng': {
      emptyMessage: '조회된 데이터가 없습니다.',
      paginationSizeOptions: [25, 50, 100, 150, 200],
      paginationDefaultSize: 50,
      /* 열이 많아 좁은 화면에서는 가로 스크롤로 전체 열 확인 */
      tableResponsiveExtraClass: 'hq-pg-api-mng-scroll',
      /* 열 너비는 CSS(data-key)로 균형 — col-resize 저장값이 엔드포인트 열만 과도하게 넓히는 것 방지 */
      tableExtraClass: 'hq-pg-api-mng-table table-no-col-resize',
      searchRows: [[
        { label: 'PG사명', type: 'text', name: 'searchPgNm' },
        { label: '사용여부', type: 'select', name: 'searchUseYn', options: [{ v: '', t: '전체' }, { v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }] },
        { type: 'searchBtn', label: '검색' }
      ]],
      noticeList: [
        '연동 용도(노티·URL·챗봇·API)와 용도별 엔드포인트를 구분해 저장합니다. URL 용도 행은 「URL금액」에서 일반(일반형) / DP(DISPLAY) / BLIND를 지정할 수 있으며, 본사 URL결제설정(FX JSON)의 해당 PG 금액 모드와 동일합니다. 노티=미들웨어 수신 매칭, URL=공개 URL 결제 플로우, 챗봇/API=PG사 API 직연동(동일 연동 URL). 목록 「연동용도」는 파스텔 색으로 구분됩니다. API Key·MD5는 목록 미노출. [삭제]는 등록일 오른쪽, 신규는 [PG사 연동 추가]입니다.',
        '통합정산 「예정(ICOPAY)」열: PG사 연동 편집에서 T+N(주말 제외 영업일·결제와 동일 시각) 또는 D+N(달력+N일·일괄 시각)을 저장합니다. OFF면 예정일을 채우지 않습니다. D는 일괄 시각(HH:mm) 필수.',
        'ChillPay는 PG코드 CHILLPAY, API·URL 엔드포인트는 ChillPayService가 병합 반영합니다. 운영 DB는 db/V35_pg_agency_integration_scope.sql 적용 후 배포하세요.'
      ],
      tableColumnGuide: true,
      summary: ['건수'],
      buttons: [
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' },
        { id: 'hqPgApiOperationalSaveBtn', label: '운영 저장', cls: 'btn-outline-primary' },
        { id: 'hqPgApiAddBtn', label: 'PG사 연동 추가', cls: 'btn-success' }
      ],
      columnGuideFixedKeys: ['rowNo', '_pgRowAct'],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'pgNm', label: '결제대행사', thClass: 'text-nowrap' }, { key: 'pgCd', label: 'PG코드', thClass: 'text-nowrap' }, { key: 'integrationScopeLabel', label: '연동용도', thClass: 'pg-api-mng-scope-th text-nowrap' }, { key: 'urlPayAmountModeLabel', label: 'URL금액', thClass: 'text-nowrap', title: 'URL결제: 일반형 / DP(DISPLAY) / BLIND' }, { key: 'endpointsSummary', label: '엔드포인트', thClass: 'pg-api-mng-endpoints-th' }, { key: 'merchantMid', label: 'MID', thClass: 'text-nowrap' }, { key: 'hasApiKey', label: 'API', thClass: 'text-nowrap' }, { key: 'hasMd5Key', label: 'MD5', thClass: 'text-nowrap' }, { key: 'routeNo', label: 'RT', thClass: 'text-nowrap', title: 'Route 번호' }, { key: 'sandboxYn', label: '환경', thClass: 'text-nowrap', title: 'Sandbox / Production' }, { key: 'extSettleMode', label: '예정', thClass: 'text-nowrap', title: '통합정산 ICOPAY 예정: OFF/T/D' }, { key: 'extSettleLag', label: 'N', thClass: 'text-nowrap' }, { key: 'extSettleBatchTime', label: 'D시각', thClass: 'text-nowrap' }, { key: 'operationalYn', label: '운영', thClass: 'text-nowrap' }, { key: 'useYn', label: '사용', thClass: 'text-nowrap' }, { key: 'regDt', label: '등록일', thClass: 'text-nowrap' }, { key: '_pgRowAct', type: 'pgApiMngRowActions', label: '관리', thClass: 'text-nowrap' }]
    },
    '/hq/defaultCommission': {
      isForm: true,
      formSections: [
        {
          title: '기본 수수료 정책',
          id: 'hqDefaultCommFeeCard',
          notice: '총본사~영업점은 조직 배분(결제율·건당)에 반영됩니다. 가맹 열은 가맹점에 적용되는 합계(기본값)이며, 가맹점이 본사설정을 따를 때 기준이 됩니다. 업체관리 수수료에서 수정하면 그 값이 우선합니다. 결제·USDT·FX는 승인금액 기준 %, 3DS는 정책통화 기준 건당 고정, 나머지 건당·월간은 통화 단위입니다.',
          rows: [
            [{ type: 'customHtml', col: 2, html: function () {
              return '<div class="form-field-block">' +
              '<label class="form-label" data-pg-ui-t="정책코드">' + escUi(L('정책코드')) + '</label>' +
              '<input type="hidden" name="templateScope" id="hqDefCommTemplateScope" value="">' +
              '<select id="hqDefCommTemplateScopeDisplay" class="form-control form-control-sm" disabled title="' + escUi(L('코드는 저장 시 자동 부여되며, 수정할 수 없습니다.')) + '">' +
              '<option value="">' + escUi(L('(신규) 저장 시 자동 부여')) + '</option></select>' +
              '<p class="text-muted small mb-0 mt-1" data-pg-ui-t="고유 코드는 시스템이 부여합니다. 목록에서 정책을 불러와 편집만 할 수 있습니다.">' + escUi(L('고유 코드는 시스템이 부여합니다. 목록에서 정책을 불러와 편집만 할 수 있습니다.')) + '</p></div>';
            } }, { label: '정책명', type: 'text', name: 'policyName', col: 2, placeholder: '예: 기본정책 A' }, { label: '배포', type: 'select', name: 'deployYn', options: [{ v: 'Y', t: '배포' }, { v: 'N', t: '미배포' }], col: 2 }, { label: '통화코드', type: 'select', name: 'currencyCode', col: 2, options: [{ v: 'KRW', t: 'KRW' }, { v: 'USD', t: 'USD' }, { v: 'JPY', t: 'JPY' }, { v: 'EUR', t: 'EUR' }, { v: 'CNY', t: 'CNY' }, { v: 'THB', t: 'THB' }, { v: 'VND', t: 'VND' }, { v: 'GBP', t: 'GBP' }, { v: 'TWD', t: 'TWD' }, { v: 'HKD', t: 'HKD' }, { v: 'USDT', t: 'USDT' }] }],
            [{ type: 'customHtml', col: 12, html: hqDefaultCommissionTierMatrixHtml }],
            [{ label: '차지백 구간정책', type: 'select', name: 'chargebackPolicyId', col: 6, options: [{ v: '', t: '(미사용) 건당 차지백만' }] }],
            [{ type: 'customHtml', col: 12, html: hqDefaultExtraFeesCardHtml }],
            [{ type: 'customHtml', col: 12, html: function () {
              return '<div class="form-field-block">' +
              '<label class="form-label" for="hqDefCommPolicyRemark" data-pg-ui-t="정책비고(저장)">' + escUi(L('정책비고(저장)')) + '</label>' +
              '<textarea class="form-control form-control-sm" name="policyRemark" id="hqDefCommPolicyRemark" rows="3"></textarea>' +
              '</div>';
            } }]
          ]
        },
        {
          title: '기본 보류율 정책',
          id: 'hqDefaultCommHoldCard',
          notice: '가맹점 등록의 [보류율 설정]과 동일한 개념입니다. 승인(결제) 금액 중 롤링(담보금) 비율(%)만큼 보류하고, 설정한 보류 영업일 수가 지나면 정산 실행 시 지급액에 합산됩니다. 본사정책 따름(Y)이면 가맹점은 아래 본사 템플릿의 롤링 비율·일수를 따릅니다.',
          rows: [
            [{ label: '롤링(담보금)비율(%)', type: 'text', name: 'rollingPct', col: 2, placeholder: '5 또는 10' }, { label: '롤링보류일수', type: 'text', name: 'rollingDays', col: 2, placeholder: '120 또는 180' }],
            [{ type: 'customHtml', col: 12, html: function () {
              return '<div class="d-flex justify-content-end flex-wrap gap-2 mt-2 pt-3 border-top">' +
              '<button type="button" class="btn btn-outline-secondary btn-sm" id="hqDefCommNewPolicyBtn" data-pg-ui-t="신규정책">' + escUi(L('신규정책')) + '</button>' +
              '<button type="button" class="btn btn-primary btn-sm" id="hqDefCommFormSaveBtn" data-pg-ui-t="저장">' + escUi(L('저장')) + '</button>' +
              '</div>';
            } }]
          ]
        },
        {
          title: '가맹점 수수료 정책',
          notice: '위 [저장] 후 목록이 갱신됩니다. 수치 열은 총본사~영업점 합계(가맹 적용분) 기준입니다. 체크 후 [수정] 또는 행 클릭으로 폼에 불러옵니다. [신규정책]으로 초기화한 뒤 입력·저장하면 코드가 자동 부여되어 목록에 나타납니다(예: A 다음 H). 정책이 많으면 아래 표 영역을 <strong>세로·가로 스크롤</strong>하여 확인하세요. 체크한 항목만 [선택 정책 삭제]할 수 있습니다(여러 건 가능). 표 머리의 체크박스로 전체 선택·해제합니다.',
          rows: [[{
            type: 'customHtml',
            col: 12,
            html: function () {
              return '<div id="hqDefaultCommissionFlash" class="alert alert-dismissible d-none mb-3" role="alert">' +
              '<span data-pg-banner-text></span>' +
              '<button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="' + escUi(L('닫기')) + '"></button></div>' +
              '<p class="small text-muted mb-2 mb-md-1" data-pg-ui-html="헤더 1행은 <strong>수수료 고정</strong>·<strong>수수료 %</strong>·<strong>담보율</strong>·<strong>기타</strong> 묶음입니다. <strong>수수료 %</strong> 열은 숫자만 표시(단위 % 생략). 결제·USDT·FX는 승인금액 기준 %이며, <strong>3DS</strong>는 정책통화 기준 <strong>건당 고정</strong>입니다. 담보(롤링) 비율은 승인금액 기준 %입니다. 열이 많아 표에 <strong>최소 너비</strong>를 두었으며, 화면이 좁으면 아래 표 영역을 <strong>가로 스크롤</strong>하여 전체 열을 볼 수 있습니다. 정책 행이 많으면 같은 영역을 <strong>세로 스크롤</strong>하여 G 이후 코드도 확인할 수 있습니다.">' + L('헤더 1행은 <strong>수수료 고정</strong>·<strong>수수료 %</strong>·<strong>담보율</strong>·<strong>기타</strong> 묶음입니다. <strong>수수료 %</strong> 열은 숫자만 표시(단위 % 생략). 결제·USDT·FX는 승인금액 기준 %이며, <strong>3DS</strong>는 정책통화 기준 <strong>건당 고정</strong>입니다. 담보(롤링) 비율은 승인금액 기준 %입니다. 열이 많아 표에 <strong>최소 너비</strong>를 두었으며, 화면이 좁으면 아래 표 영역을 <strong>가로 스크롤</strong>하여 전체 열을 볼 수 있습니다. 정책 행이 많으면 같은 영역을 <strong>세로 스크롤</strong>하여 G 이후 코드도 확인할 수 있습니다.') + '</p>' +
              '<div class="table-responsive table-scrollable border rounded hq-default-comm-policy-scroll">' +
              '<table class="table table-sm table-hover align-middle mb-0 hq-default-comm-policy-table table-no-col-resize">' +
              '<colgroup>' +
              '<col class="hq-def-comm-col" /><col class="hq-def-comm-col" /><col class="hq-def-comm-col" /><col class="hq-def-comm-col" /><col class="hq-def-comm-col" /><col class="hq-def-comm-col" />' +
              '<col class="hq-def-comm-col" /><col class="hq-def-comm-col" /><col class="hq-def-comm-col" /><col class="hq-def-comm-col" /><col class="hq-def-comm-col" /><col class="hq-def-comm-col" /><col class="hq-def-comm-col" /><col class="hq-def-comm-col" /><col class="hq-def-comm-col" /><col class="hq-def-comm-col" /><col class="hq-def-comm-col" /><col class="hq-def-comm-col" />' +
              '<col class="hq-def-comm-col" /><col class="hq-def-comm-col" /><col class="hq-def-comm-col" /><col class="hq-def-comm-col" />' +
              '<col class="hq-def-comm-col" /><col class="hq-def-comm-col" />' +
              '<col class="hq-def-comm-col" />' +
              '<col class="hq-def-comm-col" /><col class="hq-def-comm-col" /><col class="hq-def-comm-col" /><col class="hq-def-comm-col" />' +
              '<col class="hq-def-comm-col" />' +
              '</colgroup>' +
              '<thead class="table-light">' +
              '<tr>' +
              '<th rowspan="2" class="text-center align-middle hq-def-comm-th-chk">' +
              '<div class="hq-def-comm-th-chk-inner flex-column">' +
              '<input type="checkbox" class="form-check-input m-0" id="hqDefCommSelectAll" title="' + escUi(L('전체 선택')) + '" aria-label="' + escUi(L('전체 선택')) + '" />' +
              '<span class="hq-def-comm-th-chk-label">' + escUi(L('선택')) + '</span>' +
              '</div></th>' +
              '<th rowspan="2" class="text-center align-middle hq-def-comm-th-code" data-pg-ui-t="코드">' + escUi(L('코드')) + '</th>' +
              '<th rowspan="2" class="text-center align-middle hq-def-comm-th-name" data-pg-ui-t="이름">' + escUi(L('이름')) + '</th>' +
              '<th rowspan="2" class="text-center align-middle hq-def-comm-th-cb-zone small"><span data-pg-ui-t="차지백">' + escUi(L('차지백')) + '</span><br><span data-pg-ui-t="구간정책">' + escUi(L('구간정책')) + '</span></th>' +
              '<th rowspan="2" class="text-center align-middle hq-def-comm-th-deploy" data-pg-ui-t="적용">' + escUi(L('적용')) + '</th>' +
              '<th rowspan="2" class="text-center align-middle hq-def-comm-th-cur" data-pg-ui-t="통화">' + escUi(L('통화')) + '</th>' +
              '<th colspan="12" class="text-center align-middle small hq-def-comm-th-group border-start" data-pg-ui-t="수수료 고정">' + escUi(L('수수료 고정')) + '</th>' +
              '<th colspan="4" class="text-center align-middle small hq-def-comm-th-group border-start" data-pg-ui-t="수수료 %">' + escUi(L('수수료 %')) + '</th>' +
              '<th colspan="2" class="text-center align-middle small hq-def-comm-th-group border-start" data-pg-ui-t="담보율">' + escUi(L('담보율')) + '</th>' +
              '<th rowspan="2" class="text-center align-middle hq-def-comm-th-mon border-start" data-pg-ui-t="월간">' + escUi(L('월간')) + '</th>' +
              '<th colspan="4" class="text-center align-middle small hq-def-comm-th-group border-start" data-pg-ui-t="기타">' + escUi(L('기타')) + '</th>' +
              '<th rowspan="2" class="text-center align-middle hq-def-comm-th-upd text-nowrap border-start" data-pg-ui-t="일시">' + escUi(L('일시')) + '</th>' +
              '</tr>' +
              '<tr>' +
              '<th class="hq-def-comm-th-sub text-center border-start" data-pg-ui-t="건당">' + escUi(L('건당')) + '</th><th class="hq-def-comm-th-sub text-center" data-pg-ui-t="실패">' + escUi(L('실패')) + '</th><th class="hq-def-comm-th-sub text-center" data-pg-ui-t="정산">' + escUi(L('정산')) + '</th><th class="hq-def-comm-th-sub text-center" data-pg-ui-t="송금">' + escUi(L('송금')) + '</th><th class="hq-def-comm-th-sub text-center" data-pg-ui-t="U송금">' + escUi(L('U송금')) + '</th><th class="hq-def-comm-th-sub text-center" data-pg-ui-t="차지백">' + escUi(L('차지백')) + '</th><th class="hq-def-comm-th-sub text-center" data-pg-ui-t="취소">' + escUi(L('취소')) + '</th><th class="hq-def-comm-th-sub text-center" data-pg-ui-t="무효">' + escUi(L('무효')) + '</th><th class="hq-def-comm-th-sub text-center" data-pg-ui-t="수무효">' + escUi(L('수무효')) + '</th><th class="hq-def-comm-th-sub text-center" data-pg-ui-t="환불">' + escUi(L('환불')) + '</th><th class="hq-def-comm-th-sub text-center">3DS</th><th class="hq-def-comm-th-sub text-center" data-pg-ui-t="분할건">' + escUi(L('분할건')) + '</th>' +
              '<th class="hq-def-comm-th-sub text-center border-start" data-pg-ui-t="결제">' + escUi(L('결제')) + '</th><th class="hq-def-comm-th-sub text-center">USDT</th><th class="hq-def-comm-th-sub text-center">FX</th><th class="hq-def-comm-th-sub text-center" data-pg-ui-t="분할">' + escUi(L('분할')) + '</th>' +
              '<th class="hq-def-comm-th-sub text-center border-start" data-pg-ui-t="비율">' + escUi(L('비율')) + '</th><th class="hq-def-comm-th-sub text-center" data-pg-ui-t="일">' + escUi(L('일')) + '</th>' +
              '<th id="hqDefCommExtraHead1" class="hq-def-comm-th-sub text-center border-start" data-pg-ui-t="기타1">' + escUi(L('기타1')) + '</th><th id="hqDefCommExtraHead2" class="hq-def-comm-th-sub text-center" data-pg-ui-t="기타2">' + escUi(L('기타2')) + '</th><th id="hqDefCommExtraHead3" class="hq-def-comm-th-sub text-center" data-pg-ui-t="기타3">' + escUi(L('기타3')) + '</th><th id="hqDefCommExtraHead4" class="hq-def-comm-th-sub text-center" data-pg-ui-t="기타4">' + escUi(L('기타4')) + '</th>' +
              '</tr>' +
              '</thead>' +
              '<tbody id="hqDefaultCommissionPolicyList"></tbody></table>' +
              '<p class="small text-muted px-3 py-2 mb-0 d-none" id="hqDefaultCommissionPolicyListEmpty">' + escUi(L('등록된 템플릿이 없습니다. 위에서 [신규정책] 후 [저장]하세요.')) + '</p></div>' +
              '<div class="modal fade" id="hqDefaultCommissionDeleteModal" tabindex="-1" aria-hidden="true">' +
              '<div class="modal-dialog modal-dialog-centered"><div class="modal-content">' +
              '<div class="modal-header"><h5 class="modal-title">' + escUi(L('정책 삭제')) + '</h5><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="' + escUi(L('닫기')) + '"></button></div>' +
              '<div class="modal-body"><p class="mb-0" id="hqDefaultCommissionDeleteModalText"></p></div>' +
              '<div class="modal-footer">' +
              '<button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">' + escUi(L('취소')) + '</button>' +
              '<button type="button" class="btn btn-danger btn-sm" id="hqDefaultCommissionDeleteConfirmBtn">' + escUi(L('삭제')) + '</button>' +
              '</div></div></div></div>';
            }
          }]]
        }
      ],
      buttons: [
        { id: 'hqDefaultCommissionEditBtn', label: '수정', cls: 'btn-outline-primary' },
        { id: 'hqDefaultCommissionTemplateDeleteBtn', label: '선택 정책 삭제', cls: 'btn-outline-danger' }
      ]
    },
    '/hq/chargebackPolicy': {
      isForm: true,
      formSections: [{
        title: '차지백설정',
        notice: '월간 환불·강제환불(거래 상태 30·31) 건수로 구간을 정합니다. 해당 월 누적 건수에 맞는 첫 구간의 건당 금액을, 정산 배치에 포함된 환불·강제환불 건수만큼 곱해 합산합니다. 구간 정책을 쓰지 않으면 [수수료설정]의 차지백수수료(건)만 적용됩니다.',
        rows: [[{
          type: 'customHtml',
          col: 12,
          html: function () {
            return '<div id="hqChargebackPolicyFlash" class="alert alert-dismissible d-none mb-3" role="alert"><span data-pg-banner-text></span><button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="' + escUi(L('닫기')) + '"></button></div>' +
            '<div class="row g-3"><div class="col-12 col-lg-7"><div class="card h-100"><div class="card-header py-2 small fw-semibold" data-pg-ui-t="저장된 유형">' + escUi(L('저장된 유형')) + '</div><div class="card-body p-2">' +
            '<div class="table-responsive border rounded" style="max-height:480px;overflow-y:auto"><table class="table table-sm table-hover align-middle mb-0 hq-chargeback-policy-saved-table"><thead class="table-light"><tr><th class="text-nowrap">ID</th><th data-pg-ui-t="이름">' + escUi(L('이름')) + '</th><th class="text-nowrap" data-pg-ui-t="기준통화">' + escUi(L('기준통화')) + '</th><th class="hq-cb-list-remark-th" data-pg-ui-t="비고">' + escUi(L('비고')) + '</th></tr></thead><tbody id="hqChargebackPolicyListTbody"><tr><td colspan="4" class="text-muted text-center small">' + escUi(L('불러오는 중…')) + '</td></tr></tbody></table></div>' +
            '<button type="button" class="btn btn-success btn-sm mt-2 w-100" id="hqChargebackPolicyNewBtn" data-pg-ui-t="새 유형">' + escUi(L('새 유형')) + '</button></div></div></div>' +
            '<div class="col-12 col-lg-5"><div class="card h-100"><div class="card-header py-2 small fw-semibold" data-pg-ui-t="편집">' + escUi(L('편집')) + '</div><div class="card-body p-2">' +
            '<input type="hidden" id="hqCbPolId" value="" />' +
            '<div class="mb-2"><label class="form-label small mb-0" for="hqCbPolName" data-pg-ui-t="이름">' + escUi(L('이름')) + '</label><input type="text" class="form-control form-control-sm" id="hqCbPolName" maxlength="120" data-pg-ui-placeholder="예: 월간 차지백 단가표" placeholder="' + escUi(L('예: 월간 차지백 단가표')) + '" /></div>' +
            '<div class="mb-2"><label class="form-label small mb-0" for="hqCbPolCurrencyCode" data-pg-ui-t="기준통화">' + escUi(L('기준통화')) + '</label><select class="form-select form-select-sm" id="hqCbPolCurrencyCode">' +
            '<option value="KRW">KRW</option><option value="USD">USD</option><option value="JPY">JPY</option><option value="EUR">EUR</option>' +
            '<option value="CNY">CNY</option><option value="THB">THB</option><option value="VND">VND</option><option value="GBP">GBP</option>' +
            '<option value="TWD">TWD</option><option value="HKD">HKD</option><option value="USDT">USDT</option></select>' +
            '<p class="small text-muted mb-0 mt-1" data-pg-ui-t="구간 건당 금액의 표시·집계 단위 안내용입니다.">' + escUi(L('구간 건당 금액의 표시·집계 단위 안내용입니다.')) + '</p></div>' +
            '<div class="mb-2"><label class="form-label small mb-0" for="hqCbPolRemark" data-pg-ui-t="비고">' + escUi(L('비고')) + '</label><textarea class="form-control form-control-sm" id="hqCbPolRemark" rows="2" data-pg-ui-placeholder="내부 메모" placeholder="' + escUi(L('내부 메모')) + '"></textarea></div>' +
            '<div class="d-flex align-items-center justify-content-between mb-1"><span class="small fw-semibold" data-pg-ui-t="구간 (해당 월 강제환불 31 건수)">' + escUi(L('구간 (해당 월 강제환불 31 건수)')) + '</span><button type="button" class="btn btn-outline-secondary btn-sm" id="hqCbPolAddTierBtn" data-pg-ui-t="행 추가">' + escUi(L('행 추가')) + '</button></div>' +
            '<p class="small text-muted mb-2" data-pg-ui-t="sort 오름차순으로 검사하며, 건수 ≥ 최소건 and (최대건 비움 = 상한 없음 or 건수 ≤ 최대건) 인 첫 행이 적용됩니다.">' + escUi(L('sort 오름차순으로 검사하며, 건수 ≥ 최소건 and (최대건 비움 = 상한 없음 or 건수 ≤ 최대건) 인 첫 행이 적용됩니다.')) + '</p>' +
            '<div class="table-responsive border rounded mb-3"><table class="table table-sm mb-0 align-middle" id="hqCbPolTierTable"><thead class="table-light"><tr><th style="width:72px" data-pg-ui-t="sort">' + escUi(L('sort')) + '</th><th style="width:100px" data-pg-ui-t="최소건">' + escUi(L('최소건')) + '</th><th style="width:100px" data-pg-ui-t="최대건">' + escUi(L('최대건')) + '</th><th data-pg-ui-t="건당금액">' + escUi(L('건당금액')) + '</th><th style="width:52px"></th></tr></thead><tbody id="hqCbPolTierTbody"></tbody></table></div>' +
            '<div class="d-flex flex-wrap gap-2"><button type="button" class="btn btn-primary btn-sm" id="hqChargebackPolicySaveBtn" data-pg-ui-t="저장">' + escUi(L('저장')) + '</button>' +
            '<button type="button" class="btn btn-outline-danger btn-sm" id="hqChargebackPolicyDeleteBtn" data-pg-ui-t="삭제">' + escUi(L('삭제')) + '</button></div></div></div></div></div>';
          }
        }]]
      }],
      buttons: [{ id: 'hqChargebackPolicyReloadBtn', label: '목록 새로고침', cls: 'btn-outline-secondary' }]
    },
    '/hq/pgAgencyCostPolicy': {
      isForm: true,
      formSections: [{
        title: '대행수수료설정',
        notice: '가맹 수수료(수수료설정)와 별도로, PG사와의 계약 원가·정산 주기를 PG코드(MID) 단위로 관리합니다. 운영 PG결제·PG정산 화면(예정)에서 이 정책을 참조합니다.',
        rows: [[{ type: 'customHtml', col: 12, html: hqPgAgencyCostPolicyPageHtml }]]
      }],
      buttons: [{ id: 'hqPgAgencyCostReloadBtn', label: '목록 새로고침', cls: 'btn-outline-secondary' }]
    },
    '/hq/settlementAdmin': {
      hideListGrid: true,
      summary: [],
      buttons: [],
      staticHtml: hqSettlementAdminStaticHtml
    },
    '/hq/receivableRecoverySettings': {
      hideListGrid: true,
      summary: [],
      buttons: [],
      staticHtml: hqReceivableRecoveryStaticHtml
    },
    '/hq/businessDaySetting': {
      isForm: true,
      formSections: [
        {
          title: '영업일 설정',
          notice: 'KR/US/JP/TH/CN 및 GLOBAL(토·일만 휴일) 기준으로 이름별 영업일 설정을 저장합니다. CN은 중국 국무원 공지 연휴(조정일 포함)를 반영합니다. 신규 저장 시 등록자(로그인 아이디)가 자동 기록됩니다. 업체(본사) 정보에서 영업일 설정 이름을 선택하면 해당 국가·휴일이 적용됩니다. 휴일 구간은 아래에서 추가하며, [공휴일 프리셋 불러오기]로 일자를 합칠 수 있습니다. 목록 집계: 공식공휴일=저장된 비영업일 중 토·일·해당국 법정(프리셋) 일자, 추가공휴일=그 외 저장 일자, 총공휴일=저장된 비영업 일수(공식+추가).',
          rows: [
            [{ label: '이름', type: 'text', name: 'hqBizdayProfileName', col: 4, placeholder: '예: KR 기본 영업일' },
             { label: '기준국가선택', type: 'select', name: 'holidayCountryCodes', options: [{ v: 'KR', t: 'KR (대한민국)' }, { v: 'US', t: 'US (미국)' }, { v: 'JP', t: 'JP (일본)' }, { v: 'TH', t: 'TH (태국)' }, { v: 'CN', t: 'CN (중국)' }, { v: 'GLOBAL', t: 'GLOBAL (토·일만 휴일)' }], col: 3 }],
            [{ type: 'customHtml', html: hqBizdayManualUiHtml, col: 12 }],
            [{ type: 'customHtml', html: hqHolidayUiHtml, col: 12 }],
            [{ type: 'customHtml', html: hqBizdayProfileListHtml, col: 12 }]
          ]
        }
      ],
      buttons: []
    },
    '/hq/notifyEnv': {
      isForm: true,
      formSections: [
        {
          title: '전산 노티 수신 (NOTI 전산노티대상 연동)',
          notice: '권장: 아래 「노티 수신 URL(미들웨어)」을 ziobiz/NOTI·ChillPay·JPAY 등에 등록하세요(/api/middleware/notify/v1/pg-notify/…). 레거시 open 경로는 하단 참고 필드와 동일 처리입니다. 경로 끝 토큰으로 무단 호출을 막습니다. 운영 배포 후 [공개 URL 베이스]에 https://실제도메인 을 넣으면 안내 URL이 고정됩니다. 배포설정 > API연동설정에서 연동용도가 노티(등)인 PG는 노티를 MID+루트로 분기합니다. URL 결제만인 PG는 동일 MID가 여러 가맹점이면 본문에 업체코드(compId) 또는 icopayCompId= 가 필요합니다.',
          rows: [
            [{ label: '노티 수신 URL(미들웨어 권장)', type: 'text', name: 'notifyIngressUrl', col: 6, readonly: true }],
            [{ label: '노티 수신 URL(open·레거시)', type: 'text', name: 'notifyIngressUrlOpen', col: 6, readonly: true }],
            [{ label: 'Ingress 토큰(참고)', type: 'text', name: 'ingressToken', col: 6, readonly: true }],
            [{ label: '공개 URL 베이스', type: 'text', name: 'publicBaseUrl', col: 6, placeholder: '비우면 브라우저 접속 기준(예: http://localhost:8080)' }],
            [{ label: '노티 성공 응답 본문', type: 'textarea', name: 'notifyOkResponse', col: 6, placeholder: '{"result":"OK"}' }]
          ]
        },
        {
          title: '총판 노티 대상 생성',
          notice: '먼저 [연결 총판]에서 총판을 선택한 뒤 노티 대상명을 입력하고 [노티자동생성]을 누르세요. CALLBACK·RESULT URL이 발급되며 선택한 총판과 즉시 연결됩니다. 이때 해당 총판 업체 상세의 필수 노티(URL 1·2)도 발급 URL로 자동 반영됩니다(보조 URL 3·4는 유지). 목록의 [연결수정]으로 연결 총판을 바꾸면 동일하게 필수 노티가 갱신됩니다. 총판 저장 시 노티 URL에 동일 주소를 넣어 두면 저장 시 연결이 유지·갱신됩니다.',
          rows: [
            [{ label: '연결 총판', type: 'select', name: 'notifyTargetBoundOrgUnitId', col: 4, options: [{ v: '', t: '선택하세요' }] }],
            [{ label: '노티 대상명', type: 'text', name: 'newNotifyTargetName', col: 2, placeholder: '예: 총판A 수신', button: '노티자동생성', blockExtraClass: 'hq-notify-new-target-name-col' }],
            [{ type: 'customHtml', col: 12, html: '<div class="table-responsive hq-notify-target-table-wrap table-no-col-resize-wrap"><table class="table table-sm table-bordered align-middle mb-1 table-no-col-resize" id="hqNotifyTargetTable"><thead class="table-light"><tr><th class="text-center" style="width:52px">No.</th><th class="text-center hq-notify-created-th" style="width:9.5rem;min-width:9rem" data-pg-ui-t="생성일시">생성일시</th><th class="hq-notify-target-name-th" data-pg-ui-t="노티 대상명">노티 대상명</th><th class="text-center hq-notify-bound-org-th" style="width:8rem;min-width:7rem" data-pg-ui-t="연결 총판">연결 총판</th><th data-pg-ui-t="노티 주소">노티 주소</th><th class="text-center" style="width:88px" data-pg-ui-t="복사">복사</th><th class="hq-notify-channel-th text-center" style="width:132px;min-width:132px" data-pg-ui-t="노티 성격">노티 성격</th><th class="text-center" style="width:100px" data-pg-ui-t="삭제">삭제</th></tr></thead><tbody id="hqNotifyTargetTbody"></tbody></table><p class="text-muted small mb-0 d-none" id="hqNotifyTargetEmpty" data-pg-ui-t="등록된 노티 대상이 없습니다.">등록된 노티 대상이 없습니다.</p></div>' }]
          ]
        }
      ],
      buttons: [{ id: 'hqNotifyRegenTokenBtn', label: '토큰 재발급', cls: 'btn-warning' }, { id: 'hqNotifyEnvSaveBtn', label: '저장', cls: 'btn-primary' }]
    },
    '/hq/userSettings': {
      isForm: true,
      formSections: [
        {
          title: '로그인·OTP 정책 (ziobiz/NOTI 계정관리 대응)',
          notice: '모든 사용자에 OTP를 요구할지 본사(총본사) 설정에서 통일합니다. OTP 필수 시 로그인·등록 단계에서 OTP 검증을 붙일 수 있습니다(연동 예정). 사용자관리 그리드의 OTP 등록 여부와 연계됩니다. 저장은 노티·결제환경 설정(tb_hq_notify_env_config)과 동일 API를 사용합니다.',
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
          title: '담당자(보조) 메뉴 기본 권한',
          notice: '카탈로그의 일반(데스크톱) 메뉴(URL)를 조직 단계(총본사~가맹점)별로 담당자 역할 상한을 둡니다. 태블릿 전용 메뉴는 아래 「태블릿모드」에서 설정합니다. 본사권한설정과 같이 조직 단계 탭을 선택한 뒤 표를 수정합니다. 본사권한설정의 개별 조직 「담당자 권한그룹별 메뉴」 저장값이 여기 기본값보다 우선합니다.',
          rows: [
            [{
              type: 'customHtml',
              col: 12,
              html: '<div id="hqUserAsstMatrixRoot" class="hq-user-asst-matrix-root border rounded p-2 bg-body-tertiary">' +
                '<p class="small text-muted mb-2" data-pg-ui-t="조직 단계(본사권한설정과 동일)">조직 단계(본사권한설정과 동일)</p>' +
                '<ul class="nav nav-pills flex-wrap gap-1 mb-2 org-perm-level-tabs hq-user-asst-level-tabs" id="hqUserAsstOrgLevelTabs" role="tablist"></ul>' +
                '<div class="table-responsive hq-user-asst-matrix-scroll" style="max-height:28rem">' +
                '<table class="table table-sm table-bordered align-middle mb-0 bg-white" id="hqUserAsstMatrixTable">' +
                '<thead class="table-light sticky-top"><tr><th style="min-width:7rem" data-pg-ui-t="대메뉴">대메뉴</th><th style="min-width:8rem" data-pg-ui-t="메뉴">메뉴</th>' +
                '<th class="text-center text-nowrap p-1" style="min-width:5.5rem" data-pg-ui-t="MANAGER">MANAGER</th><th class="text-center text-nowrap p-1" style="min-width:5.5rem" data-pg-ui-t="OPERATOR">OPERATOR</th>' +
                '<th class="text-center text-nowrap p-1" style="min-width:5.5rem" data-pg-ui-t="SETTLEMENT">SETTLEMENT</th><th class="text-center text-nowrap p-1" style="min-width:5.5rem" data-pg-ui-t="TECH">TECH</th>' +
                '<th class="text-center text-nowrap p-1" style="min-width:5.5rem" data-pg-ui-t="CHATBOT">CHATBOT</th></tr></thead><tbody id="hqUserAsstMatrixTbody"></tbody></table></div>' +
                '<div id="hqUserAsstBulkPanel" class="mt-3 p-2 border rounded bg-white small">' +
                '<div class="fw-semibold mb-2" data-pg-ui-t="대메뉴·역할 일괄 적용">대메뉴·역할 일괄 적용</div>' +
                '<div class="row g-2 align-items-end">' +
                '<div class="col-12 col-lg-4"><div class="form-label mb-1" data-pg-ui-t="적용 대상 조직 단계">적용 대상 조직 단계</div><div id="hqUserAsstBulkOrgChecks" class="d-flex flex-wrap gap-2"></div></div>' +
                '<div class="col-6 col-md-3 col-lg-2"><label class="form-label mb-0" for="hqUserAsstBulkParentSel" data-pg-ui-t="대메뉴">대메뉴</label>' +
                '<select id="hqUserAsstBulkParentSel" class="form-select form-select-sm"></select></div>' +
                '<div class="col-12 col-md-6 col-lg-4"><div class="form-label mb-1" data-pg-ui-t="역할">역할</div><div id="hqUserAsstBulkRoleChecks" class="d-flex flex-wrap gap-2"></div></div>' +
                '<div class="col-6 col-md-3 col-lg-2"><label class="form-label mb-0" for="hqUserAsstBulkPermSel" data-pg-ui-t="권한">권한</label>' +
                '<select id="hqUserAsstBulkPermSel" class="form-select form-select-sm"></select></div>' +
                '<div class="col-6 col-md-3 col-lg-2 d-grid"><button type="button" class="btn btn-sm btn-outline-primary mt-3 mt-lg-4" id="hqUserAsstBulkApplyBtn" data-pg-ui-t="적용">적용</button></div></div>' +
                '<p class="text-muted mb-0 mt-2 small" data-pg-ui-t="체크한 조직 단계·역할에만 동일 권한이 채워집니다. 대메뉴에서 「전체 메뉴」를 고르면 카탈로그 전체 URL이 대상입니다.">체크한 조직 단계·역할에만 동일 권한이 채워집니다. 대메뉴에서 「전체 메뉴」를 고르면 카탈로그 전체 URL이 대상입니다.</p></div></div>'
            }]
          ]
        },
        {
          title: '태블릿모드 (담당자 권한)',
          notice: '태블릿 로그인·사이드바에 노출되는 메뉴만 담당자 역할별 기본 권한을 설정합니다. [태블릿설정]에서 해당 조직 단계에 노출하지 않은 메뉴는 접근불가(NONE)로 고정되며 선택이 비활성화됩니다(태블릿설정이 우선). 아래 조직 단계 탭으로 편집할 단계를 선택합니다.',
          rows: [
            [{
              type: 'customHtml',
              col: 12,
              html: '<div id="hqUserTabletMatrixRoot" class="hq-user-tablet-matrix-root border rounded p-2 bg-body-tertiary">' +
                '<p class="small text-muted mb-2" data-pg-ui-t="조직 단계(태블릿 권한 편집)">조직 단계(태블릿 권한 편집)</p>' +
                '<ul class="nav nav-pills flex-wrap gap-1 mb-2 org-perm-level-tabs hq-user-tablet-level-tabs" id="hqUserTabletOrgLevelTabs" role="tablist"></ul>' +
                '<div class="table-responsive hq-user-tablet-matrix-scroll" style="max-height:22rem">' +
                '<table class="table table-sm table-bordered align-middle mb-0 bg-white" id="hqUserTabletMatrixTable">' +
                '<thead class="table-light sticky-top"><tr><th style="min-width:10rem" data-pg-ui-t="메뉴">메뉴</th>' +
                '<th class="text-center text-nowrap p-1" style="min-width:5.5rem" data-pg-ui-t="MANAGER">MANAGER</th><th class="text-center text-nowrap p-1" style="min-width:5.5rem" data-pg-ui-t="OPERATOR">OPERATOR</th>' +
                '<th class="text-center text-nowrap p-1" style="min-width:5.5rem" data-pg-ui-t="SETTLEMENT">SETTLEMENT</th><th class="text-center text-nowrap p-1" style="min-width:5.5rem" data-pg-ui-t="TECH">TECH</th>' +
                '<th class="text-center text-nowrap p-1" style="min-width:5.5rem" data-pg-ui-t="CHATBOT">CHATBOT</th></tr></thead><tbody id="hqUserTabletMatrixTbody"></tbody></table></div>' +
                '<div id="hqUserTabletBulkPanel" class="mt-3 p-2 border rounded bg-white small">' +
                '<div class="fw-semibold mb-2" data-pg-ui-t="태블릿 · 역할 일괄 적용">태블릿 · 역할 일괄 적용</div>' +
                '<div class="row g-2 align-items-end">' +
                '<div class="col-12 col-lg-5"><div class="form-label mb-1" data-pg-ui-t="적용 대상 조직 단계">적용 대상 조직 단계</div><div id="hqUserTabletBulkOrgChecks" class="d-flex flex-wrap gap-2"></div></div>' +
                '<div class="col-12 col-md-6 col-lg-4"><div class="form-label mb-1" data-pg-ui-t="역할">역할</div><div id="hqUserTabletBulkRoleChecks" class="d-flex flex-wrap gap-2"></div></div>' +
                '<div class="col-6 col-md-3 col-lg-2"><label class="form-label mb-0" for="hqUserTabletBulkPermSel" data-pg-ui-t="권한">권한</label>' +
                '<select id="hqUserTabletBulkPermSel" class="form-select form-select-sm"></select></div>' +
                '<div class="col-6 col-md-3 col-lg-2 d-grid"><button type="button" class="btn btn-sm btn-outline-primary mt-3 mt-lg-4" id="hqUserTabletBulkApplyBtn" data-pg-ui-t="적용">적용</button></div></div>' +
                '<p class="text-muted mb-0 mt-2 small" data-pg-ui-t="체크한 조직 단계·역할에만 동일 권한이 채워집니다. 태블릿설정에서 미노출된 메뉴는 적용되지 않습니다.">체크한 조직 단계·역할에만 동일 권한이 채워집니다. 태블릿설정에서 미노출된 메뉴는 적용되지 않습니다.</p></div></div>'
            }]
          ]
        }
      ],
      buttons: [{ id: 'hqUserSettingsSaveBtn', label: '저장', cls: 'btn-primary' }]
    },
    '/hq/notifyMapping': {
      isForm: true,
      formHtmlId: 'hqNotifyMappingForm',
      formSections: [
        {
          title: '노티매핑설정 (GUI)',
          notice: '노티매핑설정 안내',
          rows: [
            [{
              type: 'customHtml',
              col: 12,
              html: '<div id="hqNotifyMappingUiRoot" class="hq-notify-mapping-ui"></div>' +
                '<textarea name="mappingDefinitionJson" id="hqNotifyMappingJsonTa" class="d-none" rows="4" autocomplete="off"></textarea>' +
                '<div class="row g-2 mt-2 align-items-end">' +
                '<div class="col-md-4"><label class="form-label small mb-0" data-pg-ui-t="최종 수정일시">최종 수정일시</label>' +
                '<input type="text" name="updatedAt" class="form-control form-control-sm" readonly></div>' +
                '<div class="col-md-8"><button type="button" class="btn btn-sm btn-outline-secondary" id="hqNotifyMappingToggleJsonBtn" data-pg-ui-t="전문가용: JSON 직접 편집">전문가용: JSON 직접 편집</button></div></div>' +
                '<div id="hqNotifyMappingJsonEditorWrap" class="d-none mt-2">' +
                '<label class="form-label small" data-pg-ui-t="매핑 정의 JSON (필드명)">매핑 정의 JSON (필드명)</label>' +
                '<textarea id="hqNotifyMappingJsonVisible" class="form-control font-monospace small" rows="14" spellcheck="false"></textarea></div>'
            }]
          ]
        }
      ],
      buttons: [{ id: 'hqNotifyMappingSaveBtn', label: '저장', cls: 'btn-primary' }]
    },
    '/hq/notifyInbound': {
      isForm: true,
      formSections: [
        {
          title: '노티 수령 정보',
          notice: '노티미들웨어·PG(칠페이 등)가 본 시스템의 노티 수신 URL(<code>/api/open/pg-notify/…</code>)로 전송한 요청을 저장한 로그입니다. 목록의 채널 열은 수신 경로 정보 표시용입니다. 대상코드·채널은 신규 수신 건부터 채워집니다(V72). 노티 대상에 연결 총판이 있으면 동일 MID라도 그 총판 트리 안에서만 분기하며, 총판 기준통화와 본문 통화가 다르면 처리 열에 통화불일치(수신경로)로 격리됩니다. <strong>수신성격</strong>은 NOTI가 요청 시 <code>X-Icopay-Notify-Delivery: LIVE|RETRY</code> 또는 <code>X-Noti-Attempt</code>(1=라이브, 2+=재전송) 헤더를 보낼 때만 구분되며, 없으면 「미표시」입니다. 바인딩·매핑을 고친 뒤 과거 건을 결제내역에 붙이려면 본문 보기 모달에서 <strong>본문을 수정·저장</strong>한 뒤 <strong>결제내역 재반영</strong>을 사용하세요(원문이 잘린 건은 불가). 공통 MID 재처리 시 본문에 <code>icopayCompId=업체코드</code> 를 추가하거나 재반영 업체코드 입력란을 사용하세요. 노티에 고객명·카드번호가 없을 때는 재반영 모달에서 <strong>재반영 고객명</strong>·<strong>재반영 카드번호</strong>를 입력한 뒤 재반영하면 결제내역에 반영됩니다.',
          rows: [
            [{ type: 'customHtml', col: 12, html: '<div class="row g-2 align-items-end mb-2 ni-inbound-toolbar">' +
              '<div class="col-6 col-md-2"><label class="form-label small mb-0" data-pg-ui-t="수신일(부터)">' + escUi(L('수신일(부터)')) + '</label><input type="date" lang="en-CA" name="niSearchFrom" class="form-control form-control-sm pg-date-input-iso" autocomplete="off"></div>' +
              '<div class="col-6 col-md-2"><label class="form-label small mb-0" data-pg-ui-t="수신일(까지)">' + escUi(L('수신일(까지)')) + '</label><input type="date" lang="en-CA" name="niSearchTo" class="form-control form-control-sm pg-date-input-iso" autocomplete="off"></div>' +
              '<div class="col-12 col-md-2"><label class="form-label small mb-0" data-pg-ui-t="검색 항목">' + escUi(L('검색 항목')) + '</label><select name="niSearchKey" class="form-select form-select-sm">' +
              '<option value="MID">MID</option><option value="ROUTE">ROUTE</option><option value="MERCHANT" data-pg-ui-t="가맹점코드">' + escUi(L('가맹점코드')) + '</option><option value="STATUS" data-pg-ui-t="처리상태">' + escUi(L('처리상태')) + '</option>' +
              '<option value="TXN_ID" data-pg-ui-t="승인번호">' + escUi(L('승인번호')) + '</option><option value="ORDER_NO">orderNo</option></select></div>' +
              '<div class="col-12 col-md-3"><label class="form-label small mb-0" data-pg-ui-t="검색어">' + escUi(L('검색어')) + '</label><input type="text" name="niSearchValue" class="form-control form-control-sm" data-pg-ui-placeholder="부분 일치" placeholder="' + escUi(L('부분 일치')) + '" autocomplete="off"></div>' +
              '<div class="col-6 col-md-2"><label class="form-label small mb-0" data-pg-ui-t="채널">' + escUi(L('채널')) + '</label><select name="niSearchChannelType" class="form-select form-select-sm">' +
              '<option value="" data-pg-ui-t="전체">' + escUi(L('전체')) + '</option><option value="CALL" data-pg-ui-t="CALL (Callback URL)">' + escUi(L('CALL (Callback URL)')) + '</option><option value="RESULT" data-pg-ui-t="RESULT (Result URL)">' + escUi(L('RESULT (Result URL)')) + '</option><option value="BOTH" data-pg-ui-t="BOTH (전체)">' + escUi(L('BOTH (전체)')) + '</option></select></div>' +
              '<div class="col-6 col-md-1 d-grid"><label class="form-label small mb-0 d-none d-md-block">&nbsp;</label><button type="button" id="hqNotifyInboundSearchBtn" class="btn btn-primary btn-sm" data-pg-ui-t="조회">' + escUi(L('조회')) + '</button></div></div>' +
              '<div class="table-responsive border rounded"><table class="table table-sm table-bordered align-middle mb-0" id="hqNotifyInboundTable">' +
              '<thead class="table-light"><tr><th style="width:3.5rem">ID</th><th class="text-nowrap" style="width:10rem" data-pg-ui-t="수신시각">수신시각</th><th style="width:4.5rem" data-pg-ui-t="채널">채널</th><th style="width:5.5rem" data-pg-ui-t="대상코드">대상코드</th><th style="width:7rem">MID</th><th style="width:4rem" data-pg-ui-t="루트">루트</th>' +
              '<th style="width:7rem" data-pg-ui-t="승인번호">승인번호</th><th style="width:7rem" data-pg-ui-t="가맹점코드">가맹점코드</th><th style="width:7.5rem" data-pg-ui-t="결제·처리">결제·처리</th><th style="width:5.5rem" data-pg-ui-t="수신성격">수신성격</th><th class="hq-ni-th-error" style="width:11rem;max-width:11rem" data-pg-ui-t="오류메시지">오류메시지</th><th style="min-width:14rem" data-pg-ui-t="본문 미리보기">본문 미리보기</th><th class="text-center" style="width:4rem" data-pg-ui-t="보기">보기</th></tr></thead>' +
              '<tbody id="hqNotifyInboundTbody"><tr><td colspan="13" class="text-center text-muted py-4">' + escUi(L('[조회]를 누르세요.')) + '</td></tr></tbody></table></div>' +
              '<div class="pagination-row mt-2">' +
              '<div class="pagination-view-at-once">' +
              '<span class="pagination-label" data-pg-ui-t="한 번에 보기:">' + escUi(L('한 번에 보기:')) + '</span>' +
              '<div class="pagination-size-options">' +
              '<button type="button" class="pagination-size-opt pagination-size-opt--active" data-size="25">25</button>' +
              '<button type="button" class="pagination-size-opt" data-size="50">50</button>' +
              '<button type="button" class="pagination-size-opt" data-size="100">100</button>' +
              '<button type="button" class="pagination-size-opt" data-size="200">200</button>' +
              '<button type="button" class="pagination-size-opt" data-size="400">400</button>' +
              '<button type="button" class="pagination-size-opt" data-size="500">500</button>' +
              '<button type="button" class="pagination-size-opt" data-size="1000">1000</button>' +
              '</div>' +
              '<span class="pagination-total"><span data-pg-ui-t="건 (총">' + escUi(L('건 (총')) + '</span> <span id="totalElementsCount">0</span><span data-pg-ui-t="건)">' + escUi(L('건)')) + '</span></span>' +
              '</div>' +
              '<input type="hidden" id="recordsPerPage" value="25">' +
              '<input type="hidden" id="pageCnt" value="1">' +
              '<span id="totalPageCount" style="display:none">1</span>' +
              '<div class="pagination-center"><div class="pagination-pages" id="paging_hq_notifyInbound"></div></div>' +
              '</div>' +
              '<div class="modal fade" id="hqNiDetailModal" tabindex="-1" aria-labelledby="hqNiDetailModalLabel" aria-hidden="true">' +
              '<div class="modal-dialog modal-xl modal-dialog-scrollable"><div class="modal-content">' +
              '<div class="modal-header py-2"><h5 class="modal-title" id="hqNiDetailModalLabel">' + escUi(L('노티 원문')) + '</h5>' +
              '<button type="button" class="btn-close" id="hqNiDetailCloseX" aria-label="' + escUi(L('닫기')) + '"></button></div>' +
              '<div class="modal-body"><p class="small text-muted mb-2" id="hqNiDetailMeta"></p>' +
              '<div class="d-flex flex-wrap gap-2 mb-2 align-items-end">' +
              '<div><label class="form-label small mb-0" for="hqNiReplayCompId" data-pg-ui-t="재반영 업체코드(icopayCompId)">재반영 업체코드(icopayCompId)</label>' +
              '<input type="text" class="form-control form-control-sm" id="hqNiReplayCompId" maxlength="20" placeholder="6000000041" style="min-width:9rem"></div>' +
              '<div><label class="form-label small mb-0" for="hqNiReplayCustomerNm" data-pg-ui-t="재반영 고객명">재반영 고객명</label>' +
              '<input type="text" class="form-control form-control-sm" id="hqNiReplayCustomerNm" maxlength="200" data-pg-ui-placeholder="재반영 고객명 placeholder" placeholder="예: 山田 太郎" style="min-width:10rem"></div>' +
              '<div><label class="form-label small mb-0" for="hqNiReplayCustomerEmail" data-pg-ui-t="재반영 이메일">재반영 이메일</label>' +
              '<input type="text" class="form-control form-control-sm" id="hqNiReplayCustomerEmail" maxlength="100" data-pg-ui-placeholder="재반영 이메일 placeholder" placeholder="test01@example.com" style="min-width:12rem"></div>' +
              '<div><label class="form-label small mb-0" for="hqNiReplayCardPan" data-pg-ui-t="재반영 카드번호">재반영 카드번호</label>' +
              '<input type="text" class="form-control form-control-sm" id="hqNiReplayCardPan" maxlength="32" data-pg-ui-placeholder="재반영 카드번호 placeholder" placeholder="489788***9416" style="min-width:11rem"></div>' +
              '</div>' +
              '<label class="form-label small mb-0" for="hqNiDetailBody" data-pg-ui-t="수신 본문 (편집 가능)">수신 본문 (편집 가능)</label>' +
              '<textarea id="hqNiDetailBody" class="form-control font-monospace small" rows="16" spellcheck="false"></textarea></div>' +
              '<div class="modal-footer py-2 border-top d-flex gap-2 justify-content-end flex-wrap">' +
              '<button type="button" class="btn btn-outline-primary btn-sm" id="hqNiSaveBodyBtn">' + escUi(L('본문 저장')) + '</button>' +
              '<button type="button" class="btn btn-primary btn-sm" id="hqNiReplayBtn">' + escUi(L('결제내역 재반영')) + '</button>' +
              '<button type="button" class="btn btn-secondary btn-sm" id="hqNiDetailCloseBtn">' + escUi(L('닫기')) + '</button></div>' +
              '</div></div></div>' }]
          ]
        }
      ],
      buttons: []
    },
    '/hq/ledgerSysSettings': {
      isForm: true,
      formSections: [
        {
          title: '시간 및 동기화 설정',
          notice: 'ziobiz/NOTI 노티미들웨어의 시스템·환경설정(시간·NTP·동기화)과 동일 목적입니다. 실제 OS 시각 동기화는 VPS에서 chrony/systemd-timesyncd 등으로 수행하고, 여기 표준시는 전산 배치·목록 표시·결제 후속조치(무효·이메일무효) 경과 판단의 기준 ZoneId로 사용합니다. 신규·미설정 시 기본은 태국(Asia/Bangkok)입니다.',
          rows: [
            [{ label: '표준 시간대 (IANA)', type: 'select', name: 'displayTimezone', col: 5, options: HQ_LEDGER_DISPLAY_TZ_OPTIONS }],
            [{ label: 'NTP 동기화 사용', type: 'select', name: 'ntpSyncEnabledYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 2 },
             { label: '동기화 주기(분)', type: 'number', name: 'timeSyncIntervalMin', col: 2, placeholder: '예: 60' }],
            [{ label: 'NTP 서버 목록', type: 'text', name: 'ntpServerList', col: 8, placeholder: '쉼표 구분, 예: pool.ntp.org, time.google.com' }],
            [{ label: '서버 기준 시각(조회 시점)', type: 'text', name: 'serverTimeIso', col: 6, readonly: true }],
            [{ label: '적용 ZoneId', type: 'text', name: 'serverZoneId', col: 4, readonly: true }]
          ]
        },
        {
          title: '데이터 보관 기간',
          notice: '쌓이는 데이터 유형별로 DB·로그·버퍼 보관 목표 일수를 지정합니다. 표에는 업체정보(등록)·업체관리·정산관리·가맹점 정산내역(수수료내역)·정산 리포트 등 모듈별 유형이 포함됩니다. 「자동삭제」를 켠 항목만 매일 새벽 스케줄로 초과분 삭제를 시도합니다(스케줄 대상만 체크 가능). 그 외 유형은 보관 목표(일)만 저장됩니다. 아래 표는 하단 「수수료·정산 로직」과 같은 테이블 래핑(둥근 테두리·작은 표 스타일)을 사용합니다. 상단 빨간 「전체 데이터 초기화」는 보관 일수와 별도로, 등록된 조직·가맹 프로필만 남기고 거래·정산·노티·수수료 정책 등 넓게 비웁니다. 파란 「정산 데이터 초기화」는 <strong>수수료내역·거래·본사 정산 설정·통합정산(외부)</strong>은 두고 정산 실행·미수·환수·담보·공제·보류/유통/리포트 근거 행만 지웁니다(복구 불가, 동일 권한).',
          rows: [
            [{ type: 'customHtml', col: 12,
              html: '<div class="d-flex flex-wrap align-items-start justify-content-between gap-3 border border-danger-subtle rounded p-3 mb-2 bg-body-secondary">' +
                '<div class="flex-grow-1 small">' +
                '<div class="fw-semibold text-danger mb-1" data-pg-ui-t="전체 데이터 초기화">' + escUi(L('전체 데이터 초기화')) + '</div>' +
                pgUiParagraphHtml('전산설정 전체 데이터 초기화 카드 본문', 'mb-0 text-muted') + '</div>' +
                '<button type="button" class="btn btn-danger btn-sm flex-shrink-0 align-self-center" id="hqLedgerOperationalDataResetBtn" data-pg-ui-t="전체 데이터 초기화…">' + escUi(L('전체 데이터 초기화…')) + '</button></div>' }],
            [{ type: 'customHtml', col: 12,
              html: '<div class="d-flex flex-wrap align-items-start justify-content-between gap-3 border border-warning-subtle rounded p-3 mb-2 bg-body-secondary">' +
                '<div class="flex-grow-1 small">' +
                '<div class="fw-semibold text-warning-emphasis mb-1" data-pg-ui-t="금일 결제·노티 삭제">' + escUi(L('금일 결제·노티 삭제')) + '</div>' +
                pgUiParagraphHtml('전산설정 금일 결제 노티 삭제 카드 본문', 'mb-2 text-muted') +
                '<div class="d-flex flex-wrap gap-2 align-items-end">' +
                '<div><label class="form-label small mb-0" for="hqLedgerPurgePayDate" data-pg-ui-t="대상 일자">대상 일자</label>' +
                '<input type="date" class="form-control form-control-sm" id="hqLedgerPurgePayDate" style="min-width:9.5rem"></div>' +
                '<div><label class="form-label small mb-0" for="hqLedgerPurgeMerchantId" data-pg-ui-t="가맹점 ID(선택)">가맹점 ID(선택)</label>' +
                '<input type="text" class="form-control form-control-sm" id="hqLedgerPurgeMerchantId" maxlength="20" placeholder="' + escUi(L('비우면 전체 가맹')) + '" style="min-width:10rem"></div>' +
                '<div class="form-check mb-1"><input class="form-check-input" type="checkbox" id="hqLedgerPurgeInboundYn" checked>' +
                '<label class="form-check-label small" for="hqLedgerPurgeInboundYn" data-pg-ui-t="노티수령정보도 삭제">노티수령정보도 삭제</label></div>' +
                '</div></div>' +
                '<button type="button" class="btn btn-warning btn-sm flex-shrink-0 align-self-center text-dark" id="hqLedgerPurgePayNotifyDayBtn" data-pg-ui-t="금일 결제·노티 삭제…">' + escUi(L('금일 결제·노티 삭제…')) + '</button></div>' }],
            [{ type: 'customHtml', col: 12,
              html: '<div class="d-flex flex-wrap align-items-start justify-content-between gap-3 border border-success-subtle rounded p-3 mb-2 bg-body-secondary">' +
                '<div class="flex-grow-1 small">' +
                '<div class="fw-semibold text-success mb-1">' + escUi(L('주문별 노티 재반영(icopayCompId)')) + '</div>' +
                '<p class="mb-2 text-muted">' + escUi(L('결제내역만 삭제하고 노티수령정보(raw_body)가 남아 있을 때, 지정 일자·주문번호별 최신 노티 원문을 icopayCompId와 함께 재반영해 pg_trnsctn을 복구합니다. 공통 MID·복수 가맹점 환경에서 업체코드가 필요합니다.')) + '</p>' +
                '<div class="d-flex flex-wrap gap-2 align-items-end">' +
                '<div><label class="form-label small mb-0" for="hqLedgerReplayNotifyDate">' + escUi(L('대상 일자')) + '</label>' +
                '<input type="date" class="form-control form-control-sm" id="hqLedgerReplayNotifyDate" style="min-width:9.5rem"></div>' +
                '<div><label class="form-label small mb-0" for="hqLedgerReplayNotifyCompId">icopayCompId</label>' +
                '<input type="text" class="form-control form-control-sm" id="hqLedgerReplayNotifyCompId" maxlength="20" placeholder="6000000041" style="min-width:10rem"></div>' +
                '<div class="flex-grow-1" style="min-width:14rem"><label class="form-label small mb-0" for="hqLedgerReplayNotifyOrders">' + escUi(L('주문번호(쉼표 구분)')) + '</label>' +
                '<input type="text" class="form-control form-control-sm" id="hqLedgerReplayNotifyOrders" placeholder="451,448,444,441,440,439,438,436"></div>' +
                '</div></div>' +
                '<button type="button" class="btn btn-success btn-sm flex-shrink-0 align-self-center" id="hqLedgerReplayNotifyOrdersBtn">' + escUi(L('주문별 노티 재반영…')) + '</button></div>' }],
            [{ type: 'customHtml', col: 12,
              html: '<div class="d-flex flex-wrap align-items-start justify-content-between gap-3 border border-primary-subtle rounded p-3 mb-2 bg-body-secondary">' +
                '<div class="flex-grow-1 small">' +
                '<div class="fw-semibold text-primary mb-1" data-pg-ui-t="정산 데이터 초기화">' + escUi(L('정산 데이터 초기화')) + '</div>' +
                pgUiParagraphHtml('전산설정 정산 데이터 초기화 카드 본문', 'mb-1 text-muted') +
                '<div class="d-flex flex-wrap gap-1 align-items-center">' +
                '<span class="text-muted small me-1" data-pg-ui-t="부분:">' + escUi(L('부분:')) + '</span>' +
                '<button type="button" class="btn btn-outline-primary btn-sm hq-ledger-sttl-reset-part" data-sttl-scope="RECEIVABLES" data-pg-ui-title="미수금 환수요청·미수금" data-pg-ui-t="미수금" title="' + escUi(L('미수금 환수요청·미수금')) + '">' + escUi(L('미수금')) + '</button>' +
                '<button type="button" class="btn btn-outline-primary btn-sm hq-ledger-sttl-reset-part" data-sttl-scope="RECOVERY" data-pg-ui-title="환수금(tb_settlement_recovery)" data-pg-ui-t="환수금" title="' + escUi(L('환수금(tb_settlement_recovery)')) + '">' + escUi(L('환수금')) + '</button>' +
                '<button type="button" class="btn btn-outline-primary btn-sm hq-ledger-sttl-reset-part" data-sttl-scope="ROLLING" data-pg-ui-title="담보·롤링(tb_rolling_reserve)" data-pg-ui-t="담보" title="' + escUi(L('담보·롤링(tb_rolling_reserve)')) + '">' + escUi(L('담보')) + '</button>' +
                '<button type="button" class="btn btn-outline-primary btn-sm hq-ledger-sttl-reset-part" data-sttl-scope="DEDUCTIONS" data-pg-ui-title="잔액공제 로그(tb_balance_deduction)" data-pg-ui-t="공제로그" title="' + escUi(L('잔액공제 로그(tb_balance_deduction)')) + '">' + escUi(L('공제로그')) + '</button>' +
                '<button type="button" class="btn btn-outline-primary btn-sm hq-ledger-sttl-reset-part" data-sttl-scope="RUNS" data-pg-ui-title="실행+위 연동 일괄(미수·환수·담보·공제·실행·settled)" data-pg-ui-t="실행+연동" title="' + escUi(L('실행+위 연동 일괄(미수·환수·담보·공제·실행·settled)')) + '">' + escUi(L('실행+연동')) + '</button>' +
                '</div></div>' +
                '<button type="button" class="btn btn-primary btn-sm flex-shrink-0 align-self-center" id="hqLedgerSettlementDataResetBtn" data-pg-ui-t="정산 데이터 초기화…">' + escUi(L('정산 데이터 초기화…')) + '</button></div>' }],
            [{ type: 'customHtml', col: 12,
              html: '<div class="border rounded"><table class="table table-sm table-bordered align-middle mb-0 w-100 hq-data-retention-table">' +
                '<thead class="table-light"><tr><th class="text-nowrap" data-pg-ui-t="데이터 유형">' + escUi(L('데이터 유형')) + '</th><th class="text-center text-nowrap" data-pg-ui-t="자동삭제">자동삭제</th><th class="text-nowrap text-center" data-pg-ui-t="삭제(일)">삭제(일)</th><th class="text-nowrap text-center" data-pg-ui-t="보관(일)">보관(일)</th><th data-pg-ui-t="설명·연동">설명·연동</th><th class="text-center text-nowrap" data-pg-ui-t="관리">관리</th></tr></thead>' +
                '<tbody id="hqDataRetentionTbody"><tr><td colspan="6" class="text-center text-muted py-3" data-pg-ui-t="불러오는 중…">' + escUi(L('불러오는 중…')) + '</td></tr></tbody></table></div>' +
                pgUiParagraphHtml('전산설정 데이터 보관 표 하단 안내', 'small text-muted mb-0 mt-1') }]
          ]
        },
        {
          title: '통합내역(칠페이) 동기화·로그 보관',
          notice: '통합내역 화면에서 날짜를 비운 채 조회하면 「최근 동기화 범위」일만큼 TransactionDate 구간을 채웁니다. [검색 초기화]는 「피지거래내역 초기화 동기화(개월)」만큼 넓은 구간으로 맞춥니다. 로그 파일 보관(일)은 매일 새벽 데이터 보관 스케줄에서 <code>logs</code> 등의 오래된 .log/.gz 파일 삭제에 반영됩니다. 로그 메모리 보관(일)은 정책 저장용(추후 진단 버퍼 연동 시 사용).',
          rows: [
            [{ label: '피지거래내역 초기화 동기화(개월)', type: 'number', name: 'chillpayTrInitSyncMonths', col: 3, placeholder: '기본 3' },
             { label: '피지거래내역 최근 동기화 범위(일)', type: 'number', name: 'chillpayTrRecentSyncDays', col: 3, placeholder: '기본 2' },
             { label: '로그 메모리 보관(일)', type: 'number', name: 'appLogMemoryRetentionDays', col: 3, placeholder: '기본 30' },
             { label: '로그 파일 보관(일)', type: 'number', name: 'appLogFileRetentionDays', col: 3, placeholder: '기본 90' }]
          ]
        },
        {
          title: 'JPAY 통합내역(동기화 기간)',
          notice: 'JPAY 포털 로그인 계정은 <strong>본사설정 &gt; 결제대행사로직</strong>에서 총판(MASTER_DIST)별로 등록합니다. 아래는 동기화 기간만 설정합니다. 서버(VPS)에 <code>Node.js</code>·Playwright Chromium이 필요합니다.',
          rows: [
            [{ label: 'JPAY 초기화 동기화(개월)', type: 'number', name: 'jpayTrInitSyncMonths', col: 3, placeholder: '기본 3' },
             { label: 'JPAY 최근 동기화 범위(일)', type: 'number', name: 'jpayTrRecentSyncDays', col: 3, placeholder: '기본 2' }]
          ]
        },
        {
          title: '수수료·정산 로직 (수수료내역)',
          notice: '통화별 표는 결제·정산 통화(알파 코드)마다 소수 자릿수·잘리는 자리 처리를 지정합니다. 소수 자릿수가 0이면 금액은 정수만 의미하므로 「잘리는 자리 처리」는 비활성화되며 저장 시 그대로(버림, DOWN)로 통일됩니다. 목록 API는 행의 결제통화·거래통화에 맞춰 이 설정을 적용합니다. JSON에 없는 통화는 아래 「기본(통화 미지정)」값을 따릅니다. 조직항목설정 VIEW SETTING의 통화 열은 가맹 정책통화·거래통화를 표시하며, 총판 하위 가맹이 쓰는 모든 통화가 데이터에 존재하면 각 행에 그대로 나타납니다.',
          rows: [
            [{ type: 'customHtml', col: 12, html: pgUiParagraphHtml('전산설정 수수료 기본 통화 미지정 안내', 'small text-muted mb-1') }],
            [{ label: '소수 자릿수', type: 'select', name: 'feeListDecimalPlaces', col: 2,
              options: [{ v: '0', t: '0' }, { v: '1', t: '1' }, { v: '2', t: '2' }, { v: '3', t: '3' }, { v: '4', t: '4' }, { v: '5', t: '5' }, { v: '6', t: '6' }, { v: '7', t: '7' }, { v: '8', t: '8' }] },
             { label: '잘리는 자리 처리', type: 'select', name: 'feeListRoundMode', col: 3,
              options: [{ v: 'CEILING', t: '절상' }, { v: 'HALF_UP', t: '반올림' }, { v: 'DOWN', t: '그대로(버림)' }] }],
            [{ type: 'customHtml', col: 12,
              html: '<div class="border rounded"><table class="table table-sm table-bordered align-middle mb-0 w-100">' +
                '<thead class="table-light"><tr>' + pgUiThT('기준통화') + pgUiThT('소수 자릿수', 'text-center text-nowrap') + pgUiThT('잘리는 자리 처리', 'text-nowrap') + pgUiThT('관리', 'text-center text-nowrap') + '</tr></thead>' +
                '<tbody id="hqFeeCurrencyFormatTbody"><tr><td colspan="4" class="text-center text-muted py-3">' + escUi(L('불러오는 중…')) + '</td></tr></tbody></table></div>' +
                pgUiParagraphHtml('전산설정 수수료 통화 표 하단 안내', 'small text-muted mb-0 mt-1') }]
          ]
        },
        {
          title: '결제 통화 (전역 표시 기준)',
          notice: '위 두 필드는 DB에 저장된 전역 기준(ISO 숫자·그에 대응하는 알파)만 보여 주며 이 화면에서는 변경할 수 없습니다. 아래 표는 서버에 정의된 지원 ISO 4217 숫자와 표시 통화(알파) 매핑 전체를 노출합니다(목록에 없는 숫자는 저장 시 기본값으로 정규화될 수 있습니다). 수수료 정책·조직 기준통화 등으로 통화가 정해지지 않을 때 결제내역·통합내역 상단 집계(단일통화 뷰)·칠페이 목록 meta의 기본 통화 폴백으로 전역 기준이 사용됩니다. API에는 <code>payDisplayCurrencyIsoNum</code>·<code>payDisplayCurrencyCode</code>·<code>payDisplayCurrencyCatalog</code>가 포함됩니다.',
          rows: [
            [{ label: '결제 통화 (ISO 숫자)', type: 'text', name: 'payDisplayCurrencyIsoNum', col: 3, readonly: true,
               placeholder: '조회 시 서버 값' },
             { label: '표시 통화(알파)', type: 'text', name: 'payDisplayCurrencyCode', col: 3, readonly: true,
               placeholder: 'ISO 숫자 기준 자동' }],
            [{ type: 'customHtml', col: 12,
              html: '<p class="small text-muted mb-1" data-pg-ui-t="지원 ISO 숫자 ↔ 표시 통화(알파) — 읽기 전용">' + escUi(L('지원 ISO 숫자 ↔ 표시 통화(알파) — 읽기 전용')) + '</p>' +
                '<div class="border rounded"><table class="table table-sm table-bordered align-middle mb-0 w-100" id="grid_hqPayDisplayCurrencyCatalog">' +
                '<thead class="table-light"><tr>' + pgUiThT('ISO 4217 숫자') + pgUiThT('표시 통화(알파)') + pgUiThT('전역 기준', 'text-center text-nowrap') + '</tr></thead>' +
                '<tbody id="hqPayDisplayCurrencyCatalogTbody"><tr><td colspan="3" class="text-center text-muted py-3">' + escUi(L('불러오는 중…')) + '</td></tr></tbody></table></div>' }]
          ]
        },
        {
          title: '사용불가카드 등록',
          notice: 'PG별 카드번호 <strong>앞자리(BIN) 접두</strong>만 등록합니다. 입력 시 결제창·승인 API에서 즉시 차단됩니다. 개별 카드번호(비활성카드)는 <strong>운영관리 → 비활성카드등록</strong> 메뉴에서 관리합니다.',
          rows: [
            [{ type: 'customHtml', col: 12,
              html: '<p class="small fw-semibold mb-1" data-pg-ui-t="사용불가 카드 BIN 접두">사용불가 카드 BIN 접두</p>' +
                '<div class="d-flex flex-wrap gap-2 align-items-end mb-2">' +
                '<div><label class="form-label small mb-0" data-pg-ui-t="PG">PG</label>' +
                '<select class="form-select form-select-sm" id="hqPayCardPrefixPg" style="min-width:8rem">' +
                '<option value="JPAY">JPAY</option><option value="CHILLPAY">ChillPay</option></select></div>' +
                '<div><label class="form-label small mb-0" data-pg-ui-t="접두(숫자)">접두(숫자)</label>' +
                '<input type="text" class="form-control form-control-sm" id="hqPayCardPrefixDigits" maxlength="8" inputmode="numeric" style="width:6rem"></div>' +
                '<div class="flex-grow-1"><label class="form-label small mb-0" data-pg-ui-t="비고">비고</label>' +
                '<input type="text" class="form-control form-control-sm" id="hqPayCardPrefixRemark"></div>' +
                '<button type="button" class="btn btn-primary btn-sm" id="hqPayCardPrefixAddBtn" data-pg-ui-t="접두 등록">접두 등록</button></div>' +
                '<div class="border rounded mb-3"><table class="table table-sm table-bordered mb-0">' +
                '<thead class="table-light"><tr><th data-pg-ui-t="PG">PG</th><th data-pg-ui-t="접두">접두</th><th data-pg-ui-t="비고">비고</th><th class="text-center" data-pg-ui-t="관리">관리</th></tr></thead>' +
                '<tbody id="hqPayCardBlockPrefixTbody"><tr><td colspan="4" class="text-center text-muted py-2" data-pg-ui-t="불러오는 중…">불러오는 중…</td></tr></tbody></table></div>' }]
          ]
        },
        {
          title: '결제 후속조치 (NOTI 환경설정 대응)',
          notice: '시간 선택 국가(기준 Zone)는 무효·이메일무효에 적용됩니다. 무효 기본은 당일 <strong>0:00~21:00</strong>. 수동무효(이메일)도 당일 <strong>시작~마감</strong>을 지정(마감 비우면 23:59). 환불은 <strong>태국</strong> 기준 결제일 <strong>익일</strong>의 <strong>시작 시각</strong>부터 일수입니다. 「설정(사용)」이 사용일 때만 편집할 수 있습니다. 아래 표에서 <strong>본사·총판</strong> 등 조직 단계별로 동일 네 기능의 허용 여부를 둡니다.',
          rows: [
            [{ type: 'customHtml', col: 12, html: function () { return hqLedgerPayFollowNotiTableHtml() + hqLedgerPayFollowLevelCapsTableHtml(); } }]
          ]
        },
        {
          title: '자동화 이메일 설정',
          notice: 'SMTP는 배치 알림·기타 자동 메일과 「이메일무효」 수동 요청 메일 발송에 공통으로 사용합니다. 아래 「이메일무효(ChillPay 등)」에서 수신처·제목·본문을 지정하면, 결제내역에서 이메일무효 실행 시 치환된 본문이 발송됩니다. 자동무효·자동환불·강제환불은 ChillPay Transaction API(무효/환불 요청)로 처리됩니다. 비밀번호는 저장 시에만 갱신하며, 조회 시에는 설정 여부만 표시됩니다.',
          rows: [
            [{ label: 'SMTP 호스트', type: 'text', name: 'smtpHost', col: 3, placeholder: 'smtp.example.com' },
             { label: 'SMTP 포트', type: 'number', name: 'smtpPort', col: 2, placeholder: '587' },
             { label: 'TLS', type: 'select', name: 'smtpTlsYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 },
             { label: 'SMTP 인증', type: 'select', name: 'smtpAuthYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 }],
            [{ label: 'SMTP 사용자', type: 'text', name: 'smtpUsername', col: 4 }],
            [{ label: 'SMTP 비밀번호 (변경 시만 입력)', type: 'password', name: 'smtpPassword', col: 4, placeholder: '비워두면 기존 유지' },
             { label: '비밀번호 저장됨', type: 'text', name: 'smtpPasswordSetLabel', col: 3, readonly: true }],
            [{ label: '발신 메일', type: 'text', name: 'mailFromAddress', col: 4, placeholder: 'noreply@example.com' },
             { label: '발신 표시명', type: 'text', name: 'mailFromName', col: 4 }],
            [{ label: '알림 수신(쉼표 구분)', type: 'textarea', name: 'alertRecipientEmails', col: 8, rows: 2, placeholder: 'a@x.com, b@x.com' }],
            [{ label: '동기화 실패 시 메일', type: 'select', name: 'emailOnSyncFailureYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 2 },
             { label: '일일 요약 메일', type: 'select', name: 'emailDailyDigestYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 2 },
             { label: '무효 배치 알림(예정)', type: 'select', name: 'emailNotifyVoidBatchYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 2 },
             { label: '환불 배치 알림(예정)', type: 'select', name: 'emailNotifyRefundBatchYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 2 }],
            [{ label: '메모', type: 'textarea', name: 'memo', col: 8, rows: 2 }],
            [{ label: '최종 수정', type: 'text', name: 'updatedAt', col: 4, readonly: true }],
            [{ type: 'customHtml', col: 12,
              html: '<hr class="my-2" /><p class="small text-muted mb-2 fw-semibold">' + escUi(L('이메일무효(수동 VOID 요청 메일)')) + '</p>' }],
            [{ label: '수신 이메일', type: 'text', name: 'emailVoidTo', col: 4, placeholder: '예: help@chillpay.co' },
             { label: '회사명(본문 치환)', type: 'text', name: 'emailVoidCompanyName', col: 4 },
             { label: '담당자 성명(본문 치환)', type: 'text', name: 'emailVoidContactName', col: 4 }],
            [{ label: '메일 제목', type: 'text', name: 'emailVoidSubject', col: 12,
              placeholder: '{{transNo}} {{orderNo}} {{amount}} {{routeNo}} {{paymentDate}} {{mid}} {{companyName}} {{contactName}}' }],
            [{ label: '메일 본문', type: 'textarea', name: 'emailVoidBodyTemplate', col: 12, rows: 8,
              placeholder: '영문 샘플·플레이스홀더는 저장 없이도 서버 기본값이 적용됩니다. 비우면 기본 영문 본문이 사용됩니다.' }],
            [{ label: '테스트 수신 이메일', type: 'text', name: 'emailVoidTestRecipient', col: 5,
              placeholder: '예: ziobizm@gmail.com', title: '실제 PG 수신처가 아닌, 본인 확인용 주소입니다. SMTP·템플릿으로 샘플 본문이 발송됩니다.' },
             { type: 'customHtml', col: 7,
              html: '<div class="d-flex flex-wrap align-items-end gap-2 mt-1 mt-md-0">' +
                '<button type="button" class="btn btn-primary btn-sm" id="hqLedgerVoidEmailTestBtn" data-pg-ui-t="테스트 메일 발송">' + escUi(L('테스트 메일 발송')) + '</button>' +
                '<span class="small text-muted" data-pg-ui-html="전산설정 이메일무효 테스트 발송 안내">' + L('전산설정 이메일무효 테스트 발송 안내') + '</span></div>' }]
          ]
        }
      ],
      buttons: [{ id: 'hqLedgerSysSettingsSaveBtn', label: '저장', cls: 'btn-primary' }]
    },
    '/hq/orgViewColumnAllowance': {
      isForm: true,
      formHtmlId: 'hqOrgViewColumnAllowanceForm',
      formSections: [
        {
          title: '조직항목설정',
          notice: '총본사가 각 본사(REGIONAL) 트리마다, 조직 유형·화면별로 VIEW SETTING에서 노출·선택 가능한 열을 지정합니다. 본사·총판·지사·대리점·영업점(동일 설정)·가맹점 네 가지로 나누어 저장합니다. 지사·대리점·영업점과 가맹점에 별도 저장이 없으면 해당 화면의 총판 설정을 그대로 따릅니다. <strong>결제관리</strong>(결제내역·분류 화면·URL/챗봇·상계 및 <strong>통합내역</strong>)과 <strong>정산관리</strong>의 <strong>통합정산</strong>은 화면·조직 유형을 바꿀 때 <strong>기본 체크안</strong>이 자동 적용되며(본사=전체 허용, 총판·지사·가맹 순으로 축소), 체크되지 않은 열은 목록에서 제거되지 않고 꺼진 상태로 둡니다. 서버에 이미 저장된 정책이 있으면 [불러오기]·정책 행 클릭 시 그대로 불러옵니다. 고정 열(번호·업체명·거래일·Route No·TransactionId 등)은 항상 표시되며 여기 목록에 나오지 않습니다. [불러오기]는 현재 선택한 본사·조직 유형·화면에 대해 서버에 저장된 체크 상태를 가져와 반영합니다. 아래 [추가 VIEW 항목]은 화면마다 다르게 본사 전용 열을 등록합니다. 등록된 항목은 해당 화면의 VIEW SETTING에 나타나며, 기본 체크안에 포함된 경우에만 조직 설정에서 자동 체크됩니다.',
          rows: [
            [
              { label: '설정 대상 본사', type: 'select', name: 'regionalOrgCode', col: 4, options: [{ v: '', t: '선택' }], loadRegionalBranches: true },
              { label: '노출 대상 조직', type: 'select', name: 'viewerScope', col: 4, options: [
                { v: 'REGIONAL', t: '본사' },
                { v: 'MASTER_DIST', t: '총판' },
                { v: 'BRANCH_GROUP', t: '지사·대리점·영업점' },
                { v: 'MERCHANT', t: '가맹점' }
              ] },
              { label: '설정 대상 화면', type: 'select', name: 'targetPageUrl', col: 4, options: [
                /* 결제관리 — 사이드 메뉴(menu-structure·index) 순서·표기와 동일 */
                { v: '/calc/chillPayTrList', t: '통합내역' },
                { v: '/calc/jpayTrList', t: '통합조회' },
                { v: '/calc/queryIntegrated', t: '조회통합' },
                { v: '/pay/splitPay', t: '분할결제내역' },
                { v: '/calc/dailyIntegrated', t: '일별통합' },
                { v: '/calc/payList', t: '결제내역' },
                { v: '/calc/dailyPay', t: '일별결제' },
                { v: '/calc/paySuccessList', t: '성공내역' },
                { v: '/calc/payFailList', t: '실패내역' },
                { v: '/calc/payCancelList', t: '취소내역' },
                { v: '/calc/payVoidList', t: '무효처리' },
                { v: '/calc/payEmailVoidList', t: '이메일 무효' },
                { v: '/calc/payRefundList', t: '환불처리' },
                { v: '/calc/payForceRefundList', t: '강제환불' },
                { v: '/pay/easyPay', t: 'URL결제내역' },
                { v: '/pay/chatbotPay', t: '챗봇결제내역' },
                { v: '/pay/jpaySubscription', t: '구독결제내역' },
                { v: '/calc/offsetCancList', t: '상계취소내역' },
                /* 분할관리 */
                { v: '/calc/splitPayList', t: '계약관리' },
                { v: '/comp/compMngTree', t: '업체관리' },
                { v: '/commission/commisionList', t: '수수료관리' },
                /* 정산관리 — 사이드 메뉴 순서·표기 */
                { v: '/calc/chillPaySettlementList', t: '통합정산' },
                { v: '/calc/feeList', t: '수수료내역' },
                { v: '/calc/dailyFee', t: '일별수수료' },
                { v: '/calc/exCalcList', t: '정산실행' },
                { v: '/settlement/settlementResultDistribute', t: '정산배포' },
                { v: '/settlement/settlementResultHold', t: '정산대기' },
                { v: '/calc/paySettlementHoldList', t: '정산보류내역' },
                { v: '/calc/compPointMngList', t: '환수금내역' },
                { v: '/calc/unpaidMng', t: '미수금내역' },
                { v: '/calc/collateralList', t: '담보금내역' },
                { v: '/calc/calcList', t: '유통망정산내역' },
                { v: '/calc/calcGmList', t: '가맹점정산내역' },
                { v: '/calc/settlementReport', t: '정산리포트' }
              ] }
            ],
            [{ type: 'customHtml', col: 12, html: '<div class="card border-secondary mb-3" id="hqViewCustomColCard">' +
              '<div class="card-header py-2 small fw-semibold" data-pg-ui-t="추가 VIEW 항목 (화면별 목록 · 본사 등록)">' + escUi(L('추가 VIEW 항목 (화면별 목록 · 본사 등록)')) + '</div>' +
              '<div class="card-body py-2">' +
              '<p class="text-muted small mb-2 mb-0" data-pg-ui-t="설정 대상 화면을 먼저 선택한 뒤, 표시명을 넣고 [항목 추가]하세요. 목록에서 이름을 바꾸거나 삭제할 수 있습니다. 내부 키는 자동 부여됩니다.">' + escUi(L('설정 대상 화면을 먼저 선택한 뒤, 표시명을 넣고 [항목 추가]하세요. 목록에서 이름을 바꾸거나 삭제할 수 있습니다. 내부 키는 자동 부여됩니다.')) + '</p>' +
              '<div class="d-flex flex-wrap align-items-end gap-2 mb-2">' +
              '<div class="flex-grow-1" style="min-width:200px"><label class="form-label small mb-0" for="hqViewCustomColNameInp" data-pg-ui-t="표시명">' + escUi(L('표시명')) + '</label>' +
              '<input type="text" class="form-control form-control-sm" id="hqViewCustomColNameInp" maxlength="200" placeholder="' + escUi(L('예: 비고란')) + '" data-pg-ui-placeholder="예: 비고란" autocomplete="off"></div>' +
              '<button type="button" class="btn btn-sm btn-success" id="hqViewCustomColAddBtn" data-pg-ui-t="항목 추가">' + escUi(L('항목 추가')) + '</button>' +
              '<button type="button" class="btn btn-sm btn-outline-secondary" id="hqViewCustomColReloadBtn" data-pg-ui-t="목록 새로고침">' + escUi(L('목록 새로고침')) + '</button></div>' +
              '<div class="table-responsive border rounded"><table class="table table-sm table-hover mb-0">' +
              '<thead class="table-light"><tr><th style="width:44%" data-pg-ui-t="표시명">' + escUi(L('표시명')) + '</th><th style="width:36%" data-pg-ui-t="내부 키">' + escUi(L('내부 키')) + '</th><th style="width:12%" data-pg-ui-t="수정">' + escUi(L('수정')) + '</th><th style="width:8%" data-pg-ui-t="삭제">' + escUi(L('삭제')) + '</th></tr></thead>' +
              '<tbody id="hqViewCustomColTbody"><tr><td colspan="4" class="text-center text-muted small py-2" data-pg-ui-t="화면을 선택하세요.">' + escUi(L('화면을 선택하세요.')) + '</td></tr></tbody></table></div>' +
              '</div></div>' }],
            [{ type: 'customHtml', col: 12, html: '<div class="mb-2">' +
              '<div class="d-flex flex-wrap align-items-center justify-content-between gap-2 mb-1">' +
              '<span class="form-label mb-0" data-pg-ui-t="선택한 조직 유형에 노출할 열 (VIEW SETTING에서 선택 가능)">' + escUi(L('선택한 조직 유형에 노출할 열 (VIEW SETTING에서 선택 가능)')) + '</span>' +
              '<div class="btn-group btn-group-sm flex-shrink-0" role="group" aria-label="' + escUi(L('열 노출 저장·일괄 선택')) + '">' +
              '<button type="button" class="btn btn-primary" id="hqOrgAllowColSaveBtn" data-pg-ui-t="저장">' + escUi(L('저장')) + '</button>' +
              '<button type="button" class="btn btn-outline-danger" id="hqOrgAllowColSelectAllBtn" data-pg-ui-t="전체선택">' + escUi(L('전체선택')) + '</button>' +
              '<button type="button" class="btn btn-outline-secondary" id="hqOrgAllowColClearAllBtn" data-pg-ui-t="전체해제">' + escUi(L('전체해제')) + '</button>' +
              '</div></div>' +
              '<div id="hqOrgAllowColumnChecks" class="hq-org-allow-col-wrap border rounded bg-light">' +
              '<div class="table-responsive hq-org-allow-col-scroll">' +
              '<table class="table table-sm table-bordered align-middle mb-0 bg-white hq-org-allow-col-table">' +
              '<thead class="table-light sticky-top"><tr>' +
              '<th class="text-center text-nowrap" style="width:5.5rem" data-pg-ui-t="현재리스트순위">현재리스트순위</th>' +
              '<th data-pg-ui-t="항목이름">항목이름</th>' +
              '<th class="text-center text-nowrap" style="width:5.5rem" data-pg-ui-t="위 아래">위 아래</th>' +
              '</tr></thead><tbody id="hqOrgAllowColumnChecksBody"></tbody></table></div></div>' +
              '<p class="text-muted small mb-0 mt-1" data-pg-ui-t="체크한 열만 해당 조직 유형 사용자 화면의 VIEW SETTING에 나타납니다. ▲▼ 버튼으로 체크된 항목의 순서를 바꾼 뒤 [저장]하면 VIEW SETTING에서의 기본 나열 순서에 반영됩니다. 지사·대리점·영업점·가맹점은 저장이 없으면 총판 설정을 사용합니다.">' + escUi(L('체크한 열만 해당 조직 유형 사용자 화면의 VIEW SETTING에 나타납니다. ▲▼ 버튼으로 체크된 항목의 순서를 바꾼 뒤 [저장]하면 VIEW SETTING에서의 기본 나열 순서에 반영됩니다. 지사·대리점·영업점·가맹점은 저장이 없으면 총판 설정을 사용합니다.')) + '</p></div>' +
              '<div class="mb-0" id="hqOrgAllowSavedWrap">' +
              '<span class="form-label d-block mb-1" data-pg-ui-t="저장된 설정 요약 (선택한 본사)">' + escUi(L('저장된 설정 요약 (선택한 본사)')) + '</span>' +
              '<p class="text-muted small mb-2" data-pg-ui-t="행을 클릭하면 위의 화면·조직 유형이 맞춰지고 서버에 저장된 체크 상태가 불러와집니다.">' + escUi(L('행을 클릭하면 위의 화면·조직 유형이 맞춰지고 서버에 저장된 체크 상태가 불러와집니다.')) + '</p>' +
              '<div class="table-responsive border rounded">' +
              '<table class="table table-sm table-hover mb-0"><thead class="thead-light"><tr>' +
              '<th data-pg-ui-t="화면">' + escUi(L('화면')) + '</th><th data-pg-ui-t="조직 유형">' + escUi(L('조직 유형')) + '</th><th class="text-right" data-pg-ui-t="허용 열 수">' + escUi(L('허용 열 수')) + '</th><th data-pg-ui-t="수정일시">' + escUi(L('수정일시')) + '</th>' +
              '</tr></thead><tbody id="hqOrgAllowPolicyList"></tbody></table></div>' +
              '<p class="text-muted small mb-0 mt-2" id="hqOrgAllowPolicyListHint"></p></div>' }]
          ]
        }
      ],
      buttons: [
        { id: 'hqOrgAllowLoadBtn', label: '불러오기', cls: 'btn-outline-secondary' },
        { id: 'hqOrgAllowSaveBtn', label: '노출 항목 저장', cls: 'btn-primary' },
        { id: 'hqOrgAllowDeleteBtn', label: '노출 제한 해제', cls: 'btn-outline-danger' }
      ]
    },
    '/hq/domainConfig': {
      domainConfigScreen: true,
      hideListGrid: true,
      summary: [],
      buttons: []
    },
    '/hq/serverManage': {
      isForm: true,
      formSections: [
        {
          title: 'SSL 인증서 모니터링',
          notice: 'Let\u2019s Encrypt: Nginx가 사용하는 fullchain.pem 을 모니터링합니다. live 폴더명은 certbot 인증서 이름(예: api.icopay.co.kr)과 동일합니다. 다중 서브도메인(SAN)은 한 장의 인증서에 포함됩니다. 카페24 등 권한 DNS에 A 레코드가 VPS IP를 가리키는지·일부 ISP DNS 캐시로 예전 IP가 남지 않는지 확인하세요. 조회·저장은 시스템 관리자(ADMIN)만 가능합니다.',
          rows: [
            [{ label: 'fullchain.pem 경로', type: 'text', name: 'serverManageSslCertPath', col: 8, placeholder: '/etc/letsencrypt/live/api.icopay.co.kr/fullchain.pem' }],
            [{ label: 'LE live 폴더명(인증서 이름)', type: 'text', name: 'serverManageSslLeDomain', col: 4, placeholder: 'api.icopay.co.kr' }],
            [{ type: 'customHtml', col: 12, html: '<label class="form-label d-block mb-1" for="serverManageUiRefreshMin" data-pg-ui-t="실시간 대시보드 자동 갱신(분)">실시간 대시보드 자동 갱신(분)</label>' }],
            [{ type: 'customHtml', col: 12, html: '<div class="d-flex flex-wrap align-items-center gap-2 hq-srv-refresh-min-row">' +
              '<div class="hq-srv-refresh-min-input-wrap">' +
              '<input type="number" class="form-control form-control-sm" name="serverManageUiRefreshMin" id="serverManageUiRefreshMin" min="1" max="60" step="1" data-pg-ui-placeholder="비우면 서버 기본" placeholder="' + escUi(L('비우면 서버 기본')) + '">' +
              '</div>' +
              '<button type="button" id="hqServerManageTopSaveBtn" class="btn btn-sm btn-outline-primary flex-shrink-0" data-pg-ui-t="저장">저장</button>' +
              '</div>' }],
            [{ type: 'customHtml', col: 12, html: '<p class="text-muted small mb-0 mt-2" data-pg-ui-t="1~60분만 저장됩니다(내부는 초로 환산). 비우면 <code>application.yml</code>의 <code>app.serverManage.uiAutoRefreshSeconds</code>가 적용됩니다. 아래 [설정 저장]과 동일하게 전체 폼을 저장합니다.">' + L('1~60분만 저장됩니다(내부는 초로 환산). 비우면 <code>application.yml</code>의 <code>app.serverManage.uiAutoRefreshSeconds</code>가 적용됩니다. 아래 [설정 저장]과 동일하게 전체 폼을 저장합니다.') + '</p>' }]
          ]
        },
        {
          title: '호스팅 약정',
          notice: '디스크·트래픽은 GB 단위로 입력합니다(소수 가능). 저장 시 서버에 MB로 환산되어 저장됩니다. 디스크 사용량은 서버 조회값과 약정을 비교합니다. 트래픽 누적은 호스팅 패널 값을 넣거나, 약정 시작일이 있으면 앱이 수집한 일별 트래픽 합으로 폼을 자동 채웁니다(패널과 다를 수 있으니 확인 후 저장).',
          rows: [
            [
              { label: '약정 디스크 (GB)', type: 'number', name: 'serverManageContractDiskGb', col: 3, step: '0.001', placeholder: '예: 1 또는 0.977' },
              { label: '약정 트래픽 (GB/기간)', type: 'number', name: 'serverManageContractTrafficGb', col: 3, step: '0.001', placeholder: '예: 1.5' },
              { label: '트래픽 누적 사용 (GB)', type: 'number', name: 'serverManageTrafficUsedGb', col: 3, step: '0.001', placeholder: '패널 누적' }
            ],
            [
              { label: '약정 시작일', type: 'date', name: 'serverManageContractStart', col: 3 },
              { label: '약정 종료일', type: 'date', name: 'serverManageContractEnd', col: 3 }
            ]
          ]
        },
        {
          title: '실시간 대시보드',
          notice: 'SSL 카드에 인증서 SAN(호스트명) 목록과 운영 안내(카페24 DNS·Cloudflare·다중 -d)가 포함됩니다. 도메인구성설정 화면에서는 전사·조직 URL과 SAN 대조 표가 함께 표시됩니다. 레이아웃은 NOTI GitHub 저장소의 /admin/system-monitor를 참고했습니다. PG는 Spring API(JSON)로 채웁니다. 교차 출처 접속 시 상단 안내를 확인하세요.',
          rows: [
            [{
              type: 'customHtml',
              col: 12,
              html: '<div id="hqServerManageDashboard" class="hq-server-manage-dashboard hq-noti-monitor">' +
                '<div class="d-flex flex-wrap align-items-center gap-3 mb-2 p-2 hq-mon-toolbar">' +
                '<span id="hqSrvGeneratedAt" class="text-muted small">—</span>' +
                '<span id="hqSrvCountdown" class="small fw-semibold text-primary">—</span>' +
                '<label class="mb-0 small d-flex align-items-center gap-1 user-select-none"><input type="checkbox" id="hqSrvAutoRefresh" checked> <span data-pg-ui-t="자동 갱신">자동 갱신</span></label>' +
                '<span class="text-muted small"><span data-pg-ui-t="간격">간격</span> <span id="hqSrvIntervalSec">—</span></span>' +
                '</div>' +
                '<div id="hqMonCrossOriginHint" class="alert alert-secondary py-2 small mb-0 mt-2 d-none" role="note"></div>' +
                '<div id="hqSrvInlineMsg" class="small mt-2" role="status" aria-live="polite"></div>' +
                '<div id="hqSrvAlerts"></div>' +
                '<div id="hqSrvCards"></div>' +
                '<div id="hqSrvUsageSection" class="hq-srv-usage-section mt-3">' +
                '<h3 class="h6 fw-bold mb-2" data-pg-ui-t="트래픽 · 메모리 피크">트래픽 · 메모리 피크</h3>' +
                '<p class="small text-muted mb-2" data-pg-ui-t="일간/주간/월간 전환 시 그래프·요약이 바뀝니다. 수집은 앱이 주기적으로 수행합니다. 레이아웃은 <a href=&quot;https://github.com/ziobiz/NOTI&quot; target=&quot;_blank&quot; rel=&quot;noopener&quot;>NOTI</a> 시스템 모니터를 참고했습니다.">' + L('일간/주간/월간 전환 시 그래프·요약이 바뀝니다. 수집은 앱이 주기적으로 수행합니다. 레이아웃은 <a href="https://github.com/ziobiz/NOTI" target="_blank" rel="noopener">NOTI</a> 시스템 모니터를 참고했습니다.') + '</p>' +
                '<div class="btn-group btn-group-sm mb-2" role="group" data-pg-ui-aria-label="기간">' +
                '<button type="button" class="btn btn-outline-primary active" data-hq-usage-grain="daily" data-pg-ui-t="일간">일간</button>' +
                '<button type="button" class="btn btn-outline-primary" data-hq-usage-grain="weekly" data-pg-ui-t="주간">주간</button>' +
                '<button type="button" class="btn btn-outline-primary" data-hq-usage-grain="monthly" data-pg-ui-t="월간">월간</button>' +
                '</div>' +
                '<div class="row g-2 mb-2">' +
                '<div class="col-lg-7"><div class="hq-usage-chart-wrap border rounded bg-white p-2"><canvas id="hqUsageChartMixed"></canvas></div></div>' +
                '<div class="col-lg-5"><div class="hq-usage-chart-wrap border rounded bg-white p-2"><canvas id="hqUsageChartMem"></canvas></div></div>' +
                '</div>' +
                '<div id="hqUsageSummary" class="hq-usage-summary border rounded bg-white p-3 small text-body"></div>' +
                '</div>' +
                '<details class="mt-3 border rounded p-2 bg-light"><summary class="small text-muted user-select-none" data-pg-ui-t="원본 JSON (디버그)">원본 JSON (디버그)</summary>' +
                '<pre id="hqSrvJsonRaw" class="small mt-2 mb-0" style="max-height:240px;overflow:auto;white-space:pre-wrap"></pre></details>' +
                '</div>'
            }]
          ]
        }
      ],
      buttons: [
        { id: 'hqServerManageSaveBtn', label: '설정 저장', cls: 'btn-primary' },
        { id: 'hqServerManageRefreshBtn', label: '요약 새로고침', cls: 'btn-outline-secondary' }
      ]
    },
    '/hq/apiConfig': {
      isForm: true,
      formSections: [
        {
          title: 'API배포설정',
          notice: '가맹점에 발급하는 통합 API의 기본 URL·인증·타임아웃입니다. PG사별 MID·API Key·시크릿은 배포설정 > 「API연동설정」에서 PG코드 단위로 추가·저장하세요(여 PG 병행).',
          rows: [
            [{ label: 'API 기본 URL', type: 'text', name: 'baseUrl', col: 6, placeholder: 'https://api.example.com/v1' }],
            [{ label: '인증방식', type: 'select', name: 'authType', options: [{ v: 'API_KEY', t: 'API Key' }, { v: 'Bearer', t: 'Bearer Token' }, { v: 'BASIC', t: 'Basic' }], col: 2 }, { label: '타임아웃(초)', type: 'text', name: 'timeoutSec', col: 2 }],
            [{ label: '비고', type: 'textarea', name: 'memo', col: 6 }]
          ]
        },
        {
          title: 'PG 자격 증명 (등록 위치)',
          notice: '[PG사 연동 추가]로 PG코드·표시명을 만든 뒤, 동일 화면에서 MID·API Key·MD5(또는 서명키)·Route·Environment (Sandbox/Production)을 입력합니다. ChillPay 결제는 PG코드 CHILLPAY 행에 값이 있으면 그것을 최우선으로 사용하고, 비어 있을 때만 아래 레거시 필드를 사용합니다.',
          rows: [[{
            type: 'customHtml',
            col: 12,
            html: '<button type="button" class="btn btn-sm btn-primary" id="hqApiConfigOpenPgLink"><span data-pg-ui-t="' + escUi('API연동설정 화면 열기') + '">' + escUi(L('API연동설정 화면 열기')) + '</span></button>' +
              '<span class="text-muted small ms-2" data-pg-ui-t="' + escUi('목록에서 행을 더블클릭하면 자격 증명을 편집할 수 있습니다.') + '">' + escUi(L('목록에서 행을 더블클릭하면 자격 증명을 편집할 수 있습니다.')) + '</span>'
          }]]
        },
        {
          title: 'ChillPay 레거시 (tb_hq_api_config 호환)',
          notice: '배포설정 > API연동설정에 CHILLPAY로 API Key·MD5가 등록되어 있으면 이 블록은 무시됩니다. 기존 DB만 쓰는 환경용입니다.',
          rows: [
            [{ label: 'Merchant Code', type: 'text', name: 'chillpayMerchantCode', col: 2, placeholder: 'M035594' }, { label: 'API Key', type: 'text', name: 'chillpayApiKey', col: 4, placeholder: 'ChillPay에서 발급' }],
            [{ label: 'MD5 Secret Key', type: 'text', name: 'chillpayMd5Key', col: 4, placeholder: 'CheckSum 생성용' }, { label: 'Route No', type: 'text', name: 'chillpayRouteNo', col: 1, placeholder: '4' }, { label: 'Environment', type: 'select', name: 'chillpaySandbox', options: [{ v: 'Y', t: 'Sandbox' }, { v: 'N', t: 'Production' }], col: 1 }]
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
    '/hq/urlPayDeploy': {
      isForm: true,
      formSections: [
        {
          title: 'URL결제설정',
          notice: '<strong>일반형</strong>은 표시·실결제 통화·FX·수동 환산·행 마진을 쓰지 않으며, 결제는 해당 <strong>총판(조직)에 설정된 통화</strong>로 진행됩니다(이 화면에서 선택 불가). <strong>DISPLAY</strong>·<strong>BLIND</strong>일 때 금액 모드·<strong>결제 방식(고정/멀티)</strong>·표시 통화·실결제 통화·FX를 PG별로 설정합니다. <strong>멀티</strong>이면 표의 <strong>표시 통화</strong> 칸은 비활성화되며, 공개 결제 페이지에서 고객이 통화를 고릅니다. <strong>BLIND+고정</strong>이면 공개 결제창에서 표시 통화 행·청구예상(환산) 행을 숨기고, <strong>BLIND+멀티</strong>이면 고객이 표시 통화·금액을 고른 뒤에도 <strong>청구예상(실결제 통화 환산)</strong>만 숨기며 견적·결제는 DISPLAY와 동일합니다. <strong>FX 자동(BOT)</strong>이면 아래 <strong>표시통화별 마진(7종)</strong>만 마진으로 쓰이고, 행의 <strong>수동 실결제/1표시</strong>·<strong>PG별 마진율</strong>은 비활성입니다. <strong>FX 수동</strong>이면 해당 행에서 실결제/1표시와 마진율을 직접 입력합니다. <strong>고정</strong>이면 공개 결제 페이지에는 <strong>표시 통화</strong>만 노출되고(셀렉트 없음), <strong>멀티</strong>이면 고객이 표시 통화를 고를 수 있는 셀렉트가 나옵니다(멀티 시 선택지는 본사 전역 순서이며, JSON에서 <code>displayCurrencies</code> 배열로 줄일 수 있음). 아래 <strong>표시→실결제(FX) 기능</strong>·견적 주기·<strong>BOT 환율 기준일</strong>·<strong>마진(표시통화별)</strong>은 DISPLAY·BLIND 모드를 쓰는 PG가 <strong>하나라도 있을 때만</strong> 활성화됩니다. BOT는 방콕 달력일당·모드당 <strong>서버에서 1회만</strong> 조회합니다. 실결제 통화가 표시와 다를 때는 BOT 일평균(THB 경유)으로 환산하고, <strong>표시=실결제</strong>이면 1:1입니다.',
          rows: [
            [{ label: '표시→실결제(FX) 기능', type: 'select', name: '_urlPayFxUiEnabled', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 },
             { label: '견적 갱신(초)', type: 'text', name: '_urlPayFxUiRefresh', col: 2, placeholder: '600' },
             { label: '견적 TTL(초)', type: 'text', name: '_urlPayFxUiTtl', col: 2, placeholder: '600' }],
            [{ label: 'BOT 환율 기준일', type: 'select', name: '_urlPayFxUiBotRateAsOf', options: [{ v: 'PREVIOUS_DAY_CLOSE', t: '전일 종가(방콕)' }, { v: 'LATEST_BOT_PERIOD', t: '당일·최신 고시일' }], col: 4 }],
            [{ label: '마진 JPY(표시)', type: 'text', name: '_urlPayFxUiMarginJpy', col: 2, placeholder: '0' },
             { label: '마진 USD(표시)', type: 'text', name: '_urlPayFxUiMarginUsd', col: 2, placeholder: '0' },
             { label: '마진 KRW(표시)', type: 'text', name: '_urlPayFxUiMarginKrw', col: 2, placeholder: '0' },
             { label: '마진 THB(표시)', type: 'text', name: '_urlPayFxUiMarginThb', col: 2, placeholder: '0' }],
            [{ label: '마진 SGD(표시)', type: 'text', name: '_urlPayFxUiMarginSgd', col: 2, placeholder: '0' },
             { label: '마진 HKD(표시)', type: 'text', name: '_urlPayFxUiMarginHkd', col: 2, placeholder: '0' },
             { label: '마진 CNY(표시)', type: 'text', name: '_urlPayFxUiMarginCny', col: 2, placeholder: '0' }],
            [{
              type: 'customHtml',
              col: 12,
              html: function hqUrlPayDeployPgGridHtml() {
                return (
                  pgUiParagraphHtml('연동용도 URL결제 PG 목록을 불러옵니다. 저장 시 <code>tb_hq_api_config.url_pay_display_fx_json</code>에 반영됩니다.', 'small text-muted mb-2') +
                  '<div class="table-responsive">' +
                  '<table class="table table-sm table-bordered align-middle mb-2" id="grid_urlPayDeployPg">' +
                  '<thead class="table-light"><tr>' +
                  '<th style="min-width:10rem" data-pg-ui-t="결제대행사">결제대행사</th>' +
                  '<th style="min-width:7rem" data-pg-ui-t="금액 모드">금액 모드</th>' +
                  '<th style="min-width:7rem" data-pg-ui-t="결제 방식">결제 방식</th>' +
                  '<th style="min-width:5rem" data-pg-ui-t="표시 통화">표시 통화</th>' +
                  '<th style="min-width:5rem" data-pg-ui-t="실결제">실결제</th>' +
                  '<th style="min-width:6rem" data-pg-ui-t="FX">FX</th>' +
                  '<th style="min-width:8rem" data-pg-ui-t="수동 실결제/1표시">수동 실결제/1표시</th>' +
                  '<th style="min-width:6rem" data-pg-ui-t="마진율">마진율</th>' +
                  '</tr></thead><tbody id="hqUrlPayDeployPgTbody"><tr><td colspan="8" class="text-muted text-center py-3 small" data-pg-ui-t="목록을 불러오는 중…">목록을 불러오는 중…</td></tr></tbody></table></div>' +
                  '<input type="hidden" name="urlPayDisplayFxJson" id="hqUrlPayDeployFxHidden" value="">'
                );
              }
            }]
          ]
        }
      ],
      buttons: [
        { id: 'hqUrlPayDeployOpenApiLink', label: 'API연동설정', cls: 'btn-outline-secondary' },
        { id: 'hqUrlPayDeploySaveBtn', label: '저장', cls: 'btn-primary' }
      ]
    },
    '/hq/chatbotAiSettings': {
      isForm: true,
      formSections: [
        {
          title: 'AI챗봇설정',
          notice: '<span data-pg-ui-t="JSON 키는 ziobiz/Stock 저장 스키마와 정합되게 report_* 접두를 사용합니다. API 키는 저장 후 화면에 다시 노출하지 않습니다. 빈 입력은 기존 키를 유지합니다.">JSON 키는 ziobiz/Stock 저장 스키마와 정합되게 report_* 접두를 사용합니다. API 키는 저장 후 화면에 다시 노출하지 않습니다. 빈 입력은 기존 키를 유지합니다.</span>',
          rows: [
            [{ type: 'customHtml', col: 12, html: hqChatbotAiSettingsFormHtml }]
          ]
        }
      ],
      buttons: [{ id: 'hqChatbotAiSaveBtn', label: '저장', cls: 'btn-primary' }]
    },
    '/chatbot/orderMng': {
      isForm: true,
      formSections: [
        {
          title: '주문관리',
          notice: '챗봇 고객이 제출한 주문·예약 정보입니다. 결제가 완료되면 접수(확정)로 바뀌며 PG 거래번호가 연결됩니다.',
          rows: [[{ type: 'customHtml', col: 12, html: chatbotOrderMngScreenHtml() }]]
        }
      ],
      buttons: []
    },
    '/chatbot/productMng': {
      isForm: true,
      formSections: [
        {
          title: '상품관리',
          rows: [[
            { type: 'customHtml', col: 12, html: chatbotProductMngNoticeHtml },
            { type: 'customHtml', col: 12, html: chatbotProductMngGridHtml }
          ]]
        }
      ],
      buttons: []
    },
    '/chatbot/chatbotKbMng': {
      isForm: true,
      formSections: [
        {
          title: '산하 가맹 챗봇 기본설정 현황',
          notice: '총본사·본사·총판 등 상위 조직은 산하 가맹점 중 챗봇결제 사용(Y) 가맹점만 표시됩니다(등록 정보와 병합된 안내 표시값). 「상업 기능」열에서 운영 보류를 두면 고객 챗봇에는 상품·예약·결제가 보이지 않지만 문의 채팅은 유지됩니다(챗봇 미사용과 다름). 가맹점 계정은 이 블록이 보이지 않으며, 하단에서 본인 업체 안내만 편집합니다.',
          rows: [[{ type: 'customHtml', col: 12, html: chatbotKbMerchantOverviewHtml }]]
        },
        {
          id: 'chatbotKbPlanPurchaseSection',
          title: '플랜구매설정',
          notice: '등록 가능 건수(플랜)과 본사 AI챗봇설정의 월 요금표가 같은 통화로 표시됩니다. 「현재 플랜」은 당월 즉시 적용 기준이며, 상향만 「즉시 상향」에서 반영됩니다(잔여일 차액 미수금). 「다음 플랜(예약)」은 변경이 없으면 예약이 없는 상태로 유지되고, 바꾸면 익월(서울 달력)부터 적용되며 하향·상향 모두 동일합니다.',
          rows: [[{ type: 'customHtml', col: 12, html: chatbotKbScopeAndPlanFormHtml }]]
        },
        {
          title: '챗봇·고객 안내 설정',
          notice: '고객 공개 챗봇에 노출되는 회사 안내·운영방식·예약 옵션입니다. 가맹은 본인 정보를 저장하고, 상위 조직은 가맹 코드로 불러온 뒤 저장합니다.',
          rows: [[{ type: 'customHtml', col: 12, html: chatbotKbMerchantGuidanceFormHtml }]]
        }
      ],
      buttons: []
    },
    '/hq/paymentOrchestration': {
      isForm: true,
      formSections: [
        {
          title: '결제대행사로직',
          notice: '결제대행 연동 핵심 정책입니다. 통합유형(API_BROKER/URL_PAY)별 결제 실행방식(INLINE/REDIRECT) 기본값과 URL결제 경로를 설정합니다.',
          rows: [
            [{ label: 'API 중계형 기본 방식', type: 'select', name: 'apiBrokerDefaultFlowType', options: [{ v: 'INLINE', t: 'INLINE' }, { v: 'REDIRECT', t: 'REDIRECT' }], col: 2 },
             { label: 'URL 결제형 기본 방식', type: 'select', name: 'urlPayDefaultFlowType', options: [{ v: 'INLINE', t: 'INLINE' }, { v: 'REDIRECT', t: 'REDIRECT' }], col: 2 },
             { label: 'URL 결제 경로 템플릿', type: 'text', name: 'urlPayPathTemplate', col: 4, placeholder: '/pay/{compCode}' }],
            [{ label: 'API 중계형 INLINE 제공', type: 'select', name: 'apiBrokerInlineEnabledYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 },
             { label: 'API 중계형 REDIRECT 제공', type: 'select', name: 'apiBrokerRedirectEnabledYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 },
             { label: 'URL 결제형 INLINE 제공', type: 'select', name: 'urlPayInlineEnabledYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 },
             { label: 'URL 결제형 REDIRECT 제공', type: 'select', name: 'urlPayRedirectEnabledYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 }],
            [{ label: 'WordPress 플러그인 제공', type: 'select', name: 'apiWordpressPluginEnabledYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 },
             { label: '', type: 'note', col: 10, text: 'WooCommerce·일반 WordPress ZIP·REST webhook 채널 전역 on/off. 가맹별 오픈은 업체관리 → 가맹 「가맹 API 연동 채널」.' }],
            [{ label: 'URL 재결제형 제공', type: 'select', name: 'urlPayRepayEnabledYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 },
             { label: 'URL 재결제 경로 템플릿', type: 'text', name: 'urlPayRepayPathTemplate', col: 4, placeholder: '/pay-repay/{compCode}' },
             { label: '', type: 'note', col: 6, text: '저장 카드(CreditToken) 재결제 전용 공개 URL. 가맹 「URL 결제 방식」(공개 URL)·「API URL 인라인 중계 결제」·「챗봇결제 설정」 각각 재결제 URL 이면 해당 채널에 적용됩니다.' }]
          ]
        },
        {
          title: 'JPAY 포털 통합내역 (총판별 계정)',
          notice: 'JPAY는 목록 API가 없습니다. 총판(MASTER_DIST)마다 merchant.j-pay.net 포털 ID 1개를 등록하면, 동기화 시 계정을 순회해 Export 엑셀을 병합·대조합니다. (예: JPY 총판·USD 총판 각각 별도 계정) 비밀번호는 저장 시에만 갱신됩니다.',
          rows: [
            [{
              type: 'customHtml',
              col: 12,
              html: '<input type="hidden" id="hqJpayPortalAccountEditId" value="">' +
                '<div class="border rounded p-2 mb-2 bg-light bg-opacity-25">' +
                '<div class="row g-2 align-items-end">' +
                '<div class="col-md-3"><label class="form-label small mb-0" for="hqJpayPortalMasterDist" data-pg-ui-t="총판">총판</label>' +
                '<select class="form-select form-select-sm" id="hqJpayPortalMasterDist"><option value="" data-pg-ui-t="선택">선택</option></select></div>' +
                '<div class="col-md-2"><label class="form-label small mb-0" for="hqJpayPortalPgCd" data-pg-ui-t="PG코드">PG코드</label>' +
                '<select class="form-select form-select-sm" id="hqJpayPortalPgCd"><option value="" data-pg-ui-t="선택">선택</option></select></div>' +
                '<div class="col-md-2"><label class="form-label small mb-0" for="hqJpayPortalLabel" data-pg-ui-t="표시명">표시명</label>' +
                '<input type="text" class="form-control form-control-sm" id="hqJpayPortalLabel" maxlength="200" data-pg-ui-placeholder="예: JPY 총판" placeholder="예: JPY 총판"></div>' +
                '<div class="col-md-2"><label class="form-label small mb-0" for="hqJpayPortalUsername" data-pg-ui-t="포털 ID">포털 ID</label>' +
                '<input type="text" class="form-control form-control-sm" id="hqJpayPortalUsername" autocomplete="off"></div>' +
                '<div class="col-md-2"><label class="form-label small mb-0" for="hqJpayPortalPassword" data-pg-ui-t="포털 비밀번호">포털 비밀번호</label>' +
                '<input type="password" class="form-control form-control-sm" id="hqJpayPortalPassword" autocomplete="new-password" data-pg-ui-placeholder="신규·변경 시 입력" placeholder="신규·변경 시 입력"></div>' +
                '<div class="col-md-1"><label class="form-label small mb-0" for="hqJpayPortalUseYn" data-pg-ui-t="사용">사용</label>' +
                '<select class="form-select form-select-sm" id="hqJpayPortalUseYn"><option value="Y">Y</option><option value="N">N</option></select></div>' +
                '<div class="col-12 d-flex flex-wrap gap-2 pb-1">' +
                '<button type="button" class="btn btn-sm btn-primary" id="hqJpayPortalAccountSaveBtn" data-pg-ui-t="계정 저장">계정 저장</button>' +
                '<button type="button" class="btn btn-sm btn-outline-secondary" id="hqJpayPortalAccountCancelBtn" data-pg-ui-t="입력 초기화">입력 초기화</button>' +
                '</div></div></div>' +
                '<div class="table-responsive"><table class="table table-sm table-bordered align-middle mb-0">' +
                '<thead class="table-light"><tr>' +
                '<th class="text-center" style="width:3rem">#</th>' +
                '<th style="min-width:8rem" data-pg-ui-t="총판코드">총판코드</th>' +
                '<th style="min-width:8rem" data-pg-ui-t="표시명">표시명</th>' +
                '<th style="min-width:6rem" data-pg-ui-t="PG코드">PG코드</th>' +
                '<th style="min-width:9rem" data-pg-ui-t="포털 ID">포털 ID</th>' +
                '<th class="text-center" style="width:5rem" data-pg-ui-t="비밀번호">비밀번호</th>' +
                '<th class="text-center" style="width:4rem" data-pg-ui-t="사용">사용</th>' +
                '<th class="text-center" style="width:4.5rem" data-pg-ui-t="수정">수정</th>' +
                '<th class="text-center" style="width:4.5rem" data-pg-ui-t="삭제">삭제</th>' +
                '</tr></thead>' +
                '<tbody id="hqJpayPortalAccountTbody"><tr><td colspan="9" class="text-center text-muted py-3" data-pg-ui-t="불러오는 중…">불러오는 중…</td></tr></tbody>' +
                '</table></div>'
            }]
          ]
        },
        {
          title: 'JPAY API 구독',
          notice: '가맹 API 인라인 구독(③) 전용입니다. URL·챗봇·1회 jpay-pay 와 분리됩니다.',
          rows: [
            [{ label: 'JPAY API 구독 제공', type: 'select', name: 'jpaySubscriptionEnabledYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 },
             { label: '구독 INLINE 제공', type: 'select', name: 'jpaySubscriptionInlineEnabledYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 },
             { label: '구독 경로 템플릿', type: 'text', name: 'jpaySubscriptionPathTemplate', col: 4, placeholder: '/jpay-subscribe/{compCode}' }],
            [{ label: '기본 plan JSON', type: 'textarea', name: 'jpaySubscriptionConfigJson', col: 12, rows: 4,
               placeholder: '{"attempts":"3","interval_time":3600,"total_count":12}' }]
          ]
        },
        {
          title: 'JPAY 결제창 입력 필드',
          notice: 'JPAY URL 결제창(jpay-pay.html) 입력 필드 본사 기본값입니다. JPAY 필수: (1)카드·CVV (2)성명 (3)이메일 (4)국가코드(ISO2) (5)전화(로컬번호). (6)배송 주소는 선택. <strong>1형 전체</strong>=모두 입력. <strong>2형 필수 4항목</strong>+국가코드. <strong>3형</strong>=카드·성명만 입력, 이메일·국가코드·전화·주소는 prepare buyerPrefill. 업체관리 → 가맹 「JPAY 결제창 입력 필드」에서 가맹별 오버라이드 가능.',
          rows: [
            [{ label: 'JPAY 결제창 입력 필드', type: 'select', name: 'jpayCheckoutFieldMode', options: [{ v: 'FULL', t: '1형 전체 (카드·성명·이메일·전화·배송)' }, { v: 'CARD_ONLY', t: '2형 필수 4항목 (카드·성명·이메일·전화)' }, { v: 'CARD_PREFILL', t: '3형 카드·성명 + 가맹 prefill' }], col: 4 }],
            [{ label: '', type: 'note', col: 12, text: 'JPAY 필수 국가코드(ISO2)는 전화번호와 분리합니다. 1·2형: 접속국가가 국가코드 드롭다운 기본값. 3형: buyerPrefill.countryIso2(없으면 접속국). 전화번호는 +82 등 국가번호 없이 로컬 번호만. pay_country_iso_code_2 로 JPAY에 전달됩니다.' }]
          ]
        },
        {
          title: 'URL 결제 폼 설정',
          notice: '공개 결제 URL(/pay/업체코드 등) 입력 화면입니다. 간편(SIMPLE)은 성명·상품·금액·DirectCreditToken(카드 데이터는 토큰/CCD에 포함)만 받고, 전체(FULL)는 연락처·배송지까지 받습니다. <strong>브라우저 탭 이름</strong>·<strong>파비콘</strong>은 이 결제 폼(탭 제목·탭 아이콘) 전용이며, 화면 하단 <strong>저장</strong>으로 DB에 반영됩니다. 인라인/리다이렉트는 위 「URL 결제형 기본 방식」과 제공 여부로 결정됩니다.',
          rows: [
            [{ label: 'URL 결제 입력 폼', type: 'select', name: 'urlPayFormMode', options: [{ v: 'FULL', t: '전체 입력 (배송지·성명 분리)' }, { v: 'SIMPLE', t: '간편 입력 (필수 최소)' }], col: 4 }],
            [{
              type: 'customHtml',
              col: 12,
              html: '<input type="hidden" name="urlPayTabTitleJson" id="urlPayTabTitleJson" value="{}">' +
                '<div class="row g-2 align-items-end border rounded p-3 bg-light bg-opacity-25">' +
                '<div class="col-md-6"><label class="form-label small mb-0" for="hqUrlPayFormTabTitleKo" data-pg-ui-t="브라우저 탭 이름 (한국어)">브라우저 탭 이름 (한국어)</label>' +
                '<input type="text" class="form-control form-control-sm" id="hqUrlPayFormTabTitleKo" maxlength="120" data-pg-ui-placeholder="비우면 기본 «Payment»" placeholder="비우면 기본 «Payment»">' +
                '<div class="d-flex flex-wrap gap-2 mt-2">' +
                '<button type="button" class="btn btn-sm btn-outline-primary" id="hqUrlPayFormTabTitleTranslateBtn" data-pg-ui-t="탭 제목 다국어">탭 제목 다국어</button>' +
                '</div><p class="text-muted small mb-0 mt-1" data-pg-ui-t="다국어는 숨김 JSON에 저장됩니다. 한국어를 바꾼 뒤 필요 시 다시 「탭 제목 다국어」를 누르세요.">다국어는 숨김 JSON에 저장됩니다. 한국어를 바꾼 뒤 필요 시 다시 「탭 제목 다국어」를 누르세요.</p></div>' +
                '<div class="col-md-6"><label class="form-label small mb-0" data-pg-ui-t="파비콘 (탭 아이콘)">파비콘 (탭 아이콘)</label>' +
                '<div class="input-group input-group-sm">' +
                '<input type="text" class="form-control font-monospace small" name="urlPayFaviconUrl" id="hqUrlPayFormFaviconUrl" readonly data-pg-ui-placeholder="업로드 후 경로가 표시됩니다" placeholder="업로드 후 경로가 표시됩니다">' +
                '<input type="file" class="d-none" id="hqUrlPayFormFaviconFile" accept=".png,.jpg,.jpeg,image/png,image/jpeg">' +
                '<button type="button" class="btn btn-outline-secondary" id="hqUrlPayFormFaviconBrowse" data-pg-ui-t="찾기">찾기</button>' +
                '<button type="button" class="btn btn-outline-primary" id="hqUrlPayFormFaviconUpload" data-pg-ui-t="업로드">업로드</button>' +
                '<button type="button" class="btn btn-outline-danger" id="hqUrlPayFormFaviconClear" data-pg-ui-t="제거">제거</button></div>' +
                '<p class="text-muted small mb-0 mt-1" data-pg-ui-t="PNG·JPG, 1MB 이하. 서버에서 32×32 PNG로 변환됩니다.">PNG·JPG, 1MB 이하. 서버에서 32×32 PNG로 변환됩니다.</p></div></div>'
            }]
          ]
        },
        {
          title: '결제통화로직설정',
          notice: '공개 결제 폼 금액은 아래 규칙에 따라 PG(칠리페이 등) API 금액으로 변환됩니다. 가맹점 URL 결제 <strong>운영</strong> PG(pg_cd)와 결제 통화가 일치하는 <strong>첫 번째</strong> 행이 적용됩니다. (예: ×100 — 입력 800 → 전송 80000) 목록을 바꾼 뒤 <strong>목록 저장(폼 반영)</strong> 또는 행별 <strong>수정 적용</strong>으로 숨김 필드를 맞춘 다음, 화면 맨 아래 <strong>저장</strong>으로 서버에 반영하세요.',
          rows: [
            [{
              type: 'customHtml',
              col: 12,
              html: '<input type="hidden" name="payCurrencyScaleRulesJson" id="payCurrencyScaleRulesJson" value="">' +
                '<div id="hqPayCurrencyScaleMount" class="hq-pay-currency-scale-mount">' +
                pgUiParagraphHtml('결제대행사는 <strong>연동용도 URL결제(Y)</strong>인 PG만 선택할 수 있습니다. 목록·선택 상자 옆의 <strong>연동용도</strong>는 API연동설정의 노티·URL결제·챗봇·API 연동 여부를 나타냅니다. 아래에서 추가·수정·삭제한 뒤 <strong>목록 저장(폼 반영)</strong> → 화면 하단 <strong>저장</strong> 순서로 저장합니다.', 'text-muted small mb-2') +
                '<div class="border rounded p-2 mb-2 bg-light bg-opacity-25">' +
                '<div class="row g-2 align-items-end">' +
                '<div class="col-md-3"><label class="form-label small mb-0" for="hqPayScaleDraftPg" data-pg-ui-t="결제대행사">결제대행사</label>' +
                '<select class="form-select form-select-sm" id="hqPayScaleDraftPg"><option value="" data-pg-ui-t="선택">선택</option></select></div>' +
                '<div class="col-md-2"><label class="form-label small mb-0" for="hqPayScaleDraftCur" data-pg-ui-t="통화">통화</label>' +
                '<select class="form-select form-select-sm" id="hqPayScaleDraftCur"></select></div>' +
                '<div class="col-md-2"><label class="form-label small mb-0" for="hqPayScaleDraftMode" data-pg-ui-t="배율">배율</label>' +
                '<select class="form-select form-select-sm" id="hqPayScaleDraftMode"></select></div>' +
                '<div class="col-md-5 d-flex flex-wrap gap-2 align-items-end pb-1">' +
                '<button type="button" class="btn btn-sm btn-primary" id="hqPayCurrencyScaleBtnAdd" data-pg-ui-t="추가">추가</button>' +
                '<button type="button" class="btn btn-sm btn-outline-primary" id="hqPayCurrencyScaleBtnApplyEdit" data-pg-ui-t="수정 적용">수정 적용</button>' +
                '<button type="button" class="btn btn-sm btn-outline-secondary" id="hqPayCurrencyScaleBtnCancelEdit" data-pg-ui-t="수정 취소">수정 취소</button>' +
                '<button type="button" class="btn btn-sm btn-success" id="hqPayCurrencyScaleBtnSyncHidden" data-pg-ui-t="목록 저장(폼 반영)">목록 저장(폼 반영)</button>' +
                '</div>' +
                '<div class="col-12"><p class="small text-primary mb-0 d-none" id="hqPayScaleEditBanner" role="status"></p></div>' +
                '</div></div>' +
                '<div class="table-responsive"><table class="table table-sm table-bordered align-middle mb-2">' +
                '<thead class="table-light"><tr><th class="text-center" style="width:3rem">#</th><th style="min-width:11rem" data-pg-ui-t="결제대행사">결제대행사</th><th style="min-width:9rem" data-pg-ui-t="연동용도">연동용도</th><th style="min-width:6rem" data-pg-ui-t="통화">통화</th><th style="min-width:7rem" data-pg-ui-t="배율">배율</th><th class="text-center" style="width:4.5rem" data-pg-ui-t="수정">수정</th><th class="text-center" style="width:4.5rem" data-pg-ui-t="삭제">삭제</th></tr></thead>' +
                '<tbody id="hqPayCurrencyScaleTbody"></tbody></table></div>' +
                '<p class="text-muted small mb-0"><span data-pg-ui-t="이전 방식 호환: ">이전 방식 호환: </span><button type="button" class="btn btn-link btn-sm p-0 align-baseline" id="hqPayCurrencyScaleAddRow" data-pg-ui-t="빈 행을 바로 목록에 넣기">빈 행을 바로 목록에 넣기</button></p></div>'
            }]
          ]
        },
        {
          title: 'BOT(태국은행) 일평균 환율 API',
          rows: [
            [{ type: 'customHtml', col: 12, html: pgUiParagraphHtml('URL 표시통화→THB 등에 쓰는 BOT Stat-ExchangeRate 호출값입니다. 칸을 비우면 서버 application.yml·환경변수(BOT_THAILAND_*)를 따릅니다. (A) 레거시 iAPI: Base https://iapi.bot.or.th, Path /Stat/Stat-ExchangeRate/DAILY_AVG_EXG_RATE_V1/, 헤더 이름 api-key. (B) API 포털 v2: Base https://gateway.api.bot.or.th/Stat-ExchangeRate/v2, Path /DAILY_AVG_EXG_RATE/, 헤더 이름 Authorization(값=구독 Client ID).', 'text-muted small mb-2 screen-section-notice') }],
            [{ label: 'API 키(Client ID)', type: 'text', name: 'botThailandApiKey', col: 6, placeholder: '비우면 BOT_THAILAND_API_KEY' },
             { label: '인증 헤더 이름', type: 'text', name: 'botThailandApiKeyHeader', col: 3, placeholder: 'api-key 또는 Authorization' }],
            [{ label: 'Base URL', type: 'text', name: 'botThailandBaseUrl', col: 6, placeholder: 'https://iapi.bot.or.th' },
             { label: '일평균 경로', type: 'text', name: 'botThailandDailyAvgPath', col: 6, placeholder: '/Stat/Stat-ExchangeRate/DAILY_AVG_EXG_RATE_V1/' }]
          ]
        },
        {
          title: 'URL 표시통화 → 실결제 THB',
          notice: 'JSON 편집은 <strong>본사설정 &gt; URL결제설정</strong>에서 합니다. 아래 숨김 필드는 결제로직설정 저장 시 기존 값이 유지되도록 동기화됩니다.',
          rows: [
            [{
              type: 'customHtml',
              col: 12,
              html: '<input type="hidden" name="urlPayDisplayFxJson" id="hqPaymentOrchUrlPayDisplayFxHidden" value="">'
            }]
          ]
        },
        {
          title: '결제구문설정',
          notice: '공개 결제 폼의 <strong>카드 *</strong> 제목·안내 문단과 <strong>결제 금액</strong> 입력란 아래 통화(×100/÷100) 안내는 <strong>PG(결제대행사)별</strong>로 함께 저장됩니다. 금액 안내는 「내용 1」 위에서 노출 여부·문구를 넣습니다(비우면 페이지 기본 다국어 문구). <strong>URL 결제 결과 문구</strong>는 <code>pay-result.html</code> 및 결제 페이지 인라인 완료 카드의 성공/실패 큰 제목·하단 안내를 바꿉니다(비우면 기본 문구). 취소 화면은 실패 문구와 동일 설정을 씁니다. (탭 제목·파비콘은 위 「URL 결제 폼 설정」에서 설정합니다.) URL 결제 연동(<strong>연동용도 URL결제</strong>) PG만 선택할 수 있습니다. 입력 후 <strong>저장</strong>으로 아래 목록에 넣고, 목록에서 <strong>활성</strong>을 켜야 반영됩니다. <strong>저장(다국어)</strong>은 본사 API가 MyMemory로 프록시하여 ENG·CHN·JPN·THA 초안을 채웁니다. 화면 맨 아래 <strong>저장</strong>으로 DB에 반영합니다. 총판 로고가 있으면 결제 폼 상단은 ICOPAY 대신 로고가 나옵니다(별도 연동).',
          rows: [
            [{
              type: 'customHtml',
              col: 12,
              html: '<input type="hidden" name="urlPayCardCopyConfigJson" id="urlPayCardCopyConfigJson" value="">' +
                '<div id="hqPayCardCopyMount" class="hq-pay-card-copy-mount border rounded p-3 mb-2 bg-light bg-opacity-25">' +
                '<div class="row g-2 align-items-end mb-3">' +
                '<div class="col-md-3"><label class="form-label small mb-0" data-pg-ui-t="결제대행사 (URL결제)">결제대행사 (URL결제)</label>' +
                '<select class="form-select form-select-sm" id="hqPayCardCopyDraftPg"><option value="" data-pg-ui-t="선택">선택</option></select></div>' +
                '<div class="col-md-9"><label class="form-label small mb-0" data-pg-ui-t="제목 (한국어) — 결제 폼 «카드 *» 제목">제목 (한국어) — 결제 폼 «카드 *» 제목</label>' +
                '<input type="text" class="form-control form-control-sm" id="hqPayCardCopyDraftTitle" maxlength="500" data-pg-ui-placeholder="예: 카드" placeholder="예: 카드"></div>' +
                '<div class="col-12 border-top pt-2 mt-1"><p class="small fw-semibold mb-2" data-pg-ui-t="결제 금액 하단 안내 (통화 스케일·PG별)">결제 금액 하단 안내 (통화 스케일·PG별)</p>' +
                '<div class="form-check mb-2">' +
                '<input class="form-check-input" type="checkbox" id="hqPayCardCopyDraftAmtShow" checked>' +
                '<label class="form-check-label small" for="hqPayCardCopyDraftAmtShow" data-pg-ui-t="금액 입력란 아래 안내 문구 노출">금액 입력란 아래 안내 문구 노출</label></div>' +
                '<label class="form-label small mb-0" for="hqPayCardCopyDraftAmtNotice" data-pg-ui-t="안내 문구 (한국어, 비우면 페이지 기본 ×100/÷100 문구)">안내 문구 (한국어, 비우면 페이지 기본 ×100/÷100 문구)</label>' +
                '<textarea class="form-control form-control-sm" id="hqPayCardCopyDraftAmtNotice" rows="2" maxlength="2000" data-pg-ui-placeholder="예: 본사 「결제통화로직설정」에 따라 입력 금액과 결제 대행사로 전달되는 금액의 관계를 안내합니다." placeholder="예: 본사 「결제통화로직설정」에 따라 입력 금액과 결제 대행사로 전달되는 금액의 관계를 안내합니다."></textarea></div>' +
                '<div class="col-12"><label class="form-label small mb-0" data-pg-ui-t="내용 1 (한국어)">내용 1 (한국어)</label>' +
                '<textarea class="form-control form-control-sm" id="hqPayCardCopyDraftBody1" rows="4" maxlength="4000" data-pg-ui-placeholder="카드 정보는 위 각 칸 안의 ChillPay 보안 입력(iframe)에서만 입력합니다. 카드 명의(Name on card)도 위 칸에만 입력하면 되며, iframe 밖에서는 값을 읽을 수 없어 하단 이름·성 입력은 표시하지 않습니다. ChillPay가 안전하게 처리하며, 당사 서버로 카드번호 평문이 전달되지 않습니다." placeholder="카드 정보는 위 각 칸 안의 ChillPay 보안 입력(iframe)에서만 입력합니다. 카드 명의(Name on card)도 위 칸에만 입력하면 되며, iframe 밖에서는 값을 읽을 수 없어 하단 이름·성 입력은 표시하지 않습니다. ChillPay가 안전하게 처리하며, 당사 서버로 카드번호 평문이 전달되지 않습니다."></textarea></div>' +
                '<div class="col-12"><label class="form-label small mb-0" data-pg-ui-t="내용 2 (한국어)">내용 2 (한국어)</label>' +
                '<textarea class="form-control form-control-sm" id="hqPayCardCopyDraftBody2" rows="4" maxlength="4000" data-pg-ui-placeholder="카드 입력은 연동된 PG사 보안 위젯(iframe 등)에서 제공합니다. 브랜드별 자릿수·CVV 규칙은 해당 PG가 처리하며, ChillPay는 AMEX 미지원입니다. 다른 PG 연동 시 같은 결제 껍데기 안에서 벤더별 위젯으로 갈아끼우는 형태가 일반적입니다." placeholder="카드 입력은 연동된 PG사 보안 위젯(iframe 등)에서 제공합니다. 브랜드별 자릿수·CVV 규칙은 해당 PG가 처리하며, ChillPay는 AMEX 미지원입니다. 다른 PG 연동 시 같은 결제 껍데기 안에서 벤더별 위젯으로 갈아끼우는 형태가 일반적입니다."></textarea></div>' +
                '<div class="col-12"><label class="form-label small mb-0" data-pg-ui-t="내용 3 (한국어)">내용 3 (한국어)</label>' +
                '<textarea class="form-control form-control-sm" id="hqPayCardCopyDraftBody3" rows="3" maxlength="4000" data-pg-ui-placeholder="예: 사용카드 안내(VISA, MASTER 등), 카드 표기와 동일한 명의 입력 안내 등" placeholder="예: 사용카드 안내(VISA, MASTER 등), 카드 표기와 동일한 명의 입력 안내 등"></textarea></div>' +
                '<div class="col-12 mt-2"><hr class="my-2"><p class="small fw-semibold mb-2" data-pg-ui-t="URL 결제 결과 화면 (성공/실패 큰 글씨·하단 안내)">URL 결제 결과 화면 (성공/실패 큰 글씨·하단 안내)</p></div>' +
                '<div class="col-md-6"><label class="form-label small mb-0" data-pg-ui-t="성공 — 안내 제목 (한국어)">성공 — 안내 제목 (한국어)</label>' +
                '<textarea class="form-control form-control-sm" id="hqPayCardCopyDraftResultOk1" rows="2" maxlength="500" data-pg-ui-placeholder="예: 결제가 완료되었습니다." placeholder="예: 결제가 완료되었습니다."></textarea></div>' +
                '<div class="col-md-6"><label class="form-label small mb-0" data-pg-ui-t="성공 — 하단 안내 (한국어)">성공 — 하단 안내 (한국어)</label>' +
                '<textarea class="form-control form-control-sm" id="hqPayCardCopyDraftResultOk2" rows="2" maxlength="2000" data-pg-ui-placeholder="예: 팝업으로 열렸다면 이 창을 닫아 주세요.…" placeholder="예: 팝업으로 열렸다면 이 창을 닫아 주세요.…"></textarea></div>' +
                '<div class="col-md-6"><label class="form-label small mb-0" data-pg-ui-t="실패·미완료 — 안내 제목 (한국어)">실패·미완료 — 안내 제목 (한국어)</label>' +
                '<textarea class="form-control form-control-sm" id="hqPayCardCopyDraftResultFail1" rows="2" maxlength="500" data-pg-ui-placeholder="예: 결제가 완료되지 않았거나 실패했습니다." placeholder="예: 결제가 완료되지 않았거나 실패했습니다."></textarea></div>' +
                '<div class="col-md-6"><label class="form-label small mb-0" data-pg-ui-t="실패·미완료 — 하단 안내 (한국어)">실패·미완료 — 하단 안내 (한국어)</label>' +
                '<textarea class="form-control form-control-sm" id="hqPayCardCopyDraftResultFail2" rows="2" maxlength="2000" data-pg-ui-placeholder="예: 팝업으로 열렸다면 이 창을 닫아 주세요.…" placeholder="예: 팝업으로 열렸다면 이 창을 닫아 주세요.…"></textarea></div>' +
                '<div class="col-12 d-flex flex-wrap gap-2 align-items-center">' +
                '<button type="button" class="btn btn-sm btn-primary" id="hqPayCardCopyBtnSave" data-pg-ui-t="저장">저장</button>' +
                '<button type="button" class="btn btn-sm btn-outline-primary" id="hqPayCardCopyBtnSaveI18n" data-pg-ui-t="저장(다국어)">저장(다국어)</button>' +
                '<button type="button" class="btn btn-sm btn-outline-secondary" id="hqPayCardCopyBtnCancelEdit" data-pg-ui-t="수정 취소">수정 취소</button>' +
                '</div>' +
                '<div class="col-12"><p class="small text-primary mb-0 d-none" id="hqPayCardCopyEditBanner" role="status"></p></div>' +
                '</div>' +
                '<div class="hq-pay-card-copy-grid-wrap w-100">' +
                '<table class="table table-sm table-bordered align-middle mb-0 hq-pay-card-copy-grid table-no-col-resize">' +
                '<colgroup><col class="hq-pay-card-copy-col-pg"><col><col><col><col><col><col><col><col><col><col style="width:3.25rem"><col style="width:3.25rem"><col style="width:3.25rem"></colgroup>' +
                '<thead class="table-light"><tr><th scope="col" data-pg-ui-t="PG">PG</th><th scope="col"  data-pg-ui-title="한국어 제목" title="한국어 제목" data-pg-ui-t="제목(한)">제목(한)</th><th scope="col"  data-pg-ui-title="결제 금액 하단 안내" title="결제 금액 하단 안내" data-pg-ui-t="금액안내">금액안내</th><th scope="col" data-pg-ui-t="내용1">내용1</th><th scope="col" data-pg-ui-t="내용2">내용2</th><th scope="col" data-pg-ui-t="내용3">내용3</th>' +
                '<th scope="col"  data-pg-ui-title="URL 결제 결과 — 성공 큰 글씨(한)" title="URL 결제 결과 — 성공 큰 글씨(한)" data-pg-ui-t="성공 제목">성공 제목</th><th scope="col"  data-pg-ui-title="URL 결제 결과 — 성공 하단 안내(한)" title="URL 결제 결과 — 성공 하단 안내(한)" data-pg-ui-t="성공 하단">성공 하단</th>' +
                '<th scope="col"  data-pg-ui-title="URL 결제 결과 — 실패 큰 글씨(한)" title="URL 결제 결과 — 실패 큰 글씨(한)" data-pg-ui-t="실패 제목">실패 제목</th><th scope="col"  data-pg-ui-title="URL 결제 결과 — 실패 하단 안내(한)" title="URL 결제 결과 — 실패 하단 안내(한)" data-pg-ui-t="실패 하단">실패 하단</th>' +
                '<th class="text-center" scope="col" data-pg-ui-t="활성">활성</th><th class="text-center" scope="col" data-pg-ui-t="수정">수정</th><th class="text-center" scope="col" data-pg-ui-t="삭제">삭제</th></tr></thead>' +
                '<tbody id="hqPayCardCopyTbody"></tbody></table></div>' +
                pgUiParagraphHtml('이 블록의 <strong>저장</strong>은 목록에만 반영됩니다. 서버(DB) 반영은 화면 맨 아래 <strong>저장</strong>이 필요합니다.', 'text-muted small mt-2 mb-0') + '</div>'
            }]
          ]
        },
        {
          title: '확장형 PG 레지스트리',
          notice: '향후 PG사 추가를 위해 벤더별 기능/방식/엔드포인트를 JSON으로 관리합니다. 기본 구조를 유지한 채 vendors 배열에 계속 추가하면 됩니다.',
          rows: [
            [{ label: '결제연동 레지스트리(JSON)', type: 'textarea', name: 'paymentProviderRegistryJson', col: 8, rows: 16,
               placeholder: '{\n  "version": 1,\n  "vendors": [\n    {\n      "vendorCode": "CHILLPAY",\n      "vendorName": "칠리페이",\n      "integrationTypes": ["API_BROKER", "URL_PAY"],\n      "flowTypes": ["INLINE", "REDIRECT"],\n      "activeYn": "Y"\n    }\n  ]\n}' }]
          ]
        }
      ],
      buttons: [{ id: 'hqPaymentOrchSaveBtn', label: '저장', cls: 'btn-primary' }]
    },
    '/hq/permissionMng': {
      orgPagePermissionMatrix: true,
      hideListGrid: true,
      summary: [],
      buttons: [],
      columns: []
    },
    '/hq/opsModeMng': {
      hqOpsModeMng: true,
      hideListGrid: true,
      summary: [],
      buttons: [],
      columns: []
    },
    '/hq/accountMng': {
      emptyMessage: '등록된 업체별 접근 규칙이 없습니다.',
      noticeList: [
        '<strong>총본사·본사·총판</strong> 소속 로그인 ID만 등록할 수 있고, <strong>허용 업체코드</strong>는 <strong>전 업체 코드</strong> 중에서 선택합니다. 허용 업체를 고른 뒤 사용자 ID를 선택하면, 그 사용자는 사용자관리 등에서 <strong>지정한 업체 코드에만</strong> 접근할 수 있으며(하위 가맹점을 자동으로 넓혀 주지 않음), 상위 조직 권한으로 이미 볼 수 있는 범위와는 별개로 여기서는 <strong>명시한 코드</strong>만큼만 열어 줍니다.',
        '행이 하나라도 있으면 사용자관리 목록·등록·초기화 범위는 <strong>하위 조직 ∩ 여기서 지정한 업체</strong>로만 제한됩니다. 담당자(ASSISTANT) 메뉴는 [본사권한설정]의 담당자 권한그룹별 메뉴에서 조정하고, OTP·로그인 정책은 [사용자설정]을 따릅니다.',
        '목록 <strong>수정</strong>·<strong>삭제</strong>, 상단 <strong>저장</strong>·<strong>삭제</strong>(행 체크), 추가·수정 창의 <strong>저장</strong>으로 적용합니다.'
      ],
      searchRows: [[{ type: 'searchBtn', label: '새로고침' }]],
      summary: ['건수'],
      buttons: [
        { id: 'searchBtn', label: '새로고침', cls: 'btn-primary' },
        { id: 'hqAccountAccessAddBtn', label: '접근권한 추가', cls: 'btn-success' },
        { id: 'hqAccountAccessSaveBtn', label: '저장', cls: 'btn-primary' },
        { id: 'hqAccountAccessBulkDelBtn', label: '삭제', cls: 'btn-outline-danger' }
      ],
      columns: [
        { key: '_chk', type: 'checkbox' },
        { key: 'rowNo', label: '번호' },
        { key: 'compNm', label: '업체명' },
        { key: 'compCode', label: '업체코드' },
        { key: 'username', label: '사용자ID' },
        { key: 'regDt', label: '등록일시' },
        { key: 'id', type: 'accountAccessActions', label: '관리' }
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
      buttons: [
        { id: 'noticeWriteBtn', label: '글작성', cls: 'btn-success', noticeToolbar: true },
        { id: 'noticeLoginHomeBtn', label: '첫화면', cls: 'btn-outline-primary', noticeToolbar: true, noticeHqOnly: true },
        { id: 'noticeLoginPopupBtn', label: '접속팝업', cls: 'btn-outline-primary', noticeToolbar: true, noticeHqOnly: true },
        { id: 'noticePostLoginPopupBtn', label: '팝업', cls: 'btn-outline-primary', noticeToolbar: true },
        { id: 'noticeMainNoticeBtn', label: '메인공지', cls: 'btn-outline-primary', noticeToolbar: true },
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' },
        { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      columns: [
        { key: '_chk', label: '', type: 'checkbox' },
        { key: 'rowNo', label: '번호' },
        { key: 'compNm', label: '업체명' },
        { key: 'compId', label: '업체코드' },
        { key: 'title', label: '제목' },
        { key: 'deployTargetLabel', label: '배포대상' },
        { key: 'writerNm', label: '작성자' },
        { key: 'showOnLogin', label: '첫화면' },
        { key: 'showAsPopup', label: '접속팝업' },
        { key: 'showPostLoginPopup', label: '팝업' },
        { key: 'showOnMain', label: '메인공지' },
        { key: 'regDt', label: '작성일' },
        { key: 'hitCnt', label: '조회수' },
        { key: '_noticeAct', type: 'noticeActions', label: '비고' }
      ],
      noticeList: [
        '기본 조회 기간은 당월(1일~말일)입니다. 가장 최근 공지 1건은 작성일이 기간 밖이어도 목록 맨 위에 「최근」으로 항상 표시됩니다.',
        '공지 등록은 총본사·본사·총판(화면 권한 수정 이상)만 가능합니다. 접속팝업·첫화면은 총본사 전용이며, 본사·총판은 팝업·메인공지만 사용할 수 있습니다.',
        '배포 대상 「특정지점」은 업체코드/이름 검색 또는 조직레벨→지점 선택으로 추가합니다. 검색·선택은 본인 하위 조직만 가능합니다.'
      ]
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
          title: '기본정보',
          notice: '로그인한 계정에 연결된 소속 업체 정보가 자동으로 표시됩니다. 아래에서 조회·수정합니다.',
          rows: [
            [{ label: '업체코드', type: 'text', name: 'compId', col: 2, readonly: true }, { label: '상위 본사', type: 'text', name: 'parentComp', col: 2, readonly: true, placeholder: '상위 코드' }, { label: '업체구분', type: 'select', name: 'compDiv', options: [{ v: '', t: '선택' }, { v: 'HEADQUARTERS', t: '총본사' }, { v: 'REGIONAL', t: '본사' }, { v: 'MASTER_DIST', t: '총판' }, { v: 'BRANCH', t: '지사' }, { v: 'AGENCY', t: '대리점' }, { v: 'SALES_OFFICE', t: '영업점' }, { v: 'MERCHANT', t: '가맹점' }], col: 2 }],
            [{ label: '업체명', type: 'text', name: 'compNm', col: 2 }, { label: '사업자번호', type: 'regNoWithType', name: 'regNo', col: 2 }, { label: '업태', type: 'text', name: 'bizType', col: 2 }, { label: '종목', type: 'text', name: 'industry', col: 2 }],
            [{ label: '대표자명', type: 'text', name: 'ceoNm', col: 2 }, { label: '휴대폰', type: 'text', name: 'ceoMobile', col: 2 }, { label: '업체전화', type: 'text', name: 'compTel', col: 2 }, { label: '팩스', type: 'text', name: 'fax', col: 2 }, { label: '이메일', type: 'text', name: 'email', col: 2 }],
            [{ type: 'countryAddressRow', zipLabel: '우편번호', addrLabel: '주소', addrDetailLabel: '상세주소', addrEtcLabel: '기타' }],
            [{ label: '사용여부', type: 'select', name: 'useYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }, { v: 'S', t: '영구정지' }], col: 1 }, { label: '태블릿 UI 기능', type: 'select', name: 'tabletFeatureUseYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 }, { label: '대표 아이디 (중복검사)', type: 'text', name: 'loginId', col: 2, button: '중복확인' }, { label: '비밀번호', type: 'passwordReset', name: 'pwdReset', col: 2 }],
            [{ label: '보조 아이디 (중복검사)', type: 'text', name: 'assistantLoginId', col: 2, button: '중복확인' }, { type: 'assistantPasswordManage', col: 2 }]
          ]
        },
        {
          title: '가맹점 상세정보',
          id: 'merchantBasicDetailCard',
          merchantOnly: true,
          rows: [
            [{ label: '사업자형태', type: 'text', name: 'bizNature', col: 2 }, { label: '취급물품', type: 'text', name: 'product', col: 2 }, { label: '대표사이트', type: 'text', name: 'homepage', col: 2, placeholder: 'https://' }, { label: '정산담당자명', type: 'text', name: 'settleName', col: 2 }, { label: '정산담당자연락처', type: 'text', name: 'settleTelNo', col: 2, placeholder: '010-0000-0000' }]
          ]
        },
        {
          title: '계좌정보',
          id: 'settlementAccountCard',
          distributorMerchantOnlyNoRegional: true,
          rows: [
            [{ type: 'countryBankRow', bankLabel: '계좌은행*', accountNoLabel: '계좌번호*', accountHolderLabel: '예금주*' }],
            [{ label: 'SWIFT', type: 'text', name: 'swift', col: 2, placeholder: 'SWIFT 코드' }, { label: '지점이름', type: 'text', name: 'branchName', col: 2 }, { label: '지점 주소', type: 'text', name: 'branchAddr', col: 2 }, { label: '담당전화번호', type: 'text', name: 'contactTel', col: 2 }],
            [{ label: '코인 지갑 주소', type: 'text', name: 'walletAddress', col: 4, placeholder: '코인 수취 지갑 주소' }, { label: '네트워크', type: 'text', name: 'networkName', col: 2, placeholder: '네트워크 이름' }]
          ]
        },
        {
          title: '출금 제한 설정',
          id: 'withdrawLimitCard',
          merchantOnly: true,
          notice: '가맹점 출금 제한 유형입니다. 매일·공휴일·공휴일 전날(17·18시) 규칙은 본사 영업일·공휴일 캘린더와 함께 출금 처리 시 해석합니다. 평일 구간은 시작·종료 시각으로 좁힙니다.',
          rows: [
            [{ label: '출금제한 유형', type: 'select', name: 'withdrawRestrictType', options: WITHDRAW_POLICY_OPTIONS, col: 2 }, { label: '시작시간', type: 'time', name: 'withdrawStartTime', col: 1 }, { label: '종료시간', type: 'time', name: 'withdrawEndTime', col: 1 }]
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
          notice: '결제 정산금 중 보류율(%)만큼 보류기간(일) 동안 지급하지 않으며, 정산일자+보류기간 경과 후 정산금으로 전환됩니다. 보류 해지일이 공휴일이면 익영업일에 전환됩니다. 본사정책 따름 시 본사 수수료 정책(롤링 비율/일수)에 연동됩니다.',
          rows: [
            [{ label: '본사정책 따름', type: 'select', name: 'holdRateFollowHq', options: [{ v: 'Y', t: '본사정책 따름' }, { v: 'N', t: '직접입력' }], col: 2 }],
            [{ label: '보류율(%)', type: 'text', name: 'holdRate', col: 1, placeholder: '5', holdRateOnly: true }, { label: '보류기간(일)', type: 'text', name: 'holdDays', col: 1, placeholder: '120', holdRateOnly: true }]
          ]
        },
        {
          title: '무효·환불 정산 (안내)',
          id: 'voidRefundSettleGuideCard',
          merchantOnly: true,
          cardExtraClass: 'pg-comp-reg-void-refund-panel',
          rows: [
            [{ type: 'customHtml', col: 12, html: merchantVoidRefundGuideHtml }],
            [{ label: '무효 정산(21·40)', type: 'select', name: 'voidSettlementMode', options: VOID_REFUND_SETTLE_MODE_OPTIONS.concat([{ v: 'FOLLOW', t: '총판·본사 따름' }]), col: 3, voidRefundSettlementModeField: true }, { label: '수동무효 정산(22·41)', type: 'select', name: 'manualVoidSettlementMode', options: VOID_REFUND_SETTLE_MODE_OPTIONS.concat([{ v: 'FOLLOW', t: '총판·본사 따름' }]), col: 3, voidRefundSettlementModeField: true }, { label: '환불 정산(30·42)', type: 'select', name: 'refundSettlementMode', options: VOID_REFUND_SETTLE_MODE_OPTIONS.concat([{ v: 'FOLLOW', t: '총판·본사 따름' }]), col: 3, voidRefundSettlementModeField: true }, { label: '강제환불(31)', type: 'select', name: 'forceRefundSettlementMode', options: VOID_REFUND_SETTLE_MODE_OPTIONS.concat([{ v: 'FOLLOW', t: '총판·본사 따름' }]), col: 3, voidRefundSettlementModeField: true }]
          ]
        },
        {
          title: '수수료정책',
          id: 'commissionPolicyCard',
          merchantRegionalMasterCommission: true,
          notice: '본사정책 따름이면 [본사 정책선택]에서 사용합니다. 목록에는 배포(Y)인 템플릿만 나오며, 가맹점 기준통화와 정책 통화코드가 같거나 정책 통화가 비어 있는 항목만 표시됩니다. 본사·총판·가맹점에 동일하게 적용·저장됩니다. 첫 항목(본사 기본 템플릿)은 선택값이 비어 있을 때 본사의 기본(DEFAULT) 수수료 템플릿을 씁니다.',
          rows: [
            [{ label: '본사정책 따름', type: 'select', name: 'commissionFollowHq', options: [{ v: 'Y', t: '본사정책 따름' }, { v: 'N', t: '직접입력' }], col: 2 }, { label: '본사 정책선택', type: 'select', name: 'hqPolicyScope', options: [{ v: '', t: '본사 기본 템플릿 (DEFAULT)' }], col: 2, hqPolicyOnly: true }],
            [{ label: '결제수수료율(%)', type: 'text', name: 'payRate', col: 2, customOnly: true }, { label: '실패수수료(건)', type: 'text', name: 'failFee', col: 2, customOnly: true }, { label: '취소수수료(건)', type: 'text', name: 'cancelRate', col: 2, customOnly: true }],
            [{ label: '무효수수료(건)', type: 'text', name: 'voidFeePerTx', col: 2, customOnly: true, placeholder: '거래 21' }, { label: '수무효수수료(건)', type: 'text', name: 'manualVoidFeePerTx', col: 2, customOnly: true, placeholder: '거래 22' }, { label: '환불수수료(건)', type: 'text', name: 'refundRate', col: 2, customOnly: true }],
            [{ label: '월간이용료(월 1회·고정)', type: 'text', name: 'usageRate', col: 2, customOnly: true, placeholder: '통화코드 단위 금액' }, { label: '비고', type: 'text', name: 'commissionMemo', col: 2, customOnly: true }],
            [{ label: '정산수수료(건)', type: 'text', name: 'feeSettlementPerTx', col: 2, customOnly: true }, { label: '송금수수료', type: 'text', name: 'remittanceTransferFee', col: 2, customOnly: true }, { label: 'USDT 송금수수료(건)', type: 'text', name: 'usdtTransferFeeUsd', col: 2, customOnly: true }, { label: 'USDT수수료율(%)', type: 'text', name: 'feeUsdt', col: 2, customOnly: true }, { label: 'FX수수료율(%)', type: 'text', name: 'feeFx', col: 2, customOnly: true }, { label: '3DS 고정(건)', type: 'text', name: 'fee3dsRate', col: 2, customOnly: true }]
          ]
        },
        {
          title: '차지백 정책',
          id: 'chargebackPolicyCard',
          merchantOnly: true,
          notice: '본사정책 따름이면 위에서 고른 본사 정책 템플릿의 3DS·차지백 설정이 적용됩니다. 직접입력일 때만 아래를 저장할 수 있습니다.',
          rows: [
            [{ label: '차지백수수료(건)', type: 'text', name: 'chargebackFeePerTx', col: 2, customOnly: true }, { label: '차지백 구간정책', type: 'select', name: 'chargebackPolicyId', col: 4, options: [{ v: '', t: '(미사용) 건당 차지백만' }] }]
          ]
        },
        {
          title: '결제 후속조치 (가맹점 관리자)',
          id: 'payFollowMerchantCard',
          merchantOnly: true,
          notice: '관리자 화면의 자동무효·이메일무효·자동환불·강제환불 사용 여부입니다. 전산설정관리(전역) 및 본사권한설정의 조직 단계 상한과 함께 적용됩니다. [기본·종전]은 미설정과 동일(허용으로 해석)입니다.',
          rows: [
            [{ label: '후속조치 사용', type: 'select', name: 'payFollowMerchantUseYn', options: [{ v: '', t: '기본·종전과 동일' }, { v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 }],
            [{ label: '자동무효', type: 'select', name: 'payFollowAutoVoidYn', options: [{ v: '', t: '기본·종전과 동일' }, { v: 'Y', t: '허용' }, { v: 'N', t: '불가' }], col: 2 }, { label: '이메일 무효', type: 'select', name: 'payFollowEmailVoidYn', options: [{ v: '', t: '기본·종전과 동일' }, { v: 'Y', t: '허용' }, { v: 'N', t: '불가' }], col: 2 }, { label: '자동환불', type: 'select', name: 'payFollowAutoRefundYn', options: [{ v: '', t: '기본·종전과 동일' }, { v: 'Y', t: '허용' }, { v: 'N', t: '불가' }], col: 2 }, { label: '강제환불', type: 'select', name: 'payFollowForceRefundYn', options: [{ v: '', t: '기본·종전과 동일' }, { v: 'Y', t: '허용' }, { v: 'N', t: '불가' }], col: 2 }]
          ]
        },
        {
          title: '정산방법',
          id: 'calcMethodCard',
          merchantOnly: true,
          notice: CALC_METHOD_MERCHANT_NOTICE,
          rows: [
            [{ label: '정산주기', type: 'select', name: 'calcCycle', options: CALC_CYCLE_OPTIONS, col: 1 }, { label: '정산구분', type: 'select', name: 'calcProcType', options: CALC_PROC_OPTIONS, col: 1 }, { label: '정산마감시간', type: 'time', name: 'calcCloseTime', col: 1 }, { label: '정산자동개시시간', type: 'time', name: 'calcStartTime', col: 1 }],
            [{ label: '정산주기 적용', type: 'select', name: 'calcCycleTransitionMode', options: [{ v: 'IMMEDIATE', t: '즉시 적용' }, { v: 'NEXT_AFTER_RUN', t: '다음 정산 실행 후(예약)' }], col: 2 }, { label: '변경 비고', type: 'text', name: 'calcCycleChangeRemark', col: 3, placeholder: '선택' }],
            [{ type: 'customHtml', col: 12, html: '<div data-pg="calcCyclePendingBanner" class="alert alert-warning py-2 px-3 small d-none mb-0"></div>' }],
            [{ label: '이체및송금구분', type: 'select', name: 'transferType', options: TRANSFER_REMIT_OPTIONS, col: 1 }, { label: '이체주기(분)', type: 'text', name: 'transferCycleDays', col: 1, placeholder: '예: 5, 60' }, { label: '이체시간', type: 'time', name: 'transferExecTime', col: 1 }],
            [{ label: '정산제외여부', type: 'select', name: 'calcExcludeYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 1 }, { label: '정산제외대상', type: 'select', name: 'calcExcludeTarget', options: [{ v: 'NONE', t: '해당없음' }, { v: 'WEB', t: 'WEB' }, { v: 'OFFLINE', t: '오프라인' }, { v: 'BOTH', t: 'WEB+오프라인' }], col: 1 }, { label: '지급보류', type: 'select', name: 'payHoldYn', options: [{ v: 'N', t: '지급' }, { v: 'Y', t: '보류' }], col: 1 }],
            [{ label: '정산최소금액', type: 'text', name: 'calcMinAmt', col: 1, placeholder: '미만 시 다음 주기' }, { label: '이체및송금최소금액', type: 'text', name: 'autoTransferMin', col: 1, placeholder: '펌뱅킹 최소' }, { label: 'VAT', type: 'select', name: 'feeVatApplyYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 1 }, { label: 'VAT율(%)', type: 'text', name: 'feeVatRatePct', col: 1, placeholder: '수수료 대비 %', feeVatRateOnly: true }]
          ]
        },
        {
          title: '통합정산설정',
          id: 'integratedSettleCard',
          merchantOnly: true,
          notice: '칠페이 통합정산 화면의 「예정(ICOPAY)」 표시에만 쓰입니다. 배포설정 API연동설정과 동일 규칙을 쓰려면 예정모드를 연동기본으로 두세요. 아래 값은 [저장] 시 등록된 모든 결제대행사 행에 동일하게 적용됩니다.',
          rows: [
            [
              { label: '예정모드', type: 'select', name: 'merchantPgExtSettleMode', col: 2, options: [{ v: '', t: '연동기본' }, { v: 'OFF', t: '가맹:미표시' }, { v: 'T', t: 'T+N' }, { v: 'D', t: 'D+N' }] },
              { label: 'N', type: 'number', name: 'merchantPgExtSettleLag', col: 1, min: 1, max: 10, step: 1, placeholder: 'T/D 시 1~10' },
              { label: 'D시각(일괄)', type: 'time', name: 'merchantPgExtSettleBatchTime', col: 1 }
            ]
          ]
        },
        {
          type: 'pgBindingList',
          title: '결제대행사 설정',
          id: 'pgBindingCard',
          merchantOnly: true,
          omitExtSettleColumns: true,
          notice: '배포설정 > API연동설정에서 사용(Y)으로 등록된 결제대행사가 목록에 표시됩니다. PG를 고르면 API연동설정의 MID·Route 등이 기본값으로 채워지며, 가맹점 전용 값은 수정·저장하면 됩니다. 예정모드·N·D시각은 위 「통합정산설정」에서 일괄 지정합니다. URL·챗봇·API 결제는 운영(체크)를 여러 행에 켤 수 있습니다. 체크된 행은 붉은 배경(파스텔)으로 표시됩니다. 노티 전용 PG는 카드브랜드가 ALL로 고정됩니다. 하단 [저장] 시 한꺼번에 반영됩니다.'
        },
        {
          title: '웹결제 사용 / 대표 기본상품정보 (온라인 URL 결제용)',
          id: 'webPaymentCard',
          merchantOnly: true,
          notice: merchantWebPaymentCardNoticeKo(),
          rows: merchantWebPaymentCardRows('가맹점 저장 후 조회')
        },
        merchantApiUrlPayCheckoutCardSection(),
        merchantJpayCheckoutFieldModeCardSection(),
        merchantApiIntegrationChannelsCardSection(),
        merchantJpayApiSubscriptionCardSection(),
        merchantSplitPayCardSection(),
        {
          title: '챗봇결제 설정',
          id: 'chatbotPaymentCard',
          merchantOnly: true,
          notice: '미사용이면 로그인한 가맹점 관리자에게 챗봇관리의 상품관리 메뉴가 표시되지 않습니다. 「URL 결제 방식」은 챗봇 주문·카탈로그 결제에만 적용되며 공개 URL·API 중계와 별도로 선택할 수 있습니다. 재결제 URL 은 본사 URL 재결제 기능 ON 및 URL재결제 PG 바인딩이 필요합니다. 챗봇결제 URL은 챗봇 쇼핑·주문 진입용입니다.',
          rows: [
            [{ label: '챗봇결제 사용여부', type: 'select', name: 'chatbotPaymentUseYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 2 },
              { label: '챗봇 상품등록 한도(건)', type: 'select', name: 'chatbotProductSlotLimit', col: 2,
              options: [
                { v: '', t: '—' }, { v: '10', t: '10' }, { v: '20', t: '20' }, { v: '50', t: '50' }, { v: '80', t: '80' },
                { v: '100', t: '100' }, { v: '150', t: '150' }, { v: '200', t: '200' }
              ] }],
            [{
              type: 'customHtml', col: 12,
              html: function chatbotHeaderLogoFieldBlock() {
                var phLogo = '업로드 시 자동 반영 · 또는 HTTPS URL 직접 입력';
                var logoHint = 'PNG·JPEG, 원본 최대 40MB. 서버에서 목표 2MB 이하(본사 AI챗봇설정 변경 가능)로 재압축합니다. chatbot_logo_llm_tune_yn=Y 일 때 AI챗봇설정 순위 LLM으로 권장 픽셀을 잡습니다.';
                return '<div class="form-field-block chatbot-header-logo-upload-block w-100">' +
                  '<label class="form-label" data-pg-ui-t="챗봇 상단 로고">' + escUi(L('챗봇 상단 로고')) + '</label>' +
                  '<div class="input-group input-group-sm mb-1">' +
                  '<input type="text" class="form-control form-control-sm" name="chatbotHeaderLogoUrl" id="chatbotHeaderLogoUrl" ' +
                  'placeholder="' + escUi(L(phLogo)) + '" data-pg-ui-placeholder="' + escUi(phLogo) + '">' +
                  '<input type="file" class="d-none" id="chatbotHeaderLogoFile" accept="image/png,image/jpeg,image/jpg">' +
                  '<button type="button" class="btn btn-outline-secondary btn-sm" id="chatbotHeaderLogoBrowse"><span data-pg-ui-t="파일 선택">' + escUi(L('파일 선택')) + '</span></button>' +
                  '<button type="button" class="btn btn-outline-primary btn-sm" id="chatbotHeaderLogoUpload"><span data-pg-ui-t="업로드·최적화">' + escUi(L('업로드·최적화')) + '</span></button>' +
                  '</div>' +
                  '<div class="form-text text-muted small" data-pg-ui-t="' + escUi(logoHint) + '">' +
                  escUi(L(logoHint)) +
                  '</div></div>';
              }
            }],
            [{ label: '챗봇 관리자(로그인ID·중복검사)', type: 'text', name: 'chatbotAdminUsername', col: 12, button: '중복확인', placeholder: '가맹당 1명 · 없는 ID는 저장 시 자동 등록(초기비밀번호: ID+1!) · 공개 챗봇 상품관리 로그인에는 OTP 필요 · 비우면 해제' }],
            [{ label: 'URL 결제 방식', type: 'select', name: 'chatbotUrlPayCheckoutMode', options: [{ v: 'STANDARD', t: '일반 URL 결제' }, { v: 'REPAY', t: '재결제 URL (저장 카드)' }], col: 3 }],
            [{ type: 'customHtml', col: 12, html: function () { return merchantChatbotPaymentUrlRowHtml('가맹점 저장 후 조회'); } }],
            [{ type: 'customHtml', col: 12, html: function () { return merchantChatbotEmbedScriptRowHtml('가맹점 저장 후 조회'); } }],
            [{ type: 'customHtml', col: 12, html: function () { return merchantChatbotQrRowHtml(); } }],
            [{ label: '가맹 활성 카탈로그 유형(CSV)', type: 'text', name: 'chatbotCatalogListingEnabled', col: 12,
              placeholder: 'SALE,RESERVATION_TIME 예: 시간예약만' }]
          ]
        },
        {
          title: '챗봇 카탈로그(산하 허용·이미지)',
          id: 'chatbotCatalogPolicyCard',
          headOfficeTierOnly: true,
          notice: '총본사·본사·총판만 설정합니다. 비우면 해당 단계에서 제한 없음(상위·시스템 기본). 산하 가맹 실효값은 체인 최소(교집합·이미지 장수)입니다.',
          rows: [
            [{ label: '산하 허용 상품유형(CSV)', type: 'text', name: 'chatbotCatalogListingGrant', col: 8,
              placeholder: 'SALE,RESERVATION_TIME,RESERVATION_PLACE' }],
            [{ label: '상품 이미지 장수 상한(1~4)', type: 'select', name: 'chatbotMaxProductImagesGrant', col: 4,
              options: [
                { v: '', t: '— 미지정' }, { v: '1', t: '1' }, { v: '2', t: '2' }, { v: '3', t: '3' }, { v: '4', t: '4' }
              ] }]
          ]
        },
        {
          type: 'branding',
          title: '브랜딩 설정',
          id: 'brandingCard',
          notice: '본사에서 [배경/로고 변경권한]을 부여한 가맹점은 메인·로고·테마를 수정할 수 있습니다. 메인이미지=로그인 화면 왼쪽 배경, 로고=로그인창 상단·사이드바 상단.'
        },
        {
          title: '기타',
          id: 'regionalMiscCard',
          headOfficeTierOnly: true,
          notice: '본사/총판 공통 설정입니다. COPYRIGHT에 입력한 문구는 화면 하단에 표시됩니다.',
          rows: [[{ label: 'COPYRIGHT', type: 'textarea', name: 'copyright', col: 6, placeholder: 'Copyright © 2025 ICOPAY Service by Ontheline Co., Ltd.' }, { label: '비고', type: 'textarea', name: 'remark', col: 6 }]]
        },
        {
          title: '기타',
          id: 'merchantMiscCard',
          merchantOnly: true,
          rows: [
            [{ label: '비고', type: 'textarea', name: 'remark', col: 6 }]
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
          title: 'JPAY 수신통보 URL',
          id: 'jpayNotifyUrlCard',
          merchantOnly: true,
          notice: 'J-Pay pay_index 전문의 pay_notifyurl·pay_callbackurl 에 사용됩니다. 노티미들웨어 가맹 수신 URL을 등록하세요. 비우면 ICOPAY ingress(cbJpay/rsJpay) 기본값을 사용합니다.',
          rows: [
            [{ label: 'Notify (pay_notifyurl) / Callback URL (NOTI MW)', type: 'text', name: 'jpayNotifyUrl', col: 5, placeholder: 'https://' }, { label: 'Callback (pay_callbackurl) / Result URL (NOTI MW)', type: 'text', name: 'jpayCallbackUrl', col: 5, placeholder: 'https://' }]
          ]
        },
        {
          title: 'URL·챗봇 결제 승인 알림',
          id: 'urlPaySuccessAlertCard',
          merchantOnly: true,
          notice: '인라인 DirectCredit(URL·챗봇) 승인 시 PG중계 JSON 전송과 함께 LINE Notify·대표 이메일(전산 SMTP) 알림을 보낼 수 있습니다. 토큰은 비우면 기존 유지, 삭제는 __CLEAR__.',
          rows: [
            [{ label: '승인 알림메일', type: 'select', name: 'urlPayAlertEmailYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용 (대표 이메일)' }], col: 2 }],
            [{ label: 'LINE Notify 토큰', type: 'password', name: 'urlPayLineNotifyToken', col: 6, placeholder: '변경 시만 입력 · 삭제: __CLEAR__' }]
          ]
        },
        { title: '첨부파일', rows: [[{ type: 'file', name: 'attach', col: 12, blockExtraClass: 'attach-field-block' }]] }
      ],
      compInfoDetailButtons: [{ id: 'compInfoUpdateBtn', label: '수정 저장', cls: 'btn-primary' }]
    },
    '/comp/compMngTree': {
      /** 상단 그리드: `table.table` + `id^=grid_` → table-column-resize.js 가 thead 에 드래그 핸들 부착·너비 localStorage 저장 */
      compMngTreeGrid: true,
      searchFormClass: 'comp-mng-search-multiline',
      searchRows: [
        [
          { label: '업체구분', type: 'select', name: 'searchCompDiv', options: [{ v: '', t: '전체' }, { v: 'REGIONAL', t: '본사' }, { v: 'MASTER_DIST', t: '총판' }, { v: 'BRANCH', t: '지사' }, { v: 'AGENCY', t: '대리점' }, { v: 'SALES_OFFICE', t: '영업점' }, { v: 'MERCHANT', t: '가맹점' }], size: 10 },
          { label: '대표자명', type: 'text', name: 'searchCeoNm', size: 12 },
          { label: '업체사용상태', type: 'select', name: 'searchUseYn', i18nLblKey: 'compTreeSearchUseStatus', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }, { v: 'S', t: '영구정지' }, { v: 'ALL', t: '전체' }], size: 10 },
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
      noticeList: ['기본 조회는 업체사용상태가 사용인 업체만 표시합니다. 미사용·영구정지·전체는 셀렉트에서 선택하세요. 미사용(N)은 로그인은 가능하나 신규 결제·정산이 중단됩니다. 영구정지(S)는 연동 사용자 계정이 정지되며 로그인할 수 없습니다. 상위를 미사용/영구정지로 두면 하위 프로필도 함께 연쇄 처리됩니다.', '엑셀등록: [SAMPLE]으로 서식 있는 xlsx(헤더 색·표선·가운데 정렬)를 받아 예시 행을 수정·추가한 뒤 [엑셀등록]에 업로드하세요.'],
      summary: ['건수'],
      buttons: [{ id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }, { id: 'excelSampleBtn', label: 'SAMPLE', cls: 'btn-outline-secondary' }, { id: 'excelRegBtn', label: '엑셀등록', cls: 'btn-outline-success' }, { id: 'compRegBtn', label: '등록', cls: 'btn-danger' }],
      tableColumnGuide: true,
      columns: [
        { key: '_chk', type: 'checkbox' },
        { key: 'rowNo', label: '번호' },
        { key: 'compId', label: '업체코드' },
        { key: 'compNm', label: '업체명' },
        { key: 'compDivNm', label: '업체구분' },
        { key: 'baseCurrency', label: '통화', title: '총판·지사·대리점·영업점·가맹점만 표시. 지사 이하는 소속 총판 기준통화와 동일. 총본사·본사는 비움.' },
        { key: 'siteRoot', label: '루트', title: '결제대행사 설정의 루트번호', align: 'center' },
        { key: 'settlementAmt', label: '정산금' },
        { key: 'receivables', label: '미수금' },
        { key: 'regNo', label: '사업자번호' },
        { key: 'ceoNm', label: '대표자명' },
        { key: 'contact', label: '연락처' },
        { key: 'bankNm', label: '은행' },
        { key: 'accountNo', label: '계좌번호' },
        { key: 'transferFee', label: '송금수수료' },
        { key: 'payIntegrationMode', label: '방식', title: '가맹 결제 연동: 웹결제(Y) 및 브로커 시크릿 발급 시 API, 미발급 시 URL', align: 'center' },
        { key: 'apiIntegrationChannel', label: '채널', title: '가맹 API 연동 채널: IN=INLINE, RE=REDIRECT, WO=WordPress/WooCommerce. 복수 사용 시 IN/RE 형식.', align: 'center' },
        { key: 'calcCycle', label: '정산주기' },
        { key: 'calcProcType', label: '정산구분' },
        { key: 'transferType', label: '이체및송금' },
        { key: 'transferCycleHours', label: '이체주기(분)' },
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
          notice: '업체코드는 등록 저장 시에만 자동 부여되며(업체구분별 접두 2자리+순번 8자리), 부여 후에는 변경할 수 없습니다. 업체관리 목록에 동일 코드로 표시됩니다. 업체구분을 선택하면 해당 입력 항목이 표시됩니다. 조직 이동은 상위로만 가능하며(하위로 이동 불가), 이동 시 하위 전체가 함께 이동합니다. 사용여부 미사용 시 하위 전체 미사용, 가맹점은 상위 변경으로 개별 활성화할 수 있습니다. 비밀번호는 입력 후 옆 [저장]으로 확정한 뒤 하단 [저장]으로 등록하세요. 등록 후 비밀번호를 잊었거나 초기화가 필요하면 [업체정보조회] 또는 [업체정보] 상세에서 [비밀번호 초기화] 후 로그인ID+1! 로 로그인해 변경하면 됩니다.',
          rows: [
            [{ label: '상위 본사', type: 'text', name: 'parentComp', col: 2, button: '검색', placeholder: '상위 코드' }, { label: '업체구분*', type: 'select', name: 'compDiv', options: [{ v: '', t: '선택' }, { v: 'REGIONAL', t: '본사' }, { v: 'MASTER_DIST', t: '총판' }, { v: 'BRANCH', t: '지사' }, { v: 'AGENCY', t: '대리점' }, { v: 'SALES_OFFICE', t: '영업점' }, { v: 'MERCHANT', t: '가맹점' }], col: 1 }, { label: '업체명*', type: 'text', name: 'compNm', col: 2 }, { label: '사업자번호*', type: 'regNoWithType', name: 'regNo', col: 2 }, { label: '업태', type: 'text', name: 'bizType', col: 1 }, { label: '종목', type: 'text', name: 'industry', col: 1 }],
            [{ label: '대표자명*', type: 'text', name: 'ceoNm', col: 2 }, { label: '휴대폰*', type: 'text', name: 'ceoMobile', col: 2 }, { label: '업체전화*', type: 'text', name: 'compTel', col: 2 }, { label: '팩스', type: 'text', name: 'fax', col: 2 }, { label: '이메일', type: 'text', name: 'email', col: 2 }],
            [{ type: 'countryAddressRow', zipLabel: '우편번호*', addrLabel: '주소*', addrDetailLabel: '상세주소', addrEtcLabel: '기타' }],
            [{ label: '사용여부*', type: 'select', name: 'useYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }, { v: 'S', t: '영구정지' }], col: 1 }, { label: '태블릿 UI 기능', type: 'select', name: 'tabletFeatureUseYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 }, { label: '로그인ID*', type: 'text', name: 'loginId', col: 2, button: '중복확인' }, { label: '비밀번호*', type: 'password', name: 'pwd', col: 2, button: '저장', placeholder: '8자 이상 → 옆 [저장] 확정' }]
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
            [{ label: '코인 지갑 주소', type: 'text', name: 'walletAddress', col: 4, placeholder: '코인 수취 지갑 주소' }, { label: '네트워크', type: 'text', name: 'networkName', col: 2, placeholder: '네트워크 이름' }, { label: '크립토 이체 수수료(USD)', type: 'text', name: 'cryptoTransferFee', col: 2, placeholder: 'USD' }, { label: '이체수수료', type: 'text', name: 'transferFee', col: 2, placeholder: '기준화폐' }],
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
            [{ label: 'PG수수료(일반)*', type: 'text', name: 'pgFeeGeneral', col: 2, placeholder: '%' }, { label: '이체수수료', type: 'text', name: 'transferFee', col: 2, placeholder: '기준화폐' }, { label: '차액정산 월횟수', type: 'text', name: 'settleDiffMonthCnt', col: 2 }, { label: '정산보고서 은행*', type: 'select', name: 'settleReportBankCd', options: [{ v: '', t: '선택하세요' }, { v: '04', t: '국민' }, { v: '20', t: '우리' }, { v: '81', t: 'KEB하나' }, { v: '88', t: '신한' }, { v: '11', t: 'NH농협' }], col: 2 }],
            [{ label: 'PG수수료(삼성페이)', type: 'text', name: 'pgFeeSamsung', col: 2 }, { label: 'SMS수수료', type: 'text', name: 'smsFee', col: 2 }, { label: '세금계산서 이메일', type: 'text', name: 'taxInvoiceEmail', col: 2 }, { label: '계좌번호', type: 'text', name: 'settleAccountNo', col: 2 }],
            [{ label: '직결수수료', type: 'text', name: 'directFee', col: 2 }, { label: '솔루션수수료', type: 'text', name: 'solutionFee', col: 2, placeholder: '0.1%' }, { label: '예금주명*', type: 'text', name: 'settleAccountHolder', col: 2 }]
          ]
        },
        {
          title: '출금 제한 시간 설정',
          id: 'regionalWithdrawLimitCard',
          regionalOnly: true,
          notice: '본사 기본 출금 제한 정책입니다. 매일: 시작~종료 매일 적용. 공휴일: 당일 00:00~23:59 전면 제한, 그 외 영업일은 시작~종료. 공휴일 전날 17시/18시 이후: 전영업일 해당 시각~공휴일 23:59(시작이 17·18시보다 이르면 시작시간부터), 그 외 날은 시작~종료. 실제 출금 시 본사 영업일·공휴일 데이터와 함께 판단합니다.',
          rows: [
            [{ label: '출금제한 유형', type: 'select', name: 'withdrawRestrictType', options: WITHDRAW_POLICY_OPTIONS, col: 2 }, { label: '출금제한시작시간*', type: 'time', name: 'withdrawRestrictStartTime', col: 1 }, { label: '출금제한종료시간*', type: 'time', name: 'withdrawRestrictEndTime', col: 1 }]
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
          notice: '총판일 때만 입력합니다. 총판은 1가지 화폐만 지정할 수 있습니다. 필수 노티(URL 1·2)는 본사설정 > 노티구성설정에서 이 총판에 노티 대상을 연결하면 자동 반영되며 화면에서 수정할 수 없습니다. 보조(URL 3·4)는 [보조 쌍 선택] 또는 드롭다운으로 추가할 수 있습니다. 연결된 본사 수신 URL로 유입되는 노티가 이 총판 트리로 분기됩니다.',
          rows: [
            [{ label: '기준 화폐*', type: 'select', name: 'baseCurrency', options: [{ v: '', t: '선택' }, { v: 'KRW', t: 'KRW (원)' }, { v: 'USD', t: 'USD (달러)' }, { v: 'JPY', t: 'JPY (엔)' }, { v: 'THB', t: 'THB (바트)' }, { v: 'EUR', t: 'EUR (유로)' }], col: 2 }, { label: '사이트개요', type: 'text', name: 'siteSummary', col: 2, placeholder: '사이트개요' }, { label: '취급물품', type: 'text', name: 'product', col: 2 }, { label: '대표사이트', type: 'text', name: 'homepage', col: 2, placeholder: 'https://' }],
            [{ label: '정산담당자명', type: 'text', name: 'settleName', col: 2 }, { label: '정산담당자연락처', type: 'text', name: 'settleTelNo', col: 2, placeholder: '010-0000-0000' }, { label: '정산형태', type: 'select', name: 'settleType', options: [{ v: '', t: '선택' }, { v: 'M', t: '가맹점별' }, { v: 'G', t: '총판' }], col: 1 }, { label: '요율(%)', type: 'text', name: 'commissionRate', col: 1, placeholder: '요율' }, { label: '사용한도', type: 'text', name: 'limitAmt', col: 2, placeholder: '사용한도' }],
            [{ type: 'notifyPairButton', col: 2, pairLabel: '필수 노티', buttonText: '노티 쌍 선택', callbackField: 'notifyUrl1', resultField: 'notifyUrl2', hint: '본사 노티구성에서 연결 시 자동 입력됩니다.', titleHint: '본사설정 > 노티구성설정에서 총판에 노티 대상을 연결하세요.', readonly: true }, { label: '노티 CALLBACK (URL 1)*', type: 'select', name: 'notifyUrl1', col: 5, loadNotifyTargets: true, button: '노티선택', readonly: true }, { label: '노티 RESULT (URL 2)*', type: 'select', name: 'notifyUrl2', col: 5, loadNotifyTargets: true, button: '노티선택', readonly: true }],
            [{ type: 'notifyPairButton', col: 2, pairLabel: '보조 노티', buttonText: '보조 쌍 선택', callbackField: 'notifyUrl3', resultField: 'notifyUrl4', hint: 'URL 3·4를 같은 쌍으로 채웁니다.', titleHint: '보조 노티 URL 3·4를 한 번에 설정합니다.' }, { label: '노티 URL 3(보조)', type: 'select', name: 'notifyUrl3', col: 5, loadNotifyTargets: true, button: '노티선택' }, { label: '노티 URL 4(보조)', type: 'select', name: 'notifyUrl4', col: 5, loadNotifyTargets: true, button: '노티선택' }]
          ]
        },
        {
          title: '가맹점 상세 정보',
          id: 'merchantExtraCard',
          merchantOnly: true,
          notice: '가맹점일 때만 입력합니다. 기준 화폐를 비우고 저장하면 상위 총판·본사 프로필의 기준통화를 자동으로 상속합니다(결제내역 VIEW의 본사/총판/가맹 기준통화 열에 반영).',
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
            [{ label: '코인 지갑 주소', type: 'text', name: 'walletAddress', col: 4, placeholder: '코인 수취 지갑 주소' }, { label: '네트워크', type: 'text', name: 'networkName', col: 2, placeholder: '네트워크 이름' }]
          ]
        },
        {
          title: '출금 제한 설정',
          id: 'withdrawLimitCard',
          merchantOnly: true,
          notice: '가맹점 출금 제한 유형입니다. 매일·공휴일·공휴일 전날(17·18시) 규칙은 본사 영업일·공휴일 캘린더와 함께 출금 처리 시 해석합니다. 평일 구간은 시작·종료 시각으로 좁힙니다.',
          rows: [
            [{ label: '출금제한 유형', type: 'select', name: 'withdrawRestrictType', options: WITHDRAW_POLICY_OPTIONS, col: 2 }, { label: '시작시간', type: 'time', name: 'withdrawStartTime', col: 1 }, { label: '종료시간', type: 'time', name: 'withdrawEndTime', col: 1 }]
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
          notice: '결제 정산금 중 보류율(%)만큼 보류기간(일) 동안 지급하지 않으며, 정산일자+보류기간 경과 후 정산금으로 전환됩니다. 보류 해지일이 공휴일이면 익영업일에 전환됩니다. 본사정책 따름 시 본사 수수료 정책(롤링 비율/일수)에 연동됩니다.',
          rows: [
            [{ label: '본사정책 따름', type: 'select', name: 'holdRateFollowHq', options: [{ v: 'Y', t: '본사정책 따름' }, { v: 'N', t: '직접입력' }], col: 2 }],
            [{ label: '보류율(%)', type: 'text', name: 'holdRate', col: 1, placeholder: '5', holdRateOnly: true }, { label: '보류기간(일)', type: 'text', name: 'holdDays', col: 1, placeholder: '120', holdRateOnly: true }]
          ]
        },
        {
          title: '무효·환불 정산 (안내)',
          id: 'voidRefundSettleGuideCard',
          merchantOnly: true,
          cardExtraClass: 'pg-comp-reg-void-refund-panel',
          rows: [
            [{ type: 'customHtml', col: 12, html: merchantVoidRefundGuideHtml }],
            [{ label: '무효 정산(21·40)', type: 'select', name: 'voidSettlementMode', options: VOID_REFUND_SETTLE_MODE_OPTIONS.concat([{ v: 'FOLLOW', t: '총판·본사 따름' }]), col: 3, voidRefundSettlementModeField: true }, { label: '수동무효 정산(22·41)', type: 'select', name: 'manualVoidSettlementMode', options: VOID_REFUND_SETTLE_MODE_OPTIONS.concat([{ v: 'FOLLOW', t: '총판·본사 따름' }]), col: 3, voidRefundSettlementModeField: true }, { label: '환불 정산(30·42)', type: 'select', name: 'refundSettlementMode', options: VOID_REFUND_SETTLE_MODE_OPTIONS.concat([{ v: 'FOLLOW', t: '총판·본사 따름' }]), col: 3, voidRefundSettlementModeField: true }, { label: '강제환불(31)', type: 'select', name: 'forceRefundSettlementMode', options: VOID_REFUND_SETTLE_MODE_OPTIONS.concat([{ v: 'FOLLOW', t: '총판·본사 따름' }]), col: 3, voidRefundSettlementModeField: true }]
          ]
        },
        {
          title: '수수료정책',
          id: 'commissionPolicyCard',
          merchantRegionalMasterCommission: true,
          notice: '본사정책 따름이면 [본사 정책선택]에서 사용합니다. 목록에는 배포(Y)인 템플릿만 나오며, 가맹점 기준통화와 정책 통화코드가 같거나 정책 통화가 비어 있는 항목만 표시됩니다. 본사·총판·가맹점에 동일하게 적용·저장됩니다. 첫 항목(본사 기본 템플릿)은 선택값이 비어 있을 때 본사의 기본(DEFAULT) 수수료 템플릿을 씁니다.',
          rows: [
            [{ label: '본사정책 따름', type: 'select', name: 'commissionFollowHq', options: [{ v: 'Y', t: '본사정책 따름' }, { v: 'N', t: '직접입력' }], col: 2 }, { label: '본사 정책선택', type: 'select', name: 'hqPolicyScope', options: [{ v: '', t: '본사 기본 템플릿 (DEFAULT)' }], col: 2, hqPolicyOnly: true }],
            [{ label: '결제수수료율(%)', type: 'text', name: 'payRate', col: 2, customOnly: true }, { label: '실패수수료(건)', type: 'text', name: 'failFee', col: 2, customOnly: true }, { label: '취소수수료(건)', type: 'text', name: 'cancelRate', col: 2, customOnly: true }],
            [{ label: '무효수수료(건)', type: 'text', name: 'voidFeePerTx', col: 2, customOnly: true, placeholder: '거래 21' }, { label: '수무효수수료(건)', type: 'text', name: 'manualVoidFeePerTx', col: 2, customOnly: true, placeholder: '거래 22' }, { label: '환불수수료(건)', type: 'text', name: 'refundRate', col: 2, customOnly: true }],
            [{ label: '월간이용료(월 1회·고정)', type: 'text', name: 'usageRate', col: 2, customOnly: true, placeholder: '통화코드 단위 금액' }, { label: '비고', type: 'text', name: 'commissionMemo', col: 2, customOnly: true }],
            [{ label: '정산수수료(건)', type: 'text', name: 'feeSettlementPerTx', col: 2, customOnly: true }, { label: '송금수수료', type: 'text', name: 'remittanceTransferFee', col: 2, customOnly: true }, { label: 'USDT 송금수수료(건)', type: 'text', name: 'usdtTransferFeeUsd', col: 2, customOnly: true }, { label: 'USDT수수료율(%)', type: 'text', name: 'feeUsdt', col: 2, customOnly: true }, { label: 'FX수수료율(%)', type: 'text', name: 'feeFx', col: 2, customOnly: true }, { label: '3DS 고정(건)', type: 'text', name: 'fee3dsRate', col: 2, customOnly: true }]
          ]
        },
        {
          title: '차지백 정책',
          id: 'chargebackPolicyCard',
          merchantOnly: true,
          notice: '본사정책 따름이면 위에서 고른 본사 정책 템플릿의 3DS·차지백 설정이 적용됩니다. 직접입력일 때만 아래를 저장할 수 있습니다.',
          rows: [
            [{ label: '차지백수수료(건)', type: 'text', name: 'chargebackFeePerTx', col: 2, customOnly: true }, { label: '차지백 구간정책', type: 'select', name: 'chargebackPolicyId', col: 4, options: [{ v: '', t: '(미사용) 건당 차지백만' }] }]
          ]
        },
        {
          title: '결제 후속조치 (가맹점 관리자)',
          id: 'payFollowMerchantCard',
          merchantOnly: true,
          notice: '관리자 화면의 자동무효·이메일무효·자동환불·강제환불 사용 여부입니다. 전산설정관리(전역) 및 본사권한설정의 조직 단계 상한과 함께 적용됩니다. [기본·종전]은 미설정과 동일(허용으로 해석)입니다.',
          rows: [
            [{ label: '후속조치 사용', type: 'select', name: 'payFollowMerchantUseYn', options: [{ v: '', t: '기본·종전과 동일' }, { v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 }],
            [{ label: '자동무효', type: 'select', name: 'payFollowAutoVoidYn', options: [{ v: '', t: '기본·종전과 동일' }, { v: 'Y', t: '허용' }, { v: 'N', t: '불가' }], col: 2 }, { label: '이메일 무효', type: 'select', name: 'payFollowEmailVoidYn', options: [{ v: '', t: '기본·종전과 동일' }, { v: 'Y', t: '허용' }, { v: 'N', t: '불가' }], col: 2 }, { label: '자동환불', type: 'select', name: 'payFollowAutoRefundYn', options: [{ v: '', t: '기본·종전과 동일' }, { v: 'Y', t: '허용' }, { v: 'N', t: '불가' }], col: 2 }, { label: '강제환불', type: 'select', name: 'payFollowForceRefundYn', options: [{ v: '', t: '기본·종전과 동일' }, { v: 'Y', t: '허용' }, { v: 'N', t: '불가' }], col: 2 }]
          ]
        },
        {
          title: '정산방법',
          id: 'calcMethodCard',
          merchantOnly: true,
          notice: CALC_METHOD_MERCHANT_NOTICE,
          rows: [
            [{ label: '정산주기', type: 'select', name: 'calcCycle', options: CALC_CYCLE_OPTIONS, col: 1 }, { label: '정산구분', type: 'select', name: 'calcProcType', options: CALC_PROC_OPTIONS, col: 1 }, { label: '정산마감시간', type: 'time', name: 'calcCloseTime', col: 1 }, { label: '정산자동개시시간', type: 'time', name: 'calcStartTime', col: 1 }],
            [{ label: '정산주기 적용', type: 'select', name: 'calcCycleTransitionMode', options: [{ v: 'IMMEDIATE', t: '즉시 적용' }, { v: 'NEXT_AFTER_RUN', t: '다음 정산 실행 후(예약)' }], col: 2 }, { label: '변경 비고', type: 'text', name: 'calcCycleChangeRemark', col: 3, placeholder: '선택' }],
            [{ type: 'customHtml', col: 12, html: '<div data-pg="calcCyclePendingBanner" class="alert alert-warning py-2 px-3 small d-none mb-0"></div>' }],
            [{ label: '이체및송금구분', type: 'select', name: 'transferType', options: TRANSFER_REMIT_OPTIONS, col: 1 }, { label: '이체주기(분)', type: 'text', name: 'transferCycleDays', col: 1, placeholder: '예: 5, 60' }, { label: '이체시간', type: 'time', name: 'transferExecTime', col: 1 }],
            [{ label: '정산제외여부', type: 'select', name: 'calcExcludeYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 1 }, { label: '정산제외대상', type: 'select', name: 'calcExcludeTarget', options: [{ v: 'NONE', t: '해당없음' }, { v: 'WEB', t: 'WEB' }, { v: 'OFFLINE', t: '오프라인' }, { v: 'BOTH', t: 'WEB+오프라인' }], col: 1 }, { label: '지급보류', type: 'select', name: 'payHoldYn', options: [{ v: 'N', t: '지급' }, { v: 'Y', t: '보류' }], col: 1 }],
            [{ label: '정산최소금액', type: 'text', name: 'calcMinAmt', col: 1, placeholder: '미만 시 다음 주기' }, { label: '이체및송금최소금액', type: 'text', name: 'autoTransferMin', col: 1, placeholder: '펌뱅킹 최소' }, { label: 'VAT', type: 'select', name: 'feeVatApplyYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 1 }, { label: 'VAT율(%)', type: 'text', name: 'feeVatRatePct', col: 1, placeholder: '수수료 대비 %', feeVatRateOnly: true }]
          ]
        },
        {
          title: '통합정산설정',
          id: 'integratedSettleCard',
          merchantOnly: true,
          notice: '칠페이 통합정산 화면의 「예정(ICOPAY)」 표시에만 쓰입니다. 배포설정 API연동설정과 동일 규칙을 쓰려면 예정모드를 연동기본으로 두세요. 아래 값은 등록 시 입력한 모든 결제대행사 행에 동일하게 적용됩니다.',
          rows: [
            [
              { label: '예정모드', type: 'select', name: 'merchantPgExtSettleMode', col: 2, options: [{ v: '', t: '연동기본' }, { v: 'OFF', t: '가맹:미표시' }, { v: 'T', t: 'T+N' }, { v: 'D', t: 'D+N' }] },
              { label: 'N', type: 'number', name: 'merchantPgExtSettleLag', col: 1, min: 1, max: 10, step: 1, placeholder: 'T/D 시 1~10' },
              { label: 'D시각(일괄)', type: 'time', name: 'merchantPgExtSettleBatchTime', col: 1 }
            ]
          ]
        },
        {
          type: 'pgBindingList',
          title: '결제대행사 설정',
          id: 'pgBindingCard',
          merchantOnly: true,
          omitExtSettleColumns: true,
          notice: '배포설정 > API연동설정에서 사용(Y)으로 등록된 결제대행사가 목록에 표시됩니다. PG를 고르면 API연동설정의 MID·Route 등이 기본값으로 채워지며, 가맹점 전용 값은 수정·저장하면 됩니다. URL·챗봇·API 결제는 운영(체크)를 여러 행에 켤 수 있습니다. 체크된 행은 붉은 배경(파스텔)로 표시됩니다. 노티 전용 PG는 카드브랜드가 ALL로 고정됩니다. 등록 화면은 하단 [저장] 시 한꺼번에 반영됩니다.'
        },
        {
          title: '웹결제 사용 / 대표 기본상품정보 (온라인 URL 결제용)',
          id: 'webPaymentCard',
          merchantOnly: true,
          notice: merchantWebPaymentCardNoticeKo(),
          rows: merchantWebPaymentCardRows('가맹점 저장 후 조회')
        },
        merchantApiUrlPayCheckoutCardSection(),
        merchantJpayCheckoutFieldModeCardSection(),
        merchantApiIntegrationChannelsCardSection(),
        merchantJpayApiSubscriptionCardSection(),
        merchantSplitPayCardSection(),
        {
          title: '챗봇결제 설정',
          id: 'chatbotPaymentCard',
          merchantOnly: true,
          notice: '미사용이면 로그인한 가맹점 관리자에게 챗봇관리의 상품관리 메뉴가 표시되지 않습니다. 「URL 결제 방식」은 챗봇 주문·카탈로그 결제에만 적용되며 공개 URL·API 중계와 별도로 선택할 수 있습니다. 재결제 URL 은 본사 URL 재결제 기능 ON 및 URL재결제 PG 바인딩이 필요합니다. 챗봇결제 URL은 챗봇 쇼핑·주문 진입용입니다.',
          rows: [
            [{ label: '챗봇결제 사용여부', type: 'select', name: 'chatbotPaymentUseYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 2 },
              { label: '챗봇 상품등록 한도(건)', type: 'select', name: 'chatbotProductSlotLimit', col: 2,
              options: [
                { v: '', t: '—' }, { v: '10', t: '10' }, { v: '20', t: '20' }, { v: '50', t: '50' }, { v: '80', t: '80' },
                { v: '100', t: '100' }, { v: '150', t: '150' }, { v: '200', t: '200' }
              ] }],
            [{
              type: 'customHtml', col: 12,
              html: function chatbotHeaderLogoFieldBlock() {
                var phLogo = '업로드 시 자동 반영 · 또는 HTTPS URL 직접 입력';
                var logoHint = 'PNG·JPEG, 원본 최대 40MB. 서버에서 목표 2MB 이하(본사 AI챗봇설정 변경 가능)로 재압축합니다. chatbot_logo_llm_tune_yn=Y 일 때 AI챗봇설정 순위 LLM으로 권장 픽셀을 잡습니다.';
                return '<div class="form-field-block chatbot-header-logo-upload-block w-100">' +
                  '<label class="form-label" data-pg-ui-t="챗봇 상단 로고">' + escUi(L('챗봇 상단 로고')) + '</label>' +
                  '<div class="input-group input-group-sm mb-1">' +
                  '<input type="text" class="form-control form-control-sm" name="chatbotHeaderLogoUrl" id="chatbotHeaderLogoUrl" ' +
                  'placeholder="' + escUi(L(phLogo)) + '" data-pg-ui-placeholder="' + escUi(phLogo) + '">' +
                  '<input type="file" class="d-none" id="chatbotHeaderLogoFile" accept="image/png,image/jpeg,image/jpg">' +
                  '<button type="button" class="btn btn-outline-secondary btn-sm" id="chatbotHeaderLogoBrowse"><span data-pg-ui-t="파일 선택">' + escUi(L('파일 선택')) + '</span></button>' +
                  '<button type="button" class="btn btn-outline-primary btn-sm" id="chatbotHeaderLogoUpload"><span data-pg-ui-t="업로드·최적화">' + escUi(L('업로드·최적화')) + '</span></button>' +
                  '</div>' +
                  '<div class="form-text text-muted small" data-pg-ui-t="' + escUi(logoHint) + '">' +
                  escUi(L(logoHint)) +
                  '</div></div>';
              }
            }],
            [{ label: '챗봇 관리자(로그인ID·중복검사)', type: 'text', name: 'chatbotAdminUsername', col: 12, button: '중복확인', placeholder: '가맹당 1명 · 없는 ID는 저장 시 자동 등록(초기비밀번호: ID+1!) · 공개 챗봇 상품관리 로그인에는 OTP 필요 · 비우면 해제' }],
            [{ label: 'URL 결제 방식', type: 'select', name: 'chatbotUrlPayCheckoutMode', options: [{ v: 'STANDARD', t: '일반 URL 결제' }, { v: 'REPAY', t: '재결제 URL (저장 카드)' }], col: 3 }],
            [{ type: 'customHtml', col: 12, html: function () { return merchantChatbotPaymentUrlRowHtml('가맹점 저장 후 조회'); } }],
            [{ type: 'customHtml', col: 12, html: function () { return merchantChatbotEmbedScriptRowHtml('가맹점 저장 후 조회'); } }],
            [{ type: 'customHtml', col: 12, html: function () { return merchantChatbotQrRowHtml(); } }],
            [{ label: '가맹 활성 카탈로그 유형(CSV)', type: 'text', name: 'chatbotCatalogListingEnabled', col: 12,
              placeholder: 'SALE,RESERVATION_TIME 예: 시간예약만' }]
          ]
        },
        {
          title: '챗봇 카탈로그(산하 허용·이미지)',
          id: 'chatbotCatalogPolicyCard',
          headOfficeTierOnly: true,
          notice: '총본사·본사·총판만 설정합니다. 비우면 해당 단계에서 제한 없음(상위·시스템 기본). 산하 가맹 실효값은 체인 최소(교집합·이미지 장수)입니다.',
          rows: [
            [{ label: '산하 허용 상품유형(CSV)', type: 'text', name: 'chatbotCatalogListingGrant', col: 8,
              placeholder: 'SALE,RESERVATION_TIME,RESERVATION_PLACE' }],
            [{ label: '상품 이미지 장수 상한(1~4)', type: 'select', name: 'chatbotMaxProductImagesGrant', col: 4,
              options: [
                { v: '', t: '— 미지정' }, { v: '1', t: '1' }, { v: '2', t: '2' }, { v: '3', t: '3' }, { v: '4', t: '4' }
              ] }]
          ]
        },
        {
          type: 'branding',
          title: '브랜딩 설정',
          id: 'brandingCard',
          regionalOrMasterDistOnly: true,
          notice: '본사·총판만 설정 가능. 메인이미지=로그인 화면 왼쪽 배경, 로고=로그인창 상단·사이드바 상단.'
        },
        { title: '기타', id: 'regionalMiscCard', headOfficeTierOnly: true, notice: '총본사/본사/총판 공통 설정입니다. COPYRIGHT에 입력한 문구는 화면 하단에 표시됩니다.', rows: [[{ label: 'COPYRIGHT', type: 'textarea', name: 'copyright', col: 6, placeholder: 'Copyright © 2025 ICOPAY Service by Ontheline Co., Ltd.' }, { label: '비고', type: 'textarea', name: 'remark', col: 6 }]] },
        { title: '기타', id: 'nonRegionalMiscCard', distributorMerchantOnlyNoRegional: true, rows: [[{ label: '비고', type: 'textarea', name: 'remark', col: 6 }]] },
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
          title: 'JPAY 수신통보 URL',
          id: 'jpayNotifyUrlCard',
          merchantOnly: true,
          notice: 'J-Pay pay_index 전문의 pay_notifyurl·pay_callbackurl 에 사용됩니다. 노티미들웨어 가맹 수신 URL을 등록하세요. 비우면 ICOPAY ingress(cbJpay/rsJpay) 기본값을 사용합니다.',
          rows: [
            [{ label: 'Notify (pay_notifyurl) / Callback URL (NOTI MW)', type: 'text', name: 'jpayNotifyUrl', col: 5, placeholder: 'https://' }, { label: 'Callback (pay_callbackurl) / Result URL (NOTI MW)', type: 'text', name: 'jpayCallbackUrl', col: 5, placeholder: 'https://' }]
          ]
        },
        {
          title: 'URL·챗봇 결제 승인 알림',
          id: 'urlPaySuccessAlertCard',
          merchantOnly: true,
          notice: '인라인 DirectCredit(URL·챗봇) 승인 시 PG중계 JSON 전송과 함께 LINE Notify·대표 이메일(전산 SMTP) 알림을 보낼 수 있습니다. 토큰은 비우면 기존 유지, 삭제는 __CLEAR__.',
          rows: [
            [{ label: '승인 알림메일', type: 'select', name: 'urlPayAlertEmailYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용 (대표 이메일)' }], col: 2 }],
            [{ label: 'LINE Notify 토큰', type: 'password', name: 'urlPayLineNotifyToken', col: 6, placeholder: '변경 시만 입력 · 삭제: __CLEAR__' }]
          ]
        },
        { title: '첨부파일', rows: [[{ type: 'file', name: 'attach', col: 12, blockExtraClass: 'attach-field-block' }]] }
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
            [{ label: '업체코드', type: 'text', name: 'compId', col: 2, readonly: true }, { label: '상위 본사', type: 'text', name: 'parentComp', col: 2, button: '검색', placeholder: '상위 코드' }, { label: '업체구분*', type: 'select', name: 'compDiv', options: [{ v: '', t: '선택' }, { v: 'HEADQUARTERS', t: '총본사' }, { v: 'REGIONAL', t: '본사' }, { v: 'MASTER_DIST', t: '총판' }, { v: 'BRANCH', t: '지사' }, { v: 'AGENCY', t: '대리점' }, { v: 'SALES_OFFICE', t: '영업점' }, { v: 'MERCHANT', t: '가맹점' }], col: 1 }, { label: '업체명*', type: 'text', name: 'compNm', col: 2 }, { label: '사업자번호*', type: 'regNoWithType', name: 'regNo', col: 2 }, { label: '업태', type: 'text', name: 'bizType', col: 1 }, { label: '종목', type: 'text', name: 'industry', col: 1 }],
            [{ label: '대표자명*', type: 'text', name: 'ceoNm', col: 2 }, { label: '휴대폰*', type: 'text', name: 'ceoMobile', col: 2 }, { label: '업체전화*', type: 'text', name: 'compTel', col: 2 }, { label: '팩스', type: 'text', name: 'fax', col: 2 }, { label: '이메일', type: 'text', name: 'email', col: 2 }, { label: '비고', type: 'text', name: 'remark', col: 2 }],
            [{ type: 'countryAddressRow', zipLabel: '우편번호*', addrLabel: '주소*', addrDetailLabel: '상세주소', addrEtcLabel: '기타' }],
            [{ label: '사용여부*', type: 'select', name: 'useYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }, { v: 'S', t: '영구정지' }], col: 1 }, { label: '태블릿 UI 기능', type: 'select', name: 'tabletFeatureUseYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 }, { label: '로그인ID*', type: 'text', name: 'loginId', col: 2, button: 'ID변경' }, { label: '비밀번호', type: 'passwordReset', name: 'pwdReset', col: 2 }]
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
            [{ label: '코인 지갑 주소', type: 'text', name: 'walletAddress', col: 4, placeholder: '코인 수취 지갑 주소' }, { label: '네트워크', type: 'text', name: 'networkName', col: 2, placeholder: '네트워크 이름' }, { label: '크립토 이체 수수료(USD)', type: 'text', name: 'cryptoTransferFee', col: 2, placeholder: 'USD' }, { label: '이체수수료', type: 'text', name: 'transferFee', col: 2, placeholder: '기준화폐' }],
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
            [{ label: 'PG수수료(일반)*', type: 'text', name: 'pgFeeGeneral', col: 2, placeholder: '%' }, { label: '이체수수료', type: 'text', name: 'transferFee', col: 2, placeholder: '기준화폐' }, { label: '차액정산 월횟수', type: 'text', name: 'settleDiffMonthCnt', col: 2 }, { label: '정산보고서 은행*', type: 'select', name: 'settleReportBankCd', options: [{ v: '', t: '선택하세요' }, { v: '04', t: '국민' }, { v: '20', t: '우리' }, { v: '81', t: 'KEB하나' }, { v: '88', t: '신한' }, { v: '11', t: 'NH농협' }], col: 2 }],
            [{ label: 'PG수수료(삼성페이)', type: 'text', name: 'pgFeeSamsung', col: 2 }, { label: 'SMS수수료', type: 'text', name: 'smsFee', col: 2 }, { label: '세금계산서 이메일', type: 'text', name: 'taxInvoiceEmail', col: 2 }, { label: '계좌번호', type: 'text', name: 'settleAccountNo', col: 2 }],
            [{ label: '직결수수료', type: 'text', name: 'directFee', col: 2 }, { label: '솔루션수수료', type: 'text', name: 'solutionFee', col: 2, placeholder: '0.1%' }, { label: '예금주명*', type: 'text', name: 'settleAccountHolder', col: 2 }]
          ]
        },
        {
          title: '출금 제한 시간 설정',
          id: 'regionalWithdrawLimitCard',
          regionalOnly: true,
          notice: '본사 기본 출금 제한 정책입니다. 매일: 시작~종료 매일 적용. 공휴일: 당일 00:00~23:59 전면 제한, 그 외 영업일은 시작~종료. 공휴일 전날 17시/18시 이후: 전영업일 해당 시각~공휴일 23:59(시작이 17·18시보다 이르면 시작시간부터), 그 외 날은 시작~종료. 실제 출금 시 본사 영업일·공휴일 데이터와 함께 판단합니다.',
          rows: [
            [{ label: '출금제한 유형', type: 'select', name: 'withdrawRestrictType', options: WITHDRAW_POLICY_OPTIONS, col: 2 }, { label: '출금제한시작시간*', type: 'time', name: 'withdrawRestrictStartTime', col: 1 }, { label: '출금제한종료시간*', type: 'time', name: 'withdrawRestrictEndTime', col: 1 }]
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
          notice: '총판일 때만 입력합니다. 총판은 1가지 화폐만 지정할 수 있습니다. 필수 노티(URL 1·2)는 본사설정 > 노티구성설정에서 이 총판에 노티 대상을 연결하면 자동 반영되며 화면에서 수정할 수 없습니다. 보조(URL 3·4)는 [보조 쌍 선택] 또는 드롭다운으로 추가할 수 있습니다. 연결된 본사 수신 URL로 유입되는 노티가 이 총판 트리로 분기됩니다.',
          rows: [
            [{ label: '기준 화폐*', type: 'select', name: 'baseCurrency', options: [{ v: '', t: '선택' }, { v: 'KRW', t: 'KRW (원)' }, { v: 'USD', t: 'USD (달러)' }, { v: 'JPY', t: 'JPY (엔)' }, { v: 'THB', t: 'THB (바트)' }, { v: 'EUR', t: 'EUR (유로)' }], col: 2 }, { label: '사이트개요', type: 'text', name: 'siteSummary', col: 2, placeholder: '사이트개요' }, { label: '취급물품', type: 'text', name: 'product', col: 2 }, { label: '대표사이트', type: 'text', name: 'homepage', col: 2, placeholder: 'https://' }],
            [{ label: '정산담당자명', type: 'text', name: 'settleName', col: 2 }, { label: '정산담당자연락처', type: 'text', name: 'settleTelNo', col: 2, placeholder: '010-0000-0000' }, { label: '정산형태', type: 'select', name: 'settleType', options: [{ v: '', t: '선택' }, { v: 'M', t: '가맹점별' }, { v: 'G', t: '총판' }], col: 1 }, { label: '요율(%)', type: 'text', name: 'commissionRate', col: 1, placeholder: '요율' }, { label: '사용한도', type: 'text', name: 'limitAmt', col: 2, placeholder: '사용한도' }],
            [{ type: 'notifyPairButton', col: 2, pairLabel: '필수 노티', buttonText: '노티 쌍 선택', callbackField: 'notifyUrl1', resultField: 'notifyUrl2', hint: '본사 노티구성에서 연결 시 자동 입력됩니다.', titleHint: '본사설정 > 노티구성설정에서 총판에 노티 대상을 연결하세요.', readonly: true }, { label: '노티 CALLBACK (URL 1)*', type: 'select', name: 'notifyUrl1', col: 5, loadNotifyTargets: true, button: '노티선택', readonly: true }, { label: '노티 RESULT (URL 2)*', type: 'select', name: 'notifyUrl2', col: 5, loadNotifyTargets: true, button: '노티선택', readonly: true }],
            [{ type: 'notifyPairButton', col: 2, pairLabel: '보조 노티', buttonText: '보조 쌍 선택', callbackField: 'notifyUrl3', resultField: 'notifyUrl4', hint: 'URL 3·4를 같은 쌍으로 채웁니다.', titleHint: '보조 노티 URL 3·4를 한 번에 설정합니다.' }, { label: '노티 URL 3(보조)', type: 'select', name: 'notifyUrl3', col: 5, loadNotifyTargets: true, button: '노티선택' }, { label: '노티 URL 4(보조)', type: 'select', name: 'notifyUrl4', col: 5, loadNotifyTargets: true, button: '노티선택' }]
          ]
        },
        {
          title: '가맹점 상세 정보',
          id: 'merchantExtraCard',
          merchantOnly: true,
          notice: '가맹점일 때만 입력합니다. 기준 화폐를 비우고 저장하면 상위 총판·본사 프로필의 기준통화를 자동으로 상속합니다(결제내역 VIEW의 본사/총판/가맹 기준통화 열에 반영).',
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
            [{ label: '코인 지갑 주소', type: 'text', name: 'walletAddress', col: 4, placeholder: '코인 수취 지갑 주소' }, { label: '네트워크', type: 'text', name: 'networkName', col: 2, placeholder: '네트워크 이름' }]
          ]
        },
        {
          title: '출금 제한 설정',
          id: 'withdrawLimitCard',
          merchantOnly: true,
          notice: '가맹점 출금 제한 유형입니다. 매일·공휴일·공휴일 전날(17·18시) 규칙은 본사 영업일·공휴일 캘린더와 함께 출금 처리 시 해석합니다. 평일 구간은 시작·종료 시각으로 좁힙니다.',
          rows: [
            [{ label: '출금제한 유형', type: 'select', name: 'withdrawRestrictType', options: WITHDRAW_POLICY_OPTIONS, col: 2 }, { label: '시작시간', type: 'time', name: 'withdrawStartTime', col: 1 }, { label: '종료시간', type: 'time', name: 'withdrawEndTime', col: 1 }]
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
          notice: '결제 정산금 중 보류율(%)만큼 보류기간(일) 동안 지급하지 않으며, 정산일자+보류기간 경과 후 정산금으로 전환됩니다. 보류 해지일이 공휴일이면 익영업일에 전환됩니다. 본사정책 따름 시 본사 수수료 정책(롤링 비율/일수)에 연동됩니다.',
          rows: [
            [{ label: '본사정책 따름', type: 'select', name: 'holdRateFollowHq', options: [{ v: 'Y', t: '본사정책 따름' }, { v: 'N', t: '직접입력' }], col: 2 }],
            [{ label: '보류율(%)', type: 'text', name: 'holdRate', col: 1, placeholder: '5', holdRateOnly: true }, { label: '보류기간(일)', type: 'text', name: 'holdDays', col: 1, placeholder: '120', holdRateOnly: true }]
          ]
        },
        {
          title: '무효·환불 정산 (안내)',
          id: 'voidRefundSettleGuideCard',
          merchantOnly: true,
          cardExtraClass: 'pg-comp-reg-void-refund-panel',
          rows: [
            [{ type: 'customHtml', col: 12, html: merchantVoidRefundGuideHtml }],
            [{ label: '무효 정산(21·40)', type: 'select', name: 'voidSettlementMode', options: VOID_REFUND_SETTLE_MODE_OPTIONS.concat([{ v: 'FOLLOW', t: '총판·본사 따름' }]), col: 3, voidRefundSettlementModeField: true }, { label: '수동무효 정산(22·41)', type: 'select', name: 'manualVoidSettlementMode', options: VOID_REFUND_SETTLE_MODE_OPTIONS.concat([{ v: 'FOLLOW', t: '총판·본사 따름' }]), col: 3, voidRefundSettlementModeField: true }, { label: '환불 정산(30·42)', type: 'select', name: 'refundSettlementMode', options: VOID_REFUND_SETTLE_MODE_OPTIONS.concat([{ v: 'FOLLOW', t: '총판·본사 따름' }]), col: 3, voidRefundSettlementModeField: true }, { label: '강제환불(31)', type: 'select', name: 'forceRefundSettlementMode', options: VOID_REFUND_SETTLE_MODE_OPTIONS.concat([{ v: 'FOLLOW', t: '총판·본사 따름' }]), col: 3, voidRefundSettlementModeField: true }]
          ]
        },
        {
          title: '수수료정책',
          id: 'commissionPolicyCard',
          merchantRegionalMasterCommission: true,
          notice: '본사정책 따름이면 [본사 정책선택]에서 사용합니다. 목록에는 배포(Y)인 템플릿만 나오며, 가맹점 기준통화와 정책 통화코드가 같거나 정책 통화가 비어 있는 항목만 표시됩니다. 본사·총판·가맹점에 동일하게 적용·저장됩니다. 첫 항목(본사 기본 템플릿)은 선택값이 비어 있을 때 본사의 기본(DEFAULT) 수수료 템플릿을 씁니다.',
          rows: [
            [{ label: '본사정책 따름', type: 'select', name: 'commissionFollowHq', options: [{ v: 'Y', t: '본사정책 따름' }, { v: 'N', t: '직접입력' }], col: 2 }, { label: '본사 정책선택', type: 'select', name: 'hqPolicyScope', options: [{ v: '', t: '본사 기본 템플릿 (DEFAULT)' }], col: 2, hqPolicyOnly: true }],
            [{ label: '결제수수료율(%)', type: 'text', name: 'payRate', col: 2, customOnly: true }, { label: '실패수수료(건)', type: 'text', name: 'failFee', col: 2, customOnly: true }, { label: '취소수수료(건)', type: 'text', name: 'cancelRate', col: 2, customOnly: true }],
            [{ label: '무효수수료(건)', type: 'text', name: 'voidFeePerTx', col: 2, customOnly: true, placeholder: '거래 21' }, { label: '수무효수수료(건)', type: 'text', name: 'manualVoidFeePerTx', col: 2, customOnly: true, placeholder: '거래 22' }, { label: '환불수수료(건)', type: 'text', name: 'refundRate', col: 2, customOnly: true }],
            [{ label: '월간이용료(월 1회·고정)', type: 'text', name: 'usageRate', col: 2, customOnly: true, placeholder: '통화코드 단위 금액' }, { label: '비고', type: 'text', name: 'commissionMemo', col: 2, customOnly: true }],
            [{ label: '정산수수료(건)', type: 'text', name: 'feeSettlementPerTx', col: 2, customOnly: true }, { label: '송금수수료', type: 'text', name: 'remittanceTransferFee', col: 2, customOnly: true }, { label: 'USDT 송금수수료(건)', type: 'text', name: 'usdtTransferFeeUsd', col: 2, customOnly: true }, { label: 'USDT수수료율(%)', type: 'text', name: 'feeUsdt', col: 2, customOnly: true }, { label: 'FX수수료율(%)', type: 'text', name: 'feeFx', col: 2, customOnly: true }, { label: '3DS 고정(건)', type: 'text', name: 'fee3dsRate', col: 2, customOnly: true }]
          ]
        },
        {
          title: '차지백 정책',
          id: 'chargebackPolicyCard',
          merchantOnly: true,
          notice: '본사정책 따름이면 위에서 고른 본사 정책 템플릿의 3DS·차지백 설정이 적용됩니다. 직접입력일 때만 아래를 저장할 수 있습니다.',
          rows: [
            [{ label: '차지백수수료(건)', type: 'text', name: 'chargebackFeePerTx', col: 2, customOnly: true }, { label: '차지백 구간정책', type: 'select', name: 'chargebackPolicyId', col: 4, options: [{ v: '', t: '(미사용) 건당 차지백만' }] }]
          ]
        },
        {
          title: '결제 후속조치 (가맹점 관리자)',
          id: 'payFollowMerchantCard',
          merchantOnly: true,
          notice: '관리자 화면의 자동무효·이메일무효·자동환불·강제환불 사용 여부입니다. 전산설정관리(전역) 및 본사권한설정의 조직 단계 상한과 함께 적용됩니다. [기본·종전]은 미설정과 동일(허용으로 해석)입니다.',
          rows: [
            [{ label: '후속조치 사용', type: 'select', name: 'payFollowMerchantUseYn', options: [{ v: '', t: '기본·종전과 동일' }, { v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 }],
            [{ label: '자동무효', type: 'select', name: 'payFollowAutoVoidYn', options: [{ v: '', t: '기본·종전과 동일' }, { v: 'Y', t: '허용' }, { v: 'N', t: '불가' }], col: 2 }, { label: '이메일 무효', type: 'select', name: 'payFollowEmailVoidYn', options: [{ v: '', t: '기본·종전과 동일' }, { v: 'Y', t: '허용' }, { v: 'N', t: '불가' }], col: 2 }, { label: '자동환불', type: 'select', name: 'payFollowAutoRefundYn', options: [{ v: '', t: '기본·종전과 동일' }, { v: 'Y', t: '허용' }, { v: 'N', t: '불가' }], col: 2 }, { label: '강제환불', type: 'select', name: 'payFollowForceRefundYn', options: [{ v: '', t: '기본·종전과 동일' }, { v: 'Y', t: '허용' }, { v: 'N', t: '불가' }], col: 2 }]
          ]
        },
        {
          title: '정산방법',
          id: 'calcMethodCard',
          merchantOnly: true,
          notice: CALC_METHOD_MERCHANT_NOTICE,
          rows: [
            [{ label: '정산주기', type: 'select', name: 'calcCycle', options: CALC_CYCLE_OPTIONS, col: 1 }, { label: '정산구분', type: 'select', name: 'calcProcType', options: CALC_PROC_OPTIONS, col: 1 }, { label: '정산마감시간', type: 'time', name: 'calcCloseTime', col: 1 }, { label: '정산자동개시시간', type: 'time', name: 'calcStartTime', col: 1 }],
            [{ label: '정산주기 적용', type: 'select', name: 'calcCycleTransitionMode', options: [{ v: 'IMMEDIATE', t: '즉시 적용' }, { v: 'NEXT_AFTER_RUN', t: '다음 정산 실행 후(예약)' }], col: 2 }, { label: '변경 비고', type: 'text', name: 'calcCycleChangeRemark', col: 3, placeholder: '선택' }],
            [{ type: 'customHtml', col: 12, html: '<div data-pg="calcCyclePendingBanner" class="alert alert-warning py-2 px-3 small d-none mb-0"></div>' }],
            [{ label: '이체및송금구분', type: 'select', name: 'transferType', options: TRANSFER_REMIT_OPTIONS, col: 1 }, { label: '이체주기(분)', type: 'text', name: 'transferCycleDays', col: 1, placeholder: '예: 5, 60' }, { label: '이체시간', type: 'time', name: 'transferExecTime', col: 1 }],
            [{ label: '정산제외여부', type: 'select', name: 'calcExcludeYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 1 }, { label: '정산제외대상', type: 'select', name: 'calcExcludeTarget', options: [{ v: 'NONE', t: '해당없음' }, { v: 'WEB', t: 'WEB' }, { v: 'OFFLINE', t: '오프라인' }, { v: 'BOTH', t: 'WEB+오프라인' }], col: 1 }, { label: '지급보류', type: 'select', name: 'payHoldYn', options: [{ v: 'N', t: '지급' }, { v: 'Y', t: '보류' }], col: 1 }],
            [{ label: '정산최소금액', type: 'text', name: 'calcMinAmt', col: 1, placeholder: '미만 시 다음 주기' }, { label: '이체및송금최소금액', type: 'text', name: 'autoTransferMin', col: 1, placeholder: '펌뱅킹 최소' }, { label: 'VAT', type: 'select', name: 'feeVatApplyYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 1 }, { label: 'VAT율(%)', type: 'text', name: 'feeVatRatePct', col: 1, placeholder: '수수료 대비 %', feeVatRateOnly: true }]
          ]
        },
        {
          title: '통합정산설정',
          id: 'integratedSettleCard',
          merchantOnly: true,
          notice: '칠페이 통합정산 화면의 「예정(ICOPAY)」 표시에만 쓰입니다. 배포설정 API연동설정과 동일 규칙을 쓰려면 예정모드를 연동기본으로 두세요. 아래 값은 [저장] 시 등록된 모든 결제대행사 행에 동일하게 적용됩니다.',
          rows: [
            [
              { label: '예정모드', type: 'select', name: 'merchantPgExtSettleMode', col: 2, options: [{ v: '', t: '연동기본' }, { v: 'OFF', t: '가맹:미표시' }, { v: 'T', t: 'T+N' }, { v: 'D', t: 'D+N' }] },
              { label: 'N', type: 'number', name: 'merchantPgExtSettleLag', col: 1, min: 1, max: 10, step: 1, placeholder: 'T/D 시 1~10' },
              { label: 'D시각(일괄)', type: 'time', name: 'merchantPgExtSettleBatchTime', col: 1 }
            ]
          ]
        },
        {
          type: 'pgBindingList',
          title: '결제대행사 설정',
          id: 'pgBindingCard',
          merchantOnly: true,
          omitExtSettleColumns: true,
          notice: '배포설정 > API연동설정(사용 Y) 전체가 목록에 나오며, PG 선택 시 본사에 등록한 MID·Route가 기본 입력됩니다. 예정모드·N·D시각은 위 「통합정산설정」에서 일괄 지정합니다. API KEY·IV는 비우면 본사 연동 자격을 따를 수 있습니다(ChillPay 등). URL·챗봇·API 결제는 운영(체크)를 여러 행에 켤 수 있습니다. 체크된 행은 붉은 배경(파스텔)로 표시됩니다. 노티 전용 PG는 카드브랜드가 ALL로 고정됩니다. [추가]로 행을 열고, 업체정보(가맹점)에서는 [저장][삭제][수정]마다 확인창이 두 번 뜹니다.'
        },
        {
          title: '웹결제 사용 / 대표 기본상품정보 (온라인 URL 결제용)',
          id: 'webPaymentCard',
          merchantOnly: true,
          notice: merchantWebPaymentCardNoticeKo(),
          rows: merchantWebPaymentCardRows('가맹점 선택 후 조회')
        },
        merchantApiUrlPayCheckoutCardSection(),
        merchantJpayCheckoutFieldModeCardSection(),
        merchantApiIntegrationChannelsCardSection(),
        merchantJpayApiSubscriptionCardSection(),
        merchantSplitPayCardSection(),
        {
          title: '챗봇결제 설정',
          id: 'chatbotPaymentCard',
          merchantOnly: true,
          notice: '미사용이면 로그인한 가맹점 관리자에게 챗봇관리의 상품관리 메뉴가 표시되지 않습니다. 「URL 결제 방식」은 챗봇 주문·카탈로그 결제에만 적용되며 공개 URL·API 중계와 별도로 선택할 수 있습니다. 재결제 URL 은 본사 URL 재결제 기능 ON 및 URL재결제 PG 바인딩이 필요합니다. 챗봇결제 URL은 챗봇 쇼핑·주문 진입용입니다.',
          rows: [
            [{ label: '챗봇결제 사용여부', type: 'select', name: 'chatbotPaymentUseYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용' }], col: 2 },
              { label: '챗봇 상품등록 한도(건)', type: 'select', name: 'chatbotProductSlotLimit', col: 2,
              options: [
                { v: '', t: '—' }, { v: '10', t: '10' }, { v: '20', t: '20' }, { v: '50', t: '50' }, { v: '80', t: '80' },
                { v: '100', t: '100' }, { v: '150', t: '150' }, { v: '200', t: '200' }
              ] }],
            [{
              type: 'customHtml', col: 12,
              html: function chatbotHeaderLogoFieldBlock() {
                var phLogo = '업로드 시 자동 반영 · 또는 HTTPS URL 직접 입력';
                var logoHint = 'PNG·JPEG, 원본 최대 40MB. 서버에서 목표 2MB 이하(본사 AI챗봇설정 변경 가능)로 재압축합니다. chatbot_logo_llm_tune_yn=Y 일 때 AI챗봇설정 순위 LLM으로 권장 픽셀을 잡습니다.';
                return '<div class="form-field-block chatbot-header-logo-upload-block w-100">' +
                  '<label class="form-label" data-pg-ui-t="챗봇 상단 로고">' + escUi(L('챗봇 상단 로고')) + '</label>' +
                  '<div class="input-group input-group-sm mb-1">' +
                  '<input type="text" class="form-control form-control-sm" name="chatbotHeaderLogoUrl" id="chatbotHeaderLogoUrl" ' +
                  'placeholder="' + escUi(L(phLogo)) + '" data-pg-ui-placeholder="' + escUi(phLogo) + '">' +
                  '<input type="file" class="d-none" id="chatbotHeaderLogoFile" accept="image/png,image/jpeg,image/jpg">' +
                  '<button type="button" class="btn btn-outline-secondary btn-sm" id="chatbotHeaderLogoBrowse"><span data-pg-ui-t="파일 선택">' + escUi(L('파일 선택')) + '</span></button>' +
                  '<button type="button" class="btn btn-outline-primary btn-sm" id="chatbotHeaderLogoUpload"><span data-pg-ui-t="업로드·최적화">' + escUi(L('업로드·최적화')) + '</span></button>' +
                  '</div>' +
                  '<div class="form-text text-muted small" data-pg-ui-t="' + escUi(logoHint) + '">' +
                  escUi(L(logoHint)) +
                  '</div></div>';
              }
            }],
            [{ label: '챗봇 관리자(로그인ID·중복검사)', type: 'text', name: 'chatbotAdminUsername', col: 12, button: '중복확인', placeholder: '가맹당 1명 · 없는 ID는 저장 시 자동 등록(초기비밀번호: ID+1!) · 공개 챗봇 상품관리 로그인에는 OTP 필요 · 비우면 해제' }],
            [{ label: 'URL 결제 방식', type: 'select', name: 'chatbotUrlPayCheckoutMode', options: [{ v: 'STANDARD', t: '일반 URL 결제' }, { v: 'REPAY', t: '재결제 URL (저장 카드)' }], col: 3 }],
            [{ type: 'customHtml', col: 12, html: function () { return merchantChatbotPaymentUrlRowHtml('가맹점 선택 후 조회'); } }],
            [{ type: 'customHtml', col: 12, html: function () { return merchantChatbotEmbedScriptRowHtml('가맹점 선택 후 조회'); } }],
            [{ type: 'customHtml', col: 12, html: function () { return merchantChatbotQrRowHtml(); } }],
            [{ label: '가맹 활성 카탈로그 유형(CSV)', type: 'text', name: 'chatbotCatalogListingEnabled', col: 12,
              placeholder: 'SALE,RESERVATION_TIME 예: 시간예약만' }]
          ]
        },
        {
          title: '챗봇 카탈로그(산하 허용·이미지)',
          id: 'chatbotCatalogPolicyCard',
          headOfficeTierOnly: true,
          notice: '총본사·본사·총판만 설정합니다. 비우면 해당 단계에서 제한 없음(상위·시스템 기본). 산하 가맹 실효값은 체인 최소(교집합·이미지 장수)입니다.',
          rows: [
            [{ label: '산하 허용 상품유형(CSV)', type: 'text', name: 'chatbotCatalogListingGrant', col: 8,
              placeholder: 'SALE,RESERVATION_TIME,RESERVATION_PLACE' }],
            [{ label: '상품 이미지 장수 상한(1~4)', type: 'select', name: 'chatbotMaxProductImagesGrant', col: 4,
              options: [
                { v: '', t: '— 미지정' }, { v: '1', t: '1' }, { v: '2', t: '2' }, { v: '3', t: '3' }, { v: '4', t: '4' }
              ] }]
          ]
        },
        {
          type: 'branding',
          title: '브랜딩 설정',
          id: 'brandingCard',
          regionalOrMasterDistOnly: true,
          notice: '본사·총판만 설정 가능. 메인이미지=로그인 화면 왼쪽 배경, 로고=로그인창 상단·사이드바 상단.'
        },
        { title: '기타', id: 'regionalMiscCard', headOfficeTierOnly: true, notice: '총본사/본사/총판 공통 설정입니다. COPYRIGHT에 입력한 문구는 화면 하단에 표시됩니다.', rows: [[{ label: 'COPYRIGHT', type: 'textarea', name: 'copyright', col: 6, placeholder: 'Copyright © 2025 ICOPAY Service by Ontheline Co., Ltd.' }]] },
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
          title: 'JPAY 수신통보 URL',
          id: 'jpayNotifyUrlCard',
          merchantOnly: true,
          notice: 'J-Pay pay_index 전문의 pay_notifyurl·pay_callbackurl 에 사용됩니다. 노티미들웨어 가맹 수신 URL을 등록하세요. 비우면 ICOPAY ingress(cbJpay/rsJpay) 기본값을 사용합니다.',
          rows: [
            [{ label: 'Notify (pay_notifyurl) / Callback URL (NOTI MW)', type: 'text', name: 'jpayNotifyUrl', col: 5, placeholder: 'https://' }, { label: 'Callback (pay_callbackurl) / Result URL (NOTI MW)', type: 'text', name: 'jpayCallbackUrl', col: 5, placeholder: 'https://' }]
          ]
        },
        {
          title: 'URL·챗봇 결제 승인 알림',
          id: 'urlPaySuccessAlertCard',
          merchantOnly: true,
          notice: '인라인 DirectCredit(URL·챗봇) 승인 시 PG중계 JSON 전송과 함께 LINE Notify·대표 이메일(전산 SMTP) 알림을 보낼 수 있습니다. 토큰은 비우면 기존 유지, 삭제는 __CLEAR__.',
          rows: [
            [{ label: '승인 알림메일', type: 'select', name: 'urlPayAlertEmailYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용 (대표 이메일)' }], col: 2 }],
            [{ label: 'LINE Notify 토큰', type: 'password', name: 'urlPayLineNotifyToken', col: 6, placeholder: '변경 시만 입력 · 삭제: __CLEAR__' }]
          ]
        },
        { title: '첨부파일', rows: [[{ type: 'file', name: 'attach', col: 12, blockExtraClass: 'attach-field-block' }]] }
      ],
      buttons: [{ id: 'compDetailListBtn', label: '목록', cls: 'btn-secondary' }, { id: 'compDetailSaveBtn', label: '저장', cls: 'btn-primary' }]
    },
    '/commission/commisionList': {
      /** 페이지네이션 행 오른쪽에 [저장] (상단 저장과 동일 동작) */
      paginationTrailingSaveButton: true,
      /** VIEW SETTING: 본사설정 조직항목설정과 동일 열 집합·키. 고정은 No·가맹점·업체코드만(체크·처리 열은 타입으로 제외). */
      tableColumnGuide: true,
      columnGuideFixedKeys: ['rowNo', 'compNm', 'compId'],
      searchRows: [
        [
          { label: '업체선택(조직)', type: 'select', name: 'searchCompDiv', i18nLblKey: 'searchCompOrgPick', options: [{ v: '', t: '전체' }, { v: 'REGIONAL', t: '본사' }, { v: 'MASTER_DIST', t: '총판' }, { v: 'BRANCH', t: '지사' }, { v: 'AGENCY', t: '대리점' }, { v: 'SALES_OFFICE', t: '영업점' }, { v: 'MERCHANT', t: '가맹점' }] },
          { label: '업체사용여부', type: 'select', name: 'searchUseYn', i18nLblKey: 'searchCompListUseYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }, { v: 'S', t: '영구정지' }] },
          { label: '통화', type: 'select', name: 'searchPolicyCur', i18nLblKey: 'searchCurType', options: [
            { v: '', t: '전체' },
            { v: 'JPY', t: 'JPY' },
            { v: 'USD', t: 'USD' },
            { v: 'THB', t: 'THB' },
            { v: 'CNY', t: 'CNY' },
            { v: 'KRW', t: 'KRW' }
          ], size: 8 },
          { label: '업체코드', type: 'text', name: 'searchCompId' },
          { label: '업체명', type: 'text', name: 'searchCompNm' },
          { type: 'searchBtn' }
        ]
      ],
      tableScrollable: true,
      /** 가로 스크롤 허용·한 줄 표시 (site.css .commission-list-table-wrap) */
      tableResponsiveExtraClass: 'commission-list-table-wrap',
      noticeList: [
        'VIEW SETTING 열 목록은 본사설정 → 조직항목설정(화면: 수수료관리)에서 허용한 키와 동일합니다. 신규 열 「통화(policyCur)」는 적용 수수료 정책의 통화코드(ISO 숫자·알파)를 THB·JPY 등 알파로 표시합니다. 조직항목설정을 바꾼 뒤 새로고침·재조회하면 체크 목록·노출 제한이 반영됩니다.',
        '적용시작일을 비우면 저장 시점(서버 시각) 기준으로 적용됩니다.',
        '동일 가맹점에 미래 적용일이 중복되지 않도록 한 번에 한 건만 등록하는 것을 권장합니다.',
        '상위 조직 수수료 정책이 바뀌면 이후 신규 가맹점 등록 시 하위 배분 설정에 반영될 수 있습니다.'
      ],
      summary: ['건수'],
      buttons: [{ id: 'commissionSettingBtn', label: '수수료설정', cls: 'btn-info' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }, { id: 'commissionInlineTopSaveBtn', label: '저장', cls: 'btn-primary' }],
      /** 2단 헤더 + site.css .commission-split-grid. 열 리사이즈(colgroup) 비활성화 */
      tableExtraClass: 'commission-split-grid table-no-col-resize',
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
        { key: 'policyCur', label: '통화', columnGuideLabel: '적용 정책 통화(THB·JPY 등)' },
        { key: 'hqNm', label: '업체명', columnGuideLabel: '총본사 · 업체명' }, { key: 'hqRate', label: '요율%' }, { key: 'hqPerTxFee', label: '건당료' },
        { key: 'regionalNm', label: '업체명', columnGuideLabel: '본사 · 업체명' }, { key: 'regionalRate', label: '요율%' }, { key: 'regionalPerTxFee', label: '건당료' },
        { key: 'masterNm', label: '업체명', columnGuideLabel: '총판 · 업체명' }, { key: 'masterRate', label: '요율%' }, { key: 'masterPerTxFee', label: '건당료' },
        { key: 'branchNm', label: '업체명', columnGuideLabel: '지사 · 업체명' }, { key: 'branchRate', label: '요율%' }, { key: 'branchPerTxFee', label: '건당료' },
        { key: 'agencyNm', label: '업체명', columnGuideLabel: '대리점 · 업체명' }, { key: 'agencyRate', label: '요율%' }, { key: 'agencyPerTxFee', label: '건당료' },
        { key: 'salesOfficeNm', label: '업체명', columnGuideLabel: '영업점 · 업체명' }, { key: 'salesOfficeRate', label: '요율%' }, { key: 'salesOfficePerTxFee', label: '건당료' },
        { key: 'totalNm', label: '기준통화', columnGuideLabel: '합계 · 가맹 기준통화(프로필)' }, { key: 'totalRate', label: '요율%' }, { key: 'totalPerTxFee', label: '건당료' },
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
          { key: 'policyCur', label: '통화' },
          { key: 'startDttm', label: '시작일시' },
          { key: 'endDttm', label: '종료일시' },
          { key: 'hqNm', label: '업체명' }, { key: 'hqRate', label: '요율%' }, { key: 'hqPerTxFee', label: '건당료' },
          { key: 'regionalNm', label: '업체명' }, { key: 'regionalRate', label: '요율%' }, { key: 'regionalPerTxFee', label: '건당료' },
          { key: 'masterNm', label: '업체명' }, { key: 'masterRate', label: '요율%' }, { key: 'masterPerTxFee', label: '건당료' },
          { key: 'branchNm', label: '업체명' }, { key: 'branchRate', label: '요율%' }, { key: 'branchPerTxFee', label: '건당료' },
          { key: 'agencyNm', label: '업체명' }, { key: 'agencyRate', label: '요율%' }, { key: 'agencyPerTxFee', label: '건당료' },
          { key: 'salesOfficeNm', label: '업체명' }, { key: 'salesOfficeRate', label: '요율%' }, { key: 'salesOfficePerTxFee', label: '건당료' },
          { key: 'totalNm', label: '기준통화' }, { key: 'totalRate', label: '요율%' }, { key: 'totalPerTxFee', label: '건당료' },
          { key: 'changedBy', label: '변경자' }
        ]
      }
    },
    '/comp/compInfoHistList': {
      searchRows: [
        [
          { label: '접속일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate' },
          { label: '업체명', type: 'text', name: 'searchCompNm', placeholder: '업체명·업체코드', i18nPhKey: 'searchCompQ' },
          { label: '변경자명', type: 'text', name: 'searchChangedBy' },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수'],
      buttons: [],
      columns: [
        { key: 'rowNo', label: 'No.' },
        { key: 'chgDt', label: '변경일시' },
        { key: 'compId', label: '업체코드' },
        { key: 'compNm', label: '업체명' },
        { key: 'chgTarget', label: '변경대상' },
        { key: 'chgBefore', label: '변경 전' },
        { key: 'chgAfter', label: '변경 후' },
        { key: 'changedBy', label: '변경자' }
      ],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/calc/payList': {
      listSortDirAnchor: 'refresh',
      payListVariant: 'INTEGRATED',
      payListStatusBar: true,
      payListFinancialInline: true,
      /** VIEW SETTING: 열 목록은 pay-list-integrated-catalog.js 로 채움 */
      tableColumnGuide: true,
      searchFormClass: 'pay-mng-search-form',
      searchRows: [
        [
          { label: '거래일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate', quickdateLabels: ['당일', '당월', '전일', '1주', '2주', '전월'], quickdateRanges: ['day', 'month', 'prevDay', 'week', 'week2', 'prevMonth'] },
          { label: '결제대행사', type: 'select', name: 'searchPgCd', options: [{ v: '', t: '전체' }], size: 13 },
          { label: '정산주기', type: 'select', name: 'searchCycle', options: CALC_CYCLE_SEARCH_OPTIONS, size: 10 }
        ],
        [
          { label: '검색구분', type: 'select', name: 'searchFieldType', options: [
            { v: 'ALL', t: '전체' },
            { v: 'CUSTOMER_ID', t: '고객ID' },
            { v: 'COMP_NM', t: '업체명' },
            { v: 'COMP_ID', t: '업체코드' },
            { v: 'APPROVAL_NO', t: '승인번호' },
            { v: 'ORDER_NO', t: '주문번호' },
            { v: 'MID', t: 'MID' },
            { v: 'ROUTE', t: '루트' },
            { v: 'CURRENCY', t: '통화' },
            { v: 'STATUS', t: '상태' },
            { v: 'AMOUNT', t: '금액' }
          ], size: 11 },
          { label: '검색어', type: 'text', name: 'searchKeyword', placeholder: '검색어', size: 22 },
          { label: '상태구분', type: 'select', name: 'searchPayDivCd', options: [
            { v: '', t: '전체' },
            { v: '10', t: '성공' },
            { v: '20', t: '취소' },
            { v: 'FAIL', t: '실패' },
            { v: '40', t: '자동무효' },
            { v: '41', t: '이메일 무효' },
            { v: '42', t: '자동환불' },
            { v: '31', t: '강제환불' }
          ], size: 11 },
          { type: 'searchBtn', label: '검색' },
          { type: 'button', name: 'searchReset', label: '검색 초기화' }
        ]
      ],
      searchRows2: [],
      searchRows3: [],
      noticeList: [
        '통합 결제내역: 칠페이 API 동기화·노티 적재·URL직접결제 등 전 출처를 한 그리드에 표시합니다. 앞쪽 컬럼(거래일~Settled)은 칠페이 거래내역 시트와 대응합니다.',
        '[순서] 내림차순·오름차순은 상단 [새로고침] 왼쪽 메뉴에서 고르며, 누르는 즉시 목록을 다시 조회합니다.',
        '[후속조치]는 본사설정 > 전산설정관리에서 기능을 켠 경우에만 동작합니다 (NOTI 환경설정과 동일).',
        '취소 건에 대한 정산 수수료 및 부가세는 정산 주기에 따라 반영됩니다.',
        '정산 주기 및 정산 수수료는 가맹점별로 상이할 수 있습니다.',
        '상단 한 줄: 건수·통화별 총거래·승인·취소·수수료·담보·부가세·추정결산(승인−(취소+수수료+담보+부가세), 수수료내역과 동일 건별 산식)·아래 상태별 금액 pill. 본사·총본사는 통화별 병기, 총판·하위는 기준 통화 한 줄.'
      ],
      summary: ['건수'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'excelDownBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      /** 2단 헤더·그리드 열: `site/js/pay-list-integrated-catalog.js` → applyPayListIntegratedCatalog (노티 기본 카탈로그와 동일 집합) */
      headerGroups: [],
      columns: [],
      emptyMessage: '조회된 데이터가 없습니다.',
      /** 결제관리: 한 번에 보기 기본 50 (통합 결제내역·분류·URL/챗봇 등 clone 동일) */
      paginationSizeOptions: [50, 100, 300, 400, 500],
      paginationDefaultSize: 50
    },
    /** ChillPay Transaction API — Search Payment Transaction (실시간, DB 비저장) */
    '/calc/chillPayTrList': {
      listSortDirAnchor: 'refresh',
      /** ChillPay Transaction API 페이지당 최대 100건 — 초과 요청은 서버에서 잘림 */
      paginationSizeOptions: [50, 100],
      paginationDefaultSize: 50,
      payListStatusBar: true,
      payListFinancialInline: true,
      tableColumnGuide: true,
      /** VIEW SETTING에서 숨길 수 없는 열: 결제내역 통합과 동일한 앞부분(번호·거래ID·업체·거래일시·Route) */
      columnGuideFixedKeys: ['rowNo', 'transactionId', 'compNm', 'compId', 'trnDate', 'trnTime', 'routeNo'],
      searchFormClass: 'screen-search-form pay-mng-search-form',
      searchRows: [
        [
          { label: '거래일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate', quickdateLabels: ['당일', '당월', '전일', '1주', '2주', '전월'], quickdateRanges: ['day', 'month', 'prevDay', 'week', 'week2', 'prevMonth'] },
          { label: '정렬', type: 'select', name: 'searchOrderBy', options: [
            { v: 'TransactionId', t: '승인번호' },
            { v: 'TransactionDate', t: '거래일자' },
            { v: 'OrderNo', t: '주문번호' },
            { v: 'PaymentDate', t: 'PaymentDate' },
            { v: 'Amount', t: '결제금액' },
            { v: 'Merchant', t: 'Merchant' },
            { v: 'Customer', t: '고객' },
            { v: 'Status', t: '상태' }
          ], size: 12 }
        ],
        [
          { label: '검색구분', type: 'select', name: 'searchFieldType', options: [
            { v: 'ALL', t: '전체' },
            { v: 'APPROVAL_NO', t: '승인번호' },
            { v: 'ORDER_NO', t: '주문번호' },
            { v: 'CUSTOMER_ID', t: '고객ID' },
            { v: 'MID', t: 'MID' },
            { v: 'ROUTE', t: '루트' },
            { v: 'STATUS', t: '상태' }
          ], size: 11 },
          { label: '검색어', type: 'text', name: 'searchKeyword', placeholder: '검색어', size: 22 },
          { label: '상태구분', type: 'select', name: 'searchPayDivCd', options: [
            { v: '', t: '전체' },
            { v: '10', t: '성공' },
            { v: '20', t: '취소' },
            { v: 'FAIL', t: '실패' },
            { v: '40', t: '자동무효' },
            { v: '41', t: '이메일 무효' },
            { v: '42', t: '자동환불' },
            { v: '31', t: '강제환불' }
          ], size: 11 },
          { type: 'searchBtn', label: '검색' },
          { type: 'button', name: 'searchReset', label: '검색 초기화' }
        ]
      ],
      noticeList: [
        'ChillPay API Transaction Services — Search Payment Transaction(실시간)입니다. ICOPAY 내부 DB(pg_trnsctn)가 아니라 칠페이 서버에서 직접 목록을 가져옵니다. ziobiz/NOTI 노티미들웨어의 종합거래·피지거래내역과 유사한 용도로 쓸 수 있습니다.',
        '자격: 배포설정 > API배포설정 또는 tb_pg_agency(ChillPay)의 MerchantCode·ApiKey·MD5 Secret Key·샌드박스 여부를 사용합니다.',
        '순서(내림차순·오름차순)는 [새로고침] 왼쪽 메뉴에서 고르며, 누르는 즉시 다시 조회됩니다(기본 내림차순). TransactionDate 범위는 검색 기간(날짜)을 ChillPay 형식(dd/MM/yyyy HH:mm:ss)으로 변환합니다. 문서: ChillPay-API-Transaction-Services-Document-EN_v1.0.6.',
        '그리드 열 노출은 상단 VIEW SETTING에서 조정합니다(저장 시 사용자별로 유지). 번호·승인번호·업체명·업체코드·거래일·거래시간(JP·TH 두 줄)·루트는 그리드에 항상 표시되며 VIEW SETTING 목록에는 나오지 않습니다. 거래일은 YYYY-MM-DD(예: 2026-05-09) 형식으로 표시됩니다. 본사설정 → 조직항목설정에서 화면「통합내역」 허용 열을 제한할 수 있습니다.'
      ],
      summary: ['건수'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'excelDownBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      columns: [
        { key: 'rowNo', label: 'No.' },
        { key: 'transactionId', label: '승인번호' },
        { key: 'compNm', label: '업체명' },
        { key: 'compId', label: '업체코드' },
        { key: 'trnDate', label: '거래일' },
        { key: 'trnTime', label: '거래시간' },
        { key: 'routeNo', label: '루트' },
        { key: 'merchant', label: 'Merchant(MID)' },
        { key: 'customer', label: '고객' },
        { key: 'orderNo', label: '주문번호' },
        { key: 'paymentChannel', label: 'PaymentChannel' },
        { key: 'payCompletedAt', label: '결제시각' },
        { key: 'amount', label: '결제금액' },
        { key: 'refundAmount', label: 'RefundAmount' },
        { key: 'fee', label: '수수료' },
        { key: 'discount', label: 'Discount' },
        { key: 'totalAmount', label: '총금액' },
        { key: 'currency', label: '통화' },
        { key: 'status', label: '상태' },
        { key: 'settled', label: '정산' },
        { key: 'icopay', label: 'ICOPAY' },
        { key: 'description', label: 'Description' },
        { key: 'transactionDate', label: '거래일자' },
        { key: 'paymentDate', label: 'PaymentDate(원문)' }
      ],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/calc/jpayTrList': {
      listSortDirAnchor: 'refresh',
      paginationSizeOptions: [50, 100, 300],
      paginationDefaultSize: 50,
      payListStatusBar: true,
      searchFormClass: 'screen-search-form pay-mng-search-form',
      searchRows: [
        [
          { label: '거래일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate', quickdateLabels: ['당일', '당월', '전일', '1주', '2주', '전월'], quickdateRanges: ['day', 'month', 'prevDay', 'week', 'week2', 'prevMonth'] }
        ],
        [
          { label: '검색구분', type: 'select', name: 'searchFieldType', options: [
            { v: 'ALL', t: '전체' },
            { v: 'ORDER_NO', t: '주문번호' },
            { v: 'APPROVAL_NO', t: '승인번호' },
            { v: 'MID', t: 'MID' }
          ], size: 11 },
          { label: '검색어', type: 'text', name: 'searchKeyword', placeholder: '검색어', size: 22 },
          { label: '상태구분', type: 'select', name: 'searchPayDivCd', options: [
            { v: '', t: '전체' },
            { v: '10', t: '성공' },
            { v: '30', t: '환불' },
            { v: '31', t: '강제환불' },
            { v: '99', t: '실패' }
          ], size: 11 },
          { type: 'searchBtn', label: '검색' },
          { type: 'button', name: 'searchReset', label: '검색 초기화' }
        ]
      ],
      noticeList: [
        'JPAY 가맹 포털(merchant.j-pay.net)에 자동 로그인 → Export 다운로드 → ICOPAY 결제내역 대조·반영합니다. 목록 API가 없어 포털 Export 엑셀을 사용합니다.',
        '본사설정 > 결제대행사로직에서 총판별 JPAY 포털 계정을 등록하고, 전산설정관리에서 동기화 기간을 설정하세요. VPS에 Node.js·Playwright(Chromium)가 필요합니다.',
        '[JPAY 동기화]는 선택 기간 Export 후 캐시 목록을 갱신합니다. 날짜 없이 검색하면 최근 동기화 범위(일)로 자동 동기화합니다.'
      ],
      summary: ['건수'],
      buttons: [
        { id: 'jpayTrSyncBtn', label: 'JPAY 동기화', cls: 'btn-primary' },
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'excelDownBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      columns: [
        { key: 'rowNo', label: 'No.' },
        { key: 'masterDistNm', label: '총판' },
        { key: 'portalLabel', label: '포털표시' },
        { key: 'transactionId', label: '승인번호' },
        { key: 'compNm', label: '업체명' },
        { key: 'compId', label: '업체코드' },
        { key: 'trnDate', label: '거래일' },
        { key: 'trnTime', label: '거래시간' },
        { key: 'merchant', label: 'MID' },
        { key: 'orderNo', label: '주문번호' },
        { key: 'amount', label: '결제금액' },
        { key: 'currency', label: '통화' },
        { key: 'status', label: 'JPAY상태' },
        { key: 'icopay', label: 'ICOPAY' },
        { key: 'dbStatus', label: 'DB상태' },
        { key: 'fee', label: '수수료' },
        { key: 'refundStatus', label: '환불' },
        { key: 'chargeback', label: '차지백' },
        { key: 'cardBin', label: 'Card BIN' },
        { key: 'urlSource', label: 'URL출처' }
      ],
      emptyMessage: '동기화 후 조회됩니다. [JPAY 동기화]를 실행하세요.'
    },
    '/calc/queryIntegrated': {
      isDailySummaryScreen: true,
      dailySummaryKind: 'jpay',
      listSortDirAnchor: 'refresh',
      searchFormClass: 'screen-search-form pay-mng-search-form',
      searchRows: [
        [
          { label: '거래일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate', quickdateLabels: ['당일', '당월', '전일', '1주', '2주', '전월'], quickdateRanges: ['day', 'month', 'prevDay', 'week', 'week2', 'prevMonth'] }
        ],
        [
          { label: '검색구분', type: 'select', name: 'searchFieldType', options: [
            { v: 'ALL', t: '전체' },
            { v: 'ORDER_NO', t: '주문번호' },
            { v: 'APPROVAL_NO', t: '승인번호' },
            { v: 'MID', t: 'MID' }
          ], size: 11 },
          { label: '검색어', type: 'text', name: 'searchKeyword', placeholder: '검색어', size: 22 },
          { label: '상태구분', type: 'select', name: 'searchPayDivCd', options: [
            { v: '', t: '전체' },
            { v: '10', t: '성공' },
            { v: '30', t: '환불' },
            { v: '31', t: '강제환불' },
            { v: '99', t: '실패' }
          ], size: 11 },
          { type: 'searchBtn', label: '검색' }
        ]
      ],
      noticeList: [
        '통합조회(JPAY Export 캐시)와 동일 필터로, 거래일(trnDate) 구간을 일 단위로 집계합니다. 일자별 성공·실패·취소·무효·이메일무효·환불·강제환불·기타 건수는 해당 일 전체 건 기준입니다. 일자 행을 더블클릭하면 아래 「선택 일자 상세」에 해당 일 통합조회 전체·금액 요약이 표시됩니다.',
        '조회 기간은 최대 93일입니다. 당월 등으로 종료일이 오늘 이후이면 표시는 전산 기준일(오늘)까지만 합니다(미래 일자 미표시). 통합조회 화면에서 [JPAY 동기화]로 캐시를 갱신한 뒤 조회하세요.'
      ],
      summary: ['건수'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'excelDownBtn', label: '엑셀다운로드', cls: 'btn-info' },
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' }
      ],
      columns: [
        { key: 'rowNo', label: '번호' },
        { key: 'day', label: '일자' },
        { key: 'totalElements', label: '총건수' },
        { statusBucketKey: 'SUCCESS', label: '성공' },
        { statusBucketKey: 'FAIL', label: '실패' },
        { statusBucketKey: 'CANCEL', label: '취소' },
        { statusBucketKey: 'VOID', label: '무효' },
        { statusBucketKey: 'EMAIL_VOID', label: '이메일 무효' },
        { statusBucketKey: 'REFUND', label: '환불' },
        { statusBucketKey: 'FORCE_REFUND', label: '강제환불' },
        { statusBucketKey: 'OTHER', label: '기타' },
        { key: 'payCur_THB', label: 'THB', currencyCode: 'THB' },
        { key: 'payCur_JPY', label: 'JPY', currencyCode: 'JPY' },
        { key: 'payCur_KRW', label: 'KRW', currencyCode: 'KRW' },
        { key: 'payCur_USD', label: 'USD', currencyCode: 'USD' },
        { key: 'payCur_CNY', label: 'CNY', currencyCode: 'CNY' },
        { key: 'note', label: '비고' }
      ],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/calc/splitPayList': {
      listSortDirAnchor: 'refresh',
      paginationSizeOptions: [50, 100, 300],
      paginationDefaultSize: 50,
      payListStatusBar: true,
      searchFormClass: 'screen-search-form pay-mng-search-form',
      searchRows: [
        [
          { label: '등록일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate', quickdateLabels: ['당일', '당월', '전일', '1주', '2주', '전월'], quickdateRanges: ['day', 'month', 'prevDay', 'week', 'week2', 'prevMonth'] }
        ],
        [
          { label: '업체코드', type: 'text', name: 'compId', placeholder: '업체코드', size: 14 },
          { label: '계약번호', type: 'text', name: 'contractNo', placeholder: '계약번호', size: 18 },
          { label: '상태', type: 'select', name: 'status', options: [
            { v: '', t: '전체' },
            { v: 'ACTIVE', t: '진행중' },
            { v: 'COMPLETED', t: '완료' },
            { v: 'STOPPED', t: '중지' },
            { v: 'CANCELLED', t: '취소' }
          ], size: 11 },
          { type: 'searchBtn', label: '검색' },
          { type: 'button', name: 'searchReset', label: '검색 초기화' }
        ]
      ],
      noticeList: [
        'URL 분할결제 계약 목록입니다. 「URL 분할결제」에서 사용을 켠 가맹만 계약을 생성할 수 있습니다.',
        '각 회차 결제는 운영 URL PG에 따라 ChillPay(pay.html) 또는 JPAY(jpay-pay.html) 결제창으로 진행됩니다. 1회차는 즉시결제(IMMEDIATE) 또는 링크발송(LINK)이며, 미납 회차는 매일 결제 링크 메일이 발송됩니다.',
        '공개 분할결제 URL 또는 API(POST /api/pay/split/contracts)로 계약합니다. 수수료는 본사 수수료정책의 분할수수료율·분할고정수수료(건)가 계약 생성 시 스냅샷으로 저장됩니다.'
      ],
      summary: ['건수'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'excelDownBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      columns: [
        { key: 'rowNo', label: 'No.' },
        { key: 'contractNo', label: '계약번호' },
        { key: 'compId', label: '업체코드' },
        { key: 'customerEmail', label: '고객이메일' },
        { key: 'totalAmount', label: '총금액' },
        { key: 'installmentCount', label: '분할횟수' },
        { key: 'paidCount', label: '납부횟수' },
        { key: 'status', label: '상태' },
        { key: 'contractDate', label: '계약일' },
        { key: 'createdAt', label: '등록일시' }
      ],
      emptyMessage: '조회된 분할결제 계약이 없습니다.'
    },
    '/splitpay/progressMng': {
      listSortDirAnchor: 'refresh',
      paginationSizeOptions: [50, 100, 300],
      paginationDefaultSize: 50,
      payListStatusBar: true,
      searchFormClass: 'screen-search-form pay-mng-search-form',
      searchRows: [
        [
          { label: '납부예정일', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate', quickdateLabels: ['당일', '당월', '전일', '1주', '2주', '전월'], quickdateRanges: ['day', 'month', 'prevDay', 'week', 'week2', 'prevMonth'] }
        ],
        [
          { label: '업체코드', type: 'text', name: 'compId', placeholder: '업체코드', size: 14 },
          { label: '계약번호', type: 'text', name: 'contractNo', placeholder: '계약번호', size: 18 },
          { label: '회차상태', type: 'select', name: 'status', options: [
            { v: '', t: '전체' },
            { v: 'PENDING', t: '미납' },
            { v: 'PAID', t: '납부완료' },
            { v: 'CANCELLED', t: '취소' }
          ], size: 11 },
          { type: 'searchBtn', label: '검색' },
          { type: 'button', name: 'searchReset', label: '검색 초기화' }
        ]
      ],
      noticeList: [
        '분할결제 계약별 회차 진행 현황입니다. 계약 단위 납부율·회차별 예정일·납부 상태를 확인합니다.',
        '회차 결제가 완료되면 결제관리 「결제내역」과 「분할결제내역」에도 동일 거래가 표시됩니다.',
        '가맹점은 본인 소속 가맹 계약·회차만 조회됩니다.'
      ],
      summary: ['건수'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'excelDownBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      columns: [
        { key: 'rowNo', label: 'No.' },
        { key: 'contractNo', label: '계약번호' },
        { key: 'compId', label: '업체코드' },
        { key: 'installmentNo', label: '회차' },
        { key: 'installmentCount', label: '총회차' },
        { key: 'paidCount', label: '납부회차' },
        { key: 'progressPct', label: '진행률(%)' },
        { key: 'amount', label: '회차금액' },
        { key: 'currencyCode', label: '통화' },
        { key: 'dueDate', label: '납부예정일' },
        { key: 'status', label: '회차상태' },
        { key: 'paidAt', label: '납부일시' },
        { key: 'customerEmail', label: '고객이메일' },
        { key: 'contractStatus', label: '계약상태' },
        { key: 'orderNo', label: '주문번호' }
      ],
      emptyMessage: '조회된 분할결제 회차가 없습니다.'
    },
    '/splitpay/mailMng': {
      listSortDirAnchor: 'refresh',
      paginationSizeOptions: [50, 100, 300],
      paginationDefaultSize: 50,
      payListStatusBar: true,
      searchFormClass: 'screen-search-form pay-mng-search-form',
      searchRows: [
        [
          { label: '납부예정일', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate', quickdateLabels: ['당일', '당월', '전일', '1주', '2주', '전월'], quickdateRanges: ['day', 'month', 'prevDay', 'week', 'week2', 'prevMonth'] }
        ],
        [
          { label: '업체코드', type: 'text', name: 'compId', placeholder: '업체코드', size: 14 },
          { label: '계약번호', type: 'text', name: 'contractNo', placeholder: '계약번호', size: 18 },
          { label: '회차상태', type: 'select', name: 'status', options: [
            { v: 'PENDING', t: '미납' },
            { v: '', t: '전체' },
            { v: 'PAID', t: '납부완료' },
            { v: 'CANCELLED', t: '취소' }
          ], size: 11 },
          { type: 'searchBtn', label: '검색' },
          { type: 'button', name: 'searchReset', label: '검색 초기화' }
        ]
      ],
      noticeList: [
        '분할결제 회차별 결제 링크 이메일 발송 현황입니다. D-1·당일(D0)·D+1·D+2·D+3 자동 발송 일시를 확인할 수 있습니다.',
        '미납(PENDING) 회차는 [링크재발송]으로 결제 안내 메일을 수동 재발송할 수 있습니다.',
        '자동 발송은 매일 스케줄러가 처리하며, 운영 메일 로그는 「운영관리 > 메일로그」에서 확인합니다.'
      ],
      summary: ['건수'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'excelDownBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      columns: [
        { key: 'rowNo', label: 'No.' },
        { key: 'contractNo', label: '계약번호' },
        { key: 'compId', label: '업체코드' },
        { key: 'installmentNo', label: '회차' },
        { key: 'customerEmail', label: '고객이메일' },
        { key: 'dueDate', label: '납부예정일' },
        { key: 'status', label: '회차상태' },
        { key: 'mailDMinus1Sent', label: 'D-1발송' },
        { key: 'mailD0Sent', label: 'D0발송' },
        { key: 'mailD1Sent', label: 'D+1발송' },
        { key: 'mailD2Sent', label: 'D+2발송' },
        { key: 'mailD3Sent', label: 'D+3발송' },
        { key: 'splitPayMailResend', label: '재발송', type: 'splitPayMailResendBtn' }
      ],
      emptyMessage: '조회된 분할결제 이메일 대상이 없습니다.'
    },
    '/splitpay/emailSettings': {
      isForm: true,
      splitPayEmailSettingsScreen: true,
      hideListGrid: true,
      formSections: [
        {
          title: '분할결제 이메일설정',
          notice: '분할결제 이메일설정 안내',
          rows: [
            [{ type: 'customHtml', col: 12, html: '<div id="splitPayEmailSettingsRoot" class="split-pay-email-settings-root"></div>' }]
          ]
        }
      ],
      buttons: [
        { id: 'splitPayEmailSettingsSaveBtn', label: '저장', cls: 'btn-primary' },
        { id: 'splitPayEmailSettingsTestBtn', label: '테스트발송', cls: 'btn-outline-primary' }
      ]
    },
    '/calc/dailyIntegrated': {
      isDailySummaryScreen: true,
      dailySummaryKind: 'chill',
      listSortDirAnchor: 'refresh',
      searchFormClass: 'screen-search-form pay-mng-search-form',
      searchRows: [
        [
          { label: '거래일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate', quickdateLabels: ['당일', '당월', '전일', '1주', '2주', '전월'], quickdateRanges: ['day', 'month', 'prevDay', 'week', 'week2', 'prevMonth'] },
          { label: '정렬', type: 'select', name: 'searchOrderBy', options: [
            { v: 'TransactionId', t: '승인번호' },
            { v: 'TransactionDate', t: '거래일자' },
            { v: 'OrderNo', t: '주문번호' },
            { v: 'PaymentDate', t: 'PaymentDate' },
            { v: 'Amount', t: '결제금액' }
          ], size: 12 }
        ],
        [
          { label: '검색구분', type: 'select', name: 'searchFieldType', options: [
            { v: 'ALL', t: '전체' },
            { v: 'APPROVAL_NO', t: '승인번호' },
            { v: 'ORDER_NO', t: '주문번호' },
            { v: 'CUSTOMER_ID', t: '고객ID' },
            { v: 'MID', t: 'MID' },
            { v: 'ROUTE', t: '루트' },
            { v: 'STATUS', t: '상태' }
          ], size: 11 },
          { label: '검색어', type: 'text', name: 'searchKeyword', placeholder: '검색어', size: 22 },
          { label: '상태구분', type: 'select', name: 'searchPayDivCd', options: [
            { v: '', t: '전체' },
            { v: '10', t: '성공' },
            { v: '20', t: '취소' },
            { v: 'FAIL', t: '실패' },
            { v: '40', t: '자동무효' },
            { v: '41', t: '이메일 무효' },
            { v: '42', t: '자동환불' },
            { v: '31', t: '강제환불' }
          ], size: 11 },
          { type: 'searchBtn', label: '검색' }
        ]
      ],
      noticeList: [
        '통합내역(칠페이 결제 검색)과 동일 자격·필터로, 거래일자(TransactionDate) 구간을 일 단위로 집계합니다. 일자별 금액·상태는 해당 일 전체 목록(최대 15,000건) 합산입니다. 일 15,000건 초과 시 통합내역 상단 요약과 동일하게 스캔 상한 안내됩니다. 일자 행을 더블클릭하면 아래 「선택 일자 상세」에 해당 일 전체 거래·금액 요약이 표시됩니다.',
        '조회 기간은 최대 93일입니다. 당월 등으로 종료일이 오늘 이후이면 표시는 전산 기준일(오늘)까지만 합니다(미래 일자 미표시). 칠페이 API 장애 시 해당 일에 오류 메시지가 표시될 수 있습니다.'
      ],
      summary: ['건수'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'excelDownBtn', label: '엑셀다운로드', cls: 'btn-info' },
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' }
      ],
      columns: [
        { key: 'rowNo', label: '번호' },
        { key: 'day', label: '일자' },
        { key: 'totalElements', label: '총건수(칠페이)' },
        { statusBucketKey: 'SUCCESS', label: '성공' },
        { statusBucketKey: 'FAIL', label: '실패' },
        { statusBucketKey: 'CANCEL', label: '취소' },
        { statusBucketKey: 'VOID', label: '무효' },
        { statusBucketKey: 'EMAIL_VOID', label: '이메일 무효' },
        { statusBucketKey: 'REFUND', label: '환불' },
        { statusBucketKey: 'FORCE_REFUND', label: '강제환불' },
        { statusBucketKey: 'OTHER', label: '기타' },
        { key: 'payCur_THB', label: 'THB', currencyCode: 'THB' },
        { key: 'payCur_JPY', label: 'JPY', currencyCode: 'JPY' },
        { key: 'payCur_KRW', label: 'KRW', currencyCode: 'KRW' },
        { key: 'payCur_USD', label: 'USD', currencyCode: 'USD' },
        { key: 'payCur_CNY', label: 'CNY', currencyCode: 'CNY' },
        { key: 'note', label: '비고' }
      ],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/calc/dailyPay': {
      isDailySummaryScreen: true,
      dailySummaryKind: 'pay',
      payListVariant: 'INTEGRATED',
      detailPayListVariant: 'INTEGRATED',
      listSortDirAnchor: 'refresh',
      searchFormClass: 'pay-mng-search-form',
      searchRows: [
        [
          { label: '거래일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate', quickdateLabels: ['당일', '당월', '전일', '1주', '2주', '전월'], quickdateRanges: ['day', 'month', 'prevDay', 'week', 'week2', 'prevMonth'] },
          { label: '결제대행사', type: 'select', name: 'searchPgCd', options: [{ v: '', t: '전체' }], size: 13 },
          { label: '정산주기', type: 'select', name: 'searchCycle', options: CALC_CYCLE_SEARCH_OPTIONS, size: 10 }
        ],
        [
          { label: '검색구분', type: 'select', name: 'searchFieldType', options: [
            { v: 'ALL', t: '전체' },
            { v: 'CUSTOMER_ID', t: '고객ID' },
            { v: 'COMP_NM', t: '업체명' },
            { v: 'COMP_ID', t: '업체코드' },
            { v: 'APPROVAL_NO', t: '승인번호' },
            { v: 'ORDER_NO', t: '주문번호' },
            { v: 'MID', t: 'MID' },
            { v: 'ROUTE', t: '루트' },
            { v: 'CURRENCY', t: '통화' },
            { v: 'STATUS', t: '상태' },
            { v: 'AMOUNT', t: '금액' }
          ], size: 11 },
          { label: '검색어', type: 'text', name: 'searchKeyword', placeholder: '검색어', size: 22 },
          { label: '상태구분', type: 'select', name: 'searchPayDivCd', options: [
            { v: '', t: '전체' },
            { v: '10', t: '성공' },
            { v: '20', t: '취소' },
            { v: 'FAIL', t: '실패' },
            { v: '40', t: '자동무효' },
            { v: '41', t: '이메일 무효' },
            { v: '42', t: '자동환불' },
            { v: '31', t: '강제환불' }
          ], size: 11 },
          { type: 'searchBtn', label: '검색' }
        ]
      ],
      noticeList: [
        '결제내역(/calc/payList, INTEGRATED)과 동일 필터·조직 범위로, 적재일(createdAt) 기준 일자별 집계합니다. 일자별 성공·실패·취소·무효·이메일무효·환불·강제환불·기타 건수는 해당 일 전체 건 기준입니다. 일자 행을 더블클릭하면 아래 「선택 일자 상세」에 해당 일 전체 결제내역·총거래~추정결산 요약이 표시됩니다(거래일 열 없음).',
        '조회 기간은 최대 93일입니다. 당월 등으로 종료일이 오늘 이후이면 표시는 전산 기준일(오늘)까지만 합니다(미래 일자 미표시).'
      ],
      summary: ['건수'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'excelDownBtn', label: '엑셀다운로드', cls: 'btn-info' },
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' }
      ],
      columns: [
        { key: 'rowNo', label: '번호' },
        { key: 'day', label: '일자' },
        { key: 'txnCount', label: '전체건수' },
        { statusBucketKey: 'SUCCESS', label: '성공' },
        { statusBucketKey: 'FAIL', label: '실패' },
        { statusBucketKey: 'CANCEL', label: '취소' },
        { statusBucketKey: 'VOID', label: '무효' },
        { statusBucketKey: 'EMAIL_VOID', label: '이메일 무효' },
        { statusBucketKey: 'REFUND', label: '환불' },
        { statusBucketKey: 'FORCE_REFUND', label: '강제환불' },
        { statusBucketKey: 'OTHER', label: '기타' },
        { key: 'payCur_THB', label: 'THB', currencyCode: 'THB' },
        { key: 'payCur_JPY', label: 'JPY', currencyCode: 'JPY' },
        { key: 'payCur_KRW', label: 'KRW', currencyCode: 'KRW' },
        { key: 'payCur_USD', label: 'USD', currencyCode: 'USD' },
        { key: 'payCur_CNY', label: 'CNY', currencyCode: 'CNY' }
      ],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    /** ChillPay Transaction API — 통합정산(Search Settlement Transaction, ICOPAY 정산 DB 비사용) */
    '/calc/chillPaySettlementList': {
      /** 상단 액션: [새로고침] 왼쪽에 내림차순·오름차순(칠페이 OrderDir) */
      listSortDirAnchor: 'refresh',
      paginationSizeOptions: [50, 100, 300, 400, 500],
      paginationDefaultSize: 50,
      payListStatusBar: true,
      payListFinancialInline: true,
      tableColumnGuide: true,
      /** VIEW SETTING·조직항목설정 고정열: 번호만. 통화 포함 그 외 열은 VIEW SETTING에서 켜고 끔 */
      columnGuideFixedKeys: ['rowNo'],
      searchFormClass: 'screen-search-form pay-mng-search-form',
      searchRows: [
        [
          { label: '', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate' }
        ],
        [
          { label: '검색구분', type: 'select', name: 'searchFieldType', options: [
            { v: 'ALL', t: '전체' },
            { v: 'COMP_NM', t: '업체명' },
            { v: 'COMP_ID', t: '업체코드' },
            { v: 'APPROVAL_NO', t: '승인번호' },
            { v: 'ORDER_NO', t: '주문번호' },
            { v: 'MID', t: 'MID' },
            { v: 'ROUTE', t: '루트' },
            { v: 'CURRENCY', t: '통화' },
            { v: 'STATUS', t: '상태' },
            { v: 'AMOUNT', t: '금액' }
          ], size: 11 },
          { label: '검색어', type: 'text', name: 'searchKeyword', placeholder: '검색어', size: 22 },
          { label: '상태그룹', type: 'select', name: 'searchStatusGroup', options: [
            { v: 'ALL', t: '전체' },
            { v: 'SUCCESS', t: '성공' },
            { v: 'FAIL', t: '실패' },
            { v: 'CANCEL', t: '취소' },
            { v: 'VOID', t: '무효' },
            { v: 'MANUAL_VOID', t: '수동무효' },
            { v: 'REFUND', t: '환불' },
            { v: 'FORCE_REFUND', t: '강제환불' },
            { v: 'EXCLUDE_SUCCESS', t: '성공제외' }
          ], size: 11 },
          { type: 'searchBtn', label: '검색' }
        ]
      ],
      noticeList: [
        '「예정(ICOPAY)」열은 배포설정 API연동설정(tb_pg_agency)의 T+N(주말 제외 영업일·결제와 동일 시각) 또는 D+N(달력+N일·일괄 시각)으로 계산합니다. OFF·MID 미매칭이면 비웁니다. 가맹 업체정보의 결제대행사 행에서 예정모드를 비우면 연동 기본을 따르고, OFF/T/D로 덮어쓸 수 있습니다.',
        '칠페이 Transaction Services — Search Settlement Transaction API(/api/v1/settlement/search, 문서 v1.0.6 Table 2.2~2.3)로 조회합니다. 통합내역은 결제 검색(/api/v1/payment/search)·승인/취소 중심 필드이고, 통합정산은 **정산 검색**·지급액·순액·서비스비·이체일 등 **정산 원문**이 다릅니다. ICOPAY 정산 실행·유통망 정산 테이블과 무관합니다.',
        '「정산(이체)」열은 **승인 성공** 건에만 ChillPay Settled를 **정산완료 / 미정산**으로 보입니다. 실패·취소·환불·무효 등은 칸을 비웁니다. 「예정(ICOPAY)」가 채워져 있으면 서울 기준 그 시각 **이전**에는 예정일 미도래로 **미정산**만 보이고, 도래 후에는 ChillPay 값을 그대로 둡니다. Settled=false 는 이체 미완·주기 미지급 등이 흔합니다. **샌드박스**는 전부 false 인 경우도 많습니다. **결제 상태** 열은 노티(tb_pg_trnsctn) 보강이며 ChillPay 이체와 동일하지 않습니다.',
        '칠페이 정산 API 정렬 키는 통합내역과 같이 TransactionId(기본)·PaymentDate 등 문서 표를 따릅니다. 통합내역(결제 검색)과 동일하게 POJO·헤더·MD5 Checksum 규칙으로 호출합니다. 조회 응답 meta에 chillPaySandbox·chillPayTxnApiEnv(SANDBOX/PRODUCTION)가 포함되어 실제 호출 환경을 확인할 수 있습니다. 상단 [새로고침] 왼쪽에서 내림차순·오름차순(OrderDir)을 고릅니다. 첫째 줄에서 결제일 구간·빠른기간을 정한 뒤, 둘째 줄에서 검색구분·검색어·상태그룹을 맞추고 [검색]을 누릅니다. 「전체」는 해당 항목으로 좁히지 않습니다. 성공/실패/취소 등 상태그룹은 정산 API의 Settled(True/False)와 다르므로, 칠페이 응답에 결제 Status가 없을 때는 ICOPAY 노티 적재 건(tb_pg_trnsctn)으로 상태를 보강한 뒤 보조 필터합니다. 이때 상단 요약은 안내 문구대로 현재 페이지만 반영될 수 있습니다. 기간을 비우면 최근 30일 결제일로 조회합니다.',
        '자격: 배포설정 > API배포설정·tb_pg_agency(ChillPay)의 MerchantCode·ApiKey·MD5 Secret Key·샌드박스와 동일합니다.',
        '그리드 열 노출은 상단 VIEW SETTING에서 조정합니다(저장 시 사용자별로 유지). 번호(No.)만 항상 표시됩니다. 거래일·거래시간·결제시각은 통합내역과 같이 거래일은 YYYY-MM-DD(예: 2026-05-09) 형식, 거래시간·결제시각은 JP(일본)·TH(태국) 두 줄로 표시합니다. SettleAmount·NetAmount·정산(이체)·이체일·컷오프·서비스료·환율·통화·승인번호·Merchant·고객·주문번호·PaymentChannel·결제금액·수수료·ICOPAY·Description·칠페이 원문 일시 등은 VIEW SETTING에서 켜고 끌 수 있습니다. 본사설정 → 조직항목설정에서 화면「통합정산」 허용 열을 제한할 수 있습니다.'
      ],
      summary: ['건수'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'excelDownBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      columns: [
        { key: 'rowNo', label: 'No.' },
        { key: 'transactionId', label: '승인번호' },
        { key: 'trnDate', label: '거래일' },
        { key: 'trnTime', label: '거래시간' },
        { key: 'routeNo', label: '루트' },
        { key: 'merchant', label: 'Merchant' },
        { key: 'customer', label: '고객' },
        { key: 'orderNo', label: '주문번호' },
        { key: 'paymentChannel', label: 'PaymentChannel' },
        { key: 'payCompletedAt', label: '결제시각' },
        { key: 'settleAmount', label: '정산금액(Settle)' },
        { key: 'netAmount', label: '순액(Net)' },
        { key: 'exchangeRate', label: '환율' },
        { key: 'serviceAmount', label: '서비스료' },
        { key: 'serviceVAT', label: '서비스 VAT' },
        { key: 'serviceWHT', label: '서비스 WHT' },
        { key: 'amount', label: '결제금액' },
        { key: 'refundAmount', label: 'RefundAmount' },
        { key: 'fee', label: '수수료' },
        { key: 'discount', label: 'Discount' },
        { key: 'totalAmount', label: '총금액' },
        { key: 'icopay', label: 'ICOPAY' },
        { key: 'currency', label: '통화' },
        { key: 'status', label: '상태(ICOPAY 보강)' },
        { key: 'settled', label: '정산(이체)' },
        { key: 'transferDate', label: '이체일(Transfer)' },
        { key: 'icopayExpectedSettleAt', label: '예정(ICOPAY)', columnGuideLabel: 'API연동 T+N(영업일·동일시각) 또는 D+N(달력·일괄시각). 칠페이 이체일과 별개.' },
        { key: 'icopayExpectedSettleRule', label: '예정규칙', columnGuideLabel: '적용 규칙 출처(연동기본/가맹 덮어쓰기 등).' },
        { key: 'cutOffTime', label: '컷오프' },
        { key: 'description', label: 'Description' },
        { key: 'transactionDate', label: '거래일자(원문)' },
        { key: 'paymentDate', label: 'PaymentDate(원문)' }
      ],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/calc/calcList': {
      listSortDirAnchor: 'refresh',
      paginationSizeOptions: [50, 100, 300, 400, 500],
      paginationDefaultSize: 50,
      /** VIEW SETTING: 번호·업체코드 고정, 나머지 열은 저장값에 따라 표 헤더·본문 동기 */
      columnGuideFixedKeys: ['rowNo', 'compId'],
      viewSettingDefaultSelectedKeys: [
        'settleMonth', 'orgDivNm', 'hqNm', 'regionalNm', 'masterNm', 'branchNm', 'agencyNm', 'salesOfficeNm', 'curType',
        'aprvCnt', 'aprvAmt', 'aprvFeePct', 'aprvFeeSum', 'aprvFeeVat',
        'canCnt', 'canAmt', 'canFeePct', 'canFeeSum', 'canFeeVat', 'settleAmt'
      ],
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
          qdDistMonthlyField(),
          { label: '업체구분', type: 'select', name: 'searchCompDiv', options: [
            { v: '', t: '전체(단계별 합산)' },
            { v: 'HEADQUARTERS', t: '총본사' },
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
        '유통망 정산: 로그인 소속 조직·그 하위 가맹만 조회됩니다(가맹점정산내역과 동일한 범위). 가맹점 단위 행은 없으며, 하위 가맹 정산액이 조직 행에 합산됩니다. 총본사(HEADQUARTERS) 단계 행도 포함됩니다.',
        '각 조직 행의 승인·취소 수수료 합계는 해당 행 조직 단계에 대응하는 구간만 합산합니다(예: 본사·총본사에만 비율이 있으면 총판 행 수수료는 0에 가깝게 나옵니다). 업체구분을 선택하면 해당 단계만 한 행으로 보입니다.',
        '「포함거래건」은 정산 실행에 저장된 집계 구간 결제 건수(tb_settlement_run.included_txn_cnt)의 합입니다. 구버전(null) 실행은 건수 1로 보정합니다. 「취소발생실행」은 해당 실행에 취소 합계 금액이 0보다 큰 정산 실행 개수입니다(결제 건수와 다릅니다).',
        '동일 조직·정산일이라도 정산서 통화(열 통화)가 다르면 행이 나뉩니다. 승인수수료%·합계는 그 행의 유통 단계 분배액만으로 승인·취소 금액에 비례 배분한 값입니다(가맹 전체 PG 수수료 비율과 다를 수 있음).',
        '조회기준·승인일자는 추후 거래일 기준 필터와 연동 예정이며, 현재는 정산일(calc_dt) 기준입니다. 가맹 지급액·유통 수수료 분배는 가맹점별 배분 설정(tb_distribution_fee_config) 비율을 바탕으로 합니다. 가맹점별 실행 한 줄은 가맹점정산내역에서 확인할 수 있습니다.'
      ],
      summary: ['Total', '정산금액', '수수료', '지급액'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'printBtn', label: '인쇄설정', cls: 'btn-success' },
        { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      columns: [
        { key: 'rowNo', label: 'No.' },
        { key: 'settleMonth', label: '정산월' },
        { key: 'orgDivNm', label: '구분' },
        { key: 'hqNm', label: '총본사' },
        { key: 'regionalNm', label: '본사' },
        { key: 'masterNm', label: '총판' },
        { key: 'branchNm', label: '지사' },
        { key: 'agencyNm', label: '대리점' },
        { key: 'salesOfficeNm', label: '영업점' },
        { key: 'compId', label: '업체코드' },
        { key: 'curType', label: '통화' },
        { key: 'aprvCnt', label: '포함거래건' },
        { key: 'aprvAmt', label: '승인금액' },
        { key: 'aprvFeeCnt', label: '포함거래건' },
        { key: 'aprvFeePct', label: '유통분배/승인%' },
        { key: 'aprvFeeSum', label: '승인수수료합계' },
        { key: 'aprvFeeVat', label: '승인부가세' },
        { key: 'canCnt', label: '취소발생실행' },
        { key: 'canAmt', label: '취소금액' },
        { key: 'canFeeCnt', label: '취소발생실행' },
        { key: 'canFeePct', label: '유통분배/취소%' },
        { key: 'canFeeSum', label: '취소수수료합계' },
        { key: 'canFeeVat', label: '취소부가세' },
        { key: 'settleAmt', label: '정산금액' }
      ]
    },
    '/calc/calcGmList': {
      listSortDirAnchor: 'refresh',
      paginationSizeOptions: [50, 100, 300, 400, 500],
      paginationDefaultSize: 50,
      tableColumnGuide: true,
      columnGuideFixedKeys: ['rowNo', 'compNm', 'compId', 'curType'],
      viewSettingDefaultSelectedKeys: [
        'calcDt', 'settlementCloseDate', 'settlementExecDate', 'targetPeriodText', 'calcCycle', 'calcMethod', 'txnCnt',
        'amount', 'feeAmt', 'holdAmt', 'settlementBatchFee', 'feeVat', 'settleAmt',
        'receivableAmt', 'settlementPublishSts', 'payoutHoldYn'
      ],
      payMngDenseGrid: true,
      noticeList: [
        '한 행은 정산 실행으로 저장된 귀사(가맹) 정산 결과입니다. 정산기간·빠른기간으로 조회한 뒤 [검색] 하세요.',
        '정산대상기간·결제금액·수수료·보증금·정산료·VAT·지급액은 정산배포·정산실행과 동일한 실행 저장값·집계 규칙을 따릅니다. 수수료 열은 건당·결제%·취소·환불(무효 등) 구간을 합산한 거래수수료(tb_settlement_run.total_fee)입니다.',
        '표시 열은 [헬로] 옆 VIEW SETTING에서 조정할 수 있습니다(저장 시 사용자별 유지). 건당·취소·환불 등 세부 분해 열은 같은 거래 구간 합산 보조값입니다.'
      ],
      searchRows: [
        [
          { label: '정산기간', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate' },
          { label: '검색구분', type: 'select', name: 'searchFieldType', options: [
            { v: 'ALL', t: '전체' },
            { v: 'CALC_CYCLE', t: '정산주기' },
            { v: 'CALC_METHOD', t: '정산방법' },
            { v: 'COMP_NM', t: '업체명' },
            { v: 'COMP_ID', t: '업체코드' },
            { v: 'APPROVAL_NO', t: '승인번호' },
            { v: 'MID', t: 'MID' },
            { v: 'ROUTE', t: '루트' },
            { v: 'CURRENCY', t: '통화' },
            { v: 'STATUS', t: '상태' },
            { v: 'SETTLEMENT_PUBLISH_STS', t: '배포상태' },
            { v: 'PAYOUT_HOLD_YN', t: '지급보류' },
            { v: 'AMOUNT', t: '금액' },
            { v: 'SETTLE_TARGET_DAY', t: '정산대상일' },
            { v: 'SETTLE_RUN_DAY', t: '정산일' }
          ], size: 10 },
          { label: '검색어', type: 'text', name: 'searchKeyword', placeholder: '검색어(일: 1~31)', size: 16 },
          { type: 'searchBtn', label: '검색' }
        ]
      ],
      summary: ['건수', '결제금액', '수수료', '보증금', '정산료', 'VAT', '지급액', '미수금'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' },
        { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      headerGroups: [],
      columns: [
        { key: 'rowNo', label: '번호' },
        { key: 'compNm', label: '업체명' },
        { key: 'compId', label: '업체코드' },
        { key: 'curType', label: '통화' },
        { key: 'calcDt', label: '정산일시', columnGuideLabel: '정산 실행 일시(표시 형식은 환경 설정).' }, { key: 'settlementCloseDate', label: '정산마감일', thClass: 'text-center text-nowrap', columnGuideLabel: '집계 구간 마감일.' }, { key: 'settlementExecDate', label: '정산일자', thClass: 'text-center text-nowrap', columnGuideLabel: '정산주기·영업일 기준 배치 예정일.' },
        { key: 'targetPeriodText', label: '정산대상기간', thClass: 'pay-grid-time-dual text-start small', columnGuideLabel: '이번 실행에 포함된 거래 집계 구간(정산실행·배포와 동일).' },
        { key: 'calcCycle', label: '정산주기' },
        { key: 'calcMethod', label: '정산방법' },
        { key: 'txnCnt', label: '건수', thClass: 'text-center text-nowrap', columnGuideLabel: '집계에 포함된 거래 건수.' },
        { key: 'amount', label: '결제금액', thClass: 'text-end', columnGuideLabel: '순매출(승인금액−취소금액). 정산실행 결제액과 동일 개념.' },
        {
          key: 'feeAmt',
          label: '수수료',
          thClass: 'text-end',
          columnGuideLabel: '거래 수수료 합계(저장값). 건당·결제%·취소·환불·무효 등 구간이 정산 집계에 포함된 합과 동일하며, 월 이용료·실행당 고정 부가 등은 결제%측 합에 조정 반영됩니다.'
        },
        { key: 'feeSumPerTx', label: '수수료(건당합)', thClass: 'text-end', columnGuideLabel: '집계 구간 거래별 건당수수료 합산(보조).' },
        { key: 'feeSumPaySide', label: '수수료(결제%·기타합)', thClass: 'text-end', columnGuideLabel: '결제율·USDT·FX·3DS·기타%·실패·차지백 및 실행당 고정 부가 등 합산(보조).' },
        { key: 'feeSumCancel', label: '수수료(취소합)', thClass: 'text-end', columnGuideLabel: '취소(20) 고정 수수료 합(보조).' },
        { key: 'feeSumRefundVoid', label: '수수료(환불·무효합)', thClass: 'text-end', columnGuideLabel: '환불·무효·수동무효 고정 수수료 합(보조).' },
        { key: 'holdAmt', label: '보증금', thClass: 'text-end', columnGuideLabel: '롤링 예치 등 실행 저장 담보(보류) 금액.' },
        { key: 'settlementBatchFee', label: '정산료', thClass: 'text-end', columnGuideLabel: '정산 실행당 1회 정산수수료.' },
        { key: 'feeVat', label: 'VAT', thClass: 'text-end', columnGuideLabel: '거래수수료·정산료 기준 부가세(가맹 전산설정).' },
        { key: 'settleAmt', label: '지급액', thClass: 'text-end', columnGuideLabel: '해당 차수 지급(예정)액(tb_settlement_run.pay_amt).' },
        { key: 'receivableAmt', label: '미수금', thClass: 'text-end', columnGuideLabel: '지급부족 시 해당 실행에 자동 등록된 미수금.' },
        { key: 'cadenceGuideKr', label: '노출주기 안내', thClass: 'text-nowrap', columnGuideLabel: '주기별 노출 요약.' },
        { key: 'settlementPublishSts', label: '배포상태', columnGuideLabel: 'PENDING·DISTRIBUTED·HOLD.' },
        { key: 'payoutHoldYn', label: '지급보류', columnGuideLabel: 'Y면 지급보류 가맹.' },
        { key: 'settlementRunId', label: '실행ID', columnGuideLabel: '정산 실행 PK.' },
        { key: 'pgRootNo', label: '루트' },
        { key: 'feeCnt', label: '수수료발생건수', columnGuideLabel: '거래별 수수료 합이 0 초과인 건수(보조).' },
        { key: 'feeRate', label: '수수료율%', columnGuideLabel: '순매출 대비 거래수수료 추정%' },
        { key: 'perTxFeeAmt', label: '건당수수료(정책×건수)', thClass: 'text-end', columnGuideLabel: '정책 건당요금×거래건수(보조).' },
        { key: 'settlementPerTxFeeAmt', label: '정산건당요금(레거시표시)', thClass: 'text-end', columnGuideLabel: '실행당 정산수수료와 혼동 없도록 정산료 열을 사용하세요.' },
        { key: 'extraFeesAmt', label: '기타%합(거래)', thClass: 'text-end', columnGuideLabel: '거래별 기타 비율 슬롯 합(보조).' }
      ]
    },
    '/calc/compPointMngList': {
      listSortDirAnchor: 'refresh',
      columnGuideFixedKeys: ['rowNo', 'compNm', 'compId', 'curType'],
      paginationSizeOptions: [50, 100, 300, 400, 500],
      paginationDefaultSize: 50,
      noticeList: [
        '「환수금」은 정산이 반영된 뒤(승인 건이 settled 등으로 정산에 올라간 이후) 같은 거래가 환불·취소·무효·차지백 등으로 바뀔 때 정산에서 거둬야 할 금액이 자동으로 잡히는 내역입니다. 금액은 전산설정(환수금 수수료 포함) 및 수수료내역과 동일한 건별 산식입니다. 다음 정산 지급액에서는 환수금(FIFO)을 먼저 차감한 뒤 미수금(FIFO)을 차감합니다. 거래별 산출·검증은 「회수·거래기준」(/settlement/recallMng) 화면을 참고하세요.'
      ],
      searchRows: [
        [
          { label: '등록일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          qdSettleStdField()
        ],
        [
          { label: '업체코드', type: 'text', name: 'searchCompId' },
          { label: '업체명', type: 'text', name: 'searchCompNm' },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수', '환수금액', '잔여'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' },
        { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      columns: [
        { key: '_chk', type: 'checkbox' },
        { key: 'rowNo', label: '번호' },
        { key: 'compNm', label: '업체명' },
        { key: 'compId', label: '업체코드' },
        { key: 'curType', label: '통화' },
        { key: 'trnId', label: '거래ID' },
        { key: 'reasonCode', label: '사유코드' },
        { key: 'recallAmount', label: '환수금액' },
        { key: 'remainingAmount', label: '잔여' },
        { key: 'appliedAmount', label: '정산차감누적' },
        { key: 'status', label: '상태' },
        { key: 'feeIncludedYn', label: '수수료포함' },
        { key: 'vatAppliedYn', label: 'VAT' },
        { key: 'createdAt', label: '등록일시' },
        { key: 'memo', label: '비고' }
      ]
    },
    '/calc/dailyFee': {
      isDailySummaryScreen: true,
      dailySummaryKind: 'fee',
      listSortDirAnchor: 'refresh',
      payMngDenseGrid: true,
      searchFormClass: 'pay-mng-search-form',
      searchRows: [
        [
          { label: '거래일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate', quickdateLabels: ['당일', '당월', '전일', '1주', '2주', '전월'], quickdateRanges: ['day', 'month', 'prevDay', 'week', 'week2', 'prevMonth'] }
        ],
        [
          { label: '검색구분', type: 'select', name: 'searchFieldType', options: [
            { v: 'ALL', t: '전체' },
            { v: 'COMP_NM', t: '업체명' },
            { v: 'COMP_ID', t: '업체코드' },
            { v: 'APPROVAL_NO', t: '승인번호' },
            { v: 'ORDER_NO', t: '주문번호' },
            { v: 'MID', t: 'MID' },
            { v: 'ROUTE', t: '루트' },
            { v: 'CURRENCY', t: '통화' },
            { v: 'STATUS', t: '상태' },
            { v: 'AMOUNT', t: '금액' }
          ], size: 11 },
          { label: '검색어', type: 'text', name: 'searchKeyword', placeholder: '검색어', size: 22 },
          { label: '상태그룹', type: 'select', name: 'searchStatusGroup', options: [
            { v: 'ALL', t: '전체' },
            { v: 'SUCCESS', t: '성공' },
            { v: 'FAIL', t: '실패' },
            { v: 'CANCEL', t: '취소' },
            { v: 'VOID', t: '무효' },
            { v: 'MANUAL_VOID', t: '수동무효' },
            { v: 'REFUND', t: '환불' },
            { v: 'FORCE_REFUND', t: '강제환불' },
            { v: 'EXCLUDE_SUCCESS', t: '성공제외' }
          ], size: 11 },
          { type: 'searchBtn', label: '검색' }
        ]
      ],
      noticeList: [
        '수수료내역과 동일 산식·동일 필터로 일자별 합계를 표시합니다. 정산유무는 해당 일 거래의 settled_yn 이 전부 Y이면 정산완료, 전부 N이면 정산대기, 혼합이면 부분정산입니다.',
        '첫 화면은 집계 부하·게이트웨이 시간 초과(504)를 줄이기 위해 최근 7일(당일 포함)만 자동 조회합니다. 당월·당일 등은 빠른기간 버튼 뒤 [검색]으로 넓히면 됩니다.',
        '일자 행을 더블클릭하면 아래 「선택 일자 상세」에 해당 일 수수료내역 전체가 표시됩니다. 번호·결제시간·승인번호·업체·정산주기·정산예정(업체명 오른쪽)·결제액·수수료·정산액(통화 표기)·상태(한글)·정산유무(거래 settled_yn)를 확인할 수 있으며 거래일 열은 두지 않습니다. 조회 기간은 최대 93일입니다.',
        '미래 일자는 표시되지 않습니다(전산 표시 기준일). 일자 순서는 [내림차순](최신일 위)·[오름차순]으로 바꿀 수 있으며 기본은 내림차순입니다.'
      ],
      summary: ['건수'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'excelDownBtn', label: '엑셀다운로드', cls: 'btn-info' },
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' }
      ],
      tableScrollable: true,
      columns: [
        { key: 'rowNo', label: '번호' },
        { key: 'day', label: '일자' },
        { key: 'txnCount', label: '전체건수' },
        { key: 'txnFixedFeesSum', label: '건당수수료' },
        { key: 'pctFeesSum', label: '결제(%)' },
        { key: 'usdtFee', label: 'USDT' },
        { key: 'fxFee', label: 'FX' },
        { key: 'fee3dsFee', label: '3DS' },
        { key: 'rollingHoldEst', label: '담보추정액' },
        { key: 'failFee', label: '실패' },
        { key: 'cancelFee', label: '취소' },
        { key: 'voidFee', label: '무효' },
        { key: 'manualVoidFee', label: '수무효' },
        { key: 'refundFee', label: '환불' },
        { key: 'chargebackFee', label: '차지백' },
        { key: 'totalFee', label: '총수수료' },
        { key: 'feeVat', label: '부가세' },
        { key: 'expectedPayout', label: '지급예상액' },
        { key: 'settlementAmt', label: '정산액' },
        { key: 'settlementStateLabel', label: '정산유무' }
      ],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/calc/feeList': {
      listSortDirAnchor: 'refresh',
      paginationSizeOptions: [50, 100, 300, 400, 500],
      paginationDefaultSize: 50,
      searchFormClass: 'pay-mng-search-form',
      payMngDenseGrid: true,
      tableScrollable: true,
      /** VIEW SETTING·그리드: 체크·번호·업체명·업체코드·거래일·통화 고정. 정산주기·거래시간·루트·승인번호·거래번호(우리) 등은 토글 */
      columnGuideFixedKeys: ['rowNo', 'compNm', 'compId', 'trnDate', 'curType'],
      viewSettingDefaultSelectedKeys: [
        'trnTime', 'routeNo', 'chillTransactionId', 'trnId', 'statusNm', 'amount', 'payCur', 'curType', 'policyCur', 'calcCycle', 'expectedSettleDate',
        'txnFixedFeesSum', 'pctFeesSum', 'usdtFee', 'fxFee', 'fee3dsFee', 'splitPayPctFee', 'splitPayFixedFee',
        'rollingPctPlain', 'rollingDays', 'rollingHoldEst',
        'failFee', 'cancelFee', 'voidFee', 'manualVoidFee', 'refundFee', 'chargebackFee',
        'totalFee', 'feeVat', 'expectedPayout', 'settlementAmt', 'vatAppliedYn'
      ],
      noticeList: [
        '검색: 첫 줄에서 거래일·빠른기간을 정한 뒤, 둘째 줄에서 검색구분·검색어·상태그룹을 맞추고 오른쪽 [검색]을 누릅니다. 「전체」는 해당 조건으로 좁히지 않습니다. VIEW SETTING에서 열 표시를 켜고 끌 수 있습니다. 앞쪽 열 순서(업체·거래일·거래시간·루트·승인번호·거래번호)는 통합 결제내역 기본과 같습니다. 건당수수료 열은 거래 성공 시 과금되는 성공(건당) 고정액만 표시합니다. 기타수수료: USDT·FX는 승인금액 대비 %(「결제(%)」 합계에 포함), 3DS는 정책통화 기준 건당 고정(합계 열에는 미포함·별도 열). 분할결제 거래는 분할(%)·분할수수료(% 과금액)·분할건당·분할고정(1회차에 분할건×회차수 합산)이 추가되며 총수수료·정산에 포함됩니다. 세 항목은 결제·건당 등과 별도로 동시 과금될 수 있습니다. 금액이 없으면 USDT·FX·3DS·분할 열은 — 입니다. 정산 수수료는 정산 실행 시 1회 과금되며, 송금(이체) 수수료는 그 이후 송금 처리 시 과금되어 정산리포트에 정산 수수료·송금 수수료로 각각 표시됩니다. 이 화면의 총수수료·지급예상에는 정산·송금 건당액이 포함되지 않습니다. 결제(성공): 건당·%(승인 시 부과) 열, 담보(롤링%·추정액), 지급예상액, 정산액(지급예상−담보추정). 실패·취소·무효·환불 등은 상태별 수수료 규칙을 따르며, 무효·환불 계열은 성공 건과 동일한 건당·%가 추가로 과금될 수 있습니다(이중 과금). 차감(취소·환불·무효·실패 등): 지급예상액은 0, 총수수료·부가세는 과금액(양수), 정산액은 −(총수수료+부가세)입니다. 담보 추정은 승인 건에만 표시됩니다. 본사·총판 등은 로그인 조직 하위 가맹점만 조회됩니다.'
      ],
      searchRows: [
        [
          { label: '거래일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate' }
        ],
        [
          { label: '검색구분', type: 'select', name: 'searchFieldType', options: [
            { v: 'ALL', t: '전체' },
            { v: 'COMP_NM', t: '업체명' },
            { v: 'COMP_ID', t: '업체코드' },
            { v: 'APPROVAL_NO', t: '승인번호' },
            { v: 'ORDER_NO', t: '주문번호' },
            { v: 'MID', t: 'MID' },
            { v: 'ROUTE', t: '루트' },
            { v: 'CURRENCY', t: '통화' },
            { v: 'STATUS', t: '상태' },
            { v: 'AMOUNT', t: '금액' }
          ], size: 11 },
          { label: '검색어', type: 'text', name: 'searchKeyword', placeholder: '검색어', size: 22 },
          { label: '상태그룹', type: 'select', name: 'searchStatusGroup', options: [
            { v: 'ALL', t: '전체' },
            { v: 'SUCCESS', t: '성공' },
            { v: 'FAIL', t: '실패' },
            { v: 'CANCEL', t: '취소' },
            { v: 'VOID', t: '무효' },
            { v: 'MANUAL_VOID', t: '수동무효' },
            { v: 'REFUND', t: '환불' },
            { v: 'FORCE_REFUND', t: '강제환불' },
            { v: 'EXCLUDE_SUCCESS', t: '성공제외' }
          ], size: 11 },
          { type: 'searchBtn', label: '검색' }
        ]
      ],
      payListFinancialInline: true,
      summary: ['건수'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' },
        { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      headerGroups: [
        { label: '거래', keys: ['trnDate', 'trnTime', 'routeNo', 'chillTransactionId', 'trnId'] },
        { label: '승인 / 결제수수료(%)', keys: ['txnFixedFeesSum', 'pctFeesSum'] },
        { label: '기타수수료', keys: ['usdtFee', 'fxFee', 'fee3dsFee', 'splitPayPctFee', 'splitPayFixedFee'] },
        { label: '담보(롤링)', keys: ['rollingPctPlain', 'rollingDays', 'rollingHoldEst'] },
        { label: '실패·취소·무효·환불·차지백', keys: ['failFee', 'cancelFee', 'voidFee', 'manualVoidFee', 'refundFee', 'chargebackFee'] },
        { label: '차감·지급', keys: ['totalFee', 'feeVat', 'expectedPayout', 'settlementAmt'] }
      ],
      columns: [
        { key: '_chk', type: 'checkbox' },
        { key: 'rowNo', label: '번호' },
        { key: 'compNm', label: '업체명' },
        { key: 'compId', label: '업체코드' },
        { key: 'calcCycle', label: '정산주기', columnGuideLabel: '가맹 정산설정 정산주기' },
        { key: 'expectedSettleDate', label: '정산예정', thClass: 'text-center text-nowrap', columnGuideLabel: '거래일·정산주기·영업일 기준 예상 정산일(수수료내역 산식)' },
        { key: 'trnDate', label: '거래일' },
        { key: 'trnTime', label: '거래시간' },
        { key: 'routeNo', label: '루트' },
        { key: 'chillTransactionId', label: '승인번호' },
        { key: 'trnId', label: '거래번호(우리)' },
        { key: 'statusNm', label: '상태' },
        { key: 'amount', label: '결제금액' },
        { key: 'payCur', label: '결제통화' },
        { key: 'curType', label: '통화' },
        { key: 'policyCur', label: '정책통화' },
        { key: 'txnFixedFeesSum', label: '건당수수료' },
        { key: 'pctFeesSum', label: '결제(%)' },
        { key: 'usdtFee', label: 'USDT', columnGuideLabel: 'USDT(%) 과금액(승인금액 기준)' },
        { key: 'fxFee', label: 'FX', columnGuideLabel: 'FX(%) 과금액(승인금액 기준)' },
        { key: 'fee3dsFee', label: '3DS', columnGuideLabel: '3DS 건당 고정 과금액' },
        { key: 'splitPayFeePctRate', label: '분할(%)', columnGuideLabel: '분할결제 % 수수료율(계약 스냅샷·정책)' },
        { key: 'splitPayPctFee', label: '분할수수료', columnGuideLabel: '분할결제 % 수수료 과금액(회차별)' },
        { key: 'splitPayFixedPerInst', label: '분할건당', columnGuideLabel: '분할 고정수수료(분할건) 정책 단가' },
        { key: 'splitPayFixedFee', label: '분할고정', columnGuideLabel: '분할 고정수수료 과금액(1회차에 계약 전체 합산)' },
        { key: 'rollingPctPlain', label: '담보율(%)' },
        { key: 'rollingDays', label: '보류일' },
        { key: 'rollingHoldEst', label: '담보추정액' },
        { key: 'failFee', label: '실패' },
        { key: 'cancelFee', label: '취소' },
        { key: 'voidFee', label: '무효' },
        { key: 'manualVoidFee', label: '수무효' },
        { key: 'refundFee', label: '환불' },
        { key: 'chargebackFee', label: '차지백' },
        { key: 'totalFee', label: '총수수료' },
        { key: 'feeVat', label: '부가세' },
        { key: 'expectedPayout', label: '지급예상액' },
        { key: 'settlementAmt', label: '정산액', columnGuideLabel: '정산액' },
        { key: 'vatAppliedYn', label: 'VAT' }
      ]
    },
    '/calc/balanceList': {
      listSortDirAnchor: 'refresh',
      searchRows: [
        [
          { label: '업체코드', type: 'text', name: 'searchCompId' },
          { label: '업체명', type: 'text', name: 'searchCompNm' },
          { label: '검색조건', type: 'select', name: 'searchCondition', options: [{ v: '', t: '전체' }, { v: 'ETC', t: 'ETC' }, { v: '카드', t: '카드' }] },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수', '충전내역합계'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' },
        { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' },
        { id: 'chargeBtn', label: '충전실행', cls: 'btn-success' }
      ],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'condition', label: '검색조건' }, { key: 'chargeType', label: '거래구분' }, { key: 'payMethod', label: '결제수단' }, { key: 'chargeNm', label: '거래명칭' }, { key: 'chargeAmt', label: '충전내역' }, { key: 'sumChargeAmt', label: '충전내역합계' }]
    },
    '/calc/unpaidMng': {
      listSortDirAnchor: 'refresh',
      noticeList: [
        '「미수금」은 해당 정산 주기에 지급해야 할 금액이 부족하거나(정산금 부족), 차지백·과태료 등으로 정산 시 부족분이 생겼을 때 가맹점에 부과되는 금액입니다. 정산 실행 시 지급액에서 환수금(FIFO)을 먼저 차감한 뒤 미수금(FIFO)이 차감됩니다. 정산 지급액이 음수로 확정되면 부족분이 자동 미수로 등록되며(사유코드 AUTO_SETTLEMENT_DEFICIT, 메모에 실행ID), 차기 정산에서 양(+) 지급이 나올 때 FIFO로 먼저 처리됩니다.',
        '「미수금등록」은 총본사·본사·총판 조직 단계의 기본 권한(미수금관리 화면: 수정 이상)으로 가능하며, 지사·대리점·영업점 등은 기본 조회만입니다. 필요 시 본사권한설정에서 조직·단계별로 「미수금관리」(/calc/unpaidMng) 권한을 MODIFY/DELETE 로 올려 수동 등록을 허용할 수 있습니다.',
        '「미수금등록」은 한 창에서 가맹 검색·선택 후 <strong>추가</strong>(신규 미수금) 또는 <strong>차감</strong>(잔여 미수금 FIFO 감소)과 금액·메모를 입력합니다. 목록 행을 더블클릭하면 해당 업체가 선택된 채로 열립니다. API는 POST /api/settlement/receivable 의 direction=ADD|DEDUCT 와 동일하며, 대손·등록 취소는 writeOff·cancel API를 사용합니다.',
        '가맹이 본사설정 「환수/미수금설정」에서 수동(MANUAL)인 경우에만 행의 [환수처리]로 다음 정산 마감 반영을 요청할 수 있습니다. 자동(AUTO) 가맹은 정산 시 미수금이 FIFO로 차감됩니다.'
      ],
      searchRows: [
        [
          { label: '등록일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          qdSettleStdField()
        ],
        [
          { label: '업체코드', type: 'text', name: 'searchCompId' },
          { label: '업체명', type: 'text', name: 'searchCompNm' },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수', '잔액합계', '미수금합계'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' },
        { id: 'receivableRegBtn', label: '미수금등록', cls: 'btn-warning' },
        { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      columns: [
        { key: '_chk', type: 'checkbox' },
        { key: 'rowNo', label: '번호' },
        { key: 'compNm', label: '업체명' },
        { key: 'compId', label: '업체코드' },
        { key: 'curType', label: '통화' },
        { key: 'title', label: '제목' },
        { key: 'totalAmount', label: '총액' },
        { key: 'deductCnt', label: '잔여' },
        { key: 'appliedAmount', label: '정산차감누적' },
        { key: 'deductStatus', label: '상태' },
        { key: 'receivableRecoveryMode', label: '환수모드' },
        { key: 'receivablePhaseNm', label: '미수금처리' },
        { key: 'receivableRecoveryAct', label: '환수' },
        { key: 'reasonCode', label: '사유코드' },
        { key: 'memo', label: '메모', thClass: 'text-nowrap', columnGuideLabel: '정산 지급부족 자동 건은 AUTO_DEFICIT:정산실행ID 로 해당 정산 실행과 연결됩니다.' },
        { key: 'createdAt', label: '등록일시' }
      ]
    },
    '/calc/exCalcList': {
      listSortDirAnchor: 'refresh',
      paginationSizeOptions: [50, 100, 300, 400, 500],
      paginationDefaultSize: 50,
      hasSettlementExecuteDetailTable: true,
      settlementExecuteListModeField: true,
      listToolbarBetweenRefreshAndSort: [
        { idPrefix: 'settlementExecuteRecentModeBtn', label: '최근정산', cls: 'btn-primary settlement-execute-recent-mode-btn' }
      ],
      noticeList: [
        '이 메뉴는 정산방법이 비자동(수동·펌뱅킹 등)인 가맹을 「수동실행」하는 화면입니다. 정산방법이 자동인 가맹은 정산 배치(크론)가 돌며, 목록에는 이력이 보일 수 있으나 행 선택은 비활성입니다. 목록은 정산일(calc_dt)이 정산기간 안에 드는 실행입니다. [수동실행]: 기간 필수·동일 주기·마감·격자 규칙 적용하되 AUTO 가맹은 서버에서 제외됩니다. 검색구분「정산대상일」: 집계 마감일(calc_dt)의 해당 월·일(1~31)만( W7 이면 주간 일요일 마감일). 「정산일」: 배치가 돌았거나 돌 예정인 정산 도래일( W7·영업일 N일 후) 또는 실행 등록일 기준 해당 월·일. 정산구분은 전체·자동·수동. 자동 배치와 동일한 마감·영업일·D0 등 제약이 적용됩니다. 지급 부족 시 미수금 자동등록·환수/FIFO 규칙은 기존과 동일합니다.'
      ],
      searchRows: [
        [
          { label: '정산기간', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate' },
          { label: '검색구분', type: 'select', name: 'searchFieldType', options: [
            { v: 'ALL', t: '전체' },
            { v: 'CALC_CYCLE', t: '정산주기' },
            { v: 'CALC_METHOD', t: '정산방법' },
            { v: 'COMP_NM', t: '업체명' },
            { v: 'COMP_ID', t: '업체코드' },
            { v: 'APPROVAL_NO', t: '승인번호' },
            { v: 'MID', t: 'MID' },
            { v: 'ROUTE', t: '루트' },
            { v: 'CURRENCY', t: '통화' },
            { v: 'STATUS', t: '상태' },
            { v: 'SETTLEMENT_PUBLISH_STS', t: '배포상태' },
            { v: 'PAYOUT_HOLD_YN', t: '지급보류' },
            { v: 'AMOUNT', t: '금액' },
            { v: 'SETTLE_TARGET_DAY', t: '정산대상일' },
            { v: 'SETTLE_RUN_DAY', t: '정산일' }
          ], size: 10 },
          { label: '정산구분', type: 'select', name: 'searchCalcProcType', options: [
            { v: '', t: '전체' },
            { v: 'AUTO', t: '자동' },
            { v: 'MANUAL', t: '수동' }
          ], size: 8 },
          { label: '검색어', type: 'text', name: 'searchKeyword', placeholder: '검색어(일: 1~31)', size: 16 },
          { type: 'searchBtn', label: '검색' }
        ]
      ],
      summary: [],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' },
        { id: 'exCalcBtn', label: '수동실행', cls: 'btn-warning' },
        { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      columnGuideFixedKeys: ['rowNo', 'compNm', 'compId'],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'curType', label: '통화' }, { key: 'txnCnt', label: '건수', thClass: 'text-center text-nowrap', columnGuideLabel: '이번 정산 실행에 집계에 포함된 거래 건수. 컬럼 도입 이전 실행 행은 비어 있을 수 있습니다.' }, { key: 'settlementBatchFee', label: '정산료', thClass: 'text-end', columnGuideLabel: '정산 실행당 1회 정산수수료.' }, { key: 'cadenceGuideKr', label: '노출주기 안내', thClass: 'text-nowrap', columnGuideLabel: '주기별 노출 요약.' }, { key: 'settlementPublishSts', label: '배포상태', columnGuideLabel: 'PENDING·DISTRIBUTED·HOLD — 가맹점정산내역 반영 전 단계.' }, { key: 'payoutHoldYn', label: '지급보류', columnGuideLabel: 'Y면 지급보류 가맹; 배포가 HOLD로 잡힐 수 있음.' }, { key: 'settlementRunId', label: '실행ID', columnGuideLabel: '정산 실행 PK(tb_settlement_run). 추적용.' }, { key: 'receivableAmt', label: '미수금', columnGuideLabel: '정산 지급부족 시 해당 실행에 자동 등록된 미수금(발생액)' }, { key: 'pgRootNo', label: '루트' }, { key: 'settlementCloseDate', label: '정산마감일', thClass: 'text-center text-nowrap', columnGuideLabel: '집계 구간 마감일(예: W7이면 해당 주 마지막 날). 기존 calc_dt와 동일.' }, { key: 'settlementExecDate', label: '정산일자', thClass: 'text-center text-nowrap', columnGuideLabel: '정산주기·영업일 기준 배치(정산) 예정일(W+N 영업일 등).' }, { key: 'calcCycle', label: '정산주기' }, { key: 'calcMethod', label: '정산방법' }, { key: 'targetPeriodText', label: '정산대상기간', thClass: 'pay-grid-time-dual text-start small' }, { key: 'targetAmt', label: '결제액' }, { key: 'totalFee', label: '수수료', columnGuideLabel: '거래 집계만. 정산 실행당 1회 정산수수료는 정산료 열.' }, { key: 'rollingReserveAmt', label: '담보금' }, { key: 'payAmount', label: '지급액', columnGuideLabel: '0으로 보정하지 않음; 부족 시 음수·미수금 자동등록.' }, { key: 'status', label: '확정여부', columnGuideLabel: 'CALCULATED=확정, PENDING=미확정.' }]
    },
    '/settlement/settlementResultDistribute': {
      listSortDirAnchor: 'refresh',
      paginationSizeOptions: [50, 100, 300, 400, 500],
      paginationDefaultSize: 50,
      hasSettlementExecuteDetailTable: true,
      settlementExecuteDetailUiVariant: 'publishDay',
      noticeList: [
        '정산배포: PENDING 만 표시. 과거 DB가 V101 백필로 전부 DISTRIBUTED였다면 운영 DB에 db/V111_settlement_publish_pending_reopen.sql 적용 후 목록이 채워집니다. 처음 열 때 정산기간이 비어 있으면 최근 1년입니다. 행 클릭 시 정산일 당일 해당 가맹 전체 거래를 아래에 표시합니다. 체크 후 배포실행 → DISTRIBUTED, 홀딩실행 → HOLD.'
      ],
      searchRows: [
        [
          { type: 'customHtml', html: '<input type="hidden" name="searchPublishTab" id="searchPublishTab" value="PENDING">', col: 12 },
          { label: '정산기간', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate' },
          { label: '검색구분', type: 'select', name: 'searchFieldType', options: [
            { v: 'ALL', t: '전체' },
            { v: 'CALC_CYCLE', t: '정산주기' },
            { v: 'CALC_METHOD', t: '정산방법' },
            { v: 'COMP_NM', t: '업체명' },
            { v: 'COMP_ID', t: '업체코드' },
            { v: 'APPROVAL_NO', t: '승인번호' },
            { v: 'MID', t: 'MID' },
            { v: 'ROUTE', t: '루트' },
            { v: 'CURRENCY', t: '통화' },
            { v: 'STATUS', t: '상태' },
            { v: 'SETTLEMENT_PUBLISH_STS', t: '배포상태' },
            { v: 'PAYOUT_HOLD_YN', t: '지급보류' },
            { v: 'AMOUNT', t: '금액' },
            { v: 'SETTLE_TARGET_DAY', t: '정산대상일' },
            { v: 'SETTLE_RUN_DAY', t: '정산일' }
          ], size: 10 },
          { label: '검색어', type: 'text', name: 'searchKeyword', placeholder: '검색어(일: 1~31)', size: 14 },
          { type: 'searchBtn', label: '검색' }
        ]
      ],
      summary: [],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' },
        { id: 'settlementPublishDistributeBtn', label: '배포실행', cls: 'btn-success' },
        { id: 'settlementPublishHoldBtn', label: '홀딩실행', cls: 'btn-warning' },
        { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      columnGuideFixedKeys: ['rowNo', 'compNm', 'compId', 'curType'],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'curType', label: '통화' }, { key: 'settlementCloseDate', label: '정산마감일', thClass: 'text-center text-nowrap', columnGuideLabel: '집계 구간 마감일(예: W7이면 해당 주 마지막 날). 기존 calc_dt와 동일.' }, { key: 'settlementExecDate', label: '정산일자', thClass: 'text-center text-nowrap', columnGuideLabel: '정산주기·영업일 기준 배치(정산) 예정일(W+N 영업일 등).' }, { key: 'calcCycle', label: '정산주기' }, { key: 'cadenceGuideKr', label: '노출주기 안내', thClass: 'text-nowrap' }, { key: 'settlementPublishSts', label: '배포상태' }, { key: 'payoutHoldYn', label: '지급보류' }, { key: 'settlementRunId', label: '실행ID', columnGuideLabel: '정산 실행 PK.' }, { key: 'calcMethod', label: '정산방법' }, { key: 'targetPeriodText', label: '정산대상기간', thClass: 'pay-grid-time-dual text-start small' }, { key: 'txnCnt', label: '건수', thClass: 'text-center text-nowrap', columnGuideLabel: '이번 정산 실행에 집계에 포함된 거래 건수. 컬럼 도입 이전 실행 행은 비어 있을 수 있습니다.' }, { key: 'pgRootNo', label: '루트' }, { key: 'targetAmt', label: '결제액' }, { key: 'totalFee', label: '수수료', columnGuideLabel: '거래 집계만. 정산 실행당 1회 정산수수료는 정산료 열.' }, { key: 'rollingReserveAmt', label: '담보금' }, { key: 'settlementBatchFee', label: '정산료', thClass: 'text-end', columnGuideLabel: '정산 실행당 1회 정산수수료.' }, { key: 'receivableAmt', label: '미수금' }, { key: 'payAmount', label: '지급액', columnGuideLabel: '0으로 보정하지 않음; 음수 가능.' }, { key: 'status', label: '확정여부', columnGuideLabel: 'CALCULATED=확정, PENDING=미확정.' }]
    },
    '/settlement/settlementResultHold': {
      listSortDirAnchor: 'refresh',
      paginationSizeOptions: [50, 100, 300, 400, 500],
      paginationDefaultSize: 50,
      noticeList: [
        '정산대기: HOLD — 가맹 정산내역에 안 나감. 처음 열 때 정산기간이 비어 있으면 최근 1년입니다. 해제·배포는 「정산보류내역」 등 운영 절차. 노출 주기 요약은 정산배포와 같습니다.'
      ],
      searchRows: [
        [
          { type: 'customHtml', html: '<input type="hidden" name="searchPublishTab" id="searchPublishTab" value="HOLD">', col: 12 },
          { label: '정산기간', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate' },
          { label: '검색구분', type: 'select', name: 'searchFieldType', options: [
            { v: 'ALL', t: '전체' },
            { v: 'CALC_CYCLE', t: '정산주기' },
            { v: 'CALC_METHOD', t: '정산방법' },
            { v: 'COMP_NM', t: '업체명' },
            { v: 'COMP_ID', t: '업체코드' },
            { v: 'APPROVAL_NO', t: '승인번호' },
            { v: 'MID', t: 'MID' },
            { v: 'ROUTE', t: '루트' },
            { v: 'CURRENCY', t: '통화' },
            { v: 'STATUS', t: '상태' },
            { v: 'SETTLEMENT_PUBLISH_STS', t: '배포상태' },
            { v: 'PAYOUT_HOLD_YN', t: '지급보류' },
            { v: 'AMOUNT', t: '금액' },
            { v: 'SETTLE_TARGET_DAY', t: '정산대상일' },
            { v: 'SETTLE_RUN_DAY', t: '정산일' }
          ], size: 10 },
          { label: '검색어', type: 'text', name: 'searchKeyword', placeholder: '검색어(일: 1~31)', size: 14 },
          { type: 'searchBtn', label: '검색' }
        ]
      ],
      summary: [],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' },
        { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      columnGuideFixedKeys: ['rowNo', 'compNm', 'compId', 'curType'],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'curType', label: '통화' }, { key: 'settlementCloseDate', label: '정산마감일', thClass: 'text-center text-nowrap', columnGuideLabel: '집계 구간 마감일(예: W7이면 해당 주 마지막 날). 기존 calc_dt와 동일.' }, { key: 'settlementExecDate', label: '정산일자', thClass: 'text-center text-nowrap', columnGuideLabel: '정산주기·영업일 기준 배치(정산) 예정일(W+N 영업일 등).' }, { key: 'calcCycle', label: '정산주기' }, { key: 'cadenceGuideKr', label: '노출주기 안내', thClass: 'text-nowrap' }, { key: 'settlementPublishSts', label: '배포상태' }, { key: 'payoutHoldYn', label: '지급보류' }, { key: 'settlementRunId', label: '실행ID', columnGuideLabel: '정산 실행 PK.' }, { key: 'calcMethod', label: '정산방법' }, { key: 'targetPeriodText', label: '정산대상기간', thClass: 'pay-grid-time-dual text-start small' }, { key: 'txnCnt', label: '건수', thClass: 'text-center text-nowrap', columnGuideLabel: '이번 정산 실행에 집계에 포함된 거래 건수. 컬럼 도입 이전 실행 행은 비어 있을 수 있습니다.' }, { key: 'pgRootNo', label: '루트' }, { key: 'targetAmt', label: '결제액' }, { key: 'totalFee', label: '수수료', columnGuideLabel: '거래 집계만. 정산 실행당 1회 정산수수료는 정산료 열.' }, { key: 'rollingReserveAmt', label: '담보금' }, { key: 'settlementBatchFee', label: '정산료', thClass: 'text-end', columnGuideLabel: '정산 실행당 1회 정산수수료.' }, { key: 'receivableAmt', label: '미수금' }, { key: 'payAmount', label: '지급액', columnGuideLabel: '0으로 보정하지 않음; 음수 가능.' }, { key: 'status', label: '확정여부', columnGuideLabel: 'CALCULATED=확정, PENDING=미확정.' }]
    },
    '/calc/settlementReport': {
      listSortDirAnchor: 'refresh',
      paginationSizeOptions: [50, 100, 300, 400, 500],
      paginationDefaultSize: 50,
      tableColumnGuide: true,
      columnGuideFixedKeys: ['_chk', 'rowNo', 'compNm', 'compId', 'curType'],
      viewSettingDefaultSelectedKeys: [
        'settlementRunId', 'settlementCloseDate', 'settlementExecDate', 'targetPeriodText', 'calcCycle', 'txnCnt', 'grossPay', 'refundAmt', 'netPay',
        'rollingReserveAmt', 'mdrFeeAmt', 'perTxnFeeAmt', 'settlementBatchFee', 'feeVat', 'remittanceFee',
        'payAmount', 'settlementDueDt', 'settledYn'
      ],
      hasSettlementExecuteDetailTable: true,
      settlementExecuteDetailUiVariant: 'report',
      noticeList: [
        '[리포트 형식] 가맹점 정산 리포트: 총본사·본사·총판 등이 소속 가맹에 보내는 정산 형식. 본사 지급 리포트: 총본사가 본사(REGIONAL)에 지급할 금액을 본사 단위로 합산(총본사·본사 로그인만 선택 가능).',
        '[하위 구분] 정산집계·정산실시·정산집계표·확정정산(리포트). 정산집계·정산실시·확정정산에서 실행 ID가 있는 행을 클릭하면 하단에 해당 정산 실행에 포함된 거래 목록이 표시됩니다. 집계표(SUM)는 요약 1행만 조회되며, 본사 지급 리포트의 정산실시(EXE)는 합산 행이라 실행 ID가 없을 수 있습니다.',
        '정산집계·정산실시의 비율형 수수료·건당수수료·부가세는 수수료 정책·거래 상태별 수수료내역 계산과 동일 규칙으로 집계합니다. 통화 열은 정책 통화(THB/KRW/USD/JPY 등)입니다.',
        '[배포 기준] 집계(AGG)·실시(EXE)·집계표(SUM)에는 정산배포가 완료된 실행(DISTRIBUTED, 레거시 null 허용)만 포함합니다. 가맹점정산내역·유통 집계와 동일합니다. 확정정산(RST)도 배포·확정된 실행만 표시합니다.'
      ],
      searchRows: [
        [
          { label: '리포트 형식', type: 'select', name: 'searchReportKind', options: [{ v: 'MERCHANT_STMT', t: '가맹점 정산 리포트' }, { v: 'REGIONAL_PAYOUT', t: '본사 지급 리포트(총본사→본사)' }], size: 22 },
          { label: '리포트구분', type: 'select', name: 'searchReportSub', options: [{ v: 'AGG', t: '정산집계' }, { v: 'EXE', t: '정산실시' }, { v: 'SUM', t: '정산집계표' }, { v: 'RST', t: '확정정산(리포트)' }], size: 14 },
          { label: '정산대상기간', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate' }
        ],
        [
          { label: '가맹점코드', type: 'text', name: 'searchCompId', placeholder: '가맹점 코드', i18nLblKey: 'searchSettlementReportMerchLbl', i18nPhKey: 'searchSettlementReportCompPh' },
          { label: '총판(상위)코드', type: 'text', name: 'searchMasterId', placeholder: '총판 조직 코드' },
          { label: '본사코드', type: 'text', name: 'searchRegionalId', placeholder: '본사 지급 리포트 시 필터' },
          { label: '통화', type: 'select', name: 'searchCurType', options: [{ v: '', t: '전체' }, { v: 'KRW', t: 'KRW' }, { v: 'USD', t: 'USD' }, { v: 'JPY', t: 'JPY' }, { v: 'THB', t: 'THB' }], size: 8 },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수', '결제액', '환불/취소', '정산액', '지급액'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' },
        { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      columns: [],
      columnsBySub: {
        AGG: [
          { key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' },
          { key: 'settlementRunId', label: '실행ID', columnGuideLabel: '정산 실행 PK. 하단 거래 목록에 사용.' },
          { key: 'targetPeriodText', label: '정산대상기간', thClass: 'pay-grid-time-dual text-start small' },
          { key: 'settlementCloseDate', label: '정산마감일', thClass: 'text-center text-nowrap' }, { key: 'settlementExecDate', label: '정산일자', thClass: 'text-center text-nowrap' }, { key: 'calcCycle', label: '정산주기', thClass: 'text-center text-nowrap small' },
          { key: 'compNm', label: '가맹점명' }, { key: 'compId', label: '가맹코드' }, { key: 'curType', label: '통화', columnGuideLabel: '정책 통화(THB/KRW/USD/JPY 등).' },
          { key: 'txnCnt', label: '거래건수', thClass: 'text-center text-nowrap', columnGuideLabel: '정산 실행에 집계 포함된 거래 건수 합(tb_settlement_run.included_txn_cnt).' },
          { key: 'grossPay', label: '결제액' }, { key: 'refundAmt', label: '환불/취소' }, { key: 'netPay', label: '정산액', columnGuideLabel: '결제액 − 환불/취소(실행 저장 순매출).' },
          { key: 'rollingReserveAmt', label: '보증금', columnGuideLabel: '롤링(담보) 보류액(실행 저장값).' },
          { key: 'mdrFeeAmt', label: '수수료', columnGuideLabel: '결제%·USDT·FX 등 비율형 수수료 합(수수료내역과 동일 규칙).' },
          { key: 'perTxnFeeAmt', label: '건당수수료', columnGuideLabel: '건당·고정·취소/무효/환불 등 MDR 외 수수료 합.' },
          { key: 'settlementBatchFee', label: '정산 수수료', thClass: 'text-end', columnGuideLabel: '정산 실행당 1회 정산수수료.' },
          { key: 'feeVat', label: 'VAT', thClass: 'text-end', columnGuideLabel: '거래수수료+정산수수료 기준 부가세(가맹 설정).' },
          { key: 'remittanceFee', label: '송금 수수료', thClass: 'text-end', columnGuideLabel: '이체 또는 USDT 송금 차감액. 목록에는 금액만 표시하며, 최종 지급액·집계는 기존과 같이 차감 반영.' },
          { key: 'totalFee', label: '거래수수료합', columnGuideLabel: '실행에 저장된 거래 수수료 합계.' },
          { key: 'payAmount', label: '지급액', columnGuideLabel: '송금 수수료 차감 전 지급액(실행 저장).' },
          { key: 'finalPayAfterRemittance', label: '최종 지급액', thClass: 'text-end fw-semibold' },
          { key: 'settlementDueDt', label: '지급예정일', columnGuideLabel: '정산주기·영업일 기준 배치(정산) 예정일(정산일자 열과 동일).' },
          { key: 'settledYn', label: '정산완료' }, { key: 'status', label: '상태' }
        ],
        EXE: [
          { key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' },
          { key: 'settlementRunId', label: '실행ID' },
          { key: 'settlementCloseDate', label: '정산마감일', thClass: 'text-center text-nowrap' }, { key: 'settlementExecDate', label: '정산일자', thClass: 'text-center text-nowrap' }, { key: 'targetPeriodText', label: '정산대상기간', thClass: 'pay-grid-time-dual text-start small' },
          { key: 'calcCycle', label: '정산주기', thClass: 'text-center text-nowrap small' },
          { key: 'compNm', label: '가맹점명' }, { key: 'compId', label: '가맹코드' }, { key: 'curType', label: '통화' },
          { key: 'txnCnt', label: '거래건수', thClass: 'text-center text-nowrap', columnGuideLabel: '정산 실행에 집계 포함된 거래 건수.' },
          { key: 'approveAmt', label: '결제액' }, { key: 'cancelAmt', label: '환불/취소' }, { key: 'netPay', label: '정산액' },
          { key: 'rollingReserveAmt', label: '보증금' },
          { key: 'mdrFeeAmt', label: '수수료' }, { key: 'perTxnFeeAmt', label: '건당수수료' },
          { key: 'settlementBatchFee', label: '정산 수수료', thClass: 'text-end' },
          { key: 'feeVat', label: 'VAT', thClass: 'text-end' },
          { key: 'remittanceFee', label: '송금 수수료', thClass: 'text-end' },
          { key: 'payAmount', label: '지급액(송금 전)', columnGuideLabel: '송금 수수료 차감 전.' },
          { key: 'totalFee', label: '거래수수료합', columnGuideLabel: '실행 저장 거래 수수료 합.' },
          { key: 'remittanceFeeBank', label: '송금수수료(통화)', thClass: 'text-end', columnGuideLabel: '통화 이체 송금 건. USDT 가맹은 비움.' },
          { key: 'remittanceFeeUsdt', label: '송금(USDT) 수수료', thClass: 'text-end' },
          { key: 'finalPayAfterRemittance', label: '최종 지급액', thClass: 'text-end fw-semibold' },
          { key: 'receivableRecoveryModeKr', label: '미수금처리', thClass: 'text-nowrap small' },
          { key: 'receivableAppliedAmt', label: '미수금차감', thClass: 'text-end' },
          { key: 'settlementDueDt', label: '지급예정일' }, { key: 'settledYn', label: '완료' }, { key: 'status', label: '상태' }
        ],
        SUM: [
          { key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' },
          { key: 'periodFrom', label: '기간FROM' }, { key: 'periodTo', label: '기간TO' }, { key: 'curType', label: '통화' },
          { key: 'grossPay', label: '결제액합' }, { key: 'refundAmt', label: '환불/취소합' }, { key: 'netPay', label: '정산액합' },
          { key: 'rollingReserveAmt', label: '보증금합' }, { key: 'mdrFeeAmt', label: '수수료합' }, { key: 'perTxnFeeAmt', label: '건당수수료합' },
          { key: 'settlementBatchFee', label: '정산수수료합' }, { key: 'feeVat', label: 'VAT합' }, { key: 'remittanceFee', label: '송금수수료합' },
          { key: 'payAmount', label: '지급액합' }, { key: 'settlementAmt', label: '정산금합', columnGuideLabel: '집계표상 지급액 합과 동일하게 표시.' },
          { key: 'txnCnt', label: '거래건수', columnGuideLabel: '기간 내 실행 행의 포함 거래 건수 합.' }, { key: 'refundCnt', label: '환불건' }, { key: 'rowCount', label: '집계행수' }
        ],
        RST: [
          { key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' },
          { key: 'calcDt', label: '정산일시' }, { key: 'settlementCloseDate', label: '정산마감일', thClass: 'text-center text-nowrap', columnGuideLabel: '집계 구간 마감일.' }, { key: 'settlementExecDate', label: '정산일자', thClass: 'text-center text-nowrap', columnGuideLabel: '정산주기·영업일 기준 배치 예정일.' }, { key: 'compNm', label: '가맹점명' }, { key: 'compId', label: '가맹코드' }, { key: 'curType', label: '통화' },
          { key: 'targetPeriodText', label: '정산대상기간' }, { key: 'calcCycle', label: '정산주기' },
          { key: 'approveAmt', label: '결제액' }, { key: 'cancelAmt', label: '환불/취소' }, { key: 'netPay', label: '정산액' },
          { key: 'feeAmt', label: '거래수수료' }, { key: 'holdAmt', label: '보증금' },
          { key: 'settlementBatchFee', label: '정산 수수료', thClass: 'text-end' },
          { key: 'payAmount', label: '지급액(송금 전)', columnGuideLabel: '송금 수수료 차감 전.' },
          { key: 'remittanceFeeBank', label: '송금수수료', thClass: 'text-end' },
          { key: 'remittanceFeeUsdt', label: '송금(USDT) 수수료', thClass: 'text-end' },
          { key: 'finalPayAfterRemittance', label: '최종 지급액', thClass: 'text-end fw-semibold' },
          { key: 'payStatus', label: '상태' }
        ]
      },
      columnsRegionalPayout: {
        AGG: [
          { key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' },
          { key: 'settlementRunId', label: '실행ID' },
          { key: 'targetPeriodText', label: '정산대상기간', thClass: 'pay-grid-time-dual text-start small' },
          { key: 'settlementCloseDate', label: '정산마감일', thClass: 'text-center text-nowrap' }, { key: 'settlementExecDate', label: '정산일자', thClass: 'text-center text-nowrap' }, { key: 'calcCycle', label: '정산주기', thClass: 'text-center text-nowrap small' },
          { key: 'regionalCompId', label: '본사코드' }, { key: 'regionalNm', label: '본사명' },
          { key: 'compNm', label: '가맹점명' }, { key: 'compId', label: '가맹코드' }, { key: 'merchantCnt', label: '가맹수', columnGuideLabel: '실행당 1.' }, { key: 'curType', label: '통화' },
          { key: 'grossPay', label: '결제액' }, { key: 'refundAmt', label: '환불/취소' }, { key: 'netPay', label: '정산액' },
          { key: 'rollingReserveAmt', label: '보증금' },
          { key: 'mdrFeeAmt', label: '수수료' }, { key: 'perTxnFeeAmt', label: '건당수수료' },
          { key: 'settlementBatchFee', label: '정산 수수료' }, { key: 'feeVat', label: 'VAT' }, { key: 'remittanceFee', label: '송금 수수료' },
          { key: 'payAmount', label: '지급액' }, { key: 'finalPayAfterRemittance', label: '최종 지급액' },
          { key: 'settlementDueDt', label: '지급예정일' }, { key: 'settledYn', label: '정산완료' }
        ],
        EXE: [
          { key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' },
          { key: 'settlementCloseDate', label: '정산마감일', thClass: 'text-center text-nowrap' }, { key: 'settlementExecDate', label: '정산일자', thClass: 'text-center text-nowrap' }, { key: 'calcCycle', label: '정산주기', thClass: 'text-center text-nowrap small' },
          { key: 'regionalCompId', label: '본사코드' }, { key: 'regionalNm', label: '본사명' }, { key: 'curType', label: '통화' },
          { key: 'batchRunCnt', label: '배치건수' }, { key: 'approveAmt', label: '결제액합' }, { key: 'cancelAmt', label: '환불/취소합' }, { key: 'netPay', label: '정산액합' },
          { key: 'rollingReserveAmt', label: '보증금합' },
          { key: 'mdrFeeAmt', label: '수수료합' }, { key: 'perTxnFeeAmt', label: '건당수수료합' },
          { key: 'settlementBatchFee', label: '정산수수료합' }, { key: 'feeVat', label: 'VAT합' }, { key: 'remittanceFee', label: '송금수수료합' },
          { key: 'payAmount', label: '지급액합(송금 전)' }, { key: 'totalFee', label: '거래수수료합' },
          { key: 'remittanceFeeBank', label: '송금수수료합(통화)', thClass: 'text-end' },
          { key: 'remittanceFeeUsdt', label: '송금(USDT) 수수료합', thClass: 'text-end' },
          { key: 'finalPayAfterRemittance', label: '최종 지급액합', thClass: 'text-end' },
          { key: 'receivableRecoveryModeKr', label: '미수금처리', thClass: 'text-nowrap small' },
          { key: 'receivableAppliedAmt', label: '미수금차감합', thClass: 'text-end' },
          { key: 'settlementDueDt', label: '지급예정일' }, { key: 'settledYn', label: '완료' }, { key: 'status', label: '상태' }
        ],
        SUM: [
          { key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' },
          { key: 'periodFrom', label: '기간FROM' }, { key: 'periodTo', label: '기간TO' }, { key: 'curType', label: '통화' },
          { key: 'grossPay', label: '결제액합' }, { key: 'refundAmt', label: '환불/취소합' }, { key: 'netPay', label: '정산액합' },
          { key: 'rollingReserveAmt', label: '보증금합' }, { key: 'mdrFeeAmt', label: '수수료합' }, { key: 'perTxnFeeAmt', label: '건당수수료합' },
          { key: 'settlementBatchFee', label: '정산수수료합' }, { key: 'feeVat', label: 'VAT합' }, { key: 'remittanceFee', label: '송금수수료합' },
          { key: 'payAmount', label: '지급액합' }, { key: 'settlementAmt', label: '정산금합' }, { key: 'txnCnt', label: '거래건수', columnGuideLabel: '기간 내 실행 행의 포함 거래 건수 합.' }, { key: 'refundCnt', label: '환불건' }, { key: 'rowCount', label: '집계행수' }
        ]
      },
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/calc/collateralList': {
      listSortDirAnchor: 'refresh',
      paginationSizeOptions: [50, 100, 300, 400, 500],
      paginationDefaultSize: 50,
      columnGuideFixedKeys: ['rowNo', 'compNm', 'compId', 'curType'],
      viewSettingDefaultSelectedKeys: [
        'trnId', 'reserveAmt', 'rollingPct', 'holdBusinessDays', 'holdStartDt',
        'releaseBizDtTime', 'remainingBizDays', 'routeNo', 'statusNm', 'releasedAt', 'settlementNote'
      ],
      noticeList: [
        '담보금(롤링): 결제(승인) 건별로 정산 실행 시 설정된 비율(%)만큼 예치되며, 보류 영업일(주말 제외·공휴일 미반영) 후 해지일에 정산 실행하면 지급액에 합산됩니다.',
        '비율·보류 일수: 본사설정 수수료정책의 롤링(담보금) 또는 가맹점 정산설정에서 「보류율 본사정책 따름=N」일 때 개별 보류율·일수를 사용합니다.',
        '해제일시·남은일자는 영업일 기준입니다. 루트는 해당 거래의 결제 루트(route_no)입니다.'
      ],
      searchRows: [
        [
          { label: '적용일(담보)', type: 'daterange', from: 'searchFromDate', to: 'searchToDate', col: 5 },
          qdSettleStdField()
        ],
        [
          { label: '검색구분', type: 'select', name: 'searchFieldType', options: [
            { v: 'ALL', t: '전체' },
            { v: 'COMP_NM', t: '업체명' },
            { v: 'COMP_ID', t: '업체코드' },
            { v: 'SETTLE_TARGET_DAY', t: '정산대상일' },
            { v: 'SETTLE_RUN_DAY', t: '정산일' }
          ], size: 10 },
          { label: '검색어', type: 'text', name: 'searchKeyword', placeholder: '검색어(일: 1~31)', col: 3 },
          { label: '상태', type: 'select', name: 'searchStatus', options: [{ v: '', t: '전체' }, { v: 'HOLD', t: '보류' }, { v: 'RELEASED', t: '해지' }], col: 2 },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수', '담보금액'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' },
        { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      columns: [
        { key: '_chk', type: 'checkbox' },
        { key: 'rowNo', label: '번호' },
        { key: 'compNm', label: '업체명' },
        { key: 'compId', label: '업체코드' },
        { key: 'curType', label: '통화' },
        { key: 'trnId', label: '거래ID' },
        { key: 'reserveAmt', label: '담보금액' },
        { key: 'rollingPct', label: '적용비율(%)' },
        { key: 'holdBusinessDays', label: '보류영업일' },
        { key: 'holdStartDt', label: '적용일' },
        { key: 'releaseDt', label: '해지(반환)일' },
        {
          key: 'releaseBizDtTime',
          label: '해제일시',
          columnGuideLabel: '보류: 영업일 기준 해제 예정일 00:00 표기. 해지: 정산 반영 처리 시각.'
        },
        { key: 'remainingBizDays', label: '남은일자(영업일)', columnGuideLabel: '오늘부터 해제 예정일까지 남은 영업일(주말 제외).' },
        { key: 'routeNo', label: '루트' },
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
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'urlType', label: 'URL구분' }, { key: 'targetUrl', label: '통보URL' }, { key: 'sendDt', label: '전송일시' }, { key: 'result', label: '결과' }, { key: 'retryCnt', label: '재전송횟수' }, { key: 'webhookPayloadPreview', label: '웹훅 본문' }, { key: 'orderNo', label: '주문번호' }, { key: 'trnId', label: '거래번호' }]
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
      searchRows: [
        [
          { label: '사용자 ID', type: 'text', name: 'searchUserId' },
          { label: '사용자명', type: 'text', name: 'searchUserNm' },
          { label: '업체코드', type: 'text', name: 'searchCompId' },
          {
            label: '사용여부',
            type: 'select',
            name: 'searchUseStatus',
            options: [
              { v: '', t: '전체' },
              { v: 'ACTIVE', t: '사용' },
              { v: 'INACTIVE', t: '미사용' },
              { v: 'SUSPENDED', t: '영구정지' }
            ]
          },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수'],
      buttons: [
        { id: 'addBtn', label: '추가', cls: 'btn-outline-secondary' },
        { id: 'saveBtn', label: '저장', cls: 'btn-primary' }
      ],
      columns: [
        { key: 'rowNo', label: 'No.' },
        { key: 'compId', label: '업체코드' },
        { key: 'compNm', label: '업체명' },
        { key: 'userId', label: '사용자ID*', type: 'userMngUserId' },
        { key: 'userNm', label: '사용자명*', type: 'userMngUserNm' },
        { key: 'mobile', label: '연락처*', type: 'userMngMobile' },
        { key: 'permissionGroupNm', label: '권한그룹*', type: 'userMngAssistantRole' },
        { key: 'roleNm', label: '역할', type: 'userMngRoleNm' },
        { key: '_pwd', label: '비밀번호', type: 'userMngPassword' },
        { key: '_otpAct', label: 'OTP', type: 'userMngOtp' },
        { key: 'userStatus', label: '사용여부*', type: 'userMngStatus' },
        { key: '_del', label: '삭제', type: 'userMngDraftDelete' },
        { key: 'inactiveReason', label: '전환사유', type: 'userMngInactiveReason' }
      ]
    },
    '/set/gridSetMng': {
      searchRows: [
        [
          { label: '메뉴 선택', type: 'select', name: 'searchMenuId', options: [{ v: '', t: '선택' }, { v: 'M0301', t: '결제내역' }, { v: 'M0404', t: '유통망정산내역' }] },
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
      searchRows: [[{ label: '업체코드', type: 'text', name: 'searchCompId' }, { label: '업체명(본사명)', type: 'text', name: 'searchCompNm', i18nLblKey: 'searchCompNmHqLabel' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }, { id: 'compInfoDetailBtn', label: '상세(지역본사정보)', cls: 'btn-info' }, { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명(본사명)' }, { key: 'compId', label: '업체코드' }, { key: 'compDivNm', label: '업체구분' }, { key: 'regNo', label: '사업자번호' }, { key: 'regDt', label: '등록일' }],
      emptyMessage: '조회된 데이터가 없습니다.',
      hasCompInfoDetailForm: true,
      compInfoDetailFormSections: [
        {
          title: '업체 정보 상세 (업체정보조회)',
          notice: '그리드에서 한 건 선택 후 [상세] 버튼으로 조회·수정합니다.',
          rows: [
            [{ label: '업체코드', type: 'text', name: 'compId', col: 2, readonly: true }, { label: '업체구분', type: 'select', name: 'compDiv', options: [{ v: '', t: '선택' }, { v: 'HEADQUARTERS', t: '총본사' }, { v: 'REGIONAL', t: '본사' }, { v: 'MASTER_DIST', t: '총판' }, { v: 'BRANCH', t: '지사' }, { v: 'AGENCY', t: '대리점' }, { v: 'SALES_OFFICE', t: '영업점' }, { v: 'MERCHANT', t: '가맹점' }], col: 2 }],
            [{ label: '업체명(본사명)*', type: 'text', name: 'compNm', col: 2 }, { label: '사업자번호*', type: 'regNoWithType', name: 'regNo', col: 2 }],
            [{ label: '업태', type: 'text', name: 'bizType', col: 2 }, { label: '종목', type: 'text', name: 'industry', col: 2 }],
            [{ label: '대표자명*', type: 'text', name: 'ceoNm', col: 2 }, { label: '휴대폰*', type: 'text', name: 'ceoMobile', col: 2 }, { label: '업체전화*', type: 'text', name: 'compTel', col: 2 }, { label: '팩스', type: 'text', name: 'fax', col: 2 }, { label: '이메일', type: 'text', name: 'email', col: 2 }, { label: '비고', type: 'text', name: 'remark', col: 2 }],
            [{ type: 'countryAddressRow', zipLabel: '우편번호*', addrLabel: '주소*', addrDetailLabel: '상세주소', addrEtcLabel: '기타' }],
            [{ label: '사용여부*', type: 'select', name: 'useYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }, { v: 'S', t: '영구정지' }], col: 2 }, { label: '태블릿 UI 기능', type: 'select', name: 'tabletFeatureUseYn', options: [{ v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 }, { label: '대표 아이디 (중복검사)', type: 'text', name: 'loginId', col: 2, button: '중복확인' }, { label: '비밀번호', type: 'passwordReset', name: 'pwdReset', col: 2 }],
            [{ label: '사업자형태', type: 'text', name: 'bizNature', col: 2 }, { label: '취급물품', type: 'text', name: 'product', col: 2 }],
            [{ label: '대표사이트', type: 'text', name: 'homepage', col: 2 }, { label: '정산담당자명', type: 'text', name: 'settleName', col: 2 }],
            [{ label: '정산담당자연락처', type: 'text', name: 'settleTelNo', col: 2 }],
            [{ label: '계좌은행', type: 'select', name: 'bankCd', options: [{ v: '', t: '선택' }, { v: '04', t: '국민' }, { v: '20', t: '우리' }, { v: '81', t: 'KEB하나' }, { v: '88', t: '신한' }, { v: '11', t: 'NH농협' }], col: 2 }, { label: '이체수수료(기준화폐)', type: 'text', name: 'transferFee', col: 2 }],
            [{ label: '계좌번호*', type: 'text', name: 'accountNo', col: 2 }, { label: '예금주*', type: 'text', name: 'accountHolder', col: 2 }],
            [{ label: '수수료 설정 권한', type: 'select', name: 'commissionConfigAllowed', options: [{ v: 'N', t: '미부여' }, { v: 'Y', t: '부여' }], col: 2 }, { label: '기준 화폐1', type: 'select', name: 'baseCurrency1', options: [{ v: '', t: '선택' }, { v: 'KRW', t: 'KRW (원)' }, { v: 'USD', t: 'USD (달러)' }, { v: 'JPY', t: 'JPY (엔)' }, { v: 'THB', t: 'THB (바트)' }, { v: 'EUR', t: 'EUR (유로)' }], col: 2 }, { label: '기준 화폐2', type: 'select', name: 'baseCurrency2', options: [{ v: '', t: '선택' }, { v: 'KRW', t: 'KRW (원)' }, { v: 'USD', t: 'USD (달러)' }, { v: 'JPY', t: 'JPY (엔)' }, { v: 'THB', t: 'THB (바트)' }, { v: 'EUR', t: 'EUR (유로)' }], col: 2 }, { label: '기준 화폐3', type: 'select', name: 'baseCurrency3', options: [{ v: '', t: '선택' }, { v: 'KRW', t: 'KRW (원)' }, { v: 'USD', t: 'USD (달러)' }, { v: 'JPY', t: 'JPY (엔)' }, { v: 'THB', t: 'THB (바트)' }, { v: 'EUR', t: 'EUR (유로)' }], col: 2 }]
          ]
        },
        {
          title: '무효·환불 정산 (안내)',
          id: 'voidRefundSettleGuideCard',
          merchantOnly: true,
          cardExtraClass: 'pg-comp-reg-void-refund-panel',
          rows: [
            [{ type: 'customHtml', col: 12, html: merchantVoidRefundGuideHtml }],
            [{ label: '무효 정산(21·40)', type: 'select', name: 'voidSettlementMode', options: VOID_REFUND_SETTLE_MODE_OPTIONS.concat([{ v: 'FOLLOW', t: '총판·본사 따름' }]), col: 3, voidRefundSettlementModeField: true }, { label: '수동무효 정산(22·41)', type: 'select', name: 'manualVoidSettlementMode', options: VOID_REFUND_SETTLE_MODE_OPTIONS.concat([{ v: 'FOLLOW', t: '총판·본사 따름' }]), col: 3, voidRefundSettlementModeField: true }, { label: '환불 정산(30·42)', type: 'select', name: 'refundSettlementMode', options: VOID_REFUND_SETTLE_MODE_OPTIONS.concat([{ v: 'FOLLOW', t: '총판·본사 따름' }]), col: 3, voidRefundSettlementModeField: true }, { label: '강제환불(31)', type: 'select', name: 'forceRefundSettlementMode', options: VOID_REFUND_SETTLE_MODE_OPTIONS.concat([{ v: 'FOLLOW', t: '총판·본사 따름' }]), col: 3, voidRefundSettlementModeField: true }]
          ]
        },
        {
          title: '수수료정책',
          id: 'commissionPolicyCard',
          merchantRegionalMasterCommission: true,
          notice: '본사정책 따름이면 [본사 정책선택]에서 사용합니다. 목록에는 배포(Y)인 템플릿만 나오며, 가맹점 기준통화와 정책 통화코드가 같거나 정책 통화가 비어 있는 항목만 표시됩니다. 본사·총판·가맹점에 동일하게 적용·저장됩니다. 첫 항목(본사 기본 템플릿)은 선택값이 비어 있을 때 본사의 기본(DEFAULT) 수수료 템플릿을 씁니다.',
          rows: [
            [{ label: '본사정책 따름', type: 'select', name: 'commissionFollowHq', options: [{ v: 'Y', t: '본사정책 따름' }, { v: 'N', t: '직접입력' }], col: 2 }, { label: '본사 정책선택', type: 'select', name: 'hqPolicyScope', options: [{ v: '', t: '본사 기본 템플릿 (DEFAULT)' }], col: 2, hqPolicyOnly: true }],
            [{ label: '결제수수료율(%)', type: 'text', name: 'payRate', col: 2, customOnly: true }, { label: '실패수수료(건)', type: 'text', name: 'failFee', col: 2, customOnly: true }, { label: '취소수수료(건)', type: 'text', name: 'cancelRate', col: 2, customOnly: true }],
            [{ label: '무효수수료(건)', type: 'text', name: 'voidFeePerTx', col: 2, customOnly: true, placeholder: '거래 21' }, { label: '수무효수수료(건)', type: 'text', name: 'manualVoidFeePerTx', col: 2, customOnly: true, placeholder: '거래 22' }, { label: '환불수수료(건)', type: 'text', name: 'refundRate', col: 2, customOnly: true }],
            [{ label: '월간이용료(월 1회·고정)', type: 'text', name: 'usageRate', col: 2, customOnly: true, placeholder: '통화코드 단위 금액' }, { label: '비고', type: 'text', name: 'commissionMemo', col: 2, customOnly: true }],
            [{ label: '정산수수료(건)', type: 'text', name: 'feeSettlementPerTx', col: 2, customOnly: true }, { label: '송금수수료', type: 'text', name: 'remittanceTransferFee', col: 2, customOnly: true }, { label: 'USDT 송금수수료(건)', type: 'text', name: 'usdtTransferFeeUsd', col: 2, customOnly: true }, { label: 'USDT수수료율(%)', type: 'text', name: 'feeUsdt', col: 2, customOnly: true }, { label: 'FX수수료율(%)', type: 'text', name: 'feeFx', col: 2, customOnly: true }, { label: '3DS 고정(건)', type: 'text', name: 'fee3dsRate', col: 2, customOnly: true }]
          ]
        },
        {
          title: '차지백 정책',
          id: 'chargebackPolicyCard',
          merchantOnly: true,
          notice: '본사정책 따름이면 위에서 고른 본사 정책 템플릿의 3DS·차지백 설정이 적용됩니다. 직접입력일 때만 아래를 저장할 수 있습니다.',
          rows: [
            [{ label: '차지백수수료(건)', type: 'text', name: 'chargebackFeePerTx', col: 2, customOnly: true }, { label: '차지백 구간정책', type: 'select', name: 'chargebackPolicyId', col: 4, options: [{ v: '', t: '(미사용) 건당 차지백만' }] }]
          ]
        },
        {
          title: '결제 후속조치 (가맹점 관리자)',
          id: 'payFollowMerchantCard',
          merchantOnly: true,
          notice: '관리자 화면의 자동무효·이메일무효·자동환불·강제환불 사용 여부입니다. 전산설정관리(전역) 및 본사권한설정의 조직 단계 상한과 함께 적용됩니다. [기본·종전]은 미설정과 동일(허용으로 해석)입니다.',
          rows: [
            [{ label: '후속조치 사용', type: 'select', name: 'payFollowMerchantUseYn', options: [{ v: '', t: '기본·종전과 동일' }, { v: 'Y', t: '사용' }, { v: 'N', t: '미사용' }], col: 2 }],
            [{ label: '자동무효', type: 'select', name: 'payFollowAutoVoidYn', options: [{ v: '', t: '기본·종전과 동일' }, { v: 'Y', t: '허용' }, { v: 'N', t: '불가' }], col: 2 }, { label: '이메일 무효', type: 'select', name: 'payFollowEmailVoidYn', options: [{ v: '', t: '기본·종전과 동일' }, { v: 'Y', t: '허용' }, { v: 'N', t: '불가' }], col: 2 }, { label: '자동환불', type: 'select', name: 'payFollowAutoRefundYn', options: [{ v: '', t: '기본·종전과 동일' }, { v: 'Y', t: '허용' }, { v: 'N', t: '불가' }], col: 2 }, { label: '강제환불', type: 'select', name: 'payFollowForceRefundYn', options: [{ v: '', t: '기본·종전과 동일' }, { v: 'Y', t: '허용' }, { v: 'N', t: '불가' }], col: 2 }]
          ]
        },
        {
          title: 'URL·챗봇 결제 승인 알림',
          id: 'urlPaySuccessAlertCard',
          merchantOnly: true,
          notice: '인라인 DirectCredit(URL·챗봇) 승인 시 PG중계 JSON 전송과 함께 LINE Notify·대표 이메일(전산 SMTP) 알림을 보낼 수 있습니다. 토큰은 비우면 기존 유지, 삭제는 __CLEAR__.',
          rows: [
            [{ label: '승인 알림메일', type: 'select', name: 'urlPayAlertEmailYn', options: [{ v: 'N', t: '미사용' }, { v: 'Y', t: '사용 (대표 이메일)' }], col: 2 }],
            [{ label: 'LINE Notify 토큰', type: 'password', name: 'urlPayLineNotifyToken', col: 6, placeholder: '변경 시만 입력 · 삭제: __CLEAR__' }]
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
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compId', label: '업체코드' }, { key: 'compNm', label: '업체명' }, { key: 'compDivNm', label: '업체구분' }, { key: 'regDt', label: '등록일' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    /** 업체변경이력 — compInfoHistList 와 동일(별칭 URL) */
    '/comp/compChangeHistory': {
      searchRows: [
        [
          { label: '접속일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate' },
          { label: '업체명', type: 'text', name: 'searchCompNm', placeholder: '업체명·업체코드', i18nPhKey: 'searchCompQ' },
          { label: '변경자명', type: 'text', name: 'searchChangedBy' },
          { type: 'searchBtn' }
        ]
      ],
      summary: ['건수'],
      buttons: [],
      columns: [
        { key: 'rowNo', label: 'No.' },
        { key: 'chgDt', label: '변경일시' },
        { key: 'compId', label: '업체코드' },
        { key: 'compNm', label: '업체명' },
        { key: 'chgTarget', label: '변경대상' },
        { key: 'chgBefore', label: '변경 전' },
        { key: 'chgAfter', label: '변경 후' },
        { key: 'changedBy', label: '변경자' }
      ],
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
      listSortDirAnchor: 'refresh',
      searchRows: [[{ label: '정산일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수', '정산금액', '수수료', '지급액'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' },
        { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      columns: [],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/settlement/franchiseList': {
      listSortDirAnchor: 'refresh',
      searchRows: [[{ label: '정산일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수', '금액', '수수료금액', '수수료부가세', '보류금액', '미수금', '정산금액'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' },
        { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      columns: [],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/settlement/recallMng': {
      listSortDirAnchor: 'refresh',
      searchRows: [[{ label: '조회일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' }, { type: 'quickdate' }, { type: 'searchBtn' }]],
      summary: ['건수'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' },
        { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      columnGuideFixedKeys: ['rowNo', 'compNm', 'compId', 'curType'],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'curType', label: '통화' }, { key: 'calcDt', label: '정산일자' }, { key: 'settleAmt', label: '정산잔액' }, { key: 'recallAmt', label: '미수금' }, { key: 'deductAmt', label: '미수금 차감' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/settlement/execute': {
      listSortDirAnchor: 'refresh',
      paginationSizeOptions: [50, 100, 300, 400, 500],
      paginationDefaultSize: 50,
      hasSettlementExecuteDetailTable: true,
      /** searchRows는 아래 init에서 `/calc/exCalcList`와 동기화 */
      searchRows: [],
      summary: [],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'searchBtn', label: '조회', cls: 'btn-primary' },
        { id: 'executeBtn', label: '수동실행', cls: 'btn-danger' }
      ],
      columnGuideFixedKeys: ['rowNo', 'compNm', 'compId'],
      columns: [{ key: '_chk', type: 'checkbox' }, { key: 'rowNo', label: '번호' }, { key: 'compNm', label: '업체명' }, { key: 'compId', label: '업체코드' }, { key: 'curType', label: '통화' }, { key: 'txnCnt', label: '건수', thClass: 'text-center text-nowrap', columnGuideLabel: '이번 정산 실행에 집계에 포함된 거래 건수. 컬럼 도입 이전 실행 행은 비어 있을 수 있습니다.' }, { key: 'settlementBatchFee', label: '정산료', thClass: 'text-end', columnGuideLabel: '정산 실행당 1회 정산수수료.' }, { key: 'cadenceGuideKr', label: '노출주기 안내', thClass: 'text-nowrap', columnGuideLabel: '주기별 노출 요약.' }, { key: 'settlementPublishSts', label: '배포상태', columnGuideLabel: 'PENDING·DISTRIBUTED·HOLD — 가맹점정산내역 반영 전 단계.' }, { key: 'payoutHoldYn', label: '지급보류', columnGuideLabel: 'Y면 지급보류 가맹; 배포가 HOLD로 잡힐 수 있음.' }, { key: 'settlementRunId', label: '실행ID', columnGuideLabel: '정산 실행 PK(tb_settlement_run). 추적용.' }, { key: 'receivableAmt', label: '미수금', columnGuideLabel: '정산 지급부족 시 해당 실행에 자동 등록된 미수금(발생액)' }, { key: 'pgRootNo', label: '루트' }, { key: 'settlementCloseDate', label: '정산마감일', thClass: 'text-center text-nowrap', columnGuideLabel: '집계 구간 마감일(예: W7이면 해당 주 마지막 날). 기존 calc_dt와 동일.' }, { key: 'settlementExecDate', label: '정산일자', thClass: 'text-center text-nowrap', columnGuideLabel: '정산주기·영업일 기준 배치(정산) 예정일(W+N 영업일 등).' }, { key: 'calcCycle', label: '정산주기' }, { key: 'calcMethod', label: '정산방법' }, { key: 'targetPeriodText', label: '정산대상기간', thClass: 'pay-grid-time-dual text-start small' }, { key: 'targetAmt', label: '결제액' }, { key: 'totalFee', label: '수수료', columnGuideLabel: '거래 집계만. 정산 실행당 1회 정산수수료는 정산료 열.' }, { key: 'rollingReserveAmt', label: '담보금' }, { key: 'payAmount', label: '지급액', columnGuideLabel: '0으로 보정하지 않음; 부족 시 음수·미수금 자동등록.' }, { key: 'status', label: '확정여부', columnGuideLabel: 'CALCULATED=확정, PENDING=미확정.' }],
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
      searchRows: [[{ label: '메뉴 선택', type: 'select', name: 'searchMenuId', options: [{ v: '', t: '선택' }] }, { type: 'searchBtn' }]],
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
    },
    '/ops/opsMng': {
      hideListGrid: true,
      staticHtml: OPS_MANAGEMENT_PLACEHOLDER_HTML,
      summary: [],
      buttons: []
    },
    '/ops/inactiveCard': {
      listTopHtml: OPS_INACTIVE_CARD_REGISTER_HTML,
      emptyMessage: '등록된 비활성 카드가 없습니다.',
      paginationDefaultSize: 20,
      paginationSizeOptions: [20, 50, 100],
      columnGuideFixedKeys: ['rowNo', '_inactiveCardRelease'],
      viewSettingDefaultSelectedKeys: [
        'registeredAt', 'registeredBy', 'panDisplay', 'pgVendor', 'reason', 'activeYn', 'releasedAt', 'releasedBy'
      ],
      noticeList: [
        '총본사·본사·총판(ADMIN 포함) 운영자용입니다. 메뉴 접근은 본사권한설정에서 부여합니다.',
        '등록·해지는 본사권한설정에서 이 화면 권한을 삭제(전체) 또는 수정으로 부여한 계정만 가능합니다.',
        '카드 종류별 접두(BIN)·자릿수가 맞지 않으면 경고가 표시됩니다. AMEX 15자리(4-6-5), Diners 14자리(4-6-4), 대부분 16자리(4×4), 기타는 접두 검증 없이 13~16자리(4×4)입니다.',
        '해지는 목록 맨 오른쪽 「OTP 해지」 버튼에서 실행합니다. Google OTP 6자리가 필요합니다.'
      ],
      searchRows: [[
        { label: '상태', type: 'select', name: 'searchActiveYn', col: 2,
          options: [{ v: 'Y', t: '등록카드' }, { v: 'N', t: '해지됨' }, { v: 'ALL', t: '전체' }] },
        { type: 'searchBtn' }
      ]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }],
      columns: [
        { key: 'rowNo', label: '번호' },
        { key: 'registeredAt', label: '등록일시' },
        { key: 'registeredBy', label: '등록자' },
        { key: 'panDisplay', label: '카드번호(마스킹)' },
        { key: 'pgVendor', label: 'PG' },
        { key: 'reason', label: '사유' },
        { key: 'activeYn', label: '상태' },
        { key: 'releasedAt', label: '해지일시' },
        { key: 'releasedBy', label: '해지자' },
        { key: '_inactiveCardRelease', type: 'inactiveCardRelease', label: 'OTP 해지', columnGuideLabel: 'OTP 해지' }
      ]
    },
    '/ops/mailLog': {
      emptyMessage: '조회된 데이터가 없습니다.',
      paginationDefaultSize: 20,
      paginationSizeOptions: [20, 50, 100, 200],
      searchRows: [[
        { label: '구분', type: 'select', name: 'searchMailKind', col: 2,
          options: [{ v: '', t: '전체' }, { v: 'VOID_TEST', t: 'VOID 테스트' }, { v: 'VOID_TXN', t: '이메일 무효(거래)' }] },
        { label: '상태', type: 'select', name: 'searchMailStatus', col: 2,
          options: [{ v: '', t: '전체' }, { v: 'SUCCESS', t: '성공' }, { v: 'FAIL', t: '실패' }] },
        { label: '등록일', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
        { type: 'quickdate' },
        { type: 'searchBtn' }
      ]],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }],
      columns: [
        { key: 'rowNo', label: '번호' },
        { key: 'createdAt', label: '등록일' },
        { key: 'mailKind', label: '구분' },
        { key: 'status', label: '상태' },
        { key: 'toAddress', label: '수신' },
        { key: 'subject', label: '제목' },
        { key: 'errorMessage', label: '오류' },
        { key: 'bodyPreview', label: '본문 미리보기' },
        { key: 'pgTrnId', label: '거래번호' },
        { key: 'actorUsername', label: '실행자' }
      ]
    },
    '/ops/taxReport': {
      emptyMessage: '조회된 데이터가 없습니다.',
      paginationDefaultSize: 50,
      paginationSizeOptions: [20, 50, 100, 200],
      columnGuideFixedKeys: ['rowNo', 'settlementRunId', 'compId'],
      noticeList: [
        '총본사·본사(REGIONAL)·총판(MASTER_DIST) 또는 ADMIN만 사용합니다. 다른 로그인은 목록이 비어 있거나 거부됩니다.',
        '로그인 조직 트리의 하위 가맹만 대상입니다(타 총판·타 본사 가맹 제외).',
        '행 원천: 확정정산(CALCULATED)·정산배포(DISTRIBUTED)·가맹점정산내역 노출 규칙을 통과한 정산 실행입니다.',
        '「월 통합」은 귀속월(YYYY-MM) 전체를 한 번에 조회합니다. 엑셀에는 실행 목록·TOTAL·가맹별 합계가 포함됩니다.',
        'FinalPayAfterRemittance는 송금 수수료 반영 후 지급 기준액으로, 실제 은행 송금과 일치시키는 용도로 검증하세요.'
      ],
      searchRows: [[
        { label: '보고구분', type: 'select', name: 'searchTaxScope', col: 2,
          options: [{ v: 'WEEKLY', t: '기간별(확정 정산 실행)' }, { v: 'MONTHLY', t: '월 통합(귀속월)' }] },
        { label: '귀속월', type: 'text', name: 'searchYearMonth', col: 2, placeholder: 'YYYY-MM', i18nPhKey: 'searchYearMonth' },
        { label: '정산일', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
        { type: 'quickdate' },
        { label: '가맹코드', type: 'text', name: 'searchCompId', col: 2 },
        { type: 'searchBtn' }
      ]],
      summary: ['건수'],
      buttons: [
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' },
        { id: 'excelBtn', label: '엑셀(xlsx)', cls: 'btn-info' }
      ],
      columns: [
        { key: 'rowNo', label: '번호' },
        { key: 'settlementRunId', label: '실행ID', columnGuideLabel: 'tb_settlement_run PK' },
        { key: 'settlementCloseDate', label: '정산마감일', thClass: 'text-center text-nowrap' }, { key: 'settlementExecDate', label: '정산일자', thClass: 'text-center text-nowrap' },
        { key: 'periodFrom', label: '집계시작', thClass: 'text-center text-nowrap' },
        { key: 'periodTo', label: '집계종료', thClass: 'text-center text-nowrap' },
        { key: 'compNm', label: '가맹명' },
        { key: 'compId', label: '가맹코드' },
        { key: 'txnCnt', label: '거래건수', thClass: 'text-center' },
        { key: 'approveAmt', label: '승인금액', thClass: 'text-end' },
        { key: 'cancelAmt', label: '취소금액', thClass: 'text-end' },
        { key: 'netSales', label: '순매출', thClass: 'text-end' },
        { key: 'totalFee', label: '거래수수료합', thClass: 'text-end' },
        { key: 'rollingReserveAmt', label: '담보(롤링)', thClass: 'text-end' },
        { key: 'settlementBatchFee', label: '정산수수료', thClass: 'text-end' },
        { key: 'payAmount', label: '지급액(송금전)', thClass: 'text-end', columnGuideLabel: '송금수수료 차감 전' },
        { key: 'remittanceFeeBank', label: '송금료(통화)', thClass: 'text-end' },
        { key: 'remittanceFeeUsdt', label: '송금료(USDT)', thClass: 'text-end' },
        { key: 'finalPayAfterRemittance', label: '최종지급(은행기준)', thClass: 'text-end text-nowrap', columnGuideLabel: '세금·은행 대조용' },
        { key: 'reportNote', label: '비고' }
      ]
    },
    '/ops/agencyTxnList': {
      listSortDirAnchor: 'refresh',
      paginationSizeOptions: [50, 100, 300, 400, 500],
      paginationDefaultSize: 50,
      searchFormClass: 'pay-mng-search-form',
      payMngDenseGrid: true,
      tableScrollable: true,
      columnGuideFixedKeys: ['rowNo', 'pgNm', 'pgCd', 'compNm', 'compId', 'trnDate', 'agencySettleYn'],
      viewSettingDefaultSelectedKeys: [
        'trnTime', 'routeNo', 'chillTransactionId', 'trnId', 'statusNm', 'amount', 'curType', 'policyCur',
        'txnFixedFeesSum', 'pctFeesSum', 'usdtFee', 'fxFee', 'fee3dsFee', 'rollingHoldEst',
        'failFee', 'cancelFee', 'voidFee', 'manualVoidFee', 'refundFee', 'chargebackFee', 'totalAgencyFee'
      ],
      noticeList: [
        '총본사·본사(REGIONAL)·총판(MASTER_DIST) 또는 ADMIN만 이용합니다. 조회 범위는 로그인 조직 하위 가맹 거래입니다.',
        '수수료는 본사설정 「대행수수료설정」(PG코드=거래 van) 기준이며, 가맹 수수료내역·가맹 정산(settled_yn)과 별개입니다.',
        '맨 오른쪽 「PG정산유무」는 대행수수료설정의 T/H/D·N·일괄시각으로 산출한 PG 계약 정산 도래 여부(Y=도래, N=미도래)입니다. 정책 없음·van 없음은 빈 칸입니다.'
      ],
      searchRows: [
        [
          { label: '거래일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
          { type: 'quickdate' }
        ],
        [
          { label: '검색구분', type: 'select', name: 'searchFieldType', options: [
            { v: 'ALL', t: '전체' },
            { v: 'PG_CD', t: 'PG코드' },
            { v: 'COMP_NM', t: '업체명' },
            { v: 'COMP_ID', t: '업체코드' },
            { v: 'APPROVAL_NO', t: '승인번호' },
            { v: 'ORDER_NO', t: '주문번호' },
            { v: 'MID', t: 'MID' },
            { v: 'ROUTE', t: '루트' },
            { v: 'CURRENCY', t: '통화' },
            { v: 'STATUS', t: '상태' },
            { v: 'AMOUNT', t: '금액' }
          ], size: 11 },
          { label: '검색어', type: 'text', name: 'searchKeyword', placeholder: '검색어', size: 22 },
          { label: '상태그룹', type: 'select', name: 'searchStatusGroup', options: [
            { v: 'ALL', t: '전체' },
            { v: 'SUCCESS', t: '성공' },
            { v: 'FAIL', t: '실패' },
            { v: 'CANCEL', t: '취소' },
            { v: 'VOID', t: '무효' },
            { v: 'MANUAL_VOID', t: '수동무효' },
            { v: 'REFUND', t: '환불' },
            { v: 'FORCE_REFUND', t: '강제환불' },
            { v: 'EXCLUDE_SUCCESS', t: '성공제외' }
          ], size: 11 },
          { type: 'searchBtn', label: '검색' }
        ]
      ],
      summary: ['건수'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' }
      ],
      headerGroups: [
        { label: 'PG·가맹', keys: ['pgNm', 'pgCd', 'compNm', 'compId'] },
        { label: '거래', keys: ['trnDate', 'trnTime', 'routeNo', 'chillTransactionId', 'trnId', 'statusNm', 'amount'] },
        { label: '승인 / 대행수수료(%)', keys: ['txnFixedFeesSum', 'pctFeesSum'] },
        { label: '기타수수료', keys: ['usdtFee', 'fxFee', 'fee3dsFee'] },
        { label: '실패·취소·무효·환불·차지백', keys: ['failFee', 'cancelFee', 'voidFee', 'manualVoidFee', 'refundFee', 'chargebackFee'] },
        { label: '합계·정산', keys: ['rollingHoldEst', 'totalAgencyFee', 'agencySettleYn'] }
      ],
      columns: [
        { key: 'rowNo', label: '번호' },
        { key: 'pgNm', label: 'PG명' },
        { key: 'pgCd', label: 'PG코드' },
        { key: 'compNm', label: '업체명' },
        { key: 'compId', label: '업체코드' },
        { key: 'trnDate', label: '거래일' },
        { key: 'trnTime', label: '거래시간' },
        { key: 'routeNo', label: '루트' },
        { key: 'chillTransactionId', label: '승인번호' },
        { key: 'trnId', label: '거래번호(우리)' },
        { key: 'statusNm', label: '상태' },
        { key: 'amount', label: '결제금액' },
        { key: 'curType', label: '통화' },
        { key: 'policyCur', label: '정책통화' },
        { key: 'txnFixedFeesSum', label: '건당수수료' },
        { key: 'pctFeesSum', label: '결제(%)' },
        { key: 'usdtFee', label: 'USDT' },
        { key: 'fxFee', label: 'FX' },
        { key: 'fee3dsFee', label: '3DS' },
        { key: 'rollingHoldEst', label: '담보추정액' },
        { key: 'failFee', label: '실패' },
        { key: 'cancelFee', label: '취소' },
        { key: 'voidFee', label: '무효' },
        { key: 'manualVoidFee', label: '수무효' },
        { key: 'refundFee', label: '환불' },
        { key: 'chargebackFee', label: '차지백' },
        { key: 'totalAgencyFee', label: '대행수수료합' },
        { key: 'agencySettleYn', label: 'PG정산유무', thClass: 'text-center text-nowrap', columnGuideLabel: '대행수수료설정 T/H/D 기준 도래 Y/N' }
      ],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/ops/integratedReport': {
      isOpsIntegratedReport: true,
      opsIntegratedReportScreen: true,
      payListFinancialInline: true,
      listSortDirAnchor: 'refresh',
      searchFormClass: 'pay-mng-search-form',
      searchRows: [[
        { label: '적재일', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
        { type: 'quickdate', quickdateLabels: ['당일', '당월', '전일', '1주', '2주', '전월'], quickdateRanges: ['day', 'month', 'prevDay', 'week', 'week2', 'prevMonth'] },
        { type: 'searchBtn', label: '검색' }
      ]],
      noticeList: [
        '총본사·본사(REGIONAL)·총판(MASTER_DIST) 또는 ADMIN만 사용합니다. 조회 범위는 로그인 조직의 하위 가맹 거래입니다.',
        '집계 기준일은 거래 적재일(created_at)이며, 일별결제와 동일합니다. 성공·취소·실패 등 상태 열의 금액은 결제액이 아니라 「건수 × 기본 수수료 정책의 해당 상태 건당 수수료」(예: 성공 7건·건당 20 → 140)입니다.',
        '일자 행을 더블클릭하면 아래 「선택 일자 상세 (통합 결제내역)」에 해당 일 통합 결제내역 전체·총거래~추정결산 요약이 표시됩니다. 번호·결제시간·승인번호·업체·정산주기·정산예정(업체명 오른쪽)·결제액·수수료(총수수료+부가세)·정산액·상태(한글)를 확인할 수 있으며, 수수료·정산액은 정산관리 수수료내역과 동일한 건별 산식(FeeListTxnBreakdown)입니다. 거래일 열은 두지 않습니다. 가맹 열 「수수료(변동·% / 건당)」은 정책 결제율(%)·건당 표시입니다.',
        '내부 상태 「요청」(08, 인증·결제 진행 중)은 상단 집계의 「기타」 건수에 포함되며 취소·실패·성공이 아닙니다. 칠페이 최종 승인 노티(성공) 전 단계이며, 가맹 결제통보 URL이 설정된 경우에만 PG→가맹 통보가 나갈 수 있습니다(칠페이→PG 수신 노티와 별개).',
        '[엑셀다운로드]는 결제내역과 동일한 상단 메뉴 형태이며, 현재 조회된 일자별 통합 리포트 표를 서식 xlsx로 받습니다.',
        '요약 바: 검색 기간 전체 거래 건수(건수)와 통화별 총결제액(승인−취소)·총수수료(부가세 제외)·총보증금(담보 추정)·예상지급액을 결제내역 상단과 같은 형식으로 표시합니다.'
      ],
      summary: ['건수'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'excelDownBtn', label: '엑셀다운로드', cls: 'btn-info' }
      ],
      columns: [{ key: 'day', label: '일자' }],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/ops/verifyReport': {
      isOpsVerifyReport: true,
      opsVerifyReportScreen: true,
      listSortDirAnchor: 'refresh',
      searchFormClass: 'pay-mng-search-form',
      searchRows: [[
        { label: '거래일자', type: 'daterange', from: 'searchFromDate', to: 'searchToDate' },
        { type: 'quickdate', quickdateLabels: ['당일', '당월', '전일', '1주', '2주', '전월'], quickdateRanges: ['day', 'month', 'prevDay', 'week', 'week2', 'prevMonth'] },
        { label: '검색구분', type: 'select', name: 'searchFieldType', options: [
          { v: 'ALL', t: '전체' },
          { v: 'APPROVAL_NO', t: '승인번호' },
          { v: 'ORDER_NO', t: '주문번호' },
          { v: 'CUSTOMER_ID', t: '고객ID' },
          { v: 'MID', t: 'MID' },
          { v: 'ROUTE', t: '루트' },
          { v: 'STATUS', t: '상태' }
        ], size: 11 },
        { label: '검색어', type: 'text', name: 'searchKeyword', placeholder: '검색어', size: 22 },
        { label: '상태구분', type: 'select', name: 'searchPayDivCd', options: [
          { v: '', t: '전체' },
          { v: '10', t: '성공' },
          { v: '20', t: '취소' },
          { v: 'FAIL', t: '실패' },
          { v: '40', t: '자동무효' },
          { v: '41', t: '이메일 무효' },
          { v: '42', t: '자동환불' },
          { v: '31', t: '강제환불' }
        ], size: 11 },
        { type: 'searchBtn', label: '검색' }
      ]],
      noticeList: [
        'ChillPay 통합내역(API, 거래일 TransactionDate)을 기준으로 결제내역 NOTI(origin=NOTI)와 승인번호(TransactionId)·결제액·상태를 대조합니다.',
        '일치 건은 하단 「선택 일자 불일치」 목록에서 제외됩니다. 통합 상태가 요청·대기(인증 전)이면 노티가 오지 않는 것이 정상이므로 NOTI 미수신으로 잡지 않습니다.',
        '상태 불일치 건은 하단에서 「통합 기준 맞춤」 또는 「상태 불일치 일괄 맞춤」으로 NOTI 결제내역을 통합 상태에 맞출 수 있습니다(금액 불일치는 제외).',
        'JPAY 등 다른 PG·URL/챗봇만 있는 건은 대상이 아니며, 통합에 없고 결제에만 있는 건은 오류로 표시하지 않습니다.',
        '일자 행을 더블클릭하면 해당 일의 불일치 건만 표시합니다(승인번호로 통합내역·결제내역에서 추적). 결제시간·금액 표기 형식 차이는 불일치로 잡지 않습니다.',
        '총본사·본사(REGIONAL)·총판(MASTER_DIST) 또는 ADMIN만 사용합니다. 조회 기간은 최대 93일입니다.'
      ],
      summary: ['건수'],
      buttons: [
        { id: 'payListRefreshBtn', label: '새로고침', cls: 'btn-outline-secondary' },
        { id: 'searchBtn', label: '검색', cls: 'btn-primary' }
      ],
      columns: [
        { key: 'rowNo', label: '번호' },
        { key: 'day', label: '일자' },
        { key: 'chillCount', label: '통합(Chill)건수' },
        { key: 'matchedCount', label: '일치건수' },
        { key: 'mismatchCount', label: '불일치건수' },
        { key: 'note', label: '비고' }
      ],
      emptyMessage: '조회된 데이터가 없습니다.'
    },
    '/deploy/integrationPlan': {
      hideListGrid: true,
      staticHtml: DEPLOY_STATIC_HTML.integrationPlan,
      summary: [],
      buttons: []
    },
    '/deploy/jpayWorkPlan': {
      hideListGrid: true,
      staticHtml: DEPLOY_STATIC_HTML.jpayWorkPlan,
      summary: [],
      buttons: []
    },
    '/deploy/merchantApiPolicy': {
      hideListGrid: true,
      staticHtml: DEPLOY_STATIC_HTML.merchantApiPolicy,
      summary: [],
      buttons: []
    },
    '/deploy/launchChecklist': {
      hideListGrid: true,
      staticHtml: DEPLOY_STATIC_HTML.launchChecklist,
      summary: [],
      buttons: []
    }
  };

  /** 통합 결제내역 `/calc/payList` — VIEW SETTING·그리드 열 단일 정의 (`PG_PAY_LIST_INTEGRATED`, 노티매핑 기본과 동기) */
  (function applyPayListIntegratedCatalog() {
    var scr = MENU_SCREENS['/calc/payList'];
    var P = typeof window !== 'undefined' ? window.PG_PAY_LIST_INTEGRATED : null;
    if (!scr || !P || !P.columns || !P.headerGroups) {
      if (typeof console !== 'undefined' && console.warn && scr) {
        console.warn('PG_PAY_LIST_INTEGRATED missing; include pay-list-integrated-catalog.js before screens.js');
      }
      return;
    }
    scr.headerGroups = JSON.parse(JSON.stringify(P.headerGroups));
    scr.columns = P.columns.map(function (c) {
      var o = { key: c.key, label: c.label };
      if (c.gridType === 'checkbox') o.type = 'checkbox';
      else if (c.gridType === 'payActions') {
        o.type = 'payActions';
        o.key = 'payActions';
      }
      else if (c.gridType === 'payRemark') {
        o.type = 'payRemark';
        o.key = 'payRemark';
      }
      return o;
    });
    if (P.columnGuideFixedKeys && P.columnGuideFixedKeys.length) {
      scr.columnGuideFixedKeys = P.columnGuideFixedKeys.slice();
    }
  })();

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
    /**
     * SUCCESS/REFUND/CANCEL/VOID 등 상태 고정 페이지는 상태구분(searchPayDivCd)이 중복이므로 숨김.
     * (결제내역은 통합 목록이므로 유지)
     */
    function stripStatusDiv(o) {
      try {
        if (!o || !o.searchRows || !o.searchRows.length) return o;
        o.searchRows = (o.searchRows || []).map(function (row) {
          return (row || []).filter(function (cell) {
            return !(cell && cell.type === 'select' && cell.name === 'searchPayDivCd');
          });
        }).filter(function (row) { return row && row.length; });
        /* 고정 상태 화면은 검색구분의 '상태'도 의미 없으므로 제거 */
        (o.searchRows || []).forEach(function (row) {
          (row || []).forEach(function (cell) {
            if (!cell || cell.type !== 'select' || cell.name !== 'searchFieldType' || !cell.options) return;
            cell.options = (cell.options || []).filter(function (opt) {
              return !(opt && String(opt.v).toUpperCase() === 'STATUS');
            });
          });
        });
      } catch (e) { /* ignore */ }
      return o;
    }
    MENU_SCREENS['/calc/payNotiList'] = cloneWith('NOTI', [
      '노티내역: 통합 결제내역과 동일한 그리드입니다(칠페이 시트 컬럼·2단 헤더·요약바·후속조치 포함). 조회만 origin=NOTI(전산 노티 적재)로 제한됩니다.',
      'ziobiz/NOTI 종합거래의 노티거래내역과 동일 성격의 데이터입니다.',
      '[후속조치]는 본사설정 > 전산설정관리에서 기능을 켠 경우에만 동작합니다 (NOTI 환경설정과 동일).',
      '취소 건에 대한 정산 수수료 및 부가세는 정산 주기에 따라 반영됩니다.',
      '정산 주기 및 정산 수수료는 가맹점별로 상이할 수 있습니다.'
    ], true);
    function asStatusOnlyPayScreen(o) {
      o.payListFinancialInline = false;
      return o;
    }
    MENU_SCREENS['/calc/paySuccessList'] = asStatusOnlyPayScreen(stripStatusDiv(cloneWith('SUCCESS', [
      '성공내역: 통합 결제내역에서 승인 성공(결제) 상태만 간추렸습니다.',
      '상단은 건수와 해당 상태(성공) 요약 pill만 표시합니다(일별통합과 동일). 금액·수수료 한 줄 집계는 통합 결제내역·수수료내역을 이용하세요.',
      '무효·이메일무효·환불·강제환불 등 후속조치는 「결제내역」(/calc/payList)에서만 제공합니다.'
    ])));
    MENU_SCREENS['/calc/payFailList'] = asStatusOnlyPayScreen(stripStatusDiv(cloneWith('FAIL', [
      '실패내역: 통합 결제내역에서 실패·거절만 간추렸습니다.',
      '상단은 건수와 해당 상태(실패) 요약 pill만 표시합니다(일별통합과 동일).'
    ])));
    MENU_SCREENS['/calc/payRefundList'] = asStatusOnlyPayScreen(stripStatusDiv(cloneWith('REFUND', [
      '환불처리: 통합 결제내역에서 일반·자동환불(내부 30·42)만 간추렸습니다.',
      '상단은 건수와 해당 상태(환불) 요약 pill만 표시합니다(일별통합과 동일).'
    ])));
    MENU_SCREENS['/calc/payForceRefundList'] = asStatusOnlyPayScreen(stripStatusDiv(cloneWith('FORCE_REFUND', [
      '강제환불: 통합 결제내역에서 강제환불(내부 31)만 간추렸습니다.',
      '상단은 건수와 해당 상태(강제환불) 요약 pill만 표시합니다(일별통합과 동일).'
    ])));
    MENU_SCREENS['/calc/payCancelList'] = asStatusOnlyPayScreen(stripStatusDiv(cloneWith('CANCEL', [
      '취소내역: 통합 결제내역에서 취소만 간추렸습니다.',
      '상단은 건수와 해당 상태(취소) 요약 pill만 표시합니다(일별통합과 동일).'
    ])));
    MENU_SCREENS['/calc/payVoidList'] = asStatusOnlyPayScreen(stripStatusDiv(cloneWith('VOID', [
      '무효처리: 통합 결제내역에서 자동·시스템 무효(내부 21·40)만 표시합니다. 이메일무효(22·41)는 「이메일무효」메뉴, 취소(20)와 구분됩니다.',
      '상단은 건수와 해당 상태(무효) 요약 pill만 표시합니다(일별통합과 동일).'
    ])));
    MENU_SCREENS['/calc/payEmailVoidList'] = asStatusOnlyPayScreen(stripStatusDiv(cloneWith('MANUAL_VOID', [
      '이메일무효: 통합 결제내역에서 수동·이메일 무효(내부 22·41)만 표시합니다. 자동무효(21·40)는 「무효처리」메뉴입니다.',
      '상단은 건수와 해당 상태(이메일 무효) 요약 pill만 표시합니다(일별통합과 동일).'
    ])));
    MENU_SCREENS['/calc/offsetCancList'] = cloneWith('OFFSET_CANCEL', [
      '상계취소내역: 가맹 정산에 이미 반영된 건(settled=Y)이 이후 취소·무효·환불·강제환불(내부 20·21·22·30·31·40·41·42)로 바뀐 경우만 표시합니다. 정산 전 실패(F0·99) 등은 제외됩니다.',
      '동일 조건으로 노티 반영 시 「환수금관리」에 POST_SETTLE_REFUND 자동 등록이 되며, 차기 정산 지급액에서 FIFO 차감됩니다(전산설정·가맹 환수모드와 동일).'
    ]);
    MENU_SCREENS['/pay/easyPay'] = cloneWith('URL_PAY', ['URL결제내역: 가맹점 API연동 노티 외, 플랫폼이 칠페이 결제 API로 발급한 결제수소(URL)로 발생한 전 건(성공·실패·환불·취소 등). 통합 결제내역에도 포함되며, 여기서는 origin=URL 만 조회합니다.']);
    MENU_SCREENS['/pay/chatbotPay'] = cloneWith('CHATBOT_PAY', [
      '챗봇결제내역: 웹 EFO 챗봇 결제 플로우에서 동일 칠페이(URL/카드) API로 생성·적재한 건만 표시합니다. 통합 결제내역에도 포함되며, 여기서는 origin=CHATBOT 만 조회합니다.',
      'URL결제내역과 동일 API(/api/calc/payList)·그리드를 사용하며 payListVariant=CHATBOT_PAY 로 구분합니다.'
    ]);
    MENU_SCREENS['/pay/splitPay'] = cloneWith('SPLIT_PAY', [
      '분할결제내역: URL 분할결제 계약의 회차별 결제(pg_trnsctn)만 표시합니다. 통합 결제내역에도 포함되며, 여기서는 분할결제 회차 주문번호로만 조회합니다.',
      'URL결제내역·챗봇결제내역과 동일 API(/api/calc/payList)·그리드를 사용하며 payListVariant=SPLIT_PAY 로 구분합니다.'
    ]);
    MENU_SCREENS['/pay/jpaySubscription'] = {
      emptyMessage: '조회된 구독이 없습니다.',
      paginationSizeOptions: [25, 50, 100],
      paginationDefaultSize: 25,
      searchRows: [[
        { label: '가맹코드', type: 'text', name: 'searchCompId' },
        { type: 'searchBtn', label: '검색' }
      ]],
      noticeList: ['JPAY API 구독 마스터(tb_merchant_jpay_subscription). 회차별 결제는 통합내역 origin=SUBSCRIPTION 을 참고하세요.'],
      summary: ['건수'],
      buttons: [{ id: 'searchBtn', label: '검색', cls: 'btn-primary' }],
      columns: [
        { key: 'rowNo', label: '번호' },
        { key: 'compCode', label: '가맹코드', thClass: 'text-nowrap' },
        { key: 'checkoutOrderNo', label: '주문번호', thClass: 'text-nowrap' },
        { key: 'status', label: '상태', thClass: 'text-nowrap' },
        { key: 'periodCount', label: '회차', thClass: 'text-nowrap' },
        { key: 'paymentTransactionId', label: '구독TX', thClass: 'text-nowrap' },
        { key: 'pgCd', label: 'PG', thClass: 'text-nowrap' },
        { key: 'createdAt', label: '등록', thClass: 'text-nowrap' },
        { key: 'cancelledAt', label: '해지', thClass: 'text-nowrap' }
      ]
    };
  })();

  /** 정산보류내역: 가맹점정산내역과 동일 그리드 + [선택 해제]. 지급보류(Y) 가맹점의 정산 실행 건만 표시 */
  (function addPaySettlementHoldListScreens() {
    try {
      var gm = MENU_SCREENS['/calc/calcGmList'];
      if (!gm) return;
      function buildPh() {
        var ph = JSON.parse(JSON.stringify(gm));
        ph.summary = ['건수', '결제금액', '미수금', '지급액'];
        ph.noticeList = [
          '정산방법에서 지급보류가 「보류」인 가맹점은 정산 실행 시 결과가 가맹점정산내역·유통망정산 집계에 나타나지 않고 이 화면에만 적치됩니다. 정산 금액·수수료 등은 이미 계산·저장된 값입니다.',
          '「보류해제」열의 [Y→N 해제]로 한 건만 바로 해제하거나, 체크 후 [선택 건 지급보류 해제]로 여러 건을 한 번에 처리할 수 있습니다. 더블 확인 후 실행 행의 지급보류(Y)가 N으로 바뀌며 가맹점정산내역(및 유통 집계)에 반영됩니다. 가맹점 설정의 지급보류는 그대로이며, 이후 신규 정산 건은 다시 이 목록에 쌓일 수 있습니다.',
          '결제 건별 롤링 예치(담보)는 「담보금내역」(/calc/collateralList)에서 확인하세요.',
          '표시 열은 [헬로] 옆 VIEW SETTING에서 조정할 수 있습니다(저장 시 사용자별 유지). 체크·보류해제 열은 항상 표시됩니다.'
        ];
        var baseBtns = gm.buttons ? JSON.parse(JSON.stringify(gm.buttons)) : [
          { id: 'searchBtn', label: '검색', cls: 'btn-primary' },
          { id: 'excelBtn', label: '엑셀다운로드', cls: 'btn-info' }
        ];
        ph.buttons = baseBtns.concat([{ id: 'payoutHoldReleaseBtn', label: '선택 건 지급보류 해제', cls: 'btn-warning' }]);
        /** 가맹점정산내역은 체크 제외 — 보류 해제 화면만 행 선택 필요 */
        var phCols = ph.columns || [];
        var hasChkPh = phCols.some(function (cc) { return cc && cc.key === '_chk'; });
        if (!hasChkPh) {
          phCols = [{ key: '_chk', type: 'checkbox' }].concat(phCols);
          ph.columns = phCols;
        }
        ph.tableColumnGuide = true;
        ph.columnGuideFixedKeys = ['_chk', 'rowNo', 'compNm', 'compId', 'curType', '_payoutHoldRelease'];
        if (gm.viewSettingDefaultSelectedKeys && gm.viewSettingDefaultSelectedKeys.length) {
          ph.viewSettingDefaultSelectedKeys = gm.viewSettingDefaultSelectedKeys.slice();
        }
        var yIdxPh = -1;
        for (var ypi = 0; ypi < phCols.length; ypi++) {
          if (phCols[ypi] && phCols[ypi].key === 'payoutHoldYn') { yIdxPh = ypi; break; }
        }
        var relColPh = {
          key: '_payoutHoldRelease',
          label: '보류해제',
          type: 'payoutHoldReleaseBtn',
          thClass: 'text-center text-nowrap',
          columnGuideLabel: '클릭 시 이 실행 건만 지급보류 Y→N 해제(가맹점정산내역·유통 반영). 가맹 설정의 지급보류는 변경되지 않습니다.'
        };
        if (yIdxPh >= 0) {
          var phCols2 = phCols.slice();
          phCols2.splice(yIdxPh + 1, 0, relColPh);
          ph.columns = phCols2;
        } else {
          ph.columns = phCols.concat([relColPh]);
        }
        return ph;
      }
      MENU_SCREENS['/calc/paySettlementHoldList'] = buildPh();
      MENU_SCREENS['/settlement/paySettlementHoldList'] = buildPh();
    } catch (ePh) { /* ignore */ }
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
        fr.searchFormClass = gm.searchFormClass || fr.searchFormClass;
        fr.payMngDenseGrid = !!gm.payMngDenseGrid;
        if (gm.paginationSizeOptions) fr.paginationSizeOptions = gm.paginationSizeOptions.slice();
        if (gm.paginationDefaultSize != null) fr.paginationDefaultSize = gm.paginationDefaultSize;
        if (gm.noticeList && gm.noticeList.length) fr.noticeList = gm.noticeList.slice();
        if (gm.buttons) fr.buttons = JSON.parse(JSON.stringify(gm.buttons));
        if (gm.listSortDirAnchor) fr.listSortDirAnchor = gm.listSortDirAnchor;
        if (gm.searchRows && gm.searchRows.length) {
          fr.searchRows = JSON.parse(JSON.stringify(gm.searchRows));
        }
        if (gm.columnGuideFixedKeys && gm.columnGuideFixedKeys.length) {
          fr.columnGuideFixedKeys = gm.columnGuideFixedKeys.slice();
        }
        if (gm.tableColumnGuide === false) fr.tableColumnGuide = false;
        else if (gm.tableColumnGuide === true) fr.tableColumnGuide = true;
        if (gm.viewSettingDefaultSelectedKeys && gm.viewSettingDefaultSelectedKeys.length) {
          fr.viewSettingDefaultSelectedKeys = gm.viewSettingDefaultSelectedKeys.slice();
        }
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
        if (cl.columnGuideFixedKeys && cl.columnGuideFixedKeys.length) {
          dist.columnGuideFixedKeys = cl.columnGuideFixedKeys.slice();
        }
        if (cl.viewSettingDefaultSelectedKeys && cl.viewSettingDefaultSelectedKeys.length) {
          dist.viewSettingDefaultSelectedKeys = cl.viewSettingDefaultSelectedKeys.slice();
        }
        if (cl.listSortDirAnchor) dist.listSortDirAnchor = cl.listSortDirAnchor;
        if (cl.paginationSizeOptions) dist.paginationSizeOptions = cl.paginationSizeOptions.slice();
        if (cl.paginationDefaultSize != null) dist.paginationDefaultSize = cl.paginationDefaultSize;
      }
      var ex = MENU_SCREENS['/settlement/execute'];
      var exc = MENU_SCREENS['/calc/exCalcList'];
      if (ex && exc && ex.columns && ex.columns.length) {
        exc.columns = JSON.parse(JSON.stringify(ex.columns));
        (exc.columns || []).forEach(function (col) {
          if (col && col.key === 'settlementCloseDate') col.label = '정산마감일';
          if (col && col.key === 'settlementExecDate') col.label = '정산일자';
        });
        if (ex.columnGuideFixedKeys && ex.columnGuideFixedKeys.length) {
          exc.columnGuideFixedKeys = ex.columnGuideFixedKeys.slice();
        }
      }
      if (ex && exc && exc.searchRows && exc.searchRows.length) {
        ex.searchRows = JSON.parse(JSON.stringify(exc.searchRows));
        if (ex.searchRows && ex.searchRows[0] && ex.searchRows[0][0]) {
          ex.searchRows[0][0].label = '정산대상일';
        }
        (ex.searchRows || []).forEach(function (row) {
          (row || []).forEach(function (cell) {
            if (cell && cell.type === 'searchBtn') cell.label = '조회';
          });
        });
      }
      if (ex && exc && exc.noticeList && exc.noticeList.length) {
        ex.noticeList = exc.noticeList.slice();
        try { delete ex.notice; } catch (eN1) { ex.notice = undefined; }
      } else if (ex && exc && exc.notice) {
        ex.notice = exc.notice;
        try { delete ex.noticeList; } catch (eN2) { ex.noticeList = undefined; }
      }
      if (ex && exc) {
        if (exc.settlementExecuteListModeField) ex.settlementExecuteListModeField = true;
        if (exc.listToolbarBetweenRefreshAndSort && exc.listToolbarBetweenRefreshAndSort.length) {
          ex.listToolbarBetweenRefreshAndSort = JSON.parse(JSON.stringify(exc.listToolbarBetweenRefreshAndSort));
        }
        if (exc.hasSettlementExecuteDetailTable) ex.hasSettlementExecuteDetailTable = true;
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
        if (rc.noticeList && rc.noticeList.length) rcs.noticeList = rc.noticeList.slice();
        else if (rc.notice) rcs.noticeList = [String(rc.notice)];
        if (rc.buttons) rcs.buttons = JSON.parse(JSON.stringify(rc.buttons));
        if (rc.listSortDirAnchor) rcs.listSortDirAnchor = rc.listSortDirAnchor;
        if (rc.paginationSizeOptions) rcs.paginationSizeOptions = rc.paginationSizeOptions.slice();
        if (rc.paginationDefaultSize != null) rcs.paginationDefaultSize = rc.paginationDefaultSize;
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
      if (sr && srs) {
        if (sr.hasSettlementExecuteDetailTable) srs.hasSettlementExecuteDetailTable = true;
        if (sr.settlementExecuteDetailUiVariant) srs.settlementExecuteDetailUiVariant = sr.settlementExecuteDetailUiVariant;
      }
      if (sr && srs && sr.columnsBySub && sr.columnsBySub.RST) {
        if (!srs.columnsBySub) srs.columnsBySub = {};
        srs.columnsBySub.RST = JSON.parse(JSON.stringify(sr.columnsBySub.RST));
      }
      if (sr && srs && sr.columnsBySub && sr.columnsBySub.EXE) {
        if (!srs.columnsBySub) srs.columnsBySub = {};
        srs.columnsBySub.EXE = JSON.parse(JSON.stringify(sr.columnsBySub.EXE));
      }
      if (sr && srs && sr.columnsBySub && sr.columnsBySub.AGG) {
        if (!srs.columnsBySub) srs.columnsBySub = {};
        srs.columnsBySub.AGG = JSON.parse(JSON.stringify(sr.columnsBySub.AGG));
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

  /** 목록 API용 searchOrderDir — 기본 내림차순; 액션줄에서는 [새로고침][내림차순][오름차순] 순(결제·정산 목록, app.js 클릭 즉시 조회) */
  function buildListSortDirSelectHtml(tabId) {
    var tid = tabId || '';
    var sid = tid ? ('searchOrderDir_' + tid.replace(/[^a-zA-Z0-9_-]/g, '_')) : 'searchOrderDir';
    return (
      '<div class="screen-list-sort-dir-wrap d-inline-flex align-items-center gap-2 flex-shrink-0">' +
      '<input type="hidden" name="searchOrderDir" class="screen-list-sort-dir-hidden" id="' + sid + '_val" value="DESC">' +
      '<div class="d-inline-flex align-items-center gap-2 screen-list-sort-dir-menu" role="toolbar" data-pg-ui-aria-label="정렬 순서" aria-label="' + escUi(L('정렬 순서')) + '">' +
      '<button type="button" class="btn btn-sm btn-secondary screen-list-sort-dir-btn" data-pg-i18n-sort="DESC" data-search-order-dir="DESC"><span data-pg-ui-t="내림차순">' + escUi(L('내림차순')) + '</span></button>' +
      '<button type="button" class="btn btn-sm btn-outline-secondary screen-list-sort-dir-btn" data-pg-i18n-sort="ASC" data-search-order-dir="ASC"><span data-pg-ui-t="오름차순">' + escUi(L('오름차순')) + '</span></button>' +
      '</div></div>'
    );
  }
  /** 선택 일자 상세 — 내림·오름차순(상세 전용) + 엑셀리스트다운 */
  function buildDailyDetailSortDirHtml(tabId) {
    var tid = tabId || '';
    var sid = tid ? ('searchDetailOrderDir_' + tid.replace(/[^a-zA-Z0-9_-]/g, '_')) : 'searchDetailOrderDir';
    return (
      '<div class="screen-list-sort-dir-wrap d-inline-flex align-items-center gap-2 flex-shrink-0">' +
      '<input type="hidden" name="searchDetailOrderDir" class="screen-list-sort-dir-hidden daily-detail-sort-dir-hidden" id="' + sid + '_val" value="DESC">' +
      '<div class="d-inline-flex align-items-center gap-2 screen-list-sort-dir-menu daily-detail-sort-dir-menu" role="toolbar" data-pg-ui-aria-label="상세 정렬 순서" aria-label="' + escUi(L('상세 정렬 순서')) + '">' +
      '<button type="button" class="btn btn-sm btn-secondary screen-list-sort-dir-btn daily-detail-sort-dir-btn" data-pg-i18n-sort="DESC" data-search-order-dir="DESC"><span data-pg-ui-t="내림차순">' + escUi(L('내림차순')) + '</span></button>' +
      '<button type="button" class="btn btn-sm btn-outline-secondary screen-list-sort-dir-btn daily-detail-sort-dir-btn" data-pg-i18n-sort="ASC" data-search-order-dir="ASC"><span data-pg-ui-t="오름차순">' + escUi(L('오름차순')) + '</span></button>' +
      '</div></div>'
    );
  }
  function buildDailyDetailToolbarHtml(tabId, opts) {
    opts = opts || {};
    var tid = tabId || '';
    var escAttr = function (s) {
      return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;');
    };
    var titleId = opts.titleId || ('dailyDetailTitle_' + tid);
    var titleKey = opts.titleKey || '선택 일자 상세';
    var countId = opts.countId || ('dailyDetailCount_' + tid);
    var finId = opts.financialSummaryId || ('dailyDetailFinancialSummary_' + tid);
    return (
      '<div class="daily-detail-toolbar d-flex flex-wrap justify-content-between align-items-center gap-2 mb-2">' +
      '<div class="daily-detail-toolbar-start d-flex flex-wrap align-items-center gap-2 flex-grow-1 min-w-0">' +
      '<div class="fw-semibold small text-secondary daily-detail-section-title flex-shrink-0" id="' + escAttr(titleId) + '" data-pg-ui-t="' + escAttr(titleKey) + '">' + escUi(L(titleKey)) + '</div>' +
      '<span class="pay-list-aggregate-inline-sep daily-detail-count-sep" aria-hidden="true" style="display:none">ㅣ</span>' +
        '<span class="summary-total-item daily-detail-count summary-count-item" id="' + escAttr(countId) + '" style="display:none" aria-live="polite"></span>' +
      '<span class="pay-list-aggregate-inline-sep daily-detail-financial-sep" aria-hidden="true" style="display:none">ㅣ</span>' +
      '<div class="pay-list-financial-summary pay-list-financial-summary--empty daily-detail-financial-summary" id="' + escAttr(finId) + '" role="status" aria-live="polite"></div>' +
      '</div>' +
      '<div class="screen-action-buttons daily-detail-action-buttons d-flex flex-wrap align-items-center gap-2 flex-shrink-0">' +
      buildDailyDetailSortDirHtml(tid) +
      '<button type="button" class="btn btn-info btn-sm" id="listExcelDownBtn"><span data-pg-ui-t="엑셀리스트다운">' + escUi(L('엑셀리스트다운')) + '</span></button>' +
      '</div></div>'
    );
  }
  function toolbarHasSearchBtnForSort(btns) {
    return (btns || []).some(function (b) { return b && String(b.id || '') === 'searchBtn'; });
  }
  function toolbarHasRefreshBtnForSort(btns) {
    return (btns || []).some(function (b) { return b && String(b.id || '') === 'payListRefreshBtn'; });
  }

  var INPUT_SCALE = 1.3;

  function renderSearchCell(field, cfg, tabId) {
    var inner = '';
    var ch = field.size != null ? field.size : autoCh(field);
    var sz = sizeStyle(Math.ceil(ch * INPUT_SCALE));
    var todayIso = (function () {
      try {
        var d = new Date();
        if (isNaN(d.getTime())) return '';
        var y = String(d.getFullYear());
        var m = String(d.getMonth() + 1).padStart(2, '0');
        var dd = String(d.getDate()).padStart(2, '0');
        return y + '-' + m + '-' + dd;
      } catch (e) {
        return '';
      }
    })();
    var escDA = function (s) {
      return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;');
    };
    if (field.type === 'daterange') {
      var drLblAttr = field.label ? (' data-pg-i18n-lbl="' + escDA('drLbl:' + field.label) + '"') : '';
      inner =
        (field.label ? '<span class="search-cell-label"' + drLblAttr + '>' + escUi(L(String(field.label))) + '</span>' : '') +
        '<div class="search-cell-input search-cell-input--daterange d-flex flex-wrap align-items-center gap-1">' +
        '<input type="date" lang="en-CA" class="form-control form-control-sm search-date-input pg-date-input-iso" id="' +
        (field.from || 'searchFromDate') +
        '" name="' +
        (field.from || 'searchFromDate') +
        '"' + (todayIso ? (' value="' + escDA(todayIso) + '"') : '') + '>' +
        '<span class="search-daterange-sep text-muted px-1" aria-hidden="true">—</span>' +
        '<input type="date" lang="en-CA" class="form-control form-control-sm search-date-input pg-date-input-iso" id="' +
        (field.to || 'searchToDate') +
        '" name="' +
        (field.to || 'searchToDate') +
        '"' + (todayIso ? (' value="' + escDA(todayIso) + '"') : '') + '>' +
        '</div>';
      return wrapSearchCell(inner, !!field.label);
    }
    if (field.type === 'quickdate') {
      var labels = field.quickdateLabels || ['당일', '당월', '전일', '1주', '2주', '전월'];
      var ranges = field.quickdateRanges || ['day', 'month', 'prevDay', 'week', 'week2', 'prevMonth'];
      var btns = '';
      for (var i = 0; i < labels.length; i++) {
        var lbl = labels[i];
        var rgi = ranges[i] || '';
        btns += '<button type="button" class="btn btn-outline-primary btn-sm mr-1 quick-date" data-range="' + rgi + '" data-pg-i18n-qd="' + escDA(rgi) + '" aria-pressed="false">' + escUi(L(String(lbl))) + '</button>';
      }
      inner = '<div class="search-cell-input">' + btns + '</div>';
      return wrapSearchCell(inner, false);
    }
    if (field.type === 'customHtml') {
      inner = '<div class="search-cell-input search-cell-input--custom-html">' + (field.html || '') + '</div>';
      return wrapSearchCell(inner, false);
    }
    if (field.type === 'compMngSearchActions') {
      var cbName = field.checkboxName || 'searchIncludeSub';
      var searchLbl = field.searchLabel || '검색';
      inner = '<div class="search-cell-input comp-mng-search-actions-wrap d-flex align-items-center gap-2 flex-wrap">' +
        '<label class="d-flex align-items-center mb-0"><input type="checkbox" class="form-check-input me-1" id="' + cbName + '" name="' + cbName + '">' +
        '<span data-pg-i18n-lbl="cb:' + escDA(cbName) + '">' + escUi(L(String(field.label || ''))) + '</span></label>' +
        '<button type="button" class="btn btn-primary btn-sm screen-search-btn" data-pg-list-search-btn="1"><span data-pg-ui-t="' + escDA(String(searchLbl)) + '">' + escUi(L(String(searchLbl))) + '</span></button>' +
        '</div>';
      return wrapSearchCell(inner, false);
    }
    if (field.type === 'searchBtn') {
      var btnKey = String(field.label != null && field.label !== '' ? field.label : '검색');
      var btnLabel = L(btnKey);
      var iconHtml = field.noIcon ? '' : '<i class="bi bi-search"></i> ';
      var ccfg = cfg || {};
      var addInlineSort = ccfg.listSortDirToolbar !== false && !ccfg._listToolbarShowsSortDir;
      inner = '<div class="search-cell-input search-cell-input--right d-flex align-items-center flex-wrap gap-2 justify-content-end">';
      if (addInlineSort) inner += buildListSortDirSelectHtml(tabId);
      inner += '<button type="button" class="btn btn-primary btn-sm screen-search-btn" data-pg-list-search-btn="1">' + iconHtml + '<span data-pg-ui-t="' + escDA(btnKey) + '">' + escUi(btnLabel) + '</span></button></div>';
      return wrapSearchCell(inner, false);
    }
    if (field.type === 'button' && field.name === 'searchReset') {
      inner = '<div class="search-cell-input"><button type="button" class="btn btn-outline-secondary btn-sm search-reset-btn" data-pg-i18n-reset="1">' + escUi(L(String(field.label || '초기화'))) + '</button></div>';
      return wrapSearchCell(inner, false);
    }
    if (field.type === 'checkbox') {
      var cbName = field.name || '';
      inner = (field.label ? '<span class="search-cell-label" data-pg-i18n-lbl="' + escDA('cb:' + cbName) + '">' + escUi(L(String(field.label))) + '</span>' : '') + '<div class="search-cell-input"><input type="checkbox" class="form-check-input" id="' + cbName + '" name="' + cbName + '"></div>';
      return wrapSearchCell(inner, !!field.label);
    }
    if (field.type === 'select') {
      var selNm = field.name || '';
      var lblKeySel = field.i18nLblKey || selNm;
      var opts = (field.options || []).map(function (o) {
        var ov = o.v != null ? String(o.v) : '';
        var optAttr = selNm ? ' data-pg-i18n-opt="' + escDA(selNm + '|' + ov) + '"' : '';
        return '<option value="' + (o.v || '') + '"' + optAttr + '>' + escUi(L(String(o.t != null ? o.t : o.v || ''))) + '</option>';
      }).join('');
      var lblSel = '';
      if (field.label) {
        lblSel = '<span class="search-cell-label"' + (lblKeySel ? ' data-pg-i18n-lbl="' + escDA(lblKeySel) + '"' : '') + '>' + escUi(L(String(field.label))) + '</span>';
      }
      inner = lblSel + '<div class="search-cell-input"><select class="form-control form-control-sm _searchChange" id="' + (field.name || '') + '" name="' + (field.name || '') + '" style="' + sz + '">' + opts + '</select></div>';
      return wrapSearchCell(inner, !!field.label);
    }
    if (field.type === 'text') {
      var txNm = field.name || '';
      var phKeyTx = field.i18nPhKey || txNm;
      var lblKeyTx = field.i18nLblKey || (txNm ? (txNm + ':label') : '');
      var lblTx = '';
      if (field.label) {
        lblTx = '<span class="search-cell-label"' + (lblKeyTx ? ' data-pg-i18n-lbl="' + escDA(lblKeyTx) + '"' : '') + '>' + escUi(L(String(field.label))) + '</span>';
      }
      var phAttr = phKeyTx ? ' data-pg-i18n-ph="' + escDA(phKeyTx) + '"' : '';
      inner = lblTx + '<div class="search-cell-input"><input type="text" class="form-control form-control-sm _searchText" id="' + (field.name || '') + '" name="' + (field.name || '') + '"' + phAttr + ' placeholder="' + escUi(L(String(field.placeholder || ''))) + '" style="' + sz + '"></div>';
      return wrapSearchCell(inner, !!field.label);
    }
    return '';
  }

  function renderSearchRow(row, cfg, tabId) {
    var cells = Array.isArray(row) ? row : (row ? [row] : []);
    if (cells.length === 0) return '';
    var html = cells.map(function (cell) { return renderSearchCell(cell, cfg, tabId); }).filter(Boolean).join('');
    return html ? '<div class="search-form-row">' + html + '</div>' : '';
  }

  function renderSearchForm(cfg, tabId) {
    var rows = cfg.searchRows || [];
    var rows2 = cfg.searchRows2 || [];
    var rows3 = cfg.searchRows3 || [];
    cfg._listToolbarHasSearchBtn = (cfg.buttons || []).some(function (b) { return b && String(b.id || '') === 'searchBtn'; });
    cfg._listToolbarShowsSortDir = false;
    if (cfg.listSortDirToolbar !== false) {
      var tbb = cfg.buttons || [];
      var hSearchBar = tbb.some(function (x) { return x && String(x.id || '') === 'searchBtn'; });
      var hRefreshBar = tbb.some(function (x) { return x && String(x.id || '') === 'payListRefreshBtn'; });
      if (cfg.listSortDirAnchor === 'refresh' && hRefreshBar) cfg._listToolbarShowsSortDir = true;
      else if (cfg.listSortDirAnchor !== 'refresh' && hSearchBar) cfg._listToolbarShowsSortDir = true;
    }
    var formClass = 'screen-search-form' + (cfg.searchFormClass ? ' ' + cfg.searchFormClass : '');
    var html = '<form id="screenSearchForm" class="' + formClass + '" onsubmit="return false;">';
    rows.forEach(function (r) { html += renderSearchRow(r, cfg, tabId); });
    rows2.forEach(function (r) { html += renderSearchRow(r, cfg, tabId); });
    rows3.forEach(function (r) { html += renderSearchRow(r, cfg, tabId); });
    if (cfg.settlementExecuteListModeField) {
      var tidH = (tabId || '').replace(/[^a-zA-Z0-9_-]/g, '_');
      html += '<input type="hidden" name="searchExecuteListMode" id="searchExecuteListMode_' + tidH + '" value="RECENT">';
    }
    html += '</form>';
    return html;
  }

  function renderNotice(cfg) {
    var list = cfg.noticeList || [];
    var refBtn = cfg.noticeRefButton;
    if (list.length === 0 && !refBtn) return '';
    function escNoticeAttr(s) {
      return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;');
    }
    var items = list.map(function (t, idx) {
      return '<li data-pg-notice-idx="' + idx + '" data-pg-ui-t="' + escNoticeAttr(String(t)) + '"></li>';
    }).join('');
    var noticeHtml = list.length > 0 ? '<ul class="mb-0">' + items + '</ul>' : '';
    var refKey = String(refBtn && refBtn.label != null && refBtn.label !== '' ? refBtn.label : '참고');
    var btnHtml = refBtn
      ? '<button type="button" class="btn btn-sm ' + (refBtn.cls || 'btn-success') + ' ms-2" id="' + (refBtn.id || 'noticeRefBtn') + '" data-pg-ui-t="' + escNoticeAttr(refKey) + '"></button>'
      : '';
    return '<div class="search-notice pg-hello-toggle-zone mb-2 d-flex align-items-center flex-wrap">' + (list.length > 0 ? '<div class="search-notice-text flex-grow-1">' + noticeHtml + '</div>' : '') + btnHtml + '</div>';
  }

  function renderTableColumnGuide(cfg) {
    if (cfg.tableColumnGuide === false || !cfg.columns || cfg.columns.length === 0) return '';
    /** 기본: 번호·업체·거래일시·Route No 등 결제 그리드 고정열 — VIEW 토글 제외. 화면별로 columnGuideFixedKeys 로 덮어쓸 수 있음(API연동설정은 Route 등 토글 가능). */
    var defaultFixed = ['rowNo', 'compId', 'compNm', 'compDivNm', 'trnDate', 'trnTime', 'routeNo'];
    var fixedKeys = (cfg.columnGuideFixedKeys && cfg.columnGuideFixedKeys.length) ? cfg.columnGuideFixedKeys : defaultFixed;
    var cols = cfg.columns.filter(function (c) {
      // payActions(후속조치)는 VIEW SETTING에서 토글 가능해야 함(통합 결제내역 등). 체크박스·인라인 액션만 제외.
      if (c.type === 'checkbox' || c.type === 'commissionInlineActions' || c.type === 'accountAccessActions' || c.type === 'accountAccessDelete' || c.type === 'userResetPassword' || c.type === 'userDelete' || c.type === 'payoutHoldReleaseBtn' || c.type === 'pgApiMngRowActions') return false;
      return fixedKeys.indexOf(c.key) === -1;
    });
    if (cols.length === 0) return '';
    var escGl = function (s) {
      return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
    };
    var items = cols.map(function (c) {
      var key = c.key || '';
      var label = c.columnGuideLabel || c.label || c.key;
      return '<label class="column-guide-item column-guide-item--on"><input type="checkbox" class="column-guide-check" data-key="' + escGl(key) + '" checked> <span class="column-guide-label">' + escGl(L(String(label))) + '</span></label>';
    }).join('');
    var actionsHtml =
      '<button type="button" class="btn btn-xs btn-outline-secondary column-guide-action-btn" id="compMngDefaultColumnsBtn" data-pg-i18n-cg-act="default">' + escGl(L('기본')) + '</button>' +
      '<button type="button" class="btn btn-xs btn-outline-secondary column-guide-action-btn" id="compMngReleaseColumnsBtn" data-pg-i18n-cg-act="release">' + escGl(L('해제')) + '</button>' +
      '<button type="button" class="btn btn-xs btn-outline-secondary column-guide-action-btn" id="compMngSelectAllColumnsBtn" data-pg-i18n-cg-act="selectAllCols">' + escGl(L('선택')) + '</button>' +
      '<button type="button" class="btn btn-xs btn-outline-primary column-guide-action-btn" id="compMngSaveColumnsBtn" data-pg-i18n-cg-act="save">' + escGl(L('저장')) + '</button>' +
      '<button type="button" class="btn btn-xs btn-outline-secondary column-guide-action-btn" id="compMngRestoreColumnsBtn" data-pg-i18n-cg-act="restore" data-pg-i18n-cg-title="restoreTip" title="' + escGl(L('바로 직전에 서버에 저장된 열 구성(또는 화면을 불러온 당시의 저장 상태)으로 되돌립니다.')) + '">' + escGl(L('복원')) + '</button>';
    var headRow =
      '<div class="column-guide-row column-guide-top column-guide-top--inline">' +
      '<div class="column-guide-inline-head">' +
      '<span class="column-guide-title" data-pg-i18n-cg-act="viewSettingTitle">VIEW SETTING</span>' +
      '<span class="column-guide-vbar" aria-hidden="true">|</span>' +
      '<div class="column-guide-actions column-guide-actions--inline">' + actionsHtml + '</div>' +
      '</div></div>';
    var rootClass = 'table-column-guide pg-hello-toggle-zone mb-3 p-2 border rounded bg-light table-column-guide--inline-head table-column-guide--two-row';
    return '<div class="' + rootClass + '" id="tableColumnGuide">' +
      headRow +
      '<div class="column-guide-row column-guide-checkboxes">' +
      '<div class="column-guide-list">' + items + '</div>' +
      '</div>' +
      '</div>';
  }

  function renderFormField(f, readonlyAttr) {
    var hqC = f.hideForHeadquarters ? ' comp-info-hide-if-hq' : '';
    var hqPolicyC = f.hqPolicyOnly ? ' hq-policy-only' : '';
    if (f.type === 'hidden') {
      return '<input type="hidden" name="' + (f.name || '') + '" id="' + (f.name || '') + '">';
    }
    if (f.type === 'note') {
      var colN = f.col || 12;
      var noteText = String(f.text != null ? f.text : (f.label || ''));
      if (!noteText) return '';
      if (pgUiNoticeHasHtml(noteText)) {
        return '<div class="col-sm-' + colN + '"><div class="text-muted small mb-2" data-pg-ui-html="' + escUi(noteText) + '">' + L(noteText) + '</div></div>';
      }
      return '<div class="col-sm-' + colN + '"><p class="text-muted small mb-2" data-pg-ui-t="' + escUi(noteText) + '">' + escUi(L(noteText)) + '</p></div>';
    }
    if (f.type === 'customHtml') {
      var colH = f.col || 12;
      var htmlBody = typeof f.html === 'function' ? f.html() : (f.html || '');
      return '<div class="col-sm-' + colH + '">' + htmlBody + '</div>';
    }
    if (f.type === 'notifyPairButton') {
      var colPair = f.col || 2;
      var cbF = f.callbackField || 'notifyUrl1';
      var rsF = f.resultField || 'notifyUrl2';
      var pairKey = String(f.pairLabel != null ? f.pairLabel : '노티 쌍');
      var btnKey = String(f.buttonText != null ? f.buttonText : 'CALLBACK+RESULT 선택');
      var hintKey = f.hint != null ? String(f.hint) : '';
      var titleHintKey = f.titleHint ? String(f.titleHint) : '';
      var titleAttr = titleHintKey ? (' data-pg-ui-title="' + escUi(titleHintKey) + '" title="' + escUi(L(titleHintKey)) + '"') : '';
      var pairBtnDis = f.readonly ? ' disabled' : '';
      return '<div class="col-sm-' + colPair + ' form-field-block comp-notify-pair-inline">' +
        '<label class="form-label comp-notify-pair-inline-label"><span data-pg-ui-t="' + escUi(pairKey) + '">' + escUi(L(pairKey)) + '</span></label>' +
        '<button type="button" class="btn btn-outline-primary btn-sm w-100 comp-notify-pair-inline-btn"' + titleAttr + pairBtnDis +
        ' data-action="노티쌍선택" data-callback-field="' + cbF + '" data-result-field="' + rsF + '"><span data-pg-ui-t="' + escUi(btnKey) + '">' + escUi(L(btnKey)) + '</span></button>' +
        (hintKey ? '<p class="text-muted small mb-0 mt-1 comp-notify-pair-inline-hint" data-pg-ui-t="' + escUi(hintKey) + '">' + escUi(L(hintKey)) + '</p>' : '') +
        '</div>';
    }
    if (f.type === 'assistantPasswordManage') {
      var colAp = f.col || 2;
      var asstHint = '입력 후 [저장]으로 확정한 뒤 하단 [수정 저장]으로 반영하세요.';
      return '<div class="col-sm-' + colAp + ' form-field-block' + hqC + hqPolicyC + '">' + pgUiFormLabelSpan('비밀번호', false) +
        '<div id="assistantPwdInitialRow">' +
        '<div class="form-input-with-btn"><span class="form-input-wrap">' +
        '<input type="password" class="form-control form-control-sm" name="assistantPwd" id="assistantPwd" autocomplete="new-password" placeholder="' + escUi(L('8자 이상')) + '" data-pg-ui-placeholder="8자 이상">' +
        '</span><button type="button" class="btn btn-outline-secondary btn-sm" data-field="assistantPwd" data-action="저장"><span data-pg-ui-t="저장">' + escUi(L('저장')) + '</span></button></div>' +
        '<p class="text-muted small mb-0 mt-1" data-pg-ui-t="' + escUi(asstHint) + '">' + escUi(L(asstHint)) + '</p></div>' +
        '<div id="assistantPwdResetRow" class="d-none">' +
        '<div class="form-input-with-btn"><button type="button" class="btn btn-outline-secondary btn-sm" id="assistantPwdResetBtn" data-action="보조 비밀번호 초기화"><span data-pg-ui-t="비밀번호 초기화">' + escUi(L('비밀번호 초기화')) + '</span></button></div></div></div>';
    }
    if (f.type === 'passwordReset') {
      var col = f.col || 2;
      var pwdLabelCore = (f.label || '비밀번호').replace(/\*$/, '');
      var pwdStar = !!(f.label && f.label.indexOf('*') !== -1);
      return '<div class="col-sm-' + col + ' form-field-block' + hqC + hqPolicyC + '">' + pgUiFormLabelSpan(pwdLabelCore, pwdStar) + '<div class="form-input-with-btn"><button type="button" class="btn btn-outline-secondary btn-sm" id="compDetailPwdResetBtn" data-action="비밀번호 초기화"><span data-pg-ui-t="비밀번호 초기화">' + escUi(L('비밀번호 초기화')) + '</span></button></div></div>';
    }
    var isRequired = !!(f.required || (f.label && f.label.indexOf('*') !== -1));
    var reqClass = isRequired ? ' required-input' : '';
    if (f.type === 'regNoWithType') {
      var col = f.col || 2;
      var labelCoreR = (f.label || '사업자번호').replace(/\*$/, '');
      var starR = !!(f.label && f.label.indexOf('*') !== -1);
      return '<div class="col-sm-' + col + ' form-field-block' + hqC + hqPolicyC + '">' + pgUiFormLabelSpan(labelCoreR, starR) +
        '<div class="d-flex gap-1 align-items-center"><select class="form-control form-control-sm' + reqClass + '" name="regType" id="regType" style="width:auto;min-width:70px"><option value="CORP" data-pg-ui-t="법인">' + escUi(L('법인')) + '</option><option value="PERSONAL" data-pg-ui-t="개인">' + escUi(L('개인')) + '</option></select>' +
        '<input type="text" class="form-control form-control-sm flex-grow-1' + reqClass + '" name="' + (f.name || 'regNo') + '" id="' + (f.name || 'regNo') + '" placeholder="' + escUi(L('번호 입력')) + '" data-pg-ui-placeholder="번호 입력"></div></div>';
    }
    var col = f.col || 2;
    var req = (f.label && f.label.indexOf('*') !== -1) ? '' : '';
    var labelCore0 = (f.label || '').replace(/\*$/, '');
    var hasStar0 = !!(f.label && f.label.indexOf('*') !== -1);
    var name = f.name || '';
    var id = name;
    var ro = (readonlyAttr || f.readonly) ? ' readonly' : '';
    var inp = '';
    var intlPhoneTargets = { ceoMobile: true, compTel: true, fax: true, settleTelNo: true, contactTel: true };
    var useIntlPhone = (f.type === 'phoneIntl') || ((f.type === 'text') && !!intlPhoneTargets[name]);
    var isWideTime = false;
    if (useIntlPhone) {
      var intlOptions = window.PG_INTL_PHONE_OPTIONS || '<option value="+81">Japan (+81)</option><option value="+82">South Korea (+82)</option><option value="+66">Thailand (+66)</option><option value="+1">United States (+1)</option><option value="+86">China (+86)</option><option value="+65">Singapore (+65)</option><option value="+852">Hong Kong (+852)</option><option value="" disabled>---------------</option>';
      var ccName = '__phone_cc_' + name;
      var numName = '__phone_num_' + name;
      inp = '<div class="d-flex gap-1 align-items-center intl-phone-field" data-intl-phone-group="' + name + '">' +
        '<input type="hidden" name="' + name + '" id="' + id + '">' +
        '<select class="form-control form-control-sm' + reqClass + '" name="' + ccName + '" data-intl-phone-code-for="' + name + '"' + (f.readonly ? ' disabled' : '') + '>' + intlOptions + '</select>' +
        '<input type="text" class="form-control form-control-sm' + reqClass + '" name="' + numName + '" data-intl-phone-number-for="' + name + '"' + (f.placeholder
          ? (' placeholder="' + escUi(L(String(f.placeholder))) + '" data-pg-ui-placeholder="' + escUi(String(f.placeholder)) + '"')
          : (' placeholder="' + escUi(L('전화번호')) + '" data-pg-ui-placeholder="전화번호"')) + ro + '>' +
        '</div>';
    } else if (f.type === 'number') {
      var numStep = (f.step != null && f.step !== '') ? String(f.step) : '1';
      var numMin = (f.min != null && f.min !== '') ? String(f.min) : '0';
      var numMaxAttr = (f.max != null && f.max !== '') ? (' max="' + String(f.max) + '"') : '';
      var numPh = f.placeholder ? (' placeholder="' + escUi(L(String(f.placeholder))) + '" data-pg-ui-placeholder="' + escUi(String(f.placeholder)) + '"') : '';
      inp = '<input type="number" min="' + numMin + '" step="' + numStep + '"' + numMaxAttr + ' class="form-control form-control-sm' + reqClass + '" name="' + name + '" id="' + id + '"' + numPh + ro + '>';
    } else if (f.type === 'date') {
      inp = '<input type="date" lang="en-CA" class="form-control form-control-sm pg-date-input-iso' + reqClass + '" name="' + name + '" id="' + id + '"' + ro + '>';
    } else if (f.type === 'text' || f.type === 'password') {
      var txPh = f.placeholder ? (' placeholder="' + escUi(L(String(f.placeholder))) + '" data-pg-ui-placeholder="' + escUi(String(f.placeholder)) + '"') : '';
      inp = '<input type="' + (f.type || 'text') + '" class="form-control form-control-sm' + reqClass + '" name="' + name + '" id="' + id + '"' + txPh + ro + '>';
    } else if (f.type === 'time') {
      var isSettlementTime = (name === 'calcCloseTime' || name === 'calcStartTime' || name === 'transferExecTime');
      var isWithdrawLimitTime = (name === 'withdrawRestrictStartTime' || name === 'withdrawRestrictEndTime' || name === 'withdrawStartTime' || name === 'withdrawEndTime');
      isWideTime = (isSettlementTime || isWithdrawLimitTime);
      var wideTime = isWideTime ? ' settle-time-wide' : '';
      var tmPh = f.placeholder ? (' placeholder="' + escUi(L(String(f.placeholder))) + '" data-pg-ui-placeholder="' + escUi(String(f.placeholder)) + '"') : '';
      inp = '<input type="time" class="form-control form-control-sm' + reqClass + wideTime + '" name="' + name + '" id="' + id + '"' + tmPh + '>';
    } else if (f.type === 'select') {
      var opts = (f.options || []).map(function (o) {
        var ok = String(o.t != null ? o.t : o.v || '');
        return '<option value="' + escUi(String(o.v || '')) + '"' + (ok ? ' data-pg-ui-t="' + escUi(ok) + '"' : '') + '>' + escUi(L(ok)) + '</option>';
      }).join('');
      var selAttrs = (f.readonly ? ' disabled' : '')
        + (f.loadCountries ? ' data-load-countries="true"' : '')
        + (f.bankByCountry ? ' data-bank-by-country="true"' : '')
        + (f.loadNotifyTargets ? ' data-load-notify-targets="true"' : '')
        + (f.loadRegionalBranches ? ' data-load-regional-branches="true"' : '');
      inp = '<select class="form-control form-control-sm' + reqClass + '" name="' + name + '" id="' + id + '"' + selAttrs + '>' + opts + '</select>';
    } else if (f.type === 'textarea') {
      var taRows = f.rows != null ? Math.max(2, parseInt(f.rows, 10) || 3) : 3;
      var taPh = f.placeholder ? (' placeholder="' + escUi(L(String(f.placeholder))) + '" data-pg-ui-placeholder="' + escUi(String(f.placeholder)) + '"') : '';
      inp = '<textarea class="form-control form-control-sm' + reqClass + '" name="' + name + '" id="' + id + '" rows="' + taRows + '"' + taPh + ro + '></textarea>';
    } else if (f.type === 'file') {
      if (name === 'attach') {
        inp = '<div class="attach-section" data-attach-section="1">' +
          '<input type="hidden" name="attachListJson" data-attach-json value="[]">' +
          '<div class="attach-toolbar">' +
          '<input type="text" class="form-control form-control-sm attach-display-name" data-attach-display-name placeholder="' + escUi(L('파일명 (예: 사업자등록증)')) + '" data-pg-ui-placeholder="파일명 (예: 사업자등록증)">' +
          '<label class="btn btn-outline-secondary btn-sm attach-file-pick mb-0">' +
          '<span data-attach-file-label data-pg-ui-t="파일 선택">' + escUi(L('파일 선택')) + '</span>' +
          '<input type="file" class="d-none" data-attach-file accept=".png,.jpg,.jpeg,.gif,.webp,.bmp,.pdf,.doc,.docx,.hwp,.hwpx,.txt,.xls,.xlsx,.ppt,.pptx">' +
          '</label>' +
          '<button type="button" class="btn btn-primary btn-sm" data-attach-add><span data-pg-ui-t="추가">' + escUi(L('추가')) + '</span></button>' +
          '</div>' +
          '<div class="table-responsive attach-table-wrap table-no-col-resize-wrap">' +
          '<table class="table table-sm table-bordered mb-0 w-100 table-no-col-resize" data-attach-table>' +
          '<thead><tr><th style="width:56px">No.</th><th><span data-pg-ui-t="파일이름">' + escUi(L('파일이름')) + '</span></th><th><span data-pg-ui-t="첨부된 파일">' + escUi(L('첨부된 파일')) + '</span></th><th style="width:80px"><span data-pg-ui-t="수정">' + escUi(L('수정')) + '</span></th><th style="width:80px"><span data-pg-ui-t="삭제">' + escUi(L('삭제')) + '</span></th></tr></thead>' +
          '<tbody><tr data-empty-row><td colspan="5" class="text-center text-muted py-2" data-pg-ui-t="첨부된 파일이 없습니다.">' + escUi(L('첨부된 파일이 없습니다.')) + '</td></tr></tbody>' +
          '</table></div>' +
          '<p class="text-muted small mt-1 mb-0" data-pg-ui-t="허용 파일: 이미지, PDF, 문서 파일(doc/docx/hwp/hwpx/txt/xls/xlsx/ppt/pptx)">' + escUi(L('허용 파일: 이미지, PDF, 문서 파일(doc/docx/hwp/hwpx/txt/xls/xlsx/ppt/pptx)')) + '</p>' +
          '</div>';
      } else {
        inp = '<input type="file" class="form-control form-control-sm" name="' + name + '" id="' + id + '">';
      }
    } else {
      var defPh = f.placeholder ? (' placeholder="' + escUi(L(String(f.placeholder))) + '" data-pg-ui-placeholder="' + escUi(String(f.placeholder)) + '"') : '';
      inp = '<input type="text" class="form-control form-control-sm' + reqClass + '" name="' + name + '" id="' + id + '"' + defPh + ro + '>';
    }
    var inpWrap = inp;
    if (f.button) {
      var sideBtnDis = f.readonly ? ' disabled' : '';
      var btnKey0 = String(f.button);
      inpWrap = '<div class="form-input-with-btn"><span class="form-input-wrap">' + inp + '</span><button type="button" class="btn btn-outline-secondary btn-sm"' + sideBtnDis + ' data-field="' + name + '" data-action="' + escUi(btnKey0) + '"><span data-pg-ui-t="' + escUi(btnKey0) + '">' + escUi(L(btnKey0)) + '</span></button></div>';
    }
    if (f.smsButton) {
      var smsCls = 'btn-outline-primary';
      if (f.smsColor === 'warning') smsCls = 'btn-outline-warning';
      else if (f.smsColor === 'success') smsCls = 'btn-outline-success';
      else if (f.smsColor === 'secondary') smsCls = 'btn-outline-secondary';
      inpWrap = '<div class="form-input-with-btn"><span class="form-input-wrap">' + inp + '</span><button type="button" class="btn ' + smsCls + ' btn-sm" data-field="' + name + '"><span data-pg-ui-t="SMS수신">' + escUi(L('SMS수신')) + '</span></button></div>';
    }
    var blockClass = 'col-sm-' + col + ' form-field-block';
    if (f.voidRefundSettlementModeField) blockClass += ' commission-void-refund-mode-field';
    if (f.customOnly) blockClass += ' commission-custom-only';
    if (f.holdRateOnly) blockClass += ' hold-rate-custom-only';
    if (f.feeVatRateOnly) blockClass += ' fee-vat-rate-only';
    if (isWideTime) blockClass += ' settle-time-wide-block';
    if (f.blockExtraClass) blockClass += ' ' + String(f.blockExtraClass);
    return '<div class="' + blockClass + hqC + hqPolicyC + '">' + pgUiFormLabelSpan(labelCore0, hasStar0) + inpWrap + '</div>';
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
      var compRegTopHint = '업체구분을 선택하시면 해당 등록 유형에 맞는 입력 창이 표시됩니다. (총판/지사/대리점/가맹점)';
      html += '<div class="comp-div-hint alert alert-info py-2 mb-3" role="alert">' +
        '<small><span data-pg-ui-t="' + escUi(compRegTopHint) + '">' + escUi(L(compRegTopHint)) + '</span></small></div>';
    }
    sections.forEach(function (sec) {
      var cardClass = 'card mb-3';
      if (sec.merchantOnly) cardClass += ' merchant-only-section d-none';
      else if (sec.regionalOnly) cardClass += ' regional-only-section d-none';
      else if (sec.masterDistOnly) cardClass += ' master-dist-only-section d-none';
      else if (sec.regionalOrMasterDistOnly) cardClass += ' regional-or-master-dist-only-section d-none';
      else if (sec.headOfficeTierOnly) cardClass += ' head-office-tier-only-section d-none';
      else if (sec.merchantRegionalMasterCommission) cardClass += ' merchant-regional-master-commission-section d-none';
      else if (sec.distributorOnly) cardClass += ' distributor-only-section d-none';
      else if (sec.distributorMerchantOnlyNoRegional) cardClass += ' distributor-merchant-no-regional-section d-none';
      else if (sec.distributorOrMerchantOnly) cardClass += ' distributor-or-merchant-section d-none';
      if (sec.branchAgencySalesHide) cardClass += ' branch-agency-sales-hide-section';
      if (sec.cardExtraClass) cardClass += ' ' + String(sec.cardExtraClass);
      var cardId = sec.id ? ' id="' + sec.id + '"' : '';
      var titleKo = String(sec.title || '');
      html += '<div' + cardId + ' class="' + cardClass + '"><div class="card-header"><span data-pg-ui-t="' + escUi(titleKo) + '">' + escUi(L(titleKo)) + '</span></div><div class="card-body">';
      /* notice: <strong>/<code> 등 HTML 포함 시 data-pg-ui-html. 순수 텍스트는 p + data-pg-ui-t */
      if (sec.notice) {
        var n = String(sec.notice);
        if (pgUiNoticeHasHtml(n)) {
          html += '<div class="text-muted small mb-2 screen-section-notice" data-pg-ui-html="' + escUi(n) + '">' + L(n) + '</div>';
        } else {
          html += '<p class="text-muted small mb-2" data-pg-ui-t="' + escUi(n) + '">' + escUi(L(n)) + '</p>';
        }
      }
      if (sec.type === 'branding') {
        html += '<p class="text-danger small mb-2" data-pg-ui-t="메인이미지는 5MB, 로고·URL결제·파비콘 이미지는 1MB까지 업로드 가능합니다. 파비콘은 PNG/JPG 업로드 시 32x32 PNG로 자동 변환되어 적용됩니다.">메인이미지는 5MB, 로고·URL결제·파비콘 이미지는 1MB까지 업로드 가능합니다. 파비콘은 PNG/JPG 업로드 시 32x32 PNG로 자동 변환되어 적용됩니다.</p>' +
          '<div class="row mb-2"><div class="col-sm-6"><label class="form-label" data-pg-ui-t="메인이미지">메인이미지</label><div class="input-group input-group-sm">' +
          '<input type="text" class="form-control form-control-sm" name="mainImageUrl" id="brandingMainImageUrl" readonly data-pg-ui-placeholder="업로드 파일명" placeholder="업로드 파일명">' +
          '<input type="file" class="d-none" id="brandingMainImageFile" accept="image/png,image/jpeg,image/jpg">' +
          '<button type="button" class="btn btn-outline-secondary" id="brandingMainImageBrowse">Browse</button><button type="button" class="btn btn-outline-danger" id="brandingMainImageDelete" data-pg-ui-t="삭제">삭제</button></div></div></div>' +
          '<div class="row mb-2"><div class="col-sm-6"><label class="form-label" data-pg-ui-t="첫화면 로고이미지(로그인 페이지)">첫화면 로고이미지(로그인 페이지)</label><div class="input-group input-group-sm">' +
          '<input type="text" class="form-control form-control-sm" name="firstLogoImageUrl" id="brandingFirstLogoImageUrl" readonly data-pg-ui-placeholder="업로드 파일명" placeholder="업로드 파일명">' +
          '<input type="file" class="d-none" id="brandingFirstLogoImageFile" accept="image/png,image/jpeg,image/jpg">' +
          '<button type="button" class="btn btn-outline-secondary" id="brandingFirstLogoImageBrowse">Browse</button><button type="button" class="btn btn-outline-danger" id="brandingFirstLogoImageDelete" data-pg-ui-t="삭제">삭제</button></div></div></div>' +
          '<div class="row mb-2"><div class="col-sm-6"><label class="form-label" data-pg-ui-t="로그인 후 로고이미지(좌측 메뉴)">로그인 후 로고이미지(좌측 메뉴)</label><div class="input-group input-group-sm">' +
          '<input type="text" class="form-control form-control-sm" name="logoImageUrl" id="brandingLogoImageUrl" readonly data-pg-ui-placeholder="업로드 파일명" placeholder="업로드 파일명">' +
          '<input type="file" class="d-none" id="brandingLogoImageFile" accept="image/png,image/jpeg,image/jpg">' +
          '<button type="button" class="btn btn-outline-secondary" id="brandingLogoImageBrowse">Browse</button><button type="button" class="btn btn-outline-danger" id="brandingLogoImageDelete" data-pg-ui-t="삭제">삭제</button></div></div></div>' +
          '<div class="row mb-2"><div class="col-sm-6"><label class="form-label" data-pg-ui-t="URL결제이미지(공개 결제 페이지 상단)">URL결제이미지(공개 결제 페이지 상단)</label><div class="input-group input-group-sm">' +
          '<input type="text" class="form-control form-control-sm" name="urlPayImageUrl" id="brandingUrlPayImageUrl" readonly data-pg-ui-placeholder="업로드 파일명" placeholder="업로드 파일명">' +
          '<input type="file" class="d-none" id="brandingUrlPayImageFile" accept="image/png,image/jpeg,image/jpg">' +
          '<button type="button" class="btn btn-outline-secondary" id="brandingUrlPayImageBrowse">Browse</button><button type="button" class="btn btn-outline-danger" id="brandingUrlPayImageDelete" data-pg-ui-t="삭제">삭제</button></div>' +
          '<div class="form-text text-muted small" data-pg-ui-t="비우면 URL 결제 상단에는 「로그인 후 로고」가 표시됩니다. 총판(상위) 브랜딩이 가맹점 결제 URL에 적용됩니다.">비우면 URL 결제 상단에는 「로그인 후 로고」가 표시됩니다. 총판(상위) 브랜딩이 가맹점 결제 URL에 적용됩니다.</div></div></div>' +
          '<div class="row mb-2"><div class="col-sm-6"><label class="form-label" data-pg-ui-t="파비콘 이미지">파비콘 이미지</label><div class="input-group input-group-sm">' +
          '<input type="text" class="form-control form-control-sm" name="popconImageUrl" id="brandingPopconImageUrl" readonly data-pg-ui-placeholder="업로드 파일명" placeholder="업로드 파일명">' +
          '<input type="file" class="d-none" id="brandingPopconImageFile" accept="image/png,image/jpeg,image/jpg">' +
          '<button type="button" class="btn btn-outline-secondary" id="brandingPopconImageBrowse">Browse</button><button type="button" class="btn btn-outline-danger" id="brandingPopconImageDelete" data-pg-ui-t="삭제">삭제</button></div></div></div>' +
          '<div class="row mb-2"><div class="col-sm-4"><label class="form-label" data-pg-ui-t="배경테마">배경테마</label><select class="form-control form-control-sm" name="brandingTheme" id="brandingTheme">' +
          '<option value="DEFAULT" data-pg-ui-t="기본(현재)">기본(현재)</option><option value="LIGHT" data-pg-ui-t="Light (흰배경/검정글씨)">Light (흰배경/검정글씨)</option><option value="GRAY" data-pg-ui-t="Gray (라이트·다크 중간 톤)">Gray (라이트·다크 중간 톤)</option><option value="BROWN" data-pg-ui-t="Brown (상단 메뉴 톤 정렬)">Brown (상단 메뉴 톤 정렬)</option><option value="DARK" data-pg-ui-t="Dark (어두운배경/흰글씨)">Dark (어두운배경/흰글씨)</option>' +
          '<option value="PASTEL_1" data-pg-ui-t="파스텔1">파스텔1</option><option value="PASTEL_2" data-pg-ui-t="파스텔2">파스텔2</option><option value="PASTEL_3" data-pg-ui-t="파스텔3">파스텔3</option><option value="PASTEL_4" data-pg-ui-t="파스텔4">파스텔4</option><option value="PASTEL_5" data-pg-ui-t="파스텔5">파스텔5</option>' +
          '</select></div></div>' +
          '<div class="row mb-2"><div class="col-sm-8"><label class="form-label" data-pg-ui-t="사이트 이름(브라우저 탭)">사이트 이름(브라우저 탭)</label><input type="text" class="form-control form-control-sm" name="siteName" id="brandingSiteName" maxlength="100" data-pg-ui-placeholder="예: OTL PAY 관리자" placeholder="예: OTL PAY 관리자"></div></div>' +
          '<div class="row mb-2"><div class="col-sm-8"><label class="form-label" data-pg-ui-t="로그인 안내 호스트">로그인 안내 호스트</label><input type="text" class="form-control form-control-sm" name="brandHost" id="brandingBrandHost" data-pg-ui-placeholder="예: api.example.com (선택)" placeholder="예: api.example.com (선택)"></div></div>';
      } else if (sec.type === 'pgBindingList') {
        var omitExtSettleCols = !!sec.omitExtSettleColumns;
        html += '<div class="pg-binding-list-wrap"' + (omitExtSettleCols ? ' data-pg-omit-ext-settle-cols="1"' : '') + '><table class="table table-sm table-bordered pg-binding-table"><thead><tr>' +
          '<th data-pg-ui-t="운영">운영</th><th data-pg-ui-t="착신화">착신화</th><th data-pg-ui-t="결제대행사">결제대행사</th><th data-pg-ui-t="결제구분">결제구분</th><th>MID</th><th data-pg-ui-t="루트번호">루트번호</th><th style="min-width:12rem">API KEY</th><th style="min-width:12rem">IV KEY</th><th data-pg-ui-t="할부">할부</th><th data-pg-ui-t="최대할부">최대할부</th><th data-pg-ui-t="카드브랜드">카드브랜드</th>' +
          (omitExtSettleCols ? '' : '<th data-pg-ui-title="비우면 연동(tb_pg_agency) 기본" title="비우면 연동(tb_pg_agency) 기본" data-pg-ui-t="예정모드">예정모드</th><th>N</th><th data-pg-ui-t="D시각">D시각</th>') +
          '<th style="min-width:200px" data-pg-ui-t="작업">작업</th></tr></thead><tbody id="pgBindingTbody"></tbody></table>' +
          '<button type="button" class="btn btn-outline-primary btn-sm mt-2" id="pgBindingAddBtn" data-pg-ui-t="+ 결제대행사 추가">+ 결제대행사 추가</button>' +
          '<input type="hidden" name="pgBindings" id="pgBindingsHidden" value="[]"></div>';
      } else if (sec.type === 'pgInfoDisplay') {
        html += '<div id="pgInfoDisplayWrap" class="pg-info-display">' +
          '<div class="row mb-2"><div class="col-sm-3"><label class="form-label" data-pg-ui-t="웹결제">웹결제</label><select class="form-control form-control-sm" name="webPaymentUseYn"><option value="Y" data-pg-ui-t="사용">사용</option><option value="N" data-pg-ui-t="미사용">미사용</option></select></div>' +
          '<div class="col-sm-5"><label class="form-label" data-pg-ui-t="결제 URL">결제 URL</label><div class="input-group input-group-sm"><input type="text" class="form-control" id="paymentUrlDisplay" readonly data-pg-ui-placeholder="가맹점 선택 후 조회" placeholder="가맹점 선택 후 조회"><button type="button" class="btn btn-outline-primary" id="paymentUrlCopyBtn" data-pg-ui-t="복사">복사</button></div></div></div>' +
          '<div class="row mb-2">' +
          '<div class="col-sm-5"><label class="form-label" data-pg-ui-t="URL 재결제 URL">URL 재결제 URL</label><div class="input-group input-group-sm"><input type="text" class="form-control" id="paymentRepayUrlDisplay" readonly data-pg-ui-placeholder="가맹점 선택 후 조회" placeholder="가맹점 선택 후 조회"><button type="button" class="btn btn-outline-primary" id="paymentRepayUrlCopyBtn" data-pg-ui-t="복사">복사</button></div></div></div>' +
          '<div class="row mb-2"><div class="col-sm-3"><label class="form-label" data-pg-ui-t="챗봇결제 사용여부">챗봇결제 사용여부</label><select class="form-control form-control-sm" name="chatbotPaymentUseYn"><option value="N" data-pg-ui-t="미사용">미사용</option><option value="Y" data-pg-ui-t="사용">사용</option></select></div>' +
          '<div class="col-sm-3"><label class="form-label" data-pg-ui-t="챗봇 상품등록 한도(건)">챗봇 상품등록 한도(건)</label><select class="form-control form-control-sm" name="chatbotProductSlotLimit"><option value="">—</option>' +
          '<option value="10">10</option><option value="20">20</option><option value="50">50</option><option value="80">80</option><option value="100">100</option><option value="150">150</option><option value="200">200</option></select></div>' +
          '<div class="col-sm-5"><label class="form-label" data-pg-ui-t="챗봇결제 URL">챗봇결제 URL</label><div class="input-group input-group-sm"><input type="text" class="form-control" id="chatbotPaymentUrlDisplay" readonly data-pg-ui-placeholder="가맹점 선택 후 조회" placeholder="가맹점 선택 후 조회"><button type="button" class="btn btn-outline-primary" id="chatbotPaymentUrlCopyBtn" data-pg-ui-t="복사">복사</button></div></div></div>' +
          merchantChatbotEmbedScriptRowHtml('가맹점 선택 후 조회') +
          merchantChatbotQrRowHtml() +
          '</div>';
      } else if (sec.type === 'regionalCardLimitTable') {
        html += '<div class="d-flex justify-content-end mb-2"><button type="button" class="btn btn-success btn-sm me-1" id="regionalCardLimitAddBtn" data-pg-ui-t="추가">추가</button><button type="button" class="btn btn-danger btn-sm" id="regionalCardLimitDelBtn" data-pg-ui-t="삭제">삭제</button></div>' +
          '<div class="table-responsive"><table class="table table-sm table-bordered"><thead class="table-info"><tr>' +
          '<th style="width:40px"><input type="checkbox" class="regional-card-limit-check-all" data-pg-ui-title="전체선택" title="전체선택"></th>' +
          '<th class="text-danger" data-pg-ui-t="결제구분">결제구분</th><th class="text-danger" data-pg-ui-t="카드사">카드사</th><th class="text-danger" data-pg-ui-t="일">일</th><th class="text-danger" data-pg-ui-t="회">회</th><th class="text-danger" data-pg-ui-t="원">원</th><th class="text-danger" data-pg-ui-t="등록사유">등록사유</th><th data-pg-ui-t="등록일자">등록일자</th><th data-pg-ui-t="수정일자">수정일자</th><th data-pg-ui-t="비고">비고</th></tr></thead>' +
          '<tbody id="regionalCardLimitTbody"></tbody></table></div>' +
          '<div class="text-center text-muted py-2 empty-table-msg" id="regionalCardLimitEmpty" data-pg-ui-t="조회된 데이터가 없습니다.">조회된 데이터가 없습니다.</div>' +
          '<input type="hidden" name="regionalCardLimits" id="regionalCardLimitsHidden" value="[]">';
      } else if (sec.type === 'regionalTerminalTable') {
        html += '<div class="table-responsive"><table class="table table-sm table-bordered"><thead class="table-info"><tr>' +
          '<th>No.</th><th data-pg-ui-t="결제대행사">결제대행사</th><th data-pg-ui-t="터미널ID">터미널ID</th><th data-pg-ui-t="비고">비고</th></tr></thead>' +
          '<tbody id="regionalTerminalTbody"></tbody></table></div>' +
          '<div class="text-center text-muted py-2 empty-table-msg" id="regionalTerminalEmpty" data-pg-ui-t="조회된 데이터가 없습니다.">조회된 데이터가 없습니다.</div>' +
          '<button type="button" class="btn btn-outline-primary btn-sm mt-2" id="regionalTerminalAddBtn" data-pg-ui-t="+ 터미널 추가">+ 터미널 추가</button>' +
          '<input type="hidden" name="regionalTerminals" id="regionalTerminalsHidden" value="[]">';
      } else {
        (sec.rows || []).forEach(function (row) {
          var first = (row || [])[0];
          if (first && first.type === 'countryAddressRow') {
            var opt = first;
            function _addrKey(raw, def) {
              return String(raw || def || '').replace(/\*$/, '');
            }
            function _addrStar(raw) {
              return !!(raw && String(raw).indexOf('*') !== -1);
            }
            var zipRaw = opt.zipLabel || '우편번호*';
            var addrRaw = opt.addrLabel || '주소*';
            var addrDetRaw = opt.addrDetailLabel || '상세주소';
            var addrEtcL = opt.addrEtcLabel;
            html += '<div class="row country-address-row" data-country-address="true">' +
              '<div class="col-sm-2 form-field-block"><label class="form-label" data-pg-ui-t="국가">국가</label><select class="form-control form-control-sm" name="addrCountryCd" data-addr-country-select><option value="" data-pg-ui-t="선택">선택</option><option value="JP">JAPAN</option><option value="KR">KOREA</option><option value="TH">THAILAND</option><option value="OTHER" data-pg-ui-t="기타">기타</option></select></div>' +
              '<div class="col-sm-2 form-field-block addr-country-other-wrap d-none"><label class="form-label" data-pg-ui-t="국가">국가</label><select class="form-control form-control-sm" name="addrCountryCdOther">' + (window.PG_COUNTRY_OTHER_OPTIONS || '<option value="" data-pg-ui-t="선택">선택</option>') + '</select></div>' +
              '<div class="col-sm-2 form-field-block zip-wrap">' + pgUiFormLabelSpan(_addrKey(zipRaw), _addrStar(zipRaw)) + '<div class="form-input-with-btn" data-zip-search-wrap><input type="text" class="form-control form-control-sm" name="zipCode" data-pg-ui-placeholder="검색" placeholder="' + escUi(L('검색')) + '" data-zip-input><button type="button" class="btn btn-outline-secondary btn-sm" data-addr-zip-search data-pg-ui-t="검색">' + escUi(L('검색')) + '</button></div></div>' +
              '<div class="col-sm-2 form-field-block">' + pgUiFormLabelSpan(_addrKey(addrRaw), _addrStar(addrRaw)) + '<input type="text" class="form-control form-control-sm" name="addr" data-addr-input></div>' +
              '<div class="col-sm-2 form-field-block">' + pgUiFormLabelSpan(addrDetRaw, false) + '<input type="text" class="form-control form-control-sm" name="addrDetail"></div>' +
              (addrEtcL ? '<div class="col-sm-2 form-field-block">' + pgUiFormLabelSpan(String(addrEtcL), false) + '<input type="text" class="form-control form-control-sm" name="addrEtc" data-pg-ui-placeholder="기타 입력" placeholder="' + escUi(L('기타 입력')) + '"></div>' : '') +
              '</div>';
          } else if (first && first.type === 'countryBankRow') {
            var opt = first;
            function _bankKey(raw, def) {
              return String(raw || def || '').replace(/\*$/, '');
            }
            function _bankStar(raw) {
              return !!(raw && String(raw).indexOf('*') !== -1);
            }
            var bankRaw = opt.bankLabel || '계좌은행*';
            var acctRaw = opt.accountNoLabel || '계좌번호*';
            var holdRaw = opt.accountHolderLabel || '예금주*';
            var bankHq = opt.hideForHeadquarters ? ' comp-info-hide-if-hq' : '';
            html += '<div class="row country-bank-row' + bankHq + '" data-country-bank="true">' +
              '<div class="col-sm-2 form-field-block"><label class="form-label" data-pg-ui-t="국가">국가</label><select class="form-control form-control-sm" name="countryCd" data-country-select><option value="" data-pg-ui-t="선택">선택</option><option value="JP">JAPAN</option><option value="KR">KOREA</option><option value="TH">THAILAND</option><option value="OTHER" data-pg-ui-t="기타">기타</option></select></div>' +
              '<div class="col-sm-2 form-field-block country-other-wrap d-none"><label class="form-label" data-pg-ui-t="국가">국가</label><select class="form-control form-control-sm" name="countryCdOther">' + (window.PG_COUNTRY_OTHER_OPTIONS || '<option value="" data-pg-ui-t="선택">선택</option>') + '</select></div>' +
              '<div class="col-sm-2 form-field-block bank-select-wrap">' + pgUiFormLabelSpan(_bankKey(bankRaw), _bankStar(bankRaw)) + '<select class="form-control form-control-sm" name="bankCd" data-bank-select><option value="" data-pg-ui-t="국가 선택 후">국가 선택 후</option></select></div>' +
              '<div class="col-sm-2 form-field-block bank-text-wrap d-none">' + pgUiFormLabelSpan(_bankKey(bankRaw), _bankStar(bankRaw)) + '<input type="text" class="form-control form-control-sm" name="bankCdText" data-pg-ui-placeholder="은행명 직접입력" placeholder="' + escUi(L('은행명 직접입력')) + '"></div>' +
              '<div class="col-sm-2 form-field-block">' + pgUiFormLabelSpan(_bankKey(acctRaw), _bankStar(acctRaw)) + '<input type="text" class="form-control form-control-sm" name="' + (opt.accountNoName || 'accountNo') + '"></div>' +
              '<div class="col-sm-2 form-field-block">' + pgUiFormLabelSpan(_bankKey(holdRaw), _bankStar(holdRaw)) + '<input type="text" class="form-control form-control-sm" name="' + (opt.accountHolderName || 'accountHolder') + '"></div>' +
              (opt.extraFields ? opt.extraFields.map(function (ef) {
                var elab = String(ef.label || '');
                var elabHtml = elab ? ('<label class="form-label"><span data-pg-ui-t="' + escUi(elab) + '">' + escUi(L(elab)) + '</span></label>') : '<label class="form-label"></label>';
                var eph = ef.placeholder ? String(ef.placeholder) : '';
                var ephAttr = eph ? (' placeholder="' + escUi(L(eph)) + '" data-pg-ui-placeholder="' + escUi(eph) + '"') : '';
                return '<div class="col-sm-' + (ef.col || 2) + ' form-field-block">' + elabHtml + '<input type="text" class="form-control form-control-sm" name="' + (ef.name || '') + '"' + ephAttr + '></div>';
              }).join('') : '') +
              '</div>';
          } else {
            var rowClass = 'row';
            if (row && row[0] && row[0].type === 'notifyPairButton') {
              rowClass = 'row g-2 mb-2 align-items-start comp-notify-pair-url-row';
            }
            html += '<div class="' + rowClass + '">';
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
        var bl = String(b.label || '');
        html += '<button type="button" class="btn ' + (b.cls || 'btn-secondary') + ' btn-sm mr-1" id="' + (b.id || '') + '">' + (bl ? ('<span data-pg-ui-t="' + escUi(bl) + '">' + escUi(L(bl)) + '</span>') : '') + '</button>';
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
        html += '<div class="col-sm-' + col + ' mb-2"><label class="form-label">' + escUi(L(String(r.label || ''))) + req + '</label><input type="text" class="form-control form-control-sm" name="' + (r.name || '') + '" id="' + (r.name || '') + '"></div>';
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
      html += '<span class="summary-item mr-3" id="summary_' + s + '">' + escUi(L(String(s))) + ': ' + fmt + '</span>';
    });
    html += '</div></div>';
    return html;
  }

  function renderButtons(cfg) {
    var btns = cfg.buttons || [];
    var html = '<div class="row mb-2 screen-action-row"><div class="col-sm-12 screen-action-buttons">';
    btns.forEach(function (b) {
      var bl2 = String(b.label || '');
      var bidRb = b.id || '';
      var isListSearchRb = bidRb === 'searchBtn';
      html += '<button type="button" class="btn ' + (b.cls || 'btn-secondary') + ' btn-sm mr-1' + (isListSearchRb ? ' screen-search-btn' : '') + '" id="' + bidRb + '"' + (isListSearchRb ? ' data-pg-list-search-btn="1"' : '') + '>' + (bl2 ? ('<span data-pg-ui-t="' + escUi(bl2) + '">' + escUi(L(bl2)) + '</span>') : '') + '</button>';
    });
    html += '</div></div>';
    return html;
  }

  /**
   * 헬로: 목록 등 액션 바가 있을 때 삽입 — 클릭 시 안내(파스텔)·VIEW SETTING만 함께 숨김/표시(집계 바는 유지).
   * 본사설정(/hq/*) 화면에는 주입하지 않습니다.
   */
  function injectViewSettingHelloIntoButtons(btns, cfg, screenUrl) {
    if (!cfg || cfg.hideListGrid) return btns || [];
    var su = screenUrl ? String(screenUrl) : '';
    if (su.indexOf('/hq/') === 0 && su !== '/hq/pgApiMng') return btns || [];
    if (!btns || !btns.length) return btns;
    var out = btns.slice();
    var refreshIdx = -1;
    var excelIdx = -1;
    for (var i = 0; i < out.length; i++) {
      var b = out[i];
      if (b && b._viewSettingHello) continue;
      var id = String(b.id || '');
      var lab = String(b.label || '');
      if (refreshIdx < 0 && (id === 'payListRefreshBtn' || lab === '새로고침' || lab === L('새로고침'))) refreshIdx = i;
      if (excelIdx < 0 && (id === 'excelBtn' || id === 'excelDownBtn' || lab.indexOf('엑셀') !== -1 || lab.indexOf('Excel') !== -1)) excelIdx = i;
    }
    var hello = { id: 'viewSettingHelloBtn', label: L('헬로'), cls: 'btn-view-setting-hello', _viewSettingHello: true };
    if (refreshIdx >= 0 && excelIdx >= 0 && excelIdx > refreshIdx) {
      out.splice(excelIdx, 0, hello);
    } else if (excelIdx >= 0) {
      out.splice(excelIdx, 0, hello);
    } else if (refreshIdx >= 0) {
      out.splice(refreshIdx + 1, 0, hello);
    } else {
      out.splice(0, 0, hello);
    }
    return out;
  }

  /** 엑셀다운로드 옆 — [검색] 조건 전체 xlsx(하단 한 번에 보기·페이지 무관) */
  function injectExcelAllDownloadButton(btns, cfg, screenUrl) {
    if (!cfg || cfg.hideListGrid || !btns || !btns.length) return btns || [];
    if (cfg.isDailySummaryScreen || cfg.isOpsIntegratedReport || cfg.orgPagePermissionMatrix || cfg.hqOpsModeMng) return btns;
    var su = screenUrl ? String(screenUrl) : '';
    if (su === '/ops/taxReport') return btns;
    var hasExcel = false;
    for (var i = 0; i < btns.length; i++) {
      var id = String(btns[i].id || '');
      if (id === 'excelBtn' || id === 'excelDownBtn') { hasExcel = true; break; }
    }
    if (!hasExcel) return btns;
    var out = btns.slice();
    var insertIdx = -1;
    for (var j = 0; j < out.length; j++) {
      var id2 = String(out[j].id || '');
      if (id2 === 'excelBtn' || id2 === 'excelDownBtn') { insertIdx = j + 1; break; }
    }
    var allBtn = { id: 'excelAllDownBtn', label: '모두다운로드', cls: 'btn-outline-info' };
    if (insertIdx >= 0) out.splice(insertIdx, 0, allBtn);
    else out.push(allBtn);
    return out;
  }

  /** 총합(요약) 왼쪽 + 액션 버튼 오른쪽 한 줄 배치 (모든 목록 화면 공통) */
  function renderSummaryAndActions(cfg, tabId, screenUrl) {
    var items = cfg.summary || [];
    var btns = injectExcelAllDownloadButton(injectViewSettingHelloIntoButtons(cfg.buttons || [], cfg, screenUrl), cfg, screenUrl);
    var fmt = cfg.summaryFormat !== undefined ? cfg.summaryFormat : '0';
    var payAggInline = !!(cfg && cfg.payListFinancialInline);
    var payStatusInline = !!(cfg && cfg.payListStatusBar && !payAggInline);
    var summaryHtml = '';
    if (items.length > 0 || payAggInline || payStatusInline) {
      summaryHtml = '<div class="summary-total-bar' + ((payAggInline || payStatusInline) ? ' summary-total-bar--pay-list-aggregate' : '') + '">';
      items.forEach(function (s) {
        var sk = String(s);
        var countCls = sk === '건수' ? ' summary-count-item' : '';
        summaryHtml += '<span class="summary-total-item' + countCls + '" id="summary_' + sk + '" data-pg-summary-key="' + sk.replace(/"/g, '&quot;') + '">' + escUi(L(sk)) + ': ' + fmt + '</span>';
      });
      if (payAggInline) {
        var tAgg = tabId || '';
        if (items.length > 0) {
          summaryHtml += '<span class="pay-list-status-bar__pipe pay-list-aggregate-inline-sep" aria-hidden="true">ㅣ</span>';
        }
        summaryHtml += '<div class="pay-list-financial-summary pay-list-financial-summary--empty" id="payListFinancialSummary_' + tAgg + '" role="status" aria-live="polite"></div>';
      }
      if (payStatusInline) {
        var tSt = tabId || '';
        if (items.length > 0) {
          summaryHtml += '<span class="pay-list-status-bar__pipe pay-list-aggregate-inline-sep" aria-hidden="true">ㅣ</span>';
        }
        summaryHtml += '<div class="pay-list-status-bar pay-list-status-bar--empty" id="payListStatusBar_' + tSt + '" role="status" aria-live="polite"></div>';
      }
      summaryHtml += '</div>';
    }
    var buttonsHtml = '';
    var sortToolbarEnabled = cfg.listSortDirToolbar !== false;
    var anchorSort = cfg.listSortDirAnchor;
    var hasTbSearchSort = toolbarHasSearchBtnForSort(btns);
    var hasTbRefreshSort = toolbarHasRefreshBtnForSort(btns);
    var toolbarSortHtml = '';
    if (sortToolbarEnabled) {
      if (anchorSort === 'refresh' && hasTbRefreshSort) {
        toolbarSortHtml = buildListSortDirSelectHtml(tabId);
      } else if (anchorSort !== 'refresh' && hasTbSearchSort) {
        toolbarSortHtml = buildListSortDirSelectHtml(tabId);
      }
    }
    if (btns.length > 0) {
      buttonsHtml = '<div class="screen-action-buttons">';
      var tid = tabId || '';
      var sortPlaced = false;
      btns.forEach(function (b) {
        if (toolbarSortHtml && !sortPlaced) {
          var bid0 = String(b && b.id || '');
          if (anchorSort === 'refresh' && bid0 === 'payListRefreshBtn') {
            var bidRf = (b && b._viewSettingHello) ? ('viewSettingHelloBtn_' + tid) : (b.id || '');
            var labRf = String(b.label || '');
            buttonsHtml += '<button type="button" class="btn ' + (b.cls || 'btn-secondary') + ' btn-sm" id="' + bidRf + '">' + (labRf ? '<span data-pg-ui-t="' + escUi(labRf) + '">' + escUi(L(labRf)) + '</span>' : '') + '</button>';
            if (cfg.listToolbarBetweenRefreshAndSort && cfg.listToolbarBetweenRefreshAndSort.length) {
              cfg.listToolbarBetweenRefreshAndSort.forEach(function (bx) {
                var pfx = bx.idPrefix || bx.id || 'listToolbarMid';
                var midId = pfx + '_' + tid;
                var labMid = String(bx.label || '');
                buttonsHtml += '<button type="button" class="btn btn-sm ' + (bx.cls || 'btn-outline-secondary') + '" id="' + midId + '">' + (labMid ? '<span data-pg-ui-t="' + escUi(labMid) + '">' + escUi(L(labMid)) + '</span>' : '') + '</button>';
              });
            }
            buttonsHtml += toolbarSortHtml;
            sortPlaced = true;
            return;
          }
          if (anchorSort !== 'refresh' && bid0 === 'searchBtn') {
            buttonsHtml += toolbarSortHtml;
            sortPlaced = true;
          }
        }
        var bid = (b && b._viewSettingHello) ? ('viewSettingHelloBtn_' + tid) : (b.id || '');
        var labB = String(b.label || '');
        var isListSearchBtn = bid === 'searchBtn';
        buttonsHtml += '<button type="button" class="btn ' + (b.cls || 'btn-secondary') + ' btn-sm' + (isListSearchBtn ? ' screen-search-btn' : '') + '" id="' + bid + '"' + (isListSearchBtn ? ' data-pg-list-search-btn="1"' : '') + '>' + (labB ? '<span data-pg-ui-t="' + escUi(labB) + '">' + escUi(L(labB)) + '</span>' : '') + '</button>';
      });
      buttonsHtml += '</div>';
    }
    if (!summaryHtml && !buttonsHtml) return '';
    return '<div class="screen-summary-action-row">' + summaryHtml + buttonsHtml + '</div>';
  }

  /** 결제·통합내역: 상태별 집계(성공/실패 등) — 금액·건수는 summary-total-bar 한 줄에 병합 */
  function renderPayListStatusBarSlot(tabId) {
    var t = tabId || '';
    return '<div class="pay-list-summary-stack pay-list-aggregate-stack mb-2 small border rounded bg-light px-2 py-2">' +
      '<div class="pay-list-aggregate-section pay-list-aggregate-section--status">' +
      '<div class="pay-list-aggregate-row pay-list-aggregate-row--status pay-list-status-bar pay-list-status-bar--empty" id="payListStatusBar_' + t + '" role="status" aria-live="polite"></div>' +
      '</div>' +
      '</div>';
  }

  /** 유통망정산내역: VIEW SETTING으로 필터된 열 순서에 맞춘 승인/취소·수수료 중첩 헤더 */
  function buildDistributionListTheadHtmlFromCols(cols) {
    function esc(s) {
      return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
    }
    function spanUiT(ko) {
      var k = String(ko == null ? '' : ko);
      if (!k) return '';
      return '<span data-pg-ui-t="' + esc(k) + '">' + escUi(L(k)) + '</span>';
    }
    function thKeyLabel(key, rowspan, cls) {
      var col = (cols || []).filter(function (x) { return x && x.key === key; })[0];
      var lab = col ? (col.label || key) : key;
      var rs = rowspan > 1 ? ' rowspan="' + rowspan + '"' : '';
      var c = cls ? String(cls) : 'text-nowrap';
      return '<th data-key="' + esc(key) + '"' + rs + ' class="' + esc(c) + '">' + spanUiT(String(lab)) + '</th>';
    }
    var APRV_MAIN = ['aprvCnt', 'aprvAmt'];
    var APRV_FEE = ['aprvFeeCnt', 'aprvFeePct', 'aprvFeeSum', 'aprvFeeVat'];
    var CAN_MAIN = ['canCnt', 'canAmt'];
    var CAN_FEE = ['canFeeCnt', 'canFeePct', 'canFeeSum', 'canFeeVat'];
    var LEADING_META = {
      rowNo: 1, settleMonth: 1, orgDivNm: 1, hqNm: 1, regionalNm: 1, masterNm: 1, branchNm: 1, agencyNm: 1, salesOfficeNm: 1, compId: 1, curType: 1
    };
    if (!cols || !cols.length) {
      return '<tr><th class="text-nowrap">—</th></tr>';
    }
    var keys = cols.map(function (c) { return c.key; });
    function has(k) { return keys.indexOf(k) >= 0; }
    var leading = [];
    cols.forEach(function (c) {
      if (c && c.key && LEADING_META[c.key]) leading.push(c.key);
    });
    var aprvM = APRV_MAIN.filter(has);
    var aprvF = APRV_FEE.filter(has);
    var canM = CAN_MAIN.filter(has);
    var canF = CAN_FEE.filter(has);
    var hasSettle = has('settleAmt');
    var aprvBlock = aprvM.length + aprvF.length > 0;
    var canBlock = canM.length + canF.length > 0;
    var aprvDeep = aprvM.length > 0 && aprvF.length > 0;
    var canDeep = canM.length > 0 && canF.length > 0;
    var useSubRows = aprvDeep || canDeep;
    var refCl = MENU_SCREENS['/calc/calcList'];
    var canonicalKeys = (refCl && refCl.columns ? refCl.columns : []).map(function (x) { return x.key; }).join('\u0001');
    var currentKeys = cols.map(function (x) { return x.key; }).join('\u0001');
    /** 열 구성·순서가 기본과 다르면 1행 헤더(본문 열 순서와 정확히 일치) */
    var forceFlatHeader = canonicalKeys !== currentKeys;

    function appendAprv(r1, r2, r3) {
      if (!aprvBlock) return;
      if (aprvM.length > 0 && aprvF.length === 0) {
        aprvM.forEach(function (k) { r1.push(thKeyLabel(k, 3, 'text-nowrap')); });
        return;
      }
      if (aprvF.length > 0 && aprvM.length === 0) {
        aprvF.forEach(function (k) { r1.push(thKeyLabel(k, 3, 'text-nowrap dist-th-fee-sub')); });
        return;
      }
      if (aprvDeep) {
        r1.push('<th colspan="' + (aprvM.length + aprvF.length) + '" class="dist-th-group text-center" data-pg-ui-t="승인"></th>');
        aprvM.forEach(function (k) { r2.push(thKeyLabel(k, 2, 'text-nowrap')); });
        r2.push('<th colspan="' + aprvF.length + '" class="dist-th-fee text-center text-nowrap" data-pg-ui-t="수수료"></th>');
        aprvF.forEach(function (k) { r3.push(thKeyLabel(k, 1, 'text-nowrap dist-th-fee-sub')); });
      }
    }
    function appendCan(r1, r2, r3) {
      if (!canBlock) return;
      if (canM.length > 0 && canF.length === 0) {
        canM.forEach(function (k) { r1.push(thKeyLabel(k, 3, 'text-nowrap')); });
        return;
      }
      if (canF.length > 0 && canM.length === 0) {
        canF.forEach(function (k) { r1.push(thKeyLabel(k, 3, 'text-nowrap dist-th-fee-sub')); });
        return;
      }
      if (canDeep) {
        r1.push('<th colspan="' + (canM.length + canF.length) + '" class="dist-th-group text-center" data-pg-ui-t="취소"></th>');
        canM.forEach(function (k) { r2.push(thKeyLabel(k, 2, 'text-nowrap')); });
        r2.push('<th colspan="' + canF.length + '" class="dist-th-fee text-center text-nowrap" data-pg-ui-t="수수료"></th>');
        canF.forEach(function (k) { r3.push(thKeyLabel(k, 1, 'text-nowrap dist-th-fee-sub')); });
      }
    }

    if (!useSubRows || forceFlatHeader) {
      var flat = [];
      cols.forEach(function (c) {
        if (!c || !c.key) return;
        var k = c.key;
        var lcls = k === 'curType' ? 'text-nowrap text-center' : 'text-nowrap';
        if (APRV_FEE.indexOf(k) >= 0 || CAN_FEE.indexOf(k) >= 0) lcls += ' dist-th-fee-sub';
        flat.push(thKeyLabel(k, 1, lcls));
      });
      return '<tr>' + flat.join('') + '</tr>';
    }

    var row1 = [];
    var row2 = [];
    var row3 = [];
    leading.forEach(function (k) {
      var lcls2 = k === 'curType' ? 'text-nowrap text-center' : 'text-nowrap';
      row1.push(thKeyLabel(k, 3, lcls2));
    });
    appendAprv(row1, row2, row3);
    appendCan(row1, row2, row3);
    if (hasSettle) row1.push(thKeyLabel('settleAmt', 3, 'text-nowrap'));
    return '<tr>' + row1.join('') + '</tr><tr>' + row2.join('') + '</tr><tr>' + row3.join('') + '</tr>';
  }

  /** @deprecated 초기 빈 그리드용 — 전체 열 기준(실조회 시에는 FromCols 사용) */
  function buildDistributionListTheadHtml() {
    var cl = MENU_SCREENS['/calc/calcList'];
    return buildDistributionListTheadHtmlFromCols(cl && cl.columns ? cl.columns : []);
  }

  function renderTable(cfg, tabId) {
    var cols = cfg.columns || [];
    var respExtra = cfg.tableResponsiveExtraClass ? (' ' + String(cfg.tableResponsiveExtraClass).trim()) : '';
    if (cfg.distributionThreeRowHeader) {
      var emptyKey0 = String(cfg.emptyMessage != null && String(cfg.emptyMessage).trim() !== '' ? cfg.emptyMessage : '조회된 데이터가 없습니다.');
      var emptyRow = '<tr><td colspan="' + cols.length + '" class="empty-state-cell text-center text-muted py-4"><span data-pg-ui-t="' + escUi(emptyKey0) + '">' + escUi(L(emptyKey0)) + '</span></td></tr>';
      var respClass = 'table-responsive' + (cfg.tableScrollable ? ' table-scrollable' : '') + respExtra;
      var tblExtra = cfg.tableExtraClass ? (' ' + cfg.tableExtraClass) : '';
      return '<div class="' + respClass + '">' +
        '<table class="table table-bordered table-hover table-sm screen-distribution-grid' + tblExtra + '" id="grid_' + (tabId || '') + '">' +
        '<thead>' + buildDistributionListTheadHtmlFromCols(cols) + '</thead>' +
        '<tbody>' + emptyRow + '</tbody></table></div>';
    }
    var ths = cols.map(function (c) {
      if (c.type === 'checkbox') {
        return '<th style="width:40px" data-key="_chk"><input type="checkbox" class="grid-check-all" data-pg-ui-title="전체선택"></th>';
      }
      var titleKey = (c.title != null ? String(c.title) : '');
      var thT = titleKey ? (' data-pg-ui-title="' + escUi(titleKey) + '"') : '';
      var thClassParts = [];
      if (c.align === 'center') thClassParts.push('text-center');
      if (c.thClass) thClassParts.push(String(c.thClass));
      var thCls = thClassParts.length ? (' class="' + thClassParts.join(' ') + '"') : '';
      var thLab = c.label != null ? String(c.label) : String(c.key || '');
      var labelInner = thLab
        ? ('<span data-pg-ui-t="' + escUi(thLab) + '">' + escUi(L(thLab)) + '</span>')
        : '';
      return '<th data-key="' + escUi(c.key || '') + '"' + thCls + thT + '>' + labelInner + '</th>';
    }).join('');
    var emptyKey = String(cfg.emptyMessage != null && String(cfg.emptyMessage).trim() !== '' ? cfg.emptyMessage : '조회된 데이터가 없습니다.');
    var emptyRow = '<tr><td colspan="' + cols.length + '" class="empty-state-cell text-center text-muted py-4"><span data-pg-ui-t="' + escUi(emptyKey) + '">' + escUi(L(emptyKey)) + '</span></td></tr>';
    var respClass = 'table-responsive' + (cfg.tableScrollable ? ' table-scrollable' : '') + respExtra;
    var tblExtra = cfg.tableExtraClass ? (' ' + cfg.tableExtraClass) : '';
    var payMngGridCls = (cfg.payListStatusBar || cfg.payMngDenseGrid) ? ' pay-mng-data-grid' : '';
    var noColResizeCls = cfg.tableNoColResize ? ' table-no-col-resize' : '';
    var compMngTreeCls = cfg.compMngTreeGrid ? ' comp-mng-tree-grid' : '';
    var html = '<div class="' + respClass + '"><table class="table table-bordered table-hover table-sm' + tblExtra + payMngGridCls + noColResizeCls + compMngTreeCls + '" id="grid_' + (tabId || '') + '"><thead><tr>' + ths + '</tr></thead><tbody>' + emptyRow + '</tbody></table></div>';
    return html;
  }

  /** 본사설정 > 도메인구성설정: 전사 URL + 본사·총판별 도메인 (개별 조직 권한 블록과 유사 레이아웃) */
  function renderDomainConfigShell(tabId) {
    var sid = tabId || 'hq_domainConfig';
    var kSslIntro = '이 서버의 <code>fullchain.pem</code> 에서 읽은 <strong>SAN(호스트명)</strong>과, 전사 URL·본사·총판에 저장된 URL의 호스트를 비교합니다. 표시·저장 시 주소에 <code>http://</code> 또는 <code>https://</code> 가 없으면 <strong>https://</strong> 를 붙입니다. 불일치 시 브라우저 인증서 경고가 날 수 있습니다. 서브도메인 추가 시 DNS A 레코드·Nginx <code>server_name</code>·<code>certbot --nginx -d …</code> 를 함께 적용하세요. 상세 SSL 경로·Certbot 타이머는 <strong>본사설정 → 서버운영관리</strong>를 참고하세요.';
    var kOrgIntro = '업체명에서 <strong>본사</strong> 또는 <strong>총판</strong>만 선택할 수 있습니다. 선택 후 설정 이름·URL을 입력하고 [설정저장]하면 하단 목록에 반영됩니다. URL에 스킴이 없으면 <strong>https://</strong> 가 자동으로 붙습니다. <strong>본사</strong> 관리자 URL: <strong>총본사·해당 본사</strong> 소속 계정만 로그인됩니다(하위 총판·가맹 등은 불가). <strong>총판</strong> URL: <strong>총본사·이 총판을 소속 트리에 두는 본사·해당 총판 및 그 하위</strong>만 로그인됩니다(다른 총판·다른 본사 트리는 불가). 브랜딩(로그인 화면 등)은 접속한 URL에 매칭된 본사·총판 조직의 설정을 따릅니다.';
    return (
      '<div class="hq-domain-config-wrap">' +
      '<div class="card mb-3">' +
      '<div class="card-header py-2 fw-semibold" data-pg-ui-t="전사 기본 URL">전사 기본 URL</div>' +
      '<div class="card-body">' +
      pgUiParagraph('노티·문서·가맹점 안내에 쓰는 기본 공개 URL입니다. 저장은 시스템 관리자(ADMIN)만 가능합니다.', 'text-muted small mb-2') +
      '<div class="row g-2 align-items-end">' +
      '<div class="col-lg-5 col-md-12"><label class="form-label small mb-1" data-pg-ui-t="관리자(웹) 공개 URL">관리자(웹) 공개 URL</label>' +
      '<input type="text" class="form-control form-control-sm" name="publicAdminSiteUrl" data-pg-ui-placeholder="https://icopay.co.kr" placeholder="https://icopay.co.kr"></div>' +
      '<div class="col-lg-5 col-md-12"><label class="form-label small mb-1" data-pg-ui-t="API 공개 베이스 URL">API 공개 베이스 URL</label>' +
      '<input type="text" class="form-control form-control-sm" name="publicApiBaseUrl" data-pg-ui-placeholder="https://api.icopay.co.kr" placeholder="https://api.icopay.co.kr"></div>' +
      '<div class="col-lg-2 col-md-12">' +
      '<button type="button" class="btn btn-sm btn-outline-primary w-100" id="hqDomainGlobalSaveBtn_' + sid + '" data-pg-ui-t="전사 URL 저장">전사 URL 저장</button></div>' +
      '</div>' +
      '<div class="small mt-2" id="hqDomainGlobalMsg_' + sid + '" role="status"></div>' +
      '</div></div>' +
      '<div class="card mb-3 border-secondary">' +
      '<div class="card-header py-2 fw-semibold" data-pg-ui-t="Let\u2019s Encrypt · 도메인구성설정 연동">Let\u2019s Encrypt · 도메인구성설정 연동</div>' +
      '<div class="card-body">' +
      pgUiParagraphHtml(kSslIntro) +
      '<div id="hqDomainSslLinkage_' + sid + '" class="small"><span data-pg-ui-t="불러오는 중…">' + escUi(L('불러오는 중…')) + '</span></div>' +
      '</div></div>' +
      '<div class="card border-0 shadow-sm mb-3 org-perm-unit-section">' +
      '<div class="card-header fw-semibold" data-pg-ui-t="본사·총판 도메인 설정">본사·총판 도메인 설정</div>' +
      '<div class="card-body">' +
      pgUiParagraphHtml(kOrgIntro, 'text-muted small mb-3') +
      '<div class="row g-2 align-items-end mb-2 org-perm-unit-control-row">' +
      '<div class="col-lg-3 col-md-6">' +
      '<label class="form-label small mb-1" data-pg-ui-t="업체명">업체명</label>' +
      '<select class="form-select form-select-sm" id="hqDomainOrgSelect_' + sid + '">' +
      '<option value="" data-pg-ui-t="— 업체를 선택하세요 —">' + escUi(L('— 업체를 선택하세요 —')) + '</option></select></div>' +
      '<div class="col-lg-2 col-md-6">' +
      '<label class="form-label small mb-1" data-pg-ui-t="업체코드">업체코드</label>' +
      '<input type="text" class="form-control form-control-sm" id="hqDomainOrgCode_' + sid + '" readonly></div>' +
      '<div class="col-lg-2 col-md-6">' +
      '<label class="form-label small mb-1" data-pg-ui-t="조직구분">조직구분</label>' +
      '<input type="text" class="form-control form-control-sm" id="hqDomainOrgLevel_' + sid + '" readonly></div>' +
      '<div class="col-lg-2 col-md-6">' +
      '<label class="form-label small mb-1" data-pg-ui-t="설정 이름">설정 이름</label>' +
      '<input type="text" class="form-control form-control-sm" id="hqDomainSettingName_' + sid + '" data-pg-ui-placeholder="표시용 이름" placeholder="표시용 이름" disabled></div>' +
      '</div>' +
      '<div class="row g-2 align-items-end mb-2">' +
      '<div class="col-lg-4 col-md-6">' +
      '<label class="form-label small mb-1" data-pg-ui-t="관리자(웹) URL">관리자(웹) URL</label>' +
      '<input type="text" class="form-control form-control-sm" id="hqDomainOrgAdminUrl_' + sid + '" data-pg-ui-placeholder="https://icopay.co.kr" placeholder="https://icopay.co.kr" disabled></div>' +
      '<div class="col-lg-4 col-md-6">' +
      '<label class="form-label small mb-1" data-pg-ui-t="API URL">API URL</label>' +
      '<input type="text" class="form-control form-control-sm" id="hqDomainOrgApiUrl_' + sid + '" data-pg-ui-placeholder="https://api.icopay.co.kr" placeholder="https://api.icopay.co.kr" disabled></div>' +
      '<div class="col-lg-2 col-md-6">' +
      '<button type="button" class="btn btn-sm btn-primary w-100" id="hqDomainOrgSaveBtn_' + sid + '" disabled data-pg-ui-t="설정저장">설정저장</button></div>' +
      '</div>' +
      '<p class="small mb-2 text-muted" id="hqDomainOrgHint_' + sid + '"><span data-pg-ui-t="업체를 선택하면 입력란이 활성화됩니다.">' + escUi(L('업체를 선택하면 입력란이 활성화됩니다.')) + '</span></p>' +
      '<div class="small mb-2" id="hqDomainOrgMsg_' + sid + '" role="status"></div>' +
      '<div class="table-responsive">' +
      '<table class="table table-sm table-bordered align-middle mb-0" id="hqDomainOrgTable_' + sid + '">' +
      '<thead><tr>' +
      '<th class="text-center text-nowrap" style="width:3rem" data-pg-ui-t="No.">No.</th>' +
      '<th data-pg-ui-t="업체명">업체명</th>' +
      '<th class="text-nowrap" style="width:9rem" data-pg-ui-t="업체코드">업체코드</th>' +
      '<th class="text-nowrap" style="width:5rem" data-pg-ui-t="조직구분">조직구분</th>' +
      '<th data-pg-ui-t="설정 이름">설정 이름</th>' +
      '<th data-pg-ui-t="관리자(웹) URL">관리자(웹) URL</th>' +
      '<th data-pg-ui-t="API URL">API URL</th>' +
      '<th class="text-center text-nowrap" style="width:5rem" data-pg-ui-t="삭제">삭제</th>' +
      '<th class="text-nowrap" style="width:10rem" data-pg-ui-t="수정일시">수정일시</th>' +
      '</tr></thead>' +
      '<tbody id="hqDomainOrgTableTbody_' + sid + '">' +
      '<tr><td colspan="9" class="text-center text-muted py-3"><span data-pg-ui-t="불러오는 중…">' + escUi(L('불러오는 중…')) + '</span></td></tr>' +
      '</tbody></table></div>' +
      '</div></div></div>'
    );
  }

  /** 본사설정 — 태블릿설정: 조직 단계(열) × 메뉴(행) 체크 매트릭스 */
  function renderHqOpsModeTabletShell(tabId) {
    var introKo = '태블릿설정 안내';
    return (
      '<div class="hq-ops-mode-tablet card border-0 shadow-sm mb-3">' +
      '<div class="card-body">' +
      '<p class="text-muted small mb-3" data-pg-ui-t="' + escUi(introKo) + '">' + escUi(L(introKo)) + '</p>' +
      '<div class="table-responsive hq-ops-mode-matrix-wrap table-no-col-resize-wrap">' +
      '<table class="table table-sm table-bordered align-middle mb-0 hq-ops-mode-matrix-grid table-no-col-resize" id="hqOpsModeMatrixTable_' + tabId + '">' +
      '<thead id="hqOpsModeMatrixThead_' + tabId + '"><tr><th class="hq-ops-mode-th-menu" data-pg-ui-t="메뉴">' + escUi(L('메뉴')) + '</th>' +
      '<th class="text-center text-muted py-4" colspan="7" data-pg-ui-t="불러오는 중…">' + escUi(L('불러오는 중…')) + '</th></tr></thead>' +
      '<tbody id="hqOpsModeMatrixTbody_' + tabId + '"><tr><td colspan="8" class="text-center text-muted py-4" data-pg-ui-t="불러오는 중…">' + escUi(L('불러오는 중…')) + '</td></tr></tbody>' +
      '</table></div>' +
      '</div></div>' +
      '<div class="d-flex justify-content-end align-items-center flex-wrap gap-2 mb-2">' +
      '<button type="button" class="btn btn-outline-secondary btn-sm" id="hqOpsModeReloadBtn_' + tabId + '" data-pg-ui-t="다시 불러오기">' + escUi(L('다시 불러오기')) + '</button>' +
      '<button type="button" class="btn btn-primary btn-sm" id="hqOpsModeSaveBtn_' + tabId + '" data-pg-ui-t="저장">' + escUi(L('저장')) + '</button>' +
      '</div>'
    );
  }

  /** 조직별 권한 세팅 — 조직 탭 + 페이지별 권한 셀렉트 (내용은 API 로드 후 채움) */
  function renderOrgPagePermissionShell(tabId) {
    var introKey = '조직 구분(총본사~가맹점)별로 메뉴(URL) 접근 권한을 설정합니다. <strong>총본사</strong>는 DB에 별도 저장이 없을 때 기본으로 <strong>모든 메뉴 전체 권한(삭제·전체)</strong>입니다. 각 대메뉴(본사설정·업체관리·배포설정 등) 구역 제목 오른쪽 <strong>간편</strong>에서 권한을 고르면 그 구역의 하위 메뉴가 한 번에 동일하게 맞춰집니다. <strong>옵저버</strong>는 조회만, <strong>수정</strong>은 쓰기·수정(삭제·일괄삭제 등 제한), <strong>삭제</strong>는 해당 화면의 삭제·수정·저장 등 모든 작업을 허용합니다. <strong>접근불가</strong>는 메뉴에서 숨깁니다. <strong>업체접근설정</strong>에 등록된 업체와 교집합으로 사용자관리 목록이 제한됩니다. 아래 <strong>담당자 권한그룹별 메뉴</strong>는 조직 최종 권한(상단 개별 조직 권한) 이내에서 관리/운영/정산/기술 담당 계정(ASSISTANT)의 메뉴를 한 단계 더 조입니다.';
    var unitIntroKey = '총본사~가맹점 <strong>각 조직</strong>을 선택해, 단계별 기본과 다른 권한을 둘 수 있습니다. <strong>단계 기본 따름</strong>이면 위 탭의 조직 구분 기준만 적용되고, <strong>개별 설정</strong>이면 아래 표에서만 덮어씁니다. 조직을 고르면 <strong>현재 적용되는 권한(최종)</strong>이 표시됩니다.';
    var assistIntroKey = '위에서 조직을 선택하면, 해당 조직에 <strong>접근 가능한 메뉴</strong>만 표시됩니다. 값을 <strong>조직 기본(상한)</strong>으로 두면 담당자에게도 조직과 동일한 권한이 적용됩니다. 본사·총판·총본사는 자기 조직만 저장할 수 있습니다.';
    return (
      '<div class="org-perm-matrix card border-0 shadow-sm mb-3">' +
      '<div class="card-body">' +
      pgUiParagraphHtml(introKey, 'text-muted small mb-3') +
      '<div class="d-flex flex-wrap align-items-center mb-2 org-perm-legend text-muted">' +
      '<span class="me-2 fw-semibold text-secondary" data-pg-ui-t="행 색:">' + escUi(L('행 색:')) + '</span>' +
      '<span><i class="org-perm-legend-none" aria-hidden="true"></i><span data-pg-ui-t="접근불가">' + escUi(L('접근불가')) + '</span></span>' +
      '<span><i class="org-perm-legend-observer" aria-hidden="true"></i><span data-pg-ui-t="옵저버">' + escUi(L('옵저버')) + '</span></span>' +
      '<span><i class="org-perm-legend-modify" aria-hidden="true"></i><span data-pg-ui-t="수정">' + escUi(L('수정')) + '</span></span>' +
      '<span><i class="org-perm-legend-delete" aria-hidden="true"></i><span data-pg-ui-t="삭제(전체)">' + escUi(L('삭제(전체)')) + '</span></span>' +
      '</div>' +
      '<ul class="nav nav-pills flex-wrap gap-1 mb-3 org-perm-level-tabs" id="orgPermTabs_' + tabId + '" role="tablist"></ul>' +
      '<div class="table-responsive org-perm-table-wrap table-no-col-resize-wrap">' +
      '<table class="table table-sm table-bordered align-middle mb-0 org-perm-table table-no-col-resize" id="orgPermTable_' + tabId + '">' +
      '<thead><tr><th class="text-center text-nowrap org-perm-th-no" style="width:3.25rem">No.</th><th style="width:13%" data-pg-ui-t="메뉴ID">' + escUi(L('메뉴ID')) + '</th><th data-pg-ui-t="화면">' + escUi(L('화면')) + '</th><th style="width:24%" data-pg-ui-t="권한">' + escUi(L('권한')) + '</th></tr></thead>' +
      '<tbody id="orgPermTbody_' + tabId + '"><tr><td colspan="4" class="text-center text-muted py-4" data-pg-ui-t="불러오는 중…">' + escUi(L('불러오는 중…')) + '</td></tr></tbody>' +
      '</table></div>' +
      '</div></div>' +
      '<div class="d-flex justify-content-end align-items-center flex-wrap gap-2 mb-2 org-perm-default-actions">' +
      '<button type="button" class="btn btn-outline-secondary btn-sm" id="hqPermissionReloadBtn" data-pg-ui-t="다시 불러오기" data-pg-ui-title="서버에 저장된 단계별 기본 권한을 다시 불러옵니다(저장하지 않은 편집은 사라질 수 있습니다)" title="' + escUi(L('서버에 저장된 단계별 기본 권한을 다시 불러옵니다(저장하지 않은 편집은 사라질 수 있습니다)')) + '">' + escUi(L('다시 불러오기')) + '</button>' +
      '<button type="button" class="btn btn-primary btn-sm" id="hqPermissionSaveBtn" data-pg-ui-t="권한 저장">' + escUi(L('권한 저장')) + '</button></div>' +
      '<div class="card border-0 shadow-sm mb-3 org-perm-unit-section">' +
      '<div class="card-header fw-semibold" data-pg-ui-t="개별 조직 권한">' + escUi(L('개별 조직 권한')) + '</div>' +
      '<div class="card-body">' +
      pgUiParagraphHtml(unitIntroKey, 'text-muted small mb-3') +
      '<div class="row g-2 align-items-end mb-2 org-perm-unit-control-row">' +
      '<div class="col-lg-3 col-md-6">' +
      '<label class="form-label small mb-1" data-pg-ui-t="업체명">' + escUi(L('업체명')) + '</label>' +
      '<select class="form-select form-select-sm" id="orgPermUnitSelect_' + tabId + '">' +
      '<option value="" data-pg-ui-t="— 업체를 선택하세요 —">' + escUi(L('— 업체를 선택하세요 —')) + '</option>' +
      '</select></div>' +
      '<div class="col-lg-2 col-md-6">' +
      '<label class="form-label small mb-1" data-pg-ui-t="업체코드">' + escUi(L('업체코드')) + '</label>' +
      '<input type="text" class="form-control form-control-sm" id="orgPermUnitCode_' + tabId + '" readonly>' +
      '</div>' +
      '<div class="col-lg-2 col-md-6">' +
      '<label class="form-label small mb-1" data-pg-ui-t="조직구분">' + escUi(L('조직구분')) + '</label>' +
      '<input type="text" class="form-control form-control-sm" id="orgPermUnitLevel_' + tabId + '" readonly>' +
      '</div>' +
      '<div class="col-lg-2 col-md-6">' +
      '<label class="form-label small mb-1" data-pg-ui-t="현재방식">' + escUi(L('현재방식')) + '</label>' +
      '<input type="text" class="form-control form-control-sm" id="orgPermUnitCurrentMode_' + tabId + '" readonly>' +
      '</div>' +
      '<div class="col-lg-2 col-md-6">' +
      '<label class="form-label small mb-1" data-pg-ui-t="적용방식">' + escUi(L('적용방식')) + '</label>' +
      '<select class="form-select form-select-sm" id="orgPermUnitMode_' + tabId + '" disabled>' +
      '<option value="LEVEL_DEFAULT" data-pg-ui-t="단계 기본 따름">' + escUi(L('단계 기본 따름')) + '</option>' +
      '<option value="CUSTOM" data-pg-ui-t="개별 설정">' + escUi(L('개별 설정')) + '</option>' +
      '</select></div>' +
      '<div class="col-lg-1 col-md-6">' +
      '<button type="button" class="btn btn-sm btn-primary w-100" id="hqOrgUnitPermissionSaveBtn_' + tabId + '" disabled data-pg-ui-t="설정저장">' + escUi(L('설정저장')) + '</button>' +
      '</div></div>' +
      '<p class="small mb-2" id="orgPermUnitHint_' + tabId + '" data-pg-ui-t="조직을 선택하면 적용 방식과 권한 표가 채워집니다.">' + escUi(L('조직을 선택하면 적용 방식과 권한 표가 채워집니다.')) + '</p>' +
      '<div class="table-responsive org-perm-table-wrap table-no-col-resize-wrap">' +
      '<table class="table table-sm table-bordered align-middle mb-0 org-perm-table table-no-col-resize" id="orgPermUnitTable_' + tabId + '">' +
      '<thead><tr><th class="text-center text-nowrap org-perm-th-no" style="width:3.25rem">No.</th><th style="width:13%" data-pg-ui-t="메뉴ID">' + escUi(L('메뉴ID')) + '</th><th data-pg-ui-t="화면">' + escUi(L('화면')) + '</th><th style="width:24%" data-pg-ui-t="권한">' + escUi(L('권한')) + '</th></tr></thead>' +
      '<tbody id="orgPermUnitTbody_' + tabId + '"><tr><td colspan="4" class="text-center text-muted py-4" data-pg-ui-t="조직을 선택하세요.">' + escUi(L('조직을 선택하세요.')) + '</td></tr></tbody>' +
      '</table></div></div></div>' +
      '<div class="card border-0 shadow-sm mb-3 org-perm-assist-section">' +
      '<div class="card-header fw-semibold" data-pg-ui-t="담당자 권한그룹별 메뉴 (조직 상한 내)">' + escUi(L('담당자 권한그룹별 메뉴 (조직 상한 내)')) + '</div>' +
      '<div class="card-body">' +
      pgUiParagraphHtml(assistIntroKey, 'text-muted small mb-2') +
      '<ul class="nav nav-pills flex-wrap gap-1 mb-2 org-perm-assist-role-tabs" id="orgPermAssistRoleTabs_' + tabId + '" role="tablist"></ul>' +
      '<div class="table-responsive org-perm-table-wrap table-no-col-resize-wrap">' +
      '<table class="table table-sm table-bordered align-middle mb-0 org-perm-table table-no-col-resize" id="orgPermAssistTable_' + tabId + '">' +
      '<thead><tr><th class="text-center text-nowrap org-perm-th-no" style="width:3.25rem">No.</th><th style="width:13%" data-pg-ui-t="메뉴ID">' + escUi(L('메뉴ID')) + '</th><th data-pg-ui-t="화면">' + escUi(L('화면')) + '</th><th style="width:28%" data-pg-ui-t="담당자 권한">' + escUi(L('담당자 권한')) + '</th></tr></thead>' +
      '<tbody id="orgPermAssistTbody_' + tabId + '"><tr><td colspan="4" class="text-center text-muted py-3" data-pg-ui-t="조직을 선택하세요.">' + escUi(L('조직을 선택하세요.')) + '</td></tr></tbody>' +
      '</table></div>' +
      '<div class="d-flex justify-content-end mt-2">' +
      '<button type="button" class="btn btn-sm btn-primary" id="hqOrgAssistSaveBtn_' + tabId + '" disabled data-pg-ui-t="권한그룹 저장">' + escUi(L('권한그룹 저장')) + '</button></div>' +
      '</div></div>'
    );
  }

  /** 한 번에 보기 — 1000건·모두(전체) 고정 버튼 HTML */
  function buildPaginationExtraSizeButtonsHtml() {
    return '<button type="button" class="pagination-size-opt pagination-size-opt--1000" data-size="1000">' +
      '<span data-pg-ui-t="1000건">' + escUi(L('1000건')) + '</span></button>' +
      '<button type="button" class="pagination-size-opt pagination-size-opt--all" data-size="all">' +
      '<span data-pg-ui-t="모두">' + escUi(L('모두')) + '</span></button>';
  }

  function renderPagination(tabId, listCfg) {
    var trailingSave = '';
    if (listCfg && listCfg.paginationTrailingSaveButton) {
      trailingSave = '<div class="pagination-row-save">' +
        '<button type="button" class="btn btn-sm btn-primary" id="commissionPaginationSaveBtn" data-pg-ui-t="저장">' + escUi(L('저장')) + '</button></div>';
    }
    var sizeOpts = (listCfg && Array.isArray(listCfg.paginationSizeOptions) && listCfg.paginationSizeOptions.length)
      ? listCfg.paginationSizeOptions.slice()
      : [50, 100, 200, 500, 1000];
    sizeOpts = sizeOpts.filter(function (n) { return n !== 1000; });
    var defSize = listCfg && listCfg.paginationDefaultSize != null ? parseInt(listCfg.paginationDefaultSize, 10) : 500;
    if (isNaN(defSize) || defSize < 1) defSize = 500;
    if (sizeOpts.indexOf(defSize) === -1) {
      defSize = sizeOpts[sizeOpts.length - 1] || 500;
    }
    var sizeBtns = sizeOpts.map(function (n) {
      var active = n === defSize ? ' pagination-size-opt--active' : '';
      return '<button type="button" class="pagination-size-opt' + active + '" data-size="' + n + '">' + n + '</button>';
    }).join('');
    sizeBtns += buildPaginationExtraSizeButtonsHtml();
    return '<div class="pagination-row">' +
      '<div class="pagination-view-at-once">' +
      '<span class="pagination-label" data-pg-ui-t="한 번에 보기:">' + escUi(L('한 번에 보기:')) + '</span>' +
      '<div class="pagination-size-options">' +
      sizeBtns +
      '</div>' +
      '<span class="pagination-total"><span data-pg-ui-t="건 (총">' + escUi(L('건 (총')) + '</span> <span id="totalElementsCount">0</span><span data-pg-ui-t="건)">' + escUi(L('건)')) + '</span></span>' +
      '</div>' +
      '<input type="hidden" id="recordsPerPage" value="' + defSize + '">' +
      '<input type="hidden" id="pageCnt" value="1">' +
      '<span id="totalPageCount" style="display:none">1</span>' +
      '<div class="pagination-center"><div class="pagination-pages" id="paging_' + (tabId || '') + '"></div></div>' +
      trailingSave +
      '</div>';
  }

  var PAGE_FOOTER_HTML = '<div class="page-footer">Copyright © 2023 ICOPAY Service by Ontheline Co., Ltd.</div>';

  function getScreenHtml(url, tabId) {
    var cfg = MENU_SCREENS[url];
    tabId = tabId || (url.replace(/^\//, '').replace(/\//g, '_'));
    if (!cfg) {
      return '<div class="card"><div class="card-body"><p class="text-muted mb-0">' + escUi(L('화면 정보가 없습니다.')) + '</p>' + PAGE_FOOTER_HTML + '</div></div>';
    }
    var html = '<div class="content" id="screenContent_' + tabId + '">';
    html += '<div class="card"><div class="card-body">';
    if (cfg.isForm && cfg.formSections && cfg.formSections.length > 0) {
      html += renderFormSections(cfg);
      html += renderSummaryAndActions(cfg, tabId, url);
    } else if (cfg.isForm && cfg.formRows && cfg.formRows.length > 0) {
      html += renderFormRows(cfg);
      html += renderSummaryAndActions(cfg, tabId, url);
    } else if (cfg.domainConfigScreen) {
      html += renderDomainConfigShell(tabId);
      html += renderSummaryAndActions(cfg, tabId, url);
    } else if (cfg.orgPagePermissionMatrix) {
      html += renderOrgPagePermissionShell(tabId);
      html += renderSummaryAndActions(cfg, tabId, url);
    } else if (cfg.hqOpsModeMng) {
      html += renderHqOpsModeTabletShell(tabId);
      html += renderSummaryAndActions(cfg, tabId, url);
    } else if (cfg.staticHtml) {
      html += typeof cfg.staticHtml === 'function' ? cfg.staticHtml() : cfg.staticHtml;
      html += renderSummaryAndActions(cfg, tabId, url);
    } else if (cfg.opsIntegratedReportScreen) {
      html += renderSearchForm(cfg, tabId);
      if (cfg.noticeList && cfg.noticeList.length > 0) html += renderNotice(cfg);
      html += renderSummaryAndActions(cfg, tabId, url);
      html += '<div class="table-responsive table-scrollable integrated-report-wrap" id="integratedReportWrap_' + tabId + '">' +
        '<table class="table table-sm table-bordered align-middle mb-0 table-no-col-resize integrated-report-grid" id="grid_' + tabId + '">' +
        '<thead><tr><th class="text-center text-muted py-2">…</th></tr></thead>' +
        '<tbody><tr><td class="text-center text-muted py-4">' + escUi(L('검색을 실행하세요.')) + '</td></tr></tbody></table></div>' +
        '<div class="mt-3" id="integratedReportDetail_' + tabId + '"></div>';
    } else {
      if (!cfg.hideListGrid) {
        if (cfg.listTopHtml) {
          html += typeof cfg.listTopHtml === 'function' ? cfg.listTopHtml() : cfg.listTopHtml;
        }
        html += renderSearchForm(cfg, tabId);
        if (cfg.noticeList && cfg.noticeList.length > 0) html += renderNotice(cfg);
        html += renderSummaryAndActions(cfg, tabId, url);
        if (cfg.payListStatusBar && cfg.payListFinancialInline) html += renderPayListStatusBarSlot(tabId);
        if (cfg.columns && cfg.columns.length > 0) html += renderTableColumnGuide(cfg);
        html += renderTable(cfg, tabId);
        if (!cfg.isDailySummaryScreen) {
        html += renderPagination(tabId, cfg);
        }
        if (cfg.hasCommissionHistoryTable) {
          html += '<div class="card mt-4 commission-history-card"><div class="card-header py-2 fw-semibold">' + escUi(L('수수료 변경 히스토리')) + '</div><div class="card-body pt-2">' +
            '<p class="text-muted small mb-2" id="commissionHistSubtitle_' + tabId + '">' + escUi(L('목록에서 가맹점 행을 클릭하면 해당 업체의 변경 이력이 표시됩니다.')) + '</p>' +
            '<div class="table-responsive table-scrollable commission-list-table-wrap"><table class="table table-bordered table-sm table-hover mb-0 commission-split-grid table-no-col-resize" id="grid_commissionHist_' + tabId + '">' +
            '<thead><tr><th class="text-muted">…</th></tr></thead><tbody><tr><td class="text-center text-muted py-3">' + escUi(L('조회 전')) + '</td></tr></tbody></table></div></div></div>';
        }
        if (cfg.hasSettlementExecuteDetailTable) {
          var pubDayDetail = cfg.settlementExecuteDetailUiVariant === 'publishDay';
          var reportDetail = cfg.settlementExecuteDetailUiVariant === 'report';
          var settleDetailTitleKo = pubDayDetail ? '정산배포 · 당일 거래 내역' : (reportDetail ? '정산리포트 · 정산 대상 거래' : '정산실행상세 · 정산 대상 거래');
          var settleDetailMetaHintKo = pubDayDetail
            ? '목록 행을 클릭하면 정산일 기준 당일 00:00~24:00 가맹 전체 거래를 표시합니다.'
            : (reportDetail ? '정산집계·정산실시·확정정산에서 실행 ID가 있는 행을 클릭하면 해당 실행에 집계된 거래가 표시됩니다.' : '상단 목록 행을 더블클릭하면 표시됩니다.');
          var settleDetailHintPKo = pubDayDetail
            ? '정산배포 목록에서 한 행을 클릭하면, 해당 실행의 정산일(calc_dt) 달력 하루 동안 해당 가맹의 전체 결제 거래를 승인일시 오름차순으로 불러옵니다(최대 2,500건·초과 시 상한 안내). 격자 정산의 집계 구간(H1 등)과 범위가 다를 수 있습니다.'
            : (reportDetail
              ? '정산집계·정산실시·확정정산에서 실행 ID가 있는 행을 클릭하면 해당 정산 실행에 포함된 거래를 정산실행 화면과 동일한 형식으로 불러옵니다. 정산집계표(SUM)는 요약 1행만 제공되고, 본사 지급 리포트의 정산실시(EXE)는 본사 합산 행이라 실행 ID가 없을 수 있습니다 — 이 경우 리포트 형식을 가맹점 정산 리포트로 바꾼 뒤 가맹 단위 행을 클릭하세요.'
              : '정산실행 목록에서 한 행을 더블클릭하면, 해당 실행에 저장된 집계 건수(included_txn_cnt)가 있으면 그 건수만큼만, 같은 기간·정렬(승인일시 오름차순)으로 표시합니다. 상단 메타의 대상 매출액은 이 실행 집계 구간(예: H1 한 시간)에 대한 승인 매출 합(정산 실행 저장값)이며, 아래 표시 행의 단순 합이 아닙니다.');
          var settleDetailEmptyKo = pubDayDetail ? '목록에서 행을 클릭하세요.' : (reportDetail ? '실행 ID가 있는 행을 클릭하세요.' : '정산실행 행을 더블클릭하세요.');
          html += '<div class="card mt-4 screen-pay-list" id="settlementExecuteDetailCard_' + tabId + '"><div class="card-header py-2 fw-semibold d-flex flex-wrap justify-content-between align-items-center gap-2">' +
            '<span data-pg-ui-t="' + escUi(settleDetailTitleKo) + '">' + escUi(L(settleDetailTitleKo)) + '</span>' +
            '<span class="small text-muted fw-normal" id="settlementExecuteDetailMeta_' + tabId + '" data-pg-ui-t="' + escUi(settleDetailMetaHintKo) + '">' + escUi(L(settleDetailMetaHintKo)) + '</span></div><div class="card-body pt-2">' +
            '<p class="text-muted small mb-2" id="settlementExecuteDetailHint_' + tabId + '"><span data-pg-ui-t="' + escUi(settleDetailHintPKo) + '">' + escUi(L(settleDetailHintPKo)) + '</span></p>' +
            '<div class="table-responsive table-scrollable"><table class="table table-sm table-bordered table-hover align-middle mb-0 pay-mng-data-grid" id="grid_settlementExecuteDetail_' + tabId + '">' +
            '<thead>' +
            '<tr>' +
            '<th rowspan="2" class="text-nowrap text-end" style="width:3rem">No</th>' +
            '<th rowspan="2" class="text-nowrap" data-pg-ui-t="거래일">거래일</th>' +
            '<th rowspan="2" class="pay-grid-time-dual text-start small" data-pg-ui-t="시각">시각</th>' +
            '<th rowspan="2" class="text-nowrap" data-pg-ui-t="주문번호">주문번호</th>' +
            '<th rowspan="2" class="text-nowrap" data-pg-ui-t="승인번호">승인번호</th>' +
            '<th rowspan="2" class="text-nowrap" data-pg-ui-t="상태">상태</th>' +
            '<th rowspan="2" class="text-nowrap" data-pg-ui-t="구분">구분</th>' +
            '<th rowspan="2" class="text-end text-nowrap" data-pg-ui-t="매출">매출</th>' +
            '<th colspan="4" class="text-center text-nowrap" data-pg-ui-t="수수료(세부)">수수료(세부)</th>' +
            '<th rowspan="2" class="text-end text-nowrap" data-pg-ui-t="예상지급">예상지급</th>' +
            '<th rowspan="2" class="text-nowrap" data-pg-ui-t="통화">통화</th>' +
            '</tr>' +
            '<tr>' +
            '<th class="text-end text-nowrap small"  data-pg-ui-title="건당(고정) 수수료" title="건당(고정) 수수료" data-pg-ui-t="고정">고정</th>' +
            '<th class="text-end text-nowrap small" data-pg-ui-title="매출 대비 % 수수료(MDR)" title="매출 대비 % 수수료(MDR)" data-pg-ui-t="MDR">MDR</th>' +
            '<th class="text-end text-nowrap small" data-pg-ui-t="담보">담보</th>' +
            '<th class="text-end text-nowrap small"  data-pg-ui-title="승인: 기타%·수수료VAT 합 / 그 외: 무효·환불·수동무효·강제환불·실패 등 건당 수수료" title="승인: 기타%·수수료VAT 합 / 그 외: 무효·환불·수동무효·강제환불·실패 등 건당 수수료" data-pg-ui-t="기타">기타</th>' +
            '</tr>' +
            '</thead><tbody><tr><td colspan="14" class="text-center text-muted py-4"><span data-pg-ui-t="' + escUi(settleDetailEmptyKo) + '">' + escUi(L(settleDetailEmptyKo)) + '</span></td></tr></tbody></table></div></div></div>';
        }
      }
      if (cfg.hasSelectedTable) {
        html += '<div class="card mt-4" id="compMngSelectedCard"><div class="card-header">' + escUi(L('선택된 업체')) + '</div><div class="card-body"><p class="text-muted small mb-2">' + escUi(L('위 테이블에서 선택 후 [선택 저장] 버튼을 누르면 선택된 항목만 아래에 표시됩니다.')) + '</p><div class="table-responsive table-scrollable" id="compMngSelectedWrap"><table class="table table-bordered table-sm" id="grid_compMngSelected"><thead><tr id="compMngSelectedThead"></tr></thead><tbody id="compMngSelectedTbody"><tr><td colspan="20" class="text-center text-muted py-4">' + escUi(L('선택된 항목이 없습니다.')) + '</td></tr></tbody></table></div></div></div>';
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

  /** 결제내역(/calc/payList) 계열 — 카탈로그 라벨을 MENU_SCREENS 복제본에 반영할 때 사용 */
  var PAY_LIST_INTEGRATED_SYNC_URLS = [
    '/calc/payList', '/calc/payNotiList', '/calc/paySuccessList', '/calc/payFailList', '/calc/payRefundList', '/calc/payForceRefundList',
    '/calc/payCancelList', '/calc/payVoidList', '/calc/payEmailVoidList', '/calc/offsetCancList', '/pay/easyPay', '/pay/chatbotPay',
    '/calc/chillPayTrList', '/calc/chillPaySettlementList', '/ops/taxReport'
  ];

  /** 표준 목록 그리드 thead (2단 헤더 그룹 포함) — app.js doSearch thead 생성과 동일 규칙 */
  function buildStandardDataGridTheadHtml(cols, headerGroups, opts) {
    opts = opts || {};
    /** 그리드 헤더: 한글 키를 data-pg-ui-t 로 두어 언어 전환 시 PG_UI_I18N.applyDom 으로 반영 */
    var selectAllKey = String(opts.selectAllTitle != null ? opts.selectAllTitle : '전체선택');
    var esc = function (s) {
      return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
    };
    function spanUiT(ko) {
      var k = String(ko == null ? '' : ko);
      if (!k) return '';
      return '<span data-pg-ui-t="' + esc(k) + '">' + esc(L(k)) + '</span>';
    }
    if (!cols || !cols.length) return '';
    var groups = headerGroups || [];
    if (!groups.length) {
      return '<tr>' + cols.map(function (c) {
        if (c.type === 'checkbox') {
          return '<th data-key="_chk" style="width:40px"><input type="checkbox" class="grid-check-all" data-pg-ui-title="' + esc(selectAllKey) + '"></th>';
        }
        var titleKey = (c.title != null ? String(c.title) : '');
        var thT = titleKey ? (' data-pg-ui-title="' + esc(titleKey) + '"') : '';
        var thClassParts = [];
        if (c.align === 'center') thClassParts.push('text-center');
        if (c.thClass) thClassParts.push(String(c.thClass));
        var thCls = thClassParts.length ? (' class="' + thClassParts.join(' ') + '"') : '';
        return '<th data-key="' + esc(c.key || '') + '"' + thT + thCls + '>' + spanUiT(String(c.label != null ? c.label : c.key || '')) + '</th>';
      }).join('') + '</tr>';
    }
    var keyToGroup = {};
    groups.forEach(function (g, gi) {
      (g.keys || []).forEach(function (k) { keyToGroup[k] = gi; });
    });
    var groupColCount = groups.map(function () { return 0; });
    cols.forEach(function (c) {
      if (c.type === 'checkbox') return;
      var gi = keyToGroup[c.key];
      if (gi !== undefined) groupColCount[gi] += 1;
    });
    var top = '';
    var sub = '';
    var startedGroups = {};
    cols.forEach(function (c) {
      if (c.type === 'checkbox') {
        top += '<th data-key="_chk" rowspan="2" style="width:40px"><input type="checkbox" class="grid-check-all" data-pg-ui-title="' + esc(selectAllKey) + '"></th>';
        return;
      }
      var gi = keyToGroup[c.key];
      if (gi === undefined) {
        var titleKey2 = (c.title != null ? String(c.title) : '');
        var thT2 = titleKey2 ? (' data-pg-ui-title="' + esc(titleKey2) + '"') : '';
        top += '<th data-key="' + esc(c.key || '') + '" rowspan="2"' + thT2 + '>' + spanUiT(String(c.label != null ? c.label : c.key || '')) + '</th>';
      } else {
        if (!startedGroups[gi] && groupColCount[gi] > 0) {
          startedGroups[gi] = true;
          top += '<th colspan="' + groupColCount[gi] + '">' + spanUiT(String(groups[gi].label || '')) + '</th>';
        }
        sub += '<th data-key="' + esc(c.key || '') + '">' + spanUiT(String(c.label != null ? c.label : c.key || '')) + '</th>';
      }
    });
    return '<tr>' + top + '</tr><tr>' + sub + '</tr>';
  }

  /** 정산리포트: columnsBySub 열 키를 합쳐 VIEW SETTING(헬로)용 catalog 생성 */
  (function mergeSettlementReportColumnGuideCatalog() {
    Object.keys(MENU_SCREENS).forEach(function (url) {
      if (url.indexOf('settlementReport') === -1) return;
      var sr = MENU_SCREENS[url];
      if (!sr || !sr.columnsBySub) return;
      var seen = Object.create(null);
      var merged = [];
      function pushCols(arr) {
        if (!arr) return;
        arr.forEach(function (col) {
          if (!col || !col.key) return;
          if (seen[col.key]) return;
          seen[col.key] = 1;
          merged.push(col);
        });
      }
      ['AGG', 'EXE', 'SUM', 'RST'].forEach(function (sub) { pushCols(sr.columnsBySub[sub]); });
      if (sr.columnsRegionalPayout) {
        ['AGG', 'EXE', 'SUM'].forEach(function (sub) { pushCols(sr.columnsRegionalPayout[sub]); });
      }
      sr.columns = merged;
    });
  })();

  function syncPayListIntegratedScreenLabelsFromCatalog() {
    var P = typeof window !== 'undefined' ? window.PG_PAY_LIST_INTEGRATED : null;
    if (!P || !P.columns || !MENU_SCREENS) return;
    var labelByKey = {};
    P.columns.forEach(function (c) {
      if (c && c.key) labelByKey[c.key] = c.label;
    });
    PAY_LIST_INTEGRATED_SYNC_URLS.forEach(function (u) {
      if (u === '/calc/chillPayTrList' || u === '/calc/chillPaySettlementList' || u === '/ops/taxReport' || u === '/ops/integratedReport' || u === '/ops/verifyReport') return;
      var scr = MENU_SCREENS[u];
      if (!scr || !scr.columns) return;
      scr.columns.forEach(function (col) {
        if (col && col.key && labelByKey[col.key] != null) col.label = labelByKey[col.key];
      });
      if (P.headerGroups && P.headerGroups.length) {
        scr.headerGroups = JSON.parse(JSON.stringify(P.headerGroups));
      }
    });
  }

  window.PG_CALC_CYCLE_OPTIONS = CALC_CYCLE_OPTIONS;
  window.PG_CALC_CYCLE_SEARCH_OPTIONS = CALC_CYCLE_SEARCH_OPTIONS;
  window.PG_SCREENS = {
    getScreenHtml: getScreenHtml,
    getMenuScreens: function () { return MENU_SCREENS; },
    buildDistributionListTheadHtml: buildDistributionListTheadHtml,
    buildDistributionListTheadHtmlFromCols: buildDistributionListTheadHtmlFromCols,
    getCompMngSearchCompDivOptions: getCompMngSearchCompDivOptions,
    buildStandardDataGridTheadHtml: buildStandardDataGridTheadHtml,
    buildDailyDetailToolbarHtml: buildDailyDetailToolbarHtml,
    syncPayListIntegratedScreenLabelsFromCatalog: syncPayListIntegratedScreenLabelsFromCatalog,
    getPayListIntegratedSyncUrls: function () { return PAY_LIST_INTEGRATED_SYNC_URLS.slice(); }
  };
})();
