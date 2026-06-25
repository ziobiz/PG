/**
 * 공통 UI: 인라인 배너·모달 (alert/prompt 대체용)
 */
(function (global) {
  'use strict';

  global.PG_UI = global.PG_UI || {};

  function uiT(s) {
    try {
      if (global.PG_UI_I18N && typeof global.PG_UI_I18N.t === 'function') return global.PG_UI_I18N.t(String(s));
    } catch (e) {}
    return String(s);
  }

  /**
   * @param {HTMLElement} root - 배너 조상(탭 pane 등)
   * @param {string} bannerId - # 제외 id
   * @param {'success'|'danger'|'warning'|'info'} variant
   * @param {string} message
   * @param {number} [autoHideMs] - 0이면 자동 숨김 없음
   */
  global.PG_UI.showBanner = function (root, bannerId, variant, message, autoHideMs) {
    if (!root || !bannerId) return;
    var box = root.querySelector('#' + bannerId);
    if (!box) return;
    var textEl = box.querySelector('[data-pg-banner-text]');
    if (textEl) textEl.textContent = message || '';
    box.classList.remove('alert-success', 'alert-danger', 'alert-warning', 'alert-info', 'alert-primary', 'd-none');
    box.classList.add('alert', 'alert-' + (variant || 'info'));
    if (box._pgHideTimer) clearTimeout(box._pgHideTimer);
    if (autoHideMs && autoHideMs > 0) {
      box._pgHideTimer = setTimeout(function () {
        box.classList.add('d-none');
      }, autoHideMs);
    }
  };

  global.PG_UI.hideBanner = function (root, bannerId) {
    if (!root || !bannerId) return;
    var box = root.querySelector('#' + bannerId);
    if (box) box.classList.add('d-none');
  };

  global.PG_UI.openModal = function (modalEl) {
    if (!modalEl || !global.bootstrap || !global.bootstrap.Modal) return;
    global.bootstrap.Modal.getOrCreateInstance(modalEl).show();
  };

  global.PG_UI.closeModal = function (modalEl) {
    if (!modalEl || !global.bootstrap || !global.bootstrap.Modal) return;
    var inst = global.bootstrap.Modal.getInstance(modalEl);
    if (inst) inst.hide();
  };

  function escHtml(s) {
    return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;');
  }

  function escAttr(s) {
    return String(s == null ? '' : s).replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;');
  }

  /** 영문·숫자 status 토큰 → uiT 키(한국어 canonical) */
  var PAY_STATUS_EN_TO_KO_KEY = {
    success: '성공',
    paid: '성공',
    complete: '성공',
    completed: '성공',
    authorized: '성공',
    approved: '승인',
    approve: '승인',
    fail: '실패',
    failed: '실패',
    failure: '실패',
    error: '오류',
    declined: '실패',
    decline: '실패',
    cancel: '취소',
    cancelled: '취소',
    canceled: '취소',
    void: '무효',
    voided: '무효',
    emailvoid: '이메일 무효',
    'email void': '이메일 무효',
    'manual void': '수동무효',
    refund: '환불',
    refunded: '환불',
    'force refund': '강제환불',
    forcerefund: '강제환불',
    'auto void': '자동무효',
    'auto refund': '자동환불',
    pending: '요청',
    processing: '요청',
    request: '요청',
    requested: '요청',
    waitauthorize: '인증대기',
    other: '기타'
  };

  /** 한국어 statusNm·payDivNm 등 → uiT */
  var PAY_STATUS_KO_KEYS = [
    '성공', '실패', '취소', '무효', '환불', '요청', '오류', '이메일 무효', '이메일무효', '자동무효', '자동환불',
    '강제환불', '수동무효', '승인', '인증대기', '기타'
  ];

  /**
   * 결제·수수료 그리드 상태 표기 — 코드·한국어·영문을 현재 UI 로케일로.
   * @param {string} rawText statusNm / chillPaymentStatus / 코드
   * @param {object} [rowOpt] status 코드 보조(row.status)
   */
  global.PG_UI.localizePayStatusLabel = function (rawText, rowOpt) {
    var s = rawText == null ? '' : String(rawText).trim();
    if (s === '이메일무효') s = '이메일 무효';
    if (!s || s === '—' || s === '-') return '—';
    if (rowOpt && rowOpt.status != null) {
      var stCode = String(rowOpt.status).trim();
      if (stCode && typeof global.PG_UI.internalPayStatusToKo === 'function') {
        var fromCode = global.PG_UI.internalPayStatusToKo(stCode);
        if (fromCode && fromCode !== '—' && fromCode !== stCode) return fromCode;
      }
    }
    if (/^[0-4]$/.test(s) || /^(08|10|20|21|22|30|31|40|41|42|99|F0|f0)$/.test(s)) {
      if (typeof global.PG_UI.internalPayStatusToKo === 'function') {
        return global.PG_UI.internalPayStatusToKo(s);
      }
    }
    var low = s.toLowerCase().replace(/\s+/g, ' ').trim();
    var enKey = PAY_STATUS_EN_TO_KO_KEY[low] || PAY_STATUS_EN_TO_KO_KEY[low.replace(/\s/g, '')];
    if (enKey) return uiT(enKey);
    if (PAY_STATUS_KO_KEYS.indexOf(s) >= 0) return uiT(s);
    if (/^(성공|취소|실패|무효|환불|요청|오류|이메일 무효|이메일무효|자동무효|자동환불|강제환불|수동무효|승인|인증대기|기타)/.test(s)) {
      var m = s.match(/^(성공|취소|실패|무효|환불|요청|오류|이메일 무효|이메일무효|자동무효|자동환불|강제환불|수동무효|승인|인증대기|기타)/);
      if (m) return uiT(m[1] === '이메일무효' ? '이메일 무효' : m[1]);
    }
    if (low.indexOf('email') >= 0 && low.indexOf('void') >= 0) return uiT('이메일 무효');
    if (low.indexOf('force') >= 0 && low.indexOf('refund') >= 0) return uiT('강제환불');
    if (low.indexOf('auto') >= 0 && low.indexOf('void') >= 0) return uiT('자동무효');
    if (low.indexOf('auto') >= 0 && low.indexOf('refund') >= 0) return uiT('자동환불');
    if (low.indexOf('manual') >= 0 && low.indexOf('void') >= 0) return uiT('수동무효');
    if (low.indexOf('void') >= 0 || s.indexOf('무효') >= 0) {
      if (low.indexOf('email') >= 0 || s.indexOf('이메일') >= 0) return uiT('이메일 무효');
      return uiT('무효');
    }
    if (low.indexOf('refund') >= 0 || s.indexOf('환불') >= 0) return uiT('환불');
    if (low.indexOf('cancel') >= 0 || s === '취소') return uiT('취소');
    if (low.indexOf('fail') >= 0 || low.indexOf('declin') >= 0 || s === '실패') return uiT('실패');
    if (low.indexOf('success') >= 0 || low === 'paid' || s === '성공') return uiT('성공');
    if (low.indexOf('pending') >= 0 || low.indexOf('request') >= 0 || s === '요청') return uiT('요청');
    var tr = uiT(s);
    return tr !== s ? tr : s;
  };

  /** 로케일 변경 시 이미 렌더된 .pay-grid-status-badge 텍스트 갱신 */
  global.PG_UI.refreshPayGridStatusBadges = function (root) {
    if (!root || !root.querySelectorAll) return;
    root.querySelectorAll('.pay-grid-status-badge').forEach(function (el) {
      var raw = el.getAttribute('data-pg-status-raw');
      if (raw == null || raw === '') raw = el.textContent || '';
      el.textContent = global.PG_UI.localizePayStatusLabel(raw, null);
    });
  };

  /**
   * 결제내역 그리드 행·Status 뱃지 톤 — 내부 status 우선, 없으면 chillPaymentStatus·구분.
   * @returns {'success'|'cancel'|'void'|'refund'|'fail'|'pending'|'other'|'neutral'}
   */
  global.PG_UI.resolvePayRowTone = function (row) {
    if (!row || typeof row !== 'object') return 'neutral';
    var divEarly = String(row.payDivNm || '').trim();
    divEarly = uiT(divEarly);
    if (divEarly.indexOf(uiT('무효')) >= 0 || divEarly === uiT('자동환불')) return 'void';
    if (divEarly === uiT('환불')) return 'refund';
    if (divEarly === uiT('실패')) return 'fail';
    if (divEarly === uiT('취소')) return 'cancel';
    if (divEarly === uiT('인증대기')) return 'other';
    var st = row.status != null ? String(row.status).trim() : '';
    if (st === '21' || st === '22' || st === '40' || st === '41' || st === '42') return 'void';
    if (st === '10') {
      var labEarly = row.chillPaymentStatus != null ? String(row.chillPaymentStatus).trim() : '';
      var ulE = labEarly.toLowerCase();
      if (labEarly && (/^(21|22|40|41|42)$/.test(labEarly) || ulE.indexOf('무효') >= 0 && ulE.indexOf('취소') === -1
          || /(^|[^a-z0-9_])void([^a-z0-9_]|$)/i.test(ulE) && ulE.indexOf('cancel') === -1
          || ulE.indexOf('voided') >= 0 || ulE.indexOf('emailvoid') >= 0 || ulE.indexOf('email_void') >= 0)) {
        return 'void';
      }
      return 'success';
    }
    if (st === '20') {
      var lab0 = row.chillPaymentStatus != null ? String(row.chillPaymentStatus).trim() : '';
      var ul0 = lab0.toLowerCase();
      if (lab0 && (/^(21|22|40|41|42)$/.test(lab0) || ul0.indexOf('무효') >= 0 && ul0.indexOf('취소') === -1
          || /(^|[^a-z0-9_])void([^a-z0-9_]|$)/i.test(ul0) && ul0.indexOf('cancel') === -1
          || ul0.indexOf('emailvoid') >= 0 || ul0.indexOf('email_void') >= 0)) {
        return 'void';
      }
      return 'cancel';
    }
    if (st === '30' || st === '31') return 'refund';
    if (st === '99' || st === 'F0' || st.toLowerCase() === 'f0') return 'fail';
    if (st === '08') return 'pending';
    var div = String(row.payDivNm || '').trim();
    div = uiT(div);
    if (div === uiT('인증대기')) return 'other';
    if (div === uiT('환불')) return 'refund';
    if (div === uiT('실패')) return 'fail';
    if (div === uiT('취소')) return 'cancel';
    if (div.indexOf(uiT('무효')) >= 0) return 'void';
    var lab = row.chillPaymentStatus != null ? String(row.chillPaymentStatus).trim() : '';
    if (!lab || lab === '-') return 'neutral';
    lab = uiT(lab);
    if (lab === uiT('성공')) return 'success';
    if (lab === uiT('취소')) return 'cancel';
    if (lab === uiT('실패') || lab === uiT('오류')) return 'fail';
    if (lab.indexOf(uiT('무효')) >= 0) return 'void';
    if (lab.indexOf(uiT('환불')) >= 0) return 'refund';
    if (lab.indexOf(uiT('인증대기')) >= 0) return 'other';
    if (lab === uiT('요청') || lab.indexOf(uiT('대기')) >= 0) return 'pending';
    if (/^(paid|success|complete|authorized)$/i.test(lab)) return 'success';
    if (/cancel/i.test(lab) && lab.indexOf('무효') === -1) return 'cancel';
    if (/refund/i.test(lab)) return 'refund';
    if (/fail|error|declin/i.test(lab)) return 'fail';
    if (/void/i.test(lab)) return 'void';
    if (/pending|processing|wait|request/i.test(lab)) return 'pending';
    return 'neutral';
  };

  /**
   * 통합내역(ChillPay 실시간) — Status·PaymentStatus 숫자·영문 토큰.
   * @returns {'success'|'cancel'|'void'|'refund'|'fail'|'pending'|'neutral'}
   */
  global.PG_UI.resolveChillTrRowTone = function (row) {
    if (!row || typeof row !== 'object') return 'neutral';
    var raw = row.status != null ? String(row.status).trim() : '';
    if (!raw) return 'neutral';
    var low = raw.toLowerCase();
    if (raw === '0') return 'success';
    if (raw === '2') return 'cancel';
    if (raw === '1' || raw === '3' || raw === '4') return 'fail';
    if (/voided|void|emailvoid|manual void|email void|무효|이메일무효/.test(low)) return 'void';
    if (/refund/.test(low)) return 'refund';
    if (/cancel|cancelled|canceled/.test(low)) return 'cancel';
    if (/fail|error|declin|오류/.test(low)) return 'fail';
    if (/paid|success|complete|authorized|settled|성공/.test(low)) return 'success';
    if (/pending|wait|request|processing|authorize|요청|대기/.test(low)) return 'pending';
    return 'neutral';
  };

  /**
   * JPAY 통합조회 — ICOPAY dbStatus·icopayStatus 우선, 포털 Trading Status 텍스트 폴백.
   * @returns {'success'|'cancel'|'void'|'refund'|'fail'|'pending'|'other'|'neutral'}
   */
  global.PG_UI.resolveJpayTrRowTone = function (row) {
    if (!row || typeof row !== 'object') return 'neutral';
    var code = row.dbStatus != null ? String(row.dbStatus).trim() : '';
    if (!code && row.icopayStatus != null) code = String(row.icopayStatus).trim();
    if (code) {
      return global.PG_UI.resolvePayRowTone({
        status: code,
        chillPaymentStatus: row.statusNm != null ? String(row.statusNm).trim() : ''
      });
    }
    var portal = row.status != null ? String(row.status).trim() : '';
    if (!portal && row.tradingStatus != null) portal = String(row.tradingStatus).trim();
    if (!portal && row.statusNm != null) portal = String(row.statusNm).trim();
    if (portal) {
      return global.PG_UI.resolveChillTrRowTone({ status: portal });
    }
    return 'neutral';
  };

  /** 결제내역·통합내역 Status 열 — 행 톤과 동일한 뱃지 색(기타=상단 집계 OTHER 톤) */
  global.PG_UI.payGridStatusBadge = function (rawText, tone, rowOpt) {
    var t = tone && tone !== 'neutral' ? tone : 'neutral';
    var raw = rawText == null ? '' : String(rawText).trim();
    var label = global.PG_UI.localizePayStatusLabel(raw, rowOpt);
    var inner = escHtml(label);
    if (!inner) inner = '—';
    var rawAttr = escAttr(raw || rawText || '');
    return '<span class="pay-grid-status-badge pay-grid-status-badge--' + t + '" data-pg-status-raw="' + rawAttr + '">' + inner + '</span>';
  };

  /**
   * 내부 결제 상태 코드 → 화면 표기 (PayListItemDto.chillInternalPayStatusToKo / chillIcPayStatusCodeTokenToKo 와 동일 계열).
   * 알 수 없는 값은 원문 그대로 반환(빈 값은 '—').
   */
  global.PG_UI.internalPayStatusToKo = function (st) {
    var s = st == null ? '' : String(st).trim();
    if (!s) return '—';
    if (/^[0-4]$/.test(s)) {
      if (s === '0') return uiT('성공');
      if (s === '1' || s === '3') return uiT('실패');
      if (s === '2') return uiT('취소');
      if (s === '4') return uiT('오류');
    }
    switch (s) {
      case '10': return uiT('성공');
      case '08': return uiT('요청');
      case '20': return uiT('취소');
      case '21': return uiT('무효');
      case '22': return uiT('이메일 무효');
      case '30': return uiT('환불');
      case '31': return uiT('강제환불');
      case '40': return uiT('자동무효');
      case '41': return uiT('이메일 무효');
      case '42': return uiT('자동환불');
      case '99':
      case 'F0':
      case 'f0': return uiT('실패');
      default: {
        var enKey = PAY_STATUS_EN_TO_KO_KEY[s.toLowerCase()];
        if (enKey) return uiT(enKey);
        if (PAY_STATUS_KO_KEYS.indexOf(s) >= 0) return uiT(s);
        var tr = uiT(s);
        return tr !== s ? tr : s;
      }
    }
  };
})(typeof window !== 'undefined' ? window : this);
