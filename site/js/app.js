/**
 * PG 솔루션 - fxhj와 동일한 메뉴 구성, 화면은 하나씩 구현
 * fnTopMenuMove(url): 메뉴 클릭 시 탭 추가/전환, 우리 페이지 또는 placeholder 표시
 */

(function () {
  'use strict';

  (function initPgUiGlobalModals() {
    if (window._pgUiGlobalModalsInited) return;
    window._pgUiGlobalModalsInited = true;
    window._pgCompLoginIdPending = { loginIdEl: null };
    window._pgHqAccountAccessPending = { pane: null, tabId: null };
    var submitLogin = document.getElementById('pgCompLoginIdChangeSubmitBtn');
    if (submitLogin) {
      submitLogin.addEventListener('click', function () {
        var compId = (document.getElementById('pgCompLoginIdChangeCompId') || {}).value || '';
        var newId = (document.getElementById('pgCompLoginIdChangeNewId') || {}).value || '';
        newId = String(newId).trim();
        if (!compId || !newId) return;
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.compChangeLoginId(compId, newId).then(function () {
          var el = window._pgCompLoginIdPending && window._pgCompLoginIdPending.loginIdEl;
          if (el) el.value = newId;
          if (window.PG_UI && window.PG_UI.closeModal) {
            window.PG_UI.closeModal(document.getElementById('pgCompLoginIdChangeModal'));
          }
        }).catch(function (err) {
          alert(err && err.message ? err.message : 'ID 변경 실패');
        }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      });
    }
    var submitAcc = document.getElementById('pgHqAccountAccessAddSubmitBtn');
    if (submitAcc) {
      submitAcc.addEventListener('click', function () {
        var uid = (document.getElementById('pgHqAccountAccessUsername') || {}).value || '';
        var cc = (document.getElementById('pgHqAccountAccessCompCode') || {}).value || '';
        uid = String(uid).trim();
        cc = String(cc).trim();
        if (!uid || !cc) return;
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.hqAccountAccessAdd({ username: uid, compCode: cc }).then(function () {
          if (window.PG_UI && window.PG_UI.closeModal) {
            window.PG_UI.closeModal(document.getElementById('pgHqAccountAccessAddModal'));
          }
          var pend = window._pgHqAccountAccessPending;
          if (pend && pend.pane && typeof doSearch === 'function') doSearch(pend.pane, pend.tabId, 1);
        }).catch(function (err) {
          alert(err && err.message ? err.message : '추가 실패');
        }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      });
    }
  })();

  /**
   * %·건당·고정액 공통: 소수 첫째 자리까지, 소수부가 0이면 정수만 (300.0→300, 5.6→5.6).
   * 빈 값은 빈 문자열 유지(입력 필드용).
   */
  function pgFmtOneDecimalStripWhole(v) {
    if (v == null || v === '') return '';
    var s = String(v).replace(/,/g, '.').trim();
    if (s === '') return '';
    var n = parseFloat(s);
    if (!isFinite(n)) return String(v);
    var x = Math.round(n * 10) / 10;
    if (Math.abs(x - Math.round(x)) < 1e-9) {
      return String(Math.round(x));
    }
    return x.toFixed(1);
  }

  /** % 필드: 소수 첫째 자리까지, 정수일 때는 .0 생략 */
  function pgFmtPctOneDecimal(v) {
    var out = pgFmtOneDecimalStripWhole(v == null || v === '' ? '0' : v);
    return out === '' ? '0' : out;
  }
  function pgFmtPctOneDecimalInput(v) {
    return pgFmtOneDecimalStripWhole(v);
  }

  /**
   * 가맹점 수수료 정책 목록 등: 건당·고정액·% 셀 표시(통화 구분 없이 동일 규칙).
   * currencyCode는 호환용으로만 받으며 포맷에는 사용하지 않음.
   */
  function pgFmtPolicyListAmount(v, currencyCode) {
    var out = pgFmtOneDecimalStripWhole(v == null || v === '' ? '0' : v);
    return out === '' ? '0' : out;
  }

  /** 업체 상세 등: 수수료·보류 관련 숫자 필드만 동일 표기 규칙 적용. 해당 없으면 null. */
  function pgFmtCompDetailNumericField(fieldName, v) {
    var pctFields = { payRate: 1, feeUsdt: 1, feeFx: 1, fee3dsRate: 1, rollingPct: 1, holdRate: 1 };
    var amtFields = { failFee: 1, usageRate: 1, cancelRate: 1, voidFeePerTx: 1, manualVoidFeePerTx: 1, refundRate: 1, feeSettlementPerTx: 1, chargebackFeePerTx: 1, perTxFee: 1 };
    var dayFields = { rollingDays: 1, holdDays: 1 };
    if (pctFields[fieldName]) return pgFmtPctOneDecimalInput(v);
    if (amtFields[fieldName]) return pgFmtOneDecimalStripWhole(v);
    if (dayFields[fieldName]) {
      var n = parseFloat(String(v).replace(/,/g, '.'));
      return isFinite(n) ? String(Math.round(n)) : String(v);
    }
    return null;
  }

  /** 본사 수수료 템플릿: 배포(Y)만. 기준통화가 있으면 정책 통화와 일치하거나 정책 통화가 비어 있으면(레거시) 전체 허용 */
  function pgFilterDeployedTemplatesForMerchant(templates, baseCurrency) {
    var bc = (baseCurrency || '').trim().toUpperCase();
    var out = [];
    (templates || []).forEach(function (t) {
      if (String(t.deployYn || '').toUpperCase() !== 'Y') return;
      var tc = (t.currencyCode != null && String(t.currencyCode).trim() !== '') ? String(t.currencyCode).trim().toUpperCase() : '';
      if (bc && tc && tc !== bc) return;
      out.push(t);
    });
    return out;
  }
  function pgHqPolicyScopeOptionsHtml(filteredTemplates) {
    var opts = '<option value="">기본(DEFAULT)</option>';
    (filteredTemplates || []).forEach(function (t) {
      var scope = t.scope || '';
      var name = t.policyName || scope;
      var cc = (t.currencyCode != null && String(t.currencyCode).trim() !== '') ? String(t.currencyCode).trim().toUpperCase() : '';
      var lab = name + (cc ? ' (' + cc + ')' : '');
      opts += '<option value="' + String(scope).replace(/"/g, '&quot;') + '">' +
        String(lab).replace(/</g, '&lt;').replace(/"/g, '&quot;') + '</option>';
    });
    return opts;
  }

  var COUNTRY_OTHER_TOP = ['CHINA', 'HONGKONG', 'INDONESIA', 'JAPAN', 'KOREA', 'MALAYSIA', 'SINGAPORE', 'THAILAND', 'USA', 'VIETNAM'];
  var COUNTRY_OTHER_REST = ['AFGHANISTAN', 'ALBANIA', 'ALGERIA', 'ARGENTINA', 'ARMENIA', 'AUSTRALIA', 'AUSTRIA', 'AZERBAIJAN', 'BAHRAIN', 'BANGLADESH', 'BELARUS', 'BELGIUM', 'BOLIVIA', 'BOSNIA', 'BRAZIL', 'BULGARIA', 'CAMBODIA', 'CAMEROON', 'CANADA', 'CHILE', 'COLOMBIA', 'COSTA RICA', 'CROATIA', 'CUBA', 'CYPRUS', 'CZECH REPUBLIC', 'DENMARK', 'ECUADOR', 'EGYPT', 'ESTONIA', 'ETHIOPIA', 'FINLAND', 'FRANCE', 'GEORGIA', 'GERMANY', 'GHANA', 'GREECE', 'GUATEMALA', 'HONDURAS', 'HUNGARY', 'ICELAND', 'INDIA', 'IRAN', 'IRAQ', 'IRELAND', 'ISRAEL', 'ITALY', 'JORDAN', 'KAZAKHSTAN', 'KENYA', 'KUWAIT', 'KYRGYZSTAN', 'LAOS', 'LATVIA', 'LEBANON', 'LIBYA', 'LITHUANIA', 'LUXEMBOURG', 'MACEDONIA', 'MADAGASCAR', 'MALAWI', 'MALTA', 'MAURITIUS', 'MEXICO', 'MOLDOVA', 'MONGOLIA', 'MONTENEGRO', 'MOROCCO', 'MOZAMBIQUE', 'MYANMAR', 'NEPAL', 'NETHERLANDS', 'NEW ZEALAND', 'NICARAGUA', 'NIGERIA', 'NORTH KOREA', 'NORWAY', 'OMAN', 'PAKISTAN', 'PALESTINE', 'PANAMA', 'PARAGUAY', 'PERU', 'PHILIPPINES', 'POLAND', 'PORTUGAL', 'QATAR', 'ROMANIA', 'RUSSIA', 'RWANDA', 'SAUDI ARABIA', 'SERBIA', 'SLOVAKIA', 'SLOVENIA', 'SOUTH AFRICA', 'SPAIN', 'SRI LANKA', 'SUDAN', 'SWEDEN', 'SWITZERLAND', 'SYRIA', 'TAIWAN', 'TANZANIA', 'TUNISIA', 'TURKEY', 'TURKMENISTAN', 'UGANDA', 'UKRAINE', 'UAE', 'UK', 'URUGUAY', 'UZBEKISTAN', 'VENEZUELA', 'YEMEN', 'ZAMBIA', 'ZIMBABWE'];
  window.PG_COUNTRY_OTHER_OPTIONS = (function () {
    var rest = COUNTRY_OTHER_REST.filter(function (c) { return COUNTRY_OTHER_TOP.indexOf(c) === -1; }).sort();
    var html = '<option value="">선택</option>';
    COUNTRY_OTHER_TOP.forEach(function (c) { html += '<option value="' + c + '">' + c + '</option>'; });
    html += '<option value="" disabled>-------------------</option>';
    rest.forEach(function (c) { html += '<option value="' + c + '">' + c + '</option>'; });
    return html;
  })();
  var PHONE_PRIORITY_ORDER = ['JP', 'KR', 'TH', 'US', 'CN', 'SG', 'HK'];
  var PHONE_DIAL_BY_ISO = {
    AF: '+93', AL: '+355', DZ: '+213', AR: '+54', AU: '+61', AT: '+43', BD: '+880', BE: '+32', BR: '+55', BN: '+673', BG: '+359', KH: '+855',
    CA: '+1', CL: '+56', CN: '+86', CO: '+57', HR: '+385', CY: '+357', CZ: '+420', DK: '+45', EG: '+20', FI: '+358', FR: '+33', DE: '+49',
    GR: '+30', HK: '+852', HU: '+36', IN: '+91', ID: '+62', IE: '+353', IL: '+972', IT: '+39', JP: '+81', JO: '+962', KZ: '+7', KE: '+254',
    KR: '+82', KW: '+965', LA: '+856', LU: '+352', MO: '+853', MY: '+60', MX: '+52', MM: '+95', NL: '+31', NZ: '+64', NG: '+234', NO: '+47',
    PK: '+92', PH: '+63', PL: '+48', PT: '+351', QA: '+974', RO: '+40', RU: '+7', SA: '+966', RS: '+381', SG: '+65', SK: '+421', SI: '+386',
    ZA: '+27', ES: '+34', LK: '+94', SE: '+46', CH: '+41', TW: '+886', TH: '+66', TR: '+90', AE: '+971', GB: '+44', US: '+1', VN: '+84'
  };
  /** Priority countries: English labels only (no Korean in dropdown). */
  var PHONE_EN_LABEL_BY_ISO = {
    JP: 'Japan', KR: 'South Korea', TH: 'Thailand', US: 'United States', CN: 'China', SG: 'Singapore', HK: 'Hong Kong'
  };
  var PG_INTL_PHONE_OPTIONS_CACHE_VER = 2;
  function phoneLabelAlphaGroup(label) {
    var c = String(label || '').trim().charAt(0).toUpperCase();
    return /^[A-Z]$/.test(c) ? c : '#';
  }
  function buildIntlPhoneOptionsHtml() {
    if (window.PG_INTL_PHONE_OPTIONS_CACHE_VER === PG_INTL_PHONE_OPTIONS_CACHE_VER && window.PG_INTL_PHONE_OPTIONS) {
      return window.PG_INTL_PHONE_OPTIONS;
    }
    var regionCodes = Object.keys(PHONE_DIAL_BY_ISO);
    if (typeof Intl !== 'undefined' && Intl.DisplayNames && Intl.supportedValuesOf) {
      try {
        var supported = Intl.supportedValuesOf('region') || [];
        regionCodes = supported.filter(function (iso) { return !!PHONE_DIAL_BY_ISO[iso]; });
      } catch (e) {}
    }
    var regionName = null;
    try {
      regionName = new Intl.DisplayNames(['en'], { type: 'region' });
    } catch (e) {}
    var byIso = {};
    regionCodes.forEach(function (iso) {
      byIso[iso] = {
        iso: iso,
        dial: PHONE_DIAL_BY_ISO[iso],
        label: PHONE_EN_LABEL_BY_ISO[iso] || (regionName && regionName.of(iso)) || iso
      };
    });
    var priorityRows = PHONE_PRIORITY_ORDER.map(function (iso) { return byIso[iso]; }).filter(Boolean);
    var prioritySet = {};
    PHONE_PRIORITY_ORDER.forEach(function (iso) { prioritySet[iso] = true; });
    var restRows = regionCodes.filter(function (iso) { return !prioritySet[iso]; }).map(function (iso) { return byIso[iso]; });
    restRows.sort(function (a, b) { return String(a.label).localeCompare(String(b.label), 'en'); });
    var parts = [];
    priorityRows.forEach(function (r) {
      parts.push('<option value="' + r.dial + '">' + r.label + ' (' + r.dial + ')</option>');
    });
    parts.push('<option value="" disabled>---------------</option>');
    var lastGroup = null;
    restRows.forEach(function (r) {
      var g = phoneLabelAlphaGroup(r.label);
      if (g !== lastGroup) {
        lastGroup = g;
        parts.push('<option value="" disabled>---------- ' + g + ' ----------</option>');
      }
      parts.push('<option value="' + r.dial + '">' + r.label + ' (' + r.dial + ')</option>');
    });
    window.PG_INTL_PHONE_OPTIONS_CACHE_VER = PG_INTL_PHONE_OPTIONS_CACHE_VER;
    window.PG_INTL_PHONE_OPTIONS = parts.join('');
    return window.PG_INTL_PHONE_OPTIONS;
  }
  /** 노티 대상 피커: CALLBACK/RESULT 구분·쌍 선택 */
  function pgEscHtml(s) {
    return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
  }
  /** 브랜딩: 저장 URL에서 파일명만 (표시 입력창) */
  function pgBrandingBasenameFromStoredUrl(url) {
    if (url == null) return '';
    var s = String(url).trim();
    if (!s) return '';
    var q = s.indexOf('?');
    if (q >= 0) s = s.slice(0, q);
    var slash = s.lastIndexOf('/');
    return slash >= 0 ? s.slice(slash + 1) : s;
  }
  function pgBrandingDisplayNameAfterUpload(data, fallbackFile) {
    var fromApi = data && data.originalFileName != null ? String(data.originalFileName).trim() : '';
    if (fromApi) return fromApi;
    if (data && data.url) return pgBrandingBasenameFromStoredUrl(data.url);
    if (fallbackFile && fallbackFile.name) return String(fallbackFile.name);
    return '';
  }
  function pgBrandingSetImageDisplayInput(rootEl, imageType, data, fallbackFile) {
    if (!rootEl) return;
    var id = imageType === 'logo' ? 'brandingLogoImageUrl' : 'brandingMainImageUrl';
    var el = rootEl.querySelector('#' + id);
    if (!el) return;
    var label = pgBrandingDisplayNameAfterUpload(data, fallbackFile);
    if (label) el.value = label;
  }
  function pgBrandingFillImageDisplayFromFetch(rootEl, b) {
    if (!rootEl || !b) return;
    var mainEl = rootEl.querySelector('#brandingMainImageUrl');
    var logoEl = rootEl.querySelector('#brandingLogoImageUrl');
    if (mainEl) mainEl.value = b.mainImageUrl ? pgBrandingBasenameFromStoredUrl(b.mainImageUrl) : '';
    if (logoEl) logoEl.value = b.logoImageUrl ? pgBrandingBasenameFromStoredUrl(b.logoImageUrl) : '';
  }
  function pgShortNotifyChannel(t) {
    var ch = String((t && t.channelType) || '').toUpperCase();
    return ch === 'RESULT' ? 'RESULT' : 'CALLBACK';
  }
  function pgNotifyPairKey(code) {
    var c = String(code || '');
    if (/^cb/i.test(c) || /^rs/i.test(c)) return 'p:' + c.substring(2).toLowerCase();
    return 'legacy:' + c;
  }
  function pgGroupNotifyTargetsByPairKey(list) {
    var byKey = {};
    var keys = [];
    (list || []).forEach(function (t) {
      var k = pgNotifyPairKey(t.targetCode);
      if (!byKey[k]) {
        byKey[k] = [];
        keys.push(k);
      }
      byKey[k].push(t);
    });
    keys.sort(function (ka, kb) {
      var maxA = Math.max.apply(null, byKey[ka].map(function (x) { return Number(x.id) || 0; }));
      var maxB = Math.max.apply(null, byKey[kb].map(function (x) { return Number(x.id) || 0; }));
      return maxB - maxA;
    });
    return keys.map(function (k) {
      var g = byKey[k].slice();
      g.sort(function (a, b) {
        var oa = pgShortNotifyChannel(a) === 'CALLBACK' ? 0 : 1;
        var ob = pgShortNotifyChannel(b) === 'CALLBACK' ? 0 : 1;
        if (oa !== ob) return oa - ob;
        return (Number(a.id) || 0) - (Number(b.id) || 0);
      });
      return g;
    });
  }
  function pgNotifyTargetsFilterKeyword(list, kw) {
    var k = kw ? String(kw).trim().toLowerCase() : '';
    return (list || []).filter(function (t) {
      if (!k) return true;
      return String(t.targetName || '').toLowerCase().indexOf(k) >= 0 || String(t.targetUrl || '').toLowerCase().indexOf(k) >= 0;
    });
  }
  buildIntlPhoneOptionsHtml();

  function parseIntlPhone(rawValue) {
    var raw = (rawValue == null) ? '' : String(rawValue).trim();
    if (!raw) return { code: '+82', number: '' };
    var m = raw.match(/^(\+\d{1,4})[\s-]*(.*)$/);
    if (!m) return { code: '+82', number: raw };
    return { code: m[1], number: (m[2] || '').trim() };
  }

  function syncIntlPhoneHidden(form, fieldName) {
    if (!form || !fieldName) return;
    var hidden = form.querySelector('[name="' + fieldName + '"]');
    var codeSel = form.querySelector('[data-intl-phone-code-for="' + fieldName + '"]');
    var numInp = form.querySelector('[data-intl-phone-number-for="' + fieldName + '"]');
    if (!hidden || !codeSel || !numInp) return;
    var code = String(codeSel.value || '').trim();
    var number = String(numInp.value || '').trim();
    hidden.value = number ? ((code || '+82') + ' ' + number) : '';
  }

  function initIntlPhoneFields(root) {
    if (!root) return;
    root.querySelectorAll('[data-intl-phone-group]').forEach(function (group) {
      if (group._intlPhoneInit) return;
      group._intlPhoneInit = true;
      var fieldName = group.getAttribute('data-intl-phone-group') || '';
      var hidden = group.querySelector('input[type="hidden"][name="' + fieldName + '"]');
      var codeSel = group.querySelector('[data-intl-phone-code-for="' + fieldName + '"]');
      var numInp = group.querySelector('[data-intl-phone-number-for="' + fieldName + '"]');
      if (!hidden || !codeSel || !numInp) return;
      if (!codeSel.querySelector('option')) codeSel.innerHTML = buildIntlPhoneOptionsHtml();
      var parsed = parseIntlPhone(hidden.value);
      if (codeSel.querySelector('option[value="' + parsed.code + '"]')) codeSel.value = parsed.code;
      else codeSel.value = '+82';
      numInp.value = parsed.number;
      codeSel.addEventListener('change', function () { syncIntlPhoneHidden(root, fieldName); });
      numInp.addEventListener('input', function () { syncIntlPhoneHidden(root, fieldName); });
    });
  }

  window.SITE_CONFIG = {
    contentBaseUrl: '',
    contentMode: 'placeholder',  // placeholder: 탭에 HTML 직접 삽입 → index.html 모달(parentCompSearchModal 등) 접근 가능
    paymentBaseUrl: ''  // 간편결제 URL 베이스 (예: https://api.example.com) - 비어있으면 현재 origin 사용
  };

  // 메뉴별 URL → 라벨, parent (브레드크럼/탭 제목용) - FXHJ + 본사설정 + 리스크 통합
  var MENU_INFO = {
    '/hq/pgApiMng': { label: 'PG사 API 연동', parent: '본사설정' },
    '/hq/defaultCommission': { label: '기본정책', parent: '본사설정' },
    '/hq/chargebackPolicy': { label: '차지백 구간정책', parent: '본사설정' },
    '/hq/businessDaySetting': { label: '영업일설정', parent: '본사설정' },
    '/hq/apiConfig': { label: 'API 구성 세팅', parent: '본사설정' },
    '/hq/domainConfig': { label: '도메인구성', parent: '본사설정' },
    '/hq/serverManage': { label: '서버관리', parent: '본사설정' },
    '/hq/permissionMng': { label: '조직별 권한 세팅', parent: '본사설정' },
    '/hq/notifyEnv': { label: '전산노티·결제환경', parent: '본사설정' },
    '/hq/notifyMapping': { label: '노티매핑설정', parent: '본사설정' },
    '/hq/orgViewColumnAllowance': { label: '조직별 노출설정', parent: '본사설정' },
    '/hq/accountMng': { label: '계정·업체접근', parent: '본사설정' },
    '/system/noticeList': { label: '공지사항', parent: '업체관리' },
    '/comp/myCompMng': { label: '업체정보조회', parent: '업체관리' },
    '/comp/compMngTree': { label: '업체관리', parent: '업체관리' },
    '/comp/compDetail': { label: '업체정보', parent: '업체관리' },
    '/commission/commisionList': { label: '수수료관리', parent: '업체관리' },
    '/comp/compInfoHistList': { label: '업체변경이력', parent: '업체관리' },
    '/comp/compReg': { label: '업체등록', parent: '업체관리' },
    '/calc/payList': { label: '결제내역', parent: '결제관리' },
    '/calc/payNotiList': { label: '노티내역', parent: '결제관리' },
    '/calc/paySuccessList': { label: '성공내역', parent: '결제관리' },
    '/calc/payFailList': { label: '실패내역', parent: '결제관리' },
    '/calc/payRefundList': { label: '환불내역', parent: '결제관리' },
    '/calc/payForceRefundList': { label: '강제환불', parent: '결제관리' },
    '/calc/payCancelList': { label: '취소내역', parent: '결제관리' },
    '/calc/offsetCancList': { label: '상계취소내역', parent: '결제관리' },
    '/pay/easyPay': { label: 'URL결제내역', parent: '결제관리' },
    '/pay/chatbotPay': { label: '챗봇결제내역', parent: '결제관리' },
    '/calc/calcList': { label: '유통망정산내역', parent: '정산관리' },
    '/calc/calcGmList': { label: '가맹정산내역', parent: '정산관리' },
    '/calc/feeList': { label: '수수료내역', parent: '정산관리' },
    '/settlement/feeList': { label: '수수료내역', parent: '정산관리' },
    '/calc/compPointMngList': { label: '환수금관리', parent: '정산관리' },
    '/calc/balcInfo': { label: '잔액/미수금관리', parent: '정산관리' },
    '/calc/balanceList': { label: '잔액내역', parent: '정산관리' },
    '/calc/unpaidMng': { label: '미수금관리', parent: '정산관리' },
    '/calc/exCalcList': { label: '정산실행', parent: '정산관리' },
    '/calc/settlementReport': { label: '정산리포트', parent: '정산관리' },
    '/pay/payHoldList': { label: '정산보류내역', parent: '정산관리' },
    '/calc/collateralList': { label: '담보금내역', parent: '정산관리' },
    '/settlement/collateralList': { label: '담보금내역', parent: '정산관리' },
    '/noti/notiUrlMng': { label: '결제통보 URL관리', parent: '통보관리' },
    '/noti/notiSendMngList': { label: '결제통보 전송관리', parent: '통보관리' },
    '/noti/notiCashReceiptUrlMng': { label: '현금영수증통보 URL관리', parent: '통보관리' },
    '/noti/notiCashReceiptSendMngList': { label: '현금영수증통보 전송관리', parent: '통보관리' },
    '/user/userMng': { label: '사용자관리', parent: '사용자관리' },
    '/set/gridSetMng': { label: '메뉴별항목순서관리', parent: '사용자관리' },
    '/risk/list': { label: '리스크 현황', parent: '리스크관리' }
  };

  var config = window.SITE_CONFIG;
  var TAB_UL = 'copyTopTabUl';
  var TAB_MAIN = 'topTapMain';
  var CONTENT_FRAME = 'content-frame';
  var PLACEHOLDER_ID = 'content-placeholder';
  var TABLE_ROW_PADDING_KEY = 'pg_table_row_padding_y';

  function getTableRowPaddingY() {
    var v = parseInt(localStorage.getItem(TABLE_ROW_PADDING_KEY), 10);
    return (isNaN(v) || v < 4 || v > 40) ? 6 : v;
  }
  function setTableRowPaddingY(px) {
    var v = Math.max(4, Math.min(40, px));
    document.documentElement.style.setProperty('--table-row-padding-y', v + 'px');
    localStorage.setItem(TABLE_ROW_PADDING_KEY, String(v));
  }
  /** 예전: 행 사이 점선 리사이즈 줄 삽입. UX상 제거(그리드·목록 테이블 공통). */
  function injectTableRowResizeHandles(/* tbody, colCount */) {
    return;
  }

  function getTabIdFromUrl(url) {
    if (!url || url === '/main') return 'main';
    var path = (url || '').replace(/^\//, '').replace(/\//g, '_');
    return path || 'main';
  }

  function getSessionUser() {
    try { return JSON.parse(sessionStorage.getItem('pg_admin_user') || '{}') || {}; } catch (e) { return {}; }
  }

  // 업체관리는 내 조직 기준 하위만 노출(상위 조직 미노출). 편집 가능 여부는 권한(OBSERVER/MODIFY/DELETE)로 별도 제어.
  // API가 본인 행을 빼고 하위만 줄 때도(영업점 등) orgUnitId로 하위만 남긴다. me 없을 때 전체 목록을 그대로 두지 않는다.
  function applyObserverCompTreeScope(list, url) {
    if (url !== '/comp/compMngTree') return list || [];
    if (!Array.isArray(list) || list.length === 0) return list || [];
    var u = getSessionUser();
    if (u && String(u.role || '').toUpperCase() === 'ADMIN') return list;

    var myCompId = String((u && u.compId) || '').trim();
    var myOuId = u && u.orgUnitId != null && u.orgUnitId !== '' ? String(u.orgUnitId) : '';
    if (!myCompId && !myOuId) return list;

    var byCompId = {};
    list.forEach(function (r) {
      if (!r) return;
      var cid = r.compId != null ? String(r.compId).trim() : '';
      if (cid) byCompId[cid] = r;
    });
    var me = myCompId ? byCompId[myCompId] : null;

    var keepIds = {};
    var seedParents = [];

    if (me && me.id != null) {
      var myId = String(me.id);
      keepIds[myId] = true;
      seedParents.push(myId);
    } else if (myOuId) {
      seedParents.push(myOuId);
    } else {
      return [];
    }

    seedParents.forEach(function (rootParent) {
      var q = [rootParent];
      var qi = 0;
      while (qi < q.length) {
        var parent = q[qi++];
        list.forEach(function (r) {
          var rid = r && r.id != null ? String(r.id) : '';
          var rpid = r && r.parentId != null ? String(r.parentId) : '';
          if (!rid || !rpid) return;
          if (rpid === parent && !keepIds[rid]) {
            keepIds[rid] = true;
            q.push(rid);
          }
        });
      }
    });

    if (Object.keys(keepIds).length === 0) return [];
    return list.filter(function (r) {
      var rid = r && r.id != null ? String(r.id) : '';
      return !!keepIds[rid];
    });
  }

  /** 업체관리 상세: 본인 소속 업체(compId) 레코드는 조회만(저장 버튼 비활성). API도 READ_ONLY_SELF_COMP. */
  window.applyCompDetailReadOnlyIfOwnNonMerchant = function (pane) {
    if (!pane) return;
    pane.classList.remove('pg-comp-detail-self-readonly');
    var form = pane.querySelector('#compDetailForm');
    if (!form) return;
    var u = getSessionUser();
    if (u && String(u.role || '').toUpperCase() === 'ADMIN') return;
    var mine = String((u && u.compId) || '').trim();
    var cidEl = form.querySelector('[name="compId"]');
    var cid = cidEl && cidEl.value ? String(cidEl.value).trim() : '';
    if (!mine || !cid || mine !== cid) return;
    pane.classList.add('pg-comp-detail-self-readonly');
    form.querySelectorAll('input, select, textarea').forEach(function (el) {
      if (el.type === 'hidden') return;
      el.disabled = true;
      if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') el.readOnly = true;
    });
    var saveBtn = pane.querySelector('#compDetailSaveBtn');
    if (saveBtn) {
      saveBtn.disabled = true;
      saveBtn.setAttribute('aria-disabled', 'true');
      saveBtn.classList.remove('btn-primary', 'btn-success', 'btn-danger', 'btn-info', 'btn-warning');
      saveBtn.classList.add('btn-secondary');
      saveBtn.style.pointerEvents = 'none';
    }
  };

  /**
   * 조직별 권한 세팅(pagePermissions)만 사용 — 가맹점 전용 하드코딩 제거.
   * NONE / OBSERVER / MODIFY / DELETE. ADMIN·미연결 → DELETE(무제한).
   */
  function getPagePermissionForUrl(url) {
    if (!url || url === '/main') return 'DELETE';
    var u = getSessionUser();
    if (u && String(u.role || '').toUpperCase() === 'ADMIN') return 'DELETE';
    var pp = u.pagePermissions;
    if (pp && typeof pp === 'object') {
      var aliases = [url];
      // 업체정보조회 권한은 compInfo/myCompMng 중 어느 키로 저장돼도 동일 적용
      if (url === '/comp/myCompMng') aliases.push('/comp/compInfo');
      else if (url === '/comp/compInfo') aliases.push('/comp/myCompMng');
      // 업체상세(compDetail)는 업체관리(compMngTree) 권한을 따라간다.
      if (url === '/comp/compDetail') aliases.push('/comp/compMngTree');
      else if (url === '/comp/compMngTree') aliases.push('/comp/compDetail');
      for (var i = 0; i < aliases.length; i++) {
        var key = aliases[i];
        if (Object.prototype.hasOwnProperty.call(pp, key)) {
          var pv = pp[key];
          return pv != null && String(pv).trim() !== '' ? String(pv).trim().toUpperCase() : 'DELETE';
        }
      }
    }
    return 'DELETE';
  }

  function isMenuAllowedForCurrentUser(url) {
    return getPagePermissionForUrl(url) !== 'NONE';
  }

  function applyAdminOnlyMenuItems() {
    var u = getSessionUser();
    if (u && String(u.role || '').toUpperCase() === 'ADMIN') return;
    var ol = String(u && u.orgLevel != null ? u.orgLevel : '').toUpperCase();
    var showPerm = ol === 'HEADQUARTERS' || ol === 'REGIONAL' || ol === 'MASTER_DIST';
    document.querySelectorAll('.child-li[data-url="/hq/permissionMng"]').forEach(function (li) {
      if (showPerm) {
        li.style.display = '';
      } else {
        li.style.display = 'none';
        li.classList.remove('mm-active');
      }
    });
  }

  /** 조직별 권한 세팅(pagePermissions): NONE 이면 메뉴 숨김 */
  function applyMenuVisibilityByPagePermissions() {
    document.querySelectorAll('.side-nav .child-li[data-url]').forEach(function (li) {
      var u = li.getAttribute('data-url') || '';
      if (!u) return;
      if (getPagePermissionForUrl(u) === 'NONE') {
        li.style.display = 'none';
        li.classList.remove('mm-active');
      } else {
        li.style.display = '';
      }
    });
    // 하위 메뉴가 모두 숨김이면 상위 큰메뉴도 숨김
    document.querySelectorAll('.side-nav .side-nav-item').forEach(function (item) {
      var children = item.querySelectorAll('.side-nav-second-level .child-li[data-url]');
      if (!children || children.length === 0) return;
      var hasVisibleChild = false;
      children.forEach(function (li) {
        if (li.style.display !== 'none') hasVisibleChild = true;
      });
      if (!hasVisibleChild) {
        item.style.display = 'none';
        item.classList.remove('mm-active');
        var sub = item.querySelector('.side-nav-second-level');
        if (sub) sub.classList.remove('mm-show');
        var link = item.querySelector('.side-nav-link');
        if (link) link.setAttribute('aria-expanded', 'false');
      } else {
        item.style.display = '';
      }
    });
  }

  /** 현재 강조 메뉴가 접근불가면 사이드바만 허용된 첫 메뉴로 맞춤(탭 자동 오픈은 하지 않음) */
  function redirectIfActiveMenuForbidden() {
    var cur = document.querySelector('.side-nav .child-li.mm-active');
    var cu = cur ? (cur.getAttribute('data-url') || '') : '';
    if (!cu || isMenuAllowedForCurrentUser(cu)) return;
    var items = document.querySelectorAll('.side-nav .child-li[data-url]');
    for (var i = 0; i < items.length; i++) {
      var u = items[i].getAttribute('data-url') || '';
      if (!u) continue;
      if (isMenuAllowedForCurrentUser(u)) {
        setActiveMenuByUrl(u);
        return;
      }
    }
    setActiveMenuByUrl('/main');
  }

  function getFullUrl(path) {
    path = (path || '').replace(/^\//, '');
    if (config.contentBaseUrl) {
      return config.contentBaseUrl.replace(/\/$/, '') + '/' + path;
    }
    return '/' + path;
  }

  function ensureContentArea() {
    var frame = document.getElementById(CONTENT_FRAME);
    if (frame) return frame;
    var container = document.getElementById('contentsMain');
    if (!container) return null;
    frame = document.createElement('iframe');
    frame.id = CONTENT_FRAME;
    frame.name = 'content-frame';
    frame.setAttribute('style', 'width:100%;height:calc(100vh - 140px);min-height:400px;border:0;display:none');
    container.appendChild(frame);
    return frame;
  }

  var MAX_TOP_TABS = 12;

  function initBankByCountry(pane) {
    if (!pane) return;
    var countrySel = pane.querySelector('select[data-load-countries="true"]');
    var bankSel = pane.querySelector('select[data-bank-by-country="true"]');
    if (!countrySel || !bankSel || bankSel._bankByCountryInit) return;
    bankSel._bankByCountryInit = true;
    function loadCountries() {
      window.PG_API.bankCountries().then(function (list) {
        var cur = countrySel.value;
        countrySel.innerHTML = '<option value="">선택</option>' + (list || []).map(function (c) {
          return '<option value="' + (c.code || '') + '">' + (c.name || c.code) + '</option>';
        }).join('');
        if (cur) countrySel.value = cur;
      }).catch(function () {});
    }
    function loadBanks(countryCd, preserveBank) {
      bankSel.innerHTML = '<option value="">국가 선택 후</option>';
      if (!countryCd || !countryCd.trim()) return;
      window.PG_API.bankListByCountry(countryCd).then(function (list) {
        bankSel.innerHTML = '<option value="">선택</option>' + (list || []).map(function (b) {
          return '<option value="' + (b.code || '') + '">' + (b.name || b.code) + '</option>';
        }).join('');
        if (preserveBank) bankSel.value = preserveBank;
      }).catch(function () {});
    }
    loadCountries();
    countrySel.addEventListener('change', function () {
      loadBanks(this.value);
    });
    if (countrySel.value) loadBanks(countrySel.value, bankSel.value);
  }

  var BANK_FALLBACK = {
    KR: [{ code: '02', name: '산업은행' }, { code: '03', name: '기업은행' }, { code: '04', name: '국민' }, { code: '07', name: '수협' }, { code: '11', name: 'NH농협' }, { code: '12', name: '지역농·축협' }, { code: '20', name: '우리' }, { code: '23', name: 'SC제일은행' }, { code: '27', name: '한국씨티' }, { code: '31', name: '대구은행' }, { code: '32', name: '부산은행' }, { code: '34', name: '광주은행' }, { code: '35', name: '제주은행' }, { code: '37', name: '전북은행' }, { code: '39', name: '경남은행' }, { code: '45', name: '새마을금고' }, { code: '48', name: '신협' }, { code: '50', name: '상호저축은행' }, { code: '64', name: '산림조합' }, { code: '71', name: '우체국' }, { code: '81', name: 'KEB하나' }, { code: '88', name: '신한' }, { code: '89', name: '케이뱅크' }, { code: '90', name: '카카오뱅크' }, { code: '92', name: '토스뱅크' }],
    JP: [{ code: '0001', name: 'みずほ銀行' }, { code: '0005', name: '三菱UFJ銀行' }, { code: '0009', name: '三井住友銀行' }, { code: '0010', name: 'りそな銀行' }, { code: '0017', name: '埼玉りそな銀行' }, { code: '0033', name: 'ジャパンネット銀行' }, { code: '0034', name: 'セブン銀行' }, { code: '0036', name: '楽天銀行' }, { code: '0038', name: 'ソニー銀行' }, { code: '0039', name: 'auじぶん銀行' }, { code: '0040', name: 'イオン銀行' }, { code: '9900', name: 'ゆうちょ銀行' }, { code: '0116', name: '横浜銀行' }, { code: '0117', name: '静岡銀行' }, { code: '0118', name: '北陸銀行' }],
    TH: [{ code: '002', name: 'Bangkok Bank' }, { code: '004', name: 'Kasikorn Bank' }, { code: '006', name: 'Krung Thai Bank' }, { code: '009', name: 'HSBC Thailand' }, { code: '011', name: 'TMBThanachart Bank' }, { code: '014', name: 'Siam Commercial Bank' }, { code: '022', name: 'Standard Chartered' }, { code: '024', name: 'UOB Thailand' }, { code: '025', name: 'Bank of Ayudhya (Krungsri)' }, { code: '030', name: 'Government Savings Bank' }, { code: '034', name: 'Government Housing Bank' }, { code: '067', name: 'ICBC Thai' }, { code: '069', name: 'Kiatnakin Phatra Bank' }, { code: '073', name: 'Land and Houses Bank' }, { code: '076', name: 'Thanachart Bank' }]
  };
  function applyBankOptions(bankSel, list, preserveBank) {
    bankSel.innerHTML = '<option value="">선택</option>' + (list || []).map(function (b) {
      return '<option value="' + (b.code || '') + '">' + (b.name || b.code) + '</option>';
    }).join('');
    if (preserveBank) bankSel.value = preserveBank;
  }
  function loadBanksForCountry(countryCd, bankSel, preserveBank) {
    if (!bankSel || !countryCd || !countryCd.trim()) return;
    bankSel.innerHTML = '<option value="">국가 선택 후</option>';
    var fallback = BANK_FALLBACK[countryCd.toUpperCase()];
    window.PG_API.bankListByCountry(countryCd).then(function (list) {
      applyBankOptions(bankSel, list && list.length ? list : fallback, preserveBank);
    }).catch(function (err) {
      if (fallback) applyBankOptions(bankSel, fallback, preserveBank);
      else if (typeof console !== 'undefined' && console.error) console.error('은행 목록 조회 실패:', err);
    });
  }
  function refreshCountryBankAfterFill(pane, bankCdVal) {
    if (!pane) return;
    pane.querySelectorAll('.country-bank-row').forEach(function (row) {
      var cs = row.querySelector('select[data-country-select]');
      var bs = row.querySelector('select[data-bank-select]');
      if (!cs || !bs || (cs.value !== 'JP' && cs.value !== 'KR' && cs.value !== 'TH')) return;
      loadBanksForCountry(cs.value, bs, bankCdVal);
    });
  }

  function initCountryAddressGroup(pane) {
    if (!pane) return;
    pane.querySelectorAll('.country-address-row').forEach(function (row) {
      if (row._countryAddressInit) return;
      row._countryAddressInit = true;
      var countrySel = row.querySelector('select[data-addr-country-select]');
      var countryOtherWrap = row.querySelector('.addr-country-other-wrap');
      var zipSearchWrap = row.querySelector('[data-zip-search-wrap]');
      var zipInput = row.querySelector('input[name="zipCode"]');
      var addrInput = row.querySelector('input[name="addr"]');
      var searchBtn = row.querySelector('[data-addr-zip-search]');
      if (!countrySel || !zipInput) return;
      function toggleByCountry() {
        var v = countrySel.value;
        if (v === 'OTHER') {
          if (countryOtherWrap) countryOtherWrap.classList.remove('d-none');
          if (searchBtn) searchBtn.style.display = 'none';
          if (zipInput) zipInput.placeholder = '직접입력';
        } else {
          if (countryOtherWrap) countryOtherWrap.classList.add('d-none');
          if (v === 'KR') {
            if (searchBtn) searchBtn.style.display = '';
            if (zipInput) zipInput.placeholder = '검색';
          } else {
            if (searchBtn) searchBtn.style.display = 'none';
            if (zipInput) zipInput.placeholder = '직접입력';
          }
        }
      }
      countrySel.addEventListener('change', toggleByCountry);
      toggleByCountry();
      if (searchBtn) {
        searchBtn.addEventListener('click', function () {
          if (countrySel.value !== 'KR') return;
          if (typeof daum === 'undefined') { alert('우편번호 서비스를 불러올 수 없습니다.'); return; }
          new daum.Postcode({
            oncomplete: function (data) {
              zipInput.value = data.zonecode || '';
              addrInput.value = data.roadAddress || data.jibunAddress || '';
            }
          }).open();
        });
      }
    });
  }

  function initCountryBankGroup(pane) {
    if (!pane) return;
    pane.querySelectorAll('.country-bank-row').forEach(function (row) {
      if (row._countryBankInit) return;
      row._countryBankInit = true;
      var countrySel = row.querySelector('select[data-country-select]');
      var countryOtherWrap = row.querySelector('.country-other-wrap');
      var bankSelectWrap = row.querySelector('.bank-select-wrap');
      var bankTextWrap = row.querySelector('.bank-text-wrap');
      var bankSel = row.querySelector('select[data-bank-select]');
      var bankCdText = row.querySelector('input[name="bankCdText"]');
      if (!countrySel || !bankSel) return;
      function loadBanks(countryCd, preserveBank) {
        loadBanksForCountry(countryCd, bankSel, preserveBank);
      }
      function toggleByCountry() {
        var v = countrySel.value;
        if (v === 'OTHER') {
          if (countryOtherWrap) countryOtherWrap.classList.remove('d-none');
          if (bankSelectWrap) bankSelectWrap.classList.add('d-none');
          if (bankTextWrap) bankTextWrap.classList.remove('d-none');
          bankSel.value = '';
        } else {
          if (countryOtherWrap) countryOtherWrap.classList.add('d-none');
          if (bankSelectWrap) bankSelectWrap.classList.remove('d-none');
          if (bankTextWrap) bankTextWrap.classList.add('d-none');
          if (bankCdText) bankCdText.value = '';
          if (v === 'JP' || v === 'KR' || v === 'TH') {
            loadBanks(v);
          } else {
            bankSel.innerHTML = '<option value="">국가 선택 후</option>';
          }
        }
      }
      countrySel.addEventListener('change', toggleByCountry);
      toggleByCountry();
      if (countrySel.value && countrySel.value !== 'OTHER') loadBanks(countrySel.value, bankSel.value);
    });
  }

  function pgDoubleConfirm(msg1, msg2) {
    return window.confirm(msg1) && window.confirm(msg2);
  }
  window.pgDoubleConfirm = pgDoubleConfirm;

  /**
   * 더블확인: 저장 시도 전 확인(확인/취소 한 번). 취소 시 false.
   */
  function pgConfirmBeforeSave(message) {
    return window.confirm(message || '저장하시겠습니까?\n취소하면 저장되지 않습니다.');
  }
  window.pgConfirmBeforeSave = pgConfirmBeforeSave;

  /** 가맹점 결제대행사 테이블. opts.rowActionMode=true 이면 업체정보(상세)에서 행별 저장/삭제·2중 확인 */
  function initPgBindingList(pane, initialBindings, opts) {
    opts = opts || {};
    var rowActionMode = !!opts.rowActionMode;
    var getCompId = opts.getCompId || function () { return ''; };

    var tbody = pane.querySelector('#pgBindingTbody');
    var addBtn = pane.querySelector('#pgBindingAddBtn');
    if (!tbody || !addBtn || addBtn._pgBindingInit) return;
    addBtn._pgBindingInit = true;

    var payMethodOpts = '<option value="">선택</option><option value="WEB">WEB</option><option value="OFFLINE">오프라인</option><option value="APM">APM</option>';
    var activationOpts = '<option value="Y">사용</option><option value="N">미사용</option>';
    var installmentOpts = '<option value="N">미사용</option><option value="Y">사용</option>';

    function rowSnapshot(tr) {
      var sel = function (f) {
        var e = tr.querySelector('[data-field="' + f + '"]');
        return e ? e.value : '';
      };
      var opInp = tr.querySelector('input[name="pgOperational"]');
      return {
        pgCd: sel('pgCd'),
        activationYn: sel('activationYn'),
        payMethod: sel('payMethod'),
        mid: sel('mid'),
        rootNo: sel('rootNo'),
        apiKey: sel('apiKey'),
        ivKey: sel('ivKey'),
        installmentYn: sel('installmentYn'),
        maxInstallmentMonths: sel('maxInstallmentMonths'),
        operationalChecked: opInp ? opInp.checked : false
      };
    }

    function applySnapshot(tr, snap) {
      if (!snap) return;
      var setSel = function (f, v) {
        var e = tr.querySelector('[data-field="' + f + '"]');
        if (e) e.value = v != null && v !== undefined ? String(v) : '';
      };
      setSel('pgCd', snap.pgCd);
      setSel('activationYn', snap.activationYn);
      setSel('payMethod', snap.payMethod);
      setSel('mid', snap.mid);
      setSel('rootNo', snap.rootNo);
      setSel('apiKey', snap.apiKey);
      setSel('ivKey', snap.ivKey);
      setSel('installmentYn', snap.installmentYn);
      setSel('maxInstallmentMonths', snap.maxInstallmentMonths);
      var opInp = tr.querySelector('input[name="pgOperational"]');
      if (opInp) opInp.checked = !!snap.operationalChecked;
    }

    function setRowReadonly(tr, ro) {
      tr.querySelectorAll('[data-field]').forEach(function (el) { el.disabled = ro; });
      tr.querySelectorAll('input[name="pgOperational"]').forEach(function (el) { el.disabled = ro; });
    }

    function toggleRowEditUi(tr, editing) {
      var editBtn = tr.querySelector('.pg-binding-edit');
      var delBtn = tr.querySelector('.pg-binding-del');
      var saveBtn = tr.querySelector('.pg-binding-save');
      var cancelBtn = tr.querySelector('.pg-binding-cancel');
      if (editBtn) editBtn.classList.toggle('d-none', editing);
      if (delBtn) delBtn.classList.toggle('d-none', editing);
      if (saveBtn) saveBtn.classList.toggle('d-none', !editing);
      if (cancelBtn) cancelBtn.classList.toggle('d-none', !editing);
    }

    function reindexRows() {
      tbody.querySelectorAll('tr').forEach(function (t, i) {
        t.dataset.idx = i;
        var r = t.querySelector('input[name="pgOperational"]');
        if (r) r.value = i;
      });
    }

    function wireRow(tr) {
      var delBtn = tr.querySelector('.pg-binding-del');
      var editBtn = tr.querySelector('.pg-binding-edit');
      var saveBtn = tr.querySelector('.pg-binding-save');
      var cancelBtn = tr.querySelector('.pg-binding-cancel');

      if (delBtn) {
        delBtn.addEventListener('click', function () {
          var bid = tr.dataset.bindingId || '';
          if (rowActionMode && bid) {
            if (!pgDoubleConfirm('이 결제대행사 연동을 삭제하시겠습니까?', '삭제하면 복구할 수 없습니다. 정말 삭제하시겠습니까?')) return;
            var compId = getCompId();
            if (!compId) { alert('업체코드가 없습니다.'); return; }
            var dimm = document.getElementById('dimm');
            if (dimm) dimm.style.display = 'flex';
            window.PG_API.compPgBindingDelete(compId, bid).then(function () {
              alert('삭제되었습니다.');
              tr.remove();
              reindexRows();
            }).catch(function (e) { alert(e && e.message ? e.message : '삭제 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
          } else {
            if (rowActionMode && !bid) {
              if (!pgDoubleConfirm('추가 중인 행을 취소하시겠습니까?', '입력 내용이 사라집니다. 계속하시겠습니까?')) return;
            }
            tr.remove();
            reindexRows();
          }
        });
      }

      if (editBtn) {
        editBtn.addEventListener('click', function () {
          if (!pgDoubleConfirm('이 연동 정보를 수정하시겠습니까?', '입력란이 활성화됩니다. 계속하시겠습니까?')) return;
          tr.dataset.editBackup = tr.dataset.snapshot;
          setRowReadonly(tr, false);
          toggleRowEditUi(tr, true);
        });
      }

      if (cancelBtn) {
        cancelBtn.addEventListener('click', function () {
          var bid = tr.dataset.bindingId || '';
          if (rowActionMode && !bid) {
            if (!pgDoubleConfirm('추가를 취소하시겠습니까?', '입력 내용이 버려집니다. 계속하시겠습니까?')) return;
            tr.remove();
            reindexRows();
            return;
          }
          if (!pgDoubleConfirm('수정을 취소하시겠습니까?', '저장되지 않은 변경이 사라집니다. 계속하시겠습니까?')) return;
          try { applySnapshot(tr, JSON.parse(tr.dataset.editBackup || tr.dataset.snapshot || '{}')); } catch (e) {}
          setRowReadonly(tr, true);
          toggleRowEditUi(tr, false);
        });
      }

      if (saveBtn && rowActionMode) {
        saveBtn.addEventListener('click', function () {
          if (!pgDoubleConfirm('이 결제대행사 연동을 저장하시겠습니까?', '저장을 진행합니다. 계속하시겠습니까?')) return;
          var compId = getCompId();
          if (!compId) { alert('업체코드가 없습니다.'); return; }
          var sel = function (f) {
            var e = tr.querySelector('[data-field="' + f + '"]');
            return e ? e.value : '';
          };
          var pgCd = sel('pgCd');
          if (!pgCd) { alert('결제대행사(PG)를 선택하세요. 본사설정 > PG사 API 연동에 먼저 등록해야 목록에 나타납니다.'); return; }
          var trs = Array.prototype.slice.call(tbody.querySelectorAll('tr'));
          var myIdx = trs.indexOf(tr);
          var formRoot = pane.querySelector('#compDetailForm') || pane.querySelector('#compRegForm');
          var opRadio = formRoot ? formRoot.querySelector('input[name="pgOperational"]:checked') : null;
          var opIdx = opRadio ? parseInt(opRadio.value, 10) : -1;
          var operationalYn = (myIdx === opIdx) ? 'Y' : 'N';
          var body = {
            compId: compId,
            pgCd: pgCd,
            payMethod: sel('payMethod') || 'WEB',
            mid: sel('mid'),
            rootNo: sel('rootNo'),
            apiKey: sel('apiKey'),
            ivKey: sel('ivKey'),
            activationYn: sel('activationYn') || 'Y',
            operationalYn: operationalYn,
            installmentYn: sel('installmentYn') || 'N',
            maxInstallmentMonths: sel('maxInstallmentMonths')
          };
          if (tr.dataset.bindingId) body.id = tr.dataset.bindingId;
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.compPgBindingSave(body).then(function (saved) {
            alert('저장되었습니다.');
            if (saved && saved.id != null) tr.dataset.bindingId = String(saved.id);
            tr.dataset.snapshot = JSON.stringify(rowSnapshot(tr));
            setRowReadonly(tr, true);
            toggleRowEditUi(tr, false);
            var actionsTd = tr.querySelector('.pg-binding-actions');
            if (actionsTd && tr.dataset.bindingId) {
              actionsTd.innerHTML = '<button type="button" class="btn btn-outline-primary btn-sm pg-binding-edit">수정</button> ' +
                '<button type="button" class="btn btn-outline-danger btn-sm pg-binding-del">삭제</button> ' +
                '<button type="button" class="btn btn-success btn-sm pg-binding-save d-none">저장</button> ' +
                '<button type="button" class="btn btn-secondary btn-sm pg-binding-cancel d-none">취소</button>';
              wireRow(tr);
            }
          }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        });
      }
    }

    function addRow(idx, data, pgAgencyOptsHtml) {
      data = data || {};
      var bid = data.id != null && data.id !== '' ? String(data.id) : '';
      var hasId = !!bid;
      var actionsCell;
      if (!rowActionMode) {
        actionsCell = '<button type="button" class="btn btn-outline-danger btn-sm pg-binding-del">삭제</button>';
      } else if (hasId) {
        actionsCell = '<button type="button" class="btn btn-outline-primary btn-sm pg-binding-edit">수정</button> ' +
          '<button type="button" class="btn btn-outline-danger btn-sm pg-binding-del">삭제</button> ' +
          '<button type="button" class="btn btn-success btn-sm pg-binding-save d-none">저장</button> ' +
          '<button type="button" class="btn btn-secondary btn-sm pg-binding-cancel d-none">취소</button>';
      } else {
        actionsCell = '<button type="button" class="btn btn-success btn-sm pg-binding-save">저장</button> ' +
          '<button type="button" class="btn btn-secondary btn-sm pg-binding-cancel">취소</button>';
      }

      var tr = document.createElement('tr');
      tr.dataset.bindingId = bid;
      tr.dataset.idx = idx;
      tr.innerHTML = '<td><input type="radio" name="pgOperational" value="' + idx + '"' + (data.operationalYn === 'Y' ? ' checked' : '') + ' title="운영대상"></td>' +
        '<td><select class="form-control form-control-sm" data-field="activationYn">' + activationOpts + '</select></td>' +
        '<td><select class="form-control form-control-sm" data-field="pgCd">' + pgAgencyOptsHtml + '</select></td>' +
        '<td><select class="form-control form-control-sm" data-field="payMethod">' + payMethodOpts + '</select></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="mid" placeholder="MID" autocomplete="off"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="rootNo" placeholder="루트(노티구분)" autocomplete="off"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="apiKey" placeholder="API KEY" autocomplete="off"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="ivKey" placeholder="IV KEY" autocomplete="off"></td>' +
        '<td><select class="form-control form-control-sm" data-field="installmentYn">' + installmentOpts + '</select></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="maxInstallmentMonths" placeholder="12" autocomplete="off"></td>' +
        '<td class="pg-binding-actions">' + actionsCell + '</td>';

      tr.querySelector('[data-field="activationYn"]').value = data.activationYn || 'Y';
      tr.querySelector('[data-field="pgCd"]').value = data.pgCd || '';
      tr.querySelector('[data-field="payMethod"]').value = data.payMethod || 'WEB';
      tr.querySelector('[data-field="mid"]').value = data.mid || '';
      tr.querySelector('[data-field="rootNo"]').value = data.rootNo || '';
      tr.querySelector('[data-field="apiKey"]').value = data.apiKey || '';
      tr.querySelector('[data-field="ivKey"]').value = data.ivKey || '';
      tr.querySelector('[data-field="installmentYn"]').value = data.installmentYn || 'N';
      tr.querySelector('[data-field="maxInstallmentMonths"]').value = data.maxInstallmentMonths != null ? String(data.maxInstallmentMonths) : '';

      tr.dataset.snapshot = JSON.stringify(rowSnapshot(tr));
      if (rowActionMode && hasId) setRowReadonly(tr, true);

      tbody.appendChild(tr);
      wireRow(tr);
    }

    window.PG_API.pgAgencyList().then(function (list) {
      var pgAgencyOptsHtml = '<option value="">선택</option>';
      (list || []).forEach(function (p) {
        pgAgencyOptsHtml += '<option value="' + (p.pgCd || '') + '">' + (p.pgNm || p.pgCd) + '</option>';
      });
      var bindings = initialBindings || [];
      if (bindings.length > 0) {
        bindings.forEach(function (b, i) {
          addRow(i, {
            id: b.id,
            pgCd: b.pgCd,
            activationYn: b.activationYn || 'Y',
            operationalYn: b.operationalYn || (i === 0 ? 'Y' : 'N'),
            payMethod: b.payMethod || 'WEB',
            mid: b.mid,
            rootNo: b.rootNo,
            apiKey: b.apiKey,
            ivKey: b.ivKey,
            installmentYn: b.installmentYn || 'N',
            maxInstallmentMonths: b.maxInstallmentMonths != null ? String(b.maxInstallmentMonths) : ''
          }, pgAgencyOptsHtml);
        });
      }
      addBtn.addEventListener('click', function () {
        var idx = tbody.querySelectorAll('tr').length;
        if (rowActionMode) {
          addRow(idx, { operationalYn: idx === 0 ? 'Y' : 'N' }, pgAgencyOptsHtml);
        } else {
          addRow(idx, idx === 0 ? { operationalYn: 'Y' } : {}, pgAgencyOptsHtml);
        }
        reindexRows();
      });
    }).catch(function () {
      var pgAgencyOptsHtml = '<option value="">선택</option>';
      addBtn.addEventListener('click', function () {
        var idx = tbody.querySelectorAll('tr').length;
        addRow(idx, idx === 0 ? { operationalYn: 'Y' } : {}, pgAgencyOptsHtml);
        reindexRows();
      });
    });
  }

  window.openPgAgencyModal = function (preset) {
    preset = preset || {};
    var idEl = document.getElementById('pgAgencyEditId');
    var cdEl = document.getElementById('pgAgencyEditPgCd');
    var nmEl = document.getElementById('pgAgencyEditPgNm');
    var epEl = document.getElementById('pgAgencyEditEndpoint');
    var uyEl = document.getElementById('pgAgencyEditUseYn');
    if (!idEl || !cdEl || !nmEl) return;
    idEl.value = preset.id != null ? String(preset.id) : '';
    cdEl.value = preset.pgCd || '';
    nmEl.value = preset.pgNm || '';
    if (epEl) epEl.value = preset.apiEndpoint || '';
    if (uyEl) uyEl.value = (preset.useYn === 'N') ? 'N' : 'Y';
    cdEl.readOnly = !!(preset.id);
    var el = document.getElementById('pgAgencyEditModal');
    if (el && window.bootstrap && bootstrap.Modal) {
      bootstrap.Modal.getOrCreateInstance(el).show();
    }
  };

  function initRegionalCardLimitTable(pane, initialData) {
    var tbody = pane.querySelector('#regionalCardLimitTbody');
    var addBtn = pane.querySelector('#regionalCardLimitAddBtn');
    var delBtn = pane.querySelector('#regionalCardLimitDelBtn');
    var emptyMsg = pane.querySelector('#regionalCardLimitEmpty');
    if (!tbody || !addBtn || addBtn._regionalCardLimitInit) return;
    addBtn._regionalCardLimitInit = true;
    function addRow(idx, data) {
      data = data || {};
      var tr = document.createElement('tr');
      tr.dataset.idx = idx;
      tr.innerHTML = '<td><input type="checkbox" class="regional-card-limit-row-check"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="payMethod" placeholder="결제구분" value="' + (data.payMethod || '') + '"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="cardIssuer" placeholder="카드사" value="' + (data.cardIssuer || '') + '"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="dayLimit" placeholder="일" value="' + (data.dayLimit || '') + '"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="timesLimit" placeholder="회" value="' + (data.timesLimit || '') + '"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="amtLimit" placeholder="원" value="' + (data.amtLimit || '') + '"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="regReason" placeholder="등록사유" value="' + (data.regReason || '') + '"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="regDt" readonly placeholder="등록일자" value="' + (data.regDt || '') + '"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="modDt" readonly placeholder="수정일자" value="' + (data.modDt || '') + '"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="remark" placeholder="비고" value="' + (data.remark || '') + '"></td>';
      tbody.appendChild(tr);
    }
    function updateEmpty() {
      if (emptyMsg) emptyMsg.style.display = tbody.querySelectorAll('tr').length ? 'none' : 'block';
    }
    (initialData || []).forEach(function (d, i) { addRow(i, d); });
    addBtn.addEventListener('click', function () {
      addRow(tbody.querySelectorAll('tr').length, {});
      updateEmpty();
    });
    if (delBtn) delBtn.addEventListener('click', function () {
      tbody.querySelectorAll('.regional-card-limit-row-check:checked').forEach(function (cb) { cb.closest('tr').remove(); });
      updateEmpty();
    });
    var checkAll = pane.querySelector('.regional-card-limit-check-all');
    if (checkAll) checkAll.addEventListener('change', function () {
      tbody.querySelectorAll('.regional-card-limit-row-check').forEach(function (cb) { cb.checked = checkAll.checked; });
    });
    updateEmpty();
  }

  function initRegionalTerminalTable(pane, initialData) {
    var tbody = pane.querySelector('#regionalTerminalTbody');
    var addBtn = pane.querySelector('#regionalTerminalAddBtn');
    var emptyMsg = pane.querySelector('#regionalTerminalEmpty');
    if (!tbody || !addBtn || addBtn._regionalTerminalInit) return;
    addBtn._regionalTerminalInit = true;
    function addRow(idx, data) {
      data = data || {};
      var tr = document.createElement('tr');
      tr.dataset.idx = idx;
      tr.innerHTML = '<td>' + (idx + 1) + '</td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="pgAgency" placeholder="결제대행사" value="' + (data.pgAgency || '') + '"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="terminalId" placeholder="터미널ID" value="' + (data.terminalId || '') + '"></td>' +
        '<td><input type="text" class="form-control form-control-sm" data-field="remark" placeholder="비고" value="' + (data.remark || '') + '"></td>';
      tbody.appendChild(tr);
    }
    function updateEmpty() {
      if (emptyMsg) emptyMsg.style.display = tbody.querySelectorAll('tr').length ? 'none' : 'block';
    }
    (initialData || []).forEach(function (d, i) { addRow(i, d); });
    addBtn.addEventListener('click', function () {
      var n = tbody.querySelectorAll('tr').length;
      addRow(n, {});
      tbody.querySelectorAll('tr').forEach(function (tr, i) { tr.querySelector('td:first-child').textContent = i + 1; });
      updateEmpty();
    });
    updateEmpty();
  }

  function initRegionalBusinessHolidayRanges(pane, initialData) {
    var tbody = pane.querySelector('#bizHolidayRangeTbody');
    var addBtn = pane.querySelector('#bizHolidayAddBtn');
    var fromEl = pane.querySelector('#bizHolidayFromDate');
    var toEl = pane.querySelector('#bizHolidayToDate');
    var reasonEl = pane.querySelector('#bizHolidayReason');
    var writerEl = pane.querySelector('#bizHolidayWriter');
    var jsonEl = pane.querySelector('#businessHolidayRangesJson');
    if (!tbody || !addBtn || addBtn._bizHolidayInit) return;
    addBtn._bizHolidayInit = true;

    function todayYmd() {
      var d = new Date();
      var m = String(d.getMonth() + 1).padStart(2, '0');
      var day = String(d.getDate()).padStart(2, '0');
      return d.getFullYear() + '-' + m + '-' + day;
    }
    function esc(s) {
      return String(s == null ? '' : s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
    }
    function readRows() {
      var rows = [];
      tbody.querySelectorAll('tr[data-row="1"]').forEach(function (tr) {
        rows.push({
          fromDate: tr.getAttribute('data-from') || '',
          toDate: tr.getAttribute('data-to') || '',
          reason: tr.getAttribute('data-reason') || '',
          addedDate: tr.getAttribute('data-added') || '',
          writer: tr.getAttribute('data-writer') || ''
        });
      });
      return rows;
    }
    function syncHidden() {
      if (jsonEl) jsonEl.value = JSON.stringify(readRows());
    }
    function clearEditor() {
      if (fromEl) fromEl.value = '';
      if (toEl) toEl.value = '';
      if (reasonEl) reasonEl.value = '';
      if (writerEl) writerEl.value = '';
      addBtn.textContent = '추가';
      delete addBtn.dataset.editingIdx;
    }
    function renderRows(rows) {
      tbody.innerHTML = '';
      if (!rows || rows.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-muted text-center">추가된 기간이 없습니다.</td></tr>';
        syncHidden();
        return;
      }
      rows.forEach(function (r, idx) {
        var tr = document.createElement('tr');
        tr.setAttribute('data-row', '1');
        tr.setAttribute('data-from', r.fromDate || '');
        tr.setAttribute('data-to', r.toDate || '');
        tr.setAttribute('data-reason', r.reason || '');
        tr.setAttribute('data-added', r.addedDate || todayYmd());
        tr.setAttribute('data-writer', r.writer || '');
        tr.innerHTML =
          '<td>' + esc(r.fromDate || '') + '</td>' +
          '<td>' + esc(r.toDate || '') + '</td>' +
          '<td>' + esc(r.reason || '') + '</td>' +
          '<td>' + esc(r.addedDate || todayYmd()) + '</td>' +
          '<td>' + esc(r.writer || '') + '</td>' +
          '<td>' +
          '<button type="button" class="btn btn-sm btn-outline-secondary me-1 biz-holiday-edit" data-idx="' + idx + '">수정</button>' +
          '<button type="button" class="btn btn-sm btn-success me-1 biz-holiday-confirm" data-idx="' + idx + '">확인</button>' +
          '<button type="button" class="btn btn-sm btn-outline-danger biz-holiday-delete" data-idx="' + idx + '">삭제</button>' +
          '</td>';
        tbody.appendChild(tr);
      });
      syncHidden();
    }
    function normalizeInitialRows(data) {
      var rows = [];
      (data || []).forEach(function (r) {
        var from = r.fromDate || r.from || r.date || '';
        var to = r.toDate || r.to || r.date || from;
        if (!from) return;
        rows.push({
          fromDate: from,
          toDate: to || from,
          reason: r.reason || r.content || '',
          addedDate: r.addedDate || r.regDt || todayYmd(),
          writer: r.writer || r.createdBy || ''
        });
      });
      return rows;
    }
    renderRows(normalizeInitialRows(initialData));

    addBtn.addEventListener('click', function () {
      var from = fromEl && fromEl.value ? fromEl.value : '';
      var to = toEl && toEl.value ? toEl.value : '';
      var reason = reasonEl && reasonEl.value ? reasonEl.value.trim() : '';
      var writer = writerEl && writerEl.value ? writerEl.value.trim() : '';
      if (!from) { alert('언제부터 날짜를 입력하세요.'); return; }
      if (!to) { alert('언제까지 날짜를 입력하세요.'); return; }
      if (from > to) { alert('시작일은 종료일보다 클 수 없습니다.'); return; }
      var rows = readRows();
      var editingIdx = addBtn.dataset.editingIdx;
      var newRow = { fromDate: from, toDate: to, reason: reason, addedDate: todayYmd(), writer: writer };
      if (editingIdx != null && editingIdx !== '') {
        var old = rows[Number(editingIdx)] || {};
        newRow.addedDate = old.addedDate || todayYmd();
        rows[Number(editingIdx)] = newRow;
      } else {
        rows.push(newRow);
      }
      renderRows(rows);
      clearEditor();
    });

    tbody.addEventListener('click', function (e) {
      var editBtn = e.target.closest('.biz-holiday-edit');
      var confirmBtn = e.target.closest('.biz-holiday-confirm');
      var deleteBtn = e.target.closest('.biz-holiday-delete');
      if (!editBtn && !confirmBtn && !deleteBtn) return;
      var idx = Number((editBtn || confirmBtn || deleteBtn).getAttribute('data-idx'));
      var rows = readRows();
      if (!rows[idx]) return;
      if (editBtn) {
        if (fromEl) fromEl.value = rows[idx].fromDate || '';
        if (toEl) toEl.value = rows[idx].toDate || '';
        if (reasonEl) reasonEl.value = rows[idx].reason || '';
        if (writerEl) writerEl.value = rows[idx].writer || '';
        addBtn.dataset.editingIdx = String(idx);
        addBtn.textContent = '수정확인';
        return;
      }
      if (confirmBtn) {
        alert('확인 완료: ' + (rows[idx].fromDate || '') + ' ~ ' + (rows[idx].toDate || ''));
        return;
      }
      if (deleteBtn) {
        if (!confirm('해당 영업일 기간을 삭제하시겠습니까?')) return;
        rows.splice(idx, 1);
        renderRows(rows);
        clearEditor();
      }
    });
  }

  function initRegionalHolidayProfileSelector(pane, form, data) {
    if (!pane || !form) return;
    var profileSel = form.querySelector('[name="holidayProfileName"]');
    if (!profileSel || profileSel._holidayProfileInit) return;
    profileSel._holidayProfileInit = true;
    var countryView = form.querySelector('[name="holidayProfileCountry"]');
    var hiddenCountryCode = form.querySelector('[name="holidayCountryCode"]');
    var hiddenCountryCodes = form.querySelector('[name="holidayCountryCodes"]');
    var hiddenDates = form.querySelector('[name="businessHolidayExtraDates"]');
    var hiddenRanges = form.querySelector('[name="businessHolidayRangesJson"]');
    var lockHint = form.querySelector('.holiday-profile-lock-hint');
    if (!lockHint && profileSel && profileSel.parentElement) {
      lockHint = document.createElement('small');
      lockHint.className = 'holiday-profile-lock-hint text-danger d-none';
      lockHint.textContent = '총본사에서 상위 본사 영업일을 지정하여 이 총판의 영업일 설정은 상속 고정됩니다.';
      profileSel.parentElement.appendChild(lockHint);
    }
    function applyLockState() {
      var locked = (data && String(data.holidayLockedByHeadquartersYn || '').toUpperCase() === 'Y');
      if (profileSel) profileSel.disabled = !!locked;
      if (lockHint) lockHint.classList.toggle('d-none', !locked);
    }
    function applySelectedProfile() {
      var name = profileSel.value || '';
      var list = pane._hqBizdayProfiles || [];
      var profile = list.find(function (x) { return (x.name || '') === name; }) || null;
      if (!profile) {
        if (countryView) countryView.value = '';
        if (hiddenCountryCode) hiddenCountryCode.value = '';
        if (hiddenCountryCodes) hiddenCountryCodes.value = '';
        if (hiddenDates) hiddenDates.value = '';
        if (hiddenRanges) hiddenRanges.value = '[]';
        return;
      }
      var cc = profile.countryCode || 'KR';
      if (countryView) countryView.value = cc;
      if (hiddenCountryCode) hiddenCountryCode.value = cc;
      if (hiddenCountryCodes) hiddenCountryCodes.value = cc;
      if (hiddenDates) hiddenDates.value = profile.businessHolidayExtraDates || '';
      if (hiddenRanges) {
        var rows = String(profile.businessHolidayExtraDates || '').split(/\r?\n/)
          .map(function (d) { return d.trim(); })
          .filter(function (d) { return !!d; })
          .map(function (d) { return { fromDate: d, toDate: d, reason: '', addedDate: '', writer: '' }; });
        hiddenRanges.value = JSON.stringify(rows);
      }
    }
    window.PG_API.hqBusinessDaySettings().then(function (list) {
      pane._hqBizdayProfiles = Array.isArray(list) ? list : [];
      var opts = '<option value="">선택</option>';
      pane._hqBizdayProfiles.forEach(function (it) {
        opts += '<option value="' + (it.name || '') + '">' + (it.name || '') + '</option>';
      });
      profileSel.innerHTML = opts;
      var initName = (data && data.holidayProfileName) ? data.holidayProfileName : (profileSel.value || '');
      if (initName) profileSel.value = initName;
      applySelectedProfile();
      applyLockState();
    }).catch(function () {
      profileSel.innerHTML = '<option value="">선택</option>';
      applySelectedProfile();
      applyLockState();
    });
    profileSel.addEventListener('change', applySelectedProfile);
  }

  function addTabAndSwitch(url, menuId, label) {
    var tabId = getTabIdFromUrl(url);
    var ul = document.getElementById(TAB_UL);
    if (!ul) return;

    var existing = ul.querySelector('[top_tab_url="' + url + '"]');
    if (existing) {
      existing.querySelector('.tab-a').click();
      return;
    }

    if (ul.querySelectorAll('.nav-item.copyTopTab').length >= MAX_TOP_TABS) {
      alert('메뉴는 최대 12탭 입니다. 추가 시 처음 탭이 자동 삭제됩니다.');
    }

    while (ul.querySelectorAll('.nav-item.copyTopTab').length >= MAX_TOP_TABS) {
      var firstNonMain = ul.querySelector('.nav-item.copyTopTab:not([top_tab_url="/main"])');
      if (!firstNonMain) break;
      var removedUrl = firstNonMain.getAttribute('top_tab_url');
      var removedTabId = getTabIdFromUrl(removedUrl);
      if (firstNonMain.classList.contains('active')) {
        var mainA = ul.querySelector('.copyTopTab[top_tab_url="/main"] .tab-a');
        if (mainA) mainA.click();
      }
      firstNonMain.remove();
      var pane = document.getElementById(removedTabId);
      if (pane && pane.parentNode) pane.parentNode.removeChild(pane);
    }

    var li = document.createElement('li');
    li.className = 'nav-item copyTopTab';
    li.setAttribute('top_tab_url', url);
    li.innerHTML = '<a href="#' + tabId + '" data-toggle="tab" class="nav-link tab-a" menu_id="' + (menuId || '') + '">' + (label || tabId) + '</a>' +
      '<button type="button" class="tab-close-button" tab_id="' + tabId + '">×</button>';
    ul.appendChild(li);

    ul.querySelectorAll('.tab-a').forEach(function (x) { x.classList.remove('active'); });
    li.querySelector('.tab-a').classList.add('active');

    setActiveMenuByUrl(url);
    loadContent(url, menuId, label);

    li.querySelector('.tab-close-button').addEventListener('click', function () {
      li.remove();
      var firstTab = ul.querySelector('.copyTopTab .tab-a');
      if (firstTab) firstTab.click();
    });
  }

  function setActiveMenuByUrl(url) {
    document.querySelectorAll('.side-nav .child-li').forEach(function (el) {
      el.classList.remove('mm-active');
      if (el.getAttribute('data-url') === url) el.classList.add('mm-active');
    });
    document.querySelectorAll('.side-nav-item').forEach(function (el) { el.classList.remove('mm-active'); });
    var activeChild = document.querySelector('.side-nav .child-li.mm-active');
    if (activeChild) {
      var parent = activeChild.closest('.side-nav-item');
      if (parent) {
        parent.classList.add('mm-active');
        var sub = parent.querySelector('.side-nav-second-level');
        if (sub) { sub.classList.add('mm-show'); parent.querySelector('.side-nav-link').setAttribute('aria-expanded', 'true'); }
      }
    }
    var menuIdEl = document.getElementById('_menuId');
    var active = document.querySelector('.side-nav .child-li.mm-active a');
    if (menuIdEl && active) menuIdEl.value = active.getAttribute('data-menu_id') || '';
  }

  function escapeCsvField(val) {
    var s = val == null ? '' : String(val);
    if (/[",\n\r]/.test(s)) return '"' + s.replace(/"/g, '""') + '"';
    return s;
  }

  function downloadTextAsFile(filename, text, mime) {
    var blob = new Blob([text], { type: mime || 'text/csv;charset=utf-8;' });
    var a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(a.href);
  }

  /** 그리드 엑셀다운로드 — 서버에서 헤더색·가운데정렬·테두리·텍스트열 서식 적용 xlsx */
  function downloadGridExcelCsv(pane) {
    var list = pane._lastGridList;
    var cols = pane._lastGridCols;
    if (!cols || !cols.length || list === undefined) {
      alert('다운로드할 데이터가 없습니다. [검색]으로 조회한 후 다시 시도하세요.');
      return;
    }
    var dataCols = cols.filter(function (c) { return c.type !== 'checkbox'; });
    if (!dataCols.length) {
      alert('보낼 컬럼이 없습니다.');
      return;
    }
    var url = pane.getAttribute('formurl') || '';
    var menu = (MENU_INFO[url] && MENU_INFO[url].label) ? MENU_INFO[url].label : '목록';
    var sheetName = menu.length > 31 ? menu.substring(0, 31) : menu;
    var headers = dataCols.map(function (c) { return String(c.label || c.key); });
    var rows = list.map(function (row) {
      return dataCols.map(function (c) {
        var v = row[c.key];
        if (v === undefined || v === null) return '';
        return String(v);
      });
    });
    var textKeys = {
      compId: 1, regNo: 1, contact: 1, accountNo: 1, zipCode: 1, loginId: 1,
      ceoMobile: 1, compTel: 1, bankNm: 1, rowNo: 1, terminalCountTerminal: 1, terminalCountWeb: 1,
      transferType: 1, calcProcType: 1, calcCycle: 1, calcExcludeYn: 1, payHoldYn: 1, useYn: 1, compDivNm: 1
    };
    var textColumnIndexes = [];
    for (var ti = 0; ti < dataCols.length; ti++) {
      if (textKeys[dataCols[ti].key]) textColumnIndexes.push(ti);
    }
    var d = new Date();
    var ymd = d.getFullYear() + String(d.getMonth() + 1).padStart(2, '0') + String(d.getDate()).padStart(2, '0');
    var dimm = document.getElementById('dimm');
    if (dimm) dimm.style.display = 'flex';
    window.PG_API.exportStyledExcel({
      sheetName: sheetName,
      headers: headers,
      rows: rows,
      textColumnIndexes: textColumnIndexes
    }).then(function (blob) {
      var a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = menu + '_' + ymd + '.xlsx';
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(a.href);
    }).catch(function (e) {
      alert(e && e.message ? e.message : '엑셀 다운로드에 실패했습니다.');
    }).finally(function () {
      if (dimm) dimm.style.display = 'none';
    });
  }

  /** 업체 엑셀등록용 SAMPLE — 서버 생성 xlsx (헤더 색·표선·가운데 정렬·계좌번호 텍스트) */
  function downloadCompExcelSampleFile() {
    var dimm = document.getElementById('dimm');
    if (dimm) dimm.style.display = 'flex';
    window.PG_API.compExcelSample().then(function (blob) {
      var d = new Date();
      var ymd = d.getFullYear() + String(d.getMonth() + 1).padStart(2, '0') + String(d.getDate()).padStart(2, '0');
      var a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = '업체등록_SAMPLE_' + ymd + '.xlsx';
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(a.href);
    }).catch(function (e) {
      alert(e && e.message ? e.message : '샘플 다운로드에 실패했습니다.');
    }).finally(function () {
      if (dimm) dimm.style.display = 'none';
    });
  }

  function bindScreenEvents(pane, tabId) {
    if (!pane) return;
    var initFormUrl = pane.getAttribute('formurl') || '';
    if (initFormUrl === '/comp/compMngTree' && window.PG_SCREENS && typeof window.PG_SCREENS.getCompMngSearchCompDivOptions === 'function') {
      var selCompDiv = pane.querySelector('select[name="searchCompDiv"]');
      if (selCompDiv) {
        var uSearch = {};
        try { uSearch = JSON.parse(sessionStorage.getItem('pg_admin_user') || '{}'); } catch (e1) { uSearch = {}; }
        var isAdminSearch = String(uSearch.role || '').toUpperCase() === 'ADMIN';
        var divOpts = window.PG_SCREENS.getCompMngSearchCompDivOptions(uSearch.orgLevel, isAdminSearch);
        var prevDiv = selCompDiv.value;
        var escOpt = function (s) { return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;'); };
        selCompDiv.innerHTML = divOpts.map(function (o) { return '<option value="' + escOpt(o.v) + '">' + escOpt(o.t) + '</option>'; }).join('');
        var keepPrev = false;
        for (var di = 0; di < selCompDiv.options.length; di++) {
          if (selCompDiv.options[di].value === prevDiv) { keepPrev = true; break; }
        }
        selCompDiv.value = keepPrev ? prevDiv : '';
      }
    }
    if (pane.classList) {
      pane.classList.toggle('screen-distribution-list', initFormUrl === '/calc/calcList' || initFormUrl === '/settlement/distributionList');
    }
    function syncColumnGuideUiState() {
      pane.querySelectorAll('.column-guide-item').forEach(function (item) {
        var cb = item.querySelector('.column-guide-check');
        var on = !!(cb && cb.checked);
        item.classList.toggle('column-guide-item--on', on);
        item.classList.toggle('column-guide-item--off', !on);
      });
    }
    function getSelectedGuideKeys() {
      var keys = [];
      pane.querySelectorAll('.column-guide-check:checked').forEach(function (cb) {
        var k = cb.getAttribute('data-key');
        if (k) keys.push(k);
      });
      return keys;
    }
    function applySelectedGuideKeys(keys) {
      var set = {};
      (keys || []).forEach(function (k) { set[k] = 1; });
      var hasAny = (keys || []).length > 0;
      pane.querySelectorAll('.column-guide-check').forEach(function (cb) {
        var k = cb.getAttribute('data-key') || '';
        cb.checked = hasAny ? !!set[k] : false;
      });
      pane._selectedColumns = hasAny ? (keys || []) : null;
      syncColumnGuideUiState();
    }
    function loadViewSetting() {
      var hasGuide = !!pane.querySelector('.column-guide-check');
      pane._columnAllowanceRestricted = false;
      pane._allowedColumnKeys = null;
      if (!hasGuide || !window.PG_API || !window.PG_API.userViewSetting) return Promise.resolve();
      return window.PG_API.userViewSetting(url).then(function (data) {
        var restricted = data && data.columnAllowanceRestricted === true;
        var allowedRaw = data && data.allowedKeysJson != null ? data.allowedKeysJson : null;
        var allowedList = null;
        if (restricted && allowedRaw != null) {
          try { allowedList = JSON.parse(String(allowedRaw)); } catch (e1) { allowedList = []; }
          if (!Array.isArray(allowedList)) allowedList = [];
        }
        pane._columnAllowanceRestricted = !!restricted;
        pane._allowedColumnKeys = (restricted && allowedList) ? allowedList : null;

        if (pane._columnAllowanceRestricted && pane._allowedColumnKeys) {
          pane.querySelectorAll('.column-guide-check').forEach(function (cb) {
            var k = cb.getAttribute('data-key') || '';
            var ok = pane._allowedColumnKeys.indexOf(k) !== -1;
            var item = cb.closest('.column-guide-item');
            if (item) item.style.display = ok ? '' : 'none';
          });
        } else {
          pane.querySelectorAll('.column-guide-item').forEach(function (item) { item.style.display = ''; });
        }

        if (!data || data.hasSetting !== true) {
          if (pane._columnAllowanceRestricted && pane._allowedColumnKeys && pane._allowedColumnKeys.length) {
            applySelectedGuideKeys(pane._allowedColumnKeys.slice());
          } else {
            pane._selectedColumns = null;
            pane.querySelectorAll('.column-guide-check').forEach(function (cb) { cb.checked = false; });
            syncColumnGuideUiState();
          }
          return;
        }
        var json = data && data.selectedKeysJson ? String(data.selectedKeysJson) : '[]';
        var keys = [];
        try { keys = JSON.parse(json); } catch (e2) { keys = []; }
        if (!Array.isArray(keys)) keys = [];
        applySelectedGuideKeys(keys);
      }).catch(function () {
        syncColumnGuideUiState();
      });
    }
    if (!pane._pgGridCheckboxDelegated) {
      pane._pgGridCheckboxDelegated = true;
      pane.addEventListener('change', function (e) {
        var t = e.target;
        if (!t || !t.classList) return;
        if (t.classList.contains('grid-check-all')) {
          var table = t.closest('table');
          if (!table) return;
          var on = !!t.checked;
          table.querySelectorAll('tbody .grid-row-check').forEach(function (cb) { cb.checked = on; });
          t.indeterminate = false;
          return;
        }
        if (t.classList.contains('grid-row-check')) {
          var table2 = t.closest('table');
          if (!table2) return;
          var all = table2.querySelectorAll('tbody .grid-row-check');
          var n = all.length;
          var checked = table2.querySelectorAll('tbody .grid-row-check:checked').length;
          var master = table2.querySelector('thead .grid-check-all');
          if (master && n) {
            master.checked = checked === n;
            master.indeterminate = checked > 0 && checked < n;
          }
        }
      });
    }
    if (!pane._pgExcelDownloadDelegated) {
      pane._pgExcelDownloadDelegated = true;
      pane.addEventListener('click', function (e) {
        var btn = e.target && e.target.closest ? e.target.closest('#excelBtn, #excelDownBtn') : null;
        if (!btn) return;
        e.preventDefault();
        downloadGridExcelCsv(pane);
      });
    }
    if (!pane._pgPrintDelegated) {
      pane._pgPrintDelegated = true;
      pane.addEventListener('click', function (e) {
        var pb = e.target && e.target.closest ? e.target.closest('#printBtn') : null;
        if (!pb) return;
        e.preventDefault();
        try { window.print(); } catch (err) {}
      });
    }
    var today = new Date();
    function fmt(d) { return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0'); }
    pane.querySelectorAll('.quick-date').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var range = this.getAttribute('data-range');
        var from = new Date(today);
        var to = new Date(today);
        if (range === 'day' || range === 'month') { from.setHours(0, 0, 0, 0); to.setHours(23, 59, 59, 999); }
        if (range === 'prevDay') { from.setDate(from.getDate() - 1); to.setTime(from.getTime()); from.setHours(0, 0, 0, 0); to.setHours(23, 59, 59, 999); }
        if (range === 'prevMonth') { from.setMonth(from.getMonth() - 1); from.setDate(1); to = new Date(from.getFullYear(), from.getMonth() + 1, 0); }
        if (range === 'month') { from.setDate(1); to = new Date(from.getFullYear(), from.getMonth() + 1, 0); }
        if (range === 'week') {
          from.setDate(from.getDate() - 6);
          from.setHours(0, 0, 0, 0);
          to.setHours(23, 59, 59, 999);
        }
        if (range === 'week2') {
          from.setDate(from.getDate() - 13);
          from.setHours(0, 0, 0, 0);
          to.setHours(23, 59, 59, 999);
        }
        /** 금주: 이번 주 월~일, 전주: 직전 주 월~일 */
        if (range === 'weekCal') {
          var wd = today.getDay();
          var toMon = wd === 0 ? -6 : 1 - wd;
          from = new Date(today);
          from.setDate(from.getDate() + toMon);
          from.setHours(0, 0, 0, 0);
          to = new Date(from);
          to.setDate(to.getDate() + 6);
          to.setHours(23, 59, 59, 999);
        }
        if (range === 'prevWeekCal') {
          var wd2 = today.getDay();
          var toMon2 = wd2 === 0 ? -6 : 1 - wd2;
          from = new Date(today);
          from.setDate(from.getDate() + toMon2 - 7);
          from.setHours(0, 0, 0, 0);
          to = new Date(from);
          to.setDate(to.getDate() + 6);
          to.setHours(23, 59, 59, 999);
        }
        var fromEl = pane.querySelector('#searchFromDate');
        var toEl = pane.querySelector('#searchToDate');
        if (fromEl) fromEl.value = fmt(from);
        if (toEl) toEl.value = fmt(to);
      });
    });
    function collectSearchParams(p) {
      var params = { page: 1, size: 20 };
      var sizeEl = p.querySelector('#recordsPerPage');
      if (sizeEl) params.size = Math.max(1, parseInt(sizeEl.value, 10) || 20);
      p.querySelectorAll('input, select').forEach(function (el) {
        var name = el.name || el.id;
        if (!name) return;
        if (el.type === 'checkbox') {
          if (!el.classList.contains('grid-check-all') && name.indexOf('search') === 0) params[name] = el.checked ? 'true' : 'false';
          return;
        }
        var v = el.value;
        if (v === undefined || v === null) return;
        if (name.indexOf('search') === 0) params[name] = v;
      });
      var pageEl = p.querySelector('#pageCnt');
      if (pageEl) params.page = parseInt(pageEl.value, 10) || 1;
      return params;
    }
    function applyTreeVisibility(pane, tbody, list) {
      var expanded = pane._treeExpanded;
      if (!expanded) return;
      var idSet = {};
      for (var h = 0; h < list.length; h++) {
        var hid = list[h].id != null ? String(list[h].id) : '';
        if (hid) idSet[hid] = true;
      }
      var visible = {};
      for (var i = 0; i < list.length; i++) {
        var row = list[i];
        var id = row.id != null ? String(row.id) : '';
        var pid = row.parentId != null ? String(row.parentId) : '';
        var parentInList = pid && idSet[pid];
        var isVisible = !parentInList ? true : (visible[pid] && (expanded.has ? expanded.has(pid) : pid in expanded));
        visible[id] = isVisible;
      }
      var rows = tbody.querySelectorAll('tr[data-id]');
      for (var j = 0; j < rows.length; j++) {
        var tr = rows[j];
        var rid = tr.getAttribute('data-id') || '';
        tr.style.display = visible[rid] !== false ? '' : 'none';
      }
      tbody.querySelectorAll('.table-row-resize-handle').forEach(function (handle) {
        var next = handle.nextElementSibling;
        handle.style.display = (next && next.style.display === 'none') ? 'none' : '';
      });
      tbody.querySelectorAll('.tree-toggle.expanded, .tree-toggle.collapsed').forEach(function (span) {
        var sid = span.getAttribute('data-id') || '';
        var isExp = expanded.has ? expanded.has(sid) : sid in expanded;
        span.className = 'tree-toggle ' + (isExp ? 'expanded' : 'collapsed');
        span.textContent = isExp ? '\u25BC' : '\u25B6';
        span.title = isExp ? '접기' : '펼치기';
      });
    }
    function buildGroupedTheadFromConfig(groups, cols) {
      var keyToGroup = {};
      (groups || []).forEach(function (g, gi) {
        (g.keys || []).forEach(function (k) { keyToGroup[k] = gi; });
      });
      var groupColCount = (groups || []).map(function () { return 0; });
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
          top += '<th data-key="_chk" rowspan="2" style="width:40px"><input type="checkbox" class="grid-check-all" title="전체선택"></th>';
          return;
        }
        var gi = keyToGroup[c.key];
        if (gi === undefined) {
          top += '<th data-key="' + (c.key || '') + '" rowspan="2">' + (c.label || c.key) + '</th>';
        } else {
          if (!startedGroups[gi] && groupColCount[gi] > 0) {
            startedGroups[gi] = true;
            top += '<th colspan="' + groupColCount[gi] + '">' + (groups[gi].label || '') + '</th>';
          }
          sub += '<th data-key="' + (c.key || '') + '">' + (c.label || c.key) + '</th>';
        }
      });
      return '<tr>' + top + '</tr><tr>' + sub + '</tr>';
    }
    function loadCommissionHistoryGrid(pane, tid) {
      var cfg = window.PG_SCREENS && window.PG_SCREENS.getMenuScreens && window.PG_SCREENS.getMenuScreens()['/commission/commisionList'];
      if (!cfg || !cfg.commissionHistory) return;
      var hcfg = cfg.commissionHistory;
      var table = pane.querySelector('#grid_commissionHist_' + tid);
      if (!table) return;
      var thead = table.querySelector('thead');
      var tbody = table.querySelector('tbody');
      var groups = hcfg.headerGroups || [];
      var cols = hcfg.columns || [];
      if (thead) thead.innerHTML = buildGroupedTheadFromConfig(groups, cols);
      var compId = pane._commissionHistCompId || '';
      var subEl = pane.querySelector('#commissionHistSubtitle_' + tid);
      if (subEl) subEl.textContent = compId ? ('표시 중: ' + compId + ' (최근 변경이 No.1)') : '목록에서 가맹점 행을 클릭하면 해당 업체의 변경 이력이 표시됩니다.';
      var thLen = Math.max(cols.length, 8);
      if (!compId || !window.PG_API || !window.PG_API.commissionHistory) {
        if (tbody) tbody.innerHTML = '<tr><td colspan="' + thLen + '" class="text-center text-muted py-3">업체를 선택하세요.</td></tr>';
        return;
      }
      window.PG_API.commissionHistory(compId, { page: 1, size: 200 }).then(function (data) {
        var list = data && data.list ? data.list : [];
        if (!tbody) return;
        if (list.length === 0) {
          tbody.innerHTML = '<tr><td colspan="' + thLen + '" class="text-center text-muted py-3">변경 이력이 없습니다.</td></tr>';
          return;
        }
        var rateKeys = ['hqRate', 'regionalRate', 'masterRate', 'branchRate', 'agencyRate', 'salesOfficeRate', 'totalRate', 'hqPerTxFee', 'regionalPerTxFee', 'masterPerTxFee', 'branchPerTxFee', 'agencyPerTxFee', 'salesOfficePerTxFee', 'totalPerTxFee'];
        var html = '';
        list.forEach(function (row) {
          html += '<tr>';
          cols.forEach(function (c) {
            if (c.type === 'checkbox') {
              html += '<td></td>';
              return;
            }
            var val = row[c.key] !== undefined && row[c.key] !== null ? String(row[c.key]) : '';
            var cls = rateKeys.indexOf(c.key) >= 0 ? ' class="text-end"' : '';
            html += '<td' + cls + '>' + val + '</td>';
          });
          html += '</tr>';
        });
        tbody.innerHTML = html;
      }).catch(function () {
        if (tbody) tbody.innerHTML = '<tr><td colspan="' + thLen + '" class="text-center text-danger py-3">히스토리 조회 실패</td></tr>';
      });
    }
    function doSearch(p, tid, pageOverride) {
      var url = p.getAttribute('formurl') || '';
      var api = window.PG_API;
      if (!url || url === '/main') return;
      if (p && p.classList) {
        p.classList.toggle('screen-calc-gm-list', url === '/calc/calcGmList' || url === '/settlement/franchiseList');
        p.classList.toggle('screen-pay-list', url === '/calc/payList' || url === '/calc/payNotiList' || url === '/calc/paySuccessList' || url === '/calc/payFailList' || url === '/calc/payRefundList' || url === '/calc/payForceRefundList' || url === '/calc/payCancelList' || url === '/calc/offsetCancList' || url === '/pay/easyPay' || url === '/pay/chatbotPay');
        p.classList.toggle('screen-distribution-list', url === '/calc/calcList' || url === '/settlement/distributionList');
        p.classList.toggle('screen-user-mng', url === '/user/userMng');
      }
      var cfg = window.PG_SCREENS && window.PG_SCREENS.getMenuScreens && window.PG_SCREENS.getMenuScreens()[url];
      /* 조직별 권한 세팅: 그리드 조건/캐시와 무관하게 URL로만 매트릭스 로드 */
      if (url === '/hq/permissionMng') {
        var dimmPerm = document.getElementById('dimm');
        if (dimmPerm) dimmPerm.style.display = 'flex';
        if (!api || !api.hqPermissionMng) {
          if (dimmPerm) dimmPerm.style.display = 'none';
          return;
        }
        var permTimeoutMs = 25000;
        var permTimeout = new Promise(function (_, reject) {
          setTimeout(function () {
            reject(new Error('응답 시간이 초과되었습니다. PG START로 서버를 재시작한 뒤 다시 시도하세요.'));
          }, permTimeoutMs);
        });
        Promise.race([api.hqPermissionMng({}), permTimeout]).then(function (data) {
          if (window.initOrgPagePermissionMatrix) window.initOrgPagePermissionMatrix(p, tid, data);
        }).catch(function (err) {
          var msg = (err && err.message) ? err.message : '권한 설정을 불러오지 못했습니다.';
          alert(msg);
          var tbErr = p.querySelector('#orgPermTbody_' + tid);
          if (tbErr) {
            var safe = String(msg).replace(/</g, '&lt;').replace(/&/g, '&amp;');
            tbErr.innerHTML = '<tr><td colspan="4" class="text-center text-danger py-4">' + safe + '</td></tr>';
          }
        }).finally(function () {
          if (dimmPerm) dimmPerm.style.display = 'none';
        });
        return;
      }
      if (!cfg || ((!cfg.columns || cfg.columns.length === 0) && !cfg.columnsBySub && !cfg.columnsRegionalPayout && !cfg.orgPagePermissionMatrix)) return;
      var params = collectSearchParams(p);
      if (pageOverride) params.page = pageOverride;
      if ((url === '/calc/settlementReport' || url === '/settlement/settlementReport') && !params.searchReportSub) {
        params.searchReportSub = 'AGG';
      }
      if ((url === '/calc/settlementReport' || url === '/settlement/settlementReport') && !params.searchReportKind) {
        params.searchReportKind = 'MERCHANT_STMT';
      }
      if (cfg.payListVariant) params.payListVariant = cfg.payListVariant;
      p.setAttribute('data-last-url', url);
      p.setAttribute('data-last-page', String(params.page));
      var dimm = document.getElementById('dimm');
      if (dimm) dimm.style.display = 'flex';
      var promise = null;
      if (url === '/system/noticeList') promise = api.noticeList(params);
      else if (url === '/calc/payList' || url === '/calc/payFailList' || url === '/calc/offsetCancList' || url === '/pay/easyPay' || url === '/pay/chatbotPay' || url === '/calc/payNotiList' || url === '/calc/paySuccessList' || url === '/calc/payRefundList' || url === '/calc/payForceRefundList' || url === '/calc/payCancelList') promise = api.payList(params);
      else if (url === '/comp/compMngTree' || url === '/comp/myCompMng' || url === '/comp/compMng' || url === '/comp/compInfo') {
        if (url === '/comp/myCompMng') params.myOrgOnly = true;
        promise = api.compList(params);
      }
      else if (url === '/comp/compInfoHistList' || url === '/comp/compChangeHistory') promise = api.compChangeHistory(params);
      else if (url === '/commission/commisionList') promise = api.commissionList(params);
      else if (url === '/user/userMng') promise = api.userList(params);
      else if (url === '/set/gridSetMng' || url === '/user/menuOrderMng') promise = api.menuOrderMng(params);
      else if (url === '/calc/calcList' || url === '/settlement/distributionList') promise = api.settlementDistributionList(params);
      else if (url === '/calc/calcGmList' || url === '/settlement/franchiseList') promise = api.settlementFranchiseList(params);
      else if (url === '/calc/feeList' || url === '/settlement/feeList') promise = api.settlementFeeList(params);
      else if (url === '/calc/compPointMngList' || url === '/settlement/recallMng') promise = api.settlementRecallMng(params);
      else if (url === '/calc/balcInfo' || url === '/settlement/balanceMng') promise = api.settlementBalanceMng(params);
      else if (url === '/calc/balanceList' || url === '/settlement/balanceList') promise = api.settlementBalanceList(params);
      else if (url === '/calc/unpaidMng' || url === '/settlement/unpaidMng') promise = api.settlementUnpaidMng(params);
      else if (url === '/pay/payHoldList' || url === '/settlement/holdList') promise = api.settlementHoldList(params);
      else if (url === '/calc/collateralList' || url === '/settlement/collateralList') promise = api.settlementCollateralList(params);
      else if (url === '/calc/exCalcList' || url === '/settlement/execute') promise = api.settlementExecute(params);
      else if (url === '/calc/settlementReport' || url === '/settlement/settlementReport') {
        var sub = params.searchReportSub || 'AGG';
        if (sub === 'EXE') promise = api.settlementReportExecute(params);
        else if (sub === 'SUM') promise = api.settlementReportSummary(params);
        else promise = api.settlementReportAggregate(params);
      }
      else if (url === '/noti/notiUrlMng' || url === '/notify/payUrlMng') promise = api.notifyPayUrlMng(params);
      else if (url === '/noti/notiSendMngList' || url === '/notify/paySendMng') promise = api.notifyPaySendMng(params);
      else if (url === '/noti/notiCashReceiptUrlMng' || url === '/notify/cashReceiptUrlMng') promise = api.notifyCashReceiptUrlMng(params);
      else if (url === '/noti/notiCashReceiptSendMngList' || url === '/notify/cashReceiptSendMng') promise = api.notifyCashReceiptSendMng(params);
      else if (url === '/hq/pgApiMng') promise = api.hqPgApiMng(params);
      else if (url === '/hq/permissionMng') promise = api.hqPermissionMng(params);
      else if (url === '/hq/accountMng') promise = api.hqAccountAccessList(params);
      else if (url === '/risk/list') promise = Promise.resolve({ list: [], totalElements: 0, totalPages: 1, page: params.page || 1, size: params.size || 20 });
      if (!promise) {
        if (dimm) dimm.style.display = 'none';
        p._lastGridList = [];
        p._lastGridCols = null;
        var tbody = p.querySelector('#grid_' + tid + ' tbody');
        var rk0 = params.searchReportKind || 'MERCHANT_STMT';
        var emptyCols = (cfg.columns && cfg.columns.length) ? cfg.columns.length
          : (rk0 === 'REGIONAL_PAYOUT' && cfg.columnsRegionalPayout && cfg.columnsRegionalPayout.AGG ? cfg.columnsRegionalPayout.AGG.length
            : (cfg.columnsBySub && cfg.columnsBySub.AGG ? cfg.columnsBySub.AGG.length : 8));
        if (tbody) tbody.innerHTML = '<tr><td colspan="' + emptyCols + '" class="empty-state-cell text-center text-muted">조회된 데이터가 없습니다.</td></tr>';
        var cntEl = p.querySelector('#summary_건수, .summary-count, [data-summary="건수"]');
        if (cntEl) cntEl.textContent = (cntEl.id === 'summary_건수' ? '건수: ' : '') + '0';
        return;
      }
      promise.then(function (data) {
        var list = data && data.list ? data.list : [];
        if (url === '/user/userMng' && p._userMngDraftRows && p._userMngDraftRows.length) {
          list = p._userMngDraftRows.concat(list);
        }
        list = applyObserverCompTreeScope(list, url);
        var total = data && data.totalElements !== undefined ? data.totalElements : list.length;
        var totalPages = data && data.totalPages !== undefined ? data.totalPages : 1;
        var repKind = params.searchReportKind || 'MERCHANT_STMT';
        var sub = params.searchReportSub || 'AGG';
        var allCols;
        if (repKind === 'REGIONAL_PAYOUT' && cfg.columnsRegionalPayout && cfg.columnsRegionalPayout[sub]) {
          allCols = cfg.columnsRegionalPayout[sub];
        } else {
          allCols = (cfg.columnsBySub && cfg.columnsBySub[sub])
            ? cfg.columnsBySub[sub]
            : (cfg.columns || []);
        }
        var selCols = p._selectedColumns;
        var fixedKeys = ['rowNo', 'compNm', 'compId', 'trnDate', 'trnTime', 'routeNo', 'chillTransactionId', 'compDivNm', 'merchantNm'];
        var restrictCols = p._columnAllowanceRestricted === true;
        var allowKeys = p._allowedColumnKeys;
        var cols = allCols.filter(function (c) {
          if (c.type === 'checkbox' || fixedKeys.indexOf(c.key) !== -1) return true;
          if (restrictCols) {
            if (!allowKeys || allowKeys.length === 0) return false;
            if (allowKeys.indexOf(c.key) === -1) return false;
          }
          if (!selCols || selCols.length === 0) return true;
          return selCols.indexOf(c.key) !== -1;
        });
        p._lastGridList = list;
        p._lastGridCols = cols;
        var thead = p.querySelector('#grid_' + tid + ' thead');
        if (thead && cfg.distributionThreeRowHeader && window.PG_SCREENS && typeof window.PG_SCREENS.buildDistributionListTheadHtml === 'function') {
          thead.innerHTML = window.PG_SCREENS.buildDistributionListTheadHtml();
        } else if (thead) {
          var groups = cfg.headerGroups || [];
          if (!groups.length) {
            var theadTr = thead.querySelector('tr') || document.createElement('tr');
            theadTr.innerHTML = cols.map(function (c) {
              if (c.type === 'checkbox') return '<th data-key="_chk" style="width:40px"><input type="checkbox" class="grid-check-all" title="전체선택"></th>';
              return '<th data-key="' + (c.key || '') + '">' + (c.label || c.key) + '</th>';
            }).join('');
            if (!theadTr.parentNode) thead.appendChild(theadTr);
          } else {
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
                top += '<th data-key="_chk" rowspan="2" style="width:40px"><input type="checkbox" class="grid-check-all" title="전체선택"></th>';
                return;
              }
              var gi = keyToGroup[c.key];
              if (gi === undefined) {
                top += '<th data-key="' + (c.key || '') + '" rowspan="2">' + (c.label || c.key) + '</th>';
              } else {
                if (!startedGroups[gi] && groupColCount[gi] > 0) {
                  startedGroups[gi] = true;
                  top += '<th colspan="' + groupColCount[gi] + '">' + (groups[gi].label || '') + '</th>';
                }
                sub += '<th data-key="' + (c.key || '') + '">' + (c.label || c.key) + '</th>';
              }
            });
            thead.innerHTML = '<tr>' + top + '</tr><tr>' + sub + '</tr>';
          }
        }
        var thLen = cols.length;
        var tbody = p.querySelector('#grid_' + tid + ' tbody');
        if (!tbody) {
          if (dimm) dimm.style.display = 'none';
          return;
        }
        if (list.length === 0) {
          tbody.innerHTML = '<tr><td colspan="' + (thLen || (cfg.columns && cfg.columns.length)) + '" class="empty-state-cell text-center text-muted py-4">조회된 데이터가 없습니다.</td></tr>';
        } else {
          var html = '';
          var isCompMngTree = (url === '/comp/compMngTree');
          var hasChildrenMap = {};
          if (isCompMngTree) {
            list.forEach(function (r) {
              var pid = r.parentId != null ? String(r.parentId) : null;
              if (pid) hasChildrenMap[pid] = true;
            });
            var SetOrFallback = window.Set || function () { var o = {}; return { add: function (k) { o[k] = 1; }, has: function (k) { return k in o; }, delete: function (k) { delete o[k]; } }; };
            p._treeExpanded = new SetOrFallback();
            list.forEach(function (r) {
              var id = r.id != null ? String(r.id) : '';
              if (id && hasChildrenMap[id]) p._treeExpanded.add(id);
            });
          }
          var groupKey = cfg.rowGroupByKey;
          var prevGroup = null;
          var pageNum = data && data.page != null ? parseInt(data.page, 10) : (params.page || 1);
          if (isNaN(pageNum) || pageNum < 1) pageNum = 1;
          var pageSize = data && data.size != null ? parseInt(data.size, 10) : 20;
          if (isNaN(pageSize) || pageSize < 1) pageSize = 20;
          var rowNoBase = (pageNum - 1) * pageSize;
          list.forEach(function (row, idx) {
            var escAttr = function (s) {
              return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
            };
            var rowId = row.id != null ? String(row.id) : '';
            var parentId = row.parentId != null ? String(row.parentId) : '';
            var hasChildren = isCompMngTree && rowId && hasChildrenMap[rowId];
            var trExtraClass = '';
            if (groupKey) {
              var gk = row[groupKey] !== undefined && row[groupKey] !== null ? String(row[groupKey]) : '';
              if (gk !== prevGroup) {
                trExtraClass = ' tr-user-group-start';
                prevGroup = gk;
              }
            }
            var dataCompAttr = (url === '/commission/commisionList' && row.compId) ? (' data-comp-id="' + String(row.compId).replace(/"/g, '&quot;') + '"') : '';
            var draftAttr = (url === '/user/userMng' && row._draft) ? (' data-draft="1" data-temp-id="' + escAttr(row._tempId) + '"') : '';
            html += '<tr' + (trExtraClass ? ' class="' + trExtraClass.trim() + '"' : '') + ' data-row-idx="' + idx + '" data-id="' + (rowId || '') + '" data-parent-id="' + (parentId || '') + '" data-row="' + (isCompMngTree ? encodeURIComponent(JSON.stringify(row)) : '') + '"' + dataCompAttr + draftAttr + '>';
            cols.forEach(function (c) {
              if (c.type === 'checkbox') html += '<td><input type="checkbox" class="grid-row-check"></td>';
              else if (c.type === 'payActions') {
                var seq = row.paySeq != null ? String(row.paySeq) : '';
                var esc = function (s) {
                  return String(s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
                };
                var es = esc(seq);
                html += '<td class="small text-nowrap pay-actions-cell">' +
                  '<button type="button" class="btn btn-link btn-sm p-0 pay-follow" data-act="AUTO_VOID" data-trn="' + es + '">자동무효</button> ' +
                  '<button type="button" class="btn btn-link btn-sm p-0 pay-follow" data-act="EMAIL_VOID" data-trn="' + es + '">이메일무효</button> ' +
                  '<button type="button" class="btn btn-link btn-sm p-0 pay-follow" data-act="AUTO_REFUND" data-trn="' + es + '">자동환불</button> ' +
                  '<button type="button" class="btn btn-link btn-sm p-0 pay-follow" data-act="FORCE_REFUND" data-trn="' + es + '">강제환불</button></td>';
              } else if (c.type === 'accountAccessDelete') {
                var delId = row.id != null ? String(row.id) : '';
                html += '<td><button type="button" class="btn btn-link btn-sm p-0 text-danger hq-acc-del" data-id="' + delId + '">삭제</button></td>';
              } else if (url === '/user/userMng' && c.type && String(c.type).indexOf('userMng') === 0) {
                if (c.type === 'userMngUserId') {
                  if (row._draft) {
                    html += '<td><input type="text" class="form-control form-control-sm user-mng-inp" data-field="userId" value="' + escAttr(row.userId || '') + '" autocomplete="off" /></td>';
                  } else {
                    html += '<td>' + escAttr(row.userId || '') + '</td>';
                  }
                } else if (c.type === 'userMngUserNm') {
                  if (row._draft) {
                    html += '<td><input type="text" class="form-control form-control-sm user-mng-inp" data-field="userNm" value="' + escAttr(row.userNm || '') + '" /></td>';
                  } else {
                    html += '<td>' + escAttr(row.userNm || '') + '</td>';
                  }
                } else if (c.type === 'userMngMobile') {
                  html += '<td><input type="text" class="form-control form-control-sm user-mng-inp" data-field="mobile" value="' + escAttr(row.mobile || '') + '" /></td>';
                } else if (c.type === 'userMngAssistantRole') {
                  var ar = row.assistantRoleType || 'MANAGER';
                  var arOpts = ['MANAGER', 'OPERATOR', 'SETTLEMENT', 'TECH'];
                  html += '<td><select class="form-select form-select-sm user-mng-sel" data-field="assistantRoleType">';
                  arOpts.forEach(function (r) {
                    html += '<option value="' + r + '"' + (ar === r ? ' selected' : '') + '>' + r + '</option>';
                  });
                  html += '</select></td>';
                } else if (c.type === 'userMngRoleNm') {
                  html += '<td>' + escAttr(row.roleNm || '') + '</td>';
                } else if (c.type === 'userMngPassword') {
                  var pwdRid = row.id != null ? String(row.id) : '';
                  var canResetPwd = String(row.canResetPassword || 'N') === 'Y';
                  if (row._draft) {
                    html += '<td><input type="password" class="form-control form-control-sm user-mng-pwd" data-field="password" placeholder="8자 이상" autocomplete="new-password" /></td>';
                  } else {
                    html += '<td class="text-nowrap user-mng-pwd-cell"><div class="dropdown d-inline-block">' +
                      '<button type="button" class="btn btn-sm btn-outline-secondary dropdown-toggle user-mng-pwd-menu-toggle py-0 px-2" data-bs-toggle="dropdown" data-bs-auto-close="true" aria-expanded="false">비밀번호</button>' +
                      '<ul class="dropdown-menu dropdown-menu-end user-mng-pwd-dropdown shadow-sm">';
                    if (canResetPwd) {
                      html += '<li><button type="button" class="dropdown-item py-1 small user-reset-pwd-btn" data-id="' + pwdRid + '">초기화</button></li>';
                    } else {
                      html += '<li><span class="dropdown-item disabled py-1 small text-muted mb-0">초기화 권한 없음</span></li>';
                    }
                    html += '</ul></div></td>';
                  }
                } else if (c.type === 'userMngOtp') {
                  var oid = row.id != null ? String(row.id) : '';
                  var canResetO = String(row.canResetPassword || 'N') === 'Y';
                  var oy = String(row.otpRegisteredYn || 'N') === 'Y';
                  html += '<td class="text-nowrap"><span class="badge rounded-pill user-mng-otp-badge ' + (oy ? 'bg-success' : 'bg-secondary') + '">' + (oy ? '등록' : '미등록') + '</span>';
                  if (!row._draft && canResetO) {
                    html += ' <button type="button" class="btn btn-link btn-sm p-0 text-info user-otp-reset-btn" data-id="' + oid + '">초기화</button>';
                  }
                  html += '</td>';
                } else if (c.type === 'userMngStatus') {
                  var us = row.userStatus || 'ACTIVE';
                  html += '<td><select class="form-select form-select-sm user-mng-sel" data-field="userStatus">';
                  html += '<option value="ACTIVE"' + (us === 'ACTIVE' ? ' selected' : '') + '>사용</option>';
                  html += '<option value="INACTIVE"' + (us === 'INACTIVE' ? ' selected' : '') + '>미사용</option>';
                  html += '<option value="SUSPENDED"' + (us === 'SUSPENDED' ? ' selected' : '') + '>영구정지</option>';
                  html += '</select></td>';
                } else if (c.type === 'userMngDraftDelete') {
                  if (row._draft) {
                    html += '<td><button type="button" class="btn btn-sm btn-outline-danger user-mng-draft-remove" data-temp-id="' + escAttr(row._tempId) + '">삭제</button></td>';
                  } else {
                    html += '<td></td>';
                  }
                } else if (c.type === 'userMngInactiveReason') {
                  html += '<td><input type="text" class="form-control form-control-sm user-mng-inp" data-field="inactiveReason" value="' + escAttr(row.inactiveReason || '') + '" /></td>';
                } else {
                  html += '<td></td>';
                }
              } else if (c.type === 'userResetPassword') {
                var resetId = row.id != null ? String(row.id) : '';
                var canReset = String(row.canResetPassword || 'N') === 'Y';
                html += '<td>' + (canReset
                  ? ('<button type="button" class="btn btn-link btn-sm p-0 text-info user-reset-pwd-btn" data-id="' + resetId + '">초기화</button>')
                  : '<span class="text-muted">-</span>') + '</td>';
              } else if (c.type === 'userDelete') {
                var userDelId = row.id != null ? String(row.id) : '';
                var canManageUsers = String(row.canManageUsers || 'N') === 'Y';
                html += '<td>' + (canManageUsers
                  ? ('<button type="button" class="btn btn-link btn-sm p-0 text-danger user-del-btn" data-id="' + userDelId + '">삭제</button>')
                  : '<span class="text-muted">-</span>') + '</td>';
              } else if (c.type === 'commissionInlineActions' && url === '/commission/commisionList') {
                html += '<td class="text-nowrap"><button type="button" class="btn btn-sm btn-primary commission-inline-save me-1">저장</button><button type="button" class="btn btn-sm btn-outline-danger commission-inline-clear">삭제</button></td>';
              } else {
                var val = row[c.key] !== undefined && row[c.key] !== null ? String(row[c.key]) : '';
                var cellClass = '';
                var isPayScr = url === '/calc/payList' || url === '/calc/payNotiList' || url === '/calc/paySuccessList' || url === '/calc/payFailList' || url === '/calc/payRefundList' || url === '/calc/payForceRefundList' || url === '/calc/payCancelList' || url === '/calc/offsetCancList' || url === '/pay/easyPay' || url === '/pay/chatbotPay';
                if (url === '/calc/calcGmList' || url === '/settlement/franchiseList') {
                  var gmCls = [];
                  if (['amount', 'feeCnt', 'feeRate', 'feeAmt', 'feeVat', 'holdRate', 'holdAmt', 'settleAmt'].indexOf(c.key) >= 0) gmCls.push('text-end');
                  if (['calcDt', 'approveDt', 'cancelDt'].indexOf(c.key) >= 0) gmCls.push('text-nowrap');
                  if (['compNm', 'merchantNm'].indexOf(c.key) >= 0) gmCls.push('text-start');
                  if (gmCls.length) cellClass = ' class="' + gmCls.join(' ') + '"';
                } else if (url === '/commission/commisionList') {
                  var commCls = [];
                  if (['hqRate', 'regionalRate', 'masterRate', 'branchRate', 'agencyRate', 'salesOfficeRate', 'totalRate', 'hqPerTxFee', 'regionalPerTxFee', 'masterPerTxFee', 'branchPerTxFee', 'agencyPerTxFee', 'salesOfficePerTxFee', 'totalPerTxFee'].indexOf(c.key) >= 0) commCls.push('text-end');
                  if (commCls.length) cellClass = ' class="' + commCls.join(' ') + '"';
                } else if (isPayScr) {
                  var payCls = [];
                  if (['pgApproveAmt', 'payAmount', 'feeCnt', 'feeRate', 'feeAmt', 'feeVat', 'holdRate', 'holdAmt', 'settleAmt', 'chillAmount', 'icopayAmt', 'chillFeeAmt', 'totalAmt'].indexOf(c.key) >= 0) payCls.push('text-end');
                  if (['payAprv', 'holdDttm', 'calcDt', 'payDttm', 'trnDate', 'trnTime', 'payCompletedAt', 'trnId', 'chillTransactionId', 'routeNo'].indexOf(c.key) >= 0) payCls.push('text-nowrap');
                  if (['compNm', 'merchantNm', 'compDivCode9', 'chillCustomer', 'productNm'].indexOf(c.key) >= 0) payCls.push('text-start');
                  if (payCls.length) cellClass = ' class="' + payCls.join(' ') + '"';
                } else if (url === '/calc/calcList' || url === '/settlement/distributionList') {
                  var distCls = [];
                  if (['aprvCnt', 'aprvAmt', 'aprvFeeCnt', 'aprvFeePct', 'aprvFeeSum', 'aprvFeeVat', 'canCnt', 'canAmt', 'canFeeCnt', 'canFeePct', 'canFeeSum', 'canFeeVat', 'settleAmt'].indexOf(c.key) >= 0) distCls.push('text-end');
                  if (['settleMonth', 'orgDivNm', 'regionalNm', 'masterNm', 'branchNm', 'agencyNm', 'compId'].indexOf(c.key) >= 0) distCls.push('text-nowrap');
                  if (distCls.length) cellClass = ' class="' + distCls.join(' ') + '"';
                } else if (url === '/calc/collateralList' || url === '/settlement/collateralList') {
                  var collCls = [];
                  if (['reserveAmt', 'remainingBizDays', 'holdBusinessDays'].indexOf(c.key) >= 0) collCls.push('text-end');
                  if (collCls.length) cellClass = ' class="' + collCls.join(' ') + '"';
                } else if (url === '/calc/feeList' || url === '/settlement/feeList') {
                  var feeCls = [];
                  if (['amount', 'perTxFee', 'usageFee', 'failFee', 'cancelFee', 'voidFee', 'manualVoidFee', 'refundFee', 'payFeeRate', 'payFee', 'usdtFeeRate', 'usdtFee', 'fxFeeRate', 'fxFee', 'fee3dsRate', 'fee3dsFee', 'settlementPerTxFee', 'chargebackFee', 'extraFees', 'totalFee', 'feeVat'].indexOf(c.key) >= 0) feeCls.push('text-end');
                  if (feeCls.length) cellClass = ' class="' + feeCls.join(' ') + '"';
                } else if (url === '/commission/commisionList') {
                  var cmCls = [];
                  if (['hqRate', 'regionalRate', 'masterRate', 'branchRate', 'agencyRate', 'salesOfficeRate', 'hqPerTxFee', 'regionalPerTxFee', 'masterPerTxFee', 'branchPerTxFee', 'agencyPerTxFee', 'salesOfficePerTxFee', 'applyDt'].indexOf(c.key) >= 0) cmCls.push('text-end');
                  if (cmCls.length) cellClass = ' class="' + cmCls.join(' ') + '"';
                }
                if (isCompMngTree && c.key === 'rowNo') {
                  html += '<td' + cellClass + '>' + (val || '') + '</td>';
                } else if (c.key === 'rowNo') {
                  html += '<td' + cellClass + '>' + (rowNoBase + idx + 1) + '</td>';
                } else if (c.key === 'otpRegisteredYn') {
                  var oy = val === 'Y';
                  html += '<td' + cellClass + '><span class="badge rounded-pill ' + (oy ? 'bg-success' : 'bg-secondary') + '">' + (oy ? '등록' : '미등록') + '</span></td>';
                } else if (isCompMngTree && c.key === 'compId') {
                  var depth = (row.depth != null && row.depth !== '') ? parseInt(row.depth, 10) : 0;
                  if (isNaN(depth)) depth = 0;
                  var px = 18 + depth * 18;
                  var expanded = p._treeExpanded && p._treeExpanded.has(rowId);
                  var folderSvg = '<svg class="tree-icon tree-icon-folder" width="16" height="16" viewBox="0 0 24 24"><path fill="currentColor" d="M10 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2h-8l-2-2z"/></svg>';
                  var docSvg = '<svg class="tree-icon tree-icon-doc" width="16" height="16" viewBox="0 0 24 24"><path fill="currentColor" d="M14 2H6c-1.1 0-2 .9-2 2v16c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V8l-6-6z"/></svg>';
                  var icon = hasChildren ? folderSvg : docSvg;
                  var toggle = hasChildren ? '<span class="tree-toggle ' + (expanded ? 'expanded' : 'collapsed') + '" data-id="' + rowId + '" title="' + (expanded ? '접기' : '펼치기') + '">' + (expanded ? '\u25BC' : '\u25B6') + '</span>' : '<span class="tree-toggle-placeholder"></span>';
                  html += '<td class="tree-comp-cell" style="padding-left:' + px + 'px">' + icon + toggle + (val || '') + '</td>';
                } else if (isCompMngTree && c.key === 'compNm') {
                  html += '<td class="text-start align-middle">' + (val || '') + '</td>';
                } else if (url === '/comp/compMng' && c.key === 'compNm') {
                  html += '<td class="text-start align-middle">' + (val || '') + '</td>';
                } else if (url === '/calc/calcList' || url === '/settlement/distributionList') {
                  var distFmtKeys = ['aprvCnt', 'aprvAmt', 'aprvFeeCnt', 'aprvFeeSum', 'aprvFeeVat', 'canCnt', 'canAmt', 'canFeeCnt', 'canFeeSum', 'canFeeVat', 'settleAmt'];
                  var distPctKeys = ['aprvFeePct', 'canFeePct'];
                  var showVal = val;
                  if (distFmtKeys.indexOf(c.key) >= 0) showVal = fmtNum(row[c.key]);
                  else if (distPctKeys.indexOf(c.key) >= 0) showVal = val;
                  html += '<td' + cellClass + '>' + showVal + '</td>';
                } else if (url === '/calc/collateralList' || url === '/settlement/collateralList') {
                  var collShow = val;
                  if (c.key === 'reserveAmt') collShow = fmtNum(row[c.key]);
                  html += '<td' + cellClass + '>' + collShow + '</td>';
                } else if (url === '/calc/feeList' || url === '/settlement/feeList') {
                  var feeShow = val;
                  if (['amount', 'perTxFee', 'usageFee', 'failFee', 'cancelFee', 'voidFee', 'manualVoidFee', 'refundFee', 'payFee', 'settlementPerTxFee', 'usdtFee', 'fxFee', 'fee3dsFee', 'chargebackFee', 'extraFees', 'totalFee', 'feeVat'].indexOf(c.key) >= 0) {
                    feeShow = fmtNum(row[c.key]);
                  } else if (['payFeeRate', 'usdtFeeRate', 'fxFeeRate', 'fee3dsRate'].indexOf(c.key) >= 0) {
                    feeShow = (val != null && val !== '') ? String(val) : '0';
                  }
                  html += '<td' + cellClass + '>' + feeShow + '</td>';
                } else if (url === '/commission/commisionList') {
                  var cEditable = ['hqRate', 'regionalRate', 'masterRate', 'branchRate', 'agencyRate', 'salesOfficeRate', 'hqPerTxFee', 'regionalPerTxFee', 'masterPerTxFee', 'branchPerTxFee', 'agencyPerTxFee', 'salesOfficePerTxFee', 'applyDt'];
                  if (cEditable.indexOf(c.key) >= 0) {
                    var cType = c.key === 'applyDt' ? 'date' : 'text';
                    var cVal = c.key === 'applyDt' ? (val ? String(val).substring(0, 10) : '') : (val || '0');
                    html += '<td><input type="' + cType + '" class="form-control form-control-sm commission-inline-input text-end" data-key="' + c.key + '" value="' + String(cVal).replace(/"/g, '&quot;') + '"></td>';
                  } else {
                    html += '<td' + cellClass + '>' + val + '</td>';
                  }
                } else html += '<td' + cellClass + '>' + val + '</td>';
              }
            });
            html += '</tr>';
          });
          tbody.innerHTML = html;
          if (url !== '/user/userMng') {
            injectTableRowResizeHandles(tbody, thLen);
          }
          if (isCompMngTree && list.length) {
            p._treeList = list;
            applyTreeVisibility(p, tbody, list);
          }
        }
        var cntEl = p.querySelector('#summary_건수, .summary-count, [data-summary="건수"]');
        if (cntEl) cntEl.textContent = (cntEl.id === 'summary_건수' ? '건수: ' : '') + total;
        if (url === '/calc/calcGmList' || url === '/settlement/franchiseList') {
          var sum = { amount: 0, feeAmt: 0, feeVat: 0, holdAmt: 0, settleAmt: 0 };
          list.forEach(function (r) {
            sum.amount += asNum(r.amount);
            sum.feeAmt += asNum(r.feeAmt);
            sum.feeVat += asNum(r.feeVat);
            sum.holdAmt += asNum(r.holdAmt);
            sum.settleAmt += asNum(r.settleAmt);
          });
          setSummaryText(p, '금액', fmtNum(sum.amount));
          setSummaryText(p, '수수료금액', fmtNum(sum.feeAmt));
          setSummaryText(p, '수수료부가세', fmtNum(sum.feeVat));
          setSummaryText(p, '보류금액', fmtNum(sum.holdAmt));
          setSummaryText(p, '정산금액', fmtNum(sum.settleAmt));
        }
        if (url === '/calc/calcList' || url === '/settlement/distributionList') {
          var distSum = { settleAmt: 0, feeSum: 0 };
          list.forEach(function (r) {
            distSum.settleAmt += asNum(r.settleAmt);
            distSum.feeSum += asNum(r.hqFee) + asNum(r.regionalFee) + asNum(r.masterFee) + asNum(r.branchFee) + asNum(r.agencyFee);
          });
          setSummaryText(p, 'Total', String(total));
          setSummaryText(p, '정산금액', fmtNum(distSum.settleAmt));
          setSummaryText(p, '수수료', fmtNum(distSum.feeSum));
          setSummaryText(p, '지급액', fmtNum(distSum.settleAmt));
        }
        if (url === '/calc/feeList' || url === '/settlement/feeList') {
          var feeSum = { totalFee: 0, vat: 0 };
          list.forEach(function (r) {
            feeSum.totalFee += asNum(r.totalFee);
            feeSum.vat += asNum(r.feeVat);
          });
          setSummaryText(p, '총수수료', fmtNum(feeSum.totalFee));
          setSummaryText(p, '부가세', fmtNum(feeSum.vat));
        }
        if (url === '/calc/balcInfo' || url === '/settlement/balanceMng') {
          var bs = { bal: 0, unpaid: 0, deducted: 0, remain: 0 };
          list.forEach(function (r) {
            bs.bal += asNum(r.balcAmount);
            bs.unpaid += asNum(r.unpaidAmount);
            bs.deducted += asNum(r.deductedAmount);
            bs.remain += asNum(r.remainAmount);
          });
          setSummaryText(p, '잔액합계', fmtNum(bs.bal));
          setSummaryText(p, '미수금합계', fmtNum(bs.unpaid));
          setSummaryText(p, '차감합계', fmtNum(bs.deducted));
          setSummaryText(p, '가용잔액합계', fmtNum(bs.remain));
        }
        if (url === '/calc/compPointMngList' || url === '/settlement/recallMng') {
          var rc = { recall: 0 };
          list.forEach(function (r) { rc.recall += asNum(r.recallAmt); });
          setSummaryText(p, '환수금액', fmtNum(rc.recall));
        }
        if (url === '/calc/settlementReport' || url === '/settlement/settlementReport') {
          var repSub = params.searchReportSub || 'AGG';
          if (repSub === 'SUM' && list.length) {
            var r0 = list[0];
            setSummaryText(p, '건수', '1');
            setSummaryText(p, '결제액', fmtNum(r0.grossPay));
            setSummaryText(p, '환불', fmtNum(r0.refundAmt));
            setSummaryText(p, '순액', fmtNum(r0.netPay));
            setSummaryText(p, '정산금', fmtNum(r0.settlementAmt));
          } else {
            var tr = { gross: 0, ref: 0, net: 0, st: 0 };
            list.forEach(function (r) {
              if (repSub === 'EXE') {
                tr.gross += asNum(r.approveAmt);
                tr.ref += asNum(r.cancelAmt);
                tr.net += asNum(r.netPay);
                tr.st += asNum(r.payAmount);
              } else {
                tr.gross += asNum(r.grossPay);
                tr.ref += asNum(r.refundAmt);
                tr.net += asNum(r.netPay);
                tr.st += asNum(r.settlementAmt);
              }
            });
            setSummaryText(p, '건수', String(total));
            setSummaryText(p, '결제액', fmtNum(tr.gross));
            setSummaryText(p, '환불', fmtNum(tr.ref));
            setSummaryText(p, '순액', fmtNum(tr.net));
            setSummaryText(p, '정산금', fmtNum(tr.st));
          }
        }
        if (url === '/calc/payList' || url === '/calc/payNotiList' || url === '/calc/paySuccessList' || url === '/calc/payFailList' || url === '/calc/payRefundList' || url === '/calc/payForceRefundList' || url === '/calc/payCancelList' || url === '/calc/offsetCancList' || url === '/pay/easyPay' || url === '/pay/chatbotPay') {
          var ps = { aprv: 0, canc: 0, fee: 0, vat: 0, pay: 0, hold: 0 };
          list.forEach(function (r) {
            var amt = asNum(r.pgApproveAmt != null ? r.pgApproveAmt : r.payAmount);
            var div = String(r.payDivNm || '');
            if (div === '결제') ps.aprv += amt;
            else if (div === '취소') ps.canc += amt;
            ps.fee += asNum(r.feeAmt);
            ps.vat += asNum(r.feeVat);
            ps.pay += asNum(r.settleAmt);
            ps.hold += asNum(r.holdAmt);
          });
          setSummaryText(p, '승인금액', fmtNum(ps.aprv));
          setSummaryText(p, '취소금액', fmtNum(ps.canc));
          setSummaryText(p, '결제금액', fmtNum(ps.aprv - ps.canc));
          setSummaryText(p, '총수수료', fmtNum(ps.fee + ps.vat));
          setSummaryText(p, '보류금액', fmtNum(ps.hold));
          setSummaryText(p, '지급액', fmtNum(ps.pay));
        }
        if (url === '/calc/collateralList' || url === '/settlement/collateralList') {
          var coll = { holdAmt: 0 };
          list.forEach(function (r) {
            if (String(r.status || '').toUpperCase() === 'HOLD') coll.holdAmt += asNum(r.reserveAmt);
          });
          setSummaryText(p, '담보금액', fmtNum(coll.holdAmt));
        }
        if (window.updatePaging) window.updatePaging(tid, params.page, totalPages, total);
        p.setAttribute('data-last-total-pages', String(totalPages));
        if (url === '/commission/commisionList') {
          p.querySelectorAll('#grid_' + tid + ' tbody tr.table-active').forEach(function (x) { x.classList.remove('table-active'); });
          p._commissionHistCompId = list.length ? list[0].compId : '';
          var firstTr = tbody ? tbody.querySelector('tr[data-comp-id]') : null;
          if (firstTr) firstTr.classList.add('table-active');
          loadCommissionHistoryGrid(p, tid);
        }
        if (url === '/comp/compMngTree') {
          var returnCompId = '';
          try { returnCompId = sessionStorage.getItem('pg_comp_detail_return_compId') || ''; } catch (e) {}
          if (returnCompId && tbody) {
            try { sessionStorage.removeItem('pg_comp_detail_return_compId'); } catch (e) {}
            var targetTr = null;
            var rows = tbody.querySelectorAll('tr[data-row]');
            for (var r = 0; r < rows.length; r++) {
              var dr = rows[r].getAttribute('data-row');
              if (dr) {
                try {
                  var rowData = JSON.parse(decodeURIComponent(dr));
                  if (rowData.compId === returnCompId) {
                    targetTr = rows[r];
                    break;
                  }
                } catch (e) {}
              }
            }
            if (targetTr) {
              var chk = targetTr.querySelector('.grid-row-check');
              if (chk) chk.checked = true;
              targetTr.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
              targetTr.classList.add('table-warning');
              setTimeout(function () { if (targetTr) targetTr.classList.remove('table-warning'); }, 2000);
            }
          }
        }
      }).catch(function (err) {
        p._lastGridList = [];
        p._lastGridCols = null;
        var tbody = p.querySelector('#grid_' + tid + ' tbody');
        var rkErr = params.searchReportKind || 'MERCHANT_STMT';
        var colFallback = (cfg.columns && cfg.columns.length) ? cfg.columns.length
          : (rkErr === 'REGIONAL_PAYOUT' && cfg.columnsRegionalPayout && cfg.columnsRegionalPayout.AGG ? cfg.columnsRegionalPayout.AGG.length
            : (cfg.columnsBySub && cfg.columnsBySub.AGG ? cfg.columnsBySub.AGG.length : 8));
        if (tbody) tbody.innerHTML = '<tr><td colspan="' + colFallback + '" class="empty-state-cell text-center text-danger">' + (err && err.message ? err.message : '조회 실패') + '</td></tr>';
      }).finally(function () {
        if (dimm) dimm.style.display = 'none';
        if (typeof window.applyPagePermissionToPane === 'function') window.applyPagePermissionToPane(p, url);
      });
    }
    function asNum(v) {
      if (v == null) return 0;
      if (typeof v === 'number') return v;
      var n = Number(String(v).replace(/,/g, ''));
      return isNaN(n) ? 0 : n;
    }
    function fmtNum(v) {
      try { return Number(v || 0).toLocaleString('ko-KR'); } catch (e) { return String(v || 0); }
    }
    function setSummaryText(paneEl, key, text) {
      if (!paneEl) return;
      var el = paneEl.querySelector('[data-summary="' + key + '"]');
      if (!el) el = paneEl.querySelector('[id="summary_' + key + '"]');
      if (el) el.textContent = key + ': ' + text;
    }
    pane.querySelectorAll('#searchBtn, .screen-search-btn').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var pageEl = pane.querySelector('#pageCnt');
        if (pageEl) pageEl.value = 1;
        doSearch(pane, tabId, 1);
      });
    });
    pane.querySelectorAll('.search-reset-btn').forEach(function (btn) {
      btn.addEventListener('click', function () {
        pane.querySelectorAll('select[name^="search"]').forEach(function (el) { el.selectedIndex = 0; });
        pane.querySelectorAll('input[name^="search"]').forEach(function (el) { if (el.type === 'text') el.value = ''; else if (el.type === 'checkbox') el.checked = false; });
        var pageEl = pane.querySelector('#pageCnt');
        if (pageEl) pageEl.value = 1;
        doSearch(pane, tabId, 1);
      });
    });
    pane.addEventListener('paging-change', function (e) {
      var detail = e.detail || {};
      doSearch(pane, tabId, detail.page);
    });
    var url = pane.getAttribute('formurl') || '';
    function ensureNotifyTargetPickerModal() {
      var modalEl = document.getElementById('notifyTargetPickerModal');
      if (modalEl) return modalEl;
      var wrap = document.createElement('div');
      wrap.innerHTML =
        '<div class="modal fade" id="notifyTargetPickerModal" tabindex="-1" aria-hidden="true">' +
        '<div class="modal-dialog modal-lg"><div class="modal-content">' +
        '<div class="modal-header"><h5 class="modal-title">노티 대상 선택</h5><button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="닫기"></button></div>' +
        '<div class="modal-body">' +
        '<div class="d-flex gap-2 mb-2"><input type="text" class="form-control form-control-sm" id="notifyTargetKeyword" placeholder="대상명/URL 검색">' +
        '<button type="button" class="btn btn-sm btn-primary" id="notifyTargetSearchBtn">검색</button></div>' +
        '<div class="table-responsive"><table class="table table-sm table-bordered mb-0 notify-target-picker-table"><thead><tr id="notifyTargetPickerTheadRow"><th class="notify-picker-select-cell align-middle">선택</th><th class="text-center" style="width:118px">구분</th><th>대상명</th><th>URL</th></tr></thead><tbody id="notifyTargetPickerTbody"></tbody></table></div>' +
        '</div></div></div></div>';
      document.body.appendChild(wrap.firstChild);
      var created = document.getElementById('notifyTargetPickerModal');
      var sb = created && created.querySelector('#notifyTargetSearchBtn');
      if (created && sb && !created._notifySearchBound && window.PG_API && window.PG_API.hqNotifyTargets) {
        created._notifySearchBound = true;
        sb.addEventListener('click', function () {
          window.PG_API.hqNotifyTargets().then(function (list2) {
            var mode = created.getAttribute('data-picker-mode') || 'single';
            if (mode === 'pair' && typeof created._renderPairRows === 'function') {
              created._renderPairRows(list2 || []);
            } else if (typeof created._renderSingleRows === 'function') {
              created._renderSingleRows(list2 || []);
            }
          }).catch(function () {});
        });
      }
      return created;
    }
    function bindNotifyTargetPicker() {
      if (!window.PG_API || !window.PG_API.hqNotifyTargets) return;
      var singleBtns = pane.querySelectorAll('button[data-action="노티선택"]');
      var pairBtns = pane.querySelectorAll('button[data-action="노티쌍선택"]');
      if (singleBtns.length === 0 && pairBtns.length === 0) return;
      function setNotifyPickerThead(modalEl, mode) {
        var theadRow = modalEl.querySelector('#notifyTargetPickerTheadRow');
        if (!theadRow) return;
        modalEl.setAttribute('data-picker-mode', mode);
        if (mode === 'pair') {
          theadRow.innerHTML =
            '<th class="notify-picker-select-cell align-middle">선택</th>' +
            '<th class="align-middle">대상명</th>' +
            '<th class="text-center align-middle notify-picker-channel-th-cb">CALLBACK <span class="text-muted fw-normal small">(서버)</span></th>' +
            '<th class="text-center align-middle notify-picker-channel-th-rs">RESULT <span class="text-muted fw-normal small">(브라우저)</span></th>';
        } else {
          theadRow.innerHTML =
            '<th class="notify-picker-select-cell align-middle">선택</th>' +
            '<th class="text-center align-middle" style="width:118px">구분</th>' +
            '<th class="align-middle">대상명</th>' +
            '<th class="align-middle">URL</th>';
        }
      }
      singleBtns.forEach(function (btn) {
        if (btn._notifyPickerBound) return;
        btn._notifyPickerBound = true;
        btn.addEventListener('click', function () {
          var field = btn.getAttribute('data-field') || '';
          var selectEl = pane.querySelector('[name="' + field + '"][data-load-notify-targets="true"]');
          if (!selectEl) return;
          var modalEl = ensureNotifyTargetPickerModal();
          var keywordEl = modalEl.querySelector('#notifyTargetKeyword');
          var tbody = modalEl.querySelector('#notifyTargetPickerTbody');
          modalEl._pickerSelectEl = selectEl;
          modalEl._pickerSelCallback = null;
          modalEl._pickerSelResult = null;
          setNotifyPickerThead(modalEl, 'single');
          function renderSingleRows(list) {
            var rows = pgNotifyTargetsFilterKeyword(list || [], keywordEl && keywordEl.value);
            tbody.innerHTML = '';
            if (rows.length === 0) {
              tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted">조회 결과가 없습니다.</td></tr>';
              return;
            }
            var sel = modalEl._pickerSelectEl;
            rows.forEach(function (t) {
              var ch = pgShortNotifyChannel(t);
              var badgeCls = ch === 'RESULT' ? 'notify-picker-badge notify-picker-badge--result' : 'notify-picker-badge notify-picker-badge--callback';
              var tr = document.createElement('tr');
              tr.innerHTML =
                '<td class="notify-picker-select-cell align-middle"><button type="button" class="btn btn-sm btn-outline-primary">선택</button></td>' +
                '<td class="text-center align-middle"><span class="' + badgeCls + '">' + ch + '</span></td>' +
                '<td class="align-middle">' + pgEscHtml(t.targetName || t.targetCode || '') + '</td>' +
                '<td class="align-middle"><code class="small notify-picker-url-code">' + pgEscHtml(t.targetUrl || '') + '</code></td>';
              tr.querySelector('button').addEventListener('click', function () {
                if (sel) sel.value = t.targetUrl || '';
                if (window.bootstrap && bootstrap.Modal) {
                  var mm = bootstrap.Modal.getInstance(modalEl);
                  if (mm) mm.hide();
                }
              });
              tbody.appendChild(tr);
            });
          }
          modalEl._renderSingleRows = renderSingleRows;
          modalEl._renderPairRows = null;
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.hqNotifyTargets().then(function (list) {
            renderSingleRows(list || []);
            if (window.bootstrap && bootstrap.Modal) {
              new bootstrap.Modal(modalEl).show();
            }
          }).catch(function (e) {
            alert(e && e.message ? e.message : '노티 대상 조회 실패');
          }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        });
      });
      pairBtns.forEach(function (btn) {
        if (btn._notifyPairPickerBound) return;
        btn._notifyPairPickerBound = true;
        btn.addEventListener('click', function () {
          var cbName = btn.getAttribute('data-callback-field') || 'notifyUrl1';
          var rsName = btn.getAttribute('data-result-field') || 'notifyUrl2';
          var selCb = pane.querySelector('[name="' + cbName + '"][data-load-notify-targets="true"]');
          var selRs = pane.querySelector('[name="' + rsName + '"][data-load-notify-targets="true"]');
          if (!selCb || !selRs) return;
          var modalEl = ensureNotifyTargetPickerModal();
          var keywordEl = modalEl.querySelector('#notifyTargetKeyword');
          var tbody = modalEl.querySelector('#notifyTargetPickerTbody');
          modalEl._pickerSelectEl = null;
          modalEl._pickerSelCallback = selCb;
          modalEl._pickerSelResult = selRs;
          setNotifyPickerThead(modalEl, 'pair');
          function renderPairRows(list) {
            var filtered = pgNotifyTargetsFilterKeyword(list || [], keywordEl && keywordEl.value);
            var groups = pgGroupNotifyTargetsByPairKey(filtered);
            var pairs = groups.filter(function (g) {
              var cb = g.filter(function (x) { return pgShortNotifyChannel(x) === 'CALLBACK'; })[0];
              var rs = g.filter(function (x) { return pgShortNotifyChannel(x) === 'RESULT'; })[0];
              return cb && rs;
            });
            tbody.innerHTML = '';
            if (pairs.length === 0) {
              tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted">CALLBACK·RESULT 쌍이 없습니다. 본사설정 &gt; 전산노티·결제환경에서 [노티자동생성]으로 등록하세요.</td></tr>';
              return;
            }
            var scb = modalEl._pickerSelCallback;
            var srs = modalEl._pickerSelResult;
            pairs.forEach(function (g) {
              var cb = g.filter(function (x) { return pgShortNotifyChannel(x) === 'CALLBACK'; })[0];
              var rs = g.filter(function (x) { return pgShortNotifyChannel(x) === 'RESULT'; })[0];
              var name = (cb && cb.targetName) || (rs && rs.targetName) || '';
              var tr = document.createElement('tr');
              tr.innerHTML =
                '<td class="notify-picker-select-cell align-middle"><button type="button" class="btn btn-sm btn-outline-primary">쌍 선택</button></td>' +
                '<td class="align-middle">' + pgEscHtml(name) + '</td>' +
                '<td class="align-middle notify-picker-url-cell notify-picker-url-cell--callback"><code class="small notify-picker-url-code">' + pgEscHtml((cb && cb.targetUrl) || '') + '</code></td>' +
                '<td class="align-middle notify-picker-url-cell notify-picker-url-cell--result"><code class="small notify-picker-url-code">' + pgEscHtml((rs && rs.targetUrl) || '') + '</code></td>';
              tr.querySelector('button').addEventListener('click', function () {
                if (scb) scb.value = (cb && cb.targetUrl) || '';
                if (srs) srs.value = (rs && rs.targetUrl) || '';
                if (window.bootstrap && bootstrap.Modal) {
                  var mm = bootstrap.Modal.getInstance(modalEl);
                  if (mm) mm.hide();
                }
              });
              tbody.appendChild(tr);
            });
          }
          modalEl._renderPairRows = renderPairRows;
          modalEl._renderSingleRows = null;
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.hqNotifyTargets().then(function (list) {
            renderPairRows(list || []);
            if (window.bootstrap && bootstrap.Modal) {
              new bootstrap.Modal(modalEl).show();
            }
          }).catch(function (e) {
            alert(e && e.message ? e.message : '노티 대상 조회 실패');
          }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        });
      });
    }
    if (pane.querySelector('select[data-load-notify-targets="true"]') && window.PG_API && window.PG_API.hqNotifyTargets) {
      window.PG_API.hqNotifyTargets().then(function (list) {
        var arr = Array.isArray(list) ? list : [];
        pane.querySelectorAll('select[data-load-notify-targets="true"]').forEach(function (sel) {
          var current = sel.value || '';
          var html = '<option value="">선택</option>';
          arr.forEach(function (t) {
            var ch = pgShortNotifyChannel(t);
            var url = String(t.targetUrl || '');
            var label = (t.targetName || t.targetCode || '노티') + ' [' + ch + ']';
            html += '<option value="' + url.replace(/&/g, '&amp;').replace(/"/g, '&quot;') + '">' + pgEscHtml(label) + '</option>';
          });
          sel.innerHTML = html;
          if (current) sel.value = current;
        });
      }).catch(function () {});
    }
    bindNotifyTargetPicker();
    if (url === '/system/noticeList') {
      var actRowNw = pane.querySelector('.screen-action-buttons');
      if (actRowNw && !pane.querySelector('[data-notice-write-btn]')) {
        var wb = document.createElement('button');
        wb.type = 'button';
        wb.className = 'btn btn-success btn-sm';
        wb.setAttribute('data-notice-write-btn', '1');
        wb.classList.add('d-none');
        wb.textContent = '공지 등록';
        wb.addEventListener('click', function () {
          var u = getSessionUser();
          if (!u || !u.canWriteNotice) { alert('공지 등록 권한이 없습니다. 조직 등급(총본사·본사·총판)과 [공지사항] 화면 권한을 확인하세요.'); return; }
          openNoticeWriteModal(function () {
            doSearch(pane, tabId, 1);
          });
        });
        actRowNw.insertBefore(wb, actRowNw.firstChild);
      }
      syncAllNoticeWriteButtons();
    }
    var autoSearchUrls = ['/system/noticeList', '/calc/payList', '/calc/payNotiList', '/calc/paySuccessList', '/calc/payFailList', '/calc/payRefundList', '/calc/payForceRefundList', '/calc/payCancelList', '/calc/offsetCancList', '/pay/easyPay', '/pay/chatbotPay',
      '/comp/compMngTree', '/comp/compInfoHistList', '/commission/commisionList',
      '/user/userMng', '/set/gridSetMng',
      '/calc/calcList', '/calc/calcGmList', '/calc/feeList', '/settlement/feeList', '/calc/compPointMngList', '/settlement/recallMng', '/calc/balcInfo', '/calc/balanceList', '/calc/unpaidMng', '/calc/exCalcList', '/pay/payHoldList', '/calc/collateralList', '/settlement/collateralList',
      '/noti/notiUrlMng', '/noti/notiSendMngList', '/noti/notiCashReceiptUrlMng', '/noti/notiCashReceiptSendMngList',
      '/hq/pgApiMng', '/hq/permissionMng', '/hq/accountMng', '/risk/list'];
    function applySettlementReportAccessThenSearch() {
      function runSearch() {
        loadViewSetting().finally(function () {
          doSearch(pane, tabId, 1);
        });
      }
      if (!window.PG_API || !window.PG_API.settlementReportAccess) {
        runSearch();
        return;
      }
      window.PG_API.settlementReportAccess().then(function (data) {
        var rk = pane.querySelector('select[name="searchReportKind"]');
        if (rk && data) {
          var mer = data.merchantStmt !== false;
          var reg = data.regionalPayout === true;
          var html = '';
          if (mer) html += '<option value="MERCHANT_STMT">가맹점 정산 리포트</option>';
          if (reg) html += '<option value="REGIONAL_PAYOUT">본사 지급 리포트(총본사→본사)</option>';
          rk.innerHTML = html || '<option value="MERCHANT_STMT">가맹점 정산 리포트</option>';
          if (!rk.value && rk.options.length) rk.selectedIndex = 0;
        }
      }).catch(function () {}).finally(function () {
        runSearch();
      });
    }
    if (url === '/calc/settlementReport' || url === '/settlement/settlementReport') {
      setTimeout(function () {
        if (window.PG_LAST_REGISTERED_COMP && url === '/commission/commisionList') {
          var sid = pane.querySelector('input[name="searchCompId"]');
          if (sid) sid.value = window.PG_LAST_REGISTERED_COMP;
          window.PG_LAST_REGISTERED_COMP = null;
        }
        applySettlementReportAccessThenSearch();
      }, 100);
    } else if (autoSearchUrls.indexOf(url) !== -1) {
      setTimeout(function () {
        if (window.PG_LAST_REGISTERED_COMP && url === '/commission/commisionList') {
          var sid = pane.querySelector('input[name="searchCompId"]');
          if (sid) sid.value = window.PG_LAST_REGISTERED_COMP;
          window.PG_LAST_REGISTERED_COMP = null;
        }
        loadViewSetting().finally(function () {
          doSearch(pane, tabId, 1);
        });
      }, 100);
    }
    if ((url === '/calc/settlementReport' || url === '/settlement/settlementReport') && !pane._settlementReportKindBound) {
      pane._settlementReportKindBound = true;
      pane.addEventListener('change', function (ev) {
        if (ev.target && ev.target.name === 'searchReportKind') {
          var pageEl = pane.querySelector('#pageCnt');
          if (pageEl) pageEl.value = 1;
          doSearch(pane, tabId, 1);
        }
      });
    }
    if (url === '/commission/commisionList' && !pane._commissionHistRowClickBound) {
      pane._commissionHistRowClickBound = true;
      pane.addEventListener('click', function (e) {
        var tr = e.target && e.target.closest ? e.target.closest('#grid_' + tabId + ' tbody tr') : null;
        if (!tr || tr.querySelector('.empty-state-cell')) return;
        if (e.target && e.target.closest && e.target.closest('.grid-row-check, .grid-check-all')) return;
        var cid = tr.getAttribute('data-comp-id');
        if (!cid) return;
        pane._commissionHistCompId = cid;
        pane.querySelectorAll('#grid_' + tabId + ' tbody tr.table-active').forEach(function (x) { x.classList.remove('table-active'); });
        tr.classList.add('table-active');
        loadCommissionHistoryGrid(pane, tabId);
      });
    }
    if (url === '/commission/commisionList') {
      var commissionSettingBtn = pane.querySelector('#commissionSettingBtn');
      if (commissionSettingBtn) {
        commissionSettingBtn.addEventListener('click', function () {
          var grid = pane.querySelector('#grid_' + tabId + ' tbody');
          var checked = grid ? grid.querySelector('tr .grid-row-check:checked') : null;
          if (!checked) {
            alert('그리드에서 한 건을 선택한 뒤 [수수료설정]을 눌러주세요.');
            return;
          }
          var tr = checked.closest('tr');
          var tds = tr.querySelectorAll('td');
          var cfg = window.PG_SCREENS && window.PG_SCREENS.getMenuScreens()['/commission/commisionList'];
          var cols = (cfg && cfg.columns) || [];
          var idx = cols.findIndex(function (c) { return c.key === 'compId'; });
          if (idx < 0) idx = 1;
          var compId = (tds[idx] && tds[idx].textContent) ? tds[idx].textContent.trim() : '';
          if (!compId) { alert('업체코드를 찾을 수 없습니다.'); return; }
          var modalEl = document.getElementById('commissionSettingModal');
          var compIdHidden = document.getElementById('commissionSettingCompId');
          if (compIdHidden) compIdHidden.value = compId;
          window._commissionSettingPane = pane;
          window._commissionSettingTabId = tabId;
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.commissionDetail(compId).then(function (data) {
            function setAmt(id, val) {
              var el = document.getElementById(id);
              if (el && val != null) el.value = pgFmtOneDecimalStripWhole(val);
            }
            function setPct(id, val) {
              var el = document.getElementById(id);
              if (el && val != null) el.value = pgFmtPctOneDecimalInput(val);
            }
            function setDay(id, val) {
              var el = document.getElementById(id);
              if (!el || val == null) return;
              var n = parseFloat(String(val).replace(/,/g, '.'));
              el.value = isFinite(n) ? String(Math.round(n)) : String(val);
            }
            var set = function (id, val) { var el = document.getElementById(id); if (el && val != null) el.value = String(val); };
            setAmt('commissionPerTxFee', data.perTxFee);
            setAmt('commissionCancelRate', data.cancelRate);
            setAmt('commissionVoidFeePerTx', data.voidFeePerTx);
            setAmt('commissionManualVoidFeePerTx', data.manualVoidFeePerTx);
            setPct('commissionPayRate', data.payRate);
            setAmt('commissionRefundRate', data.refundRate);
            setPct('commissionRollingPct', data.rollingPct);
            setDay('commissionRollingDays', data.rollingDays);
            setAmt('commissionFeeAnnual', data.feeAnnual);
            setAmt('commissionFeeSettlementPerTx', data.feeSettlementPerTx);
            setPct('commissionFee3dsRate', data.fee3dsRate);
            setAmt('commissionChargebackFeePerTx', data.chargebackFeePerTx);
            setPct('commissionHqRate', data.hqRate);
            setPct('commissionRegionalRate', data.regionalRate);
            setPct('commissionMasterRate', data.masterRate);
            setPct('commissionBranchRate', data.branchRate);
            setPct('commissionAgencyRate', data.agencyRate);
            setPct('commissionSalesOfficeRate', data.salesOfficeRate);
            setAmt('commissionHqPerTxFee', data.hqPerTxFee);
            setAmt('commissionRegionalPerTxFee', data.regionalPerTxFee);
            setAmt('commissionMasterPerTxFee', data.masterPerTxFee);
            setAmt('commissionBranchPerTxFee', data.branchPerTxFee);
            setAmt('commissionAgencyPerTxFee', data.agencyPerTxFee);
            setAmt('commissionSalesOfficePerTxFee', data.salesOfficePerTxFee);
            set('commissionApplyStartDate', data.applyStartDateStr || data.applyStartDate || '');
            fillChargebackPolicySelectsInRoot(modalEl, data.chargebackPolicyId).finally(function () {
              if (modalEl && window.bootstrap && bootstrap.Modal) {
                var modal = new bootstrap.Modal(modalEl);
                modal.show();
              }
            });
          }).catch(function (e) { alert(e && e.message ? e.message : '수수료 조회 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        });
      }
      if (!pane._commissionInlineEditBound) {
        pane._commissionInlineEditBound = true;
        pane.addEventListener('click', function (e) {
          var saveBtn = e.target && e.target.closest ? e.target.closest('.commission-inline-save') : null;
          var clearBtn = e.target && e.target.closest ? e.target.closest('.commission-inline-clear') : null;
          if (!saveBtn && !clearBtn) return;
          var tr = (saveBtn || clearBtn).closest('tr');
          if (!tr) return;
          var idx = parseInt(tr.getAttribute('data-row-idx') || '-1', 10);
          var list = pane._lastGridList || [];
          if (idx < 0 || idx >= list.length) { alert('행 데이터를 찾을 수 없습니다. 다시 검색해 주세요.'); return; }
          var row = list[idx];
          var compIdVal = row && row.compId ? String(row.compId).trim() : '';
          if (!compIdVal) { alert('업체코드를 찾을 수 없습니다.'); return; }
          var editableKeys = ['hqRate', 'regionalRate', 'masterRate', 'branchRate', 'agencyRate', 'salesOfficeRate', 'hqPerTxFee', 'regionalPerTxFee', 'masterPerTxFee', 'branchPerTxFee', 'agencyPerTxFee', 'salesOfficePerTxFee', 'applyDt'];
          var fd = {};
          editableKeys.forEach(function (k) {
            var inp = tr.querySelector('.commission-inline-input[data-key="' + k + '"]');
            if (!inp) return;
            var v = (inp.value || '').trim();
            if (clearBtn && k !== 'applyDt') v = '0';
            if (k === 'applyDt') fd.applyStartDate = v;
            else fd[k] = v === '' ? '0' : v;
          });
          var dimmI = document.getElementById('dimm');
          if (dimmI) dimmI.style.display = 'flex';
          window.PG_API.commissionSave(compIdVal, fd).then(function () {
            alert(clearBtn ? '수수료를 0으로 초기화했습니다.' : '수수료가 저장되었습니다.');
            doSearch(pane, tabId, parseInt(pane.getAttribute('data-last-page') || '1', 10) || 1);
          }).catch(function (err) {
            alert(err && err.message ? err.message : '수수료 저장 실패');
          }).finally(function () {
            if (dimmI) dimmI.style.display = 'none';
          });
        });
      }
    }
    var commissionSettingSaveBtn = document.getElementById('commissionSettingSaveBtn');
    if (commissionSettingSaveBtn && !commissionSettingSaveBtn._bound) {
      commissionSettingSaveBtn._bound = true;
      commissionSettingSaveBtn.addEventListener('click', function () {
        var cid = document.getElementById('commissionSettingCompId');
        var compIdVal = (cid && cid.value) ? cid.value.trim() : '';
        if (!compIdVal) return;
        var modalEl = document.getElementById('commissionSettingModal');
        var fd = {};
        if (modalEl) {
          modalEl.querySelectorAll('input[name], select[name]').forEach(function (el) {
            if (el.name) fd[el.name] = el.value;
          });
        }
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.commissionSave(compIdVal, fd).then(function () {
          alert('저장되었습니다.');
          if (modalEl && window.bootstrap && bootstrap.Modal) { var m = bootstrap.Modal.getInstance(modalEl); if (m) m.hide(); }
          var pane = window._commissionSettingPane;
          var tabId = window._commissionSettingTabId;
          if (pane && tabId && typeof doSearch === 'function') doSearch(pane, tabId, 1);
        }).catch(function (err) { alert(err && err.message ? err.message : '저장 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      });
    }
    if (url === '/comp/compMngTree' && !pane._treeToggleBound) {
      pane._treeToggleBound = true;
      pane.addEventListener('click', function (e) {
        var tg = e.target && e.target.closest ? e.target.closest('.tree-toggle') : null;
        if (!tg || tg.classList.contains('tree-toggle-placeholder')) return;
        var id = tg.getAttribute('data-id') || '';
        if (!id) return;
        var tbody = pane.querySelector('#grid_' + tabId + ' tbody');
        var list = pane._treeList;
        if (!tbody || !list) return;
        var expanded = pane._treeExpanded;
        if (!expanded) return;
        if (expanded.has) {
          if (expanded.has(id)) expanded.delete(id); else expanded.add(id);
        } else {
          if (id in expanded) delete expanded[id]; else expanded[id] = 1;
        }
        applyTreeVisibility(pane, tbody, list);
      });
    }
    var compMngSaveColumnsBtn = pane.querySelector('#compMngSaveColumnsBtn');
    if (compMngSaveColumnsBtn && !compMngSaveColumnsBtn._bound) {
      pane.querySelectorAll('.column-guide-check').forEach(function (cb) {
        cb.addEventListener('change', syncColumnGuideUiState);
      });
      syncColumnGuideUiState();
      compMngSaveColumnsBtn._bound = true;
      compMngSaveColumnsBtn.addEventListener('click', function () {
        if (!confirm('해당 설정값을 적용하시겠습니까?')) return;
        var keys = getSelectedGuideKeys();
        applySelectedGuideKeys(keys);
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.userViewSettingSave(url, JSON.stringify(keys)).then(function () {
          doSearch(pane, tabId, 1);
        }).catch(function (e) {
          alert(e && e.message ? e.message : 'VIEW SETTING 저장 실패');
        }).finally(function () {
          if (dimm) dimm.style.display = 'none';
        });
      });
    }
    var compMngClearColumnsBtn = pane.querySelector('#compMngClearColumnsBtn');
    if (compMngClearColumnsBtn && !compMngClearColumnsBtn._bound) {
      compMngClearColumnsBtn._bound = true;
      compMngClearColumnsBtn.addEventListener('click', function () {
        if (!confirm('모든 칼럼 선택을 해제하시겠습니까?')) return;
        applySelectedGuideKeys([]);
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.userViewSettingSave(url, '[]').then(function () {
          doSearch(pane, tabId, 1);
        }).catch(function (e) {
          alert(e && e.message ? e.message : 'VIEW SETTING 저장 실패');
        }).finally(function () {
          if (dimm) dimm.style.display = 'none';
        });
      });
    }
    // TEMP_REMOVE_AFTER_DEV — [업체전체초기화] 버튼·핸들러 (ApiCompController admin-reset·플래그와 함께 제거)
    var compAdminResetOrgBtn = pane.querySelector('#compAdminResetOrgBtn');
    if (compAdminResetOrgBtn && url === '/comp/compMngTree') {
      var uReset = getSessionUser();
      if (!uReset || String(uReset.role || '').toUpperCase() !== 'ADMIN') {
        compAdminResetOrgBtn.classList.add('d-none');
      } else {
        compAdminResetOrgBtn.classList.remove('d-none');
      }
      if (!compAdminResetOrgBtn._bound) {
      compAdminResetOrgBtn._bound = true;
      compAdminResetOrgBtn.addEventListener('click', function () {
        if (getPagePermissionForUrl(url) === 'OBSERVER') return;
        if (!confirm('【업체 전체 초기화】\n\n· 모든 조직(총판·본사·가맹점 등) 및 거래·정산·가맹 연관 데이터가 삭제됩니다.\n· ADMIN 계정은 유지되나, 모든 로그인 세션이 끊깁니다.\n· 총본사 코드 0000000000 만 다시 생성됩니다.\n\n정말 실행할까요?')) return;
        if (!confirm('마지막 확인: 되돌릴 수 없습니다. 서버에서 allow-org-hierarchy-reset 이 켜져 있어야 성공합니다. 계속?')) return;
        if (!window.PG_API || !window.PG_API.compAdminResetOrgHierarchy) {
          alert('API를 사용할 수 없습니다. site/js/api.js 를 반영했는지 확인하세요.');
          return;
        }
        var dimmR = document.getElementById('dimm');
        if (dimmR) dimmR.style.display = 'flex';
        window.PG_API.compAdminResetOrgHierarchy().then(function (r) {
          if (r && r.success === false) {
            alert(r.message || '실패했습니다. 서버 설정 app.features.allow-org-hierarchy-reset=true 및 재시작 여부를 확인하세요.');
            return;
          }
          var d = r && r.data ? r.data : r;
          var msg = (d && d.message) ? d.message : '초기화되었습니다.';
          alert(msg);
          try {
            if (window.PG_API.clearAuth) window.PG_API.clearAuth();
          } catch (e0) {}
          try {
            if (window.location) window.location.replace((window.location.origin || '') + '/login.html');
          } catch (e1) {}
        }).catch(function (e) {
          alert(e && e.message ? e.message : '초기화 요청 실패');
        }).finally(function () {
          if (dimmR) dimmR.style.display = 'none';
        });
      });
      }
    }
    // TEMP_REMOVE_AFTER_DEV — [삭제(개발)] 버튼·핸들러 (dev-tree-remove·플래그와 함께 제거)
    var compDevTreeRemoveBtn = pane.querySelector('#compDevTreeRemoveBtn');
    if (compDevTreeRemoveBtn && url === '/comp/compMngTree') {
      var uDev = getSessionUser();
      if (!uDev || String(uDev.role || '').toUpperCase() !== 'ADMIN') {
        compDevTreeRemoveBtn.classList.add('d-none');
      } else {
        compDevTreeRemoveBtn.classList.remove('d-none');
      }
      compDevTreeRemoveBtn.addEventListener('click', function () {
        if (getPagePermissionForUrl(url) === 'OBSERVER') return;
        var grid = pane.querySelector('#grid_' + tabId);
        if (!grid) return;
        var checked = grid.querySelectorAll('tbody .grid-row-check:checked');
        if (checked.length !== 1) {
          alert('그리드에서 정확히 한 건만 체크한 뒤 [삭제(개발)]을 눌러주세요.');
          return;
        }
        var tr = checked[0].closest('tr');
        var dr = tr && tr.getAttribute('data-row');
        var compId = '';
        if (dr) {
          try {
            var row = JSON.parse(decodeURIComponent(dr));
            compId = row.compId ? String(row.compId).trim() : '';
          } catch (e1) {}
        }
        if (!compId) {
          alert('선택한 행에서 업체코드를 읽을 수 없습니다.');
          return;
        }
        if (!confirm('[' + compId + '] 및 그 하위 전체 조직의 업체 프로필을 미사용(N)으로 바꿉니다.\n(개발용 — DB 행·테이블 물리 삭제 없음. 총본사는 불가)\n계속할까요?')) return;
        if (!window.PG_API || !window.PG_API.compDevTreeRemove) {
          alert('API를 사용할 수 없습니다.');
          return;
        }
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.compDevTreeRemove(compId).then(function (r) {
          var d = r && r.data ? r.data : r;
          var msg = (d && d.message) ? d.message : '처리되었습니다.';
          if (r && r.success === false) alert(r.message || msg);
          else { alert(msg); doSearch(pane, tabId, 1); }
        }).catch(function (e) { alert(e && e.message ? e.message : '처리 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      });
    }
    var excelRegBtn = pane.querySelector('#excelRegBtn');
    if (excelRegBtn && url === '/comp/compMngTree' && !excelRegBtn._bound) {
      excelRegBtn._bound = true;
      var excelRegInput = document.createElement('input');
      excelRegInput.type = 'file';
      excelRegInput.accept = '.xlsx,.xls';
      excelRegInput.style.display = 'none';
      pane.appendChild(excelRegInput);
      excelRegBtn.addEventListener('click', function () {
        excelRegInput.value = '';
        excelRegInput.click();
      });
      excelRegInput.addEventListener('change', function () {
        var file = this.files && this.files[0];
        if (!file) return;
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.compExcelRegister(file).then(function (r) {
          var d = r && r.data ? r.data : r;
          var created = (d.createdCount || 0);
          var errCnt = (d.errorCount || 0);
          var msg = '등록 완료: ' + created + '건';
          if (errCnt > 0) msg += ', 오류: ' + errCnt + '건';
          if (d.errors && d.errors.length > 0) msg += '\n\n오류:\n' + d.errors.slice(0, 5).join('\n') + (d.errors.length > 5 ? '\n...외 ' + (d.errors.length - 5) + '건' : '');
          alert(msg);
          doSearch(pane, tabId, 1);
        }).catch(function (e) { alert(e && e.message ? e.message : '엑셀 등록 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      });
    }
    var excelSampleBtn = pane.querySelector('#excelSampleBtn');
    if (excelSampleBtn && url === '/comp/compMngTree' && !excelSampleBtn._bound) {
      excelSampleBtn._bound = true;
      excelSampleBtn.addEventListener('click', function () {
        downloadCompExcelSampleFile();
      });
    }
    var compRegBtn = pane.querySelector('#compRegBtn');
    if (compRegBtn) {
      compRegBtn.addEventListener('click', function () { fnTopMenuMove('/comp/compReg'); });
    }
    var compRegSaveBtn = pane.querySelector('#compRegSaveBtn');
    if (compRegSaveBtn) {
      compRegSaveBtn.addEventListener('click', function () {
        if (getPagePermissionForUrl(url) === 'OBSERVER') return;
        var form = pane.querySelector('#compRegForm');
        if (!form) return;
        initIntlPhoneFields(form);
        ['ceoMobile', 'compTel', 'fax', 'settleTelNo', 'contactTel'].forEach(function (n) { syncIntlPhoneHidden(form, n); });
        var fd = {};
        form.querySelectorAll('input, select, textarea').forEach(function (el) {
          if (el.name && el.type !== 'file' && el.name !== 'pgOperational') {
            if (el.name.indexOf('__phone_') === 0) return;
            var card = el.closest('.card');
            if (card && card.classList.contains('d-none')) return;
            fd[el.name] = el.value;
          }
        });
        if (fd.countryCd === 'OTHER') { fd.bankCd = fd.bankCdText || fd.bankCd; delete fd.bankCdText; }
        if (fd.addrCountryCd === 'OTHER') { fd.addrCountryCd = fd.addrCountryCdOther || ''; delete fd.addrCountryCdOther; }
        fd.compNm = fd.compNm || fd.comp_name;
        fd.compDiv = fd.compDiv || fd.comp_div || 'MERCHANT';
        if (!fd.compNm || !fd.compNm.trim()) { alert('업체명을 입력하세요.'); return; }
        if (!fd.compDiv || fd.compDiv === '') { alert('업체구분을 선택하세요.'); return; }
        if (!fd.loginId || !String(fd.loginId).trim()) { alert('로그인ID를 입력하세요.'); return; }
        if (!fd.pwd || !String(fd.pwd).trim()) { alert('비밀번호를 입력하세요.'); return; }
        if ((form.getAttribute('data-password-confirmed') || '') !== 'Y') {
          alert('비밀번호 입력 후 옆의 [저장] 버튼으로 비밀번호를 확정하세요.');
          return;
        }
        var checkedId = form.getAttribute('data-login-id-checked') || '';
        if (checkedId !== String(fd.loginId).trim()) {
          alert('로그인ID 중복확인을 먼저 진행하세요.');
          return;
        }
        var needsParent = ['MASTER_DIST', 'BRANCH', 'AGENCY', 'SALES_OFFICE', 'MERCHANT'].indexOf((fd.compDiv || '').toUpperCase()) >= 0;
        if (needsParent && (!fd.parentId || !String(fd.parentId).trim())) { alert('상위 지점을 선택하세요. [검색] 버튼으로 상위업체를 선택해주세요.'); return; }
        if (fd.parentId) fd.parentComp = '';
        if (fd.regType != null) { fd.regNo = (fd.regType || 'CORP') + '|' + (fd.regNo || ''); delete fd.regType; }
        var tbody = form.querySelector('#pgBindingTbody');
        if (tbody) {
          var operationalVal = form.querySelector('input[name="pgOperational"]:checked');
          var operationalIdx = operationalVal ? parseInt(operationalVal.value, 10) : 0;
          var bindings = [];
          tbody.querySelectorAll('tr').forEach(function (tr, i) {
            var sel = function (f) { var e = tr.querySelector('[data-field="' + f + '"]'); return e ? e.value : ''; };
            var pgCd = sel('pgCd');
            if (pgCd) {
              bindings.push({
                pgCd: pgCd,
                activationYn: sel('activationYn') || 'Y',
                operationalYn: i === operationalIdx ? 'Y' : 'N',
                payMethod: sel('payMethod') || 'WEB',
                mid: sel('mid'),
                rootNo: sel('rootNo'),
                apiKey: sel('apiKey'),
                ivKey: sel('ivKey'),
                installmentYn: sel('installmentYn') || 'N',
                maxInstallmentMonths: sel('maxInstallmentMonths')
              });
            }
          });
          fd.pgBindings = JSON.stringify(bindings);
        }
        if (fd.compDiv === 'REGIONAL') {
          var bc = [fd.baseCurrency1, fd.baseCurrency2, fd.baseCurrency3].filter(function (v) { return v && v.trim(); });
          if (bc.length === 0) { alert('본사는 기준 화폐를 1가지 이상 선택하세요.'); return; }
          fd.baseCurrency = bc.join(',');
          delete fd.baseCurrency1;
          delete fd.baseCurrency2;
          delete fd.baseCurrency3;
        } else if (fd.compDiv === 'MASTER_DIST') {
          var masterCur = (fd.baseCurrency || '').trim();
          if (!masterCur) { alert('총판은 상위 본사가 설정한 기준 화폐 중 1가지를 선택하세요.'); return; }
          fd.baseCurrency = masterCur;
          delete fd.baseCurrency1;
          delete fd.baseCurrency2;
          delete fd.baseCurrency3;
        }
        if (fd.compDiv === 'REGIONAL' || fd.compDiv === 'MASTER_DIST') {
          var cardLimitTbody = form.querySelector('#regionalCardLimitTbody');
          if (cardLimitTbody) {
            var cardLimits = [];
            cardLimitTbody.querySelectorAll('tr').forEach(function (tr) {
              var sel = function (f) { var e = tr.querySelector('[data-field="' + f + '"]'); return e ? e.value : ''; };
              cardLimits.push({ payMethod: sel('payMethod'), cardIssuer: sel('cardIssuer'), dayLimit: sel('dayLimit'), timesLimit: sel('timesLimit'), amtLimit: sel('amtLimit'), regReason: sel('regReason'), regDt: sel('regDt'), modDt: sel('modDt'), remark: sel('remark') });
            });
            fd.regionalCardLimits = JSON.stringify(cardLimits);
          }
          var terminalTbody = form.querySelector('#regionalTerminalTbody');
          if (terminalTbody) {
            var terminals = [];
            terminalTbody.querySelectorAll('tr').forEach(function (tr) {
              var sel = function (f) { var e = tr.querySelector('[data-field="' + f + '"]'); return e ? e.value : ''; };
              terminals.push({ pgAgency: sel('pgAgency'), terminalId: sel('terminalId'), remark: sel('remark') });
            });
            fd.regionalTerminals = JSON.stringify(terminals);
          }
          if (fd.holidayCountryCode) fd.holidayCountryCodes = fd.holidayCountryCode;
          if (fd.businessHolidayRangesJson) {
            try {
              var _rows = JSON.parse(fd.businessHolidayRangesJson || '[]');
              fd.businessHolidayExtraDates = (_rows || []).map(function (r) { return r.fromDate || ''; }).filter(function (v) { return !!v; }).join('\n');
            } catch (e) {}
          }
          var regionalKeys = ['remitterName', 'balanceNotifyAmt', 'suspiciousNotifyAmt', 'overseasLoginNotifyAmt', 'tempPwdNotifyAmt', 'nonTranCriterionMonth', 'sameCardLimitWebDay', 'sameCardLimitWebTimes', 'sameCardLimitWebAmt', 'sameCardLimitTerminalDay', 'sameCardLimitTerminalTimes', 'sameCardLimitTerminalAmt', 'dailyUsageFee', 'depositNameLookup', 'transferAuthNo', 'autoConvertNewMemberLimit', 'newMemberDailyLimit', 'convertRefDate', 'convertDailyLimit', 'applyStartDate', 'pgFeeGeneral', 'transferFee', 'settleDiffMonthCnt', 'settleReportBankCd', 'pgFeeSamsung', 'smsFee', 'taxInvoiceEmail', 'settleAccountNo', 'directFee', 'solutionFee', 'settleAccountHolder', 'withdrawRestrictType', 'withdrawRestrictStartTime', 'withdrawRestrictEndTime', 'terminalPayRestrict', 'webPayRestrict', 'defaultFeeHq', 'defaultFeeDist', 'defaultFeeBranch', 'defaultFeeAgency', 'defaultFeeSalesOffice', 'defaultPayLimitPerTx', 'defaultPayLimitDay', 'defaultPayLimitMonth', 'defaultPayLimitYearCorp', 'defaultPayLimitYearInd', 'copyright', 'regionalCardLimits', 'regionalTerminals', 'holidayProfileName', 'holidayProfileCountry', 'holidayCountryCode', 'holidayCountryCodes', 'businessHolidayRangesJson', 'businessHolidayExtraDates'];
          var regionalSettings = {};
          regionalKeys.forEach(function (k) { if (fd[k] !== undefined && fd[k] !== null && fd[k] !== '') regionalSettings[k] = fd[k]; });
          fd.regionalSettings = JSON.stringify(regionalSettings);
        }
        if (fd.compDiv === 'MASTER_DIST') {
          var n1 = String(fd.notifyUrl1 || '').trim();
          var n2 = String(fd.notifyUrl2 || '').trim();
          var hasBackup = !!(String(fd.notifyUrl3 || '').trim() || String(fd.notifyUrl4 || '').trim());
          if (!n1) { alert('총판은 노티 CALLBACK(URL 1)을 입력해야 합니다.'); return; }
          if (!n2) { alert('총판은 노티 RESULT(URL 2)를 입력해야 합니다.'); return; }
          if (hasBackup && (!n1 || !n2)) { alert('노티 URL 3·4(보조)를 쓰려면 URL 1·2(CALLBACK·RESULT)가 모두 필요합니다.'); return; }
        }
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        var mainFile = form.querySelector('#brandingMainImageFile');
        var logoFile = form.querySelector('#brandingLogoImageFile');
        var themeEl = form.querySelector('#brandingTheme');
        var hostEl = form.querySelector('#brandingBrandHost');
        var isRegOrMaster = (fd.compDiv === 'REGIONAL' || fd.compDiv === 'MASTER_DIST');
        window.PG_API.compRegister(fd).then(function (res) {
          var data = res && res.data ? res.data : res;
          var compId = data && data.compId ? data.compId : '';
          if (compId && isRegOrMaster && window.PG_API.orgBrandingUpload && window.PG_API.orgBrandingSave) {
            var chain = Promise.resolve();
            if (mainFile && mainFile.files && mainFile.files[0]) {
              var _regMainF = mainFile.files[0];
              chain = chain.then(function () {
                return window.PG_API.orgBrandingUpload(compId, 'main', _regMainF).then(function (data) {
                  pgBrandingSetImageDisplayInput(form, 'main', data, _regMainF);
                  return data;
                });
              });
            }
            if (logoFile && logoFile.files && logoFile.files[0]) {
              var _regLogoF = logoFile.files[0];
              chain = chain.then(function () {
                return window.PG_API.orgBrandingUpload(compId, 'logo', _regLogoF).then(function (data) {
                  pgBrandingSetImageDisplayInput(form, 'logo', data, _regLogoF);
                  return data;
                });
              });
            }
            if (themeEl) {
              chain = chain.then(function () { return window.PG_API.orgBrandingSave(compId, themeEl.value || 'DEFAULT', hostEl ? hostEl.value : undefined); });
            }
            return chain.then(function () { return res; });
          }
          return res;
        }).then(function (res) {
          var data = res && res.data ? res.data : res;
          if (data && data.compId) {
            window.PG_LAST_REGISTERED_COMP = data.compId;
            if (fd.compDiv !== 'MERCHANT') {
              try { sessionStorage.setItem('pg_comp_detail_return_compId', data.compId); } catch (e) {}
            }
          }
          alert('저장되었습니다.');
          if (fd.compDiv === 'MERCHANT') {
            fnTopMenuMove('/commission/commisionList', null, '수수료관리');
          } else {
            fnTopMenuMove('/comp/compMngTree', null, '업체관리');
          }
        }).catch(function (err) {
          alert(err && err.message ? err.message : '저장에 실패했습니다.');
        }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      });
    }
    var compRegCancelBtn = pane.querySelector('#compRegCancelBtn');
    if (compRegCancelBtn) {
      compRegCancelBtn.addEventListener('click', function () { fnTopMenuMove('/comp/compMngTree'); });
    }
    if (url === '/comp/compReg') {
      initBankByCountry(pane);
      initCountryBankGroup(pane);
      initCountryAddressGroup(pane);
      initIntlPhoneFields(pane);
      var form = pane.querySelector('#compRegForm');
      var loginIdElForCheck = form ? form.querySelector('[name="loginId"]') : null;
      if (loginIdElForCheck && !loginIdElForCheck._dupResetBound) {
        loginIdElForCheck._dupResetBound = true;
        loginIdElForCheck.addEventListener('input', function () {
          if (form) form.setAttribute('data-login-id-checked', '');
        });
      }
      var pwdElForConfirm = form ? form.querySelector('[name="pwd"]') : null;
      if (pwdElForConfirm && !pwdElForConfirm._pwdConfirmResetBound) {
        pwdElForConfirm._pwdConfirmResetBound = true;
        pwdElForConfirm.addEventListener('input', function () {
          if (form) form.removeAttribute('data-password-confirmed');
        });
      }
      if (form && !form._compRegPwdSaveDelegated) {
        form._compRegPwdSaveDelegated = true;
        form.addEventListener('click', function (ev) {
          var btn = ev.target && ev.target.closest ? ev.target.closest('button[data-field="pwd"][data-action="저장"]') : null;
          if (!btn || !form.contains(btn)) return;
          var pwdInput = form.querySelector('[name="pwd"]');
          var v = pwdInput && pwdInput.value ? String(pwdInput.value).trim() : '';
          if (!v) { alert('비밀번호를 입력하세요.'); return; }
          if (v.length < 8) { alert('비밀번호는 8자 이상 입력하세요.'); return; }
          form.setAttribute('data-password-confirmed', 'Y');
          alert('비밀번호가 확정되었습니다. 하단 [저장]으로 업체를 등록하세요.');
        });
      }
      var loginDupBtn = pane.querySelector('button[data-field="loginId"][data-action="중복확인"]');
      if (loginDupBtn && !loginDupBtn._bound) {
        loginDupBtn._bound = true;
        loginDupBtn.addEventListener('click', function () {
          var input = form ? form.querySelector('[name="loginId"]') : null;
          var loginId = input && input.value ? String(input.value).trim() : '';
          if (!loginId) { alert('로그인ID를 입력하세요.'); return; }
          var dimmDup = document.getElementById('dimm');
          if (dimmDup) dimmDup.style.display = 'flex';
          window.PG_API.compCheckLoginId(loginId).then(function (r) {
            var ok = !!(r && r.available);
            if (ok) {
              if (form) form.setAttribute('data-login-id-checked', loginId);
              alert('사용 가능한 로그인ID입니다.');
            } else {
              if (form) form.setAttribute('data-login-id-checked', '');
              alert('이미 사용 중인 로그인ID입니다.');
            }
          }).catch(function (e) {
            if (form) form.setAttribute('data-login-id-checked', '');
            alert(e && e.message ? e.message : '중복확인 실패');
          }).finally(function () { if (dimmDup) dimmDup.style.display = 'none'; });
        });
      }
      if (form && !form.querySelector('input[name="parentId"]')) {
        var hid = document.createElement('input');
        hid.type = 'hidden';
        hid.name = 'parentId';
        hid.id = 'compRegParentId';
        form.insertBefore(hid, form.firstChild);
      }
      function toggleByCompDiv(compDiv) {
        var isMerchant = compDiv === 'MERCHANT';
        var isRegional = compDiv === 'REGIONAL';
        var isMasterDist = compDiv === 'MASTER_DIST';
        var isBranchAgencySales = compDiv === 'BRANCH' || compDiv === 'AGENCY' || compDiv === 'SALES_OFFICE';
        var isDistributor = isMasterDist || isBranchAgencySales;
        var showAccount = isMerchant || isDistributor || isRegional;
        pane.querySelectorAll('.merchant-only-section').forEach(function (card) {
          if (isMerchant) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.regional-only-section').forEach(function (card) {
          if (isRegional) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.master-dist-only-section').forEach(function (card) {
          if (isMasterDist) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.distributor-only-section').forEach(function (card) {
          if (isDistributor) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.distributor-merchant-no-regional-section').forEach(function (card) {
          if (isMerchant || isDistributor) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.distributor-or-merchant-section').forEach(function (card) {
          if (showAccount) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.branch-agency-sales-hide-section').forEach(function (card) {
          if (isBranchAgencySales) card.classList.add('d-none'); else card.classList.remove('d-none');
        });
        var isRegionalOrMasterDist = isRegional || isMasterDist;
        pane.querySelectorAll('.regional-or-master-dist-only-section').forEach(function (card) {
          if (isRegionalOrMasterDist) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.merchant-regional-master-commission-section').forEach(function (card) {
          if (isMerchant || isRegional || isMasterDist) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        var hint = pane.querySelector('.comp-div-hint');
        if (hint) hint.style.display = (!compDiv || compDiv === '') ? 'block' : 'none';
        if (isRegional && window.PG_HQ_HOLIDAY && typeof window.PG_HQ_HOLIDAY.init === 'function') {
          window.PG_HQ_HOLIDAY.init(pane);
        }
      }
      var compDivEl = pane.querySelector('#compRegForm [name="compDiv"]');
      if (compDivEl && !compDivEl._merchantToggleBound) {
        compDivEl._merchantToggleBound = true;
        try {
          var u = JSON.parse(sessionStorage.getItem('pg_admin_user') || '{}');
          if (u.orgLevel && u.orgLevel !== 'HEADQUARTERS') {
            var regOpt = compDivEl.querySelector('option[value="REGIONAL"]');
            if (regOpt) regOpt.remove();
          }
        } catch (e) {}
        compDivEl.addEventListener('change', function () {
          toggleByCompDiv(this.value);
        });
        // 업체등록 첫 진입 기본값은 '선택' 유지 (강제 총판 기본값 제거)
        toggleByCompDiv(compDivEl.value || '');
        initPgBindingList(pane);
        initRegionalCardLimitTable(pane);
        initRegionalTerminalTable(pane);
        var regForm = pane.querySelector('#compRegForm');
        initRegionalHolidayProfileSelector(pane, regForm, null);
        var mainBrowse = pane.querySelector('#brandingMainImageBrowse');
        var logoBrowse = pane.querySelector('#brandingLogoImageBrowse');
        if (mainBrowse) mainBrowse.addEventListener('click', function () { var f = pane.querySelector('#brandingMainImageFile'); if (f) f.click(); });
        if (logoBrowse) logoBrowse.addEventListener('click', function () { var f = pane.querySelector('#brandingLogoImageFile'); if (f) f.click(); });
        [pane.querySelector('#brandingMainImageFile'), pane.querySelector('#brandingLogoImageFile')].forEach(function (inp) {
          if (inp) inp.addEventListener('change', function () {
            var urlInp = this.id === 'brandingMainImageFile' ? pane.querySelector('#brandingMainImageUrl') : pane.querySelector('#brandingLogoImageUrl');
            if (urlInp && this.files && this.files[0]) urlInp.value = this.files[0].name;
          });
        });
      }
      var commissionFollowEl = pane.querySelector('[name="commissionFollowHq"]');
      if (commissionFollowEl && !commissionFollowEl._commissionToggleBound) {
        commissionFollowEl._commissionToggleBound = true;
        function toggleCommissionCustom(useHq) {
          pane.querySelectorAll('.commission-custom-only').forEach(function (el) {
            el.style.display = useHq === 'Y' ? 'none' : '';
          });
          pane.querySelectorAll('.hq-policy-only').forEach(function (el) {
            el.style.display = useHq === 'Y' ? '' : 'none';
          });
        }
        commissionFollowEl.addEventListener('change', function () { toggleCommissionCustom(this.value); });
        toggleCommissionCustom(commissionFollowEl.value || 'Y');
      }
      var hqPolicySel = pane.querySelector('#compRegForm [name="hqPolicyScope"]') || pane.querySelector('[name="hqPolicyScope"]');
      var baseCurEl = pane.querySelector('#compRegForm [name="baseCurrency"]');
      if (hqPolicySel && !hqPolicySel._hqPolicyRegBound) {
        hqPolicySel._hqPolicyRegBound = true;
        function refreshHqPolicyReg(hqd) {
          var list = (hqd && hqd.templates) ? hqd.templates : [];
          pane._hqCommissionTemplatesCache = list;
          var bc = baseCurEl ? baseCurEl.value : '';
          var filt = pgFilterDeployedTemplatesForMerchant(list, bc);
          var prev = hqPolicySel.value;
          hqPolicySel.innerHTML = pgHqPolicyScopeOptionsHtml(filt);
          if (prev) {
            var ok = false;
            var j;
            for (j = 0; j < hqPolicySel.options.length; j++) {
              if (hqPolicySel.options[j].value === prev) {
                ok = true;
                break;
              }
            }
            if (ok) hqPolicySel.value = prev;
          }
        }
        window.PG_API.hqDefaultCommission().then(refreshHqPolicyReg).catch(function () {});
        if (baseCurEl && !baseCurEl._hqPolicyBaseBoundReg) {
          baseCurEl._hqPolicyBaseBoundReg = true;
          baseCurEl.addEventListener('change', function () {
            if (pane._hqCommissionTemplatesCache) {
              refreshHqPolicyReg({ templates: pane._hqCommissionTemplatesCache });
            } else {
              window.PG_API.hqDefaultCommission().then(refreshHqPolicyReg).catch(function () {});
            }
          });
        }
      }
      fillChargebackPolicySelectsInRoot(pane, null);
      var holdRateFollowEl = pane.querySelector('[name="holdRateFollowHq"]');
      if (holdRateFollowEl && !holdRateFollowEl._holdRateToggleBound) {
        holdRateFollowEl._holdRateToggleBound = true;
        function toggleHoldRateCustom(useHq) {
          pane.querySelectorAll('.hold-rate-custom-only').forEach(function (el) {
            el.style.display = useHq === 'Y' ? 'none' : '';
          });
        }
        holdRateFollowEl.addEventListener('change', function () { toggleHoldRateCustom(this.value); });
        toggleHoldRateCustom(holdRateFollowEl.value || 'Y');
      }
      bindParentCompSearchModal(pane);
    }
    function applyCompInfoHeadquartersVisibility(form, compDiv) {
      if (!form) return;
      var hide = compDiv === 'HEADQUARTERS';
      form.querySelectorAll('.comp-info-hide-if-hq').forEach(function (el) {
        el.style.display = hide ? 'none' : '';
      });
    }
    /** 업체정보조회/업체정보 상세 첫 카드 제목: 조직 구분에 맞게 (가맹점 → 가맹점 정보 상세) */
    function compDivToInfoDetailTitle(compDiv) {
      var m = {
        HEADQUARTERS: '총본사 정보 상세',
        REGIONAL: '본사 정보 상세',
        MASTER_DIST: '총판 정보 상세',
        BRANCH: '지사 정보 상세',
        AGENCY: '대리점 정보 상세',
        SALES_OFFICE: '영업점 정보 상세',
        MERCHANT: '가맹점 정보 상세'
      };
      return m[compDiv] || '업체 정보 상세';
    }
    /** 업체정보조회·내업체: 본인 소속 업체 상세는 조회만 (ADMIN 제외) */
    function applyReadOnlyCompInfoDetailIfOwn(pane, data, formUrl) {
      if (!pane || !data || (formUrl !== '/comp/myCompMng' && formUrl !== '/comp/compInfo')) return;
      var form = pane.querySelector('#compInfoDetailForm');
      if (!form) return;
      var u = getSessionUser();
      if (u && String(u.role || '').toUpperCase() === 'ADMIN') return;
      var mine = String((u && u.compId) || '').trim();
      var cid = String(data.compId || '').trim();
      if (!mine || !cid || mine !== cid) return;
      form.querySelectorAll('input, select, textarea').forEach(function (el) {
        if (el.name === 'compId') return;
        el.disabled = true;
      });
      var updBtn = pane.querySelector('#compInfoUpdateBtn');
      if (updBtn) updBtn.style.display = 'none';
    }
    function applyCompInfoDetailMainCardHeader(pane, formUrl, compDiv) {
      if (!pane || (formUrl !== '/comp/myCompMng' && formUrl !== '/comp/compInfo')) return;
      var form = pane.querySelector('#compInfoDetailForm');
      if (!form) return;
      var header = form.querySelector('.card:first-child .card-header');
      if (!header) return;
      var base = compDivToInfoDetailTitle(compDiv);
      header.textContent = formUrl === '/comp/compInfo' ? (base + ' (업체정보조회)') : base;
      var notice = form.querySelector('.card:first-child .card-body > p.text-muted.small.mb-2');
      if (!notice) return;
      if (formUrl === '/comp/myCompMng') {
        if (compDiv === 'MERCHANT') {
          notice.textContent = '로그인한 계정에 연결된 가맹점 정보가 자동으로 표시됩니다. 아래에서 조회·수정합니다.';
        } else {
          notice.textContent = '로그인한 계정에 연결된 소속 업체 정보가 자동으로 표시됩니다. 아래에서 조회·수정합니다.';
        }
      } else if (formUrl === '/comp/compInfo') {
        if (compDiv === 'MERCHANT') {
          notice.textContent = '선택한 가맹점의 정보입니다. 그리드에서 한 건 선택 후 [상세] 버튼으로 조회·수정합니다.';
        } else {
          notice.textContent = '상위 본사(우리)가 권한을 준 회사의 정보입니다. 그리드에서 한 건 선택 후 [상세] 버튼으로 조회·수정합니다.';
        }
      }
    }
    /* 상위본사 표시값 정규화: "무언가 (이름)"이면 괄호 안 이름만 노출 */
    function normalizeParentCompDisplay(v) {
      var s = (v == null) ? '' : String(v).trim();
      if (!s) return '';
      var m = s.match(/\(([^()]*)\)\s*$/);
      if (m && m[1] != null && String(m[1]).trim() !== '') return String(m[1]).trim();
      return s;
    }
    /** 업체정보 상세: 본사·총판은 상위 변경 불가. 총판 산하는 동일 총판 하위만 상위 검색 목록에 표시 */
    function ensureChargebackPolicyPreviewHost(sel) {
      if (!sel) return null;
      var host = sel.parentElement ? sel.parentElement.querySelector('.pg-chargeback-policy-preview') : null;
      if (host) return host;
      host = document.createElement('div');
      host.className = 'pg-chargeback-policy-preview small text-muted mt-1';
      host.style.whiteSpace = 'pre-line';
      if (sel.parentElement) sel.parentElement.appendChild(host);
      return host;
    }
    function renderChargebackPolicyStructure(host, detail) {
      if (!host) return;
      var tiers = detail && Array.isArray(detail.tiers) ? detail.tiers : [];
      if (!tiers.length) {
        host.textContent = '';
        return;
      }
      var lines = tiers.map(function (t, i) {
        var min = t && t.countMin != null ? String(t.countMin) : '0';
        var max = (t && t.countMax != null && String(t.countMax) !== '') ? String(t.countMax) : '이상';
        var fee = t && t.feePerCase != null ? pgFmtOneDecimalStripWhole(t.feePerCase) : '0';
        return (i + 1) + ') ' + min + ' ~ ' + max + '건 : ' + fee;
      });
      host.textContent = '차지백 구간정책 구조\n' + lines.join('\n');
    }
    function bindChargebackPolicyStructurePreview(sel) {
      if (!sel || sel._cbStructureBound) return;
      sel._cbStructureBound = true;
      var host = ensureChargebackPolicyPreviewHost(sel);
      function refresh() {
        var v = sel.value != null ? String(sel.value).trim() : '';
        if (!v) {
          renderChargebackPolicyStructure(host, null);
          return;
        }
        window.PG_API.hqChargebackPolicyDetail(v).then(function (detail) {
          renderChargebackPolicyStructure(host, detail || {});
        }).catch(function () {
          renderChargebackPolicyStructure(host, null);
        });
      }
      sel.addEventListener('change', refresh);
      refresh();
    }
    function fillChargebackPolicySelectsInRoot(root, selectedId) {
      if (!root || !window.PG_API || !window.PG_API.hqChargebackPolicyList) return Promise.resolve();
      var sels = root.querySelectorAll('select[name="chargebackPolicyId"]');
      if (!sels.length) return Promise.resolve();
      var selStr = selectedId != null && String(selectedId) !== '' ? String(selectedId) : '';
      return window.PG_API.hqChargebackPolicyList().then(function (list) {
        var opts = '<option value="">(미사용) 건당 차지백만</option>';
        (list || []).forEach(function (row) {
          var id = row.id != null ? String(row.id) : '';
          if (!id) return;
          var nm = (row.name != null && String(row.name).trim()) ? String(row.name).trim() : id;
          opts += '<option value="' + id + '">' + nm.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;') + '</option>';
        });
        sels.forEach(function (sel) {
          sel.innerHTML = opts;
          if (selStr) sel.value = selStr;
          bindChargebackPolicyStructurePreview(sel);
        });
      }).catch(function () {});
    }
    function applyCompParentMoveRules(form, data) {
      if (!form) return;
      var locked = !!(data && data.parentOrgChangeLocked);
      var pc = form.querySelector('input[name="parentComp"]');
      var btn = form.querySelector('button[data-field="parentComp"][data-action="검색"]');
      if (pc) {
        pc.readOnly = locked;
        pc.title = locked ? '본사·총판은 상위 조직을 변경할 수 없습니다.' : '';
      }
      if (btn) {
        btn.disabled = locked;
        btn.title = locked ? '본사·총판은 상위 조직을 변경할 수 없습니다.' : '';
      }
      if (data && data.masterDistScopeOrgId != null && String(data.masterDistScopeOrgId) !== '') {
        form.dataset.masterDistScopeOrgId = String(data.masterDistScopeOrgId);
      } else {
        delete form.dataset.masterDistScopeOrgId;
      }
    }
    function filterParentCompCandidates(list, form) {
      if (!list || !form) return list || [];
      if (form.id !== 'compDetailForm') return list;
      var scope = form.dataset.masterDistScopeOrgId;
      if (!scope) return list;
      return list.filter(function (row) {
        var rid = row.id != null ? String(row.id) : '';
        var rscope = row.masterDistScopeOrgId != null ? String(row.masterDistScopeOrgId) : '';
        return rscope === scope || rid === scope;
      });
    }
    /** 상위로 부적합: 가맹점 행은 제외(가맹점의 상위는 영업점·대리점·지사·총판·본사·총본사 등). 총판 산하 한정은 filterParentCompCandidates 이후 적용. */
    function filterParentExcludeMerchantLeaf(list) {
      if (!list || !list.length) return list || [];
      return list.filter(function (row) {
        return String(row.compDiv || '').toUpperCase() !== 'MERCHANT';
      });
    }
    function resolveParentSearchForm(pane) {
      var reg = pane && pane.querySelector('#compRegForm');
      var det = pane && pane.querySelector('#compDetailForm');
      return reg || det;
    }
    /** 업체 등록·업체정보 상세 공통: 상위업체 검색 모달 */
    function bindParentCompSearchModal(pane) {
      var parentCompSearchBtn = pane.querySelector('button[data-field="parentComp"][data-action="검색"]');
      if (!parentCompSearchBtn || parentCompSearchBtn._parentModalBound) return;
      parentCompSearchBtn._parentModalBound = true;
      parentCompSearchBtn.addEventListener('click', function () {
        var modalEl = document.getElementById('parentCompSearchModal');
        if (!modalEl) return;
        var modal = window.bootstrap && bootstrap.Modal ? new bootstrap.Modal(modalEl) : null;
        if (modal) modal.show();
        var tbody = document.getElementById('parentCompSearchTbody');
        var kw = document.getElementById('parentCompSearchKeyword');
        function runSearch() {
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.compList({
            searchCompId: (kw && kw.value) || '',
            searchCompNm: (kw && kw.value) || '',
            searchUseYn: 'ALL',
            page: 1,
            size: 1000
          }).then(function (data) {
            var raw = (data && data.list) ? data.list : [];
            var activeForm = resolveParentSearchForm(pane);
            var list = filterParentExcludeMerchantLeaf(filterParentCompCandidates(raw, activeForm));
            if (!tbody) return;
            tbody.innerHTML = '';
            list.forEach(function (row) {
              var tr = document.createElement('tr');
              tr.style.cursor = 'pointer';
              tr.setAttribute('data-id', row.id != null ? row.id : '');
              tr.setAttribute('data-compId', row.compId != null ? row.compId : '');
              tr.setAttribute('data-compNm', row.compNm != null ? row.compNm : '');
              tr.innerHTML = '<td><button type="button" class="btn btn-sm btn-outline-primary">선택</button></td><td>' + (row.compId || '') + '</td><td>' + (row.compNm || '') + '</td><td>' + (row.compDivNm || row.compDiv || '') + '</td>';
              tr.addEventListener('click', function (e) {
                if (e.target.tagName === 'BUTTON') return;
                selectParentComp(tr);
              });
              tr.querySelector('button').addEventListener('click', function () { selectParentComp(tr); });
              tbody.appendChild(tr);
            });
            if (list.length === 0) tbody.innerHTML = '<tr><td colspan="4" class="text-muted text-center">' + (raw.length ? '동일 총판 산하로 선택 가능한 업체만 표시됩니다. 조건에 맞는 업체가 없습니다.' : '조회된 업체가 없습니다.') + '</td></tr>';
          }).catch(function (err) {
            if (tbody) tbody.innerHTML = '<tr><td colspan="4" class="text-danger text-center">' + (err && err.message ? err.message : '조회 실패') + '</td></tr>';
          }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        }
        function selectParentComp(tr) {
          var id = tr.getAttribute('data-id');
          var compId = tr.getAttribute('data-compId');
          var compNm = tr.getAttribute('data-compNm');
          var val = (compNm && String(compNm).trim()) ? String(compNm).trim() : (compId || '');
          [pane.querySelector('#compRegForm'), pane.querySelector('#compDetailForm')].forEach(function (f) {
            if (f) {
              var pid = f.querySelector('input[name="parentId"]');
              if (pid) pid.value = id || '';
              else {
                var hid = document.createElement('input');
                hid.type = 'hidden';
                hid.name = 'parentId';
                hid.value = id || '';
                f.appendChild(hid);
              }
              var pc = f.querySelector('input[name="parentComp"]');
              if (pc) pc.value = val;
            }
          });
          if (modal) modal.hide();
        }
        var modalSearchBtn = document.getElementById('parentCompSearchBtn');
        if (modalSearchBtn && !modalSearchBtn._parentCompBound) {
          modalSearchBtn._parentCompBound = true;
          modalSearchBtn.addEventListener('click', function () {
            var tbody2 = document.getElementById('parentCompSearchTbody');
            var dimm = document.getElementById('dimm');
            var kw2 = document.getElementById('parentCompSearchKeyword');
            if (!tbody2) return;
            if (dimm) dimm.style.display = 'flex';
            window.PG_API.compList({
              searchCompId: (kw2 && kw2.value) || '',
              searchCompNm: (kw2 && kw2.value) || '',
              searchUseYn: 'ALL',
              page: 1,
              size: 1000
            }).then(function (data) {
              var raw = (data && data.list) ? data.list : [];
              var activeForm = resolveParentSearchForm(pane) || document.querySelector('#compRegForm') || document.querySelector('#compDetailForm');
              var list = filterParentExcludeMerchantLeaf(filterParentCompCandidates(raw, activeForm));
              tbody2.innerHTML = '';
              list.forEach(function (row) {
                var tr = document.createElement('tr');
                tr.style.cursor = 'pointer';
                tr.setAttribute('data-id', row.id != null ? row.id : '');
                tr.setAttribute('data-compId', row.compId != null ? row.compId : '');
                tr.setAttribute('data-compNm', row.compNm != null ? row.compNm : '');
                tr.innerHTML = '<td><button type="button" class="btn btn-sm btn-outline-primary">선택</button></td><td>' + (row.compId || '') + '</td><td>' + (row.compNm || '') + '</td><td>' + (row.compDivNm || row.compDiv || '') + '</td>';
                tr.addEventListener('click', function (e) {
                  if (e.target.tagName === 'BUTTON') return;
                  var id = tr.getAttribute('data-id') || '';
                  var compNm0 = tr.getAttribute('data-compNm') || '';
                  var compId0 = tr.getAttribute('data-compId') || '';
                  var val = compNm0 ? String(compNm0).trim() : compId0;
                  ['#compRegForm', '#compDetailForm'].forEach(function (formId) {
                    var form = document.querySelector(formId);
                    if (form) {
                      var pid = form.querySelector('input[name="parentId"]');
                      if (pid) pid.value = id;
                      else { var hid = document.createElement('input'); hid.type = 'hidden'; hid.name = 'parentId'; hid.value = id; form.appendChild(hid); }
                      var pc = form.querySelector('input[name="parentComp"]');
                      if (pc) pc.value = val;
                    }
                  });
                  if (modalEl && window.bootstrap && bootstrap.Modal) { var m = bootstrap.Modal.getInstance(modalEl); if (m) m.hide(); }
                });
                tr.querySelector('button').addEventListener('click', function () {
                  var id = tr.getAttribute('data-id') || '';
                  var compNm0 = tr.getAttribute('data-compNm') || '';
                  var compId0 = tr.getAttribute('data-compId') || '';
                  var val = compNm0 ? String(compNm0).trim() : compId0;
                  ['#compRegForm', '#compDetailForm'].forEach(function (formId) {
                    var form = document.querySelector(formId);
                    if (form) {
                      var pid = form.querySelector('input[name="parentId"]');
                      if (pid) pid.value = id;
                      else { var hid = document.createElement('input'); hid.type = 'hidden'; hid.name = 'parentId'; hid.value = id; form.appendChild(hid); }
                      var pc = form.querySelector('input[name="parentComp"]');
                      if (pc) pc.value = val;
                    }
                  });
                  if (modalEl && window.bootstrap && bootstrap.Modal) { var m = bootstrap.Modal.getInstance(modalEl); if (m) m.hide(); }
                });
                tbody2.appendChild(tr);
              });
              if (list.length === 0) tbody2.innerHTML = '<tr><td colspan="4" class="text-muted text-center">' + (raw.length ? '동일 총판 산하로 선택 가능한 업체만 표시됩니다. 조건에 맞는 업체가 없습니다.' : '조회된 업체가 없습니다.') + '</td></tr>';
            }).catch(function (err) {
              tbody2.innerHTML = '<tr><td colspan="4" class="text-danger text-center">' + (err && err.message ? err.message : '조회 실패') + '</td></tr>';
            }).finally(function () { if (dimm) dimm.style.display = 'none'; });
          });
        }
        runSearch();
      });
    }
    function applyMyCompBrandingPermission(pane, form, compDiv, brandingEditAllowedYn) {
      if (!pane || !form) return;
      var isHeadquarters = compDiv === 'HEADQUARTERS';
      var allowedComp = (isHeadquarters || compDiv === 'REGIONAL' || compDiv === 'MASTER_DIST');
      var merchantBranding = compDiv === 'MERCHANT' && String(brandingEditAllowedYn || '').toUpperCase() === 'Y';
      var allowed = isHeadquarters || (allowedComp && String(brandingEditAllowedYn || '').toUpperCase() === 'Y') || merchantBranding;
      var brandingCard = pane.querySelector('#brandingCard');
      if (brandingCard) {
        if (compDiv === 'MERCHANT') {
          brandingCard.style.display = merchantBranding ? '' : 'none';
        } else {
          brandingCard.style.display = allowedComp ? '' : 'none';
        }
      }
      var mainBrowse = pane.querySelector('#brandingMainImageBrowse');
      var logoBrowse = pane.querySelector('#brandingLogoImageBrowse');
      var mainFile = pane.querySelector('#brandingMainImageFile');
      var logoFile = pane.querySelector('#brandingLogoImageFile');
      var themeSel = pane.querySelector('#brandingTheme');
      [mainBrowse, logoBrowse].forEach(function (btn) {
        if (!btn) return;
        btn.style.display = allowed ? '' : 'none';
        btn.disabled = !allowed;
      });
      [mainFile, logoFile, themeSel].forEach(function (el) {
        if (!el) return;
        el.disabled = !allowed;
      });
    }
    function resetPgBindingPaneForReload(pane) {
      if (!pane) return;
      var tb = pane.querySelector('#pgBindingTbody');
      if (tb) tb.innerHTML = '';
      var btn = pane.querySelector('#pgBindingAddBtn');
      if (btn && btn.parentNode) {
        var nb = btn.cloneNode(true);
        btn.parentNode.replaceChild(nb, btn);
      }
    }
    /** 업체정보조회(#compInfoDetailForm) 카드 표시 — compDetail toggleByCompDiv 와 동일 규칙(필요 분만) */
    function applyCompInfoPaneByCompDiv(pane, compDiv) {
      if (!pane || !compDiv) return;
      var isMerchant = compDiv === 'MERCHANT';
      var isRegional = compDiv === 'REGIONAL';
      var isMasterDist = compDiv === 'MASTER_DIST';
      var isBranchAgencySales = compDiv === 'BRANCH' || compDiv === 'AGENCY' || compDiv === 'SALES_OFFICE';
      var isDistributor = isMasterDist || isBranchAgencySales;
      var showAccount = isMerchant || isDistributor || isRegional;
      pane.querySelectorAll('.merchant-only-section').forEach(function (card) {
        if (isMerchant) card.classList.remove('d-none'); else card.classList.add('d-none');
      });
      pane.querySelectorAll('.distributor-merchant-no-regional-section').forEach(function (card) {
        if (isMerchant || isDistributor) card.classList.remove('d-none'); else card.classList.add('d-none');
      });
      pane.querySelectorAll('.distributor-or-merchant-section').forEach(function (card) {
        if (showAccount) card.classList.remove('d-none'); else card.classList.add('d-none');
      });
      pane.querySelectorAll('.branch-agency-sales-hide-section').forEach(function (card) {
        if (isBranchAgencySales) card.classList.add('d-none'); else card.classList.remove('d-none');
      });
      pane.querySelectorAll('.merchant-regional-master-commission-section').forEach(function (card) {
        if (isMerchant || isRegional || isMasterDist) card.classList.remove('d-none'); else card.classList.add('d-none');
      });
    }
    /** 가맹점 계정정보(대표/보조 아이디·비밀번호) 노출: 총본사/본사/총판만 허용 */
    function applyMerchantAccountFieldVisibility(form, targetCompDiv) {
      if (!form) return;
      var isMerchantTarget = String(targetCompDiv || '').toUpperCase() === 'MERCHANT';
      var user = getSessionUser();
      var viewerLevel = String((user && user.orgLevel) || '').toUpperCase();
      var allowedViewer = (viewerLevel === 'HEADQUARTERS' || viewerLevel === 'REGIONAL' || viewerLevel === 'MASTER_DIST');
      var hide = isMerchantTarget && !allowedViewer;

      var blocks = [];
      var candidates = [
        form.querySelector('[name="loginId"]'),
        form.querySelector('#compDetailPwdResetBtn'),
        form.querySelector('[name="assistantLoginId"]'),
        form.querySelector('#assistantPwd'),
        form.querySelector('#assistantPwdResetBtn')
      ];
      candidates.forEach(function (el) {
        if (!el || !el.closest) return;
        var block = el.closest('.form-field-block');
        if (block && blocks.indexOf(block) < 0) blocks.push(block);
      });
      blocks.forEach(function (b) { b.style.display = hide ? 'none' : ''; });
    }
    /** 가맹점 내 업체: 보조 비밀번호 — 최초 저장 전 입력+옆[저장], 이후 비밀번호 초기화만 */
    function applyAssistantPwdUi(form, data) {
      if (!form || !data) return;
      var initialRow = form.querySelector('#assistantPwdInitialRow');
      var resetRow = form.querySelector('#assistantPwdResetRow');
      var pwdInput = form.querySelector('[name="assistantPwd"]');
      if (!initialRow || !resetRow) return;
      if (data.assistantPwdSetYn === 'Y') {
        initialRow.classList.add('d-none');
        resetRow.classList.remove('d-none');
        if (pwdInput) pwdInput.value = '';
      } else {
        initialRow.classList.remove('d-none');
        resetRow.classList.add('d-none');
        if (pwdInput) pwdInput.value = '';
      }
      form.removeAttribute('data-assistant-pwd-confirmed');
    }
    function loadCompDetailIntoForm(pane, compId) {
      if (!compId) return;
      var dimm = document.getElementById('dimm');
      if (dimm) dimm.style.display = 'flex';
      window.PG_API.compDetail(compId).then(function (data) {
        var form = pane.querySelector('#compInfoDetailForm');
        if (!form || !data) return;
        var formUrl = pane.getAttribute('formurl') || '';
        form.querySelectorAll('input, select, textarea').forEach(function (el) { el.disabled = false; });
        var updBtnReset = pane.querySelector('#compInfoUpdateBtn');
        if (updBtnReset) updBtnReset.style.display = '';
        var allFieldsInfo = ['compId', 'parentComp', 'compNm', 'compDiv', 'regNo', 'bizType', 'industry', 'bizNature', 'product', 'homepage', 'settleName', 'settleTelNo', 'ceoNm', 'ceoMobile', 'compTel', 'fax', 'zipCode', 'addr', 'addrDetail', 'addrEtc', 'addrCountryCd', 'addrCountryCdOther', 'email', 'siteUrl', 'siteSummary', 'useYn', 'loginId', 'bankCd', 'transferFee', 'cryptoTransferFee', 'accountNo', 'accountHolder', 'commissionConfigAllowed', 'webPaymentUseYn', 'baseCurrency', 'remark', 'countryCd', 'countryCdOther', 'swift', 'branchName', 'branchAddr', 'contactTel', 'walletAddress', 'networkName', 'withdrawRestrictType', 'withdrawStartTime', 'withdrawEndTime', 'payLimitDefault', 'payLimitExtra', 'payLimitAlertSms', 'holdRateFollowHq', 'holdRate', 'holdDays', 'commissionFollowHq', 'hqPolicyScope', 'failFee', 'usageRate', 'payRate', 'cancelRate', 'voidFeePerTx', 'manualVoidFeePerTx', 'refundRate', 'commissionMemo', 'feeSettlementPerTx', 'feeUsdt', 'feeFx', 'fee3dsRate', 'chargebackFeePerTx', 'calcCycle', 'calcProcType', 'calcCloseTime', 'transferType', 'transferCycleDays', 'autoTransferMin', 'calcMinAmt', 'transferExecTime', 'calcExcludeYn', 'calcExcludeTarget', 'calcStartTime', 'payHoldYn', 'defaultProductName', 'defaultProductCode', 'defaultProductAmount', 'defaultProductDesc', 'notifyUrlBackground', 'notifyUrlResult', 'assistantLoginId', 'assistantPwd', 'assistantRoleType', 'brandingEditAllowedYn'];
        allFieldsInfo.forEach(function (k) {
          var el = form.querySelector('[name="' + k + '"]');
          if (el && data[k] != null) {
            var nf = pgFmtCompDetailNumericField(k, data[k]);
            el.value = nf != null ? nf : data[k];
          }
        });
        var parentCompEl0 = form.querySelector('[name="parentComp"]');
        if (parentCompEl0) parentCompEl0.value = normalizeParentCompDisplay(data.parentComp != null ? data.parentComp : parentCompEl0.value);
        var pidEl = form.querySelector('input[name="parentId"]');
        if (!pidEl) {
          pidEl = document.createElement('input');
          pidEl.type = 'hidden';
          pidEl.name = 'parentId';
          form.appendChild(pidEl);
        }
        if (data.parentId != null) pidEl.value = String(data.parentId);
        applyCompParentMoveRules(form, data);
        form.setAttribute('data-login-id-checked', data.loginId ? String(data.loginId).trim() : '');
        form.setAttribute('data-assistant-login-id-checked', data.assistantLoginId ? String(data.assistantLoginId).trim() : '');
        initIntlPhoneFields(form);
        if ((data.compDiv === 'REGIONAL' || data.compDiv === 'MASTER_DIST') && data.baseCurrency) {
          var parts = data.compDiv === 'REGIONAL' ? String(data.baseCurrency).split(/,\s*/) : [data.baseCurrency, '', ''];
          ['baseCurrency1', 'baseCurrency2', 'baseCurrency3'].forEach(function (n, i) {
            var el = form.querySelector('[name="' + n + '"]');
            if (el) el.value = parts[i] || '';
          });
        }
        var cc = data.countryCd;
        if (cc && cc !== 'JP' && cc !== 'KR' && cc !== 'TH') {
          var countrySel = form.querySelector('select[name="countryCd"]');
          var countryOther = form.querySelector('[name="countryCdOther"]');
          var bankText = form.querySelector('input[name="bankCdText"]');
          if (countrySel) { countrySel.value = 'OTHER'; countrySel.dispatchEvent(new Event('change')); }
          if (countryOther) countryOther.value = cc;
          if (bankText && data.bankCd != null) bankText.value = data.bankCd;
        } else if (cc === 'JP' || cc === 'KR' || cc === 'TH') {
          refreshCountryBankAfterFill(pane, data.bankCd);
        }
        var acc = data.addrCountryCd;
        if (acc && acc !== 'JP' && acc !== 'KR' && acc !== 'TH') {
          var addrCountrySel = form.querySelector('select[name="addrCountryCd"]');
          var addrCountryOther = form.querySelector('[name="addrCountryCdOther"]');
          if (addrCountrySel) { addrCountrySel.value = 'OTHER'; addrCountrySel.dispatchEvent(new Event('change')); }
          if (addrCountryOther) addrCountryOther.value = acc;
        }
        var rn = data.regNo;
        if (rn && rn.indexOf('|') >= 0) {
          var p = rn.split('|');
          var rt = form.querySelector('[name="regType"]');
          var rnEl = form.querySelector('[name="regNo"]');
          if (rt) rt.value = (p[0] === 'PERSONAL' || p[0] === 'CORP') ? p[0] : 'CORP';
          if (rnEl) rnEl.value = p.length > 1 ? p.slice(1).join('|') : '';
        }
        applyCompInfoPaneByCompDiv(pane, data.compDiv || '');
        if (formUrl === '/comp/myCompMng' || formUrl === '/comp/compInfo') {
          applyMerchantAccountFieldVisibility(form, data.compDiv || '');
        }
        if (formUrl === '/comp/myCompMng' || formUrl === '/comp/compInfo') {
          var cdInfo = data.compDiv || '';
          if (cdInfo === 'MERCHANT') {
            resetPgBindingPaneForReload(pane);
            initPgBindingList(pane, data.pgBindings, {
              rowActionMode: true,
              getCompId: function () {
                var el = pane.querySelector('#compInfoDetailForm [name="compId"]');
                return el && el.value ? el.value.trim() : '';
              }
            });
          }
          if (cdInfo === 'MERCHANT' || cdInfo === 'REGIONAL' || cdInfo === 'MASTER_DIST') {
            var commissionFollowEl = pane.querySelector('#compInfoDetailForm [name="commissionFollowHq"]');
            if (commissionFollowEl && !commissionFollowEl._infoToggleBound) {
              commissionFollowEl._infoToggleBound = true;
              function toggleCommissionCustom(useHq) {
                pane.querySelectorAll('#compInfoDetailForm .commission-custom-only').forEach(function (el) {
                  el.style.display = useHq === 'Y' ? 'none' : '';
                });
                pane.querySelectorAll('#compInfoDetailForm .hq-policy-only').forEach(function (el) {
                  el.style.display = useHq === 'Y' ? '' : 'none';
                });
              }
              commissionFollowEl.addEventListener('change', function () { toggleCommissionCustom(this.value); });
              toggleCommissionCustom(commissionFollowEl.value || 'Y');
            }
            var hqPolicySel = pane.querySelector('#compInfoDetailForm [name="hqPolicyScope"]');
            var baseCurInfo = pane.querySelector('#compInfoDetailForm [name="baseCurrency"]');
            if (hqPolicySel && !hqPolicySel._hqPolicyLoadedInfo) {
              hqPolicySel._hqPolicyLoadedInfo = true;
              function refreshHqPolicyInfo(hqd) {
                var list = (hqd && hqd.templates) ? hqd.templates : [];
                pane._hqCommissionTemplatesCacheInfo = list;
                var bc = (data && data.baseCurrency) ? data.baseCurrency : (baseCurInfo ? baseCurInfo.value : '');
                var filt = pgFilterDeployedTemplatesForMerchant(list, bc);
                var prev = (data && data.hqPolicyScope) ? data.hqPolicyScope : hqPolicySel.value;
                hqPolicySel.innerHTML = pgHqPolicyScopeOptionsHtml(filt);
                if (prev) {
                  var ok2 = false;
                  var ji;
                  for (ji = 0; ji < hqPolicySel.options.length; ji++) {
                    if (hqPolicySel.options[ji].value === prev) {
                      ok2 = true;
                      break;
                    }
                  }
                  if (ok2) hqPolicySel.value = prev;
                }
              }
              window.PG_API.hqDefaultCommission().then(refreshHqPolicyInfo).catch(function () {});
              if (baseCurInfo && !baseCurInfo._hqPolicyBaseBoundInfo) {
                baseCurInfo._hqPolicyBaseBoundInfo = true;
                baseCurInfo.addEventListener('change', function () {
                  if (pane._hqCommissionTemplatesCacheInfo) {
                    var list = pane._hqCommissionTemplatesCacheInfo;
                    var bc = baseCurInfo.value;
                    var filt = pgFilterDeployedTemplatesForMerchant(list, bc);
                    var prev = hqPolicySel.value;
                    hqPolicySel.innerHTML = pgHqPolicyScopeOptionsHtml(filt);
                    if (prev) {
                      var ok3 = false;
                      var k;
                      for (k = 0; k < hqPolicySel.options.length; k++) {
                        if (hqPolicySel.options[k].value === prev) {
                          ok3 = true;
                          break;
                        }
                      }
                      if (ok3) hqPolicySel.value = prev;
                    }
                  } else {
                    window.PG_API.hqDefaultCommission().then(function (hqd) {
                      pane._hqCommissionTemplatesCacheInfo = (hqd && hqd.templates) ? hqd.templates : [];
                      var filt2 = pgFilterDeployedTemplatesForMerchant(pane._hqCommissionTemplatesCacheInfo, baseCurInfo.value);
                      hqPolicySel.innerHTML = pgHqPolicyScopeOptionsHtml(filt2);
                    }).catch(function () {});
                  }
                });
              }
            } else if (hqPolicySel && data.hqPolicyScope) {
              hqPolicySel.value = data.hqPolicyScope;
            }
          }
          if (cdInfo === 'MERCHANT') {
            fillChargebackPolicySelectsInRoot(form, data.chargebackPolicyId);
          }
          if (cdInfo === 'MERCHANT') {
            var holdRateFollowEl = pane.querySelector('#compInfoDetailForm [name="holdRateFollowHq"]');
            if (holdRateFollowEl && !holdRateFollowEl._infoHoldToggleBound) {
              holdRateFollowEl._infoHoldToggleBound = true;
              function toggleHoldRateCustom(useHq) {
                pane.querySelectorAll('#compInfoDetailForm .hold-rate-custom-only').forEach(function (el) {
                  el.style.display = useHq === 'Y' ? 'none' : '';
                });
              }
              holdRateFollowEl.addEventListener('change', function () { toggleHoldRateCustom(this.value); });
              toggleHoldRateCustom(holdRateFollowEl.value || 'Y');
            }
            initBankByCountry(pane);
            initCountryBankGroup(pane);
            initCountryAddressGroup(pane);
            if (String(data.brandingEditAllowedYn || '').toUpperCase() === 'Y' && window.PG_API.orgBranding) {
              window.PG_API.orgBranding(compId).then(function (b) {
                pgBrandingFillImageDisplayFromFetch(pane, b);
                var themeSel = pane.querySelector('#brandingTheme');
                var hostEl = pane.querySelector('#brandingBrandHost');
                if (themeSel && b.theme) themeSel.value = b.theme || 'DEFAULT';
                if (hostEl) hostEl.value = (b.brandHost != null && b.brandHost !== undefined) ? b.brandHost : '';
              }).catch(function () {});
            }
          }
        }
        var paymentInfoCard = pane.querySelector('#webPaymentCard') || pane.querySelector('#pgInfoCard');
        if (paymentInfoCard) {
          paymentInfoCard.style.display = (data.compDiv === 'MERCHANT') ? '' : 'none';
          var paymentUrlEl = pane.querySelector('#paymentUrlDisplay');
          if (paymentUrlEl && data.compId && data.compDiv === 'MERCHANT') {
            var base = (window.SITE_CONFIG && window.SITE_CONFIG.paymentBaseUrl) || (window.location.origin || '');
            paymentUrlEl.value = base.replace(/\/$/, '') + '/pay/' + encodeURIComponent(String(data.compId).trim());
          } else if (paymentUrlEl) paymentUrlEl.value = '';
        }
        pane._lastCompInfoDetailData = data;
        var compDivSel = form.querySelector('[name="compDiv"]');
        if (formUrl === '/comp/myCompMng' && compDivSel) {
          compDivSel.disabled = true;
        }
        if (formUrl === '/comp/myCompMng') {
          applyMyCompBrandingPermission(pane, form, data.compDiv || '', data.brandingEditAllowedYn || 'N');
        }
        if (formUrl === '/comp/myCompMng' || formUrl === '/comp/compInfo') {
          applyCompInfoDetailMainCardHeader(pane, formUrl, data.compDiv || '');
        }
        var copyBtn = pane.querySelector('#paymentUrlCopyBtn');
        if (copyBtn && !copyBtn._bound) {
          copyBtn._bound = true;
          copyBtn.addEventListener('click', function () {
            var inp = pane.querySelector('#paymentUrlDisplay');
            if (inp && inp.value) {
              navigator.clipboard.writeText(inp.value).then(function () { alert('복사되었습니다.'); }).catch(function () { alert('복사 실패'); });
            }
          });
        }
        var card = pane.querySelector('#compInfoDetailCard');
        if (card) card.scrollIntoView({ behavior: 'smooth', block: 'start' });
        applyCompInfoHeadquartersVisibility(form, data.compDiv || '');
        applyAssistantPwdUi(form, data);
      }).catch(function (e) { alert(e && e.message ? e.message : '상세 조회 실패'); }).finally(function () {
        var formUrlFinal = pane.getAttribute('formurl') || '';
        if (typeof window.applyPagePermissionToPane === 'function') {
          window.applyPagePermissionToPane(pane, formUrlFinal);
        }
        if ((formUrlFinal === '/comp/myCompMng' || formUrlFinal === '/comp/compInfo') && pane._lastCompInfoDetailData) {
          applyReadOnlyCompInfoDetailIfOwn(pane, pane._lastCompInfoDetailData, formUrlFinal);
        }
        if (dimm) dimm.style.display = 'none';
      });
    }
    if (url === '/comp/compMngTree' && !pane._compMngTreeDblclickBound) {
      pane._compMngTreeDblclickBound = true;
      pane.addEventListener('dblclick', function (e) {
        var tr = e.target && e.target.closest ? e.target.closest('tr') : null;
        if (!tr || !tr.closest('#grid_' + tabId)) return;
        if (tr.classList.contains('empty-state-cell') || !tr.getAttribute('data-row')) return;
        if (e.target && e.target.closest && e.target.closest('.tree-toggle')) return;
        var dataRow = tr.getAttribute('data-row');
        var compId = '';
        if (dataRow) {
          try {
            var row = JSON.parse(decodeURIComponent(dataRow));
            compId = row.compId || '';
          } catch (e) {}
        }
        if (compId) {
          try {
            sessionStorage.setItem('pg_comp_detail_compId', compId);
            sessionStorage.setItem('pg_comp_detail_compDiv', row.compDiv || '');
          } catch (e) {}
          fnTopMenuMove('/comp/compDetail', null, '업체정보');
        }
      });
    }
    if (url === '/comp/compInfo' || url === '/comp/myCompMng') {
      initCountryAddressGroup(pane);
      initIntlPhoneFields(pane);
      var compInfoDetailBtn = pane.querySelector('#compInfoDetailBtn');
      if (compInfoDetailBtn) {
        compInfoDetailBtn.addEventListener('click', function () {
          var grid = pane.querySelector('#grid_' + tabId + ' tbody');
          if (!grid) return;
          var checked = grid.querySelector('tr .grid-row-check:checked');
          if (!checked) { alert('그리드에서 한 건을 선택한 뒤 [상세] 버튼을 눌러주세요.'); return; }
          var tr = checked.closest('tr');
          var tds = tr.querySelectorAll('td');
          var cfg = window.PG_SCREENS && (window.PG_SCREENS.getMenuScreens()['/comp/compInfo'] || window.PG_SCREENS.getMenuScreens()['/comp/myCompMng']);
          var cols = (cfg && cfg.columns) || [];
          var compIdIdx = cols.findIndex(function (c) { return c.key === 'compId'; });
          if (compIdIdx < 0) compIdIdx = 1;
          var compId = (tds[compIdIdx] && tds[compIdIdx].textContent) ? tds[compIdIdx].textContent.trim() : '';
          if (!compId) { alert('업체코드를 찾을 수 없습니다.'); return; }
          loadCompDetailIntoForm(pane, compId);
        });
      }
      var compInfoUpdateBtn = pane.querySelector('#compInfoUpdateBtn');
      if (compInfoUpdateBtn) {
        compInfoUpdateBtn.addEventListener('click', function () {
          if (getPagePermissionForUrl(url) === 'OBSERVER') return;
          var form = pane.querySelector('#compInfoDetailForm');
          if (!form) return;
          initIntlPhoneFields(form);
          ['ceoMobile', 'compTel', 'fax', 'settleTelNo', 'contactTel'].forEach(function (n) { syncIntlPhoneHidden(form, n); });
          var compIdEl = form.querySelector('[name="compId"]');
          var compId = compIdEl && compIdEl.value ? compIdEl.value.trim() : '';
          if (!compId) { alert('업체코드가 없습니다. 먼저 [상세]로 조회하세요.'); return; }
          var fd = {};
          form.querySelectorAll('input, select, textarea').forEach(function (el) {
            if (el.name && el.type !== 'file' && el.name !== 'pgOperational') {
              if (el.name.indexOf('__phone_') === 0) return;
              fd[el.name] = el.value;
            }
          });
          if (fd.countryCd === 'OTHER') { fd.bankCd = fd.bankCdText || fd.bankCd; delete fd.bankCdText; }
          if (fd.addrCountryCd === 'OTHER') { fd.addrCountryCd = fd.addrCountryCdOther || ''; delete fd.addrCountryCdOther; }
          fd.compId = compId;
          if (url === '/comp/myCompMng') {
            var newLoginId = fd.loginId ? String(fd.loginId).trim() : '';
            var checkedLoginId = form.getAttribute('data-login-id-checked') || '';
            if (newLoginId && checkedLoginId !== newLoginId) {
              alert('대표 아이디 중복확인을 먼저 진행하세요.');
              return;
            }
            var newAssistantLoginId = fd.assistantLoginId ? String(fd.assistantLoginId).trim() : '';
            var checkedAssistantLoginId = form.getAttribute('data-assistant-login-id-checked') || '';
            if (newAssistantLoginId && checkedAssistantLoginId !== newAssistantLoginId) {
              alert('보조 아이디 중복확인을 먼저 진행하세요.');
              return;
            }
          }
          if (fd.regType != null) { fd.regNo = (fd.regType || 'CORP') + '|' + (fd.regNo || ''); delete fd.regType; }
          var compDivVal = fd.compDiv || '';
          if (compDivVal === 'REGIONAL') {
            var bc = [fd.baseCurrency1, fd.baseCurrency2, fd.baseCurrency3].filter(function (v) { return v && v.trim(); });
            fd.baseCurrency = bc.join(',');
          } else if (compDivVal === 'MASTER_DIST') {
            fd.baseCurrency = (fd.baseCurrency || '').trim();
          }
          if (compDivVal === 'REGIONAL' || compDivVal === 'MASTER_DIST') {
            delete fd.baseCurrency1;
            delete fd.baseCurrency2;
            delete fd.baseCurrency3;
          }
          if (compDivVal === 'MERCHANT') {
            var pgTbody = form.querySelector('#pgBindingTbody');
            if (pgTbody) {
              var operationalVal = form.querySelector('input[name="pgOperational"]:checked');
              var operationalIdx = operationalVal ? parseInt(operationalVal.value, 10) : 0;
              var bindings = [];
              pgTbody.querySelectorAll('tr').forEach(function (tr, i) {
                var sel = function (f) { var e = tr.querySelector('[data-field="' + f + '"]'); return e ? e.value : ''; };
                var pgCd = sel('pgCd');
                if (pgCd) {
                  bindings.push({
                    pgCd: pgCd,
                    activationYn: sel('activationYn') || 'Y',
                    operationalYn: i === operationalIdx ? 'Y' : 'N',
                    payMethod: sel('payMethod') || 'WEB',
                    mid: sel('mid'),
                    rootNo: sel('rootNo'),
                    apiKey: sel('apiKey'),
                    ivKey: sel('ivKey'),
                    installmentYn: sel('installmentYn') || 'N',
                    maxInstallmentMonths: sel('maxInstallmentMonths')
                  });
                }
              });
              fd.pgBindings = JSON.stringify(bindings);
            }
          }
          if (url === '/comp/myCompMng' && compDivVal === 'MERCHANT') {
            var astResetRow = form.querySelector('#assistantPwdResetRow');
            var inAstResetMode = astResetRow && !astResetRow.classList.contains('d-none');
            if (inAstResetMode) {
              delete fd.assistantPwd;
            } else {
              var apw0 = fd.assistantPwd ? String(fd.assistantPwd).trim() : '';
              if (apw0 && (form.getAttribute('data-assistant-pwd-confirmed') || '') !== 'Y') {
                alert('보조 비밀번호 입력 후 옆의 [저장]으로 확정하세요.');
                return;
              }
            }
          }
          if (!pgConfirmBeforeSave('저장하시겠습니까?')) return;
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.compUpdate(fd).then(function () {
            if (compDivVal === 'MERCHANT') {
              var settleFd = {};
              var settleKeys = ['withdrawRestrictType', 'withdrawStartTime', 'withdrawEndTime', 'payLimitDefault', 'payLimitExtra', 'holdRate', 'holdDays', 'calcCloseTime', 'calcStartTime', 'transferCycleDays', 'calcProcType', 'transferType', 'autoTransferMin', 'payHoldYn', 'calcExcludeYn', 'calcExcludeTarget', 'calcMinAmt', 'transferExecTime'];
              settleKeys.splice(5, 0, 'calcCycle');
              settleKeys.forEach(function (k) {
                if (fd[k] !== undefined && fd[k] !== null && fd[k] !== '') settleFd[k] = fd[k];
              });
              if (Object.keys(settleFd).length > 0) {
                return window.PG_API.settlementSettingSave(compId, settleFd);
              }
            }
            return Promise.resolve();
          }).then(function () {
            if (compDivVal !== 'MERCHANT' || String(fd.brandingEditAllowedYn || '').toUpperCase() !== 'Y') return Promise.resolve();
            if (!window.PG_API.orgBrandingUpload || !window.PG_API.orgBrandingSave) return Promise.resolve();
            var mainFile = form.querySelector('#brandingMainImageFile');
            var logoFile = form.querySelector('#brandingLogoImageFile');
            var themeEl = form.querySelector('#brandingTheme');
            var hostEl = form.querySelector('#brandingBrandHost');
            var chain = Promise.resolve();
            if (mainFile && mainFile.files && mainFile.files[0]) {
              var _myMainF = mainFile.files[0];
              chain = chain.then(function () {
                return window.PG_API.orgBrandingUpload(compId, 'main', _myMainF).then(function (data) {
                  pgBrandingSetImageDisplayInput(form, 'main', data, _myMainF);
                  return data;
                });
              });
            }
            if (logoFile && logoFile.files && logoFile.files[0]) {
              var _myLogoF = logoFile.files[0];
              chain = chain.then(function () {
                return window.PG_API.orgBrandingUpload(compId, 'logo', _myLogoF).then(function (data) {
                  pgBrandingSetImageDisplayInput(form, 'logo', data, _myLogoF);
                  return data;
                });
              });
            }
            if (themeEl || hostEl) {
              chain = chain.then(function () { return window.PG_API.orgBrandingSave(compId, (themeEl && themeEl.value) ? themeEl.value : 'DEFAULT', hostEl ? hostEl.value : undefined); });
            }
            return chain;
          }).then(function () {
            alert('저장되었습니다.');
            if (url === '/comp/myCompMng') {
              /* 가맹점 업체정보: 저장 후 같은 탭·같은 화면 유지(상세 재조회 시 스크롤/맥락이 바뀌는 문제 방지) */
            } else {
              doSearch(pane, tabId, 1);
            }
          }).catch(function (e) { alert(e && e.message ? e.message : '수정 저장 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        });
      }
      pane.addEventListener('click', function (e) {
        var dupBtn = e.target && e.target.closest ? e.target.closest('button[data-action="중복확인"]') : null;
        if (!dupBtn || !pane.contains(dupBtn)) return;
        var fld = dupBtn.getAttribute('data-field') || '';
        if (fld !== 'loginId' && fld !== 'assistantLoginId') return;
        var form = pane.querySelector('#compInfoDetailForm');
        var idEl = form ? form.querySelector('[name="' + fld + '"]') : null;
        var lid = idEl && idEl.value ? String(idEl.value).trim() : '';
        if (!lid) { alert('아이디를 입력하세요.'); return; }
        var dimmDup = document.getElementById('dimm');
        if (dimmDup) dimmDup.style.display = 'flex';
        window.PG_API.compCheckLoginId(lid).then(function (r) {
          var ok = !!(r && r.available);
          if (ok) {
            if (form) form.setAttribute(fld === 'loginId' ? 'data-login-id-checked' : 'data-assistant-login-id-checked', lid);
            alert('사용 가능한 로그인ID입니다.');
          } else {
            if (form) form.setAttribute(fld === 'loginId' ? 'data-login-id-checked' : 'data-assistant-login-id-checked', '');
            alert('이미 사용 중인 로그인ID입니다.');
          }
        }).catch(function (err) {
          if (form) form.setAttribute(fld === 'loginId' ? 'data-login-id-checked' : 'data-assistant-login-id-checked', '');
          alert(err && err.message ? err.message : '중복확인 실패');
        }).finally(function () { if (dimmDup) dimmDup.style.display = 'none'; });
      });
      pane.addEventListener('click', function (e) {
        var pwdResetBtn = e.target && e.target.closest ? e.target.closest('#compDetailPwdResetBtn, [data-action="비밀번호 초기화"]') : null;
        if (!pwdResetBtn || !pane.contains(pwdResetBtn)) return;
        var form = pane.querySelector('#compInfoDetailForm');
        if (!form || !form.contains(pwdResetBtn)) return;
        var compIdEl = form.querySelector('[name="compId"]');
        var compId = compIdEl && compIdEl.value ? compIdEl.value.trim() : '';
        if (!compId) { alert('업체코드가 없습니다.'); return; }
        if (!confirm('대표 계정 비밀번호를 초기화하시겠습니까? (임시: 로그인ID+1!)')) return;
        var dimmPw = document.getElementById('dimm');
        if (dimmPw) dimmPw.style.display = 'flex';
        window.PG_API.compResetPassword(compId).then(function (r) {
          var pwd = (r && r.data && r.data.tempPassword) ? r.data.tempPassword : (r && r.tempPassword) ? r.tempPassword : '';
          alert(pwd ? '비밀번호가 초기화되었습니다. 임시비밀번호: ' + pwd : '비밀번호가 초기화되었습니다.');
        }).catch(function (err) { alert(err && err.message ? err.message : '비밀번호 초기화 실패'); }).finally(function () { if (dimmPw) dimmPw.style.display = 'none'; });
      });
      pane.addEventListener('click', function (e) {
        var asstReset = e.target && e.target.closest ? e.target.closest('#assistantPwdResetBtn, [data-action="보조 비밀번호 초기화"]') : null;
        if (!asstReset || !pane.contains(asstReset)) return;
        var formAs = pane.querySelector('#compInfoDetailForm');
        if (!formAs || !formAs.contains(asstReset)) return;
        var compIdEl = formAs.querySelector('[name="compId"]');
        var compIdAs = compIdEl && compIdEl.value ? compIdEl.value.trim() : '';
        if (!compIdAs) { alert('업체코드가 없습니다.'); return; }
        if (!confirm('보조 계정 비밀번호를 초기화하시겠습니까? (임시: 보조로그인ID+1!)')) return;
        var dimmAs = document.getElementById('dimm');
        if (dimmAs) dimmAs.style.display = 'flex';
        window.PG_API.compResetAssistantPassword(compIdAs).then(function (r) {
          var pwd = (r && r.data && r.data.tempPassword) ? r.data.tempPassword : (r && r.tempPassword) ? r.tempPassword : '';
          alert(pwd ? '비밀번호가 초기화되었습니다. 임시비밀번호: ' + pwd : '비밀번호가 초기화되었습니다.');
        }).catch(function (err) { alert(err && err.message ? err.message : '비밀번호 초기화 실패'); }).finally(function () { if (dimmAs) dimmAs.style.display = 'none'; });
      });
      if (url === '/comp/myCompMng') {
        var myCompForm = pane.querySelector('#compInfoDetailForm');
        if (myCompForm) {
          var loginIdInput = myCompForm.querySelector('[name="loginId"]');
          var assistantLoginIdInput = myCompForm.querySelector('[name="assistantLoginId"]');
          if (loginIdInput && !loginIdInput._dupResetBound) {
            loginIdInput._dupResetBound = true;
            loginIdInput.addEventListener('input', function () {
              myCompForm.setAttribute('data-login-id-checked', '');
            });
          }
          if (assistantLoginIdInput && !assistantLoginIdInput._dupResetBound) {
            assistantLoginIdInput._dupResetBound = true;
            assistantLoginIdInput.addEventListener('input', function () {
              myCompForm.setAttribute('data-assistant-login-id-checked', '');
              myCompForm.removeAttribute('data-assistant-pwd-confirmed');
            });
          }
          if (myCompForm && !myCompForm._assistantPwdSaveBound) {
            myCompForm._assistantPwdSaveBound = true;
            myCompForm.addEventListener('click', function (ev) {
              var btn = ev.target && ev.target.closest ? ev.target.closest('button[data-field="assistantPwd"][data-action="저장"]') : null;
              if (!btn || !myCompForm.contains(btn)) return;
              var pwdInput = myCompForm.querySelector('[name="assistantPwd"]');
              var v = pwdInput && pwdInput.value ? String(pwdInput.value).trim() : '';
              if (!v) { alert('비밀번호를 입력하세요.'); return; }
              if (v.length < 8) { alert('비밀번호는 8자 이상 입력하세요.'); return; }
              myCompForm.setAttribute('data-assistant-pwd-confirmed', 'Y');
              alert('비밀번호가 확정되었습니다. 하단 [수정 저장]으로 반영하세요.');
            });
            var apwIn = myCompForm.querySelector('[name="assistantPwd"]');
            if (apwIn && !apwIn._assistantPwdInBound) {
              apwIn._assistantPwdInBound = true;
              apwIn.addEventListener('input', function () {
                myCompForm.removeAttribute('data-assistant-pwd-confirmed');
              });
            }
          }
        }
        function runMyCompAutoLoad() {
          var cid = '';
          try {
            var u = JSON.parse(sessionStorage.getItem('pg_admin_user') || '{}');
            cid = (u.compId || '').trim();
          } catch (e) {}
          if (cid) {
            loadCompDetailIntoForm(pane, cid);
            return;
          }
          if (window.PG_API && window.PG_API.authMe) {
            window.PG_API.authMe().then(function (r) {
              var d = r && r.data ? r.data : r;
              if (d && d.compId) {
                try {
                  var prev = JSON.parse(sessionStorage.getItem('pg_admin_user') || '{}');
                  prev.compId = d.compId;
                  prev.orgLevel = d.orgLevel;
                  prev.orgUnitId = d.orgUnitId;
                  sessionStorage.setItem('pg_admin_user', JSON.stringify(prev));
                } catch (e2) {}
                loadCompDetailIntoForm(pane, d.compId);
              } else {
                alert('소속 업체코드를 확인할 수 없습니다. 다시 로그인해 주세요.');
              }
            }).catch(function () { alert('소속 업체 정보를 불러오지 못했습니다.'); });
          }
        }
        setTimeout(runMyCompAutoLoad, 50);
      }
    }
    if (url === '/comp/compDetail') {
      if (typeof window.applyPagePermissionToPane === 'function') {
        window.applyPagePermissionToPane(pane, '/comp/compDetail');
      }
      bindParentCompSearchModal(pane);
      function toggleByCompDiv(compDiv) {
        var isMerchant = compDiv === 'MERCHANT';
        var isRegional = compDiv === 'REGIONAL';
        var isMasterDist = compDiv === 'MASTER_DIST';
        var isBranchAgencySales = compDiv === 'BRANCH' || compDiv === 'AGENCY' || compDiv === 'SALES_OFFICE';
        var isDistributor = isMasterDist || isBranchAgencySales;
        var showAccount = isMerchant || isDistributor || isRegional;
        pane.querySelectorAll('.merchant-only-section').forEach(function (card) {
          if (isMerchant) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.regional-only-section').forEach(function (card) {
          if (isRegional) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.master-dist-only-section').forEach(function (card) {
          if (isMasterDist) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.distributor-only-section').forEach(function (card) {
          if (isDistributor) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.distributor-merchant-no-regional-section').forEach(function (card) {
          if (isMerchant || isDistributor) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.distributor-or-merchant-section').forEach(function (card) {
          if (showAccount) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.branch-agency-sales-hide-section').forEach(function (card) {
          if (isBranchAgencySales) card.classList.add('d-none'); else card.classList.remove('d-none');
        });
        var isRegionalOrMasterDist = isRegional || isMasterDist;
        pane.querySelectorAll('.regional-or-master-dist-only-section').forEach(function (card) {
          if (isRegionalOrMasterDist) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
        pane.querySelectorAll('.merchant-regional-master-commission-section').forEach(function (card) {
          if (isMerchant || isRegional || isMasterDist) card.classList.remove('d-none'); else card.classList.add('d-none');
        });
      }
      var compId = '';
      var storedCompDiv = '';
      try {
        compId = sessionStorage.getItem('pg_comp_detail_compId') || '';
        storedCompDiv = sessionStorage.getItem('pg_comp_detail_compDiv') || '';
      } catch (e) {}
      if (!compId) {
        pane.innerHTML = '<div class="card"><div class="card-body"><p class="text-muted">업체코드가 없습니다. 업체관리에서 행을 더블클릭하여 조회하세요.</p><button type="button" class="btn btn-secondary btn-sm" id="compDetailListBtn">목록</button></div></div>';
      } else {
      toggleByCompDiv(storedCompDiv);
      var dimm = document.getElementById('dimm');
      if (dimm) dimm.style.display = 'flex';
      window.PG_API.compDetail(compId).then(function (data) {
        if (!data) return;
        var form = pane.querySelector('#compDetailForm');
        if (!form) return;
        var allFields = ['compId', 'parentComp', 'compNm', 'compDiv', 'regNo', 'bizType', 'industry', 'bizNature', 'product', 'homepage', 'settleName', 'settleTelNo', 'ceoNm', 'ceoMobile', 'compTel', 'fax', 'zipCode', 'addr', 'addrDetail', 'addrEtc', 'addrCountryCd', 'addrCountryCdOther', 'email', 'siteUrl', 'siteSummary', 'useYn', 'loginId', 'bankCd', 'transferFee', 'cryptoTransferFee', 'accountNo', 'accountHolder', 'commissionConfigAllowed', 'webPaymentUseYn', 'baseCurrency', 'remark', 'settleType', 'commissionRate', 'limitAmt', 'countryCd', 'countryCdOther', 'swift', 'branchName', 'branchAddr', 'contactTel', 'walletAddress', 'networkName', 'withdrawRestrictType', 'withdrawStartTime', 'withdrawEndTime', 'payLimitDefault', 'payLimitExtra', 'payLimitAlertSms', 'holdRateFollowHq', 'holdRate', 'holdDays', 'commissionFollowHq', 'hqPolicyScope', 'failFee', 'usageRate', 'payRate', 'cancelRate', 'voidFeePerTx', 'manualVoidFeePerTx', 'refundRate', 'commissionMemo', 'feeSettlementPerTx', 'feeUsdt', 'feeFx', 'fee3dsRate', 'chargebackFeePerTx', 'calcCycle', 'calcProcType', 'calcCloseTime', 'transferType', 'transferCycleDays', 'autoTransferMin', 'calcMinAmt', 'transferExecTime', 'calcExcludeYn', 'calcExcludeTarget', 'calcStartTime', 'payHoldYn', 'defaultProductName', 'defaultProductCode', 'defaultProductAmount', 'defaultProductDesc', 'notifyUrlBackground', 'notifyUrlResult', 'notifyUrl1', 'notifyUrl2', 'notifyUrl3', 'notifyUrl4', 'remitterName', 'balanceNotifyAmt', 'suspiciousNotifyAmt', 'overseasLoginNotifyAmt', 'tempPwdNotifyAmt', 'nonTranCriterionMonth', 'sameCardLimitWebDay', 'sameCardLimitWebTimes', 'sameCardLimitWebAmt', 'sameCardLimitTerminalDay', 'sameCardLimitTerminalTimes', 'sameCardLimitTerminalAmt', 'dailyUsageFee', 'depositNameLookup', 'transferAuthNo', 'autoConvertNewMemberLimit', 'newMemberDailyLimit', 'convertRefDate', 'convertDailyLimit', 'applyStartDate', 'pgFeeGeneral', 'settleDiffMonthCnt', 'settleReportBankCd', 'pgFeeSamsung', 'smsFee', 'taxInvoiceEmail', 'settleAccountNo', 'directFee', 'solutionFee', 'settleAccountHolder', 'withdrawRestrictType', 'withdrawRestrictStartTime', 'withdrawRestrictEndTime', 'terminalPayRestrict', 'webPayRestrict', 'defaultFeeHq', 'defaultFeeDist', 'defaultFeeBranch', 'defaultFeeAgency', 'defaultFeeSalesOffice', 'defaultPayLimitPerTx', 'defaultPayLimitDay', 'defaultPayLimitMonth', 'defaultPayLimitYearCorp', 'defaultPayLimitYearInd', 'copyright', 'holidayProfileName', 'holidayProfileCountry', 'holidayCountryCode', 'holidayCountryCodes', 'businessHolidayRangesJson', 'businessHolidayExtraDates'];
        allFields.forEach(function (k) {
          var el = form.querySelector('[name="' + k + '"]');
          if (el && data[k] != null) {
            var nf2 = pgFmtCompDetailNumericField(k, data[k]);
            el.value = nf2 != null ? nf2 : data[k];
          }
        });
        var parentCompEl = form.querySelector('[name="parentComp"]');
        if (parentCompEl) parentCompEl.value = normalizeParentCompDisplay(parentCompEl.value);
        initIntlPhoneFields(form);
        var holidayCountryCodeEl = form.querySelector('[name="holidayCountryCode"]');
        if (holidayCountryCodeEl && !holidayCountryCodeEl.value) {
          var legacyCodes = data.holidayCountryCodes ? String(data.holidayCountryCodes).split(',') : [];
          holidayCountryCodeEl.value = (legacyCodes[0] || 'KR').trim();
        }
        if (data.compDiv === 'REGIONAL' && data.baseCurrency) {
          var parts = String(data.baseCurrency).split(/,\s*/);
          ['baseCurrency1', 'baseCurrency2', 'baseCurrency3'].forEach(function (n, i) {
            var el = form.querySelector('[name="' + n + '"]');
            if (el) el.value = parts[i] || '';
          });
        }
        var pidEl = form.querySelector('input[name="parentId"]');
        if (!pidEl) {
          pidEl = document.createElement('input');
          pidEl.type = 'hidden';
          pidEl.name = 'parentId';
          form.appendChild(pidEl);
        }
        if (data.parentId != null) pidEl.value = String(data.parentId);
        var cc = data.countryCd;
        if (cc && cc !== 'JP' && cc !== 'KR' && cc !== 'TH') {
          var countrySel = form.querySelector('select[name="countryCd"]');
          var countryOther = form.querySelector('[name="countryCdOther"]');
          var bankText = form.querySelector('input[name="bankCdText"]');
          if (countrySel) { countrySel.value = 'OTHER'; countrySel.dispatchEvent(new Event('change')); }
          if (countryOther) countryOther.value = cc;
          if (bankText && data.bankCd != null) bankText.value = data.bankCd;
        } else if (cc === 'JP' || cc === 'KR' || cc === 'TH') {
          refreshCountryBankAfterFill(pane, data.bankCd);
        }
        var acc = data.addrCountryCd;
        if (acc && acc !== 'JP' && acc !== 'KR' && acc !== 'TH') {
          var addrCountrySel = form.querySelector('select[name="addrCountryCd"]');
          var addrCountryOther = form.querySelector('[name="addrCountryCdOther"]');
          if (addrCountrySel) { addrCountrySel.value = 'OTHER'; addrCountrySel.dispatchEvent(new Event('change')); }
          if (addrCountryOther) addrCountryOther.value = acc;
        }
        var regNoVal = data.regNo;
        if (regNoVal && regNoVal.indexOf('|') >= 0) {
          var parts = regNoVal.split('|');
          var rt = form.querySelector('[name="regType"]');
          var rn = form.querySelector('[name="regNo"]');
          if (rt) rt.value = (parts[0] === 'PERSONAL' || parts[0] === 'CORP') ? parts[0] : 'CORP';
          if (rn) rn.value = parts.length > 1 ? parts.slice(1).join('|') : '';
        }
        var apiCompDiv = (data.compDiv && data.compDiv !== '-') ? data.compDiv : storedCompDiv;
        toggleByCompDiv(apiCompDiv || storedCompDiv);
        if (apiCompDiv === 'REGIONAL' && window.PG_HQ_HOLIDAY && typeof window.PG_HQ_HOLIDAY.init === 'function') {
          window.PG_HQ_HOLIDAY.init(pane, { force: true });
        }
        initPgBindingList(pane, data.pgBindings, {
          rowActionMode: true,
          getCompId: function () {
            var el = pane.querySelector('#compDetailForm [name="compId"]');
            return el && el.value ? el.value.trim() : '';
          }
        });
        initRegionalCardLimitTable(pane, data.regionalCardLimits || []);
        initRegionalTerminalTable(pane, data.regionalTerminals || []);
        initRegionalHolidayProfileSelector(pane, form, data);
        applyCompParentMoveRules(form, data);
        var commissionFollowEl = pane.querySelector('[name="commissionFollowHq"]');
        if (commissionFollowEl && !commissionFollowEl._commissionToggleBound) {
          commissionFollowEl._commissionToggleBound = true;
          function toggleCommissionCustom(useHq) {
            pane.querySelectorAll('.commission-custom-only').forEach(function (el) {
              el.style.display = useHq === 'Y' ? 'none' : '';
            });
            pane.querySelectorAll('.hq-policy-only').forEach(function (el) {
              el.style.display = useHq === 'Y' ? '' : 'none';
            });
          }
          commissionFollowEl.addEventListener('change', function () { toggleCommissionCustom(this.value); });
          toggleCommissionCustom(commissionFollowEl.value || 'Y');
        }
        var hqPolicySelDetail = pane.querySelector('#compDetailForm [name="hqPolicyScope"]');
        var baseCurDetail = pane.querySelector('#compDetailForm [name="baseCurrency"]');
        if (hqPolicySelDetail && !hqPolicySelDetail._hqPolicyLoadedDetail) {
          hqPolicySelDetail._hqPolicyLoadedDetail = true;
          function refreshHqPolicyDetail(hqd) {
            var list = (hqd && hqd.templates) ? hqd.templates : [];
            pane._hqCommissionTemplatesCacheDetail = list;
            var bc = (data && data.baseCurrency) ? data.baseCurrency : (baseCurDetail ? baseCurDetail.value : '');
            var filt = pgFilterDeployedTemplatesForMerchant(list, bc);
            var prev = (data && data.hqPolicyScope) ? data.hqPolicyScope : hqPolicySelDetail.value;
            hqPolicySelDetail.innerHTML = pgHqPolicyScopeOptionsHtml(filt);
            if (prev) {
              var okd = false;
              var di;
              for (di = 0; di < hqPolicySelDetail.options.length; di++) {
                if (hqPolicySelDetail.options[di].value === prev) {
                  okd = true;
                  break;
                }
              }
              if (okd) hqPolicySelDetail.value = prev;
            }
          }
          window.PG_API.hqDefaultCommission().then(refreshHqPolicyDetail).catch(function () {});
          if (baseCurDetail && !baseCurDetail._hqPolicyBaseBoundDetail) {
            baseCurDetail._hqPolicyBaseBoundDetail = true;
            baseCurDetail.addEventListener('change', function () {
              if (pane._hqCommissionTemplatesCacheDetail) {
                var filtD = pgFilterDeployedTemplatesForMerchant(pane._hqCommissionTemplatesCacheDetail, baseCurDetail.value);
                var prevD = hqPolicySelDetail.value;
                hqPolicySelDetail.innerHTML = pgHqPolicyScopeOptionsHtml(filtD);
                if (prevD) {
                  var ok4 = false;
                  var d2;
                  for (d2 = 0; d2 < hqPolicySelDetail.options.length; d2++) {
                    if (hqPolicySelDetail.options[d2].value === prevD) {
                      ok4 = true;
                      break;
                    }
                  }
                  if (ok4) hqPolicySelDetail.value = prevD;
                }
              } else {
                window.PG_API.hqDefaultCommission().then(refreshHqPolicyDetail).catch(function () {});
              }
            });
          }
        } else if (hqPolicySelDetail && data.hqPolicyScope) {
          hqPolicySelDetail.value = data.hqPolicyScope;
        }
        if (apiCompDiv === 'MERCHANT') {
          fillChargebackPolicySelectsInRoot(form, data.chargebackPolicyId);
        }
        var holdRateFollowEl = pane.querySelector('[name="holdRateFollowHq"]');
        if (holdRateFollowEl && !holdRateFollowEl._holdRateToggleBound) {
          holdRateFollowEl._holdRateToggleBound = true;
          function toggleHoldRateCustom(useHq) {
            pane.querySelectorAll('.hold-rate-custom-only').forEach(function (el) {
              el.style.display = useHq === 'Y' ? 'none' : '';
            });
          }
          holdRateFollowEl.addEventListener('change', function () { toggleHoldRateCustom(this.value); });
          toggleHoldRateCustom(holdRateFollowEl.value || 'Y');
        }
        var paymentInfoCard = pane.querySelector('#pgInfoCard') || pane.querySelector('#webPaymentCard');
        if (paymentInfoCard) {
          var divForPg = apiCompDiv || storedCompDiv;
          paymentInfoCard.style.display = (divForPg === 'MERCHANT') ? '' : 'none';
          var paymentUrlEl = pane.querySelector('#paymentUrlDisplay');
          if (paymentUrlEl && data.compId && divForPg === 'MERCHANT') {
            var base = (window.SITE_CONFIG && window.SITE_CONFIG.paymentBaseUrl) || (window.location.origin || '');
            paymentUrlEl.value = base.replace(/\/$/, '') + '/pay/' + encodeURIComponent(String(data.compId).trim());
          } else if (paymentUrlEl) paymentUrlEl.value = '';
        }
        var copyBtn = pane.querySelector('#paymentUrlCopyBtn');
        if (copyBtn && !copyBtn._bound) {
          copyBtn._bound = true;
          copyBtn.addEventListener('click', function () {
            var inp = pane.querySelector('#paymentUrlDisplay');
            if (inp && inp.value) {
              navigator.clipboard.writeText(inp.value).then(function () { alert('복사되었습니다.'); }).catch(function () { alert('복사 실패'); });
            }
          });
        }
        initBankByCountry(pane);
        initCountryBankGroup(pane);
        initCountryAddressGroup(pane);
        if ((apiCompDiv === 'HEADQUARTERS' || apiCompDiv === 'REGIONAL' || apiCompDiv === 'MASTER_DIST') && window.PG_API.orgBranding) {
          window.PG_API.orgBranding(compId).then(function (b) {
            pgBrandingFillImageDisplayFromFetch(pane, b);
            var themeSel = pane.querySelector('#brandingTheme');
            var hostEl = pane.querySelector('#brandingBrandHost');
            if (themeSel && b.theme) themeSel.value = b.theme || 'DEFAULT';
            if (hostEl) hostEl.value = (b.brandHost != null && b.brandHost !== undefined) ? b.brandHost : '';
          }).catch(function () {});
        }
        var mainBrowse = pane.querySelector('#brandingMainImageBrowse');
        var logoBrowse = pane.querySelector('#brandingLogoImageBrowse');
        if (mainBrowse) mainBrowse.addEventListener('click', function () { var f = pane.querySelector('#brandingMainImageFile'); if (f) f.click(); });
        if (logoBrowse) logoBrowse.addEventListener('click', function () { var f = pane.querySelector('#brandingLogoImageFile'); if (f) f.click(); });
        [pane.querySelector('#brandingMainImageFile'), pane.querySelector('#brandingLogoImageFile')].forEach(function (inp) {
          if (inp) inp.addEventListener('change', function () {
            var urlInp = this.id === 'brandingMainImageFile' ? pane.querySelector('#brandingMainImageUrl') : pane.querySelector('#brandingLogoImageUrl');
            if (urlInp && this.files && this.files[0]) urlInp.value = this.files[0].name;
          });
        });
      }).catch(function (e) {
        pane.innerHTML = '<div class="card"><div class="card-body"><p class="text-danger">' + (e && e.message ? e.message : '조회 실패') + '</p><button type="button" class="btn btn-secondary btn-sm" id="compDetailListBtn">목록</button></div></div>';
      }).finally(function () {
        if (dimm) dimm.style.display = 'none';
        if (typeof window.applyPagePermissionToPane === 'function') {
          window.applyPagePermissionToPane(pane, '/comp/compDetail');
        }
        if (typeof window.applyCompDetailReadOnlyIfOwnNonMerchant === 'function') {
          window.applyCompDetailReadOnlyIfOwnNonMerchant(pane);
        }
      });
      }
      var compDetailSaveBtn = pane.querySelector('#compDetailSaveBtn');
      if (compDetailSaveBtn) {
        compDetailSaveBtn.addEventListener('click', function () {
          if (getPagePermissionForUrl('/comp/compDetail') === 'OBSERVER') return;
          if (pane.classList && pane.classList.contains('pg-comp-detail-self-readonly')) return;
          var form = pane.querySelector('#compDetailForm');
          if (!form) return;
          initIntlPhoneFields(form);
          ['ceoMobile', 'compTel', 'fax', 'settleTelNo', 'contactTel'].forEach(function (n) { syncIntlPhoneHidden(form, n); });
          var compIdEl = form.querySelector('[name="compId"]');
          var compId = compIdEl && compIdEl.value ? compIdEl.value.trim() : '';
          if (!compId) { alert('업체코드가 없습니다.'); return; }
          var fd = {};
          form.querySelectorAll('input, select, textarea').forEach(function (el) {
            if (el.name && el.type !== 'file' && el.name !== 'pgOperational') {
              if (el.name.indexOf('__phone_') === 0) return;
              fd[el.name] = el.value;
            }
          });
          if (fd.countryCd === 'OTHER') { fd.bankCd = fd.bankCdText || fd.bankCd; delete fd.bankCdText; }
          if (fd.addrCountryCd === 'OTHER') { fd.addrCountryCd = fd.addrCountryCdOther || ''; delete fd.addrCountryCdOther; }
          fd.compId = compId;
          
          if (fd.regType != null) { fd.regNo = (fd.regType || 'CORP') + '|' + (fd.regNo || ''); delete fd.regType; }
          var tbodyPg = form.querySelector('#pgBindingTbody');
          if (tbodyPg) {
            var operationalVal = form.querySelector('input[name="pgOperational"]:checked');
            var operationalIdx = operationalVal ? parseInt(operationalVal.value, 10) : 0;
            var bindings = [];
            tbodyPg.querySelectorAll('tr').forEach(function (tr, i) {
              var sel = function (f) { var e = tr.querySelector('[data-field="' + f + '"]'); return e ? e.value : ''; };
              var pgCd = sel('pgCd');
              if (pgCd) {
                bindings.push({
                  pgCd: pgCd,
                  activationYn: sel('activationYn') || 'Y',
                  operationalYn: i === operationalIdx ? 'Y' : 'N',
                  payMethod: sel('payMethod') || 'WEB',
                  mid: sel('mid'),
                  rootNo: sel('rootNo'),
                  apiKey: sel('apiKey'),
                  ivKey: sel('ivKey'),
                  installmentYn: sel('installmentYn') || 'N',
                  maxInstallmentMonths: sel('maxInstallmentMonths')
                });
              }
            });
            fd.pgBindings = JSON.stringify(bindings);
          }
          var compDivVal = form.querySelector('[name="compDiv"]') ? form.querySelector('[name="compDiv"]').value : '';
          if (compDivVal === 'MASTER_DIST') {
            var dn1 = String(fd.notifyUrl1 || '').trim();
            var dn2 = String(fd.notifyUrl2 || '').trim();
            var dHasBackup = !!(String(fd.notifyUrl3 || '').trim() || String(fd.notifyUrl4 || '').trim());
            if (!dn1) { alert('총판은 노티 CALLBACK(URL 1)을 입력해야 합니다.'); return; }
            if (!dn2) { alert('총판은 노티 RESULT(URL 2)를 입력해야 합니다.'); return; }
            if (dHasBackup && (!dn1 || !dn2)) { alert('노티 URL 3·4(보조)를 쓰려면 URL 1·2(CALLBACK·RESULT)가 모두 필요합니다.'); return; }
          }
          if (compDivVal === 'REGIONAL') {
            var bc = [fd.baseCurrency1, fd.baseCurrency2, fd.baseCurrency3].filter(function (v) { return v && v.trim(); });
            fd.baseCurrency = bc.join(',');
            delete fd.baseCurrency1;
            delete fd.baseCurrency2;
            delete fd.baseCurrency3;
            var cardLimitTbody = form.querySelector('#regionalCardLimitTbody');
            if (cardLimitTbody) {
              var cardLimits = [];
              cardLimitTbody.querySelectorAll('tr').forEach(function (tr) {
                var sel = function (f) { var e = tr.querySelector('[data-field="' + f + '"]'); return e ? e.value : ''; };
                cardLimits.push({ payMethod: sel('payMethod'), cardIssuer: sel('cardIssuer'), dayLimit: sel('dayLimit'), timesLimit: sel('timesLimit'), amtLimit: sel('amtLimit'), regReason: sel('regReason'), regDt: sel('regDt'), modDt: sel('modDt'), remark: sel('remark') });
              });
              fd.regionalCardLimits = JSON.stringify(cardLimits);
            }
            var terminalTbody = form.querySelector('#regionalTerminalTbody');
            if (terminalTbody) {
              var terminals = [];
              terminalTbody.querySelectorAll('tr').forEach(function (tr) {
                var sel = function (f) { var e = tr.querySelector('[data-field="' + f + '"]'); return e ? e.value : ''; };
                terminals.push({ pgAgency: sel('pgAgency'), terminalId: sel('terminalId'), remark: sel('remark') });
            });
              fd.regionalTerminals = JSON.stringify(terminals);
            }
            if (fd.holidayCountryCode) fd.holidayCountryCodes = fd.holidayCountryCode;
            if (fd.businessHolidayRangesJson) {
              try {
                var _rows2 = JSON.parse(fd.businessHolidayRangesJson || '[]');
                fd.businessHolidayExtraDates = (_rows2 || []).map(function (r) { return r.fromDate || ''; }).filter(function (v) { return !!v; }).join('\n');
              } catch (e) {}
            }
            var regionalKeys = ['remitterName', 'balanceNotifyAmt', 'suspiciousNotifyAmt', 'overseasLoginNotifyAmt', 'tempPwdNotifyAmt', 'nonTranCriterionMonth', 'sameCardLimitWebDay', 'sameCardLimitWebTimes', 'sameCardLimitWebAmt', 'sameCardLimitTerminalDay', 'sameCardLimitTerminalTimes', 'sameCardLimitTerminalAmt', 'dailyUsageFee', 'depositNameLookup', 'transferAuthNo', 'autoConvertNewMemberLimit', 'newMemberDailyLimit', 'convertRefDate', 'convertDailyLimit', 'applyStartDate', 'pgFeeGeneral', 'transferFee', 'settleDiffMonthCnt', 'settleReportBankCd', 'pgFeeSamsung', 'smsFee', 'taxInvoiceEmail', 'settleAccountNo', 'directFee', 'solutionFee', 'settleAccountHolder', 'withdrawRestrictType', 'withdrawRestrictStartTime', 'withdrawRestrictEndTime', 'terminalPayRestrict', 'webPayRestrict', 'defaultFeeHq', 'defaultFeeDist', 'defaultFeeBranch', 'defaultFeeAgency', 'defaultFeeSalesOffice', 'defaultPayLimitPerTx', 'defaultPayLimitDay', 'defaultPayLimitMonth', 'defaultPayLimitYearCorp', 'defaultPayLimitYearInd', 'copyright', 'regionalCardLimits', 'regionalTerminals', 'holidayProfileName', 'holidayProfileCountry', 'holidayCountryCode', 'holidayCountryCodes', 'businessHolidayRangesJson', 'businessHolidayExtraDates'];
            var regionalSettings = {};
            regionalKeys.forEach(function (k) { if (fd[k] !== undefined && fd[k] !== null && fd[k] !== '') regionalSettings[k] = fd[k]; });
            fd.regionalSettings = JSON.stringify(regionalSettings);
          }
          if (!pgConfirmBeforeSave('저장하시겠습니까?')) return;
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          var mainFile = form.querySelector('#brandingMainImageFile');
          var logoFile = form.querySelector('#brandingLogoImageFile');
          var themeEl = form.querySelector('#brandingTheme');
          var hostEl = form.querySelector('#brandingBrandHost');
          var brandingCard = form.closest('.tab-pane') && form.closest('.tab-pane').querySelector('#brandingCard');
          var isRegOrMaster = brandingCard && !brandingCard.classList.contains('d-none');
          window.PG_API.compUpdate(fd).then(function () {
            var settleFd = {};
            var settleKeys = ['withdrawRestrictType', 'withdrawStartTime', 'withdrawEndTime', 'payLimitDefault', 'payLimitExtra', 'holdRate', 'holdDays', 'calcCloseTime', 'calcStartTime', 'transferCycleDays', 'calcProcType', 'transferType', 'autoTransferMin', 'payHoldYn', 'calcExcludeYn', 'calcExcludeTarget', 'calcMinAmt', 'transferExecTime'];
            if (compDivVal === 'MERCHANT') settleKeys.splice(5, 0, 'calcCycle');
            settleKeys.forEach(function (k) {
              if (fd[k] !== undefined && fd[k] !== null && fd[k] !== '') settleFd[k] = fd[k];
            });
            if (Object.keys(settleFd).length > 0) {
              return window.PG_API.settlementSettingSave(compId, settleFd);
            }
            return Promise.resolve();
          }).then(function () {
            if (isRegOrMaster && window.PG_API.orgBrandingUpload && window.PG_API.orgBrandingSave) {
              var chain = Promise.resolve();
              if (mainFile && mainFile.files && mainFile.files[0]) {
                var _detMainF = mainFile.files[0];
                chain = chain.then(function () {
                  return window.PG_API.orgBrandingUpload(compId, 'main', _detMainF).then(function (data) {
                    pgBrandingSetImageDisplayInput(form, 'main', data, _detMainF);
                    return data;
                  });
                });
              }
              if (logoFile && logoFile.files && logoFile.files[0]) {
                var _detLogoF = logoFile.files[0];
                chain = chain.then(function () {
                  return window.PG_API.orgBrandingUpload(compId, 'logo', _detLogoF).then(function (data) {
                    pgBrandingSetImageDisplayInput(form, 'logo', data, _detLogoF);
                    return data;
                  });
                });
              }
              if (themeEl || hostEl) {
                chain = chain.then(function () { return window.PG_API.orgBrandingSave(compId, (themeEl && themeEl.value) ? themeEl.value : 'DEFAULT', hostEl ? hostEl.value : undefined); });
              }
              return chain;
            }
            return Promise.resolve();
          }).then(function () {
            alert('저장되었습니다.');
          }).catch(function (e) { alert(e && e.message ? e.message : '수정 저장 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        });
      }
      pane.addEventListener('click', function (e) {
        var listBtn = e.target && e.target.closest ? e.target.closest('#compDetailListBtn') : null;
        var idChangeBtn = e.target && e.target.closest ? e.target.closest('[data-action="ID변경"]') : null;
        var pwdResetBtn = e.target && e.target.closest ? e.target.closest('#compDetailPwdResetBtn, [data-action="비밀번호 초기화"]') : null;
        var form = pane.querySelector('#compDetailForm');
        var compIdEl = form && form.querySelector('[name="compId"]');
        var compId = compIdEl && compIdEl.value ? compIdEl.value.trim() : '';
        if (listBtn) {
          if (compId) { try { sessionStorage.setItem('pg_comp_detail_return_compId', compId); } catch (e) {} }
          fnTopMenuMove('/comp/compMngTree', null, '업체관리');
          return;
        }
        if (idChangeBtn && compId) {
          var loginIdEl = form && form.querySelector('[name="loginId"]');
          var currentId = loginIdEl ? String(loginIdEl.value || '') : '';
          var hid = document.getElementById('pgCompLoginIdChangeCompId');
          var lab = document.getElementById('pgCompLoginIdChangeCompIdLabel');
          var inp = document.getElementById('pgCompLoginIdChangeNewId');
          var modal = document.getElementById('pgCompLoginIdChangeModal');
          if (hid && lab && inp && modal && window.PG_UI && window.PG_UI.openModal) {
            window._pgCompLoginIdPending = window._pgCompLoginIdPending || {};
            window._pgCompLoginIdPending.loginIdEl = loginIdEl;
            hid.value = compId;
            lab.textContent = compId;
            inp.value = currentId;
            window.PG_UI.openModal(modal);
            setTimeout(function () { try { inp.focus(); inp.select(); } catch (e1) {} }, 400);
          } else {
            var newIdLegacy = window.prompt('새 로그인 ID를 입력하세요.', currentId);
            if (newIdLegacy != null && newIdLegacy.trim()) {
              var dimmL = document.getElementById('dimm');
              if (dimmL) dimmL.style.display = 'flex';
              window.PG_API.compChangeLoginId(compId, newIdLegacy.trim()).then(function () {
                alert('로그인 ID가 변경되었습니다.');
                if (loginIdEl) loginIdEl.value = newIdLegacy.trim();
              }).catch(function (err) { alert(err && err.message ? err.message : 'ID 변경 실패'); }).finally(function () { if (dimmL) dimmL.style.display = 'none'; });
            }
          }
          return;
        }
        if (pwdResetBtn && compId) {
          if (!confirm('해당 업체의 비밀번호를 초기화하시겠습니까?')) return;
          var dimm = document.getElementById('dimm');
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.compResetPassword(compId).then(function (r) {
            var pwd = (r && r.data && r.data.tempPassword) ? r.data.tempPassword : (r && r.tempPassword) ? r.tempPassword : '';
            alert(pwd ? '비밀번호가 초기화되었습니다. 임시비밀번호: ' + pwd : '비밀번호가 초기화되었습니다.');
          }).catch(function (err) { alert(err && err.message ? err.message : '비밀번호 초기화 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
          return;
        }
      });
    }
    if (url === '/hq/defaultCommission') {
      var dimm = document.getElementById('dimm');
      function hqDefFlash(variant, msg) {
        if (window.PG_UI && window.PG_UI.showBanner) {
          window.PG_UI.showBanner(pane, 'hqDefaultCommissionFlash', variant, msg, 6500);
        }
      }
      function syncHqDefCommTemplateScopeDisplay() {
        var hid = pane.querySelector('#hqDefCommTemplateScope');
        var disp = pane.querySelector('#hqDefCommTemplateScopeDisplay');
        if (!hid || !disp) return;
        var v = hid.value || '';
        var i;
        for (i = 0; i < disp.options.length; i++) {
          if (disp.options[i].value === v) {
            disp.selectedIndex = i;
            return;
          }
        }
        disp.selectedIndex = 0;
      }
      function renderTemplateSelect(data) {
        var hid = pane.querySelector('#hqDefCommTemplateScope');
        var disp = pane.querySelector('#hqDefCommTemplateScopeDisplay');
        if (!hid || !disp) return;
        var prev = hid.value || '';
        var templates = (data && data.templates) ? data.templates : [];
        var h = '<option value="">(신규) 저장 시 자동 부여</option>';
        templates.forEach(function (t) {
          var s = t.scope || '';
          var nm = t.policyName || s.replace('HQPOL:', '');
          h += '<option value="' + String(s).replace(/&/g, '&amp;').replace(/"/g, '&quot;') + '">' + String(nm).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;') + '</option>';
        });
        disp.innerHTML = h;
        function applyScope(v) {
          hid.value = v != null ? String(v) : '';
          syncHqDefCommTemplateScopeDisplay();
        }
        function optSet(v) {
          if (v === '' || v == null) {
            applyScope('');
            return true;
          }
          for (var oi = 0; oi < disp.options.length; oi++) {
            if (disp.options[oi].value === v) {
              applyScope(v);
              return true;
            }
          }
          return false;
        }
        if (!optSet(prev)) {
          if (data && data.deployedTemplateScope) optSet(data.deployedTemplateScope);
          else if (templates.length > 0) optSet(templates[0].scope || '');
          else optSet('');
        }
      }
      function hqDefCommResetFormForNew() {
        pane._hqDefCommIsNew = true;
        var defs = {
          rollingPct: '5', rollingDays: '180', policyName: '', deployYn: 'N', currencyCode: 'KRW', policyRemark: ''
        };
        Object.keys(defs).forEach(function (k) {
          var el = pane.querySelector('[name="' + k + '"]');
          if (el) el.value = defs[k];
        });
        pane.querySelectorAll('.hq-tier-cell').forEach(function (inp) { inp.value = ''; });
        var mdef = {
          payRate: '2.5', perTxFee: '0', failFee: '0', cancelRate: '0', voidFeePerTx: '0', manualVoidFeePerTx: '0', refundRate: '0',
          feeSettlementPerTx: '0', feeUsdt: '0', feeFx: '0', usageRate: '0', fee3dsRate: '0', chargebackFeePerTx: '0'
        };
        Object.keys(mdef).forEach(function (fk) {
          hqTierSumLevels.forEach(function (lv) {
            var el = pane.querySelector('.hq-tier-cell[data-fee="' + fk + '"][data-level="' + lv + '"]');
            if (el) el.value = '';
          });
          var hqC = pane.querySelector('.hq-tier-cell[data-fee="' + fk + '"][data-level="hq"]');
          if (hqC) hqC.value = mdef[fk];
        });
        hqRecalcMerchantAll(pane);
        var ei;
        for (ei = 1; ei <= 4; ei++) {
          var nm = pane.querySelector('[name="extraFee' + ei + 'Name"]');
          var md = pane.querySelector('[name="extraFee' + ei + 'Mode"]');
          if (nm) nm.value = '';
          if (md) md.value = '';
          pane.querySelectorAll('.hq-tier-extra-cell[data-slot="' + ei + '"]').forEach(function (inp) { inp.value = ''; });
        }
        var cb = pane.querySelector('[name="chargebackPolicyId"]');
        if (cb) cb.value = '';
        var hid = pane.querySelector('#hqDefCommTemplateScope');
        if (hid) hid.value = '';
        syncHqDefCommTemplateScopeDisplay();
        renderChargebackPolicySelect(pane._hqDefCommLastData);
        renderPolicyTemplateTable(pane._hqDefCommLastData);
        hqDefFlash('info', '신규 정책 입력 모드입니다. 내용을 입력한 뒤 [저장]하면 코드가 자동 부여되고 목록에 반영됩니다.');
      }
      function renderPolicyTemplateTable(data) {
        var tb = pane.querySelector('#hqDefaultCommissionPolicyList');
        var emptyEl = pane.querySelector('#hqDefaultCommissionPolicyListEmpty');
        if (!tb) return;
        function escH(s) {
          return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
        }
        function nzStr(obj, key, def) {
          var v = obj[key];
          if (v == null || v === '') return def != null ? def : '0';
          return String(v);
        }
        function escAttr(s) {
          return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
        }
        function formatExtraSlot(t, idx, cc) {
          var enm = nzStr(t, 'extraFee' + idx + 'Name', '');
          if (!enm) {
            return '<td class="hq-def-comm-policy-td-extra-slot small text-muted text-center">—</td>';
          }
          var emd = (t['extraFee' + idx + 'Mode'] != null ? String(t['extraFee' + idx + 'Mode']) : '').trim().toUpperCase();
          var evv = nzStr(t, 'extraFee' + idx + 'Value', '0');
          var evFmt = pgFmtPolicyListAmount(evv, cc);
          var raw = emd === 'PCT' ? enm + ' ' + evFmt + '%' : enm + ' ' + evFmt;
          var disp = emd === 'PCT' ? escH(enm) + ' ' + escH(evFmt) + '%' : escH(enm) + ' ' + escH(evFmt);
          return '<td class="hq-def-comm-policy-td-extra-slot small text-truncate text-center" title="' + escAttr(raw) + '">' + disp + '</td>';
        }
        function extraFeeOneLine(t, idx, cc) {
          var enm = nzStr(t, 'extraFee' + idx + 'Name', '');
          if (!enm) return null;
          var emd = (t['extraFee' + idx + 'Mode'] != null ? String(t['extraFee' + idx + 'Mode']) : '').trim().toUpperCase();
          var evv = nzStr(t, 'extraFee' + idx + 'Value', '0');
          var evFmt = pgFmtPolicyListAmount(evv, cc);
          var raw = emd === 'PCT' ? enm + ' ' + evFmt + '%' : enm + ' ' + evFmt;
          var disp = emd === 'PCT' ? escH(enm) + ' ' + escH(evFmt) + '%' : escH(enm) + ' ' + escH(evFmt);
          return { raw: raw, disp: disp };
        }
        function formatExtraSlotLast(t, cc) {
          var a = extraFeeOneLine(t, 3, cc);
          var b = extraFeeOneLine(t, 4, cc);
          if (!a && !b) {
            return '<td class="hq-def-comm-policy-td-extra-slot small text-muted text-center">—</td>';
          }
          var raws = [];
          var disps = [];
          if (a) {
            raws.push(a.raw);
            disps.push(a.disp);
          }
          if (b) {
            raws.push(b.raw);
            disps.push(b.disp);
          }
          return '<td class="hq-def-comm-policy-td-extra-slot small text-truncate text-center" title="' + escAttr(raws.join(' · ')) + '">' + disps.join(' · ') + '</td>';
        }
        var templates = (data && data.templates) ? data.templates : [];
        var curSel = (pane.querySelector('[name="templateScope"]') || {}).value || '';
        var h = '';
        templates.forEach(function (t) {
          var scope = t.scope || '';
          var shortCode = scope.indexOf('HQPOL:') === 0 ? scope.substring(6) : scope;
          var rawName = String(t.policyName || shortCode || '');
          var nm = rawName.replace(/&/g, '&amp;').replace(/</g, '&lt;');
          var dep = (t.deployYn === 'Y')
            ? '<span class="badge bg-success">배포</span>'
            : '<span class="badge bg-light text-dark border">미배포</span>';
          var cur = t.currencyCode != null && String(t.currencyCode).trim() !== '' ? String(t.currencyCode).trim().toUpperCase() : '—';
          var ccForFmt = (t.currencyCode != null && String(t.currencyCode).trim() !== '') ? String(t.currencyCode).trim().toUpperCase() : 'KRW';
          var ua = t.updatedAt ? String(t.updatedAt).replace('T', ' ').replace(/\.\d+Z?$/, '') : '—';
          var active = (scope === curSel) ? ' table-active' : '';
          var esc = String(scope).replace(/&/g, '&amp;').replace(/"/g, '&quot;');
          var cbPolRaw = (t.chargebackPolicyName != null && String(t.chargebackPolicyName).trim() !== '') ? String(t.chargebackPolicyName).trim() : '';
          var cbFeeAmt = pgFmtPolicyListAmount(nzStr(t, 'chargebackFeePerTx', '0'), ccForFmt);
          var cbTd = '<td class="hq-def-comm-policy-td-num">' + escH(cbFeeAmt) + '</td>';
          function tdAmt(raw, cls) {
            return '<td class="hq-def-comm-policy-td-num' + (cls ? ' ' + cls : '') + '">' + escH(pgFmtPolicyListAmount(raw, ccForFmt)) + '</td>';
          }
          function tdPctRow(raw, cls) {
            return '<td class="hq-def-comm-policy-td-num' + (cls ? ' ' + cls : '') + '">' + escH(pgFmtPolicyListAmount(raw, ccForFmt)) + '</td>';
          }
          function tdDays(raw, cls) {
            var nd = parseFloat(String(raw == null || raw === '' ? '0' : raw).replace(/,/g, '.'));
            if (!isFinite(nd)) nd = 0;
            return '<td class="hq-def-comm-policy-td-num' + (cls ? ' ' + cls : '') + '">' + escH(String(Math.round(nd))) + '</td>';
          }
          var cbZoneTd = cbPolRaw
            ? '<td class="hq-def-comm-policy-td-cbzone small text-truncate" title="' + escAttr(cbPolRaw) + '">' + escH(cbPolRaw) + '</td>'
            : '<td class="hq-def-comm-policy-td-cbzone small text-muted">—</td>';
          h += '<tr class="hq-default-comm-policy-row' + active + '" data-scope="' + esc + '" style="cursor:pointer" title="클릭하여 이 정책 불러오기">';
          h += '<td class="text-center align-middle hq-def-comm-chk-cell"><input type="checkbox" class="form-check-input hq-def-comm-row-chk m-0 align-middle" data-scope="' + esc + '" aria-label="행 선택"></td>';
          h += '<td class="font-monospace hq-def-comm-policy-td-code text-nowrap">' + String(shortCode).replace(/&/g, '&amp;').replace(/</g, '&lt;') + '</td><td class="hq-def-comm-policy-td-name small text-truncate align-middle" title="' + escAttr(rawName) + '">' + nm + '</td>' + cbZoneTd + '<td>' + dep + '</td>';
          h += '<td class="font-monospace small">' + cur.replace(/&/g, '&amp;').replace(/</g, '&lt;') + '</td>';
          h += tdAmt(nzStr(t, 'perTxFee', '0'), 'border-start');
          h += tdAmt(nzStr(t, 'failFee', '0'));
          h += tdAmt(nzStr(t, 'feeSettlementPerTx', '0'));
          h += cbTd;
          h += tdAmt(nzStr(t, 'cancelRate', '0'));
          h += tdAmt(nzStr(t, 'voidFeePerTx', '0'));
          h += tdAmt(nzStr(t, 'manualVoidFeePerTx', '0'));
          h += tdAmt(nzStr(t, 'refundRate', '0'));
          h += tdPctRow(nzStr(t, 'payRate', '0'), 'border-start');
          h += tdPctRow(nzStr(t, 'feeUsdt', '0'));
          h += tdPctRow(nzStr(t, 'feeFx', '0'));
          h += tdPctRow(nzStr(t, 'fee3dsRate', '0'));
          h += tdPctRow(nzStr(t, 'rollingPct', '0'), 'border-start');
          h += tdDays(nzStr(t, 'rollingDays', '0'));
          h += tdAmt(nzStr(t, 'usageRate', '0'), 'border-start');
          h += formatExtraSlot(t, 1, ccForFmt);
          h += formatExtraSlot(t, 2, ccForFmt);
          h += formatExtraSlotLast(t, ccForFmt);
          h += '<td class="text-nowrap small">' + escH(ua) + '</td></tr>';
        });
        tb.innerHTML = h;
        var selAll = pane.querySelector('#hqDefCommSelectAll');
        if (selAll) selAll.checked = false;
        if (emptyEl) {
          if (templates.length === 0) emptyEl.classList.remove('d-none');
          else emptyEl.classList.add('d-none');
        }
      }
      function hqDefCommGetCheckedScopes() {
        var out = [];
        var seen = {};
        pane.querySelectorAll('#hqDefaultCommissionPolicyList .hq-def-comm-row-chk:checked').forEach(function (cb) {
          var s = cb.getAttribute('data-scope');
          if (s && !seen[s]) {
            seen[s] = true;
            out.push(s);
          }
        });
        return out;
      }
      function hqDefCommLoadScopeIntoForm(scope) {
        if (!scope) return;
        pane._hqDefCommIsNew = false;
        var hid = pane.querySelector('#hqDefCommTemplateScope');
        if (hid) hid.value = scope;
        syncHqDefCommTemplateScopeDisplay();
        fillDefaultCommissionForm(currentTemplateData(pane._hqDefCommLastData));
        renderPolicyTemplateTable(pane._hqDefCommLastData);
        hqDefFlash('info', '「' + scope.replace(/^HQPOL:/, '') + '」정책을 불러왔습니다. 수정 후 [저장]하세요.');
      }
      function renderChargebackPolicySelect(fullData) {
        var sel = pane.querySelector('[name="chargebackPolicyId"]');
        if (!sel) return;
        var opts = (fullData && fullData.chargebackPolicyOptions) ? fullData.chargebackPolicyOptions : [];
        var tmpl = currentTemplateData(fullData || {});
        var cur = (tmpl && tmpl.chargebackPolicyId != null && String(tmpl.chargebackPolicyId) !== '') ? String(tmpl.chargebackPolicyId) : '';
        function escA(s) {
          return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
        }
        var html = '<option value="">(미사용) 건당 차지백만</option>';
        opts.forEach(function (o) {
          if (!o || o.id == null) return;
          var cc = o.currencyCode != null && String(o.currencyCode).trim() !== '' ? String(o.currencyCode).trim().toUpperCase() : '';
          var nm = o.name != null ? o.name : ('#' + o.id);
          var lab = nm + (cc ? ' (' + cc + ')' : '');
          html += '<option value="' + escA(String(o.id)) + '">' + escA(lab) + '</option>';
        });
        sel.innerHTML = html;
        if (cur) sel.value = cur;
        else sel.value = '';
      }
      var hqTierSumLevels = ['hq', 'regional', 'master', 'branch', 'agency', 'salesOffice'];
      var hqPctFeeKeys = { payRate: 1, feeUsdt: 1, feeFx: 1, fee3dsRate: 1 };
      function hqParseTierCellNumber(s) {
        if (s == null || String(s).trim() === '') return 0;
        var n = parseFloat(String(s).replace(/,/g, '.').trim());
        return isFinite(n) ? n : 0;
      }
      function hqRecalcMerchantAll(paneRef) {
        var feeKeys = ['payRate', 'perTxFee', 'failFee', 'cancelRate', 'voidFeePerTx', 'manualVoidFeePerTx', 'refundRate', 'feeSettlementPerTx', 'feeUsdt', 'feeFx', 'usageRate', 'fee3dsRate', 'chargebackFeePerTx'];
        feeKeys.forEach(function (fk) {
          var sum = 0;
          hqTierSumLevels.forEach(function (lv) {
            var el = paneRef.querySelector('.hq-tier-cell[data-fee="' + fk + '"][data-level="' + lv + '"]');
            sum += hqParseTierCellNumber(el && el.value);
          });
          var mcel = paneRef.querySelector('.hq-tier-cell[data-fee="' + fk + '"][data-level="merchant"]');
          if (!mcel) return;
          if (hqPctFeeKeys[fk]) {
            mcel.value = pgFmtPctOneDecimalInput(String(sum));
          } else {
            mcel.value = pgFmtOneDecimalStripWhole(String(sum));
          }
        });
        var si;
        for (si = 1; si <= 4; si++) {
          var modeEl = paneRef.querySelector('[name="extraFee' + si + 'Mode"]');
          var mode = modeEl ? String(modeEl.value).trim().toUpperCase() : '';
          var pct = mode === 'PCT' || mode === '%';
          var sum = 0;
          hqTierSumLevels.forEach(function (lv) {
            var el = paneRef.querySelector('.hq-tier-extra-cell[data-slot="' + si + '"][data-level="' + lv + '"]');
            sum += hqParseTierCellNumber(el && el.value);
          });
          var mcel = paneRef.querySelector('.hq-tier-extra-cell[data-slot="' + si + '"][data-level="merchant"]');
          if (!mcel) continue;
          if (pct) {
            mcel.value = pgFmtPctOneDecimalInput(String(sum));
          } else {
            mcel.value = pgFmtOneDecimalStripWhole(String(sum));
          }
        }
      }
      function hqFillTierCommissionMatrix(paneRef, tmpl) {
        var levels = ['hq', 'regional', 'master', 'branch', 'agency', 'salesOffice', 'merchant'];
        var feeKeys = ['payRate', 'perTxFee', 'failFee', 'cancelRate', 'voidFeePerTx', 'manualVoidFeePerTx', 'refundRate', 'feeSettlementPerTx', 'feeUsdt', 'feeFx', 'usageRate', 'fee3dsRate', 'chargebackFeePerTx'];
        function setTierCell(fk, lv, v) {
          var el = paneRef.querySelector('.hq-tier-cell[data-fee="' + fk + '"][data-level="' + lv + '"]');
          if (!el) return;
          el.value = v != null && String(v) !== '' ? String(v) : '';
        }
        var tc = tmpl.tierCommission;
        if (tc && tc.rows && typeof tc.rows === 'object') {
          feeKeys.forEach(function (fk) {
            var row = tc.rows[fk];
            if (!row || typeof row !== 'object') return;
            levels.forEach(function (lv) {
              setTierCell(fk, lv, row[lv]);
            });
          });
        } else {
          var pctRowKeys = { payRate: 1, feeUsdt: 1, feeFx: 1, fee3dsRate: 1 };
          feeKeys.forEach(function (fk) {
            levels.forEach(function (lv) { setTierCell(fk, lv, ''); });
            if (tmpl[fk] == null || tmpl[fk] === '') return;
            var mv = pctRowKeys[fk] ? pgFmtPctOneDecimalInput(tmpl[fk]) : pgFmtOneDecimalStripWhole(tmpl[fk]);
            setTierCell(fk, 'hq', mv);
          });
        }
        var extras = (tc && tc.extras) ? tc.extras : [];
        var si;
        for (si = 1; si <= 4; si++) {
          var slot = extras[si - 1];
          var modeEl = paneRef.querySelector('[name="extraFee' + si + 'Mode"]');
          var nameEl = paneRef.querySelector('[name="extraFee' + si + 'Name"]');
          if (slot && slot.name) {
            if (nameEl) nameEl.value = String(slot.name);
            if (modeEl) modeEl.value = slot.mode ? String(slot.mode).toUpperCase() : '';
            levels.forEach(function (lv) {
              var ex = paneRef.querySelector('.hq-tier-extra-cell[data-slot="' + si + '"][data-level="' + lv + '"]');
              if (ex && slot.tiers) ex.value = slot.tiers[lv] != null ? String(slot.tiers[lv]) : '';
            });
          } else {
            if (nameEl) nameEl.value = tmpl['extraFee' + si + 'Name'] != null ? String(tmpl['extraFee' + si + 'Name']) : '';
            if (modeEl) modeEl.value = tmpl['extraFee' + si + 'Mode'] != null ? String(tmpl['extraFee' + si + 'Mode']) : '';
            paneRef.querySelectorAll('.hq-tier-extra-cell[data-slot="' + si + '"]').forEach(function (inp) { inp.value = ''; });
            var md = tmpl['extraFee' + si + 'Mode'];
            var mcel = paneRef.querySelector('.hq-tier-extra-cell[data-slot="' + si + '"][data-level="merchant"]');
            if (mcel && tmpl['extraFee' + si + 'Value'] != null && tmpl['extraFee' + si + 'Value'] !== '') {
              var hqEx = paneRef.querySelector('.hq-tier-extra-cell[data-slot="' + si + '"][data-level="hq"]');
              if (hqEx) {
                hqEx.value = md && String(md).toUpperCase() === 'PCT' ? pgFmtPctOneDecimalInput(tmpl['extraFee' + si + 'Value']) : pgFmtOneDecimalStripWhole(tmpl['extraFee' + si + 'Value']);
              }
            }
          }
        }
        hqRecalcMerchantAll(paneRef);
      }
      function hqCollectTierCommissionPayload(paneRef) {
        var levels = ['hq', 'regional', 'master', 'branch', 'agency', 'salesOffice', 'merchant'];
        var feeKeys = ['payRate', 'perTxFee', 'failFee', 'cancelRate', 'voidFeePerTx', 'manualVoidFeePerTx', 'refundRate', 'feeSettlementPerTx', 'feeUsdt', 'feeFx', 'usageRate', 'fee3dsRate', 'chargebackFeePerTx'];
        var rows = {};
        feeKeys.forEach(function (fk) {
          rows[fk] = {};
          levels.forEach(function (lv) {
            var el = paneRef.querySelector('.hq-tier-cell[data-fee="' + fk + '"][data-level="' + lv + '"]');
            rows[fk][lv] = el && el.value != null ? String(el.value).trim() : '';
          });
        });
        var extras = [];
        var si;
        for (si = 1; si <= 4; si++) {
          var modeEl = paneRef.querySelector('[name="extraFee' + si + 'Mode"]');
          var nameEl = paneRef.querySelector('[name="extraFee' + si + 'Name"]');
          var mode = modeEl ? modeEl.value.trim() : '';
          var name = nameEl ? nameEl.value.trim() : '';
          var tiers = {};
          levels.forEach(function (lv) {
            var el = paneRef.querySelector('.hq-tier-extra-cell[data-slot="' + si + '"][data-level="' + lv + '"]');
            tiers[lv] = el && el.value != null ? String(el.value).trim() : '';
          });
          if (!name || !mode) {
            extras.push({ name: '', mode: '', tiers: { hq: '', regional: '', master: '', branch: '', agency: '', salesOffice: '', merchant: '' } });
          } else {
            extras.push({ name: name, mode: mode, tiers: tiers });
          }
        }
        return { rows: rows, extras: extras };
      }
      function fillDefaultCommissionForm(tmpl) {
        if (!(tmpl && pane.querySelector('.hq-tier-cell'))) return;
        ['policyName', 'deployYn', 'templateScope', 'deployedTemplateScope', 'currencyCode', 'policyRemark', 'rollingPct', 'rollingDays'].forEach(function (k) {
          var el = pane.querySelector('[name="' + k + '"]');
          if (!el || tmpl[k] == null) return;
          if (k === 'rollingDays') {
            var rd = parseFloat(String(tmpl[k]).replace(/,/g, '.'));
            el.value = isFinite(rd) ? String(Math.round(rd)) : String(tmpl[k]);
            return;
          }
          if (k === 'rollingPct') {
            el.value = pgFmtPctOneDecimalInput(tmpl[k]);
            return;
          }
          el.value = tmpl[k];
        });
        hqFillTierCommissionMatrix(pane, tmpl);
        var selCb = pane.querySelector('[name="chargebackPolicyId"]');
        if (selCb) {
          if (tmpl.chargebackPolicyId != null && String(tmpl.chargebackPolicyId) !== '') selCb.value = String(tmpl.chargebackPolicyId);
          else selCb.value = '';
        }
        var hidTs = pane.querySelector('#hqDefCommTemplateScope');
        if (hidTs && tmpl && tmpl.scope != null && String(tmpl.scope) !== '') {
          hidTs.value = tmpl.scope;
          syncHqDefCommTemplateScopeDisplay();
        }
      }
      function currentTemplateData(raw) {
        var scopeEl = pane.querySelector('[name="templateScope"]');
        var scope = scopeEl ? scopeEl.value : '';
        var templates = raw && raw.templates ? raw.templates : [];
        for (var i = 0; i < templates.length; i++) {
          if ((templates[i].scope || '') === scope) return templates[i];
        }
        return raw || {};
      }
      function applyLoadedData(data) {
        pane._hqDefCommLastData = data;
        renderTemplateSelect(data);
        renderChargebackPolicySelect(data);
        fillDefaultCommissionForm(currentTemplateData(data));
        renderPolicyTemplateTable(data);
      }
      function reloadHqDefaultCommission() {
        if (dimm) dimm.style.display = 'flex';
        return window.PG_API.hqDefaultCommission().then(function (data) {
          applyLoadedData(data);
        }).catch(function () {
          hqDefFlash('danger', '정책 목록을 불러오지 못했습니다.');
        }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      }
      /* pane은 탭 재진입 시 유지되고 innerHTML만 갈아끼워지므로, 행 클릭 위임만 1회 등록하고 버튼은 매번 새 DOM에 바인딩한다. */
      if (!pane._hqDefCommRowClickBound) {
        pane._hqDefCommRowClickBound = true;
        pane.addEventListener('click', function (ev) {
          if (ev.target && ev.target.closest && ev.target.closest('.hq-def-comm-chk-cell')) return;
          if (ev.target && ev.target.classList && ev.target.classList.contains('hq-def-comm-row-chk')) return;
          var tr = ev.target.closest && ev.target.closest('tr.hq-default-comm-policy-row');
          if (!tr || !pane.contains(tr)) return;
          var scope = tr.getAttribute('data-scope');
          if (!scope) return;
          hqDefCommLoadScopeIntoForm(scope);
        });
      }
      if (!pane._hqDefCommTierInputBound) {
        pane._hqDefCommTierInputBound = true;
        pane.addEventListener('input', function (ev) {
          var t = ev.target;
          if (!t || !t.classList) return;
          if (t.classList.contains('hq-tier-cell') && t.getAttribute('data-level') !== 'merchant') {
            hqRecalcMerchantAll(pane);
            return;
          }
          if (t.classList.contains('hq-tier-extra-cell') && t.getAttribute('data-level') !== 'merchant') {
            hqRecalcMerchantAll(pane);
            return;
          }
          if (t.name && /^extraFee[1-4]Mode$/.test(t.name)) {
            hqRecalcMerchantAll(pane);
          }
        });
      }
      var selAllEl = pane.querySelector('#hqDefCommSelectAll');
      if (selAllEl && !selAllEl._hqDefCommBound) {
        selAllEl._hqDefCommBound = true;
        selAllEl.addEventListener('change', function () {
          var on = selAllEl.checked;
          pane.querySelectorAll('#hqDefaultCommissionPolicyList .hq-def-comm-row-chk').forEach(function (cb) { cb.checked = on; });
        });
      }
      var hqDefEdit = pane.querySelector('#hqDefaultCommissionEditBtn');
      if (hqDefEdit && !hqDefEdit._hqDefCommBound) {
        hqDefEdit._hqDefCommBound = true;
        hqDefEdit.addEventListener('click', function () {
          var scopes = hqDefCommGetCheckedScopes();
          if (scopes.length === 0) {
            hqDefFlash('warning', '수정할 정책을 목록에서 한 건 체크하세요.');
            return;
          }
          if (scopes.length > 1) {
            hqDefFlash('warning', '[수정]은 한 번에 한 건만 선택할 수 있습니다.');
            return;
          }
          if (!window.confirm('선택한 정책을 폼에 불러와 수정할 수 있습니다. 진행할까요?')) return;
          if (!window.confirm('불러온 뒤 반영하려면 [저장]을 눌러야 합니다. 계속하시겠습니까?')) return;
          hqDefCommLoadScopeIntoForm(scopes[0]);
        });
      }
      function collectHqDefCommFd() {
        hqRecalcMerchantAll(pane);
        var fd = {};
        pane.querySelectorAll('input, select, textarea').forEach(function (el) {
          if (el.name && !el.disabled) fd[el.name] = el.value;
        });
        try {
          fd.tierCommission = JSON.stringify(hqCollectTierCommissionPayload(pane));
        } catch (e) {
          fd.tierCommission = '{"rows":{},"extras":[]}';
        }
        return fd;
      }
      function hqDefCommDoSave() {
        if (!window.confirm('입력한 정책 내용을 서버에 저장하시겠습니까?')) return;
        if (!window.confirm('저장 후 목록이 갱신됩니다. 정말 저장할까요?')) return;
        function finishSaveOk() {
          hqDefFlash('success', '저장되었습니다. 아래 목록이 갱신되었습니다.');
          return reloadHqDefaultCommission();
        }
        if (pane._hqDefCommIsNew) {
          if (dimm) dimm.style.display = 'flex';
          window.PG_API.hqDefaultCommissionTemplateAdd({ templateCode: '' }).then(function (res) {
            var hid = pane.querySelector('#hqDefCommTemplateScope');
            if (hid && res && res.scope) hid.value = res.scope;
            pane._hqDefCommIsNew = false;
            syncHqDefCommTemplateScopeDisplay();
            return window.PG_API.hqDefaultCommissionSave(collectHqDefCommFd()).then(function () { return finishSaveOk(); });
          }).catch(function (e) {
            hqDefFlash('danger', (e && e.message) ? e.message : '저장 또는 정책 추가에 실패했습니다.');
          }).finally(function () { if (dimm) dimm.style.display = 'none'; });
          return;
        }
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.hqDefaultCommissionSave(collectHqDefCommFd()).then(function () { return finishSaveOk(); }).catch(function (e) {
          hqDefFlash('danger', (e && e.message) ? e.message : '저장 실패');
        }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      }
      var hqDefCommFormSave = pane.querySelector('#hqDefCommFormSaveBtn');
      if (hqDefCommFormSave && !hqDefCommFormSave._hqDefCommBound) {
        hqDefCommFormSave._hqDefCommBound = true;
        hqDefCommFormSave.addEventListener('click', function () { hqDefCommDoSave(); });
      }
      var hqDefCommNewBtn = pane.querySelector('#hqDefCommNewPolicyBtn');
      if (hqDefCommNewBtn && !hqDefCommNewBtn._hqDefCommBound) {
        hqDefCommNewBtn._hqDefCommBound = true;
        hqDefCommNewBtn.addEventListener('click', function () {
          if (!window.confirm('신규 정책 입력 모드로 전환합니다. 계속하시겠습니까?')) return;
          if (!window.confirm('폼이 초기값으로 바뀝니다. 진행할까요?')) return;
          hqDefCommResetFormForNew();
        });
      }
      var delTplBtn = pane.querySelector('#hqDefaultCommissionTemplateDeleteBtn');
      if (delTplBtn) {
        delTplBtn.addEventListener('click', function () {
          var scopes = hqDefCommGetCheckedScopes();
          if (scopes.length === 0) {
            hqDefFlash('warning', '삭제할 정책을 목록에서 체크하세요.');
            return;
          }
          if (!window.confirm(scopes.length + '건을 삭제 절차를 시작합니다. 삭제 확인 단계로 진행할까요?')) return;
          if (!window.confirm('삭제는 되돌리기 어렵습니다. 계속하시겠습니까?')) return;
          var lines = scopes.map(function (sc) {
            return '· 「' + sc.replace(/^HQPOL:/, '') + '」(' + sc + ')';
          });
          var body = pane.querySelector('#hqDefaultCommissionDeleteModalText');
          if (body) {
            body.innerHTML = '<span class="d-block mb-2">아래 ' + scopes.length + '건 템플릿을 삭제합니다. 배포 중이면 가맹점 기본 부여에 영향이 있을 수 있습니다.</span>' +
              '<span class="small text-break" style="white-space:pre-line">' + lines.join('\n') + '</span>';
          }
          pane._hqDefDeletePendingScopes = scopes.slice();
          var delModal = pane.querySelector('#hqDefaultCommissionDeleteModal');
          if (delModal && window.PG_UI && window.PG_UI.openModal) {
            window.PG_UI.openModal(delModal);
          } else if (window.confirm('모달을 열 수 없어 바로 삭제 확인을 진행합니다. 선택한 ' + scopes.length + '건을 삭제할까요?') &&
            window.confirm('삭제를 최종 확인합니다. 실행할까요?')) {
            if (dimm) dimm.style.display = 'flex';
            (function delNext(i) {
              if (i >= scopes.length) {
                hqDefFlash('success', '선택한 정책이 삭제되었습니다.');
                return reloadHqDefaultCommission();
              }
              return window.PG_API.hqDefaultCommissionTemplateDelete(scopes[i]).then(function () { return delNext(i + 1); });
            }(0)).catch(function (e) {
              hqDefFlash('danger', (e && e.message) ? e.message : '정책 삭제 실패');
            }).finally(function () { if (dimm) dimm.style.display = 'none'; });
          }
        });
      }
      var delConf = pane.querySelector('#hqDefaultCommissionDeleteConfirmBtn');
      if (delConf) {
        delConf.addEventListener('click', function () {
          var scopes = pane._hqDefDeletePendingScopes;
          if (!scopes || scopes.length === 0) return;
          if (!window.confirm('선택한 ' + scopes.length + '건 템플릿을 서버에서 영구 삭제합니다. 진행할까요?')) return;
          if (!window.confirm('삭제 후에는 복구할 수 없습니다. 정말 실행하시겠습니까?')) return;
          if (dimm) dimm.style.display = 'flex';
          (function delNext(i) {
            if (i >= scopes.length) {
              if (window.PG_UI && window.PG_UI.closeModal) {
                window.PG_UI.closeModal(pane.querySelector('#hqDefaultCommissionDeleteModal'));
              }
              pane._hqDefDeletePendingScopes = [];
              hqDefFlash('success', '선택한 정책이 삭제되었습니다. 목록을 갱신했습니다.');
              return reloadHqDefaultCommission();
            }
            return window.PG_API.hqDefaultCommissionTemplateDelete(scopes[i]).then(function () { return delNext(i + 1); });
          }(0)).catch(function (e) {
            hqDefFlash('danger', (e && e.message) ? e.message : '정책 삭제 실패');
          }).finally(function () { if (dimm) dimm.style.display = 'none'; });
        });
      }
      reloadHqDefaultCommission();
    }
    if (url === '/hq/chargebackPolicy') {
      var dimmCb = document.getElementById('dimm');
      function hqCbFlash(variant, msg) {
        if (window.PG_UI && window.PG_UI.showBanner) {
          window.PG_UI.showBanner(pane, 'hqChargebackPolicyFlash', variant, msg, 6500);
        }
      }
      function hqCbDefaultTiers() {
        return [
          { sortOrder: 0, countMin: 0, countMax: 4, feePerCase: '4500' },
          { sortOrder: 1, countMin: 6, countMax: 9, feePerCase: '9000' },
          { sortOrder: 2, countMin: 10, countMax: 14, feePerCase: '12000' },
          { sortOrder: 3, countMin: 15, countMax: '', feePerCase: '15000' },
          { sortOrder: 4, countMin: 31, countMax: '', feePerCase: '19000' }
        ];
      }
      function escCbAttr(s) {
        return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
      }
      function hqCbRenderTierRow(row) {
        var sortOrder = row.sortOrder != null ? row.sortOrder : 0;
        var cmin = row.countMin != null ? row.countMin : 0;
        var cmax = row.countMax != null && row.countMax !== '' ? row.countMax : '';
        var fee = row.feePerCase != null ? row.feePerCase : '0';
        return '<tr class="hq-cb-tier-row">' +
          '<td><input type="number" class="form-control form-control-sm hq-cb-sort" value="' + escCbAttr(String(sortOrder)) + '" /></td>' +
          '<td><input type="number" class="form-control form-control-sm hq-cb-min" min="0" value="' + escCbAttr(String(cmin)) + '" /></td>' +
          '<td><input type="number" class="form-control form-control-sm hq-cb-max" min="0" placeholder="무제한" value="' + escCbAttr(String(cmax)) + '" /></td>' +
          '<td><input type="text" class="form-control form-control-sm hq-cb-fee" value="' + escCbAttr(String(fee)) + '" /></td>' +
          '<td><button type="button" class="btn btn-sm btn-outline-danger hq-cb-tier-del py-0" aria-label="행 삭제">×</button></td></tr>';
      }
      function hqCbCollectTiers() {
        var tiers = [];
        pane.querySelectorAll('#hqCbPolTierTbody tr.hq-cb-tier-row').forEach(function (tr) {
          var so = (tr.querySelector('.hq-cb-sort') || {}).value;
          var mn = (tr.querySelector('.hq-cb-min') || {}).value;
          var mx = (tr.querySelector('.hq-cb-max') || {}).value;
          var fee = (tr.querySelector('.hq-cb-fee') || {}).value;
          var countMax = null;
          if (mx !== '' && mx != null && String(mx).trim() !== '') {
            var n = parseInt(String(mx).trim(), 10);
            if (!isNaN(n)) countMax = n;
          }
          tiers.push({
            sortOrder: so !== '' && so != null ? parseInt(String(so).trim(), 10) || 0 : 0,
            countMin: mn !== '' && mn != null ? Math.max(0, parseInt(String(mn).trim(), 10) || 0) : 0,
            countMax: countMax,
            feePerCase: fee !== '' && fee != null ? String(fee).trim() : '0'
          });
        });
        return tiers;
      }
      function hqCbClearForm() {
        var idEl = pane.querySelector('#hqCbPolId');
        if (idEl) idEl.value = '';
        var nm = pane.querySelector('#hqCbPolName');
        if (nm) nm.value = '';
        var cur = pane.querySelector('#hqCbPolCurrencyCode');
        if (cur) cur.value = 'KRW';
        var rm = pane.querySelector('#hqCbPolRemark');
        if (rm) rm.value = '';
        var tb = pane.querySelector('#hqCbPolTierTbody');
        if (tb) {
          tb.innerHTML = '';
          hqCbDefaultTiers().forEach(function (row) {
            tb.insertAdjacentHTML('beforeend', hqCbRenderTierRow(row));
          });
        }
      }
      function hqCbLoadDetail(id) {
        if (dimmCb) dimmCb.style.display = 'flex';
        return window.PG_API.hqChargebackPolicyDetail(id).then(function (d) {
          var idEl = pane.querySelector('#hqCbPolId');
          if (idEl) idEl.value = String(d.id || '');
          var nm = pane.querySelector('#hqCbPolName');
          if (nm) nm.value = d.name || '';
          var cur = pane.querySelector('#hqCbPolCurrencyCode');
          if (cur) {
            var cc = (d.currencyCode != null && String(d.currencyCode).trim() !== '') ? String(d.currencyCode).trim().toUpperCase() : 'KRW';
            cur.value = cc;
            var has = false;
            for (var ci = 0; ci < cur.options.length; ci++) {
              if (cur.options[ci].value === cc) { has = true; break; }
            }
            if (!has) {
              var opt = document.createElement('option');
              opt.value = cc;
              opt.textContent = cc;
              cur.appendChild(opt);
              cur.value = cc;
            }
          }
          var rm = pane.querySelector('#hqCbPolRemark');
          if (rm) rm.value = d.remark || '';
          var tb = pane.querySelector('#hqCbPolTierTbody');
          if (tb) {
            tb.innerHTML = '';
            var tiers = (d.tiers && d.tiers.length) ? d.tiers : hqCbDefaultTiers();
            tiers.forEach(function (t) {
              tb.insertAdjacentHTML('beforeend', hqCbRenderTierRow({
                sortOrder: t.sortOrder,
                countMin: t.countMin,
                countMax: t.countMax != null ? t.countMax : '',
                feePerCase: t.feePerCase
              }));
            });
          }
        }).catch(function (e) {
          hqCbFlash('danger', (e && e.message) ? e.message : '불러오기 실패');
        }).finally(function () { if (dimmCb) dimmCb.style.display = 'none'; });
      }
      function hqCbRemarkShort(s, maxLen) {
        var t = String(s == null ? '' : s).replace(/\s+/g, ' ').trim();
        if (t.length <= maxLen) return t;
        return t.substring(0, maxLen) + '…';
      }
      function hqCbRenderList(rows) {
        var tb = pane.querySelector('#hqChargebackPolicyListTbody');
        if (!tb) return;
        if (!rows || !rows.length) {
          tb.innerHTML = '<tr><td colspan="4" class="text-muted text-center small">등록된 정책이 없습니다.</td></tr>';
          return;
        }
        tb.innerHTML = '';
        rows.forEach(function (r) {
          var tr = document.createElement('tr');
          tr.style.cursor = 'pointer';
          tr.setAttribute('data-id', String(r.id));
          var cc = r.currencyCode != null && String(r.currencyCode).trim() !== '' ? String(r.currencyCode).trim().toUpperCase() : 'KRW';
          var rem = r.remark != null ? String(r.remark) : '';
          var remDisp = escCbAttr(hqCbRemarkShort(rem, 48));
          var remTitle = rem.length > 48 ? escCbAttr(rem) : '';
          tr.innerHTML = '<td class="font-monospace small">' + escCbAttr(String(r.id)) + '</td><td>' + escCbAttr(String(r.name || '')) + '</td>' +
            '<td class="text-nowrap small">' + escCbAttr(cc) + '</td><td class="small text-break"' + (remTitle ? ' title="' + remTitle + '"' : '') + '>' + (remDisp || '—') + '</td>';
          tr.addEventListener('click', function () { hqCbLoadDetail(r.id); });
          tb.appendChild(tr);
        });
      }
      function hqCbReloadList() {
        if (dimmCb) dimmCb.style.display = 'flex';
        return window.PG_API.hqChargebackPolicyList().then(function (list) {
          hqCbRenderList(list);
        }).catch(function () {
          hqCbRenderList([]);
          hqCbFlash('danger', '목록을 불러오지 못했습니다.');
        }).finally(function () { if (dimmCb) dimmCb.style.display = 'none'; });
      }
      function hqCbRebind(el, evt, storageKey, fn) {
        if (!el) return;
        var prev = el[storageKey];
        if (prev) el.removeEventListener(evt, prev);
        el[storageKey] = fn;
        el.addEventListener(evt, fn);
      }
      if (pane._hqCbTierDelDelegate) {
        pane.removeEventListener('click', pane._hqCbTierDelDelegate);
      }
      pane._hqCbTierDelDelegate = function (ev) {
        var delBtn = ev.target.closest && ev.target.closest('.hq-cb-tier-del');
        if (delBtn && pane.contains(delBtn)) {
          var tr = delBtn.closest('tr');
          if (tr && tr.parentNode) tr.parentNode.removeChild(tr);
        }
      };
      pane.addEventListener('click', pane._hqCbTierDelDelegate);
      hqCbRebind(pane.querySelector('#hqCbPolAddTierBtn'), 'click', '_hqCbAddTier', function () {
        var tb = pane.querySelector('#hqCbPolTierTbody');
        if (tb) {
          var n = tb.querySelectorAll('tr.hq-cb-tier-row').length;
          tb.insertAdjacentHTML('beforeend', hqCbRenderTierRow({ sortOrder: n, countMin: 0, countMax: '', feePerCase: '0' }));
        }
      });
      hqCbRebind(pane.querySelector('#hqChargebackPolicyNewBtn'), 'click', '_hqCbNew', function () {
        hqCbClearForm();
        hqCbFlash('info', '새 유형을 입력한 뒤 [저장]하세요.');
      });
      hqCbRebind(pane.querySelector('#hqChargebackPolicySaveBtn'), 'click', '_hqCbSave', function () {
        var idEl = pane.querySelector('#hqCbPolId');
        var idRaw = idEl && idEl.value ? idEl.value.trim() : '';
        var curEl = pane.querySelector('#hqCbPolCurrencyCode');
        var body = {
          name: (pane.querySelector('#hqCbPolName') || {}).value || '',
          currencyCode: (curEl && curEl.value) ? String(curEl.value).trim().toUpperCase() : 'KRW',
          remark: (pane.querySelector('#hqCbPolRemark') || {}).value || '',
          tiers: hqCbCollectTiers()
        };
        if (idRaw !== '') body.id = idRaw;
        if (dimmCb) dimmCb.style.display = 'flex';
        window.PG_API.hqChargebackPolicySave(body).then(function (res) {
          hqCbFlash('success', '저장되었습니다.');
          if (res && res.id != null && idEl && !idRaw) idEl.value = String(res.id);
          return hqCbReloadList();
        }).catch(function (e) {
          hqCbFlash('danger', (e && e.message) ? e.message : '저장 실패');
        }).finally(function () { if (dimmCb) dimmCb.style.display = 'none'; });
      });
      hqCbRebind(pane.querySelector('#hqChargebackPolicyDeleteBtn'), 'click', '_hqCbDel', function () {
        var idEl = pane.querySelector('#hqCbPolId');
        var idRaw = idEl && idEl.value ? idEl.value.trim() : '';
        if (!idRaw) {
          hqCbFlash('warning', '삭제할 항목을 목록에서 선택하세요.');
          return;
        }
        if (!window.confirm('이 차지백 정책을 삭제하시겠습니까?')) return;
        if (dimmCb) dimmCb.style.display = 'flex';
        window.PG_API.hqChargebackPolicyDelete(idRaw).then(function () {
          hqCbFlash('success', '삭제되었습니다.');
          hqCbClearForm();
          return hqCbReloadList();
        }).catch(function (e) {
          hqCbFlash('danger', (e && e.message) ? e.message : '삭제 실패');
        }).finally(function () { if (dimmCb) dimmCb.style.display = 'none'; });
      });
      hqCbRebind(pane.querySelector('#hqChargebackPolicyReloadBtn'), 'click', '_hqCbRel', function () { hqCbReloadList(); });
      hqCbReloadList().then(function () { hqCbClearForm(); });
    }
    if (url === '/hq/notifyEnv') {
      var dimmN = document.getElementById('dimm');
      function escNt(s) {
        return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
      }
      function pairKeyFromNotifyCode(code) {
        var c = String(code || '');
        if (/^cb/i.test(c) || /^rs/i.test(c)) return 'p:' + c.substring(2).toLowerCase();
        return 'legacy:' + c;
      }
      function shortNotifyChannel(t) {
        var ch = String(t.channelType || '').toUpperCase();
        return ch === 'RESULT' ? 'RESULT' : 'CALLBACK';
      }
      function groupNotifyTargetsForTable(list) {
        var byKey = {};
        var keys = [];
        (list || []).forEach(function (t) {
          var k = pairKeyFromNotifyCode(t.targetCode);
          if (!byKey[k]) {
            byKey[k] = [];
            keys.push(k);
          }
          byKey[k].push(t);
        });
        keys.sort(function (ka, kb) {
          var maxA = Math.max.apply(null, byKey[ka].map(function (x) { return Number(x.id) || 0; }));
          var maxB = Math.max.apply(null, byKey[kb].map(function (x) { return Number(x.id) || 0; }));
          return maxB - maxA;
        });
        return keys.map(function (k) {
          var g = byKey[k].slice();
          g.sort(function (a, b) {
            var oa = shortNotifyChannel(a) === 'CALLBACK' ? 0 : 1;
            var ob = shortNotifyChannel(b) === 'CALLBACK' ? 0 : 1;
            if (oa !== ob) return oa - ob;
            return (Number(a.id) || 0) - (Number(b.id) || 0);
          });
          return g;
        });
      }
      function renderHqNotifyTargetTable(arr) {
        var tbody = pane.querySelector('#hqNotifyTargetTbody');
        var emptyEl = pane.querySelector('#hqNotifyTargetEmpty');
        if (!tbody) return;
        var list = Array.isArray(arr) ? arr : [];
        if (list.length === 0) {
          tbody.innerHTML = '';
          if (emptyEl) { emptyEl.classList.remove('d-none'); }
          return;
        }
        if (emptyEl) { emptyEl.classList.add('d-none'); }
        var groups = groupNotifyTargetsForTable(list);
        var html = '';
        var no = 0;
        groups.forEach(function (g) {
          no += 1;
          var rs = g.length;
          var name = (g[0] && g[0].targetName) ? g[0].targetName : '';
          g.forEach(function (t, i) {
            var id = t.id != null ? String(t.id) : '';
            var ch = shortNotifyChannel(t);
            var url = t.targetUrl || '';
            html += '<tr>';
            if (i === 0) {
              html += '<td class="text-center align-middle" rowspan="' + rs + '">' + no + '</td>';
              html += '<td class="align-middle" rowspan="' + rs + '">' + escNt(name) + '</td>';
            }
            var urlCls = ch === 'RESULT' ? 'hq-notify-url-cell--result' : 'hq-notify-url-cell--callback';
            html += '<td class="hq-notify-url-cell align-middle ' + urlCls + '"><code class="hq-notify-url-code">' + escNt(url) + '</code></td>';
            html += '<td class="hq-notify-copy-cell text-center align-middle">' +
              '<button type="button" class="btn btn-sm btn-outline-secondary hq-notify-copy-url" data-url="' + escNt(url) + '">복사</button></td>';
            var chCls = ch === 'RESULT' ? 'hq-notify-channel-cell--result' : 'hq-notify-channel-cell--callback';
            html += '<td class="hq-notify-channel-cell align-middle ' + chCls + '"><span class="hq-notify-channel-badge">' + escNt(ch) + '</span></td>';
            html += '<td class="align-middle text-center">' +
              (id ? '<button type="button" class="btn btn-sm btn-outline-danger hq-notify-target-del" data-id="' + escNt(id) + '">삭제</button>' : '-') + '</td>';
            html += '</tr>';
          });
        });
        tbody.innerHTML = html;
      }
      function fillNotifyTargets(list) {
        var arr = Array.isArray(list) ? list : [];
        pane.querySelectorAll('select[data-load-notify-targets="true"]').forEach(function (sel) {
          var cur = sel.value || '';
          var html = '<option value="">선택</option>';
          arr.forEach(function (t) {
            var ch = shortNotifyChannel(t);
            var label = (t.targetName || t.targetCode || '노티') + ' [' + ch + '] ' + (t.targetUrl || '');
            html += '<option value="' + escNt(t.targetUrl || '') + '" data-id="' + escNt(t.id != null ? String(t.id) : '') + '">' + escNt(label) + '</option>';
          });
          sel.innerHTML = html;
          if (cur) sel.value = cur;
        });
        renderHqNotifyTargetTable(arr);
      }
      function fillNotifyEnv(data) {
        if (!data) return;
        ['notifyIngressUrl', 'ingressToken', 'publicBaseUrl', 'autoVoidYn', 'emailVoidYn', 'autoRefundYn', 'forceRefundYn', 'autoVoidAfterHours', 'notifyOkResponse', 'otpRequiredYn', 'otpPolicyMode', 'passwordPolicyMode', 'forgotPasswordEnabledYn', 'managerUserControlEnabledYn', 'managerPasswordResetEnabledYn'].forEach(function (k) {
          var el = pane.querySelector('[name="' + k + '"]');
          if (el && data[k] != null && data[k] !== undefined) el.value = data[k];
        });
      }
      if (dimmN) dimmN.style.display = 'flex';
      Promise.all([
        window.PG_API.hqNotifyEnv(),
        window.PG_API.hqNotifyTargets()
      ]).then(function (res) {
        fillNotifyEnv(res[0]);
        fillNotifyTargets(res[1]);
      }).catch(function () {}).finally(function () { if (dimmN) dimmN.style.display = 'none'; });
      var hqNotifySave = pane.querySelector('#hqNotifyEnvSaveBtn');
      if (hqNotifySave && !hqNotifySave._hqNeBound) {
        hqNotifySave._hqNeBound = true;
        hqNotifySave.addEventListener('click', function () {
          var fd = {};
          ['publicBaseUrl', 'autoVoidYn', 'emailVoidYn', 'autoRefundYn', 'forceRefundYn', 'autoVoidAfterHours', 'notifyOkResponse', 'otpRequiredYn', 'otpPolicyMode', 'passwordPolicyMode', 'forgotPasswordEnabledYn', 'managerUserControlEnabledYn', 'managerPasswordResetEnabledYn'].forEach(function (k) {
            var el = pane.querySelector('[name="' + k + '"]');
            if (el) fd[k] = el.value;
          });
          if (dimmN) dimmN.style.display = 'flex';
          window.PG_API.hqNotifyEnvSave(fd).then(function (data) { fillNotifyEnv(data); alert('저장되었습니다.'); }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimmN) dimmN.style.display = 'none'; });
        });
      }
      var hqNotifyRegen = pane.querySelector('#hqNotifyRegenTokenBtn');
      if (hqNotifyRegen && !hqNotifyRegen._hqNeBound) {
        hqNotifyRegen._hqNeBound = true;
        hqNotifyRegen.addEventListener('click', function () {
          if (!window.confirm('노티 URL 토큰이 바뀝니다. NOTI/칠페이에 등록된 URL도 함께 바꿔야 합니다. 계속하시겠습니까?')) return;
          if (dimmN) dimmN.style.display = 'flex';
          window.PG_API.hqNotifyEnvRegenerateToken().then(function (data) { fillNotifyEnv(data); alert('토큰이 재발급되었습니다. 새 URL을 NOTI에 반영하세요.'); }).catch(function (e) { alert(e && e.message ? e.message : '실패'); }).finally(function () { if (dimmN) dimmN.style.display = 'none'; });
        });
      }
      var createBtn = pane.querySelector('button[data-field="newNotifyTargetName"][data-action="노티자동생성"]');
      if (createBtn && !createBtn._bound) {
        createBtn._bound = true;
        createBtn.addEventListener('click', function () {
          var nameEl = pane.querySelector('[name="newNotifyTargetName"]');
          var name = nameEl && nameEl.value ? String(nameEl.value).trim() : '';
          if (!name) { alert('노티 대상명을 입력하세요.'); return; }
          if (dimmN) dimmN.style.display = 'flex';
          window.PG_API.hqNotifyTargetCreate(name).then(function () {
            return window.PG_API.hqNotifyTargets();
          }).then(function (list) {
            fillNotifyTargets(list);
            alert('CALLBACK·RESULT 노티 URL이 자동 생성되었습니다. 아래 목록에서 확인하세요.');
          }).catch(function (e) { alert(e && e.message ? e.message : '노티 자동생성 실패'); }).finally(function () { if (dimmN) dimmN.style.display = 'none'; });
        });
      }
      if (!pane._hqNotifyTargetTableActionDelegated) {
        pane._hqNotifyTargetTableActionDelegated = true;
        pane.addEventListener('click', function (ev) {
          var copyB = ev.target && ev.target.closest ? ev.target.closest('.hq-notify-copy-url') : null;
          if (copyB && pane.contains(copyB)) {
            var u = copyB.getAttribute('data-url') || '';
            if (!u) return;
            var fail = function () { alert('복사에 실패했습니다. 주소를 직접 선택해 복사하세요.'); };
            if (navigator.clipboard && navigator.clipboard.writeText) {
              navigator.clipboard.writeText(u).catch(function () {
                var ta = document.createElement('textarea');
                ta.value = u;
                document.body.appendChild(ta);
                ta.select();
                try { if (!document.execCommand('copy')) fail(); } catch (e2) { fail(); }
                document.body.removeChild(ta);
              });
            } else {
              var ta2 = document.createElement('textarea');
              ta2.value = u;
              document.body.appendChild(ta2);
              ta2.select();
              try { if (!document.execCommand('copy')) fail(); } catch (e3) { fail(); }
              document.body.removeChild(ta2);
            }
            return;
          }
          var delB = ev.target && ev.target.closest ? ev.target.closest('.hq-notify-target-del') : null;
          if (!delB || !pane.contains(delB)) return;
          var rid = delB.getAttribute('data-id') || '';
          if (!rid) return;
          if (!window.confirm('이 노티 URL을 삭제하시겠습니까?')) return;
          if (dimmN) dimmN.style.display = 'flex';
          window.PG_API.hqNotifyTargetDelete(rid).then(function () {
            return window.PG_API.hqNotifyTargets();
          }).then(function (list) {
            fillNotifyTargets(list);
            alert('삭제되었습니다.');
          }).catch(function (e) { alert(e && e.message ? e.message : '삭제 실패'); }).finally(function () { if (dimmN) dimmN.style.display = 'none'; });
        });
      }
    }
    if (url === '/hq/orgViewColumnAllowance' && !pane._hqOrgColAllowBound) {
      pane._hqOrgColAllowBound = true;
      var dimmO = document.getElementById('dimm');
      var fixedGuideKeys = ['rowNo', 'compId', 'compNm', 'compDivNm', 'trnDate', 'trnTime', 'routeNo'];
      var hqOrgAllowPageLabels = {
        '/calc/payList': '결제내역(통합)',
        '/comp/compMngTree': '업체관리',
        '/commission/commisionList': '수수료관리',
        '/calc/calcList': '정산·유통망정산내역',
        '/calc/calcGmList': '정산·가맹정산내역',
        '/calc/feeList': '정산·수수료내역',
        '/calc/compPointMngList': '정산·환수금관리',
        '/calc/balcInfo': '정산·잔액·미수금관리',
        '/calc/exCalcList': '정산·정산실행',
        '/calc/settlementReport': '정산·정산리포트',
        '/calc/collateralList': '정산·담보금내역',
        '/pay/payHoldList': '정산·정산보류내역'
      };
      var hqOrgAllowScopeLabels = { REGIONAL: '본사', MASTER_DIST: '총판', BRANCH_GROUP: '지사·대리점·영업점', MERCHANT: '가맹점' };
      function pageUrlVal() {
        var sel = pane.querySelector('[name="targetPageUrl"]');
        return sel && sel.value ? String(sel.value) : '/calc/payList';
      }
      function regionalVal() {
        var sel = pane.querySelector('[name="regionalOrgCode"]');
        return sel && sel.value ? String(sel.value).trim() : '';
      }
      function viewerScopeVal() {
        var sel = pane.querySelector('[name="viewerScope"]');
        return sel && sel.value ? String(sel.value).trim() : 'REGIONAL';
      }
      function resolveColumnsForHqOrgAllow(cfg, pageUrl) {
        if (!cfg) return [];
        if (cfg.columns && cfg.columns.length) return cfg.columns;
        var u = pageUrl || '';
        if (u === '/calc/settlementReport' || u === '/settlement/settlementReport') {
          var byKey = {};
          function addArr(arr) {
            (arr || []).forEach(function (c) {
              if (!c || !c.key) return;
              if (!byKey[c.key]) byKey[c.key] = c;
            });
          }
          ['AGG', 'EXE', 'SUM'].forEach(function (sub) {
            if (cfg.columnsBySub && cfg.columnsBySub[sub]) addArr(cfg.columnsBySub[sub]);
            if (cfg.columnsRegionalPayout && cfg.columnsRegionalPayout[sub]) addArr(cfg.columnsRegionalPayout[sub]);
          });
          return Object.keys(byKey).map(function (k) { return byKey[k]; });
        }
        return [];
      }
      function buildAllowanceChecks(pageUrl) {
        var mount = pane.querySelector('#hqOrgAllowColumnChecks');
        if (!mount || !window.PG_SCREENS || !window.PG_SCREENS.getMenuScreens) return;
        var cfg = window.PG_SCREENS.getMenuScreens()[pageUrl];
        var colDefs = resolveColumnsForHqOrgAllow(cfg, pageUrl);
        if (!cfg || !colDefs.length) {
          mount.innerHTML = '<span class="text-muted small">화면 정의를 찾을 수 없습니다.</span>';
          return;
        }
        var html = '';
        colDefs.forEach(function (c) {
          if (!c.key || c.type === 'checkbox' || c.type === 'payActions' || c.type === 'commissionInlineActions' || fixedGuideKeys.indexOf(c.key) !== -1) return;
          var label = c.label || c.key;
          html += '<label class="column-guide-item column-guide-item--on d-inline-flex align-items-center me-2 mb-1"><input type="checkbox" class="hq-allow-col-check" data-key="' + c.key + '" checked> <span class="column-guide-label small">' + label + '</span></label>';
        });
        mount.innerHTML = html || '<span class="text-muted small">선택 가능한 열이 없습니다.</span>';
      }
      function readCheckedAllowKeys() {
        var keys = [];
        pane.querySelectorAll('.hq-allow-col-check:checked').forEach(function (cb) {
          var k = cb.getAttribute('data-key');
          if (k) keys.push(k);
        });
        return keys;
      }
      function applyChecksFromJson(jsonStr) {
        var keys = [];
        try { keys = JSON.parse(jsonStr || '[]'); } catch (e) { keys = []; }
        if (!Array.isArray(keys)) keys = [];
        var set = {};
        keys.forEach(function (k) { set[k] = 1; });
        pane.querySelectorAll('.hq-allow-col-check').forEach(function (cb) {
          var k = cb.getAttribute('data-key') || '';
          cb.checked = !!set[k];
        });
      }
      if (pane.querySelector('select[data-load-regional-branches="true"]') && window.PG_API && window.PG_API.hqOrgViewColumnRegionalBranches) {
        window.PG_API.hqOrgViewColumnRegionalBranches().then(function (list) {
          var arr = Array.isArray(list) ? list : [];
          pane.querySelectorAll('select[data-load-regional-branches="true"]').forEach(function (sel) {
            var cur = sel.value || '';
            var h = '<option value="">선택</option>';
            arr.forEach(function (o) {
              var c = o.code || o.compId || '';
              var n = o.name || c;
              if (!c) return;
              h += '<option value="' + c + '">' + n + ' (' + c + ')</option>';
            });
            sel.innerHTML = h;
            if (cur) sel.value = cur;
          });
        }).catch(function () {});
      }
      buildAllowanceChecks(pageUrlVal());
      if (!pane._hqOrgAllowBulkBound) {
        pane._hqOrgAllowBulkBound = true;
        pane.addEventListener('click', function (ev) {
          var t = ev.target;
          if (!t || !t.id) return;
          if (t.id === 'hqOrgAllowColSelectAllBtn') {
            ev.preventDefault();
            pane.querySelectorAll('.hq-allow-col-check').forEach(function (cb) { cb.checked = true; });
          } else if (t.id === 'hqOrgAllowColClearAllBtn') {
            ev.preventDefault();
            pane.querySelectorAll('.hq-allow-col-check').forEach(function (cb) { cb.checked = false; });
          }
        });
      }
      var pageSel = pane.querySelector('[name="targetPageUrl"]');
      if (pageSel && !pageSel._hqOrgColPageBound) {
        pageSel._hqOrgColPageBound = true;
        pageSel.addEventListener('change', function () {
          buildAllowanceChecks(pageUrlVal());
        });
      }
      var loadBtn = pane.querySelector('#hqOrgAllowLoadBtn');
      if (loadBtn && !loadBtn._bound) {
        loadBtn._bound = true;
        loadBtn.addEventListener('click', function () {
          var r = regionalVal();
          var p = pageUrlVal();
          if (!r) { alert('대상 본사를 선택하세요.'); return; }
          if (dimmO) dimmO.style.display = 'flex';
          window.PG_API.hqOrgViewColumnAllowanceGet(r, p, viewerScopeVal()).then(function (d) {
            buildAllowanceChecks(p);
            if (d && d.hasPolicy && d.allowedKeysJson != null) applyChecksFromJson(String(d.allowedKeysJson));
            else pane.querySelectorAll('.hq-allow-col-check').forEach(function (cb) { cb.checked = true; });
          }).catch(function (e) { alert(e && e.message ? e.message : '불러오기 실패'); }).finally(function () { if (dimmO) dimmO.style.display = 'none'; });
        });
      }
      var saveBtn = pane.querySelector('#hqOrgAllowSaveBtn');
      if (saveBtn && !saveBtn._bound) {
        saveBtn._bound = true;
        saveBtn.addEventListener('click', function () {
          var r = regionalVal();
          var p = pageUrlVal();
          if (!r) { alert('대상 본사를 선택하세요.'); return; }
          var keys = readCheckedAllowKeys();
          if (!keys.length && !window.confirm('허용 열이 하나도 없습니다. (선택 컬럼 없음) 저장할까요?')) return;
          if (dimmO) dimmO.style.display = 'flex';
          window.PG_API.hqOrgViewColumnAllowanceSave({
            regionalOrgCode: r,
            pageUrl: p,
            viewerScope: viewerScopeVal(),
            allowedKeysJson: JSON.stringify(keys)
          }).then(function () {
            alert('저장되었습니다.');
          }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimmO) dimmO.style.display = 'none'; });
        });
      }
      var delBtn = pane.querySelector('#hqOrgAllowDeleteBtn');
      if (delBtn && !delBtn._bound) {
        delBtn._bound = true;
        delBtn.addEventListener('click', function () {
          var r = regionalVal();
          var p = pageUrlVal();
          if (!r) { alert('대상 본사를 선택하세요.'); return; }
          if (!window.confirm('선택한 본사·조직 유형·화면에 대한 컬럼 제한만 해제합니다. 계속할까요?')) return;
          if (dimmO) dimmO.style.display = 'flex';
          window.PG_API.hqOrgViewColumnAllowanceDelete({ regionalOrgCode: r, pageUrl: p, viewerScope: viewerScopeVal() }).then(function () {
            buildAllowanceChecks(p);
            applyChecksFromJson('[]');
            alert('제한이 해제되었습니다.');
          }).catch(function (e) { alert(e && e.message ? e.message : '실패'); }).finally(function () { if (dimmO) dimmO.style.display = 'none'; });
        });
      }
    }
    if (url === '/hq/notifyMapping' && !pane._hqNotifyMappingBound) {
      pane._hqNotifyMappingBound = true;
      var dimmMap = document.getElementById('dimm');
      function fillNotifyMapping(d) {
        if (!d) return;
        var j = pane.querySelector('[name="mappingDefinitionJson"]');
        if (j && d.mappingDefinitionJson != null) j.value = d.mappingDefinitionJson;
        var u = pane.querySelector('[name="updatedAt"]');
        if (u && d.updatedAt != null) u.value = d.updatedAt;
      }
      if (dimmMap) dimmMap.style.display = 'flex';
      window.PG_API.hqNotifyMapping().then(fillNotifyMapping).catch(function () {}).finally(function () { if (dimmMap) dimmMap.style.display = 'none'; });
      var hqNmSave = pane.querySelector('#hqNotifyMappingSaveBtn');
      if (hqNmSave) {
        hqNmSave.addEventListener('click', function () {
          var raw = pane.querySelector('[name="mappingDefinitionJson"]');
          var txt = raw ? String(raw.value || '').trim() : '';
          try {
            JSON.parse(txt);
          } catch (e) {
            alert('JSON 형식이 올바르지 않습니다. 중괄호·쉼표·따옴표를 확인하세요.');
            return;
          }
          if (dimmMap) dimmMap.style.display = 'flex';
          window.PG_API.hqNotifyMappingSave({ mappingDefinitionJson: txt }).then(function (d) {
            fillNotifyMapping(d);
            alert('저장되었습니다.');
          }).catch(function (e) {
            alert(e && e.message ? e.message : '저장 실패');
          }).finally(function () { if (dimmMap) dimmMap.style.display = 'none'; });
        });
      }
    }
    if (url === '/hq/accountMng' && !pane._hqAccBound) {
      pane._hqAccBound = true;
      pane.addEventListener('click', function (e) {
        var del = e.target.closest && e.target.closest('.hq-acc-del');
        if (!del || !pane.contains(del)) return;
        var rid = del.getAttribute('data-id');
        if (!rid || !window.confirm('이 접근 규칙을 삭제할까요?')) return;
        var dimmA = document.getElementById('dimm');
        if (dimmA) dimmA.style.display = 'flex';
        window.PG_API.hqAccountAccessDelete(rid).then(function () {
          if (typeof doSearch === 'function') doSearch(pane, tabId, 1);
        }).catch(function (err) { alert(err && err.message ? err.message : '삭제 실패'); }).finally(function () { if (dimmA) dimmA.style.display = 'none'; });
      });
      var hqAccAdd = pane.querySelector('#hqAccountAccessAddBtn');
      if (hqAccAdd && !hqAccAdd._bound) {
        hqAccAdd._bound = true;
        hqAccAdd.addEventListener('click', function () {
          window._pgHqAccountAccessPending = { pane: pane, tabId: tabId };
          var uEl = document.getElementById('pgHqAccountAccessUsername');
          var cEl = document.getElementById('pgHqAccountAccessCompCode');
          var modal = document.getElementById('pgHqAccountAccessAddModal');
          if (uEl) uEl.value = '';
          if (cEl) cEl.value = '';
          if (modal && window.PG_UI && window.PG_UI.openModal) {
            window.PG_UI.openModal(modal);
            setTimeout(function () { try { if (uEl) uEl.focus(); } catch (e2) {} }, 400);
          } else {
            var uid = window.prompt('사용자ID(로그인 ID)를 입력하세요.');
            if (uid == null || !String(uid).trim()) return;
            var cc = window.prompt('허용할 업체코드(본사·총판·가맹점 코드)를 입력하세요.');
            if (cc == null || !String(cc).trim()) return;
            var dimmA = document.getElementById('dimm');
            if (dimmA) dimmA.style.display = 'flex';
            window.PG_API.hqAccountAccessAdd({ username: String(uid).trim(), compCode: String(cc).trim() }).then(function () {
              if (typeof doSearch === 'function') doSearch(pane, tabId, 1);
            }).catch(function (err) { alert(err && err.message ? err.message : '추가 실패'); }).finally(function () { if (dimmA) dimmA.style.display = 'none'; });
          }
        });
      }
    }
    if (url === '/user/userMng' && !pane._userMngBound) {
      pane._userMngBound = true;
      var gridTid = tabId;
      function userMngTbody() {
        return pane.querySelector('#grid_' + gridTid + ' tbody');
      }
      var addUserBtn = pane.querySelector('#addBtn');
      if (addUserBtn && !addUserBtn._bound) {
        addUserBtn._bound = true;
        addUserBtn.addEventListener('click', function () {
          var dimmA = document.getElementById('dimm');
          if (dimmA) dimmA.style.display = 'flex';
          window.PG_API.authMe().then(function (resp) {
            var d = resp && resp.data !== undefined && resp.data !== null ? resp.data : resp;
            var compId = String((d && d.compId) || '').trim();
            var compNm = String((d && d.compNm) || '').trim() || '-';
            if (!compId) {
              alert('소속 업체코드를 확인할 수 없습니다.');
              return;
            }
            var cap = {};
            if (pane._lastGridList && pane._lastGridList.length) {
              cap = pane._lastGridList[0];
            }
            pane._userMngDraftRows = pane._userMngDraftRows || [];
            pane._userMngDraftRows.unshift({
              _draft: true,
              _tempId: 'd' + Date.now(),
              compId: compId,
              compNm: compNm,
              userId: '',
              userNm: '',
              mobile: '',
              assistantRoleType: 'MANAGER',
              roleNm: 'USER',
              otpRegisteredYn: 'N',
              userStatus: 'ACTIVE',
              inactiveReason: '',
              canManageUsers: cap.canManageUsers || 'Y',
              canResetPassword: cap.canResetPassword || 'Y'
            });
            var pg = parseInt(pane.getAttribute('data-last-page') || '1', 10) || 1;
            if (typeof doSearch === 'function') doSearch(pane, tabId, pg);
          }).catch(function () {
            alert('사용자 정보를 불러오지 못했습니다.');
          }).finally(function () { if (dimmA) dimmA.style.display = 'none'; });
        });
      }
      var saveUserBtn = pane.querySelector('#saveBtn');
      if (saveUserBtn && !saveUserBtn._bound) {
        saveUserBtn._bound = true;
        saveUserBtn.addEventListener('click', function () {
          var tbody = userMngTbody();
          if (!tbody || !window.PG_API) return;
          var dimmS = document.getElementById('dimm');
          var drafts = tbody.querySelectorAll('tr[data-draft="1"]');
          var updateRows = [];
          tbody.querySelectorAll('tr').forEach(function (tr) {
            if (tr.getAttribute('data-draft') === '1') return;
            var rid = tr.getAttribute('data-id');
            if (rid) updateRows.push(tr);
          });
          var addTasks = [];
          for (var i = 0; i < drafts.length; i++) {
            (function (tr) {
              var uidEl = tr.querySelector('[data-field="userId"]');
              var nmEl = tr.querySelector('[data-field="userNm"]');
              var mobEl = tr.querySelector('[data-field="mobile"]');
              var pwdEl = tr.querySelector('.user-mng-pwd');
              var arEl = tr.querySelector('[data-field="assistantRoleType"]');
              var userId = uidEl ? String(uidEl.value || '').trim() : '';
              var userNm = nmEl ? String(nmEl.value || '').trim() : '';
              var mobile = mobEl ? String(mobEl.value || '').trim() : '';
              var password = pwdEl ? String(pwdEl.value || '').trim() : '';
              var assistantRoleType = arEl && arEl.value ? String(arEl.value).trim().toUpperCase() : 'MANAGER';
              var compId = '';
              try {
                var rowObj = pane._userMngDraftRows.filter(function (x) { return x._tempId === tr.getAttribute('data-temp-id'); })[0];
                compId = rowObj && rowObj.compId ? String(rowObj.compId).trim() : '';
              } catch (e1) { compId = ''; }
              if (!userId || !userNm || !password) {
                alert('추가 행: 사용자ID, 사용자명, 비밀번호(8자 이상)를 입력하세요.');
                addTasks = null;
                return;
              }
              if (password.length < 8) {
                alert('비밀번호는 8자 이상이어야 합니다.');
                addTasks = null;
                return;
              }
              addTasks.push(function () {
                return window.PG_API.userAdd({
                  compId: compId,
                  userId: userId,
                  userNm: userNm,
                  mobile: mobile,
                  password: password,
                  role: 'USER',
                  userType: 'ASSISTANT',
                  assistantRoleType: assistantRoleType
                });
              });
            })(drafts[i]);
            if (addTasks === null) return;
          }
          var updTasks = [];
          for (var j = 0; j < updateRows.length; j++) {
            (function (tr) {
              var rid = tr.getAttribute('data-id');
              if (!rid) return;
              var mobEl = tr.querySelector('[data-field="mobile"]');
              var stEl = tr.querySelector('[data-field="userStatus"]');
              var irEl = tr.querySelector('[data-field="inactiveReason"]');
              var arEl = tr.querySelector('[data-field="assistantRoleType"]');
              updTasks.push(function () {
                return window.PG_API.userUpdate({
                  id: rid,
                  mobile: mobEl ? String(mobEl.value || '').trim() : '',
                  userStatus: stEl && stEl.value ? String(stEl.value).trim() : 'ACTIVE',
                  inactiveReason: irEl ? String(irEl.value || '').trim() : '',
                  assistantRoleType: arEl && arEl.value ? String(arEl.value).trim() : 'MANAGER'
                });
              });
            })(updateRows[j]);
          }
          if (dimmS) dimmS.style.display = 'flex';
          var chain = Promise.resolve();
          (addTasks || []).forEach(function (fn) {
            chain = chain.then(function () { return fn(); });
          });
          chain = chain.then(function () {
            return updTasks.reduce(function (p, fn) { return p.then(function () { return fn(); }); }, Promise.resolve());
          });
          chain.then(function () {
            pane._userMngDraftRows = [];
            alert('저장되었습니다.');
            if (typeof doSearch === 'function') doSearch(pane, tabId, 1);
          }).catch(function (err) {
            alert(err && err.message ? err.message : '저장 실패');
          }).finally(function () { if (dimmS) dimmS.style.display = 'none'; });
        });
      }
      pane.addEventListener('click', function (e) {
        var draftRm = e.target.closest && e.target.closest('.user-mng-draft-remove');
        if (draftRm && pane.contains(draftRm)) {
          var tid = draftRm.getAttribute('data-temp-id') || '';
          pane._userMngDraftRows = (pane._userMngDraftRows || []).filter(function (d) { return String(d._tempId) !== tid; });
          var pg = parseInt(pane.getAttribute('data-last-page') || '1', 10) || 1;
          if (typeof doSearch === 'function') doSearch(pane, tabId, pg);
          return;
        }
        var otpB = e.target.closest && e.target.closest('.user-otp-reset-btn');
        if (otpB && pane.contains(otpB)) {
          var oid = otpB.getAttribute('data-id');
          if (!oid || !window.confirm('OTP 등록을 초기화할까요?')) return;
          var dimmO = document.getElementById('dimm');
          if (dimmO) dimmO.style.display = 'flex';
          window.PG_API.userResetOtp(oid).then(function () {
            alert('OTP가 초기화되었습니다.');
            if (typeof doSearch === 'function') doSearch(pane, tabId, parseInt(pane.getAttribute('data-last-page') || '1', 10) || 1);
          }).catch(function (err) { alert(err && err.message ? err.message : '초기화 실패'); }).finally(function () { if (dimmO) dimmO.style.display = 'none'; });
          return;
        }
        var resetBtn = e.target.closest && e.target.closest('.user-reset-pwd-btn');
        if (!resetBtn || !pane.contains(resetBtn)) return;
        var resetId = resetBtn.getAttribute('data-id');
        if (!resetId || !window.confirm('비밀번호를 아이디+1! 로 초기화할까요? 최초 로그인 시 새 창에서 비밀번호를 다시 설정합니다.')) return;
        var dimmUR = document.getElementById('dimm');
        if (dimmUR) dimmUR.style.display = 'flex';
        window.PG_API.userResetPassword(resetId).then(function (data) {
          alert('초기화 완료\n사용자ID: ' + (data.userId || '') + '\n임시 비밀번호(아이디+1!): ' + (data.tempPassword || ''));
          if (typeof doSearch === 'function') doSearch(pane, tabId, parseInt(pane.getAttribute('data-last-page') || '1', 10) || 1);
        }).catch(function (err) {
          alert(err && err.message ? err.message : '초기화 실패');
        }).finally(function () { if (dimmUR) dimmUR.style.display = 'none'; });
      });
    }
    if ((url === '/calc/payList' || url === '/calc/payNotiList') && !pane._payFollowBound) {
      pane._payFollowBound = true;
      pane.addEventListener('click', function (e) {
        var btn = e.target.closest ? e.target.closest('.pay-follow') : null;
        if (!btn || !pane.contains(btn)) return;
        var trn = btn.getAttribute('data-trn') || '';
        var act = btn.getAttribute('data-act') || '';
        if (!trn || !act) return;
        if (!window.confirm('거래 ' + trn + '에 대해 [' + act + '] 를 실행할까요?')) return;
        var dimmP = document.getElementById('dimm');
        if (dimmP) dimmP.style.display = 'flex';
        window.PG_API.payAction(trn, act).then(function () {
          alert('처리되었습니다.');
          if (typeof doSearch === 'function') doSearch(pane, tabId, parseInt(pane.getAttribute('data-last-page') || '1', 10) || 1);
        }).catch(function (err) { alert(err && err.message ? err.message : '실패'); }).finally(function () { if (dimmP) dimmP.style.display = 'none'; });
      });
    }
    if (url === '/hq/apiConfig') {
      var dimm2 = document.getElementById('dimm');
      if (dimm2) dimm2.style.display = 'flex';
      window.PG_API.hqApiConfig().then(function (data) {
        if (data && pane.querySelector('[name="baseUrl"]')) {
          ['baseUrl', 'authType', 'timeoutSec', 'memo', 'chillpayMerchantCode', 'chillpayApiKey', 'chillpayMd5Key', 'chillpayRouteNo', 'chillpaySandbox', 'recallIncludeFeeYn', 'settlementVatApplyYn'].forEach(function (k) {
            var el = pane.querySelector('[name="' + k + '"]');
            if (el && data[k] != null) el.value = data[k];
          });
        }
      }).catch(function () {}).finally(function () { if (dimm2) dimm2.style.display = 'none'; });
      var hqApiSave = pane.querySelector('#hqApiConfigSaveBtn');
      if (hqApiSave) hqApiSave.addEventListener('click', function () {
        var fd = {};
        pane.querySelectorAll('input, select, textarea').forEach(function (el) { if (el.name) fd[el.name] = el.value; });
        if (dimm2) dimm2.style.display = 'flex';
        window.PG_API.hqApiConfigSave(fd).then(function () { alert('저장되었습니다.'); }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimm2) dimm2.style.display = 'none'; });
      });
    }
    if (url === '/hq/domainConfig') {
      var dimmDom = document.getElementById('dimm');
      var sid = tabId;
      function escDomCfg(s) {
        return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
      }
      function escDomAttr(s) {
        return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/'/g, '&#39;');
      }
      /** 표시·입력란용: 이미 http(s)면 유지, 아니면 https:// 부착 */
      function domainCfgDisplayWithHttps(raw) {
        var s = raw == null ? '' : String(raw).trim();
        if (!s) return '';
        if (/^https?:\/\//i.test(s)) return s;
        return 'https://' + s.replace(/^\/+/, '');
      }
      /** 저장 전: 비우면 '', 있으면 스킴 없을 때 https:// 부착 */
      function domainCfgNormalizeUrlForSave(raw) {
        var s = raw == null ? '' : String(raw).trim();
        if (!s) return '';
        return domainCfgDisplayWithHttps(s);
      }
      /** http/https만 허용. 스킴 없으면 https:// 부착 */
      function domainCfgSafeUrlHref(raw) {
        var s = raw == null ? '' : String(raw).trim();
        if (!s) return null;
        var tryHref = domainCfgDisplayWithHttps(s);
        try {
          var u = new URL(tryHref);
          if (u.protocol !== 'http:' && u.protocol !== 'https:') return null;
          return u.href;
        } catch (e1) {
          return null;
        }
      }
      /** 도메인구성 표시용: 유효 URL이면 새 탭 링크(표시문자에도 https:// 반영), 아니면 code만 */
      function domainCfgUrlCell(raw) {
        var s = raw == null ? '' : String(raw).trim();
        if (!s) return '<span class="text-muted">—</span>';
        var display = domainCfgDisplayWithHttps(s);
        var href = domainCfgSafeUrlHref(s);
        var label = escDomCfg(display);
        if (!href) {
          return '<code class="hq-domain-url-cell">' + label + '</code>';
        }
        return '<a href="' + escDomAttr(href) + '" target="_blank" rel="noopener noreferrer" class="hq-domain-url-link text-decoration-none"><code class="hq-domain-url-cell">' + label + '</code></a>';
      }
      function domainCfgHostLink(hostname) {
        var s = hostname == null ? '' : String(hostname).trim();
        if (!s) return '<span class="text-muted">—</span>';
        var display = domainCfgDisplayWithHttps(s);
        var href = domainCfgSafeUrlHref(s);
        if (!href) return '<span class="hq-domain-host-link font-monospace">' + escDomCfg(display) + '</span>';
        return '<a href="' + escDomAttr(href) + '" target="_blank" rel="noopener noreferrer" class="hq-domain-host-link font-monospace">' + escDomCfg(display) + '</a>';
      }
      function setDomInlineMsg(el, kind, text) {
        if (!el) return;
        el.textContent = text || '';
        el.className = 'small mb-2';
        if (kind === 'success') el.className += ' text-success';
        else if (kind === 'error') el.className += ' text-danger';
        else el.className += ' text-muted';
      }
      function renderOrgDomainTableBody(rows) {
        var tb = pane.querySelector('#hqDomainOrgTableTbody_' + sid);
        if (!tb) return;
        var list = Array.isArray(rows) ? rows : [];
        if (!list.length) {
          tb.innerHTML = '<tr><td colspan="9" class="text-center text-muted py-3">등록된 본사·총판 조직이 없습니다.</td></tr>';
          return;
        }
        var html = '';
        list.forEach(function (r, i) {
          var oid = escDomCfg(r.orgUnitId);
          var nm = escDomCfg(r.name || r.code || '');
          html += '<tr data-org-id="' + oid + '">' +
            '<td class="text-center text-muted">' + (i + 1) + '</td>' +
            '<td>' + escDomCfg(r.name) + '</td>' +
            '<td class="font-monospace small">' + escDomCfg(r.code) + '</td>' +
            '<td>' + escDomCfg(r.orgLevelLabel || r.orgLevel) + '</td>' +
            '<td>' + escDomCfg(r.domainSettingName) + '</td>' +
            '<td class="text-center">' + domainCfgUrlCell(r.orgDomainAdminUrl) + '</td>' +
            '<td class="text-center">' + domainCfgUrlCell(r.orgDomainApiUrl) + '</td>' +
            '<td class="text-center">' +
            '<button type="button" class="btn btn-outline-danger btn-sm py-0 px-1" data-action="hqDomainOrgDelete" data-org-unit-id="' + oid + '" data-org-label="' + escDomAttr(nm) + '">삭제</button>' +
            '</td>' +
            '<td class="small text-muted text-nowrap">' + escDomCfg(r.domainUrlsUpdatedAt ? String(r.domainUrlsUpdatedAt).replace('T', ' ').slice(0, 19) : '') + '</td>' +
            '</tr>';
        });
        tb.innerHTML = html;
      }
      function findOrgRowById(id) {
        var rows = pane._hqDomainOrgRows || [];
        var s = String(id || '');
        for (var i = 0; i < rows.length; i++) {
          if (String(rows[i].orgUnitId) === s) return rows[i];
        }
        return null;
      }
      function setOrgEditorEnabled(on) {
        ['hqDomainSettingName_' + sid, 'hqDomainOrgAdminUrl_' + sid, 'hqDomainOrgApiUrl_' + sid].forEach(function (id) {
          var el = pane.querySelector('#' + id);
          if (el) el.disabled = !on;
        });
        var saveB = pane.querySelector('#hqDomainOrgSaveBtn_' + sid);
        if (saveB) saveB.disabled = !on;
      }
      function fillOrgEditorFromRow(row) {
        var codeEl = pane.querySelector('#hqDomainOrgCode_' + sid);
        var lvEl = pane.querySelector('#hqDomainOrgLevel_' + sid);
        var nmEl = pane.querySelector('#hqDomainSettingName_' + sid);
        var adEl = pane.querySelector('#hqDomainOrgAdminUrl_' + sid);
        var apEl = pane.querySelector('#hqDomainOrgApiUrl_' + sid);
        if (!row) {
          if (codeEl) codeEl.value = '';
          if (lvEl) lvEl.value = '';
          if (nmEl) nmEl.value = '';
          if (adEl) adEl.value = '';
          if (apEl) apEl.value = '';
          return;
        }
        if (codeEl) codeEl.value = row.code || '';
        if (lvEl) lvEl.value = row.orgLevelLabel || row.orgLevel || '';
        if (nmEl) nmEl.value = row.domainSettingName || '';
        if (adEl) adEl.value = row.orgDomainAdminUrl ? domainCfgDisplayWithHttps(row.orgDomainAdminUrl) : '';
        if (apEl) apEl.value = row.orgDomainApiUrl ? domainCfgDisplayWithHttps(row.orgDomainApiUrl) : '';
      }
      function fillOrgSelect(rows) {
        var sel = pane.querySelector('#hqDomainOrgSelect_' + sid);
        if (!sel) return;
        var list = Array.isArray(rows) ? rows : [];
        var cur = sel.value;
        sel.innerHTML = '<option value="">— 업체를 선택하세요 —</option>' +
          list.map(function (r) {
            return '<option value="' + escDomCfg(r.orgUnitId) + '">' + escDomCfg(r.name || '') + '</option>';
          }).join('');
        if (cur && list.some(function (r) { return String(r.orgUnitId) === cur; })) sel.value = cur;
      }
      function renderSslDomainLinkage(link) {
        var box = pane.querySelector('#hqDomainSslLinkage_' + sid);
        if (!box) return;
        if (!link || typeof link !== 'object') {
          box.innerHTML = '<p class="text-muted mb-0">연동 요약을 불러오지 못했습니다.</p>';
          return;
        }
        var st = link.sslStatus || '—';
        var days = link.daysRemaining != null ? String(link.daysRemaining) : '—';
        var live = link.leLiveCertName ? String(link.leLiveCertName) : '—';
        var san = Array.isArray(link.sanDnsNames) ? link.sanDnsNames : [];
        var rows = Array.isArray(link.configuredHostRows) ? link.configuredHostRows : [];
        var miss = Array.isArray(link.hostsMissingFromCert) ? link.hostsMissingFromCert : [];
        var sanOnly = Array.isArray(link.sanWithoutConfiguredUrl) ? link.sanWithoutConfiguredUrl : [];
        var hint = link.linkageHint ? String(link.linkageHint) : '';
        var missAlert = '';
        if (miss.length) {
          missAlert = '<div class="alert alert-warning py-2 small mb-2" role="alert"><strong>인증서 SAN에 없는 호스트</strong> (URL은 저장됐으나 PEM의 SAN과 불일치)<ul class="mb-0 mt-1 ps-3">' +
            miss.map(function (m) {
              return '<li>' + domainCfgHostLink(m.hostname) + ' — ' + escDomCfg(m.source) + '</li>';
            }).join('') + '</ul></div>';
        }
        var tbl = '<div class="table-responsive mb-2"><table class="table table-sm table-bordered align-middle mb-0">' +
          '<thead class="table-light"><tr><th>호스트명</th><th>출처</th><th class="text-center text-nowrap" style="width:7rem">SAN 포함</th></tr></thead><tbody>';
        if (!rows.length) {
          tbl += '<tr><td colspan="3" class="text-muted text-center py-2">비교할 URL이 없습니다. 전사 URL 또는 본사·총판 URL을 입력하세요.</td></tr>';
        } else {
          rows.forEach(function (r) {
            var ok = r.inCertificate === true;
            tbl += '<tr><td class="text-center">' + domainCfgHostLink(r.hostname) + '</td><td>' + escDomCfg(r.source) + '</td><td class="text-center">' +
              (ok ? '<span class="badge bg-success">예</span>' : '<span class="badge bg-danger">아니오</span>') + '</td></tr>';
          });
        }
        tbl += '</tbody></table></div>';
        var sanBlock = san.length
          ? '<p class="small fw-semibold mb-2">인증서 SAN (' + san.length + ')</p>' +
            '<div class="table-responsive mb-2" style="max-height:240px;overflow:auto">' +
            '<table class="table table-sm table-bordered align-middle mb-0 text-center">' +
            '<thead class="table-light"><tr><th class="text-center" style="width:3.5rem">No.</th>' +
            '<th class="text-center">브라우저 호스트명 (SAN dNSName)</th></tr></thead><tbody>' +
            san.map(function (h, idx) {
              return '<tr><td class="text-center text-muted">' + (idx + 1) + '</td>' +
                '<td class="text-center">' + domainCfgHostLink(h) + '</td></tr>';
            }).join('') +
            '</tbody></table></div>'
          : '<p class="text-muted small mb-2">SAN 목록을 읽지 못했습니다. 서버관리에서 LE 경로를 확인하세요.</p>';
        var sanOnlyBlock = sanOnly.length
          ? '<p class="small text-muted mb-0">SAN에만 있고 도메인구성 URL에 없는 호스트: ' +
            sanOnly.map(function (h) { return domainCfgHostLink(h); }).join('<span class="text-muted">, </span>') + '</p>'
          : '';
        box.innerHTML =
          '<p class="small mb-2"><strong>PEM 상태</strong> ' + escDomCfg(st) +
          ' · <strong>LE 인증서 이름</strong> <code>' + escDomCfg(live) + '</code>' +
          ' · <strong>만료까지(일)</strong> ' + escDomCfg(days) +
          (link.notAfter ? (' · <span class="text-muted">notAfter ' + escDomCfg(String(link.notAfter)) + '</span>') : '') +
          '</p>' + missAlert + tbl + sanBlock + (hint ? '<p class="text-muted small mb-2">' + escDomCfg(hint) + '</p>' : '') + sanOnlyBlock;
      }
      if (dimmDom) dimmDom.style.display = 'flex';
      window.PG_API.hqDomainConfig().then(function (data) {
        if (!data) return;
        ['publicAdminSiteUrl', 'publicApiBaseUrl'].forEach(function (k) {
          var el = pane.querySelector('[name="' + k + '"]');
          if (el && data[k] != null && String(data[k]).trim()) el.value = domainCfgDisplayWithHttps(data[k]);
          else if (el && (data[k] == null || !String(data[k]).trim())) el.value = '';
        });
        pane._hqDomainOrgRows = data.orgDomainRows || [];
        fillOrgSelect(pane._hqDomainOrgRows);
        renderOrgDomainTableBody(pane._hqDomainOrgRows);
        setOrgEditorEnabled(false);
        fillOrgEditorFromRow(null);
        var hint = pane.querySelector('#hqDomainOrgHint_' + sid);
        if (hint) hint.textContent = '업체를 선택하면 입력란이 활성화됩니다.';
        renderSslDomainLinkage(data.sslDomainLinkage);
      }).catch(function () {
        renderOrgDomainTableBody([]);
        renderSslDomainLinkage(null);
      }).finally(function () { if (dimmDom) dimmDom.style.display = 'none'; });

      var gSave = pane.querySelector('#hqDomainGlobalSaveBtn_' + sid);
      if (gSave && !gSave._hqDomBound) {
        gSave._hqDomBound = true;
        gSave.addEventListener('click', function () {
          var gMsg = pane.querySelector('#hqDomainGlobalMsg_' + sid);
          setDomInlineMsg(gMsg, '', '');
          var fd = {
            publicAdminSiteUrl: domainCfgNormalizeUrlForSave((pane.querySelector('[name="publicAdminSiteUrl"]') || {}).value || ''),
            publicApiBaseUrl: domainCfgNormalizeUrlForSave((pane.querySelector('[name="publicApiBaseUrl"]') || {}).value || '')
          };
          if (dimmDom) dimmDom.style.display = 'flex';
          window.PG_API.hqDomainConfigSave(fd).then(function (res) {
            var paIn = pane.querySelector('[name="publicAdminSiteUrl"]');
            var pbIn = pane.querySelector('[name="publicApiBaseUrl"]');
            if (paIn) paIn.value = fd.publicAdminSiteUrl;
            if (pbIn) pbIn.value = fd.publicApiBaseUrl;
            setDomInlineMsg(gMsg, 'success', (res && res.message) ? res.message : '전사 URL이 저장되었습니다.');
            if (res && res.sslDomainLinkage) renderSslDomainLinkage(res.sslDomainLinkage);
          }).catch(function (e) {
            setDomInlineMsg(gMsg, 'error', e && e.message ? e.message : '저장 실패');
          }).finally(function () { if (dimmDom) dimmDom.style.display = 'none'; });
        });
      }

      var orgSel = pane.querySelector('#hqDomainOrgSelect_' + sid);
      if (orgSel && !orgSel._hqDomBound) {
        orgSel._hqDomBound = true;
        orgSel.addEventListener('change', function () {
          var oMsg = pane.querySelector('#hqDomainOrgMsg_' + sid);
          setDomInlineMsg(oMsg, '', '');
          var id = orgSel.value;
          if (!id) {
            setOrgEditorEnabled(false);
            fillOrgEditorFromRow(null);
            return;
          }
          var row = findOrgRowById(id);
          setOrgEditorEnabled(true);
          fillOrgEditorFromRow(row);
        });
      }

      var orgSave = pane.querySelector('#hqDomainOrgSaveBtn_' + sid);
      if (orgSave && !orgSave._hqDomBound) {
        orgSave._hqDomBound = true;
        orgSave.addEventListener('click', function () {
          var oMsg = pane.querySelector('#hqDomainOrgMsg_' + sid);
          setDomInlineMsg(oMsg, '', '');
          var selEl = pane.querySelector('#hqDomainOrgSelect_' + sid);
          var oid = selEl && selEl.value ? String(selEl.value).trim() : '';
          if (!oid) {
            setDomInlineMsg(oMsg, 'error', '업체를 먼저 선택하세요.');
            return;
          }
          var body = {
            orgUnitId: oid,
            domainSettingName: (pane.querySelector('#hqDomainSettingName_' + sid) || {}).value || '',
            orgDomainAdminUrl: domainCfgNormalizeUrlForSave((pane.querySelector('#hqDomainOrgAdminUrl_' + sid) || {}).value || ''),
            orgDomainApiUrl: domainCfgNormalizeUrlForSave((pane.querySelector('#hqDomainOrgApiUrl_' + sid) || {}).value || '')
          };
          if (dimmDom) dimmDom.style.display = 'flex';
          window.PG_API.hqDomainConfigOrgSave(body).then(function (res) {
            if (res && res.orgDomainRows) {
              pane._hqDomainOrgRows = res.orgDomainRows;
              fillOrgSelect(pane._hqDomainOrgRows);
              renderOrgDomainTableBody(pane._hqDomainOrgRows);
              fillOrgEditorFromRow(findOrgRowById(oid));
            }
            if (res && res.sslDomainLinkage) renderSslDomainLinkage(res.sslDomainLinkage);
            setDomInlineMsg(oMsg, 'success', (res && res.message) ? res.message : '도메인 설정이 저장되었습니다.');
          }).catch(function (e) {
            setDomInlineMsg(oMsg, 'error', e && e.message ? e.message : '저장 실패');
          }).finally(function () { if (dimmDom) dimmDom.style.display = 'none'; });
        });
      }

      if (!pane._hqDomainOrgTableDelBound) {
        pane._hqDomainOrgTableDelBound = true;
        pane.addEventListener('click', function (e) {
          var delBtn = e.target && e.target.closest ? e.target.closest('button[data-action="hqDomainOrgDelete"]') : null;
          if (!delBtn || !pane.contains(delBtn)) return;
          var delId = delBtn.getAttribute('data-org-unit-id') || '';
          var label = delBtn.getAttribute('data-org-label') || '';
          if (!delId) return;
          var msg1 = '[' + (label || delId) + '] 조직의 도메인 설정(설정 이름·관리자 URL·API URL)을 삭제합니다. 계속하시겠습니까?';
          var msg2 = '한 번 더 확인합니다. 삭제 후 입력 내용은 서버에서 비워집니다. 정말 삭제하시겠습니까?';
          if (typeof window.pgDoubleConfirm === 'function') {
            if (!window.pgDoubleConfirm(msg1, msg2)) return;
          } else if (!window.confirm(msg1) || !window.confirm(msg2)) {
            return;
          }
          var oMsg2 = pane.querySelector('#hqDomainOrgMsg_' + sid);
          setDomInlineMsg(oMsg2, '', '');
          if (dimmDom) dimmDom.style.display = 'flex';
          window.PG_API.hqDomainConfigOrgDelete({ orgUnitId: delId }).then(function (res) {
            if (res && res.orgDomainRows) {
              pane._hqDomainOrgRows = res.orgDomainRows;
              fillOrgSelect(pane._hqDomainOrgRows);
              renderOrgDomainTableBody(pane._hqDomainOrgRows);
              var selEl2 = pane.querySelector('#hqDomainOrgSelect_' + sid);
              var cur = selEl2 && selEl2.value ? String(selEl2.value).trim() : '';
              if (cur === delId) {
                fillOrgEditorFromRow(findOrgRowById(delId));
              }
            }
            if (res && res.sslDomainLinkage) renderSslDomainLinkage(res.sslDomainLinkage);
            setDomInlineMsg(oMsg2, 'success', (res && res.message) ? res.message : '도메인 설정을 삭제했습니다.');
          }).catch(function (err) {
            setDomInlineMsg(oMsg2, 'error', err && err.message ? err.message : '삭제 실패');
          }).finally(function () { if (dimmDom) dimmDom.style.display = 'none'; });
        });
      }
    }
    if (url === '/hq/serverManage') {
      function hqSrvClearTimers() {
        if (pane._serverManageTimer) {
          clearInterval(pane._serverManageTimer);
          pane._serverManageTimer = null;
        }
        if (pane._hqSrvCountdownTimer) {
          clearInterval(pane._hqSrvCountdownTimer);
          pane._hqSrvCountdownTimer = null;
        }
      }
      hqSrvClearTimers();
      var dimmSrv = document.getElementById('dimm');

      function hqSrvEsc(s) {
        return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
      }
      function hqSrvFmtGbFromMb(mb) {
        if (mb == null || mb === '') return '—';
        var n = Number(mb);
        if (isNaN(n) || n < 0) return '—';
        return (Math.round((n / 1024) * 1000) / 1000).toFixed(3).replace(/\.?0+$/, '') + ' GB';
      }
      function hqSrvMbToGbInput(mb) {
        if (mb === undefined || mb === null || mb === '') return '';
        var n = Number(mb);
        if (isNaN(n) || n <= 0) return '';
        var g = n / 1024;
        var s = g.toFixed(4).replace(/\.?0+$/, '');
        return s;
      }
      function hqSrvGbToMbContract(gbStr) {
        if (gbStr === undefined || gbStr === null) return null;
        var t = String(gbStr).trim().replace(',', '.');
        if (t === '') return null;
        var g = parseFloat(t);
        if (isNaN(g) || g < 0) return null;
        var mb = Math.round(g * 1024);
        return mb <= 0 ? null : mb;
      }
      function hqSrvGbToMbTrafficUsed(gbStr) {
        if (gbStr === undefined || gbStr === null) return null;
        var t = String(gbStr).trim().replace(',', '.');
        if (t === '') return null;
        var g = parseFloat(t);
        if (isNaN(g) || g < 0) return null;
        return Math.max(0, Math.round(g * 1024));
      }
      /** 분 입력 → 초(60~3600, DB server_manage_ui_refresh_sec) */
      function hqSrvMinToSecRefresh(minStr) {
        if (minStr === undefined || minStr === null) return null;
        var t = String(minStr).trim().replace(',', '.');
        if (t === '') return null;
        var m = parseFloat(t);
        if (isNaN(m) || m <= 0) return null;
        var sec = Math.round(m * 60);
        return Math.max(60, Math.min(3600, sec));
      }
      function hqSrvFmtBytes(n) {
        n = Number(n) || 0;
        if (n >= 1099511627776) return (n / 1099511627776).toFixed(2) + ' TB';
        if (n >= 1073741824) return (n / 1073741824).toFixed(2) + ' GB';
        if (n >= 1048576) return (n / 1048576).toFixed(2) + ' MB';
        if (n >= 1024) return (n / 1024).toFixed(2) + ' KB';
        return n + ' B';
      }
      function hqSrvFmtUptimeMs(ms) {
        ms = Number(ms) || 0;
        var s = Math.floor(ms / 1000);
        var d = Math.floor(s / 86400);
        s -= d * 86400;
        var h = Math.floor(s / 3600);
        s -= h * 3600;
        var m = Math.floor(s / 60);
        return d + '일 ' + h + '시간 ' + m + '분';
      }
      function hqSrvBadge(status) {
        if (status === 'danger') return '<span class="badge bg-danger">위험</span>';
        if (status === 'warn') return '<span class="badge bg-warning text-dark">주의</span>';
        return '<span class="badge bg-success">양호</span>';
      }
      function hqSrvProgress(pct, level) {
        pct = Math.max(0, Math.min(100, Number(pct) || 0));
        var cls = level === 'danger' ? 'bg-danger' : level === 'warn' ? 'bg-warning' : 'bg-success';
        return '<div class="progress mt-2" style="height:10px"><div class="progress-bar ' + cls + '" role="progressbar" style="width:' + pct + '%"></div></div>';
      }
      function updateHqMonCrossOriginHint() {
        var el = pane.querySelector('#hqMonCrossOriginHint');
        if (!el) return;
        var pageH = (window.location && window.location.hostname) || '';
        var apiRoot = '';
        try {
          apiRoot = (typeof window.PG_API_BASE === 'string' ? window.PG_API_BASE : '').replace(/\/$/, '').trim();
          if (!apiRoot) apiRoot = (window.PG_PUBLIC_ICOPAY_API || 'https://api.icopay.co.kr').replace(/\/$/, '');
          var u = new URL(apiRoot);
          if (u.hostname && pageH && u.hostname !== pageH && pageH !== 'localhost' && pageH !== '127.0.0.1') {
            el.classList.remove('d-none');
            el.className = 'alert alert-info py-2 small mb-0 mt-2';
            el.innerHTML = '<strong>구조 안내 (NOTI 대비)</strong> ' +
              '<a href="https://github.com/ziobiz/NOTI" target="_blank" rel="noopener">NOTI</a> 서버관리는 Node가 <em>같은 출처</em>로 HTML을 내려 세션만으로 조회합니다. ' +
              'PG 관리자는 브라우저가 <code>' + hqSrvEsc(u.origin) + '</code> 로 API를 호출합니다. ' +
              '목록이 비면 CORS·방화벽·최신 JAR를 확인하거나, <strong>API와 동일 호스트</strong>에서 관리자를 여는 것을 권장합니다.';
            return;
          }
        } catch (e0) { /* ignore */ }
        el.classList.add('d-none');
        el.innerHTML = '';
      }
      function hqMonStat(k, v, sub, danger) {
        return '<div class="hq-mon-stat' + (danger ? ' danger' : '') + '">' +
          '<div class="hq-mon-stat-k">' + hqSrvEsc(k) + '</div>' +
          '<div class="hq-mon-stat-v">' + hqSrvEsc(v) + '</div>' +
          (sub ? '<div class="hq-mon-stat-sub">' + sub + '</div>' : '') +
          '</div>';
      }
      function hqLoadChartJsOnce() {
        if (window.Chart) return Promise.resolve();
        if (window._pgChartJsLoading) return window._pgChartJsLoading;
        window._pgChartJsLoading = new Promise(function (resolve, reject) {
          var s = document.createElement('script');
          s.src = 'https://cdn.jsdelivr.net/npm/chart.js@4.4.1/dist/chart.umd.min.js';
          s.async = true;
          s.onload = function () { resolve(); };
          s.onerror = function () { reject(new Error('Chart.js load failed')); };
          document.head.appendChild(s);
        });
        return window._pgChartJsLoading;
      }
      function hqUsageFormatSummary(sum, grain) {
        if (!sum) return '';
        var g = grain || 'daily';
        var lines = [];
        lines.push('<div class="fw-semibold mb-2">[' + hqSrvEsc(sum.grainLabel || '일간') + '] 현황 요약 <span class="text-muted fw-normal">(아래 그래프와 동일 데이터)</span></div>');
        if (!sum.hasData) {
          lines.push('<p class="text-muted mb-2">아직 누적 데이터가 거의 없습니다. 앱이 서버에서 수집(기본 10분 간격)을 수행하면 일별로 쌓입니다.</p>');
        }
        lines.push('<ul class="mb-0 ps-3">');
        lines.push('<li>그래프 구간 수: <strong>' + (sum.daysInChart != null ? sum.daysInChart : '—') + '</strong>' +
          (g === 'daily' && sum.maxChartDays ? ' (일간 최대 ' + sum.maxChartDays + '일)' : '') + '</li>');
        lines.push('<li>최근 7일 트래픽 합: <strong>' + hqSrvEsc(String(sum.trafficTotalLast7DaysMb != null ? sum.trafficTotalLast7DaysMb : '—')) + '</strong> MB</li>');
        lines.push('<li>그래프 기간 트래픽 합: <strong>' + hqSrvEsc(String(sum.trafficTotalPeriodMb != null ? sum.trafficTotalPeriodMb : '—')) + '</strong> MB</li>');
        var liRecent = '<li>가장 최근 일(' + hqSrvEsc(sum.latestDate || '—') + ') 트래픽 <strong>' +
          hqSrvEsc(String(sum.latestTrafficMb != null ? sum.latestTrafficMb : '—')) + '</strong> MB';
        if (sum.prevTrafficMb != null) {
          liRecent += ', 전일 <strong>' + hqSrvEsc(String(sum.prevTrafficMb)) + '</strong> MB';
        }
        if (sum.trafficDeltaMb != null) {
          liRecent += ', 증감 <strong>' + hqSrvEsc(String(sum.trafficDeltaMb)) + '</strong> MB';
          if (sum.trafficDeltaPct != null) {
            liRecent += ' (<strong>' + hqSrvEsc(String(sum.trafficDeltaPct)) + '</strong>%)';
          }
        }
        liRecent += '</li>';
        lines.push(liRecent);
        lines.push('<li>최근 31일 기준 일일 트래픽 최대: <strong>' + hqSrvEsc(String(sum.maxDayTrafficMb != null ? sum.maxDayTrafficMb : '—')) + '</strong> MB' +
          (sum.maxDayTrafficDate ? ' <span class="text-muted">(' + hqSrvEsc(sum.maxDayTrafficDate) + ')</span>' : '') + '</li>');
        lines.push('<li>일평균 트래픽(트래픽이 있었던 날만): <strong>' +
          hqSrvEsc(sum.avgDailyTrafficMb != null ? String(sum.avgDailyTrafficMb) : '—') + '</strong> MB</li>');
        lines.push('<li>메모리 일일 피크(%): 그래프 최근 값 <strong>' + hqSrvEsc(String(sum.memoryLatestPeakPct != null ? sum.memoryLatestPeakPct : '—')) +
          '</strong>%, 기간 최대 <strong>' + hqSrvEsc(String(sum.memoryPeriodMaxPeakPct != null ? sum.memoryPeriodMaxPeakPct : '—')) + '</strong>% (오른쪽 붉은 그래프)</li>');
        lines.push('</ul>');
        return lines.join('');
      }
      function refreshHqUsageCharts(pane, grain) {
        var sec = pane.querySelector('#hqSrvUsageSection');
        if (!sec) return;
        hqLoadChartJsOnce().then(function () {
          return window.PG_API.hqServerUsage(grain || 'daily');
        }).then(function (payload) {
          pane._hqUsageGrain = grain || 'daily';
          var labels = payload.labels || [];
          var tGb = payload.trafficSeriesGb || [];
          var mem = payload.memoryPeakSeriesPct || [];
          var sum = payload.summary || {};
          var c1 = pane.querySelector('#hqUsageChartMixed');
          var c2 = pane.querySelector('#hqUsageChartMem');
          if (pane._hqUsageChartMixed) {
            try { pane._hqUsageChartMixed.destroy(); } catch (e1) { /* ignore */ }
            pane._hqUsageChartMixed = null;
          }
          if (pane._hqUsageChartMem) {
            try { pane._hqUsageChartMem.destroy(); } catch (e2) { /* ignore */ }
            pane._hqUsageChartMem = null;
          }
          if (c1 && window.Chart) {
            pane._hqUsageChartMixed = new Chart(c1.getContext('2d'), {
              type: 'bar',
              data: {
                labels: labels,
                datasets: [
                  {
                    type: 'bar',
                    label: '트래픽 (송수신 합, GB)',
                    data: tGb,
                    backgroundColor: 'rgba(13, 110, 253, 0.45)',
                    borderColor: 'rgba(13, 110, 253, 0.9)',
                    borderWidth: 1,
                    yAxisID: 'y'
                  },
                  {
                    type: 'line',
                    label: '메모리 피크 (%)',
                    data: mem,
                    borderColor: 'rgb(220, 53, 69)',
                    backgroundColor: 'rgba(220, 53, 69, 0.06)',
                    borderWidth: 2,
                    tension: 0.2,
                    pointRadius: 2,
                    yAxisID: 'y1'
                  }
                ]
              },
              options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: { mode: 'index', intersect: false },
                plugins: { legend: { position: 'bottom' } },
                scales: {
                  y: {
                    type: 'linear',
                    position: 'left',
                    title: { display: true, text: 'GB' },
                    beginAtZero: true
                  },
                  y1: {
                    type: 'linear',
                    position: 'right',
                    min: 0,
                    max: 100,
                    title: { display: true, text: '%' },
                    grid: { drawOnChartArea: false }
                  }
                }
              }
            });
          }
          if (c2 && window.Chart) {
            pane._hqUsageChartMem = new Chart(c2.getContext('2d'), {
              type: 'line',
              data: {
                labels: labels,
                datasets: [{
                  label: '메모리 피크 (%)',
                  data: mem,
                  borderColor: 'rgb(220, 53, 69)',
                  backgroundColor: 'rgba(220, 53, 69, 0.12)',
                  borderWidth: 2,
                  tension: 0.2,
                  fill: true,
                  pointRadius: 2
                }]
              },
              options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: 'bottom' } },
                scales: {
                  y: {
                    min: 0,
                    max: 100,
                    title: { display: true, text: '%' }
                  }
                }
              }
            });
          }
          var sumEl = pane.querySelector('#hqUsageSummary');
          if (sumEl) sumEl.innerHTML = hqUsageFormatSummary(sum, pane._hqUsageGrain);
        }).catch(function () {
          var sumEl = pane.querySelector('#hqUsageSummary');
          if (sumEl) sumEl.innerHTML = '<p class="text-danger mb-0 small">차트 데이터를 불러오지 못했습니다. ADMIN·<code>/api/hq/serverUsage</code>·최신 JAR·DB V45를 확인하세요.</p>';
        });
      }
      function renderHqServerDashboard(data) {
        var genEl = pane.querySelector('#hqSrvGeneratedAt');
        var rawEl = pane.querySelector('#hqSrvJsonRaw');
        var cardsEl = pane.querySelector('#hqSrvCards');
        var alertEl = pane.querySelector('#hqSrvAlerts');
        var intEl = pane.querySelector('#hqSrvIntervalSec');
        updateHqMonCrossOriginHint();
        if (rawEl) {
          try { rawEl.textContent = JSON.stringify(data, null, 2); } catch (e) { rawEl.textContent = String(data); }
        }
        if (genEl) genEl.textContent = '조회 시각: ' + (data && data.generatedAt ? data.generatedAt : '—');
        if (intEl) {
          var secI = (data && data.uiAutoRefreshSeconds > 0) ? data.uiAutoRefreshSeconds : 120;
          var mi = Math.floor(secI / 60);
          var sc = secI % 60;
          intEl.textContent = sc === 0 ? (mi + '분') : (mi > 0 ? (mi + '분 ' + sc + '초') : (secI + '초'));
        }
        if (alertEl) {
          var alerts = data && data.health && Array.isArray(data.health.alerts) ? data.health.alerts : [];
          if (!alerts.length) alertEl.innerHTML = '';
          else {
            alertEl.innerHTML = '<div class="alert alert-danger py-2 small mb-2" role="alert"><strong>헬스 경고</strong><ul class="mb-0 mt-1 ps-3">' +
              alerts.map(function (a) { return '<li>' + hqSrvEsc(a) + '</li>'; }).join('') + '</ul></div>';
          }
        }
        if (!cardsEl) return;
        var host = (data && data.host) || {};
        var jvm = (data && data.jvm) || {};
        if (!data || !host || Object.keys(host).length === 0) {
          var errMsg = (data && data.error) ? String(data.error) : '데이터가 없습니다.';
          cardsEl.innerHTML = '<div class="hq-mon-card border-danger">' +
            '<h3 class="text-danger">대시보드 데이터를 불러오지 못했습니다</h3>' +
            '<p class="small mb-2">' + hqSrvEsc(errMsg) + '</p>' +
            '<p class="small text-muted mb-0">' +
            '[요약 새로고침]을 누르고, F12 Network에서 <code>/api/hq/serverManage</code> 응답을 확인하세요. ' +
            'ADMIN 권한·API 기준 URL·CORS(최신 JAR)를 점검하세요.</p></div>';
          return;
        }
        var disk = (data && data.disk) || {};
        var ssl = (data && data.ssl) || {};
        var nginx = (data && data.nginxStub) || {};
        var certbot = (data && data.certbot) || {};
        var health = (data && data.health) || {};
        var tm = host.memoryTotalMb != null ? host.memoryTotalMb : 0;
        var am = host.memoryAvailableMb != null ? host.memoryAvailableMb : 0;
        var hpct = tm > 0 ? Math.round(((tm - am) / tm) * 1000) / 10 : 0;
        var heapPct = Number(jvm.heapUsedPct) || 0;
        var dp = disk.ok ? (Number(disk.usedPct) || 0) : null;
        var files = certbot.renewalConfFiles || [];
        var timer = certbot.certbotTimer || {};
        var sslDays = ssl.status === 'OK' ? Number(ssl.daysRemaining) : null;
        var sslDayCls = sslDays != null && sslDays <= 7 ? ' hq-mon-ssl-warn-danger' : sslDays != null && sslDays <= 30 ? ' hq-mon-ssl-warn' : '';
        var stats = [];
        stats.push(hqMonStat('호스트명', host.hostname || '—', host.osFamily ? (host.osFamily + ' · ' + (host.osVersion || '')) : '', !!host.error));
        stats.push(hqMonStat('시스템 메모리', host.error ? '—' : (hpct + '%'), host.error ? hqSrvEsc(host.error) : ('가용 ' + am + ' / 총 ' + tm + ' MB'), hpct >= 90));
        stats.push(hqMonStat('JVM 힙', (jvm.heapUsedMb != null ? jvm.heapUsedMb : '—') + ' / ' + (jvm.heapMaxMb != null ? jvm.heapMaxMb : '—') + ' MB', 'Java ' + (jvm.javaVersion || '—'), heapPct >= 92));
        stats.push(hqMonStat('Load(1m) · CPU', jvm.systemLoadAverage != null ? String(jvm.systemLoadAverage) : '—', '논리 ' + (jvm.cpuCount || '—') + ' 코어', false));
        stats.push(hqMonStat('업타임', hqSrvFmtUptimeMs(jvm.uptimeMs), '', false));
        stats.push(hqMonStat('디스크 사용', dp != null ? (dp + '%') : '—', disk.ok ? hqSrvFmtBytes(disk.usedBytes) + ' / ' + hqSrvFmtBytes(disk.totalBytes) : hqSrvEsc(disk.error || '조회 불가'), dp != null && dp >= 90));
        stats.push(hqMonStat('certbot.timer', timer.active || '—', 'renewal .conf ' + files.length + '개', (timer.active || '').toLowerCase() !== 'active'));
        var gridHtml = '<div class="hq-mon-grid">' + stats.join('') + '</div>';
        var resolvedPath = (data && data.sslResolvedPath) ? String(data.sslResolvedPath) : '';
        var cfgPath = (data && data.serverManageSslCertPath) ? String(data.serverManageSslCertPath) : '';
        var leDom = (data && data.serverManageSslLeDomain) ? String(data.serverManageSslLeDomain) : '';
        var sslDl = '';
        if (ssl.status === 'OK') {
          var days = Number(ssl.daysRemaining);
          var barPct = Math.min(100, Math.max(0, (days / 90) * 100));
          var lv = days < 14 ? 'danger' : days < 30 ? 'warn' : 'ok';
          sslDl =
            '<dl class="hq-mon-ssl-dl">' +
            '<dt>실제 읽은 경로</dt><dd>' + hqSrvEsc(resolvedPath || cfgPath || '—') + '</dd>' +
            '<dt>DB 저장 경로</dt><dd>' + hqSrvEsc(cfgPath || '—') + '</dd>' +
            '<dt>LE live 폴더명</dt><dd>' + hqSrvEsc(leDom || '—') + '</dd>' +
            '<dt>Subject</dt><dd class="text-break">' + hqSrvEsc(ssl.subjectDn) + '</dd>' +
            '<dt>Issuer</dt><dd class="text-break">' + hqSrvEsc(ssl.issuerDn) + '</dd>' +
            '<dt>유효 기간</dt><dd>' + hqSrvEsc(ssl.notBefore) + ' ~ ' + hqSrvEsc(ssl.notAfter) + '</dd>' +
            '<dt>잔여 일수</dt><dd class="' + sslDayCls.replace(/^\s+/, '') + '"><strong>' + days + '</strong> 일</dd>' +
            '<dt>SHA-256</dt><dd class="font-monospace small text-break">' + hqSrvEsc(ssl.fingerprintSha256 || '—') + '</dd>' +
            '</dl>' + hqSrvProgress(barPct, lv);
          var sanList = Array.isArray(ssl.sanDnsNames) ? ssl.sanDnsNames : [];
          if (sanList.length) {
            sslDl += '<p class="small fw-semibold mt-2 mb-2">SAN — 브라우저 호스트명 (dNSName)</p>' +
              '<div class="table-responsive" style="max-height:200px;overflow:auto">' +
              '<table class="table table-sm table-bordered align-middle mb-0 text-center">' +
              '<thead class="table-light"><tr><th class="text-center" style="width:3.5rem">No.</th>' +
              '<th class="text-center">호스트명</th></tr></thead><tbody>' +
              sanList.map(function (h, idx) {
                return '<tr><td class="text-center text-muted">' + (idx + 1) + '</td>' +
                  '<td class="text-center font-monospace small">' + hqSrvEsc(h) + '</td></tr>';
              }).join('') +
              '</tbody></table></div>';
          }
        } else {
          sslDl = '<p class="small text-warning mb-2">' + hqSrvEsc(ssl.detail || ssl.status || '—') + '</p>' +
            '<dl class="hq-mon-ssl-dl"><dt>실제 읽은 경로</dt><dd>' + hqSrvEsc(resolvedPath || '—') + '</dd>' +
            '<dt>환경변수</dt><dd><code>PG_SSL_CERT_PATH</code> (선택)</dd></dl>';
        }
        var sslGuide = (data && data.sslOpsGuide) || {};
        var sslGuideHtml = '';
        if (sslGuide.dnsProviderNote || sslGuide.leSanNote || sslGuide.cloudflareNote) {
          sslGuideHtml = '<details class="mt-2 small"><summary class="text-muted user-select-none">운영 안내 (DNS·SAN·프록시)</summary>' +
            '<ul class="text-muted mb-0 mt-1 ps-3">' +
            (sslGuide.dnsProviderNote ? '<li>' + hqSrvEsc(sslGuide.dnsProviderNote) + '</li>' : '') +
            (sslGuide.leSanNote ? '<li>' + hqSrvEsc(sslGuide.leSanNote) + '</li>' : '') +
            (sslGuide.cloudflareNote ? '<li>' + hqSrvEsc(sslGuide.cloudflareNote) + '</li>' : '') +
            '</ul></details>';
        }
        var sslCard = '<div class="hq-mon-card">' +
          '<h3>SSL 인증서</h3>' +
          '<p class="hq-mon-card-desc">Let’s Encrypt <code>fullchain.pem</code> 를 읽어 만료·SAN·지문을 표시합니다. 상단 폼의 LE live 폴더명(인증서 이름)을 저장하면 경로가 맞춰집니다. 도메인 URL과 SAN 대조는 <strong>도메인구성</strong> 화면을 사용하세요.</p>' +
          sslDl + sslGuideHtml + '</div>';
        var cbHtml = '<h3>Certbot · 갱신</h3>' +
          '<p class="hq-mon-card-desc"><code>certbot.timer</code> 가 주기적으로 <code>certbot renew</code> 를 실행합니다. 만료 30일 전부터 갱신이 시도됩니다. 서브도메인 추가 시에는 수동으로 <code>certbot --nginx -d …</code> 로 인증서를 확장한 뒤 Nginx를 리로드하세요.</p>' +
          '<p class="small mb-1"><strong>timer</strong> ' + hqSrvEsc(timer.active || '—') + '</p>' +
          '<p class="small text-muted text-break mb-2">다음 실행(원시): ' + hqSrvEsc(timer.next || '—') + '</p>' +
          '<p class="small fw-semibold mb-1">renewal/*.conf (' + files.length + ')</p>' +
          '<ul class="small mb-0 ps-3" style="max-height:120px;overflow:auto">' +
          files.slice(0, 40).map(function (f) { return '<li>' + hqSrvEsc(f) + '</li>'; }).join('') +
          (files.length > 40 ? '<li>… 외 ' + (files.length - 40) + '개</li>' : '') + '</ul>';
        var nxBody = '<p class="small mb-1">상태: <strong>' + hqSrvEsc(nginx.status) + '</strong></p>';
        if (nginx.bodyPreview) nxBody += '<pre class="small bg-light border rounded p-2 mb-0" style="max-height:140px;overflow:auto">' + hqSrvEsc(nginx.bodyPreview) + '</pre>';
        else if (nginx.detail) nxBody += '<p class="small text-muted mb-0">' + hqSrvEsc(nginx.detail) + '</p>';
        if (data && data.nginxStubStatusUrlConfigured === false && nginx.status === 'SKIPPED') {
          nxBody = '<p class="small text-muted mb-0">stub_status URL 미설정 (<code>NGINX_STUB_STATUS_URL</code> 또는 <code>app.serverManage.nginxStubStatusUrl</code>).</p>';
        }
        var nxHtml = '<h3>Nginx stub</h3><p class="hq-mon-card-desc">stub_status 연동 시 활성 접속 등을 표시합니다.</p>' + nxBody;
        var hRows = health.rows || [];
        var tableRows = hRows.map(function (r) {
          var crit = (r && r.criteria != null && String(r.criteria) !== '') ? String(r.criteria) : '—';
          return '<tr><td>' + hqSrvEsc(r.label) + '</td><td class="hq-mon-health-criteria text-muted">' + hqSrvEsc(crit) + '</td><td>' + hqSrvEsc(r.value) + '</td><td>' + hqSrvBadge(r.status) + '</td></tr>';
        }).join('');
        var ctr = (data && data.serverManageContract) || {};
        var sugMb = data && data.serverManageSuggestedTrafficUsedMb;
        var trUsedCard = ctr.trafficUsedMb != null
          ? hqSrvFmtGbFromMb(ctr.trafficUsedMb)
          : (sugMb != null && sugMb > 0 ? ('미저장 · 앱 수집 추정 ' + hqSrvFmtGbFromMb(sugMb)) : '미입력');
        var ctrCard = '<div class="hq-mon-card hq-mon-contract-card">' +
          '<h3>호스팅 약정</h3>' +
          '<p class="hq-mon-card-desc">상단 <strong>호스팅 약정</strong> 폼에서 저장한 값입니다. 표시는 GB이며 서버에는 MB로 저장됩니다.</p>' +
          '<p class="small mb-1">디스크 약정: <strong>' + hqSrvFmtGbFromMb(ctr.diskMb) + '</strong> · 트래픽 약정: <strong>' +
          (ctr.trafficMb != null ? hqSrvFmtGbFromMb(ctr.trafficMb) + ' (기간당)' : '—') + '</strong></p>' +
          '<p class="small text-muted mb-1">트래픽 누적 입력: <strong>' + trUsedCard + '</strong></p>' +
          '<p class="small text-muted mb-0">약정기간: ' + hqSrvEsc((ctr.periodStart || '—') + ' ~ ' + (ctr.periodEnd || '—')) + '</p></div>';
        var healthHtml = '<div class="hq-mon-card hq-mon-health-table">' +
          '<h3>헬스 요약</h3>' +
          '<p class="hq-mon-card-desc">일반 항목은 NOTI와 동일한 비율 임계치입니다. <strong>약정 디스크·트래픽</strong> 행은 약정(GB) 대비 사용률(주의 ≥75%, 위험 ≥90%)입니다.</p>' +
          '<div class="table-responsive"><table class="table table-sm table-bordered align-middle mb-0">' +
          '<thead class="table-light"><tr><th>항목</th><th>양호·주의·위험 기준</th><th>값</th><th style="width:88px">상태</th></tr></thead><tbody>' + tableRows + '</tbody></table></div>' +
          '<p class="small text-muted mt-2 mb-0">종합: ' + hqSrvBadge(health.worstStatus || 'ok') + '</p></div>';
        cardsEl.innerHTML = gridHtml + sslCard +
          '<div class="hq-mon-row2"><div class="hq-mon-card">' + cbHtml + '</div><div class="hq-mon-card">' + nxHtml + '</div></div>' +
          ctrCard + healthHtml;
        if (typeof requestAnimationFrame === 'function') {
          requestAnimationFrame(function () { refreshHqUsageCharts(pane, pane._hqUsageGrain || 'daily'); });
        } else {
          setTimeout(function () { refreshHqUsageCharts(pane, pane._hqUsageGrain || 'daily'); }, 0);
        }
      }
      function applyServerFormFromSummary(data) {
        if (!data) return;
        ['serverManageSslCertPath', 'serverManageSslLeDomain', 'serverManageContractStart', 'serverManageContractEnd'].forEach(function (k) {
          var inp = pane.querySelector('[name="' + k + '"]');
          if (!inp) return;
          var v = data[k];
          if (v === undefined || v === null || v === '') inp.value = '';
          else inp.value = v;
        });
        var refInp = pane.querySelector('[name="serverManageUiRefreshMin"]');
        if (refInp) {
          var rv = data.serverManageUiRefreshSec;
          if (rv !== undefined && rv !== null && rv !== '') {
            var sec0 = Number(rv);
            if (!isNaN(sec0) && sec0 >= 15) {
              var minVal = sec0 / 60;
              refInp.value = (Math.round(minVal * 100) / 100).toString().replace(/\.?0+$/, '');
            } else refInp.value = '';
          } else refInp.value = '';
        }
        var dIn = pane.querySelector('[name="serverManageContractDiskGb"]');
        if (dIn) dIn.value = hqSrvMbToGbInput(data.serverManageContractDiskMb);
        var tIn = pane.querySelector('[name="serverManageContractTrafficGb"]');
        if (tIn) tIn.value = hqSrvMbToGbInput(data.serverManageContractTrafficMb);
        var uIn = pane.querySelector('[name="serverManageTrafficUsedGb"]');
        if (uIn) {
          var tu = data.serverManageTrafficUsedMb;
          if (tu !== undefined && tu !== null && tu !== '') {
            uIn.value = hqSrvMbToGbInput(tu);
          } else if (data.serverManageSuggestedTrafficUsedMb != null && data.serverManageSuggestedTrafficUsedMb > 0) {
            uIn.value = hqSrvMbToGbInput(data.serverManageSuggestedTrafficUsedMb);
          } else {
            uIn.value = '';
          }
        }
      }
      function scheduleDataRefresh() {
        hqSrvClearTimers();
        var cb = pane.querySelector('#hqSrvAutoRefresh');
        var auto = !cb || cb.checked;
        var sec = Math.max(15, pane._hqSrvRefreshSec || 120);
        if (auto) {
          pane._serverManageTimer = setInterval(function () { loadServerSummary(false); }, sec * 1000);
        }
        pane._hqSrvNextRefreshAt = Date.now() + sec * 1000;
        pane._hqSrvCountdownTimer = setInterval(function () {
          var el = pane.querySelector('#hqSrvCountdown');
          var box = pane.querySelector('#hqSrvAutoRefresh');
          if (!el) return;
          if (!box || !box.checked) {
            el.textContent = '자동 갱신 꺼짐 · [요약 새로고침]으로 수동 조회';
            el.className = 'small fw-semibold text-secondary';
            return;
          }
          el.className = 'small fw-semibold text-primary';
          var rem = Math.max(0, Math.ceil((pane._hqSrvNextRefreshAt - Date.now()) / 1000));
          if (rem >= 60) {
            el.textContent = '다음 자동 갱신까지 약 ' + Math.floor(rem / 60) + '분 ' + (rem % 60) + '초';
          } else {
            el.textContent = '다음 자동 갱신까지 약 ' + rem + '초';
          }
        }, 1000);
      }
      function loadServerSummary(showDimm) {
        if (showDimm && dimmSrv) dimmSrv.style.display = 'flex';
        window.PG_API.hqServerManage().then(function (data) {
          applyServerFormFromSummary(data);
          renderHqServerDashboard(data);
          pane._hqSrvRefreshSec = (data && data.uiAutoRefreshSeconds > 0) ? data.uiAutoRefreshSeconds : 120;
          scheduleDataRefresh();
        }).catch(function (err) {
          hqSrvClearTimers();
          renderHqServerDashboard({
            error: err && err.message ? err.message : '조회 실패 (ADMIN 권한·네트워크 확인)',
            health: { alerts: [err && err.message ? err.message : '조회 실패'], rows: [], worstStatus: 'danger' }
          });
          var cEl = pane.querySelector('#hqSrvCountdown');
          if (cEl) {
            cEl.textContent = '조회 실패 — [요약 새로고침]을 눌러 주세요';
            cEl.className = 'small text-danger fw-semibold';
          }
        }).finally(function () {
          if (showDimm && dimmSrv) dimmSrv.style.display = 'none';
        });
      }
      loadServerSummary(true);
      if (!pane._hqSrvPaneChangeBound) {
        pane._hqSrvPaneChangeBound = true;
        pane.addEventListener('change', function (ev) {
          if (ev.target && ev.target.id === 'hqSrvAutoRefresh') {
            scheduleDataRefresh();
          }
        });
      }
      if (!pane._hqUsageGrainBound) {
        pane._hqUsageGrainBound = true;
        pane.addEventListener('click', function (ev) {
          var btn = ev.target && ev.target.closest && ev.target.closest('[data-hq-usage-grain]');
          if (!btn || !pane.contains(btn)) return;
          var g = btn.getAttribute('data-hq-usage-grain') || 'daily';
          pane._hqUsageGrain = g;
          pane.querySelectorAll('[data-hq-usage-grain]').forEach(function (b) {
            b.classList.toggle('active', b.getAttribute('data-hq-usage-grain') === g);
          });
          refreshHqUsageCharts(pane, g);
        });
      }
      var srvSave = pane.querySelector('#hqServerManageSaveBtn');
      var srvTopSave = pane.querySelector('#hqServerManageTopSaveBtn');
      var srvRef = pane.querySelector('#hqServerManageRefreshBtn');
      function setHqSrvInlineMsg(text, kind) {
        var el = pane.querySelector('#hqSrvInlineMsg');
        if (!el) return;
        el.textContent = text || '';
        el.className = 'small mt-2';
        if (kind === 'success') el.className += ' text-success';
        else if (kind === 'error') el.className += ' text-danger';
        else el.className += ' text-muted';
      }
      function runHqServerManageSave() {
        setHqSrvInlineMsg('', '');
        var fd = {};
        pane.querySelectorAll('input, select, textarea').forEach(function (el) { if (el.name) fd[el.name] = el.value; });
        var gDisk = fd.serverManageContractDiskGb;
        var gTrf = fd.serverManageContractTrafficGb;
        var gUsed = fd.serverManageTrafficUsedGb;
        var minR = fd.serverManageUiRefreshMin;
        delete fd.serverManageContractDiskGb;
        delete fd.serverManageContractTrafficGb;
        delete fd.serverManageTrafficUsedGb;
        delete fd.serverManageUiRefreshMin;
        fd.serverManageContractDiskMb = hqSrvGbToMbContract(gDisk);
        fd.serverManageContractTrafficMb = hqSrvGbToMbContract(gTrf);
        var usedMb = hqSrvGbToMbTrafficUsed(gUsed);
        fd.serverManageTrafficUsedMb = usedMb;
        if (minR !== undefined && minR !== null && String(minR).trim() !== '') {
          fd.serverManageUiRefreshSec = hqSrvMinToSecRefresh(minR);
        } else {
          fd.serverManageUiRefreshSec = '';
        }
        if (dimmSrv) dimmSrv.style.display = 'flex';
        window.PG_API.hqServerManageSave(fd).then(function () {
          setHqSrvInlineMsg('서버관리 설정(SSL·호스팅 약정·갱신 간격)이 저장되었습니다. 대시보드가 갱신되었습니다.', 'success');
          return window.PG_API.hqServerManage();
        }).then(function (data) {
          applyServerFormFromSummary(data);
          renderHqServerDashboard(data);
          pane._hqSrvRefreshSec = (data && data.uiAutoRefreshSeconds > 0) ? data.uiAutoRefreshSeconds : 120;
          scheduleDataRefresh();
        }).catch(function (e) {
          setHqSrvInlineMsg(e && e.message ? e.message : '저장 실패', 'error');
        }).finally(function () { if (dimmSrv) dimmSrv.style.display = 'none'; });
      }
      if (srvSave && !srvSave._bound) {
        srvSave._bound = true;
        srvSave.addEventListener('click', runHqServerManageSave);
      }
      if (srvTopSave && !srvTopSave._bound) {
        srvTopSave._bound = true;
        srvTopSave.addEventListener('click', runHqServerManageSave);
      }
      if (srvRef && !srvRef._bound) {
        srvRef._bound = true;
        srvRef.addEventListener('click', function () {
          loadServerSummary(true);
        });
      }
    }
    /** 영업일설정: 탭 pane은 유지되고 innerHTML만 갈아끼우므로, 재진입 시에도 매번 바인딩·목록조회·달력 init 필요. pane 클릭 위임은 한 번만 등록. */
    if (url === '/hq/businessDaySetting') {
      var st = pane._hqBizdayState || (pane._hqBizdayState = { manualEntries: [], currentList: [], currentEditingId: '', manualEditIdx: null });
      st.manualEntries = [];
      st.currentList = [];
      st.currentEditingId = '';
      st.manualEditIdx = null;
      var formBiz = pane.querySelector('form');
      var nameEl = formBiz ? formBiz.querySelector('[name="hqBizdayProfileName"]') : null;
      var countryEl = formBiz ? formBiz.querySelector('[name="holidayCountryCodes"]') : null;
      var extraHidden = formBiz ? (formBiz.querySelector('#hqBizdayExtraDatesHidden') || formBiz.querySelector('[name="businessHolidayExtraDates"]')) : null;
      var manualJsonHidden = formBiz ? formBiz.querySelector('#hqBizdayManualEntriesJson') : null;
      var manualTbody = pane.querySelector('#hqBizdayManualTbody');
      var tbodyBiz = pane.querySelector('#hqBizdayProfileTbody');
      var saveBtnBiz = pane.querySelector('#hqBizdayProfileSaveBtn');
      var newBtnBiz = pane.querySelector('#hqBizdayProfileNewBtn');
      var dimmBiz = document.getElementById('dimm');

      function hqExpandYmdRange(fromStr, toStr) {
        var out = [];
        if (!fromStr || String(fromStr).length < 10) return out;
        var fs = String(fromStr).substring(0, 10);
        if (!/^\d{4}-\d{2}-\d{2}$/.test(fs)) return out;
        var ts = (toStr && String(toStr).length >= 10 && /^\d{4}-\d{2}-\d{2}$/.test(String(toStr).substring(0, 10)))
          ? String(toStr).substring(0, 10) : fs;
        var a = fs.split('-').map(Number);
        var b = ts.split('-').map(Number);
        var d = new Date(a[0], a[1] - 1, a[2]);
        var end = new Date(b[0], b[1] - 1, b[2]);
        if (end < d) { var tmp = d; d = end; end = tmp; }
        var pad2 = function (n) { return n < 10 ? '0' + n : String(n); };
        while (d <= end) {
          out.push(d.getFullYear() + '-' + pad2(d.getMonth() + 1) + '-' + pad2(d.getDate()));
          d.setDate(d.getDate() + 1);
        }
        return out;
      }

      /** hidden 비영업일 문자열 → { 'yyyy-MM-dd': true } */
      function hqBizdayParseExtraMap(text) {
        var map = {};
        String(text || '').split(/\r?\n/).forEach(function (line) {
          var m = line.trim().match(/^(\d{4}-\d{2}-\d{2})/);
          if (m) map[m[1]] = true;
        });
        return map;
      }
      function hqBizdayExtraMapToLines(map) {
        return Object.keys(map).sort().join('\n');
      }

      /** 달력·프리셋으로 쌓인 일자 + 수동 구간을 합집합으로 유지 (기존: 수동만 넣어 프리셋 토·일이 저장 시 삭제되던 버그 수정) */
      function syncHiddenFromManualEntries() {
        var set = hqBizdayParseExtraMap(extraHidden && extraHidden.value);
        st.manualEntries.forEach(function (e) {
          hqExpandYmdRange(e.fromDate, e.toDate || e.fromDate).forEach(function (day) { set[day] = true; });
        });
        if (extraHidden) extraHidden.value = hqBizdayExtraMapToLines(set);
        if (manualJsonHidden) manualJsonHidden.value = JSON.stringify(st.manualEntries);
      }

      function clearManualRangeInputs() {
        var rf = pane.querySelector('#hqBizdayRangeFrom');
        var rt = pane.querySelector('#hqBizdayRangeTo');
        var rk = pane.querySelector('#hqBizdayRangeKind');
        var rn = pane.querySelector('#hqBizdayRangeNote');
        if (rf) rf.value = '';
        if (rt) rt.value = '';
        if (rk) rk.value = '공휴일';
        if (rn) rn.value = '';
      }

      function refreshManualRangeFormUi() {
        var addBtn = pane.querySelector('#hqBizdayRangeAddBtn');
        var cancelBtn = pane.querySelector('#hqBizdayRangeCancelEditBtn');
        var editing = st.manualEditIdx != null && !isNaN(st.manualEditIdx) && st.manualEditIdx >= 0 && st.manualEditIdx < st.manualEntries.length;
        if (addBtn) addBtn.textContent = editing ? '수정 반영' : '구간 추가';
        if (cancelBtn) cancelBtn.classList.toggle('d-none', !editing);
      }

      function renderManualTable() {
        if (!manualTbody) return;
        if (!st.manualEntries.length) {
          manualTbody.innerHTML = '<tr class="hq-bizday-manual-empty"><td colspan="6" class="text-center text-muted">등록된 구간이 없습니다.</td></tr>';
          refreshManualRangeFormUi();
          return;
        }
        var html = '';
        st.manualEntries.forEach(function (e, idx) {
          var hi = st.manualEditIdx === idx ? ' table-info' : '';
          html += '<tr data-manual-idx="' + idx + '" class="' + hi.trim() + '"><td>' + (e.fromDate || '') + '</td><td>' + (e.toDate || e.fromDate || '') + '</td>' +
            '<td>' + (e.holidayKind || '') + '</td><td>' + escapeHtml(String(e.note || '')) + '</td>' +
            '<td><button type="button" class="btn btn-sm btn-outline-primary hq-bizday-manual-edit" data-idx="' + idx + '">수정</button></td>' +
            '<td><button type="button" class="btn btn-sm btn-outline-danger hq-bizday-manual-del" data-idx="' + idx + '">삭제</button></td></tr>';
        });
        manualTbody.innerHTML = html;
        refreshManualRangeFormUi();
      }

      function escapeHtml(s) {
        return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
      }

      function loadManualFromItem(item) {
        if (extraHidden) {
          extraHidden.value = (item && item.businessHolidayExtraDates != null) ? String(item.businessHolidayExtraDates) : '';
        }
        st.manualEntries = [];
        if (item && Array.isArray(item.holidayManualEntries) && item.holidayManualEntries.length) {
          st.manualEntries = item.holidayManualEntries.map(function (e) {
            return {
              fromDate: (e.fromDate != null) ? String(e.fromDate).substring(0, 10) : '',
              toDate: (e.toDate != null) ? String(e.toDate).substring(0, 10) : '',
              holidayKind: (e.holidayKind != null) ? String(e.holidayKind) : '공휴일',
              note: (e.note != null) ? String(e.note) : ''
            };
          });
        }
        syncHiddenFromManualEntries();
        st.manualEditIdx = null;
        renderManualTable();
      }

      /** yyyy-MM-dd 가 토·일이면 true (공식공휴일에 포함되는 주말). */
      function bizdayIsWeekendYmd(ymd) {
        if (!ymd || String(ymd).length < 10) return false;
        var p = String(ymd).substring(0, 10).split('-').map(Number);
        if (p.length !== 3 || isNaN(p[0]) || isNaN(p[1]) || isNaN(p[2])) return false;
        var dt = new Date(p[0], p[1] - 1, p[2]);
        var w = dt.getDay();
        return w === 0 || w === 6;
      }

      /**
       * 목록 열 집계: 서버가 내려주면 holidayCount* 사용(토·일·해당국 법정 프리셋 = 공식, 나머지 저장일 = 추가).
       * 구 API는 아래 클라이언트 추정 로직으로 폴백.
       */
      function bizdayHolidayKindCounts(it) {
        if (it != null && it.holidayCountTotal != null && it.holidayCountOfficial != null && it.holidayCountAdditional != null) {
          var t0 = Number(it.holidayCountTotal);
          var o0 = Number(it.holidayCountOfficial);
          var a0 = Number(it.holidayCountAdditional);
          if (!isNaN(t0) && t0 >= 0 && !isNaN(o0) && !isNaN(a0)) {
            return { official: o0, added: a0, total: t0 };
          }
        }
        var totalMap = {};
        String((it && it.businessHolidayExtraDates) || '').split(/\r?\n/).forEach(function (line) {
          var m = line.trim().match(/^(\d{4}-\d{2}-\d{2})/);
          if (m) totalMap[m[1]] = true;
        });
        var days = Object.keys(totalMap);
        var total = days.length;
        var cc = (it && it.countryCode != null) ? String(it.countryCode).trim().toUpperCase() : '';
        if (cc === 'GLOBAL') {
          var offG = 0;
          var addG = 0;
          days.forEach(function (d) {
            if (bizdayIsWeekendYmd(d)) offG += 1;
            else addG += 1;
          });
          return { official: offG, added: addG, total: total };
        }
        var addedMap = {};
        var arr = (it && it.holidayManualEntries) || [];
        arr.forEach(function (e) {
          hqExpandYmdRange(e.fromDate, e.toDate || e.fromDate).forEach(function (day) {
            addedMap[day] = true;
          });
        });
        var added = 0;
        Object.keys(addedMap).forEach(function (d) {
          if (totalMap[d] && !bizdayIsWeekendYmd(d)) added += 1;
        });
        var official = total - added;
        return { official: official < 0 ? 0 : official, added: added, total: total };
      }
      function renderList(list) {
        st.currentList = Array.isArray(list) ? list : [];
        if (!tbodyBiz) return;
        if (!st.currentList.length) {
          tbodyBiz.innerHTML = '<tr><td colspan="11" class="text-center text-muted">저장된 설정이 없습니다.</td></tr>';
          return;
        }
        var html = '';
        var sel = String(st.currentEditingId || '');
        st.currentList.forEach(function (it, i) {
          var hc = bizdayHolidayKindCounts(it);
          var createdDisp = (it.createdAt && String(it.createdAt).trim()) ? String(it.createdAt).trim().substring(0, 10)
            : ((it.updatedAt && String(it.updatedAt).trim()) ? String(it.updatedAt).trim().substring(0, 10) : '-');
          var updatedDisp = (it.updatedAt && String(it.updatedAt).trim()) ? String(it.updatedAt).trim().substring(0, 10) : '-';
          var idStr = String(it.id || '');
          var rowHi = (sel && sel === idStr) ? ' table-active' : '';
          html += '<tr class="hq-bizday-row' + rowHi + '" data-id="' + idStr + '" style="cursor:pointer">' +
            '<td>' + (i + 1) + '</td><td>' + (it.name || '') + '</td><td>' + (it.countryCode || '') + '</td>' +
            '<td>' + (it.createdBy || '-') + '</td>' +
            '<td class="text-center align-middle">' + hc.official + '</td><td class="text-center align-middle">' + hc.added + '</td><td class="text-center align-middle">' + hc.total + '</td>' +
            '<td>' + createdDisp + '</td><td>' + updatedDisp + '</td>' +
            '<td class="text-center p-1 align-middle"><button type="button" class="btn btn-sm btn-outline-primary hq-bizday-profile-edit" data-id="' + idStr + '">수정</button></td>' +
            '<td class="text-center p-1 align-middle"><button type="button" class="btn btn-sm btn-outline-danger hq-bizday-profile-del" data-id="' + idStr + '">삭제</button></td></tr>';
        });
        tbodyBiz.innerHTML = html;
      }
      function loadBizdayProfileIntoEditor(item) {
        if (!item) return;
        st.currentEditingId = item.id || '';
        if (nameEl) nameEl.value = item.name || '';
        if (countryEl) countryEl.value = item.countryCode || 'KR';
        loadManualFromItem(item);
        if (window.PG_HQ_HOLIDAY && typeof window.PG_HQ_HOLIDAY.init === 'function') window.PG_HQ_HOLIDAY.init(pane, { force: true });
        renderList(st.currentList);
      }
      function clearEditor() {
        st.currentEditingId = '';
        st.manualEntries = [];
        if (nameEl) nameEl.value = '';
        if (countryEl) countryEl.value = 'KR';
        if (extraHidden) extraHidden.value = '';
        syncHiddenFromManualEntries();
        st.manualEditIdx = null;
        clearManualRangeInputs();
        renderManualTable();
        if (window.PG_HQ_HOLIDAY && typeof window.PG_HQ_HOLIDAY.init === 'function') window.PG_HQ_HOLIDAY.init(pane, { force: true });
        renderList(st.currentList);
      }
      function loadList() {
        if (dimmBiz) dimmBiz.style.display = 'flex';
        window.PG_API.hqBusinessDaySettings().then(function (list) {
          renderList(list || []);
          if (window.PG_HQ_HOLIDAY && typeof window.PG_HQ_HOLIDAY.init === 'function') window.PG_HQ_HOLIDAY.init(pane, { force: true });
        }).catch(function (e) {
          if (tbodyBiz) tbodyBiz.innerHTML = '<tr><td colspan="11" class="text-center text-danger">' + (e && e.message ? e.message : '조회 실패') + '</td></tr>';
        }).finally(function () { if (dimmBiz) dimmBiz.style.display = 'none'; });
      }
      /** 신규·저장·삭제 공통: 1차 확인 → 2차 최종 확인(취소 시 중단) */
      function hqBizdayTwoStepConfirm(msgStep1, msgStep2) {
        if (!window.confirm(msgStep1)) return false;
        if (!window.confirm(msgStep2)) return false;
        return true;
      }
      if (newBtnBiz) newBtnBiz.addEventListener('click', function () {
        if (!hqBizdayTwoStepConfirm(
          '[1단계] 신규를 누르면 편집 중인 이름·기준국가·휴일 구간·달력에 반영된 데이터가 모두 초기화됩니다.\n진행하시겠습니까? (취소 시 아무 변화 없음)',
          '[2단계] 최종 확인: 모든 입력을 비우고 신규 작성 화면으로 전환합니다.\n정말 진행하시겠습니까?'
        )) return;
        clearEditor();
      });
      var addRangeBtn = pane.querySelector('#hqBizdayRangeAddBtn');
      if (addRangeBtn) {
        addRangeBtn.addEventListener('click', function () {
          var rf = pane.querySelector('#hqBizdayRangeFrom');
          var rt = pane.querySelector('#hqBizdayRangeTo');
          var rk = pane.querySelector('#hqBizdayRangeKind');
          var rn = pane.querySelector('#hqBizdayRangeNote');
          var from = rf && rf.value ? rf.value.trim() : '';
          var to = rt && rt.value ? rt.value.trim() : '';
          if (!from) { alert('시작일을 선택하세요.'); return; }
          if (!to) to = from;
          if (from > to) { var x = from; from = to; to = x; }
          var entry = {
            fromDate: from,
            toDate: to,
            holidayKind: (rk && rk.value) ? rk.value : '공휴일',
            note: (rn && rn.value) ? rn.value.trim() : ''
          };
          var ix = st.manualEditIdx;
          if (ix != null && !isNaN(ix) && ix >= 0 && ix < st.manualEntries.length) {
            st.manualEntries[ix] = entry;
            st.manualEditIdx = null;
            clearManualRangeInputs();
          } else {
            st.manualEntries.push(entry);
          }
          syncHiddenFromManualEntries();
          renderManualTable();
          if (window.PG_HQ_HOLIDAY && typeof window.PG_HQ_HOLIDAY.init === 'function') window.PG_HQ_HOLIDAY.init(pane, { force: true });
        });
      }
      var cancelRangeEditBtn = pane.querySelector('#hqBizdayRangeCancelEditBtn');
      if (cancelRangeEditBtn) {
        cancelRangeEditBtn.addEventListener('click', function () {
          st.manualEditIdx = null;
          clearManualRangeInputs();
          refreshManualRangeFormUi();
          renderManualTable();
        });
      }
      if (saveBtnBiz) saveBtnBiz.addEventListener('click', function () {
        var name = nameEl && nameEl.value ? nameEl.value.trim() : '';
        var cc = countryEl && countryEl.value ? countryEl.value : 'KR';
        syncHiddenFromManualEntries();
        var extra = extraHidden && extraHidden.value ? extraHidden.value : '';
        if (!name) { alert('이름을 입력하세요.'); return; }
        if (!hqBizdayTwoStepConfirm(
          '[1단계] 현재 화면의 영업일 설정을 서버에 저장합니다.\n진행하시겠습니까? (취소 시 저장 안 함)',
          '[2단계] 최종 확인: 저장하면 목록 및 적용 데이터가 갱신됩니다.\n저장하시겠습니까?'
        )) return;
        if (dimmBiz) dimmBiz.style.display = 'flex';
        window.PG_API.hqBusinessDaySettingsSave({
          mode: 'UPSERT',
          id: st.currentEditingId,
          name: name,
          countryCode: cc,
          businessHolidayExtraDates: extra,
          holidayManualEntries: st.manualEntries
        }).then(function (data) {
          alert((data && data.message) ? data.message : '저장되었습니다.');
          if (data && data.id) st.currentEditingId = String(data.id);
          renderList((data && data.list) ? data.list : []);
          if (st.currentEditingId && data && data.list) {
            var found = data.list.find(function (x) { return String(x.id || '') === st.currentEditingId; });
            if (found) loadManualFromItem(found);
          }
        }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimmBiz) dimmBiz.style.display = 'none'; });
      });
      function runHqBizdayDeleteById(deleteId) {
        if (!deleteId) return;
        var victim = st.currentList.filter(function (x) { return String(x.id || '') === String(deleteId); })[0];
        var vname = victim && victim.name ? String(victim.name) : String(deleteId);
        if (!hqBizdayTwoStepConfirm(
          '[1단계] 영업일 설정 [' + vname + ']을(를) 삭제합니다.\n진행하시겠습니까? (취소 시 삭제 안 함)',
          '[2단계] 최종 확인: 삭제 후에는 복구할 수 없습니다.\n삭제하시겠습니까?'
        )) return;
        if (dimmBiz) dimmBiz.style.display = 'flex';
        window.PG_API.hqBusinessDaySettingsSave({ mode: 'DELETE', id: deleteId }).then(function (data) {
          alert((data && data.message) ? data.message : '삭제되었습니다.');
          if (String(st.currentEditingId) === String(deleteId)) clearEditor();
          renderList((data && data.list) ? data.list : []);
        }).catch(function (e) { alert(e && e.message ? e.message : '삭제 실패'); }).finally(function () { if (dimmBiz) dimmBiz.style.display = 'none'; });
      }
      pane._hqBizdayRuntime = {
        st: st,
        syncHiddenFromManualEntries: syncHiddenFromManualEntries,
        hqExpandYmdRange: hqExpandYmdRange,
        hqBizdayParseExtraMap: hqBizdayParseExtraMap,
        hqBizdayExtraMapToLines: hqBizdayExtraMapToLines,
        renderManualTable: renderManualTable,
        loadBizdayProfileIntoEditor: loadBizdayProfileIntoEditor,
        runHqBizdayDeleteById: runHqBizdayDeleteById,
        loadManualFromItem: loadManualFromItem,
        clearManualRangeInputs: clearManualRangeInputs,
        refreshManualRangeFormUi: refreshManualRangeFormUi
      };
      if (!pane._hqBizdayProfileClickDelegated) {
        pane._hqBizdayProfileClickDelegated = true;
        pane.addEventListener('click', function (ev) {
          var p = ev.currentTarget;
          if (p.getAttribute('formurl') !== '/hq/businessDaySetting') return;
          var rt = p._hqBizdayRuntime;
          if (!rt || !rt.st) return;
          var st0 = rt.st;
          var editM = ev.target && ev.target.closest ? ev.target.closest('.hq-bizday-manual-edit') : null;
          if (editM && p.contains(editM)) {
            var eix = parseInt(editM.getAttribute('data-idx'), 10);
            if (!isNaN(eix) && st0.manualEntries[eix]) {
              var ent = st0.manualEntries[eix];
              var rf0 = p.querySelector('#hqBizdayRangeFrom');
              var rt0 = p.querySelector('#hqBizdayRangeTo');
              var rk0 = p.querySelector('#hqBizdayRangeKind');
              var rn0 = p.querySelector('#hqBizdayRangeNote');
              if (rf0) rf0.value = ent.fromDate || '';
              if (rt0) rt0.value = ent.toDate || ent.fromDate || '';
              if (rk0) rk0.value = ent.holidayKind || '공휴일';
              if (rn0) rn0.value = ent.note || '';
              st0.manualEditIdx = eix;
              rt.refreshManualRangeFormUi();
              rt.renderManualTable();
            }
            return;
          }
          var delM = ev.target && ev.target.closest ? ev.target.closest('.hq-bizday-manual-del') : null;
          if (delM && p.contains(delM)) {
            var ix = parseInt(delM.getAttribute('data-idx'), 10);
            if (!isNaN(ix)) {
              var editingIdx = st0.manualEditIdx;
              var exH = p.querySelector('#hqBizdayExtraDatesHidden') || p.querySelector('[name="businessHolidayExtraDates"]');
              var removedEnt = st0.manualEntries[ix];
              var removedDays = removedEnt ? rt.hqExpandYmdRange(removedEnt.fromDate, removedEnt.toDate || removedEnt.fromDate) : [];
              st0.manualEntries.splice(ix, 1);
              if (exH && removedDays.length) {
                var mset = rt.hqBizdayParseExtraMap(exH.value);
                removedDays.forEach(function (d) { delete mset[d]; });
                st0.manualEntries.forEach(function (e) {
                  rt.hqExpandYmdRange(e.fromDate, e.toDate || e.fromDate).forEach(function (day) { mset[day] = true; });
                });
                exH.value = rt.hqBizdayExtraMapToLines(mset);
              }
              if (editingIdx != null && !isNaN(editingIdx)) {
                if (editingIdx === ix) {
                  st0.manualEditIdx = null;
                  rt.clearManualRangeInputs();
                } else if (editingIdx > ix) {
                  st0.manualEditIdx = editingIdx - 1;
                }
              }
              rt.syncHiddenFromManualEntries();
              rt.renderManualTable();
              rt.refreshManualRangeFormUi();
              if (window.PG_HQ_HOLIDAY && typeof window.PG_HQ_HOLIDAY.init === 'function') window.PG_HQ_HOLIDAY.init(p, { force: true });
            }
            return;
          }
          var profileDel = ev.target && ev.target.closest ? ev.target.closest('.hq-bizday-profile-del') : null;
          if (profileDel && p.contains(profileDel)) {
            ev.stopPropagation();
            var delId = profileDel.getAttribute('data-id') || '';
            rt.runHqBizdayDeleteById(delId);
            return;
          }
          var profileEdit = ev.target && ev.target.closest ? ev.target.closest('.hq-bizday-profile-edit') : null;
          if (profileEdit && p.contains(profileEdit)) {
            ev.stopPropagation();
            var peId = profileEdit.getAttribute('data-id') || '';
            var peItem = st0.currentList.find(function (x) { return String(x.id || '') === peId; });
            if (peItem) rt.loadBizdayProfileIntoEditor(peItem);
            return;
          }
          var tr = ev.target && ev.target.closest ? ev.target.closest('.hq-bizday-row') : null;
          if (!tr || !p.contains(tr)) return;
          var id = tr.getAttribute('data-id') || '';
          var item = st0.currentList.find(function (x) { return String(x.id || '') === id; });
          if (!item) return;
          rt.loadBizdayProfileIntoEditor(item);
        });
      }
      loadList();
    }
    var hqPgApiAddBtn = pane.querySelector('#hqPgApiAddBtn');
    if (hqPgApiAddBtn && url === '/hq/pgApiMng' && !hqPgApiAddBtn._hqPgAddBound) {
      hqPgApiAddBtn._hqPgAddBound = true;
      hqPgApiAddBtn.addEventListener('click', function () {
        if (window.openPgAgencyModal) window.openPgAgencyModal({});
      });
    }
    if (url === '/hq/pgApiMng' && !pane._hqPgGridDbl) {
      pane._hqPgGridDbl = true;
      pane.addEventListener('dblclick', function (e) {
        var tr = e.target.closest('tbody tr');
        if (!tr || tr.querySelector('.empty-state-cell')) return;
        var grid = pane.querySelector('#grid_' + tabId + ' tbody');
        if (!grid || !grid.contains(tr)) return;
        var rows = grid.querySelectorAll('tr');
        var idx = Array.prototype.indexOf.call(rows, tr);
        var list = pane._lastGridList || [];
        if (idx < 0 || idx >= list.length) return;
        var row = list[idx];
        if (window.openPgAgencyModal) {
          window.openPgAgencyModal({
            id: row.id,
            pgCd: row.pgCd,
            pgNm: row.pgNm,
            apiEndpoint: row.apiEndpoint || '',
            useYn: row.useYn || 'Y'
          });
        }
      });
    }
    var hqPermissionSaveBtn = pane.querySelector('#hqPermissionSaveBtn');
    if (hqPermissionSaveBtn && url === '/hq/permissionMng' && !hqPermissionSaveBtn._bound) {
      hqPermissionSaveBtn._bound = true;
      hqPermissionSaveBtn.addEventListener('click', function () {
        var st = pane._orgPermState;
        if (!st) { alert('저장할 데이터가 없습니다.'); return; }
        if (!pgConfirmBeforeSave('저장하시겠습니까?')) return;
        var keepOrgLevel = pane._orgPermActiveLv || '';
        var dimm3 = document.getElementById('dimm');
        if (dimm3) dimm3.style.display = 'flex';
        window.PG_API.hqPermissionMngSave({ matrix: st }).then(function (res) {
          if (keepOrgLevel) pane._orgPermActiveLv = keepOrgLevel;
          if (res && window.initOrgPagePermissionMatrix) {
            window.initOrgPagePermissionMatrix(pane, tabId, res);
          }
          alert('저장되었습니다.');
        }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimm3) dimm3.style.display = 'none'; });
      });
    }
    var hqPermissionReloadBtn = pane.querySelector('#hqPermissionReloadBtn');
    if (hqPermissionReloadBtn && url === '/hq/permissionMng' && !hqPermissionReloadBtn._bound) {
      hqPermissionReloadBtn._bound = true;
      hqPermissionReloadBtn.addEventListener('click', function () {
        if (!window.PG_API || !window.PG_API.hqPermissionMng) return;
        if (!pgConfirmBeforeSave('서버에 저장된 단계별 기본 권한을 다시 불러옵니다. 저장하지 않은 편집은 취소됩니다. 계속할까요?')) return;
        var keepOrgLevel = pane._orgPermActiveLv || '';
        var dimmR = document.getElementById('dimm');
        if (dimmR) dimmR.style.display = 'flex';
        window.PG_API.hqPermissionMng({}).then(function (res) {
          if (keepOrgLevel) pane._orgPermActiveLv = keepOrgLevel;
          if (res && window.initOrgPagePermissionMatrix) {
            window.initOrgPagePermissionMatrix(pane, tabId, res);
          }
        }).catch(function (e) { alert(e && e.message ? e.message : '불러오기 실패'); }).finally(function () { if (dimmR) dimmR.style.display = 'none'; });
      });
    }
    function bindSettlementExecuteRun(btn) {
      if (!btn) return;
      btn.addEventListener('click', function () {
        var fromEl = pane.querySelector('#searchFromDate');
        var toEl = pane.querySelector('#searchToDate');
        var compIdEl = pane.querySelector('input[name="searchCompId"]');
        var fromDate = fromEl && fromEl.value ? fromEl.value : '';
        var toDate = toEl && toEl.value ? toEl.value : '';
        if (!fromDate || !toDate) { alert('정산대상일(시작일~종료일)을 입력하세요.'); return; }
        var merchantId = compIdEl && compIdEl.value ? compIdEl.value.trim() : '';
        var dimm4 = document.getElementById('dimm');
        if (dimm4) dimm4.style.display = 'flex';
        var runParams = { fromDate: fromDate, toDate: toDate };
        if (merchantId) runParams.merchantId = merchantId;
        window.PG_API.settlementExecuteRun(runParams).then(function (list) {
          alert('정산 실행 완료. ' + (list && list.length ? list.length : 0) + '건');
          doSearch(pane, tabId, 1);
        }).catch(function (e) {
          alert(e && e.message ? e.message : '정산 실행 실패');
        }).finally(function () { if (dimm4) dimm4.style.display = 'none'; });
      });
    }
    var exCalcBtn = pane.querySelector('#exCalcBtn');
    if (exCalcBtn && (url === '/calc/exCalcList' || url === '/settlement/execute')) bindSettlementExecuteRun(exCalcBtn);
    var executeBtn = pane.querySelector('#executeBtn');
    if (executeBtn && (url === '/calc/exCalcList' || url === '/settlement/execute')) bindSettlementExecuteRun(executeBtn);
    function selectedGridRows() {
      var out = [];
      var tbody = pane.querySelector('#grid_' + tabId + ' tbody');
      if (!tbody) return out;
      var list = pane._lastGridList || [];
      var rows = tbody.querySelectorAll('tr');
      rows.forEach(function (tr, idx) {
        var cb = tr.querySelector('input.grid-check-row');
        if (cb && cb.checked && list[idx]) out.push(list[idx]);
      });
      return out;
    }
    var balDeductBtn = pane.querySelector('#balanceDeductBtn');
    if (balDeductBtn && (url === '/calc/balcInfo' || url === '/settlement/balanceMng')) {
      balDeductBtn.addEventListener('click', function () {
        var sels = selectedGridRows();
        if (!sels.length) { alert('차감할 업체를 선택하세요.'); return; }
        if (sels.length > 1) { alert('한 번에 1개 업체만 차감 처리할 수 있습니다.'); return; }
        var row = sels[0] || {};
        var compId = String(row.compId || '').trim();
        if (!compId) { alert('선택 행의 업체코드를 확인할 수 없습니다.'); return; }
        var remain = parseInt(row.remainAmount || row.balcAmount || 0, 10) || 0;
        var amtRaw = window.prompt('차감 금액을 입력하세요. (업체: ' + compId + ')', String(remain > 0 ? remain : '0'));
        if (amtRaw == null) return;
        var amount = parseInt(String(amtRaw).replace(/,/g, ''), 10);
        if (!(amount > 0)) { alert('차감 금액은 0보다 커야 합니다.'); return; }
        var memo = window.prompt('차감 사유(선택)', '선택차감') || '';
        var dimm4 = document.getElementById('dimm');
        if (dimm4) dimm4.style.display = 'flex';
        window.PG_API.settlementBalanceDeduct({ compId: compId, amount: amount, memo: memo }).then(function () {
          alert('차감 처리되었습니다.');
          doSearch(pane, tabId, parseInt(pane.getAttribute('data-last-page') || '1', 10) || 1);
        }).catch(function (e) {
          alert(e && e.message ? e.message : '차감 처리 실패');
        }).finally(function () { if (dimm4) dimm4.style.display = 'none'; });
      });
    }
    var balManualBtn = pane.querySelector('#balanceManualDeductBtn');
    if (balManualBtn && (url === '/calc/balcInfo' || url === '/settlement/balanceMng')) {
      balManualBtn.addEventListener('click', function () {
        var compId = (window.prompt('업체코드를 입력하세요.') || '').trim();
        if (!compId) return;
        var amtRaw = window.prompt('직접 차감 금액을 입력하세요.', '0');
        if (amtRaw == null) return;
        var amount = parseInt(String(amtRaw).replace(/,/g, ''), 10);
        if (!(amount > 0)) { alert('차감 금액은 0보다 커야 합니다.'); return; }
        var memo = window.prompt('차감 사유(선택)', '직접입력차감') || '';
        var dimm4 = document.getElementById('dimm');
        if (dimm4) dimm4.style.display = 'flex';
        window.PG_API.settlementBalanceDeduct({ compId: compId, amount: amount, memo: memo }).then(function () {
          alert('차감 처리되었습니다.');
          doSearch(pane, tabId, 1);
        }).catch(function (e) {
          alert(e && e.message ? e.message : '차감 처리 실패');
        }).finally(function () { if (dimm4) dimm4.style.display = 'none'; });
      });
    }
    bindPagingEvents(pane, tabId);
  }

  function getPagingContainer(pane, tabId) {
    if (!pane || !tabId) return null;
    return pane.querySelector('#paging_' + tabId) || pane.querySelector('.pagination-pages');
  }

  function syncPaginationSizeButtons(pane, size) {
    if (!pane) return;
    pane.querySelectorAll('.pagination-size-opt').forEach(function (btn) {
      var s = btn.getAttribute('data-size');
      btn.classList.toggle('pagination-size-opt--active', s === String(size));
    });
  }

  function renderPagingNumbers(pane, tabId, currentPage, totalPages, totalElements) {
    var container = getPagingContainer(pane, tabId);
    if (!container) return;
    currentPage = Math.max(1, parseInt(currentPage, 10) || 1);
    totalPages = Math.max(1, parseInt(totalPages, 10) || 1);
    var total = totalElements != null ? parseInt(totalElements, 10) : 0;
    if (isNaN(total)) total = 0;
    var totalPageEl = pane.querySelector('#totalPageCount');
    if (totalPageEl) totalPageEl.textContent = totalPages;
    var totalEl = pane.querySelector('#totalElementsCount');
    if (totalEl) totalEl.textContent = total;
    var pageCntEl = pane.querySelector('#pageCnt');
    if (pageCntEl) { pageCntEl.value = currentPage; pageCntEl.setAttribute('max', totalPages); }
    var sizeEl = pane.querySelector('#recordsPerPage');
    if (sizeEl) syncPaginationSizeButtons(pane, sizeEl.value);
    var html = '';
    if (totalPages <= 1) {
      html = '<span class="pagination-num pagination-num--current">1</span>';
    } else {
      var cur = currentPage;
      var total = totalPages;
      var show = [];
      var maxVisible = 7;
      if (total <= maxVisible) {
        for (var i = 1; i <= total; i++) show.push(i);
      } else {
        show.push(1);
        var from = Math.max(2, cur - 1);
        var to = Math.min(total - 1, cur + 1);
        if (from > 2) show.push('...');
        for (var j = from; j <= to; j++) { if (show.indexOf(j) === -1) show.push(j); }
        if (to < total - 1) show.push('...');
        if (total > 1) show.push(total);
      }
      for (var k = 0; k < show.length; k++) {
        var p = show[k];
        if (p === '...') {
          html += '<span class="pagination-ellipsis">…</span>';
        } else {
          var active = p === cur ? ' pagination-num--current' : '';
          html += '<button type="button" class="pagination-num' + active + '" data-page="' + p + '">' + p + '</button>';
        }
      }
    }
    container.innerHTML = html;
    container.querySelectorAll('.pagination-num[data-page]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var page = parseInt(this.getAttribute('data-page'), 10);
        if (!page) return;
        var cnt = pane.querySelector('#pageCnt');
        if (cnt) cnt.value = page;
        var totalEl = pane.querySelector('#totalElementsCount');
        var totalElements = totalEl ? parseInt(totalEl.textContent || '0', 10) : 0;
        renderPagingNumbers(pane, tabId, page, totalPages, totalElements);
        pane.dispatchEvent(new CustomEvent('paging-change', { detail: { page: page, totalPages: totalPages } }));
      });
    });
  }

  function bindPagingEvents(pane, tabId) {
    if (!pane || !tabId) return;
    renderPagingNumbers(pane, tabId, 1, 1, 0);
    pane.querySelectorAll('.pagination-size-opt').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var size = this.getAttribute('data-size');
        if (!size) return;
        var sizeInput = pane.querySelector('#recordsPerPage');
        if (sizeInput) sizeInput.value = size;
        syncPaginationSizeButtons(pane, size);
        doSearch(pane, tabId, 1);
      });
    });
    var pageCntEl = pane.querySelector('#pageCnt');
    if (pageCntEl) {
      pageCntEl.addEventListener('change', function () {
        var totalPages = parseInt(pane.querySelector('#totalPageCount').textContent || 1, 10);
        var page = Math.max(1, Math.min(totalPages, parseInt(this.value, 10) || 1));
        var totalEl = pane.querySelector('#totalElementsCount');
        var totalElements = totalEl ? parseInt(totalEl.textContent || '0', 10) : 0;
        renderPagingNumbers(pane, tabId, page, totalPages, totalElements);
      });
    }
  }

  window.updatePaging = function (tabId, currentPage, totalPages, totalElements) {
    var pane = document.getElementById(tabId);
    if (pane) renderPagingNumbers(pane, tabId, currentPage, totalPages, totalElements);
  };

  function ensureTabPane(tabId, url) {
    var container = document.getElementById(TAB_MAIN);
    if (!container) return null;
    var pane = document.getElementById(tabId);
    if (pane) return pane;
    pane = document.createElement('div');
    pane.id = tabId;
    pane.className = 'tab-pane tabConDiv';
    pane.setAttribute('formurl', url || '');
    container.appendChild(pane);
    return pane;
  }

  function loadContent(url, menuId, label) {
    var info = MENU_INFO[url] || {};
    var menuLabel = label || info.label || url;
    var tabId = getTabIdFromUrl(url);
    var mainPane = document.getElementById('main');

    if (url === '/main' || !url) {
      if (mainPane) {
        mainPane.classList.add('show', 'active');
        mainPane.style.display = 'block';
      }
      document.querySelectorAll('#' + TAB_MAIN + ' .tab-pane').forEach(function (p) {
        if (p.id !== 'main') {
          p.classList.remove('show', 'active');
          p.style.display = 'none';
        }
      });
    } else {
      if (mainPane) {
        mainPane.classList.remove('show', 'active');
        mainPane.style.display = 'none';
      }
      var pane = ensureTabPane(tabId, url);
      if (pane && window.PG_SCREENS && window.PG_SCREENS.getScreenHtml) {
        pane.setAttribute('formurl', url || '');
        pane.innerHTML = window.PG_SCREENS.getScreenHtml(url, tabId);
        pane.classList.add('show', 'active');
        pane.style.display = 'block';
        bindScreenEvents(pane, tabId);
      }
      document.querySelectorAll('#' + TAB_MAIN + ' .tab-pane').forEach(function (p) {
        if (p.id !== tabId) {
          p.classList.remove('show', 'active');
          p.style.display = 'none';
        }
      });
    }

    var breadcrumb = document.querySelector('.breadcrumb-item.navi, li.navi');
    if (breadcrumb) {
      breadcrumb.textContent = info.parent ? info.parent + ' > ' + menuLabel : menuLabel;
      breadcrumb.style.fontWeight = 'bold';
    }
    var titleEl = document.getElementById('common__header__title');
    if (titleEl) {
      titleEl.innerHTML = '<i class="bi bi-chevron-right"></i> ' + menuLabel;
      titleEl.classList.toggle('empty-title', url === '/system/noticeList');
    }
  }

  window.fnTopMenuMove = function (url, menuId, label) {
    if (!url) return;
    if (!isMenuAllowedForCurrentUser(url)) {
      alert('이 메뉴에 대한 접근 권한이 없습니다. [조직별 권한 세팅]에서 해당 화면 권한을 확인하세요.');
      return;
    }
    var link = document.querySelector('.child-li[data-url="' + url + '"] a');
    var mid = menuId || (link && link.getAttribute('data-menu_id'));
    var info = MENU_INFO[url] || {};
    var text = label || (link && link.textContent.trim()) || info.label || getTabIdFromUrl(url);
    addTabAndSwitch(url, mid, text);
  };

  function syncAllNoticeWriteButtons() {
    var u = getSessionUser();
    var show = !!(u && u.canWriteNotice);
    document.querySelectorAll('[data-notice-write-btn]').forEach(function (btn) {
      btn.classList.toggle('d-none', !show);
    });
  }

  function ensureNoticeWriteModal() {
    var el = document.getElementById('pgNoticeWriteModal');
    if (el) return el;
    var wrap = document.createElement('div');
    wrap.innerHTML =
      '<div class="modal fade" id="pgNoticeWriteModal" tabindex="-1" aria-labelledby="pgNoticeWriteModalLabel" aria-hidden="true">' +
      '<div class="modal-dialog modal-lg modal-dialog-scrollable">' +
      '<div class="modal-content">' +
      '<div class="modal-header">' +
      '<h5 class="modal-title" id="pgNoticeWriteModalLabel">공지 등록</h5>' +
      '<button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="닫기"></button></div>' +
      '<div class="modal-body">' +
      '<div class="mb-3"><label class="form-label" for="pgNoticeWriteTitle">제목</label>' +
      '<input type="text" class="form-control" id="pgNoticeWriteTitle" maxlength="500" placeholder="제목"></div>' +
      '<div class="mb-0"><label class="form-label" for="pgNoticeWriteContent">내용</label>' +
      '<textarea class="form-control" id="pgNoticeWriteContent" rows="12" placeholder="내용"></textarea></div>' +
      '</div>' +
      '<div class="modal-footer">' +
      '<button type="button" class="btn btn-secondary" data-bs-dismiss="modal">취소</button>' +
      '<button type="button" class="btn btn-primary" id="pgNoticeWriteSaveBtn">등록</button>' +
      '</div></div></div></div>';
    document.body.appendChild(wrap.firstChild);
    el = document.getElementById('pgNoticeWriteModal');
    var saveBtn = el.querySelector('#pgNoticeWriteSaveBtn');
    if (saveBtn && !saveBtn._pgBound) {
      saveBtn._pgBound = true;
      saveBtn.addEventListener('click', function () {
        var titleEl = el.querySelector('#pgNoticeWriteTitle');
        var contentEl = el.querySelector('#pgNoticeWriteContent');
        var title = titleEl && titleEl.value ? String(titleEl.value).trim() : '';
        var content = contentEl && contentEl.value ? String(contentEl.value) : '';
        if (!title) { alert('제목을 입력하세요.'); return; }
        if (!window.PG_API || !window.PG_API.noticeCreate) { alert('API를 사용할 수 없습니다.'); return; }
        window.PG_API.noticeCreate(title, content).then(function () {
          alert('등록되었습니다.');
          try {
            var M = window.bootstrap && window.bootstrap.Modal;
            if (M) {
              var inst = M.getOrCreateInstance ? M.getOrCreateInstance(el) : M.getInstance(el);
              if (inst) inst.hide();
            }
          } catch (err) {}
          if (titleEl) titleEl.value = '';
          if (contentEl) contentEl.value = '';
          if (typeof el._onNoticeSaved === 'function') {
            try { el._onNoticeSaved(); } catch (e1) {}
          }
          el._onNoticeSaved = null;
        }).catch(function (e) {
          alert(e && e.message ? e.message : '등록 실패');
        });
      });
    }
    return el;
  }

  function openNoticeWriteModal(onSaved) {
    var el = ensureNoticeWriteModal();
    el._onNoticeSaved = typeof onSaved === 'function' ? onSaved : null;
    var titleEl = el.querySelector('#pgNoticeWriteTitle');
    var contentEl = el.querySelector('#pgNoticeWriteContent');
    if (titleEl) titleEl.value = '';
    if (contentEl) contentEl.value = '';
    try {
      var M = window.bootstrap && window.bootstrap.Modal;
      if (!M) { alert('화면 구성을 불러오지 못했습니다.'); return; }
      var modal = M.getOrCreateInstance ? M.getOrCreateInstance(el) : new M(el);
      modal.show();
    } catch (e) {
      alert('공지 등록 창을 열 수 없습니다.');
    }
  }

  // 로고 클릭 / 탭 위임 / 접기
  function syncSessionUserFromAuthMe() {
    if (!window.PG_API || !window.PG_API.authMe) return Promise.resolve();
    var timeoutMs = 15000;
    var timeoutPromise = new Promise(function (_, reject) {
      setTimeout(function () { reject(new Error('timeout')); }, timeoutMs);
    });
    return Promise.race([window.PG_API.authMe(), timeoutPromise]).then(function (r) {
      var d = r && r.data ? r.data : r;
      if (!d || d.ok === false) return;
      try {
        var prev = JSON.parse(sessionStorage.getItem('pg_admin_user') || '{}');
        if (d.pagePermissions !== undefined) prev.pagePermissions = d.pagePermissions;
        if (d.orgLevel !== undefined) prev.orgLevel = d.orgLevel;
        if (d.compId !== undefined) prev.compId = d.compId;
        if (d.orgUnitId !== undefined) prev.orgUnitId = d.orgUnitId;
        if (d.role !== undefined) prev.role = d.role;
        if (d.canWriteNotice !== undefined) prev.canWriteNotice = !!d.canWriteNotice;
        sessionStorage.setItem('pg_admin_user', JSON.stringify(prev));
        syncAllNoticeWriteButtons();
      } catch (e) {}
    }).catch(function () {});
  }

  window.applyPagePermissionToPane = function (pane, url) {
    if (!pane || !url) return;
    var perm = getPagePermissionForUrl(url);
    pane.classList.remove('pg-perm-observer', 'pg-perm-modify');
    function observerAllowTarget(t) {
      if (!t || !t.closest) return false;
      if (t.closest('.screen-search-form')) return true;
      if (t.closest('.pagination-row')) return true;
      if (t.closest('.table-column-guide')) return true;
      return false;
    }
    if (perm === 'OBSERVER') {
      pane.classList.add('pg-perm-observer');
      pane.querySelectorAll('input, select, textarea').forEach(function (el) {
        if (!el) return;
        if (el.type === 'hidden') return;
        if (observerAllowTarget(el)) return;
        el.disabled = true;
        if (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA') el.readOnly = true;
      });
      pane.querySelectorAll('button').forEach(function (btn) {
        if ((btn.id || '') === 'compDetailListBtn') {
          btn.disabled = false;
          btn.removeAttribute('aria-disabled');
          btn.classList.remove('disabled');
          btn.style.pointerEvents = '';
          return;
        }
        if (observerAllowTarget(btn)) return;
        if ((btn.id || '') === 'compDevTreeRemoveBtn') return;
        if ((btn.id || '') === 'compAdminResetOrgBtn') return;
        if ((btn.id || '') === 'searchBtn' || btn.classList.contains('screen-search-btn')) return;
        if ((btn.id || '') === 'excelBtn' || (btn.id || '') === 'excelDownBtn' || (btn.id || '') === 'printBtn') return;
        if (btn.classList.contains('quick-date')) return;
        if (btn.classList.contains('pagination-size-opt')) return;
        if (btn.classList.contains('pagination-num')) return;
        if (btn.getAttribute('data-notice-write-btn')) return;
        btn.disabled = true;
        btn.style.display = '';
      });
      pane.querySelectorAll('a.btn').forEach(function (a) {
        if (!a) return;
        a.setAttribute('aria-disabled', 'true');
        a.classList.add('disabled');
        a.style.display = '';
      });
      // 핵심 저장 버튼은 항상 강제 비활성(표시는 유지)
      ['#compInfoUpdateBtn', '#compDetailSaveBtn', '#compRegSaveBtn', '#hqOrgAllowSaveBtn', '#hqBizdayProfileSaveBtn'].forEach(function (sel) {
        var b = pane.querySelector(sel);
        if (!b) return;
        b.disabled = true;
        b.style.display = '';
        b.setAttribute('aria-disabled', 'true');
        b.classList.add('disabled');
        if (sel === '#compInfoUpdateBtn' || sel === '#compDetailSaveBtn') {
          b.classList.remove('btn-primary', 'btn-success', 'btn-danger', 'btn-info', 'btn-warning');
          b.classList.add('btn-secondary');
          b.style.pointerEvents = 'none';
        }
      });
      // 옵저버는 조회만 허용: 검색/페이지네이션 외 인터랙션 이벤트 자체 차단
      if (!pane._observerGuardBound) {
        pane._observerGuardBound = true;
        pane.addEventListener('click', function (e) {
          if (getPagePermissionForUrl(url) !== 'OBSERVER') return;
          var t = e.target;
          if (!t || !t.closest) return;
          if (t.closest('#compDetailListBtn')) return;
          if (observerAllowTarget(t)) return;
          if (t.closest('#searchBtn, .screen-search-btn, #excelBtn, #excelDownBtn, #printBtn, .quick-date, .pagination-size-opt, .pagination-num, [data-notice-write-btn], #compDevTreeRemoveBtn, #compAdminResetOrgBtn')) return;
          if (t.closest('button, a, [role="button"], .btn, .tab-close-button')) {
            e.preventDefault();
            e.stopPropagation();
            e.stopImmediatePropagation();
          }
        }, true);
        pane.addEventListener('change', function (e) {
          if (getPagePermissionForUrl(url) !== 'OBSERVER') return;
          var t = e.target;
          if (!t || !t.closest) return;
          if (observerAllowTarget(t)) return;
          e.preventDefault();
          e.stopPropagation();
          e.stopImmediatePropagation();
        }, true);
        pane.addEventListener('input', function (e) {
          if (getPagePermissionForUrl(url) !== 'OBSERVER') return;
          var t = e.target;
          if (!t || !t.closest) return;
          if (observerAllowTarget(t)) return;
          e.preventDefault();
          e.stopPropagation();
          e.stopImmediatePropagation();
        }, true);
        pane.addEventListener('submit', function (e) {
          if (getPagePermissionForUrl(url) !== 'OBSERVER') return;
          e.preventDefault();
          e.stopPropagation();
          e.stopImmediatePropagation();
        }, true);
      }
    } else if (perm === 'MODIFY') {
      pane.classList.add('pg-perm-modify');
      pane.querySelectorAll('.btn-danger, button').forEach(function (btn) {
        if ((btn.id || '') === 'compDevTreeRemoveBtn') return;
        if ((btn.id || '') === 'compAdminResetOrgBtn') return;
        var t = (btn.textContent || '').trim();
        if (t.indexOf('삭제') !== -1 || t.indexOf('일괄삭제') !== -1) btn.disabled = true;
      });
    }
  };

  window.initOrgPagePermissionMatrix = function (pane, tabId, data) {
    var caps = (data && data.uiCaps) ? data.uiCaps : {};
    pane._orgPermUiCaps = caps;
    var matrixCard = pane.querySelector('.org-perm-matrix');
    var defActs = pane.querySelector('.org-perm-default-actions');
    if (caps.showLevelTabs === false) {
      if (matrixCard) matrixCard.style.display = 'none';
      if (defActs) defActs.style.display = 'none';
    } else {
      if (matrixCard) matrixCard.style.display = '';
      if (defActs) defActs.style.display = '';
    }
    var unitSec = pane.querySelector('.org-perm-unit-section');
    if (caps.showOrgUnitPanel === false) {
      if (unitSec) unitSec.style.display = 'none';
    } else if (unitSec) unitSec.style.display = '';
    var assistSec0 = pane.querySelector('.org-perm-assist-section');
    if (caps.showAssistantPanel === false) {
      if (assistSec0) assistSec0.style.display = 'none';
    } else if (assistSec0) assistSec0.style.display = '';
    var hqPermSaveTop = pane.querySelector('#hqPermissionSaveBtn');
    if (hqPermSaveTop) hqPermSaveTop.disabled = caps.canSaveLevelMatrix === false;
    var hqPermReloadTop = pane.querySelector('#hqPermissionReloadBtn');
    if (hqPermReloadTop) hqPermReloadTop.disabled = caps.canSaveLevelMatrix === false;
    var tabs = pane.querySelector('#orgPermTabs_' + tabId);
    var tbody = pane.querySelector('#orgPermTbody_' + tabId);
    if (!tabs || !tbody) {
      var fb = pane.querySelector('[id^="orgPermTbody_"]');
      if (fb) {
        fb.innerHTML = '<tr><td colspan="4" class="text-center text-danger py-4">화면을 불러오지 못했습니다. 탭을 닫았다가 다시 열거나 새로고침 후 시도하세요.</td></tr>';
      }
      return;
    }
    var catalog = (data && data.catalog) ? data.catalog : [];
    var matrix = (data && data.matrix) ? data.matrix : {};
    var orgLevels = (data && data.orgLevels) ? data.orgLevels : [];
    var permOpts = (data && data.permOptions) ? data.permOptions : [
      { v: 'NONE', t: '접근불가' }, { v: 'OBSERVER', t: '옵저버(조회만)' }, { v: 'MODIFY', t: '수정(삭제제한)' }, { v: 'DELETE', t: '삭제(전체)' }
    ];
    var state = {};
    if (caps.showLevelTabs !== false && orgLevels.length) {
      orgLevels.forEach(function (o) {
        state[o.code] = matrix[o.code] ? JSON.parse(JSON.stringify(matrix[o.code])) : {};
      });
    }
    var activeLv = pane._orgPermActiveLv || (orgLevels.length ? orgLevels[0].code : 'HEADQUARTERS');
    if (orgLevels.length && !orgLevels.some(function (o) { return o.code === activeLv; })) {
      activeLv = orgLevels[0].code;
    }
    if (caps.showLevelTabs === false || !orgLevels.length) {
      tabs.innerHTML = '';
      tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3">조직 단계별 기본 권한은 <strong>총본사</strong>(또는 시스템 관리자)만 편집합니다.</td></tr>';
      pane._orgPermState = state;
      pane._orgPermOrgLevels = orgLevels;
      pane._orgPermActiveLv = activeLv;
      pane._orgPermCatalog = catalog;
      pane._orgPermPermOpts = permOpts;
      if (typeof window.initOrgUnitPermissionPanel === 'function') {
        window.initOrgUnitPermissionPanel(pane, tabId, data);
      }
      return;
    }
    tabs.innerHTML = orgLevels.map(function (o, i) {
      return '<li class="nav-item" role="presentation"><button type="button" class="nav-link' + (i === 0 ? ' active' : '') + '" data-org-level="' + o.code + '">' + o.name + '</button></li>';
    }).join('');
    function normalizeOrgPermCode(v) {
      var x = (v != null ? String(v) : 'DELETE').trim().toUpperCase();
      if (x === 'NONE' || x === 'OBSERVER' || x === 'MODIFY' || x === 'DELETE') return x;
      return 'DELETE';
    }
    function applyOrgPermRowStyle(tr, permCode) {
      if (!tr || !tr.classList || tr.classList.contains('org-perm-group-header')) return;
      var p = normalizeOrgPermCode(permCode);
      tr.classList.remove('org-perm-row--NONE', 'org-perm-row--OBSERVER', 'org-perm-row--MODIFY', 'org-perm-row--DELETE');
      tr.classList.add('org-perm-row', 'org-perm-row--' + p);
      tr.setAttribute('data-perm', p);
    }
    var ORG_PERM_GROUP_ORDER = ['본사설정', '업체관리', '결제관리', '정산관리', '통보관리', '사용자관리', '리스크관리'];
    function buildOrgPermGroups(rows) {
      var by = {};
      (rows || []).forEach(function (row) {
        var g = (row && row.parentGroup) ? String(row.parentGroup).trim() : '기타';
        if (!by[g]) by[g] = [];
        by[g].push(row);
      });
      var out = [];
      ORG_PERM_GROUP_ORDER.forEach(function (name) {
        if (by[name] && by[name].length) {
          out.push({ name: name, rows: by[name] });
          delete by[name];
        }
      });
      Object.keys(by).forEach(function (k) {
        if (by[k].length) out.push({ name: k, rows: by[k] });
      });
      return out;
    }
    function escOrgPermHtml(s) {
      return String(s == null ? '' : s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/"/g, '&quot;');
    }
    function renderRows(lv) {
      activeLv = lv;
      pane._orgPermActiveLv = activeLv;
      var m = state[lv] || {};
      var groups = buildOrgPermGroups(catalog);
      var parts = [];
      var rowSeq = 0;
      var bulkOpts = '<option value="">이 구역 일괄…</option>' + permOpts.map(function (po) {
        return '<option value="' + po.v + '">전체 · ' + escOrgPermHtml(po.t) + '</option>';
      }).join('');
      groups.forEach(function (grp) {
        parts.push(
          '<tr class="org-perm-group-header" role="presentation">' +
          '<td colspan="4" class="org-perm-group-title p-0">' +
          '<div class="d-flex flex-wrap align-items-center justify-content-between gap-2 org-perm-group-bar">' +
          '<span class="org-perm-group-name">' + escOrgPermHtml(grp.name) + '</span>' +
          '<div class="d-flex align-items-center gap-1 flex-shrink-0">' +
          '<span class="text-muted small org-perm-bulk-hint">간편</span>' +
          '<select class="form-select form-select-sm org-perm-bulk-select" title="이 대메뉴 구역의 하위 메뉴에 동일 권한을 한 번에 적용합니다">' +
          bulkOpts +
          '</select></div></div></td></tr>'
        );
        grp.rows.forEach(function (row) {
          rowSeq += 1;
          var url = row.pageUrl || '';
          var cur = normalizeOrgPermCode(m[url] != null ? m[url] : 'DELETE');
          var opts = permOpts.map(function (po) {
            return '<option value="' + po.v + '"' + (po.v === cur ? ' selected' : '') + '>' + po.t + '</option>';
          }).join('');
          parts.push(
            '<tr class="org-perm-row org-perm-row--' + cur + '" data-perm="' + cur + '" data-page-url="' + url.replace(/"/g, '&quot;') + '">' +
            '<td class="text-center text-muted small org-perm-td-no">' + rowSeq + '</td>' +
            '<td class="font-monospace small">' + escOrgPermHtml(row.menuId || '') + '</td>' +
            '<td>' + escOrgPermHtml(row.menuNm || '') + '<div class="text-muted small">' + escOrgPermHtml(url) + '</div></td>' +
            '<td><select class="form-select form-select-sm org-perm-select" data-url="' + url.replace(/"/g, '&quot;') + '">' + opts + '</select></td></tr>'
          );
        });
      });
      tbody.innerHTML = parts.join('');
      tbody.querySelectorAll('.org-perm-bulk-select').forEach(function (bulkSel) {
        bulkSel.addEventListener('change', function () {
          var v = bulkSel.value;
          if (!v) return;
          var hdr = bulkSel.closest('tr.org-perm-group-header');
          if (!hdr) return;
          var n = hdr.nextElementSibling;
          while (n && !n.classList.contains('org-perm-group-header')) {
            var rowSel = n.querySelector('.org-perm-select');
            if (rowSel) {
              rowSel.value = v;
              var u0 = rowSel.getAttribute('data-url') || '';
              if (!state[activeLv]) state[activeLv] = {};
              state[activeLv][u0] = v;
              applyOrgPermRowStyle(n, v);
            }
            n = n.nextElementSibling;
          }
          bulkSel.selectedIndex = 0;
        });
      });
      tbody.querySelectorAll('.org-perm-select').forEach(function (sel) {
        sel.addEventListener('change', function () {
          var u0 = sel.getAttribute('data-url') || '';
          if (!state[activeLv]) state[activeLv] = {};
          state[activeLv][u0] = sel.value;
          applyOrgPermRowStyle(sel.closest('tr'), sel.value);
        });
      });
    }
    renderRows(activeLv);
    tabs.querySelectorAll('button[data-org-level]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        tabs.querySelectorAll('button[data-org-level]').forEach(function (b) { b.classList.remove('active'); });
        btn.classList.add('active');
        renderRows(btn.getAttribute('data-org-level') || 'HEADQUARTERS');
      });
    });
    tabs.querySelectorAll('button[data-org-level]').forEach(function (b) {
      if ((b.getAttribute('data-org-level') || '') === activeLv) b.classList.add('active');
      else b.classList.remove('active');
    });
    if (!tabs.querySelector('button[data-org-level].active')) {
      var firstTab = tabs.querySelector('button[data-org-level]');
      if (firstTab) firstTab.classList.add('active');
    }
    pane._orgPermState = state;
    pane._orgPermOrgLevels = orgLevels;
    pane._orgPermActiveLv = activeLv;
    pane._orgPermCatalog = catalog;
    pane._orgPermPermOpts = permOpts;
    if (typeof window.initOrgUnitPermissionPanel === 'function') {
      window.initOrgUnitPermissionPanel(pane, tabId, data);
    }
  };

  window.initOrgUnitPermissionPanel = function (pane, tabId, data) {
    var sel = pane.querySelector('#orgPermUnitSelect_' + tabId);
    var codeEl = pane.querySelector('#orgPermUnitCode_' + tabId);
    var levelEl = pane.querySelector('#orgPermUnitLevel_' + tabId);
    var currentModeEl = pane.querySelector('#orgPermUnitCurrentMode_' + tabId);
    var modeEl = pane.querySelector('#orgPermUnitMode_' + tabId);
    var tbody = pane.querySelector('#orgPermUnitTbody_' + tabId);
    var hint = pane.querySelector('#orgPermUnitHint_' + tabId);
    var saveBtn = pane.querySelector('#hqOrgUnitPermissionSaveBtn_' + tabId);
    var catalog = pane._orgPermCatalog || (data && data.catalog) || [];
    var permOpts = pane._orgPermPermOpts || [
      { v: 'NONE', t: '접근불가' }, { v: 'OBSERVER', t: '옵저버(조회만)' }, { v: 'MODIFY', t: '수정(삭제제한)' }, { v: 'DELETE', t: '삭제(전체)' }
    ];
    pane._orgPermPayloadOrgLevels = (data && data.orgLevels) ? data.orgLevels : (pane._orgPermPayloadOrgLevels || []);
    if (!sel || !modeEl || !tbody) return;

    var uiCapsPanel = pane._orgPermUiCaps || {};
    if (saveBtn && uiCapsPanel.canSaveOrgUnit === false) {
      saveBtn.disabled = true;
    }
    if (modeEl && uiCapsPanel.canSaveOrgUnit === false) {
      modeEl.disabled = true;
    }

    var ORG_PERM_GROUP_ORDER = ['본사설정', '업체관리', '결제관리', '정산관리', '통보관리', '사용자관리', '리스크관리'];

    function escOrgPermHtml(s) {
      return String(s == null ? '' : s)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/"/g, '&quot;');
    }
    function normalizeOrgPermCode(v) {
      var x = (v != null ? String(v) : 'DELETE').trim().toUpperCase();
      if (x === 'NONE' || x === 'OBSERVER' || x === 'MODIFY' || x === 'DELETE') return x;
      return 'DELETE';
    }
    function applyOrgPermRowStyle(tr, permCode) {
      if (!tr || !tr.classList || tr.classList.contains('org-perm-group-header')) return;
      var p = normalizeOrgPermCode(permCode);
      tr.classList.remove('org-perm-row--NONE', 'org-perm-row--OBSERVER', 'org-perm-row--MODIFY', 'org-perm-row--DELETE');
      tr.classList.add('org-perm-row', 'org-perm-row--' + p);
      tr.setAttribute('data-perm', p);
    }
    function buildOrgPermGroups(rows) {
      var by = {};
      (rows || []).forEach(function (row) {
        var g = (row && row.parentGroup) ? String(row.parentGroup).trim() : '기타';
        if (!by[g]) by[g] = [];
        by[g].push(row);
      });
      var out = [];
      ORG_PERM_GROUP_ORDER.forEach(function (name) {
        if (by[name] && by[name].length) {
          out.push({ name: name, rows: by[name] });
          delete by[name];
        }
      });
      Object.keys(by).forEach(function (k) {
        if (by[k].length) out.push({ name: k, rows: by[k] });
      });
      return out;
    }

    function resolveOrgLevelNameKo(u) {
      if (!u) return '';
      if (u.orgLevelName) return u.orgLevelName;
      var lv = pane._orgPermPayloadOrgLevels || [];
      var code = u.orgLevel || '';
      for (var li = 0; li < lv.length; li++) {
        if (lv[li].code === code) return lv[li].name || code;
      }
      return code;
    }

    function modeText(mode) {
      return mode === 'CUSTOM' ? '개별 설정' : '단계 기본 따름';
    }
    function fillOrgInfo(u) {
      if (codeEl) codeEl.value = u ? (u.code || '') : '';
      if (levelEl) levelEl.value = u ? resolveOrgLevelNameKo(u) : '';
      if (currentModeEl) currentModeEl.value = modeText(u && u.mode ? u.mode : 'LEVEL_DEFAULT');
    }
    function renderOrgUnitSelect(unitList, selectedId) {
      var units = unitList || [];
      pane._orgUnitList = units;
      sel.innerHTML = '<option value="">— 업체를 선택하세요 —</option>' + units.map(function (u) {
        return '<option value="' + String(u.id) + '">' + escOrgPermHtml(u.name || '') + '</option>';
      }).join('');
      if (selectedId) sel.value = String(selectedId);
    }

    function renderUnitRows(permMap, readOnly) {
      var m = permMap || {};
      var groups = buildOrgPermGroups(catalog);
      var parts = [];
      var rowSeq = 0;
      var bulkOpts = '<option value="">이 구역 일괄…</option>' + permOpts.map(function (po) {
        return '<option value="' + po.v + '">전체 · ' + escOrgPermHtml(po.t) + '</option>';
      }).join('');
      groups.forEach(function (grp) {
        parts.push(
          '<tr class="org-perm-group-header" role="presentation">' +
          '<td colspan="4" class="org-perm-group-title p-0">' +
          '<div class="d-flex flex-wrap align-items-center justify-content-between gap-2 org-perm-group-bar">' +
          '<span class="org-perm-group-name">' + escOrgPermHtml(grp.name) + '</span>' +
          '<div class="d-flex align-items-center gap-1 flex-shrink-0">' +
          '<span class="text-muted small org-perm-bulk-hint">간편</span>' +
          '<select class="form-select form-select-sm org-perm-bulk-select org-perm-unit-bulk"' + (readOnly ? ' disabled' : '') + ' title="이 대메뉴 구역의 하위 메뉴에 동일 권한을 한 번에 적용합니다">' +
          bulkOpts +
          '</select></div></div></td></tr>'
        );
        grp.rows.forEach(function (row) {
          rowSeq += 1;
          var url = row.pageUrl || '';
          var cur = normalizeOrgPermCode(m[url] != null ? m[url] : 'DELETE');
          var opts = permOpts.map(function (po) {
            return '<option value="' + po.v + '"' + (po.v === cur ? ' selected' : '') + '>' + po.t + '</option>';
          }).join('');
          parts.push(
            '<tr class="org-perm-row org-perm-row--' + cur + '" data-perm="' + cur + '" data-page-url="' + url.replace(/"/g, '&quot;') + '">' +
            '<td class="text-center text-muted small org-perm-td-no">' + rowSeq + '</td>' +
            '<td class="font-monospace small">' + escOrgPermHtml(row.menuId || '') + '</td>' +
            '<td>' + escOrgPermHtml(row.menuNm || '') + '<div class="text-muted small">' + escOrgPermHtml(url) + '</div></td>' +
            '<td><select class="form-select form-select-sm org-perm-select org-perm-unit-select"' + (readOnly ? ' disabled' : '') + ' data-url="' + url.replace(/"/g, '&quot;') + '">' + opts + '</select></td></tr>'
          );
        });
      });
      tbody.innerHTML = parts.join('');
      pane._orgUnitPermState = {};
      Object.keys(m).forEach(function (k) { pane._orgUnitPermState[k] = m[k]; });
      tbody.querySelectorAll('.org-perm-unit-bulk').forEach(function (bulkSel) {
        bulkSel.addEventListener('change', function () {
          if (readOnly) return;
          var v = bulkSel.value;
          if (!v) return;
          var hdr = bulkSel.closest('tr.org-perm-group-header');
          if (!hdr) return;
          var n = hdr.nextElementSibling;
          while (n && !n.classList.contains('org-perm-group-header')) {
            var rowSel = n.querySelector('.org-perm-unit-select');
            if (rowSel) {
              rowSel.value = v;
              var u0 = rowSel.getAttribute('data-url') || '';
              pane._orgUnitPermState[u0] = v;
              applyOrgPermRowStyle(n, v);
            }
            n = n.nextElementSibling;
          }
          bulkSel.selectedIndex = 0;
        });
      });
      tbody.querySelectorAll('.org-perm-unit-select').forEach(function (selEl) {
        selEl.addEventListener('change', function () {
          if (readOnly) return;
          var u0 = selEl.getAttribute('data-url') || '';
          pane._orgUnitPermState[u0] = selEl.value;
          applyOrgPermRowStyle(selEl.closest('tr'), selEl.value);
        });
      });
    }

    function permStrengthAssist(v) {
      var p = (v != null ? String(v) : 'DELETE').toUpperCase();
      if (p === 'DELETE') return 4;
      if (p === 'MODIFY') return 3;
      if (p === 'OBSERVER') return 2;
      if (p === 'NONE') return 1;
      return 4;
    }

    function syncOrgAssistantPanel(det) {
      var assistSec = pane.querySelector('.org-perm-assist-section');
      var roleTabs = pane.querySelector('#orgPermAssistRoleTabs_' + tabId);
      var atbody = pane.querySelector('#orgPermAssistTbody_' + tabId);
      var asave = pane.querySelector('#hqOrgAssistSaveBtn_' + tabId);
      if (!assistSec || uiCapsPanel.showAssistantPanel === false) return;
      if (!roleTabs || !atbody) return;
      if (!det || !det.effective) {
        atbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3">조직을 선택하세요.</td></tr>';
        roleTabs.innerHTML = '';
        if (asave) asave.disabled = true;
        return;
      }
      var roles = det.assistantRoles && det.assistantRoles.length ? det.assistantRoles : ['MANAGER', 'OPERATOR', 'SETTLEMENT', 'TECH'];
      var matrix = det.assistantMatrix || {};
      pane._assistDetailOrgId = det.orgUnit && det.orgUnit.id != null ? String(det.orgUnit.id) : '';
      pane._assistEffective = det.effective || {};
      pane._assistMatrixState = {};
      roles.forEach(function (r) {
        pane._assistMatrixState[r] = JSON.parse(JSON.stringify(matrix[r] || {}));
      });
      if (!pane._assistActiveRole || roles.indexOf(pane._assistActiveRole) < 0) pane._assistActiveRole = roles[0];
      pane._assistRoles = roles;

      function roleLabel(r) {
        if (r === 'MANAGER') return '관리(MANAGER)';
        if (r === 'OPERATOR') return '운영(OPERATOR)';
        if (r === 'SETTLEMENT') return '정산(SETTLEMENT)';
        if (r === 'TECH') return '기술(TECH)';
        return r;
      }

      function buildOptsForCeiling(ceiling, currentVal) {
        var c = permStrengthAssist(ceiling);
        var parts = '<option value="">조직 기본(상한)</option>';
        permOpts.forEach(function (po) {
          if (po.v === 'NONE') return;
          if (permStrengthAssist(po.v) <= c) {
            parts += '<option value="' + po.v + '"' + (currentVal === po.v ? ' selected' : '') + '>' + escOrgPermHtml(po.t) + '</option>';
          }
        });
        return parts;
      }

      function renderAssistRowsForRole(role) {
        pane._assistActiveRole = role;
        var eff = pane._assistEffective || {};
        var m = pane._assistMatrixState[role] || {};
        var groups = buildOrgPermGroups(catalog);
        var parts = [];
        var rowSeq = 0;
        groups.forEach(function (grp) {
          grp.rows.forEach(function (row) {
            var url = row.pageUrl || '';
            var ceiling = normalizeOrgPermCode(eff[url] != null ? eff[url] : 'DELETE');
            if (ceiling === 'NONE') return;
            rowSeq += 1;
            var stored = m[url] != null ? normalizeOrgPermCode(m[url]) : '';
            var selVal = stored && permStrengthAssist(stored) <= permStrengthAssist(ceiling) ? stored : '';
            var opts = buildOptsForCeiling(ceiling, selVal);
            var disp = selVal || ceiling;
            parts.push(
              '<tr class="org-perm-row org-perm-row--' + disp + '" data-page-url="' + url.replace(/"/g, '&quot;') + '">' +
              '<td class="text-center text-muted small">' + rowSeq + '</td>' +
              '<td class="font-monospace small">' + escOrgPermHtml(row.menuId || '') + '</td>' +
              '<td>' + escOrgPermHtml(row.menuNm || '') + '<div class="text-muted small">조직 상한: ' + escOrgPermHtml(ceiling) + '</div><div class="text-muted small">' + escOrgPermHtml(url) + '</div></td>' +
              '<td><select class="form-select form-select-sm org-perm-assist-select" data-url="' + url.replace(/"/g, '&quot;') + '" data-ceiling="' + ceiling + '"' + (uiCapsPanel.canSaveAssistant ? '' : ' disabled') + '>' + opts + '</select></td></tr>'
            );
          });
        });
        if (!parts.length) {
          atbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-3">이 조직에서는 접근 가능한 메뉴가 없습니다.</td></tr>';
          return;
        }
        atbody.innerHTML = parts.join('');
        atbody.querySelectorAll('.org-perm-assist-select').forEach(function (selEl) {
          selEl.addEventListener('change', function () {
            var u0 = selEl.getAttribute('data-url') || '';
            var v = selEl.value;
            var st = pane._assistMatrixState[pane._assistActiveRole] || {};
            if (!v) delete st[u0];
            else st[u0] = v;
            pane._assistMatrixState[pane._assistActiveRole] = st;
            var cl = selEl.getAttribute('data-ceiling') || 'DELETE';
            applyOrgPermRowStyle(selEl.closest('tr'), v || normalizeOrgPermCode(cl));
          });
        });
      }

      roleTabs.innerHTML = roles.map(function (r) {
        return '<li class="nav-item" role="presentation"><button type="button" class="nav-link' + (r === pane._assistActiveRole ? ' active' : '') + '" data-assist-role="' + r + '">' + escOrgPermHtml(roleLabel(r)) + '</button></li>';
      }).join('');
      roleTabs.querySelectorAll('button[data-assist-role]').forEach(function (btn) {
        btn.addEventListener('click', function () {
          roleTabs.querySelectorAll('button[data-assist-role]').forEach(function (b) { b.classList.remove('active'); });
          btn.classList.add('active');
          renderAssistRowsForRole(btn.getAttribute('data-assist-role') || roles[0]);
        });
      });
      renderAssistRowsForRole(pane._assistActiveRole);
      if (asave) asave.disabled = !uiCapsPanel.canSaveAssistant || !pane._assistDetailOrgId;
    }

    var units = (data && data.orgUnits) ? data.orgUnits : [];
    var prevSelectedId = sel.value || '';
    renderOrgUnitSelect(units, prevSelectedId);
    if (units.length === 1 && !prevSelectedId) {
      sel.value = String(units[0].id);
    }

    function loadOrgUnit(id) {
      if (!id) {
        fillOrgInfo(null);
        modeEl.value = 'LEVEL_DEFAULT';
        modeEl.disabled = true;
        if (saveBtn) saveBtn.disabled = true;
        tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted py-4">조직을 선택하세요.</td></tr>';
        hint.textContent = '조직을 선택하면 적용 방식과 권한 표가 채워집니다.';
        pane._orgUnitDetailLevelDefault = null;
        pane._orgUnitDetailEffective = null;
        syncOrgAssistantPanel(null);
        return;
      }
      var selected = (pane._orgUnitList || []).find(function (u) { return String(u.id) === String(id); }) || null;
      fillOrgInfo(selected);
      if (!window.PG_API || !window.PG_API.hqOrgUnitPermission) return;
      var dimm = document.getElementById('dimm');
      if (dimm) dimm.style.display = 'flex';
      window.PG_API.hqOrgUnitPermission({ orgUnitId: id }).then(function (res) {
        var det = res && res.data !== undefined ? res.data : res;
        if (!det) return;
        var mode = (det.mode === 'CUSTOM') ? 'CUSTOM' : 'LEVEL_DEFAULT';
        modeEl.value = mode;
        modeEl.disabled = uiCapsPanel.canSaveOrgUnit === false;
        if (saveBtn) saveBtn.disabled = uiCapsPanel.canSaveOrgUnit === false;
        if (currentModeEl) currentModeEl.value = modeText(mode);
        if (selected) selected.mode = mode;
        var levelDef = det.levelDefault || {};
        var eff = det.effective || {};
        pane._orgUnitDetailLevelDefault = levelDef;
        pane._orgUnitDetailEffective = eff;
        hint.innerHTML = mode === 'CUSTOM'
          ? '<span class="text-primary">개별 설정</span>이 저장되어 있습니다. 아래는 <strong>로그인 시 적용되는 최종 권한</strong>입니다.'
          : '<span class="text-secondary">단계 기본 따름</span> — 아래는 해당 조직 단계의 <strong>기본 매트릭스와 동일한 적용 결과</strong>입니다.';
        var readOnly = (mode === 'LEVEL_DEFAULT') || uiCapsPanel.canSaveOrgUnit === false;
        renderUnitRows(readOnly ? levelDef : eff, readOnly);
        syncOrgAssistantPanel(det);
      }).catch(function (e) {
        alert(e && e.message ? e.message : '조회 실패');
      }).finally(function () { if (dimm) dimm.style.display = 'none'; });
    }

    if (!sel._orgUnitBound) {
      sel._orgUnitBound = true;
      sel.addEventListener('change', function () {
        loadOrgUnit(sel.value);
      });
    }
    if (!modeEl._orgUnitModeBound) {
      modeEl._orgUnitModeBound = true;
      modeEl.addEventListener('change', function () {
        var ld = pane._orgUnitDetailLevelDefault || {};
        var eff = pane._orgUnitDetailEffective || {};
        if (modeEl.value === 'LEVEL_DEFAULT') {
          hint.innerHTML = '<span class="text-secondary">단계 기본 따름</span>(저장 시 개별 덮어쓰기가 제거됩니다). 미리보기는 기본 매트릭스와 동일합니다.';
          renderUnitRows(ld, true);
        } else {
          hint.innerHTML = '<span class="text-primary">개별 설정</span> — 아래에서 수정 후 상단 [설정저장]을 누르세요.';
          var base = eff && Object.keys(eff).length ? eff : ld;
          renderUnitRows(JSON.parse(JSON.stringify(base)), uiCapsPanel.canSaveOrgUnit === false);
        }
        if (!sel.value) return;
        var previewEff = modeEl.value === 'LEVEL_DEFAULT' ? ld : (eff && Object.keys(eff).length ? eff : ld);
        var fakeDet = {
          effective: previewEff,
          assistantRoles: pane._assistRoles || ['MANAGER', 'OPERATOR', 'SETTLEMENT', 'TECH'],
          assistantMatrix: pane._assistMatrixState || {},
          orgUnit: { id: sel.value }
        };
        syncOrgAssistantPanel(fakeDet);
      });
    }
    if (saveBtn && !saveBtn._orgUnitSaveBound) {
      saveBtn._orgUnitSaveBound = true;
      saveBtn.addEventListener('click', function () {
        var id = sel.value;
        if (!id) { alert('조직을 선택하세요.'); return; }
        if (!window.PG_API || !window.PG_API.hqOrgUnitPermissionSave) return;
        if (!pgConfirmBeforeSave('저장하시겠습니까?')) return;
        var mode = modeEl.value;
        var pages = pane._orgUnitPermState || {};
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.hqOrgUnitPermissionSave({ orgUnitId: id, mode: mode, pages: pages }).then(function (res) {
          var det = res && res.data !== undefined ? res.data : res;
          alert('저장되었습니다.');
          var list = (pane._orgUnitList || []).slice();
          var newMode = det && det.mode ? det.mode : mode;
          for (var ji = 0; ji < list.length; ji++) {
            if (String(list[ji].id) === String(id)) {
              list[ji].mode = newMode;
              break;
            }
          }
          pane._orgUnitList = list;
          var selected = list.find(function (u) { return String(u.id) === String(id); }) || null;
          fillOrgInfo(selected);
          loadOrgUnit(id);
        }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      });
    }
    var asaveBtn = pane.querySelector('#hqOrgAssistSaveBtn_' + tabId);
    if (asaveBtn && !asaveBtn._assistSaveBound) {
      asaveBtn._assistSaveBound = true;
      asaveBtn.addEventListener('click', function () {
        var oid = pane._assistDetailOrgId;
        if (!oid) { alert('조직을 선택하세요.'); return; }
        if (!window.PG_API || !window.PG_API.hqOrgUnitAssistantPermissionSave) return;
        if (!pgConfirmBeforeSave('담당자 권한그룹별 메뉴를 저장하시겠습니까?')) return;
        var dimmA = document.getElementById('dimm');
        if (dimmA) dimmA.style.display = 'flex';
        window.PG_API.hqOrgUnitAssistantPermissionSave({ orgUnitId: oid, matrix: pane._assistMatrixState || {} }).then(function (res) {
          alert('저장되었습니다.');
          var det = res && res.data !== undefined ? res.data : res;
          if (det) syncOrgAssistantPanel(det);
        }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimmA) dimmA.style.display = 'none'; });
      });
    }
    if (sel.value) loadOrgUnit(sel.value);
    else if (units.length === 1) loadOrgUnit(String(units[0].id));
    else loadOrgUnit('');
  };

  document.addEventListener('DOMContentLoaded', function () {
    syncSessionUserFromAuthMe().finally(function () {
      applyAdminOnlyMenuItems();
      applyMenuVisibilityByPagePermissions();
      redirectIfActiveMenuForbidden();
    });
    setTableRowPaddingY(getTableRowPaddingY());
    var ul = document.getElementById(TAB_UL);
    if (ul) {
      ul.addEventListener('click', function (e) {
        var a = e.target.closest('.tab-a');
        if (!a) return;
        e.preventDefault();
        var li = a.closest('.copyTopTab');
        if (!li) return;
        var tabUrl = li.getAttribute('top_tab_url');
        var mid = a.getAttribute('menu_id');
        document.querySelectorAll('#' + TAB_UL + ' .tab-a').forEach(function (x) { x.classList.remove('active'); });
        a.classList.add('active');
        setActiveMenuByUrl(tabUrl);
        loadContent(tabUrl, mid, a.textContent.trim());
      });
    }
    var logo = document.getElementById('leftside-logo');
    if (logo && !logo.getAttribute('data-bound')) {
      logo.setAttribute('data-bound', '1');
      logo.addEventListener('click', function (e) {
        if (!config.contentBaseUrl) {
          e.preventDefault();
          fnTopMenuMove('/main', null, '메인');
        }
      });
    }
    // 대메뉴 플라이아웃·aria (접기 버튼보다 먼저 선언)
    var flyout = document.getElementById('flyout-submenu');
    function hideFlyout() {
      if (flyout) flyout.style.display = 'none';
    }
    function syncSideNavExpandedAria() {
      document.querySelectorAll('#side-nav-ul > .side-nav-item').forEach(function (item) {
        var link = item.querySelector('.side-nav-link');
        var sub = item.querySelector('.side-nav-second-level');
        if (!link || !sub) return;
        link.setAttribute('aria-expanded', sub.classList.contains('mm-show') ? 'true' : 'false');
      });
    }
    // 접기/펴기 버튼
    var foldBtn = document.getElementById('leftSideFoldBtn');
    var leftMenu = document.querySelector('.left-side-menu');
    if (foldBtn && leftMenu) {
      foldBtn.addEventListener('click', function () {
        var isCollapsing = !leftMenu.classList.contains('collapsed');
        leftMenu.classList.toggle('collapsed');

        // CSS 적용이 안 되더라도 확실히 접히도록 width/minWidth를 직접 제어
        if (leftMenu.classList.contains('collapsed')) {
          leftMenu.style.width = '70px';
          leftMenu.style.minWidth = '70px';
        } else {
          leftMenu.style.width = '260px';
          leftMenu.style.minWidth = '260px';
        }

        if (isCollapsing) {
          document.querySelectorAll('.side-nav-second-level').forEach(function (el) { el.classList.remove('mm-show'); });
          document.querySelectorAll('.side-nav-item').forEach(function (el) { el.classList.remove('mm-active'); });
        }

        // 접기 → 펴기 전환 시에도 항상 플라이아웃은 감춰야 함
        var f = document.getElementById('flyout-submenu');
        if (f) f.style.display = 'none';

        var span = document.getElementById('leftSideFoldSpan');
        var icon = document.getElementById('leftSideFoldIcon');
        if (span) span.textContent = leftMenu.classList.contains('collapsed') ? ' » 펴기' : ' « 접기';
        if (icon) icon.className = leftMenu.classList.contains('collapsed') ? 'bi bi-chevron-double-right' : 'bi bi-chevron-double-left';
        syncSideNavExpandedAria();
      });
    }
    // 대메뉴 클릭 → 펼침: 토글 / 접힘: 플라이아웃으로 서브 표시 (사이드바 유지)
    document.querySelectorAll('.side-nav .side-nav-link').forEach(function (a) {
      a.addEventListener('click', function (e) {
        if (this.getAttribute('href') !== 'javascript:void(0)' && this.getAttribute('href') !== '#') return;
        e.preventDefault();
        var parent = this.closest('.side-nav-item');
        var sub = parent && parent.querySelector('.side-nav-second-level');
        if (!sub) return;
        var left = document.querySelector('.left-side-menu');
        if (left && left.classList.contains('collapsed') && flyout) {
          var isSame = parent.classList.contains('mm-active') && flyout.style.display === 'block';
          if (isSame) {
            hideFlyout();
            parent.classList.remove('mm-active');
            syncSideNavExpandedAria();
            return;
          }
          var rect = this.getBoundingClientRect();
          flyout.innerHTML = '';
          var clone = sub.cloneNode(true);
          clone.classList.add('mm-show');
          flyout.appendChild(clone);
          flyout.style.top = rect.top + 'px';
          flyout.style.display = 'block';
          document.querySelectorAll('.side-nav-item').forEach(function (el) { el.classList.remove('mm-active'); });
          parent.classList.add('mm-active');
          syncSideNavExpandedAria();
        } else {
          // 펼쳐진 상태: 항상 한 개의 대메뉴만 열리도록 처리
          hideFlyout();
          var isOpen = sub.classList.contains('mm-show');
          document.querySelectorAll('.side-nav-second-level').forEach(function (el) { el.classList.remove('mm-show'); });
          document.querySelectorAll('.side-nav-item').forEach(function (el) { el.classList.remove('mm-active'); });
          if (!isOpen) {
            parent.classList.add('mm-active');
            sub.classList.add('mm-show');
          }
          syncSideNavExpandedAria();
        }
      });
    });
    document.addEventListener('click', function (e) {
      if (flyout && flyout.style.display === 'block') {
        if (!flyout.contains(e.target) && !e.target.closest('.left-side-menu')) hideFlyout();
      }
    });
    document.addEventListener('keydown', function (e) {
      if (e.key === 'Escape') hideFlyout();
    });
    // 서브메뉴(.child-li) 클릭 → document 위임으로 사이드바+플라이아웃 모두 처리
    document.addEventListener('click', function (e) {
      var a = e.target.closest('.child-li a');
      if (!a) return;
      e.preventDefault();
      var li = a.closest('.child-li');
      var url = li && li.getAttribute('data-url');
      if (!url) return;
      hideFlyout();
      fnTopMenuMove(url, a.getAttribute('data-menu_id'), a.textContent.trim());
    });
    var pgAgSave = document.getElementById('pgAgencyEditSaveBtn');
    if (pgAgSave && !pgAgSave._bound) {
      pgAgSave._bound = true;
      pgAgSave.addEventListener('click', function () {
        if (!window.pgDoubleConfirm || !window.pgDoubleConfirm('PG사 연동 정보를 저장하시겠습니까?', '정말 저장하시겠습니까?')) return;
        var idVal = document.getElementById('pgAgencyEditId').value.trim();
        var body = {
          pgCd: document.getElementById('pgAgencyEditPgCd').value.trim(),
          pgNm: document.getElementById('pgAgencyEditPgNm').value.trim(),
          apiEndpoint: (document.getElementById('pgAgencyEditEndpoint') && document.getElementById('pgAgencyEditEndpoint').value) ? document.getElementById('pgAgencyEditEndpoint').value.trim() : '',
          useYn: (document.getElementById('pgAgencyEditUseYn') && document.getElementById('pgAgencyEditUseYn').value) || 'Y'
        };
        if (idVal) body.id = idVal;
        if (!body.pgCd || !body.pgNm) { alert('PG사코드와 PG사명은 필수입니다.'); return; }
        var dimm = document.getElementById('dimm');
        if (dimm) dimm.style.display = 'flex';
        window.PG_API.hqPgApiMngSave(body).then(function () {
          alert('저장되었습니다.');
          var modalEl = document.getElementById('pgAgencyEditModal');
          if (modalEl && window.bootstrap && bootstrap.Modal) {
            var inst = bootstrap.Modal.getInstance(modalEl);
            if (inst) inst.hide();
          }
          var hqPane = document.getElementById('hq_pgApiMng');
          if (hqPane) {
            var sb = hqPane.querySelector('#searchBtn');
            if (sb) sb.click();
          }
        }).catch(function (e) { alert(e && e.message ? e.message : '저장 실패'); }).finally(function () { if (dimm) dimm.style.display = 'none'; });
      });
    }
  });
})();
